package io.aurigraph.v11.contracts.enterprise.models;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Portfolio update model for enterprise dashboard
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PortfolioUpdate {
    private String organizationId;
    private Instant timestamp;
    private BigDecimal totalAUM;
    private BigDecimal dailyPnL;
    private int activePositions;
    private BigDecimal riskScore;
}