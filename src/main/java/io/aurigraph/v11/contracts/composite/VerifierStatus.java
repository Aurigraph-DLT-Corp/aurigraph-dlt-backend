package io.aurigraph.v11.contracts.composite;

/**
 * Status of verifiers in the system
 */
public enum VerifierStatus {
    PENDING_APPROVAL,  // Awaiting approval
    ACTIVE,           // Active and available
    SUSPENDED,        // Temporarily suspended
    INACTIVE,         // Inactive
    REJECTED          // Application rejected
}