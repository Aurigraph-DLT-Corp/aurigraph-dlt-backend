package io.aurigraph.v11.compliance.persistence;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.Objects;

/**
 * JPA Entity for persisting verified identities
 * Mapped to 'compliance_identities' table
 */
@Entity
@Table(name = "compliance_identities", indexes = {
    @Index(name = "idx_identity_address", columnList = "address", unique = true),
    @Index(name = "idx_identity_kyc_level", columnList = "kyc_level"),
    @Index(name = "idx_identity_country", columnList = "country"),
    @Index(name = "idx_identity_status", columnList = "status")
})
public class IdentityEntity extends PanacheEntity {

    @Column(nullable = false, unique = true, length = 42)
    public String address;

    @Column(nullable = false, length = 20)
    public String kycLevel;  // BASIC, ENHANCED, CERTIFIED

    @Column(nullable = false, length = 2)
    public String country;  // ISO 3166-1 alpha-2

    @Column(nullable = false, length = 256)
    public String documentHash;  // SHA-256 hash of KYC document

    @Column(nullable = false, length = 100)
    public String verifierName;  // Name of KYC provider

    @Column(name = "registration_date", nullable = false)
    public Instant registrationDate;

    @Column(name = "expiry_date")
    public Instant expiryDate;

    @Column(length = 50)
    public String status;  // ACTIVE, REVOKED, EXPIRED

    @Column(name = "revocation_reason", length = 255)
    public String revocationReason;

    @Column(name = "revoked_at")
    public Instant revokedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    public Instant createdAt = Instant.now();

    @Column(name = "updated_at")
    public Instant updatedAt = Instant.now();

    @PreUpdate
    public void updateTimestamp() {
        this.updatedAt = Instant.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IdentityEntity that = (IdentityEntity) o;
        return Objects.equals(address, that.address);
    }

    @Override
    public int hashCode() {
        return Objects.hash(address);
    }
}
