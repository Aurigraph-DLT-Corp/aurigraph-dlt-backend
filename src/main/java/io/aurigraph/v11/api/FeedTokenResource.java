package io.aurigraph.v11.api;

import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Data Feed Token Resource
 *
 * Manages tokenization of data feeds for throughput tracking and incentivization.
 * Each feed generates tokens based on data throughput and quality metrics.
 *
 * @author Backend Development Agent (BDA)
 * @version 1.0.0
 * @since October 2025
 */
@Path("/api/v11/feed-tokens")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Feed Token API", description = "Data feed tokenization and throughput management")
public class FeedTokenResource {

    private static final Logger LOG = Logger.getLogger(FeedTokenResource.class);

    // In-memory token storage (replace with database in production)
    private static final Map<String, FeedToken> TOKENS = new ConcurrentHashMap<>();
    private static final Map<String, List<TokenTransaction>> TRANSACTIONS = new ConcurrentHashMap<>();
    private static final Map<String, ThroughputMetrics> THROUGHPUT = new ConcurrentHashMap<>();

    static {
        initializeFeedTokens();
    }

    // ==================== TOKEN MANAGEMENT APIs ====================

    /**
     * Get all feed tokens
     * GET /api/v11/feed-tokens
     */
    @GET
    @Operation(summary = "List all feed tokens", description = "Retrieve list of all data feed tokens with throughput metrics")
    public Uni<TokensList> getAllTokens(
            @QueryParam("sortBy") @DefaultValue("throughput") String sortBy,
            @QueryParam("limit") @DefaultValue("50") int limit) {

        LOG.infof("Fetching feed tokens (sortBy: %s, limit: %d)", sortBy, limit);

        return Uni.createFrom().item(() -> {
            TokensList list = new TokensList();

            List<FeedToken> tokens = TOKENS.values().stream()
                .sorted((a, b) -> {
                    switch (sortBy) {
                        case "throughput":
                            return Double.compare(b.currentThroughput, a.currentThroughput);
                        case "value":
                            return b.tokenValue.compareTo(a.tokenValue);
                        case "supply":
                            return Long.compare(b.totalSupply, a.totalSupply);
                        default:
                            return a.feedName.compareTo(b.feedName);
                    }
                })
                .limit(limit)
                .collect(Collectors.toList());

            list.totalTokens = TOKENS.size();
            list.totalSupply = TOKENS.values().stream().mapToLong(t -> t.totalSupply).sum();
            list.totalThroughput = TOKENS.values().stream().mapToDouble(t -> t.currentThroughput).sum();
            list.tokens = tokens;

            return list;
        });
    }

    /**
     * Get specific feed token
     * GET /api/v11/feed-tokens/{feedId}
     */
    @GET
    @Path("/{feedId}")
    @Operation(summary = "Get token details", description = "Retrieve detailed token information for a specific feed")
    public Uni<Response> getTokenDetails(@PathParam("feedId") String feedId) {
        LOG.infof("Fetching token details for feed: %s", feedId);

        return Uni.createFrom().item(() -> {
            FeedToken token = TOKENS.get(feedId);
            if (token == null) {
                return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("error", "Token not found for feed", "feedId", feedId))
                    .build();
            }

            ThroughputMetrics metrics = THROUGHPUT.get(feedId);
            Map<String, Object> response = new HashMap<>();
            response.put("token", token);
            response.put("metrics", metrics);

            return Response.ok(response).build();
        });
    }

    /**
     * Mint new tokens for feed
     * POST /api/v11/feed-tokens/{feedId}/mint
     */
    @POST
    @Path("/{feedId}/mint")
    @Operation(summary = "Mint tokens", description = "Mint new tokens based on data throughput")
    public Uni<Response> mintTokens(@PathParam("feedId") String feedId, MintRequest request) {
        LOG.infof("Minting tokens for feed: %s (amount: %d)", feedId, request.amount);

        return Uni.createFrom().item(() -> {
            FeedToken token = TOKENS.get(feedId);
            if (token == null) {
                return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("error", "Token not found"))
                    .build();
            }

            long mintAmount = request.amount != null ? request.amount : calculateMintAmount(feedId);

            token.totalSupply += mintAmount;
            token.circulatingSupply += mintAmount;
            token.lastMint = Instant.now().toString();

            // Record transaction
            TokenTransaction tx = new TokenTransaction();
            tx.transactionId = "tx_" + UUID.randomUUID().toString().substring(0, 12);
            tx.type = "MINT";
            tx.feedId = feedId;
            tx.amount = mintAmount;
            tx.timestamp = Instant.now().toString();
            tx.reason = "Data throughput reward";

            TRANSACTIONS.computeIfAbsent(feedId, k -> new ArrayList<>()).add(tx);

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("tokensMinted", mintAmount);
            response.put("newTotalSupply", token.totalSupply);
            response.put("transaction", tx);

            return Response.ok(response).build();
        });
    }

    /**
     * Burn tokens
     * POST /api/v11/feed-tokens/{feedId}/burn
     */
    @POST
    @Path("/{feedId}/burn")
    @Operation(summary = "Burn tokens", description = "Burn tokens to reduce supply")
    public Uni<Response> burnTokens(@PathParam("feedId") String feedId, BurnRequest request) {
        LOG.infof("Burning tokens for feed: %s (amount: %d)", feedId, request.amount);

        return Uni.createFrom().item(() -> {
            FeedToken token = TOKENS.get(feedId);
            if (token == null) {
                return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("error", "Token not found"))
                    .build();
            }

            if (token.circulatingSupply < request.amount) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "Insufficient circulating supply"))
                    .build();
            }

            token.totalSupply -= request.amount;
            token.circulatingSupply -= request.amount;

            // Record transaction
            TokenTransaction tx = new TokenTransaction();
            tx.transactionId = "tx_" + UUID.randomUUID().toString().substring(0, 12);
            tx.type = "BURN";
            tx.feedId = feedId;
            tx.amount = request.amount;
            tx.timestamp = Instant.now().toString();
            tx.reason = request.reason != null ? request.reason : "Token burn";

            TRANSACTIONS.computeIfAbsent(feedId, k -> new ArrayList<>()).add(tx);

            return Response.ok(Map.of(
                "status", "success",
                "tokensBurned", request.amount,
                "newTotalSupply", token.totalSupply,
                "transaction", tx
            )).build();
        });
    }

    // ==================== THROUGHPUT TRACKING APIs ====================

    /**
     * Update throughput metrics
     * POST /api/v11/feed-tokens/{feedId}/throughput
     */
    @POST
    @Path("/{feedId}/throughput")
    @Operation(summary = "Update throughput", description = "Update real-time throughput metrics for a feed")
    public Uni<Response> updateThroughput(@PathParam("feedId") String feedId, ThroughputUpdate update) {
        LOG.infof("Updating throughput for feed: %s (%.2f tokens/sec)", feedId, update.tokensPerSecond);

        return Uni.createFrom().item(() -> {
            FeedToken token = TOKENS.get(feedId);
            if (token == null) {
                return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("error", "Token not found"))
                    .build();
            }

            // Update token throughput
            token.currentThroughput = update.tokensPerSecond;
            token.peakThroughput = Math.max(token.peakThroughput, update.tokensPerSecond);

            // Update throughput metrics
            ThroughputMetrics metrics = THROUGHPUT.computeIfAbsent(feedId, k -> new ThroughputMetrics());
            metrics.feedId = feedId;
            metrics.currentTPS = update.tokensPerSecond;
            metrics.peakTPS = Math.max(metrics.peakTPS, update.tokensPerSecond);
            metrics.averageTPS = (metrics.averageTPS * 0.9) + (update.tokensPerSecond * 0.1); // Moving average
            metrics.dataPoints = update.dataPoints != null ? update.dataPoints : metrics.dataPoints;
            metrics.lastUpdate = Instant.now().toString();

            // Update token value based on throughput
            updateTokenValue(token, metrics);

            return Response.ok(Map.of(
                "status", "success",
                "throughput", metrics,
                "tokenValue", token.tokenValue
            )).build();
        });
    }

    /**
     * Get throughput history
     * GET /api/v11/feed-tokens/{feedId}/throughput/history
     */
    @GET
    @Path("/{feedId}/throughput/history")
    @Operation(summary = "Get throughput history", description = "Retrieve historical throughput data")
    public Uni<Response> getThroughputHistory(
            @PathParam("feedId") String feedId,
            @QueryParam("period") @DefaultValue("1h") String period) {

        LOG.infof("Fetching throughput history for feed: %s (period: %s)", feedId, period);

        return Uni.createFrom().item(() -> {
            ThroughputMetrics metrics = THROUGHPUT.get(feedId);
            if (metrics == null) {
                return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("error", "No metrics found"))
                    .build();
            }

            // Generate sample historical data
            List<Map<String, Object>> history = generateThroughputHistory(metrics, period);

            return Response.ok(Map.of(
                "feedId", feedId,
                "period", period,
                "dataPoints", history.size(),
                "history", history
            )).build();
        });
    }

    // ==================== TRANSACTION APIs ====================

    /**
     * Get token transactions
     * GET /api/v11/feed-tokens/{feedId}/transactions
     */
    @GET
    @Path("/{feedId}/transactions")
    @Operation(summary = "Get transactions", description = "Retrieve token transaction history")
    public Uni<Response> getTransactions(
            @PathParam("feedId") String feedId,
            @QueryParam("limit") @DefaultValue("50") int limit) {

        LOG.infof("Fetching transactions for feed: %s (limit: %d)", feedId, limit);

        return Uni.createFrom().item(() -> {
            List<TokenTransaction> txs = TRANSACTIONS.getOrDefault(feedId, new ArrayList<>())
                .stream()
                .sorted((a, b) -> b.timestamp.compareTo(a.timestamp))
                .limit(limit)
                .collect(Collectors.toList());

            return Response.ok(Map.of(
                "feedId", feedId,
                "totalTransactions", TRANSACTIONS.getOrDefault(feedId, new ArrayList<>()).size(),
                "transactions", txs
            )).build();
        });
    }

    // ==================== STATISTICS APIs ====================

    /**
     * Get overall token statistics
     * GET /api/v11/feed-tokens/stats
     */
    @GET
    @Path("/stats")
    @Operation(summary = "Get token statistics", description = "Get overall token ecosystem statistics")
    public Uni<Response> getTokenStats() {
        LOG.info("Fetching token statistics");

        return Uni.createFrom().item(() -> {
            Map<String, Object> stats = new HashMap<>();

            stats.put("totalTokenTypes", TOKENS.size());
            stats.put("totalSupply", TOKENS.values().stream().mapToLong(t -> t.totalSupply).sum());
            stats.put("totalCirculating", TOKENS.values().stream().mapToLong(t -> t.circulatingSupply).sum());
            stats.put("totalThroughput", TOKENS.values().stream().mapToDouble(t -> t.currentThroughput).sum());
            stats.put("peakThroughput", TOKENS.values().stream().mapToDouble(t -> t.peakThroughput).max().orElse(0.0));
            stats.put("totalTransactions", TRANSACTIONS.values().stream().mapToInt(List::size).sum());

            BigDecimal totalMarketCap = TOKENS.values().stream()
                .map(t -> t.tokenValue.multiply(new BigDecimal(t.circulatingSupply)))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            stats.put("totalMarketCap", totalMarketCap);

            stats.put("timestamp", Instant.now().toString());

            return Response.ok(stats).build();
        });
    }

    // ==================== HELPER METHODS ====================

    private static void initializeFeedTokens() {
        // Market Data Feed Token
        createToken("feed_market_001", "MARKET", "Market Data Token", "Crypto market data feed token");

        // Oracle Feed Token
        createToken("feed_oracle_002", "ORACLE", "Oracle Token", "Chainlink price oracle token");

        // IoT Sensor Feed Token
        createToken("feed_iot_003", "IOT", "IoT Token", "Environmental sensor data token");

        // Weather Feed Token
        createToken("feed_weather_004", "WEATHER", "Weather Token", "Weather data feed token");
    }

    private static void createToken(String feedId, String symbol, String name, String description) {
        FeedToken token = new FeedToken();
        token.feedId = feedId;
        token.tokenSymbol = symbol + "_FEED";
        token.feedName = name;
        token.description = description;
        token.totalSupply = 1_000_000L;
        token.circulatingSupply = 750_000L;
        token.tokenValue = new BigDecimal("10.00");
        token.currentThroughput = 100.0 + (Math.random() * 500);
        token.peakThroughput = token.currentThroughput * 1.5;
        token.createdAt = "2025-10-01T00:00:00Z";
        token.lastMint = Instant.now().toString();

        TOKENS.put(feedId, token);

        // Initialize metrics
        ThroughputMetrics metrics = new ThroughputMetrics();
        metrics.feedId = feedId;
        metrics.currentTPS = token.currentThroughput;
        metrics.peakTPS = token.peakThroughput;
        metrics.averageTPS = token.currentThroughput;
        metrics.dataPoints = 50000L;
        metrics.lastUpdate = Instant.now().toString();

        THROUGHPUT.put(feedId, metrics);
    }

    private long calculateMintAmount(String feedId) {
        ThroughputMetrics metrics = THROUGHPUT.get(feedId);
        if (metrics == null) return 1000L;

        // Mint based on throughput: higher throughput = more tokens
        return (long) (metrics.currentTPS * 10);
    }

    private void updateTokenValue(FeedToken token, ThroughputMetrics metrics) {
        // Token value increases with higher throughput and data quality
        double multiplier = 1.0 + (metrics.currentTPS / 1000.0);
        BigDecimal baseValue = new BigDecimal("10.00");
        token.tokenValue = baseValue.multiply(new BigDecimal(multiplier)).setScale(2, RoundingMode.HALF_UP);
    }

    private List<Map<String, Object>> generateThroughputHistory(ThroughputMetrics metrics, String period) {
        List<Map<String, Object>> history = new ArrayList<>();
        int dataPoints = period.equals("1h") ? 60 : period.equals("24h") ? 24 : 7;
        int intervalMinutes = period.equals("1h") ? 1 : period.equals("24h") ? 60 : 1440;

        Random random = new Random();
        for (int i = dataPoints - 1; i >= 0; i--) {
            Map<String, Object> point = new HashMap<>();
            point.put("timestamp", Instant.now().minusSeconds(i * intervalMinutes * 60).toString());
            point.put("tokensPerSecond", metrics.averageTPS + (random.nextDouble() * 100 - 50));
            point.put("dataPoints", metrics.dataPoints + (random.nextInt(1000) - 500));
            history.add(point);
        }

        return history;
    }

    // ==================== DTOs ====================

    public static class TokensList {
        public int totalTokens;
        public long totalSupply;
        public double totalThroughput;
        public List<FeedToken> tokens;
    }

    public static class FeedToken {
        public String feedId;
        public String tokenSymbol;
        public String feedName;
        public String description;
        public long totalSupply;
        public long circulatingSupply;
        public BigDecimal tokenValue;
        public double currentThroughput;
        public double peakThroughput;
        public String createdAt;
        public String lastMint;
    }

    public static class ThroughputMetrics {
        public String feedId;
        public double currentTPS;
        public double peakTPS;
        public double averageTPS;
        public long dataPoints;
        public String lastUpdate;
    }

    public static class TokenTransaction {
        public String transactionId;
        public String type;
        public String feedId;
        public long amount;
        public String timestamp;
        public String reason;
    }

    public static class MintRequest {
        public Long amount;
    }

    public static class BurnRequest {
        public long amount;
        public String reason;
    }

    public static class ThroughputUpdate {
        public double tokensPerSecond;
        public Long dataPoints;
    }
}
