package io.aurigraph.v11.health;

import io.aurigraph.v11.websocket.MetricsWebSocket;
import io.aurigraph.v11.websocket.TransactionWebSocket;
import io.aurigraph.v11.websocket.ValidatorWebSocket;
import io.aurigraph.v11.websocket.ConsensusWebSocket;
import io.aurigraph.v11.websocket.NetworkWebSocket;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Liveness;
import org.jboss.logging.Logger;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * WebSocket Service Health Check
 * Verifies WebSocket connectivity and connection limits
 *
 * Liveness Check: Service should restart if WebSocket system fails
 *
 * @author Aurigraph Production Readiness Agent
 * @version 11.0.0
 */
@Liveness
@ApplicationScoped
public class WebSocketHealthCheck implements HealthCheck {

    private static final Logger LOG = Logger.getLogger(WebSocketHealthCheck.class);

    @ConfigProperty(name = "websocket.health.check.enabled", defaultValue = "true")
    boolean healthCheckEnabled;

    @ConfigProperty(name = "websocket.max.connections.warning", defaultValue = "8000")
    int maxConnectionsWarning;

    @Override
    public HealthCheckResponse call() {
        if (!healthCheckEnabled) {
            return HealthCheckResponse.named("websocket-service")
                .status(true)
                .withData("status", "health-check-disabled")
                .build();
        }

        try {
            int metricsConnections = MetricsWebSocket.getConnectionCount();
            int transactionConnections = TransactionWebSocket.getConnectionCount();
            int validatorConnections = ValidatorWebSocket.getConnectionCount();
            int consensusConnections = ConsensusWebSocket.getConnectionCount();
            int networkConnections = NetworkWebSocket.getConnectionCount();

            int totalConnections = metricsConnections + transactionConnections +
                validatorConnections + consensusConnections + networkConnections;

            boolean healthy = totalConnections < maxConnectionsWarning;

            var builder = HealthCheckResponse.named("websocket-service")
                .status(healthy)
                .withData("total_connections", totalConnections)
                .withData("metrics_ws", metricsConnections)
                .withData("transaction_ws", transactionConnections)
                .withData("validator_ws", validatorConnections)
                .withData("consensus_ws", consensusConnections)
                .withData("network_ws", networkConnections)
                .withData("max_warning_threshold", maxConnectionsWarning);

            if (!healthy) {
                builder.withData("message",
                    String.format("Connection count approaching limit: %d/%d",
                        totalConnections, maxConnectionsWarning));
                LOG.warnf("WebSocket connection count high: %d (warning threshold: %d)",
                    totalConnections, maxConnectionsWarning);
            }

            return builder.build();

        } catch (Exception e) {
            LOG.errorf(e, "WebSocket health check failed due to exception");
            return HealthCheckResponse.named("websocket-service")
                .status(false)
                .withData("error", e.getMessage())
                .withData("message", "Failed to retrieve WebSocket connection status")
                .build();
        }
    }
}
