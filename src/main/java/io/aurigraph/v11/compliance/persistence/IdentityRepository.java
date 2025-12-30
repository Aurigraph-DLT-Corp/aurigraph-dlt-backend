package io.aurigraph.v11.compliance.persistence;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.Optional;

/**
 * JPA Repository for IdentityEntity
 * Provides database access for verified identities
 */
@ApplicationScoped
public class IdentityRepository implements PanacheRepository<IdentityEntity> {

    /**
     * Find identity by address
     */
    public Optional<IdentityEntity> findByAddress(String address) {
        return find("address", address).firstResultOptional();
    }

    /**
     * Find all active identities
     */
    public List<IdentityEntity> findActiveIdentities() {
        return find("status", "ACTIVE").list();
    }

    /**
     * Find all identities for a country
     */
    public List<IdentityEntity> findByCountry(String country) {
        return find("country", country).list();
    }

    /**
     * Find identities by KYC level
     */
    public List<IdentityEntity> findByKycLevel(String kycLevel) {
        return find("kyc_level", kycLevel).list();
    }

    /**
     * Find revoked identities
     */
    public List<IdentityEntity> findRevokedIdentities() {
        return find("status", "REVOKED").list();
    }

    /**
     * Count active identities
     */
    public long countActive() {
        return count("status", "ACTIVE");
    }

    /**
     * Count identities by country
     */
    public long countByCountry(String country) {
        return count("country", country);
    }

    /**
     * Delete identity by address (soft delete via status update)
     */
    public void markAsRevoked(String address, String reason) {
        IdentityEntity entity = findByAddress(address).orElse(null);
        if (entity != null) {
            entity.status = "REVOKED";
            entity.revocationReason = reason;
            entity.revokedAt = java.time.Instant.now();
            persist(entity);
        }
    }

    /**
     * Restore revoked identity
     */
    public void markAsActive(String address) {
        IdentityEntity entity = findByAddress(address).orElse(null);
        if (entity != null) {
            entity.status = "ACTIVE";
            entity.revocationReason = null;
            entity.revokedAt = null;
            persist(entity);
        }
    }
}
