package io.aurigraph.v11.portal.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.List;

public class PerformanceAnalyticsDTO {
    @JsonProperty("period")
    private String period;
    @JsonProperty("average_tps")
    private Integer averageTps;
    @JsonProperty("peak_tps")
    private Integer peakTps;
    @JsonProperty("peak_tps_time")
    private String peakTpsTime;
    @JsonProperty("minimum_tps")
    private Integer minimumTps;
    @JsonProperty("minimum_tps_time")
    private String minimumTpsTime;
    @JsonProperty("tps_variance")
    private Double tpsVariance;
    @JsonProperty("average_block_time")
    private Double averageBlockTime;
    @JsonProperty("min_block_time")
    private Double minBlockTime;
    @JsonProperty("max_block_time")
    private Double maxBlockTime;
    @JsonProperty("block_time_variance")
    private Double blockTimeVariance;
    @JsonProperty("average_finality")
    private Integer averageFinality;
    @JsonProperty("min_finality")
    private Integer minFinality;
    @JsonProperty("max_finality")
    private Integer maxFinality;
    @JsonProperty("finality_variance")
    private Double finalityVariance;
    @JsonProperty("network_latency_p50")
    private Integer networkLatencyP50;
    @JsonProperty("network_latency_p95")
    private Integer networkLatencyP95;
    @JsonProperty("network_latency_p99")
    private Integer networkLatencyP99;
    @JsonProperty("cpu_usage_average")
    private Double cpuUsageAverage;
    @JsonProperty("cpu_usage_peak")
    private Double cpuUsagePeak;
    @JsonProperty("memory_usage_average")
    private Double memoryUsageAverage;
    @JsonProperty("memory_usage_peak")
    private Double memoryUsagePeak;
    @JsonProperty("disk_io_read_mbps")
    private Double diskIoReadMbps;
    @JsonProperty("disk_io_write_mbps")
    private Double diskIoWriteMbps;
    @JsonProperty("network_bandwidth_in")
    private Double networkBandwidthIn;
    @JsonProperty("network_bandwidth_out")
    private Double networkBandwidthOut;
    @JsonProperty("uptime")
    private Double uptime;
    @JsonProperty("error")
    private String error;

    public PerformanceAnalyticsDTO() {}

    private PerformanceAnalyticsDTO(Builder builder) {
        this.period = builder.period;
        this.averageTps = builder.averageTps;
        this.peakTps = builder.peakTps;
        this.peakTpsTime = builder.peakTpsTime;
        this.minimumTps = builder.minimumTps;
        this.minimumTpsTime = builder.minimumTpsTime;
        this.tpsVariance = builder.tpsVariance;
        this.averageBlockTime = builder.averageBlockTime;
        this.minBlockTime = builder.minBlockTime;
        this.maxBlockTime = builder.maxBlockTime;
        this.blockTimeVariance = builder.blockTimeVariance;
        this.averageFinality = builder.averageFinality;
        this.minFinality = builder.minFinality;
        this.maxFinality = builder.maxFinality;
        this.finalityVariance = builder.finalityVariance;
        this.networkLatencyP50 = builder.networkLatencyP50;
        this.networkLatencyP95 = builder.networkLatencyP95;
        this.networkLatencyP99 = builder.networkLatencyP99;
        this.cpuUsageAverage = builder.cpuUsageAverage;
        this.cpuUsagePeak = builder.cpuUsagePeak;
        this.memoryUsageAverage = builder.memoryUsageAverage;
        this.memoryUsagePeak = builder.memoryUsagePeak;
        this.diskIoReadMbps = builder.diskIoReadMbps;
        this.diskIoWriteMbps = builder.diskIoWriteMbps;
        this.networkBandwidthIn = builder.networkBandwidthIn;
        this.networkBandwidthOut = builder.networkBandwidthOut;
        this.uptime = builder.uptime;
        this.error = builder.error;
    }

    public String getPeriod() { return period; }
    public Integer getAverageTps() { return averageTps; }
    public Integer getPeakTps() { return peakTps; }
    public String getPeakTpsTime() { return peakTpsTime; }
    public Integer getMinimumTps() { return minimumTps; }
    public String getMinimumTpsTime() { return minimumTpsTime; }
    public Double getTpsVariance() { return tpsVariance; }
    public Double getAverageBlockTime() { return averageBlockTime; }
    public Double getMinBlockTime() { return minBlockTime; }
    public Double getMaxBlockTime() { return maxBlockTime; }
    public Double getBlockTimeVariance() { return blockTimeVariance; }
    public Integer getAverageFinality() { return averageFinality; }
    public Integer getMinFinality() { return minFinality; }
    public Integer getMaxFinality() { return maxFinality; }
    public Double getFinalityVariance() { return finalityVariance; }
    public Integer getNetworkLatencyP50() { return networkLatencyP50; }
    public Integer getNetworkLatencyP95() { return networkLatencyP95; }
    public Integer getNetworkLatencyP99() { return networkLatencyP99; }
    public Double getCpuUsageAverage() { return cpuUsageAverage; }
    public Double getCpuUsagePeak() { return cpuUsagePeak; }
    public Double getMemoryUsageAverage() { return memoryUsageAverage; }
    public Double getMemoryUsagePeak() { return memoryUsagePeak; }
    public Double getDiskIoReadMbps() { return diskIoReadMbps; }
    public Double getDiskIoWriteMbps() { return diskIoWriteMbps; }
    public Double getNetworkBandwidthIn() { return networkBandwidthIn; }
    public Double getNetworkBandwidthOut() { return networkBandwidthOut; }
    public Double getUptime() { return uptime; }
    public String getError() { return error; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String period;
        private Integer averageTps;
        private Integer peakTps;
        private String peakTpsTime;
        private Integer minimumTps;
        private String minimumTpsTime;
        private Double tpsVariance;
        private Double averageBlockTime;
        private Double minBlockTime;
        private Double maxBlockTime;
        private Double blockTimeVariance;
        private Integer averageFinality;
        private Integer minFinality;
        private Integer maxFinality;
        private Double finalityVariance;
        private Integer networkLatencyP50;
        private Integer networkLatencyP95;
        private Integer networkLatencyP99;
        private Double cpuUsageAverage;
        private Double cpuUsagePeak;
        private Double memoryUsageAverage;
        private Double memoryUsagePeak;
        private Double diskIoReadMbps;
        private Double diskIoWriteMbps;
        private Double networkBandwidthIn;
        private Double networkBandwidthOut;
        private Double uptime;
        private String error;

        public Builder period(String period) { this.period = period; return this; }
        public Builder averageTps(Integer averageTps) { this.averageTps = averageTps; return this; }
        public Builder peakTps(Integer peakTps) { this.peakTps = peakTps; return this; }
        public Builder peakTpsTime(String peakTpsTime) { this.peakTpsTime = peakTpsTime; return this; }
        public Builder minimumTps(Integer minimumTps) { this.minimumTps = minimumTps; return this; }
        public Builder minimumTpsTime(String minimumTpsTime) { this.minimumTpsTime = minimumTpsTime; return this; }
        public Builder tpsVariance(Double tpsVariance) { this.tpsVariance = tpsVariance; return this; }
        public Builder averageBlockTime(Double averageBlockTime) { this.averageBlockTime = averageBlockTime; return this; }
        public Builder minBlockTime(Double minBlockTime) { this.minBlockTime = minBlockTime; return this; }
        public Builder maxBlockTime(Double maxBlockTime) { this.maxBlockTime = maxBlockTime; return this; }
        public Builder blockTimeVariance(Double blockTimeVariance) { this.blockTimeVariance = blockTimeVariance; return this; }
        public Builder averageFinality(Integer averageFinality) { this.averageFinality = averageFinality; return this; }
        public Builder minFinality(Integer minFinality) { this.minFinality = minFinality; return this; }
        public Builder maxFinality(Integer maxFinality) { this.maxFinality = maxFinality; return this; }
        public Builder finalityVariance(Double finalityVariance) { this.finalityVariance = finalityVariance; return this; }
        public Builder networkLatencyP50(Integer networkLatencyP50) { this.networkLatencyP50 = networkLatencyP50; return this; }
        public Builder networkLatencyP95(Integer networkLatencyP95) { this.networkLatencyP95 = networkLatencyP95; return this; }
        public Builder networkLatencyP99(Integer networkLatencyP99) { this.networkLatencyP99 = networkLatencyP99; return this; }
        public Builder cpuUsageAverage(Double cpuUsageAverage) { this.cpuUsageAverage = cpuUsageAverage; return this; }
        public Builder cpuUsagePeak(Double cpuUsagePeak) { this.cpuUsagePeak = cpuUsagePeak; return this; }
        public Builder memoryUsageAverage(Double memoryUsageAverage) { this.memoryUsageAverage = memoryUsageAverage; return this; }
        public Builder memoryUsagePeak(Double memoryUsagePeak) { this.memoryUsagePeak = memoryUsagePeak; return this; }
        public Builder diskIoReadMbps(Double diskIoReadMbps) { this.diskIoReadMbps = diskIoReadMbps; return this; }
        public Builder diskIoWriteMbps(Double diskIoWriteMbps) { this.diskIoWriteMbps = diskIoWriteMbps; return this; }
        public Builder networkBandwidthIn(Double networkBandwidthIn) { this.networkBandwidthIn = networkBandwidthIn; return this; }
        public Builder networkBandwidthOut(Double networkBandwidthOut) { this.networkBandwidthOut = networkBandwidthOut; return this; }
        public Builder uptime(Double uptime) { this.uptime = uptime; return this; }
        public Builder error(String error) { this.error = error; return this; }

        public PerformanceAnalyticsDTO build() { return new PerformanceAnalyticsDTO(this); }
    }
}
