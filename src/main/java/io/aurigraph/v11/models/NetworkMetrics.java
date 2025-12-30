package io.aurigraph.v11.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Real-time network metrics for live monitoring
 * Used by /api/v11/live/network endpoint
 *
 * @author Aurigraph V11
 * @version 11.3.0
 */
public class NetworkMetrics {

    @JsonProperty("timestamp")
    private Instant timestamp;

    @JsonProperty("active_connections")
    private ConnectionMetrics activeConnections;

    @JsonProperty("bandwidth")
    private BandwidthMetrics bandwidth;

    @JsonProperty("message_rates")
    private MessageRateMetrics messageRates;

    @JsonProperty("network_events")
    private List<NetworkEvent> recentEvents;

    @JsonProperty("node_health")
    private Map<String, NodeHealthStatus> nodeHealth;

    @JsonProperty("network_status")
    private String networkStatus; // "healthy", "degraded", "critical"

    // Constructor
    public NetworkMetrics() {
        this.timestamp = Instant.now();
        this.networkStatus = "healthy";
    }

    // Getters and Setters
    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public ConnectionMetrics getActiveConnections() {
        return activeConnections;
    }

    public void setActiveConnections(ConnectionMetrics activeConnections) {
        this.activeConnections = activeConnections;
    }

    public BandwidthMetrics getBandwidth() {
        return bandwidth;
    }

    public void setBandwidth(BandwidthMetrics bandwidth) {
        this.bandwidth = bandwidth;
    }

    public MessageRateMetrics getMessageRates() {
        return messageRates;
    }

    public void setMessageRates(MessageRateMetrics messageRates) {
        this.messageRates = messageRates;
    }

    public List<NetworkEvent> getRecentEvents() {
        return recentEvents;
    }

    public void setRecentEvents(List<NetworkEvent> recentEvents) {
        this.recentEvents = recentEvents;
    }

    public Map<String, NodeHealthStatus> getNodeHealth() {
        return nodeHealth;
    }

    public void setNodeHealth(Map<String, NodeHealthStatus> nodeHealth) {
        this.nodeHealth = nodeHealth;
    }

    public String getNetworkStatus() {
        return networkStatus;
    }

    public void setNetworkStatus(String networkStatus) {
        this.networkStatus = networkStatus;
    }

    /**
     * Connection metrics
     */
    public static class ConnectionMetrics {
        @JsonProperty("total")
        private int total;

        @JsonProperty("peer_to_peer")
        private int peerToPeer;

        @JsonProperty("client_connections")
        private int clientConnections;

        @JsonProperty("validator_connections")
        private int validatorConnections;

        @JsonProperty("connection_quality")
        private double connectionQuality; // 0.0 to 1.0

        public ConnectionMetrics() {}

        public ConnectionMetrics(int total, int p2p, int clients, int validators, double quality) {
            this.total = total;
            this.peerToPeer = p2p;
            this.clientConnections = clients;
            this.validatorConnections = validators;
            this.connectionQuality = quality;
        }

        // Getters and setters
        public int getTotal() { return total; }
        public void setTotal(int total) { this.total = total; }

        public int getPeerToPeer() { return peerToPeer; }
        public void setPeerToPeer(int peerToPeer) { this.peerToPeer = peerToPeer; }

        public int getClientConnections() { return clientConnections; }
        public void setClientConnections(int clientConnections) { this.clientConnections = clientConnections; }

        public int getValidatorConnections() { return validatorConnections; }
        public void setValidatorConnections(int validatorConnections) { this.validatorConnections = validatorConnections; }

        public double getConnectionQuality() { return connectionQuality; }
        public void setConnectionQuality(double connectionQuality) { this.connectionQuality = connectionQuality; }
    }

    /**
     * Bandwidth metrics
     */
    public static class BandwidthMetrics {
        @JsonProperty("inbound_mbps")
        private double inboundMbps;

        @JsonProperty("outbound_mbps")
        private double outboundMbps;

        @JsonProperty("total_mbps")
        private double totalMbps;

        @JsonProperty("peak_mbps")
        private double peakMbps;

        @JsonProperty("utilization_percent")
        private double utilizationPercent;

        public BandwidthMetrics() {}

        public BandwidthMetrics(double inbound, double outbound, double peak) {
            this.inboundMbps = inbound;
            this.outboundMbps = outbound;
            this.totalMbps = inbound + outbound;
            this.peakMbps = peak;
            this.utilizationPercent = (totalMbps / peak) * 100.0;
        }

        // Getters and setters
        public double getInboundMbps() { return inboundMbps; }
        public void setInboundMbps(double inboundMbps) { this.inboundMbps = inboundMbps; }

        public double getOutboundMbps() { return outboundMbps; }
        public void setOutboundMbps(double outboundMbps) { this.outboundMbps = outboundMbps; }

        public double getTotalMbps() { return totalMbps; }
        public void setTotalMbps(double totalMbps) { this.totalMbps = totalMbps; }

        public double getPeakMbps() { return peakMbps; }
        public void setPeakMbps(double peakMbps) { this.peakMbps = peakMbps; }

        public double getUtilizationPercent() { return utilizationPercent; }
        public void setUtilizationPercent(double utilizationPercent) { this.utilizationPercent = utilizationPercent; }
    }

    /**
     * Message rate metrics
     */
    public static class MessageRateMetrics {
        @JsonProperty("messages_per_second")
        private double messagesPerSecond;

        @JsonProperty("transactions_per_second")
        private double transactionsPerSecond;

        @JsonProperty("blocks_per_minute")
        private double blocksPerMinute;

        @JsonProperty("average_latency_ms")
        private double averageLatencyMs;

        @JsonProperty("p95_latency_ms")
        private double p95LatencyMs;

        public MessageRateMetrics() {}

        public MessageRateMetrics(double mps, double tps, double bpm, double avgLatency, double p95Latency) {
            this.messagesPerSecond = mps;
            this.transactionsPerSecond = tps;
            this.blocksPerMinute = bpm;
            this.averageLatencyMs = avgLatency;
            this.p95LatencyMs = p95Latency;
        }

        // Getters and setters
        public double getMessagesPerSecond() { return messagesPerSecond; }
        public void setMessagesPerSecond(double messagesPerSecond) { this.messagesPerSecond = messagesPerSecond; }

        public double getTransactionsPerSecond() { return transactionsPerSecond; }
        public void setTransactionsPerSecond(double transactionsPerSecond) { this.transactionsPerSecond = transactionsPerSecond; }

        public double getBlocksPerMinute() { return blocksPerMinute; }
        public void setBlocksPerMinute(double blocksPerMinute) { this.blocksPerMinute = blocksPerMinute; }

        public double getAverageLatencyMs() { return averageLatencyMs; }
        public void setAverageLatencyMs(double averageLatencyMs) { this.averageLatencyMs = averageLatencyMs; }

        public double getP95LatencyMs() { return p95LatencyMs; }
        public void setP95LatencyMs(double p95LatencyMs) { this.p95LatencyMs = p95LatencyMs; }
    }

    /**
     * Network event
     */
    public static class NetworkEvent {
        @JsonProperty("timestamp")
        private Instant timestamp;

        @JsonProperty("event_type")
        private String eventType; // "node_joined", "node_left", "consensus_update", "high_latency", etc.

        @JsonProperty("severity")
        private String severity; // "info", "warning", "error", "critical"

        @JsonProperty("message")
        private String message;

        @JsonProperty("node_id")
        private String nodeId;

        public NetworkEvent() {
            this.timestamp = Instant.now();
        }

        public NetworkEvent(String eventType, String severity, String message, String nodeId) {
            this();
            this.eventType = eventType;
            this.severity = severity;
            this.message = message;
            this.nodeId = nodeId;
        }

        // Getters and setters
        public Instant getTimestamp() { return timestamp; }
        public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }

        public String getEventType() { return eventType; }
        public void setEventType(String eventType) { this.eventType = eventType; }

        public String getSeverity() { return severity; }
        public void setSeverity(String severity) { this.severity = severity; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public String getNodeId() { return nodeId; }
        public void setNodeId(String nodeId) { this.nodeId = nodeId; }
    }

    /**
     * Node health status
     */
    public static class NodeHealthStatus {
        @JsonProperty("node_id")
        private String nodeId;

        @JsonProperty("status")
        private String status; // "online", "degraded", "offline"

        @JsonProperty("uptime_seconds")
        private long uptimeSeconds;

        @JsonProperty("last_seen")
        private Instant lastSeen;

        @JsonProperty("cpu_usage_percent")
        private double cpuUsagePercent;

        @JsonProperty("memory_usage_percent")
        private double memoryUsagePercent;

        public NodeHealthStatus() {}

        public NodeHealthStatus(String nodeId, String status, long uptime, double cpu, double memory) {
            this.nodeId = nodeId;
            this.status = status;
            this.uptimeSeconds = uptime;
            this.lastSeen = Instant.now();
            this.cpuUsagePercent = cpu;
            this.memoryUsagePercent = memory;
        }

        // Getters and setters
        public String getNodeId() { return nodeId; }
        public void setNodeId(String nodeId) { this.nodeId = nodeId; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public long getUptimeSeconds() { return uptimeSeconds; }
        public void setUptimeSeconds(long uptimeSeconds) { this.uptimeSeconds = uptimeSeconds; }

        public Instant getLastSeen() { return lastSeen; }
        public void setLastSeen(Instant lastSeen) { this.lastSeen = lastSeen; }

        public double getCpuUsagePercent() { return cpuUsagePercent; }
        public void setCpuUsagePercent(double cpuUsagePercent) { this.cpuUsagePercent = cpuUsagePercent; }

        public double getMemoryUsagePercent() { return memoryUsagePercent; }
        public void setMemoryUsagePercent(double memoryUsagePercent) { this.memoryUsagePercent = memoryUsagePercent; }
    }
}
