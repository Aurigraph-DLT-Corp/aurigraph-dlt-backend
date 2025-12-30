package io.aurigraph.v11.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * System information model for platform version and configuration
 * Used by /api/v11/info endpoint
 *
 * @author Aurigraph V11
 * @version 11.3.0
 */
public class SystemInfo {

    @JsonProperty("platform")
    private PlatformInfo platform;

    @JsonProperty("runtime")
    private RuntimeInfo runtime;

    @JsonProperty("features")
    private FeaturesInfo features;

    @JsonProperty("network")
    private NetworkInfo network;

    @JsonProperty("build")
    private BuildInfo build;

    @JsonProperty("timestamp")
    private Instant timestamp;

    // Constructor
    public SystemInfo() {
        this.timestamp = Instant.now();
    }

    // Getters and Setters
    public PlatformInfo getPlatform() {
        return platform;
    }

    public void setPlatform(PlatformInfo platform) {
        this.platform = platform;
    }

    public RuntimeInfo getRuntime() {
        return runtime;
    }

    public void setRuntime(RuntimeInfo runtime) {
        this.runtime = runtime;
    }

    public FeaturesInfo getFeatures() {
        return features;
    }

    public void setFeatures(FeaturesInfo features) {
        this.features = features;
    }

    public NetworkInfo getNetwork() {
        return network;
    }

    public void setNetwork(NetworkInfo network) {
        this.network = network;
    }

    public BuildInfo getBuild() {
        return build;
    }

    public void setBuild(BuildInfo build) {
        this.build = build;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * Platform information
     */
    public static class PlatformInfo {
        @JsonProperty("name")
        private String name;

        @JsonProperty("version")
        private String version;

        @JsonProperty("description")
        private String description;

        @JsonProperty("environment")
        private String environment; // "development", "staging", "production"

        public PlatformInfo() {}

        public PlatformInfo(String name, String version, String description, String environment) {
            this.name = name;
            this.version = version;
            this.description = description;
            this.environment = environment;
        }

        // Getters and setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getVersion() { return version; }
        public void setVersion(String version) { this.version = version; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public String getEnvironment() { return environment; }
        public void setEnvironment(String environment) { this.environment = environment; }
    }

    /**
     * Runtime information
     */
    public static class RuntimeInfo {
        @JsonProperty("java_version")
        private String javaVersion;

        @JsonProperty("quarkus_version")
        private String quarkusVersion;

        @JsonProperty("graalvm_version")
        private String graalvmVersion;

        @JsonProperty("native_mode")
        private boolean nativeMode;

        @JsonProperty("uptime_seconds")
        private long uptimeSeconds;

        @JsonProperty("start_time")
        private Instant startTime;

        public RuntimeInfo() {}

        // Getters and setters
        public String getJavaVersion() { return javaVersion; }
        public void setJavaVersion(String javaVersion) { this.javaVersion = javaVersion; }

        public String getQuarkusVersion() { return quarkusVersion; }
        public void setQuarkusVersion(String quarkusVersion) { this.quarkusVersion = quarkusVersion; }

        public String getGraalvmVersion() { return graalvmVersion; }
        public void setGraalvmVersion(String graalvmVersion) { this.graalvmVersion = graalvmVersion; }

        public boolean isNativeMode() { return nativeMode; }
        public void setNativeMode(boolean nativeMode) { this.nativeMode = nativeMode; }

        public long getUptimeSeconds() { return uptimeSeconds; }
        public void setUptimeSeconds(long uptimeSeconds) { this.uptimeSeconds = uptimeSeconds; }

        public Instant getStartTime() { return startTime; }
        public void setStartTime(Instant startTime) { this.startTime = startTime; }
    }

    /**
     * Features information
     */
    public static class FeaturesInfo {
        @JsonProperty("consensus")
        private String consensus; // "HyperRAFT++"

        @JsonProperty("cryptography")
        private String cryptography; // "Quantum-Resistant (Kyber, Dilithium)"

        @JsonProperty("enabled_modules")
        private List<String> enabledModules;

        @JsonProperty("api_version")
        private String apiVersion;

        @JsonProperty("supported_protocols")
        private List<String> supportedProtocols; // ["REST", "gRPC", "WebSocket"]

        public FeaturesInfo() {}

        // Getters and setters
        public String getConsensus() { return consensus; }
        public void setConsensus(String consensus) { this.consensus = consensus; }

        public String getCryptography() { return cryptography; }
        public void setCryptography(String cryptography) { this.cryptography = cryptography; }

        public List<String> getEnabledModules() { return enabledModules; }
        public void setEnabledModules(List<String> enabledModules) { this.enabledModules = enabledModules; }

        public String getApiVersion() { return apiVersion; }
        public void setApiVersion(String apiVersion) { this.apiVersion = apiVersion; }

        public List<String> getSupportedProtocols() { return supportedProtocols; }
        public void setSupportedProtocols(List<String> supportedProtocols) { this.supportedProtocols = supportedProtocols; }
    }

    /**
     * Network configuration information (non-sensitive)
     */
    public static class NetworkInfo {
        @JsonProperty("node_type")
        private String nodeType; // "validator", "full_node", "api_node"

        @JsonProperty("network_id")
        private String networkId;

        @JsonProperty("cluster_size")
        private int clusterSize;

        @JsonProperty("api_endpoint")
        private String apiEndpoint;

        @JsonProperty("ports")
        private Map<String, Integer> ports; // {"http": 9003, "grpc": 9004}

        public NetworkInfo() {}

        // Getters and setters
        public String getNodeType() { return nodeType; }
        public void setNodeType(String nodeType) { this.nodeType = nodeType; }

        public String getNetworkId() { return networkId; }
        public void setNetworkId(String networkId) { this.networkId = networkId; }

        public int getClusterSize() { return clusterSize; }
        public void setClusterSize(int clusterSize) { this.clusterSize = clusterSize; }

        public String getApiEndpoint() { return apiEndpoint; }
        public void setApiEndpoint(String apiEndpoint) { this.apiEndpoint = apiEndpoint; }

        public Map<String, Integer> getPorts() { return ports; }
        public void setPorts(Map<String, Integer> ports) { this.ports = ports; }
    }

    /**
     * Build information
     */
    public static class BuildInfo {
        @JsonProperty("version")
        private String version;

        @JsonProperty("build_timestamp")
        private String buildTimestamp;

        @JsonProperty("commit_hash")
        private String commitHash;

        @JsonProperty("branch")
        private String branch;

        @JsonProperty("build_type")
        private String buildType; // "development", "release"

        public BuildInfo() {}

        // Getters and setters
        public String getVersion() { return version; }
        public void setVersion(String version) { this.version = version; }

        public String getBuildTimestamp() { return buildTimestamp; }
        public void setBuildTimestamp(String buildTimestamp) { this.buildTimestamp = buildTimestamp; }

        public String getCommitHash() { return commitHash; }
        public void setCommitHash(String commitHash) { this.commitHash = commitHash; }

        public String getBranch() { return branch; }
        public void setBranch(String branch) { this.branch = branch; }

        public String getBuildType() { return buildType; }
        public void setBuildType(String buildType) { this.buildType = buildType; }
    }
}
