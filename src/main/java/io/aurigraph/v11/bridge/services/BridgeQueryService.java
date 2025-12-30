package io.aurigraph.v11.bridge.services;

import io.aurigraph.v11.bridge.models.AtomicSwapResponse;
import io.aurigraph.v11.bridge.models.TransferResponse;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Bridge Query Service
 * Handles querying and filtering of bridge transactions, transfers, and swaps
 * Supports pagination, filtering, and sorting
 *
 * @author Backend Development Agent (BDA)
 * @version 1.0
 */
@ApplicationScoped
public class BridgeQueryService {

    private static final Logger LOG = Logger.getLogger(BridgeQueryService.class);

    // Default pagination values
    private static final int DEFAULT_PAGE_SIZE = 50;
    private static final int MAX_PAGE_SIZE = 200;

    /**
     * Get paginated list of transfers with filtering and sorting
     */
    public PaginatedResponse<TransferResponse> getTransfersHistory(
            String address,
            TransferResponse.TransferStatus status,
            Instant startDate,
            Instant endDate,
            String sortBy,
            String sortOrder,
            int pageNumber,
            int pageSize) {

        LOG.infof("Querying transfer history - address: %s, status: %s, page: %d, size: %d",
                 address, status, pageNumber, pageSize);

        // Validate pagination parameters
        int validatedPageSize = Math.min(pageSize, MAX_PAGE_SIZE);
        if (validatedPageSize <= 0) {
            validatedPageSize = DEFAULT_PAGE_SIZE;
        }

        // In production, fetch from database
        // For now, return empty list (database integration pending)
        List<TransferResponse> allTransfers = new ArrayList<>();

        // Apply filters
        List<TransferResponse> filtered = allTransfers.stream()
                .filter(t -> address == null || (t.getSourceTransactionHash() != null && t.getSourceTransactionHash().contains(address)))
                .filter(t -> status == null || t.getStatus() == status)
                .filter(t -> startDate == null || t.getCreatedAt().isAfter(startDate))
                .filter(t -> endDate == null || t.getCreatedAt().isBefore(endDate))
                .collect(Collectors.toList());

        // Apply sorting
        if (sortBy != null && !sortBy.isEmpty()) {
            switch (sortBy.toLowerCase()) {
                case "amount":
                    filtered.sort((a, b) -> {
                        int cmp = a.getAmount().compareTo(b.getAmount());
                        return "desc".equals(sortOrder) ? -cmp : cmp;
                    });
                    break;
                case "timestamp":
                    filtered.sort((a, b) -> {
                        int cmp = a.getCreatedAt().compareTo(b.getCreatedAt());
                        return "desc".equals(sortOrder) ? -cmp : cmp;
                    });
                    break;
                case "status":
                    filtered.sort((a, b) -> {
                        int cmp = a.getStatus().toString().compareTo(b.getStatus().toString());
                        return "desc".equals(sortOrder) ? -cmp : cmp;
                    });
                    break;
                default:
                    filtered.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));
            }
        } else {
            // Default: sort by timestamp descending
            filtered.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));
        }

        // Apply pagination
        int totalItems = filtered.size();
        int totalPages = (totalItems + validatedPageSize - 1) / validatedPageSize;
        int skip = (pageNumber - 1) * validatedPageSize;

        List<TransferResponse> pageItems = filtered.stream()
                .skip(skip)
                .limit(validatedPageSize)
                .collect(Collectors.toList());

        return new PaginatedResponse<>(
                pageItems,
                pageNumber,
                validatedPageSize,
                totalItems,
                totalPages,
                pageNumber > 1,
                pageNumber < totalPages
        );
    }

    /**
     * Get paginated list of atomic swaps with filtering and sorting
     */
    public PaginatedResponse<AtomicSwapResponse> getSwapsHistory(
            String address,
            AtomicSwapResponse.SwapStatus status,
            Instant startDate,
            Instant endDate,
            String sortBy,
            String sortOrder,
            int pageNumber,
            int pageSize) {

        LOG.infof("Querying swap history - address: %s, status: %s, page: %d, size: %d",
                 address, status, pageNumber, pageSize);

        // Validate pagination parameters
        int validatedPageSize = Math.min(pageSize, MAX_PAGE_SIZE);
        if (validatedPageSize <= 0) {
            validatedPageSize = DEFAULT_PAGE_SIZE;
        }

        // In production, fetch from database
        // For now, return empty list (database integration pending)
        List<AtomicSwapResponse> allSwaps = new ArrayList<>();

        // Apply filters
        List<AtomicSwapResponse> filtered = allSwaps.stream()
                .filter(s -> address == null || s.getInitiatorAddress() != null)
                .filter(s -> status == null || s.getStatus() == status)
                .filter(s -> startDate == null || s.getCreatedAt().isAfter(startDate))
                .filter(s -> endDate == null || s.getCreatedAt().isBefore(endDate))
                .collect(Collectors.toList());

        // Apply sorting
        if (sortBy != null && !sortBy.isEmpty()) {
            switch (sortBy.toLowerCase()) {
                case "amount":
                    filtered.sort((a, b) -> {
                        int cmp = a.getAmountIn().compareTo(b.getAmountIn());
                        return "desc".equals(sortOrder) ? -cmp : cmp;
                    });
                    break;
                case "timestamp":
                    filtered.sort((a, b) -> {
                        int cmp = a.getCreatedAt().compareTo(b.getCreatedAt());
                        return "desc".equals(sortOrder) ? -cmp : cmp;
                    });
                    break;
                case "status":
                    filtered.sort((a, b) -> {
                        int cmp = a.getStatus().toString().compareTo(b.getStatus().toString());
                        return "desc".equals(sortOrder) ? -cmp : cmp;
                    });
                    break;
                default:
                    filtered.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));
            }
        } else {
            // Default: sort by timestamp descending
            filtered.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));
        }

        // Apply pagination
        int totalItems = filtered.size();
        int totalPages = (totalItems + validatedPageSize - 1) / validatedPageSize;
        int skip = (pageNumber - 1) * validatedPageSize;

        List<AtomicSwapResponse> pageItems = filtered.stream()
                .skip(skip)
                .limit(validatedPageSize)
                .collect(Collectors.toList());

        return new PaginatedResponse<>(
                pageItems,
                pageNumber,
                validatedPageSize,
                totalItems,
                totalPages,
                pageNumber > 1,
                pageNumber < totalPages
        );
    }

    /**
     * Get transaction summary statistics
     */
    public TransactionSummary getTransactionSummary(
            String address,
            Instant startDate,
            Instant endDate) {

        LOG.infof("Calculating transaction summary for address: %s", address);

        // In production, calculate from database
        // For now, return empty summary
        return new TransactionSummary(
                0,           // totalTransactions
                0.0,         // totalVolumeProcessed
                0.0,         // averageTransactionValue
                0.0,         // successRate
                0,           // failedTransactions
                0,           // pendingTransactions
                0L           // averageProcessingTimeMs
        );
    }

    /**
     * Paginated Response DTO
     */
    public static class PaginatedResponse<T> {
        private List<T> items;
        private int pageNumber;
        private int pageSize;
        private int totalItems;
        private int totalPages;
        private boolean hasPrevious;
        private boolean hasNext;
        private long timestamp;

        public PaginatedResponse(List<T> items, int pageNumber, int pageSize,
                               int totalItems, int totalPages,
                               boolean hasPrevious, boolean hasNext) {
            this.items = items;
            this.pageNumber = pageNumber;
            this.pageSize = pageSize;
            this.totalItems = totalItems;
            this.totalPages = totalPages;
            this.hasPrevious = hasPrevious;
            this.hasNext = hasNext;
            this.timestamp = System.currentTimeMillis();
        }

        // Getters
        public List<T> getItems() { return items; }
        public int getPageNumber() { return pageNumber; }
        public int getPageSize() { return pageSize; }
        public int getTotalItems() { return totalItems; }
        public int getTotalPages() { return totalPages; }
        public boolean isHasPrevious() { return hasPrevious; }
        public boolean isHasNext() { return hasNext; }
        public long getTimestamp() { return timestamp; }
    }

    /**
     * Transaction Summary DTO
     */
    public static class TransactionSummary {
        private int totalTransactions;
        private double totalVolumeProcessed;
        private double averageTransactionValue;
        private double successRate; // percentage 0-100
        private int failedTransactions;
        private int pendingTransactions;
        private long averageProcessingTimeMs;
        private long timestamp;

        public TransactionSummary(int totalTransactions, double totalVolumeProcessed,
                                 double averageTransactionValue, double successRate,
                                 int failedTransactions, int pendingTransactions,
                                 long averageProcessingTimeMs) {
            this.totalTransactions = totalTransactions;
            this.totalVolumeProcessed = totalVolumeProcessed;
            this.averageTransactionValue = averageTransactionValue;
            this.successRate = successRate;
            this.failedTransactions = failedTransactions;
            this.pendingTransactions = pendingTransactions;
            this.averageProcessingTimeMs = averageProcessingTimeMs;
            this.timestamp = System.currentTimeMillis();
        }

        // Getters
        public int getTotalTransactions() { return totalTransactions; }
        public double getTotalVolumeProcessed() { return totalVolumeProcessed; }
        public double getAverageTransactionValue() { return averageTransactionValue; }
        public double getSuccessRate() { return successRate; }
        public int getFailedTransactions() { return failedTransactions; }
        public int getPendingTransactions() { return pendingTransactions; }
        public long getAverageProcessingTimeMs() { return averageProcessingTimeMs; }
        public long getTimestamp() { return timestamp; }
    }
}
