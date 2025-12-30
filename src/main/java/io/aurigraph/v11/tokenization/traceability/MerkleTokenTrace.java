package io.aurigraph.v11.tokenization.traceability;

import java.util.*;
import java.time.LocalDateTime;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * MerkleTokenTrace - Comprehensive token traceability record linking tokens to underlying assets
 * via merkle tree registry proofs.
 *
 * Tracks the complete lifecycle of a token from asset registration through trading.
 * Maintains cryptographic proofs of asset backing and ownership history.
 *
 * @author Aurigraph V12 Token Traceability Team
 * @version 1.0.0
 */
public class MerkleTokenTrace {

    @JsonProperty("trace_id")
    private String traceId;

    @JsonProperty("token_id")
    private String tokenId;

    @JsonProperty("asset_id")
    private String assetId;

    @JsonProperty("asset_type")
    private String assetType;  // REAL_ESTATE, CARBON_CREDIT, ART_COLLECTIBLE, etc.

    @JsonProperty("underlying_asset_hash")
    private String underlyingAssetHash;  // SHA3-256 hash of asset data

    @JsonProperty("merkle_proof_path")
    private List<MerkleProofNode> merkleProofPath;  // Path from asset leaf to merkle root

    @JsonProperty("merkle_root_hash")
    private String merkleRootHash;  // Root hash at time of token creation

    @JsonProperty("token_creation_timestamp")
    private LocalDateTime tokenCreationTimestamp;

    @JsonProperty("token_value_usd")
    private Double tokenValueUsd;

    @JsonProperty("fractional_ownership")
    private Double fractionalOwnership;  // Percentage of asset owned (0-100)

    @JsonProperty("owner_address")
    private String ownerAddress;

    @JsonProperty("asset_verified")
    private Boolean assetVerified;

    @JsonProperty("verification_status")
    private String verificationStatus;  // PENDING, IN_REVIEW, VERIFIED, REJECTED

    @JsonProperty("proof_valid")
    private Boolean proofValid;  // Does merkle proof validate correctly?

    @JsonProperty("ownership_history")
    private List<OwnershipTransfer> ownershipHistory;

    @JsonProperty("compliance_certifications")
    private List<String> complianceCertifications;

    @JsonProperty("audit_trail")
    private List<AuditLogEntry> auditTrail;

    @JsonProperty("last_verified_timestamp")
    private LocalDateTime lastVerifiedTimestamp;

    @JsonProperty("next_verification_due")
    private LocalDateTime nextVerificationDue;

    @JsonProperty("metadata")
    private Map<String, Object> metadata;

    // Constructors
    public MerkleTokenTrace() {
        this.merkleProofPath = new ArrayList<>();
        this.ownershipHistory = new ArrayList<>();
        this.complianceCertifications = new ArrayList<>();
        this.auditTrail = new ArrayList<>();
        this.metadata = new HashMap<>();
        this.proofValid = false;
        this.assetVerified = false;
    }

    public MerkleTokenTrace(String tokenId, String assetId, String assetType) {
        this();
        this.tokenId = tokenId;
        this.assetId = assetId;
        this.assetType = assetType;
        this.traceId = generateTraceId();
        this.tokenCreationTimestamp = LocalDateTime.now();
    }

    // Generate unique trace ID
    private String generateTraceId() {
        try {
            String input = tokenId + assetId + System.nanoTime();
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes());
            return bytesToHex(hash).substring(0, 16).toUpperCase();
        } catch (NoSuchAlgorithmException e) {
            return "TRACE-" + UUID.randomUUID().toString();
        }
    }

    private static String bytesToHex(byte[] hash) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }

    // Getters and Setters
    public String getTraceId() { return traceId; }
    public void setTraceId(String traceId) { this.traceId = traceId; }

    public String getTokenId() { return tokenId; }
    public void setTokenId(String tokenId) { this.tokenId = tokenId; }

    public String getAssetId() { return assetId; }
    public void setAssetId(String assetId) { this.assetId = assetId; }

    public String getAssetType() { return assetType; }
    public void setAssetType(String assetType) { this.assetType = assetType; }

    public String getUnderlyingAssetHash() { return underlyingAssetHash; }
    public void setUnderlyingAssetHash(String underlyingAssetHash) { this.underlyingAssetHash = underlyingAssetHash; }

    public List<MerkleProofNode> getMerkleProofPath() { return merkleProofPath; }
    public void setMerkleProofPath(List<MerkleProofNode> path) { this.merkleProofPath = path; }

    public String getMerkleRootHash() { return merkleRootHash; }
    public void setMerkleRootHash(String hash) { this.merkleRootHash = hash; }

    public LocalDateTime getTokenCreationTimestamp() { return tokenCreationTimestamp; }
    public void setTokenCreationTimestamp(LocalDateTime timestamp) { this.tokenCreationTimestamp = timestamp; }

    public Double getTokenValueUsd() { return tokenValueUsd; }
    public void setTokenValueUsd(Double value) { this.tokenValueUsd = value; }

    public Double getFractionalOwnership() { return fractionalOwnership; }
    public void setFractionalOwnership(Double ownership) { this.fractionalOwnership = ownership; }

    public String getOwnerAddress() { return ownerAddress; }
    public void setOwnerAddress(String address) { this.ownerAddress = address; }

    public Boolean getAssetVerified() { return assetVerified; }
    public void setAssetVerified(Boolean verified) { this.assetVerified = verified; }

    public String getVerificationStatus() { return verificationStatus; }
    public void setVerificationStatus(String status) { this.verificationStatus = status; }

    public Boolean getProofValid() { return proofValid; }
    public void setProofValid(Boolean valid) { this.proofValid = valid; }

    public List<OwnershipTransfer> getOwnershipHistory() { return ownershipHistory; }
    public void setOwnershipHistory(List<OwnershipTransfer> history) { this.ownershipHistory = history; }
    public void addOwnershipTransfer(OwnershipTransfer transfer) {
        this.ownershipHistory.add(transfer);
    }

    public List<String> getComplianceCertifications() { return complianceCertifications; }
    public void setComplianceCertifications(List<String> certs) { this.complianceCertifications = certs; }
    public void addCertification(String cert) { this.complianceCertifications.add(cert); }

    public List<AuditLogEntry> getAuditTrail() { return auditTrail; }
    public void setAuditTrail(List<AuditLogEntry> trail) { this.auditTrail = trail; }
    public void addAuditEntry(AuditLogEntry entry) { this.auditTrail.add(entry); }

    public LocalDateTime getLastVerifiedTimestamp() { return lastVerifiedTimestamp; }
    public void setLastVerifiedTimestamp(LocalDateTime timestamp) { this.lastVerifiedTimestamp = timestamp; }

    public LocalDateTime getNextVerificationDue() { return nextVerificationDue; }
    public void setNextVerificationDue(LocalDateTime dueDate) { this.nextVerificationDue = dueDate; }

    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }

    @Override
    public String toString() {
        return "MerkleTokenTrace{" +
                "traceId='" + traceId + '\'' +
                ", tokenId='" + tokenId + '\'' +
                ", assetId='" + assetId + '\'' +
                ", assetType='" + assetType + '\'' +
                ", proofValid=" + proofValid +
                ", assetVerified=" + assetVerified +
                ", verificationStatus='" + verificationStatus + '\'' +
                '}';
    }

    /**
     * MerkleProofNode - Individual node in the merkle proof path
     */
    public static class MerkleProofNode {
        @JsonProperty("index")
        private Integer index;  // Position in proof path

        @JsonProperty("hash")
        private String hash;  // Node hash value

        @JsonProperty("sibling_hash")
        private String siblingHash;  // Sibling hash for verification

        @JsonProperty("direction")
        private String direction;  // LEFT or RIGHT

        public MerkleProofNode() {}

        public MerkleProofNode(Integer index, String hash, String siblingHash, String direction) {
            this.index = index;
            this.hash = hash;
            this.siblingHash = siblingHash;
            this.direction = direction;
        }

        // Getters and Setters
        public Integer getIndex() { return index; }
        public void setIndex(Integer index) { this.index = index; }

        public String getHash() { return hash; }
        public void setHash(String hash) { this.hash = hash; }

        public String getSiblingHash() { return siblingHash; }
        public void setSiblingHash(String siblingHash) { this.siblingHash = siblingHash; }

        public String getDirection() { return direction; }
        public void setDirection(String direction) { this.direction = direction; }
    }

    /**
     * OwnershipTransfer - Records token ownership changes
     */
    public static class OwnershipTransfer {
        @JsonProperty("transfer_id")
        private String transferId;

        @JsonProperty("from_address")
        private String fromAddress;

        @JsonProperty("to_address")
        private String toAddress;

        @JsonProperty("timestamp")
        private LocalDateTime timestamp;

        @JsonProperty("transaction_hash")
        private String transactionHash;

        @JsonProperty("transfer_value")
        private Double transferValue;

        @JsonProperty("ownership_percentage")
        private Double ownershipPercentage;

        public OwnershipTransfer() {}

        public OwnershipTransfer(String from, String to, Double percentage) {
            this.fromAddress = from;
            this.toAddress = to;
            this.ownershipPercentage = percentage;
            this.timestamp = LocalDateTime.now();
            this.transferId = UUID.randomUUID().toString();
        }

        // Getters and Setters
        public String getTransferId() { return transferId; }
        public void setTransferId(String id) { this.transferId = id; }

        public String getFromAddress() { return fromAddress; }
        public void setFromAddress(String addr) { this.fromAddress = addr; }

        public String getToAddress() { return toAddress; }
        public void setToAddress(String addr) { this.toAddress = addr; }

        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime ts) { this.timestamp = ts; }

        public String getTransactionHash() { return transactionHash; }
        public void setTransactionHash(String hash) { this.transactionHash = hash; }

        public Double getTransferValue() { return transferValue; }
        public void setTransferValue(Double value) { this.transferValue = value; }

        public Double getOwnershipPercentage() { return ownershipPercentage; }
        public void setOwnershipPercentage(Double percent) { this.ownershipPercentage = percent; }
    }

    /**
     * AuditLogEntry - Audit trail records for compliance
     */
    public static class AuditLogEntry {
        @JsonProperty("entry_id")
        private String entryId;

        @JsonProperty("timestamp")
        private LocalDateTime timestamp;

        @JsonProperty("action")
        private String action;  // CREATED, VERIFIED, TRANSFERRED, BURNED, etc.

        @JsonProperty("actor")
        private String actor;  // User/system that performed action

        @JsonProperty("details")
        private String details;

        @JsonProperty("status")
        private String status;  // SUCCESS, FAILED, PENDING

        public AuditLogEntry() {}

        public AuditLogEntry(String action, String actor, String details) {
            this.action = action;
            this.actor = actor;
            this.details = details;
            this.timestamp = LocalDateTime.now();
            this.entryId = UUID.randomUUID().toString();
            this.status = "SUCCESS";
        }

        // Getters and Setters
        public String getEntryId() { return entryId; }
        public void setEntryId(String id) { this.entryId = id; }

        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime ts) { this.timestamp = ts; }

        public String getAction() { return action; }
        public void setAction(String action) { this.action = action; }

        public String getActor() { return actor; }
        public void setActor(String actor) { this.actor = actor; }

        public String getDetails() { return details; }
        public void setDetails(String details) { this.details = details; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }
}
