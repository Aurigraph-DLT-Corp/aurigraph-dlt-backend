package io.aurigraph.v11.demo.models;

/**
 * Enumeration of possible node operational states in the Aurigraph V11 network.
 *
 * <p>This enum represents the lifecycle states that a node can be in during its operation.
 * Nodes transition through these states based on operational events, health checks, and
 * administrative actions.
 *
 * <p><b>State Transition Flow:</b>
 * <pre>
 *     INITIALIZING → RUNNING → STOPPED
 *            ↓          ↓
 *         ERROR ←───────┘
 * </pre>
 *
 * <p>All nodes start in INITIALIZING state, transition to RUNNING when ready,
 * and can move to ERROR state if issues are detected. The STOPPED state is
 * reached through graceful shutdown or administrative action.
 *
 * @author Aurigraph V11 Platform
 * @version 11.0.0
 * @since 2025-10-11
 * @see io.aurigraph.v11.demo.nodes.Node
 * @see io.aurigraph.v11.demo.nodes.AbstractNode
 */
public enum NodeStatus {

    /**
     * Node is initializing and preparing to start operations.
     *
     * <p>During this state, the node is:
     * <ul>
     *   <li>Loading configuration</li>
     *   <li>Establishing network connections</li>
     *   <li>Initializing state storage (LevelDB, Redis)</li>
     *   <li>Registering with the network discovery service</li>
     *   <li>Performing health checks</li>
     * </ul>
     *
     * <p><b>Typical Duration:</b> 1-5 seconds for native builds, 3-10 seconds for JVM
     *
     * <p><b>Next States:</b>
     * <ul>
     *   <li>RUNNING - when initialization completes successfully</li>
     *   <li>ERROR - if initialization fails</li>
     * </ul>
     */
    INITIALIZING("Initializing", "Node is starting up and initializing components", false, false),

    /**
     * Node is fully operational and processing requests.
     *
     * <p>In this state, the node is:
     * <ul>
     *   <li>Accepting and processing transactions</li>
     *   <li>Participating in network consensus (if validator)</li>
     *   <li>Serving API requests</li>
     *   <li>Publishing metrics and health data</li>
     *   <li>Maintaining state consistency</li>
     * </ul>
     *
     * <p><b>Next States:</b>
     * <ul>
     *   <li>STOPPED - through graceful shutdown</li>
     *   <li>ERROR - if critical failure is detected</li>
     * </ul>
     */
    RUNNING("Running", "Node is operational and processing requests", true, true),

    /**
     * Node has been stopped and is not processing requests.
     *
     * <p>This state indicates:
     * <ul>
     *   <li>Graceful shutdown completed</li>
     *   <li>All pending operations flushed</li>
     *   <li>State persisted to storage</li>
     *   <li>Network connections closed cleanly</li>
     *   <li>Resources released</li>
     * </ul>
     *
     * <p><b>Recovery:</b> Node can be restarted and will transition to INITIALIZING
     *
     * <p><b>Next States:</b>
     * <ul>
     *   <li>INITIALIZING - when restart is initiated</li>
     * </ul>
     */
    STOPPED("Stopped", "Node has been stopped and is not processing requests", false, false),

    /**
     * Node encountered an error and is in degraded or failed state.
     *
     * <p>Error conditions include:
     * <ul>
     *   <li>Failed health checks</li>
     *   <li>Network connectivity issues</li>
     *   <li>Storage failures (LevelDB/Redis errors)</li>
     *   <li>Consensus failures (for validators)</li>
     *   <li>Resource exhaustion (memory, disk)</li>
     *   <li>Uncaught exceptions in critical paths</li>
     * </ul>
     *
     * <p><b>Recovery Actions:</b>
     * <ul>
     *   <li>Automatic retry for transient errors</li>
     *   <li>Alert monitoring systems</li>
     *   <li>Log detailed error information</li>
     *   <li>Attempt graceful degradation when possible</li>
     * </ul>
     *
     * <p><b>Next States:</b>
     * <ul>
     *   <li>RUNNING - if error is recovered automatically</li>
     *   <li>STOPPED - if manual intervention required</li>
     *   <li>INITIALIZING - on restart attempt</li>
     * </ul>
     */
    ERROR("Error", "Node encountered an error and requires attention", false, false);

    private final String displayName;
    private final String description;
    private final boolean operational;
    private final boolean healthy;

    /**
     * Constructs a NodeStatus with the specified properties.
     *
     * @param displayName the human-readable name of the status
     * @param description a brief description of what this status means
     * @param operational true if the node is operational in this state
     * @param healthy true if the node is healthy in this state
     */
    NodeStatus(String displayName, String description, boolean operational, boolean healthy) {
        this.displayName = displayName;
        this.description = description;
        this.operational = operational;
        this.healthy = healthy;
    }

    /**
     * Returns the human-readable display name for this status.
     *
     * @return the display name
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Returns the description of what this status indicates.
     *
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Checks if the node is operational in this status.
     *
     * <p>A node is considered operational if it can process requests and perform
     * its designated functions. Only the RUNNING state is fully operational.
     *
     * @return true if the node is operational, false otherwise
     */
    public boolean isOperational() {
        return operational;
    }

    /**
     * Checks if the node is healthy in this status.
     *
     * <p>A healthy node is one that is functioning correctly without errors.
     * Only the RUNNING state indicates a healthy node.
     *
     * @return true if the node is healthy, false otherwise
     */
    public boolean isHealthy() {
        return healthy;
    }

    /**
     * Checks if the node can accept new requests in this status.
     *
     * <p>Only RUNNING nodes can accept new requests. All other states indicate
     * the node is not ready to process new work.
     *
     * @return true if requests can be accepted, false otherwise
     */
    public boolean canAcceptRequests() {
        return this == RUNNING;
    }

    /**
     * Checks if this status represents a terminal state requiring intervention.
     *
     * @return true if this is ERROR or STOPPED status, false otherwise
     */
    public boolean isTerminalState() {
        return this == ERROR || this == STOPPED;
    }

    /**
     * Checks if the node is in a transitional state.
     *
     * @return true if this is INITIALIZING status, false otherwise
     */
    public boolean isTransitional() {
        return this == INITIALIZING;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
