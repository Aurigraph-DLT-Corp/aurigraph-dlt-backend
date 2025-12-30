package io.aurigraph.v11.performance;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Interface for high-performance caching operations in Aurigraph V11.
 * 
 * This service provides advanced caching capabilities designed to support
 * the platform's 2M+ TPS target through intelligent data caching, memory
 * optimization, and distributed cache coordination.
 * 
 * Key Features:
 * - Multi-level cache hierarchy (L1: Memory, L2: SSD, L3: Network)
 * - Intelligent cache eviction policies (LRU, LFU, TTL, Custom)
 * - Distributed cache coordination across nodes
 * - Real-time cache performance monitoring
 * - Adaptive cache sizing based on workload patterns
 * - Write-through and write-behind caching strategies
 * 
 * Performance Requirements:
 * - Sub-millisecond cache hit response times
 * - Support for 10M+ cached objects
 * - 99.9% cache availability
 * - Intelligent prefetching for predictive caching
 * 
 * @author Aurigraph DLT Platform
 * @version 11.0.0
 * @since 2025-01-01
 */
@ApplicationScoped
public interface CacheManager {

    /**
     * Stores a value in the cache with specified expiration.
     * 
     * @param key the cache key
     * @param value the value to cache
     * @param ttl time-to-live for the cached entry
     * @param cacheLevel the cache level to use
     * @return Uni indicating success or failure of the operation
     */
    <T> Uni<Boolean> put(String key, T value, Duration ttl, CacheLevel cacheLevel);

    /**
     * Stores a value in the cache with default expiration.
     * 
     * @param key the cache key
     * @param value the value to cache
     * @return Uni indicating success or failure of the operation
     */
    <T> Uni<Boolean> put(String key, T value);

    /**
     * Retrieves a value from the cache.
     * 
     * @param key the cache key
     * @param valueClass the class type of the cached value
     * @return Uni containing the cached value or null if not found
     */
    <T> Uni<T> get(String key, Class<T> valueClass);

    /**
     * Retrieves multiple values from the cache in a single operation.
     * 
     * @param keys the list of cache keys
     * @param valueClass the class type of the cached values
     * @return Uni containing a map of key-value pairs
     */
    <T> Uni<Map<String, T>> getMultiple(List<String> keys, Class<T> valueClass);

    /**
     * Removes a value from the cache.
     * 
     * @param key the cache key to remove
     * @return Uni indicating success or failure of the operation
     */
    Uni<Boolean> remove(String key);

    /**
     * Removes multiple values from the cache.
     * 
     * @param keys the list of cache keys to remove
     * @return Uni containing the number of keys successfully removed
     */
    Uni<Long> removeMultiple(List<String> keys);

    /**
     * Checks if a key exists in the cache.
     * 
     * @param key the cache key to check
     * @return Uni containing true if the key exists, false otherwise
     */
    Uni<Boolean> exists(String key);

    /**
     * Gets the remaining time-to-live for a cached entry.
     * 
     * @param key the cache key
     * @return Uni containing the remaining TTL in milliseconds
     */
    Uni<Long> getTtl(String key);

    /**
     * Updates the expiration time for a cached entry.
     * 
     * @param key the cache key
     * @param ttl the new time-to-live
     * @return Uni indicating success or failure of the operation
     */
    Uni<Boolean> expire(String key, Duration ttl);

    /**
     * Stores a value with conditional logic (only if key doesn't exist).
     * 
     * @param key the cache key
     * @param value the value to cache
     * @param ttl time-to-live for the cached entry
     * @return Uni indicating whether the value was stored (true) or key already existed (false)
     */
    <T> Uni<Boolean> putIfAbsent(String key, T value, Duration ttl);

    /**
     * Atomically replaces a cached value if it matches the expected value.
     * 
     * @param key the cache key
     * @param expectedValue the expected current value
     * @param newValue the new value to store
     * @return Uni indicating whether the replacement occurred
     */
    <T> Uni<Boolean> compareAndSet(String key, T expectedValue, T newValue);

    /**
     * Incrementally updates a numeric value in the cache.
     * 
     * @param key the cache key
     * @param delta the increment value (can be negative for decrement)
     * @return Uni containing the new value after increment
     */
    Uni<Long> increment(String key, long delta);

    /**
     * Gets cache statistics for performance monitoring.
     * 
     * @param cacheLevel the cache level to get statistics for (null for all levels)
     * @return Uni containing comprehensive cache statistics
     */
    Uni<CacheStatistics> getStatistics(CacheLevel cacheLevel);

    /**
     * Clears all entries from a specific cache level or pattern.
     * 
     * @param pattern the key pattern to match (supports wildcards)
     * @param cacheLevel the cache level to clear from
     * @return Uni containing the number of entries cleared
     */
    Uni<Long> clear(String pattern, CacheLevel cacheLevel);

    /**
     * Gets all keys matching a specific pattern.
     * 
     * @param pattern the key pattern to match (supports wildcards)
     * @param cacheLevel the cache level to search in
     * @return Multi streaming matching keys
     */
    Multi<String> getKeys(String pattern, CacheLevel cacheLevel);

    /**
     * Gets the size of cached data for monitoring memory usage.
     * 
     * @param cacheLevel the cache level to check
     * @return Uni containing the size in bytes
     */
    Uni<Long> getSize(CacheLevel cacheLevel);

    /**
     * Optimizes cache performance by reorganizing data and cleaning up.
     * 
     * @param optimizationType the type of optimization to perform
     * @return Uni containing the optimization result
     */
    Uni<CacheOptimizationResult> optimize(OptimizationType optimizationType);

    /**
     * Sets up cache warming with frequently accessed data.
     * 
     * @param warmupConfig configuration for cache warming
     * @return Uni containing the warmup result
     */
    Uni<CacheWarmupResult> warmupCache(CacheWarmupConfig warmupConfig);

    /**
     * Monitors cache performance in real-time.
     * 
     * @param monitoringInterval the interval between monitoring updates
     * @return Multi streaming real-time cache performance data
     */
    Multi<CachePerformanceData> monitorPerformance(Duration monitoringInterval);

    /**
     * Configures cache eviction policies for optimal memory management.
     * 
     * @param cacheLevel the cache level to configure
     * @param evictionPolicy the eviction policy to apply
     * @return Uni indicating success or failure of the configuration
     */
    Uni<Boolean> configureEvictionPolicy(CacheLevel cacheLevel, EvictionPolicy evictionPolicy);

    /**
     * Creates a cache partition for isolating specific data types.
     * 
     * @param partitionName the name of the cache partition
     * @param partitionConfig configuration for the partition
     * @return Uni containing the partition creation result
     */
    Uni<CachePartitionResult> createPartition(String partitionName, CachePartitionConfig partitionConfig);

    /**
     * Enables or disables cache compression for memory optimization.
     * 
     * @param cacheLevel the cache level to configure
     * @param compressionType the type of compression to use
     * @param enabled whether to enable or disable compression
     * @return Uni indicating success or failure of the configuration
     */
    Uni<Boolean> configureCompression(CacheLevel cacheLevel, CompressionType compressionType, boolean enabled);

    /**
     * Sets up intelligent cache prefetching based on access patterns.
     * 
     * @param prefetchingConfig configuration for prefetching behavior
     * @return Uni containing the prefetching setup result
     */
    Uni<CachePrefetchingResult> setupPrefetching(CachePrefetchingConfig prefetchingConfig);

    /**
     * Gets cache health status including any issues or warnings.
     * 
     * @return Uni containing comprehensive cache health information
     */
    Uni<CacheHealthStatus> getHealthStatus();

    // Inner classes and enums for data transfer objects

    /**
     * Cache levels representing the cache hierarchy.
     */
    public enum CacheLevel {
        L1_MEMORY,      // In-memory cache for fastest access
        L2_SSD,         // SSD-based cache for medium-term storage
        L3_NETWORK,     // Distributed network cache
        ALL_LEVELS      // Operation applies to all levels
    }

    /**
     * Comprehensive cache statistics.
     */
    public static class CacheStatistics {
        public CacheLevel cacheLevel;
        public long totalRequests;
        public long cacheHits;
        public long cacheMisses;
        public double hitRatio;
        public long totalKeys;
        public long totalSizeBytes;
        public long maxSizeBytes;
        public double memoryUtilization;
        public long evictedEntries;
        public long expiredEntries;
        public AverageResponseTime averageResponseTime;
        public Map<String, Object> additionalMetrics;
        public long timestamp;
    }

    /**
     * Average response times for different operations.
     */
    public static class AverageResponseTime {
        public double getOperationMs;
        public double putOperationMs;
        public double removeOperationMs;
        public double multiGetOperationMs;
    }

    /**
     * Types of cache optimization.
     */
    public enum OptimizationType {
        MEMORY_DEFRAGMENTATION,
        EVICTION_CLEANUP,
        INDEX_REBUILD,
        COMPRESSION_OPTIMIZATION,
        PARTITION_REBALANCING,
        FULL_OPTIMIZATION
    }

    /**
     * Result of cache optimization operation.
     */
    public static class CacheOptimizationResult {
        public OptimizationType optimizationType;
        public long memoryFreedBytes;
        public long entriesOptimized;
        public long optimizationTimeMs;
        public double performanceImprovement;
        public List<String> optimizationDetails;
        public boolean success;
        public String errorMessage;
    }

    /**
     * Configuration for cache warming.
     */
    public static class CacheWarmupConfig {
        public List<String> keyPatterns;
        public CacheLevel targetLevel;
        public int maxEntriesPerPattern;
        public Duration warmupTimeout;
        public boolean enablePrefetching;
        public WarmupStrategy strategy;
    }

    /**
     * Strategies for cache warming.
     */
    public enum WarmupStrategy {
        SEQUENTIAL,     // Load entries sequentially
        PARALLEL,       // Load entries in parallel
        PRIORITY_BASED, // Load high-priority entries first
        ADAPTIVE        // Adapt based on system load
    }

    /**
     * Result of cache warming operation.
     */
    public static class CacheWarmupResult {
        public int entriesLoaded;
        public int entriesFailed;
        public long warmupTimeMs;
        public Map<String, Integer> entriesPerPattern;
        public boolean success;
        public String errorMessage;
    }

    /**
     * Real-time cache performance data.
     */
    public static class CachePerformanceData {
        public long timestamp;
        public Map<CacheLevel, CacheStatistics> levelStatistics;
        public double overallHitRatio;
        public double throughputOpsPerSecond;
        public double averageLatencyMs;
        public double memoryPressure; // 0.0 to 1.0
        public List<PerformanceAlert> alerts;
    }

    /**
     * Performance alerts for cache monitoring.
     */
    public static class PerformanceAlert {
        public AlertSeverity severity;
        public String alertType;
        public String description;
        public String recommendation;
        public long timestamp;
    }

    /**
     * Severity levels for performance alerts.
     */
    public enum AlertSeverity {
        INFO,
        WARNING,
        ERROR,
        CRITICAL
    }

    /**
     * Cache eviction policies.
     */
    public static class EvictionPolicy {
        public EvictionStrategy strategy;
        public Map<String, Object> parameters;
        
        public EvictionPolicy(EvictionStrategy strategy) {
            this.strategy = strategy;
            this.parameters = Map.of();
        }
    }

    /**
     * Available eviction strategies.
     */
    public enum EvictionStrategy {
        LRU,            // Least Recently Used
        LFU,            // Least Frequently Used
        FIFO,           // First In, First Out
        LIFO,           // Last In, First Out
        TTL_BASED,      // Time-to-Live based
        CUSTOM,         // Custom strategy
        ADAPTIVE        // Adaptive based on access patterns
    }

    /**
     * Configuration for cache partitions.
     */
    public static class CachePartitionConfig {
        public long maxSizeBytes;
        public Duration defaultTtl;
        public EvictionPolicy evictionPolicy;
        public CompressionType compressionType;
        public boolean enableStatistics;
        public Map<String, Object> customSettings;
    }

    /**
     * Result of cache partition creation.
     */
    public static class CachePartitionResult {
        public String partitionName;
        public boolean created;
        public long allocatedSizeBytes;
        public String errorMessage;
    }

    /**
     * Types of cache compression.
     */
    public enum CompressionType {
        NONE,
        LZ4,            // Fast compression
        GZIP,           // Standard compression
        SNAPPY,         // Google's compression
        ZSTD,           // Facebook's Zstandard
        ADAPTIVE        // Adaptive based on data type
    }

    /**
     * Configuration for cache prefetching.
     */
    public static class CachePrefetchingConfig {
        public boolean enableAccessPatternAnalysis;
        public double predictionAccuracyThreshold;
        public int maxPrefetchBatchSize;
        public Duration prefetchAheadTime;
        public List<PrefetchingRule> prefetchingRules;
    }

    /**
     * Rules for intelligent prefetching.
     */
    public static class PrefetchingRule {
        public String keyPattern;
        public PrefetchingStrategy strategy;
        public Map<String, Object> parameters;
    }

    /**
     * Prefetching strategies.
     */
    public enum PrefetchingStrategy {
        SEQUENTIAL_ACCESS,  // Prefetch sequential keys
        RELATED_KEYS,      // Prefetch related keys
        TIME_BASED,        // Prefetch based on time patterns
        MACHINE_LEARNING   // ML-based prediction
    }

    /**
     * Result of prefetching setup.
     */
    public static class CachePrefetchingResult {
        public boolean enabled;
        public int rulesConfigured;
        public double estimatedHitRatioImprovement;
        public String errorMessage;
    }

    /**
     * Health status of the cache system.
     */
    public static class CacheHealthStatus {
        public HealthLevel overallHealth;
        public Map<CacheLevel, HealthLevel> levelHealth;
        public List<HealthIssue> issues;
        public List<HealthWarning> warnings;
        public CacheStatistics currentStats;
        public long lastHealthCheckTime;
    }

    /**
     * Health levels for cache status.
     */
    public enum HealthLevel {
        EXCELLENT,  // All systems optimal
        GOOD,       // Minor issues, good performance
        FAIR,       // Some issues, degraded performance
        POOR,       // Major issues, significant degradation
        CRITICAL    // System failure imminent
    }

    /**
     * Health issues affecting cache performance.
     */
    public static class HealthIssue {
        public IssueSeverity severity;
        public String issueType;
        public String description;
        public String impact;
        public List<String> recommendedActions;
        public long detectedTime;
    }

    /**
     * Severity levels for health issues.
     */
    public enum IssueSeverity {
        LOW,
        MEDIUM,
        HIGH,
        CRITICAL
    }

    /**
     * Health warnings for preventive maintenance.
     */
    public static class HealthWarning {
        public String warningType;
        public String description;
        public double threshold;
        public double currentValue;
        public String recommendation;
        public long detectedTime;
    }
}