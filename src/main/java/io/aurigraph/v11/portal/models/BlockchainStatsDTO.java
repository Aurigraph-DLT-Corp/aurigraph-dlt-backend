package io.aurigraph.v11.portal.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.List;

public class BlockchainStatsDTO {
    @JsonProperty("total_blocks")
    private Long totalBlocks;
    @JsonProperty("total_transactions")
    private Long totalTransactions;
    @JsonProperty("total_validators")
    private Integer totalValidators;
    @JsonProperty("active_validators")
    private Integer activeValidators;
    @JsonProperty("total_staked")
    private String totalStaked;
    @JsonProperty("median_block_time")
    private Double medianBlockTime;
    @JsonProperty("min_block_time")
    private Double minBlockTime;
    @JsonProperty("max_block_time")
    private Double maxBlockTime;
    @JsonProperty("avg_transaction_size")
    private Integer avgTransactionSize;
    @JsonProperty("total_contract_deployments")
    private Integer totalContractDeployments;
    @JsonProperty("active_smart_contracts")
    private Integer activeSmartContracts;
    @JsonProperty("total_asset_tokens")
    private Integer totalAssetTokens;
    @JsonProperty("total_r_w_a_tokens")
    private Integer totalRWATokens;
    @JsonProperty("network_uptime")
    private Double networkUptime;
    @JsonProperty("consensus_efficiency")
    private Double consensusEfficiency;
    @JsonProperty("fork_count")
    private Integer forkCount;
    @JsonProperty("error")
    private String error;

    public BlockchainStatsDTO() {}

    private BlockchainStatsDTO(Builder builder) {
        this.totalBlocks = builder.totalBlocks;
        this.totalTransactions = builder.totalTransactions;
        this.totalValidators = builder.totalValidators;
        this.activeValidators = builder.activeValidators;
        this.totalStaked = builder.totalStaked;
        this.medianBlockTime = builder.medianBlockTime;
        this.minBlockTime = builder.minBlockTime;
        this.maxBlockTime = builder.maxBlockTime;
        this.avgTransactionSize = builder.avgTransactionSize;
        this.totalContractDeployments = builder.totalContractDeployments;
        this.activeSmartContracts = builder.activeSmartContracts;
        this.totalAssetTokens = builder.totalAssetTokens;
        this.totalRWATokens = builder.totalRWATokens;
        this.networkUptime = builder.networkUptime;
        this.consensusEfficiency = builder.consensusEfficiency;
        this.forkCount = builder.forkCount;
        this.error = builder.error;
    }

    public Long getTotalBlocks() { return totalBlocks; }
    public Long getTotalTransactions() { return totalTransactions; }
    public Integer getTotalValidators() { return totalValidators; }
    public Integer getActiveValidators() { return activeValidators; }
    public String getTotalStaked() { return totalStaked; }
    public Double getMedianBlockTime() { return medianBlockTime; }
    public Double getMinBlockTime() { return minBlockTime; }
    public Double getMaxBlockTime() { return maxBlockTime; }
    public Integer getAvgTransactionSize() { return avgTransactionSize; }
    public Integer getTotalContractDeployments() { return totalContractDeployments; }
    public Integer getActiveSmartContracts() { return activeSmartContracts; }
    public Integer getTotalAssetTokens() { return totalAssetTokens; }
    public Integer getTotalRWATokens() { return totalRWATokens; }
    public Double getNetworkUptime() { return networkUptime; }
    public Double getConsensusEfficiency() { return consensusEfficiency; }
    public Integer getForkCount() { return forkCount; }
    public String getError() { return error; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Long totalBlocks;
        private Long totalTransactions;
        private Integer totalValidators;
        private Integer activeValidators;
        private String totalStaked;
        private Double medianBlockTime;
        private Double minBlockTime;
        private Double maxBlockTime;
        private Integer avgTransactionSize;
        private Integer totalContractDeployments;
        private Integer activeSmartContracts;
        private Integer totalAssetTokens;
        private Integer totalRWATokens;
        private Double networkUptime;
        private Double consensusEfficiency;
        private Integer forkCount;
        private String error;

        public Builder totalBlocks(Long totalBlocks) { this.totalBlocks = totalBlocks; return this; }
        public Builder totalTransactions(Long totalTransactions) { this.totalTransactions = totalTransactions; return this; }
        public Builder totalValidators(Integer totalValidators) { this.totalValidators = totalValidators; return this; }
        public Builder activeValidators(Integer activeValidators) { this.activeValidators = activeValidators; return this; }
        public Builder totalStaked(String totalStaked) { this.totalStaked = totalStaked; return this; }
        public Builder medianBlockTime(Double medianBlockTime) { this.medianBlockTime = medianBlockTime; return this; }
        public Builder minBlockTime(Double minBlockTime) { this.minBlockTime = minBlockTime; return this; }
        public Builder maxBlockTime(Double maxBlockTime) { this.maxBlockTime = maxBlockTime; return this; }
        public Builder avgTransactionSize(Integer avgTransactionSize) { this.avgTransactionSize = avgTransactionSize; return this; }
        public Builder totalContractDeployments(Integer totalContractDeployments) { this.totalContractDeployments = totalContractDeployments; return this; }
        public Builder activeSmartContracts(Integer activeSmartContracts) { this.activeSmartContracts = activeSmartContracts; return this; }
        public Builder totalAssetTokens(Integer totalAssetTokens) { this.totalAssetTokens = totalAssetTokens; return this; }
        public Builder totalRWATokens(Integer totalRWATokens) { this.totalRWATokens = totalRWATokens; return this; }
        public Builder networkUptime(Double networkUptime) { this.networkUptime = networkUptime; return this; }
        public Builder consensusEfficiency(Double consensusEfficiency) { this.consensusEfficiency = consensusEfficiency; return this; }
        public Builder forkCount(Integer forkCount) { this.forkCount = forkCount; return this; }
        public Builder error(String error) { this.error = error; return this; }

        public BlockchainStatsDTO build() { return new BlockchainStatsDTO(this); }
    }
}
