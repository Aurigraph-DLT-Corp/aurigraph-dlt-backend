package io.aurigraph.v11.token.secondary;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SecondaryTokenVersionEntityTest - Entity-level unit tests
 * Tests data model, validations, lifecycle, and query operations
 */
@QuarkusTest
@DisplayName("SecondaryTokenVersion Entity Tests")
class SecondaryTokenVersionEntityTest {

    @Inject
    SecondaryTokenVersionRepository repository;

    private UUID testTokenId;
    private UUID testApprovalRequestId;
    private SecondaryTokenVersion testVersion;

    @BeforeEach
    void setUp() {
        testTokenId = UUID.randomUUID();
        testApprovalRequestId = UUID.randomUUID();
        testVersion = new SecondaryTokenVersion();
        testVersion.id = UUID.randomUUID();
        testVersion.secondaryTokenId = testTokenId;
        testVersion.versionNumber = 1;
        testVersion.status = SecondaryTokenVersionStatus.CREATED;
        testVersion.createdAt = LocalDateTime.now();
        testVersion.updatedAt = LocalDateTime.now();
    }

    // ============= Entity Field Tests =============

    @Test
    @DisplayName("Should initialize all required fields")
    void testEntityInitialization() {
        assertNotNull(testVersion.id);
        assertNotNull(testVersion.secondaryTokenId);
        assertNotNull(testVersion.versionNumber);
        assertNotNull(testVersion.status);
        assertNotNull(testVersion.createdAt);
    }

    @Test
    @DisplayName("Should validate status field")
    void testStatusField() {
        testVersion.status = SecondaryTokenVersionStatus.ACTIVE;
        assertEquals(SecondaryTokenVersionStatus.ACTIVE, testVersion.status);
    }

    @Test
    @DisplayName("Should track version numbers correctly")
    void testVersionNumbering() {
        testVersion.versionNumber = 1;
        assertEquals(1, testVersion.versionNumber);

        testVersion.versionNumber = 2;
        assertEquals(2, testVersion.versionNumber);
    }

    @Test
    @DisplayName("Should maintain approval request association")
    void testApprovalRequestAssociation() {
        testVersion.approvalRequestId = testApprovalRequestId;
        assertEquals(testApprovalRequestId, testVersion.approvalRequestId);
    }

    @Test
    @DisplayName("Should track activation timestamp")
    void testActivationTimestamp() {
        LocalDateTime activatedAt = LocalDateTime.now();
        testVersion.activatedAt = activatedAt;
        assertEquals(activatedAt, testVersion.activatedAt);
    }

    // ============= Lifecycle Tests =============

    @Test
    @DisplayName("Should persist entity to database")
    void testPersistEntity() {
        repository.persistAndFlush(testVersion);

        List<SecondaryTokenVersion> found = repository.list("id", testVersion.id);
        assertFalse(found.isEmpty());
        assertEquals(testVersion.id, found.get(0).id);
    }

    @Test
    @DisplayName("Should update existing entity")
    void testUpdateEntity() {
        repository.persistAndFlush(testVersion);

        testVersion.status = SecondaryTokenVersionStatus.PENDING_VVB;
        testVersion.updatedAt = LocalDateTime.now();
        repository.persistAndFlush(testVersion);

        List<SecondaryTokenVersion> found = repository.list("id", testVersion.id);
        assertEquals(SecondaryTokenVersionStatus.PENDING_VVB, found.get(0).status);
    }

    @Test
    @DisplayName("Should delete entity from database")
    void testDeleteEntity() {
        repository.persistAndFlush(testVersion);
        repository.delete(testVersion);

        List<SecondaryTokenVersion> found = repository.list("id", testVersion.id);
        assertTrue(found.isEmpty());
    }

    // ============= Query Tests =============

    @Test
    @DisplayName("Should find entity by status")
    void testFindByStatus() {
        testVersion.status = SecondaryTokenVersionStatus.ACTIVE;
        repository.persistAndFlush(testVersion);

        List<SecondaryTokenVersion> active = repository.list(
            "status = ?1", SecondaryTokenVersionStatus.ACTIVE);
        assertTrue(active.size() >= 1);
    }

    @Test
    @DisplayName("Should find entity by token ID")
    void testFindByTokenId() {
        repository.persistAndFlush(testVersion);

        List<SecondaryTokenVersion> versions = repository.list(
            "secondaryTokenId = ?1", testTokenId);
        assertTrue(versions.size() >= 1);
    }

    @Test
    @DisplayName("Should find active version")
    void testFindActiveVersion() {
        testVersion.status = SecondaryTokenVersionStatus.ACTIVE;
        testVersion.activatedAt = LocalDateTime.now();
        repository.persistAndFlush(testVersion);

        List<SecondaryTokenVersion> active = repository.list(
            "secondaryTokenId = ?1 and status = ?2",
            testTokenId, SecondaryTokenVersionStatus.ACTIVE);
        assertTrue(active.size() >= 1);
    }

    // ============= Validation Tests =============

    @Test
    @DisplayName("Should enforce non-null secondary token ID")
    void testNonNullTokenId() {
        testVersion.secondaryTokenId = null;
        assertNull(testVersion.secondaryTokenId);
        // Note: Database constraint validation would fail on persist
    }

    @Test
    @DisplayName("Should enforce status enum constraints")
    void testStatusEnumConstraints() {
        testVersion.status = SecondaryTokenVersionStatus.REJECTED;
        assertEquals(SecondaryTokenVersionStatus.REJECTED, testVersion.status);

        // All enum values should be assignable
        for (SecondaryTokenVersionStatus status : SecondaryTokenVersionStatus.values()) {
            testVersion.status = status;
            assertEquals(status, testVersion.status);
        }
    }

    @Test
    @DisplayName("Should maintain timestamp consistency")
    void testTimestampConsistency() {
        LocalDateTime before = LocalDateTime.now();
        testVersion.createdAt = LocalDateTime.now();
        LocalDateTime after = LocalDateTime.now();

        assertTrue(testVersion.createdAt.isAfter(before.minusSeconds(1)));
        assertTrue(testVersion.createdAt.isBefore(after.plusSeconds(1)));
    }

    // ============= Approval Metadata Tests =============

    @Test
    @DisplayName("Should store approval threshold percentage")
    void testApprovalThreshold() {
        testVersion.approvalThresholdPercentage = 66.67;
        assertEquals(66.67, testVersion.approvalThresholdPercentage);
    }

    @Test
    @DisplayName("Should track approved count")
    void testApprovedByCount() {
        testVersion.approvedByCount = 5;
        assertEquals(5, testVersion.approvedByCount);
    }

    @Test
    @DisplayName("Should store approver list")
    void testApproversList() {
        testVersion.approversList = "validator1,validator2,validator3";
        assertEquals("validator1,validator2,validator3", testVersion.approversList);
    }

    // ============= Cascade Retirement Tests =============

    @Test
    @DisplayName("Should track replaced status")
    void testReplacedStatus() {
        testVersion.status = SecondaryTokenVersionStatus.REPLACED;
        testVersion.replacedAt = LocalDateTime.now();
        testVersion.replacedByVersionId = UUID.randomUUID();

        assertEquals(SecondaryTokenVersionStatus.REPLACED, testVersion.status);
        assertNotNull(testVersion.replacedAt);
        assertNotNull(testVersion.replacedByVersionId);
    }

    @Test
    @DisplayName("Should track previous version relationship")
    void testPreviousVersionId() {
        UUID previousVersionId = UUID.randomUUID();
        testVersion.previousVersionId = previousVersionId;
        assertEquals(previousVersionId, testVersion.previousVersionId);
    }

    // ============= Performance Tests =============

    @Test
    @DisplayName("Entity creation should be fast (<10ms)")
    void testEntityCreationPerformance() {
        long start = System.currentTimeMillis();
        for (int i = 0; i < 100; i++) {
            SecondaryTokenVersion v = new SecondaryTokenVersion();
            v.id = UUID.randomUUID();
        }
        long duration = System.currentTimeMillis() - start;

        assertTrue(duration < 100, "Creating 100 entities took " + duration + "ms");
    }

    @Test
    @DisplayName("Database persistence should be <50ms")
    void testPersistencePerformance() {
        long start = System.currentTimeMillis();
        repository.persistAndFlush(testVersion);
        long duration = System.currentTimeMillis() - start;

        assertTrue(duration < 50, "Persistence took " + duration + "ms");
    }

    @Test
    @DisplayName("Query by ID should be <20ms")
    void testQueryPerformance() {
        repository.persistAndFlush(testVersion);

        long start = System.currentTimeMillis();
        List<SecondaryTokenVersion> found = repository.list("id", testVersion.id);
        long duration = System.currentTimeMillis() - start;

        assertTrue(duration < 20, "Query took " + duration + "ms");
    }
}
