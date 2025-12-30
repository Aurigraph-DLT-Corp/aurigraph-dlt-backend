package io.aurigraph.v11.token.vvb;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * VVB Validation Details DTO
 * Detailed status and audit trail for a token validation
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VVBValidationDetails {
    private UUID versionId;
    private VVBValidator.VVBApprovalStatus status;
    private String changeType;
    private List<VVBValidator.VVBApprovalRecord> approvalHistory;
    private Instant submittedAt;

    public int getApprovalCount() {
        return (int) approvalHistory.stream()
            .filter(r -> r.getDecision() == VVBValidator.VVBApprovalDecision.APPROVED)
            .count();
    }

    public int getRejectionCount() {
        return (int) approvalHistory.stream()
            .filter(r -> r.getDecision() == VVBValidator.VVBApprovalDecision.REJECTED)
            .count();
    }
}
