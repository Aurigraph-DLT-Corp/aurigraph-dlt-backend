package io.aurigraph.v11.api;

import io.aurigraph.v11.monitoring.NetworkMonitoringService;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

/**
 * Network Monitoring API Resource - AV11-275
 * Provides real-time network health and peer status endpoints
 */
@Path("/api/v11/network/monitoring")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Network Monitoring", description = "Network health and peer monitoring APIs")
public class NetworkMonitoringResource {

    @Inject
    NetworkMonitoringService monitoringService;

    @GET
    @Path("/health")
    @Operation(
        summary = "Get network health status",
        description = "Returns overall network health including peer count, latency, and alerts"
    )
    public Uni<NetworkMonitoringService.NetworkHealth> getNetworkHealth() {
        return monitoringService.getNetworkHealth();
    }

    @GET
    @Path("/peers")
    @Operation(
        summary = "Get all peer statuses",
        description = "Returns detailed status information for all connected peers"
    )
    public Uni<java.util.List<NetworkMonitoringService.PeerStatus>> getPeerStatus() {
        return monitoringService.getPeerStatus();
    }

    @GET
    @Path("/peers/map")
    @Operation(
        summary = "Get peer network map",
        description = "Returns peer network topology for visualization with geolocation data"
    )
    public Uni<NetworkMonitoringService.PeerMap> getPeerMap() {
        return monitoringService.getPeerMap();
    }

    @GET
    @Path("/statistics")
    @Operation(
        summary = "Get network statistics",
        description = "Returns network-wide statistics including TPS, blocks, and performance metrics"
    )
    public Uni<NetworkMonitoringService.NetworkStatistics> getNetworkStatistics() {
        return monitoringService.getNetworkStatistics();
    }

    @GET
    @Path("/latency/histogram")
    @Operation(
        summary = "Get latency distribution",
        description = "Returns latency histogram and percentiles for all peers"
    )
    public Uni<NetworkMonitoringService.LatencyHistogram> getLatencyHistogram() {
        return monitoringService.getLatencyHistogram();
    }

    @GET
    @Path("/alerts")
    @Operation(
        summary = "Get active network alerts",
        description = "Returns list of active network health alerts and warnings"
    )
    public Uni<java.util.List<String>> getActiveAlerts() {
        return monitoringService.getNetworkHealth()
                .map(health -> health.alerts != null ? health.alerts : java.util.Collections.emptyList());
    }
}
