package io.aurigraph.v11.crm.service;

import io.aurigraph.v11.crm.dto.ScheduleDemoRequest;
import io.aurigraph.v11.crm.entity.DemoRequest;
import io.aurigraph.v11.crm.entity.Lead;
import io.aurigraph.v11.crm.repository.DemoRequestRepository;
import io.aurigraph.v11.crm.repository.LeadRepository;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Demo Service Unit Tests
 *
 * Tests for demo request lifecycle management:
 * - Creating demo requests
 * - Scheduling demos
 * - Creating meeting links via platform APIs
 * - Sending calendar invites
 * - Completing demos with feedback
 * - Lead scoring updates
 *
 * @author CRM Development Team
 * @since V11.2.0
 */
@QuarkusTest
@DisplayName("Demo Service Tests")
public class DemoServiceTest {

    @Inject
    DemoService demoService;

    @Inject
    LeadService leadService;

    @Inject
    LeadRepository leadRepository;

    @Inject
    DemoRequestRepository demoRepository;

    private UUID testLeadId;
    private Lead testLead;
    private DemoRequest testDemo;

    @BeforeEach
    public void setUp() {
        // Create test lead
        testLead = Lead.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .phoneNumber("+1234567890")
                .companyName("Acme Corp")
                .jobTitle("CEO")
                .source(Lead.LeadSource.WEBSITE_INQUIRY)
                .inquiryType("Platform Demo")
                .companySizeRange("500+")
                .industry("Finance")
                .budgetRange("$100K+")
                .gdprConsentGiven(true)
                .gdprConsentTimestamp(ZonedDateTime.now())
                .status(Lead.LeadStatus.NEW)
                .leadScore(0)
                .createdAt(ZonedDateTime.now())
                .updatedAt(ZonedDateTime.now())
                .build();

        leadRepository.persist(testLead);
        testLeadId = testLead.getId();
    }

    @AfterEach
    public void tearDown() {
        // Clean up test data
        if (testDemo != null) {
            demoRepository.delete(testDemo);
        }
        if (testLead != null) {
            leadRepository.delete(testLead);
        }
    }

    @Test
    @DisplayName("Should create demo request and increment lead score")
    public void testCreateDemoRequest() {
        // Initial lead score is 0
        Lead leadBefore = leadRepository.findById(testLeadId);
        assertEquals(0, leadBefore.getLeadScore());

        // Create demo request
        DemoRequest demo = demoService.createDemoRequest(testLeadId, DemoRequest.DemoType.PLATFORM_DEMO);

        assertNotNull(demo);
        assertNotNull(demo.getId());
        assertEquals(testLeadId, demo.getLeadId());
        assertEquals(DemoRequest.DemoType.PLATFORM_DEMO, demo.getDemoType());
        assertEquals(DemoRequest.DemoStatus.REQUESTED, demo.getStatus());
        assertNotNull(demo.getRequestedAt());

        testDemo = demo;

        // Verify lead score was incremented by 20
        Lead leadAfter = leadRepository.findById(testLeadId);
        assertEquals(20, leadAfter.getLeadScore());
    }

    @Test
    @DisplayName("Should schedule demo with time and duration")
    public void testScheduleDemo() {
        testDemo = demoService.createDemoRequest(testLeadId, DemoRequest.DemoType.PLATFORM_DEMO);

        ZonedDateTime scheduledTime = ZonedDateTime.now().plus(7, ChronoUnit.DAYS);
        ScheduleDemoRequest scheduleRequest = new ScheduleDemoRequest();
        scheduleRequest.setStartTime(scheduledTime);
        scheduleRequest.setDurationMinutes(60);

        demoService.scheduleDemo(testDemo.getId(), scheduleRequest);

        DemoRequest updatedDemo = demoRepository.findById(testDemo.getId());
        assertNotNull(updatedDemo.getScheduledAt());
        assertEquals(scheduledTime, updatedDemo.getStartTime());
        assertEquals(60, updatedDemo.getDurationMinutes());
        assertEquals(DemoRequest.DemoStatus.SCHEDULED, updatedDemo.getStatus());
        assertNotNull(updatedDemo.getEndTime());
    }

    @Test
    @DisplayName("Should create meeting link on platform and return URL")
    public void testCreateMeetingLink() {
        testDemo = demoService.createDemoRequest(testLeadId, DemoRequest.DemoType.PLATFORM_DEMO);

        ZonedDateTime scheduledTime = ZonedDateTime.now().plus(7, ChronoUnit.DAYS);
        ScheduleDemoRequest scheduleRequest = new ScheduleDemoRequest();
        scheduleRequest.setStartTime(scheduledTime);
        scheduleRequest.setDurationMinutes(60);
        demoService.scheduleDemo(testDemo.getId(), scheduleRequest);

        // Create meeting link (stubbed - will use platform API in production)
        String meetingUrl = demoService.createMeetingLink(testDemo.getId(), "zoom");

        assertNotNull(meetingUrl);
        assertFalse(meetingUrl.isEmpty());

        // Verify demo status is now CONFIRMED
        DemoRequest updatedDemo = demoRepository.findById(testDemo.getId());
        assertEquals(DemoRequest.DemoStatus.CONFIRMED, updatedDemo.getStatus());
        assertEquals("zoom", updatedDemo.getMeetingPlatform());
        assertNotNull(updatedDemo.getMeetingUrl());
    }

    @Test
    @DisplayName("Should send calendar invite")
    public void testSendCalendarInvite() {
        testDemo = demoService.createDemoRequest(testLeadId, DemoRequest.DemoType.PLATFORM_DEMO);

        ZonedDateTime scheduledTime = ZonedDateTime.now().plus(7, ChronoUnit.DAYS);
        ScheduleDemoRequest scheduleRequest = new ScheduleDemoRequest();
        scheduleRequest.setStartTime(scheduledTime);
        scheduleRequest.setDurationMinutes(60);
        demoService.scheduleDemo(testDemo.getId(), scheduleRequest);

        demoService.sendCalendarInvite(testDemo.getId());

        DemoRequest updatedDemo = demoRepository.findById(testDemo.getId());
        assertTrue(updatedDemo.isCalendarInviteSent());
        assertNotNull(updatedDemo.getCalendarInviteSentAt());
    }

    @Test
    @DisplayName("Should mark demo as completed and update lead score")
    public void testMarkDemoCompleted() {
        testDemo = demoService.createDemoRequest(testLeadId, DemoRequest.DemoType.PLATFORM_DEMO);

        // Mark as completed with high satisfaction
        demoService.markCompleted(
                testDemo.getId(),
                "https://zoom.us/rec/123456",
                9,  // High satisfaction (0-10)
                "Great demo, very interested",
                DemoRequest.DemoOutcome.VERY_INTERESTED
        );

        // Verify demo status
        DemoRequest updatedDemo = demoRepository.findById(testDemo.getId());
        assertEquals(DemoRequest.DemoStatus.COMPLETED, updatedDemo.getStatus());
        assertEquals(DemoRequest.DemoOutcome.VERY_INTERESTED, updatedDemo.getDemoOutcome());
        assertEquals(9, updatedDemo.getCustomerSatisfactionRating());
        assertEquals("Great demo, very interested", updatedDemo.getCustomerFeedbackText());
        assertTrue(updatedDemo.isFeedbackFormCompleted());
        assertNotNull(updatedDemo.getCompletedAt());
        assertEquals("https://zoom.us/rec/123456", updatedDemo.getRecordingUrl());

        // Verify lead score increased significantly (20 from request + 25 from completion + 75 from high satisfaction)
        Lead updatedLead = leadRepository.findById(testLeadId);
        assertEquals(75, updatedLead.getLeadScore());
        assertEquals(Lead.LeadStatus.QUALIFIED, updatedLead.getStatus());
    }

    @Test
    @DisplayName("Should get pending demos")
    public void testGetPendingDemos() {
        // Create multiple demos in REQUESTED status
        DemoRequest demo1 = demoService.createDemoRequest(testLeadId, DemoRequest.DemoType.PLATFORM_DEMO);
        DemoRequest demo2 = demoService.createDemoRequest(testLeadId, DemoRequest.DemoType.PARTNERSHIP);

        testDemo = demo1;

        List<DemoRequest> pendingDemos = demoService.getPendingDemos();
        assertNotNull(pendingDemos);
        assertTrue(pendingDemos.size() >= 2);
    }

    @Test
    @DisplayName("Should cancel demo with reason")
    public void testCancelDemo() {
        testDemo = demoService.createDemoRequest(testLeadId, DemoRequest.DemoType.PLATFORM_DEMO);

        demoService.cancelDemo(testDemo.getId(), "Customer requested cancellation");

        DemoRequest cancelledDemo = demoRepository.findById(testDemo.getId());
        assertEquals(DemoRequest.DemoStatus.CANCELLED, cancelledDemo.getStatus());
    }

    @Test
    @DisplayName("Should throw exception for non-existent demo")
    public void testScheduleNonExistentDemo() {
        UUID nonExistentId = UUID.randomUUID();
        ScheduleDemoRequest request = new ScheduleDemoRequest();
        request.setStartTime(ZonedDateTime.now());
        request.setDurationMinutes(60);

        assertThrows(IllegalArgumentException.class, () ->
                demoService.scheduleDemo(nonExistentId, request)
        );
    }

    @Test
    @DisplayName("Should require platform for meeting creation")
    public void testCreateMeetingLinkRequiresPlatform() {
        testDemo = demoService.createDemoRequest(testLeadId, DemoRequest.DemoType.PLATFORM_DEMO);

        assertThrows(IllegalArgumentException.class, () ->
                demoService.createMeetingLink(testDemo.getId(), null)
        );
    }

    @Test
    @DisplayName("Should handle multiple demo types")
    public void testMultipleDemoTypes() {
        DemoRequest platformDemo = demoService.createDemoRequest(testLeadId, DemoRequest.DemoType.PLATFORM_DEMO);
        DemoRequest partnershipDemo = demoService.createDemoRequest(testLeadId, DemoRequest.DemoType.PARTNERSHIP);

        assertEquals(DemoRequest.DemoType.PLATFORM_DEMO, platformDemo.getDemoType());
        assertEquals(DemoRequest.DemoType.PARTNERSHIP, partnershipDemo.getDemoType());
    }
}
