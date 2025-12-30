package io.aurigraph.v11.monitoring;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Network Monitoring Service - AV11-275
 * Provides real-time network health monitoring and peer status
 */
@ApplicationScoped
public class NetworkMonitoringService {

    @ConfigProperty(name = "aurigraph.network.max-peers", defaultValue = "1000")
    int maxPeers;

    private final Map<String, PeerStatus> activePeers = new ConcurrentHashMap<>();
    private final NetworkHealthMetrics healthMetrics = new NetworkHealthMetrics();

    /**
     * Get overall network health status
     */
    public Uni<NetworkHealth> getNetworkHealth() {
        return Uni.createFrom().item(() -> {
            NetworkHealth health = new NetworkHealth();
            health.status = calculateNetworkStatus();
            health.totalPeers = activePeers.size();
            health.healthyPeers = (int) activePeers.values().stream()
                    .filter(PeerStatus::isHealthy).count();
            health.syncedPeers = (int) activePeers.values().stream()
                    .filter(PeerStatus::isSynced).count();
            health.averageLatency = calculateAverageLatency();
            health.networkBandwidth = healthMetrics.totalBandwidth;
            health.packetLoss = healthMetrics.packetLossRate;
            health.lastUpdate = Instant.now();
            health.uptime = healthMetrics.uptime;
            health.alerts = healthMetrics.activeAlerts;
            return health;
        });
    }

    /**
     * Get detailed peer status list
     */
    public Uni<List<PeerStatus>> getPeerStatus() {
        return Uni.createFrom().item(() ->
                activePeers.values().stream()
                        .sorted(Comparator.comparing(PeerStatus::getLatency))
                        .collect(Collectors.toList())
        );
    }

    /**
     * Get peer map for visualization
     */
    public Uni<PeerMap> getPeerMap() {
        return Uni.createFrom().item(() -> {
            PeerMap map = new PeerMap();
            map.peers = activePeers.values().stream()
                    .map(this::toPeerNode)
                    .collect(Collectors.toList());
            map.connections = calculateConnections();
            map.geolocation = calculateGeoDistribution();
            return map;
        });
    }

    /**
     * Get network statistics
     */
    public Uni<NetworkStatistics> getNetworkStatistics() {
        return Uni.createFrom().item(() -> {
            NetworkStatistics stats = new NetworkStatistics();
            stats.totalTransactions = healthMetrics.totalTransactions;
            stats.transactionsPerSecond = healthMetrics.currentTPS;
            stats.totalBlocks = healthMetrics.totalBlocks;
            stats.blocksPerSecond = healthMetrics.currentBPS;
            stats.avgBlockTime = healthMetrics.avgBlockTime;
            stats.networkHashrate = healthMetrics.networkHashrate;
            stats.difficulty = healthMetrics.difficulty;
            stats.timestamp = Instant.now();
            return stats;
        });
    }

    /**
     * Get latency histogram for all peers
     */
    public Uni<LatencyHistogram> getLatencyHistogram() {
        return Uni.createFrom().item(() -> {
            LatencyHistogram histogram = new LatencyHistogram();
            Map<String, Long> distribution = new HashMap<>();

            activePeers.values().forEach(peer -> {
                String bucket = getLatencyBucket(peer.latency);
                distribution.merge(bucket, 1L, Long::sum);
            });

            histogram.distribution = distribution;
            histogram.p50 = calculatePercentile(50);
            histogram.p95 = calculatePercentile(95);
            histogram.p99 = calculatePercentile(99);
            histogram.min = activePeers.values().stream()
                    .mapToLong(PeerStatus::getLatency).min().orElse(0);
            histogram.max = activePeers.values().stream()
                    .mapToLong(PeerStatus::getLatency).max().orElse(0);
            return histogram;
        });
    }

    /**
     * Update peer status (called by network layer)
     */
    public void updatePeerStatus(String peerId, PeerStatus status) {
        activePeers.put(peerId, status);
        updateHealthMetrics();
    }

    /**
     * Remove disconnected peer
     */
    public void removePeer(String peerId) {
        activePeers.remove(peerId);
        updateHealthMetrics();
    }

    // Helper methods

    private String calculateNetworkStatus() {
        if (activePeers.size() < 3) return "CRITICAL";
        if (healthMetrics.packetLossRate > 0.1) return "DEGRADED";
        if (healthMetrics.averageLatency > 1000) return "SLOW";
        return "HEALTHY";
    }

    private double calculateAverageLatency() {
        return activePeers.values().stream()
                .mapToLong(PeerStatus::getLatency)
                .average()
                .orElse(0);
    }

    private PeerNode toPeerNode(PeerStatus peer) {
        PeerNode node = new PeerNode();
        node.id = peer.peerId;
        node.address = peer.address;
        node.latency = peer.latency;
        node.status = peer.isHealthy() ? "ACTIVE" : "INACTIVE";
        node.geolocation = peer.geolocation;
        node.version = peer.version;
        node.uptime = peer.uptime;
        return node;
    }

    private List<PeerConnection> calculateConnections() {
        // Calculate peer-to-peer connections for visualization
        return new ArrayList<>(); // Simplified for now
    }

    private Map<String, Integer> calculateGeoDistribution() {
        Map<String, Integer> distribution = new HashMap<>();
        activePeers.values().forEach(peer -> {
            distribution.merge(peer.geolocation.country, 1, Integer::sum);
        });
        return distribution;
    }

    private String getLatencyBucket(long latency) {
        if (latency < 10) return "0-10ms";
        if (latency < 50) return "10-50ms";
        if (latency < 100) return "50-100ms";
        if (latency < 500) return "100-500ms";
        return "500ms+";
    }

    private long calculatePercentile(int percentile) {
        List<Long> latencies = activePeers.values().stream()
                .map(PeerStatus::getLatency)
                .sorted()
                .collect(Collectors.toList());

        if (latencies.isEmpty()) return 0;

        int index = (int) Math.ceil((percentile / 100.0) * latencies.size()) - 1;
        return latencies.get(Math.max(0, index));
    }

    private void updateHealthMetrics() {
        healthMetrics.lastUpdate = Instant.now();
        healthMetrics.activePeerCount = activePeers.size();
        healthMetrics.averageLatency = calculateAverageLatency();

        // Check for alerts
        healthMetrics.activeAlerts.clear();
        if (activePeers.size() < 3) {
            healthMetrics.activeAlerts.add("LOW_PEER_COUNT");
        }
        if (healthMetrics.averageLatency > 1000) {
            healthMetrics.activeAlerts.add("HIGH_LATENCY");
        }
        if (healthMetrics.packetLossRate > 0.05) {
            healthMetrics.activeAlerts.add("PACKET_LOSS");
        }
    }

    // Data classes

    public static class NetworkHealth {
        public String status;
        public int totalPeers;
        public int healthyPeers;
        public int syncedPeers;
        public double averageLatency;
        public long networkBandwidth;
        public double packetLoss;
        public Instant lastUpdate;
        public long uptime;
        public List<String> alerts;
    }

    public static class PeerStatus {
        public String peerId;
        public String address;
        public long latency;
        public boolean healthy;
        public boolean synced;
        public Geolocation geolocation;
        public String version;
        public long uptime;
        public long bytesReceived;
        public long bytesSent;

        public boolean isHealthy() { return healthy; }
        public boolean isSynced() { return synced; }
        public long getLatency() { return latency; }
    }

    public static class Geolocation {
        public String country;
        public String city;
        public double latitude;
        public double longitude;
    }

    public static class PeerMap {
        public List<PeerNode> peers;
        public List<PeerConnection> connections;
        public Map<String, Integer> geolocation;
    }

    public static class PeerNode {
        public String id;
        public String address;
        public long latency;
        public String status;
        public Geolocation geolocation;
        public String version;
        public long uptime;
    }

    public static class PeerConnection {
        public String from;
        public String to;
        public long latency;
        public String quality;
    }

    public static class NetworkStatistics {
        public long totalTransactions;
        public double transactionsPerSecond;
        public long totalBlocks;
        public double blocksPerSecond;
        public double avgBlockTime;
        public long networkHashrate;
        public long difficulty;
        public Instant timestamp;
    }

    public static class LatencyHistogram {
        public Map<String, Long> distribution;
        public long p50;
        public long p95;
        public long p99;
        public long min;
        public long max;
    }

    private static class NetworkHealthMetrics {
        Instant lastUpdate = Instant.now();
        int activePeerCount = 0;
        double averageLatency = 0;
        long totalBandwidth = 0;
        double packetLossRate = 0;
        long uptime = System.currentTimeMillis();
        List<String> activeAlerts = new ArrayList<>();

        long totalTransactions = 0;
        double currentTPS = 0;
        long totalBlocks = 0;
        double currentBPS = 0;
        double avgBlockTime = 0;
        long networkHashrate = 0;
        long difficulty = 0;
    }
}
