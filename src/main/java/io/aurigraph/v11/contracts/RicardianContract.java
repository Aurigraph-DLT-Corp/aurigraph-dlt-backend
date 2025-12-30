package io.aurigraph.v11.contracts;

import io.aurigraph.v11.contracts.models.*;
import java.time.Instant;
import java.util.*;
import io.aurigraph.v11.contracts.models.ContractSignature;

/**
 * Ricardian Contract - Legal prose + Smart Contract logic
 * Combines human-readable legal agreement with machine-executable code
 */
public class RicardianContract {

    private String contractId;
    private String legalProse;
    private String smartContractCode;
    private ContractStatus status;
    private List<ContractParty> parties;
    private Map<String, Object> parameters;
    private Instant createdAt;
    private Instant updatedAt;
    private String createdBy;
    private String contractType;
    private String version;
    private boolean immutable;
    private String templateId;
    private Map<String, String> metadata;
    private List<ContractSignature> signatures;

    // Additional fields for BUG-007 fix
    private String name;
    private String jurisdiction;
    private List<ContractTerm> terms;
    private double enforceabilityScore;
    private String riskAssessment;
    private List<String> auditLog;
    private String contractHash;
    private String contractAddress;
    private String assetType;
    private List<ContractTrigger> triggers;
    private List<ExecutionResult> executions;
    private Instant lastExecutedAt;
    private Instant activatedAt;

    // Default constructor
    public RicardianContract() {
        this.contractId = UUID.randomUUID().toString();
        this.status = ContractStatus.DRAFT;
        this.parties = new ArrayList<>();
        this.parameters = new HashMap<>();
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
        this.version = "1.0.0";
        this.immutable = false;
        this.metadata = new HashMap<>();
        this.signatures = new ArrayList<>();
        this.terms = new ArrayList<>();
        this.auditLog = new ArrayList<>();
        this.enforceabilityScore = 0.0;
        this.triggers = new ArrayList<>();
        this.executions = new ArrayList<>();
    }

    // Full constructor
    public RicardianContract(String contractId, String legalProse, String smartContractCode,
                            ContractStatus status, List<ContractParty> parties) {
        this.contractId = contractId != null ? contractId : UUID.randomUUID().toString();
        this.legalProse = legalProse;
        this.smartContractCode = smartContractCode;
        this.status = status != null ? status : ContractStatus.DRAFT;
        this.parties = parties != null ? parties : new ArrayList<>();
        this.parameters = new HashMap<>();
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
        this.version = "1.0.0";
        this.immutable = false;
    }

    // Getters and Setters
    public String getContractId() {
        return contractId;
    }

    public void setContractId(String contractId) {
        this.contractId = contractId;
    }

    public String getLegalProse() {
        return legalProse;
    }

    public void setLegalProse(String legalProse) {
        this.legalProse = legalProse;
    }

    public String getSmartContractCode() {
        return smartContractCode;
    }

    public void setSmartContractCode(String smartContractCode) {
        this.smartContractCode = smartContractCode;
    }

    public ContractStatus getStatus() {
        return status;
    }

    public void setStatus(ContractStatus status) {
        this.status = status;
        this.updatedAt = Instant.now();
    }

    public List<ContractParty> getParties() {
        return parties;
    }

    public void setParties(List<ContractParty> parties) {
        this.parties = parties;
    }

    public Map<String, Object> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, Object> parameters) {
        this.parameters = parameters;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getContractType() {
        return contractType;
    }

    public void setContractType(String contractType) {
        this.contractType = contractType;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public boolean isImmutable() {
        return immutable;
    }

    public void setImmutable(boolean immutable) {
        this.immutable = immutable;
    }

    public String getTemplateId() {
        return templateId;
    }

    public void setTemplateId(String templateId) {
        this.templateId = templateId;
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, String> metadata) {
        this.metadata = metadata;
    }

    public List<ContractSignature> getSignatures() {
        return signatures;
    }

    public void setSignatures(List<ContractSignature> signatures) {
        this.signatures = signatures;
    }

    // Additional getters and setters for BUG-007 fix
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getJurisdiction() {
        return jurisdiction;
    }

    public void setJurisdiction(String jurisdiction) {
        this.jurisdiction = jurisdiction;
    }

    public List<ContractTerm> getTerms() {
        return terms;
    }

    public void setTerms(List<ContractTerm> terms) {
        this.terms = terms;
    }

    public double getEnforceabilityScore() {
        return enforceabilityScore;
    }

    public void setEnforceabilityScore(double enforceabilityScore) {
        this.enforceabilityScore = enforceabilityScore;
    }

    public String getRiskAssessment() {
        return riskAssessment;
    }

    public void setRiskAssessment(String riskAssessment) {
        this.riskAssessment = riskAssessment;
    }

    public List<String> getAuditLog() {
        return auditLog;
    }

    public void setAuditLog(List<String> auditLog) {
        this.auditLog = auditLog;
    }

    public String getContractHash() {
        return contractHash;
    }

    public void setContractHash(String contractHash) {
        this.contractHash = contractHash;
    }

    public String getContractAddress() {
        return contractAddress;
    }

    public void setContractAddress(String contractAddress) {
        this.contractAddress = contractAddress;
    }

    public String getAssetType() {
        return assetType;
    }

    public void setAssetType(String assetType) {
        this.assetType = assetType;
    }

    public List<ContractTrigger> getTriggers() {
        return triggers;
    }

    public void setTriggers(List<ContractTrigger> triggers) {
        this.triggers = triggers;
    }

    public List<ExecutionResult> getExecutions() {
        return executions;
    }

    public void setExecutions(List<ExecutionResult> executions) {
        this.executions = executions;
    }

    public Instant getLastExecutedAt() {
        return lastExecutedAt;
    }

    public void setLastExecutedAt(Instant lastExecutedAt) {
        this.lastExecutedAt = lastExecutedAt;
    }

    public Instant getActivatedAt() {
        return activatedAt;
    }

    public void setActivatedAt(Instant activatedAt) {
        this.activatedAt = activatedAt;
    }

    // Alias methods for compatibility
    public String getLegalText() {
        return this.legalProse;
    }

    public void setLegalText(String legalText) {
        this.legalProse = legalText;
    }

    public String getExecutableCode() {
        return this.smartContractCode;
    }

    public void setExecutableCode(String executableCode) {
        this.smartContractCode = executableCode;
    }

    // Business methods
    public void addParty(ContractParty party) {
        if (!this.parties.contains(party)) {
            this.parties.add(party);
            this.updatedAt = Instant.now();
        }
    }

    public void addParameter(String key, Object value) {
        this.parameters.put(key, value);
        this.updatedAt = Instant.now();
    }

    public void addTerm(ContractTerm term) {
        if (this.terms == null) {
            this.terms = new ArrayList<>();
        }
        this.terms.add(term);
        this.updatedAt = Instant.now();
    }

    public void addAuditEntry(String entry) {
        if (this.auditLog == null) {
            this.auditLog = new ArrayList<>();
        }
        this.auditLog.add(Instant.now() + ": " + entry);
    }

    public void addSignature(ContractSignature signature) {
        if (this.signatures == null) {
            this.signatures = new ArrayList<>();
        }
        this.signatures.add(signature);
        this.updatedAt = Instant.now();
    }

    public void addExecution(ExecutionResult execution) {
        if (this.executions == null) {
            this.executions = new ArrayList<>();
        }
        this.executions.add(execution);
        this.lastExecutedAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void addTrigger(ContractTrigger trigger) {
        if (this.triggers == null) {
            this.triggers = new ArrayList<>();
        }
        this.triggers.add(trigger);
        this.updatedAt = Instant.now();
    }

    public ContractParty getPartyById(String partyId) {
        if (this.parties == null) {
            return null;
        }
        return this.parties.stream()
                .filter(p -> p.getPartyId().equals(partyId))
                .findFirst()
                .orElse(null);
    }

    public ContractTrigger getTriggerById(String triggerId) {
        if (this.triggers == null) {
            return null;
        }
        return this.triggers.stream()
                .filter(t -> t.getTriggerId().equals(triggerId))
                .findFirst()
                .orElse(null);
    }

    public boolean isActive() {
        return this.status == ContractStatus.ACTIVE;
    }

    public boolean isDraft() {
        return this.status == ContractStatus.DRAFT;
    }

    public boolean isExecuted() {
        return this.status == ContractStatus.EXECUTED;
    }

    public boolean isFullySigned() {
        if (parties == null || parties.isEmpty()) {
            return false;
        }

        // Count parties that require signatures
        long requiredSignatures = parties.stream()
            .filter(ContractParty::isSignatureRequired)
            .count();

        if (requiredSignatures == 0) {
            return false; // No signatures required means contract is not properly configured
        }

        // Count actual signatures
        long actualSignatures = signatures != null ? signatures.size() : 0;

        return actualSignatures >= requiredSignatures;
    }

    // Builder pattern
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private RicardianContract contract = new RicardianContract();

        public Builder contractId(String contractId) {
            contract.contractId = contractId;
            return this;
        }

        public Builder legalText(String legalText) {
            contract.legalProse = legalText;
            return this;
        }

        public Builder executableCode(String code) {
            contract.smartContractCode = code;
            return this;
        }

        public Builder contractType(String contractType) {
            contract.contractType = contractType;
            return this;
        }

        public Builder version(String version) {
            contract.version = version;
            return this;
        }

        public Builder parties(List<ContractParty> parties) {
            contract.parties = new ArrayList<>(parties);
            return this;
        }

        public Builder status(ContractStatus status) {
            contract.status = status;
            return this;
        }

        public Builder createdAt(Instant createdAt) {
            contract.createdAt = createdAt;
            return this;
        }

        public Builder name(String name) {
            contract.name = name;
            return this;
        }

        public Builder jurisdiction(String jurisdiction) {
            contract.jurisdiction = jurisdiction;
            return this;
        }

        public RicardianContract build() {
            if (contract.terms == null) {
                contract.terms = new ArrayList<>();
            }
            if (contract.auditLog == null) {
                contract.auditLog = new ArrayList<>();
            }
            return contract;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RicardianContract that = (RicardianContract) o;
        return Objects.equals(contractId, that.contractId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(contractId);
    }

    @Override
    public String toString() {
        return "RicardianContract{" +
                "contractId='" + contractId + '\'' +
                ", status=" + status +
                ", contractType='" + contractType + '\'' +
                ", parties=" + parties.size() +
                ", version='" + version + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}
