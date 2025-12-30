package io.aurigraph.v11.bridge.model;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * HTLC (Hash Time Lock Contract) Request Model
 * Represents a request to deploy and lock funds in an HTLC contract
 * for atomic swap operations across chains
 *
 * @author Claude Code - Priority 3 Implementation
 * @version 1.0.0
 */
public class HTLCRequest {

    private String htlcId;
    private String transactionId;
    private String sourceChain;
    private String targetChain;
    private String sourceAddress;
    private String targetAddress;
    private String tokenAddress;
    private BigDecimal amount;
    private String secretHash;  // Hash of the secret (SHA-256)
    private long timelock;      // Timelock in seconds (typically 48 hours)
    private String contractAddress;
    private Instant createdAt;

    public HTLCRequest() {
        this.createdAt = Instant.now();
    }

    public HTLCRequest(String transactionId, String sourceChain, String targetChain,
                       String sourceAddress, String targetAddress, String tokenAddress,
                       BigDecimal amount, String secretHash, long timelock) {
        this.transactionId = transactionId;
        this.sourceChain = sourceChain;
        this.targetChain = targetChain;
        this.sourceAddress = sourceAddress;
        this.targetAddress = targetAddress;
        this.tokenAddress = tokenAddress;
        this.amount = amount;
        this.secretHash = secretHash;
        this.timelock = timelock;
        this.createdAt = Instant.now();
    }

    // Getters and Setters
    public String getHtlcId() {
        return htlcId;
    }

    public void setHtlcId(String htlcId) {
        this.htlcId = htlcId;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getSourceChain() {
        return sourceChain;
    }

    public void setSourceChain(String sourceChain) {
        this.sourceChain = sourceChain;
    }

    public String getTargetChain() {
        return targetChain;
    }

    public void setTargetChain(String targetChain) {
        this.targetChain = targetChain;
    }

    public String getSourceAddress() {
        return sourceAddress;
    }

    public void setSourceAddress(String sourceAddress) {
        this.sourceAddress = sourceAddress;
    }

    public String getTargetAddress() {
        return targetAddress;
    }

    public void setTargetAddress(String targetAddress) {
        this.targetAddress = targetAddress;
    }

    public String getTokenAddress() {
        return tokenAddress;
    }

    public void setTokenAddress(String tokenAddress) {
        this.tokenAddress = tokenAddress;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getSecretHash() {
        return secretHash;
    }

    public void setSecretHash(String secretHash) {
        this.secretHash = secretHash;
    }

    public long getTimelock() {
        return timelock;
    }

    public void setTimelock(long timelock) {
        this.timelock = timelock;
    }

    public String getContractAddress() {
        return contractAddress;
    }

    public void setContractAddress(String contractAddress) {
        this.contractAddress = contractAddress;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return String.format("HTLCRequest{id='%s', tx='%s', %s->%s, amount=%s, timelock=%d, hash=%s}",
                htlcId, transactionId, sourceChain, targetChain, amount, timelock,
                secretHash != null ? secretHash.substring(0, 16) + "..." : "null");
    }
}
