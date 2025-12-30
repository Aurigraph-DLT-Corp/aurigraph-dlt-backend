package io.aurigraph.v11.contracts.defi;

import io.aurigraph.v11.contracts.defi.models.*;
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
 * Sprint 4 Enhanced Lending Protocol Service
 * Manages lending/borrowing with collateralization, liquidation, and flash loans
 */
@ApplicationScoped
public class LendingProtocolService {
    
    private static final Logger logger = LoggerFactory.getLogger(LendingProtocolService.class);
    
    // Protocol storage
    private final Map<String, LendingProtocol> protocols = new ConcurrentHashMap<>();
    private final Map<String, List<LoanPosition>> userPositions = new ConcurrentHashMap<>();
    private final Map<String, LoanPosition> allPositions = new ConcurrentHashMap<>();
    
    // Performance tracking
    private final AtomicLong positionIdGenerator = new AtomicLong(0);
    private volatile BigDecimal totalSupplied = BigDecimal.ZERO;
    private volatile BigDecimal totalBorrowed = BigDecimal.ZERO;
    
    // Collateral factors for different tokens
    private final Map<String, BigDecimal> collateralFactors = new ConcurrentHashMap<>();
    private final Map<String, BigDecimal> liquidationThresholds = new ConcurrentHashMap<>();
    
    /**
     * Initialize lending protocols and configure collateral
     */
    public void initializeProtocols() {
        logger.info("Initializing lending protocols for Sprint 4");
        
        // Initialize collateral factors (LTV ratios)
        collateralFactors.put("ETH", BigDecimal.valueOf(0.85));    // 85% LTV
        collateralFactors.put("BTC", BigDecimal.valueOf(0.80));    // 80% LTV
        collateralFactors.put("USDC", BigDecimal.valueOf(0.90));   // 90% LTV
        collateralFactors.put("DAI", BigDecimal.valueOf(0.88));    // 88% LTV
        collateralFactors.put("AURI", BigDecimal.valueOf(0.75));   // 75% LTV
        
        // Initialize liquidation thresholds
        liquidationThresholds.put("ETH", BigDecimal.valueOf(0.82));   // 82%
        liquidationThresholds.put("BTC", BigDecimal.valueOf(0.77));   // 77%
        liquidationThresholds.put("USDC", BigDecimal.valueOf(0.87));  // 87%
        liquidationThresholds.put("DAI", BigDecimal.valueOf(0.85));   // 85%
        liquidationThresholds.put("AURI", BigDecimal.valueOf(0.72));  // 72%
        
        // Create lending protocols
        createProtocol("AAVE_V3", "Aave Protocol V3", 
                      BigDecimal.valueOf(0.02), BigDecimal.valueOf(0.05)); // 2% supply, 5% borrow
        createProtocol("COMPOUND_V3", "Compound V3", 
                      BigDecimal.valueOf(0.015), BigDecimal.valueOf(0.045)); // 1.5% supply, 4.5% borrow
        createProtocol("AURIGRAPH_LEND", "Aurigraph Native Lending", 
                      BigDecimal.valueOf(0.03), BigDecimal.valueOf(0.06)); // 3% supply, 6% borrow
        
        logger.info("Initialized {} lending protocols", protocols.size());
    }
    
    /**
     * Validate collateral for a loan
     */
    public boolean validateCollateral(String collateralToken, BigDecimal collateralAmount,
                                     String borrowToken, BigDecimal borrowAmount) {
        BigDecimal collateralFactor = collateralFactors.getOrDefault(collateralToken, BigDecimal.ZERO);
        if (collateralFactor.equals(BigDecimal.ZERO)) {
            return false; // Unsupported collateral
        }
        
        // Get token prices (mock implementation)
        BigDecimal collateralPrice = getTokenPrice(collateralToken);
        BigDecimal borrowPrice = getTokenPrice(borrowToken);
        
        BigDecimal collateralValue = collateralAmount.multiply(collateralPrice);
        BigDecimal borrowValue = borrowAmount.multiply(borrowPrice);
        
        BigDecimal maxBorrowable = collateralValue.multiply(collateralFactor);
        
        boolean valid = borrowValue.compareTo(maxBorrowable) <= 0;
        
        logger.debug("Collateral validation: {} {} (${}) can borrow {} {} (${}) = {}",
                   collateralAmount, collateralToken, collateralValue,
                   borrowAmount, borrowToken, borrowValue, valid);
        
        return valid;
    }
    
    /**
     * Open a lending/borrowing position
     */
    public LoanPosition openPosition(String userAddress, String collateralToken,
                                    BigDecimal collateralAmount, String borrowToken,
                                    BigDecimal borrowAmount) {
        
        // Validate collateral first
        if (!validateCollateral(collateralToken, collateralAmount, borrowToken, borrowAmount)) {
            throw new IllegalArgumentException("Insufficient collateral");
        }
        
        String positionId = generatePositionId();
        LoanPosition position = new LoanPosition(positionId, userAddress, "AURIGRAPH_LEND", 
                                               LoanPosition.PositionType.COMBINED);
        
        // Set up collateral
        List<LoanPosition.CollateralAsset> collateralAssets = new ArrayList<>();
        BigDecimal collateralPrice = getTokenPrice(collateralToken);
        LoanPosition.CollateralAsset collateral = new LoanPosition.CollateralAsset(
            collateralToken, collateralAmount, collateralPrice);
        collateral.setCollateralFactor(collateralFactors.get(collateralToken));
        collateral.setLiquidationThreshold(liquidationThresholds.get(collateralToken));
        collateralAssets.add(collateral);
        position.setCollateralTokens(collateralAssets);
        
        // Set up borrowed assets
        List<LoanPosition.BorrowedAsset> borrowedAssets = new ArrayList<>();
        BigDecimal borrowPrice = getTokenPrice(borrowToken);
        LoanPosition.BorrowedAsset borrowed = new LoanPosition.BorrowedAsset(
            borrowToken, borrowAmount, borrowPrice, BigDecimal.valueOf(0.06)); // 6% interest
        borrowedAssets.add(borrowed);
        position.setBorrowedTokens(borrowedAssets);
        
        // Calculate position metrics
        position.setTotalCollateralValue(collateralAmount.multiply(collateralPrice));
        position.setTotalBorrowedValue(borrowAmount.multiply(borrowPrice));
        position.setLiquidationThreshold(liquidationThresholds.get(collateralToken));
        position.setMaxLtv(collateralFactors.get(collateralToken));
        
        // Calculate health factor and LTV
        position.calculateHealthFactor();
        position.calculateLtv();
        
        // Set liquidation price
        BigDecimal liquidationPrice = calculateLiquidationPrice(position);
        position.setLiquidationPrice(liquidationPrice);
        
        // Store position
        allPositions.put(positionId, position);
        userPositions.computeIfAbsent(userAddress, k -> new ArrayList<>()).add(position);
        
        // Update totals
        updateTotals();
        
        logger.info("Opened lending position {} for user {}: {} {} collateral, {} {} borrowed, HF: {}",
                   positionId, userAddress, collateralAmount, collateralToken,
                   borrowAmount, borrowToken, position.getHealthFactor());
        
        return position;
    }
    
    /**
     * Execute flash loan
     */
    public boolean executeFlashLoan(String userAddress, String token, BigDecimal amount,
                                   String callbackContract, byte[] callbackData) {
        logger.info("Executing flash loan: {} {} for user {}", amount, token, userAddress);
        
        // Mock flash loan execution
        // In production, this would:
        // 1. Transfer tokens to user
        // 2. Call callback contract
        // 3. Ensure repayment with fee
        
        BigDecimal flashLoanFee = amount.multiply(BigDecimal.valueOf(0.0005)); // 0.05% fee
        
        logger.info("Flash loan executed successfully with fee: {} {}", flashLoanFee, token);
        return true;
    }
    
    /**
     * Scan for positions eligible for liquidation
     */
    public List<LoanPosition> scanForLiquidations() {
        List<LoanPosition> eligiblePositions = new ArrayList<>();
        
        for (LoanPosition position : allPositions.values()) {
            if (position.getIsActive() && position.isLiquidationEligible()) {
                // Update position with current prices before confirming eligibility
                updatePositionPrices(position);
                position.calculateHealthFactor();
                
                if (position.isLiquidationEligible()) {
                    eligiblePositions.add(position);
                }
            }
        }
        
        logger.debug("Found {} positions eligible for liquidation", eligiblePositions.size());
        return eligiblePositions;
    }
    
    /**
     * Get user's loan positions
     */
    public List<LoanPosition> getUserPositions(String userAddress) {
        return userPositions.getOrDefault(userAddress, new ArrayList<>());
    }
    
    /**
     * Get total value supplied across protocols
     */
    public BigDecimal getTotalSupplied() {
        return totalSupplied;
    }
    
    /**
     * Get total value borrowed across protocols
     */
    public BigDecimal getTotalBorrowed() {
        return totalBorrowed;
    }
    
    // Private helper methods
    private void createProtocol(String protocolId, String name, BigDecimal supplyApr, BigDecimal borrowApr) {
        LendingProtocol protocol = new LendingProtocol(protocolId, name, supplyApr, borrowApr);
        protocols.put(protocolId, protocol);
        logger.debug("Created lending protocol: {} with supply APR: {}%, borrow APR: {}%",
                   name, supplyApr.multiply(BigDecimal.valueOf(100)), borrowApr.multiply(BigDecimal.valueOf(100)));
    }
    
    private String generatePositionId() {
        return "LOAN_" + positionIdGenerator.incrementAndGet() + "_" + System.currentTimeMillis();
    }
    
    private BigDecimal getTokenPrice(String token) {
        // Mock price feed - in production would use real oracles
        Map<String, BigDecimal> prices = Map.of(
            "ETH", BigDecimal.valueOf(2000),
            "BTC", BigDecimal.valueOf(30000),
            "USDC", BigDecimal.ONE,
            "DAI", BigDecimal.ONE,
            "AURI", BigDecimal.valueOf(5)
        );
        return prices.getOrDefault(token, BigDecimal.ONE);
    }
    
    private BigDecimal calculateLiquidationPrice(LoanPosition position) {
        if (position.getCollateralTokens().isEmpty() || position.getBorrowedTokens().isEmpty()) {
            return BigDecimal.ZERO;
        }
        
        LoanPosition.CollateralAsset collateral = position.getCollateralTokens().get(0);
        LoanPosition.BorrowedAsset borrowed = position.getBorrowedTokens().get(0);
        
        // Liquidation price = (borrowed value) / (collateral amount * liquidation threshold)
        BigDecimal liquidationPrice = borrowed.getValueUsd()
            .divide(collateral.getAmount().multiply(collateral.getLiquidationThreshold()), 
                   8, RoundingMode.HALF_UP);
        
        return liquidationPrice;
    }
    
    private void updatePositionPrices(LoanPosition position) {
        // Update collateral prices
        for (LoanPosition.CollateralAsset collateral : position.getCollateralTokens()) {
            BigDecimal newPrice = getTokenPrice(collateral.getTokenAddress());
            collateral.setPriceUsd(newPrice);
            collateral.setValueUsd(collateral.getAmount().multiply(newPrice));
        }
        
        // Update borrowed asset prices
        for (LoanPosition.BorrowedAsset borrowed : position.getBorrowedTokens()) {
            BigDecimal newPrice = getTokenPrice(borrowed.getTokenAddress());
            borrowed.setPriceUsd(newPrice);
            borrowed.setValueUsd(borrowed.getAmount().multiply(newPrice));
        }
        
        // Recalculate totals
        BigDecimal totalCollateral = position.getCollateralTokens().stream()
            .map(LoanPosition.CollateralAsset::getValueUsd)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal totalBorrowed = position.getBorrowedTokens().stream()
            .map(LoanPosition.BorrowedAsset::getValueUsd)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        position.setTotalCollateralValue(totalCollateral);
        position.setTotalBorrowedValue(totalBorrowed);
        position.setLastUpdated(Instant.now());
    }
    
    private void updateTotals() {
        BigDecimal newTotalSupplied = BigDecimal.ZERO;
        BigDecimal newTotalBorrowed = BigDecimal.ZERO;
        
        for (LoanPosition position : allPositions.values()) {
            if (position.getIsActive()) {
                newTotalSupplied = newTotalSupplied.add(position.getTotalCollateralValue());
                newTotalBorrowed = newTotalBorrowed.add(position.getTotalBorrowedValue());
            }
        }
        
        this.totalSupplied = newTotalSupplied;
        this.totalBorrowed = newTotalBorrowed;
    }
    
    // Inner class for LendingProtocol
    public static class LendingProtocol {
        private String protocolId;
        private String name;
        private BigDecimal supplyApr;
        private BigDecimal borrowApr;
        private BigDecimal totalSupplied;
        private BigDecimal totalBorrowed;
        private Map<String, BigDecimal> tokenSupply;
        private Map<String, BigDecimal> tokenBorrow;
        
        public LendingProtocol(String protocolId, String name, BigDecimal supplyApr, BigDecimal borrowApr) {
            this.protocolId = protocolId;
            this.name = name;
            this.supplyApr = supplyApr;
            this.borrowApr = borrowApr;
            this.totalSupplied = BigDecimal.ZERO;
            this.totalBorrowed = BigDecimal.ZERO;
            this.tokenSupply = new ConcurrentHashMap<>();
            this.tokenBorrow = new ConcurrentHashMap<>();
        }
        
        // Getters
        public String getProtocolId() { return protocolId; }
        public String getName() { return name; }
        public BigDecimal getSupplyApr() { return supplyApr; }
        public BigDecimal getBorrowApr() { return borrowApr; }
        public BigDecimal getTotalSupplied() { return totalSupplied; }
        public BigDecimal getTotalBorrowed() { return totalBorrowed; }
        public Map<String, BigDecimal> getTokenSupply() { return tokenSupply; }
        public Map<String, BigDecimal> getTokenBorrow() { return tokenBorrow; }
    }
}