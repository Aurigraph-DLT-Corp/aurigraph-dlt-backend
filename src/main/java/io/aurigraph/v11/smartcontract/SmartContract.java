package io.aurigraph.v11.smartcontract;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Core Smart Contract Model for Aurigraph DLT
 *
 * Represents a deployed smart contract on the Aurigraph blockchain platform.
 * Supports multiple contract languages and execution environments.
 *
 * @version 11.2.1
 * @since 2025-10-12
 */
public class SmartContract {

    @JsonProperty("contractId")
    private String contractId;

    @JsonProperty("name")
    private String name;

    @JsonProperty("version")
    private String version;

    @JsonProperty("owner")
    private String owner;

    @JsonProperty("code")
    private String code;

    @JsonProperty("language")
    private ContractLanguage language;

    @JsonProperty("abi")
    private String abi; // Application Binary Interface (JSON format)

    @JsonProperty("bytecode")
    private String bytecode;

    @JsonProperty("state")
    private Map<String, Object> state;

    @JsonProperty("status")
    private ContractStatus status;

    @JsonProperty("createdAt")
    private Instant createdAt;

    @JsonProperty("updatedAt")
    private Instant updatedAt;

    @JsonProperty("deployedAt")
    private Instant deployedAt;

    @JsonProperty("metadata")
    private ContractMetadata metadata;

    /**
     * Supported smart contract programming languages
     */
    public enum ContractLanguage {
        SOLIDITY,      // Ethereum-compatible Solidity
        JAVA,          // Native Java contracts
        JAVASCRIPT,    // JavaScript contracts
        WASM,          // WebAssembly contracts
        PYTHON,        // Python contracts (experimental)
        CUSTOM         // Custom contract language
    }

    /**
     * Smart contract lifecycle status
     */
    public enum ContractStatus {
        DRAFT,         // Contract being developed
        COMPILED,      // Contract compiled successfully
        DEPLOYED,      // Contract deployed to blockchain
        ACTIVE,        // Contract active and executable
        PAUSED,        // Contract execution paused
        DEPRECATED,    // Contract marked for replacement
        TERMINATED     // Contract permanently disabled
    }

    // Constructors
    public SmartContract() {
        this.contractId = UUID.randomUUID().toString();
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
        this.status = ContractStatus.DRAFT;
    }

    public SmartContract(String name, String code, ContractLanguage language, String owner) {
        this();
        this.name = name;
        this.code = code;
        this.language = language;
        this.owner = owner;
    }

    // Getters and Setters
    public String getContractId() {
        return contractId;
    }

    public void setContractId(String contractId) {
        this.contractId = contractId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public ContractLanguage getLanguage() {
        return language;
    }

    public void setLanguage(ContractLanguage language) {
        this.language = language;
    }

    public String getAbi() {
        return abi;
    }

    public void setAbi(String abi) {
        this.abi = abi;
    }

    public String getBytecode() {
        return bytecode;
    }

    public void setBytecode(String bytecode) {
        this.bytecode = bytecode;
    }

    public Map<String, Object> getState() {
        return state;
    }

    public void setState(Map<String, Object> state) {
        this.state = state;
    }

    public ContractStatus getStatus() {
        return status;
    }

    public void setStatus(ContractStatus status) {
        this.status = status;
        this.updatedAt = Instant.now();
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

    public Instant getDeployedAt() {
        return deployedAt;
    }

    public void setDeployedAt(Instant deployedAt) {
        this.deployedAt = deployedAt;
    }

    public ContractMetadata getMetadata() {
        return metadata;
    }

    public void setMetadata(ContractMetadata metadata) {
        this.metadata = metadata;
    }

    @Override
    public String toString() {
        return String.format(
            "SmartContract{id='%s', name='%s', version='%s', language=%s, status=%s, owner='%s'}",
            contractId, name, version, language, status, owner
        );
    }
}
