package io.aurigraph.v11.portal.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

public class HealthStatusDTO {
    @JsonProperty("status")
    private String status;
    @JsonProperty("timestamp")
    private Instant timestamp;
    @JsonProperty("chain_height")
    private Long chainHeight;
    @JsonProperty("active_validators")
    private Integer activeValidators;
    @JsonProperty("latest_block_time")
    private Instant latestBlockTime;
    @JsonProperty("last_check_time")
    private Instant lastCheckTime;
    @JsonProperty("consensus_round")
    private Long consensusRound;
    @JsonProperty("finalization_time")
    private Long finalizationTime;
    @JsonProperty("network_health")
    private String networkHealth;
    @JsonProperty("sync_status")
    private String syncStatus;
    @JsonProperty("peers_connected")
    private Integer peersConnected;
    @JsonProperty("mem_pool_size")
    private Integer memPoolSize;
    @JsonProperty("error")
    private String error;

    public HealthStatusDTO() {}

    private HealthStatusDTO(Builder builder) {
        this.status = builder.status;
        this.timestamp = builder.timestamp;
        this.chainHeight = builder.chainHeight;
        this.activeValidators = builder.activeValidators;
        this.latestBlockTime = builder.latestBlockTime;
        this.lastCheckTime = builder.lastCheckTime;
        this.consensusRound = builder.consensusRound;
        this.finalizationTime = builder.finalizationTime;
        this.networkHealth = builder.networkHealth;
        this.syncStatus = builder.syncStatus;
        this.peersConnected = builder.peersConnected;
        this.memPoolSize = builder.memPoolSize;
        this.error = builder.error;
    }

    // Getters
    public String getStatus() { return status; }
    public Instant getTimestamp() { return timestamp; }
    public Long getChainHeight() { return chainHeight; }
    public Integer getActiveValidators() { return activeValidators; }
    public Instant getLatestBlockTime() { return latestBlockTime; }
    public Instant getLastCheckTime() { return lastCheckTime; }
    public Long getConsensusRound() { return consensusRound; }
    public Long getFinalizationTime() { return finalizationTime; }
    public String getNetworkHealth() { return networkHealth; }
    public String getSyncStatus() { return syncStatus; }
    public Integer getPeersConnected() { return peersConnected; }
    public Integer getMemPoolSize() { return memPoolSize; }
    public String getError() { return error; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String status;
        private Instant timestamp;
        private Long chainHeight;
        private Integer activeValidators;
        private Instant latestBlockTime;
        private Instant lastCheckTime;
        private Long consensusRound;
        private Long finalizationTime;
        private String networkHealth;
        private String syncStatus;
        private Integer peersConnected;
        private Integer memPoolSize;
        private String error;

        public Builder status(String status) { this.status = status; return this; }
        public Builder timestamp(Instant timestamp) { this.timestamp = timestamp; return this; }
        public Builder chainHeight(Long chainHeight) { this.chainHeight = chainHeight; return this; }
        public Builder activeValidators(Integer activeValidators) { this.activeValidators = activeValidators; return this; }
        public Builder latestBlockTime(Instant latestBlockTime) { this.latestBlockTime = latestBlockTime; return this; }
        public Builder lastCheckTime(Instant lastCheckTime) { this.lastCheckTime = lastCheckTime; return this; }
        public Builder consensusRound(Long consensusRound) { this.consensusRound = consensusRound; return this; }
        public Builder finalizationTime(Long finalizationTime) { this.finalizationTime = finalizationTime; return this; }
        public Builder networkHealth(String networkHealth) { this.networkHealth = networkHealth; return this; }
        public Builder syncStatus(String syncStatus) { this.syncStatus = syncStatus; return this; }
        public Builder peersConnected(Integer peersConnected) { this.peersConnected = peersConnected; return this; }
        public Builder memPoolSize(Integer memPoolSize) { this.memPoolSize = memPoolSize; return this; }
        public Builder error(String error) { this.error = error; return this; }

        public HealthStatusDTO build() { return new HealthStatusDTO(this); }
    }
}
