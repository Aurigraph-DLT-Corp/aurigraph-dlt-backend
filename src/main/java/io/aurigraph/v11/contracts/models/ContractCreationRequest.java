package io.aurigraph.v11.contracts.models;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.List;

public class ContractCreationRequest {

    @NotBlank
    private String contractName;

    @NotBlank
    private String contractType;

    @NotBlank
    private String templateId;

    private String description;

    @NotNull
    private Map<String, Object> parameters;

    @NotBlank
    private String creatorAddress;

    private BigDecimal value;

    private Instant expirationDate;

    private Map<String, String> metadata;

    private boolean autoExecute = false;

    private String[] requiredSignatures;

    private String legalText;

    private String executableCode;

    private List<String> parties;

    // Constructors
    public ContractCreationRequest() {
    }

    public ContractCreationRequest(String contractName, String contractType, String templateId,
                                   String description, Map<String, Object> parameters, String creatorAddress,
                                   BigDecimal value, Instant expirationDate, Map<String, String> metadata,
                                   boolean autoExecute, String[] requiredSignatures, String legalText,
                                   String executableCode, List<String> parties) {
        this.contractName = contractName;
        this.contractType = contractType;
        this.templateId = templateId;
        this.description = description;
        this.parameters = parameters;
        this.creatorAddress = creatorAddress;
        this.value = value;
        this.expirationDate = expirationDate;
        this.metadata = metadata;
        this.autoExecute = autoExecute;
        this.requiredSignatures = requiredSignatures;
        this.legalText = legalText;
        this.executableCode = executableCode;
        this.parties = parties;
    }

    // Getters
    public String getContractName() {
        return contractName;
    }

    public String getContractType() {
        return contractType;
    }

    public String getTemplateId() {
        return templateId;
    }

    public String getDescription() {
        return description;
    }

    public Map<String, Object> getParameters() {
        return parameters;
    }

    public String getCreatorAddress() {
        return creatorAddress;
    }

    public BigDecimal getValue() {
        return value;
    }

    public Instant getExpirationDate() {
        return expirationDate;
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }

    public boolean isAutoExecute() {
        return autoExecute;
    }

    public String[] getRequiredSignatures() {
        return requiredSignatures;
    }

    public String getLegalText() {
        return legalText;
    }

    public String getExecutableCode() {
        return executableCode;
    }

    public List<String> getParties() {
        return parties;
    }

    // Setters
    public void setContractName(String contractName) {
        this.contractName = contractName;
    }

    public void setContractType(String contractType) {
        this.contractType = contractType;
    }

    public void setTemplateId(String templateId) {
        this.templateId = templateId;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setParameters(Map<String, Object> parameters) {
        this.parameters = parameters;
    }

    public void setCreatorAddress(String creatorAddress) {
        this.creatorAddress = creatorAddress;
    }

    public void setValue(BigDecimal value) {
        this.value = value;
    }

    public void setExpirationDate(Instant expirationDate) {
        this.expirationDate = expirationDate;
    }

    public void setMetadata(Map<String, String> metadata) {
        this.metadata = metadata;
    }

    public void setAutoExecute(boolean autoExecute) {
        this.autoExecute = autoExecute;
    }

    public void setRequiredSignatures(String[] requiredSignatures) {
        this.requiredSignatures = requiredSignatures;
    }

    public void setLegalText(String legalText) {
        this.legalText = legalText;
    }

    public void setExecutableCode(String executableCode) {
        this.executableCode = executableCode;
    }

    public void setParties(List<String> parties) {
        this.parties = parties;
    }
}