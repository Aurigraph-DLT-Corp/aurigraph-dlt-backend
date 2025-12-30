package io.aurigraph.v11.analytics;

import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Analytics Cache
 *
 * In-memory cache for analytics data with TTL support.
 * Reduces redundant calculations for frequently requested analytics.
 *
 * Part of Sprint 9 - Transaction Analytics (AV11-177)
 *
 * @author Backend Development Agent (BDA)
 * @version 11.0.0
 * @since Sprint 9
 */
@ApplicationScoped
public class AnalyticsCache {

    private static final Logger LOG = Logger.getLogger(AnalyticsCache.class);

    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();
    private static final long DEFAULT_TTL_SECONDS = 300; // 5 minutes

    /**
     * Get cached value
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        CacheEntry entry = cache.get(key);
        if (entry == null) {
            LOG.tracef("Cache miss for key: %s", key);
            return null;
        }

        if (entry.isExpired()) {
            cache.remove(key);
            LOG.tracef("Cache expired for key: %s", key);
            return null;
        }

        LOG.tracef("Cache hit for key: %s", key);
        return (T) entry.value;
    }

    /**
     * Put value in cache with default TTL
     */
    public void put(String key, Object value) {
        put(key, value, DEFAULT_TTL_SECONDS);
    }

    /**
     * Put value in cache with custom TTL
     */
    public void put(String key, Object value, long ttlSeconds) {
        Instant expiryTime = Instant.now().plusSeconds(ttlSeconds);
        cache.put(key, new CacheEntry(value, expiryTime));
        LOG.tracef("Cached key: %s with TTL: %d seconds", key, ttlSeconds);
    }

    /**
     * Remove value from cache
     */
    public void remove(String key) {
        cache.remove(key);
        LOG.tracef("Removed from cache: %s", key);
    }

    /**
     * Clear all cache entries
     */
    public void clear() {
        cache.clear();
        LOG.info("Cache cleared");
    }

    /**
     * Clear expired entries
     */
    public void clearExpired() {
        cache.entrySet().removeIf(entry -> entry.getValue().isExpired());
        LOG.debug("Cleared expired cache entries");
    }

    /**
     * Get cache size
     */
    public int size() {
        return cache.size();
    }

    /**
     * Check if key exists and is not expired
     */
    public boolean contains(String key) {
        CacheEntry entry = cache.get(key);
        return entry != null && !entry.isExpired();
    }

    /**
     * Cache entry with expiry
     */
    private static class CacheEntry {
        final Object value;
        final Instant expiryTime;

        CacheEntry(Object value, Instant expiryTime) {
            this.value = value;
            this.expiryTime = expiryTime;
        }

        boolean isExpired() {
            return Instant.now().isAfter(expiryTime);
        }
    }
}
