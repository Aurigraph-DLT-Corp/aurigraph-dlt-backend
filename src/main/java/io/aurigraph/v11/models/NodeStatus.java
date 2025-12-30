package io.aurigraph.v11.models;

/**
 * Node Status Enum
 *
 * Represents the operational states of a node in the Aurigraph V11 network.
 *
 * Part of Sprint 9 - Story 3 (AV11-053)
 *
 * @author Claude Code
 * @version 11.0.0
 * @since Sprint 9
 */
public enum NodeStatus {
    /**
     * Node is offline and not participating in the network
     */
    OFFLINE("Offline", "Node is not connected to the network"),

    /**
     * Node is starting up and initializing
     */
    STARTING("Starting", "Node is initializing"),

    /**
     * Node is syncing with the blockchain
     */
    SYNCING("Syncing", "Node is synchronizing with the blockchain"),

    /**
     * Node is online and ready
     */
    ONLINE("Online", "Node is online and ready"),

    /**
     * Node is actively validating transactions and blocks
     */
    VALIDATING("Validating", "Node is actively validating"),

    /**
     * Node is experiencing issues but still operational
     */
    DEGRADED("Degraded", "Node is experiencing performance issues"),

    /**
     * Node is being maintained
     */
    MAINTENANCE("Maintenance", "Node is under maintenance"),

    /**
     * Node has been banned from the network
     */
    BANNED("Banned", "Node is banned from the network");

    private final String displayName;
    private final String description;

    NodeStatus(String displayName, String description) {
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
     * Check if node is operational
     */
    public boolean isOperational() {
        return this == ONLINE || this == SYNCING || this == VALIDATING || this == DEGRADED;
    }

    /**
     * Check if node can participate in consensus
     */
    public boolean canParticipateInConsensus() {
        return this == ONLINE || this == VALIDATING;
    }
}
