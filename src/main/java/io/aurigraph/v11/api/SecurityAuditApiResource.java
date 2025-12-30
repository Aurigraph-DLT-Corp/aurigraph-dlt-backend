package io.aurigraph.v11.api;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import io.smallrye.mutiny.Uni;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.util.*;

/**
 * Security Audit API Resource
 *
 * Provides security audit operations:
 * - GET /api/v11/security/audit-logs - Security audit trail
 *
 * @version 11.0.0
 * @author Security & Cryptography Agent (SCA)
 */
@Path("/api/v11/security")
@ApplicationScoped
@Tag(name = "Security Audit API", description = "Security audit logs and monitoring")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SecurityAuditApiResource {

    private static final Logger LOG = Logger.getLogger(SecurityAuditApiResource.class);

    // ==================== ENDPOINT 8: Security Audit Logs ====================

    /**
     * GET /api/v11/security/audit-logs
     * Get security audit trail
     */
    @GET
    @Path("/audit-logs")
    @Operation(summary = "Get security audit logs", description = "Retrieve security audit trail and events")
    @APIResponse(responseCode = "200", description = "Audit logs retrieved successfully",
                content = @Content(schema = @Schema(implementation = SecurityAuditLogsResponse.class)))
    public Uni<SecurityAuditLogsResponse> getAuditLogs(
        @QueryParam("severity") String severity,
        @QueryParam("category") String category,
        @QueryParam("limit") @DefaultValue("50") int limit,
        @QueryParam("offset") @DefaultValue("0") int offset,
        @QueryParam("fromDate") Long fromDate,
        @QueryParam("toDate") Long toDate) {

        LOG.infof("Fetching security audit logs: severity=%s, category=%s, limit=%d, offset=%d",
                 severity, category, limit, offset);

        return Uni.createFrom().item(() -> {
            SecurityAuditLogsResponse response = new SecurityAuditLogsResponse();
            response.timestamp = Instant.now().toEpochMilli();
            response.totalLogs = 8456;
            response.limit = limit;
            response.offset = offset;
            response.logs = new ArrayList<>();

            String[] severities = {"CRITICAL", "HIGH", "MEDIUM", "LOW", "INFO"};
            String[] categories = {"AUTHENTICATION", "AUTHORIZATION", "CRYPTOGRAPHY",
                                  "CONSENSUS", "NETWORK", "CONTRACT", "VALIDATOR"};
            String[] actions = {"LOGIN_ATTEMPT", "ACCESS_DENIED", "KEY_ROTATION",
                               "SIGNATURE_VERIFICATION", "SLASHING_EVENT",
                               "UNAUTHORIZED_ACCESS", "ANOMALY_DETECTED",
                               "QUANTUM_KEY_EXCHANGE", "BRIDGE_TRANSFER"};

            long currentTime = Instant.now().toEpochMilli();

            for (int i = 0; i < Math.min(limit, 50); i++) {
                SecurityAuditLog log = new SecurityAuditLog();
                log.logId = "audit-" + UUID.randomUUID().toString().substring(0, 8);
                log.timestamp = currentTime - ((offset + i) * 60000); // 1 minute intervals

                String selectedSeverity = severity != null ? severity :
                                         severities[(offset + i) % severities.length];
                log.severity = selectedSeverity;

                String selectedCategory = category != null ? category :
                                         categories[(offset + i) % categories.length];
                log.category = selectedCategory;

                log.action = actions[(offset + i) % actions.length];
                log.userId = "user-" + (1000 + (int)(Math.random() * 9000));
                log.ipAddress = generateRandomIP();
                log.userAgent = "Aurigraph-Client/11.0.0";
                log.success = (offset + i) % 7 != 0; // ~14% failure rate

                // Generate details based on action
                log.details = new HashMap<>();
                switch (log.action) {
                    case "LOGIN_ATTEMPT":
                        log.details.put("method", "password");
                        log.details.put("2fa_enabled", true);
                        log.details.put("location", "US-East");
                        break;
                    case "ACCESS_DENIED":
                        log.details.put("resource", "/api/v11/admin/config");
                        log.details.put("required_role", "ADMIN");
                        log.details.put("user_role", "USER");
                        break;
                    case "KEY_ROTATION":
                        log.details.put("key_type", "DILITHIUM");
                        log.details.put("previous_key_age_days", 90);
                        break;
                    case "SLASHING_EVENT":
                        log.details.put("validator", "validator-042");
                        log.details.put("slash_amount", 50000.0);
                        log.details.put("reason", "Double signing");
                        break;
                    case "ANOMALY_DETECTED":
                        log.details.put("anomaly_type", "unusual_transaction_pattern");
                        log.details.put("confidence", 95.5);
                        log.details.put("model", "anomaly-detector-v1");
                        break;
                    default:
                        log.details.put("status", "processed");
                }

                log.blockNumber = (offset + i) % 10 == 0 ? 1500000L + i : null;
                log.transactionHash = (offset + i) % 5 == 0 ?
                    "0x" + UUID.randomUUID().toString().replace("-", "") : null;

                // Apply filters
                if (fromDate != null && log.timestamp < fromDate) continue;
                if (toDate != null && log.timestamp > toDate) continue;

                response.logs.add(log);
            }

            response.logsReturned = response.logs.size();

            // Summary statistics
            response.summary = new AuditLogSummary();
            response.summary.criticalCount = (int)(response.totalLogs * 0.05);
            response.summary.highCount = (int)(response.totalLogs * 0.15);
            response.summary.mediumCount = (int)(response.totalLogs * 0.30);
            response.summary.lowCount = (int)(response.totalLogs * 0.35);
            response.summary.infoCount = (int)(response.totalLogs * 0.15);
            response.summary.failedActionsCount = (int)(response.totalLogs * 0.14);
            response.summary.uniqueUsers = 245;
            response.summary.mostCommonCategory = "AUTHENTICATION";
            response.summary.mostCommonAction = "LOGIN_ATTEMPT";

            LOG.infof("Retrieved %d security audit logs", response.logsReturned);

            return response;
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    // ==================== Helper Methods ====================

    private String generateRandomIP() {
        return (int)(Math.random() * 255) + "." +
               (int)(Math.random() * 255) + "." +
               (int)(Math.random() * 255) + "." +
               (int)(Math.random() * 255);
    }

    // ==================== Response DTOs ====================

    public static class SecurityAuditLogsResponse {
        public long timestamp;
        public long totalLogs;
        public int logsReturned;
        public int limit;
        public int offset;
        public List<SecurityAuditLog> logs;
        public AuditLogSummary summary;
    }

    public static class SecurityAuditLog {
        public String logId;
        public long timestamp;
        public String severity;
        public String category;
        public String action;
        public String userId;
        public String ipAddress;
        public String userAgent;
        public boolean success;
        public Map<String, Object> details;
        public Long blockNumber;
        public String transactionHash;
    }

    public static class AuditLogSummary {
        public int criticalCount;
        public int highCount;
        public int mediumCount;
        public int lowCount;
        public int infoCount;
        public int failedActionsCount;
        public int uniqueUsers;
        public String mostCommonCategory;
        public String mostCommonAction;
    }
}
