package io.aurigraph.v11.compliance.persistence;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * JPA Repository for TransferAuditEntity
 * Provides audit trail persistence for transfer compliance checks
 */
@ApplicationScoped
public class TransferAuditRepository implements PanacheRepository<TransferAuditEntity> {

    /**
     * Find transfer history for a token
     */
    public List<TransferAuditEntity> findByTokenId(String tokenId) {
        return find("token_id", tokenId).list();
    }

    /**
     * Find transfers from an address
     */
    public List<TransferAuditEntity> findFromAddress(String address) {
        return find("from_address", address).list();
    }

    /**
     * Find transfers to an address
     */
    public List<TransferAuditEntity> findToAddress(String address) {
        return find("to_address", address).list();
    }

    /**
     * Find approved transfers
     */
    public List<TransferAuditEntity> findApprovedTransfers(String tokenId) {
        return find("token_id = ?1 AND success = true", tokenId).list();
    }

    /**
     * Find rejected transfers
     */
    public List<TransferAuditEntity> findRejectedTransfers(String tokenId) {
        return find("token_id = ?1 AND success = false", tokenId).list();
    }

    /**
     * Count approved transfers for token
     */
    public long countApproved(String tokenId) {
        return count("token_id = ?1 AND success = true", tokenId);
    }

    /**
     * Count rejected transfers for token
     */
    public long countRejected(String tokenId) {
        return count("token_id = ?1 AND success = false", tokenId);
    }

    /**
     * Get total transfer amount for token
     */
    public BigDecimal getTotalTransferAmount(String tokenId) {
        return find("SELECT SUM(amount) FROM TransferAuditEntity WHERE token_id = ?1 AND success = true",
            tokenId).project(BigDecimal.class).firstResult();
    }

    /**
     * Get transfer history between two addresses
     */
    public List<TransferAuditEntity> findBetweenAddresses(String tokenId, String from, String to) {
        return find("token_id = ?1 AND from_address = ?2 AND to_address = ?3",
            tokenId, from, to).list();
    }

    /**
     * Get recent transfers (last N hours)
     */
    public List<TransferAuditEntity> findRecentTransfers(String tokenId, long hours) {
        Instant since = Instant.now().minusSeconds(hours * 3600);
        return find("token_id = ?1 AND transaction_timestamp > ?2", tokenId, since).list();
    }
}
