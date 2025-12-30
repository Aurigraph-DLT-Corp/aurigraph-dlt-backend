package io.aurigraph.v11.models;

/**
 * Channel Permission Enum
 *
 * Defines granular permissions for channel members in Aurigraph V11.
 *
 * Part of Sprint 10 - Story 1 (AV11-054)
 *
 * @author Claude Code - Backend Development Agent (BDA)
 * @version 11.0.0
 * @since Sprint 10
 */
public enum ChannelPermission {
    /**
     * Read permission - can view channel data
     */
    READ("Read", "Can view channel data and transactions"),

    /**
     * Write permission - can submit transactions
     */
    WRITE("Write", "Can submit transactions to channel"),

    /**
     * Validate permission - can validate blocks
     */
    VALIDATE("Validate", "Can validate and produce blocks"),

    /**
     * Admin permission - full administrative access
     */
    ADMIN("Admin", "Full administrative privileges"),

    /**
     * Manage members permission - can add/remove members
     */
    MANAGE_MEMBERS("Manage Members", "Can manage channel members"),

    /**
     * Configure permission - can modify channel settings
     */
    CONFIGURE("Configure", "Can modify channel configuration"),

    /**
     * Deploy contracts permission - can deploy smart contracts
     */
    DEPLOY_CONTRACTS("Deploy Contracts", "Can deploy smart contracts"),

    /**
     * Archive permission - can archive the channel
     */
    ARCHIVE("Archive", "Can archive the channel");

    private final String displayName;
    private final String description;

    ChannelPermission(String displayName, String description) {
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
     * Check if this is an administrative permission
     *
     * @return true if permission grants administrative access
     */
    public boolean isAdministrative() {
        return this == ADMIN || this == MANAGE_MEMBERS || this == CONFIGURE || this == ARCHIVE;
    }

    /**
     * Check if this is a consensus permission
     *
     * @return true if permission relates to consensus
     */
    public boolean isConsensusRelated() {
        return this == VALIDATE;
    }
}
