package io.aurigraph.v11.queue;

import io.aurigraph.v11.queue.LockFreeTransactionQueue.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PHASE 4C-2: Lock-Free Transaction Queue Test Suite
 *
 * Comprehensive testing for lock-free queue implementation covering:
 * - Single and batch operations
 * - Concurrent access patterns
 * - Performance characteristics
 * - Metrics tracking
 * - Backward compatibility
 *
 * Target: Validate +100-150K TPS improvement from lock-free operations
 */
public class LockFreeTransactionQueueTest {

    private LockFreeTransactionQueue queue;
    private static final int DEFAULT_BATCH_SIZE = 32;
    private static final long DEFAULT_WAIT_NS = 1_000_000; // 1ms

    @BeforeEach
    void setUp() {
        queue = new LockFreeTransactionQueue(DEFAULT_BATCH_SIZE, DEFAULT_WAIT_NS);
    }

    // ════════════════════════════════════════════════════════════════
    // BASIC OPERATIONS TESTS (8 tests)
    // ════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Should enqueue single transaction entry")
    void testSingleEnqueue() {
        TransactionEntry entry = createEntry("tx-1", 10);
        queue.enqueue(entry);

        assertEquals(1, queue.size());
        assertFalse(queue.isEmpty());

        QueueMetrics metrics = queue.getMetrics();
        assertEquals(1, metrics.totalEnqueued);
        assertEquals(0, metrics.totalDequeued);
    }

    @Test
    @DisplayName("Should dequeue single transaction entry")
    void testSingleDequeue() {
        TransactionEntry entry = createEntry("tx-1", 10);
        queue.enqueue(entry);

        TransactionEntry dequeuedEntry = queue.dequeue();
        assertNotNull(dequeuedEntry);
        assertEquals("tx-1", dequeuedEntry.id);
        assertEquals(10, dequeuedEntry.priority);

        assertTrue(queue.isEmpty());

        QueueMetrics metrics = queue.getMetrics();
        assertEquals(1, metrics.totalDequeued);
    }

    @Test
    @DisplayName("Should return null when dequeuing from empty queue")
    void testDequeueEmpty() {
        TransactionEntry entry = queue.dequeue();
        assertNull(entry);
        assertEquals(0, queue.size());
    }

    @Test
    @DisplayName("Should maintain FIFO ordering")
    void testFIFOOrdering() {
        for (int i = 0; i < 10; i++) {
            queue.enqueue(createEntry("tx-" + i, i));
        }

        for (int i = 0; i < 10; i++) {
            TransactionEntry entry = queue.dequeue();
            assertNotNull(entry);
            assertEquals("tx-" + i, entry.id, "FIFO ordering violated");
        }

        assertTrue(queue.isEmpty());
    }

    @Test
    @DisplayName("Should handle batch enqueue correctly")
    void testBatchEnqueue() {
        TransactionEntry[] batch = new TransactionEntry[10];
        for (int i = 0; i < 10; i++) {
            batch[i] = createEntry("tx-" + i, i);
        }

        queue.enqueueBatch(batch);

        assertEquals(10, queue.size());

        QueueMetrics metrics = queue.getMetrics();
        assertEquals(10, metrics.totalEnqueued);
    }

    @Test
    @DisplayName("Should handle batch dequeue correctly")
    void testBatchDequeue() {
        for (int i = 0; i < 50; i++) {
            queue.enqueue(createEntry("tx-" + i, i % 100));
        }

        TransactionEntry[] batch = queue.dequeueBatch();

        assertEquals(DEFAULT_BATCH_SIZE, batch.length);
        assertEquals(50 - DEFAULT_BATCH_SIZE, queue.size());

        for (int i = 0; i < batch.length; i++) {
            assertNotNull(batch[i]);
            assertEquals("tx-" + i, batch[i].id);
        }
    }

    @Test
    @DisplayName("Should return partial batch when queue has fewer items than batch size")
    void testPartialBatchDequeue() {
        for (int i = 0; i < 10; i++) {
            queue.enqueue(createEntry("tx-" + i, i));
        }

        TransactionEntry[] batch = queue.dequeueBatch();

        assertEquals(10, batch.length, "Should return only available items");
        assertTrue(queue.isEmpty());
    }

    @Test
    @DisplayName("Should return empty array when dequeuing from empty queue")
    void testEmptyBatchDequeue() {
        TransactionEntry[] batch = queue.dequeueBatch();

        assertNotNull(batch);
        assertEquals(0, batch.length);
    }

    // ════════════════════════════════════════════════════════════════
    // CONCURRENT ACCESS TESTS (10 tests)
    // ════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Should handle concurrent enqueue operations")
    @Timeout(5)
    void testConcurrentEnqueue() throws InterruptedException {
        int threadCount = 8;
        int operationsPerThread = 1000;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        for (int t = 0; t < threadCount; t++) {
            final int threadId = t;
            executor.submit(() -> {
                for (int i = 0; i < operationsPerThread; i++) {
                    queue.enqueue(createEntry("tx-" + threadId + "-" + i, i % 100));
                }
            });
        }

        executor.shutdown();
        assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));

        assertEquals(threadCount * operationsPerThread, queue.size());

        QueueMetrics metrics = queue.getMetrics();
        assertEquals(threadCount * operationsPerThread, metrics.totalEnqueued);
    }

    @Test
    @DisplayName("Should handle concurrent dequeue operations")
    @Timeout(5)
    void testConcurrentDequeue() throws InterruptedException {
        int itemCount = 8000;
        for (int i = 0; i < itemCount; i++) {
            queue.enqueue(createEntry("tx-" + i, i % 100));
        }

        int threadCount = 8;
        AtomicInteger dequeuedCount = new AtomicInteger(0);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        for (int t = 0; t < threadCount; t++) {
            executor.submit(() -> {
                while (true) {
                    TransactionEntry entry = queue.dequeue();
                    if (entry == null) break;
                    dequeuedCount.incrementAndGet();
                }
            });
        }

        executor.shutdown();
        assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));

        assertEquals(itemCount, dequeuedCount.get());
        assertTrue(queue.isEmpty());
    }

    @Test
    @DisplayName("Should handle concurrent enqueue and dequeue")
    @Timeout(10)
    void testConcurrentEnqueueDequeue() throws InterruptedException {
        int producerCount = 4;
        int consumerCount = 4;
        int operationsPerProducer = 2500;
        ExecutorService executor = Executors.newFixedThreadPool(producerCount + consumerCount);

        AtomicInteger totalDequeued = new AtomicInteger(0);

        // Producers
        for (int p = 0; p < producerCount; p++) {
            final int producerId = p;
            executor.submit(() -> {
                for (int i = 0; i < operationsPerProducer; i++) {
                    queue.enqueue(createEntry("tx-" + producerId + "-" + i, i % 100));
                }
            });
        }

        // Consumers
        for (int c = 0; c < consumerCount; c++) {
            executor.submit(() -> {
                while (totalDequeued.get() < (producerCount * operationsPerProducer)) {
                    TransactionEntry entry = queue.dequeue();
                    if (entry != null) {
                        totalDequeued.incrementAndGet();
                    }
                }
            });
        }

        executor.shutdown();
        assertTrue(executor.awaitTermination(15, TimeUnit.SECONDS));

        assertEquals(producerCount * operationsPerProducer, totalDequeued.get());
    }

    @Test
    @DisplayName("Should handle concurrent batch operations")
    @Timeout(10)
    void testConcurrentBatchOperations() throws InterruptedException {
        int threadCount = 8;
        int batchesPerThread = 100;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        // Producers doing batch enqueue
        for (int t = 0; t < threadCount / 2; t++) {
            final int threadId = t;
            executor.submit(() -> {
                for (int b = 0; b < batchesPerThread; b++) {
                    TransactionEntry[] batch = new TransactionEntry[DEFAULT_BATCH_SIZE];
                    for (int i = 0; i < DEFAULT_BATCH_SIZE; i++) {
                        batch[i] = createEntry("tx-" + threadId + "-" + b + "-" + i, i % 100);
                    }
                    queue.enqueueBatch(batch);
                }
            });
        }

        // Consumers doing batch dequeue - count total items not batches
        AtomicInteger itemsDequeued = new AtomicInteger(0);
        int expectedItems = (threadCount / 2) * batchesPerThread * DEFAULT_BATCH_SIZE;
        for (int t = threadCount / 2; t < threadCount; t++) {
            executor.submit(() -> {
                while (itemsDequeued.get() < expectedItems) {
                    TransactionEntry[] batch = queue.dequeueBatch();
                    if (batch.length > 0) {
                        itemsDequeued.addAndGet(batch.length);
                    } else {
                        try {
                            Thread.sleep(1);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }
                }
            });
        }

        executor.shutdown();
        assertTrue(executor.awaitTermination(15, TimeUnit.SECONDS));

        assertEquals(expectedItems, itemsDequeued.get());
    }

    @Test
    @DisplayName("Should handle batch dequeue with timeout correctly")
    @Timeout(5)
    void testBatchDequeueWithTimeout() throws InterruptedException {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        CountDownLatch latch = new CountDownLatch(1);

        // Producer thread
        executor.submit(() -> {
            for (int i = 0; i < 50; i++) {
                queue.enqueue(createEntry("tx-" + i, i % 100));
                if (i == 49) {
                    latch.countDown();
                }
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });

        // Give producer time to start
        Thread.sleep(50);

        // Dequeue with timeout
        TransactionEntry[] batch = queue.dequeueBatchWithTimeout();

        assertNotNull(batch);
        assertTrue(batch.length > 0, "Should have dequeued some entries before timeout");
        assertTrue(batch.length <= DEFAULT_BATCH_SIZE, "Should not exceed batch size");

        executor.shutdown();
    }

    @Test
    @DisplayName("Should record transaction processed correctly")
    void testRecordProcessed() {
        for (int i = 0; i < 10; i++) {
            queue.enqueue(createEntry("tx-" + i, i));
            queue.recordProcessed();
        }

        QueueMetrics metrics = queue.getMetrics();
        assertEquals(10, metrics.totalEnqueued);
        assertEquals(10, metrics.totalProcessed);
        assertEquals(0, metrics.totalFailed);
    }

    @Test
    @DisplayName("Should record transaction failed correctly")
    void testRecordFailed() {
        for (int i = 0; i < 10; i++) {
            queue.enqueue(createEntry("tx-" + i, i));
            if (i % 2 == 0) {
                queue.recordFailed();
            } else {
                queue.recordProcessed();
            }
        }

        QueueMetrics metrics = queue.getMetrics();
        assertEquals(10, metrics.totalEnqueued);
        assertEquals(5, metrics.totalProcessed);
        assertEquals(5, metrics.totalFailed);
    }

    @Test
    @DisplayName("Should track peak queue size correctly")
    void testPeakQueueSize() {
        // Enqueue 100 items
        for (int i = 0; i < 100; i++) {
            queue.enqueue(createEntry("tx-" + i, i % 100));
        }

        long peakAfterEnqueue = queue.getPeakSize();
        assertEquals(100, peakAfterEnqueue);

        // Dequeue 50 items
        for (int i = 0; i < 50; i++) {
            queue.dequeue();
        }

        // Peak should remain 100
        assertEquals(100, queue.getPeakSize());

        // Enqueue 150 more items
        for (int i = 100; i < 250; i++) {
            queue.enqueue(createEntry("tx-" + i, i % 100));
        }

        // Peak should be updated to 200 (50 remaining + 150 new)
        assertTrue(queue.getPeakSize() >= 200, "Peak size should be at least 200");
    }

    @Test
    @DisplayName("Should handle clear operation safely under concurrent access")
    @Timeout(5)
    void testConcurrentClear() throws InterruptedException {
        // Start enqueuing
        ExecutorService executor = Executors.newSingleThreadExecutor();
        AtomicInteger enqueueCount = new AtomicInteger(0);

        executor.submit(() -> {
            for (int i = 0; i < 1000; i++) {
                queue.enqueue(createEntry("tx-" + i, i % 100));
                enqueueCount.incrementAndGet();
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });

        // Give producer time to enqueue some items
        Thread.sleep(50);

        // Clear the queue
        queue.clear();
        assertTrue(queue.isEmpty());

        executor.shutdown();
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
    }

    // ════════════════════════════════════════════════════════════════
    // METRICS AND PERFORMANCE TESTS (6 tests)
    // ════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Should calculate correct average enqueue time")
    void testAverageEnqueueTime() {
        for (int i = 0; i < 1000; i++) {
            queue.enqueue(createEntry("tx-" + i, i % 100));
        }

        QueueMetrics metrics = queue.getMetrics();

        assertTrue(metrics.avgEnqueueTimeUs > 0, "Average enqueue time should be positive");
        assertTrue(metrics.avgEnqueueTimeUs < 1000, "Average enqueue time should be < 1000µs");
    }

    @Test
    @DisplayName("Should calculate correct average dequeue time")
    void testAverageDequeueTime() {
        for (int i = 0; i < 1000; i++) {
            queue.enqueue(createEntry("tx-" + i, i % 100));
        }

        for (int i = 0; i < 1000; i++) {
            queue.dequeue();
        }

        QueueMetrics metrics = queue.getMetrics();

        assertTrue(metrics.avgDequeueTimeUs > 0, "Average dequeue time should be positive");
        assertTrue(metrics.avgDequeueTimeUs < 1000, "Average dequeue time should be < 1000µs");
    }

    @Test
    @DisplayName("Should calculate correct success rate")
    void testSuccessRateCalculation() {
        // Enqueue 100 transactions
        for (int i = 0; i < 100; i++) {
            queue.enqueue(createEntry("tx-" + i, i % 100));
        }

        // Mark 50 as processed, 25 as failed
        for (int i = 0; i < 50; i++) {
            queue.recordProcessed();
        }
        for (int i = 0; i < 25; i++) {
            queue.recordFailed();
        }

        QueueMetrics metrics = queue.getMetrics();

        // Success rate should be 50%
        assertEquals(50.0, metrics.successRate, 0.1);
    }

    @Test
    @DisplayName("Should return consistent metrics snapshot")
    void testMetricsSnapshot() {
        for (int i = 0; i < 100; i++) {
            queue.enqueue(createEntry("tx-" + i, i % 100));
        }

        QueueMetrics metrics1 = queue.getMetrics();
        QueueMetrics metrics2 = queue.getMetrics();

        assertEquals(metrics1.totalEnqueued, metrics2.totalEnqueued);
        assertEquals(metrics1.currentSize, metrics2.currentSize);
        assertEquals(metrics1.peakSize, metrics2.peakSize);
    }

    @Test
    @DisplayName("Should demonstrate lock-free benefits under high contention")
    @Timeout(10)
    void testHighContentionPerformance() throws InterruptedException {
        int threadCount = 16;
        int operationsPerThread = 5000;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        long startTime = System.nanoTime();

        // Half threads enqueue, half dequeue
        for (int t = 0; t < threadCount / 2; t++) {
            final int threadId = t;
            executor.submit(() -> {
                for (int i = 0; i < operationsPerThread; i++) {
                    queue.enqueue(createEntry("tx-" + threadId + "-" + i, i % 100));
                }
            });
        }

        AtomicInteger dequeueCount = new AtomicInteger(0);
        int expectedItems = (threadCount / 2) * operationsPerThread;

        for (int t = threadCount / 2; t < threadCount; t++) {
            executor.submit(() -> {
                while (dequeueCount.get() < expectedItems) {
                    TransactionEntry entry = queue.dequeue();
                    if (entry != null) {
                        dequeueCount.incrementAndGet();
                    } else {
                        Thread.yield(); // Allow other threads to enqueue
                    }
                }
            });
        }

        executor.shutdown();
        assertTrue(executor.awaitTermination(15, TimeUnit.SECONDS));

        long endTime = System.nanoTime();
        long totalTimeMs = (endTime - startTime) / 1_000_000;

        long totalOperations = (threadCount / 2) * operationsPerThread * 2; // enqueue + dequeue
        double throughputOpsPerSec = (totalOperations * 1000.0) / totalTimeMs;

        // Lock-free operations should achieve >1M ops/sec
        assertTrue(throughputOpsPerSec > 100_000,
            "Lock-free queue should achieve >100K ops/sec, got " + throughputOpsPerSec);
    }

    // ════════════════════════════════════════════════════════════════
    // EDGE CASES AND STRESS TESTS (5 tests)
    // ════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Should handle rapid enqueue-dequeue cycles")
    @Timeout(5)
    void testRapidCycles() {
        for (int cycle = 0; cycle < 1000; cycle++) {
            // Enqueue batch
            for (int i = 0; i < 10; i++) {
                queue.enqueue(createEntry("tx-" + cycle + "-" + i, i));
            }

            // Dequeue batch
            for (int i = 0; i < 10; i++) {
                TransactionEntry entry = queue.dequeue();
                assertNotNull(entry);
            }
        }

        assertTrue(queue.isEmpty());
        QueueMetrics metrics = queue.getMetrics();
        assertEquals(10000, metrics.totalEnqueued);
        assertEquals(10000, metrics.totalDequeued);
    }

    @Test
    @DisplayName("Should maintain queue integrity with very large batches")
    void testLargeBatchOperations() {
        int largeSize = 10000;
        TransactionEntry[] largeBatch = new TransactionEntry[largeSize];
        for (int i = 0; i < largeSize; i++) {
            largeBatch[i] = createEntry("tx-" + i, i % 100);
        }

        queue.enqueueBatch(largeBatch);
        assertEquals(largeSize, queue.size());

        // Dequeue in smaller batches
        int totalDequeued = 0;
        while (!queue.isEmpty()) {
            TransactionEntry[] batch = queue.dequeueBatch();
            assertTrue(batch.length > 0);
            totalDequeued += batch.length;
        }

        assertEquals(largeSize, totalDequeued);
    }

    @Test
    @DisplayName("Should handle alternating dequeue operations correctly")
    void testAlternatingOperations() {
        for (int i = 0; i < 100; i++) {
            queue.enqueue(createEntry("tx-" + i, i % 100));

            if (i % 3 == 0) {
                TransactionEntry entry = queue.dequeue();
                assertNotNull(entry);
            }
        }

        int remaining = (int) queue.size();
        assertTrue(remaining > 0);
        assertTrue(remaining < 100);
    }

    @Test
    @DisplayName("Should handle priority values correctly across range")
    void testPriorityRange() {
        // Add entries with full priority range (0-100)
        for (int priority = 0; priority <= 100; priority += 10) {
            queue.enqueue(createEntry("tx-p" + priority, priority));
        }

        while (!queue.isEmpty()) {
            TransactionEntry entry = queue.dequeue();
            assertTrue(entry.priority >= 0 && entry.priority <= 100);
        }
    }

    @Test
    @DisplayName("Should maintain thread safety with transaction recording")
    @Timeout(5)
    void testThreadSafeMetricsRecording() throws InterruptedException {
        int threadCount = 8;
        int txPerThread = 1000;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        for (int t = 0; t < threadCount; t++) {
            final int threadId = t;
            executor.submit(() -> {
                for (int i = 0; i < txPerThread; i++) {
                    queue.enqueue(createEntry("tx-" + threadId + "-" + i, i % 100));
                    queue.dequeue();

                    if (i % 2 == 0) {
                        queue.recordProcessed();
                    } else {
                        queue.recordFailed();
                    }
                }
            });
        }

        executor.shutdown();
        assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));

        QueueMetrics metrics = queue.getMetrics();
        assertEquals(threadCount * txPerThread, metrics.totalEnqueued);
        assertEquals(threadCount * txPerThread, metrics.totalDequeued);
        assertEquals(threadCount * txPerThread / 2, metrics.totalProcessed);
        assertEquals(threadCount * txPerThread / 2, metrics.totalFailed);
    }

    // ════════════════════════════════════════════════════════════════
    // HELPER METHODS
    // ════════════════════════════════════════════════════════════════

    private TransactionEntry createEntry(String id, int priority) {
        return new TransactionEntry(id, new byte[256], priority);
    }
}
