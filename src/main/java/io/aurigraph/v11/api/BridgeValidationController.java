package io.aurigraph.v11.api;

import io.aurigraph.v11.bridge.BridgeValidatorService;
import io.aurigraph.v11.bridge.models.ValidationRequest;
import io.aurigraph.v11.bridge.models.ValidationResponse;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;

/**
 * Bridge Validation Controller
 * REST API endpoint for validating cross-chain bridge transactions
 *
 * Endpoint: POST /api/v11/bridge/validate/initiate
 *
 * Features:
 * - Cross-chain signature verification
 * - Liquidity pool availability checks
 * - Transaction fee calculation
 * - Rate limiting (100 req/s per address)
 * - Token validation and decimals handling
 * - Slippage estimation
 *
 * @author Backend Development Agent (BDA)
 * @version 1.0
 * @since AV11-634
 */
@Path("/api/v11/bridge/validate")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Bridge Validation", description = "Bridge transaction validation and pre-flight checks")
public class BridgeValidationController {

    private static final Logger LOG = Logger.getLogger(BridgeValidationController.class);

    @Inject
    BridgeValidatorService validatorService;

    /**
     * Validate a bridge transaction before submission
     *
     * This endpoint performs comprehensive validation of a cross-chain bridge transaction:
     * 1. Signature verification (SECP256K1, ED25519, ECDSA)
     * 2. Liquidity pool availability checks
     * 3. Token support and decimal validation
     * 4. Chain compatibility verification
     * 5. Rate limiting checks (100 req/s per address)
     * 6. Fee calculation and slippage estimation
     * 7. Amount limit validation
     *
     * @param request Validation request containing bridge transaction details
     * @return Validation response with detailed results and recommendations
     *
     * HTTP Status Codes:
     * - 200 OK: Validation completed (check status field for SUCCESS/WARNINGS/FAILED)
     * - 400 Bad Request: Invalid request format
     * - 429 Too Many Requests: Rate limit exceeded
     * - 500 Internal Server Error: Server error during validation
     */
    @POST
    @Path("/initiate")
    @Operation(
            summary = "Validate bridge transaction",
            description = "Performs comprehensive validation of a cross-chain bridge transaction including signature verification, liquidity checks, and fee estimation"
    )
    @RequestBody(
            description = "Bridge transaction validation request",
            required = true,
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = ValidationRequest.class)
            )
    )
    @APIResponses({
            @APIResponse(
                    responseCode = "200",
                    description = "Validation completed successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = ValidationResponse.class)
                    )
            ),
            @APIResponse(
                    responseCode = "400",
                    description = "Invalid request format or missing required fields"
            ),
            @APIResponse(
                    responseCode = "429",
                    description = "Rate limit exceeded (100 requests per second per address)"
            ),
            @APIResponse(
                    responseCode = "500",
                    description = "Server error during validation"
            )
    })
    public Uni<Response> validateBridgeTransaction(
            @RequestBody ValidationRequest request) {

        LOG.infof("Received validation request for bridge: %s", request.getBridgeId());

        return Uni.createFrom().item(() -> {
            try {
                // Validate request
                if (request == null) {
                    LOG.warn("Validation request is null");
                    return Response.status(Response.Status.BAD_REQUEST)
                            .entity(createErrorResponse("Request body is required"))
                            .build();
                }

                // Basic field validation
                if (request.getBridgeId() == null || request.getBridgeId().isEmpty()) {
                    return Response.status(Response.Status.BAD_REQUEST)
                            .entity(createErrorResponse("Bridge ID is required"))
                            .build();
                }

                if (request.getSourceAddress() == null || request.getSourceAddress().isEmpty()) {
                    return Response.status(Response.Status.BAD_REQUEST)
                            .entity(createErrorResponse("Source address is required for rate limiting"))
                            .build();
                }

                // Perform validation
                ValidationResponse validationResult = validatorService.validateBridgeTransaction(request);

                LOG.infof("Validation completed with status: %s", validationResult.getStatus());

                // Return appropriate HTTP status based on validation result
                Response.Status httpStatus = switch (validationResult.getStatus()) {
                    case SUCCESS -> Response.Status.OK;
                    case WARNINGS -> Response.Status.OK; // Still valid, but with warnings
                    case FAILED -> Response.Status.BAD_REQUEST;
                };

                // Check for rate limiting
                if (validationResult.getRateLimitInfo() != null &&
                    validationResult.getRateLimitInfo().getIsRateLimited() != null &&
                    validationResult.getRateLimitInfo().getIsRateLimited()) {
                    LOG.warnf("Rate limit exceeded for address: %s", request.getSourceAddress());
                    return Response.status(Response.Status.TOO_MANY_REQUESTS)
                            .entity(validationResult)
                            .build();
                }

                return Response.status(httpStatus)
                        .entity(validationResult)
                        .build();

            } catch (Exception e) {
                LOG.error("Error during bridge validation", e);
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                        .entity(createErrorResponse("Internal server error: " + e.getMessage()))
                        .build();
            }
        }).onFailure().recoverWithItem(() -> {
            LOG.error("Unexpected error in validation endpoint");
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(createErrorResponse("Unexpected server error"))
                    .build();
        });
    }

    /**
     * Quick validation endpoint (lightweight)
     * Returns just SUCCESS/FAILED without detailed analysis
     *
     * Useful for quick checks before committing to validation
     */
    @POST
    @Path("/quick")
    @Operation(
            summary = "Quick validation check",
            description = "Fast validation check returning only SUCCESS or FAILED status"
    )
    @APIResponse(
            responseCode = "200",
            description = "Quick validation result"
    )
    public Uni<Response> quickValidate(ValidationRequest request) {
        LOG.debugf("Quick validation requested for bridge: %s", request.getBridgeId());

        return Uni.createFrom().item(() -> {
            if (request == null || request.getBridgeId() == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(createErrorResponse("Invalid request"))
                        .build();
            }

            ValidationResponse result = validatorService.validateBridgeTransaction(request);
            boolean isValid = result.isSuccessful();

            return Response.ok()
                    .entity(createQuickValidationResponse(isValid, result.getValidationId()))
                    .build();
        });
    }

    /**
     * Get validation result details by validation ID
     *
     * Retrieve detailed validation results if needed later
     */
    @GET
    @Path("/{validationId}")
    @Operation(
            summary = "Get validation details",
            description = "Retrieve detailed validation results by ID"
    )
    @APIResponse(
            responseCode = "200",
            description = "Validation details found"
    )
    @APIResponse(
            responseCode = "404",
            description = "Validation ID not found"
    )
    public Uni<Response> getValidationDetails(
            @PathParam("validationId") String validationId) {

        LOG.debugf("Retrieving validation details for ID: %s", validationId);

        // In a real implementation, this would retrieve from database
        // For now, return not implemented
        return Uni.createFrom().item(() ->
                Response.status(Response.Status.NOT_FOUND)
                        .entity(createErrorResponse("Validation storage not yet implemented"))
                        .build()
        );
    }

    /**
     * Create error response object
     */
    private ErrorResponse createErrorResponse(String message) {
        return new ErrorResponse(
                "ERROR",
                message,
                System.currentTimeMillis()
        );
    }

    /**
     * Create quick validation response
     */
    private QuickValidationResponse createQuickValidationResponse(boolean valid, String validationId) {
        return new QuickValidationResponse(
                valid ? "SUCCESS" : "FAILED",
                validationId,
                System.currentTimeMillis()
        );
    }

    /**
     * Error response DTO
     */
    public static class ErrorResponse {
        public String status;
        public String message;
        public long timestamp;

        public ErrorResponse(String status, String message, long timestamp) {
            this.status = status;
            this.message = message;
            this.timestamp = timestamp;
        }
    }

    /**
     * Quick validation response DTO
     */
    public static class QuickValidationResponse {
        public String status;
        public String validationId;
        public long timestamp;

        public QuickValidationResponse(String status, String validationId, long timestamp) {
            this.status = status;
            this.validationId = validationId;
            this.timestamp = timestamp;
        }
    }
}
