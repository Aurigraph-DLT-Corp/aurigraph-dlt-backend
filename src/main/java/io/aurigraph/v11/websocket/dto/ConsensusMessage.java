package io.aurigraph.v11.websocket.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

/**
 * Real-time consensus state broadcast via WebSocket
 * Sent on consensus state changes
 */
public record ConsensusMessage(
    @JsonProperty("timestamp")
    Instant timestamp,

    @JsonProperty("leader")
    String leader,

    @JsonProperty("epoch")
    long epoch,

    @JsonProperty("round")
    long round,

    @JsonProperty("term")
    long term,

    @JsonProperty("state")
    String state,

    @JsonProperty("performanceScore")
    double performanceScore,

    @JsonProperty("activeValidators")
    int activeValidators
) {
    public ConsensusMessage {
        if (timestamp == null) {
            timestamp = Instant.now();
        }
    }

    /**
     * Create a sample consensus message for testing
     */
    public static ConsensusMessage sample() {
        return new ConsensusMessage(
            Instant.now(),
            "0xleader1234567890",
            145,
            3,
            7,
            "COMMITTED",
            0.98,
            156
        );
    }
}
