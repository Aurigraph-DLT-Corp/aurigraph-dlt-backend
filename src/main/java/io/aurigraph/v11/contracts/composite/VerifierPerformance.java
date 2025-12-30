package io.aurigraph.v11.contracts.composite;

import java.math.BigDecimal;

/**
 * Verifier performance metrics
 */
public class VerifierPerformance {
    private final String verifierId;
    private final String name;
    private final VerifierTier tier;
    private final BigDecimal reputation;
    private final int completedVerifications;
    private final BigDecimal successRate;
    private final long averageResponseTime;

    public VerifierPerformance(String verifierId, String name, VerifierTier tier,
                             BigDecimal reputation, int completedVerifications,
                             BigDecimal successRate, long averageResponseTime) {
        this.verifierId = verifierId;
        this.name = name;
        this.tier = tier;
        this.reputation = reputation;
        this.completedVerifications = completedVerifications;
        this.successRate = successRate;
        this.averageResponseTime = averageResponseTime;
    }

    // Getters
    public String getVerifierId() { return verifierId; }
    public String getName() { return name; }
    public VerifierTier getTier() { return tier; }
    public BigDecimal getReputation() { return reputation; }
    public int getCompletedVerifications() { return completedVerifications; }
    public BigDecimal getSuccessRate() { return successRate; }
    public long getAverageResponseTime() { return averageResponseTime; }
}