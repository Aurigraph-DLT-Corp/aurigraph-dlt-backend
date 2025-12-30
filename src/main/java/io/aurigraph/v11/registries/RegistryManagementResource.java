package io.aurigraph.v11.registries;

import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import io.quarkus.logging.Log;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Registry Management REST API Resource
 *
 * Unified API for multi-registry search and management.
 * Provides endpoints for searching, listing, and managing entries across:
 * - Smart Contracts
 * - Tokens (ERC20, ERC721, ERC1155)
 * - Real-World Assets (RWATs)
 * - Merkle Trees
 * - Compliance Records
 *
 * Base Path: /api/v11/registries
 *
 * @version 11.5.0
 * @since 2025-11-14
 */
@Path("/api/v11/registries")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Registry Management", description = "Unified multi-registry search and management API")
public class RegistryManagementResource {

    @Inject
    RegistryManagementService registryManagementService;

    private static final String VALID_TYPES = "smart-contract, token, rwa, merkle-tree, compliance";

    // ========== Search Endpoints ==========

    /**
     * Search across all registries with optional type filtering
     *
     * GET /api/v11/registries/search?keyword=xxx&types=smart-contract,token&limit=50&offset=0
     *
     * @param keyword search keyword (optional)
     * @param types comma-separated list of registry types to search (optional, default: all)
     * @param limit maximum number of results (default: 50, max: 500)
     * @param offset pagination offset (default: 0)
     * @return list of search results with status 200
     */
    @GET
    @Path("/search")
    @Operation(
        summary = "Search across all registries",
        description = "Perform unified keyword search across Smart Contracts, Tokens, RWATs, Merkle Trees, and Compliance registries"
    )
    @APIResponse(
        responseCode = "200",
        description = "Search results",
        content = @Content(schema = @Schema(implementation = RegistrySearchResult[].class))
    )
    @APIResponse(responseCode = "400", description = "Invalid registry type")
    @APIResponse(responseCode = "500", description = "Internal server error")
    public Uni<Response> search(
            @QueryParam("keyword") String keyword,
            @QueryParam("types") String types,
            @QueryParam("limit") @DefaultValue("50") int limit,
            @QueryParam("offset") @DefaultValue("0") int offset) {

        Log.infof("REST: Search registries - keyword: %s, types: %s, limit: %d, offset: %d",
                keyword, types, limit, offset);

        // Parse types
        List<String> typeList = null;
        if (types != null && !types.trim().isEmpty()) {
            typeList = Arrays.stream(types.split(","))
                    .map(String::trim)
                    .filter(t -> !t.isEmpty())
                    .collect(Collectors.toList());

            // Validate types
            for (String type : typeList) {
                if (!RegistryType.isValid(type)) {
                    String errorMsg = String.format("Invalid registry type: %s. Valid types: %s", type, VALID_TYPES);
                    Log.warnf(errorMsg);
                    return Uni.createFrom().item(
                        Response.status(Response.Status.BAD_REQUEST)
                                .entity(Map.of("error", errorMsg))
                                .build()
                    );
                }
            }
        }

        return registryManagementService.searchAllRegistries(keyword, typeList, limit, offset)
                .map(results -> Response.ok(results).build())
                .onFailure().recoverWithItem(error -> {
                    Log.errorf("Search error: %s", error.getMessage());
                    return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                            .entity(Map.of("error", error.getMessage()))
                            .build();
                });
    }

    // ========== Statistics Endpoints ==========

    /**
     * Get aggregated statistics across all registries
     *
     * GET /api/v11/registries/stats
     *
     * @return aggregated statistics
     */
    @GET
    @Path("/stats")
    @Operation(
        summary = "Get aggregated registry statistics",
        description = "Retrieve combined statistics across all registry types including totals, verification coverage, and health status"
    )
    @APIResponse(
        responseCode = "200",
        description = "Aggregated statistics",
        content = @Content(schema = @Schema(implementation = RegistryAggregation.class))
    )
    @APIResponse(responseCode = "500", description = "Internal server error")
    public Uni<Response> getAggregatedStats() {
        Log.infof("REST: Get aggregated registry statistics");

        return registryManagementService.getAggregatedStats()
                .map(stats -> Response.ok(stats).build())
                .onFailure().recoverWithItem(error -> {
                    Log.errorf("Error getting aggregated stats: %s", error.getMessage());
                    return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                            .entity(Map.of("error", error.getMessage()))
                            .build();
                });
    }

    /**
     * Get statistics for a specific registry type
     *
     * GET /api/v11/registries/stats/{type}
     *
     * @param type the registry type (smart-contract, token, rwa, merkle-tree, compliance)
     * @return statistics for that registry type
     */
    @GET
    @Path("/stats/{type}")
    @Operation(
        summary = "Get statistics for a specific registry type",
        description = "Retrieve detailed statistics for a particular registry type"
    )
    @APIResponse(
        responseCode = "200",
        description = "Registry type statistics",
        content = @Content(schema = @Schema(implementation = RegistryStatistics.class))
    )
    @APIResponse(responseCode = "400", description = "Invalid registry type")
    @APIResponse(responseCode = "500", description = "Internal server error")
    public Uni<Response> getTypeStats(@PathParam("type") String type) {
        Log.infof("REST: Get statistics for registry type: %s", type);

        if (!RegistryType.isValid(type)) {
            String errorMsg = String.format("Invalid registry type: %s. Valid types: %s", type, VALID_TYPES);
            Log.warnf(errorMsg);
            return Uni.createFrom().item(
                Response.status(Response.Status.BAD_REQUEST)
                        .entity(Map.of("error", errorMsg))
                        .build()
            );
        }

        return registryManagementService.getRegistryStats(type)
                .map(stats -> Response.ok(stats).build())
                .onFailure().recoverWithItem(error -> {
                    Log.errorf("Error getting registry stats: %s", error.getMessage());
                    return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                            .entity(Map.of("error", error.getMessage()))
                            .build();
                });
    }

    // ========== List Endpoints ==========

    /**
     * List all entries of a specific registry type
     *
     * GET /api/v11/registries/list/{type}?limit=50&offset=0
     *
     * @param type the registry type (smart-contract, token, rwa, merkle-tree, compliance)
     * @param limit maximum number of results (default: 50, max: 500)
     * @param offset pagination offset (default: 0)
     * @return list of entries for that type
     */
    @GET
    @Path("/list/{type}")
    @Operation(
        summary = "List entries by registry type",
        description = "Retrieve paginated list of all entries in a specific registry type"
    )
    @APIResponse(
        responseCode = "200",
        description = "List of entries",
        content = @Content(schema = @Schema(implementation = RegistrySearchResult[].class))
    )
    @APIResponse(responseCode = "400", description = "Invalid registry type")
    @APIResponse(responseCode = "500", description = "Internal server error")
    public Uni<Response> listByType(
            @PathParam("type") String type,
            @QueryParam("limit") @DefaultValue("50") int limit,
            @QueryParam("offset") @DefaultValue("0") int offset) {

        Log.infof("REST: List registry entries - type: %s, limit: %d, offset: %d", type, limit, offset);

        if (!RegistryType.isValid(type)) {
            String errorMsg = String.format("Invalid registry type: %s. Valid types: %s", type, VALID_TYPES);
            Log.warnf(errorMsg);
            return Uni.createFrom().item(
                Response.status(Response.Status.BAD_REQUEST)
                        .entity(Map.of("error", errorMsg))
                        .build()
            );
        }

        return registryManagementService.listByType(type, limit, offset)
                .map(entries -> Response.ok(entries).build())
                .onFailure().recoverWithItem(error -> {
                    Log.errorf("Error listing registry entries: %s", error.getMessage());
                    return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                            .entity(Map.of("error", error.getMessage()))
                            .build();
                });
    }

    // ========== Verification Endpoints ==========

    /**
     * Verify an entry across all registries
     *
     * GET /api/v11/registries/verify/{entryId}
     *
     * Searches for the entry ID across all registry types and reports where it was found.
     *
     * @param entryId the entry ID to verify
     * @return verification results showing which registries contain the entry
     */
    @GET
    @Path("/verify/{entryId}")
    @Operation(
        summary = "Verify entry across registries",
        description = "Check if an entry exists in any of the registered registries"
    )
    @APIResponse(
        responseCode = "200",
        description = "Verification results",
        content = @Content(schema = @Schema(implementation = java.util.Map.class))
    )
    @APIResponse(responseCode = "500", description = "Internal server error")
    public Uni<Response> verifyEntry(@PathParam("entryId") String entryId) {
        Log.infof("REST: Verify entry - %s", entryId);

        if (entryId == null || entryId.trim().isEmpty()) {
            return Uni.createFrom().item(
                Response.status(Response.Status.BAD_REQUEST)
                        .entity(Map.of("error", "Entry ID cannot be empty"))
                        .build()
            );
        }

        return registryManagementService.verifyEntry(entryId)
                .map(results -> Response.ok(results).build())
                .onFailure().recoverWithItem(error -> {
                    Log.errorf("Error verifying entry: %s", error.getMessage());
                    return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                            .entity(Map.of("error", error.getMessage()))
                            .build();
                });
    }

    // ========== Summary Endpoints ==========

    /**
     * Get overall registry summary
     *
     * GET /api/v11/registries/summary
     *
     * @return summary information across all registries
     */
    @GET
    @Path("/summary")
    @Operation(
        summary = "Get registry summary",
        description = "Retrieve high-level overview of all registries including entry counts, health status, and verification coverage"
    )
    @APIResponse(
        responseCode = "200",
        description = "Registry summary",
        content = @Content(schema = @Schema(implementation = java.util.Map.class))
    )
    @APIResponse(responseCode = "500", description = "Internal server error")
    public Uni<Response> getRegistrySummary() {
        Log.infof("REST: Get registry summary");

        return registryManagementService.getRegistrySummary()
                .map(summary -> Response.ok(summary).build())
                .onFailure().recoverWithItem(error -> {
                    Log.errorf("Error getting registry summary: %s", error.getMessage());
                    return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                            .entity(Map.of("error", error.getMessage()))
                            .build();
                });
    }

    // ========== Info Endpoints ==========

    /**
     * Get supported registry types
     *
     * GET /api/v11/registries/info/types
     *
     * @return list of supported registry types
     */
    @GET
    @Path("/info/types")
    @Operation(
        summary = "Get supported registry types",
        description = "List all available registry types that can be queried"
    )
    @APIResponse(
        responseCode = "200",
        description = "Registry type information"
    )
    public Response getSupportedTypes() {
        Log.infof("REST: Get supported registry types");

        Map<String, Object> result = new HashMap<>();
        result.put("count", RegistryType.values().length);
        result.put("types", Arrays.stream(RegistryType.values())
                .map(t -> Map.of(
                    "id", t.getId(),
                    "displayName", t.getDisplayName()
                ))
                .collect(Collectors.toList()));

        return Response.ok(result).build();
    }

    /**
     * Health check endpoint
     *
     * GET /api/v11/registries/health
     *
     * @return health status of the registry management API
     */
    @GET
    @Path("/health")
    @Operation(summary = "Health check", description = "Check if registry management API is operational")
    @APIResponse(responseCode = "200", description = "API is healthy")
    public Response health() {
        Map<String, String> healthStatus = Map.of(
            "status", "UP",
            "service", "Registry Management API",
            "version", "12.0.0"
        );
        return Response.ok(healthStatus).build();
    }
}
