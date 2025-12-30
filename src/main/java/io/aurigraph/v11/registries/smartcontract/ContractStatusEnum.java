package io.aurigraph.v11.registries.smartcontract;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Smart Contract Lifecycle Status Enumeration
 *
 * Represents the various states a contract can transition through from creation to deployment.
 * Each status corresponds to a specific phase in the contract lifecycle.
 *
 * @version 11.5.0
 * @since 2025-11-14
 */
public enum ContractStatusEnum {
    /**
     * Initial state when contract is created but not yet compiled or deployed.
     * Used for contracts under development or awaiting review.
     */
    @JsonProperty("DRAFT")
    DRAFT("Draft", "Contract in development state, pending compilation and validation"),

    /**
     * Contract has been successfully compiled into bytecode.
     * Ready for deployment or awaiting additional verification.
     */
    @JsonProperty("DEPLOYED")
    DEPLOYED("Deployed", "Contract deployed to blockchain, not yet activated"),

    /**
     * Contract is actively running and accepting transactions.
     * Can be executed and modified by authorized callers.
     */
    @JsonProperty("ACTIVE")
    ACTIVE("Active", "Contract is active and accepting transactions"),

    /**
     * Contract has passed formal verification/audit process.
     * Indicates high confidence in contract security and functionality.
     */
    @JsonProperty("AUDITED")
    AUDITED("Audited", "Contract has passed security audit verification"),

    /**
     * Contract is no longer in use and has been superseded by a newer version.
     * Marked for archival but records retained for historical purposes.
     */
    @JsonProperty("DEPRECATED")
    DEPRECATED("Deprecated", "Contract replaced by newer version, no longer active"),

    /**
     * Contract failed to deploy or has critical issues preventing execution.
     * Requires investigation and remediation.
     */
    @JsonProperty("FAILED")
    FAILED("Failed", "Contract deployment or execution failed, requires review");

    private final String displayName;
    private final String description;

    /**
     * Constructor for ContractStatusEnum
     *
     * @param displayName Human-readable name for the status
     * @param description Detailed description of the status
     */
    ContractStatusEnum(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    /**
     * Get the human-readable display name for this status
     *
     * @return Display name
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Get the detailed description for this status
     *
     * @return Description text
     */
    public String getDescription() {
        return description;
    }

    /**
     * Check if status is valid for active contract operations
     *
     * @return true if contract can execute transactions
     */
    public boolean isActive() {
        return this == ACTIVE || this == DEPLOYED;
    }

    /**
     * Check if status represents a terminal state
     *
     * @return true if contract cannot transition to other states
     */
    public boolean isTerminal() {
        return this == DEPRECATED || this == FAILED;
    }

    /**
     * Check if status is valid for deployment
     *
     * @return true if contract is ready or has been deployed
     */
    public boolean isDeployable() {
        return this == DRAFT || this == DEPLOYED || this == ACTIVE;
    }

    /**
     * Parse status from string representation
     *
     * @param value String value to parse
     * @return ContractStatusEnum or throws IllegalArgumentException
     */
    public static ContractStatusEnum fromValue(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Status value cannot be null or empty");
        }

        try {
            return ContractStatusEnum.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                String.format("Invalid contract status: '%s'. Valid values are: DRAFT, DEPLOYED, ACTIVE, AUDITED, DEPRECATED, FAILED", value),
                e
            );
        }
    }

    /**
     * Check if transition is valid between two statuses
     *
     * @param from Source status
     * @param to Target status
     * @return true if transition is allowed
     */
    public static boolean isValidTransition(ContractStatusEnum from, ContractStatusEnum to) {
        if (from == to) {
            return false; // No self-transitions
        }

        return switch (from) {
            case DRAFT -> to == DEPLOYED || to == FAILED;
            case DEPLOYED -> to == ACTIVE || to == AUDITED || to == FAILED;
            case ACTIVE -> to == AUDITED || to == DEPRECATED || to == FAILED;
            case AUDITED -> to == DEPRECATED || to == FAILED;
            case DEPRECATED, FAILED -> false; // Terminal states
        };
    }

    @Override
    public String toString() {
        return String.format("%s (%s)", displayName, name());
    }
}
