package io.aurigraph.v11.contracts.composite;

import java.time.Instant;
import java.util.*;

/**
 * Main composite token entity
 */
public class CompositeToken {
    private String compositeId;
    private String assetId;
    private String assetType;
    private PrimaryToken primaryToken;
    private List<SecondaryToken> secondaryTokens;
    private String ownerAddress;
    private Instant createdAt;
    private CompositeTokenStatus status;
    private VerificationLevel verificationLevel;
    private Map<String, Object> metadata;

    // Builder pattern
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private CompositeToken token = new CompositeToken();

        public Builder compositeId(String compositeId) {
            token.compositeId = compositeId;
            return this;
        }

        public Builder assetId(String assetId) {
            token.assetId = assetId;
            return this;
        }

        public Builder assetType(String assetType) {
            token.assetType = assetType;
            return this;
        }

        public Builder primaryToken(PrimaryToken primaryToken) {
            token.primaryToken = primaryToken;
            return this;
        }

        public Builder secondaryTokens(List<SecondaryToken> secondaryTokens) {
            token.secondaryTokens = new ArrayList<>(secondaryTokens);
            return this;
        }

        public Builder ownerAddress(String ownerAddress) {
            token.ownerAddress = ownerAddress;
            return this;
        }

        public Builder createdAt(Instant createdAt) {
            token.createdAt = createdAt;
            return this;
        }

        public Builder status(CompositeTokenStatus status) {
            token.status = status;
            return this;
        }

        public Builder verificationLevel(VerificationLevel verificationLevel) {
            token.verificationLevel = verificationLevel;
            return this;
        }

        public CompositeToken build() {
            token.metadata = new HashMap<>();
            token.secondaryTokens = token.secondaryTokens != null ? token.secondaryTokens : new ArrayList<>();
            return token;
        }
    }

    // Getters and setters
    public String getCompositeId() { return compositeId; }
    public String getAssetId() { return assetId; }
    public String getAssetType() { return assetType; }
    public PrimaryToken getPrimaryToken() { return primaryToken; }
    public List<SecondaryToken> getSecondaryTokens() { return secondaryTokens; }
    public String getOwnerAddress() { return ownerAddress; }
    public void setOwnerAddress(String ownerAddress) { this.ownerAddress = ownerAddress; }
    public Instant getCreatedAt() { return createdAt; }
    public CompositeTokenStatus getStatus() { return status; }
    public void setStatus(CompositeTokenStatus status) { this.status = status; }
    public VerificationLevel getVerificationLevel() { return verificationLevel; }
    public void setVerificationLevel(VerificationLevel verificationLevel) { this.verificationLevel = verificationLevel; }
    public Map<String, Object> getMetadata() { return metadata; }
}