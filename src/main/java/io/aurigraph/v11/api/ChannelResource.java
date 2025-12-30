package io.aurigraph.v11.api;

import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.util.*;

/**
 * Channel Management API Resource
 *
 * REST API for real-time communication channels:
 * - Channel creation and management
 * - Real-time messaging
 * - Member management
 * - Channel metrics and analytics
 *
 * @version 11.0.0 (Priority #3 - Backend Development Agent)
 * @author Aurigraph V11 Development Team
 */
@Path("/api/v11/channels")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Channel Management", description = "Real-time communication channel operations")
public class ChannelResource {

    private static final Logger LOG = Logger.getLogger(ChannelResource.class);

    // In-memory storage for demo (will be replaced with ChannelManagementService + LevelDB)
    private static final Map<String, Map<String, Object>> CHANNELS = new java.util.concurrent.ConcurrentHashMap<>();
    private static final Map<String, List<Map<String, Object>>> CHANNEL_MESSAGES = new java.util.concurrent.ConcurrentHashMap<>();
    private static final Map<String, List<Map<String, Object>>> CHANNEL_MEMBERS = new java.util.concurrent.ConcurrentHashMap<>();

    // ==================== CHANNEL OPERATIONS ====================

    /**
     * List all channels with pagination
     * GET /api/v11/channels
     */
    @GET
    @Operation(summary = "List channels", description = "Retrieve list of channels with pagination")
    public Uni<Response> listChannels(
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("20") int size,
            @QueryParam("type") String channelType,
            @QueryParam("status") String status
    ) {
        return Uni.createFrom().item(() -> {
            LOG.infof("Listing channels: page=%d, size=%d, type=%s, status=%s",
                    page, size, channelType, status);

            List<Map<String, Object>> channels = new ArrayList<>(CHANNELS.values());

            // Apply filters
            if (channelType != null) {
                channels = channels.stream()
                        .filter(c -> channelType.equals(c.get("channelType")))
                        .toList();
            }
            if (status != null) {
                channels = channels.stream()
                        .filter(c -> status.equals(c.get("status")))
                        .toList();
            }

            // Pagination
            int start = page * size;
            int end = Math.min(start + size, channels.size());
            List<Map<String, Object>> paginatedChannels = start < channels.size() ?
                    channels.subList(start, end) : List.of();

            Map<String, Object> response = new HashMap<>();
            response.put("channels", paginatedChannels);
            response.put("page", page);
            response.put("size", size);
            response.put("totalChannels", channels.size());
            response.put("totalPages", (channels.size() + size - 1) / size);

            return Response.ok(response).build();
        });
    }

    /**
     * Get channel details
     * GET /api/v11/channels/{id}
     */
    @GET
    @Path("/{id}")
    @Operation(summary = "Get channel", description = "Get detailed information about a specific channel")
    public Uni<Response> getChannel(@PathParam("id") String channelId) {
        return Uni.createFrom().item(() -> {
            LOG.infof("Getting channel: %s", channelId);

            Map<String, Object> channel = CHANNELS.get(channelId);
            if (channel == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(Map.of("error", "Channel not found: " + channelId))
                        .build();
            }

            // Add member and message counts
            Map<String, Object> enrichedChannel = new HashMap<>(channel);
            enrichedChannel.put("memberCount", CHANNEL_MEMBERS.getOrDefault(channelId, List.of()).size());
            enrichedChannel.put("messageCount", CHANNEL_MESSAGES.getOrDefault(channelId, List.of()).size());

            return Response.ok(enrichedChannel).build();
        });
    }

    /**
     * Create new channel
     * POST /api/v11/channels
     */
    @POST
    @Operation(summary = "Create channel", description = "Create a new communication channel")
    public Uni<Response> createChannel(ChannelCreateRequest request) {
        return Uni.createFrom().item(() -> {
            LOG.infof("Creating channel: %s", request.name);

            String channelId = "CH_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8);

            Map<String, Object> channel = new HashMap<>();
            channel.put("channelId", channelId);
            channel.put("name", request.name);
            channel.put("channelType", request.channelType != null ? request.channelType : "GROUP");
            channel.put("ownerAddress", request.ownerAddress);
            channel.put("description", request.description);
            channel.put("topic", request.topic);
            channel.put("isPublic", request.isPublic != null ? request.isPublic : false);
            channel.put("isEncrypted", request.isEncrypted != null ? request.isEncrypted : false);
            channel.put("maxMembers", request.maxMembers != null ? request.maxMembers : 100);
            channel.put("status", "ACTIVE");
            channel.put("createdAt", Instant.now().toString());
            channel.put("memberCount", 1);
            channel.put("messageCount", 0);

            CHANNELS.put(channelId, channel);

            // Add owner as first member
            List<Map<String, Object>> members = new ArrayList<>();
            Map<String, Object> ownerMember = new HashMap<>();
            ownerMember.put("channelId", channelId);
            ownerMember.put("memberAddress", request.ownerAddress);
            ownerMember.put("role", "OWNER");
            ownerMember.put("status", "ACTIVE");
            ownerMember.put("joinedAt", Instant.now().toString());
            members.add(ownerMember);
            CHANNEL_MEMBERS.put(channelId, members);

            // Initialize empty message list
            CHANNEL_MESSAGES.put(channelId, new ArrayList<>());

            return Response.status(Response.Status.CREATED).entity(channel).build();
        });
    }

    /**
     * Close a channel
     * DELETE /api/v11/channels/{id}
     */
    @DELETE
    @Path("/{id}")
    @Operation(summary = "Close channel", description = "Close or archive a channel")
    public Uni<Response> closeChannel(
            @PathParam("id") String channelId,
            @QueryParam("closedBy") String closedBy
    ) {
        return Uni.createFrom().item(() -> {
            LOG.infof("Closing channel: %s by %s", channelId, closedBy);

            Map<String, Object> channel = CHANNELS.get(channelId);
            if (channel == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(Map.of("error", "Channel not found: " + channelId))
                        .build();
            }

            channel.put("status", "CLOSED");
            channel.put("closedAt", Instant.now().toString());
            channel.put("closedBy", closedBy);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("channelId", channelId);
            result.put("operation", "CLOSED");
            result.put("message", "Channel closed successfully");
            result.put("timestamp", Instant.now().toString());

            return Response.ok(result).build();
        });
    }

    // ==================== MESSAGING OPERATIONS ====================

    /**
     * Send message to channel
     * POST /api/v11/channels/{id}/messages
     */
    @POST
    @Path("/{id}/messages")
    @Operation(summary = "Send message", description = "Send a message to a channel")
    public Uni<Response> sendMessage(
            @PathParam("id") String channelId,
            MessageSendRequest request
    ) {
        return Uni.createFrom().item(() -> {
            LOG.infof("Sending message to channel %s from %s", channelId, request.senderAddress);

            Map<String, Object> channel = CHANNELS.get(channelId);
            if (channel == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(Map.of("error", "Channel not found: " + channelId))
                        .build();
            }

            String messageId = "MSG_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8);

            Map<String, Object> message = new HashMap<>();
            message.put("messageId", messageId);
            message.put("channelId", channelId);
            message.put("senderAddress", request.senderAddress);
            message.put("content", request.content);
            message.put("messageType", request.messageType != null ? request.messageType : "TEXT");
            message.put("status", "SENT");
            message.put("createdAt", Instant.now().toString());

            List<Map<String, Object>> messages = CHANNEL_MESSAGES.get(channelId);
            messages.add(message);

            // Update channel message count
            channel.put("messageCount", messages.size());
            channel.put("lastMessageAt", Instant.now().toString());

            return Response.status(Response.Status.CREATED).entity(message).build();
        });
    }

    /**
     * Get messages from channel
     * GET /api/v11/channels/{id}/messages
     */
    @GET
    @Path("/{id}/messages")
    @Operation(summary = "Get messages", description = "Retrieve messages from a channel")
    public Uni<Response> getMessages(
            @PathParam("id") String channelId,
            @QueryParam("limit") @DefaultValue("50") int limit,
            @QueryParam("before") String before
    ) {
        return Uni.createFrom().item(() -> {
            LOG.infof("Getting messages from channel: %s (limit=%d)", channelId, limit);

            if (!CHANNELS.containsKey(channelId)) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(Map.of("error", "Channel not found: " + channelId))
                        .build();
            }

            List<Map<String, Object>> messages = CHANNEL_MESSAGES.getOrDefault(channelId, List.of());
            List<Map<String, Object>> limitedMessages = messages.stream()
                    .limit(limit)
                    .toList();

            Map<String, Object> response = new HashMap<>();
            response.put("channelId", channelId);
            response.put("messages", limitedMessages);
            response.put("count", limitedMessages.size());
            response.put("totalMessages", messages.size());

            return Response.ok(response).build();
        });
    }

    // ==================== MEMBER OPERATIONS ====================

    /**
     * Get channel members
     * GET /api/v11/channels/{id}/members
     */
    @GET
    @Path("/{id}/members")
    @Operation(summary = "Get members", description = "Get list of channel members")
    public Uni<Response> getMembers(@PathParam("id") String channelId) {
        return Uni.createFrom().item(() -> {
            LOG.infof("Getting members for channel: %s", channelId);

            if (!CHANNELS.containsKey(channelId)) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(Map.of("error", "Channel not found: " + channelId))
                        .build();
            }

            List<Map<String, Object>> members = CHANNEL_MEMBERS.getOrDefault(channelId, List.of());

            Map<String, Object> response = new HashMap<>();
            response.put("channelId", channelId);
            response.put("members", members);
            response.put("totalMembers", members.size());
            response.put("activeMembers", members.stream()
                    .filter(m -> "ACTIVE".equals(m.get("status")))
                    .count());

            return Response.ok(response).build();
        });
    }

    /**
     * Join channel
     * POST /api/v11/channels/{id}/join
     */
    @POST
    @Path("/{id}/join")
    @Operation(summary = "Join channel", description = "Join a public channel or accept invitation")
    public Uni<Response> joinChannel(
            @PathParam("id") String channelId,
            @QueryParam("memberAddress") String memberAddress
    ) {
        return Uni.createFrom().item(() -> {
            LOG.infof("Member %s joining channel %s", memberAddress, channelId);

            Map<String, Object> channel = CHANNELS.get(channelId);
            if (channel == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(Map.of("error", "Channel not found: " + channelId))
                        .build();
            }

            List<Map<String, Object>> members = CHANNEL_MEMBERS.get(channelId);
            boolean alreadyMember = members.stream()
                    .anyMatch(m -> memberAddress.equals(m.get("memberAddress")));

            if (alreadyMember) {
                return Response.status(Response.Status.CONFLICT)
                        .entity(Map.of("error", "Already a member of this channel"))
                        .build();
            }

            Map<String, Object> member = new HashMap<>();
            member.put("channelId", channelId);
            member.put("memberAddress", memberAddress);
            member.put("role", "MEMBER");
            member.put("status", "ACTIVE");
            member.put("joinedAt", Instant.now().toString());
            members.add(member);

            // Update channel member count
            channel.put("memberCount", members.size());

            return Response.status(Response.Status.CREATED).entity(member).build();
        });
    }

    // ==================== METRICS OPERATIONS ====================

    /**
     * Get channel metrics
     * GET /api/v11/channels/{id}/metrics
     */
    @GET
    @Path("/{id}/metrics")
    @Operation(summary = "Get channel metrics", description = "Get performance metrics and statistics for a channel")
    public Uni<Response> getChannelMetrics(@PathParam("id") String channelId) {
        return Uni.createFrom().item(() -> {
            LOG.infof("Getting metrics for channel: %s", channelId);

            Map<String, Object> channel = CHANNELS.get(channelId);
            if (channel == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(Map.of("error", "Channel not found: " + channelId))
                        .build();
            }

            List<Map<String, Object>> members = CHANNEL_MEMBERS.getOrDefault(channelId, List.of());
            List<Map<String, Object>> messages = CHANNEL_MESSAGES.getOrDefault(channelId, List.of());

            Map<String, Object> metrics = new HashMap<>();
            metrics.put("channelId", channelId);
            metrics.put("name", channel.get("name"));
            metrics.put("status", channel.get("status"));
            metrics.put("totalMembers", members.size());
            metrics.put("activeMembers", members.stream()
                    .filter(m -> "ACTIVE".equals(m.get("status")))
                    .count());
            metrics.put("totalMessages", messages.size());
            metrics.put("messagesLast24h", messages.stream()
                    .filter(m -> {
                        Instant msgTime = Instant.parse((String) m.get("createdAt"));
                        return msgTime.isAfter(Instant.now().minusSeconds(86400));
                    })
                    .count());
            metrics.put("averageMessagesPerDay", messages.size());
            metrics.put("createdAt", channel.get("createdAt"));
            metrics.put("lastMessageAt", channel.get("lastMessageAt"));

            return Response.ok(metrics).build();
        });
    }

    // ==================== DATA MODELS ====================

    public static class ChannelCreateRequest {
        public String name;
        public String channelType;
        public String ownerAddress;
        public String description;
        public String topic;
        public Boolean isPublic;
        public Boolean isEncrypted;
        public Integer maxMembers;
    }

    public static class MessageSendRequest {
        public String senderAddress;
        public String content;
        public String messageType;
        public String threadId;
        public String replyToMessageId;
    }
}
