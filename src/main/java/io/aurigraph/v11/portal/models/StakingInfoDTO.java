package io.aurigraph.v11.portal.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.List;

public class StakingInfoDTO {
    @JsonProperty("total_staked")
    private String totalStaked;
    @JsonProperty("staked_percentage")
    private Double stakedPercentage;
    @JsonProperty("total_validators")
    private Integer totalValidators;
    @JsonProperty("active_validators")
    private Integer activeValidators;
    @JsonProperty("min_stake_amount")
    private String minStakeAmount;
    @JsonProperty("max_stake_amount")
    private String maxStakeAmount;
    @JsonProperty("average_validator_stake")
    private String averageValidatorStake;
    @JsonProperty("median_validator_stake")
    private String medianValidatorStake;
    @JsonProperty("unbonding_period")
    private Integer unbondingPeriod;
    @JsonProperty("commission_min")
    private Double commissionMin;
    @JsonProperty("commission_max")
    private Double commissionMax;
    @JsonProperty("average_commission")
    private Double averageCommission;
    @JsonProperty("annual_reward_rate")
    private Double annualRewardRate;
    @JsonProperty("estimated_annual_reward")
    private String estimatedAnnualReward;
    @JsonProperty("slashing_rate")
    private Double slashingRate;
    @JsonProperty("jail_period")
    private Integer jailPeriod;
    @JsonProperty("total_rewards_paid")
    private String totalRewardsPaid;
    @JsonProperty("rewards_distributed_last24h")
    private String rewardsDistributedLast24h;
    @JsonProperty("next_reward_distribution")
    private Instant nextRewardDistribution;
    @JsonProperty("error")
    private String error;

    public StakingInfoDTO() {}

    private StakingInfoDTO(Builder builder) {
        this.totalStaked = builder.totalStaked;
        this.stakedPercentage = builder.stakedPercentage;
        this.totalValidators = builder.totalValidators;
        this.activeValidators = builder.activeValidators;
        this.minStakeAmount = builder.minStakeAmount;
        this.maxStakeAmount = builder.maxStakeAmount;
        this.averageValidatorStake = builder.averageValidatorStake;
        this.medianValidatorStake = builder.medianValidatorStake;
        this.unbondingPeriod = builder.unbondingPeriod;
        this.commissionMin = builder.commissionMin;
        this.commissionMax = builder.commissionMax;
        this.averageCommission = builder.averageCommission;
        this.annualRewardRate = builder.annualRewardRate;
        this.estimatedAnnualReward = builder.estimatedAnnualReward;
        this.slashingRate = builder.slashingRate;
        this.jailPeriod = builder.jailPeriod;
        this.totalRewardsPaid = builder.totalRewardsPaid;
        this.rewardsDistributedLast24h = builder.rewardsDistributedLast24h;
        this.nextRewardDistribution = builder.nextRewardDistribution;
        this.error = builder.error;
    }

    public String getTotalStaked() { return totalStaked; }
    public Double getStakedPercentage() { return stakedPercentage; }
    public Integer getTotalValidators() { return totalValidators; }
    public Integer getActiveValidators() { return activeValidators; }
    public String getMinStakeAmount() { return minStakeAmount; }
    public String getMaxStakeAmount() { return maxStakeAmount; }
    public String getAverageValidatorStake() { return averageValidatorStake; }
    public String getMedianValidatorStake() { return medianValidatorStake; }
    public Integer getUnbondingPeriod() { return unbondingPeriod; }
    public Double getCommissionMin() { return commissionMin; }
    public Double getCommissionMax() { return commissionMax; }
    public Double getAverageCommission() { return averageCommission; }
    public Double getAnnualRewardRate() { return annualRewardRate; }
    public String getEstimatedAnnualReward() { return estimatedAnnualReward; }
    public Double getSlashingRate() { return slashingRate; }
    public Integer getJailPeriod() { return jailPeriod; }
    public String getTotalRewardsPaid() { return totalRewardsPaid; }
    public String getRewardsDistributedLast24h() { return rewardsDistributedLast24h; }
    public Instant getNextRewardDistribution() { return nextRewardDistribution; }
    public String getError() { return error; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String totalStaked;
        private Double stakedPercentage;
        private Integer totalValidators;
        private Integer activeValidators;
        private String minStakeAmount;
        private String maxStakeAmount;
        private String averageValidatorStake;
        private String medianValidatorStake;
        private Integer unbondingPeriod;
        private Double commissionMin;
        private Double commissionMax;
        private Double averageCommission;
        private Double annualRewardRate;
        private String estimatedAnnualReward;
        private Double slashingRate;
        private Integer jailPeriod;
        private String totalRewardsPaid;
        private String rewardsDistributedLast24h;
        private Instant nextRewardDistribution;
        private String error;

        public Builder totalStaked(String totalStaked) { this.totalStaked = totalStaked; return this; }
        public Builder stakedPercentage(Double stakedPercentage) { this.stakedPercentage = stakedPercentage; return this; }
        public Builder totalValidators(Integer totalValidators) { this.totalValidators = totalValidators; return this; }
        public Builder activeValidators(Integer activeValidators) { this.activeValidators = activeValidators; return this; }
        public Builder minStakeAmount(String minStakeAmount) { this.minStakeAmount = minStakeAmount; return this; }
        public Builder maxStakeAmount(String maxStakeAmount) { this.maxStakeAmount = maxStakeAmount; return this; }
        public Builder averageValidatorStake(String averageValidatorStake) { this.averageValidatorStake = averageValidatorStake; return this; }
        public Builder medianValidatorStake(String medianValidatorStake) { this.medianValidatorStake = medianValidatorStake; return this; }
        public Builder unbondingPeriod(Integer unbondingPeriod) { this.unbondingPeriod = unbondingPeriod; return this; }
        public Builder commissionMin(Double commissionMin) { this.commissionMin = commissionMin; return this; }
        public Builder commissionMax(Double commissionMax) { this.commissionMax = commissionMax; return this; }
        public Builder averageCommission(Double averageCommission) { this.averageCommission = averageCommission; return this; }
        public Builder annualRewardRate(Double annualRewardRate) { this.annualRewardRate = annualRewardRate; return this; }
        public Builder estimatedAnnualReward(String estimatedAnnualReward) { this.estimatedAnnualReward = estimatedAnnualReward; return this; }
        public Builder slashingRate(Double slashingRate) { this.slashingRate = slashingRate; return this; }
        public Builder jailPeriod(Integer jailPeriod) { this.jailPeriod = jailPeriod; return this; }
        public Builder totalRewardsPaid(String totalRewardsPaid) { this.totalRewardsPaid = totalRewardsPaid; return this; }
        public Builder rewardsDistributedLast24h(String rewardsDistributedLast24h) { this.rewardsDistributedLast24h = rewardsDistributedLast24h; return this; }
        public Builder nextRewardDistribution(Instant nextRewardDistribution) { this.nextRewardDistribution = nextRewardDistribution; return this; }
        public Builder error(String error) { this.error = error; return this; }

        public StakingInfoDTO build() { return new StakingInfoDTO(this); }
    }
}
