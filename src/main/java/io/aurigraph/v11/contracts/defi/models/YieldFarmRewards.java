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
 * Sprint 4 Enhanced Yield Farm Rewards
 * Represents yield farming rewards with auto-compounding and risk-adjusted returns
 * Supports multiple reward tokens, boosting mechanisms, and vesting schedules
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class YieldFarmRewards {
    
    @JsonProperty("farmId")
    private String farmId;
    
    @JsonProperty("userAddress")
    private String userAddress;
    
    @JsonProperty("stakedAmount")
    private BigDecimal stakedAmount;
    
    @JsonProperty("stakingToken")
    private String stakingToken;
    
    @JsonProperty("rewardTokens")
    private List<RewardToken> rewardTokens;
    
    @JsonProperty("totalRewardsEarned")
    private BigDecimal totalRewardsEarned;
    
    @JsonProperty("pendingRewards")
    private BigDecimal pendingRewards;
    
    @JsonProperty("claimedRewards")
    private BigDecimal claimedRewards;
    
    @JsonProperty("baseApr")
    private BigDecimal baseApr;
    
    @JsonProperty("boostedApr")
    private BigDecimal boostedApr;
    
    @JsonProperty("boostMultiplier")
    private BigDecimal boostMultiplier;
    
    @JsonProperty("stakingStartTime")
    private Instant stakingStartTime;
    
    @JsonProperty("lastClaimTime")
    private Instant lastClaimTime;
    
    @JsonProperty("lockupPeriod")
    private Long lockupPeriod; // in seconds
    
    @JsonProperty("vestingSchedule")
    private VestingSchedule vestingSchedule;
    
    @JsonProperty("compoundingEnabled")
    private Boolean compoundingEnabled;
    
    @JsonProperty("autoCompoundFrequency")
    private Long autoCompoundFrequency; // in seconds
    
    @JsonProperty("lastCompoundTime")
    private Instant lastCompoundTime;
    
    // Sprint 4 Enhancements
    @JsonProperty("riskAdjustedAPR")
    private BigDecimal riskAdjustedAPR;
    
    @JsonProperty("impermanentLossRisk")
    private BigDecimal impermanentLossRisk;
    
    @JsonProperty("farmMetadata")
    private Map<String, Object> farmMetadata;
    
    @JsonProperty("performance")
    private FarmPerformance performance;
    
    // Constructors
    public YieldFarmRewards() {
        this.stakingStartTime = Instant.now();
        this.totalRewardsEarned = BigDecimal.ZERO;
        this.pendingRewards = BigDecimal.ZERO;
        this.claimedRewards = BigDecimal.ZERO;
        this.boostMultiplier = BigDecimal.ONE;
        this.compoundingEnabled = false;
        this.rewardTokens = new ArrayList<>();
        this.farmMetadata = new HashMap<>();
        this.riskAdjustedAPR = BigDecimal.ZERO;
        this.impermanentLossRisk = BigDecimal.ZERO;
    }
    
    public YieldFarmRewards(String farmId, String userAddress, BigDecimal stakedAmount, String stakingToken) {
        this();
        this.farmId = farmId;
        this.userAddress = userAddress;
        this.stakedAmount = stakedAmount;
        this.stakingToken = stakingToken;
    }
    
    // Getters and Setters
    public String getFarmId() { return farmId; }
    public void setFarmId(String farmId) { this.farmId = farmId; }
    
    public String getUserAddress() { return userAddress; }
    public void setUserAddress(String userAddress) { this.userAddress = userAddress; }
    
    public BigDecimal getStakedAmount() { return stakedAmount; }
    public void setStakedAmount(BigDecimal stakedAmount) { this.stakedAmount = stakedAmount; }
    
    public String getStakingToken() { return stakingToken; }
    public void setStakingToken(String stakingToken) { this.stakingToken = stakingToken; }
    
    public List<RewardToken> getRewardTokens() { return rewardTokens; }
    public void setRewardTokens(List<RewardToken> rewardTokens) { this.rewardTokens = rewardTokens; }
    
    public BigDecimal getTotalRewardsEarned() { return totalRewardsEarned; }
    public void setTotalRewardsEarned(BigDecimal totalRewardsEarned) { this.totalRewardsEarned = totalRewardsEarned; }
    
    public BigDecimal getPendingRewards() { return pendingRewards; }
    public void setPendingRewards(BigDecimal pendingRewards) { this.pendingRewards = pendingRewards; }
    
    public BigDecimal getClaimedRewards() { return claimedRewards; }
    public void setClaimedRewards(BigDecimal claimedRewards) { this.claimedRewards = claimedRewards; }
    
    public BigDecimal getBaseApr() { return baseApr; }
    public void setBaseApr(BigDecimal baseApr) { this.baseApr = baseApr; }
    
    public BigDecimal getBoostedApr() { return boostedApr; }
    public void setBoostedApr(BigDecimal boostedApr) { this.boostedApr = boostedApr; }
    
    public BigDecimal getBoostMultiplier() { return boostMultiplier; }
    public void setBoostMultiplier(BigDecimal boostMultiplier) { this.boostMultiplier = boostMultiplier; }
    
    public Instant getStakingStartTime() { return stakingStartTime; }
    public void setStakingStartTime(Instant stakingStartTime) { this.stakingStartTime = stakingStartTime; }
    
    public Instant getLastClaimTime() { return lastClaimTime; }
    public void setLastClaimTime(Instant lastClaimTime) { this.lastClaimTime = lastClaimTime; }
    
    public Long getLockupPeriod() { return lockupPeriod; }
    public void setLockupPeriod(Long lockupPeriod) { this.lockupPeriod = lockupPeriod; }
    
    public VestingSchedule getVestingSchedule() { return vestingSchedule; }
    public void setVestingSchedule(VestingSchedule vestingSchedule) { this.vestingSchedule = vestingSchedule; }
    
    public Boolean getCompoundingEnabled() { return compoundingEnabled; }
    public boolean isCompoundingEnabled() { return compoundingEnabled != null ? compoundingEnabled : false; }
    public void setCompoundingEnabled(Boolean compoundingEnabled) { this.compoundingEnabled = compoundingEnabled; }
    
    public Long getAutoCompoundFrequency() { return autoCompoundFrequency; }
    public void setAutoCompoundFrequency(Long autoCompoundFrequency) { this.autoCompoundFrequency = autoCompoundFrequency; }
    
    public Instant getLastCompoundTime() { return lastCompoundTime; }
    public void setLastCompoundTime(Instant lastCompoundTime) { this.lastCompoundTime = lastCompoundTime; }
    
    public BigDecimal getRiskAdjustedAPR() { return riskAdjustedAPR; }
    public void setRiskAdjustedAPR(BigDecimal riskAdjustedAPR) { this.riskAdjustedAPR = riskAdjustedAPR; }
    
    public BigDecimal getImpermanentLossRisk() { return impermanentLossRisk; }
    public void setImpermanentLossRisk(BigDecimal impermanentLossRisk) { this.impermanentLossRisk = impermanentLossRisk; }
    
    public Map<String, Object> getFarmMetadata() { return farmMetadata; }
    public void setFarmMetadata(Map<String, Object> farmMetadata) { this.farmMetadata = farmMetadata; }
    
    public FarmPerformance getPerformance() { return performance; }
    public void setPerformance(FarmPerformance performance) { this.performance = performance; }
    
    // Business logic methods
    public BigDecimal calculatePendingRewards() {
        if (stakingStartTime == null || baseApr == null || stakedAmount == null) {
            return BigDecimal.ZERO;
        }
        
        Instant now = Instant.now();
        long stakingDuration = now.getEpochSecond() - stakingStartTime.getEpochSecond();
        BigDecimal annualSeconds = BigDecimal.valueOf(365 * 24 * 3600);
        
        // Calculate base rewards
        BigDecimal baseRewards = stakedAmount
            .multiply(baseApr)
            .multiply(BigDecimal.valueOf(stakingDuration))
            .divide(annualSeconds, 8, RoundingMode.HALF_UP);
        
        // Apply boost multiplier
        BigDecimal totalRewards = baseRewards.multiply(boostMultiplier);
        
        // Subtract already claimed rewards
        return totalRewards.subtract(claimedRewards);
    }
    
    public boolean canClaim() {
        if (lockupPeriod == null || lockupPeriod == 0) return true;
        
        Instant now = Instant.now();
        long stakingDuration = now.getEpochSecond() - stakingStartTime.getEpochSecond();
        return stakingDuration >= lockupPeriod;
    }
    
    public boolean shouldAutoCompound() {
        if (!compoundingEnabled || autoCompoundFrequency == null) return false;
        
        Instant now = Instant.now();
        if (lastCompoundTime == null) return true;
        
        long timeSinceLastCompound = now.getEpochSecond() - lastCompoundTime.getEpochSecond();
        return timeSinceLastCompound >= autoCompoundFrequency;
    }
    
    public BigDecimal getEffectiveApr() {
        if (boostedApr != null) return boostedApr;
        if (baseApr != null && boostMultiplier != null) {
            return baseApr.multiply(boostMultiplier);
        }
        return baseApr != null ? baseApr : BigDecimal.ZERO;
    }
    
    // Inner classes
    public static class RewardToken {
        @JsonProperty("tokenAddress")
        private String tokenAddress;
        
        @JsonProperty("tokenSymbol")
        private String tokenSymbol;
        
        @JsonProperty("rewardRate")
        private BigDecimal rewardRate;
        
        @JsonProperty("totalEarned")
        private BigDecimal totalEarned;
        
        @JsonProperty("pendingAmount")
        private BigDecimal pendingAmount;
        
        public RewardToken() {}
        
        public RewardToken(String tokenAddress, String tokenSymbol, BigDecimal rewardRate) {
            this.tokenAddress = tokenAddress;
            this.tokenSymbol = tokenSymbol;
            this.rewardRate = rewardRate;
            this.totalEarned = BigDecimal.ZERO;
            this.pendingAmount = BigDecimal.ZERO;
        }
        
        // Getters and setters
        public String getTokenAddress() { return tokenAddress; }
        public void setTokenAddress(String tokenAddress) { this.tokenAddress = tokenAddress; }
        
        public String getTokenSymbol() { return tokenSymbol; }
        public void setTokenSymbol(String tokenSymbol) { this.tokenSymbol = tokenSymbol; }
        
        public BigDecimal getRewardRate() { return rewardRate; }
        public void setRewardRate(BigDecimal rewardRate) { this.rewardRate = rewardRate; }
        
        public BigDecimal getTotalEarned() { return totalEarned; }
        public void setTotalEarned(BigDecimal totalEarned) { this.totalEarned = totalEarned; }
        
        public BigDecimal getPendingAmount() { return pendingAmount; }
        public void setPendingAmount(BigDecimal pendingAmount) { this.pendingAmount = pendingAmount; }
    }
    
    public static class VestingSchedule {
        @JsonProperty("vestingPeriod")
        private Long vestingPeriod;
        
        @JsonProperty("cliffPeriod")
        private Long cliffPeriod;
        
        @JsonProperty("vestingSteps")
        private Integer vestingSteps;
        
        @JsonProperty("vestedAmount")
        private BigDecimal vestedAmount;
        
        public VestingSchedule() {}
        
        public VestingSchedule(Long vestingPeriod, Long cliffPeriod, Integer vestingSteps) {
            this.vestingPeriod = vestingPeriod;
            this.cliffPeriod = cliffPeriod;
            this.vestingSteps = vestingSteps;
            this.vestedAmount = BigDecimal.ZERO;
        }
        
        // Getters and setters
        public Long getVestingPeriod() { return vestingPeriod; }
        public void setVestingPeriod(Long vestingPeriod) { this.vestingPeriod = vestingPeriod; }
        
        public Long getCliffPeriod() { return cliffPeriod; }
        public void setCliffPeriod(Long cliffPeriod) { this.cliffPeriod = cliffPeriod; }
        
        public Integer getVestingSteps() { return vestingSteps; }
        public void setVestingSteps(Integer vestingSteps) { this.vestingSteps = vestingSteps; }
        
        public BigDecimal getVestedAmount() { return vestedAmount; }
        public void setVestedAmount(BigDecimal vestedAmount) { this.vestedAmount = vestedAmount; }
    }
    
    public static class FarmPerformance {
        @JsonProperty("totalValueLocked")
        private BigDecimal totalValueLocked;
        
        @JsonProperty("participantCount")
        private Long participantCount;
        
        @JsonProperty("averageApr")
        private BigDecimal averageApr;
        
        @JsonProperty("rewardMultiplier")
        private BigDecimal rewardMultiplier;
        
        public FarmPerformance() {}
        
        // Getters and setters
        public BigDecimal getTotalValueLocked() { return totalValueLocked; }
        public void setTotalValueLocked(BigDecimal totalValueLocked) { this.totalValueLocked = totalValueLocked; }
        
        public Long getParticipantCount() { return participantCount; }
        public void setParticipantCount(Long participantCount) { this.participantCount = participantCount; }
        
        public BigDecimal getAverageApr() { return averageApr; }
        public void setAverageApr(BigDecimal averageApr) { this.averageApr = averageApr; }
        
        public BigDecimal getRewardMultiplier() { return rewardMultiplier; }
        public void setRewardMultiplier(BigDecimal rewardMultiplier) { this.rewardMultiplier = rewardMultiplier; }
    }
    
    @Override
    public String toString() {
        return String.format("YieldFarmRewards{farmId='%s', stakedAmount=%s, totalRewards=%s, effectiveApr=%s}", 
                           farmId, stakedAmount, totalRewardsEarned, getEffectiveApr());
    }
}