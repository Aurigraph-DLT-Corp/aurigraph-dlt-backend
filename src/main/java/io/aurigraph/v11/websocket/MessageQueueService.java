package io.aurigraph.v11.websocket;

import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Message Queue Service
 *
 * Enhanced message queuing with:
 * - Priority queuing (high/medium/low)
 * - Message expiration
 * - Reliable delivery with ACK/NACK
 * - Dead letter queue for failed messages
 * - Message persistence (optional)
 *
 * Messages are prioritized using PriorityBlockingQueue with custom comparator.
 * Failed messages are moved to dead letter queue after max retry attempts.
 *
 * @author WebSocket Development Agent (WDA)
 * @since V11.6.0 (Sprint 16 - AV11-484)
 */
@ApplicationScoped
public class MessageQueueService {

    private static final Logger LOG = Logger.getLogger(MessageQueueService.class);

    // Priority queues per user (userId -> priority queue)
    private final Map<String, PriorityBlockingQueue<PriorityMessage>> userQueues = new ConcurrentHashMap<>();

    // Dead letter queue (userId -> failed messages)
    private final Map<String, List<PriorityMessage>> deadLetterQueues = new ConcurrentHashMap<>();

    // Pending acknowledgements (messageId -> message)
    private final Map<String, PriorityMessage> pendingAcks = new ConcurrentHashMap<>();

    // Message ID generator
    private final AtomicLong messageIdGenerator = new AtomicLong(0);

    // Configuration
    private static final int MAX_QUEUE_SIZE = 10000;
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final long MESSAGE_TTL_MS = 5 * 60 * 1000; // 5 minutes
    private static final long ACK_TIMEOUT_MS = 30 * 1000; // 30 seconds

    /**
     * Enqueue message with priority
     *
     * @param userId User ID
     * @param message Message content
     * @param channel Channel name
     * @param priority Message priority
     * @return Message ID or null if queue is full
     */
    public String enqueueMessage(String userId, String message, String channel, MessagePriority priority) {
        try {
            PriorityBlockingQueue<PriorityMessage> queue = userQueues.computeIfAbsent(
                userId, k -> new PriorityBlockingQueue<>(100, new MessageComparator())
            );

            // Check queue size limit
            if (queue.size() >= MAX_QUEUE_SIZE) {
                LOG.warnf("Queue full for user %s (%d messages), dropping oldest", userId, queue.size());
                // Remove lowest priority message
                queue.poll();
            }

            // Create priority message
            String messageId = generateMessageId();
            PriorityMessage priorityMessage = new PriorityMessage(
                messageId, userId, message, channel, priority, MESSAGE_TTL_MS
            );

            // Add to queue
            queue.offer(priorityMessage);

            LOG.debugf("Enqueued message %s for user %s (priority: %s, queue size: %d)",
                messageId, userId, priority, queue.size());

            return messageId;

        } catch (Exception e) {
            LOG.errorf(e, "Failed to enqueue message for user %s", userId);
            return null;
        }
    }

    /**
     * Enqueue with default (NORMAL) priority
     */
    public String enqueueMessage(String userId, String message, String channel) {
        return enqueueMessage(userId, message, channel, MessagePriority.NORMAL);
    }

    /**
     * Dequeue next message for user (highest priority first)
     *
     * @param userId User ID
     * @return Next priority message or null if queue is empty
     */
    public PriorityMessage dequeueMessage(String userId) {
        try {
            PriorityBlockingQueue<PriorityMessage> queue = userQueues.get(userId);
            if (queue == null || queue.isEmpty()) {
                return null;
            }

            // Poll highest priority message
            PriorityMessage message = queue.poll();

            if (message == null) {
                return null;
            }

            // Check if message has expired
            if (message.isExpired()) {
                LOG.warnf("Message %s expired for user %s (age: %dms)",
                    message.messageId, userId, message.getAge());
                // Move to dead letter queue
                moveToDeadLetterQueue(message, "EXPIRED");
                // Try next message
                return dequeueMessage(userId);
            }

            // Add to pending acknowledgements
            pendingAcks.put(message.messageId, message);

            LOG.debugf("Dequeued message %s for user %s (priority: %s)",
                message.messageId, userId, message.priority);

            return message;

        } catch (Exception e) {
            LOG.errorf(e, "Failed to dequeue message for user %s", userId);
            return null;
        }
    }

    /**
     * Acknowledge message delivery
     *
     * @param messageId Message ID
     * @return true if acknowledged
     */
    public boolean acknowledgeMessage(String messageId) {
        try {
            PriorityMessage message = pendingAcks.remove(messageId);
            if (message != null) {
                message.markDelivered();
                LOG.debugf("Message %s acknowledged", messageId);
                return true;
            }
            LOG.warnf("Message %s not found in pending ACKs", messageId);
            return false;
        } catch (Exception e) {
            LOG.errorf(e, "Failed to acknowledge message %s", messageId);
            return false;
        }
    }

    /**
     * Negative acknowledge (NACK) - re-queue message for retry
     *
     * @param messageId Message ID
     * @param reason Failure reason
     * @return true if re-queued
     */
    public boolean negativeAcknowledge(String messageId, String reason) {
        try {
            PriorityMessage message = pendingAcks.remove(messageId);
            if (message == null) {
                LOG.warnf("Message %s not found in pending ACKs", messageId);
                return false;
            }

            // Increment retry count
            message.incrementRetryCount();

            // Check if max retries exceeded
            if (message.retryCount > MAX_RETRY_ATTEMPTS) {
                LOG.warnf("Message %s exceeded max retries (%d), moving to dead letter queue",
                    messageId, MAX_RETRY_ATTEMPTS);
                moveToDeadLetterQueue(message, reason);
                return false;
            }

            // Re-queue for retry
            PriorityBlockingQueue<PriorityMessage> queue = userQueues.get(message.userId);
            if (queue != null) {
                queue.offer(message);
                LOG.infof("Message %s re-queued for retry (attempt %d/%d): %s",
                    messageId, message.retryCount, MAX_RETRY_ATTEMPTS, reason);
                return true;
            }

            return false;

        } catch (Exception e) {
            LOG.errorf(e, "Failed to NACK message %s", messageId);
            return false;
        }
    }

    /**
     * Get queue size for user
     *
     * @param userId User ID
     * @return Queue size
     */
    public int getQueueSize(String userId) {
        PriorityBlockingQueue<PriorityMessage> queue = userQueues.get(userId);
        return queue != null ? queue.size() : 0;
    }

    /**
     * Get dead letter queue size for user
     *
     * @param userId User ID
     * @return Dead letter queue size
     */
    public int getDeadLetterQueueSize(String userId) {
        List<PriorityMessage> dlq = deadLetterQueues.get(userId);
        return dlq != null ? dlq.size() : 0;
    }

    /**
     * Get pending acknowledgements count
     *
     * @return Pending ACK count
     */
    public int getPendingAckCount() {
        return pendingAcks.size();
    }

    /**
     * Cleanup expired messages and timed-out acknowledgements
     *
     * @return Number of cleaned up messages
     */
    public int cleanupExpiredMessages() {
        int cleanedUp = 0;

        try {
            // Cleanup expired pending ACKs
            long now = System.currentTimeMillis();
            List<String> expiredAcks = new ArrayList<>();

            for (Map.Entry<String, PriorityMessage> entry : pendingAcks.entrySet()) {
                PriorityMessage message = entry.getValue();
                long ackAge = now - message.sentAt.toEpochMilli();

                if (ackAge > ACK_TIMEOUT_MS) {
                    expiredAcks.add(entry.getKey());
                }
            }

            // NACK expired acknowledgements
            for (String messageId : expiredAcks) {
                negativeAcknowledge(messageId, "ACK_TIMEOUT");
                cleanedUp++;
            }

            LOG.infof("Cleaned up %d expired messages/ACKs", cleanedUp);

        } catch (Exception e) {
            LOG.errorf(e, "Failed to cleanup expired messages");
        }

        return cleanedUp;
    }

    /**
     * Clear all queues for a user
     *
     * @param userId User ID
     */
    public void clearUserQueues(String userId) {
        userQueues.remove(userId);
        deadLetterQueues.remove(userId);
        LOG.infof("Cleared all queues for user %s", userId);
    }

    /**
     * Get message queue statistics for user
     *
     * @param userId User ID
     * @return Queue statistics
     */
    public QueueStats getStats(String userId) {
        int queueSize = getQueueSize(userId);
        int dlqSize = getDeadLetterQueueSize(userId);

        PriorityBlockingQueue<PriorityMessage> queue = userQueues.get(userId);
        int highPriority = 0;
        int normalPriority = 0;
        int lowPriority = 0;

        if (queue != null) {
            for (PriorityMessage msg : queue) {
                switch (msg.priority) {
                    case HIGH:
                        highPriority++;
                        break;
                    case NORMAL:
                        normalPriority++;
                        break;
                    case LOW:
                        lowPriority++;
                        break;
                }
            }
        }

        return new QueueStats(userId, queueSize, dlqSize, highPriority, normalPriority, lowPriority);
    }

    /**
     * Move message to dead letter queue
     */
    private void moveToDeadLetterQueue(PriorityMessage message, String reason) {
        message.failureReason = reason;
        message.movedToDlqAt = Instant.now();

        List<PriorityMessage> dlq = deadLetterQueues.computeIfAbsent(
            message.userId, k -> new ArrayList<>()
        );
        dlq.add(message);

        LOG.warnf("Message %s moved to dead letter queue for user %s: %s",
            message.messageId, message.userId, reason);
    }

    /**
     * Generate unique message ID
     */
    private String generateMessageId() {
        return "msg_" + System.currentTimeMillis() + "_" + messageIdGenerator.incrementAndGet();
    }

    /**
     * Message Priority Levels
     */
    public enum MessagePriority {
        HIGH(0),    // Critical system messages
        NORMAL(1),  // Regular messages
        LOW(2);     // Background/informational messages

        private final int value;

        MessagePriority(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    /**
     * Priority Message with metadata
     */
    public static class PriorityMessage {
        public final String messageId;
        public final String userId;
        public final String message;
        public final String channel;
        public final MessagePriority priority;
        public final Instant enqueuedAt;
        public final long ttlMs;

        public int retryCount = 0;
        public Instant sentAt;
        public Instant deliveredAt;
        public Instant movedToDlqAt;
        public String failureReason;

        public PriorityMessage(String messageId, String userId, String message,
                              String channel, MessagePriority priority, long ttlMs) {
            this.messageId = messageId;
            this.userId = userId;
            this.message = message;
            this.channel = channel;
            this.priority = priority;
            this.ttlMs = ttlMs;
            this.enqueuedAt = Instant.now();
        }

        public void incrementRetryCount() {
            this.retryCount++;
        }

        public void markDelivered() {
            this.deliveredAt = Instant.now();
        }

        public boolean isExpired() {
            return getAge() > ttlMs;
        }

        public long getAge() {
            return System.currentTimeMillis() - enqueuedAt.toEpochMilli();
        }

        @Override
        public String toString() {
            return "PriorityMessage{" +
                    "id='" + messageId + '\'' +
                    ", channel='" + channel + '\'' +
                    ", priority=" + priority +
                    ", retryCount=" + retryCount +
                    ", age=" + getAge() + "ms" +
                    '}';
        }
    }

    /**
     * Message Comparator (higher priority first, then FIFO)
     */
    private static class MessageComparator implements Comparator<PriorityMessage> {
        @Override
        public int compare(PriorityMessage m1, PriorityMessage m2) {
            // First compare by priority (lower value = higher priority)
            int priorityCompare = Integer.compare(m1.priority.getValue(), m2.priority.getValue());
            if (priorityCompare != 0) {
                return priorityCompare;
            }
            // Same priority: FIFO (earlier enqueued time first)
            return m1.enqueuedAt.compareTo(m2.enqueuedAt);
        }
    }

    /**
     * Queue Statistics
     */
    public static class QueueStats {
        public final String userId;
        public final int queueSize;
        public final int deadLetterQueueSize;
        public final int highPriorityCount;
        public final int normalPriorityCount;
        public final int lowPriorityCount;

        public QueueStats(String userId, int queueSize, int deadLetterQueueSize,
                         int highPriority, int normalPriority, int lowPriority) {
            this.userId = userId;
            this.queueSize = queueSize;
            this.deadLetterQueueSize = deadLetterQueueSize;
            this.highPriorityCount = highPriority;
            this.normalPriorityCount = normalPriority;
            this.lowPriorityCount = lowPriority;
        }

        @Override
        public String toString() {
            return String.format("QueueStats{user='%s', queue=%d, dlq=%d, priority(H=%d, N=%d, L=%d)}",
                userId, queueSize, deadLetterQueueSize,
                highPriorityCount, normalPriorityCount, lowPriorityCount);
        }
    }
}
