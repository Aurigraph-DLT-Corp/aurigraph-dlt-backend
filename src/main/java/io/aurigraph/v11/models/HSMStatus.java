package io.aurigraph.v11.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Hardware Security Module (HSM) status model
 * Used by /api/v11/security/hsm/status endpoint
 *
 * @author Aurigraph V11
 * @version 11.3.0
 */
public class HSMStatus {

    @JsonProperty("timestamp")
    private Instant timestamp;

    @JsonProperty("modules")
    private List<HSMModule> modules;

    @JsonProperty("overall_status")
    private String overallStatus; // "online", "degraded", "offline"

    @JsonProperty("key_storage")
    private KeyStorageInfo keyStorage;

    @JsonProperty("operations")
    private OperationStats operations;

    @JsonProperty("alerts")
    private List<Alert> alerts;

    // Constructor
    public HSMStatus() {
        this.timestamp = Instant.now();
    }

    // Getters and Setters
    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public List<HSMModule> getModules() {
        return modules;
    }

    public void setModules(List<HSMModule> modules) {
        this.modules = modules;
    }

    public String getOverallStatus() {
        return overallStatus;
    }

    public void setOverallStatus(String overallStatus) {
        this.overallStatus = overallStatus;
    }

    public KeyStorageInfo getKeyStorage() {
        return keyStorage;
    }

    public void setKeyStorage(KeyStorageInfo keyStorage) {
        this.keyStorage = keyStorage;
    }

    public OperationStats getOperations() {
        return operations;
    }

    public void setOperations(OperationStats operations) {
        this.operations = operations;
    }

    public List<Alert> getAlerts() {
        return alerts;
    }

    public void setAlerts(List<Alert> alerts) {
        this.alerts = alerts;
    }

    /**
     * HSM Module information
     */
    public static class HSMModule {
        @JsonProperty("module_id")
        private String moduleId;

        @JsonProperty("name")
        private String name;

        @JsonProperty("type")
        private String type; // "hardware", "software", "cloud"

        @JsonProperty("status")
        private String status; // "online", "degraded", "offline"

        @JsonProperty("manufacturer")
        private String manufacturer;

        @JsonProperty("model")
        private String model;

        @JsonProperty("firmware_version")
        private String firmwareVersion;

        @JsonProperty("connection")
        private ConnectionInfo connection;

        @JsonProperty("health")
        private HealthMetrics health;

        public HSMModule() {}

        // Getters and setters
        public String getModuleId() { return moduleId; }
        public void setModuleId(String moduleId) { this.moduleId = moduleId; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public String getManufacturer() { return manufacturer; }
        public void setManufacturer(String manufacturer) { this.manufacturer = manufacturer; }

        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }

        public String getFirmwareVersion() { return firmwareVersion; }
        public void setFirmwareVersion(String firmwareVersion) { this.firmwareVersion = firmwareVersion; }

        public ConnectionInfo getConnection() { return connection; }
        public void setConnection(ConnectionInfo connection) { this.connection = connection; }

        public HealthMetrics getHealth() { return health; }
        public void setHealth(HealthMetrics health) { this.health = health; }
    }

    /**
     * Connection information
     */
    public static class ConnectionInfo {
        @JsonProperty("protocol")
        private String protocol; // "PKCS#11", "REST", "proprietary"

        @JsonProperty("host")
        private String host;

        @JsonProperty("port")
        private int port;

        @JsonProperty("secure")
        private boolean secure;

        @JsonProperty("last_connected")
        private Instant lastConnected;

        @JsonProperty("uptime_seconds")
        private long uptimeSeconds;

        public ConnectionInfo() {}

        // Getters and setters
        public String getProtocol() { return protocol; }
        public void setProtocol(String protocol) { this.protocol = protocol; }

        public String getHost() { return host; }
        public void setHost(String host) { this.host = host; }

        public int getPort() { return port; }
        public void setPort(int port) { this.port = port; }

        public boolean isSecure() { return secure; }
        public void setSecure(boolean secure) { this.secure = secure; }

        public Instant getLastConnected() { return lastConnected; }
        public void setLastConnected(Instant lastConnected) { this.lastConnected = lastConnected; }

        public long getUptimeSeconds() { return uptimeSeconds; }
        public void setUptimeSeconds(long uptimeSeconds) { this.uptimeSeconds = uptimeSeconds; }
    }

    /**
     * Health metrics for HSM module
     */
    public static class HealthMetrics {
        @JsonProperty("temperature_celsius")
        private double temperatureCelsius;

        @JsonProperty("cpu_usage_percent")
        private double cpuUsagePercent;

        @JsonProperty("memory_usage_percent")
        private double memoryUsagePercent;

        @JsonProperty("error_rate")
        private double errorRate; // errors per 1000 operations

        @JsonProperty("response_time_ms")
        private double responseTimeMs;

        public HealthMetrics() {}

        // Getters and setters
        public double getTemperatureCelsius() { return temperatureCelsius; }
        public void setTemperatureCelsius(double temperatureCelsius) { this.temperatureCelsius = temperatureCelsius; }

        public double getCpuUsagePercent() { return cpuUsagePercent; }
        public void setCpuUsagePercent(double cpuUsagePercent) { this.cpuUsagePercent = cpuUsagePercent; }

        public double getMemoryUsagePercent() { return memoryUsagePercent; }
        public void setMemoryUsagePercent(double memoryUsagePercent) { this.memoryUsagePercent = memoryUsagePercent; }

        public double getErrorRate() { return errorRate; }
        public void setErrorRate(double errorRate) { this.errorRate = errorRate; }

        public double getResponseTimeMs() { return responseTimeMs; }
        public void setResponseTimeMs(double responseTimeMs) { this.responseTimeMs = responseTimeMs; }
    }

    /**
     * Key storage information
     */
    public static class KeyStorageInfo {
        @JsonProperty("total_keys")
        private long totalKeys;

        @JsonProperty("key_types")
        private Map<String, Long> keyTypes; // e.g., {"rsa": 100, "ecc": 50, "quantum": 25}

        @JsonProperty("storage_capacity_keys")
        private long storageCapacityKeys;

        @JsonProperty("storage_used_percent")
        private double storageUsedPercent;

        @JsonProperty("backed_up")
        private boolean backedUp;

        @JsonProperty("last_backup")
        private Instant lastBackup;

        public KeyStorageInfo() {}

        // Getters and setters
        public long getTotalKeys() { return totalKeys; }
        public void setTotalKeys(long totalKeys) { this.totalKeys = totalKeys; }

        public Map<String, Long> getKeyTypes() { return keyTypes; }
        public void setKeyTypes(Map<String, Long> keyTypes) { this.keyTypes = keyTypes; }

        public long getStorageCapacityKeys() { return storageCapacityKeys; }
        public void setStorageCapacityKeys(long storageCapacityKeys) { this.storageCapacityKeys = storageCapacityKeys; }

        public double getStorageUsedPercent() { return storageUsedPercent; }
        public void setStorageUsedPercent(double storageUsedPercent) { this.storageUsedPercent = storageUsedPercent; }

        public boolean isBackedUp() { return backedUp; }
        public void setBackedUp(boolean backedUp) { this.backedUp = backedUp; }

        public Instant getLastBackup() { return lastBackup; }
        public void setLastBackup(Instant lastBackup) { this.lastBackup = lastBackup; }
    }

    /**
     * Operation statistics
     */
    public static class OperationStats {
        @JsonProperty("operations_per_second")
        private double operationsPerSecond;

        @JsonProperty("total_operations")
        private long totalOperations;

        @JsonProperty("successful_operations")
        private long successfulOperations;

        @JsonProperty("failed_operations")
        private long failedOperations;

        @JsonProperty("success_rate")
        private double successRate; // 0.0 - 1.0

        @JsonProperty("average_latency_ms")
        private double averageLatencyMs;

        @JsonProperty("operation_breakdown")
        private Map<String, Long> operationBreakdown; // e.g., {"sign": 1000, "verify": 2000, "encrypt": 500}

        public OperationStats() {}

        // Getters and setters
        public double getOperationsPerSecond() { return operationsPerSecond; }
        public void setOperationsPerSecond(double operationsPerSecond) { this.operationsPerSecond = operationsPerSecond; }

        public long getTotalOperations() { return totalOperations; }
        public void setTotalOperations(long totalOperations) { this.totalOperations = totalOperations; }

        public long getSuccessfulOperations() { return successfulOperations; }
        public void setSuccessfulOperations(long successfulOperations) { this.successfulOperations = successfulOperations; }

        public long getFailedOperations() { return failedOperations; }
        public void setFailedOperations(long failedOperations) { this.failedOperations = failedOperations; }

        public double getSuccessRate() { return successRate; }
        public void setSuccessRate(double successRate) { this.successRate = successRate; }

        public double getAverageLatencyMs() { return averageLatencyMs; }
        public void setAverageLatencyMs(double averageLatencyMs) { this.averageLatencyMs = averageLatencyMs; }

        public Map<String, Long> getOperationBreakdown() { return operationBreakdown; }
        public void setOperationBreakdown(Map<String, Long> operationBreakdown) { this.operationBreakdown = operationBreakdown; }
    }

    /**
     * Alert information
     */
    public static class Alert {
        @JsonProperty("id")
        private String id;

        @JsonProperty("severity")
        private String severity; // "info", "warning", "error", "critical"

        @JsonProperty("message")
        private String message;

        @JsonProperty("module_id")
        private String moduleId;

        @JsonProperty("timestamp")
        private Instant timestamp;

        @JsonProperty("acknowledged")
        private boolean acknowledged;

        public Alert() {
            this.timestamp = Instant.now();
        }

        public Alert(String id, String severity, String message, String moduleId) {
            this();
            this.id = id;
            this.severity = severity;
            this.message = message;
            this.moduleId = moduleId;
        }

        // Getters and setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getSeverity() { return severity; }
        public void setSeverity(String severity) { this.severity = severity; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public String getModuleId() { return moduleId; }
        public void setModuleId(String moduleId) { this.moduleId = moduleId; }

        public Instant getTimestamp() { return timestamp; }
        public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }

        public boolean isAcknowledged() { return acknowledged; }
        public void setAcknowledged(boolean acknowledged) { this.acknowledged = acknowledged; }
    }
}
