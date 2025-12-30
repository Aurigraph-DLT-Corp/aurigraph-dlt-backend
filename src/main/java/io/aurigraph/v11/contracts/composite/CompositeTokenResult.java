package io.aurigraph.v11.contracts.composite;

/**
 * Result of composite token creation
 */
public class CompositeTokenResult {
    private final CompositeToken compositeToken;
    private final boolean success;
    private final String message;
    private final long processingTime;

    public CompositeTokenResult(CompositeToken compositeToken, boolean success, 
                               String message, long processingTime) {
        this.compositeToken = compositeToken;
        this.success = success;
        this.message = message;
        this.processingTime = processingTime;
    }

    public CompositeToken getCompositeToken() { return compositeToken; }
    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public long getProcessingTime() { return processingTime; }
}