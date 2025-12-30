package io.aurigraph.v11.ai;

import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * ML-Based Transaction Scoring Model
 * Intelligently scores and orders transactions for optimal consensus performance
 * Target: 150-250K TPS improvement through better transaction ordering
 *
 * Scoring Features:
 * - Transaction size (prefer smaller for batching efficiency)
 * - Sender hotness (group by sender for cache locality)
 * - Gas price (prioritize high-value transactions)
 * - Dependencies (minimize cross-dependencies for parallel processing)
 * - Age (prefer older transactions to prevent starvation)
 *
 * @author Performance Optimization Agent
 * @version 1.0
 */
@ApplicationScoped
public class TransactionScoringModel {

    private static final Logger LOG = Logger.getLogger(TransactionScoringModel.class);

    @ConfigProperty(name = "ml.scoring.enabled", defaultValue = "true")
    boolean scoringEnabled;

    @ConfigProperty(name = "ml.scoring.batch.size", defaultValue = "25000")
    int batchSize;

    @ConfigProperty(name = "ml.scoring.learning.interval", defaultValue = "10000")
    int learningInterval;

    // ML Model weights (adaptive)
    private volatile double weightSize = 0.2;           // Prefer smaller transactions
    private volatile double weightSenderHotness = 0.25; // Prefer grouped senders
    private volatile double weightGasPrice = 0.15;      // Value prioritization
    private volatile double weightAge = 0.2;            // Fairness (prevent starvation)
    private volatile double weightDependency = 0.2;     // Parallelism

    // Performance tracking for online learning
    private final AtomicLong transactionsScored = new AtomicLong(0);
    private final AtomicInteger currentBatchNumber = new AtomicInteger(0);
    private final Map<Integer, BatchPerformance> batchMetrics = new ConcurrentHashMap<>();

    // Sender frequency tracking for cache locality
    private final Map<String, AtomicLong> senderFrequency = new ConcurrentHashMap<>();
    private final Map<String, Long> senderLastSeenMs = new ConcurrentHashMap<>();

    public static class ScoredTransaction implements Comparable<ScoredTransaction> {
        public final String txnId;
        public final String sender;
        public final long sizeBytes;
        public final BigDecimal gasPrice;
        public final long createdAtMs;
        public final Set<String> dependencies;
        public final double score;
        public final Map<String, Double> featureScores;

        public ScoredTransaction(String txnId, String sender, long sizeBytes, BigDecimal gasPrice,
                               long createdAtMs, Set<String> dependencies, double score,
                               Map<String, Double> featureScores) {
            this.txnId = txnId;
            this.sender = sender;
            this.sizeBytes = sizeBytes;
            this.gasPrice = gasPrice;
            this.createdAtMs = createdAtMs;
            this.dependencies = dependencies;
            this.score = score;
            this.featureScores = featureScores;
        }

        @Override
        public int compareTo(ScoredTransaction other) {
            // Higher score = higher priority
            return Double.compare(other.score, this.score);
        }
    }

    public static class BatchPerformance {
        public final int batchNumber;
        public final long createdAtMs;
        public final long completedAtMs;
        public final int transactionCount;
        public final long latencyMs;
        public final double throughputTps;
        public final double averageScore;

        public BatchPerformance(int batchNumber, long createdAtMs, long completedAtMs,
                               int transactionCount, long latencyMs, double throughputTps,
                               double averageScore) {
            this.batchNumber = batchNumber;
            this.createdAtMs = createdAtMs;
            this.completedAtMs = completedAtMs;
            this.transactionCount = transactionCount;
            this.latencyMs = latencyMs;
            this.throughputTps = throughputTps;
            this.averageScore = averageScore;
        }
    }

    /**
     * Score a single transaction based on ML features
     * Always calculates feature scores for testing; scoringEnabled controls production use
     */
    public ScoredTransaction scoreTransaction(String txnId, String sender, long sizeBytes,
                                              BigDecimal gasPrice, long createdAtMs,
                                              Set<String> dependencies) {
        Map<String, Double> featureScores = new HashMap<>();

        // Feature 1: Size score (normalize 0-1000 bytes)
        double sizeScore = Math.max(0, 1.0 - (sizeBytes / 1000.0));
        featureScores.put("size", sizeScore);

        // Feature 2: Sender hotness score (frequency + recency)
        long now = System.currentTimeMillis();
        long lastSeen = senderLastSeenMs.getOrDefault(sender, now);
        long senderFreq = senderFrequency.computeIfAbsent(sender, k -> new AtomicLong(0))
                .incrementAndGet();
        // Recency: higher score for recently seen senders (decay over 60s)
        double recencyScore = Math.max(0, 1.0 - Math.min(1.0, (now - lastSeen) / 60000.0));
        // Frequency: higher score for frequently appearing senders
        double frequencyScore = Math.min(1.0, senderFreq / 100.0);
        double senderHotnessScore = (recencyScore + frequencyScore) / 2.0;
        featureScores.put("senderHotness", senderHotnessScore);
        senderLastSeenMs.put(sender, now);

        // Feature 3: Gas price score (normalize to 0-1)
        double gasScore = Math.min(1.0, gasPrice.doubleValue() / 1000.0);
        featureScores.put("gasPrice", gasScore);

        // Feature 4: Age score (prefer older transactions, prevent starvation)
        long ageMs = now - createdAtMs;
        double ageScore = Math.min(1.0, ageMs / 5000.0); // Max priority after 5 seconds
        featureScores.put("age", ageScore);

        // Feature 5: Dependency score (fewer dependencies = higher score)
        double dependencyScore = Math.max(0, 1.0 - (dependencies.size() / 10.0));
        featureScores.put("dependency", dependencyScore);

        // Weighted score combination (sum of weights is 1.0, so result is already 0-1)
        double totalScore = (weightSize * sizeScore +
                weightSenderHotness * senderHotnessScore +
                weightGasPrice * gasScore +
                weightAge * ageScore +
                weightDependency * dependencyScore);

        // Ensure total score is clamped to 0-1 range
        totalScore = Math.max(0, Math.min(1.0, totalScore));

        transactionsScored.incrementAndGet();

        return new ScoredTransaction(txnId, sender, sizeBytes, gasPrice, createdAtMs,
                dependencies, totalScore, featureScores);
    }

    /**
     * Score and order a batch of transactions for optimal processing
     * Always calculates scores; scoringEnabled controls whether batch metrics are tracked
     */
    public List<ScoredTransaction> scoreAndOrderBatch(List<TransactionData> transactions) {
        long batchStartMs = System.currentTimeMillis();
        int batchNum = currentBatchNumber.incrementAndGet();

        // Score all transactions (always calculates features)
        List<ScoredTransaction> scoredTxns = transactions.stream()
                .map(t -> scoreTransaction(t.txnId, t.sender, t.sizeBytes, t.gasPrice,
                        t.createdAtMs, t.dependencies))
                .collect(Collectors.toList());

        // Sort by score (highest first)
        List<ScoredTransaction> ordered = new ArrayList<>(scoredTxns);
        Collections.sort(ordered);

        // Apply grouping optimization: cluster same-sender transactions (only if enabled in production)
        if (scoringEnabled) {
            ordered = optimizeGrouping(ordered);
        }

        // Track batch performance
        long batchCompleteMs = System.currentTimeMillis();
        long latencyMs = batchCompleteMs - batchStartMs;
        double avgScore = ordered.stream()
                .collect(Collectors.averagingDouble(t -> t.score));
        double throughputTps = (transactions.size() * 1000.0) / Math.max(1, latencyMs);

        BatchPerformance perf = new BatchPerformance(batchNum, batchStartMs, batchCompleteMs,
                transactions.size(), latencyMs, throughputTps, avgScore);
        batchMetrics.put(batchNum, perf);

        // Trigger learning if interval reached (only if enabled in production)
        if (scoringEnabled && batchNum % learningInterval == 0) {
            updateModelWeights();
        }

        LOG.debugf("Batch %d: scored %d txns, latency=%dms, throughput=%.0f TPS, avg_score=%.3f",
                batchNum, transactions.size(), latencyMs, throughputTps, avgScore);

        return ordered;
    }

    /**
     * Optimize grouping to maximize cache locality
     * Group transactions from same sender together when possible
     */
    private List<ScoredTransaction> optimizeGrouping(List<ScoredTransaction> transactions) {
        // Group by sender while maintaining score order
        Map<String, List<ScoredTransaction>> grouped = new LinkedHashMap<>();
        List<ScoredTransaction> ungrouped = new ArrayList<>();

        for (ScoredTransaction txn : transactions) {
            if (senderFrequency.getOrDefault(txn.sender, new AtomicLong(0)).get() > 10) {
                // Hot sender - group together
                grouped.computeIfAbsent(txn.sender, k -> new ArrayList<>()).add(txn);
            } else {
                ungrouped.add(txn);
            }
        }

        // Reconstruct list: sorted groups first, then ungrouped
        List<ScoredTransaction> result = new ArrayList<>();
        grouped.values().forEach(result::addAll);
        result.addAll(ungrouped);

        return result;
    }

    /**
     * Online learning: update model weights based on batch performance
     */
    private void updateModelWeights() {
        if (batchMetrics.size() < 2) return;

        // Get recent batches for comparison
        int currentBatch = currentBatchNumber.get();
        BatchPerformance recent = batchMetrics.get(currentBatch);
        BatchPerformance previous = batchMetrics.get(currentBatch - learningInterval);

        if (recent == null || previous == null) return;

        double performanceGain = (recent.throughputTps - previous.throughputTps) / previous.throughputTps;
        double scoreImprovement = recent.averageScore - previous.averageScore;

        LOG.infof("ML Learning: performance gain=%.2f%%, score improvement=%.3f",
                performanceGain * 100, scoreImprovement);

        // Adaptive weight adjustment (simple reinforcement)
        // If performance improved, slightly increase weights of high-scoring features
        if (performanceGain > 0.01) { // 1% improvement threshold
            double adjustment = 0.01 * performanceGain;
            weightSenderHotness = Math.min(0.35, weightSenderHotness + adjustment);
            weightSize = Math.max(0.1, weightSize - adjustment / 2);
            LOG.infof("Weights updated: senderHotness=%.3f, size=%.3f",
                    weightSenderHotness, weightSize);
        }
    }

    /**
     * Get current model weights
     */
    public Map<String, Double> getModelWeights() {
        return Map.ofEntries(
                Map.entry("size", weightSize),
                Map.entry("senderHotness", weightSenderHotness),
                Map.entry("gasPrice", weightGasPrice),
                Map.entry("age", weightAge),
                Map.entry("dependency", weightDependency)
        );
    }

    /**
     * Get statistics on model performance
     */
    public Map<String, Object> getStatistics() {
        int batchNum = currentBatchNumber.get();
        BatchPerformance latest = batchMetrics.get(batchNum);

        Map<String, Object> stats = new HashMap<>();
        stats.put("transactionsScored", transactionsScored.get());
        stats.put("batchesProcessed", batchNum);
        stats.put("sendersCached", senderFrequency.size());

        if (latest != null) {
            stats.put("latestBatchLatencyMs", latest.latencyMs);
            stats.put("latestBatchThroughputTps", latest.throughputTps);
            stats.put("latestBatchAvgScore", latest.averageScore);
        }

        return stats;
    }

    /**
     * Transaction data structure for scoring
     */
    public static class TransactionData {
        public final String txnId;
        public final String sender;
        public final long sizeBytes;
        public final BigDecimal gasPrice;
        public final long createdAtMs;
        public final Set<String> dependencies;

        public TransactionData(String txnId, String sender, long sizeBytes,
                              BigDecimal gasPrice, long createdAtMs, Set<String> dependencies) {
            this.txnId = txnId;
            this.sender = sender;
            this.sizeBytes = sizeBytes;
            this.gasPrice = gasPrice;
            this.createdAtMs = createdAtMs;
            this.dependencies = dependencies == null ? Set.of() : dependencies;
        }
    }
}
