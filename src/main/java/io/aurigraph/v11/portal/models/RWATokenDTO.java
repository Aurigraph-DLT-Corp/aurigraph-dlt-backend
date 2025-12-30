package io.aurigraph.v11.portal.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.List;

public class RWATokenDTO {
    @JsonProperty("token_id")
    private String tokenId;
    @JsonProperty("name")
    private String name;
    @JsonProperty("symbol")
    private String symbol;
    @JsonProperty("asset_type")
    private String assetType;
    @JsonProperty("underlying_asset_value")
    private String underlyingAssetValue;
    @JsonProperty("tokenized_value")
    private String tokenizedValue;
    @JsonProperty("total_tokens")
    private String totalTokens;
    @JsonProperty("fractional_tokens")
    private String fractionalTokens;
    @JsonProperty("price_per_token")
    private String pricePerToken;
    @JsonProperty("token_holders")
    private Integer tokenHolders;
    @JsonProperty("registry_address")
    private String registryAddress;
    @JsonProperty("merkle_root")
    private String merkleRoot;
    @JsonProperty("verification_status")
    private String verificationStatus;
    @JsonProperty("audited_by")
    private String auditedBy;
    @JsonProperty("last_audit_date")
    private Instant lastAuditDate;
    @JsonProperty("dividend_yield")
    private Double dividendYield;
    @JsonProperty("last_dividend_payment")
    private Instant lastDividendPayment;
    @JsonProperty("next_dividend_date")
    private Instant nextDividendDate;
    @JsonProperty("error")
    private String error;

    public RWATokenDTO() {}

    private RWATokenDTO(Builder builder) {
        this.tokenId = builder.tokenId;
        this.name = builder.name;
        this.symbol = builder.symbol;
        this.assetType = builder.assetType;
        this.underlyingAssetValue = builder.underlyingAssetValue;
        this.tokenizedValue = builder.tokenizedValue;
        this.totalTokens = builder.totalTokens;
        this.fractionalTokens = builder.fractionalTokens;
        this.pricePerToken = builder.pricePerToken;
        this.tokenHolders = builder.tokenHolders;
        this.registryAddress = builder.registryAddress;
        this.merkleRoot = builder.merkleRoot;
        this.verificationStatus = builder.verificationStatus;
        this.auditedBy = builder.auditedBy;
        this.lastAuditDate = builder.lastAuditDate;
        this.dividendYield = builder.dividendYield;
        this.lastDividendPayment = builder.lastDividendPayment;
        this.nextDividendDate = builder.nextDividendDate;
        this.error = builder.error;
    }

    public String getTokenId() { return tokenId; }
    public String getName() { return name; }
    public String getSymbol() { return symbol; }
    public String getAssetType() { return assetType; }
    public String getUnderlyingAssetValue() { return underlyingAssetValue; }
    public String getTokenizedValue() { return tokenizedValue; }
    public String getTotalTokens() { return totalTokens; }
    public String getFractionalTokens() { return fractionalTokens; }
    public String getPricePerToken() { return pricePerToken; }
    public Integer getTokenHolders() { return tokenHolders; }
    public String getRegistryAddress() { return registryAddress; }
    public String getMerkleRoot() { return merkleRoot; }
    public String getVerificationStatus() { return verificationStatus; }
    public String getAuditedBy() { return auditedBy; }
    public Instant getLastAuditDate() { return lastAuditDate; }
    public Double getDividendYield() { return dividendYield; }
    public Instant getLastDividendPayment() { return lastDividendPayment; }
    public Instant getNextDividendDate() { return nextDividendDate; }
    public String getError() { return error; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String tokenId;
        private String name;
        private String symbol;
        private String assetType;
        private String underlyingAssetValue;
        private String tokenizedValue;
        private String totalTokens;
        private String fractionalTokens;
        private String pricePerToken;
        private Integer tokenHolders;
        private String registryAddress;
        private String merkleRoot;
        private String verificationStatus;
        private String auditedBy;
        private Instant lastAuditDate;
        private Double dividendYield;
        private Instant lastDividendPayment;
        private Instant nextDividendDate;
        private String error;

        public Builder tokenId(String tokenId) { this.tokenId = tokenId; return this; }
        public Builder name(String name) { this.name = name; return this; }
        public Builder symbol(String symbol) { this.symbol = symbol; return this; }
        public Builder assetType(String assetType) { this.assetType = assetType; return this; }
        public Builder underlyingAssetValue(String underlyingAssetValue) { this.underlyingAssetValue = underlyingAssetValue; return this; }
        public Builder tokenizedValue(String tokenizedValue) { this.tokenizedValue = tokenizedValue; return this; }
        public Builder totalTokens(String totalTokens) { this.totalTokens = totalTokens; return this; }
        public Builder fractionalTokens(String fractionalTokens) { this.fractionalTokens = fractionalTokens; return this; }
        public Builder pricePerToken(String pricePerToken) { this.pricePerToken = pricePerToken; return this; }
        public Builder tokenHolders(Integer tokenHolders) { this.tokenHolders = tokenHolders; return this; }
        public Builder registryAddress(String registryAddress) { this.registryAddress = registryAddress; return this; }
        public Builder merkleRoot(String merkleRoot) { this.merkleRoot = merkleRoot; return this; }
        public Builder verificationStatus(String verificationStatus) { this.verificationStatus = verificationStatus; return this; }
        public Builder auditedBy(String auditedBy) { this.auditedBy = auditedBy; return this; }
        public Builder lastAuditDate(Instant lastAuditDate) { this.lastAuditDate = lastAuditDate; return this; }
        public Builder dividendYield(Double dividendYield) { this.dividendYield = dividendYield; return this; }
        public Builder lastDividendPayment(Instant lastDividendPayment) { this.lastDividendPayment = lastDividendPayment; return this; }
        public Builder nextDividendDate(Instant nextDividendDate) { this.nextDividendDate = nextDividendDate; return this; }
        public Builder error(String error) { this.error = error; return this; }

        public RWATokenDTO build() { return new RWATokenDTO(this); }
    }
}
