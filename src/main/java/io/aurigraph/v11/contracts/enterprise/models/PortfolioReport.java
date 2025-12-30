package io.aurigraph.v11.contracts.enterprise.models;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Builder;

import java.time.Instant;
import java.util.List;

/**
 * Portfolio report model for enterprise dashboard
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PortfolioReport {
    private String organizationId;
    private String reportType;
    private Instant reportDate;
    private Instant periodStart;
    private Instant periodEnd;
    private String executiveSummary;
    private List<String> positionAnalysis;
    private String riskAssessment;
    private String performanceAttribution;
    private List<String> recommendations;
}