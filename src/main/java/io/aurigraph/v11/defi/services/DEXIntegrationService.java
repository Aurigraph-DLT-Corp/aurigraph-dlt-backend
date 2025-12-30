package io.aurigraph.v11.defi.services;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Service interface for managing Decentralized Exchange (DEX) operations and integrations.
 * 
 * This service provides a unified interface for interacting with various DEX protocols
 * including Uniswap, SushiSwap, PancakeSwap, and other automated market makers (AMMs).
 * It handles token swaps, liquidity provision, price discovery, and arbitrage opportunities.
 * 
 * Key Features:
 * - Multi-DEX aggregation and routing
 * - Automated optimal path finding
 * - Slippage protection and MEV resistance
 * - Cross-chain DEX operations
 * - Real-time price feeds and arbitrage detection
 * - Advanced order types (limit, stop-loss, DCA)
 * 
 * Performance Requirements:
 * - Support for 500K+ swaps per day
 * - Sub-100ms price quote responses
 * - Real-time liquidity monitoring across 50+ DEXes
 * 
 * @author Aurigraph DLT Platform
 * @version 11.0.0
 * @since 2025-01-01
 */
@ApplicationScoped
public interface DEXIntegrationService {

    /**
     * Executes a token swap using the optimal route across available DEXes.
     * 
     * @param tokenIn the input token address
     * @param tokenOut the output token address
     * @param amountIn the amount of input tokens
     * @param minAmountOut the minimum acceptable output amount (slippage protection)
     * @param recipient the recipient address for output tokens
     * @param deadline the swap deadline timestamp
     * @return Uni containing the swap execution result
     */
    Uni<SwapResult> executeSwap(
        String tokenIn,
        String tokenOut,
        BigDecimal amountIn,
        BigDecimal minAmountOut,
        String recipient,
        long deadline
    );

    /**
     * Gets the best quote for a token swap across all available DEXes.
     * 
     * @param tokenIn the input token address
     * @param tokenOut the output token address
     * @param amountIn the amount of input tokens
     * @return Uni containing the best swap quote with routing information
     */
    Uni<SwapQuote> getBestSwapQuote(String tokenIn, String tokenOut, BigDecimal amountIn);

    /**
     * Executes a multi-hop swap through multiple token pairs.
     * 
     * @param swapPath the ordered list of tokens in the swap path
     * @param amountIn the amount of initial input tokens
     * @param minAmountOut the minimum acceptable final output amount
     * @param recipient the recipient address for final output tokens
     * @param deadline the swap deadline timestamp
     * @return Uni containing the multi-hop swap result
     */
    Uni<SwapResult> executeMultiHopSwap(
        List<String> swapPath,
        BigDecimal amountIn,
        BigDecimal minAmountOut,
        String recipient,
        long deadline
    );

    /**
     * Adds liquidity to a DEX liquidity pool.
     * 
     * @param tokenA the first token address
     * @param tokenB the second token address
     * @param amountADesired the desired amount of tokenA
     * @param amountBDesired the desired amount of tokenB
     * @param amountAMin the minimum amount of tokenA to add
     * @param amountBMin the minimum amount of tokenB to add
     * @param recipient the recipient address for LP tokens
     * @param deadline the transaction deadline timestamp
     * @param dexId the DEX protocol identifier
     * @return Uni containing the liquidity addition result
     */
    Uni<LiquidityResult> addLiquidity(
        String tokenA,
        String tokenB,
        BigDecimal amountADesired,
        BigDecimal amountBDesired,
        BigDecimal amountAMin,
        BigDecimal amountBMin,
        String recipient,
        long deadline,
        String dexId
    );

    /**
     * Removes liquidity from a DEX liquidity pool.
     * 
     * @param tokenA the first token address
     * @param tokenB the second token address
     * @param lpTokenAmount the amount of LP tokens to burn
     * @param amountAMin the minimum amount of tokenA to receive
     * @param amountBMin the minimum amount of tokenB to receive
     * @param recipient the recipient address for underlying tokens
     * @param deadline the transaction deadline timestamp
     * @param dexId the DEX protocol identifier
     * @return Uni containing the liquidity removal result
     */
    Uni<LiquidityResult> removeLiquidity(
        String tokenA,
        String tokenB,
        BigDecimal lpTokenAmount,
        BigDecimal amountAMin,
        BigDecimal amountBMin,
        String recipient,
        long deadline,
        String dexId
    );

    /**
     * Gets current price information for a token pair across all DEXes.
     * 
     * @param tokenA the first token address
     * @param tokenB the second token address
     * @return Uni containing aggregated price information
     */
    Uni<TokenPairPrice> getTokenPairPrice(String tokenA, String tokenB);

    /**
     * Monitors price changes for specified token pairs in real-time.
     * 
     * @param tokenPairs the list of token pairs to monitor
     * @param priceChangeThreshold the minimum price change threshold for notifications
     * @return Multi streaming price updates for monitored pairs
     */
    Multi<PriceUpdate> monitorPriceChanges(List<TokenPair> tokenPairs, BigDecimal priceChangeThreshold);

    /**
     * Identifies arbitrage opportunities across different DEXes.
     * 
     * @param tokenPairs the token pairs to analyze for arbitrage
     * @param minProfitThreshold the minimum profit threshold for opportunities
     * @return Multi streaming arbitrage opportunities
     */
    Multi<ArbitrageOpportunity> identifyArbitrageOpportunities(
        List<TokenPair> tokenPairs, 
        BigDecimal minProfitThreshold
    );

    /**
     * Executes an arbitrage trade across multiple DEXes.
     * 
     * @param opportunity the arbitrage opportunity to execute
     * @param maxGasPrice the maximum acceptable gas price
     * @return Uni containing the arbitrage execution result
     */
    Uni<ArbitrageResult> executeArbitrage(ArbitrageOpportunity opportunity, BigDecimal maxGasPrice);

    /**
     * Places a limit order that executes when price conditions are met.
     * 
     * @param tokenIn the input token address
     * @param tokenOut the output token address
     * @param amountIn the amount of input tokens
     * @param limitPrice the limit price for execution
     * @param orderType the type of limit order
     * @param expiryTimestamp the order expiry timestamp
     * @return Uni containing the limit order details
     */
    Uni<LimitOrder> placeLimitOrder(
        String tokenIn,
        String tokenOut,
        BigDecimal amountIn,
        BigDecimal limitPrice,
        LimitOrderType orderType,
        long expiryTimestamp
    );

    /**
     * Cancels an existing limit order.
     * 
     * @param orderId the limit order identifier
     * @return Uni containing the cancellation result
     */
    Uni<Boolean> cancelLimitOrder(String orderId);

    /**
     * Gets all active limit orders for a user.
     * 
     * @param userAddress the user's wallet address
     * @return Multi streaming active limit orders
     */
    Multi<LimitOrder> getUserLimitOrders(String userAddress);

    /**
     * Gets liquidity pool information for a token pair on a specific DEX.
     * 
     * @param tokenA the first token address
     * @param tokenB the second token address
     * @param dexId the DEX protocol identifier
     * @return Uni containing the liquidity pool information
     */
    Uni<LiquidityPoolInfo> getLiquidityPoolInfo(String tokenA, String tokenB, String dexId);

    /**
     * Calculates impermanent loss for a liquidity position.
     * 
     * @param tokenA the first token address
     * @param tokenB the second token address
     * @param initialAmountA the initial amount of tokenA provided
     * @param initialAmountB the initial amount of tokenB provided
     * @param initialPriceRatio the initial price ratio (tokenA/tokenB)
     * @param currentPriceRatio the current price ratio (tokenA/tokenB)
     * @return Uni containing the impermanent loss calculation
     */
    Uni<ImpermanentLossInfo> calculateImpermanentLoss(
        String tokenA,
        String tokenB,
        BigDecimal initialAmountA,
        BigDecimal initialAmountB,
        BigDecimal initialPriceRatio,
        BigDecimal currentPriceRatio
    );

    /**
     * Gets historical trading volume data for analysis.
     * 
     * @param tokenPair the token pair to analyze
     * @param dexId the DEX protocol identifier (null for all DEXes)
     * @param fromTimestamp the start timestamp for historical data
     * @param toTimestamp the end timestamp for historical data
     * @return Multi streaming historical volume data
     */
    Multi<VolumeData> getHistoricalVolume(
        TokenPair tokenPair,
        String dexId,
        long fromTimestamp,
        long toTimestamp
    );

    // Inner classes and enums for data transfer objects
    
    /**
     * Represents the result of a token swap operation.
     */
    public static class SwapResult {
        public String transactionHash;
        public String tokenIn;
        public String tokenOut;
        public BigDecimal amountIn;
        public BigDecimal amountOut;
        public BigDecimal executionPrice;
        public BigDecimal slippage;
        public BigDecimal gasCost;
        public List<String> routingPath;
        public String dexId;
        public long executedTimestamp;
        public boolean success;
        public String errorMessage;
    }

    /**
     * Contains the best quote information for a token swap.
     */
    public static class SwapQuote {
        public String tokenIn;
        public String tokenOut;
        public BigDecimal amountIn;
        public BigDecimal amountOut;
        public BigDecimal price;
        public BigDecimal priceImpact;
        public BigDecimal estimatedGas;
        public List<RouteInfo> routes;
        public String bestDexId;
        public long quoteTimestamp;
        public long validUntil;
    }

    /**
     * Contains routing information for a swap path.
     */
    public static class RouteInfo {
        public String dexId;
        public List<String> path;
        public BigDecimal outputAmount;
        public BigDecimal priceImpact;
        public BigDecimal gasEstimate;
        public BigDecimal liquidityDepth;
    }

    /**
     * Represents the result of liquidity operations.
     */
    public static class LiquidityResult {
        public String transactionHash;
        public String tokenA;
        public String tokenB;
        public BigDecimal amountA;
        public BigDecimal amountB;
        public BigDecimal lpTokensMinted;
        public BigDecimal lpTokensBurned;
        public String poolAddress;
        public String dexId;
        public boolean success;
        public String errorMessage;
    }

    /**
     * Represents a token pair for trading operations.
     */
    public static class TokenPair {
        public String tokenA;
        public String tokenB;
        public String pairAddress;
        public String dexId;

        public TokenPair(String tokenA, String tokenB) {
            this.tokenA = tokenA;
            this.tokenB = tokenB;
        }
    }

    /**
     * Contains price information for a token pair.
     */
    public static class TokenPairPrice {
        public String tokenA;
        public String tokenB;
        public BigDecimal price; // tokenA/tokenB
        public BigDecimal inversePrice; // tokenB/tokenA
        public BigDecimal volume24h;
        public BigDecimal priceChange24h;
        public Map<String, BigDecimal> dexPrices; // dexId -> price
        public long lastUpdated;
    }

    /**
     * Represents a real-time price update.
     */
    public static class PriceUpdate {
        public TokenPair tokenPair;
        public BigDecimal oldPrice;
        public BigDecimal newPrice;
        public BigDecimal priceChange;
        public BigDecimal priceChangePercent;
        public String dexId;
        public long timestamp;
    }

    /**
     * Represents an arbitrage opportunity between DEXes.
     */
    public static class ArbitrageOpportunity {
        public TokenPair tokenPair;
        public String buyDex;
        public String sellDex;
        public BigDecimal buyPrice;
        public BigDecimal sellPrice;
        public BigDecimal priceDifference;
        public BigDecimal profitPercent;
        public BigDecimal maxProfitAmount;
        public BigDecimal requiredCapital;
        public BigDecimal estimatedGasCost;
        public BigDecimal netProfit;
        public long detectedTimestamp;
        public long validUntil;
    }

    /**
     * Represents the result of an arbitrage execution.
     */
    public static class ArbitrageResult {
        public String opportunityId;
        public String buyTransactionHash;
        public String sellTransactionHash;
        public BigDecimal actualProfit;
        public BigDecimal totalGasCost;
        public BigDecimal netProfit;
        public BigDecimal executionTime;
        public boolean success;
        public String errorMessage;
    }

    /**
     * Represents a limit order.
     */
    public static class LimitOrder {
        public String orderId;
        public String userAddress;
        public String tokenIn;
        public String tokenOut;
        public BigDecimal amountIn;
        public BigDecimal limitPrice;
        public LimitOrderType orderType;
        public OrderStatus status;
        public long createdTimestamp;
        public long expiryTimestamp;
        public String executionTransactionHash;
        public BigDecimal executedAmount;
        public BigDecimal executedPrice;
    }

    /**
     * Types of limit orders.
     */
    public enum LimitOrderType {
        BUY_LIMIT,
        SELL_LIMIT,
        STOP_LOSS,
        TAKE_PROFIT
    }

    /**
     * Status of limit orders.
     */
    public enum OrderStatus {
        PENDING,
        PARTIALLY_FILLED,
        FILLED,
        CANCELLED,
        EXPIRED
    }

    /**
     * Contains liquidity pool information.
     */
    public static class LiquidityPoolInfo {
        public String poolAddress;
        public String tokenA;
        public String tokenB;
        public BigDecimal reserveA;
        public BigDecimal reserveB;
        public BigDecimal totalSupply;
        public BigDecimal price;
        public BigDecimal volume24h;
        public BigDecimal fees24h;
        public BigDecimal apr;
        public String dexId;
        public long lastUpdated;
    }

    /**
     * Contains impermanent loss calculation information.
     */
    public static class ImpermanentLossInfo {
        public String tokenA;
        public String tokenB;
        public BigDecimal impermanentLoss;
        public BigDecimal impermanentLossPercent;
        public BigDecimal holdValue;
        public BigDecimal lpValue;
        public BigDecimal feesEarned;
        public BigDecimal netResult;
        public long calculationTimestamp;
    }

    /**
     * Represents historical volume data.
     */
    public static class VolumeData {
        public TokenPair tokenPair;
        public String dexId;
        public BigDecimal volume;
        public BigDecimal volumeUSD;
        public int transactionCount;
        public long timestamp;
        public String timeframe; // "1h", "1d", "1w", etc.
    }
}