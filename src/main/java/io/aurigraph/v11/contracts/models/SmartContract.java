package io.aurigraph.v11.contracts.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import io.aurigraph.v11.contracts.models.ContractStatus;

/**
 * Smart Contract Model for Aurigraph V11 - LevelDB Compatible
 *
 * Represents a Ricardian smart contract with RWA (Real-World Asset) support.
 * Supports contract lifecycle management, digital twins, and multi-party agreements.
 *
 * LevelDB Storage: Uses contractId as primary key
 * JSON Serializable: All fields stored as JSON in LevelDB
 *
 * @version 4.0.0 (LevelDB Migration - Oct 8, 2025)
 * @author Aurigraph V11 Development Team
 */
public class SmartContract {

    @JsonProperty("contractId")
    private String contractId;

    @JsonProperty("owner")
    private String owner;

    @JsonProperty("name")
    private String name;

    @JsonProperty("description")
    private String description;

    @JsonProperty("contractType")
    private ContractType contractType;

    @JsonProperty("sourceCode")
    private String sourceCode;

    @JsonProperty("bytecode")
    private String bytecode;

    @JsonProperty("abiDefinition")
    private String abiDefinition;

    @JsonProperty("status")
    private ContractStatus status;

    @JsonProperty("version")
    private Integer version = 1;

    @JsonProperty("templateId")
    private String templateId;

    @JsonProperty("parentContractId")
    private String parentContractId;

    // Financial fields
    @JsonProperty("value")
    private BigDecimal value;

    @JsonProperty("currency")
    private String currency = "AUR";

    // RWA (Real-World Asset) fields
    @JsonProperty("assetId")
    private String assetId;

    @JsonProperty("assetType")
    private AssetType assetType;

    @JsonProperty("isRWA")
    private Boolean isRWA = false;

    @JsonProperty("isDigitalTwin")
    private Boolean isDigitalTwin = false;

    // Verification and security
    @JsonProperty("isVerified")
    private Boolean isVerified = false;

    @JsonProperty("verificationHash")
    private String verificationHash;

    @JsonProperty("securityAuditStatus")
    private String securityAuditStatus;

    // Timestamps
    @JsonProperty("createdAt")
    private Instant createdAt;

    @JsonProperty("updatedAt")
    private Instant updatedAt;

    @JsonProperty("deployedAt")
    private Instant deployedAt;

    @JsonProperty("completedAt")
    private Instant completedAt;

    @JsonProperty("expiresAt")
    private Instant expiresAt;

    // Execution metrics
    @JsonProperty("executionCount")
    private Long executionCount = 0L;

    @JsonProperty("gasUsed")
    private Long gasUsed = 0L;

    @JsonProperty("lastExecutedAt")
    private Instant lastExecutedAt;

    // Multi-party support
    @JsonProperty("parties")
    private List<String> parties = new ArrayList<>();

    @JsonProperty("tags")
    private List<String> tags = new ArrayList<>();

    // Compliance and regulatory
    @JsonProperty("jurisdiction")
    private String jurisdiction;

    @JsonProperty("complianceStatus")
    private String complianceStatus;

    @JsonProperty("kycVerified")
    private Boolean kycVerified = false;

    @JsonProperty("amlChecked")
    private Boolean amlChecked = false;

    // Metadata
    @JsonProperty("metadata")
    private String metadata;

    @JsonProperty("ipfsHash")
    private String ipfsHash;

    // ==================== CONSTRUCTORS ====================

    public SmartContract() {
        this.createdAt = Instant.now();
        this.status = ContractStatus.DRAFT;
        this.version = 1;
        this.currency = "AUR";
        this.executionCount = 0L;
        this.gasUsed = 0L;
        this.isRWA = false;
        this.isDigitalTwin = false;
        this.isVerified = false;
        this.kycVerified = false;
        this.amlChecked = false;
    }

    public SmartContract(String contractId, String owner, String name, ContractType contractType) {
        this();
        this.contractId = contractId;
        this.owner = owner;
        this.name = name;
        this.contractType = contractType;
    }

    // ==================== LIFECYCLE METHODS ====================

    /**
     * Ensure createdAt is set (call before first persist)
     */
    public void ensureCreatedAt() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (status == null) {
            status = ContractStatus.DRAFT;
        }
    }

    /**
     * Update timestamp (call before each persist)
     */
    public void updateTimestamp() {
        updatedAt = Instant.now();
    }

    /**
     * Deploy the contract (move from DRAFT to DEPLOYED state)
     */
    public void deploy() {
        if (status != ContractStatus.DRAFT && status != ContractStatus.COMPILED) {
            throw new IllegalStateException("Cannot deploy contract in state: " + status);
        }
        this.status = ContractStatus.DEPLOYED;
        this.deployedAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    /**
     * Activate the contract
     */
    public void activate() {
        if (status != ContractStatus.DEPLOYED) {
            throw new IllegalStateException("Cannot activate contract in state: " + status);
        }
        this.status = ContractStatus.ACTIVE;
        this.updatedAt = Instant.now();
    }

    /**
     * Complete the contract
     */
    public void complete() {
        if (status != ContractStatus.ACTIVE) {
            throw new IllegalStateException("Cannot complete contract in state: " + status);
        }
        this.status = ContractStatus.COMPLETED;
        this.completedAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    /**
     * Terminate the contract
     */
    public void terminate() {
        this.status = ContractStatus.TERMINATED;
        this.updatedAt = Instant.now();
    }

    /**
     * Record contract execution
     */
    public void recordExecution(long gasConsumed) {
        this.executionCount++;
        this.gasUsed += gasConsumed;
        this.lastExecutedAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    /**
     * Check if contract is active and can be executed
     */
    public boolean isExecutable() {
        return status == ContractStatus.ACTIVE &&
               (expiresAt == null || expiresAt.isAfter(Instant.now()));
    }

    /**
     * Check if contract has expired
     */
    public boolean isExpired() {
        return expiresAt != null && expiresAt.isBefore(Instant.now());
    }

    // ==================== GETTERS AND SETTERS ====================

    public String getContractId() { return contractId; }
    public void setContractId(String contractId) { this.contractId = contractId; }

    public String getOwner() { return owner; }
    public void setOwner(String owner) { this.owner = owner; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public ContractType getContractType() { return contractType; }
    public void setContractType(ContractType contractType) { this.contractType = contractType; }

    public String getSourceCode() { return sourceCode; }
    public void setSourceCode(String sourceCode) { this.sourceCode = sourceCode; }

    public String getBytecode() { return bytecode; }
    public void setBytecode(String bytecode) { this.bytecode = bytecode; }

    public String getAbiDefinition() { return abiDefinition; }
    public void setAbiDefinition(String abiDefinition) { this.abiDefinition = abiDefinition; }

    public ContractStatus getStatus() { return status; }
    public void setStatus(ContractStatus status) { this.status = status; }

    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }

    public String getTemplateId() { return templateId; }
    public void setTemplateId(String templateId) { this.templateId = templateId; }

    public String getParentContractId() { return parentContractId; }
    public void setParentContractId(String parentContractId) { this.parentContractId = parentContractId; }

    public BigDecimal getValue() { return value; }
    public void setValue(BigDecimal value) { this.value = value; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getAssetId() { return assetId; }
    public void setAssetId(String assetId) { this.assetId = assetId; }

    public AssetType getAssetType() { return assetType; }
    public void setAssetType(AssetType assetType) { this.assetType = assetType; }

    public Boolean getIsRWA() { return isRWA; }
    public void setIsRWA(Boolean isRWA) { this.isRWA = isRWA; }

    public Boolean getIsDigitalTwin() { return isDigitalTwin; }
    public void setIsDigitalTwin(Boolean isDigitalTwin) { this.isDigitalTwin = isDigitalTwin; }

    public Boolean getIsVerified() { return isVerified; }
    public void setIsVerified(Boolean isVerified) { this.isVerified = isVerified; }

    public String getVerificationHash() { return verificationHash; }
    public void setVerificationHash(String verificationHash) { this.verificationHash = verificationHash; }

    public String getSecurityAuditStatus() { return securityAuditStatus; }
    public void setSecurityAuditStatus(String securityAuditStatus) { this.securityAuditStatus = securityAuditStatus; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public Instant getDeployedAt() { return deployedAt; }
    public void setDeployedAt(Instant deployedAt) { this.deployedAt = deployedAt; }

    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }

    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }

    public Long getExecutionCount() { return executionCount; }
    public void setExecutionCount(Long executionCount) { this.executionCount = executionCount; }

    public Long getGasUsed() { return gasUsed; }
    public void setGasUsed(Long gasUsed) { this.gasUsed = gasUsed; }

    public Instant getLastExecutedAt() { return lastExecutedAt; }
    public void setLastExecutedAt(Instant lastExecutedAt) { this.lastExecutedAt = lastExecutedAt; }

    public List<String> getParties() { return parties; }
    public void setParties(List<String> parties) { this.parties = parties; }

    public void addParty(String partyAddress) {
        if (!this.parties.contains(partyAddress)) {
            this.parties.add(partyAddress);
            this.updatedAt = Instant.now();
        }
    }

    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }

    public void addTag(String tag) {
        if (!this.tags.contains(tag)) {
            this.tags.add(tag);
            this.updatedAt = Instant.now();
        }
    }

    public String getJurisdiction() { return jurisdiction; }
    public void setJurisdiction(String jurisdiction) { this.jurisdiction = jurisdiction; }

    public String getComplianceStatus() { return complianceStatus; }
    public void setComplianceStatus(String complianceStatus) { this.complianceStatus = complianceStatus; }

    public Boolean getKycVerified() { return kycVerified; }
    public void setKycVerified(Boolean kycVerified) { this.kycVerified = kycVerified; }

    public Boolean getAmlChecked() { return amlChecked; }
    public void setAmlChecked(Boolean amlChecked) { this.amlChecked = amlChecked; }

    public String getMetadata() { return metadata; }
    public void setMetadata(String metadata) { this.metadata = metadata; }

    public String getIpfsHash() { return ipfsHash; }
    public void setIpfsHash(String ipfsHash) { this.ipfsHash = ipfsHash; }

    // ==================== ENUM DEFINITIONS ====================

    public enum ContractType {
        RICARDIAN,
        RWA_TOKENIZATION,
        DIGITAL_TWIN,
        STANDARD,
        TEMPLATE,
        ESCROW,
        MULTI_PARTY,
        FRACTIONAL_OWNERSHIP
    }

    @Override
    public String toString() {
        return String.format("SmartContract{contractId='%s', name='%s', type=%s, status=%s, owner='%s'}",
                contractId, name, contractType, status, owner);
    }
}
