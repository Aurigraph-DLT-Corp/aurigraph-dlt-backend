package io.aurigraph.v11.compliance.dashboard;

import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import io.aurigraph.v11.compliance.erc3643.IdentityRegistry;
import io.aurigraph.v11.compliance.erc3643.TransferManager;
import io.aurigraph.v11.compliance.erc3643.ComplianceRegistry;
import io.aurigraph.v11.compliance.oracle.OFACSanctionsOracle;
import io.aurigraph.v11.compliance.persistence.ComplianceRepository;
import io.aurigraph.v11.compliance.persistence.TransferAuditRepository;
import io.aurigraph.v11.compliance.persistence.IdentityRepository;
import java.time.Instant;
import java.util.*;

/**
 * Compliance Monitoring Dashboard
 * Provides real-time compliance metrics and monitoring data
 */
@ApplicationScoped
public class ComplianceDashboard {

    @Inject
    IdentityRegistry identityRegistry;

    @Inject
    TransferManager transferManager;

    @Inject
    ComplianceRegistry complianceRegistry;

    @Inject
    OFACSanctionsOracle sanctionsOracle;

    @Inject
    ComplianceRepository complianceRepository;

    @Inject
    TransferAuditRepository transferAuditRepository;

    @Inject
    IdentityRepository identityRepository;

    /**
     * Generate comprehensive dashboard metrics
     */
    public DashboardMetrics generateDashboardMetrics() {
        Log.info("Generating compliance dashboard metrics");

        DashboardMetrics metrics = new DashboardMetrics();
        metrics.setGeneratedAt(Instant.now());

        // Identity metrics
        var identityStats = identityRegistry.getStats();
        metrics.setIdentityMetrics(new DashboardMetrics.IdentityMetrics(
            (int) identityStats.getTotalRegistered(),
            (int) identityStats.getActiveIdentities(),
            (int) identityStats.getRevokedIdentities(),
            identityStats.getRestrictedCountries()
        ));

        // Transfer metrics
        var transferStats = transferManager.getStats();
        metrics.setTransferMetrics(new DashboardMetrics.TransferMetrics(
            transferStats.getTotalTransfers(),
            transferStats.getApprovedTransfers(),
            transferStats.getRejectedTransfers(),
            transferStats.getApprovalRate()
        ));

        // Compliance metrics
        var complianceStats = complianceRegistry.getStats();
        metrics.setComplianceMetrics(new DashboardMetrics.ComplianceMetrics(
            complianceStats.getTotalChecks(),
            complianceStats.getPassedChecks(),
            complianceStats.getFailedChecks(),
            complianceStats.getComplianceRate()
        ));

        // OFAC metrics
        var oracleStats = sanctionsOracle.getStats();
        metrics.setOracleMetrics(new DashboardMetrics.OracleMetrics(
            oracleStats.getSanctionedAddresses(),
            oracleStats.getRestrictedCountries(),
            oracleStats.getSanctionedIndividuals(),
            oracleStats.getHitRate(),
            oracleStats.getTotalChecks()
        ));

        // System health
        metrics.setSystemHealth(calculateSystemHealth(metrics));

        return metrics;
    }

    /**
     * Get alert summary (compliance violations and issues)
     */
    public AlertSummary getAlertSummary() {
        Log.info("Generating alert summary");

        AlertSummary summary = new AlertSummary();
        summary.setGeneratedAt(Instant.now());

        // Collect alerts from various sources
        List<Alert> alerts = new ArrayList<>();

        // High rejection rate alert
        var transferStats = transferManager.getStats();
        if (transferStats.getTotalTransfers() > 0) {
            double rejectionRate = (transferStats.getRejectedTransfers() * 100.0) / transferStats.getTotalTransfers();
            if (rejectionRate > 20.0) {
                alerts.add(new Alert(
                    "HIGH_REJECTION_RATE",
                    "High rejection rate detected: " + String.format("%.2f%%", rejectionRate),
                    Alert.Severity.WARNING,
                    Instant.now()
                ));
            }
        }

        // OFAC violations alert
        var oracleStats = sanctionsOracle.getStats();
        if (oracleStats.getSanctionedAddresses() > 0) {
            alerts.add(new Alert(
                "OFAC_SANCTIONS_DETECTED",
                "Sanctioned entities detected: " + oracleStats.getSanctionedAddresses() + " addresses",
                Alert.Severity.CRITICAL,
                Instant.now()
            ));
        }

        // Low cache hit rate alert
        if (oracleStats.getHitRate() < 50.0 && oracleStats.getTotalChecks() > 100) {
            alerts.add(new Alert(
                "LOW_CACHE_HIT_RATE",
                "Cache hit rate is low: " + String.format("%.2f%%", oracleStats.getHitRate()),
                Alert.Severity.INFO,
                Instant.now()
            ));
        }

        summary.setAlerts(alerts);
        summary.setCriticalCount((int) alerts.stream().filter(a -> a.getSeverity() == Alert.Severity.CRITICAL).count());
        summary.setWarningCount((int) alerts.stream().filter(a -> a.getSeverity() == Alert.Severity.WARNING).count());
        summary.setInfoCount((int) alerts.stream().filter(a -> a.getSeverity() == Alert.Severity.INFO).count());

        return summary;
    }

    /**
     * Get compliance status overview
     */
    public ComplianceStatusOverview getComplianceStatus() {
        Log.info("Getting compliance status overview");

        ComplianceStatusOverview overview = new ComplianceStatusOverview();
        overview.setGeneratedAt(Instant.now());

        // Get compliant vs non-compliant tokens
        long compliantCount = complianceRepository.countCompliant();
        long nonCompliantCount = complianceRepository.countNonCompliant();

        overview.setCompliantTokens((int) compliantCount);
        overview.setNonCompliantTokens((int) nonCompliantCount);

        double complianceRate = (compliantCount + nonCompliantCount) > 0 ?
            (compliantCount * 100.0) / (compliantCount + nonCompliantCount) : 0.0;
        overview.setComplianceRate(complianceRate);

        // Get active identities and their status
        var identityStats = identityRegistry.getStats();
        overview.setActiveIdentities((int) identityStats.getActiveIdentities());
        overview.setRevokedIdentities((int) identityStats.getRevokedIdentities());

        // Get recent transfer stats (last 24 hours)
        var recentTransfers = transferAuditRepository.findByTokenId("all");
        long recentApproved = recentTransfers.stream().filter(t -> t.success).count();
        long recentRejected = recentTransfers.stream().filter(t -> !t.success).count();

        overview.setRecentApprovedTransfers((int) recentApproved);
        overview.setRecentRejectedTransfers((int) recentRejected);

        return overview;
    }

    /**
     * Get top compliance risks
     */
    public List<RiskItem> getTopRisks(int limit) {
        Log.infof("Getting top %d compliance risks", limit);

        List<RiskItem> risks = new ArrayList<>();

        // Check for high-rejection-rate tokens
        var allCompliance = complianceRepository.listAll();
        for (var compliance : allCompliance) {
            String tokenId = compliance.tokenId;
            long approved = transferAuditRepository.countApproved(tokenId);
            long rejected = transferAuditRepository.countRejected(tokenId);
            long total = approved + rejected;

            if (total > 0) {
                double rejectionRate = (rejected * 100.0) / total;
                if (rejectionRate > 15.0) {
                    risks.add(new RiskItem(
                        tokenId,
                        "HIGH_REJECTION_RATE",
                        "High rejection rate: " + String.format("%.2f%%", rejectionRate),
                        rejectionRate / 100.0,
                        Instant.now()
                    ));
                }
            }
        }

        // Check for countries with many restricted identities
        var identities = identityRepository.listAll();
        Map<String, Integer> countryRiskMap = new HashMap<>();
        for (var identity : identities) {
            if ("REVOKED".equals(identity.status)) {
                countryRiskMap.merge(identity.country, 1, Integer::sum);
            }
        }

        for (Map.Entry<String, Integer> entry : countryRiskMap.entrySet()) {
            if (entry.getValue() > 5) {
                risks.add(new RiskItem(
                    "COUNTRY:" + entry.getKey(),
                    "HIGH_REVOCATION_RATE",
                    "High revocation rate in country: " + entry.getValue() + " revoked identities",
                    Math.min(1.0, entry.getValue() / 20.0),
                    Instant.now()
                ));
            }
        }

        // Sort by risk score and limit
        return risks.stream()
            .sorted((r1, r2) -> Double.compare(r2.getRiskScore(), r1.getRiskScore()))
            .limit(limit)
            .toList();
    }

    /**
     * Calculate system health score
     */
    private SystemHealth calculateSystemHealth(DashboardMetrics metrics) {
        SystemHealth health = new SystemHealth();

        // Calculate individual component health scores
        double identityHealth = 100.0; // All components healthy by default
        double transferHealth = metrics.getTransferMetrics().getApprovalRate();
        double complianceHealth = metrics.getComplianceMetrics().getComplianceRate();
        double oracleHealth = metrics.getOracleMetrics().getCacheHitRate();

        // Overall health is average of components
        double overallHealth = (identityHealth + transferHealth + complianceHealth + oracleHealth) / 4.0;

        health.setIdentityServiceHealth(identityHealth);
        health.setTransferServiceHealth(transferHealth);
        health.setComplianceServiceHealth(complianceHealth);
        health.setOracleServiceHealth(oracleHealth);
        health.setOverallHealth(overallHealth);

        // Determine health status
        if (overallHealth >= 90.0) {
            health.setStatus("HEALTHY");
        } else if (overallHealth >= 70.0) {
            health.setStatus("DEGRADED");
        } else {
            health.setStatus("UNHEALTHY");
        }

        return health;
    }

    // Inner classes for dashboard data models

    public static class DashboardMetrics {
        private Instant generatedAt;
        private IdentityMetrics identityMetrics;
        private TransferMetrics transferMetrics;
        private ComplianceMetrics complianceMetrics;
        private OracleMetrics oracleMetrics;
        private SystemHealth systemHealth;

        // Identity metrics
        public static class IdentityMetrics {
            private final int totalIdentities;
            private final int activeIdentities;
            private final int revokedIdentities;
            private final int restrictedCountries;

            public IdentityMetrics(int total, int active, int revoked, int restricted) {
                this.totalIdentities = total;
                this.activeIdentities = active;
                this.revokedIdentities = revoked;
                this.restrictedCountries = restricted;
            }

            public int getTotalIdentities() { return totalIdentities; }
            public int getActiveIdentities() { return activeIdentities; }
            public int getRevokedIdentities() { return revokedIdentities; }
            public int getRestrictedCountries() { return restrictedCountries; }
        }

        // Transfer metrics
        public static class TransferMetrics {
            private final long totalTransfers;
            private final long approvedTransfers;
            private final long rejectedTransfers;
            private final double approvalRate;

            public TransferMetrics(long total, long approved, long rejected, double rate) {
                this.totalTransfers = total;
                this.approvedTransfers = approved;
                this.rejectedTransfers = rejected;
                this.approvalRate = rate;
            }

            public long getTotalTransfers() { return totalTransfers; }
            public long getApprovedTransfers() { return approvedTransfers; }
            public long getRejectedTransfers() { return rejectedTransfers; }
            public double getApprovalRate() { return approvalRate; }
        }

        // Compliance metrics
        public static class ComplianceMetrics {
            private final long totalChecks;
            private final long passedChecks;
            private final long failedChecks;
            private final double complianceRate;

            public ComplianceMetrics(long total, long passed, long failed, double rate) {
                this.totalChecks = total;
                this.passedChecks = passed;
                this.failedChecks = failed;
                this.complianceRate = rate;
            }

            public long getTotalChecks() { return totalChecks; }
            public long getPassedChecks() { return passedChecks; }
            public long getFailedChecks() { return failedChecks; }
            public double getComplianceRate() { return complianceRate; }
        }

        // Oracle metrics
        public static class OracleMetrics {
            private final int sanctionedAddresses;
            private final int restrictedCountries;
            private final int sanctionedIndividuals;
            private final double cacheHitRate;
            private final long totalChecks;

            public OracleMetrics(int addresses, int countries, int individuals, double hitRate, long checks) {
                this.sanctionedAddresses = addresses;
                this.restrictedCountries = countries;
                this.sanctionedIndividuals = individuals;
                this.cacheHitRate = hitRate;
                this.totalChecks = checks;
            }

            public int getSanctionedAddresses() { return sanctionedAddresses; }
            public int getRestrictedCountries() { return restrictedCountries; }
            public int getSanctionedIndividuals() { return sanctionedIndividuals; }
            public double getCacheHitRate() { return cacheHitRate; }
            public long getTotalChecks() { return totalChecks; }
        }

        // Getters and Setters
        public Instant getGeneratedAt() { return generatedAt; }
        public void setGeneratedAt(Instant generatedAt) { this.generatedAt = generatedAt; }
        public IdentityMetrics getIdentityMetrics() { return identityMetrics; }
        public void setIdentityMetrics(IdentityMetrics metrics) { this.identityMetrics = metrics; }
        public TransferMetrics getTransferMetrics() { return transferMetrics; }
        public void setTransferMetrics(TransferMetrics metrics) { this.transferMetrics = metrics; }
        public ComplianceMetrics getComplianceMetrics() { return complianceMetrics; }
        public void setComplianceMetrics(ComplianceMetrics metrics) { this.complianceMetrics = metrics; }
        public OracleMetrics getOracleMetrics() { return oracleMetrics; }
        public void setOracleMetrics(OracleMetrics metrics) { this.oracleMetrics = metrics; }
        public SystemHealth getSystemHealth() { return systemHealth; }
        public void setSystemHealth(SystemHealth health) { this.systemHealth = health; }
    }

    public static class SystemHealth {
        private double identityServiceHealth;
        private double transferServiceHealth;
        private double complianceServiceHealth;
        private double oracleServiceHealth;
        private double overallHealth;
        private String status;

        // Getters and Setters
        public double getIdentityServiceHealth() { return identityServiceHealth; }
        public void setIdentityServiceHealth(double health) { this.identityServiceHealth = health; }
        public double getTransferServiceHealth() { return transferServiceHealth; }
        public void setTransferServiceHealth(double health) { this.transferServiceHealth = health; }
        public double getComplianceServiceHealth() { return complianceServiceHealth; }
        public void setComplianceServiceHealth(double health) { this.complianceServiceHealth = health; }
        public double getOracleServiceHealth() { return oracleServiceHealth; }
        public void setOracleServiceHealth(double health) { this.oracleServiceHealth = health; }
        public double getOverallHealth() { return overallHealth; }
        public void setOverallHealth(double health) { this.overallHealth = health; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }

    public static class AlertSummary {
        private List<Alert> alerts = new ArrayList<>();
        private int criticalCount;
        private int warningCount;
        private int infoCount;
        private Instant generatedAt;

        public List<Alert> getAlerts() { return alerts; }
        public void setAlerts(List<Alert> alerts) { this.alerts = alerts; }
        public int getCriticalCount() { return criticalCount; }
        public void setCriticalCount(int count) { this.criticalCount = count; }
        public int getWarningCount() { return warningCount; }
        public void setWarningCount(int count) { this.warningCount = count; }
        public int getInfoCount() { return infoCount; }
        public void setInfoCount(int count) { this.infoCount = count; }
        public Instant getGeneratedAt() { return generatedAt; }
        public void setGeneratedAt(Instant generatedAt) { this.generatedAt = generatedAt; }
    }

    public static class Alert {
        public enum Severity { CRITICAL, WARNING, INFO }

        private final String alertType;
        private final String message;
        private final Severity severity;
        private final Instant timestamp;

        public Alert(String type, String message, Severity severity, Instant timestamp) {
            this.alertType = type;
            this.message = message;
            this.severity = severity;
            this.timestamp = timestamp;
        }

        public String getAlertType() { return alertType; }
        public String getMessage() { return message; }
        public Severity getSeverity() { return severity; }
        public Instant getTimestamp() { return timestamp; }
    }

    public static class ComplianceStatusOverview {
        private int compliantTokens;
        private int nonCompliantTokens;
        private double complianceRate;
        private int activeIdentities;
        private int revokedIdentities;
        private int recentApprovedTransfers;
        private int recentRejectedTransfers;
        private Instant generatedAt;

        public int getCompliantTokens() { return compliantTokens; }
        public void setCompliantTokens(int count) { this.compliantTokens = count; }
        public int getNonCompliantTokens() { return nonCompliantTokens; }
        public void setNonCompliantTokens(int count) { this.nonCompliantTokens = count; }
        public double getComplianceRate() { return complianceRate; }
        public void setComplianceRate(double rate) { this.complianceRate = rate; }
        public int getActiveIdentities() { return activeIdentities; }
        public void setActiveIdentities(int count) { this.activeIdentities = count; }
        public int getRevokedIdentities() { return revokedIdentities; }
        public void setRevokedIdentities(int count) { this.revokedIdentities = count; }
        public int getRecentApprovedTransfers() { return recentApprovedTransfers; }
        public void setRecentApprovedTransfers(int count) { this.recentApprovedTransfers = count; }
        public int getRecentRejectedTransfers() { return recentRejectedTransfers; }
        public void setRecentRejectedTransfers(int count) { this.recentRejectedTransfers = count; }
        public Instant getGeneratedAt() { return generatedAt; }
        public void setGeneratedAt(Instant generatedAt) { this.generatedAt = generatedAt; }
    }

    public static class RiskItem {
        private final String identifier;
        private final String riskType;
        private final String description;
        private final double riskScore;
        private final Instant detectedAt;

        public RiskItem(String id, String type, String desc, double score, Instant detected) {
            this.identifier = id;
            this.riskType = type;
            this.description = desc;
            this.riskScore = score;
            this.detectedAt = detected;
        }

        public String getIdentifier() { return identifier; }
        public String getRiskType() { return riskType; }
        public String getDescription() { return description; }
        public double getRiskScore() { return riskScore; }
        public Instant getDetectedAt() { return detectedAt; }
    }
}
