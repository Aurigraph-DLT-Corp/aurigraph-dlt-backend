package io.aurigraph.v11.crm.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * DemoRequest Entity - Represents demo scheduling and execution
 *
 * Maps to PostgreSQL demo_requests table with support for:
 * - Demo lifecycle tracking (requested → scheduled → confirmed → completed)
 * - Meeting platform integration (Zoom, Teams, Google Meet)
 * - Automated reminders (24h, 1h before demo)
 * - Recording and feedback collection
 * - Customer satisfaction tracking
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "demo_requests", indexes = {
        @Index(name = "idx_demo_lead_id", columnList = "lead_id"),
        @Index(name = "idx_demo_status", columnList = "status"),
        @Index(name = "idx_demo_scheduled_at", columnList = "scheduled_at"),
        @Index(name = "idx_demo_outcome", columnList = "demo_outcome")
})
public class DemoRequest extends PanacheEntityBase {

    // Enums
    public enum DemoType {
        STANDARD_DEMO,
        CUSTOM_DEMO,
        TECHNICAL_DEEP_DIVE,
        PROOF_OF_CONCEPT,
        PILOT_PROGRAM,
        EXECUTIVE_BRIEFING,
        PLATFORM_DEMO,
        PARTNERSHIP
    }

    public enum DemoStatus {
        REQUESTED,
        SCHEDULED,
        CONFIRMED,
        IN_PROGRESS,
        COMPLETED,
        CANCELLED,
        NO_SHOW,
        RESCHEDULED
    }

    public enum DemoOutcome {
        VERY_INTERESTED,
        INTERESTED,
        NEUTRAL,
        NOT_INTERESTED,
        NO_SHOW
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "lead_id", nullable = false)
    private UUID leadId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DemoType demoType = DemoType.STANDARD_DEMO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DemoStatus status = DemoStatus.REQUESTED;

    @Column(name = "requested_at")
    private ZonedDateTime requestedAt;

    @Column(name = "scheduled_at")
    private ZonedDateTime scheduledAt;

    @Column(name = "start_time")
    private ZonedDateTime startTime;

    @Column(name = "end_time")
    private ZonedDateTime endTime;

    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    @Column(name = "requester_timezone", length = 50)
    private String requesterTimezone;

    @Column(name = "demo_timezone", length = 50)
    private String demoTimezone;

    // Meeting Details
    @Column(name = "meeting_platform", length = 50)
    private String meetingPlatform;

    @Column(name = "meeting_id", length = 255)
    private String meetingId;

    @Column(name = "meeting_url", columnDefinition = "TEXT")
    private String meetingUrl;

    @Column(name = "meeting_join_url", columnDefinition = "TEXT")
    private String meetingJoinUrl;

    @Column(name = "meeting_passcode", length = 50)
    private String meetingPasscode;

    // Invitation & Reminders
    @Column(name = "calendar_invite_sent")
    private Boolean calendarInviteSent = false;

    @Column(name = "calendar_invite_sent_at")
    private ZonedDateTime calendarInviteSentAt;

    @Column(name = "reminder24h_sent")
    private Boolean reminder24hSent = false;

    @Column(name = "reminder24h_sent_at")
    private ZonedDateTime reminder24hSentAt;

    @Column(name = "reminder1h_sent")
    private Boolean reminder1hSent = false;

    @Column(name = "reminder1h_sent_at")
    private ZonedDateTime reminder1hSentAt;

    // Completion & Recording
    @Column(name = "completed_at")
    private ZonedDateTime completedAt;

    @Column(name = "recording_url", columnDefinition = "TEXT")
    private String recordingUrl;

    @Column(name = "recording_available_at")
    private ZonedDateTime recordingAvailableAt;

    // Feedback Collection
    @Column(name = "feedback_form_sent")
    private Boolean feedbackFormSent = false;

    @Column(name = "feedback_form_sent_at")
    private ZonedDateTime feedbackFormSentAt;

    @Column(name = "feedback_form_completed")
    private Boolean feedbackFormCompleted = false;

    @Column(name = "feedback_form_completed_at")
    private ZonedDateTime feedbackFormCompletedAt;

    @Column(name = "customer_satisfaction_rating")
    private Integer customerSatisfactionRating;

    @Column(name = "customer_feedback_text", columnDefinition = "TEXT")
    private String customerFeedbackText;

    @Enumerated(EnumType.STRING)
    @Column(name = "demo_outcome")
    private DemoOutcome demoOutcome;

    // Follow-up
    @Column(name = "follow_up_email_sent")
    private Boolean followUpEmailSent = false;

    @Column(name = "follow_up_email_sent_at")
    private ZonedDateTime followUpEmailSentAt;

    @Column(name = "follow_up_notes", columnDefinition = "TEXT")
    private String followUpNotes;

    // Audit Fields
    @Column(name = "created_at", nullable = false, updatable = false)
    private ZonedDateTime createdAt;

    @Column(name = "updated_at")
    private ZonedDateTime updatedAt;

    @Column(name = "created_by", length = 255)
    private String createdBy;

    @Column(name = "updated_by", length = 255)
    private String updatedBy;

    @PrePersist
    public void prePersist() {
        this.createdAt = ZonedDateTime.now();
        this.updatedAt = ZonedDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = ZonedDateTime.now();
    }

    // Helper methods for test compatibility
    public Boolean isReminder24hSent() {
        return reminder24hSent != null && reminder24hSent;
    }

    public Boolean isReminder1hSent() {
        return reminder1hSent != null && reminder1hSent;
    }

    public Boolean isRecordingEnabled() {
        return recordingUrl != null && !recordingUrl.isEmpty();
    }

    public Boolean isFeedbackFormCompleted() {
        return feedbackFormCompleted != null && feedbackFormCompleted;
    }

    public Boolean isCalendarInviteSent() {
        return calendarInviteSent != null && calendarInviteSent;
    }
}
