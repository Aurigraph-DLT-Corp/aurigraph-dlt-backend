package io.aurigraph.v11.crm.service;

import io.aurigraph.v11.crm.dto.ScheduleDemoRequest;
import io.aurigraph.v11.crm.entity.DemoRequest;
import io.aurigraph.v11.crm.entity.Lead;
import io.aurigraph.v11.crm.repository.DemoRequestRepository;
import io.aurigraph.v11.crm.repository.LeadRepository;
import lombok.extern.slf4j.Slf4j;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * DemoService - Business logic for demo scheduling and execution
 *
 * Handles:
 * - Demo request creation and scheduling
 * - Meeting platform integration (Zoom, Teams, Google Meet, Webex)
 * - Calendar invite generation with iCalendar format
 * - Automated reminders (24h, 1h) via email
 * - Recording retrieval and storage
 * - Follow-up workflows
 * - Demo outcome tracking and lead scoring
 *
 * Integration points:
 * - MeetingPlatformService: Meeting provisioning
 * - EmailService: Calendar invites and reminders
 * - ReminderService: Scheduled reminder processing
 */
@Slf4j
@ApplicationScoped
public class DemoService {

    @Inject
    DemoRequestRepository demoRepository;

    @Inject
    LeadRepository leadRepository;

    @Inject
    LeadService leadService;

    @Inject
    EmailService emailService;

    @Inject
    MeetingPlatformService meetingPlatformService;

    @Inject
    ReminderService reminderService;

    /**
     * Create a new demo request
     */
    @Transactional
    public DemoRequest createDemoRequest(UUID leadId, DemoRequest.DemoType demoType) {
        log.info("Creating demo request for lead {}", leadId);

        DemoRequest demo = DemoRequest.builder()
                .leadId(leadId)
                .demoType(demoType)
                .status(DemoRequest.DemoStatus.REQUESTED)
                .requestedAt(ZonedDateTime.now())
                .createdAt(ZonedDateTime.now())
                .updatedAt(ZonedDateTime.now())
                .build();

        demoRepository.persist(demo);

        // Increment lead score for demo request
        leadService.updateLeadScore(leadId, 20);

        log.info("Demo request created with ID: {}", demo.getId());

        return demo;
    }

    /**
     * Schedule a demo for a specific time
     */
    @Transactional
    public void scheduleDemo(UUID demoId, ScheduleDemoRequest request) {
        Optional<DemoRequest> demoOpt = demoRepository.find("id", demoId).firstResultOptional();
        if (demoOpt.isEmpty()) {
            throw new IllegalArgumentException("Demo not found: " + demoId);
        }
        DemoRequest demo = demoOpt.get();

        log.info("Scheduling demo {} for {}", demoId, request.getStartTime());

        demo.setScheduledAt(request.getStartTime());
        demo.setStartTime(request.getStartTime());
        demo.setEndTime(request.getStartTime().plusMinutes(request.getDurationMinutes()));
        demo.setDurationMinutes(request.getDurationMinutes());
        demo.setStatus(DemoRequest.DemoStatus.SCHEDULED);
        demo.setUpdatedAt(ZonedDateTime.now());

        demoRepository.update("scheduledAt = ?1, startTime = ?2, endTime = ?3, status = ?4, updatedAt = ?5 WHERE id = ?6",
                request.getStartTime(), request.getStartTime(), request.getStartTime().plusMinutes(request.getDurationMinutes()),
                DemoRequest.DemoStatus.SCHEDULED, ZonedDateTime.now(), demoId);
    }

    /**
     * Create meeting link via platform API (Zoom, Teams, Google Meet, Webex)
     * Integrates with MeetingPlatformService for actual platform provisioning
     */
    @Transactional
    public String createMeetingLink(UUID demoId, String platform) {
        log.info("Creating {} meeting for demo {}", platform, demoId);

        Optional<DemoRequest> demoOpt = demoRepository.find("id", demoId).firstResultOptional();
        if (demoOpt.isEmpty()) {
            throw new IllegalArgumentException("Demo not found: " + demoId);
        }

        DemoRequest demo = demoOpt.get();

        try {
            // Provision meeting via platform API
            MeetingPlatformService.MeetingConfig config =
                meetingPlatformService.createMeeting(demo, platform);

            // Enable recording
            meetingPlatformService.enableRecording(platform, config.getMeetingId());

            // Persist meeting details
            demoRepository.update(
                "meetingPlatform = ?1, meetingId = ?2, meetingUrl = ?3, meetingJoinUrl = ?4, " +
                "recordingEnabled = TRUE, status = ?5, updatedAt = ?6 WHERE id = ?7",
                platform,
                config.getMeetingId(),
                config.getMeetingUrl(),
                config.getJoinUrl(),
                DemoRequest.DemoStatus.CONFIRMED,
                ZonedDateTime.now(),
                demoId
            );

            log.info("Meeting link created for demo {}: {}", demoId, config.getMeetingUrl());
            return config.getMeetingUrl();
        } catch (Exception e) {
            log.error("Failed to create meeting for demo {} on platform {}", demoId, platform, e);
            throw new RuntimeException("Failed to provision meeting", e);
        }
    }

    /**
     * Send calendar invites
     * Stub - would integrate with email service
     */
    @Transactional
    public void sendCalendarInvite(UUID demoId) {
        Optional<DemoRequest> demoOpt = demoRepository.find("id", demoId).firstResultOptional();
        if (demoOpt.isEmpty()) {
            throw new IllegalArgumentException("Demo not found: " + demoId);
        }
        DemoRequest demo = demoOpt.get();

        log.info("Sending calendar invite for demo {}", demoId);

        demo.setCalendarInviteSent(true);
        demo.setCalendarInviteSentAt(ZonedDateTime.now());
        demo.setUpdatedAt(ZonedDateTime.now());

        demoRepository.update("calendarInviteSent = TRUE, calendarInviteSentAt = ?1, updatedAt = ?1 WHERE id = ?2",
                ZonedDateTime.now(), demoId);
    }

    /**
     * Mark demo as completed
     */
    @Transactional
    public void markCompleted(UUID demoId, String recordingUrl, Integer satisfaction, String feedback, DemoRequest.DemoOutcome outcome) {
        Optional<DemoRequest> demoOpt = demoRepository.find("id", demoId).firstResultOptional();
        if (demoOpt.isEmpty()) {
            throw new IllegalArgumentException("Demo not found: " + demoId);
        }
        DemoRequest demo = demoOpt.get();

        log.info("Marking demo {} as completed", demoId);

        demo.setCompletedAt(ZonedDateTime.now());
        demo.setRecordingUrl(recordingUrl);
        demo.setRecordingAvailableAt(ZonedDateTime.now());
        demo.setCustomerSatisfactionRating(satisfaction);
        demo.setCustomerFeedbackText(feedback);
        demo.setDemoOutcome(outcome);
        demo.setFeedbackFormCompleted(true);
        demo.setFeedbackFormCompletedAt(ZonedDateTime.now());
        demo.setStatus(DemoRequest.DemoStatus.COMPLETED);
        demo.setUpdatedAt(ZonedDateTime.now());

        demoRepository.update("completedAt = ?1, recordingUrl = ?2, recordingAvailableAt = ?3, " +
                "customerSatisfactionRating = ?4, customerFeedbackText = ?5, demoOutcome = ?6, " +
                "feedbackFormCompleted = TRUE, feedbackFormCompletedAt = ?7, status = ?8, updatedAt = ?7 WHERE id = ?9",
                ZonedDateTime.now(), recordingUrl, ZonedDateTime.now(),
                satisfaction, feedback, outcome,
                ZonedDateTime.now(),
                DemoRequest.DemoStatus.COMPLETED, demoId);

        // Increment lead score for completed demo
        leadService.updateLeadScore(demo.getLeadId(), 25);

        // Update lead status based on outcome
        if (outcome == DemoRequest.DemoOutcome.VERY_INTERESTED) {
            leadService.updateLeadStatus(demo.getLeadId(), io.aurigraph.v11.crm.entity.Lead.LeadStatus.QUALIFIED);
            leadService.updateLeadScore(demo.getLeadId(), 75);
        }
    }

    /**
     * Get pending demos awaiting scheduling
     */
    public List<DemoRequest> getPendingDemos() {
        return demoRepository.findPendingDemos();
    }

    /**
     * Get today's demos
     */
    public List<DemoRequest> getTodaysDemos() {
        return demoRepository.findTodaysDemos();
    }

    /**
     * Get demos needing follow-up
     */
    public List<DemoRequest> getDemosNeedingFollowUp() {
        return demoRepository.findCompletedAwaitingFollowUp();
    }

    /**
     * Cancel demo
     */
    @Transactional
    public void cancelDemo(UUID demoId, String reason) {
        log.info("Cancelling demo {}: {}", demoId, reason);
        demoRepository.updateStatus(demoId, DemoRequest.DemoStatus.CANCELLED);
    }

    /**
     * Send reminder 24 hours before demo
     */
    @Transactional
    public void sendReminder24h(UUID demoId) {
        log.info("Sending 24-hour reminder for demo {}", demoId);
        demoRepository.markReminder24hSent(demoId);
        // TODO: Integrate with email service to send actual reminder
    }

    /**
     * Send reminder 1 hour before demo
     */
    @Transactional
    public void sendReminder1h(UUID demoId) {
        log.info("Sending 1-hour reminder for demo {}", demoId);
        demoRepository.markReminder1hSent(demoId);
        // TODO: Integrate with email service to send actual reminder
    }
}
