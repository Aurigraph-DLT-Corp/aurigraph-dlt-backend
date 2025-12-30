package io.aurigraph.v11.token.secondary;

/**
 * Secondary Token Version Status Enum
 *
 * Represents the lifecycle states of a secondary token version.
 * Version states progress through a defined workflow from creation to archival.
 *
 * State Transitions:
 * - CREATED → PENDING_VVB, ACTIVE, REJECTED (immediate approval paths)
 * - PENDING_VVB → ACTIVE, REJECTED, EXPIRED (approval workflow)
 * - ACTIVE → REPLACED, ARCHIVED, EXPIRED (active phase)
 * - REPLACED → ARCHIVED (superseded)
 * - REJECTED → ARCHIVED (failed approval)
 * - EXPIRED → ARCHIVED (timeout)
 * - ARCHIVED → (terminal state)
 *
 * @version 12.0.0
 * @since December 23, 2025
 */
public enum SecondaryTokenVersionStatus {
    /**
     * Initial state - version created but not yet active.
     * Can immediately transition to ACTIVE, PENDING_VVB, or REJECTED.
     * Timeout: 30 days
     */
    CREATED,

    /**
     * Awaiting Virtual Validator Board approval.
     * Version is under review for VVB certification.
     * Can transition to ACTIVE, REJECTED, or EXPIRED.
     * Timeout: 7 days
     */
    PENDING_VVB,

    /**
     * Currently active and in use.
     * This is the operational state where the version is live.
     * Can transition to REPLACED, ARCHIVED, or EXPIRED.
     * Timeout: 365 days
     */
    ACTIVE,

    /**
     * Superseded by a newer version.
     * Previous active version when a new version activates.
     * Can transition to ARCHIVED.
     * Timeout: 365 days (retention period)
     */
    REPLACED,

    /**
     * VVB rejected the version.
     * Version cannot be activated after rejection.
     * Can transition to ARCHIVED.
     * Timeout: 90 days (retention period)
     */
    REJECTED,

    /**
     * Timeout reached without completion.
     * Version exceeded allowed time in non-terminal state.
     * Can transition to ARCHIVED.
     * Timeout: Calculated from prior state's timeout
     */
    EXPIRED,

    /**
     * Old version retained for history/audit.
     * Terminal state - no further transitions.
     * Retained indefinitely for compliance and audit trails.
     */
    ARCHIVED
}
