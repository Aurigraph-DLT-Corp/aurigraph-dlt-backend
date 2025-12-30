package io.aurigraph.v11.registries;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Registry Search Result DTO
 *
 * Represents a single search result from the unified registry search across
 * all registry types (Smart Contract, Token, RWA, Merkle Tree, Compliance).
 *
 * @version 11.5.0
 * @since 2025-11-14
 */
public class RegistrySearchResult {

    @JsonProperty("entryId")
    private String entryId;

    @JsonProperty("entryType")
    private String entryType; // Specific type (e.g., "ActiveContract", "TokenRegistry", "RWATRegistry")

    @JsonProperty("name")
    private String name;

    @JsonProperty("description")
    private String description;

    @JsonProperty("registryType")
    private String registryType; // SMART_CONTRACT, TOKEN, RWA, MERKLE_TREE, COMPLIANCE

    @JsonProperty("registryStatus")
    private String registryStatus; // ACTIVE, VERIFIED, PENDING, SUSPENDED, etc.

    @JsonProperty("verificationStatus")
    private String verificationStatus; // VERIFIED, PENDING, IN_REVIEW, REJECTED, etc.

    @JsonProperty("lastUpdated")
    private Instant lastUpdated;

    @JsonProperty("metadata")
    private Map<String, Object> metadata = new HashMap<>();

    // Constructors

    public RegistrySearchResult() {
    }

    public RegistrySearchResult(String entryId, String entryType, String name, String registryType) {
        this.entryId = entryId;
        this.entryType = entryType;
        this.name = name;
        this.registryType = registryType;
        this.lastUpdated = Instant.now();
    }

    // Getters and Setters

    public String getEntryId() {
        return entryId;
    }

    public void setEntryId(String entryId) {
        this.entryId = entryId;
    }

    public String getEntryType() {
        return entryType;
    }

    public void setEntryType(String entryType) {
        this.entryType = entryType;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getRegistryType() {
        return registryType;
    }

    public void setRegistryType(String registryType) {
        this.registryType = registryType;
    }

    public String getRegistryStatus() {
        return registryStatus;
    }

    public void setRegistryStatus(String registryStatus) {
        this.registryStatus = registryStatus;
    }

    public String getVerificationStatus() {
        return verificationStatus;
    }

    public void setVerificationStatus(String verificationStatus) {
        this.verificationStatus = verificationStatus;
    }

    public Instant getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(Instant lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    /**
     * Add a metadata entry
     */
    public void addMetadata(String key, Object value) {
        if (this.metadata == null) {
            this.metadata = new HashMap<>();
        }
        this.metadata.put(key, value);
    }

    @Override
    public String toString() {
        return "RegistrySearchResult{" +
                "entryId='" + entryId + '\'' +
                ", name='" + name + '\'' +
                ", registryType='" + registryType + '\'' +
                ", verificationStatus='" + verificationStatus + '\'' +
                '}';
    }
}
