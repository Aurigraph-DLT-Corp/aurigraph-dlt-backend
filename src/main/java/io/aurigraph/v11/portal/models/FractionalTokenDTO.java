package io.aurigraph.v11.portal.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.List;

public class FractionalTokenDTO {
    @JsonProperty("fractional_id")
    private String fractionalId;
    @JsonProperty("original_token_id")
    private String originalTokenId;
    @JsonProperty("fraction_value")
    private String fractionValue;
    @JsonProperty("total_fractions")
    private String totalFractions;
    @JsonProperty("circulating_fractions")
    private String circulatingFractions;
    @JsonProperty("min_purchase_unit")
    private Integer minPurchaseUnit;
    @JsonProperty("transferable")
    private Boolean transferable;
    @JsonProperty("tradable_on")
    private String tradableOn;
    @JsonProperty("created_at")
    private Instant createdAt;
    @JsonProperty("status")
    private String status;

    public FractionalTokenDTO() {}

    private FractionalTokenDTO(Builder builder) {
        this.fractionalId = builder.fractionalId;
        this.originalTokenId = builder.originalTokenId;
        this.fractionValue = builder.fractionValue;
        this.totalFractions = builder.totalFractions;
        this.circulatingFractions = builder.circulatingFractions;
        this.minPurchaseUnit = builder.minPurchaseUnit;
        this.transferable = builder.transferable;
        this.tradableOn = builder.tradableOn;
        this.createdAt = builder.createdAt;
        this.status = builder.status;
    }

    public String getFractionalId() { return fractionalId; }
    public String getOriginalTokenId() { return originalTokenId; }
    public String getFractionValue() { return fractionValue; }
    public String getTotalFractions() { return totalFractions; }
    public String getCirculatingFractions() { return circulatingFractions; }
    public Integer getMinPurchaseUnit() { return minPurchaseUnit; }
    public Boolean getTransferable() { return transferable; }
    public String getTradableOn() { return tradableOn; }
    public Instant getCreatedAt() { return createdAt; }
    public String getStatus() { return status; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String fractionalId;
        private String originalTokenId;
        private String fractionValue;
        private String totalFractions;
        private String circulatingFractions;
        private Integer minPurchaseUnit;
        private Boolean transferable;
        private String tradableOn;
        private Instant createdAt;
        private String status;

        public Builder fractionalId(String fractionalId) { this.fractionalId = fractionalId; return this; }
        public Builder originalTokenId(String originalTokenId) { this.originalTokenId = originalTokenId; return this; }
        public Builder fractionValue(String fractionValue) { this.fractionValue = fractionValue; return this; }
        public Builder totalFractions(String totalFractions) { this.totalFractions = totalFractions; return this; }
        public Builder circulatingFractions(String circulatingFractions) { this.circulatingFractions = circulatingFractions; return this; }
        public Builder minPurchaseUnit(Integer minPurchaseUnit) { this.minPurchaseUnit = minPurchaseUnit; return this; }
        public Builder transferable(Boolean transferable) { this.transferable = transferable; return this; }
        public Builder tradableOn(String tradableOn) { this.tradableOn = tradableOn; return this; }
        public Builder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }
        public Builder status(String status) { this.status = status; return this; }

        public FractionalTokenDTO build() { return new FractionalTokenDTO(this); }
    }
}
