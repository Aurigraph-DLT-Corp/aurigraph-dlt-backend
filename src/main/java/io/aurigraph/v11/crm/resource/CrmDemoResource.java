package io.aurigraph.v11.crm.resource;

import io.aurigraph.v11.crm.dto.DemoResponse;
import io.aurigraph.v11.crm.dto.ScheduleDemoRequest;
import io.aurigraph.v11.crm.entity.DemoRequest;
import io.aurigraph.v11.crm.service.DemoService;
import io.aurigraph.v11.crm.service.ReminderService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * REST API endpoints for demo management
 * Base path: /api/v11/crm/demos
 */
@Slf4j
@Path("/api/v11/crm/demos")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CrmDemoResource {

    @Inject
    DemoService demoService;

    @Inject
    ReminderService reminderService;

    /**
     * Create a new demo request
     * POST /api/v11/crm/demos
     */
    @POST
    public Response createDemo(CreateDemoRequest request) {
        try {
            if (request == null || request.getLeadId() == null || request.getDemoType() == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new ErrorResponse("leadId and demoType are required"))
                        .build();
            }

            DemoRequest demo = demoService.createDemoRequest(request.getLeadId(), request.getDemoType());
            DemoResponse response = DemoResponse.fromEntity(demo);

            return Response.status(Response.Status.CREATED)
                    .entity(response)
                    .build();
        } catch (Exception e) {
            log.error("Error creating demo", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Error creating demo"))
                    .build();
        }
    }

    /**
     * Schedule a demo
     * POST /api/v11/crm/demos/{id}/schedule
     */
    @POST
    @Path("/{id}/schedule")
    public Response scheduleDemo(@PathParam("id") UUID demoId, ScheduleDemoRequest request) {
        try {
            if (request == null || request.getStartTime() == null || request.getDurationMinutes() == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new ErrorResponse("startTime and durationMinutes are required"))
                        .build();
            }

            demoService.scheduleDemo(demoId, request);
            return Response.ok(new SuccessResponse("Demo scheduled successfully")).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorResponse(e.getMessage()))
                    .build();
        }
    }

    /**
     * Create meeting link for demo
     * POST /api/v11/crm/demos/{id}/meeting
     */
    @POST
    @Path("/{id}/meeting")
    public Response createMeetingLink(@PathParam("id") UUID demoId, @QueryParam("platform") String platform) {
        try {
            if (platform == null || platform.isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new ErrorResponse("platform is required (e.g., zoom, teams, google-meet)"))
                        .build();
            }

            String meetingUrl = demoService.createMeetingLink(demoId, platform);
            return Response.ok(new MeetingLinkResponse(meetingUrl)).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorResponse(e.getMessage()))
                    .build();
        } catch (Exception e) {
            log.error("Error creating meeting link", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Error creating meeting link"))
                    .build();
        }
    }

    /**
     * Send calendar invite
     * POST /api/v11/crm/demos/{id}/send-invite
     */
    @POST
    @Path("/{id}/send-invite")
    public Response sendCalendarInvite(@PathParam("id") UUID demoId) {
        try {
            demoService.sendCalendarInvite(demoId);
            return Response.ok(new SuccessResponse("Calendar invite sent")).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorResponse(e.getMessage()))
                    .build();
        }
    }

    /**
     * Complete demo with feedback
     * POST /api/v11/crm/demos/{id}/complete
     */
    @POST
    @Path("/{id}/complete")
    public Response completeDemo(@PathParam("id") UUID demoId, CompleteDemoRequest request) {
        try {
            if (request == null || request.getOutcome() == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new ErrorResponse("outcome is required"))
                        .build();
            }

            demoService.markCompleted(demoId, request.getRecordingUrl(), request.getSatisfaction(),
                    request.getFeedback(), request.getOutcome());
            return Response.ok(new SuccessResponse("Demo marked as completed")).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorResponse(e.getMessage()))
                    .build();
        }
    }

    /**
     * Get pending demos
     * GET /api/v11/crm/demos/pending
     */
    @GET
    @Path("/pending")
    public Response getPendingDemos() {
        List<DemoResponse> demos = demoService.getPendingDemos()
                .stream()
                .map(DemoResponse::fromEntity)
                .collect(Collectors.toList());
        return Response.ok(demos).build();
    }

    /**
     * Get today's demos
     * GET /api/v11/crm/demos/today
     */
    @GET
    @Path("/today")
    public Response getTodaysDemos() {
        List<DemoResponse> demos = demoService.getTodaysDemos()
                .stream()
                .map(DemoResponse::fromEntity)
                .collect(Collectors.toList());
        return Response.ok(demos).build();
    }

    /**
     * Get demos needing follow-up
     * GET /api/v11/crm/demos/follow-up
     */
    @GET
    @Path("/follow-up")
    public Response getDemosNeedingFollowUp() {
        List<DemoResponse> demos = demoService.getDemosNeedingFollowUp()
                .stream()
                .map(DemoResponse::fromEntity)
                .collect(Collectors.toList());
        return Response.ok(demos).build();
    }

    /**
     * Cancel demo
     * POST /api/v11/crm/demos/{id}/cancel
     */
    @POST
    @Path("/{id}/cancel")
    public Response cancelDemo(@PathParam("id") UUID demoId, @QueryParam("reason") String reason) {
        try {
            if (reason == null || reason.isEmpty()) {
                reason = "No reason provided";
            }
            demoService.cancelDemo(demoId, reason);
            return Response.ok(new SuccessResponse("Demo cancelled")).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorResponse(e.getMessage()))
                    .build();
        }
    }

    /**
     * Send 24-hour reminder
     * POST /api/v11/crm/demos/{id}/remind-24h
     */
    @POST
    @Path("/{id}/remind-24h")
    public Response sendReminder24h(@PathParam("id") UUID demoId) {
        try {
            demoService.sendReminder24h(demoId);
            return Response.ok(new SuccessResponse("24-hour reminder sent")).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Error sending reminder"))
                    .build();
        }
    }

    /**
     * Send 1-hour reminder
     * POST /api/v11/crm/demos/{id}/remind-1h
     */
    @POST
    @Path("/{id}/remind-1h")
    public Response sendReminder1h(@PathParam("id") UUID demoId) {
        try {
            demoService.sendReminder1h(demoId);
            return Response.ok(new SuccessResponse("1-hour reminder sent")).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Error sending reminder"))
                    .build();
        }
    }

    /**
     * Get reminder statistics
     * GET /api/v11/crm/demos/reminders/stats
     */
    @GET
    @Path("/reminders/stats")
    public Response getReminderStats() {
        try {
            ReminderService.ReminderStatistics stats = reminderService.getStatistics();
            return Response.ok(new ReminderStatsResponse(
                    stats.pending24hReminders(),
                    stats.pending1hReminders(),
                    stats.sentReminders24h(),
                    stats.sentReminders1h()
            )).build();
        } catch (Exception e) {
            log.error("Error fetching reminder statistics", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Error fetching reminder statistics"))
                    .build();
        }
    }

    /**
     * Request DTOs
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateDemoRequest {
        private UUID leadId;
        private DemoRequest.DemoType demoType;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CompleteDemoRequest {
        private String recordingUrl;
        private Integer satisfaction;
        private String feedback;
        private DemoRequest.DemoOutcome outcome;
    }

    /**
     * Response DTOs
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ErrorResponse {
        private String message;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SuccessResponse {
        private String message;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MeetingLinkResponse {
        private String meetingUrl;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReminderStatsResponse {
        private long pending24hReminders;
        private long pending1hReminders;
        private long sentReminders24h;
        private long sentReminders1h;
    }
}
