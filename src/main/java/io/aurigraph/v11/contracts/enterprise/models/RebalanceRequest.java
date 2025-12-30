package io.aurigraph.v11.contracts.enterprise.models;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Rebalance request model for enterprise dashboard
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RebalanceRequest {
    private String organizationId;
    private String rebalanceType; // MANUAL, AUTOMATIC, THRESHOLD_BASED
    private Map<String, BigDecimal> targetAllocations;
    private Map<String, BigDecimal> currentAllocations;
    private BigDecimal toleranceThreshold;
    private List<String> excludeAssets;
    private List<String> priorityAssets;
    private BigDecimal maxTransactionCost;
    private boolean considerTaxImpact;
    private String executionStrategy; // IMMEDIATE, GRADUAL, SCHEDULED
    private Instant executionTime;
    private Map<String, Object> constraints;
}