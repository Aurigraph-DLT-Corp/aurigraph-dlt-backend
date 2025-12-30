package io.aurigraph.v11.ai;

import io.smallrye.mutiny.Uni;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.stat.regression.SimpleRegression;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Dynamic Batch Size Optimizer - Sprint 1 Day 1 Task 1.2
 *
 * Optimizes transaction batch sizes dynamically based on:
 * - Network conditions (latency, throughput, packet loss)
 * - Node performance (CPU, memory, thread pool utilization)
 * - Transaction characteristics (size, complexity, dependencies)
 * - Historical performance patterns
 *
 * Uses Apache Commons Math for statistical analysis and linear regression
 * to predict optimal batch sizes for maximum throughput.
 *
 * Performance Targets:
 * - Throughput improvement: >20% (via optimal batching)
 * - Latency trade-off: Maintain <100ms P99
 * - Batch size range: 1,000 - 10,000 transactions
 * - Optimization interval: Every 5 seconds
 *
 * @version 1.0.0
 * @since Sprint 1 (Nov 7, 2025)
 */
@ApplicationScoped
public class DynamicBatchSizeOptimizer {

    private static final Logger LOG = Logger.getLogger(DynamicBatchSizeOptimizer.class);

    @ConfigProperty(name = "batch.processor.enabled", defaultValue = "true")
    boolean enabled;

    @ConfigProperty(name = "batch.processor.min.size", defaultValue = "2000")
    int minBatchSize;

    @ConfigProperty(name = "batch.processor.max.size", defaultValue = "15000")
    int maxBatchSize;

    @ConfigProperty(name = "batch.processor.default.size", defaultValue = "8000")
    int defaultBatchSize;

    @ConfigProperty(name = "batch.processor.adaptation.interval.ms", defaultValue = "3000")
    long adaptationIntervalMs;

    @ConfigProperty(name = "consensus.target.tps", defaultValue = "2000000")
    long targetTPS;

    @Inject
    AnomalyDetectionService anomalyDetectionService;

    // Current optimal batch size
    private final AtomicInteger currentBatchSize = new AtomicInteger(5000);

    // Performance tracking
    private final DescriptiveStatistics throughputHistory = new DescriptiveStatistics(100);
    private final DescriptiveStatistics latencyHistory = new DescriptiveStatistics(100);
    private final DescriptiveStatistics batchSizeHistory = new DescriptiveStatistics(100);

    // Regression model for batch size prediction
    private final SimpleRegression batchSizePredictor = new SimpleRegression();

    // Network conditions
    private final AtomicReference<NetworkConditions> currentNetwork =
        new AtomicReference<>(new NetworkConditions(0.0, 0.0, 0.0));

    // Node performance
    private final AtomicReference<NodePerformance> currentPerformance =
        new AtomicReference<>(new NodePerformance(0.0, 0.0, 0.0));

    // Optimization metrics
    private final AtomicLong optimizationRuns = new AtomicLong(0);
    private final AtomicLong throughputImprovements = new AtomicLong(0);
    private volatile double bestThroughput = 0.0;
    private volatile int bestBatchSize = 5000;

    // Last optimization timestamp (initialized to 0 to allow first optimization immediately)
    private volatile long lastOptimization = 0;

    @PostConstruct
    public void initialize() {
        if (!enabled) {
            LOG.info("Dynamic Batch Size Optimizer is DISABLED");
            return;
        }

        currentBatchSize.set(defaultBatchSize);
        bestBatchSize = defaultBatchSize;

        LOG.infof("Dynamic Batch Size Optimizer initialized - Range: [%d, %d], Default: %d",
                 minBatchSize, maxBatchSize, defaultBatchSize);
        LOG.infof("Target TPS: %d, Adaptation Interval: %dms", targetTPS, adaptationIntervalMs);
    }

    /**
     * Get the current optimal batch size
     */
    public int getOptimalBatchSize() {
        if (!enabled) {
            return defaultBatchSize;
        }
        return currentBatchSize.get();
    }

    /**
     * Optimize batch size based on current performance metrics
     *
     * @param currentThroughput Current system throughput (TPS)
     * @param currentLatency Current system latency (ms)
     * @param batchSize Current batch size being used
     * @return Optimized batch size
     */
    public Uni<OptimizationResult> optimizeBatchSize(
            double currentThroughput,
            double currentLatency,
            int batchSize) {

        return Uni.createFrom().item(() -> {
            if (!enabled) {
                return new OptimizationResult(
                    false,
                    defaultBatchSize,
                    defaultBatchSize,
                    "Optimizer disabled",
                    0.0
                );
            }

            // Check if it's time to optimize
            long now = System.currentTimeMillis();
            if (now - lastOptimization < adaptationIntervalMs) {
                return new OptimizationResult(
                    false,
                    currentBatchSize.get(),
                    currentBatchSize.get(),
                    "Too soon to re-optimize",
                    0.0
                );
            }

            lastOptimization = now;
            optimizationRuns.incrementAndGet();

            // Record current metrics
            throughputHistory.addValue(currentThroughput);
            latencyHistory.addValue(currentLatency);
            batchSizeHistory.addValue(batchSize);

            // Update regression model
            batchSizePredictor.addData(batchSize, currentThroughput);

            // Calculate optimal batch size
            int oldBatchSize = currentBatchSize.get();
            int newBatchSize = calculateOptimalBatchSize(
                currentThroughput,
                currentLatency,
                batchSize
            );

            // Apply constraints
            newBatchSize = Math.max(minBatchSize, Math.min(maxBatchSize, newBatchSize));

            // Update if better
            if (currentThroughput > bestThroughput) {
                bestThroughput = currentThroughput;
                bestBatchSize = batchSize;
                throughputImprovements.incrementAndGet();
            }

            currentBatchSize.set(newBatchSize);

            double improvementPercent = ((double) newBatchSize / oldBatchSize - 1.0) * 100.0;

            LOG.infof("Batch Size Optimization: %d → %d (%.1f%%) | TPS: %.0f, Latency: %.2fms",
                     oldBatchSize, newBatchSize, improvementPercent,
                     currentThroughput, currentLatency);

            return new OptimizationResult(
                true,
                oldBatchSize,
                newBatchSize,
                "Optimized based on performance metrics",
                improvementPercent
            );
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    /**
     * Calculate optimal batch size using ML-based analysis
     * Sprint 18 Optimization: Adaptive weighting and increased ramp-up speed
     */
    private int calculateOptimalBatchSize(
            double currentThroughput,
            double currentLatency,
            int currentBatch) {

        // Strategy 1: Regression-based prediction
        int regressionBatch = predictBatchSizeFromRegression(targetTPS);

        // Strategy 2: Performance ratio adjustment
        int ratioAdjusted = adjustBatchByPerformanceRatio(
            currentThroughput,
            currentLatency,
            currentBatch
        );

        // Strategy 3: Gradient ascent (if throughput improving)
        int gradientBatch = applyGradientAscent(currentBatch);

        // Sprint 18: Adaptive weight distribution based on model quality
        double rSquare = batchSizePredictor.getN() >= 5 ? batchSizePredictor.getRSquare() : 0.0;
        double regressionWeight = rSquare > 0.8 ? 0.5 : 0.3;
        double ratioWeight = 0.4;
        double gradientWeight = 1.0 - regressionWeight - ratioWeight;

        // Weighted combination of strategies
        int predictedBatch = (int) (
            regressionBatch * regressionWeight +
            ratioAdjusted * ratioWeight +
            gradientBatch * gradientWeight
        );

        // Sprint 18: Adaptive change rate - faster during ramp-up, conservative at steady-state
        long samples = optimizationRuns.get();
        int maxChange = samples < 20
            ? (int) (currentBatch * 0.3)  // 30% change during ramp-up (first 20 optimizations)
            : (int) (currentBatch * 0.2); // 20% change at steady-state

        int change = predictedBatch - currentBatch;
        if (Math.abs(change) > maxChange) {
            change = change > 0 ? maxChange : -maxChange;
        }

        return currentBatch + change;
    }

    /**
     * Predict batch size using linear regression model
     * Sprint 18 Optimization: Reduced threshold from 10 to 5 for faster cold-start
     */
    private int predictBatchSizeFromRegression(double targetThroughput) {
        if (batchSizePredictor.getN() < 5) {
            return currentBatchSize.get(); // Not enough data yet
        }

        try {
            // Predict batch size needed for target throughput
            // throughput = slope * batchSize + intercept
            // batchSize = (throughput - intercept) / slope

            double slope = batchSizePredictor.getSlope();
            double intercept = batchSizePredictor.getIntercept();

            if (slope <= 0) {
                return currentBatchSize.get(); // Invalid model
            }

            int predicted = (int) ((targetThroughput - intercept) / slope);

            LOG.debugf("Regression prediction: batch=%d for target TPS=%.0f (slope=%.2f, R²=%.3f)",
                      predicted, targetThroughput, slope, batchSizePredictor.getRSquare());

            return predicted;
        } catch (Exception e) {
            LOG.debugf("Regression prediction failed: %s", e.getMessage());
            return currentBatchSize.get();
        }
    }

    /**
     * Adjust batch size based on performance ratio
     */
    private int adjustBatchByPerformanceRatio(
            double currentThroughput,
            double currentLatency,
            int currentBatch) {

        // Performance efficiency: how close are we to target?
        double throughputRatio = currentThroughput / targetTPS;
        double latencyTarget = 100.0; // Target P99 latency (ms)
        double latencyRatio = latencyTarget / Math.max(1.0, currentLatency);

        // Combined efficiency score (0.0 - 2.0)
        double efficiency = (throughputRatio + latencyRatio) / 2.0;

        int adjusted = currentBatch;

        if (efficiency < 0.7) {
            // Poor performance: reduce batch size (less latency)
            adjusted = (int) (currentBatch * 0.9);
        } else if (efficiency > 1.2 && currentLatency < latencyTarget) {
            // Excellent performance: increase batch size (more throughput)
            adjusted = (int) (currentBatch * 1.1);
        }

        LOG.debugf("Performance ratio adjustment: efficiency=%.2f, batch %d → %d",
                  efficiency, currentBatch, adjusted);

        return adjusted;
    }

    /**
     * Apply gradient ascent for throughput optimization
     */
    private int applyGradientAscent(int currentBatch) {
        if (throughputHistory.getN() < 5) {
            return currentBatch; // Not enough history
        }

        // Calculate throughput gradient (recent trend)
        double[] recent = throughputHistory.getValues();
        int n = Math.min(5, recent.length);

        double gradient = 0.0;
        for (int i = recent.length - n; i < recent.length - 1; i++) {
            gradient += recent[i + 1] - recent[i];
        }
        gradient /= (n - 1);

        int adjusted = currentBatch;

        if (gradient > 0) {
            // Throughput improving: continue in same direction
            adjusted = (int) (currentBatch * 1.05);
        } else if (gradient < -10000) {
            // Throughput degrading: reverse direction
            adjusted = (int) (currentBatch * 0.95);
        }

        LOG.debugf("Gradient ascent: gradient=%.0f TPS/step, batch %d → %d",
                  gradient, currentBatch, adjusted);

        return adjusted;
    }

    /**
     * Update network conditions for optimization
     */
    public void updateNetworkConditions(double latency, double throughput, double packetLoss) {
        currentNetwork.set(new NetworkConditions(latency, throughput, packetLoss));

        // Check for network anomalies
        if (anomalyDetectionService != null && enabled) {
            anomalyDetectionService.analyzePerformance(throughput, latency)
                .subscribe().with(
                    result -> {
                        if (result.isAnomaly()) {
                            LOG.warnf("Network anomaly detected: %s", result.getReason());
                            // Reduce batch size during anomalies
                            int reduced = (int) (currentBatchSize.get() * 0.8);
                            currentBatchSize.set(Math.max(minBatchSize, reduced));
                        }
                    },
                    error -> LOG.debugf("Anomaly detection failed: %s", error.getMessage())
                );
        }
    }

    /**
     * Update node performance metrics
     */
    public void updateNodePerformance(double cpuUsage, double memoryUsage, double threadUtil) {
        currentPerformance.set(new NodePerformance(cpuUsage, memoryUsage, threadUtil));

        // Adapt batch size based on resource utilization
        if (enabled) {
            if (cpuUsage > 90 || memoryUsage > 90) {
                // High resource usage: reduce batch size
                int reduced = (int) (currentBatchSize.get() * 0.85);
                currentBatchSize.set(Math.max(minBatchSize, reduced));
                LOG.debugf("High resource usage detected (CPU: %.1f%%, Mem: %.1f%%), reducing batch to %d",
                          cpuUsage, memoryUsage, reduced);
            }
        }
    }

    /**
     * Get optimization statistics
     */
    public OptimizationStatistics getStatistics() {
        return new OptimizationStatistics(
            optimizationRuns.get(),
            currentBatchSize.get(),
            bestBatchSize,
            bestThroughput,
            throughputHistory.getMean(),
            latencyHistory.getMean(),
            throughputHistory.getStandardDeviation(),
            latencyHistory.getStandardDeviation(),
            throughputImprovements.get(),
            batchSizePredictor.getN(),
            batchSizePredictor.getRSquare()
        );
    }

    /**
     * Reset optimizer state (for testing)
     */
    public void reset() {
        currentBatchSize.set(defaultBatchSize);
        throughputHistory.clear();
        latencyHistory.clear();
        batchSizeHistory.clear();
        batchSizePredictor.clear();
        optimizationRuns.set(0);
        throughputImprovements.set(0);
        bestThroughput = 0.0;
        bestBatchSize = defaultBatchSize;
        lastOptimization = 0;  // Reset to 0 to allow immediate optimization
        LOG.info("Dynamic Batch Size Optimizer reset");
    }

    // ==================== DATA CLASSES ====================

    public static class NetworkConditions {
        public final double latency;
        public final double throughput;
        public final double packetLoss;

        public NetworkConditions(double latency, double throughput, double packetLoss) {
            this.latency = latency;
            this.throughput = throughput;
            this.packetLoss = packetLoss;
        }
    }

    public static class NodePerformance {
        public final double cpuUsage;
        public final double memoryUsage;
        public final double threadUtilization;

        public NodePerformance(double cpuUsage, double memoryUsage, double threadUtilization) {
            this.cpuUsage = cpuUsage;
            this.memoryUsage = memoryUsage;
            this.threadUtilization = threadUtilization;
        }
    }

    public static class OptimizationResult {
        public final boolean optimized;
        public final int oldBatchSize;
        public final int newBatchSize;
        public final String reason;
        public final double improvementPercent;

        public OptimizationResult(boolean optimized, int oldBatchSize, int newBatchSize,
                                 String reason, double improvementPercent) {
            this.optimized = optimized;
            this.oldBatchSize = oldBatchSize;
            this.newBatchSize = newBatchSize;
            this.reason = reason;
            this.improvementPercent = improvementPercent;
        }

        public int getNewBatchSize() { return newBatchSize; }
        public boolean isOptimized() { return optimized; }
    }

    public static class OptimizationStatistics {
        public final long totalOptimizations;
        public final int currentBatchSize;
        public final int bestBatchSize;
        public final double bestThroughput;
        public final double avgThroughput;
        public final double avgLatency;
        public final double stdDevThroughput;
        public final double stdDevLatency;
        public final long improvements;
        public final long regressionDataPoints;
        public final double regressionRSquare;

        public OptimizationStatistics(long totalOptimizations, int currentBatchSize,
                                     int bestBatchSize, double bestThroughput,
                                     double avgThroughput, double avgLatency,
                                     double stdDevThroughput, double stdDevLatency,
                                     long improvements, long regressionDataPoints,
                                     double regressionRSquare) {
            this.totalOptimizations = totalOptimizations;
            this.currentBatchSize = currentBatchSize;
            this.bestBatchSize = bestBatchSize;
            this.bestThroughput = bestThroughput;
            this.avgThroughput = avgThroughput;
            this.avgLatency = avgLatency;
            this.stdDevThroughput = stdDevThroughput;
            this.stdDevLatency = stdDevLatency;
            this.improvements = improvements;
            this.regressionDataPoints = regressionDataPoints;
            this.regressionRSquare = regressionRSquare;
        }

        @Override
        public String toString() {
            return String.format(
                "OptimizationStatistics{optimizations=%d, current=%d, best=%d (%.0f TPS), " +
                "avgTPS=%.0f, avgLatency=%.2fms, improvements=%d, R²=%.3f}",
                totalOptimizations, currentBatchSize, bestBatchSize, bestThroughput,
                avgThroughput, avgLatency, improvements, regressionRSquare
            );
        }
    }
}
