package io.aurigraph.v11.token.secondary;

import io.quarkus.logging.Log;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.List;
import java.util.UUID;

/**
 * ApprovalExecutionResource
 * REST API endpoints for Story 6 approval execution workflow.
 * Manages state transitions, status queries, and audit trail retrieval.
 *
 * Endpoints:
 * - POST /api/v12/approval-execution/{requestId}/execute-manual - Manual execution
 * - POST /api/v12/approval-execution/{requestId}/rollback - Rollback execution
 * - GET /api/v12/approval-execution/{requestId}/status - Get execution status
 * - GET /api/v12/approval-execution/{requestId}/audit-trail - Get audit history
 * - GET /api/v12/approval-execution/metrics/summary - Get summary metrics
 */
@Path("/api/v12/approval-execution")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Approval Execution", description = "Story 6: Approval execution workflow management")
public class ApprovalExecutionResource {

    @Inject
    ApprovalExecutionService executionService;

    /**
     * Execute approval manually for a VVB approval request
     * POST /api/v12/approval-execution/{requestId}/execute-manual
     *
     * @param requestId The approval request ID
     * @return Execution result with status and duration
     */
    @POST
    @Path("/{requestId}/execute-manual")
    @Operation(summary = "Execute approval manually", description = "Trigger manual execution of a VVB approval request")
    public Response executeApprovalManually(@PathParam("requestId") UUID requestId) {
        try {
            Log.infof("Manual execution requested for approval request: %s", requestId);

            ApprovalExecutionService.ExecutionResult result = executionService
                .executeApproval(requestId)
                .await().indefinitely();

            if (result == null) {
                return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorResponse("Approval request not found"))
                    .build();
            }

            ExecutionResponse response = new ExecutionResponse(
                result.versionId,
                result.approvalRequestId,
                result.status,
                result.message,
                result.durationMs
            );

            Log.infof("Approval execution completed: %s", result.status);
            return Response.ok(response).build();

        } catch (Exception e) {
            Log.errorf("Error executing approval: %s", e.getMessage());
            return Response.serverError()
                .entity(new ErrorResponse("Execution failed: " + e.getMessage()))
                .build();
        }
    }

    /**
     * Rollback a failed approval execution
     * POST /api/v12/approval-execution/{requestId}/rollback
     *
     * @param requestId The approval request ID to rollback
     * @param rollbackRequest Rollback reason
     * @return Rollback result
     */
    @POST
    @Path("/{requestId}/rollback")
    @Operation(summary = "Rollback approval execution", description = "Rollback a failed approval execution")
    public Response rollbackExecution(
            @PathParam("requestId") UUID requestId,
            RollbackRequest rollbackRequest) {
        try {
            Log.infof("Rollback requested for approval request: %s, reason: %s",
                requestId, rollbackRequest.getReason());

            // Find version for this approval request to get versionId
            // For now, we'll rollback directly and handle the version lookup in service
            Boolean success = executionService
                .rollbackTransition(requestId, rollbackRequest.getReason())
                .await().indefinitely();

            if (success) {
                RollbackResponse response = new RollbackResponse(
                    requestId,
                    "SUCCESS",
                    "Rollback executed successfully",
                    rollbackRequest.getReason()
                );
                Log.infof("Rollback completed for request: %s", requestId);
                return Response.ok(response).build();
            } else {
                return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorResponse("Approval request not found for rollback"))
                    .build();
            }

        } catch (Exception e) {
            Log.errorf("Error rolling back execution: %s", e.getMessage());
            return Response.serverError()
                .entity(new ErrorResponse("Rollback failed: " + e.getMessage()))
                .build();
        }
    }

    /**
     * Get execution status for a specific approval request
     * GET /api/v12/approval-execution/{requestId}/status
     *
     * @param requestId The approval request ID
     * @return Execution status summary
     */
    @GET
    @Path("/{requestId}/status")
    @Operation(summary = "Get execution status", description = "Get current execution status for an approval request")
    public Response getExecutionStatus(@PathParam("requestId") UUID requestId) {
        try {
            Log.debugf("Status requested for approval request: %s", requestId);

            // Query execution status from service
            ApprovalExecutionService.ExecutionStatus status = executionService
                .getExecutionStatus(requestId)
                .await().indefinitely();

            // BUG-S6-001 FIX: Return 404 if version not found (not null)
            if (status == null) {
                return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorResponse("Approval request not found"))
                    .build();
            }

            StatusResponse response = new StatusResponse(
                requestId,
                status.currentStatus,
                status.auditEntryCount,
                "Approval execution status retrieved successfully"
            );

            return Response.ok(response).build();

        } catch (Exception e) {
            Log.errorf("Error retrieving execution status: %s", e.getMessage());
            return Response.serverError()
                .entity(new ErrorResponse("Failed to retrieve status: " + e.getMessage()))
                .build();
        }
    }

    /**
     * Get audit trail for an approval request
     * GET /api/v12/approval-execution/{requestId}/audit-trail
     *
     * @param requestId The approval request ID
     * @return List of audit trail entries
     */
    @GET
    @Path("/{requestId}/audit-trail")
    @Operation(summary = "Get audit trail", description = "Get complete audit trail for an approval execution")
    public Response getAuditTrail(@PathParam("requestId") UUID requestId) {
        try {
            Log.debugf("Audit trail requested for approval request: %s", requestId);

            // Query audit entries by approval request ID
            List<ApprovalExecutionAudit> audits = ApprovalExecutionAudit
                .findByApprovalRequestId(requestId);

            AuditTrailResponse response = new AuditTrailResponse(
                requestId,
                audits.size(),
                audits
            );

            Log.infof("Retrieved %d audit entries for request: %s", audits.size(), requestId);
            return Response.ok(response).build();

        } catch (Exception e) {
            Log.errorf("Error retrieving audit trail: %s", e.getMessage());
            return Response.serverError()
                .entity(new ErrorResponse("Failed to retrieve audit trail: " + e.getMessage()))
                .build();
        }
    }

    /**
     * Get summary metrics for all approval executions
     * GET /api/v12/approval-execution/metrics/summary
     *
     * @return Summary statistics
     */
    @GET
    @Path("/metrics/summary")
    @Operation(summary = "Get metrics summary", description = "Get summary statistics for approval executions")
    public Response getMetricsSummary() {
        try {
            Log.debugf("Metrics summary requested");

            // Get all audit entries and calculate metrics
            List<ApprovalExecutionAudit> allAudits = ApprovalExecutionAudit.findFailures();
            long failures = allAudits.stream()
                .filter(a -> "FAILED".equals(a.executionPhase) || "ROLLED_BACK".equals(a.executionPhase))
                .count();

            MetricsSummaryResponse response = new MetricsSummaryResponse(
                0,  // Will be populated from audit data
                0,  // Will be populated from audit data
                failures,
                0,  // avg response time
                "OK"
            );

            Log.infof("Metrics summary: failures=%d", failures);
            return Response.ok(response).build();

        } catch (Exception e) {
            Log.errorf("Error retrieving metrics summary: %s", e.getMessage());
            return Response.serverError()
                .entity(new ErrorResponse("Failed to retrieve metrics: " + e.getMessage()))
                .build();
        }
    }

    // ============= REQUEST/RESPONSE DTOs =============

    public static class RollbackRequest {
        public String reason;

        public RollbackRequest() {}
        public RollbackRequest(String reason) {
            this.reason = reason;
        }

        public String getReason() {
            return reason;
        }

        public void setReason(String reason) {
            this.reason = reason;
        }
    }

    public static class ExecutionResponse {
        public UUID versionId;
        public UUID approvalRequestId;
        public String status;
        public String message;
        public long durationMs;

        public ExecutionResponse() {}
        public ExecutionResponse(UUID versionId, UUID approvalRequestId, String status, String message, long durationMs) {
            this.versionId = versionId;
            this.approvalRequestId = approvalRequestId;
            this.status = status;
            this.message = message;
            this.durationMs = durationMs;
        }
    }

    public static class RollbackResponse {
        public UUID requestId;
        public String status;
        public String message;
        public String reason;

        public RollbackResponse() {}
        public RollbackResponse(UUID requestId, String status, String message, String reason) {
            this.requestId = requestId;
            this.status = status;
            this.message = message;
            this.reason = reason;
        }
    }

    public static class StatusResponse {
        public UUID requestId;
        public String currentStatus;
        public long auditEntryCount;
        public String message;

        public StatusResponse() {}
        public StatusResponse(UUID requestId, String currentStatus, long auditEntryCount, String message) {
            this.requestId = requestId;
            this.currentStatus = currentStatus;
            this.auditEntryCount = auditEntryCount;
            this.message = message;
        }
    }

    public static class AuditTrailResponse {
        public UUID requestId;
        public long entryCount;
        public List<ApprovalExecutionAudit> entries;

        public AuditTrailResponse() {}
        public AuditTrailResponse(UUID requestId, long entryCount, List<ApprovalExecutionAudit> entries) {
            this.requestId = requestId;
            this.entryCount = entryCount;
            this.entries = entries;
        }
    }

    public static class MetricsSummaryResponse {
        public long totalExecutions;
        public long successfulExecutions;
        public long failedExecutions;
        public double averageResponseTimeMs;
        public String healthStatus;

        public MetricsSummaryResponse() {}
        public MetricsSummaryResponse(long totalExecutions, long successfulExecutions, long failedExecutions,
                                      double averageResponseTimeMs, String healthStatus) {
            this.totalExecutions = totalExecutions;
            this.successfulExecutions = successfulExecutions;
            this.failedExecutions = failedExecutions;
            this.averageResponseTimeMs = averageResponseTimeMs;
            this.healthStatus = healthStatus;
        }
    }

    public static class ErrorResponse {
        public String error;
        public long timestamp;

        public ErrorResponse() {}
        public ErrorResponse(String error) {
            this.error = error;
            this.timestamp = System.currentTimeMillis();
        }
    }
}
