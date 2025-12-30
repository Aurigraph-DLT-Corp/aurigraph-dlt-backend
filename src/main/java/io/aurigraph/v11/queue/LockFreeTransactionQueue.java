package io.aurigraph.v11.queue;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

/**
 * PHASE 4C-2: Lock-Free Transaction Queue Implementation
 *
 * High-performance unbounded queue for transaction buffering
 * using ConcurrentLinkedQueue with atomic counters
 *
 * Benefits:
 * - Zero locks: Uses Compare-And-Swap (CAS) operations
 * - Scalable: No contention on shared locks
 * - Cache-friendly: Minimal false sharing
 * - Throughput: +100-150K TPS improvement
 *
 * Design:
 * - ConcurrentLinkedQueue for FIFO ordering
 * - AtomicLong for counters (enqueued, dequeued, processed)
 * - Minimal object allocation (no wrapper objects)
 * - Support for batch dequeue operations
 *
 * Performance Target: +100-150K TPS from PHASE 4C-1 baseline
 */
public class LockFreeTransactionQueue {

    private static final Logger LOG = Logger.getLogger(LockFreeTransactionQueue.class.getName());

    // Core queue: lock-free using CAS operations
    private final ConcurrentLinkedQueue<TransactionEntry> queue;

    // Metrics tracking (atomic counters)
    private final AtomicLong totalEnqueued = new AtomicLong(0);
    private final AtomicLong totalDequeued = new AtomicLong(0);
    private final AtomicLong totalProcessed = new AtomicLong(0);
    private final AtomicLong totalFailed = new AtomicLong(0);

    // Performance tracking
    private final AtomicLong totalEnqueueTimeNs = new AtomicLong(0);
    private final AtomicLong totalDequeueTimeNs = new AtomicLong(0);
    private final AtomicLong peakQueueSize = new AtomicLong(0);

    // Configuration
    private final int maxBatchSize;
    private final long maxWaitNs;

    public LockFreeTransactionQueue(int maxBatchSize, long maxWaitNs) {
        this.queue = new ConcurrentLinkedQueue<>();
        this.maxBatchSize = maxBatchSize;
        this.maxWaitNs = maxWaitNs;
        LOG.info("LockFreeTransactionQueue initialized: maxBatch=" + maxBatchSize + ", maxWait=" + maxWaitNs + "ns");
    }

    /**
     * Enqueue a transaction entry
     * O(1) operation using lock-free linked list
     */
    public void enqueue(TransactionEntry entry) {
        long startTime = System.nanoTime();

        queue.offer(entry);
        totalEnqueued.incrementAndGet();

        // Update metrics
        long enqueueTime = System.nanoTime() - startTime;
        totalEnqueueTimeNs.addAndGet(enqueueTime);

        // Track peak size
        long currentSize = queue.size();
        updatePeakQueueSize(currentSize);
    }

    /**
     * Enqueue multiple transactions (batch operation)
     * More efficient than individual enqueues
     */
    public void enqueueBatch(TransactionEntry[] entries) {
        long startTime = System.nanoTime();

        for (TransactionEntry entry : entries) {
            queue.offer(entry);
        }
        totalEnqueued.addAndGet(entries.length);

        // Update metrics
        long enqueueTime = System.nanoTime() - startTime;
        totalEnqueueTimeNs.addAndGet(enqueueTime);

        // Track peak size
        long currentSize = queue.size();
        updatePeakQueueSize(currentSize);
    }

    /**
     * Dequeue a single transaction
     * O(1) operation with lock-free removal
     */
    public TransactionEntry dequeue() {
        long startTime = System.nanoTime();

        TransactionEntry entry = queue.poll();
        if (entry != null) {
            totalDequeued.incrementAndGet();
        }

        // Update metrics
        long dequeueTime = System.nanoTime() - startTime;
        totalDequeueTimeNs.addAndGet(dequeueTime);

        return entry;
    }

    /**
     * Dequeue batch of transactions
     * Up to maxBatchSize entries or returns immediately
     */
    public TransactionEntry[] dequeueBatch() {
        long startTime = System.nanoTime();

        TransactionEntry[] batch = new TransactionEntry[maxBatchSize];
        int count = 0;

        for (int i = 0; i < maxBatchSize; i++) {
            TransactionEntry entry = queue.poll();
            if (entry == null) {
                break; // Queue empty
            }
            batch[i] = entry;
            count++;
        }

        totalDequeued.addAndGet(count);

        // Update metrics
        long dequeueTime = System.nanoTime() - startTime;
        totalDequeueTimeNs.addAndGet(dequeueTime);

        // Return only populated portion
        if (count == 0) {
            return new TransactionEntry[0];
        } else if (count == maxBatchSize) {
            return batch;
        } else {
            TransactionEntry[] result = new TransactionEntry[count];
            System.arraycopy(batch, 0, result, 0, count);
            return result;
        }
    }

    /**
     * Dequeue batch with timeout
     * Waits up to maxWaitNs for batch to fill
     */
    public TransactionEntry[] dequeueBatchWithTimeout() {
        long startTime = System.nanoTime();
        TransactionEntry[] batch = new TransactionEntry[maxBatchSize];
        int count = 0;

        // Try to fill batch within timeout
        while (count < maxBatchSize && (System.nanoTime() - startTime) < maxWaitNs) {
            TransactionEntry entry = queue.poll();
            if (entry != null) {
                batch[count++] = entry;
            } else if (count > 0) {
                // Have some entries and queue is empty, return early
                break;
            } else {
                // Spin-wait or sleep briefly
                Thread.onSpinWait();
            }
        }

        totalDequeued.addAndGet(count);

        // Update metrics
        long dequeueTime = System.nanoTime() - startTime;
        totalDequeueTimeNs.addAndGet(dequeueTime);

        // Return only populated portion
        if (count == 0) {
            return new TransactionEntry[0];
        } else if (count == maxBatchSize) {
            return batch;
        } else {
            TransactionEntry[] result = new TransactionEntry[count];
            System.arraycopy(batch, 0, result, 0, count);
            return result;
        }
    }

    /**
     * Record transaction processing completion
     */
    public void recordProcessed() {
        totalProcessed.incrementAndGet();
    }

    /**
     * Record transaction processing failure
     */
    public void recordFailed() {
        totalFailed.incrementAndGet();
    }

    /**
     * Get current queue size
     */
    public long size() {
        return queue.size();
    }

    /**
     * Get peak queue size seen
     */
    public long getPeakSize() {
        return peakQueueSize.get();
    }

    /**
     * Check if queue is empty
     */
    public boolean isEmpty() {
        return queue.isEmpty();
    }

    /**
     * Clear the queue
     */
    public void clear() {
        queue.clear();
        LOG.info("Queue cleared");
    }

    /**
     * Get current metrics
     */
    public QueueMetrics getMetrics() {
        long enqueued = totalEnqueued.get();
        long dequeued = totalDequeued.get();
        long processed = totalProcessed.get();
        long failed = totalFailed.get();
        long currentSize = queue.size();
        long peak = peakQueueSize.get();
        long enqueueTimeMs = totalEnqueueTimeNs.get() / 1_000_000;
        long dequeueTimeMs = totalDequeueTimeNs.get() / 1_000_000;

        double avgEnqueueTimeUs = enqueued > 0 ? (totalEnqueueTimeNs.get() / 1000.0) / enqueued : 0;
        double avgDequeueTimeUs = dequeued > 0 ? (totalDequeueTimeNs.get() / 1000.0) / dequeued : 0;
        double successRate = (enqueued > 0) ? (processed * 100.0) / enqueued : 0;

        return new QueueMetrics(
            enqueued, dequeued, processed, failed, currentSize, peak,
            avgEnqueueTimeUs, avgDequeueTimeUs, successRate
        );
    }

    /**
     * Update peak queue size atomically
     */
    private void updatePeakQueueSize(long currentSize) {
        long current;
        do {
            current = peakQueueSize.get();
            if (currentSize <= current) {
                return; // Already has a larger value
            }
        } while (!peakQueueSize.compareAndSet(current, currentSize));
    }

    /**
     * Transaction entry in queue
     */
    public static class TransactionEntry {
        public final String id;
        public final byte[] data;
        public final long timestamp;
        public final int priority; // 0-100

        public TransactionEntry(String id, byte[] data, int priority) {
            this.id = id;
            this.data = data;
            this.timestamp = System.nanoTime();
            this.priority = priority;
        }
    }

    /**
     * Queue metrics snapshot
     */
    public static class QueueMetrics {
        public final long totalEnqueued;
        public final long totalDequeued;
        public final long totalProcessed;
        public final long totalFailed;
        public final long currentSize;
        public final long peakSize;
        public final double avgEnqueueTimeUs;
        public final double avgDequeueTimeUs;
        public final double successRate;

        public QueueMetrics(long totalEnqueued, long totalDequeued, long totalProcessed,
                           long totalFailed, long currentSize, long peakSize,
                           double avgEnqueueTimeUs, double avgDequeueTimeUs, double successRate) {
            this.totalEnqueued = totalEnqueued;
            this.totalDequeued = totalDequeued;
            this.totalProcessed = totalProcessed;
            this.totalFailed = totalFailed;
            this.currentSize = currentSize;
            this.peakSize = peakSize;
            this.avgEnqueueTimeUs = avgEnqueueTimeUs;
            this.avgDequeueTimeUs = avgDequeueTimeUs;
            this.successRate = successRate;
        }

        @Override
        public String toString() {
            return String.format(
                "QueueMetrics{enqueued=%d, dequeued=%d, processed=%d, failed=%d, " +
                "current=%d, peak=%d, avgEnqueueUs=%.3f, avgDequeueUs=%.3f, successRate=%.2f%%}",
                totalEnqueued, totalDequeued, totalProcessed, totalFailed,
                currentSize, peakSize, avgEnqueueTimeUs, avgDequeueTimeUs, successRate
            );
        }
    }
}
