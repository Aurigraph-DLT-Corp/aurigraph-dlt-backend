package io.aurigraph.v11.analytics;

import io.smallrye.mutiny.Multi;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Real-Time Analytics REST API - AV11-485 Implementation
 *
 * Provides REST API endpoints for real-time analytics data.
 * These serve as fallback when WebSocket connections are not available.
 *
 * Endpoints:
 * - GET /api/v11/analytics/realtime - Current real-time snapshot
 * - GET /api/v11/analytics/history - Historical data (query params: from, to)
 * - GET /api/v11/analytics/stream - Server-Sent Events (SSE) stream
 * - GET /api/v11/analytics/health - Analytics service health check
 *
 * Note: For optimal performance, use WebSocket subscriptions via /ws/v11
 * (subscribe to "analytics" channel)
 *
 * @author Real-Time Communication Agent (RTCA)
 * @since V11.6.0 (Sprint 16 - AV11-485)
 * @epic AV11-491 Real-Time Communication Infrastructure
 */
@Path("/api/v11/analytics")
@Tag(name = "Real-Time Analytics", description = "Real-time analytics and metrics endpoints")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RealTimeAnalyticsResource {

    private static final Logger LOG = Logger.getLogger(RealTimeAnalyticsResource.class);

    @Inject
    RealTimeAnalyticsService analyticsService;

    /**
     * Get current real-time metrics snapshot
     *
     * GET /api/v11/analytics/realtime
     *
     * @return Current real-time metrics
     */
    @GET
    @Path("/realtime")
    @Operation(
        summary = "Get current real-time metrics",
        description = "Returns the current snapshot of real-time analytics metrics including TPS, validators, pending transactions, and network health"
    )
    @APIResponse(
        responseCode = "200",
        description = "Current metrics snapshot",
        content = @Content(schema = @Schema(implementation = RealTimeMetricsResponse.class))
    )
    public Response getCurrentMetrics() {
        try {
            RealTimeAnalyticsService.RealTimeMetrics metrics = analyticsService.getCurrentSnapshot();

            Map<String, Object> response = buildMetricsResponse(metrics);

            LOG.debug("Returned current real-time metrics");
            return Response.ok(response).build();

        } catch (Exception e) {
            LOG.errorf(e, "Error getting current metrics");
            return Response.serverError()
                .entity(Map.of("error", "Failed to get current metrics", "message", e.getMessage()))
                .build();
        }
    }

    /**
     * Get historical metrics
     *
     * GET /api/v11/analytics/history?from=<timestamp>&to=<timestamp>&seconds=<n>
     *
     * Query Parameters:
     * - from: Start timestamp (epoch seconds, optional)
     * - to: End timestamp (epoch seconds, optional)
     * - seconds: Get last N seconds (alternative to from/to)
     *
     * @param fromTimestamp Start timestamp (epoch seconds)
     * @param toTimestamp End timestamp (epoch seconds)
     * @param lastSeconds Get last N seconds
     * @return Historical metrics
     */
    @GET
    @Path("/history")
    @Operation(
        summary = "Get historical metrics",
        description = "Returns historical real-time metrics for a specified time range"
    )
    @APIResponse(
        responseCode = "200",
        description = "Historical metrics",
        content = @Content(schema = @Schema(implementation = HistoricalMetricsResponse.class))
    )
    public Response getHistoricalMetrics(
        @QueryParam("from") Long fromTimestamp,
        @QueryParam("to") Long toTimestamp,
        @QueryParam("seconds") @DefaultValue("60") Integer lastSeconds
    ) {
        try {
            List<RealTimeAnalyticsService.RealTimeMetrics> metrics;

            if (fromTimestamp != null && toTimestamp != null) {
                // Query by timestamp range
                metrics = analyticsService.getHistoricalMetrics(fromTimestamp, toTimestamp);
                LOG.debugf("Retrieved %d historical metrics (from=%d, to=%d)",
                    metrics.size(), fromTimestamp, toTimestamp);
            } else {
                // Query by last N seconds
                metrics = analyticsService.getRecentMetrics(lastSeconds);
                LOG.debugf("Retrieved %d historical metrics (last %d seconds)",
                    metrics.size(), lastSeconds);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("count", metrics.size());
            response.put("from", fromTimestamp != null ? fromTimestamp : Instant.now().minusSeconds(lastSeconds).getEpochSecond());
            response.put("to", toTimestamp != null ? toTimestamp : Instant.now().getEpochSecond());
            response.put("data", metrics.stream()
                .map(this::buildMetricsResponse)
                .toList()
            );

            return Response.ok(response).build();

        } catch (Exception e) {
            LOG.errorf(e, "Error getting historical metrics");
            return Response.serverError()
                .entity(Map.of("error", "Failed to get historical metrics", "message", e.getMessage()))
                .build();
        }
    }

    /**
     * Stream real-time metrics via Server-Sent Events (SSE)
     *
     * GET /api/v11/analytics/stream
     *
     * @return SSE stream of metrics (updates every second)
     */
    @GET
    @Path("/stream")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    @Operation(
        summary = "Stream real-time metrics",
        description = "Server-Sent Events (SSE) stream of real-time metrics. Updates every second."
    )
    @APIResponse(
        responseCode = "200",
        description = "SSE stream of metrics",
        content = @Content(mediaType = MediaType.SERVER_SENT_EVENTS)
    )
    public Multi<Map<String, Object>> streamMetrics() {
        LOG.info("Client connected to real-time metrics SSE stream");

        return analyticsService.streamMetrics()
            .onItem().transform(this::buildMetricsResponse)
            .onFailure().invoke(throwable ->
                LOG.errorf(throwable, "Error in metrics stream")
            );
    }

    /**
     * Health check for analytics service
     *
     * GET /api/v11/analytics/health
     *
     * @return Health status
     */
    @GET
    @Path("/health")
    @Operation(
        summary = "Analytics service health check",
        description = "Returns health status of the real-time analytics service"
    )
    @APIResponse(
        responseCode = "200",
        description = "Service is healthy"
    )
    public Response healthCheck() {
        try {
            RealTimeAnalyticsService.RealTimeMetrics currentMetrics = analyticsService.getCurrentSnapshot();

            Map<String, Object> health = Map.of(
                "status", "UP",
                "service", "RealTimeAnalyticsService",
                "timestamp", Instant.now().toString(),
                "currentTPS", currentMetrics.currentTPS,
                "networkHealth", currentMetrics.networkHealth,
                "activeValidators", currentMetrics.activeValidators
            );

            return Response.ok(health).build();

        } catch (Exception e) {
            LOG.errorf(e, "Analytics service health check failed");
            return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                .entity(Map.of("status", "DOWN", "error", e.getMessage()))
                .build();
        }
    }

    /**
     * Build metrics response map
     *
     * @param metrics Real-time metrics
     * @return Response map
     */
    private Map<String, Object> buildMetricsResponse(RealTimeAnalyticsService.RealTimeMetrics metrics) {
        Map<String, Object> response = new HashMap<>();

        response.put("timestamp", metrics.timestamp.toString());
        response.put("epochSeconds", metrics.timestamp.getEpochSecond());
        response.put("currentTPS", metrics.currentTPS);
        response.put("activeValidators", metrics.activeValidators);
        response.put("pendingTransactions", metrics.pendingTransactions);
        response.put("currentBlockHeight", metrics.currentBlockHeight);
        response.put("networkHealth", metrics.networkHealth);

        // Bridge metrics
        Map<String, Object> bridge = new HashMap<>();
        bridge.put("totalTransfers", metrics.bridgeMetrics.totalTransfers);
        bridge.put("pendingTransfers", metrics.bridgeMetrics.pendingTransfers);
        bridge.put("activeChains", metrics.bridgeMetrics.activeChains);
        response.put("bridge", bridge);

        // System resources
        Map<String, Object> resources = new HashMap<>();
        resources.put("cpuUsage", String.format("%.2f%%", metrics.resources.cpuUsage));
        resources.put("memoryUsage", String.format("%.2f%%", metrics.resources.memoryUsage));
        resources.put("diskUsage", String.format("%.2f%%", metrics.resources.diskUsage));
        response.put("resources", resources);

        return response;
    }

    /**
     * Response schema for OpenAPI documentation
     */
    public static class RealTimeMetricsResponse {
        public String timestamp;
        public long epochSeconds;
        public double currentTPS;
        public int activeValidators;
        public long pendingTransactions;
        public long currentBlockHeight;
        public String networkHealth;
        public BridgeResponse bridge;
        public ResourcesResponse resources;
    }

    public static class BridgeResponse {
        public long totalTransfers;
        public long pendingTransfers;
        public int activeChains;
    }

    public static class ResourcesResponse {
        public String cpuUsage;
        public String memoryUsage;
        public String diskUsage;
    }

    public static class HistoricalMetricsResponse {
        public int count;
        public long from;
        public long to;
        public List<RealTimeMetricsResponse> data;
    }
}
