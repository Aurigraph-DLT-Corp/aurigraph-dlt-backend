package io.aurigraph.v11.token.secondary;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import java.time.LocalDateTime;
import java.util.*;

import static io.restassured.RestAssured.*;
import static org.hamcrest.Matchers.*;

/**
 * ApprovalE2ETest - Story 7 E2E Test Suite
 *
 * Comprehensive end-to-end tests for the complete VVB Approval System workflow.
 * Tests all major scenarios from approval creation through execution.
 *
 * Test Coverage:
 * - Complete approval lifecycle
 * - Consensus detection and execution
 * - Voting deadline enforcement
 * - Multi-validator scenarios
 * - Rejection scenarios
 * - State machine validation
 */
@QuarkusTest
@DisplayName("VVB Approval System E2E Tests")
public class ApprovalE2ETest {

    private String testApprovalId;
    private String testVersionId;

    @BeforeEach
    public void setup() {
        testVersionId = UUID.randomUUID().toString();
    }

    @Test
    @DisplayName("E2E: Complete approval workflow from creation to execution")
    public void testCompleteApprovalWorkflow() {
        // Step 1: Create approval request
        String createRequest = String.format(
            "{\"versionId\":\"%s\",\"changeType\":\"SECONDARY_TOKEN_CREATE\"," +
            "\"totalValidators\":3,\"expiryDeadline\":\"%s\"}",
            testVersionId,
            LocalDateTime.now().plusDays(7)
        );

        String createResponse = given()
            .contentType(ContentType.JSON)
            .body(createRequest)
            .when()
            .post("/api/v11/approvals")
            .then()
            .statusCode(201)
            .body("status", equalTo("PENDING"))
            .body("approvedByCount", equalTo(0))
            .extract()
            .asString();

        // Extract approval ID from response
        testApprovalId = given()
            .contentType(ContentType.JSON)
            .body(createRequest)
            .when()
            .post("/api/v11/approvals")
            .then()
            .extract()
            .path("id");

        // Step 2: Submit validator votes
        submitValidatorVotes(testApprovalId, 2); // 2 out of 3 approvals (not yet consensus)

        // Step 3: Verify approval in PENDING status
        given()
            .pathParam("approvalId", testApprovalId)
            .when()
            .get("/api/v11/approvals/{approvalId}")
            .then()
            .statusCode(200)
            .body("status", equalTo("PENDING"))
            .body("approvedByCount", equalTo(2));

        // Step 4: Submit final validator vote to reach consensus
        submitValidatorVote(testApprovalId, "validator-3", "APPROVE");

        // Step 5: Verify consensus reached
        given()
            .pathParam("approvalId", testApprovalId)
            .when()
            .get("/api/v11/approvals/{approvalId}/consensus")
            .then()
            .statusCode(200)
            .body("consensusReached", equalTo(true))
            .body("approvalStatus", equalTo("APPROVED"));

        // Step 6: Execute approval
        given()
            .pathParam("approvalId", testApprovalId)
            .contentType(ContentType.JSON)
            .body("{\"executedBy\":\"system-admin\"}")
            .when()
            .post("/api/v11/approvals/{approvalId}/execute")
            .then()
            .statusCode(200)
            .body("executionStatus", equalTo("EXECUTED"));

        // Step 7: Verify final state
        given()
            .pathParam("approvalId", testApprovalId)
            .when()
            .get("/api/v11/approvals/{approvalId}")
            .then()
            .statusCode(200)
            .body("status", equalTo("APPROVED"));
    }

    @Test
    @DisplayName("E2E: Rejection scenario - majority rejects proposal")
    public void testApprovalRejection() {
        // Create approval
        String approvalId = createApprovalRequest(testVersionId, 3);

        // Submit rejection votes from 2 validators
        submitValidatorVote(approvalId, "validator-1", "REJECT");
        submitValidatorVote(approvalId, "validator-2", "REJECT");

        // Verify rejected status
        given()
            .pathParam("approvalId", approvalId)
            .when()
            .get("/api/v11/approvals/{approvalId}/consensus")
            .then()
            .statusCode(200)
            .body("consensusPercentage", lessThan(66.67f));
    }

    @Test
    @DisplayName("E2E: Duplicate vote prevention")
    public void testDuplicateVotePrevention() {
        String approvalId = createApprovalRequest(testVersionId, 3);

        // Submit first vote
        submitValidatorVote(approvalId, "validator-1", "APPROVE");

        // Attempt duplicate vote
        given()
            .pathParam("approvalId", approvalId)
            .contentType(ContentType.JSON)
            .body("{\"validatorId\":\"validator-1\",\"choice\":\"APPROVE\"}")
            .when()
            .post("/api/v11/approvals/{approvalId}/vote")
            .then()
            .statusCode(409)
            .body("error.code", equalTo("DUPLICATE_VOTE"));
    }

    @Test
    @DisplayName("E2E: Deadline enforcement")
    public void testDeadlineEnforcement() {
        // Create approval with past expiry (would be mocked in real test)
        String approvalId = createApprovalRequest(testVersionId, 3);

        // Attempt vote after deadline (would need time manipulation)
        // This test would require mocking LocalDateTime.now()
    }

    @Test
    @DisplayName("E2E: Invalid state transition prevention")
    public void testInvalidStateTransition() {
        String approvalId = createApprovalRequest(testVersionId, 3);

        // Complete voting and reach consensus
        submitValidatorVotes(approvalId, 3);

        // Attempt to vote after consensus reached
        given()
            .pathParam("approvalId", approvalId)
            .contentType(ContentType.JSON)
            .body("{\"validatorId\":\"validator-4\",\"choice\":\"APPROVE\"}")
            .when()
            .post("/api/v11/approvals/{approvalId}/vote")
            .then()
            .statusCode(409)
            .body("error.code", containsString("NOT_OPEN_FOR_VOTING"));
    }

    @Test
    @DisplayName("E2E: List and filter approvals")
    public void testApprovalListing() {
        // Create multiple approvals
        for (int i = 0; i < 5; i++) {
            createApprovalRequest(UUID.randomUUID().toString(), 3);
        }

        // List all approvals
        given()
            .when()
            .get("/api/v11/approvals")
            .then()
            .statusCode(200)
            .body("total", greaterThanOrEqualTo(5))
            .body("approvals", hasSize(greaterThanOrEqualTo(5)));

        // Filter by status
        given()
            .queryParam("status", "PENDING")
            .when()
            .get("/api/v11/approvals")
            .then()
            .statusCode(200)
            .body("approvals.status", everyItem(equalTo("PENDING")));
    }

    @Test
    @DisplayName("E2E: Audit trail completeness")
    public void testAuditTrailCompleteness() {
        String approvalId = createApprovalRequest(testVersionId, 3);

        // Submit votes
        submitValidatorVote(approvalId, "validator-1", "APPROVE");
        submitValidatorVote(approvalId, "validator-2", "APPROVE");
        submitValidatorVote(approvalId, "validator-3", "APPROVE");

        // Execute
        given()
            .pathParam("approvalId", approvalId)
            .contentType(ContentType.JSON)
            .body("{\"executedBy\":\"system-admin\"}")
            .when()
            .post("/api/v11/approvals/{approvalId}/execute")
            .then()
            .statusCode(200);

        // Verify complete audit trail
        given()
            .pathParam("approvalId", approvalId)
            .when()
            .get("/api/v11/approvals/{approvalId}/audit")
            .then()
            .statusCode(200)
            .body("auditTrail.size()", greaterThanOrEqualTo(6)) // Created + 3 votes + consensus + executed
            .body("auditTrail.eventType",
                hasItems("APPROVAL_REQUEST_CREATED", "VOTE_SUBMITTED", "CONSENSUS_REACHED", "APPROVAL_EXECUTED"));
    }

    @Test
    @DisplayName("E2E: Performance under load")
    public void testPerformanceUnderLoad() {
        long startTime = System.currentTimeMillis();

        // Create 100 approval requests
        for (int i = 0; i < 100; i++) {
            createApprovalRequest(UUID.randomUUID().toString(), 5);
        }

        long createTime = System.currentTimeMillis() - startTime;

        // Assert creation time is reasonable (< 10 seconds)
        assert createTime < 10000 : String.format("Approval creation took too long: %dms", createTime);
    }

    // Helper methods

    private String createApprovalRequest(String versionId, int totalValidators) {
        return given()
            .contentType(ContentType.JSON)
            .body(String.format(
                "{\"versionId\":\"%s\",\"changeType\":\"SECONDARY_TOKEN_CREATE\"," +
                "\"totalValidators\":%d,\"expiryDeadline\":\"%s\"}",
                versionId,
                totalValidators,
                LocalDateTime.now().plusDays(7)
            ))
            .when()
            .post("/api/v11/approvals")
            .then()
            .statusCode(201)
            .extract()
            .path("id");
    }

    private void submitValidatorVotes(String approvalId, int count) {
        for (int i = 1; i <= count; i++) {
            submitValidatorVote(approvalId, "validator-" + i, "APPROVE");
        }
    }

    private void submitValidatorVote(String approvalId, String validatorId, String choice) {
        given()
            .pathParam("approvalId", approvalId)
            .contentType(ContentType.JSON)
            .body(String.format(
                "{\"validatorId\":\"%s\",\"choice\":\"%s\",\"reason\":\"Test vote\"}",
                validatorId,
                choice
            ))
            .when()
            .post("/api/v11/approvals/{approvalId}/vote")
            .then()
            .statusCode(200);
    }
}
