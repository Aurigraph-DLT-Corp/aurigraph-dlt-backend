package io.aurigraph.v11.demo.api;

import io.aurigraph.v11.demo.models.*;
import io.aurigraph.v11.demo.services.ChannelNodeService;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.Map;

/**
 * REST API resource for Channel Node operations in the Aurigraph V11 network.
 *
 * <p>This resource provides HTTP endpoints for:
 * <ul>
 *   <li>Channel lifecycle management (POST /channels, GET /channels/{id})</li>
 *   <li>Channel activation and closure (PUT /channels/{id}/activate, PUT /channels/{id}/close)</li>
 *   <li>Participant management (POST /channels/{id}/participants)</li>
 *   <li>Message routing (POST /channels/{id}/messages)</li>
 *   <li>Health monitoring (GET /health)</li>
 *   <li>Performance metrics (GET /metrics)</li>
 * </ul>
 * </p>
 *
 * <p><b>API Endpoints:</b>
 * <pre>
 * POST   /api/v11/demo/channel-nodes/channels              - Create a new channel
 * GET    /api/v11/demo/channel-nodes/channels              - Get all channels
 * GET    /api/v11/demo/channel-nodes/channels/{id}         - Get channel by ID
 * PUT    /api/v11/demo/channel-nodes/channels/{id}/activate - Activate a channel
 * PUT    /api/v11/demo/channel-nodes/channels/{id}/close   - Close a channel
 * GET    /api/v11/demo/channel-nodes/channels/{id}/state   - Get channel state
 * POST   /api/v11/demo/channel-nodes/channels/{id}/participants - Add participant
 * POST   /api/v11/demo/channel-nodes/channels/{id}/messages - Route a message
 * GET    /api/v11/demo/channel-nodes/health                - Health check
 * GET    /api/v11/demo/channel-nodes/metrics               - Performance metrics
 * GET    /api/v11/demo/channel-nodes/info                  - Node information
 * </pre>
 * </p>
 *
 * @author Backend Development Agent (BDA)
 * @version 11.0.0
 * @since 2025-10-11
 */
@Path("/api/v11/demo/channel-nodes")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ChannelNodeResource {

    private static final Logger LOG = Logger.getLogger(ChannelNodeResource.class);

    @Inject
    ChannelNodeService channelNodeService;

    /**
     * Creates a new channel.
     *
     * <p><b>Request Body Example:</b>
     * <pre>
     * {
     *   "name": "Trading Channel",
     *   "description": "Real-time market data channel",
     *   "config": {
     *     "maxParticipants": 1000,
     *     "messageQueueSize": 100000,
     *     "enableOffChainData": true
     *   }
     * }
     * </pre>
     * </p>
     *
     * @param request The channel creation request
     * @return A Uni that completes with the created channel
     */
    @POST
    @Path("/channels")
    public Uni<Response> createChannel(CreateChannelRequest request) {
        LOG.infof("REST API: Creating channel '%s'", request.name);

        return channelNodeService.createChannel(request.name, request.description, request.config)
                .onItem().transform(channel -> Response.status(Response.Status.CREATED)
                        .entity(channel)
                        .build())
                .onFailure().recoverWithItem(failure -> {
                    LOG.errorf(failure, "Failed to create channel '%s'", request.name);
                    return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                            .entity(Map.of("error", failure.getMessage()))
                            .build();
                });
    }

    /**
     * Gets all active channels.
     *
     * @return A Uni that completes with the list of all channels
     */
    @GET
    @Path("/channels")
    public Uni<Response> getAllChannels() {
        LOG.debug("REST API: Getting all channels");

        return channelNodeService.getAllChannels()
                .onItem().transform(channels -> Response.ok(channels).build())
                .onFailure().recoverWithItem(failure -> {
                    LOG.error("Failed to get all channels", failure);
                    return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                            .entity(Map.of("error", failure.getMessage()))
                            .build();
                });
    }

    /**
     * Gets a channel by its identifier.
     *
     * @param channelId The channel identifier
     * @return A Uni that completes with the channel
     */
    @GET
    @Path("/channels/{id}")
    public Uni<Response> getChannel(@PathParam("id") String channelId) {
        LOG.debugf("REST API: Getting channel %s", channelId);

        return channelNodeService.getChannel(channelId)
                .onItem().transform(channel -> Response.ok(channel).build())
                .onFailure().recoverWithItem(failure -> {
                    LOG.errorf(failure, "Failed to get channel %s", channelId);
                    return Response.status(Response.Status.NOT_FOUND)
                            .entity(Map.of("error", failure.getMessage()))
                            .build();
                });
    }

    /**
     * Activates a channel to enable message routing.
     *
     * @param channelId The channel identifier
     * @return A Uni that completes with the activated channel
     */
    @PUT
    @Path("/channels/{id}/activate")
    public Uni<Response> activateChannel(@PathParam("id") String channelId) {
        LOG.infof("REST API: Activating channel %s", channelId);

        return channelNodeService.activateChannel(channelId)
                .onItem().transform(channel -> Response.ok(channel).build())
                .onFailure().recoverWithItem(failure -> {
                    LOG.errorf(failure, "Failed to activate channel %s", channelId);
                    return Response.status(Response.Status.BAD_REQUEST)
                            .entity(Map.of("error", failure.getMessage()))
                            .build();
                });
    }

    /**
     * Closes a channel permanently.
     *
     * @param channelId The channel identifier
     * @return A Uni that completes with success status
     */
    @PUT
    @Path("/channels/{id}/close")
    public Uni<Response> closeChannel(@PathParam("id") String channelId) {
        LOG.infof("REST API: Closing channel %s", channelId);

        return channelNodeService.closeChannel(channelId)
                .onItem().transform(v -> Response.ok()
                        .entity(Map.of("message", "Channel closed successfully", "channelId", channelId))
                        .build())
                .onFailure().recoverWithItem(failure -> {
                    LOG.errorf(failure, "Failed to close channel %s", channelId);
                    return Response.status(Response.Status.BAD_REQUEST)
                            .entity(Map.of("error", failure.getMessage()))
                            .build();
                });
    }

    /**
     * Gets the current state of a channel.
     *
     * @param channelId The channel identifier
     * @return A Uni that completes with the channel state
     */
    @GET
    @Path("/channels/{id}/state")
    public Uni<Response> getChannelState(@PathParam("id") String channelId) {
        LOG.debugf("REST API: Getting state for channel %s", channelId);

        return channelNodeService.getChannelState(channelId)
                .onItem().transform(state -> Response.ok()
                        .entity(Map.of("channelId", channelId, "state", state))
                        .build())
                .onFailure().recoverWithItem(failure -> {
                    LOG.errorf(failure, "Failed to get channel state %s", channelId);
                    return Response.status(Response.Status.NOT_FOUND)
                            .entity(Map.of("error", failure.getMessage()))
                            .build();
                });
    }

    /**
     * Adds a participant to a channel.
     *
     * <p><b>Request Body Example:</b>
     * <pre>
     * {
     *   "participantId": "participant-12345"
     * }
     * </pre>
     * </p>
     *
     * @param channelId The channel identifier
     * @param request The add participant request
     * @return A Uni that completes with success status
     */
    @POST
    @Path("/channels/{id}/participants")
    public Uni<Response> addParticipant(@PathParam("id") String channelId,
                                       AddParticipantRequest request) {
        LOG.infof("REST API: Adding participant %s to channel %s", request.participantId, channelId);

        return channelNodeService.addParticipant(channelId, request.participantId)
                .onItem().transform(v -> Response.ok()
                        .entity(Map.of(
                                "message", "Participant added successfully",
                                "channelId", channelId,
                                "participantId", request.participantId
                        ))
                        .build())
                .onFailure().recoverWithItem(failure -> {
                    LOG.errorf(failure, "Failed to add participant %s to channel %s",
                            request.participantId, channelId);
                    return Response.status(Response.Status.BAD_REQUEST)
                            .entity(Map.of("error", failure.getMessage()))
                            .build();
                });
    }

    /**
     * Routes a message through a channel.
     *
     * <p><b>Request Body Example:</b>
     * <pre>
     * {
     *   "senderId": "participant-001",
     *   "recipientId": "participant-002",
     *   "payload": "SGVsbG8gV29ybGQ="
     * }
     * </pre>
     * Note: payload should be Base64-encoded binary data
     * </p>
     *
     * @param channelId The channel identifier
     * @param request The message routing request
     * @return A Uni that completes with the routed message
     */
    @POST
    @Path("/channels/{id}/messages")
    public Uni<Response> routeMessage(@PathParam("id") String channelId,
                                     RouteMessageRequest request) {
        LOG.debugf("REST API: Routing message in channel %s from %s to %s (size: %d bytes)",
                channelId, request.senderId, request.recipientId,
                request.payload != null ? request.payload.length : 0);

        return channelNodeService.routeMessage(
                        channelId,
                        request.senderId,
                        request.recipientId,
                        request.payload
                )
                .onItem().transform(message -> Response.status(Response.Status.CREATED)
                        .entity(message)
                        .build())
                .onFailure().recoverWithItem(failure -> {
                    LOG.errorf(failure, "Failed to route message in channel %s", channelId);
                    return Response.status(Response.Status.BAD_REQUEST)
                            .entity(Map.of("error", failure.getMessage()))
                            .build();
                });
    }

    /**
     * Performs a health check on the Channel Node.
     *
     * @return A Uni that completes with the health status
     */
    @GET
    @Path("/health")
    public Uni<Response> healthCheck() {
        LOG.debug("REST API: Health check requested");

        return channelNodeService.healthCheck()
                .onItem().transform(health -> Response.ok(health).build())
                .onFailure().recoverWithItem(failure -> {
                    LOG.error("Health check failed", failure);
                    return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                            .entity(Map.of("error", failure.getMessage()))
                            .build();
                });
    }

    /**
     * Gets performance metrics for the Channel Node.
     *
     * @return A Uni that completes with the metrics
     */
    @GET
    @Path("/metrics")
    public Uni<Response> getMetrics() {
        LOG.debug("REST API: Metrics requested");

        return channelNodeService.getMetrics()
                .onItem().transform(metrics -> Response.ok(metrics).build())
                .onFailure().recoverWithItem(failure -> {
                    LOG.error("Failed to get metrics", failure);
                    return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                            .entity(Map.of("error", failure.getMessage()))
                            .build();
                });
    }

    /**
     * Gets general information about the Channel Node.
     *
     * @return A response with node information
     */
    @GET
    @Path("/info")
    public Response getNodeInfo() {
        return Response.ok(Map.of(
                "nodeId", channelNodeService.getNodeId(),
                "nodeType", "CHANNEL",
                "status", channelNodeService.getNodeStatus().toString(),
                "version", "12.0.0",
                "description", "Aurigraph V11 Channel Node - Multi-channel data flow coordination"
        )).build();
    }

    // Request/Response DTOs

    /**
     * Request object for creating a channel.
     */
    public static class CreateChannelRequest {
        public String name;
        public String description;
        public ChannelConfig config;
    }

    /**
     * Request object for adding a participant.
     */
    public static class AddParticipantRequest {
        public String participantId;
    }

    /**
     * Request object for routing a message.
     */
    public static class RouteMessageRequest {
        public String senderId;
        public String recipientId;
        public byte[] payload;
    }
}
