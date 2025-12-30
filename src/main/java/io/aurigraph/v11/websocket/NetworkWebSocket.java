package io.aurigraph.v11.websocket;

import io.aurigraph.v11.websocket.dto.NetworkMessage;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.websocket.*;
import jakarta.websocket.server.ServerEndpoint;
import org.jboss.logging.Logger;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket endpoint for real-time network topology streaming
 * Broadcasts peer connection/disconnection events
 *
 * Endpoint: /ws/network
 * Message format: NetworkMessage (JSON)
 * Broadcast trigger: Peer connection state change
 */
@ServerEndpoint("/ws/network")
@ApplicationScoped
public class NetworkWebSocket {

    private static final Logger LOG = Logger.getLogger(NetworkWebSocket.class);
    private static final Set<Session> sessions = ConcurrentHashMap.newKeySet();

    @OnOpen
    public void onOpen(Session session) {
        sessions.add(session);
        LOG.infof("Network WebSocket connected: %s (Total: %d)", session.getId(), sessions.size());
    }

    @OnClose
    public void onClose(Session session) {
        sessions.remove(session);
        LOG.infof("Network WebSocket disconnected: %s (Total: %d)", session.getId(), sessions.size());
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        LOG.errorf(throwable, "Network WebSocket error on session %s", session.getId());
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
