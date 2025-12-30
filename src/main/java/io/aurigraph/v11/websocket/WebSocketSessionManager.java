package io.aurigraph.v11.websocket;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.websocket.Session;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * WebSocket Session Manager
 *
 * Central manager for WebSocket sessions with:
 * - Session registration and lifecycle management
 * - Channel subscription management
 * - Message broadcasting to subscribed clients
 * - Message queuing for offline/slow clients
 * - Session statistics and monitoring
 *
 * Thread-safety: All maps are ConcurrentHashMap for thread-safe operations.
 *
 * Architecture:
 * - activeSessions: Map<sessionId, WebSocketSession>
 * - userSessions: Map<userId, Set<sessionId>> - for multi-device support
 * - channelSubscriptions: Map<channel, Set<sessionId>>
 * - messageQueues: Map<userId, MessageQueue>
 *
 * @author WebSocket Development Agent (WDA)
 * @since V11.6.0 (Sprint 16 - AV11-484)
 */
@ApplicationScoped
public class WebSocketSessionManager {

    private static final Logger LOG = Logger.getLogger(WebSocketSessionManager.class);

    // Session storage
    private final Map<String, WebSocketSession> activeSessions = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> userSessions = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> channelSubscriptions = new ConcurrentHashMap<>();
    private final Map<String, MessageQueue> messageQueues = new ConcurrentHashMap<>();

    /**
     * Register a new WebSocket session
     *
     * @param session WebSocket session
     * @param userId User ID (null for unauthenticated)
     * @param authenticated Authentication status
     * @return WebSocketSession wrapper
     */
    public WebSocketSession registerSession(Session session, String userId, boolean authenticated) {
        String sessionId = session.getId();

        // Create session wrapper
        WebSocketSession wsSession = new WebSocketSession(sessionId, userId, session, authenticated);

        // Store in active sessions
        activeSessions.put(sessionId, wsSession);

        // Track user sessions (for multi-device support)
        if (userId != null) {
            userSessions.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet()).add(sessionId);

            // Create message queue if it doesn't exist
            messageQueues.computeIfAbsent(userId, MessageQueue::new);
        }

        LOG.infof("✅ Registered WebSocket session %s for user %s (authenticated: %s, total sessions: %d)",
                sessionId, userId != null ? userId : "anonymous", authenticated, activeSessions.size());

        return wsSession;
    }

    /**
     * Unregister a WebSocket session
     *
     * Cleans up:
     * - Active session
     * - User session mapping
     * - All channel subscriptions
     * - Heartbeat timer
     *
     * @param sessionId Session ID to unregister
     */
    public void unregisterSession(String sessionId) {
        WebSocketSession wsSession = activeSessions.remove(sessionId);

        if (wsSession == null) {
            LOG.warnf("⚠️ Attempted to unregister unknown session: %s", sessionId);
            return;
        }

        // Cancel heartbeat timer
        wsSession.cancelHeartbeatTimer();

        // Remove from user sessions
        String userId = wsSession.getUserId();
        if (userId != null) {
            Set<String> sessions = userSessions.get(userId);
            if (sessions != null) {
                sessions.remove(sessionId);
                if (sessions.isEmpty()) {
                    userSessions.remove(userId);
                }
            }
        }

        // Remove from all channel subscriptions
        Set<String> subscribedChannels = wsSession.getSubscribedChannels();
        for (String channel : subscribedChannels) {
            unsubscribe(sessionId, channel);
        }

        LOG.infof("✅ Unregistered WebSocket session %s for user %s (remaining sessions: %d)",
                sessionId, userId != null ? userId : "anonymous", activeSessions.size());
    }

    /**
     * Subscribe session to a channel
     *
     * @param sessionId Session ID
     * @param channel Channel name
     * @return true if subscription successful
     */
    public boolean subscribe(String sessionId, String channel) {
        WebSocketSession wsSession = activeSessions.get(sessionId);

        if (wsSession == null) {
            LOG.warnf("⚠️ Cannot subscribe: Session %s not found", sessionId);
            return false;
        }

        // Add to session's subscriptions
        wsSession.subscribe(channel);

        // Add to channel's subscribers
        channelSubscriptions.computeIfAbsent(channel, k -> ConcurrentHashMap.newKeySet()).add(sessionId);

        LOG.infof("✅ Session %s subscribed to channel '%s' (channel subscribers: %d)",
                sessionId, channel, channelSubscriptions.get(channel).size());

        return true;
    }

    /**
     * Unsubscribe session from a channel
     *
     * @param sessionId Session ID
     * @param channel Channel name
     * @return true if unsubscription successful
     */
    public boolean unsubscribe(String sessionId, String channel) {
        WebSocketSession wsSession = activeSessions.get(sessionId);

        if (wsSession == null) {
            LOG.warnf("⚠️ Cannot unsubscribe: Session %s not found", sessionId);
            return false;
        }

        // Remove from session's subscriptions
        wsSession.unsubscribe(channel);

        // Remove from channel's subscribers
        Set<String> subscribers = channelSubscriptions.get(channel);
        if (subscribers != null) {
            subscribers.remove(sessionId);
            if (subscribers.isEmpty()) {
                channelSubscriptions.remove(channel);
            }
        }

        LOG.infof("✅ Session %s unsubscribed from channel '%s'", sessionId, channel);

        return true;
    }

    /**
     * Broadcast message to all subscribers of a channel
     *
     * @param channel Channel name
     * @param message Message to broadcast
     */
    public void broadcast(String channel, String message) {
        Set<String> subscribers = channelSubscriptions.get(channel);

        if (subscribers == null || subscribers.isEmpty()) {
            LOG.debugf("No subscribers for channel '%s'", channel);
            return;
        }

        int successCount = 0;
        int queuedCount = 0;
        int errorCount = 0;

        for (String sessionId : subscribers) {
            WebSocketSession wsSession = activeSessions.get(sessionId);

            if (wsSession == null || !wsSession.isAlive()) {
                // Session no longer active - queue message for user
                if (wsSession != null && wsSession.getUserId() != null) {
                    queueMessage(wsSession.getUserId(), message, channel);
                    queuedCount++;
                }
                continue;
            }

            try {
                // Send message asynchronously
                wsSession.getWsSession().getAsyncRemote().sendText(message);
                successCount++;
            } catch (Exception e) {
                LOG.errorf(e, "❌ Failed to send message to session %s", sessionId);

                // Queue message for later delivery
                if (wsSession.getUserId() != null) {
                    queueMessage(wsSession.getUserId(), message, channel);
                    queuedCount++;
                }
                errorCount++;
            }
        }

        LOG.infof("📢 Broadcast to channel '%s': %d sent, %d queued, %d errors (total subscribers: %d)",
                channel, successCount, queuedCount, errorCount, subscribers.size());
    }

    /**
     * Send message to a specific session
     *
     * @param sessionId Session ID
     * @param message Message to send
     * @return true if message sent successfully
     */
    public boolean sendToSession(String sessionId, String message) {
        WebSocketSession wsSession = activeSessions.get(sessionId);

        if (wsSession == null || !wsSession.isAlive()) {
            LOG.warnf("⚠️ Cannot send message: Session %s not active", sessionId);
            return false;
        }

        try {
            wsSession.getWsSession().getAsyncRemote().sendText(message);
            LOG.debugf("Message sent to session %s", sessionId);
            return true;
        } catch (Exception e) {
            LOG.errorf(e, "❌ Failed to send message to session %s", sessionId);
            return false;
        }
    }

    /**
     * Queue message for offline/unavailable user
     *
     * @param userId User ID
     * @param message Message content
     * @param channel Channel name
     */
    public void queueMessage(String userId, String message, String channel) {
        MessageQueue queue = messageQueues.computeIfAbsent(userId, MessageQueue::new);
        queue.enqueue(message, channel);
        LOG.debugf("Message queued for user %s on channel %s (queue size: %d)",
                userId, channel, queue.size());
    }

    /**
     * Deliver queued messages to a session
     *
     * @param sessionId Session ID
     * @return Number of messages delivered
     */
    public int deliverQueuedMessages(String sessionId) {
        WebSocketSession wsSession = activeSessions.get(sessionId);

        if (wsSession == null || wsSession.getUserId() == null) {
            return 0;
        }

        MessageQueue queue = messageQueues.get(wsSession.getUserId());
        if (queue == null || queue.isEmpty()) {
            return 0;
        }

        int delivered = 0;
        MessageQueue.QueuedMessage queuedMsg;

        while ((queuedMsg = queue.dequeue()) != null) {
            try {
                wsSession.getWsSession().getAsyncRemote().sendText(queuedMsg.getMessage());
                delivered++;
            } catch (Exception e) {
                LOG.errorf(e, "❌ Failed to deliver queued message to session %s", sessionId);
                // Re-queue the message
                queue.enqueue(queuedMsg.getMessage(), queuedMsg.getChannel(), queuedMsg.getPriority());
                break; // Stop trying to deliver more messages
            }
        }

        LOG.infof("📬 Delivered %d queued messages to session %s", delivered, sessionId);
        return delivered;
    }

    /**
     * Get session by ID
     */
    public WebSocketSession getSession(String sessionId) {
        return activeSessions.get(sessionId);
    }

    /**
     * Get all sessions for a user
     */
    public List<WebSocketSession> getUserSessions(String userId) {
        Set<String> sessionIds = userSessions.get(userId);
        if (sessionIds == null || sessionIds.isEmpty()) {
            return Collections.emptyList();
        }

        return sessionIds.stream()
                .map(activeSessions::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * Get session statistics
     */
    public SessionStats getStats() {
        int authenticatedSessions = (int) activeSessions.values().stream()
                .filter(WebSocketSession::isAuthenticated)
                .count();

        int totalQueuedMessages = messageQueues.values().stream()
                .mapToInt(MessageQueue::size)
                .sum();

        return new SessionStats(
                activeSessions.size(),
                authenticatedSessions,
                userSessions.size(),
                channelSubscriptions.size(),
                totalQueuedMessages
        );
    }

    /**
     * Session Statistics
     */
    public static class SessionStats {
        public final int totalSessions;
        public final int authenticatedSessions;
        public final int uniqueUsers;
        public final int activeChannels;
        public final int queuedMessages;

        public SessionStats(int totalSessions, int authenticatedSessions,
                          int uniqueUsers, int activeChannels, int queuedMessages) {
            this.totalSessions = totalSessions;
            this.authenticatedSessions = authenticatedSessions;
            this.uniqueUsers = uniqueUsers;
            this.activeChannels = activeChannels;
            this.queuedMessages = queuedMessages;
        }

        @Override
        public String toString() {
            return String.format("SessionStats{sessions=%d, authenticated=%d, users=%d, channels=%d, queued=%d}",
                    totalSessions, authenticatedSessions, uniqueUsers, activeChannels, queuedMessages);
        }
    }
}
