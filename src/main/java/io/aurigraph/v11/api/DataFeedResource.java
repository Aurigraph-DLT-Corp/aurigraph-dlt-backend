package io.aurigraph.v11.api;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * External Data Feed API Resource
 *
 * Provides data feed management for slim agents including:
 * - Feed creation and management
 * - Agent subscriptions
 * - Real-time data streaming
 * - Feed validation and filtering
 *
 * @author Backend Development Agent (BDA)
 * @version 1.0.0
 * @since October 2025
 */
@Path("/api/v11/datafeeds")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Data Feed API", description = "External data feed management for slim agents")
public class DataFeedResource {

    private static final Logger LOG = Logger.getLogger(DataFeedResource.class);

    // In-memory storage for demo (replace with database in production)
    private static final Map<String, DataFeed> FEEDS = new ConcurrentHashMap<>();
    private static final Map<String, List<AgentSubscription>> SUBSCRIPTIONS = new ConcurrentHashMap<>();

    static {
        // Initialize with sample feeds
        initializeSampleFeeds();
    }

    // ==================== FEED MANAGEMENT APIs ====================

    /**
     * Get all data feeds
     * GET /api/v11/datafeeds
     */
    @GET
    @Operation(summary = "List all data feeds", description = "Retrieve list of all external data feeds")
    public Uni<DataFeedsList> getAllFeeds(
            @QueryParam("status") String status,
            @QueryParam("type") String type,
            @QueryParam("limit") @DefaultValue("50") int limit) {

        LOG.infof("Fetching data feeds (status: %s, type: %s, limit: %d)", status, type, limit);

        return Uni.createFrom().item(() -> {
            DataFeedsList list = new DataFeedsList();

            List<DataFeed> feeds = FEEDS.values().stream()
                .filter(f -> status == null || f.status.equalsIgnoreCase(status))
                .filter(f -> type == null || f.type.equalsIgnoreCase(type))
                .limit(limit)
                .collect(Collectors.toList());

            list.totalFeeds = FEEDS.size();
            list.activeFeeds = (int) FEEDS.values().stream().filter(f -> "ACTIVE".equals(f.status)).count();
            list.feeds = feeds;

            return list;
        });
    }

    /**
     * Get specific data feed
     * GET /api/v11/datafeeds/{id}
     */
    @GET
    @Path("/{id}")
    @Operation(summary = "Get feed details", description = "Retrieve detailed information about a specific data feed")
    public Uni<Response> getFeedDetails(@PathParam("id") String feedId) {
        LOG.infof("Fetching feed details: %s", feedId);

        return Uni.createFrom().item(() -> {
            DataFeed feed = FEEDS.get(feedId);
            if (feed == null) {
                return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("error", "Feed not found", "feedId", feedId))
                    .build();
            }
            return Response.ok(feed).build();
        });
    }

    /**
     * Create new data feed
     * POST /api/v11/datafeeds
     */
    @POST
    @Operation(summary = "Create data feed", description = "Create a new external data feed")
    public Uni<Response> createFeed(DataFeedRequest request) {
        LOG.infof("Creating new data feed: %s (%s)", request.name, request.type);

        return Uni.createFrom().item(() -> {
            String feedId = "feed_" + UUID.randomUUID().toString().substring(0, 8);

            DataFeed feed = new DataFeed();
            feed.feedId = feedId;
            feed.name = request.name;
            feed.type = request.type;
            feed.source = request.source;
            feed.status = "ACTIVE";
            feed.endpoint = request.endpoint;
            feed.updateFrequency = request.updateFrequency != null ? request.updateFrequency : "1m";
            feed.dataFormat = request.dataFormat != null ? request.dataFormat : "JSON";
            feed.subscribedAgents = 0;
            feed.totalDataPoints = 0L;
            feed.lastUpdate = Instant.now().toString();
            feed.createdAt = Instant.now().toString();
            feed.healthStatus = "HEALTHY";
            feed.latency = 0;

            FEEDS.put(feedId, feed);

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("feedId", feedId);
            response.put("message", "Data feed created successfully");
            response.put("feed", feed);

            return Response.status(Response.Status.CREATED).entity(response).build();
        });
    }

    /**
     * Update data feed
     * PUT /api/v11/datafeeds/{id}
     */
    @PUT
    @Path("/{id}")
    @Operation(summary = "Update data feed", description = "Update an existing data feed configuration")
    public Uni<Response> updateFeed(@PathParam("id") String feedId, DataFeedRequest request) {
        LOG.infof("Updating data feed: %s", feedId);

        return Uni.createFrom().item(() -> {
            DataFeed feed = FEEDS.get(feedId);
            if (feed == null) {
                return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("error", "Feed not found"))
                    .build();
            }

            if (request.name != null) feed.name = request.name;
            if (request.source != null) feed.source = request.source;
            if (request.endpoint != null) feed.endpoint = request.endpoint;
            if (request.updateFrequency != null) feed.updateFrequency = request.updateFrequency;
            if (request.dataFormat != null) feed.dataFormat = request.dataFormat;

            return Response.ok(Map.of("status", "success", "feed", feed)).build();
        });
    }

    /**
     * Delete data feed
     * DELETE /api/v11/datafeeds/{id}
     */
    @DELETE
    @Path("/{id}")
    @Operation(summary = "Delete data feed", description = "Delete a data feed and unsubscribe all agents")
    public Uni<Response> deleteFeed(@PathParam("id") String feedId) {
        LOG.infof("Deleting data feed: %s", feedId);

        return Uni.createFrom().item(() -> {
            DataFeed removed = FEEDS.remove(feedId);
            if (removed == null) {
                return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("error", "Feed not found"))
                    .build();
            }

            SUBSCRIPTIONS.remove(feedId);

            return Response.ok(Map.of(
                "status", "success",
                "message", "Feed deleted successfully",
                "feedId", feedId
            )).build();
        });
    }

    // ==================== AGENT SUBSCRIPTION APIs ====================

    /**
     * Subscribe agent to feed
     * POST /api/v11/datafeeds/{id}/subscribe
     */
    @POST
    @Path("/{id}/subscribe")
    @Operation(summary = "Subscribe agent", description = "Subscribe a slim agent to a data feed")
    public Uni<Response> subscribeAgent(@PathParam("id") String feedId, SubscriptionRequest request) {
        LOG.infof("Subscribing agent %s to feed %s", request.agentId, feedId);

        return Uni.createFrom().item(() -> {
            DataFeed feed = FEEDS.get(feedId);
            if (feed == null) {
                return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("error", "Feed not found"))
                    .build();
            }

            AgentSubscription subscription = new AgentSubscription();
            subscription.subscriptionId = "sub_" + UUID.randomUUID().toString().substring(0, 8);
            subscription.agentId = request.agentId;
            subscription.agentName = request.agentName;
            subscription.feedId = feedId;
            subscription.filters = request.filters;
            subscription.status = "ACTIVE";
            subscription.subscribedAt = Instant.now().toString();
            subscription.dataReceived = 0L;

            SUBSCRIPTIONS.computeIfAbsent(feedId, k -> new ArrayList<>()).add(subscription);
            feed.subscribedAgents++;

            return Response.ok(Map.of(
                "status", "success",
                "message", "Agent subscribed successfully",
                "subscription", subscription
            )).build();
        });
    }

    /**
     * Unsubscribe agent from feed
     * POST /api/v11/datafeeds/{id}/unsubscribe
     */
    @POST
    @Path("/{id}/unsubscribe")
    @Operation(summary = "Unsubscribe agent", description = "Unsubscribe a slim agent from a data feed")
    public Uni<Response> unsubscribeAgent(@PathParam("id") String feedId, @QueryParam("agentId") String agentId) {
        LOG.infof("Unsubscribing agent %s from feed %s", agentId, feedId);

        return Uni.createFrom().item(() -> {
            List<AgentSubscription> subs = SUBSCRIPTIONS.get(feedId);
            if (subs == null) {
                return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("error", "Feed not found"))
                    .build();
            }

            boolean removed = subs.removeIf(s -> s.agentId.equals(agentId));
            if (removed) {
                DataFeed feed = FEEDS.get(feedId);
                if (feed != null) feed.subscribedAgents--;

                return Response.ok(Map.of(
                    "status", "success",
                    "message", "Agent unsubscribed successfully"
                )).build();
            }

            return Response.status(Response.Status.NOT_FOUND)
                .entity(Map.of("error", "Subscription not found"))
                .build();
        });
    }

    /**
     * Get feed subscriptions
     * GET /api/v11/datafeeds/{id}/subscriptions
     */
    @GET
    @Path("/{id}/subscriptions")
    @Operation(summary = "Get subscriptions", description = "Get all agent subscriptions for a data feed")
    public Uni<Response> getFeedSubscriptions(@PathParam("id") String feedId) {
        LOG.infof("Fetching subscriptions for feed: %s", feedId);

        return Uni.createFrom().item(() -> {
            List<AgentSubscription> subs = SUBSCRIPTIONS.getOrDefault(feedId, new ArrayList<>());

            return Response.ok(Map.of(
                "feedId", feedId,
                "totalSubscriptions", subs.size(),
                "subscriptions", subs
            )).build();
        });
    }

    // ==================== DATA INGESTION APIs ====================

    /**
     * Push data to feed
     * POST /api/v11/datafeeds/{id}/data
     */
    @POST
    @Path("/{id}/data")
    @Operation(summary = "Push data", description = "Push new data to a feed (distributes to subscribed agents)")
    public Uni<Response> pushData(@PathParam("id") String feedId, DataPushRequest request) {
        LOG.infof("Pushing data to feed: %s (%d data points)", feedId, request.data.size());

        return Uni.createFrom().item(() -> {
            DataFeed feed = FEEDS.get(feedId);
            if (feed == null) {
                return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("error", "Feed not found"))
                    .build();
            }

            // Update feed statistics
            feed.totalDataPoints += request.data.size();
            feed.lastUpdate = Instant.now().toString();

            // Distribute to subscribed agents
            List<AgentSubscription> subs = SUBSCRIPTIONS.getOrDefault(feedId, new ArrayList<>());
            subs.forEach(sub -> sub.dataReceived += request.data.size());

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("feedId", feedId);
            response.put("dataPointsReceived", request.data.size());
            response.put("agentsNotified", subs.size());
            response.put("timestamp", Instant.now().toString());

            return Response.ok(response).build();
        });
    }

    /**
     * Get feed data (for agents)
     * GET /api/v11/datafeeds/{id}/data
     */
    @GET
    @Path("/{id}/data")
    @Operation(summary = "Get feed data", description = "Retrieve latest data from feed")
    public Uni<Response> getFeedData(
            @PathParam("id") String feedId,
            @QueryParam("limit") @DefaultValue("100") int limit,
            @QueryParam("since") String since) {

        LOG.infof("Fetching data for feed: %s (limit: %d)", feedId, limit);

        return Uni.createFrom().item(() -> {
            DataFeed feed = FEEDS.get(feedId);
            if (feed == null) {
                return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("error", "Feed not found"))
                    .build();
            }

            // Generate sample data based on feed type
            List<Map<String, Object>> data = generateSampleData(feed.type, limit);

            return Response.ok(Map.of(
                "feedId", feedId,
                "dataPoints", data.size(),
                "data", data,
                "timestamp", Instant.now().toString()
            )).build();
        });
    }

    // ==================== MONITORING APIs ====================

    /**
     * Get feed statistics
     * GET /api/v11/datafeeds/stats
     */
    @GET
    @Path("/stats")
    @Operation(summary = "Get statistics", description = "Get overall data feed system statistics")
    public Uni<Response> getStats() {
        LOG.info("Fetching data feed statistics");

        return Uni.createFrom().item(() -> {
            Map<String, Object> stats = new HashMap<>();
            stats.put("totalFeeds", FEEDS.size());
            stats.put("activeFeeds", FEEDS.values().stream().filter(f -> "ACTIVE".equals(f.status)).count());
            stats.put("totalSubscriptions", SUBSCRIPTIONS.values().stream().mapToInt(List::size).sum());
            stats.put("totalDataPoints", FEEDS.values().stream().mapToLong(f -> f.totalDataPoints).sum());

            Map<String, Long> feedsByType = FEEDS.values().stream()
                .collect(Collectors.groupingBy(f -> f.type, Collectors.counting()));
            stats.put("feedsByType", feedsByType);

            stats.put("timestamp", Instant.now().toString());

            return Response.ok(stats).build();
        });
    }

    // ==================== HELPER METHODS ====================

    private static void initializeSampleFeeds() {
        // Market Data Feed
        DataFeed marketFeed = new DataFeed();
        marketFeed.feedId = "feed_market_001";
        marketFeed.name = "Crypto Market Data";
        marketFeed.type = "MARKET_DATA";
        marketFeed.source = "CoinGecko API";
        marketFeed.endpoint = "https://api.coingecko.com/api/v3/simple/price";
        marketFeed.status = "ACTIVE";
        marketFeed.updateFrequency = "30s";
        marketFeed.dataFormat = "JSON";
        marketFeed.subscribedAgents = 5;
        marketFeed.totalDataPoints = 15_234L;
        marketFeed.lastUpdate = Instant.now().minusSeconds(25).toString();
        marketFeed.createdAt = "2025-10-01T00:00:00Z";
        marketFeed.healthStatus = "HEALTHY";
        marketFeed.latency = 125;
        FEEDS.put(marketFeed.feedId, marketFeed);

        // Oracle Feed
        DataFeed oracleFeed = new DataFeed();
        oracleFeed.feedId = "feed_oracle_002";
        oracleFeed.name = "Chainlink Price Oracle";
        oracleFeed.type = "ORACLE";
        oracleFeed.source = "Chainlink Network";
        oracleFeed.endpoint = "https://data.chain.link/feeds";
        oracleFeed.status = "ACTIVE";
        oracleFeed.updateFrequency = "1m";
        oracleFeed.dataFormat = "JSON";
        oracleFeed.subscribedAgents = 12;
        oracleFeed.totalDataPoints = 45_678L;
        oracleFeed.lastUpdate = Instant.now().minusSeconds(45).toString();
        oracleFeed.createdAt = "2025-09-15T00:00:00Z";
        oracleFeed.healthStatus = "HEALTHY";
        oracleFeed.latency = 89;
        FEEDS.put(oracleFeed.feedId, oracleFeed);

        // IoT Sensor Feed
        DataFeed iotFeed = new DataFeed();
        iotFeed.feedId = "feed_iot_003";
        iotFeed.name = "IoT Environmental Sensors";
        iotFeed.type = "IOT_SENSOR";
        iotFeed.source = "AWS IoT Core";
        iotFeed.endpoint = "mqtt://iot.amazonaws.com/sensors";
        iotFeed.status = "ACTIVE";
        iotFeed.updateFrequency = "10s";
        iotFeed.dataFormat = "JSON";
        iotFeed.subscribedAgents = 8;
        iotFeed.totalDataPoints = 128_456L;
        iotFeed.lastUpdate = Instant.now().minusSeconds(8).toString();
        iotFeed.createdAt = "2025-09-20T00:00:00Z";
        iotFeed.healthStatus = "HEALTHY";
        iotFeed.latency = 45;
        FEEDS.put(iotFeed.feedId, iotFeed);

        // Weather Feed
        DataFeed weatherFeed = new DataFeed();
        weatherFeed.feedId = "feed_weather_004";
        weatherFeed.name = "Weather Data API";
        weatherFeed.type = "WEATHER";
        weatherFeed.source = "OpenWeather";
        weatherFeed.endpoint = "https://api.openweathermap.org/data/2.5/weather";
        weatherFeed.status = "ACTIVE";
        weatherFeed.updateFrequency = "5m";
        weatherFeed.dataFormat = "JSON";
        weatherFeed.subscribedAgents = 3;
        weatherFeed.totalDataPoints = 8_921L;
        weatherFeed.lastUpdate = Instant.now().minusSeconds(280).toString();
        weatherFeed.createdAt = "2025-10-05T00:00:00Z";
        weatherFeed.healthStatus = "HEALTHY";
        weatherFeed.latency = 210;
        FEEDS.put(weatherFeed.feedId, weatherFeed);
    }

    private List<Map<String, Object>> generateSampleData(String feedType, int limit) {
        List<Map<String, Object>> data = new ArrayList<>();
        Random random = new Random();

        for (int i = 0; i < limit; i++) {
            Map<String, Object> dataPoint = new HashMap<>();
            dataPoint.put("timestamp", Instant.now().minusSeconds(i * 30).toString());

            switch (feedType) {
                case "MARKET_DATA":
                    dataPoint.put("symbol", "BTC/USD");
                    dataPoint.put("price", 45000 + random.nextDouble() * 5000);
                    dataPoint.put("volume", random.nextInt(1000000));
                    break;
                case "ORACLE":
                    dataPoint.put("asset", "ETH");
                    dataPoint.put("price", 3000 + random.nextDouble() * 500);
                    dataPoint.put("confidence", 95 + random.nextDouble() * 5);
                    break;
                case "IOT_SENSOR":
                    dataPoint.put("temperature", 20 + random.nextDouble() * 15);
                    dataPoint.put("humidity", 40 + random.nextDouble() * 40);
                    dataPoint.put("sensorId", "sensor_" + random.nextInt(10));
                    break;
                case "WEATHER":
                    dataPoint.put("temperature", 15 + random.nextDouble() * 20);
                    dataPoint.put("conditions", random.nextBoolean() ? "Clear" : "Cloudy");
                    dataPoint.put("windSpeed", random.nextDouble() * 30);
                    break;
                default:
                    dataPoint.put("value", random.nextDouble() * 100);
            }

            data.add(dataPoint);
        }

        return data;
    }

    // ==================== DTOs ====================

    public static class DataFeedsList {
        public int totalFeeds;
        public int activeFeeds;
        public List<DataFeed> feeds;
    }

    public static class DataFeed {
        public String feedId;
        public String name;
        public String type;
        public String source;
        public String endpoint;
        public String status;
        public String updateFrequency;
        public String dataFormat;
        public int subscribedAgents;
        public Long totalDataPoints;
        public String lastUpdate;
        public String createdAt;
        public String healthStatus;
        public Integer latency;
    }

    public static class DataFeedRequest {
        public String name;
        public String type;
        public String source;
        public String endpoint;
        public String updateFrequency;
        public String dataFormat;
    }

    public static class AgentSubscription {
        public String subscriptionId;
        public String agentId;
        public String agentName;
        public String feedId;
        public Map<String, String> filters;
        public String status;
        public String subscribedAt;
        public Long dataReceived;
    }

    public static class SubscriptionRequest {
        public String agentId;
        public String agentName;
        public Map<String, String> filters;
    }

    public static class DataPushRequest {
        public List<Map<String, Object>> data;
    }
}
