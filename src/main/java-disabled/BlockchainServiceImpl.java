package io.aurigraph.v11.grpc.services;

// Explicit imports from proto-generated blockchain package
import io.aurigraph.v11.grpc.blockchain.Block;
import io.aurigraph.v11.grpc.blockchain.BlockList;
import io.aurigraph.v11.grpc.blockchain.BlockQuery;
import io.aurigraph.v11.grpc.blockchain.BlockRangeQuery;
import io.aurigraph.v11.grpc.blockchain.BlockStreamRequest;
import io.aurigraph.v11.grpc.blockchain.BlockchainInfo;
import io.aurigraph.v11.grpc.blockchain.BlockchainService;
import io.aurigraph.v11.grpc.blockchain.ChainStatistics;
import io.aurigraph.v11.grpc.blockchain.InfoRequest;
import io.aurigraph.v11.grpc.blockchain.LatestBlockRequest;
import io.aurigraph.v11.grpc.blockchain.StatsRequest;
import io.aurigraph.v11.grpc.blockchain.Transaction;
import io.aurigraph.v11.grpc.blockchain.ValidatorInfo;

import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.Multi;
import jakarta.inject.Singleton;
import org.jboss.logging.Logger;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.security.MessageDigest;
import com.google.protobuf.ByteString;

/**
 * Blockchain Service Implementation
 * Sprint 13 - Block and chain management
 *
 * Provides block queries, streaming, and chain statistics.
 * Integrates with ConsensusServiceImpl for block proposals.
 */
@GrpcService
@Singleton
public class BlockchainServiceImpl implements BlockchainService {

    private static final Logger LOG = Logger.getLogger(BlockchainServiceImpl.class);

    // In-memory blockchain storage (will be replaced with persistent storage)
    private final Map<Long, Block> blocksByNumber = new ConcurrentHashMap<>();
    private final Map<String, Block> blocksByHash = new ConcurrentHashMap<>();
    private final AtomicLong latestBlockNumber = new AtomicLong(0);

    // Chain metadata
    private final String networkId = "aurigraph-v11-mainnet";
    private final String consensusAlgorithm = "HyperRAFT++";
    private final long genesisTimestamp;
    private final List<ValidatorInfo> validators = new ArrayList<>();

    // Statistics tracking
    private final AtomicLong totalTransactions = new AtomicLong(0);
    private final AtomicLong totalGasUsed = new AtomicLong(0);
    private final Set<String> uniqueAddresses = ConcurrentHashMap.newKeySet();

    public BlockchainServiceImpl() {
        this.genesisTimestamp = System.currentTimeMillis();
        LOG.info("Blockchain service initialized");

        // Initialize genesis block
        initializeGenesisBlock();

        // Initialize validator set
        initializeValidators();
    }

    /**
     * Get block by number or hash
     * Sprint 13 - Core block retrieval
     */
    @Override
    public Uni<Block> getBlock(BlockQuery request) {
        LOG.debugf("Get block request: %s", request);

        return Uni.createFrom().item(() -> {
            Block block = null;

            if (request.hasBlockNumber()) {
                long blockNumber = request.getBlockNumber();
                block = blocksByNumber.get(blockNumber);

                if (block == null) {
                    throw new IllegalArgumentException("Block not found: " + blockNumber);
                }
            } else if (request.hasBlockHash()) {
                String blockHash = request.getBlockHash();
                block = blocksByHash.get(blockHash);

                if (block == null) {
                    throw new IllegalArgumentException("Block not found: " + blockHash);
                }
            } else {
                throw new IllegalArgumentException("Must specify block_number or block_hash");
            }

            return block;
        });
    }

    /**
     * Get latest block
     * Sprint 13 - Latest block retrieval
     */
    @Override
    public Uni<Block> getLatestBlock(LatestBlockRequest request) {
        LOG.debugf("Get latest block, includeTransactions=%s", request.getIncludeTransactions());

        return Uni.createFrom().item(() -> {
            long latest = latestBlockNumber.get();
            Block block = blocksByNumber.get(latest);

            if (block == null) {
                throw new IllegalStateException("No blocks in chain");
            }

            // Optionally exclude transactions for lightweight response
            if (!request.getIncludeTransactions() && block.getTransactionsCount() > 0) {
                block = Block.newBuilder(block)
                    .clearTransactions()
                    .build();
            }

            return block;
        });
    }

    /**
     * Get block range
     * Sprint 13 - Range queries for sync
     */
    @Override
    public Uni<BlockList> getBlockRange(BlockRangeQuery request) {
        LOG.infof("Get block range: %d to %d", request.getStartBlock(), request.getEndBlock());

        return Uni.createFrom().item(() -> {
            long start = request.getStartBlock();
            long end = request.getEndBlock();
            int maxResults = request.getMaxResults() > 0 ? request.getMaxResults() : 100;

            // Validate range
            if (start > end) {
                throw new IllegalArgumentException("start_block must be <= end_block");
            }

            if (end - start > maxResults) {
                end = start + maxResults;
            }

            List<Block> blocks = new ArrayList<>();
            for (long i = start; i <= end && i <= latestBlockNumber.get(); i++) {
                Block block = blocksByNumber.get(i);
                if (block != null) {
                    // Optionally exclude transactions
                    if (!request.getIncludeTransactions() && block.getTransactionsCount() > 0) {
                        block = Block.newBuilder(block)
                            .clearTransactions()
                            .build();
                    }
                    blocks.add(block);
                }
            }

            boolean hasMore = end < latestBlockNumber.get();

            return BlockList.newBuilder()
                .addAllBlocks(blocks)
                .setTotalCount(blocks.size())
                .setHasMore(hasMore)
                .build();
        }).runSubscriptionOn(runnable -> Thread.startVirtualThread(runnable));
    }

    /**
     * Stream new blocks as they are created
     * Sprint 13 - Real-time block streaming
     */
    @Override
    public Multi<Block> streamBlocks(BlockStreamRequest request) {
        LOG.infof("Starting block stream from block %d", request.getStartFromBlock());

        long startFrom = request.getStartFromBlock();
        if (startFrom == 0) {
            startFrom = latestBlockNumber.get();
        }

        final long startBlock = startFrom;

        // Stream blocks every second
        return Multi.createFrom().ticks().every(Duration.ofSeconds(1))
            .onItem().transformToMultiAndMerge(tick -> {
                List<Block> newBlocks = new ArrayList<>();
                long latest = latestBlockNumber.get();

                // Get any new blocks since last check
                for (long i = startBlock; i <= latest; i++) {
                    Block block = blocksByNumber.get(i);
                    if (block != null) {
                        if (!request.getIncludeTransactions()) {
                            block = Block.newBuilder(block)
                                .clearTransactions()
                                .build();
                        }
                        newBlocks.add(block);
                    }
                }

                return Multi.createFrom().iterable(newBlocks);
            });
    }

    /**
     * Get blockchain information
     * Sprint 13 - Chain metadata
     */
    @Override
    public Uni<BlockchainInfo> getBlockchainInfo(InfoRequest request) {
        LOG.debug("Get blockchain info");

        return Uni.createFrom().item(() -> {
            Block latest = blocksByNumber.get(latestBlockNumber.get());

            // Calculate average block time
            long avgBlockTime = calculateAverageBlockTime();

            // Calculate current TPS
            long currentTps = calculateCurrentTPS();

            BlockchainInfo.Builder builder = BlockchainInfo.newBuilder()
                .setLatestBlockNumber(latestBlockNumber.get())
                .setLatestBlockHash(latest != null ? latest.getBlockHash() : "")
                .setTotalTransactions(totalTransactions.get())
                .setGenesisTimestamp(genesisTimestamp)
                .setNetworkId(networkId)
                .setConsensusAlgorithm(consensusAlgorithm)
                .setValidatorCount(validators.size())
                .setAvgBlockTime(avgBlockTime)
                .setCurrentTps(currentTps);

            // Optionally include validator set
            if (request.getIncludeValidatorSet()) {
                builder.addAllValidators(validators);
            }

            return builder.build();
        });
    }

    /**
     * Get chain statistics
     * Sprint 17 - Analytics integration
     */
    @Override
    public Uni<ChainStatistics> getChainStats(StatsRequest request) {
        LOG.infof("Get chain stats: blocks %d to %d", request.getFromBlock(), request.getToBlock());

        return Uni.createFrom().item(() -> {
            long fromBlock = request.getFromBlock();
            long toBlock = request.getToBlock() > 0 ? request.getToBlock() : latestBlockNumber.get();

            // Calculate statistics for range
            long totalBlocks = toBlock - fromBlock + 1;
            long totalTxs = 0;
            long totalGas = 0;
            long totalTime = 0;
            long peakTps = 0;

            Block previousBlock = null;
            for (long i = fromBlock; i <= toBlock && i <= latestBlockNumber.get(); i++) {
                Block block = blocksByNumber.get(i);
                if (block != null) {
                    totalTxs += block.getTransactionCount();
                    totalGas += block.getGasUsed();

                    // Calculate block time
                    if (previousBlock != null) {
                        long blockTime = block.getTimestamp() - previousBlock.getTimestamp();
                        totalTime += blockTime;

                        // Calculate TPS for this block
                        if (blockTime > 0) {
                            long blockTps = (block.getTransactionCount() * 1000L) / blockTime;
                            peakTps = Math.max(peakTps, blockTps);
                        }
                    }

                    previousBlock = block;
                }
            }

            long avgTxsPerBlock = totalBlocks > 0 ? totalTxs / totalBlocks : 0;
            long avgBlockTime = totalBlocks > 1 ? totalTime / (totalBlocks - 1) : 0;

            return ChainStatistics.newBuilder()
                .setTotalBlocks(totalBlocks)
                .setTotalTransactions(totalTxs)
                .setAvgTransactionsPerBlock(avgTxsPerBlock)
                .setAvgBlockTimeMs(avgBlockTime)
                .setPeakTps(peakTps)
                .setTotalGasUsed(totalGas)
                .setUniqueAddresses(uniqueAddresses.size())
                .setActiveValidators(validators.size())
                .build();
        }).runSubscriptionOn(runnable -> Thread.startVirtualThread(runnable));
    }

    // Helper methods

    private void initializeGenesisBlock() {
        Block genesis = Block.newBuilder()
            .setBlockNumber(0)
            .setBlockHash(calculateBlockHash(0, "0", genesisTimestamp))
            .setPreviousHash("0")
            .setTimestamp(genesisTimestamp)
            .setProposer("genesis")
            .setTransactionCount(0)
            .setGasUsed(0)
            .setGasLimit(30000000L)
            .setStateRoot(ByteString.copyFrom(new byte[32]))
            .setTransactionsRoot(ByteString.copyFrom(new byte[32]))
            .setReceiptsRoot(ByteString.copyFrom(new byte[32]))
            .setConfirmations(1)
            .build();

        blocksByNumber.put(0L, genesis);
        blocksByHash.put(genesis.getBlockHash(), genesis);
        latestBlockNumber.set(0);

        LOG.info("Genesis block initialized: " + genesis.getBlockHash());
    }

    private void initializeValidators() {
        // Initialize validator set (stub data)
        for (int i = 1; i <= 10; i++) {
            ValidatorInfo validator = ValidatorInfo.newBuilder()
                .setValidatorId("validator-" + i)
                .setAddress("0x" + String.format("%040d", i))
                .setStakeAmount(1000000L)
                .setIsOnline(true)
                .setBlocksProduced(0)
                .build();

            validators.add(validator);
        }

        LOG.infof("Initialized %d validators", validators.size());
    }

    private String calculateBlockHash(long blockNumber, String previousHash, long timestamp) {
        try {
            String input = blockNumber + previousHash + timestamp;
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes());

            return "0x" + bytesToHex(hash);
        } catch (Exception e) {
            LOG.error("Failed to calculate block hash", e);
            return "0x" + UUID.randomUUID().toString().replace("-", "");
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }

    private long calculateAverageBlockTime() {
        if (latestBlockNumber.get() < 2) {
            return 0;
        }

        // Calculate average over last 100 blocks
        long numBlocks = Math.min(100, latestBlockNumber.get());
        long startBlock = latestBlockNumber.get() - numBlocks;

        Block first = blocksByNumber.get(startBlock);
        Block last = blocksByNumber.get(latestBlockNumber.get());

        if (first == null || last == null) {
            return 0;
        }

        long totalTime = last.getTimestamp() - first.getTimestamp();
        return totalTime / numBlocks;
    }

    private long calculateCurrentTPS() {
        long avgBlockTime = calculateAverageBlockTime();
        if (avgBlockTime == 0) {
            return 0;
        }

        // Calculate TPS based on recent blocks
        long numBlocks = Math.min(10, latestBlockNumber.get());
        long txCount = 0;

        for (long i = latestBlockNumber.get() - numBlocks + 1; i <= latestBlockNumber.get(); i++) {
            Block block = blocksByNumber.get(i);
            if (block != null) {
                txCount += block.getTransactionCount();
            }
        }

        long avgTxPerBlock = txCount / numBlocks;
        return (avgTxPerBlock * 1000L) / avgBlockTime;
    }

    /**
     * Add a new block to the chain
     * Called by ConsensusServiceImpl when block is finalized
     * Sprint 15 - Integration with consensus
     */
    public void addBlock(Block block) {
        long blockNumber = block.getBlockNumber();

        blocksByNumber.put(blockNumber, block);
        blocksByHash.put(block.getBlockHash(), block);
        latestBlockNumber.set(blockNumber);

        // Update statistics
        totalTransactions.addAndGet(block.getTransactionCount());
        totalGasUsed.addAndGet(block.getGasUsed());

        // Track unique addresses
        for (Transaction tx : block.getTransactionsList()) {
            uniqueAddresses.add(tx.getFrom());
            uniqueAddresses.add(tx.getTo());
        }

        LOG.infof("Block %d added to chain with %d transactions",
            blockNumber, block.getTransactionCount());
    }

    /**
     * Create a new block proposal
     * Sprint 15 - Block proposal for consensus
     */
    public Block createBlockProposal(List<Transaction> transactions, String proposer) {
        long blockNumber = latestBlockNumber.get() + 1;
        long timestamp = System.currentTimeMillis();

        Block previousBlock = blocksByNumber.get(latestBlockNumber.get());
        String previousHash = previousBlock != null ? previousBlock.getBlockHash() : "0";

        // Calculate block hash
        String blockHash = calculateBlockHash(blockNumber, previousHash, timestamp);

        // Calculate gas used
        long gasUsed = transactions.stream()
            .mapToLong(Transaction::getGasUsed)
            .sum();

        // Extract transaction hashes
        List<String> txHashes = transactions.stream()
            .map(Transaction::getTxHash)
            .toList();

        return Block.newBuilder()
            .setBlockNumber(blockNumber)
            .setBlockHash(blockHash)
            .setPreviousHash(previousHash)
            .setTimestamp(timestamp)
            .setProposer(proposer)
            .setTransactionCount(transactions.size())
            .addAllTransactionHashes(txHashes)
            .addAllTransactions(transactions)
            .setGasUsed(gasUsed)
            .setGasLimit(30000000L)
            .setStateRoot(ByteString.copyFrom(new byte[32]))
            .setTransactionsRoot(ByteString.copyFrom(new byte[32]))
            .setReceiptsRoot(ByteString.copyFrom(new byte[32]))
            .setConfirmations(0)
            .build();
    }

    public long getLatestBlockNumber() {
        return latestBlockNumber.get();
    }

    public long getTotalTransactions() {
        return totalTransactions.get();
    }
}
