package io.aurigraph.v11.bridge.persistence;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Atomic Swap State Entity - HTLC Persistence
 *
 * Provides persistent storage for Hash Time-Locked Contract (HTLC) state
 * used in atomic swap transactions. This ensures that atomic swaps can
 * survive service restarts and maintains the security guarantees of HTLCs.
 *
 * HTLCs enable trustless cross-chain swaps by ensuring that:
 * 1. Both parties reveal the secret, or
 * 2. Both parties get refunded after timeout
 *
 * This prevents situations where one party completes their side but the
 * other doesn't, ensuring atomic (all-or-nothing) execution.
 *
 * @author BDA + IBA
 * @version 11.1.0
 * @since Sprint 14
 */
@Entity
@Table(name = "atomic_swap_state", indexes = {
    @Index(name = "idx_swap_tx_id", columnList = "transaction_id", unique = true),
    @Index(name = "idx_swap_htlc_hash", columnList = "htlc_hash"),
    @Index(name = "idx_swap_status", columnList = "swap_status"),
    @Index(name = "idx_swap_timeout", columnList = "timeout_at")
})
public class AtomicSwapStateEntity extends PanacheEntity {

    /**
     * Unique transaction identifier
     * Links to BridgeTransactionEntity.transactionId
     */
    @NotBlank
    @Column(name = "transaction_id", nullable = false, unique = true, length = 64)
    public String transactionId;

    /**
     * HTLC hash (SHA-256 of the secret)
     * This is the cryptographic commitment that both parties use
     */
    @NotBlank
    @Column(name = "htlc_hash", nullable = false, length = 64)
    public String htlcHash;

    /**
     * HTLC secret (revealed by initiator to claim funds)
     * Null until revealed, then both parties can use it
     */
    @Column(name = "htlc_secret", length = 64)
    public String htlcSecret;

    /**
     * Lock time in Unix epoch milliseconds
     * Funds are locked until this time
     */
    @NotNull
    @Column(name = "lock_time", nullable = false)
    public Long lockTime;

    /**
     * Timeout timestamp (when HTLC can be refunded)
     * Typically 24-48 hours from creation
     */
    @NotNull
    @Column(name = "timeout_at", nullable = false)
    public LocalDateTime timeoutAt;

    /**
     * Current atomic swap status
     * Lifecycle: INITIATED -> LOCKED -> REVEALED -> COMPLETED
     * Or: INITIATED -> LOCKED -> (timeout) -> REFUNDED
     */
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "swap_status", nullable = false, length = 32)
    public AtomicSwapStatus swapStatus;

    /**
     * Swap amount in token base units
     */
    @NotNull
    @Positive
    @Column(name = "amount", nullable = false, precision = 36, scale = 18)
    public BigDecimal amount;

    /**
     * Initiator's address (creates the HTLC)
     */
    @NotBlank
    @Column(name = "initiator_address", nullable = false, length = 128)
    public String initiatorAddress;

    /**
     * Participant's address (responds to the HTLC)
     */
    @NotBlank
    @Column(name = "participant_address", nullable = false, length = 128)
    public String participantAddress;

    /**
     * Source blockchain identifier
     */
    @Column(name = "source_chain", length = 32)
    public String sourceChain;

    /**
     * Target blockchain identifier
     */
    @Column(name = "target_chain", length = 32)
    public String targetChain;

    /**
     * Source chain HTLC contract address
     */
    @Column(name = "source_contract_address", length = 128)
    public String sourceContractAddress;

    /**
     * Target chain HTLC contract address
     */
    @Column(name = "target_contract_address", length = 128)
    public String targetContractAddress;

    /**
     * Source chain transaction hash (for HTLC lock)
     */
    @Column(name = "source_lock_tx_hash", length = 128)
    public String sourceLockTxHash;

    /**
     * Target chain transaction hash (for HTLC lock)
     */
    @Column(name = "target_lock_tx_hash", length = 128)
    public String targetLockTxHash;

    /**
     * Source chain redeem transaction hash
     */
    @Column(name = "source_redeem_tx_hash", length = 128)
    public String sourceRedeemTxHash;

    /**
     * Target chain redeem transaction hash
     */
    @Column(name = "target_redeem_tx_hash", length = 128)
    public String targetRedeemTxHash;

    /**
     * Timestamp when atomic swap was created
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    public LocalDateTime createdAt;

    /**
     * Timestamp when atomic swap was last updated
     */
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    public LocalDateTime updatedAt;

    /**
     * Timestamp when swap completed or was refunded
     */
    @Column(name = "completed_at")
    public LocalDateTime completedAt;

    /**
     * Optimistic locking version for concurrency control
     */
    @Version
    @Column(name = "version")
    public Long version;

    // =========================================================================
    // Panache Query Helper Methods
    // =========================================================================

    /**
     * Find atomic swap by transaction ID
     *
     * @param txId Transaction identifier
     * @return AtomicSwapStateEntity or null if not found
     */
    public static AtomicSwapStateEntity findByTransactionId(String txId) {
        return find("transactionId", txId).firstResult();
    }

    /**
     * Find atomic swap by HTLC hash
     * Used when secret is revealed to find corresponding swap
     *
     * @param hash HTLC hash (SHA-256)
     * @return AtomicSwapStateEntity or null if not found
     */
    public static AtomicSwapStateEntity findByHtlcHash(String hash) {
        return find("htlcHash", hash).firstResult();
    }

    /**
     * Find expired atomic swaps eligible for refund
     * Returns swaps in LOCKED status past their timeout
     *
     * @return List of expired swaps
     */
    public static List<AtomicSwapStateEntity> findExpiredSwaps() {
        LocalDateTime now = LocalDateTime.now();
        return find("swapStatus = ?1 and timeoutAt < ?2",
            AtomicSwapStatus.LOCKED,
            now)
            .list();
    }

    /**
     * Find active atomic swaps
     * Returns swaps in INITIATED or LOCKED status
     *
     * @return List of active swaps
     */
    public static List<AtomicSwapStateEntity> findActiveSwaps() {
        return find("swapStatus in (?1, ?2)",
            AtomicSwapStatus.INITIATED,
            AtomicSwapStatus.LOCKED)
            .list();
    }

    /**
     * Find swaps by status
     *
     * @param status Status to filter by
     * @return List of swaps with given status
     */
    public static List<AtomicSwapStateEntity> findByStatus(AtomicSwapStatus status) {
        return find("swapStatus = ?1 order by createdAt desc", status).list();
    }

    /**
     * Find swaps by initiator address
     *
     * @param address Initiator's address
     * @return List of swaps initiated by the address
     */
    public static List<AtomicSwapStateEntity> findByInitiator(String address) {
        return find("initiatorAddress = ?1 order by createdAt desc", address).list();
    }

    /**
     * Find swaps by participant address
     *
     * @param address Participant's address
     * @return List of swaps where address is participant
     */
    public static List<AtomicSwapStateEntity> findByParticipant(String address) {
        return find("participantAddress = ?1 order by createdAt desc", address).list();
    }

    /**
     * Find swaps expiring soon
     * Returns swaps that will timeout within the specified minutes
     *
     * @param minutesUntilExpiry Time window in minutes
     * @return List of soon-to-expire swaps
     */
    public static List<AtomicSwapStateEntity> findExpiringSoon(int minutesUntilExpiry) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime cutoff = now.plusMinutes(minutesUntilExpiry);
        return find("swapStatus = ?1 and timeoutAt between ?2 and ?3",
            AtomicSwapStatus.LOCKED,
            now,
            cutoff)
            .list();
    }

    /**
     * Find swaps with revealed secrets
     * Returns swaps in REVEALED status
     *
     * @return List of swaps with revealed secrets
     */
    public static List<AtomicSwapStateEntity> findWithRevealedSecrets() {
        return find("swapStatus = ?1 and htlcSecret is not null",
            AtomicSwapStatus.REVEALED)
            .list();
    }

    /**
     * Count swaps by status
     *
     * @param status Status to count
     * @return Number of swaps with given status
     */
    public static long countByStatus(AtomicSwapStatus status) {
        return count("swapStatus", status);
    }

    /**
     * Find swaps between specific chains
     *
     * @param source Source chain identifier
     * @param target Target chain identifier
     * @return List of swaps between the chains
     */
    public static List<AtomicSwapStateEntity> findByChainPair(String source, String target) {
        return find("sourceChain = ?1 and targetChain = ?2 order by createdAt desc",
            source, target).list();
    }

    // =========================================================================
    // Utility Methods
    // =========================================================================

    /**
     * Check if HTLC has expired
     *
     * @return true if past timeout
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(timeoutAt);
    }

    /**
     * Check if HTLC secret has been revealed
     *
     * @return true if secret is known
     */
    public boolean isSecretRevealed() {
        return htlcSecret != null && !htlcSecret.isEmpty();
    }

    /**
     * Check if swap is in a final state
     *
     * @return true if completed or refunded
     */
    public boolean isFinal() {
        return swapStatus == AtomicSwapStatus.COMPLETED ||
               swapStatus == AtomicSwapStatus.REFUNDED ||
               swapStatus == AtomicSwapStatus.EXPIRED;
    }

    /**
     * Check if swap can be redeemed
     * Requires secret revealed and not expired
     *
     * @return true if swap can be redeemed
     */
    public boolean canRedeem() {
        return isSecretRevealed() &&
               !isExpired() &&
               (swapStatus == AtomicSwapStatus.REVEALED || swapStatus == AtomicSwapStatus.LOCKED);
    }

    /**
     * Check if swap can be refunded
     * Requires expired and not yet completed
     *
     * @return true if swap can be refunded
     */
    public boolean canRefund() {
        return isExpired() &&
               !isFinal() &&
               swapStatus == AtomicSwapStatus.LOCKED;
    }

    /**
     * Get time remaining until expiry in minutes
     *
     * @return Minutes until timeout (negative if expired)
     */
    public long getMinutesUntilExpiry() {
        LocalDateTime now = LocalDateTime.now();
        return java.time.Duration.between(now, timeoutAt).toMinutes();
    }

    @Override
    public String toString() {
        return String.format("AtomicSwap[tx=%s, %s<->%s, %s %s, status=%s, expires=%s]",
            transactionId,
            initiatorAddress.substring(0, Math.min(10, initiatorAddress.length())),
            participantAddress.substring(0, Math.min(10, participantAddress.length())),
            amount,
            sourceChain,
            swapStatus,
            timeoutAt);
    }
}

/**
 * Atomic Swap Status Enumeration
 *
 * Defines the lifecycle states of an HTLC-based atomic swap
 */
enum AtomicSwapStatus {
    /**
     * Swap initiated, HTLC hash generated
     */
    INITIATED,

    /**
     * Funds locked in HTLC contracts on both chains
     */
    LOCKED,

    /**
     * Secret revealed by initiator
     */
    REVEALED,

    /**
     * Swap completed successfully (both parties claimed funds)
     */
    COMPLETED,

    /**
     * Swap refunded (timeout expired, funds returned)
     */
    REFUNDED,

    /**
     * Swap expired without completion or refund
     */
    EXPIRED
}
