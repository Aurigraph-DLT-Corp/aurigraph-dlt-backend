package io.aurigraph.v11.channels;

import io.aurigraph.v11.channels.models.Message;
import io.aurigraph.v11.channels.models.Message.MessageStatus;
import io.aurigraph.v11.channels.models.Message.MessageType;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Message Repository - JPA/Panache Implementation
 *
 * Provides database persistence for Message entities.
 * Supports message queries, threading, and statistics.
 *
 * @version 3.8.0 (Phase 2 Day 11)
 * @author Aurigraph V11 Development Team
 */
@ApplicationScoped
public class MessageRepository implements PanacheRepository<Message> {

    // ==================== BASIC QUERIES ====================

    public Optional<Message> findByMessageId(String messageId) {
        return find("messageId", messageId).firstResultOptional();
    }

    public List<Message> findByChannel(String channelId) {
        return find("channelId = ?1 and isDeleted = false",
                Sort.descending("createdAt"), channelId)
                .list();
    }

    public List<Message> findByChannel(String channelId, int limit) {
        return find("channelId = ?1 and isDeleted = false",
                Sort.descending("createdAt"), channelId)
                .page(Page.ofSize(limit))
                .list();
    }

    public List<Message> findBySender(String senderAddress) {
        return find("senderAddress = ?1 and isDeleted = false",
                Sort.descending("createdAt"), senderAddress)
                .list();
    }

    // ==================== PAGINATION QUERIES ====================

    public List<Message> findByChannelPaginated(String channelId, int page, int size) {
        return find("channelId = ?1 and isDeleted = false",
                Sort.descending("createdAt"), channelId)
                .page(page, size)
                .list();
    }

    public List<Message> findBeforeMessage(String channelId, String messageId, int limit) {
        Optional<Message> message = findByMessageId(messageId);
        if (message.isEmpty()) {
            return List.of();
        }

        return find("channelId = ?1 and createdAt < ?2 and isDeleted = false",
                Sort.descending("createdAt"),
                channelId,
                message.get().getCreatedAt())
                .page(Page.ofSize(limit))
                .list();
    }

    public List<Message> findAfterMessage(String channelId, String messageId, int limit) {
        Optional<Message> message = findByMessageId(messageId);
        if (message.isEmpty()) {
            return List.of();
        }

        return find("channelId = ?1 and createdAt > ?2 and isDeleted = false",
                Sort.ascending("createdAt"),
                channelId,
                message.get().getCreatedAt())
                .page(Page.ofSize(limit))
                .list();
    }

    // ==================== THREADING QUERIES ====================

    public List<Message> findByThread(String threadId) {
        return find("threadId = ?1 and isDeleted = false",
                Sort.ascending("createdAt"), threadId)
                .list();
    }

    public List<Message> findReplies(String replyToMessageId) {
        return find("replyToMessageId = ?1 and isDeleted = false",
                Sort.ascending("createdAt"), replyToMessageId)
                .list();
    }

    public long countReplies(String messageId) {
        return count("replyToMessageId = ?1 and isDeleted = false", messageId);
    }

    // ==================== TYPE QUERIES ====================

    public List<Message> findByType(MessageType messageType) {
        return find("messageType = ?1 and isDeleted = false",
                Sort.descending("createdAt"), messageType)
                .list();
    }

    public List<Message> findByChannelAndType(String channelId, MessageType messageType) {
        return find("channelId = ?1 and messageType = ?2 and isDeleted = false",
                Sort.descending("createdAt"),
                channelId,
                messageType)
                .list();
    }

    // ==================== STATUS QUERIES ====================

    public List<Message> findByStatus(MessageStatus status) {
        return find("status = ?1 and isDeleted = false",
                Sort.descending("createdAt"), status)
                .list();
    }

    public List<Message> findUnreadByChannel(String channelId) {
        return find("channelId = ?1 and status = ?2 and isDeleted = false",
                Sort.descending("createdAt"),
                channelId,
                MessageStatus.SENT)
                .list();
    }

    // ==================== TIME-BASED QUERIES ====================

    public List<Message> findByChannelAfter(String channelId, Instant after) {
        return find("channelId = ?1 and createdAt > ?2 and isDeleted = false",
                Sort.ascending("createdAt"),
                channelId,
                after)
                .list();
    }

    public List<Message> findByChannelBetween(String channelId, Instant start, Instant end) {
        return find("channelId = ?1 and createdAt >= ?2 and createdAt <= ?3 and isDeleted = false",
                Sort.ascending("createdAt"),
                channelId,
                start,
                end)
                .list();
    }

    public List<Message> findRecentMessages(long secondsAgo, int limit) {
        Instant since = Instant.now().minusSeconds(secondsAgo);
        return find("createdAt > ?1 and isDeleted = false",
                Sort.descending("createdAt"), since)
                .page(Page.ofSize(limit))
                .list();
    }

    // ==================== EDITED/DELETED QUERIES ====================

    public List<Message> findEditedMessages(String channelId) {
        return find("channelId = ?1 and isEdited = true and isDeleted = false",
                Sort.descending("editedAt"), channelId)
                .list();
    }

    public List<Message> findDeletedMessages(String channelId) {
        return find("channelId = ?1 and isDeleted = true",
                Sort.descending("deletedAt"), channelId)
                .list();
    }

    // ==================== ATTACHMENT QUERIES ====================

    public List<Message> findMessagesWithAttachments(String channelId) {
        return find("channelId = ?1 and hasAttachments = true and isDeleted = false",
                Sort.descending("createdAt"), channelId)
                .list();
    }

    public long countAttachmentsByChannel(String channelId) {
        Long count = find("select sum(attachmentCount) from Message where channelId = ?1 and isDeleted = false", channelId)
                .project(Long.class)
                .firstResult();
        return count != null ? count : 0L;
    }

    // ==================== REACTION QUERIES ====================

    public List<Message> findMessagesWithReactions(String channelId) {
        return find("channelId = ?1 and reactionCount > 0 and isDeleted = false",
                Sort.descending("reactionCount"), channelId)
                .list();
    }

    public List<Message> findMostReacted(String channelId, int limit) {
        return find("channelId = ?1 and reactionCount > 0 and isDeleted = false",
                Sort.descending("reactionCount"), channelId)
                .page(Page.ofSize(limit))
                .list();
    }

    // ==================== SEARCH QUERIES ====================

    public List<Message> searchByContent(String channelId, String contentPattern) {
        return find("channelId = ?1 and lower(content) like lower(?2) and isDeleted = false",
                Sort.descending("createdAt"),
                channelId,
                "%" + contentPattern + "%")
                .list();
    }

    public List<Message> searchBySender(String channelId, String senderAddress) {
        return find("channelId = ?1 and senderAddress = ?2 and isDeleted = false",
                Sort.descending("createdAt"),
                channelId,
                senderAddress)
                .list();
    }

    // ==================== STATISTICS ====================

    public long countByChannel(String channelId) {
        return count("channelId = ?1 and isDeleted = false", channelId);
    }

    public long countBySender(String senderAddress) {
        return count("senderAddress = ?1 and isDeleted = false", senderAddress);
    }

    public MessageStatistics getChannelStatistics(String channelId) {
        long total = countByChannel(channelId);
        long edited = count("channelId = ?1 and isEdited = true and isDeleted = false", channelId);
        long deleted = count("channelId = ?1 and isDeleted = true", channelId);
        long withAttachments = count("channelId = ?1 and hasAttachments = true and isDeleted = false", channelId);

        Long totalAttachments = find("select sum(attachmentCount) from Message where channelId = ?1 and isDeleted = false", channelId)
                .project(Long.class)
                .firstResult();

        Long totalReactions = find("select sum(reactionCount) from Message where channelId = ?1 and isDeleted = false", channelId)
                .project(Long.class)
                .firstResult();

        return new MessageStatistics(
                total,
                edited,
                deleted,
                withAttachments,
                totalAttachments != null ? totalAttachments : 0L,
                totalReactions != null ? totalReactions : 0L
        );
    }

    // ==================== CLEANUP ====================

    public long deleteBefore(Instant before) {
        return delete("createdAt < ?1", before);
    }

    public long deleteByChannel(String channelId) {
        return delete("channelId", channelId);
    }

    // ==================== DATA MODELS ====================

    public record MessageStatistics(
            long totalMessages,
            long editedMessages,
            long deletedMessages,
            long messagesWithAttachments,
            long totalAttachments,
            long totalReactions
    ) {}
}
