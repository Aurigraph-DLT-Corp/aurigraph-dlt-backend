package io.aurigraph.grpc.service;

import com.google.protobuf.Timestamp;
import io.aurigraph.v11.proto.BatchProcessingStatus;
import io.aurigraph.v11.proto.BatchTransactionSubmissionRequest;
import io.aurigraph.v11.proto.BatchTransactionSubmissionResponse;
import io.aurigraph.v11.proto.CancelTransactionRequest;
import io.aurigraph.v11.proto.CancelTransactionResponse;
import io.aurigraph.v11.proto.EstimateGasCostRequest;
import io.aurigraph.v11.proto.GasEstimate;
import io.aurigraph.v11.proto.GetPendingTransactionsRequest;
import io.aurigraph.v11.proto.GetTransactionHistoryRequest;
import io.aurigraph.v11.proto.GetTransactionStatusRequest;
import io.aurigraph.v11.proto.GetTxPoolSizeRequest;
import io.aurigraph.v11.proto.PendingTransactionsResponse;
import io.aurigraph.v11.proto.ResendTransactionRequest;
import io.aurigraph.v11.proto.ResendTransactionResponse;
import io.aurigraph.v11.proto.StreamTransactionEventsRequest;
import io.aurigraph.v11.proto.SubmitTransactionRequest;
import io.aurigraph.v11.proto.Transaction;
import io.aurigraph.v11.proto.TransactionEvent;
import io.aurigraph.v11.proto.TransactionHistoryResponse;
import io.aurigraph.v11.proto.TransactionReceipt;
import io.aurigraph.v11.proto.TransactionService;
import io.aurigraph.v11.proto.TransactionSignatureValidationResult;
import io.aurigraph.v11.proto.TransactionStatus;
import io.aurigraph.v11.proto.TransactionStatusResponse;
import io.aurigraph.v11.proto.TransactionSubmissionResponse;
import io.aurigraph.v11.proto.TxPoolStatistics;
import io.aurigraph.v11.proto.ValidateTransactionSignatureRequest;
import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import jakarta.inject.Inject;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@GrpcService
public class TransactionServiceImpl implements TransactionService {

    @Inject
    io.aurigraph.v11.TransactionService transactionService;

    @Override
    public Uni<TransactionSubmissionResponse> submitTransaction(SubmitTransactionRequest request) {
        // In a real implementation, we would delegate to transactionService.submitTransaction(request.getTransaction())
        return Uni.createFrom().item(TransactionSubmissionResponse.newBuilder()
                .setTransactionHash(UUID.randomUUID().toString())
                .setStatus(TransactionStatus.TRANSACTION_PENDING)
                .setTimestamp(toTimestamp(Instant.now()))
                .setMessage("Transaction submitted successfully")
                .build());
    }

    @Override
    public Uni<BatchTransactionSubmissionResponse> batchSubmitTransactions(BatchTransactionSubmissionRequest request) {
        return Uni.createFrom().item(BatchTransactionSubmissionResponse.newBuilder()
                .setBatchId(UUID.randomUUID().toString())
                .setAcceptedCount(request.getTransactionsCount())
                .setRejectedCount(0)
                .setTimestamp(toTimestamp(Instant.now()))
                .build());
    }

    @Override
    public Uni<TransactionStatusResponse> getTransactionStatus(GetTransactionStatusRequest request) {
        return Uni.createFrom().item(TransactionStatusResponse.newBuilder()
                .setStatus(TransactionStatus.TRANSACTION_CONFIRMED)
                .setConfirmations(1)
                .setTimestamp(toTimestamp(Instant.now()))
                .build());
    }

    @Override
    public Uni<TransactionReceipt> getTransactionReceipt(GetTransactionStatusRequest request) {
        return Uni.createFrom().item(TransactionReceipt.newBuilder()
                .setStatus(TransactionStatus.TRANSACTION_CONFIRMED)
                .setBlockHeight(100)
                .setGasUsed(21000)
                .setExecutionTime(toTimestamp(Instant.now()))
                .build());
    }

    @Override
    public Uni<CancelTransactionResponse> cancelTransaction(CancelTransactionRequest request) {
        return Uni.createFrom().item(CancelTransactionResponse.newBuilder()
                .setCancellationSuccessful(true)
                .setReason("Cancelled by user")
                .setTimestamp(toTimestamp(Instant.now()))
                .build());
    }

    @Override
    public Uni<ResendTransactionResponse> resendTransaction(ResendTransactionRequest request) {
        return Uni.createFrom().item(ResendTransactionResponse.newBuilder()
                .setOriginalTransactionHash(request.getOriginalTransactionHash())
                .setNewTransactionHash(UUID.randomUUID().toString())
                .setStatus(TransactionStatus.TRANSACTION_PENDING)
                .setTimestamp(toTimestamp(Instant.now()))
                .build());
    }

    @Override
    public Uni<GasEstimate> estimateGasCost(EstimateGasCostRequest request) {
        return Uni.createFrom().item(GasEstimate.newBuilder()
                .setEstimatedGas(21000)
                .setGasPriceWei(1000000000) // 1 Gwei
                .setTotalCost("21000000000000")
                .setTimestamp(toTimestamp(Instant.now()))
                .build());
    }

    @Override
    public Uni<TransactionSignatureValidationResult> validateTransactionSignature(ValidateTransactionSignatureRequest request) {
        return Uni.createFrom().item(TransactionSignatureValidationResult.newBuilder()
                .setSignatureValid(true)
                .setSenderValid(true)
                .setNonceValid(true)
                .setTimestamp(toTimestamp(Instant.now()))
                .build());
    }

    @Override
    public Uni<PendingTransactionsResponse> getPendingTransactions(GetPendingTransactionsRequest request) {
        return Uni.createFrom().item(PendingTransactionsResponse.newBuilder()
                .setTotalPending(0)
                .setAverageGasPrice(1000000000)
                .setQueryTime(toTimestamp(Instant.now()))
                .build());
    }

    @Override
    public Uni<TransactionHistoryResponse> getTransactionHistory(GetTransactionHistoryRequest request) {
        return Uni.createFrom().item(TransactionHistoryResponse.newBuilder()
                .setTotalCount(0)
                .setReturnedCount(0)
                .setQueryTime(toTimestamp(Instant.now()))
                .build());
    }

    @Override
    public Uni<TxPoolStatistics> getTxPoolSize(GetTxPoolSizeRequest request) {
        return Uni.createFrom().item(TxPoolStatistics.newBuilder()
                .setTotalPending(0)
                .setTotalQueued(0)
                .setTimestamp(toTimestamp(Instant.now()))
                .build());
    }

    @Override
    public Multi<TransactionEvent> streamTransactionEvents(StreamTransactionEventsRequest request) {
        return Multi.createFrom().ticks().every(Duration.ofMillis(1000))
                .onItem().transform(tick -> TransactionEvent.newBuilder()
                        .setEventType("TRANSACTION_CONFIRMED")
                        .setStatus(TransactionStatus.TRANSACTION_CONFIRMED)
                        .setTimestamp(toTimestamp(Instant.now()))
                        .build())
                .runSubscriptionOn(Infrastructure.getDefaultWorkerPool());
    }

    private Timestamp toTimestamp(Instant instant) {
        if (instant == null) return Timestamp.getDefaultInstance();
        return Timestamp.newBuilder()
                .setSeconds(instant.getEpochSecond())
                .setNanos(instant.getNano())
                .build();
    }
}
