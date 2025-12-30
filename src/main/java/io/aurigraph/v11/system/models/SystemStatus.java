package io.aurigraph.v11.system.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * System Status Model for Aurigraph V11 - LevelDB Compatible
 *
 * Tracks comprehensive system health and performance metrics.
 * Monitors consensus, resources, network, and service availability.
 *
 * LevelDB Storage: Uses composite key "nodeId:timestamp" or nodeId as primary key
 * JSON Serializable: All fields stored as JSON in LevelDB
 *
 * @version 4.0.0 (LevelDB Migration - Oct 8, 2025)
 * @author Aurigraph V11 Development Team
 */
public class SystemStatus {

    @JsonProperty("nodeId")
    private String nodeId;

    @JsonProperty("healthStatus")
    private HealthStatus healthStatus;

    // Timestamps
    @JsonProperty("timestamp")
    private Instant timestamp;

    @JsonProperty("uptime")
    private Long uptime = 0L; // seconds

    @JsonProperty("startedAt")
    private Instant startedAt;

    // Performance Metrics
    @JsonProperty("tps")
    private BigDecimal tps; // Transactions per second

    @JsonProperty("avgLatency")
    private BigDecimal avgLatency; // milliseconds

    @JsonProperty("peakTps")
    private BigDecimal peakTps;

    @JsonProperty("totalTransactions")
    private Long totalTransactions = 0L;

    @JsonProperty("failedTransactions")
    private Long failedTransactions = 0L;

    // Resource Usage
    @JsonProperty("cpuUsage")
    private BigDecimal cpuUsage; // percentage

    @JsonProperty("memoryUsed")
    private Long memoryUsed = 0L; // bytes

    @JsonProperty("memoryTotal")
    private Long memoryTotal = 0L; // bytes

    @JsonProperty("diskUsed")
    private Long diskUsed = 0L; // bytes

    @JsonProperty("diskTotal")
    private Long diskTotal = 0L; // bytes

    @JsonProperty("threadCount")
    private Integer threadCount = 0;

    @JsonProperty("activeThreadCount")
    private Integer activeThreadCount = 0;

    // Consensus Metrics
    @JsonProperty("consensusStatus")
    private ConsensusStatus consensusStatus;

    @JsonProperty("blockHeight")
    private Long blockHeight = 0L;

    @JsonProperty("blockTime")
    private BigDecimal blockTime; // milliseconds

    @JsonProperty("isLeader")
    private Boolean isLeader = false;

    @JsonProperty("peerCount")
    private Integer peerCount = 0;

    @JsonProperty("activePeers")
    private Integer activePeers = 0;

    // Network Metrics
    @JsonProperty("networkBytesIn")
    private Long networkBytesIn = 0L;

    @JsonProperty("networkBytesOut")
    private Long networkBytesOut = 0L;

    @JsonProperty("networkErrors")
    private Long networkErrors = 0L;

    @JsonProperty("connectionCount")
    private Integer connectionCount = 0;

    // Service Availability
    @JsonProperty("apiAvailable")
    private Boolean apiAvailable = true;

    @JsonProperty("grpcAvailable")
    private Boolean grpcAvailable = true;

    @JsonProperty("databaseAvailable")
    private Boolean databaseAvailable = true;

    @JsonProperty("cacheAvailable")
    private Boolean cacheAvailable = true;

    // Error Tracking
    @JsonProperty("errorCount")
    private Long errorCount = 0L;

    @JsonProperty("warningCount")
    private Long warningCount = 0L;

    @JsonProperty("lastError")
    private String lastError;

    @JsonProperty("lastErrorAt")
    private Instant lastErrorAt;

    // Metadata
    @JsonProperty("version")
    private String version;

    @JsonProperty("environment")
    private String environment;

    @JsonProperty("region")
    private String region;

    @JsonProperty("metadata")
    private String metadata;

    // ==================== CONSTRUCTORS ====================

    public SystemStatus() {
        this.timestamp = Instant.now();
        this.startedAt = Instant.now();
        this.healthStatus = HealthStatus.HEALTHY;
        this.consensusStatus = ConsensusStatus.SYNCED;
        this.totalTransactions = 0L;
        this.failedTransactions = 0L;
        this.errorCount = 0L;
        this.warningCount = 0L;
        this.apiAvailable = true;
        this.grpcAvailable = true;
        this.databaseAvailable = true;
        this.cacheAvailable = true;
        this.isLeader = false;
        this.uptime = 0L;
        this.memoryUsed = 0L;
        this.memoryTotal = 0L;
        this.diskUsed = 0L;
        this.diskTotal = 0L;
        this.threadCount = 0;
        this.activeThreadCount = 0;
        this.blockHeight = 0L;
        this.peerCount = 0;
        this.activePeers = 0;
        this.networkBytesIn = 0L;
        this.networkBytesOut = 0L;
        this.networkErrors = 0L;
        this.connectionCount = 0;
    }

    public SystemStatus(String nodeId) {
        this();
        this.nodeId = nodeId;
    }

    // ==================== LIFECYCLE METHODS ====================

    /**
     * Ensure timestamp is set (call before first persist)
     */
    public void ensureTimestamp() {
        if (timestamp == null) {
            timestamp = Instant.now();
        }
        updateUptime();
    }

    /**
     * Update uptime in seconds
     */
    public void updateUptime() {
        if (startedAt != null) {
            this.uptime = Instant.now().getEpochSecond() - startedAt.getEpochSecond();
        }
    }

    /**
     * Calculate memory usage percentage
     */
    public BigDecimal getMemoryUsagePercent() {
        if (memoryTotal == 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(memoryUsed)
                .divide(BigDecimal.valueOf(memoryTotal), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }

    /**
     * Calculate disk usage percentage
     */
    public BigDecimal getDiskUsagePercent() {
        if (diskTotal == 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(diskUsed)
                .divide(BigDecimal.valueOf(diskTotal), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }

    /**
     * Calculate transaction success rate
     */
    public BigDecimal getSuccessRate() {
        if (totalTransactions == 0) {
            return BigDecimal.valueOf(100);
        }
        long successful = totalTransactions - failedTransactions;
        return BigDecimal.valueOf(successful)
                .divide(BigDecimal.valueOf(totalTransactions), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }

    /**
     * Check if system is healthy
     */
    public boolean isHealthy() {
        return healthStatus == HealthStatus.HEALTHY;
    }

    /**
     * Check if consensus is synced
     */
    public boolean isConsensusSynced() {
        return consensusStatus == ConsensusStatus.SYNCED;
    }

    /**
     * Record an error
     */
    public void recordError(String error) {
        this.errorCount++;
        this.lastError = error;
        this.lastErrorAt = Instant.now();
        updateHealthStatus();
    }

    /**
     * Record a warning
     */
    public void recordWarning() {
        this.warningCount++;
        updateHealthStatus();
    }

    /**
     * Update health status based on metrics
     */
    public void updateHealthStatus() {
        // Check critical services
        if (!apiAvailable || !databaseAvailable) {
            this.healthStatus = HealthStatus.CRITICAL;
            return;
        }

        // Check resource usage
        BigDecimal memUsage = getMemoryUsagePercent();
        BigDecimal diskUsage = getDiskUsagePercent();

        if (memUsage.compareTo(BigDecimal.valueOf(90)) > 0 ||
            diskUsage.compareTo(BigDecimal.valueOf(90)) > 0) {
            this.healthStatus = HealthStatus.DEGRADED;
            return;
        }

        // Check error rate
        BigDecimal successRate = getSuccessRate();
        if (successRate.compareTo(BigDecimal.valueOf(95)) < 0) {
            this.healthStatus = HealthStatus.DEGRADED;
            return;
        }

        // Check consensus
        if (consensusStatus != ConsensusStatus.SYNCED) {
            this.healthStatus = HealthStatus.DEGRADED;
            return;
        }

        this.healthStatus = HealthStatus.HEALTHY;
    }

    /**
     * Get status summary
     */
    public Map<String, Object> getSummary() {
        Map<String, Object> summary = new HashMap<>();
        summary.put("nodeId", nodeId);
        summary.put("health", healthStatus);
        summary.put("uptime", uptime);
        summary.put("tps", tps);
        summary.put("blockHeight", blockHeight);
        summary.put("peerCount", peerCount);
        summary.put("cpuUsage", cpuUsage);
        summary.put("memoryUsagePercent", getMemoryUsagePercent());
        summary.put("diskUsagePercent", getDiskUsagePercent());
        summary.put("successRate", getSuccessRate());
        summary.put("isLeader", isLeader);
        summary.put("consensusStatus", consensusStatus);
        return summary;
    }

    // ==================== GETTERS AND SETTERS ====================

    public String getNodeId() { return nodeId; }
    public void setNodeId(String nodeId) { this.nodeId = nodeId; }

    public HealthStatus getHealthStatus() { return healthStatus; }
    public void setHealthStatus(HealthStatus healthStatus) { this.healthStatus = healthStatus; }

    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }

    public Long getUptime() { return uptime; }
    public void setUptime(Long uptime) { this.uptime = uptime; }

    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }

    public BigDecimal getTps() { return tps; }
    public void setTps(BigDecimal tps) { this.tps = tps; }

    public BigDecimal getAvgLatency() { return avgLatency; }
    public void setAvgLatency(BigDecimal avgLatency) { this.avgLatency = avgLatency; }

    public BigDecimal getPeakTps() { return peakTps; }
    public void setPeakTps(BigDecimal peakTps) { this.peakTps = peakTps; }

    public Long getTotalTransactions() { return totalTransactions; }
    public void setTotalTransactions(Long totalTransactions) { this.totalTransactions = totalTransactions; }

    public Long getFailedTransactions() { return failedTransactions; }
    public void setFailedTransactions(Long failedTransactions) { this.failedTransactions = failedTransactions; }

    public BigDecimal getCpuUsage() { return cpuUsage; }
    public void setCpuUsage(BigDecimal cpuUsage) { this.cpuUsage = cpuUsage; }

    public Long getMemoryUsed() { return memoryUsed; }
    public void setMemoryUsed(Long memoryUsed) { this.memoryUsed = memoryUsed; }

    public Long getMemoryTotal() { return memoryTotal; }
    public void setMemoryTotal(Long memoryTotal) { this.memoryTotal = memoryTotal; }

    public Long getDiskUsed() { return diskUsed; }
    public void setDiskUsed(Long diskUsed) { this.diskUsed = diskUsed; }

    public Long getDiskTotal() { return diskTotal; }
    public void setDiskTotal(Long diskTotal) { this.diskTotal = diskTotal; }

    public Integer getThreadCount() { return threadCount; }
    public void setThreadCount(Integer threadCount) { this.threadCount = threadCount; }

    public Integer getActiveThreadCount() { return activeThreadCount; }
    public void setActiveThreadCount(Integer activeThreadCount) { this.activeThreadCount = activeThreadCount; }

    public ConsensusStatus getConsensusStatus() { return consensusStatus; }
    public void setConsensusStatus(ConsensusStatus consensusStatus) { this.consensusStatus = consensusStatus; }

    public Long getBlockHeight() { return blockHeight; }
    public void setBlockHeight(Long blockHeight) { this.blockHeight = blockHeight; }

    public BigDecimal getBlockTime() { return blockTime; }
    public void setBlockTime(BigDecimal blockTime) { this.blockTime = blockTime; }

    public Boolean getIsLeader() { return isLeader; }
    public void setIsLeader(Boolean isLeader) { this.isLeader = isLeader; }

    public Integer getPeerCount() { return peerCount; }
    public void setPeerCount(Integer peerCount) { this.peerCount = peerCount; }

    public Integer getActivePeers() { return activePeers; }
    public void setActivePeers(Integer activePeers) { this.activePeers = activePeers; }

    public Long getNetworkBytesIn() { return networkBytesIn; }
    public void setNetworkBytesIn(Long networkBytesIn) { this.networkBytesIn = networkBytesIn; }

    public Long getNetworkBytesOut() { return networkBytesOut; }
    public void setNetworkBytesOut(Long networkBytesOut) { this.networkBytesOut = networkBytesOut; }

    public Long getNetworkErrors() { return networkErrors; }
    public void setNetworkErrors(Long networkErrors) { this.networkErrors = networkErrors; }

    public Integer getConnectionCount() { return connectionCount; }
    public void setConnectionCount(Integer connectionCount) { this.connectionCount = connectionCount; }

    public Boolean getApiAvailable() { return apiAvailable; }
    public void setApiAvailable(Boolean apiAvailable) { this.apiAvailable = apiAvailable; }

    public Boolean getGrpcAvailable() { return grpcAvailable; }
    public void setGrpcAvailable(Boolean grpcAvailable) { this.grpcAvailable = grpcAvailable; }

    public Boolean getDatabaseAvailable() { return databaseAvailable; }
    public void setDatabaseAvailable(Boolean databaseAvailable) { this.databaseAvailable = databaseAvailable; }

    public Boolean getCacheAvailable() { return cacheAvailable; }
    public void setCacheAvailable(Boolean cacheAvailable) { this.cacheAvailable = cacheAvailable; }

    public Long getErrorCount() { return errorCount; }
    public void setErrorCount(Long errorCount) { this.errorCount = errorCount; }

    public Long getWarningCount() { return warningCount; }
    public void setWarningCount(Long warningCount) { this.warningCount = warningCount; }

    public String getLastError() { return lastError; }
    public void setLastError(String lastError) { this.lastError = lastError; }

    public Instant getLastErrorAt() { return lastErrorAt; }
    public void setLastErrorAt(Instant lastErrorAt) { this.lastErrorAt = lastErrorAt; }

    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }

    public String getEnvironment() { return environment; }
    public void setEnvironment(String environment) { this.environment = environment; }

    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }

    public String getMetadata() { return metadata; }
    public void setMetadata(String metadata) { this.metadata = metadata; }

    // ==================== ENUM DEFINITIONS ====================

    public enum HealthStatus {
        HEALTHY,        // System operating normally
        DEGRADED,       // System operating with reduced performance
        UNHEALTHY,      // System experiencing significant issues
        CRITICAL,       // System in critical state
        UNKNOWN         // Status cannot be determined
    }

    public enum ConsensusStatus {
        SYNCED,         // Fully synchronized
        SYNCING,        // Synchronization in progress
        OUT_OF_SYNC,    // Not synchronized
        STALLED,        // Synchronization stalled
        DISABLED        // Consensus disabled
    }

    @Override
    public String toString() {
        return String.format("SystemStatus{nodeId='%s', health=%s, tps=%s, blockHeight=%d, uptime=%d}",
                nodeId, healthStatus, tps, blockHeight, uptime);
    }
}
