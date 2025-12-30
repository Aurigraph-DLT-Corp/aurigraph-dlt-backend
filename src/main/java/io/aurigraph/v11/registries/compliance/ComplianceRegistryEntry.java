package io.aurigraph.v11.registries.compliance;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.*;

/**
 * Compliance Registry Entry DTO
 *
 * Represents a single compliance certification record in the registry.
 * Tracks the complete lifecycle of a certification from issuance through renewal or revocation.
 *
 * Certification Status:
 * - ACTIVE: Currently valid and in effect
 * - EXPIRED: Past expiry date
 * - REVOKED: Manually revoked
 * - PENDING_RENEWAL: Renewal request submitted
 *
 * Supported Standards:
 * - ISO (27001, 27002, 9001, 14001)
 * - SOC 2 (Type I, Type II)
 * - NIST (SP 800-53, SP 800-171)
 * - ERC-3643 (Compliant Token Standard)
 * - GDPR, CCPA, HIPAA
 * - PCI DSS
 *
 * @version 11.5.0
 * @since 2025-11-14
 */
public class ComplianceRegistryEntry {

    @JsonProperty("certificationId")
    private String certificationId;

    @JsonProperty("entityId")
    private String entityId;

    @JsonProperty("certificationType")
    private String certificationType;  // ISO-27001, SOC2-Type-II, NIST-SP800-53, ERC-3643, etc.

    @JsonProperty("issuingAuthority")
    private String issuingAuthority;   // e.g., ISAE, PCI-SSC, NIST, Aurigraph

    @JsonProperty("issuanceDate")
    private Instant issuanceDate;

    @JsonProperty("expiryDate")
    private Instant expiryDate;

    @JsonProperty("currentStatus")
    private CertificationStatus currentStatus;

    @JsonProperty("complianceLevel")
    private ComplianceLevelEnum complianceLevel;

    @JsonProperty("certificateNumber")
    private String certificateNumber;  // Official certificate number from issuer

    @JsonProperty("certificationScope")
    private String certificationScope;  // What the cert covers (e.g., "Data Processing", "Cloud Services")

    @JsonProperty("auditTrail")
    private List<AuditEvent> auditTrail = new ArrayList<>();

    @JsonProperty("verificationMetadata")
    private Map<String, Object> verificationMetadata = new HashMap<>();

    @JsonProperty("createdAt")
    private Instant createdAt;

    @JsonProperty("updatedAt")
    private Instant updatedAt;

    @JsonProperty("lastRenewalDate")
    private Instant lastRenewalDate;

    @JsonProperty("nextRenewalDue")
    private Instant nextRenewalDue;

    @JsonProperty("renewalWindowDays")
    private int renewalWindowDays = 90;  // Renewal window opens 90 days before expiry

    // Certification Status Enum
    public enum CertificationStatus {
        ACTIVE,
        EXPIRED,
        REVOKED,
        PENDING_RENEWAL,
        SUSPENDED,
        PROVISIONAL
    }

    // Audit Event for tracking changes
    public static class AuditEvent {
        @JsonProperty("timestamp")
        public Instant timestamp;

        @JsonProperty("eventType")
        public String eventType;  // CREATED, RENEWED, REVOKED, VERIFIED, etc.

        @JsonProperty("description")
        public String description;

        @JsonProperty("performedBy")
        public String performedBy;

        public AuditEvent(String eventType, String description, String performedBy) {
            this.timestamp = Instant.now();
            this.eventType = eventType;
            this.description = description;
            this.performedBy = performedBy;
        }

        public AuditEvent() {}
    }

    // Constructors
    public ComplianceRegistryEntry() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public ComplianceRegistryEntry(String entityId, String certificationType,
                                   String issuingAuthority, String certificationId,
                                   Instant issuanceDate, Instant expiryDate) {
        this();
        this.entityId = entityId;
        this.certificationType = certificationType;
        this.issuingAuthority = issuingAuthority;
        this.certificationId = certificationId;
        this.issuanceDate = issuanceDate;
        this.expiryDate = expiryDate;
        this.currentStatus = CertificationStatus.ACTIVE;
    }

    // Getters and Setters
    public String getCertificationId() { return certificationId; }
    public void setCertificationId(String certificationId) { this.certificationId = certificationId; }

    public String getEntityId() { return entityId; }
    public void setEntityId(String entityId) { this.entityId = entityId; }

    public String getCertificationType() { return certificationType; }
    public void setCertificationType(String certificationType) { this.certificationType = certificationType; }

    public String getIssuingAuthority() { return issuingAuthority; }
    public void setIssuingAuthority(String issuingAuthority) { this.issuingAuthority = issuingAuthority; }

    public Instant getIssuanceDate() { return issuanceDate; }
    public void setIssuanceDate(Instant issuanceDate) { this.issuanceDate = issuanceDate; }

    public Instant getExpiryDate() { return expiryDate; }
    public void setExpiryDate(Instant expiryDate) { this.expiryDate = expiryDate; }

    public CertificationStatus getCurrentStatus() { return currentStatus; }
    public void setCurrentStatus(CertificationStatus currentStatus) {
        this.currentStatus = currentStatus;
        this.updatedAt = Instant.now();
    }

    public ComplianceLevelEnum getComplianceLevel() { return complianceLevel; }
    public void setComplianceLevel(ComplianceLevelEnum complianceLevel) {
        this.complianceLevel = complianceLevel;
    }

    public String getCertificateNumber() { return certificateNumber; }
    public void setCertificateNumber(String certificateNumber) { this.certificateNumber = certificateNumber; }

    public String getCertificationScope() { return certificationScope; }
    public void setCertificationScope(String certificationScope) { this.certificationScope = certificationScope; }

    public List<AuditEvent> getAuditTrail() { return auditTrail; }
    public void setAuditTrail(List<AuditEvent> auditTrail) { this.auditTrail = auditTrail; }

    public Map<String, Object> getVerificationMetadata() { return verificationMetadata; }
    public void setVerificationMetadata(Map<String, Object> verificationMetadata) {
        this.verificationMetadata = verificationMetadata;
    }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public Instant getLastRenewalDate() { return lastRenewalDate; }
    public void setLastRenewalDate(Instant lastRenewalDate) { this.lastRenewalDate = lastRenewalDate; }

    public Instant getNextRenewalDue() { return nextRenewalDue; }
    public void setNextRenewalDue(Instant nextRenewalDue) { this.nextRenewalDue = nextRenewalDue; }

    public int getRenewalWindowDays() { return renewalWindowDays; }
    public void setRenewalWindowDays(int renewalWindowDays) { this.renewalWindowDays = renewalWindowDays; }

    // Business logic methods

    /**
     * Check if certification is currently expired
     */
    public boolean isExpired() {
        return expiryDate != null && expiryDate.isBefore(Instant.now());
    }

    /**
     * Check if renewal window is open (within renewal window before expiry)
     */
    public boolean isRenewalWindowOpen() {
        if (expiryDate == null) {
            return false;
        }
        Instant renewalStartDate = expiryDate.minusSeconds((long) renewalWindowDays * 24 * 60 * 60);
        return !Instant.now().isBefore(renewalStartDate) && Instant.now().isBefore(expiryDate);
    }

    /**
     * Get days until expiry
     */
    public long getDaysUntilExpiry() {
        if (expiryDate == null) {
            return -1;
        }
        long secondsUntilExpiry = expiryDate.getEpochSecond() - Instant.now().getEpochSecond();
        return secondsUntilExpiry / (24 * 60 * 60);
    }

    /**
     * Check if certification is in critical renewal window (last 30 days)
     */
    public boolean isInCriticalRenewalWindow() {
        long daysUntilExpiry = getDaysUntilExpiry();
        return daysUntilExpiry >= 0 && daysUntilExpiry <= 30;
    }

    /**
     * Add audit event to trail
     */
    public void addAuditEvent(String eventType, String description, String performedBy) {
        auditTrail.add(new AuditEvent(eventType, description, performedBy));
    }

    /**
     * Add metadata entry
     */
    public void addMetadata(String key, Object value) {
        verificationMetadata.put(key, value);
    }

    /**
     * Update certification renewal
     */
    public void renew(Instant newExpiryDate) {
        this.lastRenewalDate = Instant.now();
        this.expiryDate = newExpiryDate;
        this.nextRenewalDue = newExpiryDate.minusSeconds((long) renewalWindowDays * 24 * 60 * 60);
        this.currentStatus = CertificationStatus.ACTIVE;
        this.updatedAt = Instant.now();
        addAuditEvent("RENEWED", "Certification renewed until " + newExpiryDate, "SYSTEM");
    }

    /**
     * Revoke certification
     */
    public void revoke(String reason) {
        this.currentStatus = CertificationStatus.REVOKED;
        this.updatedAt = Instant.now();
        addAuditEvent("REVOKED", "Revocation reason: " + reason, "SYSTEM");
    }

    @Override
    public String toString() {
        return String.format("ComplianceEntry{id='%s', entity='%s', type='%s', status=%s, level=%s, expires=%s}",
                certificationId, entityId, certificationType, currentStatus, complianceLevel, expiryDate);
    }
}
