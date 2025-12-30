package io.aurigraph.v11.tokenization.fractionalization;

import io.aurigraph.v11.tokenization.fractionalization.models.FractionalAsset;
import io.aurigraph.v11.tokenization.fractionalization.models.FractionHolder;
import io.quarkus.logging.Log;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Core Fractionalization Service
 * Creates and manages fractionalized assets
 *
 * Performance Targets:
 * - Fractionalization creation: <10 seconds
 * - Merkle proof generation: <2 seconds
 * - Contract deployment: <3 seconds
 *
 * @author Backend Development Agent (BDA)
 * @since Phase 1 Foundation - Week 1-2
 */
@ApplicationScoped
public class FractionalizationService {

    @Inject
    PrimaryTokenService primaryTokenService;

    @Inject
    BreakingChangeDetector changeDetector;

    // Asset registry (in-memory for Phase 1)
    private final Map<String, FractionalAsset> assetRegistry = new ConcurrentHashMap<>();
    private final Map<String, Map<String, FractionHolder>> holderRegistry = new ConcurrentHashMap<>();

    /**
     * Fractionalize an asset
     *
     * @param assetId Asset ID
     * @param assetType Asset type
     * @param assetValue Initial asset value
     * @param totalFractions Total fractions to create
     * @param distributionModel Distribution model
     * @param ownerAddress Owner address
     * @return Fractionalization result
     */
    public Uni<FractionalizationResult> fractionalizeAsset(
            String assetId,
            String assetType,
            String description,
            BigDecimal assetValue,
            long totalFractions,
            FractionalAsset.DistributionModel distributionModel,
            FractionalAsset.RevaluationConfig revaluationConfig,
            FractionalAsset.CustodyInfo custodyInfo,
            String ownerAddress) {

        return Uni.createFrom().item(() -> {
            long startTime = System.nanoTime();

            // Validate inputs
            validateFractionalizationRequest(assetId, assetValue, totalFractions);

            // Phase 1: Create primary token (immutable reference)
            long primaryTokenStart = System.nanoTime();
            String primaryTokenId = primaryTokenService.createPrimaryToken(
                assetId, assetType, assetValue);
            long primaryTokenEnd = System.nanoTime();

            // Phase 2: Deploy fractionalization contract
            long contractStart = System.nanoTime();
            String contractAddress = deployFractionalizationContract(
                primaryTokenId, totalFractions);
            long contractEnd = System.nanoTime();

            // Phase 3: Generate Merkle proof
            long merkleStart = System.nanoTime();
            String merkleRoot = generateAssetMerkleProof(
                assetId, assetValue, totalFractions);
            long merkleEnd = System.nanoTime();

            // Calculate fraction value
            BigDecimal fractionValue = assetValue.divide(
                BigDecimal.valueOf(totalFractions), 8, RoundingMode.HALF_UP);

            // Create fractional asset
            FractionalAsset asset = FractionalAsset.builder()
                .primaryTokenId(primaryTokenId)
                .assetId(assetId)
                .assetType(assetType)
                .description(description)
                .initialValuation(assetValue)
                .currentValuation(assetValue)
                .totalFractions(totalFractions)
                .fractionValue(fractionValue)
                .contractAddress(contractAddress)
                .distributionModel(distributionModel)
                .revaluationConfig(revaluationConfig)
                .custodyInfo(custodyInfo)
                .merkleRoot(merkleRoot)
                .createdAt(Instant.now())
                .lastValuationUpdate(Instant.now())
                .ownerAddress(ownerAddress)
                .status(FractionalAsset.FractionalizationStatus.ACTIVE)
                .valuationHistory(new ArrayList<>())
                .build();

            // Register asset
            assetRegistry.put(primaryTokenId, asset);

            // Initialize holder registry with owner
            Map<String, FractionHolder> holders = new ConcurrentHashMap<>();
            FractionHolder owner = FractionHolder.builder()
                .holderAddress(ownerAddress)
                .fractionCount(totalFractions)
                .ownershipPercentage(BigDecimal.valueOf(100))
                .holdingValue(assetValue)
                .totalDividendsReceived(BigDecimal.ZERO)
                .acquisitionDate(Instant.now())
                .tierLevel(FractionHolder.TierLevel.fromFractionCount(totalFractions))
                .governanceScore(100)
                .impactScore(BigDecimal.valueOf(100))
                .holdingDurationDays(0L)
                .build();

            holders.put(ownerAddress, owner);
            holderRegistry.put(primaryTokenId, holders);

            long totalTime = System.nanoTime() - startTime;

            Log.infof("Fractionalized asset %s into %d fractions (value: %s per fraction) in %.2f ms",
                assetId, totalFractions, fractionValue, totalTime / 1_000_000.0);

            FractionalizationMetrics metrics = FractionalizationMetrics.builder()
                .primaryTokenTimeNanos(primaryTokenEnd - primaryTokenStart)
                .contractDeploymentTimeNanos(contractEnd - contractStart)
                .merkleGenerationTimeNanos(merkleEnd - merkleStart)
                .totalTimeNanos(totalTime)
                .build();

            return FractionalizationResult.builder()
                .success(true)
                .asset(asset)
                .primaryTokenId(primaryTokenId)
                .contractAddress(contractAddress)
                .merkleRoot(merkleRoot)
                .fractionValue(fractionValue)
                .metrics(metrics)
                .message("Asset successfully fractionalized")
                .build();

        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    /**
     * Transfer fractions between holders
     */
    public Uni<TransferResult> transferFractions(
            String primaryTokenId,
            String fromAddress,
            String toAddress,
            long fractionCount,
            BigDecimal pricePerFraction) {

        return Uni.createFrom().item(() -> {
            FractionalAsset asset = assetRegistry.get(primaryTokenId);
            if (asset == null) {
                throw new AssetNotFoundException("Asset not found: " + primaryTokenId);
            }

            Map<String, FractionHolder> holders = holderRegistry.get(primaryTokenId);
            if (holders == null) {
                throw new IllegalStateException("Holder registry not initialized");
            }

            // Validate transfer
            FractionHolder fromHolder = holders.get(fromAddress);
            if (fromHolder == null || fromHolder.getFractionCount() < fractionCount) {
                throw new InsufficientFractionsException(
                    "Insufficient fractions for transfer");
            }

            // Execute transfer
            fromHolder.removeFractions(fractionCount, asset.getFractionValue());

            FractionHolder toHolder = holders.get(toAddress);
            if (toHolder == null) {
                toHolder = FractionHolder.builder()
                    .holderAddress(toAddress)
                    .fractionCount(0)
                    .ownershipPercentage(BigDecimal.ZERO)
                    .holdingValue(BigDecimal.ZERO)
                    .totalDividendsReceived(BigDecimal.ZERO)
                    .acquisitionDate(Instant.now())
                    .tierLevel(FractionHolder.TierLevel.TIER_1)
                    .governanceScore(0)
                    .impactScore(BigDecimal.ZERO)
                    .holdingDurationDays(0L)
                    .build();
                holders.put(toAddress, toHolder);
            }

            toHolder.addFractions(fractionCount, asset.getFractionValue());

            // Update ownership percentages
            updateOwnershipPercentages(primaryTokenId, holders, asset.getTotalFractions());

            Log.infof("Transferred %d fractions from %s to %s (asset: %s)",
                fractionCount, fromAddress, toAddress, primaryTokenId);

            return TransferResult.builder()
                .success(true)
                .primaryTokenId(primaryTokenId)
                .fromAddress(fromAddress)
                .toAddress(toAddress)
                .fractionCount(fractionCount)
                .pricePerFraction(pricePerFraction)
                .message("Transfer completed successfully")
                .build();

        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    /**
     * Get asset by primary token ID
     */
    public Uni<Optional<FractionalAsset>> getAsset(String primaryTokenId) {
        return Uni.createFrom().item(() -> Optional.ofNullable(assetRegistry.get(primaryTokenId)))
            .runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    /**
     * Get holder information
     */
    @SuppressWarnings("unchecked")
    public Uni<Optional<FractionHolder>> getHolder(String primaryTokenId, String holderAddress) {
        return (Uni<Optional<FractionHolder>>) (Uni<?>) Uni.createFrom().item(() -> {
            Map<String, FractionHolder> holders = holderRegistry.get(primaryTokenId);
            if (holders == null) {
                return Optional.empty();
            }
            return Optional.ofNullable(holders.get(holderAddress));
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    /**
     * Get all holders for asset
     */
    @SuppressWarnings("unchecked")
    public Uni<List<FractionHolder>> getAllHolders(String primaryTokenId) {
        return (Uni<List<FractionHolder>>) (Uni<?>) Uni.createFrom().item(() -> {
            Map<String, FractionHolder> holders = holderRegistry.get(primaryTokenId);
            if (holders == null) {
                return new ArrayList<>();
            }
            List<FractionHolder> holderList = new ArrayList<>(holders.values());
            return holderList;
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    // Private helper methods

    private void validateFractionalizationRequest(
            String assetId, BigDecimal assetValue, long totalFractions) {

        if (assetId == null || assetId.trim().isEmpty()) {
            throw new IllegalArgumentException("Asset ID cannot be null or empty");
        }

        if (assetValue == null || assetValue.compareTo(BigDecimal.valueOf(1_000_000)) < 0) {
            throw new IllegalArgumentException(
                "Asset value must be at least $1M for fractionalization");
        }

        if (totalFractions < 2 || totalFractions > 10_000_000) {
            throw new IllegalArgumentException(
                "Total fractions must be between 2 and 10M");
        }

        // Check if asset already fractionalized
        boolean exists = assetRegistry.values().stream()
            .anyMatch(a -> a.getAssetId().equals(assetId));

        if (exists) {
            throw new IllegalStateException("Asset already fractionalized: " + assetId);
        }
    }

    private String deployFractionalizationContract(String primaryTokenId, long totalFractions) {
        // Generate deterministic contract address
        String contractData = primaryTokenId + ":" + totalFractions;
        return "CONTRACT-" + UUID.nameUUIDFromBytes(contractData.getBytes()).toString();
    }

    private String generateAssetMerkleProof(String assetId, BigDecimal value, long fractions) {
        try {
            String assetData = String.format("%s|%s|%d",
                assetId, value.toPlainString(), fractions);
            MessageDigest digest = MessageDigest.getInstance("SHA3-256");
            byte[] hashBytes = digest.digest(assetData.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to generate Merkle proof", e);
        }
    }

    private void updateOwnershipPercentages(
            String primaryTokenId, Map<String, FractionHolder> holders, long totalFractions) {

        holders.values().forEach(holder -> {
            BigDecimal percentage = BigDecimal.valueOf(holder.getFractionCount())
                .divide(BigDecimal.valueOf(totalFractions), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
            holder.setOwnershipPercentage(percentage);
        });
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    // Supporting classes

    /**
     * Fractionalization result
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class FractionalizationResult {
        private boolean success;
        private FractionalAsset asset;
        private String primaryTokenId;
        private String contractAddress;
        private String merkleRoot;
        private BigDecimal fractionValue;
        private FractionalizationMetrics metrics;
        private String message;
    }

    /**
     * Fractionalization performance metrics
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class FractionalizationMetrics {
        private long primaryTokenTimeNanos;
        private long contractDeploymentTimeNanos;
        private long merkleGenerationTimeNanos;
        private long totalTimeNanos;

        public double getTotalTimeMs() {
            return totalTimeNanos / 1_000_000.0;
        }

        public boolean meetsTarget() {
            // Target: <10 seconds
            return getTotalTimeMs() < 10000.0;
        }
    }

    /**
     * Transfer result
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class TransferResult {
        private boolean success;
        private String primaryTokenId;
        private String fromAddress;
        private String toAddress;
        private long fractionCount;
        private BigDecimal pricePerFraction;
        private String message;
    }

    // Exceptions

    public static class AssetNotFoundException extends RuntimeException {
        public AssetNotFoundException(String message) {
            super(message);
        }
    }

    public static class InsufficientFractionsException extends RuntimeException {
        public InsufficientFractionsException(String message) {
            super(message);
        }
    }
}
