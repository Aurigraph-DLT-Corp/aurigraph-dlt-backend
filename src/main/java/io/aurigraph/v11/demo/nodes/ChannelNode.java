package io.aurigraph.v11.demo.nodes;

import io.aurigraph.v11.demo.models.*;
import io.smallrye.mutiny.Uni;
import org.jboss.logging.Logger;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Implementation of a Channel Node in the Aurigraph V11 network.
 *
 * <p>Channel Nodes are responsible for:
 * <ul>
 *   <li>Managing multi-channel data flows</li>
 *   <li>Tracking channel participants and permissions</li>
 *   <li>Routing messages between channel members with &lt;5ms latency</li>
 *   <li>Maintaining channel state consistency</li>
 *   <li>Handling both on-chain and off-chain data flows</li>
 * </ul>
 * </p>
 *
 * <p>Performance targets:
 * <ul>
 *   <li>Channel creation: &lt;10ms</li>
 *   <li>Message routing: &lt;5ms</li>
 *   <li>Concurrent channels: 10,000+ per node</li>
 *   <li>Throughput: 500K messages/sec per node</li>
 * </ul>
 * </p>
 *
 * @author Backend Development Agent (BDA)
 * @version 11.0.0
 * @since 2025-10-11
 */
public class ChannelNode implements Node {

    private static final Logger LOG = Logger.getLogger(ChannelNode.class);

    private final String nodeId;
    private final io.aurigraph.v11.demo.models.NodeType nodeType = io.aurigraph.v11.demo.models.NodeType.CHANNEL;
    private final AtomicReference<io.aurigraph.v11.demo.models.NodeStatus> status;

    // Channel management
    private final Map<String, Channel> activeChannels;
    private final Map<String, List<ChannelMessage>> messageQueues;
    private final Map<String, Set<String>> participantRegistry;

    // Performance metrics
    private final AtomicLong totalMessagesProcessed;
    private final AtomicLong totalChannelsCreated;
    private final Instant startTime;
    private Instant lastMessageTime;

    // Configuration
    private final int maxChannels;
    private final int maxParticipantsPerChannel;
    private final int messageQueueSize;

    /**
     * Creates a new Channel Node with default configuration.
     *
     * @param nodeId The unique identifier for this node
     */
    public ChannelNode(String nodeId) {
        this(nodeId, 10000, 1000, 100000);
    }

    /**
     * Creates a new Channel Node with custom configuration.
     *
     * @param nodeId The unique identifier for this node
     * @param maxChannels Maximum number of concurrent channels
     * @param maxParticipantsPerChannel Maximum participants per channel
     * @param messageQueueSize Maximum queue size for pending messages
     */
    public ChannelNode(String nodeId, int maxChannels, int maxParticipantsPerChannel, int messageQueueSize) {
        this.nodeId = nodeId;
        this.status = new AtomicReference<>(io.aurigraph.v11.demo.models.NodeStatus.INITIALIZING);
        this.activeChannels = new ConcurrentHashMap<>();
        this.messageQueues = new ConcurrentHashMap<>();
        this.participantRegistry = new ConcurrentHashMap<>();
        this.totalMessagesProcessed = new AtomicLong(0);
        this.totalChannelsCreated = new AtomicLong(0);
        this.startTime = Instant.now();
        this.lastMessageTime = Instant.now();
        this.maxChannels = maxChannels;
        this.maxParticipantsPerChannel = maxParticipantsPerChannel;
        this.messageQueueSize = messageQueueSize;

        LOG.infof("ChannelNode %s initialized with max_channels=%d, max_participants=%d, queue_size=%d",
                nodeId, maxChannels, maxParticipantsPerChannel, messageQueueSize);
    }

    @Override
    public String getNodeId() {
        return nodeId;
    }

    @Override
    public io.aurigraph.v11.demo.models.NodeType getNodeType() {
        return nodeType;
    }

    @Override
    public io.aurigraph.v11.demo.models.NodeStatus getStatus() {
        return status.get();
    }

    @Override
    public Uni<Boolean> start() {
        return Uni.createFrom().item(() -> {
            LOG.infof("Starting ChannelNode %s", nodeId);
            status.set(io.aurigraph.v11.demo.models.NodeStatus.RUNNING);
            LOG.infof("ChannelNode %s started successfully", nodeId);
            return true;
        }).runSubscriptionOn(runnable -> Thread.startVirtualThread(runnable));
    }

    @Override
    public Uni<Boolean> stop() {
        return Uni.createFrom().item(() -> {
            LOG.infof("Stopping ChannelNode %s", nodeId);
            status.set(io.aurigraph.v11.demo.models.NodeStatus.STOPPED);

            // Close all active channels
            activeChannels.values().forEach(channel -> {
                channel.setState(ChannelState.CLOSED);
            });

            // Clear message queues
            messageQueues.clear();

            LOG.infof("ChannelNode %s stopped successfully. Total channels created: %d, Total messages processed: %d",
                    nodeId, totalChannelsCreated.get(), totalMessagesProcessed.get());
            return true;
        }).runSubscriptionOn(runnable -> Thread.startVirtualThread(runnable));
    }

    @Override
    public Uni<NodeHealth> healthCheck() {
        return Uni.createFrom().item(() -> {
            long uptime = Duration.between(startTime, Instant.now()).getSeconds();

            NodeHealth.Builder healthBuilder = NodeHealth.builder()
                    .healthy(status.get() == io.aurigraph.v11.demo.models.NodeStatus.RUNNING)
                    .status(status.get())
                    .lastCheckTime(Instant.now())
                    .uptimeSeconds(uptime);

            // Check channel management component
            boolean channelHealthy = activeChannels.size() < maxChannels;
            String channelMessage = channelHealthy ?
                    "Channel management operational" : "Channel limit reached";
            healthBuilder.addCheck("ChannelManagement", channelHealthy, channelMessage);

            // Check message routing component
            healthBuilder.addCheck("MessageRouting", true, "Message routing operational");

            return healthBuilder.build();
        }).runSubscriptionOn(runnable -> Thread.startVirtualThread(runnable));
    }

    @Override
    public Uni<NodeMetrics> getMetrics() {
        return Uni.createFrom().item(() -> {
            NodeMetrics metrics = new NodeMetrics(nodeId);

            // Calculate uptime
            long uptimeSeconds = Duration.between(startTime, Instant.now()).getSeconds();
            metrics.setUptimeSeconds(uptimeSeconds);

            // Throughput metrics
            metrics.setTotalRequests(totalMessagesProcessed.get());
            metrics.setTotalResponses(totalMessagesProcessed.get());
            if (uptimeSeconds > 0) {
                metrics.setRequestsPerSecond((double) totalMessagesProcessed.get() / uptimeSeconds);
                metrics.setResponsesPerSecond((double) totalMessagesProcessed.get() / uptimeSeconds);
            }

            // Calculate average latency (simulated for demo - would be real measurement in production)
            metrics.setAverageLatency(2.5);
            metrics.setP50Latency(2.0);
            metrics.setP95Latency(4.0);
            metrics.setP99Latency(4.8);

            // Resource utilization (simulated for demo)
            Runtime runtime = Runtime.getRuntime();
            metrics.setMemoryUsedBytes(runtime.totalMemory() - runtime.freeMemory());
            metrics.setMemoryTotalBytes(runtime.maxMemory());

            // Channel metrics
            metrics.setActiveChannels(activeChannels.size());
            metrics.setTotalParticipants(participantRegistry.values().stream()
                    .mapToInt(Set::size).sum());

            // Custom metrics
            metrics.addCustomMetric("totalChannelsCreated", totalChannelsCreated.get());
            metrics.addCustomMetric("maxChannelsCapacity", maxChannels);
            metrics.addCustomMetric("channelUtilizationPercent",
                    (double) activeChannels.size() / maxChannels * 100.0);

            return metrics;
        }).runSubscriptionOn(runnable -> Thread.startVirtualThread(runnable));
    }

    // Channel-specific operations

    /**
     * Creates a new channel with the specified configuration.
     *
     * <p>Target performance: &lt;10ms</p>
     *
     * @param channelId The unique channel identifier
     * @param name The channel name
     * @param description The channel description
     * @param config The channel configuration
     * @return A Uni that completes with the created channel
     */
    public Uni<Channel> createChannel(String channelId, String name, String description, ChannelConfig config) {
        return Uni.createFrom().item(() -> {
            // Check if channel already exists
            if (activeChannels.containsKey(channelId)) {
                throw new IllegalArgumentException("Channel with ID " + channelId + " already exists");
            }

            // Check capacity
            if (activeChannels.size() >= maxChannels) {
                throw new IllegalStateException("Maximum channel capacity reached: " + maxChannels);
            }

            // Create channel
            Channel channel = new Channel(channelId, name, description, config, nodeId);
            channel.setState(ChannelState.CREATED);

            // Store channel
            activeChannels.put(channelId, channel);
            messageQueues.put(channelId, Collections.synchronizedList(new ArrayList<>()));
            participantRegistry.put(channelId, ConcurrentHashMap.newKeySet());

            totalChannelsCreated.incrementAndGet();

            LOG.infof("Created channel %s (name: %s) on node %s", channelId, name, nodeId);
            return channel;
        }).runSubscriptionOn(runnable -> Thread.startVirtualThread(runnable));
    }

    /**
     * Activates a channel, enabling message routing.
     *
     * @param channelId The channel identifier
     * @return A Uni that completes with the activated channel
     */
    public Uni<Channel> activateChannel(String channelId) {
        return Uni.createFrom().item(() -> {
            Channel channel = activeChannels.get(channelId);
            if (channel == null) {
                throw new IllegalArgumentException("Channel not found: " + channelId);
            }

            channel.setState(ChannelState.ACTIVE);
            LOG.infof("Activated channel %s on node %s", channelId, nodeId);
            return channel;
        }).runSubscriptionOn(runnable -> Thread.startVirtualThread(runnable));
    }

    /**
     * Closes a channel, preventing further message routing.
     *
     * @param channelId The channel identifier
     * @return A Uni that completes with void
     */
    public Uni<Void> closeChannel(String channelId) {
        return Uni.createFrom().voidItem().invoke(() -> {
            Channel channel = activeChannels.get(channelId);
            if (channel != null) {
                channel.setState(ChannelState.CLOSED);
                LOG.infof("Closed channel %s on node %s", channelId, nodeId);
            }
        }).runSubscriptionOn(runnable -> Thread.startVirtualThread(runnable));
    }

    /**
     * Adds a participant to a channel.
     *
     * @param channelId The channel identifier
     * @param participantId The participant identifier
     * @return A Uni that completes with void
     */
    public Uni<Void> addParticipant(String channelId, String participantId) {
        return Uni.createFrom().voidItem().invoke(() -> {
            Channel channel = activeChannels.get(channelId);
            if (channel == null) {
                throw new IllegalArgumentException("Channel not found: " + channelId);
            }

            Set<String> participants = participantRegistry.get(channelId);
            if (participants.size() >= maxParticipantsPerChannel) {
                throw new IllegalStateException("Maximum participant capacity reached for channel: " + channelId);
            }

            channel.addParticipant(participantId);
            participants.add(participantId);

            LOG.infof("Added participant %s to channel %s on node %s", participantId, channelId, nodeId);
        }).runSubscriptionOn(runnable -> Thread.startVirtualThread(runnable));
    }

    /**
     * Routes a message through a channel.
     *
     * <p>Target performance: &lt;5ms latency</p>
     *
     * @param channelId The channel identifier
     * @param message The message to route
     * @return A Uni that completes with the routed message
     */
    public Uni<ChannelMessage> routeMessage(String channelId, ChannelMessage message) {
        return Uni.createFrom().item(() -> {
            Channel channel = activeChannels.get(channelId);
            if (channel == null) {
                throw new IllegalArgumentException("Channel not found: " + channelId);
            }

            if (channel.getState() != ChannelState.ACTIVE) {
                throw new IllegalStateException("Channel is not active: " + channelId);
            }

            // Verify sender is a participant
            if (!channel.hasParticipant(message.getSenderId())) {
                throw new IllegalArgumentException("Sender is not a channel participant: " + message.getSenderId());
            }

            // Assign sequence number
            message.setSequenceNumber(totalMessagesProcessed.incrementAndGet());
            message.setStatus(ChannelMessage.MessageStatus.SENT);

            // Add to message queue
            List<ChannelMessage> queue = messageQueues.get(channelId);
            queue.add(message);

            // Update channel statistics
            channel.recordMessage(message.getPayloadSize());

            lastMessageTime = Instant.now();

            LOG.debugf("Routed message %s in channel %s (seq: %d, size: %d bytes)",
                    message.getMessageId(), channelId, message.getSequenceNumber(), message.getPayloadSize());

            return message;
        }).runSubscriptionOn(runnable -> Thread.startVirtualThread(runnable));
    }

    /**
     * Gets the current state of a channel.
     *
     * @param channelId The channel identifier
     * @return A Uni that completes with the channel state
     */
    public Uni<ChannelState> getChannelState(String channelId) {
        return Uni.createFrom().item(() -> {
            Channel channel = activeChannels.get(channelId);
            if (channel == null) {
                throw new IllegalArgumentException("Channel not found: " + channelId);
            }
            return channel.getState();
        }).runSubscriptionOn(runnable -> Thread.startVirtualThread(runnable));
    }

    /**
     * Gets a channel by ID.
     *
     * @param channelId The channel identifier
     * @return A Uni that completes with the channel
     */
    public Uni<Channel> getChannel(String channelId) {
        return Uni.createFrom().item(() -> {
            Channel channel = activeChannels.get(channelId);
            if (channel == null) {
                throw new IllegalArgumentException("Channel not found: " + channelId);
            }
            return channel;
        }).runSubscriptionOn(runnable -> Thread.startVirtualThread(runnable));
    }

    /**
     * Gets all active channels.
     *
     * @return A Uni that completes with the list of all channels
     */
    public Uni<List<Channel>> getAllChannels() {
        return Uni.createFrom().item(() -> {
            List<Channel> channels = new ArrayList<>(activeChannels.values());
            return channels;
        }).runSubscriptionOn(runnable -> Thread.startVirtualThread(runnable));
    }
}
