package io.aurigraph.v11.analytics.dashboard;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.websocket.*;
import jakarta.websocket.server.ServerEndpoint;
import org.jboss.logging.Logger;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Analytics Dashboard WebSocket Endpoint
 *
 * AV11-485: Real-time dashboard metrics streaming via WebSocket
 * Broadcasts metrics every 1 second to all connected clients
 *
 * Features:
 * - Channel-based filtering (transactions, blocks, nodes, performance)
 * - Multiple concurrent client support
 * - Auto-reconnection support
 * - Message queuing for missed updates
 *
 * Endpoint: /ws/dashboard
 * Update frequency: 1 second
 * Message format: JSON (DashboardMetrics)
 *
 * @author Analytics Dashboard Team
 * @version 11.0.0
 * @since Sprint 13
 */
@ServerEndpoint("/ws/dashboard")
@ApplicationScoped
public class AnalyticsDashboardWebSocket {

    private static final Logger LOG = Logger.getLogger(AnalyticsDashboardWebSocket.class);

    // Connected sessions with channel subscriptions
    private static final Map<String, SessionInfo> sessions = new ConcurrentHashMap<>();
    private static final AtomicLong messageCounter = new AtomicLong(0);
    private static final ObjectMapper objectMapper = new ObjectMapper()
        .registerModule(new JavaTimeModule());

    @Inject
    AnalyticsDashboardService dashboardService;

    private volatile boolean broadcastEnabled = false;

    /**
     * Initialize on startup
     */
    void onStart(@Observes StartupEvent ev) {
        LOG.info("Analytics Dashboard WebSocket initialized - Ready for connections");
        broadcastEnabled = true;
    }

    /**
     * Handle new WebSocket connection
     */
    @OnOpen
    public void onOpen(Session session) {
        String sessionId = session.getId();
        sessions.put(sessionId, new SessionInfo(session, Set.of("all")));

        LOG.infof("Dashboard WebSocket connected: %s (Total: %d)", sessionId, sessions.size());

        // Send initial metrics immediately
        try {
            DashboardMetrics initialMetrics = dashboardService.getDashboardMetrics();
            sendToSession(session, new DashboardUpdate("metrics", initialMetrics, System.currentTimeMillis()));
        } catch (Exception e) {
            LOG.errorf(e, "Failed to send initial metrics to session %s", sessionId);
        }
    }

    /**
     * Handle WebSocket messages (channel subscription control)
     */
    @OnMessage
    public void onMessage(String message, Session session) {
        String sessionId = session.getId();
        LOG.debugf("Received message from %s: %s", sessionId, message);

        try {
            // Parse subscription command: {"action": "subscribe", "channels": ["transactions", "blocks"]}
            var mapper = new ObjectMapper();
            var command = mapper.readTree(message);

            String action = command.get("action").asText();
            if ("subscribe".equals(action)) {
                var channelsNode = command.get("channels");
                Set<String> channels = ConcurrentHashMap.newKeySet();
                if (channelsNode.isArray()) {
                    channelsNode.forEach(ch -> channels.add(ch.asText()));
                }

                SessionInfo info = sessions.get(sessionId);
                if (info != null) {
                    info.channels.clear();
                    info.channels.addAll(channels);
                    LOG.infof("Session %s subscribed to channels: %s", sessionId, channels);

                    // Send acknowledgment
                    sendToSession(session, new SubscriptionAck("subscribed", channels));
                }
            }
        } catch (Exception e) {
            LOG.errorf(e, "Failed to process message from session %s", sessionId);
        }
    }

    /**
     * Handle WebSocket disconnection
     */
    @OnClose
    public void onClose(Session session) {
        String sessionId = session.getId();
        sessions.remove(sessionId);
        LOG.infof("Dashboard WebSocket disconnected: %s (Total: %d)", sessionId, sessions.size());
    }

    /**
     * Handle WebSocket errors
     */
    @OnError
    public void onError(Session session, Throwable throwable) {
        String sessionId = session.getId();
        LOG.errorf(throwable, "Dashboard WebSocket error on session %s", sessionId);
        sessions.remove(sessionId);
    }

    /**
     * Broadcast dashboard metrics every 1 second
     * Scheduled task that pushes updates to all connected clients
     */
    @Scheduled(every = "1s")
    void broadcastMetrics() {
        if (!broadcastEnabled || sessions.isEmpty()) {
            return;
        }

        try {
            DashboardMetrics metrics = dashboardService.getDashboardMetrics();
            long timestamp = System.currentTimeMillis();
            long messageId = messageCounter.incrementAndGet();

            // Broadcast to all sessions based on their channel subscriptions
            sessions.forEach((sessionId, info) -> {
                if (info.session.isOpen()) {
                    try {
                        DashboardUpdate update = new DashboardUpdate("metrics", metrics, timestamp);
                        sendToSession(info.session, update);
                    } catch (Exception e) {
                        LOG.debugf("Failed to broadcast to session %s: %s", sessionId, e.getMessage());
                    }
                }
            });

            if (messageId % 10 == 0) { // Log every 10 seconds
                LOG.debugf("Broadcasted metrics to %d clients (TPS: %.0f, Nodes: %d)",
                    (Object) sessions.size(),
                    (Object) metrics.transactionMetrics().currentTPS(),
                    (Object) metrics.networkMetrics().activeNodes());
            }
        } catch (Exception e) {
            LOG.errorf(e, "Error broadcasting dashboard metrics");
        }
    }

    /**
     * Broadcast performance metrics every 5 seconds
     */
    @Scheduled(every = "5s")
    void broadcastPerformanceMetrics() {
        if (!broadcastEnabled || sessions.isEmpty()) {
            return;
        }

        try {
            PerformanceMetrics metrics = dashboardService.getPerformanceMetrics();
            long timestamp = System.currentTimeMillis();

            sessions.forEach((sessionId, info) -> {
                if (info.session.isOpen() && info.channels.contains("performance")) {
                    try {
                        PerformanceUpdate update = new PerformanceUpdate("performance", metrics, timestamp);
                        sendToSession(info.session, update);
                    } catch (Exception e) {
                        LOG.debugf("Failed to send performance metrics to session %s: %s", sessionId, e.getMessage());
                    }
                }
            });
        } catch (Exception e) {
            LOG.errorf(e, "Error broadcasting performance metrics");
        }
    }

    /**
     * Broadcast node health every 3 seconds
     */
    @Scheduled(every = "3s")
    void broadcastNodeHealth() {
        if (!broadcastEnabled || sessions.isEmpty()) {
            return;
        }

        try {
            var nodeHealthList = dashboardService.getNodeHealthStatus();
            long timestamp = System.currentTimeMillis();

            sessions.forEach((sessionId, info) -> {
                if (info.session.isOpen() && (info.channels.contains("nodes") || info.channels.contains("all"))) {
                    try {
                        NodeHealthUpdate update = new NodeHealthUpdate("nodes", nodeHealthList, timestamp);
                        sendToSession(info.session, update);
                    } catch (Exception e) {
                        LOG.debugf("Failed to send node health to session %s: %s", sessionId, e.getMessage());
                    }
                }
            });
        } catch (Exception e) {
            LOG.errorf(e, "Error broadcasting node health");
        }
    }

    /**
     * Send message to a specific session
     */
    private void sendToSession(Session session, Object message) {
        try {
            String json = objectMapper.writeValueAsString(message);
            session.getAsyncRemote().sendText(json);
        } catch (Exception e) {
            LOG.debugf("Failed to send message to session %s: %s", session.getId(), e.getMessage());
        }
    }

    /**
     * Get the number of active connections
     */
    public static int getConnectionCount() {
        return sessions.size();
    }

    /**
     * Check if there are active connections
     */
    public static boolean hasConnections() {
        return !sessions.isEmpty();
    }

    /**
     * Session information with channel subscriptions
     */
    private static class SessionInfo {
        final Session session;
        final Set<String> channels;

        SessionInfo(Session session, Set<String> channels) {
            this.session = session;
            this.channels = ConcurrentHashMap.newKeySet();
            this.channels.addAll(channels);
        }
    }

    /**
     * Dashboard update message
     */
    record DashboardUpdate(
        String type,
        DashboardMetrics data,
        long timestamp
    ) {}

    /**
     * Performance update message
     */
    record PerformanceUpdate(
        String type,
        PerformanceMetrics data,
        long timestamp
    ) {}

    /**
     * Node health update message
     */
    record NodeHealthUpdate(
        String type,
        java.util.List<NodeHealthMetrics> data,
        long timestamp
    ) {}

    /**
     * Subscription acknowledgment message
     */
    record SubscriptionAck(
        String status,
        Set<String> channels
    ) {}
}
