package io.aurigraph.v11.portal.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.List;

public class SystemStatusDTO {
    @JsonProperty("system_health")
    private String systemHealth;
    @JsonProperty("timestamp")
    private Instant timestamp;
    @JsonProperty("cpu_usage")
    private Double cpuUsage;
    @JsonProperty("memory_usage")
    private Double memoryUsage;
    @JsonProperty("disk_usage")
    private Double diskUsage;
    @JsonProperty("network_i0")
    private Double networkI0;
    @JsonProperty("database_latency")
    private Integer databaseLatency;
    @JsonProperty("api_response_time")
    private Integer apiResponseTime;
    @JsonProperty("cache_hit_rate")
    private Double cacheHitRate;
    @JsonProperty("error_rate")
    private Double errorRate;
    @JsonProperty("warning_count")
    private Integer warningCount;
    @JsonProperty("critical_count")
    private Integer criticalCount;
    @JsonProperty("last_restart")
    private Instant lastRestart;
    @JsonProperty("last_update")
    private Instant lastUpdate;
    @JsonProperty("next_maintenance_window")
    private Instant nextMaintenanceWindow;
    @JsonProperty("redundancy_status")
    private String redundancyStatus;
    @JsonProperty("backup_status")
    private String backupStatus;
    @JsonProperty("monitoring_status")
    private String monitoringStatus;
    @JsonProperty("security_status")
    private String securityStatus;
    @JsonProperty("compliance_status")
    private String complianceStatus;
    @JsonProperty("error")
    private String error;

    public SystemStatusDTO() {}

    private SystemStatusDTO(Builder builder) {
        this.systemHealth = builder.systemHealth;
        this.timestamp = builder.timestamp;
        this.cpuUsage = builder.cpuUsage;
        this.memoryUsage = builder.memoryUsage;
        this.diskUsage = builder.diskUsage;
        this.networkI0 = builder.networkI0;
        this.databaseLatency = builder.databaseLatency;
        this.apiResponseTime = builder.apiResponseTime;
        this.cacheHitRate = builder.cacheHitRate;
        this.errorRate = builder.errorRate;
        this.warningCount = builder.warningCount;
        this.criticalCount = builder.criticalCount;
        this.lastRestart = builder.lastRestart;
        this.lastUpdate = builder.lastUpdate;
        this.nextMaintenanceWindow = builder.nextMaintenanceWindow;
        this.redundancyStatus = builder.redundancyStatus;
        this.backupStatus = builder.backupStatus;
        this.monitoringStatus = builder.monitoringStatus;
        this.securityStatus = builder.securityStatus;
        this.complianceStatus = builder.complianceStatus;
        this.error = builder.error;
    }

    public String getSystemHealth() { return systemHealth; }
    public Instant getTimestamp() { return timestamp; }
    public Double getCpuUsage() { return cpuUsage; }
    public Double getMemoryUsage() { return memoryUsage; }
    public Double getDiskUsage() { return diskUsage; }
    public Double getNetworkI0() { return networkI0; }
    public Integer getDatabaseLatency() { return databaseLatency; }
    public Integer getApiResponseTime() { return apiResponseTime; }
    public Double getCacheHitRate() { return cacheHitRate; }
    public Double getErrorRate() { return errorRate; }
    public Integer getWarningCount() { return warningCount; }
    public Integer getCriticalCount() { return criticalCount; }
    public Instant getLastRestart() { return lastRestart; }
    public Instant getLastUpdate() { return lastUpdate; }
    public Instant getNextMaintenanceWindow() { return nextMaintenanceWindow; }
    public String getRedundancyStatus() { return redundancyStatus; }
    public String getBackupStatus() { return backupStatus; }
    public String getMonitoringStatus() { return monitoringStatus; }
    public String getSecurityStatus() { return securityStatus; }
    public String getComplianceStatus() { return complianceStatus; }
    public String getError() { return error; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String systemHealth;
        private Instant timestamp;
        private Double cpuUsage;
        private Double memoryUsage;
        private Double diskUsage;
        private Double networkI0;
        private Integer databaseLatency;
        private Integer apiResponseTime;
        private Double cacheHitRate;
        private Double errorRate;
        private Integer warningCount;
        private Integer criticalCount;
        private Instant lastRestart;
        private Instant lastUpdate;
        private Instant nextMaintenanceWindow;
        private String redundancyStatus;
        private String backupStatus;
        private String monitoringStatus;
        private String securityStatus;
        private String complianceStatus;
        private String error;

        public Builder systemHealth(String systemHealth) { this.systemHealth = systemHealth; return this; }
        public Builder timestamp(Instant timestamp) { this.timestamp = timestamp; return this; }
        public Builder cpuUsage(Double cpuUsage) { this.cpuUsage = cpuUsage; return this; }
        public Builder memoryUsage(Double memoryUsage) { this.memoryUsage = memoryUsage; return this; }
        public Builder diskUsage(Double diskUsage) { this.diskUsage = diskUsage; return this; }
        public Builder networkI0(Double networkI0) { this.networkI0 = networkI0; return this; }
        public Builder databaseLatency(Integer databaseLatency) { this.databaseLatency = databaseLatency; return this; }
        public Builder apiResponseTime(Integer apiResponseTime) { this.apiResponseTime = apiResponseTime; return this; }
        public Builder cacheHitRate(Double cacheHitRate) { this.cacheHitRate = cacheHitRate; return this; }
        public Builder errorRate(Double errorRate) { this.errorRate = errorRate; return this; }
        public Builder warningCount(Integer warningCount) { this.warningCount = warningCount; return this; }
        public Builder criticalCount(Integer criticalCount) { this.criticalCount = criticalCount; return this; }
        public Builder lastRestart(Instant lastRestart) { this.lastRestart = lastRestart; return this; }
        public Builder lastUpdate(Instant lastUpdate) { this.lastUpdate = lastUpdate; return this; }
        public Builder nextMaintenanceWindow(Instant nextMaintenanceWindow) { this.nextMaintenanceWindow = nextMaintenanceWindow; return this; }
        public Builder redundancyStatus(String redundancyStatus) { this.redundancyStatus = redundancyStatus; return this; }
        public Builder backupStatus(String backupStatus) { this.backupStatus = backupStatus; return this; }
        public Builder monitoringStatus(String monitoringStatus) { this.monitoringStatus = monitoringStatus; return this; }
        public Builder securityStatus(String securityStatus) { this.securityStatus = securityStatus; return this; }
        public Builder complianceStatus(String complianceStatus) { this.complianceStatus = complianceStatus; return this; }
        public Builder error(String error) { this.error = error; return this; }

        public SystemStatusDTO build() { return new SystemStatusDTO(this); }
    }
}
