package io.aurigraph.v11.performance;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Interface for intelligent load balancing and traffic distribution in Aurigraph V11.
 * 
 * This service provides advanced load balancing capabilities designed to optimize
 * traffic distribution across nodes and services to achieve the platform's 2M+ TPS
 * target with optimal resource utilization and minimal latency.
 * 
 * Key Features:
 * - Multiple load balancing algorithms (Round Robin, Weighted, Least Connections, etc.)
 * - Real-time health monitoring and automatic failover
 * - Adaptive load balancing based on performance metrics
 * - Circuit breaker pattern implementation
 * - Geographic and affinity-based routing
 * - Dynamic scaling and auto-provisioning integration
 * 
 * Performance Requirements:
 * - Route 2M+ requests per second with sub-millisecond overhead
 * - Support 1000+ backend nodes/services
 * - Achieve 99.99% availability through intelligent failover
 * - Maintain optimal resource utilization across all nodes
 * 
 * @author Aurigraph DLT Platform
 * @version 11.0.0
 * @since 2025-01-01
 */
@ApplicationScoped
public interface LoadBalancer {

    /**
     * Routes a request to the optimal backend service based on current load balancing strategy.
     * 
     * @param request the request to route
     * @param routingConfig configuration for request routing
     * @return Uni containing the routing result
     */
    Uni<RoutingResult> routeRequest(ServiceRequest request, RoutingConfig routingConfig);

    /**
     * Routes multiple requests in batch for optimal performance.
     * 
     * @param requests the batch of requests to route
     * @param batchConfig configuration for batch routing
     * @return Uni containing the batch routing result
     */
    Uni<BatchRoutingResult> routeBatch(List<ServiceRequest> requests, BatchRoutingConfig batchConfig);

    /**
     * Registers a new backend service or node for load balancing.
     * 
     * @param serviceEndpoint the service endpoint to register
     * @param serviceConfig configuration for the service
     * @return Uni indicating success or failure of registration
     */
    Uni<Boolean> registerService(ServiceEndpoint serviceEndpoint, ServiceConfig serviceConfig);

    /**
     * Unregisters a backend service or node from load balancing.
     * 
     * @param serviceId the identifier of the service to unregister
     * @param drainConnections whether to drain existing connections before removal
     * @return Uni containing the unregistration result
     */
    Uni<UnregistrationResult> unregisterService(String serviceId, boolean drainConnections);

    /**
     * Updates the configuration for an existing service.
     * 
     * @param serviceId the identifier of the service to update
     * @param newConfig the new configuration for the service
     * @return Uni indicating success or failure of the update
     */
    Uni<Boolean> updateServiceConfig(String serviceId, ServiceConfig newConfig);

    /**
     * Gets the current health status of all registered services.
     * 
     * @return Multi streaming health status for all services
     */
    Multi<ServiceHealthStatus> getServicesHealthStatus();

    /**
     * Gets detailed load balancing statistics.
     * 
     * @param timeWindow the time window for statistics calculation
     * @return Uni containing comprehensive load balancing statistics
     */
    Uni<LoadBalancingStatistics> getStatistics(Duration timeWindow);

    /**
     * Configures the load balancing algorithm and parameters.
     * 
     * @param algorithm the load balancing algorithm to use
     * @param algorithmConfig configuration specific to the chosen algorithm
     * @return Uni indicating success or failure of the configuration
     */
    Uni<Boolean> configureLoadBalancing(LoadBalancingAlgorithm algorithm, AlgorithmConfig algorithmConfig);

    /**
     * Enables or disables circuit breaker for a specific service.
     * 
     * @param serviceId the identifier of the service
     * @param circuitBreakerConfig configuration for the circuit breaker
     * @return Uni containing the circuit breaker setup result
     */
    Uni<CircuitBreakerResult> configureCircuitBreaker(String serviceId, CircuitBreakerConfig circuitBreakerConfig);

    /**
     * Monitors load balancing performance in real-time.
     * 
     * @param monitoringInterval the interval between monitoring updates
     * @return Multi streaming real-time load balancing performance data
     */
    Multi<LoadBalancingMetrics> monitorPerformance(Duration monitoringInterval);

    /**
     * Sets up automatic failover policies for high availability.
     * 
     * @param failoverPolicy the failover policy configuration
     * @return Uni containing the failover setup result
     */
    Uni<FailoverSetupResult> configureFailover(FailoverPolicy failoverPolicy);

    /**
     * Triggers manual failover to backup services.
     * 
     * @param primaryServiceId the primary service to failover from
     * @param backupServiceId the backup service to failover to (null for automatic selection)
     * @return Uni containing the failover execution result
     */
    Uni<FailoverExecutionResult> executeFailover(String primaryServiceId, String backupServiceId);

    /**
     * Optimizes load distribution based on current performance metrics.
     * 
     * @param optimizationConfig configuration for load optimization
     * @return Uni containing the optimization result
     */
    Uni<LoadOptimizationResult> optimizeLoadDistribution(LoadOptimizationConfig optimizationConfig);

    /**
     * Sets up geographic routing for global load distribution.
     * 
     * @param geoRoutingConfig configuration for geographic routing
     * @return Uni containing the geographic routing setup result
     */
    Uni<GeoRoutingSetupResult> configureGeographicRouting(GeographicRoutingConfig geoRoutingConfig);

    /**
     * Configures session affinity (sticky sessions) for stateful applications.
     * 
     * @param affinityConfig configuration for session affinity
     * @return Uni containing the affinity setup result
     */
    Uni<AffinitySetupResult> configureSessionAffinity(SessionAffinityConfig affinityConfig);

    /**
     * Gets the current routing table and service mappings.
     * 
     * @return Uni containing the current routing table
     */
    Uni<RoutingTable> getRoutingTable();

    /**
     * Predicts optimal load distribution for future traffic patterns.
     * 
     * @param predictionConfig configuration for load prediction
     * @return Uni containing load distribution predictions
     */
    Uni<LoadPrediction> predictLoadDistribution(LoadPredictionConfig predictionConfig);

    /**
     * Configures rate limiting per service to prevent overload.
     * 
     * @param serviceId the identifier of the service
     * @param rateLimitConfig configuration for rate limiting
     * @return Uni containing the rate limiting setup result
     */
    Uni<RateLimitSetupResult> configureRateLimit(String serviceId, RateLimitConfig rateLimitConfig);

    // Inner classes and enums for data transfer objects

    /**
     * Represents a service request to be routed.
     */
    public static class ServiceRequest {
        public String requestId;
        public String serviceType;
        public String clientId;
        public Map<String, String> headers;
        public byte[] payload;
        public RequestPriority priority;
        public String sourceRegion;
        public long timestamp;
        public Map<String, Object> metadata;

        public ServiceRequest(String requestId, String serviceType) {
            this.requestId = requestId;
            this.serviceType = serviceType;
            this.timestamp = System.currentTimeMillis();
            this.priority = RequestPriority.NORMAL;
        }
    }

    /**
     * Priority levels for request routing.
     */
    public enum RequestPriority {
        LOW,
        NORMAL,
        HIGH,
        CRITICAL
    }

    /**
     * Configuration for request routing.
     */
    public static class RoutingConfig {
        public LoadBalancingAlgorithm algorithm;
        public boolean enableHealthCheck;
        public Duration timeoutMs;
        public int maxRetries;
        public boolean enableCircuitBreaker;
        public Map<String, String> routingHints;
        public String preferredRegion;
    }

    /**
     * Result of request routing operation.
     */
    public static class RoutingResult {
        public String requestId;
        public ServiceEndpoint selectedEndpoint;
        public long routingTimeMs;
        public RoutingDecision decision;
        public boolean success;
        public String errorMessage;
        public Map<String, Object> routingMetadata;
    }

    /**
     * Information about routing decision.
     */
    public static class RoutingDecision {
        public LoadBalancingAlgorithm algorithmUsed;
        public String selectionReason;
        public double endpointScore;
        public List<ServiceEndpoint> consideredEndpoints;
        public Map<String, Object> decisionFactors;
    }

    /**
     * Configuration for batch routing.
     */
    public static class BatchRoutingConfig {
        public boolean enableParallelRouting;
        public int maxParallelism;
        public LoadBalancingAlgorithm algorithm;
        public boolean preserveOrder;
        public Duration batchTimeout;
    }

    /**
     * Result of batch routing operation.
     */
    public static class BatchRoutingResult {
        public int totalRequests;
        public int successfulRoutes;
        public int failedRoutes;
        public List<RoutingResult> routingResults;
        public long totalRoutingTimeMs;
        public Map<String, Integer> endpointDistribution;
        public boolean allSuccessful;
    }

    /**
     * Represents a backend service endpoint.
     */
    public static class ServiceEndpoint {
        public String serviceId;
        public String host;
        public int port;
        public String protocol; // HTTP, HTTPS, gRPC, etc.
        public String region;
        public String zone;
        public EndpointStatus status;
        public Map<String, String> tags;
        public long registeredTimestamp;

        public ServiceEndpoint(String serviceId, String host, int port, String protocol) {
            this.serviceId = serviceId;
            this.host = host;
            this.port = port;
            this.protocol = protocol;
            this.status = EndpointStatus.HEALTHY;
            this.registeredTimestamp = System.currentTimeMillis();
        }
    }

    /**
     * Status of service endpoints.
     */
    public enum EndpointStatus {
        HEALTHY,
        DEGRADED,
        UNHEALTHY,
        DRAINING,
        OFFLINE
    }

    /**
     * Configuration for service endpoints.
     */
    public static class ServiceConfig {
        public int weight; // Weight for weighted load balancing
        public int maxConnections;
        public Duration timeout;
        public HealthCheckConfig healthCheck;
        public Map<String, String> metadata;
        public boolean enableCircuitBreaker;
        public RateLimitConfig rateLimit;
    }

    /**
     * Health check configuration for services.
     */
    public static class HealthCheckConfig {
        public String healthCheckPath;
        public Duration interval;
        public Duration timeout;
        public int healthyThreshold;
        public int unhealthyThreshold;
        public Map<String, String> expectedHeaders;
        public int expectedStatusCode;
    }

    /**
     * Result of service unregistration.
     */
    public static class UnregistrationResult {
        public String serviceId;
        public boolean unregistered;
        public int drainedConnections;
        public Duration drainTime;
        public String errorMessage;
    }

    /**
     * Health status of a service.
     */
    public static class ServiceHealthStatus {
        public String serviceId;
        public EndpointStatus status;
        public double healthScore; // 0.0 to 1.0
        public long lastHealthCheck;
        public String healthCheckMessage;
        public ServiceMetrics metrics;
        public List<String> healthIssues;
    }

    /**
     * Metrics for individual services.
     */
    public static class ServiceMetrics {
        public int activeConnections;
        public double averageResponseTime;
        public double requestsPerSecond;
        public double errorRate;
        public double cpuUtilization;
        public double memoryUtilization;
        public long totalRequests;
        public long totalErrors;
    }

    /**
     * Comprehensive load balancing statistics.
     */
    public static class LoadBalancingStatistics {
        public long totalRequests;
        public long successfulRoutes;
        public long failedRoutes;
        public double averageRoutingTime;
        public double requestsPerSecond;
        public Map<String, Long> requestsByService;
        public Map<LoadBalancingAlgorithm, Long> requestsByAlgorithm;
        public ServiceDistribution serviceDistribution;
        public FailoverStatistics failoverStats;
        public long statisticsTimeWindow;
    }

    /**
     * Distribution of requests across services.
     */
    public static class ServiceDistribution {
        public Map<String, Double> requestPercentages;
        public Map<String, Double> responseTimesByService;
        public Map<String, Double> errorRatesByService;
        public double distributionBalance; // 0.0 (unbalanced) to 1.0 (perfectly balanced)
    }

    /**
     * Statistics about failover operations.
     */
    public static class FailoverStatistics {
        public int totalFailovers;
        public int successfulFailovers;
        public int failedFailovers;
        public double averageFailoverTime;
        public Map<String, Integer> failoversByService;
        public long lastFailoverTimestamp;
    }

    /**
     * Available load balancing algorithms.
     */
    public enum LoadBalancingAlgorithm {
        ROUND_ROBIN,
        WEIGHTED_ROUND_ROBIN,
        LEAST_CONNECTIONS,
        WEIGHTED_LEAST_CONNECTIONS,
        LEAST_RESPONSE_TIME,
        RESOURCE_BASED,
        GEOGRAPHIC,
        CONSISTENT_HASH,
        RANDOM,
        CUSTOM
    }

    /**
     * Configuration specific to load balancing algorithms.
     */
    public static class AlgorithmConfig {
        public LoadBalancingAlgorithm algorithm;
        public Map<String, Object> parameters;
        public Duration healthCheckInterval;
        public boolean enableDynamicWeights;
        public double convergenceThreshold;
    }

    /**
     * Configuration for circuit breaker functionality.
     */
    public static class CircuitBreakerConfig {
        public boolean enabled;
        public int failureThreshold;
        public Duration timeout;
        public int successThreshold;
        public Duration monitoringWindow;
        public List<String> monitoredErrorTypes;
    }

    /**
     * Result of circuit breaker configuration.
     */
    public static class CircuitBreakerResult {
        public String serviceId;
        public CircuitBreakerState state;
        public boolean configured;
        public String errorMessage;
    }

    /**
     * States of circuit breaker.
     */
    public enum CircuitBreakerState {
        CLOSED,      // Normal operation
        OPEN,        // Failing requests immediately
        HALF_OPEN    // Testing if service has recovered
    }

    /**
     * Real-time load balancing metrics.
     */
    public static class LoadBalancingMetrics {
        public long timestamp;
        public double currentRPS;
        public double averageLatency;
        public Map<String, ServiceMetrics> serviceMetrics;
        public LoadDistribution currentDistribution;
        public List<String> activeAlerts;
        public SystemLoad systemLoad;
    }

    /**
     * Current load distribution across services.
     */
    public static class LoadDistribution {
        public Map<String, Double> serviceLoadPercentages;
        public double balanceScore; // 0.0 to 1.0
        public String distributionPattern;
        public List<String> overloadedServices;
        public List<String> underutilizedServices;
    }

    /**
     * Overall system load information.
     */
    public static class SystemLoad {
        public double overallCpuUtilization;
        public double overallMemoryUtilization;
        public double overallNetworkUtilization;
        public int totalActiveConnections;
        public LoadLevel loadLevel;
    }

    /**
     * System load levels.
     */
    public enum LoadLevel {
        LOW,
        NORMAL,
        HIGH,
        CRITICAL
    }

    /**
     * Configuration for failover policies.
     */
    public static class FailoverPolicy {
        public boolean enableAutomaticFailover;
        public Duration detectionTimeout;
        public int maxFailureCount;
        public List<String> backupServices;
        public FailoverStrategy strategy;
        public boolean enableFailback;
        public Duration failbackDelay;
    }

    /**
     * Failover strategies.
     */
    public enum FailoverStrategy {
        IMMEDIATE,      // Failover immediately on failure
        GRADUAL,        // Gradually shift traffic
        PRIORITY_BASED, // Use priority-ordered backup services
        ROUND_ROBIN,    // Distribute among backup services
        CUSTOM          // Custom failover logic
    }

    /**
     * Result of failover setup.
     */
    public static class FailoverSetupResult {
        public boolean configured;
        public List<String> configuredBackups;
        public FailoverStrategy strategy;
        public String errorMessage;
    }

    /**
     * Result of failover execution.
     */
    public static class FailoverExecutionResult {
        public String primaryServiceId;
        public String backupServiceId;
        public boolean failoverSuccessful;
        public Duration failoverTime;
        public int affectedConnections;
        public String failoverReason;
        public String errorMessage;
    }

    /**
     * Configuration for load optimization.
     */
    public static class LoadOptimizationConfig {
        public OptimizationGoal goal;
        public Duration optimizationWindow;
        public boolean enablePredictiveOptimization;
        public double acceptableImbalance;
        public List<String> excludeServices;
    }

    /**
     * Goals for load optimization.
     */
    public enum OptimizationGoal {
        MINIMIZE_LATENCY,
        MAXIMIZE_THROUGHPUT,
        BALANCE_LOAD,
        MINIMIZE_RESOURCE_USAGE,
        CUSTOM
    }

    /**
     * Result of load optimization.
     */
    public static class LoadOptimizationResult {
        public boolean optimized;
        public LoadDistribution newDistribution;
        public double improvementScore;
        public List<String> changesApplied;
        public Duration optimizationTime;
        public String errorMessage;
    }

    /**
     * Configuration for geographic routing.
     */
    public static class GeographicRoutingConfig {
        public boolean enableGeoRouting;
        public Map<String, List<String>> regionToServices;
        public boolean enableCrossRegionFailover;
        public LatencyConstraints latencyConstraints;
        public boolean preferLocalServices;
    }

    /**
     * Latency constraints for geographic routing.
     */
    public static class LatencyConstraints {
        public Duration maxAcceptableLatency;
        public Duration preferredLatency;
        public boolean enforceConstraints;
    }

    /**
     * Result of geographic routing setup.
     */
    public static class GeoRoutingSetupResult {
        public boolean configured;
        public Map<String, List<String>> routingMap;
        public String errorMessage;
    }

    /**
     * Configuration for session affinity.
     */
    public static class SessionAffinityConfig {
        public boolean enableAffinity;
        public AffinityStrategy strategy;
        public Duration sessionTimeout;
        public String affinityKey; // Header or cookie name
        public boolean enableFailover;
    }

    /**
     * Session affinity strategies.
     */
    public enum AffinityStrategy {
        COOKIE_BASED,
        HEADER_BASED,
        IP_HASH,
        CONSISTENT_HASH,
        CUSTOM
    }

    /**
     * Result of session affinity setup.
     */
    public static class AffinitySetupResult {
        public boolean configured;
        public AffinityStrategy strategy;
        public String affinityKey;
        public String errorMessage;
    }

    /**
     * Current routing table configuration.
     */
    public static class RoutingTable {
        public Map<String, List<ServiceEndpoint>> serviceRoutes;
        public LoadBalancingAlgorithm activeAlgorithm;
        public Map<String, RoutingRule> routingRules;
        public long lastUpdated;
    }

    /**
     * Individual routing rule.
     */
    public static class RoutingRule {
        public String ruleId;
        public String condition;
        public String targetService;
        public int priority;
        public boolean enabled;
        public Map<String, Object> parameters;
    }

    /**
     * Configuration for load prediction.
     */
    public static class LoadPredictionConfig {
        public Duration predictionHorizon;
        public PredictionMethod method;
        public boolean includeSeasonality;
        public double confidenceLevel;
        public Map<String, Object> methodParameters;
    }

    /**
     * Methods for load prediction.
     */
    public enum PredictionMethod {
        STATISTICAL,
        MACHINE_LEARNING,
        TREND_ANALYSIS,
        SEASONAL_DECOMPOSITION,
        ENSEMBLE
    }

    /**
     * Load distribution predictions.
     */
    public static class LoadPrediction {
        public Duration predictionHorizon;
        public Map<String, ServiceLoadPrediction> servicePredictions;
        public double overallPredictedLoad;
        public List<String> recommendedActions;
        public double predictionConfidence;
        public long predictionTimestamp;
    }

    /**
     * Load prediction for individual services.
     */
    public static class ServiceLoadPrediction {
        public String serviceId;
        public double predictedRPS;
        public double predictedCpuUtilization;
        public double predictedMemoryUtilization;
        public LoadLevel predictedLoadLevel;
        public List<String> recommendations;
    }

    /**
     * Configuration for rate limiting.
     */
    public static class RateLimitConfig {
        public boolean enabled;
        public int requestsPerSecond;
        public int burstSize;
        public Duration windowSize;
        public RateLimitStrategy strategy;
        public Map<String, Integer> perClientLimits;
    }

    /**
     * Rate limiting strategies.
     */
    public enum RateLimitStrategy {
        TOKEN_BUCKET,
        LEAKY_BUCKET,
        FIXED_WINDOW,
        SLIDING_WINDOW,
        ADAPTIVE
    }

    /**
     * Result of rate limiting setup.
     */
    public static class RateLimitSetupResult {
        public String serviceId;
        public boolean configured;
        public RateLimitConfig appliedConfig;
        public String errorMessage;
    }
}