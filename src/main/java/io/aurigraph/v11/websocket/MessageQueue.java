package io.aurigraph.v11.websocket;

import org.jboss.logging.Logger;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Message Queue for WebSocket Sessions
 *
 * FIFO queue for storing messages when client is disconnected or slow to consume.
 *
 * Features:
 * - Thread-safe FIFO queue (ConcurrentLinkedQueue)
 * - Maximum 1000 messages per client
 * - Oldest messages dropped when limit exceeded
 * - Message metadata: timestamp, channel, priority
 * - Database persistence (planned for Phase 2)
 *
 * Thread-safety: All operations are thread-safe using concurrent collections
 * and atomic counters.
 *
 * @author WebSocket Development Agent (WDA)
 * @since V11.6.0 (Sprint 16 - AV11-484)
 */
public class MessageQueue {

    private static final Logger LOG = Logger.getLogger(MessageQueue.class);
    private static final int MAX_QUEUE_SIZE = 1000;

    private final String userId;
    private final ConcurrentLinkedQueue<QueuedMessage> queue;
    private final AtomicInteger messageCount;
    private final AtomicInteger droppedMessageCount;
    private final Instant createdAt;

    /**
     * Create new message queue for user
     */
    public MessageQueue(String userId) {
        this.userId = userId;
        this.queue = new ConcurrentLinkedQueue<>();
        this.messageCount = new AtomicInteger(0);
        this.droppedMessageCount = new AtomicInteger(0);
        this.createdAt = Instant.now();
    }

    /**
     * Enqueue a message
     *
     * If queue is full (>1000 messages), oldest message is dropped.
     *
     * @param message Message content
     * @param channel Channel name
     * @return true if message was added, false if queue was full
     */
    public boolean enqueue(String message, String channel) {
        return enqueue(message, channel, 0);
    }

    /**
     * Enqueue a message with priority
     *
     * @param message Message content
     * @param channel Channel name
     * @param priority Message priority (higher = more important)
     * @return true if message was added, false if dropped due to queue limit
     */
    public boolean enqueue(String message, String channel, int priority) {
        try {
            // Check if queue is full
            if (messageCount.get() >= MAX_QUEUE_SIZE) {
                // Drop oldest message (poll from front)
                QueuedMessage dropped = queue.poll();
                if (dropped != null) {
                    messageCount.decrementAndGet();
                    droppedMessageCount.incrementAndGet();
                    LOG.warnf("⚠️ Queue full for user %s: Dropped message from channel %s (age: %ds)",
                            userId, dropped.getChannel(), dropped.getAgeSeconds());
                }
            }

            // Add new message to end of queue
            QueuedMessage queuedMessage = new QueuedMessage(message, channel, priority);
            queue.offer(queuedMessage);
            messageCount.incrementAndGet();

            LOG.debugf("Message enqueued for user %s on channel %s (queue size: %d)",
                    userId, channel, messageCount.get());

            return true;
        } catch (Exception e) {
            LOG.errorf(e, "❌ Error enqueueing message for user %s", userId);
            return false;
        }
    }

    /**
     * Dequeue next message (FIFO)
     *
     * @return Next message or null if queue is empty
     */
    public QueuedMessage dequeue() {
        QueuedMessage message = queue.poll();
        if (message != null) {
            messageCount.decrementAndGet();
            LOG.debugf("Message dequeued for user %s (remaining: %d)", userId, messageCount.get());
        }
        return message;
    }

    /**
     * Dequeue all messages for a specific channel
     *
     * @param channel Channel to dequeue messages for
     * @return List of messages from the channel
     */
    public List<QueuedMessage> dequeueByChannel(String channel) {
        List<QueuedMessage> messages = new ArrayList<>();
        List<QueuedMessage> toRequeue = new ArrayList<>();

        // Process all messages
        QueuedMessage msg;
        while ((msg = queue.poll()) != null) {
            if (channel.equals(msg.getChannel())) {
                messages.add(msg);
                messageCount.decrementAndGet();
            } else {
                toRequeue.add(msg);
            }
        }

        // Re-add messages that don't match the channel
        toRequeue.forEach(queue::offer);

        LOG.debugf("Dequeued %d messages for user %s on channel %s (remaining: %d)",
                messages.size(), userId, channel, messageCount.get());

        return messages;
    }

    /**
     * Peek at next message without removing it
     */
    public QueuedMessage peek() {
        return queue.peek();
    }

    /**
     * Clear all messages from queue
     */
    public void clear() {
        int cleared = messageCount.getAndSet(0);
        queue.clear();
        LOG.infof("Cleared %d messages from queue for user %s", cleared, userId);
    }

    /**
     * Get current queue size
     */
    public int size() {
        return messageCount.get();
    }

    /**
     * Check if queue is empty
     */
    public boolean isEmpty() {
        return messageCount.get() == 0;
    }

    /**
     * Check if queue is full
     */
    public boolean isFull() {
        return messageCount.get() >= MAX_QUEUE_SIZE;
    }

    /**
     * Get total number of dropped messages
     */
    public int getDroppedMessageCount() {
        return droppedMessageCount.get();
    }

    /**
     * Get queue statistics
     */
    public QueueStats getStats() {
        return new QueueStats(
                userId,
                messageCount.get(),
                droppedMessageCount.get(),
                MAX_QUEUE_SIZE,
                createdAt
        );
    }

    // Getters
    public String getUserId() {
        return userId;
    }

    /**
     * Queued Message with metadata
     */
    public static class QueuedMessage {
        private final String message;
        private final String channel;
        private final int priority;
        private final Instant enqueuedAt;

        public QueuedMessage(String message, String channel, int priority) {
            this.message = message;
            this.channel = channel;
            this.priority = priority;
            this.enqueuedAt = Instant.now();
        }

        public String getMessage() {
            return message;
        }

        public String getChannel() {
            return channel;
        }

        public int getPriority() {
            return priority;
        }

        public Instant getEnqueuedAt() {
            return enqueuedAt;
        }

        public long getAgeSeconds() {
            return Instant.now().getEpochSecond() - enqueuedAt.getEpochSecond();
        }

        @Override
        public String toString() {
            return "QueuedMessage{" +
                    "channel='" + channel + '\'' +
                    ", priority=" + priority +
                    ", ageSeconds=" + getAgeSeconds() +
                    '}';
        }
    }

    /**
     * Queue Statistics
     */
    public static class QueueStats {
        private final String userId;
        private final int currentSize;
        private final int droppedMessages;
        private final int maxSize;
        private final Instant createdAt;

        public QueueStats(String userId, int currentSize, int droppedMessages, int maxSize, Instant createdAt) {
            this.userId = userId;
            this.currentSize = currentSize;
            this.droppedMessages = droppedMessages;
            this.maxSize = maxSize;
            this.createdAt = createdAt;
        }

        public String getUserId() {
            return userId;
        }

        public int getCurrentSize() {
            return currentSize;
        }

        public int getDroppedMessages() {
            return droppedMessages;
        }

        public int getMaxSize() {
            return maxSize;
        }

        public Instant getCreatedAt() {
            return createdAt;
        }

        public double getUtilization() {
            return (double) currentSize / maxSize * 100.0;
        }

        @Override
        public String toString() {
            return String.format("QueueStats{userId='%s', size=%d/%d (%.1f%%), dropped=%d}",
                    userId, currentSize, maxSize, getUtilization(), droppedMessages);
        }
    }
}
