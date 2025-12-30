package io.aurigraph.v11.defi.services;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Service interface for comprehensive liquidity pool management across DeFi protocols.
 * 
 * This service provides advanced liquidity pool operations including creation, management,
 * monitoring, and optimization. It supports multiple AMM types including Uniswap V2/V3,
 * Curve Finance, Balancer, and concentrated liquidity pools.
 * 
 * Key Features:
 * - Multi-protocol liquidity pool management
 * - Concentrated liquidity position optimization
 * - Impermanent loss tracking and protection
 * - Automated rebalancing and range adjustment
 * - Fee optimization and MEV protection
 * - Cross-pool arbitrage and yield maximization
 * 
 * Performance Requirements:
 * - Support for 10K+ active LP positions
 * - Real-time monitoring of 1000+ pools
 * - Sub-second price and fee calculations
 * 
 * @author Aurigraph DLT Platform
 * @version 11.0.0
 * @since 2025-01-01
 */
@ApplicationScoped
public interface LiquidityPoolService {

    /**
     * Creates a new liquidity pool for a token pair.
     * 
     * @param tokenA the first token address
     * @param tokenB the second token address
     * @param feeRate the fee rate for the pool (e.g., 0.3% = 3000)
     * @param initialPriceRatio the initial price ratio (tokenA/tokenB)
     * @param protocolId the AMM protocol to use
     * @param poolType the type of pool to create
     * @return Uni containing the created pool information
     */
    Uni<LiquidityPool> createLiquidityPool(
        String tokenA,
        String tokenB,
        int feeRate,
        BigDecimal initialPriceRatio,
        String protocolId,
        PoolType poolType
    );

    /**
     * Adds liquidity to an existing pool with automatic slippage protection.
     * 
     * @param poolId the liquidity pool identifier
     * @param tokenAAmount the amount of tokenA to add
     * @param tokenBAmount the amount of tokenB to add
     * @param minTokenAAmount minimum tokenA amount (slippage protection)
     * @param minTokenBAmount minimum tokenB amount (slippage protection)
     * @param recipient the recipient address for LP tokens
     * @param deadline the transaction deadline timestamp
     * @return Uni containing the liquidity addition result
     */
    Uni<LiquidityAddition> addLiquidity(
        String poolId,
        BigDecimal tokenAAmount,
        BigDecimal tokenBAmount,
        BigDecimal minTokenAAmount,
        BigDecimal minTokenBAmount,
        String recipient,
        long deadline
    );

    /**
     * Adds concentrated liquidity within a specific price range (for V3-style pools).
     * 
     * @param poolId the liquidity pool identifier
     * @param tokenAAmount the amount of tokenA to add
     * @param tokenBAmount the amount of tokenB to add
     * @param lowerPrice the lower bound of the price range
     * @param upperPrice the upper bound of the price range
     * @param recipient the recipient address for the position NFT
     * @param deadline the transaction deadline timestamp
     * @return Uni containing the concentrated liquidity position
     */
    Uni<ConcentratedLiquidityPosition> addConcentratedLiquidity(
        String poolId,
        BigDecimal tokenAAmount,
        BigDecimal tokenBAmount,
        BigDecimal lowerPrice,
        BigDecimal upperPrice,
        String recipient,
        long deadline
    );

    /**
     * Removes liquidity from a pool position.
     * 
     * @param positionId the liquidity position identifier
     * @param liquidityPercent the percentage of liquidity to remove (0-100)
     * @param minTokenAAmount minimum tokenA amount to receive
     * @param minTokenBAmount minimum tokenB amount to receive
     * @param recipient the recipient address for tokens
     * @param deadline the transaction deadline timestamp
     * @return Uni containing the liquidity removal result
     */
    Uni<LiquidityRemoval> removeLiquidity(
        String positionId,
        BigDecimal liquidityPercent,
        BigDecimal minTokenAAmount,
        BigDecimal minTokenBAmount,
        String recipient,
        long deadline
    );

    /**
     * Collects accrued fees from a liquidity position.
     * 
     * @param positionId the liquidity position identifier
     * @param recipient the recipient address for collected fees
     * @return Uni containing the fee collection result
     */
    Uni<FeeCollection> collectFees(String positionId, String recipient);

    /**
     * Rebalances a concentrated liquidity position to a new price range.
     * 
     * @param positionId the concentrated liquidity position identifier
     * @param newLowerPrice the new lower bound of the price range
     * @param newUpperPrice the new upper bound of the price range
     * @param maxSlippage maximum acceptable slippage for rebalancing
     * @return Uni containing the rebalancing result
     */
    Uni<RebalancingResult> rebalancePosition(
        String positionId,
        BigDecimal newLowerPrice,
        BigDecimal newUpperPrice,
        BigDecimal maxSlippage
    );

    /**
     * Gets comprehensive information about a liquidity pool.
     * 
     * @param poolId the liquidity pool identifier
     * @return Uni containing detailed pool information
     */
    Uni<LiquidityPool> getPoolInfo(String poolId);

    /**
     * Retrieves all liquidity positions for a specific user.
     * 
     * @param userAddress the user's wallet address
     * @param protocolId optional protocol filter
     * @return Multi streaming user's liquidity positions
     */
    Multi<LiquidityPosition> getUserLiquidityPositions(String userAddress, String protocolId);

    /**
     * Calculates the current value and performance of a liquidity position.
     * 
     * @param positionId the liquidity position identifier
     * @return Uni containing position value and performance metrics
     */
    Uni<PositionAnalysis> analyzePosition(String positionId);

    /**
     * Monitors pools for optimal rebalancing opportunities.
     * 
     * @param userAddress the user's wallet address
     * @param maxPriceDeviation maximum price deviation threshold
     * @return Multi streaming rebalancing recommendations
     */
    Multi<RebalancingRecommendation> monitorRebalancingOpportunities(
        String userAddress,
        BigDecimal maxPriceDeviation
    );

    /**
     * Finds optimal liquidity provision opportunities based on user criteria.
     * 
     * @param tokenA the first token (null for any token)
     * @param tokenB the second token (null for any token)
     * @param minAPR minimum APR requirement
     * @param maxRiskLevel maximum acceptable risk level
     * @param liquidityAmount the amount of liquidity to provide
     * @return Multi streaming optimal liquidity opportunities
     */
    Multi<LiquidityOpportunity> findOptimalLiquidityOpportunities(
        String tokenA,
        String tokenB,
        BigDecimal minAPR,
        RiskLevel maxRiskLevel,
        BigDecimal liquidityAmount
    );

    /**
     * Calculates impermanent loss for a liquidity position over time.
     * 
     * @param positionId the liquidity position identifier
     * @param fromTimestamp start timestamp for calculation
     * @param toTimestamp end timestamp for calculation
     * @return Uni containing detailed impermanent loss analysis
     */
    Uni<ImpermanentLossAnalysis> calculateImpermanentLoss(
        String positionId,
        long fromTimestamp,
        long toTimestamp
    );

    /**
     * Optimizes fee collection timing based on gas costs and accumulated fees.
     * 
     * @param positionIds the list of position identifiers to analyze
     * @param maxGasPrice maximum acceptable gas price
     * @return Uni containing fee collection optimization recommendations
     */
    Uni<FeeOptimization> optimizeFeeCollection(List<String> positionIds, BigDecimal maxGasPrice);

    /**
     * Sets up automated position management with predefined strategies.
     * 
     * @param positionId the liquidity position identifier
     * @param strategy the automation strategy to apply
     * @param parameters strategy-specific parameters
     * @return Uni containing the automation setup result
     */
    Uni<AutomationSetup> setupPositionAutomation(
        String positionId,
        AutomationStrategy strategy,
        Map<String, Object> parameters
    );

    /**
     * Simulates liquidity provision scenarios for risk assessment.
     * 
     * @param poolId the liquidity pool identifier
     * @param tokenAAmount the amount of tokenA to simulate
     * @param tokenBAmount the amount of tokenB to simulate
     * @param priceScenarios different price scenarios to test
     * @param timeHorizonDays the time horizon for simulation in days
     * @return Uni containing simulation results
     */
    Uni<LiquiditySimulation> simulateLiquidityProvision(
        String poolId,
        BigDecimal tokenAAmount,
        BigDecimal tokenBAmount,
        List<BigDecimal> priceScenarios,
        int timeHorizonDays
    );

    /**
     * Gets historical performance data for a liquidity pool.
     * 
     * @param poolId the liquidity pool identifier
     * @param fromTimestamp start timestamp for historical data
     * @param toTimestamp end timestamp for historical data
     * @param interval data interval (e.g., "1h", "1d")
     * @return Multi streaming historical pool performance data
     */
    Multi<PoolPerformanceData> getHistoricalPoolPerformance(
        String poolId,
        long fromTimestamp,
        long toTimestamp,
        String interval
    );

    /**
     * Executes cross-pool arbitrage using liquidity positions.
     * 
     * @param sourcePoolId the source liquidity pool
     * @param targetPoolId the target liquidity pool
     * @param arbitrageAmount the amount to arbitrage
     * @param maxSlippage maximum acceptable slippage
     * @return Uni containing arbitrage execution result
     */
    Uni<ArbitrageResult> executeCrossPoolArbitrage(
        String sourcePoolId,
        String targetPoolId,
        BigDecimal arbitrageAmount,
        BigDecimal maxSlippage
    );

    // Inner classes and enums for data transfer objects

    /**
     * Represents a liquidity pool with comprehensive information.
     */
    public static class LiquidityPool {
        public String poolId;
        public String poolAddress;
        public String protocolId;
        public String protocolName;
        public String tokenA;
        public String tokenB;
        public String tokenASymbol;
        public String tokenBSymbol;
        public BigDecimal reserveA;
        public BigDecimal reserveB;
        public BigDecimal totalLiquidity;
        public BigDecimal currentPrice;
        public int feeRate; // in basis points (300 = 0.3%)
        public BigDecimal volume24h;
        public BigDecimal fees24h;
        public BigDecimal apr;
        public BigDecimal apy;
        public PoolType poolType;
        public RiskLevel riskLevel;
        public long createdTimestamp;
        public boolean isActive;
    }

    /**
     * Types of liquidity pools.
     */
    public enum PoolType {
        CONSTANT_PRODUCT, // Uniswap V2 style
        CONCENTRATED_LIQUIDITY, // Uniswap V3 style
        STABLE_SWAP, // Curve style
        WEIGHTED, // Balancer style
        VOLATILE_AMM // Solidly style
    }

    /**
     * Risk levels for liquidity pools.
     */
    public enum RiskLevel {
        LOW, // Stable pairs, established protocols
        MEDIUM, // Major tokens, proven protocols
        HIGH, // Volatile pairs, newer protocols
        VERY_HIGH // Experimental tokens or protocols
    }

    /**
     * Result of adding liquidity to a pool.
     */
    public static class LiquidityAddition {
        public String transactionHash;
        public String poolId;
        public String positionId;
        public BigDecimal tokenAAdded;
        public BigDecimal tokenBAdded;
        public BigDecimal lpTokensMinted;
        public BigDecimal shareOfPool;
        public BigDecimal estimatedAPR;
        public boolean success;
        public String errorMessage;
    }

    /**
     * Represents a concentrated liquidity position.
     */
    public static class ConcentratedLiquidityPosition {
        public String positionId;
        public String tokenId; // NFT token ID for the position
        public String poolId;
        public BigDecimal liquidity;
        public BigDecimal lowerPrice;
        public BigDecimal upperPrice;
        public BigDecimal tokenAAmount;
        public BigDecimal tokenBAmount;
        public BigDecimal feesEarned;
        public boolean inRange;
        public BigDecimal utilizationRate;
        public long createdTimestamp;
    }

    /**
     * Result of removing liquidity from a pool.
     */
    public static class LiquidityRemoval {
        public String transactionHash;
        public String positionId;
        public BigDecimal liquidityRemoved;
        public BigDecimal tokenAReceived;
        public BigDecimal tokenBReceived;
        public BigDecimal feesCollected;
        public BigDecimal lpTokensBurned;
        public boolean success;
        public String errorMessage;
    }

    /**
     * Result of collecting fees from a position.
     */
    public static class FeeCollection {
        public String transactionHash;
        public String positionId;
        public BigDecimal tokenAFees;
        public BigDecimal tokenBFees;
        public BigDecimal totalFeesUSD;
        public BigDecimal gasCost;
        public BigDecimal netFeesUSD;
        public boolean success;
        public String errorMessage;
    }

    /**
     * Result of rebalancing a concentrated liquidity position.
     */
    public static class RebalancingResult {
        public String transactionHash;
        public String oldPositionId;
        public String newPositionId;
        public BigDecimal newLowerPrice;
        public BigDecimal newUpperPrice;
        public BigDecimal rebalancingCost;
        public BigDecimal slippageIncurred;
        public BigDecimal newUtilizationRate;
        public boolean success;
        public String errorMessage;
    }

    /**
     * Represents a user's liquidity position.
     */
    public static class LiquidityPosition {
        public String positionId;
        public String poolId;
        public String userAddress;
        public PoolType poolType;
        public BigDecimal liquidity;
        public BigDecimal tokenAAmount;
        public BigDecimal tokenBAmount;
        public BigDecimal shareOfPool;
        public BigDecimal valueUSD;
        public BigDecimal feesEarned;
        public BigDecimal currentAPR;
        public BigDecimal impermanentLoss;
        public long createdTimestamp;
        public boolean isActive;
        
        // For concentrated liquidity positions
        public BigDecimal lowerPrice;
        public BigDecimal upperPrice;
        public boolean inRange;
    }

    /**
     * Comprehensive analysis of a liquidity position.
     */
    public static class PositionAnalysis {
        public String positionId;
        public BigDecimal currentValue;
        public BigDecimal initialValue;
        public BigDecimal totalReturn;
        public BigDecimal totalReturnPercent;
        public BigDecimal feesEarned;
        public BigDecimal impermanentLoss;
        public BigDecimal impermanentLossPercent;
        public BigDecimal netResult;
        public BigDecimal dailyAPR;
        public BigDecimal projectedAnnualReturn;
        public RiskMetrics riskMetrics;
        public long analysisTimestamp;
    }

    /**
     * Risk metrics for position analysis.
     */
    public static class RiskMetrics {
        public BigDecimal volatility;
        public BigDecimal maxDrawdown;
        public BigDecimal sharpeRatio;
        public BigDecimal valueAtRisk;
        public RiskLevel riskLevel;
    }

    /**
     * Recommendation for position rebalancing.
     */
    public static class RebalancingRecommendation {
        public String positionId;
        public BigDecimal currentPrice;
        public BigDecimal lowerBound;
        public BigDecimal upperBound;
        public BigDecimal recommendedLowerPrice;
        public BigDecimal recommendedUpperPrice;
        public BigDecimal priceDeviation;
        public BigDecimal estimatedGasCost;
        public BigDecimal potentialBenefit;
        public String urgency; // "LOW", "MEDIUM", "HIGH", "CRITICAL"
        public String rationale;
    }

    /**
     * Optimal liquidity provision opportunity.
     */
    public static class LiquidityOpportunity {
        public String poolId;
        public String tokenA;
        public String tokenB;
        public BigDecimal currentAPR;
        public BigDecimal projectedAPR;
        public BigDecimal volume24h;
        public BigDecimal liquidity;
        public RiskLevel riskLevel;
        public BigDecimal optimalAmount;
        public BigDecimal expectedReturns;
        public String recommendation;
    }

    /**
     * Detailed impermanent loss analysis.
     */
    public static class ImpermanentLossAnalysis {
        public String positionId;
        public BigDecimal initialValue;
        public BigDecimal currentValue;
        public BigDecimal holdValue;
        public BigDecimal impermanentLoss;
        public BigDecimal impermanentLossPercent;
        public BigDecimal feesCompensation;
        public BigDecimal netResult;
        public boolean isProfitable;
        public List<ImpermanentLossPoint> historicalPoints;
        public long analysisTimestamp;
    }

    /**
     * Point-in-time impermanent loss data.
     */
    public static class ImpermanentLossPoint {
        public BigDecimal price;
        public BigDecimal impermanentLoss;
        public BigDecimal feesEarned;
        public BigDecimal netPosition;
        public long timestamp;
    }

    /**
     * Fee collection optimization recommendations.
     */
    public static class FeeOptimization {
        public List<FeeCollectionRecommendation> recommendations;
        public BigDecimal totalOptimalFees;
        public BigDecimal totalGasCost;
        public BigDecimal netOptimalFees;
        public long optimalCollectionTime;
    }

    /**
     * Individual fee collection recommendation.
     */
    public static class FeeCollectionRecommendation {
        public String positionId;
        public BigDecimal accruedFees;
        public BigDecimal estimatedGasCost;
        public BigDecimal netFees;
        public boolean shouldCollect;
        public String reason;
        public long recommendedTime;
    }

    /**
     * Result of setting up position automation.
     */
    public static class AutomationSetup {
        public String positionId;
        public AutomationStrategy strategy;
        public Map<String, Object> parameters;
        public boolean enabled;
        public long nextExecutionTime;
        public boolean success;
        public String errorMessage;
    }

    /**
     * Automation strategies for liquidity positions.
     */
    public enum AutomationStrategy {
        AUTO_REBALANCE,
        AUTO_HARVEST_FEES,
        STOP_LOSS,
        TAKE_PROFIT,
        RANGE_ORDER
    }

    /**
     * Simulation results for liquidity provision.
     */
    public static class LiquiditySimulation {
        public String poolId;
        public BigDecimal initialValue;
        public List<SimulationScenario> scenarios;
        public SimulationScenario bestCase;
        public SimulationScenario worstCase;
        public SimulationScenario expectedCase;
        public RiskAssessment riskAssessment;
    }

    /**
     * Individual simulation scenario result.
     */
    public static class SimulationScenario {
        public BigDecimal priceChange;
        public BigDecimal finalValue;
        public BigDecimal totalReturn;
        public BigDecimal impermanentLoss;
        public BigDecimal feesEarned;
        public BigDecimal netResult;
        public BigDecimal probability;
    }

    /**
     * Risk assessment for simulation.
     */
    public static class RiskAssessment {
        public BigDecimal expectedReturn;
        public BigDecimal standardDeviation;
        public BigDecimal valueAtRisk95;
        public BigDecimal conditionalValueAtRisk;
        public BigDecimal maxPotentialLoss;
        public String riskRating;
    }

    /**
     * Historical pool performance data.
     */
    public static class PoolPerformanceData {
        public String poolId;
        public BigDecimal price;
        public BigDecimal volume;
        public BigDecimal fees;
        public BigDecimal liquidity;
        public BigDecimal apr;
        public int transactionCount;
        public long timestamp;
    }

    /**
     * Cross-pool arbitrage execution result.
     */
    public static class ArbitrageResult {
        public String sourcePoolId;
        public String targetPoolId;
        public BigDecimal arbitrageAmount;
        public BigDecimal profit;
        public BigDecimal profitPercent;
        public BigDecimal executionCost;
        public BigDecimal netProfit;
        public boolean success;
        public String errorMessage;
        public long executionTime;
    }
}