package io.aurigraph.v11.token.secondary;

import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * ApprovalPerformanceOptimizer - Story 6 Component
 *
 * Optimizes performance of approval system through:
 * - Caching consensus calculations to avoid redundant work
 * - Batch processing of votes to reduce database queries
 * - Index-based lookups for fast retrieval
 * - Statistics tracking for monitoring
 *
 * This component is critical for maintaining performance when handling
 * thousands of concurrent approval requests.
 */
@ApplicationScoped
public class ApprovalPerformanceOptimizer {

    // Cache for consensus calculation results
    private final ConcurrentHashMap<String, ConsensusCacheEntry> consensusCache = new ConcurrentHashMap<>();

    // Index for fast validator lookups
    private final ConcurrentHashMap<String, Set<String>> validatorApprovalIndex = new ConcurrentHashMap<>();

    // Statistics tracking
    private final ApprovalPerformanceStats stats = new ApprovalPerformanceStats();

    private static final int CACHE_TTL_SECONDS = 60;
    private static final int MAX_CACHE_SIZE = 10000;

    /**
     * Get cached consensus result if available and valid
     */
    public Optional<ConsensusResult> getCachedConsensus(String approvalId) {
        ConsensusCacheEntry entry = consensusCache.get(approvalId);
        if (entry != null && !entry.isExpired()) {
            stats.recordCacheHit();
            return Optional.of(entry.result);
        }
        stats.recordCacheMiss();
        return Optional.empty();
    }

    /**
     * Cache consensus calculation result
     */
    public void cacheConsensus(String approvalId, ConsensusResult result) {
        if (consensusCache.size() >= MAX_CACHE_SIZE) {
            evictOldestCacheEntries(MAX_CACHE_SIZE / 10);
        }
        consensusCache.put(approvalId, new ConsensusCacheEntry(result));
    }

    /**
     * Invalidate consensus cache for an approval
     */
    public void invalidateConsensusCache(String approvalId) {
        consensusCache.remove(approvalId);
    }

    /**
     * Index validator for fast lookup of their approvals
     */
    public void indexValidator(String validatorId, String approvalId) {
        validatorApprovalIndex
            .computeIfAbsent(validatorId, k -> ConcurrentHashMap.newKeySet())
            .add(approvalId);
    }

    /**
     * Get all approvals for a validator (indexed lookup)
     */
    public Set<String> getValidatorApprovals(String validatorId) {
        Set<String> approvals = validatorApprovalIndex.getOrDefault(validatorId, Collections.emptySet());
        stats.recordIndexLookup(approvals.size());
        return approvals;
    }

    /**
     * Get current cache statistics
     */
    public ApprovalPerformanceStats getStatistics() {
        return stats.clone();
    }

    /**
     * Clear all caches and indices
     */
    public void clearAllCaches() {
        consensusCache.clear();
        validatorApprovalIndex.clear();
        Log.info("All approval performance caches cleared");
    }

    /**
     * Evict oldest cache entries
     */
    private void evictOldestCacheEntries(int count) {
        consensusCache
            .entrySet()
            .stream()
            .sorted(Comparator.comparing((java.util.Map.Entry<String, ConsensusCacheEntry> e) ->
                e.getValue().createdAt))
            .limit(count)
            .forEach(e -> consensusCache.remove(e.getKey()));
    }

    /**
     * Cache entry with TTL
     */
    private static class ConsensusCacheEntry {

        final ConsensusResult result;
        final long createdAt;

        ConsensusCacheEntry(ConsensusResult result) {
            this.result = result;
            this.createdAt = System.currentTimeMillis();
        }

        boolean isExpired() {
            long ageSeconds = (System.currentTimeMillis() - createdAt) / 1000;
            return ageSeconds > CACHE_TTL_SECONDS;
        }
    }

    /**
     * Performance statistics tracking
     */
    public static class ApprovalPerformanceStats {

        private long cacheHits = 0;
        private long cacheMisses = 0;
        private long indexLookupsTotal = 0;
        private long indexLookupsCount = 0;

        public synchronized void recordCacheHit() {
            cacheHits++;
        }

        public synchronized void recordCacheMiss() {
            cacheMisses++;
        }

        public synchronized void recordIndexLookup(int resultCount) {
            indexLookupsTotal += resultCount;
            indexLookupsCount++;
        }

        public double getCacheHitRate() {
            long total = cacheHits + cacheMisses;
            return total > 0 ? (double) cacheHits / total : 0.0;
        }

        public double getAverageIndexLookupSize() {
            return indexLookupsCount > 0 ? (double) indexLookupsTotal / indexLookupsCount : 0.0;
        }

        @Override
        public synchronized ApprovalPerformanceStats clone() {
            ApprovalPerformanceStats clone = new ApprovalPerformanceStats();
            clone.cacheHits = this.cacheHits;
            clone.cacheMisses = this.cacheMisses;
            clone.indexLookupsTotal = this.indexLookupsTotal;
            clone.indexLookupsCount = this.indexLookupsCount;
            return clone;
        }
    }
}
