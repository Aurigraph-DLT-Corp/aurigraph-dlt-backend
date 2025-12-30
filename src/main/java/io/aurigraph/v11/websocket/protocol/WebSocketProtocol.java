package io.aurigraph.v11.websocket.protocol;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.Map;

/**
 * WebSocket Protocol Definition
 *
 * Standardized message format for WebSocket communication:
 * - Type-safe message structure
 * - Command/response patterns
 * - Error codes and handling
 * - Heartbeat protocol
 *
 * Message Flow:
 * 1. Client connects with authentication token
 * 2. Server sends CONNECTED message
 * 3. Client subscribes to channels
 * 4. Server confirms with ACK
 * 5. Bidirectional message exchange
 * 6. Heartbeat messages every 30s
 * 7. Graceful disconnect
 *
 * @author WebSocket Development Agent (WDA)
 * @since V11.6.0 (Sprint 16 - AV11-486)
 */
public class WebSocketProtocol {

    /**
     * Message Types
     */
    public enum MessageType {
        // Connection lifecycle
        CONNECTED,
        DISCONNECTED,

        // Channel management
        SUBSCRIBE,
        UNSUBSCRIBE,
        SUBSCRIBED,
        UNSUBSCRIBED,

        // Data exchange
        MESSAGE,
        BROADCAST,

        // Acknowledgements
        ACK,
        NACK,

        // Heartbeat
        PING,
        PONG,
        HEARTBEAT,

        // Errors
        ERROR,
        WARNING
    }

    /**
     * Error Codes
     */
    public enum ErrorCode {
        // Connection errors (1xxx)
        CONNECTION_FAILED(1000, "Connection failed"),
        AUTHENTICATION_FAILED(1001, "Authentication failed"),
        NETWORK_TIMEOUT(1002, "Network timeout"),
        INVALID_TOKEN(1003, "Invalid authentication token"),

        // Message errors (2xxx)
        INVALID_MESSAGE(2000, "Invalid message format"),
        INVALID_CHANNEL(2001, "Invalid channel name"),
        MESSAGE_TOO_LARGE(2002, "Message exceeds size limit"),
        RATE_LIMIT_EXCEEDED(2003, "Rate limit exceeded"),

        // Server errors (3xxx)
        SERVER_ERROR(3000, "Internal server error"),
        SERVICE_UNAVAILABLE(3001, "Service temporarily unavailable"),
        CIRCUIT_BREAKER_OPEN(3002, "Circuit breaker is open"),

        // Channel errors (4xxx)
        CHANNEL_NOT_FOUND(4000, "Channel not found"),
        SUBSCRIPTION_FAILED(4001, "Failed to subscribe to channel"),
        UNSUBSCRIPTION_FAILED(4002, "Failed to unsubscribe from channel"),
        PERMISSION_DENIED(4003, "Permission denied for channel");

        private final int code;
        private final String message;

        ErrorCode(int code, String message) {
            this.code = code;
            this.message = message;
        }

        public int getCode() {
            return code;
        }

        public String getMessage() {
            return message;
        }
    }

    /**
     * Base WebSocket Message
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Message {
        @JsonProperty("type")
        private MessageType type;

        @JsonProperty("id")
        private String id;

        @JsonProperty("timestamp")
        private long timestamp;

        @JsonProperty("channel")
        private String channel;

        @JsonProperty("data")
        private Object data;

        @JsonProperty("error")
        private ErrorInfo error;

        @JsonProperty("metadata")
        private Map<String, Object> metadata;

        public Message() {
            this.timestamp = Instant.now().toEpochMilli();
        }

        public Message(MessageType type) {
            this();
            this.type = type;
        }

        public Message(MessageType type, String id) {
            this(type);
            this.id = id;
        }

        // Getters and setters
        public MessageType getType() {
            return type;
        }

        public void setType(MessageType type) {
            this.type = type;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(long timestamp) {
            this.timestamp = timestamp;
        }

        public String getChannel() {
            return channel;
        }

        public void setChannel(String channel) {
            this.channel = channel;
        }

        public Object getData() {
            return data;
        }

        public void setData(Object data) {
            this.data = data;
        }

        public ErrorInfo getError() {
            return error;
        }

        public void setError(ErrorInfo error) {
            this.error = error;
        }

        public Map<String, Object> getMetadata() {
            return metadata;
        }

        public void setMetadata(Map<String, Object> metadata) {
            this.metadata = metadata;
        }

        @Override
        public String toString() {
            return "Message{" +
                    "type=" + type +
                    ", id='" + id + '\'' +
                    ", channel='" + channel + '\'' +
                    ", timestamp=" + timestamp +
                    '}';
        }
    }

    /**
     * Error Information
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ErrorInfo {
        @JsonProperty("code")
        private int code;

        @JsonProperty("errorCode")
        private String errorCode;

        @JsonProperty("message")
        private String message;

        @JsonProperty("details")
        private Object details;

        public ErrorInfo() {
        }

        public ErrorInfo(ErrorCode errorCode) {
            this.code = errorCode.getCode();
            this.errorCode = errorCode.name();
            this.message = errorCode.getMessage();
        }

        public ErrorInfo(ErrorCode errorCode, String details) {
            this(errorCode);
            this.details = details;
        }

        // Getters and setters
        public int getCode() {
            return code;
        }

        public void setCode(int code) {
            this.code = code;
        }

        public String getErrorCode() {
            return errorCode;
        }

        public void setErrorCode(String errorCode) {
            this.errorCode = errorCode;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public Object getDetails() {
            return details;
        }

        public void setDetails(Object details) {
            this.details = details;
        }

        @Override
        public String toString() {
            return "ErrorInfo{" +
                    "code=" + code +
                    ", errorCode='" + errorCode + '\'' +
                    ", message='" + message + '\'' +
                    '}';
        }
    }

    /**
     * Subscription Request
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class SubscriptionRequest {
        @JsonProperty("channel")
        private String channel;

        @JsonProperty("filters")
        private Map<String, Object> filters;

        public SubscriptionRequest() {
        }

        public SubscriptionRequest(String channel) {
            this.channel = channel;
        }

        public String getChannel() {
            return channel;
        }

        public void setChannel(String channel) {
            this.channel = channel;
        }

        public Map<String, Object> getFilters() {
            return filters;
        }

        public void setFilters(Map<String, Object> filters) {
            this.filters = filters;
        }
    }

    /**
     * Subscription Response
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class SubscriptionResponse {
        @JsonProperty("channel")
        private String channel;

        @JsonProperty("success")
        private boolean success;

        @JsonProperty("subscriberCount")
        private int subscriberCount;

        public SubscriptionResponse() {
        }

        public SubscriptionResponse(String channel, boolean success, int subscriberCount) {
            this.channel = channel;
            this.success = success;
            this.subscriberCount = subscriberCount;
        }

        public String getChannel() {
            return channel;
        }

        public void setChannel(String channel) {
            this.channel = channel;
        }

        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public int getSubscriberCount() {
            return subscriberCount;
        }

        public void setSubscriberCount(int subscriberCount) {
            this.subscriberCount = subscriberCount;
        }
    }

    /**
     * Heartbeat Message
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class HeartbeatMessage {
        @JsonProperty("timestamp")
        private long timestamp;

        @JsonProperty("serverTime")
        private long serverTime;

        @JsonProperty("latency")
        private Long latency;

        public HeartbeatMessage() {
            this.timestamp = Instant.now().toEpochMilli();
            this.serverTime = this.timestamp;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(long timestamp) {
            this.timestamp = timestamp;
        }

        public long getServerTime() {
            return serverTime;
        }

        public void setServerTime(long serverTime) {
            this.serverTime = serverTime;
        }

        public Long getLatency() {
            return latency;
        }

        public void setLatency(Long latency) {
            this.latency = latency;
        }
    }

    /**
     * Connection Info
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ConnectionInfo {
        @JsonProperty("connectionId")
        private String connectionId;

        @JsonProperty("userId")
        private String userId;

        @JsonProperty("authenticated")
        private boolean authenticated;

        @JsonProperty("connectedAt")
        private long connectedAt;

        @JsonProperty("serverVersion")
        private String serverVersion;

        public ConnectionInfo() {
            this.connectedAt = Instant.now().toEpochMilli();
        }

        public ConnectionInfo(String connectionId, String userId, boolean authenticated) {
            this();
            this.connectionId = connectionId;
            this.userId = userId;
            this.authenticated = authenticated;
        }

        public String getConnectionId() {
            return connectionId;
        }

        public void setConnectionId(String connectionId) {
            this.connectionId = connectionId;
        }

        public String getUserId() {
            return userId;
        }

        public void setUserId(String userId) {
            this.userId = userId;
        }

        public boolean isAuthenticated() {
            return authenticated;
        }

        public void setAuthenticated(boolean authenticated) {
            this.authenticated = authenticated;
        }

        public long getConnectedAt() {
            return connectedAt;
        }

        public void setConnectedAt(long connectedAt) {
            this.connectedAt = connectedAt;
        }

        public String getServerVersion() {
            return serverVersion;
        }

        public void setServerVersion(String serverVersion) {
            this.serverVersion = serverVersion;
        }
    }

    /**
     * Message Builder
     */
    public static class MessageBuilder {
        private final Message message;

        private MessageBuilder(MessageType type) {
            this.message = new Message(type);
        }

        public static MessageBuilder create(MessageType type) {
            return new MessageBuilder(type);
        }

        public MessageBuilder id(String id) {
            message.setId(id);
            return this;
        }

        public MessageBuilder channel(String channel) {
            message.setChannel(channel);
            return this;
        }

        public MessageBuilder data(Object data) {
            message.setData(data);
            return this;
        }

        public MessageBuilder error(ErrorInfo error) {
            message.setError(error);
            return this;
        }

        public MessageBuilder error(ErrorCode errorCode) {
            message.setError(new ErrorInfo(errorCode));
            return this;
        }

        public MessageBuilder error(ErrorCode errorCode, String details) {
            message.setError(new ErrorInfo(errorCode, details));
            return this;
        }

        public MessageBuilder metadata(Map<String, Object> metadata) {
            message.setMetadata(metadata);
            return this;
        }

        public Message build() {
            return message;
        }
    }

    /**
     * Protocol Constants
     */
    public static class Constants {
        // Size limits
        public static final int MAX_MESSAGE_SIZE = 1024 * 1024; // 1 MB
        public static final int MAX_CHANNEL_NAME_LENGTH = 128;
        public static final int MAX_CHANNELS_PER_CLIENT = 100;

        // Timing
        public static final int HEARTBEAT_INTERVAL_MS = 30000; // 30 seconds
        public static final int HEARTBEAT_TIMEOUT_MS = 60000; // 60 seconds
        public static final int RECONNECT_DELAY_MS = 1000; // 1 second
        public static final int MAX_RECONNECT_DELAY_MS = 30000; // 30 seconds

        // Rate limiting
        public static final int MAX_MESSAGES_PER_SECOND = 100;
        public static final int MAX_SUBSCRIPTIONS_PER_SECOND = 10;

        // Channels
        public static final String CHANNEL_TRANSACTIONS = "transactions";
        public static final String CHANNEL_CONSENSUS = "consensus";
        public static final String CHANNEL_VALIDATORS = "validators";
        public static final String CHANNEL_NETWORK = "network";
        public static final String CHANNEL_METRICS = "metrics";
        public static final String CHANNEL_SYSTEM = "system";
    }
}
