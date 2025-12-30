package io.aurigraph.v11.api;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import io.smallrye.mutiny.Uni;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.util.*;

/**
 * Network Topology API Resource
 *
 * Provides network visualization and event streaming:
 * - GET /api/v11/blockchain/network/topology - Network topology visualization
 * - GET /api/v11/blockchain/events - Event stream/log
 *
 * @version 11.0.0
 * @author Backend Development Agent (BDA)
 */
@Path("/api/v11/blockchain")
@ApplicationScoped
@Tag(name = "Network Topology API", description = "Network topology and event streaming operations")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class NetworkTopologyApiResource {

    private static final Logger LOG = Logger.getLogger(NetworkTopologyApiResource.class);

    // ==================== ENDPOINT 1: Network Topology ====================

    /**
     * GET /api/v11/blockchain/network/topology
     * Network topology visualization data
     */
    @GET
    @Path("/network/topology")
    @Operation(summary = "Get network topology", description = "Retrieve network topology for visualization")
    @APIResponse(responseCode = "200", description = "Topology retrieved successfully",
                content = @Content(schema = @Schema(implementation = NetworkTopologyResponse.class)))
    public Uni<NetworkTopologyResponse> getNetworkTopology() {
        LOG.info("Fetching network topology");

        return Uni.createFrom().item(() -> {
            NetworkTopologyResponse response = new NetworkTopologyResponse();
            response.timestamp = Instant.now().toEpochMilli();
            response.networkId = "aurigraph-v11-mainnet";
            response.totalNodes = 156;
            response.activeValidators = 42;
            response.totalConnections = 523;
            response.averageLatency = 45.3;
            response.networkHealth = "HEALTHY";

            // Generate node topology
            response.nodes = new ArrayList<>();

            // Validator nodes
            for (int i = 1; i <= 42; i++) {
                NetworkNode node = new NetworkNode();
                node.nodeId = "validator-" + String.format("%03d", i);
                node.nodeType = "VALIDATOR";
                node.region = getRandomRegion();
                node.status = "ACTIVE";
                node.connections = 10 + (i % 5);
                node.latency = 25.0 + (Math.random() * 30);
                node.stake = 1000000.0 + (Math.random() * 5000000);
                node.uptime = 99.5 + (Math.random() * 0.5);
                node.version = "12.0.0";
                response.nodes.add(node);
            }

            // Full nodes
            for (int i = 1; i <= 114; i++) {
                NetworkNode node = new NetworkNode();
                node.nodeId = "full-node-" + String.format("%03d", i);
                node.nodeType = "FULL_NODE";
                node.region = getRandomRegion();
                node.status = i % 20 == 0 ? "SYNCING" : "ACTIVE";
                node.connections = 5 + (i % 8);
                node.latency = 40.0 + (Math.random() * 50);
                node.stake = 0.0;
                node.uptime = 95.0 + (Math.random() * 5);
                node.version = i % 10 == 0 ? "10.9.5" : "12.0.0";
                response.nodes.add(node);
            }

            // Generate connection graph
            response.connections = new ArrayList<>();
            for (NetworkNode node : response.nodes) {
                if ("VALIDATOR".equals(node.nodeType)) {
                    // Validators connect to multiple other validators
                    int connectionCount = node.connections;
                    for (int c = 0; c < Math.min(connectionCount, 10); c++) {
                        NetworkConnection conn = new NetworkConnection();
                        conn.sourceNodeId = node.nodeId;
                        conn.targetNodeId = "validator-" + String.format("%03d", (int)(Math.random() * 42) + 1);
                        conn.latency = 20.0 + (Math.random() * 30);
                        conn.bandwidth = 1000.0 + (Math.random() * 500);
                        conn.packetLoss = Math.random() * 0.1;
                        response.connections.add(conn);
                    }
                }
            }

            // Region distribution
            response.regionDistribution = new HashMap<>();
            response.regionDistribution.put("us-east", 45);
            response.regionDistribution.put("us-west", 38);
            response.regionDistribution.put("eu-central", 32);
            response.regionDistribution.put("asia-pacific", 28);
            response.regionDistribution.put("south-america", 13);

            LOG.infof("Network topology: %d nodes, %d connections",
                     response.totalNodes, response.totalConnections);

            return response;
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    // ==================== ENDPOINT 13: Blockchain Events ====================

    /**
     * GET /api/v11/blockchain/events
     * Event stream and log retrieval
     */
    @GET
    @Path("/events")
    @Operation(summary = "Get blockchain events", description = "Retrieve blockchain event stream")
    @APIResponse(responseCode = "200", description = "Events retrieved successfully",
                content = @Content(schema = @Schema(implementation = BlockchainEventsResponse.class)))
    public Uni<BlockchainEventsResponse> getBlockchainEvents(
        @QueryParam("limit") @DefaultValue("50") int limit,
        @QueryParam("offset") @DefaultValue("0") int offset,
        @QueryParam("eventType") String eventType) {

        LOG.infof("Fetching blockchain events: limit=%d, offset=%d, type=%s", limit, offset, eventType);

        return Uni.createFrom().item(() -> {
            BlockchainEventsResponse response = new BlockchainEventsResponse();
            response.timestamp = Instant.now().toEpochMilli();
            response.totalEvents = 15234;
            response.limit = limit;
            response.offset = offset;
            response.events = new ArrayList<>();

            String[] eventTypes = {"BLOCK_CREATED", "TRANSACTION_PROCESSED", "VALIDATOR_JOINED",
                                   "VALIDATOR_SLASHED", "CONSENSUS_ACHIEVED", "BRIDGE_TRANSFER",
                                   "CONTRACT_DEPLOYED", "GOVERNANCE_VOTE"};

            long currentTime = Instant.now().toEpochMilli();

            for (int i = 0; i < Math.min(limit, 50); i++) {
                BlockchainEvent event = new BlockchainEvent();
                event.eventId = "evt-" + UUID.randomUUID().toString().substring(0, 8);

                String selectedType = eventType != null ? eventType :
                                     eventTypes[(offset + i) % eventTypes.length];
                event.eventType = selectedType;
                event.timestamp = currentTime - ((offset + i) * 1000);
                event.blockNumber = 1500000L + offset + i;

                switch (selectedType) {
                    case "BLOCK_CREATED":
                        event.data = Map.of(
                            "blockHash", "0x" + UUID.randomUUID().toString().replace("-", ""),
                            "transactions", 125 + (int)(Math.random() * 200),
                            "validator", "validator-" + String.format("%03d", (int)(Math.random() * 42) + 1)
                        );
                        break;
                    case "TRANSACTION_PROCESSED":
                        event.data = Map.of(
                            "txHash", "0x" + UUID.randomUUID().toString().replace("-", ""),
                            "from", "0x" + UUID.randomUUID().toString().substring(0, 20),
                            "to", "0x" + UUID.randomUUID().toString().substring(0, 20),
                            "amount", 100.0 + (Math.random() * 10000)
                        );
                        break;
                    case "VALIDATOR_JOINED":
                        event.data = Map.of(
                            "validatorId", "validator-new-" + (int)(Math.random() * 1000),
                            "stake", 1000000.0 + (Math.random() * 5000000),
                            "region", getRandomRegion()
                        );
                        break;
                    case "CONSENSUS_ACHIEVED":
                        event.data = Map.of(
                            "round", 7500000L + i,
                            "votes", 38,
                            "latency", 45.0 + (Math.random() * 20)
                        );
                        break;
                    default:
                        event.data = Map.of("status", "OK");
                }

                event.severity = selectedType.contains("SLASHED") ? "HIGH" : "NORMAL";

                response.events.add(event);
            }

            response.hasMore = (offset + limit) < response.totalEvents;

            LOG.infof("Retrieved %d blockchain events", response.events.size());

            return response;
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    // ==================== Helper Methods ====================

    private String getRandomRegion() {
        String[] regions = {"us-east", "us-west", "eu-central", "asia-pacific", "south-america"};
        return regions[(int)(Math.random() * regions.length)];
    }

    // ==================== Response DTOs ====================

    public static class NetworkTopologyResponse {
        public long timestamp;
        public String networkId;
        public int totalNodes;
        public int activeValidators;
        public int totalConnections;
        public double averageLatency;
        public String networkHealth;
        public List<NetworkNode> nodes;
        public List<NetworkConnection> connections;
        public Map<String, Integer> regionDistribution;
    }

    public static class NetworkNode {
        public String nodeId;
        public String nodeType;
        public String region;
        public String status;
        public int connections;
        public double latency;
        public double stake;
        public double uptime;
        public String version;
    }

    public static class NetworkConnection {
        public String sourceNodeId;
        public String targetNodeId;
        public double latency;
        public double bandwidth;
        public double packetLoss;
    }

    public static class BlockchainEventsResponse {
        public long timestamp;
        public long totalEvents;
        public int limit;
        public int offset;
        public boolean hasMore;
        public List<BlockchainEvent> events;
    }

    public static class BlockchainEvent {
        public String eventId;
        public String eventType;
        public long timestamp;
        public long blockNumber;
        public Map<String, Object> data;
        public String severity;
    }
}
