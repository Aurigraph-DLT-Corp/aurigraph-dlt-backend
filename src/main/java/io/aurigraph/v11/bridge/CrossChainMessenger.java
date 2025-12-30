package io.aurigraph.v11.bridge;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Interface for cross-chain messaging and communication in Aurigraph V11.
 * 
 * This service provides secure and reliable messaging between different blockchain
 * networks, enabling interoperability and cross-chain functionality. It supports
 * various message types including asset transfers, state synchronization, and
 * arbitrary data communication.
 * 
 * Key Features:
 * - Secure message authentication and verification
 * - Multi-protocol support (Ethereum, BSC, Polygon, Solana, etc.)
 * - Message ordering and delivery guarantees
 * - Cross-chain state synchronization
 * - Batched message processing for efficiency
 * - Automatic retry and failure handling
 * 
 * Performance Requirements:
 * - Process 10K+ cross-chain messages per second
 * - Sub-30-second message delivery times
 * - 99.9% message delivery success rate
 * - Support for 50+ blockchain networks
 * 
 * @author Aurigraph DLT Platform
 * @version 11.0.0
 * @since 2025-01-01
 */
@ApplicationScoped
public interface CrossChainMessenger {

    /**
     * Sends a cross-chain message to a target blockchain network.
     * 
     * @param message the message to send
     * @param targetChain the target blockchain identifier
     * @param messageConfig configuration for message delivery
     * @return Uni containing the message sending result
     */
    Uni<MessageSendResult> sendMessage(
        CrossChainMessage message,
        String targetChain,
        MessageConfig messageConfig
    );

    /**
     * Sends multiple messages in a batch for efficiency.
     * 
     * @param messages the list of messages to send
     * @param targetChain the target blockchain identifier
     * @param batchConfig configuration for batch processing
     * @return Uni containing the batch sending result
     */
    Uni<BatchMessageResult> sendBatch(
        List<CrossChainMessage> messages,
        String targetChain,
        BatchMessageConfig batchConfig
    );

    /**
     * Receives and processes incoming cross-chain messages.
     * 
     * @param sourceChain the source blockchain identifier
     * @param messageFilter optional filter for message types
     * @return Multi streaming incoming messages
     */
    Multi<ReceivedMessage> receiveMessages(String sourceChain, MessageFilter messageFilter);

    /**
     * Acknowledges receipt and processing of a cross-chain message.
     * 
     * @param messageId the identifier of the message to acknowledge
     * @param acknowledgmentType the type of acknowledgment
     * @param processingResult the result of message processing
     * @return Uni indicating success or failure of acknowledgment
     */
    Uni<Boolean> acknowledgeMessage(
        String messageId,
        AcknowledgmentType acknowledgmentType,
        MessageProcessingResult processingResult
    );

    /**
     * Gets the current status of a cross-chain message.
     * 
     * @param messageId the identifier of the message
     * @return Uni containing the current message status
     */
    Uni<MessageStatus> getMessageStatus(String messageId);

    /**
     * Tracks multiple messages and provides status updates.
     * 
     * @param messageIds the list of message identifiers to track
     * @return Multi streaming status updates for the tracked messages
     */
    Multi<MessageStatusUpdate> trackMessages(List<String> messageIds);

    /**
     * Retries failed message delivery with exponential backoff.
     * 
     * @param messageId the identifier of the failed message
     * @param retryConfig configuration for retry behavior
     * @return Uni containing the retry result
     */
    Uni<MessageRetryResult> retryMessage(String messageId, RetryConfig retryConfig);

    /**
     * Cancels a pending cross-chain message if still possible.
     * 
     * @param messageId the identifier of the message to cancel
     * @param reason the reason for cancellation
     * @return Uni containing the cancellation result
     */
    Uni<MessageCancellationResult> cancelMessage(String messageId, String reason);

    /**
     * Validates a cross-chain message before sending.
     * 
     * @param message the message to validate
     * @param targetChain the target blockchain identifier
     * @param validationLevel the level of validation to perform
     * @return Uni containing the validation result
     */
    Uni<MessageValidationResult> validateMessage(
        CrossChainMessage message,
        String targetChain,
        ValidationLevel validationLevel
    );

    /**
     * Estimates the cost and time for cross-chain message delivery.
     * 
     * @param message the message to estimate for
     * @param targetChain the target blockchain identifier
     * @return Uni containing delivery estimates
     */
    Uni<DeliveryEstimate> estimateDelivery(CrossChainMessage message, String targetChain);

    /**
     * Sets up message routing rules for automatic message handling.
     * 
     * @param routingRule the routing rule configuration
     * @return Uni containing the routing setup result
     */
    Uni<RoutingSetupResult> setupMessageRouting(MessageRoutingRule routingRule);

    /**
     * Configures message encryption and security settings.
     * 
     * @param chainId the blockchain identifier
     * @param securityConfig security configuration for the chain
     * @return Uni indicating success or failure of configuration
     */
    Uni<Boolean> configureMessageSecurity(String chainId, MessageSecurityConfig securityConfig);

    /**
     * Monitors cross-chain messaging performance and reliability.
     * 
     * @param monitoringInterval the interval between monitoring updates
     * @return Multi streaming messaging performance metrics
     */
    Multi<MessagingMetrics> monitorMessaging(Duration monitoringInterval);

    /**
     * Gets historical messaging statistics for analysis.
     * 
     * @param chainPair the source and target chain pair
     * @param fromTimestamp start timestamp for statistics
     * @param toTimestamp end timestamp for statistics
     * @return Uni containing historical messaging statistics
     */
    Uni<MessagingStatistics> getMessagingStatistics(
        ChainPair chainPair,
        long fromTimestamp,
        long toTimestamp
    );

    /**
     * Synchronizes state between different blockchain networks.
     * 
     * @param stateSync the state synchronization request
     * @return Uni containing the state synchronization result
     */
    Uni<StateSyncResult> synchronizeState(StateSynchronizationRequest stateSync);

    /**
     * Establishes a persistent messaging channel between chains.
     * 
     * @param channelConfig configuration for the messaging channel
     * @return Uni containing the channel establishment result
     */
    Uni<ChannelEstablishmentResult> establishChannel(MessagingChannelConfig channelConfig);

    // Inner classes and enums for data transfer objects

    /**
     * Represents a cross-chain message.
     */
    public static class CrossChainMessage {
        public String messageId;
        public String sourceChain;
        public String targetChain;
        public MessageType messageType;
        public String sender;
        public String recipient;
        public byte[] payload;
        public Map<String, String> metadata;
        public MessagePriority priority;
        public long timestamp;
        public String signature;
        public BigDecimal nonce;

        public CrossChainMessage(String sourceChain, String targetChain, MessageType messageType) {
            this.messageId = generateMessageId();
            this.sourceChain = sourceChain;
            this.targetChain = targetChain;
            this.messageType = messageType;
            this.timestamp = System.currentTimeMillis();
            this.priority = MessagePriority.NORMAL;
        }

        private String generateMessageId() {
            return "msg_" + System.nanoTime() + "_" + Math.random();
        }
    }

    /**
     * Types of cross-chain messages.
     */
    public enum MessageType {
        ASSET_TRANSFER,
        STATE_SYNC,
        CONTRACT_CALL,
        EVENT_NOTIFICATION,
        GOVERNANCE_PROPOSAL,
        ORACLE_UPDATE,
        GENERIC_DATA,
        PROTOCOL_MESSAGE
    }

    /**
     * Priority levels for message processing.
     */
    public enum MessagePriority {
        LOW,
        NORMAL,
        HIGH,
        CRITICAL
    }

    /**
     * Configuration for message sending.
     */
    public static class MessageConfig {
        public Duration timeout;
        public int maxRetries;
        public boolean requireConfirmation;
        public EncryptionLevel encryptionLevel;
        public CompressionType compressionType;
        public boolean enableBatching;
        public MessagePriority priority;
    }

    /**
     * Encryption levels for message security.
     */
    public enum EncryptionLevel {
        NONE,
        BASIC,
        STRONG,
        QUANTUM_RESISTANT
    }

    /**
     * Compression types for message optimization.
     */
    public enum CompressionType {
        NONE,
        GZIP,
        LZ4,
        ZSTD
    }

    /**
     * Result of message sending operation.
     */
    public static class MessageSendResult {
        public String messageId;
        public String transactionHash;
        public MessageDeliveryStatus status;
        public long estimatedDeliveryTime;
        public BigDecimal deliveryCost;
        public String routingPath;
        public boolean success;
        public String errorMessage;
    }

    /**
     * Status of message delivery.
     */
    public enum MessageDeliveryStatus {
        PENDING,
        ROUTING,
        IN_TRANSIT,
        DELIVERED,
        ACKNOWLEDGED,
        FAILED,
        EXPIRED,
        CANCELLED
    }

    /**
     * Configuration for batch message processing.
     */
    public static class BatchMessageConfig {
        public int maxBatchSize;
        public Duration batchTimeout;
        public boolean preserveOrder;
        public boolean enableParallelProcessing;
        public FailureHandling failureHandling;
    }

    /**
     * Failure handling strategies for batch processing.
     */
    public enum FailureHandling {
        FAIL_FAST,      // Stop on first failure
        CONTINUE,       // Continue processing despite failures
        RETRY_FAILED,   // Retry failed messages
        ROLLBACK_ALL    // Rollback entire batch on failure
    }

    /**
     * Result of batch message processing.
     */
    public static class BatchMessageResult {
        public int totalMessages;
        public int successfulMessages;
        public int failedMessages;
        public List<MessageSendResult> results;
        public long totalProcessingTime;
        public BigDecimal totalCost;
        public boolean allSuccessful;
    }

    /**
     * Filter for incoming messages.
     */
    public static class MessageFilter {
        public List<MessageType> messageTypes;
        public List<String> senders;
        public List<String> recipients;
        public MessagePriority minPriority;
        public long fromTimestamp;
        public long toTimestamp;
        public Map<String, String> metadataFilters;
    }

    /**
     * Received cross-chain message.
     */
    public static class ReceivedMessage {
        public CrossChainMessage message;
        public long receivedTimestamp;
        public String routingPath;
        public MessageValidationStatus validationStatus;
        public boolean requiresAcknowledgment;
        public Duration processingDeadline;
    }

    /**
     * Validation status for received messages.
     */
    public enum MessageValidationStatus {
        VALID,
        INVALID_SIGNATURE,
        INVALID_NONCE,
        EXPIRED,
        MALFORMED,
        UNAUTHORIZED
    }

    /**
     * Types of message acknowledgments.
     */
    public enum AcknowledgmentType {
        RECEIVED,       // Message received
        PROCESSED,      // Message processed successfully
        FAILED,         // Message processing failed
        REJECTED        // Message rejected
    }

    /**
     * Result of message processing.
     */
    public static class MessageProcessingResult {
        public boolean success;
        public String resultHash;
        public Map<String, Object> processingData;
        public String errorMessage;
        public long processingTime;
    }

    /**
     * Current status of a message.
     */
    public static class MessageStatus {
        public String messageId;
        public MessageDeliveryStatus deliveryStatus;
        public long statusTimestamp;
        public String currentLocation; // Current chain or relay
        public double deliveryProgress; // 0.0 to 1.0
        public List<MessageEvent> events;
        public String nextStep;
        public Duration estimatedTimeToDelivery;
    }

    /**
     * Events in message lifecycle.
     */
    public static class MessageEvent {
        public String eventType;
        public long timestamp;
        public String location;
        public String description;
        public Map<String, Object> eventData;
    }

    /**
     * Status update for tracked messages.
     */
    public static class MessageStatusUpdate {
        public String messageId;
        public MessageDeliveryStatus oldStatus;
        public MessageDeliveryStatus newStatus;
        public long updateTimestamp;
        public String updateReason;
        public Map<String, Object> additionalInfo;
    }

    /**
     * Configuration for message retry behavior.
     */
    public static class RetryConfig {
        public int maxRetries;
        public Duration initialDelay;
        public double backoffMultiplier;
        public Duration maxDelay;
        public List<String> retryableErrors;
        public boolean enableCircuitBreaker;
    }

    /**
     * Result of message retry operation.
     */
    public static class MessageRetryResult {
        public String messageId;
        public boolean retryStarted;
        public int attemptNumber;
        public Duration nextRetryDelay;
        public String errorMessage;
    }

    /**
     * Result of message cancellation.
     */
    public static class MessageCancellationResult {
        public String messageId;
        public boolean cancelled;
        public String cancellationReason;
        public BigDecimal refundAmount;
        public String errorMessage;
    }

    /**
     * Levels of message validation.
     */
    public enum ValidationLevel {
        BASIC,          // Basic format and signature validation
        STANDARD,       // Standard + nonce and timestamp validation
        STRICT,         // Strict + sender authorization validation
        COMPREHENSIVE   // Comprehensive + payload validation
    }

    /**
     * Result of message validation.
     */
    public static class MessageValidationResult {
        public boolean isValid;
        public List<ValidationError> errors;
        public List<ValidationWarning> warnings;
        public ValidationLevel validationLevel;
        public Map<String, Object> validationData;
    }

    /**
     * Validation error information.
     */
    public static class ValidationError {
        public String errorCode;
        public String errorMessage;
        public String fieldName;
        public String suggestedFix;
    }

    /**
     * Validation warning information.
     */
    public static class ValidationWarning {
        public String warningCode;
        public String warningMessage;
        public String recommendation;
    }

    /**
     * Delivery cost and time estimates.
     */
    public static class DeliveryEstimate {
        public BigDecimal estimatedCost;
        public Duration estimatedDeliveryTime;
        public Duration minDeliveryTime;
        public Duration maxDeliveryTime;
        public List<String> routingOptions;
        public Map<String, Object> costBreakdown;
        public double deliveryConfidence; // 0.0 to 1.0
    }

    /**
     * Message routing rule configuration.
     */
    public static class MessageRoutingRule {
        public String ruleId;
        public MessageFilter filter;
        public String targetHandler;
        public RoutingAction action;
        public Map<String, Object> actionParameters;
        public int priority;
        public boolean enabled;
    }

    /**
     * Actions for message routing.
     */
    public enum RoutingAction {
        FORWARD,
        TRANSFORM,
        AGGREGATE,
        FILTER,
        DUPLICATE,
        CUSTOM
    }

    /**
     * Result of routing setup.
     */
    public static class RoutingSetupResult {
        public String ruleId;
        public boolean configured;
        public String errorMessage;
    }

    /**
     * Security configuration for messaging.
     */
    public static class MessageSecurityConfig {
        public EncryptionLevel defaultEncryption;
        public String encryptionKey;
        public boolean requireSignatures;
        public String signingKey;
        public List<String> authorizedSenders;
        public boolean enableAntiReplay;
        public Duration maxMessageAge;
    }

    /**
     * Real-time messaging performance metrics.
     */
    public static class MessagingMetrics {
        public long timestamp;
        public double messagesPerSecond;
        public double averageDeliveryTime;
        public double deliverySuccessRate;
        public Map<String, Double> deliveryTimesByChain;
        public Map<MessageType, Long> messagesByType;
        public List<String> activeIssues;
        public SystemHealth systemHealth;
    }

    /**
     * System health for messaging.
     */
    public enum SystemHealth {
        HEALTHY,
        DEGRADED,
        CRITICAL,
        MAINTENANCE
    }

    /**
     * Historical messaging statistics.
     */
    public static class MessagingStatistics {
        public ChainPair chainPair;
        public long totalMessages;
        public long successfulDeliveries;
        public long failedDeliveries;
        public double averageDeliveryTime;
        public double deliverySuccessRate;
        public BigDecimal totalCosts;
        public Map<MessageType, Long> messageBreakdown;
        public List<CommonFailureReason> failureReasons;
        public long statisticsTimeWindow;
    }

    /**
     * Pair of source and target chains.
     */
    public static class ChainPair {
        public String sourceChain;
        public String targetChain;

        public ChainPair(String sourceChain, String targetChain) {
            this.sourceChain = sourceChain;
            this.targetChain = targetChain;
        }
    }

    /**
     * Common reasons for message delivery failures.
     */
    public static class CommonFailureReason {
        public String reason;
        public long count;
        public double percentage;
        public String recommendation;
    }

    /**
     * State synchronization request.
     */
    public static class StateSynchronizationRequest {
        public String sourceChain;
        public String targetChain;
        public String stateIdentifier;
        public byte[] stateData;
        public String stateHash;
        public SyncMode syncMode;
        public boolean requireConfirmation;
    }

    /**
     * Modes for state synchronization.
     */
    public enum SyncMode {
        FULL_SYNC,      // Synchronize complete state
        DELTA_SYNC,     // Synchronize only changes
        MERKLE_SYNC,    // Use Merkle tree for efficient sync
        CUSTOM          // Custom synchronization method
    }

    /**
     * Result of state synchronization.
     */
    public static class StateSyncResult {
        public String syncId;
        public boolean successful;
        public String stateHash;
        public long syncTime;
        public SyncMode syncMode;
        public String errorMessage;
    }

    /**
     * Configuration for messaging channels.
     */
    public static class MessagingChannelConfig {
        public String channelId;
        public String sourceChain;
        public String targetChain;
        public ChannelType channelType;
        public boolean enableOrdering;
        public Duration keepAliveInterval;
        public Map<String, Object> channelParameters;
    }

    /**
     * Types of messaging channels.
     */
    public enum ChannelType {
        UNORDERED,      // No message ordering guarantees
        ORDERED,        // Messages delivered in order
        RELIABLE,       // Guaranteed delivery with retries
        STREAMING       // Continuous streaming channel
    }

    /**
     * Result of channel establishment.
     */
    public static class ChannelEstablishmentResult {
        public String channelId;
        public boolean established;
        public String channelEndpoint;
        public ChannelType channelType;
        public String errorMessage;
    }
}