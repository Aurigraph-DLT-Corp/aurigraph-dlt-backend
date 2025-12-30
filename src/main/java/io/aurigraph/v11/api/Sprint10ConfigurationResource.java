package io.aurigraph.v11.api;

import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.util.*;

/**
 * Sprint 10: System Configuration REST API
 *
 * Provides configuration endpoints for:
 * - Network configuration (consensus params, node settings, network rules)
 * - System settings (portal preferences, notifications, API keys)
 *
 * Story Points: 13 (8 + 5)
 * JIRA: AV11-179 (Network Configuration) + AV11-180 (System Settings)
 *
 * @author Backend Development Agent (BDA)
 * @version 11.0.0
 * @since Sprint 10
 */
@Path("/api/v11/config")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class Sprint10ConfigurationResource {

    private static final Logger LOG = Logger.getLogger(Sprint10ConfigurationResource.class);

    /**
     * Get network configuration
     * GET /api/v11/config/network
     */
    @GET
    @Path("/network")
    public Uni<NetworkConfiguration> getNetworkConfiguration() {
        LOG.info("Fetching network configuration");

        return Uni.createFrom().item(() -> {
            NetworkConfiguration config = new NetworkConfiguration();

            // Consensus parameters
            config.consensusParams = new ConsensusParams();
            config.consensusParams.algorithm = "HyperRAFT++";
            config.consensusParams.blockTime = 500; // ms
            config.consensusParams.blockSize = 10000; // transactions
            config.consensusParams.minValidators = 4;
            config.consensusParams.maxValidators = 1000;
            config.consensusParams.quorumPercentage = 67;
            config.consensusParams.leaderElectionTimeout = 5000; // ms
            config.consensusParams.heartbeatInterval = 1000; // ms
            config.consensusParams.aiOptimizationEnabled = true;
            config.consensusParams.quantumResistant = true;

            // Node settings
            config.nodeSettings = new NodeSettings();
            config.nodeSettings.maxPeers = 100;
            config.nodeSettings.maxInboundPeers = 50;
            config.nodeSettings.maxOutboundPeers = 50;
            config.nodeSettings.peerDiscoveryEnabled = true;
            config.nodeSettings.bootstrapNodes = Arrays.asList(
                "node1.aurigraph.io:9000",
                "node2.aurigraph.io:9000",
                "node3.aurigraph.io:9000",
                "node4.aurigraph.io:9000"
            );
            config.nodeSettings.syncMode = "fast";
            config.nodeSettings.pruningEnabled = true;
            config.nodeSettings.pruningInterval = 86400; // 24 hours in seconds
            config.nodeSettings.snapshotInterval = 10000; // blocks

            // Network rules
            config.networkRules = new NetworkRules();
            config.networkRules.networkId = "aurigraph-mainnet";
            config.networkRules.chainId = 11;
            config.networkRules.minGasPrice = "1000000000"; // 1 Gwei
            config.networkRules.maxGasPerBlock = 30000000L;
            config.networkRules.minStakeAmount = "100000"; // AUR
            config.networkRules.slashingEnabled = true;
            config.networkRules.slashingPercentage = 5.0;
            config.networkRules.maxTransactionSize = 131072; // 128 KB
            config.networkRules.maxContractSize = 24576; // 24 KB
            config.networkRules.allowedContractLanguages = Arrays.asList("Solidity", "Vyper", "Rust");

            // Performance limits
            config.performanceLimits = new PerformanceLimits();
            config.performanceLimits.targetTPS = 2000000;
            config.performanceLimits.maxTPS = 3000000;
            config.performanceLimits.maxMemoryUsage = "4GB";
            config.performanceLimits.maxCPUUsage = 80; // percent
            config.performanceLimits.maxNetworkBandwidth = "1Gbps";
            config.performanceLimits.cacheSize = "2GB";

            config.lastUpdated = Instant.now().toString();
            config.version = "12.0.0";

            return config;
        });
    }

    /**
     * Update network configuration
     * PUT /api/v11/config/network
     */
    @PUT
    @Path("/network")
    public Uni<Response> updateNetworkConfiguration(NetworkConfiguration config) {
        LOG.infof("Updating network configuration");

        return Uni.createFrom().item(() -> {
            // In production, validate and persist configuration
            LOG.infof("Network configuration updated: consensus=%s, blockTime=%dms",
                    config.consensusParams.algorithm, config.consensusParams.blockTime);

            return Response.ok(Map.of(
                "status", "success",
                "message", "Network configuration updated successfully",
                "timestamp", Instant.now().toString()
            )).build();
        });
    }

    /**
     * Get system settings
     * GET /api/v11/config/settings
     */
    @GET
    @Path("/settings")
    public Uni<SystemSettings> getSystemSettings() {
        LOG.info("Fetching system settings");

        return Uni.createFrom().item(() -> {
            SystemSettings settings = new SystemSettings();

            // Portal preferences
            settings.portalPreferences = new PortalPreferences();
            settings.portalPreferences.theme = "dark";
            settings.portalPreferences.language = "en";
            settings.portalPreferences.timezone = "UTC";
            settings.portalPreferences.dateFormat = "YYYY-MM-DD HH:mm:ss";
            settings.portalPreferences.numberFormat = "en-US";
            settings.portalPreferences.chartRefreshInterval = 30; // seconds
            settings.portalPreferences.tablePageSize = 50;
            settings.portalPreferences.enableAnimations = true;
            settings.portalPreferences.enableSounds = false;
            settings.portalPreferences.compactMode = false;

            // Notification settings
            settings.notificationSettings = new NotificationSettings();
            settings.notificationSettings.emailNotifications = true;
            settings.notificationSettings.smsNotifications = false;
            settings.notificationSettings.pushNotifications = true;
            settings.notificationSettings.slackWebhook = "https://hooks.slack.com/services/xxx";
            settings.notificationSettings.discordWebhook = "";
            settings.notificationSettings.alertThresholds = new AlertThresholds();
            settings.notificationSettings.alertThresholds.tpsDropPercentage = 20.0;
            settings.notificationSettings.alertThresholds.uptimePercentage = 99.0;
            settings.notificationSettings.alertThresholds.errorRatePercentage = 1.0;
            settings.notificationSettings.alertThresholds.memoryUsagePercentage = 90.0;
            settings.notificationSettings.alertThresholds.diskUsagePercentage = 85.0;

            // API keys (mock - in production, never return actual keys)
            settings.apiKeys = new APIKeys();
            settings.apiKeys.restApiKey = "ak_prod_**********************abc123";
            settings.apiKeys.grpcApiKey = "gk_prod_**********************def456";
            settings.apiKeys.websocketKey = "ws_prod_**********************ghi789";
            settings.apiKeys.adminKey = "adm_prod_*********************jkl012";
            settings.apiKeys.readOnlyKey = "ro_prod_**********************mno345";
            settings.apiKeys.keyRotationInterval = 90; // days
            settings.apiKeys.lastRotated = "2025-09-15T00:00:00Z";
            settings.apiKeys.nextRotation = "2025-12-14T00:00:00Z";

            // Security settings
            settings.securitySettings = new SecuritySettings();
            settings.securitySettings.twoFactorEnabled = true;
            settings.securitySettings.ipWhitelist = Arrays.asList(
                "192.168.1.0/24",
                "10.0.0.0/8",
                "172.16.0.0/12"
            );
            settings.securitySettings.rateLimitPerMinute = 1000;
            settings.securitySettings.sessionTimeout = 3600; // seconds
            settings.securitySettings.passwordPolicy = "strong";
            settings.securitySettings.allowedOrigins = Arrays.asList(
                "https://portal.aurigraph.io",
                "https://dashboard.aurigraph.io"
            );

            // Logging settings
            settings.loggingSettings = new LoggingSettings();
            settings.loggingSettings.logLevel = "INFO";
            settings.loggingSettings.enableDebug = false;
            settings.loggingSettings.enableAuditLog = true;
            settings.loggingSettings.logRetentionDays = 90;
            settings.loggingSettings.logToFile = true;
            settings.loggingSettings.logToConsole = true;
            settings.loggingSettings.logToElasticsearch = true;
            settings.loggingSettings.elasticsearchUrl = "https://logs.aurigraph.io:9200";

            settings.lastUpdated = Instant.now().toString();
            settings.version = "12.0.0";

            return settings;
        });
    }

    /**
     * Update system settings
     * PUT /api/v11/config/settings
     */
    @PUT
    @Path("/settings")
    public Uni<Response> updateSystemSettings(SystemSettings settings) {
        LOG.infof("Updating system settings");

        return Uni.createFrom().item(() -> {
            // In production, validate and persist settings
            LOG.infof("System settings updated: theme=%s, notifications=%b",
                    settings.portalPreferences.theme,
                    settings.notificationSettings.emailNotifications);

            return Response.ok(Map.of(
                "status", "success",
                "message", "System settings updated successfully",
                "timestamp", Instant.now().toString()
            )).build();
        });
    }

    /**
     * Rotate API keys
     * POST /api/v11/config/settings/rotate-keys
     */
    @POST
    @Path("/settings/rotate-keys")
    public Uni<Response> rotateApiKeys() {
        LOG.info("Rotating API keys");

        return Uni.createFrom().item(() -> {
            // In production, generate new keys and invalidate old ones
            Map<String, String> newKeys = Map.of(
                "restApiKey", "ak_prod_" + UUID.randomUUID().toString().replace("-", ""),
                "grpcApiKey", "gk_prod_" + UUID.randomUUID().toString().replace("-", ""),
                "websocketKey", "ws_prod_" + UUID.randomUUID().toString().replace("-", ""),
                "rotatedAt", Instant.now().toString()
            );

            LOG.info("API keys rotated successfully");

            return Response.ok(Map.of(
                "status", "success",
                "message", "API keys rotated successfully",
                "keys", newKeys,
                "expiresIn", "90 days"
            )).build();
        });
    }

    /**
     * Get configuration history
     * GET /api/v11/config/history
     */
    @GET
    @Path("/history")
    public Uni<ConfigurationHistory> getConfigurationHistory(
            @QueryParam("limit") @DefaultValue("50") int limit) {

        LOG.infof("Fetching configuration history (limit: %d)", limit);

        return Uni.createFrom().item(() -> {
            ConfigurationHistory history = new ConfigurationHistory();
            history.changes = new ArrayList<>();

            // Mock history data
            history.changes.add(new ConfigChange(
                "2025-10-06T14:30:00Z",
                "admin@aurigraph.io",
                "network",
                "consensusParams.blockTime",
                "1000",
                "500",
                "Performance optimization"
            ));

            history.changes.add(new ConfigChange(
                "2025-10-05T10:15:00Z",
                "admin@aurigraph.io",
                "settings",
                "notificationSettings.emailNotifications",
                "false",
                "true",
                "Enable email alerts"
            ));

            history.changes.add(new ConfigChange(
                "2025-10-04T16:45:00Z",
                "devops@aurigraph.io",
                "network",
                "nodeSettings.maxPeers",
                "50",
                "100",
                "Increase network capacity"
            ));

            history.totalChanges = history.changes.size();
            history.periodStart = "2025-09-01T00:00:00Z";
            history.periodEnd = Instant.now().toString();

            return history;
        });
    }

    // DTOs

    public static class NetworkConfiguration {
        public ConsensusParams consensusParams;
        public NodeSettings nodeSettings;
        public NetworkRules networkRules;
        public PerformanceLimits performanceLimits;
        public String lastUpdated;
        public String version;
    }

    public static class ConsensusParams {
        public String algorithm;
        public int blockTime;
        public int blockSize;
        public int minValidators;
        public int maxValidators;
        public int quorumPercentage;
        public int leaderElectionTimeout;
        public int heartbeatInterval;
        public boolean aiOptimizationEnabled;
        public boolean quantumResistant;
    }

    public static class NodeSettings {
        public int maxPeers;
        public int maxInboundPeers;
        public int maxOutboundPeers;
        public boolean peerDiscoveryEnabled;
        public List<String> bootstrapNodes;
        public String syncMode;
        public boolean pruningEnabled;
        public int pruningInterval;
        public int snapshotInterval;
    }

    public static class NetworkRules {
        public String networkId;
        public int chainId;
        public String minGasPrice;
        public long maxGasPerBlock;
        public String minStakeAmount;
        public boolean slashingEnabled;
        public double slashingPercentage;
        public int maxTransactionSize;
        public int maxContractSize;
        public List<String> allowedContractLanguages;
    }

    public static class PerformanceLimits {
        public int targetTPS;
        public int maxTPS;
        public String maxMemoryUsage;
        public int maxCPUUsage;
        public String maxNetworkBandwidth;
        public String cacheSize;
    }

    public static class SystemSettings {
        public PortalPreferences portalPreferences;
        public NotificationSettings notificationSettings;
        public APIKeys apiKeys;
        public SecuritySettings securitySettings;
        public LoggingSettings loggingSettings;
        public String lastUpdated;
        public String version;
    }

    public static class PortalPreferences {
        public String theme;
        public String language;
        public String timezone;
        public String dateFormat;
        public String numberFormat;
        public int chartRefreshInterval;
        public int tablePageSize;
        public boolean enableAnimations;
        public boolean enableSounds;
        public boolean compactMode;
    }

    public static class NotificationSettings {
        public boolean emailNotifications;
        public boolean smsNotifications;
        public boolean pushNotifications;
        public String slackWebhook;
        public String discordWebhook;
        public AlertThresholds alertThresholds;
    }

    public static class AlertThresholds {
        public double tpsDropPercentage;
        public double uptimePercentage;
        public double errorRatePercentage;
        public double memoryUsagePercentage;
        public double diskUsagePercentage;
    }

    public static class APIKeys {
        public String restApiKey;
        public String grpcApiKey;
        public String websocketKey;
        public String adminKey;
        public String readOnlyKey;
        public int keyRotationInterval;
        public String lastRotated;
        public String nextRotation;
    }

    public static class SecuritySettings {
        public boolean twoFactorEnabled;
        public List<String> ipWhitelist;
        public int rateLimitPerMinute;
        public int sessionTimeout;
        public String passwordPolicy;
        public List<String> allowedOrigins;
    }

    public static class LoggingSettings {
        public String logLevel;
        public boolean enableDebug;
        public boolean enableAuditLog;
        public int logRetentionDays;
        public boolean logToFile;
        public boolean logToConsole;
        public boolean logToElasticsearch;
        public String elasticsearchUrl;
    }

    public static class ConfigurationHistory {
        public List<ConfigChange> changes;
        public int totalChanges;
        public String periodStart;
        public String periodEnd;
    }

    public static class ConfigChange {
        public String timestamp;
        public String user;
        public String section;
        public String field;
        public String oldValue;
        public String newValue;
        public String reason;

        public ConfigChange(String timestamp, String user, String section, String field,
                           String oldValue, String newValue, String reason) {
            this.timestamp = timestamp;
            this.user = user;
            this.section = section;
            this.field = field;
            this.oldValue = oldValue;
            this.newValue = newValue;
            this.reason = reason;
        }
    }
}
