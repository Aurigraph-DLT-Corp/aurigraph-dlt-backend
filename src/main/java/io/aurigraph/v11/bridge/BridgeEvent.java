package io.aurigraph.v11.bridge;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

/**
 * Bridge Event - Domain model for bridge state change events
 *
 * Represents various events that occur during bridge transactions:
 * - State transitions
 * - Multi-signature validations
 * - Atomic swap progress
 * - Error conditions
 *
 * @author Aurigraph V11 Bridge Team
 * @version 11.0.0
 */
public class BridgeEvent {

    public enum EventType {
        BRIDGE_INITIATED,
        LOCK_CREATED,
        SIGNATURE_RECEIVED,
        MULTI_SIG_THRESHOLD_REACHED,
        FUNDS_LOCKED,
        FUNDS_RELEASED,
        SWAP_COMPLETED,
        SWAP_REFUNDED,
        BRIDGE_COMPLETED,
        BRIDGE_FAILED,
        TIMEOUT_WARNING,
        TIMEOUT_OCCURRED
    }

    private final String eventId;
    private final String transactionId;
    private final EventType eventType;
    private final String sourceChain;
    private final String targetChain;
    private final BigDecimal amount;
    private final String message;
    private final Map<String, Object> metadata;
    private final Instant timestamp;

    public BridgeEvent(String eventId, String transactionId, EventType eventType,
                      String sourceChain, String targetChain, BigDecimal amount,
                      String message, Map<String, Object> metadata) {
        this.eventId = eventId;
        this.transactionId = transactionId;
        this.eventType = eventType;
        this.sourceChain = sourceChain;
        this.targetChain = targetChain;
        this.amount = amount;
        this.message = message;
        this.metadata = metadata;
        this.timestamp = Instant.now();
    }

    // Getters
    public String getEventId() { return eventId; }
    public String getTransactionId() { return transactionId; }
    public EventType getEventType() { return eventType; }
    public String getSourceChain() { return sourceChain; }
    public String getTargetChain() { return targetChain; }
    public BigDecimal getAmount() { return amount; }
    public String getMessage() { return message; }
    public Map<String, Object> getMetadata() { return metadata; }
    public Instant getTimestamp() { return timestamp; }

    @Override
    public String toString() {
        return String.format("BridgeEvent{id='%s', type=%s, tx='%s', %s->%s, amount=%s, time=%s}",
            eventId, eventType, transactionId, sourceChain, targetChain, amount, timestamp);
    }
}
