package io.aurigraph.v11.portal.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.List;

public class ValidatorDetailDTO {
    @JsonProperty("validator_id")
    private String validatorId;
    @JsonProperty("address")
    private String address;
    @JsonProperty("status")
    private String status;
    @JsonProperty("stake")
    private String stake;
    @JsonProperty("commission_rate")
    private Double commissionRate;
    @JsonProperty("uptime")
    private Double uptime;
    @JsonProperty("block_proposals")
    private Integer blockProposals;
    @JsonProperty("missed_blocks")
    private Integer missedBlocks;
    @JsonProperty("consensus_participation")
    private Double consensusParticipation;
    @JsonProperty("joined_at")
    private Instant joinedAt;
    @JsonProperty("last_proposal_time")
    private Instant lastProposalTime;
    @JsonProperty("total_rewards")
    private String totalRewards;
    @JsonProperty("delegators")
    private java.util.List<String> delegators;
    @JsonProperty("delegation_count")
    private Integer delegationCount;
    @JsonProperty("recent_block_proposals")
    private java.util.List<Long> recentBlockProposals;
    @JsonProperty("average_block_time")
    private Double averageBlockTime;
    @JsonProperty("jailed_until")
    private Instant jailedUntil;
    @JsonProperty("error")
    private String error;

    public ValidatorDetailDTO() {}

    private ValidatorDetailDTO(Builder builder) {
        this.validatorId = builder.validatorId;
        this.address = builder.address;
        this.status = builder.status;
        this.stake = builder.stake;
        this.commissionRate = builder.commissionRate;
        this.uptime = builder.uptime;
        this.blockProposals = builder.blockProposals;
        this.missedBlocks = builder.missedBlocks;
        this.consensusParticipation = builder.consensusParticipation;
        this.joinedAt = builder.joinedAt;
        this.lastProposalTime = builder.lastProposalTime;
        this.totalRewards = builder.totalRewards;
        this.delegators = builder.delegators;
        this.delegationCount = builder.delegationCount;
        this.recentBlockProposals = builder.recentBlockProposals;
        this.averageBlockTime = builder.averageBlockTime;
        this.jailedUntil = builder.jailedUntil;
        this.error = builder.error;
    }

    public String getValidatorId() { return validatorId; }
    public String getAddress() { return address; }
    public String getStatus() { return status; }
    public String getStake() { return stake; }
    public Double getCommissionRate() { return commissionRate; }
    public Double getUptime() { return uptime; }
    public Integer getBlockProposals() { return blockProposals; }
    public Integer getMissedBlocks() { return missedBlocks; }
    public Double getConsensusParticipation() { return consensusParticipation; }
    public Instant getJoinedAt() { return joinedAt; }
    public Instant getLastProposalTime() { return lastProposalTime; }
    public String getTotalRewards() { return totalRewards; }
    public java.util.List<String> getDelegators() { return delegators; }
    public Integer getDelegationCount() { return delegationCount; }
    public java.util.List<Long> getRecentBlockProposals() { return recentBlockProposals; }
    public Double getAverageBlockTime() { return averageBlockTime; }
    public Instant getJailedUntil() { return jailedUntil; }
    public String getError() { return error; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String validatorId;
        private String address;
        private String status;
        private String stake;
        private Double commissionRate;
        private Double uptime;
        private Integer blockProposals;
        private Integer missedBlocks;
        private Double consensusParticipation;
        private Instant joinedAt;
        private Instant lastProposalTime;
        private String totalRewards;
        private java.util.List<String> delegators;
        private Integer delegationCount;
        private java.util.List<Long> recentBlockProposals;
        private Double averageBlockTime;
        private Instant jailedUntil;
        private String error;

        public Builder validatorId(String validatorId) { this.validatorId = validatorId; return this; }
        public Builder address(String address) { this.address = address; return this; }
        public Builder status(String status) { this.status = status; return this; }
        public Builder stake(String stake) { this.stake = stake; return this; }
        public Builder commissionRate(Double commissionRate) { this.commissionRate = commissionRate; return this; }
        public Builder uptime(Double uptime) { this.uptime = uptime; return this; }
        public Builder blockProposals(Integer blockProposals) { this.blockProposals = blockProposals; return this; }
        public Builder missedBlocks(Integer missedBlocks) { this.missedBlocks = missedBlocks; return this; }
        public Builder consensusParticipation(Double consensusParticipation) { this.consensusParticipation = consensusParticipation; return this; }
        public Builder joinedAt(Instant joinedAt) { this.joinedAt = joinedAt; return this; }
        public Builder lastProposalTime(Instant lastProposalTime) { this.lastProposalTime = lastProposalTime; return this; }
        public Builder totalRewards(String totalRewards) { this.totalRewards = totalRewards; return this; }
        public Builder delegators(java.util.List<String> delegators) { this.delegators = delegators; return this; }
        public Builder delegationCount(Integer delegationCount) { this.delegationCount = delegationCount; return this; }
        public Builder recentBlockProposals(java.util.List<Long> recentBlockProposals) { this.recentBlockProposals = recentBlockProposals; return this; }
        public Builder averageBlockTime(Double averageBlockTime) { this.averageBlockTime = averageBlockTime; return this; }
        public Builder jailedUntil(Instant jailedUntil) { this.jailedUntil = jailedUntil; return this; }
        public Builder error(String error) { this.error = error; return this; }

        public ValidatorDetailDTO build() { return new ValidatorDetailDTO(this); }
    }
}
