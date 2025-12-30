package io.aurigraph.v11.auth;

import jakarta.websocket.*;
import jakarta.websocket.server.ServerEndpoint;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * TokenInvalidationWebSocket
 *
 * WebSocket endpoint for real-time token invalidation notifications
 * Clients can connect and receive immediate notifications when:
 * - Token is revoked
 * - Token expires
 * - Logout all devices is triggered
 * - Password is changed (requiring re-authentication)
 *
 * WebSocket URL: ws://localhost:9003/ws/tokens
 *
 * Message Format (JSON):
 * {
 *   "action": "SUBSCRIBE|UNSUBSCRIBE|TOKEN_REVOKED|TOKEN_EXPIRED|LOGOUT_ALL",
 *   "tokenId": "token-id",
 *   "userId": "user-id",
 *   "reason": "Revocation reason",
 *   "timestamp": "2025-11-11T10:15:00"
 * }
 */
@ServerEndpoint("/ws/tokens")
@ApplicationScoped
public class TokenInvalidationWebSocket {

    private static final Logger LOG = Logger.getLogger(TokenInvalidationWebSocket.class);

    // Store active WebSocket sessions per user
    private static final Map<String, Set<Session>> userSessions = new ConcurrentHashMap<>();

    // Store active WebSocket sessions per token
    private static final Map<String, Session> tokenSessions = new ConcurrentHashMap<>();

    @Inject
    AuthTokenService authTokenService;

    private ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Handle new WebSocket connection
     */
    @OnOpen
    public void onOpen(Session session) {
        LOG.infof("✅ WebSocket connection opened: %s", session.getId());
    }

    /**
     * Handle WebSocket message
     * Expected format: {"action": "SUBSCRIBE", "userId": "user-id", "tokenId": "token-id"}
     */
    @OnMessage
    public void onMessage(String message, Session session) {
        try {
            TokenWebSocketMessage msg = objectMapper.readValue(message, TokenWebSocketMessage.class);

            switch (msg.action) {
                case "SUBSCRIBE":
                    handleSubscribe(session, msg.userId, msg.tokenId);
                    break;
                case "UNSUBSCRIBE":
                    handleUnsubscribe(session, msg.userId, msg.tokenId);
                    break;
                case "PING":
                    handlePing(session);
                    break;
                default:
                    sendError(session, "Unknown action: " + msg.action);
            }
        } catch (Exception e) {
            LOG.errorf(e, "Error processing WebSocket message");
            sendError(session, "Error processing message: " + e.getMessage());
        }
    }

    /**
     * Handle WebSocket close
     */
    @OnClose
    public void onClose(Session session) {
        LOG.infof("✅ WebSocket connection closed: %s", session.getId());
        removeSession(session);
    }

    /**
     * Handle WebSocket error
     */
    @OnError
    public void onError(Session session, Throwable error) {
        LOG.errorf(error, "WebSocket error on session %s", session.getId());
    }

    /**
     * Handle subscription to token invalidation notifications
     * SECURITY: Client must provide valid JWT token for authentication
     */
    private void handleSubscribe(Session session, String userId, String tokenId) {
        if (userId == null || userId.isBlank()) {
            sendError(session, "userId is required");
            return;
        }

        // SECURITY: Validate that userId matches authenticated user from JWT
        // Extract from session properties if available, or from JWT in handshake
        String authenticatedUserId = getAuthenticatedUserId(session);
        if (authenticatedUserId == null) {
            sendError(session, "Authentication required. Please provide valid JWT token");
            LOG.warnf("❌ SECURITY: WebSocket subscription attempt without authentication for userId: %s", userId);
            return;
        }

        // SECURITY: Prevent user from subscribing to another user's tokens
        if (!userId.equals(authenticatedUserId)) {
            sendError(session, "Unauthorized: Cannot subscribe to another user's tokens");
            LOG.warnf("❌ SECURITY: User %s attempted to subscribe to user %s's tokens", authenticatedUserId, userId);
            return;
        }

        // Register session for user
        userSessions.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet()).add(session);

        // Optionally register for specific token if provided
        if (tokenId != null && !tokenId.isBlank()) {
            tokenSessions.put(tokenId, session);
        }

        // Send confirmation
        TokenWebSocketMessage response = new TokenWebSocketMessage();
        response.action = "SUBSCRIBED";
        response.userId = userId;
        response.tokenId = tokenId;
        response.timestamp = LocalDateTime.now().toString();
        response.message = "Subscribed to token invalidation notifications";

        sendMessage(session, response);
        LOG.infof("✅ User %s subscribed to token notifications (token: %s)", userId, tokenId);
    }

    /**
     * Handle unsubscribe from notifications
     */
    private void handleUnsubscribe(Session session, String userId, String tokenId) {
        if (userId != null && !userId.isBlank()) {
            Set<Session> sessions = userSessions.get(userId);
            if (sessions != null) {
                sessions.remove(session);
            }
        }

        if (tokenId != null && !tokenId.isBlank()) {
            tokenSessions.remove(tokenId);
        }

        TokenWebSocketMessage response = new TokenWebSocketMessage();
        response.action = "UNSUBSCRIBED";
        response.message = "Unsubscribed from notifications";

        sendMessage(session, response);
        LOG.infof("✅ Unsubscribed from token notifications");
    }

    /**
     * Handle ping/keepalive
     */
    private void handlePing(Session session) {
        TokenWebSocketMessage response = new TokenWebSocketMessage();
        response.action = "PONG";
        response.timestamp = LocalDateTime.now().toString();

        sendMessage(session, response);
    }

    /**
     * Notify all clients of a specific token revocation
     * Called when token is revoked via API or login
     */
    public void notifyTokenRevoked(String tokenId, String userId, String reason) {
        TokenWebSocketMessage notification = new TokenWebSocketMessage();
        notification.action = "TOKEN_REVOKED";
        notification.tokenId = tokenId;
        notification.userId = userId;
        notification.reason = reason;
        notification.timestamp = LocalDateTime.now().toString();
        notification.message = "Token has been revoked";

        // Notify specific token session
        Session tokenSession = tokenSessions.get(tokenId);
        if (tokenSession != null && tokenSession.isOpen()) {
            sendMessage(tokenSession, notification);
        }

        // Notify all user sessions
        broadcastToUser(userId, notification);

        LOG.infof("🔔 Notified revocation of token %s for user %s: %s", tokenId, userId, reason);
    }

    /**
     * Notify all clients of all tokens being revoked (logout all devices)
     */
    public void notifyLogoutAll(String userId, String reason) {
        TokenWebSocketMessage notification = new TokenWebSocketMessage();
        notification.action = "LOGOUT_ALL";
        notification.userId = userId;
        notification.reason = reason;
        notification.timestamp = LocalDateTime.now().toString();
        notification.message = "All devices have been logged out";

        broadcastToUser(userId, notification);

        LOG.infof("🔔 Notified logout all devices for user %s: %s", userId, reason);
    }

    /**
     * Notify clients of token expiration
     */
    public void notifyTokenExpired(String tokenId, String userId) {
        TokenWebSocketMessage notification = new TokenWebSocketMessage();
        notification.action = "TOKEN_EXPIRED";
        notification.tokenId = tokenId;
        notification.userId = userId;
        notification.timestamp = LocalDateTime.now().toString();
        notification.message = "Token has expired";

        // Notify specific token session
        Session tokenSession = tokenSessions.get(tokenId);
        if (tokenSession != null && tokenSession.isOpen()) {
            sendMessage(tokenSession, notification);
        }

        broadcastToUser(userId, notification);

        LOG.infof("🔔 Notified expiration of token %s for user %s", tokenId, userId);
    }

    /**
     * Notify clients of password change (requires re-authentication)
     */
    public void notifyPasswordChanged(String userId) {
        TokenWebSocketMessage notification = new TokenWebSocketMessage();
        notification.action = "PASSWORD_CHANGED";
        notification.userId = userId;
        notification.timestamp = LocalDateTime.now().toString();
        notification.message = "Password has been changed - please re-authenticate";

        broadcastToUser(userId, notification);

        // Revoke all tokens for this user
        authTokenService.revokeAllTokensForUser(userId, "Password changed");

        LOG.infof("🔔 Notified password change and revoked all tokens for user %s", userId);
    }

    /**
     * Broadcast message to all sessions of a specific user
     */
    private void broadcastToUser(String userId, TokenWebSocketMessage message) {
        Set<Session> sessions = userSessions.get(userId);
        if (sessions != null) {
            sessions.forEach(session -> {
                if (session.isOpen()) {
                    sendMessage(session, message);
                }
            });
        }
    }

    /**
     * Send message to specific session
     */
    private void sendMessage(Session session, Object message) {
        try {
            String json = objectMapper.writeValueAsString(message);
            session.getBasicRemote().sendText(json);
        } catch (IOException e) {
            LOG.errorf(e, "Error sending WebSocket message to session %s", session.getId());
            try {
                session.close();
            } catch (IOException closeException) {
                LOG.errorf(closeException, "Error closing session %s", session.getId());
            }
        }
    }

    /**
     * Send error message to session
     */
    private void sendError(Session session, String errorMessage) {
        TokenWebSocketMessage error = new TokenWebSocketMessage();
        error.action = "ERROR";
        error.message = errorMessage;
        error.timestamp = LocalDateTime.now().toString();

        sendMessage(session, error);
    }

    /**
     * Remove session from tracking
     */
    private void removeSession(Session session) {
        userSessions.values().forEach(sessions -> sessions.remove(session));
        tokenSessions.values().removeIf(s -> s.equals(session));
    }

    /**
     * Get active connections count
     */
    public int getActiveConnectionsCount() {
        return userSessions.values().stream()
            .mapToInt(Set::size)
            .sum();
    }

    /**
     * Get connections count for specific user
     */
    public int getConnectionsCountForUser(String userId) {
        Set<Session> sessions = userSessions.get(userId);
        return sessions != null ? sessions.size() : 0;
    }

    /**
     * SECURITY: Extract authenticated user ID from WebSocket session
     * This should come from JWT token validation during handshake
     * For now, this is a placeholder - in production, use proper JWT extraction
     *
     * @param session WebSocket session
     * @return authenticated user ID, or null if not authenticated
     */
    private String getAuthenticatedUserId(Session session) {
        // Try to extract from session properties (set during handshake)
        Object userIdObj = session.getUserProperties().get("userId");
        if (userIdObj instanceof String) {
            return (String) userIdObj;
        }

        // Try to extract from query parameters (for WebSocket URI like ws://host/ws/tokens?token=JWT)
        // This is a fallback and should be replaced with proper JWT validation
        try {
            Object userIdFromQuery = session.getUserProperties().get("jwtUserId");
            if (userIdFromQuery instanceof String) {
                return (String) userIdFromQuery;
            }
        } catch (Exception e) {
            LOG.debugf("Could not extract user ID from WebSocket session: %s", e.getMessage());
        }

        return null;
    }

    // ==================== Message DTO ====================

    public static class TokenWebSocketMessage {
        public String action;
        public String tokenId;
        public String userId;
        public String reason;
        public String message;
        public String timestamp;

        // Getters and Setters
        public String getAction() { return action; }
        public void setAction(String action) { this.action = action; }

        public String getTokenId() { return tokenId; }
        public void setTokenId(String tokenId) { this.tokenId = tokenId; }

        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }

        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public String getTimestamp() { return timestamp; }
        public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
    }
}
