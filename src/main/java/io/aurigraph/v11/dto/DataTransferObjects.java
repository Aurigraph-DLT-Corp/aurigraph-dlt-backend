package io.aurigraph.v11.dto;

import java.util.List;

public class DataTransferObjects {

    /**
     * Network statistics (AV11-360)
     */
    public record NetworkStatistics(
        long currentTps,
        long peakTps,
        long totalTransactions,
        int connectedNodes,
        int totalValidators,
        double networkHealth, // 0.0 to 1.0
        String networkState,
        long uptimeSeconds,
        long timestamp
    ) {}

    /**
     * Information about a single validator (AV11-361)
     */
    public record ValidatorInfo(
        String validatorId,
        String nodeId,
        String address,
        long uptime, // in seconds
        double performanceScore, // 0.0 to 1.0
        long totalBlocksProposed,
        long totalSignatures,
        String status, // e.g., ACTIVE, JAILED
        long timestamp
    ) {}

    /**
     * List of active validators (AV11-361)
     */
    public record ValidatorList(
        int activeValidators,
        int totalValidators,
        List<ValidatorInfo> validators,
        long timestamp
    ) {}

    /**
     * Real-time consensus state (AV11-362)
     */
    public record ConsensusState(
        String leaderId,
        long term,
        long commitIndex,
        long lastApplied,
        String state, // e.g., LEADER, FOLLOWER, CANDIDATE
        int clusterSize,
        long timestamp
    ) {}

    /**
     * Composite data for the analytics dashboard (AV11-363)
     */
    public record AnalyticsDashboard(
        NetworkStatistics networkStats,
        ConsensusState consensusState,
        io.aurigraph.v11.TransactionService.EnhancedProcessingStats transactionStats,
        long activeAlerts,
        List<String> recentEvents,
        long timestamp
    ) {}

    /**
     * Detailed information about a single transaction (AV11-364)
     */
    public record TransactionInfo(
        String transactionHash,
        long blockNumber,
        String fromAddress,
        String toAddress,
        double amount,
        long timestamp,
        String status, // e.g., CONFIRMED, PENDING, FAILED
        int confirmations
    ) {}

    /**
     * A list of transactions matching search criteria (AV11-364)
     */
    public record TransactionSearchResult(
        List<TransactionInfo> transactions,
        int resultCount
    ) {}

    /**
     * Snapshot of the mempool state (AV11-365)
     */
    public record MempoolState(
        int pendingTransactions,
        double totalFees,
        long sizeBytes,
        List<String> topTransactions
    ) {}

    /**
     * Information about a specific wallet (AV11-366)
     */
    public record WalletInfo(
        String address,
        double balance,
        long transactionCount,
        List<TransactionInfo> recentTransactions
    ) {}

    /**
     * Request to submit a new transaction (AV11-367)
     */
    public record SubmitTransactionRequest(
        String fromAddress,
        String toAddress,
        double amount,
        String privateKey
    ) {}

    /**
     * Response after submitting a transaction (AV11-367)
     */
    public record SubmitTransactionResponse(
        String transactionHash,
        String status,
        String message
    ) {}

    /**
     * A list of recent blocks (AV11-369)
     */
    public record BlockList(
        List<BlockInfo> blocks
    ) {}

    /**
     * Information about a specific token (AV11-370)
     */
    public record TokenInfo(
        String tokenId,
        String name,
        String symbol,
        long totalSupply,
        String contractAddress
    ) {}

    /**
     * Information about a governance proposal (AV11-371)
     */
    public record GovernanceProposal(
        String proposalId,
        String title,
        String description,
        String status,
        long submitTimestamp,
        long votingEndTimestamp
    ) {}

    /**
     * A list of governance proposals (AV11-371)
     */
    public record GovernanceProposalList(
        List<GovernanceProposal> proposals
    ) {}

    /**
     * Request to submit a vote on a governance proposal (AV11-372)
     */
    public record SubmitVoteRequest(
        String voterAddress,
        boolean inFavor,
        String signature
    ) {}

    /**
     * Response after submitting a vote (AV11-372)
     */
    public record SubmitVoteResponse(
        String proposalId,
        String voterAddress,
        boolean inFavor,
        String status,
        String message
    ) {}

    /**
     * Block information (AV11-367)
     */
    public record BlockInfo(
        long blockNumber,
        String blockHash,
        String parentHash,
        long timestamp,
        int transactionCount,
        String validator,
        double blockTime,
        String consensusAlgorithm,
        boolean finalized
    ) {}

    /**
     * Blockchain statistics (AV11-367)
     */
    public record BlockchainStats(
        long totalBlocks,
        long totalTransactions,
        double currentTPS,
        double averageBlockTime,
        long averageTransactionsPerBlock,
        int activeValidators,
        int totalNodes,
        String networkHashRate,
        double networkLatency,
        String consensusAlgorithm,
        String networkStatus,
        double healthScore,
        long timestamp
    ) {}

    /**
     * Consensus performance metrics (AV11-368)
     */
    public record ConsensusMetrics(
        String nodeState,
        long currentTerm,
        long commitIndex,
        long lastApplied,
        int votesReceived,
        int totalVotesNeeded,
        String leaderNodeId,
        double averageConsensusLatency,
        long consensusRoundsCompleted,
        double successRate,
        String algorithm,
        long timestamp
    ) {}

    /**
     * Cryptography performance metrics (AV11-368)
     */
    public record CryptoMetrics(
        boolean enabled,
        String algorithm,
        int securityLevel,
        long operationsPerSecond,
        long encryptionCount,
        long decryptionCount,
        long signatureCount,
        long verificationCount,
        double averageEncryptionTime,
        double averageDecryptionTime,
        String implementation,
        long timestamp
    ) {}

    /**
     * Chain information for cross-chain bridge (AV11-369)
     */
    public record ChainInfo(
        String chainId,
        String name,
        String network,
        boolean active,
        long blockHeight,
        String bridgeContract
    ) {}

    /**
     * Supported chains response (AV11-369)
     */
    public record SupportedChains(
        int totalChains,
        List<ChainInfo> chains,
        String bridgeVersion,
        long timestamp
    ) {}

    /**
     * Real-World Asset status (AV11-370)
     */
    public record RWAStatus(
        boolean enabled,
        long totalAssetsTokenized,
        String totalValueLocked,
        int activeAssetTypes,
        List<String> supportedAssetCategories,
        String complianceLevel,
        String status,
        long timestamp
    ) {}

    /**
     * Health status of the system
     */
    public record HealthStatus(
        String status,
        String message,
        long timestamp,
        long uptime
    ) {}

    /**
     * System information
     */
    public record SystemInfo(
        String version,
        String platform,
        String javaVersion,
        long maxMemory,
        long totalMemory,
        long freeMemory,
        int processorCount,
        String operatingSystem,
        long timestamp
    ) {}

    /**
     * Performance statistics
     */
    public record PerformanceStats(
        long tps,
        long peakTps,
        double avgLatency,
        double p99Latency,
        long transactionCount,
        long blockCount,
        double cpuUsage,
        double memoryUsage,
        String networkStatus,
        long timestamp
    ) {}

    /**
     * System status
     */
    public record SystemStatus(
        String status,
        HealthStatus health,
        PerformanceStats performance,
        long timestamp
    ) {}

    /**
     * Ultra-high throughput request
     */
    public record UltraHighThroughputRequest(
        int transactionCount,
        int concurrentRequests,
        long durationSeconds,
        String testType
    ) {}

    /**
     * Ultra-high throughput statistics
     */
    public record UltraHighThroughputStats(
        long achievedTps,
        long targetTps,
        double avgLatency,
        double p99Latency,
        long totalTransactions,
        long successCount,
        long failureCount,
        String status,
        long timestamp
    ) {}
}
