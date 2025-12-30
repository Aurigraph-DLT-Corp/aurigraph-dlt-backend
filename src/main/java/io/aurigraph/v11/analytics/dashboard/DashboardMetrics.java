package io.aurigraph.v11.analytics.dashboard;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Real-Time Analytics Dashboard Metrics
 *
 * AV11-485: Comprehensive dashboard data model for real-time monitoring
 * Supports streaming via WebSocket and REST API access
 *
 * @author Analytics Dashboard Team
 * @version 11.0.0
 * @since Sprint 13
 */
public record DashboardMetrics(
    // Real-time transaction metrics
    TransactionMetrics transactionMetrics,

    // Network performance metrics
    NetworkMetrics networkMetrics,

    // Consensus health metrics
    ConsensusMetrics consensusMetrics,

    // Node health status
    List<NodeHealth> nodeHealth,

    // System performance metrics
    SystemPerformance systemPerformance,

    // Historical data points (last 60 seconds)
    List<HistoricalDataPoint> recentHistory,

    // Metadata
    Instant timestamp,
    long updateIntervalMs
) {

    /**
     * Transaction metrics including TPS, finality, and success rates
     */
    public record TransactionMetrics(
        double currentTPS,
        double averageTPS,
        double peakTPS,
        long totalTransactions,
        long pendingTransactions,
        long confirmedTransactions,
        long failedTransactions,
        double successRate,
        double averageFinalityMs,
        double p50FinalityMs,
        double p95FinalityMs,
        double p99FinalityMs,
        Map<String, Long> transactionsByType
    ) {}

    /**
     * Network performance and health metrics
     */
    public record NetworkMetrics(
        int totalNodes,
        int activeNodes,
        int inactiveNodes,
        long totalBlocks,
        double averageBlockTime,
        double networkLatencyMs,
        long bandwidthInMBps,
        long bandwidthOutMBps,
        int activeConnections,
        double networkHealthScore
    ) {}

    /**
     * Consensus system metrics
     */
    public record ConsensusMetrics(
        String consensusAlgorithm,
        long currentTerm,
        String leaderId,
        long commitIndex,
        double consensusLatencyMs,
        double throughputTPS,
        int quorumSize,
        int activeValidators,
        double consensusHealthScore,
        long totalConsensusOps,
        long failedConsensusOps
    ) {}

    /**
     * Individual node health status
     */
    public record NodeHealth(
        String nodeId,
        String status,
        String role,
        double cpuUsage,
        double memoryUsage,
        double diskUsage,
        long uptime,
        Instant lastHeartbeat,
        double latencyMs,
        long blocksProcessed,
        long transactionsProcessed,
        boolean isHealthy
    ) {}

    /**
     * System-wide performance metrics
     */
    public record SystemPerformance(
        double cpuUtilization,
        long memoryUsedMB,
        long memoryTotalMB,
        long diskUsedGB,
        long diskTotalGB,
        double diskIOReadMBps,
        double diskIOWriteMBps,
        int activeThreads,
        int threadPoolSize,
        double gcPauseMs,
        long uptimeSeconds
    ) {}

    /**
     * Historical data point for time-series visualization
     */
    public record HistoricalDataPoint(
        Instant timestamp,
        double tps,
        double latencyMs,
        double cpuUsage,
        double memoryUsage,
        int activeNodes
    ) {}

    /**
     * Create a default/empty dashboard metrics instance
     */
    public static DashboardMetrics createDefault() {
        return new DashboardMetrics(
            new TransactionMetrics(0, 0, 0, 0, 0, 0, 0, 100.0, 0, 0, 0, 0, Map.of()),
            new NetworkMetrics(0, 0, 0, 0, 0, 0, 0, 0, 0, 100.0),
            new ConsensusMetrics("HyperRAFT++", 0, "", 0, 0, 0, 0, 0, 100.0, 0, 0),
            List.of(),
            new SystemPerformance(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0),
            List.of(),
            Instant.now(),
            1000
        );
    }
}
