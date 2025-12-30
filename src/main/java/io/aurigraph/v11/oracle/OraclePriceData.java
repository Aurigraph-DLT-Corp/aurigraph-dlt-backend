package io.aurigraph.v11.oracle;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Oracle Price Data DTO
 * Represents price data fetched from an individual oracle
 *
 * @author Aurigraph V11 - Backend Development Agent
 * @version 11.0.0
 * @sprint Sprint 16 - Oracle Verification System (AV11-483)
 */
public class OraclePriceData {

    private String oracleId;
    private String oracleName;
    private String provider;
    private String assetId; // Asset ID for signature verification
    private BigDecimal price;
    private Instant timestamp;
    private String signature;
    private boolean signatureValid;
    private long responseTimeMs;
    private String status; // "success", "failed", "timeout"
    private String errorMessage;

    // Constructors
    public OraclePriceData() {
        this.timestamp = Instant.now();
    }

    public OraclePriceData(String oracleId, String oracleName, String provider) {
        this();
        this.oracleId = oracleId;
        this.oracleName = oracleName;
        this.provider = provider;
    }

    // Getters and Setters
    public String getOracleId() {
        return oracleId;
    }

    public void setOracleId(String oracleId) {
        this.oracleId = oracleId;
    }

    public String getOracleName() {
        return oracleName;
    }

    public void setOracleName(String oracleName) {
        this.oracleName = oracleName;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getAssetId() {
        return assetId;
    }

    public void setAssetId(String assetId) {
        this.assetId = assetId;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public boolean isSignatureValid() {
        return signatureValid;
    }

    public void setSignatureValid(boolean signatureValid) {
        this.signatureValid = signatureValid;
    }

    public long getResponseTimeMs() {
        return responseTimeMs;
    }

    public void setResponseTimeMs(long responseTimeMs) {
        this.responseTimeMs = responseTimeMs;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    /**
     * Check if this oracle data is valid for consensus calculation
     */
    public boolean isValidForConsensus() {
        return "success".equals(status)
            && price != null
            && price.compareTo(BigDecimal.ZERO) > 0
            && signatureValid;
    }

    @Override
    public String toString() {
        return String.format("OraclePriceData{oracleId='%s', provider='%s', price=%s, status='%s', signatureValid=%b}",
            oracleId, provider, price, status, signatureValid);
    }
}
