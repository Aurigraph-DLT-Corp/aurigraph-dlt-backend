package io.aurigraph.v11.live;

import io.aurigraph.v11.models.NetworkMetrics;
import io.aurigraph.v11.models.NetworkMetrics.*;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Live Network Monitoring Service
 * Provides real-time network metrics and health status
 *
 * @author Aurigraph V11
 * @version 11.3.0
 */
@ApplicationScoped
public class LiveNetworkService {

    private static final Logger LOG = Logger.getLogger(LiveNetworkService.class);

    // Simulated network state (in production, this would come from actual network monitoring)
    private final Map<String, NodeHealthStatus> nodeHealthMap = new ConcurrentHashMap<>();
    private final List<NetworkEvent> eventHistory = Collections.synchronizedList(new ArrayList<>());
    private final AtomicLong messageCounter = new AtomicLong(0);
    private final AtomicLong transactionCounter = new AtomicLong(0);
    private final Instant startTime = Instant.now();

    // Configuration
    private static final int MAX_EVENTS = 100;
    private static final int SIMULATED_NODE_COUNT = 7; // HyperRAFT++ cluster

    public LiveNetworkService() {
        initializeSimulatedNodes();
        LOG.infof("LiveNetworkService initialized with %d simulated nodes", SIMULATED_NODE_COUNT);
    }

    /**
     * Get current network metrics
     */
    public Uni<NetworkMetrics> getNetworkMetrics() {
        return Uni.createFrom().item(() -> {
            NetworkMetrics metrics = new NetworkMetrics();
            metrics.setTimestamp(Instant.now());

            // Active connections
            metrics.setActiveConnections(calculateConnectionMetrics());

            // Bandwidth metrics
            metrics.setBandwidth(calculateBandwidthMetrics());

            // Message rate metrics
            metrics.setMessageRates(calculateMessageRateMetrics());

            // Recent network events
            metrics.setRecentEvents(getRecentEvents(10));

            // Node health status
            metrics.setNodeHealth(new HashMap<>(nodeHealthMap));

            // Overall network status
            metrics.setNetworkStatus(calculateNetworkStatus());

            LOG.debugf("Generated network metrics: %s connections, %s TPS",
                    String.valueOf(metrics.getActiveConnections().getTotal()),
                    String.format("%.2f", metrics.getMessageRates().getTransactionsPerSecond()));

            return metrics;
        });
    }

    /**
     * Calculate connection metrics
     */
    private ConnectionMetrics calculateConnectionMetrics() {
        int total = nodeHealthMap.size() * 3; // Simulated: each node has ~3 connections
        int p2p = nodeHealthMap.size();
        int clients = total - p2p - (nodeHealthMap.size() / 2);
        int validators = nodeHealthMap.size() / 2;

        // Connection quality based on node health
        long healthyNodes = nodeHealthMap.values().stream()
                .filter(n -> "online".equals(n.getStatus()))
                .count();
        double quality = nodeHealthMap.isEmpty() ? 0.0 : (double) healthyNodes / nodeHealthMap.size();

        return new ConnectionMetrics(total, p2p, clients, validators, quality);
    }

    /**
     * Calculate bandwidth metrics
     */
    private BandwidthMetrics calculateBandwidthMetrics() {
        // Simulated bandwidth based on message rates
        double baseInbound = 50.0 + (Math.random() * 20.0); // 50-70 Mbps
        double baseOutbound = 40.0 + (Math.random() * 15.0); // 40-55 Mbps
        double peak = 150.0;

        return new BandwidthMetrics(baseInbound, baseOutbound, peak);
    }

    /**
     * Calculate message rate metrics
     */
    private MessageRateMetrics calculateMessageRateMetrics() {
        // Simulated rates (in production, calculated from actual metrics)
        long uptimeSeconds = java.time.Duration.between(startTime, Instant.now()).getSeconds();
        double mps = 1500.0 + (Math.random() * 500.0); // 1500-2000 msg/sec
        double tps = 850.0 + (Math.random() * 150.0); // 850-1000 TPS
        double bpm = 12.0 + (Math.random() * 2.0); // 12-14 blocks/min

        // Latency simulation
        double avgLatency = 25.0 + (Math.random() * 15.0); // 25-40ms
        double p95Latency = 45.0 + (Math.random() * 25.0); // 45-70ms

        return new MessageRateMetrics(mps, tps, bpm, avgLatency, p95Latency);
    }

    /**
     * Get recent network events
     */
    private List<NetworkEvent> getRecentEvents(int limit) {
        synchronized (eventHistory) {
            int size = eventHistory.size();
            int start = Math.max(0, size - limit);
            return new ArrayList<>(eventHistory.subList(start, size));
        }
    }

    /**
     * Calculate overall network status
     */
    private String calculateNetworkStatus() {
        long onlineNodes = nodeHealthMap.values().stream()
                .filter(n -> "online".equals(n.getStatus()))
                .count();

        long totalNodes = nodeHealthMap.size();

        if (totalNodes == 0) {
            return "unknown";
        }

        double healthyPercent = (double) onlineNodes / totalNodes;

        if (healthyPercent >= 0.9) {
            return "healthy";
        } else if (healthyPercent >= 0.7) {
            return "degraded";
        } else {
            return "critical";
        }
    }

    /**
     * Initialize simulated nodes for demonstration
     */
    private void initializeSimulatedNodes() {
        String[] nodeTypes = {"validator", "channel", "business", "api_integration"};

        for (int i = 0; i < SIMULATED_NODE_COUNT; i++) {
            String nodeId = String.format("node-%s-%d",
                    nodeTypes[i % nodeTypes.length], i + 1);

            String status = (Math.random() > 0.1) ? "online" : "degraded";
            long uptime = (long) (Math.random() * 86400 * 7); // 0-7 days
            double cpu = 20.0 + (Math.random() * 50.0); // 20-70%
            double memory = 30.0 + (Math.random() * 40.0); // 30-70%

            nodeHealthMap.put(nodeId, new NodeHealthStatus(nodeId, status, uptime, cpu, memory));
        }

        // Add initial network events
        addNetworkEvent("system_start", "info", "Network monitoring initialized", "system");
        addNetworkEvent("cluster_ready", "info",
                String.format("%d nodes in HyperRAFT++ cluster", SIMULATED_NODE_COUNT), "system");
    }

    /**
     * Add a network event
     */
    public void addNetworkEvent(String eventType, String severity, String message, String nodeId) {
        synchronized (eventHistory) {
            NetworkEvent event = new NetworkEvent(eventType, severity, message, nodeId);
            eventHistory.add(event);

            // Keep only recent events
            while (eventHistory.size() > MAX_EVENTS) {
                eventHistory.remove(0);
            }

            LOG.debugf("Network event added: %s - %s", eventType, message);
        }
    }

    /**
     * Update node health status
     */
    public void updateNodeHealth(String nodeId, String status, double cpuUsage, double memoryUsage) {
        NodeHealthStatus health = nodeHealthMap.get(nodeId);
        if (health != null) {
            health.setStatus(status);
            health.setCpuUsagePercent(cpuUsage);
            health.setMemoryUsagePercent(memoryUsage);
            health.setLastSeen(Instant.now());

            LOG.debugf("Node health updated: %s - %s", nodeId, status);

            // Generate event if status changed
            if (!status.equals("online")) {
                addNetworkEvent("node_status_change", "warning",
                        String.format("Node %s status: %s", nodeId, status), nodeId);
            }
        }
    }

    /**
     * Increment message counter (called from actual message processing)
     */
    public void incrementMessageCount() {
        messageCounter.incrementAndGet();
    }

    /**
     * Increment transaction counter (called from actual transaction processing)
     */
    public void incrementTransactionCount() {
        transactionCounter.incrementAndGet();
    }

    /**
     * Get network uptime
     */
    public long getUptimeSeconds() {
        return java.time.Duration.between(startTime, Instant.now()).getSeconds();
    }
}
