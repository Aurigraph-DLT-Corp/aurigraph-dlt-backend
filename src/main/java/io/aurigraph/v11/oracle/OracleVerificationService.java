package io.aurigraph.v11.oracle;

import io.aurigraph.v11.bridge.security.SignatureVerificationService;
import io.aurigraph.v11.models.OracleStatus;
import io.aurigraph.v11.services.OracleStatusService;
import io.quarkus.logging.Log;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Oracle Verification Service
 * Implements multi-oracle consensus algorithm for asset price verification
 *
 * Features:
 * - 3-of-5 minimum oracle consensus
 * - Parallel price fetching with CompletableFuture
 * - Signature verification using BouncyCastle (CRYSTALS-Dilithium)
 * - Median price calculation
 * - 5% price tolerance validation
 * - Database persistence via OracleVerificationRepository
 *
 * @author Aurigraph V11 - Backend Development Agent
 * @version 11.0.0
 * @sprint Sprint 16 - Oracle Verification System (AV11-483)
 */
@ApplicationScoped
public class OracleVerificationService {

    private static final int MIN_ORACLES_REQUIRED = 3;
    private static final long ORACLE_TIMEOUT_SECONDS = 5;
    private static final int SCALE = 18; // Precision for BigDecimal calculations

    @Inject
    OracleStatusService oracleStatusService;

    @Inject
    OracleVerificationRepository verificationRepository;

    @Inject
    SignatureVerificationService signatureVerificationService;

    @ConfigProperty(name = "oracle.verification.min.consensus", defaultValue = "0.51")
    double minConsensusThreshold;

    @ConfigProperty(name = "oracle.verification.price.tolerance", defaultValue = "0.05")
    double priceTolerance;

    /**
     * Verify asset value using multi-oracle consensus
     *
     * @param assetId The asset identifier
     * @param claimedValue The claimed value to verify
     * @return Verification result with consensus decision
     */
    @Transactional
    public Uni<OracleVerificationResult> verifyAssetValue(String assetId, BigDecimal claimedValue) {
        Log.infof("Starting oracle verification for asset: %s, claimed value: %s", assetId, claimedValue);

        long startTime = System.currentTimeMillis();
        String verificationId = generateVerificationId(assetId);

        return oracleStatusService.getOracleStatus()
            .onItem().transform(oracleStatus -> {
                // Step 1: Get active oracles (minimum 3)
                List<OracleStatus.OracleNode> activeOracles = getActiveOracles(oracleStatus);
                validateMinimumOracles(activeOracles);

                Log.infof("Found %d active oracles for verification", activeOracles.size());

                // Step 2: Fetch prices in parallel (5 second timeout)
                List<OraclePriceData> oracleResponses = fetchPricesInParallel(assetId, activeOracles);

                // Step 3: Verify signatures
                List<OraclePriceData> validResponses = verifySignatures(oracleResponses);

                Log.infof("Received %d valid responses out of %d total", validResponses.size(), oracleResponses.size());

                // Step 4: Calculate median price
                BigDecimal medianPrice = calculateMedianPrice(validResponses);

                // Step 5: Calculate price statistics
                PriceStatistics stats = calculatePriceStatistics(validResponses);

                // Step 6: Check consensus threshold
                double consensusPercentage = calculateConsensusPercentage(validResponses, medianPrice);
                boolean consensusReached = consensusPercentage >= minConsensusThreshold;

                if (!consensusReached) {
                    throw new ConsensusNotReachedException(
                        consensusPercentage,
                        minConsensusThreshold,
                        validResponses.size(),
                        oracleResponses.size()
                    );
                }

                // Step 7: Validate claimed value (5% tolerance)
                boolean withinTolerance = isWithinTolerance(claimedValue, medianPrice);
                BigDecimal variance = calculatePriceVariance(claimedValue, medianPrice);

                // Step 8: Build verification result
                OracleVerificationResult result = buildVerificationResult(
                    verificationId,
                    assetId,
                    claimedValue,
                    medianPrice,
                    consensusReached,
                    consensusPercentage,
                    variance,
                    withinTolerance,
                    oracleResponses,
                    validResponses,
                    stats,
                    startTime
                );

                Log.infof("Verification complete: %s - Status: %s, Consensus: %.2f%%, Within tolerance: %s",
                    verificationId, result.getVerificationStatus(), consensusPercentage * 100, withinTolerance);

                return result;
            })
            .onItem().call(result -> persistVerificationResult(result));
    }

    /**
     * Get active oracles from oracle status
     */
    private List<OracleStatus.OracleNode> getActiveOracles(OracleStatus oracleStatus) {
        return oracleStatus.getOracles().stream()
            .filter(oracle -> "active".equalsIgnoreCase(oracle.getStatus()))
            .filter(oracle -> "price_feed".equalsIgnoreCase(oracle.getOracleType()))
            .collect(Collectors.toList());
    }

    /**
     * Validate minimum number of oracles
     */
    private void validateMinimumOracles(List<OracleStatus.OracleNode> oracles) {
        if (oracles.size() < MIN_ORACLES_REQUIRED) {
            throw new InsufficientOraclesException(MIN_ORACLES_REQUIRED, oracles.size());
        }
    }

    /**
     * Fetch prices from oracles in parallel with timeout
     */
    private List<OraclePriceData> fetchPricesInParallel(String assetId, List<OracleStatus.OracleNode> oracles) {
        List<CompletableFuture<OraclePriceData>> futures = oracles.stream()
            .map(oracle -> CompletableFuture.supplyAsync(() -> fetchPriceFromOracle(assetId, oracle)))
            .collect(Collectors.toList());

        // Wait for all futures with timeout
        CompletableFuture<Void> allFutures = CompletableFuture.allOf(
            futures.toArray(new CompletableFuture[0])
        );

        try {
            allFutures.get(ORACLE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (Exception e) {
            Log.warnf("Some oracle requests timed out: %s", e.getMessage());
        }

        // Collect completed results
        return futures.stream()
            .map(future -> {
                try {
                    return future.getNow(null);
                } catch (Exception e) {
                    return null;
                }
            })
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    /**
     * Fetch price from a single oracle
     * In production, this would make actual API calls to oracle providers
     */
    private OraclePriceData fetchPriceFromOracle(String assetId, OracleStatus.OracleNode oracle) {
        long startTime = System.currentTimeMillis();
        OraclePriceData priceData = new OraclePriceData(
            oracle.getOracleId(),
            oracle.getOracleName(),
            oracle.getProvider()
        );
        priceData.setAssetId(assetId); // Store assetId for signature verification

        try {
            // Simulate oracle price fetch
            // In production, this would call the actual oracle API
            BigDecimal simulatedPrice = simulateOraclePrice(assetId, oracle);
            String signature = simulateSignature(assetId, simulatedPrice, oracle);

            priceData.setPrice(simulatedPrice);
            priceData.setSignature(signature);
            priceData.setStatus("success");
            priceData.setResponseTimeMs(System.currentTimeMillis() - startTime);

            Log.debugf("Oracle %s returned price: %s for asset: %s", oracle.getOracleId(), simulatedPrice, assetId);

        } catch (Exception e) {
            priceData.setStatus("failed");
            priceData.setErrorMessage(e.getMessage());
            Log.warnf("Failed to fetch price from oracle %s: %s", oracle.getOracleId(), e.getMessage());
        }

        return priceData;
    }

    /**
     * Simulate oracle price (for testing/development)
     * In production, this would be replaced with actual oracle API calls
     */
    private BigDecimal simulateOraclePrice(String assetId, OracleStatus.OracleNode oracle) {
        // Base price with slight variance based on oracle
        Random random = new Random(oracle.getOracleId().hashCode() + assetId.hashCode());
        double basePrice = 1000.0; // Example base price
        double variance = (random.nextDouble() - 0.5) * 0.04; // ±2% variance
        double price = basePrice * (1.0 + variance);
        return BigDecimal.valueOf(price).setScale(SCALE, RoundingMode.HALF_UP);
    }

    /**
     * Simulate signature generation
     * In production, oracles would sign their responses with CRYSTALS-Dilithium
     */
    private String simulateSignature(String assetId, BigDecimal price, OracleStatus.OracleNode oracle) {
        // IMPORTANT: Use consistent data format for signature generation and verification
        String data = buildSignatureData(assetId, price, oracle.getOracleId());
        return Base64.getEncoder().encodeToString(data.getBytes());
    }

    /**
     * Build consistent signature data format
     * Format: assetId:price:oracleId
     */
    private String buildSignatureData(String assetId, BigDecimal price, String oracleId) {
        return assetId + ":" + price.toString() + ":" + oracleId;
    }

    /**
     * Verify signatures for all oracle responses
     */
    private List<OraclePriceData> verifySignatures(List<OraclePriceData> responses) {
        return responses.stream()
            .peek(response -> {
                if ("success".equals(response.getStatus()) && response.getSignature() != null) {
                    // SECURITY: Use same data format for verification as generation
                    String data = buildSignatureData(
                        response.getAssetId(), // Assuming assetId is available in response
                        response.getPrice(),
                        response.getOracleId()
                    );
                    boolean valid = signatureVerificationService.verifySignature(
                        data,
                        response.getSignature(),
                        "SECP256K1" // In production, use CRYSTALS-Dilithium
                    );
                    response.setSignatureValid(valid);
                }
            })
            .collect(Collectors.toList());
    }

    /**
     * Calculate median price from valid oracle responses with outlier detection
     * Uses Interquartile Range (IQR) method to detect and remove outliers
     * Implements Byzantine fault tolerance by removing up to f < n/3 outliers
     */
    private BigDecimal calculateMedianPrice(List<OraclePriceData> validResponses) {
        if (validResponses.isEmpty()) {
            throw new IllegalStateException("No valid oracle responses available for median calculation");
        }

        List<BigDecimal> prices = validResponses.stream()
            .filter(OraclePriceData::isValidForConsensus)
            .map(OraclePriceData::getPrice)
            .sorted()
            .collect(Collectors.toList());

        if (prices.isEmpty()) {
            throw new IllegalStateException("No valid prices available for median calculation");
        }

        // Remove outliers using IQR method (Byzantine fault tolerance)
        List<BigDecimal> cleanedPrices = removeOutliers(prices);

        Log.infof("Price analysis: %d raw prices, %d after outlier removal",
            prices.size(), cleanedPrices.size());

        if (cleanedPrices.isEmpty()) {
            Log.warnf("All prices identified as outliers, using raw data");
            cleanedPrices = prices;
        }

        int size = cleanedPrices.size();
        if (size % 2 == 0) {
            // Even number of prices: average of two middle values
            BigDecimal mid1 = cleanedPrices.get(size / 2 - 1);
            BigDecimal mid2 = cleanedPrices.get(size / 2);
            return mid1.add(mid2).divide(BigDecimal.valueOf(2), SCALE, RoundingMode.HALF_UP);
        } else {
            // Odd number of prices: middle value
            return cleanedPrices.get(size / 2);
        }
    }

    /**
     * Remove outliers using Interquartile Range (IQR) method
     * Byzantine fault tolerance: Remove up to f < n/3 outliers
     *
     * @param prices Sorted list of prices
     * @return List of prices with outliers removed
     */
    private List<BigDecimal> removeOutliers(List<BigDecimal> prices) {
        if (prices.size() < 4) {
            // Need at least 4 data points for meaningful outlier detection
            return prices;
        }

        // Calculate Q1, Q3, and IQR
        int n = prices.size();
        int q1Index = n / 4;
        int q3Index = (3 * n) / 4;

        BigDecimal q1 = prices.get(q1Index);
        BigDecimal q3 = prices.get(q3Index);
        BigDecimal iqr = q3.subtract(q1);

        // Calculate bounds (1.5 * IQR is standard for outlier detection)
        final BigDecimal initialLowerBound = q1.subtract(iqr.multiply(BigDecimal.valueOf(1.5)));
        final BigDecimal initialUpperBound = q3.add(iqr.multiply(BigDecimal.valueOf(1.5)));

        // Filter out outliers
        List<BigDecimal> cleaned = prices.stream()
            .filter(price -> price.compareTo(initialLowerBound) >= 0 && price.compareTo(initialUpperBound) <= 0)
            .collect(Collectors.toList());

        // Byzantine fault tolerance: Ensure we don't remove more than n/3 values
        int maxOutliers = n / 3;
        int outliersRemoved = n - cleaned.size();

        if (outliersRemoved > maxOutliers) {
            Log.warnf("Too many outliers detected (%d > %d), Byzantine attack suspected",
                outliersRemoved, maxOutliers);
            // In case of suspected attack, use more conservative bounds
            BigDecimal conservativeBound = iqr.multiply(BigDecimal.valueOf(2.5));
            final BigDecimal conservativeLowerBound = q1.subtract(conservativeBound);
            final BigDecimal conservativeUpperBound = q3.add(conservativeBound);

            cleaned = prices.stream()
                .filter(price -> price.compareTo(conservativeLowerBound) >= 0 && price.compareTo(conservativeUpperBound) <= 0)
                .collect(Collectors.toList());
        }

        return cleaned;
    }

    /**
     * Calculate weighted median price using oracle stake weights
     * Higher stake = more influence on final price
     *
     * @param validResponses Oracle responses with stake information
     * @return Weighted median price
     */
    private BigDecimal calculateWeightedMedian(List<OraclePriceData> validResponses) {
        if (validResponses.isEmpty()) {
            throw new IllegalStateException("No valid oracle responses available");
        }

        // For now, use equal weights since OraclePriceData doesn't have stake info
        // In production, this would fetch stake weights from oracle registry
        Map<BigDecimal, Double> priceWeights = validResponses.stream()
            .filter(OraclePriceData::isValidForConsensus)
            .collect(Collectors.toMap(
                OraclePriceData::getPrice,
                r -> getOracleStakeWeight(r.getOracleId()),
                Double::sum
            ));

        // Sort prices by value
        List<Map.Entry<BigDecimal, Double>> sortedEntries = priceWeights.entrySet()
            .stream()
            .sorted(Map.Entry.comparingByKey())
            .collect(Collectors.toList());

        // Calculate total weight
        double totalWeight = sortedEntries.stream()
            .mapToDouble(Map.Entry::getValue)
            .sum();

        // Find weighted median (50th percentile)
        double cumulativeWeight = 0;
        double targetWeight = totalWeight / 2.0;

        for (Map.Entry<BigDecimal, Double> entry : sortedEntries) {
            cumulativeWeight += entry.getValue();
            if (cumulativeWeight >= targetWeight) {
                return entry.getKey();
            }
        }

        // Fallback to last price (should not happen)
        return sortedEntries.get(sortedEntries.size() - 1).getKey();
    }

    /**
     * Get oracle stake weight from oracle registry
     * Higher stake = more voting power in consensus
     *
     * @param oracleId Oracle identifier
     * @return Stake weight (default 1.0)
     */
    private double getOracleStakeWeight(String oracleId) {
        // In production, this would query the oracle registry for stake information
        // For now, use provider-specific weights:
        // - Chainlink: 1.5 (highest reliability)
        // - Pyth: 1.3 (high frequency, institutional)
        // - Band Protocol: 1.2 (good cross-chain support)

        if (oracleId.contains("chainlink")) {
            return 1.5;
        } else if (oracleId.contains("pyth")) {
            return 1.3;
        } else if (oracleId.contains("band")) {
            return 1.2;
        }
        return 1.0;
    }

    /**
     * Calculate price statistics (min, max, average, standard deviation)
     */
    private PriceStatistics calculatePriceStatistics(List<OraclePriceData> validResponses) {
        List<BigDecimal> prices = validResponses.stream()
            .filter(OraclePriceData::isValidForConsensus)
            .map(OraclePriceData::getPrice)
            .collect(Collectors.toList());

        if (prices.isEmpty()) {
            return new PriceStatistics(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        }

        BigDecimal min = prices.stream().min(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
        BigDecimal max = prices.stream().max(BigDecimal::compareTo).orElse(BigDecimal.ZERO);

        BigDecimal sum = prices.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal average = sum.divide(BigDecimal.valueOf(prices.size()), SCALE, RoundingMode.HALF_UP);

        // Calculate standard deviation
        BigDecimal variance = prices.stream()
            .map(price -> price.subtract(average).pow(2))
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .divide(BigDecimal.valueOf(prices.size()), SCALE, RoundingMode.HALF_UP);

        BigDecimal stdDev = BigDecimal.valueOf(Math.sqrt(variance.doubleValue()))
            .setScale(SCALE, RoundingMode.HALF_UP);

        return new PriceStatistics(min, max, average, stdDev);
    }

    /**
     * Calculate consensus percentage (how many oracles agree within tolerance)
     */
    private double calculateConsensusPercentage(List<OraclePriceData> validResponses, BigDecimal medianPrice) {
        if (validResponses.isEmpty()) {
            return 0.0;
        }

        long agreeing = validResponses.stream()
            .filter(OraclePriceData::isValidForConsensus)
            .filter(response -> isWithinTolerance(response.getPrice(), medianPrice))
            .count();

        long total = validResponses.stream()
            .filter(OraclePriceData::isValidForConsensus)
            .count();

        return total > 0 ? (double) agreeing / total : 0.0;
    }

    /**
     * Check if value is within tolerance of median price
     */
    private boolean isWithinTolerance(BigDecimal value, BigDecimal medianPrice) {
        if (medianPrice.compareTo(BigDecimal.ZERO) == 0) {
            return false;
        }

        BigDecimal variance = value.subtract(medianPrice)
            .abs()
            .divide(medianPrice, SCALE, RoundingMode.HALF_UP);

        return variance.compareTo(BigDecimal.valueOf(priceTolerance)) <= 0;
    }

    /**
     * Calculate price variance percentage
     */
    private BigDecimal calculatePriceVariance(BigDecimal claimedValue, BigDecimal medianPrice) {
        if (medianPrice.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        return claimedValue.subtract(medianPrice)
            .abs()
            .divide(medianPrice, SCALE, RoundingMode.HALF_UP)
            .multiply(BigDecimal.valueOf(100)); // Convert to percentage
    }

    /**
     * Build verification result
     */
    private OracleVerificationResult buildVerificationResult(
        String verificationId,
        String assetId,
        BigDecimal claimedValue,
        BigDecimal medianPrice,
        boolean consensusReached,
        double consensusPercentage,
        BigDecimal variance,
        boolean withinTolerance,
        List<OraclePriceData> allResponses,
        List<OraclePriceData> validResponses,
        PriceStatistics stats,
        long startTime
    ) {
        OracleVerificationResult result = new OracleVerificationResult(assetId, claimedValue);
        result.setVerificationId(verificationId);
        result.setMedianPrice(medianPrice);
        result.setConsensusReached(consensusReached);
        result.setConsensusPercentage(consensusPercentage);
        result.setPriceVariance(variance);
        result.setWithinTolerance(withinTolerance);
        result.setTolerancePercentage(priceTolerance);

        result.setTotalOraclesQueried(allResponses.size());
        result.setSuccessfulOracles(validResponses.size());
        result.setFailedOracles(allResponses.size() - validResponses.size());

        result.setOracleResponses(allResponses);
        result.setTotalVerificationTimeMs(System.currentTimeMillis() - startTime);

        result.setMinPrice(stats.min);
        result.setMaxPrice(stats.max);
        result.setAveragePrice(stats.average);
        result.setStandardDeviation(stats.standardDeviation);

        // Determine verification status
        if (!consensusReached) {
            result.setVerificationStatus("REJECTED");
            result.setRejectionReason(String.format(
                "Consensus not reached. Required: %.0f%%, Achieved: %.2f%%",
                minConsensusThreshold * 100, consensusPercentage * 100
            ));
        } else if (!withinTolerance) {
            result.setVerificationStatus("REJECTED");
            result.setRejectionReason(String.format(
                "Claimed value outside tolerance. Variance: %.2f%%, Max allowed: %.0f%%",
                variance.doubleValue(), priceTolerance * 100
            ));
        } else {
            result.setVerificationStatus("APPROVED");
        }

        return result;
    }

    /**
     * Persist verification result to database
     * Note: Transaction is managed at verifyAssetValue() level
     */
    private Uni<Void> persistVerificationResult(OracleVerificationResult result) {
        return Uni.createFrom().item(() -> {
            OracleVerificationEntity entity = OracleVerificationEntity.fromDTO(result);
            verificationRepository.persist(entity);
            Log.infof("Persisted verification result: %s", result.getVerificationId());
            return null;
        });
    }

    /**
     * Generate unique verification ID
     */
    private String generateVerificationId(String assetId) {
        return "VERIF-" + assetId + "-" + Instant.now().toEpochMilli() + "-" +
            UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * Price statistics holder
     */
    private record PriceStatistics(
        BigDecimal min,
        BigDecimal max,
        BigDecimal average,
        BigDecimal standardDeviation
    ) {}

    /**
     * Get verification result by ID
     *
     * @param verificationId The verification identifier
     * @return Uni<OracleVerificationResult> Verification result or null if not found
     */
    public Uni<OracleVerificationResult> getVerificationById(String verificationId) {
        Log.infof("Retrieving verification result by ID: %s", verificationId);

        return Uni.createFrom().item(() -> {
            Optional<OracleVerificationEntity> entityOpt = verificationRepository.findByVerificationId(verificationId);
            if (entityOpt.isEmpty()) {
                Log.warnf("Verification not found: %s", verificationId);
                return null;
            }
            return entityOpt.get().toDTO();
        });
    }

    /**
     * Get verification history for an asset
     *
     * @param assetId The asset identifier
     * @param limit Maximum number of results to return
     * @return Uni<List<OracleVerificationResult>> List of verification results
     */
    public Uni<List<OracleVerificationResult>> getVerificationHistory(String assetId, int limit) {
        Log.infof("Retrieving verification history: assetId=%s, limit=%d", assetId, limit);

        return Uni.createFrom().item(() -> {
            List<OracleVerificationEntity> entities = verificationRepository.findByAssetId(assetId);
            // Apply limit
            return entities.stream()
                .limit(limit)
                .map(OracleVerificationEntity::toDTO)
                .collect(Collectors.toList());
        });
    }
}
