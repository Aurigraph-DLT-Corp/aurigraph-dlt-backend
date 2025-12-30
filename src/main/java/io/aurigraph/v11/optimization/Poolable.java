package io.aurigraph.v11.optimization;

/**
 * Interface for poolable objects (must implement reset method)
 *
 * Objects implementing this interface can be reused by ObjectPool,
 * reducing GC pressure and improving performance.
 *
 * @author BDA-Performance
 * @version 1.0
 * @since Sprint 15
 */
public interface Poolable {
    /**
     * Reset object state for reuse
     * MUST clear all fields to prevent data leakage
     *
     * Implementation guidelines:
     * 1. Set all reference fields to null
     * 2. Reset primitive fields to default values
     * 3. Clear all collections
     * 4. Reset any state flags
     *
     * Example:
     * <pre>
     * public void reset() {
     *     this.id = null;
     *     this.amount = 0;
     *     this.timestamp = 0L;
     *     this.data.clear();
     *     this.processed = false;
     * }
     * </pre>
     */
    void reset();
}
