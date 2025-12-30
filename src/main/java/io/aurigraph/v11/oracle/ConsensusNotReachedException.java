package io.aurigraph.v11.oracle;

import java.math.BigDecimal;

/**
 * Consensus Not Reached Exception
 * Thrown when oracles fail to reach consensus on asset price
 *
 * @author Aurigraph V11 - Backend Development Agent
 * @version 11.0.0
 * @sprint Sprint 16 - Oracle Verification System (AV11-483)
 */
public class ConsensusNotReachedException extends RuntimeException {

    private final double consensusPercentage;
    private final double requiredConsensus;
    private final int validResponses;
    private final int totalResponses;
    private final BigDecimal priceVariance;

    public ConsensusNotReachedException(
        double consensusPercentage,
        double requiredConsensus,
        int validResponses,
        int totalResponses
    ) {
        super(String.format(
            "Oracle consensus not reached. Achieved: %.2f%%, Required: %.2f%%, Valid responses: %d/%d",
            consensusPercentage * 100, requiredConsensus * 100, validResponses, totalResponses
        ));
        this.consensusPercentage = consensusPercentage;
        this.requiredConsensus = requiredConsensus;
        this.validResponses = validResponses;
        this.totalResponses = totalResponses;
        this.priceVariance = null;
    }

    public ConsensusNotReachedException(
        String message,
        double consensusPercentage,
        double requiredConsensus,
        int validResponses,
        int totalResponses,
        BigDecimal priceVariance
    ) {
        super(message);
        this.consensusPercentage = consensusPercentage;
        this.requiredConsensus = requiredConsensus;
        this.validResponses = validResponses;
        this.totalResponses = totalResponses;
        this.priceVariance = priceVariance;
    }

    public ConsensusNotReachedException(
        String message,
        Throwable cause,
        double consensusPercentage,
        double requiredConsensus
    ) {
        super(message, cause);
        this.consensusPercentage = consensusPercentage;
        this.requiredConsensus = requiredConsensus;
        this.validResponses = 0;
        this.totalResponses = 0;
        this.priceVariance = null;
    }

    public double getConsensusPercentage() {
        return consensusPercentage;
    }

    public double getRequiredConsensus() {
        return requiredConsensus;
    }

    public int getValidResponses() {
        return validResponses;
    }

    public int getTotalResponses() {
        return totalResponses;
    }

    public BigDecimal getPriceVariance() {
        return priceVariance;
    }
}
