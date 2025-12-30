package io.aurigraph.v11.token.vvb;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * VVB Approval Result DTO
 * Response model for approval decisions
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VVBApprovalResult {
    private UUID versionId;
    private VVBValidator.VVBApprovalStatus status;
    private String message;
    private List<String> pendingApprovers;

    public boolean isApproved() {
        return status == VVBValidator.VVBApprovalStatus.APPROVED;
    }

    public boolean isPending() {
        return status == VVBValidator.VVBApprovalStatus.PENDING_VVB;
    }
}
