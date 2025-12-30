package io.aurigraph.v11.demo.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.aurigraph.v11.demo.config.*;
import io.aurigraph.v11.demo.models.NodeType;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for managing node configurations.
 *
 * This service provides:
 * - Loading configurations from JSON files
 * - Validating configuration integrity
 * - Creating default configuration templates
 * - Managing configuration lifecycle
 * - Configuration hot-reloading support
 *
 * Usage:
 * <pre>
 * {@code
 * NodeConfiguration config = configService.loadConfiguration("config/channel-node-1.json");
 * configService.validateConfiguration(config);
 * configService.saveConfiguration(config, "config/updated-config.json");
 * }
 * </pre>
 */
@ApplicationScoped
public class NodeConfigurationService {

    private static final Logger LOG = Logger.getLogger(NodeConfigurationService.class);

    @Inject
    ObjectMapper objectMapper;

    /**
     * In-memory cache of loaded configurations.
     * Key: configuration file path
     * Value: loaded configuration
     */
    private final Map<String, NodeConfiguration> configCache = new HashMap<>();

    /**
     * Load a node configuration from a JSON file.
     *
     * @param filePath path to the JSON configuration file
     * @return loaded NodeConfiguration
     * @throws IOException if file cannot be read
     * @throws IllegalArgumentException if configuration is invalid
     */
    public NodeConfiguration loadConfiguration(String filePath) throws IOException {
        LOG.infof("Loading configuration from: %s", filePath);

        // Check cache first
        if (configCache.containsKey(filePath)) {
            LOG.debugf("Using cached configuration for: %s", filePath);
            return configCache.get(filePath);
        }

        Path path = Paths.get(filePath);
        if (!Files.exists(path)) {
            throw new IOException("Configuration file not found: " + filePath);
        }

        byte[] jsonData = Files.readAllBytes(path);
        NodeConfiguration config = objectMapper.readValue(jsonData, NodeConfiguration.class);

        // Validate the configuration
        validateConfiguration(config);

        // Cache the configuration
        configCache.put(filePath, config);

        LOG.infof("Successfully loaded configuration: %s", config);
        return config;
    }

    /**
     * Load a node configuration from a JSON string.
     *
     * @param json JSON string containing configuration
     * @return loaded NodeConfiguration
     * @throws IOException if JSON cannot be parsed
     * @throws IllegalArgumentException if configuration is invalid
     */
    public NodeConfiguration loadConfigurationFromString(String json) throws IOException {
        LOG.debug("Loading configuration from JSON string");

        NodeConfiguration config = objectMapper.readValue(json, NodeConfiguration.class);
        validateConfiguration(config);

        LOG.infof("Successfully loaded configuration from string: %s", config);
        return config;
    }

    /**
     * Load a node configuration from an input stream.
     *
     * @param inputStream input stream containing JSON configuration
     * @return loaded NodeConfiguration
     * @throws IOException if stream cannot be read
     * @throws IllegalArgumentException if configuration is invalid
     */
    public NodeConfiguration loadConfiguration(InputStream inputStream) throws IOException {
        LOG.debug("Loading configuration from input stream");

        NodeConfiguration config = objectMapper.readValue(inputStream, NodeConfiguration.class);
        validateConfiguration(config);

        LOG.infof("Successfully loaded configuration from stream: %s", config);
        return config;
    }

    /**
     * Save a node configuration to a JSON file.
     *
     * @param config configuration to save
     * @param filePath path where to save the configuration
     * @throws IOException if file cannot be written
     */
    public void saveConfiguration(NodeConfiguration config, String filePath) throws IOException {
        LOG.infof("Saving configuration to: %s", filePath);

        // Validate before saving
        validateConfiguration(config);

        // Ensure parent directory exists
        Path path = Paths.get(filePath);
        Files.createDirectories(path.getParent());

        // Write configuration
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        objectMapper.writeValue(path.toFile(), config);

        // Update cache
        configCache.put(filePath, config);

        LOG.infof("Successfully saved configuration to: %s", filePath);
    }

    /**
     * Validate a node configuration.
     *
     * @param config configuration to validate
     * @throws IllegalArgumentException if validation fails
     */
    public void validateConfiguration(NodeConfiguration config) {
        LOG.debugf("Validating configuration: %s", config);

        if (config == null) {
            throw new IllegalArgumentException("Configuration cannot be null");
        }

        try {
            config.validate();
            LOG.debug("Configuration validation successful");
        } catch (IllegalArgumentException e) {
            LOG.errorf("Configuration validation failed: %s", e.getMessage());
            throw e;
        }
    }

    /**
     * Validate multiple configurations.
     *
     * @param configs list of configurations to validate
     * @return list of validation errors (empty if all valid)
     */
    public List<String> validateConfigurations(List<NodeConfiguration> configs) {
        LOG.infof("Validating %d configurations", configs.size());

        List<String> errors = new ArrayList<>();

        for (int i = 0; i < configs.size(); i++) {
            NodeConfiguration config = configs.get(i);
            try {
                validateConfiguration(config);
            } catch (IllegalArgumentException e) {
                errors.add(String.format("Config %d (%s): %s", i, config.getNodeId(), e.getMessage()));
            }
        }

        if (errors.isEmpty()) {
            LOG.info("All configurations are valid");
        } else {
            LOG.warnf("Found %d validation errors", errors.size());
        }

        return errors;
    }

    /**
     * Create a default configuration template for a specific node type.
     *
     * @param nodeType type of node
     * @param nodeId unique identifier for the node
     * @return default configuration
     */
    public NodeConfiguration createDefaultConfiguration(NodeType nodeType, String nodeId) {
        LOG.infof("Creating default configuration for type: %s, id: %s", nodeType, nodeId);

        NodeConfiguration config = switch (nodeType) {
            case CHANNEL -> new ChannelNodeConfig(nodeId);
            case VALIDATOR -> new ValidatorNodeConfig(nodeId);
            case BUSINESS -> new BusinessNodeConfig(nodeId);
            case API_INTEGRATION -> new APINodeConfig(nodeId);
        };

        LOG.infof("Created default configuration: %s", config);
        return config;
    }

    /**
     * Create and save a default configuration template.
     *
     * @param nodeType type of node
     * @param nodeId unique identifier for the node
     * @param filePath path where to save the template
     * @throws IOException if file cannot be written
     */
    public void createDefaultConfigurationFile(NodeType nodeType, String nodeId, String filePath) throws IOException {
        NodeConfiguration config = createDefaultConfiguration(nodeType, nodeId);
        saveConfiguration(config, filePath);
    }

    /**
     * Reload a configuration from disk (bypasses cache).
     *
     * @param filePath path to the configuration file
     * @return reloaded configuration
     * @throws IOException if file cannot be read
     */
    public NodeConfiguration reloadConfiguration(String filePath) throws IOException {
        LOG.infof("Reloading configuration from: %s", filePath);

        // Remove from cache to force reload
        configCache.remove(filePath);

        return loadConfiguration(filePath);
    }

    /**
     * Clear the configuration cache.
     */
    public void clearCache() {
        LOG.info("Clearing configuration cache");
        configCache.clear();
    }

    /**
     * Get the number of cached configurations.
     *
     * @return cache size
     */
    public int getCacheSize() {
        return configCache.size();
    }

    /**
     * Check if a configuration is cached.
     *
     * @param filePath path to check
     * @return true if cached, false otherwise
     */
    public boolean isCached(String filePath) {
        return configCache.containsKey(filePath);
    }

    /**
     * Convert a configuration to JSON string.
     *
     * @param config configuration to convert
     * @return JSON representation
     * @throws IOException if conversion fails
     */
    public String toJson(NodeConfiguration config) throws IOException {
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        return objectMapper.writeValueAsString(config);
    }

    /**
     * Create example configuration templates for all node types.
     *
     * @param outputDir directory where to save examples
     * @throws IOException if files cannot be written
     */
    public void createExampleConfigurations(String outputDir) throws IOException {
        LOG.infof("Creating example configurations in: %s", outputDir);

        Path dir = Paths.get(outputDir);
        Files.createDirectories(dir);

        // Channel Node example
        createDefaultConfigurationFile(
            NodeType.CHANNEL,
            "channel-node-001",
            outputDir + "/channel-node-example.json"
        );

        // Validator Node example
        createDefaultConfigurationFile(
            NodeType.VALIDATOR,
            "validator-node-001",
            outputDir + "/validator-node-example.json"
        );

        // Business Node example
        createDefaultConfigurationFile(
            NodeType.BUSINESS,
            "business-node-001",
            outputDir + "/business-node-example.json"
        );

        // API Integration Node example
        createDefaultConfigurationFile(
            NodeType.API_INTEGRATION,
            "api-node-001",
            outputDir + "/api-node-example.json"
        );

        LOG.info("Successfully created all example configurations");
    }

    /**
     * Batch load configurations from a directory.
     *
     * @param directoryPath directory containing configuration files
     * @return list of loaded configurations
     * @throws IOException if directory cannot be read
     */
    public List<NodeConfiguration> loadConfigurationsFromDirectory(String directoryPath) throws IOException {
        LOG.infof("Loading configurations from directory: %s", directoryPath);

        Path dir = Paths.get(directoryPath);
        if (!Files.exists(dir) || !Files.isDirectory(dir)) {
            throw new IOException("Directory not found or is not a directory: " + directoryPath);
        }

        List<NodeConfiguration> configs = new ArrayList<>();

        Files.walk(dir, 1)
            .filter(path -> path.toString().endsWith(".json"))
            .forEach(path -> {
                try {
                    NodeConfiguration config = loadConfiguration(path.toString());
                    configs.add(config);
                } catch (IOException e) {
                    LOG.warnf("Failed to load configuration from %s: %s", path, e.getMessage());
                }
            });

        LOG.infof("Loaded %d configurations from directory", configs.size());
        return configs;
    }

    /**
     * Get configuration summary for monitoring/debugging.
     *
     * @param config configuration to summarize
     * @return human-readable summary
     */
    public String getConfigurationSummary(NodeConfiguration config) {
        StringBuilder summary = new StringBuilder();
        summary.append("Node Configuration Summary\n");
        summary.append("==========================\n");
        summary.append(String.format("Node ID: %s\n", config.getNodeId()));
        summary.append(String.format("Node Type: %s\n", config.getNodeType()));
        summary.append(String.format("Version: %s\n", config.getVersion()));

        if (config.getNetwork() != null) {
            summary.append(String.format("Network ID: %s\n", config.getNetwork().getNetworkId()));
            summary.append(String.format("Max Peers: %d\n", config.getNetwork().getMaxPeers()));
        }

        if (config.getPerformance() != null) {
            summary.append(String.format("Thread Pool Size: %d\n", config.getPerformance().getThreadPoolSize()));
            summary.append(String.format("Queue Size: %d\n", config.getPerformance().getQueueSize()));
        }

        if (config.getSecurity() != null) {
            summary.append(String.format("TLS Enabled: %s\n", config.getSecurity().isEnableTLS()));
            summary.append(String.format("Quantum Resistant: %s\n", config.getSecurity().isQuantumResistant()));
        }

        // Type-specific details
        if (config instanceof ChannelNodeConfig channelConfig) {
            summary.append(String.format("Max Channels: %d\n", channelConfig.getMaxChannels()));
            summary.append(String.format("Max Participants/Channel: %d\n", channelConfig.getMaxParticipantsPerChannel()));
        } else if (config instanceof ValidatorNodeConfig validatorConfig) {
            summary.append(String.format("Consensus Algorithm: %s\n", validatorConfig.getConsensusAlgorithm()));
            summary.append(String.format("Block Time: %dms\n", validatorConfig.getBlockTime()));
            summary.append(String.format("Block Size: %d tx\n", validatorConfig.getBlockSize()));
        } else if (config instanceof BusinessNodeConfig businessConfig) {
            summary.append(String.format("Max Concurrent Transactions: %d\n", businessConfig.getMaxConcurrentTransactions()));
            summary.append(String.format("Workflow Engine: %s\n", businessConfig.getWorkflowEngine()));
        } else if (config instanceof APINodeConfig apiConfig) {
            summary.append(String.format("Configured APIs: %d\n", apiConfig.getApiConfigs().size()));
            summary.append(String.format("Cache Size: %s\n", apiConfig.getCacheSize()));
            summary.append(String.format("Cache TTL: %ds\n", apiConfig.getCacheTTL()));
        }

        return summary.toString();
    }
}
