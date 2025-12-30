package io.aurigraph.v11.portal.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.List;

public class NetworkHealthDTO {
    @JsonProperty("status")
    private String status;
    @JsonProperty("timestamp")
    private Instant timestamp;
    @JsonProperty("uptime")
    private Double uptime;
    @JsonProperty("total_nodes")
    private Integer totalNodes;
    @JsonProperty("active_nodes")
    private Integer activeNodes;
    @JsonProperty("inactive_nodes")
    private Integer inactiveNodes;
    @JsonProperty("validator_nodes")
    private Integer validatorNodes;
    @JsonProperty("full_nodes")
    private Integer fullNodes;
    @JsonProperty("light_nodes")
    private Integer lightNodes;
    @JsonProperty("archive_nodes")
    private Integer archiveNodes;
    @JsonProperty("average_node_latency")
    private Integer averageNodeLatency;
    @JsonProperty("max_node_latency")
    private Integer maxNodeLatency;
    @JsonProperty("min_node_latency")
    private Integer minNodeLatency;
    @JsonProperty("network_partitions")
    private Integer networkPartitions;
    @JsonProperty("forks")
    private Integer forks;
    @JsonProperty("consensus_health")
    private String consensusHealth;
    @JsonProperty("peers_per_node")
    private Integer peersPerNode;
    @JsonProperty("inbound_connections")
    private Integer inboundConnections;
    @JsonProperty("outbound_connections")
    private Integer outboundConnections;
    @JsonProperty("total_bandwidth")
    private String totalBandwidth;
    @JsonProperty("average_block_propagation")
    private Double averageBlockPropagation;
    @JsonProperty("max_block_propagation")
    private Double maxBlockPropagation;
    @JsonProperty("block_orphan_rate")
    private Double blockOrphanRate;
    @JsonProperty("error")
    private String error;

    public NetworkHealthDTO() {}

    private NetworkHealthDTO(Builder builder) {
        this.status = builder.status;
        this.timestamp = builder.timestamp;
        this.uptime = builder.uptime;
        this.totalNodes = builder.totalNodes;
        this.activeNodes = builder.activeNodes;
        this.inactiveNodes = builder.inactiveNodes;
        this.validatorNodes = builder.validatorNodes;
        this.fullNodes = builder.fullNodes;
        this.lightNodes = builder.lightNodes;
        this.archiveNodes = builder.archiveNodes;
        this.averageNodeLatency = builder.averageNodeLatency;
        this.maxNodeLatency = builder.maxNodeLatency;
        this.minNodeLatency = builder.minNodeLatency;
        this.networkPartitions = builder.networkPartitions;
        this.forks = builder.forks;
        this.consensusHealth = builder.consensusHealth;
        this.peersPerNode = builder.peersPerNode;
        this.inboundConnections = builder.inboundConnections;
        this.outboundConnections = builder.outboundConnections;
        this.totalBandwidth = builder.totalBandwidth;
        this.averageBlockPropagation = builder.averageBlockPropagation;
        this.maxBlockPropagation = builder.maxBlockPropagation;
        this.blockOrphanRate = builder.blockOrphanRate;
        this.error = builder.error;
    }

    public String getStatus() { return status; }
    public Instant getTimestamp() { return timestamp; }
    public Double getUptime() { return uptime; }
    public Integer getTotalNodes() { return totalNodes; }
    public Integer getActiveNodes() { return activeNodes; }
    public Integer getInactiveNodes() { return inactiveNodes; }
    public Integer getValidatorNodes() { return validatorNodes; }
    public Integer getFullNodes() { return fullNodes; }
    public Integer getLightNodes() { return lightNodes; }
    public Integer getArchiveNodes() { return archiveNodes; }
    public Integer getAverageNodeLatency() { return averageNodeLatency; }
    public Integer getMaxNodeLatency() { return maxNodeLatency; }
    public Integer getMinNodeLatency() { return minNodeLatency; }
    public Integer getNetworkPartitions() { return networkPartitions; }
    public Integer getForks() { return forks; }
    public String getConsensusHealth() { return consensusHealth; }
    public Integer getPeersPerNode() { return peersPerNode; }
    public Integer getInboundConnections() { return inboundConnections; }
    public Integer getOutboundConnections() { return outboundConnections; }
    public String getTotalBandwidth() { return totalBandwidth; }
    public Double getAverageBlockPropagation() { return averageBlockPropagation; }
    public Double getMaxBlockPropagation() { return maxBlockPropagation; }
    public Double getBlockOrphanRate() { return blockOrphanRate; }
    public String getError() { return error; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String status;
        private Instant timestamp;
        private Double uptime;
        private Integer totalNodes;
        private Integer activeNodes;
        private Integer inactiveNodes;
        private Integer validatorNodes;
        private Integer fullNodes;
        private Integer lightNodes;
        private Integer archiveNodes;
        private Integer averageNodeLatency;
        private Integer maxNodeLatency;
        private Integer minNodeLatency;
        private Integer networkPartitions;
        private Integer forks;
        private String consensusHealth;
        private Integer peersPerNode;
        private Integer inboundConnections;
        private Integer outboundConnections;
        private String totalBandwidth;
        private Double averageBlockPropagation;
        private Double maxBlockPropagation;
        private Double blockOrphanRate;
        private String error;

        public Builder status(String status) { this.status = status; return this; }
        public Builder timestamp(Instant timestamp) { this.timestamp = timestamp; return this; }
        public Builder uptime(Double uptime) { this.uptime = uptime; return this; }
        public Builder totalNodes(Integer totalNodes) { this.totalNodes = totalNodes; return this; }
        public Builder activeNodes(Integer activeNodes) { this.activeNodes = activeNodes; return this; }
        public Builder inactiveNodes(Integer inactiveNodes) { this.inactiveNodes = inactiveNodes; return this; }
        public Builder validatorNodes(Integer validatorNodes) { this.validatorNodes = validatorNodes; return this; }
        public Builder fullNodes(Integer fullNodes) { this.fullNodes = fullNodes; return this; }
        public Builder lightNodes(Integer lightNodes) { this.lightNodes = lightNodes; return this; }
        public Builder archiveNodes(Integer archiveNodes) { this.archiveNodes = archiveNodes; return this; }
        public Builder averageNodeLatency(Integer averageNodeLatency) { this.averageNodeLatency = averageNodeLatency; return this; }
        public Builder maxNodeLatency(Integer maxNodeLatency) { this.maxNodeLatency = maxNodeLatency; return this; }
        public Builder minNodeLatency(Integer minNodeLatency) { this.minNodeLatency = minNodeLatency; return this; }
        public Builder networkPartitions(Integer networkPartitions) { this.networkPartitions = networkPartitions; return this; }
        public Builder forks(Integer forks) { this.forks = forks; return this; }
        public Builder consensusHealth(String consensusHealth) { this.consensusHealth = consensusHealth; return this; }
        public Builder peersPerNode(Integer peersPerNode) { this.peersPerNode = peersPerNode; return this; }
        public Builder inboundConnections(Integer inboundConnections) { this.inboundConnections = inboundConnections; return this; }
        public Builder outboundConnections(Integer outboundConnections) { this.outboundConnections = outboundConnections; return this; }
        public Builder totalBandwidth(String totalBandwidth) { this.totalBandwidth = totalBandwidth; return this; }
        public Builder averageBlockPropagation(Double averageBlockPropagation) { this.averageBlockPropagation = averageBlockPropagation; return this; }
        public Builder maxBlockPropagation(Double maxBlockPropagation) { this.maxBlockPropagation = maxBlockPropagation; return this; }
        public Builder blockOrphanRate(Double blockOrphanRate) { this.blockOrphanRate = blockOrphanRate; return this; }
        public Builder error(String error) { this.error = error; return this; }

        public NetworkHealthDTO build() { return new NetworkHealthDTO(this); }
    }
}
