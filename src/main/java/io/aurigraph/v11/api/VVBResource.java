package io.aurigraph.v11.api;

import io.aurigraph.v11.token.vvb.*;
import io.quarkus.logging.Log;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * VVB (Verified Valuator Board) REST API Resource
 * Endpoints for token approval workflow management
 */
@Path("/api/v12/vvb")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "VVB Approval", description = "Token approval workflow endpoints")
public class VVBResource {

    @Inject
    VVBValidator validator;

    @Inject
    VVBWorkflowService workflowService;

    @Inject
    TokenLifecycleGovernance governance;

    /**
     * Submit token version for VVB validation
     * POST /api/v12/vvb/validate
     */
    @POST
    @Path("/validate")
    @Operation(summary = "Submit token for VVB validation", description = "Submit a token version for VVB approval workflow")
    public Response submitForValidation(VVBValidationRequest request) {
        try {
            Log.infof("VVB validation request: changeType=%s, submitter=%s",
                request.getChangeType(), request.getSubmitterId());

            UUID versionId = UUID.randomUUID();

            VVBApprovalResult result = validator.validateTokenVersion(versionId, request)
                .await().indefinitely();

            Log.infof("Validation submitted with status: %s", result.getStatus());

            return Response.accepted(result).build();
        } catch (Exception e) {
            Log.errorf("Error submitting for validation: %s", e.getMessage());
            return Response.serverError().entity("Error: " + e.getMessage()).build();
        }
    }

    /**
     * Approve token version
     * POST /api/v12/vvb/{versionId}/approve
     */
    @POST
    @Path("/{versionId}/approve")
    @Operation(summary = "Approve token version", description = "Approve a token version for activation")
    public Response approveTokenVersion(
            @PathParam("versionId") UUID versionId,
            ApprovalRequestDto approvalRequest) {
        try {
            Log.infof("Approving version %s by approver %s", versionId, approvalRequest.getApproverId());

            VVBApprovalResult result = validator.approveTokenVersion(versionId, approvalRequest.getApproverId())
                .await().indefinitely();

            Log.infof("Approval decision: %s", result.getStatus());

            return Response.ok(result).build();
        } catch (Exception e) {
            Log.errorf("Error approving token: %s", e.getMessage());
            return Response.serverError().entity("Error: " + e.getMessage()).build();
        }
    }

    /**
     * Reject token version
     * POST /api/v12/vvb/{versionId}/reject
     */
    @POST
    @Path("/{versionId}/reject")
    @Operation(summary = "Reject token version", description = "Reject a token version with reason")
    public Response rejectTokenVersion(
            @PathParam("versionId") UUID versionId,
            RejectionRequestDto rejectionRequest) {
        try {
            Log.infof("Rejecting version %s with reason: %s", versionId, rejectionRequest.getReason());

            VVBApprovalResult result = validator.rejectTokenVersion(versionId, rejectionRequest.getReason())
                .await().indefinitely();

            Log.infof("Rejection recorded: %s", result.getStatus());

            return Response.ok(result).build();
        } catch (Exception e) {
            Log.errorf("Error rejecting token: %s", e.getMessage());
            return Response.serverError().entity("Error: " + e.getMessage()).build();
        }
    }

    /**
     * Get all pending approvals
     * GET /api/v12/vvb/pending
     */
    @GET
    @Path("/pending")
    @Operation(summary = "List pending approvals", description = "Get all pending token approvals")
    public Response getPendingApprovals() {
        try {
            List<VVBValidator.VVBValidationStatus> pending = validator.getPendingApprovals()
                .await().indefinitely();

            Log.infof("Retrieved %d pending approvals", pending.size());

            return Response.ok(pending).build();
        } catch (Exception e) {
            Log.errorf("Error retrieving pending approvals: %s", e.getMessage());
            return Response.serverError().entity("Error: " + e.getMessage()).build();
        }
    }

    /**
     * Get pending approvals for specific user
     * GET /api/v12/vvb/pending?userId={userId}
     */
    @GET
    @Path("/pending-by-user")
    @Operation(summary = "List pending approvals by user", description = "Get pending approvals for a specific user")
    public Response getPendingApprovalsForUser(@QueryParam("userId") String userId) {
        try {
            List<VVBWorkflowService.PendingApprovalDetail> pending = workflowService
                .getPendingApprovalsForUser(userId)
                .await().indefinitely();

            Log.infof("Retrieved %d pending approvals for user %s", pending.size(), userId);

            return Response.ok(pending).build();
        } catch (Exception e) {
            Log.errorf("Error retrieving pending approvals for user: %s", e.getMessage());
            return Response.serverError().entity("Error: " + e.getMessage()).build();
        }
    }

    /**
     * Get validation statistics
     * GET /api/v12/vvb/statistics
     */
    @GET
    @Path("/statistics")
    @Operation(summary = "Get VVB statistics", description = "Get approval workflow statistics")
    public Response getStatistics() {
        try {
            VVBStatistics stats = validator.getValidationStatistics()
                .await().indefinitely();

            Log.infof("VVB Statistics: Approved=%d, Rejected=%d, Pending=%d",
                stats.getApprovedCount(), stats.getRejectedCount(), stats.getPendingCount());

            return Response.ok(stats).build();
        } catch (Exception e) {
            Log.errorf("Error retrieving statistics: %s", e.getMessage());
            return Response.serverError().entity("Error: " + e.getMessage()).build();
        }
    }

    /**
     * Get validation details for specific version
     * GET /api/v12/vvb/{versionId}/details
     */
    @GET
    @Path("/{versionId}/details")
    @Operation(summary = "Get validation details", description = "Get detailed validation status and audit trail")
    public Response getValidationDetails(@PathParam("versionId") UUID versionId) {
        try {
            VVBValidationDetails details = validator.getValidationDetails(versionId)
                .await().indefinitely();

            if (details == null) {
                Log.warnf("Validation details not found for version %s", versionId);
                return Response.status(Response.Status.NOT_FOUND).build();
            }

            Log.infof("Retrieved validation details for version %s", versionId);

            return Response.ok(details).build();
        } catch (Exception e) {
            Log.errorf("Error retrieving validation details: %s", e.getMessage());
            return Response.serverError().entity("Error: " + e.getMessage()).build();
        }
    }

    /**
     * Get approval report for date range
     * GET /api/v12/vvb/reports/approvals?startDate=2025-01-01&endDate=2025-01-31
     */
    @GET
    @Path("/reports/approvals")
    @Operation(summary = "Get approval report", description = "Get approval statistics for date range")
    public Response getApprovalReport(
            @QueryParam("startDate") String startDate,
            @QueryParam("endDate") String endDate) {
        try {
            LocalDate start = LocalDate.parse(startDate);
            LocalDate end = LocalDate.parse(endDate);

            VVBWorkflowService.ApprovalStatistics report = workflowService
                .generateApprovalReport(start, end)
                .await().indefinitely();

            Log.infof("Generated approval report for %s to %s", startDate, endDate);

            return Response.ok(report).build();
        } catch (Exception e) {
            Log.errorf("Error generating approval report: %s", e.getMessage());
            return Response.serverError().entity("Error: " + e.getMessage()).build();
        }
    }

    /**
     * Validate retirement eligibility
     * GET /api/v12/vvb/governance/retirement-validation?primaryTokenId={id}
     */
    @GET
    @Path("/governance/retirement-validation")
    @Operation(summary = "Validate retirement", description = "Check if primary token can be retired")
    public Response validateRetirement(@QueryParam("primaryTokenId") String primaryTokenId) {
        try {
            TokenLifecycleGovernance.GovernanceValidation validation = governance
                .validateRetirement(primaryTokenId)
                .await().indefinitely();

            Log.infof("Retirement validation for %s: valid=%b", primaryTokenId, validation.isValid());

            return Response.ok(validation).build();
        } catch (Exception e) {
            Log.errorf("Error validating retirement: %s", e.getMessage());
            return Response.serverError().entity("Error: " + e.getMessage()).build();
        }
    }

    /**
     * Get blocking child tokens
     * GET /api/v12/vvb/governance/blocking-tokens?primaryTokenId={id}
     */
    @GET
    @Path("/governance/blocking-tokens")
    @Operation(summary = "Get blocking tokens", description = "Get secondary tokens blocking retirement")
    public Response getBlockingTokens(@QueryParam("primaryTokenId") String primaryTokenId) {
        try {
            List<String> blockingTokens = governance.getBlockingChildTokens(primaryTokenId)
                .await().indefinitely();

            Log.infof("Blocking tokens for %s: %d tokens", primaryTokenId, blockingTokens.size());

            return Response.ok(blockingTokens).build();
        } catch (Exception e) {
            Log.errorf("Error retrieving blocking tokens: %s", e.getMessage());
            return Response.serverError().entity("Error: " + e.getMessage()).build();
        }
    }

    // ============= REQUEST/RESPONSE DTOS =============

    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ApprovalRequestDto {
        private String approverId;
        private String comments;
    }

    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class RejectionRequestDto {
        private String reason;
        private String rejectedBy;
    }
}
