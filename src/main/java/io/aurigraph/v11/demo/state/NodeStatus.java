package io.aurigraph.v11.demo.state;

/**
 * NodeStatus - Enumeration of possible node states
 *
 * State machine transitions:
 * INITIALIZING → RUNNING → PAUSED → RUNNING → STOPPED
 * INITIALIZING → ERROR
 * RUNNING → ERROR
 * PAUSED → ERROR
 * ERROR → STOPPED
 *
 * @author Backend Development Agent (BDA)
 * @version 1.0
 * @since AV11-216
 */
public enum NodeStatus {
    /**
     * Node is initializing (loading config, connecting to peers, etc.)
     * This is the initial state when a node starts
     */
    INITIALIZING("Initializing", "Node is starting up and initializing resources", false),

    /**
     * Node is fully operational and processing requests
     */
    RUNNING("Running", "Node is operational and processing requests", true),

    /**
     * Node is temporarily paused (maintenance, upgrades, etc.)
     */
    PAUSED("Paused", "Node is temporarily paused", false),

    /**
     * Node has stopped and is no longer processing requests
     * This is a terminal state
     */
    STOPPED("Stopped", "Node has been stopped", false),

    /**
     * Node encountered an error and cannot continue operating
     * This is a terminal state that requires intervention
     */
    ERROR("Error", "Node encountered a critical error", false);

    private final String displayName;
    private final String description;
    private final boolean operational;

    NodeStatus(String displayName, String description, boolean operational) {
        this.displayName = displayName;
        this.description = description;
        this.operational = operational;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Returns true if the node is operational in this state
     */
    public boolean isOperational() {
        return operational;
    }

    /**
     * Returns true if this is a terminal state (cannot transition from here)
     */
    public boolean isTerminal() {
        return this == STOPPED || this == ERROR;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
