package io.aurigraph.v11.token.secondary;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * ApprovalExecutionCompleted
 * Event fired by ApprovalExecutionService after successful approval execution.
 *
 * Signals that:
 * - State transition PENDING_VVB → ACTIVE completed
 * - Approval metadata updated
 * - Audit trail recorded
 * - Cascade retirement executed (if applicable)
 *
 * Subscribers:
 * - RevenueStreamService (setup settlement flows)
 * - MonitoringService (record metrics)
 * - NotificationService (send confirmations)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalExecutionCompleted {
    /**
     * The token version that was approved
     */
    private UUID versionId;

    /**
     * The approval request ID
     */
    private UUID approvalRequestId;

    /**
     * Execution duration in milliseconds
     */
    private long executionDurationMs;

    /**
     * When execution completed
     */
    private Instant completedAt;

    /**
     * Constructor with minimal parameters
     */
    public ApprovalExecutionCompleted(UUID versionId, UUID approvalRequestId, long duration) {
        this.versionId = versionId;
        this.approvalRequestId = approvalRequestId;
        this.executionDurationMs = duration;
        this.completedAt = Instant.now();
    }

    @Override
    public String toString() {
        return "ApprovalExecutionCompleted{" +
                "versionId=" + versionId +
                ", approvalRequestId=" + approvalRequestId +
                ", executionDurationMs=" + executionDurationMs +
                '}';
    }
}
