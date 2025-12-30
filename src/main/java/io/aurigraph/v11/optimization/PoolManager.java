package io.aurigraph.v11.optimization;

import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.annotation.PostConstruct;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Pool Manager - Sprint 15 Phase 2
 * Manages object pools for Transaction, ValidationContext, and MessageBuffer
 *
 * @author BDA-Performance
 * @version 1.0
 * @since Sprint 15
 */
@ApplicationScoped
public class PoolManager {

    @ConfigProperty(name = "optimization.memory.pool.enabled", defaultValue = "true")
    boolean enabled;

    @ConfigProperty(name = "optimization.pool.transaction.initial.size", defaultValue = "5000")
    int txInitialSize;

    @ConfigProperty(name = "optimization.pool.transaction.max.size", defaultValue = "50000")
    int txMaxSize;

    @ConfigProperty(name = "optimization.pool.transaction.acquire.timeout.ms", defaultValue = "10")
    long txAcquireTimeout;

    @ConfigProperty(name = "optimization.pool.validation.initial.size", defaultValue = "1000")
    int validationInitialSize;

    @ConfigProperty(name = "optimization.pool.validation.max.size", defaultValue = "10000")
    int validationMaxSize;

    @ConfigProperty(name = "optimization.pool.validation.acquire.timeout.ms", defaultValue = "5")
    long validationAcquireTimeout;

    @ConfigProperty(name = "optimization.pool.message.initial.size", defaultValue = "2000")
    int messageInitialSize;

    @ConfigProperty(name = "optimization.pool.message.max.size", defaultValue = "20000")
    int messageMaxSize;

    @ConfigProperty(name = "optimization.pool.message.acquire.timeout.ms", defaultValue = "5")
    long messageAcquireTimeout;

    private ObjectPool<TransactionContext> transactionPool;
    private ObjectPool<ValidationContext> validationPool;
    private ObjectPool<MessageBuffer> messagePool;

    @PostConstruct
    public void init() {
        if (!enabled) {
            Log.info("Memory pooling disabled");
            return;
        }

        // Initialize transaction context pool
        transactionPool = new ObjectPool<>(
            "TransactionContext",
            TransactionContext::new,
            txInitialSize,
            txMaxSize,
            txAcquireTimeout
        );

        // Initialize validation context pool
        validationPool = new ObjectPool<>(
            "ValidationContext",
            ValidationContext::new,
            validationInitialSize,
            validationMaxSize,
            validationAcquireTimeout
        );

        // Initialize message buffer pool
        messagePool = new ObjectPool<>(
            "MessageBuffer",
            MessageBuffer::new,
            messageInitialSize,
            messageMaxSize,
            messageAcquireTimeout
        );

        Log.infof("Pool Manager initialized with 3 pools: TransactionContext(%d/%d), ValidationContext(%d/%d), MessageBuffer(%d/%d)",
                 txInitialSize, txMaxSize,
                 validationInitialSize, validationMaxSize,
                 messageInitialSize, messageMaxSize);
    }

    public ObjectPool<TransactionContext> getTransactionPool() {
        return transactionPool;
    }

    public ObjectPool<ValidationContext> getValidationPool() {
        return validationPool;
    }

    public ObjectPool<MessageBuffer> getMessagePool() {
        return messagePool;
    }

    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Get combined pool metrics
     */
    public PoolManagerMetrics getMetrics() {
        return new PoolManagerMetrics(
            transactionPool != null ? transactionPool.getMetrics() : null,
            validationPool != null ? validationPool.getMetrics() : null,
            messagePool != null ? messagePool.getMetrics() : null
        );
    }

    public record PoolManagerMetrics(
        ObjectPool.PoolMetrics transactionMetrics,
        ObjectPool.PoolMetrics validationMetrics,
        ObjectPool.PoolMetrics messageMetrics
    ) {
        public double overallHitRate() {
            if (transactionMetrics == null || validationMetrics == null || messageMetrics == null) {
                return 0.0;
            }

            long totalAcquires = transactionMetrics.totalAcquires() +
                               validationMetrics.totalAcquires() +
                               messageMetrics.totalAcquires();

            long totalTimeouts = transactionMetrics.totalTimeouts() +
                               validationMetrics.totalTimeouts() +
                               messageMetrics.totalTimeouts();

            return totalAcquires > 0 ?
                (double) (totalAcquires - totalTimeouts) / totalAcquires : 0.0;
        }
    }

    /**
     * Transaction context for pooling
     */
    public static class TransactionContext implements Poolable {
        private String id;
        private String from;
        private String to;
        private long amount;
        private long timestamp;
        private byte[] signature;

        @Override
        public void reset() {
            this.id = null;
            this.from = null;
            this.to = null;
            this.amount = 0;
            this.timestamp = 0;
            this.signature = null;
        }

        // Getters and setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getFrom() { return from; }
        public void setFrom(String from) { this.from = from; }
        public String getTo() { return to; }
        public void setTo(String to) { this.to = to; }
        public long getAmount() { return amount; }
        public void setAmount(long amount) { this.amount = amount; }
        public long getTimestamp() { return timestamp; }
        public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
        public byte[] getSignature() { return signature; }
        public void setSignature(byte[] signature) { this.signature = signature; }
    }

    /**
     * Validation context for pooling
     */
    public static class ValidationContext implements Poolable {
        private String transactionId;
        private boolean signatureValid;
        private boolean balanceValid;
        private boolean nonceValid;
        private String errorMessage;

        @Override
        public void reset() {
            this.transactionId = null;
            this.signatureValid = false;
            this.balanceValid = false;
            this.nonceValid = false;
            this.errorMessage = null;
        }

        // Getters and setters
        public String getTransactionId() { return transactionId; }
        public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
        public boolean isSignatureValid() { return signatureValid; }
        public void setSignatureValid(boolean signatureValid) { this.signatureValid = signatureValid; }
        public boolean isBalanceValid() { return balanceValid; }
        public void setBalanceValid(boolean balanceValid) { this.balanceValid = balanceValid; }
        public boolean isNonceValid() { return nonceValid; }
        public void setNonceValid(boolean nonceValid) { this.nonceValid = nonceValid; }
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    }

    /**
     * Message buffer for pooling
     */
    public static class MessageBuffer implements Poolable {
        private static final int DEFAULT_CAPACITY = 8192;
        private byte[] buffer;
        private int position;
        private int limit;

        public MessageBuffer() {
            this.buffer = new byte[DEFAULT_CAPACITY];
            this.position = 0;
            this.limit = DEFAULT_CAPACITY;
        }

        @Override
        public void reset() {
            this.position = 0;
            this.limit = buffer.length;
            // Don't reallocate buffer, just reset position
        }

        // Buffer operations
        public byte[] getBuffer() { return buffer; }
        public int getPosition() { return position; }
        public void setPosition(int position) { this.position = position; }
        public int getLimit() { return limit; }
        public void setLimit(int limit) { this.limit = limit; }
        public int capacity() { return buffer.length; }
        public int remaining() { return limit - position; }
    }
}
