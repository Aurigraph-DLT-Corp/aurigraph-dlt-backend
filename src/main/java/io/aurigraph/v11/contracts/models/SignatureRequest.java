package io.aurigraph.v11.contracts.models;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public class SignatureRequest {

    @NotBlank
    private String contractId;

    @NotBlank
    private String signerAddress;

    @NotBlank
    private String signature;

    @NotBlank
    private String messageHash;

    private String publicKey;

    private long timestamp;

    private String signatureAlgorithm = "Dilithium";

    private String privateKey;

    private List<String> witnesses;

    // Constructors
    public SignatureRequest() {
    }

    public SignatureRequest(String contractId, String signerAddress, String signature, String messageHash,
                           String publicKey, long timestamp, String signatureAlgorithm, String privateKey,
                           List<String> witnesses) {
        this.contractId = contractId;
        this.signerAddress = signerAddress;
        this.signature = signature;
        this.messageHash = messageHash;
        this.publicKey = publicKey;
        this.timestamp = timestamp;
        this.signatureAlgorithm = signatureAlgorithm;
        this.privateKey = privateKey;
        this.witnesses = witnesses;
    }

    // Getters
    public String getContractId() {
        return contractId;
    }

    public String getSignerAddress() {
        return signerAddress;
    }

    public String getSignature() {
        return signature;
    }

    public String getMessageHash() {
        return messageHash;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getSignatureAlgorithm() {
        return signatureAlgorithm;
    }

    public String getPrivateKey() {
        return privateKey;
    }

    public List<String> getWitnesses() {
        return witnesses;
    }

    // Setters
    public void setContractId(String contractId) {
        this.contractId = contractId;
    }

    public void setSignerAddress(String signerAddress) {
        this.signerAddress = signerAddress;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public void setMessageHash(String messageHash) {
        this.messageHash = messageHash;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public void setSignatureAlgorithm(String signatureAlgorithm) {
        this.signatureAlgorithm = signatureAlgorithm;
    }

    public void setPrivateKey(String privateKey) {
        this.privateKey = privateKey;
    }

    public void setWitnesses(List<String> witnesses) {
        this.witnesses = witnesses;
    }
}