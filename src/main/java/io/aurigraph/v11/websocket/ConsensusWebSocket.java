package io.aurigraph.v11.websocket;

import io.aurigraph.v11.websocket.dto.ConsensusMessage;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.websocket.*;
import jakarta.websocket.server.ServerEndpoint;
import org.jboss.logging.Logger;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket endpoint for real-time consensus state streaming
 * Broadcasts consensus state changes
 *
 * Endpoint: /ws/consensus
 * Message format: ConsensusMessage (JSON)
 * Broadcast trigger: Consensus state change
 */
@ServerEndpoint("/ws/consensus")
@ApplicationScoped
public class ConsensusWebSocket {

    private static final Logger LOG = Logger.getLogger(ConsensusWebSocket.class);
    private static final Set<Session> sessions = ConcurrentHashMap.newKeySet();

    @OnOpen
    public void onOpen(Session session) {
        sessions.add(session);
        LOG.infof("Consensus WebSocket connected: %s (Total: %d)", session.getId(), sessions.size());
    }

    @OnClose
    public void onClose(Session session) {
        sessions.remove(session);
        LOG.infof("Consensus WebSocket disconnected: %s (Total: %d)", session.getId(), sessions.size());
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        LOG.errorf(throwable, "Consensus WebSocket error on session %s", session.getId());
        sessions.remove(session);
    }

    /**
     * Broadcast message to all connected clients
     */
    public static void broadcast(String message) {
        sessions.forEach(session -> {
            if (session.isOpen()) {
                try {
                    session.getAsyncRemote().sendText(message);
                } catch (Exception e) {
                    LOG.errorf(e, "Failed to broadcast to session %s", session.getId());
                }
            }
        });
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
}
