package io.aurigraph.v11.demo.services;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Data Feed Token - Represents tokenized data from external APIs
 *
 * Each external API feed generates tokens that are registered in the Merkle tree registry
 * for cryptographic verification and immutability.
 */
public class DataFeedToken {
    public String tokenId;
    public String feedId;
    public String feedName;
    public String dataType;
    public Object tokenizedData;
    public String tokenHash;
    public Instant createdAt;
    public Instant updatedAt;
    public Map<String, Object> metadata;

    public DataFeedToken(String tokenId, String feedId, String feedName, String dataType, Object tokenizedData) {
        this.tokenId = tokenId;
        this.feedId = feedId;
        this.feedName = feedName;
        this.dataType = dataType;
        this.tokenizedData = tokenizedData;
        this.tokenHash = MerkleHashUtil.sha3Hash(tokenId + feedId + String.valueOf(tokenizedData));
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
        this.metadata = new HashMap<>();
    }

    public void update(Object newData) {
        this.tokenizedData = newData;
        this.tokenHash = MerkleHashUtil.sha3Hash(tokenId + feedId + String.valueOf(newData));
        this.updatedAt = Instant.now();
    }

    /**
     * Utility class for SHA3 hashing
     */
    protected static class MerkleHashUtil {
        public static String sha3Hash(String input) {
            org.bouncycastle.crypto.digests.SHA3Digest digest =
                new org.bouncycastle.crypto.digests.SHA3Digest(256);
            byte[] inputBytes = input.getBytes();
            digest.update(inputBytes, 0, inputBytes.length);
            byte[] hash = new byte[32];
            digest.doFinal(hash, 0);
            return org.bouncycastle.util.encoders.Hex.toHexString(hash);
        }
    }
}
