package io.aurigraph.v11.bridge;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Bridge transaction information
 */
public class BridgeTransaction {
    private final String transactionId;
    private final String sourceChain;
    private final String targetChain;
    private final String sourceAddress;
    private final String targetAddress;
    private final String tokenContract;
    private final String tokenSymbol;
    private final BigDecimal amount;
    private final BigDecimal bridgeFee;
    private final BridgeTransactionStatus status;
    private final BridgeTransactionType type;
    private final Instant createdAt;

    public BridgeTransaction(String transactionId, String sourceChain, String targetChain,
                           String sourceAddress, String targetAddress, String tokenContract,
                           String tokenSymbol, BigDecimal amount, BigDecimal bridgeFee,
                           BridgeTransactionStatus status, BridgeTransactionType type, Instant createdAt) {
        this.transactionId = transactionId;
        this.sourceChain = sourceChain;
        this.targetChain = targetChain;
        this.sourceAddress = sourceAddress;
        this.targetAddress = targetAddress;
        this.tokenContract = tokenContract;
        this.tokenSymbol = tokenSymbol;
        this.amount = amount;
        this.bridgeFee = bridgeFee;
        this.status = status;
        this.type = type;
        this.createdAt = createdAt;
    }

    public BridgeTransaction withStatus(BridgeTransactionStatus newStatus) {
        return new BridgeTransaction(transactionId, sourceChain, targetChain, sourceAddress,
            targetAddress, tokenContract, tokenSymbol, amount, bridgeFee, newStatus, type, createdAt);
    }

    // Getters
    public String getTransactionId() { return transactionId; }
    public String getSourceChain() { return sourceChain; }
    public String getTargetChain() { return targetChain; }
    public String getSourceAddress() { return sourceAddress; }
    public String getTargetAddress() { return targetAddress; }
    public String getTokenContract() { return tokenContract; }
    public String getTokenSymbol() { return tokenSymbol; }
    public BigDecimal getAmount() { return amount; }
    public BigDecimal getBridgeFee() { return bridgeFee; }
    public BridgeTransactionStatus getStatus() { return status; }
    public BridgeTransactionType getType() { return type; }
    public Instant getCreatedAt() { return createdAt; }

    @Override
    public String toString() {
        return String.format("BridgeTransaction{id='%s', %s->%s, amount=%s %s, status=%s}",
            transactionId, sourceChain, targetChain, amount, tokenSymbol, status);
    }
}
