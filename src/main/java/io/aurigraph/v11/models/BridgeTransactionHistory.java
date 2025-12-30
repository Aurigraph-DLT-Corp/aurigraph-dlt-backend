package io.aurigraph.v11.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Bridge Transaction History with Pagination
 * Used for paginated transaction history responses
 *
 * @author Aurigraph V11
 * @version 11.3.0
 */
public class BridgeTransactionHistory {

    @JsonProperty("transactions")
    private List<BridgeTransaction> transactions;

    @JsonProperty("pagination")
    private PaginationInfo pagination;

    @JsonProperty("filters_applied")
    private FiltersApplied filtersApplied;

    @JsonProperty("summary")
    private HistorySummary summary;

    // Constructor
    public BridgeTransactionHistory() {}

    // Getters and Setters
    public List<BridgeTransaction> getTransactions() { return transactions; }
    public void setTransactions(List<BridgeTransaction> transactions) { this.transactions = transactions; }

    public PaginationInfo getPagination() { return pagination; }
    public void setPagination(PaginationInfo pagination) { this.pagination = pagination; }

    public FiltersApplied getFiltersApplied() { return filtersApplied; }
    public void setFiltersApplied(FiltersApplied filtersApplied) { this.filtersApplied = filtersApplied; }

    public HistorySummary getSummary() { return summary; }
    public void setSummary(HistorySummary summary) { this.summary = summary; }

    /**
     * Pagination Information
     */
    public static class PaginationInfo {
        @JsonProperty("page")
        private int page;

        @JsonProperty("page_size")
        private int pageSize;

        @JsonProperty("total_pages")
        private int totalPages;

        @JsonProperty("total_records")
        private long totalRecords;

        @JsonProperty("has_next")
        private boolean hasNext;

        @JsonProperty("has_previous")
        private boolean hasPrevious;

        public PaginationInfo() {}

        // Getters and Setters
        public int getPage() { return page; }
        public void setPage(int page) { this.page = page; }

        public int getPageSize() { return pageSize; }
        public void setPageSize(int pageSize) { this.pageSize = pageSize; }

        public int getTotalPages() { return totalPages; }
        public void setTotalPages(int totalPages) { this.totalPages = totalPages; }

        public long getTotalRecords() { return totalRecords; }
        public void setTotalRecords(long totalRecords) { this.totalRecords = totalRecords; }

        public boolean isHasNext() { return hasNext; }
        public void setHasNext(boolean hasNext) { this.hasNext = hasNext; }

        public boolean isHasPrevious() { return hasPrevious; }
        public void setHasPrevious(boolean hasPrevious) { this.hasPrevious = hasPrevious; }
    }

    /**
     * Filters Applied
     */
    public static class FiltersApplied {
        @JsonProperty("source_chain")
        private String sourceChain;

        @JsonProperty("target_chain")
        private String targetChain;

        @JsonProperty("asset")
        private String asset;

        @JsonProperty("status")
        private String status;

        @JsonProperty("user_address")
        private String userAddress;

        @JsonProperty("min_amount_usd")
        private Double minAmountUsd;

        @JsonProperty("max_amount_usd")
        private Double maxAmountUsd;

        public FiltersApplied() {}

        // Getters and Setters
        public String getSourceChain() { return sourceChain; }
        public void setSourceChain(String sourceChain) { this.sourceChain = sourceChain; }

        public String getTargetChain() { return targetChain; }
        public void setTargetChain(String targetChain) { this.targetChain = targetChain; }

        public String getAsset() { return asset; }
        public void setAsset(String asset) { this.asset = asset; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public String getUserAddress() { return userAddress; }
        public void setUserAddress(String userAddress) { this.userAddress = userAddress; }

        public Double getMinAmountUsd() { return minAmountUsd; }
        public void setMinAmountUsd(Double minAmountUsd) { this.minAmountUsd = minAmountUsd; }

        public Double getMaxAmountUsd() { return maxAmountUsd; }
        public void setMaxAmountUsd(Double maxAmountUsd) { this.maxAmountUsd = maxAmountUsd; }
    }

    /**
     * History Summary
     */
    public static class HistorySummary {
        @JsonProperty("total_transactions")
        private long totalTransactions;

        @JsonProperty("total_volume_usd")
        private double totalVolumeUsd;

        @JsonProperty("completed_count")
        private long completedCount;

        @JsonProperty("pending_count")
        private long pendingCount;

        @JsonProperty("failed_count")
        private long failedCount;

        @JsonProperty("average_duration_seconds")
        private double averageDurationSeconds;

        public HistorySummary() {}

        // Getters and Setters
        public long getTotalTransactions() { return totalTransactions; }
        public void setTotalTransactions(long totalTransactions) { this.totalTransactions = totalTransactions; }

        public double getTotalVolumeUsd() { return totalVolumeUsd; }
        public void setTotalVolumeUsd(double totalVolumeUsd) { this.totalVolumeUsd = totalVolumeUsd; }

        public long getCompletedCount() { return completedCount; }
        public void setCompletedCount(long completedCount) { this.completedCount = completedCount; }

        public long getPendingCount() { return pendingCount; }
        public void setPendingCount(long pendingCount) { this.pendingCount = pendingCount; }

        public long getFailedCount() { return failedCount; }
        public void setFailedCount(long failedCount) { this.failedCount = failedCount; }

        public double getAverageDurationSeconds() { return averageDurationSeconds; }
        public void setAverageDurationSeconds(double averageDurationSeconds) { this.averageDurationSeconds = averageDurationSeconds; }
    }
}
