package io.aurigraph.v11.bridge;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Atomic Swap State - Tracks HTLC atomic swap progress
 */
public class AtomicSwapState {
    public enum SwapPhase {
        INITIATED, LOCKED, SECRET_REVEALED, COMPLETED, REFUNDED, TIMEOUT
    }

    private final String swapId;
    private final String transactionId;
    private final String hashLock;
    private final String secret;
    private final long lockTime;
    private SwapPhase phase;
    private final Instant createdAt;
    private Instant updatedAt;

    public AtomicSwapState(String swapId, String transactionId, String hashLock, long lockTime) {
        this.swapId = swapId;
        this.transactionId = transactionId;
        this.hashLock = hashLock;
        this.secret = null;
        this.lockTime = lockTime;
        this.phase = SwapPhase.INITIATED;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void updatePhase(SwapPhase newPhase) {
        this.phase = newPhase;
        this.updatedAt = Instant.now();
    }

    public boolean isExpired() {
        return Instant.now().getEpochSecond() > lockTime;
    }

    // Getters
    public String getSwapId() { return swapId; }
    public String getTransactionId() { return transactionId; }
    public String getHashLock() { return hashLock; }
    public String getSecret() { return secret; }
    public long getLockTime() { return lockTime; }
    public SwapPhase getPhase() { return phase; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
