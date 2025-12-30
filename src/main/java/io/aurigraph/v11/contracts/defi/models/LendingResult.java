package io.aurigraph.v11.contracts.defi.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Sprint 4 Lending Result Model
 * Result of lending protocol operations
 */
public class LendingResult {
    
    @JsonProperty("success")
    private boolean success;
    
    @JsonProperty("transactionHash")
    private String transactionHash;
    
    @JsonProperty("positionId")
    private String positionId;
    
    @JsonProperty("userAddress")
    private String userAddress;
    
    @JsonProperty("operation")
    private LendingOperation operation;
    
    @JsonProperty("principalAmount")
    private BigDecimal principalAmount;
    
    @JsonProperty("interestRate")
    private BigDecimal interestRate;
    
    @JsonProperty("collateralAmount")
    private BigDecimal collateralAmount;
    
    @JsonProperty("collateralRatio")
    private BigDecimal collateralRatio;
    
    @JsonProperty("liquidationThreshold")
    private BigDecimal liquidationThreshold;
    
    @JsonProperty("healthFactor")
    private BigDecimal healthFactor;
    
    @JsonProperty("interestAccrued")
    private BigDecimal interestAccrued;
    
    @JsonProperty("feesIncurred")
    private BigDecimal feesIncurred;
    
    @JsonProperty("gasUsed")
    private BigDecimal gasUsed;
    
    @JsonProperty("timestamp")
    private Instant timestamp;
    
    @JsonProperty("errorMessage")
    private String errorMessage;
    
    public enum LendingOperation {
        SUPPLY,
        WITHDRAW,
        BORROW,
        REPAY,
        LIQUIDATE,
        CLAIM_REWARDS
    }
    
    // Constructors
    public LendingResult() {
        this.timestamp = Instant.now();
        this.success = true;
        this.interestAccrued = BigDecimal.ZERO;
        this.feesIncurred = BigDecimal.ZERO;
        this.gasUsed = BigDecimal.ZERO;
    }
    
    public LendingResult(boolean success) {
        this();
        this.success = success;
    }
    
    public LendingResult(String positionId, String userAddress, LendingOperation operation) {
        this();
        this.positionId = positionId;
        this.userAddress = userAddress;
        this.operation = operation;
    }
    
    // Static factory methods
    public static LendingResult success(String positionId, String userAddress, LendingOperation operation) {
        return new LendingResult(positionId, userAddress, operation);
    }
    
    public static LendingResult failure(String errorMessage) {
        LendingResult result = new LendingResult(false);
        result.setErrorMessage(errorMessage);
        return result;
    }
    
    // Getters and Setters
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    
    public String getTransactionHash() { return transactionHash; }
    public void setTransactionHash(String transactionHash) { this.transactionHash = transactionHash; }
    
    public String getPositionId() { return positionId; }
    public void setPositionId(String positionId) { this.positionId = positionId; }
    
    public String getUserAddress() { return userAddress; }
    public void setUserAddress(String userAddress) { this.userAddress = userAddress; }
    
    public LendingOperation getOperation() { return operation; }
    public void setOperation(LendingOperation operation) { this.operation = operation; }
    
    public BigDecimal getPrincipalAmount() { return principalAmount; }
    public void setPrincipalAmount(BigDecimal principalAmount) { this.principalAmount = principalAmount; }
    
    public BigDecimal getInterestRate() { return interestRate; }
    public void setInterestRate(BigDecimal interestRate) { this.interestRate = interestRate; }
    
    public BigDecimal getCollateralAmount() { return collateralAmount; }
    public void setCollateralAmount(BigDecimal collateralAmount) { this.collateralAmount = collateralAmount; }
    
    public BigDecimal getCollateralRatio() { return collateralRatio; }
    public void setCollateralRatio(BigDecimal collateralRatio) { this.collateralRatio = collateralRatio; }
    
    public BigDecimal getLiquidationThreshold() { return liquidationThreshold; }
    public void setLiquidationThreshold(BigDecimal liquidationThreshold) { this.liquidationThreshold = liquidationThreshold; }
    
    public BigDecimal getHealthFactor() { return healthFactor; }
    public void setHealthFactor(BigDecimal healthFactor) { this.healthFactor = healthFactor; }
    
    public BigDecimal getInterestAccrued() { return interestAccrued; }
    public void setInterestAccrued(BigDecimal interestAccrued) { this.interestAccrued = interestAccrued; }
    
    public BigDecimal getFeesIncurred() { return feesIncurred; }
    public void setFeesIncurred(BigDecimal feesIncurred) { this.feesIncurred = feesIncurred; }
    
    public BigDecimal getGasUsed() { return gasUsed; }
    public void setGasUsed(BigDecimal gasUsed) { this.gasUsed = gasUsed; }
    
    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
    
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    
    // Helper methods
    public boolean isHealthy() {
        return healthFactor != null && healthFactor.compareTo(BigDecimal.ONE) > 0;
    }
    
    public boolean isLiquidationEligible() {
        return healthFactor != null && healthFactor.compareTo(BigDecimal.ONE) <= 0;
    }
}