package io.aurigraph.v11.contracts.enterprise.models;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Rebalance result model for enterprise dashboard
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RebalanceResult {
    private String rebalanceId;
    private String organizationId;
    private String status; // PENDING, IN_PROGRESS, COMPLETED, FAILED, CANCELLED
    private Instant initiatedAt;
    private Instant completedAt;
    private Map<String, BigDecimal> executedTrades;
    private BigDecimal totalTransactionCosts;
    private BigDecimal estimatedSlippage;
    private BigDecimal actualSlippage;
    private Map<String, BigDecimal> finalAllocations;
    private List<String> executionSteps;
    private String errorMessage;
    private Map<String, Object> metrics;
}