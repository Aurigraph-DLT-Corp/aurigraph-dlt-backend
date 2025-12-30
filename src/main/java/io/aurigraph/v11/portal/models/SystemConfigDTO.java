package io.aurigraph.v11.portal.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.List;

public class SystemConfigDTO {
    @JsonProperty("max_block_size")
    private Integer maxBlockSize;
    @JsonProperty("max_transaction_size")
    private Integer maxTransactionSize;
    @JsonProperty("block_time")
    private Integer blockTime;
    @JsonProperty("epoch_length")
    private Integer epochLength;
    @JsonProperty("consensus_timeout")
    private Integer consensusTimeout;
    @JsonProperty("max_gas_per_block")
    private Integer maxGasPerBlock;
    @JsonProperty("min_gas_price")
    private String minGasPrice;
    @JsonProperty("max_validators")
    private Integer maxValidators;
    @JsonProperty("min_validator_stake")
    private String minValidatorStake;
    @JsonProperty("commission_min")
    private Double commissionMin;
    @JsonProperty("commission_max")
    private Double commissionMax;
    @JsonProperty("jail_time")
    private Integer jailTime;
    @JsonProperty("unbonding_time")
    private Integer unbondingTime;
    @JsonProperty("slash_percentage")
    private Double slashPercentage;
    @JsonProperty("max_proposers")
    private Integer maxProposers;
    @JsonProperty("block_reward")
    private String blockReward;
    @JsonProperty("validator_reward_percentage")
    private Double validatorRewardPercentage;
    @JsonProperty("treasury_percentage")
    private Double treasuryPercentage;
    @JsonProperty("community_percentage")
    private Double communityPercentage;
    @JsonProperty("governance_voting_period")
    private Integer governanceVotingPeriod;
    @JsonProperty("governance_quorum")
    private Double governanceQuorum;
    @JsonProperty("network_id")
    private String networkId;
    @JsonProperty("chain_id")
    private Integer chainId;
    @JsonProperty("protocol_version")
    private String protocolVersion;
    @JsonProperty("genesis_time")
    private Instant genesisTime;
    @JsonProperty("error")
    private String error;

    public SystemConfigDTO() {}

    private SystemConfigDTO(Builder builder) {
        this.maxBlockSize = builder.maxBlockSize;
        this.maxTransactionSize = builder.maxTransactionSize;
        this.blockTime = builder.blockTime;
        this.epochLength = builder.epochLength;
        this.consensusTimeout = builder.consensusTimeout;
        this.maxGasPerBlock = builder.maxGasPerBlock;
        this.minGasPrice = builder.minGasPrice;
        this.maxValidators = builder.maxValidators;
        this.minValidatorStake = builder.minValidatorStake;
        this.commissionMin = builder.commissionMin;
        this.commissionMax = builder.commissionMax;
        this.jailTime = builder.jailTime;
        this.unbondingTime = builder.unbondingTime;
        this.slashPercentage = builder.slashPercentage;
        this.maxProposers = builder.maxProposers;
        this.blockReward = builder.blockReward;
        this.validatorRewardPercentage = builder.validatorRewardPercentage;
        this.treasuryPercentage = builder.treasuryPercentage;
        this.communityPercentage = builder.communityPercentage;
        this.governanceVotingPeriod = builder.governanceVotingPeriod;
        this.governanceQuorum = builder.governanceQuorum;
        this.networkId = builder.networkId;
        this.chainId = builder.chainId;
        this.protocolVersion = builder.protocolVersion;
        this.genesisTime = builder.genesisTime;
        this.error = builder.error;
    }

    public Integer getMaxBlockSize() { return maxBlockSize; }
    public Integer getMaxTransactionSize() { return maxTransactionSize; }
    public Integer getBlockTime() { return blockTime; }
    public Integer getEpochLength() { return epochLength; }
    public Integer getConsensusTimeout() { return consensusTimeout; }
    public Integer getMaxGasPerBlock() { return maxGasPerBlock; }
    public String getMinGasPrice() { return minGasPrice; }
    public Integer getMaxValidators() { return maxValidators; }
    public String getMinValidatorStake() { return minValidatorStake; }
    public Double getCommissionMin() { return commissionMin; }
    public Double getCommissionMax() { return commissionMax; }
    public Integer getJailTime() { return jailTime; }
    public Integer getUnbondingTime() { return unbondingTime; }
    public Double getSlashPercentage() { return slashPercentage; }
    public Integer getMaxProposers() { return maxProposers; }
    public String getBlockReward() { return blockReward; }
    public Double getValidatorRewardPercentage() { return validatorRewardPercentage; }
    public Double getTreasuryPercentage() { return treasuryPercentage; }
    public Double getCommunityPercentage() { return communityPercentage; }
    public Integer getGovernanceVotingPeriod() { return governanceVotingPeriod; }
    public Double getGovernanceQuorum() { return governanceQuorum; }
    public String getNetworkId() { return networkId; }
    public Integer getChainId() { return chainId; }
    public String getProtocolVersion() { return protocolVersion; }
    public Instant getGenesisTime() { return genesisTime; }
    public String getError() { return error; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Integer maxBlockSize;
        private Integer maxTransactionSize;
        private Integer blockTime;
        private Integer epochLength;
        private Integer consensusTimeout;
        private Integer maxGasPerBlock;
        private String minGasPrice;
        private Integer maxValidators;
        private String minValidatorStake;
        private Double commissionMin;
        private Double commissionMax;
        private Integer jailTime;
        private Integer unbondingTime;
        private Double slashPercentage;
        private Integer maxProposers;
        private String blockReward;
        private Double validatorRewardPercentage;
        private Double treasuryPercentage;
        private Double communityPercentage;
        private Integer governanceVotingPeriod;
        private Double governanceQuorum;
        private String networkId;
        private Integer chainId;
        private String protocolVersion;
        private Instant genesisTime;
        private String error;

        public Builder maxBlockSize(Integer maxBlockSize) { this.maxBlockSize = maxBlockSize; return this; }
        public Builder maxTransactionSize(Integer maxTransactionSize) { this.maxTransactionSize = maxTransactionSize; return this; }
        public Builder blockTime(Integer blockTime) { this.blockTime = blockTime; return this; }
        public Builder epochLength(Integer epochLength) { this.epochLength = epochLength; return this; }
        public Builder consensusTimeout(Integer consensusTimeout) { this.consensusTimeout = consensusTimeout; return this; }
        public Builder maxGasPerBlock(Integer maxGasPerBlock) { this.maxGasPerBlock = maxGasPerBlock; return this; }
        public Builder minGasPrice(String minGasPrice) { this.minGasPrice = minGasPrice; return this; }
        public Builder maxValidators(Integer maxValidators) { this.maxValidators = maxValidators; return this; }
        public Builder minValidatorStake(String minValidatorStake) { this.minValidatorStake = minValidatorStake; return this; }
        public Builder commissionMin(Double commissionMin) { this.commissionMin = commissionMin; return this; }
        public Builder commissionMax(Double commissionMax) { this.commissionMax = commissionMax; return this; }
        public Builder jailTime(Integer jailTime) { this.jailTime = jailTime; return this; }
        public Builder unbondingTime(Integer unbondingTime) { this.unbondingTime = unbondingTime; return this; }
        public Builder slashPercentage(Double slashPercentage) { this.slashPercentage = slashPercentage; return this; }
        public Builder maxProposers(Integer maxProposers) { this.maxProposers = maxProposers; return this; }
        public Builder blockReward(String blockReward) { this.blockReward = blockReward; return this; }
        public Builder validatorRewardPercentage(Double validatorRewardPercentage) { this.validatorRewardPercentage = validatorRewardPercentage; return this; }
        public Builder treasuryPercentage(Double treasuryPercentage) { this.treasuryPercentage = treasuryPercentage; return this; }
        public Builder communityPercentage(Double communityPercentage) { this.communityPercentage = communityPercentage; return this; }
        public Builder governanceVotingPeriod(Integer governanceVotingPeriod) { this.governanceVotingPeriod = governanceVotingPeriod; return this; }
        public Builder governanceQuorum(Double governanceQuorum) { this.governanceQuorum = governanceQuorum; return this; }
        public Builder networkId(String networkId) { this.networkId = networkId; return this; }
        public Builder chainId(Integer chainId) { this.chainId = chainId; return this; }
        public Builder protocolVersion(String protocolVersion) { this.protocolVersion = protocolVersion; return this; }
        public Builder genesisTime(Instant genesisTime) { this.genesisTime = genesisTime; return this; }
        public Builder error(String error) { this.error = error; return this; }

        public SystemConfigDTO build() { return new SystemConfigDTO(this); }
    }
}
