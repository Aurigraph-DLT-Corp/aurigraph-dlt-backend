package io.aurigraph.v11.bridge.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Bridge Transfer Response Model
 * Contains transfer execution results and status information
 *
 * @author Backend Development Agent (BDA)
 * @version 1.0
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TransferResponse {

    @JsonProperty("transferId")
    private String transferId;

    @JsonProperty("status")
    private TransferStatus status; // PENDING, SIGNED, APPROVED, EXECUTING, COMPLETED, FAILED

    @JsonProperty("signaturesReceived")
    private Integer signaturesReceived;

    @JsonProperty("signaturesRequired")
    private Integer signaturesRequired;

    @JsonProperty("signatureProgress")
    private Double signatureProgress; // 0-100%

    @JsonProperty("sourceTransactionHash")
    private String sourceTransactionHash;

    @JsonProperty("targetTransactionHash")
    private String targetTransactionHash;

    @JsonProperty("sourceConfirmations")
    private Integer sourceConfirmations;

    @JsonProperty("targetConfirmations")
    private Integer targetConfirmations;

    @JsonProperty("requiredConfirmations")
    private Integer requiredConfirmations;

    @JsonProperty("amount")
    private BigDecimal amount;

    @JsonProperty("tokenSymbol")
    private String tokenSymbol;

    @JsonProperty("totalFees")
    private BigDecimal totalFees;

    @JsonProperty("gasUsed")
    private BigDecimal gasUsed;

    @JsonProperty("liquidityDeducted")
    private BigDecimal liquidityDeducted;

    @JsonProperty("estimatedCompletionTime")
    private Long estimatedCompletionTime; // milliseconds

    @JsonProperty("actualCompletionTime")
    private Long actualCompletionTime; // milliseconds

    @JsonProperty("sourceBlockNumber")
    private Long sourceBlockNumber;

    @JsonProperty("targetBlockNumber")
    private Long targetBlockNumber;

    @JsonProperty("approvalSignatures")
    private List<ApprovalSignature> approvalSignatures;

    @JsonProperty("events")
    private List<TransferEvent> events;

    @JsonProperty("errorDetails")
    private String errorDetails;

    @JsonProperty("errorCode")
    private String errorCode;

    @JsonProperty("createdAt")
    private Instant createdAt;

    @JsonProperty("updatedAt")
    private Instant updatedAt;

    @JsonProperty("completedAt")
    private Instant completedAt;

    @JsonProperty("metadata")
    private Map<String, Object> metadata;

    /**
     * Approval signature information
     */
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ApprovalSignature {
        @JsonProperty("signer")
        private String signer;

        @JsonProperty("signatureType")
        private String signatureType;

        @JsonProperty("isVerified")
        private Boolean isVerified;

        @JsonProperty("verifiedAt")
        private Instant verifiedAt;

        @JsonProperty("weight")
        private Integer weight;

        // Getters
        public String getSigner() { return signer; }
        public String getSignatureType() { return signatureType; }
        public Boolean getIsVerified() { return isVerified; }
        public Instant getVerifiedAt() { return verifiedAt; }
        public Integer getWeight() { return weight; }
    }

    /**
     * Transfer event log
     */
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class TransferEvent {
        @JsonProperty("eventType")
        private String eventType; // INITIATED, SIGNED, APPROVED, LOCKED, RELEASED, FAILED

        @JsonProperty("message")
        private String message;

        @JsonProperty("timestamp")
        private Instant timestamp;

        @JsonProperty("details")
        private Map<String, Object> details;

        // Getters
        public String getEventType() { return eventType; }
        public String getMessage() { return message; }
        public Instant getTimestamp() { return timestamp; }
        public Map<String, Object> getDetails() { return details; }
    }

    /**
     * Transfer status enum
     */
    public enum TransferStatus {
        PENDING,      // Waiting for signatures
        SIGNED,       // Signatures collected
        APPROVED,     // Multi-sig approved
        EXECUTING,    // Being executed on blockchain
        COMPLETED,    // Successfully completed
        FAILED,       // Transfer failed
        CANCELLED     // Transfer cancelled
    }

    // Getters
    public String getTransferId() { return transferId; }
    public TransferStatus getStatus() { return status; }
    public Integer getSignaturesReceived() { return signaturesReceived; }
    public Integer getSignaturesRequired() { return signaturesRequired; }
    public Double getSignatureProgress() { return signatureProgress; }
    public String getSourceTransactionHash() { return sourceTransactionHash; }
    public String getTargetTransactionHash() { return targetTransactionHash; }
    public Integer getSourceConfirmations() { return sourceConfirmations; }
    public Integer getTargetConfirmations() { return targetConfirmations; }
    public Integer getRequiredConfirmations() { return requiredConfirmations; }
    public BigDecimal getAmount() { return amount; }
    public String getTokenSymbol() { return tokenSymbol; }
    public BigDecimal getTotalFees() { return totalFees; }
    public BigDecimal getGasUsed() { return gasUsed; }
    public BigDecimal getLiquidityDeducted() { return liquidityDeducted; }
    public Long getEstimatedCompletionTime() { return estimatedCompletionTime; }
    public Long getActualCompletionTime() { return actualCompletionTime; }
    public Long getSourceBlockNumber() { return sourceBlockNumber; }
    public Long getTargetBlockNumber() { return targetBlockNumber; }
    public List<ApprovalSignature> getApprovalSignatures() { return approvalSignatures; }
    public List<TransferEvent> getEvents() { return events; }
    public String getErrorDetails() { return errorDetails; }
    public String getErrorCode() { return errorCode; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Instant getCompletedAt() { return completedAt; }
    public Map<String, Object> getMetadata() { return metadata; }

    /**
     * Check if transfer is complete
     */
    public boolean isComplete() {
        return status == TransferStatus.COMPLETED;
    }

    /**
     * Check if transfer failed
     */
    public boolean hasFailed() {
        return status == TransferStatus.FAILED || status == TransferStatus.CANCELLED;
    }

    /**
     * Check if signatures are complete
     */
    public boolean areSignaturesComplete() {
        return signaturesReceived != null && signaturesRequired != null &&
               signaturesReceived >= signaturesRequired;
    }

    /**
     * Check if transfer is ready to execute
     */
    public boolean isReadyToExecute() {
        return areSignaturesComplete() && status == TransferStatus.APPROVED;
    }

    /**
     * Add transfer event
     */
    public void addEvent(TransferEvent event) {
        if (events == null) {
            events = new java.util.ArrayList<>();
        }
        events.add(event);
    }

    /**
     * Add approval signature
     */
    public void addApprovalSignature(ApprovalSignature signature) {
        if (approvalSignatures == null) {
            approvalSignatures = new java.util.ArrayList<>();
        }
        approvalSignatures.add(signature);
    }

    // Setters (explicit to work around Lombok annotation processor issues)
    public void setTransferId(String transferId) { this.transferId = transferId; }
    public void setStatus(TransferStatus status) { this.status = status; }
    public void setSignaturesReceived(Integer signaturesReceived) { this.signaturesReceived = signaturesReceived; }
    public void setSignaturesRequired(Integer signaturesRequired) { this.signaturesRequired = signaturesRequired; }
    public void setSignatureProgress(Double signatureProgress) { this.signatureProgress = signatureProgress; }
    public void setSourceTransactionHash(String sourceTransactionHash) { this.sourceTransactionHash = sourceTransactionHash; }
    public void setTargetTransactionHash(String targetTransactionHash) { this.targetTransactionHash = targetTransactionHash; }
    public void setSourceConfirmations(Integer sourceConfirmations) { this.sourceConfirmations = sourceConfirmations; }
    public void setTargetConfirmations(Integer targetConfirmations) { this.targetConfirmations = targetConfirmations; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public void setTotalFees(BigDecimal totalFees) { this.totalFees = totalFees; }
    public void setGasUsed(BigDecimal gasUsed) { this.gasUsed = gasUsed; }
    public void setLiquidityDeducted(BigDecimal liquidityDeducted) { this.liquidityDeducted = liquidityDeducted; }
    public void setEstimatedCompletionTime(Long estimatedCompletionTime) { this.estimatedCompletionTime = estimatedCompletionTime; }
    public void setActualCompletionTime(Long actualCompletionTime) { this.actualCompletionTime = actualCompletionTime; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }
    public void setErrorDetails(String errorDetails) { this.errorDetails = errorDetails; }
    public void setApprovalSignatures(List<ApprovalSignature> approvalSignatures) { this.approvalSignatures = approvalSignatures; }
    public void setEvents(List<TransferEvent> events) { this.events = events; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
