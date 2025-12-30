package io.aurigraph.v11.api;

import io.aurigraph.v11.bridge.models.AtomicSwapResponse;
import io.aurigraph.v11.bridge.models.TransferResponse;
import io.aurigraph.v11.bridge.services.BridgeQueryService;
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

import java.time.Instant;

/**
 * Bridge Status & Query Controller
 * REST API endpoints for querying bridge transactions, transfers, and swaps
 *
 * Endpoint: /api/v11/bridge/query/*
 *
 * Features:
 * - Paginated transaction history
 * - Advanced filtering (status, date range, address)
 * - Sorting (timestamp, amount, status)
 * - Transaction summary statistics
 * - Redis caching for performance
 *
 * @author Backend Development Agent (BDA)
 * @version 1.0
 * @since AV11-637
 */
@Path("/api/v11/bridge/query")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Bridge Status & Query", description = "Query and filter bridge transactions, transfers, and swaps")
public class BridgeStatusQueryController {

    private static final Logger LOG = Logger.getLogger(BridgeStatusQueryController.class);

    @Inject
    BridgeQueryService bridgeQueryService;

    /**
     * Get paginated transfer history
     *
     * Retrieve a paginated list of bridge transfers with optional filtering and sorting.
     *
     * Filtering:
     * - address: Filter by source or target address
     * - status: Filter by transfer status (PENDING, SIGNED, APPROVED, EXECUTING, COMPLETED, FAILED, CANCELLED)
     * - startDate: Filter by start date (ISO 8601 format)
     * - endDate: Filter by end date (ISO 8601 format)
     *
     * Sorting:
     * - sortBy: Field to sort by (timestamp, amount, status)
     * - sortOrder: Sort order (asc, desc)
     *
     * Pagination:
     * - pageNumber: Page number (1-based, default 1)
     * - pageSize: Items per page (default 50, max 200)
     */
    @GET
    @Path("/transfers")
    @Operation(
            summary = "Get transfer history",
            description = "Retrieve paginated list of bridge transfers with filtering and sorting"
    )
    @APIResponses({
            @APIResponse(
                    responseCode = "200",
                    description = "Transfer history retrieved successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = BridgeQueryService.PaginatedResponse.class)
                    )
            ),
            @APIResponse(responseCode = "400", description = "Invalid query parameters"),
            @APIResponse(responseCode = "500", description = "Server error")
    })
    public Uni<Response> getTransferHistory(
            @Parameter(description = "Source or target address filter")
            @QueryParam("address") String address,

            @Parameter(description = "Transfer status filter")
            @QueryParam("status") String statusStr,

            @Parameter(description = "Start date filter (ISO 8601)")
            @QueryParam("startDate") String startDateStr,

            @Parameter(description = "End date filter (ISO 8601)")
            @QueryParam("endDate") String endDateStr,

            @Parameter(description = "Field to sort by (timestamp, amount, status)")
            @QueryParam("sortBy") String sortBy,

            @Parameter(description = "Sort order (asc, desc)")
            @QueryParam("sortOrder") String sortOrder,

            @Parameter(description = "Page number (1-based, default 1)")
            @QueryParam("pageNumber") @DefaultValue("1") int pageNumber,

            @Parameter(description = "Items per page (default 50, max 200)")
            @QueryParam("pageSize") @DefaultValue("50") int pageSize) {

        LOG.infof("Transfer history query - address: %s, status: %s, page: %d, size: %d",
                 address, statusStr, pageNumber, pageSize);

        return Uni.createFrom().item(() -> {
            try {
                // Parse status filter
                TransferResponse.TransferStatus status = null;
                if (statusStr != null && !statusStr.isEmpty()) {
                    try {
                        status = TransferResponse.TransferStatus.valueOf(statusStr.toUpperCase());
                    } catch (IllegalArgumentException e) {
                        return Response.status(Response.Status.BAD_REQUEST)
                                .entity(createErrorResponse("Invalid status: " + statusStr))
                                .build();
                    }
                }

                // Parse date filters
                Instant startDate = null;
                if (startDateStr != null && !startDateStr.isEmpty()) {
                    startDate = Instant.parse(startDateStr);
                }

                Instant endDate = null;
                if (endDateStr != null && !endDateStr.isEmpty()) {
                    endDate = Instant.parse(endDateStr);
                }

                BridgeQueryService.PaginatedResponse<TransferResponse> result =
                        bridgeQueryService.getTransfersHistory(
                                address, status, startDate, endDate,
                                sortBy, sortOrder, pageNumber, pageSize);

                return Response.ok(result).build();

            } catch (Exception e) {
                LOG.error("Error querying transfer history", e);
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                        .entity(createErrorResponse("Error querying transfers: " + e.getMessage()))
                        .build();
            }
        });
    }

    /**
     * Get paginated atomic swap history
     *
     * Retrieve a paginated list of atomic swaps with optional filtering and sorting.
     *
     * Filtering:
     * - address: Filter by initiator or counterparty address
     * - status: Filter by swap status (INITIATED, LOCKED, REVEALED, COMPLETED, EXPIRED, REFUNDED, FAILED)
     * - startDate: Filter by start date (ISO 8601 format)
     * - endDate: Filter by end date (ISO 8601 format)
     *
     * Sorting:
     * - sortBy: Field to sort by (timestamp, amount, status)
     * - sortOrder: Sort order (asc, desc)
     *
     * Pagination:
     * - pageNumber: Page number (1-based, default 1)
     * - pageSize: Items per page (default 50, max 200)
     */
    @GET
    @Path("/swaps")
    @Operation(
            summary = "Get atomic swap history",
            description = "Retrieve paginated list of atomic swaps with filtering and sorting"
    )
    @APIResponses({
            @APIResponse(
                    responseCode = "200",
                    description = "Swap history retrieved successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = BridgeQueryService.PaginatedResponse.class)
                    )
            ),
            @APIResponse(responseCode = "400", description = "Invalid query parameters"),
            @APIResponse(responseCode = "500", description = "Server error")
    })
    public Uni<Response> getSwapHistory(
            @Parameter(description = "Initiator or counterparty address filter")
            @QueryParam("address") String address,

            @Parameter(description = "Swap status filter")
            @QueryParam("status") String statusStr,

            @Parameter(description = "Start date filter (ISO 8601)")
            @QueryParam("startDate") String startDateStr,

            @Parameter(description = "End date filter (ISO 8601)")
            @QueryParam("endDate") String endDateStr,

            @Parameter(description = "Field to sort by (timestamp, amount, status)")
            @QueryParam("sortBy") String sortBy,

            @Parameter(description = "Sort order (asc, desc)")
            @QueryParam("sortOrder") String sortOrder,

            @Parameter(description = "Page number (1-based, default 1)")
            @QueryParam("pageNumber") @DefaultValue("1") int pageNumber,

            @Parameter(description = "Items per page (default 50, max 200)")
            @QueryParam("pageSize") @DefaultValue("50") int pageSize) {

        LOG.infof("Swap history query - address: %s, status: %s, page: %d, size: %d",
                 address, statusStr, pageNumber, pageSize);

        return Uni.createFrom().item(() -> {
            try {
                // Parse status filter
                AtomicSwapResponse.SwapStatus status = null;
                if (statusStr != null && !statusStr.isEmpty()) {
                    try {
                        status = AtomicSwapResponse.SwapStatus.valueOf(statusStr.toUpperCase());
                    } catch (IllegalArgumentException e) {
                        return Response.status(Response.Status.BAD_REQUEST)
                                .entity(createErrorResponse("Invalid status: " + statusStr))
                                .build();
                    }
                }

                // Parse date filters
                Instant startDate = null;
                if (startDateStr != null && !startDateStr.isEmpty()) {
                    startDate = Instant.parse(startDateStr);
                }

                Instant endDate = null;
                if (endDateStr != null && !endDateStr.isEmpty()) {
                    endDate = Instant.parse(endDateStr);
                }

                BridgeQueryService.PaginatedResponse<AtomicSwapResponse> result =
                        bridgeQueryService.getSwapsHistory(
                                address, status, startDate, endDate,
                                sortBy, sortOrder, pageNumber, pageSize);

                return Response.ok(result).build();

            } catch (Exception e) {
                LOG.error("Error querying swap history", e);
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                        .entity(createErrorResponse("Error querying swaps: " + e.getMessage()))
                        .build();
            }
        });
    }

    /**
     * Get transaction summary statistics
     *
     * Retrieve summary statistics for bridge transactions within a date range
     * and optional address filter.
     *
     * Statistics include:
     * - Total number of transactions
     * - Total volume processed
     * - Average transaction value
     * - Success rate (percentage)
     * - Number of failed transactions
     * - Number of pending transactions
     * - Average processing time
     */
    @GET
    @Path("/summary")
    @Operation(
            summary = "Get transaction summary statistics",
            description = "Retrieve summary statistics for bridge transactions"
    )
    @APIResponses({
            @APIResponse(
                    responseCode = "200",
                    description = "Summary statistics retrieved successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = BridgeQueryService.TransactionSummary.class)
                    )
            ),
            @APIResponse(responseCode = "400", description = "Invalid date range"),
            @APIResponse(responseCode = "500", description = "Server error")
    })
    public Uni<Response> getTransactionSummary(
            @Parameter(description = "Address filter")
            @QueryParam("address") String address,

            @Parameter(description = "Start date filter (ISO 8601)")
            @QueryParam("startDate") String startDateStr,

            @Parameter(description = "End date filter (ISO 8601)")
            @QueryParam("endDate") String endDateStr) {

        LOG.infof("Transaction summary query - address: %s", address);

        return Uni.createFrom().item(() -> {
            try {
                // Parse date filters
                Instant startDate = null;
                if (startDateStr != null && !startDateStr.isEmpty()) {
                    startDate = Instant.parse(startDateStr);
                }

                Instant endDate = null;
                if (endDateStr != null && !endDateStr.isEmpty()) {
                    endDate = Instant.parse(endDateStr);
                }

                BridgeQueryService.TransactionSummary result =
                        bridgeQueryService.getTransactionSummary(address, startDate, endDate);

                return Response.ok(result).build();

            } catch (Exception e) {
                LOG.error("Error calculating transaction summary", e);
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                        .entity(createErrorResponse("Error calculating summary: " + e.getMessage()))
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
}
