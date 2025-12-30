package io.aurigraph.v11.demo.config;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.aurigraph.v11.demo.models.NodeType;
import io.quarkus.runtime.annotations.RegisterForReflection;

import java.util.List;
import java.util.Objects;

/**
 * Base configuration class for all Aurigraph V11 nodes.
 *
 * This class provides common configuration properties shared across all node types:
 * - Channel Nodes
 * - Validator Nodes
 * - Business Nodes
 * - API Integration Nodes
 *
 * Node-specific configurations extend this base class with additional properties.
 *
 * @see ChannelNodeConfig
 * @see ValidatorNodeConfig
 * @see BusinessNodeConfig
 * @see APINodeConfig
 */
@RegisterForReflection
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "nodeType"
)
@JsonSubTypes({
    @JsonSubTypes.Type(value = ChannelNodeConfig.class, name = "CHANNEL"),
    @JsonSubTypes.Type(value = ValidatorNodeConfig.class, name = "VALIDATOR"),
    @JsonSubTypes.Type(value = BusinessNodeConfig.class, name = "BUSINESS"),
    @JsonSubTypes.Type(value = APINodeConfig.class, name = "API_INTEGRATION")
})
public abstract class NodeConfiguration {

    /**
     * Unique identifier for this node instance.
     * Must be unique across the entire network.
     */
    private String nodeId;

    /**
     * Type of node (CHANNEL, VALIDATOR, BUSINESS, API_INTEGRATION).
     */
    private NodeType nodeType;

    /**
     * Version of the node software.
     * Format: MAJOR.MINOR.PATCH (e.g., "12.0.0")
     */
    private String version = "12.0.0";

    /**
     * Network-level configuration.
     */
    private NetworkConfig network;

    /**
     * Performance-related configuration.
     */
    private PerformanceConfig performance;

    /**
     * Security configuration.
     */
    private SecurityConfig security;

    /**
     * Monitoring and observability configuration.
     */
    private MonitoringConfig monitoring;

    // Constructors

    public NodeConfiguration() {
        // Default constructor for Jackson
    }

    public NodeConfiguration(String nodeId, NodeType nodeType) {
        this.nodeId = nodeId;
        this.nodeType = nodeType;
        this.network = new NetworkConfig();
        this.performance = new PerformanceConfig();
        this.security = new SecurityConfig();
        this.monitoring = new MonitoringConfig();
    }

    // Getters and Setters

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public NodeType getNodeType() {
        return nodeType;
    }

    public void setNodeType(NodeType nodeType) {
        this.nodeType = nodeType;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public NetworkConfig getNetwork() {
        return network;
    }

    public void setNetwork(NetworkConfig network) {
        this.network = network;
    }

    public PerformanceConfig getPerformance() {
        return performance;
    }

    public void setPerformance(PerformanceConfig performance) {
        this.performance = performance;
    }

    public SecurityConfig getSecurity() {
        return security;
    }

    public void setSecurity(SecurityConfig security) {
        this.security = security;
    }

    public MonitoringConfig getMonitoring() {
        return monitoring;
    }

    public void setMonitoring(MonitoringConfig monitoring) {
        this.monitoring = monitoring;
    }

    /**
     * Validates the configuration.
     * Subclasses should override this method and call super.validate() first.
     *
     * @throws IllegalArgumentException if validation fails
     */
    public void validate() {
        if (nodeId == null || nodeId.trim().isEmpty()) {
            throw new IllegalArgumentException("Node ID cannot be null or empty");
        }

        if (nodeType == null) {
            throw new IllegalArgumentException("Node type cannot be null");
        }

        if (version == null || !version.matches("\\d+\\.\\d+\\.\\d+")) {
            throw new IllegalArgumentException("Version must be in MAJOR.MINOR.PATCH format");
        }

        if (network != null) {
            network.validate();
        }

        if (performance != null) {
            performance.validate();
        }

        if (security != null) {
            security.validate();
        }

        if (monitoring != null) {
            monitoring.validate();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof NodeConfiguration)) return false;
        NodeConfiguration that = (NodeConfiguration) o;
        return Objects.equals(nodeId, that.nodeId) &&
               nodeType == that.nodeType &&
               Objects.equals(version, that.version);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nodeId, nodeType, version);
    }

    @Override
    public String toString() {
        return "NodeConfiguration{" +
               "nodeId='" + nodeId + '\'' +
               ", nodeType=" + nodeType +
               ", version='" + version + '\'' +
               '}';
    }

    /**
     * Network configuration for node connectivity.
     */
    @RegisterForReflection
    public static class NetworkConfig {
        private String networkId = "aurigraph-mainnet";
        private List<String> bootstrapNodes;
        private int maxPeers = 100;
        private boolean enableDiscovery = true;

        public String getNetworkId() {
            return networkId;
        }

        public void setNetworkId(String networkId) {
            this.networkId = networkId;
        }

        public List<String> getBootstrapNodes() {
            return bootstrapNodes;
        }

        public void setBootstrapNodes(List<String> bootstrapNodes) {
            this.bootstrapNodes = bootstrapNodes;
        }

        public int getMaxPeers() {
            return maxPeers;
        }

        public void setMaxPeers(int maxPeers) {
            this.maxPeers = maxPeers;
        }

        public boolean isEnableDiscovery() {
            return enableDiscovery;
        }

        public void setEnableDiscovery(boolean enableDiscovery) {
            this.enableDiscovery = enableDiscovery;
        }

        public void validate() {
            if (networkId == null || networkId.trim().isEmpty()) {
                throw new IllegalArgumentException("Network ID cannot be null or empty");
            }

            if (maxPeers <= 0) {
                throw new IllegalArgumentException("Max peers must be greater than 0");
            }
        }
    }

    /**
     * Performance configuration for resource allocation.
     */
    @RegisterForReflection
    public static class PerformanceConfig {
        private int threadPoolSize = 256;
        private int queueSize = 100000;
        private int batchSize = 1000;

        public int getThreadPoolSize() {
            return threadPoolSize;
        }

        public void setThreadPoolSize(int threadPoolSize) {
            this.threadPoolSize = threadPoolSize;
        }

        public int getQueueSize() {
            return queueSize;
        }

        public void setQueueSize(int queueSize) {
            this.queueSize = queueSize;
        }

        public int getBatchSize() {
            return batchSize;
        }

        public void setBatchSize(int batchSize) {
            this.batchSize = batchSize;
        }

        public void validate() {
            if (threadPoolSize <= 0) {
                throw new IllegalArgumentException("Thread pool size must be greater than 0");
            }

            if (queueSize <= 0) {
                throw new IllegalArgumentException("Queue size must be greater than 0");
            }

            if (batchSize <= 0) {
                throw new IllegalArgumentException("Batch size must be greater than 0");
            }
        }
    }

    /**
     * Security configuration for cryptography and access control.
     */
    @RegisterForReflection
    public static class SecurityConfig {
        private boolean enableTLS = true;
        private String tlsCertPath = "/etc/aurigraph/tls/cert.pem";
        private String tlsKeyPath = "/etc/aurigraph/tls/key.pem";
        private boolean quantumResistant = true;

        public boolean isEnableTLS() {
            return enableTLS;
        }

        public void setEnableTLS(boolean enableTLS) {
            this.enableTLS = enableTLS;
        }

        public String getTlsCertPath() {
            return tlsCertPath;
        }

        public void setTlsCertPath(String tlsCertPath) {
            this.tlsCertPath = tlsCertPath;
        }

        public String getTlsKeyPath() {
            return tlsKeyPath;
        }

        public void setTlsKeyPath(String tlsKeyPath) {
            this.tlsKeyPath = tlsKeyPath;
        }

        public boolean isQuantumResistant() {
            return quantumResistant;
        }

        public void setQuantumResistant(boolean quantumResistant) {
            this.quantumResistant = quantumResistant;
        }

        public void validate() {
            if (enableTLS) {
                if (tlsCertPath == null || tlsCertPath.trim().isEmpty()) {
                    throw new IllegalArgumentException("TLS cert path cannot be null or empty when TLS is enabled");
                }

                if (tlsKeyPath == null || tlsKeyPath.trim().isEmpty()) {
                    throw new IllegalArgumentException("TLS key path cannot be null or empty when TLS is enabled");
                }
            }
        }
    }

    /**
     * Monitoring configuration for metrics and health checks.
     */
    @RegisterForReflection
    public static class MonitoringConfig {
        private boolean metricsEnabled = true;
        private int metricsPort = 9090;
        private int healthCheckPort = 9091;
        private String logLevel = "INFO";

        public boolean isMetricsEnabled() {
            return metricsEnabled;
        }

        public void setMetricsEnabled(boolean metricsEnabled) {
            this.metricsEnabled = metricsEnabled;
        }

        public int getMetricsPort() {
            return metricsPort;
        }

        public void setMetricsPort(int metricsPort) {
            this.metricsPort = metricsPort;
        }

        public int getHealthCheckPort() {
            return healthCheckPort;
        }

        public void setHealthCheckPort(int healthCheckPort) {
            this.healthCheckPort = healthCheckPort;
        }

        public String getLogLevel() {
            return logLevel;
        }

        public void setLogLevel(String logLevel) {
            this.logLevel = logLevel;
        }

        public void validate() {
            if (metricsPort <= 0 || metricsPort > 65535) {
                throw new IllegalArgumentException("Metrics port must be between 1 and 65535");
            }

            if (healthCheckPort <= 0 || healthCheckPort > 65535) {
                throw new IllegalArgumentException("Health check port must be between 1 and 65535");
            }

            if (!logLevel.matches("TRACE|DEBUG|INFO|WARN|ERROR")) {
                throw new IllegalArgumentException("Log level must be one of: TRACE, DEBUG, INFO, WARN, ERROR");
            }
        }
    }
}
