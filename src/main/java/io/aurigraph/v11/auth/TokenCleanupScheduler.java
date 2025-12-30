package io.aurigraph.v11.auth;

import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

/**
 * TokenCleanupScheduler
 *
 * Scheduled job to cleanup expired JWT tokens from the database.
 * Runs daily at 2 AM to prevent database bloat.
 *
 * Schedule: 0 0 2 * * ? (Daily at 2:00 AM)
 */
@ApplicationScoped
public class TokenCleanupScheduler {

    private static final Logger LOG = Logger.getLogger(TokenCleanupScheduler.class);

    @Inject
    AuthTokenService authTokenService;

    /**
     * Cleanup expired tokens
     * Runs daily at 2 AM
     */
    @Scheduled(cron = "0 0 2 * * ?", identity = "token-cleanup")
    void cleanupExpiredTokens() {
        LOG.infof("🧹 Starting scheduled token cleanup job");
        try {
            int deletedCount = authTokenService.cleanupExpiredTokens();
            LOG.infof("✅ Cleanup completed: Deleted %d expired tokens", deletedCount);
        } catch (Exception e) {
            LOG.errorf(e, "❌ Token cleanup failed");
        }
    }

    /**
     * Cleanup old tokens (older than 30 days)
     * Runs weekly on Sunday at 3 AM
     */
    @Scheduled(cron = "0 0 3 ? * SUN", identity = "token-cleanup-old")
    void cleanupOldTokens() {
        LOG.infof("🧹 Starting scheduled old token cleanup job");
        try {
            int deletedCount = authTokenService.cleanupOldExpiredTokens(30);
            LOG.infof("✅ Old token cleanup completed: Deleted %d tokens older than 30 days", deletedCount);
        } catch (Exception e) {
            LOG.errorf(e, "❌ Old token cleanup failed");
        }
    }

    /**
     * Cleanup unused tokens (not used for 60 days)
     * Runs weekly on Saturday at 4 AM
     */
    @Scheduled(cron = "0 0 4 ? * SAT", identity = "token-cleanup-unused")
    void cleanupUnusedTokens() {
        LOG.infof("🧹 Starting scheduled unused token cleanup job");
        try {
            // Future enhancement: Add method to cleanup unused tokens
            // For now, this serves as a template
            LOG.infof("✅ Unused token cleanup scheduled (implementation pending)");
        } catch (Exception e) {
            LOG.errorf(e, "❌ Unused token cleanup failed");
        }
    }
}
