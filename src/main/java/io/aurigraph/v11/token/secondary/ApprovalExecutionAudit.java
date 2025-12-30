package io.aurigraph.v11.token.secondary;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * ApprovalExecutionAudit
 * Immutable audit trail for approval execution lifecycle.
 * Records all state transitions and execution phases.
 *
 * Execution Phases:
 * - INITIATED: Execution started
 * - VALIDATED: Preconditions checked
 * - TRANSITIONED: State changed in database
 * - COMPLETED: Events fired and metadata updated
 * - FAILED: Exception occurred during execution
 * - ROLLED_BACK: Compensating transaction executed
 */
@Entity
@Table(name = "approval_execution_audit", indexes = {
    @Index(name = "idx_aea_version_id", columnList = "version_id"),
    @Index(name = "idx_aea_approval_request_id", columnList = "approval_request_id"),
    @Index(name = "idx_aea_execution_timestamp", columnList = "execution_timestamp"),
    @Index(name = "idx_aea_execution_phase", columnList = "execution_phase"),
    @Index(name = "idx_aea_version_timestamp", columnList = "version_id, execution_timestamp DESC"),
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalExecutionAudit extends PanacheEntity {

    // Panache provides inherited 'id' field; this UUID column is for business-level identification
    @Column(columnDefinition = "uuid")
    public UUID auditId;

    /**
     * Reference to the version being transitioned
     */
    @Column(name = "version_id", nullable = false, columnDefinition = "uuid")
    public UUID versionId;

    /**
     * Reference to the VVB approval request (if applicable)
     */
    @Column(name = "approval_request_id", columnDefinition = "uuid")
    public UUID approvalRequestId;

    /**
     * Execution phase: INITIATED, VALIDATED, TRANSITIONED, COMPLETED, FAILED, ROLLED_BACK
     */
    @Column(name = "execution_phase", nullable = false, length = 50)
    public String executionPhase;

    /**
     * Previous status before this phase
     */
    @Column(name = "previous_status", length = 50)
    public String previousStatus;

    /**
     * New status after this phase
     */
    @Column(name = "new_status", length = 50)
    public String newStatus;

    /**
     * Who executed this phase (SYSTEM, user ID, etc)
     */
    @Column(name = "executed_by", length = 100)
    public String executedBy;

    /**
     * When this phase was executed
     */
    @Column(name = "execution_timestamp", nullable = false)
    public Instant executionTimestamp;

    /**
     * Error message if phase failed
     */
    @Column(name = "error_message", columnDefinition = "text")
    public String errorMessage;

    /**
     * Additional metadata in JSON format
     * Examples:
     * - {"duration_ms": 145, "validator_count": 8}
     * - {"error": "Database constraint violation", "retry_count": 2}
     * - {"approval_threshold": 66.67, "consensus_reached": true}
     */
    @Column(name = "metadata", columnDefinition = "jsonb")
    public JsonNode metadata;

    /**
     * Query by version ID
     */
    public static java.util.List<ApprovalExecutionAudit> findByVersionId(UUID versionId) {
        return find("version_id = ?1 order by execution_timestamp asc", versionId).list();
    }

    /**
     * Query by approval request ID
     */
    public static java.util.List<ApprovalExecutionAudit> findByApprovalRequestId(UUID approvalRequestId) {
        return find("approval_request_id = ?1 order by execution_timestamp asc", approvalRequestId).list();
    }

    /**
     * Query failed executions
     */
    public static java.util.List<ApprovalExecutionAudit> findFailures() {
        return find("execution_phase in ('FAILED', 'ROLLED_BACK') order by execution_timestamp desc").list();
    }

    @Override
    public String toString() {
        return "ApprovalExecutionAudit{" +
                "id=" + id +
                ", versionId=" + versionId +
                ", executionPhase='" + executionPhase + '\'' +
                ", previousStatus='" + previousStatus + '\'' +
                ", newStatus='" + newStatus + '\'' +
                ", executedBy='" + executedBy + '\'' +
                ", executionTimestamp=" + executionTimestamp +
                '}';
    }
}
