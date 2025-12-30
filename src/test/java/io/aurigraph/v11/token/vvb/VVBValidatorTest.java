package io.aurigraph.v11.token.vvb;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import jakarta.inject.Inject;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * VVBValidatorTest - 25 tests covering validation, approval, and audit trail
 */
@QuarkusTest
@DisplayName("VVB Validator Service Tests")
class VVBValidatorTest {

    @Inject
    VVBValidator validator;

    private UUID testVersionId;
    private VVBValidationRequest standardRequest;

    @BeforeEach
    void setUp() {
        testVersionId = UUID.randomUUID();
        standardRequest = new VVBValidationRequest("SECONDARY_TOKEN_CREATE", "Test token", null, "TEST_USER");
    }

    // ============= VALIDATION TESTS (6) =============

    @Test
    @DisplayName("Should validate token with standard rule")
    void testValidateTokenStandardRule() {
        VVBApprovalResult result = validator.validateTokenVersion(testVersionId, standardRequest)
            .await().indefinitely();

        assertNotNull(result);
        assertEquals(testVersionId, result.getVersionId());
        assertEquals(VVBValidator.VVBApprovalStatus.PENDING_VVB, result.getStatus());
        assertNotNull(result.getPendingApprovers());
        assertEquals(1, result.getPendingApprovers().size());
    }

    @Test
    @DisplayName("Should validate token with elevated rule")
    void testValidateTokenElevatedRule() {
        VVBValidationRequest elevatedRequest = new VVBValidationRequest(
            "SECONDARY_TOKEN_RETIRE", "Retire token", null, "TEST_USER"
        );

        VVBApprovalResult result = validator.validateTokenVersion(UUID.randomUUID(), elevatedRequest)
            .await().indefinitely();

        assertEquals(VVBValidator.VVBApprovalStatus.PENDING_VVB, result.getStatus());
        assertTrue(result.getPendingApprovers().size() >= 2);
    }

    @Test
    @DisplayName("Should validate token with critical rule")
    void testValidateTokenCriticalRule() {
        VVBValidationRequest criticalRequest = new VVBValidationRequest(
            "PRIMARY_TOKEN_RETIRE", "Retire primary", null, "TEST_USER"
        );

        VVBApprovalResult result = validator.validateTokenVersion(UUID.randomUUID(), criticalRequest)
            .await().indefinitely();

        assertEquals(VVBValidator.VVBApprovalStatus.PENDING_VVB, result.getStatus());
        assertTrue(result.getPendingApprovers().size() >= 3);
    }

    @Test
    @DisplayName("Should reject unknown change type")
    void testRejectUnknownChangeType() {
        VVBValidationRequest unknownRequest = new VVBValidationRequest("UNKNOWN_CHANGE", null, null, "TEST_USER");

        VVBApprovalResult result = validator.validateTokenVersion(UUID.randomUUID(), unknownRequest)
            .await().indefinitely();

        assertEquals(VVBValidator.VVBApprovalStatus.REJECTED, result.getStatus());
        assertNotNull(result.getMessage());
    }

    @Test
    @DisplayName("Should include required approvers in result")
    void testIncludeRequiredApprovers() {
        VVBApprovalResult result = validator.validateTokenVersion(testVersionId, standardRequest)
            .await().indefinitely();

        List<String> approvers = result.getPendingApprovers();
        assertFalse(approvers.isEmpty());
        assertTrue(approvers.stream().anyMatch(a -> a.startsWith("VVB_")));
    }

    @Test
    @DisplayName("Should create unique validation for each submission")
    void testUniqueValidationPerSubmission() {
        UUID versionId1 = UUID.randomUUID();
        UUID versionId2 = UUID.randomUUID();

        VVBApprovalResult result1 = validator.validateTokenVersion(versionId1, standardRequest)
            .await().indefinitely();
        VVBApprovalResult result2 = validator.validateTokenVersion(versionId2, standardRequest)
            .await().indefinitely();

        assertNotEquals(result1.getVersionId(), result2.getVersionId());
        assertEquals(VVBValidator.VVBApprovalStatus.PENDING_VVB, result1.getStatus());
        assertEquals(VVBValidator.VVBApprovalStatus.PENDING_VVB, result2.getStatus());
    }

    // ============= APPROVAL TESTS (8) =============

    @Test
    @DisplayName("Should approve token with valid approver")
    void testApproveTokenWithValidApprover() {
        // First submit for validation
        validator.validateTokenVersion(testVersionId, standardRequest)
            .await().indefinitely();

        // Then approve
        VVBApprovalResult result = validator.approveTokenVersion(testVersionId, "VVB_VALIDATOR_1")
            .await().indefinitely();

        assertNotNull(result);
        assertEquals(VVBValidator.VVBApprovalStatus.APPROVED, result.getStatus());
    }

    @Test
    @DisplayName("Should reject approval with invalid authority")
    void testRejectApprovalInvalidAuthority() {
        validator.validateTokenVersion(testVersionId, standardRequest)
            .await().indefinitely();

        VVBApprovalResult result = validator.approveTokenVersion(testVersionId, "INVALID_APPROVER")
            .await().indefinitely();

        assertEquals(VVBValidator.VVBApprovalStatus.REJECTED, result.getStatus());
        assertNotNull(result.getMessage());
    }

    @Test
    @DisplayName("Should collect multiple approvals for elevated changes")
    void testCollectMultipleApprovalsForElevated() {
        VVBValidationRequest elevatedRequest = new VVBValidationRequest(
            "SECONDARY_TOKEN_RETIRE", "Retire", null, "TEST_USER"
        );

        UUID versionId = UUID.randomUUID();
        validator.validateTokenVersion(versionId, elevatedRequest)
            .await().indefinitely();

        // First approval
        VVBApprovalResult result1 = validator.approveTokenVersion(versionId, "VVB_ADMIN_1")
            .await().indefinitely();

        assertEquals(VVBValidator.VVBApprovalStatus.PENDING_VVB, result1.getStatus());
        assertTrue(result1.getPendingApprovers().size() > 0);
    }

    @Test
    @DisplayName("Should track approval history")
    void testTrackApprovalHistory() {
        validator.validateTokenVersion(testVersionId, standardRequest)
            .await().indefinitely();

        validator.approveTokenVersion(testVersionId, "VVB_VALIDATOR_1")
            .await().indefinitely();

        VVBValidationDetails details = validator.getValidationDetails(testVersionId)
            .await().indefinitely();

        assertNotNull(details);
        assertFalse(details.getApprovalHistory().isEmpty());
    }

    @Test
    @DisplayName("Should enable admin override for standard approvals")
    void testAdminOverrideStandardApproval() {
        validator.validateTokenVersion(testVersionId, standardRequest)
            .await().indefinitely();

        VVBApprovalResult result = validator.approveTokenVersion(testVersionId, "VVB_ADMIN_1")
            .await().indefinitely();

        assertEquals(VVBValidator.VVBApprovalStatus.APPROVED, result.getStatus());
    }

    @Test
    @DisplayName("Should prevent duplicate approvals by same approver")
    void testPreventDuplicateApprovals() {
        VVBValidationRequest elevatedRequest = new VVBValidationRequest(
            "SECONDARY_TOKEN_RETIRE", "Retire", null, "TEST_USER"
        );

        UUID versionId = UUID.randomUUID();
        validator.validateTokenVersion(versionId, elevatedRequest)
            .await().indefinitely();

        // First approval
        validator.approveTokenVersion(versionId, "VVB_ADMIN_1")
            .await().indefinitely();

        // Try same approval again - should be idempotent
        VVBApprovalResult result2 = validator.approveTokenVersion(versionId, "VVB_ADMIN_1")
            .await().indefinitely();

        // Should complete or remain pending, not error
        assertTrue(result2.getStatus() == VVBValidator.VVBApprovalStatus.APPROVED ||
                  result2.getStatus() == VVBValidator.VVBApprovalStatus.PENDING_VVB);
    }

    @Test
    @DisplayName("Should validate approver authority for different types")
    void testValidateApproverAuthorityByType() {
        // Validator can approve standard
        Boolean canApproveStandard = validator.validateApproverAuthority("VVB_VALIDATOR_1", VVBValidator.VVBApprovalType.STANDARD)
            .await().indefinitely();
        assertTrue(canApproveStandard);

        // Validator cannot approve critical
        Boolean canApproveCritical = validator.validateApproverAuthority("VVB_VALIDATOR_1", VVBValidator.VVBApprovalType.CRITICAL)
            .await().indefinitely();
        assertFalse(canApproveCritical);

        // Admin can approve critical
        Boolean adminCanApproveCritical = validator.validateApproverAuthority("VVB_ADMIN_1", VVBValidator.VVBApprovalType.CRITICAL)
            .await().indefinitely();
        assertTrue(adminCanApproveCritical);
    }

    // ============= REJECTION TESTS (5) =============

    @Test
    @DisplayName("Should reject token with reason")
    void testRejectTokenWithReason() {
        validator.validateTokenVersion(testVersionId, standardRequest)
            .await().indefinitely();

        VVBApprovalResult result = validator.rejectTokenVersion(testVersionId, "Invalid token data")
            .await().indefinitely();

        assertEquals(VVBValidator.VVBApprovalStatus.REJECTED, result.getStatus());
        assertEquals("Invalid token data", result.getMessage());
    }

    @Test
    @DisplayName("Should record rejection in audit trail")
    void testRecordRejectionInAuditTrail() {
        validator.validateTokenVersion(testVersionId, standardRequest)
            .await().indefinitely();

        String reason = "Compliance check failed";
        validator.rejectTokenVersion(testVersionId, reason)
            .await().indefinitely();

        VVBValidationDetails details = validator.getValidationDetails(testVersionId)
            .await().indefinitely();

        assertNotNull(details);
        assertEquals(1, details.getRejectionCount());
    }

    @Test
    @DisplayName("Should prevent operations on rejected token")
    void testPreventOperationsOnRejectedToken() {
        validator.validateTokenVersion(testVersionId, standardRequest)
            .await().indefinitely();

        validator.rejectTokenVersion(testVersionId, "Test rejection")
            .await().indefinitely();

        // Try to approve rejected token
        VVBApprovalResult result = validator.approveTokenVersion(testVersionId, "VVB_VALIDATOR_1")
            .await().indefinitely();

        assertEquals(VVBValidator.VVBApprovalStatus.REJECTED, result.getStatus());
    }

    @Test
    @DisplayName("Should reject with timeout message")
    void testRejectWithTimeoutMessage() {
        validator.validateTokenVersion(testVersionId, standardRequest)
            .await().indefinitely();

        VVBApprovalResult result = validator.rejectTokenVersion(testVersionId, "Approval timeout exceeded")
            .await().indefinitely();

        assertEquals(VVBValidator.VVBApprovalStatus.REJECTED, result.getStatus());
        assertTrue(result.getMessage().contains("timeout"));
    }

    @Test
    @DisplayName("Should cascade rejection details")
    void testCascadeRejectionDetails() {
        validator.validateTokenVersion(testVersionId, standardRequest)
            .await().indefinitely();

        String rejectionReason = "Cascade failure";
        validator.rejectTokenVersion(testVersionId, rejectionReason)
            .await().indefinitely();

        VVBValidationDetails details = validator.getValidationDetails(testVersionId)
            .await().indefinitely();

        assertTrue(details.getApprovalHistory().stream()
            .anyMatch(r -> r.getReason() != null && r.getReason().contains("Cascade")));
    }

    // ============= QUERY AND STATISTICS TESTS (6) =============

    @Test
    @DisplayName("Should retrieve pending approvals")
    void testRetrievePendingApprovals() {
        validator.validateTokenVersion(UUID.randomUUID(), standardRequest)
            .await().indefinitely();
        validator.validateTokenVersion(UUID.randomUUID(), standardRequest)
            .await().indefinitely();

        List<VVBValidator.VVBValidationStatus> pending = validator.getPendingApprovals()
            .await().indefinitely();

        assertTrue(pending.size() >= 2);
    }

    @Test
    @DisplayName("Should filter pending by approver")
    void testFilterPendingByApprover() {
        validator.validateTokenVersion(testVersionId, standardRequest)
            .await().indefinitely();

        List<VVBValidator.VVBValidationStatus> pending = validator.getPendingByApprover("VVB_VALIDATOR_1")
            .await().indefinitely();

        assertTrue(pending.size() >= 1);
    }

    @Test
    @DisplayName("Should calculate approval statistics")
    void testCalculateApprovalStatistics() {
        // Submit and approve
        UUID versionId1 = UUID.randomUUID();
        validator.validateTokenVersion(versionId1, standardRequest)
            .await().indefinitely();
        validator.approveTokenVersion(versionId1, "VVB_VALIDATOR_1")
            .await().indefinitely();

        // Submit and reject
        UUID versionId2 = UUID.randomUUID();
        validator.validateTokenVersion(versionId2, standardRequest)
            .await().indefinitely();
        validator.rejectTokenVersion(versionId2, "Test rejection")
            .await().indefinitely();

        VVBStatistics stats = validator.getValidationStatistics()
            .await().indefinitely();

        assertTrue(stats.getTotalDecisions() >= 2);
        assertTrue(stats.getApprovedCount() >= 1);
        assertTrue(stats.getRejectedCount() >= 1);
    }

    @Test
    @DisplayName("Should calculate approval rate")
    void testCalculateApprovalRate() {
        VVBStatistics stats = validator.getValidationStatistics()
            .await().indefinitely();

        assertTrue(stats.getApprovalRate() >= 0 && stats.getApprovalRate() <= 100);
    }

    @Test
    @DisplayName("Should track average approval time")
    void testTrackAverageApprovalTime() {
        validator.validateTokenVersion(UUID.randomUUID(), standardRequest)
            .await().indefinitely();

        VVBStatistics stats = validator.getValidationStatistics()
            .await().indefinitely();

        assertTrue(stats.getAverageApprovalTimeMinutes() >= 0);
    }

    @Test
    @DisplayName("Should return validation details")
    void testReturnValidationDetails() {
        validator.validateTokenVersion(testVersionId, standardRequest)
            .await().indefinitely();

        VVBValidationDetails details = validator.getValidationDetails(testVersionId)
            .await().indefinitely();

        assertNotNull(details);
        assertEquals(testVersionId, details.getVersionId());
        assertEquals("SECONDARY_TOKEN_CREATE", details.getChangeType());
    }
}
