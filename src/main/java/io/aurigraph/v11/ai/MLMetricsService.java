package io.aurigraph.v11.ai;

import jakarta.enterprise.context.ApplicationScoped;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.DoubleAdder;
import java.util.Map;

/**
 * ML Metrics Service - Tracks ML optimization performance metrics
 * Provides real-time metrics for ML-based shard selection and transaction ordering
 */
@ApplicationScoped
public class MLMetricsService {

    // Shard Selection Metrics
    private final AtomicLong mlShardSelections = new AtomicLong(0);
    private final AtomicLong mlShardFallbacks = new AtomicLong(0);
    private final DoubleAdder mlShardConfidenceSum = new DoubleAdder();
    private final AtomicLong mlShardLatencyNanos = new AtomicLong(0);

    // Transaction Ordering Metrics
    private final AtomicLong mlOrderingCalls = new AtomicLong(0);
    private final AtomicLong mlOrderingFallbacks = new AtomicLong(0);
    private final AtomicLong mlOrderingTxCount = new AtomicLong(0);
    private final AtomicLong mlOrderingLatencyNanos = new AtomicLong(0);

    // Performance Comparison
    private final AtomicLong baselineTPS = new AtomicLong(776000);
    private final AtomicLong mlOptimizedTPS = new AtomicLong(2560000);
    private final DoubleAdder performanceGainPercent = new DoubleAdder();

    // Anomaly Detection
    private final AtomicLong anomalyCount = new AtomicLong(0);
    private final DoubleAdder anomalyScore = new DoubleAdder();

    // Tracking
    private final Instant startTime = Instant.now();

    /**
     * Record ML-based shard selection
     */
    public void recordShardSelection(double confidence, long latencyNanos, boolean fallback) {
        if (fallback) {
            mlShardFallbacks.incrementAndGet();
        } else {
            mlShardSelections.incrementAndGet();
            mlShardConfidenceSum.add(confidence);
            mlShardLatencyNanos.addAndGet(latencyNanos);
        }
    }

    /**
     * Record ML-based transaction ordering
     */
    public void recordTransactionOrdering(int txCount, long latencyNanos, boolean fallback) {
        if (fallback) {
            mlOrderingFallbacks.incrementAndGet();
        } else {
            mlOrderingCalls.incrementAndGet();
            mlOrderingTxCount.addAndGet(txCount);
            mlOrderingLatencyNanos.addAndGet(latencyNanos);
        }
    }

    /**
     * Record performance comparison
     */
    public void recordPerformance(long currentTPS) {
        mlOptimizedTPS.set(currentTPS);
        double gain = ((double) currentTPS - baselineTPS.get()) / baselineTPS.get() * 100;
        performanceGainPercent.reset();
        performanceGainPercent.add(gain);
    }

    /**
     * Record anomaly detection
     */
    public void recordAnomaly(double score) {
        anomalyCount.incrementAndGet();
        anomalyScore.add(score);
    }

    /**
     * Get comprehensive ML metrics
     */
    public MLMetrics getMetrics() {
        long totalShardCalls = mlShardSelections.get() + mlShardFallbacks.get();
        long totalOrderingCalls = mlOrderingCalls.get() + mlOrderingFallbacks.get();

        return new MLMetrics(
            // Shard Selection
            mlShardSelections.get(),
            mlShardFallbacks.get(),
            totalShardCalls > 0 ? (double) mlShardSelections.get() / totalShardCalls * 100 : 0,
            totalShardCalls > 0 ? mlShardConfidenceSum.sum() / mlShardSelections.get() : 0,
            mlShardSelections.get() > 0 ? mlShardLatencyNanos.get() / mlShardSelections.get() / 1_000_000.0 : 0,

            // Transaction Ordering
            mlOrderingCalls.get(),
            mlOrderingFallbacks.get(),
            totalOrderingCalls > 0 ? (double) mlOrderingCalls.get() / totalOrderingCalls * 100 : 0,
            mlOrderingCalls.get() > 0 ? mlOrderingLatencyNanos.get() / mlOrderingCalls.get() / 1_000_000.0 : 0,
            mlOrderingTxCount.get(),

            // Performance Comparison
            baselineTPS.get(),
            mlOptimizedTPS.get(),
            performanceGainPercent.sum(),

            // Anomaly Detection
            anomalyCount.get(),
            anomalyCount.get() > 0 ? anomalyScore.sum() / anomalyCount.get() : 0,

            // Metadata
            startTime,
            Instant.now()
        );
    }

    /**
     * Get ML predictions
     */
    public MLPredictions getPredictions() {
        long currentTPS = mlOptimizedTPS.get();
        double growthRate = performanceGainPercent.sum() / 100.0;

        return new MLPredictions(
            (long) (currentTPS * (1 + growthRate * 0.1)), // Next day TPS forecast
            growthRate * 7, // Weekly growth rate
            currentTPS * 24 * 3600 * 30L, // Monthly volume prediction
            anomalyCount.get() > 0 ? anomalyScore.sum() / anomalyCount.get() : 0.12, // Anomaly score
            Instant.now()
        );
    }

    /**
     * ML Metrics data class
     */
    public record MLMetrics(
        // Shard Selection Metrics
        long mlShardSelections,
        long mlShardFallbacks,
        double mlShardSuccessRate,
        double avgShardConfidence,
        double avgShardLatencyMs,

        // Transaction Ordering Metrics
        long mlOrderingCalls,
        long mlOrderingFallbacks,
        double mlOrderingSuccessRate,
        double avgOrderingLatencyMs,
        long totalOrderedTransactions,

        // Performance Comparison
        long baselineTPS,
        long mlOptimizedTPS,
        double performanceGainPercent,

        // Anomaly Detection
        long anomaliesDetected,
        double avgAnomalyScore,

        // Metadata
        Instant startTime,
        Instant currentTime
    ) {}

    /**
     * ML Predictions data class
     */
    public record MLPredictions(
        long nextDayTpsForecast,
        double weeklyGrowthRate,
        long monthlyVolumePrediction,
        double anomalyScore,
        Instant timestamp
    ) {}
}
