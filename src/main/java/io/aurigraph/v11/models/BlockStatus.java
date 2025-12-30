package io.aurigraph.v11.models;

/**
 * Block Status Enum
 *
 * Represents the lifecycle states of a block in the Aurigraph V11 blockchain.
 *
 * Part of Sprint 9 - Story 2 (AV11-052)
 *
 * @author Claude Code
 * @version 11.0.0
 * @since Sprint 9
 */
public enum BlockStatus {
    /**
     * Block is being created and filled with transactions
     */
    PENDING("Pending", "Block is being created"),

    /**
     * Block has been proposed by a validator
     */
    PROPOSED("Proposed", "Block has been proposed to the network"),

    /**
     * Block is being validated by validators
     */
    VALIDATING("Validating", "Block is being validated"),

    /**
     * Block has been confirmed by consensus
     */
    CONFIRMED("Confirmed", "Block has been confirmed by consensus"),

    /**
     * Block has been finalized and cannot be reversed
     */
    FINALIZED("Finalized", "Block is finalized and immutable"),

    /**
     * Block was rejected by validators
     */
    REJECTED("Rejected", "Block was rejected"),

    /**
     * Block is orphaned (not part of main chain)
     */
    ORPHANED("Orphaned", "Block is orphaned");

    private final String displayName;
    private final String description;

    BlockStatus(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Check if block is in a terminal state (cannot change)
     */
    public boolean isTerminal() {
        return this == FINALIZED || this == REJECTED || this == ORPHANED;
    }

    /**
     * Check if block is valid and part of the chain
     */
    public boolean isValid() {
        return this == CONFIRMED || this == FINALIZED;
    }
}
