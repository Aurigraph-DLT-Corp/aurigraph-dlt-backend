package io.aurigraph.v11.bridge.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Bridge Transfer Request Model
 * Contains all information needed to submit a cross-chain bridge transfer
 *
 * @author Backend Development Agent (BDA)
 * @version 1.0
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TransferRequest {

    @JsonProperty("transferId")
    private String transferId; // Unique transfer ID

    @JsonProperty("bridgeId")
    private String bridgeId;

    @JsonProperty("sourceChain")
    private String sourceChain;

    @JsonProperty("targetChain")
    private String targetChain;

    @JsonProperty("sourceAddress")
    private String sourceAddress;

    @JsonProperty("targetAddress")
    private String targetAddress;

    @JsonProperty("tokenContract")
    private String tokenContract;

    @JsonProperty("tokenSymbol")
    private String tokenSymbol;

    @JsonProperty("amount")
    private BigDecimal amount;

    @JsonProperty("requiredSignatures")
    private Integer requiredSignatures; // e.g., 2 for 2-of-3, 3 for 3-of-5

    @JsonProperty("totalSigners")
    private Integer totalSigners; // e.g., 3 for 2-of-3, 5 for 3-of-5

    @JsonProperty("signatures")
    private List<SignatureData> signatures; // Multi-sig signatures

    @JsonProperty("nonce")
    private Long nonce;

    @JsonProperty("timestamp")
    private Long timestamp;

    @JsonProperty("memo")
    private String memo; // Optional memo

    @JsonProperty("metadata")
    private String metadata; // Additional data as JSON

    /**
     * Signature data for multi-signature support
     */
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SignatureData {
        @JsonProperty("signer")
        private String signer; // Signer address

        @JsonProperty("signature")
        private String signature; // Hex encoded signature

        @JsonProperty("signatureType")
        private String signatureType; // SECP256K1, ED25519, ECDSA

        @JsonProperty("signedAt")
        private Long signedAt; // Timestamp when signed

        @JsonProperty("weight")
        private Integer weight; // Signing weight (for weighted multi-sig)

        // Getters
        public String getSigner() { return signer; }
        public String getSignature() { return signature; }
        public String getSignatureType() { return signatureType; }
        public Long getSignedAt() { return signedAt; }
        public Integer getWeight() { return weight; }
    }

    // Getters
    public String getTransferId() { return transferId; }
    public String getBridgeId() { return bridgeId; }
    public String getSourceChain() { return sourceChain; }
    public String getTargetChain() { return targetChain; }
    public String getSourceAddress() { return sourceAddress; }
    public String getTargetAddress() { return targetAddress; }
    public String getTokenContract() { return tokenContract; }
    public String getTokenSymbol() { return tokenSymbol; }
    public BigDecimal getAmount() { return amount; }
    public Integer getRequiredSignatures() { return requiredSignatures; }
    public Integer getTotalSigners() { return totalSigners; }
    public List<SignatureData> getSignatures() { return signatures; }
    public Long getNonce() { return nonce; }
    public Long getTimestamp() { return timestamp; }
    public String getMemo() { return memo; }
    public String getMetadata() { return metadata; }

    /**
     * Check if multi-signature threshold is met
     */
    public boolean isMultiSignatureComplete() {
        if (signatures == null || requiredSignatures == null) {
            return false;
        }
        return signatures.size() >= requiredSignatures;
    }

    /**
     * Get validation errors
     */
    public List<String> getValidationErrors() {
        List<String> errors = new java.util.ArrayList<>();

        if (transferId == null || transferId.isEmpty()) {
            errors.add("Transfer ID is required");
        }
        if (bridgeId == null || bridgeId.isEmpty()) {
            errors.add("Bridge ID is required");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            errors.add("Amount must be greater than zero");
        }
        if (requiredSignatures == null || requiredSignatures < 1) {
            errors.add("Required signatures must be at least 1");
        }
        if (totalSigners == null || totalSigners < requiredSignatures) {
            errors.add("Total signers must be >= required signatures");
        }
        if (signatures == null || signatures.isEmpty()) {
            errors.add("At least one signature is required");
        }

        return errors;
    }
}
