package io.aurigraph.v11.crm.repository;

import io.aurigraph.v11.crm.entity.DemoRequest;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Demo Request Repository Tests
 *
 * Tests for demo request data access layer:
 * - Querying by status
 * - Querying by time windows
 * - Ordering and sorting
 * - Status updates
 *
 * @author CRM Development Team
 * @since V11.2.0
 */
@QuarkusTest
@DisplayName("Demo Request Repository Tests")
public class DemoRequestRepositoryTest {

    @Inject
    DemoRequestRepository demoRepository;

    private List<DemoRequest> testDemos;

    @BeforeEach
    public void setUp() {
        testDemos = List.of();

        // Create test demos with different statuses and times
        ZonedDateTime now = ZonedDateTime.now();

        // Demo 1: Pending, scheduled for today
        DemoRequest demo1 = DemoRequest.builder()
                .leadId(UUID.randomUUID())
                .demoType(DemoRequest.DemoType.PLATFORM_DEMO)
                .status(DemoRequest.DemoStatus.SCHEDULED)
                .scheduledAt(now.plus(2, ChronoUnit.HOURS))
                .startTime(now.plus(2, ChronoUnit.HOURS))
                .endTime(now.plus(3, ChronoUnit.HOURS))
                .durationMinutes(60)
                .requestedAt(now)
                .createdAt(now)
                .updatedAt(now)
                .build();

        // Demo 2: Pending, scheduled for tomorrow
        DemoRequest demo2 = DemoRequest.builder()
                .leadId(UUID.randomUUID())
                .demoType(DemoRequest.DemoType.PARTNERSHIP)
                .status(DemoRequest.DemoStatus.SCHEDULED)
                .scheduledAt(now.plus(1, ChronoUnit.DAYS))
                .startTime(now.plus(1, ChronoUnit.DAYS))
                .endTime(now.plus(1, ChronoUnit.DAYS).plus(1, ChronoUnit.HOURS))
                .durationMinutes(60)
                .requestedAt(now)
                .createdAt(now)
                .updatedAt(now)
                .build();

        // Demo 3: Confirmed, scheduled for 2 weeks from now
        DemoRequest demo3 = DemoRequest.builder()
                .leadId(UUID.randomUUID())
                .demoType(DemoRequest.DemoType.PLATFORM_DEMO)
                .status(DemoRequest.DemoStatus.CONFIRMED)
                .scheduledAt(now.plus(14, ChronoUnit.DAYS))
                .startTime(now.plus(14, ChronoUnit.DAYS))
                .endTime(now.plus(14, ChronoUnit.DAYS).plus(1, ChronoUnit.HOURS))
                .durationMinutes(60)
                .meetingPlatform("zoom")
                .requestedAt(now)
                .createdAt(now)
                .updatedAt(now)
                .build();

        // Demo 4: Completed
        DemoRequest demo4 = DemoRequest.builder()
                .leadId(UUID.randomUUID())
                .demoType(DemoRequest.DemoType.PLATFORM_DEMO)
                .status(DemoRequest.DemoStatus.COMPLETED)
                .scheduledAt(now.minus(1, ChronoUnit.DAYS))
                .startTime(now.minus(1, ChronoUnit.DAYS))
                .endTime(now.minus(1, ChronoUnit.DAYS).plus(1, ChronoUnit.HOURS))
                .durationMinutes(60)
                .demoOutcome(DemoRequest.DemoOutcome.VERY_INTERESTED)
                .customerSatisfactionRating(9)
                .completedAt(now.minus(1, ChronoUnit.HOURS))
                .requestedAt(now.minus(2, ChronoUnit.DAYS))
                .createdAt(now.minus(2, ChronoUnit.DAYS))
                .updatedAt(now)
                .build();

        demoRepository.persist(demo1);
        demoRepository.persist(demo2);
        demoRepository.persist(demo3);
        demoRepository.persist(demo4);

        testDemos = List.of(demo1, demo2, demo3, demo4);
    }

    @AfterEach
    public void tearDown() {
        // Clean up test data
        for (DemoRequest demo : testDemos) {
            demoRepository.delete(demo);
        }
    }

    @Test
    @DisplayName("Should find pending demos")
    public void testFindPendingDemos() {
        List<DemoRequest> pendingDemos = demoRepository.findPendingDemos();

        assertNotNull(pendingDemos);
        assertTrue(pendingDemos.size() >= 2);  // At least demo1 and demo2
    }

    @Test
    @DisplayName("Should find today's demos")
    public void testFindTodaysDemos() {
        List<DemoRequest> todaysDemos = demoRepository.findTodaysDemos();

        assertNotNull(todaysDemos);
        assertTrue(todaysDemos.size() >= 1);  // At least demo1 (today)
    }

    @Test
    @DisplayName("Should find demos awaiting confirmation")
    public void testFindAwaitingConfirmation() {
        List<DemoRequest> awaitingConfirmation = demoRepository.findAwaitingConfirmation();

        assertNotNull(awaitingConfirmation);
        assertTrue(awaitingConfirmation.size() >= 1);  // At least demo1 or demo2
    }

    @Test
    @DisplayName("Should find completed demos awaiting follow-up")
    public void testFindCompletedAwaitingFollowUp() {
        List<DemoRequest> awaitingFollowUp = demoRepository.findCompletedAwaitingFollowUp();

        assertNotNull(awaitingFollowUp);
        // Should include completed demos that haven't been followed up
    }

    @Test
    @DisplayName("Should find demos by status")
    public void testFindByStatus() {
        List<DemoRequest> completedDemos = demoRepository.find(
                "status = ?1",
                DemoRequest.DemoStatus.COMPLETED
        ).list();

        assertNotNull(completedDemos);
        assertTrue(completedDemos.size() >= 1);
    }

    @Test
    @DisplayName("Should update demo status")
    public void testUpdateStatus() {
        DemoRequest demo = testDemos.get(0);

        demoRepository.updateStatus(demo.getId(), DemoRequest.DemoStatus.CONFIRMED);

        DemoRequest updated = demoRepository.findById(demo.getId());
        assertEquals(DemoRequest.DemoStatus.CONFIRMED, updated.getStatus());
    }

    @Test
    @DisplayName("Should mark 24-hour reminder as sent")
    public void testMark24HourReminderSent() {
        DemoRequest demo = testDemos.get(0);

        demoRepository.markReminder24hSent(demo.getId());

        DemoRequest updated = demoRepository.findById(demo.getId());
        assertTrue(updated.isReminder24hSent());
        assertNotNull(updated.getReminder24hSentAt());
    }

    @Test
    @DisplayName("Should mark 1-hour reminder as sent")
    public void testMark1HourReminderSent() {
        DemoRequest demo = testDemos.get(0);

        demoRepository.markReminder1hSent(demo.getId());

        DemoRequest updated = demoRepository.findById(demo.getId());
        assertTrue(updated.isReminder1hSent());
        assertNotNull(updated.getReminder1hSentAt());
    }

    @Test
    @DisplayName("Should find demos scheduled within time window")
    public void testFindDemosInTimeWindow() {
        ZonedDateTime now = ZonedDateTime.now();
        ZonedDateTime in24h = now.plus(24, ChronoUnit.HOURS);

        List<DemoRequest> demos = demoRepository.find(
                "status = ?1 AND scheduledAt >= ?2 AND scheduledAt <= ?3",
                DemoRequest.DemoStatus.SCHEDULED, now, in24h
        ).list();

        assertNotNull(demos);
        assertTrue(demos.size() >= 1);  // Should find demo1 (scheduled for today)
    }

    @Test
    @DisplayName("Should count demos by status")
    public void testCountByStatus() {
        long completedCount = demoRepository.count(
                "status = ?1",
                DemoRequest.DemoStatus.COMPLETED
        );

        assertTrue(completedCount >= 1);
    }

    @Test
    @DisplayName("Should handle demo with all fields populated")
    public void testDemoWithAllFields() {
        ZonedDateTime now = ZonedDateTime.now();

        DemoRequest fullDemo = DemoRequest.builder()
                .leadId(UUID.randomUUID())
                .demoType(DemoRequest.DemoType.PLATFORM_DEMO)
                .status(DemoRequest.DemoStatus.COMPLETED)
                .scheduledAt(now.minus(1, ChronoUnit.DAYS))
                .startTime(now.minus(1, ChronoUnit.DAYS))
                .endTime(now.minus(1, ChronoUnit.DAYS).plus(1, ChronoUnit.HOURS))
                .durationMinutes(60)
                .meetingPlatform("zoom")
                .meetingId("zoom-123")
                .meetingUrl("https://zoom.us/j/123")
                .meetingJoinUrl("https://zoom.us/j/123?pwd=xxx")
                .recordingUrl("https://zoom.us/rec/123")
                .recordingAvailableAt(now.minus(1, ChronoUnit.HOURS))
                .calendarInviteSent(true)
                .calendarInviteSentAt(now.minus(1, ChronoUnit.DAYS))
                .reminder24hSent(true)
                .reminder24hSentAt(now.minus(1, ChronoUnit.DAYS).plus(1, ChronoUnit.HOURS))
                .reminder1hSent(true)
                .reminder1hSentAt(now.minus(59, ChronoUnit.MINUTES))
                .customerSatisfactionRating(9)
                .customerFeedbackText("Excellent demo")
                .demoOutcome(DemoRequest.DemoOutcome.VERY_INTERESTED)
                .feedbackFormCompleted(true)
                .feedbackFormCompletedAt(now.minus(1, ChronoUnit.HOURS))
                .completedAt(now.minus(1, ChronoUnit.HOURS))
                .requestedAt(now.minus(7, ChronoUnit.DAYS))
                .createdAt(now.minus(7, ChronoUnit.DAYS))
                .updatedAt(now)
                .build();

        demoRepository.persist(fullDemo);

        DemoRequest retrieved = demoRepository.findById(fullDemo.getId());
        assertNotNull(retrieved);
        assertEquals("zoom", retrieved.getMeetingPlatform());
        assertEquals(9, retrieved.getCustomerSatisfactionRating());
        assertEquals(DemoRequest.DemoOutcome.VERY_INTERESTED, retrieved.getDemoOutcome());
        assertTrue(retrieved.isRecordingEnabled());
        assertTrue(retrieved.isFeedbackFormCompleted());

        demoRepository.deleteById(fullDemo.getId());
    }
}
