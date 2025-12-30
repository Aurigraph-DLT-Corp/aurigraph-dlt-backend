package io.aurigraph.v11.token.secondary;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ApprovalExecutionAuditEntityTest - Audit trail entity tests
 * Tests immutable audit record persistence and querying
 */
@QuarkusTest
@DisplayName("ApprovalExecutionAudit Entity Tests")
class ApprovalExecutionAuditEntityTest {

    @Inject
    ApprovalExecutionAuditRepository auditRepository;

    private UUID testVersionId;
    private UUID testApprovalRequestId;
    private ApprovalExecutionAudit testAudit;

    @BeforeEach
    void setUp() {
        testVersionId = UUID.randomUUID();
        testApprovalRequestId = UUID.randomUUID();
        testAudit = new ApprovalExecutionAudit();
        testAudit.id = UUID.randomUUID();
        testAudit.versionId = testVersionId;
        testAudit.approvalRequestId = testApprovalRequestId;
        testAudit.executionPhase = "INITIATED";
        testAudit.previousStatus = "CREATED";
        testAudit.newStatus = "PENDING_VVB";
        testAudit.executionTimestamp = Instant.now();
    }

    // ============= Field Tests =============

    @Test
    @DisplayName("Should initialize all audit fields")
    void testAuditInitialization() {
        assertNotNull(testAudit.id);
        assertNotNull(testAudit.versionId);
        assertNotNull(testAudit.approvalRequestId);
        assertNotNull(testAudit.executionPhase);
        assertNotNull(testAudit.executionTimestamp);
    }

    @Test
    @DisplayName("Should track execution phase")
    void testExecutionPhase() {
        String[] phases = {"INITIATED", "VALIDATED", "TRANSITIONED", "COMPLETED", "FAILED", "ROLLED_BACK"};
        for (String phase : phases) {
            testAudit.executionPhase = phase;
            assertEquals(phase, testAudit.executionPhase);
        }
    }

    @Test
    @DisplayName("Should track status transitions")
    void testStatusTransitions() {
        testAudit.previousStatus = "PENDING_VVB";
        testAudit.newStatus = "ACTIVE";

        assertEquals("PENDING_VVB", testAudit.previousStatus);
        assertEquals("ACTIVE", testAudit.newStatus);
    }

    @Test
    @DisplayName("Should store executed by information")
    void testExecutedBy() {
        testAudit.executedBy = "system-service";
        assertEquals("system-service", testAudit.executedBy);
    }

    @Test
    @DisplayName("Should store error messages for failures")
    void testErrorMessage() {
        testAudit.executionPhase = "FAILED";
        testAudit.errorMessage = "State transition validation failed";

        assertEquals("State transition validation failed", testAudit.errorMessage);
    }

    @Test
    @DisplayName("Should store metadata as JSON")
    void testMetadata() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("timestamp", "2025-12-23T23:00:00Z");
        metadata.put("duration", "145");
        testAudit.metadata = metadata;

        assertNotNull(testAudit.metadata);
        assertEquals("145", testAudit.metadata.get("duration"));
    }

    // ============= Persistence Tests =============

    @Test
    @DisplayName("Should persist audit record to database")
    void testPersistAudit() {
        auditRepository.persistAndFlush(testAudit);

        List<ApprovalExecutionAudit> found = auditRepository.list("id", testAudit.id);
        assertFalse(found.isEmpty());
        assertEquals(testAudit.id, found.get(0).id);
    }

    @Test
    @DisplayName("Should maintain immutability after persistence")
    void testImmutability() {
        auditRepository.persistAndFlush(testAudit);
        String originalPhase = testAudit.executionPhase;

        // Update should create new record, not modify existing
        ApprovalExecutionAudit audit2 = new ApprovalExecutionAudit();
        audit2.id = UUID.randomUUID();
        audit2.versionId = testVersionId;
        audit2.executionPhase = "TRANSITIONED";
        auditRepository.persistAndFlush(audit2);

        List<ApprovalExecutionAudit> original = auditRepository.list("id", testAudit.id);
        assertEquals(originalPhase, original.get(0).executionPhase);
    }

    // ============= Query Tests =============

    @Test
    @DisplayName("Should find audit records by version ID")
    void testFindByVersionId() {
        auditRepository.persistAndFlush(testAudit);

        List<ApprovalExecutionAudit> audits = auditRepository.list(
            "versionId = ?1", testVersionId);
        assertTrue(audits.size() >= 1);
    }

    @Test
    @DisplayName("Should find audit records by approval request ID")
    void testFindByApprovalRequestId() {
        auditRepository.persistAndFlush(testAudit);

        List<ApprovalExecutionAudit> audits = auditRepository.list(
            "approvalRequestId = ?1", testApprovalRequestId);
        assertTrue(audits.size() >= 1);
    }

    @Test
    @DisplayName("Should find audit records by execution phase")
    void testFindByPhase() {
        auditRepository.persistAndFlush(testAudit);

        List<ApprovalExecutionAudit> audits = auditRepository.list(
            "executionPhase = ?1", "INITIATED");
        assertTrue(audits.size() >= 1);
    }

    @Test
    @DisplayName("Should find failed execution records")
    void testFindFailedRecords() {
        testAudit.executionPhase = "FAILED";
        testAudit.errorMessage = "Validation failed";
        auditRepository.persistAndFlush(testAudit);

        List<ApprovalExecutionAudit> failed = auditRepository.list(
            "executionPhase = ?1", "FAILED");
        assertTrue(failed.stream()
            .anyMatch(a -> a.errorMessage != null && !a.errorMessage.isEmpty()));
    }

    @Test
    @DisplayName("Should retrieve audit trail in chronological order")
    void testChronologicalOrdering() {
        // Create multiple audit records
        for (int i = 0; i < 3; i++) {
            ApprovalExecutionAudit audit = new ApprovalExecutionAudit();
            audit.id = UUID.randomUUID();
            audit.versionId = testVersionId;
            audit.approvalRequestId = testApprovalRequestId;
            audit.executionPhase = String.valueOf(i);
            audit.executionTimestamp = Instant.now().plusSeconds(i);
            auditRepository.persistAndFlush(audit);
        }

        // Query with ORDER BY
        List<ApprovalExecutionAudit> audits = auditRepository.list(
            "versionId = ?1 order by executionTimestamp asc", testVersionId);

        // Verify ordering
        if (audits.size() >= 2) {
            for (int i = 0; i < audits.size() - 1; i++) {
                assertTrue(audits.get(i).executionTimestamp
                    .isBefore(audits.get(i + 1).executionTimestamp));
            }
        }
    }

    // ============= Audit Trail Completeness Tests =============

    @Test
    @DisplayName("Should record all 6 execution phases")
    void testAllExecutionPhases() {
        String[] phases = {"INITIATED", "VALIDATED", "TRANSITIONED", "COMPLETED", "FAILED", "ROLLED_BACK"};
        for (String phase : phases) {
            ApprovalExecutionAudit audit = new ApprovalExecutionAudit();
            audit.id = UUID.randomUUID();
            audit.versionId = testVersionId;
            audit.executionPhase = phase;
            audit.executionTimestamp = Instant.now();
            auditRepository.persistAndFlush(audit);
        }

        List<ApprovalExecutionAudit> all = auditRepository.list("versionId", testVersionId);
        assertTrue(all.size() >= phases.length);
    }

    @Test
    @DisplayName("Should include error details for failed phases")
    void testFailureDetails() {
        testAudit.executionPhase = "FAILED";
        testAudit.errorMessage = "State transition not allowed: PENDING_VVB -> INVALID";
        testAudit.metadata = Map.of("error_code", "STATE_INVALID", "retry_count", "3");

        auditRepository.persistAndFlush(testAudit);

        List<ApprovalExecutionAudit> failed = auditRepository.list(
            "versionId = ?1 and executionPhase = ?2",
            testVersionId, "FAILED");

        assertFalse(failed.isEmpty());
        assertNotNull(failed.get(0).errorMessage);
    }

    // ============= Metadata Tests =============

    @Test
    @DisplayName("Should store execution duration in metadata")
    void testDurationMetadata() {
        testAudit.metadata = Map.of("duration_ms", "145");
        auditRepository.persistAndFlush(testAudit);

        List<ApprovalExecutionAudit> audits = auditRepository.list(
            "versionId", testVersionId);
        assertEquals("145", audits.get(0).metadata.get("duration_ms"));
    }

    @Test
    @DisplayName("Should store approval details in metadata")
    void testApprovalMetadata() {
        testAudit.metadata = Map.of(
            "approved_count", "5",
            "threshold_percentage", "66.67",
            "approvers", "validator1,validator2,validator3"
        );
        auditRepository.persistAndFlush(testAudit);

        List<ApprovalExecutionAudit> audits = auditRepository.list(
            "versionId", testVersionId);
        assertEquals("5", audits.get(0).metadata.get("approved_count"));
    }

    // ============= Performance Tests =============

    @Test
    @DisplayName("Audit persistence should be <30ms")
    void testPersistencePerformance() {
        long start = System.currentTimeMillis();
        for (int i = 0; i < 10; i++) {
            ApprovalExecutionAudit audit = new ApprovalExecutionAudit();
            audit.id = UUID.randomUUID();
            audit.versionId = testVersionId;
            audit.executionPhase = "INITIATED";
            audit.executionTimestamp = Instant.now();
            auditRepository.persistAndFlush(audit);
        }
        long duration = System.currentTimeMillis() - start;

        assertTrue(duration < 300, "Persisting 10 audits took " + duration + "ms");
    }

    @Test
    @DisplayName("Audit trail query should be <50ms")
    void testQueryPerformance() {
        auditRepository.persistAndFlush(testAudit);

        long start = System.currentTimeMillis();
        List<ApprovalExecutionAudit> audits = auditRepository.list(
            "versionId = ?1 order by executionTimestamp asc", testVersionId);
        long duration = System.currentTimeMillis() - start;

        assertTrue(duration < 50, "Query took " + duration + "ms");
    }

    @Test
    @DisplayName("Large audit trail retrieval should be <100ms")
    void testLargeAuditTrailPerformance() {
        // Create 100 audit records
        for (int i = 0; i < 100; i++) {
            ApprovalExecutionAudit audit = new ApprovalExecutionAudit();
            audit.id = UUID.randomUUID();
            audit.versionId = testVersionId;
            audit.executionPhase = "PHASE_" + (i % 6);
            audit.executionTimestamp = Instant.now().plusSeconds(i);
            auditRepository.persistAndFlush(audit);
        }

        long start = System.currentTimeMillis();
        List<ApprovalExecutionAudit> audits = auditRepository.list(
            "versionId = ?1 order by executionTimestamp asc", testVersionId);
        long duration = System.currentTimeMillis() - start;

        assertTrue(duration < 100, "Large query took " + duration + "ms");
        assertEquals(100, audits.size());
    }
}
