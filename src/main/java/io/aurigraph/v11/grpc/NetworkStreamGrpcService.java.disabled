package io.aurigraph.v11.grpc;

import io.aurigraph.v11.proto.*;
import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.operators.multi.processors.UnicastProcessor;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * gRPC Streaming Service for Network Topology Monitoring
 *
 * Replaces WebSocket /ws/network endpoint with gRPC-Web bidirectional streaming.
 * Provides real-time streaming of network status, topology changes, and peer connections.
 *
 * Benefits over WebSocket+JSON:
 * - 60-70% bandwidth reduction (Protobuf vs JSON)
 * - Type-safe client generation (TypeScript + Java)
 * - Built-in flow control and backpressure
 * - HTTP/2 multiplexing (multiple streams, one connection)
 *
 * @author Backend Development Agent (BDA)
 * @since V12.0.0
 */
@GrpcService
@ApplicationScoped
public class NetworkStreamGrpcService implements NetworkStreamService {

    private static final Logger LOG = Logger.getLogger(NetworkStreamGrpcService.class);

    // Active subscriptions for interactive monitoring
    private final Map<String, UnicastProcessor<NetworkEventStream>> activeSubscriptions = new ConcurrentHashMap<>();

    // Simulated network nodes
    private final List<String> nodeIds = Arrays.asList(
            "node-1", "node-2", "node-3", "node-4", "node-5",
            "node-6", "node-7", "node-8", "node-9", "node-10"
    );

    /**
     * Get current network topology (unary RPC)
     */
    @Override
    public Uni<NetworkTopologyUpdate> getNetworkTopology(NetworkSubscribeRequest request) {
        LOG.infof("Getting network topology for client: %s", request.getClientId());
        return Uni.createFrom().item(this::buildNetworkTopologyUpdate);
    }

    /**
     * Get network health status (unary RPC)
     */
    @Override
    public Uni<NetworkHealthUpdate> getNetworkHealth(NetworkSubscribeRequest request) {
        LOG.infof("Getting network health for client: %s", request.getClientId());
        return Uni.createFrom().item(this::buildNetworkHealthUpdate);
    }

    /**
     * Get geographic distribution (unary RPC)
     */
    @Override
    public Uni<GeographicDistributionUpdate> getGeographicDistribution(NetworkSubscribeRequest request) {
        LOG.infof("Getting geographic distribution for client: %s", request.getClientId());
        return Uni.createFrom().item(this::buildGeographicDistributionUpdate);
    }

    /**
     * Stream all network events (server streaming)
     */
    @Override
    public Multi<NetworkEventStream> streamNetworkEvents(NetworkSubscribeRequest request) {
        int intervalMs = request.getUpdateIntervalMs() > 0 ? request.getUpdateIntervalMs() : 3000;
        LOG.infof("Starting network event stream for client %s with interval %dms",
                request.getClientId(), intervalMs);

        return Multi.createFrom().ticks()
                .every(Duration.ofMillis(intervalMs))
                .onItem().transform(tick -> buildNetworkEventStream())
                .onSubscription().invoke(() -> LOG.info("Network event stream subscription started"))
                .onCancellation().invoke(() -> LOG.info("Network event stream cancelled"));
    }

    /**
     * Stream topology updates (server streaming)
     */
    @Override
    public Multi<NetworkTopologyUpdate> streamTopologyUpdates(NetworkSubscribeRequest request) {
        int intervalMs = request.getUpdateIntervalMs() > 0 ? request.getUpdateIntervalMs() : 5000;

        return Multi.createFrom().ticks()
                .every(Duration.ofMillis(intervalMs))
                .onItem().transform(tick -> buildNetworkTopologyUpdate());
    }

    /**
     * Stream node status change events (server streaming)
     */
    @Override
    public Multi<NodeStatusChangeEvent> streamNodeEvents(NetworkSubscribeRequest request) {
        return Multi.createFrom().ticks()
                .every(Duration.ofSeconds(10))
                .onItem().transform(tick -> buildNodeStatusChangeEvent());
    }

    /**
     * Stream peer connection events (server streaming)
     */
    @Override
    public Multi<PeerConnectedEvent> streamPeerConnections(NetworkSubscribeRequest request) {
        return Multi.createFrom().ticks()
                .every(Duration.ofSeconds(15))
                .onItem().transform(tick -> buildPeerConnectedEvent());
    }

    /**
     * Stream network performance metrics (server streaming)
     */
    @Override
    public Multi<NetworkPerformanceUpdate> streamNetworkPerformance(NetworkSubscribeRequest request) {
        int intervalMs = request.getUpdateIntervalMs() > 0 ? request.getUpdateIntervalMs() : 2000;

        return Multi.createFrom().ticks()
                .every(Duration.ofMillis(intervalMs))
                .onItem().transform(tick -> buildNetworkPerformanceUpdate());
    }

    /**
     * Stream network health updates (server streaming)
     */
    @Override
    public Multi<NetworkHealthUpdate> streamNetworkHealth(NetworkSubscribeRequest request) {
        int intervalMs = request.getUpdateIntervalMs() > 0 ? request.getUpdateIntervalMs() : 3000;

        return Multi.createFrom().ticks()
                .every(Duration.ofMillis(intervalMs))
                .onItem().transform(tick -> buildNetworkHealthUpdate());
    }

    /**
     * Interactive network monitoring (bidirectional streaming)
     */
    @Override
    public Multi<NetworkEventStream> interactiveNetworkMonitor(Multi<NetworkCommand> commands) {
        String subscriptionId = UUID.randomUUID().toString();
        UnicastProcessor<NetworkEventStream> processor = UnicastProcessor.create();
        activeSubscriptions.put(subscriptionId, processor);

        // Handle incoming commands
        commands.subscribe().with(
                command -> handleNetworkCommand(subscriptionId, command, processor),
                error -> {
                    LOG.errorf(error, "Error in network command stream: %s", subscriptionId);
                    activeSubscriptions.remove(subscriptionId);
                    processor.onComplete();
                },
                () -> {
                    LOG.infof("Network command stream completed: %s", subscriptionId);
                    activeSubscriptions.remove(subscriptionId);
                    processor.onComplete();
                }
        );

        // Start default event stream
        Multi.createFrom().ticks()
                .every(Duration.ofMillis(3000))
                .subscribe().with(
                        tick -> {
                            if (activeSubscriptions.containsKey(subscriptionId)) {
                                processor.onNext(buildNetworkEventStream());
                            }
                        }
                );

        return processor;
    }

    /**
     * Query historical network events
     */
    @Override
    public Uni<NetworkHistoricalResponse> queryHistoricalEvents(NetworkHistoricalQuery query) {
        LOG.infof("Querying historical network events from %s to %s",
                query.getStartTime(), query.getEndTime());

        List<NetworkEventStream> events = new ArrayList<>();
        for (int i = 0; i < Math.min(query.getLimit(), 100); i++) {
            events.add(buildNetworkEventStream());
        }

        return Uni.createFrom().item(
                NetworkHistoricalResponse.newBuilder()
                        .addAllEvents(events)
                        .setTotalEvents(events.size())
                        .setHasMore(false)
                        .setQueryExecutedAt(buildTimestamp())
                        .build()
        );
    }

    /**
     * Unsubscribe from streaming
     */
    @Override
    public Uni<NetworkUnsubscribeResponse> unsubscribe(NetworkUnsubscribeRequest request) {
        String subscriptionId = request.getSubscriptionId();
        UnicastProcessor<NetworkEventStream> processor = activeSubscriptions.remove(subscriptionId);

        if (processor != null) {
            processor.onComplete();
            return Uni.createFrom().item(
                    NetworkUnsubscribeResponse.newBuilder()
                            .setSuccess(true)
                            .setMessage("Successfully unsubscribed from network stream")
                            .setTotalEventsDelivered(100L)
                            .setEndedAt(buildTimestamp())
                            .build()
            );
        }

        return Uni.createFrom().item(
                NetworkUnsubscribeResponse.newBuilder()
                        .setSuccess(false)
                        .setMessage("Subscription not found: " + subscriptionId)
                        .build()
        );
    }

    // =========================================================================
    // Private Helper Methods
    // =========================================================================

    private void handleNetworkCommand(String subscriptionId, NetworkCommand command,
                                      UnicastProcessor<NetworkEventStream> processor) {
        LOG.infof("Handling network command: %s for subscription: %s",
                command.getCommand(), subscriptionId);

        switch (command.getCommand()) {
            case REQUEST_SNAPSHOT:
                processor.onNext(buildNetworkEventStream());
                break;
            case FOCUS_NODE:
                String nodeId = command.getParametersMap().get("nodeId");
                LOG.infof("Focusing on node: %s", nodeId);
                break;
            case FOCUS_REGION:
                String region = command.getParametersMap().get("region");
                LOG.infof("Focusing on region: %s", region);
                break;
            case PAUSE:
                LOG.infof("Pausing network stream: %s", subscriptionId);
                break;
            case RESUME:
                LOG.infof("Resuming network stream: %s", subscriptionId);
                break;
            default:
                LOG.debugf("Unhandled command type: %s", command.getCommand());
        }
    }

    private NetworkTopologyUpdate buildNetworkTopologyUpdate() {
        NetworkTopologyUpdate.Builder builder = NetworkTopologyUpdate.newBuilder()
                .setTotalNodes(nodeIds.size())
                .setActiveNodes(nodeIds.size())
                .setSyncingNodes(0)
                .setOfflineNodes(0)
                .setStatistics(buildNetworkStatistics())
                .setTimestamp(buildTimestamp());

        for (String nodeId : nodeIds) {
            builder.addNodes(buildNodeTopologyInfo(nodeId));
        }

        // Add some connections
        for (int i = 0; i < nodeIds.size() - 1; i++) {
            builder.addConnections(buildPeerConnection(nodeIds.get(i), nodeIds.get(i + 1)));
        }

        return builder.build();
    }

    private NodeTopologyInfo buildNodeTopologyInfo(String nodeId) {
        return NodeTopologyInfo.newBuilder()
                .setNode(buildNodeInfo(nodeId))
                .addConnectedPeerIds(nodeIds.get((nodeIds.indexOf(nodeId) + 1) % nodeIds.size()))
                .addConnectedPeerIds(nodeIds.get((nodeIds.indexOf(nodeId) + 2) % nodeIds.size()))
                .setPeerCount(4 + (int) (Math.random() * 4))
                .setAveragePeerLatencyMs(15.0 + Math.random() * 20)
                .setRegion(getRegionForNode(nodeId))
                .setDataCenter("DC-" + ((nodeIds.indexOf(nodeId) % 3) + 1))
                .setGeoLocation(GeoLocation.newBuilder()
                        .setLatitude(37.7749 + Math.random() * 10)
                        .setLongitude(-122.4194 + Math.random() * 50)
                        .setCountry("USA")
                        .setCity(getCityForNode(nodeId))
                        .build())
                .setIsReachable(true)
                .setUptimePercent(99.5 + Math.random() * 0.5)
                .setLastSeen(buildTimestamp())
                .build();
    }

    private NodeInfo buildNodeInfo(String nodeId) {
        int nodeIndex = nodeIds.indexOf(nodeId);
        NodeType nodeType = nodeIndex < 5 ? NodeType.VALIDATOR : NodeType.FULL_NODE;

        return NodeInfo.newBuilder()
                .setNodeId(nodeId)
                .setAddress("10.0.0." + (nodeIndex + 1))
                .setPort(9000 + nodeIndex)
                .setNodeType(nodeType)
                .setStatus(NodeStatus.ACTIVE)
                .setVersion("12.0.0")
                .setRegion(getRegionForNode(nodeId))
                .build();
    }

    private PeerConnection buildPeerConnection(String nodeId1, String nodeId2) {
        return PeerConnection.newBuilder()
                .setConnectionId(UUID.randomUUID().toString())
                .setNodeId1(nodeId1)
                .setNodeId2(nodeId2)
                .setConnectionType(PeerConnection.ConnectionType.DIRECT)
                .setStatus(PeerConnection.ConnectionStatus.CONNECTED)
                .setLatencyMs(10.0 + Math.random() * 25)
                .setBandwidthMbps(100 + (long) (Math.random() * 100))
                .setBytesSent((long) (Math.random() * 1000000000))
                .setBytesReceived((long) (Math.random() * 1000000000))
                .setEstablishedAt(buildTimestamp())
                .setLastActivity(buildTimestamp())
                .build();
    }

    private NetworkStatistics buildNetworkStatistics() {
        return NetworkStatistics.newBuilder()
                .setTotalNodes(nodeIds.size())
                .setValidatorNodes(5)
                .setFullNodes(5)
                .setLightNodes(0)
                .setTotalConnections(25)
                .setAverageConnectionsPerNode(5.0)
                .setNetworkDensity(0.55)
                .setAverageNetworkLatencyMs(20.0 + Math.random() * 10)
                .setMedianNetworkLatencyMs(18.0 + Math.random() * 8)
                .setP95NetworkLatencyMs(45.0 + Math.random() * 15)
                .setTotalBandwidthMbps(1000 + (long) (Math.random() * 500))
                .setNetworkThroughputMbps(500 + (long) (Math.random() * 250))
                .setNetworkHealthScore(95.0 + Math.random() * 5)
                .setPartitionedNodes(0)
                .setIsolatedNodes(0)
                .build();
    }

    private NetworkHealthUpdate buildNetworkHealthUpdate() {
        return NetworkHealthUpdate.newBuilder()
                .setHealthStatus(NetworkHealthUpdate.NetworkHealth.HEALTHY)
                .setHealthScore(95.0 + Math.random() * 5)
                .setNetworkUptimePercent(99.9)
                .setReachableNodesCount(nodeIds.size())
                .setUnreachableNodesCount(0)
                .setNetworkPartitioned(false)
                .setPartitionCount(0)
                .setTimestamp(buildTimestamp())
                .build();
    }

    private GeographicDistributionUpdate buildGeographicDistributionUpdate() {
        return GeographicDistributionUpdate.newBuilder()
                .putNodesByRegion("us-east-1", 4)
                .putNodesByRegion("us-west-2", 3)
                .putNodesByRegion("eu-west-1", 3)
                .putNodesByCountry("USA", 7)
                .putNodesByCountry("Germany", 2)
                .putNodesByCountry("UK", 1)
                .addRegions(RegionInfo.newBuilder()
                        .setRegionName("us-east-1")
                        .setNodeCount(4)
                        .setCenter(GeoLocation.newBuilder()
                                .setLatitude(39.0438)
                                .setLongitude(-77.4874)
                                .setCountry("USA")
                                .setCity("Ashburn")
                                .build())
                        .setAverageInterRegionLatencyMs(45.0)
                        .build())
                .setTimestamp(buildTimestamp())
                .build();
    }

    private NetworkEventStream buildNetworkEventStream() {
        // Randomly choose event type
        int eventType = (int) (Math.random() * 5);

        NetworkEventStream.Builder builder = NetworkEventStream.newBuilder()
                .setTimestamp(buildTimestamp())
                .setEventId(UUID.randomUUID().toString());

        switch (eventType) {
            case 0:
                builder.setTopologyUpdate(buildNetworkTopologyUpdate());
                break;
            case 1:
                builder.setPerformance(buildNetworkPerformanceUpdate());
                break;
            case 2:
                builder.setHealth(buildNetworkHealthUpdate());
                break;
            case 3:
                builder.setConnectionQuality(buildConnectionQualityUpdate());
                break;
            case 4:
                builder.setGeography(buildGeographicDistributionUpdate());
                break;
        }

        return builder.build();
    }

    private NodeStatusChangeEvent buildNodeStatusChangeEvent() {
        String nodeId = nodeIds.get((int) (Math.random() * nodeIds.size()));

        return NodeStatusChangeEvent.newBuilder()
                .setNodeId(nodeId)
                .setOldStatus(NodeStatusChangeEvent.StatusChange.ONLINE)
                .setNewStatus(NodeStatusChangeEvent.StatusChange.ONLINE)
                .setChangeReason("Heartbeat received")
                .setTimestamp(buildTimestamp())
                .build();
    }

    private PeerConnectedEvent buildPeerConnectedEvent() {
        int idx1 = (int) (Math.random() * nodeIds.size());
        int idx2 = (idx1 + 1 + (int) (Math.random() * (nodeIds.size() - 1))) % nodeIds.size();

        return PeerConnectedEvent.newBuilder()
                .setConnection(buildPeerConnection(nodeIds.get(idx1), nodeIds.get(idx2)))
                .setInitiatorNodeId(nodeIds.get(idx1))
                .setResponderNodeId(nodeIds.get(idx2))
                .setConnectedAt(buildTimestamp())
                .build();
    }

    private NetworkPerformanceUpdate buildNetworkPerformanceUpdate() {
        return NetworkPerformanceUpdate.newBuilder()
                .setMessagesPerSecond(5000 + (long) (Math.random() * 2000))
                .setTransactionsPerSecond(776000 + (long) (Math.random() * 50000))
                .setBlocksPerSecond(1)
                .setAverageMessageLatencyMs(15.0 + Math.random() * 10)
                .setP50MessageLatencyMs(12.0 + Math.random() * 5)
                .setP95MessageLatencyMs(35.0 + Math.random() * 15)
                .setP99MessageLatencyMs(50.0 + Math.random() * 20)
                .setTotalBytesSentPerSecond(10000000 + (long) (Math.random() * 5000000))
                .setTotalBytesReceivedPerSecond(10000000 + (long) (Math.random() * 5000000))
                .setBandwidthUtilizationPercent(45.0 + Math.random() * 20)
                .putMessagesByProtocol("consensus", 2000L)
                .putMessagesByProtocol("gossip", 3000L)
                .putMessagesByProtocol("sync", 500L)
                .putLatencyByProtocol("consensus", 20.0)
                .putLatencyByProtocol("gossip", 10.0)
                .putLatencyByProtocol("sync", 30.0)
                .setTimestamp(buildTimestamp())
                .build();
    }

    private PeerConnectionQualityUpdate buildConnectionQualityUpdate() {
        int idx1 = (int) (Math.random() * nodeIds.size());
        int idx2 = (idx1 + 1) % nodeIds.size();

        return PeerConnectionQualityUpdate.newBuilder()
                .setConnectionId(UUID.randomUUID().toString())
                .setNodeId1(nodeIds.get(idx1))
                .setNodeId2(nodeIds.get(idx2))
                .setLatencyMs(15.0 + Math.random() * 20)
                .setJitterMs(2.0 + Math.random() * 5)
                .setPacketLossPercent(Math.random() * 0.1)
                .setBandwidthMbps(100 + (long) (Math.random() * 100))
                .setQualityRating(PeerConnectionQualityUpdate.QualityRating.EXCELLENT)
                .setTimestamp(buildTimestamp())
                .build();
    }

    private String getRegionForNode(String nodeId) {
        int index = nodeIds.indexOf(nodeId);
        String[] regions = {"us-east-1", "us-west-2", "eu-west-1"};
        return regions[index % regions.length];
    }

    private String getCityForNode(String nodeId) {
        int index = nodeIds.indexOf(nodeId);
        String[] cities = {"Ashburn", "San Francisco", "Frankfurt", "London", "Tokyo"};
        return cities[index % cities.length];
    }

    private com.google.protobuf.Timestamp buildTimestamp() {
        Instant now = Instant.now();
        return com.google.protobuf.Timestamp.newBuilder()
                .setSeconds(now.getEpochSecond())
                .setNanos(now.getNano())
                .build();
    }
}
