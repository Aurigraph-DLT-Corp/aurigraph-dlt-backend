package io.aurigraph.v11.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.aurigraph.v11.TransactionService;
import io.aurigraph.v11.websocket.dto.*;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;

/**
 * WebSocket broadcaster service
 * Broadcasts real-time data to connected WebSocket clients
 *
 * Features:
 * - Scheduled broadcasts (metrics every 1 second)
 * - Event-driven broadcasts (transactions, validators, consensus, network)
 * - Message compression
 * - Connection management
 * - Latency tracking (<100ms target)
 */
@ApplicationScoped
public class WebSocketBroadcaster {

    private static final Logger LOG = Logger.getLogger(WebSocketBroadcaster.class);

    @Inject
    TransactionService transactionService;

    @Inject
    ObjectMapper objectMapper;

    private final OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
    private final AtomicLong messagesSent = new AtomicLong(0);
    private final AtomicLong broadcastLatencyMs = new AtomicLong(0);

    void onStart(@Observes StartupEvent ev) {
        LOG.info("WebSocket Broadcaster initialized - Ready for real-time streaming");
    }

    /**
     * Broadcast metrics data every 1 second
     * Target latency: <100ms
     */
    @Scheduled(every = "1s", identity = "metrics-broadcast")
    void broadcastMetrics() {
        if (!MetricsWebSocket.hasConnections()) {
            return; // Skip if no clients connected
        }

        long startTime = System.currentTimeMillis();

        try {
            // Gather current metrics from TransactionService
            TransactionService.EnhancedProcessingStats stats = transactionService.getStats();

            // Get system metrics
            double cpuUsage = osBean.getSystemLoadAverage() / Runtime.getRuntime().availableProcessors() * 100;
            long memoryUsed = stats.memoryUsed() / (1024 * 1024); // Convert to MB
            int connections = MetricsWebSocket.getConnectionCount() +
                            TransactionWebSocket.getConnectionCount() +
                            ValidatorWebSocket.getConnectionCount() +
                            ConsensusWebSocket.getConnectionCount() +
                            NetworkWebSocket.getConnectionCount();

            // Calculate current TPS (use ultra-high-throughput if available)
            long currentTps = (long) stats.currentThroughputMeasurement();

            // Create metrics message
            MetricsMessage message = new MetricsMessage(
                Instant.now(),
                currentTps,
                cpuUsage,
                memoryUsed,
                connections,
                0.001 // Error rate (mock for now)
            );

            // Serialize to JSON and broadcast
            String json = objectMapper.writeValueAsString(message);
            MetricsWebSocket.broadcast(json);

            long latency = System.currentTimeMillis() - startTime;
            broadcastLatencyMs.set(latency);
            messagesSent.incrementAndGet();

            if (latency > 100) {
                LOG.warnf("Metrics broadcast latency exceeded 100ms: %dms", latency);
            }

        } catch (Exception e) {
            LOG.errorf(e, "Error broadcasting metrics");
        }
    }

    /**
     * Broadcast transaction event
     * Called when new transaction is processed
     */
    public void broadcastTransaction(String txHash, String from, String to, String value, String status, long gasUsed) {
        if (!TransactionWebSocket.hasConnections()) {
            return;
        }

        long startTime = System.currentTimeMillis();

        try {
            TransactionMessage message = new TransactionMessage(
                Instant.now(),
                txHash,
                from,
                to,
                value,
                status,
                gasUsed
            );

            String json = objectMapper.writeValueAsString(message);
            TransactionWebSocket.broadcast(json);

            long latency = System.currentTimeMillis() - startTime;
            messagesSent.incrementAndGet();

            if (latency > 100) {
                LOG.warnf("Transaction broadcast latency exceeded 100ms: %dms", latency);
            }

        } catch (Exception e) {
            LOG.errorf(e, "Error broadcasting transaction");
        }
    }

    /**
     * Broadcast validator status change
     */
    public void broadcastValidatorStatus(String validator, String status, long votingPower, double uptime, long lastBlockProposed) {
        if (!ValidatorWebSocket.hasConnections()) {
            return;
        }

        long startTime = System.currentTimeMillis();

        try {
            ValidatorMessage message = new ValidatorMessage(
                Instant.now(),
                validator,
                status,
                votingPower,
                uptime,
                lastBlockProposed
            );

            String json = objectMapper.writeValueAsString(message);
            ValidatorWebSocket.broadcast(json);

            long latency = System.currentTimeMillis() - startTime;
            messagesSent.incrementAndGet();

            if (latency > 100) {
                LOG.warnf("Validator broadcast latency exceeded 100ms: %dms", latency);
            }

        } catch (Exception e) {
            LOG.errorf(e, "Error broadcasting validator status");
        }
    }

    /**
     * Broadcast consensus state change
     */
    public void broadcastConsensusState(String leader, long epoch, long round, long term, String state, double performanceScore, int activeValidators) {
        if (!ConsensusWebSocket.hasConnections()) {
            return;
        }

        long startTime = System.currentTimeMillis();

        try {
            ConsensusMessage message = new ConsensusMessage(
                Instant.now(),
                leader,
                epoch,
                round,
                term,
                state,
                performanceScore,
                activeValidators
            );

            String json = objectMapper.writeValueAsString(message);
            ConsensusWebSocket.broadcast(json);

            long latency = System.currentTimeMillis() - startTime;
            messagesSent.incrementAndGet();

            if (latency > 100) {
                LOG.warnf("Consensus broadcast latency exceeded 100ms: %dms", latency);
            }

        } catch (Exception e) {
            LOG.errorf(e, "Error broadcasting consensus state");
        }
    }

    /**
     * Broadcast network topology change
     */
    public void broadcastNetworkTopology(String peerId, String ip, boolean connected, int latency, String version) {
        if (!NetworkWebSocket.hasConnections()) {
            return;
        }

        long startTime = System.currentTimeMillis();

        try {
            NetworkMessage message = new NetworkMessage(
                Instant.now(),
                peerId,
                ip,
                connected,
                latency,
                version
            );

            String json = objectMapper.writeValueAsString(message);
            NetworkWebSocket.broadcast(json);

            long broadcastLatency = System.currentTimeMillis() - startTime;
            messagesSent.incrementAndGet();

            if (broadcastLatency > 100) {
                LOG.warnf("Network broadcast latency exceeded 100ms: %dms", broadcastLatency);
            }

        } catch (Exception e) {
            LOG.errorf(e, "Error broadcasting network topology");
        }
    }

    /**
     * Get total messages sent across all WebSocket streams
     */
    public long getMessagesSent() {
        return messagesSent.get();
    }

    /**
     * Get last broadcast latency in milliseconds
     */
    public long getLastBroadcastLatency() {
        return broadcastLatencyMs.get();
    }

    /**
     * Get total active connections across all WebSocket streams
     */
    public int getTotalConnections() {
        return MetricsWebSocket.getConnectionCount() +
               TransactionWebSocket.getConnectionCount() +
               ValidatorWebSocket.getConnectionCount() +
               ConsensusWebSocket.getConnectionCount() +
               NetworkWebSocket.getConnectionCount();
    }
}
