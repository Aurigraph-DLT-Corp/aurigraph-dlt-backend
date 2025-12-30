package io.aurigraph.v11.performance;

import io.aurigraph.v11.proto.*;
import io.aurigraph.v11.grpc.TransactionServiceGrpcImpl;
import io.aurigraph.v11.service.TransactionService;
import io.aurigraph.v11.service.TransactionServiceImpl;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * PHASE 4D: Comprehensive Performance Testing Suite
 *
 * Performance benchmarks for V11 gRPC platform:
 * - Baseline TPS measurement (current: 776K)
 * - Target TPS validation (goal: 2M+)
 * - gRPC vs REST comparison
 * - Memory footprint analysis
 * - Latency distribution profiling
 * - Sustained load testing (24-hour capability)
 *
 * @author Performance Testing Agent (PTA)
 * @since PHASE 4D - Performance & Load Validation
 */
public class PerformanceTestSuite {

    private TransactionServiceGrpcImpl grpcService;
    private TransactionService transactionService;
    private PerformanceMetrics metrics;

    @BeforeEach
    void setUp() {
        transactionService = new TransactionServiceImpl();
        grpcService = new TransactionServiceGrpcImpl();

        try {
            var field = TransactionServiceGrpcImpl.class.getDeclaredField("transactionService");
            field.setAccessible(true);
            field.set(grpcService, transactionService);
        } catch (Exception e) {
            throw new RuntimeException("Failed to inject TransactionService", e);
        }

        metrics = new PerformanceMetrics();
    }

    /**
     * BASELINE PERFORMANCE: Measure current TPS without optimization
     * Target: 776K TPS (production baseline)
     */
    @Test
    @DisplayName("Baseline TPS: Measure current transaction throughput")
    @Timeout(120)
    void testBaselineTPS() throws Exception {
        final int totalTransactions = 100_000;
        final int threadCount = 8;
        final int txPerThread = totalTransactions / threadCount;

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicLong successCount = new AtomicLong(0);
        long startTime = System.nanoTime();

        for (int t = 0; t < threadCount; t++) {
            executor.submit(() -> {
                try {
                    for (int i = 0; i < txPerThread; i++) {
                        Transaction tx = createTransaction("0xAlice", "0xBob", "1000000");
                        TestStreamObserver<TransactionSubmissionResponse> observer = new TestStreamObserver<>();
                        grpcService.submitTransaction(
                            SubmitTransactionRequest.newBuilder()
                                .setTransaction(tx)
                                .setPrioritize(false)
                                .build(),
                            observer
                        );

                        if (!observer.values.isEmpty()) {
                            successCount.incrementAndGet();
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        long endTime = System.nanoTime();

        double elapsedSeconds = (endTime - startTime) / 1_000_000_000.0;
        double tps = successCount.get() / elapsedSeconds;

        metrics.recordBaseline("Single-thread gRPC", tps, elapsedSeconds);

        System.out.println("\n╔═══════════════════════════════════════╗");
        System.out.println("║     BASELINE PERFORMANCE RESULTS     ║");
        System.out.println("╠═══════════════════════════════════════╣");
        System.out.println(String.format("║ Transactions:     %,d         ║", successCount.get()));
        System.out.println(String.format("║ Duration:         %.2f seconds     ║", elapsedSeconds));
        System.out.println(String.format("║ Measured TPS:     %.0f K        ║", tps / 1000));
        System.out.println("║ Target TPS:       776K               ║");
        System.out.println("╚═══════════════════════════════════════╝\n");
    }

    /**
     * SUSTAINED LOAD TEST: Measure TPS over extended period
     * Duration: 5 minutes (production environment would be 24 hours)
     */
    @Test
    @DisplayName("Sustained Load Test: 5-minute performance validation")
    @Timeout(360)
    void testSustainedLoad() throws Exception {
        final long testDurationSeconds = 5 * 60; // 5 minutes for quick validation
        final int threadCount = 16;
        final AtomicLong totalProcessed = new AtomicLong(0);
        final AtomicLong totalLatency = new AtomicLong(0);

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        long startTime = System.currentTimeMillis();

        List<Future<?>> futures = new ArrayList<>();
        for (int t = 0; t < threadCount; t++) {
            futures.add(executor.submit(() -> {
                while (System.currentTimeMillis() - startTime < testDurationSeconds * 1000) {
                    long txStart = System.nanoTime();

                    Transaction tx = createTransaction("0x" + UUID.randomUUID(), "0xBob", "1000");
                    TestStreamObserver<TransactionSubmissionResponse> observer = new TestStreamObserver<>();
                    grpcService.submitTransaction(
                        SubmitTransactionRequest.newBuilder()
                            .setTransaction(tx)
                            .setPrioritize(false)
                            .build(),
                        observer
                    );

                    long txEnd = System.nanoTime();
                    if (!observer.values.isEmpty()) {
                        totalProcessed.incrementAndGet();
                        totalLatency.addAndGet(txEnd - txStart);
                    }
                }
            }));
        }

        futures.forEach(f -> {
            try { f.get(); } catch (Exception e) { /* ignore */ }
        });

        long totalTime = System.currentTimeMillis() - startTime;
        double tps = (totalProcessed.get() * 1000.0) / totalTime;
        double avgLatency = totalLatency.get() / (totalProcessed.get() * 1_000_000.0); // ms

        metrics.recordSustainedLoad("5-min load test", tps, avgLatency);

        System.out.println("\n╔═══════════════════════════════════════╗");
        System.out.println("║   SUSTAINED LOAD TEST RESULTS       ║");
        System.out.println("╠═══════════════════════════════════════╣");
        System.out.println(String.format("║ Total Transactions:  %,d        ║", totalProcessed.get()));
        System.out.println(String.format("║ Duration:           %.1f seconds   ║", totalTime / 1000.0));
        System.out.println(String.format("║ Sustained TPS:      %.0f K       ║", tps / 1000));
        System.out.println(String.format("║ Avg Latency:        %.3f ms     ║", avgLatency));
        System.out.println("║ Target TPS:         2M+              ║");
        System.out.println("╚═══════════════════════════════════════╝\n");
    }

    /**
     * LATENCY PROFILING: Measure p50, p99, p99.9 latencies
     */
    @Test
    @DisplayName("Latency Distribution: Profile p50, p99, p99.9 percentiles")
    @Timeout(120)
    void testLatencyDistribution() throws Exception {
        final int totalTransactions = 50_000;
        List<Long> latencies = new CopyOnWriteArrayList<>();

        ExecutorService executor = Executors.newFixedThreadPool(16);
        CountDownLatch latch = new CountDownLatch(16);

        for (int t = 0; t < 16; t++) {
            executor.submit(() -> {
                try {
                    for (int i = 0; i < totalTransactions / 16; i++) {
                        long start = System.nanoTime();

                        Transaction tx = createTransaction("0xAlice", "0xBob", "1000");
                        TestStreamObserver<TransactionSubmissionResponse> observer = new TestStreamObserver<>();
                        grpcService.submitTransaction(
                            SubmitTransactionRequest.newBuilder()
                                .setTransaction(tx)
                                .setPrioritize(false)
                                .build(),
                            observer
                        );

                        long end = System.nanoTime();
                        latencies.add((end - start) / 1_000); // microseconds
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();

        List<Long> sorted = new ArrayList<>(latencies);
        Collections.sort(sorted);

        long p50 = sorted.get((int)(sorted.size() * 0.50));
        long p99 = sorted.get((int)(sorted.size() * 0.99));
        long p99_9 = sorted.get((int)(sorted.size() * 0.999));
        long max = sorted.get(sorted.size() - 1);
        long min = sorted.get(0);

        metrics.recordLatencies("gRPC latency distribution", p50 / 1000.0, p99 / 1000.0, p99_9 / 1000.0);

        System.out.println("\n╔════════════════════════════════════════╗");
        System.out.println("║   LATENCY DISTRIBUTION RESULTS      ║");
        System.out.println("╠════════════════════════════════════════╣");
        System.out.println(String.format("║ Total Samples:        %,d          ║", latencies.size()));
        System.out.println(String.format("║ Min Latency:          %.3f ms      ║", min / 1000.0));
        System.out.println(String.format("║ P50 (Median):         %.3f ms      ║", p50 / 1000.0));
        System.out.println(String.format("║ P99 (99th):           %.3f ms      ║", p99 / 1000.0));
        System.out.println(String.format("║ P99.9 (99.9th):       %.3f ms      ║", p99_9 / 1000.0));
        System.out.println(String.format("║ Max Latency:          %.3f ms      ║", max / 1000.0));
        System.out.println("║ Target P99:           <10ms           ║");
        System.out.println("╚════════════════════════════════════════╝\n");
    }

    /**
     * MEMORY FOOTPRINT: Measure heap usage and GC impact
     */
    @Test
    @DisplayName("Memory Footprint: Analyze heap usage and GC")
    @Timeout(120)
    void testMemoryFootprint() throws Exception {
        Runtime runtime = Runtime.getRuntime();

        // Force GC before measurement
        System.gc();
        Thread.sleep(1000);

        long memoryBefore = runtime.totalMemory() - runtime.freeMemory();

        // Process 100k transactions
        for (int i = 0; i < 100_000; i++) {
            Transaction tx = createTransaction("0xAlice", "0xBob", "1000");
            TestStreamObserver<TransactionSubmissionResponse> observer = new TestStreamObserver<>();
            grpcService.submitTransaction(
                SubmitTransactionRequest.newBuilder()
                    .setTransaction(tx)
                    .setPrioritize(false)
                    .build(),
                observer
            );
        }

        long memoryAfter = runtime.totalMemory() - runtime.freeMemory();
        long memoryUsed = memoryAfter - memoryBefore;
        long maxMemory = runtime.maxMemory();

        System.out.println("\n╔════════════════════════════════════════╗");
        System.out.println("║     MEMORY FOOTPRINT RESULTS        ║");
        System.out.println("╠════════════════════════════════════════╣");
        System.out.println(String.format("║ Memory Before:        %,d KB       ║", memoryBefore / 1024));
        System.out.println(String.format("║ Memory After:         %,d KB       ║", memoryAfter / 1024));
        System.out.println(String.format("║ Memory Used:          %,d KB       ║", memoryUsed / 1024));
        System.out.println(String.format("║ Max Heap Available:   %,d KB       ║", maxMemory / 1024));
        System.out.println(String.format("║ Memory Per TX:        %.2f bytes   ║", (double)memoryUsed / 100_000));
        System.out.println("║ Target Heap:          <256MB          ║");
        System.out.println("╚════════════════════════════════════════╝\n");
    }

    /**
     * BATCH PROCESSING EFFICIENCY: Compare batch vs single submissions
     */
    @Test
    @DisplayName("Batch Processing: Compare batch vs individual transactions")
    @Timeout(120)
    void testBatchProcessingEfficiency() throws Exception {
        final int batchSize = 100;
        final int numBatches = 1000;

        // Single transaction submissions
        long singleStart = System.nanoTime();
        for (int b = 0; b < numBatches; b++) {
            for (int i = 0; i < batchSize; i++) {
                Transaction tx = createTransaction("0xAlice", "0xBob", "1000");
                TestStreamObserver<TransactionSubmissionResponse> observer = new TestStreamObserver<>();
                grpcService.submitTransaction(
                    SubmitTransactionRequest.newBuilder()
                        .setTransaction(tx)
                        .setPrioritize(false)
                        .build(),
                    observer
                );
            }
        }
        long singleEnd = System.nanoTime();

        // Batch submission
        long batchStart = System.nanoTime();
        for (int b = 0; b < numBatches; b++) {
            BatchTransactionSubmissionRequest.Builder builder = BatchTransactionSubmissionRequest.newBuilder()
                .setBatchId("batch-" + b);

            for (int i = 0; i < batchSize; i++) {
                builder.addTransactions(createTransaction("0xAlice", "0xRecipient" + i, "1000"));
            }

            TestStreamObserver<BatchTransactionSubmissionResponse> observer = new TestStreamObserver<>();
            grpcService.batchSubmitTransactions(builder.build(), observer);
        }
        long batchEnd = System.nanoTime();

        double singleTime = (singleEnd - singleStart) / 1_000_000.0; // ms
        double batchTime = (batchEnd - batchStart) / 1_000_000.0; // ms
        double improvement = ((singleTime - batchTime) / singleTime) * 100;

        System.out.println("\n╔════════════════════════════════════════╗");
        System.out.println("║  BATCH PROCESSING EFFICIENCY         ║");
        System.out.println("╠════════════════════════════════════════╣");
        System.out.println(String.format("║ Total Transactions:   %,d          ║", numBatches * batchSize));
        System.out.println(String.format("║ Single TX Time:       %.2f ms      ║", singleTime));
        System.out.println(String.format("║ Batch TX Time:        %.2f ms      ║", batchTime));
        System.out.println(String.format("║ Improvement:          %.1f%%        ║", improvement));
        System.out.println("║ Target Improvement:   >50%            ║");
        System.out.println("╚════════════════════════════════════════╝\n");
    }

    // ══════════════════════════════════════════════════════════════════════
    // HELPER METHODS
    // ══════════════════════════════════════════════════════════════════════

    private Transaction createTransaction(String from, String to, String amount) {
        return Transaction.newBuilder()
            .setFromAddress(from)
            .setToAddress(to)
            .setAmount(amount)
            .setNonce((int) (Math.random() * 1_000_000))
            .build();
    }

    private static class TestStreamObserver<T> implements StreamObserver<T> {
        List<T> values = new ArrayList<>();
        Throwable error;
        boolean completed = false;

        @Override
        public void onNext(T value) { values.add(value); }

        @Override
        public void onError(Throwable t) { error = t; }

        @Override
        public void onCompleted() { completed = true; }
    }

    /**
     * Performance metrics aggregator
     */
    public static class PerformanceMetrics {
        private final List<TestResult> results = new CopyOnWriteArrayList<>();

        void recordBaseline(String name, double tps, double duration) {
            results.add(new TestResult(name, "Baseline", tps, duration, 0, 0));
        }

        void recordSustainedLoad(String name, double tps, double avgLatency) {
            results.add(new TestResult(name, "Sustained Load", tps, 0, avgLatency, 0));
        }

        void recordLatencies(String name, double p50, double p99, double p99_9) {
            results.add(new TestResult(name, "Latency Distribution", 0, 0, p50, p99));
        }

        public void printSummary() {
            System.out.println("\n╔════════════════════════════════════════════════════════╗");
            System.out.println("║         PHASE 4D PERFORMANCE TEST SUMMARY              ║");
            System.out.println("╚════════════════════════════════════════════════════════╝\n");

            for (TestResult r : results) {
                System.out.println(r);
            }
        }
    }

    private static class TestResult {
        String name, type;
        double tps, duration, p50, p99;

        TestResult(String name, String type, double tps, double duration, double p50, double p99) {
            this.name = name;
            this.type = type;
            this.tps = tps;
            this.duration = duration;
            this.p50 = p50;
            this.p99 = p99;
        }

        @Override
        public String toString() {
            return String.format("%-35s | %-20s | TPS: %.0fK | P99: %.3fms",
                name, type, tps / 1000, p99);
        }
    }
}
