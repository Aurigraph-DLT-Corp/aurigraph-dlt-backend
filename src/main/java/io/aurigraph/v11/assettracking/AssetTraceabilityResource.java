package io.aurigraph.v11.assettracking;

import io.quarkus.logging.Log;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;

import java.util.List;
import java.util.Map;

/**
 * Asset Traceability REST Resource
 *
 * Provides comprehensive REST endpoints for tracking the complete lifecycle
 * of tokenized assets with full audit trails and ownership history.
 *
 * Base Path: /api/v11/assets/traceability
 *
 * Features:
 * - Create and manage asset traces
 * - Track ownership transfers with percentage splits
 * - Comprehensive audit trail logging
 * - Search and filter capabilities
 * - Ownership history tracking
 *
 * @version 1.0.0
 * @author Aurigraph V11 Development Team
 */
@Path("/api/v11/assets/traceability")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@ApplicationScoped
@Tag(name = "Asset Traceability", description = "Asset lifecycle and ownership tracking")
public class AssetTraceabilityResource {

    @Inject
    AssetTraceabilityService assetTraceabilityService;

    /**
     * Create a new asset trace
     *
     * POST /api/v11/assets/traceability/create
     *
     * @param assetId Unique asset identifier
     * @param assetName Human-readable asset name
     * @param assetType Asset type (REAL_ESTATE, COMMODITY, SECURITY, ART, CARBON_CREDIT, etc.)
     * @param valuation Asset valuation in base currency
     * @param owner Current owner identifier
     * @return Created AssetTrace with HTTP 201
     */
    @POST
    @Path("/create")
    @Operation(summary = "Create new asset trace", description = "Initialize a new asset trace with ownership record")
    @APIResponse(responseCode = "201", description = "Asset trace created successfully")
    @APIResponse(responseCode = "400", description = "Invalid parameters")
    public Uni<Response> createAssetTrace(
            @QueryParam("assetId") String assetId,
            @QueryParam("assetName") String assetName,
            @QueryParam("assetType") String assetType,
            @QueryParam("valuation") Double valuation,
            @QueryParam("owner") String owner) {

        // Input validation
        if (assetId == null || assetId.isEmpty() || assetName == null || assetName.isEmpty() ||
            assetType == null || assetType.isEmpty() || valuation == null || owner == null || owner.isEmpty()) {
            Log.warn("Invalid parameters for asset trace creation");
            return Uni.createFrom().item(
                Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "Missing required parameters"))
                    .build()
            );
        }

        return assetTraceabilityService.createAssetTrace(assetId, assetName, assetType, valuation, owner)
            .map(trace -> {
                Log.infof("Created asset trace: %s for asset: %s", trace.getTraceId(), assetId);
                return Response.status(Response.Status.CREATED).entity(trace).build();
            })
            .onFailure().recoverWithItem(e -> {
                Log.errorf("Error creating asset trace for %s: %s", assetId, e.getMessage());
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Failed to create asset trace: " + e.getMessage()))
                    .build();
            });
    }

    /**
     * Get asset trace details
     *
     * GET /api/v11/assets/traceability/{traceId}
     *
     * @param traceId Trace identifier
     * @return AssetTrace details or HTTP 404
     */
    @GET
    @Path("/{traceId}")
    @Operation(summary = "Get asset trace", description = "Retrieve complete asset trace with all metadata")
    @APIResponse(responseCode = "200", description = "Asset trace found")
    @APIResponse(responseCode = "404", description = "Asset trace not found")
    public Uni<Response> getAssetTrace(@PathParam("traceId") String traceId) {
        return assetTraceabilityService.getAssetTrace(traceId)
            .map(optionalTrace -> {
                if (optionalTrace.isPresent()) {
                    Log.debugf("Retrieved asset trace: %s", traceId);
                    return Response.ok(optionalTrace.get()).build();
                } else {
                    Log.warnf("Asset trace not found: %s", traceId);
                    return Response.status(Response.Status.NOT_FOUND)
                        .entity(Map.of("error", "Asset trace not found"))
                        .build();
                }
            })
            .onFailure().recoverWithItem(e -> {
                Log.errorf("Error retrieving asset trace %s: %s", traceId, e.getMessage());
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Failed to retrieve asset trace"))
                    .build();
            });
    }

    /**
     * Search assets with filters
     *
     * GET /api/v11/assets/traceability/search
     *
     * @param assetType Filter by asset type (optional)
     * @param owner Filter by owner (optional)
     * @param minVal Minimum valuation filter (optional)
     * @param maxVal Maximum valuation filter (optional)
     * @param limit Number of results to return (default: 50)
     * @param offset Pagination offset (default: 0)
     * @return List of matching AssetTrace objects
     */
    @GET
    @Path("/search")
    @Operation(summary = "Search assets", description = "Search and filter assets by criteria")
    @APIResponse(responseCode = "200", description = "Search results returned")
    public Uni<Response> searchAssets(
            @QueryParam("assetType") String assetType,
            @QueryParam("owner") String owner,
            @QueryParam("minVal") Double minVal,
            @QueryParam("maxVal") Double maxVal,
            @DefaultValue("50") @QueryParam("limit") int limit,
            @DefaultValue("0") @QueryParam("offset") int offset) {

        // Validate pagination parameters
        if (limit < 1 || limit > 1000) {
            limit = 50;
        }
        if (offset < 0) {
            offset = 0;
        }

        return assetTraceabilityService.searchAssets(assetType, owner, minVal, maxVal, limit, offset)
            .map(results -> {
                Log.debugf("Asset search: assetType=%s, owner=%s, results=%d", assetType, owner, results.size());
                return Response.ok(results).build();
            })
            .onFailure().recoverWithItem(e -> {
                Log.errorf("Error searching assets: %s", e.getMessage());
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Failed to search assets"))
                    .build();
            });
    }

    /**
     * Record ownership transfer
     *
     * POST /api/v11/assets/traceability/{traceId}/transfer
     *
     * @param traceId Trace identifier
     * @param fromOwner Current owner
     * @param toOwner New owner
     * @param percentage Ownership percentage being transferred (0-100)
     * @return Updated OwnershipRecord
     */
    @POST
    @Path("/{traceId}/transfer")
    @Operation(summary = "Transfer ownership", description = "Record an ownership transfer event")
    @APIResponse(responseCode = "200", description = "Transfer recorded successfully")
    @APIResponse(responseCode = "400", description = "Invalid transfer parameters")
    @APIResponse(responseCode = "404", description = "Asset trace not found")
    public Uni<Response> transferOwnership(
            @PathParam("traceId") String traceId,
            @QueryParam("fromOwner") String fromOwner,
            @QueryParam("toOwner") String toOwner,
            @QueryParam("percentage") Double percentage) {

        // Input validation
        if (fromOwner == null || fromOwner.isEmpty() || toOwner == null || toOwner.isEmpty() ||
            percentage == null || percentage <= 0 || percentage > 100) {
            Log.warn("Invalid transfer parameters");
            return Uni.createFrom().item(
                Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "Invalid transfer parameters"))
                    .build()
            );
        }

        return assetTraceabilityService.transferOwnership(traceId, fromOwner, toOwner, percentage)
            .map(result -> {
                if (result != null) {
                    Log.infof("Ownership transfer recorded: %s -> %s (%.1f%%)", fromOwner, toOwner, percentage);
                    return Response.ok(result).build();
                } else {
                    return Response.status(Response.Status.NOT_FOUND)
                        .entity(Map.of("error", "Asset trace not found"))
                        .build();
                }
            })
            .onFailure().recoverWithItem(e -> {
                Log.errorf("Error recording ownership transfer: %s", e.getMessage());
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Failed to record transfer"))
                    .build();
            });
    }

    /**
     * Get ownership history chain
     *
     * GET /api/v11/assets/traceability/{traceId}/history
     *
     * @param traceId Trace identifier
     * @return List of OwnershipRecord objects in chronological order
     */
    @GET
    @Path("/{traceId}/history")
    @Operation(summary = "Get ownership history", description = "Retrieve complete ownership change history")
    @APIResponse(responseCode = "200", description = "Ownership history retrieved")
    @APIResponse(responseCode = "404", description = "Asset trace not found")
    public Uni<Response> getOwnershipHistory(@PathParam("traceId") String traceId) {
        return assetTraceabilityService.getOwnershipHistory(traceId)
            .map(optionalHistory -> {
                if (optionalHistory.isPresent()) {
                    List<OwnershipRecord> history = optionalHistory.get();
                    Log.debugf("Retrieved ownership history for %s: %d records", traceId, history.size());
                    return Response.ok(history).build();
                } else {
                    return Response.status(Response.Status.NOT_FOUND)
                        .entity(Map.of("error", "Asset trace not found"))
                        .build();
                }
            })
            .onFailure().recoverWithItem(e -> {
                Log.errorf("Error retrieving ownership history: %s", e.getMessage());
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Failed to retrieve ownership history"))
                    .build();
            });
    }

    /**
     * Get audit trail
     *
     * GET /api/v11/assets/traceability/{traceId}/audit
     *
     * @param traceId Trace identifier
     * @return List of AuditTrailEntry objects in chronological order
     */
    @GET
    @Path("/{traceId}/audit")
    @Operation(summary = "Get audit trail", description = "Retrieve complete audit trail of all actions")
    @APIResponse(responseCode = "200", description = "Audit trail retrieved")
    @APIResponse(responseCode = "404", description = "Asset trace not found")
    public Uni<Response> getAuditTrail(@PathParam("traceId") String traceId) {
        return assetTraceabilityService.getAuditTrail(traceId)
            .map(optionalAudit -> {
                if (optionalAudit.isPresent()) {
                    List<AuditTrailEntry> audit = optionalAudit.get();
                    Log.debugf("Retrieved audit trail for %s: %d entries", traceId, audit.size());
                    return Response.ok(audit).build();
                } else {
                    return Response.status(Response.Status.NOT_FOUND)
                        .entity(Map.of("error", "Asset trace not found"))
                        .build();
                }
            })
            .onFailure().recoverWithItem(e -> {
                Log.errorf("Error retrieving audit trail: %s", e.getMessage());
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Failed to retrieve audit trail"))
                    .build();
            });
    }

    /**
     * Health check endpoint
     *
     * @return Service status
     */
    @GET
    @Path("/health")
    @Operation(summary = "Health check", description = "Check service health status")
    public Response healthCheck() {
        return Response.ok(Map.of("status", "UP", "service", "AssetTraceability")).build();
    }
}
