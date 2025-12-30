package io.aurigraph.v11.demo.nodes;

import io.aurigraph.v11.demo.models.NodeHealth;
import io.aurigraph.v11.demo.models.NodeMetrics;
import io.aurigraph.v11.demo.models.NodeStatus;
import io.aurigraph.v11.demo.models.NodeType;
import io.smallrye.mutiny.Uni;

/**
 * Base interface for all node types in the Aurigraph V11 blockchain network.
 *
 * <p>This interface defines the core contract that all node implementations must fulfill,
 * regardless of their specific type (Channel, Validator, Business, or API Integration).
 * It provides reactive programming support through Mutiny's {@link Uni} for asynchronous
 * operations.
 *
 * <p><b>Node Lifecycle:</b>
 * <pre>
 *     INITIALIZING → start() → RUNNING → stop() → STOPPED
 *            ↓                    ↓
 *         ERROR                ERROR
 * </pre>
 *
 * <p><b>Supported Node Types:</b>
 * <ul>
 *   <li><b>CHANNEL</b> - Multi-channel data flow coordination</li>
 *   <li><b>VALIDATOR</b> - HyperRAFT++ consensus participation</li>
 *   <li><b>BUSINESS</b> - Business logic and smart contract execution</li>
 *   <li><b>API_INTEGRATION</b> - External data source integration</li>
 * </ul>
 *
 * <p><b>Core Responsibilities:</b>
 * <ul>
 *   <li>Lifecycle management (start, stop, restart)</li>
 *   <li>Health monitoring and reporting</li>
 *   <li>Performance metrics collection</li>
 *   <li>State management and persistence</li>
 *   <li>Network communication and peer management</li>
 * </ul>
 *
 * <p><b>Reactive Programming:</b>
 * All asynchronous operations return {@link Uni} to support non-blocking,
 * reactive execution patterns. This enables high-throughput processing
 * with efficient resource utilization.
 *
 * @author Aurigraph V11 Platform
 * @version 11.0.0
 * @since 2025-10-11
 * @see AbstractNode
 * @see NodeType
 * @see NodeStatus
 * @see NodeHealth
 * @see NodeMetrics
 */
public interface Node {

    /**
     * Gets the unique identifier for this node.
     *
     * @return The node's unique identifier (UUID format)
     */
    String getNodeId();

    /**
     * Gets the type of this node.
     *
     * @return The node type (CHANNEL, VALIDATOR, BUSINESS, or API_INTEGRATION)
     */
    NodeType getNodeType();

    /**
     * Gets the current operational status of this node.
     *
     * @return The current node status
     */
    NodeStatus getStatus();

    /**
     * Starts the node and initializes all required services.
     *
     * <p>This method is asynchronous and returns a Uni that completes
     * with true if the node started successfully, false otherwise.</p>
     *
     * @return A Uni that completes with the start result
     */
    Uni<Boolean> start();

    /**
     * Stops the node and gracefully shuts down all services.
     *
     * <p>This method is asynchronous and returns a Uni that completes
     * with true if the node stopped successfully, false otherwise.</p>
     *
     * @return A Uni that completes with the stop result
     */
    Uni<Boolean> stop();

    /**
     * Performs a health check on the node and its services.
     *
     * <p>Returns detailed health information including:
     * <ul>
     *   <li>Overall health status (healthy/unhealthy)</li>
     *   <li>Component-level health status</li>
     *   <li>Timestamp of the health check</li>
     *   <li>Additional diagnostic information</li>
     * </ul>
     * </p>
     *
     * @return A Uni that completes with the node health information
     */
    Uni<NodeHealth> healthCheck();

    /**
     * Retrieves current performance and operational metrics for this node.
     *
     * <p>Metrics may include:
     * <ul>
     *   <li>Throughput statistics (TPS, messages/sec)</li>
     *   <li>Latency measurements (average, p50, p95, p99)</li>
     *   <li>Resource utilization (CPU, memory, disk)</li>
     *   <li>Active connections/channels</li>
     *   <li>Error rates and counts</li>
     *   <li>Node-type specific metrics</li>
     * </ul>
     * </p>
     *
     * @return A Uni that completes with the node metrics
     */
    Uni<NodeMetrics> getMetrics();

    /**
     * Restarts the node by stopping and starting it.
     *
     * <p>This is a convenience method equivalent to calling {@code stop().chain(unused -> start())}.
     * It performs a full shutdown and startup cycle, which can be useful for:
     * <ul>
     *   <li>Applying configuration changes</li>
     *   <li>Recovering from degraded states</li>
     *   <li>Clearing cached state</li>
     *   <li>Reconnecting to network peers</li>
     * </ul>
     *
     * @return a Uni emitting {@code true} if the node restarted successfully,
     *         or a failure if restart failed
     */
    default Uni<Boolean> restart() {
        return stop().chain(stopped -> start());
    }

    /**
     * Checks if the node is currently running and operational.
     *
     * @return true if the node status is RUNNING, false otherwise
     */
    default boolean isRunning() {
        return getStatus() == NodeStatus.RUNNING;
    }

    /**
     * Checks if the node is in a healthy state.
     *
     * <p>This is a synchronous convenience method that checks the current status.
     * For real-time health checks, use {@link #healthCheck()}.
     *
     * @return true if the node is running and healthy, false otherwise
     */
    default boolean isHealthy() {
        return getStatus() == NodeStatus.RUNNING && getStatus().isHealthy();
    }
}
