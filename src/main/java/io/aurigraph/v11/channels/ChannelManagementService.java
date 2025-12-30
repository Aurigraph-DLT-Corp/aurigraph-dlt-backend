package io.aurigraph.v11.channels;

import io.aurigraph.v11.channels.models.Channel;
import io.aurigraph.v11.channels.models.Channel.ChannelStatus;
import io.aurigraph.v11.channels.models.Channel.ChannelType;
import io.aurigraph.v11.channels.models.ChannelMember;
import io.aurigraph.v11.channels.models.ChannelMember.MemberRole;
import io.aurigraph.v11.channels.models.ChannelMember.MemberStatus;
import io.aurigraph.v11.channels.models.Message;
import io.aurigraph.v11.channels.models.Message.MessageStatus;
import io.aurigraph.v11.channels.models.Message.MessageType;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Channel Management Service for Aurigraph V11
 *
 * Comprehensive service for managing communication channels:
 * - Channel creation and lifecycle
 * - Real-time messaging
 * - Member management and permissions
 * - Message history and search
 * - Channel metrics and analytics
 *
 * @version 3.8.0 (Phase 2 Day 11)
 * @author Aurigraph V11 Development Team
 */
@ApplicationScoped
public class ChannelManagementService {

    private static final Logger LOG = Logger.getLogger(ChannelManagementService.class);

    @Inject
    ChannelRepository channelRepository;

    @Inject
    MessageRepository messageRepository;

    @Inject
    ChannelMemberRepository memberRepository;

    // Performance metrics
    private final AtomicLong channelsCreated = new AtomicLong(0);
    private final AtomicLong messagesSent = new AtomicLong(0);
    private final AtomicLong membersAdded = new AtomicLong(0);
    private final AtomicLong channelsClosed = new AtomicLong(0);

    // Virtual thread executor for high concurrency
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    // ==================== CHANNEL OPERATIONS ====================

    /**
     * Create a new channel
     */
    @Transactional
    public Uni<Channel> createChannel(ChannelCreationRequest request) {
        return Uni.createFrom().item(() -> {
            LOG.infof("Creating channel: %s", request.name());

            Channel channel = new Channel();
            channel.setChannelId(generateChannelId());
            channel.setName(request.name());
            channel.setChannelType(request.channelType());
            channel.setOwnerAddress(request.ownerAddress());
            channel.setDescription(request.description());
            channel.setTopic(request.topic());
            channel.setIsPublic(request.isPublic() != null ? request.isPublic() : false);
            channel.setIsEncrypted(request.isEncrypted() != null ? request.isEncrypted() : false);
            channel.setMaxMembers(request.maxMembers() != null ? request.maxMembers() : 100);
            channel.setAllowGuestAccess(request.allowGuestAccess() != null ? request.allowGuestAccess() : false);
            channel.setMetadata(request.metadata());

            channelRepository.persist(channel);

            // Add owner as first member
            ChannelMember owner = new ChannelMember(channel.getChannelId(), request.ownerAddress(), MemberRole.OWNER);
            memberRepository.persist(owner);

            // Update member count
            channel.updateMemberCount(1);
            channel.updateActiveMembers(1);
            channelRepository.persist(channel);

            channelsCreated.incrementAndGet();
            membersAdded.incrementAndGet();

            LOG.infof("Channel created: %s", channel.getChannelId());
            return channel;
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    /**
     * Get channel by ID
     */
    public Uni<Channel> getChannel(String channelId) {
        return Uni.createFrom().item(() ->
                channelRepository.findByChannelId(channelId)
                        .orElseThrow(() -> new IllegalArgumentException("Channel not found: " + channelId))
        ).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    /**
     * Close a channel
     */
    @Transactional
    public Uni<ChannelOperationResult> closeChannel(String channelId, String closedBy) {
        return Uni.createFrom().item(() -> {
            LOG.infof("Closing channel: %s", channelId);

            Channel channel = channelRepository.findByChannelId(channelId)
                    .orElseThrow(() -> new IllegalArgumentException("Channel not found: " + channelId));

            // Verify permission
            verifyPermission(channelId, closedBy, "manage");

            channel.close();
            channelRepository.persist(channel);
            channelsClosed.incrementAndGet();

            LOG.infof("Channel closed: %s", channelId);
            return new ChannelOperationResult(
                    channelId,
                    "CLOSED",
                    "Channel closed successfully",
                    Instant.now()
            );
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    /**
     * List channels with pagination
     */
    public Uni<List<Channel>> listChannels(int page, int size) {
        return Uni.createFrom().item(() -> {
            return channelRepository.findAll()
                    .page(page, size)
                    .list();
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    /**
     * List public channels
     */
    public Uni<List<Channel>> listPublicChannels() {
        return Uni.createFrom().item(() -> {
            return channelRepository.findPublicChannels();
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    // ==================== MESSAGE OPERATIONS ====================

    /**
     * Send a message to a channel
     */
    @Transactional
    public Uni<Message> sendMessage(MessageSendRequest request) {
        return Uni.createFrom().item(() -> {
            LOG.infof("Sending message to channel %s from %s", request.channelId(), request.senderAddress());

            // Verify channel exists and is active
            Channel channel = channelRepository.findByChannelId(request.channelId())
                    .orElseThrow(() -> new IllegalArgumentException("Channel not found: " + request.channelId()));

            if (!channel.isActive()) {
                throw new IllegalStateException("Channel is not active");
            }

            // Verify sender is member and has permission
            verifyPermission(request.channelId(), request.senderAddress(), "post");

            // Create message
            Message message = new Message();
            message.setMessageId(generateMessageId());
            message.setChannelId(request.channelId());
            message.setSenderAddress(request.senderAddress());
            message.setContent(request.content());
            message.setMessageType(request.messageType() != null ? request.messageType() : MessageType.TEXT);
            message.setMetadata(request.metadata());
            message.setThreadId(request.threadId());
            message.setReplyToMessageId(request.replyToMessageId());

            // Handle attachments
            if (request.attachmentUrls() != null && !request.attachmentUrls().isEmpty()) {
                message.setHasAttachments(true);
                message.setAttachmentUrls(String.join(",", request.attachmentUrls()));
                message.setAttachmentCount(request.attachmentUrls().size());
            }

            messageRepository.persist(message);

            // Update channel statistics
            channel.recordMessage();
            channelRepository.persist(channel);

            // Update unread counts for other members
            List<ChannelMember> otherMembers = memberRepository.findActiveByChannel(request.channelId())
                    .stream()
                    .filter(m -> !m.getMemberAddress().equals(request.senderAddress()))
                    .toList();

            otherMembers.forEach(member -> {
                member.incrementUnreadCount();
                memberRepository.persist(member);
            });

            messagesSent.incrementAndGet();

            LOG.infof("Message sent: %s", message.getMessageId());
            return message;
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    /**
     * Get messages from a channel
     */
    public Uni<List<Message>> getMessages(String channelId, int limit) {
        return Uni.createFrom().item(() -> {
            return messageRepository.findByChannel(channelId, limit);
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    /**
     * Get messages with pagination
     */
    public Uni<List<Message>> getMessagesPaginated(String channelId, int page, int size) {
        return Uni.createFrom().item(() -> {
            return messageRepository.findByChannelPaginated(channelId, page, size);
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    /**
     * Delete a message
     */
    @Transactional
    public Uni<MessageOperationResult> deleteMessage(String messageId, String deletedBy) {
        return Uni.createFrom().item(() -> {
            LOG.infof("Deleting message: %s", messageId);

            Message message = messageRepository.findByMessageId(messageId)
                    .orElseThrow(() -> new IllegalArgumentException("Message not found: " + messageId));

            // Verify permission (owner or admin/mod)
            if (!message.getSenderAddress().equals(deletedBy)) {
                verifyPermission(message.getChannelId(), deletedBy, "manage");
            }

            message.delete();
            messageRepository.persist(message);

            LOG.infof("Message deleted: %s", messageId);
            return new MessageOperationResult(
                    messageId,
                    "DELETED",
                    "Message deleted successfully",
                    Instant.now()
            );
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    // ==================== MEMBER OPERATIONS ====================

    /**
     * Join a channel
     */
    @Transactional
    public Uni<ChannelMember> joinChannel(String channelId, String memberAddress) {
        return Uni.createFrom().item(() -> {
            LOG.infof("Member %s joining channel %s", memberAddress, channelId);

            Channel channel = channelRepository.findByChannelId(channelId)
                    .orElseThrow(() -> new IllegalArgumentException("Channel not found: " + channelId));

            // Check if channel is full
            if (channel.isFull()) {
                throw new IllegalStateException("Channel is full");
            }

            // Check if channel is public or requires invite
            if (!channel.getIsPublic() && !channel.getAllowGuestAccess()) {
                throw new IllegalStateException("Channel is private");
            }

            // Check if already a member
            Optional<ChannelMember> existing = memberRepository.findByChannelAndMember(channelId, memberAddress);
            if (existing.isPresent()) {
                if (existing.get().getStatus() == MemberStatus.ACTIVE) {
                    throw new IllegalStateException("Already a member");
                }
                // Rejoin if previously left
                ChannelMember member = existing.get();
                member.setStatus(MemberStatus.ACTIVE);
                member.setJoinedAt(Instant.now());
                memberRepository.persist(member);
                return member;
            }

            // Create new membership
            ChannelMember member = new ChannelMember(channelId, memberAddress, MemberRole.MEMBER);
            memberRepository.persist(member);

            // Update channel statistics
            long memberCount = memberRepository.countActiveByChannel(channelId);
            channel.updateMemberCount((int) memberCount);
            channelRepository.persist(channel);

            membersAdded.incrementAndGet();

            LOG.infof("Member joined: %s", memberAddress);
            return member;
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    /**
     * Leave a channel
     */
    @Transactional
    public Uni<MemberOperationResult> leaveChannel(String channelId, String memberAddress) {
        return Uni.createFrom().item(() -> {
            LOG.infof("Member %s leaving channel %s", memberAddress, channelId);

            ChannelMember member = memberRepository.findByChannelAndMember(channelId, memberAddress)
                    .orElseThrow(() -> new IllegalArgumentException("Not a member of this channel"));

            // Don't allow owner to leave
            if (member.getRole() == MemberRole.OWNER) {
                throw new IllegalStateException("Channel owner cannot leave");
            }

            member.leave();
            memberRepository.persist(member);

            // Update channel statistics
            long memberCount = memberRepository.countActiveByChannel(channelId);
            Channel channel = channelRepository.findByChannelId(channelId)
                    .orElseThrow(() -> new IllegalArgumentException("Channel not found"));
            channel.updateMemberCount((int) memberCount);
            channelRepository.persist(channel);

            LOG.infof("Member left: %s", memberAddress);
            return new MemberOperationResult(
                    channelId,
                    memberAddress,
                    "LEFT",
                    "Left channel successfully",
                    Instant.now()
            );
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    /**
     * Get channel members
     */
    public Uni<List<ChannelMember>> getMembers(String channelId) {
        return Uni.createFrom().item(() -> {
            return memberRepository.findActiveByChannel(channelId);
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    /**
     * Promote member
     */
    @Transactional
    public Uni<MemberOperationResult> promoteMember(String channelId, String memberAddress, String promotedBy) {
        return Uni.createFrom().item(() -> {
            LOG.infof("Promoting member %s in channel %s", memberAddress, channelId);

            // Verify permission
            verifyPermission(channelId, promotedBy, "manage");

            ChannelMember member = memberRepository.findByChannelAndMember(channelId, memberAddress)
                    .orElseThrow(() -> new IllegalArgumentException("Not a member of this channel"));

            member.promote();
            memberRepository.persist(member);

            LOG.infof("Member promoted: %s to %s", memberAddress, member.getRole());
            return new MemberOperationResult(
                    channelId,
                    memberAddress,
                    "PROMOTED",
                    "Promoted to " + member.getRole(),
                    Instant.now()
            );
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    // ==================== STATISTICS ====================

    /**
     * Get channel statistics
     */
    public Uni<Map<String, Object>> getChannelStatistics(String channelId) {
        return Uni.createFrom().item(() -> {
            Map<String, Object> stats = new HashMap<>();

            Channel channel = channelRepository.findByChannelId(channelId)
                    .orElseThrow(() -> new IllegalArgumentException("Channel not found: " + channelId));

            stats.put("channelId", channelId);
            stats.put("name", channel.getName());
            stats.put("type", channel.getChannelType());
            stats.put("status", channel.getStatus());
            stats.put("memberCount", channel.getMemberCount());
            stats.put("messageCount", channel.getMessageCount());
            stats.put("activeMembers", channel.getActiveMembers());
            stats.put("createdAt", channel.getCreatedAt());
            stats.put("lastMessageAt", channel.getLastMessageAt());

            // Member statistics
            ChannelMemberRepository.MemberStatistics memberStats =
                    memberRepository.getChannelStatistics(channelId);
            stats.put("memberStatistics", Map.of(
                    "totalMembers", memberStats.totalMembers(),
                    "owners", memberStats.owners(),
                    "admins", memberStats.admins(),
                    "moderators", memberStats.moderators(),
                    "regularMembers", memberStats.regularMembers(),
                    "activeMembers", memberStats.activeMembers(),
                    "totalUnread", memberStats.totalUnread()
            ));

            // Message statistics
            MessageRepository.MessageStatistics messageStats =
                    messageRepository.getChannelStatistics(channelId);
            stats.put("messageStatistics", Map.of(
                    "totalMessages", messageStats.totalMessages(),
                    "editedMessages", messageStats.editedMessages(),
                    "deletedMessages", messageStats.deletedMessages(),
                    "messagesWithAttachments", messageStats.messagesWithAttachments(),
                    "totalAttachments", messageStats.totalAttachments(),
                    "totalReactions", messageStats.totalReactions()
            ));

            return stats;
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    /**
     * Get service statistics
     */
    public Uni<Map<String, Object>> getStatistics() {
        return Uni.createFrom().item(() -> {
            Map<String, Object> stats = new HashMap<>();

            stats.put("channelsCreated", channelsCreated.get());
            stats.put("messagesSent", messagesSent.get());
            stats.put("membersAdded", membersAdded.get());
            stats.put("channelsClosed", channelsClosed.get());

            ChannelRepository.ChannelStatistics channelStats = channelRepository.getStatistics();
            stats.put("channelStatistics", Map.of(
                    "totalChannels", channelStats.totalChannels(),
                    "activeChannels", channelStats.activeChannels(),
                    "archivedChannels", channelStats.archivedChannels(),
                    "closedChannels", channelStats.closedChannels(),
                    "publicChannels", channelStats.publicChannels(),
                    "privateChannels", channelStats.privateChannels(),
                    "encryptedChannels", channelStats.encryptedChannels(),
                    "totalMembers", channelStats.totalMembers(),
                    "totalMessages", channelStats.totalMessages()
            ));

            stats.put("timestamp", Instant.now());

            return stats;
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    // ==================== HELPER METHODS ====================

    private String generateChannelId() {
        return "CH_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8);
    }

    private String generateMessageId() {
        return "MSG_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8);
    }

    private void verifyPermission(String channelId, String memberAddress, String permission) {
        if (!memberRepository.hasPermission(channelId, memberAddress, permission)) {
            throw new IllegalStateException("Insufficient permissions: " + permission);
        }
    }

    // ==================== DATA MODELS ====================

    public record ChannelCreationRequest(
            String name,
            ChannelType channelType,
            String ownerAddress,
            String description,
            String topic,
            Boolean isPublic,
            Boolean isEncrypted,
            Integer maxMembers,
            Boolean allowGuestAccess,
            String metadata
    ) {}

    public record MessageSendRequest(
            String channelId,
            String senderAddress,
            String content,
            MessageType messageType,
            String threadId,
            String replyToMessageId,
            List<String> attachmentUrls,
            String metadata
    ) {}

    public record ChannelOperationResult(
            String channelId,
            String operation,
            String message,
            Instant timestamp
    ) {}

    public record MessageOperationResult(
            String messageId,
            String operation,
            String message,
            Instant timestamp
    ) {}

    public record MemberOperationResult(
            String channelId,
            String memberAddress,
            String operation,
            String message,
            Instant timestamp
    ) {}
}
