package io.aurigraph.v11.transaction.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class TransactionResponse {
    public Long id;
    public String txHash;
    public String userId;
    public String fromAddress;
    public String toAddress;
    public BigDecimal amount;
    public String status;
    public String transactionType;
    public BigDecimal totalFee;
    public Long blockNumber;
    public Integer confirmationCount;
    public Long finalityTimeMs;
    public LocalDateTime createdAt;
    public LocalDateTime confirmedAt;
    public LocalDateTime finalizedAt;

    public TransactionResponse(
        Long id,
        String txHash,
        String userId,
        String fromAddress,
        String toAddress,
        BigDecimal amount,
        String status,
        String transactionType,
        BigDecimal totalFee,
        Long blockNumber,
        Integer confirmationCount,
        Long finalityTimeMs,
        LocalDateTime createdAt,
        LocalDateTime confirmedAt,
        LocalDateTime finalizedAt
    ) {
        this.id = id;
        this.txHash = txHash;
        this.userId = userId;
        this.fromAddress = fromAddress;
        this.toAddress = toAddress;
        this.amount = amount;
        this.status = status;
        this.transactionType = transactionType;
        this.totalFee = totalFee;
        this.blockNumber = blockNumber;
        this.confirmationCount = confirmationCount;
        this.finalityTimeMs = finalityTimeMs;
        this.createdAt = createdAt;
        this.confirmedAt = confirmedAt;
        this.finalizedAt = finalizedAt;
    }
}
