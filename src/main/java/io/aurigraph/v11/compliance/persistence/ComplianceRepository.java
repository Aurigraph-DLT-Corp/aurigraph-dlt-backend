package io.aurigraph.v11.compliance.persistence;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.Optional;

/**
 * JPA Repository for ComplianceEntity
 * Provides persistence for token compliance records
 */
@ApplicationScoped
public class ComplianceRepository implements PanacheRepository<ComplianceEntity> {

    /**
     * Find compliance record by token ID
     */
    public Optional<ComplianceEntity> findByTokenId(String tokenId) {
        return find("token_id", tokenId).firstResultOptional();
    }

    /**
     * Find all compliant tokens
     */
    public List<ComplianceEntity> findCompliant() {
        return find("compliance_status", "COMPLIANT").list();
    }

    /**
     * Find non-compliant tokens
     */
    public List<ComplianceEntity> findNonCompliant() {
        return find("compliance_status", "NON_COMPLIANT").list();
    }

    /**
     * Find tokens by jurisdiction
     */
    public List<ComplianceEntity> findByJurisdiction(String jurisdiction) {
        return find("jurisdiction", jurisdiction).list();
    }

    /**
     * Count compliant tokens
     */
    public long countCompliant() {
        return count("compliance_status", "COMPLIANT");
    }

    /**
     * Count non-compliant tokens
     */
    public long countNonCompliant() {
        return count("compliance_status", "NON_COMPLIANT");
    }

    /**
     * Update compliance status
     */
    public void updateComplianceStatus(String tokenId, String status) {
        ComplianceEntity entity = findByTokenId(tokenId).orElse(null);
        if (entity != null) {
            entity.complianceStatus = status;
            entity.lastCheckTimestamp = java.time.Instant.now();
            persist(entity);
        }
    }

    /**
     * Add certification to token
     */
    public void addCertification(String tokenId, String certificationName) {
        ComplianceEntity entity = findByTokenId(tokenId).orElse(null);
        if (entity != null) {
            if (entity.certifications == null || entity.certifications.isEmpty()) {
                entity.certifications = certificationName;
            } else {
                entity.certifications += "," + certificationName;
            }
            persist(entity);
        }
    }

    /**
     * Get tokens due for compliance check
     */
    public List<ComplianceEntity> getTokensDueForCheck(long hoursAgo) {
        java.time.Instant threshold = java.time.Instant.now().minusSeconds(hoursAgo * 3600);
        return find("last_check_timestamp < ?1 OR last_check_timestamp IS NULL", threshold).list();
    }
}
