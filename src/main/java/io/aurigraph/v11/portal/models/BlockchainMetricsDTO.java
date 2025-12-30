package io.aurigraph.v11.portal.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

/**
 * BlockchainMetricsDTO - Real-time blockchain metrics
 */
public class BlockchainMetricsDTO {
    @JsonProperty("tps")
    private Double tps;
    @JsonProperty("avg_block_time")
    private Double avgBlockTime;
    @JsonProperty("active_nodes")
    private Integer activeNodes;
    @JsonProperty("total_transactions")
    private Long totalTransactions;
    @JsonProperty("consensus")
    private String consensus;
    @JsonProperty("status")
    private String status;
    @JsonProperty("block_height")
    private Long blockHeight;
    @JsonProperty("difficulty")
    private String difficulty;
    @JsonProperty("network_load")
    private Double networkLoad;
    @JsonProperty("finality")
    private Integer finality;
    @JsonProperty("active_validators")
    private Integer activeValidators;
    @JsonProperty("pending_transactions")
    private Integer pendingTransactions;
    @JsonProperty("last_block_time")
    private Instant lastBlockTime;
    @JsonProperty("mem_pool_fill")
    private Double memPoolFill;
    @JsonProperty("network_latency")
    private Integer networkLatency;
    @JsonProperty("error")
    private String error;

    // Constructors
    public BlockchainMetricsDTO() {}

    private BlockchainMetricsDTO(Builder builder) {
        this.tps = builder.tps;
        this.avgBlockTime = builder.avgBlockTime;
        this.activeNodes = builder.activeNodes;
        this.totalTransactions = builder.totalTransactions;
        this.consensus = builder.consensus;
        this.status = builder.status;
        this.blockHeight = builder.blockHeight;
        this.difficulty = builder.difficulty;
        this.networkLoad = builder.networkLoad;
        this.finality = builder.finality;
        this.activeValidators = builder.activeValidators;
        this.pendingTransactions = builder.pendingTransactions;
        this.lastBlockTime = builder.lastBlockTime;
        this.memPoolFill = builder.memPoolFill;
        this.networkLatency = builder.networkLatency;
        this.error = builder.error;
    }

    // Getters and Setters
    public Double getTps() { return tps; }
    public void setTps(Double tps) { this.tps = tps; }

    public Double getAvgBlockTime() { return avgBlockTime; }
    public void setAvgBlockTime(Double avgBlockTime) { this.avgBlockTime = avgBlockTime; }

    public Integer getActiveNodes() { return activeNodes; }
    public void setActiveNodes(Integer activeNodes) { this.activeNodes = activeNodes; }

    public Long getTotalTransactions() { return totalTransactions; }
    public void setTotalTransactions(Long totalTransactions) { this.totalTransactions = totalTransactions; }

    public String getConsensus() { return consensus; }
    public void setConsensus(String consensus) { this.consensus = consensus; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Long getBlockHeight() { return blockHeight; }
    public void setBlockHeight(Long blockHeight) { this.blockHeight = blockHeight; }

    public String getDifficulty() { return difficulty; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }

    public Double getNetworkLoad() { return networkLoad; }
    public void setNetworkLoad(Double networkLoad) { this.networkLoad = networkLoad; }

    public Integer getFinality() { return finality; }
    public void setFinality(Integer finality) { this.finality = finality; }

    public Integer getActiveValidators() { return activeValidators; }
    public void setActiveValidators(Integer activeValidators) { this.activeValidators = activeValidators; }

    public Integer getPendingTransactions() { return pendingTransactions; }
    public void setPendingTransactions(Integer pendingTransactions) { this.pendingTransactions = pendingTransactions; }

    public Instant getLastBlockTime() { return lastBlockTime; }
    public void setLastBlockTime(Instant lastBlockTime) { this.lastBlockTime = lastBlockTime; }

    public Double getMemPoolFill() { return memPoolFill; }
    public void setMemPoolFill(Double memPoolFill) { this.memPoolFill = memPoolFill; }

    public Integer getNetworkLatency() { return networkLatency; }
    public void setNetworkLatency(Integer networkLatency) { this.networkLatency = networkLatency; }

    public String getError() { return error; }
    public void setError(String error) { this.error = error; }

    // Builder Pattern
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Double tps;
        private Double avgBlockTime;
        private Integer activeNodes;
        private Long totalTransactions;
        private String consensus;
        private String status;
        private Long blockHeight;
        private String difficulty;
        private Double networkLoad;
        private Integer finality;
        private Integer activeValidators;
        private Integer pendingTransactions;
        private Instant lastBlockTime;
        private Double memPoolFill;
        private Integer networkLatency;
        private String error;

        public Builder tps(Double tps) { this.tps = tps; return this; }
        public Builder avgBlockTime(Double avgBlockTime) { this.avgBlockTime = avgBlockTime; return this; }
        public Builder activeNodes(Integer activeNodes) { this.activeNodes = activeNodes; return this; }
        public Builder totalTransactions(Long totalTransactions) { this.totalTransactions = totalTransactions; return this; }
        public Builder consensus(String consensus) { this.consensus = consensus; return this; }
        public Builder status(String status) { this.status = status; return this; }
        public Builder blockHeight(Long blockHeight) { this.blockHeight = blockHeight; return this; }
        public Builder difficulty(String difficulty) { this.difficulty = difficulty; return this; }
        public Builder networkLoad(Double networkLoad) { this.networkLoad = networkLoad; return this; }
        public Builder finality(Integer finality) { this.finality = finality; return this; }
        public Builder activeValidators(Integer activeValidators) { this.activeValidators = activeValidators; return this; }
        public Builder pendingTransactions(Integer pendingTransactions) { this.pendingTransactions = pendingTransactions; return this; }
        public Builder lastBlockTime(Instant lastBlockTime) { this.lastBlockTime = lastBlockTime; return this; }
        public Builder memPoolFill(Double memPoolFill) { this.memPoolFill = memPoolFill; return this; }
        public Builder networkLatency(Integer networkLatency) { this.networkLatency = networkLatency; return this; }
        public Builder error(String error) { this.error = error; return this; }

        public BlockchainMetricsDTO build() {
            return new BlockchainMetricsDTO(this);
        }
    }
}
