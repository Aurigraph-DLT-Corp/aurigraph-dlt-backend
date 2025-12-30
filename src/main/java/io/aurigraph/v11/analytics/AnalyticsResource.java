package io.aurigraph.v11.analytics;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.jboss.logging.Logger;

/**
 * Analytics REST API Resource
 *
 * Provides RESTful endpoints for analytics dashboard and performance metrics.
 *
 * Endpoints:
 * - GET /api/v11/analytics/dashboard - Analytics Dashboard (AV11-270)
 * - GET /api/v11/analytics/performance - Performance Metrics (AV11-271)
 *
 * Story Points: 10 (5 + 5)
 * JIRA: AV11-270 (Analytics Dashboard API) + AV11-271 (Performance Metrics API)
 *
 * @author Backend Development Agent (BDA) - Analytics Specialist
 * @version 11.0.0
 * @since Sprint 9
 */
@Path("/api/v11/analytics")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AnalyticsResource {

    private static final Logger LOG = Logger.getLogger(AnalyticsResource.class);

    @Inject
    AnalyticsService analyticsService;

    /**
     * Get Analytics Dashboard
     *
     * Returns comprehensive analytics data including:
     * - TPS over time (last 24 hours)
     * - Transaction breakdown by type
     * - Top validators by performance
     * - Network utilization metrics
     * - Gas usage statistics
     * - Block time distribution
     *
     * AV11-270: Analytics Dashboard API (5 SP)
     *
     * @return AnalyticsDashboard with aggregated metrics
     */
    @GET
    @Path("/dashboard")
    public Uni<AnalyticsService.AnalyticsDashboard> getDashboard() {
        LOG.info("📊 Analytics Dashboard requested");

        return Uni.createFrom().item(() -> {
            try {
                AnalyticsService.AnalyticsDashboard dashboard = analyticsService.getDashboardAnalytics();

                LOG.infof("✅ Analytics Dashboard generated: %d total transactions, %.0f avg TPS, %d validators",
                        dashboard.totalTransactions(),
                        dashboard.avgTPS(),
                        dashboard.topValidators().size());

                return dashboard;
            } catch (Exception e) {
                LOG.errorf(e, "❌ Error generating analytics dashboard");
                throw new WebApplicationException(
                        "Failed to generate analytics dashboard: " + e.getMessage(),
                        500
                );
            }
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    /**
     * Get Performance Metrics
     *
     * Returns real-time system performance data including:
     * - Memory usage (total, used, free)
     * - CPU utilization percentage
     * - Disk I/O (read/write MB/s)
     * - Network I/O (inbound/outbound MB/s)
     * - Response time percentiles (p50, p95, p99)
     * - Current throughput (TPS)
     * - Error rate percentage
     * - System uptime in seconds
     *
     * AV11-271: Performance Metrics API (5 SP)
     *
     * @return PerformanceMetrics with system metrics
     */
    @GET
    @Path("/performance")
    public Uni<AnalyticsService.PerformanceMetrics> getPerformanceMetrics() {
        LOG.info("⚡ Performance Metrics requested");

        return Uni.createFrom().item(() -> {
            try {
                AnalyticsService.PerformanceMetrics metrics = analyticsService.getPerformanceMetrics();

                LOG.infof("✅ Performance Metrics collected: CPU=%.1f%%, Memory=%dMB/%dMB, TPS=%.0f, Errors=%.2f%%",
                        metrics.cpuUtilization(),
                        metrics.memoryUsage().used(),
                        metrics.memoryUsage().total(),
                        metrics.throughput(),
                        metrics.errorRate());

                return metrics;
            } catch (Exception e) {
                LOG.errorf(e, "❌ Error collecting performance metrics");
                throw new WebApplicationException(
                        "Failed to collect performance metrics: " + e.getMessage(),
                        500
                );
            }
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    /**
     * Health check endpoint for analytics service
     */
    @GET
    @Path("/health")
    public Uni<AnalyticsHealthStatus> getHealth() {
        return Uni.createFrom().item(() -> {
            boolean healthy = true;
            String status = "HEALTHY";
            String message = "Analytics service is operational";

            try {
                // Quick validation of analytics service
                analyticsService.getPerformanceMetrics();
            } catch (Exception e) {
                healthy = false;
                status = "UNHEALTHY";
                message = "Analytics service error: " + e.getMessage();
                LOG.error("Analytics health check failed", e);
            }

            return new AnalyticsHealthStatus(status, healthy, message);
        });
    }

    /**
     * Record TPS data point (internal endpoint for system monitoring)
     */
    @POST
    @Path("/record/tps")
    public Uni<RecordResponse> recordTPS(TPSRecordRequest request) {
        return Uni.createFrom().item(() -> {
            try {
                analyticsService.recordTPSDataPoint(request.tps());
                LOG.tracef("Recorded TPS: %.0f", request.tps());
                return new RecordResponse(true, "TPS recorded successfully");
            } catch (Exception e) {
                LOG.error("Failed to record TPS", e);
                return new RecordResponse(false, "Failed to record TPS: " + e.getMessage());
            }
        });
    }

    /**
     * Record transaction by type (internal endpoint)
     */
    @POST
    @Path("/record/transaction")
    public Uni<RecordResponse> recordTransaction(TransactionRecordRequest request) {
        return Uni.createFrom().item(() -> {
            try {
                analyticsService.recordTransaction(request.type());
                LOG.tracef("Recorded transaction type: %s", request.type());
                return new RecordResponse(true, "Transaction recorded successfully");
            } catch (Exception e) {
                LOG.error("Failed to record transaction", e);
                return new RecordResponse(false, "Failed to record transaction: " + e.getMessage());
            }
        });
    }

    /**
     * Update validator performance (internal endpoint)
     */
    @POST
    @Path("/record/validator")
    public Uni<RecordResponse> recordValidatorPerformance(ValidatorRecordRequest request) {
        return Uni.createFrom().item(() -> {
            try {
                analyticsService.updateValidatorPerformance(
                        request.validatorId(),
                        request.performanceScore(),
                        request.blocksProposed()
                );
                LOG.tracef("Recorded validator performance: %s", request.validatorId());
                return new RecordResponse(true, "Validator performance recorded successfully");
            } catch (Exception e) {
                LOG.error("Failed to record validator performance", e);
                return new RecordResponse(false, "Failed to record validator performance: " + e.getMessage());
            }
        });
    }

    // DTOs for requests and responses

    public record AnalyticsHealthStatus(
            String status,
            boolean healthy,
            String message
    ) {}

    public record TPSRecordRequest(
            double tps
    ) {}

    public record TransactionRecordRequest(
            String type
    ) {}

    public record ValidatorRecordRequest(
            String validatorId,
            double performanceScore,
            long blocksProposed
    ) {}

    public record RecordResponse(
            boolean success,
            String message
    ) {}
}
