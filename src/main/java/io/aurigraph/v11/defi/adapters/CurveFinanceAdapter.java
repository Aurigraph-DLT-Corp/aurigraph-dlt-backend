package io.aurigraph.v11.defi.adapters;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.math.BigDecimal;
import java.util.List;

/**
 * Adapter interface for Curve Finance protocol integration.
 * 
 * This adapter provides comprehensive integration with Curve Finance's stable swap
 * and concentrated liquidity pools. It specializes in efficient trading of similar
 * assets (stablecoins, wrapped tokens) with minimal slippage and supports both
 * V1 and V2 (Cryptoswap) pool types.
 * 
 * Key Features:
 * - Stable swap pools for low-slippage trading
 * - Crypto pools for volatile asset pairs
 * - Gauge staking for CRV and protocol rewards
 * - Cross-asset arbitrage with minimal slippage
 * - Meta pool and factory pool support
 * - Vote-escrowed CRV (veCRV) governance integration
 * 
 * Performance Requirements:
 * - Support for 200+ pools across all Curve deployments
 * - Sub-100ms quote calculations for optimal routing
 * - Real-time gauge reward monitoring
 * 
 * @author Aurigraph DLT Platform
 * @version 11.0.0
 * @since 2025-01-01
 */
@ApplicationScoped
public interface CurveFinanceAdapter {

    /**
     * Executes a swap through a Curve pool with minimal slippage.
     * 
     * @param poolAddress the Curve pool contract address
     * @param fromTokenIndex the index of the input token in the pool
     * @param toTokenIndex the index of the output token in the pool
     * @param inputAmount the amount of input tokens
     * @param minOutputAmount the minimum acceptable output amount
     * @param recipient the recipient address for output tokens
     * @return Uni containing the swap execution result
     */
    Uni<CurveSwapResult> exchange(
        String poolAddress,
        int fromTokenIndex,
        int toTokenIndex,
        BigDecimal inputAmount,
        BigDecimal minOutputAmount,
        String recipient
    );

    /**
     * Executes a swap using underlying tokens (for meta pools).
     * 
     * @param poolAddress the Curve meta pool contract address
     * @param fromTokenIndex the index of the input token
     * @param toTokenIndex the index of the output token
     * @param inputAmount the amount of input tokens
     * @param minOutputAmount the minimum acceptable output amount
     * @param recipient the recipient address for output tokens
     * @return Uni containing the swap execution result
     */
    Uni<CurveSwapResult> exchangeUnderlying(
        String poolAddress,
        int fromTokenIndex,
        int toTokenIndex,
        BigDecimal inputAmount,
        BigDecimal minOutputAmount,
        String recipient
    );

    /**
     * Gets the expected output amount for a potential swap.
     * 
     * @param poolAddress the Curve pool contract address
     * @param fromTokenIndex the index of the input token
     * @param toTokenIndex the index of the output token
     * @param inputAmount the amount of input tokens
     * @return Uni containing the expected output amount
     */
    Uni<BigDecimal> getExchangeAmount(
        String poolAddress,
        int fromTokenIndex,
        int toTokenIndex,
        BigDecimal inputAmount
    );

    /**
     * Adds liquidity to a Curve pool.
     * 
     * @param poolAddress the Curve pool contract address
     * @param amounts array of token amounts to add (must match pool token count)
     * @param minMintAmount minimum acceptable LP token amount
     * @param recipient the recipient address for LP tokens
     * @return Uni containing the liquidity addition result
     */
    Uni<CurveLiquidityResult> addLiquidity(
        String poolAddress,
        BigDecimal[] amounts,
        BigDecimal minMintAmount,
        String recipient
    );

    /**
     * Removes liquidity from a Curve pool proportionally.
     * 
     * @param poolAddress the Curve pool contract address
     * @param lpTokenAmount the amount of LP tokens to burn
     * @param minAmounts minimum amounts for each underlying token
     * @param recipient the recipient address for underlying tokens
     * @return Uni containing the liquidity removal result
     */
    Uni<CurveLiquidityResult> removeLiquidity(
        String poolAddress,
        BigDecimal lpTokenAmount,
        BigDecimal[] minAmounts,
        String recipient
    );

    /**
     * Removes liquidity in a single token (imbalanced withdrawal).
     * 
     * @param poolAddress the Curve pool contract address
     * @param lpTokenAmount the amount of LP tokens to burn
     * @param tokenIndex the index of the token to receive
     * @param minAmount minimum amount of the output token
     * @param recipient the recipient address for tokens
     * @return Uni containing the single-token withdrawal result
     */
    Uni<CurveLiquidityResult> removeLiquidityOneCoin(
        String poolAddress,
        BigDecimal lpTokenAmount,
        int tokenIndex,
        BigDecimal minAmount,
        String recipient
    );

    /**
     * Stakes LP tokens in a Curve gauge to earn CRV rewards.
     * 
     * @param gaugeAddress the Curve gauge contract address
     * @param lpTokenAmount the amount of LP tokens to stake
     * @return Uni containing the staking result
     */
    Uni<CurveStakingResult> depositToGauge(String gaugeAddress, BigDecimal lpTokenAmount);

    /**
     * Withdraws LP tokens from a Curve gauge.
     * 
     * @param gaugeAddress the Curve gauge contract address
     * @param lpTokenAmount the amount of LP tokens to withdraw
     * @return Uni containing the withdrawal result
     */
    Uni<CurveStakingResult> withdrawFromGauge(String gaugeAddress, BigDecimal lpTokenAmount);

    /**
     * Claims CRV and other rewards from a gauge.
     * 
     * @param gaugeAddress the Curve gauge contract address
     * @param recipient the recipient address for rewards
     * @return Uni containing the claim result
     */
    Uni<CurveRewardClaim> claimRewards(String gaugeAddress, String recipient);

    /**
     * Gets comprehensive information about a Curve pool.
     * 
     * @param poolAddress the Curve pool contract address
     * @return Uni containing detailed pool information
     */
    Uni<CurvePoolInfo> getPoolInfo(String poolAddress);

    /**
     * Gets information about a Curve gauge.
     * 
     * @param gaugeAddress the Curve gauge contract address
     * @return Uni containing gauge information
     */
    Uni<CurveGaugeInfo> getGaugeInfo(String gaugeAddress);

    /**
     * Gets user's staking position in a gauge.
     * 
     * @param gaugeAddress the Curve gauge contract address
     * @param userAddress the user's address
     * @return Uni containing user's gauge position
     */
    Uni<CurveUserGaugePosition> getUserGaugePosition(String gaugeAddress, String userAddress);

    /**
     * Finds the most efficient route for a multi-pool swap.
     * 
     * @param fromToken the input token address
     * @param toToken the output token address
     * @param inputAmount the amount of input tokens
     * @return Uni containing the optimal swap route
     */
    Uni<CurveSwapRoute> findOptimalRoute(String fromToken, String toToken, BigDecimal inputAmount);

    /**
     * Calculates the annual percentage yield for a gauge.
     * 
     * @param gaugeAddress the Curve gauge contract address
     * @param includeBoost whether to include CRV boost calculations
     * @return Uni containing the APY calculation
     */
    Uni<CurveAPYCalculation> calculateGaugeAPY(String gaugeAddress, boolean includeBoost);

    /**
     * Monitors all user gauge positions for optimal reward claiming.
     * 
     * @param userAddress the user's address
     * @param minRewardThreshold minimum reward threshold for claiming
     * @return Multi streaming reward claiming opportunities
     */
    Multi<CurveRewardOpportunity> monitorRewardOpportunities(
        String userAddress,
        BigDecimal minRewardThreshold
    );

    /**
     * Locks CRV tokens for veCRV to boost gauge rewards.
     * 
     * @param amount the amount of CRV to lock
     * @param unlockTime the timestamp when tokens can be unlocked
     * @return Uni containing the lock result
     */
    Uni<CurveLockResult> createLock(BigDecimal amount, long unlockTime);

    /**
     * Increases the amount of locked CRV.
     * 
     * @param amount the additional amount of CRV to lock
     * @return Uni containing the lock increase result
     */
    Uni<CurveLockResult> increaseAmount(BigDecimal amount);

    /**
     * Extends the lock time for existing veCRV.
     * 
     * @param unlockTime the new unlock timestamp
     * @return Uni containing the lock extension result
     */
    Uni<CurveLockResult> increaseUnlockTime(long unlockTime);

    /**
     * Withdraws expired locked CRV tokens.
     * 
     * @return Uni containing the withdrawal result
     */
    Uni<CurveLockResult> withdraw();

    /**
     * Gets historical pool performance data.
     * 
     * @param poolAddress the Curve pool contract address
     * @param fromTimestamp start timestamp
     * @param toTimestamp end timestamp
     * @param interval data interval in seconds
     * @return Multi streaming historical performance data
     */
    Multi<CurveHistoricalData> getHistoricalPerformance(
        String poolAddress,
        long fromTimestamp,
        long toTimestamp,
        int interval
    );

    // Inner classes for data transfer objects

    /**
     * Result of a Curve swap operation.
     */
    public static class CurveSwapResult {
        public String transactionHash;
        public String poolAddress;
        public String fromToken;
        public String toToken;
        public BigDecimal inputAmount;
        public BigDecimal outputAmount;
        public BigDecimal executionPrice;
        public BigDecimal slippage;
        public BigDecimal fee;
        public boolean success;
        public String errorMessage;
    }

    /**
     * Result of liquidity operations (add/remove).
     */
    public static class CurveLiquidityResult {
        public String transactionHash;
        public String poolAddress;
        public String operation; // "ADD" or "REMOVE"
        public BigDecimal[] tokenAmounts;
        public BigDecimal lpTokenAmount;
        public BigDecimal virtualPrice;
        public BigDecimal totalSupply;
        public boolean success;
        public String errorMessage;
    }

    /**
     * Result of gauge staking operations.
     */
    public static class CurveStakingResult {
        public String transactionHash;
        public String gaugeAddress;
        public String operation; // "DEPOSIT" or "WITHDRAW"
        public BigDecimal lpTokenAmount;
        public BigDecimal totalStaked;
        public BigDecimal estimatedRewards;
        public boolean success;
        public String errorMessage;
    }

    /**
     * Result of claiming rewards from a gauge.
     */
    public static class CurveRewardClaim {
        public String transactionHash;
        public String gaugeAddress;
        public BigDecimal crvClaimed;
        public List<CurveTokenReward> additionalRewards;
        public BigDecimal totalValueUSD;
        public boolean success;
        public String errorMessage;
    }

    /**
     * Individual token reward information.
     */
    public static class CurveTokenReward {
        public String tokenAddress;
        public String tokenSymbol;
        public BigDecimal amount;
        public BigDecimal valueUSD;
    }

    /**
     * Comprehensive information about a Curve pool.
     */
    public static class CurvePoolInfo {
        public String poolAddress;
        public String lpTokenAddress;
        public String poolType; // "STABLE", "CRYPTO", "META"
        public String[] coins;
        public String[] underlyingCoins; // For meta pools
        public BigDecimal[] balances;
        public BigDecimal totalSupply;
        public BigDecimal virtualPrice;
        public BigDecimal amplificationParameter; // A parameter for stable pools
        public BigDecimal adminFee;
        public BigDecimal fee;
        public BigDecimal volume24h;
        public BigDecimal tvl;
        public boolean isFactory;
        public long lastUpdated;
    }

    /**
     * Information about a Curve gauge.
     */
    public static class CurveGaugeInfo {
        public String gaugeAddress;
        public String lpTokenAddress;
        public String poolAddress;
        public BigDecimal totalSupply;
        public BigDecimal workingSupply;
        public BigDecimal inflationRate;
        public BigDecimal relativeWeight;
        public List<CurveRewardToken> rewardTokens;
        public boolean isKilled;
        public long periodFinish;
    }

    /**
     * Reward token information for a gauge.
     */
    public static class CurveRewardToken {
        public String tokenAddress;
        public String tokenSymbol;
        public BigDecimal rewardRate;
        public BigDecimal periodFinish;
        public BigDecimal totalRewards;
    }

    /**
     * User's position in a Curve gauge.
     */
    public static class CurveUserGaugePosition {
        public String gaugeAddress;
        public String userAddress;
        public BigDecimal stakedBalance;
        public BigDecimal workingBalance;
        public BigDecimal boost;
        public BigDecimal pendingCRV;
        public List<CurveTokenReward> pendingRewards;
        public BigDecimal totalPendingValueUSD;
        public long lastAction;
    }

    /**
     * Optimal swap route through multiple Curve pools.
     */
    public static class CurveSwapRoute {
        public String fromToken;
        public String toToken;
        public BigDecimal inputAmount;
        public BigDecimal expectedOutput;
        public BigDecimal totalFee;
        public BigDecimal priceImpact;
        public List<CurveSwapStep> swapSteps;
        public BigDecimal estimatedGas;
        public long quoteTimestamp;
    }

    /**
     * Individual step in a multi-pool swap route.
     */
    public static class CurveSwapStep {
        public String poolAddress;
        public String fromToken;
        public String toToken;
        public int fromIndex;
        public int toIndex;
        public BigDecimal inputAmount;
        public BigDecimal outputAmount;
        public boolean useUnderlying;
    }

    /**
     * APY calculation for a Curve gauge.
     */
    public static class CurveAPYCalculation {
        public String gaugeAddress;
        public BigDecimal baseAPY; // From trading fees
        public BigDecimal crvAPY; // From CRV rewards
        public BigDecimal additionalRewardsAPY; // From other reward tokens
        public BigDecimal totalAPY;
        public BigDecimal boostedAPY; // APY with current veCRV boost
        public BigDecimal maxBoostAPY; // APY with maximum possible boost
        public BigDecimal currentBoost;
        public long calculationTimestamp;
    }

    /**
     * Reward claiming opportunity for a user.
     */
    public static class CurveRewardOpportunity {
        public String gaugeAddress;
        public String poolName;
        public BigDecimal pendingCRV;
        public List<CurveTokenReward> pendingRewards;
        public BigDecimal totalPendingUSD;
        public BigDecimal estimatedGasCost;
        public BigDecimal netRewardUSD;
        public boolean shouldClaim;
        public String recommendation;
    }

    /**
     * Result of CRV locking operations for veCRV.
     */
    public static class CurveLockResult {
        public String transactionHash;
        public String operation; // "CREATE", "INCREASE_AMOUNT", "INCREASE_TIME", "WITHDRAW"
        public BigDecimal crvAmount;
        public long unlockTime;
        public BigDecimal veCrvBalance;
        public BigDecimal votingPower;
        public boolean success;
        public String errorMessage;
    }

    /**
     * Historical performance data for a Curve pool.
     */
    public static class CurveHistoricalData {
        public String poolAddress;
        public BigDecimal virtualPrice;
        public BigDecimal volume;
        public BigDecimal fees;
        public BigDecimal tvl;
        public BigDecimal apy;
        public BigDecimal[] balances;
        public long timestamp;
    }
}