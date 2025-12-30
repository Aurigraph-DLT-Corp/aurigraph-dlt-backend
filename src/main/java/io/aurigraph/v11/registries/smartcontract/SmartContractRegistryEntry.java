package io.aurigraph.v11.registries.smartcontract;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.*;

/**
 * Smart Contract Registry Entry Data Transfer Object
 *
 * Represents a smart contract entry in the registry with complete metadata,
 * deployment information, and asset linkage. Each entry maintains a comprehensive
 * audit trail and supports asset tracking.
 *
 * @version 11.5.0
 * @since 2025-11-14
 */
public class SmartContractRegistryEntry {

    @JsonProperty("contractId")
    private String contractId; // Unique contract identifier

    @JsonProperty("contractName")
    private String contractName; // Human-readable contract name

    @JsonProperty("description")
    private String description; // Contract description/purpose

    @JsonProperty("deploymentAddress")
    private String deploymentAddress; // Blockchain deployment address

    @JsonProperty("deploymentTxHash")
    private String deploymentTxHash; // Deployment transaction hash

    @JsonProperty("codeHash")
    private String codeHash; // SHA-256 hash of contract code

    @JsonProperty("currentStatus")
    private ContractStatusEnum currentStatus; // Current lifecycle status

    @JsonProperty("registeredAt")
    private Instant registeredAt; // Registration timestamp

    @JsonProperty("linkedAssetCount")
    private int linkedAssetCount; // Number of assets linked to this contract

    @JsonProperty("linkedAssets")
    private Set<String> linkedAssets; // IDs of linked assets

    @JsonProperty("deploymentInfo")
    private ContractDeploymentInfo deploymentInfo; // Full deployment details

    @JsonProperty("metadata")
    private Map<String, String> metadata; // Additional metadata key-value pairs

    @JsonProperty("owner")
    private String owner; // Contract owner address

    @JsonProperty("version")
    private String version; // Contract version

    @JsonProperty("language")
    private String language; // Programming language (SOLIDITY, JAVA, etc)

    @JsonProperty("auditStatus")
    private String auditStatus; // Audit status (PENDING, PASSED, FAILED)

    @JsonProperty("lastModified")
    private Instant lastModified; // Last modification timestamp

    @JsonProperty("verificationHash")
    private String verificationHash; // Hash for integrity verification

    @JsonProperty("tags")
    private Set<String> tags; // Classification tags

    // Constructors

    /**
     * Default constructor
     */
    public SmartContractRegistryEntry() {
        this.registeredAt = Instant.now();
        this.lastModified = Instant.now();
        this.linkedAssets = new HashSet<>();
        this.metadata = new HashMap<>();
        this.tags = new HashSet<>();
        this.linkedAssetCount = 0;
        this.currentStatus = ContractStatusEnum.DRAFT;
    }

    /**
     * Full constructor with essential fields
     */
    public SmartContractRegistryEntry(
            String contractId,
            String contractName,
            String description,
            String deploymentAddress,
            String deploymentTxHash,
            String codeHash,
            String owner) {
        this();
        this.contractId = contractId;
        this.contractName = contractName;
        this.description = description;
        this.deploymentAddress = deploymentAddress;
        this.deploymentTxHash = deploymentTxHash;
        this.codeHash = codeHash;
        this.owner = owner;
    }

    // Getters and Setters

    public String getContractId() {
        return contractId;
    }

    public void setContractId(String contractId) {
        this.contractId = contractId;
    }

    public String getContractName() {
        return contractName;
    }

    public void setContractName(String contractName) {
        this.contractName = contractName;
        this.lastModified = Instant.now();
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
        this.lastModified = Instant.now();
    }

    public String getDeploymentAddress() {
        return deploymentAddress;
    }

    public void setDeploymentAddress(String deploymentAddress) {
        this.deploymentAddress = deploymentAddress;
    }

    public String getDeploymentTxHash() {
        return deploymentTxHash;
    }

    public void setDeploymentTxHash(String deploymentTxHash) {
        this.deploymentTxHash = deploymentTxHash;
    }

    public String getCodeHash() {
        return codeHash;
    }

    public void setCodeHash(String codeHash) {
        this.codeHash = codeHash;
        this.lastModified = Instant.now();
    }

    public ContractStatusEnum getCurrentStatus() {
        return currentStatus;
    }

    public void setCurrentStatus(ContractStatusEnum currentStatus) {
        this.currentStatus = currentStatus;
        this.lastModified = Instant.now();
    }

    public Instant getRegisteredAt() {
        return registeredAt;
    }

    public void setRegisteredAt(Instant registeredAt) {
        this.registeredAt = registeredAt;
    }

    public int getLinkedAssetCount() {
        return linkedAssetCount;
    }

    public void setLinkedAssetCount(int linkedAssetCount) {
        this.linkedAssetCount = linkedAssetCount;
    }

    public Set<String> getLinkedAssets() {
        return linkedAssets != null ? linkedAssets : new HashSet<>();
    }

    public void setLinkedAssets(Set<String> linkedAssets) {
        this.linkedAssets = linkedAssets != null ? linkedAssets : new HashSet<>();
        this.linkedAssetCount = this.linkedAssets.size();
    }

    public ContractDeploymentInfo getDeploymentInfo() {
        return deploymentInfo;
    }

    public void setDeploymentInfo(ContractDeploymentInfo deploymentInfo) {
        this.deploymentInfo = deploymentInfo;
        this.lastModified = Instant.now();
    }

    public Map<String, String> getMetadata() {
        return metadata != null ? metadata : new HashMap<>();
    }

    public void setMetadata(Map<String, String> metadata) {
        this.metadata = metadata != null ? metadata : new HashMap<>();
        this.lastModified = Instant.now();
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
        this.lastModified = Instant.now();
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
        this.lastModified = Instant.now();
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getAuditStatus() {
        return auditStatus;
    }

    public void setAuditStatus(String auditStatus) {
        this.auditStatus = auditStatus;
        this.lastModified = Instant.now();
    }

    public Instant getLastModified() {
        return lastModified;
    }

    public void setLastModified(Instant lastModified) {
        this.lastModified = lastModified;
    }

    public String getVerificationHash() {
        return verificationHash;
    }

    public void setVerificationHash(String verificationHash) {
        this.verificationHash = verificationHash;
    }

    public Set<String> getTags() {
        return tags != null ? tags : new HashSet<>();
    }

    public void setTags(Set<String> tags) {
        this.tags = tags != null ? tags : new HashSet<>();
    }

    // Business Logic Methods

    /**
     * Link an asset to this contract
     *
     * @param assetId ID of asset to link
     * @return true if asset was added, false if already linked
     */
    public boolean linkAsset(String assetId) {
        if (assetId == null || assetId.isBlank()) {
            throw new IllegalArgumentException("Asset ID cannot be null or empty");
        }
        boolean added = linkedAssets.add(assetId);
        if (added) {
            linkedAssetCount++;
            this.lastModified = Instant.now();
        }
        return added;
    }

    /**
     * Unlink an asset from this contract
     *
     * @param assetId ID of asset to unlink
     * @return true if asset was removed, false if not found
     */
    public boolean unlinkAsset(String assetId) {
        boolean removed = linkedAssets.remove(assetId);
        if (removed) {
            linkedAssetCount--;
            this.lastModified = Instant.now();
        }
        return removed;
    }

    /**
     * Check if asset is linked to this contract
     *
     * @param assetId Asset ID to check
     * @return true if asset is linked
     */
    public boolean isAssetLinked(String assetId) {
        return linkedAssets.contains(assetId);
    }

    /**
     * Add metadata key-value pair
     *
     * @param key Metadata key
     * @param value Metadata value
     */
    public void addMetadata(String key, String value) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("Metadata key cannot be null or empty");
        }
        metadata.put(key, value);
        this.lastModified = Instant.now();
    }

    /**
     * Remove metadata entry
     *
     * @param key Metadata key to remove
     * @return Previous value or null
     */
    public String removeMetadata(String key) {
        String removed = metadata.remove(key);
        if (removed != null) {
            this.lastModified = Instant.now();
        }
        return removed;
    }

    /**
     * Add tag to contract
     *
     * @param tag Tag to add
     */
    public void addTag(String tag) {
        if (tag == null || tag.isBlank()) {
            throw new IllegalArgumentException("Tag cannot be null or empty");
        }
        tags.add(tag.toLowerCase());
        this.lastModified = Instant.now();
    }

    /**
     * Check if contract is deployed and active
     *
     * @return true if contract can execute transactions
     */
    public boolean isActive() {
        return currentStatus != null && currentStatus.isActive();
    }

    /**
     * Check if contract has reached terminal state
     *
     * @return true if contract cannot transition further
     */
    public boolean isTerminal() {
        return currentStatus != null && currentStatus.isTerminal();
    }

    /**
     * Update contract status with validation
     *
     * @param newStatus New status to transition to
     * @throws IllegalStateException if transition is invalid
     */
    public void updateStatus(ContractStatusEnum newStatus) {
        if (newStatus == null) {
            throw new IllegalArgumentException("Status cannot be null");
        }

        if (!ContractStatusEnum.isValidTransition(currentStatus, newStatus)) {
            throw new IllegalStateException(
                String.format("Invalid status transition from %s to %s", currentStatus, newStatus)
            );
        }

        this.currentStatus = newStatus;
        this.lastModified = Instant.now();
    }

    @Override
    public String toString() {
        return String.format(
            "SmartContractEntry{id='%s', name='%s', status=%s, assets=%d, owner='%s', deployed='%s'}",
            contractId, contractName, currentStatus, linkedAssetCount, owner, deploymentAddress
        );
    }
}
