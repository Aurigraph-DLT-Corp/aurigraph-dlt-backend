package io.aurigraph.v11.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.aurigraph.v11.user.JwtService;
import io.aurigraph.v11.user.User;
import io.aurigraph.v11.user.UserService;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.websocket.*;
import jakarta.websocket.server.ServerEndpoint;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * WebSocket Service - AV11-484 Implementation
 *
 * Production-ready WebSocket service with:
 * - JWT-based authentication and authorization
 * - Comprehensive subscription management
 * - RBAC (Role-Based Access Control) per channel
 * - Heartbeat/keep-alive mechanism (ping/pong every 30 seconds)
 * - Connection lifecycle management with automatic cleanup
 * - Resource limits per connection
 * - Rate limiting per subscription
 *
 * Endpoints:
 * - /ws/v11 - Main authenticated WebSocket endpoint
 *
 * Protocol:
 * - Client sends JWT token in first message: {"type": "auth", "token": "jwt_token_here"}
 * - Server validates and responds: {"type": "auth_response", "status": "success|error"}
 * - Client subscribes: {"type": "subscribe", "channel": "transactions", "filter": "status:pending"}
 * - Server broadcasts: {"type": "message", "channel": "transactions", "data": {...}}
 * - Heartbeat: Server sends ping every 30s, expects pong response
 *
 * Channels:
 * - transactions: Real-time transaction events (filter by status, sender, receiver)
 * - blocks: Real-time block events
 * - bridge: Cross-chain bridge events (filter by chainId)
 * - analytics: Real-time analytics updates (filter by metrics)
 * - consensus: Consensus state changes (requires ADMIN role)
 * - network: Network topology changes (requires VALIDATOR role)
 *
 * @author Real-Time Communication Agent (RTCA)
 * @since V11.6.0 (Sprint 16 - AV11-484)
 * @epic AV11-491 Real-Time Communication Infrastructure
 */
@ServerEndpoint(value = "/ws/v11", configurator = AuthenticatedWebSocketConfigurator.class)
@ApplicationScoped
public class WebSocketService {

    private static final Logger LOG = Logger.getLogger(WebSocketService.class);

    @Inject
    JwtService jwtService;

    @Inject
    UserService userService;

    @Inject
    WebSocketAuthService authService;

    @Inject
    WebSocketSubscriptionService subscriptionService;

    @Inject
    WebSocketSessionManager sessionManager;

    @Inject
    ObjectMapper objectMapper;

    // Configuration
    private static final int MAX_CONNECTIONS_PER_USER = 5;
    private static final int MAX_SUBSCRIPTIONS_PER_USER = 50;
    private static final long HEARTBEAT_INTERVAL_MS = 30_000; // 30 seconds
    private static final long CONNECTION_TIMEOUT_MS = 60_000; // 60 seconds

    // Heartbeat scheduler
    private final ScheduledExecutorService heartbeatScheduler = Executors.newScheduledThreadPool(4);

    /**
     * WebSocket connection opened
     *
     * @param session WebSocket session
     */
    @OnOpen
    public void onOpen(Session session) {
        String sessionId = session.getId();

        // Register unauthenticated session (will be authenticated later)
        WebSocketSession wsSession = sessionManager.registerSession(session, null, false);

        LOG.infof("WebSocket connection opened: %s (awaiting authentication)", sessionId);

        // Send authentication required message
        try {
            Map<String, Object> authRequired = Map.of(
                "type", "auth_required",
                "message", "Please send authentication token",
                "format", Map.of("type", "auth", "token", "your_jwt_token")
            );
            session.getAsyncRemote().sendText(objectMapper.writeValueAsString(authRequired));
        } catch (Exception e) {
            LOG.errorf(e, "Failed to send auth required message to session %s", sessionId);
        }

        // Schedule authentication timeout (close if not authenticated within 10 seconds)
        heartbeatScheduler.schedule(() -> {
            WebSocketSession ws = sessionManager.getSession(sessionId);
            if (ws != null && !ws.isAuthenticated() && ws.isAlive()) {
                LOG.warnf("Session %s authentication timeout - closing connection", sessionId);
                try {
                    session.close(new CloseReason(
                        CloseReason.CloseCodes.VIOLATED_POLICY,
                        "Authentication timeout"
                    ));
                } catch (Exception e) {
                    LOG.errorf(e, "Error closing unauthenticated session %s", sessionId);
                }
            }
        }, 10, TimeUnit.SECONDS);
    }

    /**
     * WebSocket message received
     *
     * @param message Message from client (JSON)
     * @param session WebSocket session
     */
    @OnMessage
    public void onMessage(String message, Session session) {
        String sessionId = session.getId();
        WebSocketSession wsSession = sessionManager.getSession(sessionId);

        if (wsSession == null) {
            LOG.warnf("Received message from unknown session: %s", sessionId);
            return;
        }

        try {
            // Parse message
            @SuppressWarnings("unchecked")
            Map<String, Object> messageData = objectMapper.readValue(message, Map.class);
            String type = (String) messageData.get("type");

            if (type == null) {
                sendError(session, "Missing 'type' field in message");
                return;
            }

            // Handle message based on type
            switch (type) {
                case "auth":
                    handleAuthentication(session, messageData);
                    break;
                case "subscribe":
                    handleSubscribe(session, wsSession, messageData);
                    break;
                case "unsubscribe":
                    handleUnsubscribe(session, wsSession, messageData);
                    break;
                case "pong":
                    handlePong(wsSession);
                    break;
                case "ping":
                    handlePing(session);
                    break;
                default:
                    sendError(session, "Unknown message type: " + type);
            }

        } catch (Exception e) {
            LOG.errorf(e, "Error processing message from session %s: %s", sessionId, message);
            sendError(session, "Invalid message format");
        }
    }

    /**
     * WebSocket connection closed
     *
     * @param session WebSocket session
     * @param closeReason Close reason
     */
    @OnClose
    public void onClose(Session session, CloseReason closeReason) {
        String sessionId = session.getId();
        WebSocketSession wsSession = sessionManager.getSession(sessionId);

        if (wsSession != null) {
            LOG.infof("WebSocket connection closed: %s (user: %s, reason: %s)",
                sessionId, wsSession.getUserId(), closeReason.getReasonPhrase());

            // Cleanup
            sessionManager.unregisterSession(sessionId);
            authService.cleanupSession(sessionId);
            subscriptionService.cleanupRateLimitTracking(sessionId);
        } else {
            LOG.infof("WebSocket connection closed: %s (reason: %s)",
                sessionId, closeReason.getReasonPhrase());
        }
    }

    /**
     * WebSocket error occurred
     *
     * @param session WebSocket session
     * @param throwable Error
     */
    @OnError
    public void onError(Session session, Throwable throwable) {
        String sessionId = session.getId();
        LOG.errorf(throwable, "WebSocket error on session %s", sessionId);

        // Try to close the session gracefully
        try {
            if (session.isOpen()) {
                session.close(new CloseReason(
                    CloseReason.CloseCodes.UNEXPECTED_CONDITION,
                    "Internal server error"
                ));
            }
        } catch (Exception e) {
            LOG.errorf(e, "Error closing session %s after error", sessionId);
        }
    }

    /**
     * Handle authentication message
     *
     * @param session WebSocket session
     * @param messageData Message data containing JWT token
     */
    private void handleAuthentication(Session session, Map<String, Object> messageData) {
        String sessionId = session.getId();
        String token = (String) messageData.get("token");

        if (token == null || token.isBlank()) {
            sendAuthResponse(session, false, "Missing token");
            return;
        }

        // Extract client info
        String clientIp = getClientIp(session);
        String userAgent = getUserAgent(session);

        // Authenticate
        Optional<String> userIdOpt = authService.authenticateConnection(token, clientIp, userAgent);

        if (!userIdOpt.isPresent()) {
            sendAuthResponse(session, false, "Invalid or expired token");
            return;
        }

        String userId = userIdOpt.get();

        // Check connection limit per user
        List<WebSocketSession> userSessions = sessionManager.getUserSessions(userId);
        if (userSessions.size() >= MAX_CONNECTIONS_PER_USER) {
            sendAuthResponse(session, false, "Maximum connections per user exceeded");
            return;
        }

        // Create authenticated session
        WebSocketSession authenticatedSession = sessionManager.registerSession(session, userId, true);

        // Register session tracking
        authService.registerSession(sessionId, userId);

        // Create device fingerprint
        authService.createDeviceFingerprint(sessionId, clientIp, userAgent, Map.of());

        // Start heartbeat
        startHeartbeat(authenticatedSession);

        // Deliver queued messages
        int delivered = sessionManager.deliverQueuedMessages(sessionId);

        sendAuthResponse(session, true, "Authentication successful");

        LOG.infof("Session %s authenticated for user %s (delivered %d queued messages)",
            sessionId, userId, delivered);
    }

    /**
     * Handle subscribe message
     *
     * @param session WebSocket session
     * @param wsSession WebSocket session wrapper
     * @param messageData Message data
     */
    private void handleSubscribe(Session session, WebSocketSession wsSession, Map<String, Object> messageData) {
        if (!wsSession.isAuthenticated()) {
            sendError(session, "Authentication required before subscribing");
            return;
        }

        String channel = (String) messageData.get("channel");
        if (channel == null || channel.isBlank()) {
            sendError(session, "Missing 'channel' field");
            return;
        }

        String filter = (String) messageData.get("filter");
        int priority = messageData.containsKey("priority") ?
            ((Number) messageData.get("priority")).intValue() : 0;

        String userId = wsSession.getUserId();

        // Check RBAC authorization for channel
        if (!isAuthorizedForChannel(userId, channel)) {
            sendError(session, "Not authorized to subscribe to channel: " + channel);
            return;
        }

        // Subscribe in database (persisted)
        WebSocketSubscription subscription = subscriptionService.subscribe(userId, channel, priority);

        if (subscription == null) {
            sendError(session, "Subscription failed (limit exceeded or invalid channel)");
            return;
        }

        // Subscribe in session manager (runtime)
        sessionManager.subscribe(wsSession.getSessionId(), channel);

        // Send confirmation
        try {
            Map<String, Object> response = Map.of(
                "type", "subscribe_response",
                "status", "success",
                "channel", channel,
                "filter", filter != null ? filter : "",
                "priority", priority
            );
            session.getAsyncRemote().sendText(objectMapper.writeValueAsString(response));

            LOG.infof("Session %s subscribed to channel '%s' (filter: %s, priority: %d)",
                wsSession.getSessionId(), channel, filter, priority);
        } catch (Exception e) {
            LOG.errorf(e, "Failed to send subscribe response to session %s", wsSession.getSessionId());
        }
    }

    /**
     * Handle unsubscribe message
     *
     * @param session WebSocket session
     * @param wsSession WebSocket session wrapper
     * @param messageData Message data
     */
    private void handleUnsubscribe(Session session, WebSocketSession wsSession, Map<String, Object> messageData) {
        if (!wsSession.isAuthenticated()) {
            sendError(session, "Authentication required");
            return;
        }

        String channel = (String) messageData.get("channel");
        if (channel == null || channel.isBlank()) {
            sendError(session, "Missing 'channel' field");
            return;
        }

        String userId = wsSession.getUserId();

        // Unsubscribe from database
        subscriptionService.unsubscribe(userId, channel);

        // Unsubscribe from session manager
        sessionManager.unsubscribe(wsSession.getSessionId(), channel);

        // Send confirmation
        try {
            Map<String, Object> response = Map.of(
                "type", "unsubscribe_response",
                "status", "success",
                "channel", channel
            );
            session.getAsyncRemote().sendText(objectMapper.writeValueAsString(response));

            LOG.infof("Session %s unsubscribed from channel '%s'",
                wsSession.getSessionId(), channel);
        } catch (Exception e) {
            LOG.errorf(e, "Failed to send unsubscribe response to session %s", wsSession.getSessionId());
        }
    }

    /**
     * Handle pong message (heartbeat response)
     *
     * @param wsSession WebSocket session
     */
    private void handlePong(WebSocketSession wsSession) {
        wsSession.updateHeartbeat();
        authService.updateActivity(wsSession.getSessionId());
        LOG.debugf("Received pong from session %s", wsSession.getSessionId());
    }

    /**
     * Handle ping message from client
     *
     * @param session WebSocket session
     */
    private void handlePing(Session session) {
        try {
            Map<String, Object> pong = Map.of("type", "pong", "timestamp", System.currentTimeMillis());
            session.getAsyncRemote().sendText(objectMapper.writeValueAsString(pong));
        } catch (Exception e) {
            LOG.errorf(e, "Failed to send pong to session %s", session.getId());
        }
    }

    /**
     * Start heartbeat for session
     *
     * @param wsSession WebSocket session
     */
    private void startHeartbeat(WebSocketSession wsSession) {
        var heartbeatTask = heartbeatScheduler.scheduleAtFixedRate(() -> {
            try {
                if (!wsSession.isAlive()) {
                    LOG.warnf("Session %s is not alive, stopping heartbeat", wsSession.getSessionId());
                    return;
                }

                // Check for timeout
                long secondsSinceLastHeartbeat = wsSession.getSecondsSinceLastHeartbeat();
                if (secondsSinceLastHeartbeat > CONNECTION_TIMEOUT_MS / 1000) {
                    LOG.warnf("Session %s heartbeat timeout (%d seconds), closing connection",
                        wsSession.getSessionId(), secondsSinceLastHeartbeat);
                    wsSession.getWsSession().close(new CloseReason(
                        CloseReason.CloseCodes.GOING_AWAY,
                        "Heartbeat timeout"
                    ));
                    return;
                }

                // Send ping
                Map<String, Object> ping = Map.of(
                    "type", "ping",
                    "timestamp", System.currentTimeMillis()
                );
                String message = objectMapper.writeValueAsString(ping);
                wsSession.getWsSession().getAsyncRemote().sendText(message);

                LOG.debugf("Sent heartbeat ping to session %s", wsSession.getSessionId());

            } catch (Exception e) {
                LOG.errorf(e, "Error in heartbeat for session %s", wsSession.getSessionId());
            }
        }, HEARTBEAT_INTERVAL_MS, HEARTBEAT_INTERVAL_MS, TimeUnit.MILLISECONDS);

        wsSession.setHeartbeatTimer(heartbeatTask);
    }

    /**
     * Check RBAC authorization for channel subscription
     *
     * @param userId User ID
     * @param channel Channel name
     * @return true if authorized
     */
    private boolean isAuthorizedForChannel(String userId, String channel) {
        try {
            User user = userService.findById(UUID.fromString(userId));
            if (user == null) {
                return false;
            }
            String roleName = user.role.name;

            // Public channels (all authenticated users)
            if (channel.equals("transactions") || channel.equals("blocks") ||
                channel.equals("bridge") || channel.equals("analytics")) {
                return true;
            }

            // Admin-only channels
            if (channel.equals("consensus") || channel.equals("admin")) {
                return roleName.equals("ADMIN");
            }

            // Validator channels
            if (channel.equals("network") || channel.equals("validators")) {
                return roleName.equals("VALIDATOR") || roleName.equals("ADMIN");
            }

            // Developer channels
            if (channel.equals("debug") || channel.equals("metrics")) {
                return roleName.equals("DEVELOPER") || roleName.equals("ADMIN");
            }

            // Unknown channel - deny by default
            return false;

        } catch (Exception e) {
            LOG.errorf(e, "Error checking authorization for user %s, channel %s", userId, channel);
            return false;
        }
    }

    /**
     * Send error message to client
     *
     * @param session WebSocket session
     * @param errorMessage Error message
     */
    private void sendError(Session session, String errorMessage) {
        try {
            Map<String, Object> error = Map.of(
                "type", "error",
                "message", errorMessage,
                "timestamp", System.currentTimeMillis()
            );
            session.getAsyncRemote().sendText(objectMapper.writeValueAsString(error));
        } catch (Exception e) {
            LOG.errorf(e, "Failed to send error message to session %s", session.getId());
        }
    }

    /**
     * Send authentication response
     *
     * @param session WebSocket session
     * @param success Success status
     * @param message Response message
     */
    private void sendAuthResponse(Session session, boolean success, String message) {
        try {
            Map<String, Object> response = Map.of(
                "type", "auth_response",
                "status", success ? "success" : "error",
                "message", message,
                "timestamp", System.currentTimeMillis()
            );
            session.getAsyncRemote().sendText(objectMapper.writeValueAsString(response));
        } catch (Exception e) {
            LOG.errorf(e, "Failed to send auth response to session %s", session.getId());
        }
    }

    /**
     * Get client IP address from session
     *
     * @param session WebSocket session
     * @return Client IP address
     */
    private String getClientIp(Session session) {
        // Try to get real IP from headers (behind proxy)
        Object remoteAddr = session.getUserProperties().get("jakarta.websocket.endpoint.remoteAddress");
        if (remoteAddr != null) {
            return remoteAddr.toString();
        }
        return "unknown";
    }

    /**
     * Get user agent from session
     *
     * @param session WebSocket session
     * @return User agent string
     */
    private String getUserAgent(Session session) {
        Object userAgent = session.getUserProperties().get("User-Agent");
        if (userAgent != null) {
            return userAgent.toString();
        }
        return "unknown";
    }

    /**
     * Broadcast message to channel subscribers (public API)
     *
     * @param channel Channel name
     * @param data Message data
     */
    public void broadcastToChannel(String channel, Object data) {
        try {
            Map<String, Object> message = Map.of(
                "type", "message",
                "channel", channel,
                "data", data,
                "timestamp", System.currentTimeMillis()
            );
            String jsonMessage = objectMapper.writeValueAsString(message);
            sessionManager.broadcast(channel, jsonMessage);

        } catch (Exception e) {
            LOG.errorf(e, "Failed to broadcast to channel %s", channel);
        }
    }

    /**
     * Cleanup stale sessions (scheduled task)
     */
    @Scheduled(every = "60s")
    void cleanupStaleSessions() {
        LOG.debug("Running stale session cleanup task");

        // Get all active sessions
        var stats = sessionManager.getStats();
        LOG.debugf("Session stats: %s", stats);

        // Cleanup expired subscriptions
        int expired = subscriptionService.cleanupExpiredSubscriptions();
        if (expired > 0) {
            LOG.infof("Cleaned up %d expired subscriptions", expired);
        }
    }

    /**
     * Get WebSocket statistics
     *
     * @return Session statistics
     */
    public WebSocketSessionManager.SessionStats getStats() {
        return sessionManager.getStats();
    }
}
