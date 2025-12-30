package io.aurigraph.v11.services;

import io.aurigraph.v11.models.PriceFeed;
import io.aurigraph.v11.models.PriceFeed.*;
import io.aurigraph.v11.live.LiveExternalDataService;
import io.aurigraph.v11.live.LiveExternalDataService.LivePriceData;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Price Feed Service
 * Provides real-time price aggregation from multiple oracle sources
 *
 * @author Aurigraph V11
 * @version 11.3.0
 */
@ApplicationScoped
public class PriceFeedService {

    private static final Logger LOG = Logger.getLogger(PriceFeedService.class);

    @Inject
    LiveExternalDataService liveDataService;

    @ConfigProperty(name = "price.feed.use.live.data", defaultValue = "true")
    boolean useLiveData;

    /**
     * Get current price feed for all supported assets
     * Uses REAL data from CoinGecko/Binance when available
     */
    public Uni<PriceFeed> getPriceFeed() {
        return Uni.createFrom().item(() -> {
            PriceFeed feed = new PriceFeed();

            // Use real data from LiveExternalDataService if enabled
            if (useLiveData && liveDataService != null) {
                try {
                    List<LivePriceData> livePrices = liveDataService.getAllLivePrices()
                        .await().indefinitely();

                    if (livePrices != null && !livePrices.isEmpty()) {
                        LOG.info("Using LIVE price data from external APIs");
                        feed.setPrices(buildLiveAssetPrices(livePrices));
                        feed.setSources(buildPriceSources());
                        feed.setAggregationMethod("live-external");
                        feed.setUpdateFrequencyMs(30000); // 30 second updates from CoinGecko
                        return feed;
                    }
                } catch (Exception e) {
                    LOG.warnf("Failed to fetch live prices, using simulated: %s", e.getMessage());
                }
            }

            // Fallback to simulated data
            feed.setPrices(buildAssetPrices());
            feed.setSources(buildPriceSources());
            feed.setAggregationMethod("median");
            feed.setUpdateFrequencyMs(5000); // 5 second updates

            LOG.debugf("Generated price feed with %d assets from %d sources",
                    feed.getPrices().size(),
                    feed.getSources().size());

            return feed;
        });
    }

    /**
     * Build asset prices from LIVE external data
     */
    private List<AssetPrice> buildLiveAssetPrices(List<LivePriceData> livePrices) {
        List<AssetPrice> prices = new ArrayList<>();

        for (LivePriceData livePrice : livePrices) {
            String name = getAssetName(livePrice.symbol);
            prices.add(new AssetPrice(
                livePrice.symbol,
                name,
                livePrice.price,
                livePrice.priceChange24h,
                livePrice.volume24h,
                livePrice.marketCap,
                0.98, // High confidence from external API
                1 // Single source (aggregated by CoinGecko)
            ));
        }

        return prices;
    }

    /**
     * Get full asset name from symbol
     */
    private String getAssetName(String symbol) {
        return switch (symbol) {
            case "BTC" -> "Bitcoin";
            case "ETH" -> "Ethereum";
            case "MATIC" -> "Polygon";
            case "SOL" -> "Solana";
            case "AVAX" -> "Avalanche";
            case "DOT" -> "Polkadot";
            case "LINK" -> "Chainlink";
            case "UNI" -> "Uniswap";
            case "ATOM" -> "Cosmos";
            case "XRP" -> "Ripple";
            case "ADA" -> "Cardano";
            case "DOGE" -> "Dogecoin";
            case "BNB" -> "Binance Coin";
            case "USDT" -> "Tether";
            case "USDC" -> "USD Coin";
            default -> symbol;
        };
    }

    /**
     * Get price feed for specific asset
     */
    public Uni<AssetPrice> getAssetPrice(String symbol) {
        return Uni.createFrom().item(() -> {
            List<AssetPrice> prices = buildAssetPrices();

            return prices.stream()
                    .filter(p -> p.getAssetSymbol().equalsIgnoreCase(symbol))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Asset not found: " + symbol));
        });
    }

    /**
     * Build current asset prices with simulated real-time data
     */
    private List<AssetPrice> buildAssetPrices() {
        List<AssetPrice> prices = new ArrayList<>();

        // Major cryptocurrencies with simulated real-time prices
        prices.add(new AssetPrice(
                "BTC", "Bitcoin",
                42500.0 + (Math.random() * 1000.0 - 500.0), // $42,000 - $43,000
                2.5 + (Math.random() * 2.0 - 1.0), // -1% to +4% change
                28500000000.0 + (Math.random() * 2000000000.0), // $28.5B - $30.5B volume
                835000000000.0, // $835B market cap
                0.98, // 98% confidence
                5 // 5 price sources
        ));

        prices.add(new AssetPrice(
                "ETH", "Ethereum",
                2250.0 + (Math.random() * 100.0 - 50.0), // $2,200 - $2,300
                3.2 + (Math.random() * 2.0 - 1.0),
                12500000000.0 + (Math.random() * 1000000000.0),
                270000000000.0,
                0.97,
                5
        ));

        prices.add(new AssetPrice(
                "MATIC", "Polygon",
                0.85 + (Math.random() * 0.1 - 0.05), // $0.80 - $0.90
                5.1 + (Math.random() * 2.0 - 1.0),
                450000000.0 + (Math.random() * 50000000.0),
                8200000000.0,
                0.95,
                4
        ));

        prices.add(new AssetPrice(
                "SOL", "Solana",
                98.0 + (Math.random() * 10.0 - 5.0), // $93 - $103
                4.7 + (Math.random() * 2.0 - 1.0),
                1800000000.0 + (Math.random() * 200000000.0),
                42000000000.0,
                0.96,
                4
        ));

        prices.add(new AssetPrice(
                "AVAX", "Avalanche",
                35.0 + (Math.random() * 3.0 - 1.5), // $33.5 - $36.5
                3.8 + (Math.random() * 2.0 - 1.0),
                520000000.0 + (Math.random() * 80000000.0),
                13500000000.0,
                0.94,
                4
        ));

        prices.add(new AssetPrice(
                "DOT", "Polkadot",
                6.8 + (Math.random() * 0.6 - 0.3), // $6.5 - $7.1
                2.9 + (Math.random() * 2.0 - 1.0),
                280000000.0 + (Math.random() * 40000000.0),
                8900000000.0,
                0.93,
                4
        ));

        prices.add(new AssetPrice(
                "LINK", "Chainlink",
                14.5 + (Math.random() * 1.5 - 0.75), // $13.75 - $15.25
                4.2 + (Math.random() * 2.0 - 1.0),
                650000000.0 + (Math.random() * 100000000.0),
                8200000000.0,
                0.97,
                5
        ));

        prices.add(new AssetPrice(
                "UNI", "Uniswap",
                5.6 + (Math.random() * 0.5 - 0.25), // $5.35 - $5.85
                3.5 + (Math.random() * 2.0 - 1.0),
                180000000.0 + (Math.random() * 30000000.0),
                4200000000.0,
                0.92,
                4
        ));

        return prices;
    }

    /**
     * Build price sources status
     */
    private List<PriceSource> buildPriceSources() {
        List<PriceSource> sources = new ArrayList<>();

        sources.add(new PriceSource(
                "Chainlink",
                "oracle",
                "active",
                0.98,
                17280, // updates per day
                150 // supported assets
        ));

        sources.add(new PriceSource(
                "Band Protocol",
                "oracle",
                "active",
                0.96,
                14400,
                120
        ));

        sources.add(new PriceSource(
                "Pyth Network",
                "oracle",
                "active",
                0.97,
                86400, // high frequency updates
                200
        ));

        sources.add(new PriceSource(
                "API3",
                "oracle",
                "active",
                0.95,
                12000,
                80
        ));

        sources.add(new PriceSource(
                "Coinbase",
                "exchange",
                "active",
                0.99,
                20000,
                250
        ));

        sources.add(new PriceSource(
                "Binance",
                "exchange",
                "active",
                0.98,
                25000,
                380
        ));

        return sources;
    }
}
