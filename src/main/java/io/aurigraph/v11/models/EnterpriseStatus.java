package io.aurigraph.v11.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Enterprise Status Model
 * Provides enterprise features overview and multi-tenancy status
 *
 * Used by /api/v11/enterprise/status endpoint
 *
 * @author Aurigraph V11
 * @version 11.3.0
 */
public class EnterpriseStatus {

    @JsonProperty("timestamp")
    private Instant timestamp;

    @JsonProperty("tenants")
    private TenantSummary tenants;

    @JsonProperty("features")
    private EnterpriseFeatures features;

    @JsonProperty("usage")
    private UsageMetrics usage;

    @JsonProperty("compliance")
    private ComplianceInfo compliance;

    @JsonProperty("support")
    private SupportInfo support;

    // Constructor
    public EnterpriseStatus() {
        this.timestamp = Instant.now();
    }

    // Getters and Setters
    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }

    public TenantSummary getTenants() { return tenants; }
    public void setTenants(TenantSummary tenants) { this.tenants = tenants; }

    public EnterpriseFeatures getFeatures() { return features; }
    public void setFeatures(EnterpriseFeatures features) { this.features = features; }

    public UsageMetrics getUsage() { return usage; }
    public void setUsage(UsageMetrics usage) { this.usage = usage; }

    public ComplianceInfo getCompliance() { return compliance; }
    public void setCompliance(ComplianceInfo compliance) { this.compliance = compliance; }

    public SupportInfo getSupport() { return support; }
    public void setSupport(SupportInfo support) { this.support = support; }

    /**
     * Tenant Summary
     */
    public static class TenantSummary {
        @JsonProperty("total_tenants")
        private int totalTenants;

        @JsonProperty("active_tenants")
        private int activeTenants;

        @JsonProperty("trial_tenants")
        private int trialTenants;

        @JsonProperty("suspended_tenants")
        private int suspendedTenants;

        @JsonProperty("tier_distribution")
        private Map<String, Integer> tierDistribution; // enterprise, professional, starter

        public TenantSummary() {}

        // Getters and Setters
        public int getTotalTenants() { return totalTenants; }
        public void setTotalTenants(int totalTenants) { this.totalTenants = totalTenants; }

        public int getActiveTenants() { return activeTenants; }
        public void setActiveTenants(int activeTenants) { this.activeTenants = activeTenants; }

        public int getTrialTenants() { return trialTenants; }
        public void setTrialTenants(int trialTenants) { this.trialTenants = trialTenants; }

        public int getSuspendedTenants() { return suspendedTenants; }
        public void setSuspendedTenants(int suspendedTenants) { this.suspendedTenants = suspendedTenants; }

        public Map<String, Integer> getTierDistribution() { return tierDistribution; }
        public void setTierDistribution(Map<String, Integer> tierDistribution) { this.tierDistribution = tierDistribution; }
    }

    /**
     * Enterprise Features
     */
    public static class EnterpriseFeatures {
        @JsonProperty("multi_tenancy")
        private FeatureStatus multiTenancy;

        @JsonProperty("advanced_analytics")
        private FeatureStatus advancedAnalytics;

        @JsonProperty("white_labeling")
        private FeatureStatus whiteLabeling;

        @JsonProperty("sso_integration")
        private FeatureStatus ssoIntegration;

        @JsonProperty("custom_contracts")
        private FeatureStatus customContracts;

        @JsonProperty("dedicated_support")
        private FeatureStatus dedicatedSupport;

        public EnterpriseFeatures() {}

        // Getters and Setters
        public FeatureStatus getMultiTenancy() { return multiTenancy; }
        public void setMultiTenancy(FeatureStatus multiTenancy) { this.multiTenancy = multiTenancy; }

        public FeatureStatus getAdvancedAnalytics() { return advancedAnalytics; }
        public void setAdvancedAnalytics(FeatureStatus advancedAnalytics) { this.advancedAnalytics = advancedAnalytics; }

        public FeatureStatus getWhiteLabeling() { return whiteLabeling; }
        public void setWhiteLabeling(FeatureStatus whiteLabeling) { this.whiteLabeling = whiteLabeling; }

        public FeatureStatus getSsoIntegration() { return ssoIntegration; }
        public void setSsoIntegration(FeatureStatus ssoIntegration) { this.ssoIntegration = ssoIntegration; }

        public FeatureStatus getCustomContracts() { return customContracts; }
        public void setCustomContracts(FeatureStatus customContracts) { this.customContracts = customContracts; }

        public FeatureStatus getDedicatedSupport() { return dedicatedSupport; }
        public void setDedicatedSupport(FeatureStatus dedicatedSupport) { this.dedicatedSupport = dedicatedSupport; }
    }

    /**
     * Feature Status
     */
    public static class FeatureStatus {
        @JsonProperty("enabled")
        private boolean enabled;

        @JsonProperty("usage_count")
        private long usageCount;

        @JsonProperty("available_tiers")
        private List<String> availableTiers;

        public FeatureStatus() {}

        public FeatureStatus(boolean enabled, long usageCount, List<String> tiers) {
            this.enabled = enabled;
            this.usageCount = usageCount;
            this.availableTiers = tiers;
        }

        // Getters and Setters
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }

        public long getUsageCount() { return usageCount; }
        public void setUsageCount(long usageCount) { this.usageCount = usageCount; }

        public List<String> getAvailableTiers() { return availableTiers; }
        public void setAvailableTiers(List<String> availableTiers) { this.availableTiers = availableTiers; }
    }

    /**
     * Usage Metrics
     */
    public static class UsageMetrics {
        @JsonProperty("total_transactions_30d")
        private long totalTransactions30d;

        @JsonProperty("total_storage_gb")
        private double totalStorageGb;

        @JsonProperty("api_calls_30d")
        private long apiCalls30d;

        @JsonProperty("data_transfer_gb_30d")
        private double dataTransferGb30d;

        @JsonProperty("average_tps")
        private double averageTps;

        @JsonProperty("peak_tps")
        private double peakTps;

        public UsageMetrics() {}

        // Getters and Setters
        public long getTotalTransactions30d() { return totalTransactions30d; }
        public void setTotalTransactions30d(long totalTransactions30d) { this.totalTransactions30d = totalTransactions30d; }

        public double getTotalStorageGb() { return totalStorageGb; }
        public void setTotalStorageGb(double totalStorageGb) { this.totalStorageGb = totalStorageGb; }

        public long getApiCalls30d() { return apiCalls30d; }
        public void setApiCalls30d(long apiCalls30d) { this.apiCalls30d = apiCalls30d; }

        public double getDataTransferGb30d() { return dataTransferGb30d; }
        public void setDataTransferGb30d(double dataTransferGb30d) { this.dataTransferGb30d = dataTransferGb30d; }

        public double getAverageTps() { return averageTps; }
        public void setAverageTps(double averageTps) { this.averageTps = averageTps; }

        public double getPeakTps() { return peakTps; }
        public void setPeakTps(double peakTps) { this.peakTps = peakTps; }
    }

    /**
     * Compliance Information
     */
    public static class ComplianceInfo {
        @JsonProperty("gdpr_compliant")
        private boolean gdprCompliant;

        @JsonProperty("hipaa_compliant")
        private boolean hipaaCompliant;

        @JsonProperty("soc2_certified")
        private boolean soc2Certified;

        @JsonProperty("iso27001_certified")
        private boolean iso27001Certified;

        @JsonProperty("audit_logs_enabled")
        private boolean auditLogsEnabled;

        @JsonProperty("data_residency_regions")
        private List<String> dataResidencyRegions;

        public ComplianceInfo() {}

        // Getters and Setters
        public boolean isGdprCompliant() { return gdprCompliant; }
        public void setGdprCompliant(boolean gdprCompliant) { this.gdprCompliant = gdprCompliant; }

        public boolean isHipaaCompliant() { return hipaaCompliant; }
        public void setHipaaCompliant(boolean hipaaCompliant) { this.hipaaCompliant = hipaaCompliant; }

        public boolean isSoc2Certified() { return soc2Certified; }
        public void setSoc2Certified(boolean soc2Certified) { this.soc2Certified = soc2Certified; }

        public boolean isIso27001Certified() { return iso27001Certified; }
        public void setIso27001Certified(boolean iso27001Certified) { this.iso27001Certified = iso27001Certified; }

        public boolean isAuditLogsEnabled() { return auditLogsEnabled; }
        public void setAuditLogsEnabled(boolean auditLogsEnabled) { this.auditLogsEnabled = auditLogsEnabled; }

        public List<String> getDataResidencyRegions() { return dataResidencyRegions; }
        public void setDataResidencyRegions(List<String> dataResidencyRegions) { this.dataResidencyRegions = dataResidencyRegions; }
    }

    /**
     * Support Information
     */
    public static class SupportInfo {
        @JsonProperty("support_tier")
        private String supportTier; // "basic", "professional", "enterprise"

        @JsonProperty("sla_uptime_percent")
        private double slaUptimePercent;

        @JsonProperty("response_time_hours")
        private int responseTimeHours;

        @JsonProperty("support_channels")
        private List<String> supportChannels;

        @JsonProperty("dedicated_manager")
        private boolean dedicatedManager;

        public SupportInfo() {}

        // Getters and Setters
        public String getSupportTier() { return supportTier; }
        public void setSupportTier(String supportTier) { this.supportTier = supportTier; }

        public double getSlaUptimePercent() { return slaUptimePercent; }
        public void setSlaUptimePercent(double slaUptimePercent) { this.slaUptimePercent = slaUptimePercent; }

        public int getResponseTimeHours() { return responseTimeHours; }
        public void setResponseTimeHours(int responseTimeHours) { this.responseTimeHours = responseTimeHours; }

        public List<String> getSupportChannels() { return supportChannels; }
        public void setSupportChannels(List<String> supportChannels) { this.supportChannels = supportChannels; }

        public boolean isDedicatedManager() { return dedicatedManager; }
        public void setDedicatedManager(boolean dedicatedManager) { this.dedicatedManager = dedicatedManager; }
    }
}
