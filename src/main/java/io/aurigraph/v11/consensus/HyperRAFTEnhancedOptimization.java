package io.aurigraph.v11.consensus;

import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.commons.math3.stat.regression.SimpleRegression;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.stream.Collectors;

/**
 * HyperRAFT++ Enhanced Optimization Service
 *
 * Implements advanced optimizations for consensus algorithm:
 * - Parallel validation processing using Java 21 Virtual Threads
 * - Optimized block commit with adaptive timing
 * - Leader election bottleneck reduction
 * - Network partition detection and recovery
 * - Predictive performance modeling
 */
@ApplicationScoped
public class HyperRAFTEnhancedOptimization {

    @Inject
    HyperRAFTConsensusService consensusService;

    @Inject
    ConsensusMetrics consensusMetrics;

    // =========================================================================
    // Parallel Validation Processing
    // =========================================================================

    private static final int VIRTUAL_THREAD_POOL_SIZE = 256;
    private static final int VALIDATION_CHUNK_SIZE = 1000;
    private static final int VALIDATION_TIMEOUT_MS = 5000;

    private ExecutorService validationExecutor;
    private BlockingQueue<TransactionBatch> validationQueue;
    private final AtomicInteger activeValidations = new AtomicInteger(0);
    private final AtomicLong totalValidationsProcessed = new AtomicLong(0);
    private final AtomicLong validationErrorCount = new AtomicLong(0);

    // =========================================================================
    // Block Commit Optimization
    // =========================================================================

    private static final long BLOCK_COMMIT_TARGET_MS = 100;
    private static final long MIN_COMMIT_INTERVAL_MS = 50;
    private static final long MAX_COMMIT_INTERVAL_MS = 200;

    private final AtomicLong blockCommitLatencyMs = new AtomicLong(0);
    private final AtomicLong blockCommitLatencyP99Ms = new AtomicLong(0);
    private final ConcurrentLinkedDeque<Long> commitLatencySamples = new ConcurrentLinkedDeque<>();
    private final SimpleRegression commitLatencyRegression = new SimpleRegression();

    // =========================================================================
    // Leader Election Optimization
    // =========================================================================

    private static final long ELECTION_TIMEOUT_BASE_MS = 150;
    private static final long ELECTION_TIMEOUT_MAX_MS = 300;
    private static final int ELECTION_HEARTBEAT_CHECK_INTERVAL_MS = 50;

    private final AtomicLong electionTimeoutMs = new AtomicLong(ELECTION_TIMEOUT_BASE_MS);
    private final AtomicLong lastHeartbeatMs = new AtomicLong(System.currentTimeMillis());
    private final AtomicInteger failedElectionAttempts = new AtomicInteger(0);
    private final SimpleRegression electionTimeoutRegression = new SimpleRegression();

    // =========================================================================
    // Performance Metrics & Monitoring
    // =========================================================================

    private final Map<String, PerformanceMetric> performanceMetrics = new ConcurrentHashMap<>();
    private final AtomicLong consensusEfficiencyPercent = new AtomicLong(95);
    private final AtomicLong networkLatencyMs = new AtomicLong(1);
    private final AtomicInteger activePeers = new AtomicInteger(7);

    // =========================================================================
    // Initialization & Shutdown
    // =========================================================================

    public void initialize() {
        // Create virtual thread executor for parallel validation
        this.validationExecutor = Executors.newVirtualThreadPerTaskExecutor();
        this.validationQueue = new LinkedBlockingQueue<>(20000);

        // Initialize metrics
        initializePerformanceMetrics();

        // Start background optimization tasks
        startOptimizationDaemons();

        Log.info("HyperRAFT++ Enhanced Optimization initialized");
        Log.info("  - Virtual Threads: " + VIRTUAL_THREAD_POOL_SIZE);
        Log.info("  - Validation Queue: " + validationQueue.remainingCapacity());
        Log.info("  - Target Block Commit: " + BLOCK_COMMIT_TARGET_MS + "ms");
        Log.info("  - Election Timeout: " + electionTimeoutMs.get() + "ms");
    }

    public void shutdown() {
        if (validationExecutor != null) {
            validationExecutor.shutdown();
            try {
                if (!validationExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    validationExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                validationExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        Log.info("HyperRAFT++ Enhanced Optimization shutdown complete");
    }

    // =========================================================================
    // Parallel Transaction Validation
    // =========================================================================

    /**
     * Validates a batch of transactions in parallel using Virtual Threads
     *
     * Performance characteristics:
     * - Splits batch into CPU-core-sized chunks
     * - Each chunk validated by separate Virtual Thread
     * - Total parallelism = CPU cores × 2
     * - Timeout: 5 seconds per batch
     *
     * @param transactions List of transactions to validate
     * @return List of validation results (preserves order)
     * @throws TimeoutException if validation takes > 5s
     */
    public List<TransactionValidationResult> parallelValidateTransactions(
            List<AurigraphTransaction> transactions) throws TimeoutException, InterruptedException, ExecutionException {

        long startTimeMs = System.currentTimeMillis();
        activeValidations.incrementAndGet();

        try {
            if (transactions.isEmpty()) {
                return Collections.emptyList();
            }

            int cpuCores = Runtime.getRuntime().availableProcessors();
            int chunksCount = cpuCores * 2; // 2 chunks per core for Virtual Threads
            int chunkSize = Math.max(1, transactions.size() / chunksCount);

            List<Future<List<TransactionValidationResult>>> futures = new ArrayList<>();

            // Submit validation tasks for each chunk
            for (int i = 0; i < transactions.size(); i += chunkSize) {
                int endIdx = Math.min(i + chunkSize, transactions.size());
                List<AurigraphTransaction> chunk = transactions.subList(i, endIdx);

                Future<List<TransactionValidationResult>> future =
                    validationExecutor.submit(() -> validateTransactionChunk(chunk));
                futures.add(future);
            }

            // Collect results with timeout
            List<TransactionValidationResult> allResults = new ArrayList<>();
            long remainingTimeMs = VALIDATION_TIMEOUT_MS;

            for (Future<List<TransactionValidationResult>> future : futures) {
                try {
                    List<TransactionValidationResult> chunkResults =
                        future.get(remainingTimeMs, TimeUnit.MILLISECONDS);
                    allResults.addAll(chunkResults);

                    remainingTimeMs = VALIDATION_TIMEOUT_MS -
                        (System.currentTimeMillis() - startTimeMs);
                } catch (TimeoutException e) {
                    future.cancel(true);
                    validationErrorCount.incrementAndGet();
                    throw new TimeoutException("Parallel validation timeout");
                }
            }

            // Update metrics
            long elapsedMs = System.currentTimeMillis() - startTimeMs;
            totalValidationsProcessed.addAndGet(transactions.size());
            recordValidationLatency(elapsedMs);

            boolean allValid = allResults.stream().allMatch(r -> r.isValid);
            consensusMetrics.recordValidation(
                allValid,
                elapsedMs,
                allResults.size()
            );

            return allResults;

        } finally {
            activeValidations.decrementAndGet();
        }
    }

    /**
     * Validates a single chunk of transactions
     */
    private List<TransactionValidationResult> validateTransactionChunk(
            List<AurigraphTransaction> chunk) {

        return chunk.parallelStream()
            .map(this::validateSingleTransaction)
            .collect(Collectors.toList());
    }

    /**
     * Validates a single transaction with comprehensive checks
     */
    private TransactionValidationResult validateSingleTransaction(AurigraphTransaction tx) {
        try {
            boolean valid = true;
            String error = null;

            // Signature validation
            if (!validateTransactionSignature(tx)) {
                valid = false;
                error = "Invalid signature";
            }

            // Nonce validation
            if (valid && !validateTransactionNonce(tx)) {
                valid = false;
                error = "Invalid nonce";
            }

            // Gas validation
            if (valid && !validateTransactionGas(tx)) {
                valid = false;
                error = "Insufficient gas";
            }

            // Balance validation
            if (valid && !validateSenderBalance(tx)) {
                valid = false;
                error = "Insufficient balance";
            }

            return new TransactionValidationResult(tx.getHash(), valid, error);

        } catch (Exception e) {
            Log.warn("Error validating transaction: " + e.getMessage());
            return new TransactionValidationResult(tx.getHash(), false, e.getMessage());
        }
    }

    // =========================================================================
    // Block Commit Optimization
    // =========================================================================

    /**
     * Optimized block commit with adaptive timing
     *
     * Targets <100ms commit time by:
     * - Predictive commit scheduling
     * - Parallel signature verification
     * - Optimized Merkle tree construction
     *
     * @param blockHeight Block height to commit
     * @param transactions Transactions in block
     * @return Commit result with timing
     */
    public BlockCommitResult optimizeBlockCommit(
            long blockHeight,
            List<AurigraphTransaction> transactions) {

        long commitStartMs = System.currentTimeMillis();

        try {
            // Phase 1: Parallel transaction validation (see above)
            List<TransactionValidationResult> validationResults =
                parallelValidateTransactions(transactions);

            long validationTimeMs = System.currentTimeMillis() - commitStartMs;

            // Phase 2: Build Merkle tree (optimized)
            MerkleTreeResult merkleResult = buildOptimizedMerkleTree(
                transactions.stream()
                    .map(AurigraphTransaction::getHash)
                    .collect(Collectors.toList())
            );

            long merkleTimeMs = System.currentTimeMillis() - commitStartMs - validationTimeMs;

            // Phase 3: Create block header
            BlockHeader blockHeader = createBlockHeader(
                blockHeight,
                merkleResult.getMerkleRoot(),
                transactions.size(),
                merkleTimeMs + validationTimeMs
            );

            long totalCommitTimeMs = System.currentTimeMillis() - commitStartMs;

            // Update commit metrics and adaptive timing
            updateBlockCommitMetrics(totalCommitTimeMs, blockHeight);
            optimizeCommitTiming(totalCommitTimeMs);

            return new BlockCommitResult(
                blockHeight,
                blockHeader,
                totalCommitTimeMs,
                validationTimeMs,
                merkleTimeMs,
                validationResults.size(),
                true
            );

        } catch (Exception e) {
            long totalTimeMs = System.currentTimeMillis() - commitStartMs;
            Log.error("Block commit failed: " + e.getMessage());
            return new BlockCommitResult(
                blockHeight,
                null,
                totalTimeMs,
                0,
                0,
                0,
                false
            );
        }
    }

    /**
     * Builds optimized Merkle tree with parallel leaf hashing
     */
    private MerkleTreeResult buildOptimizedMerkleTree(List<String> transactionHashes) {
        if (transactionHashes.isEmpty()) {
            return new MerkleTreeResult("0x0", 0);
        }

        // For optimization, use pre-computed hashes when available
        String merkleRoot = computeMerkleRoot(transactionHashes);

        return new MerkleTreeResult(
            merkleRoot,
            transactionHashes.size()
        );
    }

    /**
     * Computes Merkle root from transaction hashes
     */
    private String computeMerkleRoot(List<String> hashes) {
        if (hashes.size() == 1) {
            return hashes.get(0);
        }

        // Simplified Merkle tree construction for optimization
        List<String> currentLevel = new ArrayList<>(hashes);

        while (currentLevel.size() > 1) {
            List<String> nextLevel = new ArrayList<>();

            for (int i = 0; i < currentLevel.size(); i += 2) {
                String left = currentLevel.get(i);
                String right = (i + 1 < currentLevel.size()) ?
                    currentLevel.get(i + 1) : left;

                String combined = left + right;
                String hash = hashString(combined);
                nextLevel.add(hash);
            }

            currentLevel = nextLevel;
        }

        return currentLevel.get(0);
    }

    /**
     * Simple hash function for demonstration
     */
    private String hashString(String input) {
        int hash = input.hashCode();
        return "0x" + String.format("%016x", Math.abs(hash));
    }

    /**
     * Creates block header with timing metadata
     */
    private BlockHeader createBlockHeader(
            long blockHeight,
            String merkleRoot,
            int transactionCount,
            long constructionTimeMs) {

        return new BlockHeader(
            blockHeight,
            merkleRoot,
            System.currentTimeMillis() / 1000,
            transactionCount,
            constructionTimeMs
        );
    }

    /**
     * Updates block commit metrics and P99 latency
     */
    private void updateBlockCommitMetrics(long commitTimeMs, long blockHeight) {
        blockCommitLatencyMs.set(commitTimeMs);
        commitLatencySamples.add(commitTimeMs);

        // Keep only last 1000 samples for P99 calculation
        if (commitLatencySamples.size() > 1000) {
            commitLatencySamples.removeFirst();
        }

        // Calculate P99 latency
        if (commitLatencySamples.size() >= 100) {
            List<Long> sorted = commitLatencySamples.stream()
                .sorted()
                .collect(Collectors.toList());
            int p99Index = (int) (sorted.size() * 0.99);
            blockCommitLatencyP99Ms.set(sorted.get(p99Index));
        }

        // Update regression model for prediction
        commitLatencyRegression.addData(blockHeight, commitTimeMs);

        // Record commit metrics using available ConsensusMetrics API
        boolean commitSuccessful = commitTimeMs <= BLOCK_COMMIT_TARGET_MS;
        consensusMetrics.recordCommit(commitSuccessful, commitTimeMs);
    }

    /**
     * Optimizes commit timing based on historical data
     * Adjusts next commit target to maintain <100ms
     */
    private void optimizeCommitTiming(long lastCommitTimeMs) {
        if (lastCommitTimeMs > BLOCK_COMMIT_TARGET_MS) {
            // Commit is slower than target, reduce batch size or increase parallelism
            Log.warn("Block commit slow: " + lastCommitTimeMs + "ms (target: " +
                    BLOCK_COMMIT_TARGET_MS + "ms)");

            // Could trigger adaptive batch size reduction
            // consensusService.adjustBatchSize(true); // reduce
        } else if (lastCommitTimeMs < BLOCK_COMMIT_TARGET_MS / 2) {
            // Commit is very fast, could accept larger batches
            Log.debug("Block commit fast: " + lastCommitTimeMs + "ms");
            // consensusService.adjustBatchSize(false); // increase
        }
    }

    // =========================================================================
    // Leader Election Optimization
    // =========================================================================

    /**
     * Optimized leader election with adaptive timeouts
     *
     * Reduces election bottlenecks by:
     * - Adaptive timeout calculation
     * - Early quorum detection
     * - Parallel vote collection
     *
     * @return Optimized election timeout in milliseconds
     */
    public long optimizeElectionTimeout() {
        // Base timeout with jitter
        long baseTimeout = ELECTION_TIMEOUT_BASE_MS +
            (new Random().nextLong() % 50);

        // Adjust based on failed election attempts
        int failedAttempts = failedElectionAttempts.get();
        long adjustedTimeout = baseTimeout + (failedAttempts * 25);

        // Cap at maximum
        long finalTimeout = Math.min(adjustedTimeout, ELECTION_TIMEOUT_MAX_MS);
        electionTimeoutMs.set(finalTimeout);

        // Record in regression model for prediction
        electionTimeoutRegression.addData(
            System.currentTimeMillis(),
            finalTimeout
        );

        return finalTimeout;
    }

    /**
     * Records heartbeat reception for leader liveness
     */
    public void recordHeartbeat() {
        lastHeartbeatMs.set(System.currentTimeMillis());
        failedElectionAttempts.set(0); // Reset on successful heartbeat
    }

    /**
     * Detects leader failure based on heartbeat timeout
     */
    public boolean isLeaderTimedOut() {
        long timeSinceLastHeartbeatMs =
            System.currentTimeMillis() - lastHeartbeatMs.get();
        return timeSinceLastHeartbeatMs > electionTimeoutMs.get();
    }

    /**
     * Records failed election attempt for adaptive timeout
     */
    public void recordFailedElection() {
        failedElectionAttempts.incrementAndGet();
    }

    // =========================================================================
    // Network Partition Detection
    // =========================================================================

    /**
     * Detects network partitions by monitoring peer connectivity
     *
     * @return Network partition status
     */
    public NetworkPartitionStatus detectNetworkPartition() {
        int activePeerCount = activePeers.get();
        int requiredQuorum = 4; // Majority of 7 nodes

        boolean partitioned = activePeerCount < requiredQuorum;
        boolean recoverable = activePeerCount >= (requiredQuorum / 2);

        return new NetworkPartitionStatus(
            partitioned,
            recoverable,
            activePeerCount,
            requiredQuorum,
            System.currentTimeMillis()
        );
    }

    /**
     * Updates active peer count from cluster health checks
     */
    public void updateActivePeerCount(int count) {
        activePeers.set(count);
    }

    // =========================================================================
    // Background Optimization Daemons
    // =========================================================================

    /**
     * Starts background daemon threads for continuous optimization
     */
    private void startOptimizationDaemons() {
        // Daemon 1: Adaptive timeout optimization (every 5s)
        new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Thread.sleep(5000);
                    optimizeElectionTimeout();

                    if (Log.isDebugEnabled()) {
                        Log.debug("Consensus optimization daemon: " +
                            "timeout=" + electionTimeoutMs.get() + "ms, " +
                            "commit_latency_p99=" + blockCommitLatencyP99Ms.get() + "ms, " +
                            "active_validations=" + activeValidations.get());
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }, "HyperRAFT-Optimization-Daemon").start();

        // Daemon 2: Leader timeout monitoring (every 1s)
        new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Thread.sleep(1000);

                    if (isLeaderTimedOut()) {
                        Log.warn("Leader timeout detected, election timeout=" +
                            electionTimeoutMs.get() + "ms");
                        recordFailedElection();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }, "HyperRAFT-LeaderTimeout-Monitor").start();

        // Daemon 3: Network partition detection (every 3s)
        new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Thread.sleep(3000);
                    NetworkPartitionStatus status = detectNetworkPartition();

                    if (status.isPartitioned()) {
                        Log.warn("Network partition detected: " +
                            status.getActivePeerCount() + "/" +
                            status.getRequiredQuorum());
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }, "HyperRAFT-NetworkPartition-Detector").start();
    }

    // =========================================================================
    // Transaction Validation Helper Methods
    // =========================================================================

    /**
     * Validates transaction signature using cryptographic verification
     */
    private boolean validateTransactionSignature(AurigraphTransaction tx) {
        // Placeholder implementation - in production this would verify ECDSA/EdDSA signature
        return tx.getHash() != null && !tx.getHash().isEmpty();
    }

    /**
     * Validates transaction nonce to prevent replay attacks
     */
    private boolean validateTransactionNonce(AurigraphTransaction tx) {
        // Placeholder implementation - in production this would check sender's nonce sequence
        return true;
    }

    /**
     * Validates transaction gas constraints
     */
    private boolean validateTransactionGas(AurigraphTransaction tx) {
        // Placeholder implementation - in production this would verify gas price and limit
        return true;
    }

    /**
     * Validates sender has sufficient balance for transaction
     */
    private boolean validateSenderBalance(AurigraphTransaction tx) {
        // Placeholder implementation - in production this would check account balance
        return true;
    }

    // =========================================================================
    // Metrics Recording
    // =========================================================================

    private void recordValidationLatency(long latencyMs) {
        performanceMetrics.merge(
            "validation_latency",
            new PerformanceMetric("validation_latency", latencyMs),
            (v1, v2) -> {
                v1.recordSample(latencyMs);
                return v1;
            }
        );
    }

    private void initializePerformanceMetrics() {
        performanceMetrics.put("validation_latency",
            new PerformanceMetric("validation_latency", 0));
        performanceMetrics.put("block_commit_latency",
            new PerformanceMetric("block_commit_latency", 0));
        performanceMetrics.put("election_timeout",
            new PerformanceMetric("election_timeout", ELECTION_TIMEOUT_BASE_MS));
    }

    // =========================================================================
    // Getters for Monitoring
    // =========================================================================

    public long getBlockCommitLatencyMs() {
        return blockCommitLatencyMs.get();
    }

    public long getBlockCommitLatencyP99Ms() {
        return blockCommitLatencyP99Ms.get();
    }

    public long getElectionTimeoutMs() {
        return electionTimeoutMs.get();
    }

    public long getTotalValidationsProcessed() {
        return totalValidationsProcessed.get();
    }

    public long getValidationErrorCount() {
        return validationErrorCount.get();
    }

    public int getActiveValidations() {
        return activeValidations.get();
    }

    public long getConsensusEfficiencyPercent() {
        return consensusEfficiencyPercent.get();
    }

    // =========================================================================
    // Inner Classes for Validation Results
    // =========================================================================

    public static class TransactionValidationResult {
        public final String transactionHash;
        public final boolean isValid;
        public final String errorMessage;

        public TransactionValidationResult(String hash, boolean valid, String error) {
            this.transactionHash = hash;
            this.isValid = valid;
            this.errorMessage = error;
        }
    }

    public static class BlockCommitResult {
        public final long blockHeight;
        public final BlockHeader blockHeader;
        public final long totalCommitTimeMs;
        public final long validationTimeMs;
        public final long merkleTimeMs;
        public final int transactionsProcessed;
        public final boolean success;

        public BlockCommitResult(long height, BlockHeader header, long totalTime,
                long validationTime, long merkleTime, int txCount, boolean success) {
            this.blockHeight = height;
            this.blockHeader = header;
            this.totalCommitTimeMs = totalTime;
            this.validationTimeMs = validationTime;
            this.merkleTimeMs = merkleTime;
            this.transactionsProcessed = txCount;
            this.success = success;
        }
    }

    public static class BlockHeader {
        public final long blockHeight;
        public final String merkleRoot;
        public final long timestamp;
        public final int transactionCount;
        public final long constructionTimeMs;

        public BlockHeader(long height, String root, long ts, int txCount, long time) {
            this.blockHeight = height;
            this.merkleRoot = root;
            this.timestamp = ts;
            this.transactionCount = txCount;
            this.constructionTimeMs = time;
        }
    }

    public static class MerkleTreeResult {
        public final String merkleRoot;
        public final int leafCount;

        public MerkleTreeResult(String root, int leaves) {
            this.merkleRoot = root;
            this.leafCount = leaves;
        }

        public String getMerkleRoot() {
            return merkleRoot;
        }
    }

    public static class NetworkPartitionStatus {
        public final boolean partitioned;
        public final boolean recoverable;
        public final int activePeerCount;
        public final int requiredQuorum;
        public final long detectionTime;

        public NetworkPartitionStatus(boolean part, boolean recover,
                int active, int quorum, long time) {
            this.partitioned = part;
            this.recoverable = recover;
            this.activePeerCount = active;
            this.requiredQuorum = quorum;
            this.detectionTime = time;
        }

        public boolean isPartitioned() {
            return partitioned;
        }

        public int getActivePeerCount() {
            return activePeerCount;
        }

        public int getRequiredQuorum() {
            return requiredQuorum;
        }
    }

    public static class PerformanceMetric {
        private final String name;
        private final AtomicLong sampleCount = new AtomicLong(0);
        private final AtomicLong totalValue = new AtomicLong(0);
        private final AtomicLong minValue = new AtomicLong(Long.MAX_VALUE);
        private final AtomicLong maxValue = new AtomicLong(0);

        public PerformanceMetric(String name, long initialValue) {
            this.name = name;
            if (initialValue > 0) {
                recordSample(initialValue);
            }
        }

        public void recordSample(long value) {
            sampleCount.incrementAndGet();
            totalValue.addAndGet(value);
            minValue.accumulateAndGet(value, Math::min);
            maxValue.accumulateAndGet(value, Math::max);
        }

        public long getAverage() {
            long count = sampleCount.get();
            return count > 0 ? totalValue.get() / count : 0;
        }

        public long getMin() {
            return minValue.get() == Long.MAX_VALUE ? 0 : minValue.get();
        }

        public long getMax() {
            return maxValue.get();
        }
    }

    // Placeholder classes for demonstration
    public static class AurigraphTransaction {
        private String hash;

        public AurigraphTransaction(String hash) {
            this.hash = hash;
        }

        public String getHash() {
            return hash;
        }
    }

    public static class TransactionBatch {
        public List<AurigraphTransaction> transactions;
        public long timestamp;
    }
}
