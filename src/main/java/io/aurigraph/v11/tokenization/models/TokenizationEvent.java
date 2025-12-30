package io.aurigraph.v11.tokenization.models;

/**
 * Tokenization Event Model
 *
 * Represents a real-time tokenization event for SSE streaming
 *
 * @version 11.3.0
 * @author Backend Development Agent (BDA)
 */
public class TokenizationEvent {

    public String eventType;  // "transaction_created", "heartbeat", "error"
    public String transactionId;
    public String channel;
    public String dataHash;
    public long size;
    public String status;
    public String timestamp;  // ISO format

    public TokenizationEvent() {
        // Default constructor for Jackson/JSON deserialization
    }

    public TokenizationEvent(String eventType, String transactionId,
                           String channel, String dataHash,
                           long size, String status, String timestamp) {
        this.eventType = eventType;
        this.transactionId = transactionId;
        this.channel = channel;
        this.dataHash = dataHash;
        this.size = size;
        this.status = status;
        this.timestamp = timestamp;
    }
}
