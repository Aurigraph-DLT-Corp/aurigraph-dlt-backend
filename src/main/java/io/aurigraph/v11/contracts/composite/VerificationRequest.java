package io.aurigraph.v11.contracts.composite;

import java.time.Instant;
import java.util.*;

/**
 * Verification request
 */
public class VerificationRequest {
    private final String requestId;
    private final String compositeId;
    private final String assetType;
    private final VerificationLevel requiredLevel;
    private final List<String> assignedVerifiers;
    private final Instant requestedAt;
    private final List<VerificationResult> verificationResults;

    public VerificationRequest(String requestId, String compositeId, String assetType,
                             VerificationLevel requiredLevel, List<String> assignedVerifiers,
                             Instant requestedAt) {
        this.requestId = requestId;
        this.compositeId = compositeId;
        this.assetType = assetType;
        this.requiredLevel = requiredLevel;
        this.assignedVerifiers = new ArrayList<>(assignedVerifiers);
        this.requestedAt = requestedAt;
        this.verificationResults = new ArrayList<>();
    }

    public void addVerificationResult(VerificationResult result) {
        verificationResults.add(result);
    }

    public boolean isComplete() {
        return verificationResults.size() >= assignedVerifiers.size();
    }

    // Getters
    public String getRequestId() { return requestId; }
    public String getCompositeId() { return compositeId; }
    public String getAssetType() { return assetType; }
    public VerificationLevel getRequiredLevel() { return requiredLevel; }
    public List<String> getAssignedVerifiers() { return List.copyOf(assignedVerifiers); }
    public Instant getRequestedAt() { return requestedAt; }
    public List<VerificationResult> getVerificationResults() { return List.copyOf(verificationResults); }
}