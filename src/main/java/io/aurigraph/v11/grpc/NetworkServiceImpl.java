package io.aurigraph.v11.grpc;

import io.quarkus.grpc.GrpcService;
import io.quarkus.logging.Log;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.aurigraph.v11.proto.*;
import com.google.protobuf.Timestamp;
import jakarta.inject.Singleton;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * NetworkServiceImpl - High-Performance Peer Management and Network Communication
 *
 * Implements 4 RPC methods for network operations with:
 * - GetNetworkStatus() - Network health and peer count monitoring
 * - GetPeerList() - Comprehensive peer information retrieval
 * - BroadcastMessage() - Network-wide message distribution
 * - SubscribeNetworkEvents() - Real-time event streaming
 *
 * Performance Targets:
 * - GetNetworkStatus: <5ms latency
 * - GetPeerList: <10ms latency
 * - BroadcastMessage: <50ms peer-to-peer propagation
 * - SubscribeNetworkEvents: 100+ concurrent subscribers
 *
 * Protocol: gRPC with Protocol Buffers and HTTP/2 multiplexing
 */
@GrpcService
@Singleton
public class NetworkServiceImpl implements io.aurigraph.v11.proto.NetworkService {

    // Peer management storage
    private final Map<String, io.aurigraph.v11.proto.PeerInfo> connectedPeers = new ConcurrentHashMap<>();
    private final Set<String> bannedPeers = ConcurrentHashMap.newKeySet();

    // Network metrics
    private final AtomicLong totalMessagesSent = new AtomicLong(0);
    private final AtomicLong totalMessagesReceived = new AtomicLong(0);
    private volatile double averageLatencyMs = 12.5;
    private volatile double networkBandwidthMbps = 125.0;
    private volatile int syncProgressPercent = 100;

    // Event streaming management
    private final Map<String, Multi<io.aurigraph.v11.proto.NetworkEvent>> activeStreams = new ConcurrentHashMap<>();
    private final AtomicLong eventSequenceCounter = new AtomicLong(0);
    private final Queue<io.aurigraph.v11.proto.NetworkEvent> recentEvents = new ConcurrentLinkedQueue<>();

    // Message broadcast tracking
    private final Map<String, io.aurigraph.v11.proto.BroadcastMessageResponse> broadcastHistory = new ConcurrentHashMap<>();

    public NetworkServiceImpl() {
        Log.infof("✅ NetworkServiceImpl initialized for HTTP/2 gRPC communication");
        initializeMockPeers();
    }

    /**
     * getNetworkStatus - Returns current network health and metrics
     * Performance Target: <5ms latency
     */
    @Override
    public Uni<io.aurigraph.v11.proto.NetworkStatus> getNetworkStatus(io.aurigraph.v11.proto.GetNetworkStatusRequest request) {
        long startTime = System.nanoTime();

        return Uni.createFrom().item(() -> {
            try {
                // Count active peers
                int activePeers = (int) connectedPeers.values().stream()
                    .filter(peer -> peer.getConnectionStatus() == io.aurigraph.v11.proto.PeerConnectionStatus.PEER_CONNECTED)
                    .count();

                io.aurigraph.v11.proto.NetworkStatus.Builder statusBuilder = io.aurigraph.v11.proto.NetworkStatus.newBuilder()
                    .setNetworkHealth(io.aurigraph.v11.proto.HealthStatus.HEALTH_SERVING)
                    .setPeerCount(connectedPeers.size())
                    .setActiveConnections(activePeers)
                    .setSyncProgressPercent(syncProgressPercent)
                    .setTotalMessagesSent(totalMessagesSent.get())
                    .setTotalMessagesReceived(totalMessagesReceived.get())
                    .setAverageLatencyMs(averageLatencyMs)
                    .setNetworkBandwidthMbps(networkBandwidthMbps)
                    .setTimestamp(getCurrentTimestamp());

                // Include peer details if requested
                if (request.getIncludePeerDetails()) {
                    List<io.aurigraph.v11.proto.PeerInfo> topPeers = connectedPeers.values().stream()
                        .filter(peer -> peer.getConnectionStatus() == io.aurigraph.v11.proto.PeerConnectionStatus.PEER_CONNECTED)
                        .sorted(Comparator.comparingDouble(io.aurigraph.v11.proto.PeerInfo::getLatencyMs))
                        .limit(10)
                        .collect(Collectors.toList());

                    statusBuilder.addAllTopPeers(topPeers);
                }

                // Include topology if requested
                if (request.getIncludeTopology()) {
                    io.aurigraph.v11.proto.NetworkTopology topology = io.aurigraph.v11.proto.NetworkTopology.newBuilder()
                        .setTotalNodes(connectedPeers.size())
                        .setTotalEdges(activePeers * 2) // Bidirectional connections
                        .setNetworkDiameter(5) // Mock value
                        .setClusteringCoefficient(0.75) // Mock value
                        .addAllHubNodes(getHubNodes())
                        .build();

                    statusBuilder.setTopology(topology);
                }

                long elapsedMs = (System.nanoTime() - startTime) / 1_000_000;
                Log.debugf("GetNetworkStatus completed in %dms (target: <5ms)", elapsedMs);

                return statusBuilder.build();

            } catch (Exception e) {
                Log.errorf("GetNetworkStatus failed: %s", e.getMessage());
                return io.aurigraph.v11.proto.NetworkStatus.newBuilder()
                    .setNetworkHealth(io.aurigraph.v11.proto.HealthStatus.HEALTH_NOT_SERVING)
                    .setTimestamp(getCurrentTimestamp())
                    .build();
            }
        });
    }

    /**
     * getPeerList - Returns list of connected peers with metadata
     * Performance Target: <10ms latency
     */
    @Override
    public Uni<io.aurigraph.v11.proto.PeerListResponse> getPeerList(io.aurigraph.v11.proto.GetPeerListRequest request) {
        long startTime = System.nanoTime();

        return Uni.createFrom().item(() -> {
            try {
                // Filter peers based on request
                List<io.aurigraph.v11.proto.PeerInfo> filteredPeers = connectedPeers.values().stream()
                    .filter(peer -> filterPeer(peer, request))
                    .collect(Collectors.toList());

                // Sort peers
                sortPeers(filteredPeers, request.getSortBy(), request.getSortDescending());

                // Apply pagination
                int limit = request.getLimit() > 0 ? request.getLimit() : 100;
                int offset = request.getOffset();

                List<io.aurigraph.v11.proto.PeerInfo> paginatedPeers = filteredPeers.stream()
                    .skip(offset)
                    .limit(limit)
                    .collect(Collectors.toList());

                long elapsedMs = (System.nanoTime() - startTime) / 1_000_000;
                Log.debugf("GetPeerList completed in %dms (target: <10ms) - returned %d/%d peers",
                    elapsedMs, paginatedPeers.size(), filteredPeers.size());

                return io.aurigraph.v11.proto.PeerListResponse.newBuilder()
                    .addAllPeers(paginatedPeers)
                    .setTotalCount(filteredPeers.size())
                    .setReturnedCount(paginatedPeers.size())
                    .setTimestamp(getCurrentTimestamp())
                    .build();

            } catch (Exception e) {
                Log.errorf("GetPeerList failed: %s", e.getMessage());
                return io.aurigraph.v11.proto.PeerListResponse.newBuilder()
                    .setTimestamp(getCurrentTimestamp())
                    .build();
            }
        });
    }

    /**
     * broadcastMessage - Broadcasts message to network peers
     * Performance Target: <50ms peer-to-peer propagation
     */
    @Override
    public Uni<io.aurigraph.v11.proto.BroadcastMessageResponse> broadcastMessage(io.aurigraph.v11.proto.BroadcastMessageRequest request) {
        long startTime = System.nanoTime();

        return Uni.createFrom().item(() -> {
            try {
                String messageId = request.getMessageId();
                if (messageId.isEmpty()) {
                    messageId = UUID.randomUUID().toString();
                }

                // Determine target peers
                List<io.aurigraph.v11.proto.PeerInfo> targetPeers = getTargetPeers(request);

                // Simulate message delivery
                List<io.aurigraph.v11.proto.PeerDeliveryStatus> deliveryStatuses = new ArrayList<>();
                int peersReached = 0;
                int acknowledgementsReceived = 0;

                for (io.aurigraph.v11.proto.PeerInfo peer : targetPeers) {
                    // Simulate delivery time based on peer latency
                    double deliveryTime = peer.getLatencyMs() * (1.0 + Math.random() * 0.2);
                    boolean delivered = peer.getConnectionStatus() == io.aurigraph.v11.proto.PeerConnectionStatus.PEER_CONNECTED;
                    boolean acknowledged = delivered && request.getRequireAcknowledgment();

                    if (delivered) {
                        peersReached++;
                        totalMessagesSent.incrementAndGet();
                    }

                    if (acknowledged) {
                        acknowledgementsReceived++;
                    }

                    io.aurigraph.v11.proto.PeerDeliveryStatus status = io.aurigraph.v11.proto.PeerDeliveryStatus.newBuilder()
                        .setPeerId(peer.getPeerId())
                        .setDelivered(delivered)
                        .setAcknowledged(acknowledged)
                        .setDeliveryTimeMs(deliveryTime)
                        .setErrorMessage(delivered ? "" : "Peer not connected")
                        .build();

                    deliveryStatuses.add(status);
                }

                // Check if minimum acknowledgments requirement is met
                boolean success = true;
                String errorMessage = "";

                if (request.getRequireAcknowledgment() && request.getMinAcknowledgments() > 0) {
                    if (acknowledgementsReceived < request.getMinAcknowledgments()) {
                        success = false;
                        errorMessage = String.format("Required %d acknowledgments, received %d",
                            request.getMinAcknowledgments(), acknowledgementsReceived);
                    }
                }

                long elapsedMs = (System.nanoTime() - startTime) / 1_000_000;
                Log.infof("BroadcastMessage completed in %dms (target: <50ms) - %d peers reached, %d acknowledged",
                    elapsedMs, peersReached, acknowledgementsReceived);

                io.aurigraph.v11.proto.BroadcastMessageResponse response = io.aurigraph.v11.proto.BroadcastMessageResponse.newBuilder()
                    .setMessageId(messageId)
                    .setSuccess(success)
                    .setPeersReached(peersReached)
                    .setAcknowledgmentsReceived(acknowledgementsReceived)
                    .setTimestamp(getCurrentTimestamp())
                    .addAllDeliveryStatus(deliveryStatuses)
                    .setErrorMessage(errorMessage)
                    .build();

                // Store in broadcast history
                broadcastHistory.put(messageId, response);

                // Generate network event
                emitNetworkEvent(io.aurigraph.v11.proto.NetworkEventType.EVENT_MESSAGE_SENT,
                    messageId, String.format("Broadcast message sent to %d peers", peersReached));

                return response;

            } catch (Exception e) {
                Log.errorf("BroadcastMessage failed: %s", e.getMessage());
                return io.aurigraph.v11.proto.BroadcastMessageResponse.newBuilder()
                    .setMessageId(request.getMessageId())
                    .setSuccess(false)
                    .setErrorMessage("Broadcast failed: " + e.getMessage())
                    .setTimestamp(getCurrentTimestamp())
                    .build();
            }
        });
    }

    /**
     * subscribeNetworkEvents - Streams real-time network events
     * Performance Target: 100+ concurrent subscribers
     */
    @Override
    public Multi<io.aurigraph.v11.proto.NetworkEvent> subscribeNetworkEvents(io.aurigraph.v11.proto.NetworkEventSubscription request) {
        String streamId = UUID.randomUUID().toString();
        Log.infof("Starting network event stream: %s (active streams: %d)", streamId, activeStreams.size() + 1);

        // Create event stream
        Multi<io.aurigraph.v11.proto.NetworkEvent> eventStream = Multi.createFrom().ticks()
            .every(Duration.ofSeconds(2))
            .onItem().transform(tick -> {
                // Generate periodic network events
                io.aurigraph.v11.proto.NetworkEventType eventType = selectRandomEventType(request.getEventTypesList());
                String peerId = selectRandomPeerId(request.getFilterPeerId());
                String eventMessage = generateEventMessage(eventType, peerId);

                long sequence = eventSequenceCounter.incrementAndGet();

                io.aurigraph.v11.proto.NetworkEvent event = io.aurigraph.v11.proto.NetworkEvent.newBuilder()
                    .setEventId(UUID.randomUUID().toString())
                    .setEventType(eventType)
                    .setTimestamp(getCurrentTimestamp())
                    .setPeerId(peerId)
                    .setEventMessage(eventMessage)
                    .putEventMetadata("stream_id", streamId)
                    .putEventMetadata("tick", String.valueOf(tick))
                    .setStreamId(streamId)
                    .setEventSequence(sequence)
                    .build();

                // Store recent event
                recentEvents.offer(event);
                if (recentEvents.size() > 1000) {
                    recentEvents.poll();
                }

                return event;
            })
            .onCancellation().invoke(() -> {
                activeStreams.remove(streamId);
                Log.infof("Network event stream ended: %s (remaining streams: %d)", streamId, activeStreams.size());
            });

        // Register stream
        activeStreams.put(streamId, eventStream);

        // Include historical events if requested
        if (request.getIncludeHistorical()) {
            List<io.aurigraph.v11.proto.NetworkEvent> historicalEvents = new ArrayList<>(recentEvents);
            Multi<io.aurigraph.v11.proto.NetworkEvent> historical = Multi.createFrom().iterable(historicalEvents);
            return Multi.createBy().concatenating().streams(historical, eventStream);
        }

        return eventStream;
    }

    // ============================================================================
    // Helper Methods
    // ============================================================================

    private void initializeMockPeers() {
        // Initialize validator nodes (10 peers)
        for (int i = 1; i <= 10; i++) {
            String peerId = "validator-" + i;
            io.aurigraph.v11.proto.PeerInfo peer = io.aurigraph.v11.proto.PeerInfo.newBuilder()
                .setPeerId(peerId)
                .setIpAddress("10.0.1." + i)
                .setPort(9004)
                .setNodeType("VALIDATOR")
                .setConnectionStatus(io.aurigraph.v11.proto.PeerConnectionStatus.PEER_CONNECTED)
                .setLastSeen(getCurrentTimestamp())
                .setConnectedSince(getCurrentTimestamp())
                .setLatencyMs(5.0 + Math.random() * 10.0)
                .setMessagesSent(1000 + (long)(Math.random() * 5000))
                .setMessagesReceived(1200 + (long)(Math.random() * 5000))
                .setUptimePercent(99.5 + Math.random() * 0.5)
                .setProtocolVersion("v11.0.0")
                .addCapabilities("consensus")
                .addCapabilities("validation")
                .setBlockHeight(1000000 + i)
                .build();

            connectedPeers.put(peerId, peer);
        }

        // Initialize business nodes (15 peers)
        for (int i = 1; i <= 15; i++) {
            String peerId = "business-" + i;
            io.aurigraph.v11.proto.PeerInfo peer = io.aurigraph.v11.proto.PeerInfo.newBuilder()
                .setPeerId(peerId)
                .setIpAddress("10.0.2." + i)
                .setPort(9004)
                .setNodeType("BUSINESS")
                .setConnectionStatus(io.aurigraph.v11.proto.PeerConnectionStatus.PEER_CONNECTED)
                .setLastSeen(getCurrentTimestamp())
                .setConnectedSince(getCurrentTimestamp())
                .setLatencyMs(10.0 + Math.random() * 15.0)
                .setMessagesSent(500 + (long)(Math.random() * 3000))
                .setMessagesReceived(600 + (long)(Math.random() * 3000))
                .setUptimePercent(98.0 + Math.random() * 2.0)
                .setProtocolVersion("v11.0.0")
                .addCapabilities("transactions")
                .addCapabilities("smart_contracts")
                .setBlockHeight(1000000 + i)
                .build();

            connectedPeers.put(peerId, peer);
        }

        Log.infof("Initialized %d mock peers (%d validators, %d business nodes)",
            connectedPeers.size(), 10, 15);
    }

    private boolean filterPeer(io.aurigraph.v11.proto.PeerInfo peer, io.aurigraph.v11.proto.GetPeerListRequest request) {
        // Filter by connection status
        if (request.getFilterStatus() != io.aurigraph.v11.proto.PeerConnectionStatus.PEER_STATUS_UNKNOWN) {
            if (peer.getConnectionStatus() != request.getFilterStatus()) {
                return false;
            }
        }

        // Filter by node type
        if (!request.getFilterNodeType().isEmpty()) {
            if (!peer.getNodeType().equalsIgnoreCase(request.getFilterNodeType())) {
                return false;
            }
        }

        return true;
    }

    private void sortPeers(List<io.aurigraph.v11.proto.PeerInfo> peers, String sortBy, boolean descending) {
        Comparator<io.aurigraph.v11.proto.PeerInfo> comparator;

        switch (sortBy.toLowerCase()) {
            case "latency":
                comparator = Comparator.comparingDouble(io.aurigraph.v11.proto.PeerInfo::getLatencyMs);
                break;
            case "last_seen":
                comparator = Comparator.comparing(p -> p.getLastSeen().getSeconds());
                break;
            case "uptime":
                comparator = Comparator.comparingDouble(io.aurigraph.v11.proto.PeerInfo::getUptimePercent);
                break;
            default:
                comparator = Comparator.comparing(io.aurigraph.v11.proto.PeerInfo::getPeerId);
        }

        if (descending) {
            comparator = comparator.reversed();
        }

        peers.sort(comparator);
    }

    private List<io.aurigraph.v11.proto.PeerInfo> getTargetPeers(io.aurigraph.v11.proto.BroadcastMessageRequest request) {
        // If specific peers are targeted
        if (!request.getTargetPeerIdsList().isEmpty()) {
            return request.getTargetPeerIdsList().stream()
                .map(connectedPeers::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        }

        // Filter by node type if specified
        if (!request.getTargetNodeType().isEmpty()) {
            return connectedPeers.values().stream()
                .filter(peer -> peer.getNodeType().equalsIgnoreCase(request.getTargetNodeType()))
                .filter(peer -> peer.getConnectionStatus() == io.aurigraph.v11.proto.PeerConnectionStatus.PEER_CONNECTED)
                .collect(Collectors.toList());
        }

        // Default: all connected peers
        return connectedPeers.values().stream()
            .filter(peer -> peer.getConnectionStatus() == io.aurigraph.v11.proto.PeerConnectionStatus.PEER_CONNECTED)
            .collect(Collectors.toList());
    }

    private List<String> getHubNodes() {
        // Return top 3 nodes with highest message counts
        return connectedPeers.values().stream()
            .sorted(Comparator.comparingLong(p -> -(p.getMessagesSent() + p.getMessagesReceived())))
            .limit(3)
            .map(io.aurigraph.v11.proto.PeerInfo::getPeerId)
            .collect(Collectors.toList());
    }

    private io.aurigraph.v11.proto.NetworkEventType selectRandomEventType(List<io.aurigraph.v11.proto.NetworkEventType> filterTypes) {
        // If filter is specified, rotate through filtered types
        if (filterTypes != null && !filterTypes.isEmpty()) {
            int index = (int)(eventSequenceCounter.get() % filterTypes.size());
            return filterTypes.get(index);
        }

        // Default: rotate through common event types
        io.aurigraph.v11.proto.NetworkEventType[] commonEvents = {
            io.aurigraph.v11.proto.NetworkEventType.EVENT_MESSAGE_RECEIVED,
            io.aurigraph.v11.proto.NetworkEventType.EVENT_MESSAGE_SENT,
            io.aurigraph.v11.proto.NetworkEventType.EVENT_PEER_CONNECTED,
            io.aurigraph.v11.proto.NetworkEventType.EVENT_SYNC_COMPLETED
        };

        int index = (int)(eventSequenceCounter.get() % commonEvents.length);
        return commonEvents[index];
    }

    private String selectRandomPeerId(String filterPeerId) {
        // If filter specified, use it
        if (filterPeerId != null && !filterPeerId.isEmpty()) {
            return filterPeerId;
        }

        // Select random peer
        List<String> peerIds = new ArrayList<>(connectedPeers.keySet());
        if (peerIds.isEmpty()) {
            return "unknown";
        }

        int index = (int)(Math.random() * peerIds.size());
        return peerIds.get(index);
    }

    private String generateEventMessage(io.aurigraph.v11.proto.NetworkEventType eventType, String peerId) {
        return switch (eventType) {
            case EVENT_PEER_CONNECTED -> "Peer " + peerId + " connected to network";
            case EVENT_PEER_DISCONNECTED -> "Peer " + peerId + " disconnected from network";
            case EVENT_MESSAGE_RECEIVED -> "Message received from peer " + peerId;
            case EVENT_MESSAGE_SENT -> "Message sent to peer " + peerId;
            case EVENT_SYNC_STARTED -> "Blockchain sync started with peer " + peerId;
            case EVENT_SYNC_COMPLETED -> "Blockchain sync completed with peer " + peerId;
            case EVENT_HIGH_LATENCY -> "High latency detected on peer " + peerId;
            default -> "Network event from peer " + peerId;
        };
    }

    private void emitNetworkEvent(io.aurigraph.v11.proto.NetworkEventType eventType, String peerId, String message) {
        io.aurigraph.v11.proto.NetworkEvent event = io.aurigraph.v11.proto.NetworkEvent.newBuilder()
            .setEventId(UUID.randomUUID().toString())
            .setEventType(eventType)
            .setTimestamp(getCurrentTimestamp())
            .setPeerId(peerId)
            .setEventMessage(message)
            .setEventSequence(eventSequenceCounter.incrementAndGet())
            .build();

        recentEvents.offer(event);
        if (recentEvents.size() > 1000) {
            recentEvents.poll();
        }
    }

    private Timestamp getCurrentTimestamp() {
        Instant now = Instant.now();
        return Timestamp.newBuilder()
            .setSeconds(now.getEpochSecond())
            .setNanos(now.getNano())
            .build();
    }

    // ============================================================================
    // Public API for other services
    // ============================================================================

    public int getConnectedPeerCount() {
        return (int) connectedPeers.values().stream()
            .filter(peer -> peer.getConnectionStatus() == io.aurigraph.v11.proto.PeerConnectionStatus.PEER_CONNECTED)
            .count();
    }

    public Map<String, io.aurigraph.v11.proto.PeerInfo> getAllPeers() {
        return new HashMap<>(connectedPeers);
    }
}
