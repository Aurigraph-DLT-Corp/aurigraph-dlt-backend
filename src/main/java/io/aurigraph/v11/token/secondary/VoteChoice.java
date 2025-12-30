package io.aurigraph.v11.token.secondary;

/**
 * Vote Choice Enum
 *
 * Represents validator vote choices in VVB approval workflow.
 * Supports Byzantine Fault Tolerant consensus with abstention.
 *
 * Consensus Rules:
 * - YES votes: Count toward approval
 * - NO votes: Count toward rejection
 * - ABSTAIN votes: Excluded from calculation (enables Byzantine FT)
 *
 * Byzantine FT Logic:
 * - Requires >2/3 of active validators to vote YES for approval
 * - Requires >2/3 of active validators to vote NO for rejection
 * - Abstentions reduce quorum requirement (e.g., 3 of 5 validators, 2 abstain = 3 of 3 = 100%)
 *
 * @version 12.0.0
 * @since December 23, 2025
 */
public enum VoteChoice {
    /**
     * Vote in favor of approval.
     * Counted toward approval consensus.
     */
    YES,

    /**
     * Vote against approval.
     * Counted toward rejection consensus.
     */
    NO,

    /**
     * Abstain from voting.
     * Excluded from consensus calculation (enables Byzantine FT).
     * Useful for conflicted validators.
     */
    ABSTAIN
}
