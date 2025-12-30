package io.aurigraph.v11.contracts.composite;

import java.time.Instant;
import java.util.*;

/**
 * Verification result from a third-party verifier
 */
public class VerificationResult {
    private final String resultId;
    private final String verifierId;
    private final String compositeId;
    private final boolean verified;
    private final VerificationLevel verificationLevel;
    private final String reportSummary;
    private final Instant verifiedAt;
    private final Map<String, Object> resultData;

    public VerificationResult(String resultId, String verifierId, String compositeId,
                             boolean verified, VerificationLevel verificationLevel,
                             String reportSummary, Instant verifiedAt) {
        this.resultId = resultId;
        this.verifierId = verifierId;
        this.compositeId = compositeId;
        this.verified = verified;
        this.verificationLevel = verificationLevel;
        this.reportSummary = reportSummary;
        this.verifiedAt = verifiedAt;
        this.resultData = new HashMap<>();
    }

    // Getters
    public String getResultId() { return resultId; }
    public String getVerifierId() { return verifierId; }
    public String getCompositeId() { return compositeId; }
    public boolean isVerified() { return verified; }
    public VerificationLevel getVerificationLevel() { return verificationLevel; }
    public String getReportSummary() { return reportSummary; }
    public Instant getVerifiedAt() { return verifiedAt; }
    public Map<String, Object> getResultData() { return resultData; }
}