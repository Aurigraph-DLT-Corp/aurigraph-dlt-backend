package io.aurigraph.v11.contracts.defi.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Sprint 4 Yield Farm Result Model
 * Result of yield farming operations
 */
public class YieldFarmResult {
    
    @JsonProperty("success")
    private boolean success;
    
    @JsonProperty("transactionHash")
    private String transactionHash;
    
    @JsonProperty("farmId")
    private String farmId;
    
    @JsonProperty("userAddress")
    private String userAddress;
    
    @JsonProperty("operation")
    private YieldFarmRequest.FarmOperation operation;
    
    @JsonProperty("stakedAmount")
    private BigDecimal stakedAmount;
    
    @JsonProperty("unstakedAmount")
    private BigDecimal unstakedAmount;
    
    @JsonProperty("rewardsClaimed")
    private Map<String, BigDecimal> rewardsClaimed;
    
    @JsonProperty("rewardsCompounded")
    private Map<String, BigDecimal> rewardsCompounded;
    
    @JsonProperty("currentStake")
    private BigDecimal currentStake;
    
    @JsonProperty("pendingRewards")
    private Map<String, BigDecimal> pendingRewards;
    
    @JsonProperty("apr")
    private BigDecimal apr;
    
    @JsonProperty("apy")
    private BigDecimal apy;
    
    @JsonProperty("multiplier")
    private BigDecimal multiplier;
    
    @JsonProperty("lockupEndTime")
    private Instant lockupEndTime;
    
    @JsonProperty("nextCompoundTime")
    private Instant nextCompoundTime;
    
    @JsonProperty("depositFee")
    private BigDecimal depositFee;
    
    @JsonProperty("withdrawalFee")
    private BigDecimal withdrawalFee;
    
    @JsonProperty("performanceFee")
    private BigDecimal performanceFee;
    
    @JsonProperty("gasUsed")
    private BigDecimal gasUsed;
    
    @JsonProperty("totalValue")
    private BigDecimal totalValue;
    
    @JsonProperty("impermanentLoss")
    private BigDecimal impermanentLoss;
    
    @JsonProperty("timestamp")
    private Instant timestamp;
    
    @JsonProperty("errorMessage")
    private String errorMessage;
    
    @JsonProperty("isAutoCompoundEnabled")
    private boolean isAutoCompoundEnabled;
    
    @JsonProperty("compoundFrequency")
    private Long compoundFrequency;
    
    // Constructors
    public YieldFarmResult() {
        this.timestamp = Instant.now();
        this.success = true;
        this.rewardsClaimed = new HashMap<>();
        this.rewardsCompounded = new HashMap<>();
        this.pendingRewards = new HashMap<>();
        this.isAutoCompoundEnabled = false;
        this.stakedAmount = BigDecimal.ZERO;
        this.unstakedAmount = BigDecimal.ZERO;
        this.currentStake = BigDecimal.ZERO;
        this.apr = BigDecimal.ZERO;
        this.apy = BigDecimal.ZERO;
        this.multiplier = BigDecimal.ONE;
        this.depositFee = BigDecimal.ZERO;
        this.withdrawalFee = BigDecimal.ZERO;
        this.performanceFee = BigDecimal.ZERO;
        this.gasUsed = BigDecimal.ZERO;
        this.totalValue = BigDecimal.ZERO;
        this.impermanentLoss = BigDecimal.ZERO;
    }
    
    public YieldFarmResult(boolean success) {
        this();
        this.success = success;
    }
    
    public YieldFarmResult(String farmId, String userAddress, YieldFarmRequest.FarmOperation operation) {
        this();
        this.farmId = farmId;
        this.userAddress = userAddress;
        this.operation = operation;
    }
    
    // Static factory methods
    public static YieldFarmResult success(String farmId, String userAddress, YieldFarmRequest.FarmOperation operation) {
        return new YieldFarmResult(farmId, userAddress, operation);
    }
    
    public static YieldFarmResult failure(String errorMessage) {
        YieldFarmResult result = new YieldFarmResult(false);
        result.setErrorMessage(errorMessage);
        return result;
    }
    
    public static YieldFarmResult stakeSuccess(String farmId, String userAddress, BigDecimal stakedAmount) {
        YieldFarmResult result = success(farmId, userAddress, YieldFarmRequest.FarmOperation.STAKE);
        result.setStakedAmount(stakedAmount);
        result.setCurrentStake(stakedAmount);
        return result;
    }
    
    public static YieldFarmResult unstakeSuccess(String farmId, String userAddress, BigDecimal unstakedAmount) {
        YieldFarmResult result = success(farmId, userAddress, YieldFarmRequest.FarmOperation.UNSTAKE);
        result.setUnstakedAmount(unstakedAmount);
        return result;
    }
    
    // Getters and Setters
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    
    public String getTransactionHash() { return transactionHash; }
    public void setTransactionHash(String transactionHash) { this.transactionHash = transactionHash; }
    
    public String getFarmId() { return farmId; }
    public void setFarmId(String farmId) { this.farmId = farmId; }
    
    public String getUserAddress() { return userAddress; }
    public void setUserAddress(String userAddress) { this.userAddress = userAddress; }
    
    public YieldFarmRequest.FarmOperation getOperation() { return operation; }
    public void setOperation(YieldFarmRequest.FarmOperation operation) { this.operation = operation; }
    
    public BigDecimal getStakedAmount() { return stakedAmount; }
    public void setStakedAmount(BigDecimal stakedAmount) { this.stakedAmount = stakedAmount; }
    
    public BigDecimal getUnstakedAmount() { return unstakedAmount; }
    public void setUnstakedAmount(BigDecimal unstakedAmount) { this.unstakedAmount = unstakedAmount; }
    
    public Map<String, BigDecimal> getRewardsClaimed() { return rewardsClaimed; }
    public void setRewardsClaimed(Map<String, BigDecimal> rewardsClaimed) { this.rewardsClaimed = rewardsClaimed; }
    
    public Map<String, BigDecimal> getRewardsCompounded() { return rewardsCompounded; }
    public void setRewardsCompounded(Map<String, BigDecimal> rewardsCompounded) { this.rewardsCompounded = rewardsCompounded; }
    
    public BigDecimal getCurrentStake() { return currentStake; }
    public void setCurrentStake(BigDecimal currentStake) { this.currentStake = currentStake; }
    
    public Map<String, BigDecimal> getPendingRewards() { return pendingRewards; }
    public void setPendingRewards(Map<String, BigDecimal> pendingRewards) { this.pendingRewards = pendingRewards; }
    
    public BigDecimal getApr() { return apr; }
    public void setApr(BigDecimal apr) { this.apr = apr; }
    
    public BigDecimal getApy() { return apy; }
    public void setApy(BigDecimal apy) { this.apy = apy; }
    
    public BigDecimal getMultiplier() { return multiplier; }
    public void setMultiplier(BigDecimal multiplier) { this.multiplier = multiplier; }
    
    public Instant getLockupEndTime() { return lockupEndTime; }
    public void setLockupEndTime(Instant lockupEndTime) { this.lockupEndTime = lockupEndTime; }
    
    public Instant getNextCompoundTime() { return nextCompoundTime; }
    public void setNextCompoundTime(Instant nextCompoundTime) { this.nextCompoundTime = nextCompoundTime; }
    
    public BigDecimal getDepositFee() { return depositFee; }
    public void setDepositFee(BigDecimal depositFee) { this.depositFee = depositFee; }
    
    public BigDecimal getWithdrawalFee() { return withdrawalFee; }
    public void setWithdrawalFee(BigDecimal withdrawalFee) { this.withdrawalFee = withdrawalFee; }
    
    public BigDecimal getPerformanceFee() { return performanceFee; }
    public void setPerformanceFee(BigDecimal performanceFee) { this.performanceFee = performanceFee; }
    
    public BigDecimal getGasUsed() { return gasUsed; }
    public void setGasUsed(BigDecimal gasUsed) { this.gasUsed = gasUsed; }
    
    public BigDecimal getTotalValue() { return totalValue; }
    public void setTotalValue(BigDecimal totalValue) { this.totalValue = totalValue; }
    
    public BigDecimal getImpermanentLoss() { return impermanentLoss; }
    public void setImpermanentLoss(BigDecimal impermanentLoss) { this.impermanentLoss = impermanentLoss; }
    
    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
    
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    
    public boolean isAutoCompoundEnabled() { return isAutoCompoundEnabled; }
    public void setAutoCompoundEnabled(boolean autoCompoundEnabled) { isAutoCompoundEnabled = autoCompoundEnabled; }
    
    public Long getCompoundFrequency() { return compoundFrequency; }
    public void setCompoundFrequency(Long compoundFrequency) { this.compoundFrequency = compoundFrequency; }
    
    // Helper methods
    public void addRewardClaimed(String token, BigDecimal amount) {
        if (rewardsClaimed == null) {
            rewardsClaimed = new HashMap<>();
        }
        rewardsClaimed.put(token, rewardsClaimed.getOrDefault(token, BigDecimal.ZERO).add(amount));
    }
    
    public void addRewardCompounded(String token, BigDecimal amount) {
        if (rewardsCompounded == null) {
            rewardsCompounded = new HashMap<>();
        }
        rewardsCompounded.put(token, rewardsCompounded.getOrDefault(token, BigDecimal.ZERO).add(amount));
    }
    
    public void addPendingReward(String token, BigDecimal amount) {
        if (pendingRewards == null) {
            pendingRewards = new HashMap<>();
        }
        pendingRewards.put(token, amount);
    }
    
    public BigDecimal getTotalRewardsClaimed() {
        return rewardsClaimed != null ? 
            rewardsClaimed.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add) :
            BigDecimal.ZERO;
    }
    
    public BigDecimal getTotalRewardsCompounded() {
        return rewardsCompounded != null ? 
            rewardsCompounded.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add) :
            BigDecimal.ZERO;
    }
    
    public BigDecimal getTotalPendingRewards() {
        return pendingRewards != null ? 
            pendingRewards.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add) :
            BigDecimal.ZERO;
    }
    
    public boolean isLocked() {
        return lockupEndTime != null && Instant.now().isBefore(lockupEndTime);
    }
    
    public boolean canCompound() {
        return isAutoCompoundEnabled && nextCompoundTime != null && 
               Instant.now().isAfter(nextCompoundTime) &&
               getTotalPendingRewards().compareTo(BigDecimal.ZERO) > 0;
    }
    
    public BigDecimal getNetReturn() {
        BigDecimal totalEarnings = getTotalRewardsClaimed().add(getTotalRewardsCompounded());
        BigDecimal totalFees = depositFee.add(withdrawalFee).add(performanceFee);
        return totalEarnings.subtract(totalFees);
    }
}