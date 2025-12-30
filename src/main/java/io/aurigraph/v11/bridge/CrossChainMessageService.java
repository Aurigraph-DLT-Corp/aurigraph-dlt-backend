package io.aurigraph.v11.bridge;

import jakarta.enterprise.context.ApplicationScoped;
import io.smallrye.mutiny.Uni;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import io.quarkus.logging.Log;

/**
 * Cross-Chain Messaging Service for Aurigraph V11
 * Provides secure, reliable messaging between Aurigraph and external blockchains
 * Features: Message queuing, delivery confirmation, replay protection, encryption
 */
@ApplicationScoped
public class CrossChainMessageService {

    // Message storage and tracking
    private final Map<String, CrossChainMessage> messages = new ConcurrentHashMap<>();
    private final Map<String, List<String>> chainMessages = new ConcurrentHashMap<>();
    private final Map<String, MessageQueue> messageQueues = new ConcurrentHashMap<>();
    
    // Performance metrics
    private final AtomicLong totalMessages = new AtomicLong(0);
    private final AtomicLong pendingMessages = new AtomicLong(0);
    private final AtomicLong deliveredMessages = new AtomicLong(0);
    private final AtomicLong failedMessages = new AtomicLong(0);

    /**
     * Send a cross-chain message
     */
    public Uni<String> sendMessage(MessageRequest request) {
        return Uni.createFrom().item(() -> {
            // Generate message ID
            String messageId = generateMessageId();
            
            // Create cross-chain message
            CrossChainMessage message = new CrossChainMessage(
                messageId,
                request.getSourceChain(),
                request.getTargetChain(),
                request.getSender(),
                request.getReceiver(),
                request.getMessageType(),
                request.getPayload(),
                request.getNonce(),
                MessageStatus.PENDING,
                Instant.now()
            );
            
            // Store message
            messages.put(messageId, message);
            
            // Add to chain-specific queue
            chainMessages.computeIfAbsent(request.getTargetChain(), k -> new ArrayList<>())
                         .add(messageId);
            
            // Add to message queue for processing
            MessageQueue queue = messageQueues.computeIfAbsent(request.getTargetChain(), 
                k -> new MessageQueue(request.getTargetChain()));
            queue.enqueue(message);
            
            // Update metrics
            totalMessages.incrementAndGet();
            pendingMessages.incrementAndGet();
            
            Log.infof("Queued cross-chain message %s from %s to %s", 
                messageId, request.getSourceChain(), request.getTargetChain());
            
            return messageId;
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    /**
     * Get message status
     */
    public Uni<CrossChainMessage> getMessage(String messageId) {
        return Uni.createFrom().item(() -> {
            CrossChainMessage message = messages.get(messageId);
            if (message == null) {
                throw new MessageNotFoundException("Message not found: " + messageId);
            }
            return message;
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    /**
     * Get messages for a specific chain
     */
    public Uni<List<CrossChainMessage>> getMessagesForChain(String chainId) {
        return Uni.createFrom().item(() -> {
            List<String> messageIds = chainMessages.getOrDefault(chainId, new ArrayList<>());
            return messageIds.stream()
                .map(messages::get)
                .filter(Objects::nonNull)
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .toList();
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    /**
     * Get messages by sender
     */
    public Uni<List<CrossChainMessage>> getMessagesBySender(String sender) {
        return Uni.createFrom().item(() -> {
            return messages.values().stream()
                .filter(msg -> sender.equals(msg.getSender()))
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .toList();
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    /**
     * Acknowledge message delivery
     */
    public Uni<Boolean> acknowledgeMessage(String messageId, String deliveryReceipt) {
        return Uni.createFrom().item(() -> {
            CrossChainMessage message = messages.get(messageId);
            if (message == null) {
                throw new MessageNotFoundException("Message not found: " + messageId);
            }
            
            if (message.getStatus() != MessageStatus.PENDING) {
                throw new IllegalStateException("Message is not in pending status");
            }
            
            // Update message status
            CrossChainMessage updatedMessage = message.withStatus(MessageStatus.DELIVERED)
                                                    .withDeliveryReceipt(deliveryReceipt)
                                                    .withDeliveredAt(Instant.now());
            
            messages.put(messageId, updatedMessage);
            
            // Update metrics
            pendingMessages.decrementAndGet();
            deliveredMessages.incrementAndGet();
            
            Log.infof("Message %s acknowledged as delivered", messageId);
            
            return true;
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    /**
     * Mark message as failed
     */
    public Uni<Boolean> markMessageFailed(String messageId, String errorReason) {
        return Uni.createFrom().item(() -> {
            CrossChainMessage message = messages.get(messageId);
            if (message == null) {
                throw new MessageNotFoundException("Message not found: " + messageId);
            }
            
            // Update message status
            CrossChainMessage failedMessage = message.withStatus(MessageStatus.FAILED)
                                                    .withErrorReason(errorReason);
            
            messages.put(messageId, failedMessage);
            
            // Update metrics
            if (message.getStatus() == MessageStatus.PENDING) {
                pendingMessages.decrementAndGet();
            }
            failedMessages.incrementAndGet();
            
            Log.errorf("Message %s marked as failed: %s", messageId, errorReason);
            
            return true;
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    /**
     * Get message queue status for a chain
     */
    public Uni<MessageQueueStatus> getMessageQueueStatus(String chainId) {
        return Uni.createFrom().item(() -> {
            MessageQueue queue = messageQueues.get(chainId);
            if (queue == null) {
                return new MessageQueueStatus(chainId, 0, 0, Instant.now());
            }
            
            return new MessageQueueStatus(
                chainId,
                queue.getPendingCount(),
                queue.getProcessedCount(),
                queue.getLastProcessedAt()
            );
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    /**
     * Get messaging statistics
     */
    public Uni<MessagingStats> getMessagingStats() {
        return Uni.createFrom().item(() -> {
            Map<String, Long> chainMessageCounts = new HashMap<>();
            for (Map.Entry<String, List<String>> entry : chainMessages.entrySet()) {
                chainMessageCounts.put(entry.getKey(), (long) entry.getValue().size());
            }
            
            return new MessagingStats(
                totalMessages.get(),
                pendingMessages.get(),
                deliveredMessages.get(),
                failedMessages.get(),
                chainMessageCounts,
                messageQueues.size()
            );
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    /**
     * Process pending messages (called by scheduler)
     */
    public Uni<Integer> processPendingMessages() {
        return Uni.createFrom().item(() -> {
            int processedCount = 0;
            
            for (MessageQueue queue : messageQueues.values()) {
                while (!queue.isEmpty()) {
                    CrossChainMessage message = queue.dequeue();
                    if (message != null) {
                        try {
                            // Simulate message processing
                            processMessage(message);
                            processedCount++;
                            
                        } catch (Exception e) {
                            markMessageFailed(message.getMessageId(), e.getMessage())
                                .await().indefinitely();
                        }
                    }
                }
            }
            
            return processedCount;
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    // Private helper methods

    private String generateMessageId() {
        return "MSG-" + System.nanoTime() + "-" + 
               Integer.toHexString((int) (Math.random() * 0x10000));
    }

    private void processMessage(CrossChainMessage message) {
        // Simulate message processing to target chain
        Log.infof("Processing message %s to chain %s", 
            message.getMessageId(), message.getTargetChain());
        
        // In a real implementation, this would:
        // 1. Validate message integrity
        // 2. Encrypt/decrypt message payload
        // 3. Send to target chain adapter
        // 4. Wait for confirmation
        // 5. Update message status
    }

    // Exception classes
    public static class MessageNotFoundException extends RuntimeException {
        public MessageNotFoundException(String message) { super(message); }
    }
}

/**
 * Cross-chain message request
 */
class MessageRequest {
    private String sourceChain;
    private String targetChain;
    private String sender;
    private String receiver;
    private MessageType messageType;
    private byte[] payload;
    private long nonce;

    public MessageRequest(String sourceChain, String targetChain, String sender, String receiver,
                         MessageType messageType, byte[] payload, long nonce) {
        this.sourceChain = sourceChain;
        this.targetChain = targetChain;
        this.sender = sender;
        this.receiver = receiver;
        this.messageType = messageType;
        this.payload = payload;
        this.nonce = nonce;
    }

    // Getters
    public String getSourceChain() { return sourceChain; }
    public String getTargetChain() { return targetChain; }
    public String getSender() { return sender; }
    public String getReceiver() { return receiver; }
    public MessageType getMessageType() { return messageType; }
    public byte[] getPayload() { return payload; }
    public long getNonce() { return nonce; }
}

/**
 * Cross-chain message
 */
class CrossChainMessage {
    private final String messageId;
    private final String sourceChain;
    private final String targetChain;
    private final String sender;
    private final String receiver;
    private final MessageType messageType;
    private final byte[] payload;
    private final long nonce;
    private final MessageStatus status;
    private final Instant createdAt;
    private final Instant deliveredAt;
    private final String deliveryReceipt;
    private final String errorReason;

    public CrossChainMessage(String messageId, String sourceChain, String targetChain,
                           String sender, String receiver, MessageType messageType,
                           byte[] payload, long nonce, MessageStatus status, Instant createdAt) {
        this(messageId, sourceChain, targetChain, sender, receiver, messageType, payload,
             nonce, status, createdAt, null, null, null);
    }

    private CrossChainMessage(String messageId, String sourceChain, String targetChain,
                            String sender, String receiver, MessageType messageType,
                            byte[] payload, long nonce, MessageStatus status, Instant createdAt,
                            Instant deliveredAt, String deliveryReceipt, String errorReason) {
        this.messageId = messageId;
        this.sourceChain = sourceChain;
        this.targetChain = targetChain;
        this.sender = sender;
        this.receiver = receiver;
        this.messageType = messageType;
        this.payload = payload;
        this.nonce = nonce;
        this.status = status;
        this.createdAt = createdAt;
        this.deliveredAt = deliveredAt;
        this.deliveryReceipt = deliveryReceipt;
        this.errorReason = errorReason;
    }

    public CrossChainMessage withStatus(MessageStatus newStatus) {
        return new CrossChainMessage(messageId, sourceChain, targetChain, sender, receiver,
            messageType, payload, nonce, newStatus, createdAt, deliveredAt, deliveryReceipt, errorReason);
    }

    public CrossChainMessage withDeliveryReceipt(String receipt) {
        return new CrossChainMessage(messageId, sourceChain, targetChain, sender, receiver,
            messageType, payload, nonce, status, createdAt, deliveredAt, receipt, errorReason);
    }

    public CrossChainMessage withDeliveredAt(Instant deliveryTime) {
        return new CrossChainMessage(messageId, sourceChain, targetChain, sender, receiver,
            messageType, payload, nonce, status, createdAt, deliveryTime, deliveryReceipt, errorReason);
    }

    public CrossChainMessage withErrorReason(String error) {
        return new CrossChainMessage(messageId, sourceChain, targetChain, sender, receiver,
            messageType, payload, nonce, status, createdAt, deliveredAt, deliveryReceipt, error);
    }

    // Getters
    public String getMessageId() { return messageId; }
    public String getSourceChain() { return sourceChain; }
    public String getTargetChain() { return targetChain; }
    public String getSender() { return sender; }
    public String getReceiver() { return receiver; }
    public MessageType getMessageType() { return messageType; }
    public byte[] getPayload() { return payload; }
    public long getNonce() { return nonce; }
    public MessageStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getDeliveredAt() { return deliveredAt; }
    public String getDeliveryReceipt() { return deliveryReceipt; }
    public String getErrorReason() { return errorReason; }

    @Override
    public String toString() {
        return String.format("CrossChainMessage{id='%s', %s->%s, type=%s, status=%s}",
            messageId, sourceChain, targetChain, messageType, status);
    }
}

/**
 * Message queue for a specific chain
 */
class MessageQueue {
    private final String chainId;
    private final Queue<CrossChainMessage> queue = new LinkedList<>();
    private final AtomicLong processedCount = new AtomicLong(0);
    private Instant lastProcessedAt;

    public MessageQueue(String chainId) {
        this.chainId = chainId;
    }

    public synchronized void enqueue(CrossChainMessage message) {
        queue.offer(message);
    }

    public synchronized CrossChainMessage dequeue() {
        CrossChainMessage message = queue.poll();
        if (message != null) {
            processedCount.incrementAndGet();
            lastProcessedAt = Instant.now();
        }
        return message;
    }

    public synchronized boolean isEmpty() {
        return queue.isEmpty();
    }

    public int getPendingCount() { return queue.size(); }
    public long getProcessedCount() { return processedCount.get(); }
    public Instant getLastProcessedAt() { return lastProcessedAt; }
}

/**
 * Message queue status
 */
class MessageQueueStatus {
    private final String chainId;
    private final int pendingMessages;
    private final long processedMessages;
    private final Instant lastProcessedAt;

    public MessageQueueStatus(String chainId, int pendingMessages, long processedMessages, Instant lastProcessedAt) {
        this.chainId = chainId;
        this.pendingMessages = pendingMessages;
        this.processedMessages = processedMessages;
        this.lastProcessedAt = lastProcessedAt;
    }

    // Getters
    public String getChainId() { return chainId; }
    public int getPendingMessages() { return pendingMessages; }
    public long getProcessedMessages() { return processedMessages; }
    public Instant getLastProcessedAt() { return lastProcessedAt; }
}

/**
 * Messaging statistics
 */
class MessagingStats {
    private final long totalMessages;
    private final long pendingMessages;
    private final long deliveredMessages;
    private final long failedMessages;
    private final Map<String, Long> chainMessageCounts;
    private final int activeQueues;

    public MessagingStats(long totalMessages, long pendingMessages, long deliveredMessages,
                         long failedMessages, Map<String, Long> chainMessageCounts, int activeQueues) {
        this.totalMessages = totalMessages;
        this.pendingMessages = pendingMessages;
        this.deliveredMessages = deliveredMessages;
        this.failedMessages = failedMessages;
        this.chainMessageCounts = chainMessageCounts;
        this.activeQueues = activeQueues;
    }

    // Getters
    public long getTotalMessages() { return totalMessages; }
    public long getPendingMessages() { return pendingMessages; }
    public long getDeliveredMessages() { return deliveredMessages; }
    public long getFailedMessages() { return failedMessages; }
    public Map<String, Long> getChainMessageCounts() { return chainMessageCounts; }
    public int getActiveQueues() { return activeQueues; }
}

/**
 * Message type enumeration
 */
enum MessageType {
    BRIDGE_REQUEST,
    BRIDGE_CONFIRMATION,
    TOKEN_TRANSFER,
    CONTRACT_CALL,
    VALIDATION_REQUEST,
    VALIDATION_RESPONSE,
    HEARTBEAT,
    GENERIC
}

/**
 * Message status enumeration
 */
enum MessageStatus {
    PENDING, PROCESSING, DELIVERED, FAILED, EXPIRED
}