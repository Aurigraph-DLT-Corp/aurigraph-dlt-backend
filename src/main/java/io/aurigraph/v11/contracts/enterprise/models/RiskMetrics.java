package io.aurigraph.v11.contracts.enterprise.models;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Risk metrics model for enterprise dashboard
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RiskMetrics {
    private String organizationId;
    private Instant calculationDate;
    private BigDecimal valueAtRisk95;
    private BigDecimal valueAtRisk99;
    private BigDecimal conditionalValueAtRisk;
    private BigDecimal beta;
    private BigDecimal volatility;
    private BigDecimal sharpeRatio;
    private BigDecimal sortinoRatio;
    private BigDecimal maxDrawdown;
    private BigDecimal concentrationRisk;
    private BigDecimal liquidityRisk;
    private BigDecimal creditRisk;
    private BigDecimal marketRisk;
    private BigDecimal counterpartyRisk;
    private Map<String, BigDecimal> riskFactors;
    private List<String> riskWarnings;
    private String riskRating;
}