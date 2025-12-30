package io.aurigraph.v11.demo.nodes;

import io.aurigraph.v11.demo.models.NodeHealth;
import io.aurigraph.v11.demo.models.NodeMetrics;
import io.aurigraph.v11.demo.models.NodeStatus;
import io.aurigraph.v11.demo.models.NodeType;
import io.smallrye.mutiny.Uni;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Abstract base class providing common functionality for all node implementations.
 *
 * <p>This class implements the core {@link Node} interface and provides:
 * <ul>
 *   <li>Standard lifecycle management (start/stop/restart)</li>
 *   <li>Status tracking and transitions</li>
 *   <li>Basic health checking infrastructure</li>
 *   <li>Metrics collection framework</li>
 *   <li>Thread-safe state management</li>
 *   <li>Logging infrastructure</li>
 * </ul>
 *
 * <p><b>Subclass Responsibilities:</b>
 * Concrete node implementations must override the following methods:
 * <ul>
 *   <li>{@link #doStart()} - Node-specific startup logic</li>
 *   <li>{@link #doStop()} - Node-specific shutdown logic</li>
 *   <li>{@link #doHealthCheck()} - Node-specific health validation</li>
 *   <li>{@link #doGetMetrics()} - Node-specific metrics collection</li>
 * </ul>
 *
 * <p><b>State Management:</b>
 * This class uses atomic references for thread-safe state management:
 * <ul>
 *   <li>{@code status} - Current operational status (AtomicReference)</li>
 *   <li>{@code startTime} - Node start timestamp (AtomicLong)</li>
 *   <li>{@code lastHealthCheck} - Last health check result (cached)</li>
 * </ul>
 *
 * <p><b>Lifecycle Guarantees:</b>
 * <ul>
 *   <li>Start/stop operations are idempotent</li>
 *   <li>Status transitions are atomic and thread-safe</li>
 *   <li>Resources are properly cleaned up on shutdown</li>
 *   <li>Errors during lifecycle operations transition to ERROR state</li>
 * </ul>
 *
 * <p><b>Usage Example:</b>
 * <pre>{@code
 * public class ChannelNode extends AbstractNode {
 *
 *     public ChannelNode(String nodeId) {
 *         super(nodeId, NodeType.CHANNEL);
 *     }
 *
 *     @Override
 *     protected Uni<Void> doStart() {
 *         return Uni.createFrom().item(() -> {
 *             // Initialize channel-specific services
 *             initializeChannelRouter();
 *             initializeParticipantRegistry();
 *             return null;
 *         });
 *     }
 *
 *     @Override
 *     protected Uni<Void> doStop() {
 *         return Uni.createFrom().item(() -> {
 *             // Shutdown channel-specific services
 *             shutdownChannelRouter();
 *             shutdownParticipantRegistry();
 *             return null;
 *         });
 *     }
 *
 *     @Override
 *     protected Uni<NodeHealth> doHealthCheck() {
 *         return Uni.createFrom().item(() ->
 *             NodeHealth.builder()
 *                 .healthy(isRunning())
 *                 .status(getStatus())
 *                 .lastCheckTime(Instant.now())
 *                 .build()
 *         );
 *     }
 *
 *     @Override
 *     protected Uni<NodeMetrics> doGetMetrics() {
 *         return Uni.createFrom().item(() ->
 *             NodeMetrics.builder()
 *                 .nodeId(getNodeId())
 *                 .nodeType(getNodeType())
 *                 .build()
 *         );
 *     }
 * }
 * }</pre>
 *
 * @author Aurigraph V11 Platform
 * @version 11.0.0
 * @since 2025-10-11
 * @see Node
 * @see NodeType
 * @see NodeStatus
 */
public abstract class AbstractNode implements Node {

    private static final Logger logger = LoggerFactory.getLogger(AbstractNode.class);

    private final String nodeId;
    private final NodeType nodeType;
    private final AtomicReference<NodeStatus> status;
    private final AtomicLong startTime;
    private final AtomicReference<NodeHealth> lastHealthCheck;

    /**
     * Constructs a new AbstractNode with the specified ID and type.
     *
     * @param nodeId the unique identifier for this node (must not be null)
     * @param nodeType the type of this node (must not be null)
     * @throws IllegalArgumentException if nodeId or nodeType is null
     */
    protected AbstractNode(String nodeId, NodeType nodeType) {
        if (nodeId == null || nodeId.trim().isEmpty()) {
            throw new IllegalArgumentException("Node ID cannot be null or empty");
        }
        if (nodeType == null) {
            throw new IllegalArgumentException("Node type cannot be null");
        }

        this.nodeId = nodeId;
        this.nodeType = nodeType;
        this.status = new AtomicReference<>(NodeStatus.INITIALIZING);
        this.startTime = new AtomicLong(0);
        this.lastHealthCheck = new AtomicReference<>();

        logger.info("Initialized {} node with ID: {}", nodeType, nodeId);
    }

    @Override
    public String getNodeId() {
        return nodeId;
    }

    @Override
    public NodeType getNodeType() {
        return nodeType;
    }

    @Override
    public NodeStatus getStatus() {
        return status.get();
    }

    @Override
    public Uni<Boolean> start() {
        logger.info("Starting node: {} ({})", nodeId, nodeType);

        // Check if already running
        if (status.get() == NodeStatus.RUNNING) {
            logger.warn("Node {} is already running", nodeId);
            return Uni.createFrom().item(false);
        }

        // Set status to INITIALIZING
        status.set(NodeStatus.INITIALIZING);

        return doStart()
            .onItem().invoke(() -> {
                status.set(NodeStatus.RUNNING);
                startTime.set(System.currentTimeMillis());
                logger.info("Node {} started successfully", nodeId);
            })
            .onFailure().invoke(err -> {
                status.set(NodeStatus.ERROR);
                logger.error("Failed to start node {}: {}", nodeId, err.getMessage(), err);
            })
            .map(v -> status.get() == NodeStatus.RUNNING);
    }

    @Override
    public Uni<Boolean> stop() {
        logger.info("Stopping node: {} ({})", nodeId, nodeType);

        // Check if already stopped
        if (status.get() == NodeStatus.STOPPED) {
            logger.warn("Node {} is already stopped", nodeId);
            return Uni.createFrom().item(false);
        }

        return doStop()
            .onItem().invoke(() -> {
                status.set(NodeStatus.STOPPED);
                logger.info("Node {} stopped successfully", nodeId);
            })
            .onFailure().invoke(err -> {
                status.set(NodeStatus.ERROR);
                logger.error("Failed to stop node {}: {}", nodeId, err.getMessage(), err);
            })
            .map(v -> status.get() == NodeStatus.STOPPED);
    }

    @Override
    public Uni<NodeHealth> healthCheck() {
        logger.debug("Performing health check for node: {}", nodeId);

        return doHealthCheck()
            .onItem().invoke(health -> {
                lastHealthCheck.set(health);
                if (!health.isHealthy()) {
                    logger.warn("Node {} is unhealthy: {}", nodeId, health.getErrorMessage());
                }
            })
            .onFailure().invoke(err -> {
                logger.error("Health check failed for node {}: {}", nodeId, err.getMessage(), err);
                NodeHealth errorHealth = NodeHealth.builder()
                    .healthy(false)
                    .status(NodeStatus.ERROR)
                    .lastCheckTime(Instant.now())
                    .errorMessage("Health check failed: " + err.getMessage())
                    .build();
                lastHealthCheck.set(errorHealth);
            })
            .onFailure().recoverWithItem(() -> lastHealthCheck.get());
    }

    @Override
    public Uni<NodeMetrics> getMetrics() {
        logger.debug("Collecting metrics for node: {}", nodeId);

        return doGetMetrics()
            .onFailure().invoke(err -> {
                logger.error("Metrics collection failed for node {}: {}", nodeId, err.getMessage(), err);
            })
            .onFailure().recoverWithItem(() -> createDefaultMetrics());
    }

    /**
     * Returns the node uptime in seconds.
     *
     * @return uptime in seconds, or 0 if not started
     */
    protected long getUptimeSeconds() {
        long start = startTime.get();
        if (start == 0) {
            return 0;
        }
        return (System.currentTimeMillis() - start) / 1000;
    }

    /**
     * Returns the last cached health check result.
     *
     * @return the last health check, or null if never checked
     */
    protected NodeHealth getLastHealthCheck() {
        return lastHealthCheck.get();
    }

    /**
     * Updates the node status atomically.
     *
     * @param newStatus the new status to set
     */
    protected void setStatus(NodeStatus newStatus) {
        NodeStatus oldStatus = status.getAndSet(newStatus);
        if (oldStatus != newStatus) {
            logger.info("Node {} status changed: {} -> {}", nodeId, oldStatus, newStatus);
        }
    }

    /**
     * Creates default metrics when metrics collection fails.
     *
     * @return default metrics with basic information
     */
    private NodeMetrics createDefaultMetrics() {
        NodeMetrics metrics = new NodeMetrics(nodeId);
        metrics.setTimestamp(Instant.now());
        metrics.setUptimeSeconds(getUptimeSeconds());
        return metrics;
    }

    // Abstract methods to be implemented by subclasses

    /**
     * Performs node-specific startup operations.
     *
     * <p>This method is called by {@link #start()} after the node transitions to
     * INITIALIZING status. Implementations should:
     * <ul>
     *   <li>Initialize storage backends (LevelDB, Redis)</li>
     *   <li>Establish network connections</li>
     *   <li>Register with discovery service</li>
     *   <li>Start background processing tasks</li>
     * </ul>
     *
     * <p>If this method throws an exception or returns a failed Uni,
     * the node will transition to ERROR status.
     *
     * @return a Uni that completes when startup is finished, or fails on error
     */
    protected abstract Uni<Void> doStart();

    /**
     * Performs node-specific shutdown operations.
     *
     * <p>This method is called by {@link #stop()} to perform graceful shutdown.
     * Implementations should:
     * <ul>
     *   <li>Stop accepting new requests</li>
     *   <li>Complete or cancel in-flight operations</li>
     *   <li>Persist state to storage</li>
     *   <li>Close network connections</li>
     *   <li>Release resources</li>
     *   <li>Deregister from discovery service</li>
     * </ul>
     *
     * <p>If this method throws an exception or returns a failed Uni,
     * the node will transition to ERROR status.
     *
     * @return a Uni that completes when shutdown is finished, or fails on error
     */
    protected abstract Uni<Void> doStop();

    /**
     * Performs node-specific health checks.
     *
     * <p>This method is called by {@link #healthCheck()} to evaluate node health.
     * Implementations should check:
     * <ul>
     *   <li>Storage connectivity and performance</li>
     *   <li>Network connectivity to peers</li>
     *   <li>Resource utilization (CPU, memory, disk)</li>
     *   <li>Service-specific health indicators</li>
     * </ul>
     *
     * <p>Health checks should complete within 5 seconds and should be
     * lightweight (no heavy I/O or computation).
     *
     * @return a Uni emitting the health status, never null
     */
    protected abstract Uni<NodeHealth> doHealthCheck();

    /**
     * Collects node-specific performance metrics.
     *
     * <p>This method is called by {@link #getMetrics()} to gather metrics.
     * Implementations should collect:
     * <ul>
     *   <li>Throughput statistics (TPS, messages/sec)</li>
     *   <li>Latency measurements (avg, p50, p95, p99)</li>
     *   <li>Resource utilization (CPU, memory, disk)</li>
     *   <li>Service-specific metrics</li>
     * </ul>
     *
     * <p>Metrics collection should add minimal overhead (&lt;1ms)
     * and should use cached/aggregated data where possible.
     *
     * @return a Uni emitting the metrics, never null
     */
    protected abstract Uni<NodeMetrics> doGetMetrics();
}
