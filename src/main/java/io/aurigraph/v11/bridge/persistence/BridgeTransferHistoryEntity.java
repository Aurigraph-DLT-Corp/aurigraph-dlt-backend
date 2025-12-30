package io.aurigraph.v11.bridge.persistence;

import io.aurigraph.v11.bridge.BridgeTransactionStatus;
import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Bridge Transfer History Entity - Immutable Audit Trail
 *
 * Provides a complete, immutable audit trail for all bridge transaction state
 * transitions. Every status change creates a new history entry that cannot be
 * modified or deleted.
 *
 * This entity is critical for:
 * - Forensic analysis of failed transactions
 * - Regulatory compliance and audit requirements
 * - Debugging and troubleshooting
 * - Multi-signature validation tracking
 *
 * @author BDA + IBA
 * @version 11.1.0
 * @since Sprint 14
 */
@Entity
@Table(name = "bridge_transfer_history", indexes = {
    @Index(name = "idx_history_tx_id", columnList = "transaction_id"),
    @Index(name = "idx_history_timestamp", columnList = "timestamp")
})
public class BridgeTransferHistoryEntity extends PanacheEntity {

    /**
     * Transaction ID this history entry belongs to
     * Foreign key reference to bridge_transactions.transaction_id
     */
    @NotBlank
    @Column(name = "transaction_id", nullable = false, length = 64)
    public String transactionId;

    /**
     * Previous status (null for initial creation)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", length = 32)
    public BridgeTransactionStatus fromStatus;

    /**
     * New status after transition
     */
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false, length = 32)
    public BridgeTransactionStatus toStatus;

    /**
     * Human-readable reason for the state transition
     * Examples:
     * - "Validator quorum reached (4/7)"
     * - "Confirmations received: 12/12"
     * - "HTLC timeout expired"
     * - "Manual retry initiated"
     */
    @Column(name = "reason", length = 512)
    public String reason;

    /**
     * Detailed error information if transition was due to failure
     * Stored as TEXT for extensive error context including stack traces
     */
    @Column(name = "error_details", columnDefinition = "TEXT")
    public String errorDetails;

    /**
     * Validator signatures at time of transition (for multi-sig validation)
     * Stored as JSON array: ["validator-1:sig1", "validator-2:sig2", ...]
     * This provides cryptographic proof of validator consensus
     */
    @Column(name = "validator_signatures", columnDefinition = "TEXT")
    public String validatorSignatures;

    /**
     * Timestamp of the state transition
     * Immutable - set once during creation
     */
    @NotNull
    @Column(name = "timestamp", nullable = false, updatable = false)
    public LocalDateTime timestamp;

    /**
     * Agent/service that initiated the state change
     * Examples:
     * - "CrossChainBridgeService"
     * - "ScheduledRecoveryService"
     * - "ManualIntervention"
     * - "ValidatorNode-3"
     */
    @Column(name = "agent", length = 64)
    public String agent;

    /**
     * Additional metadata in JSON format (optional)
     * Can store transaction-specific context like:
     * - Block numbers
     * - Gas costs
     * - Network conditions
     */
    @Column(name = "metadata", columnDefinition = "TEXT")
    public String metadata;

    // =========================================================================
    // Panache Query Helper Methods
    // =========================================================================

    /**
     * Find all history entries for a transaction
     * Ordered by timestamp descending (newest first)
     *
     * @param txId Transaction ID to lookup
     * @return List of history entries for the transaction
     */
    public static List<BridgeTransferHistoryEntity> findByTransactionId(String txId) {
        return find("transactionId = ?1 order by timestamp desc", txId).list();
    }

    /**
     * Find history entries for a transaction ordered chronologically
     * Useful for sequential analysis of transaction lifecycle
     *
     * @param txId Transaction ID to lookup
     * @return List of history entries in chronological order
     */
    public static List<BridgeTransferHistoryEntity> findByTransactionIdChronological(String txId) {
        return find("transactionId = ?1 order by timestamp asc", txId).list();
    }

    /**
     * Find all transitions to a specific status
     * Used for analyzing patterns (e.g., all failed transactions)
     *
     * @param status Target status to filter by
     * @return List of history entries where toStatus matches
     */
    public static List<BridgeTransferHistoryEntity> findByToStatus(BridgeTransactionStatus status) {
        return find("toStatus = ?1 order by timestamp desc", status).list();
    }

    /**
     * Find all transitions from a specific status
     * Used for analyzing state machine behavior
     *
     * @param status Source status to filter by
     * @return List of history entries where fromStatus matches
     */
    public static List<BridgeTransferHistoryEntity> findByFromStatus(BridgeTransactionStatus status) {
        return find("fromStatus = ?1 order by timestamp desc", status).list();
    }

    /**
     * Find recent history entries within time window
     * Used for real-time monitoring and debugging
     *
     * @param minutesAgo Time window in minutes
     * @return List of recent history entries
     */
    public static List<BridgeTransferHistoryEntity> findRecentHistory(int minutesAgo) {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(minutesAgo);
        return find("timestamp >= ?1 order by timestamp desc", cutoff).list();
    }

    /**
     * Find all failure transitions
     * Returns history entries where toStatus is FAILED
     *
     * @return List of failure history entries
     */
    public static List<BridgeTransferHistoryEntity> findFailures() {
        return find("toStatus = ?1 order by timestamp desc",
            BridgeTransactionStatus.FAILED).list();
    }

    /**
     * Find history entries by agent
     * Used for tracking actions by specific services or operators
     *
     * @param agentName Agent identifier to filter by
     * @return List of history entries from the agent
     */
    public static List<BridgeTransferHistoryEntity> findByAgent(String agentName) {
        return find("agent = ?1 order by timestamp desc", agentName).list();
    }

    /**
     * Count total state transitions
     *
     * @return Total number of history entries
     */
    public static long countTransitions() {
        return count();
    }

    /**
     * Count transitions to specific status
     *
     * @param status Target status to count
     * @return Number of transitions to the status
     */
    public static long countByToStatus(BridgeTransactionStatus status) {
        return count("toStatus", status);
    }

    /**
     * Find history entries with error details
     * Returns only entries that have error information
     *
     * @return List of history entries with errors
     */
    public static List<BridgeTransferHistoryEntity> findWithErrors() {
        return find("errorDetails is not null order by timestamp desc").list();
    }

    /**
     * Find history entries with validator signatures
     * Returns entries that include multi-sig validation data
     *
     * @return List of history entries with validator signatures
     */
    public static List<BridgeTransferHistoryEntity> findWithValidatorSignatures() {
        return find("validatorSignatures is not null order by timestamp desc").list();
    }

    // =========================================================================
    // Utility Methods
    // =========================================================================

    /**
     * Create a history entry for a state transition
     * Factory method for consistent history entry creation
     *
     * @param txId Transaction ID
     * @param from Previous status (null for initial creation)
     * @param to New status
     * @param reason Human-readable reason
     * @param agent Agent initiating the change
     * @return New history entity (not persisted)
     */
    public static BridgeTransferHistoryEntity createTransition(
        String txId,
        BridgeTransactionStatus from,
        BridgeTransactionStatus to,
        String reason,
        String agent
    ) {
        BridgeTransferHistoryEntity history = new BridgeTransferHistoryEntity();
        history.transactionId = txId;
        history.fromStatus = from;
        history.toStatus = to;
        history.reason = reason;
        history.agent = agent;
        history.timestamp = LocalDateTime.now();
        return history;
    }

    /**
     * Create a history entry with error details
     *
     * @param txId Transaction ID
     * @param from Previous status
     * @param to New status
     * @param reason Human-readable reason
     * @param errorDetails Detailed error information
     * @param agent Agent initiating the change
     * @return New history entity (not persisted)
     */
    public static BridgeTransferHistoryEntity createFailureTransition(
        String txId,
        BridgeTransactionStatus from,
        BridgeTransactionStatus to,
        String reason,
        String errorDetails,
        String agent
    ) {
        BridgeTransferHistoryEntity history = createTransition(txId, from, to, reason, agent);
        history.errorDetails = errorDetails;
        return history;
    }

    /**
     * Create a history entry with validator signatures
     *
     * @param txId Transaction ID
     * @param from Previous status
     * @param to New status
     * @param reason Human-readable reason
     * @param validatorSigs JSON array of validator signatures
     * @param agent Agent initiating the change
     * @return New history entity (not persisted)
     */
    public static BridgeTransferHistoryEntity createValidatedTransition(
        String txId,
        BridgeTransactionStatus from,
        BridgeTransactionStatus to,
        String reason,
        String validatorSigs,
        String agent
    ) {
        BridgeTransferHistoryEntity history = createTransition(txId, from, to, reason, agent);
        history.validatorSignatures = validatorSigs;
        return history;
    }

    /**
     * Check if this is a failure transition
     *
     * @return true if toStatus is FAILED
     */
    public boolean isFailure() {
        return toStatus == BridgeTransactionStatus.FAILED;
    }

    /**
     * Check if this is a completion transition
     *
     * @return true if toStatus is COMPLETED
     */
    public boolean isCompletion() {
        return toStatus == BridgeTransactionStatus.COMPLETED;
    }

    /**
     * Check if this transition involved validator signatures
     *
     * @return true if validator signatures are present
     */
    public boolean hasValidatorSignatures() {
        return validatorSignatures != null && !validatorSignatures.isEmpty();
    }

    /**
     * Check if this transition has error details
     *
     * @return true if error details are present
     */
    public boolean hasErrorDetails() {
        return errorDetails != null && !errorDetails.isEmpty();
    }

    @Override
    public String toString() {
        return String.format("BridgeHistory[tx=%s, %s->%s, reason=%s, time=%s, agent=%s]",
            transactionId,
            fromStatus,
            toStatus,
            reason,
            timestamp,
            agent);
    }
}
