package io.aurigraph.v11.contracts.models;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Asset Tokenization Request
 *
 * Request model for tokenizing real-world assets (RWA) on Aurigraph ActiveContracts platform.
 * Supports tokenization of:
 * - Carbon Credits
 * - Real Estate
 * - Financial Assets (Securities, Bonds)
 * - Supply Chain Goods
 * - Intellectual Property
 *
 * @version 11.3.0
 * @since 2025-10-13
 */
public class AssetTokenizationRequest {

    @JsonProperty("assetId")
    private String assetId; // Unique asset identifier

    @JsonProperty("assetType")
    private String assetType; // CarbonCredit, RealEstate, Financial, SupplyChain, IP

    @JsonProperty("assetName")
    private String assetName; // Human-readable asset name

    @JsonProperty("assetDescription")
    private String assetDescription; // Detailed asset description

    @JsonProperty("valuation")
    private double valuation; // Asset valuation in USD

    @JsonProperty("tokenSupply")
    private long tokenSupply; // Number of tokens to create

    @JsonProperty("tokenPrice")
    private double tokenPrice; // Price per token in USD

    @JsonProperty("tokenSymbol")
    private String tokenSymbol; // Token symbol (e.g., "CC" for carbon credit)

    @JsonProperty("tokenName")
    private String tokenName; // Token name

    @JsonProperty("metadata")
    private java.util.Map<String, String> metadata; // Additional metadata

    // Default constructor
    public AssetTokenizationRequest() {
        this.metadata = new java.util.HashMap<>();
    }

    // Builder pattern
    public static Builder builder() {
        return new Builder();
    }

    // Getters and setters
    public String getAssetId() {
        return assetId;
    }

    public void setAssetId(String assetId) {
        this.assetId = assetId;
    }

    public String getAssetType() {
        return assetType;
    }

    public void setAssetType(String assetType) {
        this.assetType = assetType;
    }

    public String getAssetName() {
        return assetName;
    }

    public void setAssetName(String assetName) {
        this.assetName = assetName;
    }

    public String getAssetDescription() {
        return assetDescription;
    }

    public void setAssetDescription(String assetDescription) {
        this.assetDescription = assetDescription;
    }

    public double getValuation() {
        return valuation;
    }

    public void setValuation(double valuation) {
        this.valuation = valuation;
    }

    public long getTokenSupply() {
        return tokenSupply;
    }

    public void setTokenSupply(long tokenSupply) {
        this.tokenSupply = tokenSupply;
    }

    public double getTokenPrice() {
        return tokenPrice;
    }

    public void setTokenPrice(double tokenPrice) {
        this.tokenPrice = tokenPrice;
    }

    public String getTokenSymbol() {
        return tokenSymbol;
    }

    public void setTokenSymbol(String tokenSymbol) {
        this.tokenSymbol = tokenSymbol;
    }

    public String getTokenName() {
        return tokenName;
    }

    public void setTokenName(String tokenName) {
        this.tokenName = tokenName;
    }

    public java.util.Map<String, String> getMetadata() {
        return metadata;
    }

    public void setMetadata(java.util.Map<String, String> metadata) {
        this.metadata = metadata;
    }

    // Builder class
    public static class Builder {
        private AssetTokenizationRequest request = new AssetTokenizationRequest();

        public Builder assetId(String assetId) {
            request.assetId = assetId;
            return this;
        }

        public Builder assetType(String assetType) {
            request.assetType = assetType;
            return this;
        }

        public Builder assetName(String assetName) {
            request.assetName = assetName;
            return this;
        }

        public Builder assetDescription(String assetDescription) {
            request.assetDescription = assetDescription;
            return this;
        }

        public Builder valuation(double valuation) {
            request.valuation = valuation;
            return this;
        }

        public Builder tokenSupply(long tokenSupply) {
            request.tokenSupply = tokenSupply;
            return this;
        }

        public Builder tokenPrice(double tokenPrice) {
            request.tokenPrice = tokenPrice;
            return this;
        }

        public Builder tokenSymbol(String tokenSymbol) {
            request.tokenSymbol = tokenSymbol;
            return this;
        }

        public Builder tokenName(String tokenName) {
            request.tokenName = tokenName;
            return this;
        }

        public Builder metadata(java.util.Map<String, String> metadata) {
            request.metadata = metadata;
            return this;
        }

        public AssetTokenizationRequest build() {
            // Validate required fields
            if (request.assetId == null || request.assetId.trim().isEmpty()) {
                throw new IllegalArgumentException("Asset ID is required");
            }
            if (request.assetType == null || request.assetType.trim().isEmpty()) {
                throw new IllegalArgumentException("Asset type is required");
            }
            if (request.valuation <= 0) {
                throw new IllegalArgumentException("Valuation must be positive");
            }
            if (request.tokenSupply <= 0) {
                throw new IllegalArgumentException("Token supply must be positive");
            }
            if (request.tokenPrice <= 0) {
                throw new IllegalArgumentException("Token price must be positive");
            }

            return request;
        }
    }

    @Override
    public String toString() {
        return String.format("AssetTokenizationRequest{assetId='%s', type='%s', valuation=%.2f, tokens=%d}",
                assetId, assetType, valuation, tokenSupply);
    }
}
