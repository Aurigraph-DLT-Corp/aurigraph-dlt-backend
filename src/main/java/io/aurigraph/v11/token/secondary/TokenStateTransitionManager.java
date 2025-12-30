package io.aurigraph.v11.token.secondary;

import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import io.smallrye.mutiny.Uni;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

/**
 * TokenStateTransitionManager
 * Manages state transitions with comprehensive audit trail and rollback support.
 * Integrates with Story 4's SecondaryTokenVersionStateMachine for validation.
 *
 * Responsibilities:
 * - Validate state transitions using state machine rules
 * - Execute state changes atomically with audit trail
 * - Record execution phases with audit trail
 * - Support rollback of failed transitions
 */
@ApplicationScoped
public class TokenStateTransitionManager {

    @Inject
    SecondaryTokenVersionRepository versionRepository;

    @Inject
    ApprovalExecutionAuditRepository auditRepository;

    @Inject
    ObjectMapper objectMapper;

    /**
     * Execute state transition with validation, audit trail, and error handling
     *
     * @param versionId Version to transition
     * @param fromStatus Current status (must match)
     * @param toStatus Target status (must be valid per state machine)
     * @param metadata Additional execution metadata
     * @return Version after successful transition
     */
    @Transactional
    public Uni<SecondaryTokenVersion> executeTransition(
            UUID versionId,
            SecondaryTokenVersionStatus fromStatus,
            SecondaryTokenVersionStatus toStatus,
            Map<String, Object> metadata) {

        return Uni.createFrom().item(() -> {
            try {
                // Load version by UUID
                List<SecondaryTokenVersion> versions = versionRepository.list("id", versionId);
                if (versions.isEmpty()) {
                    throw new IllegalStateException("Version not found: " + versionId);
                }
                SecondaryTokenVersion version = versions.get(0);

                // Validate current status matches expected
                if (!version.status.equals(fromStatus)) {
                    throw new IllegalStateException(
                        String.format("Invalid status: expected %s, got %s", fromStatus, version.status));
                }

                // Validate state machine allows transition
                if (!canTransition(version, toStatus)) {
                    throw new IllegalStateException(
                        String.format("Transition %s → %s not allowed", fromStatus, toStatus));
                }

                // Record INITIATED phase
                recordAuditPhase(versionId, fromStatus, toStatus, "INITIATED", metadata);

                // Record VALIDATED phase
                recordAuditPhase(versionId, fromStatus, toStatus, "VALIDATED", metadata);

                // Execute transition
                version.status = toStatus;
                version.updatedAt = LocalDateTime.now();

                // Set activation timestamp if transitioning to ACTIVE
                if (toStatus == SecondaryTokenVersionStatus.ACTIVE) {
                    version.activatedAt = LocalDateTime.now();
                }

                // Persist changes
                versionRepository.persistAndFlush(version);

                // Record TRANSITIONED phase
                recordAuditPhase(versionId, fromStatus, toStatus, "TRANSITIONED", metadata);

                // Record COMPLETED phase
                recordAuditPhase(versionId, fromStatus, toStatus, "COMPLETED", metadata);

                Log.infof("State transition completed: %s → %s", fromStatus, toStatus);
                return version;

            } catch (Exception e) {
                Log.errorf("State transition failed: %s", e.getMessage());
                recordAuditPhase(versionId, fromStatus, toStatus, "FAILED",
                    Map.of("error", e.getMessage()));
                throw new RuntimeException("State transition failed: " + e.getMessage(), e);
            }
        });
    }

    /**
     * Check if state machine allows transition
     */
    private boolean canTransition(SecondaryTokenVersion version, SecondaryTokenVersionStatus toStatus) {
        SecondaryTokenVersionStatus current = version.status;

        return switch (current) {
            case CREATED -> toStatus == SecondaryTokenVersionStatus.PENDING_VVB;
            case PENDING_VVB -> toStatus == SecondaryTokenVersionStatus.ACTIVE ||
                               toStatus == SecondaryTokenVersionStatus.REJECTED;
            case ACTIVE -> toStatus == SecondaryTokenVersionStatus.REPLACED ||
                          toStatus == SecondaryTokenVersionStatus.EXPIRED;
            case REPLACED -> toStatus == SecondaryTokenVersionStatus.ARCHIVED;
            case REJECTED, EXPIRED, ARCHIVED -> false;
        };
    }

    /**
     * Record execution phase in audit trail
     */
    private void recordAuditPhase(
            UUID versionId,
            SecondaryTokenVersionStatus fromStatus,
            SecondaryTokenVersionStatus toStatus,
            String phase,
            Map<String, Object> metadata) {
        try {
            ApprovalExecutionAudit audit = new ApprovalExecutionAudit();
            audit.auditId = UUID.randomUUID();
            audit.versionId = versionId;
            audit.executionPhase = phase;
            audit.previousStatus = fromStatus != null ? fromStatus.toString() : null;
            audit.newStatus = toStatus != null ? toStatus.toString() : null;
            audit.executedBy = "SYSTEM";
            audit.executionTimestamp = Instant.now();
            // Convert Map to JsonNode for JSON storage
            audit.metadata = metadata != null ? objectMapper.valueToTree(metadata) : null;

            auditRepository.persistAndFlush(audit);
        } catch (Exception e) {
            Log.warnf("Failed to record audit phase %s: %s", phase, e.getMessage());
        }
    }

    /**
     * Rollback a failed transition
     */
    @Transactional
    public Uni<Boolean> rollbackTransition(UUID versionId, String reason) {
        return Uni.createFrom().item(() -> {
            try {
                List<SecondaryTokenVersion> versions = versionRepository.list("id", versionId);
                if (versions.isEmpty()) {
                    return false;
                }
                SecondaryTokenVersion version = versions.get(0);

                // Record rollback phase
                recordAuditPhase(versionId, version.status, null, "ROLLED_BACK",
                    Map.of("reason", reason));

                Log.infof("Rollback recorded for version: %s", versionId);
                return true;
            } catch (Exception e) {
                Log.errorf("Rollback failed: %s", e.getMessage());
                return false;
            }
        });
    }

    /**
     * Get audit history for a version
     */
    public Uni<List<ApprovalExecutionAudit>> getAuditHistory(UUID versionId) {
        return Uni.createFrom().item(() -> {
            return auditRepository.list("versionId", versionId);
        });
    }
}
