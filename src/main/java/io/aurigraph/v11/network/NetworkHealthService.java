package io.aurigraph.v11.network;

import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Network Health Monitoring Service
 *
 * Provides comprehensive network health metrics including:
 * - Overall network status (HEALTHY, DEGRADED, UNHEALTHY)
 * - Connected peers tracking
 * - Synchronization status
 * - Network latency scoring
 * - Bandwidth utilization
 * - Packet loss monitoring
 *
 * Part of AV11-273, AV11-274, AV11-275 network monitoring implementation.
 *
 * @author Aurigraph V11 Backend Development Agent
 * @version 11.0.0
 */
@ApplicationScoped
public class NetworkHealthService {

    private static final Logger LOG = Logger.getLogger(NetworkHealthService.class);

    // Simulated network state (will be replaced with real P2P network integration)
    private final Random random = new Random();
    private long lastHealthCheck = System.currentTimeMillis();
    private int connectedPeersCount = 0;
    private List<PeerInfo> peers = new ArrayList<>();

    public NetworkHealthService() {
        initializePeers();
    }

    /**
     * Get current network health status
     */
    public NetworkHealth getNetworkHealth() {
        long now = System.currentTimeMillis();

        // Simulate realistic network metrics
        int connectedPeers = 12 + random.nextInt(8); // 12-20 peers
        double syncStatus = 98.5 + random.nextDouble() * 1.5; // 98.5-100%
        int latencyScore = 85 + random.nextInt(15); // 85-100
        double bandwidthUtilization = 45.0 + random.nextDouble() * 30.0; // 45-75%
        double packetLoss = random.nextDouble() * 0.5; // 0-0.5%

        // Determine health status based on metrics
        NetworkStatus status = determineHealthStatus(connectedPeers, syncStatus, latencyScore, packetLoss);

        this.lastHealthCheck = now;
        this.connectedPeersCount = connectedPeers;

        return new NetworkHealth(
            status,
            connectedPeers,
            syncStatus,
            latencyScore,
            bandwidthUtilization,
            packetLoss,
            now
        );
    }

    /**
     * Get detailed peer information
     */
    public PeerMap getPeerMap() {
        updatePeerMetrics();

        return new PeerMap(
            new ArrayList<>(peers),
            peers.size(),
            calculateAverageLatency(),
            System.currentTimeMillis()
        );
    }

    /**
     * Get live network metrics for real-time monitoring
     */
    public LiveNetworkMetrics getLiveNetworkMetrics() {
        int activeConnections = connectedPeersCount > 0 ? connectedPeersCount : (12 + random.nextInt(8));

        BandwidthMetrics bandwidth = new BandwidthMetrics(
            125.5 + random.nextDouble() * 50.0, // 125-175 Mbps inbound
            118.3 + random.nextDouble() * 45.0  // 118-163 Mbps outbound
        );

        int messageRate = 15000 + random.nextInt(10000); // 15k-25k msg/s
        int blockPropagation = 150 + random.nextInt(100); // 150-250ms
        int transactionPool = 5000 + random.nextInt(5000); // 5k-10k pending

        List<NetworkEvent> recentEvents = generateRecentEvents();

        return new LiveNetworkMetrics(
            activeConnections,
            bandwidth,
            messageRate,
            blockPropagation,
            transactionPool,
            recentEvents,
            System.currentTimeMillis()
        );
    }

    /**
     * Initialize peer list with geographically distributed nodes
     */
    private void initializePeers() {
        peers = new ArrayList<>();

        // North America peers
        peers.add(createPeer("peer-na-01", "52.12.45.123", 9000, "New York", "USA", 40.7128, -74.0060, 25, "EXCELLENT", "12.0.0"));
        peers.add(createPeer("peer-na-02", "34.56.78.90", 9000, "San Francisco", "USA", 37.7749, -122.4194, 35, "GOOD", "12.0.0"));
        peers.add(createPeer("peer-na-03", "13.24.56.78", 9000, "Toronto", "Canada", 43.6532, -79.3832, 28, "EXCELLENT", "12.0.0"));
        peers.add(createPeer("peer-na-04", "45.67.89.12", 9000, "Chicago", "USA", 41.8781, -87.6298, 32, "GOOD", "12.0.0"));

        // Europe peers
        peers.add(createPeer("peer-eu-01", "85.45.67.89", 9000, "London", "UK", 51.5074, -0.1278, 45, "GOOD", "12.0.0"));
        peers.add(createPeer("peer-eu-02", "92.34.56.78", 9000, "Frankfurt", "Germany", 50.1109, 8.6821, 40, "EXCELLENT", "12.0.0"));
        peers.add(createPeer("peer-eu-03", "88.12.34.56", 9000, "Amsterdam", "Netherlands", 52.3702, 4.8952, 42, "GOOD", "12.0.0"));
        peers.add(createPeer("peer-eu-04", "95.67.89.12", 9000, "Paris", "France", 48.8566, 2.3522, 48, "FAIR", "10.9.5"));
        peers.add(createPeer("peer-eu-05", "87.23.45.67", 9000, "Stockholm", "Sweden", 59.3293, 18.0686, 50, "FAIR", "12.0.0"));

        // Asia-Pacific peers
        peers.add(createPeer("peer-ap-01", "103.45.67.89", 9000, "Singapore", "Singapore", 1.3521, 103.8198, 120, "GOOD", "12.0.0"));
        peers.add(createPeer("peer-ap-02", "202.34.56.78", 9000, "Tokyo", "Japan", 35.6762, 139.6503, 135, "FAIR", "12.0.0"));
        peers.add(createPeer("peer-ap-03", "115.23.45.67", 9000, "Sydney", "Australia", -33.8688, 151.2093, 180, "FAIR", "10.9.5"));
        peers.add(createPeer("peer-ap-04", "119.12.34.56", 9000, "Seoul", "South Korea", 37.5665, 126.9780, 140, "GOOD", "12.0.0"));

        // South America peers
        peers.add(createPeer("peer-sa-01", "177.45.67.89", 9000, "São Paulo", "Brazil", -23.5505, -46.6333, 155, "FAIR", "12.0.0"));
        peers.add(createPeer("peer-sa-02", "190.34.56.78", 9000, "Buenos Aires", "Argentina", -34.6037, -58.3816, 160, "FAIR", "10.9.5"));

        // Africa/Middle East peers
        peers.add(createPeer("peer-me-01", "41.45.67.89", 9000, "Dubai", "UAE", 25.2048, 55.2708, 95, "GOOD", "12.0.0"));
        peers.add(createPeer("peer-af-01", "102.34.56.78", 9000, "Cape Town", "South Africa", -33.9249, 18.4241, 170, "FAIR", "10.9.5"));

        LOG.infof("Initialized %d geographically distributed peers", peers.size());
    }

    /**
     * Create a peer information object
     */
    private PeerInfo createPeer(String peerId, String ip, int port, String city, String country,
                                double lat, double lon, int latency, String quality, String version) {
        long uptime = System.currentTimeMillis() - (random.nextInt(86400) * 1000L); // Up to 24h uptime

        return new PeerInfo(
            peerId,
            ip,
            port,
            new Location(city, country, new Coordinates(lat, lon)),
            latency,
            quality,
            version,
            uptime
        );
    }

    /**
     * Update peer metrics with slight variations for realism
     */
    private void updatePeerMetrics() {
        for (PeerInfo peer : peers) {
            // Add small random variations to latency
            int variation = random.nextInt(11) - 5; // -5 to +5 ms
            int newLatency = Math.max(20, peer.latency() + variation);

            // Occasionally update connection quality based on latency
            String quality = determineConnectionQuality(newLatency);

            peers.set(peers.indexOf(peer),
                new PeerInfo(
                    peer.peerId(),
                    peer.ipAddress(),
                    peer.port(),
                    peer.location(),
                    newLatency,
                    quality,
                    peer.version(),
                    peer.uptime()
                )
            );
        }
    }

    /**
     * Determine health status based on metrics
     */
    private NetworkStatus determineHealthStatus(int connectedPeers, double syncStatus,
                                                int latencyScore, double packetLoss) {
        if (connectedPeers < 8 || syncStatus < 95.0 || latencyScore < 70 || packetLoss > 1.0) {
            return NetworkStatus.UNHEALTHY;
        } else if (connectedPeers < 12 || syncStatus < 98.0 || latencyScore < 85 || packetLoss > 0.5) {
            return NetworkStatus.DEGRADED;
        }
        return NetworkStatus.HEALTHY;
    }

    /**
     * Determine connection quality based on latency
     */
    private String determineConnectionQuality(int latency) {
        if (latency < 50) return "EXCELLENT";
        if (latency < 100) return "GOOD";
        if (latency < 150) return "FAIR";
        return "POOR";
    }

    /**
     * Calculate average latency across all peers
     */
    private double calculateAverageLatency() {
        if (peers.isEmpty()) return 0.0;
        return peers.stream()
            .mapToInt(PeerInfo::latency)
            .average()
            .orElse(0.0);
    }

    /**
     * Generate recent network events for live monitoring
     */
    private List<NetworkEvent> generateRecentEvents() {
        List<NetworkEvent> events = new ArrayList<>();
        long now = System.currentTimeMillis();

        // Generate 5-10 recent events
        int eventCount = 5 + random.nextInt(6);
        String[] eventTypes = {"PEER_CONNECTED", "PEER_DISCONNECTED", "BLOCK_RECEIVED", "SYNC_COMPLETED", "CONSENSUS_ACHIEVED"};

        for (int i = 0; i < eventCount; i++) {
            String type = eventTypes[random.nextInt(eventTypes.length)];
            String description = generateEventDescription(type);
            long timestamp = now - (i * 5000L); // Events every 5 seconds

            events.add(new NetworkEvent(type, description, timestamp));
        }

        return events;
    }

    /**
     * Generate event description based on type
     */
    private String generateEventDescription(String type) {
        return switch (type) {
            case "PEER_CONNECTED" -> "New peer connected from " + (peers.isEmpty() ? "unknown" : peers.get(random.nextInt(peers.size())).location().city());
            case "PEER_DISCONNECTED" -> "Peer disconnected: " + (peers.isEmpty() ? "unknown" : peers.get(random.nextInt(peers.size())).peerId());
            case "BLOCK_RECEIVED" -> "Block #" + (1450000 + random.nextInt(1000)) + " received and validated";
            case "SYNC_COMPLETED" -> "Blockchain sync completed at height " + (1450000 + random.nextInt(1000));
            case "CONSENSUS_ACHIEVED" -> "Consensus achieved for block in " + (150 + random.nextInt(100)) + "ms";
            default -> "Network event occurred";
        };
    }

    // ==================== DATA MODELS ====================

    /**
     * Network health status enumeration
     */
    public enum NetworkStatus {
        HEALTHY,
        DEGRADED,
        UNHEALTHY
    }

    /**
     * Network health information
     */
    public record NetworkHealth(
        NetworkStatus status,
        int connectedPeers,
        double syncStatus,
        int latencyScore,
        double bandwidthUtilization,
        double packetLoss,
        long lastHealthCheck
    ) {}

    /**
     * Peer map with all connected peers
     */
    public record PeerMap(
        List<PeerInfo> peers,
        int totalPeers,
        double averageLatency,
        long timestamp
    ) {}

    /**
     * Individual peer information
     */
    public record PeerInfo(
        String peerId,
        String ipAddress,
        int port,
        Location location,
        int latency,
        String connectionQuality,
        String version,
        long uptime
    ) {}

    /**
     * Geographic location information
     */
    public record Location(
        String city,
        String country,
        Coordinates coordinates
    ) {}

    /**
     * Geographic coordinates
     */
    public record Coordinates(
        double latitude,
        double longitude
    ) {}

    /**
     * Live network metrics for real-time monitoring
     */
    public record LiveNetworkMetrics(
        int activeConnections,
        BandwidthMetrics bandwidth,
        int messageRate,
        int blockPropagation,
        int transactionPool,
        List<NetworkEvent> networkEvents,
        long timestamp
    ) {}

    /**
     * Bandwidth metrics (inbound/outbound)
     */
    public record BandwidthMetrics(
        double inbound,
        double outbound
    ) {}

    /**
     * Network event information
     */
    public record NetworkEvent(
        String type,
        String description,
        long timestamp
    ) {}
}
