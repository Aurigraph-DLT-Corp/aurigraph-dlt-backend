package io.aurigraph.v11.registries;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Registry Aggregation DTO
 *
 * Provides aggregated statistics across all registry types.
 * Combines metrics from Smart Contract, Token, RWA, Merkle Tree, and Compliance registries.
 *
 * @version 11.5.0
 * @since 2025-11-14
 */
public class RegistryAggregation {

    @JsonProperty("totalRegistries")
    private int totalRegistries; // Number of registry types

    @JsonProperty("totalEntries")
    private long totalEntries; // Sum across all registries

    @JsonProperty("totalVerifiedEntries")
    private long totalVerifiedEntries;

    @JsonProperty("totalPendingEntries")
    private long totalPendingEntries;

    @JsonProperty("totalActiveEntries")
    private long totalActiveEntries;

    @JsonProperty("verificationCoverage")
    private double verificationCoverage; // Percentage of verified entries

    @JsonProperty("avgVerificationTime")
    private double avgVerificationTime; // Average across all registries (milliseconds)

    @JsonProperty("lastUpdatedTimestamp")
    private Instant lastUpdatedTimestamp;

    @JsonProperty("registryTypeStats")
    private Map<String, RegistryTypeAggregateData> registryTypeStats = new HashMap<>();

    @JsonProperty("topCategories")
    private Map<String, Long> topCategories = new HashMap<>();

    @JsonProperty("healthStatus")
    private String healthStatus; // HEALTHY, DEGRADED, CRITICAL

    // Constructors

    public RegistryAggregation() {
        this.totalRegistries = 5; // Smart Contract, Token, RWA, Merkle Tree, Compliance
        this.lastUpdatedTimestamp = Instant.now();
    }

    // Getters and Setters

    public int getTotalRegistries() {
        return totalRegistries;
    }

    public void setTotalRegistries(int totalRegistries) {
        this.totalRegistries = totalRegistries;
    }

    public long getTotalEntries() {
        return totalEntries;
    }

    public void setTotalEntries(long totalEntries) {
        this.totalEntries = totalEntries;
    }

    public long getTotalVerifiedEntries() {
        return totalVerifiedEntries;
    }

    public void setTotalVerifiedEntries(long totalVerifiedEntries) {
        this.totalVerifiedEntries = totalVerifiedEntries;
    }

    public long getTotalPendingEntries() {
        return totalPendingEntries;
    }

    public void setTotalPendingEntries(long totalPendingEntries) {
        this.totalPendingEntries = totalPendingEntries;
    }

    public long getTotalActiveEntries() {
        return totalActiveEntries;
    }

    public void setTotalActiveEntries(long totalActiveEntries) {
        this.totalActiveEntries = totalActiveEntries;
    }

    public double getVerificationCoverage() {
        return verificationCoverage;
    }

    public void setVerificationCoverage(double verificationCoverage) {
        this.verificationCoverage = verificationCoverage;
    }

    public double getAvgVerificationTime() {
        return avgVerificationTime;
    }

    public void setAvgVerificationTime(double avgVerificationTime) {
        this.avgVerificationTime = avgVerificationTime;
    }

    public Instant getLastUpdatedTimestamp() {
        return lastUpdatedTimestamp;
    }

    public void setLastUpdatedTimestamp(Instant lastUpdatedTimestamp) {
        this.lastUpdatedTimestamp = lastUpdatedTimestamp;
    }

    public Map<String, RegistryTypeAggregateData> getRegistryTypeStats() {
        return registryTypeStats;
    }

    public void setRegistryTypeStats(Map<String, RegistryTypeAggregateData> registryTypeStats) {
        this.registryTypeStats = registryTypeStats;
    }

    public Map<String, Long> getTopCategories() {
        return topCategories;
    }

    public void setTopCategories(Map<String, Long> topCategories) {
        this.topCategories = topCategories;
    }

    public String getHealthStatus() {
        return healthStatus;
    }

    public void setHealthStatus(String healthStatus) {
        this.healthStatus = healthStatus;
    }

    /**
     * Add registry type stats
     */
    public void addRegistryTypeStat(String registryType, RegistryTypeAggregateData data) {
        this.registryTypeStats.put(registryType, data);
    }

    /**
     * Add top category
     */
    public void addTopCategory(String category, long count) {
        this.topCategories.put(category, count);
    }

    /**
     * Calculate overall verification coverage
     */
    public void calculateVerificationCoverage() {
        if (totalEntries == 0) {
            this.verificationCoverage = 0.0;
        } else {
            this.verificationCoverage = (double) totalVerifiedEntries / totalEntries * 100.0;
        }
    }

    /**
     * Determine health status based on metrics
     */
    public void determineHealthStatus() {
        double coverage = getVerificationCoveragePercentage();

        if (coverage >= 80.0 && totalActiveEntries >= (totalEntries * 0.7)) {
            this.healthStatus = "HEALTHY";
        } else if (coverage >= 50.0 && totalActiveEntries >= (totalEntries * 0.3)) {
            this.healthStatus = "DEGRADED";
        } else {
            this.healthStatus = "CRITICAL";
        }
    }

    /**
     * Get verification coverage as a percentage
     */
    private double getVerificationCoveragePercentage() {
        if (totalEntries == 0) {
            return 0.0;
        }
        return (double) totalVerifiedEntries / totalEntries * 100.0;
    }

    @Override
    public String toString() {
        return "RegistryAggregation{" +
                "totalRegistries=" + totalRegistries +
                ", totalEntries=" + totalEntries +
                ", verificationCoverage=" + verificationCoverage +
                ", healthStatus='" + healthStatus + '\'' +
                '}';
    }

    /**
     * Inner class for per-registry-type aggregate data
     */
    public static class RegistryTypeAggregateData {
        @JsonProperty("registryType")
        private String registryType;

        @JsonProperty("entryCount")
        private long entryCount;

        @JsonProperty("verifiedCount")
        private long verifiedCount;

        @JsonProperty("percentageOfTotal")
        private double percentageOfTotal;

        // Constructors
        public RegistryTypeAggregateData() {
        }

        public RegistryTypeAggregateData(String registryType, long entryCount, long verifiedCount) {
            this.registryType = registryType;
            this.entryCount = entryCount;
            this.verifiedCount = verifiedCount;
        }

        // Getters and Setters
        public String getRegistryType() {
            return registryType;
        }

        public void setRegistryType(String registryType) {
            this.registryType = registryType;
        }

        public long getEntryCount() {
            return entryCount;
        }

        public void setEntryCount(long entryCount) {
            this.entryCount = entryCount;
        }

        public long getVerifiedCount() {
            return verifiedCount;
        }

        public void setVerifiedCount(long verifiedCount) {
            this.verifiedCount = verifiedCount;
        }

        public double getPercentageOfTotal() {
            return percentageOfTotal;
        }

        public void setPercentageOfTotal(double percentageOfTotal) {
            this.percentageOfTotal = percentageOfTotal;
        }

        /**
         * Get verification rate for this registry type
         */
        public double getVerificationRate() {
            if (entryCount == 0) {
                return 0.0;
            }
            return (double) verifiedCount / entryCount * 100.0;
        }

        @Override
        public String toString() {
            return "RegistryTypeAggregateData{" +
                    "registryType='" + registryType + '\'' +
                    ", entryCount=" + entryCount +
                    ", verifiedCount=" + verifiedCount +
                    '}';
        }
    }
}
