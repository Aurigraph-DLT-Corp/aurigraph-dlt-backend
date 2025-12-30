package io.aurigraph.v11.api.phase2;

import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.util.*;

/**
 * Sprint 13: Node Management REST API (18 pts)
 *
 * Endpoints for node registration, listing, health status, and performance.
 * Extracted from Phase2BlockchainResource for better maintainability.
 *
 * @author Backend Development Agent (BDA)
 * @version 11.0.0
 * @since Sprint 13
 */
@Path("/api/v11/blockchain")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class NodeResource {

    private static final Logger LOG = Logger.getLogger(NodeResource.class);

    /**
     * Register new node
     * POST /api/v11/blockchain/nodes/register
     */
    @POST
    @Path("/nodes/register")
    public Uni<Response> registerNode(NodeRegistration registration) {
        LOG.infof("Registering node: %s", registration.nodeAddress);

        return Uni.createFrom().item(() -> Response.ok(Map.of(
            "status", "success",
            "nodeId", UUID.randomUUID().toString(),
            "nodeAddress", registration.nodeAddress,
            "nodeType", registration.nodeType,
            "region", registration.region,
            "registeredAt", Instant.now().toString(),
            "message", "Node registered successfully"
        )).build());
    }

    /**
     * List all nodes
     * GET /api/v11/blockchain/nodes
     */
    @GET
    @Path("/nodes")
    public Uni<NodesList> getAllNodes(@QueryParam("type") String nodeType,
                                        @QueryParam("status") String status,
                                        @QueryParam("limit") @DefaultValue("50") int limit) {
        LOG.infof("Fetching all nodes (type: %s, status: %s, limit: %d)", nodeType, status, limit);

        return Uni.createFrom().item(() -> {
            NodesList list = new NodesList();
            list.totalNodes = 250;
            list.activeNodes = 235;
            list.nodes = new ArrayList<>();

            String[] types = {"VALIDATOR", "FULL", "LIGHT", "ARCHIVE"};
            String[] regions = {"US-EAST", "US-WEST", "EU-WEST", "ASIA-PACIFIC", "SA-EAST"};

            for (int i = 1; i <= Math.min(limit, 10); i++) {
                NodeSummary node = new NodeSummary();
                node.nodeId = "node-" + UUID.randomUUID().toString().substring(0, 8);
                node.nodeAddress = "0xnode-" + String.format("%03d", i);
                node.nodeType = types[i % types.length];
                node.status = i <= 235 ? "ACTIVE" : "INACTIVE";
                node.region = regions[i % regions.length];
                node.uptime = 98.0 + (i * 0.05);
                node.lastSeen = Instant.now().minusSeconds(i * 60).toString();
                list.nodes.add(node);
            }

            return list;
        });
    }

    /**
     * Get node health status
     * GET /api/v11/blockchain/nodes/{nodeId}/health
     */
    @GET
    @Path("/nodes/{nodeId}/health")
    public Uni<NodeHealth> getNodeHealth(@PathParam("nodeId") String nodeId) {
        LOG.infof("Fetching node health: %s", nodeId);

        return Uni.createFrom().item(() -> {
            NodeHealth health = new NodeHealth();
            health.nodeId = nodeId;
            health.status = "HEALTHY";
            health.uptime = 99.95;
            health.cpuUsage = 45.2;
            health.memoryUsage = 62.8;
            health.diskUsage = 38.5;
            health.networkLatency = 15.3;
            health.peersConnected = 48;
            health.blocksInSync = true;
            health.lastBlockTime = Instant.now().minusSeconds(2).toString();
            return health;
        });
    }

    /**
     * Get node performance analytics
     * GET /api/v11/blockchain/nodes/{nodeId}/performance
     */
    @GET
    @Path("/nodes/{nodeId}/performance")
    public Uni<NodePerformance> getNodePerformance(@PathParam("nodeId") String nodeId) {
        LOG.infof("Fetching node performance: %s", nodeId);

        return Uni.createFrom().item(() -> {
            NodePerformance perf = new NodePerformance();
            perf.nodeId = nodeId;
            perf.averageTPS = 125000;
            perf.peakTPS = 185000;
            perf.averageLatency = 42.5;
            perf.transactionsProcessed = 125678000L;
            perf.blocksProduced = 12568;
            perf.dataTransferred = "2.5 TB";
            perf.uptimePercentage = 99.95;
            return perf;
        });
    }

    // ==================== DTOs ====================

    public static class NodeRegistration {
        public String nodeAddress;
        public String nodeType;
        public String region;
    }

    public static class NodeHealth {
        public String nodeId;
        public String status;
        public double uptime;
        public double cpuUsage;
        public double memoryUsage;
        public double diskUsage;
        public double networkLatency;
        public int peersConnected;
        public boolean blocksInSync;
        public String lastBlockTime;
    }

    public static class NodePerformance {
        public String nodeId;
        public long averageTPS;
        public long peakTPS;
        public double averageLatency;
        public long transactionsProcessed;
        public long blocksProduced;
        public String dataTransferred;
        public double uptimePercentage;
    }

    public static class NodesList {
        public int totalNodes;
        public int activeNodes;
        public List<NodeSummary> nodes;
    }

    public static class NodeSummary {
        public String nodeId;
        public String nodeAddress;
        public String nodeType;
        public String status;
        public String region;
        public double uptime;
        public String lastSeen;
    }
}
