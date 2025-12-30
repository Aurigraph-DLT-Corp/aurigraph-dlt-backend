package io.aurigraph.v11.health;

import io.aurigraph.v11.services.OracleStatusService;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.HealthCheckResponseBuilder;
import org.eclipse.microprofile.health.Readiness;
import org.jboss.logging.Logger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Oracle Service Health Check
 * Verifies that sufficient oracles are available for verification operations
 *
 * Readiness Check: Service should not receive traffic if oracles unavailable
 *
 * @author Aurigraph Production Readiness Agent
 * @version 11.0.0
 */
@Readiness
@ApplicationScoped
public class OracleHealthCheck implements HealthCheck {

    private static final Logger LOG = Logger.getLogger(OracleHealthCheck.class);

    @Inject
    OracleStatusService oracleStatusService;

    @ConfigProperty(name = "oracle.verification.min.required", defaultValue = "3")
    int minOraclesRequired;

    @ConfigProperty(name = "oracle.health.check.enabled", defaultValue = "true")
    boolean healthCheckEnabled;

    @Override
    public HealthCheckResponse call() {
        if (!healthCheckEnabled) {
            return HealthCheckResponse.named("oracle-service")
                .status(true)
                .withData("status", "health-check-disabled")
                .build();
        }

        HealthCheckResponseBuilder builder = HealthCheckResponse.named("oracle-service");

        try {
            // Get oracle status synchronously for health check
            var oracleStatus = oracleStatusService.getOracleStatus()
                .await()
                .atMost(java.time.Duration.ofSeconds(5));

            long activeOracles = oracleStatus.getOracles().stream()
                .filter(oracle -> "active".equalsIgnoreCase(oracle.getStatus()))
                .count();

            boolean healthy = activeOracles >= minOraclesRequired;

            builder.status(healthy)
                .withData("active_oracles", activeOracles)
                .withData("minimum_required", minOraclesRequired)
                .withData("total_oracles", oracleStatus.getOracles().size());

            if (!healthy) {
                builder.withData("message",
                    String.format("Insufficient oracles: %d active, %d required",
                        activeOracles, minOraclesRequired));
                LOG.warnf("Oracle health check failed: %d active oracles, %d required",
                    activeOracles, minOraclesRequired);
            }

            return builder.build();

        } catch (Exception e) {
            LOG.errorf(e, "Oracle health check failed due to exception");
            return builder.status(false)
                .withData("error", e.getMessage())
                .withData("message", "Failed to retrieve oracle status")
                .build();
        }
    }
}
