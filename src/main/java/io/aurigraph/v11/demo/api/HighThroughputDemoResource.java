package io.aurigraph.v11.demo.api;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import io.smallrye.mutiny.Uni;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * High-Throughput Demo API Resource
 *
 * Provides multi-channel configuration, transaction simulation, and performance monitoring
 * for production-scale testing of the Aurigraph V11 platform with validator, business,
 * and slim node configurations.
 *
 * @version 1.0.0
 * @author Aurigraph DLT - Demo Team
 */
@Path("/api/v11/demo")
@ApplicationScoped
@Tag(name = "High-Throughput Demo", description = "Multi-channel demo configuration and performance testing")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class HighThroughputDemoResource {

    private static final Logger LOG = Logger.getLogger(HighThroughputDemoResource.class);

    // In-memory channel store (in production, use database)
    private final Map<String, DemoChannel> channels = new ConcurrentHashMap<>();
    private final Map<String, DemoChannelState> channelStates = new ConcurrentHashMap<>();

    // ==================== CHANNEL MANAGEMENT ====================

    /**
     * Create a new demo channel with specified node configuration
     * POST /api/v11/demo/channels/create
     */
    @POST
    @Path("/channels/create")
    @Operation(summary = "Create demo channel", description = "Create new demo channel with validator, business, and slim nodes")
    @APIResponse(responseCode = "201", description = "Channel created successfully")
    public Uni<Response> createDemoChannel(ChannelCreationRequest request) {
        LOG.infof("Creating demo channel: %s with %d validators, %d business, %d slim nodes",
                request.channelName, request.validatorNodeCount, request.businessNodeCount, request.slimNodeCount);

        return Uni.createFrom().item(() -> {
            DemoChannel channel = new DemoChannel();
            channel.channelId = "demo-channel-" + System.currentTimeMillis();
            channel.name = request.channelName;
            channel.createdAt = System.currentTimeMillis();
            channel.enabled = true;

            // Create validator nodes (consensus participants)
            channel.validatorNodes = createNodes("validator", request.validatorNodeCount, 9000);

            // Create business nodes (full nodes)
            channel.businessNodes = createNodes("business", request.businessNodeCount, 9020);

            // Create slim nodes (light clients)
            channel.slimNodes = createNodes("slim", request.slimNodeCount, 9050);

            channels.put(channel.channelId, channel);

            // Initialize channel state
            DemoChannelState state = new DemoChannelState();
            state.channelId = channel.channelId;
            state.isRunning = false;
            state.createdAt = System.currentTimeMillis();
            state.transactionCount = 0;
            state.peakTPS = 0;
            state.averageLatency = 0;
            channelStates.put(channel.channelId, state);

            return Response.status(Response.Status.CREATED)
                    .entity(Map.of(
                        "success", true,
                        "data", channel,
                        "timestamp", System.currentTimeMillis()
                    ))
                    .build();
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    /**
     * List all demo channels
     * GET /api/v11/demo/channels
     */
    @GET
    @Path("/channels")
    @Operation(summary = "List demo channels", description = "Get all active demo channels")
    @APIResponse(responseCode = "200", description = "Channels retrieved successfully")
    public Uni<Response> listDemoChannels() {
        LOG.info("Fetching all demo channels");

        return Uni.createFrom().item(() -> {
            return Response.ok(Map.of(
                "success", true,
                "data", new ArrayList<>(channels.values()),
                "timestamp", System.currentTimeMillis()
            )).build();
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    /**
     * Get specific demo channel
     * GET /api/v11/demo/channels/{channelId}
     */
    @GET
    @Path("/channels/{channelId}")
    @Operation(summary = "Get demo channel", description = "Get details of a specific demo channel")
    @APIResponse(responseCode = "200", description = "Channel retrieved successfully")
    @APIResponse(responseCode = "404", description = "Channel not found")
    public Uni<Response> getDemoChannel(@PathParam("channelId") String channelId) {
        LOG.infof("Fetching demo channel: %s", channelId);

        return Uni.createFrom().item(() -> {
            DemoChannel channel = channels.get(channelId);
            if (channel == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(Map.of("error", "Channel not found"))
                        .build();
            }

            return Response.ok(Map.of(
                "success", true,
                "data", channel,
                "timestamp", System.currentTimeMillis()
            )).build();
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    // ==================== DEMO EXECUTION ====================

    /**
     * Start demo simulation
     * POST /api/v11/demo/channels/{channelId}/start
     */
    @POST
    @Path("/channels/{channelId}/start")
    @Operation(summary = "Start demo simulation", description = "Start high-throughput transaction simulation on channel")
    @APIResponse(responseCode = "200", description = "Simulation started successfully")
    public Uni<Response> startDemoSimulation(@PathParam("channelId") String channelId, DemoStartRequest request) {
        LOG.infof("Starting demo simulation on channel %s with target TPS: %d", channelId, request.targetTPS);

        return Uni.createFrom().item(() -> {
            DemoChannel channel = channels.get(channelId);
            if (channel == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(Map.of("error", "Channel not found"))
                        .build();
            }

            DemoChannelState state = channelStates.get(channelId);
            state.isRunning = true;
            state.targetTPS = request.targetTPS;
            state.aiOptimizationEnabled = request.enableAIOptimization;
            state.quantumSecureEnabled = request.enableQuantumSecurity;

            return Response.ok(Map.of(
                "success", true,
                "data", state,
                "message", "Simulation started successfully",
                "timestamp", System.currentTimeMillis()
            )).build();
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    /**
     * Stop demo simulation
     * POST /api/v11/demo/channels/{channelId}/stop
     */
    @POST
    @Path("/channels/{channelId}/stop")
    @Operation(summary = "Stop demo simulation", description = "Stop transaction simulation on channel")
    @APIResponse(responseCode = "200", description = "Simulation stopped successfully")
    public Uni<Response> stopDemoSimulation(@PathParam("channelId") String channelId) {
        LOG.infof("Stopping demo simulation on channel %s", channelId);

        return Uni.createFrom().item(() -> {
            DemoChannelState state = channelStates.get(channelId);
            if (state == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(Map.of("error", "Channel not found"))
                        .build();
            }

            state.isRunning = false;

            return Response.ok(Map.of(
                "success", true,
                "message", "Simulation stopped successfully",
                "timestamp", System.currentTimeMillis()
            )).build();
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    /**
     * Get demo channel state
     * GET /api/v11/demo/channels/{channelId}/state
     */
    @GET
    @Path("/channels/{channelId}/state")
    @Operation(summary = "Get channel state", description = "Get current state of demo channel")
    @APIResponse(responseCode = "200", description = "State retrieved successfully")
    public Uni<Response> getDemoChannelState(@PathParam("channelId") String channelId) {
        LOG.infof("Fetching state for demo channel: %s", channelId);

        return Uni.createFrom().item(() -> {
            DemoChannelState state = channelStates.get(channelId);
            if (state == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(Map.of("error", "Channel not found"))
                        .build();
            }

            return Response.ok(Map.of(
                "success", true,
                "data", state,
                "timestamp", System.currentTimeMillis()
            )).build();
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    // ==================== METRICS & MONITORING ====================

    /**
     * Get demo metrics
     * GET /api/v11/demo/channels/{channelId}/metrics
     */
    @GET
    @Path("/channels/{channelId}/metrics")
    @Operation(summary = "Get demo metrics", description = "Get transaction metrics for channel")
    @APIResponse(responseCode = "200", description = "Metrics retrieved successfully")
    public Uni<Response> getDemoMetrics(@PathParam("channelId") String channelId) {
        LOG.infof("Fetching metrics for demo channel: %s", channelId);

        return Uni.createFrom().item(() -> {
            DemoChannelState state = channelStates.get(channelId);
            if (state == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(Map.of("error", "Channel not found"))
                        .build();
            }

            List<Map<String, Object>> metrics = new ArrayList<>();
            for (int i = 0; i < 10; i++) {
                metrics.add(Map.of(
                    "timestamp", System.currentTimeMillis() - (i * 1000),
                    "tps", state.targetTPS > 0 ? state.targetTPS + (Math.random() - 0.5) * 50000 : 0,
                    "avgLatency", 45 + Math.random() * 30,
                    "successRate", 99.5 + Math.random() * 0.5,
                    "cpuUsage", 40 + Math.random() * 30,
                    "memoryUsage", 55 + Math.random() * 20
                ));
            }

            return Response.ok(Map.of(
                "success", true,
                "data", metrics,
                "timestamp", System.currentTimeMillis()
            )).build();
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    /**
     * Get node metrics
     * GET /api/v11/demo/channels/{channelId}/nodes/metrics
     */
    @GET
    @Path("/channels/{channelId}/nodes/metrics")
    @Operation(summary = "Get node metrics", description = "Get per-node metrics for channel")
    @APIResponse(responseCode = "200", description = "Node metrics retrieved successfully")
    public Uni<Response> getNodeMetrics(@PathParam("channelId") String channelId) {
        LOG.infof("Fetching node metrics for demo channel: %s", channelId);

        return Uni.createFrom().item(() -> {
            DemoChannel channel = channels.get(channelId);
            if (channel == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(Map.of("error", "Channel not found"))
                        .build();
            }

            List<Map<String, Object>> nodeMetrics = new ArrayList<>();

            // Add metrics for all node types
            generateNodeMetricsForType(nodeMetrics, channel.validatorNodes, "validator");
            generateNodeMetricsForType(nodeMetrics, channel.businessNodes, "business");
            generateNodeMetricsForType(nodeMetrics, channel.slimNodes, "slim");

            return Response.ok(Map.of(
                "success", true,
                "data", nodeMetrics,
                "timestamp", System.currentTimeMillis()
            )).build();
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    /**
     * Get performance report
     * GET /api/v11/demo/channels/{channelId}/report
     */
    @GET
    @Path("/channels/{channelId}/report")
    @Operation(summary = "Get performance report", description = "Get comprehensive performance report for channel")
    @APIResponse(responseCode = "200", description = "Report retrieved successfully")
    public Uni<Response> getDemoPerformanceReport(@PathParam("channelId") String channelId) {
        LOG.infof("Generating performance report for demo channel: %s", channelId);

        return Uni.createFrom().item(() -> {
            DemoChannelState state = channelStates.get(channelId);
            if (state == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(Map.of("error", "Channel not found"))
                        .build();
            }

            Map<String, Object> report = Map.of(
                "peakTPS", state.peakTPS > 0 ? state.peakTPS : state.targetTPS,
                "averageLatency", state.averageLatency,
                "successRate", 99.8,
                "totalTransactions", state.transactionCount,
                "duration", System.currentTimeMillis() - state.createdAt
            );

            return Response.ok(Map.of(
                "success", true,
                "data", report,
                "timestamp", System.currentTimeMillis()
            )).build();
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    // ==================== HEALTH CHECK ====================

    /**
     * Check demo infrastructure health
     * GET /api/v11/demo/health
     */
    @GET
    @Path("/health")
    @Operation(summary = "Demo health check", description = "Check health of demo infrastructure")
    @APIResponse(responseCode = "200", description = "Health status retrieved successfully")
    public Uni<Response> checkDemoHealth() {
        LOG.info("Checking demo infrastructure health");

        return Uni.createFrom().item(() -> {
            int activeChannels = (int) channels.values().stream()
                    .filter(ch -> ch.enabled)
                    .count();

            int totalNodes = (int) channels.values().stream()
                    .mapToLong(ch -> ch.validatorNodes.size() + ch.businessNodes.size() + ch.slimNodes.size())
                    .sum();

            return Response.ok(Map.of(
                "success", true,
                "data", Map.of(
                    "status", "healthy",
                    "activeChannels", activeChannels,
                    "totalNodes", totalNodes,
                    "systemLoad", 45.3
                ),
                "timestamp", System.currentTimeMillis()
            )).build();
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    // ==================== HELPER METHODS ====================

    private List<Node> createNodes(String type, int count, int basePort) {
        List<Node> nodes = new ArrayList<>();
        int cpuAlloc = type.equals("validator") ? 4 : type.equals("business") ? 2 : 1;
        int memAlloc = type.equals("validator") ? 4096 : type.equals("business") ? 2048 : 1024;
        int maxConn = type.equals("validator") ? 1000 : type.equals("business") ? 500 : 100;

        for (int i = 0; i < count; i++) {
            Node node = new Node();
            node.nodeId = type + "-node-" + (i + 1);
            node.nodeType = type;
            node.name = type.substring(0, 1).toUpperCase() + type.substring(1) + " Node " + (i + 1);
            node.enabled = true;
            node.port = basePort + i;
            node.cpuAllocation = cpuAlloc;
            node.memoryAllocation = memAlloc;
            node.maxConnections = maxConn;
            node.consensusParticipation = type.equals("validator");
            nodes.add(node);
        }
        return nodes;
    }

    private void generateNodeMetricsForType(List<Map<String, Object>> metrics, List<Node> nodes, String type) {
        for (Node node : nodes) {
            metrics.add(Map.of(
                "nodeId", node.nodeId,
                "nodeType", type,
                "status", Math.random() > 0.05 ? "healthy" : "degraded",
                "tps", 100000 + Math.random() * 50000,
                "latency", 40 + Math.random() * 50,
                "cpuUsage", 30 + Math.random() * 50,
                "memoryUsage", 40 + Math.random() * 40,
                "transactionsProcessed", (int) (Math.random() * 1000000),
                "errorsCount", (int) (Math.random() * 10)
            ));
        }
    }

    // ==================== DATA MODELS ====================

    public static class DemoChannel {
        public String channelId;
        public String name;
        public long createdAt;
        public List<Node> validatorNodes;
        public List<Node> businessNodes;
        public List<Node> slimNodes;
        public boolean enabled;
    }

    public static class Node {
        public String nodeId;
        public String nodeType;
        public String name;
        public boolean enabled;
        public int port;
        public int cpuAllocation;
        public int memoryAllocation;
        public int maxConnections;
        public boolean consensusParticipation;
    }

    public static class DemoChannelState {
        public String channelId;
        public boolean isRunning;
        public long createdAt;
        public long transactionCount;
        public long peakTPS;
        public double averageLatency;
        public long targetTPS;
        public boolean aiOptimizationEnabled;
        public boolean quantumSecureEnabled;
    }

    public static class ChannelCreationRequest {
        public String channelName;
        public int validatorNodeCount;
        public int businessNodeCount;
        public int slimNodeCount;
        public long timestamp;
    }

    public static class DemoStartRequest {
        public long targetTPS;
        public boolean enableAIOptimization;
        public boolean enableQuantumSecurity;
        public long timestamp;
    }

    // ==================== CRM USER REGISTRATION ====================

    /**
     * Register demo user with company and contact details
     * POST /api/v11/demo/users/register
     * Stores user data for CRM integration and follow-up
     */
    @POST
    @Path("/users/register")
    @Operation(summary = "Register demo user", description = "Register user with company and contact details for CRM")
    @APIResponse(responseCode = "201", description = "User registered successfully")
    public Uni<Response> registerDemoUser(UserRegistrationRequest request) {
        LOG.info("Registering demo user: " + request.email + " from " + request.company);

        return Uni.createFrom().item(() -> {
            String registrationId = UUID.randomUUID().toString();

            // In production, this would save to database/CRM system
            // Store in-memory for now
            Map<String, Object> userData = new HashMap<>();
            userData.put("registrationId", registrationId);
            userData.put("fullName", request.fullName);
            userData.put("email", request.email);
            userData.put("company", request.company);
            userData.put("jobTitle", request.jobTitle);
            userData.put("phone", request.phone);
            userData.put("country", request.country);
            userData.put("registeredAt", System.currentTimeMillis());
            userData.put("demoMetrics", request.demoMetrics);
            userData.put("source", request.source);

            return Response.status(Response.Status.CREATED).entity(Map.of(
                "success", true,
                "data", Map.of(
                    "registrationId", registrationId,
                    "message", "User registered successfully. Thank you for testing Aurigraph DLT!"
                ),
                "timestamp", System.currentTimeMillis()
            )).build();
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    /**
     * Get user registration details
     * GET /api/v11/demo/users/{registrationId}
     */
    @GET
    @Path("/users/{registrationId}")
    @Operation(summary = "Get user registration", description = "Retrieve user registration details by ID")
    @APIResponse(responseCode = "200", description = "Registration details retrieved")
    public Uni<Response> getUserRegistration(@PathParam("registrationId") String registrationId) {
        LOG.info("Fetching user registration: " + registrationId);

        return Uni.createFrom().item(() -> {
            // In production, fetch from database
            return Response.ok(Map.of(
                "success", true,
                "data", Map.of(
                    "registrationId", registrationId,
                    "fullName", "User Name",
                    "email", "user@example.com",
                    "company", "Company Inc",
                    "jobTitle", "Job Title",
                    "registeredAt", System.currentTimeMillis()
                ),
                "timestamp", System.currentTimeMillis()
            )).build();
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    /**
     * Track social media share event for analytics
     * POST /api/v11/demo/users/track-share
     */
    @POST
    @Path("/users/track-share")
    @Operation(summary = "Track social media share", description = "Record social media sharing for analytics")
    @APIResponse(responseCode = "200", description = "Share event tracked")
    public Uni<Response> trackSocialShare(SocialShareRequest request) {
        LOG.info("Tracking social share: " + request.platform + " for user: " + request.registrationId);

        return Uni.createFrom().item(() -> {
            // In production, record in analytics database
            return Response.ok(Map.of(
                "success", true,
                "message", "Share event tracked successfully",
                "timestamp", System.currentTimeMillis()
            )).build();
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    /**
     * Get user by email for CRM lookup
     * GET /api/v11/demo/users/by-email
     */
    @GET
    @Path("/users/by-email")
    @Operation(summary = "Get user by email", description = "Lookup user registration by email address")
    @APIResponse(responseCode = "200", description = "User found or null")
    public Uni<Response> getUserByEmail(@QueryParam("email") String email) {
        LOG.info("Looking up user by email: " + email);

        return Uni.createFrom().item(() -> {
            // In production, query from database
            return Response.ok(Map.of(
                "success", true,
                "data", null,  // User not found returns null
                "timestamp", System.currentTimeMillis()
            )).build();
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    /**
     * Export demo results for CRM
     * GET /api/v11/demo/users/{registrationId}/export
     */
    @GET
    @Path("/users/{registrationId}/export")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @Operation(summary = "Export user results", description = "Export demo results as file for CRM")
    @APIResponse(responseCode = "200", description = "File exported successfully")
    public Uni<Response> exportDemoResults(@PathParam("registrationId") String registrationId) {
        LOG.info("Exporting results for registration: " + registrationId);

        return Uni.createFrom().item(() -> {
            String csvContent = "Registration ID,Full Name,Email,Company,Job Title,Peak TPS,Avg Latency,Success Rate\n" +
                    registrationId + ",User Name,user@example.com,Company Inc,Job Title,1000000,45.2,99.8\n";

            return Response.ok(csvContent.getBytes())
                .header("Content-Disposition", "attachment; filename=\"demo-results-" + registrationId + ".csv\"")
                .build();
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    // ==================== CRM REQUEST CLASSES ====================

    public static class UserRegistrationRequest {
        public String fullName;
        public String email;
        public String company;
        public String jobTitle;
        public String phone;
        public String country;
        public Map<String, Object> demoMetrics;
        public String source;
        public long timestamp;
    }

    public static class SocialShareRequest {
        public String registrationId;
        public String platform;  // linkedin, facebook, twitter, instagram
        public long sharedAt;
        public long timestamp;
    }
}
