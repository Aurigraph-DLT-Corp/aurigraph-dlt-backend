package io.aurigraph.v11.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.runtime.ShutdownEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.websocket.*;
import jakarta.websocket.server.ServerEndpoint;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Enhanced Transaction WebSocket with Authentication
 *
 * Features:
 * - JWT authentication via AuthenticatedWebSocketConfigurator
 * - Channel subscription management (SUBSCRIBE/UNSUBSCRIBE commands)
 * - Heartbeat mechanism (30-second intervals)
 * - Message queuing for offline/slow clients
 * - Real-time transaction updates
 *
 * Endpoint: /ws/transactions
 *
 * Authentication:
 * - Query parameter: /ws/transactions?token=<jwt>
 * - Authorization header: Authorization: Bearer <jwt>
 *
 * Commands:
 * - SUBSCRIBE <channel>: Subscribe to a channel
 * - UNSUBSCRIBE <channel>: Unsubscribe from a channel
 * - PING: Heartbeat ping
 *
 * Responses:
 * - PONG: Heartbeat response
 * - SUBSCRIBED <channel>: Subscription confirmation
 * - UNSUBSCRIBED <channel>: Unsubscription confirmation
 * - ERROR <message>: Error message
 *
 * @author WebSocket Development Agent (WDA)
 * @since V11.6.0 (Sprint 16 - AV11-484)
 */
@ServerEndpoint(
        value = "/ws/transactions",
        configurator = AuthenticatedWebSocketConfigurator.class
)
@ApplicationScoped
public class EnhancedTransactionWebSocket {

    private static final Logger LOG = Logger.getLogger(EnhancedTransactionWebSocket.class);
    private static final int HEARTBEAT_INTERVAL_SECONDS = 30;

    // SECURITY FIX: Managed executor service with proper shutdown
    private static final ScheduledExecutorService heartbeatExecutor = Executors.newScheduledThreadPool(4, r -> {
        Thread t = new Thread(r);
        t.setName("WebSocket-Heartbeat-" + t.getId());
        t.setDaemon(true);
        return t;
    });

    @Inject
    WebSocketSessionManager sessionManager;

    @Inject
    WebSocketSubscriptionService subscriptionService;

    @Inject
    WebSocketAuthService authService;

    @Inject
    MessageQueueService messageQueueService;

    @Inject
    ObjectMapper objectMapper;

    /**
     * Shutdown observer to cleanly stop executor service
     * SECURITY FIX: Prevents resource leak on application shutdown
     */
    void onShutdown(@Observes ShutdownEvent event) {
        LOG.info("Shutting down WebSocket heartbeat executor service");
        heartbeatExecutor.shutdown();
        try {
            if (!heartbeatExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                LOG.warn("Heartbeat executor did not terminate gracefully, forcing shutdown");
                heartbeatExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            LOG.error("Interrupted while waiting for executor shutdown", e);
            heartbeatExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    @OnOpen
    public void onOpen(Session session, EndpointConfig config) {
        try {
            // SECURITY: Validate authentication IMMEDIATELY before any processing
            Object authenticatedObj = config.getUserProperties().get("authenticated");

            // Reject unauthenticated connections immediately
            if (authenticatedObj == null || !(boolean) authenticatedObj) {
                LOG.warnf("⚠️ SECURITY: Unauthenticated WebSocket connection blocked: %s", session.getId());
                session.close(new CloseReason(CloseReason.CloseCodes.VIOLATED_POLICY, "Authentication required"));
                return; // Exit immediately - no further processing
            }

            // Extract user ID only after authentication confirmed
            Object userIdObj = config.getUserProperties().get("userId");
            String userId = userIdObj != null ? userIdObj.toString() : null;

            // Register session with auth service for timeout tracking
            authService.registerSession(session.getId(), userId);

            // Create device fingerprint for security
            String clientIp = (String) config.getUserProperties().get("clientIp");
            String userAgent = (String) config.getUserProperties().get("userAgent");
            authService.createDeviceFingerprint(session.getId(), clientIp, userAgent, Map.of());

            // Register session with manager (user is authenticated if we reach here)
            WebSocketSession wsSession = sessionManager.registerSession(session, userId, true);

            // Send welcome message
            String welcome = String.format(
                    "{\"type\":\"WELCOME\",\"userId\":\"%s\",\"sessionId\":\"%s\",\"message\":\"Connected to Aurigraph transaction stream\"}",
                    userId, session.getId()
            );
            session.getAsyncRemote().sendText(welcome);

            // Load subscription preferences from database
            List<WebSocketSubscription> userSubscriptions = subscriptionService.getActiveSubscriptions(userId);

            if (userSubscriptions.isEmpty()) {
                // First-time user: create default subscriptions
                subscriptionService.subscribe(userId, "transactions", 5);
                subscriptionService.subscribe(userId, "system", 8);
                userSubscriptions = subscriptionService.getActiveSubscriptions(userId);
            }

            // Subscribe to all persisted channels
            for (WebSocketSubscription subscription : userSubscriptions) {
                sessionManager.subscribe(session.getId(), subscription.channel);
                session.getAsyncRemote().sendText("SUBSCRIBED " + subscription.channel);
            }

            // Deliver any queued messages
            int deliveredCount = sessionManager.deliverQueuedMessages(session.getId());
            if (deliveredCount > 0) {
                String queueInfo = String.format(
                        "{\"type\":\"INFO\",\"message\":\"Delivered %d queued messages\"}",
                        deliveredCount
                );
                session.getAsyncRemote().sendText(queueInfo);
            }

            // Start heartbeat timer
            ScheduledFuture<?> heartbeatTimer = heartbeatExecutor.scheduleAtFixedRate(
                    () -> sendHeartbeat(session, wsSession),
                    HEARTBEAT_INTERVAL_SECONDS,
                    HEARTBEAT_INTERVAL_SECONDS,
                    TimeUnit.SECONDS
            );
            wsSession.setHeartbeatTimer(heartbeatTimer);

            LOG.infof("✅ Enhanced WebSocket opened: session=%s, user=%s, authenticated=true",
                    session.getId(), userId);

        } catch (Exception e) {
            LOG.errorf(e, "❌ Error opening WebSocket connection: %s", session.getId());
            try {
                session.close(new CloseReason(CloseReason.CloseCodes.UNEXPECTED_CONDITION, "Server error"));
            } catch (Exception closeError) {
                LOG.errorf(closeError, "Error closing session after error");
            }
        }
    }

    @OnMessage
    public void onMessage(String message, Session session) {
        try {
            WebSocketSession wsSession = sessionManager.getSession(session.getId());

            if (wsSession == null) {
                LOG.warnf("⚠️ Message from unknown session: %s", session.getId());
                session.getAsyncRemote().sendText("ERROR: Session not found");
                return;
            }

            // Update heartbeat and session activity
            wsSession.updateHeartbeat();
            authService.updateActivity(session.getId());

            // Check for session timeout
            if (authService.isSessionTimedOut(session.getId())) {
                LOG.warnf("Session %s timed out, closing connection", session.getId());
                session.close(new CloseReason(CloseReason.CloseCodes.GOING_AWAY, "Session timeout"));
                return;
            }

            // Detect suspicious activity
            if (authService.detectSuspiciousActivity(session.getId(), wsSession.getUserId())) {
                LOG.warnf("Suspicious activity detected for session %s, closing connection", session.getId());
                session.close(new CloseReason(CloseReason.CloseCodes.VIOLATED_POLICY, "Suspicious activity"));
                return;
            }

            // Parse command
            String[] parts = message.trim().split("\\s+", 2);
            String command = parts[0].toUpperCase();

            switch (command) {
                case "PING":
                    handlePing(session, wsSession);
                    break;

                case "SUBSCRIBE":
                    if (parts.length < 2) {
                        session.getAsyncRemote().sendText("ERROR: SUBSCRIBE requires channel name");
                        return;
                    }
                    handleSubscribe(session, wsSession, parts[1]);
                    break;

                case "UNSUBSCRIBE":
                    if (parts.length < 2) {
                        session.getAsyncRemote().sendText("ERROR: UNSUBSCRIBE requires channel name");
                        return;
                    }
                    handleUnsubscribe(session, wsSession, parts[1]);
                    break;

                case "LIST":
                    handleListSubscriptions(session, wsSession);
                    break;

                case "STATS":
                    handleStats(session);
                    break;

                default:
                    String errorMsg = String.format("ERROR: Unknown command '%s'. Valid commands: PING, SUBSCRIBE, UNSUBSCRIBE, LIST, STATS", command);
                    session.getAsyncRemote().sendText(errorMsg);
                    LOG.warnf("⚠️ Unknown command from session %s: %s", session.getId(), command);
            }

        } catch (Exception e) {
            LOG.errorf(e, "❌ Error processing message from session %s", session.getId());
            try {
                session.getAsyncRemote().sendText("ERROR: " + e.getMessage());
            } catch (Exception sendError) {
                LOG.errorf(sendError, "Error sending error message");
            }
        }
    }

    @OnClose
    public void onClose(Session session, CloseReason closeReason) {
        try {
            WebSocketSession wsSession = sessionManager.getSession(session.getId());

            if (wsSession != null) {
                // Cancel heartbeat timer
                wsSession.cancelHeartbeatTimer();

                LOG.infof("📊 Session stats before close: user=%s, duration=%ds, channels=%d",
                        wsSession.getUserId(),
                        wsSession.getConnectionDurationSeconds(),
                        wsSession.getSubscribedChannels().size());
            }

            // Cleanup auth service tracking
            authService.cleanupSession(session.getId());

            // Cleanup rate limit tracking
            subscriptionService.cleanupRateLimitTracking(session.getId());

            // Cleanup message queue service
            messageQueueService.cleanupExpiredMessages();

            // Unregister session (cleans up all subscriptions)
            sessionManager.unregisterSession(session.getId());

            LOG.infof("✅ Enhanced WebSocket closed: session=%s, reason=%s",
                    session.getId(), closeReason.getReasonPhrase());

        } catch (Exception e) {
            LOG.errorf(e, "❌ Error closing WebSocket session: %s", session.getId());
        }
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        LOG.errorf(throwable, "❌ Enhanced WebSocket error on session %s", session.getId());

        try {
            sessionManager.unregisterSession(session.getId());
        } catch (Exception e) {
            LOG.errorf(e, "Error during error handling cleanup");
        }
    }

    /**
     * Handle PING command
     */
    private void handlePing(Session session, WebSocketSession wsSession) {
        session.getAsyncRemote().sendText("PONG");
        LOG.debugf("Heartbeat ping from session %s", session.getId());
    }

    /**
     * Handle SUBSCRIBE command
     */
    private void handleSubscribe(Session session, WebSocketSession wsSession, String channel) {
        String userId = wsSession.getUserId();

        // Persist subscription to database
        WebSocketSubscription subscription = subscriptionService.subscribe(userId, channel, 5);

        if (subscription != null) {
            // Add to session manager
            boolean success = sessionManager.subscribe(session.getId(), channel);

            if (success) {
                String response = String.format("SUBSCRIBED %s", channel);
                session.getAsyncRemote().sendText(response);
                LOG.infof("✅ Session %s subscribed to channel: %s (persisted)", session.getId(), channel);
            } else {
                String error = String.format("ERROR: Failed to subscribe to channel '%s'", channel);
                session.getAsyncRemote().sendText(error);
            }
        } else {
            String error = String.format("ERROR: Failed to create subscription for channel '%s' (limit reached?)", channel);
            session.getAsyncRemote().sendText(error);
        }
    }

    /**
     * Handle UNSUBSCRIBE command
     */
    private void handleUnsubscribe(Session session, WebSocketSession wsSession, String channel) {
        String userId = wsSession.getUserId();

        // Remove from session manager
        boolean sessionSuccess = sessionManager.unsubscribe(session.getId(), channel);

        // Remove from database (persistent unsubscribe)
        boolean dbSuccess = subscriptionService.unsubscribe(userId, channel);

        if (sessionSuccess || dbSuccess) {
            String response = String.format("UNSUBSCRIBED %s", channel);
            session.getAsyncRemote().sendText(response);
            LOG.infof("✅ Session %s unsubscribed from channel: %s (removed from DB)", session.getId(), channel);
        } else {
            String error = String.format("ERROR: Failed to unsubscribe from channel '%s'", channel);
            session.getAsyncRemote().sendText(error);
        }
    }

    /**
     * Handle LIST command - list all subscribed channels
     */
    private void handleListSubscriptions(Session session, WebSocketSession wsSession) {
        try {
            String channels = String.join(", ", wsSession.getSubscribedChannels());
            String response = String.format(
                    "{\"type\":\"SUBSCRIPTIONS\",\"channels\":[%s]}",
                    wsSession.getSubscribedChannels().stream()
                            .map(ch -> "\"" + ch + "\"")
                            .reduce((a, b) -> a + "," + b)
                            .orElse("")
            );
            session.getAsyncRemote().sendText(response);
        } catch (Exception e) {
            LOG.errorf(e, "Error listing subscriptions");
            session.getAsyncRemote().sendText("ERROR: Failed to list subscriptions");
        }
    }

    /**
     * Handle STATS command - return session statistics
     */
    private void handleStats(Session session) {
        try {
            WebSocketSessionManager.SessionStats stats = sessionManager.getStats();
            String response = String.format(
                    "{\"type\":\"STATS\",\"totalSessions\":%d,\"authenticatedSessions\":%d,\"uniqueUsers\":%d,\"activeChannels\":%d,\"queuedMessages\":%d}",
                    stats.totalSessions,
                    stats.authenticatedSessions,
                    stats.uniqueUsers,
                    stats.activeChannels,
                    stats.queuedMessages
            );
            session.getAsyncRemote().sendText(response);
        } catch (Exception e) {
            LOG.errorf(e, "Error getting stats");
            session.getAsyncRemote().sendText("ERROR: Failed to get statistics");
        }
    }

    /**
     * Send heartbeat to client
     */
    private void sendHeartbeat(Session session, WebSocketSession wsSession) {
        try {
            if (!session.isOpen()) {
                LOG.debugf("Session %s closed, canceling heartbeat", session.getId());
                wsSession.cancelHeartbeatTimer();
                return;
            }

            // Check if client is responsive (hasn't sent heartbeat in 2x interval)
            long secondsSinceLastHeartbeat = wsSession.getSecondsSinceLastHeartbeat();
            if (secondsSinceLastHeartbeat > HEARTBEAT_INTERVAL_SECONDS * 2) {
                LOG.warnf("⚠️ Session %s unresponsive for %ds, closing connection",
                        session.getId(), secondsSinceLastHeartbeat);
                session.close(new CloseReason(CloseReason.CloseCodes.GOING_AWAY, "Connection timeout"));
                return;
            }

            // Send heartbeat
            String heartbeat = String.format(
                    "{\"type\":\"HEARTBEAT\",\"timestamp\":%d,\"connectionDuration\":%d}",
                    System.currentTimeMillis(),
                    wsSession.getConnectionDurationSeconds()
            );
            session.getAsyncRemote().sendText(heartbeat);

            LOG.debugf("Heartbeat sent to session %s (connection: %ds)",
                    session.getId(), wsSession.getConnectionDurationSeconds());

        } catch (Exception e) {
            LOG.errorf(e, "❌ Error sending heartbeat to session %s", session.getId());
        }
    }

    /**
     * Broadcast transaction event to all subscribers
     * Called by external services when new transaction occurs
     */
    public static void broadcastTransaction(WebSocketSessionManager manager, String transactionData) {
        if (manager != null) {
            manager.broadcast("transactions", transactionData);
        }
    }
}
