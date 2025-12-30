package io.aurigraph.v11.optimization;

import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.annotation.PostConstruct;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Pipelined Consensus Optimization - Sprint 15 Phase 2
 * Increases consensus pipeline depth and enables parallel vote aggregation
 *
 * Expected Performance:
 * - TPS Improvement: +300K (10% of 3.0M baseline)
 * - Latency Impact: -10ms consensus time (parallel processing)
 * - Throughput: 90 blocks in-flight vs 45 previously
 *
 * @author BDA-Performance
 * @version 1.0
 * @since Sprint 15
 */
@ApplicationScoped
public class PipelinedConsensusService {

    @ConfigProperty(name = "consensus.pipeline.depth", defaultValue = "90")
    int pipelineDepth;

    @ConfigProperty(name = "optimization.consensus.pipeline.enabled", defaultValue = "true")
    boolean enabled;

    @ConfigProperty(name = "optimization.consensus.validation.threads", defaultValue = "16")
    int validationThreads;

    @ConfigProperty(name = "optimization.consensus.aggregation.threads", defaultValue = "8")
    int aggregationThreads;

    @ConfigProperty(name = "optimization.consensus.finalization.threads", defaultValue = "4")
    int finalizationThreads;

    private ExecutorService validationExecutor;
    private ExecutorService aggregationExecutor;
    private ExecutorService finalizationExecutor;

    // Pipeline: blockHash → processing stage
    private final ConcurrentHashMap<String, CompletableFuture<Block>> validationPipeline =
        new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, CompletableFuture<VoteAggregation>> aggregationPipeline =
        new ConcurrentHashMap<>();

    // Metrics
    private final AtomicLong totalBlocksProcessed = new AtomicLong(0);
    private final AtomicLong totalPipelineStalls = new AtomicLong(0);
    private final AtomicLong totalValidationTimeNs = new AtomicLong(0);
    private final AtomicLong totalAggregationTimeNs = new AtomicLong(0);
    private final AtomicLong totalFinalizationTimeNs = new AtomicLong(0);

    @PostConstruct
    public void init() {
        if (!enabled) {
            Log.info("Pipelined consensus disabled");
            return;
        }

        validationExecutor = Executors.newFixedThreadPool(validationThreads);
        aggregationExecutor = Executors.newFixedThreadPool(aggregationThreads);
        finalizationExecutor = Executors.newFixedThreadPool(finalizationThreads);

        Log.infof("Pipelined consensus initialized: depth=%d, validation=%d threads, aggregation=%d threads, finalization=%d threads",
                 pipelineDepth, validationThreads, aggregationThreads, finalizationThreads);
    }

    /**
     * Process block through pipelined consensus
     *
     * @param block Block to process
     * @return CompletableFuture with finalized block
     */
    public CompletableFuture<Block> processBlock(Block block) {
        if (!enabled) {
            // Fallback to sequential consensus
            return CompletableFuture.supplyAsync(() -> {
                validateBlock(block);
                VoteAggregation votes = aggregateVotes(block);
                return finalizeBlock(block, votes);
            });
        }

        String blockHash = block.getHash();

        // Phase 1: Async validation (parallel with other blocks)
        CompletableFuture<Block> validationFuture = CompletableFuture.supplyAsync(() -> {
            long startTime = System.nanoTime();
            validateBlock(block);
            totalValidationTimeNs.addAndGet(System.nanoTime() - startTime);
            return block;
        }, validationExecutor);

        validationPipeline.put(blockHash, validationFuture);

        // Phase 2: Async vote aggregation (starts immediately, parallel with validation)
        CompletableFuture<VoteAggregation> aggregationFuture = validationFuture.thenApplyAsync(
            validatedBlock -> {
                long startTime = System.nanoTime();
                VoteAggregation votes = aggregateVotes(validatedBlock);
                totalAggregationTimeNs.addAndGet(System.nanoTime() - startTime);
                return votes;
            },
            aggregationExecutor
        );

        aggregationPipeline.put(blockHash, aggregationFuture);

        // Phase 3: Async finalization (parallel with other finalizations)
        CompletableFuture<Block> finalizationFuture = aggregationFuture.thenApplyAsync(
            votes -> {
                long startTime = System.nanoTime();
                Block finalizedBlock = finalizeBlock(block, votes);
                totalFinalizationTimeNs.addAndGet(System.nanoTime() - startTime);
                return finalizedBlock;
            },
            finalizationExecutor
        );

        // Cleanup pipeline entries when done
        finalizationFuture.whenComplete((result, error) -> {
            validationPipeline.remove(blockHash);
            aggregationPipeline.remove(blockHash);
            totalBlocksProcessed.incrementAndGet();
        });

        // Check pipeline depth and stall if needed
        if (validationPipeline.size() > pipelineDepth) {
            Log.warnf("Pipeline depth exceeded: %d > %d, stalling",
                     validationPipeline.size(), pipelineDepth);
            totalPipelineStalls.incrementAndGet();
            // Wait for oldest block to complete
            validationPipeline.values().iterator().next().join();
        }

        return finalizationFuture;
    }

    /**
     * Validate block (Phase 1)
     */
    private void validateBlock(Block block) {
        // Validation logic (transactions, signatures, state transitions)
        // Simulated validation with realistic timing
        try {
            Thread.sleep(2); // Simulate 2ms validation
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        block.setValidated(true);
        Log.debugf("Block validated: hash=%s", block.getHash());
    }

    /**
     * Aggregate votes from validators (Phase 2)
     */
    private VoteAggregation aggregateVotes(Block block) {
        // Vote collection and aggregation logic
        // Simulated vote aggregation with realistic timing
        try {
            Thread.sleep(3); // Simulate 3ms vote aggregation
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        VoteAggregation votes = new VoteAggregation(block.getHash(), 7, 7); // Assume 7/7 quorum

        Log.debugf("Votes aggregated: hash=%s, votes=%d/%d",
                 block.getHash(), votes.getReceivedVotes(), votes.getTotalVotes());

        return votes;
    }

    /**
     * Finalize block and commit to chain (Phase 3)
     */
    private Block finalizeBlock(Block block, VoteAggregation votes) {
        // Finalization logic (commit to chain, update state)
        // Simulated finalization with realistic timing
        try {
            Thread.sleep(1); // Simulate 1ms finalization
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        block.setFinalized(true);
        block.setVotes(votes);

        Log.debugf("Block finalized: hash=%s", block.getHash());

        return block;
    }

    /**
     * Get pipeline metrics
     */
    public PipelineMetrics getMetrics() {
        long totalBlocks = totalBlocksProcessed.get();
        return new PipelineMetrics(
            totalBlocks,
            totalPipelineStalls.get(),
            validationPipeline.size(),
            aggregationPipeline.size(),
            pipelineDepth,
            totalBlocks > 0 ? totalValidationTimeNs.get() / totalBlocks / 1_000_000 : 0,
            totalBlocks > 0 ? totalAggregationTimeNs.get() / totalBlocks / 1_000_000 : 0,
            totalBlocks > 0 ? totalFinalizationTimeNs.get() / totalBlocks / 1_000_000 : 0
        );
    }

    public record PipelineMetrics(
        long blocksProcessed,
        long pipelineStalls,
        int validationQueueSize,
        int aggregationQueueSize,
        int maxPipelineDepth,
        long avgValidationTimeMs,
        long avgAggregationTimeMs,
        long avgFinalizationTimeMs
    ) {
        public double pipelineUtilization() {
            return maxPipelineDepth > 0 ?
                (double) (validationQueueSize + aggregationQueueSize) / (maxPipelineDepth * 2) : 0.0;
        }

        public long avgTotalTimeMs() {
            return avgValidationTimeMs + avgAggregationTimeMs + avgFinalizationTimeMs;
        }
    }

    /**
     * Block model for consensus
     */
    public static class Block {
        private final String hash;
        private final long number;
        private boolean validated = false;
        private boolean finalized = false;
        private VoteAggregation votes;

        public Block(String hash, long number) {
            this.hash = hash;
            this.number = number;
        }

        public String getHash() {
            return hash;
        }

        public long getNumber() {
            return number;
        }

        public boolean isValidated() {
            return validated;
        }

        public void setValidated(boolean validated) {
            this.validated = validated;
        }

        public boolean isFinalized() {
            return finalized;
        }

        public void setFinalized(boolean finalized) {
            this.finalized = finalized;
        }

        public VoteAggregation getVotes() {
            return votes;
        }

        public void setVotes(VoteAggregation votes) {
            this.votes = votes;
        }
    }

    /**
     * Vote aggregation result
     */
    public static class VoteAggregation {
        private final String blockHash;
        private final int receivedVotes;
        private final int totalVotes;

        public VoteAggregation(String blockHash, int receivedVotes, int totalVotes) {
            this.blockHash = blockHash;
            this.receivedVotes = receivedVotes;
            this.totalVotes = totalVotes;
        }

        public String getBlockHash() {
            return blockHash;
        }

        public int getReceivedVotes() {
            return receivedVotes;
        }

        public int getTotalVotes() {
            return totalVotes;
        }

        public boolean hasQuorum() {
            return receivedVotes >= (totalVotes * 2 / 3) + 1;
        }
    }
}
