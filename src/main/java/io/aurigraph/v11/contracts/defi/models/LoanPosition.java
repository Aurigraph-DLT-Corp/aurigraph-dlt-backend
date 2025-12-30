package io.aurigraph.v11.contracts.defi.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Sprint 4 Enhanced Loan Position
 * Represents a lending/borrowing position in DeFi protocols
 * Tracks collateral, debt, health factor, liquidation risk with advanced risk management
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class LoanPosition {
    
    @JsonProperty("positionId")
    private String positionId;
    
    @JsonProperty("userAddress")
    private String userAddress;
    
    @JsonProperty("protocolId")
    private String protocolId;
    
    @JsonProperty("positionType")
    private PositionType positionType;
    
    @JsonProperty("collateralTokens")
    private List<CollateralAsset> collateralTokens;
    
    @JsonProperty("borrowedTokens")
    private List<BorrowedAsset> borrowedTokens;
    
    @JsonProperty("totalCollateralValue")
    private BigDecimal totalCollateralValue;
    
    @JsonProperty("totalBorrowedValue")
    private BigDecimal totalBorrowedValue;
    
    @JsonProperty("healthFactor")
    private BigDecimal healthFactor;
    
    @JsonProperty("liquidationThreshold")
    private BigDecimal liquidationThreshold;
    
    @JsonProperty("ltv")
    private BigDecimal ltv; // Loan-to-Value ratio
    
    @JsonProperty("maxLtv")
    private BigDecimal maxLtv;
    
    @JsonProperty("interestAccrued")
    private BigDecimal interestAccrued;
    
    @JsonProperty("liquidationPrice")
    private BigDecimal liquidationPrice;
    
    @JsonProperty("createdAt")
    private Instant createdAt;
    
    @JsonProperty("lastUpdated")
    private Instant lastUpdated;
    
    @JsonProperty("maturityDate")
    private Instant maturityDate;
    
    @JsonProperty("isActive")
    private Boolean isActive;
    
    @JsonProperty("riskLevel")
    private RiskLevel riskLevel;
    
    @JsonProperty("autoLiquidationEnabled")
    private Boolean autoLiquidationEnabled;
    
    // Sprint 4 Enhancements
    @JsonProperty("riskScore")
    private BigDecimal riskScore;
    
    @JsonProperty("liquidationProtection")
    private Boolean liquidationProtection;
    
    @JsonProperty("borrowingPower")
    private BigDecimal borrowingPower;
    
    @JsonProperty("metadata")
    private Map<String, Object> metadata;
    
    // Constructors
    public LoanPosition() {
        this.createdAt = Instant.now();
        this.lastUpdated = Instant.now();
        this.isActive = true;
        this.totalCollateralValue = BigDecimal.ZERO;
        this.totalBorrowedValue = BigDecimal.ZERO;
        this.interestAccrued = BigDecimal.ZERO;
        this.autoLiquidationEnabled = false;
        this.liquidationProtection = false;
        this.riskScore = BigDecimal.ZERO;
        this.borrowingPower = BigDecimal.ZERO;
        this.collateralTokens = new ArrayList<>();
        this.borrowedTokens = new ArrayList<>();
        this.metadata = new HashMap<>();
    }
    
    public LoanPosition(String positionId, String userAddress, String protocolId, PositionType positionType) {
        this();
        this.positionId = positionId;
        this.userAddress = userAddress;
        this.protocolId = protocolId;
        this.positionType = positionType;
    }
    
    // Enums
    public enum PositionType {
        LENDING, BORROWING, COMBINED
    }
    
    public enum RiskLevel {
        LOW, MEDIUM, HIGH, CRITICAL
    }
    
    // Getters and Setters
    public String getPositionId() { return positionId; }
    public void setPositionId(String positionId) { this.positionId = positionId; }
    
    public String getUserAddress() { return userAddress; }
    public void setUserAddress(String userAddress) { this.userAddress = userAddress; }
    
    public String getProtocolId() { return protocolId; }
    public void setProtocolId(String protocolId) { this.protocolId = protocolId; }
    
    public PositionType getPositionType() { return positionType; }
    public void setPositionType(PositionType positionType) { this.positionType = positionType; }
    
    public List<CollateralAsset> getCollateralTokens() { return collateralTokens; }
    public void setCollateralTokens(List<CollateralAsset> collateralTokens) { this.collateralTokens = collateralTokens; }
    
    public List<BorrowedAsset> getBorrowedTokens() { return borrowedTokens; }
    public void setBorrowedTokens(List<BorrowedAsset> borrowedTokens) { this.borrowedTokens = borrowedTokens; }
    
    public BigDecimal getTotalCollateralValue() { return totalCollateralValue; }
    public void setTotalCollateralValue(BigDecimal totalCollateralValue) { this.totalCollateralValue = totalCollateralValue; }
    
    public BigDecimal getTotalBorrowedValue() { return totalBorrowedValue; }
    public void setTotalBorrowedValue(BigDecimal totalBorrowedValue) { this.totalBorrowedValue = totalBorrowedValue; }
    
    public BigDecimal getHealthFactor() { return healthFactor; }
    public void setHealthFactor(BigDecimal healthFactor) { this.healthFactor = healthFactor; }
    
    public BigDecimal getLiquidationThreshold() { return liquidationThreshold; }
    public void setLiquidationThreshold(BigDecimal liquidationThreshold) { this.liquidationThreshold = liquidationThreshold; }
    
    public BigDecimal getLtv() { return ltv; }
    public void setLtv(BigDecimal ltv) { this.ltv = ltv; }
    
    public BigDecimal getMaxLtv() { return maxLtv; }
    public void setMaxLtv(BigDecimal maxLtv) { this.maxLtv = maxLtv; }
    
    public BigDecimal getInterestAccrued() { return interestAccrued; }
    public void setInterestAccrued(BigDecimal interestAccrued) { this.interestAccrued = interestAccrued; }
    
    public BigDecimal getLiquidationPrice() { return liquidationPrice; }
    public void setLiquidationPrice(BigDecimal liquidationPrice) { this.liquidationPrice = liquidationPrice; }
    
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    
    public Instant getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(Instant lastUpdated) { this.lastUpdated = lastUpdated; }
    
    public Instant getMaturityDate() { return maturityDate; }
    public void setMaturityDate(Instant maturityDate) { this.maturityDate = maturityDate; }
    
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
    
    public RiskLevel getRiskLevel() { return riskLevel; }
    public void setRiskLevel(RiskLevel riskLevel) { this.riskLevel = riskLevel; }
    
    public Boolean getAutoLiquidationEnabled() { return autoLiquidationEnabled; }
    public void setAutoLiquidationEnabled(Boolean autoLiquidationEnabled) { this.autoLiquidationEnabled = autoLiquidationEnabled; }
    
    public BigDecimal getRiskScore() { return riskScore; }
    public void setRiskScore(BigDecimal riskScore) { this.riskScore = riskScore; }
    
    public Boolean getLiquidationProtection() { return liquidationProtection; }
    public void setLiquidationProtection(Boolean liquidationProtection) { this.liquidationProtection = liquidationProtection; }
    
    public BigDecimal getBorrowingPower() { return borrowingPower; }
    public void setBorrowingPower(BigDecimal borrowingPower) { this.borrowingPower = borrowingPower; }
    
    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
    
    // Business logic methods
    public void calculateHealthFactor() {
        if (totalBorrowedValue == null || totalBorrowedValue.compareTo(BigDecimal.ZERO) == 0) {
            this.healthFactor = BigDecimal.valueOf(Double.MAX_VALUE);
            return;
        }
        
        BigDecimal adjustedCollateral = totalCollateralValue.multiply(liquidationThreshold);
        this.healthFactor = adjustedCollateral.divide(totalBorrowedValue, 4, RoundingMode.HALF_UP);
        
        updateRiskLevel();
    }
    
    public void calculateLtv() {
        if (totalCollateralValue == null || totalCollateralValue.compareTo(BigDecimal.ZERO) == 0) {
            this.ltv = BigDecimal.ZERO;
            return;
        }
        
        this.ltv = totalBorrowedValue.divide(totalCollateralValue, 4, RoundingMode.HALF_UP);
    }
    
    public void updateRiskLevel() {
        if (healthFactor == null) {
            this.riskLevel = RiskLevel.CRITICAL;
            return;
        }
        
        if (healthFactor.compareTo(BigDecimal.valueOf(2.0)) >= 0) {
            this.riskLevel = RiskLevel.LOW;
        } else if (healthFactor.compareTo(BigDecimal.valueOf(1.5)) >= 0) {
            this.riskLevel = RiskLevel.MEDIUM;
        } else if (healthFactor.compareTo(BigDecimal.valueOf(1.1)) >= 0) {
            this.riskLevel = RiskLevel.HIGH;
        } else {
            this.riskLevel = RiskLevel.CRITICAL;
        }
    }
    
    public boolean isLiquidationEligible() {
        return healthFactor != null && healthFactor.compareTo(BigDecimal.ONE) < 0;
    }
    
    public BigDecimal getAvailableBorrowingPower() {
        if (maxLtv == null || totalCollateralValue == null) {
            return BigDecimal.ZERO;
        }
        
        BigDecimal maxBorrowable = totalCollateralValue.multiply(maxLtv);
        BigDecimal available = maxBorrowable.subtract(totalBorrowedValue);
        return available.max(BigDecimal.ZERO);
    }
    
    public boolean hasMatured() {
        return maturityDate != null && Instant.now().isAfter(maturityDate);
    }
    
    // Inner classes
    public static class CollateralAsset {
        @JsonProperty("tokenAddress")
        private String tokenAddress;
        
        @JsonProperty("amount")
        private BigDecimal amount;
        
        @JsonProperty("priceUsd")
        private BigDecimal priceUsd;
        
        @JsonProperty("valueUsd")
        private BigDecimal valueUsd;
        
        @JsonProperty("collateralFactor")
        private BigDecimal collateralFactor;
        
        @JsonProperty("liquidationThreshold")
        private BigDecimal liquidationThreshold;
        
        public CollateralAsset() {}
        
        public CollateralAsset(String tokenAddress, BigDecimal amount, BigDecimal priceUsd) {
            this.tokenAddress = tokenAddress;
            this.amount = amount;
            this.priceUsd = priceUsd;
            this.valueUsd = amount.multiply(priceUsd);
        }
        
        // Getters and setters
        public String getTokenAddress() { return tokenAddress; }
        public void setTokenAddress(String tokenAddress) { this.tokenAddress = tokenAddress; }
        
        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }
        
        public BigDecimal getPriceUsd() { return priceUsd; }
        public void setPriceUsd(BigDecimal priceUsd) { this.priceUsd = priceUsd; }
        
        public BigDecimal getValueUsd() { return valueUsd; }
        public void setValueUsd(BigDecimal valueUsd) { this.valueUsd = valueUsd; }
        
        public BigDecimal getCollateralFactor() { return collateralFactor; }
        public void setCollateralFactor(BigDecimal collateralFactor) { this.collateralFactor = collateralFactor; }
        
        public BigDecimal getLiquidationThreshold() { return liquidationThreshold; }
        public void setLiquidationThreshold(BigDecimal liquidationThreshold) { this.liquidationThreshold = liquidationThreshold; }
    }
    
    public static class BorrowedAsset {
        @JsonProperty("tokenAddress")
        private String tokenAddress;
        
        @JsonProperty("amount")
        private BigDecimal amount;
        
        @JsonProperty("priceUsd")
        private BigDecimal priceUsd;
        
        @JsonProperty("valueUsd")
        private BigDecimal valueUsd;
        
        @JsonProperty("interestRate")
        private BigDecimal interestRate;
        
        @JsonProperty("interestAccrued")
        private BigDecimal interestAccrued;
        
        @JsonProperty("borrowedAt")
        private Instant borrowedAt;
        
        public BorrowedAsset() {
            this.borrowedAt = Instant.now();
            this.interestAccrued = BigDecimal.ZERO;
        }
        
        public BorrowedAsset(String tokenAddress, BigDecimal amount, BigDecimal priceUsd, BigDecimal interestRate) {
            this();
            this.tokenAddress = tokenAddress;
            this.amount = amount;
            this.priceUsd = priceUsd;
            this.valueUsd = amount.multiply(priceUsd);
            this.interestRate = interestRate;
        }
        
        // Getters and setters
        public String getTokenAddress() { return tokenAddress; }
        public void setTokenAddress(String tokenAddress) { this.tokenAddress = tokenAddress; }
        
        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }
        
        public BigDecimal getPriceUsd() { return priceUsd; }
        public void setPriceUsd(BigDecimal priceUsd) { this.priceUsd = priceUsd; }
        
        public BigDecimal getValueUsd() { return valueUsd; }
        public void setValueUsd(BigDecimal valueUsd) { this.valueUsd = valueUsd; }
        
        public BigDecimal getInterestRate() { return interestRate; }
        public void setInterestRate(BigDecimal interestRate) { this.interestRate = interestRate; }
        
        public BigDecimal getInterestAccrued() { return interestAccrued; }
        public void setInterestAccrued(BigDecimal interestAccrued) { this.interestAccrued = interestAccrued; }
        
        public Instant getBorrowedAt() { return borrowedAt; }
        public void setBorrowedAt(Instant borrowedAt) { this.borrowedAt = borrowedAt; }
    }
    
    @Override
    public String toString() {
        return String.format("LoanPosition{positionId='%s', healthFactor=%s, ltv=%s, riskLevel=%s}", 
                           positionId, healthFactor, ltv, riskLevel);
    }
}