package io.aurigraph.v11.assettracking;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.Map;

/**
 * Audit Trail Entry Data Transfer Object
 *
 * Represents a single audit log entry for an asset trace.
 * Records all actions performed on an asset including creation, transfers, verification, etc.
 *
 * Action Types:
 * - CREATED: Asset trace was created
 * - TRANSFERRED: Ownership was transferred
 * - VERIFIED: Asset was verified for compliance
 * - VALUATED: Asset valuation was updated
 * - TOKENIZED: Asset was tokenized
 * - AUDITED: Asset was audited
 * - COMPLIANCE_CHECK: Compliance check was performed
 * - ERROR: An error occurred during processing
 *
 * @version 1.0.0
 * @author Aurigraph V11 Development Team
 */
public class AuditTrailEntry {

    @JsonProperty("entryId")
    private String entryId;

    @JsonProperty("action")
    private String action;

    @JsonProperty("actor")
    private String actor;

    @JsonProperty("timestamp")
    private Instant timestamp;

    @JsonProperty("details")
    private Map<String, Object> details;

    @JsonProperty("status")
    private String status;

    // Constructors
    public AuditTrailEntry() {
    }

    public AuditTrailEntry(String entryId, String action, String actor, Instant timestamp, String status) {
        this.entryId = entryId;
        this.action = action;
        this.actor = actor;
        this.timestamp = timestamp;
        this.status = status;
    }

    // Getters and Setters
    public String getEntryId() {
        return entryId;
    }

    public void setEntryId(String entryId) {
        this.entryId = entryId;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getActor() {
        return actor;
    }

    public void setActor(String actor) {
        this.actor = actor;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public Map<String, Object> getDetails() {
        return details;
    }

    public void setDetails(Map<String, Object> details) {
        this.details = details;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * Check if this audit entry represents a successful operation
     *
     * @return true if status is SUCCESS
     */
    public boolean isSuccessful() {
        return "SUCCESS".equalsIgnoreCase(status);
    }

    /**
     * Check if this audit entry represents a failed operation
     *
     * @return true if status is FAILED or ERROR
     */
    public boolean isFailed() {
        return "FAILED".equalsIgnoreCase(status) || "ERROR".equalsIgnoreCase(status);
    }

    /**
     * Get action description for user-friendly display
     *
     * @return Human-readable action description
     */
    public String getActionDescription() {
        return switch (action) {
            case "CREATED" -> "Asset trace created";
            case "TRANSFERRED" -> "Ownership transferred";
            case "VERIFIED" -> "Asset verified for compliance";
            case "VALUATED" -> "Asset valuation updated";
            case "TOKENIZED" -> "Asset tokenized";
            case "AUDITED" -> "Asset audited";
            case "COMPLIANCE_CHECK" -> "Compliance check performed";
            case "ERROR" -> "Operation failed";
            default -> action;
        };
    }

    @Override
    public String toString() {
        return "AuditTrailEntry{" +
                "entryId='" + entryId + '\'' +
                ", action='" + action + '\'' +
                ", actor='" + actor + '\'' +
                ", timestamp=" + timestamp +
                ", status='" + status + '\'' +
                ", details.size=" + (details != null ? details.size() : 0) +
                '}';
    }
}
