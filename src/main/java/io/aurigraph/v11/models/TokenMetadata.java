package io.aurigraph.v11.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.UUID;

/**
 * Token Metadata Model for Aurigraph V11 - LevelDB Compatible
 *
 * Stores detailed metadata for individual tokens (NFTs) or token types.
 * Particularly important for ERC721 and ERC1155 tokens where each token
 * can have unique metadata, images, and attributes.
 *
 * Features:
 * - IPFS-based metadata storage
 * - NFT attributes and properties
 * - Real-world asset data integration
 * - Verification and validation tracking
 * - Multi-format media support (images, videos, 3D models)
 *
 * LevelDB Storage: Uses contentHash as primary key
 * JSON Serializable: All fields stored as JSON in LevelDB
 *
 * @version 4.0.0 (LevelDB Migration - Oct 8, 2025)
 * @since Sprint 12 - AV11-058: Token & RWA APIs
 */
public class TokenMetadata {

    /**
     * Content hash for integrity verification - PRIMARY KEY for LevelDB
     * SHA3-256 hash of all metadata fields
     */
    @JsonProperty("contentHash")
    private String contentHash;

    /**
     * Token ID (for ERC721/ERC1155)
     * For ERC20, this will be null as metadata applies to all tokens
     */
    @JsonProperty("tokenId")
    private String tokenId;

    /**
     * Reference to the token registry address (replaces @ManyToOne relationship)
     */
    @JsonProperty("tokenRegistryAddress")
    private String tokenRegistryAddress;

    /**
     * Metadata URI (URL or IPFS path)
     * Example: "ipfs://QmHash...", "https://api.example.com/metadata/123"
     */
    @JsonProperty("metadataUri")
    private String metadataUri;

    /**
     * Token name (can differ from registry name for individual NFTs)
     * Example: "Bored Ape #1234", "Carbon Credit Certificate #5678"
     */
    @JsonProperty("name")
    private String name;

    /**
     * Detailed description of the token
     */
    @JsonProperty("description")
    private String description;

    /**
     * Primary image URL or IPFS hash
     * Example: "ipfs://QmImageHash", "https://cdn.example.com/nft/1234.png"
     */
    @JsonProperty("image")
    private String image;

    /**
     * Image IPFS hash (redundant storage for reliability)
     */
    @JsonProperty("imageIpfsHash")
    private String imageIpfsHash;

    /**
     * Animation/video URL for animated NFTs
     */
    @JsonProperty("animationUrl")
    private String animationUrl;

    /**
     * External URL to project/asset website
     */
    @JsonProperty("externalUrl")
    private String externalUrl;

    /**
     * Background color (for NFT display)
     * Hex format: "FFFFFF"
     */
    @JsonProperty("backgroundColor")
    private String backgroundColor;

    /**
     * Token attributes in JSON format
     * OpenSea-compatible format:
     * [
     *   {"trait_type": "Color", "value": "Blue"},
     *   {"trait_type": "Rarity", "value": "Legendary"}
     * ]
     */
    @JsonProperty("attributes")
    private String attributes;

    /**
     * Additional properties in JSON format
     * Custom fields specific to the token type
     */
    @JsonProperty("properties")
    private String properties;

    /**
     * RWA-specific data in JSON format
     * For real-world asset tokens, includes:
     * - Asset location
     * - Asset valuation
     * - Certification details
     * - Ownership documents
     * - Legal compliance data
     */
    @JsonProperty("rwaData")
    private String rwaData;

    /**
     * IPFS hash of the complete metadata JSON
     */
    @JsonProperty("ipfsHash")
    private String ipfsHash;

    /**
     * Metadata verification status
     */
    @JsonProperty("verificationStatus")
    private MetadataVerificationStatus verificationStatus = MetadataVerificationStatus.UNVERIFIED;

    /**
     * Verifier address (who verified this metadata)
     */
    @JsonProperty("verifierAddress")
    private String verifierAddress;

    /**
     * Verification timestamp
     */
    @JsonProperty("verifiedAt")
    private Instant verifiedAt;

    /**
     * Flag indicating if metadata is frozen (immutable)
     */
    @JsonProperty("isFrozen")
    private Boolean isFrozen = false;

    /**
     * Timestamp when metadata was frozen
     */
    @JsonProperty("frozenAt")
    private Instant frozenAt;

    /**
     * Media type classification
     */
    @JsonProperty("mediaType")
    private MediaType mediaType = MediaType.IMAGE;

    /**
     * File format/MIME type
     * Example: "image/png", "video/mp4", "model/gltf-binary"
     */
    @JsonProperty("fileFormat")
    private String fileFormat;

    /**
     * File size in bytes
     */
    @JsonProperty("fileSize")
    private Long fileSize;

    /**
     * Image dimensions (if applicable)
     * Format: "1920x1080"
     */
    @JsonProperty("dimensions")
    private String dimensions;

    /**
     * Duration in seconds (for video/audio)
     */
    @JsonProperty("duration")
    private Integer duration;

    /**
     * Creator/artist name
     */
    @JsonProperty("creatorName")
    private String creatorName;

    /**
     * Creator address
     */
    @JsonProperty("creatorAddress")
    private String creatorAddress;

    /**
     * Royalty percentage (for NFT resales)
     * Stored as basis points (100 = 1%)
     */
    @JsonProperty("royaltyPercentage")
    private Integer royaltyPercentage = 0;

    /**
     * Royalty recipient address
     */
    @JsonProperty("royaltyRecipient")
    private String royaltyRecipient;

    /**
     * Category/collection name
     */
    @JsonProperty("category")
    private String category;

    /**
     * Tags for searchability (comma-separated)
     */
    @JsonProperty("tags")
    private String tags;

    /**
     * Rarity score (0-100, calculated from attributes)
     */
    @JsonProperty("rarityScore")
    private Double rarityScore;

    /**
     * Metadata version (for tracking updates)
     */
    @JsonProperty("version")
    private Integer version = 1;

    /**
     * Previous metadata content hash (for version tracking)
     */
    @JsonProperty("previousMetadataHash")
    private String previousMetadataHash;

    /**
     * Metadata creation timestamp
     */
    @JsonProperty("createdAt")
    private Instant createdAt;

    /**
     * Last update timestamp
     */
    @JsonProperty("updatedAt")
    private Instant updatedAt;

    /**
     * Soft delete flag
     */
    @JsonProperty("isDeleted")
    private Boolean isDeleted = false;

    // Lifecycle methods (converted from JPA @PrePersist/@PreUpdate)

    /**
     * Initialize timestamps and content hash (call before first save)
     */
    public void ensureCreatedAt() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (updatedAt == null) {
            updatedAt = Instant.now();
        }

        // Generate content hash if not set
        if (contentHash == null) {
            contentHash = generateContentHash();
        }
    }

    /**
     * Update timestamp and regenerate content hash (call before each save)
     * Throws exception if metadata is frozen
     */
    public void updateTimestamp() {
        if (isFrozen) {
            throw new IllegalStateException("Cannot update frozen metadata");
        }
        updatedAt = Instant.now();
        // Regenerate content hash on update
        contentHash = generateContentHash();
    }

    // Constructors

    public TokenMetadata() {
    }

    public TokenMetadata(String tokenRegistryAddress, String name, String description) {
        this.tokenRegistryAddress = tokenRegistryAddress;
        this.name = name;
        this.description = description;
        this.verificationStatus = MetadataVerificationStatus.UNVERIFIED;
    }

    // Helper methods

    /**
     * Generate content hash from all metadata fields
     */
    private String generateContentHash() {
        // Simple implementation - in production, use proper SHA3-256 hashing
        StringBuilder content = new StringBuilder();
        if (name != null) content.append(name);
        if (description != null) content.append(description);
        if (image != null) content.append(image);
        if (attributes != null) content.append(attributes);
        if (rwaData != null) content.append(rwaData);

        return UUID.nameUUIDFromBytes(content.toString().getBytes()).toString();
    }

    /**
     * Freeze metadata (make immutable)
     */
    public void freeze() {
        this.isFrozen = true;
        this.frozenAt = Instant.now();
    }

    /**
     * Verify metadata
     */
    public void verify(String verifierAddress) {
        this.verificationStatus = MetadataVerificationStatus.VERIFIED;
        this.verifierAddress = verifierAddress;
        this.verifiedAt = Instant.now();
    }

    /**
     * Reject metadata verification
     */
    public void reject(String verifierAddress) {
        this.verificationStatus = MetadataVerificationStatus.REJECTED;
        this.verifierAddress = verifierAddress;
        this.verifiedAt = Instant.now();
    }

    /**
     * Check if metadata is verified
     */
    public boolean isVerified() {
        return verificationStatus == MetadataVerificationStatus.VERIFIED;
    }

    /**
     * Check if this is RWA metadata
     */
    public boolean hasRWAData() {
        return rwaData != null && !rwaData.trim().isEmpty();
    }

    /**
     * Calculate rarity score from attributes
     */
    public void calculateRarityScore() {
        // Simple implementation - in production, use actual rarity calculation
        // based on attribute frequency in the collection
        if (attributes != null && !attributes.trim().isEmpty()) {
            // Count number of attributes (simple JSON array count)
            int attrCount = attributes.split("\\{").length - 1;
            this.rarityScore = Math.min(100.0, attrCount * 10.0);
        } else {
            this.rarityScore = 0.0;
        }
    }

    // Getters and Setters

    public String getContentHash() {
        return contentHash;
    }

    public void setContentHash(String contentHash) {
        this.contentHash = contentHash;
    }

    public String getTokenId() {
        return tokenId;
    }

    public void setTokenId(String tokenId) {
        this.tokenId = tokenId;
    }

    public String getTokenRegistryAddress() {
        return tokenRegistryAddress;
    }

    public void setTokenRegistryAddress(String tokenRegistryAddress) {
        this.tokenRegistryAddress = tokenRegistryAddress;
    }

    public String getMetadataUri() {
        return metadataUri;
    }

    public void setMetadataUri(String metadataUri) {
        this.metadataUri = metadataUri;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getImageIpfsHash() {
        return imageIpfsHash;
    }

    public void setImageIpfsHash(String imageIpfsHash) {
        this.imageIpfsHash = imageIpfsHash;
    }

    public String getAnimationUrl() {
        return animationUrl;
    }

    public void setAnimationUrl(String animationUrl) {
        this.animationUrl = animationUrl;
    }

    public String getExternalUrl() {
        return externalUrl;
    }

    public void setExternalUrl(String externalUrl) {
        this.externalUrl = externalUrl;
    }

    public String getBackgroundColor() {
        return backgroundColor;
    }

    public void setBackgroundColor(String backgroundColor) {
        this.backgroundColor = backgroundColor;
    }

    public String getAttributes() {
        return attributes;
    }

    public void setAttributes(String attributes) {
        this.attributes = attributes;
    }

    public String getProperties() {
        return properties;
    }

    public void setProperties(String properties) {
        this.properties = properties;
    }

    public String getRwaData() {
        return rwaData;
    }

    public void setRwaData(String rwaData) {
        this.rwaData = rwaData;
    }

    public String getIpfsHash() {
        return ipfsHash;
    }

    public void setIpfsHash(String ipfsHash) {
        this.ipfsHash = ipfsHash;
    }

    public MetadataVerificationStatus getVerificationStatus() {
        return verificationStatus;
    }

    public void setVerificationStatus(MetadataVerificationStatus verificationStatus) {
        this.verificationStatus = verificationStatus;
    }

    public String getVerifierAddress() {
        return verifierAddress;
    }

    public void setVerifierAddress(String verifierAddress) {
        this.verifierAddress = verifierAddress;
    }

    public Instant getVerifiedAt() {
        return verifiedAt;
    }

    public void setVerifiedAt(Instant verifiedAt) {
        this.verifiedAt = verifiedAt;
    }

    public Boolean getIsFrozen() {
        return isFrozen;
    }

    public void setIsFrozen(Boolean isFrozen) {
        this.isFrozen = isFrozen;
    }

    public Instant getFrozenAt() {
        return frozenAt;
    }

    public void setFrozenAt(Instant frozenAt) {
        this.frozenAt = frozenAt;
    }

    public MediaType getMediaType() {
        return mediaType;
    }

    public void setMediaType(MediaType mediaType) {
        this.mediaType = mediaType;
    }

    public String getFileFormat() {
        return fileFormat;
    }

    public void setFileFormat(String fileFormat) {
        this.fileFormat = fileFormat;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public String getDimensions() {
        return dimensions;
    }

    public void setDimensions(String dimensions) {
        this.dimensions = dimensions;
    }

    public Integer getDuration() {
        return duration;
    }

    public void setDuration(Integer duration) {
        this.duration = duration;
    }

    public String getCreatorName() {
        return creatorName;
    }

    public void setCreatorName(String creatorName) {
        this.creatorName = creatorName;
    }

    public String getCreatorAddress() {
        return creatorAddress;
    }

    public void setCreatorAddress(String creatorAddress) {
        this.creatorAddress = creatorAddress;
    }

    public Integer getRoyaltyPercentage() {
        return royaltyPercentage;
    }

    public void setRoyaltyPercentage(Integer royaltyPercentage) {
        this.royaltyPercentage = royaltyPercentage;
    }

    public String getRoyaltyRecipient() {
        return royaltyRecipient;
    }

    public void setRoyaltyRecipient(String royaltyRecipient) {
        this.royaltyRecipient = royaltyRecipient;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public Double getRarityScore() {
        return rarityScore;
    }

    public void setRarityScore(Double rarityScore) {
        this.rarityScore = rarityScore;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public String getPreviousMetadataHash() {
        return previousMetadataHash;
    }

    public void setPreviousMetadataHash(String previousMetadataHash) {
        this.previousMetadataHash = previousMetadataHash;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Boolean getIsDeleted() {
        return isDeleted;
    }

    public void setIsDeleted(Boolean isDeleted) {
        this.isDeleted = isDeleted;
    }

    @Override
    public String toString() {
        return "TokenMetadata{" +
                "contentHash='" + contentHash + '\'' +
                ", tokenId='" + tokenId + '\'' +
                ", name='" + name + '\'' +
                ", verificationStatus=" + verificationStatus +
                ", mediaType=" + mediaType +
                ", isFrozen=" + isFrozen +
                '}';
    }
}

/**
 * Metadata Verification Status Enumeration
 */
enum MetadataVerificationStatus {
    /**
     * Metadata not yet verified
     */
    UNVERIFIED,

    /**
     * Metadata under verification
     */
    PENDING,

    /**
     * Metadata verified and approved
     */
    VERIFIED,

    /**
     * Metadata verification rejected
     */
    REJECTED,

    /**
     * Metadata flagged for review
     */
    FLAGGED
}

/**
 * Media Type Enumeration
 */
enum MediaType {
    /**
     * Static image
     */
    IMAGE,

    /**
     * Video/animation
     */
    VIDEO,

    /**
     * Audio file
     */
    AUDIO,

    /**
     * 3D model
     */
    MODEL_3D,

    /**
     * Document/PDF
     */
    DOCUMENT,

    /**
     * Other/mixed media
     */
    OTHER
}
