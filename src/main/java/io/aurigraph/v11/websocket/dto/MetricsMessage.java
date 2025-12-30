package io.aurigraph.v11.websocket.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

/**
 * Real-time metrics data broadcast via WebSocket
 * Sent every 1 second to connected clients
 */
public record MetricsMessage(
    @JsonProperty("timestamp")
    Instant timestamp,

    @JsonProperty("tps")
    long tps,

    @JsonProperty("cpu")
    double cpu,

    @JsonProperty("memory")
    long memory,

    @JsonProperty("connections")
    int connections,

    @JsonProperty("errorRate")
    double errorRate
) {
    public MetricsMessage {
        if (timestamp == null) {
            timestamp = Instant.now();
        }
    }

    /**
     * Create a sample metrics message for testing
     */
    public static MetricsMessage sample() {
        return new MetricsMessage(
            Instant.now(),
            8510000,
            45.2,
            2048,
            256,
            0.001
        );
    }
}
