package io.aurigraph.v11.grpc;

import io.aurigraph.v11.proto.*;
import io.aurigraph.v11.service.TransactionService;
import io.aurigraph.v11.service.TransactionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PHASE 4C-4C: Transaction Service gRPC Implementation Integration Tests
 *
 * Comprehensive testing for gRPC layer integration covering:
 * - Single transaction submission and retrieval
 * - Batch transaction processing
 * - Transaction status lifecycle management
 * - Gas estimation and pricing
 * - Transaction cancellation and resending
 * - Concurrent transaction processing
 * - Performance benchmarking
 * - Error handling and edge cases
 *
 * Target: Validate gRPC performance improvements (10x faster than REST)
 * and lock-free queue integration with +250-300K TPS improvement
 */
public class TransactionServiceGrpcImplTest {

    private TransactionServiceGrpcImpl grpcService;
    private TransactionService transactionService;

    @BeforeEach
    void setUp() {
        // Initialize transaction service
        transactionService = new TransactionServiceImpl();

        // Initialize gRPC service with injected transaction service
        grpcService = new TransactionServiceGrpcImpl();
        // Use reflection to inject service (since @Inject won't work in unit test)
        try {
            var field = TransactionServiceGrpcImpl.class.getDeclaredField("transactionService");
            field.setAccessible(true);
            field.set(grpcService, transactionService);
        } catch (Exception e) {
            fail("Failed to inject TransactionService: " + e.getMessage());
        }
    }

    // ════════════════════════════════════════════════════════════════
    // SINGLE TRANSACTION TESTS (5 tests)
    // ════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Should submit single transaction via gRPC")
    void testSubmitSingleTransaction() throws Exception {
        // Given
        Transaction.Builder txBuilder = Transaction.newBuilder()
                .setFromAddress("0xAlice")
                .setToAddress("0xBob")
                .setAmount("1000000")
                .setNonce(1);

        SubmitTransactionRequest request = SubmitTransactionRequest.newBuilder()
                .setTransaction(txBuilder.build())
                .setPrioritize(false)
                .build();

        // When/Then - Using mock StreamObserver
        TestStreamObserver<TransactionSubmissionResponse> observer = new TestStreamObserver<>();
        grpcService.submitTransaction(request, observer);

        // Verify
        assertFalse(observer.values.isEmpty(), "Response should be returned");
        TransactionSubmissionResponse response = observer.values.get(0);
        assertNotNull(response.getTransactionHash());
        assertEquals("Transaction submitted successfully", response.getMessage());
    }

    @Test
    @DisplayName("Should submit prioritized transaction via gRPC")
    void testSubmitPrioritizedTransaction() throws Exception {
        // Given
        Transaction tx = Transaction.newBuilder()
                .setFromAddress("0xAlice")
                .setToAddress("0xBob")
                .setAmount("5000000")
                .setNonce(1)
                .build();

        SubmitTransactionRequest request = SubmitTransactionRequest.newBuilder()
                .setTransaction(tx)
                .setPrioritize(true)  // High priority
                .build();

        // When/Then
        TestStreamObserver<TransactionSubmissionResponse> observer = new TestStreamObserver<>();
        grpcService.submitTransaction(request, observer);

        // Verify
        assertTrue(observer.completed, "gRPC call should complete successfully");
        assertFalse(observer.values.isEmpty(), "Response should be returned");
    }

    @Test
    @DisplayName("Should get transaction status after submission")
    void testGetTransactionStatus() throws Exception {
        // Given - Submit a transaction first
        Transaction tx = createTransaction("0xAlice", "0xBob", "1000000");
        String txHash = transactionService.submitTransaction(tx, false);

        // When - Get status via gRPC
        GetTransactionStatusRequest request = GetTransactionStatusRequest.newBuilder()
                .setTransactionHash(txHash)
                .build();

        TestStreamObserver<TransactionStatusResponse> observer = new TestStreamObserver<>();
        grpcService.getTransactionStatus(request, observer);

        // Then
        assertTrue(observer.completed, "gRPC call should complete");
        assertFalse(observer.values.isEmpty(), "Status response should be returned");
    }

    @Test
    @DisplayName("Should handle transaction not found gracefully")
    void testGetStatusForNonExistentTransaction() throws Exception {
        // Given
        GetTransactionStatusRequest request = GetTransactionStatusRequest.newBuilder()
                .setTransactionHash("non-existent-hash")
                .build();

        // When/Then
        TestStreamObserver<TransactionStatusResponse> observer = new TestStreamObserver<>();
        grpcService.getTransactionStatus(request, observer);

        // Should complete with error
        assertTrue(observer.error != null || observer.completed,
                "Should either error or complete");
    }

    @Test
    @DisplayName("Should estimate gas correctly")
    void testEstimateGasCost() throws Exception {
        // Given
        EstimateGasCostRequest request = EstimateGasCostRequest.newBuilder()
                .setFromAddress("0xAlice")
                .setToAddress("0xBob")
                .setData("contract data payload")
                .build();

        // When/Then
        TestStreamObserver<GasEstimate> observer = new TestStreamObserver<>();
        grpcService.estimateGasCost(request, observer);

        // Verify
        assertTrue(observer.completed, "Gas estimation should complete");
        assertFalse(observer.values.isEmpty(), "Gas estimate should be returned");
        GasEstimate estimate = observer.values.get(0);
        assertTrue(estimate.getEstimatedGas() > 0, "Estimated gas should be positive");
    }

    // ════════════════════════════════════════════════════════════════
    // BATCH OPERATION TESTS (3 tests)
    // ════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Should submit batch of transactions via gRPC")
    void testBatchSubmitTransactions() throws Exception {
        // Given - Create batch of 5 transactions
        BatchTransactionSubmissionRequest.Builder requestBuilder =
                BatchTransactionSubmissionRequest.newBuilder()
                .setBatchId("batch-001");

        for (int i = 0; i < 5; i++) {
            Transaction tx = createTransaction(
                    "0xAlice",
                    "0xRecipient" + i,
                    String.valueOf(1000000 * (i + 1))
            );
            requestBuilder.addTransactions(tx);
        }

        // When/Then
        TestStreamObserver<BatchTransactionSubmissionResponse> observer =
                new TestStreamObserver<>();
        grpcService.batchSubmitTransactions(requestBuilder.build(), observer);

        // Verify
        assertTrue(observer.completed, "Batch submission should complete");
        assertFalse(observer.values.isEmpty(), "Batch response should be returned");
        BatchTransactionSubmissionResponse response = observer.values.get(0);
        assertEquals(5, response.getAcceptedCount(), "All 5 transactions should be accepted");
        assertEquals(0, response.getRejectedCount(), "No transactions should be rejected");
    }

    @Test
    @DisplayName("Should handle large batch transactions")
    void testLargeBatchTransaction() throws Exception {
        // Given - Create batch of 100 transactions
        int batchSize = 100;
        BatchTransactionSubmissionRequest.Builder requestBuilder =
                BatchTransactionSubmissionRequest.newBuilder()
                .setBatchId("large-batch-001");

        for (int i = 0; i < batchSize; i++) {
            Transaction tx = createTransaction(
                    "0xAlice",
                    "0xRecipient" + i,
                    String.valueOf(100000 + i)
            );
            requestBuilder.addTransactions(tx);
        }

        // When
        TestStreamObserver<BatchTransactionSubmissionResponse> observer =
                new TestStreamObserver<>();
        long startTime = System.currentTimeMillis();
        grpcService.batchSubmitTransactions(requestBuilder.build(), observer);
        long duration = System.currentTimeMillis() - startTime;

        // Then
        assertTrue(observer.completed, "Large batch should complete");
        BatchTransactionSubmissionResponse response = observer.values.get(0);
        assertEquals(batchSize, response.getAcceptedCount(),
                "All transactions should be accepted");
        assertTrue(duration < 5000, "Batch should complete within 5 seconds");
    }

    @Test
    @DisplayName("Should track batch metrics correctly")
    void testBatchMetricsTracking() throws Exception {
        // Given
        long initialRpcCalls = grpcService.getTotalRpcCalls();

        BatchTransactionSubmissionRequest request =
                BatchTransactionSubmissionRequest.newBuilder()
                .setBatchId("metrics-test")
                .addTransactions(createTransaction("0xAlice", "0xBob", "1000000"))
                .addTransactions(createTransaction("0xAlice", "0xBob", "2000000"))
                .build();

        // When
        TestStreamObserver<BatchTransactionSubmissionResponse> observer =
                new TestStreamObserver<>();
        grpcService.batchSubmitTransactions(request, observer);

        // Then
        long finalRpcCalls = grpcService.getTotalRpcCalls();
        assertEquals(initialRpcCalls + 2, finalRpcCalls,
                "RPC call count should increase by transaction count");
    }

    // ════════════════════════════════════════════════════════════════
    // TRANSACTION MANAGEMENT TESTS (4 tests)
    // ════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Should cancel pending transaction")
    void testCancelTransaction() throws Exception {
        // Given - Submit and cancel a transaction
        String txHash = transactionService.submitTransaction(
                createTransaction("0xAlice", "0xBob", "1000000"), false);

        CancelTransactionRequest request = CancelTransactionRequest.newBuilder()
                .setTransactionHash(txHash)
                .setCancellationReason("User requested cancellation")
                .build();

        // When/Then
        TestStreamObserver<CancelTransactionResponse> observer =
                new TestStreamObserver<>();
        grpcService.cancelTransaction(request, observer);

        // Verify
        assertTrue(observer.completed, "Cancellation should complete");
        assertFalse(observer.values.isEmpty(), "Response should be returned");
    }

    @Test
    @DisplayName("Should resend transaction with new gas price")
    void testResendTransaction() throws Exception {
        // Given - Submit initial transaction
        String originalHash = transactionService.submitTransaction(
                createTransaction("0xAlice", "0xBob", "1000000"), false);

        ResendTransactionRequest request = ResendTransactionRequest.newBuilder()
                .setOriginalTransactionHash(originalHash)
                .setNewGasPrice(75.0)
                .build();

        // When/Then
        TestStreamObserver<ResendTransactionResponse> observer =
                new TestStreamObserver<>();
        grpcService.resendTransaction(request, observer);

        // Verify
        assertTrue(observer.completed, "Resend should complete");
        assertFalse(observer.values.isEmpty(), "New transaction hash should be returned");
        ResendTransactionResponse response = observer.values.get(0);
        assertNotEquals(originalHash, response.getNewTransactionHash(),
                "New hash should be different");
    }

    @Test
    @DisplayName("Should get pending transactions")
    void testGetPendingTransactions() throws Exception {
        // Given - Submit multiple transactions
        for (int i = 0; i < 3; i++) {
            transactionService.submitTransaction(
                    createTransaction("0xAlice", "0xBob" + i, String.valueOf(1000000 + i)),
                    false);
        }

        // When
        GetPendingTransactionsRequest request = GetPendingTransactionsRequest.newBuilder()
                .setLimit(10)
                .setSortByFee(false)
                .build();

        TestStreamObserver<PendingTransactionsResponse> observer =
                new TestStreamObserver<>();
        grpcService.getPendingTransactions(request, observer);

        // Then
        assertTrue(observer.completed, "Query should complete");
        assertFalse(observer.values.isEmpty(), "Pending transactions should be returned");
    }

    @Test
    @DisplayName("Should get transaction history for address")
    void testGetTransactionHistory() throws Exception {
        // Given - Submit transactions from specific address
        String address = "0xAlice";
        for (int i = 0; i < 5; i++) {
            transactionService.submitTransaction(
                    createTransaction(address, "0xRecipient" + i, String.valueOf(100000 * i)),
                    false);
        }

        // When
        GetTransactionHistoryRequest request = GetTransactionHistoryRequest.newBuilder()
                .setAddress(address)
                .setLimit(10)
                .setOffset(0)
                .build();

        TestStreamObserver<TransactionHistoryResponse> observer =
                new TestStreamObserver<>();
        grpcService.getTransactionHistory(request, observer);

        // Then
        assertTrue(observer.completed, "History query should complete");
        assertFalse(observer.values.isEmpty(), "History should be returned");
    }

    // ════════════════════════════════════════════════════════════════
    // PERFORMANCE AND CONCURRENCY TESTS (3 tests)
    // ════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Should handle concurrent transactions")
    @Timeout(10)
    void testConcurrentTransactions() throws Exception {
        // Given
        int threadCount = 4;
        int txPerThread = 25;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);

        // When - Submit transactions concurrently
        for (int t = 0; t < threadCount; t++) {
            executor.submit(() -> {
                try {
                    for (int i = 0; i < txPerThread; i++) {
                        Transaction tx = createTransaction(
                                "0xThread" + Thread.currentThread().getId(),
                                "0xRecipient" + i,
                                String.valueOf(1000000 + i)
                        );
                        SubmitTransactionRequest request = SubmitTransactionRequest.newBuilder()
                                .setTransaction(tx)
                                .setPrioritize(false)
                                .build();

                        TestStreamObserver<TransactionSubmissionResponse> observer =
                                new TestStreamObserver<>();
                        grpcService.submitTransaction(request, observer);

                        if (!observer.values.isEmpty()) {
                            successCount.incrementAndGet();
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        // Then
        assertTrue(latch.await(10, TimeUnit.SECONDS), "All threads should complete");
        int expectedCount = threadCount * txPerThread;
        assertEquals(expectedCount, successCount.get(),
                "All transactions should succeed");
    }

    @Test
    @DisplayName("Should track metrics accurately under load")
    void testMetricsUnderLoad() throws Exception {
        // Given
        long initialSubmitted = 0; // Will measure from start
        int txCount = 50;

        // When - Submit many transactions
        for (int i = 0; i < txCount; i++) {
            SubmitTransactionRequest request = SubmitTransactionRequest.newBuilder()
                    .setTransaction(createTransaction("0xAlice", "0xBob" + i,
                            String.valueOf(100000 + i)))
                    .setPrioritize(i % 5 == 0) // 20% prioritized
                    .build();

            TestStreamObserver<TransactionSubmissionResponse> observer =
                    new TestStreamObserver<>();
            grpcService.submitTransaction(request, observer);
        }

        // Then
        long totalRpcCalls = grpcService.getTotalRpcCalls();
        assertTrue(totalRpcCalls >= txCount, "RPC calls should reflect submitted transactions");
    }

    @Test
    @DisplayName("Should measure gRPC performance metrics")
    void testGrpcPerformanceMetrics() throws Exception {
        // Given
        long startTime = System.currentTimeMillis();
        int iterations = 100;

        // When - Submit many transactions and measure time
        for (int i = 0; i < iterations; i++) {
            SubmitTransactionRequest request = SubmitTransactionRequest.newBuilder()
                    .setTransaction(createTransaction("0xAlice", "0xBob",
                            String.valueOf(1000000 + i)))
                    .setPrioritize(false)
                    .build();

            TestStreamObserver<TransactionSubmissionResponse> observer =
                    new TestStreamObserver<>();
            grpcService.submitTransaction(request, observer);
        }

        long duration = System.currentTimeMillis() - startTime;

        // Then - Calculate and verify performance
        double avgTimePerTx = (double) duration / iterations;
        assertTrue(avgTimePerTx < 100,
                "Average time per transaction should be < 100ms (actual: " + avgTimePerTx + "ms)");

        // Estimate TPS (transactions per second)
        double estimatedTps = 1000.0 / avgTimePerTx;
        System.out.println("Estimated TPS: " + (int)estimatedTps);
    }

    // ════════════════════════════════════════════════════════════════
    // HELPER METHODS AND TEST UTILITIES
    // ════════════════════════════════════════════════════════════════

    private Transaction createTransaction(String from, String to, String amount) {
        return Transaction.newBuilder()
                .setFromAddress(from)
                .setToAddress(to)
                .setAmount(amount)
                .setNonce((int) (Math.random() * 1000000))
                .build();
    }

    /**
     * Mock StreamObserver for testing gRPC responses
     */
    private static class TestStreamObserver<T> implements io.grpc.stub.StreamObserver<T> {
        List<T> values = new ArrayList<>();
        Throwable error;
        boolean completed = false;

        @Override
        public void onNext(T value) {
            values.add(value);
        }

        @Override
        public void onError(Throwable t) {
            error = t;
        }

        @Override
        public void onCompleted() {
            completed = true;
        }
    }
}
