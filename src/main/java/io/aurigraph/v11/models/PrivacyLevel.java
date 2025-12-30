package io.aurigraph.v11.models;

/**
 * Privacy Level Enum
 *
 * Defines privacy and visibility levels for blockchain channels in Aurigraph V11.
 *
 * Part of Sprint 10 - Story 1 (AV11-054)
 *
 * @author Claude Code - Backend Development Agent (BDA)
 * @version 11.0.0
 * @since Sprint 10
 */
public enum PrivacyLevel {
    /**
     * Public channel - visible to all, anyone can join with approval
     */
    PUBLIC("Public", "Visible to all network participants"),

    /**
     * Private channel - visible to members only, invite-only
     */
    PRIVATE("Private", "Visible to members only, invite required"),

    /**
     * Consortium channel - shared between specific organizations
     */
    CONSORTIUM("Consortium", "Shared between approved organizations"),

    /**
     * Confidential channel - encrypted data, restricted access
     */
    CONFIDENTIAL("Confidential", "Encrypted with restricted access");

    private final String displayName;
    private final String description;

    PrivacyLevel(String displayName, String description) {
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
     * Check if privacy level requires invitation
     *
     * @return true if invitation is required to join
     */
    public boolean requiresInvitation() {
        return this == PRIVATE || this == CONSORTIUM || this == CONFIDENTIAL;
    }

    /**
     * Check if data should be encrypted
     *
     * @return true if channel data should be encrypted
     */
    public boolean requiresEncryption() {
        return this == CONFIDENTIAL;
    }

    /**
     * Check if channel is publicly visible
     *
     * @return true if channel information is publicly visible
     */
    public boolean isPubliclyVisible() {
        return this == PUBLIC;
    }
}
