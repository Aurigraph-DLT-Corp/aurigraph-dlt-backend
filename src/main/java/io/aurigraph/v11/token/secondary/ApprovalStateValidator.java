package io.aurigraph.v11.token.secondary;

import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * ApprovalStateValidator - Story 6 Component
 *
 * Validates state transitions and approval workflow invariants for secondary tokens.
 * Ensures that approval state changes follow valid transitions and maintain data integrity.
 *
 * Key Responsibilities:
 * - Validate state transition rules (CREATED → IN_VOTING → CONSENSUS_REACHED → EXECUTED/REJECTED)
 * - Check approval thresholds and quorum requirements
 * - Verify vote counts and deadline compliance
 * - Prevent invalid state combinations
 */
@ApplicationScoped
public class ApprovalStateValidator {

    @Inject
    VVBApprovalRegistry approvalRegistry;

    /**
     * Valid state transitions for approval workflow
     */
    private static final Set<String> VALID_STATE_TRANSITIONS = new HashSet<>();

    static {
        // From PENDING
        VALID_STATE_TRANSITIONS.add("PENDING->APPROVED");
        VALID_STATE_TRANSITIONS.add("PENDING->REJECTED");
        VALID_STATE_TRANSITIONS.add("PENDING->EXPIRED");

        // Terminal states
        // APPROVED, REJECTED, and EXPIRED don't have outgoing transitions
    }

    /**
     * Validates state transition for approval request
     * @param fromStatus Current status
     * @param toStatus Target status
     * @throws IllegalStateException if transition is invalid
     */
    public void validateStateTransition(ApprovalStatus fromStatus, ApprovalStatus toStatus) {
        String transition = fromStatus + "->" + toStatus;
        if (!VALID_STATE_TRANSITIONS.contains(transition)) {
            throw new IllegalStateException(
                String.format("Invalid state transition: %s -> %s", fromStatus, toStatus)
            );
        }
    }

    /**
     * Validates approval threshold has been met
     * @param approval The approval request
     * @param currentApproveCount Current number of approvals
     * @throws IllegalStateException if threshold not met
     */
    public void validateThresholdMet(VVBApprovalRequest approval, Integer currentApproveCount) {
        int requiredApprovals = calculateRequiredApprovals(approval.totalValidators);

        if (currentApproveCount < requiredApprovals) {
            throw new IllegalStateException(
                String.format(
                    "Approval threshold not met: %d/%d required approvals",
                    currentApproveCount,
                    requiredApprovals
                )
            );
        }
    }

    /**
     * Validates approval deadline compliance
     * @param approval The approval request
     * @throws IllegalStateException if deadline exceeded
     */
    public void validateDeadlineCompliance(VVBApprovalRequest approval) {
        if (approval.votingWindowEnd != null && LocalDateTime.now().isAfter(approval.votingWindowEnd)) {
            throw new IllegalStateException("Approval voting window has expired");
        }
    }

    /**
     * Validates approval consistency with version
     * @param approval The approval request
     * @param version The secondary token version
     * @throws IllegalStateException if inconsistent
     */
    public void validateApprovalVersionConsistency(VVBApprovalRequest approval, SecondaryTokenVersion version) {
        if (!approval.tokenVersionId.equals(version.id)) {
            throw new IllegalStateException(
                String.format(
                    "Approval version mismatch: approval for %s but version is %s",
                    approval.tokenVersionId,
                    version.id
                )
            );
        }
    }

    /**
     * Calculate required approvals based on validator count
     * Uses >2/3 threshold for consensus
     */
    private Integer calculateRequiredApprovals(Integer totalValidators) {
        if (totalValidators <= 0) {
            return 1; // At least 1 approval needed
        }
        // >2/3 threshold means: (2/3 * n) + 1
        return (int) Math.ceil((2.0 * totalValidators / 3.0) + 0.01);
    }

    /**
     * Validates vote is not a duplicate
     * @param approval The approval request
     * @param validatorId The validator attempting to vote
     * @throws IllegalStateException if validator already voted
     */
    public void validateNoDuplicateVote(VVBApprovalRequest approval, String validatorId) {
        List<ValidatorVote> existingVotes = approvalRegistry.getVotesByRequest(approval.requestId);
        boolean hasVoted = existingVotes.stream()
            .anyMatch(vote -> vote.validatorId.equals(validatorId));

        if (hasVoted) {
            throw new IllegalStateException(
                String.format("Validator %s has already voted", validatorId)
            );
        }
    }

    /**
     * Validates approval can still receive votes
     * @param approval The approval request
     * @throws IllegalStateException if voting is closed
     */
    public void validateApprovalOpenForVoting(VVBApprovalRequest approval) {
        if (approval.status != ApprovalStatus.PENDING) {
            throw new IllegalStateException(
                String.format("Approval is not open for voting. Current status: %s", approval.status)
            );
        }
        validateDeadlineCompliance(approval);
    }

    /**
     * Validates vote execution prerequisites
     * @param approval The approval request
     * @throws IllegalStateException if prerequisites not met
     */
    public void validateExecutionPrerequisites(VVBApprovalRequest approval) {
        if (approval.status != ApprovalStatus.APPROVED) {
            throw new IllegalStateException(
                String.format(
                    "Cannot execute approval in %s status. Must be APPROVED",
                    approval.status
                )
            );
        }
    }
}
