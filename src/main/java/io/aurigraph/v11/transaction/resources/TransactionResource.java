package io.aurigraph.v11.transaction.resources;

import io.aurigraph.v11.transaction.models.Transaction;
import io.aurigraph.v11.transaction.services.TransactionService;
import io.aurigraph.v11.transaction.dto.SubmitTransactionRequest;
import io.aurigraph.v11.transaction.dto.TransactionResponse;
import io.quarkus.logging.Log;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.stream.Collectors;

@Path("/api/v11/transactions")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class TransactionResource {

    @Inject
    TransactionService transactionService;

    @POST
    public Response submitTransaction(SubmitTransactionRequest request) {
        try {
            Log.infof("POST /transactions - Submitting transaction");

            Transaction transaction = transactionService.submitTransaction(
                request.userId,
                request.fromAddress,
                request.toAddress,
                request.amount,
                request.transactionType,
                request.gasLimit,
                request.gasPrice,
                request.data
            );

            return Response.status(Response.Status.ACCEPTED)
                .entity(toResponse(transaction))
                .build();
        } catch (Exception e) {
            Log.errorf(e, "Failed to submit transaction");
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ErrorResponse(e.getMessage()))
                .build();
        }
    }

    @GET
    @Path("/{txHash}")
    public Response getTransaction(@PathParam("txHash") String txHash) {
        try {
            var transaction = transactionService.getTransaction(txHash);
            if (transaction.isEmpty()) {
                return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorResponse("Transaction not found"))
                    .build();
            }

            return Response.ok(toResponse(transaction.get())).build();
        } catch (Exception e) {
            Log.errorf(e, "Failed to get transaction");
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ErrorResponse(e.getMessage()))
                .build();
        }
    }

    @GET
    @Path("/user/{userId}")
    public Response getUserTransactions(@PathParam("userId") String userId) {
        try {
            List<TransactionResponse> transactions = transactionService
                .getUserTransactions(userId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

            return Response.ok(transactions).build();
        } catch (Exception e) {
            Log.errorf(e, "Failed to get user transactions");
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ErrorResponse(e.getMessage()))
                .build();
        }
    }

    @GET
    @Path("/pending")
    public Response getPendingTransactions() {
        try {
            List<TransactionResponse> transactions = transactionService
                .getPendingTransactions()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

            return Response.ok(transactions).build();
        } catch (Exception e) {
            Log.errorf(e, "Failed to get pending transactions");
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ErrorResponse(e.getMessage()))
                .build();
        }
    }

    @POST
    @Path("/{txHash}/confirm")
    public Response confirmTransaction(
        @PathParam("txHash") String txHash,
        @QueryParam("blockNumber") Long blockNumber,
        @QueryParam("blockHash") String blockHash
    ) {
        try {
            Log.infof("POST /transactions/{%s}/confirm", txHash);

            transactionService.confirmTransaction(txHash, blockNumber, blockHash);

            return Response.ok(new SuccessResponse("Transaction confirmed")).build();
        } catch (Exception e) {
            Log.errorf(e, "Failed to confirm transaction");
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ErrorResponse(e.getMessage()))
                .build();
        }
    }

    @POST
    @Path("/{txHash}/finalize")
    public Response finalizeTransaction(@PathParam("txHash") String txHash) {
        try {
            Log.infof("POST /transactions/{%s}/finalize", txHash);

            transactionService.finalizeTransaction(txHash);

            return Response.ok(new SuccessResponse("Transaction finalized")).build();
        } catch (Exception e) {
            Log.errorf(e, "Failed to finalize transaction");
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ErrorResponse(e.getMessage()))
                .build();
        }
    }

    @GET
    @Path("/stats")
    public Response getTransactionStats() {
        try {
            long pending = transactionService.getPendingTransactionCount();
            return Response.ok(new StatsResponse(pending)).build();
        } catch (Exception e) {
            Log.errorf(e, "Failed to get transaction stats");
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ErrorResponse(e.getMessage()))
                .build();
        }
    }

    private TransactionResponse toResponse(Transaction transaction) {
        return new TransactionResponse(
            transaction.id,
            transaction.txHash,
            transaction.userId,
            transaction.fromAddress,
            transaction.toAddress,
            transaction.amount,
            transaction.status.name(),
            transaction.transactionType.name(),
            transaction.totalFee,
            transaction.blockNumber,
            transaction.confirmationCount,
            transaction.finalityTimeMs,
            transaction.createdAt,
            transaction.confirmedAt,
            transaction.finalizedAt
        );
    }

    public static class ErrorResponse {
        public String error;

        public ErrorResponse(String error) {
            this.error = error;
        }
    }

    public static class SuccessResponse {
        public String message;

        public SuccessResponse(String message) {
            this.message = message;
        }
    }

    public static class StatsResponse {
        public long pendingTransactions;

        public StatsResponse(long pending) {
            this.pendingTransactions = pending;
        }
    }
}
