package io.aurigraph.v11.bridge.persistence;

import io.aurigraph.v11.bridge.BridgeTransactionStatus;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Panache Repository for BridgeTransactionEntity persistence operations.
 *
 * Provides high-level CRUD and query operations for bridge transactions
 * using Panache ORM patterns. This repository handles all database
 * interactions for bridge transaction lifecycle management.
 *
 * Key Features:
 * - Transaction lifecycle management (PENDING -> COMPLETED/FAILED)
 * - Stuck transfer detection and recovery
 * - Multi-signature validation tracking
 * - Optimistic locking for concurrent access control
 * - Query optimization via comprehensive indexing
 *
 * Usage Example:
 * <pre>
 * @Inject
 * BridgeTransactionRepository repository;
 *
 * // Create and persist
 * BridgeTransactionEntity tx = new BridgeTransactionEntity();
 * tx.transactionId = "BRIDGE-12345";
 * tx.sourceChain = "ethereum";
 * // ... set other fields ...
 * repository.persist(tx);
 *
 * // Query by ID
 * BridgeTransactionEntity found = repository.findByTransactionId("BRIDGE-12345");
 *
 * // Find pending transfers for recovery
 * List<BridgeTransactionEntity> pending = repository.findPendingTransfers(30);
 * </pre>
 *
 * @author BDA + IBA
 * @version 11.1.0
 * @since Sprint 14
 */
@ApplicationScoped
public class BridgeTransactionRepository implements PanacheRepository<BridgeTransactionEntity> {

    /**
     * Find transaction by unique transaction ID
     *
     * @param transactionId The business key transaction identifier
     * @return Optional containing the transaction or empty if not found
     */
    public Optional<BridgeTransactionEntity> findOptionalByTransactionId(String transactionId) {
        return find("transactionId", transactionId).firstResultOptional();
    }

    /**
     * Find all pending transfers that haven't been confirmed
     * Used for periodic recovery and monitoring
     *
     * @param olderThanMinutes Timeout threshold - find transfers older than this
     * @return List of transactions in PENDING status older than threshold
     */
    public List<BridgeTransactionEntity> findPendingTransfers(int olderThanMinutes) {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(olderThanMinutes);
        return find("status = ?1 and createdAt < ?2 order by createdAt asc",
                BridgeTransactionStatus.PENDING,
                cutoff).list();
    }

    /**
     * Find all stuck transfers (PENDING or CONFIRMING beyond timeout)
     * Critical for identifying transfers that need intervention
     *
     * @param timeoutMinutes Timeout threshold in minutes
     * @return List of stuck transfers requiring recovery
     */
    @Transactional
    public List<BridgeTransactionEntity> findStuckTransfers(int timeoutMinutes) {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(timeoutMinutes);
        return find("status in (?1, ?2) and createdAt < ?3 order by createdAt asc",
                BridgeTransactionStatus.PENDING,
                BridgeTransactionStatus.CONFIRMING,
                cutoff).list();
    }

    /**
     * Find all transactions with a specific status
     *
     * @param status The status to filter by
     * @return List of transactions with the given status
     */
    public List<BridgeTransactionEntity> findByStatus(BridgeTransactionStatus status) {
        return find("status = ?1 order by createdAt desc", status).list();
    }

    /**
     * Find transactions by source or target address
     * Used for user transaction history queries
     *
     * @param address The wallet address to search for
     * @return List of transactions involving this address
     */
    public List<BridgeTransactionEntity> findByAddress(String address) {
        return find("sourceAddress = ?1 or targetAddress = ?1 order by createdAt desc", address).list();
    }

    /**
     * Find transactions by source chain
     *
     * @param sourceChain The source blockchain identifier
     * @return List of transactions from the source chain
     */
    public List<BridgeTransactionEntity> findBySourceChain(String sourceChain) {
        return find("sourceChain = ?1 order by createdAt desc", sourceChain).list();
    }

    /**
     * Find transactions by target chain
     *
     * @param targetChain The target blockchain identifier
     * @return List of transactions to the target chain
     */
    public List<BridgeTransactionEntity> findByTargetChain(String targetChain) {
        return find("targetChain = ?1 order by createdAt desc", targetChain).list();
    }

    /**
     * Find transactions between specific chain pairs
     *
     * @param sourceChain Source blockchain
     * @param targetChain Target blockchain
     * @return List of transactions for the chain pair
     */
    public List<BridgeTransactionEntity> findByChainPair(String sourceChain, String targetChain) {
        return find("sourceChain = ?1 and targetChain = ?2 order by createdAt desc",
                sourceChain, targetChain).list();
    }

    /**
     * Find failed transactions eligible for retry
     * Returns transactions that haven't exceeded max retry count
     *
     * @return List of transactions that can be retried
     */
    public List<BridgeTransactionEntity> findRetryableTransfers() {
        return find("status = ?1 and retryCount < maxRetries order by createdAt asc",
                BridgeTransactionStatus.FAILED).list();
    }

    /**
     * Find transactions requiring multi-signature validation
     * Returns PENDING transactions without validator approval
     *
     * @return List of transactions awaiting validator quorum
     */
    public List<BridgeTransactionEntity> findPendingValidation() {
        return find("status = ?1 and multiSigValidated = false order by createdAt asc",
                BridgeTransactionStatus.PENDING).list();
    }

    /**
     * Find active atomic swaps (HTLC-based transactions in progress)
     *
     * @return List of active atomic swap transactions
     */
    public List<BridgeTransactionEntity> findActiveAtomicSwaps() {
        return find("transactionType = ?1 and status in (?2, ?3) order by createdAt asc",
                io.aurigraph.v11.bridge.BridgeTransactionType.ATOMIC_SWAP,
                BridgeTransactionStatus.PENDING,
                BridgeTransactionStatus.CONFIRMING).list();
    }

    /**
     * Count transactions by status
     * Used for monitoring and statistics
     *
     * @param status The status to count
     * @return Number of transactions with the given status
     */
    public long countByStatus(BridgeTransactionStatus status) {
        return count("status", status);
    }

    /**
     * Get total volume of completed transfers
     *
     * @return Total amount of successfully transferred funds
     */
    public java.math.BigDecimal getTotalCompletedVolume() {
        List<BridgeTransactionEntity> completed = find("status = ?1",
                BridgeTransactionStatus.COMPLETED).list();
        return completed.stream()
                .map(tx -> tx.amount)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
    }

    /**
     * Get statistics for a specific time window
     *
     * @param minutesAgo Number of minutes to look back
     * @return Transaction statistics for the time period
     */
    public BridgeTransactionStats getStatsForWindow(int minutesAgo) {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(minutesAgo);
        List<BridgeTransactionEntity> transactions = find(
                "createdAt >= ?1 order by createdAt asc", cutoff).list();

        BridgeTransactionStats stats = new BridgeTransactionStats();
        stats.totalTransactions = transactions.size();
        stats.successfulTransactions = (int) transactions.stream()
                .filter(tx -> tx.status == BridgeTransactionStatus.COMPLETED)
                .count();
        stats.failedTransactions = (int) transactions.stream()
                .filter(tx -> tx.status == BridgeTransactionStatus.FAILED)
                .count();
        stats.pendingTransactions = (int) transactions.stream()
                .filter(tx -> tx.status == BridgeTransactionStatus.PENDING)
                .count();
        stats.totalVolume = transactions.stream()
                .map(tx -> tx.amount)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);

        return stats;
    }

    /**
     * Update transaction status with optimistic locking
     * Handles concurrent modification safely
     *
     * @param transactionId The transaction ID to update
     * @param newStatus The new status
     * @return true if update successful, false if version conflict
     */
    @Transactional
    public boolean updateStatus(String transactionId, BridgeTransactionStatus newStatus) {
        try {
            BridgeTransactionEntity tx = find("transactionId", transactionId).firstResult();
            if (tx == null) {
                return false;
            }
            tx.status = newStatus;
            tx.updatedAt = LocalDateTime.now();
            persist(tx);
            return true;
        } catch (jakarta.persistence.OptimisticLockException e) {
            return false; // Version conflict detected
        }
    }

    /**
     * Mark transaction as completed
     * Records the completion timestamp for SLA tracking
     *
     * @param transactionId The transaction ID
     * @return true if successful, false if not found or conflict
     */
    @Transactional
    public boolean markCompleted(String transactionId) {
        BridgeTransactionEntity tx = find("transactionId", transactionId).firstResult();
        if (tx == null) {
            return false;
        }
        tx.status = BridgeTransactionStatus.COMPLETED;
        tx.completedAt = LocalDateTime.now();
        tx.updatedAt = LocalDateTime.now();
        tx.multiSigValidated = true;
        persist(tx);
        return true;
    }

    /**
     * Mark transaction as failed with error message
     *
     * @param transactionId The transaction ID
     * @param errorMessage Detailed error message
     * @return true if successful, false if not found or conflict
     */
    @Transactional
    public boolean markFailed(String transactionId, String errorMessage) {
        BridgeTransactionEntity tx = find("transactionId", transactionId).firstResult();
        if (tx == null) {
            return false;
        }
        tx.status = BridgeTransactionStatus.FAILED;
        tx.errorMessage = errorMessage;
        tx.completedAt = LocalDateTime.now();
        tx.updatedAt = LocalDateTime.now();
        persist(tx);
        return true;
    }

    /**
     * Increment retry count and prepare for next attempt
     *
     * @param transactionId The transaction ID
     * @return true if successful and can retry, false if max retries exceeded
     */
    @Transactional
    public boolean incrementRetry(String transactionId) {
        BridgeTransactionEntity tx = find("transactionId", transactionId).firstResult();
        if (tx == null || tx.retryCount >= tx.maxRetries) {
            return false;
        }
        tx.retryCount++;
        tx.status = BridgeTransactionStatus.PENDING; // Reset to PENDING for retry
        tx.updatedAt = LocalDateTime.now();
        persist(tx);
        return true;
    }

    /**
     * Update validator signatures on transaction
     * Called when multi-sig validation is performed
     *
     * @param transactionId The transaction ID
     * @param validatorCount Number of validators who signed
     * @param multiSigValidated Whether quorum was reached
     * @return true if successful, false if not found
     */
    @Transactional
    public boolean updateValidation(String transactionId, int validatorCount, boolean multiSigValidated) {
        BridgeTransactionEntity tx = find("transactionId", transactionId).firstResult();
        if (tx == null) {
            return false;
        }
        tx.validatorCount = validatorCount;
        tx.multiSigValidated = multiSigValidated;
        tx.updatedAt = LocalDateTime.now();
        persist(tx);
        return true;
    }

    /**
     * Delete old completed transactions (for archiving)
     * Call periodically to clean up database
     *
     * @param olderThanDays Delete transactions completed more than this many days ago
     * @return Number of transactions deleted
     */
    @Transactional
    public long deleteOldCompleted(int olderThanDays) {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(olderThanDays);
        return delete("status = ?1 and completedAt < ?2",
                BridgeTransactionStatus.COMPLETED,
                cutoff);
    }

    /**
     * Transaction statistics DTO for reporting
     */
    public static class BridgeTransactionStats {
        public int totalTransactions;
        public int successfulTransactions;
        public int failedTransactions;
        public int pendingTransactions;
        public java.math.BigDecimal totalVolume;

        public double getSuccessRate() {
            if (totalTransactions == 0) {
                return 0.0;
            }
            return (successfulTransactions * 100.0) / totalTransactions;
        }

        @Override
        public String toString() {
            return String.format(
                    "BridgeStats[total=%d, success=%d, failed=%d, pending=%d, rate=%.1f%%, volume=%s]",
                    totalTransactions, successfulTransactions, failedTransactions,
                    pendingTransactions, getSuccessRate(), totalVolume);
        }
    }
}
