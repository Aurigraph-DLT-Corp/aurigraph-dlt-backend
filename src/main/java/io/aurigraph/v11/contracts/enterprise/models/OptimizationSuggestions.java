package io.aurigraph.v11.contracts.enterprise.models;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/**
 * Optimization suggestions model for enterprise dashboard
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class OptimizationSuggestions {
    private String organizationId;
    private Instant optimizationDate;
    private List<String> yieldOptimizations;
    private List<String> riskReductions;
    private List<String> diversificationSuggestions;
    private List<String> costReductions;
    private List<String> rebalancingRecommendations;
}