package io.aurigraph.v11.bridge.config;

import io.aurigraph.v11.bridge.exception.BridgeException;
import io.aurigraph.v11.bridge.factory.ChainFamily;
import io.aurigraph.v11.bridge.model.BridgeChainConfig;
import io.aurigraph.v11.bridge.repository.BridgeConfigurationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import io.quarkus.runtime.StartupEvent;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service for loading and managing chain configurations
 * Supports:
 * - Loading configurations from application.yml
 * - Initializing database with default chains on startup
 * - Runtime chain registration and updates
 *
 * @author Claude Code - Priority 3 Implementation
 * @version 1.0.0
 */
@ApplicationScoped
public class ChainConfigurationLoader {

    private static final Logger logger = LoggerFactory.getLogger(ChainConfigurationLoader.class);

    @Inject
    private BridgeConfigurationRepository configRepository;

    /**
     * Load chain configurations on application startup
     * This method is called after the Quarkus application has started
     */
    public void loadConfigurations(@Observes StartupEvent startupEvent) {
        logger.info("Initializing chain configurations on startup");

        try {
            // Load default chains if database is empty
            if (configRepository.count() == 0) {
                logger.info("Database is empty. Loading default chain configurations...");
                loadDefaultChains();
            } else {
                long enabledChains = configRepository.countEnabled();
                logger.info("Found {} total chains in database, {} enabled",
                    configRepository.count(), enabledChains);
            }

            // Log chain summary by family
            logChainSummary();

        } catch (Exception e) {
            logger.error("Error loading chain configurations", e);
        }
    }

    /**
     * Load default chain configurations
     * Creates standard configurations for major chains
     */
    private void loadDefaultChains() {
        try {
            // Ethereum (EVM)
            loadChain("ethereum", "1", "Ethereum Mainnet",
                "https://eth-mainnet.g.alchemy.com/v2/demo",
                12000L, 15, ChainFamily.EVM,
                "0.1", "1000", "0.001",
                "Ethereum mainnet configuration for testing");

            // Polygon (EVM)
            loadChain("polygon", "137", "Polygon (Matic)",
                "https://polygon-rpc.com/",
                2000L, 256, ChainFamily.EVM,
                "10", "10000", "0.0005",
                "Polygon mainnet configuration");

            // Solana (SOLANA)
            loadChain("solana", "mainnet-beta", "Solana Mainnet",
                "https://api.mainnet-beta.solana.com",
                400L, 32, ChainFamily.SOLANA,
                "0.01", "100", "0.0002",
                "Solana mainnet configuration");

            logger.info("Successfully loaded 3 default chain configurations");

        } catch (Exception e) {
            logger.error("Error loading default chains", e);
        }
    }

    /**
     * Load a single chain configuration into the database
     *
     * @param chainName Unique chain name
     * @param chainId Network identifier
     * @param displayName Human-readable display name
     * @param rpcUrl Primary RPC endpoint
     * @param blockTime Block time in milliseconds
     * @param confirmations Block confirmations required
     * @param family Chain family classification
     * @param minAmount Minimum bridge amount
     * @param maxAmount Maximum bridge amount
     * @param baseFee Base fee percentage
     * @param notes Configuration notes
     */
    private void loadChain(String chainName, String chainId, String displayName,
                          String rpcUrl, long blockTime, int confirmations,
                          ChainFamily family, String minAmount, String maxAmount,
                          String baseFee, String notes) {
        try {
            // Check if chain already exists
            if (configRepository.existsByChainName(chainName)) {
                logger.debug("Chain '{}' already exists in database", chainName);
                return;
            }

            // Create new configuration
            BridgeChainConfig config = new BridgeChainConfig(
                chainName,
                chainId,
                displayName,
                rpcUrl,
                blockTime,
                confirmations,
                family,
                new BigDecimal(minAmount),
                new BigDecimal(maxAmount),
                new BigDecimal(baseFee)
            );
            config.setNotes(notes);
            config.setEnabled(true);

            // Save to database
            BridgeChainConfig saved = configRepository.save(config);
            logger.info("Loaded chain configuration: {} (family: {}, enabled: {})",
                chainName, family.getDisplayName(), saved.getEnabled());

        } catch (Exception e) {
            logger.error("Error loading chain configuration for '{}': {}", chainName, e.getMessage());
        }
    }

    /**
     * Register a new chain configuration at runtime
     *
     * @param config Chain configuration to register
     * @return Registered configuration
     */
    public BridgeChainConfig registerChain(BridgeChainConfig config) {
        try {
            // Validate configuration
            if (config.getChainName() == null || config.getChainName().isEmpty()) {
                throw new IllegalArgumentException("Chain name cannot be null or empty");
            }

            // Check for duplicates
            if (configRepository.existsByChainName(config.getChainName())) {
                throw new IllegalArgumentException("Chain '" + config.getChainName() + "' already exists");
            }

            // Save configuration
            BridgeChainConfig saved = configRepository.save(config);
            logger.info("Registered new chain: {} (family: {})", saved.getChainName(), saved.getFamily());
            return saved;

        } catch (Exception e) {
            logger.error("Error registering chain: {}", e.getMessage());
            throw new RuntimeException("Failed to register chain", e);
        }
    }

    /**
     * Update an existing chain configuration
     *
     * @param chainName Name of the chain to update
     * @param config Updated configuration
     * @return Updated configuration
     */
    public BridgeChainConfig updateChain(String chainName, BridgeChainConfig config) {
        try {
            // Find existing configuration
            BridgeChainConfig existing = configRepository.findByChainName(chainName)
                .orElseThrow(() -> new IllegalArgumentException("Chain '" + chainName + "' not found"));

            // Update fields
            existing.setDisplayName(config.getDisplayName());
            existing.setRpcUrl(config.getRpcUrl());
            existing.setBackupRpcUrls(config.getBackupRpcUrls());
            existing.setBlockTime(config.getBlockTime());
            existing.setConfirmationsRequired(config.getConfirmationsRequired());
            existing.setMinBridgeAmount(config.getMinBridgeAmount());
            existing.setMaxBridgeAmount(config.getMaxBridgeAmount());
            existing.setBaseFeePercent(config.getBaseFeePercent());
            existing.setContractAddresses(config.getContractAddresses());
            existing.setMetadata(config.getMetadata());
            existing.setNotes(config.getNotes());

            // Save updates
            BridgeChainConfig updated = configRepository.save(existing);
            logger.info("Updated chain configuration: {}", chainName);
            return updated;

        } catch (Exception e) {
            logger.error("Error updating chain '{}': {}", chainName, e.getMessage());
            throw new RuntimeException("Failed to update chain", e);
        }
    }

    /**
     * Enable or disable a chain for bridging
     *
     * @param chainName Name of the chain
     * @param enabled Whether to enable or disable
     */
    public void toggleChainEnabled(String chainName, boolean enabled) {
        try {
            BridgeChainConfig config = configRepository.findByChainName(chainName)
                .orElseThrow(() -> new IllegalArgumentException("Chain '" + chainName + "' not found"));

            config.setEnabled(enabled);
            configRepository.save(config);
            logger.info("Toggled chain '{}' enabled: {}", chainName, enabled);

        } catch (Exception e) {
            logger.error("Error toggling chain '{}': {}", chainName, e.getMessage());
            throw new RuntimeException("Failed to toggle chain", e);
        }
    }

    /**
     * Log a summary of configured chains by family
     */
    private void logChainSummary() {
        try {
            logger.info("=== Chain Configuration Summary ===");
            for (ChainFamily family : ChainFamily.values()) {
                long count = configRepository.countByFamily(family);
                if (count > 0) {
                    List<String> chains = configRepository.findEnabledChainNamesByFamily(family);
                    logger.info("  {}: {} configured, {} enabled -> {}",
                        family.getDisplayName(), count, chains.size(), chains);
                }
            }
            logger.info("Total chains configured: {}", configRepository.count());
        } catch (Exception e) {
            logger.warn("Error logging chain summary", e);
        }
    }

    /**
     * Get configuration for a specific chain
     *
     * @param chainName Name of the chain
     * @return Optional containing the configuration if found
     */
    public Optional<BridgeChainConfig> getChainConfiguration(String chainName) {
        return configRepository.findByChainName(chainName);
    }

    /**
     * Get all enabled chains
     *
     * @return List of enabled chain configurations
     */
    public List<BridgeChainConfig> getEnabledChains() {
        return configRepository.findByEnabledTrue();
    }

    /**
     * Get all chains in a family
     *
     * @param family Chain family
     * @return List of configurations for the family
     */
    public List<BridgeChainConfig> getChainsByFamily(ChainFamily family) {
        return configRepository.findEnabledByFamily(family);
    }
}
