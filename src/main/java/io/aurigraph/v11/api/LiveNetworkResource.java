package io.aurigraph.v11.api;

import io.aurigraph.v11.live.LiveNetworkService;
import io.aurigraph.v11.models.NetworkMetrics;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.Map;

/**
 * Live Network Monitor API
 * Provides real-time network metrics and health status
 *
 * AV11-275: Implement Live Network Monitor API
 *
 * Endpoints:
 * - GET /api/v11/live/network - Get real-time network metrics
 *
 * @author Aurigraph V11
 * @version 11.3.0
 */
@Path("/api/v11/live/network")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Live Network Monitor", description = "Real-time network monitoring and metrics")
public class LiveNetworkResource {

    private static final Logger LOG = Logger.getLogger(LiveNetworkResource.class);

    @Inject
    LiveNetworkService liveNetworkService;

    /**
     * Get real-time network metrics
     *
     * Returns comprehensive network metrics including:
     * - Active connections (total, P2P, client, validator)
     * - Bandwidth usage (inbound, outbound, utilization)
     * - Message rates (messages/sec, TPS, blocks/min, latency)
     * - Recent network events
     * - Node health status
     * - Overall network status
     *
     * @return NetworkMetrics with all current network data
     */
    @GET
    @Operation(summary = "Get real-time network metrics",
               description = "Returns comprehensive real-time network metrics including connections, bandwidth, message rates, events, and node health")
    @APIResponses(value = {
            @APIResponse(responseCode = "200",
                         description = "Network metrics retrieved successfully",
                         content = @Content(mediaType = MediaType.APPLICATION_JSON,
                                          schema = @Schema(implementation = NetworkMetrics.class))),
            @APIResponse(responseCode = "500",
                         description = "Internal server error")
    })
    public Uni<Response> getNetworkMetrics() {
        LOG.info("GET /api/v11/live/network - Fetching real-time network metrics");

        return liveNetworkService.getNetworkMetrics()
                .map(metrics -> {
                    LOG.debugf("Network metrics retrieved: status=%s, connections=%d, tps=%.2f",
                            metrics.getNetworkStatus(),
                            metrics.getActiveConnections() != null ?
                                    metrics.getActiveConnections().getTotal() : 0,
                            metrics.getMessageRates() != null ?
                                    metrics.getMessageRates().getTransactionsPerSecond() : 0.0);

                    return Response.ok(metrics).build();
                })
                .onFailure().recoverWithItem(throwable -> {
                    LOG.error("Error retrieving network metrics", throwable);
                    return (Response) Response.serverError()
                            .entity(Map.of(
                                    "error", "Failed to retrieve network metrics",
                                    "message", throwable.getMessage()
                            ))
                            .build();
                });
    }

    /**
     * Get network health summary
     *
     * Simplified endpoint for quick health checks
     *
     * @return Network status summary
     */
    @GET
    @Path("/health")
    @Operation(summary = "Get network health summary",
               description = "Returns simplified network health status for quick checks")
    @APIResponses(value = {
            @APIResponse(responseCode = "200",
                         description = "Network health retrieved successfully"),
            @APIResponse(responseCode = "500",
                         description = "Internal server error")
    })
    public Uni<Response> getNetworkHealth() {
        LOG.debug("GET /api/v11/live/network/health - Fetching network health summary");

        return liveNetworkService.getNetworkMetrics()
                .map(metrics -> {
                    Map<String, Object> health = Map.of(
                            "status", metrics.getNetworkStatus(),
                            "timestamp", metrics.getTimestamp().toString(),
                            "total_connections", metrics.getActiveConnections() != null ?
                                    metrics.getActiveConnections().getTotal() : 0,
                            "tps", metrics.getMessageRates() != null ?
                                    metrics.getMessageRates().getTransactionsPerSecond() : 0.0,
                            "uptime_seconds", liveNetworkService.getUptimeSeconds()
                    );

                    return Response.ok(health).build();
                })
                .onFailure().recoverWithItem(throwable -> {
                    LOG.error("Error retrieving network health", throwable);
                    return (Response) Response.serverError()
                            .entity(Map.of(
                                    "error", "Failed to retrieve network health",
                                    "message", throwable.getMessage()
                            ))
                            .build();
                });
    }

    /**
     * Get network events
     *
     * Returns recent network events
     *
     * @param limit Maximum number of events to return (default: 20, max: 100)
     * @return List of recent network events
     */
    @GET
    @Path("/events")
    @Operation(summary = "Get recent network events",
               description = "Returns recent network events with optional limit")
    @APIResponses(value = {
            @APIResponse(responseCode = "200",
                         description = "Network events retrieved successfully"),
            @APIResponse(responseCode = "400",
                         description = "Invalid limit parameter"),
            @APIResponse(responseCode = "500",
                         description = "Internal server error")
    })
    public Uni<Response> getNetworkEvents(
            @QueryParam("limit") @DefaultValue("20") int limit) {

        LOG.debugf("GET /api/v11/live/network/events?limit=%d", limit);

        if (limit < 1 || limit > 100) {
            return Uni.createFrom().item(
                    Response.status(Response.Status.BAD_REQUEST)
                            .entity(Map.of(
                                    "error", "Invalid limit parameter",
                                    "message", "Limit must be between 1 and 100"
                            ))
                            .build()
            );
        }

        return liveNetworkService.getNetworkMetrics()
                .map(metrics -> {
                    var events = metrics.getRecentEvents();
                    if (events != null && events.size() > limit) {
                        events = events.subList(Math.max(0, events.size() - limit), events.size());
                    }

                    return Response.ok(Map.of(
                            "events", events != null ? events : java.util.Collections.emptyList(),
                            "count", events != null ? events.size() : 0
                    )).build();
                })
                .onFailure().recoverWithItem(throwable -> {
                    LOG.error("Error retrieving network events", throwable);
                    return (Response) Response.serverError()
                            .entity(Map.of(
                                    "error", "Failed to retrieve network events",
                                    "message", throwable.getMessage()
                            ))
                            .build();
                });
    }
}
