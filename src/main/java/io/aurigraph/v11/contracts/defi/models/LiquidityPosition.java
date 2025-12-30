package io.aurigraph.v11.contracts.defi.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents a liquidity provider position in an AMM pool
 * Tracks LP tokens, underlying assets, fees earned, and impermanent loss
 * Enhanced for Sprint 4 with IL protection and advanced metrics
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class LiquidityPosition {
    
    @JsonProperty("positionId")
    private String positionId;
    
    @JsonProperty("poolId")
    private String poolId;
    
    @JsonProperty("lpTokenAddress")
    private String lpTokenAddress;
    
    @JsonProperty("lpTokenAmount")
    private BigDecimal lpTokenAmount;
    
    @JsonProperty("token0Address")
    private String token0Address;
    
    @JsonProperty("token1Address")
    private String token1Address;
    
    @JsonProperty("token0Amount")
    private BigDecimal token0Amount;
    
    @JsonProperty("token1Amount")
    private BigDecimal token1Amount;
    
    @JsonProperty("token0EntryPrice")
    private BigDecimal token0EntryPrice;
    
    @JsonProperty("token1EntryPrice")
    private BigDecimal token1EntryPrice;
    
    @JsonProperty("feesEarned0")
    private BigDecimal feesEarned0;
    
    @JsonProperty("feesEarned1")
    private BigDecimal feesEarned1;
    
    @JsonProperty("impermanentLoss")
    private BigDecimal impermanentLoss;
    
    @JsonProperty("totalValueLocked")
    private BigDecimal totalValueLocked;
    
    @JsonProperty("currentValue")
    private BigDecimal currentValue;
    
    @JsonProperty("roi")
    private BigDecimal roi;
    
    @JsonProperty("apr")
    private BigDecimal apr;
    
    @JsonProperty("createdAt")
    private Instant createdAt;
    
    @JsonProperty("lastUpdated")
    private Instant lastUpdated;
    
    @JsonProperty("isActive")
    private Boolean isActive;
    
    @JsonProperty("poolFee")
    private BigDecimal poolFee;
    
    @JsonProperty("priceRange")
    private PriceRange priceRange;
    
    // Sprint 4 Enhancements
    @JsonProperty("ilProtectionEnabled")
    private Boolean ilProtectionEnabled;
    
    @JsonProperty("ilProtectionFee")
    private BigDecimal ilProtectionFee;
    
    @JsonProperty("riskScore")
    private BigDecimal riskScore;
    
    @JsonProperty("metadata")
    private Map<String, Object> metadata;
    
    // Constructors
    public LiquidityPosition() {
        this.createdAt = Instant.now();
        this.lastUpdated = Instant.now();
        this.isActive = true;
        this.feesEarned0 = BigDecimal.ZERO;
        this.feesEarned1 = BigDecimal.ZERO;
        this.impermanentLoss = BigDecimal.ZERO;
        this.roi = BigDecimal.ZERO;
        this.ilProtectionEnabled = false;
        this.ilProtectionFee = BigDecimal.ZERO;
        this.riskScore = BigDecimal.ZERO;
        this.metadata = new HashMap<>();
    }
    
    public LiquidityPosition(String positionId, String poolId, String lpTokenAddress) {
        this();
        this.positionId = positionId;
        this.poolId = poolId;
        this.lpTokenAddress = lpTokenAddress;
    }
    
    // Getters and Setters
    public String getPositionId() { return positionId; }
    public void setPositionId(String positionId) { this.positionId = positionId; }
    
    public String getPoolId() { return poolId; }
    public void setPoolId(String poolId) { this.poolId = poolId; }
    
    public String getLpTokenAddress() { return lpTokenAddress; }
    public void setLpTokenAddress(String lpTokenAddress) { this.lpTokenAddress = lpTokenAddress; }
    
    public BigDecimal getLpTokenAmount() { return lpTokenAmount; }
    public void setLpTokenAmount(BigDecimal lpTokenAmount) { this.lpTokenAmount = lpTokenAmount; }
    
    public String getToken0Address() { return token0Address; }
    public void setToken0Address(String token0Address) { this.token0Address = token0Address; }
    
    public String getToken1Address() { return token1Address; }
    public void setToken1Address(String token1Address) { this.token1Address = token1Address; }
    
    public BigDecimal getToken0Amount() { return token0Amount; }
    public void setToken0Amount(BigDecimal token0Amount) { this.token0Amount = token0Amount; }
    
    public BigDecimal getToken1Amount() { return token1Amount; }
    public void setToken1Amount(BigDecimal token1Amount) { this.token1Amount = token1Amount; }
    
    public BigDecimal getToken0EntryPrice() { return token0EntryPrice; }
    public void setToken0EntryPrice(BigDecimal token0EntryPrice) { this.token0EntryPrice = token0EntryPrice; }
    
    public BigDecimal getToken1EntryPrice() { return token1EntryPrice; }
    public void setToken1EntryPrice(BigDecimal token1EntryPrice) { this.token1EntryPrice = token1EntryPrice; }
    
    public BigDecimal getFeesEarned0() { return feesEarned0; }
    public void setFeesEarned0(BigDecimal feesEarned0) { this.feesEarned0 = feesEarned0; }
    
    public BigDecimal getFeesEarned1() { return feesEarned1; }
    public void setFeesEarned1(BigDecimal feesEarned1) { this.feesEarned1 = feesEarned1; }
    
    public BigDecimal getImpermanentLoss() { return impermanentLoss; }
    public void setImpermanentLoss(BigDecimal impermanentLoss) { this.impermanentLoss = impermanentLoss; }
    
    public BigDecimal getTotalValueLocked() { return totalValueLocked; }
    public void setTotalValueLocked(BigDecimal totalValueLocked) { this.totalValueLocked = totalValueLocked; }
    
    public BigDecimal getCurrentValue() { return currentValue; }
    public void setCurrentValue(BigDecimal currentValue) { this.currentValue = currentValue; }
    
    public BigDecimal getRoi() { return roi; }
    public void setRoi(BigDecimal roi) { this.roi = roi; }
    
    public BigDecimal getApr() { return apr; }
    public void setApr(BigDecimal apr) { this.apr = apr; }
    
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    
    public Instant getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(Instant lastUpdated) { this.lastUpdated = lastUpdated; }
    
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
    
    public BigDecimal getPoolFee() { return poolFee; }
    public void setPoolFee(BigDecimal poolFee) { this.poolFee = poolFee; }
    
    public PriceRange getPriceRange() { return priceRange; }
    public void setPriceRange(PriceRange priceRange) { this.priceRange = priceRange; }
    
    public Boolean getILProtectionEnabled() { return ilProtectionEnabled; }
    public void setILProtectionEnabled(Boolean ilProtectionEnabled) { this.ilProtectionEnabled = ilProtectionEnabled; }
    
    public BigDecimal getILProtectionFee() { return ilProtectionFee; }
    public void setILProtectionFee(BigDecimal ilProtectionFee) { this.ilProtectionFee = ilProtectionFee; }
    
    public BigDecimal getRiskScore() { return riskScore; }
    public void setRiskScore(BigDecimal riskScore) { this.riskScore = riskScore; }
    
    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
    
    // Business logic methods
    public void updatePosition(BigDecimal token0Current, BigDecimal token1Current, 
                              BigDecimal fees0, BigDecimal fees1) {
        this.token0Amount = token0Current;
        this.token1Amount = token1Current;
        this.feesEarned0 = this.feesEarned0.add(fees0);
        this.feesEarned1 = this.feesEarned1.add(fees1);
        this.lastUpdated = Instant.now();
        calculateMetrics();
    }
    
    public void calculateMetrics() {
        // Calculate current value
        if (token0Amount != null && token1Amount != null && 
            token0EntryPrice != null && token1EntryPrice != null) {
            
            BigDecimal token0Value = token0Amount.multiply(token0EntryPrice);
            BigDecimal token1Value = token1Amount.multiply(token1EntryPrice);
            this.currentValue = token0Value.add(token1Value);
            
            // Calculate ROI including fees
            if (totalValueLocked != null && totalValueLocked.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal feesValue = feesEarned0.multiply(token0EntryPrice)
                    .add(feesEarned1.multiply(token1EntryPrice));
                BigDecimal totalReturn = currentValue.add(feesValue).subtract(totalValueLocked);
                this.roi = totalReturn.divide(totalValueLocked, 4, RoundingMode.HALF_UP);
            }
        }
    }
    
    public boolean isInRange(BigDecimal currentPrice) {
        if (priceRange == null) return true;
        return currentPrice.compareTo(priceRange.getMinPrice()) >= 0 && 
               currentPrice.compareTo(priceRange.getMaxPrice()) <= 0;
    }
    
    public static class PriceRange {
        @JsonProperty("minPrice")
        private BigDecimal minPrice;
        
        @JsonProperty("maxPrice")
        private BigDecimal maxPrice;
        
        public PriceRange() {}
        
        public PriceRange(BigDecimal minPrice, BigDecimal maxPrice) {
            this.minPrice = minPrice;
            this.maxPrice = maxPrice;
        }
        
        public BigDecimal getMinPrice() { return minPrice; }
        public void setMinPrice(BigDecimal minPrice) { this.minPrice = minPrice; }
        
        public BigDecimal getMaxPrice() { return maxPrice; }
        public void setMaxPrice(BigDecimal maxPrice) { this.maxPrice = maxPrice; }
    }
    
    @Override
    public String toString() {
        return String.format("LiquidityPosition{positionId='%s', poolId='%s', currentValue=%s, roi=%s}", 
                           positionId, poolId, currentValue, roi);
    }
}