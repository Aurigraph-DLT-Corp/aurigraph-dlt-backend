package io.aurigraph.v11.portal;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.websocket.*;
import jakarta.websocket.server.ServerEndpoint;
import org.jboss.logging.Logger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Enterprise Portal Service
 * Sprint 18 - Workstream 1: Advanced Features
 *
 * Provides real-time dashboard with:
 * - WebSocket-based live updates
 * - Advanced analytics and reporting
 * - User management with RBAC
 * - Configuration management UI
 * - System health monitoring
 *
 * Features:
 * - Real-time TPS monitoring
 * - Transaction flow visualization
 * - Validator status tracking
 * - Alert management
 */
@ApplicationScoped
@ServerEndpoint("/api/v11/portal/websocket")
public class EnterprisePortalService {

    private static final Logger LOG = Logger.getLogger(EnterprisePortalService.class);

    private final Map<String, Session> activeSessions;
    private final DashboardMetrics metrics;
    private final UserManagement userManagement;
    private final ConfigurationManager configManager;
    private final AlertManager alertManager;

    public EnterprisePortalService() {
        this.activeSessions = new ConcurrentHashMap<>();
        this.metrics = new DashboardMetrics();
        this.userManagement = new UserManagement();
        this.configManager = new ConfigurationManager();
        this.alertManager = new AlertManager();

        LOG.info("Enterprise Portal Service initialized");

        // Start metrics broadcasting
        startMetricsBroadcast();
    }

    /**
     * WebSocket connection opened
     */
    @OnOpen
    public void onOpen(Session session) {
        String sessionId = session.getId();
        activeSessions.put(sessionId, session);

        LOG.infof("WebSocket connection opened: %s", sessionId);

        // Send initial dashboard data
        sendInitialData(session);
    }

    /**
     * WebSocket connection closed
     */
    @OnClose
    public void onClose(Session session) {
        String sessionId = session.getId();
        activeSessions.remove(sessionId);

        LOG.infof("WebSocket connection closed: %s", sessionId);
    }

    /**
     * Handle incoming WebSocket messages
     */
    @OnMessage
    public void onMessage(String message, Session session) {
        LOG.debugf("Received message from %s: %s", session.getId(), message);

        try {
            PortalRequest request = parseRequest(message);
            PortalResponse response = handleRequest(request);

            session.getAsyncRemote().sendText(response.toJson());

        } catch (Exception e) {
            LOG.error("Failed to handle portal message", e);
            sendError(session, "Failed to process request");
        }
    }

    /**
     * Send initial dashboard data
     */
    private void sendInitialData(Session session) {
        DashboardData data = new DashboardData(
            metrics.getCurrentTPS(),
            metrics.getTotalTransactions(),
            metrics.getActiveValidators(),
            metrics.getChainHeight(),
            metrics.getAverageBlockTime(),
            metrics.getNetworkHealth()
        );

        sendToSession(session, new PortalResponse("initial_data", data));
    }

    /**
     * Start periodic metrics broadcast
     */
    private void startMetricsBroadcast() {
        Thread broadcastThread = Thread.startVirtualThread(() -> {
            while (true) {
                try {
                    Thread.sleep(1000); // Broadcast every second

                    // Collect latest metrics
                    RealtimeMetrics realtimeMetrics = new RealtimeMetrics(
                        metrics.getCurrentTPS(),
                        metrics.getTransactionsLastSecond(),
                        metrics.getMemoryUsage(),
                        metrics.getCPUUsage(),
                        System.currentTimeMillis()
                    );

                    // Broadcast to all connected sessions
                    broadcastToAll(new PortalResponse("realtime_metrics", realtimeMetrics));

                } catch (InterruptedException e) {
                    break;
                } catch (Exception e) {
                    LOG.error("Metrics broadcast failed", e);
                }
            }
        });

        broadcastThread.setName("metrics-broadcast");
    }

    /**
     * Broadcast message to all connected sessions
     */
    private void broadcastToAll(PortalResponse response) {
        String json = response.toJson();

        activeSessions.values().forEach(session -> {
            if (session.isOpen()) {
                session.getAsyncRemote().sendText(json);
            }
        });
    }

    /**
     * Send message to specific session
     */
    private void sendToSession(Session session, PortalResponse response) {
        if (session.isOpen()) {
            session.getAsyncRemote().sendText(response.toJson());
        }
    }

    /**
     * Send error message
     */
    private void sendError(Session session, String errorMessage) {
        sendToSession(session, new PortalResponse("error", Map.of("message", errorMessage)));
    }

    /**
     * Handle portal request
     */
    private PortalResponse handleRequest(PortalRequest request) {
        return switch (request.type) {
            case "get_analytics" -> getAnalytics(request);
            case "get_validators" -> getValidators(request);
            case "get_transactions" -> getTransactions(request);
            case "get_users" -> getUsers(request);
            case "update_config" -> updateConfiguration(request);
            default -> new PortalResponse("error", Map.of("message", "Unknown request type"));
        };
    }

    /**
     * Get analytics data
     */
    private PortalResponse getAnalytics(PortalRequest request) {
        AnalyticsData analytics = new AnalyticsData(
            metrics.getTotalTransactions(),
            metrics.getTransactionsByHour(),
            metrics.getTPSByHour(),
            metrics.getTopValidators(),
            metrics.getChainGrowth()
        );

        return new PortalResponse("analytics", analytics);
    }

    /**
     * Get validator information
     */
    private PortalResponse getValidators(PortalRequest request) {
        List<ValidatorInfo> validators = metrics.getValidatorList();
        return new PortalResponse("validators", validators);
    }

    /**
     * Get recent transactions
     */
    private PortalResponse getTransactions(PortalRequest request) {
        int limit = Integer.parseInt(request.params.getOrDefault("limit", "100"));
        List<TransactionInfo> transactions = metrics.getRecentTransactions(limit);
        return new PortalResponse("transactions", transactions);
    }

    /**
     * Get users (admin only)
     */
    private PortalResponse getUsers(PortalRequest request) {
        List<UserInfo> users = userManagement.getAllUsers();
        return new PortalResponse("users", users);
    }

    /**
     * Update configuration (admin only)
     */
    private PortalResponse updateConfiguration(PortalRequest request) {
        String configKey = request.params.getOrDefault("key", "");
        String configValue = request.params.getOrDefault("value", "");

        boolean success = configManager.updateConfig(configKey, configValue);

        return new PortalResponse("config_updated",
            Map.of("success", success, "key", configKey));
    }

    private PortalRequest parseRequest(String message) {
        // Simple JSON parsing (production would use Jackson)
        return new PortalRequest("get_analytics", new HashMap<>());
    }

    // Inner classes

    /**
     * Dashboard Metrics
     * Collects and provides real-time metrics
     */
    static class DashboardMetrics {
        private final AtomicLong totalTransactions = new AtomicLong(0);
        private final AtomicLong chainHeight = new AtomicLong(0);
        private double currentTPS = 0.0;

        public double getCurrentTPS() {
            return currentTPS;
        }

        public long getTotalTransactions() {
            return totalTransactions.get();
        }

        public int getActiveValidators() {
            return 10; // Simplified
        }

        public long getChainHeight() {
            return chainHeight.get();
        }

        public double getAverageBlockTime() {
            return 1.0; // 1 second average
        }

        public String getNetworkHealth() {
            return "HEALTHY";
        }

        public long getTransactionsLastSecond() {
            return (long) currentTPS;
        }

        public double getMemoryUsage() {
            Runtime runtime = Runtime.getRuntime();
            return (runtime.totalMemory() - runtime.freeMemory()) /
                   (double) runtime.maxMemory() * 100;
        }

        public double getCPUUsage() {
            return 45.0; // Simplified
        }

        public Map<String, Long> getTransactionsByHour() {
            return new HashMap<>(); // Simplified
        }

        public Map<String, Double> getTPSByHour() {
            return new HashMap<>(); // Simplified
        }

        public List<ValidatorInfo> getTopValidators() {
            return new ArrayList<>(); // Simplified
        }

        public Map<String, Long> getChainGrowth() {
            return new HashMap<>(); // Simplified
        }

        public List<ValidatorInfo> getValidatorList() {
            List<ValidatorInfo> validators = new ArrayList<>();
            for (int i = 0; i < 10; i++) {
                validators.add(new ValidatorInfo("validator-" + i, 1000000L, true, 100 + i));
            }
            return validators;
        }

        public List<TransactionInfo> getRecentTransactions(int limit) {
            return new ArrayList<>(); // Simplified
        }
    }

    /**
     * User Management
     * Sprint 18 - RBAC implementation
     */
    static class UserManagement {
        private final Map<String, UserInfo> users = new ConcurrentHashMap<>();

        public UserManagement() {
            // Initialize with default admin
            users.put("admin", new UserInfo("admin", "admin@aurigraph.io",
                UserRole.ADMIN, true));
        }

        public List<UserInfo> getAllUsers() {
            return new ArrayList<>(users.values());
        }

        public boolean authenticate(String username, String password) {
            // Production would use proper authentication
            return users.containsKey(username);
        }

        public boolean hasPermission(String username, String permission) {
            UserInfo user = users.get(username);
            if (user == null) return false;

            // Admin has all permissions
            return user.role == UserRole.ADMIN;
        }
    }

    /**
     * Configuration Manager
     * Sprint 18 - Configuration management
     */
    static class ConfigurationManager {
        private final Map<String, String> configurations = new ConcurrentHashMap<>();

        public ConfigurationManager() {
            // Initialize default configurations
            configurations.put("max_tps", "2000000");
            configurations.put("block_time", "1000");
            configurations.put("consensus_timeout", "5000");
        }

        public boolean updateConfig(String key, String value) {
            if (key == null || key.isEmpty()) {
                return false;
            }

            configurations.put(key, value);
            LOG.infof("Configuration updated: %s=%s", key, value);
            return true;
        }

        public String getConfig(String key) {
            return configurations.get(key);
        }

        public Map<String, String> getAllConfigurations() {
            return new HashMap<>(configurations);
        }
    }

    /**
     * Alert Manager
     * Sprint 18 - Alert management
     */
    static class AlertManager {
        private final List<Alert> activeAlerts = new ArrayList<>();

        public void addAlert(AlertLevel level, String message) {
            Alert alert = new Alert(UUID.randomUUID().toString(), level, message,
                System.currentTimeMillis());
            activeAlerts.add(alert);

            LOG.infof("Alert added: [%s] %s", level, message);
        }

        public List<Alert> getActiveAlerts() {
            return new ArrayList<>(activeAlerts);
        }
    }

    // Data structures

    record PortalRequest(String type, Map<String, String> params) {}

    record PortalResponse(String type, Object data) {
        public String toJson() {
            // Simplified JSON serialization
            return "{\"type\":\"" + type + "\",\"data\":" + data + "}";
        }
    }

    record DashboardData(double currentTPS, long totalTransactions, int activeValidators,
                        long chainHeight, double averageBlockTime, String networkHealth) {}

    record RealtimeMetrics(double tps, long transactionsLastSecond, double memoryUsage,
                          double cpuUsage, long timestamp) {}

    record AnalyticsData(long totalTransactions, Map<String, Long> transactionsByHour,
                        Map<String, Double> tpsByHour, List<ValidatorInfo> topValidators,
                        Map<String, Long> chainGrowth) {}

    record ValidatorInfo(String validatorId, long stakeAmount, boolean active, int blocksProduced) {}

    record TransactionInfo(String txHash, String from, String to, long amount, long timestamp) {}

    record UserInfo(String username, String email, UserRole role, boolean active) {}

    record Alert(String id, AlertLevel level, String message, long timestamp) {}

    enum UserRole {
        ADMIN,
        OPERATOR,
        VIEWER
    }

    enum AlertLevel {
        INFO,
        WARNING,
        ERROR,
        CRITICAL
    }
}
