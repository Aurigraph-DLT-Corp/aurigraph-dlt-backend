package io.aurigraph.v11.contracts.enterprise.models;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Builder;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Report request model for enterprise dashboard
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ReportRequest {
    private String reportType;
    private Instant periodStart;
    private Instant periodEnd;
    private List<String> includeMetrics;
    private List<String> excludeMetrics;
    private String format; // PDF, Excel, JSON
    private String granularity; // DAILY, WEEKLY, MONTHLY
    private Map<String, Object> parameters;
    private boolean includeBenchmarks;
    private boolean includeProjections;
}