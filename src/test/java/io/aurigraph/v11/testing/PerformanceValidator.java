package io.aurigraph.v11.testing;

import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * PerformanceValidator - Validates all performance targets and detects regressions
 *
 * Responsible for:
 * - Tracking latency metrics (p50, p95, p99)
 * - Detecting performance regressions
 * - Generating performance reports
 * - Validating against SLO targets
 * - Trending analysis and alerting
 *
 * Target Metrics:
 * - Registry lookup: <5ms (p99)
 * - Merkle proof generation: <50ms (p99)
 * - Merkle proof verification: <10ms (p99)
 * - Contract execution: <500ms (p99)
 * - Composite assembly: <100ms (p99)
 * - Bulk operations (1000 items): <100ms (p99)
 * - Distributed registry replication: <100ms (p99)
 * - Cache hit: <5ms (p99)
 * - Cache miss + update: <50ms (p99)
 */
@ApplicationScoped
public class PerformanceValidator {

    // Performance targets in milliseconds
    private static final Map<String, PerformanceTarget> TARGETS = Map.ofEntries(
        Map.entry("registry_lookup", new PerformanceTarget("Registry Lookup", 5, 5, 10)),
        Map.entry("merkle_proof_gen", new PerformanceTarget("Merkle Proof Generation", 50, 30, 50)),
        Map.entry("merkle_proof_verify", new PerformanceTarget("Merkle Proof Verification", 10, 5, 15)),
        Map.entry("contract_execution", new PerformanceTarget("Contract Execution", 500, 300, 700)),
        Map.entry("composite_assembly", new PerformanceTarget("Composite Assembly", 100, 50, 150)),
        Map.entry("bulk_operations", new PerformanceTarget("Bulk Operations (1K items)", 100, 50, 150)),
        Map.entry("registry_replication", new PerformanceTarget("Registry Replication", 100, 50, 150)),
        Map.entry("cache_hit", new PerformanceTarget("Cache Hit", 5, 2, 8)),
        Map.entry("cache_miss_update", new PerformanceTarget("Cache Miss + Update", 50, 25, 75))
    );

    // Metrics storage: operation -> list of latencies
    private final ConcurrentHashMap<String, List<Long>> operationMetrics = new ConcurrentHashMap<>();

    // Baseline metrics for regression detection
    private final ConcurrentHashMap<String, PerformanceBaseline> baselineMetrics = new ConcurrentHashMap<>();

    // Regression detection threshold (15% above baseline)
    private static final double REGRESSION_THRESHOLD = 0.15;

    // Code coverage tracking
    private volatile double currentCoverage = 0.0;
    private volatile Instant lastCoverageUpdate = Instant.now();

    public PerformanceValidator() {
        initializeMetrics();
        initializeBaselines();
    }

    /**
     * Initialize metric collections
     */
    private void initializeMetrics() {
        TARGETS.keySet().forEach(key -> operationMetrics.put(key, Collections.synchronizedList(new ArrayList<>())));
    }

    /**
     * Initialize baseline metrics from previous runs
     */
    private void initializeBaselines() {
        // Set initial baselines from targets
        TARGETS.forEach((key, target) -> {
            PerformanceBaseline baseline = new PerformanceBaseline();
            baseline.operationName = key;
            baseline.p50 = target.p50Target;
            baseline.p95 = target.p95Target;
            baseline.p99 = target.p99Target;
            baseline.lastUpdated = Instant.now();
            baselineMetrics.put(key, baseline);
        });
    }

    /**
     * Record a latency measurement
     */
    public void recordLatency(String operationName, long latencyMs) {
        if (operationMetrics.containsKey(operationName)) {
            operationMetrics.get(operationName).add(latencyMs);
            Log.debug("Recorded latency for " + operationName + ": " + latencyMs + "ms");
        } else {
            Log.warn("Unknown operation: " + operationName);
        }
    }

    /**
     * Record multiple latency measurements
     */
    public void recordLatencies(String operationName, List<Long> latencies) {
        if (operationMetrics.containsKey(operationName)) {
            operationMetrics.get(operationName).addAll(latencies);
            Log.debug("Recorded " + latencies.size() + " latencies for " + operationName);
        }
    }

    /**
     * Validate all performance metrics against targets
     */
    public void validateAllMetrics() {
        Log.info("Validating performance metrics against targets");

        TARGETS.forEach((key, target) -> {
            PerformanceMetrics metrics = calculateMetrics(key);
            PerformanceBaseline baseline = baselineMetrics.get(key);

            Log.info("Operation: " + target.displayName);
            Log.info("  p50: " + String.format("%.2f", metrics.p50) + "ms (target: " + target.p50Target + "ms)");
            Log.info("  p95: " + String.format("%.2f", metrics.p95) + "ms (target: " + target.p95Target + "ms)");
            Log.info("  p99: " + String.format("%.2f", metrics.p99) + "ms (target: " + target.p99Target + "ms)");

            // Check against targets
            validateMetricAgainstTarget(key, metrics, target);

            // Check for regressions
            detectRegression(key, metrics, baseline);
        });
    }

    /**
     * Validate a single metric against its target
     */
    private void validateMetricAgainstTarget(String operationName, PerformanceMetrics metrics, PerformanceTarget target) {
        boolean p50Pass = metrics.p50 <= target.p50Target;
        boolean p95Pass = metrics.p95 <= target.p95Target;
        boolean p99Pass = metrics.p99 <= target.p99Target;

        if (!p50Pass) {
            Log.warn("FAIL: " + operationName + " p50 exceeds target");
        }
        if (!p95Pass) {
            Log.warn("FAIL: " + operationName + " p95 exceeds target");
        }
        if (!p99Pass) {
            Log.warn("FAIL: " + operationName + " p99 exceeds target");
        }

        if (p50Pass && p95Pass && p99Pass) {
            Log.info("PASS: " + operationName + " meets all targets");
        }
    }

    /**
     * Detect performance regressions
     */
    private void detectRegression(String operationName, PerformanceMetrics metrics, PerformanceBaseline baseline) {
        double p99Increase = (metrics.p99 - baseline.p99) / baseline.p99;

        if (p99Increase > REGRESSION_THRESHOLD) {
            Log.warn("REGRESSION DETECTED: " + operationName);
            Log.warn("  p99: " + String.format("%.2f", metrics.p99) + "ms (was " + String.format("%.2f", baseline.p99) + "ms)");
            Log.warn("  Increase: " + String.format("%.1f%%", p99Increase * 100));
        }
    }

    /**
     * Calculate percentiles for an operation
     */
    private PerformanceMetrics calculateMetrics(String operationName) {
        List<Long> latencies = operationMetrics.getOrDefault(operationName, new ArrayList<>());

        if (latencies.isEmpty()) {
            return new PerformanceMetrics(0, 0, 0, 0);
        }

        List<Long> sorted = new ArrayList<>(latencies);
        Collections.sort(sorted);

        int size = sorted.size();
        double p50 = sorted.get((int) (size * 0.50));
        double p95 = sorted.get((int) (size * 0.95));
        double p99 = sorted.get((int) (size * 0.99));
        double avg = latencies.stream().mapToLong(Long::longValue).average().orElse(0);

        return new PerformanceMetrics(avg, p50, p95, p99);
    }

    /**
     * Check if performance regressions have been detected
     */
    public boolean hasRegressions() {
        for (Map.Entry<String, List<Long>> entry : operationMetrics.entrySet()) {
            if (entry.getValue().isEmpty()) continue;

            PerformanceMetrics metrics = calculateMetrics(entry.getKey());
            PerformanceBaseline baseline = baselineMetrics.get(entry.getKey());

            double p99Increase = (metrics.p99 - baseline.p99) / baseline.p99;
            if (p99Increase > REGRESSION_THRESHOLD) {
                return true;
            }
        }
        return false;
    }

    /**
     * Generate comprehensive performance report
     */
    public String generateMetricsReport() {
        StringBuilder report = new StringBuilder();
        report.append("| Operation | p50 (ms) | p95 (ms) | p99 (ms) | Target (ms) | Status |\n");
        report.append("|-----------|----------|----------|----------|-------------|--------|\n");

        TARGETS.forEach((key, target) -> {
            PerformanceMetrics metrics = calculateMetrics(key);
            boolean passes = metrics.p99 <= target.p99Target;
            String status = passes ? "✅ PASS" : "❌ FAIL";

            report.append("| ").append(target.displayName)
                .append(" | ").append(String.format("%.2f", metrics.p50))
                .append(" | ").append(String.format("%.2f", metrics.p95))
                .append(" | ").append(String.format("%.2f", metrics.p99))
                .append(" | ").append(target.p99Target)
                .append(" | ").append(status)
                .append(" |\n");
        });

        return report.toString();
    }

    /**
     * Generate trend analysis report
     */
    public String generateTrendReport() {
        StringBuilder report = new StringBuilder();
        report.append("## Performance Trends\n\n");

        TARGETS.forEach((key, target) -> {
            PerformanceBaseline baseline = baselineMetrics.get(key);
            PerformanceMetrics current = calculateMetrics(key);

            double trend = ((current.p99 - baseline.p99) / baseline.p99) * 100;
            String trendIndicator = trend < -5 ? "📉 Improving" : trend > 5 ? "📈 Degrading" : "➡️ Stable";

            report.append("- **").append(target.displayName).append("**: ")
                .append(String.format("%.1f%%", trend))
                .append(" ").append(trendIndicator)
                .append(" (").append(String.format("%.2f", baseline.p99))
                .append("ms → ").append(String.format("%.2f", current.p99)).append("ms)\n");
        });

        return report.toString();
    }

    /**
     * Update code coverage metric
     */
    public void updateCoverage(double coverage) {
        this.currentCoverage = coverage;
        this.lastCoverageUpdate = Instant.now();
        Log.info("Code coverage updated: " + String.format("%.1f%%", coverage * 100));
    }

    /**
     * Get current code coverage
     */
    public double getCurrentCoverage() {
        return currentCoverage;
    }

    /**
     * Clear metrics for a fresh test cycle
     */
    public void clearMetrics() {
        operationMetrics.forEach((key, latencies) -> latencies.clear());
        Log.info("Performance metrics cleared");
    }

    /**
     * Get detailed metrics for an operation
     */
    public PerformanceMetrics getMetrics(String operationName) {
        return calculateMetrics(operationName);
    }

    /**
     * Export metrics as JSON
     */
    public String exportMetricsAsJson() {
        StringBuilder json = new StringBuilder("{\n");

        List<String> entries = TARGETS.keySet().stream().map(key -> {
            PerformanceMetrics metrics = calculateMetrics(key);
            PerformanceTarget target = TARGETS.get(key);
            return "  \"" + key + "\": {\n" +
                "    \"displayName\": \"" + target.displayName + "\",\n" +
                "    \"p50\": " + String.format("%.2f", metrics.p50) + ",\n" +
                "    \"p95\": " + String.format("%.2f", metrics.p95) + ",\n" +
                "    \"p99\": " + String.format("%.2f", metrics.p99) + ",\n" +
                "    \"target\": " + target.p99Target + "\n" +
                "  }";
        }).collect(Collectors.toList());

        json.append(String.join(",\n", entries));
        json.append("\n}");

        return json.toString();
    }

    // ===== Inner Classes =====

    public static class PerformanceTarget {
        public String displayName;
        public double p50Target;
        public double p95Target;
        public double p99Target;

        public PerformanceTarget(String displayName, double p99Target, double p50Target, double p95Target) {
            this.displayName = displayName;
            this.p99Target = p99Target;
            this.p50Target = p50Target;
            this.p95Target = p95Target;
        }
    }

    public static class PerformanceMetrics {
        public double avg;
        public double p50;
        public double p95;
        public double p99;

        public PerformanceMetrics(double avg, double p50, double p95, double p99) {
            this.avg = avg;
            this.p50 = p50;
            this.p95 = p95;
            this.p99 = p99;
        }
    }

    public static class PerformanceBaseline {
        public String operationName;
        public double p50;
        public double p95;
        public double p99;
        public Instant lastUpdated;
    }
}
