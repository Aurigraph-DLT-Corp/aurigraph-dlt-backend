package io.aurigraph.v11.defi.adapters;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.math.BigDecimal;
import java.util.List;

/**
 * Adapter interface for Uniswap V3 protocol integration.
 * 
 * This adapter provides comprehensive integration with Uniswap V3's concentrated liquidity
 * and advanced AMM features. It handles position management, fee collection, range orders,
 * and multi-hop swaps with optimal routing.
 * 
 * Key Features:
 * - Concentrated liquidity position management
 * - Advanced swap routing with multi-hop paths
 * - Fee tier optimization and selection
 * - Range order execution and monitoring
 * - Flash loans and flash swaps
 * - MEV protection and sandwich attack resistance
 * 
 * Performance Requirements:
 * - Support for 100K+ positions across all fee tiers
 * - Sub-second quote generation and routing
 * - Real-time price and liquidity monitoring
 * 
 * @author Aurigraph DLT Platform
 * @version 11.0.0
 * @since 2025-01-01
 */
@ApplicationScoped
public interface UniswapV3Integration {

    /**
     * Executes a single-hop swap through Uniswap V3 with optimal fee tier selection.
     * 
     * @param tokenIn the input token address
     * @param tokenOut the output token address
     * @param amountIn the amount of input tokens
     * @param amountOutMinimum minimum acceptable output amount
     * @param sqrtPriceLimitX96 the price limit for the swap (0 for no limit)
     * @param recipient the recipient address
     * @param deadline the swap deadline timestamp
     * @return Uni containing the swap execution result
     */
    Uni<UniswapV3SwapResult> exactInputSingle(
        String tokenIn,
        String tokenOut,
        BigDecimal amountIn,
        BigDecimal amountOutMinimum,
        BigDecimal sqrtPriceLimitX96,
        String recipient,
        long deadline
    );

    /**
     * Executes a multi-hop swap through multiple Uniswap V3 pools.
     * 
     * @param path the encoded path (token addresses and fee tiers)
     * @param amountIn the amount of input tokens
     * @param amountOutMinimum minimum acceptable output amount
     * @param recipient the recipient address
     * @param deadline the swap deadline timestamp
     * @return Uni containing the multi-hop swap result
     */
    Uni<UniswapV3SwapResult> exactInput(
        byte[] path,
        BigDecimal amountIn,
        BigDecimal amountOutMinimum,
        String recipient,
        long deadline
    );

    /**
     * Gets the best quote for a swap with automatic route optimization.
     * 
     * @param tokenIn the input token address
     * @param tokenOut the output token address
     * @param amountIn the amount of input tokens
     * @param feeTiers the fee tiers to consider (null for all tiers)
     * @return Uni containing the best quote with routing information
     */
    Uni<UniswapV3Quote> getBestQuote(
        String tokenIn,
        String tokenOut,
        BigDecimal amountIn,
        List<Integer> feeTiers
    );

    /**
     * Mints a new concentrated liquidity position.
     * 
     * @param tokenA the first token address
     * @param tokenB the second token address
     * @param fee the fee tier (500, 3000, 10000)
     * @param tickLower the lower tick boundary
     * @param tickUpper the upper tick boundary
     * @param amount0Desired desired amount of token0
     * @param amount1Desired desired amount of token1
     * @param amount0Min minimum amount of token0
     * @param amount1Min minimum amount of token1
     * @param recipient the recipient of the position NFT
     * @param deadline the transaction deadline
     * @return Uni containing the minting result
     */
    Uni<UniswapV3MintResult> mintPosition(
        String tokenA,
        String tokenB,
        int fee,
        int tickLower,
        int tickUpper,
        BigDecimal amount0Desired,
        BigDecimal amount1Desired,
        BigDecimal amount0Min,
        BigDecimal amount1Min,
        String recipient,
        long deadline
    );

    /**
     * Increases liquidity for an existing position.
     * 
     * @param tokenId the position NFT token ID
     * @param amount0Desired desired amount of token0 to add
     * @param amount1Desired desired amount of token1 to add
     * @param amount0Min minimum amount of token0 to add
     * @param amount1Min minimum amount of token1 to add
     * @param deadline the transaction deadline
     * @return Uni containing the liquidity increase result
     */
    Uni<UniswapV3LiquidityResult> increaseLiquidity(
        BigDecimal tokenId,
        BigDecimal amount0Desired,
        BigDecimal amount1Desired,
        BigDecimal amount0Min,
        BigDecimal amount1Min,
        long deadline
    );

    /**
     * Decreases liquidity from an existing position.
     * 
     * @param tokenId the position NFT token ID
     * @param liquidity the amount of liquidity to remove
     * @param amount0Min minimum amount of token0 to receive
     * @param amount1Min minimum amount of token1 to receive
     * @param deadline the transaction deadline
     * @return Uni containing the liquidity decrease result
     */
    Uni<UniswapV3LiquidityResult> decreaseLiquidity(
        BigDecimal tokenId,
        BigDecimal liquidity,
        BigDecimal amount0Min,
        BigDecimal amount1Min,
        long deadline
    );

    /**
     * Collects fees from a liquidity position.
     * 
     * @param tokenId the position NFT token ID
     * @param recipient the recipient address for fees
     * @param amount0Max maximum amount of token0 fees to collect
     * @param amount1Max maximum amount of token1 fees to collect
     * @return Uni containing the fee collection result
     */
    Uni<UniswapV3FeeCollection> collectFees(
        BigDecimal tokenId,
        String recipient,
        BigDecimal amount0Max,
        BigDecimal amount1Max
    );

    /**
     * Burns a liquidity position NFT after removing all liquidity.
     * 
     * @param tokenId the position NFT token ID
     * @return Uni containing the burn result
     */
    Uni<Boolean> burnPosition(BigDecimal tokenId);

    /**
     * Gets detailed information about a liquidity position.
     * 
     * @param tokenId the position NFT token ID
     * @return Uni containing the position information
     */
    Uni<UniswapV3Position> getPositionInfo(BigDecimal tokenId);

    /**
     * Gets comprehensive pool information for a token pair and fee tier.
     * 
     * @param tokenA the first token address
     * @param tokenB the second token address
     * @param fee the fee tier
     * @return Uni containing the pool information
     */
    Uni<UniswapV3Pool> getPoolInfo(String tokenA, String tokenB, int fee);

    /**
     * Calculates the optimal price range for a concentrated liquidity position.
     * 
     * @param tokenA the first token address
     * @param tokenB the second token address
     * @param fee the fee tier
     * @param capitalAmount the amount of capital to deploy
     * @param riskTolerance the user's risk tolerance
     * @param timeHorizon the intended holding period in days
     * @return Uni containing the optimal range calculation
     */
    Uni<OptimalRangeCalculation> calculateOptimalRange(
        String tokenA,
        String tokenB,
        int fee,
        BigDecimal capitalAmount,
        RiskTolerance riskTolerance,
        int timeHorizon
    );

    /**
     * Monitors positions for range adjustment opportunities.
     * 
     * @param userAddress the user's wallet address
     * @param maxPriceDeviation maximum price deviation before recommendation
     * @return Multi streaming range adjustment recommendations
     */
    Multi<RangeAdjustmentRecommendation> monitorRangeAdjustments(
        String userAddress,
        BigDecimal maxPriceDeviation
    );

    /**
     * Executes a flash swap for arbitrage or other purposes.
     * 
     * @param tokenA the first token address
     * @param tokenB the second token address
     * @param fee the fee tier
     * @param amount0 amount of token0 to borrow (negative to pay)
     * @param amount1 amount of token1 to borrow (negative to pay)
     * @param recipient the recipient address
     * @param data callback data for flash swap execution
     * @return Uni containing the flash swap result
     */
    Uni<UniswapV3FlashSwapResult> flashSwap(
        String tokenA,
        String tokenB,
        int fee,
        BigDecimal amount0,
        BigDecimal amount1,
        String recipient,
        byte[] data
    );

    /**
     * Gets historical price data for a pool with specified intervals.
     * 
     * @param tokenA the first token address
     * @param tokenB the second token address
     * @param fee the fee tier
     * @param fromTimestamp start timestamp
     * @param toTimestamp end timestamp
     * @param interval data interval in seconds
     * @return Multi streaming historical price data
     */
    Multi<UniswapV3PriceData> getHistoricalPrices(
        String tokenA,
        String tokenB,
        int fee,
        long fromTimestamp,
        long toTimestamp,
        int interval
    );

    /**
     * Calculates impermanent loss for a Uniswap V3 position.
     * 
     * @param tokenId the position NFT token ID
     * @param fromTimestamp start timestamp for calculation
     * @param toTimestamp end timestamp for calculation
     * @return Uni containing impermanent loss analysis
     */
    Uni<UniswapV3ImpermanentLoss> calculateImpermanentLoss(
        BigDecimal tokenId,
        long fromTimestamp,
        long toTimestamp
    );

    // Inner classes and enums for data transfer objects

    /**
     * Risk tolerance levels for position optimization.
     */
    public enum RiskTolerance {
        CONSERVATIVE,
        MODERATE,
        AGGRESSIVE
    }

    /**
     * Result of a Uniswap V3 swap operation.
     */
    public static class UniswapV3SwapResult {
        public String transactionHash;
        public String tokenIn;
        public String tokenOut;
        public BigDecimal amountIn;
        public BigDecimal amountOut;
        public BigDecimal executionPrice;
        public BigDecimal priceImpact;
        public BigDecimal gasCost;
        public List<SwapPath> swapPath;
        public long executionTime;
        public boolean success;
        public String errorMessage;
    }

    /**
     * Swap path information for multi-hop swaps.
     */
    public static class SwapPath {
        public String tokenIn;
        public String tokenOut;
        public int fee;
        public BigDecimal amountIn;
        public BigDecimal amountOut;
        public BigDecimal priceImpact;
    }

    /**
     * Quote information for Uniswap V3 swaps.
     */
    public static class UniswapV3Quote {
        public String tokenIn;
        public String tokenOut;
        public BigDecimal amountIn;
        public BigDecimal amountOut;
        public BigDecimal executionPrice;
        public BigDecimal priceImpact;
        public BigDecimal estimatedGas;
        public List<QuoteRoute> routes;
        public QuoteRoute bestRoute;
        public long quoteTimestamp;
    }

    /**
     * Route information for quote calculation.
     */
    public static class QuoteRoute {
        public List<SwapPath> path;
        public BigDecimal outputAmount;
        public BigDecimal priceImpact;
        public BigDecimal gasEstimate;
        public int poolCount;
    }

    /**
     * Result of minting a new Uniswap V3 position.
     */
    public static class UniswapV3MintResult {
        public String transactionHash;
        public BigDecimal tokenId;
        public BigDecimal liquidity;
        public BigDecimal amount0;
        public BigDecimal amount1;
        public int tickLower;
        public int tickUpper;
        public boolean success;
        public String errorMessage;
    }

    /**
     * Result of liquidity operations (increase/decrease).
     */
    public static class UniswapV3LiquidityResult {
        public String transactionHash;
        public BigDecimal tokenId;
        public BigDecimal liquidityDelta;
        public BigDecimal amount0;
        public BigDecimal amount1;
        public BigDecimal newLiquidity;
        public boolean success;
        public String errorMessage;
    }

    /**
     * Result of fee collection from a position.
     */
    public static class UniswapV3FeeCollection {
        public String transactionHash;
        public BigDecimal tokenId;
        public BigDecimal amount0Collected;
        public BigDecimal amount1Collected;
        public BigDecimal totalFeesUSD;
        public boolean success;
        public String errorMessage;
    }

    /**
     * Comprehensive information about a Uniswap V3 position.
     */
    public static class UniswapV3Position {
        public BigDecimal tokenId;
        public String token0;
        public String token1;
        public int fee;
        public int tickLower;
        public int tickUpper;
        public BigDecimal liquidity;
        public BigDecimal feeGrowthInside0LastX128;
        public BigDecimal feeGrowthInside1LastX128;
        public BigDecimal tokensOwed0;
        public BigDecimal tokensOwed1;
        public BigDecimal amount0;
        public BigDecimal amount1;
        public BigDecimal uncollectedFees0;
        public BigDecimal uncollectedFees1;
        public boolean inRange;
        public BigDecimal currentPrice;
        public BigDecimal lowerPrice;
        public BigDecimal upperPrice;
    }

    /**
     * Information about a Uniswap V3 pool.
     */
    public static class UniswapV3Pool {
        public String poolAddress;
        public String token0;
        public String token1;
        public int fee;
        public BigDecimal sqrtPriceX96;
        public int tick;
        public int observationIndex;
        public int observationCardinality;
        public int observationCardinalityNext;
        public BigDecimal feeProtocol;
        public boolean unlocked;
        public BigDecimal liquidity;
        public BigDecimal volume24h;
        public BigDecimal tvl;
        public BigDecimal price;
    }

    /**
     * Optimal range calculation for concentrated liquidity.
     */
    public static class OptimalRangeCalculation {
        public String token0;
        public String token1;
        public int fee;
        public BigDecimal currentPrice;
        public BigDecimal optimalLowerPrice;
        public BigDecimal optimalUpperPrice;
        public int optimalLowerTick;
        public int optimalUpperTick;
        public BigDecimal expectedAPR;
        public BigDecimal capitalEfficiency;
        public BigDecimal riskScore;
        public String rationale;
    }

    /**
     * Range adjustment recommendation for existing positions.
     */
    public static class RangeAdjustmentRecommendation {
        public BigDecimal tokenId;
        public BigDecimal currentPrice;
        public BigDecimal lowerBound;
        public BigDecimal upperBound;
        public BigDecimal recommendedLowerPrice;
        public BigDecimal recommendedUpperPrice;
        public BigDecimal priceDeviation;
        public BigDecimal estimatedGasCost;
        public BigDecimal potentialBenefit;
        public String urgency; // "LOW", "MEDIUM", "HIGH"
        public String reason;
    }

    /**
     * Result of a flash swap operation.
     */
    public static class UniswapV3FlashSwapResult {
        public String transactionHash;
        public String poolAddress;
        public BigDecimal amount0;
        public BigDecimal amount1;
        public BigDecimal fee0;
        public BigDecimal fee1;
        public BigDecimal profit;
        public boolean success;
        public String errorMessage;
    }

    /**
     * Historical price data for a Uniswap V3 pool.
     */
    public static class UniswapV3PriceData {
        public String token0;
        public String token1;
        public int fee;
        public BigDecimal price;
        public BigDecimal sqrtPriceX96;
        public int tick;
        public BigDecimal liquidity;
        public BigDecimal volume;
        public long timestamp;
    }

    /**
     * Impermanent loss analysis for a Uniswap V3 position.
     */
    public static class UniswapV3ImpermanentLoss {
        public BigDecimal tokenId;
        public BigDecimal initialValue;
        public BigDecimal currentValue;
        public BigDecimal holdValue;
        public BigDecimal impermanentLoss;
        public BigDecimal impermanentLossPercent;
        public BigDecimal feesEarned;
        public BigDecimal netResult;
        public BigDecimal netResultPercent;
        public boolean outperformedHolding;
        public long calculationTimestamp;
    }
}