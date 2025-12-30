package io.aurigraph.v11.token.vvb;

import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import io.smallrye.mutiny.Uni;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * VVB Workflow Service
 * Manages token approval workflow state transitions and cascading operations
 */
@ApplicationScoped
public class VVBWorkflowService {

    @Inject
    VVBValidator validator;

    @Inject
    Event<VVBWorkflowEvent> workflowEvent;

    private final Map<UUID, TokenVersionWithVVB> tokenVersions = new ConcurrentHashMap<>();
    private final Map<String, ApprovalStatistics> statisticsCache = new ConcurrentHashMap<>();

    /**
     * Submit token version for VVB approval
     */
    @Transactional
    public Uni<TokenVersionWithVVB> submitForApproval(UUID versionId, String submitter) {
        return Uni.createFrom().item(() -> {
            Log.infof("Submitting version %s for approval by %s", versionId, submitter);

            TokenVersionWithVVB version = new TokenVersionWithVVB(
                versionId,
                TokenVersionState.PENDING_VVB,
                submitter,
                Instant.now()
            );

            tokenVersions.put(versionId, version);
            workflowEvent.fire(new VVBWorkflowEvent(versionId, TokenVersionState.PENDING_VVB, "Submitted"));

            return version;
        });
    }

    /**
     * Process approval decision and manage state transitions
     */
    @Transactional
    public Uni<TokenVersionWithVVB> processApproval(UUID versionId, VVBApprovalDecision decision) {
        return Uni.createFrom().item(() -> {
            TokenVersionWithVVB version = tokenVersions.get(versionId);
            if (version == null) {
                Log.warnf("Version not found: %s", versionId);
                return null;
            }

            TokenVersionState newState = decision.getDecision() == VVBValidator.VVBApprovalDecision.APPROVED
                ? TokenVersionState.APPROVED
                : TokenVersionState.REJECTED;

            version.setState(newState);
            version.setApprovalTimestamp(Instant.now());

            if (newState == TokenVersionState.REJECTED) {
                // Cascade rejection to dependent tokens
                cascadeRejection(versionId, decision.getReason());
            }

            workflowEvent.fire(new VVBWorkflowEvent(
                versionId, newState, decision.getReason()
            ));

            Log.infof("Version %s transitioned to %s", versionId, newState);

            return version;
        });
    }

    /**
     * Get pending approvals for specific user
     */
    @Transactional
    public Uni<List<PendingApprovalDetail>> getPendingApprovalsForUser(String userId) {
        return Uni.createFrom().item(() -> {
            return validator.getPendingByApprover(userId).await().indefinitely()
                .stream()
                .map(status -> new PendingApprovalDetail(
                    status.getVersionId(),
                    status.getChangeType(),
                    status.getCreatedAt(),
                    status.getApprovalType().toString()
                ))
                .collect(Collectors.toList());
        });
    }

    /**
     * Generate approval statistics report
     */
    @Transactional
    public Uni<ApprovalStatistics> generateApprovalReport(LocalDate startDate, LocalDate endDate) {
        return Uni.createFrom().item(() -> {
            String key = startDate + "_" + endDate;

            ApprovalStatistics cached = statisticsCache.get(key);
            if (cached != null) {
                return cached;
            }

            VVBStatistics vvbStats = validator.getValidationStatistics().await().indefinitely();

            ApprovalStatistics report = new ApprovalStatistics(
                startDate,
                endDate,
                vvbStats.getApprovedCount(),
                vvbStats.getRejectedCount(),
                vvbStats.getPendingCount(),
                vvbStats.getAverageApprovalTimeMinutes(),
                vvbStats.getApprovalRate()
            );

            statisticsCache.put(key, report);

            return report;
        });
    }

    /**
     * Determine if change can proceed without VVB approval
     */
    public Uni<Boolean> canProceedWithoutApproval(UUID versionId, String changeType) {
        return Uni.createFrom().item(() -> {
            // Changes that can bypass approval
            Set<String> bypassableChanges = Set.of(
                "SECONDARY_TOKEN_ACTIVATE",
                "SECONDARY_TOKEN_REDEEM",
                "TOKEN_TRANSFER"
            );

            return bypassableChanges.contains(changeType);
        });
    }

    /**
     * Handle approval timeout
     */
    @Transactional
    public Uni<Void> handleApprovalTimeout(UUID versionId) {
        return Uni.createFrom().item(() -> {
            TokenVersionWithVVB version = tokenVersions.get(versionId);
            if (version != null && version.getState() == TokenVersionState.PENDING_VVB) {
                version.setState(TokenVersionState.TIMEOUT);
                workflowEvent.fire(new VVBWorkflowEvent(
                    versionId, TokenVersionState.TIMEOUT, "Approval timeout"
                ));

                Log.warnf("Approval timeout for version %s", versionId);
            }
            return null;
        });
    }

    // ============= PRIVATE HELPER METHODS =============

    private void cascadeRejection(UUID versionId, String reason) {
        Log.infof("Cascading rejection for version %s: %s", versionId, reason);

        // Find dependent tokens and mark as rejected
        tokenVersions.values().stream()
            .filter(v -> v.getParentVersionId() != null &&
                        v.getParentVersionId().equals(versionId) &&
                        v.getState() == TokenVersionState.PENDING_VVB)
            .forEach(v -> {
                v.setState(TokenVersionState.REJECTED);
                workflowEvent.fire(new VVBWorkflowEvent(
                    v.getVersionId(),
                    TokenVersionState.REJECTED,
                    "Cascaded from parent rejection: " + reason
                ));
            });
    }

    // ============= INNER CLASSES =============

    public enum TokenVersionState {
        CREATED,
        PENDING_VVB,
        APPROVED,
        REJECTED,
        ACTIVE,
        TIMEOUT,
        RETIRED
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    public static class TokenVersionWithVVB {
        private UUID versionId;
        private TokenVersionState state;
        private String submittedBy;
        private Instant submittedAt;
        private Instant approvalTimestamp;
        private UUID parentVersionId;

        public TokenVersionWithVVB(UUID versionId, TokenVersionState state, String submittedBy, Instant submittedAt) {
            this.versionId = versionId;
            this.state = state;
            this.submittedBy = submittedBy;
            this.submittedAt = submittedAt;
        }

        public long getHoursInApproval() {
            if (approvalTimestamp == null) {
                return ChronoUnit.HOURS.between(submittedAt, Instant.now());
            }
            return ChronoUnit.HOURS.between(submittedAt, approvalTimestamp);
        }
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    public static class VVBApprovalDecision {
        private UUID versionId;
        private VVBValidator.VVBApprovalDecision decision;
        private String reason;
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    public static class PendingApprovalDetail {
        private UUID versionId;
        private String changeType;
        private Instant submittedAt;
        private String approvalType;
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    public static class ApprovalStatistics {
        private LocalDate startDate;
        private LocalDate endDate;
        private long approvedCount;
        private long rejectedCount;
        private long pendingCount;
        private double averageApprovalTimeMinutes;
        private double approvalRate;

        public long getTotalDecisions() {
            return approvedCount + rejectedCount;
        }

        public long getTotalProcessed() {
            return approvedCount + rejectedCount + pendingCount;
        }
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    public static class VVBWorkflowEvent {
        private UUID versionId;
        private TokenVersionState newState;
        private String details;
        private Instant timestamp;

        public VVBWorkflowEvent(UUID versionId, TokenVersionState newState, String details) {
            this.versionId = versionId;
            this.newState = newState;
            this.details = details;
            this.timestamp = Instant.now();
        }
    }
}
