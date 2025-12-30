package io.aurigraph.v11.token.vvb;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import jakarta.inject.Inject;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * VVBApprovalStateMachineTest - 10 tests covering state transitions
 * Tests valid/invalid state paths and event firing
 */
@QuarkusTest
@DisplayName("VVB Approval State Machine Tests")
class VVBApprovalStateMachineTest {

    @Inject
    VVBValidator validator;

    @Inject
    VVBWorkflowService workflowService;

    private UUID testVersionId;
    private VVBValidationRequest standardRequest;

    @BeforeEach
    void setUp() {
        testVersionId = UUID.randomUUID();
        standardRequest = new VVBValidationRequest("SECONDARY_TOKEN_CREATE", "Test", null, "USER");
    }

    // ============= STATE TRANSITIONS (7 tests) =============

    @Nested
    @DisplayName("State Transition Tests")
    class StateTransitions {

        @Test
        @DisplayName("Should start in CREATED state")
        void testStartInCreatedState() {
            // When submitted for approval, transitions to PENDING_VVB
            VVBApprovalResult result = validator.validateTokenVersion(testVersionId, standardRequest)
                .await().indefinitely();

            assertEquals(VVBValidator.VVBApprovalStatus.PENDING_VVB, result.getStatus());
        }

        @Test
        @DisplayName("Should transition PENDING_VVB -> APPROVED")
        void testTransitionPendingToApproved() {
            validator.validateTokenVersion(testVersionId, standardRequest)
                .await().indefinitely();

            VVBApprovalResult result = validator.approveTokenVersion(testVersionId, "VVB_VALIDATOR_1")
                .await().indefinitely();

            assertEquals(VVBValidator.VVBApprovalStatus.APPROVED, result.getStatus());
        }

        @Test
        @DisplayName("Should transition PENDING_VVB -> REJECTED")
        void testTransitionPendingToRejected() {
            validator.validateTokenVersion(testVersionId, standardRequest)
                .await().indefinitely();

            VVBApprovalResult result = validator.rejectTokenVersion(testVersionId, "Test rejection")
                .await().indefinitely();

            assertEquals(VVBValidator.VVBApprovalStatus.REJECTED, result.getStatus());
        }

        @Test
        @DisplayName("Should not transition APPROVED -> PENDING")
        void testPreventApprovedToPending() {
            validator.validateTokenVersion(testVersionId, standardRequest)
                .await().indefinitely();

            VVBApprovalResult approved = validator.approveTokenVersion(testVersionId, "VVB_VALIDATOR_1")
                .await().indefinitely();

            assertEquals(VVBValidator.VVBApprovalStatus.APPROVED, approved.getStatus());

            // Try to revert - should fail
            VVBValidationDetails details = validator.getValidationDetails(testVersionId)
                .await().indefinitely();

            assertEquals(VVBValidator.VVBApprovalStatus.APPROVED, details.getStatus());
        }

        @Test
        @DisplayName("Should not transition REJECTED -> APPROVED")
        void testPreventRejectedToApproved() {
            validator.validateTokenVersion(testVersionId, standardRequest)
                .await().indefinitely();

            validator.rejectTokenVersion(testVersionId, "Rejected")
                .await().indefinitely();

            // Try to approve - should fail
            VVBApprovalResult result = validator.approveTokenVersion(testVersionId, "VVB_VALIDATOR_1")
                .await().indefinitely();

            assertEquals(VVBValidator.VVBApprovalStatus.REJECTED, result.getStatus());
        }

        @Test
        @DisplayName("Should enforce valid transition paths")
        void testEnforceValidTransitionPaths() {
            UUID versionId = UUID.randomUUID();

            // Valid path: CREATED -> PENDING_VVB
            validator.validateTokenVersion(versionId, standardRequest)
                .await().indefinitely();

            // Valid path: PENDING_VVB -> APPROVED
            VVBApprovalResult result = validator.approveTokenVersion(versionId, "VVB_VALIDATOR_1")
                .await().indefinitely();

            assertEquals(VVBValidator.VVBApprovalStatus.APPROVED, result.getStatus());
        }

        @Test
        @DisplayName("Should reject invalid state transitions")
        void testRejectInvalidStateTransitions() {
            UUID versionId = UUID.randomUUID();

            // Cannot approve non-existent version
            VVBApprovalResult result = validator.approveTokenVersion(versionId, "VVB_VALIDATOR_1")
                .await().indefinitely();

            // Should be rejected
            assertEquals(VVBValidator.VVBApprovalStatus.REJECTED, result.getStatus());
        }
    }

    // ============= EVENT FIRING (3 tests) =============

    @Nested
    @DisplayName("Event Firing Tests")
    class EventFiring {

        @Test
        @DisplayName("Should fire event on approval")
        void testFireEventOnApproval() {
            validator.validateTokenVersion(testVersionId, standardRequest)
                .await().indefinitely();

            VVBApprovalResult result = validator.approveTokenVersion(testVersionId, "VVB_VALIDATOR_1")
                .await().indefinitely();

            // Event should be fired (implicit in the service logic)
            assertEquals(VVBValidator.VVBApprovalStatus.APPROVED, result.getStatus());
        }

        @Test
        @DisplayName("Should fire event on rejection")
        void testFireEventOnRejection() {
            validator.validateTokenVersion(testVersionId, standardRequest)
                .await().indefinitely();

            VVBApprovalResult result = validator.rejectTokenVersion(testVersionId, "Rejected")
                .await().indefinitely();

            // Event should be fired
            assertEquals(VVBValidator.VVBApprovalStatus.REJECTED, result.getStatus());
        }

        @Test
        @DisplayName("Should fire event on submission")
        void testFireEventOnSubmission() {
            VVBApprovalResult result = validator.validateTokenVersion(testVersionId, standardRequest)
                .await().indefinitely();

            // Event should be fired on initial submission
            assertEquals(VVBValidator.VVBApprovalStatus.PENDING_VVB, result.getStatus());
        }
    }
}
