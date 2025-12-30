package io.aurigraph.v11.blockchain;

/**
 * Network Statistics Data Transfer Object
 *
 * Represents comprehensive network statistics for the Aurigraph blockchain.
 * This model is used by the NetworkStatsService to provide real-time
 * network health and performance metrics.
 *
 * @param totalNodes Total number of network nodes
 * @param activeValidators Number of active validators
 * @param currentTPS Current transactions per second
 * @param networkHashRate Network hash rate
 * @param averageBlockTime Average block time in milliseconds
 * @param totalBlocks Total blocks in blockchain
 * @param totalTransactions Total transactions processed
 * @param networkLatency Average network latency in milliseconds
 * @param timestamp Timestamp when stats were generated
 *
 * @version 11.0.0
 * @since AV11-267
 */
public record NetworkStats(
    int totalNodes,
    int activeValidators,
    double currentTPS,
    String networkHashRate,
    double averageBlockTime,
    long totalBlocks,
    long totalTransactions,
    double networkLatency,
    long timestamp
) {

    /**
     * Creates a NetworkStats instance with default values for testing
     */
    public static NetworkStats createDefault() {
        return new NetworkStats(
            127,                    // totalNodes
            121,                    // activeValidators
            1_850_000.0,           // currentTPS
            "12.5 PH/s",           // networkHashRate
            2000.0,                // averageBlockTime (2 seconds)
            1_450_789L,            // totalBlocks
            125_678_000L,          // totalTransactions
            42.5,                  // networkLatency (ms)
            System.currentTimeMillis()
        );
    }

    /**
     * Calculates network health score (0-100)
     * Based on TPS, latency, and validator participation
     */
    public double getHealthScore() {
        // Simple health calculation based on key metrics
        double tpsScore = Math.min(100.0, (currentTPS / 2_000_000.0) * 100.0);
        double latencyScore = Math.max(0.0, 100.0 - (networkLatency / 100.0) * 100.0);
        double validatorScore = (activeValidators / (double) totalNodes) * 100.0;

        return (tpsScore + latencyScore + validatorScore) / 3.0;
    }

    /**
     * Returns a human-readable network status
     */
    public String getNetworkStatus() {
        double health = getHealthScore();
        if (health >= 90.0) return "EXCELLENT";
        if (health >= 75.0) return "GOOD";
        if (health >= 60.0) return "FAIR";
        if (health >= 40.0) return "DEGRADED";
        return "CRITICAL";
    }
}
