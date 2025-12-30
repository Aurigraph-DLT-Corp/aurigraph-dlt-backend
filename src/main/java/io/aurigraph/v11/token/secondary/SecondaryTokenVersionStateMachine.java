package io.aurigraph.v11.token.secondary;

import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Secondary Token Version State Machine
 *
 * Manages state transitions and timeout logic for token versions.
 * Ensures versions follow valid state progression paths.
 *
 * State Diagram:
 * CREATED → {PENDING_VVB, ACTIVE, REJECTED}
 * PENDING_VVB → {ACTIVE, REJECTED, EXPIRED}
 * ACTIVE → {REPLACED, ARCHIVED, EXPIRED}
 * REPLACED → ARCHIVED
 * REJECTED → ARCHIVED
 * EXPIRED → ARCHIVED
 * ARCHIVED → (terminal)
 *
 * Timeout Rules:
 * - CREATED: 30 days (version must be activated or rejected)
 * - PENDING_VVB: 7 days (VVB must approve or reject)
 * - ACTIVE: 365 days (operational lifetime)
 * - REPLACED: 365 days (retention period)
 * - REJECTED: 90 days (retention period)
 * - EXPIRED: moved to ARCHIVED immediately
 *
 * @version 12.0.0
 * @since December 23, 2025
 */
@ApplicationScoped
@Slf4j
public class SecondaryTokenVersionStateMachine {

    // =========================================================================
    // State Transition Rules
    // =========================================================================

    /**
     * Valid state transitions map
     * Key: current state, Value: set of allowed next states
     */
    private static final Map<SecondaryTokenVersionStatus, Set<SecondaryTokenVersionStatus>>
            VALID_TRANSITIONS = Collections.unmodifiableMap(new LinkedHashMap<SecondaryTokenVersionStatus, Set<SecondaryTokenVersionStatus>>() {{
        // CREATED state: can move to pending approval, active, or rejected
        put(SecondaryTokenVersionStatus.CREATED, Set.of(
                SecondaryTokenVersionStatus.PENDING_VVB,
                SecondaryTokenVersionStatus.ACTIVE,
                SecondaryTokenVersionStatus.REJECTED,
                SecondaryTokenVersionStatus.EXPIRED
        ));

        // PENDING_VVB state: waiting for approval
        put(SecondaryTokenVersionStatus.PENDING_VVB, Set.of(
                SecondaryTokenVersionStatus.ACTIVE,
                SecondaryTokenVersionStatus.REJECTED,
                SecondaryTokenVersionStatus.EXPIRED
        ));

        // ACTIVE state: in use
        put(SecondaryTokenVersionStatus.ACTIVE, Set.of(
                SecondaryTokenVersionStatus.REPLACED,
                SecondaryTokenVersionStatus.ARCHIVED,
                SecondaryTokenVersionStatus.EXPIRED
        ));

        // REPLACED state: superseded
        put(SecondaryTokenVersionStatus.REPLACED, Set.of(
                SecondaryTokenVersionStatus.ARCHIVED
        ));

        // REJECTED state: approval failed
        put(SecondaryTokenVersionStatus.REJECTED, Set.of(
                SecondaryTokenVersionStatus.ARCHIVED
        ));

        // EXPIRED state: timeout reached
        put(SecondaryTokenVersionStatus.EXPIRED, Set.of(
                SecondaryTokenVersionStatus.ARCHIVED
        ));

        // ARCHIVED state: terminal (no transitions)
        put(SecondaryTokenVersionStatus.ARCHIVED, Collections.emptySet());
    }});

    // =========================================================================
    // Timeout Rules (in days)
    // =========================================================================

    /**
     * Maximum allowed duration in each state (days)
     * Used for automatic expiration
     */
    private static final Map<SecondaryTokenVersionStatus, Integer> TIMEOUT_DAYS =
            Collections.unmodifiableMap(new LinkedHashMap<SecondaryTokenVersionStatus, Integer>() {{
                put(SecondaryTokenVersionStatus.CREATED, 30);          // 30 days to activate or reject
                put(SecondaryTokenVersionStatus.PENDING_VVB, 7);       // 7 days for VVB approval
                put(SecondaryTokenVersionStatus.ACTIVE, 365);          // 365 days operational
                put(SecondaryTokenVersionStatus.REPLACED, 365);        // 365 days retention
                put(SecondaryTokenVersionStatus.REJECTED, 90);         // 90 days retention
                put(SecondaryTokenVersionStatus.EXPIRED, 0);           // Immediately archive
                put(SecondaryTokenVersionStatus.ARCHIVED, null);       // No timeout (terminal)
            }});

    // =========================================================================
    // Transition Validation
    // =========================================================================

    /**
     * Check if transition is valid
     *
     * @param from Current status
     * @param to Desired status
     * @return true if transition is allowed
     */
    public boolean canTransition(SecondaryTokenVersionStatus from, SecondaryTokenVersionStatus to) {
        if (from == null || to == null) {
            return false;
        }
        if (from == to) {
            return false; // No self-transitions
        }

        Set<SecondaryTokenVersionStatus> allowed = VALID_TRANSITIONS.get(from);
        return allowed != null && allowed.contains(to);
    }

    /**
     * Get all allowed next states for current status
     *
     * @param currentStatus Current status
     * @return Set of allowed next states
     */
    public Set<SecondaryTokenVersionStatus> getAllowedTransitions(SecondaryTokenVersionStatus currentStatus) {
        Set<SecondaryTokenVersionStatus> allowed = VALID_TRANSITIONS.get(currentStatus);
        return allowed != null ? new HashSet<>(allowed) : Collections.emptySet();
    }

    // =========================================================================
    // State Transition Execution
    // =========================================================================

    /**
     * Execute state transition with validation
     *
     * @param version The version to transition
     * @param newStatus The new status
     * @throws IllegalStateException if transition is invalid
     */
    public void transitionState(SecondaryTokenVersion version, SecondaryTokenVersionStatus newStatus) {
        SecondaryTokenVersionStatus oldStatus = version.getStatus();

        // Validate transition
        if (!canTransition(oldStatus, newStatus)) {
            String msg = String.format("Invalid transition: %s → %s", oldStatus, newStatus);
            log.warn("State machine violation: {}", msg);
            throw new IllegalStateException(msg);
        }

        log.info("Transitioning version {} from {} to {}", version.getId(), oldStatus, newStatus);

        // Execute exit actions for old state
        executeStateExitAction(version, oldStatus);

        // Update status
        version.setStatus(newStatus);
        version.setUpdatedAt(LocalDateTime.now());

        // Execute entry actions for new state
        executeStateEntryAction(version, newStatus);
    }

    /**
     * Execute state-specific entry actions
     *
     * @param version The version entering new state
     * @param newStatus The new status
     */
    private void executeStateEntryAction(SecondaryTokenVersion version, SecondaryTokenVersionStatus newStatus) {
        switch (newStatus) {
            case CREATED:
                // Initialize new version
                if (version.getCreatedAt() == null) {
                    version.setCreatedAt(LocalDateTime.now());
                }
                log.debug("Version {} created", version.getId());
                break;

            case PENDING_VVB:
                // Waiting for approval - no special action
                log.debug("Version {} submitted for VVB approval", version.getId());
                break;

            case ACTIVE:
                // Version is now active
                // Generate Merkle hash if not present
                if (version.getMerkleHash() == null) {
                    // Note: Hash generation done by service layer
                    log.debug("Version {} activated, awaiting Merkle hash", version.getId());
                }
                log.info("Version {} is now ACTIVE", version.getId());
                break;

            case REPLACED:
                // Record replacement timestamp
                version.setReplacedAt(LocalDateTime.now());
                log.info("Version {} has been replaced", version.getId());
                break;

            case REJECTED:
                // Rejection already has reason set
                log.info("Version {} rejected: {}", version.getId(), version.getRejectionReason());
                break;

            case EXPIRED:
                // Timeout occurred
                log.warn("Version {} expired due to timeout in state {}", version.getId(), version.getStatus());
                break;

            case ARCHIVED:
                // Record archival timestamp
                version.setArchivedAt(LocalDateTime.now());
                log.info("Version {} archived", version.getId());
                break;
        }
    }

    /**
     * Execute state-specific exit actions
     *
     * @param version The version leaving state
     * @param oldStatus The old status
     */
    private void executeStateExitAction(SecondaryTokenVersion version, SecondaryTokenVersionStatus oldStatus) {
        switch (oldStatus) {
            case ACTIVE:
                // Clear active status - will be replaced
                log.debug("Clearing ACTIVE status for version {}", version.getId());
                break;

            case PENDING_VVB:
                // VVB approval decision made
                if (version.getVvbApprovedAt() == null && !version.getStatus().equals(SecondaryTokenVersionStatus.REJECTED)) {
                    version.setVvbApprovedAt(LocalDateTime.now());
                }
                log.debug("VVB approval workflow completed for version {}", version.getId());
                break;

            default:
                // No special exit action
                break;
        }
    }

    // =========================================================================
    // Timeout Management
    // =========================================================================

    /**
     * Check if version has exceeded its timeout for current state
     *
     * @param version The version to check
     * @return true if version is expired
     */
    public boolean isExpired(SecondaryTokenVersion version) {
        if (version.getCreatedAt() == null) {
            return false; // Versions without creation time cannot expire
        }

        SecondaryTokenVersionStatus status = version.getStatus();
        Integer timeoutDays = TIMEOUT_DAYS.get(status);

        if (timeoutDays == null || timeoutDays < 0) {
            return false; // No timeout for this state
        }

        LocalDateTime expiryTime = version.getCreatedAt().plusDays(timeoutDays);
        return LocalDateTime.now().isAfter(expiryTime);
    }

    /**
     * Get days remaining before expiration
     *
     * @param version The version to check
     * @return Days remaining, or negative if expired
     */
    public long getDaysUntilExpiration(SecondaryTokenVersion version) {
        if (version.getCreatedAt() == null) {
            return -1;
        }

        SecondaryTokenVersionStatus status = version.getStatus();
        Integer timeoutDays = TIMEOUT_DAYS.get(status);

        if (timeoutDays == null || timeoutDays < 0) {
            return -1; // No expiration
        }

        LocalDateTime expiryTime = version.getCreatedAt().plusDays(timeoutDays);
        return java.time.temporal.ChronoUnit.DAYS.between(LocalDateTime.now(), expiryTime);
    }

    /**
     * Get timeout duration for status
     *
     * @param status The status to check
     * @return Number of days, or null if no timeout
     */
    public Integer getTimeoutDays(SecondaryTokenVersionStatus status) {
        return TIMEOUT_DAYS.get(status);
    }

    /**
     * Check if status has a timeout
     *
     * @param status The status to check
     * @return true if status has timeout
     */
    public boolean hasTimeout(SecondaryTokenVersionStatus status) {
        Integer timeout = TIMEOUT_DAYS.get(status);
        return timeout != null && timeout >= 0;
    }

    // =========================================================================
    // State Classification
    // =========================================================================

    /**
     * Check if status is a terminal state (no more transitions)
     *
     * @param status The status to check
     * @return true if terminal
     */
    public boolean isTerminal(SecondaryTokenVersionStatus status) {
        return status == SecondaryTokenVersionStatus.ARCHIVED;
    }

    /**
     * Check if status is transient (temporary waiting state)
     *
     * @param status The status to check
     * @return true if transient
     */
    public boolean isTransient(SecondaryTokenVersionStatus status) {
        return status == SecondaryTokenVersionStatus.CREATED ||
               status == SecondaryTokenVersionStatus.PENDING_VVB;
    }

    /**
     * Check if status is active/usable
     *
     * @param status The status to check
     * @return true if usable
     */
    public boolean isActive(SecondaryTokenVersionStatus status) {
        return status == SecondaryTokenVersionStatus.ACTIVE;
    }

    /**
     * Check if version requires approval
     *
     * @param version The version to check
     * @return true if needs VVB approval
     */
    public boolean requiresApproval(SecondaryTokenVersion version) {
        return Boolean.TRUE.equals(version.getVvbRequired()) &&
               version.getStatus() == SecondaryTokenVersionStatus.PENDING_VVB;
    }

    // =========================================================================
    // Utility Methods
    // =========================================================================

    /**
     * Get human-readable description of status
     *
     * @param status The status
     * @return Description
     */
    public String getStatusDescription(SecondaryTokenVersionStatus status) {
        return switch (status) {
            case CREATED -> "Version created, awaiting activation";
            case PENDING_VVB -> "Awaiting Virtual Validator Board approval";
            case ACTIVE -> "Version is currently active and in use";
            case REPLACED -> "Version has been superseded by newer version";
            case REJECTED -> "VVB rejected the version";
            case EXPIRED -> "Version exceeded allowed timeout";
            case ARCHIVED -> "Version archived for historical record";
        };
    }

    /**
     * Format state for logging/display
     *
     * @param status The status
     * @return Formatted string
     */
    public String formatStatus(SecondaryTokenVersionStatus status) {
        return String.format("[%s] %s", status.name(), getStatusDescription(status));
    }
}
