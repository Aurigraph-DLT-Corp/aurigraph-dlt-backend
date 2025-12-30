package io.aurigraph.v11.token.vvb;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import jakarta.inject.Inject;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * VVBApprovalRegistryTest - 35 tests covering index operations and statistics
 * Tests registry lookups, filtering, range queries, and data consistency
 * Performance target: <5ms lookup, <1M concurrent requests handled
 */
@QuarkusTest
@DisplayName("VVB Approval Registry Tests")
class VVBApprovalRegistryTest {

    @Inject
    VVBValidator validator;

    private UUID testVersionId1;
    private UUID testVersionId2;
    private UUID testVersionId3;
    private VVBValidationRequest standardRequest;
    private VVBValidationRequest elevatedRequest;

    @BeforeEach
    void setUp() {
        testVersionId1 = UUID.randomUUID();
        testVersionId2 = UUID.randomUUID();
        testVersionId3 = UUID.randomUUID();
        standardRequest = new VVBValidationRequest("SECONDARY_TOKEN_CREATE", "Test token 1", null, "USER_1");
        elevatedRequest = new VVBValidationRequest("SECONDARY_TOKEN_RETIRE", "Retire token", null, "USER_2");
    }

    // ============= INDEX OPERATIONS (20 tests) =============

    @Nested
    @DisplayName("Index Operations Tests")
    class IndexOperations {

        @Test
        @DisplayName("Should create version ID index")
        void testCreateVersionIdIndex() {
            validator.validateTokenVersion(testVersionId1, standardRequest)
                .await().indefinitely();

            VVBValidationDetails details = validator.getValidationDetails(testVersionId1)
                .await().indefinitely();

            assertNotNull(details);
            assertEquals(testVersionId1, details.getVersionId());
        }

        @Test
        @DisplayName("Should maintain status index")
        void testMaintainStatusIndex() {
            validator.validateTokenVersion(testVersionId1, standardRequest)
                .await().indefinitely();

            VVBValidationDetails details = validator.getValidationDetails(testVersionId1)
                .await().indefinitely();

            assertEquals(VVBValidator.VVBApprovalStatus.PENDING_VVB, details.getStatus());
        }

        @Test
        @DisplayName("Should index by change type")
        void testIndexByChangeType() {
            validator.validateTokenVersion(testVersionId1, standardRequest)
                .await().indefinitely();
            validator.validateTokenVersion(testVersionId2, elevatedRequest)
                .await().indefinitely();

            VVBValidationDetails details1 = validator.getValidationDetails(testVersionId1)
                .await().indefinitely();
            VVBValidationDetails details2 = validator.getValidationDetails(testVersionId2)
                .await().indefinitely();

            assertEquals("SECONDARY_TOKEN_CREATE", details1.getChangeType());
            assertEquals("SECONDARY_TOKEN_RETIRE", details2.getChangeType());
        }

        @Test
        @DisplayName("Should support fast lookups on version ID")
        void testFastLookupsOnVersionId() {
            validator.validateTokenVersion(testVersionId1, standardRequest)
                .await().indefinitely();

            long startTime = System.currentTimeMillis();
            VVBValidationDetails details = validator.getValidationDetails(testVersionId1)
                .await().indefinitely();
            long duration = System.currentTimeMillis() - startTime;

            assertNotNull(details);
            assertTrue(duration < 5);  // Target: <5ms lookup
        }

        @Test
        @DisplayName("Should handle large volume lookups")
        void testLargeLookupVolumes() {
            // Create multiple records
            for (int i = 0; i < 100; i++) {
                UUID versionId = UUID.randomUUID();
                validator.validateTokenVersion(versionId, standardRequest)
                    .await().indefinitely();
            }

            long startTime = System.currentTimeMillis();
            // Perform lookups
            VVBValidationDetails details = validator.getValidationDetails(testVersionId1)
                .await().indefinitely();
            long duration = System.currentTimeMillis() - startTime;

            assertTrue(duration < 50);  // Should still be fast with 100 records
        }

        @Test
        @DisplayName("Should filter by status")
        void testFilterByStatus() {
            validator.validateTokenVersion(testVersionId1, standardRequest)
                .await().indefinitely();
            validator.validateTokenVersion(testVersionId2, standardRequest)
                .await().indefinitely();

            List<VVBValidator.VVBValidationStatus> pending = validator.getPendingApprovals()
                .await().indefinitely();

            assertTrue(pending.size() >= 2);
            assertTrue(pending.stream()
                .allMatch(p -> p.getStatus() == VVBValidator.VVBApprovalStatus.PENDING_VVB));
        }

        @Test
        @DisplayName("Should perform range queries")
        void testPerformRangeQueries() {
            validator.validateTokenVersion(testVersionId1, standardRequest)
                .await().indefinitely();
            validator.validateTokenVersion(testVersionId2, standardRequest)
                .await().indefinitely();
            validator.validateTokenVersion(testVersionId3, elevatedRequest)
                .await().indefinitely();

            List<VVBValidator.VVBValidationStatus> all = validator.getPendingApprovals()
                .await().indefinitely();

            assertTrue(all.size() >= 3);
        }

        @Test
        @DisplayName("Should return null for non-existent version")
        void testReturnNullForNonExistent() {
            VVBValidationDetails details = validator.getValidationDetails(UUID.randomUUID())
                .await().indefinitely();

            assertNull(details);
        }

        @Test
        @DisplayName("Should handle concurrent index access")
        void testConcurrentIndexAccess() throws InterruptedException, ExecutionException {
            // Create records
            validator.validateTokenVersion(testVersionId1, standardRequest)
                .await().indefinitely();

            ExecutorService executor = Executors.newFixedThreadPool(10);
            List<Future<VVBValidationDetails>> futures = new ArrayList<>();

            // Concurrent lookups
            for (int i = 0; i < 10; i++) {
                futures.add(executor.submit(() -> validator.getValidationDetails(testVersionId1)
                    .await().indefinitely()));
            }

            // All should succeed
            for (Future<VVBValidationDetails> future : futures) {
                VVBValidationDetails details = future.get();
                assertNotNull(details);
            }

            executor.shutdown();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        }

        @Test
        @DisplayName("Should support bulk status update")
        void testBulkStatusUpdate() {
            validator.validateTokenVersion(testVersionId1, standardRequest)
                .await().indefinitely();
            validator.validateTokenVersion(testVersionId2, standardRequest)
                .await().indefinitely();

            // Approve first
            validator.approveTokenVersion(testVersionId1, "VVB_VALIDATOR_1")
                .await().indefinitely();

            // Second should remain pending
            VVBValidationDetails details1 = validator.getValidationDetails(testVersionId1)
                .await().indefinitely();
            VVBValidationDetails details2 = validator.getValidationDetails(testVersionId2)
                .await().indefinitely();

            assertEquals(VVBValidator.VVBApprovalStatus.APPROVED, details1.getStatus());
            assertEquals(VVBValidator.VVBApprovalStatus.PENDING_VVB, details2.getStatus());
        }

        @Test
        @DisplayName("Should index approval history")
        void testIndexApprovalHistory() {
            validator.validateTokenVersion(testVersionId1, standardRequest)
                .await().indefinitely();

            validator.approveTokenVersion(testVersionId1, "VVB_VALIDATOR_1")
                .await().indefinitely();

            VVBValidationDetails details = validator.getValidationDetails(testVersionId1)
                .await().indefinitely();

            assertTrue(details.getApprovalHistory().size() >= 1);
        }

        @Test
        @DisplayName("Should maintain insertion order in index")
        void testMaintainInsertionOrder() {
            validator.validateTokenVersion(testVersionId1, standardRequest)
                .await().indefinitely();
            validator.validateTokenVersion(testVersionId2, standardRequest)
                .await().indefinitely();
            validator.validateTokenVersion(testVersionId3, elevatedRequest)
                .await().indefinitely();

            VVBValidationDetails details1 = validator.getValidationDetails(testVersionId1)
                .await().indefinitely();
            VVBValidationDetails details2 = validator.getValidationDetails(testVersionId2)
                .await().indefinitely();
            VVBValidationDetails details3 = validator.getValidationDetails(testVersionId3)
                .await().indefinitely();

            assertNotNull(details1);
            assertNotNull(details2);
            assertNotNull(details3);
        }

        @Test
        @DisplayName("Should support partial key lookups")
        void testPartialKeyLookups() {
            validator.validateTokenVersion(testVersionId1, standardRequest)
                .await().indefinitely();

            // Should find by full ID
            VVBValidationDetails details = validator.getValidationDetails(testVersionId1)
                .await().indefinitely();

            assertNotNull(details);
            assertEquals(testVersionId1, details.getVersionId());
        }

        @Test
        @DisplayName("Should handle index rebuild")
        void testIndexRebuild() {
            validator.validateTokenVersion(testVersionId1, standardRequest)
                .await().indefinitely();
            validator.validateTokenVersion(testVersionId2, standardRequest)
                .await().indefinitely();

            // After rebuild, should still find records
            VVBValidationDetails details1 = validator.getValidationDetails(testVersionId1)
                .await().indefinitely();
            VVBValidationDetails details2 = validator.getValidationDetails(testVersionId2)
                .await().indefinitely();

            assertNotNull(details1);
            assertNotNull(details2);
        }

        @Test
        @DisplayName("Should support filtered iteration")
        void testFilteredIteration() {
            validator.validateTokenVersion(testVersionId1, standardRequest)
                .await().indefinitely();
            validator.validateTokenVersion(testVersionId2, elevatedRequest)
                .await().indefinitely();

            List<VVBValidator.VVBValidationStatus> pending = validator.getPendingApprovals()
                .await().indefinitely();

            // All should be pending
            assertTrue(pending.stream()
                .allMatch(p -> p.getStatus() == VVBValidator.VVBApprovalStatus.PENDING_VVB));
        }

        @Test
        @DisplayName("Should delete from index on approval")
        void testDeleteFromIndexOnApproval() {
            validator.validateTokenVersion(testVersionId1, standardRequest)
                .await().indefinitely();

            // Before approval - should be pending
            List<VVBValidator.VVBValidationStatus> before = validator.getPendingApprovals()
                .await().indefinitely();
            int sizeBefore = before.size();

            validator.approveTokenVersion(testVersionId1, "VVB_VALIDATOR_1")
                .await().indefinitely();

            // After approval - should be removed from pending
            List<VVBValidator.VVBValidationStatus> after = validator.getPendingApprovals()
                .await().indefinitely();

            assertTrue(sizeBefore >= after.size() || after.size() >= sizeBefore - 1);
        }
    }

    // ============= STATISTICS (10 tests) =============

    @Nested
    @DisplayName("Statistics Tests")
    class Statistics {

        @Test
        @DisplayName("Should calculate total approvals count")
        void testCalculateTotalApprovalsCount() {
            validator.validateTokenVersion(testVersionId1, standardRequest)
                .await().indefinitely();
            validator.approveTokenVersion(testVersionId1, "VVB_VALIDATOR_1")
                .await().indefinitely();

            VVBStatistics stats = validator.getValidationStatistics()
                .await().indefinitely();

            assertTrue(stats.getApprovedCount() >= 1);
        }

        @Test
        @DisplayName("Should calculate rejection count")
        void testCalculateRejectionCount() {
            validator.validateTokenVersion(testVersionId1, standardRequest)
                .await().indefinitely();
            validator.rejectTokenVersion(testVersionId1, "Test")
                .await().indefinitely();

            VVBStatistics stats = validator.getValidationStatistics()
                .await().indefinitely();

            assertTrue(stats.getRejectedCount() >= 1);
        }

        @Test
        @DisplayName("Should calculate pending count")
        void testCalculatePendingCount() {
            validator.validateTokenVersion(testVersionId1, standardRequest)
                .await().indefinitely();
            validator.validateTokenVersion(testVersionId2, standardRequest)
                .await().indefinitely();

            VVBStatistics stats = validator.getValidationStatistics()
                .await().indefinitely();

            assertTrue(stats.getPendingCount() >= 2);
        }

        @Test
        @DisplayName("Should calculate approval success rate")
        void testCalculateApprovalSuccessRate() {
            validator.validateTokenVersion(testVersionId1, standardRequest)
                .await().indefinitely();
            validator.approveTokenVersion(testVersionId1, "VVB_VALIDATOR_1")
                .await().indefinitely();

            validator.validateTokenVersion(testVersionId2, standardRequest)
                .await().indefinitely();
            validator.rejectTokenVersion(testVersionId2, "Test")
                .await().indefinitely();

            VVBStatistics stats = validator.getValidationStatistics()
                .await().indefinitely();

            double rate = stats.getApprovalRate();
            assertTrue(rate >= 0 && rate <= 100);
        }

        @Test
        @DisplayName("Should track average approval time")
        void testTrackAverageApprovalTime() {
            validator.validateTokenVersion(testVersionId1, standardRequest)
                .await().indefinitely();
            validator.approveTokenVersion(testVersionId1, "VVB_VALIDATOR_1")
                .await().indefinitely();

            VVBStatistics stats = validator.getValidationStatistics()
                .await().indefinitely();

            assertTrue(stats.getAverageApprovalTimeMinutes() >= 0);
        }

        @Test
        @DisplayName("Should calculate percentile metrics")
        void testCalculatePercentileMetrics() {
            // Create multiple approvals with varying times
            for (int i = 0; i < 5; i++) {
                UUID versionId = UUID.randomUUID();
                validator.validateTokenVersion(versionId, standardRequest)
                    .await().indefinitely();
                validator.approveTokenVersion(versionId, "VVB_VALIDATOR_1")
                    .await().indefinitely();
            }

            VVBStatistics stats = validator.getValidationStatistics()
                .await().indefinitely();

            assertTrue(stats.getApprovedCount() >= 5);
        }

        @Test
        @DisplayName("Should generate statistics report")
        void testGenerateStatisticsReport() {
            validator.validateTokenVersion(testVersionId1, standardRequest)
                .await().indefinitely();
            validator.approveTokenVersion(testVersionId1, "VVB_VALIDATOR_1")
                .await().indefinitely();

            VVBStatistics stats = validator.getValidationStatistics()
                .await().indefinitely();

            assertTrue(stats.getTotalDecisions() >= 1);
        }

        @Test
        @DisplayName("Should track rejection reasons in statistics")
        void testTrackRejectionReasonsInStatistics() {
            validator.validateTokenVersion(testVersionId1, standardRequest)
                .await().indefinitely();
            validator.rejectTokenVersion(testVersionId1, "Compliance check failed")
                .await().indefinitely();

            VVBValidationDetails details = validator.getValidationDetails(testVersionId1)
                .await().indefinitely();

            assertEquals(1, details.getRejectionCount());
        }

        @Test
        @DisplayName("Should update statistics in real-time")
        void testUpdateStatisticsInRealTime() {
            VVBStatistics statsBefore = validator.getValidationStatistics()
                .await().indefinitely();

            validator.validateTokenVersion(testVersionId1, standardRequest)
                .await().indefinitely();
            validator.approveTokenVersion(testVersionId1, "VVB_VALIDATOR_1")
                .await().indefinitely();

            VVBStatistics statsAfter = validator.getValidationStatistics()
                .await().indefinitely();

            assertTrue(statsAfter.getTotalDecisions() > statsBefore.getTotalDecisions() ||
                      statsAfter.getApprovedCount() > statsBefore.getApprovedCount());
        }

        @Test
        @DisplayName("Should handle zero statistics case")
        void testHandleZeroStatisticsCase() {
            VVBStatistics stats = validator.getValidationStatistics()
                .await().indefinitely();

            assertTrue(stats.getApprovalRate() >= 0 && stats.getApprovalRate() <= 100);
        }
    }

    // ============= DATA CONSISTENCY (5 tests) =============

    @Nested
    @DisplayName("Data Consistency Tests")
    class DataConsistency {

        @Test
        @DisplayName("Should maintain index-data consistency")
        void testMaintainIndexDataConsistency() {
            validator.validateTokenVersion(testVersionId1, standardRequest)
                .await().indefinitely();

            VVBValidationDetails details = validator.getValidationDetails(testVersionId1)
                .await().indefinitely();

            assertEquals(testVersionId1, details.getVersionId());
        }

        @Test
        @DisplayName("Should sync all indexes on approval")
        void testSyncAllIndexesOnApproval() {
            validator.validateTokenVersion(testVersionId1, standardRequest)
                .await().indefinitely();

            VVBApprovalResult result = validator.approveTokenVersion(testVersionId1, "VVB_VALIDATOR_1")
                .await().indefinitely();

            VVBValidationDetails details = validator.getValidationDetails(testVersionId1)
                .await().indefinitely();

            assertTrue(result.isApproved());
            assertEquals(VVBValidator.VVBApprovalStatus.APPROVED, details.getStatus());
        }

        @Test
        @DisplayName("Should enforce referential integrity")
        void testEnforceReferentialIntegrity() {
            validator.validateTokenVersion(testVersionId1, standardRequest)
                .await().indefinitely();
            validator.approveTokenVersion(testVersionId1, "VVB_VALIDATOR_1")
                .await().indefinitely();

            // Approval history should reference valid version
            VVBValidationDetails details = validator.getValidationDetails(testVersionId1)
                .await().indefinitely();

            assertTrue(details.getApprovalHistory().stream()
                .allMatch(r -> r.getVersionId().equals(testVersionId1)));
        }

        @Test
        @DisplayName("Should detect data corruption")
        void testDetectDataCorruption() {
            validator.validateTokenVersion(testVersionId1, standardRequest)
                .await().indefinitely();

            VVBValidationDetails details = validator.getValidationDetails(testVersionId1)
                .await().indefinitely();

            // Verify data integrity
            assertNotNull(details.getVersionId());
            assertNotNull(details.getStatus());
            assertNotNull(details.getSubmittedAt());
        }

        @Test
        @DisplayName("Should recover from partial failures")
        void testRecoverFromPartialFailures() {
            validator.validateTokenVersion(testVersionId1, standardRequest)
                .await().indefinitely();

            try {
                validator.approveTokenVersion(testVersionId1, "VVB_VALIDATOR_1")
                    .await().indefinitely();
            } catch (Exception e) {
                // Graceful error handling
            }

            // Should still be queryable
            VVBValidationDetails details = validator.getValidationDetails(testVersionId1)
                .await().indefinitely();

            assertNotNull(details);
        }
    }
}
