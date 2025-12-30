package io.aurigraph.v11.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Block Model for Aurigraph V11 - LevelDB Compatible
 *
 * Represents a block in the Aurigraph V11 blockchain.
 * Contains block header information, transactions, and Merkle root.
 *
 * LevelDB Storage: Uses hash as primary key
 * JSON Serializable: All fields stored as JSON in LevelDB
 *
 * @version 4.0.0 (LevelDB Migration - Oct 8, 2025)
 * @author Aurigraph V11 Development Team
 * @since Sprint 9
 */
public class Block {

    @JsonProperty("height")
    private Long height;

    @JsonProperty("hash")
    private String hash;

    @JsonProperty("previousHash")
    private String previousHash;

    @JsonProperty("merkleRoot")
    private String merkleRoot;

    @JsonProperty("timestamp")
    private Instant timestamp;

    @JsonProperty("blockSize")
    private Long blockSize;

    @JsonProperty("transactionCount")
    private Integer transactionCount;

    @JsonProperty("validatorAddress")
    private String validatorAddress;

    @JsonProperty("validatorSignature")
    private String validatorSignature;

    @JsonProperty("consensusAlgorithm")
    private String consensusAlgorithm = "HyperRAFT++";

    @JsonProperty("channelId")
    private String channelId;

    @JsonProperty("status")
    private BlockStatus status = BlockStatus.PENDING;

    @JsonProperty("difficulty")
    private Double difficulty;

    @JsonProperty("nonce")
    private Long nonce;

    @JsonProperty("gasUsed")
    private Long gasUsed;

    @JsonProperty("gasLimit")
    private Long gasLimit;

    @JsonProperty("stateRoot")
    private String stateRoot;

    @JsonProperty("receiptsRoot")
    private String receiptsRoot;

    @JsonProperty("extraData")
    private String extraData;

    // Transaction IDs instead of full objects for LevelDB
    @JsonProperty("transactionIds")
    private List<String> transactionIds = new ArrayList<>();

    @JsonProperty("createdAt")
    private Instant createdAt;

    @JsonProperty("updatedAt")
    private Instant updatedAt;

    // ==================== CONSTRUCTORS ====================

    public Block() {
        this.status = BlockStatus.PENDING;
        this.consensusAlgorithm = "HyperRAFT++";
        this.transactionIds = new ArrayList<>();
    }

    public Block(Long height, String hash, String previousHash, String merkleRoot,
                 Instant timestamp, String validatorAddress) {
        this();
        this.height = height;
        this.hash = hash;
        this.previousHash = previousHash;
        this.merkleRoot = merkleRoot;
        this.timestamp = timestamp;
        this.validatorAddress = validatorAddress;
    }

    // ==================== LIFECYCLE METHODS ====================

    /**
     * Ensure createdAt is set (call before first persist)
     */
    public void ensureCreatedAt() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (updatedAt == null) {
            updatedAt = Instant.now();
        }
    }

    /**
     * Update timestamp (call before each persist)
     */
    public void updateTimestamp() {
        updatedAt = Instant.now();
    }

    // ==================== BUSINESS LOGIC METHODS ====================

    /**
     * Add transaction ID to block
     */
    public void addTransactionId(String transactionId) {
        if (!transactionIds.contains(transactionId)) {
            transactionIds.add(transactionId);
            transactionCount = transactionIds.size();
            updateTimestamp();
        }
    }

    /**
     * Remove transaction ID from block
     */
    public void removeTransactionId(String transactionId) {
        if (transactionIds.remove(transactionId)) {
            transactionCount = transactionIds.size();
            updateTimestamp();
        }
    }

    /**
     * Check if block is finalized
     */
    public boolean isFinalized() {
        return status == BlockStatus.FINALIZED;
    }

    /**
     * Check if block is confirmed
     */
    public boolean isConfirmed() {
        return status == BlockStatus.CONFIRMED || status == BlockStatus.FINALIZED;
    }

    // ==================== GETTERS AND SETTERS ====================

    public Long getHeight() { return height; }
    public void setHeight(Long height) { this.height = height; }

    public String getHash() { return hash; }
    public void setHash(String hash) { this.hash = hash; }

    public String getPreviousHash() { return previousHash; }
    public void setPreviousHash(String previousHash) { this.previousHash = previousHash; }

    public String getMerkleRoot() { return merkleRoot; }
    public void setMerkleRoot(String merkleRoot) { this.merkleRoot = merkleRoot; }

    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }

    public Long getBlockSize() { return blockSize; }
    public void setBlockSize(Long blockSize) { this.blockSize = blockSize; }

    public Integer getTransactionCount() { return transactionCount; }
    public void setTransactionCount(Integer transactionCount) { this.transactionCount = transactionCount; }

    public String getValidatorAddress() { return validatorAddress; }
    public void setValidatorAddress(String validatorAddress) { this.validatorAddress = validatorAddress; }

    public String getValidatorSignature() { return validatorSignature; }
    public void setValidatorSignature(String validatorSignature) { this.validatorSignature = validatorSignature; }

    public String getConsensusAlgorithm() { return consensusAlgorithm; }
    public void setConsensusAlgorithm(String consensusAlgorithm) { this.consensusAlgorithm = consensusAlgorithm; }

    public String getChannelId() { return channelId; }
    public void setChannelId(String channelId) { this.channelId = channelId; }

    public BlockStatus getStatus() { return status; }
    public void setStatus(BlockStatus status) { this.status = status; }

    public Double getDifficulty() { return difficulty; }
    public void setDifficulty(Double difficulty) { this.difficulty = difficulty; }

    public Long getNonce() { return nonce; }
    public void setNonce(Long nonce) { this.nonce = nonce; }

    public Long getGasUsed() { return gasUsed; }
    public void setGasUsed(Long gasUsed) { this.gasUsed = gasUsed; }

    public Long getGasLimit() { return gasLimit; }
    public void setGasLimit(Long gasLimit) { this.gasLimit = gasLimit; }

    public String getStateRoot() { return stateRoot; }
    public void setStateRoot(String stateRoot) { this.stateRoot = stateRoot; }

    public String getReceiptsRoot() { return receiptsRoot; }
    public void setReceiptsRoot(String receiptsRoot) { this.receiptsRoot = receiptsRoot; }

    public String getExtraData() { return extraData; }
    public void setExtraData(String extraData) { this.extraData = extraData; }

    public List<String> getTransactionIds() { return transactionIds; }
    public void setTransactionIds(List<String> transactionIds) { this.transactionIds = transactionIds; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    @Override
    public String toString() {
        return String.format("Block{height=%d, hash='%s', previousHash='%s', merkleRoot='%s', " +
                        "timestamp=%s, transactionCount=%d, validatorAddress='%s', status=%s}",
                height, hash, previousHash, merkleRoot, timestamp, transactionCount, validatorAddress, status);
    }
}
