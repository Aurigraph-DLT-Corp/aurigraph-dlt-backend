package io.aurigraph.v11.websocket;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * WebSocket Subscription Service Tests
 *
 * Tests for subscription management with database persistence:
 * - Subscription creation and deletion
 * - Rate limiting
 * - Subscription limits per user
 * - Pause/resume functionality
 * - Statistics and cleanup
 *
 * @author WebSocket Development Agent (WDA)
 * @since V11.6.0 (Sprint 16 - AV11-484)
 */
@QuarkusTest
public class WebSocketSubscriptionServiceTest {

    @Inject
    WebSocketSubscriptionService subscriptionService;

    private static final String TEST_USER_ID = "test-user-123";
    private static final String TEST_CHANNEL = "test-channel";

    @BeforeEach
    @Transactional
    public void setUp() {
        // Cleanup any existing test data
        subscriptionService.deleteAll();
    }

    @AfterEach
    @Transactional
    public void tearDown() {
        // Cleanup test data
        subscriptionService.deleteAll();
    }

    @Test
    @Transactional
    public void testSubscribeUser() {
        // Subscribe user to channel
        WebSocketSubscription subscription = subscriptionService.subscribe(TEST_USER_ID, TEST_CHANNEL, 5);

        assertNotNull(subscription, "Subscription should be created");
        assertEquals(TEST_USER_ID, subscription.userId);
        assertEquals(TEST_CHANNEL, subscription.channel);
        assertEquals(5, subscription.priority);
        assertEquals(WebSocketSubscription.SubscriptionStatus.ACTIVE, subscription.status);
    }

    @Test
    @Transactional
    public void testSubscribeDuplicate() {
        // Subscribe twice to same channel
        WebSocketSubscription sub1 = subscriptionService.subscribe(TEST_USER_ID, TEST_CHANNEL, 5);
        WebSocketSubscription sub2 = subscriptionService.subscribe(TEST_USER_ID, TEST_CHANNEL, 7);

        assertNotNull(sub1);
        assertNotNull(sub2);

        // Should return same subscription (no duplicate)
        assertEquals(sub1.subscriptionId, sub2.subscriptionId);

        // Verify only one subscription exists
        List<WebSocketSubscription> subs = subscriptionService.getActiveSubscriptions(TEST_USER_ID);
        assertEquals(1, subs.size());
    }

    @Test
    @Transactional
    public void testUnsubscribe() {
        // Subscribe and then unsubscribe
        subscriptionService.subscribe(TEST_USER_ID, TEST_CHANNEL, 5);

        boolean result = subscriptionService.unsubscribe(TEST_USER_ID, TEST_CHANNEL);
        assertTrue(result, "Unsubscribe should succeed");

        // Verify subscription is removed
        List<WebSocketSubscription> subs = subscriptionService.getActiveSubscriptions(TEST_USER_ID);
        assertEquals(0, subs.size());
    }

    @Test
    @Transactional
    public void testPauseAndResume() {
        // Create subscription
        subscriptionService.subscribe(TEST_USER_ID, TEST_CHANNEL, 5);

        // Pause subscription
        boolean pauseResult = subscriptionService.pauseSubscription(TEST_USER_ID, TEST_CHANNEL);
        assertTrue(pauseResult, "Pause should succeed");

        // Verify it's paused
        List<WebSocketSubscription> active = subscriptionService.getActiveSubscriptions(TEST_USER_ID);
        assertEquals(0, active.size(), "No active subscriptions after pause");

        // Resume subscription
        boolean resumeResult = subscriptionService.resumeSubscription(TEST_USER_ID, TEST_CHANNEL);
        assertTrue(resumeResult, "Resume should succeed");

        // Verify it's active again
        active = subscriptionService.getActiveSubscriptions(TEST_USER_ID);
        assertEquals(1, active.size(), "One active subscription after resume");
    }

    @Test
    @Transactional
    public void testRateLimit() {
        // Create subscription
        subscriptionService.subscribe(TEST_USER_ID, TEST_CHANNEL, 5);

        String sessionId = "session-123";

        // Should allow first 100 messages (default rate limit)
        for (int i = 0; i < 100; i++) {
            boolean allowed = subscriptionService.checkRateLimit(sessionId, TEST_USER_ID, TEST_CHANNEL);
            assertTrue(allowed, "Message " + i + " should be allowed");
        }

        // 101st message should be blocked
        boolean blocked = subscriptionService.checkRateLimit(sessionId, TEST_USER_ID, TEST_CHANNEL);
        assertFalse(blocked, "Message over rate limit should be blocked");

        // Subscription should be suspended
        WebSocketSubscription sub = subscriptionService.findByUserIdAndChannel(TEST_USER_ID, TEST_CHANNEL).orElse(null);
        assertNotNull(sub);
        assertEquals(WebSocketSubscription.SubscriptionStatus.SUSPENDED, sub.status);
    }

    @Test
    @Transactional
    public void testSubscriptionLimit() {
        // Try to create 51 subscriptions (limit is 50)
        for (int i = 0; i < 51; i++) {
            WebSocketSubscription sub = subscriptionService.subscribe(TEST_USER_ID, "channel-" + i, 5);
            if (i < 50) {
                assertNotNull(sub, "Subscription " + i + " should be created");
            } else {
                assertNull(sub, "Subscription 51 should be blocked (limit exceeded)");
            }
        }

        // Verify only 50 subscriptions exist
        List<WebSocketSubscription> subs = subscriptionService.getActiveSubscriptions(TEST_USER_ID);
        assertEquals(50, subs.size());
    }

    @Test
    @Transactional
    public void testRecordMessageDelivery() {
        // Create subscription
        subscriptionService.subscribe(TEST_USER_ID, TEST_CHANNEL, 5);

        // Record message deliveries
        for (int i = 0; i < 10; i++) {
            subscriptionService.recordMessageDelivery(TEST_USER_ID, TEST_CHANNEL);
        }

        // Verify message count
        WebSocketSubscription sub = subscriptionService.findByUserIdAndChannel(TEST_USER_ID, TEST_CHANNEL).orElse(null);
        assertNotNull(sub);
        assertEquals(10, sub.messageCount);
        assertNotNull(sub.lastMessageAt);
    }

    @Test
    @Transactional
    public void testGetStats() {
        // Create multiple subscriptions with different states
        subscriptionService.subscribe(TEST_USER_ID, "channel-1", 5);
        subscriptionService.subscribe(TEST_USER_ID, "channel-2", 5);
        subscriptionService.subscribe(TEST_USER_ID, "channel-3", 5);

        // Pause one
        subscriptionService.pauseSubscription(TEST_USER_ID, "channel-2");

        // Get stats
        WebSocketSubscriptionService.SubscriptionStats stats = subscriptionService.getStats(TEST_USER_ID);

        assertEquals(3, stats.totalSubscriptions);
        assertEquals(2, stats.activeSubscriptions);
        assertEquals(1, stats.pausedSubscriptions);
        assertEquals(0, stats.suspendedSubscriptions);
    }

    @Test
    @Transactional
    public void testCleanupRateLimitTracking() {
        String sessionId = "session-cleanup-test";

        // Trigger rate limit check to create tracking
        subscriptionService.subscribe(TEST_USER_ID, TEST_CHANNEL, 5);
        subscriptionService.checkRateLimit(sessionId, TEST_USER_ID, TEST_CHANNEL);

        // Cleanup
        subscriptionService.cleanupRateLimitTracking(sessionId);

        // Rate limit should reset
        for (int i = 0; i < 100; i++) {
            boolean allowed = subscriptionService.checkRateLimit(sessionId, TEST_USER_ID, TEST_CHANNEL);
            assertTrue(allowed, "Rate limit should be reset after cleanup");
        }
    }

    @Test
    @Transactional
    public void testMultipleUsers() {
        String user1 = "user-1";
        String user2 = "user-2";

        // Subscribe both users to same channel
        subscriptionService.subscribe(user1, TEST_CHANNEL, 5);
        subscriptionService.subscribe(user2, TEST_CHANNEL, 5);

        // Each should have their own subscription
        List<WebSocketSubscription> subs1 = subscriptionService.getActiveSubscriptions(user1);
        List<WebSocketSubscription> subs2 = subscriptionService.getActiveSubscriptions(user2);

        assertEquals(1, subs1.size());
        assertEquals(1, subs2.size());
        assertNotEquals(subs1.get(0).subscriptionId, subs2.get(0).subscriptionId);
    }

    @Test
    public void testGetActiveSubscriptionsEmpty() {
        // User with no subscriptions
        List<WebSocketSubscription> subs = subscriptionService.getActiveSubscriptions("non-existent-user");
        assertNotNull(subs);
        assertEquals(0, subs.size());
    }
}
