package io.aurigraph.v11.token.secondary;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * CDI Event: Vote Submitted
 *
 * Fired when a validator submits a vote on an approval request.
 * Allows subscribers to track voting progress (logging, audit trail, metrics).
 *
 * @version 12.0.0
 * @since December 23, 2025
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VoteSubmittedEvent {
    private UUID voteId;
    private UUID approvalRequestId;
    private String validatorId;
    private VoteChoice vote;
}
