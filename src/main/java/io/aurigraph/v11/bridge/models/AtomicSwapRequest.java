package io.aurigraph.v11.bridge.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Atomic Swap (HTLC) Request Model
 * Contains all information needed to initiate a hash-time-locked contract swap
 *
 * @author Backend Development Agent (BDA)
 * @version 1.0
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AtomicSwapRequest {

    @JsonProperty("swapId")
    private String swapId; // Unique swap ID

    @JsonProperty("initiator")
    private String initiator; // Address initiating the swap

    @JsonProperty("counterparty")
    private String counterparty; // Address of counterparty

    @JsonProperty("sourceChain")
    private String sourceChain;

    @JsonProperty("targetChain")
    private String targetChain;

    @JsonProperty("tokenIn")
    private String tokenIn; // Source token symbol

    @JsonProperty("tokenOut")
    private String tokenOut; // Target token symbol

    @JsonProperty("amountIn")
    private BigDecimal amountIn; // Amount to send

    @JsonProperty("amountOut")
    private BigDecimal amountOut; // Expected amount to receive

    @JsonProperty("sourceAddress")
    private String sourceAddress; // Source user address

    @JsonProperty("targetAddress")
    private String targetAddress; // Target user address

    @JsonProperty("hashAlgo")
    private String hashAlgo; // SHA256, SHA3, BLAKE2B

    @JsonProperty("hashLock")
    private String hashLock; // Hash of secret (hex encoded)

    @JsonProperty("timelock")
    private Long timelock; // Lock expiry time (milliseconds from now, default 5 minutes)

    @JsonProperty("refundAddress")
    private String refundAddress; // Address to refund if swap fails

    @JsonProperty("nonce")
    private Long nonce;

    @JsonProperty("timestamp")
    private Long timestamp;

    @JsonProperty("metadata")
    private String metadata;

    // Getters
    public String getSwapId() { return swapId; }
    public String getInitiator() { return initiator; }
    public String getCounterparty() { return counterparty; }
    public String getSourceChain() { return sourceChain; }
    public String getTargetChain() { return targetChain; }
    public String getTokenIn() { return tokenIn; }
    public String getTokenOut() { return tokenOut; }
    public BigDecimal getAmountIn() { return amountIn; }
    public BigDecimal getAmountOut() { return amountOut; }
    public String getSourceAddress() { return sourceAddress; }
    public String getTargetAddress() { return targetAddress; }
    public String getHashAlgo() { return hashAlgo; }
    public String getHashLock() { return hashLock; }
    public Long getTimelock() { return timelock; }
    public String getRefundAddress() { return refundAddress; }
    public Long getNonce() { return nonce; }
    public Long getTimestamp() { return timestamp; }
    public String getMetadata() { return metadata; }

    /**
     * Get validation errors
     */
    public List<String> getValidationErrors() {
        List<String> errors = new java.util.ArrayList<>();

        if (swapId == null || swapId.isEmpty()) {
            errors.add("Swap ID is required");
        }
        if (initiator == null || initiator.isEmpty()) {
            errors.add("Initiator address is required");
        }
        if (counterparty == null || counterparty.isEmpty()) {
            errors.add("Counterparty address is required");
        }
        if (amountIn == null || amountIn.compareTo(BigDecimal.ZERO) <= 0) {
            errors.add("Amount in must be greater than zero");
        }
        if (amountOut == null || amountOut.compareTo(BigDecimal.ZERO) <= 0) {
            errors.add("Amount out must be greater than zero");
        }
        if (hashLock == null || hashLock.isEmpty()) {
            errors.add("Hash lock is required");
        }
        if (hashAlgo == null || hashAlgo.isEmpty()) {
            errors.add("Hash algorithm is required");
        }
        if (timelock == null || timelock <= 0) {
            errors.add("Timelock duration must be greater than zero");
        }
        if (initiator.equals(counterparty)) {
            errors.add("Initiator and counterparty must be different");
        }

        return errors;
    }

    /**
     * Get effective timelock (expiry time in milliseconds)
     */
    public long getExpiryTime() {
        return System.currentTimeMillis() + (timelock != null ? timelock : 300000); // Default 5 minutes
    }

    /**
     * Check if swap has expired
     */
    public boolean isExpired() {
        return System.currentTimeMillis() > getExpiryTime();
    }
}
