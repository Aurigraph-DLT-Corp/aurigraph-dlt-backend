package io.aurigraph.v11.token.secondary;

/**
 * Approval Status Enum
 *
 * Represents the lifecycle states of a VVB approval request.
 * Status controls allowed operations and state transitions.
 *
 * State Transitions:
 * - PENDING → APPROVED, REJECTED, EXPIRED (voting phase)
 * - APPROVED → (terminal state)
 * - REJECTED → (terminal state)
 * - EXPIRED → (terminal state)
 *
 * @version 12.0.0
 * @since December 23, 2025
 */
public enum ApprovalStatus {
    /**
     * Approval voting is active.
     * Validators can submit votes.
     * Timeout: votingWindowSeconds (typically 7 days = 604,800s)
     */
    PENDING,

    /**
     * Approval achieved consensus.
     * Token version can be activated.
     * Terminal state.
     */
    APPROVED,

    /**
     * Approval rejected by consensus.
     * Token version cannot be activated.
     * Terminal state.
     */
    REJECTED,

    /**
     * Voting window expired without consensus.
     * Treated as rejection (cannot activate version).
     * Terminal state.
     */
    EXPIRED
}
