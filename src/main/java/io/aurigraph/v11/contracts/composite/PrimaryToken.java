package io.aurigraph.v11.contracts.composite;

import java.time.Instant;
import java.util.*;

/**
 * Primary asset token (ERC-721)
 */
public class PrimaryToken {
    private String tokenId;
    private String compositeId;
    private String assetId;
    private String assetType;
    private String ownerAddress;
    private String legalTitle;
    private String jurisdiction;
    private String coordinates;
    private boolean fractionalizable;
    private Instant createdAt;
    private Map<String, Object> metadata;

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private PrimaryToken token = new PrimaryToken();

        public Builder tokenId(String tokenId) {
            token.tokenId = tokenId;
            return this;
        }

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

        public Builder ownerAddress(String ownerAddress) {
            token.ownerAddress = ownerAddress;
            return this;
        }

        public Builder legalTitle(String legalTitle) {
            token.legalTitle = legalTitle;
            return this;
        }

        public Builder jurisdiction(String jurisdiction) {
            token.jurisdiction = jurisdiction;
            return this;
        }

        public Builder coordinates(String coordinates) {
            token.coordinates = coordinates;
            return this;
        }

        public Builder fractionalizable(boolean fractionalizable) {
            token.fractionalizable = fractionalizable;
            return this;
        }

        public Builder createdAt(Instant createdAt) {
            token.createdAt = createdAt;
            return this;
        }

        public PrimaryToken build() {
            token.metadata = new HashMap<>();
            return token;
        }
    }

    // Getters
    public String getTokenId() { return tokenId; }
    public String getCompositeId() { return compositeId; }
    public String getAssetId() { return assetId; }
    public String getAssetType() { return assetType; }
    public String getOwnerAddress() { return ownerAddress; }
    public void setOwnerAddress(String ownerAddress) { this.ownerAddress = ownerAddress; }
    public String getLegalTitle() { return legalTitle; }
    public String getJurisdiction() { return jurisdiction; }
    public String getCoordinates() { return coordinates; }
    public boolean isFractionalizable() { return fractionalizable; }
    public Instant getCreatedAt() { return createdAt; }
    public Map<String, Object> getMetadata() { return metadata; }
}