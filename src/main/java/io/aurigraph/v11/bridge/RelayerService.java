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
 * Interface for cross-chain relayer operations in Aurigraph V11.
 * 
 * This service manages decentralized relayers that facilitate cross-chain
 * communication and transaction execution. Relayers are responsible for
 * monitoring source chains, generating proofs, and executing transactions
 * on target chains with economic incentives and penalty mechanisms.
 * 
 * Key Features:
 * - Decentralized relayer network management
 * - Economic incentive and penalty mechanisms
 * - Automated proof generation and verification
 * - Load balancing across relayer nodes
 * - Fraud detection and dispute resolution
 * - Performance monitoring and reputation scoring
 * 
 * Performance Requirements:
 * - Support 1000+ active relayers across networks
 * - Sub-5-minute cross-chain message relay times
 * - 99.9% relay success rate with automatic failover
 * - Economic security through staking and slashing
 * 
 * @author Aurigraph DLT Platform
 * @version 11.0.0
 * @since 2025-01-01
 */
@ApplicationScoped
public interface RelayerService {

    /**
     * Registers a new relayer in the network.
     * 
     * @param relayerRegistration the relayer registration details
     * @return Uni containing the registration result
     */
    Uni<RelayerRegistrationResult> registerRelayer(RelayerRegistration relayerRegistration);

    /**
     * Unregisters a relayer from the network.
     * 
     * @param relayerId the identifier of the relayer to unregister
     * @param unstakeTokens whether to unstake tokens immediately
     * @return Uni containing the unregistration result
     */
    Uni<RelayerUnregistrationResult> unregisterRelayer(String relayerId, boolean unstakeTokens);

    /**
     * Updates relayer configuration and parameters.
     * 
     * @param relayerId the identifier of the relayer
     * @param newConfig the new configuration for the relayer
     * @return Uni indicating success or failure of the update
     */
    Uni<Boolean> updateRelayerConfig(String relayerId, RelayerConfig newConfig);

    /**
     * Submits a cross-chain relay request to available relayers.
     * 
     * @param relayRequest the relay request details
     * @param selectionCriteria criteria for selecting relayers
     * @return Uni containing the relay submission result
     */
    Uni<RelaySubmissionResult> submitRelayRequest(
        RelayRequest relayRequest,
        RelayerSelectionCriteria selectionCriteria
    );

    /**
     * Gets the current status of a relay operation.
     * 
     * @param relayId the identifier of the relay operation
     * @return Uni containing the relay status
     */
    Uni<RelayStatus> getRelayStatus(String relayId);

    /**
     * Tracks multiple relay operations with real-time updates.
     * 
     * @param relayIds the list of relay identifiers to track
     * @return Multi streaming status updates for tracked relays
     */
    Multi<RelayStatusUpdate> trackRelays(List<String> relayIds);

    /**
     * Gets information about active relayers in the network.
     * 
     * @param chainPair optional filter by source and target chain pair
     * @return Multi streaming active relayer information
     */
    Multi<RelayerInfo> getActiveRelayers(ChainPair chainPair);

    /**
     * Calculates reputation scores for relayers based on performance.
     * 
     * @param timeWindow the time window for reputation calculation
     * @return Multi streaming relayer reputation scores
     */
    Multi<RelayerReputation> calculateReputationScores(Duration timeWindow);

    /**
     * Stakes tokens to become or remain an active relayer.
     * 
     * @param relayerId the identifier of the relayer
     * @param stakeAmount the amount of tokens to stake
     * @param stakeDuration the duration to lock the stake
     * @return Uni containing the staking result
     */
    Uni<StakingResult> stakeTokens(String relayerId, BigDecimal stakeAmount, Duration stakeDuration);

    /**
     * Unstakes tokens from a relayer after the lock period.
     * 
     * @param relayerId the identifier of the relayer
     * @param unstakeAmount the amount of tokens to unstake (null for all)
     * @return Uni containing the unstaking result
     */
    Uni<UnstakingResult> unstakeTokens(String relayerId, BigDecimal unstakeAmount);

    /**
     * Claims rewards earned from successful relay operations.
     * 
     * @param relayerId the identifier of the relayer
     * @param rewardType the type of rewards to claim
     * @return Uni containing the reward claim result
     */
    Uni<RewardClaimResult> claimRewards(String relayerId, RewardType rewardType);

    /**
     * Reports fraudulent or malicious behavior by a relayer.
     * 
     * @param fraudReport the fraud report with evidence
     * @return Uni containing the fraud report submission result
     */
    Uni<FraudReportResult> reportFraud(FraudReport fraudReport);

    /**
     * Initiates a dispute resolution process for contested operations.
     * 
     * @param dispute the dispute details and evidence
     * @return Uni containing the dispute initiation result
     */
    Uni<DisputeResult> initiateDispute(DisputeCase dispute);

    /**
     * Slashes stakes from relayers found guilty of malicious behavior.
     * 
     * @param relayerId the identifier of the relayer to slash
     * @param slashingReason the reason for slashing
     * @param slashingAmount the amount to slash
     * @return Uni containing the slashing result
     */
    Uni<SlashingResult> slashRelayer(String relayerId, String slashingReason, BigDecimal slashingAmount);

    /**
     * Monitors relayer network health and performance.
     * 
     * @param monitoringInterval the interval between monitoring updates
     * @return Multi streaming network health metrics
     */
    Multi<RelayerNetworkHealth> monitorNetworkHealth(Duration monitoringInterval);

    /**
     * Optimizes relayer selection and load distribution.
     * 
     * @param optimizationConfig configuration for optimization
     * @return Uni containing the optimization result
     */
    Uni<OptimizationResult> optimizeRelayerSelection(RelayerOptimizationConfig optimizationConfig);

    /**
     * Configures economic parameters for the relayer network.
     * 
     * @param economicParams the economic parameter configuration
     * @return Uni indicating success or failure of configuration
     */
    Uni<Boolean> configureEconomicParameters(EconomicParameters economicParams);

    /**
     * Gets historical statistics about relayer performance.
     * 
     * @param relayerId optional filter by specific relayer
     * @param fromTimestamp start timestamp for statistics
     * @param toTimestamp end timestamp for statistics
     * @return Uni containing historical relayer statistics
     */
    Uni<RelayerStatistics> getRelayerStatistics(
        String relayerId,
        long fromTimestamp,
        long toTimestamp
    );

    /**
     * Sets up automated relayer operations and maintenance.
     * 
     * @param automationConfig configuration for automated operations
     * @return Uni containing the automation setup result
     */
    Uni<AutomationSetupResult> setupAutomation(RelayerAutomationConfig automationConfig);

    // Inner classes and enums for data transfer objects

    /**
     * Registration information for a new relayer.
     */
    public static class RelayerRegistration {
        public String relayerAddress;
        public String operatorAddress;
        public List<String> supportedChains;
        public RelayerCapabilities capabilities;
        public RelayerConfig config;
        public BigDecimal initialStake;
        public String publicKey;
        public Map<String, String> metadata;
        public List<String> endorsements; // Endorsements from other relayers
    }

    /**
     * Capabilities that a relayer can provide.
     */
    public static class RelayerCapabilities {
        public boolean supportsProofGeneration;
        public boolean supportsEventMonitoring;
        public boolean supportsMultiChain;
        public boolean supportsHighFrequency;
        public List<String> supportedProtocols;
        public int maxConcurrentRelays;
        public Duration averageRelayTime;
        public double uptimePercentage;
    }

    /**
     * Configuration for a relayer.
     */
    public static class RelayerConfig {
        public Duration relayTimeout;
        public BigDecimal minRewardThreshold;
        public int maxRetries;
        public List<String> preferredChains;
        public boolean enableAutoStaking;
        public boolean enableAutoRewardClaim;
        public Map<String, Object> customSettings;
    }

    /**
     * Result of relayer registration.
     */
    public static class RelayerRegistrationResult {
        public String relayerId;
        public boolean registered;
        public BigDecimal stakedAmount;
        public String registrationTransactionHash;
        public List<String> assignedChains;
        public RelayerStatus status;
        public String errorMessage;
    }

    /**
     * Status of relayers in the network.
     */
    public enum RelayerStatus {
        PENDING,        // Registration pending
        ACTIVE,         // Active and available for relays
        INACTIVE,       // Temporarily inactive
        SLASHED,        // Slashed for malicious behavior
        JAILED,         // Temporarily banned
        WITHDRAWN       // Permanently withdrawn from network
    }

    /**
     * Result of relayer unregistration.
     */
    public static class RelayerUnregistrationResult {
        public String relayerId;
        public boolean unregistered;
        public BigDecimal unstakedAmount;
        public Duration cooldownPeriod;
        public String unregistrationTransactionHash;
        public String errorMessage;
    }

    /**
     * Cross-chain relay request.
     */
    public static class RelayRequest {
        public String requestId;
        public String sourceChain;
        public String targetChain;
        public String messageHash;
        public byte[] messageData;
        public String sourceTransactionHash;
        public RelayPriority priority;
        public BigDecimal maxReward;
        public Duration deadline;
        public Map<String, Object> metadata;

        public RelayRequest(String sourceChain, String targetChain, String messageHash) {
            this.requestId = generateRequestId();
            this.sourceChain = sourceChain;
            this.targetChain = targetChain;
            this.messageHash = messageHash;
            this.priority = RelayPriority.NORMAL;
        }

        private String generateRequestId() {
            return "relay_" + System.nanoTime() + "_" + Math.random();
        }
    }

    /**
     * Priority levels for relay requests.
     */
    public enum RelayPriority {
        LOW,
        NORMAL,
        HIGH,
        URGENT
    }

    /**
     * Criteria for selecting relayers.
     */
    public static class RelayerSelectionCriteria {
        public double minReputationScore;
        public BigDecimal minStakeAmount;
        public Duration maxResponseTime;
        public List<String> preferredRelayers;
        public List<String> excludedRelayers;
        public boolean requireMultipleRelayers;
        public int redundancyLevel; // Number of relayers for redundancy
        public SelectionStrategy strategy;
    }

    /**
     * Strategies for relayer selection.
     */
    public enum SelectionStrategy {
        REPUTATION_BASED,   // Select based on reputation scores
        STAKE_WEIGHTED,     // Select based on stake amount
        ROUND_ROBIN,        // Rotate among available relayers
        LOAD_BALANCED,      // Balance load across relayers
        COST_OPTIMIZED,     // Select cheapest available relayers
        RANDOM              // Random selection
    }

    /**
     * Result of relay submission.
     */
    public static class RelaySubmissionResult {
        public String relayId;
        public List<String> assignedRelayers;
        public BigDecimal totalReward;
        public Duration estimatedCompletionTime;
        public boolean submitted;
        public String errorMessage;
    }

    /**
     * Current status of a relay operation.
     */
    public static class RelayStatus {
        public String relayId;
        public RelayState state;
        public List<String> assignedRelayers;
        public String currentRelayer;
        public double progressPercentage;
        public Duration timeRemaining;
        public List<RelayEvent> events;
        public String errorMessage;
    }

    /**
     * States of relay operations.
     */
    public enum RelayState {
        SUBMITTED,          // Request submitted
        ASSIGNED,           // Relayers assigned
        MONITORING,         // Monitoring source chain
        PROOF_GENERATED,    // Proof generated
        EXECUTING,          // Executing on target chain
        COMPLETED,          // Successfully completed
        FAILED,             // Failed to complete
        DISPUTED,           // Under dispute
        SLASHED             // Relayer slashed for failure
    }

    /**
     * Events in relay operation lifecycle.
     */
    public static class RelayEvent {
        public String eventType;
        public long timestamp;
        public String relayerId;
        public String description;
        public Map<String, Object> eventData;
    }

    /**
     * Status update for tracked relays.
     */
    public static class RelayStatusUpdate {
        public String relayId;
        public RelayState oldState;
        public RelayState newState;
        public String relayerId;
        public long updateTimestamp;
        public String updateReason;
        public Map<String, Object> additionalInfo;
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
     * Information about active relayers.
     */
    public static class RelayerInfo {
        public String relayerId;
        public String relayerAddress;
        public String operatorAddress;
        public List<String> supportedChains;
        public RelayerStatus status;
        public BigDecimal stakedAmount;
        public double reputationScore;
        public RelayerCapabilities capabilities;
        public RelayerPerformanceMetrics performance;
        public long registrationTimestamp;
    }

    /**
     * Performance metrics for relayers.
     */
    public static class RelayerPerformanceMetrics {
        public int totalRelays;
        public int successfulRelays;
        public int failedRelays;
        public double successRate;
        public Duration averageRelayTime;
        public BigDecimal totalRewardsEarned;
        public BigDecimal totalSlashedAmount;
        public double uptimePercentage;
        public long lastActiveTimestamp;
    }

    /**
     * Reputation score for relayers.
     */
    public static class RelayerReputation {
        public String relayerId;
        public double reputationScore; // 0.0 to 1.0
        public ReputationLevel level;
        public Map<String, Double> reputationFactors;
        public long calculationTimestamp;
        public String reputationTrend; // "IMPROVING", "STABLE", "DECLINING"
    }

    /**
     * Reputation levels for relayers.
     */
    public enum ReputationLevel {
        NOVICE,
        TRUSTED,
        VETERAN,
        ELITE,
        BLACKLISTED
    }

    /**
     * Result of token staking.
     */
    public static class StakingResult {
        public String relayerId;
        public String stakingTransactionHash;
        public BigDecimal stakedAmount;
        public BigDecimal totalStake;
        public Duration lockPeriod;
        public long unlockTimestamp;
        public boolean success;
        public String errorMessage;
    }

    /**
     * Result of token unstaking.
     */
    public static class UnstakingResult {
        public String relayerId;
        public String unstakingTransactionHash;
        public BigDecimal unstakedAmount;
        public BigDecimal remainingStake;
        public Duration cooldownPeriod;
        public boolean success;
        public String errorMessage;
    }

    /**
     * Types of rewards that can be claimed.
     */
    public enum RewardType {
        RELAY_REWARDS,      // Rewards from successful relays
        STAKING_REWARDS,    // Rewards from staking tokens
        BONUS_REWARDS,      // Bonus rewards for exceptional performance
        ALL_REWARDS         // Claim all available rewards
    }

    /**
     * Result of reward claiming.
     */
    public static class RewardClaimResult {
        public String relayerId;
        public String claimTransactionHash;
        public Map<RewardType, BigDecimal> claimedRewards;
        public BigDecimal totalClaimedAmount;
        public BigDecimal remainingRewards;
        public boolean success;
        public String errorMessage;
    }

    /**
     * Report of fraudulent relayer behavior.
     */
    public static class FraudReport {
        public String reportId;
        public String accusedRelayerId;
        public String reporterAddress;
        public FraudType fraudType;
        public String description;
        public List<String> evidence; // Transaction hashes, proofs, etc.
        public BigDecimal bondAmount; // Bond posted by reporter
        public long reportTimestamp;
    }

    /**
     * Types of fraudulent behavior.
     */
    public enum FraudType {
        INVALID_PROOF,      // Submitting invalid proofs
        DOUBLE_SPENDING,    // Attempting double spend
        CENSORSHIP,         // Censoring valid transactions
        COLLUSION,          // Colluding with other relayers
        DOWNTIME,           // Excessive downtime
        MALICIOUS_RELAY     // Malicious relay execution
    }

    /**
     * Result of fraud report submission.
     */
    public static class FraudReportResult {
        public String reportId;
        public boolean submitted;
        public String investigationId;
        public Duration investigationPeriod;
        public String submissionTransactionHash;
        public String errorMessage;
    }

    /**
     * Dispute case for contested operations.
     */
    public static class DisputeCase {
        public String disputeId;
        public String relayId;
        public String disputantAddress;
        public String respondentRelayerId;
        public DisputeType disputeType;
        public String description;
        public List<String> evidence;
        public BigDecimal disputeBond;
        public Duration resolutionDeadline;
    }

    /**
     * Types of disputes.
     */
    public enum DisputeType {
        INCORRECT_EXECUTION,    // Incorrect relay execution
        DELAYED_EXECUTION,      // Excessive delay in execution
        REWARD_DISPUTE,         // Dispute over reward calculation
        SLASHING_DISPUTE,       // Dispute over slashing decision
        REPUTATION_DISPUTE      // Dispute over reputation scoring
    }

    /**
     * Result of dispute initiation.
     */
    public static class DisputeResult {
        public String disputeId;
        public boolean initiated;
        public List<String> arbitrators;
        public Duration votingPeriod;
        public String disputeTransactionHash;
        public String errorMessage;
    }

    /**
     * Result of relayer slashing.
     */
    public static class SlashingResult {
        public String relayerId;
        public String slashingTransactionHash;
        public BigDecimal slashedAmount;
        public BigDecimal remainingStake;
        public Duration jailPeriod;
        public SlashingReason reason;
        public boolean success;
        public String errorMessage;
    }

    /**
     * Reasons for slashing relayers.
     */
    public enum SlashingReason {
        FRAUD_PROVEN,
        EXCESSIVE_DOWNTIME,
        INVALID_PROOFS,
        MALICIOUS_BEHAVIOR,
        DISPUTE_LOST
    }

    /**
     * Health metrics for the relayer network.
     */
    public static class RelayerNetworkHealth {
        public long timestamp;
        public int totalRelayers;
        public int activeRelayers;
        public int inactiveRelayers;
        public double averageReputationScore;
        public BigDecimal totalStakedAmount;
        public double networkUtilization;
        public Duration averageRelayTime;
        public double networkSuccessRate;
        public List<String> networkIssues;
    }

    /**
     * Configuration for relayer optimization.
     */
    public static class RelayerOptimizationConfig {
        public OptimizationGoal goal;
        public Duration optimizationWindow;
        public boolean enableDynamicSelection;
        public Map<String, Double> selectionWeights;
        public boolean enableLoadBalancing;
    }

    /**
     * Goals for relayer optimization.
     */
    public enum OptimizationGoal {
        MINIMIZE_COST,
        MINIMIZE_TIME,
        MAXIMIZE_RELIABILITY,
        BALANCE_ALL
    }

    /**
     * Result of relayer optimization.
     */
    public static class OptimizationResult {
        public boolean optimized;
        public String newSelectionStrategy;
        public Map<String, Object> optimizationMetrics;
        public double improvementScore;
        public String errorMessage;
    }

    /**
     * Economic parameters for the relayer network.
     */
    public static class EconomicParameters {
        public BigDecimal minimumStakeAmount;
        public BigDecimal baseRewardAmount;
        public double rewardMultiplier;
        public double slashingPercentage;
        public Duration stakeLockPeriod;
        public Duration unstakeCooldownPeriod;
        public BigDecimal disputeBondAmount;
        public Duration disputeResolutionPeriod;
    }

    /**
     * Historical statistics for relayers.
     */
    public static class RelayerStatistics {
        public String relayerId; // Null for network-wide stats
        public long totalRelays;
        public long successfulRelays;
        public long failedRelays;
        public double successRate;
        public Duration averageRelayTime;
        public BigDecimal totalRewardsEarned;
        public BigDecimal totalSlashedAmount;
        public Map<String, Long> relaysByChainPair;
        public Map<RelayPriority, Long> relaysByPriority;
        public long statisticsTimeWindow;
    }

    /**
     * Configuration for automated relayer operations.
     */
    public static class RelayerAutomationConfig {
        public boolean enableAutoStaking;
        public boolean enableAutoRewardClaim;
        public boolean enableAutoReputationMonitoring;
        public BigDecimal autoStakeThreshold;
        public Duration autoClaimInterval;
        public boolean enableAutomaticDisputes;
        public Map<String, Object> customAutomationRules;
    }

    /**
     * Result of automation setup.
     */
    public static class AutomationSetupResult {
        public boolean configured;
        public List<String> enabledAutomations;
        public String configurationTransactionHash;
        public String errorMessage;
    }
}