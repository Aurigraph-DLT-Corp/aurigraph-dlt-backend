package io.aurigraph.v11.api;

import io.aurigraph.v11.bridge.models.TransferRequest;
import io.aurigraph.v11.bridge.models.TransferResponse;
import io.aurigraph.v11.bridge.services.BridgeTransferService;
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
 * Bridge Transfer Controller
 * REST API endpoints for submitting and managing multi-signature bridge transfers
 *
 * Endpoint: POST /api/v11/bridge/transfer/submit
 *
 * Features:
 * - Multi-signature support (M-of-N schemes)
 * - State machine management (PENDING → SIGNED → APPROVED → EXECUTING → COMPLETED)
 * - Liquidity pool management
 * - Atomic state transitions
 * - Event publishing
 *
 * @author Backend Development Agent (BDA)
 * @version 1.0
 * @since AV11-635
 */
@Path("/api/v11/bridge/transfer")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Bridge Transfer", description = "Multi-signature bridge transfer submission and management")
public class BridgeTransferController {

    private static final Logger LOG = Logger.getLogger(BridgeTransferController.class);

    @Inject
    BridgeTransferService bridgeTransferService;

    /**
     * Submit a new multi-signature bridge transfer
     *
     * Supports M-of-N multi-signature schemes:
     * - 2-of-3: Requires 2 out of 3 signers
     * - 3-of-5: Requires 3 out of 5 signers
     * - Etc.
     *
     * State transitions:
     * 1. PENDING: Initial state, awaiting signatures
     * 2. SIGNED: All signatures collected and verified
     * 3. APPROVED: Multi-sig threshold met, ready for execution
     * 4. EXECUTING: Transfer locked on source chain
     * 5. COMPLETED: Transfer released and confirmed on target chain
     *
     * @param request Transfer request with signatures
     * @return Transfer response with status and signature progress
     *
     * HTTP Status Codes:
     * - 200 OK: Transfer submitted successfully
     * - 400 Bad Request: Invalid request or signature validation failed
     * - 409 Conflict: Transfer already exists
     * - 500 Internal Server Error: Server error
     */
    @POST
    @Path("/submit")
    @Operation(
            summary = "Submit multi-signature bridge transfer",
            description = "Submit a bridge transfer with multi-signatures for cross-chain execution"
    )
    @RequestBody(
            description = "Bridge transfer request with M-of-N signatures",
            required = true,
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = TransferRequest.class)
            )
    )
    @APIResponses({
            @APIResponse(
                    responseCode = "200",
                    description = "Transfer submitted successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = TransferResponse.class)
                    )
            ),
            @APIResponse(
                    responseCode = "400",
                    description = "Invalid request format or signature validation failed"
            ),
            @APIResponse(
                    responseCode = "409",
                    description = "Transfer already exists"
            ),
            @APIResponse(
                    responseCode = "500",
                    description = "Server error during submission"
            )
    })
    public Uni<Response> submitBridgeTransfer(
            @RequestBody TransferRequest request) {

        LOG.infof("Received transfer submission request for transfer: %s", request.getTransferId());

        return Uni.createFrom().item(() -> {
            try {
                if (request == null) {
                    LOG.warn("Transfer request is null");
                    return Response.status(Response.Status.BAD_REQUEST)
                            .entity(createErrorResponse("Request body is required"))
                            .build();
                }

                if (request.getTransferId() == null || request.getTransferId().isEmpty()) {
                    return Response.status(Response.Status.BAD_REQUEST)
                            .entity(createErrorResponse("Transfer ID is required"))
                            .build();
                }

                // Submit transfer
                TransferResponse response = bridgeTransferService.submitBridgeTransfer(request);

                // Determine HTTP status based on transfer status
                Response.Status httpStatus = switch (response.getStatus()) {
                    case PENDING, SIGNED, APPROVED -> Response.Status.OK;
                    case EXECUTING, COMPLETED -> Response.Status.OK;
                    case FAILED -> Response.Status.BAD_REQUEST;
                    case CANCELLED -> Response.Status.OK;
                };

                LOG.infof("Transfer submission completed with status: %s", response.getStatus());

                return Response.status(httpStatus)
                        .entity(response)
                        .build();

            } catch (Exception e) {
                LOG.error("Error during transfer submission", e);
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                        .entity(createErrorResponse("Internal server error: " + e.getMessage()))
                        .build();
            }
        }).onFailure().recoverWithItem(() -> {
            LOG.error("Unexpected error in transfer submission endpoint");
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(createErrorResponse("Unexpected server error"))
                    .build();
        });
    }

    /**
     * Approve a signed transfer
     * Transitions from SIGNED to APPROVED state
     */
    @POST
    @Path("/{transferId}/approve")
    @Operation(
            summary = "Approve a bridge transfer",
            description = "Approve a transfer after all signatures are collected"
    )
    @APIResponse(responseCode = "200", description = "Transfer approved successfully")
    @APIResponse(responseCode = "404", description = "Transfer not found")
    @APIResponse(responseCode = "409", description = "Transfer not in SIGNED state")
    public Uni<Response> approveBridgeTransfer(
            @PathParam("transferId") String transferId) {

        LOG.infof("Approving transfer: %s", transferId);

        return Uni.createFrom().item(() -> {
            TransferResponse response = bridgeTransferService.approveBridgeTransfer(transferId);

            if (response == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(createErrorResponse("Transfer not found"))
                        .build();
            }

            return Response.ok(response).build();
        });
    }

    /**
     * Execute an approved transfer
     * Transitions from APPROVED to EXECUTING state
     */
    @POST
    @Path("/{transferId}/execute")
    @Operation(
            summary = "Execute an approved bridge transfer",
            description = "Execute a transfer that has been approved by multi-sig"
    )
    @APIResponse(responseCode = "200", description = "Transfer execution started")
    @APIResponse(responseCode = "404", description = "Transfer not found")
    @APIResponse(responseCode = "409", description = "Transfer not in APPROVED state")
    public Uni<Response> executeBridgeTransfer(
            @PathParam("transferId") String transferId) {

        LOG.infof("Executing transfer: %s", transferId);

        return Uni.createFrom().item(() -> {
            TransferResponse response = bridgeTransferService.executeBridgeTransfer(transferId);

            if (response == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(createErrorResponse("Transfer not found"))
                        .build();
            }

            return Response.ok(response).build();
        });
    }

    /**
     * Complete a transfer
     * Transitions from EXECUTING to COMPLETED state
     */
    @POST
    @Path("/{transferId}/complete")
    @Operation(
            summary = "Mark transfer as completed",
            description = "Mark a transfer as completed after receiving confirmations"
    )
    @APIResponse(responseCode = "200", description = "Transfer marked as completed")
    @APIResponse(responseCode = "404", description = "Transfer not found")
    public Uni<Response> completeBridgeTransfer(
            @PathParam("transferId") String transferId,
            @QueryParam("sourceHash") String sourceHash,
            @QueryParam("targetHash") String targetHash) {

        LOG.infof("Completing transfer: %s", transferId);

        return Uni.createFrom().item(() -> {
            TransferResponse response = bridgeTransferService.completeBridgeTransfer(
                    transferId, sourceHash, targetHash);

            if (response == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(createErrorResponse("Transfer not found"))
                        .build();
            }

            return Response.ok(response).build();
        });
    }

    /**
     * Cancel a transfer
     */
    @POST
    @Path("/{transferId}/cancel")
    @Operation(
            summary = "Cancel a bridge transfer",
            description = "Cancel a transfer (only from PENDING or SIGNED state)"
    )
    @APIResponse(responseCode = "200", description = "Transfer cancelled")
    @APIResponse(responseCode = "404", description = "Transfer not found")
    @APIResponse(responseCode = "409", description = "Transfer cannot be cancelled from current state")
    public Uni<Response> cancelBridgeTransfer(
            @PathParam("transferId") String transferId,
            @QueryParam("reason") String reason) {

        LOG.infof("Cancelling transfer: %s, reason: %s", transferId, reason);

        return Uni.createFrom().item(() -> {
            TransferResponse response = bridgeTransferService.cancelBridgeTransfer(
                    transferId, reason != null ? reason : "User cancelled");

            if (response == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(createErrorResponse("Transfer not found"))
                        .build();
            }

            return Response.ok(response).build();
        });
    }

    /**
     * Get transfer status
     */
    @GET
    @Path("/{transferId}/status")
    @Operation(
            summary = "Get transfer status",
            description = "Retrieve the current status and details of a transfer"
    )
    @APIResponse(responseCode = "200", description = "Transfer status retrieved")
    @APIResponse(responseCode = "404", description = "Transfer not found")
    public Uni<Response> getTransferStatus(
            @PathParam("transferId") String transferId) {

        LOG.debugf("Retrieving status for transfer: %s", transferId);

        return Uni.createFrom().item(() -> {
            TransferResponse response = bridgeTransferService.getTransferStatus(transferId);

            if (response == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(createErrorResponse("Transfer not found"))
                        .build();
            }

            return Response.ok(response).build();
        });
    }

    /**
     * Create error response object
     */
    private ErrorResponse createErrorResponse(String message) {
        return new ErrorResponse("ERROR", message, System.currentTimeMillis());
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
}
