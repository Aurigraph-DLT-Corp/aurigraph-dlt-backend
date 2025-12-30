package io.aurigraph.v11.token.secondary;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Secondary Token Version Entity
 *
 * Represents a version of a secondary token with full lifecycle management,
 * VVB approval workflow integration, and Merkle proof verification.
 *
 * Versioning Strategy:
 * - Each secondary token can have multiple versions
 * - Only one version can be ACTIVE at a time
 * - Versions form a chain (previous_version_id links)
 * - All versions are retained for audit and compliance
 * - Each version has unique (secondary_token_id, version_number) pair
 *
 * VVB Workflow:
 * - Versions can require Virtual Validator Board approval
 * - VVB approval gates movement from PENDING_VVB → ACTIVE
 * - Approval includes timestamp and approver identity
 * - Rejections include reason for audit
 *
 * @version 12.0.0
 * @since December 23, 2025
 */
@Entity
@Table(name = "secondary_token_versions", indexes = {
        @Index(name = "idx_stv_token_id", columnList = "secondary_token_id"),
        @Index(name = "idx_stv_version_num", columnList = "version_number"),
        @Index(name = "idx_stv_status", columnList = "status"),
        @Index(name = "idx_stv_token_status", columnList = "secondary_token_id, status"),
        @Index(name = "idx_stv_created_at", columnList = "created_at"),
        @Index(name = "idx_stv_vvb_pending", columnList = "secondary_token_id, status")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SecondaryTokenVersion extends PanacheEntity {

    // =========================================================================
    // Core Identity Fields
    // =========================================================================

    /**
     * UUID identifier for this version
     * Note: Uses Panache's inherited `id` field as primary key (Long).
     * This field provides business-level UUID unique identification.
     */
    @NotNull
    @Column(columnDefinition = "UUID", nullable = false)
    public UUID id;

    /**
     * Reference to the parent secondary token
     */
    @NotNull
    @Column(name = "secondary_token_id", columnDefinition = "UUID", nullable = false)
    public UUID secondaryTokenId;

    /**
     * Version number (1, 2, 3, ...)
     * Unique per token with secondary_token_id
     */
    @NotNull
    @Positive
    @Column(name = "version_number", nullable = false)
    public Integer versionNumber;

    // =========================================================================
    // Content & Integrity Fields
    // =========================================================================

    /**
     * Version content as JSON
     * Stores the actual token data for this version
     */
    @NotNull
    @Column(name = "content", columnDefinition = "JSONB", nullable = false)
    public String content;

    /**
     * SHA-256 hash of version content
     * Used for integrity verification and Merkle proof generation
     */
    @Column(name = "merkle_hash", length = 64)
    public String merkleHash;

    // =========================================================================
    // Versioning Chain
    // =========================================================================

    /**
     * Reference to previous version (forms version chain)
     * Null for first version
     */
    @Column(name = "previous_version_id", columnDefinition = "UUID")
    public UUID previousVersionId;

    // =========================================================================
    // Status & Lifecycle
    // =========================================================================

    /**
     * Current status of this version
     * Controls allowed transitions and operations
     */
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    public SecondaryTokenVersionStatus status;

    /**
     * Timestamp when version was replaced by newer version
     */
    @Column(name = "replaced_at")
    public LocalDateTime replacedAt;

    /**
     * ID of version that replaced this one
     */
    @Column(name = "replaced_by_version_id", columnDefinition = "UUID")
    public UUID replacedByVersionId;

    // =========================================================================
    // VVB Approval Workflow
    // =========================================================================

    /**
     * Whether this version requires VVB approval
     * Determines if PENDING_VVB state is required
     */
    @Column(name = "vvb_required")
    public Boolean vvbRequired = false;

    /**
     * Timestamp of VVB approval
     */
    @Column(name = "vvb_approved_at")
    public LocalDateTime vvbApprovedAt;

    /**
     * Identifier of approver (VVB member or system)
     */
    @Column(name = "vvb_approved_by", length = 256)
    public String vvbApprovedBy;

    /**
     * Rejection reason if version was rejected
     */
    @Column(name = "rejection_reason")
    public String rejectionReason;

    // =========================================================================
    // Approval Execution (Story 6)
    // =========================================================================

    /**
     * Reference to VVB approval request (Story 5 integration)
     */
    @Column(name = "approval_request_id", columnDefinition = "UUID")
    public UUID approvalRequestId;

    /**
     * Approval threshold percentage (e.g., 66.67 for >2/3)
     */
    @Column(name = "approval_threshold_percentage")
    public Double approvalThresholdPercentage;

    /**
     * Count of validators who approved this version
     */
    @Column(name = "approved_by_count")
    public Integer approvedByCount;

    /**
     * Timestamp when approval was granted
     */
    @Column(name = "approval_timestamp")
    public LocalDateTime approvalTimestamp;

    /**
     * JSON array of approver identifiers
     */
    @Column(name = "approvers_list", columnDefinition = "JSONB")
    public String approversList;

    /**
     * Deadline for approval voting window
     */
    @Column(name = "approval_expiry_deadline")
    public LocalDateTime approvalExpiryDeadline;

    /**
     * Timestamp when version transitioned to ACTIVE status
     */
    @Column(name = "activated_at")
    public LocalDateTime activatedAt;

    // =========================================================================
    // Audit Timestamps
    // =========================================================================

    /**
     * When this version was created
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    public LocalDateTime createdAt;

    /**
     * When this version was last updated
     */
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    public LocalDateTime updatedAt;

    /**
     * When this version was archived
     */
    @Column(name = "archived_at")
    public LocalDateTime archivedAt;

    // =========================================================================
    // Panache Query Helper Methods
    // =========================================================================

    /**
     * Find all versions of a token, ordered by version number descending
     *
     * @param tokenId The secondary token ID
     * @return List of versions for the token
     */
    public static List<SecondaryTokenVersion> findBySecondaryTokenId(UUID tokenId) {
        return find("secondaryTokenId = ?1 order by versionNumber desc", tokenId).list();
    }

    /**
     * Find the currently active version of a token
     *
     * @param tokenId The secondary token ID
     * @return Active version or null if none found
     */
    public static SecondaryTokenVersion findActiveVersion(UUID tokenId) {
        return find("secondaryTokenId = ?1 and status = ?2 order by versionNumber desc",
                tokenId, SecondaryTokenVersionStatus.ACTIVE).firstResult();
    }

    /**
     * Get complete version chain (linked by previousVersionId)
     *
     * @param tokenId The secondary token ID
     * @return List of versions in chain order
     */
    public static List<SecondaryTokenVersion> findVersionChain(UUID tokenId) {
        return find("secondaryTokenId = ?1 order by versionNumber asc", tokenId).list();
    }

    /**
     * Find all versions pending VVB approval
     *
     * @return List of versions in PENDING_VVB status
     */
    public static List<SecondaryTokenVersion> findPendingVVBApproval() {
        return find("status = ?1 order by createdAt asc", SecondaryTokenVersionStatus.PENDING_VVB).list();
    }

    /**
     * Find versions by status
     *
     * @param status The status to filter by
     * @param tokenId The secondary token ID (optional, use null for all)
     * @return List of versions with given status
     */
    public static List<SecondaryTokenVersion> findByStatus(SecondaryTokenVersionStatus status, UUID tokenId) {
        if (tokenId != null) {
            return find("secondaryTokenId = ?1 and status = ?2 order by versionNumber desc",
                    tokenId, status).list();
        }
        return find("status = ?1 order by createdAt desc", status).list();
    }

    /**
     * Find expired versions (by status and age)
     *
     * @param maxAgeMinutes Maximum age before considering expired
     * @return List of expired versions
     */
    public static List<SecondaryTokenVersion> findExpiredVersions(int maxAgeMinutes) {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(maxAgeMinutes);
        return find("status = ?1 and createdAt < ?2 order by createdAt asc",
                SecondaryTokenVersionStatus.EXPIRED, cutoff).list();
    }

    /**
     * Count active versions for a token (should be 0 or 1)
     *
     * @param tokenId The secondary token ID
     * @return Count of active versions
     */
    public static long countActiveVersions(UUID tokenId) {
        return count("secondaryTokenId = ?1 and status = ?2",
                tokenId, SecondaryTokenVersionStatus.ACTIVE);
    }

    /**
     * Get next version number for a token
     *
     * @param tokenId The secondary token ID
     * @return Next version number (1-indexed)
     */
    public static Integer getNextVersionNumber(UUID tokenId) {
        Object maxVersion = find("select max(versionNumber) from SecondaryTokenVersion where secondaryTokenId = ?1",
                tokenId).singleResult();
        if (maxVersion == null) {
            return 1;
        }
        if (maxVersion instanceof Number) {
            return ((Number) maxVersion).intValue() + 1;
        }
        return 1;
    }

    // =========================================================================
    // Validation Methods
    // =========================================================================

    /**
     * Validate version state and content
     * Checks required fields and status consistency
     *
     * @throws IllegalStateException if validation fails
     */
    public void validate() {
        if (secondaryTokenId == null) {
            throw new IllegalStateException("secondaryTokenId cannot be null");
        }
        if (versionNumber == null || versionNumber <= 0) {
            throw new IllegalStateException("versionNumber must be positive");
        }
        if (status == null) {
            throw new IllegalStateException("status cannot be null");
        }
        if (content == null || content.isEmpty()) {
            throw new IllegalStateException("content cannot be null or empty");
        }

        // Consistency checks for status-specific fields
        if (status == SecondaryTokenVersionStatus.ACTIVE && merkleHash == null) {
            throw new IllegalStateException("ACTIVE version must have merkleHash");
        }
        if (status == SecondaryTokenVersionStatus.REPLACED && replacedAt == null) {
            throw new IllegalStateException("REPLACED version must have replacedAt timestamp");
        }
        if (status == SecondaryTokenVersionStatus.REJECTED && rejectionReason == null) {
            throw new IllegalStateException("REJECTED version must have rejectionReason");
        }
    }

    /**
     * Check if version is in a terminal state
     *
     * @return true if status is ARCHIVED or EXPIRED
     */
    public boolean isTerminal() {
        return status == SecondaryTokenVersionStatus.ARCHIVED ||
               status == SecondaryTokenVersionStatus.EXPIRED;
    }

    /**
     * Check if version is waiting for approval
     *
     * @return true if status is PENDING_VVB
     */
    public boolean isPendingApproval() {
        return status == SecondaryTokenVersionStatus.PENDING_VVB;
    }

    /**
     * Check if version is active and usable
     *
     * @return true if status is ACTIVE
     */
    public boolean isActive() {
        return status == SecondaryTokenVersionStatus.ACTIVE;
    }

    @Override
    public String toString() {
        return String.format("SecondaryTokenVersion[id=%s, tokenId=%s, version=%d, status=%s, hash=%s, created=%s]",
                id, secondaryTokenId, versionNumber, status,
                merkleHash != null ? merkleHash.substring(0, 8) + "..." : "null",
                createdAt);
    }
}
