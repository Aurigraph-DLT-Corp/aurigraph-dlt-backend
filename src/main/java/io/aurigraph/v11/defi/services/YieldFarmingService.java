package io.aurigraph.v11.defi.services;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Service interface for managing yield farming and staking operations across DeFi protocols.
 * 
 * This service provides comprehensive yield farming capabilities including staking,
 * reward harvesting, compound farming, and automated yield optimization strategies.
 * It supports multiple farming protocols and implements advanced features like
 * auto-compounding and yield maximization.
 * 
 * Key Features:
 * - Multi-protocol yield farming support
 * - Automated compound farming strategies
 * - Real-time APY/APR monitoring and optimization
 * - Risk-adjusted yield calculations
 * - Cross-protocol yield comparison
 * - Automated harvest and reinvestment
 * 
 * Performance Requirements:
 * - Support for 50K+ active farming positions
 * - Sub-second APY calculations and updates
 * - Real-time monitoring of 100+ farming pools
 * 
 * @author Aurigraph DLT Platform
 * @version 11.0.0
 * @since 2025-01-01
 */
@ApplicationScoped
public interface YieldFarmingService {

    /**
     * Stakes tokens in a yield farming pool to earn rewards.
     * 
     * @param farmId the farming pool identifier
     * @param stakingToken the token to stake
     * @param amount the amount of tokens to stake
     * @param userAddress the user's wallet address
     * @param lockPeriod optional lock period in seconds (0 for no lock)
     * @return Uni containing the staking result
     */
    Uni<StakingResult> stakeTokens(
        String farmId,
        String stakingToken,
        BigDecimal amount,
        String userAddress,
        long lockPeriod
    );

    /**
     * Unstakes tokens from a yield farming pool.
     * 
     * @param farmId the farming pool identifier
     * @param amount the amount to unstake (null for full unstaking)
     * @param userAddress the user's wallet address
     * @return Uni containing the unstaking result
     */
    Uni<UnstakingResult> unstakeTokens(String farmId, BigDecimal amount, String userAddress);

    /**
     * Harvests accumulated rewards from farming positions.
     * 
     * @param farmId the farming pool identifier
     * @param userAddress the user's wallet address
     * @param reinvest whether to reinvest rewards back into the pool
     * @return Uni containing the harvest result
     */
    Uni<HarvestResult> harvestRewards(String farmId, String userAddress, boolean reinvest);

    /**
     * Harvests rewards from multiple farming positions in a single transaction.
     * 
     * @param farmIds the list of farming pool identifiers
     * @param userAddress the user's wallet address
     * @param reinvestmentStrategy the strategy for reinvesting harvested rewards
     * @return Uni containing the batch harvest result
     */
    Uni<BatchHarvestResult> batchHarvestRewards(
        List<String> farmIds,
        String userAddress,
        ReinvestmentStrategy reinvestmentStrategy
    );

    /**
     * Gets comprehensive information about a yield farming pool.
     * 
     * @param farmId the farming pool identifier
     * @return Uni containing the farming pool details
     */
    Uni<YieldFarm> getFarmInfo(String farmId);

    /**
     * Retrieves all available yield farming opportunities.
     * 
     * @param minApy minimum APY threshold for filtering
     * @param stakingToken optional filter by staking token
     * @param protocolId optional filter by protocol
     * @return Multi streaming available farming opportunities
     */
    Multi<YieldFarm> getAvailableFarms(BigDecimal minApy, String stakingToken, String protocolId);

    /**
     * Gets all farming positions for a specific user.
     * 
     * @param userAddress the user's wallet address
     * @return Multi streaming user's farming positions
     */
    Multi<FarmingPosition> getUserFarmingPositions(String userAddress);

    /**
     * Calculates the current value and pending rewards for a farming position.
     * 
     * @param farmId the farming pool identifier
     * @param userAddress the user's wallet address
     * @return Uni containing the position value and rewards
     */
    Uni<PositionValue> calculatePositionValue(String farmId, String userAddress);

    /**
     * Optimizes yield farming positions by suggesting better opportunities.
     * 
     * @param userAddress the user's wallet address
     * @param riskTolerance the user's risk tolerance level
     * @param minYieldImprovement minimum yield improvement threshold
     * @return Uni containing yield optimization suggestions
     */
    Uni<List<YieldOptimization>> optimizeYieldPositions(
        String userAddress,
        RiskTolerance riskTolerance,
        BigDecimal minYieldImprovement
    );

    /**
     * Sets up automated compound farming for a position.
     * 
     * @param farmId the farming pool identifier
     * @param userAddress the user's wallet address
     * @param compoundingFrequency how often to compound (in hours)
     * @param minHarvestAmount minimum reward amount required to trigger harvest
     * @param maxGasPrice maximum acceptable gas price for compounding
     * @return Uni containing the auto-compound setup result
     */
    Uni<AutoCompoundSetup> setupAutoCompounding(
        String farmId,
        String userAddress,
        int compoundingFrequency,
        BigDecimal minHarvestAmount,
        BigDecimal maxGasPrice
    );

    /**
     * Disables automated compounding for a position.
     * 
     * @param farmId the farming pool identifier
     * @param userAddress the user's wallet address
     * @return Uni containing the disable result
     */
    Uni<Boolean> disableAutoCompounding(String farmId, String userAddress);

    /**
     * Monitors farming positions for optimal harvest timing.
     * 
     * @param userAddress the user's wallet address
     * @return Multi streaming harvest recommendations
     */
    Multi<HarvestRecommendation> monitorHarvestOpportunities(String userAddress);

    /**
     * Calculates the optimal allocation across multiple farming pools.
     * 
     * @param totalAmount the total amount to allocate
     * @param stakingToken the token to allocate
     * @param riskTolerance the user's risk tolerance
     * @param targetAPY the target APY goal
     * @return Uni containing the optimal allocation strategy
     */
    Uni<AllocationStrategy> calculateOptimalAllocation(
        BigDecimal totalAmount,
        String stakingToken,
        RiskTolerance riskTolerance,
        BigDecimal targetAPY
    );

    /**
     * Gets historical APY data for yield farming analysis.
     * 
     * @param farmId the farming pool identifier
     * @param fromTimestamp start timestamp for historical data
     * @param toTimestamp end timestamp for historical data
     * @return Multi streaming historical APY data
     */
    Multi<HistoricalAPY> getHistoricalAPY(String farmId, long fromTimestamp, long toTimestamp);

    /**
     * Calculates impermanent loss for LP token farming positions.
     * 
     * @param farmId the farming pool identifier (for LP farms)
     * @param userAddress the user's wallet address
     * @param initialPrice the initial price ratio when LP was created
     * @return Uni containing impermanent loss information
     */
    Uni<ImpermanentLossInfo> calculateFarmingImpermanentLoss(
        String farmId,
        String userAddress,
        BigDecimal initialPrice
    );

    /**
     * Executes emergency withdrawal from a farming position.
     * 
     * @param farmId the farming pool identifier
     * @param userAddress the user's wallet address
     * @param acceptPenalty whether to accept early withdrawal penalties
     * @return Uni containing the emergency withdrawal result
     */
    Uni<EmergencyWithdrawalResult> emergencyWithdraw(
        String farmId,
        String userAddress,
        boolean acceptPenalty
    );

    // Inner classes and enums for data transfer objects

    /**
     * Represents the result of a staking operation.
     */
    public static class StakingResult {
        public String transactionHash;
        public String farmId;
        public String stakingToken;
        public BigDecimal stakedAmount;
        public BigDecimal totalStaked;
        public BigDecimal currentAPY;
        public long lockExpiryTimestamp;
        public boolean success;
        public String errorMessage;
    }

    /**
     * Represents the result of an unstaking operation.
     */
    public static class UnstakingResult {
        public String transactionHash;
        public String farmId;
        public BigDecimal unstakedAmount;
        public BigDecimal remainingStaked;
        public BigDecimal penalty;
        public BigDecimal netReceived;
        public boolean success;
        public String errorMessage;
    }

    /**
     * Represents the result of a reward harvest operation.
     */
    public static class HarvestResult {
        public String transactionHash;
        public String farmId;
        public Map<String, BigDecimal> harvestedRewards; // rewardToken -> amount
        public BigDecimal totalValueUSD;
        public BigDecimal gasCost;
        public boolean reinvested;
        public BigDecimal reinvestedAmount;
        public boolean success;
        public String errorMessage;
    }

    /**
     * Represents the result of batch harvesting multiple farms.
     */
    public static class BatchHarvestResult {
        public List<HarvestResult> individualHarvests;
        public BigDecimal totalRewardsUSD;
        public BigDecimal totalGasCost;
        public BigDecimal netRewards;
        public int successfulHarvests;
        public int failedHarvests;
        public boolean allSuccessful;
    }

    /**
     * Represents a yield farming pool/opportunity.
     */
    public static class YieldFarm {
        public String farmId;
        public String protocolId;
        public String protocolName;
        public String stakingToken;
        public List<String> rewardTokens;
        public BigDecimal currentAPY;
        public BigDecimal currentAPR;
        public BigDecimal totalValueLocked;
        public BigDecimal dailyRewards;
        public int lockPeriod; // in seconds, 0 for no lock
        public BigDecimal earlyWithdrawalPenalty;
        public RiskLevel riskLevel;
        public String farmType; // "SINGLE_STAKE", "LP_FARM", "VAULT"
        public boolean autoCompoundAvailable;
        public long createdTimestamp;
        public boolean isActive;
    }

    /**
     * Represents a user's farming position.
     */
    public static class FarmingPosition {
        public String positionId;
        public String farmId;
        public String userAddress;
        public BigDecimal stakedAmount;
        public BigDecimal pendingRewards;
        public Map<String, BigDecimal> pendingRewardsByToken;
        public BigDecimal totalRewardsEarned;
        public BigDecimal currentAPY;
        public long stakedTimestamp;
        public long lockExpiryTimestamp;
        public boolean autoCompoundEnabled;
        public boolean canWithdraw;
    }

    /**
     * Contains position value and reward calculations.
     */
    public static class PositionValue {
        public String farmId;
        public BigDecimal stakedValue;
        public BigDecimal pendingRewardsValue;
        public BigDecimal totalValue;
        public BigDecimal unrealizedGains;
        public BigDecimal dailyEarnings;
        public BigDecimal projectedAnnualEarnings;
        public long calculationTimestamp;
    }

    /**
     * Contains yield optimization suggestions.
     */
    public static class YieldOptimization {
        public String currentFarmId;
        public String recommendedFarmId;
        public BigDecimal currentAPY;
        public BigDecimal recommendedAPY;
        public BigDecimal yieldImprovement;
        public BigDecimal migrationCost;
        public BigDecimal breakEvenDays;
        public RiskLevel currentRisk;
        public RiskLevel recommendedRisk;
        public String optimizationType; // "PROTOCOL_SWITCH", "COMPOUND_OPTIMIZATION", "REBALANCE"
    }

    /**
     * Setup result for automated compounding.
     */
    public static class AutoCompoundSetup {
        public String farmId;
        public String userAddress;
        public boolean enabled;
        public int compoundingFrequency;
        public BigDecimal minHarvestAmount;
        public BigDecimal maxGasPrice;
        public long nextCompoundTimestamp;
        public boolean success;
        public String errorMessage;
    }

    /**
     * Harvest timing recommendation.
     */
    public static class HarvestRecommendation {
        public String farmId;
        public BigDecimal pendingRewards;
        public BigDecimal currentGasPrice;
        public BigDecimal harvestCost;
        public BigDecimal netProfit;
        public boolean shouldHarvest;
        public String recommendation; // "HARVEST_NOW", "WAIT_FOR_LOWER_GAS", "ACCUMULATE_MORE"
        public long optimalHarvestTimestamp;
    }

    /**
     * Optimal allocation strategy across multiple farms.
     */
    public static class AllocationStrategy {
        public BigDecimal totalAmount;
        public String stakingToken;
        public List<AllocationRecommendation> allocations;
        public BigDecimal expectedAPY;
        public RiskLevel overallRisk;
        public BigDecimal diversificationScore;
    }

    /**
     * Individual allocation recommendation.
     */
    public static class AllocationRecommendation {
        public String farmId;
        public BigDecimal allocationPercent;
        public BigDecimal allocationAmount;
        public BigDecimal expectedAPY;
        public RiskLevel riskLevel;
        public String rationale;
    }

    /**
     * Historical APY data point.
     */
    public static class HistoricalAPY {
        public String farmId;
        public BigDecimal apy;
        public BigDecimal tvl;
        public BigDecimal dailyVolume;
        public long timestamp;
    }

    /**
     * Impermanent loss information for LP farms.
     */
    public static class ImpermanentLossInfo {
        public String farmId;
        public BigDecimal impermanentLoss;
        public BigDecimal impermanentLossPercent;
        public BigDecimal farmingRewards;
        public BigDecimal netResult;
        public boolean isProfitable;
        public long calculationTimestamp;
    }

    /**
     * Emergency withdrawal result.
     */
    public static class EmergencyWithdrawalResult {
        public String transactionHash;
        public String farmId;
        public BigDecimal withdrawnAmount;
        public BigDecimal penalty;
        public BigDecimal netReceived;
        public BigDecimal forfeitedRewards;
        public boolean success;
        public String errorMessage;
    }

    /**
     * Risk tolerance levels for yield optimization.
     */
    public enum RiskTolerance {
        CONSERVATIVE,
        MODERATE,
        AGGRESSIVE,
        HIGH_RISK
    }

    /**
     * Risk levels for farming pools.
     */
    public enum RiskLevel {
        LOW,
        MEDIUM,
        HIGH,
        VERY_HIGH
    }

    /**
     * Strategies for reinvesting harvested rewards.
     */
    public enum ReinvestmentStrategy {
        COMPOUND_SAME_FARM,
        OPTIMIZE_ACROSS_FARMS,
        WITHDRAW_TO_WALLET,
        DIVERSIFY_PROTOCOLS
    }
}