package io.aurigraph.v11.models;

/**
 * Channel Member Status Enum
 *
 * Represents the status of a channel member in Aurigraph V11.
 *
 * Part of Sprint 10 - Story 1 (AV11-054)
 *
 * @author Claude Code - Backend Development Agent (BDA)
 * @version 11.0.0
 * @since Sprint 10
 */
public enum ChannelMemberStatus {
    /**
     * Member is active and participating
     */
    ACTIVE("Active", "Member is active in the channel"),

    /**
     * Member invitation is pending
     */
    PENDING("Pending", "Member invitation pending"),

    /**
     * Member is suspended
     */
    SUSPENDED("Suspended", "Member is temporarily suspended"),

    /**
     * Member has been removed
     */
    REMOVED("Removed", "Member has been removed from channel"),

    /**
     * Member left voluntarily
     */
    LEFT("Left", "Member left the channel");

    private final String displayName;
    private final String description;

    ChannelMemberStatus(String displayName, String description) {
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
     * Check if member can participate
     *
     * @return true if member can participate in channel
     */
    public boolean canParticipate() {
        return this == ACTIVE;
    }

    /**
     * Check if member is in the channel
     *
     * @return true if member is still in channel
     */
    public boolean isInChannel() {
        return this == ACTIVE || this == PENDING || this == SUSPENDED;
    }
}
