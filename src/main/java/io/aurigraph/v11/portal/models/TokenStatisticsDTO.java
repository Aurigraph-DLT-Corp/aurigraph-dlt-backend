package io.aurigraph.v11.portal.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.List;

public class TokenStatisticsDTO {
    @JsonProperty("total_tokens")
    private Integer totalTokens;
    @JsonProperty("active_tokens")
    private Integer activeTokens;
    @JsonProperty("total_token_supply")
    private String totalTokenSupply;
    @JsonProperty("total_token_value")
    private String totalTokenValue;
    @JsonProperty("native_token_price")
    private String nativeTokenPrice;
    @JsonProperty("governance_token_price")
    private String governanceTokenPrice;
    @JsonProperty("stablecoin_value")
    private String stablecoinValue;
    @JsonProperty("rwa_token_value")
    private String rwaTokenValue;
    @JsonProperty("top_token_by_market_cap")
    private String topTokenByMarketCap;
    @JsonProperty("top_token_by_volume")
    private String topTokenByVolume;
    @JsonProperty("average_token_price")
    private String averageTokenPrice;
    @JsonProperty("price_volatility")
    private Double priceVolatility;
    @JsonProperty("total_token_holders")
    private Integer totalTokenHolders;
    @JsonProperty("token_created_last24h")
    private Integer tokenCreatedLast24h;
    @JsonProperty("token_burned_last24h")
    private Integer tokenBurnedLast24h;
    @JsonProperty("token_transfers_last24h")
    private Integer tokenTransfersLast24h;
    @JsonProperty("total_token_value24h_change")
    private Double totalTokenValue24hChange;
    @JsonProperty("governance_token_holders")
    private Integer governanceTokenHolders;
    @JsonProperty("rwa_token_count")
    private Integer rwaTokenCount;
    @JsonProperty("rwa_token_holders")
    private Integer rwaTokenHolders;
    @JsonProperty("fractional_token_count")
    private Integer fractionalTokenCount;
    @JsonProperty("error")
    private String error;

    public TokenStatisticsDTO() {}

    private TokenStatisticsDTO(Builder builder) {
        this.totalTokens = builder.totalTokens;
        this.activeTokens = builder.activeTokens;
        this.totalTokenSupply = builder.totalTokenSupply;
        this.totalTokenValue = builder.totalTokenValue;
        this.nativeTokenPrice = builder.nativeTokenPrice;
        this.governanceTokenPrice = builder.governanceTokenPrice;
        this.stablecoinValue = builder.stablecoinValue;
        this.rwaTokenValue = builder.rwaTokenValue;
        this.topTokenByMarketCap = builder.topTokenByMarketCap;
        this.topTokenByVolume = builder.topTokenByVolume;
        this.averageTokenPrice = builder.averageTokenPrice;
        this.priceVolatility = builder.priceVolatility;
        this.totalTokenHolders = builder.totalTokenHolders;
        this.tokenCreatedLast24h = builder.tokenCreatedLast24h;
        this.tokenBurnedLast24h = builder.tokenBurnedLast24h;
        this.tokenTransfersLast24h = builder.tokenTransfersLast24h;
        this.totalTokenValue24hChange = builder.totalTokenValue24hChange;
        this.governanceTokenHolders = builder.governanceTokenHolders;
        this.rwaTokenCount = builder.rwaTokenCount;
        this.rwaTokenHolders = builder.rwaTokenHolders;
        this.fractionalTokenCount = builder.fractionalTokenCount;
        this.error = builder.error;
    }

    public Integer getTotalTokens() { return totalTokens; }
    public Integer getActiveTokens() { return activeTokens; }
    public String getTotalTokenSupply() { return totalTokenSupply; }
    public String getTotalTokenValue() { return totalTokenValue; }
    public String getNativeTokenPrice() { return nativeTokenPrice; }
    public String getGovernanceTokenPrice() { return governanceTokenPrice; }
    public String getStablecoinValue() { return stablecoinValue; }
    public String getRwaTokenValue() { return rwaTokenValue; }
    public String getTopTokenByMarketCap() { return topTokenByMarketCap; }
    public String getTopTokenByVolume() { return topTokenByVolume; }
    public String getAverageTokenPrice() { return averageTokenPrice; }
    public Double getPriceVolatility() { return priceVolatility; }
    public Integer getTotalTokenHolders() { return totalTokenHolders; }
    public Integer getTokenCreatedLast24h() { return tokenCreatedLast24h; }
    public Integer getTokenBurnedLast24h() { return tokenBurnedLast24h; }
    public Integer getTokenTransfersLast24h() { return tokenTransfersLast24h; }
    public Double getTotalTokenValue24hChange() { return totalTokenValue24hChange; }
    public Integer getGovernanceTokenHolders() { return governanceTokenHolders; }
    public Integer getRwaTokenCount() { return rwaTokenCount; }
    public Integer getRwaTokenHolders() { return rwaTokenHolders; }
    public Integer getFractionalTokenCount() { return fractionalTokenCount; }
    public String getError() { return error; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Integer totalTokens;
        private Integer activeTokens;
        private String totalTokenSupply;
        private String totalTokenValue;
        private String nativeTokenPrice;
        private String governanceTokenPrice;
        private String stablecoinValue;
        private String rwaTokenValue;
        private String topTokenByMarketCap;
        private String topTokenByVolume;
        private String averageTokenPrice;
        private Double priceVolatility;
        private Integer totalTokenHolders;
        private Integer tokenCreatedLast24h;
        private Integer tokenBurnedLast24h;
        private Integer tokenTransfersLast24h;
        private Double totalTokenValue24hChange;
        private Integer governanceTokenHolders;
        private Integer rwaTokenCount;
        private Integer rwaTokenHolders;
        private Integer fractionalTokenCount;
        private String error;

        public Builder totalTokens(Integer totalTokens) { this.totalTokens = totalTokens; return this; }
        public Builder activeTokens(Integer activeTokens) { this.activeTokens = activeTokens; return this; }
        public Builder totalTokenSupply(String totalTokenSupply) { this.totalTokenSupply = totalTokenSupply; return this; }
        public Builder totalTokenValue(String totalTokenValue) { this.totalTokenValue = totalTokenValue; return this; }
        public Builder nativeTokenPrice(String nativeTokenPrice) { this.nativeTokenPrice = nativeTokenPrice; return this; }
        public Builder governanceTokenPrice(String governanceTokenPrice) { this.governanceTokenPrice = governanceTokenPrice; return this; }
        public Builder stablecoinValue(String stablecoinValue) { this.stablecoinValue = stablecoinValue; return this; }
        public Builder rwaTokenValue(String rwaTokenValue) { this.rwaTokenValue = rwaTokenValue; return this; }
        public Builder topTokenByMarketCap(String topTokenByMarketCap) { this.topTokenByMarketCap = topTokenByMarketCap; return this; }
        public Builder topTokenByVolume(String topTokenByVolume) { this.topTokenByVolume = topTokenByVolume; return this; }
        public Builder averageTokenPrice(String averageTokenPrice) { this.averageTokenPrice = averageTokenPrice; return this; }
        public Builder priceVolatility(Double priceVolatility) { this.priceVolatility = priceVolatility; return this; }
        public Builder totalTokenHolders(Integer totalTokenHolders) { this.totalTokenHolders = totalTokenHolders; return this; }
        public Builder tokenCreatedLast24h(Integer tokenCreatedLast24h) { this.tokenCreatedLast24h = tokenCreatedLast24h; return this; }
        public Builder tokenBurnedLast24h(Integer tokenBurnedLast24h) { this.tokenBurnedLast24h = tokenBurnedLast24h; return this; }
        public Builder tokenTransfersLast24h(Integer tokenTransfersLast24h) { this.tokenTransfersLast24h = tokenTransfersLast24h; return this; }
        public Builder totalTokenValue24hChange(Double totalTokenValue24hChange) { this.totalTokenValue24hChange = totalTokenValue24hChange; return this; }
        public Builder governanceTokenHolders(Integer governanceTokenHolders) { this.governanceTokenHolders = governanceTokenHolders; return this; }
        public Builder rwaTokenCount(Integer rwaTokenCount) { this.rwaTokenCount = rwaTokenCount; return this; }
        public Builder rwaTokenHolders(Integer rwaTokenHolders) { this.rwaTokenHolders = rwaTokenHolders; return this; }
        public Builder fractionalTokenCount(Integer fractionalTokenCount) { this.fractionalTokenCount = fractionalTokenCount; return this; }
        public Builder error(String error) { this.error = error; return this; }

        public TokenStatisticsDTO build() { return new TokenStatisticsDTO(this); }
    }
}
