package io.aurigraph.v11.bridge;

import io.aurigraph.v11.bridge.models.ValidationRequest;
import io.aurigraph.v11.bridge.models.ValidationResponse;
import io.aurigraph.v11.bridge.security.SignatureVerificationService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;

/**
 * Bridge Validator Service
 * Validates cross-chain bridge transactions including signatures, liquidity, and fees
 *
 * @author Backend Development Agent (BDA)
 * @version 1.0
 */
@ApplicationScoped
public class BridgeValidatorService {

    private static final Logger LOG = Logger.getLogger(BridgeValidatorService.class);

    // Rate limiting constants
    private static final int MAX_REQUESTS_PER_SECOND = 100;
    private static final Map<String, RequestTracker> requestTrackers = Collections.synchronizedMap(new LinkedHashMap<>());

    // Bridge configuration
    private static final Map<String, ChainConfig> CHAIN_CONFIGS = initializeChainConfigs();
    private static final Map<String, TokenConfig> TOKEN_CONFIGS = initializeTokenConfigs();

    @Inject
    SignatureVerificationService signatureVerifier;

    /**
     * Validate a bridge transaction request
     */
    public ValidationResponse validateBridgeTransaction(ValidationRequest request) {
        LOG.infof("Starting validation for bridge transaction: %s", request.getBridgeId());

        // Create response
        ValidationResponse.ValidationResponseBuilder responseBuilder = ValidationResponse.builder()
                .validationId(UUID.randomUUID().toString())
                .timestamp(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300)) // Valid for 5 minutes
                .nonce(request.getNonce());

        // Step 1: Basic request validation
        List<String> validationErrors = new ArrayList<>(request.getValidationErrors());
        if (!validationErrors.isEmpty()) {
            LOG.warnf("Request validation failed with %d errors", validationErrors.size());
            return responseBuilder
                    .status(ValidationResponse.ValidationStatus.FAILED)
                    .validationErrors(validationErrors)
                    .signatureValid(false)
                    .build();
        }

        List<String> validationWarnings = new ArrayList<>();

        // Step 2: Signature verification
        boolean signatureValid = verifySignature(request);
        if (!signatureValid) {
            validationErrors.add("Signature verification failed");
            LOG.warn("Signature verification failed for bridge transaction");
        }

        // Step 3: Liquidity check
        LiquidityCheckResult liquidityResult = checkLiquidity(request);
        if (!liquidityResult.available && request.getLiquidityCheckRequired() != Boolean.FALSE) {
            validationErrors.add("Insufficient liquidity: available=" + liquidityResult.availableLiquidity +
                               ", required=" + liquidityResult.requiredLiquidity);
        }

        // Step 4: Token support and decimals
        TokenValidationResult tokenResult = validateToken(request);
        if (!tokenResult.supported) {
            validationErrors.add("Token not supported on target chain");
        }
        if (!tokenResult.isAmountWithinLimits) {
            validationErrors.add("Amount outside acceptable range (min: " + tokenResult.minAmount +
                               ", max: " + tokenResult.maxAmount + ")");
        }

        // Step 5: Chain compatibility
        if (!isChainCompatible(request.getSourceChain(), request.getTargetChain())) {
            validationErrors.add("Source and target chains are not compatible");
        }

        // Step 6: Rate limiting
        ValidationResponse.RateLimitInfo rateLimitInfo = checkRateLimit(request.getSourceAddress());
        if (rateLimitInfo.getIsRateLimited() != null && rateLimitInfo.getIsRateLimited()) {
            validationErrors.add("Rate limit exceeded: " + rateLimitInfo.getResetTimeSeconds() + "s until reset");
        }

        // Step 7: Fee calculation
        FeeCalculationResult feeResult = calculateFees(request);

        // Step 8: Slippage estimation
        BigDecimal slippage = estimateSlippage(request);

        // Build warnings
        if (slippage.compareTo(BigDecimal.valueOf(2)) > 0) {
            validationWarnings.add("High slippage estimated: " + slippage.stripTrailingZeros() + "%");
        }
        if (request.getAmount().compareTo(liquidityResult.requiredLiquidity) > 0) {
            validationWarnings.add("Amount exceeds 50% of available liquidity");
        }

        // Determine overall status
        ValidationResponse.ValidationStatus status = validationErrors.isEmpty() ?
                (validationWarnings.isEmpty() ? ValidationResponse.ValidationStatus.SUCCESS :
                 ValidationResponse.ValidationStatus.WARNINGS) :
                ValidationResponse.ValidationStatus.FAILED;

        // Build validation details
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("signatureType", request.getSignatureType());
        details.put("sourceChain", request.getSourceChain());
        details.put("targetChain", request.getTargetChain());
        details.put("tokenSymbol", request.getTokenSymbol());
        details.put("exchangeRate", feeResult.exchangeRate);

        LOG.infof("Validation completed with status: %s (errors: %d, warnings: %d)",
                 status, validationErrors.size(), validationWarnings.size());

        return responseBuilder
                .status(status)
                .signatureValid(signatureValid)
                .liquidityAvailable(liquidityResult.available)
                .availableLiquidity(liquidityResult.availableLiquidity)
                .requiredLiquidity(liquidityResult.requiredLiquidity)
                .feeEstimate(feeResult.bridgeFee)
                .feeCurrency(request.getTokenSymbol())
                .gasFeeEstimate(feeResult.gasFee)
                .totalFeeEstimate(feeResult.totalFee)
                .estimatedTime(estimateTransactionTime(request))
                .exchangeRate(feeResult.exchangeRate)
                .sourceTokenDecimals(tokenResult.sourceDecimals)
                .targetTokenDecimals(tokenResult.targetDecimals)
                .sourceTokenBalance(tokenResult.userBalance)
                .rateLimitInfo(rateLimitInfo)  // Lombok builder method for rateLimitInfo field
                .chainCompatibility(isChainCompatible(request.getSourceChain(), request.getTargetChain()))
                .tokenSupported(tokenResult.supported)
                .amountWithinLimits(tokenResult.isAmountWithinLimits)
                .minTransferAmount(tokenResult.minAmount)
                .maxTransferAmount(tokenResult.maxAmount)
                .slippagePercentage(slippage)
                .validationErrors(validationErrors.isEmpty() ? null : validationErrors)
                .validationWarnings(validationWarnings.isEmpty() ? null : validationWarnings)
                .validationDetails(details)
                .build();
    }

    /**
     * Verify transaction signature
     */
    private boolean verifySignature(ValidationRequest request) {
        String dataToVerify = createSignableData(request);
        boolean verified = signatureVerifier.verifySignature(dataToVerify, request.getSignature(), request.getSignatureType());
        LOG.debugf("Signature verification: %s", verified ? "PASSED" : "FAILED");
        return verified;
    }

    /**
     * Create signable data from request
     */
    private String createSignableData(ValidationRequest request) {
        return String.format("%s|%s|%s|%s|%s|%s|%s|%d",
                request.getBridgeId(),
                request.getSourceChain(),
                request.getTargetChain(),
                request.getSourceAddress(),
                request.getTargetAddress(),
                request.getTokenSymbol(),
                request.getAmount().toPlainString(),
                request.getNonce() != null ? request.getNonce() : 0);
    }

    /**
     * Check liquidity availability
     */
    private LiquidityCheckResult checkLiquidity(ValidationRequest request) {
        ChainConfig targetConfig = CHAIN_CONFIGS.get(request.getTargetChain());
        BigDecimal availableLiquidity = targetConfig != null ?
                targetConfig.availableLiquidity : BigDecimal.valueOf(1_000_000);

        boolean available = availableLiquidity.compareTo(request.getAmount()) >= 0;
        return new LiquidityCheckResult(available, availableLiquidity, request.getAmount());
    }

    /**
     * Validate token support and settings
     */
    private TokenValidationResult validateToken(ValidationRequest request) {
        TokenConfig tokenConfig = TOKEN_CONFIGS.get(request.getTokenSymbol());

        if (tokenConfig == null) {
            return TokenValidationResult.notSupported();
        }

        boolean withinLimits = request.getAmount().compareTo(tokenConfig.minAmount) >= 0 &&
                              request.getAmount().compareTo(tokenConfig.maxAmount) <= 0;

        return new TokenValidationResult(
                true,
                tokenConfig.sourceDecimals,
                tokenConfig.targetDecimals,
                BigDecimal.valueOf(100_000), // Mock user balance
                tokenConfig.minAmount,
                tokenConfig.maxAmount,
                withinLimits
        );
    }

    /**
     * Check chain compatibility
     */
    private boolean isChainCompatible(String sourceChain, String targetChain) {
        // Supported chain pairs
        Set<String> supportedChains = Set.of("Ethereum", "Polygon", "BSC", "Avalanche", "Solana", "Aurigraph");
        return supportedChains.contains(sourceChain) && supportedChains.contains(targetChain);
    }

    /**
     * Check rate limiting
     */
    private ValidationResponse.RateLimitInfo checkRateLimit(String address) {
        RequestTracker tracker = requestTrackers.computeIfAbsent(address, k -> new RequestTracker());

        long now = System.currentTimeMillis();
        tracker.cleanOldRequests(now);

        boolean isRateLimited = tracker.requestCount >= MAX_REQUESTS_PER_SECOND;
        int resetTime = isRateLimited ? (int) ((tracker.resetTime - now) / 1000 + 1) : 0;

        if (!isRateLimited) {
            tracker.addRequest(now);
        }

        return ValidationResponse.RateLimitInfo.builder()
                .requestsPerSecond(MAX_REQUESTS_PER_SECOND)
                .remainingRequests(Math.max(0, MAX_REQUESTS_PER_SECOND - tracker.requestCount))
                .resetTimeSeconds(resetTime)
                .isRateLimited(isRateLimited)
                .build();
    }

    /**
     * Calculate bridge and gas fees
     */
    private FeeCalculationResult calculateFees(ValidationRequest request) {
        BigDecimal baseFeePercentage = BigDecimal.valueOf(0.001); // 0.1%
        BigDecimal bridgeFee = request.getAmount().multiply(baseFeePercentage).setScale(6, RoundingMode.HALF_UP);

        BigDecimal gasFee = request.getGasPrice() != null ?
                request.getGasPrice().multiply(BigDecimal.valueOf(21000)) :
                BigDecimal.valueOf(0.01);

        BigDecimal totalFee = bridgeFee.add(gasFee);
        BigDecimal exchangeRate = BigDecimal.ONE; // 1:1 for same token

        return new FeeCalculationResult(bridgeFee, gasFee, totalFee, exchangeRate);
    }

    /**
     * Estimate slippage
     */
    private BigDecimal estimateSlippage(ValidationRequest request) {
        // Simple slippage estimation based on amount
        BigDecimal percentageOfPool = request.getAmount().divide(BigDecimal.valueOf(1_000_000), 4, RoundingMode.HALF_UP);
        return percentageOfPool.multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Estimate transaction completion time
     */
    private long estimateTransactionTime(ValidationRequest request) {
        ChainConfig sourceConfig = CHAIN_CONFIGS.get(request.getSourceChain());
        ChainConfig targetConfig = CHAIN_CONFIGS.get(request.getTargetChain());

        long sourceTime = sourceConfig != null ? sourceConfig.avgBlockTime : 15000;
        long targetTime = targetConfig != null ? targetConfig.avgBlockTime : 15000;

        // Typical bridge: source block finality + bridge relay time + target block finality
        return sourceTime + 30000 + targetTime; // milliseconds
    }

    /**
     * Initialize supported chain configurations
     */
    private static Map<String, ChainConfig> initializeChainConfigs() {
        Map<String, ChainConfig> configs = new HashMap<>();
        configs.put("Ethereum", new ChainConfig(15000, BigDecimal.valueOf(500_000)));
        configs.put("Polygon", new ChainConfig(2000, BigDecimal.valueOf(800_000)));
        configs.put("BSC", new ChainConfig(3000, BigDecimal.valueOf(700_000)));
        configs.put("Avalanche", new ChainConfig(2000, BigDecimal.valueOf(600_000)));
        configs.put("Solana", new ChainConfig(400, BigDecimal.valueOf(900_000)));
        configs.put("Aurigraph", new ChainConfig(1000, BigDecimal.valueOf(1_000_000)));
        return configs;
    }

    /**
     * Initialize token configurations
     */
    private static Map<String, TokenConfig> initializeTokenConfigs() {
        Map<String, TokenConfig> configs = new HashMap<>();
        configs.put("ETH", new TokenConfig(18, 18, BigDecimal.valueOf(0.01), BigDecimal.valueOf(100)));
        configs.put("USDT", new TokenConfig(6, 6, BigDecimal.valueOf(100), BigDecimal.valueOf(1_000_000)));
        configs.put("USDC", new TokenConfig(6, 6, BigDecimal.valueOf(100), BigDecimal.valueOf(1_000_000)));
        configs.put("WBTC", new TokenConfig(8, 8, BigDecimal.valueOf(0.001), BigDecimal.valueOf(10)));
        configs.put("AUR", new TokenConfig(18, 18, BigDecimal.valueOf(1), BigDecimal.valueOf(10_000_000)));
        return configs;
    }

    // Helper classes
    static class ChainConfig {
        long avgBlockTime; // milliseconds
        BigDecimal availableLiquidity;

        ChainConfig(long avgBlockTime, BigDecimal availableLiquidity) {
            this.avgBlockTime = avgBlockTime;
            this.availableLiquidity = availableLiquidity;
        }
    }

    static class TokenConfig {
        int sourceDecimals;
        int targetDecimals;
        BigDecimal minAmount;
        BigDecimal maxAmount;

        TokenConfig(int sourceDecimals, int targetDecimals, BigDecimal minAmount, BigDecimal maxAmount) {
            this.sourceDecimals = sourceDecimals;
            this.targetDecimals = targetDecimals;
            this.minAmount = minAmount;
            this.maxAmount = maxAmount;
        }
    }

    static class LiquidityCheckResult {
        boolean available;
        BigDecimal availableLiquidity;
        BigDecimal requiredLiquidity;

        LiquidityCheckResult(boolean available, BigDecimal availableLiquidity, BigDecimal requiredLiquidity) {
            this.available = available;
            this.availableLiquidity = availableLiquidity;
            this.requiredLiquidity = requiredLiquidity;
        }
    }

    static class TokenValidationResult {
        boolean supported;
        int sourceDecimals;
        int targetDecimals;
        BigDecimal userBalance;
        BigDecimal minAmount;
        BigDecimal maxAmount;
        boolean isAmountWithinLimits;

        TokenValidationResult(boolean supported, int sourceDecimals, int targetDecimals,
                            BigDecimal userBalance, BigDecimal minAmount, BigDecimal maxAmount,
                            boolean isAmountWithinLimits) {
            this.supported = supported;
            this.sourceDecimals = sourceDecimals;
            this.targetDecimals = targetDecimals;
            this.userBalance = userBalance;
            this.minAmount = minAmount;
            this.maxAmount = maxAmount;
            this.isAmountWithinLimits = isAmountWithinLimits;
        }

        static TokenValidationResult notSupported() {
            return new TokenValidationResult(false, 0, 0, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, false);
        }
    }

    static class FeeCalculationResult {
        BigDecimal bridgeFee;
        BigDecimal gasFee;
        BigDecimal totalFee;
        BigDecimal exchangeRate;

        FeeCalculationResult(BigDecimal bridgeFee, BigDecimal gasFee, BigDecimal totalFee, BigDecimal exchangeRate) {
            this.bridgeFee = bridgeFee;
            this.gasFee = gasFee;
            this.totalFee = totalFee;
            this.exchangeRate = exchangeRate;
        }
    }

    static class RequestTracker {
        List<Long> requestTimes = Collections.synchronizedList(new ArrayList<>());
        int requestCount = 0;
        long resetTime = System.currentTimeMillis() + 1000;

        void cleanOldRequests(long now) {
            requestTimes.removeIf(time -> time < now - 1000);
            requestCount = requestTimes.size();
            if (requestCount >= MAX_REQUESTS_PER_SECOND) {
                resetTime = requestTimes.get(0) + 1000;
            }
        }

        void addRequest(long now) {
            requestTimes.add(now);
            requestCount++;
        }
    }
}
