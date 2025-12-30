package io.aurigraph.v11.contracts.defi.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Sprint 4 Yield Farm Model
 * Represents a yield farming pool with staking and rewards
 */
public class YieldFarm {
    
    @JsonProperty("farmId")
    private String farmId;
    
    @JsonProperty("protocol")
    private String protocol;
    
    @JsonProperty("name")
    private String name;
    
    @JsonProperty("stakingToken")
    private String stakingToken;
    
    @JsonProperty("rewardTokens")
    private List<String> rewardTokens;
    
    @JsonProperty("totalStaked")
    private BigDecimal totalStaked;
    
    @JsonProperty("totalValueLocked")
    private BigDecimal totalValueLocked;
    
    @JsonProperty("baseAPR")
    private BigDecimal baseAPR;
    
    @JsonProperty("boostedAPR")
    private BigDecimal boostedAPR;
    
    @JsonProperty("maxAPR")
    private BigDecimal maxAPR;
    
    @JsonProperty("emissionRate")
    private BigDecimal emissionRate; // Tokens per second
    
    @JsonProperty("multiplier")
    private BigDecimal multiplier;
    
    @JsonProperty("lockupPeriod")
    private Long lockupPeriod; // in seconds
    
    @JsonProperty("minStakeAmount")
    private BigDecimal minStakeAmount;
    
    @JsonProperty("maxStakeAmount")
    private BigDecimal maxStakeAmount;
    
    @JsonProperty("depositFee")
    private BigDecimal depositFee;
    
    @JsonProperty("withdrawalFee")
    private BigDecimal withdrawalFee;
    
    @JsonProperty("performanceFee")
    private BigDecimal performanceFee;
    
    @JsonProperty("compoundFrequency")
    private Long compoundFrequency; // seconds between auto-compounds
    
    @JsonProperty("isAutoCompoundEnabled")
    private boolean isAutoCompoundEnabled;
    
    @JsonProperty("isActive")
    private boolean isActive;
    
    @JsonProperty("isPaused")
    private boolean isPaused;
    
    @JsonProperty("startTime")
    private Instant startTime;
    
    @JsonProperty("endTime")
    private Instant endTime;
    
    @JsonProperty("lastRewardTime")
    private Instant lastRewardTime;
    
    @JsonProperty("createdAt")
    private Instant createdAt;
    
    @JsonProperty("lastUpdated")
    private Instant lastUpdated;
    
    @JsonProperty("riskLevel")
    private RiskLevel riskLevel;
    
    @JsonProperty("category")
    private FarmCategory category;
    
    public enum RiskLevel {
        LOW, MEDIUM, HIGH, EXTREME
    }
    
    public enum FarmCategory {
        SINGLE_ASSET,
        LP_TOKEN,
        SYNTHETIC,
        LEVERAGED,
        CROSS_CHAIN
    }
    
    // Constructors
    public YieldFarm() {
        this.rewardTokens = new ArrayList<>();
        this.isActive = true;
        this.isPaused = false;
        this.isAutoCompoundEnabled = true;
        this.compoundFrequency = 86400L; // Daily compounding default
        this.depositFee = BigDecimal.ZERO;
        this.withdrawalFee = BigDecimal.ZERO;
        this.performanceFee = BigDecimal.valueOf(0.02); // 2% default performance fee
        this.multiplier = BigDecimal.ONE;
        this.riskLevel = RiskLevel.MEDIUM;
        this.category = FarmCategory.SINGLE_ASSET;
        this.createdAt = Instant.now();
        this.lastUpdated = Instant.now();
        this.lastRewardTime = Instant.now();
        this.totalStaked = BigDecimal.ZERO;
        this.totalValueLocked = BigDecimal.ZERO;
        this.baseAPR = BigDecimal.ZERO;
        this.boostedAPR = BigDecimal.ZERO;
        this.maxAPR = BigDecimal.ZERO;
        this.emissionRate = BigDecimal.ZERO;
    }
    
    public YieldFarm(String farmId, String protocol, String stakingToken) {
        this();
        this.farmId = farmId;
        this.protocol = protocol;
        this.stakingToken = stakingToken;
        this.name = protocol + " " + stakingToken + " Farm";
    }
    
    // Getters and Setters
    public String getFarmId() { return farmId; }
    public void setFarmId(String farmId) { this.farmId = farmId; }
    
    public String getProtocol() { return protocol; }
    public void setProtocol(String protocol) { this.protocol = protocol; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getStakingToken() { return stakingToken; }
    public void setStakingToken(String stakingToken) { this.stakingToken = stakingToken; }
    
    public List<String> getRewardTokens() { return rewardTokens; }
    public void setRewardTokens(List<String> rewardTokens) { this.rewardTokens = rewardTokens; }
    
    public BigDecimal getTotalStaked() { return totalStaked; }
    public void setTotalStaked(BigDecimal totalStaked) { 
        this.totalStaked = totalStaked;
        this.lastUpdated = Instant.now();
    }
    
    public BigDecimal getTotalValueLocked() { return totalValueLocked; }
    public void setTotalValueLocked(BigDecimal totalValueLocked) { this.totalValueLocked = totalValueLocked; }
    
    public BigDecimal getBaseAPR() { return baseAPR; }
    public void setBaseAPR(BigDecimal baseAPR) { this.baseAPR = baseAPR; }
    
    public BigDecimal getBoostedAPR() { return boostedAPR; }
    public void setBoostedAPR(BigDecimal boostedAPR) { this.boostedAPR = boostedAPR; }
    
    public BigDecimal getMaxAPR() { return maxAPR; }
    public void setMaxAPR(BigDecimal maxAPR) { this.maxAPR = maxAPR; }
    
    public BigDecimal getEmissionRate() { return emissionRate; }
    public void setEmissionRate(BigDecimal emissionRate) { this.emissionRate = emissionRate; }
    
    public BigDecimal getMultiplier() { return multiplier; }
    public void setMultiplier(BigDecimal multiplier) { this.multiplier = multiplier; }
    
    public Long getLockupPeriod() { return lockupPeriod; }
    public void setLockupPeriod(Long lockupPeriod) { this.lockupPeriod = lockupPeriod; }
    
    public BigDecimal getMinStakeAmount() { return minStakeAmount; }
    public void setMinStakeAmount(BigDecimal minStakeAmount) { this.minStakeAmount = minStakeAmount; }
    
    public BigDecimal getMaxStakeAmount() { return maxStakeAmount; }
    public void setMaxStakeAmount(BigDecimal maxStakeAmount) { this.maxStakeAmount = maxStakeAmount; }
    
    public BigDecimal getDepositFee() { return depositFee; }
    public void setDepositFee(BigDecimal depositFee) { this.depositFee = depositFee; }
    
    public BigDecimal getWithdrawalFee() { return withdrawalFee; }
    public void setWithdrawalFee(BigDecimal withdrawalFee) { this.withdrawalFee = withdrawalFee; }
    
    public BigDecimal getPerformanceFee() { return performanceFee; }
    public void setPerformanceFee(BigDecimal performanceFee) { this.performanceFee = performanceFee; }
    
    public Long getCompoundFrequency() { return compoundFrequency; }
    public void setCompoundFrequency(Long compoundFrequency) { this.compoundFrequency = compoundFrequency; }
    
    public boolean isAutoCompoundEnabled() { return isAutoCompoundEnabled; }
    public void setAutoCompoundEnabled(boolean autoCompoundEnabled) { isAutoCompoundEnabled = autoCompoundEnabled; }
    
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
    
    public boolean isPaused() { return isPaused; }
    public void setPaused(boolean paused) { isPaused = paused; }
    
    public Instant getStartTime() { return startTime; }
    public void setStartTime(Instant startTime) { this.startTime = startTime; }
    
    public Instant getEndTime() { return endTime; }
    public void setEndTime(Instant endTime) { this.endTime = endTime; }
    
    public Instant getLastRewardTime() { return lastRewardTime; }
    public void setLastRewardTime(Instant lastRewardTime) { this.lastRewardTime = lastRewardTime; }
    
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    
    public Instant getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(Instant lastUpdated) { this.lastUpdated = lastUpdated; }
    
    public RiskLevel getRiskLevel() { return riskLevel; }
    public void setRiskLevel(RiskLevel riskLevel) { this.riskLevel = riskLevel; }
    
    public FarmCategory getCategory() { return category; }
    public void setCategory(FarmCategory category) { this.category = category; }
    
    // Helper methods
    public void addRewardToken(String rewardToken) {
        if (rewardTokens == null) {
            rewardTokens = new ArrayList<>();
        }
        if (!rewardTokens.contains(rewardToken)) {
            rewardTokens.add(rewardToken);
        }
    }
    
    public boolean isExpired() {
        return endTime != null && Instant.now().isAfter(endTime);
    }
    
    public boolean isStarted() {
        return startTime == null || Instant.now().isAfter(startTime);
    }
    
    public boolean isAvailable() {
        return isActive && !isPaused && isStarted() && !isExpired();
    }
    
    public BigDecimal getCurrentAPR() {
        if (boostedAPR != null && boostedAPR.compareTo(BigDecimal.ZERO) > 0) {
            return boostedAPR;
        }
        return baseAPR != null ? baseAPR : BigDecimal.ZERO;
    }
    
    public BigDecimal calculateAPY(BigDecimal apr) {
        if (apr == null || apr.equals(BigDecimal.ZERO)) {
            return BigDecimal.ZERO;
        }
        
        // APY = (1 + APR/n)^n - 1, where n is compounding frequency per year
        double compoundingPerYear = 365.0 * 24 * 3600 / compoundFrequency;
        double aprValue = apr.doubleValue();
        double apy = Math.pow(1 + aprValue / compoundingPerYear, compoundingPerYear) - 1;
        
        return BigDecimal.valueOf(apy);
    }
    
    public BigDecimal getRiskAdjustedAPR() {
        BigDecimal currentAPR = getCurrentAPR();
        if (currentAPR == null) return BigDecimal.ZERO;
        
        // Adjust APR based on risk level
        BigDecimal riskAdjustment = switch (riskLevel) {
            case LOW -> BigDecimal.valueOf(0.95);
            case MEDIUM -> BigDecimal.valueOf(0.85);
            case HIGH -> BigDecimal.valueOf(0.70);
            case EXTREME -> BigDecimal.valueOf(0.50);
        };
        
        return currentAPR.multiply(riskAdjustment);
    }
}