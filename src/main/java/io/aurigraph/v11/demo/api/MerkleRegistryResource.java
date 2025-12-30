package io.aurigraph.v11.demo.api;

import io.aurigraph.v11.demo.services.DataFeedRegistry;
import io.aurigraph.v11.demo.services.DemoChannelSimulationService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import io.smallrye.mutiny.Uni;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 * Merkle Registry API Resource
 *
 * Provides real-time Merkle tree registry updates and data feed tokenization
 * for the demo channel simulation with support for 5 external API data feeds.
 *
 * Standard Demo Configuration:
 * - Validator Nodes: 5
 * - Business Nodes: 10
 * - Slim Nodes: 5 (one per external API data feed)
 *
 * @version 1.0.0
 */
@Path("/api/v11/demo/registry")
@ApplicationScoped
@Tag(name = "Merkle Registry", description = "Real-time Merkle tree registry and data feed tokenization")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class MerkleRegistryResource {

    private static final Logger LOG = Logger.getLogger(MerkleRegistryResource.class);

    @Inject
    DemoChannelSimulationService simulationService;

    /**
     * Start a new demo channel simulation with Merkle tree registry
     * POST /api/v11/demo/registry/start
     */
    @POST
    @Path("/start")
    @Operation(summary = "Start demo simulation", description = "Create and start a new demo channel with Merkle registry and 5 data feeds")
    @APIResponse(responseCode = "201", description = "Simulation started")
    public Uni<Response> startDemoSimulation(DemoSimulationRequest request) {
        LOG.infof("Starting demo simulation with channel name: %s", request.channelName);

        return simulationService.createAndStartDemo(request.channelName)
            .map(simulation -> {
                return Response.status(Response.Status.CREATED)
                    .entity(Map.of(
                        "success", true,
                        "data", Map.of(
                            "channelId", simulation.channelId,
                            "channelName", simulation.channelName,
                            "config", Map.of(
                                "validators", simulation.validatorNodes,
                                "businessNodes", simulation.businessNodes,
                                "slimNodes", simulation.slimNodes,
                                "apiFeeds", 5
                            ),
                            "status", "running"
                        ),
                        "timestamp", System.currentTimeMillis()
                    ))
                    .build();
            });
    }

    /**
     * Get simulation status with real-time Merkle registry data
     * GET /api/v11/demo/registry/simulation/{channelId}
     */
    @GET
    @Path("/simulation/{channelId}")
    @Operation(summary = "Get simulation status", description = "Get real-time status with Merkle tree registry updates")
    @APIResponse(responseCode = "200", description = "Simulation status retrieved")
    public Uni<Response> getSimulationStatus(@PathParam("channelId") String channelId) {
        return simulationService.getSimulation(channelId)
            .map(simulation -> {
                Map<String, Object> response = new HashMap<>();
                response.put("channelId", simulation.channelId);
                response.put("channelName", simulation.channelName);
                response.put("status", simulation.running ? "running" : "completed");
                response.put("duration", simulation.getDuration());

                // Configuration
                Map<String, Object> config = new HashMap<>();
                config.put("validatorNodes", simulation.validatorNodes);
                config.put("businessNodes", simulation.businessNodes);
                config.put("slimNodes", simulation.slimNodes);
                response.put("config", config);

                // Real-time metrics
                Map<String, Object> metrics = new HashMap<>();
                metrics.put("totalTransactions", simulation.totalTransactions);
                metrics.put("successfulTransactions", simulation.successfulTransactions);
                metrics.put("failedTransactions", simulation.failedTransactions);
                metrics.put("successRate", String.format("%.2f%%", simulation.getSuccessRate()));
                metrics.put("peakTPS", String.format("%.0f", simulation.peakTPS));
                metrics.put("averageLatency", String.format("%.2f ms", simulation.averageLatency));
                metrics.put("blockHeight", simulation.blockHeight);
                response.put("metrics", metrics);

                // Merkle tree registry status
                Map<String, Object> merkleRegistry = new HashMap<>();
                merkleRegistry.put("rootHash", simulation.merkleRoot);
                merkleRegistry.put("registeredDataFeeds", simulation.registeredDataFeeds);
                merkleRegistry.put("totalTokens", simulation.totalTokens);
                merkleRegistry.put("lastUpdate", simulation.lastMerkleUpdate);
                response.put("merkleRegistry", merkleRegistry);

                return Response.ok(Map.of(
                    "success", true,
                    "data", response,
                    "timestamp", System.currentTimeMillis()
                )).build();
            })
            .onFailure().recoverWithItem(() ->
                Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of(
                        "success", false,
                        "error", "Simulation not found: " + channelId,
                        "timestamp", System.currentTimeMillis()
                    ))
                    .build()
            );
    }

    /**
     * Get Merkle tree registry statistics
     * GET /api/v11/demo/registry/stats
     */
    @GET
    @Path("/stats")
    @Operation(summary = "Get registry statistics", description = "Get detailed Merkle tree registry statistics")
    @APIResponse(responseCode = "200", description = "Registry statistics retrieved")
    public Uni<Response> getRegistryStats() {
        DataFeedRegistry registry = simulationService.getDataFeedRegistry();

        return registry.getStats()
            .map(stats -> {
                Map<String, Object> registryInfo = new HashMap<>();
                registryInfo.put("rootHash", stats.getRootHash());
                registryInfo.put("entryCount", stats.getEntryCount());
                registryInfo.put("treeHeight", stats.getTreeHeight());
                registryInfo.put("lastUpdate", stats.getLastUpdate());
                registryInfo.put("rebuildCount", stats.getRebuildCount());
                registryInfo.put("totalUpdates", stats.totalUpdates);
                registryInfo.put("totalTokens", stats.totalTokens);
                registryInfo.put("apiCount", stats.apiCount);
                registryInfo.put("lastFeedUpdate", stats.lastFeedUpdate);

                return Response.ok(Map.of(
                    "success", true,
                    "data", registryInfo,
                    "timestamp", System.currentTimeMillis()
                )).build();
            });
    }

    /**
     * Get all external API data feeds
     * GET /api/v11/demo/registry/feeds
     */
    @GET
    @Path("/feeds")
    @Operation(summary = "Get data feeds", description = "Get all external API data feeds")
    @APIResponse(responseCode = "200", description = "Data feeds retrieved")
    public Uni<Response> getDataFeeds() {
        DataFeedRegistry registry = simulationService.getDataFeedRegistry();

        return registry.getAllAPIs()
            .map(apis -> {
                Map<String, Object> feedList = new HashMap<>();
                feedList.put("count", apis.size());
                feedList.put("feeds", apis);

                return Response.ok(Map.of(
                    "success", true,
                    "data", feedList,
                    "timestamp", System.currentTimeMillis()
                )).build();
            });
    }

    /**
     * Get data feed tokens for specific API
     * GET /api/v11/demo/registry/feeds/{apiId}/tokens
     */
    @GET
    @Path("/feeds/{apiId}/tokens")
    @Operation(summary = "Get feed tokens", description = "Get all tokenized data for a specific API feed")
    @APIResponse(responseCode = "200", description = "Feed tokens retrieved")
    public Uni<Response> getFeedTokens(@PathParam("apiId") String apiId) {
        DataFeedRegistry registry = simulationService.getDataFeedRegistry();

        return registry.getTokensByAPI(apiId)
            .map(tokens -> {
                return Response.ok(Map.of(
                    "success", true,
                    "data", Map.of(
                        "apiId", apiId,
                        "tokenCount", tokens.size(),
                        "tokens", tokens
                    ),
                    "timestamp", System.currentTimeMillis()
                )).build();
            });
    }

    /**
     * Get specific feed status
     * GET /api/v11/demo/registry/feeds/{apiId}/status
     */
    @GET
    @Path("/feeds/{apiId}/status")
    @Operation(summary = "Get feed status", description = "Get status and statistics for a specific data feed")
    @APIResponse(responseCode = "200", description = "Feed status retrieved")
    public Uni<Response> getFeedStatus(@PathParam("apiId") String apiId) {
        DataFeedRegistry registry = simulationService.getDataFeedRegistry();

        return registry.getFeedStatus(apiId)
            .map(status -> {
                return Response.ok(Map.of(
                    "success", true,
                    "data", status,
                    "timestamp", System.currentTimeMillis()
                )).build();
            })
            .onFailure().recoverWithItem(() ->
                Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of(
                        "success", false,
                        "error", "Feed not found: " + apiId,
                        "timestamp", System.currentTimeMillis()
                    ))
                    .build()
            );
    }

    /**
     * Get all active simulations
     * GET /api/v11/demo/registry/simulations
     */
    @GET
    @Path("/simulations")
    @Operation(summary = "Get active simulations", description = "Get all active demo channel simulations")
    @APIResponse(responseCode = "200", description = "Simulations retrieved")
    public Uni<Response> getActiveSimulations() {
        return simulationService.getActiveSimulations()
            .map(simulations -> {
                return Response.ok(Map.of(
                    "success", true,
                    "data", Map.of(
                        "count", simulations.size(),
                        "simulations", simulations
                    ),
                    "timestamp", System.currentTimeMillis()
                )).build();
            });
    }

    /**
     * Stop a simulation
     * POST /api/v11/demo/registry/simulation/{channelId}/stop
     */
    @POST
    @Path("/simulation/{channelId}/stop")
    @Operation(summary = "Stop simulation", description = "Stop a running demo channel simulation")
    @APIResponse(responseCode = "200", description = "Simulation stopped")
    public Uni<Response> stopSimulation(@PathParam("channelId") String channelId) {
        return simulationService.stopSimulation(channelId)
            .map(stopped -> {
                if (stopped) {
                    return Response.ok(Map.of(
                        "success", true,
                        "message", "Simulation stopped: " + channelId,
                        "timestamp", System.currentTimeMillis()
                    )).build();
                } else {
                    return Response.status(Response.Status.NOT_FOUND)
                        .entity(Map.of(
                            "success", false,
                            "error", "Simulation not found: " + channelId,
                            "timestamp", System.currentTimeMillis()
                        ))
                        .build();
                }
            });
    }

    // ==================== DTOs ====================

    public static class DemoSimulationRequest {
        public String channelName;

        public DemoSimulationRequest() {}

        public DemoSimulationRequest(String channelName) {
            this.channelName = channelName;
        }
    }
}
