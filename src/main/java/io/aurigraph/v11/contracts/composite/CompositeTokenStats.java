package io.aurigraph.v11.contracts.composite;

import java.math.BigDecimal;
import java.util.*;

/**
 * Statistics for composite token factory
 */
public class CompositeTokenStats {
    private final int totalCompositeTokens;
    private final Map<String, Long> typeDistribution;
    private final Map<CompositeTokenStatus, Long> statusDistribution;
    private final long totalTokensCreated;
    private final BigDecimal totalValue;

    public CompositeTokenStats(int totalCompositeTokens, Map<String, Long> typeDistribution,
                              Map<CompositeTokenStatus, Long> statusDistribution,
                              long totalTokensCreated, BigDecimal totalValue) {
        this.totalCompositeTokens = totalCompositeTokens;
        this.typeDistribution = new HashMap<>(typeDistribution);
        this.statusDistribution = new HashMap<>(statusDistribution);
        this.totalTokensCreated = totalTokensCreated;
        this.totalValue = totalValue;
    }

    public int getTotalCompositeTokens() { return totalCompositeTokens; }
    public Map<String, Long> getTypeDistribution() { return typeDistribution; }
    public Map<CompositeTokenStatus, Long> getStatusDistribution() { return statusDistribution; }
    public long getTotalTokensCreated() { return totalTokensCreated; }
    public BigDecimal getTotalValue() { return totalValue; }
}