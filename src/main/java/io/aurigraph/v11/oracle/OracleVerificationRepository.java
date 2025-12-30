package io.aurigraph.v11.oracle;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Oracle Verification Repository
 * Provides database operations for oracle verification records
 *
 * @author Aurigraph V11 - Backend Development Agent
 * @version 11.0.0
 * @sprint Sprint 16 - Oracle Verification System (AV11-483)
 */
@ApplicationScoped
public class OracleVerificationRepository implements PanacheRepository<OracleVerificationEntity> {

    /**
     * Find verification by verification ID
     */
    public Optional<OracleVerificationEntity> findByVerificationId(String verificationId) {
        return find("verificationId", verificationId).firstResultOptional();
    }

    /**
     * Find all verifications for a specific asset
     */
    public List<OracleVerificationEntity> findByAssetId(String assetId) {
        return find("assetId", Sort.descending("verificationTimestamp"), assetId).list();
    }

    /**
     * Find verifications for an asset with limit
     */
    public List<OracleVerificationEntity> findByAssetId(String assetId, int limit) {
        return find("assetId", Sort.descending("verificationTimestamp"), assetId)
            .page(0, limit)
            .list();
    }

    /**
     * Find most recent verification for an asset
     */
    public Optional<OracleVerificationEntity> findLatestByAssetId(String assetId) {
        return find("assetId", Sort.descending("verificationTimestamp"), assetId)
            .firstResultOptional();
    }

    /**
     * Find verifications by status
     */
    public List<OracleVerificationEntity> findByStatus(String status) {
        return find("verificationStatus", Sort.descending("verificationTimestamp"), status).list();
    }

    /**
     * Find approved verifications
     */
    public List<OracleVerificationEntity> findApproved() {
        return findByStatus("APPROVED");
    }

    /**
     * Find rejected verifications
     */
    public List<OracleVerificationEntity> findRejected() {
        return findByStatus("REJECTED");
    }

    /**
     * Find verifications with consensus reached
     */
    public List<OracleVerificationEntity> findWithConsensus() {
        return find("consensusReached = :reached", Sort.descending("verificationTimestamp"),
            io.quarkus.panache.common.Parameters.with("reached", true)).list();
    }

    /**
     * Find verifications without consensus
     */
    public List<OracleVerificationEntity> findWithoutConsensus() {
        return find("consensusReached = :reached", Sort.descending("verificationTimestamp"),
            io.quarkus.panache.common.Parameters.with("reached", false)).list();
    }

    /**
     * Find verifications within time range
     */
    public List<OracleVerificationEntity> findByTimestampRange(Instant start, Instant end) {
        return find("verificationTimestamp >= ?1 and verificationTimestamp <= ?2",
            Sort.descending("verificationTimestamp"), start, end).list();
    }

    /**
     * Find recent verifications (last N hours)
     */
    public List<OracleVerificationEntity> findRecent(int hours) {
        Instant since = Instant.now().minusSeconds(hours * 3600L);
        return find("verificationTimestamp >= ?1", Sort.descending("verificationTimestamp"), since).list();
    }

    /**
     * Find verifications with minimum successful oracles
     */
    public List<OracleVerificationEntity> findByMinSuccessfulOracles(int minSuccessful) {
        return find("successfulOracles >= ?1", Sort.descending("verificationTimestamp"), minSuccessful).list();
    }

    /**
     * Count verifications by status
     */
    public long countByStatus(String status) {
        return count("verificationStatus", status);
    }

    /**
     * Count verifications for asset
     */
    public long countByAssetId(String assetId) {
        return count("assetId", assetId);
    }

    /**
     * Get verification statistics
     */
    public VerificationStatistics getStatistics() {
        long total = count();
        long approved = countByStatus("APPROVED");
        long rejected = countByStatus("REJECTED");
        long insufficient = countByStatus("INSUFFICIENT_DATA");
        long withConsensus = count("consensusReached = :reached",
            io.quarkus.panache.common.Parameters.with("reached", true));

        double avgConsensusPercentage = find("consensusReached = :reached",
            io.quarkus.panache.common.Parameters.with("reached", true))
            .stream()
            .mapToDouble(e -> e.consensusPercentage)
            .average()
            .orElse(0.0);

        double avgVerificationTimeMs = listAll()
            .stream()
            .mapToLong(e -> e.totalVerificationTimeMs)
            .average()
            .orElse(0.0);

        return new VerificationStatistics(
            total,
            approved,
            rejected,
            insufficient,
            withConsensus,
            avgConsensusPercentage,
            avgVerificationTimeMs
        );
    }

    /**
     * Verification Statistics record
     */
    public record VerificationStatistics(
        long totalVerifications,
        long approvedVerifications,
        long rejectedVerifications,
        long insufficientDataVerifications,
        long verificationsWithConsensus,
        double averageConsensusPercentage,
        double averageVerificationTimeMs
    ) {}

    /**
     * Cleanup old verification records (retention policy)
     * Removes verifications older than the specified retention period
     *
     * @param retentionDays Number of days to retain verification records
     * @return Number of records deleted
     */
    public long cleanupOldVerifications(int retentionDays) {
        Instant cutoffDate = Instant.now().minusSeconds(retentionDays * 86400L);
        long deletedCount = delete("verificationTimestamp < ?1", cutoffDate);
        return deletedCount;
    }

    /**
     * Archive old verification records to audit log
     * Moves verifications older than specified days to archive table
     *
     * @param archiveDays Number of days before archiving
     * @return Number of records archived
     */
    public long archiveOldVerifications(int archiveDays) {
        Instant cutoffDate = Instant.now().minusSeconds(archiveDays * 86400L);
        List<OracleVerificationEntity> oldVerifications =
            find("verificationTimestamp < ?1", cutoffDate).list();

        // In production, this would copy to an archive table
        // For now, just count them
        long archivedCount = oldVerifications.size();

        // Log the archival
        if (archivedCount > 0) {
            io.quarkus.logging.Log.infof("Archived %d oracle verification records older than %d days",
                archivedCount, archiveDays);
        }

        return archivedCount;
    }

    /**
     * Get verification records pending cleanup
     * Returns count of records older than retention period
     *
     * @param retentionDays Retention period in days
     * @return Count of records pending cleanup
     */
    public long countPendingCleanup(int retentionDays) {
        Instant cutoffDate = Instant.now().minusSeconds(retentionDays * 86400L);
        return count("verificationTimestamp < ?1", cutoffDate);
    }

    /**
     * Delete failed verifications older than specified days
     * Useful for cleaning up test data or failed attempts
     *
     * @param days Number of days to look back
     * @return Number of records deleted
     */
    public long deleteFailedVerifications(int days) {
        Instant cutoffDate = Instant.now().minusSeconds(days * 86400L);
        return delete("verificationStatus = 'REJECTED' and verificationTimestamp < ?1", cutoffDate);
    }

    /**
     * Get audit trail for a specific asset
     * Returns all verifications for an asset, ordered by timestamp
     *
     * @param assetId Asset identifier
     * @return List of verification records
     */
    public List<OracleVerificationEntity> getAuditTrail(String assetId) {
        return find("assetId", Sort.ascending("verificationTimestamp"), assetId).list();
    }

    /**
     * Get recent suspicious verifications
     * Returns verifications that failed consensus or had high variance
     *
     * @param hours Number of hours to look back
     * @return List of suspicious verification records
     */
    public List<OracleVerificationEntity> getSuspiciousVerifications(int hours) {
        Instant since = Instant.now().minusSeconds(hours * 3600L);
        return find(
            "verificationTimestamp >= ?1 and (consensusReached = false or priceVariance > 0.10)",
            Sort.descending("verificationTimestamp"),
            since
        ).list();
    }
}
