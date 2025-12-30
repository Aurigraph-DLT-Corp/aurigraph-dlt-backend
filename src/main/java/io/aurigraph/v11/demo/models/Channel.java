package io.aurigraph.v11.demo.models;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Represents a communication channel in the Aurigraph V11 network.
 *
 * <p>A channel enables multi-party communication with controlled participant access,
 * message ordering, and state consistency. Channels support both on-chain and off-chain
 * data flows depending on configuration.</p>
 *
 * <p>Key features:
 * <ul>
 *   <li>Multi-participant support (up to 1000 participants per channel)</li>
 *   <li>Message ordering and delivery guarantees</li>
 *   <li>Channel lifecycle management (create, activate, close)</li>
 *   <li>Participant permission management</li>
 * </ul>
 * </p>
 *
 * @author Backend Development Agent (BDA)
 * @version 11.0.0
 * @since 2025-10-11
 */
public class Channel {

    private String channelId;
    private String name;
    private String description;
    private ChannelState state;
    private List<String> participantIds;
    private String createdBy;
    private Instant createdAt;
    private Instant updatedAt;
    private ChannelConfig config;
    private long messageCount;
    private long totalDataSize;

    /**
     * Default constructor for deserialization.
     */
    public Channel() {
        this.participantIds = new ArrayList<>();
        this.state = ChannelState.CREATED;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
        this.messageCount = 0L;
        this.totalDataSize = 0L;
    }

    /**
     * Creates a new channel with specified parameters.
     *
     * @param channelId The unique channel identifier
     * @param name The channel name
     * @param description The channel description
     * @param config The channel configuration
     * @param createdBy The node ID that created this channel
     */
    public Channel(String channelId, String name, String description, ChannelConfig config, String createdBy) {
        this();
        this.channelId = channelId;
        this.name = name;
        this.description = description;
        this.config = config;
        this.createdBy = createdBy;
    }

    // Getters and Setters

    public String getChannelId() {
        return channelId;
    }

    public void setChannelId(String channelId) {
        this.channelId = channelId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
        this.updatedAt = Instant.now();
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
        this.updatedAt = Instant.now();
    }

    public ChannelState getState() {
        return state;
    }

    public void setState(ChannelState state) {
        this.state = state;
        this.updatedAt = Instant.now();
    }

    public List<String> getParticipantIds() {
        return new ArrayList<>(participantIds);
    }

    public void setParticipantIds(List<String> participantIds) {
        this.participantIds = new ArrayList<>(participantIds);
        this.updatedAt = Instant.now();
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public ChannelConfig getConfig() {
        return config;
    }

    public void setConfig(ChannelConfig config) {
        this.config = config;
        this.updatedAt = Instant.now();
    }

    public long getMessageCount() {
        return messageCount;
    }

    public void setMessageCount(long messageCount) {
        this.messageCount = messageCount;
    }

    public long getTotalDataSize() {
        return totalDataSize;
    }

    public void setTotalDataSize(long totalDataSize) {
        this.totalDataSize = totalDataSize;
    }

    // Business methods

    /**
     * Adds a participant to the channel.
     *
     * @param participantId The participant ID to add
     * @return true if the participant was added, false if already present
     */
    public boolean addParticipant(String participantId) {
        if (!participantIds.contains(participantId)) {
            participantIds.add(participantId);
            this.updatedAt = Instant.now();
            return true;
        }
        return false;
    }

    /**
     * Removes a participant from the channel.
     *
     * @param participantId The participant ID to remove
     * @return true if the participant was removed, false if not present
     */
    public boolean removeParticipant(String participantId) {
        boolean removed = participantIds.remove(participantId);
        if (removed) {
            this.updatedAt = Instant.now();
        }
        return removed;
    }

    /**
     * Checks if a participant is a member of this channel.
     *
     * @param participantId The participant ID to check
     * @return true if the participant is a member
     */
    public boolean hasParticipant(String participantId) {
        return participantIds.contains(participantId);
    }

    /**
     * Increments the message count and updates total data size.
     *
     * @param messageSize The size of the message in bytes
     */
    public void recordMessage(long messageSize) {
        this.messageCount++;
        this.totalDataSize += messageSize;
        this.updatedAt = Instant.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Channel channel = (Channel) o;
        return Objects.equals(channelId, channel.channelId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(channelId);
    }

    @Override
    public String toString() {
        return "Channel{" +
                "channelId='" + channelId + '\'' +
                ", name='" + name + '\'' +
                ", state=" + state +
                ", participantCount=" + participantIds.size() +
                ", messageCount=" + messageCount +
                ", totalDataSize=" + totalDataSize +
                ", createdAt=" + createdAt +
                '}';
    }
}
