package io.aurigraph.v11.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Transaction Model for Aurigraph V11 - LevelDB Compatible
 *
 * Represents a transaction in the Aurigraph V11 platform.
 * Maps to Transaction message in aurigraph-v11.proto
 *
 * LevelDB Storage: Uses id (hash) as primary key
 * JSON Serializable: All fields stored as JSON in LevelDB
 * Binary Data: Stored as Base64 encoded strings for JSON compatibility
 *
 * @version 4.0.0 (LevelDB Migration - Oct 8, 2025)
 * @author Aurigraph V11 Development Team
 * @since Sprint 9
 */
public class Transaction {

    @JsonProperty("id")
    private String id;

    @JsonProperty("payload")
    private String payload; // Base64 encoded

    @JsonProperty("priority")
    private int priority = 0;

    @JsonProperty("timestamp")
    private Instant timestamp;

    @JsonProperty("status")
    private TransactionStatus status = TransactionStatus.PENDING;

    @JsonProperty("type")
    private TransactionType type = TransactionType.TRANSFER;

    @JsonProperty("fromAddress")
    private String fromAddress;

    @JsonProperty("toAddress")
    private String toAddress;

    @JsonProperty("amount")
    private long amount = 0L;

    @JsonProperty("gasPrice")
    private long gasPrice = 0L;

    @JsonProperty("gasLimit")
    private long gasLimit = 0L;

    @JsonProperty("signature")
    private String signature; // Base64 encoded

    @JsonProperty("metadata")
    private Map<String, String> metadata = new HashMap<>();

    @JsonProperty("hash")
    private String hash;

    @JsonProperty("from")
    private String from;

    @JsonProperty("to")
    private String to;

    @JsonProperty("zkProof")
    private String zkProof; // Base64 encoded

    @JsonProperty("blockHeight")
    private Long blockHeight;

    @JsonProperty("blockHash")
    private String blockHash;

    @JsonProperty("confirmations")
    private int confirmations = 0;

    // Block reference as ID instead of full object
    @JsonProperty("blockId")
    private String blockId;

    // ==================== CONSTRUCTORS ====================

    public Transaction() {
        this.priority = 0;
        this.amount = 0L;
        this.gasPrice = 0L;
        this.gasLimit = 0L;
        this.confirmations = 0;
        this.metadata = new HashMap<>();
    }

    public Transaction(String id, String fromAddress, String toAddress, long amount) {
        this();
        this.id = id;
        this.fromAddress = fromAddress;
        this.toAddress = toAddress;
        this.amount = amount;
        this.timestamp = Instant.now();
        this.status = TransactionStatus.PENDING;
        this.type = TransactionType.TRANSFER;
    }

    // ==================== LIFECYCLE METHODS ====================

    /**
     * Ensure timestamp is set (call before first persist)
     */
    public void ensureTimestamp() {
        if (timestamp == null) {
            timestamp = Instant.now();
        }
        if (status == null) {
            status = TransactionStatus.PENDING;
        }
        if (type == null) {
            type = TransactionType.TRANSFER;
        }
    }

    // ==================== GETTERS AND SETTERS ====================

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }

    public int getPriority() { return priority; }
    public void setPriority(int priority) { this.priority = priority; }

    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }

    public TransactionStatus getStatus() { return status; }
    public void setStatus(TransactionStatus status) { this.status = status; }

    public TransactionType getType() { return type; }
    public void setType(TransactionType type) { this.type = type; }

    public String getFromAddress() { return fromAddress; }
    public void setFromAddress(String fromAddress) { this.fromAddress = fromAddress; }

    public String getToAddress() { return toAddress; }
    public void setToAddress(String toAddress) { this.toAddress = toAddress; }

    public long getAmount() { return amount; }
    public void setAmount(long amount) { this.amount = amount; }

    public long getGasPrice() { return gasPrice; }
    public void setGasPrice(long gasPrice) { this.gasPrice = gasPrice; }

    public long getGasLimit() { return gasLimit; }
    public void setGasLimit(long gasLimit) { this.gasLimit = gasLimit; }

    public String getSignature() { return signature; }
    public void setSignature(String signature) { this.signature = signature; }

    public Map<String, String> getMetadata() { return metadata; }
    public void setMetadata(Map<String, String> metadata) { this.metadata = metadata; }

    public String getHash() { return hash; }
    public void setHash(String hash) { this.hash = hash; }

    public String getFrom() { return from; }
    public void setFrom(String from) { this.from = from; }

    public String getTo() { return to; }
    public void setTo(String to) { this.to = to; }

    public String getZkProof() { return zkProof; }
    public void setZkProof(String zkProof) { this.zkProof = zkProof; }

    public Long getBlockHeight() { return blockHeight; }
    public void setBlockHeight(Long blockHeight) { this.blockHeight = blockHeight; }

    public String getBlockHash() { return blockHash; }
    public void setBlockHash(String blockHash) { this.blockHash = blockHash; }

    public int getConfirmations() { return confirmations; }
    public void setConfirmations(int confirmations) { this.confirmations = confirmations; }

    public String getBlockId() { return blockId; }
    public void setBlockId(String blockId) { this.blockId = blockId; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Transaction that = (Transaction) o;
        return Objects.equals(id, that.id) && Objects.equals(hash, that.hash);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, hash);
    }

    @Override
    public String toString() {
        return String.format("Transaction{id='%s', fromAddress='%s', toAddress='%s', " +
                        "amount=%d, status=%s, type=%s, timestamp=%s, hash='%s'}",
                id, fromAddress, toAddress, amount, status, type, timestamp, hash);
    }
}
