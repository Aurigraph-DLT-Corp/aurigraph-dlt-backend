package io.aurigraph.v11.contracts.enterprise.models;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

/**
 * Performance analytics model for enterprise dashboard
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PerformanceAnalytics {
    private String organizationId;
    private Instant analysisDate;
    private BigDecimal dailyReturn;
    private BigDecimal weeklyReturn;
    private BigDecimal monthlyReturn;
    private BigDecimal yearToDateReturn;
    private BigDecimal sharpeRatio;
    private BigDecimal sortinoRatio;
    private BigDecimal maxDrawdown;
    private BigDecimal averageAPY;
    private Map<String, BigDecimal> yieldBreakdown;
    private BigDecimal totalFeesEarned;
    private Map<String, BigDecimal> feeBreakdown;
}