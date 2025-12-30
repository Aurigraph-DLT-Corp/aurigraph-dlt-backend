package io.aurigraph.v11.tokenization.models;

/**
 * Tokenization Result Model
 *
 * Represents the result of a tokenization operation
 *
 * @version 11.3.0
 * @author Backend Development Agent (BDA)
 */
public class TokenizationResult {

    public boolean success;
    public String transactionId;
    public String dataHash;
    public long size;
    public String channel;
    public String leveldbPath;
    public String message;

    public TokenizationResult() {
        // Default constructor for Jackson/JSON deserialization
    }

    public TokenizationResult(boolean success, String transactionId,
                            String dataHash, long size, String channel,
                            String leveldbPath, String message) {
        this.success = success;
        this.transactionId = transactionId;
        this.dataHash = dataHash;
        this.size = size;
        this.channel = channel;
        this.leveldbPath = leveldbPath;
        this.message = message;
    }
}
