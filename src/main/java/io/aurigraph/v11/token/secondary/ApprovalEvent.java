package io.aurigraph.v11.token.secondary;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * ApprovalEvent
 * CDI event fired by Story 5 (VVBApprovalService) when consensus is reached.
 * Consumed by Story 6 (ApprovalExecutionService) to trigger state transitions.
 *
 * This event signals that:
 * - VVB voting completed
 * - Consensus reached (>2/3 majority approved)
 * - Ready for approval execution
 *
 * Integration Flow:
 * VVBApprovalService.approveTokenVersion() [Story 5]
 *   ↓ fires ApprovalEvent
 * ApprovalExecutionService.onApprovalEvent() [Story 6]
 *   ↓ executes
 * TokenStateTransitionManager.executeTransition()
 *   ↓ PENDING_VVB → ACTIVE
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalEvent {
    /**
     * Unique identifier for this approval request
     */
    private UUID requestId;

    /**
     * The token version being approved
     */
    private UUID versionId;

    /**
     * The secondary token ID (parent)
     */
    private UUID secondaryTokenId;

    /**
     * Approval status (typically APPROVED)
     */
    private String status;

    /**
     * Number of approvals received
     */
    private int approvalCount;

    /**
     * Total validators in voting
     */
    private int totalValidators;

    /**
     * Approval threshold percentage (e.g., 66.67)
     */
    private double thresholdPercentage;

    /**
     * When consensus was reached
     */
    private Instant consensusReachedAt;

    /**
     * Approver identifiers who voted YES
     */
    private java.util.List<String> approverIds;

    /**
     * Optional error message if approval failed
     */
    private String errorMessage;

    @Override
    public String toString() {
        return "ApprovalEvent{" +
                "requestId=" + requestId +
                ", versionId=" + versionId +
                ", status='" + status + '\'' +
                ", approvalCount=" + approvalCount +
                ", totalValidators=" + totalValidators +
                '}';
    }
}
