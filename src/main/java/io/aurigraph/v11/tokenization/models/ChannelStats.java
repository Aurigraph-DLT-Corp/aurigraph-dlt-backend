package io.aurigraph.v11.tokenization.models;

/**
 * Channel Statistics Model
 *
 * Represents statistics for a tokenization channel
 *
 * @version 11.3.0
 * @author Backend Development Agent (BDA)
 */
public class ChannelStats {

    public String channelId;
    public String channelName;
    public int transactionCount;
    public long totalSize;  // Total bytes stored
    public String lastUpdated;  // ISO timestamp
    public String status;  // "active", "paused", "error"

    public ChannelStats() {
        // Default constructor for Jackson/JSON deserialization
    }

    public ChannelStats(String channelId, String channelName,
                       int transactionCount, long totalSize,
                       String lastUpdated, String status) {
        this.channelId = channelId;
        this.channelName = channelName;
        this.transactionCount = transactionCount;
        this.totalSize = totalSize;
        this.lastUpdated = lastUpdated;
        this.status = status;
    }
}
