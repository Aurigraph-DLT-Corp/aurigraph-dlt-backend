package io.aurigraph.v11.contracts;

/**
 * Enum representing the various states of a smart contract
 */
public enum ContractStatus {
    DRAFT,
    DEPLOYED,      // Added for BUG-002
    PENDING_APPROVAL,
    PENDING_SIGNATURES,
    ACTIVE,
    PAUSED,        // Added for BUG-002
    EXECUTED,
    COMPLETED,
    CANCELLED,
    EXPIRED,
    SUSPENDED,
    TERMINATED
}