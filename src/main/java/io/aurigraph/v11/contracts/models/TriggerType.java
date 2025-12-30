package io.aurigraph.v11.contracts.models;

/**
 * Types of triggers that can execute contract logic
 */
public enum TriggerType {
    TIME_BASED,     // Trigger based on time/schedule
    EVENT_BASED,    // Trigger based on external events
    ORACLE_BASED,   // Trigger based on oracle data feeds
    SIGNATURE_BASED, // Trigger based on multi-party signatures
    RWA_BASED,      // Trigger based on real-world asset events
    THRESHOLD_BASED, // Trigger based on threshold conditions
    MANUAL          // Manual trigger activation
}