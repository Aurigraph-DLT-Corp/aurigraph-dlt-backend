package io.aurigraph.v11.tokenization.traceability;

import io.quarkus.logging.Log;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.Map;

/**
 * TokenTraceabilityResource - REST API endpoints for token traceability
 * Links tokens to underlying merkle tree registry assets.
 *
 * Endpoints:
 * - POST /api/v11/traceability/tokens/{tokenId}/trace - Create token trace
 * - POST /api/v11/traceability/tokens/{tokenId}/link-asset - Link token to asset
 * - POST /api/v11/traceability/tokens/{tokenId}/verify-proof - Verify asset proof
 * - POST /api/v11/traceability/tokens/{tokenId}/transfer - Record ownership transfer
 * - GET /api/v11/traceability/tokens/{tokenId} - Get token trace
 * - GET /api/v11/traceability/tokens - List all token traces
 * - GET /api/v11/traceability/tokens/type/{assetType} - Query by asset type
 * - GET /api/v11/traceability/tokens/owner/{ownerAddress} - Query by owner
 * - GET /api/v11/traceability/tokens/status/{verificationStatus} - Query by status
 * - GET /api/v11/traceability/tokens/{tokenId}/compliance - Get compliance summary
 * - POST /api/v11/traceability/tokens/{tokenId}/certify - Add certification
 * - GET /api/v11/traceability/statistics - Get trace statistics
 *
 * @author Aurigraph V12 Token Traceability Team
 * @version 1.0.0
 */
@Path("/api/v11/traceability")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class TokenTraceabilityResource {

    @Inject
    MerkleTokenTraceabilityService traceabilityService;

    /**
     * Create a new token trace
     * POST /api/v11/traceability/tokens/{tokenId}/trace
     *
     * @param tokenId - Token identifier
     * @param request - Create request with assetId, assetType, owner
     * @return Created token trace
     */
    @POST
    @Path("/tokens/{tokenId}/trace")
    public Uni<Response> createTokenTrace(
            @PathParam("tokenId") String tokenId,
            CreateTraceRequest request) {

        Log.info("Creating token trace for token: " + tokenId);

        return traceabilityService.createTokenTrace(
                tokenId,
                request.assetId,
                request.assetType,
                request.ownerAddress
        ).map(trace -> Response.status(Response.Status.CREATED).entity(trace).build())
         .onFailure().recoverWithItem(ex -> {
             Log.error("Error creating token trace", ex);
             return Response.status(Response.Status.BAD_REQUEST)
                 .entity(Map.of("error", ex.getMessage())).build();
         });
    }

    /**
     * Link token to an underlying asset
     * POST /api/v11/traceability/tokens/{tokenId}/link-asset
     *
     * @param tokenId - Token to link
     * @param request - Link request with rwatId
     * @return Updated trace
     */
    @POST
    @Path("/tokens/{tokenId}/link-asset")
    public Uni<Response> linkTokenToAsset(
            @PathParam("tokenId") String tokenId,
            LinkAssetRequest request) {

        Log.info("Linking token " + tokenId + " to asset " + request.rwatId);

        return traceabilityService.linkTokenToAsset(tokenId, request.rwatId)
            .map(trace -> Response.ok(trace).build())
            .onFailure().recoverWithItem(ex -> {
                Log.error("Error linking token to asset", ex);
                return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("error", ex.getMessage())).build();
            });
    }

    /**
     * Verify a token's underlying asset through merkle proof
     * POST /api/v11/traceability/tokens/{tokenId}/verify-proof
     *
     * @param tokenId - Token to verify
     * @return Verification result
     */
    @POST
    @Path("/tokens/{tokenId}/verify-proof")
    public Uni<Response> verifyTokenAssetProof(
            @PathParam("tokenId") String tokenId) {

        Log.info("Verifying token asset proof: " + tokenId);

        return traceabilityService.verifyTokenAssetProof(tokenId)
            .map(trace -> Response.ok(Map.of(
                "token_id", tokenId,
                "proof_valid", trace.getProofValid(),
                "verification_status", trace.getVerificationStatus(),
                "asset_verified", trace.getAssetVerified(),
                "merkle_proof_path", trace.getMerkleProofPath()
            )).build())
            .onFailure().recoverWithItem(ex -> {
                Log.error("Error verifying token asset proof", ex);
                return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("error", ex.getMessage())).build();
            });
    }

    /**
     * Record an ownership transfer in token trace
     * POST /api/v11/traceability/tokens/{tokenId}/transfer
     *
     * @param tokenId - Token being transferred
     * @param request - Transfer request with addresses and percentage
     * @return Updated trace with transfer record
     */
    @POST
    @Path("/tokens/{tokenId}/transfer")
    public Uni<Response> recordOwnershipTransfer(
            @PathParam("tokenId") String tokenId,
            TransferRequest request) {

        Log.info("Recording ownership transfer for token: " + tokenId);

        return traceabilityService.recordOwnershipTransfer(
                tokenId,
                request.fromAddress,
                request.toAddress,
                request.ownershipPercentage
        ).map(trace -> Response.ok(Map.of(
                "token_id", tokenId,
                "new_owner", trace.getOwnerAddress(),
                "ownership_percentage", trace.getFractionalOwnership(),
                "transfers_recorded", trace.getOwnershipHistory().size()
            )).build())
         .onFailure().recoverWithItem(ex -> {
             Log.error("Error recording ownership transfer", ex);
             return Response.status(Response.Status.BAD_REQUEST)
                 .entity(Map.of("error", ex.getMessage())).build();
         });
    }

    /**
     * Get complete token trace
     * GET /api/v11/traceability/tokens/{tokenId}
     *
     * @param tokenId - Token to retrieve
     * @return Complete token trace
     */
    @GET
    @Path("/tokens/{tokenId}")
    public Uni<Response> getTokenTrace(
            @PathParam("tokenId") String tokenId) {

        Log.info("Retrieving token trace: " + tokenId);

        return traceabilityService.getTokenTrace(tokenId)
            .map(trace -> Response.ok(trace).build())
            .onFailure().recoverWithItem(ex -> {
                Log.error("Error retrieving token trace", ex);
                return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("error", "Token trace not found: " + tokenId)).build();
            });
    }

    /**
     * Get all token traces
     * GET /api/v11/traceability/tokens
     *
     * @return List of all token traces
     */
    @GET
    @Path("/tokens")
    public Uni<Response> getAllTokenTraces() {
        Log.info("Retrieving all token traces");

        return traceabilityService.getAllTraces()
            .map(traces -> Response.ok(Map.of(
                "total", traces.size(),
                "traces", traces
            )).build());
    }

    /**
     * Get token traces by asset type
     * GET /api/v11/traceability/tokens/type/{assetType}
     *
     * @param assetType - Asset type to query
     * @return List of traces for asset type
     */
    @GET
    @Path("/tokens/type/{assetType}")
    public Uni<Response> getTracesByAssetType(
            @PathParam("assetType") String assetType) {

        Log.info("Querying token traces by asset type: " + assetType);

        return traceabilityService.getTracesByAssetType(assetType)
            .map(traces -> Response.ok(Map.of(
                "asset_type", assetType,
                "total", traces.size(),
                "traces", traces
            )).build());
    }

    /**
     * Get token traces by owner address
     * GET /api/v11/traceability/tokens/owner/{ownerAddress}
     *
     * @param ownerAddress - Owner address to query
     * @return List of traces owned by address
     */
    @GET
    @Path("/tokens/owner/{ownerAddress}")
    public Uni<Response> getTracesByOwner(
            @PathParam("ownerAddress") String ownerAddress) {

        Log.info("Querying token traces by owner: " + ownerAddress);

        return traceabilityService.getTracesByOwner(ownerAddress)
            .map(traces -> Response.ok(Map.of(
                "owner_address", ownerAddress,
                "total", traces.size(),
                "traces", traces
            )).build());
    }

    /**
     * Get token traces by verification status
     * GET /api/v11/traceability/tokens/status/{verificationStatus}
     *
     * @param status - Verification status to query (PENDING, IN_REVIEW, VERIFIED, REJECTED)
     * @return List of traces with status
     */
    @GET
    @Path("/tokens/status/{verificationStatus}")
    public Uni<Response> getTracesByStatus(
            @PathParam("verificationStatus") String status) {

        Log.info("Querying token traces by verification status: " + status);

        return traceabilityService.getTracesByVerificationStatus(status)
            .map(traces -> Response.ok(Map.of(
                "verification_status", status,
                "total", traces.size(),
                "traces", traces
            )).build());
    }

    /**
     * Get compliance summary for a token
     * GET /api/v11/traceability/tokens/{tokenId}/compliance
     *
     * @param tokenId - Token to check
     * @return Compliance summary
     */
    @GET
    @Path("/tokens/{tokenId}/compliance")
    public Uni<Response> getComplianceSummary(
            @PathParam("tokenId") String tokenId) {

        Log.info("Retrieving compliance summary for token: " + tokenId);

        return traceabilityService.getComplianceSummary(tokenId)
            .map(summary -> Response.ok(summary).build())
            .onFailure().recoverWithItem(ex -> {
                Log.error("Error retrieving compliance summary", ex);
                return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("error", ex.getMessage())).build();
            });
    }

    /**
     * Add compliance certification to token
     * POST /api/v11/traceability/tokens/{tokenId}/certify
     *
     * @param tokenId - Token to certify
     * @param request - Certification request
     * @return Updated trace
     */
    @POST
    @Path("/tokens/{tokenId}/certify")
    public Uni<Response> addComplianceCertification(
            @PathParam("tokenId") String tokenId,
            CertificationRequest request) {

        Log.info("Adding compliance certification to token: " + tokenId);

        return traceabilityService.addComplianceCertification(tokenId, request.certification)
            .map(trace -> Response.ok(Map.of(
                "token_id", tokenId,
                "certification_added", request.certification,
                "total_certifications", trace.getComplianceCertifications().size()
            )).build())
            .onFailure().recoverWithItem(ex -> {
                Log.error("Error adding compliance certification", ex);
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", ex.getMessage())).build();
            });
    }

    /**
     * Get token traceability statistics
     * GET /api/v11/traceability/statistics
     *
     * @return Statistics summary
     */
    @GET
    @Path("/statistics")
    public Uni<Response> getStatistics() {
        Log.info("Retrieving token traceability statistics");

        return traceabilityService.getTraceStatistics()
            .map(stats -> Response.ok(stats).build());
    }

    // ==================== Request Classes ====================

    public static class CreateTraceRequest {
        public String assetId;
        public String assetType;
        public String ownerAddress;
    }

    public static class LinkAssetRequest {
        public String rwatId;
    }

    public static class TransferRequest {
        public String fromAddress;
        public String toAddress;
        public Double ownershipPercentage;
    }

    public static class CertificationRequest {
        public String certification;
    }
}
