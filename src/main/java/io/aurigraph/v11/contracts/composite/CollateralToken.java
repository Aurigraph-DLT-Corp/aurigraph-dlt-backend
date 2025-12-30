package io.aurigraph.v11.contracts.composite;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

/**
 * Collateral Token (ERC-1155) - Tracks collateral assets backing the composite token
 */
public class CollateralToken extends SecondaryToken {
    private List<CollateralAsset> collateralAssets;

    public CollateralToken(String tokenId, String compositeId, List<CollateralAsset> collateralAssets) {
        super(tokenId, compositeId, SecondaryTokenType.COLLATERAL);
        this.collateralAssets = new ArrayList<>(collateralAssets);
    }

    @Override
    public void updateData(Map<String, Object> updateData) {
        this.lastUpdated = Instant.now();
        this.data.putAll(updateData);
    }

    public void addCollateral(CollateralAsset asset) {
        collateralAssets.add(asset);
        this.lastUpdated = Instant.now();
    }

    public void removeCollateral(String assetId) {
        collateralAssets.removeIf(asset -> assetId.equals(asset.getAssetId()));
        this.lastUpdated = Instant.now();
    }

    public BigDecimal getTotalCollateralValue() {
        return collateralAssets.stream()
            .map(CollateralAsset::getCurrentValue)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // Getters
    public List<CollateralAsset> getCollateralAssets() { return List.copyOf(collateralAssets); }

    /**
     * Collateral asset record
     */
    public static class CollateralAsset {
        private final String assetId;
        private final String assetType;
        private final BigDecimal originalValue;
        private BigDecimal currentValue;
        private final Instant addedAt;
        private String status;
        private Map<String, Object> metadata;

        public CollateralAsset(String assetId, String assetType, BigDecimal originalValue) {
            this.assetId = assetId;
            this.assetType = assetType;
            this.originalValue = originalValue;
            this.currentValue = originalValue;
            this.addedAt = Instant.now();
            this.status = "ACTIVE";
            this.metadata = new HashMap<>();
        }

        // Getters and setters
        public String getAssetId() { return assetId; }
        public String getAssetType() { return assetType; }
        public BigDecimal getOriginalValue() { return originalValue; }
        public BigDecimal getCurrentValue() { return currentValue; }
        public void setCurrentValue(BigDecimal currentValue) { this.currentValue = currentValue; }
        public Instant getAddedAt() { return addedAt; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public Map<String, Object> getMetadata() { return metadata; }
    }
}