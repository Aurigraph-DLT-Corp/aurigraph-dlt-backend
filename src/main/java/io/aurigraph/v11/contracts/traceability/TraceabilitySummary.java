package io.aurigraph.v11.contracts.traceability;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Traceability Summary DTO
 *
 * Aggregated metrics for the entire contract-asset traceability registry.
 * Provides high-level statistics about contracts, assets, tokens, and system health.
 *
 * @version 1.0.0
 * @author Aurigraph V11 Development Team
 */
public class TraceabilitySummary {

    @JsonProperty("totalLinks")
    private Long totalLinks;

    @JsonProperty("totalContracts")
    private Long totalContracts;

    @JsonProperty("totalAssets")
    private Long totalAssets;

    @JsonProperty("totalTokens")
    private Long totalTokens;

    @JsonProperty("averageLinkSuccessRate")
    private Double averageLinkSuccessRate;

    @JsonProperty("totalAssetValue")
    private Double totalAssetValue;

    // Constructors
    public TraceabilitySummary() {
        this.totalLinks = 0L;
        this.totalContracts = 0L;
        this.totalAssets = 0L;
        this.totalTokens = 0L;
        this.averageLinkSuccessRate = 0.0;
        this.totalAssetValue = 0.0;
    }

    // Getters and Setters
    public Long getTotalLinks() {
        return totalLinks;
    }

    public void setTotalLinks(Long totalLinks) {
        this.totalLinks = totalLinks;
    }

    public Long getTotalContracts() {
        return totalContracts;
    }

    public void setTotalContracts(Long totalContracts) {
        this.totalContracts = totalContracts;
    }

    public Long getTotalAssets() {
        return totalAssets;
    }

    public void setTotalAssets(Long totalAssets) {
        this.totalAssets = totalAssets;
    }

    public Long getTotalTokens() {
        return totalTokens;
    }

    public void setTotalTokens(Long totalTokens) {
        this.totalTokens = totalTokens;
    }

    public Double getAverageLinkSuccessRate() {
        return averageLinkSuccessRate;
    }

    public void setAverageLinkSuccessRate(Double averageLinkSuccessRate) {
        this.averageLinkSuccessRate = averageLinkSuccessRate;
    }

    public Double getTotalAssetValue() {
        return totalAssetValue;
    }

    public void setTotalAssetValue(Double totalAssetValue) {
        this.totalAssetValue = totalAssetValue;
    }

    @Override
    public String toString() {
        return String.format(
            "TraceabilitySummary{links=%d, contracts=%d, assets=%d, tokens=%d, avgSuccess=%.2f%%, totalValue=$%.2f}",
            totalLinks,
            totalContracts,
            totalAssets,
            totalTokens,
            averageLinkSuccessRate,
            totalAssetValue
        );
    }
}
