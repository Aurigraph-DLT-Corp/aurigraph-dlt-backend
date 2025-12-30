package io.aurigraph.v11.portal.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.List;

public class TokenDTO {
    @JsonProperty("token_id")
    private String tokenId;
    @JsonProperty("name")
    private String name;
    @JsonProperty("symbol")
    private String symbol;
    @JsonProperty("decimals")
    private Integer decimals;
    @JsonProperty("total_supply")
    private String totalSupply;
    @JsonProperty("circulating_supply")
    private String circulatingSupply;
    @JsonProperty("contract_address")
    private String contractAddress;
    @JsonProperty("type")
    private String type;
    @JsonProperty("price")
    private String price;
    @JsonProperty("price_change24h")
    private Double priceChange24h;
    @JsonProperty("market_cap")
    private String marketCap;
    @JsonProperty("volume24h")
    private String volume24h;
    @JsonProperty("holders")
    private Integer holders;
    @JsonProperty("created_at")
    private Instant createdAt;
    @JsonProperty("status")
    private String status;

    public TokenDTO() {}

    private TokenDTO(Builder builder) {
        this.tokenId = builder.tokenId;
        this.name = builder.name;
        this.symbol = builder.symbol;
        this.decimals = builder.decimals;
        this.totalSupply = builder.totalSupply;
        this.circulatingSupply = builder.circulatingSupply;
        this.contractAddress = builder.contractAddress;
        this.type = builder.type;
        this.price = builder.price;
        this.priceChange24h = builder.priceChange24h;
        this.marketCap = builder.marketCap;
        this.volume24h = builder.volume24h;
        this.holders = builder.holders;
        this.createdAt = builder.createdAt;
        this.status = builder.status;
    }

    public String getTokenId() { return tokenId; }
    public String getName() { return name; }
    public String getSymbol() { return symbol; }
    public Integer getDecimals() { return decimals; }
    public String getTotalSupply() { return totalSupply; }
    public String getCirculatingSupply() { return circulatingSupply; }
    public String getContractAddress() { return contractAddress; }
    public String getType() { return type; }
    public String getPrice() { return price; }
    public Double getPriceChange24h() { return priceChange24h; }
    public String getMarketCap() { return marketCap; }
    public String getVolume24h() { return volume24h; }
    public Integer getHolders() { return holders; }
    public Instant getCreatedAt() { return createdAt; }
    public String getStatus() { return status; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String tokenId;
        private String name;
        private String symbol;
        private Integer decimals;
        private String totalSupply;
        private String circulatingSupply;
        private String contractAddress;
        private String type;
        private String price;
        private Double priceChange24h;
        private String marketCap;
        private String volume24h;
        private Integer holders;
        private Instant createdAt;
        private String status;

        public Builder tokenId(String tokenId) { this.tokenId = tokenId; return this; }
        public Builder name(String name) { this.name = name; return this; }
        public Builder symbol(String symbol) { this.symbol = symbol; return this; }
        public Builder decimals(Integer decimals) { this.decimals = decimals; return this; }
        public Builder totalSupply(String totalSupply) { this.totalSupply = totalSupply; return this; }
        public Builder circulatingSupply(String circulatingSupply) { this.circulatingSupply = circulatingSupply; return this; }
        public Builder contractAddress(String contractAddress) { this.contractAddress = contractAddress; return this; }
        public Builder type(String type) { this.type = type; return this; }
        public Builder price(String price) { this.price = price; return this; }
        public Builder priceChange24h(Double priceChange24h) { this.priceChange24h = priceChange24h; return this; }
        public Builder marketCap(String marketCap) { this.marketCap = marketCap; return this; }
        public Builder volume24h(String volume24h) { this.volume24h = volume24h; return this; }
        public Builder holders(Integer holders) { this.holders = holders; return this; }
        public Builder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }
        public Builder status(String status) { this.status = status; return this; }

        public TokenDTO build() { return new TokenDTO(this); }
    }
}
