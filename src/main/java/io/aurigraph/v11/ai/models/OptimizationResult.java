package io.aurigraph.v11.ai.models;

import java.time.Instant;

/**
 * Optimization result from AI consensus optimizer
 * Contains optimized consensus parameters and confidence score
 */
public class OptimizationResult {
    private final int optimalBatchSize;
    private final int optimalThreads;
    private final int optimalTimeoutMs;
    private final double confidence;
    private final double expectedLatencyReduction;
    private final Instant timestamp;

    public OptimizationResult(int optimalBatchSize, int optimalThreads,
                             int optimalTimeoutMs, double confidence) {
        this(optimalBatchSize, optimalThreads, optimalTimeoutMs, confidence, 0.0);
    }

    public OptimizationResult(int optimalBatchSize, int optimalThreads,
                             int optimalTimeoutMs, double confidence,
                             double expectedLatencyReduction) {
        this.optimalBatchSize = optimalBatchSize;
        this.optimalThreads = optimalThreads;
        this.optimalTimeoutMs = optimalTimeoutMs;
        this.confidence = confidence;
        this.expectedLatencyReduction = expectedLatencyReduction;
        this.timestamp = Instant.now();
    }

    public int getOptimalBatchSize() {
        return optimalBatchSize;
    }

    public int getOptimalThreads() {
        return optimalThreads;
    }

    public int getOptimalTimeoutMs() {
        return optimalTimeoutMs;
    }

    public double getConfidence() {
        return confidence;
    }

    public double getExpectedLatencyReduction() {
        return expectedLatencyReduction;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return String.format(
            "OptimizationResult{batchSize=%d, threads=%d, timeout=%dms, confidence=%.2f%%, latencyReduction=%.2f%%}",
            optimalBatchSize, optimalThreads, optimalTimeoutMs, confidence * 100, expectedLatencyReduction * 100
        );
    }
}
