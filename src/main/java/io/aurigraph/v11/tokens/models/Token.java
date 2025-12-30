package io.aurigraph.v11.tokens.models;

import io.aurigraph.v11.contracts.models.AssetType;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Token Model for Aurigraph V11 - LevelDB Compatible
 *
 * Supports standard tokens (fungible/non-fungible) and RWA tokenization.
 * Includes balance tracking, holder management, and transaction history.
 *
 * LevelDB Storage: Uses tokenId as primary key
 * JSON Serializable: All fields stored as JSON in LevelDB
 *
 * @version 4.0.0 (LevelDB Migration - Oct 8, 2025)
 * @author Aurigraph V11 Development Team
 */
public class Token {

    @JsonProperty("tokenId")
    private String tokenId;

    @JsonProperty("name")
    private String name;

    @JsonProperty("symbol")
    private String symbol;

    @JsonProperty("decimals")
    private Integer decimals = 18;

    @JsonProperty("tokenType")
    private TokenType tokenType;

    @JsonProperty("totalSupply")
    private BigDecimal totalSupply;

    @JsonProperty("circulatingSupply")
    private BigDecimal circulatingSupply;

    @JsonProperty("owner")
    private String owner;

    @JsonProperty("contractAddress")
    private String contractAddress;

    // RWA fields
    @JsonProperty("isRWA")
    private Boolean isRWA = false;

    @JsonProperty("assetType")
    private AssetType assetType;

    @JsonProperty("assetId")
    private String assetId;

    @JsonProperty("assetValue")
    private BigDecimal assetValue;

    @JsonProperty("assetCurrency")
    private String assetCurrency;

    // Token economics
    @JsonProperty("isMintable")
    private Boolean isMintable = true;

    @JsonProperty("isBurnable")
    private Boolean isBurnable = true;

    @JsonProperty("isPausable")
    private Boolean isPausable = false;

    @JsonProperty("isPaused")
    private Boolean isPaused = false;

    @JsonProperty("maxSupply")
    private BigDecimal maxSupply;

    // Compliance
    @JsonProperty("isCompliant")
    private Boolean isCompliant = false;

    @JsonProperty("complianceStandard")
    private String complianceStandard; // ERC20, ERC721, ERC1155, etc.

    @JsonProperty("kycRequired")
    private Boolean kycRequired = false;

    // Timestamps
    @JsonProperty("createdAt")
    private Instant createdAt;

    @JsonProperty("updatedAt")
    private Instant updatedAt;

    @JsonProperty("lastTransferAt")
    private Instant lastTransferAt;

    // Metrics
    @JsonProperty("transferCount")
    private Long transferCount = 0L;

    @JsonProperty("holderCount")
    private Long holderCount = 0L;

    @JsonProperty("burnedAmount")
    private BigDecimal burnedAmount = BigDecimal.ZERO;

    // Metadata
    @JsonProperty("description")
    private String description;

    @JsonProperty("iconUrl")
    private String iconUrl;

    @JsonProperty("websiteUrl")
    private String websiteUrl;

    @JsonProperty("metadata")
    private String metadata;

    // Collections
    @JsonProperty("tags")
    private List<String> tags = new ArrayList<>();

    // ==================== CONSTRUCTORS ====================

    public Token() {
        this.createdAt = Instant.now();
        this.totalSupply = BigDecimal.ZERO;
        this.circulatingSupply = BigDecimal.ZERO;
        this.burnedAmount = BigDecimal.ZERO;
    }

    public Token(String tokenId, String name, String symbol, TokenType tokenType) {
        this();
        this.tokenId = tokenId;
        this.name = name;
        this.symbol = symbol;
        this.tokenType = tokenType;
    }

    // ==================== BUSINESS LOGIC METHODS ====================

    /**
     * Ensure createdAt is set (call before first persist)
     */
    public void ensureCreatedAt() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    /**
     * Update timestamp (call before each persist after creation)
     */
    public void updateTimestamp() {
        updatedAt = Instant.now();
    }

    /**
     * Mint new tokens
     */
    public void mint(BigDecimal amount) {
        if (!isMintable) {
            throw new IllegalStateException("Token is not mintable");
        }
        if (isPaused) {
            throw new IllegalStateException("Token is paused");
        }
        if (maxSupply != null && totalSupply.add(amount).compareTo(maxSupply) > 0) {
            throw new IllegalStateException("Mint would exceed max supply");
        }

        this.totalSupply = this.totalSupply.add(amount);
        this.circulatingSupply = this.circulatingSupply.add(amount);
        this.updatedAt = Instant.now();
    }

    /**
     * Burn tokens
     */
    public void burn(BigDecimal amount) {
        if (!isBurnable) {
            throw new IllegalStateException("Token is not burnable");
        }
        if (circulatingSupply.compareTo(amount) < 0) {
            throw new IllegalStateException("Insufficient circulating supply to burn");
        }

        this.circulatingSupply = this.circulatingSupply.subtract(amount);
        this.burnedAmount = this.burnedAmount.add(amount);
        this.updatedAt = Instant.now();
    }

    /**
     * Record a transfer
     */
    public void recordTransfer() {
        this.transferCount++;
        this.lastTransferAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    /**
     * Update holder count
     */
    public void updateHolderCount(long count) {
        this.holderCount = count;
    }

    /**
     * Pause token operations
     */
    public void pause() {
        if (!isPausable) {
            throw new IllegalStateException("Token is not pausable");
        }
        this.isPaused = true;
        this.updatedAt = Instant.now();
    }

    /**
     * Unpause token operations
     */
    public void unpause() {
        this.isPaused = false;
        this.updatedAt = Instant.now();
    }

    /**
     * Add tag
     */
    public void addTag(String tag) {
        if (!this.tags.contains(tag)) {
            this.tags.add(tag);
        }
    }

    // ==================== GETTERS AND SETTERS ====================

    public String getTokenId() { return tokenId; }
    public void setTokenId(String tokenId) { this.tokenId = tokenId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }

    public Integer getDecimals() { return decimals; }
    public void setDecimals(Integer decimals) { this.decimals = decimals; }

    public TokenType getTokenType() { return tokenType; }
    public void setTokenType(TokenType tokenType) { this.tokenType = tokenType; }

    public BigDecimal getTotalSupply() { return totalSupply; }
    public void setTotalSupply(BigDecimal totalSupply) { this.totalSupply = totalSupply; }

    public BigDecimal getCirculatingSupply() { return circulatingSupply; }
    public void setCirculatingSupply(BigDecimal circulatingSupply) { this.circulatingSupply = circulatingSupply; }

    public String getOwner() { return owner; }
    public void setOwner(String owner) { this.owner = owner; }

    public String getContractAddress() { return contractAddress; }
    public void setContractAddress(String contractAddress) { this.contractAddress = contractAddress; }

    public Boolean getIsRWA() { return isRWA; }
    public void setIsRWA(Boolean isRWA) { this.isRWA = isRWA; }

    public AssetType getAssetType() { return assetType; }
    public void setAssetType(AssetType assetType) { this.assetType = assetType; }

    public String getAssetId() { return assetId; }
    public void setAssetId(String assetId) { this.assetId = assetId; }

    public BigDecimal getAssetValue() { return assetValue; }
    public void setAssetValue(BigDecimal assetValue) { this.assetValue = assetValue; }

    public String getAssetCurrency() { return assetCurrency; }
    public void setAssetCurrency(String assetCurrency) { this.assetCurrency = assetCurrency; }

    public Boolean getIsMintable() { return isMintable; }
    public void setIsMintable(Boolean isMintable) { this.isMintable = isMintable; }

    public Boolean getIsBurnable() { return isBurnable; }
    public void setIsBurnable(Boolean isBurnable) { this.isBurnable = isBurnable; }

    public Boolean getIsPausable() { return isPausable; }
    public void setIsPausable(Boolean isPausable) { this.isPausable = isPausable; }

    public Boolean getIsPaused() { return isPaused; }
    public void setIsPaused(Boolean isPaused) { this.isPaused = isPaused; }

    public BigDecimal getMaxSupply() { return maxSupply; }
    public void setMaxSupply(BigDecimal maxSupply) { this.maxSupply = maxSupply; }

    public Boolean getIsCompliant() { return isCompliant; }
    public void setIsCompliant(Boolean isCompliant) { this.isCompliant = isCompliant; }

    public String getComplianceStandard() { return complianceStandard; }
    public void setComplianceStandard(String complianceStandard) { this.complianceStandard = complianceStandard; }

    public Boolean getKycRequired() { return kycRequired; }
    public void setKycRequired(Boolean kycRequired) { this.kycRequired = kycRequired; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public Instant getLastTransferAt() { return lastTransferAt; }
    public void setLastTransferAt(Instant lastTransferAt) { this.lastTransferAt = lastTransferAt; }

    public Long getTransferCount() { return transferCount; }
    public void setTransferCount(Long transferCount) { this.transferCount = transferCount; }

    public Long getHolderCount() { return holderCount; }
    public void setHolderCount(Long holderCount) { this.holderCount = holderCount; }

    public BigDecimal getBurnedAmount() { return burnedAmount; }
    public void setBurnedAmount(BigDecimal burnedAmount) { this.burnedAmount = burnedAmount; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getIconUrl() { return iconUrl; }
    public void setIconUrl(String iconUrl) { this.iconUrl = iconUrl; }

    public String getWebsiteUrl() { return websiteUrl; }
    public void setWebsiteUrl(String websiteUrl) { this.websiteUrl = websiteUrl; }

    public String getMetadata() { return metadata; }
    public void setMetadata(String metadata) { this.metadata = metadata; }

    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }

    // ==================== ENUM DEFINITIONS ====================

    public enum TokenType {
        FUNGIBLE,           // Standard ERC20-like token
        NON_FUNGIBLE,       // NFT (ERC721-like)
        SEMI_FUNGIBLE,      // ERC1155-like
        RWA_BACKED,         // Real-world asset backed
        GOVERNANCE,         // Governance/voting token
        UTILITY,            // Utility token
        SECURITY,           // Security token (regulated)
        STABLECOIN          // Stablecoin (pegged value)
    }

    @Override
    public String toString() {
        return String.format("Token{tokenId='%s', name='%s', symbol='%s', type=%s, totalSupply=%s}",
                tokenId, name, symbol, tokenType, totalSupply);
    }
}
