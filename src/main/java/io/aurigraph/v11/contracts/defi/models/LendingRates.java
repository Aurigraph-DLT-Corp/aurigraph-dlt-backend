package io.aurigraph.v11.contracts.defi.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Sprint 4 Lending Rates Model
 * Current lending and borrowing rates across protocols
 */
public class LendingRates {
    
    @JsonProperty("protocol")
    private String protocol;
    
    @JsonProperty("asset")
    private String asset;
    
    @JsonProperty("supplyAPY")
    private BigDecimal supplyAPY;
    
    @JsonProperty("borrowAPY")
    private BigDecimal borrowAPY;
    
    @JsonProperty("stableBorrowAPY")
    private BigDecimal stableBorrowAPY;
    
    @JsonProperty("variableBorrowAPY")
    private BigDecimal variableBorrowAPY;
    
    @JsonProperty("utilizationRate")
    private BigDecimal utilizationRate;
    
    @JsonProperty("totalSupply")
    private BigDecimal totalSupply;
    
    @JsonProperty("totalBorrow")
    private BigDecimal totalBorrow;
    
    @JsonProperty("availableLiquidity")
    private BigDecimal availableLiquidity;
    
    @JsonProperty("liquidityIndex")
    private BigDecimal liquidityIndex;
    
    @JsonProperty("variableBorrowIndex")
    private BigDecimal variableBorrowIndex;
    
    @JsonProperty("lastUpdateTimestamp")
    private Instant lastUpdateTimestamp;
    
    @JsonProperty("reserveFactor")
    private BigDecimal reserveFactor;
    
    @JsonProperty("ltv")
    private BigDecimal ltv; // Loan-to-Value ratio
    
    @JsonProperty("liquidationThreshold")
    private BigDecimal liquidationThreshold;
    
    @JsonProperty("liquidationBonus")
    private BigDecimal liquidationBonus;
    
    @JsonProperty("isActive")
    private boolean isActive;
    
    @JsonProperty("isFrozen")
    private boolean isFrozen;
    
    @JsonProperty("isPaused")
    private boolean isPaused;
    
    @JsonProperty("rateHistory")
    private Map<String, BigDecimal> rateHistory;
    
    // Constructors
    public LendingRates() {
        this.lastUpdateTimestamp = Instant.now();
        this.isActive = true;
        this.isFrozen = false;
        this.isPaused = false;
        this.rateHistory = new HashMap<>();
        this.liquidityIndex = BigDecimal.ONE;
        this.variableBorrowIndex = BigDecimal.ONE;
        this.reserveFactor = BigDecimal.valueOf(0.1); // 10% default
        this.utilizationRate = BigDecimal.ZERO;
    }
    
    public LendingRates(String protocol, String asset) {
        this();
        this.protocol = protocol;
        this.asset = asset;
    }
    
    // Getters and Setters
    public String getProtocol() { return protocol; }
    public void setProtocol(String protocol) { this.protocol = protocol; }
    
    public String getAsset() { return asset; }
    public void setAsset(String asset) { this.asset = asset; }
    
    public BigDecimal getSupplyAPY() { return supplyAPY; }
    public void setSupplyAPY(BigDecimal supplyAPY) { 
        this.supplyAPY = supplyAPY;
        updateTimestamp();
    }
    
    public BigDecimal getBorrowAPY() { return borrowAPY; }
    public void setBorrowAPY(BigDecimal borrowAPY) { 
        this.borrowAPY = borrowAPY;
        updateTimestamp();
    }
    
    public BigDecimal getStableBorrowAPY() { return stableBorrowAPY; }
    public void setStableBorrowAPY(BigDecimal stableBorrowAPY) { this.stableBorrowAPY = stableBorrowAPY; }
    
    public BigDecimal getVariableBorrowAPY() { return variableBorrowAPY; }
    public void setVariableBorrowAPY(BigDecimal variableBorrowAPY) { this.variableBorrowAPY = variableBorrowAPY; }
    
    public BigDecimal getUtilizationRate() { return utilizationRate; }
    public void setUtilizationRate(BigDecimal utilizationRate) { this.utilizationRate = utilizationRate; }
    
    public BigDecimal getTotalSupply() { return totalSupply; }
    public void setTotalSupply(BigDecimal totalSupply) { 
        this.totalSupply = totalSupply;
        calculateUtilizationRate();
    }
    
    public BigDecimal getTotalBorrow() { return totalBorrow; }
    public void setTotalBorrow(BigDecimal totalBorrow) { 
        this.totalBorrow = totalBorrow;
        calculateUtilizationRate();
    }
    
    public BigDecimal getAvailableLiquidity() { return availableLiquidity; }
    public void setAvailableLiquidity(BigDecimal availableLiquidity) { this.availableLiquidity = availableLiquidity; }
    
    public BigDecimal getLiquidityIndex() { return liquidityIndex; }
    public void setLiquidityIndex(BigDecimal liquidityIndex) { this.liquidityIndex = liquidityIndex; }
    
    public BigDecimal getVariableBorrowIndex() { return variableBorrowIndex; }
    public void setVariableBorrowIndex(BigDecimal variableBorrowIndex) { this.variableBorrowIndex = variableBorrowIndex; }
    
    public Instant getLastUpdateTimestamp() { return lastUpdateTimestamp; }
    public void setLastUpdateTimestamp(Instant lastUpdateTimestamp) { this.lastUpdateTimestamp = lastUpdateTimestamp; }
    
    public BigDecimal getReserveFactor() { return reserveFactor; }
    public void setReserveFactor(BigDecimal reserveFactor) { this.reserveFactor = reserveFactor; }
    
    public BigDecimal getLtv() { return ltv; }
    public void setLtv(BigDecimal ltv) { this.ltv = ltv; }
    
    public BigDecimal getLiquidationThreshold() { return liquidationThreshold; }
    public void setLiquidationThreshold(BigDecimal liquidationThreshold) { this.liquidationThreshold = liquidationThreshold; }
    
    public BigDecimal getLiquidationBonus() { return liquidationBonus; }
    public void setLiquidationBonus(BigDecimal liquidationBonus) { this.liquidationBonus = liquidationBonus; }
    
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
    
    public boolean isFrozen() { return isFrozen; }
    public void setFrozen(boolean frozen) { isFrozen = frozen; }
    
    public boolean isPaused() { return isPaused; }
    public void setPaused(boolean paused) { isPaused = paused; }
    
    public Map<String, BigDecimal> getRateHistory() { return rateHistory; }
    public void setRateHistory(Map<String, BigDecimal> rateHistory) { this.rateHistory = rateHistory; }
    
    // Helper methods
    private void updateTimestamp() {
        this.lastUpdateTimestamp = Instant.now();
    }
    
    private void calculateUtilizationRate() {
        if (totalSupply != null && totalBorrow != null && !totalSupply.equals(BigDecimal.ZERO)) {
            this.utilizationRate = totalBorrow.divide(totalSupply, 4, java.math.RoundingMode.HALF_UP);
        }
    }
    
    public void addRateToHistory(String timeKey, BigDecimal rate) {
        if (rateHistory == null) {
            rateHistory = new HashMap<>();
        }
        rateHistory.put(timeKey, rate);
    }
    
    public BigDecimal getSpread() {
        if (borrowAPY != null && supplyAPY != null) {
            return borrowAPY.subtract(supplyAPY);
        }
        return BigDecimal.ZERO;
    }
    
    public boolean isHealthy() {
        return isActive && !isFrozen && !isPaused && 
               utilizationRate != null && utilizationRate.compareTo(BigDecimal.valueOf(0.95)) < 0;
    }
    
    public BigDecimal calculateAPR() {
        // Convert APY to APR (simple approximation)
        if (supplyAPY != null) {
            return supplyAPY.multiply(BigDecimal.valueOf(0.95)); // Approximate conversion
        }
        return BigDecimal.ZERO;
    }
}