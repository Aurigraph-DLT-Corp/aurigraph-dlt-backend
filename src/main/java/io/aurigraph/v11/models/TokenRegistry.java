package io.aurigraph.v11.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Token Registry Model for Aurigraph V11 - LevelDB Compatible
 *
 * Central registry for all tokens created on the Aurigraph V11 platform.
 * Supports ERC20, ERC721, and ERC1155 token standards with RWA integration.
 *
 * Features:
 * - Multi-standard token support (ERC20/721/1155)
 * - Real-world asset (RWA) tokenization tracking
 * - IPFS metadata storage
 * - Deployment transaction tracking
 * - Supply and circulation management
 *
 * LevelDB Storage: Uses tokenAddress as primary key
 * JSON Serializable: All fields stored as JSON in LevelDB
 *
 * @version 4.0.0 (LevelDB Migration - Oct 8, 2025)
 * @since Sprint 12 - AV11-058: Token & RWA APIs
 */
public class TokenRegistry {

    /**
     * Unique token address on Aurigraph network - PRIMARY KEY for LevelDB
     * Format: 0x[64 hex chars] (SHA3-256 hash)
     */
    @JsonProperty("tokenAddress")
    private String tokenAddress;

    /**
     * Token standard type
     * ERC20: Fungible tokens
     * ERC721: Non-fungible tokens (NFTs)
     * ERC1155: Multi-token standard
     */
    @JsonProperty("tokenType")
    private TokenType tokenType;

    /**
     * Human-readable token name
     * Example: "Aurigraph Wrapped Gold", "Carbon Credit Token"
     */
    @JsonProperty("name")
    private String name;

    /**
     * Token ticker symbol
     * Example: "wAUG", "CCT", "RWA-GOLD"
     */
    @JsonProperty("symbol")
    private String symbol;

    /**
     * Decimal precision for token amounts
     * Standard: 18 for ERC20, 0 for ERC721, variable for ERC1155
     */
    @JsonProperty("decimals")
    private Integer decimals = 18;

    /**
     * Total supply of tokens
     * For ERC721: number of minted NFTs
     * For ERC20: total fungible tokens
     * For ERC1155: sum of all token types
     */
    @JsonProperty("totalSupply")
    private BigDecimal totalSupply = BigDecimal.ZERO;

    /**
     * Circulating supply (total supply minus burned/locked tokens)
     */
    @JsonProperty("circulatingSupply")
    private BigDecimal circulatingSupply = BigDecimal.ZERO;

    /**
     * Smart contract address if deployed via contract factory
     */
    @JsonProperty("contractAddress")
    private String contractAddress;

    /**
     * Transaction hash of the deployment transaction
     */
    @JsonProperty("deploymentTxHash")
    private String deploymentTxHash;

    /**
     * Block number where token was deployed
     */
    @JsonProperty("deploymentBlock")
    private Long deploymentBlock;

    /**
     * Flag indicating if this token represents a Real-World Asset
     */
    @JsonProperty("isRWA")
    private Boolean isRWA = false;

    /**
     * Reference to RWA asset ID (if isRWA = true)
     * Links to external RWA tokenization system
     */
    @JsonProperty("rwaAssetId")
    private String rwaAssetId;

    /**
     * Token metadata in JSON format
     * Contains custom properties, descriptions, images, etc.
     */
    @JsonProperty("metadata")
    private String metadata;

    /**
     * IPFS hash for metadata storage
     * Used for decentralized metadata hosting
     */
    @JsonProperty("ipfsHash")
    private String ipfsHash;

    /**
     * Token creator/deployer address
     */
    @JsonProperty("creatorAddress")
    private String creatorAddress;

    /**
     * Current token owner/controller address (for admin functions)
     */
    @JsonProperty("ownerAddress")
    private String ownerAddress;

    /**
     * Flag indicating if token is mintable after creation
     */
    @JsonProperty("isMintable")
    private Boolean isMintable = false;

    /**
     * Flag indicating if token is burnable
     */
    @JsonProperty("isBurnable")
    private Boolean isBurnable = false;

    /**
     * Flag indicating if token is pausable
     */
    @JsonProperty("isPausable")
    private Boolean isPausable = false;

    /**
     * Current pause state
     */
    @JsonProperty("isPaused")
    private Boolean isPaused = false;

    /**
     * Token verification status
     */
    @JsonProperty("verificationStatus")
    private VerificationStatus verificationStatus = VerificationStatus.PENDING;

    /**
     * Token listing status (for DEX/exchanges)
     */
    @JsonProperty("listingStatus")
    private ListingStatus listingStatus = ListingStatus.UNLISTED;

    /**
     * Current market price in AUR (if listed)
     */
    @JsonProperty("marketPrice")
    private BigDecimal marketPrice;

    /**
     * 24-hour trading volume
     */
    @JsonProperty("volume24h")
    private BigDecimal volume24h = BigDecimal.ZERO;

    /**
     * Market capitalization (total supply * market price)
     */
    @JsonProperty("marketCap")
    private BigDecimal marketCap;

    /**
     * Total number of unique holders
     */
    @JsonProperty("holderCount")
    private Long holderCount = 0L;

    /**
     * Total number of transfers
     */
    @JsonProperty("transferCount")
    private Long transferCount = 0L;

    /**
     * Website URL for token project
     */
    @JsonProperty("websiteUrl")
    private String websiteUrl;

    /**
     * Social media links (JSON array)
     */
    @JsonProperty("socialLinks")
    private String socialLinks;

    /**
     * Audit report URL/hash
     */
    @JsonProperty("auditReport")
    private String auditReport;

    /**
     * Compliance certifications (JSON array)
     */
    @JsonProperty("complianceCerts")
    private String complianceCerts;

    /**
     * Token category/tags (comma-separated)
     * Examples: "DeFi,Stablecoin", "NFT,Gaming", "RWA,Carbon"
     */
    @JsonProperty("categories")
    private String categories;

    /**
     * Risk score (1-10, calculated by AI)
     * 1 = Very Low Risk, 10 = Very High Risk
     */
    @JsonProperty("riskScore")
    private Integer riskScore = 5;

    /**
     * Liquidity score (0-100, calculated by AI)
     * 0 = No liquidity, 100 = Highly liquid
     */
    @JsonProperty("liquidityScore")
    private Double liquidityScore = 0.0;

    /**
     * Last price update timestamp
     */
    @JsonProperty("lastPriceUpdate")
    private Instant lastPriceUpdate;

    /**
     * Token creation timestamp
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

    /**
     * Deletion timestamp
     */
    @JsonProperty("deletedAt")
    private Instant deletedAt;

    // Lifecycle methods (converted from JPA @PrePersist/@PreUpdate)

    /**
     * Initialize timestamps and token address (call before first save)
     */
    public void ensureCreatedAt() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (updatedAt == null) {
            updatedAt = Instant.now();
        }

        // Auto-generate token address if not set
        if (tokenAddress == null) {
            tokenAddress = generateTokenAddress();
        }
    }

    /**
     * Update timestamp and auto-calculate market cap (call before each save)
     */
    public void updateTimestamp() {
        updatedAt = Instant.now();

        // Auto-calculate market cap if price is available
        if (marketPrice != null && totalSupply != null) {
            marketCap = marketPrice.multiply(totalSupply);
        }
    }

    // Constructors

    public TokenRegistry() {
    }

    public TokenRegistry(String name, String symbol, TokenType tokenType, String creatorAddress) {
        this.name = name;
        this.symbol = symbol;
        this.tokenType = tokenType;
        this.creatorAddress = creatorAddress;
        this.ownerAddress = creatorAddress;
        this.verificationStatus = VerificationStatus.PENDING;
        this.listingStatus = ListingStatus.UNLISTED;
    }

    // Helper methods

    /**
     * Generate unique token address using UUID
     */
    private String generateTokenAddress() {
        return "0x" + UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * Calculate market capitalization
     */
    public BigDecimal calculateMarketCap() {
        if (marketPrice != null && circulatingSupply != null) {
            return marketPrice.multiply(circulatingSupply);
        }
        return BigDecimal.ZERO;
    }

    /**
     * Check if token is active and tradeable
     */
    public boolean isTradeable() {
        return verificationStatus == VerificationStatus.VERIFIED
            && listingStatus == ListingStatus.LISTED
            && !isPaused
            && !isDeleted;
    }

    /**
     * Check if token is an RWA token
     */
    public boolean isRealWorldAsset() {
        return isRWA != null && isRWA && rwaAssetId != null;
    }

    /**
     * Increment holder count
     */
    public void incrementHolderCount() {
        this.holderCount = (this.holderCount != null ? this.holderCount : 0L) + 1;
    }

    /**
     * Decrement holder count
     */
    public void decrementHolderCount() {
        this.holderCount = Math.max(0, (this.holderCount != null ? this.holderCount : 0L) - 1);
    }

    /**
     * Increment transfer count
     */
    public void incrementTransferCount() {
        this.transferCount = (this.transferCount != null ? this.transferCount : 0L) + 1;
    }

    /**
     * Update market price and volume
     */
    public void updateMarketData(BigDecimal newPrice, BigDecimal tradingVolume) {
        this.marketPrice = newPrice;
        this.volume24h = tradingVolume;
        this.lastPriceUpdate = Instant.now();
        this.marketCap = calculateMarketCap();
    }

    /**
     * Mint new tokens (increase total supply)
     */
    public void mint(BigDecimal amount) {
        if (!isMintable) {
            throw new IllegalStateException("Token is not mintable");
        }
        this.totalSupply = this.totalSupply.add(amount);
        this.circulatingSupply = this.circulatingSupply.add(amount);
    }

    /**
     * Burn tokens (decrease total supply)
     */
    public void burn(BigDecimal amount) {
        if (!isBurnable) {
            throw new IllegalStateException("Token is not burnable");
        }
        if (this.totalSupply.compareTo(amount) < 0) {
            throw new IllegalArgumentException("Burn amount exceeds total supply");
        }
        this.totalSupply = this.totalSupply.subtract(amount);
        this.circulatingSupply = this.circulatingSupply.subtract(amount);
    }

    /**
     * Pause token transfers
     */
    public void pause() {
        if (!isPausable) {
            throw new IllegalStateException("Token is not pausable");
        }
        this.isPaused = true;
    }

    /**
     * Unpause token transfers
     */
    public void unpause() {
        if (!isPausable) {
            throw new IllegalStateException("Token is not pausable");
        }
        this.isPaused = false;
    }

    /**
     * Verify token
     */
    public void verify() {
        this.verificationStatus = VerificationStatus.VERIFIED;
    }

    /**
     * List token on exchange
     */
    public void list() {
        if (verificationStatus != VerificationStatus.VERIFIED) {
            throw new IllegalStateException("Token must be verified before listing");
        }
        this.listingStatus = ListingStatus.LISTED;
    }

    /**
     * Soft delete token
     */
    public void softDelete() {
        this.isDeleted = true;
        this.deletedAt = Instant.now();
    }

    // Getters and Setters

    public String getTokenAddress() {
        return tokenAddress;
    }

    public void setTokenAddress(String tokenAddress) {
        this.tokenAddress = tokenAddress;
    }

    public TokenType getTokenType() {
        return tokenType;
    }

    public void setTokenType(TokenType tokenType) {
        this.tokenType = tokenType;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public Integer getDecimals() {
        return decimals;
    }

    public void setDecimals(Integer decimals) {
        this.decimals = decimals;
    }

    public BigDecimal getTotalSupply() {
        return totalSupply;
    }

    public void setTotalSupply(BigDecimal totalSupply) {
        this.totalSupply = totalSupply;
    }

    public BigDecimal getCirculatingSupply() {
        return circulatingSupply;
    }

    public void setCirculatingSupply(BigDecimal circulatingSupply) {
        this.circulatingSupply = circulatingSupply;
    }

    public String getContractAddress() {
        return contractAddress;
    }

    public void setContractAddress(String contractAddress) {
        this.contractAddress = contractAddress;
    }

    public String getDeploymentTxHash() {
        return deploymentTxHash;
    }

    public void setDeploymentTxHash(String deploymentTxHash) {
        this.deploymentTxHash = deploymentTxHash;
    }

    public Long getDeploymentBlock() {
        return deploymentBlock;
    }

    public void setDeploymentBlock(Long deploymentBlock) {
        this.deploymentBlock = deploymentBlock;
    }

    public Boolean getIsRWA() {
        return isRWA;
    }

    public void setIsRWA(Boolean isRWA) {
        this.isRWA = isRWA;
    }

    public String getRwaAssetId() {
        return rwaAssetId;
    }

    public void setRwaAssetId(String rwaAssetId) {
        this.rwaAssetId = rwaAssetId;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }

    public String getIpfsHash() {
        return ipfsHash;
    }

    public void setIpfsHash(String ipfsHash) {
        this.ipfsHash = ipfsHash;
    }

    public String getCreatorAddress() {
        return creatorAddress;
    }

    public void setCreatorAddress(String creatorAddress) {
        this.creatorAddress = creatorAddress;
    }

    public String getOwnerAddress() {
        return ownerAddress;
    }

    public void setOwnerAddress(String ownerAddress) {
        this.ownerAddress = ownerAddress;
    }

    public Boolean getIsMintable() {
        return isMintable;
    }

    public void setIsMintable(Boolean isMintable) {
        this.isMintable = isMintable;
    }

    public Boolean getIsBurnable() {
        return isBurnable;
    }

    public void setIsBurnable(Boolean isBurnable) {
        this.isBurnable = isBurnable;
    }

    public Boolean getIsPausable() {
        return isPausable;
    }

    public void setIsPausable(Boolean isPausable) {
        this.isPausable = isPausable;
    }

    public Boolean getIsPaused() {
        return isPaused;
    }

    public void setIsPaused(Boolean isPaused) {
        this.isPaused = isPaused;
    }

    public VerificationStatus getVerificationStatus() {
        return verificationStatus;
    }

    public void setVerificationStatus(VerificationStatus verificationStatus) {
        this.verificationStatus = verificationStatus;
    }

    public ListingStatus getListingStatus() {
        return listingStatus;
    }

    public void setListingStatus(ListingStatus listingStatus) {
        this.listingStatus = listingStatus;
    }

    public BigDecimal getMarketPrice() {
        return marketPrice;
    }

    public void setMarketPrice(BigDecimal marketPrice) {
        this.marketPrice = marketPrice;
    }

    public BigDecimal getVolume24h() {
        return volume24h;
    }

    public void setVolume24h(BigDecimal volume24h) {
        this.volume24h = volume24h;
    }

    public BigDecimal getMarketCap() {
        return marketCap;
    }

    public void setMarketCap(BigDecimal marketCap) {
        this.marketCap = marketCap;
    }

    public Long getHolderCount() {
        return holderCount;
    }

    public void setHolderCount(Long holderCount) {
        this.holderCount = holderCount;
    }

    public Long getTransferCount() {
        return transferCount;
    }

    public void setTransferCount(Long transferCount) {
        this.transferCount = transferCount;
    }

    public String getWebsiteUrl() {
        return websiteUrl;
    }

    public void setWebsiteUrl(String websiteUrl) {
        this.websiteUrl = websiteUrl;
    }

    public String getSocialLinks() {
        return socialLinks;
    }

    public void setSocialLinks(String socialLinks) {
        this.socialLinks = socialLinks;
    }

    public String getAuditReport() {
        return auditReport;
    }

    public void setAuditReport(String auditReport) {
        this.auditReport = auditReport;
    }

    public String getComplianceCerts() {
        return complianceCerts;
    }

    public void setComplianceCerts(String complianceCerts) {
        this.complianceCerts = complianceCerts;
    }

    public String getCategories() {
        return categories;
    }

    public void setCategories(String categories) {
        this.categories = categories;
    }

    public Integer getRiskScore() {
        return riskScore;
    }

    public void setRiskScore(Integer riskScore) {
        this.riskScore = riskScore;
    }

    public Double getLiquidityScore() {
        return liquidityScore;
    }

    public void setLiquidityScore(Double liquidityScore) {
        this.liquidityScore = liquidityScore;
    }

    public Instant getLastPriceUpdate() {
        return lastPriceUpdate;
    }

    public void setLastPriceUpdate(Instant lastPriceUpdate) {
        this.lastPriceUpdate = lastPriceUpdate;
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

    public Instant getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(Instant deletedAt) {
        this.deletedAt = deletedAt;
    }

    @Override
    public String toString() {
        return "TokenRegistry{" +
                "tokenAddress='" + tokenAddress + '\'' +
                ", tokenType=" + tokenType +
                ", name='" + name + '\'' +
                ", symbol='" + symbol + '\'' +
                ", totalSupply=" + totalSupply +
                ", verificationStatus=" + verificationStatus +
                ", listingStatus=" + listingStatus +
                ", isRWA=" + isRWA +
                '}';
    }
}

/**
 * Verification Status Enumeration
 */
enum VerificationStatus {
    /**
     * Token verification pending
     */
    PENDING,

    /**
     * Token under review
     */
    IN_REVIEW,

    /**
     * Token verified and approved
     */
    VERIFIED,

    /**
     * Token verification rejected
     */
    REJECTED,

    /**
     * Token suspended due to compliance issues
     */
    SUSPENDED
}

/**
 * Listing Status Enumeration
 */
enum ListingStatus {
    /**
     * Token not listed on any exchange
     */
    UNLISTED,

    /**
     * Token listed on exchange(s)
     */
    LISTED,

    /**
     * Token listing suspended
     */
    DELISTED
}

/**
 * Token Type Enumeration
 */
enum TokenType {
    /**
     * ERC20 - Fungible tokens
     */
    ERC20,

    /**
     * ERC721 - Non-fungible tokens (NFTs)
     */
    ERC721,

    /**
     * ERC1155 - Multi-token standard
     */
    ERC1155
}
