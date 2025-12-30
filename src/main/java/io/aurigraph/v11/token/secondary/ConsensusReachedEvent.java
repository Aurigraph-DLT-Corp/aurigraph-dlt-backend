package io.aurigraph.v11.token.secondary;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * CDI Event: Consensus Reached
 *
 * Fired when consensus is reached (or becomes impossible) on an approval request.
 * Allows subscribers to react to consensus outcome (logging, activation, notifications).
 *
 * @version 12.0.0
 * @since December 23, 2025
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConsensusReachedEvent {
    private UUID approvalRequestId;
    private ConsensusResult consensusResult;
}
