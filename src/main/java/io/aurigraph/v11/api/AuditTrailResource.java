package io.aurigraph.v11.api;

import io.aurigraph.v11.compliance.AuditTrailService;
import io.quarkus.logging.Log;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import io.smallrye.mutiny.Uni;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * AuditTrailResource - REST API for audit trail management
 *
 * Endpoints:
 * - GET  /api/v12/audit/trails/{tokenId}     - Get token audit trail
 * - GET  /api/v12/audit/actor/{actor}         - Get actor audit trail
 * - GET  /api/v12/audit/compliance            - Generate compliance report
 * - GET  /api/v12/audit/statistics            - Get audit statistics
 * - POST /api/v12/audit/verify                - Verify audit integrity
 * - POST /api/v12/audit/archive               - Archive old records
 */
@Path("/api/v12/audit")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Audit Trail", description = "Audit trail management and compliance reporting")
public class AuditTrailResource {

    @Inject
    AuditTrailService auditTrailService;

    /**
     * Get audit trail for a specific token
     */
    @GET
    @Path("/trails/{tokenId}")
    @Operation(summary = "Get token audit trail", description = "Retrieve complete audit trail for a token")
    @APIResponse(responseCode = "200", description = "Audit trail retrieved successfully")
    public Uni<List<AuditTrailService.AuditRecord>> getTokenAuditTrail(
            @PathParam("tokenId") String tokenId) {
        Log.info("Retrieving audit trail for token: " + tokenId);
        return auditTrailService.getTokenAuditTrail(tokenId);
    }

    /**
     * Get audit trail for an actor
     */
    @GET
    @Path("/actor/{actor}")
    @Operation(summary = "Get actor audit trail", description = "Retrieve audit trail for specific actor")
    @APIResponse(responseCode = "200", description = "Actor audit trail retrieved successfully")
    public Uni<List<AuditTrailService.AuditRecord>> getActorAuditTrail(
            @PathParam("actor") String actor,
            @QueryParam("days") @DefaultValue("7") int days) {
        Log.info("Retrieving audit trail for actor: " + actor);
        Instant now = Instant.now();
        Instant fromDate = now.minus(days, ChronoUnit.DAYS);
        return auditTrailService.getActorAuditTrail(actor, fromDate, now);
    }

    /**
     * Generate compliance report
     */
    @GET
    @Path("/compliance")
    @Operation(summary = "Generate compliance report", description = "Generate compliance report for audit trail")
    @APIResponse(responseCode = "200", description = "Compliance report generated")
    public Uni<AuditTrailService.ComplianceReport> generateComplianceReport(
            @QueryParam("days") @DefaultValue("30") int days) {
        Log.info("Generating compliance report for last " + days + " days");
        Instant now = Instant.now();
        Instant fromDate = now.minus(days, ChronoUnit.DAYS);
        return auditTrailService.generateComplianceReport(fromDate, now);
    }

    /**
     * Get audit statistics
     */
    @GET
    @Path("/statistics")
    @Operation(summary = "Get audit statistics", description = "Retrieve audit trail statistics")
    @APIResponse(responseCode = "200", description = "Statistics retrieved successfully")
    public Uni<AuditTrailService.AuditStatistics> getAuditStatistics() {
        Log.info("Retrieving audit statistics");
        return auditTrailService.getAuditStatistics();
    }

    /**
     * Verify audit trail integrity
     */
    @POST
    @Path("/verify")
    @Operation(summary = "Verify audit integrity", description = "Verify integrity of audit trail using Merkle chain")
    @APIResponse(responseCode = "200", description = "Audit integrity verified")
    public Uni<AuditTrailService.IntegrityVerificationResult> verifyIntegrity() {
        Log.info("Verifying audit trail integrity");
        return auditTrailService.verifyAuditIntegrity();
    }

    /**
     * Archive old audit records
     */
    @POST
    @Path("/archive")
    @Operation(summary = "Archive old records", description = "Archive audit records older than specified date")
    @APIResponse(responseCode = "200", description = "Records archived successfully")
    public Uni<Response> archiveRecords(
            @QueryParam("days") @DefaultValue("90") int days) {
        Log.info("Archiving audit records older than " + days + " days");
        Instant cutoffDate = Instant.now().minus(days, ChronoUnit.DAYS);
        return auditTrailService.archiveAuditRecords(cutoffDate)
            .map(v -> Response.ok().entity("Records archived successfully").build());
    }
}
