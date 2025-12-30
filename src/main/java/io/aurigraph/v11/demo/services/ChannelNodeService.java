package io.aurigraph.v11.demo.services;

import io.aurigraph.v11.demo.models.*;
import io.aurigraph.v11.demo.nodes.ChannelNode;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.UUID;

/**
 * Service for managing Channel Node operations in the Aurigraph V11 network.
 *
 * <p>This service provides a high-level API for:
 * <ul>
 *   <li>Channel lifecycle management (create, activate, close)</li>
 *   <li>Participant management (add, remove, query)</li>
 *   <li>Message routing with performance optimization</li>
 *   <li>Channel state queries and monitoring</li>
 * </ul>
 * </p>
 *
 * <p>The service is implemented as a CDI ApplicationScoped bean and manages
 * a single ChannelNode instance for the entire application.</p>
 *
 * @author Backend Development Agent (BDA)
 * @version 11.0.0
 * @since 2025-10-11
 */
@ApplicationScoped
public class ChannelNodeService {

    private static final Logger LOG = Logger.getLogger(ChannelNodeService.class);

    private ChannelNode channelNode;

    /**
     * Initializes the Channel Node on service startup.
     */
    @PostConstruct
    public void init() {
        String nodeId = "channel-node-" + UUID.randomUUID().toString().substring(0, 8);
        LOG.infof("Initializing ChannelNodeService with nodeId: %s", nodeId);

        // Create channel node with production-ready configuration
        channelNode = new ChannelNode(
                nodeId,
                10000,   // maxChannels: 10,000 concurrent channels
                1000,    // maxParticipantsPerChannel: 1,000 participants
                100000   // messageQueueSize: 100,000 pending messages
        );

        // Start the node
        channelNode.start()
                .subscribe()
                .with(
                        success -> LOG.infof("ChannelNode %s started successfully", nodeId),
                        failure -> LOG.errorf(failure, "Failed to start ChannelNode %s", nodeId)
                );
    }

    /**
     * Shuts down the Channel Node on service destruction.
     */
    @PreDestroy
    public void destroy() {
        if (channelNode != null) {
            LOG.infof("Shutting down ChannelNodeService");
            channelNode.stop()
                    .subscribe()
                    .with(
                            success -> LOG.info("ChannelNode stopped successfully"),
                            failure -> LOG.error("Failed to stop ChannelNode", failure)
                    );
        }
    }

    /**
     * Gets the Channel Node instance.
     *
     * @return The channel node
     */
    public ChannelNode getChannelNode() {
        return channelNode;
    }

    /**
     * Creates a new channel with the specified parameters.
     *
     * @param name The channel name
     * @param description The channel description
     * @param config The channel configuration (optional, uses defaults if null)
     * @return A Uni that completes with the created channel
     */
    public Uni<Channel> createChannel(String name, String description, ChannelConfig config) {
        String channelId = "channel-" + UUID.randomUUID().toString();
        ChannelConfig channelConfig = config != null ? config : new ChannelConfig();

        LOG.infof("Creating channel: %s (name: %s)", channelId, name);

        return channelNode.createChannel(channelId, name, description, channelConfig)
                .onItem().invoke(channel -> LOG.infof("Channel created successfully: %s", channelId))
                .onFailure().invoke(failure -> LOG.errorf(failure, "Failed to create channel: %s", name));
    }

    /**
     * Activates a channel to enable message routing.
     *
     * @param channelId The channel identifier
     * @return A Uni that completes with the activated channel
     */
    public Uni<Channel> activateChannel(String channelId) {
        LOG.infof("Activating channel: %s", channelId);

        return channelNode.activateChannel(channelId)
                .onItem().invoke(channel -> LOG.infof("Channel activated successfully: %s", channelId))
                .onFailure().invoke(failure -> LOG.errorf(failure, "Failed to activate channel: %s", channelId));
    }

    /**
     * Closes a channel permanently.
     *
     * @param channelId The channel identifier
     * @return A Uni that completes with void
     */
    public Uni<Void> closeChannel(String channelId) {
        LOG.infof("Closing channel: %s", channelId);

        return channelNode.closeChannel(channelId)
                .onItem().invoke(() -> LOG.infof("Channel closed successfully: %s", channelId))
                .onFailure().invoke(failure -> LOG.errorf(failure, "Failed to close channel: %s", channelId));
    }

    /**
     * Adds a participant to a channel.
     *
     * @param channelId The channel identifier
     * @param participantId The participant identifier
     * @return A Uni that completes with void
     */
    public Uni<Void> addParticipant(String channelId, String participantId) {
        LOG.infof("Adding participant %s to channel %s", participantId, channelId);

        return channelNode.addParticipant(channelId, participantId)
                .onItem().invoke(() -> LOG.infof("Participant added successfully: %s to channel %s",
                        participantId, channelId))
                .onFailure().invoke(failure -> LOG.errorf(failure,
                        "Failed to add participant %s to channel %s", participantId, channelId));
    }

    /**
     * Routes a message through a channel.
     *
     * <p>Performance optimization techniques used:
     * <ul>
     *   <li>Asynchronous processing with virtual threads</li>
     *   <li>Lock-free concurrent data structures</li>
     *   <li>Message batching for high throughput</li>
     *   <li>Zero-copy message handling where possible</li>
     * </ul>
     * </p>
     *
     * @param channelId The channel identifier
     * @param senderId The sender's participant ID
     * @param recipientId The recipient's participant ID (null for broadcast)
     * @param payload The message payload
     * @return A Uni that completes with the routed message
     */
    public Uni<ChannelMessage> routeMessage(String channelId, String senderId,
                                           String recipientId, byte[] payload) {
        String messageId = "msg-" + UUID.randomUUID().toString();

        ChannelMessage message = new ChannelMessage(
                messageId,
                channelId,
                senderId,
                ChannelMessage.MessageType.DATA,
                payload
        );
        message.setRecipientId(recipientId);

        LOG.debugf("Routing message %s in channel %s (sender: %s, size: %d bytes)",
                messageId, channelId, senderId, payload.length);

        return channelNode.routeMessage(channelId, message)
                .onItem().invoke(routedMessage -> LOG.debugf("Message routed successfully: %s", messageId))
                .onFailure().invoke(failure -> LOG.errorf(failure, "Failed to route message: %s", messageId));
    }

    /**
     * Gets a channel by its identifier.
     *
     * @param channelId The channel identifier
     * @return A Uni that completes with the channel
     */
    public Uni<Channel> getChannel(String channelId) {
        return channelNode.getChannel(channelId)
                .onFailure().invoke(failure -> LOG.errorf(failure, "Failed to get channel: %s", channelId));
    }

    /**
     * Gets all active channels.
     *
     * @return A Uni that completes with the list of all channels
     */
    public Uni<List<Channel>> getAllChannels() {
        return channelNode.getAllChannels()
                .onItem().invoke(channels -> LOG.debugf("Retrieved %d channels", channels.size()))
                .onFailure().invoke(failure -> LOG.error("Failed to get all channels", failure));
    }

    /**
     * Gets the current state of a channel.
     *
     * @param channelId The channel identifier
     * @return A Uni that completes with the channel state
     */
    public Uni<ChannelState> getChannelState(String channelId) {
        return channelNode.getChannelState(channelId)
                .onFailure().invoke(failure -> LOG.errorf(failure,
                        "Failed to get channel state: %s", channelId));
    }

    /**
     * Performs a health check on the Channel Node.
     *
     * @return A Uni that completes with the health status
     */
    public Uni<NodeHealth> healthCheck() {
        return channelNode.healthCheck()
                .onFailure().invoke(failure -> LOG.error("Health check failed", failure));
    }

    /**
     * Gets performance metrics for the Channel Node.
     *
     * @return A Uni that completes with the metrics
     */
    public Uni<NodeMetrics> getMetrics() {
        return channelNode.getMetrics()
                .onFailure().invoke(failure -> LOG.error("Failed to get metrics", failure));
    }

    /**
     * Gets the node identifier.
     *
     * @return The node ID
     */
    public String getNodeId() {
        return channelNode.getNodeId();
    }

    /**
     * Gets the current node status.
     *
     * @return The node status
     */
    public NodeStatus getNodeStatus() {
        return channelNode.getStatus();
    }
}
