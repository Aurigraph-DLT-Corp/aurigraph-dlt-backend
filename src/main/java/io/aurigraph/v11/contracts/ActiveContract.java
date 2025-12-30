package io.aurigraph.v11.contracts;

import io.aurigraph.v11.contracts.models.*;
import java.time.Instant;
import java.util.*;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Aurigraph ActiveContract entity - combines legal prose with executable code
 * Features quantum-safe signatures and multi-party execution
 *
 * ActiveContracts (formerly Ricardian Contracts) are self-executing agreements
 * that link legal text with smart contract code for automated enforcement.
 */
public class ActiveContract {
    
    @JsonProperty("contractId")
    private String contractId;
    
    @JsonProperty("name")
    private String name;
    
    @JsonProperty("version")
    private String version = "1.0.0";

    @JsonProperty("owner")
    private String owner; // Contract owner/deployer address

    @JsonProperty("legalText")
    private String legalText; // Human-readable legal prose

    @JsonProperty("executableCode")
    private String executableCode; // Smart contract code (alias for 'code')

    @JsonProperty("code")
    private String code; // Smart contract code (SDK field)

    @JsonProperty("language")
    private ContractLanguage language; // Contract programming language

    @JsonProperty("abi")
    private String abi; // Application Binary Interface (JSON)

    @JsonProperty("bytecode")
    private String bytecode; // Compiled bytecode

    @JsonProperty("state")
    private Map<String, Object> state; // Contract state variables
    
    @JsonProperty("contractType")
    private String contractType; // RWA, Carbon, RealEstate, etc.
    
    @JsonProperty("assetType")
    private String assetType;
    
    @JsonProperty("jurisdiction")
    private String jurisdiction;
    
    @JsonProperty("parties")
    private List<ContractParty> parties = new ArrayList<>(); // Changed to ContractParty
    
    @JsonProperty("terms")
    private List<ContractTerm> terms = new ArrayList<>();
    
    @JsonProperty("triggers")
    private List<ContractTrigger> triggers = new ArrayList<>();
    
    @JsonProperty("signatures")
    private List<ContractSignature> signatures;
    
    @JsonProperty("status")
    private ContractStatus status;
    
    @JsonProperty("createdAt")
    private Instant createdAt;

    @JsonProperty("deployedAt")
    private Instant deployedAt; // When contract was deployed to network

    @JsonProperty("updatedAt")
    private Instant updatedAt;

    @JsonProperty("activatedAt")
    private Instant activatedAt;

    @JsonProperty("lastExecutedAt")
    private Instant lastExecutedAt;
    
    @JsonProperty("executionCount")
    private long executionCount;
    
    @JsonProperty("templateId")
    private String templateId;
    
    @JsonProperty("metadata")
    private Map<String, String> metadata;
    
    @JsonProperty("enforceabilityScore")
    private double enforceabilityScore;
    
    @JsonProperty("riskAssessment")
    private String riskAssessment;
    
    @JsonProperty("auditTrail")
    private List<String> auditTrail = new ArrayList<>();
    
    @JsonProperty("executions")
    private List<ExecutionResult> executions = new ArrayList<>();
    
    @JsonProperty("quantumSafe")
    private boolean quantumSafe = true; // All contracts use quantum-safe crypto

    // Default constructor
    public ActiveContract() {
        this.signatures = new ArrayList<>();
        this.metadata = new HashMap<>();
        this.state = new HashMap<>();
        this.executionCount = 0;
    }

    // Constructor with basic fields (SDK compatibility)
    public ActiveContract(String name, String code, ContractLanguage language, String owner) {
        this();
        this.name = name;
        this.code = code;
        this.executableCode = code; // Sync both fields
        this.language = language;
        this.owner = owner;
        this.status = ContractStatus.DRAFT;
        this.createdAt = Instant.now();
    }

    // Builder pattern
    public static ActiveContractBuilder builder() {
        return new ActiveContractBuilder();
    }

    // Getters and setters
    public String getContractId() { return contractId; }
    public void setContractId(String contractId) { this.contractId = contractId; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }

    public String getOwner() { return owner; }
    public void setOwner(String owner) { this.owner = owner; }

    public String getLegalText() { return legalText; }
    public void setLegalText(String legalText) { this.legalText = legalText; }
    
    public String getExecutableCode() { return executableCode; }
    public void setExecutableCode(String executableCode) {
        this.executableCode = executableCode;
        this.code = executableCode; // Keep both fields in sync
    }

    public String getCode() { return code; }
    public void setCode(String code) {
        this.code = code;
        this.executableCode = code; // Keep both fields in sync
    }

    public ContractLanguage getLanguage() { return language; }
    public void setLanguage(ContractLanguage language) { this.language = language; }

    public String getAbi() { return abi; }
    public void setAbi(String abi) { this.abi = abi; }

    public String getBytecode() { return bytecode; }
    public void setBytecode(String bytecode) { this.bytecode = bytecode; }

    public Map<String, Object> getState() { return state; }
    public void setState(Map<String, Object> state) { this.state = state; }

    public String getContractType() { return contractType; }
    public void setContractType(String contractType) { this.contractType = contractType; }
    
    public String getAssetType() { return assetType; }
    public void setAssetType(String assetType) { this.assetType = assetType; }
    
    public String getJurisdiction() { return jurisdiction; }
    public void setJurisdiction(String jurisdiction) { this.jurisdiction = jurisdiction; }
    
    public List<ContractParty> getParties() { return parties; }
    public void setParties(List<ContractParty> parties) { this.parties = parties; }
    
    public List<ContractTerm> getTerms() { return terms; }
    public void setTerms(List<ContractTerm> terms) { this.terms = terms; }
    
    public List<ContractTrigger> getTriggers() { return triggers; }
    public void setTriggers(List<ContractTrigger> triggers) { this.triggers = triggers; }
    
    public List<ContractSignature> getSignatures() { return signatures; }
    public void setSignatures(List<ContractSignature> signatures) { this.signatures = signatures; }
    
    public ContractStatus getStatus() { return status; }
    public void setStatus(ContractStatus status) { this.status = status; }
    
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getDeployedAt() { return deployedAt; }
    public void setDeployedAt(Instant deployedAt) { this.deployedAt = deployedAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public Instant getActivatedAt() { return activatedAt; }
    public void setActivatedAt(Instant activatedAt) { this.activatedAt = activatedAt; }
    
    public Instant getLastExecutedAt() { return lastExecutedAt; }
    public void setLastExecutedAt(Instant lastExecutedAt) { this.lastExecutedAt = lastExecutedAt; }
    
    public long getExecutionCount() { return executionCount; }
    public void setExecutionCount(long executionCount) { this.executionCount = executionCount; }
    
    public String getTemplateId() { return templateId; }
    public void setTemplateId(String templateId) { this.templateId = templateId; }
    
    public Map<String, String> getMetadata() { return metadata; }
    public void setMetadata(Map<String, String> metadata) { this.metadata = metadata; }
    
    public double getEnforceabilityScore() { return enforceabilityScore; }
    public void setEnforceabilityScore(double enforceabilityScore) { this.enforceabilityScore = enforceabilityScore; }
    
    public String getRiskAssessment() { return riskAssessment; }
    public void setRiskAssessment(String riskAssessment) { this.riskAssessment = riskAssessment; }
    
    public List<String> getAuditTrail() { return auditTrail; }
    public void setAuditTrail(List<String> auditTrail) { this.auditTrail = auditTrail; }
    
    public List<ExecutionResult> getExecutions() { return executions; }
    public void setExecutions(List<ExecutionResult> executions) { this.executions = executions; }
    
    public boolean isQuantumSafe() { return quantumSafe; }
    public void setQuantumSafe(boolean quantumSafe) { this.quantumSafe = quantumSafe; }

    // Utility methods
    public boolean isActive() {
        return status == ContractStatus.ACTIVE;
    }

    public boolean isFullySigned() {
        Set<String> requiredSigners = new HashSet<>();
        for (ContractParty party : parties) {
            if (party.isSignatureRequired()) {
                requiredSigners.add(party.getAddress());
            }
        }
        
        Set<String> actualSigners = new HashSet<>();
        for (ContractSignature signature : signatures) {
            actualSigners.add(signature.getSignerAddress());
        }
        
        return actualSigners.containsAll(requiredSigners);
    }

    public ContractSignature getSignatureByAddress(String address) {
        return signatures.stream()
            .filter(sig -> address.equals(sig.getSignerAddress()))
            .findFirst()
            .orElse(null);
    }

    public void addMetadata(String key, String value) {
        if (metadata == null) {
            metadata = new HashMap<>();
        }
        metadata.put(key, value);
    }
    
    // Additional methods needed by SmartContractService
    public void addParty(ContractParty party) {
        if (parties == null) {
            parties = new ArrayList<>();
        }
        parties.add(party);
    }
    
    public void addTerm(ContractTerm term) {
        if (terms == null) {
            terms = new ArrayList<>();
        }
        terms.add(term);
    }
    
    public void addSignature(ContractSignature signature) {
        if (signatures == null) {
            signatures = new ArrayList<>();
        }
        signatures.add(signature);
    }
    
    public void addExecution(ExecutionResult execution) {
        if (executions == null) {
            executions = new ArrayList<>();
        }
        executions.add(execution);
    }
    
    public void addAuditEntry(String entry) {
        if (auditTrail == null) {
            auditTrail = new ArrayList<>();
        }
        auditTrail.add(entry);
    }
    
    public ContractParty getPartyById(String partyId) {
        if (parties == null) return null;
        return parties.stream()
            .filter(party -> partyId.equals(party.getPartyId()))
            .findFirst()
            .orElse(null);
    }
    
    public ContractTrigger getTriggerById(String triggerId) {
        if (triggers == null) return null;
        return triggers.stream()
            .filter(trigger -> triggerId.equals(trigger.getTriggerId()))
            .findFirst()
            .orElse(null);
    }

    @Override
    public String toString() {
        return String.format("ActiveContract{id='%s', type='%s', status=%s, parties=%d, signatures=%d}",
            contractId, contractType, status, parties != null ? parties.size() : 0, signatures.size());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ActiveContract that = (ActiveContract) o;
        return Objects.equals(contractId, that.contractId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(contractId);
    }

    // Builder class
    public static class ActiveContractBuilder {
        private ActiveContract contract = new ActiveContract();

        public ActiveContractBuilder contractId(String contractId) {
            contract.contractId = contractId;
            return this;
        }

        public ActiveContractBuilder name(String name) {
            contract.name = name;
            return this;
        }

        public ActiveContractBuilder owner(String owner) {
            contract.owner = owner;
            return this;
        }

        public ActiveContractBuilder legalText(String legalText) {
            contract.legalText = legalText;
            return this;
        }

        public ActiveContractBuilder executableCode(String executableCode) {
            contract.executableCode = executableCode;
            contract.code = executableCode; // Keep both in sync
            return this;
        }

        public ActiveContractBuilder code(String code) {
            contract.code = code;
            contract.executableCode = code; // Keep both in sync
            return this;
        }

        public ActiveContractBuilder language(ContractLanguage language) {
            contract.language = language;
            return this;
        }

        public ActiveContractBuilder abi(String abi) {
            contract.abi = abi;
            return this;
        }

        public ActiveContractBuilder bytecode(String bytecode) {
            contract.bytecode = bytecode;
            return this;
        }

        public ActiveContractBuilder state(Map<String, Object> state) {
            contract.state = state;
            return this;
        }

        public ActiveContractBuilder contractType(String contractType) {
            contract.contractType = contractType;
            return this;
        }

        public ActiveContractBuilder parties(List<ContractParty> parties) {
            contract.parties = parties;
            return this;
        }

        public ActiveContractBuilder signatures(List<ContractSignature> signatures) {
            contract.signatures = signatures;
            return this;
        }

        public ActiveContractBuilder status(ContractStatus status) {
            contract.status = status;
            return this;
        }

        public ActiveContractBuilder createdAt(Instant createdAt) {
            contract.createdAt = createdAt;
            return this;
        }

        public ActiveContractBuilder deployedAt(Instant deployedAt) {
            contract.deployedAt = deployedAt;
            return this;
        }

        public ActiveContractBuilder activatedAt(Instant activatedAt) {
            contract.activatedAt = activatedAt;
            return this;
        }

        public ActiveContractBuilder templateId(String templateId) {
            contract.templateId = templateId;
            return this;
        }

        public ActiveContractBuilder metadata(Map<String, String> metadata) {
            contract.metadata = metadata;
            return this;
        }

        public ActiveContractBuilder version(String version) {
            contract.version = version;
            return this;
        }

        public ActiveContract build() {
            // Validate required fields
            if (contract.contractId == null || contract.contractId.trim().isEmpty()) {
                throw new IllegalArgumentException("Contract ID is required");
            }
            if (contract.legalText == null || contract.legalText.trim().isEmpty()) {
                throw new IllegalArgumentException("Legal text is required");
            }
            if (contract.executableCode == null || contract.executableCode.trim().isEmpty()) {
                throw new IllegalArgumentException("Executable code is required");
            }
            if (contract.parties == null || contract.parties.isEmpty()) {
                throw new IllegalArgumentException("At least one party is required");
            }
            
            // Set defaults
            if (contract.signatures == null) {
                contract.signatures = new ArrayList<>();
            }
            if (contract.metadata == null) {
                contract.metadata = new HashMap<>();
            }
            if (contract.status == null) {
                contract.status = ContractStatus.DRAFT;
            }
            if (contract.createdAt == null) {
                contract.createdAt = Instant.now();
            }
            
            return contract;
        }
    }
}
