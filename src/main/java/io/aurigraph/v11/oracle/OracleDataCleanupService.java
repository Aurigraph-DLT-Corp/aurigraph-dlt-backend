package io.aurigraph.v11.oracle;

import io.quarkus.logging.Log;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Oracle Data Cleanup Service
 * Automatically cleans up old oracle verification records
 *
 * Features:
 * - Daily cleanup of expired verification records
 * - Configurable retention period
 * - Automatic archival before deletion
 * - Performance metrics tracking
 *
 * @author Aurigraph V11 - Development Agent 4
 * @version 11.0.0
 * @sprint Sprint 16 - Oracle Verification System (AV11-483)
 */
@ApplicationScoped
public class OracleDataCleanupService {

    @Inject
    OracleVerificationRepository verificationRepository;

    @ConfigProperty(name = "oracle.verification.retention.days", defaultValue = "90")
    int retentionDays;

    @ConfigProperty(name = "oracle.verification.archive.days", defaultValue = "30")
    int archiveDays;

    @ConfigProperty(name = "oracle.verification.cleanup.enabled", defaultValue = "true")
    boolean cleanupEnabled;

    /**
     * Scheduled cleanup - runs daily at 2 AM
     */
    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional
    public void performDailyCleanup() {
        if (!cleanupEnabled) {
            Log.debug("Oracle verification cleanup is disabled");
            return;
        }

        Log.info("Starting oracle verification data cleanup");
        long startTime = System.currentTimeMillis();

        try {
            // Step 1: Archive old verifications (30+ days)
            long archivedCount = verificationRepository.archiveOldVerifications(archiveDays);
            Log.infof("Archived %d verification records older than %d days", archivedCount, archiveDays);

            // Step 2: Cleanup very old verifications (90+ days)
            long deletedCount = verificationRepository.cleanupOldVerifications(retentionDays);
            Log.infof("Deleted %d verification records older than %d days", deletedCount, retentionDays);

            // Step 3: Cleanup failed verifications older than 7 days
            long failedDeletedCount = verificationRepository.deleteFailedVerifications(7);
            Log.infof("Deleted %d failed verification records older than 7 days", failedDeletedCount);

            // Step 4: Report statistics
            long pendingCleanup = verificationRepository.countPendingCleanup(retentionDays);
            long totalTime = System.currentTimeMillis() - startTime;

            Log.infof("Oracle verification cleanup completed in %dms. " +
                "Archived: %d, Deleted: %d, Failed deleted: %d, Pending cleanup: %d",
                totalTime, archivedCount, deletedCount, failedDeletedCount, pendingCleanup);

        } catch (Exception e) {
            Log.errorf("Error during oracle verification cleanup: %s", e.getMessage(), e);
        }
    }

    /**
     * Manual cleanup trigger (for testing or on-demand cleanup)
     *
     * @return Cleanup summary
     */
    @Transactional
    public CleanupSummary performManualCleanup() {
        Log.info("Starting manual oracle verification cleanup");
        long startTime = System.currentTimeMillis();

        CleanupSummary summary = new CleanupSummary();

        try {
            summary.archivedCount = verificationRepository.archiveOldVerifications(archiveDays);
            summary.deletedCount = verificationRepository.cleanupOldVerifications(retentionDays);
            summary.failedDeletedCount = verificationRepository.deleteFailedVerifications(7);
            summary.pendingCleanup = verificationRepository.countPendingCleanup(retentionDays);
            summary.cleanupTimeMs = System.currentTimeMillis() - startTime;
            summary.success = true;

            Log.infof("Manual cleanup completed: %s", summary);

        } catch (Exception e) {
            summary.success = false;
            summary.errorMessage = e.getMessage();
            Log.errorf("Error during manual cleanup: %s", e.getMessage(), e);
        }

        return summary;
    }

    /**
     * Get cleanup statistics without performing cleanup
     *
     * @return Current cleanup statistics
     */
    public CleanupStatistics getCleanupStatistics() {
        CleanupStatistics stats = new CleanupStatistics();
        stats.retentionDays = retentionDays;
        stats.archiveDays = archiveDays;
        stats.cleanupEnabled = cleanupEnabled;
        stats.pendingCleanup = verificationRepository.countPendingCleanup(retentionDays);
        stats.pendingArchive = verificationRepository.countPendingCleanup(archiveDays);
        stats.totalVerifications = verificationRepository.count();
        return stats;
    }

    /**
     * Cleanup summary record
     */
    public static class CleanupSummary {
        public long archivedCount;
        public long deletedCount;
        public long failedDeletedCount;
        public long pendingCleanup;
        public long cleanupTimeMs;
        public boolean success;
        public String errorMessage;

        @Override
        public String toString() {
            return String.format(
                "CleanupSummary{archived=%d, deleted=%d, failedDeleted=%d, pending=%d, time=%dms, success=%s}",
                archivedCount, deletedCount, failedDeletedCount, pendingCleanup, cleanupTimeMs, success
            );
        }
    }

    /**
     * Cleanup statistics record
     */
    public static class CleanupStatistics {
        public int retentionDays;
        public int archiveDays;
        public boolean cleanupEnabled;
        public long pendingCleanup;
        public long pendingArchive;
        public long totalVerifications;
    }
}
