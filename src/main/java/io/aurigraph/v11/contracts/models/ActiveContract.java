package io.aurigraph.v11.contracts.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Active Contract Model for Aurigraph V11 - LevelDB Compatible
 *
 * Represents contracts that are currently active in the system.
 * Tracks lifecycle events, parties involved, and execution status.
 *
 * LevelDB Storage: Uses contractId as primary key
 * JSON Serializable: All fields stored as JSON in LevelDB
 *
 * @version 4.0.0 (LevelDB Migration - Oct 8, 2025)
 * @author Aurigraph V11 Development Team
 */
public class ActiveContract {

    @JsonProperty("contractId")
    private String contractId;

    @JsonProperty("name")
    private String name;

    @JsonProperty("status")
    private ActiveContractStatus status;

    @JsonProperty("creatorAddress")
    private String creatorAddress;

    @JsonProperty("contractType")
    private String contractType;

    // Timestamps
    @JsonProperty("createdAt")
    private Instant createdAt;

    @JsonProperty("activatedAt")
    private Instant activatedAt;

    @JsonProperty("completedAt")
    private Instant completedAt;

    @JsonProperty("terminatedAt")
    private Instant terminatedAt;

    @JsonProperty("expiresAt")
    private Instant expiresAt;

    @JsonProperty("lastEventAt")
    private Instant lastEventAt;

    @JsonProperty("updatedAt")
    private Instant updatedAt;

    // Parties
    @JsonProperty("parties")
    private List<String> parties = new ArrayList<>();

    // Execution details
    @JsonProperty("executionCount")
    private Long executionCount = 0L;

    @JsonProperty("eventCount")
    private Long eventCount = 0L;

    @JsonProperty("lastExecutionAt")
    private Instant lastExecutionAt;

    @JsonProperty("lastExecutionStatus")
    private String lastExecutionStatus;

    // Metadata
    @JsonProperty("description")
    private String description;

    @JsonProperty("metadata")
    private String metadata;

    @JsonProperty("tags")
    private String tags;

    // Notifications
    @JsonProperty("notificationEnabled")
    private Boolean notificationEnabled = true;

    @JsonProperty("notificationRecipients")
    private String notificationRecipients;

    // ==================== CONSTRUCTORS ====================

    public ActiveContract() {
        this.createdAt = Instant.now();
        this.status = ActiveContractStatus.PENDING;
        this.executionCount = 0L;
        this.eventCount = 0L;
        this.notificationEnabled = true;
    }

    public ActiveContract(String contractId, String name, String creatorAddress) {
        this();
        this.contractId = contractId;
        this.name = name;
        this.creatorAddress = creatorAddress;
    }

    // ==================== LIFECYCLE METHODS ====================

    /**
     * Ensure createdAt is set (call before first persist)
     */
    public void ensureCreatedAt() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    /**
     * Update timestamp (call before each persist)
     */
    public void updateTimestamp() {
        updatedAt = Instant.now();
    }

    /**
     * Activate the contract
     */
    public void activate() {
        if (status != ActiveContractStatus.PENDING) {
            throw new IllegalStateException("Only PENDING contracts can be activated");
        }
        this.status = ActiveContractStatus.ACTIVE;
        this.activatedAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    /**
     * Complete the contract
     */
    public void complete() {
        if (status != ActiveContractStatus.ACTIVE) {
            throw new IllegalStateException("Only ACTIVE contracts can be completed");
        }
        this.status = ActiveContractStatus.COMPLETED;
        this.completedAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    /**
     * Terminate the contract
     */
    public void terminate(String reason) {
        if (status == ActiveContractStatus.COMPLETED || status == ActiveContractStatus.TERMINATED) {
            throw new IllegalStateException("Contract is already finalized");
        }
        this.status = ActiveContractStatus.TERMINATED;
        this.terminatedAt = Instant.now();
        this.updatedAt = Instant.now();
        this.metadata = (this.metadata != null ? this.metadata + "\n" : "") + "Termination reason: " + reason;
    }

    /**
     * Pause the contract
     */
    public void pause() {
        if (status != ActiveContractStatus.ACTIVE) {
            throw new IllegalStateException("Only ACTIVE contracts can be paused");
        }
        this.status = ActiveContractStatus.PAUSED;
        this.updatedAt = Instant.now();
    }

    /**
     * Resume the contract
     */
    public void resume() {
        if (status != ActiveContractStatus.PAUSED) {
            throw new IllegalStateException("Only PAUSED contracts can be resumed");
        }
        this.status = ActiveContractStatus.ACTIVE;
        this.updatedAt = Instant.now();
    }

    /**
     * Record an execution
     */
    public void recordExecution(String executionStatus) {
        this.executionCount++;
        this.lastExecutionAt = Instant.now();
        this.lastExecutionStatus = executionStatus;
        this.updatedAt = Instant.now();
    }

    /**
     * Record an event
     */
    public void recordEvent() {
        this.eventCount++;
        this.lastEventAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    /**
     * Add a party to the contract
     */
    public void addParty(String partyAddress) {
        if (!this.parties.contains(partyAddress)) {
            this.parties.add(partyAddress);
            this.updatedAt = Instant.now();
        }
    }

    /**
     * Remove a party from the contract
     */
    public void removeParty(String partyAddress) {
        if (this.parties.remove(partyAddress)) {
            this.updatedAt = Instant.now();
        }
    }

    /**
     * Check if contract is expired
     */
    public boolean isExpired() {
        return expiresAt != null && Instant.now().isAfter(expiresAt);
    }

    /**
     * Check if contract is active
     */
    public boolean isActive() {
        return status == ActiveContractStatus.ACTIVE && !isExpired();
    }

    // ==================== GETTERS AND SETTERS ====================

    public String getContractId() { return contractId; }
    public void setContractId(String contractId) { this.contractId = contractId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public ActiveContractStatus getStatus() { return status; }
    public void setStatus(ActiveContractStatus status) { this.status = status; }

    public String getCreatorAddress() { return creatorAddress; }
    public void setCreatorAddress(String creatorAddress) { this.creatorAddress = creatorAddress; }

    public String getContractType() { return contractType; }
    public void setContractType(String contractType) { this.contractType = contractType; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getActivatedAt() { return activatedAt; }
    public void setActivatedAt(Instant activatedAt) { this.activatedAt = activatedAt; }

    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }

    public Instant getTerminatedAt() { return terminatedAt; }
    public void setTerminatedAt(Instant terminatedAt) { this.terminatedAt = terminatedAt; }

    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }

    public Instant getLastEventAt() { return lastEventAt; }
    public void setLastEventAt(Instant lastEventAt) { this.lastEventAt = lastEventAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public List<String> getParties() { return parties; }
    public void setParties(List<String> parties) { this.parties = parties; }

    public Long getExecutionCount() { return executionCount; }
    public void setExecutionCount(Long executionCount) { this.executionCount = executionCount; }

    public Long getEventCount() { return eventCount; }
    public void setEventCount(Long eventCount) { this.eventCount = eventCount; }

    public Instant getLastExecutionAt() { return lastExecutionAt; }
    public void setLastExecutionAt(Instant lastExecutionAt) { this.lastExecutionAt = lastExecutionAt; }

    public String getLastExecutionStatus() { return lastExecutionStatus; }
    public void setLastExecutionStatus(String lastExecutionStatus) { this.lastExecutionStatus = lastExecutionStatus; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getMetadata() { return metadata; }
    public void setMetadata(String metadata) { this.metadata = metadata; }

    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }

    public Boolean getNotificationEnabled() { return notificationEnabled; }
    public void setNotificationEnabled(Boolean notificationEnabled) { this.notificationEnabled = notificationEnabled; }

    public String getNotificationRecipients() { return notificationRecipients; }
    public void setNotificationRecipients(String notificationRecipients) { this.notificationRecipients = notificationRecipients; }

    // ==================== ENUM DEFINITIONS ====================

    public enum ActiveContractStatus {
        PENDING,        // Created but not yet activated
        ACTIVE,         // Currently active and executing
        PAUSED,         // Temporarily paused
        COMPLETED,      // Successfully completed
        TERMINATED,     // Terminated before completion
        EXPIRED         // Expired without completion
    }

    @Override
    public String toString() {
        return String.format("ActiveContract{contractId='%s', name='%s', status=%s, parties=%d}",
                contractId, name, status, parties.size());
    }
}
