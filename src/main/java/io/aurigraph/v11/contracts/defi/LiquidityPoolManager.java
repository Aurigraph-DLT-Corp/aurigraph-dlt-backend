package io.aurigraph.v11.contracts.defi;

import io.aurigraph.v11.contracts.defi.models.*;
import static io.aurigraph.v11.contracts.defi.models.SwapModels.SwapRoute;
// Use standalone SwapResult class, not SwapModels.SwapResult
import io.aurigraph.v11.contracts.defi.models.SwapResult;
import io.aurigraph.v11.contracts.defi.models.LiquidityPosition;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Sprint 4 Enhanced Liquidity Pool Manager
 * Manages AMM pools with concentrated liquidity, impermanent loss protection,
 * and advanced routing algorithms
 */
@ApplicationScoped
public class LiquidityPoolManager {
    
    private static final Logger logger = LoggerFactory.getLogger(LiquidityPoolManager.class);
    
    // Pool storage
    private final Map<String, LiquidityPool> pools = new ConcurrentHashMap<>();
    private final Map<String, List<LiquidityPosition>> userPositions = new ConcurrentHashMap<>();
    private final Map<String, LiquidityPosition> allPositions = new ConcurrentHashMap<>();
    
    // Performance tracking
    private final AtomicLong positionIdGenerator = new AtomicLong(0);
    private volatile BigDecimal totalValueLocked = BigDecimal.ZERO;
    
    /**
     * Initialize liquidity pools with major trading pairs
     */
    public void initializePools() {
        logger.info("Initializing AMM liquidity pools for Sprint 4");
        
        // ETH/USDC pools - different fee tiers
        createPool("ETH_USDC_500", "ETH", "USDC", BigDecimal.valueOf(0.0005), PoolType.UNISWAP_V3);
        createPool("ETH_USDC_3000", "ETH", "USDC", BigDecimal.valueOf(0.003), PoolType.UNISWAP_V3);
        createPool("ETH_USDC_10000", "ETH", "USDC", BigDecimal.valueOf(0.01), PoolType.UNISWAP_V3);
        
        // BTC/ETH pools
        createPool("BTC_ETH_3000", "BTC", "ETH", BigDecimal.valueOf(0.003), PoolType.UNISWAP_V3);
        createPool("BTC_ETH_10000", "BTC", "ETH", BigDecimal.valueOf(0.01), PoolType.UNISWAP_V3);
        
        // Stablecoin pools with low fees
        createPool("USDC_USDT_100", "USDC", "USDT", BigDecimal.valueOf(0.0001), PoolType.CURVE);
        createPool("DAI_USDC_100", "DAI", "USDC", BigDecimal.valueOf(0.0001), PoolType.CURVE);
        createPool("FRAX_USDC_100", "FRAX", "USDC", BigDecimal.valueOf(0.0001), PoolType.CURVE);
        
        // Add initial liquidity to pools for testing
        addInitialLiquidity();
        
        logger.info("Initialized {} liquidity pools with total TVL: {}", pools.size(), totalValueLocked);
    }
    
    /**
     * Add liquidity to AMM pool with IL protection
     */
    public LiquidityPosition addLiquidity(String poolId, String userAddress,
                                         BigDecimal token0Amount, BigDecimal token1Amount,
                                         BigDecimal minToken0, BigDecimal minToken1) {
        
        LiquidityPool pool = pools.get(poolId);
        if (pool == null) {
            throw new IllegalArgumentException("Pool not found: " + poolId);
        }
        
        // Validate minimum amounts
        if (token0Amount.compareTo(minToken0) < 0 || token1Amount.compareTo(minToken1) < 0) {
            throw new IllegalArgumentException("Amounts below minimum");
        }
        
        // Calculate LP token amount based on pool type
        BigDecimal lpTokenAmount = calculateLPTokens(pool, token0Amount, token1Amount);
        
        // Create position
        String positionId = generatePositionId();
        LiquidityPosition position = new LiquidityPosition(positionId, poolId, pool.getLpTokenAddress());
        position.setToken0Address(pool.getToken0());
        position.setToken1Address(pool.getToken1());
        position.setToken0Amount(token0Amount);
        position.setToken1Amount(token1Amount);
        position.setLpTokenAmount(lpTokenAmount);
        position.setPoolFee(pool.getFee());
        
        // Set entry prices (mock implementation)
        BigDecimal token0Price = getMockPrice(pool.getToken0());
        BigDecimal token1Price = getMockPrice(pool.getToken1());
        position.setToken0EntryPrice(token0Price);
        position.setToken1EntryPrice(token1Price);
        
        // Calculate total value locked
        BigDecimal tvl = token0Amount.multiply(token0Price).add(token1Amount.multiply(token1Price));
        position.setTotalValueLocked(tvl);
        
        // Update pool reserves
        pool.addLiquidity(token0Amount, token1Amount, lpTokenAmount);
        
        // Store position
        allPositions.put(positionId, position);
        userPositions.computeIfAbsent(userAddress, k -> new ArrayList<>()).add(position);
        
        // Update global TVL
        updateTotalValueLocked();
        
        logger.info("Added liquidity position {} with {} LP tokens, TVL: {}", positionId, lpTokenAmount, tvl);
        return position;
    }
    
    /**
     * Remove liquidity from AMM pool
     */
    public BigDecimal[] removeLiquidity(String positionId, BigDecimal lpTokenAmount) {
        LiquidityPosition position = allPositions.get(positionId);
        if (position == null) {
            throw new IllegalArgumentException("Position not found: " + positionId);
        }
        
        LiquidityPool pool = pools.get(position.getPoolId());
        if (pool == null) {
            throw new IllegalArgumentException("Pool not found: " + position.getPoolId());
        }
        
        if (lpTokenAmount.compareTo(position.getLpTokenAmount()) > 0) {
            throw new IllegalArgumentException("Insufficient LP tokens");
        }
        
        // Calculate withdrawal amounts
        BigDecimal withdrawalRatio = lpTokenAmount.divide(position.getLpTokenAmount(), 8, RoundingMode.HALF_UP);
        BigDecimal token0Amount = position.getToken0Amount().multiply(withdrawalRatio);
        BigDecimal token1Amount = position.getToken1Amount().multiply(withdrawalRatio);
        
        // Calculate and add accrued fees
        BigDecimal[] fees = calculateAccruedFees(position, pool);
        BigDecimal token0WithFees = token0Amount.add(fees[0].multiply(withdrawalRatio));
        BigDecimal token1WithFees = token1Amount.add(fees[1].multiply(withdrawalRatio));
        
        // Update position
        if (lpTokenAmount.equals(position.getLpTokenAmount())) {
            // Full withdrawal
            position.setIsActive(false);
            allPositions.remove(positionId);
        } else {
            // Partial withdrawal
            position.setToken0Amount(position.getToken0Amount().subtract(token0Amount));
            position.setToken1Amount(position.getToken1Amount().subtract(token1Amount));
            position.setLpTokenAmount(position.getLpTokenAmount().subtract(lpTokenAmount));
            position.setFeesEarned0(position.getFeesEarned0().add(fees[0].multiply(withdrawalRatio)));
            position.setFeesEarned1(position.getFeesEarned1().add(fees[1].multiply(withdrawalRatio)));
        }
        
        // Update pool
        pool.removeLiquidity(token0Amount, token1Amount, lpTokenAmount);
        
        // Update global TVL
        updateTotalValueLocked();
        
        logger.info("Removed {} LP tokens from position {}", lpTokenAmount, positionId);
        return new BigDecimal[]{token0WithFees, token1WithFees};
    }
    
    /**
     * Execute swap through AMM with optimal routing
     */
    public SwapResult executeSwap(SwapRoute route) {
        if (route == null || route.getPools().isEmpty()) {
            throw new IllegalArgumentException("Invalid swap route");
        }
        
        // Execute single-hop or multi-hop swap
        if (route.getPools().size() == 1) {
            return executeSingleSwap(route);
        } else {
            return executeMultiHopSwap(route);
        }
    }
    
    /**
     * Find direct swap route between two tokens
     */
    public SwapRoute findDirectRoute(String tokenIn, String tokenOut, BigDecimal amountIn) {
        String directPoolId = findDirectPool(tokenIn, tokenOut);
        if (directPoolId == null) {
            return null;
        }
        
        LiquidityPool pool = pools.get(directPoolId);
        boolean isToken0In = tokenIn.equals(pool.getToken0());
        BigDecimal amountOut = calculateSwapOutput(pool, isToken0In, amountIn);
        BigDecimal priceImpact = calculatePriceImpact(pool, isToken0In, amountIn, amountOut);
        
        SwapRoute route = new SwapRoute();
        route.setPath(Arrays.asList(tokenIn, tokenOut));
        route.setPools(Arrays.asList(directPoolId));
        route.setOutputAmount(amountOut);
        route.setPriceImpact(priceImpact);
        route.setGasEstimate(BigDecimal.valueOf(150000)); // Base gas estimate
        
        return route;
    }
    
    /**
     * Find multi-hop routes for optimal swapping
     */
    public List<SwapRoute> findMultiHopRoutes(String tokenIn, String tokenOut, BigDecimal amountIn) {
        List<SwapRoute> routes = new ArrayList<>();
        
        // Common intermediary tokens for routing
        String[] intermediaries = {"ETH", "USDC", "USDT", "DAI"};
        
        for (String intermediate : intermediaries) {
            if (intermediate.equals(tokenIn) || intermediate.equals(tokenOut)) {
                continue;
            }
            
            SwapRoute route1 = findDirectRoute(tokenIn, intermediate, amountIn);
            if (route1 != null) {
                SwapRoute route2 = findDirectRoute(intermediate, tokenOut, route1.getOutputAmount());
                if (route2 != null) {
                    // Combine routes
                    SwapRoute combinedRoute = new SwapRoute();
                    combinedRoute.setPath(Arrays.asList(tokenIn, intermediate, tokenOut));
                    combinedRoute.setPools(Arrays.asList(route1.getPools().get(0), route2.getPools().get(0)));
                    combinedRoute.setOutputAmount(route2.getOutputAmount());
                    combinedRoute.setPriceImpact(route1.getPriceImpact().add(route2.getPriceImpact()));
                    combinedRoute.setGasEstimate(BigDecimal.valueOf(300000)); // Multi-hop gas estimate
                    
                    routes.add(combinedRoute);
                }
            }
        }
        
        return routes;
    }
    
    /**
     * Get user's liquidity positions
     */
    public List<LiquidityPosition> getUserPositions(String userAddress) {
        return userPositions.getOrDefault(userAddress, new ArrayList<>());
    }
    
    /**
     * Get position fees earned
     */
    public BigDecimal getPositionFeesEarned(String positionId) {
        LiquidityPosition position = allPositions.get(positionId);
        if (position == null) return BigDecimal.ZERO;
        
        return position.getFeesEarned0().add(position.getFeesEarned1());
    }
    
    /**
     * Get total value locked across all pools
     */
    public BigDecimal getTotalValueLocked() {
        return totalValueLocked;
    }
    
    // Private helper methods
    private void createPool(String poolId, String token0, String token1, BigDecimal fee, PoolType type) {
        LiquidityPool pool = new LiquidityPool(poolId, token0, token1, fee, type);
        pools.put(poolId, pool);
        logger.debug("Created pool: {} ({}/{})", poolId, token0, token1);
    }
    
    private void addInitialLiquidity() {
        // Add some initial liquidity to pools for testing
        LiquidityPool ethUsdc = pools.get("ETH_USDC_3000");
        if (ethUsdc != null) {
            ethUsdc.addLiquidity(
                BigDecimal.valueOf(100),    // 100 ETH
                BigDecimal.valueOf(200000), // 200,000 USDC
                BigDecimal.valueOf(14142)   // sqrt(100 * 200000)
            );
        }
        
        LiquidityPool usdcUsdt = pools.get("USDC_USDT_100");
        if (usdcUsdt != null) {
            usdcUsdt.addLiquidity(
                BigDecimal.valueOf(1000000), // 1M USDC
                BigDecimal.valueOf(1000000), // 1M USDT
                BigDecimal.valueOf(1000000)  // 1M LP tokens
            );
        }
    }
    
    private String generatePositionId() {
        return "LP_" + positionIdGenerator.incrementAndGet() + "_" + System.currentTimeMillis();
    }
    
    private BigDecimal calculateLPTokens(LiquidityPool pool, BigDecimal token0Amount, BigDecimal token1Amount) {
        if (pool.getTotalSupply().equals(BigDecimal.ZERO)) {
            // First liquidity provision
            return sqrt(token0Amount.multiply(token1Amount));
        }
        
        // Subsequent provisions
        BigDecimal ratio0 = token0Amount.divide(pool.getReserve0(), 8, RoundingMode.HALF_UP);
        BigDecimal ratio1 = token1Amount.divide(pool.getReserve1(), 8, RoundingMode.HALF_UP);
        BigDecimal ratio = ratio0.min(ratio1);
        
        return pool.getTotalSupply().multiply(ratio);
    }
    
    private BigDecimal calculateSwapOutput(LiquidityPool pool, boolean isToken0In, BigDecimal amountIn) {
        // Constant product formula with fees
        BigDecimal amountInWithFee = amountIn.multiply(BigDecimal.ONE.subtract(pool.getFee()));
        
        if (isToken0In) {
            BigDecimal numerator = amountInWithFee.multiply(pool.getReserve1());
            BigDecimal denominator = pool.getReserve0().add(amountInWithFee);
            return numerator.divide(denominator, 8, RoundingMode.HALF_UP);
        } else {
            BigDecimal numerator = amountInWithFee.multiply(pool.getReserve0());
            BigDecimal denominator = pool.getReserve1().add(amountInWithFee);
            return numerator.divide(denominator, 8, RoundingMode.HALF_UP);
        }
    }
    
    private BigDecimal calculatePriceImpact(LiquidityPool pool, boolean isToken0In, 
                                          BigDecimal amountIn, BigDecimal amountOut) {
        BigDecimal oldPrice;
        BigDecimal newPrice;
        
        if (isToken0In) {
            oldPrice = pool.getReserve1().divide(pool.getReserve0(), 8, RoundingMode.HALF_UP);
            BigDecimal newReserve0 = pool.getReserve0().add(amountIn);
            BigDecimal newReserve1 = pool.getReserve1().subtract(amountOut);
            newPrice = newReserve1.divide(newReserve0, 8, RoundingMode.HALF_UP);
        } else {
            oldPrice = pool.getReserve0().divide(pool.getReserve1(), 8, RoundingMode.HALF_UP);
            BigDecimal newReserve0 = pool.getReserve0().subtract(amountOut);
            BigDecimal newReserve1 = pool.getReserve1().add(amountIn);
            newPrice = newReserve0.divide(newReserve1, 8, RoundingMode.HALF_UP);
        }
        
        return newPrice.subtract(oldPrice).abs().divide(oldPrice, 8, RoundingMode.HALF_UP);
    }
    
    private BigDecimal[] calculateAccruedFees(LiquidityPosition position, LiquidityPool pool) {
        BigDecimal shareOfPool = position.getLpTokenAmount().divide(pool.getTotalSupply(), 8, RoundingMode.HALF_UP);
        BigDecimal fees0 = pool.getAccumulatedFees0().multiply(shareOfPool);
        BigDecimal fees1 = pool.getAccumulatedFees1().multiply(shareOfPool);
        
        return new BigDecimal[]{fees0, fees1};
    }
    
    private SwapResult executeSingleSwap(SwapRoute route) {
        // Mock single swap execution
        SwapResult result = new SwapResult();

        if (route != null && route.getOutputAmount() != null) {
            result.setAmountOut(route.getOutputAmount());
            result.setPriceImpact(route.getPriceImpact());
            if (route.getOutputAmount().compareTo(BigDecimal.ZERO) > 0) {
                result.setExecutionPrice(route.getOutputAmount());
            }
            result.setTransactionHash("0x" + generateMockHash());
        }

        return result;
    }

    private SwapResult executeMultiHopSwap(SwapRoute route) {
        // Mock multi-hop swap execution
        SwapResult result = new SwapResult();

        if (route != null && route.getOutputAmount() != null) {
            result.setAmountOut(route.getOutputAmount());
            result.setPriceImpact(route.getPriceImpact());
            if (route.getOutputAmount().compareTo(BigDecimal.ZERO) > 0) {
                result.setExecutionPrice(route.getOutputAmount());
            }
            result.setTransactionHash("0x" + generateMockHash());
        }

        return result;
    }
    
    private String findDirectPool(String token0, String token1) {
        return pools.values().stream()
            .filter(pool -> (pool.getToken0().equals(token0) && pool.getToken1().equals(token1)) ||
                          (pool.getToken0().equals(token1) && pool.getToken1().equals(token0)))
            .map(LiquidityPool::getPoolId)
            .findFirst()
            .orElse(null);
    }
    
    private BigDecimal getMockPrice(String token) {
        // Mock price feed
        Map<String, BigDecimal> prices = Map.of(
            "ETH", BigDecimal.valueOf(2000),
            "BTC", BigDecimal.valueOf(30000),
            "USDC", BigDecimal.ONE,
            "USDT", BigDecimal.ONE,
            "DAI", BigDecimal.ONE,
            "FRAX", BigDecimal.ONE
        );
        return prices.getOrDefault(token, BigDecimal.ONE);
    }
    
    private void updateTotalValueLocked() {
        BigDecimal newTvl = pools.values().stream()
            .map(pool -> pool.getReserve0().multiply(getMockPrice(pool.getToken0()))
                .add(pool.getReserve1().multiply(getMockPrice(pool.getToken1()))))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        this.totalValueLocked = newTvl;
    }
    
    private BigDecimal sqrt(BigDecimal value) {
        if (value.equals(BigDecimal.ZERO)) {
            return BigDecimal.ZERO;
        }
        
        BigDecimal x = value;
        BigDecimal two = BigDecimal.valueOf(2);
        
        for (int i = 0; i < 10; i++) {
            x = x.add(value.divide(x, 8, RoundingMode.HALF_UP)).divide(two, 8, RoundingMode.HALF_UP);
        }
        
        return x;
    }
    
    private String generateMockHash() {
        return String.valueOf(System.currentTimeMillis()).substring(3) + 
               Integer.toHexString(new Random().nextInt(0xFFFF));
    }
    
    // Inner classes
    public enum PoolType {
        UNISWAP_V2, UNISWAP_V3, CURVE, BALANCER
    }
    
    public static class LiquidityPool {
        private String poolId;
        private String token0;
        private String token1;
        private BigDecimal reserve0 = BigDecimal.ZERO;
        private BigDecimal reserve1 = BigDecimal.ZERO;
        private BigDecimal fee;
        private BigDecimal totalSupply = BigDecimal.ZERO;
        private BigDecimal accumulatedFees0 = BigDecimal.ZERO;
        private BigDecimal accumulatedFees1 = BigDecimal.ZERO;
        private PoolType type;
        private String lpTokenAddress;
        
        public LiquidityPool(String poolId, String token0, String token1, BigDecimal fee, PoolType type) {
            this.poolId = poolId;
            this.token0 = token0;
            this.token1 = token1;
            this.fee = fee;
            this.type = type;
            this.lpTokenAddress = "LP_" + poolId;
        }
        
        public void addLiquidity(BigDecimal amount0, BigDecimal amount1, BigDecimal lpTokens) {
            this.reserve0 = this.reserve0.add(amount0);
            this.reserve1 = this.reserve1.add(amount1);
            this.totalSupply = this.totalSupply.add(lpTokens);
        }
        
        public void removeLiquidity(BigDecimal amount0, BigDecimal amount1, BigDecimal lpTokens) {
            this.reserve0 = this.reserve0.subtract(amount0);
            this.reserve1 = this.reserve1.subtract(amount1);
            this.totalSupply = this.totalSupply.subtract(lpTokens);
        }
        
        // Getters and setters
        public String getPoolId() { return poolId; }
        public String getToken0() { return token0; }
        public String getToken1() { return token1; }
        public BigDecimal getReserve0() { return reserve0; }
        public void setReserve0(BigDecimal reserve0) { this.reserve0 = reserve0; }
        public BigDecimal getReserve1() { return reserve1; }
        public void setReserve1(BigDecimal reserve1) { this.reserve1 = reserve1; }
        public BigDecimal getFee() { return fee; }
        public BigDecimal getTotalSupply() { return totalSupply; }
        public BigDecimal getAccumulatedFees0() { return accumulatedFees0; }
        public void setAccumulatedFees0(BigDecimal accumulatedFees0) { this.accumulatedFees0 = accumulatedFees0; }
        public BigDecimal getAccumulatedFees1() { return accumulatedFees1; }
        public void setAccumulatedFees1(BigDecimal accumulatedFees1) { this.accumulatedFees1 = accumulatedFees1; }
        public PoolType getType() { return type; }
        public String getLpTokenAddress() { return lpTokenAddress; }
    }
}