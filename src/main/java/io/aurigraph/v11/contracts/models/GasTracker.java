package io.aurigraph.v11.contracts.models;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Gas tracking and metering for smart contract execution
 * Provides precise gas consumption tracking with overflow protection
 */
public class GasTracker {
    
    private final String executionId;
    private final long gasLimit;
    private final AtomicLong gasUsed;
    private final Instant startTime;
    
    public GasTracker(long gasLimit, String executionId) {
        if (gasLimit <= 0) {
            throw new IllegalArgumentException("Gas limit must be positive");
        }
        this.gasLimit = gasLimit;
        this.executionId = executionId;
        this.gasUsed = new AtomicLong(0);
        this.startTime = Instant.now();
    }
    
    /**
     * Consume gas for an operation
     * Throws OutOfGasException if limit exceeded
     */
    public void consumeGas(long amount) {
        if (amount <= 0) {
            return; // No gas consumption for free operations
        }
        
        long currentUsed = gasUsed.addAndGet(amount);
        if (currentUsed > gasLimit) {
            throw new OutOfGasException(
                String.format("Gas limit exceeded: used=%d, limit=%d, execution=%s", 
                    currentUsed, gasLimit, executionId)
            );
        }
    }
    
    /**
     * Check if enough gas is available
     */
    public boolean hasGas(long amount) {
        return gasUsed.get() + amount <= gasLimit;
    }
    
    /**
     * Get remaining gas
     */
    public long getRemainingGas() {
        return Math.max(0, gasLimit - gasUsed.get());
    }
    
    /**
     * Get gas usage percentage
     */
    public double getGasUsagePercentage() {
        return (double) gasUsed.get() / gasLimit * 100.0;
    }
    
    /**
     * Check if gas usage is critical (>90%)
     */
    public boolean isCriticalGasUsage() {
        return getGasUsagePercentage() > 90.0;
    }
    
    // Getters
    public String getExecutionId() { return executionId; }
    public long getGasLimit() { return gasLimit; }
    public long getGasUsed() { return gasUsed.get(); }
    public Instant getStartTime() { return startTime; }
    
    @Override
    public String toString() {
        return String.format("GasTracker{execution='%s', used=%d, limit=%d, remaining=%d}", 
            executionId, gasUsed.get(), gasLimit, getRemainingGas());
    }
    
    /**
     * Exception thrown when gas limit is exceeded
     */
    public static class OutOfGasException extends RuntimeException {
        public OutOfGasException(String message) {
            super(message);
        }
    }
}