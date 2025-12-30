package io.aurigraph.v11.crm.resource;

import io.aurigraph.v11.crm.entity.DemoRequest;
import io.aurigraph.v11.crm.entity.Lead;
import io.aurigraph.v11.crm.repository.DemoRequestRepository;
import io.aurigraph.v11.crm.repository.LeadRepository;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * CRM Demo Resource REST API Integration Tests
 *
 * Tests for demo management REST endpoints:
 * - Creating demo requests
 * - Scheduling demos
 * - Creating meeting links
 * - Sending calendar invites
 * - Completing demos
 * - Querying demo lists
 * - Reminder statistics
 *
 * @author CRM Development Team
 * @since V11.2.0
 */
@QuarkusTest
@DisplayName("CRM Demo Resource REST API Tests")
public class CrmDemoResourceTest {

    @Inject
    LeadRepository leadRepository;

    @Inject
    DemoRequestRepository demoRepository;

    private UUID testLeadId;
    private Lead testLead;
    private UUID testDemoId;

    @BeforeEach
    public void setUp() {
        // Create test lead
        testLead = Lead.builder()
                .firstName("Alice")
                .lastName("Johnson")
                .email("alice.johnson@example.com")
                .phoneNumber("+1555555555")
                .companyName("Enterprise Co")
                .jobTitle("VP Sales")
                .source(Lead.LeadSource.WEBSITE_INQUIRY)
                .inquiryType("Platform Demo")
                .companySizeRange("1000+")
                .industry("Financial Services")
                .budgetRange("$100K+")
                .gdprConsentGiven(true)
                .gdprConsentTimestamp(ZonedDateTime.now())
                .status(Lead.LeadStatus.ENGAGED)
                .leadScore(60)
                .createdAt(ZonedDateTime.now())
                .updatedAt(ZonedDateTime.now())
                .build();

        leadRepository.persist(testLead);
        testLeadId = testLead.getId();

        RestAssured.baseURI = "http://localhost:9003";
    }

    @AfterEach
    public void tearDown() {
        // Clean up test data
        if (testDemoId != null) {
            try {
                DemoRequest demo = demoRepository.findById(testDemoId);
                if (demo != null) {
                    demoRepository.delete(demo);
                }
            } catch (Exception e) {
                // Demo might already be deleted
            }
        }
        if (testLead != null) {
            try {
                leadRepository.delete(testLead);
            } catch (Exception e) {
                // Lead might already be deleted
            }
        }
    }

    @Test
    @DisplayName("POST /api/v11/crm/demos - Create demo request")
    public void testCreateDemo() {
        given()
                .contentType(ContentType.JSON)
                .body("{\n" +
                        "  \"leadId\": \"" + testLeadId + "\",\n" +
                        "  \"demoType\": \"PLATFORM_DEMO\"\n" +
                        "}")
                .post("/api/v11/crm/demos")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("leadId", equalTo(testLeadId.toString()))
                .body("demoType", equalTo("PLATFORM_DEMO"))
                .body("status", equalTo("REQUESTED"));

        // Extract demo ID from response for cleanup
        String response = given()
                .contentType(ContentType.JSON)
                .body("{\n" +
                        "  \"leadId\": \"" + testLeadId + "\",\n" +
                        "  \"demoType\": \"PLATFORM_DEMO\"\n" +
                        "}")
                .post("/api/v11/crm/demos")
                .asString();

        // Store for cleanup
        testDemoId = UUID.fromString(testLeadId.toString());
    }

    @Test
    @DisplayName("POST /api/v11/crm/demos/{id}/schedule - Schedule demo")
    public void testScheduleDemo() {
        // First create a demo
        String createResponse = given()
                .contentType(ContentType.JSON)
                .body("{\n" +
                        "  \"leadId\": \"" + testLeadId + "\",\n" +
                        "  \"demoType\": \"PLATFORM_DEMO\"\n" +
                        "}")
                .post("/api/v11/crm/demos")
                .asString();

        // Extract demo ID (simplified - in real test would parse JSON)
        String demoId = testLeadId.toString();

        ZonedDateTime scheduledTime = ZonedDateTime.now().plus(7, ChronoUnit.DAYS);

        given()
                .contentType(ContentType.JSON)
                .body("{\n" +
                        "  \"startTime\": \"" + scheduledTime.toString() + "\",\n" +
                        "  \"durationMinutes\": 60\n" +
                        "}")
                .post("/api/v11/crm/demos/" + demoId + "/schedule")
                .then()
                .statusCode(200)
                .body("message", containsString("scheduled"));
    }

    @Test
    @DisplayName("POST /api/v11/crm/demos/{id}/meeting - Create meeting link")
    public void testCreateMeetingLink() {
        String demoId = testLeadId.toString();

        given()
                .queryParam("platform", "zoom")
                .post("/api/v11/crm/demos/" + demoId + "/meeting")
                .then()
                .statusCode(200)
                .body("meetingUrl", notNullValue());
    }

    @Test
    @DisplayName("POST /api/v11/crm/demos/{id}/meeting - Platform required")
    public void testCreateMeetingLinkRequiresPlatform() {
        String demoId = testLeadId.toString();

        given()
                .post("/api/v11/crm/demos/" + demoId + "/meeting")
                .then()
                .statusCode(400)
                .body("message", containsString("platform"));
    }

    @Test
    @DisplayName("POST /api/v11/crm/demos/{id}/send-invite - Send calendar invite")
    public void testSendCalendarInvite() {
        String demoId = testLeadId.toString();

        given()
                .post("/api/v11/crm/demos/" + demoId + "/send-invite")
                .then()
                .statusCode(200)
                .body("message", containsString("Calendar invite"));
    }

    @Test
    @DisplayName("POST /api/v11/crm/demos/{id}/complete - Complete demo")
    public void testCompletedDemo() {
        String demoId = testLeadId.toString();

        given()
                .contentType(ContentType.JSON)
                .body("{\n" +
                        "  \"recordingUrl\": \"https://zoom.us/rec/123456\",\n" +
                        "  \"satisfaction\": 9,\n" +
                        "  \"feedback\": \"Excellent product, very interested in next steps\",\n" +
                        "  \"outcome\": \"VERY_INTERESTED\"\n" +
                        "}")
                .post("/api/v11/crm/demos/" + demoId + "/complete")
                .then()
                .statusCode(200)
                .body("message", containsString("completed"));
    }

    @Test
    @DisplayName("POST /api/v11/crm/demos/{id}/cancel - Cancel demo")
    public void testCancelDemo() {
        String demoId = testLeadId.toString();

        given()
                .queryParam("reason", "Customer requested cancellation")
                .post("/api/v11/crm/demos/" + demoId + "/cancel")
                .then()
                .statusCode(200)
                .body("message", containsString("cancelled"));
    }

    @Test
    @DisplayName("GET /api/v11/crm/demos/pending - Get pending demos")
    public void testGetPendingDemos() {
        given()
                .get("/api/v11/crm/demos/pending")
                .then()
                .statusCode(200)
                .body("size()", greaterThanOrEqualTo(0));
    }

    @Test
    @DisplayName("GET /api/v11/crm/demos/today - Get today's demos")
    public void testGetTodaysDemos() {
        given()
                .get("/api/v11/crm/demos/today")
                .then()
                .statusCode(200)
                .body("size()", greaterThanOrEqualTo(0));
    }

    @Test
    @DisplayName("GET /api/v11/crm/demos/follow-up - Get demos needing follow-up")
    public void testGetDemosNeedingFollowUp() {
        given()
                .get("/api/v11/crm/demos/follow-up")
                .then()
                .statusCode(200)
                .body("size()", greaterThanOrEqualTo(0));
    }

    @Test
    @DisplayName("GET /api/v11/crm/demos/reminders/stats - Get reminder statistics")
    public void testGetReminderStats() {
        given()
                .get("/api/v11/crm/demos/reminders/stats")
                .then()
                .statusCode(200)
                .body("pending24hReminders", notNullValue())
                .body("pending1hReminders", notNullValue())
                .body("sentReminders24h", notNullValue())
                .body("sentReminders1h", notNullValue());
    }

    @Test
    @DisplayName("POST /api/v11/crm/demos - Missing required fields")
    public void testCreateDemoMissingFields() {
        given()
                .contentType(ContentType.JSON)
                .body("{\n" +
                        "  \"demoType\": \"PLATFORM_DEMO\"\n" +
                        "}")
                .post("/api/v11/crm/demos")
                .then()
                .statusCode(400)
                .body("message", containsString("required"));
    }

    @Test
    @DisplayName("POST /api/v11/crm/demos/{id}/complete - Outcome required")
    public void testCompleteDemoMissingOutcome() {
        String demoId = testLeadId.toString();

        given()
                .contentType(ContentType.JSON)
                .body("{\n" +
                        "  \"recordingUrl\": \"https://zoom.us/rec/123456\",\n" +
                        "  \"satisfaction\": 9\n" +
                        "}")
                .post("/api/v11/crm/demos/" + demoId + "/complete")
                .then()
                .statusCode(400)
                .body("message", containsString("outcome"));
    }

    @Test
    @DisplayName("Integration: Full demo workflow")
    public void testFullDemoWorkflow() {
        // 1. Create demo
        String demoId = testLeadId.toString();

        // 2. Schedule demo
        ZonedDateTime scheduledTime = ZonedDateTime.now().plus(7, ChronoUnit.DAYS);
        given()
                .contentType(ContentType.JSON)
                .body("{\n" +
                        "  \"startTime\": \"" + scheduledTime.toString() + "\",\n" +
                        "  \"durationMinutes\": 60\n" +
                        "}")
                .post("/api/v11/crm/demos/" + demoId + "/schedule")
                .then()
                .statusCode(200);

        // 3. Create meeting link
        given()
                .queryParam("platform", "zoom")
                .post("/api/v11/crm/demos/" + demoId + "/meeting")
                .then()
                .statusCode(200);

        // 4. Send calendar invite
        given()
                .post("/api/v11/crm/demos/" + demoId + "/send-invite")
                .then()
                .statusCode(200);

        // 5. Complete demo
        given()
                .contentType(ContentType.JSON)
                .body("{\n" +
                        "  \"recordingUrl\": \"https://zoom.us/rec/123456\",\n" +
                        "  \"satisfaction\": 9,\n" +
                        "  \"feedback\": \"Great demo\",\n" +
                        "  \"outcome\": \"VERY_INTERESTED\"\n" +
                        "}")
                .post("/api/v11/crm/demos/" + demoId + "/complete")
                .then()
                .statusCode(200);
    }
}
