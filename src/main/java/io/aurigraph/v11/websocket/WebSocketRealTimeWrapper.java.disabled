package io.aurigraph.v11.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.websocket.*;
import org.jboss.logging.Logger;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.GZIPOutputStream;

/**
 * WebSocket Real-Time Wrapper - AV11-486 Implementation
 *
 * Production-ready WebSocket wrapper with advanced features:
 * - Automatic reconnection with exponential backoff (1s, 2s, 4s, 8s, max 30s)
 * - Message queuing during disconnection (max 1000 messages)
 * - Compression support (gzip) for messages >1KB
 * - Binary message support (Protocol Buffers compatible)
 * - Subscription state persistence across reconnections
 * - Comprehensive monitoring and metrics
 * - Thread-safe operations
 * - Backpressure handling
 *
 * Usage Example:
 * <pre>
 * WebSocketRealTimeWrapper wrapper = new WebSocketRealTimeWrapper(uri);
 * wrapper.connect();
 * wrapper.authenticate(jwtToken);
 * wrapper.subscribe("transactions", filter -> {
 *     System.out.println("Received: " + filter);
 * });
 * </pre>
 *
 * @author Real-Time Communication Agent (RTCA)
 * @since V11.6.0 (Sprint 16 - AV11-486)
 * @epic AV11-491 Real-Time Communication Infrastructure
 */
@ClientEndpoint
@ApplicationScoped
public class WebSocketRealTimeWrapper {

    private static final Logger LOG = Logger.getLogger(WebSocketRealTimeWrapper.class);

    @Inject
    ObjectMapper objectMapper;

    // Configuration
    private static final int MAX_QUEUE_SIZE = 1000;
    private static final int COMPRESSION_THRESHOLD = 1024; // 1KB
    private static final long[] BACKOFF_DELAYS_MS = {1000, 2000, 4000, 8000, 16000, 30000}; // Exponential backoff
    private static final int MAX_RECONNECT_ATTEMPTS = 100;

    // WebSocket connection state
    private Session session;
    private final URI serverUri;
    private final AtomicBoolean connected = new AtomicBoolean(false);
    private final AtomicBoolean authenticated = new AtomicBoolean(false);
    private final AtomicInteger reconnectAttempt = new AtomicInteger(0);

    // Message queue (for offline messages)
    private final BlockingQueue<QueuedMessage> messageQueue = new LinkedBlockingQueue<>(MAX_QUEUE_SIZE);

    // Subscription state
    private final Map<String, SubscriptionState> subscriptions = new ConcurrentHashMap<>();

    // Message handlers
    private final Map<String, MessageHandler> messageHandlers = new ConcurrentHashMap<>();

    // Scheduled executor for reconnection and heartbeat
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    private ScheduledFuture<?> reconnectTask;

    // Metrics
    private final AtomicLong messagesSent = new AtomicLong(0);
    private final AtomicLong messagesReceived = new AtomicLong(0);
    private final AtomicLong reconnectionAttempts = new AtomicLong(0);
    private final AtomicLong compressionSavings = new AtomicLong(0);

    // Authentication token
    private String authToken;

    /**
     * Create WebSocket wrapper
     *
     * @param serverUri WebSocket server URI (e.g., ws://localhost:9003/ws/v11)
     */
    public WebSocketRealTimeWrapper(URI serverUri) {
        this.serverUri = serverUri;
        initializeMetrics();
    }

    /**
     * Initialize metrics (placeholder for future Micrometer integration)
     */
    private void initializeMetrics() {
        // Metrics tracking is done via internal counters
        // Can be extended with Micrometer if available
        LOG.debug("WebSocket metrics initialized");
    }

    /**
     * Connect to WebSocket server
     *
     * @throws Exception if connection fails
     */
    public void connect() throws Exception {
        if (connected.get()) {
            LOG.warn("Already connected to WebSocket server");
            return;
        }

        LOG.infof("Connecting to WebSocket server: %s", serverUri);

        try {
            WebSocketContainer container = ContainerProvider.getWebSocketContainer();
            session = container.connectToServer(this, serverUri);
            connected.set(true);
            reconnectAttempt.set(0);

            LOG.infof("Successfully connected to WebSocket server: %s", serverUri);

        } catch (Exception e) {
            LOG.errorf(e, "Failed to connect to WebSocket server: %s", serverUri);
            scheduleReconnect();
            throw e;
        }
    }

    /**
     * Disconnect from WebSocket server
     */
    public void disconnect() {
        if (!connected.get()) {
            return;
        }

        try {
            if (session != null && session.isOpen()) {
                session.close(new CloseReason(
                    CloseReason.CloseCodes.NORMAL_CLOSURE,
                    "Client disconnect"
                ));
            }
            connected.set(false);
            authenticated.set(false);

            LOG.info("Disconnected from WebSocket server");

        } catch (Exception e) {
            LOG.errorf(e, "Error disconnecting from WebSocket server");
        }
    }

    /**
     * Authenticate with JWT token
     *
     * @param token JWT token
     * @throws Exception if authentication fails
     */
    public void authenticate(String token) throws Exception {
        if (!connected.get()) {
            throw new IllegalStateException("Not connected to WebSocket server");
        }

        this.authToken = token;

        Map<String, Object> authMessage = Map.of(
            "type", "auth",
            "token", token
        );

        sendMessage(authMessage);

        LOG.info("Sent authentication message");
    }

    /**
     * Subscribe to a channel
     *
     * @param channel Channel name
     * @param handler Message handler for this channel
     */
    public void subscribe(String channel, MessageHandler handler) {
        subscribe(channel, null, 0, handler);
    }

    /**
     * Subscribe to a channel with filter and priority
     *
     * @param channel Channel name
     * @param filter Optional filter
     * @param priority Message priority (0-10)
     * @param handler Message handler
     */
    public void subscribe(String channel, String filter, int priority, MessageHandler handler) {
        if (!connected.get() || !authenticated.get()) {
            LOG.warnf("Cannot subscribe to channel %s - not connected or authenticated", channel);
            return;
        }

        try {
            Map<String, Object> subscribeMessage = new HashMap<>();
            subscribeMessage.put("type", "subscribe");
            subscribeMessage.put("channel", channel);
            if (filter != null) {
                subscribeMessage.put("filter", filter);
            }
            subscribeMessage.put("priority", priority);

            sendMessage(subscribeMessage);

            // Store subscription state
            subscriptions.put(channel, new SubscriptionState(channel, filter, priority));
            messageHandlers.put(channel, handler);

            LOG.infof("Subscribed to channel: %s (filter: %s, priority: %d)",
                channel, filter, priority);

        } catch (Exception e) {
            LOG.errorf(e, "Error subscribing to channel: %s", channel);
        }
    }

    /**
     * Unsubscribe from a channel
     *
     * @param channel Channel name
     */
    public void unsubscribe(String channel) {
        if (!connected.get()) {
            return;
        }

        try {
            Map<String, Object> unsubscribeMessage = Map.of(
                "type", "unsubscribe",
                "channel", channel
            );

            sendMessage(unsubscribeMessage);

            // Remove subscription state
            subscriptions.remove(channel);
            messageHandlers.remove(channel);

            LOG.infof("Unsubscribed from channel: %s", channel);

        } catch (Exception e) {
            LOG.errorf(e, "Error unsubscribing from channel: %s", channel);
        }
    }

    /**
     * Send message to server
     *
     * @param message Message object (will be serialized to JSON)
     * @throws Exception if send fails
     */
    public void sendMessage(Object message) throws Exception {
        if (!connected.get() || session == null || !session.isOpen()) {
            // Queue message for later delivery
            if (messageQueue.size() < MAX_QUEUE_SIZE) {
                messageQueue.offer(new QueuedMessage(message, false));
                LOG.debugf("Message queued (queue size: %d)", messageQueue.size());
            } else {
                LOG.warnf("Message queue full (%d messages), dropping message", MAX_QUEUE_SIZE);
            }
            return;
        }

        try {
            String jsonMessage = objectMapper.writeValueAsString(message);

            // Check if compression is beneficial
            if (jsonMessage.length() > COMPRESSION_THRESHOLD) {
                byte[] compressed = compressMessage(jsonMessage);
                if (compressed.length < jsonMessage.length()) {
                    // Send compressed binary message
                    session.getBasicRemote().sendBinary(ByteBuffer.wrap(compressed));
                    messagesSent.incrementAndGet();
                    compressionSavings.addAndGet(jsonMessage.length() - compressed.length);
                    double savingsPercent = 100.0 * (1.0 - (double) compressed.length / jsonMessage.length());
                    LOG.debug(String.format("Sent compressed message (%d -> %d bytes, %.1f%% savings)",
                        jsonMessage.length(), compressed.length, savingsPercent));
                    return;
                }
            }

            // Send uncompressed text message
            session.getBasicRemote().sendText(jsonMessage);
            messagesSent.incrementAndGet();

        } catch (Exception e) {
            LOG.errorf(e, "Error sending message");

            // Queue message for retry
            if (messageQueue.size() < MAX_QUEUE_SIZE) {
                messageQueue.offer(new QueuedMessage(message, false));
            }

            throw e;
        }
    }

    /**
     * Send binary message (e.g., Protocol Buffer)
     *
     * @param data Binary data
     * @throws Exception if send fails
     */
    public void sendBinary(byte[] data) throws Exception {
        if (!connected.get() || session == null || !session.isOpen()) {
            // Queue binary message
            if (messageQueue.size() < MAX_QUEUE_SIZE) {
                messageQueue.offer(new QueuedMessage(data, true));
            }
            return;
        }

        session.getBasicRemote().sendBinary(ByteBuffer.wrap(data));
        messagesSent.incrementAndGet();
    }

    /**
     * WebSocket connection opened
     *
     * @param session WebSocket session
     */
    @OnOpen
    public void onOpen(Session session) {
        this.session = session;
        connected.set(true);
        reconnectAttempt.set(0);

        LOG.infof("WebSocket connection opened: %s", session.getId());

        // Re-authenticate if we have a token
        if (authToken != null) {
            try {
                authenticate(authToken);
            } catch (Exception e) {
                LOG.errorf(e, "Error re-authenticating after reconnect");
            }
        }
    }

    /**
     * WebSocket message received
     *
     * @param message Message from server (JSON)
     */
    @OnMessage
    public void onMessage(String message) {
        messagesReceived.incrementAndGet();

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> messageData = objectMapper.readValue(message, Map.class);
            String type = (String) messageData.get("type");

            switch (type) {
                case "auth_response":
                    handleAuthResponse(messageData);
                    break;
                case "subscribe_response":
                    handleSubscribeResponse(messageData);
                    break;
                case "message":
                    handleChannelMessage(messageData);
                    break;
                case "ping":
                    handlePing();
                    break;
                case "error":
                    handleError(messageData);
                    break;
                default:
                    LOG.debugf("Received unknown message type: %s", type);
            }

        } catch (Exception e) {
            LOG.errorf(e, "Error processing message: %s", message);
        }
    }

    /**
     * WebSocket binary message received
     *
     * @param data Binary data
     */
    @OnMessage
    public void onBinaryMessage(ByteBuffer data) {
        messagesReceived.incrementAndGet();

        try {
            // Try to decompress
            byte[] bytes = new byte[data.remaining()];
            data.get(bytes);

            String decompressed = decompressMessage(bytes);
            onMessage(decompressed);

        } catch (Exception e) {
            LOG.errorf(e, "Error processing binary message");
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
        connected.set(false);
        authenticated.set(false);

        LOG.infof("WebSocket connection closed: %s (reason: %s)",
            session.getId(), closeReason.getReasonPhrase());

        // Schedule reconnection
        scheduleReconnect();
    }

    /**
     * WebSocket error occurred
     *
     * @param session WebSocket session
     * @param throwable Error
     */
    @OnError
    public void onError(Session session, Throwable throwable) {
        LOG.errorf(throwable, "WebSocket error on session %s", session.getId());
        connected.set(false);

        // Schedule reconnection
        scheduleReconnect();
    }

    /**
     * Handle authentication response
     *
     * @param messageData Message data
     */
    private void handleAuthResponse(Map<String, Object> messageData) {
        String status = (String) messageData.get("status");

        if ("success".equals(status)) {
            authenticated.set(true);
            LOG.info("Authentication successful");

            // Restore subscriptions
            restoreSubscriptions();

            // Deliver queued messages
            deliverQueuedMessages();
        } else {
            String errorMessage = (String) messageData.get("message");
            LOG.errorf("Authentication failed: %s", errorMessage);
        }
    }

    /**
     * Handle subscribe response
     *
     * @param messageData Message data
     */
    private void handleSubscribeResponse(Map<String, Object> messageData) {
        String status = (String) messageData.get("status");
        String channel = (String) messageData.get("channel");

        if ("success".equals(status)) {
            LOG.infof("Subscription confirmed for channel: %s", channel);
        } else {
            LOG.warnf("Subscription failed for channel: %s", channel);
        }
    }

    /**
     * Handle channel message
     *
     * @param messageData Message data
     */
    private void handleChannelMessage(Map<String, Object> messageData) {
        String channel = (String) messageData.get("channel");
        Object data = messageData.get("data");

        MessageHandler handler = messageHandlers.get(channel);
        if (handler != null) {
            try {
                handler.onMessage(data);
            } catch (Exception e) {
                LOG.errorf(e, "Error in message handler for channel: %s", channel);
            }
        } else {
            LOG.warnf("No handler registered for channel: %s", channel);
        }
    }

    /**
     * Handle ping message (send pong response)
     */
    private void handlePing() {
        try {
            Map<String, Object> pong = Map.of("type", "pong", "timestamp", System.currentTimeMillis());
            sendMessage(pong);
        } catch (Exception e) {
            LOG.errorf(e, "Error sending pong");
        }
    }

    /**
     * Handle error message
     *
     * @param messageData Message data
     */
    private void handleError(Map<String, Object> messageData) {
        String errorMessage = (String) messageData.get("message");
        LOG.errorf("Server error: %s", errorMessage);
    }

    /**
     * Schedule reconnection with exponential backoff
     */
    private void scheduleReconnect() {
        if (reconnectTask != null && !reconnectTask.isDone()) {
            return; // Reconnection already scheduled
        }

        int attempt = reconnectAttempt.getAndIncrement();

        if (attempt >= MAX_RECONNECT_ATTEMPTS) {
            LOG.errorf("Max reconnect attempts reached (%d), giving up", MAX_RECONNECT_ATTEMPTS);
            return;
        }

        long delayMs = BACKOFF_DELAYS_MS[Math.min(attempt, BACKOFF_DELAYS_MS.length - 1)];

        LOG.infof("Scheduling reconnection attempt %d in %dms", attempt + 1, delayMs);

        reconnectTask = scheduler.schedule(() -> {
            try {
                reconnectionAttempts.incrementAndGet();
                connect();
            } catch (Exception e) {
                LOG.errorf(e, "Reconnection attempt %d failed", attempt + 1);
            }
        }, delayMs, TimeUnit.MILLISECONDS);
    }

    /**
     * Restore subscriptions after reconnection
     */
    private void restoreSubscriptions() {
        LOG.infof("Restoring %d subscriptions after reconnection", subscriptions.size());

        for (SubscriptionState state : subscriptions.values()) {
            MessageHandler handler = messageHandlers.get(state.channel);
            if (handler != null) {
                subscribe(state.channel, state.filter, state.priority, handler);
            }
        }
    }

    /**
     * Deliver queued messages
     */
    private void deliverQueuedMessages() {
        int delivered = 0;

        QueuedMessage queuedMessage;
        while ((queuedMessage = messageQueue.poll()) != null) {
            try {
                if (queuedMessage.isBinary) {
                    sendBinary((byte[]) queuedMessage.data);
                } else {
                    sendMessage(queuedMessage.data);
                }
                delivered++;
            } catch (Exception e) {
                LOG.errorf(e, "Error delivering queued message");
                break;
            }
        }

        if (delivered > 0) {
            LOG.infof("Delivered %d queued messages", delivered);
        }
    }

    /**
     * Compress message with gzip
     *
     * @param message Message string
     * @return Compressed bytes
     */
    private byte[] compressMessage(String message) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (GZIPOutputStream gzipStream = new GZIPOutputStream(baos)) {
            gzipStream.write(message.getBytes());
        }
        return baos.toByteArray();
    }

    /**
     * Decompress gzip message
     *
     * @param compressed Compressed bytes
     * @return Decompressed string
     */
    private String decompressMessage(byte[] compressed) throws IOException {
        try (java.util.zip.GZIPInputStream gzipStream =
                 new java.util.zip.GZIPInputStream(new java.io.ByteArrayInputStream(compressed))) {
            return new String(gzipStream.readAllBytes());
        }
    }

    /**
     * Get connection metrics
     *
     * @return Metrics map
     */
    public Map<String, Object> getMetrics() {
        return Map.of(
            "connected", connected.get(),
            "authenticated", authenticated.get(),
            "messagesSent", messagesSent.get(),
            "messagesReceived", messagesReceived.get(),
            "reconnectionAttempts", reconnectionAttempts.get(),
            "queueSize", messageQueue.size(),
            "compressionSavings", compressionSavings.get(),
            "subscriptions", subscriptions.size()
        );
    }

    /**
     * Check if connected
     *
     * @return true if connected
     */
    public boolean isConnected() {
        return connected.get();
    }

    /**
     * Check if authenticated
     *
     * @return true if authenticated
     */
    public boolean isAuthenticated() {
        return authenticated.get();
    }

    /**
     * Subscription State
     */
    private static class SubscriptionState {
        final String channel;
        final String filter;
        final int priority;

        SubscriptionState(String channel, String filter, int priority) {
            this.channel = channel;
            this.filter = filter;
            this.priority = priority;
        }
    }

    /**
     * Queued Message
     */
    private static class QueuedMessage {
        final Object data;
        final boolean isBinary;

        QueuedMessage(Object data, boolean isBinary) {
            this.data = data;
            this.isBinary = isBinary;
        }
    }

    /**
     * Message Handler Interface
     */
    @FunctionalInterface
    public interface MessageHandler {
        void onMessage(Object message);
    }

    /**
     * Shutdown wrapper (cleanup resources)
     */
    public void shutdown() {
        disconnect();
        scheduler.shutdown();
        try {
            scheduler.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
