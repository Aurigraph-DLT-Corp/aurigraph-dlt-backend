package io.aurigraph.v11.contracts.composite;

import java.time.Instant;
import java.util.*;

/**
 * Base class for secondary tokens
 */
public abstract class SecondaryToken {
    protected String tokenId;
    protected String compositeId;
    protected SecondaryTokenType tokenType;
    protected Instant createdAt;
    protected Instant lastUpdated;
    protected Map<String, Object> data;

    public SecondaryToken(String tokenId, String compositeId, SecondaryTokenType tokenType) {
        this.tokenId = tokenId;
        this.compositeId = compositeId;
        this.tokenType = tokenType;
        this.createdAt = Instant.now();
        this.lastUpdated = Instant.now();
        this.data = new HashMap<>();
    }

    public abstract void updateData(Map<String, Object> updateData);

    // Getters and setters
    public String getTokenId() { return tokenId; }
    public String getCompositeId() { return compositeId; }
    public SecondaryTokenType getTokenType() { return tokenType; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(Instant lastUpdated) { this.lastUpdated = lastUpdated; }
    public Map<String, Object> getData() { return data; }
}