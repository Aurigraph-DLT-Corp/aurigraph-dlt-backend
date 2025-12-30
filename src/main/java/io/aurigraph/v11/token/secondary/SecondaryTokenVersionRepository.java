package io.aurigraph.v11.token.secondary;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Secondary Token Version Repository
 *
 * Panache repository for SecondaryTokenVersion entity.
 * Provides database access and query methods for token version management.
 *
 * Query Performance:
 * - Token ID lookup: O(1) with index idx_stv_token_id
 * - Status queries: O(n) with index idx_stv_status (n = versions with status)
 * - Version chain queries: O(n) with secondary sort
 * - VVB pending queries: O(m) with partial index idx_stv_vvb_pending
 *
 * @version 12.0.0
 * @since December 23, 2025
 */
@ApplicationScoped
public class SecondaryTokenVersionRepository implements PanacheRepository<SecondaryTokenVersion> {

    // =========================================================================
    // Token-Based Queries
    // =========================================================================

    /**
     * Find all versions for a secondary token
     *
     * @param tokenId The secondary token ID
     * @return List of versions, ordered by version number descending
     */
    public List<SecondaryTokenVersion> findBySecondaryTokenId(UUID tokenId) {
        return find("secondaryTokenId = ?1 order by versionNumber desc", tokenId).list();
    }

    /**
     * Find active version for a token
     * Only one version should be ACTIVE at a time
     *
     * @param tokenId The secondary token ID
     * @return The active version or null if none
     */
    public SecondaryTokenVersion findActiveVersion(UUID tokenId) {
        return find("secondaryTokenId = ?1 and status = ?2 order by versionNumber desc",
                tokenId, SecondaryTokenVersionStatus.ACTIVE)
                .firstResult();
    }

    /**
     * Get complete version chain for a token
     * Returns versions in ascending order (oldest to newest)
     *
     * @param tokenId The secondary token ID
     * @return List of versions in chronological order
     */
    public List<SecondaryTokenVersion> findVersionChain(UUID tokenId) {
        return find("secondaryTokenId = ?1 order by versionNumber asc", tokenId).list();
    }

    /**
     * Count total versions for a token
     *
     * @param tokenId The secondary token ID
     * @return Number of versions
     */
    public long countBySecondaryTokenId(UUID tokenId) {
        return count("secondaryTokenId", tokenId);
    }

    // =========================================================================
    // Status-Based Queries
    // =========================================================================

    /**
     * Find versions by token and status
     *
     * @param tokenId The secondary token ID
     * @param status The status to filter by
     * @return List of versions with given status
     */
    public List<SecondaryTokenVersion> findBySecondaryTokenIdAndStatus(
            UUID tokenId, SecondaryTokenVersionStatus status) {
        return find("secondaryTokenId = ?1 and status = ?2 order by versionNumber desc",
                tokenId, status)
                .list();
    }

    /**
     * Find all versions with given status (all tokens)
     *
     * @param status The status to filter by
     * @return List of versions with given status
     */
    public List<SecondaryTokenVersion> findByStatus(SecondaryTokenVersionStatus status) {
        return find("status = ?1 order by createdAt desc", status).list();
    }

    // =========================================================================
    // VVB Approval Workflow Queries
    // =========================================================================

    /**
     * Find all versions pending VVB approval
     * Uses partial index for performance
     *
     * @return List of versions in PENDING_VVB status
     */
    public List<SecondaryTokenVersion> findPendingVVBApproval() {
        return find("status = ?1 order by createdAt asc",
                SecondaryTokenVersionStatus.PENDING_VVB)
                .list();
    }

    /**
     * Find pending VVB approvals for specific token
     *
     * @param tokenId The secondary token ID
     * @return List of pending versions for token
     */
    public List<SecondaryTokenVersion> findPendingVVBForToken(UUID tokenId) {
        return find("secondaryTokenId = ?1 and status = ?2 order by createdAt asc",
                tokenId, SecondaryTokenVersionStatus.PENDING_VVB)
                .list();
    }

    /**
     * Count pending VVB approvals
     *
     * @return Number of versions pending approval
     */
    public long countPendingVVBApproval() {
        return count("status", SecondaryTokenVersionStatus.PENDING_VVB);
    }

    // =========================================================================
    // Archive and Retention Queries
    // =========================================================================

    /**
     * Find archived versions older than specified date
     * Used for cleanup and retention management
     *
     * @param cutoffDate Date before which to find archived versions
     * @return List of archived versions older than cutoff
     */
    public List<SecondaryTokenVersion> findArchivedBefore(LocalDateTime cutoffDate) {
        return find("status = ?1 and archivedAt < ?2 order by archivedAt asc",
                SecondaryTokenVersionStatus.ARCHIVED, cutoffDate)
                .list();
    }

    /**
     * Find expired versions
     *
     * @return List of versions with EXPIRED status
     */
    public List<SecondaryTokenVersion> findExpired() {
        return find("status = ?1 order by createdAt asc",
                SecondaryTokenVersionStatus.EXPIRED)
                .list();
    }

    // =========================================================================
    // Content and Integrity Queries
    // =========================================================================

    /**
     * Find version by Merkle hash
     * Used for integrity verification
     *
     * @param merkleHash The SHA-256 hash
     * @return Version with matching hash or null
     */
    public SecondaryTokenVersion findByMerkleHash(String merkleHash) {
        return find("merkleHash = ?1", merkleHash).firstResult();
    }

    /**
     * Find versions without Merkle hash
     * Identifies versions needing hash generation
     *
     * @param tokenId The secondary token ID (optional)
     * @return List of unhashed versions
     */
    public List<SecondaryTokenVersion> findWithoutMerkleHash(UUID tokenId) {
        if (tokenId != null) {
            return find("secondaryTokenId = ?1 and merkleHash is null order by versionNumber desc",
                    tokenId)
                    .list();
        }
        return find("merkleHash is null order by createdAt desc").list();
    }

    // =========================================================================
    // Versioning Chain Queries
    // =========================================================================

    /**
     * Find version by previous version ID
     * Used for chain reconstruction
     *
     * @param previousVersionId The ID of previous version
     * @return Version that links to the previous version
     */
    public SecondaryTokenVersion findByPreviousVersionId(UUID previousVersionId) {
        return find("previousVersionId = ?1", previousVersionId).firstResult();
    }

    /**
     * Find versions with no previous version (first versions)
     *
     * @return List of first versions per token
     */
    public List<SecondaryTokenVersion> findFirstVersions() {
        return find("previousVersionId is null order by createdAt asc").list();
    }

    // =========================================================================
    // Statistics and Monitoring
    // =========================================================================

    /**
     * Count active versions (should equal number of active tokens)
     *
     * @return Number of ACTIVE versions across all tokens
     */
    public long countActiveVersions() {
        return count("status", SecondaryTokenVersionStatus.ACTIVE);
    }

    /**
     * Find versions created between dates
     * Used for audit and statistics
     *
     * @param startDate Start of date range
     * @param endDate End of date range
     * @return List of versions created in range
     */
    public List<SecondaryTokenVersion> findCreatedBetween(LocalDateTime startDate, LocalDateTime endDate) {
        return find("createdAt >= ?1 and createdAt <= ?2 order by createdAt desc",
                startDate, endDate)
                .list();
    }

    /**
     * Find recently updated versions
     * Used for monitoring changes
     *
     * @param maxAgeMinutes Maximum age in minutes
     * @return List of recently updated versions
     */
    public List<SecondaryTokenVersion> findRecentlyUpdated(int maxAgeMinutes) {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(maxAgeMinutes);
        return find("updatedAt >= ?1 order by updatedAt desc", cutoff).list();
    }

    /**
     * Get the highest version number for a token
     *
     * @param tokenId The secondary token ID
     * @return Maximum version number or 0 if no versions exist
     */
    public Integer getMaxVersionNumber(UUID tokenId) {
        Object result = find("select max(versionNumber) from SecondaryTokenVersion where secondaryTokenId = ?1",
                tokenId)
                .singleResult();
        if (result == null) {
            return 0;
        }
        if (result instanceof Number) {
            int value = ((Number) result).intValue();
            return value > 0 ? value : 0;
        }
        return 0;
    }
}
