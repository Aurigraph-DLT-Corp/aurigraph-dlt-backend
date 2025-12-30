package io.aurigraph.v11.api;

import io.aurigraph.v11.bridge.models.AtomicSwapRequest;
import io.aurigraph.v11.bridge.models.AtomicSwapResponse;
import io.aurigraph.v11.bridge.services.HashTimeLockContract;
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
 * Bridge Swap Controller (HTLC)
 * REST API endpoints for atomic swaps using Hash-Time-Locked Contracts
 *
 * Endpoint: /api/v11/bridge/swap/*
 *
 * Features:
 * - Hash-Time-Locked Contract (HTLC) management
 * - Support for SHA256, SHA3 hash algorithms
 * - Automatic secret generation
 * - Timelock management (default 5 minutes)
 * - State machine management (INITIATED → LOCKED → REVEALED → COMPLETED)
 * - Atomic swap execution across chains
 * - Fallback refund mechanism on expiry
 *
 * @author Backend Development Agent (BDA)
 * @version 1.0
 * @since AV11-636
 */
@Path("/api/v11/bridge/swap")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Bridge Atomic Swap", description = "Hash-Time-Locked Contract (HTLC) atomic swap operations")
public class BridgeSwapController {

    private static final Logger LOG = Logger.getLogger(BridgeSwapController.class);

    @Inject
    HashTimeLockContract hashTimeLockContract;

    /**
     * Initiate an atomic swap (HTLC)
     *
     * Creates a new Hash-Time-Locked Contract for an atomic swap between two chains.
     * Generates a random secret and returns its hash (hashLock) to initiator.
     *
     * HTLC Flow:
     * 1. Initiator creates HTLC with hashLock
     * 2. Counterparty locks funds with same hashLock on target chain
     * 3. Initiator reveals secret to claim counterparty's funds
     * 4. Counterparty uses revealed secret to claim initiator's funds
     * 5. If secret not revealed within timelock, funds auto-refund
     *
     * @param request Atomic swap request with all required parameters
     * @return Atomic swap response with hashLock and expiry time
     *
     * HTTP Status Codes:
     * - 200 OK: Swap initiated successfully
     * - 400 Bad Request: Invalid request or validation failed
     * - 409 Conflict: Swap already exists
     * - 500 Internal Server Error: Server error
     */
    @POST
    @Path("/initiate")
    @Operation(
            summary = "Initiate an atomic swap (HTLC)",
            description = "Create a Hash-Time-Locked Contract for cross-chain atomic swap"
    )
    @RequestBody(
            description = "Atomic swap request parameters",
            required = true,
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = AtomicSwapRequest.class)
            )
    )
    @APIResponses({
            @APIResponse(
                    responseCode = "200",
                    description = "Atomic swap initiated successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = AtomicSwapResponse.class)
                    )
            ),
            @APIResponse(
                    responseCode = "400",
                    description = "Invalid request format or validation failed"
            ),
            @APIResponse(
                    responseCode = "409",
                    description = "Swap already exists"
            ),
            @APIResponse(
                    responseCode = "500",
                    description = "Server error during initiation"
            )
    })
    public Uni<Response> initiateAtomicSwap(
            @RequestBody AtomicSwapRequest request) {

        LOG.infof("Received atomic swap initiation request for swap: %s", request.getSwapId());

        return Uni.createFrom().item(() -> {
            try {
                if (request == null) {
                    LOG.warn("Atomic swap request is null");
                    return Response.status(Response.Status.BAD_REQUEST)
                            .entity(createErrorResponse("Request body is required"))
                            .build();
                }

                if (request.getSwapId() == null || request.getSwapId().isEmpty()) {
                    return Response.status(Response.Status.BAD_REQUEST)
                            .entity(createErrorResponse("Swap ID is required"))
                            .build();
                }

                // Initiate swap
                AtomicSwapResponse response = hashTimeLockContract.initiateSwap(request);

                // Determine HTTP status based on swap status
                Response.Status httpStatus = switch (response.getStatus()) {
                    case INITIATED -> Response.Status.OK;
                    case FAILED -> Response.Status.BAD_REQUEST;
                    default -> Response.Status.OK;
                };

                LOG.infof("Atomic swap initiated successfully: %s, expires at: %s",
                         response.getSwapId(), response.getExpiryTime());

                return Response.status(httpStatus)
                        .entity(response)
                        .build();

            } catch (Exception e) {
                LOG.error("Error during atomic swap initiation", e);
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                        .entity(createErrorResponse("Internal server error: " + e.getMessage()))
                        .build();
            }
        }).onFailure().recoverWithItem(() -> {
            LOG.error("Unexpected error in atomic swap initiation endpoint");
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(createErrorResponse("Unexpected server error"))
                    .build();
        });
    }

    /**
     * Lock funds for an atomic swap
     * Transitions from INITIATED to LOCKED state
     *
     * Called by the counterparty to lock matching funds on the target chain
     * with the same hash lock. This initiates the actual fund lock.
     */
    @POST
    @Path("/{swapId}/lock")
    @Operation(
            summary = "Lock funds for atomic swap",
            description = "Lock funds on target chain with hash lock (INITIATED → LOCKED)"
    )
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Funds locked successfully"),
            @APIResponse(responseCode = "404", description = "Swap not found"),
            @APIResponse(responseCode = "409", description = "Swap not in INITIATED state"),
            @APIResponse(responseCode = "500", description = "Server error")
    })
    public Uni<Response> lockFundsForSwap(
            @PathParam("swapId") String swapId,
            @QueryParam("lockTxHash") String lockTxHash) {

        LOG.infof("Locking funds for atomic swap: %s", swapId);

        return Uni.createFrom().item(() -> {
            try {
                AtomicSwapResponse response = hashTimeLockContract.lockFunds(swapId, lockTxHash);

                if (response == null) {
                    return Response.status(Response.Status.NOT_FOUND)
                            .entity(createErrorResponse("Swap not found"))
                            .build();
                }

                if (response.getStatus() == AtomicSwapResponse.SwapStatus.FAILED) {
                    return Response.status(Response.Status.CONFLICT)
                            .entity(response)
                            .build();
                }

                LOG.infof("Funds locked for atomic swap: %s", swapId);
                return Response.ok(response).build();

            } catch (Exception e) {
                LOG.error("Error locking funds for atomic swap", e);
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                        .entity(createErrorResponse("Error locking funds: " + e.getMessage()))
                        .build();
            }
        });
    }

    /**
     * Reveal secret for atomic swap
     * Transitions from LOCKED to REVEALED state
     *
     * Called by initiator to reveal the secret. The secret must match the
     * hash lock. Once revealed, the swap can be completed.
     */
    @POST
    @Path("/{swapId}/reveal")
    @Operation(
            summary = "Reveal secret for atomic swap",
            description = "Reveal secret to enable swap completion (LOCKED → REVEALED)"
    )
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Secret revealed successfully"),
            @APIResponse(responseCode = "404", description = "Swap not found"),
            @APIResponse(responseCode = "409", description = "Swap not in LOCKED state or secret invalid"),
            @APIResponse(responseCode = "500", description = "Server error")
    })
    public Uni<Response> revealSecretForSwap(
            @PathParam("swapId") String swapId,
            @QueryParam("secret") String secret) {

        LOG.infof("Revealing secret for atomic swap: %s", swapId);

        return Uni.createFrom().item(() -> {
            try {
                if (secret == null || secret.isEmpty()) {
                    return Response.status(Response.Status.BAD_REQUEST)
                            .entity(createErrorResponse("Secret is required"))
                            .build();
                }

                AtomicSwapResponse response = hashTimeLockContract.revealSecret(swapId, secret);

                if (response == null) {
                    return Response.status(Response.Status.NOT_FOUND)
                            .entity(createErrorResponse("Swap not found"))
                            .build();
                }

                if (response.getStatus() == AtomicSwapResponse.SwapStatus.FAILED) {
                    return Response.status(Response.Status.CONFLICT)
                            .entity(response)
                            .build();
                }

                LOG.infof("Secret revealed for atomic swap: %s", swapId);
                return Response.ok(response).build();

            } catch (Exception e) {
                LOG.error("Error revealing secret for atomic swap", e);
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                        .entity(createErrorResponse("Error revealing secret: " + e.getMessage()))
                        .build();
            }
        });
    }

    /**
     * Complete an atomic swap
     * Transitions from REVEALED to COMPLETED state
     *
     * Called by counterparty to finalize the swap after secret is revealed.
     * This releases funds and confirms the transaction on the target chain.
     */
    @POST
    @Path("/{swapId}/complete")
    @Operation(
            summary = "Complete atomic swap",
            description = "Finalize swap execution (REVEALED → COMPLETED)"
    )
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Swap completed successfully"),
            @APIResponse(responseCode = "404", description = "Swap not found"),
            @APIResponse(responseCode = "409", description = "Swap not in REVEALED state"),
            @APIResponse(responseCode = "500", description = "Server error")
    })
    public Uni<Response> completeAtomicSwap(
            @PathParam("swapId") String swapId,
            @QueryParam("targetTxHash") String targetTxHash) {

        LOG.infof("Completing atomic swap: %s", swapId);

        return Uni.createFrom().item(() -> {
            try {
                AtomicSwapResponse response = hashTimeLockContract.completeSwap(swapId, targetTxHash);

                if (response == null) {
                    return Response.status(Response.Status.NOT_FOUND)
                            .entity(createErrorResponse("Swap not found"))
                            .build();
                }

                if (response.getStatus() == AtomicSwapResponse.SwapStatus.FAILED) {
                    return Response.status(Response.Status.CONFLICT)
                            .entity(response)
                            .build();
                }

                LOG.infof("Atomic swap completed successfully: %s", swapId);
                return Response.ok(response).build();

            } catch (Exception e) {
                LOG.error("Error completing atomic swap", e);
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                        .entity(createErrorResponse("Error completing swap: " + e.getMessage()))
                        .build();
            }
        });
    }

    /**
     * Refund atomic swap after timelock expires
     * Transitions from INITIATED/LOCKED to REFUNDED state
     *
     * Called after timelock expiry to automatically refund initiator's funds
     * if counterparty never locked or revealed the secret.
     */
    @POST
    @Path("/{swapId}/refund")
    @Operation(
            summary = "Refund atomic swap after timelock expiry",
            description = "Process refund after HTLC timelock expires"
    )
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Swap refunded successfully"),
            @APIResponse(responseCode = "404", description = "Swap not found"),
            @APIResponse(responseCode = "409", description = "Swap cannot be refunded (not expired or in final state)"),
            @APIResponse(responseCode = "500", description = "Server error")
    })
    public Uni<Response> refundAtomicSwap(
            @PathParam("swapId") String swapId,
            @QueryParam("refundTxHash") String refundTxHash) {

        LOG.infof("Refunding atomic swap: %s", swapId);

        return Uni.createFrom().item(() -> {
            try {
                AtomicSwapResponse response = hashTimeLockContract.refundSwap(swapId, refundTxHash);

                if (response == null) {
                    return Response.status(Response.Status.NOT_FOUND)
                            .entity(createErrorResponse("Swap not found"))
                            .build();
                }

                if (response.getStatus() == AtomicSwapResponse.SwapStatus.FAILED) {
                    return Response.status(Response.Status.CONFLICT)
                            .entity(response)
                            .build();
                }

                LOG.infof("Atomic swap refunded: %s", swapId);
                return Response.ok(response).build();

            } catch (Exception e) {
                LOG.error("Error refunding atomic swap", e);
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                        .entity(createErrorResponse("Error refunding swap: " + e.getMessage()))
                        .build();
            }
        });
    }

    /**
     * Get atomic swap status
     *
     * Retrieve current status and details of an atomic swap including:
     * - Current state (INITIATED, LOCKED, REVEALED, COMPLETED, EXPIRED, REFUNDED, FAILED)
     * - Timelock expiry information
     * - Transaction hashes on source and target chains
     * - Event audit trail
     */
    @GET
    @Path("/{swapId}/status")
    @Operation(
            summary = "Get atomic swap status",
            description = "Retrieve current status and details of an atomic swap"
    )
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Swap status retrieved"),
            @APIResponse(responseCode = "404", description = "Swap not found"),
            @APIResponse(responseCode = "500", description = "Server error")
    })
    public Uni<Response> getAtomicSwapStatus(
            @PathParam("swapId") String swapId) {

        LOG.debugf("Retrieving status for atomic swap: %s", swapId);

        return Uni.createFrom().item(() -> {
            try {
                AtomicSwapResponse response = hashTimeLockContract.getSwapStatus(swapId);

                if (response == null) {
                    return Response.status(Response.Status.NOT_FOUND)
                            .entity(createErrorResponse("Swap not found"))
                            .build();
                }

                return Response.ok(response).build();

            } catch (Exception e) {
                LOG.error("Error retrieving atomic swap status", e);
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                        .entity(createErrorResponse("Error retrieving status: " + e.getMessage()))
                        .build();
            }
        });
    }

    /**
     * Generate a random secret for HTLC
     *
     * Utility endpoint to generate a cryptographically secure random 32-byte
     * secret. The client should hash this secret to create the hashLock.
     *
     * Usage:
     * 1. Call this endpoint to get a random secret
     * 2. Hash the secret using desired algorithm (SHA256/SHA3)
     * 3. Use hash as hashLock in swap initiation
     * 4. Store secret securely until reveal step
     */
    @POST
    @Path("/generate-secret")
    @Operation(
            summary = "Generate random secret for HTLC",
            description = "Generate a cryptographically secure random secret (32 bytes)"
    )
    @APIResponse(responseCode = "200", description = "Secret generated successfully")
    public Uni<Response> generateSecret() {

        LOG.info("Generating new secret for HTLC");

        return Uni.createFrom().item(() -> {
            try {
                String secret = hashTimeLockContract.generateSecret();

                SecretResponse secretResponse = new SecretResponse(secret);
                LOG.infof("Secret generated successfully");

                return Response.ok(secretResponse).build();

            } catch (Exception e) {
                LOG.error("Error generating secret", e);
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                        .entity(createErrorResponse("Error generating secret: " + e.getMessage()))
                        .build();
            }
        });
    }

    /**
     * Compute hash of a secret
     *
     * Utility endpoint to compute the hash of a secret using specified algorithm.
     * This is used to create the hashLock for HTLC initiation.
     *
     * Supported algorithms:
     * - SHA256: Standard 256-bit hash
     * - SHA3: SHA3-256 hash
     */
    @POST
    @Path("/compute-hash")
    @Operation(
            summary = "Compute hash of secret",
            description = "Compute hash of a secret using specified algorithm (SHA256/SHA3)"
    )
    @APIResponse(responseCode = "200", description = "Hash computed successfully")
    public Uni<Response> computeHash(
            @QueryParam("secret") String secret,
            @QueryParam("algorithm") String algorithm) {

        LOG.infof("Computing hash for secret using algorithm: %s", algorithm);

        return Uni.createFrom().item(() -> {
            try {
                if (secret == null || secret.isEmpty()) {
                    return Response.status(Response.Status.BAD_REQUEST)
                            .entity(createErrorResponse("Secret is required"))
                            .build();
                }

                if (algorithm == null || algorithm.isEmpty()) {
                    return Response.status(Response.Status.BAD_REQUEST)
                            .entity(createErrorResponse("Algorithm is required (SHA256/SHA3)"))
                            .build();
                }

                String hash = hashTimeLockContract.computeHash(secret, algorithm);

                if (hash == null) {
                    return Response.status(Response.Status.BAD_REQUEST)
                            .entity(createErrorResponse("Unsupported hash algorithm: " + algorithm))
                            .build();
                }

                HashResponse hashResponse = new HashResponse(hash, algorithm);
                LOG.infof("Hash computed successfully using %s", algorithm);

                return Response.ok(hashResponse).build();

            } catch (Exception e) {
                LOG.error("Error computing hash", e);
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                        .entity(createErrorResponse("Error computing hash: " + e.getMessage()))
                        .build();
            }
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

    /**
     * Secret response DTO
     */
    public static class SecretResponse {
        public String secret;
        public long generatedAt;

        public SecretResponse(String secret) {
            this.secret = secret;
            this.generatedAt = System.currentTimeMillis();
        }
    }

    /**
     * Hash response DTO
     */
    public static class HashResponse {
        public String hash;
        public String algorithm;
        public long computedAt;

        public HashResponse(String hash, String algorithm) {
            this.hash = hash;
            this.algorithm = algorithm;
            this.computedAt = System.currentTimeMillis();
        }
    }
}
