package io.aurigraph.v11.demo.config;

import io.aurigraph.v11.demo.models.NodeType;
import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * Configuration specific to Channel Nodes.
 *
 * Channel Nodes are responsible for:
 * - Managing channel lifecycle (create, update, close)
 * - Tracking channel participants and permissions
 * - Routing messages between channel members
 * - Maintaining channel state consistency
 * - Handling off-chain data when appropriate
 *
 * Performance Targets:
 * - Channel creation: <10ms
 * - Message routing: <5ms
 * - Support: 10,000+ concurrent channels per node
 * - Throughput: 500K messages/sec per node
 */
@RegisterForReflection
public class ChannelNodeConfig extends NodeConfiguration {

    /**
     * Maximum number of concurrent channels this node can manage.
     * Default: 10,000 channels
     */
    private int maxChannels = 10000;

    /**
     * Maximum number of participants allowed per channel.
     * Default: 1,000 participants
     */
    private int maxParticipantsPerChannel = 1000;

    /**
     * Size of the message queue for incoming messages.
     * Larger queues provide better burst handling but use more memory.
     * Default: 100,000 messages
     */
    private int messageQueueSize = 100000;

    /**
     * Enable off-chain data storage for non-critical channel data.
     * This can improve performance for high-volume channels.
     * Default: true
     */
    private boolean enableOffChainData = true;

    /**
     * Backend storage system for channel persistence.
     * Options: "leveldb", "rocksdb", "postgres"
     * Default: "leveldb"
     */
    private String persistenceBackend = "leveldb";

    /**
     * Cache size for frequently accessed channel data.
     * Format: number + unit (e.g., "2GB", "512MB")
     * Default: "2GB"
     */
    private String cacheSize = "2GB";

    /**
     * Message routing strategy.
     * Options: "direct", "broadcast", "multicast"
     * Default: "direct"
     */
    private String routingStrategy = "direct";

    /**
     * Enable channel state snapshots for faster recovery.
     * Default: true
     */
    private boolean enableSnapshots = true;

    /**
     * Snapshot interval in seconds.
     * Default: 300 seconds (5 minutes)
     */
    private int snapshotIntervalSeconds = 300;

    // Constructors

    public ChannelNodeConfig() {
        super();
        setNodeType(NodeType.CHANNEL);
    }

    public ChannelNodeConfig(String nodeId) {
        super(nodeId, NodeType.CHANNEL);
    }

    // Getters and Setters

    public int getMaxChannels() {
        return maxChannels;
    }

    public void setMaxChannels(int maxChannels) {
        this.maxChannels = maxChannels;
    }

    public int getMaxParticipantsPerChannel() {
        return maxParticipantsPerChannel;
    }

    public void setMaxParticipantsPerChannel(int maxParticipantsPerChannel) {
        this.maxParticipantsPerChannel = maxParticipantsPerChannel;
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

    public String getPersistenceBackend() {
        return persistenceBackend;
    }

    public void setPersistenceBackend(String persistenceBackend) {
        this.persistenceBackend = persistenceBackend;
    }

    public String getCacheSize() {
        return cacheSize;
    }

    public void setCacheSize(String cacheSize) {
        this.cacheSize = cacheSize;
    }

    public String getRoutingStrategy() {
        return routingStrategy;
    }

    public void setRoutingStrategy(String routingStrategy) {
        this.routingStrategy = routingStrategy;
    }

    public boolean isEnableSnapshots() {
        return enableSnapshots;
    }

    public void setEnableSnapshots(boolean enableSnapshots) {
        this.enableSnapshots = enableSnapshots;
    }

    public int getSnapshotIntervalSeconds() {
        return snapshotIntervalSeconds;
    }

    public void setSnapshotIntervalSeconds(int snapshotIntervalSeconds) {
        this.snapshotIntervalSeconds = snapshotIntervalSeconds;
    }

    @Override
    public void validate() {
        // Validate base configuration first
        super.validate();

        // Validate channel-specific configuration
        if (maxChannels <= 0) {
            throw new IllegalArgumentException("Max channels must be greater than 0");
        }

        if (maxChannels > 1000000) {
            throw new IllegalArgumentException("Max channels cannot exceed 1,000,000");
        }

        if (maxParticipantsPerChannel <= 0) {
            throw new IllegalArgumentException("Max participants per channel must be greater than 0");
        }

        if (maxParticipantsPerChannel > 100000) {
            throw new IllegalArgumentException("Max participants per channel cannot exceed 100,000");
        }

        if (messageQueueSize <= 0) {
            throw new IllegalArgumentException("Message queue size must be greater than 0");
        }

        if (!persistenceBackend.matches("leveldb|rocksdb|postgres")) {
            throw new IllegalArgumentException("Persistence backend must be one of: leveldb, rocksdb, postgres");
        }

        if (!cacheSize.matches("\\d+[KMG]B")) {
            throw new IllegalArgumentException("Cache size must be in format: number + KB/MB/GB (e.g., '2GB')");
        }

        if (!routingStrategy.matches("direct|broadcast|multicast")) {
            throw new IllegalArgumentException("Routing strategy must be one of: direct, broadcast, multicast");
        }

        if (enableSnapshots && snapshotIntervalSeconds <= 0) {
            throw new IllegalArgumentException("Snapshot interval must be greater than 0 when snapshots are enabled");
        }
    }

    @Override
    public String toString() {
        return "ChannelNodeConfig{" +
               "nodeId='" + getNodeId() + '\'' +
               ", maxChannels=" + maxChannels +
               ", maxParticipantsPerChannel=" + maxParticipantsPerChannel +
               ", messageQueueSize=" + messageQueueSize +
               ", enableOffChainData=" + enableOffChainData +
               ", persistenceBackend='" + persistenceBackend + '\'' +
               ", cacheSize='" + cacheSize + '\'' +
               ", routingStrategy='" + routingStrategy + '\'' +
               '}';
    }
}
