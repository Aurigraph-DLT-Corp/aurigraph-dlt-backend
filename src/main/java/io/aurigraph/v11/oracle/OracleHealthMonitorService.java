package io.aurigraph.v11.oracle;

import io.aurigraph.v11.oracle.adapter.OracleAdapter;
import io.aurigraph.v11.oracle.adapter.ChainlinkAdapter;
import io.aurigraph.v11.oracle.adapter.PythAdapter;
import io.aurigraph.v11.oracle.adapter.BandProtocolAdapter;
import io.quarkus.logging.Log;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Oracle Health Monitor Service
 * Continuously monitors oracle availability, reliability, and performance
 *
 * Features:
 * - Periodic health checks every 30 seconds
 * - Reliability score tracking
 * - Automatic failover for unhealthy oracles
 * - Alert generation for degraded services
 * - Historical uptime tracking
 *
 * @author Aurigraph V11 - Development Agent 4
 * @version 11.0.0
 * @sprint Sprint 16 - Oracle Verification System (AV11-483)
 */
@ApplicationScoped
public class OracleHealthMonitorService {

    private static final long HEALTH_CHECK_TIMEOUT_MS = 5000;
    private static final double MIN_RELIABILITY_THRESHOLD = 0.7;
    private static final int HEALTH_CHECK_HISTORY_SIZE = 100;

    @Inject
    ChainlinkAdapter chainlinkAdapter;

    @Inject
    PythAdapter pythAdapter;

    @Inject
    BandProtocolAdapter bandProtocolAdapter;

    // Oracle health status tracking
    private final Map<String, OracleHealthStatus> oracleHealthMap = new ConcurrentHashMap<>();
    private final Map<String, List<HealthCheckResult>> healthCheckHistory = new ConcurrentHashMap<>();

    /**
     * Get all registered oracle adapters
     */
    private List<OracleAdapter> getAllAdapters() {
        return List.of(chainlinkAdapter, pythAdapter, bandProtocolAdapter);
    }

    /**
     * Scheduled health check - runs every 30 seconds
     */
    @Scheduled(every = "30s")
    public void performHealthCheck() {
        Log.info("Starting scheduled oracle health check");

        List<OracleAdapter> adapters = getAllAdapters();
        long startTime = System.currentTimeMillis();

        List<CompletableFuture<HealthCheckResult>> futures = adapters.stream()
            .map(adapter -> checkOracleHealth(adapter))
            .collect(Collectors.toList());

        // Wait for all health checks to complete (with timeout)
        CompletableFuture<Void> allChecks = CompletableFuture.allOf(
            futures.toArray(new CompletableFuture[0])
        );

        try {
            allChecks.get(HEALTH_CHECK_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            Log.warnf("Some health checks timed out: %s", e.getMessage());
        }

        // Collect results
        List<HealthCheckResult> results = futures.stream()
            .map(future -> {
                try {
                    return future.getNow(null);
                } catch (Exception e) {
                    return null;
                }
            })
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

        // Update health status and check for alerts
        results.forEach(this::updateHealthStatus);

        long totalTime = System.currentTimeMillis() - startTime;
        Log.infof("Health check completed in %dms: %d oracles checked, %d healthy, %d degraded, %d failed",
            totalTime,
            results.size(),
            results.stream().filter(r -> r.isHealthy).count(),
            results.stream().filter(r -> !r.isHealthy && r.reliabilityScore >= MIN_RELIABILITY_THRESHOLD).count(),
            results.stream().filter(r -> r.reliabilityScore < MIN_RELIABILITY_THRESHOLD).count()
        );
    }

    /**
     * Check health of a single oracle
     */
    private CompletableFuture<HealthCheckResult> checkOracleHealth(OracleAdapter adapter) {
        return CompletableFuture.supplyAsync(() -> {
            long startTime = System.currentTimeMillis();
            HealthCheckResult result = new HealthCheckResult();
            result.oracleId = adapter.getOracleId();
            result.providerName = adapter.getProviderName();
            result.timestamp = Instant.now();

            try {
                // Check if oracle is healthy
                Boolean isHealthy = adapter.isHealthy()
                    .get(HEALTH_CHECK_TIMEOUT_MS, TimeUnit.MILLISECONDS);

                result.isHealthy = Boolean.TRUE.equals(isHealthy);
                result.responseTime = System.currentTimeMillis() - startTime;
                result.reliabilityScore = adapter.getReliabilityScore();
                result.lastUpdateTimestamp = adapter.getLastUpdateTimestamp();
                result.stakeWeight = adapter.getStakeWeight();

                // Calculate uptime based on last update
                long timeSinceUpdate = System.currentTimeMillis() - adapter.getLastUpdateTimestamp();
                result.isUpToDate = timeSinceUpdate < (adapter.getUpdateFrequency() * 2);

            } catch (Exception e) {
                result.isHealthy = false;
                result.error = e.getMessage();
                result.responseTime = System.currentTimeMillis() - startTime;
                result.reliabilityScore = adapter.getReliabilityScore();
                Log.warnf("Health check failed for %s: %s", adapter.getProviderName(), e.getMessage());
            }

            return result;
        });
    }

    /**
     * Update oracle health status and check for alerts
     */
    private void updateHealthStatus(HealthCheckResult result) {
        String oracleId = result.oracleId;

        // Update or create health status
        OracleHealthStatus status = oracleHealthMap.computeIfAbsent(
            oracleId,
            id -> new OracleHealthStatus(id, result.providerName)
        );

        // Update status
        status.lastCheckTime = result.timestamp;
        status.isHealthy = result.isHealthy;
        status.reliabilityScore = result.reliabilityScore;
        status.lastResponseTime = result.responseTime;
        status.totalChecks++;

        if (result.isHealthy) {
            status.successfulChecks++;
        } else {
            status.failedChecks++;
            status.lastFailureTime = result.timestamp;
            status.lastFailureReason = result.error;
        }

        // Calculate uptime percentage
        status.uptimePercentage = (double) status.successfulChecks / status.totalChecks;

        // Store in history
        List<HealthCheckResult> history = healthCheckHistory.computeIfAbsent(
            oracleId,
            id -> new ArrayList<>()
        );
        history.add(result);

        // Keep only recent history
        if (history.size() > HEALTH_CHECK_HISTORY_SIZE) {
            history.remove(0);
        }

        // Check for alerts
        checkAndGenerateAlerts(status, result);
    }

    /**
     * Check if alerts should be generated for this oracle
     */
    private void checkAndGenerateAlerts(OracleHealthStatus status, HealthCheckResult result) {
        // Alert if oracle becomes unhealthy
        if (!result.isHealthy && status.successfulChecks > 0) {
            Log.warnf("ALERT: Oracle %s (%s) is now UNHEALTHY. Reliability: %.2f%%, Error: %s",
                status.oracleId,
                status.providerName,
                status.reliabilityScore * 100,
                result.error
            );
        }

        // Alert if reliability drops below threshold
        if (result.reliabilityScore < MIN_RELIABILITY_THRESHOLD) {
            Log.warnf("ALERT: Oracle %s (%s) reliability dropped to %.2f%% (threshold: %.2f%%)",
                status.oracleId,
                status.providerName,
                result.reliabilityScore * 100,
                MIN_RELIABILITY_THRESHOLD * 100
            );
        }

        // Alert if response time is too high (> 5 seconds)
        if (result.responseTime > 5000) {
            Log.warnf("ALERT: Oracle %s (%s) response time is high: %dms",
                status.oracleId,
                status.providerName,
                result.responseTime
            );
        }

        // Alert if oracle hasn't updated recently
        if (!result.isUpToDate) {
            long timeSinceUpdate = System.currentTimeMillis() - result.lastUpdateTimestamp;
            Log.warnf("ALERT: Oracle %s (%s) last update was %d seconds ago",
                status.oracleId,
                status.providerName,
                timeSinceUpdate / 1000
            );
        }
    }

    /**
     * Get current health status for all oracles
     */
    public Map<String, OracleHealthStatus> getAllOracleHealthStatus() {
        return new HashMap<>(oracleHealthMap);
    }

    /**
     * Get health status for a specific oracle
     */
    public Optional<OracleHealthStatus> getOracleHealthStatus(String oracleId) {
        return Optional.ofNullable(oracleHealthMap.get(oracleId));
    }

    /**
     * Get healthy oracles only (reliability >= threshold)
     */
    public List<String> getHealthyOracleIds() {
        return oracleHealthMap.values().stream()
            .filter(status -> status.isHealthy && status.reliabilityScore >= MIN_RELIABILITY_THRESHOLD)
            .map(status -> status.oracleId)
            .collect(Collectors.toList());
    }

    /**
     * Get oracle health check history
     */
    public List<HealthCheckResult> getHealthCheckHistory(String oracleId) {
        return new ArrayList<>(healthCheckHistory.getOrDefault(oracleId, Collections.emptyList()));
    }

    /**
     * Get overall system health summary
     */
    public OracleSystemHealthSummary getSystemHealthSummary() {
        OracleSystemHealthSummary summary = new OracleSystemHealthSummary();
        summary.timestamp = Instant.now();
        summary.totalOracles = oracleHealthMap.size();
        summary.healthyOracles = (int) oracleHealthMap.values().stream()
            .filter(status -> status.isHealthy)
            .count();
        summary.degradedOracles = (int) oracleHealthMap.values().stream()
            .filter(status -> !status.isHealthy && status.reliabilityScore >= MIN_RELIABILITY_THRESHOLD)
            .count();
        summary.failedOracles = (int) oracleHealthMap.values().stream()
            .filter(status -> status.reliabilityScore < MIN_RELIABILITY_THRESHOLD)
            .count();

        // Calculate average uptime
        summary.averageUptime = oracleHealthMap.values().stream()
            .mapToDouble(status -> status.uptimePercentage)
            .average()
            .orElse(0.0);

        // Calculate average reliability
        summary.averageReliability = oracleHealthMap.values().stream()
            .mapToDouble(status -> status.reliabilityScore)
            .average()
            .orElse(0.0);

        return summary;
    }

    /**
     * Health check result for a single oracle
     */
    public static class HealthCheckResult {
        public String oracleId;
        public String providerName;
        public Instant timestamp;
        public boolean isHealthy;
        public boolean isUpToDate;
        public double reliabilityScore;
        public long responseTime;
        public long lastUpdateTimestamp;
        public double stakeWeight;
        public String error;
    }

    /**
     * Oracle health status tracking
     */
    public static class OracleHealthStatus {
        public String oracleId;
        public String providerName;
        public Instant lastCheckTime;
        public boolean isHealthy;
        public double reliabilityScore;
        public long lastResponseTime;
        public long totalChecks;
        public long successfulChecks;
        public long failedChecks;
        public double uptimePercentage;
        public Instant lastFailureTime;
        public String lastFailureReason;

        public OracleHealthStatus(String oracleId, String providerName) {
            this.oracleId = oracleId;
            this.providerName = providerName;
            this.isHealthy = true;
            this.reliabilityScore = 1.0;
            this.uptimePercentage = 1.0;
        }
    }

    /**
     * System-wide health summary
     */
    public static class OracleSystemHealthSummary {
        public Instant timestamp;
        public int totalOracles;
        public int healthyOracles;
        public int degradedOracles;
        public int failedOracles;
        public double averageUptime;
        public double averageReliability;

        public boolean isSystemHealthy() {
            return healthyOracles >= (totalOracles * 2 / 3); // 2/3 majority
        }
    }
}
