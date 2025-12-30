package io.aurigraph.v11.demo.services;

import io.aurigraph.v11.merkle.MerkleTreeRegistry;
import io.smallrye.mutiny.Uni;
import org.jboss.logging.Logger;

import java.util.*;

/**
 * Data Feed Registry using Merkle Tree for cryptographic verification
 *
 * Manages external API data feeds with tokenization:
 * - 5 external API endpoints (mapped to 5 slim nodes)
 * - Each data feed is tokenized as RWAT (Real-World Asset Token)
 * - Merkle tree ensures data integrity
 * - Real-time updates tracked with timestamps
 */
public class DataFeedRegistry extends MerkleTreeRegistry<DataFeedToken> {

    private static final Logger LOG = Logger.getLogger(DataFeedRegistry.class);

    public static class ExternalAPI {
        public String id;
        public String name;
        public String endpoint;
        public String dataType;
        public long lastUpdate;

        public ExternalAPI(String id, String name, String endpoint, String dataType) {
            this.id = id;
            this.name = name;
            this.endpoint = endpoint;
            this.dataType = dataType;
            this.lastUpdate = System.currentTimeMillis();
        }
    }

    // Standard 5 external API data feeds (for 5 slim nodes)
    private static final String[] STANDARD_APIS = {
        "api-0-price-feed",
        "api-1-market-data",
        "api-2-weather-station",
        "api-3-iot-sensors",
        "api-4-supply-chain"
    };

    private static final String[] STANDARD_API_NAMES = {
        "Price Feed API",
        "Market Data API",
        "Weather Station API",
        "IoT Sensors API",
        "Supply Chain API"
    };

    private static final String[] STANDARD_API_ENDPOINTS = {
        "https://api.example.com/prices",
        "https://api.example.com/market",
        "https://api.example.com/weather",
        "https://api.example.com/iot",
        "https://api.example.com/supply-chain"
    };

    private static final String[] STANDARD_DATA_TYPES = {
        "PRICE_DATA",
        "MARKET_DATA",
        "ENVIRONMENTAL_DATA",
        "SENSOR_DATA",
        "LOGISTICS_DATA"
    };

    private final Map<String, ExternalAPI> externalAPIs = new HashMap<>();
    private final Map<String, Long> lastUpdateTime = new HashMap<>();
    private long totalUpdates = 0;
    private long totalTokens = 0;

    public DataFeedRegistry() {
        super();
        initializeStandardAPIs();
    }

    /**
     * Initialize the 5 standard external API feeds
     */
    private void initializeStandardAPIs() {
        for (int i = 0; i < STANDARD_APIS.length; i++) {
            ExternalAPI api = new ExternalAPI(
                STANDARD_APIS[i],
                STANDARD_API_NAMES[i],
                STANDARD_API_ENDPOINTS[i],
                STANDARD_DATA_TYPES[i]
            );
            externalAPIs.put(STANDARD_APIS[i], api);
            lastUpdateTime.put(STANDARD_APIS[i], System.currentTimeMillis());
        }
        LOG.infof("Initialized %d standard external API data feeds", STANDARD_APIS.length);
    }

    /**
     * Register a new data feed and tokenize it
     */
    public Uni<String> registerAndTokenizeFeed(String apiId, Object feedData) {
        return Uni.createFrom().item(() -> {
            ExternalAPI api = externalAPIs.get(apiId);
            if (api == null) {
                throw new IllegalArgumentException("Unknown API: " + apiId);
            }

            // Create token
            String tokenId = "TOKEN-" + UUID.randomUUID().toString().substring(0, 8);
            DataFeedToken token = new DataFeedToken(
                tokenId,
                apiId,
                api.name,
                api.dataType,
                feedData
            );

            // Add to registry (which updates Merkle tree)
            registry.put(tokenId, token);
            rebuildMerkleTree();

            // Update metadata
            lastUpdateTime.put(apiId, System.currentTimeMillis());
            totalUpdates++;
            totalTokens++;
            api.lastUpdate = System.currentTimeMillis();

            LOG.infof("Tokenized data feed from %s: %s (root: %s)",
                api.name, tokenId, currentRootHash.substring(0, 16));

            return tokenId;
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    /**
     * Update existing data feed token
     */
    public Uni<Boolean> updateFeedToken(String tokenId, Object newData) {
        return Uni.createFrom().item(() -> {
            DataFeedToken token = registry.get(tokenId);
            if (token == null) {
                return false;
            }

            token.update(newData);
            rebuildMerkleTree();
            totalUpdates++;

            ExternalAPI api = externalAPIs.get(token.feedId);
            if (api != null) {
                api.lastUpdate = System.currentTimeMillis();
                lastUpdateTime.put(token.feedId, System.currentTimeMillis());
            }

            LOG.infof("Updated feed token %s from %s (root: %s)",
                tokenId, token.feedName, currentRootHash.substring(0, 16));

            return true;
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    /**
     * Get all tokens for a specific API
     */
    public Uni<List<DataFeedToken>> getTokensByAPI(String apiId) {
        return Uni.createFrom().item(() -> {
            List<DataFeedToken> result = new ArrayList<>();
            for (DataFeedToken token : registry.values()) {
                if (token.feedId.equals(apiId)) {
                    result.add(token);
                }
            }
            return result;
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    /**
     * Get registry statistics
     */
    public Uni<DataFeedRegistryStats> getStats() {
        return getTreeStats().map(treeStats -> new DataFeedRegistryStats(
            treeStats.getRootHash(),
            treeStats.getEntryCount(),
            treeStats.getTreeHeight(),
            treeStats.getLastUpdate(),
            treeStats.getRebuildCount(),
            totalUpdates,
            totalTokens,
            externalAPIs.size(),
            lastUpdateTime.values().stream()
                .max(Long::compare)
                .orElse(System.currentTimeMillis())
        ));
    }

    /**
     * Get detailed feed status
     */
    public Uni<FeedStatus> getFeedStatus(String apiId) {
        return Uni.createFrom().item(() -> {
            ExternalAPI api = externalAPIs.get(apiId);
            if (api == null) {
                throw new IllegalArgumentException("Unknown API: " + apiId);
            }

            List<DataFeedToken> tokens = new ArrayList<>();
            for (DataFeedToken token : registry.values()) {
                if (token.feedId.equals(apiId)) {
                    tokens.add(token);
                }
            }

            return new FeedStatus(
                api.id,
                api.name,
                api.endpoint,
                api.dataType,
                tokens.size(),
                api.lastUpdate,
                currentRootHash
            );
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    /**
     * Get all external APIs
     */
    public Uni<List<ExternalAPI>> getAllAPIs() {
        return Uni.createFrom().<List<ExternalAPI>>item(() -> new ArrayList<>(externalAPIs.values()))
            .runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    /**
     * Serialize value for Merkle tree hashing
     */
    @Override
    protected String serializeValue(DataFeedToken token) {
        return String.format("%s|%s|%s|%s",
            token.tokenId,
            token.feedId,
            token.dataType,
            token.tokenHash
        );
    }

    // Statistics class
    public static class DataFeedRegistryStats {
        public String rootHash;
        public int tokenCount;
        public int treeHeight;
        public java.time.Instant lastUpdate;
        public long rebuildCount;
        public long totalUpdates;
        public long totalTokens;
        public int apiCount;
        public long lastFeedUpdate;

        public DataFeedRegistryStats(String rootHash, int tokenCount, int treeHeight,
                                    java.time.Instant lastUpdate, long rebuildCount,
                                    long totalUpdates, long totalTokens, int apiCount,
                                    long lastFeedUpdate) {
            this.rootHash = rootHash;
            this.tokenCount = tokenCount;
            this.treeHeight = treeHeight;
            this.lastUpdate = lastUpdate;
            this.rebuildCount = rebuildCount;
            this.totalUpdates = totalUpdates;
            this.totalTokens = totalTokens;
            this.apiCount = apiCount;
            this.lastFeedUpdate = lastFeedUpdate;
        }

        public String getRootHash() { return rootHash; }
        public int getTokenCount() { return tokenCount; }
        public int getEntryCount() { return tokenCount; }
        public int getTreeHeight() { return treeHeight; }
        public java.time.Instant getLastUpdate() { return lastUpdate; }
        public long getRebuildCount() { return rebuildCount; }
    }

    // Feed status class
    public static class FeedStatus {
        public String apiId;
        public String apiName;
        public String endpoint;
        public String dataType;
        public int tokenCount;
        public long lastUpdate;
        public String merkleRoot;

        public FeedStatus(String apiId, String apiName, String endpoint, String dataType,
                         int tokenCount, long lastUpdate, String merkleRoot) {
            this.apiId = apiId;
            this.apiName = apiName;
            this.endpoint = endpoint;
            this.dataType = dataType;
            this.tokenCount = tokenCount;
            this.lastUpdate = lastUpdate;
            this.merkleRoot = merkleRoot;
        }
    }

    /**
     * Utility class for SHA3 hashing
     */
    protected static class MerkleHashUtil {
        public static String sha3Hash(String input) {
            org.bouncycastle.crypto.digests.SHA3Digest digest =
                new org.bouncycastle.crypto.digests.SHA3Digest(256);
            byte[] inputBytes = input.getBytes();
            digest.update(inputBytes, 0, inputBytes.length);
            byte[] hash = new byte[32];
            digest.doFinal(hash, 0);
            return org.bouncycastle.util.encoders.Hex.toHexString(hash);
        }
    }
}
