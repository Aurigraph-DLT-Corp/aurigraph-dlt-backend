package io.aurigraph.v11.transaction.repositories;

import io.aurigraph.v11.transaction.models.PerformanceMetrics;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.LocalDateTime;
import java.util.List;

@ApplicationScoped
public class PerformanceMetricsRepository implements PanacheRepository<PerformanceMetrics> {

    public List<PerformanceMetrics> findByMetricType(PerformanceMetrics.MetricType metricType) {
        return list("metricType", metricType);
    }

    public List<PerformanceMetrics> findByTimestampRange(
        LocalDateTime startDate,
        LocalDateTime endDate
    ) {
        return list(
            "timestamp >= ?1 and timestamp <= ?2",
            startDate,
            endDate
        );
    }

    public List<PerformanceMetrics> findByMetricTypeAndTimeRange(
        PerformanceMetrics.MetricType metricType,
        LocalDateTime startDate,
        LocalDateTime endDate
    ) {
        return list(
            "metricType = ?1 and timestamp >= ?2 and timestamp <= ?3",
            metricType,
            startDate,
            endDate
        );
    }

    public List<PerformanceMetrics> findRecentMetrics(int limit) {
        return find("select m from PerformanceMetrics m order by m.timestamp desc")
            .page(0, limit)
            .list();
    }

    public List<PerformanceMetrics> findLatencyMetricsSince(LocalDateTime since) {
        return list(
            "metricType = ?1 and timestamp >= ?2",
            PerformanceMetrics.MetricType.TRANSACTION_LATENCY,
            since
        );
    }

    public List<PerformanceMetrics> findThroughputMetricsSince(LocalDateTime since) {
        return list(
            "metricType = ?1 and timestamp >= ?2",
            PerformanceMetrics.MetricType.THROUGHPUT,
            since
        );
    }

    public List<PerformanceMetrics> findConsensusTimeMetricsSince(LocalDateTime since) {
        return list(
            "metricType = ?1 and timestamp >= ?2",
            PerformanceMetrics.MetricType.CONSENSUS_TIME,
            since
        );
    }

    public Double getAverageMetricValue(
        PerformanceMetrics.MetricType metricType,
        LocalDateTime startDate,
        LocalDateTime endDate
    ) {
        return find(
            "select avg(avgValue) from PerformanceMetrics where metricType = ?1 and timestamp >= ?2 and timestamp <= ?3",
            metricType,
            startDate,
            endDate
        ).project(Double.class)
            .firstResultOptional()
            .orElse(0.0);
    }

    public List<PerformanceMetrics> findLastNMetrics(PerformanceMetrics.MetricType metricType, int n) {
        return find("metricType = ?1 order by timestamp desc", metricType)
            .page(0, n)
            .list();
    }
}
