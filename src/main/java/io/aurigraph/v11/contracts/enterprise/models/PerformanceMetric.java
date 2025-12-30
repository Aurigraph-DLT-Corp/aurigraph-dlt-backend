package io.aurigraph.v11.contracts.enterprise.models;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Performance metric model for enterprise dashboard
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PerformanceMetric {
    private String metricName;
    private BigDecimal value;
    private String unit;
    private Instant timestamp;
    private String description;
    private BigDecimal benchmark;
    private String benchmarkName;
    private BigDecimal percentile;
}