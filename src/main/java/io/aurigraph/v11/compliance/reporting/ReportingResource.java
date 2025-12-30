package io.aurigraph.v11.compliance.reporting;

import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import io.quarkus.logging.Log;
import java.time.LocalDate;
import java.util.Map;

/**
 * REST API endpoints for compliance reporting
 * Provides endpoints to generate and retrieve regulatory compliance reports
 */
@Path("/api/v11/compliance/reports")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ReportingResource {

    @Inject
    ComplianceReportGenerator reportGenerator;

    /**
     * Generate comprehensive compliance report for a token
     * GET /api/v11/compliance/reports/token/{tokenId}?startDate=2025-01-01&endDate=2025-01-31
     */
    @GET
    @Path("/token/{tokenId}")
    public Uni<Response> generateTokenReport(
            @PathParam("tokenId") String tokenId,
            @QueryParam("startDate") String startDate,
            @QueryParam("endDate") String endDate) {

        return Uni.createFrom().deferred(() -> {
            Log.infof("Generating token compliance report for %s", tokenId);

            LocalDate start = startDate != null ? LocalDate.parse(startDate) : LocalDate.now().minusMonths(1);
            LocalDate end = endDate != null ? LocalDate.parse(endDate) : LocalDate.now();

            ComplianceReport report = reportGenerator.generateTokenComplianceReport(tokenId, start, end);

            var reportMap = new java.util.HashMap<String, Object>();
            reportMap.put("success", true);
            reportMap.put("tokenId", report.getTokenId());
            reportMap.put("reportType", report.getReportType());
            reportMap.put("reportDate", report.getReportDate().toString());
            reportMap.put("jurisdiction", report.getJurisdiction());
            reportMap.put("complianceStatus", report.getComplianceStatus());
            reportMap.put("transferStats", Map.of(
                "total", report.getTotalTransfers(),
                "approved", report.getApprovedTransfers(),
                "rejected", report.getRejectedTransfers(),
                "approvalRate", String.format("%.2f%%", report.getApprovalRate())
            ));
            reportMap.put("identityStats", Map.of(
                "total", report.getTotalIdentities(),
                "active", report.getActiveIdentities(),
                "revoked", report.getRevokedIdentities()
            ));
            reportMap.put("riskAssessment", Map.of(
                "riskScore", report.getRiskScore(),
                "flaggedTransactions", report.getFlaggedTransactions(),
                "issues", report.getComplianceIssues()
            ));
            reportMap.put("rules", report.getApplicableRules());
            reportMap.put("certifications", report.getCertifications());
            reportMap.put("generatedAt", report.getGeneratedAt().toString());

            return Uni.createFrom().item(Response.ok(reportMap).build());
        });
    }

    /**
     * Generate transfer compliance report
     * GET /api/v11/compliance/reports/transfers/{tokenId}
     */
    @GET
    @Path("/transfers/{tokenId}")
    public Uni<Response> generateTransferReport(@PathParam("tokenId") String tokenId) {
        return Uni.createFrom().deferred(() -> {
            Log.infof("Generating transfer report for token %s", tokenId);

            TransferComplianceReport report = reportGenerator.generateTransferReport(tokenId);

            var reportMap = new java.util.HashMap<String, Object>();
            reportMap.put("success", true);
            reportMap.put("tokenId", report.getTokenId());
            reportMap.put("reportDate", report.getReportDate().toString());
            reportMap.put("totalTransfers", report.getTotalTransfers());
            reportMap.put("approved", report.getApprovedCount());
            reportMap.put("rejected", report.getRejectedCount());
            reportMap.put("totalAmount", report.getTotalTransferAmount().toPlainString());
            reportMap.put("averageAmount", report.getAverageTransferAmount().toPlainString());
            reportMap.put("topTransferers", report.getTopTransferers());
            reportMap.put("topRecipients", report.getTopRecipients());
            reportMap.put("generatedAt", report.getGeneratedAt().toString());

            return Uni.createFrom().item(Response.ok(reportMap).build());
        });
    }

    /**
     * Generate KYC/AML report
     * GET /api/v11/compliance/reports/kyc-aml
     */
    @GET
    @Path("/kyc-aml")
    public Uni<Response> generateKYCAMLReport() {
        return Uni.createFrom().item(() -> {
            Log.info("Generating KYC/AML report");

            KYCAMLReport report = reportGenerator.generateKYCAMLReport();

            return Response.ok(Map.of(
                "success", true,
                "reportDate", report.getReportDate().toString(),
                "identities", Map.of(
                    "total", report.getTotalIdentities(),
                    "active", report.getActiveIdentities(),
                    "revoked", report.getRevokedIdentities()
                ),
                "kycLevelDistribution", report.getKycLevelDistribution(),
                "countryDistribution", report.getCountryDistribution(),
                "amlRiskRating", String.format("%.2f", report.getAmlRiskRating()),
                "generatedAt", report.getGeneratedAt().toString()
            )).build();
        });
    }

    /**
     * Generate audit trail report
     * GET /api/v11/compliance/reports/audit-trail/{tokenId}?limit=100
     */
    @GET
    @Path("/audit-trail/{tokenId}")
    public Uni<Response> generateAuditTrailReport(
            @PathParam("tokenId") String tokenId,
            @QueryParam("limit") @DefaultValue("100") int limit) {
        return Uni.createFrom().item(() -> {
            Log.infof("Generating audit trail report for token %s with limit %d", tokenId, limit);

            AuditTrailReport report = reportGenerator.generateAuditTrailReport(tokenId, limit);

            var entries = report.getAuditEntries().stream()
                .map(entry -> Map.of(
                    "from", entry.getFrom(),
                    "to", entry.getTo(),
                    "amount", entry.getAmount().toPlainString(),
                    "success", entry.isSuccess(),
                    "reason", entry.getReason() != null ? entry.getReason() : "Approved",
                    "timestamp", entry.getTimestamp().toString()
                ))
                .toList();

            return Response.ok(Map.of(
                "success", true,
                "tokenId", report.getTokenId(),
                "reportDate", report.getReportDate().toString(),
                "entryCount", report.getEntryCount(),
                "entries", entries,
                "generatedAt", report.getGeneratedAt().toString()
            )).build();
        });
    }

    /**
     * Export compliance report in CSV format
     * GET /api/v11/compliance/reports/export/token/{tokenId}?format=csv
     */
    @GET
    @Path("/export/token/{tokenId}")
    @Produces("text/csv")
    public Uni<Response> exportTokenReport(
            @PathParam("tokenId") String tokenId,
            @QueryParam("startDate") String startDate,
            @QueryParam("endDate") String endDate) {
        return Uni.createFrom().item(() -> {
            Log.infof("Exporting token report for %s", tokenId);

            LocalDate start = startDate != null ? LocalDate.parse(startDate) : LocalDate.now().minusMonths(1);
            LocalDate end = endDate != null ? LocalDate.parse(endDate) : LocalDate.now();

            ComplianceReport report = reportGenerator.generateTokenComplianceReport(tokenId, start, end);

            // Generate CSV content
            String csv = String.format(
                "Token ID,Report Date,Jurisdiction,Compliance Status,Total Transfers,Approved,Rejected,Approval Rate,Risk Score\n" +
                "%s,%s,%s,%s,%d,%d,%d,%.2f%%,%.2f\n",
                report.getTokenId(),
                report.getReportDate(),
                report.getJurisdiction(),
                report.getComplianceStatus(),
                report.getTotalTransfers(),
                report.getApprovedTransfers(),
                report.getRejectedTransfers(),
                report.getApprovalRate(),
                report.getRiskScore()
            );

            return Response.ok(csv)
                .header("Content-Disposition", "attachment; filename=\"compliance_report_" + tokenId + ".csv\"")
                .build();
        });
    }

    /**
     * Get list of available reports
     * GET /api/v11/compliance/reports/available
     */
    @GET
    @Path("/available")
    public Uni<Response> getAvailableReports() {
        return Uni.createFrom().item(
            Response.ok(Map.of(
                "success", true,
                "reports", new Object[] {
                    Map.of("id", "token", "name", "Token Compliance Report", "description", "Comprehensive compliance report for a specific token"),
                    Map.of("id", "transfers", "name", "Transfer Compliance Report", "description", "Transfer statistics and analysis"),
                    Map.of("id", "kyc-aml", "name", "KYC/AML Report", "description", "Know-Your-Customer and Anti-Money Laundering report"),
                    Map.of("id", "audit-trail", "name", "Audit Trail Report", "description", "Detailed audit trail of all transactions")
                }
            )).build()
        );
    }
}
