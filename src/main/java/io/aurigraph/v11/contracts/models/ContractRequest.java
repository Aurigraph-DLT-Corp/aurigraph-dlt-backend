package io.aurigraph.v11.contracts.models;

import jakarta.validation.constraints.NotBlank;

import java.util.List;
import java.util.Map;

public class ContractRequest {

    @NotBlank
    private String name;

    @NotBlank
    private String version;

    @NotBlank
    private String legalText;

    @NotBlank
    private String executableCode;

    @NotBlank
    private String jurisdiction;

    @NotBlank
    private String contractType;

    @NotBlank
    private String assetType;

    private List<ContractParty> parties;

    private List<ContractTerm> terms;

    private Map<String, Object> metadata;

    // Legacy fields for backward compatibility
    private String requesterAddress;
    private String signature;
    private long timestamp;
    private String nonce;

    // Constructors
    public ContractRequest() {
    }

    public ContractRequest(String name, String version, String legalText, String executableCode,
                          String jurisdiction, String contractType, String assetType,
                          List<ContractParty> parties, List<ContractTerm> terms, Map<String, Object> metadata,
                          String requesterAddress, String signature, long timestamp, String nonce) {
        this.name = name;
        this.version = version;
        this.legalText = legalText;
        this.executableCode = executableCode;
        this.jurisdiction = jurisdiction;
        this.contractType = contractType;
        this.assetType = assetType;
        this.parties = parties;
        this.terms = terms;
        this.metadata = metadata;
        this.requesterAddress = requesterAddress;
        this.signature = signature;
        this.timestamp = timestamp;
        this.nonce = nonce;
    }

    // Getters
    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }

    public String getLegalText() {
        return legalText;
    }

    public String getExecutableCode() {
        return executableCode;
    }

    public String getJurisdiction() {
        return jurisdiction;
    }

    public String getContractType() {
        return contractType;
    }

    public String getAssetType() {
        return assetType;
    }

    public List<ContractParty> getParties() {
        return parties;
    }

    public List<ContractTerm> getTerms() {
        return terms;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public String getRequesterAddress() {
        return requesterAddress;
    }

    public String getSignature() {
        return signature;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getNonce() {
        return nonce;
    }

    // Setters
    public void setName(String name) {
        this.name = name;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public void setLegalText(String legalText) {
        this.legalText = legalText;
    }

    public void setExecutableCode(String executableCode) {
        this.executableCode = executableCode;
    }

    public void setJurisdiction(String jurisdiction) {
        this.jurisdiction = jurisdiction;
    }

    public void setContractType(String contractType) {
        this.contractType = contractType;
    }

    public void setAssetType(String assetType) {
        this.assetType = assetType;
    }

    public void setParties(List<ContractParty> parties) {
        this.parties = parties;
    }

    public void setTerms(List<ContractTerm> terms) {
        this.terms = terms;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    public void setRequesterAddress(String requesterAddress) {
        this.requesterAddress = requesterAddress;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public void setNonce(String nonce) {
        this.nonce = nonce;
    }
}