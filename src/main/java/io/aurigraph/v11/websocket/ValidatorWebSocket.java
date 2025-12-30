package io.aurigraph.v11.websocket;

import io.aurigraph.v11.websocket.dto.ValidatorMessage;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.websocket.*;
import jakarta.websocket.server.ServerEndpoint;
import org.jboss.logging.Logger;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket endpoint for real-time validator status streaming
 * Broadcasts validator state changes
 *
 * Endpoint: /ws/validators
 * Message format: ValidatorMessage (JSON)
 * Broadcast trigger: Validator state change
 */
@ServerEndpoint("/ws/validators")
@ApplicationScoped
public class ValidatorWebSocket {

    private static final Logger LOG = Logger.getLogger(ValidatorWebSocket.class);
    private static final Set<Session> sessions = ConcurrentHashMap.newKeySet();

    @OnOpen
    public void onOpen(Session session) {
        sessions.add(session);
        LOG.infof("Validator WebSocket connected: %s (Total: %d)", session.getId(), sessions.size());
    }

    @OnClose
    public void onClose(Session session) {
        sessions.remove(session);
        LOG.infof("Validator WebSocket disconnected: %s (Total: %d)", session.getId(), sessions.size());
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        LOG.errorf(throwable, "Validator WebSocket error on session %s", session.getId());
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
