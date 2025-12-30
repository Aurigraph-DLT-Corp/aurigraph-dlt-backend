package io.aurigraph.v11.models;

/**
 * Isolation Mode Enum
 *
 * Defines the isolation level for channel transaction processing in Aurigraph V11.
 *
 * Part of Sprint 10 - Story 1 (AV11-054)
 *
 * @author Claude Code - Backend Development Agent (BDA)
 * @version 11.0.0
 * @since Sprint 10
 */
public enum IsolationMode {
    /**
     * Shared resources with main chain
     */
    SHARED("Shared", "Shares resources with main blockchain"),

    /**
     * Isolated processing with own resources
     */
    ISOLATED("Isolated", "Dedicated resources and processing"),

    /**
     * Fully independent sidechain
     */
    SIDECHAIN("Sidechain", "Independent sidechain with own consensus");

    private final String displayName;
    private final String description;

    IsolationMode(String displayName, String description) {
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
     * Check if mode requires dedicated resources
     *
     * @return true if dedicated resources are required
     */
    public boolean requiresDedicatedResources() {
        return this == ISOLATED || this == SIDECHAIN;
    }

    /**
     * Check if mode has independent consensus
     *
     * @return true if mode uses independent consensus
     */
    public boolean hasIndependentConsensus() {
        return this == SIDECHAIN;
    }
}
