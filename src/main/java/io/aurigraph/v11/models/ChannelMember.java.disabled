package io.aurigraph.v11.models;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

/**
 * Channel Member Entity
 *
 * Represents a member (node/participant) in a blockchain channel.
 * Each member has specific permissions and roles within the channel.
 *
 * Part of Sprint 10 - Story 1 (AV11-054)
 *
 * @author Claude Code - Backend Development Agent (BDA)
 * @version 11.0.0
 * @since Sprint 10
 */
@Entity
@Table(name = "channel_members", indexes = {
    @Index(name = "idx_member_channel", columnList = "channel_id"),
    @Index(name = "idx_member_address", columnList = "member_address"),
    @Index(name = "idx_member_type", columnList = "node_type"),
    @Index(name = "idx_member_status", columnList = "status"),
    @Index(name = "idx_member_joined", columnList = "joined_at")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uk_channel_member", columnNames = {"channel_id", "member_address"})
})
public class ChannelMember {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "channel_id", nullable = false)
    private Channel channel;

    @Column(name = "member_address", nullable = false, length = 128)
    private String memberAddress;

    @Enumerated(EnumType.STRING)
    @Column(name = "node_type", nullable = false, length = 20)
    private ChannelMemberType nodeType = ChannelMemberType.PARTICIPANT;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ChannelMemberStatus status = ChannelMemberStatus.ACTIVE;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "channel_member_permissions", joinColumns = @JoinColumn(name = "member_id"))
    @Column(name = "permission")
    @Enumerated(EnumType.STRING)
    private Set<ChannelPermission> permissions = new HashSet<>();

    @Column(name = "node_id")
    private String nodeId;

    @Column(name = "public_key", length = 512)
    private String publicKey;

    @Column(name = "stake_amount")
    private Long stakeAmount = 0L;

    @Column(name = "blocks_validated")
    private Long blocksValidated = 0L;

    @Column(name = "transactions_submitted")
    private Long transactionsSubmitted = 0L;

    @Column(name = "last_activity")
    private Instant lastActivity;

    @Column(name = "joined_at", nullable = false, updatable = false)
    private Instant joinedAt;

    @Column(name = "invited_by")
    private String invitedBy;

    @Column(name = "removed_at")
    private Instant removedAt;

    @Column(name = "removed_by")
    private String removedBy;

    @Column(name = "removal_reason", columnDefinition = "TEXT")
    private String removalReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
        joinedAt = Instant.now();
        lastActivity = Instant.now();

        // Set default permissions based on node type
        if (permissions.isEmpty()) {
            setDefaultPermissions();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    // Constructors
    public ChannelMember() {
    }

    public ChannelMember(Channel channel, String memberAddress, ChannelMemberType nodeType) {
        this.channel = channel;
        this.memberAddress = memberAddress;
        this.nodeType = nodeType;
        this.status = ChannelMemberStatus.ACTIVE;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Channel getChannel() {
        return channel;
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
    }

    public String getMemberAddress() {
        return memberAddress;
    }

    public void setMemberAddress(String memberAddress) {
        this.memberAddress = memberAddress;
    }

    public ChannelMemberType getNodeType() {
        return nodeType;
    }

    public void setNodeType(ChannelMemberType nodeType) {
        this.nodeType = nodeType;
    }

    public ChannelMemberStatus getStatus() {
        return status;
    }

    public void setStatus(ChannelMemberStatus status) {
        this.status = status;
    }

    public Set<ChannelPermission> getPermissions() {
        return permissions;
    }

    public void setPermissions(Set<ChannelPermission> permissions) {
        this.permissions = permissions;
    }

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }

    public Long getStakeAmount() {
        return stakeAmount;
    }

    public void setStakeAmount(Long stakeAmount) {
        this.stakeAmount = stakeAmount;
    }

    public Long getBlocksValidated() {
        return blocksValidated;
    }

    public void setBlocksValidated(Long blocksValidated) {
        this.blocksValidated = blocksValidated;
    }

    public Long getTransactionsSubmitted() {
        return transactionsSubmitted;
    }

    public void setTransactionsSubmitted(Long transactionsSubmitted) {
        this.transactionsSubmitted = transactionsSubmitted;
    }

    public Instant getLastActivity() {
        return lastActivity;
    }

    public void setLastActivity(Instant lastActivity) {
        this.lastActivity = lastActivity;
    }

    public Instant getJoinedAt() {
        return joinedAt;
    }

    public String getInvitedBy() {
        return invitedBy;
    }

    public void setInvitedBy(String invitedBy) {
        this.invitedBy = invitedBy;
    }

    public Instant getRemovedAt() {
        return removedAt;
    }

    public void setRemovedAt(Instant removedAt) {
        this.removedAt = removedAt;
    }

    public String getRemovedBy() {
        return removedBy;
    }

    public void setRemovedBy(String removedBy) {
        this.removedBy = removedBy;
    }

    public String getRemovalReason() {
        return removalReason;
    }

    public void setRemovalReason(String removalReason) {
        this.removalReason = removalReason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    /**
     * Helper: Check if member has specific permission
     *
     * @param permission Permission to check
     * @return true if member has the permission
     */
    public boolean hasPermission(ChannelPermission permission) {
        return permissions.contains(permission);
    }

    /**
     * Helper: Add permission
     *
     * @param permission Permission to add
     */
    public void addPermission(ChannelPermission permission) {
        permissions.add(permission);
    }

    /**
     * Helper: Remove permission
     *
     * @param permission Permission to remove
     */
    public void removePermission(ChannelPermission permission) {
        permissions.remove(permission);
    }

    /**
     * Helper: Set default permissions based on node type
     */
    private void setDefaultPermissions() {
        switch (nodeType) {
            case VALIDATOR:
                permissions.add(ChannelPermission.READ);
                permissions.add(ChannelPermission.WRITE);
                permissions.add(ChannelPermission.VALIDATE);
                break;
            case PARTICIPANT:
                permissions.add(ChannelPermission.READ);
                permissions.add(ChannelPermission.WRITE);
                break;
            case OBSERVER:
                permissions.add(ChannelPermission.READ);
                break;
            case ADMIN:
                permissions.add(ChannelPermission.READ);
                permissions.add(ChannelPermission.WRITE);
                permissions.add(ChannelPermission.VALIDATE);
                permissions.add(ChannelPermission.ADMIN);
                permissions.add(ChannelPermission.MANAGE_MEMBERS);
                permissions.add(ChannelPermission.CONFIGURE);
                break;
        }
    }

    /**
     * Helper: Check if member is active
     *
     * @return true if member is active
     */
    public boolean isActive() {
        return status == ChannelMemberStatus.ACTIVE;
    }

    /**
     * Helper: Check if member is validator
     *
     * @return true if member is a validator
     */
    public boolean isValidator() {
        return nodeType == ChannelMemberType.VALIDATOR;
    }

    /**
     * Helper: Check if member is admin
     *
     * @return true if member is an admin
     */
    public boolean isAdmin() {
        return nodeType == ChannelMemberType.ADMIN || hasPermission(ChannelPermission.ADMIN);
    }

    /**
     * Helper: Update last activity timestamp
     */
    public void updateActivity() {
        this.lastActivity = Instant.now();
    }

    /**
     * Helper: Increment blocks validated counter
     */
    public void incrementBlocksValidated() {
        this.blocksValidated++;
        updateActivity();
    }

    /**
     * Helper: Increment transactions submitted counter
     */
    public void incrementTransactionsSubmitted() {
        this.transactionsSubmitted++;
        updateActivity();
    }

    @Override
    public String toString() {
        return "ChannelMember{" +
                "id='" + id + '\'' +
                ", memberAddress='" + memberAddress + '\'' +
                ", nodeType=" + nodeType +
                ", status=" + status +
                ", permissions=" + permissions +
                ", blocksValidated=" + blocksValidated +
                ", transactionsSubmitted=" + transactionsSubmitted +
                ", joinedAt=" + joinedAt +
                '}';
    }
}
