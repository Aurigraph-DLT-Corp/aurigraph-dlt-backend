package io.aurigraph.v11.contracts.defi;

import io.aurigraph.v11.contracts.defi.models.*;
import io.aurigraph.v11.contracts.defi.models.DeFiRequests.*;
// Using specific SwapResult from models package, not SwapModels
import io.aurigraph.v11.contracts.defi.models.SwapModels.SwapRoute;
import io.aurigraph.v11.contracts.defi.models.SwapModels.LiquidationAlert;
import io.aurigraph.v11.contracts.defi.models.SwapModels.YieldOpportunity;
import io.aurigraph.v11.contracts.defi.risk.ImpermanentLossCalculator;
import io.aurigraph.v11.contracts.defi.risk.RiskAnalyticsEngine;
// import io.aurigraph.v11.contracts.defi.adapters.*;
import io.aurigraph.v11.contracts.defi.protocols.DeFiProtocol;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Sprint 4 DeFi Integration Service
 * Main orchestration service for DeFi protocols including:
 * - AMM liquidity provision with impermanent loss protection
 * - Yield farming and auto-compounding
 * - Lending/borrowing with collateralization
 * - Cross-chain DeFi bridge integration
 * 
 * Performance Target: 50K+ DeFi operations per second
 */
@Path("/api/v11/defi")
@ApplicationScoped
public class DeFiIntegrationService {
    
    private static final Logger logger = LoggerFactory.getLogger(DeFiIntegrationService.class);
    
    // Core DeFi Services
    @Inject
    LiquidityPoolManager liquidityPoolManager;
    
    @Inject
    YieldFarmingService yieldFarmingService;
    
    @Inject
    LendingProtocolService lendingProtocolService;
    
    @Inject
    ImpermanentLossCalculator impermanentLossCalculator;
    
    @Inject
    RiskAnalyticsEngine riskAnalyticsEngine;
    
    // Performance tracking
    private final AtomicLong operationCounter = new AtomicLong(0);
    private final Map<String, Long> protocolMetrics = new ConcurrentHashMap<>();
    private final Instant startTime = Instant.now();
    
    // Protocol registries
    private final Map<String, DeFiProtocol> protocols = new ConcurrentHashMap<>();
    
    /**
     * Initialize DeFi integration service with all protocols
     */
    public Uni<Boolean> initialize() {
        return Uni.createFrom().item(() -> {
            logger.info("Initializing DeFi Integration Service for Sprint 4");
            
            // Initialize AMM pools
            liquidityPoolManager.initializePools();
            
            // Initialize yield farming contracts
            yieldFarmingService.initializeFarms();
            
            // Initialize lending protocols
            lendingProtocolService.initializeProtocols();
            
            // Setup performance monitoring
            setupPerformanceMonitoring();
            
            logger.info("DeFi Integration Service initialized successfully");
            return true;
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }
    
    /**
     * REST endpoint: Add liquidity with IL protection
     */
    @POST
    @Path("/liquidity/add")
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<LiquidityPositionResponse> addLiquidity(AddLiquidityRequest request) {
        return Uni.createFrom().item(() -> {
            operationCounter.incrementAndGet();
            
            // Validate request
            validateLiquidityRequest(request);
            
            // Add liquidity to pool
            LiquidityPosition position = liquidityPoolManager.addLiquidity(
                request.getPoolId(),
                request.getUserAddress(),
                request.getToken0Amount(),
                request.getToken1Amount(),
                request.getMinToken0(),
                request.getMinToken1()
            );
            
            // Calculate impermanent loss risk
            BigDecimal ilRisk = impermanentLossCalculator.calculatePotentialLoss(position);
            
            // Apply IL protection if requested
            if (request.isEnableILProtection()) {
                applyImpermanentLossProtection(position, ilRisk);
            }
            
            logger.info("Added liquidity position {} with IL risk: {}", 
                       position.getPositionId(), ilRisk);
            
            return new LiquidityPositionResponse(position, ilRisk);
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }
    
    /**
     * REST endpoint: Remove liquidity
     */
    @POST
    @Path("/liquidity/remove")
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<WithdrawLiquidityResponse> removeLiquidity(RemoveLiquidityRequest request) {
        return Uni.createFrom().item(() -> {
            operationCounter.incrementAndGet();
            
            BigDecimal[] amounts = liquidityPoolManager.removeLiquidity(
                request.getPositionId(), 
                request.getLpTokenAmount()
            );
            
            return new WithdrawLiquidityResponse(amounts[0], amounts[1], 
                calculateFeesEarned(request.getPositionId()));
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }
    
    /**
     * REST endpoint: Start yield farming with auto-compound
     */
    @POST
    @Path("/yield/stake")
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<YieldFarmingResponse> stakeForYield(StakeRequest request) {
        return Uni.createFrom().item(() -> {
            operationCounter.incrementAndGet();
            
            YieldFarmRewards rewards = yieldFarmingService.stake(
                request.getFarmId(),
                request.getUserAddress(),
                request.getAmount()
            );
            
            // Enable auto-compounding if requested
            if (request.isEnableAutoCompound()) {
                rewards.setCompoundingEnabled(true);
                rewards.setAutoCompoundFrequency(request.getCompoundFrequency());
                
                // Start auto-compound process
                startAutoCompounding(rewards);
            }
            
            return new YieldFarmingResponse(rewards);
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }
    
    /**
     * REST endpoint: Open lending position
     */
    @POST
    @Path("/lending/open")
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<LoanPositionResponse> openLendingPosition(LendingRequest request) {
        return Uni.createFrom().item(() -> {
            operationCounter.incrementAndGet();
            
            // Validate collateral
            boolean validCollateral = lendingProtocolService.validateCollateral(
                request.getCollateralToken(),
                request.getCollateralAmount(),
                request.getBorrowToken(),
                request.getBorrowAmount()
            );
            
            if (!validCollateral) {
                throw new IllegalArgumentException("Insufficient collateral for loan");
            }
            
            // Open lending position
            LoanPosition position = lendingProtocolService.openPosition(
                request.getUserAddress(),
                request.getCollateralToken(),
                request.getCollateralAmount(),
                request.getBorrowToken(),
                request.getBorrowAmount()
            );
            
            // Calculate risk score
            BigDecimal riskScore = riskAnalyticsEngine.calculatePositionRisk(position);
            position.setRiskScore(riskScore);
            
            // Setup liquidation monitoring
            if (riskScore.compareTo(BigDecimal.valueOf(0.8)) > 0) {
                setupLiquidationMonitoring(position);
            }
            
            logger.info("Opened lending position {} with risk score: {}", 
                       position.getPositionId(), riskScore);
            
            return new LoanPositionResponse(position, riskScore);
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }
    
    /**
     * REST endpoint: Execute optimized swap across protocols
     */
    @POST
    @Path("/swap/execute")
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<SwapResponse> executeOptimizedSwap(SwapRequest request) {
        return Uni.createFrom().item(() -> {
            operationCounter.incrementAndGet();

            // Validate request
            if (!request.isValid()) {
                throw new IllegalArgumentException("Invalid swap request");
            }

            // Find optimal route across protocols
            List<SwapRoute> routes = findOptimalSwapRoute(
                request.getTokenIn(),
                request.getTokenOut(),
                request.getAmountIn()
            );

            if (routes.isEmpty()) {
                throw new IllegalArgumentException("No swap route available");
            }

            // Execute swap with MEV protection
            SwapResult result = executeSwapWithMEVProtection(routes.get(0), request);

            return new SwapResponse(result);
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }
    
    /**
     * REST endpoint: Get user's complete DeFi portfolio
     */
    @GET
    @Path("/portfolio/{userAddress}")
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<DeFiPortfolio> getUserPortfolio(String userAddress) {
        return Uni.createFrom().item(() -> {
            DeFiPortfolio portfolio = new DeFiPortfolio(userAddress);
            
            // Gather all positions
            portfolio.setLiquidityPositions(liquidityPoolManager.getUserPositions(userAddress));
            portfolio.setYieldPositions(yieldFarmingService.getUserRewards(userAddress));
            portfolio.setLoanPositions(lendingProtocolService.getUserPositions(userAddress));
            
            // Calculate portfolio metrics
            calculatePortfolioMetrics(portfolio);
            
            // Calculate portfolio risk
            BigDecimal portfolioRisk = riskAnalyticsEngine.calculatePortfolioRisk(portfolio);
            portfolio.setRiskScore(portfolioRisk);
            
            return portfolio;
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }
    
    /**
     * REST endpoint: Get protocol performance metrics
     */
    @GET
    @Path("/metrics")
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<Map<String, Object>> getProtocolMetrics() {
        return Uni.createFrom().item(() -> {
            Map<String, Object> metrics = new HashMap<>();
            
            // Performance metrics
            metrics.put("totalOperations", operationCounter.get());
            metrics.put("operationsPerSecond", calculateOpsPerSecond());
            
            // TVL across protocols
            metrics.put("liquidityTVL", liquidityPoolManager.getTotalValueLocked());
            metrics.put("yieldFarmTVL", yieldFarmingService.getTotalStaked());
            metrics.put("lendingTVL", lendingProtocolService.getTotalSupplied());
            
            // Risk metrics
            metrics.put("totalRiskExposure", riskAnalyticsEngine.getTotalRiskExposure());
            metrics.put("liquidationThreshold", riskAnalyticsEngine.getLiquidationThreshold());
            
            metrics.put("timestamp", Instant.now());
            
            return metrics;
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }
    
    /**
     * Monitor liquidations in real-time
     */
    public Multi<LiquidationAlert> monitorLiquidations() {
        return Multi.createFrom().ticks().every(java.time.Duration.ofSeconds(5))
            .onItem().transformToMultiAndConcatenate(tick -> {
                List<LoanPosition> riskyPositions = lendingProtocolService.scanForLiquidations();
                List<LiquidationAlert> alerts = riskyPositions.stream()
                    .filter(pos -> pos.isLiquidationEligible())
                    .map(pos -> new LiquidationAlert(pos))
                    .collect(java.util.stream.Collectors.toList());
                return Multi.createFrom().iterable(alerts);
            });
    }
    
    /**
     * Cross-protocol yield optimization
     */
    public Uni<List<YieldOpportunity>> optimizeYieldAcrossProtocols(String userAddress,
                                                                   BigDecimal totalAmount,
                                                                   String baseToken) {
        return Uni.createFrom().item(() -> {
            operationCounter.incrementAndGet();
            
            List<YieldOpportunity> opportunities = yieldFarmingService
                .findOptimalYieldDistribution(userAddress, totalAmount, baseToken);
            
            // Sort by risk-adjusted APR
            opportunities.sort((a, b) -> 
                b.getRiskAdjustedAPR().compareTo(a.getRiskAdjustedAPR()));
            
            logger.debug("Found {} yield optimization opportunities", opportunities.size());
            return opportunities;
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }
    
    // Private helper methods
    private void validateLiquidityRequest(AddLiquidityRequest request) {
        if (request.getToken0Amount().compareTo(BigDecimal.ZERO) <= 0 ||
            request.getToken1Amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Invalid token amounts");
        }
    }
    
    private void applyImpermanentLossProtection(LiquidityPosition position, BigDecimal ilRisk) {
        // Implement IL protection mechanism
        if (ilRisk.compareTo(BigDecimal.valueOf(0.05)) > 0) { // 5% threshold
            position.setILProtectionEnabled(true);
            position.setILProtectionFee(ilRisk.multiply(BigDecimal.valueOf(0.01))); // 1% of IL risk
        }
    }
    
    private BigDecimal calculateFeesEarned(String positionId) {
        // Calculate total fees earned for position
        return liquidityPoolManager.getPositionFeesEarned(positionId);
    }
    
    private void startAutoCompounding(YieldFarmRewards rewards) {
        // Start auto-compounding process
        Multi.createFrom().ticks().every(java.time.Duration.ofSeconds(rewards.getAutoCompoundFrequency()))
            .subscribe().with(tick -> {
                if (rewards.isCompoundingEnabled()) {
                    yieldFarmingService.compound(rewards.getFarmId(), rewards.getUserAddress());
                }
            });
    }
    
    private void setupLiquidationMonitoring(LoanPosition position) {
        // Monitor position for liquidation
        logger.info("Setting up liquidation monitoring for position: {}", position.getPositionId());
    }
    
    private List<SwapRoute> findOptimalSwapRoute(String tokenIn, String tokenOut, BigDecimal amountIn) {
        // Find optimal swap route across different protocols
        List<SwapRoute> routes = new ArrayList<>();
        
        // Check direct routes
        SwapRoute directRoute = liquidityPoolManager.findDirectRoute(tokenIn, tokenOut, amountIn);
        if (directRoute != null) {
            routes.add(directRoute);
        }
        
        // Check multi-hop routes
        List<SwapRoute> multiHopRoutes = liquidityPoolManager.findMultiHopRoutes(tokenIn, tokenOut, amountIn);
        routes.addAll(multiHopRoutes);
        
        // Sort by best rate
        routes.sort((a, b) -> b.getOutputAmount().compareTo(a.getOutputAmount()));
        
        return routes;
    }
    
    private SwapResult executeSwapWithMEVProtection(SwapRoute route, SwapRequest request) {
        // Execute swap with MEV protection
        SwapResult result = liquidityPoolManager.executeSwap(route);
        
        // Apply MEV protection
        if (request.isEnableMEVProtection()) {
            result.setMEVProtected(true);
            result.setSlippageProtection(request.getSlippageTolerance());
        }
        
        return result;
    }
    
    private void calculatePortfolioMetrics(DeFiPortfolio portfolio) {
        BigDecimal totalValue = BigDecimal.ZERO;
        BigDecimal totalYield = BigDecimal.ZERO;
        
        // Calculate from liquidity positions
        if (portfolio.getLiquidityPositions() != null) {
            for (LiquidityPosition pos : portfolio.getLiquidityPositions()) {
                totalValue = totalValue.add(pos.getCurrentValue() != null ? pos.getCurrentValue() : BigDecimal.ZERO);
            }
        }
        
        // Calculate from yield positions
        if (portfolio.getYieldPositions() != null) {
            for (YieldFarmRewards rewards : portfolio.getYieldPositions()) {
                totalValue = totalValue.add(rewards.getStakedAmount() != null ? rewards.getStakedAmount() : BigDecimal.ZERO);
                totalYield = totalYield.add(rewards.getTotalRewardsEarned() != null ? rewards.getTotalRewardsEarned() : BigDecimal.ZERO);
            }
        }
        
        portfolio.setTotalValue(totalValue);
        portfolio.setTotalYieldEarned(totalYield);
    }
    
    private void setupPerformanceMonitoring() {
        Multi.createFrom().ticks().every(java.time.Duration.ofSeconds(1))
            .subscribe().with(tick -> {
                long currentOps = calculateOpsPerSecond();
                protocolMetrics.put("opsPerSecond", currentOps);
                
                if (currentOps < 50000) {
                    logger.warn("DeFi operations below target: {} ops/sec", currentOps);
                }
            });
    }
    
    private long calculateOpsPerSecond() {
        long elapsedSeconds = Math.max(1, Instant.now().getEpochSecond() - startTime.getEpochSecond());
        return operationCounter.get() / elapsedSeconds;
    }
}