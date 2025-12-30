package io.aurigraph.v11.credentials.models;

import java.time.Instant;
import java.util.Objects;

/**
 * Credential Model for Identity Verification
 *
 * Represents a verifiable credential issued by an authority
 * with cryptographic verification support.
 *
 * @version 11.5.0
 * @since 2025-10-30 - AV11-457: CredentialRegistry Merkle Tree
 */
public class Credential {
    private String credentialId;
    private String userId;
    private String credentialType;
    private String issuer;
    private String subject;
    private String verificationHash;
    private Instant issuedAt;
    private Instant expiresAt;
    private CredentialStatus status;
    private String metadata;
    private int revisionNumber;

    public enum CredentialStatus {
        ACTIVE,
        EXPIRED,
        REVOKED,
        SUSPENDED,
        ARCHIVED
    }

    public Credential() {
        this.revisionNumber = 1;
        this.status = CredentialStatus.ACTIVE;
    }

    public Credential(String credentialId, String userId, String credentialType,
                     String issuer, String subject, Instant issuedAt, Instant expiresAt) {
        this.credentialId = credentialId;
        this.userId = userId;
        this.credentialType = credentialType;
        this.issuer = issuer;
        this.subject = subject;
        this.issuedAt = issuedAt;
        this.expiresAt = expiresAt;
        this.status = CredentialStatus.ACTIVE;
        this.revisionNumber = 1;
    }

    // Getters and Setters
    public String getCredentialId() {
        return credentialId;
    }

    public void setCredentialId(String credentialId) {
        this.credentialId = credentialId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getCredentialType() {
        return credentialType;
    }

    public void setCredentialType(String credentialType) {
        this.credentialType = credentialType;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getVerificationHash() {
        return verificationHash;
    }

    public void setVerificationHash(String verificationHash) {
        this.verificationHash = verificationHash;
    }

    public Instant getIssuedAt() {
        return issuedAt;
    }

    public void setIssuedAt(Instant issuedAt) {
        this.issuedAt = issuedAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public CredentialStatus getStatus() {
        return status;
    }

    public void setStatus(CredentialStatus status) {
        this.status = status;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }

    public int getRevisionNumber() {
        return revisionNumber;
    }

    public void setRevisionNumber(int revisionNumber) {
        this.revisionNumber = revisionNumber;
    }

    public boolean isExpired() {
        return expiresAt != null && Instant.now().isAfter(expiresAt);
    }

    public boolean isValid() {
        return status == CredentialStatus.ACTIVE && !isExpired();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Credential that = (Credential) o;
        return Objects.equals(credentialId, that.credentialId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(credentialId);
    }

    @Override
    public String toString() {
        return "Credential{" +
                "credentialId='" + credentialId + '\'' +
                ", userId='" + userId + '\'' +
                ", credentialType='" + credentialType + '\'' +
                ", status=" + status +
                ", isExpired=" + isExpired() +
                '}';
    }
}
