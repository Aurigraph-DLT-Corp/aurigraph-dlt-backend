package io.aurigraph.v11.registries;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Registry Statistics DTO
 *
 * Provides statistics and metrics for a specific registry type.
 * Includes counts, verification metrics, and search analytics.
 *
 * @version 11.5.0
 * @since 2025-11-14
 */
public class RegistryStatistics {

    @JsonProperty("registryType")
    private String registryType; // SMART_CONTRACT, TOKEN, RWA, MERKLE_TREE, COMPLIANCE

    @JsonProperty("totalEntries")
    private long totalEntries;

    @JsonProperty("verifiedEntries")
    private long verifiedEntries;

    @JsonProperty("pendingEntries")
    private long pendingEntries;

    @JsonProperty("rejectedEntries")
    private long rejectedEntries;

    @JsonProperty("activeEntries")
    private long activeEntries;

    @JsonProperty("archivedEntries")
    private long archivedEntries;

    @JsonProperty("avgVerificationTime")
    private double avgVerificationTime; // in milliseconds

    @JsonProperty("lastUpdatedTimestamp")
    private Instant lastUpdatedTimestamp;

    @JsonProperty("searchMetrics")
    private SearchMetrics searchMetrics;

    @JsonProperty("categoryBreakdown")
    private Map<String, Long> categoryBreakdown = new HashMap<>();

    @JsonProperty("statusBreakdown")
    private Map<String, Long> statusBreakdown = new HashMap<>();

    // Constructors

    public RegistryStatistics() {
        this.searchMetrics = new SearchMetrics();
        this.lastUpdatedTimestamp = Instant.now();
    }

    public RegistryStatistics(String registryType) {
        this();
        this.registryType = registryType;
    }

    // Getters and Setters

    public String getRegistryType() {
        return registryType;
    }

    public void setRegistryType(String registryType) {
        this.registryType = registryType;
    }

    public long getTotalEntries() {
        return totalEntries;
    }

    public void setTotalEntries(long totalEntries) {
        this.totalEntries = totalEntries;
    }

    public long getVerifiedEntries() {
        return verifiedEntries;
    }

    public void setVerifiedEntries(long verifiedEntries) {
        this.verifiedEntries = verifiedEntries;
    }

    public long getPendingEntries() {
        return pendingEntries;
    }

    public void setPendingEntries(long pendingEntries) {
        this.pendingEntries = pendingEntries;
    }

    public long getRejectedEntries() {
        return rejectedEntries;
    }

    public void setRejectedEntries(long rejectedEntries) {
        this.rejectedEntries = rejectedEntries;
    }

    public long getActiveEntries() {
        return activeEntries;
    }

    public void setActiveEntries(long activeEntries) {
        this.activeEntries = activeEntries;
    }

    public long getArchivedEntries() {
        return archivedEntries;
    }

    public void setArchivedEntries(long archivedEntries) {
        this.archivedEntries = archivedEntries;
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

    public SearchMetrics getSearchMetrics() {
        return searchMetrics;
    }

    public void setSearchMetrics(SearchMetrics searchMetrics) {
        this.searchMetrics = searchMetrics;
    }

    public Map<String, Long> getCategoryBreakdown() {
        return categoryBreakdown;
    }

    public void setCategoryBreakdown(Map<String, Long> categoryBreakdown) {
        this.categoryBreakdown = categoryBreakdown;
    }

    public Map<String, Long> getStatusBreakdown() {
        return statusBreakdown;
    }

    public void setStatusBreakdown(Map<String, Long> statusBreakdown) {
        this.statusBreakdown = statusBreakdown;
    }

    /**
     * Add category breakdown entry
     */
    public void addCategoryBreakdown(String category, long count) {
        this.categoryBreakdown.put(category, count);
    }

    /**
     * Add status breakdown entry
     */
    public void addStatusBreakdown(String status, long count) {
        this.statusBreakdown.put(status, count);
    }

    /**
     * Calculate verification coverage percentage
     */
    public double getVerificationCoverage() {
        if (totalEntries == 0) {
            return 0.0;
        }
        return (double) verifiedEntries / totalEntries * 100.0;
    }

    @Override
    public String toString() {
        return "RegistryStatistics{" +
                "registryType='" + registryType + '\'' +
                ", totalEntries=" + totalEntries +
                ", verifiedEntries=" + verifiedEntries +
                ", activeEntries=" + activeEntries +
                '}';
    }

    /**
     * Inner class for search metrics
     */
    public static class SearchMetrics {
        @JsonProperty("totalSearches")
        private long totalSearches;

        @JsonProperty("uniqueSearchTerms")
        private long uniqueSearchTerms;

        @JsonProperty("avgSearchResultCount")
        private double avgSearchResultCount;

        @JsonProperty("lastSearchTimestamp")
        private Instant lastSearchTimestamp;

        // Constructors
        public SearchMetrics() {
        }

        // Getters and Setters
        public long getTotalSearches() {
            return totalSearches;
        }

        public void setTotalSearches(long totalSearches) {
            this.totalSearches = totalSearches;
        }

        public long getUniqueSearchTerms() {
            return uniqueSearchTerms;
        }

        public void setUniqueSearchTerms(long uniqueSearchTerms) {
            this.uniqueSearchTerms = uniqueSearchTerms;
        }

        public double getAvgSearchResultCount() {
            return avgSearchResultCount;
        }

        public void setAvgSearchResultCount(double avgSearchResultCount) {
            this.avgSearchResultCount = avgSearchResultCount;
        }

        public Instant getLastSearchTimestamp() {
            return lastSearchTimestamp;
        }

        public void setLastSearchTimestamp(Instant lastSearchTimestamp) {
            this.lastSearchTimestamp = lastSearchTimestamp;
        }

        @Override
        public String toString() {
            return "SearchMetrics{" +
                    "totalSearches=" + totalSearches +
                    ", uniqueSearchTerms=" + uniqueSearchTerms +
                    '}';
        }
    }
}
