package io.aurigraph.v11.websocket;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

/**
 * WebSocket Configuration
 * Externalized configuration for WebSocket services
 *
 * All WebSocket parameters are now configurable via application.properties
 * with appropriate defaults for development and production environments
 *
 * @author Aurigraph Production Readiness Agent
 * @version 11.0.0
 */
@ConfigMapping(prefix = "websocket")
public interface WebSocketConfiguration {

    /**
     * Maximum message queue size per WebSocket connection
     */
    @WithDefault("1000")
    int queueMaxSize();

    /**
     * Message processing timeout in milliseconds
     */
    @WithDefault("5000")
    long processingTimeoutMs();

    /**
     * Maximum concurrent connections per WebSocket endpoint
     */
    @WithDefault("10000")
    int maxConnectionsPerEndpoint();

    /**
     * Enable message compression for bandwidth optimization
     */
    @WithDefault("true")
    boolean compressionEnabled();

    /**
     * Heartbeat interval in seconds
     */
    @WithDefault("30")
    int heartbeatIntervalSeconds();

    /**
     * Maximum message size in bytes
     */
    @WithDefault("1048576")
    int maxMessageSizeBytes();

    /**
     * Enable automatic reconnection
     */
    @WithDefault("true")
    boolean autoReconnectEnabled();

    /**
     * Maximum reconnection attempts
     */
    @WithDefault("3")
    int maxReconnectAttempts();

    /**
     * Reconnection delay in milliseconds
     */
    @WithDefault("5000")
    long reconnectDelayMs();

    /**
     * Enable broadcast rate limiting
     */
    @WithDefault("true")
    boolean rateLimitingEnabled();

    /**
     * Maximum broadcasts per second per endpoint
     */
    @WithDefault("100")
    int maxBroadcastsPerSecond();

    /**
     * Target broadcast latency in milliseconds
     */
    @WithDefault("100")
    long targetLatencyMs();

    /**
     * Broadcast configuration
     */
    BroadcastConfig broadcast();

    interface BroadcastConfig {
        /**
         * Broadcast metrics interval
         */
        @WithDefault("1s")
        String metricsInterval();

        /**
         * Enable latency tracking
         */
        @WithDefault("true")
        boolean latencyTrackingEnabled();
    }
}
