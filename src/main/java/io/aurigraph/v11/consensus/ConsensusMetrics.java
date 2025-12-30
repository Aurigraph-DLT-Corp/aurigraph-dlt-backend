package io.aurigraph.v11.consensus;

import java.util.concurrent.atomic.AtomicLong;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Metrics tracking for HyperRAFT++ consensus operations
 * Tracks TPS, latency, replication performance, and consensus state
 */
@ApplicationScoped
public class ConsensusMetrics {

    private final AtomicLong transactionsProcessed = new AtomicLong(0);
    private final AtomicLong transactionsFailed = new AtomicLong(0);
    private final AtomicLong totalReplicationTime = new AtomicLong(0);
    private final AtomicLong replicationCount = new AtomicLong(0);
    private final AtomicLong electionCount = new AtomicLong(0);
    private final AtomicLong heartbeatsSent = new AtomicLong(0);
    private final AtomicLong totalCommitTime = new AtomicLong(0);
    private final AtomicLong commitCount = new AtomicLong(0);
    private final AtomicLong successfulCommits = new AtomicLong(0);
    private final AtomicLong totalValidationTime = new AtomicLong(0);
    private final AtomicLong validationCount = new AtomicLong(0);
    private final AtomicLong successfulValidations = new AtomicLong(0);

    private volatile long lastResetTime = System.currentTimeMillis();

    /**
     * Record a successfully processed transaction
     */
    public void recordTransactionProcessed() {
        transactionsProcessed.incrementAndGet();
    }

    /**
     * Record a failed transaction
     */
    public void recordTransactionFailed() {
        transactionsFailed.incrementAndGet();
    }

    /**
     * Record replication metrics
     */
    public void recordReplication(long replicationTimeMs) {
        replicationCount.incrementAndGet();
        totalReplicationTime.addAndGet(replicationTimeMs);
    }

    /**
     * Record election event
     */
    public void recordElection() {
        electionCount.incrementAndGet();
    }

    /**
     * Record election with status and timing
     */
    public void recordElection(boolean success, long electionTimeMs) {
        electionCount.incrementAndGet();
        if (success) {
            successfulCommits.incrementAndGet();
        }
    }

    /**
     * Record commit metrics
     */
    public void recordCommit(boolean success, long commitTimeMs) {
        commitCount.incrementAndGet();
        totalCommitTime.addAndGet(commitTimeMs);
        if (success) {
            successfulCommits.incrementAndGet();
        }
    }

    /**
     * Record validation metrics
     */
    public void recordValidation(boolean success, long validationTimeMs, int txnCount) {
        validationCount.incrementAndGet();
        totalValidationTime.addAndGet(validationTimeMs);
        if (success) {
            successfulValidations.incrementAndGet();
        }
    }

    /**
     * Record heartbeat sent
     */
    public void recordHeartbeat() {
        heartbeatsSent.incrementAndGet();
    }

    /**
     * Get current TPS (transactions per second)
     */
    public double getCurrentTPS() {
        long elapsedSeconds = (System.currentTimeMillis() - lastResetTime) / 1000;
        if (elapsedSeconds == 0) return 0;
        return (double) transactionsProcessed.get() / elapsedSeconds;
    }

    /**
     * Get average replication latency in milliseconds
     */
    public double getAverageReplicationLatency() {
        long count = replicationCount.get();
        if (count == 0) return 0;
        return (double) totalReplicationTime.get() / count;
    }

    /**
     * Get average commit latency in milliseconds
     */
    public double getAverageCommitLatency() {
        long count = commitCount.get();
        if (count == 0) return 0;
        return (double) totalCommitTime.get() / count;
    }

    /**
     * Get transaction success rate (0-1)
     */
    public double getSuccessRate() {
        long total = transactionsProcessed.get() + transactionsFailed.get();
        if (total == 0) return 1.0;
        return (double) transactionsProcessed.get() / total;
    }

    /**
     * Get commit success rate (0-1)
     */
    public double getCommitSuccessRate() {
        long total = commitCount.get();
        if (total == 0) return 1.0;
        return (double) successfulCommits.get() / total;
    }

    /**
     * Reset all metrics
     */
    public void reset() {
        transactionsProcessed.set(0);
        transactionsFailed.set(0);
        totalReplicationTime.set(0);
        replicationCount.set(0);
        electionCount.set(0);
        heartbeatsSent.set(0);
        totalCommitTime.set(0);
        commitCount.set(0);
        successfulCommits.set(0);
        totalValidationTime.set(0);
        validationCount.set(0);
        successfulValidations.set(0);
        lastResetTime = System.currentTimeMillis();
    }

    /**
     * Get metrics snapshot
     */
    public MetricsSnapshot getSnapshot() {
        return new MetricsSnapshot(
            getCurrentTPS(),
            getAverageReplicationLatency(),
            getAverageCommitLatency(),
            getSuccessRate(),
            getCommitSuccessRate(),
            transactionsProcessed.get(),
            transactionsFailed.get(),
            electionCount.get(),
            heartbeatsSent.get(),
            commitCount.get(),
            validationCount.get()
        );
    }

    /**
     * Metrics snapshot class for reporting
     */
    public static class MetricsSnapshot {
        public final double tps;
        public final double avgReplicationLatency;
        public final double avgCommitLatency;
        public final double txnSuccessRate;
        public final double commitSuccessRate;
        public final long processedTransactions;
        public final long failedTransactions;
        public final long elections;
        public final long heartbeats;
        public final long commits;
        public final long validations;
        public final long timestamp;

        public MetricsSnapshot(
                double tps,
                double avgReplicationLatency,
                double avgCommitLatency,
                double txnSuccessRate,
                double commitSuccessRate,
                long processedTransactions,
                long failedTransactions,
                long elections,
                long heartbeats,
                long commits,
                long validations) {
            this.tps = tps;
            this.avgReplicationLatency = avgReplicationLatency;
            this.avgCommitLatency = avgCommitLatency;
            this.txnSuccessRate = txnSuccessRate;
            this.commitSuccessRate = commitSuccessRate;
            this.processedTransactions = processedTransactions;
            this.failedTransactions = failedTransactions;
            this.elections = elections;
            this.heartbeats = heartbeats;
            this.commits = commits;
            this.validations = validations;
            this.timestamp = System.currentTimeMillis();
        }
    }

    /**
     * Get metrics summary as string
     */
    @Override
    public String toString() {
        return String.format(
            "ConsensusMetrics{TPS=%.2f, AvgReplicationMs=%.2f, AvgCommitMs=%.2f, " +
            "TxnSuccessRate=%.2f%%, CommitSuccessRate=%.2f%%, Processed=%d, Failed=%d, " +
            "Elections=%d, Heartbeats=%d, Commits=%d, Validations=%d}",
            getCurrentTPS(),
            getAverageReplicationLatency(),
            getAverageCommitLatency(),
            getSuccessRate() * 100,
            getCommitSuccessRate() * 100,
            transactionsProcessed.get(),
            transactionsFailed.get(),
            electionCount.get(),
            heartbeatsSent.get(),
            commitCount.get(),
            validationCount.get()
        );
    }
}
