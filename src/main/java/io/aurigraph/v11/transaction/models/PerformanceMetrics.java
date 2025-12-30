package io.aurigraph.v11.transaction.models;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "performance_metrics", indexes = {
    @Index(name = "idx_timestamp", columnList = "timestamp"),
    @Index(name = "idx_metric_type", columnList = "metric_type")
})
public class PerformanceMetrics extends PanacheEntity {

    public enum MetricType {
        TRANSACTION_LATENCY, THROUGHPUT, CONSENSUS_TIME, FINALITY_TIME, MEMORY_USAGE, CPU_USAGE,
        NETWORK_LATENCY, BLOCK_SIZE, GAS_USAGE, ERROR_RATE
    }

    @Column(name = "metric_type", nullable = false)
    @Enumerated(EnumType.STRING)
    public MetricType metricType;

    @Column(name = "value", nullable = false)
    public BigDecimal value;

    @Column(name = "unit", nullable = false)
    public String unit;

    @Column(name = "timestamp", nullable = false)
    public LocalDateTime timestamp;

    @Column(name = "period_seconds")
    public Integer periodSeconds;

    @Column(name = "min_value")
    public BigDecimal minValue;

    @Column(name = "max_value")
    public BigDecimal maxValue;

    @Column(name = "avg_value")
    public BigDecimal avgValue;

    @Column(name = "percentile_95")
    public BigDecimal percentile95;

    @Column(name = "percentile_99")
    public BigDecimal percentile99;

    @Column(name = "sample_count")
    public Long sampleCount;

    @Column(name = "tags", columnDefinition = "jsonb")
    public String tags;

    @Column(name = "created_at", nullable = false, updatable = false)
    public LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    public static PerformanceMetricsBuilder builder() {
        return new PerformanceMetricsBuilder();
    }

    public static class PerformanceMetricsBuilder {
        private MetricType metricType;
        private BigDecimal value;
        private String unit;
        private LocalDateTime timestamp;
        private Integer periodSeconds;
        private BigDecimal minValue;
        private BigDecimal maxValue;
        private BigDecimal avgValue;
        private BigDecimal percentile95;
        private BigDecimal percentile99;
        private Long sampleCount;
        private String tags;

        public PerformanceMetricsBuilder metricType(MetricType type) {
            this.metricType = type;
            return this;
        }

        public PerformanceMetricsBuilder value(BigDecimal value) {
            this.value = value;
            return this;
        }

        public PerformanceMetricsBuilder unit(String unit) {
            this.unit = unit;
            return this;
        }

        public PerformanceMetricsBuilder timestamp(LocalDateTime timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public PerformanceMetricsBuilder periodSeconds(Integer seconds) {
            this.periodSeconds = seconds;
            return this;
        }

        public PerformanceMetricsBuilder minValue(BigDecimal value) {
            this.minValue = value;
            return this;
        }

        public PerformanceMetricsBuilder maxValue(BigDecimal value) {
            this.maxValue = value;
            return this;
        }

        public PerformanceMetricsBuilder avgValue(BigDecimal value) {
            this.avgValue = value;
            return this;
        }

        public PerformanceMetricsBuilder percentile95(BigDecimal value) {
            this.percentile95 = value;
            return this;
        }

        public PerformanceMetricsBuilder percentile99(BigDecimal value) {
            this.percentile99 = value;
            return this;
        }

        public PerformanceMetricsBuilder sampleCount(Long count) {
            this.sampleCount = count;
            return this;
        }

        public PerformanceMetricsBuilder tags(String tags) {
            this.tags = tags;
            return this;
        }

        public PerformanceMetrics build() {
            PerformanceMetrics metrics = new PerformanceMetrics();
            metrics.metricType = this.metricType;
            metrics.value = this.value;
            metrics.unit = this.unit;
            metrics.timestamp = this.timestamp;
            metrics.periodSeconds = this.periodSeconds;
            metrics.minValue = this.minValue;
            metrics.maxValue = this.maxValue;
            metrics.avgValue = this.avgValue;
            metrics.percentile95 = this.percentile95;
            metrics.percentile99 = this.percentile99;
            metrics.sampleCount = this.sampleCount;
            metrics.tags = this.tags;
            return metrics;
        }
    }
}
