package io.aurigraph.v11.models;

/**
 * Channel Status Enum
 *
 * Represents the operational states of a blockchain channel in Aurigraph V11.
 *
 * Part of Sprint 10 - Story 1 (AV11-054)
 *
 * @author Claude Code - Backend Development Agent (BDA)
 * @version 11.0.0
 * @since Sprint 10
 */
public enum ChannelStatus {
    /**
     * Channel is active and processing transactions
     */
    ACTIVE("Active", "Channel is operational and processing transactions"),

    /**
     * Channel is paused (temporarily suspended)
     */
    PAUSED("Paused", "Channel is temporarily suspended"),

    /**
     * Channel is being initialized
     */
    INITIALIZING("Initializing", "Channel is being set up"),

    /**
     * Channel is in maintenance mode
     */
    MAINTENANCE("Maintenance", "Channel is under maintenance"),

    /**
     * Channel is archived (read-only)
     */
    ARCHIVED("Archived", "Channel is archived and read-only"),

    /**
     * Channel is closed permanently
     */
    CLOSED("Closed", "Channel is permanently closed"),

    /**
     * Channel is in error state
     */
    ERROR("Error", "Channel encountered an error");

    private final String displayName;
    private final String description;

    ChannelStatus(String displayName, String description) {
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
     * Check if channel can process transactions
     *
     * @return true if channel can process transactions
     */
    public boolean canProcessTransactions() {
        return this == ACTIVE;
    }

    /**
     * Check if channel is operational
     *
     * @return true if channel is operational
     */
    public boolean isOperational() {
        return this == ACTIVE || this == PAUSED || this == MAINTENANCE;
    }

    /**
     * Check if channel is readable
     *
     * @return true if channel data can be read
     */
    public boolean isReadable() {
        return this != CLOSED && this != ERROR;
    }
}
