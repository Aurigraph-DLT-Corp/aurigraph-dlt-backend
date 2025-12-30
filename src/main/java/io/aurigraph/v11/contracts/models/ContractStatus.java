package io.aurigraph.v11.contracts.models;

/**
 * Smart Contract Lifecycle Status
 *
 * Represents the various states a smart contract can be in during its lifecycle.
 *
 * @version 3.8.0 (Phase 2)
 * @author Aurigraph V11 Development Team
 */
public enum ContractStatus {
    /**
     * Contract is being drafted, not yet compiled
     */
    DRAFT,

    /**
     * Contract has been compiled successfully
     */
    COMPILED,

    /**
     * Contract has been deployed to the blockchain
     */
    DEPLOYED,

    /**
     * Contract is active and can be executed
     */
    ACTIVE,

    /**
     * Contract has been paused (temporarily inactive)
     */
    PAUSED,

    /**
     * Contract execution has completed successfully
     */
    COMPLETED,

    /**
     * Contract has been terminated/cancelled
     */
    TERMINATED,

    /**
     * Contract has failed during execution
     */
    FAILED,

    /**
     * Contract has expired (time-based expiration)
     */
    EXPIRED;

    /**
     * Check if contract is in a final state (cannot be modified)
     */
    public boolean isFinal() {
        return this == COMPLETED || this == TERMINATED || this == FAILED || this == EXPIRED;
    }

    /**
     * Check if contract is executable
     */
    public boolean isExecutable() {
        return this == ACTIVE;
    }

    /**
     * Check if contract can be deployed
     */
    public boolean canDeploy() {
        return this == DRAFT || this == COMPILED;
    }

    /**
     * Check if contract can be activated
     */
    public boolean canActivate() {
        return this == DEPLOYED;
    }

    /**
     * Check if contract can be paused
     */
    public boolean canPause() {
        return this == ACTIVE;
    }

    /**
     * Check if contract can be resumed
     */
    public boolean canResume() {
        return this == PAUSED;
    }
}
