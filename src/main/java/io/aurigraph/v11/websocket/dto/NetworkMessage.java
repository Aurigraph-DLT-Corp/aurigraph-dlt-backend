package io.aurigraph.v11.websocket.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

/**
 * Real-time network topology broadcast via WebSocket
 * Sent on peer connection/disconnection events
 */
public record NetworkMessage(
    @JsonProperty("timestamp")
    Instant timestamp,

    @JsonProperty("peerId")
    String peerId,

    @JsonProperty("ip")
    String ip,

    @JsonProperty("connected")
    boolean connected,

    @JsonProperty("latency")
    int latency,

    @JsonProperty("version")
    String version
) {
    public NetworkMessage {
        if (timestamp == null) {
            timestamp = Instant.now();
        }
    }

    /**
     * Create a sample network message for testing
     */
    public static NetworkMessage sample() {
        return new NetworkMessage(
            Instant.now(),
            "peer-abc123def456",
            "192.168.1.100",
            true,
            25,
            "11.4.3"
        );
    }
}
