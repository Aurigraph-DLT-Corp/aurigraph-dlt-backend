package io.aurigraph.v11.websocket;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * WebSocket Subscription Service
 *
 * Manages WebSocket channel subscriptions with database persistence:
 * - Dynamic subscription/unsubscription
 * - Subscription persistence (save user preferences to DB)
 * - Subscription limits (max channels per user)
 * - Rate limiting per subscription
 * - Subscription history and analytics
 *
 * Thread-safe with concurrent rate limiting tracking.
 *
 * @author WebSocket Development Agent (WDA)
 * @since V11.6.0 (Sprint 16 - AV11-484)
 */
@ApplicationScoped
public class WebSocketSubscriptionService implements PanacheRepository<WebSocketSubscription> {

    private static final Logger LOG = Logger.getLogger(WebSocketSubscriptionService.class);
    private static final int DEFAULT_MAX_SUBSCRIPTIONS_PER_USER = 50;
    private static final int DEFAULT_RATE_LIMIT = 100; // messages per minute

    // Rate limiting tracking (sessionId -> channel -> message count in current window)
    private final Map<String, Map<String, RateLimitWindow>> rateLimitTracker = new ConcurrentHashMap<>();

    /**
     * Subscribe user to a channel (persisted to database)
     *
     * @param userId User ID
     * @param channel Channel name
     * @param priority Message priority (0-10, higher = more important)
     * @return WebSocketSubscription or null if failed
     */
    @Transactional
    public WebSocketSubscription subscribe(String userId, String channel, int priority) {
        try {
            // Check if subscription already exists
            Optional<WebSocketSubscription> existing = findByUserIdAndChannel(userId, channel);
            if (existing.isPresent()) {
                WebSocketSubscription subscription = existing.get();
                if (!subscription.isActive()) {
                    subscription.resume();
                    persist(subscription);
                    LOG.infof("Resumed existing subscription for user %s to channel %s", userId, channel);
                }
                return subscription;
            }

            // Check subscription limit
            long count = countActiveSubscriptionsByUserId(userId);
            if (count >= DEFAULT_MAX_SUBSCRIPTIONS_PER_USER) {
                LOG.warnf("User %s exceeded max subscriptions (%d)", userId, DEFAULT_MAX_SUBSCRIPTIONS_PER_USER);
                return null;
            }

            // Create new subscription
            WebSocketSubscription subscription = new WebSocketSubscription(userId, channel, priority);
            subscription.rateLimit = DEFAULT_RATE_LIMIT;
            persist(subscription);

            LOG.infof("Created subscription for user %s to channel %s (priority: %d)", userId, channel, priority);
            return subscription;

        } catch (Exception e) {
            LOG.errorf(e, "Failed to subscribe user %s to channel %s", userId, channel);
            return null;
        }
    }

    /**
     * Subscribe with default priority
     */
    @Transactional
    public WebSocketSubscription subscribe(String userId, String channel) {
        return subscribe(userId, channel, 0);
    }

    /**
     * Unsubscribe user from a channel
     *
     * @param userId User ID
     * @param channel Channel name
     * @return true if successful
     */
    @Transactional
    public boolean unsubscribe(String userId, String channel) {
        try {
            Optional<WebSocketSubscription> subscription = findByUserIdAndChannel(userId, channel);
            if (subscription.isPresent()) {
                delete(subscription.get());
                LOG.infof("Deleted subscription for user %s from channel %s", userId, channel);
                return true;
            }
            return false;
        } catch (Exception e) {
            LOG.errorf(e, "Failed to unsubscribe user %s from channel %s", userId, channel);
            return false;
        }
    }

    /**
     * Pause subscription (keep in DB but don't deliver messages)
     *
     * @param userId User ID
     * @param channel Channel name
     * @return true if successful
     */
    @Transactional
    public boolean pauseSubscription(String userId, String channel) {
        try {
            Optional<WebSocketSubscription> subscription = findByUserIdAndChannel(userId, channel);
            if (subscription.isPresent()) {
                WebSocketSubscription sub = subscription.get();
                sub.pause();
                persist(sub);
                LOG.infof("Paused subscription for user %s to channel %s", userId, channel);
                return true;
            }
            return false;
        } catch (Exception e) {
            LOG.errorf(e, "Failed to pause subscription for user %s, channel %s", userId, channel);
            return false;
        }
    }

    /**
     * Resume paused subscription
     *
     * @param userId User ID
     * @param channel Channel name
     * @return true if successful
     */
    @Transactional
    public boolean resumeSubscription(String userId, String channel) {
        try {
            Optional<WebSocketSubscription> subscription = findByUserIdAndChannel(userId, channel);
            if (subscription.isPresent()) {
                WebSocketSubscription sub = subscription.get();
                sub.resume();
                persist(sub);
                LOG.infof("Resumed subscription for user %s to channel %s", userId, channel);
                return true;
            }
            return false;
        } catch (Exception e) {
            LOG.errorf(e, "Failed to resume subscription for user %s, channel %s", userId, channel);
            return false;
        }
    }

    /**
     * Get all active subscriptions for a user
     *
     * @param userId User ID
     * @return List of active subscriptions
     */
    public List<WebSocketSubscription> getActiveSubscriptions(String userId) {
        return find("userId = ?1 AND status = ?2", userId, WebSocketSubscription.SubscriptionStatus.ACTIVE).list();
    }

    /**
     * Get all subscriptions for a user (including paused/expired)
     *
     * @param userId User ID
     * @return List of all subscriptions
     */
    public List<WebSocketSubscription> getAllSubscriptions(String userId) {
        return find("userId", userId).list();
    }

    /**
     * Find subscription by user and channel
     *
     * @param userId User ID
     * @param channel Channel name
     * @return Optional subscription
     */
    public Optional<WebSocketSubscription> findByUserIdAndChannel(String userId, String channel) {
        return find("userId = ?1 AND channel = ?2", userId, channel).firstResultOptional();
    }

    /**
     * Count active subscriptions for a user
     *
     * @param userId User ID
     * @return Count of active subscriptions
     */
    public long countActiveSubscriptionsByUserId(String userId) {
        return count("userId = ?1 AND status = ?2", userId, WebSocketSubscription.SubscriptionStatus.ACTIVE);
    }

    /**
     * Check rate limit for subscription
     *
     * @param sessionId Session ID
     * @param userId User ID
     * @param channel Channel name
     * @return true if message can be delivered (under rate limit)
     */
    public boolean checkRateLimit(String sessionId, String userId, String channel) {
        try {
            // Get subscription to check rate limit
            Optional<WebSocketSubscription> subscriptionOpt = findByUserIdAndChannel(userId, channel);
            if (!subscriptionOpt.isPresent()) {
                return true; // No subscription = no rate limit
            }

            WebSocketSubscription subscription = subscriptionOpt.get();
            if (!subscription.isActive()) {
                return false; // Inactive subscriptions can't receive messages
            }

            // Get or create rate limit window
            Map<String, RateLimitWindow> channelWindows = rateLimitTracker.computeIfAbsent(
                sessionId, k -> new ConcurrentHashMap<>()
            );

            RateLimitWindow window = channelWindows.computeIfAbsent(
                channel, k -> new RateLimitWindow()
            );

            // Check if window has expired (1 minute)
            if (window.isExpired()) {
                window.reset();
            }

            // Check rate limit
            int currentCount = window.getCount();
            if (currentCount >= subscription.rateLimit) {
                LOG.warnf("Rate limit exceeded for user %s on channel %s (%d/%d messages)",
                    userId, channel, currentCount, subscription.rateLimit);

                // Suspend subscription temporarily
                subscription.suspend();
                persist(subscription);

                return false;
            }

            // Increment count
            window.increment();
            return true;

        } catch (Exception e) {
            LOG.errorf(e, "Error checking rate limit for user %s, channel %s", userId, channel);
            return true; // Allow message on error
        }
    }

    /**
     * Record message delivery to subscription
     *
     * @param userId User ID
     * @param channel Channel name
     */
    @Transactional
    public void recordMessageDelivery(String userId, String channel) {
        try {
            Optional<WebSocketSubscription> subscriptionOpt = findByUserIdAndChannel(userId, channel);
            if (subscriptionOpt.isPresent()) {
                WebSocketSubscription subscription = subscriptionOpt.get();
                subscription.incrementMessageCount();
                persist(subscription);
            }
        } catch (Exception e) {
            LOG.errorf(e, "Failed to record message delivery for user %s, channel %s", userId, channel);
        }
    }

    /**
     * Cleanup rate limit tracking for session
     *
     * @param sessionId Session ID to cleanup
     */
    public void cleanupRateLimitTracking(String sessionId) {
        rateLimitTracker.remove(sessionId);
        LOG.debugf("Cleaned up rate limit tracking for session %s", sessionId);
    }

    /**
     * Get subscription statistics for a user
     *
     * @param userId User ID
     * @return Subscription statistics
     */
    public SubscriptionStats getStats(String userId) {
        List<WebSocketSubscription> all = getAllSubscriptions(userId);
        long active = all.stream().filter(WebSocketSubscription::isActive).count();
        long paused = all.stream().filter(s -> s.status == WebSocketSubscription.SubscriptionStatus.PAUSED).count();
        long suspended = all.stream().filter(s -> s.status == WebSocketSubscription.SubscriptionStatus.SUSPENDED).count();
        long expired = all.stream().filter(s -> s.status == WebSocketSubscription.SubscriptionStatus.EXPIRED).count();
        long totalMessages = all.stream().mapToLong(s -> s.messageCount).sum();

        return new SubscriptionStats(userId, all.size(), active, paused, suspended, expired, totalMessages);
    }

    /**
     * Cleanup expired subscriptions (background task)
     *
     * @return Number of cleaned up subscriptions
     */
    @Transactional
    public int cleanupExpiredSubscriptions() {
        try {
            List<WebSocketSubscription> expired = find(
                "expiresAt IS NOT NULL AND expiresAt < ?1 AND status != ?2",
                LocalDateTime.now(),
                WebSocketSubscription.SubscriptionStatus.EXPIRED
            ).list();

            for (WebSocketSubscription subscription : expired) {
                subscription.status = WebSocketSubscription.SubscriptionStatus.EXPIRED;
                persist(subscription);
            }

            LOG.infof("Marked %d subscriptions as expired", expired.size());
            return expired.size();

        } catch (Exception e) {
            LOG.errorf(e, "Failed to cleanup expired subscriptions");
            return 0;
        }
    }

    /**
     * Rate Limit Window (1 minute sliding window)
     */
    private static class RateLimitWindow {
        private int count = 0;
        private long windowStart = System.currentTimeMillis();
        private static final long WINDOW_DURATION_MS = 60_000; // 1 minute

        public void increment() {
            count++;
        }

        public int getCount() {
            return count;
        }

        public boolean isExpired() {
            return System.currentTimeMillis() - windowStart > WINDOW_DURATION_MS;
        }

        public void reset() {
            count = 0;
            windowStart = System.currentTimeMillis();
        }
    }

    /**
     * Subscription Statistics
     */
    public static class SubscriptionStats {
        public final String userId;
        public final long totalSubscriptions;
        public final long activeSubscriptions;
        public final long pausedSubscriptions;
        public final long suspendedSubscriptions;
        public final long expiredSubscriptions;
        public final long totalMessagesDelivered;

        public SubscriptionStats(String userId, long total, long active, long paused,
                               long suspended, long expired, long totalMessages) {
            this.userId = userId;
            this.totalSubscriptions = total;
            this.activeSubscriptions = active;
            this.pausedSubscriptions = paused;
            this.suspendedSubscriptions = suspended;
            this.expiredSubscriptions = expired;
            this.totalMessagesDelivered = totalMessages;
        }

        @Override
        public String toString() {
            return String.format("SubscriptionStats{user='%s', total=%d, active=%d, paused=%d, suspended=%d, expired=%d, messages=%d}",
                userId, totalSubscriptions, activeSubscriptions, pausedSubscriptions,
                suspendedSubscriptions, expiredSubscriptions, totalMessagesDelivered);
        }
    }
}
