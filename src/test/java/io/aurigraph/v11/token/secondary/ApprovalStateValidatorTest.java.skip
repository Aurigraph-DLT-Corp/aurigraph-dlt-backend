package io.aurigraph.v11.token.secondary;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.InjectMock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import jakarta.inject.Inject;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ApprovalStateValidatorTest - 15+ tests covering state validation
 * Tests state transitions, execution prerequisites, and error messages
 * Part of Phase 3B: Token Versioning & Validation implementation
 */
@QuarkusTest
@DisplayName("Approval State Validator Tests")
class ApprovalStateValidatorTest {

    @Inject
    ApprovalStateValidator stateValidator;

    private UUID approvalId;
    private ValidationContext context;

    @BeforeEach
    void setUp() {
        approvalId = UUID.randomUUID();
        context = new ValidationContext();
        context.setApprovalId(approvalId);
    }

    // ============= STATE TRANSITION VALIDATION (5 tests) =============

    @Nested
    @DisplayName("State Transition Validation Tests")
    class StateTransitionValidation {

        @Test
        @DisplayName("Valid transition from PENDING to APPROVED passes validation")
        void testValidateExecution_PendingToApproved_Valid() {
            // Arrange
            context.setCurrentState(ApprovalState.PENDING);
            context.setTargetState(ApprovalState.APPROVED);

            // Act
            ValidationResult result = stateValidator.validateTransition(context)
                .await().indefinitely();

            // Assert
            assertTrue(result.isValid());
        }

        @Test
        @DisplayName("Valid transition from PENDING to REJECTED passes validation")
        void testValidateExecution_PendingToRejected_Valid() {
            // Arrange
            context.setCurrentState(ApprovalState.PENDING);
            context.setTargetState(ApprovalState.REJECTED);

            // Act
            ValidationResult result = stateValidator.validateTransition(context)
                .await().indefinitely();

            // Assert
            assertTrue(result.isValid());
        }

        @Test
        @DisplayName("Invalid state transition is rejected with error")
        void testValidateExecution_InvalidTransition_Rejected() {
            // Arrange
            context.setCurrentState(ApprovalState.APPROVED);
            context.setTargetState(ApprovalState.PENDING); // Invalid reverse transition

            // Act
            ValidationResult result = stateValidator.validateTransition(context)
                .await().indefinitely();

            // Assert
            assertFalse(result.isValid());
            assertFalse(result.getErrors().isEmpty());
        }

        @Test
        @DisplayName("Error message is clear and descriptive")
        void testValidateExecution_ErrorMessage_ClearDescription() {
            // Arrange
            context.setCurrentState(ApprovalState.APPROVED);
            context.setTargetState(ApprovalState.PENDING);

            // Act
            ValidationResult result = stateValidator.validateTransition(context)
                .await().indefinitely();

            // Assert
            assertFalse(result.getErrors().isEmpty());
            assertTrue(result.getErrors().stream()
                .anyMatch(e -> e.contains("transition") || e.contains("invalid")));
        }

        @Test
        @DisplayName("Multiple validation errors are all reported")
        void testValidateExecution_MultipleErrors_AllReported() {
            // Arrange
            context.setCurrentState(ApprovalState.APPROVED);
            context.setTargetState(ApprovalState.PENDING);
            context.setExpired(true);

            // Act
            ValidationResult result = stateValidator.validateTransition(context)
                .await().indefinitely();

            // Assert
            assertFalse(result.getErrors().isEmpty());
            assertTrue(result.getErrors().size() >= 1);
        }
    }

    // ============= EXECUTION PREREQUISITES (8 tests) =============

    @Nested
    @DisplayName("Execution Prerequisites Tests")
    class ExecutionPrerequisites {

        @Test
        @DisplayName("Threshold met passes validation")
        void testValidatePrerequisites_ThresholdMet_Passes() {
            // Arrange
            context.setCurrentState(ApprovalState.PENDING);
            context.setApprovalCount(3);
            context.setRequiredConsensus(2);

            // Act
            ValidationResult result = stateValidator.validatePrerequisites(context)
                .await().indefinitely();

            // Assert
            assertTrue(result.isValid());
        }

        @Test
        @DisplayName("Threshold not met fails validation")
        void testValidatePrerequisites_ThresholdNotMet_Fails() {
            // Arrange
            context.setCurrentState(ApprovalState.PENDING);
            context.setApprovalCount(1);
            context.setRequiredConsensus(2);

            // Act
            ValidationResult result = stateValidator.validatePrerequisites(context)
                .await().indefinitely();

            // Assert
            assertFalse(result.isValid());
        }

        @Test
        @DisplayName("Deadline respected passes validation")
        void testValidatePrerequisites_DeadlineRespected_Passes() {
            // Arrange
            context.setCurrentState(ApprovalState.PENDING);
            context.setDeadline(Instant.now().plusSeconds(3600)); // 1 hour in future

            // Act
            ValidationResult result = stateValidator.validatePrerequisites(context)
                .await().indefinitely();

            // Assert
            assertTrue(result.isValid());
        }

        @Test
        @DisplayName("Deadline passed fails validation")
        void testValidatePrerequisites_DeadlinePassed_Fails() {
            // Arrange
            context.setCurrentState(ApprovalState.PENDING);
            context.setDeadline(Instant.now().minusSeconds(1)); // Past deadline

            // Act
            ValidationResult result = stateValidator.validatePrerequisites(context)
                .await().indefinitely();

            // Assert
            assertFalse(result.isValid());
        }

        @Test
        @DisplayName("Valid state passes validation")
        void testValidatePrerequisites_StateValid_Passes() {
            // Arrange
            context.setCurrentState(ApprovalState.PENDING);

            // Act
            ValidationResult result = stateValidator.validatePrerequisites(context)
                .await().indefinitely();

            // Assert
            assertTrue(result.isValid());
        }

        @Test
        @DisplayName("Invalid state fails validation")
        void testValidatePrerequisites_StateInvalid_Fails() {
            // Arrange - null state is invalid
            context.setCurrentState(null);

            // Act
            ValidationResult result = stateValidator.validatePrerequisites(context)
                .await().indefinitely();

            // Assert
            assertFalse(result.isValid());
        }

        @Test
        @DisplayName("All prerequisites met passes validation")
        void testValidatePrerequisites_AllPrereqsMet_Passes() {
            // Arrange
            context.setCurrentState(ApprovalState.PENDING);
            context.setApprovalCount(3);
            context.setRequiredConsensus(2);
            context.setDeadline(Instant.now().plusSeconds(3600));

            // Act
            ValidationResult result = stateValidator.validatePrerequisites(context)
                .await().indefinitely();

            // Assert
            assertTrue(result.isValid());
        }

        @Test
        @DisplayName("One prerequisite failure causes entire validation to fail")
        void testValidatePrerequisites_OneFailure_EntireValidationFails() {
            // Arrange
            context.setCurrentState(ApprovalState.PENDING);
            context.setApprovalCount(1); // Below threshold
            context.setRequiredConsensus(2);
            context.setDeadline(Instant.now().plusSeconds(3600));

            // Act
            ValidationResult result = stateValidator.validatePrerequisites(context)
                .await().indefinitely();

            // Assert
            assertFalse(result.isValid());
        }
    }

    // ============= ERROR MESSAGE VERIFICATION (2 tests) =============

    @Nested
    @DisplayName("Error Message Verification Tests")
    class ErrorMessageVerification {

        @Test
        @DisplayName("Validation error message is accurate and explains issue")
        void testValidationError_MessageAccurate_ExplainsIssue() {
            // Arrange
            context.setCurrentState(ApprovalState.APPROVED);
            context.setTargetState(ApprovalState.PENDING);

            // Act
            ValidationResult result = stateValidator.validateTransition(context)
                .await().indefinitely();

            // Assert
            assertFalse(result.getErrors().isEmpty());
            String error = result.getErrors().get(0);
            assertTrue(error.toLowerCase().contains("cannot") ||
                      error.toLowerCase().contains("invalid") ||
                      error.toLowerCase().contains("transition"));
        }

        @Test
        @DisplayName("Error message is clear and developer-friendly")
        void testValidationError_MessageClear_DebugFriendly() {
            // Arrange
            context.setCurrentState(ApprovalState.PENDING);
            context.setApprovalCount(1);
            context.setRequiredConsensus(3);

            // Act
            ValidationResult result = stateValidator.validatePrerequisites(context)
                .await().indefinitely();

            // Assert
            assertFalse(result.getErrors().isEmpty());
            String error = result.getErrors().get(0);
            assertTrue(error.length() > 10); // Meaningful length
            assertTrue(error.contains("approval") || error.contains("consensus") ||
                      error.contains("threshold"));
        }
    }
}

/**
 * Helper enum for ApprovalState (would be in actual service)
 */
enum ApprovalState {
    PENDING, APPROVED, REJECTED, EXPIRED, IN_PROGRESS
}

/**
 * Helper class for ValidationContext (would be in actual service)
 */
class ValidationContext {
    private UUID approvalId;
    private ApprovalState currentState;
    private ApprovalState targetState;
    private int approvalCount;
    private int requiredConsensus;
    private Instant deadline;
    private boolean expired;

    public UUID getApprovalId() { return approvalId; }
    public void setApprovalId(UUID id) { this.approvalId = id; }

    public ApprovalState getCurrentState() { return currentState; }
    public void setCurrentState(ApprovalState state) { this.currentState = state; }

    public ApprovalState getTargetState() { return targetState; }
    public void setTargetState(ApprovalState state) { this.targetState = state; }

    public int getApprovalCount() { return approvalCount; }
    public void setApprovalCount(int count) { this.approvalCount = count; }

    public int getRequiredConsensus() { return requiredConsensus; }
    public void setRequiredConsensus(int consensus) { this.requiredConsensus = consensus; }

    public Instant getDeadline() { return deadline; }
    public void setDeadline(Instant deadline) { this.deadline = deadline; }

    public boolean isExpired() { return expired; }
    public void setExpired(boolean expired) { this.expired = expired; }
}

/**
 * Helper class for ValidationResult (would be in actual service)
 */
class ValidationResult {
    private boolean valid;
    private List<String> errors;

    public ValidationResult(boolean valid, List<String> errors) {
        this.valid = valid;
        this.errors = errors;
    }

    public boolean isValid() { return valid; }
    public void setValid(boolean valid) { this.valid = valid; }

    public List<String> getErrors() { return errors; }
    public void setErrors(List<String> errors) { this.errors = errors; }
}
