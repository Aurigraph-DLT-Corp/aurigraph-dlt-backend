package io.aurigraph.v11.registries.compliance;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.*;

/**
 * Compliance Certification DTO
 *
 * Detailed representation of a compliance certification with verification data,
 * audit trails, and metadata. Used for comprehensive certification management
 * and verification workflows.
 *
 * @version 11.5.0
 * @since 2025-11-14
 */
public class ComplianceCertification {

    @JsonProperty("certificateType")
    private String certificateType;  // ISO-27001, SOC2-Type-II, etc.

    @JsonProperty("issuingAuthority")
    private String issuingAuthority;

    @JsonProperty("issuanceDate")
    private Instant issuanceDate;

    @JsonProperty("expiryDate")
    private Instant expiryDate;

    @JsonProperty("certificateNumber")
    private String certificateNumber;

    @JsonProperty("certificateHash")
    private String certificateHash;  // SHA-256 hash of certificate document

    @JsonProperty("documentUrl")
    private String documentUrl;  // URL to certificate document

    @JsonProperty("verificationStatus")
    private VerificationStatus verificationStatus;

    @JsonProperty("auditTrail")
    private List<AuditTrailEntry> auditTrail = new ArrayList<>();

    @JsonProperty("verificationMetadata")
    private VerificationMetadata verificationMetadata;

    @JsonProperty("validationScope")
    private String validationScope;  // What this certificate validates

    @JsonProperty("complianceStandards")
    private List<String> complianceStandards = new ArrayList<>();  // ISO, SOC2, NIST, etc.

    @JsonProperty("jurisdiction")
    private String jurisdiction;  // Geographic/regulatory jurisdiction

    @JsonProperty("tags")
    private Set<String> tags = new HashSet<>();

    // Verification Status Enum
    public enum VerificationStatus {
        PENDING,
        VERIFIED,
        FAILED_VERIFICATION,
        REVOKED,
        SUSPENDED,
        EXPIRED
    }

    // Audit Trail Entry
    public static class AuditTrailEntry {
        @JsonProperty("timestamp")
        public Instant timestamp;

        @JsonProperty("action")
        public String action;

        @JsonProperty("details")
        public String details;

        @JsonProperty("verifier")
        public String verifier;

        public AuditTrailEntry(String action, String details, String verifier) {
            this.timestamp = Instant.now();
            this.action = action;
            this.details = details;
            this.verifier = verifier;
        }

        public AuditTrailEntry() {}
    }

    // Verification Metadata
    public static class VerificationMetadata {
        @JsonProperty("verifiedAt")
        public Instant verifiedAt;

        @JsonProperty("verifiedBy")
        public String verifiedBy;

        @JsonProperty("verificationMethod")
        public String verificationMethod;  // MANUAL, AUTOMATED, BLOCKCHAIN, etc.

        @JsonProperty("confidenceScore")
        public double confidenceScore;  // 0-100

        @JsonProperty("blockchainHash")
        public String blockchainHash;  // Hash on blockchain for immutable proof

        @JsonProperty("onChainVerified")
        public boolean onChainVerified;

        @JsonProperty("additionalMetadata")
        public Map<String, Object> additionalMetadata = new HashMap<>();

        public VerificationMetadata() {}

        public VerificationMetadata(String verifiedBy, double confidenceScore) {
            this.verifiedAt = Instant.now();
            this.verifiedBy = verifiedBy;
            this.confidenceScore = confidenceScore;
            this.verificationMethod = "MANUAL";
        }
    }

    // Constructors
    public ComplianceCertification() {}

    public ComplianceCertification(String certificateType, String issuingAuthority,
                                   Instant issuanceDate, Instant expiryDate) {
        this.certificateType = certificateType;
        this.issuingAuthority = issuingAuthority;
        this.issuanceDate = issuanceDate;
        this.expiryDate = expiryDate;
        this.verificationStatus = VerificationStatus.PENDING;
    }

    // Getters and Setters
    public String getCertificateType() { return certificateType; }
    public void setCertificateType(String certificateType) { this.certificateType = certificateType; }

    public String getIssuingAuthority() { return issuingAuthority; }
    public void setIssuingAuthority(String issuingAuthority) { this.issuingAuthority = issuingAuthority; }

    public Instant getIssuanceDate() { return issuanceDate; }
    public void setIssuanceDate(Instant issuanceDate) { this.issuanceDate = issuanceDate; }

    public Instant getExpiryDate() { return expiryDate; }
    public void setExpiryDate(Instant expiryDate) { this.expiryDate = expiryDate; }

    public String getCertificateNumber() { return certificateNumber; }
    public void setCertificateNumber(String certificateNumber) { this.certificateNumber = certificateNumber; }

    public String getCertificateHash() { return certificateHash; }
    public void setCertificateHash(String certificateHash) { this.certificateHash = certificateHash; }

    public String getDocumentUrl() { return documentUrl; }
    public void setDocumentUrl(String documentUrl) { this.documentUrl = documentUrl; }

    public VerificationStatus getVerificationStatus() { return verificationStatus; }
    public void setVerificationStatus(VerificationStatus verificationStatus) {
        this.verificationStatus = verificationStatus;
    }

    public List<AuditTrailEntry> getAuditTrail() { return auditTrail; }
    public void setAuditTrail(List<AuditTrailEntry> auditTrail) { this.auditTrail = auditTrail; }

    public VerificationMetadata getVerificationMetadata() { return verificationMetadata; }
    public void setVerificationMetadata(VerificationMetadata verificationMetadata) {
        this.verificationMetadata = verificationMetadata;
    }

    public String getValidationScope() { return validationScope; }
    public void setValidationScope(String validationScope) { this.validationScope = validationScope; }

    public List<String> getComplianceStandards() { return complianceStandards; }
    public void setComplianceStandards(List<String> complianceStandards) {
        this.complianceStandards = complianceStandards;
    }

    public String getJurisdiction() { return jurisdiction; }
    public void setJurisdiction(String jurisdiction) { this.jurisdiction = jurisdiction; }

    public Set<String> getTags() { return tags; }
    public void setTags(Set<String> tags) { this.tags = tags; }

    // Business logic methods

    /**
     * Add audit trail entry
     */
    public void addAuditEntry(String action, String details, String verifier) {
        auditTrail.add(new AuditTrailEntry(action, details, verifier));
    }

    /**
     * Add compliance standard
     */
    public void addComplianceStandard(String standard) {
        if (!complianceStandards.contains(standard)) {
            complianceStandards.add(standard);
        }
    }

    /**
     * Add tag
     */
    public void addTag(String tag) {
        tags.add(tag);
    }

    /**
     * Check if certificate is currently valid
     */
    public boolean isValid() {
        if (expiryDate == null) {
            return true;
        }
        boolean notExpired = expiryDate.isAfter(Instant.now());
        boolean verified = verificationStatus == VerificationStatus.VERIFIED;
        return notExpired && verified;
    }

    /**
     * Check if certificate is expired
     */
    public boolean isExpired() {
        return expiryDate != null && expiryDate.isBefore(Instant.now());
    }

    /**
     * Get days until expiry
     */
    public long getDaysUntilExpiry() {
        if (expiryDate == null) {
            return Long.MAX_VALUE;
        }
        long secondsUntilExpiry = expiryDate.getEpochSecond() - Instant.now().getEpochSecond();
        return secondsUntilExpiry / (24 * 60 * 60);
    }

    /**
     * Verify certificate
     */
    public void verify(String verifiedBy, double confidenceScore, String blockchainHash) {
        this.verificationStatus = VerificationStatus.VERIFIED;
        this.verificationMetadata = new VerificationMetadata(verifiedBy, confidenceScore);
        if (blockchainHash != null) {
            this.verificationMetadata.blockchainHash = blockchainHash;
            this.verificationMetadata.onChainVerified = true;
        }
        addAuditEntry("VERIFIED", String.format("Verified by %s with %.1f%% confidence", 
            verifiedBy, confidenceScore), verifiedBy);
    }

    /**
     * Mark as revoked
     */
    public void revoke(String reason, String revokedBy) {
        this.verificationStatus = VerificationStatus.REVOKED;
        addAuditEntry("REVOKED", "Reason: " + reason, revokedBy);
    }

    @Override
    public String toString() {
        return String.format("Certification{type='%s', issuer='%s', status=%s, expires=%s}",
                certificateType, issuingAuthority, verificationStatus, expiryDate);
    }
}
