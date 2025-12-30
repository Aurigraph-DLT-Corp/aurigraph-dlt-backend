package io.aurigraph.v11.token.secondary;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * CDI Event: Rejection Finalized
 *
 * Fired when an approval request is finalized as REJECTED.
 * The associated token version is archived and cannot be activated.
 * Allows subscribers to react to rejection (cleanup, notifications, audit).
 *
 * @version 12.0.0
 * @since December 23, 2025
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RejectionEvent {
    private UUID approvalRequestId;
    private UUID tokenVersionId;
    private String rejectionReason;
}
