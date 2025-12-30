package io.aurigraph.v11.optimization;

import io.quarkus.logging.Log;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

/**
 * Generic Object Pool - Sprint 15 Phase 2 Memory Optimization
 * Reduces GC pressure by reusing objects instead of allocating new ones
 *
 * Expected Performance:
 * - Memory allocations: -37% (top 3 hotspots)
 * - GC pause time: -20ms (less young gen pressure)
 * - TPS Improvement: +240K (8% of 3.0M baseline)
 *
 * @author BDA-Performance
 * @version 1.0
 * @since Sprint 15
 */
public class ObjectPool<T extends Poolable> {

    private final BlockingQueue<T> pool;
    private final Supplier<T> factory;
    private final int maxSize;
    private final long acquireTimeoutMs;
    private final String poolName;

    // Metrics
    private final AtomicLong totalAcquires = new AtomicLong(0);
    private final AtomicLong totalReleases = new AtomicLong(0);
    private final AtomicLong totalTimeouts = new AtomicLong(0);
    private final AtomicLong totalCreations = new AtomicLong(0);

    /**
     * Create object pool
     *
     * @param poolName Pool name for logging
     * @param factory Object factory function
     * @param initialSize Initial pool size (pre-allocated)
     * @param maxSize Maximum pool size
     * @param acquireTimeoutMs Timeout for acquiring object (fail-fast)
     */
    public ObjectPool(String poolName, Supplier<T> factory, int initialSize, int maxSize, long acquireTimeoutMs) {
        this.poolName = poolName;
        this.factory = factory;
        this.maxSize = maxSize;
        this.acquireTimeoutMs = acquireTimeoutMs;
        this.pool = new ArrayBlockingQueue<>(maxSize);

        // Pre-allocate objects
        for (int i = 0; i < initialSize; i++) {
            T object = factory.get();
            pool.offer(object);
            totalCreations.incrementAndGet();
        }

        Log.infof("Object pool '%s' initialized: initialSize=%d, maxSize=%d",
                 poolName, initialSize, maxSize);
    }

    /**
     * Acquire object from pool (blocks if pool empty, max acquireTimeoutMs)
     *
     * @return Pooled object or newly created if pool exhausted
     */
    public T acquire() {
        totalAcquires.incrementAndGet();

        try {
            T object = pool.poll(acquireTimeoutMs, TimeUnit.MILLISECONDS);

            if (object == null) {
                // Pool exhausted, create new object
                Log.debugf("Pool '%s' exhausted, creating new object", poolName);
                totalTimeouts.incrementAndGet();
                object = factory.get();
                totalCreations.incrementAndGet();
            }

            return object;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            Log.errorf("Pool '%s' acquire interrupted", poolName);
            // Fallback: create new object
            totalTimeouts.incrementAndGet();
            T object = factory.get();
            totalCreations.incrementAndGet();
            return object;
        }
    }

    /**
     * Release object back to pool
     *
     * @param object Object to release (must be reset before releasing)
     */
    public void release(T object) {
        if (object == null) {
            return;
        }

        totalReleases.incrementAndGet();

        // Reset object state
        try {
            object.reset();
        } catch (Exception e) {
            Log.errorf(e, "Failed to reset object in pool '%s', discarding", poolName);
            return;
        }

        // Return to pool (drop if pool full)
        boolean offered = pool.offer(object);
        if (!offered) {
            Log.debugf("Pool '%s' full, dropping object", poolName);
        }
    }

    /**
     * Get pool metrics
     */
    public PoolMetrics getMetrics() {
        return new PoolMetrics(
            poolName,
            pool.size(),
            maxSize,
            totalAcquires.get(),
            totalReleases.get(),
            totalTimeouts.get(),
            totalCreations.get()
        );
    }

    /**
     * Get current pool size
     */
    public int size() {
        return pool.size();
    }

    /**
     * Clear pool (for testing/shutdown)
     */
    public void clear() {
        pool.clear();
        Log.infof("Pool '%s' cleared", poolName);
    }

    public record PoolMetrics(
        String poolName,
        int currentSize,
        int maxSize,
        long totalAcquires,
        long totalReleases,
        long totalTimeouts,
        long totalCreations
    ) {
        public double utilizationPercent() {
            return maxSize > 0 ? (1.0 - (double) currentSize / maxSize) * 100 : 0.0;
        }

        public double hitRate() {
            return totalAcquires > 0 ?
                (double) (totalAcquires - totalTimeouts) / totalAcquires : 0.0;
        }

        public double missRate() {
            return 1.0 - hitRate();
        }
    }
}
