package io.aurigraph.v11.crm.dto;

import io.aurigraph.v11.crm.entity.DemoRequest;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * DTO for DemoRequest response from API
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DemoResponse {
    private UUID id;
    private UUID leadId;
    private DemoRequest.DemoType demoType;
    private DemoRequest.DemoStatus status;
    private ZonedDateTime scheduledAt;
    private ZonedDateTime startTime;
    private ZonedDateTime endTime;
    private Integer durationMinutes;
    private String meetingPlatform;
    private String meetingUrl;
    private String meetingJoinUrl;
    private String recordingUrl;
    private Integer customerSatisfactionRating;
    private String customerFeedbackText;
    private DemoRequest.DemoOutcome demoOutcome;
    private Boolean reminder24hSent;
    private Boolean reminder1hSent;
    private Boolean calendarInviteSent;
    private ZonedDateTime completedAt;
    private ZonedDateTime createdAt;
    private ZonedDateTime updatedAt;

    /**
     * Convert DemoRequest entity to DemoResponse DTO
     */
    public static DemoResponse fromEntity(DemoRequest demo) {
        return DemoResponse.builder()
                .id(demo.getId())
                .leadId(demo.getLeadId())
                .demoType(demo.getDemoType())
                .status(demo.getStatus())
                .scheduledAt(demo.getScheduledAt())
                .startTime(demo.getStartTime())
                .endTime(demo.getEndTime())
                .durationMinutes(demo.getDurationMinutes())
                .meetingPlatform(demo.getMeetingPlatform())
                .meetingUrl(demo.getMeetingUrl())
                .meetingJoinUrl(demo.getMeetingJoinUrl())
                .recordingUrl(demo.getRecordingUrl())
                .customerSatisfactionRating(demo.getCustomerSatisfactionRating())
                .customerFeedbackText(demo.getCustomerFeedbackText())
                .demoOutcome(demo.getDemoOutcome())
                .reminder24hSent(demo.getReminder24hSent())
                .reminder1hSent(demo.getReminder1hSent())
                .calendarInviteSent(demo.getCalendarInviteSent())
                .completedAt(demo.getCompletedAt())
                .createdAt(demo.getCreatedAt())
                .updatedAt(demo.getUpdatedAt())
                .build();
    }
}
