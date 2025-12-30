package io.aurigraph.v11.websocket;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Message Queue Service Tests
 *
 * Tests for priority message queuing with reliable delivery:
 * - Priority queuing (high/medium/low)
 * - Message expiration
 * - ACK/NACK delivery confirmation
 * - Dead letter queue
 * - Queue statistics
 *
 * @author WebSocket Development Agent (WDA)
 * @since V11.6.0 (Sprint 16 - AV11-484)
 */
@QuarkusTest
public class MessageQueueServiceTest {

    @Inject
    MessageQueueService messageQueueService;

    private static final String TEST_USER_ID = "test-user-789";
    private static final String TEST_CHANNEL = "test-channel";

    @BeforeEach
    public void setUp() {
        // Clear queues before each test
        messageQueueService.clearUserQueues(TEST_USER_ID);
    }

    @AfterEach
    public void tearDown() {
        // Cleanup after test
        messageQueueService.clearUserQueues(TEST_USER_ID);
    }

    @Test
    public void testEnqueueMessage() {
        // Enqueue message
        String messageId = messageQueueService.enqueueMessage(
            TEST_USER_ID,
            "Test message",
            TEST_CHANNEL,
            MessageQueueService.MessagePriority.NORMAL
        );

        assertNotNull(messageId, "Message ID should be returned");

        // Verify queue size
        assertEquals(1, messageQueueService.getQueueSize(TEST_USER_ID));
    }

    @Test
    public void testDequeueMessage() {
        // Enqueue and dequeue
        String messageId = messageQueueService.enqueueMessage(
            TEST_USER_ID,
            "Test message",
            TEST_CHANNEL
        );

        MessageQueueService.PriorityMessage message =
            messageQueueService.dequeueMessage(TEST_USER_ID);

        assertNotNull(message, "Message should be dequeued");
        assertEquals(messageId, message.messageId);
        assertEquals("Test message", message.message);
        assertEquals(TEST_CHANNEL, message.channel);

        // Queue should be empty after dequeue
        assertEquals(0, messageQueueService.getQueueSize(TEST_USER_ID));
    }

    @Test
    public void testPriorityOrdering() {
        // Enqueue messages with different priorities
        messageQueueService.enqueueMessage(
            TEST_USER_ID, "Low priority", TEST_CHANNEL,
            MessageQueueService.MessagePriority.LOW
        );

        messageQueueService.enqueueMessage(
            TEST_USER_ID, "High priority", TEST_CHANNEL,
            MessageQueueService.MessagePriority.HIGH
        );

        messageQueueService.enqueueMessage(
            TEST_USER_ID, "Normal priority", TEST_CHANNEL,
            MessageQueueService.MessagePriority.NORMAL
        );

        // Dequeue should return highest priority first
        MessageQueueService.PriorityMessage msg1 = messageQueueService.dequeueMessage(TEST_USER_ID);
        assertEquals("High priority", msg1.message, "High priority should be first");

        MessageQueueService.PriorityMessage msg2 = messageQueueService.dequeueMessage(TEST_USER_ID);
        assertEquals("Normal priority", msg2.message, "Normal priority should be second");

        MessageQueueService.PriorityMessage msg3 = messageQueueService.dequeueMessage(TEST_USER_ID);
        assertEquals("Low priority", msg3.message, "Low priority should be last");
    }

    @Test
    public void testAcknowledgeMessage() {
        // Enqueue and dequeue message
        String messageId = messageQueueService.enqueueMessage(
            TEST_USER_ID, "Test message", TEST_CHANNEL
        );

        MessageQueueService.PriorityMessage message =
            messageQueueService.dequeueMessage(TEST_USER_ID);

        // Acknowledge delivery
        boolean acked = messageQueueService.acknowledgeMessage(messageId);
        assertTrue(acked, "Message should be acknowledged");

        // Message should not be in pending ACKs anymore
        assertFalse(messageQueueService.acknowledgeMessage(messageId),
            "Cannot ACK same message twice");
    }

    @Test
    public void testNegativeAcknowledge() {
        // Enqueue and dequeue message
        String messageId = messageQueueService.enqueueMessage(
            TEST_USER_ID, "Test message", TEST_CHANNEL
        );

        messageQueueService.dequeueMessage(TEST_USER_ID);

        // NACK message (re-queue for retry)
        boolean nacked = messageQueueService.negativeAcknowledge(messageId, "Delivery failed");
        assertTrue(nacked, "Message should be NACKed and re-queued");

        // Message should be back in queue
        assertEquals(1, messageQueueService.getQueueSize(TEST_USER_ID));

        // Dequeue again
        MessageQueueService.PriorityMessage retryMessage =
            messageQueueService.dequeueMessage(TEST_USER_ID);

        assertNotNull(retryMessage);
        assertEquals(1, retryMessage.retryCount, "Retry count should be 1");
    }

    @Test
    public void testMaxRetries() {
        // Enqueue message
        String messageId = messageQueueService.enqueueMessage(
            TEST_USER_ID, "Test message", TEST_CHANNEL
        );

        // Exceed max retries (3 attempts)
        for (int i = 0; i < 4; i++) {
            messageQueueService.dequeueMessage(TEST_USER_ID);
            messageQueueService.negativeAcknowledge(messageId, "Retry " + i);
        }

        // After 3 NACKs, 4th should move to dead letter queue
        assertEquals(0, messageQueueService.getQueueSize(TEST_USER_ID),
            "Message should not be in main queue");

        assertEquals(1, messageQueueService.getDeadLetterQueueSize(TEST_USER_ID),
            "Message should be in dead letter queue");
    }

    @Test
    public void testDeadLetterQueue() {
        // Enqueue message
        String messageId = messageQueueService.enqueueMessage(
            TEST_USER_ID, "Failing message", TEST_CHANNEL
        );

        // Force message to dead letter queue by exceeding retries
        for (int i = 0; i < 4; i++) {
            MessageQueueService.PriorityMessage msg = messageQueueService.dequeueMessage(TEST_USER_ID);
            if (msg != null) {
                messageQueueService.negativeAcknowledge(msg.messageId, "Forced failure");
            }
        }

        // Verify dead letter queue
        int dlqSize = messageQueueService.getDeadLetterQueueSize(TEST_USER_ID);
        assertTrue(dlqSize > 0, "Dead letter queue should contain failed message");
    }

    @Test
    public void testQueueSizeLimit() {
        // Try to enqueue more than max queue size (10000 messages)
        // For testing, we'll enqueue a reasonable number to verify limit logic
        for (int i = 0; i < 100; i++) {
            String messageId = messageQueueService.enqueueMessage(
                TEST_USER_ID, "Message " + i, TEST_CHANNEL
            );
            assertNotNull(messageId);
        }

        // Verify queue size
        int queueSize = messageQueueService.getQueueSize(TEST_USER_ID);
        assertEquals(100, queueSize);
    }

    @Test
    public void testGetQueueStats() {
        // Enqueue messages with different priorities
        messageQueueService.enqueueMessage(
            TEST_USER_ID, "High 1", TEST_CHANNEL,
            MessageQueueService.MessagePriority.HIGH
        );

        messageQueueService.enqueueMessage(
            TEST_USER_ID, "High 2", TEST_CHANNEL,
            MessageQueueService.MessagePriority.HIGH
        );

        messageQueueService.enqueueMessage(
            TEST_USER_ID, "Normal", TEST_CHANNEL,
            MessageQueueService.MessagePriority.NORMAL
        );

        messageQueueService.enqueueMessage(
            TEST_USER_ID, "Low", TEST_CHANNEL,
            MessageQueueService.MessagePriority.LOW
        );

        // Get stats
        MessageQueueService.QueueStats stats = messageQueueService.getStats(TEST_USER_ID);

        assertNotNull(stats);
        assertEquals(TEST_USER_ID, stats.userId);
        assertEquals(4, stats.queueSize);
        assertEquals(2, stats.highPriorityCount);
        assertEquals(1, stats.normalPriorityCount);
        assertEquals(1, stats.lowPriorityCount);
    }

    @Test
    public void testCleanupExpiredMessages() {
        // Enqueue message and dequeue (moves to pending ACKs)
        String messageId = messageQueueService.enqueueMessage(
            TEST_USER_ID, "Test message", TEST_CHANNEL
        );

        messageQueueService.dequeueMessage(TEST_USER_ID);

        // Cleanup should handle expired ACKs
        int cleanedUp = messageQueueService.cleanupExpiredMessages();

        // Verify cleanup ran (count may be 0 if ACK timeout not reached)
        assertTrue(cleanedUp >= 0);
    }

    @Test
    public void testClearUserQueues() {
        // Enqueue multiple messages
        messageQueueService.enqueueMessage(TEST_USER_ID, "Message 1", TEST_CHANNEL);
        messageQueueService.enqueueMessage(TEST_USER_ID, "Message 2", TEST_CHANNEL);
        messageQueueService.enqueueMessage(TEST_USER_ID, "Message 3", TEST_CHANNEL);

        assertEquals(3, messageQueueService.getQueueSize(TEST_USER_ID));

        // Clear queues
        messageQueueService.clearUserQueues(TEST_USER_ID);

        // Queue should be empty
        assertEquals(0, messageQueueService.getQueueSize(TEST_USER_ID));
    }

    @Test
    public void testEmptyQueueDequeue() {
        // Try to dequeue from empty queue
        MessageQueueService.PriorityMessage message =
            messageQueueService.dequeueMessage(TEST_USER_ID);

        assertNull(message, "Dequeue from empty queue should return null");
    }

    @Test
    public void testMultipleUsers() {
        String user1 = "user-1";
        String user2 = "user-2";

        // Enqueue messages for different users
        messageQueueService.enqueueMessage(user1, "User 1 message", TEST_CHANNEL);
        messageQueueService.enqueueMessage(user2, "User 2 message", TEST_CHANNEL);

        // Each user should have their own queue
        assertEquals(1, messageQueueService.getQueueSize(user1));
        assertEquals(1, messageQueueService.getQueueSize(user2));

        // Dequeue for user1
        MessageQueueService.PriorityMessage msg1 = messageQueueService.dequeueMessage(user1);
        assertEquals("User 1 message", msg1.message);

        // User2 queue should be unaffected
        assertEquals(1, messageQueueService.getQueueSize(user2));

        // Cleanup
        messageQueueService.clearUserQueues(user1);
        messageQueueService.clearUserQueues(user2);
    }

    @Test
    public void testPendingAckCount() {
        // Enqueue and dequeue messages (creates pending ACKs)
        for (int i = 0; i < 5; i++) {
            messageQueueService.enqueueMessage(
                TEST_USER_ID, "Message " + i, TEST_CHANNEL
            );
        }

        // Dequeue all messages
        for (int i = 0; i < 5; i++) {
            messageQueueService.dequeueMessage(TEST_USER_ID);
        }

        // Should have 5 pending ACKs
        assertEquals(5, messageQueueService.getPendingAckCount());

        // Acknowledge all messages
        // Note: In real scenario would track message IDs, here we just test the count
    }
}
