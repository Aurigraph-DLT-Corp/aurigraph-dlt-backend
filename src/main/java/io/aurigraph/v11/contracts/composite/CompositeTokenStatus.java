package io.aurigraph.v11.contracts.composite;

/**
 * Status of composite tokens
 */
public enum CompositeTokenStatus {
    PENDING_VERIFICATION,  // Awaiting verification
    VERIFIED,             // Verified and active
    REJECTED,             // Verification rejected
    EXPIRED,              // Verification expired
    SUSPENDED,            // Temporarily suspended
    TRANSFERRED           // Ownership transferred
}