package io.aurigraph.v11.bridge.persistence;

import io.aurigraph.v11.bridge.BridgeTransactionStatus;
import io.aurigraph.v11.bridge.BridgeTransactionType;
import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Bridge Transaction Entity for persistent storage
 *
 * Represents a cross-chain bridge transaction with full lifecycle tracking
 * including HTLC (Hash Time-Locked Contract) state, multi-signature validation,
 * and retry mechanism support.
 *
 * This entity uses optimistic locking to prevent concurrent modification issues
 * and includes comprehensive indexing for query performance.
 *
 * @author BDA + IBA
 * @version 11.1.0
 * @since Sprint 14
 */
@Entity
@Table(name = "bridge_transactions", indexes = {
    @Index(name = "idx_tx_id", columnList = "transaction_id", unique = true),
    @Index(name = "idx_status", columnList = "status"),
    @Index(name = "idx_created", columnList = "created_at"),
    @Index(name = "idx_source_address", columnList = "source_address"),
    @Index(name = "idx_target_address", columnList = "target_address"),
    @Index(name = "idx_source_chain", columnList = "source_chain"),
    @Index(name = "idx_target_chain", columnList = "target_chain"),
    @Index(name = "idx_status_created", columnList = "status, created_at")
})
public class BridgeTransactionEntity extends PanacheEntity {

    /**
     * Unique transaction identifier (UUID format)
     * This is the primary business key for tracking bridge transactions
     */
    @NotBlank
    @Column(name = "transaction_id", nullable = false, unique = true, length = 64)
    public String transactionId;

    /**
     * Source blockchain identifier (e.g., "ethereum", "solana", "polkadot")
     */
    @NotBlank
    @Column(name = "source_chain", nullable = false, length = 32)
    public String sourceChain;

    /**
     * Target blockchain identifier
     */
    @NotBlank
    @Column(name = "target_chain", nullable = false, length = 32)
    public String targetChain;

    /**
     * Source chain wallet address
     */
    @NotBlank
    @Column(name = "source_address", nullable = false, length = 128)
    public String sourceAddress;

    /**
     * Target chain wallet address
     */
    @NotBlank
    @Column(name = "target_address", nullable = false, length = 128)
    public String targetAddress;

    /**
     * Token contract address (optional, for ERC-20/SPL tokens)
     */
    @Column(name = "token_contract", length = 128)
    public String tokenContract;

    /**
     * Token symbol (e.g., "ETH", "SOL", "DOT")
     */
    @NotBlank
    @Column(name = "token_symbol", nullable = false, length = 16)
    public String tokenSymbol;

    /**
     * Transfer amount in token's base units
     * Precision: 36 digits total, 18 decimal places (supports crypto precision)
     */
    @NotNull
    @Positive
    @Column(name = "amount", nullable = false, precision = 36, scale = 18)
    public BigDecimal amount;

    /**
     * Bridge fee charged for the transfer
     */
    @NotNull
    @PositiveOrZero
    @Column(name = "bridge_fee", nullable = false, precision = 36, scale = 18)
    public BigDecimal bridgeFee;

    /**
     * Current transaction status
     * Values: PENDING, CONFIRMING, COMPLETED, FAILED, REFUNDED
     */
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    public BridgeTransactionStatus status;

    /**
     * Transaction type
     * Values: BRIDGE, ATOMIC_SWAP, LOCK_MINT, BURN_MINT
     */
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 32)
    public BridgeTransactionType transactionType;

    /**
     * HTLC (Hash Time-Locked Contract) hash for atomic swaps
     * SHA-256 hash of the secret
     */
    @Column(name = "htlc_hash", length = 64)
    public String htlcHash;

    /**
     * HTLC secret (revealed when claiming funds)
     */
    @Column(name = "htlc_secret", length = 64)
    public String htlcSecret;

    /**
     * HTLC timeout timestamp (Unix epoch milliseconds)
     */
    @Column(name = "htlc_timeout")
    public Long htlcTimeout;

    /**
     * Source chain transaction hash
     */
    @Column(name = "source_tx_hash", length = 128)
    public String sourceTxHash;

    /**
     * Target chain transaction hash
     */
    @Column(name = "target_tx_hash", length = 128)
    public String targetTxHash;

    /**
     * Current number of confirmations received
     */
    @PositiveOrZero
    @Column(name = "confirmations")
    public Integer confirmations = 0;

    /**
     * Required confirmations for finality
     * Default: 12 for Ethereum, 32 for Polkadot, etc.
     */
    @Positive
    @Column(name = "required_confirmations")
    public Integer requiredConfirmations = 12;

    /**
     * Error message if transaction failed
     */
    @Column(name = "error_message", length = 512)
    public String errorMessage;

    /**
     * Current retry attempt count
     */
    @PositiveOrZero
    @Column(name = "retry_count")
    public Integer retryCount = 0;

    /**
     * Maximum retry attempts allowed
     */
    @Positive
    @Column(name = "max_retries")
    public Integer maxRetries = 3;

    /**
     * Multi-signature validation status
     * True if validator network quorum reached (4/7)
     */
    @Column(name = "multi_sig_validated")
    public Boolean multiSigValidated = false;

    /**
     * Number of validators who signed this transaction
     */
    @PositiveOrZero
    @Column(name = "validator_count")
    public Integer validatorCount = 0;

    /**
     * Timestamp when transaction was created
     * Automatically set by Hibernate
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    public LocalDateTime createdAt;

    /**
     * Timestamp when transaction was last updated
     * Automatically updated by Hibernate on every modification
     */
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    public LocalDateTime updatedAt;

    /**
     * Timestamp when transaction completed (success or failure)
     */
    @Column(name = "completed_at")
    public LocalDateTime completedAt;

    /**
     * Optimistic locking version for concurrency control
     * Prevents lost updates when multiple processes modify same transaction
     */
    @Version
    @Column(name = "version")
    public Long version;

    // =========================================================================
    // Panache Query Helper Methods
    // =========================================================================

    /**
     * Find transaction by unique transaction ID
     *
     * @param txId The transaction ID to search for
     * @return BridgeTransactionEntity or null if not found
     */
    public static BridgeTransactionEntity findByTransactionId(String txId) {
        return find("transactionId", txId).firstResult();
    }

    /**
     * Find pending transfers older than specified age
     * Used for stuck transfer detection and recovery
     *
     * @param ageMinutes Age threshold in minutes
     * @return List of pending transfers older than threshold
     */
    public static List<BridgeTransactionEntity> findPendingTransfers(int ageMinutes) {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(ageMinutes);
        return find("status = ?1 and createdAt < ?2",
            BridgeTransactionStatus.PENDING,
            cutoff)
            .list();
    }

    /**
     * Find all transactions with specific status
     * Ordered by creation time (newest first)
     *
     * @param status The status to filter by
     * @return List of transactions with given status
     */
    public static List<BridgeTransactionEntity> findByStatus(BridgeTransactionStatus status) {
        return find("status = ?1 order by createdAt desc", status).list();
    }

    /**
     * Find stuck transfers (PENDING or CONFIRMING for longer than timeout)
     * Used by scheduled recovery process
     *
     * @param timeoutMinutes Timeout threshold in minutes
     * @return List of stuck transfers requiring intervention
     */
    public static List<BridgeTransactionEntity> findStuckTransfers(int timeoutMinutes) {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(timeoutMinutes);
        return find("status in (?1, ?2) and createdAt < ?3",
            BridgeTransactionStatus.PENDING,
            BridgeTransactionStatus.CONFIRMING,
            cutoff)
            .list();
    }

    /**
     * Count transactions by status
     * Used for monitoring and statistics
     *
     * @param status The status to count
     * @return Number of transactions with given status
     */
    public static long countByStatus(BridgeTransactionStatus status) {
        return count("status", status);
    }

    /**
     * Find transactions by source or target address
     * Used for address-based history lookup
     *
     * @param address The address to search for
     * @return List of transactions involving the address
     */
    public static List<BridgeTransactionEntity> findByAddress(String address) {
        return find("sourceAddress = ?1 or targetAddress = ?1 order by createdAt desc", address).list();
    }

    /**
     * Find failed transactions eligible for retry
     * Returns failed transactions where retryCount < maxRetries
     *
     * @return List of transactions eligible for retry
     */
    public static List<BridgeTransactionEntity> findRetryableTransfers() {
        return find("status = ?1 and retryCount < maxRetries",
            BridgeTransactionStatus.FAILED)
            .list();
    }

    /**
     * Find transactions by source chain
     *
     * @param chainName Source chain identifier
     * @return List of transactions from the chain
     */
    public static List<BridgeTransactionEntity> findBySourceChain(String chainName) {
        return find("sourceChain = ?1 order by createdAt desc", chainName).list();
    }

    /**
     * Find transactions by target chain
     *
     * @param chainName Target chain identifier
     * @return List of transactions to the chain
     */
    public static List<BridgeTransactionEntity> findByTargetChain(String chainName) {
        return find("targetChain = ?1 order by createdAt desc", chainName).list();
    }

    /**
     * Find transactions requiring multi-sig validation
     * Returns transactions in PENDING status without validator approval
     *
     * @return List of transactions awaiting validation
     */
    public static List<BridgeTransactionEntity> findPendingValidation() {
        return find("status = ?1 and multiSigValidated = false",
            BridgeTransactionStatus.PENDING)
            .list();
    }

    /**
     * Find active HTLC contracts (atomic swaps in progress)
     *
     * @return List of active atomic swap transactions
     */
    public static List<BridgeTransactionEntity> findActiveAtomicSwaps() {
        return find("transactionType = ?1 and status in (?2, ?3)",
            BridgeTransactionType.ATOMIC_SWAP,
            BridgeTransactionStatus.PENDING,
            BridgeTransactionStatus.CONFIRMING)
            .list();
    }

    // =========================================================================
    // Utility Methods
    // =========================================================================

    /**
     * Check if transaction is in a final state (completed or failed)
     *
     * @return true if transaction is complete or failed
     */
    public boolean isFinal() {
        return status == BridgeTransactionStatus.COMPLETED ||
               status == BridgeTransactionStatus.FAILED ||
               status == BridgeTransactionStatus.REFUNDED;
    }

    /**
     * Check if transaction can be retried
     *
     * @return true if retry is possible
     */
    public boolean canRetry() {
        return status == BridgeTransactionStatus.FAILED &&
               retryCount < maxRetries;
    }

    /**
     * Check if HTLC has timed out
     *
     * @return true if HTLC timeout has passed
     */
    public boolean isHtlcExpired() {
        if (htlcTimeout == null) {
            return false;
        }
        return System.currentTimeMillis() > htlcTimeout;
    }

    /**
     * Check if transaction has sufficient confirmations
     *
     * @return true if confirmations >= required confirmations
     */
    public boolean hasRequiredConfirmations() {
        if (confirmations == null || requiredConfirmations == null) {
            return false;
        }
        return confirmations >= requiredConfirmations;
    }

    @Override
    public String toString() {
        return String.format("BridgeTransaction[id=%s, %s->%s, %s %s, status=%s, created=%s]",
            transactionId,
            sourceChain,
            targetChain,
            amount,
            tokenSymbol,
            status,
            createdAt);
    }
}
