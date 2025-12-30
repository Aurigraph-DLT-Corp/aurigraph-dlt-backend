package io.aurigraph.v11.bridge.factory;

import io.aurigraph.v11.bridge.ChainAdapter;
import io.aurigraph.v11.bridge.adapter.BaseChainAdapter;
import io.aurigraph.v11.bridge.exception.BridgeException;
import io.aurigraph.v11.bridge.model.BridgeChainConfig;
import io.aurigraph.v11.bridge.repository.BridgeConfigurationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Factory for creating and caching chain adapters
 * Implements the factory pattern for adapter instantiation
 * Uses chain family classification for adapter selection
 *
 * Provides:
 * - Dynamic adapter creation based on chain family
 * - Adapter caching for performance (<100µs cached lookup)
 * - Configuration loading and management
 * - Supported chain enumeration
 * - Thread-safe concurrent access (ConcurrentHashMap)
 *
 * PHASE: 3 (Week 5-8) - Chain Adapter Implementation
 * Reactive adapters with full Mutiny support
 *
 * @author Claude Code - Priority 3 Implementation
 * @version 2.0.0 - Phase 3 with reactive support
 */
@ApplicationScoped
public class ChainAdapterFactory {

    private static final Logger logger = LoggerFactory.getLogger(ChainAdapterFactory.class);

    @Inject
    private BridgeConfigurationRepository configRepository;

    // Cache adapters by chain name to avoid recreating
    // ConcurrentHashMap ensures thread-safety for concurrent access
    private final Map<String, ChainAdapter> adapterCache = new ConcurrentHashMap<>();

    // Cache configuration by chain name
    private final Map<String, BridgeChainConfig> configCache = new ConcurrentHashMap<>();

    /**
     * Get or create an adapter for a specific chain
     * Uses caching to ensure <100µs lookup time for cached adapters
     *
     * @param chainName Name of the blockchain (e.g., "ethereum", "polygon", "solana")
     * @return Initialized ChainAdapter for the specified chain
     * @throws ChainNotSupportedException if chain is not configured
     * @throws BridgeException if adapter initialization fails
     */
    public ChainAdapter getAdapter(String chainName) throws Exception {
        if (chainName == null || chainName.isEmpty()) {
            throw new IllegalArgumentException("Chain name cannot be null or empty");
        }

        String normalizedChainName = chainName.toLowerCase();

        // Check cache first - avoid repeated instantiation
        // Performance: <100µs for cached lookups
        if (adapterCache.containsKey(normalizedChainName)) {
            logger.debug("Returning cached adapter for chain: {}", normalizedChainName);
            return adapterCache.get(normalizedChainName);
        }

        // Load configuration from repository
        BridgeChainConfig config = loadChainConfiguration(normalizedChainName);

        // Create appropriate adapter based on chain family
        // Supports 7 families: EVM, SOLANA, COSMOS, SUBSTRATE, LAYER2, UTXO, OTHER
        BaseChainAdapter adapter = createAdapterForFamily(config.getFamily(), config);

        // Initialize adapter with configuration
        adapter.initialize(config);

        // Cache for future requests
        adapterCache.put(normalizedChainName, adapter);
        logger.info("Created and cached adapter for chain: {} (family: {})",
            normalizedChainName, config.getFamily().getDisplayName());

        return adapter;
    }

    /**
     * Load chain configuration from database
     * Uses cache to avoid repeated database queries
     *
     * @param chainName Name of the chain
     * @return BridgeChainConfig for the chain
     * @throws ChainNotSupportedException if chain not found in database
     */
    private BridgeChainConfig loadChainConfiguration(String chainName) throws Exception {
        // Check configuration cache first
        if (configCache.containsKey(chainName)) {
            return configCache.get(chainName);
        }

        // Load from repository
        BridgeChainConfig config = configRepository.findByChainName(chainName)
            .orElseThrow(() -> new ChainNotSupportedException(
                "Chain '" + chainName + "' is not configured. " +
                "Supported chains: " + String.join(", ", getSupportedChains())
            ));

        // Validate configuration
        validateChainConfiguration(config);

        // Cache configuration
        configCache.put(chainName, config);
        return config;
    }

    /**
     * Validate chain configuration has required fields
     */
    private void validateChainConfiguration(BridgeChainConfig config) throws BridgeException {
        if (config.getChainName() == null || config.getChainName().isEmpty()) {
            throw new BridgeException("Chain configuration missing chain name");
        }
        if (config.getChainId() == null || config.getChainId().isEmpty()) {
            throw new BridgeException("Chain configuration missing chain ID");
        }
        if (config.getRpcUrl() == null || config.getRpcUrl().isEmpty()) {
            throw new BridgeException("Chain configuration missing RPC URL");
        }
        if (config.getFamily() == null) {
            throw new BridgeException("Chain configuration missing family");
        }
    }

    /**
     * Create appropriate adapter instance for the given chain family
     * Performance target: <500µs for adapter creation
     *
     * @param family ChainFamily enum value
     * @param config Chain configuration
     * @return Instantiated BaseChainAdapter for the family
     * @throws BridgeException if adapter creation fails
     */
    private BaseChainAdapter createAdapterForFamily(ChainFamily family,
            BridgeChainConfig config) throws BridgeException {
        try {
            @SuppressWarnings("unchecked")
            Class<? extends BaseChainAdapter> adapterClass = (Class<? extends BaseChainAdapter>) family.getAdapterClass();

            if (adapterClass == null) {
                throw new BridgeException(
                    "No adapter implementation configured for family: " + family.getDisplayName() +
                    ". Adapter implementations are deferred to Week 5-8 Phase 3."
                );
            }

            // Try to instantiate the adapter class
            // Note: Quarkus/CDI will handle dependency injection via reflection
            BaseChainAdapter adapter = adapterClass.getDeclaredConstructor().newInstance();

            if (adapter == null) {
                throw new BridgeException(
                    "Failed to instantiate adapter for family: " + family.getDisplayName()
                );
            }

            logger.debug("Created adapter instance for family: {}", family.getDisplayName());
            return adapter;

        } catch (Exception e) {
            throw new BridgeException(
                "Failed to create adapter for family " + family.getDisplayName() + ": " + e.getMessage(),
                e
            );
        }
    }

    /**
     * Invalidate cached adapter for a specific chain
     * Use after updating chain configuration
     *
     * @param chainName Name of the chain
     */
    public void invalidateCache(String chainName) {
        String normalized = chainName.toLowerCase();
        adapterCache.remove(normalized);
        configCache.remove(normalized);
        logger.info("Invalidated adapter and config cache for chain: {}", normalized);
    }

    /**
     * Invalidate all cached adapters and configurations
     * Use after bulk configuration changes
     */
    public void invalidateAllCache() {
        adapterCache.clear();
        configCache.clear();
        logger.info("Invalidated all adapter and config cache");
    }

    /**
     * Get list of all supported chains
     *
     * @return List of chain names available in configuration
     */
    public List<String> getSupportedChains() {
        try {
            return configRepository.findAllChainNames();
        } catch (Exception e) {
            logger.warn("Failed to retrieve supported chains list", e);
            return new ArrayList<>();
        }
    }

    /**
     * Get list of supported chains by family
     *
     * @param family ChainFamily to filter by
     * @return List of chain names in the specified family
     */
    public List<String> getSupportedChainsByFamily(ChainFamily family) {
        try {
            return configRepository.findChainNamesByFamily(family);
        } catch (Exception e) {
            logger.warn("Failed to retrieve chains for family: {}", family.getDisplayName(), e);
            return new ArrayList<>();
        }
    }

    /**
     * Get adapter statistics for monitoring/debugging
     *
     * @return Map containing adapter cache size and configuration count
     */
    public Map<String, Integer> getStatistics() {
        Map<String, Integer> stats = new HashMap<>();
        stats.put("cached_adapters", adapterCache.size());
        stats.put("cached_configs", configCache.size());
        return stats;
    }

    /**
     * Check if a chain is supported
     *
     * @param chainName Name of the chain
     * @return true if chain is configured and supported
     */
    public boolean isChainSupported(String chainName) {
        try {
            loadChainConfiguration(chainName.toLowerCase());
            return true;
        } catch (ChainNotSupportedException e) {
            return false;
        } catch (Exception e) {
            logger.warn("Error checking if chain is supported: {}", chainName, e);
            return false;
        }
    }

    /**
     * Get chain information without creating full adapter
     *
     * @param chainName Name of the chain
     * @return BridgeChainConfig with chain details
     */
    public BridgeChainConfig getChainConfiguration(String chainName) throws Exception {
        return loadChainConfiguration(chainName.toLowerCase());
    }

    /**
     * Register a new chain configuration
     * Typically called during startup or for dynamic chain addition
     *
     * @param config New chain configuration
     * @return Registered BridgeChainConfig
     */
    public BridgeChainConfig registerChain(BridgeChainConfig config) throws Exception {
        validateChainConfiguration(config);

        BridgeChainConfig saved = configRepository.save(config);

        // Invalidate cache to pick up new configuration
        invalidateCache(config.getChainName());

        logger.info("Registered new chain: {} (family: {})",
            config.getChainName(), config.getFamily().getDisplayName());

        return saved;
    }

    /**
     * Update existing chain configuration
     *
     * @param chainName Name of the chain to update
     * @param config Updated configuration
     * @return Updated BridgeChainConfig
     */
    public BridgeChainConfig updateChain(String chainName, BridgeChainConfig config) throws Exception {
        validateChainConfiguration(config);

        BridgeChainConfig updated = configRepository.save(config);

        // Invalidate cache to pick up changes
        invalidateCache(chainName);

        logger.info("Updated chain configuration: {} (family: {})",
            chainName, config.getFamily().getDisplayName());

        return updated;
    }

    /**
     * Remove a chain from supported chains
     *
     * @param chainName Name of the chain to remove
     */
    public void removeChain(String chainName) throws Exception {
        configRepository.deleteByChainName(chainName);
        invalidateCache(chainName);
        logger.info("Removed chain from configuration: {}", chainName);
    }
}
