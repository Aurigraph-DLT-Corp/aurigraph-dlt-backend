package io.aurigraph.v11.logging;

import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;
import org.jboss.logging.MDC;

import java.util.Map;
import java.util.UUID;
import java.util.HashMap;

/**
 * Structured Logging Service for Aurigraph V11
 *
 * Provides centralized logging with structured fields for ELK stack integration.
 * All logs are emitted in JSON format with correlation IDs, context data, and MDC support.
 *
 * Usage:
 * <pre>
 * @Inject
 * LoggingService loggingService;
 *
 * loggingService.logTransaction("tx123", "SUBMITTED", Map.of("from", "alice", "to", "bob", "amount", 100.0));
 * loggingService.logPerformance("submitTx", 15.5, 65000.0);
 * loggingService.logError("Transaction failed", exception, Map.of("txId", "tx123"));
 * </pre>
 */
@ApplicationScoped
public class LoggingService {

    private static final Logger LOG = Logger.getLogger(LoggingService.class);

    // Log categories
    private static final String CATEGORY_TRANSACTION = "TRANSACTION";
    private static final String CATEGORY_CONSENSUS = "CONSENSUS";
    private static final String CATEGORY_CRYPTO = "CRYPTO";
    private static final String CATEGORY_BRIDGE = "BRIDGE";
    private static final String CATEGORY_AI = "AI_OPTIMIZATION";
    private static final String CATEGORY_PERFORMANCE = "PERFORMANCE";
    private static final String CATEGORY_SECURITY = "SECURITY";
    private static final String CATEGORY_HTTP = "HTTP";
    private static final String CATEGORY_ERROR = "ERROR";
    private static final String CATEGORY_SYSTEM = "SYSTEM";

    /**
     * Log a transaction event with structured data
     */
    public void logTransaction(String txId, String status, Map<String, Object> metadata) {
        Map<String, Object> logData = new HashMap<>(metadata);
        logData.put("category", CATEGORY_TRANSACTION);
        logData.put("txId", txId);
        logData.put("status", status);
        logData.put("timestamp", System.currentTimeMillis());

        setMDC(logData);
        LOG.infof("Transaction %s: %s", txId, status);
        clearMDC();
    }

    /**
     * Log consensus operation with details
     */
    public void logConsensus(String operation, String nodeId, long term, Map<String, Object> metadata) {
        Map<String, Object> logData = new HashMap<>(metadata);
        logData.put("category", CATEGORY_CONSENSUS);
        logData.put("operation", operation);
        logData.put("nodeId", nodeId);
        logData.put("term", term);
        logData.put("timestamp", System.currentTimeMillis());

        setMDC(logData);
        LOG.infof("Consensus [%s]: %s (term=%d)", nodeId, operation, term);
        clearMDC();
    }

    /**
     * Log cryptographic operation with timing
     */
    public void logCrypto(String operation, String algorithm, long durationNs, boolean success) {
        Map<String, Object> logData = new HashMap<>();
        logData.put("category", CATEGORY_CRYPTO);
        logData.put("operation", operation);
        logData.put("algorithm", algorithm);
        logData.put("durationMs", durationNs / 1_000_000.0);
        logData.put("success", success);
        logData.put("timestamp", System.currentTimeMillis());

        setMDC(logData);
        LOG.infof("Crypto %s using %s: %s (%.2fms)", operation, algorithm,
                 success ? "SUCCESS" : "FAILED", durationNs / 1_000_000.0);
        clearMDC();
    }

    /**
     * Log cross-chain bridge operation
     */
    public void logBridge(String bridgeId, String sourceChain, String destChain,
                         String operation, Map<String, Object> metadata) {
        Map<String, Object> logData = new HashMap<>(metadata);
        logData.put("category", CATEGORY_BRIDGE);
        logData.put("bridgeId", bridgeId);
        logData.put("sourceChain", sourceChain);
        logData.put("destChain", destChain);
        logData.put("operation", operation);
        logData.put("timestamp", System.currentTimeMillis());

        setMDC(logData);
        LOG.infof("Bridge [%s]: %s -> %s (%s)", bridgeId, sourceChain, destChain, operation);
        clearMDC();
    }

    /**
     * Log AI optimization event
     */
    public void logAIOptimization(String component, double metric, String action, Map<String, Object> metadata) {
        Map<String, Object> logData = new HashMap<>(metadata);
        logData.put("category", CATEGORY_AI);
        logData.put("component", component);
        logData.put("metric", metric);
        logData.put("action", action);
        logData.put("timestamp", System.currentTimeMillis());

        setMDC(logData);
        LOG.infof("AI Optimization [%s]: metric=%.2f, action=%s", component, metric, action);
        clearMDC();
    }

    /**
     * Log performance metrics
     */
    public void logPerformance(String operation, double durationMs, double tps) {
        Map<String, Object> logData = new HashMap<>();
        logData.put("category", CATEGORY_PERFORMANCE);
        logData.put("operation", operation);
        logData.put("durationMs", durationMs);
        logData.put("tps", tps);
        logData.put("timestamp", System.currentTimeMillis());

        setMDC(logData);
        LOG.infof("Performance [%s]: duration=%.2fms, tps=%.0f", operation, durationMs, tps);
        clearMDC();
    }

    /**
     * Log security event (authentication, authorization, suspicious activity)
     */
    public void logSecurity(String eventType, String userId, String action,
                           boolean success, Map<String, Object> metadata) {
        Map<String, Object> logData = new HashMap<>(metadata);
        logData.put("category", CATEGORY_SECURITY);
        logData.put("eventType", eventType);
        logData.put("userId", userId);
        logData.put("action", action);
        logData.put("success", success);
        logData.put("timestamp", System.currentTimeMillis());

        setMDC(logData);
        if (success) {
            LOG.infof("Security [%s]: user=%s, action=%s SUCCESS", eventType, userId, action);
        } else {
            LOG.warnf("Security [%s]: user=%s, action=%s FAILED", eventType, userId, action);
        }
        clearMDC();
    }

    /**
     * Log HTTP request with details
     */
    public void logHttpRequest(String method, String path, int statusCode,
                              long durationMs, String correlationId) {
        Map<String, Object> logData = new HashMap<>();
        logData.put("category", CATEGORY_HTTP);
        logData.put("method", method);
        logData.put("path", path);
        logData.put("statusCode", statusCode);
        logData.put("durationMs", durationMs);
        logData.put("correlationId", correlationId);
        logData.put("timestamp", System.currentTimeMillis());

        setMDC(logData);
        LOG.infof("HTTP %s %s -> %d (%dms) [%s]", method, path, statusCode, durationMs, correlationId);
        clearMDC();
    }

    /**
     * Log error with exception and context
     */
    public void logError(String message, Throwable exception, Map<String, Object> context) {
        Map<String, Object> logData = new HashMap<>(context);
        logData.put("category", CATEGORY_ERROR);
        logData.put("errorMessage", message);
        logData.put("exceptionClass", exception.getClass().getName());
        logData.put("exceptionMessage", exception.getMessage());
        logData.put("timestamp", System.currentTimeMillis());

        setMDC(logData);
        LOG.error(message, exception);
        clearMDC();
    }

    /**
     * Log warning with context
     */
    public void logWarning(String message, Map<String, Object> context) {
        Map<String, Object> logData = new HashMap<>(context);
        logData.put("timestamp", System.currentTimeMillis());

        setMDC(logData);
        LOG.warn(message);
        clearMDC();
    }

    /**
     * Log system event (startup, shutdown, configuration changes)
     */
    public void logSystem(String eventType, String message, Map<String, Object> metadata) {
        Map<String, Object> logData = new HashMap<>(metadata);
        logData.put("category", CATEGORY_SYSTEM);
        logData.put("eventType", eventType);
        logData.put("timestamp", System.currentTimeMillis());

        setMDC(logData);
        LOG.infof("System [%s]: %s", eventType, message);
        clearMDC();
    }

    /**
     * Generate a new correlation ID for request tracing
     */
    public String generateCorrelationId() {
        return UUID.randomUUID().toString();
    }

    /**
     * Set correlation ID in MDC for request tracing
     */
    public void setCorrelationId(String correlationId) {
        MDC.put("correlationId", correlationId);
    }

    /**
     * Get current correlation ID from MDC
     */
    public String getCorrelationId() {
        Object correlationId = MDC.get("correlationId");
        return correlationId != null ? correlationId.toString() : null;
    }

    /**
     * Clear correlation ID from MDC
     */
    public void clearCorrelationId() {
        MDC.remove("correlationId");
    }

    /**
     * Set user context in MDC
     */
    public void setUserContext(String userId, String sessionId) {
        MDC.put("userId", userId);
        MDC.put("sessionId", sessionId);
    }

    /**
     * Clear user context from MDC
     */
    public void clearUserContext() {
        MDC.remove("userId");
        MDC.remove("sessionId");
    }

    /**
     * Set all fields in MDC for structured logging
     */
    private void setMDC(Map<String, Object> data) {
        data.forEach((key, value) -> {
            if (value != null) {
                MDC.put(key, value.toString());
            }
        });
    }

    /**
     * Clear all MDC fields
     */
    private void clearMDC() {
        MDC.clear();
    }

    /**
     * Create a map with common metadata fields
     */
    public Map<String, Object> createMetadata() {
        return new HashMap<>();
    }

    /**
     * Log a debug message (only in dev mode)
     */
    public void logDebug(String message, Map<String, Object> context) {
        if (LOG.isDebugEnabled()) {
            setMDC(context);
            LOG.debug(message);
            clearMDC();
        }
    }
}
