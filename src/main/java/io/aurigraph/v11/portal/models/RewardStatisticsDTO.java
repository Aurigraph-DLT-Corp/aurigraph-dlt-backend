package io.aurigraph.v11.portal.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.List;

public class RewardStatisticsDTO {
    @JsonProperty("total_rewards_generated")
    private String totalRewardsGenerated;
    @JsonProperty("total_rewards_distributed")
    private String totalRewardsDistributed;
    @JsonProperty("total_rewards_burned")
    private String totalRewardsBurned;
    @JsonProperty("total_rewards_pending")
    private String totalRewardsPending;
    @JsonProperty("daily_reward_generation")
    private String dailyRewardGeneration;
    @JsonProperty("weekly_reward_generation")
    private String weeklyRewardGeneration;
    @JsonProperty("monthly_reward_generation")
    private String monthlyRewardGeneration;
    @JsonProperty("average_reward_wait_time")
    private Integer averageRewardWaitTime;
    @JsonProperty("min_reward_amount")
    private String minRewardAmount;
    @JsonProperty("max_reward_amount")
    private String maxRewardAmount;
    @JsonProperty("average_reward_amount")
    private String averageRewardAmount;
    @JsonProperty("median_reward_amount")
    private String medianRewardAmount;
    @JsonProperty("top_reward_recipient")
    private String topRewardRecipient;
    @JsonProperty("top_reward_amount")
    private String topRewardAmount;
    @JsonProperty("reward_growth_rate")
    private Double rewardGrowthRate;
    @JsonProperty("claim_rate")
    private Double claimRate;
    @JsonProperty("reinvestment_rate")
    private Double reinvestmentRate;
    @JsonProperty("auto_compounding_enabled")
    private Boolean autoCompoundingEnabled;
    @JsonProperty("tax_rate")
    private Double taxRate;
    @JsonProperty("error")
    private String error;

    public RewardStatisticsDTO() {}

    private RewardStatisticsDTO(Builder builder) {
        this.totalRewardsGenerated = builder.totalRewardsGenerated;
        this.totalRewardsDistributed = builder.totalRewardsDistributed;
        this.totalRewardsBurned = builder.totalRewardsBurned;
        this.totalRewardsPending = builder.totalRewardsPending;
        this.dailyRewardGeneration = builder.dailyRewardGeneration;
        this.weeklyRewardGeneration = builder.weeklyRewardGeneration;
        this.monthlyRewardGeneration = builder.monthlyRewardGeneration;
        this.averageRewardWaitTime = builder.averageRewardWaitTime;
        this.minRewardAmount = builder.minRewardAmount;
        this.maxRewardAmount = builder.maxRewardAmount;
        this.averageRewardAmount = builder.averageRewardAmount;
        this.medianRewardAmount = builder.medianRewardAmount;
        this.topRewardRecipient = builder.topRewardRecipient;
        this.topRewardAmount = builder.topRewardAmount;
        this.rewardGrowthRate = builder.rewardGrowthRate;
        this.claimRate = builder.claimRate;
        this.reinvestmentRate = builder.reinvestmentRate;
        this.autoCompoundingEnabled = builder.autoCompoundingEnabled;
        this.taxRate = builder.taxRate;
        this.error = builder.error;
    }

    public String getTotalRewardsGenerated() { return totalRewardsGenerated; }
    public String getTotalRewardsDistributed() { return totalRewardsDistributed; }
    public String getTotalRewardsBurned() { return totalRewardsBurned; }
    public String getTotalRewardsPending() { return totalRewardsPending; }
    public String getDailyRewardGeneration() { return dailyRewardGeneration; }
    public String getWeeklyRewardGeneration() { return weeklyRewardGeneration; }
    public String getMonthlyRewardGeneration() { return monthlyRewardGeneration; }
    public Integer getAverageRewardWaitTime() { return averageRewardWaitTime; }
    public String getMinRewardAmount() { return minRewardAmount; }
    public String getMaxRewardAmount() { return maxRewardAmount; }
    public String getAverageRewardAmount() { return averageRewardAmount; }
    public String getMedianRewardAmount() { return medianRewardAmount; }
    public String getTopRewardRecipient() { return topRewardRecipient; }
    public String getTopRewardAmount() { return topRewardAmount; }
    public Double getRewardGrowthRate() { return rewardGrowthRate; }
    public Double getClaimRate() { return claimRate; }
    public Double getReinvestmentRate() { return reinvestmentRate; }
    public Boolean getAutoCompoundingEnabled() { return autoCompoundingEnabled; }
    public Double getTaxRate() { return taxRate; }
    public String getError() { return error; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String totalRewardsGenerated;
        private String totalRewardsDistributed;
        private String totalRewardsBurned;
        private String totalRewardsPending;
        private String dailyRewardGeneration;
        private String weeklyRewardGeneration;
        private String monthlyRewardGeneration;
        private Integer averageRewardWaitTime;
        private String minRewardAmount;
        private String maxRewardAmount;
        private String averageRewardAmount;
        private String medianRewardAmount;
        private String topRewardRecipient;
        private String topRewardAmount;
        private Double rewardGrowthRate;
        private Double claimRate;
        private Double reinvestmentRate;
        private Boolean autoCompoundingEnabled;
        private Double taxRate;
        private String error;

        public Builder totalRewardsGenerated(String totalRewardsGenerated) { this.totalRewardsGenerated = totalRewardsGenerated; return this; }
        public Builder totalRewardsDistributed(String totalRewardsDistributed) { this.totalRewardsDistributed = totalRewardsDistributed; return this; }
        public Builder totalRewardsBurned(String totalRewardsBurned) { this.totalRewardsBurned = totalRewardsBurned; return this; }
        public Builder totalRewardsPending(String totalRewardsPending) { this.totalRewardsPending = totalRewardsPending; return this; }
        public Builder dailyRewardGeneration(String dailyRewardGeneration) { this.dailyRewardGeneration = dailyRewardGeneration; return this; }
        public Builder weeklyRewardGeneration(String weeklyRewardGeneration) { this.weeklyRewardGeneration = weeklyRewardGeneration; return this; }
        public Builder monthlyRewardGeneration(String monthlyRewardGeneration) { this.monthlyRewardGeneration = monthlyRewardGeneration; return this; }
        public Builder averageRewardWaitTime(Integer averageRewardWaitTime) { this.averageRewardWaitTime = averageRewardWaitTime; return this; }
        public Builder minRewardAmount(String minRewardAmount) { this.minRewardAmount = minRewardAmount; return this; }
        public Builder maxRewardAmount(String maxRewardAmount) { this.maxRewardAmount = maxRewardAmount; return this; }
        public Builder averageRewardAmount(String averageRewardAmount) { this.averageRewardAmount = averageRewardAmount; return this; }
        public Builder medianRewardAmount(String medianRewardAmount) { this.medianRewardAmount = medianRewardAmount; return this; }
        public Builder topRewardRecipient(String topRewardRecipient) { this.topRewardRecipient = topRewardRecipient; return this; }
        public Builder topRewardAmount(String topRewardAmount) { this.topRewardAmount = topRewardAmount; return this; }
        public Builder rewardGrowthRate(Double rewardGrowthRate) { this.rewardGrowthRate = rewardGrowthRate; return this; }
        public Builder claimRate(Double claimRate) { this.claimRate = claimRate; return this; }
        public Builder reinvestmentRate(Double reinvestmentRate) { this.reinvestmentRate = reinvestmentRate; return this; }
        public Builder autoCompoundingEnabled(Boolean autoCompoundingEnabled) { this.autoCompoundingEnabled = autoCompoundingEnabled; return this; }
        public Builder taxRate(Double taxRate) { this.taxRate = taxRate; return this; }
        public Builder error(String error) { this.error = error; return this; }

        public RewardStatisticsDTO build() { return new RewardStatisticsDTO(this); }
    }
}
