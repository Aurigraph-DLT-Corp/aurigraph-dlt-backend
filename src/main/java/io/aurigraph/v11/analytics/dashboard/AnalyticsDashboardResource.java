package io.aurigraph.v11.analytics.dashboard;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.Map;

/**
 * Analytics Dashboard REST API Resource
 *
 * AV11-485: Real-time analytics dashboard REST endpoints
 * Provides comprehensive metrics, performance data, and historical analytics
 *
 * Endpoints:
 * - GET /api/v11/dashboard - Dashboard summary
 * - GET /api/v11/dashboard/performance - Performance metrics
 * - GET /api/v11/dashboard/transactions - Transaction stats
 * - GET /api/v11/dashboard/nodes - Node health
 * - GET /api/v11/dashboard/history/{period} - Historical data
 * - GET /api/v11/dashboard/websocket-status - WebSocket connection status
 *
 * @author Analytics Dashboard Team
 * @version 11.0.0
 * @since Sprint 13
 */
@Path("/api/v11/dashboard")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Analytics Dashboard", description = "Real-time analytics and monitoring endpoints")
public class AnalyticsDashboardResource {

    private static final Logger LOG = Logger.getLogger(AnalyticsDashboardResource.class);

    @Inject
    AnalyticsDashboardService dashboardService;

    /**
     * Get comprehensive dashboard metrics
     * Returns complete dashboard view with all metrics
     */
    @GET
    @Operation(
        summary = "Get Dashboard Metrics",
        description = "Returns comprehensive dashboard metrics including TPS, transactions, blocks, nodes, and performance data"
    )
    @APIResponse(
        responseCode = "200",
        description = "Dashboard metrics retrieved successfully",
        content = @Content(schema = @Schema(implementation = DashboardMetrics.class))
    )
    public Response getDashboard() {
        LOG.debug("GET /api/v11/dashboard - Fetching dashboard metrics");

        try {
            DashboardMetrics metrics = dashboardService.getDashboardMetrics();
            return Response.ok(metrics).build();
        } catch (Exception e) {
            LOG.error("Failed to get dashboard metrics", e);
            return Response.serverError()
                .entity(Map.of("error", "Failed to retrieve dashboard metrics", "message", e.getMessage()))
                .build();
        }
    }

    /**
     * Get detailed performance metrics
     * Returns throughput, latency, resource utilization, and reliability metrics
     */
    @GET
    @Path("/performance")
    @Operation(
        summary = "Get Performance Metrics",
        description = "Returns detailed performance metrics including throughput, latency, resource utilization, and reliability"
    )
    @APIResponse(
        responseCode = "200",
        description = "Performance metrics retrieved successfully",
        content = @Content(schema = @Schema(implementation = PerformanceMetrics.class))
    )
    public Response getPerformance() {
        LOG.debug("GET /api/v11/dashboard/performance - Fetching performance metrics");

        try {
            PerformanceMetrics metrics = dashboardService.getPerformanceMetrics();
            return Response.ok(metrics).build();
        } catch (Exception e) {
            LOG.error("Failed to get performance metrics", e);
            return Response.serverError()
                .entity(Map.of("error", "Failed to retrieve performance metrics", "message", e.getMessage()))
                .build();
        }
    }

    /**
     * Get transaction statistics
     * Returns TPS, transaction counts, and transaction type breakdown
     */
    @GET
    @Path("/transactions")
    @Operation(
        summary = "Get Transaction Statistics",
        description = "Returns transaction statistics including TPS, totals, and breakdown by type"
    )
    @APIResponse(
        responseCode = "200",
        description = "Transaction statistics retrieved successfully",
        content = @Content(schema = @Schema(implementation = AnalyticsDashboardService.TransactionStats.class))
    )
    public Response getTransactions() {
        LOG.debug("GET /api/v11/dashboard/transactions - Fetching transaction statistics");

        try {
            AnalyticsDashboardService.TransactionStats stats = dashboardService.getTransactionStats();
            return Response.ok(stats).build();
        } catch (Exception e) {
            LOG.error("Failed to get transaction statistics", e);
            return Response.serverError()
                .entity(Map.of("error", "Failed to retrieve transaction statistics", "message", e.getMessage()))
                .build();
        }
    }

    /**
     * Get node health status
     * Returns health metrics for all nodes in the network
     */
    @GET
    @Path("/nodes")
    @Operation(
        summary = "Get Node Health Status",
        description = "Returns health status and metrics for all nodes in the network"
    )
    @APIResponse(
        responseCode = "200",
        description = "Node health status retrieved successfully",
        content = @Content(schema = @Schema(implementation = NodeHealthMetrics.class))
    )
    public Response getNodes() {
        LOG.debug("GET /api/v11/dashboard/nodes - Fetching node health status");

        try {
            List<NodeHealthMetrics> nodeHealth = dashboardService.getNodeHealthStatus();
            return Response.ok(nodeHealth).build();
        } catch (Exception e) {
            LOG.error("Failed to get node health status", e);
            return Response.serverError()
                .entity(Map.of("error", "Failed to retrieve node health status", "message", e.getMessage()))
                .build();
        }
    }

    /**
     * Get historical data for specified period
     * Returns time-series data for visualization
     */
    @GET
    @Path("/history/{period}")
    @Operation(
        summary = "Get Historical Data",
        description = "Returns historical time-series data for the specified period (1h, 6h, 24h)"
    )
    @APIResponse(
        responseCode = "200",
        description = "Historical data retrieved successfully",
        content = @Content(schema = @Schema(implementation = DashboardMetrics.HistoricalDataPoint.class))
    )
    @APIResponse(
        responseCode = "400",
        description = "Invalid period specified"
    )
    public Response getHistory(
        @Parameter(description = "Time period (1h, 6h, 24h)", required = true)
        @PathParam("period") String period
    ) {
        LOG.debugf("GET /api/v11/dashboard/history/%s - Fetching historical data", period);

        // Validate period
        if (!period.matches("(1h|6h|24h)")) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(Map.of("error", "Invalid period. Must be one of: 1h, 6h, 24h"))
                .build();
        }

        try {
            List<DashboardMetrics.HistoricalDataPoint> history = dashboardService.getHistoricalData(period);
            return Response.ok(Map.of(
                "period", period,
                "dataPoints", history.size(),
                "data", history
            )).build();
        } catch (Exception e) {
            LOG.error("Failed to get historical data", e);
            return Response.serverError()
                .entity(Map.of("error", "Failed to retrieve historical data", "message", e.getMessage()))
                .build();
        }
    }

    /**
     * Get WebSocket connection status
     * Returns number of active WebSocket connections
     */
    @GET
    @Path("/websocket-status")
    @Operation(
        summary = "Get WebSocket Status",
        description = "Returns status of WebSocket connections for real-time streaming"
    )
    @APIResponse(
        responseCode = "200",
        description = "WebSocket status retrieved successfully"
    )
    public Response getWebSocketStatus() {
        LOG.debug("GET /api/v11/dashboard/websocket-status - Fetching WebSocket status");

        try {
            int connections = AnalyticsDashboardWebSocket.getConnectionCount();
            boolean hasConnections = AnalyticsDashboardWebSocket.hasConnections();

            return Response.ok(Map.of(
                "endpoint", "/ws/dashboard",
                "activeConnections", connections,
                "hasActiveConnections", hasConnections,
                "status", hasConnections ? "active" : "idle",
                "updateInterval", "1000ms",
                "supportedChannels", List.of("all", "transactions", "blocks", "nodes", "performance")
            )).build();
        } catch (Exception e) {
            LOG.error("Failed to get WebSocket status", e);
            return Response.serverError()
                .entity(Map.of("error", "Failed to retrieve WebSocket status", "message", e.getMessage()))
                .build();
        }
    }

    /**
     * Health check endpoint for dashboard service
     */
    @GET
    @Path("/health")
    @Operation(
        summary = "Dashboard Health Check",
        description = "Checks if the analytics dashboard service is healthy and operational"
    )
    @APIResponse(
        responseCode = "200",
        description = "Dashboard service is healthy"
    )
    public Response health() {
        try {
            // Check if service is operational by fetching metrics
            dashboardService.getDashboardMetrics();

            return Response.ok(Map.of(
                "status", "healthy",
                "service", "analytics-dashboard",
                "version", "12.0.0",
                "timestamp", System.currentTimeMillis()
            )).build();
        } catch (Exception e) {
            LOG.error("Dashboard health check failed", e);
            return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                .entity(Map.of(
                    "status", "unhealthy",
                    "service", "analytics-dashboard",
                    "error", e.getMessage()
                ))
                .build();
        }
    }

    /**
     * Get dashboard configuration and capabilities
     */
    @GET
    @Path("/config")
    @Operation(
        summary = "Get Dashboard Configuration",
        description = "Returns dashboard configuration and available features"
    )
    @APIResponse(
        responseCode = "200",
        description = "Dashboard configuration retrieved successfully"
    )
    public Response getConfig() {
        LOG.debug("GET /api/v11/dashboard/config - Fetching dashboard configuration");

        return Response.ok(Map.of(
            "version", "12.0.0",
            "features", Map.of(
                "realTimeStreaming", true,
                "historicalData", true,
                "nodeMonitoring", true,
                "performanceMetrics", true,
                "transactionAnalytics", true
            ),
            "updateIntervals", Map.of(
                "dashboard", "1000ms",
                "performance", "5000ms",
                "nodeHealth", "3000ms"
            ),
            "websocket", Map.of(
                "endpoint", "/ws/dashboard",
                "protocol", "ws",
                "channels", List.of("all", "transactions", "blocks", "nodes", "performance")
            ),
            "historicalDataPeriods", List.of("1h", "6h", "24h"),
            "metricsRetention", "24h"
        )).build();
    }
}
