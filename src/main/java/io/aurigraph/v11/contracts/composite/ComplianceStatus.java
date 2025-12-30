package io.aurigraph.v11.contracts.composite;

/**
 * Compliance status for regulatory compliance
 */
public enum ComplianceStatus {
    PENDING,      // Pending compliance check
    COMPLIANT,    // Fully compliant
    NON_COMPLIANT, // Non-compliant
    UNDER_REVIEW  // Under regulatory review
}