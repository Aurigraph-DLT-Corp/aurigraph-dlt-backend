package io.aurigraph.v11.models;

/**
 * Channel Member Type Enum
 *
 * Defines the types/roles of members in a blockchain channel.
 *
 * Part of Sprint 10 - Story 1 (AV11-054)
 *
 * @author Claude Code - Backend Development Agent (BDA)
 * @version 11.0.0
 * @since Sprint 10
 */
public enum ChannelMemberType {
    /**
     * Validator node - participates in consensus
     */
    VALIDATOR("Validator", "Validates transactions and blocks"),

    /**
     * Participant node - submits transactions
     */
    PARTICIPANT("Participant", "Submits transactions to channel"),

    /**
     * Observer node - read-only access
     */
    OBSERVER("Observer", "Read-only observer"),

    /**
     * Admin node - channel administration
     */
    ADMIN("Admin", "Channel administrator with full permissions");

    private final String displayName;
    private final String description;

    ChannelMemberType(String displayName, String description) {
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
     * Check if member type can validate
     *
     * @return true if member can validate blocks
     */
    public boolean canValidate() {
        return this == VALIDATOR || this == ADMIN;
    }

    /**
     * Check if member type can write
     *
     * @return true if member can submit transactions
     */
    public boolean canWrite() {
        return this == VALIDATOR || this == PARTICIPANT || this == ADMIN;
    }

    /**
     * Check if member type has admin privileges
     *
     * @return true if member has admin privileges
     */
    public boolean isAdmin() {
        return this == ADMIN;
    }
}
