package io.aurigraph.v11.compliance.dashboard;

import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import io.quarkus.logging.Log;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST API endpoints for Compliance Monitoring Dashboard
 * Provides real-time compliance metrics, alerts, and risk assessment
 */
@Path("/api/v11/compliance/dashboard")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class DashboardResource {

    @Inject
    ComplianceDashboard dashboard;

    /**
     * Get comprehensive dashboard metrics
     * GET /api/v11/compliance/dashboard/metrics
     */
    @GET
    @Path("/metrics")
    public Uni<Response> getDashboardMetrics() {
        return Uni.createFrom().deferred(() -> {
            Log.info("Getting dashboard metrics");

            var metrics = dashboard.generateDashboardMetrics();

            return Uni.createFrom().item(
                Response.ok(Map.of(
                    "success", true,
                    "generatedAt", metrics.getGeneratedAt().toString(),
                    "identity", Map.of(
                        "total", metrics.getIdentityMetrics().getTotalIdentities(),
                        "active", metrics.getIdentityMetrics().getActiveIdentities(),
                        "revoked", metrics.getIdentityMetrics().getRevokedIdentities(),
                        "restrictedCountries", metrics.getIdentityMetrics().getRestrictedCountries()
                    ),
                    "transfers", Map.of(
                        "total", metrics.getTransferMetrics().getTotalTransfers(),
                        "approved", metrics.getTransferMetrics().getApprovedTransfers(),
                        "rejected", metrics.getTransferMetrics().getRejectedTransfers(),
                        "approvalRate", String.format("%.2f%%", metrics.getTransferMetrics().getApprovalRate())
                    ),
                    "compliance", Map.of(
                        "totalChecks", metrics.getComplianceMetrics().getTotalChecks(),
                        "passed", metrics.getComplianceMetrics().getPassedChecks(),
                        "failed", metrics.getComplianceMetrics().getFailedChecks(),
                        "complianceRate", String.format("%.2f%%", metrics.getComplianceMetrics().getComplianceRate())
                    ),
                    "oracle", Map.of(
                        "sanctionedAddresses", metrics.getOracleMetrics().getSanctionedAddresses(),
                        "restrictedCountries", metrics.getOracleMetrics().getRestrictedCountries(),
                        "sanctionedIndividuals", metrics.getOracleMetrics().getSanctionedIndividuals(),
                        "cacheHitRate", String.format("%.2f%%", metrics.getOracleMetrics().getCacheHitRate()),
                        "totalChecks", metrics.getOracleMetrics().getTotalChecks()
                    ),
                    "systemHealth", Map.of(
                        "identityService", String.format("%.2f%%", metrics.getSystemHealth().getIdentityServiceHealth()),
                        "transferService", String.format("%.2f%%", metrics.getSystemHealth().getTransferServiceHealth()),
                        "complianceService", String.format("%.2f%%", metrics.getSystemHealth().getComplianceServiceHealth()),
                        "oracleService", String.format("%.2f%%", metrics.getSystemHealth().getOracleServiceHealth()),
                        "overall", String.format("%.2f%%", metrics.getSystemHealth().getOverallHealth()),
                        "status", metrics.getSystemHealth().getStatus()
                    )
                )).build()
            );
        });
    }

    /**
     * Get alert summary
     * GET /api/v11/compliance/dashboard/alerts
     */
    @GET
    @Path("/alerts")
    public Uni<Response> getAlerts() {
        return Uni.createFrom().deferred(() -> {
            Log.info("Getting alert summary");

            var alerts = dashboard.getAlertSummary();

            var alertList = alerts.getAlerts().stream()
                .map(alert -> Map.of(
                    "type", alert.getAlertType(),
                    "message", alert.getMessage(),
                    "severity", alert.getSeverity().toString(),
                    "timestamp", alert.getTimestamp().toString()
                ))
                .collect(Collectors.toList());

            return Uni.createFrom().item(
                Response.ok(Map.of(
                    "success", true,
                    "critical", alerts.getCriticalCount(),
                    "warning", alerts.getWarningCount(),
                    "info", alerts.getInfoCount(),
                    "alerts", alertList,
                    "generatedAt", alerts.getGeneratedAt().toString()
                )).build()
            );
        });
    }

    /**
     * Get compliance status overview
     * GET /api/v11/compliance/dashboard/status
     */
    @GET
    @Path("/status")
    public Uni<Response> getComplianceStatus() {
        return Uni.createFrom().deferred(() -> {
            Log.info("Getting compliance status");

            var status = dashboard.getComplianceStatus();

            return Uni.createFrom().item(
                Response.ok(Map.of(
                    "success", true,
                    "compliantTokens", status.getCompliantTokens(),
                    "nonCompliantTokens", status.getNonCompliantTokens(),
                    "complianceRate", String.format("%.2f%%", status.getComplianceRate()),
                    "identities", Map.of(
                        "active", status.getActiveIdentities(),
                        "revoked", status.getRevokedIdentities()
                    ),
                    "recentTransfers", Map.of(
                        "approved", status.getRecentApprovedTransfers(),
                        "rejected", status.getRecentRejectedTransfers()
                    ),
                    "generatedAt", status.getGeneratedAt().toString()
                )).build()
            );
        });
    }

    /**
     * Get top compliance risks
     * GET /api/v11/compliance/dashboard/risks?limit=10
     */
    @GET
    @Path("/risks")
    public Uni<Response> getTopRisks(@QueryParam("limit") @DefaultValue("10") int limit) {
        return Uni.createFrom().deferred(() -> {
            Log.infof("Getting top %d compliance risks", limit);

            var risks = dashboard.getTopRisks(limit);

            var riskList = risks.stream()
                .map(risk -> Map.of(
                    "identifier", risk.getIdentifier(),
                    "type", risk.getRiskType(),
                    "description", risk.getDescription(),
                    "score", String.format("%.2f", risk.getRiskScore() * 100),
                    "detectedAt", risk.getDetectedAt().toString()
                ))
                .collect(Collectors.toList());

            return Uni.createFrom().item(
                Response.ok(Map.of(
                    "success", true,
                    "riskCount", riskList.size(),
                    "risks", riskList
                )).build()
            );
        });
    }

    /**
     * Get system health
     * GET /api/v11/compliance/dashboard/health
     */
    @GET
    @Path("/health")
    public Uni<Response> getSystemHealth() {
        return Uni.createFrom().deferred(() -> {
            Log.info("Getting system health");

            var metrics = dashboard.generateDashboardMetrics();
            var health = metrics.getSystemHealth();

            return Uni.createFrom().item(
                Response.ok(Map.of(
                    "success", true,
                    "status", health.getStatus(),
                    "overall", String.format("%.2f%%", health.getOverallHealth()),
                    "components", Map.of(
                        "identityService", String.format("%.2f%%", health.getIdentityServiceHealth()),
                        "transferService", String.format("%.2f%%", health.getTransferServiceHealth()),
                        "complianceService", String.format("%.2f%%", health.getComplianceServiceHealth()),
                        "oracleService", String.format("%.2f%%", health.getOracleServiceHealth())
                    ),
                    "timestamp", java.time.Instant.now().toString()
                )).build()
            );
        });
    }

    /**
     * Get dashboard summary (quick overview)
     * GET /api/v11/compliance/dashboard/summary
     */
    @GET
    @Path("/summary")
    public Uni<Response> getDashboardSummary() {
        return Uni.createFrom().deferred(() -> {
            Log.info("Getting dashboard summary");

            var metrics = dashboard.generateDashboardMetrics();
            var alerts = dashboard.getAlertSummary();
            var risks = dashboard.getTopRisks(5);

            return Uni.createFrom().item(
                Response.ok(Map.of(
                    "success", true,
                    "systemStatus", metrics.getSystemHealth().getStatus(),
                    "overallHealth", String.format("%.2f%%", metrics.getSystemHealth().getOverallHealth()),
                    "criticalAlerts", alerts.getCriticalCount(),
                    "activeRisks", risks.size(),
                    "totalTransfers", metrics.getTransferMetrics().getTotalTransfers(),
                    "approvalRate", String.format("%.2f%%", metrics.getTransferMetrics().getApprovalRate()),
                    "complianceRate", String.format("%.2f%%", metrics.getComplianceMetrics().getComplianceRate()),
                    "activeIdentities", metrics.getIdentityMetrics().getActiveIdentities(),
                    "timestamp", java.time.Instant.now().toString()
                )).build()
            );
        });
    }
}
