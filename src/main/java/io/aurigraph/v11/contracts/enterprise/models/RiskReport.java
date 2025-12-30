package io.aurigraph.v11.contracts.enterprise.models;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Risk report model for enterprise dashboard
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RiskReport {
    private String organizationId;
    private Instant reportDate;
    private BigDecimal portfolioValue;
    private BigDecimal concentrationRisk;
    private BigDecimal liquidityRisk;
    private BigDecimal creditRisk;
    private BigDecimal marketRisk;
    private BigDecimal valueAtRisk95;
    private BigDecimal valueAtRisk99;
    private List<String> stressTestResults;
}