package io.aurigraph.v11.channels;

import io.aurigraph.v11.channels.models.Channel;
import io.aurigraph.v11.channels.models.Channel.ChannelStatus;
import io.aurigraph.v11.channels.models.Channel.ChannelType;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Channel Repository - JPA/Panache Implementation
 *
 * Provides database persistence for Channel entities.
 * Supports channel queries, statistics, and search.
 *
 * @version 3.8.0 (Phase 2 Day 11)
 * @author Aurigraph V11 Development Team
 */
@ApplicationScoped
public class ChannelRepository implements PanacheRepository<Channel> {

    // ==================== BASIC QUERIES ====================

    public Optional<Channel> findByChannelId(String channelId) {
        return find("channelId", channelId).firstResultOptional();
    }

    public boolean existsByChannelId(String channelId) {
        return count("channelId", channelId) > 0;
    }

    public List<Channel> findByOwner(String ownerAddress) {
        return find("ownerAddress", Sort.descending("createdAt"), ownerAddress).list();
    }

    public List<Channel> findByOwner(String ownerAddress, Page page) {
        return find("ownerAddress", Sort.descending("createdAt"), ownerAddress)
                .page(page)
                .list();
    }

    // ==================== TYPE QUERIES ====================

    public List<Channel> findByType(ChannelType channelType) {
        return find("channelType", Sort.descending("createdAt"), channelType).list();
    }

    public List<Channel> findByType(ChannelType channelType, Page page) {
        return find("channelType", Sort.descending("createdAt"), channelType)
                .page(page)
                .list();
    }

    public long countByType(ChannelType channelType) {
        return count("channelType", channelType);
    }

    // ==================== STATUS QUERIES ====================

    public List<Channel> findByStatus(ChannelStatus status) {
        return find("status", Sort.descending("createdAt"), status).list();
    }

    public List<Channel> findByStatus(ChannelStatus status, Page page) {
        return find("status", Sort.descending("createdAt"), status)
                .page(page)
                .list();
    }

    public long countByStatus(ChannelStatus status) {
        return count("status", status);
    }

    public List<Channel> findActiveChannels() {
        return find("status", Sort.descending("lastMessageAt"), ChannelStatus.ACTIVE).list();
    }

    public List<Channel> findPublicChannels() {
        return find("isPublic = true and status = ?1",
                Sort.descending("lastMessageAt"), ChannelStatus.ACTIVE)
                .list();
    }

    public List<Channel> findPrivateChannels() {
        return find("isPublic = false and status = ?1",
                Sort.descending("lastMessageAt"), ChannelStatus.ACTIVE)
                .list();
    }

    // ==================== ACTIVITY QUERIES ====================

    public List<Channel> findMostActive(int limit) {
        return find("status = ?1 and messageCount > 0",
                Sort.descending("messageCount"), ChannelStatus.ACTIVE)
                .page(Page.ofSize(limit))
                .list();
    }

    public List<Channel> findRecentlyActive(long secondsAgo) {
        Instant since = Instant.now().minusSeconds(secondsAgo);
        return find("lastMessageAt > ?1 and status = ?2",
                Sort.descending("lastMessageAt"),
                since,
                ChannelStatus.ACTIVE)
                .list();
    }

    public List<Channel> findByActivityBetween(Instant start, Instant end) {
        return find("lastMessageAt >= ?1 and lastMessageAt <= ?2",
                Sort.descending("lastMessageAt"), start, end)
                .list();
    }

    // ==================== MEMBER QUERIES ====================

    public List<Channel> findByMemberCountRange(int min, int max) {
        return find("memberCount >= ?1 and memberCount <= ?2",
                Sort.descending("memberCount"), min, max)
                .list();
    }

    public List<Channel> findMostPopular(int limit) {
        return find("status = ?1", Sort.descending("memberCount"), ChannelStatus.ACTIVE)
                .page(Page.ofSize(limit))
                .list();
    }

    public List<Channel> findAvailableChannels() {
        return find("status = ?1 and memberCount < maxMembers",
                Sort.descending("lastMessageAt"), ChannelStatus.ACTIVE)
                .list();
    }

    public List<Channel> findFullChannels() {
        return find("status = ?1 and memberCount >= maxMembers",
                Sort.descending("createdAt"), ChannelStatus.ACTIVE)
                .list();
    }

    // ==================== TIME-BASED QUERIES ====================

    public List<Channel> findCreatedAfter(Instant after) {
        return find("createdAt > ?1", Sort.descending("createdAt"), after).list();
    }

    public List<Channel> findCreatedBetween(Instant start, Instant end) {
        return find("createdAt >= ?1 and createdAt <= ?2",
                Sort.descending("createdAt"), start, end)
                .list();
    }

    public List<Channel> findInactiveChannels(long secondsAgo) {
        Instant threshold = Instant.now().minusSeconds(secondsAgo);
        return find("lastMessageAt < ?1 and status = ?2",
                Sort.ascending("lastMessageAt"),
                threshold,
                ChannelStatus.ACTIVE)
                .list();
    }

    // ==================== SEARCH QUERIES ====================

    public List<Channel> searchByName(String namePattern) {
        return find("lower(name) like lower(?1) and status = ?2",
                Sort.descending("memberCount"),
                "%" + namePattern + "%",
                ChannelStatus.ACTIVE)
                .list();
    }

    public List<Channel> searchByDescription(String descPattern) {
        return find("lower(description) like lower(?1) and status = ?2",
                Sort.descending("memberCount"),
                "%" + descPattern + "%",
                ChannelStatus.ACTIVE)
                .list();
    }

    public List<Channel> searchByTopic(String topicPattern) {
        return find("lower(topic) like lower(?1) and status = ?2",
                Sort.descending("lastMessageAt"),
                "%" + topicPattern + "%",
                ChannelStatus.ACTIVE)
                .list();
    }

    public List<Channel> findByTag(String tag) {
        return find("select c from Channel c join c.tags t where t = ?1 and c.status = ?2",
                Sort.descending("lastMessageAt"),
                tag,
                ChannelStatus.ACTIVE)
                .list();
    }

    // ==================== TYPE-SPECIFIC QUERIES ====================

    public List<Channel> findDirectMessageChannels() {
        return find("channelType = ?1 and status = ?2",
                Sort.descending("lastMessageAt"),
                ChannelType.DIRECT,
                ChannelStatus.ACTIVE)
                .list();
    }

    public List<Channel> findGroupChannels() {
        return find("channelType = ?1 and status = ?2",
                Sort.descending("lastMessageAt"),
                ChannelType.GROUP,
                ChannelStatus.ACTIVE)
                .list();
    }

    public List<Channel> findBroadcastChannels() {
        return find("channelType = ?1 and status = ?2",
                Sort.descending("lastMessageAt"),
                ChannelType.BROADCAST,
                ChannelStatus.ACTIVE)
                .list();
    }

    // ==================== ENCRYPTION QUERIES ====================

    public List<Channel> findEncryptedChannels() {
        return find("isEncrypted = true and status = ?1",
                Sort.descending("createdAt"), ChannelStatus.ACTIVE)
                .list();
    }

    // ==================== STATISTICS ====================

    public ChannelStatistics getStatistics() {
        long total = count();
        long active = countByStatus(ChannelStatus.ACTIVE);
        long archived = countByStatus(ChannelStatus.ARCHIVED);
        long closed = countByStatus(ChannelStatus.CLOSED);

        long publicChannels = count("isPublic = true and status = ?1", ChannelStatus.ACTIVE);
        long privateChannels = count("isPublic = false and status = ?1", ChannelStatus.ACTIVE);
        long encryptedChannels = count("isEncrypted = true and status = ?1", ChannelStatus.ACTIVE);

        Long totalMembers = find("select sum(memberCount) from Channel where status = ?1", ChannelStatus.ACTIVE)
                .project(Long.class)
                .firstResult();

        Long totalMessages = find("select sum(messageCount) from Channel where status = ?1", ChannelStatus.ACTIVE)
                .project(Long.class)
                .firstResult();

        return new ChannelStatistics(
                total,
                active,
                archived,
                closed,
                publicChannels,
                privateChannels,
                encryptedChannels,
                totalMembers != null ? totalMembers : 0L,
                totalMessages != null ? totalMessages : 0L
        );
    }

    // ==================== CLEANUP ====================

    public long deleteEmptyChannels() {
        return delete("memberCount = 0 and messageCount = 0 and status = ?1", ChannelStatus.ARCHIVED);
    }

    public long archiveInactiveBefore(Instant before) {
        List<Channel> channels = find("lastMessageAt < ?1 and status = ?2",
                before, ChannelStatus.ACTIVE).list();

        channels.forEach(Channel::archive);
        persist(channels);

        return channels.size();
    }

    // ==================== DATA MODELS ====================

    public record ChannelStatistics(
            long totalChannels,
            long activeChannels,
            long archivedChannels,
            long closedChannels,
            long publicChannels,
            long privateChannels,
            long encryptedChannels,
            long totalMembers,
            long totalMessages
    ) {}
}
