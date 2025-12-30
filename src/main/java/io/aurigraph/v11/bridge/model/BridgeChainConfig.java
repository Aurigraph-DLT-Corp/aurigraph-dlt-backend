package io.aurigraph.v11.bridge.model;

import io.aurigraph.v11.bridge.factory.ChainFamily;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * JPA entity for blockchain chain configuration
 * Stores all configuration needed to instantiate and connect to a blockchain
 *
 * Provides:
 * - Chain identification (name, ID, RPC URL)
 * - Family classification (EVM, Solana, Cosmos, etc.)
 * - Bridge parameters (min/max amounts, fees)
 * - Contract addresses mapping
 * - Block time and confirmation requirements
 * - Timestamp tracking (created, updated)
 *
 * @author Claude Code - Priority 3 Implementation
 * @version 1.0.0
 */
@Entity
@Table(name = "bridge_chain_config", indexes = {
    @Index(name = "idx_chain_name", columnList = "chain_name", unique = true),
    @Index(name = "idx_chain_family", columnList = "chain_family"),
    @Index(name = "idx_enabled", columnList = "enabled")
})
public class BridgeChainConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Unique chain name (e.g., "ethereum", "polygon", "solana", "cosmos-hub")
     * Lowercase, hyphen-separated for consistency
     */
    @Column(name = "chain_name", nullable = false, unique = true, length = 100)
    private String chainName;

    /**
     * Chain ID (network identifier)
     * Examples: "1" for Ethereum mainnet, "137" for Polygon, etc.
     */
    @Column(name = "chain_id", nullable = false, length = 100)
    private String chainId;

    /**
     * Display name for UI/logging (e.g., "Ethereum Mainnet", "Polygon", "Solana")
     */
    @Column(name = "display_name", nullable = false, length = 255)
    private String displayName;

    /**
     * RPC endpoint URL for connecting to the chain
     * Examples:
     * - "https://eth-mainnet.g.alchemy.com/v2/key"
     * - "https://solana-api.projectserum.com"
     * - "https://rpc.cosmoshub.zone"
     */
    @Column(name = "rpc_url", nullable = false, length = 2048)
    private String rpcUrl;

    /**
     * Backup RPC URLs for failover support (semicolon-separated)
     * Used if primary RPC fails
     */
    @Column(name = "backup_rpc_urls", length = 5000)
    private String backupRpcUrls;

    /**
     * Block time in milliseconds
     * Examples: 12000 (Ethereum), 400 (Solana), 5000 (Cosmos)
     */
    @Column(name = "block_time_ms", nullable = false)
    private Long blockTime;

    /**
     * Number of block confirmations required for finality
     * Examples: 15 (Ethereum), 32 (Solana), 1 (Cosmos with IBC)
     */
    @Column(name = "confirmations_required", nullable = false)
    private Integer confirmationsRequired;

    /**
     * Chain family classification (EVM, SOLANA, COSMOS, etc.)
     */
    @Column(name = "chain_family", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private ChainFamily family;

    /**
     * Minimum bridge amount (in native token units)
     * Prevents dust transactions
     */
    @Column(name = "min_bridge_amount", nullable = false, precision = 38, scale = 18)
    private BigDecimal minBridgeAmount;

    /**
     * Maximum bridge amount (in native token units)
     * Prevents single transaction concentration
     */
    @Column(name = "max_bridge_amount", nullable = false, precision = 38, scale = 18)
    private BigDecimal maxBridgeAmount;

    /**
     * Base fee as percentage (0.001 = 0.1%)
     * Applied to all bridge transactions
     */
    @Column(name = "base_fee_percent", nullable = false, precision = 5, scale = 4)
    private BigDecimal baseFeePercent;

    /**
     * Whether this chain is currently enabled for bridging
     * Can be toggled to quickly disable a chain without deletion
     */
    @Column(name = "enabled", nullable = false)
    private Boolean enabled = true;

    /**
     * Contract addresses by key (JSON-serialized)
     * Examples keys:
     * - "htlc" -> HTLC contract address
     * - "token" -> Token contract address
     * - "bridge_gateway" -> Bridge gateway contract
     * - "oracle" -> Price oracle address
     *
     * Stored as JSON in database
     */
    @Column(name = "contract_addresses", columnDefinition = "TEXT")
    @Convert(converter = JsonStringMapConverter.class)
    private Map<String, String> contractAddresses = new HashMap<>();

    /**
     * Additional metadata (JSON-serialized)
     * Extensible field for chain-specific configuration
     * Examples:
     * - "programId" -> Solana program ID
     * - "explorerUrl" -> Block explorer URL
     * - "derivationPath" -> HD wallet derivation path
     * - "tokenDecimals" -> Native token decimal places
     */
    @Column(name = "metadata", columnDefinition = "TEXT")
    @Convert(converter = JsonStringMapConverter.class)
    private Map<String, String> metadata = new HashMap<>();

    /**
     * Timestamp when this configuration was created
     * Auto-populated by Hibernate
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    /**
     * Timestamp when this configuration was last updated
     * Auto-updated by Hibernate
     */
    @Column(name = "updated_at", nullable = false)
    @UpdateTimestamp
    private LocalDateTime updatedAt;

    /**
     * Notes about this chain configuration
     * For documentation and troubleshooting
     */
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    // ==================== Constructors ====================

    public BridgeChainConfig() {
    }

    public BridgeChainConfig(String chainName, String chainId, String displayName,
                            String rpcUrl, Long blockTime, Integer confirmationsRequired,
                            ChainFamily family, BigDecimal minBridgeAmount, BigDecimal maxBridgeAmount,
                            BigDecimal baseFeePercent) {
        this.chainName = chainName;
        this.chainId = chainId;
        this.displayName = displayName;
        this.rpcUrl = rpcUrl;
        this.blockTime = blockTime;
        this.confirmationsRequired = confirmationsRequired;
        this.family = family;
        this.minBridgeAmount = minBridgeAmount;
        this.maxBridgeAmount = maxBridgeAmount;
        this.baseFeePercent = baseFeePercent;
        this.enabled = true;
    }

    // ==================== Getters and Setters ====================

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getChainName() {
        return chainName;
    }

    public void setChainName(String chainName) {
        this.chainName = chainName;
    }

    public String getChainId() {
        return chainId;
    }

    public void setChainId(String chainId) {
        this.chainId = chainId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getRpcUrl() {
        return rpcUrl;
    }

    public void setRpcUrl(String rpcUrl) {
        this.rpcUrl = rpcUrl;
    }

    public String getBackupRpcUrls() {
        return backupRpcUrls;
    }

    public void setBackupRpcUrls(String backupRpcUrls) {
        this.backupRpcUrls = backupRpcUrls;
    }

    public Long getBlockTime() {
        return blockTime;
    }

    public void setBlockTime(Long blockTime) {
        this.blockTime = blockTime;
    }

    public Integer getConfirmationsRequired() {
        return confirmationsRequired;
    }

    public void setConfirmationsRequired(Integer confirmationsRequired) {
        this.confirmationsRequired = confirmationsRequired;
    }

    public ChainFamily getFamily() {
        return family;
    }

    public void setFamily(ChainFamily family) {
        this.family = family;
    }

    public BigDecimal getMinBridgeAmount() {
        return minBridgeAmount;
    }

    public void setMinBridgeAmount(BigDecimal minBridgeAmount) {
        this.minBridgeAmount = minBridgeAmount;
    }

    public BigDecimal getMaxBridgeAmount() {
        return maxBridgeAmount;
    }

    public void setMaxBridgeAmount(BigDecimal maxBridgeAmount) {
        this.maxBridgeAmount = maxBridgeAmount;
    }

    public BigDecimal getBaseFeePercent() {
        return baseFeePercent;
    }

    public void setBaseFeePercent(BigDecimal baseFeePercent) {
        this.baseFeePercent = baseFeePercent;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled != null && enabled;
    }

    public Map<String, String> getContractAddresses() {
        return contractAddresses;
    }

    public void setContractAddresses(Map<String, String> contractAddresses) {
        this.contractAddresses = contractAddresses;
    }

    public String getContractAddress(String key) {
        return contractAddresses.get(key);
    }

    public void putContractAddress(String key, String address) {
        this.contractAddresses.put(key, address);
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, String> metadata) {
        this.metadata = metadata;
    }

    public String getMetadata(String key) {
        return metadata.get(key);
    }

    public void putMetadata(String key, String value) {
        this.metadata.put(key, value);
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    // ==================== toString ====================

    @Override
    public String toString() {
        return "BridgeChainConfig{" +
            "id=" + id +
            ", chainName='" + chainName + '\'' +
            ", displayName='" + displayName + '\'' +
            ", family=" + family +
            ", enabled=" + enabled +
            ", createdAt=" + createdAt +
            '}';
    }
}
