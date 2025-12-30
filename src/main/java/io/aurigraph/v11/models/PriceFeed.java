package io.aurigraph.v11.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Price Feed Model
 * Provides real-time price aggregation from multiple oracle sources
 *
 * Used by /api/v11/datafeeds/prices endpoint
 *
 * @author Aurigraph V11
 * @version 11.3.0
 */
public class PriceFeed {

    @JsonProperty("timestamp")
    private Instant timestamp;

    @JsonProperty("prices")
    private List<AssetPrice> prices;

    @JsonProperty("sources")
    private List<PriceSource> sources;

    @JsonProperty("aggregation_method")
    private String aggregationMethod; // "median", "mean", "weighted_average"

    @JsonProperty("update_frequency_ms")
    private long updateFrequencyMs;

    // Constructor
    public PriceFeed() {
        this.timestamp = Instant.now();
        this.aggregationMethod = "median";
        this.updateFrequencyMs = 5000; // 5 seconds default
    }

    // Getters and Setters
    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }

    public List<AssetPrice> getPrices() { return prices; }
    public void setPrices(List<AssetPrice> prices) { this.prices = prices; }

    public List<PriceSource> getSources() { return sources; }
    public void setSources(List<PriceSource> sources) { this.sources = sources; }

    public String getAggregationMethod() { return aggregationMethod; }
    public void setAggregationMethod(String aggregationMethod) { this.aggregationMethod = aggregationMethod; }

    public long getUpdateFrequencyMs() { return updateFrequencyMs; }
    public void setUpdateFrequencyMs(long updateFrequencyMs) { this.updateFrequencyMs = updateFrequencyMs; }

    /**
     * Asset Price
     */
    public static class AssetPrice {
        @JsonProperty("asset_symbol")
        private String assetSymbol; // BTC, ETH, MATIC, etc.

        @JsonProperty("asset_name")
        private String assetName;

        @JsonProperty("price_usd")
        private double priceUsd;

        @JsonProperty("price_change_24h")
        private double priceChange24h; // percentage

        @JsonProperty("volume_24h_usd")
        private double volume24hUsd;

        @JsonProperty("market_cap_usd")
        private double marketCapUsd;

        @JsonProperty("confidence_score")
        private double confidenceScore; // 0.0 to 1.0

        @JsonProperty("source_count")
        private int sourceCount; // number of sources providing this price

        @JsonProperty("last_updated")
        private Instant lastUpdated;

        public AssetPrice() {}

        public AssetPrice(String symbol, String name, double price, double change24h,
                         double volume, double marketCap, double confidence, int sources) {
            this.assetSymbol = symbol;
            this.assetName = name;
            this.priceUsd = price;
            this.priceChange24h = change24h;
            this.volume24hUsd = volume;
            this.marketCapUsd = marketCap;
            this.confidenceScore = confidence;
            this.sourceCount = sources;
            this.lastUpdated = Instant.now();
        }

        // Getters and Setters
        public String getAssetSymbol() { return assetSymbol; }
        public void setAssetSymbol(String assetSymbol) { this.assetSymbol = assetSymbol; }

        public String getAssetName() { return assetName; }
        public void setAssetName(String assetName) { this.assetName = assetName; }

        public double getPriceUsd() { return priceUsd; }
        public void setPriceUsd(double priceUsd) { this.priceUsd = priceUsd; }

        public double getPriceChange24h() { return priceChange24h; }
        public void setPriceChange24h(double priceChange24h) { this.priceChange24h = priceChange24h; }

        public double getVolume24hUsd() { return volume24hUsd; }
        public void setVolume24hUsd(double volume24hUsd) { this.volume24hUsd = volume24hUsd; }

        public double getMarketCapUsd() { return marketCapUsd; }
        public void setMarketCapUsd(double marketCapUsd) { this.marketCapUsd = marketCapUsd; }

        public double getConfidenceScore() { return confidenceScore; }
        public void setConfidenceScore(double confidenceScore) { this.confidenceScore = confidenceScore; }

        public int getSourceCount() { return sourceCount; }
        public void setSourceCount(int sourceCount) { this.sourceCount = sourceCount; }

        public Instant getLastUpdated() { return lastUpdated; }
        public void setLastUpdated(Instant lastUpdated) { this.lastUpdated = lastUpdated; }
    }

    /**
     * Price Source
     */
    public static class PriceSource {
        @JsonProperty("source_name")
        private String sourceName; // "Chainlink", "Band Protocol", "Pyth", etc.

        @JsonProperty("source_type")
        private String sourceType; // "oracle", "exchange", "aggregator"

        @JsonProperty("status")
        private String status; // "active", "degraded", "offline"

        @JsonProperty("reliability_score")
        private double reliabilityScore; // 0.0 to 1.0

        @JsonProperty("last_update")
        private Instant lastUpdate;

        @JsonProperty("update_count_24h")
        private long updateCount24h;

        @JsonProperty("supported_assets")
        private int supportedAssets;

        public PriceSource() {}

        public PriceSource(String name, String type, String status, double reliability,
                          long updateCount, int assets) {
            this.sourceName = name;
            this.sourceType = type;
            this.status = status;
            this.reliabilityScore = reliability;
            this.lastUpdate = Instant.now();
            this.updateCount24h = updateCount;
            this.supportedAssets = assets;
        }

        // Getters and Setters
        public String getSourceName() { return sourceName; }
        public void setSourceName(String sourceName) { this.sourceName = sourceName; }

        public String getSourceType() { return sourceType; }
        public void setSourceType(String sourceType) { this.sourceType = sourceType; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public double getReliabilityScore() { return reliabilityScore; }
        public void setReliabilityScore(double reliabilityScore) { this.reliabilityScore = reliabilityScore; }

        public Instant getLastUpdate() { return lastUpdate; }
        public void setLastUpdate(Instant lastUpdate) { this.lastUpdate = lastUpdate; }

        public long getUpdateCount24h() { return updateCount24h; }
        public void setUpdateCount24h(long updateCount24h) { this.updateCount24h = updateCount24h; }

        public int getSupportedAssets() { return supportedAssets; }
        public void setSupportedAssets(int supportedAssets) { this.supportedAssets = supportedAssets; }
    }
}
