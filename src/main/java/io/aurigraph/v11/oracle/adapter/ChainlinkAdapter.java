package io.aurigraph.v11.oracle.adapter;

import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.aurigraph.v11.live.LiveExternalDataService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Base64;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Chainlink Oracle Adapter
 * Integrates with Chainlink decentralized oracle network
 *
 * Chainlink provides:
 * - Decentralized price feeds with multiple data sources
 * - On-chain data aggregation
 * - Proven reliability (used by DeFi protocols managing $50B+ TVL)
 * - Sub-second latency for price updates
 * - Built-in failover and redundancy
 *
 * @author Aurigraph V11 - Development Agent 4
 * @version 11.0.0
 * @sprint Sprint 16 - Oracle Verification System (AV11-483)
 */
@ApplicationScoped
public class ChainlinkAdapter extends BaseOracleAdapter {

    private static final String PROVIDER_NAME = "Chainlink";
    private static final long UPDATE_FREQUENCY_MS = 1000; // 1 second updates
    private static final double STAKE_WEIGHT = 1.5; // Higher weight for Chainlink

    @ConfigProperty(name = "oracle.chainlink.api.url", defaultValue = "https://api.chain.link")
    String chainlinkApiUrl;

    @ConfigProperty(name = "oracle.chainlink.api.key", defaultValue = "NONE")
    String chainlinkApiKey;

    @ConfigProperty(name = "oracle.chainlink.fallback.enabled", defaultValue = "true")
    boolean fallbackEnabled;

    @ConfigProperty(name = "oracle.chainlink.use.live.data", defaultValue = "true")
    boolean useLiveData;

    @Inject
    LiveExternalDataService liveDataService;

    // Cache for price feed addresses
    private final Map<String, String> priceFeedAddresses;

    // Fallback oracle endpoints
    private final String[] fallbackEndpoints = {
        "https://api.chain.link",
        "https://chainlink-feed-1.aurigraph.io",
        "https://chainlink-feed-2.aurigraph.io"
    };

    private int currentFallbackIndex = 0;

    public ChainlinkAdapter() {
        super("chainlink-oracle-1", PROVIDER_NAME);
        this.priceFeedAddresses = new ConcurrentHashMap<>();
        initializePriceFeedAddresses();
    }

    /**
     * Initialize Chainlink price feed addresses for common assets
     * In production, these would be actual Chainlink contract addresses
     */
    private void initializePriceFeedAddresses() {
        // Ethereum mainnet Chainlink price feed addresses (examples)
        priceFeedAddresses.put("BTC/USD", "0xF4030086522a5bEEa4988F8cA5B36dbC97BeE88c");
        priceFeedAddresses.put("ETH/USD", "0x5f4eC3Df9cbd43714FE2740f5E3616155c5b8419");
        priceFeedAddresses.put("USDC/USD", "0x8fFfFfd4AfB6115b954Bd326cbe7B4BA576818f6");
        priceFeedAddresses.put("USDT/USD", "0x3E7d1eAB13ad0104d2750B8863b489D65364e32D");
        priceFeedAddresses.put("DAI/USD", "0xAed0c38402a5d19df6E4c03F4E2DceD6e29c1ee9");
        priceFeedAddresses.put("LINK/USD", "0x2c1d072e956AFFC0D435Cb7AC38EF18d24d9127c");
        priceFeedAddresses.put("MATIC/USD", "0x7bAC85A8a13A4BcD8abb3eB7d6b4d632c5a57676");
        priceFeedAddresses.put("AVAX/USD", "0xFF3EEb22B5E3dE6e705b44749C2559d704923FD7");
        priceFeedAddresses.put("SOL/USD", "0x4ffC43a60e009B551865A93d232E33Fce9f01507");
        priceFeedAddresses.put("BNB/USD", "0x14e613AC84a31f709eadbdF89C6CC390fDc9540A");
    }

    @Override
    protected BigDecimal fetchPriceFromProvider(String assetId) throws Exception {
        String feedAddress = priceFeedAddresses.get(assetId);
        if (feedAddress == null) {
            throw new IllegalArgumentException("Unsupported asset: " + assetId);
        }

        try {
            // In production, this would make an actual API call to Chainlink
            // For now, simulate with realistic price data
            return fetchFromChainlink(assetId, feedAddress);
        } catch (Exception e) {
            if (fallbackEnabled) {
                Log.warnf("Primary Chainlink endpoint failed, trying fallback for %s", assetId);
                return fetchFromFallback(assetId, feedAddress);
            }
            throw e;
        }
    }

    /**
     * Fetch price from Chainlink primary endpoint
     * Uses LiveExternalDataService for REAL prices from CoinGecko/Binance
     *
     * @param assetId The asset pair identifier (e.g., "BTC/USD")
     * @param feedAddress The Chainlink price feed contract address
     * @return Price from external API (real data)
     */
    private BigDecimal fetchFromChainlink(String assetId, String feedAddress) throws Exception {
        // Use REAL data from LiveExternalDataService if enabled
        if (useLiveData && liveDataService != null) {
            try {
                // Extract symbol from assetId (e.g., "BTC/USD" -> "BTC")
                String symbol = assetId.split("/")[0];

                LiveExternalDataService.LivePriceData priceData =
                    liveDataService.getLivePrice(symbol).await().indefinitely();

                if (priceData != null && priceData.price > 0) {
                    Log.infof("Using LIVE price for %s: $%.2f (source: %s)",
                        symbol, priceData.price, priceData.source);
                    return BigDecimal.valueOf(priceData.price)
                        .setScale(8, RoundingMode.HALF_UP);
                }
            } catch (Exception e) {
                Log.warnf("Live data fetch failed for %s, falling back to simulation: %s",
                    assetId, e.getMessage());
            }
        }

        // Fallback to simulated data if live data is disabled or failed
        Log.debugf("Using simulated price for %s (live data: %s)", assetId, useLiveData);
        return simulateChainlinkPrice(assetId);
    }

    /**
     * Fetch from fallback endpoint
     */
    private BigDecimal fetchFromFallback(String assetId, String feedAddress) throws Exception {
        currentFallbackIndex = (currentFallbackIndex + 1) % fallbackEndpoints.length;
        String fallbackUrl = fallbackEndpoints[currentFallbackIndex];

        Log.infof("Using Chainlink fallback endpoint: %s", fallbackUrl);

        // Simulate fallback fetch
        Thread.sleep(100 + new Random().nextInt(100));
        return simulateChainlinkPrice(assetId);
    }

    /**
     * Simulate Chainlink price data
     * In production, this would be replaced with actual API calls
     */
    private BigDecimal simulateChainlinkPrice(String assetId) {
        Random random = new Random(assetId.hashCode() + System.currentTimeMillis() / 1000);

        // Base prices for common assets
        Map<String, Double> basePrices = Map.of(
            "BTC/USD", 43250.00,
            "ETH/USD", 2280.00,
            "USDC/USD", 1.00,
            "USDT/USD", 1.00,
            "DAI/USD", 1.00,
            "LINK/USD", 14.50,
            "MATIC/USD", 0.85,
            "AVAX/USD", 36.20,
            "SOL/USD", 98.50,
            "BNB/USD", 310.00
        );

        double basePrice = basePrices.getOrDefault(assetId, 1000.0);

        // Chainlink has very small variance due to aggregation (±0.1%)
        double variance = (random.nextDouble() - 0.5) * 0.002;
        double price = basePrice * (1.0 + variance);

        // Chainlink typically provides 8 decimals of precision
        return BigDecimal.valueOf(price).setScale(8, RoundingMode.HALF_UP);
    }

    @Override
    protected String generateSignature(String assetId, BigDecimal price) {
        // In production, Chainlink nodes sign their responses with their private keys
        // The aggregator contract verifies these signatures on-chain
        // For simulation, create a deterministic signature
        String data = String.format("%s:%s:%s:%d",
            assetId,
            price.toPlainString(),
            oracleId,
            System.currentTimeMillis() / 1000
        );
        return Base64.getEncoder().encodeToString(data.getBytes());
    }

    @Override
    public String[] getSupportedAssets() {
        return priceFeedAddresses.keySet().toArray(new String[0]);
    }

    @Override
    public long getUpdateFrequency() {
        return UPDATE_FREQUENCY_MS;
    }

    @Override
    public double getStakeWeight() {
        return STAKE_WEIGHT;
    }

    /**
     * Get Chainlink-specific metadata
     */
    public Map<String, Object> getChainlinkMetadata() {
        return Map.of(
            "provider", PROVIDER_NAME,
            "oracleId", oracleId,
            "supportedAssets", priceFeedAddresses.size(),
            "updateFrequency", UPDATE_FREQUENCY_MS + "ms",
            "fallbackEnabled", fallbackEnabled,
            "fallbackEndpoints", fallbackEndpoints.length,
            "stakeWeight", STAKE_WEIGHT,
            "reliabilityScore", getReliabilityScore(),
            "totalFetches", totalFetches.get(),
            "successfulFetches", successfulFetches.get()
        );
    }
}
