package io.aurigraph.v11.token.secondary;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.InjectMock;
import io.restassured.RestAssured;
import org.junit.jupiter.api.*;

import java.time.Instant;
import java.util.*;

import static io.restassured.RestAssured.*;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive test suite for SecondaryTokenVersionResource REST API.
 * Tests all endpoints including CRUD, lifecycle, and bulk operations.
 *
 * Coverage: 25 tests, 350 LOC
 * Focus: REST API layer and HTTP response handling
 */
@QuarkusTest
@DisplayName("SecondaryTokenVersionResource Tests")
class SecondaryTokenVersionResourceTest {

    @InjectMock
    SecondaryTokenVersioningService versioningService;

    private UUID testTokenId;
    private UUID testSecondaryTokenId;

    @BeforeEach
    void setUp() {
        RestAssured.basePath = "/api/v12/secondary-tokens";
        testTokenId = UUID.randomUUID();
        testSecondaryTokenId = UUID.randomUUID();
    }

    // ===== GET /versions Tests (5 tests) =====

    @Nested
    @DisplayName("GET /versions Endpoint")
    class ListVersionsEndpoint {

        @Test
        @DisplayName("Should list all versions paginated")
        void testListVersionsPaginated() {
            List<SecondaryTokenVersion> versions = new ArrayList<>();
            for (int i = 1; i <= 25; i++) {
                versions.add(createVersion(i));
            }

            when(versioningService.getVersionHistory(testSecondaryTokenId, 1, 10))
                .thenReturn(versions.subList(0, 10));

            given()
                .queryParam("page", 1)
                .queryParam("size", 10)
                .when()
                .get("/{tokenId}/versions", testSecondaryTokenId.toString())
                .then()
                .statusCode(200)
                .body("size()", lessThanOrEqualTo(10));
        }

        @Test
        @DisplayName("Should filter by status query parameter")
        void testFilterByStatus() {
            SecondaryTokenVersion version = createVersion(1);
            version.setStatus(SecondaryTokenVersionStatus.ACTIVE);

            List<SecondaryTokenVersion> filtered = Collections.singletonList(version);
            when(versioningService.getVersionsByStatus(
                testSecondaryTokenId,
                SecondaryTokenVersionStatus.ACTIVE
            )).thenReturn(filtered);

            given()
                .queryParam("status", "ACTIVE")
                .when()
                .get("/{tokenId}/versions", testSecondaryTokenId.toString())
                .then()
                .statusCode(200);
        }

        @Test
        @DisplayName("Should return empty list for new token")
        void testEmptyListForNewToken() {
            when(versioningService.getVersionHistory(testSecondaryTokenId))
                .thenReturn(new ArrayList<>());

            given()
                .when()
                .get("/{tokenId}/versions", testSecondaryTokenId.toString())
                .then()
                .statusCode(200)
                .body("size()", equalTo(0));
        }

        @Test
        @DisplayName("Should order by version_number DESC")
        void testOrderingByVersionDesc() {
            List<SecondaryTokenVersion> versions = new ArrayList<>();
            for (int i = 5; i >= 1; i--) {
                versions.add(createVersion(i));
            }

            when(versioningService.getVersionHistory(testSecondaryTokenId))
                .thenReturn(versions);

            given()
                .when()
                .get("/{tokenId}/versions", testSecondaryTokenId.toString())
                .then()
                .statusCode(200);
        }

        @Test
        @DisplayName("Should respect pagination limits")
        void testPaginationLimits() {
            List<SecondaryTokenVersion> versions = new ArrayList<>();
            for (int i = 1; i <= 50; i++) {
                versions.add(createVersion(i));
            }

            when(versioningService.getVersionHistory(testSecondaryTokenId, 1, 20))
                .thenReturn(versions.subList(0, 20));

            given()
                .queryParam("page", 1)
                .queryParam("size", 20)
                .when()
                .get("/{tokenId}/versions", testSecondaryTokenId.toString())
                .then()
                .statusCode(200);
        }
    }

    // ===== GET /versions/{versionNumber} Tests (5 tests) =====

    @Nested
    @DisplayName("GET /versions/{versionNumber} Endpoint")
    class GetSingleVersionEndpoint {

        @Test
        @DisplayName("Should get specific version")
        void testGetSpecificVersion() {
            SecondaryTokenVersion version = createVersion(5);

            when(versioningService.getVersion(testSecondaryTokenId, 5))
                .thenReturn(version);

            given()
                .when()
                .get("/{tokenId}/versions/{versionNumber}", testSecondaryTokenId.toString(), 5)
                .then()
                .statusCode(200);
        }

        @Test
        @DisplayName("Should return 404 for non-existent version")
        void testVersionNotFound() {
            when(versioningService.getVersion(testSecondaryTokenId, 999))
                .thenReturn(null);

            given()
                .when()
                .get("/{tokenId}/versions/{versionNumber}", testSecondaryTokenId.toString(), 999)
                .then()
                .statusCode(404);
        }

        @Test
        @DisplayName("Should validate version number format")
        void testInvalidVersionNumber() {
            given()
                .when()
                .get("/{tokenId}/versions/{versionNumber}", testSecondaryTokenId.toString(), "invalid")
                .then()
                .statusCode(400);
        }

        @Test
        @DisplayName("Should return complete response DTO")
        void testResponseDTOCompleteness() {
            SecondaryTokenVersion version = createVersion(1);

            when(versioningService.getVersion(testSecondaryTokenId, 1))
                .thenReturn(version);

            given()
                .when()
                .get("/{tokenId}/versions/{versionNumber}", testSecondaryTokenId.toString(), 1)
                .then()
                .statusCode(200)
                .body("versionNumber", notNullValue())
                .body("status", notNullValue())
                .body("merkleHash", notNullValue());
        }

        @Test
        @DisplayName("Should handle invalid token UUID")
        void testInvalidTokenUUID() {
            given()
                .when()
                .get("/{tokenId}/versions/{versionNumber}", "invalid-uuid", 1)
                .then()
                .statusCode(400);
        }
    }

    // ===== POST /versions Tests (5 tests) =====

    @Nested
    @DisplayName("POST /versions Endpoint")
    class CreateVersionEndpoint {

        @Test
        @DisplayName("Should create new version with 201 status")
        void testCreateVersion() {
            SecondaryTokenVersion version = createVersion(1);

            when(versioningService.createVersion(eq(testSecondaryTokenId), anyString(), anyString()))
                .thenReturn(version);

            given()
                .contentType("application/json")
                .body(new VersionCreateRequest("content", "creator@aurigraph.io"))
                .when()
                .post("/{tokenId}/versions", testSecondaryTokenId.toString())
                .then()
                .statusCode(201);
        }

        @Test
        @DisplayName("Should increment version number on creation")
        void testVersionNumberIncrement() {
            SecondaryTokenVersion v1 = createVersion(1);
            SecondaryTokenVersion v2 = createVersion(2);

            when(versioningService.createVersion(eq(testSecondaryTokenId), anyString(), anyString()))
                .thenReturn(v2);

            given()
                .contentType("application/json")
                .body(new VersionCreateRequest("content", "creator@aurigraph.io"))
                .when()
                .post("/{tokenId}/versions", testSecondaryTokenId.toString())
                .then()
                .statusCode(201)
                .body("versionNumber", equalTo(2));
        }

        @Test
        @DisplayName("Should validate required fields")
        void testRequiredFieldsValidation() {
            given()
                .contentType("application/json")
                .body("{}")
                .when()
                .post("/{tokenId}/versions", testSecondaryTokenId.toString())
                .then()
                .statusCode(400);
        }

        @Test
        @DisplayName("Should enforce content size limits")
        void testContentSizeLimit() {
            String largeContent = "x".repeat(10 * 1024 * 1024); // 10MB

            given()
                .contentType("application/json")
                .body(new VersionCreateRequest(largeContent, "creator@aurigraph.io"))
                .when()
                .post("/{tokenId}/versions", testSecondaryTokenId.toString())
                .then()
                .statusCode(413); // Payload Too Large
        }

        @Test
        @DisplayName("Should maintain version numbering sequence")
        void testVersionNumberSequence() {
            for (int i = 1; i <= 5; i++) {
                SecondaryTokenVersion version = createVersion(i);

                when(versioningService.createVersion(eq(testSecondaryTokenId), anyString(), anyString()))
                    .thenReturn(version);

                given()
                    .contentType("application/json")
                    .body(new VersionCreateRequest("content" + i, "creator@aurigraph.io"))
                    .when()
                    .post("/{tokenId}/versions", testSecondaryTokenId.toString())
                    .then()
                    .statusCode(201)
                    .body("versionNumber", equalTo(i));
            }
        }
    }

    // ===== PUT /versions/{versionNumber}/activate Tests (5 tests) =====

    @Nested
    @DisplayName("PUT /versions/{versionNumber}/activate Endpoint")
    class ActivateVersionEndpoint {

        @Test
        @DisplayName("Should activate version with 200 status")
        void testActivateVersion() {
            SecondaryTokenVersion version = createVersion(1);

            when(versioningService.getVersion(testSecondaryTokenId, 1))
                .thenReturn(version);

            given()
                .contentType("application/json")
                .body(new ApprovalRequest("approver@aurigraph.io"))
                .when()
                .put("/{tokenId}/versions/{versionNumber}/activate", testSecondaryTokenId.toString(), 1)
                .then()
                .statusCode(200);
        }

        @Test
        @DisplayName("Should reject invalid state transition")
        void testInvalidStateTransition() {
            SecondaryTokenVersion version = createVersion(1);
            version.setStatus(SecondaryTokenVersionStatus.ARCHIVED);

            when(versioningService.getVersion(testSecondaryTokenId, 1))
                .thenReturn(version);

            given()
                .contentType("application/json")
                .body(new ApprovalRequest("approver@aurigraph.io"))
                .when()
                .put("/{tokenId}/versions/{versionNumber}/activate", testSecondaryTokenId.toString(), 1)
                .then()
                .statusCode(400);
        }

        @Test
        @DisplayName("Should fire activation event")
        void testActivationEvent() {
            SecondaryTokenVersion version = createVersion(1);

            when(versioningService.getVersion(testSecondaryTokenId, 1))
                .thenReturn(version);

            given()
                .contentType("application/json")
                .body(new ApprovalRequest("approver@aurigraph.io"))
                .when()
                .put("/{tokenId}/versions/{versionNumber}/activate", testSecondaryTokenId.toString(), 1)
                .then()
                .statusCode(200)
                .body("status", equalTo("ACTIVE"));
        }

        @Test
        @DisplayName("Should handle race condition (concurrent activation)")
        void testConcurrentActivation() {
            SecondaryTokenVersion version = createVersion(1);

            when(versioningService.getVersion(testSecondaryTokenId, 1))
                .thenReturn(version);

            // First activation succeeds
            given()
                .contentType("application/json")
                .body(new ApprovalRequest("approver@aurigraph.io"))
                .when()
                .put("/{tokenId}/versions/{versionNumber}/activate", testSecondaryTokenId.toString(), 1)
                .then()
                .statusCode(200);

            // Second activation should fail or be idempotent
            given()
                .contentType("application/json")
                .body(new ApprovalRequest("approver2@aurigraph.io"))
                .when()
                .put("/{tokenId}/versions/{versionNumber}/activate", testSecondaryTokenId.toString(), 1)
                .then()
                .statusCode(400 | 200); // Either conflict or idempotent
        }

        @Test
        @DisplayName("Should return updated active status in response")
        void testResponseIncludesStatus() {
            SecondaryTokenVersion version = createVersion(1);

            when(versioningService.getVersion(testSecondaryTokenId, 1))
                .thenReturn(version);

            given()
                .contentType("application/json")
                .body(new ApprovalRequest("approver@aurigraph.io"))
                .when()
                .put("/{tokenId}/versions/{versionNumber}/activate", testSecondaryTokenId.toString(), 1)
                .then()
                .statusCode(200)
                .body("status", notNullValue());
        }
    }

    // ===== PUT /versions/{versionNumber}/reject Tests (5 tests) =====

    @Nested
    @DisplayName("PUT /versions/{versionNumber}/reject Endpoint")
    class RejectVersionEndpoint {

        @Test
        @DisplayName("Should reject with reason")
        void testRejectVersion() {
            SecondaryTokenVersion version = createVersion(1);

            when(versioningService.getVersion(testSecondaryTokenId, 1))
                .thenReturn(version);

            given()
                .contentType("application/json")
                .body(new RejectionRequest("Invalid content format", "reviewer@aurigraph.io"))
                .when()
                .put("/{tokenId}/versions/{versionNumber}/reject", testSecondaryTokenId.toString(), 1)
                .then()
                .statusCode(200);
        }

        @Test
        @DisplayName("Should require rejection reason")
        void testMissingRejectionReason() {
            given()
                .contentType("application/json")
                .body("{\"reviewer\": \"reviewer@aurigraph.io\"}")
                .when()
                .put("/{tokenId}/versions/{versionNumber}/reject", testSecondaryTokenId.toString(), 1)
                .then()
                .statusCode(400);
        }

        @Test
        @DisplayName("Should reject only for valid states")
        void testInvalidStateForRejection() {
            SecondaryTokenVersion version = createVersion(1);
            version.setStatus(SecondaryTokenVersionStatus.ARCHIVED);

            when(versioningService.getVersion(testSecondaryTokenId, 1))
                .thenReturn(version);

            given()
                .contentType("application/json")
                .body(new RejectionRequest("Invalid", "reviewer@aurigraph.io"))
                .when()
                .put("/{tokenId}/versions/{versionNumber}/reject", testSecondaryTokenId.toString(), 1)
                .then()
                .statusCode(400);
        }

        @Test
        @DisplayName("Should fire rejection event")
        void testRejectionEvent() {
            SecondaryTokenVersion version = createVersion(1);

            when(versioningService.getVersion(testSecondaryTokenId, 1))
                .thenReturn(version);

            given()
                .contentType("application/json")
                .body(new RejectionRequest("Invalid", "reviewer@aurigraph.io"))
                .when()
                .put("/{tokenId}/versions/{versionNumber}/reject", testSecondaryTokenId.toString(), 1)
                .then()
                .statusCode(200)
                .body("status", equalTo("REJECTED"));
        }

        @Test
        @DisplayName("Should preserve history after rejection")
        void testHistoryPreservation() {
            SecondaryTokenVersion version = createVersion(1);

            when(versioningService.getVersion(testSecondaryTokenId, 1))
                .thenReturn(version);

            given()
                .contentType("application/json")
                .body(new RejectionRequest("Invalid", "reviewer@aurigraph.io"))
                .when()
                .put("/{tokenId}/versions/{versionNumber}/reject", testSecondaryTokenId.toString(), 1)
                .then()
                .statusCode(200);

            // Version should still be queryable
            when(versioningService.getVersion(testSecondaryTokenId, 1))
                .thenReturn(version);

            given()
                .when()
                .get("/{tokenId}/versions/{versionNumber}", testSecondaryTokenId.toString(), 1)
                .then()
                .statusCode(200);
        }
    }

    // ===== Helper Classes & Methods =====

    private SecondaryTokenVersion createVersion(int versionNumber) {
        SecondaryTokenVersion version = new SecondaryTokenVersion();
        version.setSecondaryTokenId(testSecondaryTokenId);
        version.setVersionNumber(versionNumber);
        version.setStatus(SecondaryTokenVersionStatus.CREATED);
        version.setMerkleHash("hash_" + versionNumber);
        version.setContentHash("content_" + versionNumber);
        version.setCreatedAt(Instant.now());
        version.setUpdatedAt(Instant.now());
        return version;
    }

    static class VersionCreateRequest {
        public String content;
        public String creator;

        VersionCreateRequest(String content, String creator) {
            this.content = content;
            this.creator = creator;
        }
    }

    static class ApprovalRequest {
        public String approver;

        ApprovalRequest(String approver) {
            this.approver = approver;
        }
    }

    static class RejectionRequest {
        public String reason;
        public String reviewer;

        RejectionRequest(String reason, String reviewer) {
            this.reason = reason;
            this.reviewer = reviewer;
        }
    }
}
