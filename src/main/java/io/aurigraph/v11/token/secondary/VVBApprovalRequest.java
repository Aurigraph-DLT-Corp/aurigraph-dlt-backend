package io.aurigraph.v11.token.secondary;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * VVB Approval Request Entity
 *
 * Represents a Virtual Validator Board (VVB) approval request for a secondary token version.
 * Manages the voting process with voting window, quorum, and Byzantine Fault Tolerance.
 *
 * Lifecycle:
 * - Created with initial validators and voting window
 * - Validators submit votes during voting window
 * - Consensus calculated after each vote (with early termination)
 * - Final status determined when consensus reached or voting window expires
 *
 * Byzantine Fault Tolerance:
 * - Requires >2/3 majority (>66.67%)
 * - Supports ABSTAIN votes (excluded from consensus)
 * - Can determine impossibility before voting window closes
 *
 * Performance Target: <10ms consensus calculation, >1,000 votes/sec
 *
 * @version 12.0.0
 * @since December 23, 2025
 */
@Entity
@Table(name = "vvb_approval_requests", indexes = {
        @Index(name = "idx_vvb_request_id", columnList = "id"),
        @Index(name = "idx_vvb_token_version_id", columnList = "token_version_id"),
        @Index(name = "idx_vvb_status", columnList = "status"),
        @Index(name = "idx_vvb_voting_window", columnList = "voting_window_end"),
        @Index(name = "idx_vvb_token_status", columnList = "token_version_id, status"),
        @Index(name = "idx_vvb_created_at", columnList = "created_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VVBApprovalRequest extends PanacheEntity {

    // =========================================================================
    // Core Identity Fields
    // =========================================================================

    /**
     * UUID identifier for this approval request.
     * Unique across all approval requests.
     */
    @NotNull
    @Column(columnDefinition = "UUID", nullable = false)
    public UUID requestId;

    /**
     * Reference to the secondary token version being approved.
     * Links this approval to the version lifecycle.
     */
    @NotNull
    @Column(name = "token_version_id", columnDefinition = "UUID", nullable = false)
    public UUID tokenVersionId;

    // =========================================================================
    // Voting Window Configuration
    // =========================================================================

    /**
     * Start time of the voting window.
     * Voting begins immediately after approval request creation.
     */
    @NotNull
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    public LocalDateTime createdAt;

    /**
     * End time of the voting window.
     * Voting closes at this time; approval window expires.
     * Calculated as: createdAt + votingWindowSeconds
     */
    @NotNull
    @Column(name = "voting_window_end", nullable = false)
    public LocalDateTime votingWindowEnd;

    /**
     * Voting window duration in seconds.
     * Typical values: 604,800 (7 days), 2,592,000 (30 days), 86,400 (1 day for testing).
     */
    @NotNull
    @Positive
    @Column(name = "voting_window_seconds", nullable = false)
    public Long votingWindowSeconds;

    // =========================================================================
    // Consensus Configuration
    // =========================================================================

    /**
     * Approval threshold percentage required for consensus.
     * Typically 66.67 (>2/3 majority for Byzantine FT).
     * Can be customized per token type (e.g., 75%, 80%, 90%).
     */
    @NotNull
    @Column(name = "approval_threshold", nullable = false)
    public Double approvalThreshold = 66.67;

    /**
     * Total number of validators eligible to vote.
     * Includes all validators even if they abstain.
     */
    @NotNull
    @Positive
    @Column(name = "total_validators", nullable = false)
    public Integer totalValidators;

    /**
     * Number of approval votes received so far.
     * Updated with each vote; used for consensus calculation.
     */
    @Column(name = "approval_count", nullable = false)
    public Integer approvalCount = 0;

    /**
     * Number of rejection votes received so far.
     */
    @Column(name = "rejection_count", nullable = false)
    public Integer rejectionCount = 0;

    /**
     * Number of abstain votes received so far.
     * Excluded from consensus calculation (enables Byzantine FT).
     */
    @Column(name = "abstain_count", nullable = false)
    public Integer abstainCount = 0;

    // =========================================================================
    // Status & Lifecycle
    // =========================================================================

    /**
     * Current status of this approval request.
     * Controls allowed operations and state transitions.
     */
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    public ApprovalStatus status = ApprovalStatus.PENDING;

    // =========================================================================
    // Merkle Proof & Integrity
    // =========================================================================

    /**
     * Merkle proof of the token version being approved.
     * Used to verify version integrity during voting.
     */
    @Column(name = "merkle_proof", columnDefinition = "TEXT")
    public String merkleProof;

    // =========================================================================
    // Audit Timestamps
    // =========================================================================

    /**
     * When this approval request was last updated.
     * Reflects the most recent vote or status change.
     */
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    public LocalDateTime updatedAt;

    // =========================================================================
    // Validation & Helper Methods
    // =========================================================================

    /**
     * Check if voting window is still open.
     *
     * @return true if current time is before votingWindowEnd
     */
    public boolean isVotingOpen() {
        return LocalDateTime.now().isBefore(votingWindowEnd);
    }

    /**
     * Check if voting window has expired.
     *
     * @return true if current time is after votingWindowEnd
     */
    public boolean hasExpired() {
        return LocalDateTime.now().isAfter(votingWindowEnd);
    }

    /**
     * Get remaining time in voting window (seconds).
     *
     * @return seconds remaining, or 0 if expired
     */
    public long getSecondsRemaining() {
        LocalDateTime now = LocalDateTime.now();
        if (now.isAfter(votingWindowEnd)) {
            return 0;
        }
        return java.time.temporal.ChronoUnit.SECONDS.between(now, votingWindowEnd);
    }

    /**
     * Get total votes received so far.
     *
     * @return approvalCount + rejectionCount + abstainCount
     */
    public int getTotalVotesReceived() {
        return approvalCount + rejectionCount + abstainCount;
    }

    /**
     * Get votes still needed from remaining validators.
     *
     * @return totalValidators - totalVotesReceived
     */
    public int getVotesRemaining() {
        return totalValidators - getTotalVotesReceived();
    }

    /**
     * Calculate current approval percentage.
     * Only considers active votes (not abstainers).
     *
     * @return approval percentage (0-100), or 0 if no active votes
     */
    public double getApprovalPercentage() {
        int activeVotes = approvalCount + rejectionCount;
        if (activeVotes == 0) {
            return 0.0;
        }
        return (approvalCount * 100.0) / activeVotes;
    }

    /**
     * Validate request state and configuration.
     *
     * @throws IllegalStateException if validation fails
     */
    public void validate() {
        if (requestId == null) {
            throw new IllegalStateException("requestId cannot be null");
        }
        if (tokenVersionId == null) {
            throw new IllegalStateException("tokenVersionId cannot be null");
        }
        if (totalValidators == null || totalValidators <= 0) {
            throw new IllegalStateException("totalValidators must be positive");
        }
        if (votingWindowSeconds == null || votingWindowSeconds <= 0) {
            throw new IllegalStateException("votingWindowSeconds must be positive");
        }
        if (approvalThreshold == null || approvalThreshold <= 0 || approvalThreshold > 100) {
            throw new IllegalStateException("approvalThreshold must be between 0 and 100");
        }
        if (votingWindowEnd == null) {
            throw new IllegalStateException("votingWindowEnd cannot be null");
        }
        if (status == null) {
            throw new IllegalStateException("status cannot be null");
        }

        // Verify vote counts don't exceed validators
        int totalVotes = getTotalVotesReceived();
        if (totalVotes > totalValidators) {
            throw new IllegalStateException("Total votes cannot exceed total validators");
        }
    }

    /**
     * Check if this request is in a terminal state.
     *
     * @return true if status is APPROVED, REJECTED, or EXPIRED
     */
    public boolean isTerminal() {
        return status == ApprovalStatus.APPROVED ||
               status == ApprovalStatus.REJECTED ||
               status == ApprovalStatus.EXPIRED;
    }

    /**
     * Panache query: Find by request ID.
     *
     * @param requestId the approval request ID
     * @return the approval request or null
     */
    public static VVBApprovalRequest findByRequestId(UUID requestId) {
        return find("requestId = ?1", requestId).firstResult();
    }

    /**
     * Panache query: Find by token version ID.
     *
     * @param tokenVersionId the secondary token version ID
     * @return the approval request or null
     */
    public static VVBApprovalRequest findByTokenVersionId(UUID tokenVersionId) {
        return find("tokenVersionId = ?1", tokenVersionId).firstResult();
    }

    /**
     * Panache query: Find all pending approval requests.
     *
     * @return list of pending requests
     */
    public static java.util.List<VVBApprovalRequest> findPending() {
        return find("status = ?1 order by createdAt asc", ApprovalStatus.PENDING).list();
    }

    /**
     * Panache query: Find all expired requests.
     *
     * @return list of expired requests
     */
    public static java.util.List<VVBApprovalRequest> findExpired() {
        return find("status = ?1 and votingWindowEnd < now() order by votingWindowEnd asc",
                ApprovalStatus.EXPIRED).list();
    }

    @Override
    public String toString() {
        return String.format(
            "VVBApprovalRequest[id=%s, tokenVersion=%s, status=%s, votes=%d/%d, " +
            "approval=%.2f%%, window=%s]",
            requestId, tokenVersionId, status, getTotalVotesReceived(), totalValidators,
            getApprovalPercentage(), votingWindowEnd
        );
    }
}
