package io.aurigraph.v11.websocket.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

/**
 * Real-time validator status broadcast via WebSocket
 * Sent on validator state changes
 */
public record ValidatorMessage(
    @JsonProperty("timestamp")
    Instant timestamp,

    @JsonProperty("validator")
    String validator,

    @JsonProperty("status")
    String status,

    @JsonProperty("votingPower")
    long votingPower,

    @JsonProperty("uptime")
    double uptime,

    @JsonProperty("lastBlockProposed")
    long lastBlockProposed
) {
    public ValidatorMessage {
        if (timestamp == null) {
            timestamp = Instant.now();
        }
    }

    /**
     * Create a sample validator message for testing
     */
    public static ValidatorMessage sample() {
        return new ValidatorMessage(
            Instant.now(),
            "0xvalidator1234567890",
            "ACTIVE",
            1000000,
            99.95,
            12345
        );
    }
}
