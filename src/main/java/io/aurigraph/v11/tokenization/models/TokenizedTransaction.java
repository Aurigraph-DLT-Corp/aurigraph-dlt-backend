package io.aurigraph.v11.tokenization.models;

/**
 * Tokenized Transaction Model
 *
 * Represents a tokenized transaction from external API data
 *
 * @version 11.3.0
 * @author Backend Development Agent (BDA)
 */
public class TokenizedTransaction {

    public String id;
    public String sourceId;
    public String sourceName;
    public String channel;
    public String timestamp;  // ISO format
    public String dataHash;   // SHA-256 hash (0x prefix)
    public long size;         // bytes
    public String status;     // "pending", "stored", "verified"
    public String leveldbPath;  // LevelDB storage path

    public TokenizedTransaction() {
        // Default constructor for Jackson/JSON deserialization
    }

    public TokenizedTransaction(String id, String sourceId, String sourceName,
                               String channel, String timestamp, String dataHash,
                               long size, String status, String leveldbPath) {
        this.id = id;
        this.sourceId = sourceId;
        this.sourceName = sourceName;
        this.channel = channel;
        this.timestamp = timestamp;
        this.dataHash = dataHash;
        this.size = size;
        this.status = status;
        this.leveldbPath = leveldbPath;
    }
}
