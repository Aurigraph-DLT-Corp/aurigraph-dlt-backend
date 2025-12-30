package io.aurigraph.v11.token.vvb;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * VVBTestDataBuilder - Test data utilities for VVB Approval Workflow tests
 * Provides builders, factories, and random data generators for comprehensive testing
 *
 * Usage:
 *   VVBValidationRequest req = VVBTestDataBuilder.validationRequest()
 *       .changeType("SECONDARY_TOKEN_CREATE")
 *       .submitterId("USER_1")
 *       .build();
 *
 *   VVBValidationRequest random = VVBTestDataBuilder.randomValidationRequest();
 *
 *   List<String> approvers = VVBTestDataBuilder.generateApprovers(5);
 */
public class VVBTestDataBuilder {

    private static final String[] CHANGE_TYPES = {
        "SECONDARY_TOKEN_CREATE",
        "SECONDARY_TOKEN_RETIRE",
        "PRIMARY_TOKEN_RETIRE",
        "TOKEN_SUSPENSION",
        "TOKEN_REACTIVATION",
        "TOKEN_TRANSFER"
    };

    private static final String[] APPROVER_ROLES = {
        "VVB_VALIDATOR",
        "VVB_ADMIN",
        "VVB_SUPERVISOR"
    };

    private static final String[] REJECTION_REASONS = {
        "Compliance check failed",
        "Invalid token data",
        "Unauthorized change type",
        "Security validation failed",
        "Insufficient permissions",
        "Token already exists",
        "Invalid parent token"
    };

    // ============= VALIDATION REQUEST BUILDER =============

    public static ValidationRequestBuilder validationRequest() {
        return new ValidationRequestBuilder();
    }

    public static class ValidationRequestBuilder {
        private String changeType = "SECONDARY_TOKEN_CREATE";
        private String description = "Test validation request";
        private Map<String, Object> metadata = new HashMap<>();
        private String submitterId = "TEST_USER";

        public ValidationRequestBuilder changeType(String changeType) {
            this.changeType = changeType;
            return this;
        }

        public ValidationRequestBuilder description(String description) {
            this.description = description;
            return this;
        }

        public ValidationRequestBuilder metadata(Map<String, Object> metadata) {
            this.metadata = metadata;
            return this;
        }

        public ValidationRequestBuilder submitterId(String submitterId) {
            this.submitterId = submitterId;
            return this;
        }

        public VVBValidationRequest build() {
            return new VVBValidationRequest(changeType, description, metadata, submitterId);
        }

        public VVBValidationRequest buildStandard() {
            this.changeType = "SECONDARY_TOKEN_CREATE";
            return build();
        }

        public VVBValidationRequest buildElevated() {
            this.changeType = "SECONDARY_TOKEN_RETIRE";
            return build();
        }

        public VVBValidationRequest buildCritical() {
            this.changeType = "PRIMARY_TOKEN_RETIRE";
            return build();
        }
    }

    // ============= APPROVAL REQUEST BUILDER =============

    public static ApprovalRequestBuilder approvalRequest() {
        return new ApprovalRequestBuilder();
    }

    public static class ApprovalRequestBuilder {
        private UUID versionId = UUID.randomUUID();
        private String approverId = "VVB_VALIDATOR_1";
        private VVBValidator.VVBApprovalType approvalType = VVBValidator.VVBApprovalType.STANDARD;
        private List<String> requiredApprovers = new ArrayList<>();
        private String reason = null;

        public ApprovalRequestBuilder versionId(UUID versionId) {
            this.versionId = versionId;
            return this;
        }

        public ApprovalRequestBuilder approverId(String approverId) {
            this.approverId = approverId;
            return this;
        }

        public ApprovalRequestBuilder approvalType(VVBValidator.VVBApprovalType type) {
            this.approvalType = type;
            return this;
        }

        public ApprovalRequestBuilder requiredApprovers(List<String> approvers) {
            this.requiredApprovers = new ArrayList<>(approvers);
            return this;
        }

        public ApprovalRequestBuilder reason(String reason) {
            this.reason = reason;
            return this;
        }

        public Map<String, Object> build() {
            Map<String, Object> request = new HashMap<>();
            request.put("versionId", versionId);
            request.put("approverId", approverId);
            request.put("approvalType", approvalType);
            request.put("requiredApprovers", requiredApprovers);
            if (reason != null) {
                request.put("reason", reason);
            }
            return request;
        }

        public Map<String, Object> buildStandard() {
            this.approvalType = VVBValidator.VVBApprovalType.STANDARD;
            this.requiredApprovers = List.of("VVB_VALIDATOR_1");
            return build();
        }

        public Map<String, Object> buildElevated() {
            this.approvalType = VVBValidator.VVBApprovalType.ELEVATED;
            this.requiredApprovers = List.of("VVB_ADMIN_1", "VVB_VALIDATOR_1");
            return build();
        }

        public Map<String, Object> buildCritical() {
            this.approvalType = VVBValidator.VVBApprovalType.CRITICAL;
            this.requiredApprovers = List.of("VVB_ADMIN_1", "VVB_ADMIN_2", "VVB_VALIDATOR_1");
            return build();
        }
    }

    // ============= RANDOM DATA GENERATORS =============

    public static VVBValidationRequest randomValidationRequest() {
        String changeType = randomChangeType();
        return new VVBValidationRequest(
            changeType,
            "Random validation request for " + changeType,
            randomMetadata(),
            randomSubmitterId()
        );
    }

    public static VVBValidationRequest randomValidationRequest(String changeType) {
        return new VVBValidationRequest(
            changeType,
            "Validation request for " + changeType,
            randomMetadata(),
            randomSubmitterId()
        );
    }

    public static String randomChangeType() {
        return CHANGE_TYPES[ThreadLocalRandom.current().nextInt(CHANGE_TYPES.length)];
    }

    public static String randomSubmitterId() {
        return "USER_" + ThreadLocalRandom.current().nextInt(1000);
    }

    public static String randomApproverId() {
        String role = APPROVER_ROLES[ThreadLocalRandom.current().nextInt(APPROVER_ROLES.length)];
        return role + "_" + ThreadLocalRandom.current().nextInt(10);
    }

    public static String randomRejectionReason() {
        return REJECTION_REASONS[ThreadLocalRandom.current().nextInt(REJECTION_REASONS.length)];
    }

    public static Map<String, Object> randomMetadata() {
        Map<String, Object> metadata = new HashMap<>();
        int fieldCount = ThreadLocalRandom.current().nextInt(1, 5);
        for (int i = 0; i < fieldCount; i++) {
            metadata.put("field_" + i, "value_" + UUID.randomUUID());
        }
        return metadata;
    }

    // ============= FACTORY METHODS =============

    public static List<String> generateApprovers(int count) {
        List<String> approvers = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            approvers.add("VVB_APPROVER_" + i);
        }
        return approvers;
    }

    public static List<String> generateValidators(int count) {
        List<String> validators = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            validators.add("VVB_VALIDATOR_" + i);
        }
        return validators;
    }

    public static List<String> generateAdmins(int count) {
        List<String> admins = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            admins.add("VVB_ADMIN_" + i);
        }
        return admins;
    }

    public static List<UUID> generateVersionIds(int count) {
        List<UUID> ids = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            ids.add(UUID.randomUUID());
        }
        return ids;
    }

    // ============= SIGNATURE GENERATION (Mock) =============

    /**
     * Generate mock signature for approver
     * In real implementation, would use actual cryptographic signing
     */
    public static String generateMockSignature(UUID versionId, String approverId) {
        return "SIGNATURE_" + versionId.toString().substring(0, 8) + "_" + approverId.hashCode();
    }

    /**
     * Generate mock signature batch
     */
    public static Map<String, String> generateMockSignatures(UUID versionId, List<String> approverIds) {
        Map<String, String> signatures = new HashMap<>();
        for (String approverId : approverIds) {
            signatures.put(approverId, generateMockSignature(versionId, approverId));
        }
        return signatures;
    }

    // ============= APPROVAL RECORD FACTORY =============

    public static VVBValidator.VVBApprovalRecord createApprovalRecord(
            UUID recordId,
            UUID versionId,
            String approverId,
            VVBValidator.VVBApprovalDecision decision) {
        return new VVBValidator.VVBApprovalRecord(
            recordId,
            versionId,
            approverId,
            decision,
            decision == VVBValidator.VVBApprovalDecision.REJECTED ? randomRejectionReason() : null,
            Instant.now()
        );
    }

    public static VVBValidator.VVBApprovalRecord createApprovalRecord(
            UUID versionId,
            String approverId,
            VVBValidator.VVBApprovalDecision decision) {
        return createApprovalRecord(UUID.randomUUID(), versionId, approverId, decision);
    }

    // ============= VALIDATION STATUS FACTORY =============

    public static VVBValidator.VVBValidationStatus createValidationStatus(
            UUID versionId,
            String changeType,
            VVBValidator.VVBApprovalType approvalType,
            List<String> requiredApprovers) {
        return new VVBValidator.VVBValidationStatus(
            versionId,
            changeType,
            approvalType,
            requiredApprovers,
            Instant.now()
        );
    }

    public static VVBValidator.VVBValidationStatus createValidationStatusStandard(UUID versionId) {
        return createValidationStatus(
            versionId,
            "SECONDARY_TOKEN_CREATE",
            VVBValidator.VVBApprovalType.STANDARD,
            generateValidators(1)
        );
    }

    public static VVBValidator.VVBValidationStatus createValidationStatusElevated(UUID versionId) {
        List<String> approvers = new ArrayList<>();
        approvers.addAll(generateAdmins(1));
        approvers.addAll(generateValidators(1));
        return createValidationStatus(
            versionId,
            "SECONDARY_TOKEN_RETIRE",
            VVBValidator.VVBApprovalType.ELEVATED,
            approvers
        );
    }

    public static VVBValidator.VVBValidationStatus createValidationStatusCritical(UUID versionId) {
        List<String> approvers = new ArrayList<>();
        approvers.addAll(generateAdmins(2));
        approvers.addAll(generateValidators(1));
        return createValidationStatus(
            versionId,
            "PRIMARY_TOKEN_RETIRE",
            VVBValidator.VVBApprovalType.CRITICAL,
            approvers
        );
    }

    // ============= APPROVAL RESULT FACTORY =============

    public static VVBApprovalResult createApprovalResult(
            UUID versionId,
            VVBValidator.VVBApprovalStatus status,
            String message,
            List<String> pendingApprovers) {
        return new VVBApprovalResult(versionId, status, message, pendingApprovers);
    }

    public static VVBApprovalResult createApprovedResult(UUID versionId) {
        return new VVBApprovalResult(
            versionId,
            VVBValidator.VVBApprovalStatus.APPROVED,
            "Token approved",
            null
        );
    }

    public static VVBApprovalResult createRejectedResult(UUID versionId, String reason) {
        return new VVBApprovalResult(
            versionId,
            VVBValidator.VVBApprovalStatus.REJECTED,
            reason,
            null
        );
    }

    public static VVBApprovalResult createPendingResult(UUID versionId, List<String> pendingApprovers) {
        return new VVBApprovalResult(
            versionId,
            VVBValidator.VVBApprovalStatus.PENDING_VVB,
            "Awaiting approvals",
            pendingApprovers
        );
    }

    // ============= BULK DATA GENERATORS =============

    /**
     * Generate test data sets for stress testing
     */
    public static List<VVBValidationRequest> generateValidationRequests(int count) {
        List<VVBValidationRequest> requests = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            requests.add(randomValidationRequest());
        }
        return requests;
    }

    /**
     * Generate approval records for testing statistics
     */
    public static List<VVBValidator.VVBApprovalRecord> generateApprovalRecords(
            UUID versionId,
            int approvalCount,
            int rejectionCount) {
        List<VVBValidator.VVBApprovalRecord> records = new ArrayList<>();

        // Generate approvals
        for (int i = 0; i < approvalCount; i++) {
            records.add(createApprovalRecord(
                versionId,
                randomApproverId(),
                VVBValidator.VVBApprovalDecision.APPROVED
            ));
        }

        // Generate rejections
        for (int i = 0; i < rejectionCount; i++) {
            records.add(createApprovalRecord(
                versionId,
                randomApproverId(),
                VVBValidator.VVBApprovalDecision.REJECTED
            ));
        }

        return records;
    }

    // ============= PROPERTY-BASED TEST GENERATORS =============

    /**
     * Generate random valid approval type
     */
    public static VVBValidator.VVBApprovalType randomApprovalType() {
        VVBValidator.VVBApprovalType[] types = VVBValidator.VVBApprovalType.values();
        return types[ThreadLocalRandom.current().nextInt(types.length)];
    }

    /**
     * Generate random approval status
     */
    public static VVBValidator.VVBApprovalStatus randomApprovalStatus() {
        VVBValidator.VVBApprovalStatus[] statuses = VVBValidator.VVBApprovalStatus.values();
        return statuses[ThreadLocalRandom.current().nextInt(statuses.length)];
    }

    /**
     * Generate random approval decision
     */
    public static VVBValidator.VVBApprovalDecision randomApprovalDecision() {
        VVBValidator.VVBApprovalDecision[] decisions = VVBValidator.VVBApprovalDecision.values();
        return decisions[ThreadLocalRandom.current().nextInt(decisions.length)];
    }

    // ============= SCENARIO BUILDERS =============

    /**
     * Build a complete approval workflow scenario
     */
    public static ApprovalScenario buildApprovalScenario(
            VVBValidator.VVBApprovalType approvalType,
            int approverCount,
            int rejectCount) {
        ApprovalScenario scenario = new ApprovalScenario();
        scenario.versionId = UUID.randomUUID();
        scenario.approvalType = approvalType;
        scenario.requiredApprovers = generateApprovers(approverCount);
        scenario.approvingApprovers = new ArrayList<>();
        scenario.rejectingApprovers = new ArrayList<>();

        // Select which approvers approve/reject
        for (int i = 0; i < approverCount - rejectCount; i++) {
            scenario.approvingApprovers.add(scenario.requiredApprovers.get(i));
        }
        for (int i = approverCount - rejectCount; i < approverCount; i++) {
            scenario.rejectingApprovers.add(scenario.requiredApprovers.get(i));
        }

        return scenario;
    }

    public static class ApprovalScenario {
        public UUID versionId;
        public VVBValidator.VVBApprovalType approvalType;
        public List<String> requiredApprovers;
        public List<String> approvingApprovers;
        public List<String> rejectingApprovers;
    }
}
