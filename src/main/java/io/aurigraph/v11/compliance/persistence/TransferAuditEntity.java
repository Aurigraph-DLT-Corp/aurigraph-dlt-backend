package io.aurigraph.v11.compliance.persistence;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * JPA Entity for persisting transfer audit trail
 * Mapped to 'compliance_transfer_audits' table
 */
@Entity
@Table(name = "compliance_transfer_audits", indexes = {
    @Index(name = "idx_transfer_token_id", columnList = "token_id"),
    @Index(name = "idx_transfer_from", columnList = "from_address"),
    @Index(name = "idx_transfer_to", columnList = "to_address"),
    @Index(name = "idx_transfer_success", columnList = "success"),
    @Index(name = "idx_transfer_timestamp", columnList = "transaction_timestamp")
})
public class TransferAuditEntity extends PanacheEntity {

    @Column(name = "token_id", nullable = false, length = 100)
    public String tokenId;

    @Column(name = "from_address", nullable = false, length = 42)
    public String fromAddress;

    @Column(name = "to_address", nullable = false, length = 42)
    public String toAddress;

    @Column(nullable = false, precision = 38, scale = 18)
    public BigDecimal amount;

    @Column(nullable = false)
    public boolean success;

    @Column(name = "reject_reason", columnDefinition = "TEXT")
    public String rejectReason;

    @Column(name = "transaction_timestamp", nullable = false)
    public Instant transactionTimestamp;

    @Column(name = "created_at", nullable = false, updatable = false)
    public Instant createdAt = Instant.now();

    /**
     * Get approval status
     */
    public String getStatus() {
        return success ? "APPROVED" : "REJECTED";
    }
}
