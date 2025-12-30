package io.aurigraph.v11.contracts.enterprise.models;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Optimization request model for enterprise dashboard
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class OptimizationRequest {
    private String objective; // "yield", "risk", "balanced"
    private BigDecimal riskTolerance;
    private List<String> constraints;
}