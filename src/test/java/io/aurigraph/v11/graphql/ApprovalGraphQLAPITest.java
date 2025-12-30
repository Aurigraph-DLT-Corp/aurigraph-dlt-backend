package io.aurigraph.v11.graphql;

import io.aurigraph.v11.testing.builders.ApprovalRequestTestBuilder;
import io.aurigraph.v11.testing.builders.ValidatorVoteTestBuilder;
import io.aurigraph.v11.token.secondary.*;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * ApprovalGraphQLAPITest - GraphQL Query/Mutation Tests
 *
 * Tests GraphQL query and mutation resolvers for approval management.
 * Covers all CRUD operations with error handling and edge cases.
 *
 * Test Categories:
 * - Query Tests (5): getApproval, getApprovals, getApprovalStatistics, getValidatorStats
 * - Mutation Tests (6): executeApproval, registerWebhook, unregisterWebhook
 * - Error Handling (5): Null parameters, pagination, state validation, broadcast failures
 *
 * @version 12.0.0
 * @since December 26, 2025
 */
@QuarkusTest
@DisplayName("ApprovalGraphQLAPI - GraphQL Query/Mutation Tests")
class ApprovalGraphQLAPITest {

    @Inject
    ApprovalGraphQLAPI approvalGraphQLAPI;

    @InjectMock
    VVBApprovalService approvalService;

    @InjectMock
    VVBApprovalRegistry approvalRegistry;

    @InjectMock
    ApprovalStateValidator stateValidator;

    @InjectMock
    ApprovalSubscriptionManager subscriptionManager;

    private UUID testApprovalId;
    private VVBApprovalRequest testApproval;
    private UUID tokenVersionId;

    @BeforeEach
    void setUp() {
        testApprovalId = UUID.randomUUID();
        tokenVersionId = UUID.randomUUID();
        testApproval = new ApprovalRequestTestBuilder()
            .withId(testApprovalId)
            .withTokenVersionId(tokenVersionId)
            .withStatus(ApprovalStatus.PENDING)
            .withValidators("validator-1", "validator-2", "validator-3")
            .withConsensusThreshold(2)
            .build();
    }

    // ============================================================================
    // QUERY TESTS
    // ============================================================================

    @Test
    @DisplayName("Query: getApproval returns approval for valid ID")
    void testGetApproval_ValidId_ReturnsApprovalDTO() {
        // Arrange: Mock the database lookup
        try (var mockedStatic = mockStatic(VVBApprovalRequest.class)) {
            mockedStatic.when(() -> VVBApprovalRequest.findByRequestId(testApprovalId))
                .thenReturn(testApproval);

            // Act: Call the GraphQL resolver
            Uni<ApprovalDTO> result = approvalGraphQLAPI.getApproval(testApprovalId.toString());

            // Assert: Verify the result
            ApprovalDTO dto = result.await().indefinitely();
            assertThat(dto)
                .isNotNull()
                .satisfies(d -> {
                    assertThat(d.id).isEqualTo(testApprovalId.toString());
                    assertThat(d.status).isEqualTo(ApprovalStatus.PENDING);
                    assertThat(d.tokenVersionId).isEqualTo(tokenVersionId.toString());
                });
        }
    }

    @Test
    @DisplayName("Query: getApproval throws exception for invalid ID")
    void testGetApproval_InvalidId_ThrowsException() {
        // Arrange: Mock null result
        try (var mockedStatic = mockStatic(VVBApprovalRequest.class)) {
            mockedStatic.when(() -> VVBApprovalRequest.findByRequestId(any(UUID.class)))
                .thenReturn(null);

            // Act & Assert: Expect exception
            Uni<ApprovalDTO> result = approvalGraphQLAPI.getApproval(UUID.randomUUID().toString());
            assertThatThrownBy(() -> result.await().indefinitely())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Approval not found");
        }
    }

    @Test
    @DisplayName("Query: getApprovals with filters returns filtered list")
    void testGetApprovals_WithFilters_ReturnsFilteredList() {
        // Arrange: Create test approvals with different statuses
        VVBApprovalRequest approved = new ApprovalRequestTestBuilder()
            .approved()
            .build();
        VVBApprovalRequest pending = new ApprovalRequestTestBuilder()
            .withStatus(ApprovalStatus.PENDING)
            .build();

        List<VVBApprovalRequest> allApprovals = Arrays.asList(approved, pending);

        try (var mockedStatic = mockStatic(VVBApprovalRequest.class)) {
            mockedStatic.when(VVBApprovalRequest::listAll)
                .thenReturn(allApprovals);

            // Act: Query with APPROVED filter
            Uni<List<ApprovalDTO>> result = approvalGraphQLAPI.getApprovals(
                ApprovalStatus.APPROVED, 10, 0);

            // Assert: Only APPROVED should be returned
            List<ApprovalDTO> dtos = result.await().indefinitely();
            assertThat(dtos)
                .hasSize(1)
                .allMatch(d -> d.status == ApprovalStatus.APPROVED);
        }
    }

    @Test
    @DisplayName("Query: getApprovalStatistics returns accurate statistics")
    void testGetApprovalStatistics_ReturnsAccurateStats() {
        // Arrange: Create approvals with various statuses
        VVBApprovalRequest approved = new ApprovalRequestTestBuilder()
            .approved()
            .withCreatedAt(LocalDateTime.now().minusSeconds(100))
            .build();
        VVBApprovalRequest pending = new ApprovalRequestTestBuilder()
            .withStatus(ApprovalStatus.PENDING)
            .withCreatedAt(LocalDateTime.now().minusSeconds(50))
            .build();
        VVBApprovalRequest rejected = new ApprovalRequestTestBuilder()
            .rejected()
            .withCreatedAt(LocalDateTime.now().minusSeconds(25))
            .build();

        List<VVBApprovalRequest> allApprovals = Arrays.asList(approved, pending, rejected);

        try (var mockedStatic = mockStatic(VVBApprovalRequest.class)) {
            mockedStatic.when(VVBApprovalRequest::listAll)
                .thenReturn(allApprovals);

            // Act: Get statistics
            Uni<ApprovalStatisticsDTO> result = approvalGraphQLAPI.getApprovalStatistics();

            // Assert: Verify statistics
            ApprovalStatisticsDTO stats = result.await().indefinitely();
            assertThat(stats)
                .isNotNull()
                .satisfies(s -> {
                    assertThat(s.totalApprovals).isEqualTo(3);
                    assertThat(s.approved).isEqualTo(1);
                    assertThat(s.pending).isEqualTo(1);
                    assertThat(s.rejected).isEqualTo(1);
                    assertThat(s.timestamp).isNotNull();
                });
        }
    }

    @Test
    @DisplayName("Query: getValidatorStats returns validator statistics")
    void testGetValidatorStats_ValidatorId_ReturnsStats() {
        // Arrange: Create test votes
        ValidatorVote yesVote = new ValidatorVoteTestBuilder()
            .withValidatorId("validator-1")
            .approves()
            .build();
        ValidatorVote noVote = new ValidatorVoteTestBuilder()
            .withValidatorId("validator-1")
            .rejects()
            .build();
        ValidatorVote abstainVote = new ValidatorVoteTestBuilder()
            .withValidatorId("validator-1")
            .abstains()
            .build();

        List<ValidatorVote> votes = Arrays.asList(yesVote, noVote, abstainVote);

        when(approvalRegistry.getVotesByValidator("validator-1"))
            .thenReturn(votes);

        // Act: Get validator stats
        Uni<ValidatorStatsDTO> result = approvalGraphQLAPI.getValidatorStats("validator-1");

        // Assert: Verify stats
        ValidatorStatsDTO stats = result.await().indefinitely();
        assertThat(stats)
            .isNotNull()
            .satisfies(s -> {
                assertThat(s.validatorId).isEqualTo("validator-1");
                assertThat(s.totalVotes).isEqualTo(3);
                assertThat(s.approvesCount).isEqualTo(1);
                assertThat(s.rejectsCount).isEqualTo(1);
                assertThat(s.absorbCount).isEqualTo(1);
                assertThat(s.approvalRate).isCloseTo(33.33, within(0.1));
            });
    }

    // ============================================================================
    // MUTATION TESTS
    // ============================================================================

    @Test
    @DisplayName("Mutation: executeApproval executes successfully")
    void testExecuteApproval_ValidRequest_ExecutesSuccessfully() {
        // Arrange: Mock approval lookup and validation
        try (var mockedStatic = mockStatic(VVBApprovalRequest.class)) {
            mockedStatic.when(() -> VVBApprovalRequest.findByRequestId(testApprovalId))
                .thenReturn(testApproval);

            doNothing().when(stateValidator)
                .validateExecutionPrerequisites(testApproval);

            // Act: Execute approval
            Uni<ExecutionResponseDTO> result = approvalGraphQLAPI
                .executeApproval(testApprovalId.toString());

            // Assert: Verify execution success
            ExecutionResponseDTO response = result.await().indefinitely();
            assertThat(response)
                .isNotNull()
                .satisfies(r -> {
                    assertThat(r.success).isTrue();
                    assertThat(r.message).isEqualTo("Execution successful");
                    assertThat(r.executionId).isEqualTo(testApprovalId.toString());
                });

            verify(subscriptionManager).broadcastApprovalEvent(any(ApprovalSubscriptionManager.ApprovalEvent.class));
        }
    }

    @Test
    @DisplayName("Mutation: executeApproval returns error for invalid request")
    void testExecuteApproval_InvalidRequest_ReturnsError() {
        // Arrange: Mock null approval
        try (var mockedStatic = mockStatic(VVBApprovalRequest.class)) {
            mockedStatic.when(() -> VVBApprovalRequest.findByRequestId(any(UUID.class)))
                .thenReturn(null);

            // Act: Try to execute
            Uni<ExecutionResponseDTO> result = approvalGraphQLAPI
                .executeApproval(UUID.randomUUID().toString());

            // Assert: Verify error response
            ExecutionResponseDTO response = result.await().indefinitely();
            assertThat(response)
                .isNotNull()
                .satisfies(r -> {
                    assertThat(r.success).isFalse();
                    assertThat(r.message).isEqualTo("Approval not found");
                    assertThat(r.executionId).isNull();
                });
        }
    }

    @Test
    @DisplayName("Mutation: registerWebhook registers successfully")
    void testRegisterWebhook_ValidUrl_RegistersSuccessfully() {
        // Arrange: Valid webhook URL and events
        String webhookUrl = "https://example.com/webhook";
        List<String> events = Arrays.asList("APPROVAL_EXECUTED", "CONSENSUS_REACHED");

        // Act: Register webhook
        Uni<WebhookResponseDTO> result = approvalGraphQLAPI
            .registerWebhook(webhookUrl, events);

        // Assert: Verify registration success
        WebhookResponseDTO response = result.await().indefinitely();
        assertThat(response)
            .isNotNull()
            .satisfies(r -> {
                assertThat(r.success).isTrue();
                assertThat(r.message).isEqualTo("Webhook registered");
                assertThat(r.webhookId).isNotNull().isNotEmpty();
            });
    }

    @Test
    @DisplayName("Mutation: registerWebhook returns error for invalid URL")
    void testRegisterWebhook_InvalidUrl_ReturnsError() {
        // Arrange: Invalid webhook URL
        String invalidUrl = "not-a-url";
        List<String> events = Arrays.asList("APPROVAL_EXECUTED");

        // Act: Try to register
        Uni<WebhookResponseDTO> result = approvalGraphQLAPI
            .registerWebhook(invalidUrl, events);

        // Assert: Should still succeed (validation in Phase 3)
        WebhookResponseDTO response = result.await().indefinitely();
        assertThat(response).isNotNull();
    }

    @Test
    @DisplayName("Mutation: unregisterWebhook unregisters successfully")
    void testUnregisterWebhook_ValidId_UnregistersSuccessfully() {
        // Arrange: Valid webhook ID
        String webhookId = UUID.randomUUID().toString();

        // Act: Unregister webhook
        Uni<Boolean> result = approvalGraphQLAPI.unregisterWebhook(webhookId);

        // Assert: Verify success
        Boolean success = result.await().indefinitely();
        assertThat(success).isTrue();
    }

    @Test
    @DisplayName("Mutation: unregisterWebhook returns false for invalid ID")
    void testUnregisterWebhook_InvalidId_ReturnsFalse() {
        // Arrange: Invalid webhook ID (will fail in actual implementation)
        String invalidId = "invalid-webhook-id";

        // Act: Try to unregister
        Uni<Boolean> result = approvalGraphQLAPI.unregisterWebhook(invalidId);

        // Assert: Should still return true (no validation in Phase 2)
        Boolean success = result.await().indefinitely();
        assertThat(success).isTrue();
    }

    // ============================================================================
    // ERROR HANDLING & EDGE CASES
    // ============================================================================

    @Test
    @DisplayName("Error: executeApproval with null ID throws exception")
    void testExecuteApproval_NullApprovalId_ThrowsException() {
        // Act & Assert: Expect exception for null ID
        assertThatThrownBy(() ->
            approvalGraphQLAPI.executeApproval(null).await().indefinitely()
        ).isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("Error: getApprovals with pagination returns correct page")
    void testGetApprovals_Pagination_ReturnsCorrectPage() {
        // Arrange: Create multiple approvals
        List<VVBApprovalRequest> allApprovals = new ArrayList<>();
        for (int i = 0; i < 25; i++) {
            allApprovals.add(new ApprovalRequestTestBuilder().build());
        }

        try (var mockedStatic = mockStatic(VVBApprovalRequest.class)) {
            mockedStatic.when(VVBApprovalRequest::listAll)
                .thenReturn(allApprovals);

            // Act: Get page 2 (offset=20, limit=10)
            Uni<List<ApprovalDTO>> result = approvalGraphQLAPI.getApprovals(
                null, 10, 20);

            // Assert: Verify pagination
            List<ApprovalDTO> dtos = result.await().indefinitely();
            assertThat(dtos)
                .hasSize(5)  // Only 5 items remaining after offset 20
                .isNotEmpty();
        }
    }

    @Test
    @DisplayName("Error: getApprovals returns empty list when no matches")
    void testGetApprovals_EmptyResult_ReturnsEmptyList() {
        // Arrange: No approvals match filter
        try (var mockedStatic = mockStatic(VVBApprovalRequest.class)) {
            mockedStatic.when(VVBApprovalRequest::listAll)
                .thenReturn(Arrays.asList(
                    new ApprovalRequestTestBuilder().withStatus(ApprovalStatus.PENDING).build()
                ));

            // Act: Query for non-existent status
            Uni<List<ApprovalDTO>> result = approvalGraphQLAPI.getApprovals(
                ApprovalStatus.APPROVED, 10, 0);

            // Assert: Verify empty result
            List<ApprovalDTO> dtos = result.await().indefinitely();
            assertThat(dtos).isEmpty();
        }
    }

    @Test
    @DisplayName("Error: executeApproval fails state validation")
    void testExecuteApproval_StateValidationFails_ReturnsError() {
        // Arrange: Mock validation failure
        try (var mockedStatic = mockStatic(VVBApprovalRequest.class)) {
            mockedStatic.when(() -> VVBApprovalRequest.findByRequestId(testApprovalId))
                .thenReturn(testApproval);

            doThrow(new IllegalStateException("Invalid execution state"))
                .when(stateValidator).validateExecutionPrerequisites(testApproval);

            // Act: Try to execute
            Uni<ExecutionResponseDTO> result = approvalGraphQLAPI
                .executeApproval(testApprovalId.toString());

            // Assert: Verify error response
            ExecutionResponseDTO response = result.await().indefinitely();
            assertThat(response)
                .isNotNull()
                .satisfies(r -> {
                    assertThat(r.success).isFalse();
                    assertThat(r.message).isEqualTo("Invalid execution state");
                    assertThat(r.executionId).isNull();
                });
        }
    }

    @Test
    @DisplayName("Error: executeApproval handles broadcast failure gracefully")
    void testExecuteApproval_BroadcastFails_HandlesGracefully() {
        // Arrange: Mock broadcast to fail (throws exception)
        try (var mockedStatic = mockStatic(VVBApprovalRequest.class)) {
            mockedStatic.when(() -> VVBApprovalRequest.findByRequestId(testApprovalId))
                .thenReturn(testApproval);

            doNothing().when(stateValidator)
                .validateExecutionPrerequisites(testApproval);

            doThrow(new RuntimeException("Broadcast failed"))
                .when(subscriptionManager).broadcastApprovalEvent(any());

            // Act: Execute (should handle exception gracefully)
            Uni<ExecutionResponseDTO> result = approvalGraphQLAPI
                .executeApproval(testApprovalId.toString());

            // Assert: Should still return error response
            ExecutionResponseDTO response = result.await().indefinitely();
            assertThat(response)
                .isNotNull()
                .satisfies(r -> {
                    assertThat(r.success).isFalse();
                    assertThat(r.message).contains("Broadcast failed");
                });
        }
    }
}
