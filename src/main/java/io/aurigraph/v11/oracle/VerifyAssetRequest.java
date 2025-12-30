package io.aurigraph.v11.oracle;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.math.BigDecimal;

/**
 * Verify Asset Request DTO
 * Request body for oracle verification API
 *
 * @author API Development Agent (ADA) - Sprint 16
 * @version 11.0.0
 * @sprint Sprint 16 - Oracle Verification REST API (AV11-496)
 */
@Schema(description = "Request to verify an asset value using oracle consensus")
public class VerifyAssetRequest {

    @JsonProperty("asset_id")
    @Schema(description = "Unique identifier of the asset to verify", required = true, example = "ASSET-001")
    private String assetId;

    @JsonProperty("claimed_value")
    @Schema(description = "The claimed value of the asset to be verified", required = true, example = "50000.00")
    private BigDecimal claimedValue;

    @JsonProperty("tolerance_percentage")
    @Schema(description = "Acceptable variance percentage (optional, default: 5.0)", example = "5.0")
    private Double tolerancePercentage;

    // Constructors
    public VerifyAssetRequest() {
    }

    public VerifyAssetRequest(String assetId, BigDecimal claimedValue) {
        this.assetId = assetId;
        this.claimedValue = claimedValue;
    }

    public VerifyAssetRequest(String assetId, BigDecimal claimedValue, Double tolerancePercentage) {
        this.assetId = assetId;
        this.claimedValue = claimedValue;
        this.tolerancePercentage = tolerancePercentage;
    }

    // Getters and Setters
    public String getAssetId() {
        return assetId;
    }

    public void setAssetId(String assetId) {
        this.assetId = assetId;
    }

    public BigDecimal getClaimedValue() {
        return claimedValue;
    }

    public void setClaimedValue(BigDecimal claimedValue) {
        this.claimedValue = claimedValue;
    }

    public Double getTolerancePercentage() {
        return tolerancePercentage;
    }

    public void setTolerancePercentage(Double tolerancePercentage) {
        this.tolerancePercentage = tolerancePercentage;
    }

    /**
     * Validation method
     */
    public boolean isValid() {
        return assetId != null && !assetId.trim().isEmpty()
            && claimedValue != null && claimedValue.compareTo(BigDecimal.ZERO) > 0;
    }

    @Override
    public String toString() {
        return String.format("VerifyAssetRequest{assetId='%s', claimedValue=%s, tolerancePercentage=%s}",
            assetId, claimedValue, tolerancePercentage);
    }
}
