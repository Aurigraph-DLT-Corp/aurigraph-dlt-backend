package io.aurigraph.v11.websocket;

import io.aurigraph.v11.websocket.dto.TransactionMessage;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.websocket.*;
import jakarta.websocket.server.ServerEndpoint;
import org.jboss.logging.Logger;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket endpoint for real-time transaction streaming (LEGACY - NO AUTH)
 * Broadcasts new transactions as they occur
 *
 * NOTE: This is the legacy endpoint without authentication.
 * For secure WebSocket with JWT auth, use EnhancedTransactionWebSocket at /ws/transactions
 *
 * Endpoint: /ws/transactions/legacy
 * Message format: TransactionMessage (JSON)
 * Broadcast trigger: New transaction event
 *
 * @deprecated Use EnhancedTransactionWebSocket for authenticated connections
 */
@Deprecated
@ServerEndpoint("/ws/transactions/legacy")
@ApplicationScoped
public class TransactionWebSocket {

    private static final Logger LOG = Logger.getLogger(TransactionWebSocket.class);
    private static final Set<Session> sessions = ConcurrentHashMap.newKeySet();

    @OnOpen
    public void onOpen(Session session) {
        sessions.add(session);
        LOG.infof("Transaction WebSocket connected: %s (Total: %d)", session.getId(), sessions.size());
    }

    @OnClose
    public void onClose(Session session) {
        sessions.remove(session);
        LOG.infof("Transaction WebSocket disconnected: %s (Total: %d)", session.getId(), sessions.size());
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        LOG.errorf(throwable, "Transaction WebSocket error on session %s", session.getId());
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
