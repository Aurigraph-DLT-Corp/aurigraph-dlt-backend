package io.aurigraph.v11.demo.models;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Represents a message within a channel in the Aurigraph V11 network.
 *
 * <p>Channel messages support various content types and delivery modes:
 * <ul>
 *   <li>Direct messages to specific participants</li>
 *   <li>Broadcast messages to all channel participants</li>
 *   <li>On-chain vs off-chain message routing</li>
 *   <li>Message ordering and sequencing</li>
 * </ul>
 * </p>
 *
 * <p>Performance characteristics:
 * <ul>
 *   <li>Message routing: &lt;5ms latency target</li>
 *   <li>Throughput: 500K messages/sec per channel node</li>
 *   <li>Message size: Up to 1MB per message</li>
 * </ul>
 * </p>
 *
 * @author Backend Development Agent (BDA)
 * @version 11.0.0
 * @since 2025-10-11
 */
public class ChannelMessage {

    private String messageId;
    private String channelId;
    private String senderId;
    private String recipientId; // null for broadcast messages
    private MessageType type;
    private byte[] payload;
    private Map<String, String> metadata;
    private Instant timestamp;
    private long sequenceNumber;
    private boolean onChain;
    private String signature;
    private MessageStatus status;

    /**
     * Default constructor for deserialization.
     */
    public ChannelMessage() {
        this.metadata = new HashMap<>();
        this.timestamp = Instant.now();
        this.onChain = false;
        this.status = MessageStatus.PENDING;
    }

    /**
     * Creates a new channel message.
     *
     * @param messageId The unique message identifier
     * @param channelId The channel this message belongs to
     * @param senderId The sender's participant ID
     * @param type The message type
     * @param payload The message payload
     */
    public ChannelMessage(String messageId, String channelId, String senderId,
                          MessageType type, byte[] payload) {
        this();
        this.messageId = messageId;
        this.channelId = channelId;
        this.senderId = senderId;
        this.type = type;
        this.payload = payload;
    }

    // Getters and Setters

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getChannelId() {
        return channelId;
    }

    public void setChannelId(String channelId) {
        this.channelId = channelId;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getRecipientId() {
        return recipientId;
    }

    public void setRecipientId(String recipientId) {
        this.recipientId = recipientId;
    }

    public MessageType getType() {
        return type;
    }

    public void setType(MessageType type) {
        this.type = type;
    }

    public byte[] getPayload() {
        return payload;
    }

    public void setPayload(byte[] payload) {
        this.payload = payload;
    }

    public Map<String, String> getMetadata() {
        return new HashMap<>(metadata);
    }

    public void setMetadata(Map<String, String> metadata) {
        this.metadata = new HashMap<>(metadata);
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public long getSequenceNumber() {
        return sequenceNumber;
    }

    public void setSequenceNumber(long sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }

    public boolean isOnChain() {
        return onChain;
    }

    public void setOnChain(boolean onChain) {
        this.onChain = onChain;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public MessageStatus getStatus() {
        return status;
    }

    public void setStatus(MessageStatus status) {
        this.status = status;
    }

    // Business methods

    /**
     * Checks if this is a broadcast message.
     *
     * @return true if the message is addressed to all participants
     */
    public boolean isBroadcast() {
        return recipientId == null;
    }

    /**
     * Gets the message payload size in bytes.
     *
     * @return The payload size
     */
    public long getPayloadSize() {
        return payload != null ? payload.length : 0;
    }

    /**
     * Adds metadata to the message.
     *
     * @param key The metadata key
     * @param value The metadata value
     */
    public void addMetadata(String key, String value) {
        this.metadata.put(key, value);
    }

    /**
     * Gets a metadata value by key.
     *
     * @param key The metadata key
     * @return The metadata value, or null if not present
     */
    public String getMetadataValue(String key) {
        return this.metadata.get(key);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChannelMessage that = (ChannelMessage) o;
        return Objects.equals(messageId, that.messageId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(messageId);
    }

    @Override
    public String toString() {
        return "ChannelMessage{" +
                "messageId='" + messageId + '\'' +
                ", channelId='" + channelId + '\'' +
                ", senderId='" + senderId + '\'' +
                ", recipientId='" + recipientId + '\'' +
                ", type=" + type +
                ", payloadSize=" + getPayloadSize() +
                ", sequenceNumber=" + sequenceNumber +
                ", timestamp=" + timestamp +
                ", status=" + status +
                '}';
    }

    /**
     * Enumeration of message types.
     */
    public enum MessageType {
        DATA,           // Regular data message
        CONTROL,        // Control/administrative message
        TRANSACTION,    // Transaction message
        EVENT,          // Event notification
        QUERY,          // Query request
        RESPONSE        // Query response
    }

    /**
     * Enumeration of message delivery statuses.
     */
    public enum MessageStatus {
        PENDING,        // Message created but not yet sent
        SENT,           // Message sent to channel
        DELIVERED,      // Message delivered to recipient(s)
        CONFIRMED,      // Message confirmed on-chain (if applicable)
        FAILED          // Message delivery failed
    }
}
