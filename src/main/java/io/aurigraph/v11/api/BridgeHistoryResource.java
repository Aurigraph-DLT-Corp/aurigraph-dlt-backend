package io.aurigraph.v11.api;

import io.aurigraph.v11.models.BridgeTransaction;
import io.aurigraph.v11.models.BridgeTransactionHistory;
import io.aurigraph.v11.services.BridgeHistoryService;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.Map;

/**
 * Bridge History API
 * Provides cross-chain bridge transaction history with pagination and filtering
 *
 * AV11-282: Implement Bridge Transaction History API
 *
 * Endpoints:
 * - GET /api/v11/bridge/history - Get paginated transaction history
 * - GET /api/v11/bridge/history/{transactionId} - Get specific transaction
 * - GET /api/v11/bridge/history/user/{address} - Get user's transaction history
 *
 * @author Aurigraph V11
 * @version 11.3.0
 */
@Path("/api/v11/bridge/history")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Bridge History", description = "Cross-chain bridge transaction history with pagination and filtering")
public class BridgeHistoryResource {

    private static final Logger LOG = Logger.getLogger(BridgeHistoryResource.class);

    @Inject
    BridgeHistoryService bridgeHistoryService;

    /**
     * Get paginated transaction history
     *
     * Returns paginated bridge transaction history with optional filters:
     * - Source/target chain filtering
     * - Asset filtering
     * - Status filtering (pending, processing, completed, failed)
     * - Amount range filtering
     * - User address filtering
     */
    @GET
    @Operation(summary = "Get bridge transaction history",
               description = "Returns paginated bridge transaction history with optional filters")
    @APIResponses(value = {
            @APIResponse(responseCode = "200",
                         description = "Transaction history retrieved successfully",
                         content = @Content(mediaType = MediaType.APPLICATION_JSON,
                                          schema = @Schema(implementation = BridgeTransactionHistory.class))),
            @APIResponse(responseCode = "400",
                         description = "Invalid request parameters"),
            @APIResponse(responseCode = "500",
                         description = "Internal server error")
    })
    public Uni<Response> getTransactionHistory(
            @Parameter(description = "Page number (1-indexed)") @QueryParam("page") @DefaultValue("1") int page,
            @Parameter(description = "Page size (max 100)") @QueryParam("pageSize") @DefaultValue("20") int pageSize,
            @Parameter(description = "Source chain filter") @QueryParam("sourceChain") String sourceChain,
            @Parameter(description = "Target chain filter") @QueryParam("targetChain") String targetChain,
            @Parameter(description = "Asset filter") @QueryParam("asset") String asset,
            @Parameter(description = "Status filter") @QueryParam("status") String status,
            @Parameter(description = "User address filter") @QueryParam("userAddress") String userAddress,
            @Parameter(description = "Minimum amount (USD)") @QueryParam("minAmount") Double minAmount,
            @Parameter(description = "Maximum amount (USD)") @QueryParam("maxAmount") Double maxAmount
    ) {
        LOG.infof("GET /api/v11/bridge/history - page=%d, pageSize=%d, filters: source=%s, target=%s, asset=%s, status=%s",
                page, pageSize, sourceChain, targetChain, asset, status);

        // Validate parameters
        if (page < 1) {
            return Uni.createFrom().item(
                    Response.status(Response.Status.BAD_REQUEST)
                            .entity(Map.of("error", "Page must be >= 1"))
                            .build()
            );
        }

        if (pageSize < 1 || pageSize > 100) {
            return Uni.createFrom().item(
                    Response.status(Response.Status.BAD_REQUEST)
                            .entity(Map.of("error", "Page size must be between 1 and 100"))
                            .build()
            );
        }

        return bridgeHistoryService.getTransactionHistory(
                        page, pageSize, sourceChain, targetChain, asset, status,
                        userAddress, minAmount, maxAmount
                )
                .map(history -> {
                    LOG.debugf("Retrieved %d transactions (page %d/%d)",
                            history.getTransactions().size(),
                            history.getPagination().getPage(),
                            history.getPagination().getTotalPages());

                    return Response.ok(history).build();
                })
                .onFailure().recoverWithItem(throwable -> {
                    LOG.error("Error retrieving transaction history", throwable);
                    return (Response) Response.serverError()
                            .entity(Map.of(
                                    "error", "Failed to retrieve transaction history",
                                    "message", throwable.getMessage()
                            ))
                            .build();
                });
    }

    /**
     * Get specific transaction by ID
     *
     * Returns detailed information for a specific bridge transaction
     *
     * @param transactionId Transaction identifier
     */
    @GET
    @Path("/{transactionId}")
    @Operation(summary = "Get transaction by ID",
               description = "Returns detailed information for a specific bridge transaction")
    @APIResponses(value = {
            @APIResponse(responseCode = "200",
                         description = "Transaction retrieved successfully",
                         content = @Content(mediaType = MediaType.APPLICATION_JSON,
                                          schema = @Schema(implementation = BridgeTransaction.class))),
            @APIResponse(responseCode = "404",
                         description = "Transaction not found"),
            @APIResponse(responseCode = "500",
                         description = "Internal server error")
    })
    public Uni<Response> getTransactionById(@PathParam("transactionId") String transactionId) {
        LOG.debugf("GET /api/v11/bridge/history/%s - Fetching transaction", transactionId);

        return bridgeHistoryService.getTransactionById(transactionId)
                .map(transaction -> {
                    if (transaction == null) {
                        return Response.status(Response.Status.NOT_FOUND)
                                .entity(Map.of(
                                        "error", "Transaction not found",
                                        "transactionId", transactionId
                                ))
                                .build();
                    }

                    return Response.ok(transaction).build();
                })
                .onFailure().recoverWithItem(throwable -> {
                    LOG.error("Error retrieving transaction by ID", throwable);
                    return (Response) Response.serverError()
                            .entity(Map.of(
                                    "error", "Failed to retrieve transaction",
                                    "message", throwable.getMessage()
                            ))
                            .build();
                });
    }

    /**
     * Get user's transaction history
     *
     * Returns all transactions for a specific user address
     *
     * @param address User's wallet address
     */
    @GET
    @Path("/user/{address}")
    @Operation(summary = "Get user transaction history",
               description = "Returns paginated transaction history for a specific user address")
    @APIResponses(value = {
            @APIResponse(responseCode = "200",
                         description = "User transaction history retrieved successfully"),
            @APIResponse(responseCode = "400",
                         description = "Invalid address format"),
            @APIResponse(responseCode = "500",
                         description = "Internal server error")
    })
    public Uni<Response> getUserTransactionHistory(
            @PathParam("address") String address,
            @Parameter(description = "Page number (1-indexed)") @QueryParam("page") @DefaultValue("1") int page,
            @Parameter(description = "Page size (max 100)") @QueryParam("pageSize") @DefaultValue("20") int pageSize
    ) {
        LOG.debugf("GET /api/v11/bridge/history/user/%s - page=%d, pageSize=%d",
                address, page, pageSize);

        // Basic address validation
        if (!address.startsWith("0x") || address.length() != 42) {
            return Uni.createFrom().item(
                    Response.status(Response.Status.BAD_REQUEST)
                            .entity(Map.of("error", "Invalid address format"))
                            .build()
            );
        }

        return bridgeHistoryService.getTransactionHistory(
                        page, pageSize, null, null, null, null,
                        address, null, null
                )
                .map(history -> {
                    LOG.debugf("Retrieved %d transactions for user %s",
                            history.getTransactions().size(), address);

                    return Response.ok(history).build();
                })
                .onFailure().recoverWithItem(throwable -> {
                    LOG.error("Error retrieving user transaction history", throwable);
                    return (Response) Response.serverError()
                            .entity(Map.of(
                                    "error", "Failed to retrieve user transaction history",
                                    "message", throwable.getMessage()
                            ))
                            .build();
                });
    }

    /**
     * Get transaction summary statistics
     *
     * Returns aggregated statistics across all transactions
     */
    @GET
    @Path("/summary")
    @Operation(summary = "Get transaction summary",
               description = "Returns aggregated statistics across all bridge transactions")
    @APIResponses(value = {
            @APIResponse(responseCode = "200",
                         description = "Summary retrieved successfully"),
            @APIResponse(responseCode = "500",
                         description = "Internal server error")
    })
    public Uni<Response> getTransactionSummary() {
        LOG.debug("GET /api/v11/bridge/history/summary - Fetching summary");

        return bridgeHistoryService.getTransactionHistory(1, 1, null, null, null, null, null, null, null)
                .map(history -> {
                    Map<String, Object> summary = Map.of(
                            "total_transactions", history.getSummary().getTotalTransactions(),
                            "total_volume_usd", history.getSummary().getTotalVolumeUsd(),
                            "completed_count", history.getSummary().getCompletedCount(),
                            "pending_count", history.getSummary().getPendingCount(),
                            "failed_count", history.getSummary().getFailedCount(),
                            "average_duration_seconds", history.getSummary().getAverageDurationSeconds(),
                            "success_rate", history.getSummary().getTotalTransactions() > 0
                                    ? (double) history.getSummary().getCompletedCount() / history.getSummary().getTotalTransactions()
                                    : 0.0
                    );

                    return Response.ok(summary).build();
                })
                .onFailure().recoverWithItem(throwable -> {
                    LOG.error("Error retrieving transaction summary", throwable);
                    return (Response) Response.serverError()
                            .entity(Map.of(
                                    "error", "Failed to retrieve transaction summary",
                                    "message", throwable.getMessage()
                            ))
                            .build();
                });
    }
}
