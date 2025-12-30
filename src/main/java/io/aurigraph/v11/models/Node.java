package io.aurigraph.v11.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Node Model for Aurigraph V11 - LevelDB Compatible
 *
 * Represents a network node in the Aurigraph V11 blockchain network.
 * Nodes can be validators, full nodes, or light clients.
 *
 * LevelDB Storage: Uses address as primary key
 * JSON Serializable: All fields stored as JSON in LevelDB
 *
 * @version 4.0.0 (LevelDB Migration - Oct 8, 2025)
 * @author Aurigraph V11 Development Team
 * @since Sprint 9
 */
public class Node {

    @JsonProperty("address")
    private String address;

    @JsonProperty("nodeType")
    private NodeType nodeType;

    @JsonProperty("status")
    private NodeStatus status = NodeStatus.OFFLINE;

    @JsonProperty("isValidator")
    private Boolean isValidator = false;

    @JsonProperty("validatorRank")
    private Integer validatorRank;

    @JsonProperty("stakeAmount")
    private Long stakeAmount = 0L;

    @JsonProperty("hostAddress")
    private String hostAddress;

    @JsonProperty("p2pPort")
    private Integer p2pPort = 30303;

    @JsonProperty("rpcPort")
    private Integer rpcPort = 8545;

    @JsonProperty("grpcPort")
    private Integer grpcPort = 9000;

    @JsonProperty("publicKey")
    private String publicKey;

    @JsonProperty("nodeVersion")
    private String nodeVersion = "12.0.0";

    @JsonProperty("consensusAlgorithm")
    private String consensusAlgorithm = "HyperRAFT++";

    @JsonProperty("blocksValidated")
    private Long blocksValidated = 0L;

    @JsonProperty("blocksProduced")
    private Long blocksProduced = 0L;

    @JsonProperty("transactionsProcessed")
    private Long transactionsProcessed = 0L;

    @JsonProperty("uptimeSeconds")
    private Long uptimeSeconds = 0L;

    @JsonProperty("lastHeartbeat")
    private Instant lastHeartbeat;

    @JsonProperty("lastBlockHeight")
    private Long lastBlockHeight = 0L;

    @JsonProperty("peerCount")
    private Integer peerCount = 0;

    @JsonProperty("region")
    private String region;

    @JsonProperty("countryCode")
    private String countryCode;

    @JsonProperty("latitude")
    private Double latitude;

    @JsonProperty("longitude")
    private Double longitude;

    @JsonProperty("cpuUsagePercent")
    private Double cpuUsagePercent = 0.0;

    @JsonProperty("memoryUsagePercent")
    private Double memoryUsagePercent = 0.0;

    @JsonProperty("diskUsagePercent")
    private Double diskUsagePercent = 0.0;

    @JsonProperty("networkInMbps")
    private Double networkInMbps = 0.0;

    @JsonProperty("networkOutMbps")
    private Double networkOutMbps = 0.0;

    @JsonProperty("metadata")
    private Map<String, String> metadata = new HashMap<>();

    @JsonProperty("registeredAt")
    private Instant registeredAt;

    @JsonProperty("updatedAt")
    private Instant updatedAt;

    // ==================== CONSTRUCTORS ====================

    public Node() {
        this.status = NodeStatus.OFFLINE;
        this.isValidator = false;
        this.stakeAmount = 0L;
        this.p2pPort = 30303;
        this.rpcPort = 8545;
        this.grpcPort = 9000;
        this.nodeVersion = "12.0.0";
        this.consensusAlgorithm = "HyperRAFT++";
        this.blocksValidated = 0L;
        this.blocksProduced = 0L;
        this.transactionsProcessed = 0L;
        this.uptimeSeconds = 0L;
        this.lastBlockHeight = 0L;
        this.peerCount = 0;
        this.cpuUsagePercent = 0.0;
        this.memoryUsagePercent = 0.0;
        this.diskUsagePercent = 0.0;
        this.networkInMbps = 0.0;
        this.networkOutMbps = 0.0;
        this.metadata = new HashMap<>();
    }

    public Node(String address, NodeType nodeType, String hostAddress) {
        this();
        this.address = address;
        this.nodeType = nodeType;
        this.hostAddress = hostAddress;
    }

    // ==================== LIFECYCLE METHODS ====================

    /**
     * Ensure registeredAt is set (call before first persist)
     */
    public void ensureRegisteredAt() {
        if (registeredAt == null) {
            registeredAt = Instant.now();
        }
        if (updatedAt == null) {
            updatedAt = Instant.now();
        }
        if (lastHeartbeat == null) {
            lastHeartbeat = Instant.now();
        }
    }

    /**
     * Update timestamp (call before each persist)
     */
    public void updateTimestamp() {
        updatedAt = Instant.now();
    }

    // ==================== BUSINESS LOGIC METHODS ====================

    /**
     * Check if node is online
     */
    public boolean isOnline() {
        return status == NodeStatus.ONLINE || status == NodeStatus.SYNCING || status == NodeStatus.VALIDATING;
    }

    /**
     * Check if node is healthy
     */
    public boolean isHealthy() {
        if (!isOnline()) {
            return false;
        }

        // Check if last heartbeat is recent (within 1 minute)
        if (lastHeartbeat != null) {
            long secondsSinceHeartbeat = Instant.now().getEpochSecond() - lastHeartbeat.getEpochSecond();
            if (secondsSinceHeartbeat > 60) {
                return false;
            }
        }

        // Check resource usage
        return cpuUsagePercent < 90 && memoryUsagePercent < 90 && diskUsagePercent < 90;
    }

    /**
     * Update heartbeat
     */
    public void updateHeartbeat() {
        this.lastHeartbeat = Instant.now();
        this.updatedAt = Instant.now();
    }

    /**
     * Increment blocks validated
     */
    public void incrementBlocksValidated() {
        this.blocksValidated++;
        this.updatedAt = Instant.now();
    }

    /**
     * Increment blocks produced
     */
    public void incrementBlocksProduced() {
        this.blocksProduced++;
        this.updatedAt = Instant.now();
    }

    // ==================== GETTERS AND SETTERS ====================

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public NodeType getNodeType() { return nodeType; }
    public void setNodeType(NodeType nodeType) { this.nodeType = nodeType; }

    public NodeStatus getStatus() { return status; }
    public void setStatus(NodeStatus status) { this.status = status; }

    public Boolean getIsValidator() { return isValidator; }
    public void setIsValidator(Boolean isValidator) { this.isValidator = isValidator; }

    public Integer getValidatorRank() { return validatorRank; }
    public void setValidatorRank(Integer validatorRank) { this.validatorRank = validatorRank; }

    public Long getStakeAmount() { return stakeAmount; }
    public void setStakeAmount(Long stakeAmount) { this.stakeAmount = stakeAmount; }

    public String getHostAddress() { return hostAddress; }
    public void setHostAddress(String hostAddress) { this.hostAddress = hostAddress; }

    public Integer getP2pPort() { return p2pPort; }
    public void setP2pPort(Integer p2pPort) { this.p2pPort = p2pPort; }

    public Integer getRpcPort() { return rpcPort; }
    public void setRpcPort(Integer rpcPort) { this.rpcPort = rpcPort; }

    public Integer getGrpcPort() { return grpcPort; }
    public void setGrpcPort(Integer grpcPort) { this.grpcPort = grpcPort; }

    public String getPublicKey() { return publicKey; }
    public void setPublicKey(String publicKey) { this.publicKey = publicKey; }

    public String getNodeVersion() { return nodeVersion; }
    public void setNodeVersion(String nodeVersion) { this.nodeVersion = nodeVersion; }

    public String getConsensusAlgorithm() { return consensusAlgorithm; }
    public void setConsensusAlgorithm(String consensusAlgorithm) { this.consensusAlgorithm = consensusAlgorithm; }

    public Long getBlocksValidated() { return blocksValidated; }
    public void setBlocksValidated(Long blocksValidated) { this.blocksValidated = blocksValidated; }

    public Long getBlocksProduced() { return blocksProduced; }
    public void setBlocksProduced(Long blocksProduced) { this.blocksProduced = blocksProduced; }

    public Long getTransactionsProcessed() { return transactionsProcessed; }
    public void setTransactionsProcessed(Long transactionsProcessed) { this.transactionsProcessed = transactionsProcessed; }

    public Long getUptimeSeconds() { return uptimeSeconds; }
    public void setUptimeSeconds(Long uptimeSeconds) { this.uptimeSeconds = uptimeSeconds; }

    public Instant getLastHeartbeat() { return lastHeartbeat; }
    public void setLastHeartbeat(Instant lastHeartbeat) { this.lastHeartbeat = lastHeartbeat; }

    public Long getLastBlockHeight() { return lastBlockHeight; }
    public void setLastBlockHeight(Long lastBlockHeight) { this.lastBlockHeight = lastBlockHeight; }

    public Integer getPeerCount() { return peerCount; }
    public void setPeerCount(Integer peerCount) { this.peerCount = peerCount; }

    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }

    public String getCountryCode() { return countryCode; }
    public void setCountryCode(String countryCode) { this.countryCode = countryCode; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public Double getCpuUsagePercent() { return cpuUsagePercent; }
    public void setCpuUsagePercent(Double cpuUsagePercent) { this.cpuUsagePercent = cpuUsagePercent; }

    public Double getMemoryUsagePercent() { return memoryUsagePercent; }
    public void setMemoryUsagePercent(Double memoryUsagePercent) { this.memoryUsagePercent = memoryUsagePercent; }

    public Double getDiskUsagePercent() { return diskUsagePercent; }
    public void setDiskUsagePercent(Double diskUsagePercent) { this.diskUsagePercent = diskUsagePercent; }

    public Double getNetworkInMbps() { return networkInMbps; }
    public void setNetworkInMbps(Double networkInMbps) { this.networkInMbps = networkInMbps; }

    public Double getNetworkOutMbps() { return networkOutMbps; }
    public void setNetworkOutMbps(Double networkOutMbps) { this.networkOutMbps = networkOutMbps; }

    public Map<String, String> getMetadata() { return metadata; }
    public void setMetadata(Map<String, String> metadata) { this.metadata = metadata; }

    public Instant getRegisteredAt() { return registeredAt; }
    public void setRegisteredAt(Instant registeredAt) { this.registeredAt = registeredAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    @Override
    public String toString() {
        return String.format("Node{address='%s', nodeType=%s, status=%s, isValidator=%s, " +
                        "hostAddress='%s', lastHeartbeat=%s}",
                address, nodeType, status, isValidator, hostAddress, lastHeartbeat);
    }
}
