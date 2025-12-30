package io.aurigraph.v11.contracts.enterprise.models;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Enterprise portfolio model for dashboard
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class EnterprisePortfolio {
    private String organizationId;
    private BigDecimal totalAUM;
    private int activePositionCount;
    private BigDecimal riskScore;
    private Instant createdAt;
    private Instant lastUpdated;
}