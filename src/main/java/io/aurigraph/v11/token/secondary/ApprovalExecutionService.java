package io.aurigraph.v11.token.secondary;

import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import io.smallrye.mutiny.Uni;

import java.time.LocalDateTime;
import java.util.*;

/**
 * ApprovalExecutionService
 * Orchestrates approval execution workflow triggered by ApprovalEvent from Story 5.
 * Manages state transitions, metadata updates, and cascade retirement.
 *
 * Story 6 Responsibilities:
 * - Listen for ApprovalEvent from VVB consensus (Story 5)
 * - Execute state transitions: PENDING_VVB → ACTIVE
 * - Update approval metadata after successful transition
 * - Execute cascade retirement of previous versions
 * - Fire completion/failure events
 * - Provide execution status and audit trail queries
 */
@ApplicationScoped
public class ApprovalExecutionService {

    @Inject
    TokenStateTransitionManager stateTransitionManager;

    @Inject
    SecondaryTokenVersionRepository versionRepository;

    @Inject
    ApprovalExecutionAuditRepository auditRepository;

    @Inject
    Event<ApprovalExecutionCompleted> completionEvent;

    @Inject
    Event<ApprovalExecutionFailed> failureEvent;

    /**
     * Listen for approval events from VVB (Story 5 integration)
     */
    @Transactional
    public void onApprovalEvent(@Observes ApprovalEvent event) {
        Log.infof("Received approval event: requestId=%s, versionId=%s",
            event.getRequestId(), event.getVersionId());

        try {
            executeApproval(event.getRequestId()).await().indefinitely();
        } catch (Exception e) {
            Log.errorf("Failed to execute approval: %s", e.getMessage());
            failureEvent.fire(new ApprovalExecutionFailed(
                event.getRequestId(),
                event.getVersionId(),
                e.getMessage()
            ));
        }
    }

    /**
     * Execute approval workflow for a VVB approval request
     *
     * @param approvalRequestId The approval request ID from Story 5
     * @return Result of execution
     */
    @Transactional
    public Uni<ExecutionResult> executeApproval(UUID approvalRequestId) {
        return Uni.createFrom().item(() -> {
            long startTime = System.currentTimeMillis();

            try {
                // Step 1: Validate approval request exists
                if (approvalRequestId == null) {
                    throw new IllegalStateException("Approval request ID is required");
                }

                // Step 2: Load token version for this approval request
                List<SecondaryTokenVersion> versions = versionRepository
                    .list("approvalRequestId = ?1", approvalRequestId);

                if (versions.isEmpty()) {
                    throw new IllegalStateException(
                        "Token version not found for approval request: " + approvalRequestId);
                }

                SecondaryTokenVersion version = versions.get(0);
                Log.infof("Executing approval for version: %s", version.id);

                // Step 3: Execute state transition (PENDING_VVB → ACTIVE)
                SecondaryTokenVersionStatus fromStatus = version.status;
                SecondaryTokenVersion updated = stateTransitionManager.executeTransition(
                    version.id,
                    fromStatus,
                    SecondaryTokenVersionStatus.ACTIVE,
                    Map.of("approval_request_id", approvalRequestId.toString())
                ).await().indefinitely();

                // Step 4: Update approval metadata
                updateApprovalMetadata(updated, approvalRequestId);

                // Step 5: Execute cascade retirement
                executeCascadeRetirement(updated);

                // Step 6: Fire completion event
                long duration = System.currentTimeMillis() - startTime;
                completionEvent.fire(new ApprovalExecutionCompleted(
                    updated.id,
                    approvalRequestId,
                    duration
                ));

                Log.infof("Approval execution completed: version=%s, duration=%dms",
                    updated.id, duration);

                return ExecutionResult.success(
                    updated.id,
                    approvalRequestId,
                    fromStatus,
                    SecondaryTokenVersionStatus.ACTIVE,
                    duration
                );

            } catch (Exception e) {
                long duration = System.currentTimeMillis() - startTime;
                Log.errorf("Approval execution failed after %dms: %s", duration, e.getMessage());
                throw new RuntimeException("Approval execution failed: " + e.getMessage(), e);
            }
        });
    }

    /**
     * Update approval metadata after successful transition
     */
    private void updateApprovalMetadata(SecondaryTokenVersion version, UUID approvalRequestId) {
        try {
            version.approvalRequestId = approvalRequestId;
            version.approvalTimestamp = LocalDateTime.now();
            version.activatedAt = LocalDateTime.now();

            versionRepository.persistAndFlush(version);
            Log.infof("Approval metadata updated: version=%s", version.id);

        } catch (Exception e) {
            Log.errorf("Failed to update approval metadata: %s", e.getMessage());
            throw new RuntimeException("Failed to update approval metadata", e);
        }
    }

    /**
     * Execute cascade retirement of previous version (if applicable)
     */
    private void executeCascadeRetirement(SecondaryTokenVersion newVersion) {
        try {
            // Check if this version has a previous version
            if (newVersion.previousVersionId == null) {
                Log.debugf("No previous version found, skipping cascade retirement: version=%s",
                    newVersion.id);
                return;
            }

            // Load previous version
            List<SecondaryTokenVersion> prevVersions = versionRepository.list("id", newVersion.previousVersionId);
            if (prevVersions.isEmpty()) {
                Log.debugf("Previous version not found: %s", newVersion.previousVersionId);
                return;
            }
            SecondaryTokenVersion previousVersion = prevVersions.get(0);

            // Check if safe to retire (no active children)
            long activeChildren = versionRepository.count(
                "previousVersionId = ?1 and status = ?2",
                previousVersion.id,
                SecondaryTokenVersionStatus.ACTIVE);

            if (activeChildren > 1) {
                Log.infof("Cannot retire version %s: %d active children", previousVersion.id, activeChildren);
                return;
            }

            // Transition previous version to REPLACED
            previousVersion.status = SecondaryTokenVersionStatus.REPLACED;
            previousVersion.replacedAt = LocalDateTime.now();
            previousVersion.replacedByVersionId = newVersion.id;
            previousVersion.updatedAt = LocalDateTime.now();

            versionRepository.persistAndFlush(previousVersion);
            Log.infof("Cascade retirement executed: %s → REPLACED", previousVersion.id);

        } catch (Exception e) {
            Log.warnf("Cascade retirement failed (non-blocking): %s", e.getMessage());
            // Don't throw - this is a non-critical operation
        }
    }

    /**
     * Get execution status for a version
     */
    public Uni<ExecutionStatus> getExecutionStatus(UUID versionId) {
        return Uni.createFrom().item(() -> {
            List<SecondaryTokenVersion> versions = versionRepository.list("id", versionId);
            if (versions.isEmpty()) {
                return null;
            }
            SecondaryTokenVersion version = versions.get(0);

            List<ApprovalExecutionAudit> audits = auditRepository.list("versionId", versionId);
            String latestPhase = audits.isEmpty() ? null : audits.get(audits.size() - 1).executionPhase;

            return new ExecutionStatus(
                versionId,
                version.status.toString(),
                version.activatedAt,
                version.approvalTimestamp,
                audits.size(),
                latestPhase
            );
        });
    }

    /**
     * Get audit trail for a version
     * BUG-S6-002 FIX: Results are now ordered chronologically by executionTimestamp
     */
    public Uni<List<ApprovalExecutionAudit>> getAuditTrail(UUID versionId) {
        return Uni.createFrom().item(() -> {
            return auditRepository.list(
                "versionId = ?1 order by executionTimestamp asc",
                versionId
            );
        });
    }

    /**
     * Rollback a failed transition for a given approval request
     */
    @Transactional
    public Uni<Boolean> rollbackTransition(UUID approvalRequestId, String reason) {
        return Uni.createFrom().item(() -> {
            try {
                // Find version with this approval request
                List<SecondaryTokenVersion> versions = versionRepository
                    .list("approvalRequestId", approvalRequestId);

                if (versions.isEmpty()) {
                    Log.warnf("No version found to rollback for approval request: %s", approvalRequestId);
                    return false;
                }

                SecondaryTokenVersion version = versions.get(0);
                return stateTransitionManager
                    .rollbackTransition(version.id, reason)
                    .await().indefinitely();

            } catch (Exception e) {
                Log.errorf("Rollback failed: %s", e.getMessage());
                return false;
            }
        });
    }

    // ============= INNER CLASSES =============

    /**
     * Result of approval execution
     */
    public static class ExecutionResult {
        public final UUID versionId;
        public final UUID approvalRequestId;
        public final String status;  // SUCCESS or FAILURE
        public final String message;
        public final SecondaryTokenVersionStatus fromStatus;
        public final SecondaryTokenVersionStatus toStatus;
        public final long durationMs;

        private ExecutionResult(
                UUID versionId,
                UUID approvalRequestId,
                String status,
                String message,
                SecondaryTokenVersionStatus fromStatus,
                SecondaryTokenVersionStatus toStatus,
                long durationMs) {
            this.versionId = versionId;
            this.approvalRequestId = approvalRequestId;
            this.status = status;
            this.message = message;
            this.fromStatus = fromStatus;
            this.toStatus = toStatus;
            this.durationMs = durationMs;
        }

        public static ExecutionResult success(
                UUID versionId,
                UUID requestId,
                SecondaryTokenVersionStatus from,
                SecondaryTokenVersionStatus to,
                long duration) {
            return new ExecutionResult(versionId, requestId, "SUCCESS",
                "Approval executed successfully", from, to, duration);
        }

        public static ExecutionResult failure(UUID requestId, String message, long duration) {
            return new ExecutionResult(null, requestId, "FAILURE", message, null, null, duration);
        }
    }

    /**
     * Execution status summary
     */
    public static class ExecutionStatus {
        public final UUID versionId;
        public final String currentStatus;
        public final LocalDateTime activatedAt;
        public final LocalDateTime approvalTimestamp;
        public final int auditEntryCount;
        public final String latestPhase;

        public ExecutionStatus(
                UUID versionId,
                String currentStatus,
                LocalDateTime activatedAt,
                LocalDateTime approvalTimestamp,
                int auditEntryCount,
                String latestPhase) {
            this.versionId = versionId;
            this.currentStatus = currentStatus;
            this.activatedAt = activatedAt;
            this.approvalTimestamp = approvalTimestamp;
            this.auditEntryCount = auditEntryCount;
            this.latestPhase = latestPhase;
        }
    }
}
