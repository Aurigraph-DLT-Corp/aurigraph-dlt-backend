package io.aurigraph.v11.websocket.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

/**
 * Real-time transaction data broadcast via WebSocket
 * Sent on new transaction events
 */
public record TransactionMessage(
    @JsonProperty("timestamp")
    Instant timestamp,

    @JsonProperty("txHash")
    String txHash,

    @JsonProperty("from")
    String from,

    @JsonProperty("to")
    String to,

    @JsonProperty("value")
    String value,

    @JsonProperty("status")
    String status,

    @JsonProperty("gasUsed")
    long gasUsed
) {
    public TransactionMessage {
        if (timestamp == null) {
            timestamp = Instant.now();
        }
    }

    /**
     * Create a sample transaction message for testing
     */
    public static TransactionMessage sample() {
        return new TransactionMessage(
            Instant.now(),
            "0x1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef",
            "0xabcdef1234567890",
            "0x1234567890abcdef",
            "1000000000000000000",
            "PENDING",
            21000
        );
    }
}
