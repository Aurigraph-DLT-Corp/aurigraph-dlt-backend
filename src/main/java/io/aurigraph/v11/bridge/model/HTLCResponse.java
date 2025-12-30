package io.aurigraph.v11.bridge.model;

import java.time.Instant;

/**
 * HTLC (Hash Time Lock Contract) Response Model
 * Represents the response after deploying an HTLC contract on a blockchain
 *
 * @author Claude Code - Priority 3 Implementation
 * @version 1.0.0
 */
public class HTLCResponse {

    private String contractAddress;
    private String transactionHash;
    private String chain;
    private String status;  // PENDING, CONFIRMED, FAILED
    private long blockHeight;
    private String errorMessage;
    private Instant createdAt;

    public HTLCResponse() {
        this.createdAt = Instant.now();
    }

    public HTLCResponse(String contractAddress, String transactionHash, String chain,
                        String status, long blockHeight) {
        this.contractAddress = contractAddress;
        this.transactionHash = transactionHash;
        this.chain = chain;
        this.status = status;
        this.blockHeight = blockHeight;
        this.createdAt = Instant.now();
    }

    // Getters and Setters
    public String getContractAddress() {
        return contractAddress;
    }

    public void setContractAddress(String contractAddress) {
        this.contractAddress = contractAddress;
    }

    public String getTransactionHash() {
        return transactionHash;
    }

    public void setTransactionHash(String transactionHash) {
        this.transactionHash = transactionHash;
    }

    public String getChain() {
        return chain;
    }

    public void setChain(String chain) {
        this.chain = chain;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public long getBlockHeight() {
        return blockHeight;
    }

    public void setBlockHeight(long blockHeight) {
        this.blockHeight = blockHeight;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isSuccessful() {
        return "CONFIRMED".equals(status);
    }

    public boolean isFailed() {
        return "FAILED".equals(status);
    }

    @Override
    public String toString() {
        return String.format("HTLCResponse{contract='%s', tx='%s', chain=%s, status=%s, block=%d}",
                contractAddress != null ? contractAddress.substring(0, Math.min(10, contractAddress.length())) + "..." : "null",
                transactionHash != null ? transactionHash.substring(0, 16) + "..." : "null",
                chain, status, blockHeight);
    }
}
