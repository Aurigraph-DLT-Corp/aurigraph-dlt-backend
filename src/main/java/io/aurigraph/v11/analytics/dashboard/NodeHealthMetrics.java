package io.aurigraph.v11.analytics.dashboard;

import java.time.Instant;
import java.util.Map;

/**
 * Node Health Metrics Data Model
 *
 * AV11-485: Individual node health tracking and monitoring
 * Tracks node status, resource usage, and performance
 *
 * @author Analytics Dashboard Team
 * @version 11.0.0
 * @since Sprint 13
 */
public record NodeHealthMetrics(
    // Node identification
    String nodeId,
    String nodeType,
    String version,

    // Node status
    NodeStatus status,

    // Performance metrics
    NodePerformance performance,

    // Resource utilization
    NodeResources resources,

    // Connectivity metrics
    NodeConnectivity connectivity,

    // Blockchain metrics
    NodeBlockchain blockchain,

    // Metadata
    Instant timestamp,
    Instant lastUpdate
) {

    /**
     * Node status information
     */
    public record NodeStatus(
        String state,
        String role,
        boolean isHealthy,
        boolean isValidator,
        boolean isSynced,
        long uptimeSeconds,
        Instant startTime,
        Instant lastHeartbeat,
        double healthScore
    ) {}

    /**
     * Node performance metrics
     */
    public record NodePerformance(
        double transactionsPerSecond,
        double blocksPerSecond,
        long totalTransactionsProcessed,
        long totalBlocksProduced,
        double averageLatencyMs,
        double p99LatencyMs,
        long validationsPerformed,
        double validationSuccessRate
    ) {}

    /**
     * Node resource utilization
     */
    public record NodeResources(
        // CPU
        double cpuUsage,
        double cpuUserTime,
        double cpuSystemTime,
        int cpuCores,

        // Memory
        long memoryUsedMB,
        long memoryTotalMB,
        double memoryUsagePercent,
        long heapUsedMB,
        long heapMaxMB,

        // Disk
        long diskUsedGB,
        long diskTotalGB,
        double diskUsagePercent,
        double diskReadMBps,
        double diskWriteMBps,

        // Network
        double networkInMBps,
        double networkOutMBps,
        long totalBytesReceived,
        long totalBytesSent,

        // Threads
        int activeThreads,
        int peakThreads
    ) {}

    /**
     * Node connectivity metrics
     */
    public record NodeConnectivity(
        int connectedPeers,
        int maxPeers,
        double averagePeerLatencyMs,
        Map<String, Double> peerLatencies,
        long inboundConnections,
        long outboundConnections,
        boolean isReachable,
        String publicAddress
    ) {}

    /**
     * Node blockchain metrics
     */
    public record NodeBlockchain(
        long currentBlockHeight,
        long syncedBlockHeight,
        long blocksBehind,
        double syncProgress,
        Instant lastBlockTime,
        long transactionPoolSize,
        long mempoolSize,
        double blockchainSizeGB
    ) {}

    /**
     * Health check result
     */
    public enum HealthStatus {
        HEALTHY,
        DEGRADED,
        UNHEALTHY,
        UNREACHABLE
    }

    /**
     * Node role types
     */
    public enum NodeRole {
        VALIDATOR,
        FULL_NODE,
        LIGHT_NODE,
        ARCHIVE_NODE,
        BOOTSTRAP_NODE
    }

    /**
     * Create default node health metrics
     */
    public static NodeHealthMetrics createDefault(String nodeId) {
        Instant now = Instant.now();
        return new NodeHealthMetrics(
            nodeId,
            "VALIDATOR",
            "12.0.0",
            new NodeStatus("ACTIVE", "LEADER", true, true, true, 0, now, now, 100.0),
            new NodePerformance(0, 0, 0, 0, 0, 0, 0, 100.0),
            new NodeResources(0, 0, 0, Runtime.getRuntime().availableProcessors(),
                0, 0, 0, 0, 0,
                0, 0, 0, 0, 0,
                0, 0, 0, 0,
                0, 0),
            new NodeConnectivity(0, 100, 0, Map.of(), 0, 0, true, "localhost:9003"),
            new NodeBlockchain(0, 0, 0, 100.0, now, 0, 0, 0),
            now,
            now
        );
    }

    /**
     * Calculate overall health score
     */
    public double calculateHealthScore() {
        double statusScore = status.isHealthy() ? 1.0 : 0.0;
        double performanceScore = Math.min(1.0, performance.transactionsPerSecond() / 100000.0);
        double resourceScore = 1.0 - (resources.cpuUsage() / 100.0 * 0.3 +
                                      resources.memoryUsagePercent() / 100.0 * 0.3 +
                                      resources.diskUsagePercent() / 100.0 * 0.2);
        double connectivityScore = Math.min(1.0, connectivity.connectedPeers() / 20.0);

        return (statusScore * 0.4 + performanceScore * 0.3 + resourceScore * 0.2 + connectivityScore * 0.1) * 100.0;
    }
}
