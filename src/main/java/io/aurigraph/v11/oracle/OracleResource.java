package io.aurigraph.v11.oracle;

import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;

import java.util.List;

/**
 * Oracle Verification REST API Resource
 * Provides REST endpoints for oracle-based asset value verification
 *
 * @author API Development Agent (ADA) - Sprint 16
 * @version 11.0.0
 * @sprint Sprint 16 - Oracle Verification REST API (AV11-496)
 */
@Path("/api/v11/oracle")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Oracle Verification", description = "Multi-oracle consensus verification for asset values")
public class OracleResource {

    private static final Logger LOG = Logger.getLogger(OracleResource.class);

    @Inject
    OracleVerificationService verificationService;

    /**
     * Verify asset value using multi-oracle consensus
     *
     * POST /api/v11/oracle/verify
     */
    @POST
    @Path("/verify")
    @Operation(
        summary = "Verify asset value",
        description = "Verifies an asset value by querying multiple oracles and reaching consensus"
    )
    @APIResponses({
        @APIResponse(
            responseCode = "200",
            description = "Verification completed successfully",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON,
                schema = @Schema(implementation = OracleVerificationResult.class)
            )
        ),
        @APIResponse(
            responseCode = "400",
            description = "Invalid request parameters"
        ),
        @APIResponse(
            responseCode = "500",
            description = "Internal server error during verification"
        )
    })
    public Uni<Response> verifyAssetValue(
        @Schema(description = "Asset verification request", required = true)
        VerifyAssetRequest request
    ) {
        LOG.infof("Received verification request: assetId=%s, claimedValue=%s",
            request.getAssetId(), request.getClaimedValue());

        // Validate request
        if (!request.isValid()) {
            LOG.warnf("Invalid verification request: %s", request);
            return Uni.createFrom().item(
                Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("Invalid request: assetId and claimedValue are required"))
                    .build()
            );
        }

        return verificationService.verifyAssetValue(request.getAssetId(), request.getClaimedValue())
            .onItem().transform(result -> {
                if (result == null) {
                    return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                        .entity(new ErrorResponse("Verification failed: null result"))
                        .build();
                }
                return Response.ok(result).build();
            })
            .onFailure().recoverWithItem(throwable -> {
                LOG.errorf(throwable, "Verification failed for request: %s", request);
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Verification error: " + throwable.getMessage()))
                    .build();
            });
    }

    /**
     * Get verification result by ID
     *
     * GET /api/v11/oracle/verify/{verificationId}
     */
    @GET
    @Path("/verify/{verificationId}")
    @Operation(
        summary = "Get verification result",
        description = "Retrieves a verification result by its unique identifier"
    )
    @APIResponses({
        @APIResponse(
            responseCode = "200",
            description = "Verification result found",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON,
                schema = @Schema(implementation = OracleVerificationResult.class)
            )
        ),
        @APIResponse(
            responseCode = "404",
            description = "Verification not found"
        )
    })
    public Uni<Response> getVerificationResult(
        @Parameter(description = "Verification ID", required = true, example = "550e8400-e29b-41d4-a716-446655440000")
        @PathParam("verificationId") String verificationId
    ) {
        LOG.infof("Retrieving verification result: verificationId=%s", verificationId);

        return verificationService.getVerificationById(verificationId)
            .onItem().transform(result -> {
                if (result == null) {
                    LOG.warnf("Verification not found: verificationId=%s", verificationId);
                    return Response.status(Response.Status.NOT_FOUND)
                        .entity(new ErrorResponse("Verification not found: " + verificationId))
                        .build();
                }
                return Response.ok(result).build();
            })
            .onFailure().recoverWithItem(throwable -> {
                LOG.errorf(throwable, "Error retrieving verification: verificationId=%s", verificationId);
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Error retrieving verification: " + throwable.getMessage()))
                    .build();
            });
    }

    /**
     * Get verification history for an asset
     *
     * GET /api/v11/oracle/history/{assetId}
     */
    @GET
    @Path("/history/{assetId}")
    @Operation(
        summary = "Get verification history",
        description = "Retrieves verification history for a specific asset"
    )
    @APIResponses({
        @APIResponse(
            responseCode = "200",
            description = "Verification history retrieved",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON
            )
        ),
        @APIResponse(
            responseCode = "400",
            description = "Invalid limit parameter"
        )
    })
    public Uni<Response> getVerificationHistory(
        @Parameter(description = "Asset ID", required = true, example = "ASSET-001")
        @PathParam("assetId") String assetId,
        @Parameter(description = "Maximum number of results", example = "10")
        @QueryParam("limit") @DefaultValue("10") int limit
    ) {
        LOG.infof("Retrieving verification history: assetId=%s, limit=%d", assetId, limit);

        // Validate limit
        if (limit < 1 || limit > 100) {
            LOG.warnf("Invalid limit parameter: %d", limit);
            return Uni.createFrom().item(
                Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("Invalid limit: must be between 1 and 100"))
                    .build()
            );
        }

        return verificationService.getVerificationHistory(assetId, limit)
            .onItem().transform(history -> Response.ok(history).build())
            .onFailure().recoverWithItem(throwable -> {
                LOG.errorf(throwable, "Error retrieving history: assetId=%s", assetId);
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Error retrieving history: " + throwable.getMessage()))
                    .build();
            });
    }

    /**
     * Health check endpoint for oracle service
     *
     * GET /api/v11/oracle/health
     */
    @GET
    @Path("/health")
    @Operation(
        summary = "Oracle service health",
        description = "Check if the oracle verification service is operational"
    )
    @APIResponses({
        @APIResponse(
            responseCode = "200",
            description = "Service is healthy"
        )
    })
    public Uni<Response> healthCheck() {
        return Uni.createFrom().item(
            Response.ok(new HealthResponse("HEALTHY", "Oracle verification service is operational"))
                .build()
        );
    }

    // ==================== Helper Classes ====================

    /**
     * Error response DTO
     */
    @Schema(description = "Error response")
    public static class ErrorResponse {
        @Schema(description = "Error message")
        private String error;

        public ErrorResponse() {
        }

        public ErrorResponse(String error) {
            this.error = error;
        }

        public String getError() {
            return error;
        }

        public void setError(String error) {
            this.error = error;
        }
    }

    /**
     * Health response DTO
     */
    @Schema(description = "Health check response")
    public static class HealthResponse {
        @Schema(description = "Service status")
        private String status;
        
        @Schema(description = "Status message")
        private String message;

        public HealthResponse() {
        }

        public HealthResponse(String status, String message) {
            this.status = status;
            this.message = message;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }
}
