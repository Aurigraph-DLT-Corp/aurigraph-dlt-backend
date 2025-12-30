package io.aurigraph.v11.websocket;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * WebSocket Subscription Entity
 *
 * Stores user subscription preferences for WebSocket channels.
 * Enables subscription persistence across sessions and devices.
 *
 * Features:
 * - User-channel mapping with priority
 * - Subscription lifecycle tracking
 * - Rate limiting per subscription
 * - Audit trail (created/updated timestamps)
 *
 * @author WebSocket Development Agent (WDA)
 * @since V11.6.0 (Sprint 16 - AV11-484)
 */
@Entity
@Table(name = "websocket_subscriptions", indexes = {
    @Index(name = "idx_user_id", columnList = "user_id"),
    @Index(name = "idx_channel", columnList = "channel"),
    @Index(name = "idx_user_channel", columnList = "user_id, channel", unique = true),
    @Index(name = "idx_status", columnList = "status")
})
public class WebSocketSubscription {

    @Id
    @Column(name = "subscription_id", length = 36, nullable = false)
    public String subscriptionId;

    @Column(name = "user_id", length = 100, nullable = false)
    public String userId;

    @Column(name = "channel", length = 100, nullable = false)
    public String channel;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    public SubscriptionStatus status = SubscriptionStatus.ACTIVE;

    @Column(name = "priority", nullable = false)
    public int priority = 0;

    @Column(name = "rate_limit", nullable = false)
    public int rateLimit = 100; // messages per minute

    @Column(name = "message_count", nullable = false)
    public long messageCount = 0;

    @Column(name = "last_message_at")
    public LocalDateTime lastMessageAt;

    @Column(name = "created_at", nullable = false)
    public LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    public LocalDateTime updatedAt;

    @Column(name = "expires_at")
    public LocalDateTime expiresAt;

    @Column(name = "metadata", columnDefinition = "TEXT")
    public String metadata;

    /**
     * Subscription Status
     */
    public enum SubscriptionStatus {
        ACTIVE,      // Actively receiving messages
        PAUSED,      // Temporarily paused
        SUSPENDED,   // Suspended due to rate limit violation
        EXPIRED      // Expired subscription
    }

    /**
     * Default constructor for JPA
     */
    public WebSocketSubscription() {
        this.subscriptionId = UUID.randomUUID().toString();
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Create new subscription
     *
     * @param userId User ID
     * @param channel Channel name
     */
    public WebSocketSubscription(String userId, String channel) {
        this();
        this.userId = userId;
        this.channel = channel;
    }

    /**
     * Create new subscription with priority
     *
     * @param userId User ID
     * @param channel Channel name
     * @param priority Message priority
     */
    public WebSocketSubscription(String userId, String channel, int priority) {
        this(userId, channel);
        this.priority = priority;
    }

    /**
     * Update timestamp on any modification
     */
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Increment message count
     */
    public void incrementMessageCount() {
        this.messageCount++;
        this.lastMessageAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Check if subscription is active
     */
    public boolean isActive() {
        if (status != SubscriptionStatus.ACTIVE) {
            return false;
        }
        if (expiresAt != null && LocalDateTime.now().isAfter(expiresAt)) {
            status = SubscriptionStatus.EXPIRED;
            return false;
        }
        return true;
    }

    /**
     * Pause subscription
     */
    public void pause() {
        this.status = SubscriptionStatus.PAUSED;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Resume subscription
     */
    public void resume() {
        if (status == SubscriptionStatus.PAUSED || status == SubscriptionStatus.SUSPENDED) {
            this.status = SubscriptionStatus.ACTIVE;
            this.updatedAt = LocalDateTime.now();
        }
    }

    /**
     * Suspend subscription (rate limit violation)
     */
    public void suspend() {
        this.status = SubscriptionStatus.SUSPENDED;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Check if rate limit is exceeded
     *
     * @param messagesInWindow Number of messages in time window
     * @return true if rate limit exceeded
     */
    public boolean isRateLimitExceeded(int messagesInWindow) {
        return messagesInWindow > rateLimit;
    }

    /**
     * Set expiration time
     *
     * @param hours Hours until expiration
     */
    public void setExpiration(int hours) {
        this.expiresAt = LocalDateTime.now().plusHours(hours);
        this.updatedAt = LocalDateTime.now();
    }

    @Override
    public String toString() {
        return "WebSocketSubscription{" +
                "subscriptionId='" + subscriptionId + '\'' +
                ", userId='" + userId + '\'' +
                ", channel='" + channel + '\'' +
                ", status=" + status +
                ", priority=" + priority +
                ", messageCount=" + messageCount +
                ", createdAt=" + createdAt +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof WebSocketSubscription)) return false;
        WebSocketSubscription that = (WebSocketSubscription) o;
        return subscriptionId != null && subscriptionId.equals(that.subscriptionId);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
