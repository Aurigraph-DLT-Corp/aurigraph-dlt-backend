package io.aurigraph.v11.contracts.enterprise.models;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

/**
 * Portfolio snapshot model for enterprise dashboard
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PortfolioSnapshot {
    private String organizationId;
    private Instant snapshotDate;
    private BigDecimal totalValue;
    private BigDecimal dailyChange;
    private BigDecimal weeklyChange;
    private BigDecimal monthlyChange;
    private Map<String, BigDecimal> assetBreakdown;
    private Map<String, BigDecimal> protocolBreakdown;
    private int totalPositions;
    private BigDecimal unrealizedPnL;
    private BigDecimal realizedPnL;
}