package io.aurigraph.v11.token.secondary;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Validator Vote Entity
 *
 * Represents a single validator vote in a VVB approval request.
 * Each validator can vote once per approval request (unique constraint).
 *
 * Voting Details:
 * - voteId: Unique identifier for this vote
 * - approvalRequestId: Links to the parent approval request
 * - validatorId: Identifier of the voting validator
 * - vote: The choice (YES, NO, ABSTAIN)
 * - signature: Cryptographic signature of vote (optional, for auditability)
 * - reason: Optional justification for the vote
 * - votedAt: Timestamp when vote was cast
 *
 * Constraints:
 * - One vote per (approvalRequestId, validatorId) pair
 * - Cannot vote after approval request expires
 * - Immutable after creation (no vote changes allowed)
 *
 * Performance Target: <1ms vote lookup, >1,000 votes/sec
 *
 * @version 12.0.0
 * @since December 23, 2025
 */
@Entity
@Table(name = "validator_votes", indexes = {
        @Index(name = "idx_vote_id", columnList = "id"),
        @Index(name = "idx_vote_request_id", columnList = "approval_request_id"),
        @Index(name = "idx_vote_validator_id", columnList = "validator_id"),
        @Index(name = "idx_vote_choice", columnList = "vote"),
        @Index(name = "idx_vote_request_validator", columnList = "approval_request_id, validator_id"),
        @Index(name = "idx_vote_voted_at", columnList = "voted_at")
},
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_vote_request_validator",
                        columnNames = {"approval_request_id", "validator_id"}
                )
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ValidatorVote extends PanacheEntity {

    // =========================================================================
    // Core Identity Fields
    // =========================================================================

    /**
     * UUID identifier for this vote.
     * Unique across all votes.
     */
    @NotNull
    @Column(columnDefinition = "UUID", nullable = false)
    public UUID voteId;

    /**
     * Reference to the parent approval request.
     * Each vote belongs to exactly one approval request.
     * Foreign key to VVBApprovalRequest.
     */
    @NotNull
    @Column(name = "approval_request_id", columnDefinition = "UUID", nullable = false)
    public UUID approvalRequestId;

    /**
     * Identifier of the validator casting this vote.
     * Examples: "validator-1", "vvb-member-xyz", "3HHS...k2Ws" (address-based).
     */
    @NotNull
    @Column(name = "validator_id", length = 256, nullable = false)
    public String validatorId;

    // =========================================================================
    // Vote Content
    // =========================================================================

    /**
     * The validator's vote choice.
     * YES = approval, NO = rejection, ABSTAIN = abstention.
     */
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "vote", nullable = false, length = 20)
    public VoteChoice vote;

    /**
     * Cryptographic signature of this vote.
     * Format: typically hex-encoded signature (e.g., ECDSA or Ed25519).
     * Optional for in-memory voting; required for on-chain verification.
     * Length supports 256-byte signatures (512 hex chars).
     */
    @Column(name = "signature", columnDefinition = "TEXT")
    public String signature;

    /**
     * Optional reason or justification for this vote.
     * Examples: "Security concerns", "Insufficient testing", "Approved after review".
     * Can be null if validator chooses not to provide explanation.
     */
    @Column(name = "reason", columnDefinition = "TEXT")
    public String reason;

    // =========================================================================
    // Audit Timestamps
    // =========================================================================

    /**
     * Timestamp when this vote was cast.
     * Set automatically at vote creation; immutable after.
     */
    @NotNull
    @CreationTimestamp
    @Column(name = "voted_at", nullable = false, updatable = false)
    public LocalDateTime votedAt;

    // =========================================================================
    // Validation & Helper Methods
    // =========================================================================

    /**
     * Validate vote state and content.
     *
     * @throws IllegalStateException if validation fails
     */
    public void validate() {
        if (voteId == null) {
            throw new IllegalStateException("voteId cannot be null");
        }
        if (approvalRequestId == null) {
            throw new IllegalStateException("approvalRequestId cannot be null");
        }
        if (validatorId == null || validatorId.trim().isEmpty()) {
            throw new IllegalStateException("validatorId cannot be null or empty");
        }
        if (vote == null) {
            throw new IllegalStateException("vote cannot be null");
        }
        if (votedAt == null) {
            throw new IllegalStateException("votedAt cannot be null");
        }
    }

    /**
     * Check if vote has a cryptographic signature.
     *
     * @return true if signature field is not null/empty
     */
    public boolean hasSigned() {
        return signature != null && !signature.trim().isEmpty();
    }

    /**
     * Check if vote includes a reason/justification.
     *
     * @return true if reason field is not null/empty
     */
    public boolean hasReason() {
        return reason != null && !reason.trim().isEmpty();
    }

    /**
     * Panache query: Find vote by vote ID.
     *
     * @param voteId the vote ID
     * @return the vote or null
     */
    public static ValidatorVote findByVoteId(UUID voteId) {
        return find("voteId = ?1", voteId).firstResult();
    }

    /**
     * Panache query: Find vote by approval request and validator.
     *
     * @param approvalRequestId the approval request ID
     * @param validatorId the validator ID
     * @return the vote or null
     */
    public static ValidatorVote findByRequestAndValidator(UUID approvalRequestId, String validatorId) {
        return find("approvalRequestId = ?1 and validatorId = ?2", approvalRequestId, validatorId)
                .firstResult();
    }

    /**
     * Panache query: Find all votes for an approval request.
     *
     * @param approvalRequestId the approval request ID
     * @return list of votes for the request
     */
    public static java.util.List<ValidatorVote> findByApprovalRequest(UUID approvalRequestId) {
        return find("approvalRequestId = ?1 order by votedAt asc", approvalRequestId).list();
    }

    /**
     * Panache query: Count votes for an approval request by choice.
     *
     * @param approvalRequestId the approval request ID
     * @param choice the vote choice to count
     * @return count of votes with the given choice
     */
    public static long countByRequestAndChoice(UUID approvalRequestId, VoteChoice choice) {
        return count("approvalRequestId = ?1 and vote = ?2", approvalRequestId, choice);
    }

    /**
     * Panache query: Find all votes by a specific validator.
     *
     * @param validatorId the validator ID
     * @return list of votes cast by the validator
     */
    public static java.util.List<ValidatorVote> findByValidator(String validatorId) {
        return find("validatorId = ?1 order by votedAt desc", validatorId).list();
    }

    /**
     * Panache query: Check if validator has already voted.
     *
     * @param approvalRequestId the approval request ID
     * @param validatorId the validator ID
     * @return true if validator has voted in this request
     */
    public static boolean hasVoted(UUID approvalRequestId, String validatorId) {
        return find("approvalRequestId = ?1 and validatorId = ?2", approvalRequestId, validatorId)
                .count() > 0;
    }

    @Override
    public String toString() {
        return String.format(
            "ValidatorVote[id=%s, request=%s, validator=%s, vote=%s, votedAt=%s%s]",
            voteId, approvalRequestId, validatorId, vote, votedAt,
            hasReason() ? ", reason=" + reason.substring(0, Math.min(20, reason.length())) + "..." : ""
        );
    }
}
