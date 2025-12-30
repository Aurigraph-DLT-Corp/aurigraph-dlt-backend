package io.aurigraph.v11.contracts.composite;

import java.math.BigDecimal;
import java.util.*;

/**
 * Verifier statistics
 */
public class VerifierStats {
    private final int totalVerifiers;
    private final Map<VerifierTier, Integer> tierDistribution;
    private final Map<VerifierStatus, Integer> statusDistribution;
    private final int activeRequests;
    private final BigDecimal averageReputation;

    public VerifierStats(int totalVerifiers, Map<VerifierTier, Integer> tierDistribution,
                        Map<VerifierStatus, Integer> statusDistribution, int activeRequests,
                        BigDecimal averageReputation) {
        this.totalVerifiers = totalVerifiers;
        this.tierDistribution = new HashMap<>(tierDistribution);
        this.statusDistribution = new HashMap<>(statusDistribution);
        this.activeRequests = activeRequests;
        this.averageReputation = averageReputation;
    }

    // Getters
    public int getTotalVerifiers() { return totalVerifiers; }
    public Map<VerifierTier, Integer> getTierDistribution() { return tierDistribution; }
    public Map<VerifierStatus, Integer> getStatusDistribution() { return statusDistribution; }
    public int getActiveRequests() { return activeRequests; }
    public BigDecimal getAverageReputation() { return averageReputation; }
}