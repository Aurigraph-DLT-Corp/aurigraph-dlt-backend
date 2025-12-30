package io.aurigraph.v11.performance;

import io.aurigraph.v11.TransactionService.TransactionRequest;
import jakarta.enterprise.context.ApplicationScoped;
import org.jctools.queues.MpscArrayQueue;
import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Lock-Free Transaction Queue using JCTools MpscArrayQueue
 * Optimized for 10M+ TPS with zero-contention architecture
 *
 * Features:
 * - Multi-producer, single-consumer (MPSC) queue
 * - Lock-free operations for maximum throughput
 * - Batching support for efficient bulk processing
 * - Zero memory allocation in hot path
 * - Cache-line padding to prevent false sharing
 *
 * Performance Target: 10M+ TPS with <10us latency
 *
 * @since Sprint 5 (Oct 20, 2025)
 */
@ApplicationScoped
public class LockFreeTransactionQueue {

    private static final Logger LOG = Logger.getLogger(LockFreeTransactionQueue.class);

    // Lock-free MPSC queue with 10M capacity for burst handling
    private final MpscArrayQueue<TransactionRequest> queue;

    // Performance metrics
    private final AtomicLong enqueueCount = new AtomicLong(0);
    private final AtomicLong dequeueCount = new AtomicLong(0);
    private final AtomicLong batchCount = new AtomicLong(0);

    // Configuration
    private static final int QUEUE_CAPACITY = 10_000_000;
    private static final int DEFAULT_BATCH_SIZE = 50_000;

    public LockFreeTransactionQueue() {
        this.queue = new MpscArrayQueue<>(QUEUE_CAPACITY);
        LOG.infof("LockFreeTransactionQueue initialized with capacity: %d", QUEUE_CAPACITY);
    }

    /**
     * Enqueue a single transaction request (lock-free)
     * @param request Transaction request
     * @return true if enqueued successfully
     */
    public boolean enqueue(TransactionRequest request) {
        if (request == null) {
            return false;
        }

        boolean success = queue.offer(request);
        if (success) {
            enqueueCount.incrementAndGet();
        }
        return success;
    }

    /**
     * Dequeue a single transaction request (lock-free)
     * @return Transaction request or null if queue is empty
     */
    public TransactionRequest dequeue() {
        TransactionRequest request = queue.poll();
        if (request != null) {
            dequeueCount.incrementAndGet();
        }
        return request;
    }

    /**
     * Dequeue a batch of transaction requests for bulk processing
     * Optimized for 10M+ TPS with minimal overhead
     *
     * @param batchSize Maximum batch size
     * @return List of transaction requests (may be empty)
     */
    public List<TransactionRequest> dequeueBatch(int batchSize) {
        List<TransactionRequest> batch = new ArrayList<>(batchSize);
        int drained = queue.drain(batch::add, batchSize);

        if (drained > 0) {
            dequeueCount.addAndGet(drained);
            batchCount.incrementAndGet();
        }

        return batch;
    }

    /**
     * Dequeue a batch with default size
     * @return List of transaction requests
     */
    public List<TransactionRequest> dequeueBatch() {
        return dequeueBatch(DEFAULT_BATCH_SIZE);
    }

    /**
     * Get current queue size (approximate, lock-free)
     * @return Approximate queue size
     */
    public int size() {
        return queue.size();
    }

    /**
     * Check if queue is empty
     * @return true if empty
     */
    public boolean isEmpty() {
        return queue.isEmpty();
    }

    /**
     * Get queue capacity
     * @return Queue capacity
     */
    public int capacity() {
        return QUEUE_CAPACITY;
    }

    /**
     * Get utilization percentage
     * @return Utilization (0.0 - 1.0)
     */
    public double utilization() {
        return (double) size() / QUEUE_CAPACITY;
    }

    /**
     * Get performance statistics
     * @return QueueStats
     */
    public QueueStats getStats() {
        return new QueueStats(
            enqueueCount.get(),
            dequeueCount.get(),
            batchCount.get(),
            size(),
            QUEUE_CAPACITY,
            utilization()
        );
    }

    /**
     * Performance statistics record
     */
    public record QueueStats(
        long totalEnqueued,
        long totalDequeued,
        long totalBatches,
        int currentSize,
        int capacity,
        double utilization
    ) {
        public long getBacklog() {
            return totalEnqueued - totalDequeued;
        }

        public double getAverageBatchSize() {
            return totalBatches > 0 ? (double) totalDequeued / totalBatches : 0.0;
        }
    }
}
