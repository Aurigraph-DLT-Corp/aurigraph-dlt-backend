package io.aurigraph.v11.contracts.composite;

/**
 * Verification tiers for third-party verifiers
 */
public enum VerifierTier {
    TIER_1,  // Local professionals (0.05% compensation)
    TIER_2,  // Regional certified firms (0.1% compensation) 
    TIER_3,  // National certification firms (0.15% compensation)
    TIER_4   // Big 4 and institutional firms (0.2% compensation)
}