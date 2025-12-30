package io.aurigraph.v11.demo.models;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents performance metrics and operational statistics for a node in the Aurigraph V11 network.
 *
 * <p>This class captures comprehensive metrics for monitoring node performance, including:
 * <ul>
 *   <li>Transaction processing metrics (TPS, latency, throughput)</li>
 *   <li>Resource utilization (CPU, memory, disk)</li>
 *   <li>Network statistics (peer count, message throughput)</li>
 *   <li>Type-specific metrics (consensus, channel, business, API)</li>
 *   <li>Latency measurements (average, p50, p95, p99)</li>
 *   <li>Error rates and counts</li>
 * </ul>
 *
 * <p><b>Key Performance Indicators (KPIs):</b>
 * <ul>
 *   <li><b>Transactions Per Second (TPS)</b> - Current transaction processing rate</li>
 *   <li><b>Average Latency</b> - Average time to process a transaction</li>
 *   <li><b>Success Rate</b> - Percentage of successful operations</li>
 *   <li><b>Resource Usage</b> - CPU, memory, and disk utilization</li>
 * </ul>
 *
 * @author Aurigraph V11 Platform
 * @version 11.0.0
 * @since 2025-10-11
 * @see io.aurigraph.v11.demo.nodes.Node
 * @see NodeType
 */
public class NodeMetrics {

    private String nodeId;
    private Instant timestamp;
    private long uptimeSeconds;

    // Throughput metrics
    private long totalRequests;
    private long totalResponses;
    private double requestsPerSecond;
    private double responsesPerSecond;

    // Latency metrics (in milliseconds)
    private double averageLatency;
    private double p50Latency;
    private double p95Latency;
    private double p99Latency;

    // Resource utilization
    private double cpuUsagePercent;
    private long memoryUsedBytes;
    private long memoryTotalBytes;
    private long diskUsedBytes;
    private long diskTotalBytes;

    // Connection/Channel metrics
    private int activeConnections;
    private int activeChannels;
    private int totalParticipants;

    // Error metrics
    private long errorCount;
    private double errorRate;

    // Custom metrics
    private Map<String, Object> customMetrics;

    /**
     * Default constructor.
     */
    public NodeMetrics() {
        this.timestamp = Instant.now();
        this.customMetrics = new HashMap<>();
    }

    /**
     * Creates a node metrics report.
     *
     * @param nodeId The node identifier
     */
    public NodeMetrics(String nodeId) {
        this();
        this.nodeId = nodeId;
    }

    // Getters and Setters

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public long getUptimeSeconds() {
        return uptimeSeconds;
    }

    public void setUptimeSeconds(long uptimeSeconds) {
        this.uptimeSeconds = uptimeSeconds;
    }

    public long getTotalRequests() {
        return totalRequests;
    }

    public void setTotalRequests(long totalRequests) {
        this.totalRequests = totalRequests;
    }

    public long getTotalResponses() {
        return totalResponses;
    }

    public void setTotalResponses(long totalResponses) {
        this.totalResponses = totalResponses;
    }

    public double getRequestsPerSecond() {
        return requestsPerSecond;
    }

    public void setRequestsPerSecond(double requestsPerSecond) {
        this.requestsPerSecond = requestsPerSecond;
    }

    public double getResponsesPerSecond() {
        return responsesPerSecond;
    }

    public void setResponsesPerSecond(double responsesPerSecond) {
        this.responsesPerSecond = responsesPerSecond;
    }

    public double getAverageLatency() {
        return averageLatency;
    }

    public void setAverageLatency(double averageLatency) {
        this.averageLatency = averageLatency;
    }

    public double getP50Latency() {
        return p50Latency;
    }

    public void setP50Latency(double p50Latency) {
        this.p50Latency = p50Latency;
    }

    public double getP95Latency() {
        return p95Latency;
    }

    public void setP95Latency(double p95Latency) {
        this.p95Latency = p95Latency;
    }

    public double getP99Latency() {
        return p99Latency;
    }

    public void setP99Latency(double p99Latency) {
        this.p99Latency = p99Latency;
    }

    public double getCpuUsagePercent() {
        return cpuUsagePercent;
    }

    public void setCpuUsagePercent(double cpuUsagePercent) {
        this.cpuUsagePercent = cpuUsagePercent;
    }

    public long getMemoryUsedBytes() {
        return memoryUsedBytes;
    }

    public void setMemoryUsedBytes(long memoryUsedBytes) {
        this.memoryUsedBytes = memoryUsedBytes;
    }

    public long getMemoryTotalBytes() {
        return memoryTotalBytes;
    }

    public void setMemoryTotalBytes(long memoryTotalBytes) {
        this.memoryTotalBytes = memoryTotalBytes;
    }

    public long getDiskUsedBytes() {
        return diskUsedBytes;
    }

    public void setDiskUsedBytes(long diskUsedBytes) {
        this.diskUsedBytes = diskUsedBytes;
    }

    public long getDiskTotalBytes() {
        return diskTotalBytes;
    }

    public void setDiskTotalBytes(long diskTotalBytes) {
        this.diskTotalBytes = diskTotalBytes;
    }

    public int getActiveConnections() {
        return activeConnections;
    }

    public void setActiveConnections(int activeConnections) {
        this.activeConnections = activeConnections;
    }

    public int getActiveChannels() {
        return activeChannels;
    }

    public void setActiveChannels(int activeChannels) {
        this.activeChannels = activeChannels;
    }

    public int getTotalParticipants() {
        return totalParticipants;
    }

    public void setTotalParticipants(int totalParticipants) {
        this.totalParticipants = totalParticipants;
    }

    public long getErrorCount() {
        return errorCount;
    }

    public void setErrorCount(long errorCount) {
        this.errorCount = errorCount;
    }

    public double getErrorRate() {
        return errorRate;
    }

    public void setErrorRate(double errorRate) {
        this.errorRate = errorRate;
    }

    public Map<String, Object> getCustomMetrics() {
        return new HashMap<>(customMetrics);
    }

    public void setCustomMetrics(Map<String, Object> customMetrics) {
        this.customMetrics = new HashMap<>(customMetrics);
    }

    /**
     * Adds a custom metric.
     *
     * @param key The metric key
     * @param value The metric value
     */
    public void addCustomMetric(String key, Object value) {
        this.customMetrics.put(key, value);
    }

    /**
     * Calculates memory usage percentage.
     *
     * @return Memory usage as a percentage
     */
    public double getMemoryUsagePercent() {
        if (memoryTotalBytes == 0) return 0.0;
        return (double) memoryUsedBytes / memoryTotalBytes * 100.0;
    }

    /**
     * Calculates disk usage percentage.
     *
     * @return Disk usage as a percentage
     */
    public double getDiskUsagePercent() {
        if (diskTotalBytes == 0) return 0.0;
        return (double) diskUsedBytes / diskTotalBytes * 100.0;
    }

    @Override
    public String toString() {
        return "NodeMetrics{" +
                "nodeId='" + nodeId + '\'' +
                ", timestamp=" + timestamp +
                ", uptimeSeconds=" + uptimeSeconds +
                ", requestsPerSecond=" + requestsPerSecond +
                ", averageLatency=" + averageLatency + "ms" +
                ", cpuUsagePercent=" + cpuUsagePercent + "%" +
                ", memoryUsagePercent=" + getMemoryUsagePercent() + "%" +
                ", activeChannels=" + activeChannels +
                ", errorRate=" + errorRate +
                '}';
    }
}
