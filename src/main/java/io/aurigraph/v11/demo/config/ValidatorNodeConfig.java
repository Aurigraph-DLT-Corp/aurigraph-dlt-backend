package io.aurigraph.v11.demo.config;

import io.aurigraph.v11.demo.models.NodeType;
import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * Configuration specific to Validator Nodes.
 *
 * Validator Nodes are responsible for:
 * - Participating in consensus rounds (HyperRAFT++)
 * - Validating transaction batches
 * - Proposing and voting on blocks
 * - Maintaining blockchain state
 * - Executing consensus algorithm
 *
 * Performance Targets:
 * - Block proposal time: <500ms
 * - Consensus finality: <1s
 * - TPS per validator: 200K+
 * - Total network TPS: 2M+ (with 10+ validators)
 */
@RegisterForReflection
public class ValidatorNodeConfig extends NodeConfiguration {

    /**
     * Consensus algorithm to use.
     * Currently supports: "HyperRAFT++"
     * Default: "HyperRAFT++"
     */
    private String consensusAlgorithm = "HyperRAFT++";

    /**
     * Minimum number of validators required for the network.
     * Recommended: 4-21 validators for optimal performance
     * Default: 4
     */
    private int minValidators = 4;

    /**
     * Quorum percentage required for consensus decisions.
     * Typically 67% for Byzantine fault tolerance.
     * Default: 67 (67%)
     */
    private int quorumPercentage = 67;

    /**
     * Target block time in milliseconds.
     * Lower values increase throughput but may reduce finality guarantees.
     * Default: 500ms
     */
    private int blockTime = 500;

    /**
     * Maximum number of transactions per block.
     * Larger blocks increase throughput but may increase latency.
     * Default: 10,000 transactions
     */
    private int blockSize = 10000;

    /**
     * Enable AI-based consensus optimization.
     * Uses machine learning to optimize validator behavior and transaction ordering.
     * Default: true
     */
    private boolean enableAIOptimization = true;

    /**
     * Enable quantum-resistant cryptography for consensus messages.
     * Uses CRYSTALS-Dilithium for signatures.
     * Default: true
     */
    private boolean quantumResistant = true;

    /**
     * Staking amount required to participate as a validator.
     * Format: string representation of amount (e.g., "1000000")
     * Default: "1000000"
     */
    private String stakingAmount = "1000000";

    /**
     * Maximum pending transactions in the mempool.
     * Default: 1,000,000 transactions
     */
    private int maxMempoolSize = 1000000;

    /**
     * Transaction validation timeout in milliseconds.
     * Default: 100ms
     */
    private int validationTimeout = 100;

    /**
     * Enable block pruning to save disk space.
     * Old blocks are archived after a certain period.
     * Default: true
     */
    private boolean enablePruning = true;

    /**
     * Number of blocks to keep before pruning.
     * Default: 100,000 blocks
     */
    private int pruningThreshold = 100000;

    /**
     * Enable state snapshots for faster syncing.
     * Default: true
     */
    private boolean enableStateSnapshots = true;

    /**
     * State snapshot interval in blocks.
     * Default: 10,000 blocks
     */
    private int snapshotInterval = 10000;

    // Constructors

    public ValidatorNodeConfig() {
        super();
        setNodeType(NodeType.VALIDATOR);
    }

    public ValidatorNodeConfig(String nodeId) {
        super(nodeId, NodeType.VALIDATOR);
    }

    // Getters and Setters

    public String getConsensusAlgorithm() {
        return consensusAlgorithm;
    }

    public void setConsensusAlgorithm(String consensusAlgorithm) {
        this.consensusAlgorithm = consensusAlgorithm;
    }

    public int getMinValidators() {
        return minValidators;
    }

    public void setMinValidators(int minValidators) {
        this.minValidators = minValidators;
    }

    public int getQuorumPercentage() {
        return quorumPercentage;
    }

    public void setQuorumPercentage(int quorumPercentage) {
        this.quorumPercentage = quorumPercentage;
    }

    public int getBlockTime() {
        return blockTime;
    }

    public void setBlockTime(int blockTime) {
        this.blockTime = blockTime;
    }

    public int getBlockSize() {
        return blockSize;
    }

    public void setBlockSize(int blockSize) {
        this.blockSize = blockSize;
    }

    public boolean isEnableAIOptimization() {
        return enableAIOptimization;
    }

    public void setEnableAIOptimization(boolean enableAIOptimization) {
        this.enableAIOptimization = enableAIOptimization;
    }

    public boolean isQuantumResistant() {
        return quantumResistant;
    }

    public void setQuantumResistant(boolean quantumResistant) {
        this.quantumResistant = quantumResistant;
    }

    public String getStakingAmount() {
        return stakingAmount;
    }

    public void setStakingAmount(String stakingAmount) {
        this.stakingAmount = stakingAmount;
    }

    public int getMaxMempoolSize() {
        return maxMempoolSize;
    }

    public void setMaxMempoolSize(int maxMempoolSize) {
        this.maxMempoolSize = maxMempoolSize;
    }

    public int getValidationTimeout() {
        return validationTimeout;
    }

    public void setValidationTimeout(int validationTimeout) {
        this.validationTimeout = validationTimeout;
    }

    public boolean isEnablePruning() {
        return enablePruning;
    }

    public void setEnablePruning(boolean enablePruning) {
        this.enablePruning = enablePruning;
    }

    public int getPruningThreshold() {
        return pruningThreshold;
    }

    public void setPruningThreshold(int pruningThreshold) {
        this.pruningThreshold = pruningThreshold;
    }

    public boolean isEnableStateSnapshots() {
        return enableStateSnapshots;
    }

    public void setEnableStateSnapshots(boolean enableStateSnapshots) {
        this.enableStateSnapshots = enableStateSnapshots;
    }

    public int getSnapshotInterval() {
        return snapshotInterval;
    }

    public void setSnapshotInterval(int snapshotInterval) {
        this.snapshotInterval = snapshotInterval;
    }

    @Override
    public void validate() {
        // Validate base configuration first
        super.validate();

        // Validate validator-specific configuration
        if (!consensusAlgorithm.equals("HyperRAFT++")) {
            throw new IllegalArgumentException("Currently only HyperRAFT++ consensus algorithm is supported");
        }

        if (minValidators < 4) {
            throw new IllegalArgumentException("Minimum validators must be at least 4 for Byzantine fault tolerance");
        }

        if (minValidators > 21) {
            throw new IllegalArgumentException("Minimum validators should not exceed 21 for optimal performance");
        }

        if (quorumPercentage < 51 || quorumPercentage > 100) {
            throw new IllegalArgumentException("Quorum percentage must be between 51 and 100");
        }

        if (blockTime <= 0) {
            throw new IllegalArgumentException("Block time must be greater than 0");
        }

        if (blockTime < 100) {
            throw new IllegalArgumentException("Block time should be at least 100ms for stability");
        }

        if (blockSize <= 0) {
            throw new IllegalArgumentException("Block size must be greater than 0");
        }

        if (blockSize > 100000) {
            throw new IllegalArgumentException("Block size should not exceed 100,000 for optimal performance");
        }

        if (stakingAmount == null || !stakingAmount.matches("\\d+")) {
            throw new IllegalArgumentException("Staking amount must be a positive number");
        }

        long stakingValue = Long.parseLong(stakingAmount);
        if (stakingValue <= 0) {
            throw new IllegalArgumentException("Staking amount must be greater than 0");
        }

        if (maxMempoolSize <= 0) {
            throw new IllegalArgumentException("Max mempool size must be greater than 0");
        }

        if (validationTimeout <= 0) {
            throw new IllegalArgumentException("Validation timeout must be greater than 0");
        }

        if (enablePruning && pruningThreshold <= 0) {
            throw new IllegalArgumentException("Pruning threshold must be greater than 0 when pruning is enabled");
        }

        if (enableStateSnapshots && snapshotInterval <= 0) {
            throw new IllegalArgumentException("Snapshot interval must be greater than 0 when snapshots are enabled");
        }
    }

    @Override
    public String toString() {
        return "ValidatorNodeConfig{" +
               "nodeId='" + getNodeId() + '\'' +
               ", consensusAlgorithm='" + consensusAlgorithm + '\'' +
               ", minValidators=" + minValidators +
               ", quorumPercentage=" + quorumPercentage +
               ", blockTime=" + blockTime +
               ", blockSize=" + blockSize +
               ", enableAIOptimization=" + enableAIOptimization +
               ", quantumResistant=" + quantumResistant +
               ", stakingAmount='" + stakingAmount + '\'' +
               '}';
    }
}
