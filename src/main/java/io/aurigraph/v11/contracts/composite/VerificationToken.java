package io.aurigraph.v11.contracts.composite;

import java.time.Instant;
import java.util.*;

/**
 * Verification Token (ERC-721) - Tracks verification results from third-party verifiers
 */
public class VerificationToken extends SecondaryToken {
    private VerificationLevel requiredLevel;
    private List<VerificationResult> verificationResults;

    public VerificationToken(String tokenId, String compositeId, VerificationLevel requiredLevel,
                           List<VerificationResult> verificationResults) {
        super(tokenId, compositeId, SecondaryTokenType.VERIFICATION);
        this.requiredLevel = requiredLevel;
        this.verificationResults = new ArrayList<>(verificationResults);
    }

    @Override
    public void updateData(Map<String, Object> updateData) {
        this.lastUpdated = Instant.now();
        this.data.putAll(updateData);
    }

    public void addVerificationResult(VerificationResult result) {
        verificationResults.add(result);
        this.lastUpdated = Instant.now();
    }

    public boolean hasConsensus() {
        // Check if we have enough positive verifications for consensus
        long positiveResults = verificationResults.stream()
            .filter(VerificationResult::isVerified)
            .count();
        
        return positiveResults >= 2; // Minimum 2 verifiers for consensus
    }

    public VerificationLevel getConsensusLevel() {
        if (!hasConsensus()) {
            return VerificationLevel.NONE;
        }
        
        // Return the highest verification level achieved by consensus
        return verificationResults.stream()
            .filter(VerificationResult::isVerified)
            .map(VerificationResult::getVerificationLevel)
            .max(Enum::compareTo)
            .orElse(VerificationLevel.NONE);
    }

    // Getters
    public VerificationLevel getRequiredLevel() { return requiredLevel; }
    public List<VerificationResult> getVerificationResults() { return List.copyOf(verificationResults); }
}