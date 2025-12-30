package io.aurigraph.v11.oracle;

/**
 * Insufficient Oracles Exception
 * Thrown when there are not enough active oracles available for verification
 *
 * @author Aurigraph V11 - Backend Development Agent
 * @version 11.0.0
 * @sprint Sprint 16 - Oracle Verification System (AV11-483)
 */
public class InsufficientOraclesException extends RuntimeException {

    private final int requiredOracles;
    private final int availableOracles;

    public InsufficientOraclesException(int requiredOracles, int availableOracles) {
        super(String.format(
            "Insufficient oracles for verification. Required: %d, Available: %d",
            requiredOracles, availableOracles
        ));
        this.requiredOracles = requiredOracles;
        this.availableOracles = availableOracles;
    }

    public InsufficientOraclesException(String message, int requiredOracles, int availableOracles) {
        super(message);
        this.requiredOracles = requiredOracles;
        this.availableOracles = availableOracles;
    }

    public InsufficientOraclesException(String message, Throwable cause, int requiredOracles, int availableOracles) {
        super(message, cause);
        this.requiredOracles = requiredOracles;
        this.availableOracles = availableOracles;
    }

    public int getRequiredOracles() {
        return requiredOracles;
    }

    public int getAvailableOracles() {
        return availableOracles;
    }
}
