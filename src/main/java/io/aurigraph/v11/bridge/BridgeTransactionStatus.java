package io.aurigraph.v11.bridge;

/**
 * Bridge transaction status enumeration
 *
 * Defines the lifecycle states of a cross-chain bridge transaction.
 *
 * State transitions:
 * PENDING -> CONFIRMING -> COMPLETED (success path)
 * PENDING -> FAILED (failure path)
 * CONFIRMING -> FAILED -> REFUNDED (refund path)
 *
 * @author BDA + IBA
 * @version 11.1.0
 * @since Sprint 14
 */
public enum BridgeTransactionStatus {
    /**
     * Transaction initiated, awaiting validator approval
     */
    PENDING,

    /**
     * Transaction approved, awaiting blockchain confirmations
     */
    CONFIRMING,

    /**
     * Transaction completed successfully
     */
    COMPLETED,

    /**
     * Transaction failed (can be retried)
     */
    FAILED,

    /**
     * Transaction refunded (funds returned to sender)
     */
    REFUNDED
}
