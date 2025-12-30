package io.aurigraph.v11.api;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import io.smallrye.mutiny.Uni;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Node Management API Resource
 *
 * Provides endpoints for managing and monitoring network nodes.
 * Part of Enterprise Portal V4.8.0 implementation.
 *
 * Endpoints:
 * - GET /api/v11/nodes - List all network nodes with status and metrics
 *
 * @author Aurigraph V11 Team
 * @version 4.8.0
 */
@Path("/api/v11/nodes")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Node Management", description = "Network node monitoring and management")
public class NodeManagementResource {

    private static final Logger LOG = Logger.getLogger(NodeManagementResource.class);

    /**
     * GET /api/v11/nodes
     *
     * Returns list of network nodes with status and resource usage.
     *
     * Query Parameters:
     * - page: Page number (default: 0)
     * - size: Page size (default: 20)
     * - status: Filter by status (ACTIVE, SYNCING, OFFLINE)
     * - type: Filter by node type (VALIDATOR, API, BUSINESS, CHANNEL)
     *
     * Response includes:
     * - nodeId: Unique node identifier
     * - nodeType: Type of node (VALIDATOR, API, etc.)
     * - status: Current status (ACTIVE, SYNCING, OFFLINE)
     * - version: Software version
     * - uptime: Node uptime in seconds
     * - resourceUsage: CPU, memory, disk usage
     * - syncProgress: Blockchain synchronization percentage
     * - lastSeen: Last heartbeat timestamp
     * - location: Geographic location data
     */
    @GET
    @Operation(
        summary = "List network nodes",
        description = "Returns paginated list of all network nodes with status, metrics, and resource usage"
    )
    @APIResponses({
        @APIResponse(
            responseCode = "200",
            description = "Nodes retrieved successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = NodeListResponse.class)
            )
        ),
        @APIResponse(
            responseCode = "400",
            description = "Invalid query parameters"
        ),
        @APIResponse(
            responseCode = "500",
            description = "Internal server error"
        )
    })
    public Uni<Response> getNodes(
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("20") int size,
            @QueryParam("status") String statusFilter,
            @QueryParam("type") String typeFilter) {

        return Uni.createFrom().item(() -> {
            try {
                LOG.infof("GET /api/v11/nodes - page=%d, size=%d, status=%s, type=%s",
                    page, size, statusFilter, typeFilter);

                // Validate pagination parameters
                if (page < 0 || size < 1 || size > 100) {
                    return Response.status(Response.Status.BAD_REQUEST)
                        .entity(Map.of("error", "Invalid pagination parameters"))
                        .build();
                }

                // Generate mock node data (in production, fetch from database/service)
                List<NodeInfo> allNodes = generateMockNodes();

                // Apply filters
                List<NodeInfo> filteredNodes = allNodes.stream()
                    .filter(node -> statusFilter == null || node.status().equalsIgnoreCase(statusFilter))
                    .filter(node -> typeFilter == null || node.nodeType().equalsIgnoreCase(typeFilter))
                    .toList();

                // Calculate pagination
                int totalNodes = filteredNodes.size();
                int totalPages = (int) Math.ceil((double) totalNodes / size);
                int fromIndex = page * size;
                int toIndex = Math.min(fromIndex + size, totalNodes);

                List<NodeInfo> paginatedNodes = filteredNodes.subList(
                    Math.min(fromIndex, totalNodes),
                    Math.min(toIndex, totalNodes)
                );

                // Build response
                NodeListResponse response = new NodeListResponse(
                    paginatedNodes,
                    totalNodes,
                    page,
                    size,
                    totalPages,
                    page + 1 < totalPages,
                    page > 0,
                    System.currentTimeMillis()
                );

                LOG.infof("Returning %d nodes (total: %d, filtered: %d)",
                    paginatedNodes.size(), allNodes.size(), totalNodes);

                return Response.ok(response).build();

            } catch (Exception e) {
                LOG.errorf(e, "Failed to retrieve nodes");
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Failed to retrieve nodes", "message", e.getMessage()))
                    .build();
            }
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    /**
     * GET /api/v11/nodes/{nodeId}
     *
     * Returns detailed information about a specific node.
     */
    @GET
    @Path("/{nodeId}")
    @Operation(
        summary = "Get node details",
        description = "Returns detailed information about a specific network node"
    )
    @APIResponses({
        @APIResponse(responseCode = "200", description = "Node retrieved successfully"),
        @APIResponse(responseCode = "404", description = "Node not found"),
        @APIResponse(responseCode = "500", description = "Internal server error")
    })
    public Uni<Response> getNode(@PathParam("nodeId") String nodeId) {
        return Uni.createFrom().item(() -> {
            try {
                LOG.infof("GET /api/v11/nodes/%s", nodeId);

                // Find node in mock data
                List<NodeInfo> nodes = generateMockNodes();
                NodeInfo node = nodes.stream()
                    .filter(n -> n.nodeId().equals(nodeId))
                    .findFirst()
                    .orElse(null);

                if (node == null) {
                    return Response.status(Response.Status.NOT_FOUND)
                        .entity(Map.of("error", "Node not found", "nodeId", nodeId))
                        .build();
                }

                return Response.ok(node).build();

            } catch (Exception e) {
                LOG.errorf(e, "Failed to retrieve node: %s", nodeId);
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Failed to retrieve node", "message", e.getMessage()))
                    .build();
            }
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    /**
     * GET /api/v11/nodes/summary
     *
     * Returns summary statistics for all nodes.
     */
    @GET
    @Path("/summary")
    @Operation(
        summary = "Get nodes summary",
        description = "Returns summary statistics including node counts by type and status"
    )
    @APIResponse(responseCode = "200", description = "Summary retrieved successfully")
    public Uni<Response> getNodesSummary() {
        return Uni.createFrom().item(() -> {
            try {
                LOG.info("GET /api/v11/nodes/summary");

                List<NodeInfo> nodes = generateMockNodes();

                // Calculate summary statistics
                long totalNodes = nodes.size();
                long activeNodes = nodes.stream().filter(n -> "ACTIVE".equals(n.status())).count();
                long syncingNodes = nodes.stream().filter(n -> "SYNCING".equals(n.status())).count();
                long offlineNodes = nodes.stream().filter(n -> "OFFLINE".equals(n.status())).count();

                long validators = nodes.stream().filter(n -> "VALIDATOR".equals(n.nodeType())).count();
                long apiNodes = nodes.stream().filter(n -> "API".equals(n.nodeType())).count();
                long businessNodes = nodes.stream().filter(n -> "BUSINESS".equals(n.nodeType())).count();
                long channelNodes = nodes.stream().filter(n -> "CHANNEL".equals(n.nodeType())).count();

                double avgSyncProgress = nodes.stream()
                    .mapToDouble(NodeInfo::syncProgress)
                    .average()
                    .orElse(0.0);

                Map<String, Object> summary = new HashMap<>();
                summary.put("totalNodes", totalNodes);
                summary.put("activeNodes", activeNodes);
                summary.put("syncingNodes", syncingNodes);
                summary.put("offlineNodes", offlineNodes);
                summary.put("byType", Map.of(
                    "validators", validators,
                    "apiNodes", apiNodes,
                    "businessNodes", businessNodes,
                    "channelNodes", channelNodes
                ));
                summary.put("byStatus", Map.of(
                    "active", activeNodes,
                    "syncing", syncingNodes,
                    "offline", offlineNodes
                ));
                summary.put("averageSyncProgress", avgSyncProgress);
                summary.put("timestamp", System.currentTimeMillis());

                return Response.ok(summary).build();

            } catch (Exception e) {
                LOG.errorf(e, "Failed to retrieve nodes summary");
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Failed to retrieve summary", "message", e.getMessage()))
                    .build();
            }
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    // ==================== HELPER METHODS ====================

    /**
     * Generate mock node data for demonstration.
     * In production, this would fetch from a database or node registry service.
     */
    private List<NodeInfo> generateMockNodes() {
        List<NodeInfo> nodes = new ArrayList<>();
        long currentTime = System.currentTimeMillis();

        // Add validator nodes
        for (int i = 0; i < 127; i++) {
            String nodeId = "validator-" + String.format("%03d", i);
            String status = i < 121 ? "ACTIVE" : (i < 125 ? "SYNCING" : "OFFLINE");
            double syncProgress = status.equals("ACTIVE") ? 100.0 : (95.0 + (i % 5));

            nodes.add(new NodeInfo(
                nodeId,
                "VALIDATOR",
                status,
                "12.0.0",
                (long) (Math.random() * 30 * 24 * 3600), // 0-30 days uptime
                new ResourceUsage(
                    45.5 + (i % 30),      // CPU %
                    62.3 + (i % 20),      // Memory %
                    48.7 + (i % 25),      // Disk %
                    85.2 + (i % 10)       // Network %
                ),
                syncProgress,
                currentTime - (i * 1000),
                new LocationInfo(
                    getCountryForIndex(i),
                    getRegionForIndex(i),
                    getCityForIndex(i),
                    getLatForIndex(i),
                    getLonForIndex(i)
                ),
                "10.0." + (i / 256) + "." + (i % 256),
                9000 + i,
                (1_000_000 + i * 10_000) + " AUR",
                99.95 - (i * 0.001)
            ));
        }

        // Add API nodes
        for (int i = 0; i < 8; i++) {
            String nodeId = "api-node-" + String.format("%02d", i);
            nodes.add(new NodeInfo(
                nodeId,
                "API",
                "ACTIVE",
                "12.0.0",
                (long) (Math.random() * 60 * 24 * 3600),
                new ResourceUsage(35.2 + i * 3, 55.8 + i * 2, 42.1 + i * 2, 78.5 + i),
                100.0,
                currentTime - (i * 500),
                new LocationInfo("USA", "North America", "New York", 40.7128, -74.0060),
                "10.1.0." + i,
                8080 + i,
                "N/A",
                99.99
            ));
        }

        // Add business nodes
        for (int i = 0; i < 5; i++) {
            String nodeId = "business-node-" + String.format("%02d", i);
            nodes.add(new NodeInfo(
                nodeId,
                "BUSINESS",
                i < 4 ? "ACTIVE" : "SYNCING",
                "12.0.0",
                (long) (Math.random() * 15 * 24 * 3600),
                new ResourceUsage(28.3 + i * 4, 48.7 + i * 3, 38.9 + i * 2, 65.4 + i * 2),
                i < 4 ? 100.0 : 98.5,
                currentTime - (i * 750),
                new LocationInfo("UK", "Europe", "London", 51.5074, -0.1278),
                "10.2.0." + i,
                7070 + i,
                "N/A",
                99.97
            ));
        }

        // Add channel nodes
        for (int i = 0; i < 5; i++) {
            String nodeId = "channel-node-" + String.format("%02d", i);
            nodes.add(new NodeInfo(
                nodeId,
                "CHANNEL",
                "ACTIVE",
                "12.0.0",
                (long) (Math.random() * 45 * 24 * 3600),
                new ResourceUsage(32.1 + i * 3, 51.2 + i * 2, 44.5 + i * 2, 72.8 + i),
                100.0,
                currentTime - (i * 600),
                new LocationInfo("Singapore", "Asia", "Singapore", 1.3521, 103.8198),
                "10.3.0." + i,
                6060 + i,
                "N/A",
                99.98
            ));
        }

        return nodes;
    }

    private String getCountryForIndex(int i) {
        String[] countries = {"USA", "Canada", "UK", "Germany", "Netherlands", "France",
            "Sweden", "Singapore", "Japan", "Australia", "South Korea", "Brazil", "UAE"};
        return countries[i % countries.length];
    }

    private String getRegionForIndex(int i) {
        String[] regions = {"North America", "Europe", "Asia", "South America", "Middle East"};
        return regions[i % regions.length];
    }

    private String getCityForIndex(int i) {
        String[] cities = {"New York", "London", "Singapore", "Tokyo", "Sydney",
            "Amsterdam", "Stockholm", "Toronto", "Berlin", "Seoul"};
        return cities[i % cities.length];
    }

    private double getLatForIndex(int i) {
        return -90.0 + (i * 1.42); // Distributed across latitudes
    }

    private double getLonForIndex(int i) {
        return -180.0 + (i * 2.83); // Distributed across longitudes
    }

    // ==================== DATA MODELS ====================

    /**
     * Node information record
     */
    public record NodeInfo(
        String nodeId,
        String nodeType,
        String status,
        String version,
        long uptimeSeconds,
        ResourceUsage resourceUsage,
        double syncProgress,
        long lastSeenTimestamp,
        LocationInfo location,
        String ipAddress,
        int port,
        String stakeAmount,
        double reliability
    ) {}

    /**
     * Resource usage metrics
     */
    public record ResourceUsage(
        double cpuPercent,
        double memoryPercent,
        double diskPercent,
        double networkPercent
    ) {}

    /**
     * Node location information
     */
    public record LocationInfo(
        String country,
        String region,
        String city,
        double latitude,
        double longitude
    ) {}

    /**
     * Paginated node list response
     */
    public record NodeListResponse(
        List<NodeInfo> nodes,
        int totalNodes,
        int page,
        int size,
        int totalPages,
        boolean hasNext,
        boolean hasPrevious,
        long timestamp
    ) {}
}
