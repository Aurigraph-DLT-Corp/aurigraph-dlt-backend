package io.aurigraph.v11.portal.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.List;

public class RewardDistributionDTO {
    @JsonProperty("pool_id")
    private String poolId;
    @JsonProperty("pool_name")
    private String poolName;
    @JsonProperty("description")
    private String description;
    @JsonProperty("pool_type")
    private String poolType;
    @JsonProperty("total_rewards")
    private String totalRewards;
    @JsonProperty("rewards_distributed_last24h")
    private String rewardsDistributedLast24h;
    @JsonProperty("rewards_distributed_last30d")
    private String rewardsDistributedLast30d;
    @JsonProperty("participant_count")
    private Integer participantCount;
    @JsonProperty("average_reward_per_participant")
    private String averageRewardPerParticipant;
    @JsonProperty("reward_frequency")
    private String rewardFrequency;
    @JsonProperty("next_distribution")
    private Instant nextDistribution;
    @JsonProperty("distribution_percentage")
    private Double distributionPercentage;
    @JsonProperty("status")
    private String status;

    public RewardDistributionDTO() {}

    private RewardDistributionDTO(Builder builder) {
        this.poolId = builder.poolId;
        this.poolName = builder.poolName;
        this.description = builder.description;
        this.poolType = builder.poolType;
        this.totalRewards = builder.totalRewards;
        this.rewardsDistributedLast24h = builder.rewardsDistributedLast24h;
        this.rewardsDistributedLast30d = builder.rewardsDistributedLast30d;
        this.participantCount = builder.participantCount;
        this.averageRewardPerParticipant = builder.averageRewardPerParticipant;
        this.rewardFrequency = builder.rewardFrequency;
        this.nextDistribution = builder.nextDistribution;
        this.distributionPercentage = builder.distributionPercentage;
        this.status = builder.status;
    }

    public String getPoolId() { return poolId; }
    public String getPoolName() { return poolName; }
    public String getDescription() { return description; }
    public String getPoolType() { return poolType; }
    public String getTotalRewards() { return totalRewards; }
    public String getRewardsDistributedLast24h() { return rewardsDistributedLast24h; }
    public String getRewardsDistributedLast30d() { return rewardsDistributedLast30d; }
    public Integer getParticipantCount() { return participantCount; }
    public String getAverageRewardPerParticipant() { return averageRewardPerParticipant; }
    public String getRewardFrequency() { return rewardFrequency; }
    public Instant getNextDistribution() { return nextDistribution; }
    public Double getDistributionPercentage() { return distributionPercentage; }
    public String getStatus() { return status; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String poolId;
        private String poolName;
        private String description;
        private String poolType;
        private String totalRewards;
        private String rewardsDistributedLast24h;
        private String rewardsDistributedLast30d;
        private Integer participantCount;
        private String averageRewardPerParticipant;
        private String rewardFrequency;
        private Instant nextDistribution;
        private Double distributionPercentage;
        private String status;

        public Builder poolId(String poolId) { this.poolId = poolId; return this; }
        public Builder poolName(String poolName) { this.poolName = poolName; return this; }
        public Builder description(String description) { this.description = description; return this; }
        public Builder poolType(String poolType) { this.poolType = poolType; return this; }
        public Builder totalRewards(String totalRewards) { this.totalRewards = totalRewards; return this; }
        public Builder rewardsDistributedLast24h(String rewardsDistributedLast24h) { this.rewardsDistributedLast24h = rewardsDistributedLast24h; return this; }
        public Builder rewardsDistributedLast30d(String rewardsDistributedLast30d) { this.rewardsDistributedLast30d = rewardsDistributedLast30d; return this; }
        public Builder participantCount(Integer participantCount) { this.participantCount = participantCount; return this; }
        public Builder averageRewardPerParticipant(String averageRewardPerParticipant) { this.averageRewardPerParticipant = averageRewardPerParticipant; return this; }
        public Builder rewardFrequency(String rewardFrequency) { this.rewardFrequency = rewardFrequency; return this; }
        public Builder nextDistribution(Instant nextDistribution) { this.nextDistribution = nextDistribution; return this; }
        public Builder distributionPercentage(Double distributionPercentage) { this.distributionPercentage = distributionPercentage; return this; }
        public Builder status(String status) { this.status = status; return this; }

        public RewardDistributionDTO build() { return new RewardDistributionDTO(this); }
    }
}
