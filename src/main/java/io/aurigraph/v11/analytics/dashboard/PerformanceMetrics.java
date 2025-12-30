package io.aurigraph.v11.analytics.dashboard;

import java.time.Instant;
import java.util.Map;

/**
 * Performance Metrics Data Model
 *
 * AV11-485: Detailed performance metrics for system monitoring
 * Tracks latency, throughput, resource utilization, and errors
 *
 * @author Analytics Dashboard Team
 * @version 11.0.0
 * @since Sprint 13
 */
public record PerformanceMetrics(
    // Throughput metrics
    ThroughputMetrics throughput,

    // Latency metrics
    LatencyMetrics latency,

    // Resource utilization
    ResourceMetrics resources,

    // Error and reliability metrics
    ReliabilityMetrics reliability,

    // Cache and storage metrics
    StorageMetrics storage,

    // Metadata
    Instant timestamp
) {

    /**
     * Throughput metrics (TPS and block production)
     */
    public record ThroughputMetrics(
        double currentTPS,
        double averageTPS,
        double peakTPS,
        double targetTPS,
        double throughputEfficiency,
        long totalTransactionsProcessed,
        double blocksPerSecond,
        double transactionsPerBlock
    ) {}

    /**
     * Latency metrics (response times and percentiles)
     */
    public record LatencyMetrics(
        double averageLatencyMs,
        double p50LatencyMs,
        double p95LatencyMs,
        double p99LatencyMs,
        double minLatencyMs,
        double maxLatencyMs,
        double finalityTimeMs,
        double consensusLatencyMs
    ) {}

    /**
     * Resource utilization metrics
     */
    public record ResourceMetrics(
        // CPU metrics
        double cpuUtilization,
        int availableCPUs,
        double systemLoad,

        // Memory metrics
        long memoryUsedMB,
        long memoryFreeMB,
        long memoryTotalMB,
        double memoryUtilization,

        // Disk metrics
        long diskUsedGB,
        long diskFreeGB,
        long diskTotalGB,
        double diskUtilization,

        // Network metrics
        double networkInMBps,
        double networkOutMBps,
        long totalBytesIn,
        long totalBytesOut,

        // Thread metrics
        int activeThreads,
        int peakThreads,
        int threadPoolSize
    ) {}

    /**
     * Reliability and error metrics
     */
    public record ReliabilityMetrics(
        double errorRate,
        long totalErrors,
        long totalRequests,
        double successRate,
        long timeoutsCount,
        long retriesCount,
        Map<String, Long> errorsByType,
        double uptime,
        Instant lastFailure
    ) {}

    /**
     * Storage and cache metrics
     */
    public record StorageMetrics(
        // Transaction storage
        long storedTransactions,
        long transactionCacheHits,
        long transactionCacheMisses,
        double transactionCacheHitRate,

        // Block storage
        long storedBlocks,
        long blockchainSizeGB,

        // Database metrics
        long dbConnections,
        long dbActiveConnections,
        double dbQueryTimeMs,

        // Cache metrics
        long cacheSize,
        long cacheMaxSize,
        double cacheUtilization,
        long totalCacheEvictions
    ) {}

    /**
     * Create default performance metrics
     */
    public static PerformanceMetrics createDefault() {
        return new PerformanceMetrics(
            new ThroughputMetrics(0, 0, 0, 2_000_000, 0, 0, 0, 0),
            new LatencyMetrics(0, 0, 0, 0, 0, 0, 0, 0),
            new ResourceMetrics(0, Runtime.getRuntime().availableProcessors(), 0,
                0, 0, Runtime.getRuntime().totalMemory() / (1024 * 1024), 0,
                0, 0, 0, 0,
                0, 0, 0, 0,
                0, 0, 0),
            new ReliabilityMetrics(0, 0, 0, 100.0, 0, 0, Map.of(), 100.0, null),
            new StorageMetrics(0, 0, 0, 100.0, 0, 0, 0, 0, 0, 0, 0, 0, 0),
            Instant.now()
        );
    }
}
