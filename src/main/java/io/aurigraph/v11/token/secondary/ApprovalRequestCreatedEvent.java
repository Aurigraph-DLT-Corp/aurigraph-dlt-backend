package io.aurigraph.v11.token.secondary;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * CDI Event: Approval Request Created
 *
 * Fired when a new VVB approval request is created.
 * Allows subscribers to react to approval initiation (logging, notifications, metrics).
 *
 * @version 12.0.0
 * @since December 23, 2025
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalRequestCreatedEvent {
    private UUID requestId;
    private UUID tokenVersionId;
    private Integer validatorCount;
    private LocalDateTime votingWindowEnd;
}
