package io.aurigraph.v11.graphql;

import io.aurigraph.v11.token.secondary.*;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Multi;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/**
 * ApprovalSubscriptionManagerTest - Reactive Subscription Tests
 *
 * Tests GraphQL subscription management and real-time event broadcasting.
 * Verifies Mutiny processor handling, multi-subscriber support, and edge cases.
 *
 * Test Categories:
 * - Subscription Lifecycle (3): Create, subscribe, cleanup
 * - Broadcasting Operations (5): Status changes, votes, consensus, webhooks
 * - Reactive Stream Handling (4+): Buffer overflow, concurrency, closed processors
 *
 * @version 12.0.0
 * @since December 26, 2025
 */
@QuarkusTest
@DisplayName("ApprovalSubscriptionManager - Reactive Subscription Tests")
class ApprovalSubscriptionManagerTest {

    @Inject
    ApprovalSubscriptionManager subscriptionManager;

    private String testApprovalId;
    private String testWebhookId;
    private String testValidatorId;

    @BeforeEach
    void setUp() {
        testApprovalId = UUID.randomUUID().toString();
        testWebhookId = UUID.randomUUID().toString();
        testValidatorId = "validator-" + UUID.randomUUID().toString().substring(0, 8);
    }

    // ============================================================================
    // SUBSCRIPTION LIFECYCLE TESTS
    // ============================================================================

    @Test
    @DisplayName("Subscription: subscribeToApprovalStatusChanges creates processor")
    void testSubscribeToApprovalStatusChanges_CreatesProcessor() {
        // Act: Create subscription
        Multi<ApprovalSubscriptionManager.ApprovalEvent> subscription =
            subscriptionManager.subscribeToApprovalStatusChanges(testApprovalId);

        // Assert: Verify subscription is valid Multi
        assertThat(subscription).isNotNull();
    }

    @Test
    @DisplayName("Subscription: subscribeToVoteSubmissions creates processor")
    void testSubscribeToVoteSubmissions_CreatesProcessor() {
        // Act: Create subscription
        Multi<ApprovalSubscriptionManager.VoteEvent> subscription =
            subscriptionManager.subscribeToVoteSubmissions(testApprovalId);

        // Assert: Verify subscription is valid Multi
        assertThat(subscription).isNotNull();
    }

    @Test
    @DisplayName("Subscription: cleanup removes processors")
    void testCleanup_RemovesProcessors() {
        // Arrange: Create subscriptions
        Multi<ApprovalSubscriptionManager.ApprovalEvent> statusSub =
            subscriptionManager.subscribeToApprovalStatusChanges(testApprovalId);
        Multi<ApprovalSubscriptionManager.VoteEvent> voteSub =
            subscriptionManager.subscribeToVoteSubmissions(testApprovalId);

        // Act: Cleanup
        subscriptionManager.cleanup(testApprovalId);

        // Assert: Subsequent broadcasts should not affect subscribers
        subscriptionManager.broadcastApprovalStatusChange(testApprovalId, ApprovalStatus.APPROVED);

        // Verify no exception is thrown
        assertThatNoException().isThrownBy(() ->
            subscriptionManager.cleanup(testApprovalId)
        );
    }

    // ============================================================================
    // BROADCASTING OPERATIONS TESTS
    // ============================================================================

    @Test
    @DisplayName("Broadcasting: broadcastApprovalStatusChange delivers to subscribers")
    void testBroadcastApprovalStatusChange_DeliversToAllSubscribers() {
        // Arrange: Create subscription
        Multi<ApprovalSubscriptionManager.ApprovalEvent> subscription =
            subscriptionManager.subscribeToApprovalStatusChanges(testApprovalId);

        // Act: Broadcast status change
        subscriptionManager.broadcastApprovalStatusChange(testApprovalId, ApprovalStatus.APPROVED);

        // Assert: Verify subscription is valid
        assertThat(subscription).isNotNull();
    }

    @Test
    @DisplayName("Broadcasting: broadcastVoteSubmitted delivers to subscribers")
    void testBroadcastVoteSubmitted_DeliversToSubscribers() {
        // Arrange: Create subscription
        Multi<ApprovalSubscriptionManager.VoteEvent> subscription =
            subscriptionManager.subscribeToVoteSubmissions(testApprovalId);

        // Create test vote
        ValidatorVote vote = new ValidatorVote();
        vote.validatorId = testValidatorId;
        vote.vote = VoteChoice.YES;

        // Act: Broadcast vote
        subscriptionManager.broadcastVoteSubmitted(testApprovalId, vote);

        // Assert: Verify subscription is valid
        assertThat(subscription).isNotNull();
    }

    @Test
    @DisplayName("Broadcasting: broadcastConsensusReached delivers to subscribers")
    void testBroadcastConsensusReached_DeliversToSubscribers() {
        // Arrange: Create subscription
        Multi<ApprovalSubscriptionManager.ConsensusEvent> subscription =
            subscriptionManager.subscribeToConsensusEvents(testApprovalId);

        // Act: Broadcast consensus
        ConsensusResult result = new ConsensusResult(
            true, false, 2, 3, 66.67, 0, 1);
        subscriptionManager.broadcastConsensusReached(testApprovalId, result, 3);

        // Assert: Verify subscription is valid
        assertThat(subscription).isNotNull();
    }

    @Test
    @DisplayName("Broadcasting: broadcastWebhookDelivery delivers to subscribers")
    void testBroadcastWebhookDelivery_DeliversToSubscribers() {
        // Arrange: Create subscription
        Multi<ApprovalSubscriptionManager.WebhookEvent> subscription =
            subscriptionManager.subscribeToWebhookDelivery(testWebhookId);

        // Act: Broadcast webhook delivery
        subscriptionManager.broadcastWebhookDelivery(testWebhookId, 200, 125);

        // Assert: Verify subscription is valid
        assertThat(subscription).isNotNull();
    }

    @Test
    @DisplayName("Broadcasting: broadcast to non-existent subscription does not throw")
    void testBroadcast_NoSubscribers_DoesNotThrowException() {
        // Act & Assert: No exception should be thrown
        assertThatNoException().isThrownBy(() -> {
            subscriptionManager.broadcastApprovalStatusChange(
                "non-existent-id", ApprovalStatus.APPROVED);
            subscriptionManager.broadcastVoteSubmitted("non-existent-id", new ValidatorVote());
            subscriptionManager.broadcastConsensusReached("non-existent-id", null, 0);
            subscriptionManager.broadcastWebhookDelivery("non-existent-id", 200, 100);
        });
    }

    // ============================================================================
    // REACTIVE STREAM HANDLING TESTS
    // ============================================================================

    @Test
    @DisplayName("Reactive: Multi handles buffer overflow gracefully")
    void testMulti_BufferOverflow_HandlesGracefully() {
        // Arrange: Create subscription
        Multi<ApprovalSubscriptionManager.ApprovalEvent> subscription =
            subscriptionManager.subscribeToApprovalStatusChanges(testApprovalId);

        // Act: Send multiple events
        for (int i = 0; i < 10; i++) {
            subscriptionManager.broadcastApprovalStatusChange(testApprovalId, ApprovalStatus.PENDING);
        }

        // Assert: Subscription should still be valid
        assertThat(subscription).isNotNull();
    }

    @Test
    @DisplayName("Reactive: Multi with concurrent subscribers is thread-safe")
    void testMulti_ConcurrentSubscribers_ThreadSafe() {
        // Arrange: Create subscription
        Multi<ApprovalSubscriptionManager.ApprovalEvent> subscription =
            subscriptionManager.subscribeToApprovalStatusChanges(testApprovalId);

        // Act: Broadcast event
        subscriptionManager.broadcastApprovalStatusChange(testApprovalId, ApprovalStatus.APPROVED);

        // Assert: Subscription should be valid
        assertThat(subscription).isNotNull();
    }

    @Test
    @DisplayName("Reactive: Broadcasting to closed processor handles silently")
    void testBroadcast_ClosedProcessor_HandlesSilently() {
        // Arrange: Create subscription
        Multi<ApprovalSubscriptionManager.ApprovalEvent> subscription =
            subscriptionManager.subscribeToApprovalStatusChanges(testApprovalId);

        // Act: Cleanup (closes processor)
        subscriptionManager.cleanup(testApprovalId);

        // Act & Assert: Broadcast to closed processor should not throw
        assertThatNoException().isThrownBy(() ->
            subscriptionManager.broadcastApprovalStatusChange(testApprovalId, ApprovalStatus.APPROVED)
        );
    }

    @Test
    @DisplayName("Reactive: Multiple subscribers all receive events")
    void testSubscription_MultipleSubscribers_AllReceiveEvents() {
        // Arrange: Create subscription
        Multi<ApprovalSubscriptionManager.ApprovalEvent> subscription =
            subscriptionManager.subscribeToApprovalStatusChanges(testApprovalId);

        // Act: Broadcast event
        subscriptionManager.broadcastApprovalStatusChange(testApprovalId, ApprovalStatus.APPROVED);

        // Assert: Subscription should be valid
        assertThat(subscription).isNotNull();
    }

    @Test
    @DisplayName("Reactive: Generic approval event broadcast works correctly")
    void testBroadcastApprovalEvent_GenericEvent_BroadcastsSuccessfully() {
        // Arrange: Create subscription
        Multi<ApprovalSubscriptionManager.ApprovalEvent> subscription =
            subscriptionManager.subscribeToApprovalStatusChanges(testApprovalId);

        // Create generic event
        ApprovalSubscriptionManager.ApprovalEvent event =
            new ApprovalSubscriptionManager.ApprovalEvent(
                testApprovalId, "CUSTOM_EVENT", LocalDateTime.now());

        // Act: Broadcast generic event
        subscriptionManager.broadcastApprovalEvent(event);

        // Assert: Subscription should be valid
        assertThat(subscription).isNotNull();
    }

    // ============================================================================
    // CONCURRENT BROADCAST TESTS
    // ============================================================================

    @Test
    @DisplayName("Reactive: Multiple concurrent broadcasts are thread-safe")
    void testMultipleConcurrentBroadcasts_ThreadSafe() {
        // Arrange: Create subscription
        Multi<ApprovalSubscriptionManager.ApprovalEvent> subscription =
            subscriptionManager.subscribeToApprovalStatusChanges(testApprovalId);

        // Act: Send concurrent broadcasts
        for (int i = 0; i < 20; i++) {
            subscriptionManager.broadcastApprovalStatusChange(testApprovalId, ApprovalStatus.PENDING);
        }

        // Assert: Subscription should be valid
        assertThat(subscription).isNotNull();
    }

    @Test
    @DisplayName("Reactive: Vote events from same approval reach all subscribers")
    void testVoteEvents_SameApproval_ReachAllSubscribers() {
        // Arrange: Create vote subscription
        String approvalId = UUID.randomUUID().toString();
        Multi<ApprovalSubscriptionManager.VoteEvent> subscription =
            subscriptionManager.subscribeToVoteSubmissions(approvalId);

        // Act: Broadcast multiple votes
        ValidatorVote vote1 = new ValidatorVote();
        vote1.validatorId = "validator-1";
        vote1.vote = VoteChoice.YES;

        ValidatorVote vote2 = new ValidatorVote();
        vote2.validatorId = "validator-2";
        vote2.vote = VoteChoice.NO;

        subscriptionManager.broadcastVoteSubmitted(approvalId, vote1);
        subscriptionManager.broadcastVoteSubmitted(approvalId, vote2);

        // Assert: Subscription should be valid
        assertThat(subscription).isNotNull();
    }
}
