package io.aurigraph.v11.analytics;

import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Metrics Aggregator
 *
 * Aggregates and processes platform-wide metrics for analytics.
 * Provides statistical analysis and trend detection.
 *
 * Part of Sprint 9 - Transaction Analytics (AV11-177)
 *
 * @author Backend Development Agent (BDA)
 * @version 11.0.0
 * @since Sprint 9
 */
@ApplicationScoped
public class MetricsAggregator {

    private static final Logger LOG = Logger.getLogger(MetricsAggregator.class);

    // In-memory metric storage (replace with time-series DB in production)
    private final Map<String, List<MetricPoint>> metrics = new ConcurrentHashMap<>();

    /**
     * Record a metric data point
     */
    public void recordMetric(String metricName, double value, Instant timestamp) {
        metrics.computeIfAbsent(metricName, k -> new ArrayList<>())
               .add(new MetricPoint(value, timestamp));
        LOG.tracef("Recorded metric: %s = %.2f at %s", metricName, value, timestamp);
    }

    /**
     * Get metrics for a specific time range
     */
    public List<MetricPoint> getMetrics(String metricName, Instant startTime, Instant endTime) {
        List<MetricPoint> allPoints = metrics.getOrDefault(metricName, Collections.emptyList());
        return allPoints.stream()
                .filter(p -> !p.timestamp.isBefore(startTime) && !p.timestamp.isAfter(endTime))
                .sorted(Comparator.comparing(p -> p.timestamp))
                .toList();
    }

    /**
     * Calculate average value for a metric in time range
     */
    public double calculateAverage(String metricName, Instant startTime, Instant endTime) {
        List<MetricPoint> points = getMetrics(metricName, startTime, endTime);
        if (points.isEmpty()) {
            return 0.0;
        }
        return points.stream()
                .mapToDouble(p -> p.value)
                .average()
                .orElse(0.0);
    }

    /**
     * Calculate sum of metric values in time range
     */
    public double calculateSum(String metricName, Instant startTime, Instant endTime) {
        List<MetricPoint> points = getMetrics(metricName, startTime, endTime);
        return points.stream()
                .mapToDouble(p -> p.value)
                .sum();
    }

    /**
     * Calculate peak (maximum) value in time range
     */
    public MetricPoint calculatePeak(String metricName, Instant startTime, Instant endTime) {
        List<MetricPoint> points = getMetrics(metricName, startTime, endTime);
        return points.stream()
                .max(Comparator.comparing(p -> p.value))
                .orElse(new MetricPoint(0.0, Instant.now()));
    }

    /**
     * Calculate trend (percentage change) between two periods
     */
    public double calculateTrend(String metricName, Instant period1Start, Instant period1End,
                                  Instant period2Start, Instant period2End) {
        double period1Avg = calculateAverage(metricName, period1Start, period1End);
        double period2Avg = calculateAverage(metricName, period2Start, period2End);

        if (period2Avg == 0) {
            return 0.0;
        }

        return ((period1Avg - period2Avg) / period2Avg) * 100;
    }

    /**
     * Clear old metrics (retention policy)
     */
    public void clearOldMetrics(Instant beforeTime) {
        metrics.values().forEach(points ->
            points.removeIf(p -> p.timestamp.isBefore(beforeTime))
        );
        LOG.infof("Cleared metrics before %s", beforeTime);
    }

    /**
     * Metric data point
     */
    public static class MetricPoint {
        public final double value;
        public final Instant timestamp;

        public MetricPoint(double value, Instant timestamp) {
            this.value = value;
            this.timestamp = timestamp;
        }
    }
}
