package io.aurigraph.v11.token.secondary;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.UUID;

/**
 * ApprovalExecutionAuditRepository
 * Panache repository for approval execution audit trail persistence.
 *
 * Provides database access for:
 * - Recording audit entries for each execution phase
 * - Querying execution history
 * - Filtering by version, approval request, or execution phase
 * - Analysis of failed executions
 */
@ApplicationScoped
public class ApprovalExecutionAuditRepository implements PanacheRepository<ApprovalExecutionAudit> {

    /**
     * Find all audit entries for a token version
     *
     * @param versionId The version ID
     * @return List of audit entries ordered by timestamp (oldest first)
     */
    public List<ApprovalExecutionAudit> findByVersionId(UUID versionId) {
        return list("version_id = ?1 order by execution_timestamp asc", versionId);
    }

    /**
     * Find all audit entries for an approval request
     *
     * @param approvalRequestId The approval request ID
     * @return List of audit entries ordered by timestamp (oldest first)
     */
    public List<ApprovalExecutionAudit> findByApprovalRequestId(UUID approvalRequestId) {
        return list("approval_request_id = ?1 order by execution_timestamp asc", approvalRequestId);
    }

    /**
     * Find failed executions (FAILED or ROLLED_BACK phases)
     *
     * @return List of failed audit entries (most recent first)
     */
    public List<ApprovalExecutionAudit> findFailures() {
        return list("execution_phase in ('FAILED', 'ROLLED_BACK') order by execution_timestamp desc");
    }

    /**
     * Find latest audit entry for a version
     *
     * @param versionId The version ID
     * @return Latest audit entry, or null if not found
     */
    public ApprovalExecutionAudit findLatestByVersionId(UUID versionId) {
        return find("version_id = ?1 order by execution_timestamp desc", versionId)
            .firstResult();
    }

    /**
     * Find audit entries for a specific phase
     *
     * @param versionId The version ID
     * @param phase The execution phase (INITIATED, VALIDATED, TRANSITIONED, COMPLETED, FAILED, ROLLED_BACK)
     * @return List of matching audit entries
     */
    public List<ApprovalExecutionAudit> findByVersionAndPhase(UUID versionId, String phase) {
        return list("version_id = ?1 and execution_phase = ?2 order by execution_timestamp asc",
            versionId, phase);
    }

    /**
     * Count failures for a specific version
     *
     * @param versionId The version ID
     * @return Number of failed executions
     */
    public long countFailuresByVersionId(UUID versionId) {
        return count("version_id = ?1 and execution_phase in ('FAILED', 'ROLLED_BACK')",
            versionId);
    }

    /**
     * Count total execution attempts for a version
     *
     * @param versionId The version ID
     * @return Total number of execution attempts (all phases)
     */
    public long countExecutionAttempts(UUID versionId) {
        return count("version_id = ?1", versionId);
    }

    /**
     * Delete audit entries for a specific version (dangerous - use with caution)
     *
     * @param versionId The version ID
     * @return Number of entries deleted
     */
    public long deleteByVersionId(UUID versionId) {
        return delete("version_id = ?1", versionId);
    }
}
