package io.aurigraph.v11.registries.smartcontract;

import io.quarkus.logging.Log;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.*;

/**
 * Smart Contract Registry REST API Resource
 *
 * Provides RESTful endpoints for managing smart contract registrations.
 * All endpoints return Uni<Response> for reactive operations and proper HTTP status codes.
 * Base path: /api/v11/registries/smart-contract
 *
 * @version 11.5.0
 * @since 2025-11-14
 */
@Path("/api/v11/registries/smart-contract")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Smart Contract Registry", description = "Smart Contract Registry API - Contract management and lifecycle")
public class SmartContractRegistryResource {

    @Inject
    SmartContractRegistryService registryService;

    // ========== Core Registry Endpoints ==========

    /**
     * Register a new smart contract
     *
     * POST /api/v11/registries/smart-contract/register
     * Response: 201 Created
     */
    @POST
    @Path("/register")
    @Operation(
        summary = "Register Smart Contract",
        description = "Register a new smart contract in the registry with deployment information"
    )
    public Uni<Response> registerContract(Map<String, Object> request) {
        Log.infof("REST: Register contract request - %s", request.get("contractName"));

        try {
            // Extract request parameters
            String contractId = (String) request.get("contractId");
            String contractName = (String) request.get("contractName");
            String description = (String) request.get("description");
            String deploymentAddress = (String) request.get("deploymentAddress");
            String deploymentTxHash = (String) request.get("deploymentTxHash");
            String codeHash = (String) request.get("codeHash");
            String status = (String) request.getOrDefault("status", "DRAFT");

            // Validate required fields
            if (contractId == null || contractId.isBlank()) {
                return Uni.createFrom().item(Response.status(Response.Status.BAD_REQUEST)
                        .entity(Map.of("error", "contractId is required"))
                        .build());
            }

            return registryService.registerContract(
                    contractId,
                    contractName,
                    description,
                    deploymentAddress,
                    deploymentTxHash,
                    codeHash,
                    status
            )
                    .map(entry -> Response.status(Response.Status.CREATED)
                            .entity(Map.of(
                                    "success", true,
                                    "message", "Contract registered successfully",
                                    "contract", entry
                            ))
                            .build())
                    .onFailure().recoverWithItem(error -> {
                        Log.errorf("Contract registration failed: %s", error.getMessage());
                        return Response.status(Response.Status.CONFLICT)
                                .entity(Map.of("error", error.getMessage()))
                                .build();
                    });
        } catch (Exception e) {
            Log.errorf("Unexpected error during contract registration: %s", e.getMessage());
            return Uni.createFrom().item(Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", e.getMessage()))
                    .build());
        }
    }

    /**
     * Get contract details by ID
     *
     * GET /api/v11/registries/smart-contract/{contractId}
     * Response: 200 OK or 404 Not Found
     */
    @GET
    @Path("/{contractId}")
    @Operation(
        summary = "Get Contract Details",
        description = "Retrieve detailed information about a registered contract"
    )
    public Uni<Response> getContractDetails(@PathParam("contractId") String contractId) {
        Log.infof("REST: Get contract details - %s", contractId);

        return registryService.getContractDetails(contractId)
                .map(entry -> Response.ok(Map.of(
                        "success", true,
                        "contract", entry
                ))
                        .build())
                .onFailure().recoverWithItem(error ->
                        Response.status(Response.Status.NOT_FOUND)
                                .entity(Map.of("error", error.getMessage()))
                                .build()
                );
    }

    /**
     * Search contracts by name and/or status
     *
     * GET /api/v11/registries/smart-contract/search?name=&status=&limit=10&offset=0
     * Response: 200 OK
     */
    @GET
    @Path("/search")
    @Operation(
        summary = "Search Contracts",
        description = "Search contracts by name and status with pagination"
    )
    public Uni<Response> searchContracts(
            @QueryParam("name") String name,
            @QueryParam("status") String status,
            @QueryParam("limit") @DefaultValue("10") int limit,
            @QueryParam("offset") @DefaultValue("0") int offset) {
        Log.infof("REST: Search contracts - name:%s, status:%s, limit:%d, offset:%d", name, status, limit, offset);

        return registryService.searchContracts(name, status, limit, offset)
                .map(contracts -> Response.ok(Map.of(
                        "success", true,
                        "count", contracts.size(),
                        "limit", limit,
                        "offset", offset,
                        "contracts", contracts
                ))
                        .build())
                .onFailure().recoverWithItem(error ->
                        Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                                .entity(Map.of("error", error.getMessage()))
                                .build()
                );
    }

    /**
     * Get linked assets for a contract
     *
     * GET /api/v11/registries/smart-contract/{contractId}/assets
     * Response: 200 OK or 404 Not Found
     */
    @GET
    @Path("/{contractId}/assets")
    @Operation(
        summary = "Get Linked Assets",
        description = "Retrieve all assets linked to a smart contract"
    )
    public Uni<Response> getLinkedAssets(@PathParam("contractId") String contractId) {
        Log.infof("REST: Get linked assets - %s", contractId);

        return registryService.getLinkedAssets(contractId)
                .map(assets -> Response.ok(Map.of(
                        "success", true,
                        "contractId", contractId,
                        "assetCount", assets.size(),
                        "assets", assets
                ))
                        .build())
                .onFailure().recoverWithItem(error ->
                        Response.status(Response.Status.NOT_FOUND)
                                .entity(Map.of("error", error.getMessage()))
                                .build()
                );
    }

    /**
     * Update contract status
     *
     * PUT /api/v11/registries/smart-contract/{contractId}/status
     * Response: 200 OK or 404 Not Found
     */
    @PUT
    @Path("/{contractId}/status")
    @Operation(
        summary = "Update Contract Status",
        description = "Update the lifecycle status of a registered contract"
    )
    public Uni<Response> updateContractStatus(
            @PathParam("contractId") String contractId,
            Map<String, String> request) {
        Log.infof("REST: Update contract status - %s", contractId);

        String newStatus = request.get("status");
        if (newStatus == null || newStatus.isBlank()) {
            return Uni.createFrom().item(Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "status field is required"))
                    .build());
        }

        return registryService.updateContractStatus(contractId, newStatus)
                .map(entry -> Response.ok(Map.of(
                        "success", true,
                        "message", "Status updated successfully",
                        "contract", entry
                ))
                        .build())
                .onFailure().recoverWithItem(error -> {
                    Log.warnf("Status update failed: %s", error.getMessage());
                    return Response.status(Response.Status.BAD_REQUEST)
                            .entity(Map.of("error", error.getMessage()))
                            .build();
                });
    }

    /**
     * Remove contract from registry
     *
     * DELETE /api/v11/registries/smart-contract/{contractId}
     * Response: 200 OK or 404 Not Found
     */
    @DELETE
    @Path("/{contractId}")
    @Operation(
        summary = "Remove Contract",
        description = "Remove a smart contract from the registry"
    )
    public Uni<Response> removeContract(@PathParam("contractId") String contractId) {
        Log.infof("REST: Remove contract - %s", contractId);

        return registryService.removeContract(contractId)
                .map(success -> Response.ok(Map.of(
                        "success", true,
                        "message", "Contract removed successfully",
                        "contractId", contractId
                ))
                        .build())
                .onFailure().recoverWithItem(error ->
                        Response.status(Response.Status.NOT_FOUND)
                                .entity(Map.of("error", error.getMessage()))
                                .build()
                );
    }

    // ========== Asset Linking Endpoints ==========

    /**
     * Link an asset to a contract
     *
     * POST /api/v11/registries/smart-contract/{contractId}/assets/{assetId}
     * Response: 200 OK
     */
    @POST
    @Path("/{contractId}/assets/{assetId}")
    @Operation(
        summary = "Link Asset to Contract",
        description = "Link a real-world asset to a smart contract"
    )
    public Uni<Response> linkAsset(
            @PathParam("contractId") String contractId,
            @PathParam("assetId") String assetId) {
        Log.infof("REST: Link asset - contract:%s, asset:%s", contractId, assetId);

        return registryService.linkAsset(contractId, assetId)
                .map(entry -> Response.ok(Map.of(
                        "success", true,
                        "message", "Asset linked successfully",
                        "contract", entry
                ))
                        .build())
                .onFailure().recoverWithItem(error ->
                        Response.status(Response.Status.BAD_REQUEST)
                                .entity(Map.of("error", error.getMessage()))
                                .build()
                );
    }

    /**
     * Unlink an asset from a contract
     *
     * DELETE /api/v11/registries/smart-contract/{contractId}/assets/{assetId}
     * Response: 200 OK
     */
    @DELETE
    @Path("/{contractId}/assets/{assetId}")
    @Operation(
        summary = "Unlink Asset from Contract",
        description = "Remove the link between an asset and a smart contract"
    )
    public Uni<Response> unlinkAsset(
            @PathParam("contractId") String contractId,
            @PathParam("assetId") String assetId) {
        Log.infof("REST: Unlink asset - contract:%s, asset:%s", contractId, assetId);

        return registryService.unlinkAsset(contractId, assetId)
                .map(entry -> Response.ok(Map.of(
                        "success", true,
                        "message", "Asset unlinked successfully",
                        "contract", entry
                ))
                        .build())
                .onFailure().recoverWithItem(error ->
                        Response.status(Response.Status.BAD_REQUEST)
                                .entity(Map.of("error", error.getMessage()))
                                .build()
                );
    }

    /**
     * Get contracts for a specific asset
     *
     * GET /api/v11/registries/smart-contract/asset/{assetId}/contracts
     * Response: 200 OK
     */
    @GET
    @Path("/asset/{assetId}/contracts")
    @Operation(
        summary = "Get Contracts for Asset",
        description = "Retrieve all contracts linked to a specific asset"
    )
    public Uni<Response> getContractsForAsset(@PathParam("assetId") String assetId) {
        Log.infof("REST: Get contracts for asset - %s", assetId);

        return registryService.getContractsForAsset(assetId)
                .map(contracts -> Response.ok(Map.of(
                        "success", true,
                        "assetId", assetId,
                        "contractCount", contracts.size(),
                        "contracts", contracts
                ))
                        .build())
                .onFailure().recoverWithItem(error ->
                        Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                                .entity(Map.of("error", error.getMessage()))
                                .build()
                );
    }

    // ========== Statistics and Audit Endpoints ==========

    /**
     * Get contract registry statistics
     *
     * GET /api/v11/registries/smart-contract/statistics
     * Response: 200 OK
     */
    @GET
    @Path("/statistics")
    @Operation(
        summary = "Get Registry Statistics",
        description = "Retrieve comprehensive statistics about the contract registry"
    )
    public Uni<Response> getStatistics() {
        Log.info("REST: Get contract statistics");

        return registryService.getContractStatistics()
                .map(stats -> Response.ok(Map.of(
                        "success", true,
                        "statistics", stats
                ))
                        .build())
                .onFailure().recoverWithItem(error -> {
                    Log.warnf("Failed to get statistics: %s", error.getMessage());
                    return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                            .entity(Map.of("error", error.getMessage()))
                            .build();
                });
    }

    /**
     * Get audit trail for a contract
     *
     * GET /api/v11/registries/smart-contract/{contractId}/audit
     * Response: 200 OK or 404 Not Found
     */
    @GET
    @Path("/{contractId}/audit")
    @Operation(
        summary = "Get Audit Trail",
        description = "Retrieve the complete audit trail for a contract"
    )
    public Uni<Response> getAuditTrail(@PathParam("contractId") String contractId) {
        Log.infof("REST: Get audit trail - %s", contractId);

        return registryService.getAuditTrail(contractId)
                .map(trail -> Response.ok(Map.of(
                        "success", true,
                        "contractId", contractId,
                        "entryCount", trail.size(),
                        "auditTrail", trail
                ))
                        .build())
                .onFailure().recoverWithItem(error ->
                        Response.status(Response.Status.NOT_FOUND)
                                .entity(Map.of("error", error.getMessage()))
                                .build()
                );
    }

    // ========== Health and Info Endpoints ==========

    /**
     * Get registry health status
     *
     * GET /api/v11/registries/smart-contract/health
     * Response: 200 OK
     */
    @GET
    @Path("/health")
    @Operation(
        summary = "Registry Health",
        description = "Check the health status of the contract registry"
    )
    public Response getHealth() {
        return Response.ok(Map.of(
                "status", "healthy",
                "service", "SmartContractRegistry",
                "version", "12.0.0",
                "timestamp", new Date().toString()
        )).build();
    }

    /**
     * Get registry API information
     *
     * GET /api/v11/registries/smart-contract/info
     * Response: 200 OK
     */
    @GET
    @Path("/info")
    @Operation(
        summary = "Registry Information",
        description = "Get information about the Smart Contract Registry API"
    )
    public Response getInfo() {
        // Create endpoints map with LinkedHashMap to support 11+ entries
        // (Map.of() has a 10-entry limit)
        Map<String, String> endpoints = new LinkedHashMap<>();
        endpoints.put("register", "POST /register");
        endpoints.put("getContract", "GET /{contractId}");
        endpoints.put("search", "GET /search");
        endpoints.put("getAssets", "GET /{contractId}/assets");
        endpoints.put("updateStatus", "PUT /{contractId}/status");
        endpoints.put("removeContract", "DELETE /{contractId}");
        endpoints.put("linkAsset", "POST /{contractId}/assets/{assetId}");
        endpoints.put("unlinkAsset", "DELETE /{contractId}/assets/{assetId}");
        endpoints.put("getContractsForAsset", "GET /asset/{assetId}/contracts");
        endpoints.put("statistics", "GET /statistics");
        endpoints.put("auditTrail", "GET /{contractId}/audit");

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("service", "Smart Contract Registry");
        response.put("version", "12.0.0");
        response.put("basePath", "/api/v11/registries/smart-contract");
        response.put("endpoints", endpoints);
        response.put("features", List.of(
                "Contract Registration",
                "Deployment Tracking",
                "Asset Linking",
                "Status Management",
                "Audit Trail",
                "Search and Filtering",
                "Statistics and Analytics"
        ));

        return Response.ok(response).build();
    }
}
