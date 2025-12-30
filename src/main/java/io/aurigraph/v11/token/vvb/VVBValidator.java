package io.aurigraph.v11.token.vvb;

import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import io.smallrye.mutiny.Uni;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * VVB (Verified Valuator Board) Validator Service
 * Core validation engine for token approval workflow with comprehensive audit trail
 */
@ApplicationScoped
public class VVBValidator {

    private static final long APPROVAL_TIMEOUT_DAYS = 7;
    private static final Map<String, VVBApprovalRule> APPROVAL_RULES = new ConcurrentHashMap<>();
    private static final Map<UUID, VVBApprovalRecord> APPROVAL_RECORDS = new ConcurrentHashMap<>();
    private static final Map<UUID, VVBValidationStatus> VALIDATION_STATUS = new ConcurrentHashMap<>();

    @Inject
    Event<VVBApprovalEvent> approvalEvent;

    static {
        // Initialize default approval rules
        APPROVAL_RULES.put("SECONDARY_TOKEN_CREATE", new VVBApprovalRule(
            "SECONDARY_TOKEN_CREATE", VVBApprovalType.STANDARD, "VVB_VALIDATOR"
        ));
        APPROVAL_RULES.put("SECONDARY_TOKEN_RETIRE", new VVBApprovalRule(
            "SECONDARY_TOKEN_RETIRE", VVBApprovalType.ELEVATED, "VVB_ADMIN"
        ));
        APPROVAL_RULES.put("PRIMARY_TOKEN_RETIRE", new VVBApprovalRule(
            "PRIMARY_TOKEN_RETIRE", VVBApprovalType.CRITICAL, "VVB_ADMIN"
        ));
        APPROVAL_RULES.put("TOKEN_SUSPENSION", new VVBApprovalRule(
            "TOKEN_SUSPENSION", VVBApprovalType.ELEVATED, "VVB_ADMIN"
        ));
    }

    /**
     * Validate token version with comprehensive rule engine
     */
    @Transactional
    public Uni<VVBApprovalResult> validateTokenVersion(UUID versionId, VVBValidationRequest request) {
        return Uni.createFrom().item(() -> {
            Log.infof("VVB validation initiated for version: %s, changeType: %s", versionId, request.getChangeType());

            // Check if changeType is provided
            if (request.getChangeType() == null) {
                return new VVBApprovalResult(
                    versionId, VVBApprovalStatus.REJECTED, "Change type is required", null
                );
            }

            // Check if rule exists
            VVBApprovalRule rule = APPROVAL_RULES.get(request.getChangeType());
            if (rule == null) {
                return new VVBApprovalResult(
                    versionId, VVBApprovalStatus.REJECTED, "Unknown change type", null
                );
            }

            // Determine required approvers based on rule
            List<String> requiredApprovers = determineRequiredApprovers(rule);

            // Create validation record
            VVBValidationStatus status = new VVBValidationStatus(
                versionId, request.getChangeType(), rule.getApprovalType(),
                requiredApprovers, Instant.now()
            );

            VALIDATION_STATUS.put(versionId, status);

            Log.infof("Validation status created for version %s, requires %d approvers",
                versionId, requiredApprovers.size());

            return new VVBApprovalResult(
                versionId, VVBApprovalStatus.PENDING_VVB, "Submitted for VVB review", requiredApprovers
            );
        });
    }

    /**
     * Approve token version by authorized approver
     */
    @Transactional
    public Uni<VVBApprovalResult> approveTokenVersion(UUID versionId, String approverIdentifier) {
        return Uni.createFrom().item(() -> {
            VVBValidationStatus status = VALIDATION_STATUS.get(versionId);
            if (status == null) {
                return new VVBApprovalResult(
                    versionId, VVBApprovalStatus.REJECTED, "Version not found in validation", null
                );
            }

            // Check timeout
            if (isApprovalTimedOut(status)) {
                VALIDATION_STATUS.remove(versionId);
                return new VVBApprovalResult(
                    versionId, VVBApprovalStatus.REJECTED, "Approval timeout exceeded", null
                );
            }

            // Verify approver authority
            if (!canApprove(approverIdentifier, status.getApprovalType())) {
                Log.warnf("Unauthorized approval attempt: %s for type %s",
                    approverIdentifier, status.getApprovalType());
                return new VVBApprovalResult(
                    versionId, VVBApprovalStatus.REJECTED, "Insufficient authority", null
                );
            }

            // Record approval
            VVBApprovalRecord record = new VVBApprovalRecord(
                UUID.randomUUID(), versionId, approverIdentifier,
                VVBApprovalDecision.APPROVED, null, Instant.now()
            );

            APPROVAL_RECORDS.put(record.getId(), record);
            status.recordApproval(approverIdentifier);

            // Check if all approvals received
            if (status.hasAllApprovalsReceived()) {
                VALIDATION_STATUS.remove(versionId);
                approvalEvent.fire(new VVBApprovalEvent(versionId, VVBApprovalStatus.APPROVED, approverIdentifier));
                Log.infof("Token version %s fully approved", versionId);

                return new VVBApprovalResult(
                    versionId, VVBApprovalStatus.APPROVED, "All approvals received", null
                );
            }

            return new VVBApprovalResult(
                versionId, VVBApprovalStatus.PENDING_VVB,
                "Approval recorded, awaiting additional approvals",
                status.getPendingApprovers()
            );
        });
    }

    /**
     * Reject token version with reason
     */
    @Transactional
    public Uni<VVBApprovalResult> rejectTokenVersion(UUID versionId, String rejectionReason) {
        return Uni.createFrom().item(() -> {
            VVBValidationStatus status = VALIDATION_STATUS.get(versionId);
            if (status == null) {
                return new VVBApprovalResult(
                    versionId, VVBApprovalStatus.REJECTED, "Version not found", null
                );
            }

            // Record rejection
            VVBApprovalRecord record = new VVBApprovalRecord(
                UUID.randomUUID(), versionId, "SYSTEM",
                VVBApprovalDecision.REJECTED, rejectionReason, Instant.now()
            );

            APPROVAL_RECORDS.put(record.getId(), record);
            VALIDATION_STATUS.remove(versionId);

            approvalEvent.fire(new VVBApprovalEvent(versionId, VVBApprovalStatus.REJECTED, "SYSTEM"));
            Log.infof("Token version %s rejected: %s", versionId, rejectionReason);

            return new VVBApprovalResult(
                versionId, VVBApprovalStatus.REJECTED, rejectionReason, null
            );
        });
    }

    /**
     * Get detailed validation status
     */
    public Uni<VVBValidationDetails> getValidationDetails(UUID versionId) {
        return Uni.createFrom().item(() -> {
            VVBValidationStatus status = VALIDATION_STATUS.get(versionId);
            List<VVBApprovalRecord> records = new ArrayList<>();

            APPROVAL_RECORDS.values().stream()
                .filter(r -> r.getVersionId().equals(versionId))
                .forEach(records::add);

            if (status == null && records.isEmpty()) {
                return null;
            }

            // Determine the submitted timestamp
            Instant submittedAt = status != null ? status.getCreatedAt() : null;
            if (submittedAt == null && !records.isEmpty()) {
                // Use the earliest record's timestamp if validation status is no longer available
                submittedAt = records.stream()
                    .map(VVBApprovalRecord::getCreatedAt)
                    .min(Instant::compareTo)
                    .orElse(null);
            }

            return new VVBValidationDetails(
                versionId,
                status != null ? status.getStatus() : VVBApprovalStatus.APPROVED,
                status != null ? status.getChangeType() : "UNKNOWN",
                records,
                submittedAt
            );
        });
    }

    /**
     * Get all pending approvals
     */
    public Uni<List<VVBValidationStatus>> getPendingApprovals() {
        return Uni.createFrom().item(() -> {
            return new ArrayList<>(VALIDATION_STATUS.values());
        });
    }

    /**
     * Get pending approvals for specific approver
     */
    public Uni<List<VVBValidationStatus>> getPendingByApprover(String approverId) {
        return Uni.createFrom().item(() -> {
            List<VVBValidationStatus> pending = new ArrayList<>();

            VALIDATION_STATUS.values().stream()
                .filter(status -> status.getRequiredApprovers().contains(approverId) &&
                                 !status.getApprovedBy().contains(approverId))
                .forEach(pending::add);

            return pending;
        });
    }

    /**
     * Get validation statistics
     */
    public Uni<VVBStatistics> getValidationStatistics() {
        return Uni.createFrom().item(() -> {
            long approved = APPROVAL_RECORDS.values().stream()
                .filter(r -> r.getDecision() == VVBApprovalDecision.APPROVED)
                .count();

            long rejected = APPROVAL_RECORDS.values().stream()
                .filter(r -> r.getDecision() == VVBApprovalDecision.REJECTED)
                .count();

            double avgApprovalTime = APPROVAL_RECORDS.values().stream()
                .mapToLong(r -> ChronoUnit.MINUTES.between(Instant.now(), r.getCreatedAt()))
                .average()
                .orElse(0.0);

            return new VVBStatistics(
                approved + rejected,
                approved,
                rejected,
                VALIDATION_STATUS.size(),
                avgApprovalTime
            );
        });
    }

    /**
     * Validate approver authority
     */
    public Uni<Boolean> validateApproverAuthority(String approverId, VVBApprovalType type) {
        return Uni.createFrom().item(() -> canApprove(approverId, type));
    }

    // ============= PRIVATE HELPER METHODS =============

    private List<String> determineRequiredApprovers(VVBApprovalRule rule) {
        List<String> approvers = new ArrayList<>();

        switch (rule.getApprovalType()) {
            case STANDARD:
                approvers.add("VVB_VALIDATOR_1");
                break;
            case ELEVATED:
                approvers.add("VVB_ADMIN_1");
                approvers.add("VVB_VALIDATOR_1");
                break;
            case CRITICAL:
                approvers.add("VVB_ADMIN_1");
                approvers.add("VVB_ADMIN_2");
                approvers.add("VVB_VALIDATOR_1");
                break;
        }

        return approvers;
    }

    private boolean canApprove(String approverId, VVBApprovalType type) {
        // Extract role from identifier (e.g., "VVB_ADMIN_1" -> "VVB_ADMIN")
        String role = approverId.substring(0, approverId.lastIndexOf('_'));

        return switch (type) {
            case STANDARD -> role.equals("VVB_VALIDATOR") || role.equals("VVB_ADMIN");
            case ELEVATED -> role.equals("VVB_ADMIN") || role.equals("VVB_VALIDATOR");  // Both roles required for quorum
            case CRITICAL -> role.equals("VVB_ADMIN");  // Only admins for critical changes
        };
    }

    private boolean isApprovalTimedOut(VVBValidationStatus status) {
        Instant deadline = status.getCreatedAt().plus(APPROVAL_TIMEOUT_DAYS, ChronoUnit.DAYS);
        return Instant.now().isAfter(deadline);
    }

    // ============= INNER CLASSES =============

    public enum VVBApprovalType {
        STANDARD,    // Single validator approval
        ELEVATED,    // Admin + validator approval
        CRITICAL     // Dual admin approval required
    }

    public enum VVBApprovalStatus {
        PENDING_VVB,
        APPROVED,
        REJECTED,
        COMPLETED,
        TIMEOUT
    }

    public enum VVBApprovalDecision {
        APPROVED,
        REJECTED,
        PENDING
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    public static class VVBApprovalRule {
        private String changeType;
        private VVBApprovalType approvalType;
        private String requiredRole;
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    public static class VVBValidationStatus {
        private UUID versionId;
        private String changeType;
        private VVBApprovalType approvalType;
        private List<String> requiredApprovers;
        private Instant createdAt;
        private final Set<String> approvedBy = ConcurrentHashMap.newKeySet();

        public void recordApproval(String approverId) {
            approvedBy.add(approverId);
        }

        public boolean hasAllApprovalsReceived() {
            return approvedBy.size() >= requiredApprovers.size();
        }

        public List<String> getPendingApprovers() {
            return requiredApprovers.stream()
                .filter(a -> !approvedBy.contains(a))
                .toList();
        }

        public VVBApprovalStatus getStatus() {
            if (hasAllApprovalsReceived()) return VVBApprovalStatus.APPROVED;
            if (Instant.now().isAfter(createdAt.plus(APPROVAL_TIMEOUT_DAYS, ChronoUnit.DAYS)))
                return VVBApprovalStatus.TIMEOUT;
            return VVBApprovalStatus.PENDING_VVB;
        }
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    public static class VVBApprovalRecord {
        private UUID id;
        private UUID versionId;
        private String approverId;
        private VVBApprovalDecision decision;
        private String reason;
        private Instant createdAt;
    }
}
