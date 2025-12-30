package io.aurigraph.v11.api;

import io.aurigraph.v11.models.BridgeStatus;
import io.aurigraph.v11.services.BridgeStatusService;
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
 * Bridge Status API
 * Provides cross-chain bridge health monitoring and operational status
 *
 * AV11-281: Implement Bridge Status Monitor API
 *
 * Endpoints:
 * - GET /api/v11/bridge/status - Get overall bridge status
 * - GET /api/v11/bridge/status/{bridgeId} - Get specific bridge status
 * - GET /api/v11/bridge/statistics - Get bridge statistics
 * - GET /api/v11/bridge/performance - Get performance metrics
 *
 * @author Aurigraph V11
 * @version 11.3.0
 */
@Path("/api/v11/bridge")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Bridge Status", description = "Cross-chain bridge health monitoring and operational status")
public class BridgeStatusResource {

    private static final Logger LOG = Logger.getLogger(BridgeStatusResource.class);

    @Inject
    BridgeStatusService bridgeStatusService;

    /**
     * Get overall bridge status
     *
     * Returns comprehensive bridge network status including:
     * - All active bridges (Ethereum, BSC, Polygon, Avalanche)
     * - Health metrics and capacity information
     * - Transfer statistics and performance
     * - Active alerts and warnings
     */
    @GET
    @Path("/status")
    @Operation(summary = "Get bridge status",
               description = "Returns comprehensive cross-chain bridge network status and health information")
    @APIResponses(value = {
            @APIResponse(responseCode = "200",
                         description = "Bridge status retrieved successfully",
                         content = @Content(mediaType = MediaType.APPLICATION_JSON,
                                          schema = @Schema(implementation = BridgeStatus.class))),
            @APIResponse(responseCode = "500",
                         description = "Internal server error")
    })
    public Uni<Response> getBridgeStatus() {
        LOG.info("GET /api/v11/bridge/status - Fetching bridge network status");

        return bridgeStatusService.getBridgeStatus()
                .map(status -> {
                    LOG.debugf("Bridge status retrieved: %s, %d active bridges",
                            status.getOverallStatus(),
                            status.getStatistics().getActiveBridges());

                    return Response.ok(status).build();
                })
                .onFailure().recoverWithItem(throwable -> {
                    LOG.error("Error retrieving bridge status", throwable);
                    return (Response) Response.serverError()
                            .entity(Map.of(
                                    "error", "Failed to retrieve bridge status",
                                    "message", throwable.getMessage()
                            ))
                            .build();
                });
    }

    /**
     * Get specific bridge status
     *
     * Returns detailed status for a specific cross-chain bridge
     *
     * @param bridgeId Bridge identifier (e.g., bridge-eth-001)
     */
    @GET
    @Path("/status/{bridgeId}")
    @Operation(summary = "Get specific bridge status",
               description = "Returns detailed status information for a specific bridge")
    @APIResponses(value = {
            @APIResponse(responseCode = "200",
                         description = "Bridge status retrieved successfully"),
            @APIResponse(responseCode = "404",
                         description = "Bridge not found"),
            @APIResponse(responseCode = "500",
                         description = "Internal server error")
    })
    public Uni<Response> getBridgeById(@PathParam("bridgeId") String bridgeId) {
        LOG.debugf("GET /api/v11/bridge/status/%s - Fetching specific bridge status", bridgeId);

        return bridgeStatusService.getBridgeById(bridgeId)
                .map(bridge -> {
                    if (bridge == null) {
                        return Response.status(Response.Status.NOT_FOUND)
                                .entity(Map.of("error", "Bridge not found", "bridgeId", bridgeId))
                                .build();
                    }

                    return Response.ok(bridge).build();
                })
                .onFailure().recoverWithItem(throwable -> {
                    LOG.error("Error retrieving bridge by ID", throwable);
                    return (Response) Response.serverError()
                            .entity(Map.of(
                                    "error", "Failed to retrieve bridge status",
                                    "message", throwable.getMessage()
                            ))
                            .build();
                });
    }

    /**
     * Get bridge statistics
     *
     * Returns aggregated statistics across all bridges
     */
    @GET
    @Path("/statistics")
    @Operation(summary = "Get bridge statistics",
               description = "Returns aggregated statistics across all cross-chain bridges")
    @APIResponses(value = {
            @APIResponse(responseCode = "200",
                         description = "Bridge statistics retrieved successfully"),
            @APIResponse(responseCode = "500",
                         description = "Internal server error")
    })
    public Uni<Response> getBridgeStatistics() {
        LOG.debug("GET /api/v11/bridge/statistics - Fetching bridge statistics");

        return bridgeStatusService.getBridgeStatus()
                .map(status -> {
                    Map<String, Object> stats = Map.of(
                            "total_bridges", status.getStatistics().getTotalBridges(),
                            "active_bridges", status.getStatistics().getActiveBridges(),
                            "total_transfers", status.getStatistics().getTotalTransfers(),
                            "total_volume_usd", status.getStatistics().getTotalVolumeUsd(),
                            "transfers_24h", status.getStatistics().getTransfers24h(),
                            "volume_24h_usd", status.getStatistics().getVolume24hUsd(),
                            "unique_users_24h", status.getStatistics().getUniqueUsers24h(),
                            "chain_distribution", status.getStatistics().getChainDistribution()
                    );

                    return Response.ok(stats).build();
                })
                .onFailure().recoverWithItem(throwable -> {
                    LOG.error("Error retrieving bridge statistics", throwable);
                    return (Response) Response.serverError()
                            .entity(Map.of(
                                    "error", "Failed to retrieve bridge statistics",
                                    "message", throwable.getMessage()
                            ))
                            .build();
                });
    }

    /**
     * Get bridge performance metrics
     *
     * Returns performance metrics including transfer times and gas efficiency
     */
    @GET
    @Path("/performance")
    @Operation(summary = "Get bridge performance metrics",
               description = "Returns performance metrics including transfer times, throughput, and gas efficiency")
    @APIResponses(value = {
            @APIResponse(responseCode = "200",
                         description = "Performance metrics retrieved successfully"),
            @APIResponse(responseCode = "500",
                         description = "Internal server error")
    })
    public Uni<Response> getBridgePerformance() {
        LOG.debug("GET /api/v11/bridge/performance - Fetching bridge performance");

        return bridgeStatusService.getBridgeStatus()
                .map(status -> {
                    Map<String, Object> performance = Map.of(
                            "average_transfer_time_seconds", status.getPerformance().getAverageTransferTimeSeconds(),
                            "fastest_transfer_seconds", status.getPerformance().getFastestTransferSeconds(),
                            "slowest_transfer_seconds", status.getPerformance().getSlowestTransferSeconds(),
                            "transfers_per_hour", status.getPerformance().getTransfersPerHour(),
                            "gas_efficiency", Map.of(
                                    "average_gas_cost_usd", status.getPerformance().getGasEfficiency().getAverageGasCostUsd(),
                                    "total_gas_spent_24h_usd", status.getPerformance().getGasEfficiency().getTotalGasSpent24hUsd(),
                                    "gas_optimization_percent", status.getPerformance().getGasEfficiency().getGasOptimizationPercent()
                            ),
                            "uptime_seconds", bridgeStatusService.getUptimeSeconds()
                    );

                    return Response.ok(performance).build();
                })
                .onFailure().recoverWithItem(throwable -> {
                    LOG.error("Error retrieving bridge performance", throwable);
                    return (Response) Response.serverError()
                            .entity(Map.of(
                                    "error", "Failed to retrieve bridge performance metrics",
                                    "message", throwable.getMessage()
                            ))
                            .build();
                });
    }

    /**
     * Get bridge alerts
     *
     * Returns active alerts and warnings for all bridges
     */
    @GET
    @Path("/alerts")
    @Operation(summary = "Get bridge alerts",
               description = "Returns active alerts and warnings across all bridges")
    @APIResponses(value = {
            @APIResponse(responseCode = "200",
                         description = "Alerts retrieved successfully"),
            @APIResponse(responseCode = "500",
                         description = "Internal server error")
    })
    public Uni<Response> getBridgeAlerts() {
        LOG.debug("GET /api/v11/bridge/alerts - Fetching bridge alerts");

        return bridgeStatusService.getBridgeStatus()
                .map(status -> {
                    Map<String, Object> alertsResponse = Map.of(
                            "alerts", status.getAlerts(),
                            "total_alerts", status.getAlerts().size(),
                            "critical_count", status.getAlerts().stream()
                                    .filter(a -> "critical".equals(a.getSeverity())).count(),
                            "warning_count", status.getAlerts().stream()
                                    .filter(a -> "warning".equals(a.getSeverity())).count()
                    );

                    return Response.ok(alertsResponse).build();
                })
                .onFailure().recoverWithItem(throwable -> {
                    LOG.error("Error retrieving bridge alerts", throwable);
                    return (Response) Response.serverError()
                            .entity(Map.of(
                                    "error", "Failed to retrieve bridge alerts",
                                    "message", throwable.getMessage()
                            ))
                            .build();
                });
    }
}
