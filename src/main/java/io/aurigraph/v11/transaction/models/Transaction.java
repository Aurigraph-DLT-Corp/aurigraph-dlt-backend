package io.aurigraph.v11.transaction.models;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transaction", indexes = {
    @Index(name = "idx_user_id", columnList = "user_id"),
    @Index(name = "idx_status", columnList = "status"),
    @Index(name = "idx_created_at", columnList = "created_at"),
    @Index(name = "idx_tx_hash", columnList = "tx_hash")
})
public class Transaction extends PanacheEntity {

    public enum TransactionStatus {
        PENDING, VALIDATING, CONFIRMED, FINALIZED, FAILED, CANCELLED, REJECTED
    }

    public enum TransactionType {
        TRANSFER, SMART_CONTRACT, GOVERNANCE, ASSET_CREATION, CROSS_CHAIN, INTERNAL
    }

    @Column(name = "tx_hash", nullable = false, unique = true)
    public String txHash;

    @Column(name = "user_id", nullable = false)
    public String userId;

    @Column(name = "from_address", nullable = false)
    public String fromAddress;

    @Column(name = "to_address")
    public String toAddress;

    @Column(name = "amount", nullable = false)
    public BigDecimal amount;

    @Column(name = "transaction_type", nullable = false)
    @Enumerated(EnumType.STRING)
    public TransactionType transactionType;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    public TransactionStatus status;

    @Column(name = "gas_limit")
    public Long gasLimit;

    @Column(name = "gas_used")
    public Long gasUsed;

    @Column(name = "gas_price", nullable = false)
    public BigDecimal gasPrice;

    @Column(name = "total_fee", nullable = false)
    public BigDecimal totalFee;

    @Column(name = "nonce")
    public Long nonce;

    @Column(name = "data", columnDefinition = "jsonb")
    public String data;

    @Column(name = "block_number")
    public Long blockNumber;

    @Column(name = "block_hash")
    public String blockHash;

    @Column(name = "confirmation_count")
    public Integer confirmationCount = 0;

    @Column(name = "finality_time_ms")
    public Long finalityTimeMs;

    @Column(name = "error_code")
    public String errorCode;

    @Column(name = "error_message")
    public String errorMessage;

    @Column(name = "optimization_score")
    public BigDecimal optimizationScore;

    @Column(name = "created_at", nullable = false, updatable = false)
    public LocalDateTime createdAt;

    @Column(name = "submitted_at")
    public LocalDateTime submittedAt;

    @Column(name = "confirmed_at")
    public LocalDateTime confirmedAt;

    @Column(name = "finalized_at")
    public LocalDateTime finalizedAt;

    @Column(name = "updated_at")
    public LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isPending() {
        return status == TransactionStatus.PENDING;
    }

    public boolean isConfirmed() {
        return status == TransactionStatus.CONFIRMED || status == TransactionStatus.FINALIZED;
    }

    public boolean isFailed() {
        return status == TransactionStatus.FAILED || status == TransactionStatus.REJECTED;
    }

    public boolean isFinalized() {
        return status == TransactionStatus.FINALIZED;
    }

    public static TransactionBuilder builder() {
        return new TransactionBuilder();
    }

    public static class TransactionBuilder {
        private String txHash;
        private String userId;
        private String fromAddress;
        private String toAddress;
        private BigDecimal amount;
        private TransactionType transactionType;
        private TransactionStatus status;
        private Long gasLimit;
        private Long gasUsed;
        private BigDecimal gasPrice;
        private BigDecimal totalFee;
        private Long nonce;
        private String data;
        private Long blockNumber;
        private String blockHash;
        private Integer confirmationCount = 0;
        private Long finalityTimeMs;
        private String errorCode;
        private String errorMessage;
        private BigDecimal optimizationScore;

        public TransactionBuilder txHash(String hash) {
            this.txHash = hash;
            return this;
        }

        public TransactionBuilder userId(String userId) {
            this.userId = userId;
            return this;
        }

        public TransactionBuilder fromAddress(String address) {
            this.fromAddress = address;
            return this;
        }

        public TransactionBuilder toAddress(String address) {
            this.toAddress = address;
            return this;
        }

        public TransactionBuilder amount(BigDecimal amount) {
            this.amount = amount;
            return this;
        }

        public TransactionBuilder transactionType(TransactionType type) {
            this.transactionType = type;
            return this;
        }

        public TransactionBuilder status(TransactionStatus status) {
            this.status = status;
            return this;
        }

        public TransactionBuilder gasLimit(Long limit) {
            this.gasLimit = limit;
            return this;
        }

        public TransactionBuilder gasUsed(Long used) {
            this.gasUsed = used;
            return this;
        }

        public TransactionBuilder gasPrice(BigDecimal price) {
            this.gasPrice = price;
            return this;
        }

        public TransactionBuilder totalFee(BigDecimal fee) {
            this.totalFee = fee;
            return this;
        }

        public TransactionBuilder nonce(Long nonce) {
            this.nonce = nonce;
            return this;
        }

        public TransactionBuilder data(String data) {
            this.data = data;
            return this;
        }

        public TransactionBuilder blockNumber(Long blockNumber) {
            this.blockNumber = blockNumber;
            return this;
        }

        public TransactionBuilder blockHash(String blockHash) {
            this.blockHash = blockHash;
            return this;
        }

        public TransactionBuilder confirmationCount(Integer count) {
            this.confirmationCount = count;
            return this;
        }

        public TransactionBuilder finalityTimeMs(Long timeMs) {
            this.finalityTimeMs = timeMs;
            return this;
        }

        public TransactionBuilder errorCode(String code) {
            this.errorCode = code;
            return this;
        }

        public TransactionBuilder errorMessage(String message) {
            this.errorMessage = message;
            return this;
        }

        public TransactionBuilder optimizationScore(BigDecimal score) {
            this.optimizationScore = score;
            return this;
        }

        public Transaction build() {
            Transaction tx = new Transaction();
            tx.txHash = this.txHash;
            tx.userId = this.userId;
            tx.fromAddress = this.fromAddress;
            tx.toAddress = this.toAddress;
            tx.amount = this.amount;
            tx.transactionType = this.transactionType;
            tx.status = this.status;
            tx.gasLimit = this.gasLimit;
            tx.gasUsed = this.gasUsed;
            tx.gasPrice = this.gasPrice;
            tx.totalFee = this.totalFee;
            tx.nonce = this.nonce;
            tx.data = this.data;
            tx.blockNumber = this.blockNumber;
            tx.blockHash = this.blockHash;
            tx.confirmationCount = this.confirmationCount;
            tx.finalityTimeMs = this.finalityTimeMs;
            tx.errorCode = this.errorCode;
            tx.errorMessage = this.errorMessage;
            tx.optimizationScore = this.optimizationScore;
            return tx;
        }
    }
}
