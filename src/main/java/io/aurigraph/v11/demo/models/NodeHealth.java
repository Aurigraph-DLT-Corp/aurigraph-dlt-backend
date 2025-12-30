package io.aurigraph.v11.demo.models;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Represents the health status and diagnostic information for a node in the Aurigraph V11 network.
 *
 * <p>This class provides comprehensive health information including:
 * <ul>
 *   <li>Overall health status (healthy/unhealthy)</li>
 *   <li>Component-level health checks</li>
 *   <li>Last health check timestamp</li>
 *   <li>Uptime information</li>
 *   <li>Error messages and diagnostics</li>
 * </ul>
 *
 * <p><b>Health Check Components:</b>
 * <ul>
 *   <li><b>Storage</b> - LevelDB/Redis connectivity and performance</li>
 *   <li><b>Network</b> - Peer connectivity and communication health</li>
 *   <li><b>Consensus</b> - Consensus participation (validators only)</li>
 *   <li><b>Memory</b> - Memory usage and availability</li>
 *   <li><b>CPU</b> - CPU utilization and responsiveness</li>
 * </ul>
 *
 * <p><b>Usage Example:</b>
 * <pre>{@code
 * NodeHealth health = NodeHealth.builder()
 *     .healthy(true)
 *     .status(NodeStatus.RUNNING)
 *     .lastCheckTime(Instant.now())
 *     .uptimeSeconds(3600L)
 *     .addCheck("storage", true, "LevelDB operational")
 *     .addCheck("network", true, "Connected to 5 peers")
 *     .build();
 * }</pre>
 *
 * @author Aurigraph V11 Platform
 * @version 11.0.0
 * @since 2025-10-11
 * @see io.aurigraph.v11.demo.nodes.Node
 * @see NodeStatus
 */
public class NodeHealth {

    private final boolean healthy;
    private final NodeStatus status;
    private final Instant lastCheckTime;
    private final Long uptimeSeconds;
    private final Map<String, HealthCheck> componentChecks;
    private final String errorMessage;

    /**
     * Private constructor for builder pattern.
     */
    private NodeHealth(Builder builder) {
        this.healthy = builder.healthy;
        this.status = builder.status;
        this.lastCheckTime = builder.lastCheckTime;
        this.uptimeSeconds = builder.uptimeSeconds;
        this.componentChecks = new HashMap<>(builder.componentChecks);
        this.errorMessage = builder.errorMessage;
    }

    /**
     * Creates a new builder for NodeHealth.
     *
     * @return a new Builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Checks if the node is healthy.
     *
     * <p>A node is considered healthy if:
     * <ul>
     *   <li>All critical component checks pass</li>
     *   <li>Node status is RUNNING</li>
     *   <li>No critical errors are present</li>
     * </ul>
     *
     * @return true if the node is healthy, false otherwise
     */
    public boolean isHealthy() {
        return healthy;
    }

    /**
     * Returns the current operational status of the node.
     *
     * @return the node status
     */
    public NodeStatus getStatus() {
        return status;
    }

    /**
     * Returns the timestamp of the last health check.
     *
     * @return the last check time, or null if never checked
     */
    public Instant getLastCheckTime() {
        return lastCheckTime;
    }

    /**
     * Returns the node uptime in seconds.
     *
     * @return the uptime in seconds, or null if not available
     */
    public Long getUptimeSeconds() {
        return uptimeSeconds;
    }

    /**
     * Returns the map of component health checks.
     *
     * @return an unmodifiable view of component checks
     */
    public Map<String, HealthCheck> getComponentChecks() {
        return Map.copyOf(componentChecks);
    }

    /**
     * Returns the error message if the node is unhealthy.
     *
     * @return the error message, or null if healthy
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * Checks if a specific component is healthy.
     *
     * @param componentName the name of the component to check
     * @return true if the component is healthy, false otherwise
     */
    public boolean isComponentHealthy(String componentName) {
        HealthCheck check = componentChecks.get(componentName);
        return check != null && check.isHealthy();
    }

    /**
     * Gets the health check details for a specific component.
     *
     * @param componentName the name of the component
     * @return the health check details, or null if not found
     */
    public HealthCheck getComponentCheck(String componentName) {
        return componentChecks.get(componentName);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NodeHealth that = (NodeHealth) o;
        return healthy == that.healthy &&
               status == that.status &&
               Objects.equals(lastCheckTime, that.lastCheckTime) &&
               Objects.equals(uptimeSeconds, that.uptimeSeconds) &&
               Objects.equals(componentChecks, that.componentChecks) &&
               Objects.equals(errorMessage, that.errorMessage);
    }

    @Override
    public int hashCode() {
        return Objects.hash(healthy, status, lastCheckTime, uptimeSeconds, componentChecks, errorMessage);
    }

    @Override
    public String toString() {
        return "NodeHealth{" +
               "healthy=" + healthy +
               ", status=" + status +
               ", lastCheckTime=" + lastCheckTime +
               ", uptimeSeconds=" + uptimeSeconds +
               ", componentChecks=" + componentChecks.size() +
               ", errorMessage='" + errorMessage + '\'' +
               '}';
    }

    /**
     * Represents a health check for a specific component.
     */
    public static class HealthCheck {
        private final boolean healthy;
        private final String message;
        private final Instant checkTime;

        public HealthCheck(boolean healthy, String message) {
            this(healthy, message, Instant.now());
        }

        public HealthCheck(boolean healthy, String message, Instant checkTime) {
            this.healthy = healthy;
            this.message = message;
            this.checkTime = checkTime;
        }

        public boolean isHealthy() {
            return healthy;
        }

        public String getMessage() {
            return message;
        }

        public Instant getCheckTime() {
            return checkTime;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            HealthCheck that = (HealthCheck) o;
            return healthy == that.healthy &&
                   Objects.equals(message, that.message) &&
                   Objects.equals(checkTime, that.checkTime);
        }

        @Override
        public int hashCode() {
            return Objects.hash(healthy, message, checkTime);
        }

        @Override
        public String toString() {
            return "HealthCheck{" +
                   "healthy=" + healthy +
                   ", message='" + message + '\'' +
                   ", checkTime=" + checkTime +
                   '}';
        }
    }

    /**
     * Builder for NodeHealth instances.
     */
    public static class Builder {
        private boolean healthy = true;
        private NodeStatus status = NodeStatus.INITIALIZING;
        private Instant lastCheckTime;
        private Long uptimeSeconds;
        private Map<String, HealthCheck> componentChecks = new HashMap<>();
        private String errorMessage;

        private Builder() {}

        /**
         * Sets whether the node is healthy.
         *
         * @param healthy true if healthy, false otherwise
         * @return this builder
         */
        public Builder healthy(boolean healthy) {
            this.healthy = healthy;
            return this;
        }

        /**
         * Sets the node status.
         *
         * @param status the node status
         * @return this builder
         */
        public Builder status(NodeStatus status) {
            this.status = status;
            return this;
        }

        /**
         * Sets the last health check time.
         *
         * @param lastCheckTime the check timestamp
         * @return this builder
         */
        public Builder lastCheckTime(Instant lastCheckTime) {
            this.lastCheckTime = lastCheckTime;
            return this;
        }

        /**
         * Sets the node uptime in seconds.
         *
         * @param uptimeSeconds the uptime in seconds
         * @return this builder
         */
        public Builder uptimeSeconds(Long uptimeSeconds) {
            this.uptimeSeconds = uptimeSeconds;
            return this;
        }

        /**
         * Adds a component health check.
         *
         * @param componentName the name of the component
         * @param healthy whether the component is healthy
         * @param message the health check message
         * @return this builder
         */
        public Builder addCheck(String componentName, boolean healthy, String message) {
            this.componentChecks.put(componentName, new HealthCheck(healthy, message));
            return this;
        }

        /**
         * Adds a component health check with a specific timestamp.
         *
         * @param componentName the name of the component
         * @param healthy whether the component is healthy
         * @param message the health check message
         * @param checkTime the time of the check
         * @return this builder
         */
        public Builder addCheck(String componentName, boolean healthy, String message, Instant checkTime) {
            this.componentChecks.put(componentName, new HealthCheck(healthy, message, checkTime));
            return this;
        }

        /**
         * Sets the error message (typically used when healthy = false).
         *
         * @param errorMessage the error message
         * @return this builder
         */
        public Builder errorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
            return this;
        }

        /**
         * Builds the NodeHealth instance.
         *
         * @return the constructed NodeHealth
         */
        public NodeHealth build() {
            return new NodeHealth(this);
        }
    }
}
