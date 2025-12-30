package io.aurigraph.v11.models;

import java.time.Instant;

/**
 * Live Consensus State Data Transfer Object
 * Represents real-time HyperRAFT++ consensus state for AV11-269
 *
 * This model captures the complete state of the HyperRAFT++ consensus algorithm
 * including leader information, epoch/round/term tracking, participation metrics,
 * and performance indicators.
 */
public record ConsensusState(
    // Algorithm identification
    String algorithm,                    // "HyperRAFT++"

    // Leadership and node information
    String currentLeader,                // Current leader node ID
    String nodeId,                       // This node's ID
    String nodeState,                    // LEADER, FOLLOWER, CANDIDATE

    // Consensus progression tracking
    long epoch,                          // Current epoch number
    long round,                          // Current round number within epoch
    long term,                           // Current consensus term

    // Cluster participation
    int participants,                    // Number of participating nodes
    int quorumSize,                      // Required quorum size for consensus
    int totalNodes,                      // Total nodes in cluster

    // Performance metrics
    Instant lastCommit,                  // Last commit timestamp
    long consensusLatency,               // Average consensus latency in ms
    double throughput,                   // Current consensus throughput (blocks/sec)
    long commitIndex,                    // Current commit index

    // Next election timing
    Instant nextLeaderElection,          // Estimated next election time
    long electionTimeoutMs,              // Election timeout in milliseconds

    // Health and status
    String consensusHealth,              // HEALTHY, DEGRADED, ELECTING, UNHEALTHY
    boolean isHealthy,                   // Overall health indicator
    double performanceScore,             // Performance score (0.0-1.0)

    // Timestamp
    long timestamp                       // Response timestamp
) {

    /**
     * Creates a ConsensusState with default/calculated values
     */
    public static ConsensusState create(
            String currentLeader,
            String nodeId,
            String nodeState,
            long epoch,
            long round,
            long term,
            int participants,
            int quorumSize,
            int totalNodes,
            Instant lastCommit,
            long consensusLatency,
            double throughput,
            long commitIndex,
            Instant nextLeaderElection,
            long electionTimeoutMs,
            String consensusHealth,
            boolean isHealthy,
            double performanceScore) {

        return new ConsensusState(
            "HyperRAFT++",
            currentLeader,
            nodeId,
            nodeState,
            epoch,
            round,
            term,
            participants,
            quorumSize,
            totalNodes,
            lastCommit,
            consensusLatency,
            throughput,
            commitIndex,
            nextLeaderElection,
            electionTimeoutMs,
            consensusHealth,
            isHealthy,
            performanceScore,
            System.currentTimeMillis()
        );
    }

    /**
     * Calculate quorum size based on total nodes (majority: n/2 + 1)
     */
    public static int calculateQuorumSize(int totalNodes) {
        return (totalNodes / 2) + 1;
    }

    /**
     * Determine consensus health based on metrics
     */
    public static String determineHealth(String nodeState, int participants, int totalNodes, long latency) {
        if (nodeState.equals("CANDIDATE")) {
            return "ELECTING";
        }

        // Check if we have enough participants for quorum
        int requiredQuorum = calculateQuorumSize(totalNodes);
        if (participants < requiredQuorum) {
            return "UNHEALTHY";
        }

        // Check if cluster is degraded (lost some nodes but still operational)
        if (participants < totalNodes * 0.8) {
            return "DEGRADED";
        }

        // Check latency
        if (latency > 50) {
            return "DEGRADED";
        }

        return "HEALTHY";
    }

    /**
     * Calculate performance score based on metrics
     */
    public static double calculatePerformanceScore(long latency, double throughput, int participants, int totalNodes) {
        // Latency score (lower is better, target <10ms)
        double latencyScore = Math.max(0, 1.0 - (latency / 100.0));

        // Throughput score (higher is better, target 100k blocks/sec)
        double throughputScore = Math.min(1.0, throughput / 100000.0);

        // Participation score (higher is better)
        double participationScore = (double) participants / totalNodes;

        // Weighted average: latency (40%), throughput (40%), participation (20%)
        return (latencyScore * 0.4) + (throughputScore * 0.4) + (participationScore * 0.2);
    }

    /**
     * Get a human-readable summary of the consensus state
     */
    public String getSummary() {
        return String.format(
            "HyperRAFT++ Consensus - Leader: %s, Term: %d, Health: %s, " +
            "Latency: %dms, Throughput: %.0f blocks/sec, Performance: %.1f%%",
            currentLeader,
            term,
            consensusHealth,
            consensusLatency,
            throughput,
            performanceScore * 100
        );
    }

    /**
     * Check if consensus is operating at optimal performance
     */
    public boolean isOptimalPerformance() {
        return isHealthy &&
               consensusLatency < 10 &&
               throughput > 50000 &&
               performanceScore > 0.8;
    }
}
