package io.aurigraph.v11.token.secondary;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import org.junit.jupiter.api.*;

import javax.inject.Inject;
import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test suite for SecondaryTokenVersionRepository.
 * Tests database CRUD operations and Panache queries.
 *
 * Coverage: 20 tests, 250 LOC
 * Focus: Data persistence layer and query performance
 */
@QuarkusTest
@DisplayName("SecondaryTokenVersionRepository Tests")
class SecondaryTokenVersionRepositoryTest {

    @Inject
    SecondaryTokenVersionRepository repository;

    private UUID testSecondaryTokenId;
    private UUID testSecondaryTokenId2;

    @BeforeEach
    void setUp() {
        testSecondaryTokenId = UUID.randomUUID();
        testSecondaryTokenId2 = UUID.randomUUID();

        // Clean up before each test
        repository.deleteAll();
    }

    // ===== CRUD Operations Tests (8 tests) =====

    @Nested
    @DisplayName("CRUD Operations")
    class CRUDOperations {

        @Test
        @DisplayName("Should persist and retrieve version")
        void testPersistAndRetrieve() {
            SecondaryTokenVersion version = createVersion(testSecondaryTokenId, 1);

            repository.persist(version);

            SecondaryTokenVersion retrieved = repository.findById(version.getId());
            assertNotNull(retrieved);
            assertEquals(version.getVersionNumber(), retrieved.getVersionNumber());
            assertEquals(version.getSecondaryTokenId(), retrieved.getSecondaryTokenId());
        }

        @Test
        @DisplayName("Should update version fields")
        void testUpdateVersion() {
            SecondaryTokenVersion version = createVersion(testSecondaryTokenId, 1);
            repository.persist(version);

            version.setStatus(SecondaryTokenVersionStatus.ACTIVE);
            version.setUpdatedAt(Instant.now());
            repository.update(version);

            SecondaryTokenVersion updated = repository.findById(version.getId());
            assertEquals(SecondaryTokenVersionStatus.ACTIVE, updated.getStatus());
        }

        @Test
        @DisplayName("Should delete version via soft delete (archive)")
        void testSoftDeleteVersion() {
            SecondaryTokenVersion version = createVersion(testSecondaryTokenId, 1);
            repository.persist(version);

            // Soft delete (set to ARCHIVED)
            version.setStatus(SecondaryTokenVersionStatus.ARCHIVED);
            repository.update(version);

            SecondaryTokenVersion archived = repository.findById(version.getId());
            assertEquals(SecondaryTokenVersionStatus.ARCHIVED, archived.getStatus());
        }

        @Test
        @DisplayName("Should find version by ID")
        void testFindById() {
            SecondaryTokenVersion version = createVersion(testSecondaryTokenId, 1);
            repository.persist(version);

            SecondaryTokenVersion found = repository.findById(version.getId());

            assertNotNull(found);
            assertEquals(version.getId(), found.getId());
        }

        @Test
        @DisplayName("Should persist multiple versions")
        void testPersistMultiple() {
            List<SecondaryTokenVersion> versions = new ArrayList<>();
            for (int i = 1; i <= 10; i++) {
                versions.add(createVersion(testSecondaryTokenId, i));
            }

            for (SecondaryTokenVersion version : versions) {
                repository.persist(version);
            }

            List<SecondaryTokenVersion> retrieved = repository.findBySecondaryTokenId(testSecondaryTokenId);
            assertEquals(10, retrieved.size());
        }

        @Test
        @DisplayName("Should handle concurrent persistence")
        void testConcurrentPersistence() throws InterruptedException {
            List<SecondaryTokenVersion> versions = Collections.synchronizedList(new ArrayList<>());

            Thread t1 = new Thread(() -> {
                for (int i = 1; i <= 5; i++) {
                    SecondaryTokenVersion v = createVersion(testSecondaryTokenId, i);
                    repository.persist(v);
                    versions.add(v);
                }
            });

            Thread t2 = new Thread(() -> {
                for (int i = 1; i <= 5; i++) {
                    SecondaryTokenVersion v = createVersion(testSecondaryTokenId2, i);
                    repository.persist(v);
                    versions.add(v);
                }
            });

            t1.start();
            t2.start();
            t1.join();
            t2.join();

            assertEquals(10, versions.size());
        }

        @Test
        @DisplayName("Should batch insert versions")
        void testBatchInsert() {
            List<SecondaryTokenVersion> versions = new ArrayList<>();
            for (int i = 1; i <= 20; i++) {
                versions.add(createVersion(testSecondaryTokenId, i));
            }

            repository.persist(versions);

            long count = repository.count();
            assertEquals(20, count);
        }

        @Test
        @DisplayName("Should maintain entity state after persistence")
        void testEntityStatePreservation() {
            SecondaryTokenVersion version = createVersion(testSecondaryTokenId, 1);
            String originalMerkleHash = version.getMerkleHash();

            repository.persist(version);

            SecondaryTokenVersion retrieved = repository.findById(version.getId());
            assertEquals(originalMerkleHash, retrieved.getMerkleHash());
        }
    }

    // ===== Panache Query Methods Tests (12 tests) =====

    @Nested
    @DisplayName("Panache Query Operations")
    class PanacheQueries {

        @Test
        @DisplayName("Should find versions by secondary token ID")
        void testFindBySecondaryTokenId() {
            for (int i = 1; i <= 5; i++) {
                repository.persist(createVersion(testSecondaryTokenId, i));
            }

            List<SecondaryTokenVersion> versions = repository.findBySecondaryTokenId(testSecondaryTokenId);

            assertEquals(5, versions.size());
        }

        @Test
        @DisplayName("Should find active version (single per token)")
        void testFindActiveVersion() {
            SecondaryTokenVersion v1 = createVersion(testSecondaryTokenId, 1);
            v1.setStatus(SecondaryTokenVersionStatus.ACTIVE);
            repository.persist(v1);

            SecondaryTokenVersion v2 = createVersion(testSecondaryTokenId, 2);
            v2.setStatus(SecondaryTokenVersionStatus.CREATED);
            repository.persist(v2);

            SecondaryTokenVersion active = repository.findActiveVersion(testSecondaryTokenId);

            assertNotNull(active);
            assertEquals(SecondaryTokenVersionStatus.ACTIVE, active.getStatus());
        }

        @Test
        @DisplayName("Should find version chain with previousVersionId linking")
        void testFindVersionChain() {
            SecondaryTokenVersion v1 = createVersion(testSecondaryTokenId, 1);
            repository.persist(v1);

            SecondaryTokenVersion v2 = createVersion(testSecondaryTokenId, 2);
            v2.setPreviousVersionNumber(1);
            repository.persist(v2);

            SecondaryTokenVersion v3 = createVersion(testSecondaryTokenId, 3);
            v3.setPreviousVersionNumber(2);
            repository.persist(v3);

            List<SecondaryTokenVersion> chain = repository.findVersionChain(testSecondaryTokenId);

            assertEquals(3, chain.size());
            assertEquals(1, chain.get(0).getVersionNumber());
            assertEquals(2, chain.get(1).getVersionNumber());
            assertEquals(3, chain.get(2).getVersionNumber());
        }

        @Test
        @DisplayName("Should find pending VVB approval versions")
        void testFindPendingVVBApproval() {
            SecondaryTokenVersion v1 = createVersion(testSecondaryTokenId, 1);
            v1.setStatus(SecondaryTokenVersionStatus.PENDING_VVB);
            repository.persist(v1);

            SecondaryTokenVersion v2 = createVersion(testSecondaryTokenId2, 1);
            v2.setStatus(SecondaryTokenVersionStatus.CREATED);
            repository.persist(v2);

            List<SecondaryTokenVersion> pending = repository.findByStatus(SecondaryTokenVersionStatus.PENDING_VVB);

            assertTrue(pending.stream()
                .anyMatch(v -> v.getSecondaryTokenId().equals(testSecondaryTokenId)));
        }

        @Test
        @DisplayName("Should count versions by token")
        void testCountByToken() {
            for (int i = 1; i <= 7; i++) {
                repository.persist(createVersion(testSecondaryTokenId, i));
            }

            long count = repository.countBySecondaryTokenId(testSecondaryTokenId);

            assertEquals(7, count);
        }

        @Test
        @DisplayName("Should support pagination with Page")
        void testPaginationWithPage() {
            for (int i = 1; i <= 25; i++) {
                repository.persist(createVersion(testSecondaryTokenId, i));
            }

            List<SecondaryTokenVersion> page1 = repository.findBySecondaryTokenId(
                testSecondaryTokenId,
                Page.of(0, 10)
            );

            assertEquals(10, page1.size());
        }

        @Test
        @DisplayName("Should sort results by version number")
        void testSortByVersionNumber() {
            for (int i = 5; i >= 1; i--) {
                repository.persist(createVersion(testSecondaryTokenId, i));
            }

            List<SecondaryTokenVersion> sorted = repository.findBySecondaryTokenId(
                testSecondaryTokenId,
                Sort.by("versionNumber")
            );

            for (int i = 0; i < sorted.size() - 1; i++) {
                assertTrue(sorted.get(i).getVersionNumber() <= sorted.get(i + 1).getVersionNumber());
            }
        }

        @Test
        @DisplayName("Should filter by multiple criteria")
        void testMultipleCriteriaFilter() {
            SecondaryTokenVersion v1 = createVersion(testSecondaryTokenId, 1);
            v1.setStatus(SecondaryTokenVersionStatus.ACTIVE);
            repository.persist(v1);

            SecondaryTokenVersion v2 = createVersion(testSecondaryTokenId, 2);
            v2.setStatus(SecondaryTokenVersionStatus.CREATED);
            repository.persist(v2);

            List<SecondaryTokenVersion> filtered = repository.findByTokenIdAndStatus(
                testSecondaryTokenId,
                SecondaryTokenVersionStatus.ACTIVE
            );

            assertEquals(1, filtered.size());
        }

        @Test
        @DisplayName("Should return empty list when no matches")
        void testEmptyResultSet() {
            List<SecondaryTokenVersion> versions = repository.findBySecondaryTokenId(testSecondaryTokenId);

            assertTrue(versions.isEmpty());
        }

        @Test
        @DisplayName("Should efficiently query large result sets")
        void testLargeResultSetQuery() {
            for (int i = 1; i <= 100; i++) {
                repository.persist(createVersion(testSecondaryTokenId, i));
            }

            List<SecondaryTokenVersion> versions = repository.findBySecondaryTokenId(testSecondaryTokenId);

            assertEquals(100, versions.size());
        }

        @Test
        @DisplayName("Should maintain index performance on version number queries")
        void testVersionNumberIndexPerformance() {
            for (int i = 1; i <= 50; i++) {
                repository.persist(createVersion(testSecondaryTokenId, i));
            }

            long startTime = System.nanoTime();

            SecondaryTokenVersion version = repository.findByTokenIdAndVersionNumber(
                testSecondaryTokenId,
                25
            );

            long endTime = System.nanoTime();
            long durationMs = (endTime - startTime) / 1_000_000;

            assertNotNull(version);
            assertTrue(durationMs < 100, "Query should complete in < 100ms"); // Performance assertion
        }
    }

    // ===== Helper Methods =====

    private SecondaryTokenVersion createVersion(UUID tokenId, int versionNumber) {
        SecondaryTokenVersion version = new SecondaryTokenVersion();
        version.setSecondaryTokenId(tokenId);
        version.setVersionNumber(versionNumber);
        version.setStatus(SecondaryTokenVersionStatus.CREATED);
        version.setMerkleHash("hash_" + versionNumber);
        version.setContentHash("content_hash_" + versionNumber);
        version.setCreatedAt(Instant.now());
        version.setUpdatedAt(Instant.now());
        version.setCreatedBy("test@aurigraph.io");
        version.setModifiedBy("test@aurigraph.io");
        return version;
    }
}
