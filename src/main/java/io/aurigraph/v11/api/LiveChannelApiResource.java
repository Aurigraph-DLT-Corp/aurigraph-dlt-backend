package io.aurigraph.v11.api;

import io.aurigraph.v11.channels.LiveChannelDataService;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;

import java.util.*;

/**
 * Live Channel API Resource
 *
 * Provides real-time, live data for channels and their participant nodes.
 * This replaces dummy data in the frontend with actual backend-provided live data.
 *
 * Endpoints:
 * - GET /api/v11/live/channels - Get all channels with live metrics
 * - GET /api/v11/live/channels/{id} - Get specific channel with live data
 * - GET /api/v11/live/channels/{id}/participants - Get channel participants/nodes
 * - GET /api/v11/live/channels/stats - Get overall statistics
 *
 * @version 11.0.0
 * @author Aurigraph V11 Development Team
 */
@Path("/api/v11/live/channels")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Live Channel Data", description = "Real-time channel and participant node data")
public class LiveChannelApiResource {

    private static final Logger LOG = Logger.getLogger(LiveChannelApiResource.class);

    @Inject
    LiveChannelDataService liveChannelDataService;

    /**
     * Get all channels with live metrics
     * GET /api/v11/live/channels
     */
    @GET
    @Operation(summary = "Get all channels", description = "Retrieve all channels with real-time metrics")
    public Uni<Response> getAllChannels() {
        return Uni.createFrom().item(() -> {
            LOG.debug("Getting all channels with live data");

            List<LiveChannelDataService.ChannelData> channels = liveChannelDataService.getAllChannels();

            Map<String, Object> response = new HashMap<>();
            response.put("channels", channels);
            response.put("totalChannels", channels.size());
            response.put("timestamp", java.time.Instant.now().toString());

            return Response.ok(response).build();
        });
    }

    /**
     * Get specific channel with live data
     * GET /api/v11/live/channels/{id}
     */
    @GET
    @Path("/{id}")
    @Operation(summary = "Get channel", description = "Get specific channel with live metrics and participant count")
    public Uni<Response> getChannel(@PathParam("id") String channelId) {
        return Uni.createFrom().item(() -> {
            LOG.debugf("Getting channel: %s", channelId);

            LiveChannelDataService.ChannelData channel = liveChannelDataService.getChannel(channelId);

            if (channel == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(Map.of("error", "Channel not found: " + channelId))
                        .build();
            }

            // Enrich with participant count
            List<LiveChannelDataService.ParticipantNode> participants =
                    liveChannelDataService.getChannelParticipants(channelId);

            Map<String, Object> response = new HashMap<>();
            response.put("channel", channel);
            response.put("participantCount", participants.size());
            response.put("onlineParticipants", participants.stream()
                    .filter(p -> "online".equals(p.status))
                    .count());
            response.put("timestamp", java.time.Instant.now().toString());

            return Response.ok(response).build();
        });
    }

    /**
     * Get channel participants/nodes
     * GET /api/v11/live/channels/{id}/participants
     */
    @GET
    @Path("/{id}/participants")
    @Operation(summary = "Get channel participants", description = "Get all participant nodes for a channel with live status")
    public Uni<Response> getChannelParticipants(@PathParam("id") String channelId) {
        return Uni.createFrom().item(() -> {
            LOG.debugf("Getting participants for channel: %s", channelId);

            LiveChannelDataService.ChannelData channel = liveChannelDataService.getChannel(channelId);

            if (channel == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(Map.of("error", "Channel not found: " + channelId))
                        .build();
            }

            List<LiveChannelDataService.ParticipantNode> participants =
                    liveChannelDataService.getChannelParticipants(channelId);

            Map<String, Object> response = new HashMap<>();
            response.put("channelId", channelId);
            response.put("channelName", channel.name);
            response.put("participants", participants);
            response.put("totalParticipants", participants.size());
            response.put("onlineParticipants", participants.stream()
                    .filter(p -> "online".equals(p.status))
                    .count());
            response.put("offlineParticipants", participants.stream()
                    .filter(p -> "offline".equals(p.status))
                    .count());

            // Group by role
            Map<String, Long> roleDistribution = new HashMap<>();
            participants.forEach(p -> {
                roleDistribution.merge(p.role, 1L, Long::sum);
            });
            response.put("roleDistribution", roleDistribution);

            response.put("timestamp", java.time.Instant.now().toString());

            return Response.ok(response).build();
        });
    }

    /**
     * Get channel with participants in one call
     * GET /api/v11/live/channels/{id}/full
     */
    @GET
    @Path("/{id}/full")
    @Operation(summary = "Get channel with participants", description = "Get complete channel data including all participants in a single call")
    public Uni<Response> getChannelWithParticipants(@PathParam("id") String channelId) {
        return Uni.createFrom().item(() -> {
            LOG.debugf("Getting full channel data: %s", channelId);

            Map<String, Object> result = liveChannelDataService.getChannelWithParticipants(channelId);

            if (result == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(Map.of("error", "Channel not found: " + channelId))
                        .build();
            }

            return Response.ok(result).build();
        });
    }

    /**
     * Get overall channel statistics
     * GET /api/v11/live/channels/stats
     */
    @GET
    @Path("/stats")
    @Operation(summary = "Get channel statistics", description = "Get overall statistics for all channels")
    public Uni<Response> getStatistics() {
        return Uni.createFrom().item(() -> {
            LOG.debug("Getting channel statistics");

            Map<String, Object> stats = liveChannelDataService.getStatistics();

            return Response.ok(stats).build();
        });
    }

    /**
     * Get all channels with their participants
     * GET /api/v11/live/channels/all-with-participants
     */
    @GET
    @Path("/all-with-participants")
    @Operation(summary = "Get all channels with participants", description = "Get all channels with their participant nodes in a single call")
    public Uni<Response> getAllChannelsWithParticipants() {
        return Uni.createFrom().item(() -> {
            LOG.debug("Getting all channels with participants");

            List<LiveChannelDataService.ChannelData> channels = liveChannelDataService.getAllChannels();
            List<Map<String, Object>> enrichedChannels = new ArrayList<>();

            for (LiveChannelDataService.ChannelData channel : channels) {
                List<LiveChannelDataService.ParticipantNode> participants =
                        liveChannelDataService.getChannelParticipants(channel.channelId);

                Map<String, Object> enrichedChannel = new HashMap<>();
                enrichedChannel.put("id", channel.channelId);
                enrichedChannel.put("name", channel.name);
                enrichedChannel.put("type", channel.type);
                enrichedChannel.put("status", channel.status);
                enrichedChannel.put("config", Map.of(
                        "consensusType", channel.consensusType,
                        "targetTps", channel.targetTps,
                        "blockSize", 10000,
                        "blockTimeout", 2
                ));
                enrichedChannel.put("metrics", Map.of(
                        "tps", channel.currentTps,
                        "totalTransactions", channel.totalTransactions,
                        "blockHeight", channel.blockHeight,
                        "latency", channel.latencyMs,
                        "throughput", channel.throughput,
                        "nodeCount", channel.nodeCount,
                        "activeContracts", channel.activeContracts,
                        "storageUsed", channel.storageUsed
                ));
                enrichedChannel.put("participants", participants);
                enrichedChannel.put("smartContracts", List.of()); // Empty for now
                enrichedChannel.put("createdAt", channel.createdAt.toString());
                enrichedChannel.put("updatedAt", channel.lastUpdated.toString());

                enrichedChannels.add(enrichedChannel);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("channels", enrichedChannels);
            response.put("totalChannels", enrichedChannels.size());
            response.put("timestamp", java.time.Instant.now().toString());

            return Response.ok(response).build();
        });
    }
}
