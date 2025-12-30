package io.aurigraph.v11.contracts.defi.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

/**
 * Sprint 4 Yield Farm Request Model
 * Request for yield farming operations
 */
public class YieldFarmRequest {
    
    @JsonProperty("farmId")
    private String farmId;
    
    @JsonProperty("userAddress")
    private String userAddress;
    
    @JsonProperty("operation")
    private FarmOperation operation;
    
    @JsonProperty("amount")
    private BigDecimal amount;
    
    @JsonProperty("stakingToken")
    private String stakingToken;
    
    @JsonProperty("enableAutoCompound")
    private boolean enableAutoCompound;
    
    @JsonProperty("compoundFrequency")
    private Long compoundFrequency;
    
    @JsonProperty("lockupPeriod")
    private Long lockupPeriod;
    
    @JsonProperty("maxSlippage")
    private BigDecimal maxSlippage;
    
    @JsonProperty("minRewardAmount")
    private BigDecimal minRewardAmount;
    
    @JsonProperty("deadline")
    private Long deadline;
    
    @JsonProperty("referrer")
    private String referrer;
    
    @JsonProperty("reinvestRewards")
    private boolean reinvestRewards;
    
    public enum FarmOperation {
        STAKE,
        UNSTAKE,
        CLAIM_REWARDS,
        COMPOUND,
        EMERGENCY_WITHDRAW,
        UPDATE_REWARDS,
        BOOST_STAKE,
        MIGRATE_FARM
    }
    
    // Constructors
    public YieldFarmRequest() {
        this.enableAutoCompound = true;
        this.compoundFrequency = 86400L; // Daily compounding default
        this.maxSlippage = BigDecimal.valueOf(0.005); // 0.5% default slippage
        this.deadline = System.currentTimeMillis() + 1800000; // 30 minutes default
        this.reinvestRewards = true;
    }
    
    public YieldFarmRequest(String farmId, String userAddress, FarmOperation operation) {
        this();
        this.farmId = farmId;
        this.userAddress = userAddress;
        this.operation = operation;
    }
    
    public YieldFarmRequest(String farmId, String userAddress, FarmOperation operation, BigDecimal amount) {
        this(farmId, userAddress, operation);
        this.amount = amount;
    }
    
    // Getters and Setters
    public String getFarmId() { return farmId; }
    public void setFarmId(String farmId) { this.farmId = farmId; }
    
    public String getUserAddress() { return userAddress; }
    public void setUserAddress(String userAddress) { this.userAddress = userAddress; }
    
    public FarmOperation getOperation() { return operation; }
    public void setOperation(FarmOperation operation) { this.operation = operation; }
    
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    
    public String getStakingToken() { return stakingToken; }
    public void setStakingToken(String stakingToken) { this.stakingToken = stakingToken; }
    
    public boolean isEnableAutoCompound() { return enableAutoCompound; }
    public void setEnableAutoCompound(boolean enableAutoCompound) { this.enableAutoCompound = enableAutoCompound; }
    
    public Long getCompoundFrequency() { return compoundFrequency; }
    public void setCompoundFrequency(Long compoundFrequency) { this.compoundFrequency = compoundFrequency; }
    
    public Long getLockupPeriod() { return lockupPeriod; }
    public void setLockupPeriod(Long lockupPeriod) { this.lockupPeriod = lockupPeriod; }
    
    public BigDecimal getMaxSlippage() { return maxSlippage; }
    public void setMaxSlippage(BigDecimal maxSlippage) { this.maxSlippage = maxSlippage; }
    
    public BigDecimal getMinRewardAmount() { return minRewardAmount; }
    public void setMinRewardAmount(BigDecimal minRewardAmount) { this.minRewardAmount = minRewardAmount; }
    
    public Long getDeadline() { return deadline; }
    public void setDeadline(Long deadline) { this.deadline = deadline; }
    
    public String getReferrer() { return referrer; }
    public void setReferrer(String referrer) { this.referrer = referrer; }
    
    public boolean isReinvestRewards() { return reinvestRewards; }
    public void setReinvestRewards(boolean reinvestRewards) { this.reinvestRewards = reinvestRewards; }
    
    // Validation methods
    public boolean isValid() {
        boolean basicValid = farmId != null && 
                           userAddress != null && 
                           operation != null;
        
        // Amount validation for operations that require it
        if (operation == FarmOperation.STAKE || 
            operation == FarmOperation.UNSTAKE || 
            operation == FarmOperation.BOOST_STAKE) {
            return basicValid && amount != null && amount.compareTo(BigDecimal.ZERO) > 0;
        }
        
        return basicValid;
    }
    
    // Static factory methods
    public static YieldFarmRequest stakeRequest(String farmId, String userAddress, BigDecimal amount) {
        return new YieldFarmRequest(farmId, userAddress, FarmOperation.STAKE, amount);
    }
    
    public static YieldFarmRequest unstakeRequest(String farmId, String userAddress, BigDecimal amount) {
        return new YieldFarmRequest(farmId, userAddress, FarmOperation.UNSTAKE, amount);
    }
    
    public static YieldFarmRequest claimRequest(String farmId, String userAddress) {
        return new YieldFarmRequest(farmId, userAddress, FarmOperation.CLAIM_REWARDS);
    }
    
    public static YieldFarmRequest compoundRequest(String farmId, String userAddress) {
        return new YieldFarmRequest(farmId, userAddress, FarmOperation.COMPOUND);
    }
    
    public static YieldFarmRequest emergencyWithdrawRequest(String farmId, String userAddress) {
        YieldFarmRequest request = new YieldFarmRequest(farmId, userAddress, FarmOperation.EMERGENCY_WITHDRAW);
        request.setEnableAutoCompound(false);
        request.setReinvestRewards(false);
        return request;
    }
    
    // Helper methods
    public boolean requiresAmount() {
        return operation == FarmOperation.STAKE || 
               operation == FarmOperation.UNSTAKE || 
               operation == FarmOperation.BOOST_STAKE;
    }
    
    public boolean isEmergencyOperation() {
        return operation == FarmOperation.EMERGENCY_WITHDRAW;
    }
    
    public boolean isRewardOperation() {
        return operation == FarmOperation.CLAIM_REWARDS || 
               operation == FarmOperation.COMPOUND;
    }
}