package io.aurigraph.v11.performance;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Comprehensive Performance Metrics for Sprint 5 Optimization
 *
 * Tracks key performance indicators for 3M+ TPS optimization:
 * - Throughput (TPS, peak TPS, sustained TPS)
 * - Latency (P50, P95, P99, P99.9)
 * - Resource utilization (CPU, memory, threads)
 * - ML optimization (batch size, thread count, confidence)
 * - Queue metrics (depth, utilization, backpressure)
 *
 * @version 2.0.0
 * @since Sprint 5 (Oct 20, 2025)
 */
public class PerformanceMetrics {

    // Throughput Metrics
    public final double tps;                    // Current TPS
    public final double peakTps;                // Peak TPS achieved
    public final double targetTps;              // Target TPS (3M)
    public final double avgTps;                 // Average TPS over window
    public final long totalTransactions;        // Total transactions processed

    // Latency Metrics (in milliseconds)
    public final double latencyP50;             // Median latency
    public final double latencyP95;             // 95th percentile
    public final double latencyP99;             // 99th percentile
    public final double latencyP999;            // 99.9th percentile
    public final double avgLatency;             // Average latency
    public final double minLatency;             // Minimum latency
    public final double maxLatency;             // Maximum latency

    // Resource Utilization
    public final double cpuUtilization;         // CPU usage (0.0-1.0)
    public final double memoryUsage;            // Heap memory usage (0.0-1.0)
    public final double memoryHeapMB;           // Heap memory in MB
    public final double memoryNonHeapMB;        // Non-heap memory in MB
    public final int activeThreads;             // Active thread count
    public final int virtualThreads;            // Virtual thread count

    // ML Optimization Metrics
    public final int currentBatchSize;          // Current batch size
    public final int optimalBatchSize;          // ML-predicted optimal batch size
    public final int currentThreadCount;        // Current thread count
    public final int optimalThreadCount;        // ML-predicted optimal thread count
    public final double mlConfidence;           // ML model confidence (0.0-1.0)
    public final double mlLatency;              // ML prediction latency (ms)
    public final long mlFallbackCount;          // Number of ML fallbacks

    // Queue Metrics
    public final int queueDepth;                // Current queue depth
    public final int queueCapacity;             // Maximum queue capacity
    public final double queueUtilization;       // Queue utilization (0.0-1.0)
    public final boolean backpressure;          // Backpressure detected

    // Error Metrics
    public final double errorRate;              // Error rate (0.0-1.0)
    public final long errorCount;               // Total error count
    public final long successCount;             // Total success count

    // Timestamp
    public final Instant timestamp;             // Metric collection timestamp

    /**
     * Builder for PerformanceMetrics
     */
    public static class Builder {
        private double tps = 0.0;
        private double peakTps = 0.0;
        private double targetTps = 3_000_000.0;
        private double avgTps = 0.0;
        private long totalTransactions = 0L;

        private double latencyP50 = 0.0;
        private double latencyP95 = 0.0;
        private double latencyP99 = 0.0;
        private double latencyP999 = 0.0;
        private double avgLatency = 0.0;
        private double minLatency = 0.0;
        private double maxLatency = 0.0;

        private double cpuUtilization = 0.0;
        private double memoryUsage = 0.0;
        private double memoryHeapMB = 0.0;
        private double memoryNonHeapMB = 0.0;
        private int activeThreads = 0;
        private int virtualThreads = 0;

        private int currentBatchSize = 0;
        private int optimalBatchSize = 0;
        private int currentThreadCount = 0;
        private int optimalThreadCount = 0;
        private double mlConfidence = 0.0;
        private double mlLatency = 0.0;
        private long mlFallbackCount = 0L;

        private int queueDepth = 0;
        private int queueCapacity = 500000;
        private double queueUtilization = 0.0;
        private boolean backpressure = false;

        private double errorRate = 0.0;
        private long errorCount = 0L;
        private long successCount = 0L;

        private Instant timestamp = Instant.now();

        // Throughput Setters
        public Builder tps(double tps) { this.tps = tps; return this; }
        public Builder peakTps(double peakTps) { this.peakTps = peakTps; return this; }
        public Builder targetTps(double targetTps) { this.targetTps = targetTps; return this; }
        public Builder avgTps(double avgTps) { this.avgTps = avgTps; return this; }
        public Builder totalTransactions(long total) { this.totalTransactions = total; return this; }

        // Latency Setters
        public Builder latencyP50(double p50) { this.latencyP50 = p50; return this; }
        public Builder latencyP95(double p95) { this.latencyP95 = p95; return this; }
        public Builder latencyP99(double p99) { this.latencyP99 = p99; return this; }
        public Builder latencyP999(double p999) { this.latencyP999 = p999; return this; }
        public Builder avgLatency(double avg) { this.avgLatency = avg; return this; }
        public Builder minLatency(double min) { this.minLatency = min; return this; }
        public Builder maxLatency(double max) { this.maxLatency = max; return this; }

        // Resource Setters
        public Builder cpuUtilization(double cpu) { this.cpuUtilization = cpu; return this; }
        public Builder memoryUsage(double memory) { this.memoryUsage = memory; return this; }
        public Builder memoryHeapMB(double heap) { this.memoryHeapMB = heap; return this; }
        public Builder memoryNonHeapMB(double nonHeap) { this.memoryNonHeapMB = nonHeap; return this; }
        public Builder activeThreads(int threads) { this.activeThreads = threads; return this; }
        public Builder virtualThreads(int vThreads) { this.virtualThreads = vThreads; return this; }

        // ML Optimization Setters
        public Builder currentBatchSize(int size) { this.currentBatchSize = size; return this; }
        public Builder optimalBatchSize(int size) { this.optimalBatchSize = size; return this; }
        public Builder currentThreadCount(int count) { this.currentThreadCount = count; return this; }
        public Builder optimalThreadCount(int count) { this.optimalThreadCount = count; return this; }
        public Builder mlConfidence(double confidence) { this.mlConfidence = confidence; return this; }
        public Builder mlLatency(double latency) { this.mlLatency = latency; return this; }
        public Builder mlFallbackCount(long count) { this.mlFallbackCount = count; return this; }

        // Queue Setters
        public Builder queueDepth(int depth) { this.queueDepth = depth; return this; }
        public Builder queueCapacity(int capacity) { this.queueCapacity = capacity; return this; }
        public Builder queueUtilization(double utilization) { this.queueUtilization = utilization; return this; }
        public Builder backpressure(boolean bp) { this.backpressure = bp; return this; }

        // Error Setters
        public Builder errorRate(double rate) { this.errorRate = rate; return this; }
        public Builder errorCount(long count) { this.errorCount = count; return this; }
        public Builder successCount(long count) { this.successCount = count; return this; }

        // Timestamp Setter
        public Builder timestamp(Instant ts) { this.timestamp = ts; return this; }

        public PerformanceMetrics build() {
            return new PerformanceMetrics(this);
        }
    }

    private PerformanceMetrics(Builder builder) {
        this.tps = builder.tps;
        this.peakTps = builder.peakTps;
        this.targetTps = builder.targetTps;
        this.avgTps = builder.avgTps;
        this.totalTransactions = builder.totalTransactions;

        this.latencyP50 = builder.latencyP50;
        this.latencyP95 = builder.latencyP95;
        this.latencyP99 = builder.latencyP99;
        this.latencyP999 = builder.latencyP999;
        this.avgLatency = builder.avgLatency;
        this.minLatency = builder.minLatency;
        this.maxLatency = builder.maxLatency;

        this.cpuUtilization = builder.cpuUtilization;
        this.memoryUsage = builder.memoryUsage;
        this.memoryHeapMB = builder.memoryHeapMB;
        this.memoryNonHeapMB = builder.memoryNonHeapMB;
        this.activeThreads = builder.activeThreads;
        this.virtualThreads = builder.virtualThreads;

        this.currentBatchSize = builder.currentBatchSize;
        this.optimalBatchSize = builder.optimalBatchSize;
        this.currentThreadCount = builder.currentThreadCount;
        this.optimalThreadCount = builder.optimalThreadCount;
        this.mlConfidence = builder.mlConfidence;
        this.mlLatency = builder.mlLatency;
        this.mlFallbackCount = builder.mlFallbackCount;

        this.queueDepth = builder.queueDepth;
        this.queueCapacity = builder.queueCapacity;
        this.queueUtilization = builder.queueUtilization;
        this.backpressure = builder.backpressure;

        this.errorRate = builder.errorRate;
        this.errorCount = builder.errorCount;
        this.successCount = builder.successCount;

        this.timestamp = builder.timestamp;
    }

    /**
     * Calculate performance grade based on TPS achievement
     *
     * @return Grade string (EXCEPTIONAL+, EXCEPTIONAL, EXCELLENT, GOOD, ACCEPTABLE, NEEDS IMPROVEMENT)
     */
    public String getPerformanceGrade() {
        double achievement = (tps / targetTps) * 100.0;

        if (achievement >= 175.0) return "EXCEPTIONAL+ (175%+)";
        if (achievement >= 150.0) return "EXCEPTIONAL (150%+)";
        if (achievement >= 125.0) return "EXCELLENT (125%+)";
        if (achievement >= 100.0) return "GOOD (100%+)";
        if (achievement >= 75.0) return "ACCEPTABLE (75%+)";
        return "NEEDS IMPROVEMENT (<75%)";
    }

    /**
     * Calculate target achievement percentage
     */
    public double getTargetAchievement() {
        return (tps / targetTps) * 100.0;
    }

    /**
     * Check if system is meeting performance targets
     */
    public boolean meetsTargets() {
        return tps >= targetTps &&
               latencyP99 < 50.0 &&
               errorRate < 0.001 &&
               cpuUtilization < 0.85;
    }

    /**
     * Get human-readable summary
     */
    public String getSummary() {
        return String.format(
            "TPS: %.2fM/%.2fM (%.1f%%) | P99: %.1fms | Errors: %.3f%% | CPU: %.1f%% | Grade: %s",
            tps / 1_000_000.0,
            targetTps / 1_000_000.0,
            getTargetAchievement(),
            latencyP99,
            errorRate * 100.0,
            cpuUtilization * 100.0,
            getPerformanceGrade()
        );
    }

    @Override
    public String toString() {
        return String.format(
            "PerformanceMetrics{tps=%.2fM, peakTps=%.2fM, targetTps=%.2fM, " +
            "latencyP99=%.2fms, errorRate=%.4f%%, cpu=%.1f%%, memory=%.1fMB, " +
            "batch=%d/%d, threads=%d/%d, mlConfidence=%.2f, " +
            "queue=%d/%d (%.1f%%), timestamp=%s}",
            tps / 1_000_000.0,
            peakTps / 1_000_000.0,
            targetTps / 1_000_000.0,
            latencyP99,
            errorRate * 100.0,
            cpuUtilization * 100.0,
            memoryHeapMB,
            currentBatchSize,
            optimalBatchSize,
            currentThreadCount,
            optimalThreadCount,
            mlConfidence,
            queueDepth,
            queueCapacity,
            queueUtilization * 100.0,
            timestamp
        );
    }
}
