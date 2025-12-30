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
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Reminder Service Unit Tests
 *
 * Tests for scheduled demo reminder processing:
 * - 24-hour pre-demo reminders
 * - 1-hour pre-demo reminders
 * - No-show detection and lead scoring
 * - Reminder statistics tracking
 *
 * @author CRM Development Team
 * @since V11.2.0
 */
@QuarkusTest
@DisplayName("Reminder Service Tests")
public class ReminderServiceTest {

    @Inject
    ReminderService reminderService;

    @Inject
    DemoService demoService;

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
                .firstName("Jane")
                .lastName("Smith")
                .email("jane.smith@example.com")
                .phoneNumber("+1987654321")
                .companyName("Tech Solutions")
                .jobTitle("COO")
                .source(Lead.LeadSource.PARTNER_REFERRAL)
                .inquiryType("Platform Demo")
                .companySizeRange("201-500")
                .industry("Technology")
                .budgetRange("$50K-100K")
                .gdprConsentGiven(true)
                .gdprConsentTimestamp(ZonedDateTime.now())
                .status(Lead.LeadStatus.NEW)
                .leadScore(50)
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
    @DisplayName("Should process 24-hour reminders for upcoming demos")
    public void testProcess24HourReminders() {
        // Create demo scheduled for 22 hours from now
        testDemo = demoService.createDemoRequest(testLeadId, DemoRequest.DemoType.PLATFORM_DEMO);

        ZonedDateTime scheduledTime = ZonedDateTime.now().plus(22, ChronoUnit.HOURS);
        ScheduleDemoRequest request = new ScheduleDemoRequest();
        request.setStartTime(scheduledTime);
        request.setDurationMinutes(60);

        demoService.scheduleDemo(testDemo.getId(), request);

        // Process 24-hour reminders
        reminderService.processReminders24h();

        // Verify reminder was marked as sent
        DemoRequest updatedDemo = demoRepository.findById(testDemo.getId());
        assertTrue(updatedDemo.isReminder24hSent());
        assertNotNull(updatedDemo.getReminder24hSentAt());
    }

    @Test
    @DisplayName("Should process 1-hour reminders for demos starting soon")
    public void testProcess1HourReminders() {
        // Create demo scheduled for 50 minutes from now
        testDemo = demoService.createDemoRequest(testLeadId, DemoRequest.DemoType.PLATFORM_DEMO);

        ZonedDateTime scheduledTime = ZonedDateTime.now().plus(50, ChronoUnit.MINUTES);
        ScheduleDemoRequest request = new ScheduleDemoRequest();
        request.setStartTime(scheduledTime);
        request.setDurationMinutes(60);

        demoService.scheduleDemo(testDemo.getId(), request);

        // Process 1-hour reminders
        reminderService.processReminders1h();

        // Verify reminder was marked as sent
        DemoRequest updatedDemo = demoRepository.findById(testDemo.getId());
        assertTrue(updatedDemo.isReminder1hSent());
        assertNotNull(updatedDemo.getReminder1hSentAt());
    }

    @Test
    @DisplayName("Should not send duplicate 24-hour reminders")
    public void testNoDuplicate24HourReminders() {
        testDemo = demoService.createDemoRequest(testLeadId, DemoRequest.DemoType.PLATFORM_DEMO);

        ZonedDateTime scheduledTime = ZonedDateTime.now().plus(22, ChronoUnit.HOURS);
        ScheduleDemoRequest request = new ScheduleDemoRequest();
        request.setStartTime(scheduledTime);
        request.setDurationMinutes(60);

        demoService.scheduleDemo(testDemo.getId(), request);

        // Process reminders twice
        reminderService.processReminders24h();
        ZonedDateTime firstSentTime = demoRepository.findById(testDemo.getId()).getReminder24hSentAt();

        reminderService.processReminders24h();
        ZonedDateTime secondSentTime = demoRepository.findById(testDemo.getId()).getReminder24hSentAt();

        // Times should be the same (reminder was only sent once)
        assertEquals(firstSentTime, secondSentTime);
    }

    @Test
    @DisplayName("Should mark demos as no-show if not completed after 1 hour")
    public void testProcessMissedDemos() {
        // Create demo that ended 1.5 hours ago
        testDemo = DemoRequest.builder()
                .leadId(testLeadId)
                .demoType(DemoRequest.DemoType.PLATFORM_DEMO)
                .status(DemoRequest.DemoStatus.CONFIRMED)
                .scheduledAt(ZonedDateTime.now().minus(2, ChronoUnit.HOURS))
                .startTime(ZonedDateTime.now().minus(2, ChronoUnit.HOURS))
                .endTime(ZonedDateTime.now().minus(1, ChronoUnit.HOURS).minus(30, ChronoUnit.MINUTES))
                .durationMinutes(60)
                .requestedAt(ZonedDateTime.now().minus(3, ChronoUnit.DAYS))
                .createdAt(ZonedDateTime.now().minus(3, ChronoUnit.DAYS))
                .updatedAt(ZonedDateTime.now())
                .build();

        demoRepository.persist(testDemo);

        // Process missed demos
        reminderService.processMissedDemos();

        // Verify demo is marked as NO_SHOW
        DemoRequest updatedDemo = demoRepository.findById(testDemo.getId());
        assertEquals(DemoRequest.DemoStatus.NO_SHOW, updatedDemo.getStatus());
        assertEquals(DemoRequest.DemoOutcome.NO_SHOW, updatedDemo.getDemoOutcome());
    }

    @Test
    @DisplayName("Should reduce lead score by 15 points for no-show")
    public void testLeadScoreReducedForNoShow() {
        // Create demo that ended more than 1 hour ago
        testDemo = DemoRequest.builder()
                .leadId(testLeadId)
                .demoType(DemoRequest.DemoType.PLATFORM_DEMO)
                .status(DemoRequest.DemoStatus.CONFIRMED)
                .scheduledAt(ZonedDateTime.now().minus(3, ChronoUnit.HOURS))
                .startTime(ZonedDateTime.now().minus(3, ChronoUnit.HOURS))
                .endTime(ZonedDateTime.now().minus(2, ChronoUnit.HOURS))
                .durationMinutes(60)
                .requestedAt(ZonedDateTime.now().minus(3, ChronoUnit.DAYS))
                .createdAt(ZonedDateTime.now().minus(3, ChronoUnit.DAYS))
                .updatedAt(ZonedDateTime.now())
                .build();

        demoRepository.persist(testDemo);

        Lead leadBefore = leadRepository.findById(testLeadId);
        int scoreBefore = leadBefore.getLeadScore();

        // Process missed demos
        reminderService.processMissedDemos();

        // Verify lead score was reduced by 15 points
        Lead leadAfter = leadRepository.findById(testLeadId);
        assertEquals(scoreBefore - 15, leadAfter.getLeadScore());
    }

    @Test
    @DisplayName("Should not reduce score below zero for no-show")
    public void testLeadScoreCappedAtZero() {
        // Create lead with low score
        testLead = leadRepository.findById(testLeadId);
        leadRepository.updateScore(testLeadId, 5);

        // Create demo that ended more than 1 hour ago
        testDemo = DemoRequest.builder()
                .leadId(testLeadId)
                .demoType(DemoRequest.DemoType.PLATFORM_DEMO)
                .status(DemoRequest.DemoStatus.CONFIRMED)
                .scheduledAt(ZonedDateTime.now().minus(3, ChronoUnit.HOURS))
                .startTime(ZonedDateTime.now().minus(3, ChronoUnit.HOURS))
                .endTime(ZonedDateTime.now().minus(2, ChronoUnit.HOURS))
                .durationMinutes(60)
                .requestedAt(ZonedDateTime.now().minus(3, ChronoUnit.DAYS))
                .createdAt(ZonedDateTime.now().minus(3, ChronoUnit.DAYS))
                .updatedAt(ZonedDateTime.now())
                .build();

        demoRepository.persist(testDemo);

        // Process missed demos
        reminderService.processMissedDemos();

        // Verify score is capped at zero
        Lead leadAfter = leadRepository.findById(testLeadId);
        assertEquals(0, leadAfter.getLeadScore());
    }

    @Test
    @DisplayName("Should provide reminder statistics")
    public void testGetReminderStatistics() {
        // Create multiple demos with different reminder states
        testDemo = demoService.createDemoRequest(testLeadId, DemoRequest.DemoType.PLATFORM_DEMO);

        ZonedDateTime scheduledTime = ZonedDateTime.now().plus(22, ChronoUnit.HOURS);
        ScheduleDemoRequest request = new ScheduleDemoRequest();
        request.setStartTime(scheduledTime);
        request.setDurationMinutes(60);
        demoService.scheduleDemo(testDemo.getId(), request);

        // Process 24-hour reminders
        reminderService.processReminders24h();

        // Get statistics
        ReminderService.ReminderStatistics stats = reminderService.getStatistics();

        assertNotNull(stats);
        assertTrue(stats.pending1hReminders() >= 0);
        assertTrue(stats.sentReminders24h() > 0);
    }

    @Test
    @DisplayName("Should only process confirmed demos for reminders")
    public void testOnlyProcessConfirmedDemos() {
        // Create demo in REQUESTED state (not confirmed)
        testDemo = demoService.createDemoRequest(testLeadId, DemoRequest.DemoType.PLATFORM_DEMO);

        // Process 24-hour reminders
        reminderService.processReminders24h();

        // Verify reminder was NOT sent for non-confirmed demo
        DemoRequest updatedDemo = demoRepository.findById(testDemo.getId());
        assertFalse(updatedDemo.isReminder24hSent());
    }

    @Test
    @DisplayName("Should handle missing lead gracefully")
    public void testHandleMissingLeadGracefully() {
        // Create demo with non-existent lead
        testDemo = DemoRequest.builder()
                .leadId(UUID.randomUUID())  // Non-existent lead
                .demoType(DemoRequest.DemoType.PLATFORM_DEMO)
                .status(DemoRequest.DemoStatus.CONFIRMED)
                .scheduledAt(ZonedDateTime.now().plus(22, ChronoUnit.HOURS))
                .startTime(ZonedDateTime.now().plus(22, ChronoUnit.HOURS))
                .endTime(ZonedDateTime.now().plus(23, ChronoUnit.HOURS))
                .durationMinutes(60)
                .requestedAt(ZonedDateTime.now())
                .createdAt(ZonedDateTime.now())
                .updatedAt(ZonedDateTime.now())
                .build();

        demoRepository.persist(testDemo);

        // Should not throw exception even with missing lead
        assertDoesNotThrow(() -> reminderService.processReminders24h());
    }
}
