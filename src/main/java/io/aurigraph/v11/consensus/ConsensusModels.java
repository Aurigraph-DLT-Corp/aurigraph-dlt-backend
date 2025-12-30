package io.aurigraph.v11.consensus;

import java.time.Instant;
import java.util.Map;

/**
 * Data models for consensus operations
 * Stub implementation for compilation - full implementation pending
 */
public class ConsensusModels {

    /**
     * Consensus status information
     */
    public static class ConsensusStatus {
        public final String nodeId;
        public final String state;
        public final long currentTerm;
        public final long commitIndex;
        public final String leaderId;
        public final long consensusLatency;
        public final long throughput;
        public final int clusterSize;
        public final Instant timestamp;

        public ConsensusStatus(String nodeId, String state, long currentTerm,
                             long commitIndex, String leaderId, long consensusLatency,
                             long throughput, int clusterSize) {
            this.nodeId = nodeId;
            this.state = state;
            this.currentTerm = currentTerm;
            this.commitIndex = commitIndex;
            this.leaderId = leaderId;
            this.consensusLatency = consensusLatency;
            this.throughput = throughput;
            this.clusterSize = clusterSize;
            this.timestamp = Instant.now();
        }
    }

    /**
     * Performance metrics for consensus operations
     */
    public static class PerformanceMetrics {
        public final long tps;
        public final long latencyMs;
        public final long throughput;
        public final double cpuUsage;
        public final double memoryUsage;
        public final Instant timestamp;
        public final Map<String, Object> additionalMetrics;

        public PerformanceMetrics(long tps, long latencyMs, long throughput,
                                double cpuUsage, double memoryUsage,
                                Map<String, Object> additionalMetrics) {
            this.tps = tps;
            this.latencyMs = latencyMs;
            this.throughput = throughput;
            this.cpuUsage = cpuUsage;
            this.memoryUsage = memoryUsage;
            this.timestamp = Instant.now();
            this.additionalMetrics = additionalMetrics != null ? additionalMetrics : Map.of();
        }

        public PerformanceMetrics(long tps, long latencyMs, long throughput) {
            this(tps, latencyMs, throughput, 0.0, 0.0, null);
        }
    }

    /**
     * Transaction model for consensus
     */
    public static class Transaction {
        public final String id;
        public final String from;
        public final String to;
        public final long amount;
        public final Instant timestamp;
        public final String signature;
        public final Map<String, Object> metadata;

        public Transaction(String id, String from, String to, long amount,
                         String signature, Map<String, Object> metadata) {
            this.id = id;
            this.from = from;
            this.to = to;
            this.amount = amount;
            this.timestamp = Instant.now();
            this.signature = signature;
            this.metadata = metadata != null ? metadata : Map.of();
        }

        public Transaction(String id, String from, String to, long amount) {
            this(id, from, to, amount, "", null);
        }
    }
}
