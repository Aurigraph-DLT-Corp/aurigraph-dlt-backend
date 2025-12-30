package io.aurigraph.v11.token.vvb;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * VVB Statistics DTO
 * Metrics for VVB approval workflow
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VVBStatistics {
    private long totalDecisions;
    private long approvedCount;
    private long rejectedCount;
    private long pendingCount;
    private double averageApprovalTimeMinutes;

    public double getApprovalRate() {
        if (totalDecisions == 0) return 0.0;
        return (double) approvedCount / totalDecisions * 100;
    }

    public double getRejectionRate() {
        if (totalDecisions == 0) return 0.0;
        return (double) rejectedCount / totalDecisions * 100;
    }
}
