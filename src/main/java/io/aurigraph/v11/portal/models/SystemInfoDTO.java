package io.aurigraph.v11.portal.models;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SystemInfoDTO {
    @JsonProperty("version")
    private String version;
    @JsonProperty("build_time")
    private String buildTime;
    @JsonProperty("environment")
    private String environment;
    @JsonProperty("uptime_ms")
    private Long uptimeMs;
    @JsonProperty("java_version")
    private String javaVersion;
    @JsonProperty("os")
    private String os;
    @JsonProperty("architecture")
    private String architecture;
    @JsonProperty("processor_count")
    private Integer processorCount;
    @JsonProperty("max_memory_mb")
    private Long maxMemoryMb;
    @JsonProperty("consensus_algorithm")
    private String consensusAlgorithm;
    @JsonProperty("crypto_level")
    private String cryptoLevel;
    @JsonProperty("protocol_version")
    private String protocolVersion;
    @JsonProperty("network_mode")
    private String networkMode;
    @JsonProperty("error")
    private String error;

    public SystemInfoDTO() {}

    private SystemInfoDTO(Builder builder) {
        this.version = builder.version;
        this.buildTime = builder.buildTime;
        this.environment = builder.environment;
        this.uptimeMs = builder.uptimeMs;
        this.javaVersion = builder.javaVersion;
        this.os = builder.os;
        this.architecture = builder.architecture;
        this.processorCount = builder.processorCount;
        this.maxMemoryMb = builder.maxMemoryMb;
        this.consensusAlgorithm = builder.consensusAlgorithm;
        this.cryptoLevel = builder.cryptoLevel;
        this.protocolVersion = builder.protocolVersion;
        this.networkMode = builder.networkMode;
        this.error = builder.error;
    }

    public String getVersion() { return version; }
    public String getBuildTime() { return buildTime; }
    public String getEnvironment() { return environment; }
    public Long getUptimeMs() { return uptimeMs; }
    public String getJavaVersion() { return javaVersion; }
    public String getOs() { return os; }
    public String getArchitecture() { return architecture; }
    public Integer getProcessorCount() { return processorCount; }
    public Long getMaxMemoryMb() { return maxMemoryMb; }
    public String getConsensusAlgorithm() { return consensusAlgorithm; }
    public String getCryptoLevel() { return cryptoLevel; }
    public String getProtocolVersion() { return protocolVersion; }
    public String getNetworkMode() { return networkMode; }
    public String getError() { return error; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String version;
        private String buildTime;
        private String environment;
        private Long uptimeMs;
        private String javaVersion;
        private String os;
        private String architecture;
        private Integer processorCount;
        private Long maxMemoryMb;
        private String consensusAlgorithm;
        private String cryptoLevel;
        private String protocolVersion;
        private String networkMode;
        private String error;

        public Builder version(String version) { this.version = version; return this; }
        public Builder buildTime(String buildTime) { this.buildTime = buildTime; return this; }
        public Builder environment(String environment) { this.environment = environment; return this; }
        public Builder uptimeMs(Long uptimeMs) { this.uptimeMs = uptimeMs; return this; }
        public Builder javaVersion(String javaVersion) { this.javaVersion = javaVersion; return this; }
        public Builder os(String os) { this.os = os; return this; }
        public Builder architecture(String architecture) { this.architecture = architecture; return this; }
        public Builder processorCount(Integer processorCount) { this.processorCount = processorCount; return this; }
        public Builder maxMemoryMb(Long maxMemoryMb) { this.maxMemoryMb = maxMemoryMb; return this; }
        public Builder consensusAlgorithm(String consensusAlgorithm) { this.consensusAlgorithm = consensusAlgorithm; return this; }
        public Builder cryptoLevel(String cryptoLevel) { this.cryptoLevel = cryptoLevel; return this; }
        public Builder protocolVersion(String protocolVersion) { this.protocolVersion = protocolVersion; return this; }
        public Builder networkMode(String networkMode) { this.networkMode = networkMode; return this; }
        public Builder error(String error) { this.error = error; return this; }

        public SystemInfoDTO build() { return new SystemInfoDTO(this); }
    }
}
