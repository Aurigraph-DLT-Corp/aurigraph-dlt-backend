package io.aurigraph.v11.compliance.persistence;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

/**
 * JPA Entity for persisting token compliance records
 * Mapped to 'compliance_records' table
 */
@Entity
@Table(name = "compliance_records", indexes = {
    @Index(name = "idx_compliance_token_id", columnList = "token_id", unique = true),
    @Index(name = "idx_compliance_jurisdiction", columnList = "jurisdiction"),
    @Index(name = "idx_compliance_status", columnList = "compliance_status")
})
public class ComplianceEntity extends PanacheEntity {

    @Column(name = "token_id", nullable = false, unique = true, length = 100)
    public String tokenId;

    @Column(nullable = false, length = 50)
    public String jurisdiction;  // US, UK, EU, GLOBAL, etc.

    @Column(name = "applicable_rules", columnDefinition = "TEXT")
    public String applicableRules;  // Comma-separated list

    @Column(name = "compliance_status", length = 20)
    public String complianceStatus;  // COMPLIANT, NON_COMPLIANT, PENDING

    @Column(name = "last_check_timestamp")
    public Instant lastCheckTimestamp;

    @Column(name = "certifications", columnDefinition = "TEXT")
    public String certifications;  // JSON array of certifications

    @Column(columnDefinition = "TEXT")
    public String metadata;  // Additional metadata as JSON

    @Column(name = "created_at", nullable = false, updatable = false)
    public Instant createdAt = Instant.now();

    @Column(name = "updated_at")
    public Instant updatedAt = Instant.now();

    @PreUpdate
    public void updateTimestamp() {
        this.updatedAt = Instant.now();
    }
}
