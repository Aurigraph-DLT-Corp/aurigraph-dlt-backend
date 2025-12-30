package io.aurigraph.v11.contracts.rwa;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

/**
 * Asset Digital Twin Model
 * Represents a digital representation of real-world assets with blockchain verification
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AssetDigitalTwin {

    @JsonProperty("assetId")
    private String assetId;

    @JsonProperty("name")
    private String name;

    @JsonProperty("description")
    private String description;

    @JsonProperty("assetType")
    private String assetType;

    @JsonProperty("physicalLocation")
    private String physicalLocation;

    @JsonProperty("value")
    private BigDecimal value;

    @JsonProperty("currency")
    private String currency;

    @JsonProperty("owner")
    private String owner;

    @JsonProperty("issuer")
    private String issuer;

    @JsonProperty("createdAt")
    private Instant createdAt;

    @JsonProperty("updatedAt")
    private Instant updatedAt;

    @JsonProperty("metadata")
    private Map<String, Object> metadata;

    /**
     * Get asset summary
     */
    public String getAssetSummary() {
        return String.format("%s (%s): %s %s", name, assetType, value, currency);
    }

    /**
     * Get the digital twin ID for this asset
     */
    public String getTwinId() {
        return "DT_" + assetId + "_" + System.currentTimeMillis();
    }

    /**
     * Record an ownership change for this digital twin
     */
    public void recordOwnershipChange(String newOwner, String timestamp, BigDecimal transferAmount) {
        this.owner = newOwner;
        this.updatedAt = Instant.now();
        if (this.metadata == null) {
            this.metadata = new java.util.HashMap<>();
        }
        this.metadata.put("lastOwnershipChange", timestamp);
        this.metadata.put("lastTransferAmount", transferAmount);
    }
}
