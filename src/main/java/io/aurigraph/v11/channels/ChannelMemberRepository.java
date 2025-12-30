package io.aurigraph.v11.channels;

import io.aurigraph.v11.channels.models.ChannelMember;
import io.aurigraph.v11.channels.models.ChannelMember.MemberRole;
import io.aurigraph.v11.channels.models.ChannelMember.MemberStatus;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Channel Member Repository - JPA/Panache Implementation
 *
 * Provides database persistence for ChannelMember entities.
 * Supports member queries, role management, and activity tracking.
 *
 * @version 3.8.0 (Phase 2 Day 11)
 * @author Aurigraph V11 Development Team
 */
@ApplicationScoped
public class ChannelMemberRepository implements PanacheRepository<ChannelMember> {

    // ==================== BASIC QUERIES ====================

    public Optional<ChannelMember> findByChannelAndMember(String channelId, String memberAddress) {
        return find("channelId = ?1 and memberAddress = ?2", channelId, memberAddress)
                .firstResultOptional();
    }

    public List<ChannelMember> findByChannel(String channelId) {
        return find("channelId", Sort.descending("joinedAt"), channelId).list();
    }

    public List<ChannelMember> findByMember(String memberAddress) {
        return find("memberAddress", Sort.descending("joinedAt"), memberAddress).list();
    }

    public boolean isMember(String channelId, String memberAddress) {
        return count("channelId = ?1 and memberAddress = ?2 and status = ?3",
                channelId, memberAddress, MemberStatus.ACTIVE) > 0;
    }

    // ==================== STATUS QUERIES ====================

    public List<ChannelMember> findActiveByChannel(String channelId) {
        return find("channelId = ?1 and status = ?2",
                Sort.descending("lastActiveAt"),
                channelId,
                MemberStatus.ACTIVE)
                .list();
    }

    public List<ChannelMember> findActiveMembersByMember(String memberAddress) {
        return find("memberAddress = ?1 and status = ?2",
                Sort.descending("lastActiveAt"),
                memberAddress,
                MemberStatus.ACTIVE)
                .list();
    }

    public List<ChannelMember> findByStatus(MemberStatus status) {
        return find("status", Sort.descending("joinedAt"), status).list();
    }

    public long countActiveByChannel(String channelId) {
        return count("channelId = ?1 and status = ?2", channelId, MemberStatus.ACTIVE);
    }

    // ==================== ROLE QUERIES ====================

    public List<ChannelMember> findByRole(String channelId, MemberRole role) {
        return find("channelId = ?1 and role = ?2 and status = ?3",
                Sort.ascending("joinedAt"),
                channelId,
                role,
                MemberStatus.ACTIVE)
                .list();
    }

    public Optional<ChannelMember> findOwner(String channelId) {
        return find("channelId = ?1 and role = ?2 and status = ?3",
                channelId,
                MemberRole.OWNER,
                MemberStatus.ACTIVE)
                .firstResultOptional();
    }

    public List<ChannelMember> findAdmins(String channelId) {
        return find("channelId = ?1 and (role = ?2 or role = ?3) and status = ?4",
                Sort.ascending("joinedAt"),
                channelId,
                MemberRole.OWNER,
                MemberRole.ADMIN,
                MemberStatus.ACTIVE)
                .list();
    }

    public List<ChannelMember> findModerators(String channelId) {
        return find("channelId = ?1 and role = ?2 and status = ?3",
                Sort.ascending("joinedAt"),
                channelId,
                MemberRole.MODERATOR,
                MemberStatus.ACTIVE)
                .list();
    }

    // ==================== ACTIVITY QUERIES ====================

    public List<ChannelMember> findRecentlyActive(String channelId, long secondsAgo) {
        Instant since = Instant.now().minusSeconds(secondsAgo);
        return find("channelId = ?1 and lastActiveAt > ?2 and status = ?3",
                Sort.descending("lastActiveAt"),
                channelId,
                since,
                MemberStatus.ACTIVE)
                .list();
    }

    public List<ChannelMember> findInactiveMembers(String channelId, long secondsAgo) {
        Instant threshold = Instant.now().minusSeconds(secondsAgo);
        return find("channelId = ?1 and lastActiveAt < ?2 and status = ?3",
                Sort.ascending("lastActiveAt"),
                channelId,
                threshold,
                MemberStatus.ACTIVE)
                .list();
    }

    public List<ChannelMember> findMostActive(String channelId, int limit) {
        return find("channelId = ?1 and status = ?2",
                Sort.descending("lastActiveAt"),
                channelId,
                MemberStatus.ACTIVE)
                .page(Page.ofSize(limit))
                .list();
    }

    // ==================== TIME-BASED QUERIES ====================

    public List<ChannelMember> findJoinedAfter(String channelId, Instant after) {
        return find("channelId = ?1 and joinedAt > ?2",
                Sort.descending("joinedAt"),
                channelId,
                after)
                .list();
    }

    public List<ChannelMember> findJoinedBetween(String channelId, Instant start, Instant end) {
        return find("channelId = ?1 and joinedAt >= ?2 and joinedAt <= ?3",
                Sort.descending("joinedAt"),
                channelId,
                start,
                end)
                .list();
    }

    public List<ChannelMember> findLeftBefore(String channelId, Instant before) {
        return find("channelId = ?1 and leftAt < ?2 and status = ?3",
                Sort.ascending("leftAt"),
                channelId,
                before,
                MemberStatus.LEFT)
                .list();
    }

    // ==================== PERMISSION QUERIES ====================

    public List<ChannelMember> findCanPost(String channelId) {
        return find("channelId = ?1 and canPost = true and status = ?2",
                Sort.descending("lastActiveAt"),
                channelId,
                MemberStatus.ACTIVE)
                .list();
    }

    public List<ChannelMember> findCanInvite(String channelId) {
        return find("channelId = ?1 and canInvite = true and status = ?2",
                Sort.descending("joinedAt"),
                channelId,
                MemberStatus.ACTIVE)
                .list();
    }

    public List<ChannelMember> findCanManage(String channelId) {
        return find("channelId = ?1 and canManage = true and status = ?2",
                Sort.descending("joinedAt"),
                channelId,
                MemberStatus.ACTIVE)
                .list();
    }

    public boolean hasPermission(String channelId, String memberAddress, String permission) {
        String query = switch (permission) {
            case "post" -> "channelId = ?1 and memberAddress = ?2 and canPost = true and status = ?3";
            case "read" -> "channelId = ?1 and memberAddress = ?2 and canRead = true and status = ?3";
            case "invite" -> "channelId = ?1 and memberAddress = ?2 and canInvite = true and status = ?3";
            case "manage" -> "channelId = ?1 and memberAddress = ?2 and canManage = true and status = ?3";
            default -> throw new IllegalArgumentException("Unknown permission: " + permission);
        };

        return count(query, channelId, memberAddress, MemberStatus.ACTIVE) > 0;
    }

    // ==================== MUTE QUERIES ====================

    public List<ChannelMember> findMutedMembers(String channelId) {
        Instant now = Instant.now();
        return find("channelId = ?1 and mutedUntil > ?2 and status = ?3",
                Sort.descending("mutedUntil"),
                channelId,
                now,
                MemberStatus.ACTIVE)
                .list();
    }

    public boolean isMuted(String channelId, String memberAddress) {
        Instant now = Instant.now();
        return count("channelId = ?1 and memberAddress = ?2 and mutedUntil > ?3 and status = ?4",
                channelId, memberAddress, now, MemberStatus.ACTIVE) > 0;
    }

    // ==================== UNREAD QUERIES ====================

    public List<ChannelMember> findWithUnreadMessages(String channelId) {
        return find("channelId = ?1 and unreadCount > 0 and status = ?2",
                Sort.descending("unreadCount"),
                channelId,
                MemberStatus.ACTIVE)
                .list();
    }

    public long getTotalUnreadCount(String memberAddress) {
        Long count = find("select sum(unreadCount) from ChannelMember where memberAddress = ?1 and status = ?2",
                memberAddress, MemberStatus.ACTIVE)
                .project(Long.class)
                .firstResult();
        return count != null ? count : 0L;
    }

    // ==================== NOTIFICATION QUERIES ====================

    public List<ChannelMember> findWithNotificationsEnabled(String channelId) {
        return find("channelId = ?1 and notificationsEnabled = true and status = ?2",
                Sort.descending("lastActiveAt"),
                channelId,
                MemberStatus.ACTIVE)
                .list();
    }

    // ==================== INVITATION QUERIES ====================

    public List<ChannelMember> findInvitedBy(String inviterAddress) {
        return find("invitedBy", Sort.descending("invitedAt"), inviterAddress).list();
    }

    public List<ChannelMember> findPendingInvites(String channelId) {
        return find("channelId = ?1 and status = ?2",
                Sort.descending("invitedAt"),
                channelId,
                MemberStatus.INVITED)
                .list();
    }

    // ==================== STATISTICS ====================

    public long countByChannel(String channelId) {
        return count("channelId = ?1 and status = ?2", channelId, MemberStatus.ACTIVE);
    }

    public long countByMember(String memberAddress) {
        return count("memberAddress = ?1 and status = ?2", memberAddress, MemberStatus.ACTIVE);
    }

    public MemberStatistics getChannelStatistics(String channelId) {
        long total = countByChannel(channelId);
        long owners = count("channelId = ?1 and role = ?2 and status = ?3",
                channelId, MemberRole.OWNER, MemberStatus.ACTIVE);
        long admins = count("channelId = ?1 and role = ?2 and status = ?3",
                channelId, MemberRole.ADMIN, MemberStatus.ACTIVE);
        long moderators = count("channelId = ?1 and role = ?2 and status = ?3",
                channelId, MemberRole.MODERATOR, MemberStatus.ACTIVE);
        long members = count("channelId = ?1 and role = ?2 and status = ?3",
                channelId, MemberRole.MEMBER, MemberStatus.ACTIVE);

        Long totalUnread = find("select sum(unreadCount) from ChannelMember where channelId = ?1 and status = ?2",
                channelId, MemberStatus.ACTIVE)
                .project(Long.class)
                .firstResult();

        Instant recentlyActive = Instant.now().minusSeconds(3600); // 1 hour
        long activeMembers = count("channelId = ?1 and lastActiveAt > ?2 and status = ?3",
                channelId, recentlyActive, MemberStatus.ACTIVE);

        return new MemberStatistics(
                total,
                owners,
                admins,
                moderators,
                members,
                activeMembers,
                totalUnread != null ? totalUnread : 0L
        );
    }

    // ==================== CLEANUP ====================

    public long deleteLeftBefore(Instant before) {
        return delete("leftAt < ?1 and status = ?2", before, MemberStatus.LEFT);
    }

    public long deleteBannedBefore(Instant before) {
        return delete("leftAt < ?1 and status = ?2", before, MemberStatus.BANNED);
    }

    // ==================== DATA MODELS ====================

    public record MemberStatistics(
            long totalMembers,
            long owners,
            long admins,
            long moderators,
            long regularMembers,
            long activeMembers,
            long totalUnread
    ) {}
}
