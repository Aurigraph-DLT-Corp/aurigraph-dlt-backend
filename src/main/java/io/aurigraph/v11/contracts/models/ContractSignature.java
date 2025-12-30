package io.aurigraph.v11.contracts.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.*;

/**
 * Represents a digital signature on a contract with quantum-safe cryptography
 */
public class ContractSignature {

    @JsonProperty("signatureId")
    private String signatureId;

    @JsonProperty("contractId")
    private String contractId;

    @JsonProperty("partyId")
    private String partyId;  // Party identifier for multi-party contracts

    @JsonProperty("signerAddress")
    private String signerAddress;

    @JsonProperty("signerName")
    private String signerName;

    @JsonProperty("signatureData")
    private String signatureData;

    @JsonProperty("signature")
    private String signature; // Quantum-safe signature (Dilithium)

    @JsonProperty("signatureAlgorithm")
    private String signatureAlgorithm;

    @JsonProperty("algorithm")
    private String algorithm = "CRYSTALS-Dilithium"; // Default quantum-safe algorithm

    @JsonProperty("signatureType")
    private String signatureType = "CRYSTALS-Dilithium";

    @JsonProperty("signedAt")
    private Instant signedAt;

    @JsonProperty("timestamp")
    private Instant timestamp;

    @JsonProperty("publicKey")
    private String publicKey; // Quantum-safe public key

    @JsonProperty("verified")
    private boolean verified;

    @JsonProperty("witnessedBy")
    private List<String> witnessedBy = new ArrayList<>();

    @JsonProperty("metadata")
    private Map<String, String> metadata = new HashMap<>();

    // Constructors
    public ContractSignature() {
    }

    public ContractSignature(String signerAddress, String signatureData) {
        this.signerAddress = signerAddress;
        this.signatureData = signatureData;
        this.signature = signatureData;
        this.signedAt = Instant.now();
        this.timestamp = Instant.now();
        this.verified = false;
    }

    public ContractSignature(String signerAddress, String signature, String publicKey) {
        this();
        this.signerAddress = signerAddress;
        this.signature = signature;
        this.signatureData = signature;
        this.publicKey = publicKey;
        this.signedAt = Instant.now();
        this.timestamp = Instant.now();
    }

    public ContractSignature(String signatureId, String contractId, String signerAddress,
                           String signerName, String signatureData, String signatureAlgorithm,
                           Instant signedAt, String publicKey, boolean verified) {
        this.signatureId = signatureId;
        this.contractId = contractId;
        this.signerAddress = signerAddress;
        this.signerName = signerName;
        this.signatureData = signatureData;
        this.signature = signatureData;
        this.signatureAlgorithm = signatureAlgorithm;
        this.algorithm = signatureAlgorithm;
        this.signedAt = signedAt;
        this.timestamp = signedAt;
        this.publicKey = publicKey;
        this.verified = verified;
    }

    // Getters
    public String getSignatureId() {
        return signatureId;
    }

    public String getContractId() {
        return contractId;
    }

    public String getPartyId() {
        return partyId != null ? partyId : signerAddress;
    }

    public String getSignerAddress() {
        return signerAddress;
    }

    public String getSignerName() {
        return signerName;
    }

    public String getSignatureData() {
        return signatureData;
    }

    public String getSignatureAlgorithm() {
        return signatureAlgorithm;
    }

    public Instant getSignedAt() {
        return signedAt;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public boolean isVerified() {
        return verified;
    }

    // Setters
    public void setSignatureId(String signatureId) {
        this.signatureId = signatureId;
    }

    public void setContractId(String contractId) {
        this.contractId = contractId;
    }

    public void setPartyId(String partyId) {
        this.partyId = partyId;
    }

    public void setSignerAddress(String signerAddress) {
        this.signerAddress = signerAddress;
    }

    public void setSignerName(String signerName) {
        this.signerName = signerName;
    }

    public void setSignatureData(String signatureData) {
        this.signatureData = signatureData;
    }

    public void setSignatureAlgorithm(String signatureAlgorithm) {
        this.signatureAlgorithm = signatureAlgorithm;
    }

    public void setSignedAt(Instant signedAt) {
        this.signedAt = signedAt;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }

    public void setVerified(boolean verified) {
        this.verified = verified;
    }

    public String getSignature() {
        return signature != null ? signature : signatureData;
    }

    public void setSignature(String signature) {
        this.signature = signature;
        if (this.signatureData == null) {
            this.signatureData = signature;
        }
    }

    public String getAlgorithm() {
        return algorithm != null ? algorithm : signatureAlgorithm;
    }

    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
        if (this.signatureAlgorithm == null) {
            this.signatureAlgorithm = algorithm;
        }
    }

    public String getSignatureType() {
        return signatureType;
    }

    public void setSignatureType(String signatureType) {
        this.signatureType = signatureType;
    }

    public Instant getTimestamp() {
        return timestamp != null ? timestamp : signedAt;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public List<String> getWitnessedBy() {
        return witnessedBy;
    }

    public void setWitnessedBy(List<String> witnessedBy) {
        this.witnessedBy = witnessedBy;
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, String> metadata) {
        this.metadata = metadata;
    }

    @Override
    public String toString() {
        return String.format("ContractSignature{signer='%s', algorithm='%s', signedAt=%s}",
            signerAddress, algorithm, signedAt);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ContractSignature that = (ContractSignature) o;
        return Objects.equals(signerAddress, that.signerAddress) &&
               Objects.equals(signature, that.signature);
    }

    @Override
    public int hashCode() {
        return Objects.hash(signerAddress, signature);
    }
}
