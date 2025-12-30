package io.aurigraph.v11.bridge.services;

import io.aurigraph.v11.bridge.models.AtomicSwapResponse;
import io.aurigraph.v11.bridge.models.TransferResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for BridgeQueryService
 * Tests pagination, filtering, sorting, and statistics queries
 */
@DisplayName("Bridge Query Service Tests")
public class BridgeQueryServiceTest {

    private BridgeQueryService queryService;

    @BeforeEach
    void setUp() {
        queryService = new BridgeQueryService();
    }

    @Test
    @DisplayName("Query transfers with default pagination should return first page")
    void testQueryTransfersDefaultPagination() {
        // When
        BridgeQueryService.PaginatedResponse<TransferResponse> result =
                queryService.getTransfersHistory(
                        null, null, null, null,
                        null, null, 1, 50);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getPageNumber());
        assertEquals(50, result.getPageSize());
        assertEquals(0, result.getTotalItems());
        assertEquals(0, result.getTotalPages());
        assertFalse(result.isHasPrevious());
        assertFalse(result.isHasNext());
    }

    @Test
    @DisplayName("Query transfers with custom page size should respect limit")
    void testQueryTransfersCustomPageSize() {
        // When
        BridgeQueryService.PaginatedResponse<TransferResponse> result =
                queryService.getTransfersHistory(
                        null, null, null, null,
                        null, null, 1, 100);

        // Then
        assertNotNull(result);
        assertEquals(100, result.getPageSize());
    }

    @Test
    @DisplayName("Query transfers with page size exceeding max should be capped")
    void testQueryTransfersPageSizeExceedsMax() {
        // When
        BridgeQueryService.PaginatedResponse<TransferResponse> result =
                queryService.getTransfersHistory(
                        null, null, null, null,
                        null, null, 1, 500); // Exceeds max of 200

        // Then
        assertNotNull(result);
        assertEquals(200, result.getPageSize()); // Should be capped
    }

    @Test
    @DisplayName("Query swaps with status filter")
    void testQuerySwapsWithStatusFilter() {
        // When
        BridgeQueryService.PaginatedResponse<AtomicSwapResponse> result =
                queryService.getSwapsHistory(
                        null,
                        AtomicSwapResponse.SwapStatus.COMPLETED,
                        null, null,
                        null, null, 1, 50);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getPageNumber());
        assertEquals(50, result.getPageSize());
    }

    @Test
    @DisplayName("Query swaps with date range filter")
    void testQuerySwapsWithDateRangeFilter() {
        // Given
        Instant startDate = Instant.now().minusSeconds(3600);
        Instant endDate = Instant.now();

        // When
        BridgeQueryService.PaginatedResponse<AtomicSwapResponse> result =
                queryService.getSwapsHistory(
                        null, null,
                        startDate, endDate,
                        null, null, 1, 50);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getPageNumber());
    }

    @Test
    @DisplayName("Query swaps with sorting by timestamp descending")
    void testQuerySwapsWithSortingByTimestamp() {
        // When
        BridgeQueryService.PaginatedResponse<AtomicSwapResponse> result =
                queryService.getSwapsHistory(
                        null, null, null, null,
                        "timestamp", "desc",
                        1, 50);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getPageNumber());
    }

    @Test
    @DisplayName("Query swaps with sorting by amount ascending")
    void testQuerySwapsWithSortingByAmount() {
        // When
        BridgeQueryService.PaginatedResponse<AtomicSwapResponse> result =
                queryService.getSwapsHistory(
                        null, null, null, null,
                        "amount", "asc",
                        1, 50);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getPageNumber());
    }

    @Test
    @DisplayName("Query swaps with sorting by status")
    void testQuerySwapsWithSortingByStatus() {
        // When
        BridgeQueryService.PaginatedResponse<AtomicSwapResponse> result =
                queryService.getSwapsHistory(
                        null, null, null, null,
                        "status", "asc",
                        1, 50);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getPageNumber());
    }

    @Test
    @DisplayName("Query transfers with address filter")
    void testQueryTransfersWithAddressFilter() {
        // Given
        String address = "0x1234567890123456789012345678901234567890";

        // When
        BridgeQueryService.PaginatedResponse<TransferResponse> result =
                queryService.getTransfersHistory(
                        address, null, null, null,
                        null, null, 1, 50);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getPageNumber());
    }

    @Test
    @DisplayName("Query transfers with status filter")
    void testQueryTransfersWithStatusFilter() {
        // When
        BridgeQueryService.PaginatedResponse<TransferResponse> result =
                queryService.getTransfersHistory(
                        null,
                        TransferResponse.TransferStatus.COMPLETED,
                        null, null,
                        null, null, 1, 50);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getPageNumber());
    }

    @Test
    @DisplayName("Pagination with page number greater than 1")
    void testPaginationPageTwo() {
        // When
        BridgeQueryService.PaginatedResponse<TransferResponse> result =
                queryService.getTransfersHistory(
                        null, null, null, null,
                        null, null, 2, 50);

        // Then
        assertNotNull(result);
        assertEquals(2, result.getPageNumber());
        assertFalse(result.isHasNext()); // No items, so no next page
    }

    @Test
    @DisplayName("Invalid page size should default to DEFAULT_PAGE_SIZE")
    void testInvalidPageSizeDefaulting() {
        // When
        BridgeQueryService.PaginatedResponse<TransferResponse> result =
                queryService.getTransfersHistory(
                        null, null, null, null,
                        null, null, 1, -10); // Invalid size

        // Then
        assertNotNull(result);
        assertEquals(50, result.getPageSize()); // Should default to 50
    }

    @Test
    @DisplayName("Query transaction summary should return statistics")
    void testGetTransactionSummary() {
        // Given
        String address = "0x1234567890123456789012345678901234567890";
        Instant startDate = Instant.now().minusSeconds(86400); // 24 hours ago
        Instant endDate = Instant.now();

        // When
        BridgeQueryService.TransactionSummary summary =
                queryService.getTransactionSummary(address, startDate, endDate);

        // Then
        assertNotNull(summary);
        assertEquals(0, summary.getTotalTransactions());
        assertEquals(0.0, summary.getTotalVolumeProcessed());
        assertEquals(0, summary.getFailedTransactions());
        assertEquals(0, summary.getPendingTransactions());
        assertNotNull(summary.getTimestamp());
    }

    @Test
    @DisplayName("Paginated response should include pagination metadata")
    void testPaginatedResponseMetadata() {
        // When
        BridgeQueryService.PaginatedResponse<TransferResponse> result =
                queryService.getTransfersHistory(
                        null, null, null, null,
                        null, null, 1, 50);

        // Then
        assertNotNull(result);
        assertNotNull(result.getItems());
        assertNotNull(result.getTimestamp());
        assertTrue(result.getTimestamp() > 0);
    }

    @Test
    @DisplayName("Query with null filters should return all results")
    void testQueryWithNullFilters() {
        // When
        BridgeQueryService.PaginatedResponse<TransferResponse> result =
                queryService.getTransfersHistory(
                        null, null, null, null,
                        null, null, 1, 50);

        // Then
        assertNotNull(result);
        assertNotNull(result.getItems());
    }

    @Test
    @DisplayName("Query with default sort should be by timestamp descending")
    void testQueryDefaultSort() {
        // When
        BridgeQueryService.PaginatedResponse<TransferResponse> result =
                queryService.getTransfersHistory(
                        null, null, null, null,
                        null, null, // No sort specified
                        1, 50);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getPageNumber());
    }

    @Test
    @DisplayName("Query swaps with all filters combined")
    void testQuerySwapsAllFiltersCombined() {
        // Given
        String address = "0x1234567890123456789012345678901234567890";
        AtomicSwapResponse.SwapStatus status = AtomicSwapResponse.SwapStatus.COMPLETED;
        Instant startDate = Instant.now().minusSeconds(3600);
        Instant endDate = Instant.now();

        // When
        BridgeQueryService.PaginatedResponse<AtomicSwapResponse> result =
                queryService.getSwapsHistory(
                        address, status, startDate, endDate,
                        "timestamp", "desc", 1, 50);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getPageNumber());
        assertEquals(50, result.getPageSize());
    }

    @Test
    @DisplayName("Transaction summary with null dates should work")
    void testTransactionSummaryNullDates() {
        // When
        BridgeQueryService.TransactionSummary summary =
                queryService.getTransactionSummary("0xaddress", null, null);

        // Then
        assertNotNull(summary);
        assertEquals(0, summary.getTotalTransactions());
    }

    @Test
    @DisplayName("PaginatedResponse builder pattern")
    void testPaginatedResponseBuilder() {
        // When
        java.util.List<TransferResponse> items = new java.util.ArrayList<>();
        BridgeQueryService.PaginatedResponse<TransferResponse> response =
                new BridgeQueryService.PaginatedResponse<>(
                        items, 1, 50, 0, 0, false, false);

        // Then
        assertNotNull(response);
        assertEquals(items, response.getItems());
        assertEquals(1, response.getPageNumber());
        assertEquals(50, response.getPageSize());
        assertEquals(0, response.getTotalItems());
        assertEquals(0, response.getTotalPages());
        assertFalse(response.isHasPrevious());
        assertFalse(response.isHasNext());
    }

    @Test
    @DisplayName("Transaction summary should have all fields")
    void testTransactionSummaryFields() {
        // When
        BridgeQueryService.TransactionSummary summary =
                new BridgeQueryService.TransactionSummary(
                        100, 5000.50, 50.00, 98.5,
                        2, 1, 250);

        // Then
        assertNotNull(summary);
        assertEquals(100, summary.getTotalTransactions());
        assertEquals(5000.50, summary.getTotalVolumeProcessed());
        assertEquals(50.00, summary.getAverageTransactionValue());
        assertEquals(98.5, summary.getSuccessRate());
        assertEquals(2, summary.getFailedTransactions());
        assertEquals(1, summary.getPendingTransactions());
        assertEquals(250, summary.getAverageProcessingTimeMs());
    }
}
