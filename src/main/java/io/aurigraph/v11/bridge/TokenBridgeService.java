package io.aurigraph.v11.bridge;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Interface for cross-chain token bridging operations in Aurigraph V11.
 * 
 * This service provides secure and efficient token bridging capabilities between
 * different blockchain networks, supporting various token standards and ensuring
 * atomic operations with proper validation and security measures.
 * 
 * Key Features:
 * - Multi-token standard support (ERC-20, ERC-721, ERC-1155, etc.)
 * - Atomic cross-chain token transfers with rollback capabilities
 * - Liquidity pool management for efficient bridging
 * - Automated market making for token conversions
 * - Fee optimization and cost estimation
 * - Multi-signature security and validator consensus
 * 
 * Performance Requirements:
 * - Process 50K+ token bridge operations per day
 * - Sub-10-minute bridge completion times
 * - 99.9% bridge success rate with automatic rollback
 * - Support for 100+ token types across 20+ chains
 * 
 * @author Aurigraph DLT Platform
 * @version 11.0.0
 * @since 2025-01-01
 */
@ApplicationScoped
public interface TokenBridgeService {

    /**
     * Initiates a token bridge operation from source to target chain.
     * 
     * @param bridgeRequest the token bridge request details
     * @return Uni containing the bridge initiation result
     */
    Uni<BridgeInitiationResult> initiateTokenBridge(TokenBridgeRequest bridgeRequest);

    /**
     * Completes a token bridge operation on the target chain.
     * 
     * @param bridgeId the identifier of the bridge operation
     * @param completionProof proof of successful source chain lock/burn
     * @return Uni containing the bridge completion result
     */
    Uni<BridgeCompletionResult> completeBridge(String bridgeId, BridgeCompletionProof completionProof);

    /**
     * Rolls back a failed or cancelled bridge operation.
     * 
     * @param bridgeId the identifier of the bridge operation
     * @param rollbackReason the reason for rollback
     * @return Uni containing the rollback result
     */
    Uni<BridgeRollbackResult> rollbackBridge(String bridgeId, String rollbackReason);

    /**
     * Gets the current status of a token bridge operation.
     * 
     * @param bridgeId the identifier of the bridge operation
     * @return Uni containing the current bridge status
     */
    Uni<BridgeStatus> getBridgeStatus(String bridgeId);

    /**
     * Tracks multiple bridge operations with real-time updates.
     * 
     * @param bridgeIds the list of bridge identifiers to track
     * @return Multi streaming status updates for tracked bridges
     */
    Multi<BridgeStatusUpdate> trackBridges(List<String> bridgeIds);

    /**
     * Estimates the cost and time for a token bridge operation.
     * 
     * @param bridgeRequest the proposed bridge request
     * @return Uni containing cost and time estimates
     */
    Uni<BridgeEstimate> estimateBridge(TokenBridgeRequest bridgeRequest);

    /**
     * Validates a token bridge request before execution.
     * 
     * @param bridgeRequest the bridge request to validate
     * @param validationLevel the level of validation to perform
     * @return Uni containing the validation result
     */
    Uni<BridgeValidationResult> validateBridgeRequest(
        TokenBridgeRequest bridgeRequest,
        ValidationLevel validationLevel
    );

    /**
     * Gets available bridging routes for a token pair.
     * 
     * @param sourceChain the source blockchain identifier
     * @param targetChain the target blockchain identifier
     * @param tokenAddress the token address on source chain
     * @return Multi streaming available bridging routes
     */
    Multi<BridgingRoute> getAvailableRoutes(String sourceChain, String targetChain, String tokenAddress);

    /**
     * Adds liquidity to a bridge liquidity pool.
     * 
     * @param poolId the liquidity pool identifier
     * @param liquidityAmount the amount of liquidity to add
     * @param tokenAddress the token address for liquidity
     * @param provider the liquidity provider address
     * @return Uni containing the liquidity addition result
     */
    Uni<LiquidityAdditionResult> addLiquidity(
        String poolId,
        BigDecimal liquidityAmount,
        String tokenAddress,
        String provider
    );

    /**
     * Removes liquidity from a bridge liquidity pool.
     * 
     * @param poolId the liquidity pool identifier
     * @param liquidityAmount the amount of liquidity to remove
     * @param provider the liquidity provider address
     * @return Uni containing the liquidity removal result
     */
    Uni<LiquidityRemovalResult> removeLiquidity(
        String poolId,
        BigDecimal liquidityAmount,
        String provider
    );

    /**
     * Gets information about bridge liquidity pools.
     * 
     * @param chainPair the source and target chain pair
     * @param tokenAddress the token address (null for all tokens)
     * @return Multi streaming liquidity pool information
     */
    Multi<BridgeLiquidityPool> getLiquidityPools(ChainPair chainPair, String tokenAddress);

    /**
     * Configures bridge parameters and security settings.
     * 
     * @param chainId the blockchain identifier
     * @param bridgeConfig configuration for the bridge
     * @return Uni indicating success or failure of configuration
     */
    Uni<Boolean> configureBridge(String chainId, BridgeConfiguration bridgeConfig);

    /**
     * Sets up automated market making for token conversions.
     * 
     * @param ammConfig configuration for automated market making
     * @return Uni containing the AMM setup result
     */
    Uni<AMMSetupResult> setupAutomatedMarketMaking(AMMConfiguration ammConfig);

    /**
     * Monitors bridge performance and security metrics.
     * 
     * @param monitoringInterval the interval between monitoring updates
     * @return Multi streaming bridge performance metrics
     */
    Multi<BridgeMetrics> monitorBridge(Duration monitoringInterval);

    /**
     * Gets historical bridge statistics for analysis.
     * 
     * @param chainPair the source and target chain pair
     * @param fromTimestamp start timestamp for statistics
     * @param toTimestamp end timestamp for statistics
     * @return Uni containing historical bridge statistics
     */
    Uni<BridgeStatistics> getBridgeStatistics(
        ChainPair chainPair,
        long fromTimestamp,
        long toTimestamp
    );

    /**
     * Registers a new token for cross-chain bridging.
     * 
     * @param tokenRegistration the token registration details
     * @return Uni containing the token registration result
     */
    Uni<TokenRegistrationResult> registerToken(TokenRegistration tokenRegistration);

    /**
     * Updates bridge validator set for multi-signature security.
     * 
     * @param chainId the blockchain identifier
     * @param validatorSet the new validator set configuration
     * @return Uni containing the validator update result
     */
    Uni<ValidatorUpdateResult> updateValidators(String chainId, ValidatorSet validatorSet);

    // Inner classes and enums for data transfer objects

    /**
     * Request for token bridge operation.
     */
    public static class TokenBridgeRequest {
        public String bridgeId;
        public String sourceChain;
        public String targetChain;
        public String tokenAddress;
        public TokenStandard tokenStandard;
        public BigDecimal amount;
        public String fromAddress;
        public String toAddress;
        public BridgeMode bridgeMode;
        public Map<String, String> metadata;
        public Duration timeout;
        public BigDecimal maxFee;

        public TokenBridgeRequest(String sourceChain, String targetChain, String tokenAddress) {
            this.bridgeId = generateBridgeId();
            this.sourceChain = sourceChain;
            this.targetChain = targetChain;
            this.tokenAddress = tokenAddress;
            this.bridgeMode = BridgeMode.LOCK_AND_MINT;
            this.timeout = Duration.ofMinutes(30);
        }

        private String generateBridgeId() {
            return "bridge_" + System.nanoTime() + "_" + Math.random();
        }
    }

    /**
     * Supported token standards.
     */
    public enum TokenStandard {
        ERC20,
        ERC721,
        ERC1155,
        BEP20,
        SPL_TOKEN,
        NATIVE,
        CUSTOM
    }

    /**
     * Bridge operation modes.
     */
    public enum BridgeMode {
        LOCK_AND_MINT,      // Lock on source, mint on target
        BURN_AND_MINT,      // Burn on source, mint on target
        POOL_BASED,         // Use liquidity pools
        ATOMIC_SWAP,        // Atomic cross-chain swap
        WRAPPED_TOKEN       // Create wrapped token representation
    }

    /**
     * Result of bridge initiation.
     */
    public static class BridgeInitiationResult {
        public String bridgeId;
        public String lockTransactionHash;
        public BridgeOperation bridgeStatus;
        public long estimatedCompletionTime;
        public BigDecimal actualFee;
        public String proofHash;
        public boolean success;
        public String errorMessage;
    }

    /**
     * Bridge operation status.
     */
    public enum BridgeOperation {
        INITIATED,
        LOCKING_TOKENS,
        TOKENS_LOCKED,
        GENERATING_PROOF,
        PROOF_GENERATED,
        VALIDATING_PROOF,
        MINTING_TOKENS,
        COMPLETED,
        FAILED,
        ROLLED_BACK
    }

    /**
     * Proof of bridge completion for target chain.
     */
    public static class BridgeCompletionProof {
        public String bridgeId;
        public String lockTransactionHash;
        public String merkleRoot;
        public List<String> merkleProof;
        public Map<String, String> validatorSignatures;
        public String proofHash;
        public long blockHeight;
        public String blockHash;
    }

    /**
     * Result of bridge completion.
     */
    public static class BridgeCompletionResult {
        public String bridgeId;
        public String mintTransactionHash;
        public BigDecimal mintedAmount;
        public String targetTokenAddress;
        public boolean success;
        public String errorMessage;
    }

    /**
     * Result of bridge rollback operation.
     */
    public static class BridgeRollbackResult {
        public String bridgeId;
        public String unlockTransactionHash;
        public BigDecimal refundedAmount;
        public BigDecimal refundFee;
        public boolean success;
        public String errorMessage;
    }

    /**
     * Current status of a bridge operation.
     */
    public static class BridgeStatus {
        public String bridgeId;
        public BridgeOperation currentOperation;
        public double progressPercentage; // 0.0 to 100.0
        public long estimatedTimeRemaining;
        public List<BridgeEvent> events;
        public String currentTransactionHash;
        public Map<String, Object> statusData;
    }

    /**
     * Events in bridge operation lifecycle.
     */
    public static class BridgeEvent {
        public String eventType;
        public long timestamp;
        public String chainId;
        public String transactionHash;
        public String description;
        public Map<String, Object> eventData;
    }

    /**
     * Status update for tracked bridges.
     */
    public static class BridgeStatusUpdate {
        public String bridgeId;
        public BridgeOperation oldOperation;
        public BridgeOperation newOperation;
        public long updateTimestamp;
        public String updateReason;
        public Map<String, Object> additionalInfo;
    }

    /**
     * Cost and time estimates for bridging.
     */
    public static class BridgeEstimate {
        public BigDecimal estimatedFee;
        public BigDecimal gasCost;
        public BigDecimal validatorFee;
        public Duration estimatedTime;
        public Duration minTime;
        public Duration maxTime;
        public List<String> feeBreakdown;
        public double successProbability; // 0.0 to 1.0
    }

    /**
     * Validation levels for bridge requests.
     */
    public enum ValidationLevel {
        BASIC,          // Basic format and balance validation
        STANDARD,       // Standard + token contract validation
        STRICT,         // Strict + compliance and security checks
        COMPREHENSIVE   // Comprehensive + liquidity and route validation
    }

    /**
     * Result of bridge request validation.
     */
    public static class BridgeValidationResult {
        public boolean isValid;
        public List<ValidationError> errors;
        public List<ValidationWarning> warnings;
        public BridgeEstimate estimate;
        public List<String> recommendations;
    }

    /**
     * Validation error information.
     */
    public static class ValidationError {
        public String errorCode;
        public String errorMessage;
        public String fieldName;
        public String suggestedFix;
        public ErrorSeverity severity;
    }

    /**
     * Severity levels for validation errors.
     */
    public enum ErrorSeverity {
        LOW,
        MEDIUM,
        HIGH,
        CRITICAL
    }

    /**
     * Validation warning information.
     */
    public static class ValidationWarning {
        public String warningCode;
        public String warningMessage;
        public String recommendation;
        public double riskLevel; // 0.0 to 1.0
    }

    /**
     * Available route for token bridging.
     */
    public static class BridgingRoute {
        public String routeId;
        public String sourceChain;
        public String targetChain;
        public String tokenAddress;
        public String targetTokenAddress;
        public BridgeMode bridgeMode;
        public BigDecimal minimumAmount;
        public BigDecimal maximumAmount;
        public BigDecimal baseFee;
        public double feePercentage;
        public Duration averageTime;
        public double reliability; // 0.0 to 1.0
        public List<String> intermediateSteps;
    }

    /**
     * Source and target chain pair.
     */
    public static class ChainPair {
        public String sourceChain;
        public String targetChain;

        public ChainPair(String sourceChain, String targetChain) {
            this.sourceChain = sourceChain;
            this.targetChain = targetChain;
        }
    }

    /**
     * Result of liquidity addition.
     */
    public static class LiquidityAdditionResult {
        public String poolId;
        public String transactionHash;
        public BigDecimal liquidityAdded;
        public BigDecimal lpTokensReceived;
        public BigDecimal poolSharePercentage;
        public boolean success;
        public String errorMessage;
    }

    /**
     * Result of liquidity removal.
     */
    public static class LiquidityRemovalResult {
        public String poolId;
        public String transactionHash;
        public BigDecimal liquidityRemoved;
        public BigDecimal lpTokensBurned;
        public BigDecimal tokensReceived;
        public boolean success;
        public String errorMessage;
    }

    /**
     * Bridge liquidity pool information.
     */
    public static class BridgeLiquidityPool {
        public String poolId;
        public String sourceChain;
        public String targetChain;
        public String tokenAddress;
        public BigDecimal totalLiquidity;
        public BigDecimal availableLiquidity;
        public double utilizationRate;
        public double apy; // Annual percentage yield for liquidity providers
        public int liquidityProvidersCount;
        public BigDecimal volume24h;
        public List<String> supportedOperations;
    }

    /**
     * Bridge configuration settings.
     */
    public static class BridgeConfiguration {
        public BigDecimal minBridgeAmount;
        public BigDecimal maxBridgeAmount;
        public BigDecimal baseFee;
        public double feePercentage;
        public Duration operationTimeout;
        public int requiredConfirmations;
        public List<String> supportedTokens;
        public SecuritySettings securitySettings;
    }

    /**
     * Security settings for bridge operations.
     */
    public static class SecuritySettings {
        public int requiredValidators;
        public double consensusThreshold; // 0.0 to 1.0
        public Duration challengePeriod;
        public boolean enablePauseGuard;
        public List<String> authorizedOperators;
        public BigDecimal maxDailyVolume;
    }

    /**
     * Configuration for automated market making.
     */
    public static class AMMConfiguration {
        public String poolId;
        public String tokenA;
        public String tokenB;
        public BigDecimal initialLiquidityA;
        public BigDecimal initialLiquidityB;
        public double feePercentage;
        public AMMType ammType;
        public Map<String, Object> parameters;
    }

    /**
     * Types of automated market makers.
     */
    public enum AMMType {
        CONSTANT_PRODUCT,   // x * y = k
        STABLE_SWAP,        // For similar assets
        WEIGHTED,           // Balancer-style weighted pools
        CONCENTRATED,       // Concentrated liquidity
        HYBRID              // Hybrid AMM model
    }

    /**
     * Result of AMM setup.
     */
    public static class AMMSetupResult {
        public String poolId;
        public String poolAddress;
        public AMMType ammType;
        public BigDecimal initialPrice;
        public boolean success;
        public String errorMessage;
    }

    /**
     * Real-time bridge performance metrics.
     */
    public static class BridgeMetrics {
        public long timestamp;
        public double bridgeOperationsPerHour;
        public double averageBridgeTime;
        public double bridgeSuccessRate;
        public BigDecimal totalVolumeUSD;
        public Map<String, Long> operationsByChain;
        public Map<BridgeMode, Long> operationsByMode;
        public List<String> activeIssues;
        public BridgeHealth bridgeHealth;
    }

    /**
     * Bridge system health status.
     */
    public enum BridgeHealth {
        HEALTHY,
        DEGRADED,
        CRITICAL,
        MAINTENANCE
    }

    /**
     * Historical bridge statistics.
     */
    public static class BridgeStatistics {
        public ChainPair chainPair;
        public long totalOperations;
        public long successfulOperations;
        public long failedOperations;
        public double averageTime;
        public double successRate;
        public BigDecimal totalVolume;
        public BigDecimal totalFees;
        public Map<String, Long> tokenBreakdown;
        public List<CommonFailureReason> failureReasons;
        public long statisticsTimeWindow;
    }

    /**
     * Common reasons for bridge failures.
     */
    public static class CommonFailureReason {
        public String reason;
        public long count;
        public double percentage;
        public String mitigation;
    }

    /**
     * Token registration for cross-chain bridging.
     */
    public static class TokenRegistration {
        public String tokenAddress;
        public String chainId;
        public TokenStandard tokenStandard;
        public String tokenName;
        public String tokenSymbol;
        public int decimals;
        public List<String> targetChains;
        public Map<String, String> targetTokenAddresses;
        public BridgeMode preferredBridgeMode;
        public Map<String, Object> tokenMetadata;
    }

    /**
     * Result of token registration.
     */
    public static class TokenRegistrationResult {
        public String tokenAddress;
        public boolean registered;
        public Map<String, String> bridgeAddresses;
        public List<String> supportedChains;
        public String errorMessage;
    }

    /**
     * Validator set configuration.
     */
    public static class ValidatorSet {
        public List<ValidatorInfo> validators;
        public int requiredSignatures;
        public double consensusThreshold;
        public Duration rotationPeriod;
        public boolean enableSlashing;
    }

    /**
     * Information about bridge validators.
     */
    public static class ValidatorInfo {
        public String validatorAddress;
        public String publicKey;
        public BigDecimal stake;
        public double reputation; // 0.0 to 1.0
        public boolean isActive;
        public long joinedTimestamp;
    }

    /**
     * Result of validator set update.
     */
    public static class ValidatorUpdateResult {
        public String chainId;
        public boolean updated;
        public int newValidatorCount;
        public List<String> addedValidators;
        public List<String> removedValidators;
        public String errorMessage;
    }
}