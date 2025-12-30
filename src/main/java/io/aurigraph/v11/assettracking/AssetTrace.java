package io.aurigraph.v11.assettracking;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Asset Trace Data Transfer Object
 *
 * Represents the complete lifecycle and traceability information for a tokenized asset.
 * Includes ownership history, audit trail, compliance status, and metadata.
 *
 * @version 1.0.0
 * @author Aurigraph V11 Development Team
 */
public class AssetTrace {

    @JsonProperty("traceId")
    private String traceId;

    @JsonProperty("assetId")
    private String assetId;

    @JsonProperty("assetName")
    private String assetName;

    @JsonProperty("assetType")
    private String assetType;

    @JsonProperty("valuation")
    private Double valuation;

    @JsonProperty("currencyCode")
    private String currencyCode;

    @JsonProperty("currentOwner")
    private String currentOwner;

    @JsonProperty("ownershipHistory")
    private List<OwnershipRecord> ownershipHistory;

    @JsonProperty("auditTrail")
    private List<AuditTrailEntry> auditTrail;

    @JsonProperty("complianceStatus")
    private String complianceStatus;

    @JsonProperty("lastUpdated")
    private Instant lastUpdated;

    @JsonProperty("metadata")
    private Map<String, Object> metadata;

    // Constructors
    public AssetTrace() {
        this.metadata = new ConcurrentHashMap<>();
    }

    public AssetTrace(String traceId, String assetId, String assetName, String assetType,
                      Double valuation, String currencyCode) {
        this.traceId = traceId;
        this.assetId = assetId;
        this.assetName = assetName;
        this.assetType = assetType;
        this.valuation = valuation;
        this.currencyCode = currencyCode;
        this.metadata = new ConcurrentHashMap<>();
    }

    // Getters and Setters
    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public String getAssetId() {
        return assetId;
    }

    public void setAssetId(String assetId) {
        this.assetId = assetId;
    }

    public String getAssetName() {
        return assetName;
    }

    public void setAssetName(String assetName) {
        this.assetName = assetName;
    }

    public String getAssetType() {
        return assetType;
    }

    public void setAssetType(String assetType) {
        this.assetType = assetType;
    }

    public Double getValuation() {
        return valuation;
    }

    public void setValuation(Double valuation) {
        this.valuation = valuation;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    public String getCurrentOwner() {
        return currentOwner;
    }

    public void setCurrentOwner(String currentOwner) {
        this.currentOwner = currentOwner;
    }

    public List<OwnershipRecord> getOwnershipHistory() {
        return ownershipHistory;
    }

    public void setOwnershipHistory(List<OwnershipRecord> ownershipHistory) {
        this.ownershipHistory = ownershipHistory;
    }

    public List<AuditTrailEntry> getAuditTrail() {
        return auditTrail;
    }

    public void setAuditTrail(List<AuditTrailEntry> auditTrail) {
        this.auditTrail = auditTrail;
    }

    public String getComplianceStatus() {
        return complianceStatus;
    }

    public void setComplianceStatus(String complianceStatus) {
        this.complianceStatus = complianceStatus;
    }

    public Instant getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(Instant lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    @Override
    public String toString() {
        return "AssetTrace{" +
                "traceId='" + traceId + '\'' +
                ", assetId='" + assetId + '\'' +
                ", assetName='" + assetName + '\'' +
                ", assetType='" + assetType + '\'' +
                ", valuation=" + valuation +
                ", currencyCode='" + currencyCode + '\'' +
                ", currentOwner='" + currentOwner + '\'' +
                ", complianceStatus='" + complianceStatus + '\'' +
                ", lastUpdated=" + lastUpdated +
                ", ownershipHistory.size=" + (ownershipHistory != null ? ownershipHistory.size() : 0) +
                ", auditTrail.size=" + (auditTrail != null ? auditTrail.size() : 0) +
                '}';
    }
}
