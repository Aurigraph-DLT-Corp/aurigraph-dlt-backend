package io.aurigraph.v11.contracts.traceability;

import io.quarkus.logging.Log;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.Map;

/**
 * Contract-Asset Traceability REST Resource
 *
 * Provides REST endpoints for querying and managing contract-asset traceability links.
 * Enables complete visibility into the lineage from contracts through assets to tokens.
 *
 * Base Path: /api/v11/traceability
 *
 * @version 1.0.0
 * @author Aurigraph V11 Development Team
 */
@Path("/api/v11/traceability")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ContractAssetTraceabilityResource {

    @Inject
    ContractAssetTraceabilityService traceabilityService;

    /**
     * Create a new contract-asset traceability link
     *
     * @param contractId Contract identifier
     * @param contractName Contract name
     * @param assetId Asset identifier
     * @param assetName Asset name
     * @param assetType Asset type (e.g., REAL_ESTATE, COMMODITY, SECURITY)
     * @param assetValuation Asset valuation in USD
     * @param tokenId Token identifier
     * @param tokenSymbol Token symbol
     * @return Created ContractAssetLink
     */
    @POST
    @Path("/links")
    public Uni<Response> createTraceabilityLink(
            @QueryParam("contractId") String contractId,
            @QueryParam("contractName") String contractName,
            @QueryParam("assetId") String assetId,
            @QueryParam("assetName") String assetName,
            @QueryParam("assetType") String assetType,
            @QueryParam("assetValuation") Double assetValuation,
            @QueryParam("tokenId") String tokenId,
            @QueryParam("tokenSymbol") String tokenSymbol) {

        return traceabilityService.linkContractToAsset(
                contractId, contractName, assetId, assetName, assetType,
                assetValuation, tokenId, tokenSymbol
        ).map(link -> {
            Log.info("Created traceability link: " + link.getLinkId());
            return Response.status(Response.Status.CREATED).entity(link).build();
        }).onFailure().recoverWithItem(e -> {
            Log.error("Error creating traceability link", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Failed to create traceability link"))
                    .build();
        });
    }

    /**
     * Get all assets linked to a specific contract
     *
     * @param contractId Contract identifier
     * @return List of ContractAssetLink objects
     */
    @GET
    @Path("/contracts/{contractId}/assets")
    public Uni<Response> getAssetsByContract(@PathParam("contractId") String contractId) {
        return traceabilityService.getAssetsByContract(contractId)
                .map(assets -> Response.ok(assets).build())
                .onFailure().recoverWithItem(e -> {
                    Log.error("Error fetching assets for contract: " + contractId, e);
                    return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                            .entity(Map.of("error", "Failed to fetch assets"))
                            .build();
                });
    }

    /**
     * Get all contracts linked to a specific asset
     *
     * @param assetId Asset identifier
     * @return List of ContractAssetLink objects
     */
    @GET
    @Path("/assets/{assetId}/contracts")
    public Uni<Response> getContractsByAsset(@PathParam("assetId") String assetId) {
        return traceabilityService.getContractsByAsset(assetId)
                .map(contracts -> Response.ok(contracts).build())
                .onFailure().recoverWithItem(e -> {
                    Log.error("Error fetching contracts for asset: " + assetId, e);
                    return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                            .entity(Map.of("error", "Failed to fetch contracts"))
                            .build();
                });
    }

    /**
     * Get complete lineage from contract to assets to tokens
     *
     * @param contractId Contract identifier
     * @return ContractAssetLineage with all linked assets and aggregated metrics
     */
    @GET
    @Path("/contracts/{contractId}/lineage")
    public Uni<Response> getCompleteLineage(@PathParam("contractId") String contractId) {
        return traceabilityService.getCompleteLineage(contractId)
                .map(lineage -> Response.ok(lineage).build())
                .onFailure().recoverWithItem(e -> {
                    Log.error("Error fetching lineage for contract: " + contractId, e);
                    return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                            .entity(Map.of("error", "Failed to fetch lineage"))
                            .build();
                });
    }

    /**
     * Get specific traceability link by linkId
     *
     * @param linkId Link identifier
     * @return ContractAssetLink or 404 if not found
     */
    @GET
    @Path("/links/{linkId}")
    public Uni<Response> getTraceabilityLink(@PathParam("linkId") String linkId) {
        return traceabilityService.getTraceabilityLink(linkId)
                .map(optionalLink -> {
                    if (optionalLink.isPresent()) {
                        return Response.ok(optionalLink.get()).build();
                    } else {
                        return Response.status(Response.Status.NOT_FOUND)
                                .entity(Map.of("error", "Link not found"))
                                .build();
                    }
                })
                .onFailure().recoverWithItem(e -> {
                    Log.error("Error fetching traceability link: " + linkId, e);
                    return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                            .entity(Map.of("error", "Failed to fetch link"))
                            .build();
                });
    }

    /**
     * Record contract execution and update success metrics
     *
     * @param linkId Link identifier
     * @param success Whether execution was successful
     * @return Updated ContractAssetLink
     */
    @POST
    @Path("/links/{linkId}/execute")
    public Uni<Response> recordContractExecution(
            @PathParam("linkId") String linkId,
            @QueryParam("success") boolean success) {

        return traceabilityService.recordContractExecution(linkId, success)
                .map(link -> {
                    if (link != null) {
                        Log.info("Recorded execution for link: " + linkId);
                        return Response.ok(link).build();
                    } else {
                        return Response.status(Response.Status.NOT_FOUND)
                                .entity(Map.of("error", "Link not found"))
                                .build();
                    }
                })
                .onFailure().recoverWithItem(e -> {
                    Log.error("Error recording execution for link: " + linkId, e);
                    return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                            .entity(Map.of("error", "Failed to record execution"))
                            .build();
                });
    }

    /**
     * Update asset valuation and record history
     *
     * @param linkId Link identifier
     * @param newValuation New asset valuation in USD
     * @return Updated ContractAssetLink
     */
    @PUT
    @Path("/links/{linkId}/valuation")
    public Uni<Response> updateAssetValuation(
            @PathParam("linkId") String linkId,
            @QueryParam("valuation") Double newValuation) {

        return traceabilityService.updateAssetValuation(linkId, newValuation)
                .map(link -> {
                    if (link != null) {
                        Log.info("Updated valuation for link: " + linkId);
                        return Response.ok(link).build();
                    } else {
                        return Response.status(Response.Status.NOT_FOUND)
                                .entity(Map.of("error", "Link not found"))
                                .build();
                    }
                })
                .onFailure().recoverWithItem(e -> {
                    Log.error("Error updating valuation for link: " + linkId, e);
                    return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                            .entity(Map.of("error", "Failed to update valuation"))
                            .build();
                });
    }

    /**
     * Update tokenization details (shares and status)
     *
     * @param linkId Link identifier
     * @param totalShares Total shares issued
     * @param sharesOutstanding Shares still outstanding
     * @return Updated ContractAssetLink
     */
    @PUT
    @Path("/links/{linkId}/tokenization")
    public Uni<Response> updateTokenizationDetails(
            @PathParam("linkId") String linkId,
            @QueryParam("totalShares") Long totalShares,
            @QueryParam("sharesOutstanding") Long sharesOutstanding) {

        return traceabilityService.updateTokenizationDetails(linkId, totalShares, sharesOutstanding, java.time.Instant.now())
                .map(link -> {
                    if (link != null) {
                        Log.info("Updated tokenization for link: " + linkId);
                        return Response.ok(link).build();
                    } else {
                        return Response.status(Response.Status.NOT_FOUND)
                                .entity(Map.of("error", "Link not found"))
                                .build();
                    }
                })
                .onFailure().recoverWithItem(e -> {
                    Log.error("Error updating tokenization for link: " + linkId, e);
                    return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                            .entity(Map.of("error", "Failed to update tokenization"))
                            .build();
                });
    }

    /**
     * Get system-wide traceability summary
     *
     * @return TraceabilitySummary with aggregate metrics
     */
    @GET
    @Path("/summary")
    public Uni<Response> getTraceabilitySummary() {
        return traceabilityService.getTraceabilitySummary()
                .map(summary -> Response.ok(summary).build())
                .onFailure().recoverWithItem(e -> {
                    Log.error("Error fetching traceability summary", e);
                    return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                            .entity(Map.of("error", "Failed to fetch summary"))
                            .build();
                });
    }

    /**
     * Search traceability links by criteria
     *
     * @param assetType Asset type filter (optional)
     * @param complianceStatus Compliance status filter (optional)
     * @param riskLevel Risk level filter (optional)
     * @return List of matching ContractAssetLink objects
     */
    @GET
    @Path("/search")
    public Uni<Response> searchLinks(
            @QueryParam("assetType") String assetType,
            @QueryParam("complianceStatus") String complianceStatus,
            @QueryParam("riskLevel") String riskLevel) {

        return traceabilityService.searchLinks(assetType, complianceStatus, riskLevel)
                .map(links -> Response.ok(links).build())
                .onFailure().recoverWithItem(e -> {
                    Log.error("Error searching traceability links", e);
                    return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                            .entity(Map.of("error", "Failed to search links"))
                            .build();
                });
    }

    /**
     * Verify contract-asset binding integrity
     *
     * @param linkId Link identifier
     * @return Verification result with status and details
     */
    @POST
    @Path("/links/{linkId}/verify")
    public Uni<Response> verifyIntegrity(@PathParam("linkId") String linkId) {
        return traceabilityService.verifyIntegrity(linkId)
                .map(verification -> Response.ok(verification).build())
                .onFailure().recoverWithItem(e -> {
                    Log.error("Error verifying integrity for link: " + linkId, e);
                    return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                            .entity(Map.of("error", "Failed to verify integrity"))
                            .build();
                });
    }

    /**
     * Health check endpoint for traceability service
     *
     * @return Service status
     */
    @GET
    @Path("/health")
    public Response healthCheck() {
        return Response.ok(Map.of("status", "UP", "service", "ContractAssetTraceability")).build();
    }
}
