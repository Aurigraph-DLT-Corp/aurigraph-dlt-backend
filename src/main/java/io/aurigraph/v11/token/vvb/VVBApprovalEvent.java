package io.aurigraph.v11.token.vvb;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * VVB Approval Event
 * CDI event fired when approval decisions are made
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VVBApprovalEvent {
    private UUID versionId;
    private VVBValidator.VVBApprovalStatus status;
    private String decidedBy;
    private String reason;
    private Instant timestamp;

    public VVBApprovalEvent(UUID versionId, VVBValidator.VVBApprovalStatus status, String decidedBy) {
        this.versionId = versionId;
        this.status = status;
        this.decidedBy = decidedBy;
        this.timestamp = Instant.now();
    }
}
