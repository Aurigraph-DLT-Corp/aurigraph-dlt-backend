package io.aurigraph.v11.contracts.defi.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

/**
 * Sprint 4 Borrow Request Model
 * Request for borrowing assets against collateral
 */
public class BorrowRequest {
    
    @JsonProperty("userAddress")
    private String userAddress;
    
    @JsonProperty("protocol")
    private String protocol;
    
    @JsonProperty("collateralToken")
    private String collateralToken;
    
    @JsonProperty("collateralAmount")
    private BigDecimal collateralAmount;
    
    @JsonProperty("borrowToken")
    private String borrowToken;
    
    @JsonProperty("borrowAmount")
    private BigDecimal borrowAmount;
    
    @JsonProperty("maxInterestRate")
    private BigDecimal maxInterestRate;
    
    @JsonProperty("rateType")
    private InterestRateType rateType;
    
    @JsonProperty("liquidationThreshold")
    private BigDecimal liquidationThreshold;
    
    @JsonProperty("enableRiskManagement")
    private boolean enableRiskManagement;
    
    @JsonProperty("autoRepayThreshold")
    private BigDecimal autoRepayThreshold;
    
    @JsonProperty("deadline")
    private Long deadline;
    
    public enum InterestRateType {
        FIXED,
        VARIABLE,
        STABLE
    }
    
    // Constructors
    public BorrowRequest() {
        this.rateType = InterestRateType.VARIABLE;
        this.enableRiskManagement = true;
        this.liquidationThreshold = BigDecimal.valueOf(0.85); // 85% default threshold
        this.deadline = System.currentTimeMillis() + 3600000; // 1 hour default
    }
    
    public BorrowRequest(String userAddress, String collateralToken, BigDecimal collateralAmount,
                       String borrowToken, BigDecimal borrowAmount) {
        this();
        this.userAddress = userAddress;
        this.collateralToken = collateralToken;
        this.collateralAmount = collateralAmount;
        this.borrowToken = borrowToken;
        this.borrowAmount = borrowAmount;
    }
    
    // Getters and Setters
    public String getUserAddress() { return userAddress; }
    public void setUserAddress(String userAddress) { this.userAddress = userAddress; }
    
    public String getProtocol() { return protocol; }
    public void setProtocol(String protocol) { this.protocol = protocol; }
    
    public String getCollateralToken() { return collateralToken; }
    public void setCollateralToken(String collateralToken) { this.collateralToken = collateralToken; }
    
    public BigDecimal getCollateralAmount() { return collateralAmount; }
    public void setCollateralAmount(BigDecimal collateralAmount) { this.collateralAmount = collateralAmount; }
    
    public String getBorrowToken() { return borrowToken; }
    public void setBorrowToken(String borrowToken) { this.borrowToken = borrowToken; }
    
    public BigDecimal getBorrowAmount() { return borrowAmount; }
    public void setBorrowAmount(BigDecimal borrowAmount) { this.borrowAmount = borrowAmount; }
    
    public BigDecimal getMaxInterestRate() { return maxInterestRate; }
    public void setMaxInterestRate(BigDecimal maxInterestRate) { this.maxInterestRate = maxInterestRate; }
    
    public InterestRateType getRateType() { return rateType; }
    public void setRateType(InterestRateType rateType) { this.rateType = rateType; }
    
    public BigDecimal getLiquidationThreshold() { return liquidationThreshold; }
    public void setLiquidationThreshold(BigDecimal liquidationThreshold) { this.liquidationThreshold = liquidationThreshold; }
    
    public boolean isEnableRiskManagement() { return enableRiskManagement; }
    public void setEnableRiskManagement(boolean enableRiskManagement) { this.enableRiskManagement = enableRiskManagement; }
    
    public BigDecimal getAutoRepayThreshold() { return autoRepayThreshold; }
    public void setAutoRepayThreshold(BigDecimal autoRepayThreshold) { this.autoRepayThreshold = autoRepayThreshold; }
    
    public Long getDeadline() { return deadline; }
    public void setDeadline(Long deadline) { this.deadline = deadline; }
    
    // Validation methods
    public boolean isValid() {
        return userAddress != null &&
               collateralToken != null &&
               collateralAmount != null && collateralAmount.compareTo(BigDecimal.ZERO) > 0 &&
               borrowToken != null &&
               borrowAmount != null && borrowAmount.compareTo(BigDecimal.ZERO) > 0 &&
               liquidationThreshold != null && 
               liquidationThreshold.compareTo(BigDecimal.ZERO) > 0 &&
               liquidationThreshold.compareTo(BigDecimal.ONE) <= 0;
    }
    
    // Calculate collateral ratio
    public BigDecimal calculateCollateralRatio(BigDecimal collateralPrice, BigDecimal borrowPrice) {
        if (collateralPrice == null || borrowPrice == null || 
            borrowAmount == null || collateralAmount == null ||
            borrowPrice.equals(BigDecimal.ZERO) || borrowAmount.equals(BigDecimal.ZERO)) {
            return BigDecimal.ZERO;
        }
        
        BigDecimal collateralValue = collateralAmount.multiply(collateralPrice);
        BigDecimal borrowValue = borrowAmount.multiply(borrowPrice);
        
        return collateralValue.divide(borrowValue, 4, java.math.RoundingMode.HALF_UP);
    }
}