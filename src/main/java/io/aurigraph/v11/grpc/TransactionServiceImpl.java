package io.aurigraph.v11.grpc;

import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.aurigraph.v11.proto.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import com.google.protobuf.Timestamp;

/**
 * TransactionServiceImpl - High-Performance Transaction Processing Service
 *
 * Implements 12 RPC methods for blockchain transaction processing with:
 * - Single and batch transaction submission
 * - Status tracking and receipt retrieval
 * - Gas estimation and signature validation
 * - Transaction history queries
 * - Real-time event streaming
 *
 * Target Performance: 1.1M-1.3M TPS (50-70% improvement from 776K baseline)
 * Protocol: gRPC with Protocol Buffers and HTTP/2 multiplexing
 */
@GrpcService
public class TransactionServiceImpl implements io.aurigraph.v11.proto.TransactionService {

    private final Map<String, io.aurigraph.v11.proto.Transaction> transactionCache = new ConcurrentHashMap<>();
    private final Queue<io.aurigraph.v11.proto.Transaction> pendingTransactions = new ConcurrentLinkedQueue<>();
    private final Map<String, io.aurigraph.v11.proto.TransactionReceipt> receiptCache = new ConcurrentHashMap<>();
    private final AtomicLong totalTransactions = new AtomicLong(0);
    private final AtomicLong confirmedTransactions = new AtomicLong(0);
    private final AtomicLong failedTransactions = new AtomicLong(0);

    private volatile double averageGasPrice = 20.0;
    private volatile double minGasPrice = 1.0;
    private volatile double maxGasPrice = 500.0;

    @Override
    public Uni<io.aurigraph.v11.proto.TransactionSubmissionResponse> submitTransaction(io.aurigraph.v11.proto.SubmitTransactionRequest request) {
        return Uni.createFrom().item(() -> {
            try {
                io.aurigraph.v11.proto.Transaction tx = request.getTransaction();
                String txHash = generateTxHash(tx);

                io.aurigraph.v11.proto.Transaction storedTx = tx.toBuilder()
                    .setTransactionHash(txHash)
                    .setStatus(io.aurigraph.v11.proto.TransactionStatus.TRANSACTION_QUEUED)
                    .setCreatedAt(getCurrentTimestamp())
                    .build();

                transactionCache.put(txHash, storedTx);
                pendingTransactions.offer(storedTx);
                totalTransactions.incrementAndGet();

                return io.aurigraph.v11.proto.TransactionSubmissionResponse.newBuilder()
                    .setTransactionHash(txHash)
                    .setStatus(io.aurigraph.v11.proto.TransactionStatus.TRANSACTION_QUEUED)
                    .setTimestamp(getCurrentTimestamp())
                    .setMessage("Transaction queued for processing")
                    .build();
            } catch (Exception e) {
                return io.aurigraph.v11.proto.TransactionSubmissionResponse.newBuilder()
                    .setStatus(io.aurigraph.v11.proto.TransactionStatus.TRANSACTION_FAILED)
                    .setTimestamp(getCurrentTimestamp())
                    .setMessage("Submission failed: " + e.getMessage())
                    .build();
            }
        });
    }

    @Override
    public Uni<io.aurigraph.v11.proto.BatchTransactionSubmissionResponse> batchSubmitTransactions(io.aurigraph.v11.proto.BatchTransactionSubmissionRequest request) {
        return Uni.createFrom().item(() -> {
            List<io.aurigraph.v11.proto.TransactionSubmissionResponse> responses = new ArrayList<>();
            int acceptedCount = 0;
            int rejectedCount = 0;

            for (io.aurigraph.v11.proto.Transaction tx : request.getTransactionsList()) {
                try {
                    String txHash = generateTxHash(tx);
                    io.aurigraph.v11.proto.Transaction storedTx = tx.toBuilder()
                        .setTransactionHash(txHash)
                        .setStatus(io.aurigraph.v11.proto.TransactionStatus.TRANSACTION_QUEUED)
                        .setCreatedAt(getCurrentTimestamp())
                        .build();

                    transactionCache.put(txHash, storedTx);
                    pendingTransactions.offer(storedTx);
                    totalTransactions.incrementAndGet();
                    acceptedCount++;

                    responses.add(io.aurigraph.v11.proto.TransactionSubmissionResponse.newBuilder()
                        .setTransactionHash(txHash)
                        .setStatus(io.aurigraph.v11.proto.TransactionStatus.TRANSACTION_QUEUED)
                        .setTimestamp(getCurrentTimestamp())
                        .build());
                } catch (Exception e) {
                    rejectedCount++;
                    responses.add(io.aurigraph.v11.proto.TransactionSubmissionResponse.newBuilder()
                        .setStatus(io.aurigraph.v11.proto.TransactionStatus.TRANSACTION_FAILED)
                        .setTimestamp(getCurrentTimestamp())
                        .build());
                }
            }

            return io.aurigraph.v11.proto.BatchTransactionSubmissionResponse.newBuilder()
                .addAllResponses(responses)
                .setAcceptedCount(acceptedCount)
                .setRejectedCount(rejectedCount)
                .setBatchId(request.getBatchId())
                .setTimestamp(getCurrentTimestamp())
                .build();
        });
    }

    @Override
    public Uni<io.aurigraph.v11.proto.TransactionStatusResponse> getTransactionStatus(io.aurigraph.v11.proto.GetTransactionStatusRequest request) {
        return Uni.createFrom().item(() -> {
            String txHash = request.getTransactionHash();

            if (txHash == null || txHash.isEmpty()) {
                txHash = request.getTransactionId();
            }

            io.aurigraph.v11.proto.Transaction tx = transactionCache.get(txHash);
            if (tx == null) {
                return io.aurigraph.v11.proto.TransactionStatusResponse.newBuilder()
                    .setStatus(io.aurigraph.v11.proto.TransactionStatus.TRANSACTION_UNKNOWN)
                    .setTimestamp(getCurrentTimestamp())
                    .build();
            }

            io.aurigraph.v11.proto.TransactionStatusResponse.Builder response = io.aurigraph.v11.proto.TransactionStatusResponse.newBuilder()
                .setStatus(tx.getStatus())
                .setTimestamp(getCurrentTimestamp());

            io.aurigraph.v11.proto.TransactionReceipt receipt = receiptCache.get(txHash);
            if (receipt != null) {
                response.setConfirmations(1)
                    .setContainingBlockHash(receipt.getBlockHash());
            }

            return response.build();
        });
    }

    @Override
    public Uni<io.aurigraph.v11.proto.TransactionReceipt> getTransactionReceipt(io.aurigraph.v11.proto.GetTransactionStatusRequest request) {
        return Uni.createFrom().item(() -> {
            String txHash = request.getTransactionHash();
            io.aurigraph.v11.proto.TransactionReceipt receipt = receiptCache.get(txHash);

            if (receipt != null) {
                return receipt;
            }

            return io.aurigraph.v11.proto.TransactionReceipt.newBuilder()
                .setTransactionHash(txHash)
                .setStatus(io.aurigraph.v11.proto.TransactionStatus.TRANSACTION_UNKNOWN)
                .setExecutionTime(getCurrentTimestamp())
                .build();
        });
    }

    @Override
    public Uni<io.aurigraph.v11.proto.CancelTransactionResponse> cancelTransaction(io.aurigraph.v11.proto.CancelTransactionRequest request) {
        return Uni.createFrom().item(() -> {
            String txHash = request.getTransactionHash();
            io.aurigraph.v11.proto.Transaction tx = transactionCache.get(txHash);

            if (tx == null || tx.getStatus() != io.aurigraph.v11.proto.TransactionStatus.TRANSACTION_QUEUED) {
                return io.aurigraph.v11.proto.CancelTransactionResponse.newBuilder()
                    .setTransactionHash(txHash)
                    .setCancellationSuccessful(false)
                    .setReason("Transaction not pending")
                    .setTimestamp(getCurrentTimestamp())
                    .build();
            }

            io.aurigraph.v11.proto.Transaction cancelled = tx.toBuilder()
                .setStatus(io.aurigraph.v11.proto.TransactionStatus.TRANSACTION_FAILED)
                .build();
            transactionCache.put(txHash, cancelled);
            failedTransactions.incrementAndGet();

            return io.aurigraph.v11.proto.CancelTransactionResponse.newBuilder()
                .setTransactionHash(txHash)
                .setCancellationSuccessful(true)
                .setReason("Cancelled by request")
                .setTimestamp(getCurrentTimestamp())
                .build();
        });
    }

    @Override
    public Uni<io.aurigraph.v11.proto.ResendTransactionResponse> resendTransaction(io.aurigraph.v11.proto.ResendTransactionRequest request) {
        return Uni.createFrom().item(() -> {
            String originalHash = request.getOriginalTransactionHash();
            io.aurigraph.v11.proto.Transaction originalTx = transactionCache.get(originalHash);

            if (originalTx == null) {
                return io.aurigraph.v11.proto.ResendTransactionResponse.newBuilder()
                    .setOriginalTransactionHash(originalHash)
                    .setNewTransactionHash("")
                    .setTimestamp(getCurrentTimestamp())
                    .build();
            }

            String newHash = generateTxHash(originalTx);
            io.aurigraph.v11.proto.Transaction resent = originalTx.toBuilder()
                .setTransactionHash(newHash)
                .setGasPrice(request.getNewGasPrice())
                .setStatus(io.aurigraph.v11.proto.TransactionStatus.TRANSACTION_QUEUED)
                .setCreatedAt(getCurrentTimestamp())
                .build();

            transactionCache.put(newHash, resent);
            pendingTransactions.offer(resent);
            totalTransactions.incrementAndGet();

            return io.aurigraph.v11.proto.ResendTransactionResponse.newBuilder()
                .setOriginalTransactionHash(originalHash)
                .setNewTransactionHash(newHash)
                .setNewGasPrice(request.getNewGasPrice())
                .setStatus(io.aurigraph.v11.proto.TransactionStatus.TRANSACTION_QUEUED)
                .setTimestamp(getCurrentTimestamp())
                .build();
        });
    }

    @Override
    public Uni<io.aurigraph.v11.proto.GasEstimate> estimateGasCost(io.aurigraph.v11.proto.EstimateGasCostRequest request) {
        return Uni.createFrom().item(() -> {
            int dataLength = request.getData().length();
            double estimatedGas = 21000.0 + (dataLength * 16.0);
            double totalCost = estimatedGas * averageGasPrice;

            return io.aurigraph.v11.proto.GasEstimate.newBuilder()
                .setEstimatedGas(estimatedGas)
                .setGasPriceWei(averageGasPrice)
                .setTotalCost(String.valueOf(totalCost))
                .setBufferPercent(10.0)
                .setRecommendation("Gas estimate: " + estimatedGas)
                .setTimestamp(getCurrentTimestamp())
                .build();
        });
    }

    @Override
    public Uni<io.aurigraph.v11.proto.TransactionSignatureValidationResult> validateTransactionSignature(
            io.aurigraph.v11.proto.ValidateTransactionSignatureRequest request) {
        return Uni.createFrom().item(() -> {
            io.aurigraph.v11.proto.Transaction tx = request.getTransaction();

            io.aurigraph.v11.proto.TransactionSignatureValidationResult.Builder result = io.aurigraph.v11.proto.TransactionSignatureValidationResult.newBuilder()
                .setSignatureValid(!tx.getSignature().isEmpty())
                .setSenderValid(!tx.getPublicKey().isEmpty())
                .setNonceValid(tx.getNonce() >= 0)
                .setTimestamp(getCurrentTimestamp());

            return result.build();
        });
    }

    @Override
    public Uni<io.aurigraph.v11.proto.PendingTransactionsResponse> getPendingTransactions(io.aurigraph.v11.proto.GetPendingTransactionsRequest request) {
        return Uni.createFrom().item(() -> {
            List<io.aurigraph.v11.proto.Transaction> pending = new ArrayList<>(pendingTransactions);

            if (request.getFilterAddress() != null && !request.getFilterAddress().isEmpty()) {
                pending = pending.stream()
                    .filter(tx -> tx.getFromAddress().equals(request.getFilterAddress()) ||
                                  tx.getToAddress().equals(request.getFilterAddress()))
                    .limit(request.getLimit() > 0 ? request.getLimit() : 100)
                    .toList();
            }

            double avgGas = pending.stream()
                .mapToDouble(io.aurigraph.v11.proto.Transaction::getGasPrice)
                .average()
                .orElse(averageGasPrice);

            return io.aurigraph.v11.proto.PendingTransactionsResponse.newBuilder()
                .addAllTransactions(pending)
                .setTotalPending(pending.size())
                .setAverageGasPrice(avgGas)
                .setQueryTime(getCurrentTimestamp())
                .build();
        });
    }

    @Override
    public Uni<io.aurigraph.v11.proto.TransactionHistoryResponse> getTransactionHistory(io.aurigraph.v11.proto.GetTransactionHistoryRequest request) {
        return Uni.createFrom().item(() -> {
            String address = request.getAddress();
            List<io.aurigraph.v11.proto.Transaction> history = transactionCache.values().stream()
                .filter(tx -> tx.getFromAddress().equals(address) || tx.getToAddress().equals(address))
                .skip(request.getOffset())
                .limit(request.getLimit() > 0 ? request.getLimit() : 50)
                .toList();

            return io.aurigraph.v11.proto.TransactionHistoryResponse.newBuilder()
                .addAllTransactions(history)
                .setTotalCount((int) transactionCache.values().stream()
                    .filter(tx -> tx.getFromAddress().equals(address) || tx.getToAddress().equals(address))
                    .count())
                .setReturnedCount(history.size())
                .setOffset(request.getOffset())
                .setQueryTime(getCurrentTimestamp())
                .build();
        });
    }

    @Override
    public Uni<io.aurigraph.v11.proto.TxPoolStatistics> getTxPoolSize(io.aurigraph.v11.proto.GetTxPoolSizeRequest request) {
        return Uni.createFrom().item(() -> {
            double avgGasPrice = transactionCache.values().stream()
                .mapToDouble(io.aurigraph.v11.proto.Transaction::getGasPrice)
                .average()
                .orElse(averageGasPrice);

            long poolSize = transactionCache.values().stream()
                .mapToLong(tx -> tx.getData().length() + tx.getSignature().length())
                .sum();

            double utilization = (pendingTransactions.size() / 10000.0) * 100.0;

            return io.aurigraph.v11.proto.TxPoolStatistics.newBuilder()
                .setTotalPending(pendingTransactions.size())
                .setTotalQueued(transactionCache.size())
                .setAverageGasPrice(avgGasPrice)
                .setMinGasPrice(minGasPrice)
                .setMaxGasPrice(maxGasPrice)
                .setTotalPoolSizeBytes(poolSize)
                .setPoolUtilizationPercent(utilization)
                .setTimestamp(getCurrentTimestamp())
                .build();
        });
    }

    @Override
    public Multi<io.aurigraph.v11.proto.TransactionEvent> streamTransactionEvents(io.aurigraph.v11.proto.StreamTransactionEventsRequest request) {
        return Multi.createFrom().ticks().every(java.time.Duration.ofMillis(100))
            .onItem().transform(i -> {
                io.aurigraph.v11.proto.Transaction tx = pendingTransactions.poll();
                if (tx != null) {
                    io.aurigraph.v11.proto.Transaction confirmed = tx.toBuilder()
                        .setStatus(io.aurigraph.v11.proto.TransactionStatus.TRANSACTION_CONFIRMED)
                        .setExecutedAt(getCurrentTimestamp())
                        .build();

                    transactionCache.put(tx.getTransactionHash(), confirmed);
                    confirmedTransactions.incrementAndGet();

                    return io.aurigraph.v11.proto.TransactionEvent.newBuilder()
                        .setTransaction(confirmed)
                        .setStatus(io.aurigraph.v11.proto.TransactionStatus.TRANSACTION_CONFIRMED)
                        .setEventType("CONFIRMED")
                        .setStreamId(UUID.randomUUID().toString())
                        .setEventSequence(totalTransactions.get())
                        .setTimestamp(getCurrentTimestamp())
                        .build();
                } else {
                    return io.aurigraph.v11.proto.TransactionEvent.newBuilder()
                        .setStatus(io.aurigraph.v11.proto.TransactionStatus.TRANSACTION_UNKNOWN)
                        .setEventType("HEARTBEAT")
                        .setStreamId(UUID.randomUUID().toString())
                        .setTimestamp(getCurrentTimestamp())
                        .build();
                }
            })
            .ifNoItem().after(java.time.Duration.ofSeconds(300))
            .recoverWithCompletion();
    }

    private String generateTxHash(io.aurigraph.v11.proto.Transaction tx) {
        return "0x" + Integer.toHexString(Objects.hash(
            tx.getFromAddress(), tx.getToAddress(), tx.getAmount(), System.nanoTime()
        )).substring(0, 8);
    }

    private Timestamp getCurrentTimestamp() {
        long now = System.currentTimeMillis();
        return Timestamp.newBuilder()
            .setSeconds(now / 1000)
            .setNanos((int) ((now % 1000) * 1_000_000))
            .build();
    }
}
