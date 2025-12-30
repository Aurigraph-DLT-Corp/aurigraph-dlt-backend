package io.aurigraph.v11.demo.config;

import io.aurigraph.v11.demo.models.NodeType;
import io.quarkus.runtime.annotations.RegisterForReflection;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration specific to API Integration Nodes.
 *
 * API Integration Nodes are responsible for:
 * - Connecting to external APIs (Alpaca, Weather, etc.)
 * - Fetching and validating external data
 * - Caching external data for performance
 * - Providing oracle services to the blockchain
 * - Handling API rate limiting and backoff strategies
 *
 * Performance Targets:
 * - API call latency: <100ms (with caching)
 * - Cache hit rate: >90%
 * - Data freshness: <5s for critical data
 * - Throughput: 10K external API calls/sec
 */
@RegisterForReflection
public class APINodeConfig extends NodeConfiguration {

    /**
     * Configuration for external API integrations.
     * Key: API identifier (e.g., "alpaca", "weather")
     * Value: API-specific configuration
     */
    private Map<String, APIConfig> apiConfigs = new HashMap<>();

    /**
     * Cache size for API response data.
     * Format: number + unit (e.g., "5GB", "1024MB")
     * Default: "5GB"
     */
    private String cacheSize = "5GB";

    /**
     * Cache TTL (Time To Live) in seconds.
     * How long cached data remains valid.
     * Default: 300 seconds (5 minutes)
     */
    private int cacheTTL = 300;

    /**
     * Enable oracle service for providing external data to smart contracts.
     * Default: true
     */
    private boolean enableOracleService = true;

    /**
     * Oracle data verification mode.
     * Options: "single_source", "multi_source", "consensus"
     * Default: "multi_source"
     */
    private String oracleVerificationMode = "multi_source";

    /**
     * Maximum concurrent API requests.
     * Default: 1,000 requests
     */
    private int maxConcurrentRequests = 1000;

    /**
     * Request timeout in milliseconds.
     * Default: 5,000ms (5 seconds)
     */
    private int requestTimeout = 5000;

    /**
     * Enable circuit breaker pattern for fault tolerance.
     * Prevents cascading failures when external APIs are down.
     * Default: true
     */
    private boolean enableCircuitBreaker = true;

    /**
     * Circuit breaker threshold (number of failures before opening).
     * Default: 5 failures
     */
    private int circuitBreakerThreshold = 5;

    /**
     * Circuit breaker timeout in milliseconds (how long to wait before retry).
     * Default: 30,000ms (30 seconds)
     */
    private int circuitBreakerTimeout = 30000;

    /**
     * Enable exponential backoff for retries.
     * Default: true
     */
    private boolean enableExponentialBackoff = true;

    /**
     * Maximum retry attempts for failed API calls.
     * Default: 3 attempts
     */
    private int maxRetryAttempts = 3;

    /**
     * Initial backoff delay in milliseconds.
     * Default: 1,000ms (1 second)
     */
    private int initialBackoffDelay = 1000;

    /**
     * Enable response validation to ensure data integrity.
     * Default: true
     */
    private boolean enableResponseValidation = true;

    /**
     * Enable API metrics collection.
     * Tracks success rates, latencies, cache hit rates, etc.
     * Default: true
     */
    private boolean enableMetrics = true;

    // Constructors

    public APINodeConfig() {
        super();
        setNodeType(NodeType.API_INTEGRATION);
        initializeDefaultAPIs();
    }

    public APINodeConfig(String nodeId) {
        super(nodeId, NodeType.API_INTEGRATION);
        initializeDefaultAPIs();
    }

    /**
     * Initialize default API configurations for common integrations.
     */
    private void initializeDefaultAPIs() {
        // Alpaca Markets configuration
        apiConfigs.put("alpaca", new APIConfig(
            "https://api.alpaca.markets",
            200,
            "encrypted"
        ));

        // Weather.com configuration
        apiConfigs.put("weather", new APIConfig(
            "https://api.weather.com",
            100,
            "encrypted"
        ));
    }

    // Getters and Setters

    public Map<String, APIConfig> getApiConfigs() {
        return apiConfigs;
    }

    public void setApiConfigs(Map<String, APIConfig> apiConfigs) {
        this.apiConfigs = apiConfigs;
    }

    public String getCacheSize() {
        return cacheSize;
    }

    public void setCacheSize(String cacheSize) {
        this.cacheSize = cacheSize;
    }

    public int getCacheTTL() {
        return cacheTTL;
    }

    public void setCacheTTL(int cacheTTL) {
        this.cacheTTL = cacheTTL;
    }

    public boolean isEnableOracleService() {
        return enableOracleService;
    }

    public void setEnableOracleService(boolean enableOracleService) {
        this.enableOracleService = enableOracleService;
    }

    public String getOracleVerificationMode() {
        return oracleVerificationMode;
    }

    public void setOracleVerificationMode(String oracleVerificationMode) {
        this.oracleVerificationMode = oracleVerificationMode;
    }

    public int getMaxConcurrentRequests() {
        return maxConcurrentRequests;
    }

    public void setMaxConcurrentRequests(int maxConcurrentRequests) {
        this.maxConcurrentRequests = maxConcurrentRequests;
    }

    public int getRequestTimeout() {
        return requestTimeout;
    }

    public void setRequestTimeout(int requestTimeout) {
        this.requestTimeout = requestTimeout;
    }

    public boolean isEnableCircuitBreaker() {
        return enableCircuitBreaker;
    }

    public void setEnableCircuitBreaker(boolean enableCircuitBreaker) {
        this.enableCircuitBreaker = enableCircuitBreaker;
    }

    public int getCircuitBreakerThreshold() {
        return circuitBreakerThreshold;
    }

    public void setCircuitBreakerThreshold(int circuitBreakerThreshold) {
        this.circuitBreakerThreshold = circuitBreakerThreshold;
    }

    public int getCircuitBreakerTimeout() {
        return circuitBreakerTimeout;
    }

    public void setCircuitBreakerTimeout(int circuitBreakerTimeout) {
        this.circuitBreakerTimeout = circuitBreakerTimeout;
    }

    public boolean isEnableExponentialBackoff() {
        return enableExponentialBackoff;
    }

    public void setEnableExponentialBackoff(boolean enableExponentialBackoff) {
        this.enableExponentialBackoff = enableExponentialBackoff;
    }

    public int getMaxRetryAttempts() {
        return maxRetryAttempts;
    }

    public void setMaxRetryAttempts(int maxRetryAttempts) {
        this.maxRetryAttempts = maxRetryAttempts;
    }

    public int getInitialBackoffDelay() {
        return initialBackoffDelay;
    }

    public void setInitialBackoffDelay(int initialBackoffDelay) {
        this.initialBackoffDelay = initialBackoffDelay;
    }

    public boolean isEnableResponseValidation() {
        return enableResponseValidation;
    }

    public void setEnableResponseValidation(boolean enableResponseValidation) {
        this.enableResponseValidation = enableResponseValidation;
    }

    public boolean isEnableMetrics() {
        return enableMetrics;
    }

    public void setEnableMetrics(boolean enableMetrics) {
        this.enableMetrics = enableMetrics;
    }

    @Override
    public void validate() {
        // Validate base configuration first
        super.validate();

        // Validate API node-specific configuration
        if (apiConfigs == null || apiConfigs.isEmpty()) {
            throw new IllegalArgumentException("At least one API configuration must be provided");
        }

        // Validate each API configuration
        for (Map.Entry<String, APIConfig> entry : apiConfigs.entrySet()) {
            String apiName = entry.getKey();
            APIConfig config = entry.getValue();

            if (config == null) {
                throw new IllegalArgumentException("API configuration for '" + apiName + "' cannot be null");
            }

            try {
                config.validate();
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid configuration for API '" + apiName + "': " + e.getMessage());
            }
        }

        if (!cacheSize.matches("\\d+[KMG]B")) {
            throw new IllegalArgumentException("Cache size must be in format: number + KB/MB/GB (e.g., '5GB')");
        }

        if (cacheTTL <= 0) {
            throw new IllegalArgumentException("Cache TTL must be greater than 0");
        }

        if (!oracleVerificationMode.matches("single_source|multi_source|consensus")) {
            throw new IllegalArgumentException("Oracle verification mode must be one of: single_source, multi_source, consensus");
        }

        if (maxConcurrentRequests <= 0) {
            throw new IllegalArgumentException("Max concurrent requests must be greater than 0");
        }

        if (requestTimeout <= 0) {
            throw new IllegalArgumentException("Request timeout must be greater than 0");
        }

        if (enableCircuitBreaker) {
            if (circuitBreakerThreshold <= 0) {
                throw new IllegalArgumentException("Circuit breaker threshold must be greater than 0 when enabled");
            }

            if (circuitBreakerTimeout <= 0) {
                throw new IllegalArgumentException("Circuit breaker timeout must be greater than 0 when enabled");
            }
        }

        if (enableExponentialBackoff) {
            if (maxRetryAttempts <= 0) {
                throw new IllegalArgumentException("Max retry attempts must be greater than 0 when backoff is enabled");
            }

            if (initialBackoffDelay <= 0) {
                throw new IllegalArgumentException("Initial backoff delay must be greater than 0 when backoff is enabled");
            }
        }
    }

    @Override
    public String toString() {
        return "APINodeConfig{" +
               "nodeId='" + getNodeId() + '\'' +
               ", apiCount=" + apiConfigs.size() +
               ", cacheSize='" + cacheSize + '\'' +
               ", cacheTTL=" + cacheTTL +
               ", enableOracleService=" + enableOracleService +
               ", oracleVerificationMode='" + oracleVerificationMode + '\'' +
               '}';
    }

    /**
     * Configuration for a specific external API.
     */
    @RegisterForReflection
    public static class APIConfig {
        private String baseUrl;
        private int rateLimit;
        private String apiKey;

        public APIConfig() {
            // Default constructor for Jackson
        }

        public APIConfig(String baseUrl, int rateLimit, String apiKey) {
            this.baseUrl = baseUrl;
            this.rateLimit = rateLimit;
            this.apiKey = apiKey;
        }

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public int getRateLimit() {
            return rateLimit;
        }

        public void setRateLimit(int rateLimit) {
            this.rateLimit = rateLimit;
        }

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public void validate() {
            if (baseUrl == null || baseUrl.trim().isEmpty()) {
                throw new IllegalArgumentException("API base URL cannot be null or empty");
            }

            if (!baseUrl.startsWith("http://") && !baseUrl.startsWith("https://")) {
                throw new IllegalArgumentException("API base URL must start with http:// or https://");
            }

            if (rateLimit <= 0) {
                throw new IllegalArgumentException("API rate limit must be greater than 0");
            }

            if (apiKey == null || apiKey.trim().isEmpty()) {
                throw new IllegalArgumentException("API key cannot be null or empty");
            }
        }

        @Override
        public String toString() {
            return "APIConfig{" +
                   "baseUrl='" + baseUrl + '\'' +
                   ", rateLimit=" + rateLimit +
                   ", apiKey='***'" + // Don't expose API key in logs
                   '}';
        }
    }
}
