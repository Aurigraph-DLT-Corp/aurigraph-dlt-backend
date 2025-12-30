package io.aurigraph.v11.token.secondary;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * ApprovalExecutionFailed
 * Event fired by ApprovalExecutionService when approval execution fails.
 *
 * Signals that:
 * - State transition failed
 * - Rollback executed (if possible)
 * - Error recorded in audit trail
 * - Manual intervention may be needed
 *
 * Subscribers:
 * - MonitoringService (alert on failure)
 * - ErrorHandlingService (retry logic)
 * - NotificationService (notify administrators)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalExecutionFailed {
    /**
     * The approval request ID
     */
    private UUID approvalRequestId;

    /**
     * The token version (may be null if not found)
     */
    private UUID versionId;

    /**
     * Error message describing the failure
     */
    private String errorMessage;

    /**
     * When failure occurred
     */
    private Instant failedAt;

    /**
     * Root cause exception class name
     */
    private String exceptionType;

    /**
     * Stack trace (truncated for brevity)
     */
    private String stackTrace;

    /**
     * Constructor with minimal parameters
     */
    public ApprovalExecutionFailed(UUID requestId, UUID versionId, String errorMsg) {
        this.approvalRequestId = requestId;
        this.versionId = versionId;
        this.errorMessage = errorMsg;
        this.failedAt = Instant.now();
    }

    @Override
    public String toString() {
        return "ApprovalExecutionFailed{" +
                "approvalRequestId=" + approvalRequestId +
                ", versionId=" + versionId +
                ", errorMessage='" + errorMessage + '\'' +
                '}';
    }
}
