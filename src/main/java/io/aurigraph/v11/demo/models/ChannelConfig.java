package io.aurigraph.v11.demo.models;

/**
 * Configuration settings for a channel in the Aurigraph V11 network.
 *
 * <p>This class encapsulates all configurable parameters for channel operation,
 * including capacity limits, performance settings, and feature flags.</p>
 *
 * @author Backend Development Agent (BDA)
 * @version 11.0.0
 * @since 2025-10-11
 */
public class ChannelConfig {

    private int maxParticipants;
    private int messageQueueSize;
    private boolean enableOffChainData;
    private boolean enableEncryption;
    private boolean enableMessageOrdering;
    private long maxMessageSize;
    private int messageTTL; // Time to live in seconds

    /**
     * Default constructor with standard configuration values.
     */
    public ChannelConfig() {
        this.maxParticipants = 1000;
        this.messageQueueSize = 100000;
        this.enableOffChainData = true;
        this.enableEncryption = true;
        this.enableMessageOrdering = true;
        this.maxMessageSize = 1024 * 1024; // 1MB
        this.messageTTL = 3600; // 1 hour
    }

    /**
     * Creates a channel configuration with specified parameters.
     *
     * @param maxParticipants Maximum number of participants
     * @param messageQueueSize Maximum queue size for pending messages
     * @param enableOffChainData Whether to allow off-chain data
     */
    public ChannelConfig(int maxParticipants, int messageQueueSize, boolean enableOffChainData) {
        this();
        this.maxParticipants = maxParticipants;
        this.messageQueueSize = messageQueueSize;
        this.enableOffChainData = enableOffChainData;
    }

    // Getters and Setters

    public int getMaxParticipants() {
        return maxParticipants;
    }

    public void setMaxParticipants(int maxParticipants) {
        this.maxParticipants = maxParticipants;
    }

    public int getMessageQueueSize() {
        return messageQueueSize;
    }

    public void setMessageQueueSize(int messageQueueSize) {
        this.messageQueueSize = messageQueueSize;
    }

    public boolean isEnableOffChainData() {
        return enableOffChainData;
    }

    public void setEnableOffChainData(boolean enableOffChainData) {
        this.enableOffChainData = enableOffChainData;
    }

    public boolean isEnableEncryption() {
        return enableEncryption;
    }

    public void setEnableEncryption(boolean enableEncryption) {
        this.enableEncryption = enableEncryption;
    }

    public boolean isEnableMessageOrdering() {
        return enableMessageOrdering;
    }

    public void setEnableMessageOrdering(boolean enableMessageOrdering) {
        this.enableMessageOrdering = enableMessageOrdering;
    }

    public long getMaxMessageSize() {
        return maxMessageSize;
    }

    public void setMaxMessageSize(long maxMessageSize) {
        this.maxMessageSize = maxMessageSize;
    }

    public int getMessageTTL() {
        return messageTTL;
    }

    public void setMessageTTL(int messageTTL) {
        this.messageTTL = messageTTL;
    }

    @Override
    public String toString() {
        return "ChannelConfig{" +
                "maxParticipants=" + maxParticipants +
                ", messageQueueSize=" + messageQueueSize +
                ", enableOffChainData=" + enableOffChainData +
                ", enableEncryption=" + enableEncryption +
                ", enableMessageOrdering=" + enableMessageOrdering +
                ", maxMessageSize=" + maxMessageSize +
                ", messageTTL=" + messageTTL +
                '}';
    }
}
