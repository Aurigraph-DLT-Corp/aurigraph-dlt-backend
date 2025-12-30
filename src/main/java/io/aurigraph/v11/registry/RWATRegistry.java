package io.aurigraph.v11.registry;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Real-World Asset Token (RWAT) Model
 *
 * Represents a tokenized real-world asset in the registry.
 *
 * @version 11.4.0
 * @since 2025-10-13
 */
public class RWATRegistry {

    @JsonProperty("rwatId")
    private String rwatId;

    @JsonProperty("assetId")
    private String assetId; // Reference to original asset

    @JsonProperty("contractId")
    private String contractId; // Reference to ActiveContract

    @JsonProperty("assetName")
    private String assetName;

    @JsonProperty("assetType")
    private AssetType assetType;

    @JsonProperty("assetCategory")
    private String assetCategory; // RealEstate, Carbon, Art, IP, Financial, SupplyChain

    @JsonProperty("tokenSymbol")
    private String tokenSymbol;

    @JsonProperty("tokenSupply")
    private long tokenSupply;

    @JsonProperty("tokenPrice")
    private double tokenPrice; // Per token price in USD

    @JsonProperty("totalValue")
    private double totalValue; // Total asset value in USD

    @JsonProperty("verificationStatus")
    private VerificationStatus verificationStatus;

    @JsonProperty("verifiedBy")
    private String verifiedBy; // Verifier ID

    @JsonProperty("verifiedAt")
    private Instant verifiedAt;

    @JsonProperty("listedAt")
    private Instant listedAt;

    @JsonProperty("owner")
    private String owner; // Asset owner address

    @JsonProperty("jurisdiction")
    private String jurisdiction;

    @JsonProperty("location")
    private String location; // Physical location if applicable

    @JsonProperty("metadata")
    private Map<String, String> metadata;

    @JsonProperty("documentCount")
    private int documentCount;

    @JsonProperty("photoCount")
    private int photoCount;

    @JsonProperty("videoCount")
    private int videoCount;

    @JsonProperty("completenessScore")
    private double completenessScore; // 0.0 to 1.0

    @JsonProperty("qualityScore")
    private double qualityScore; // 0.0 to 1.0

    @JsonProperty("isActive")
    private boolean isActive = true;

    @JsonProperty("tradingVolume")
    private double tradingVolume; // Total trading volume in USD

    @JsonProperty("transactionCount")
    private long transactionCount;

    // Constructors
    public RWATRegistry() {
        this.metadata = new HashMap<>();
        this.listedAt = Instant.now();
    }

    // Asset Type Enum
    public enum AssetType {
        REAL_ESTATE,
        CARBON_CREDIT,
        ART_COLLECTIBLE,
        INTELLECTUAL_PROPERTY,
        FINANCIAL_ASSET,
        SUPPLY_CHAIN_ASSET,
        COMMODITY,
        OTHER
    }

    // Verification Status Enum
    public enum VerificationStatus {
        PENDING,
        IN_REVIEW,
        VERIFIED,
        REJECTED,
        EXPIRED
    }

    // Getters and setters
    public String getRwatId() { return rwatId; }
    public void setRwatId(String rwatId) { this.rwatId = rwatId; }

    public String getAssetId() { return assetId; }
    public void setAssetId(String assetId) { this.assetId = assetId; }

    public String getContractId() { return contractId; }
    public void setContractId(String contractId) { this.contractId = contractId; }

    public String getAssetName() { return assetName; }
    public void setAssetName(String assetName) { this.assetName = assetName; }

    public AssetType getAssetType() { return assetType; }
    public void setAssetType(AssetType assetType) { this.assetType = assetType; }

    public String getAssetCategory() { return assetCategory; }
    public void setAssetCategory(String assetCategory) { this.assetCategory = assetCategory; }

    public String getTokenSymbol() { return tokenSymbol; }
    public void setTokenSymbol(String tokenSymbol) { this.tokenSymbol = tokenSymbol; }

    public long getTokenSupply() { return tokenSupply; }
    public void setTokenSupply(long tokenSupply) { this.tokenSupply = tokenSupply; }

    public double getTokenPrice() { return tokenPrice; }
    public void setTokenPrice(double tokenPrice) { this.tokenPrice = tokenPrice; }

    public double getTotalValue() { return totalValue; }
    public void setTotalValue(double totalValue) { this.totalValue = totalValue; }

    public VerificationStatus getVerificationStatus() { return verificationStatus; }
    public void setVerificationStatus(VerificationStatus verificationStatus) {
        this.verificationStatus = verificationStatus;
    }

    public String getVerifiedBy() { return verifiedBy; }
    public void setVerifiedBy(String verifiedBy) { this.verifiedBy = verifiedBy; }

    public Instant getVerifiedAt() { return verifiedAt; }
    public void setVerifiedAt(Instant verifiedAt) { this.verifiedAt = verifiedAt; }

    public Instant getListedAt() { return listedAt; }
    public void setListedAt(Instant listedAt) { this.listedAt = listedAt; }

    public String getOwner() { return owner; }
    public void setOwner(String owner) { this.owner = owner; }

    public String getJurisdiction() { return jurisdiction; }
    public void setJurisdiction(String jurisdiction) { this.jurisdiction = jurisdiction; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public Map<String, String> getMetadata() { return metadata; }
    public void setMetadata(Map<String, String> metadata) { this.metadata = metadata; }

    public int getDocumentCount() { return documentCount; }
    public void setDocumentCount(int documentCount) { this.documentCount = documentCount; }

    public int getPhotoCount() { return photoCount; }
    public void setPhotoCount(int photoCount) { this.photoCount = photoCount; }

    public int getVideoCount() { return videoCount; }
    public void setVideoCount(int videoCount) { this.videoCount = videoCount; }

    public double getCompletenessScore() { return completenessScore; }
    public void setCompletenessScore(double completenessScore) { this.completenessScore = completenessScore; }

    public double getQualityScore() { return qualityScore; }
    public void setQualityScore(double qualityScore) { this.qualityScore = qualityScore; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public double getTradingVolume() { return tradingVolume; }
    public void setTradingVolume(double tradingVolume) { this.tradingVolume = tradingVolume; }

    public long getTransactionCount() { return transactionCount; }
    public void setTransactionCount(long transactionCount) { this.transactionCount = transactionCount; }

    @Override
    public String toString() {
        return String.format("RWAT{id='%s', name='%s', type=%s, value=%.2f, status=%s}",
                rwatId, assetName, assetType, totalValue, verificationStatus);
    }
}
