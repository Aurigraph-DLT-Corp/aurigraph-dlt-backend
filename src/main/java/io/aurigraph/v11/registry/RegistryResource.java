package io.aurigraph.v11.registry;

import io.aurigraph.v11.contracts.ActiveContract;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * Registry REST API
 *
 * Public API for ActiveContract Registry and RWAT Registry.
 *
 * @version 11.4.0
 * @since 2025-10-13
 */
@Path("/api/v11/registry")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RegistryResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(RegistryResource.class);

    @Inject
    ActiveContractRegistryService contractRegistry;

    @Inject
    RWATRegistryService rwatRegistry;

    // ========== ActiveContract Registry Endpoints ==========

    /**
     * Search contracts
     *
     * GET /api/v11/registry/contracts/search?keyword=xxx
     */
    @GET
    @Path("/contracts/search")
    public Uni<Response> searchContracts(@QueryParam("keyword") String keyword) {
        LOGGER.info("REST: Search contracts - keyword: {}", keyword);

        return contractRegistry.searchContracts(keyword)
                .map(contracts -> Response.ok(contracts).build());
    }

    /**
     * Get contract by ID
     *
     * GET /api/v11/registry/contracts/{contractId}
     */
    @GET
    @Path("/contracts/{contractId}")
    public Uni<Response> getContract(@PathParam("contractId") String contractId) {
        LOGGER.info("REST: Get contract - {}", contractId);

        return contractRegistry.getContractPublic(contractId)
                .map(contract -> Response.ok(contract).build())
                .onFailure().recoverWithItem(error ->
                        Response.status(Response.Status.NOT_FOUND)
                                .entity(Map.of("error", error.getMessage()))
                                .build()
                );
    }

    /**
     * List contracts by category
     *
     * GET /api/v11/registry/contracts/category/{category}
     */
    @GET
    @Path("/contracts/category/{category}")
    public Uni<Response> listByCategory(@PathParam("category") String category) {
        LOGGER.info("REST: List contracts by category - {}", category);

        return contractRegistry.listByCategory(category)
                .map(contracts -> Response.ok(contracts).build());
    }

    /**
     * List recent contracts
     *
     * GET /api/v11/registry/contracts/recent?limit=10
     */
    @GET
    @Path("/contracts/recent")
    public Uni<Response> listRecentContracts(@QueryParam("limit") @DefaultValue("10") int limit) {
        LOGGER.info("REST: List recent contracts - limit: {}", limit);

        return contractRegistry.listRecentContracts(limit)
                .map(contracts -> Response.ok(contracts).build());
    }

    /**
     * List featured contracts
     *
     * GET /api/v11/registry/contracts/featured?limit=10
     */
    @GET
    @Path("/contracts/featured")
    public Uni<Response> listFeaturedContracts(@QueryParam("limit") @DefaultValue("10") int limit) {
        LOGGER.info("REST: List featured contracts - limit: {}", limit);

        return contractRegistry.listFeaturedContracts(limit)
                .map(contracts -> Response.ok(contracts).build());
    }

    /**
     * Get contract registry statistics
     *
     * GET /api/v11/registry/contracts/stats
     */
    @GET
    @Path("/contracts/stats")
    public Response getContractStats() {
        LOGGER.info("REST: Get contract registry stats");

        Map<String, Object> stats = contractRegistry.getRegistryStatistics();
        return Response.ok(stats).build();
    }

    // ========== RWAT Registry Endpoints ==========

    /**
     * Register new RWAT
     *
     * POST /api/v11/registry/rwat/register
     */
    @POST
    @Path("/rwat/register")
    public Uni<Response> registerRWAT(RWATRegistry rwat) {
        LOGGER.info("REST: Register RWAT - {}", rwat.getAssetName());

        return rwatRegistry.registerRWAT(rwat)
                .map(registered -> Response.ok(registered).build())
                .onFailure().recoverWithItem(error -> {
                    LOGGER.error("RWAT registration failed: {}", error.getMessage());
                    return Response.status(Response.Status.BAD_REQUEST)
                            .entity(Map.of("error", error.getMessage()))
                            .build();
                });
    }

    /**
     * Get RWAT by ID
     *
     * GET /api/v11/registry/rwat/{rwatId}
     */
    @GET
    @Path("/rwat/{rwatId}")
    public Uni<Response> getRWAT(@PathParam("rwatId") String rwatId) {
        LOGGER.info("REST: Get RWAT - {}", rwatId);

        return rwatRegistry.getRWAT(rwatId)
                .map(rwat -> Response.ok(rwat).build())
                .onFailure().recoverWithItem(error ->
                        Response.status(Response.Status.NOT_FOUND)
                                .entity(Map.of("error", error.getMessage()))
                                .build()
                );
    }

    /**
     * Search RWATs
     *
     * GET /api/v11/registry/rwat/search?keyword=xxx
     */
    @GET
    @Path("/rwat/search")
    public Uni<Response> searchRWATs(@QueryParam("keyword") String keyword) {
        LOGGER.info("REST: Search RWATs - keyword: {}", keyword);

        return rwatRegistry.searchRWATs(keyword)
                .map(rwats -> Response.ok(rwats).build());
    }

    /**
     * List RWATs by asset type
     *
     * GET /api/v11/registry/rwat/type/{assetType}
     */
    @GET
    @Path("/rwat/type/{assetType}")
    public Uni<Response> listByAssetType(@PathParam("assetType") String assetType) {
        LOGGER.info("REST: List RWATs by type - {}", assetType);

        try {
            RWATRegistry.AssetType type = RWATRegistry.AssetType.valueOf(assetType.toUpperCase());
            return rwatRegistry.listByAssetType(type)
                    .map(rwats -> Response.ok(rwats).build());
        } catch (IllegalArgumentException e) {
            return Uni.createFrom().item(Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "Invalid asset type"))
                    .build());
        }
    }

    /**
     * List verified RWATs
     *
     * GET /api/v11/registry/rwat/verified
     */
    @GET
    @Path("/rwat/verified")
    public Uni<Response> listVerifiedRWATs() {
        LOGGER.info("REST: List verified RWATs");

        return rwatRegistry.listVerifiedRWATs()
                .map(rwats -> Response.ok(rwats).build());
    }

    /**
     * List recent RWATs
     *
     * GET /api/v11/registry/rwat/recent?limit=10
     */
    @GET
    @Path("/rwat/recent")
    public Uni<Response> listRecentRWATs(@QueryParam("limit") @DefaultValue("10") int limit) {
        LOGGER.info("REST: List recent RWATs - limit: {}", limit);

        return rwatRegistry.listRecentRWATs(limit)
                .map(rwats -> Response.ok(rwats).build());
    }

    /**
     * List top RWATs by trading volume
     *
     * GET /api/v11/registry/rwat/top-volume?limit=10
     */
    @GET
    @Path("/rwat/top-volume")
    public Uni<Response> listTopByVolume(@QueryParam("limit") @DefaultValue("10") int limit) {
        LOGGER.info("REST: List top RWATs by volume - limit: {}", limit);

        return rwatRegistry.listTopByVolume(limit)
                .map(rwats -> Response.ok(rwats).build());
    }

    /**
     * Update RWAT verification status (admin)
     *
     * PUT /api/v11/registry/rwat/{rwatId}/verify
     */
    @PUT
    @Path("/rwat/{rwatId}/verify")
    public Uni<Response> updateVerification(
            @PathParam("rwatId") String rwatId,
            Map<String, String> request
    ) {
        LOGGER.info("REST: Update RWAT verification - {}", rwatId);

        String status = request.get("status");
        String verifierId = request.get("verifierId");

        if (status == null) {
            return Uni.createFrom().item(Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "status is required"))
                    .build());
        }

        try {
            RWATRegistry.VerificationStatus verificationStatus =
                    RWATRegistry.VerificationStatus.valueOf(status.toUpperCase());

            return rwatRegistry.updateVerificationStatus(rwatId, verificationStatus, verifierId)
                    .map(rwat -> Response.ok(rwat).build())
                    .onFailure().recoverWithItem(error ->
                            Response.status(Response.Status.BAD_REQUEST)
                                    .entity(Map.of("error", error.getMessage()))
                                    .build()
                    );
        } catch (IllegalArgumentException e) {
            return Uni.createFrom().item(Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "Invalid verification status"))
                    .build());
        }
    }

    /**
     * Get RWAT registry statistics
     *
     * GET /api/v11/registry/rwat/stats
     */
    @GET
    @Path("/rwat/stats")
    public Response getRWATStats() {
        LOGGER.info("REST: Get RWAT registry stats");

        Map<String, Object> stats = rwatRegistry.getStatistics();
        return Response.ok(stats).build();
    }

    // ========== Merkle Tree Endpoints ==========

    /**
     * Get Merkle root hash for RWAT registry
     *
     * GET /api/v11/registry/rwat/merkle/root
     */
    @GET
    @Path("/rwat/merkle/root")
    public Uni<Response> getMerkleRootHash() {
        LOGGER.info("REST: Get Merkle root hash");

        return rwatRegistry.getMerkleRootHash()
                .map(rootHashResponse -> Response.ok(rootHashResponse).build());
    }

    /**
     * Generate Merkle proof for an RWAT
     *
     * GET /api/v11/registry/rwat/{rwatId}/merkle/proof
     */
    @GET
    @Path("/rwat/{rwatId}/merkle/proof")
    public Uni<Response> generateMerkleProof(@PathParam("rwatId") String rwatId) {
        LOGGER.info("REST: Generate Merkle proof for RWAT - {}", rwatId);

        return rwatRegistry.getProof(rwatId)
                .map(proofData -> Response.ok(proofData).build())
                .onFailure().recoverWithItem(error ->
                        Response.status(Response.Status.NOT_FOUND)
                                .entity(Map.of("error", error.getMessage()))
                                .build()
                );
    }

    /**
     * Verify a Merkle proof
     *
     * POST /api/v11/registry/rwat/merkle/verify
     */
    @POST
    @Path("/rwat/merkle/verify")
    public Uni<Response> verifyMerkleProof(io.aurigraph.v11.merkle.MerkleProof.ProofData proofData) {
        LOGGER.info("REST: Verify Merkle proof");

        return rwatRegistry.verifyMerkleProof(proofData)
                .map(verificationResponse -> Response.ok(verificationResponse).build())
                .onFailure().recoverWithItem(error ->
                        Response.status(Response.Status.BAD_REQUEST)
                                .entity(Map.of("error", error.getMessage()))
                                .build()
                );
    }

    /**
     * Get Merkle tree statistics
     *
     * GET /api/v11/registry/rwat/merkle/stats
     */
    @GET
    @Path("/rwat/merkle/stats")
    public Uni<Response> getMerkleTreeStats() {
        LOGGER.info("REST: Get Merkle tree statistics");

        return rwatRegistry.getTreeStats()
                .map(stats -> Response.ok(Map.of(
                        "rootHash", stats.getRootHash(),
                        "entryCount", stats.getEntryCount(),
                        "treeHeight", stats.getTreeHeight(),
                        "lastUpdate", stats.getLastUpdate(),
                        "rebuildCount", stats.getRebuildCount()
                )).build());
    }
}
