package io.aurigraph.v11.websocket;

import jakarta.websocket.Session;

import java.time.Instant;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

/**
 * WebSocket Session Model
 *
 * Tracks session state for authenticated WebSocket connections:
 * - User identification and authentication status
 * - Channel subscriptions
 * - Heartbeat timer for connection monitoring
 * - Session lifecycle timestamps
 *
 * Thread-safe with concurrent collections for subscription management.
 *
 * @author WebSocket Development Agent (WDA)
 * @since V11.6.0 (Sprint 16 - AV11-484)
 */
public class WebSocketSession {

    private final String sessionId;
    private final String userId;
    private final Session wsSession;
    private final boolean authenticated;
    private final Set<String> subscribedChannels;
    private final Instant connectedAt;
    private Instant lastHeartbeat;
    private ScheduledFuture<?> heartbeatTimer;

    /**
     * Create new WebSocket session
     *
     * @param sessionId WebSocket session ID
     * @param userId User ID (null if unauthenticated)
     * @param wsSession Underlying WebSocket session
     * @param authenticated Authentication status
     */
    public WebSocketSession(String sessionId, String userId, Session wsSession, boolean authenticated) {
        this.sessionId = sessionId;
        this.userId = userId;
        this.wsSession = wsSession;
        this.authenticated = authenticated;
        this.subscribedChannels = ConcurrentHashMap.newKeySet();
        this.connectedAt = Instant.now();
        this.lastHeartbeat = Instant.now();
    }

    /**
     * Subscribe to a channel
     */
    public void subscribe(String channel) {
        subscribedChannels.add(channel);
    }

    /**
     * Unsubscribe from a channel
     */
    public void unsubscribe(String channel) {
        subscribedChannels.remove(channel);
    }

    /**
     * Check if subscribed to a channel
     */
    public boolean isSubscribedTo(String channel) {
        return subscribedChannels.contains(channel);
    }

    /**
     * Get all subscribed channels
     */
    public Set<String> getSubscribedChannels() {
        return Set.copyOf(subscribedChannels);
    }

    /**
     * Update heartbeat timestamp
     */
    public void updateHeartbeat() {
        this.lastHeartbeat = Instant.now();
    }

    /**
     * Set heartbeat timer for connection monitoring
     */
    public void setHeartbeatTimer(ScheduledFuture<?> timer) {
        // Cancel existing timer if any
        if (this.heartbeatTimer != null && !this.heartbeatTimer.isDone()) {
            this.heartbeatTimer.cancel(false);
        }
        this.heartbeatTimer = timer;
    }

    /**
     * Cancel heartbeat timer
     */
    public void cancelHeartbeatTimer() {
        if (this.heartbeatTimer != null && !this.heartbeatTimer.isDone()) {
            this.heartbeatTimer.cancel(false);
        }
    }

    /**
     * Check if session is alive (WebSocket session is open)
     */
    public boolean isAlive() {
        return wsSession != null && wsSession.isOpen();
    }

    /**
     * Get time since last heartbeat in seconds
     */
    public long getSecondsSinceLastHeartbeat() {
        return Instant.now().getEpochSecond() - lastHeartbeat.getEpochSecond();
    }

    /**
     * Get connection duration in seconds
     */
    public long getConnectionDurationSeconds() {
        return Instant.now().getEpochSecond() - connectedAt.getEpochSecond();
    }

    // Getters
    public String getSessionId() {
        return sessionId;
    }

    public String getUserId() {
        return userId;
    }

    public Session getWsSession() {
        return wsSession;
    }

    public boolean isAuthenticated() {
        return authenticated;
    }

    public Instant getConnectedAt() {
        return connectedAt;
    }

    public Instant getLastHeartbeat() {
        return lastHeartbeat;
    }

    @Override
    public String toString() {
        return "WebSocketSession{" +
                "sessionId='" + sessionId + '\'' +
                ", userId='" + userId + '\'' +
                ", authenticated=" + authenticated +
                ", subscribedChannels=" + subscribedChannels.size() +
                ", connectedAt=" + connectedAt +
                ", lastHeartbeat=" + lastHeartbeat +
                ", alive=" + isAlive() +
                '}';
    }
}
