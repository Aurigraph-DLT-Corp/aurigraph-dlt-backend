package io.aurigraph.v11.contracts.models;

import java.time.Instant;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

public class ContractTemplate {

    private String templateId;
    private String name;
    private String description;
    private String contractType;
    private String category;
    private String assetType;
    private String jurisdiction;
    private String legalText;
    private String sourceCode;
    private String bytecode;
    private String abi; // Application Binary Interface
    private Map<String, Object> defaultParameters;
    private String[] requiredParameters;
    private String version;
    private String author;
    private Instant createdAt;
    private Instant updatedAt;
    private boolean verified;
    private String verificationHash;
    private List<TemplateVariable> variables = new ArrayList<>();

    // Constructors
    public ContractTemplate() {
    }

    public ContractTemplate(String templateId, String name, String description, String contractType,
                           String category, String assetType, String jurisdiction, String legalText,
                           String sourceCode, String bytecode, String abi, Map<String, Object> defaultParameters,
                           String[] requiredParameters, String version, String author, Instant createdAt,
                           Instant updatedAt, boolean verified, String verificationHash,
                           List<TemplateVariable> variables) {
        this.templateId = templateId;
        this.name = name;
        this.description = description;
        this.contractType = contractType;
        this.category = category;
        this.assetType = assetType;
        this.jurisdiction = jurisdiction;
        this.legalText = legalText;
        this.sourceCode = sourceCode;
        this.bytecode = bytecode;
        this.abi = abi;
        this.defaultParameters = defaultParameters;
        this.requiredParameters = requiredParameters;
        this.version = version;
        this.author = author;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.verified = verified;
        this.verificationHash = verificationHash;
        this.variables = variables != null ? variables : new ArrayList<>();
    }

    // Getters
    public String getTemplateId() {
        return templateId;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getContractType() {
        return contractType;
    }

    public String getCategory() {
        return category;
    }

    public String getAssetType() {
        return assetType;
    }

    public String getJurisdiction() {
        return jurisdiction;
    }

    public String getLegalText() {
        return legalText;
    }

    public String getSourceCode() {
        return sourceCode;
    }

    public String getBytecode() {
        return bytecode;
    }

    public String getAbi() {
        return abi;
    }

    public Map<String, Object> getDefaultParameters() {
        return defaultParameters;
    }

    public String[] getRequiredParameters() {
        return requiredParameters;
    }

    public String getVersion() {
        return version;
    }

    public String getAuthor() {
        return author;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public boolean isVerified() {
        return verified;
    }

    public String getVerificationHash() {
        return verificationHash;
    }

    public List<TemplateVariable> getVariables() {
        return variables;
    }

    // Setters
    public void setTemplateId(String templateId) {
        this.templateId = templateId;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setContractType(String contractType) {
        this.contractType = contractType;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public void setAssetType(String assetType) {
        this.assetType = assetType;
    }

    public void setJurisdiction(String jurisdiction) {
        this.jurisdiction = jurisdiction;
    }

    public void setLegalText(String legalText) {
        this.legalText = legalText;
    }

    public void setSourceCode(String sourceCode) {
        this.sourceCode = sourceCode;
    }

    public void setBytecode(String bytecode) {
        this.bytecode = bytecode;
    }

    public void setAbi(String abi) {
        this.abi = abi;
    }

    public void setDefaultParameters(Map<String, Object> defaultParameters) {
        this.defaultParameters = defaultParameters;
    }

    public void setRequiredParameters(String[] requiredParameters) {
        this.requiredParameters = requiredParameters;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public void setVerified(boolean verified) {
        this.verified = verified;
    }

    public void setVerificationHash(String verificationHash) {
        this.verificationHash = verificationHash;
    }

    public void setVariables(List<TemplateVariable> variables) {
        this.variables = variables;
    }
}