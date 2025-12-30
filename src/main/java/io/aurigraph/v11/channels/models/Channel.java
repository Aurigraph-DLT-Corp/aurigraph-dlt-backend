package io.aurigraph.v11.channels.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Channel Model for Aurigraph V11 - LevelDB Compatible
 *
 * Represents a communication channel for multi-party messaging.
 * Supports real-time messaging, member management, and message history.
 *
 * LevelDB Storage: Uses channelId as primary key
 * JSON Serializable: All fields stored as JSON in LevelDB
 *
 * @version 4.0.0 (LevelDB Migration - Oct 8, 2025)
 * @author Aurigraph V11 Development Team
 */
public class Channel {

    @JsonProperty("channelId")
    private String channelId;

    @JsonProperty("name")
    private String name;

    @JsonProperty("channelType")
    private ChannelType channelType;

    @JsonProperty("status")
    private ChannelStatus status;

    @JsonProperty("ownerAddress")
    private String ownerAddress;

    // Channel configuration
    @JsonProperty("isPublic")
    private Boolean isPublic = false;

    @JsonProperty("isEncrypted")
    private Boolean isEncrypted = false;

    @JsonProperty("maxMembers")
    private Integer maxMembers = 100;

    @JsonProperty("allowGuestAccess")
    private Boolean allowGuestAccess = false;

    // Timestamps
    @JsonProperty("createdAt")
    private Instant createdAt;

    @JsonProperty("updatedAt")
    private Instant updatedAt;

    @JsonProperty("lastMessageAt")
    private Instant lastMessageAt;

    @JsonProperty("closedAt")
    private Instant closedAt;

    // Statistics
    @JsonProperty("memberCount")
    private Integer memberCount = 0;

    @JsonProperty("messageCount")
    private Long messageCount = 0L;

    @JsonProperty("activeMembers")
    private Integer activeMembers = 0;

    // Metadata
    @JsonProperty("description")
    private String description;

    @JsonProperty("topic")
    private String topic;

    @JsonProperty("metadata")
    private String metadata;

    @JsonProperty("tags")
    private List<String> tags = new ArrayList<>();

    // ==================== CONSTRUCTORS ====================

    public Channel() {
        this.createdAt = Instant.now();
        this.status = ChannelStatus.ACTIVE;
        this.memberCount = 0;
        this.messageCount = 0L;
        this.activeMembers = 0;
    }

    public Channel(String channelId, String name, String ownerAddress, ChannelType channelType) {
        this();
        this.channelId = channelId;
        this.name = name;
        this.ownerAddress = ownerAddress;
        this.channelType = channelType;
    }

    // ==================== BUSINESS LOGIC METHODS ====================

    /**
     * Ensure createdAt is set (call before first persist)
     */
    public void ensureCreatedAt() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    /**
     * Update timestamp (call before each persist)
     */
    public void updateTimestamp() {
        updatedAt = Instant.now();
    }

    /**
     * Close the channel
     */
    public void close() {
        if (status == ChannelStatus.CLOSED) {
            throw new IllegalStateException("Channel is already closed");
        }
        this.status = ChannelStatus.CLOSED;
        this.closedAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    /**
     * Archive the channel
     */
    public void archive() {
        if (status == ChannelStatus.CLOSED) {
            throw new IllegalStateException("Cannot archive a closed channel");
        }
        this.status = ChannelStatus.ARCHIVED;
        this.updatedAt = Instant.now();
    }

    /**
     * Reactivate an archived channel
     */
    public void reactivate() {
        if (status != ChannelStatus.ARCHIVED) {
            throw new IllegalStateException("Only archived channels can be reactivated");
        }
        this.status = ChannelStatus.ACTIVE;
        this.updatedAt = Instant.now();
    }

    /**
     * Record a new message
     */
    public void recordMessage() {
        this.messageCount++;
        this.lastMessageAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    /**
     * Update member count
     */
    public void updateMemberCount(int count) {
        this.memberCount = count;
        this.updatedAt = Instant.now();
    }

    /**
     * Update active member count
     */
    public void updateActiveMembers(int count) {
        this.activeMembers = count;
        this.updatedAt = Instant.now();
    }

    /**
     * Check if channel is full
     */
    public boolean isFull() {
        return memberCount >= maxMembers;
    }

    /**
     * Check if channel is active
     */
    public boolean isActive() {
        return status == ChannelStatus.ACTIVE;
    }

    /**
     * Add tag
     */
    public void addTag(String tag) {
        if (!this.tags.contains(tag)) {
            this.tags.add(tag);
        }
    }

    // ==================== GETTERS AND SETTERS ====================

    public String getChannelId() { return channelId; }
    public void setChannelId(String channelId) { this.channelId = channelId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public ChannelType getChannelType() { return channelType; }
    public void setChannelType(ChannelType channelType) { this.channelType = channelType; }

    public ChannelStatus getStatus() { return status; }
    public void setStatus(ChannelStatus status) { this.status = status; }

    public String getOwnerAddress() { return ownerAddress; }
    public void setOwnerAddress(String ownerAddress) { this.ownerAddress = ownerAddress; }

    public Boolean getIsPublic() { return isPublic; }
    public void setIsPublic(Boolean isPublic) { this.isPublic = isPublic; }

    public Boolean getIsEncrypted() { return isEncrypted; }
    public void setIsEncrypted(Boolean isEncrypted) { this.isEncrypted = isEncrypted; }

    public Integer getMaxMembers() { return maxMembers; }
    public void setMaxMembers(Integer maxMembers) { this.maxMembers = maxMembers; }

    public Boolean getAllowGuestAccess() { return allowGuestAccess; }
    public void setAllowGuestAccess(Boolean allowGuestAccess) { this.allowGuestAccess = allowGuestAccess; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public Instant getLastMessageAt() { return lastMessageAt; }
    public void setLastMessageAt(Instant lastMessageAt) { this.lastMessageAt = lastMessageAt; }

    public Instant getClosedAt() { return closedAt; }
    public void setClosedAt(Instant closedAt) { this.closedAt = closedAt; }

    public Integer getMemberCount() { return memberCount; }
    public void setMemberCount(Integer memberCount) { this.memberCount = memberCount; }

    public Long getMessageCount() { return messageCount; }
    public void setMessageCount(Long messageCount) { this.messageCount = messageCount; }

    public Integer getActiveMembers() { return activeMembers; }
    public void setActiveMembers(Integer activeMembers) { this.activeMembers = activeMembers; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getTopic() { return topic; }
    public void setTopic(String topic) { this.topic = topic; }

    public String getMetadata() { return metadata; }
    public void setMetadata(String metadata) { this.metadata = metadata; }

    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }

    // ==================== ENUM DEFINITIONS ====================

    public enum ChannelType {
        PUBLIC,         // Public channel, anyone can join
        PRIVATE,        // Private channel, invite only
        DIRECT,         // Direct message channel (1-on-1)
        GROUP,          // Group channel (multiple members)
        BROADCAST,      // Broadcast channel (one-to-many)
        SUPPORT,        // Customer support channel
        NOTIFICATION    // Notification channel (read-only for members)
    }

    public enum ChannelStatus {
        ACTIVE,         // Channel is active
        ARCHIVED,       // Channel is archived but can be reactivated
        CLOSED          // Channel is permanently closed
    }

    @Override
    public String toString() {
        return String.format("Channel{channelId='%s', name='%s', type=%s, status=%s, members=%d}",
                channelId, name, channelType, status, memberCount);
    }
}
