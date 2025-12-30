package io.aurigraph.v11.ai.models;

import java.time.Instant;
import java.util.Map;

/**
 * Consensus metrics for AI optimization
 * Contains real-time consensus performance data used for ML-based optimization
 */
public class ConsensusMetrics {
    private final long blockHeight;
    private final double currentTPS;
    private final double averageLatency;
    private final int activeValidators;
    private final double networkCongestion;
    private final double gasPrice;
    private final Instant timestamp;
    private final Map<String, Object> additionalMetrics;

    public ConsensusMetrics(long blockHeight, double currentTPS, double averageLatency,
                           int activeValidators, double networkCongestion, double gasPrice,
                           Map<String, Object> additionalMetrics) {
        this.blockHeight = blockHeight;
        this.currentTPS = currentTPS;
        this.averageLatency = averageLatency;
        this.activeValidators = activeValidators;
        this.networkCongestion = networkCongestion;
        this.gasPrice = gasPrice;
        this.timestamp = Instant.now();
        this.additionalMetrics = additionalMetrics != null ? additionalMetrics : Map.of();
    }

    public long getBlockHeight() {
        return blockHeight;
    }

    public double getCurrentTPS() {
        return currentTPS;
    }

    public double getAverageLatency() {
        return averageLatency;
    }

    public int getActiveValidators() {
        return activeValidators;
    }

    public double getNetworkCongestion() {
        return networkCongestion;
    }

    public double getGasPrice() {
        return gasPrice;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public Map<String, Object> getAdditionalMetrics() {
        return additionalMetrics;
    }

    /**
     * Convert metrics to feature vector for ML models
     * @return Feature array with 6 dimensions
     */
    public double[] toFeatureVector() {
        return new double[]{
            (double) blockHeight,
            currentTPS,
            averageLatency,
            (double) activeValidators,
            networkCongestion,
            gasPrice
        };
    }

    @Override
    public String toString() {
        return String.format(
            "ConsensusMetrics{blockHeight=%d, TPS=%.2f, latency=%.2fms, validators=%d, congestion=%.2f, gasPrice=%.2f}",
            blockHeight, currentTPS, averageLatency, activeValidators, networkCongestion, gasPrice
        );
    }
}
