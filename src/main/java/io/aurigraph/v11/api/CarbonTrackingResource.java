package io.aurigraph.v11.api;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import io.smallrye.mutiny.Uni;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.util.*;

/**
 * Carbon Tracking API Resource
 *
 * Provides carbon emission tracking and reporting for blockchain operations.
 * Part of Enterprise Portal V4.8.0 implementation.
 *
 * Endpoints:
 * - GET /api/v11/carbon/emissions - Get carbon emission metrics
 * - POST /api/v11/carbon/report - Submit carbon offset report
 * - GET /api/v11/carbon/summary - Get carbon tracking summary
 *
 * @author Aurigraph V11 Team
 * @version 4.8.0
 */
@Path("/api/v11/carbon")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Carbon Tracking", description = "Carbon emission tracking and offset reporting")
public class CarbonTrackingResource {

    private static final Logger LOG = Logger.getLogger(CarbonTrackingResource.class);

    // In-memory storage for carbon reports
    private static final List<CarbonReport> reports = new ArrayList<>();
    private static long reportIdCounter = 1;

    /**
     * GET /api/v11/carbon/emissions
     *
     * Returns carbon emission metrics for blockchain operations.
     *
     * Query Parameters:
     * - timeRange: Time range for emissions (daily, weekly, monthly, yearly)
     * - startDate: Start date for custom range (ISO 8601 format)
     * - endDate: End date for custom range (ISO 8601 format)
     *
     * Response includes:
     * - totalEmissions: Total CO2 emissions in kg
     * - emissionsByOperation: Breakdown by operation type
     * - emissionsByPeriod: Time-series emission data
     * - offsetCredits: Carbon offset credits purchased
     * - netEmissions: Total emissions minus offsets
     * - reductionTargets: Emission reduction goals and progress
     */
    @GET
    @Path("/emissions")
    @Operation(
        summary = "Get carbon emissions",
        description = "Returns carbon emission metrics for blockchain operations with time-series data"
    )
    @APIResponses({
        @APIResponse(
            responseCode = "200",
            description = "Emissions data retrieved successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = CarbonEmissions.class)
            )
        ),
        @APIResponse(
            responseCode = "400",
            description = "Invalid query parameters"
        ),
        @APIResponse(
            responseCode = "500",
            description = "Internal server error"
        )
    })
    public Uni<Response> getEmissions(
            @QueryParam("timeRange") @DefaultValue("monthly") String timeRange,
            @QueryParam("startDate") String startDate,
            @QueryParam("endDate") String endDate) {

        return Uni.createFrom().item(() -> {
            try {
                LOG.infof("GET /api/v11/carbon/emissions - timeRange=%s", timeRange);

                // Validate time range
                if (!Arrays.asList("daily", "weekly", "monthly", "yearly", "custom").contains(timeRange)) {
                    return Response.status(Response.Status.BAD_REQUEST)
                        .entity(Map.of("error", "Invalid timeRange parameter"))
                        .build();
                }

                // Generate emission metrics
                CarbonEmissions emissions = generateEmissionMetrics(timeRange, startDate, endDate);

                LOG.infof("Carbon emissions: total=%.2f kg CO2, net=%.2f kg CO2",
                    emissions.totalEmissions(), emissions.netEmissions());

                return Response.ok(emissions).build();

            } catch (Exception e) {
                LOG.errorf(e, "Failed to retrieve carbon emissions");
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Failed to retrieve emissions", "message", e.getMessage()))
                    .build();
            }
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    /**
     * POST /api/v11/carbon/report
     *
     * Submit a carbon offset report.
     *
     * Request body:
     * - reportType: Type of report (OFFSET_PURCHASE, RENEWABLE_ENERGY, EFFICIENCY_IMPROVEMENT)
     * - offsetAmount: Amount of CO2 offset in kg
     * - offsetProvider: Name of offset provider
     * - verificationCertificate: Certificate number
     * - cost: Cost of offset in USD
     * - description: Report description
     * - submittedBy: User submitting the report
     *
     * Response:
     * - reportId: Unique report identifier
     * - status: Report status (PENDING, VERIFIED, REJECTED)
     * - submittedAt: Submission timestamp
     * - verificationRequired: Whether verification is needed
     */
    @POST
    @Path("/report")
    @Operation(
        summary = "Submit carbon offset report",
        description = "Submit a carbon offset report for validation and tracking"
    )
    @APIResponses({
        @APIResponse(
            responseCode = "201",
            description = "Report submitted successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = CarbonReportResponse.class)
            )
        ),
        @APIResponse(
            responseCode = "400",
            description = "Invalid report data"
        ),
        @APIResponse(
            responseCode = "500",
            description = "Internal server error"
        )
    })
    public Uni<Response> submitReport(CarbonReportRequest request) {
        return Uni.createFrom().item(() -> {
            try {
                LOG.infof("POST /api/v11/carbon/report - type=%s, offset=%.2f kg",
                    request.reportType(), request.offsetAmount());

                // Validate request
                if (request.offsetAmount() <= 0) {
                    return Response.status(Response.Status.BAD_REQUEST)
                        .entity(Map.of("error", "Offset amount must be greater than 0"))
                        .build();
                }

                if (request.reportType() == null || request.reportType().trim().isEmpty()) {
                    return Response.status(Response.Status.BAD_REQUEST)
                        .entity(Map.of("error", "Report type is required"))
                        .build();
                }

                // Create report
                long reportId = reportIdCounter++;
                Instant submittedAt = Instant.now();

                CarbonReport report = new CarbonReport(
                    reportId,
                    request.reportType(),
                    request.offsetAmount(),
                    request.offsetProvider(),
                    request.verificationCertificate(),
                    request.cost(),
                    request.description(),
                    request.submittedBy(),
                    "PENDING",
                    submittedAt,
                    null
                );

                reports.add(report);

                // Create response
                CarbonReportResponse response = new CarbonReportResponse(
                    reportId,
                    "PENDING",
                    submittedAt,
                    true, // verificationRequired
                    "Report submitted successfully and pending verification",
                    System.currentTimeMillis()
                );

                LOG.infof("Carbon report created: id=%d, status=PENDING", reportId);

                return Response.status(Response.Status.CREATED)
                    .entity(response)
                    .build();

            } catch (Exception e) {
                LOG.errorf(e, "Failed to submit carbon report");
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Failed to submit report", "message", e.getMessage()))
                    .build();
            }
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    /**
     * GET /api/v11/carbon/summary
     *
     * Returns summary of carbon tracking metrics.
     */
    @GET
    @Path("/summary")
    @Operation(
        summary = "Get carbon tracking summary",
        description = "Returns summary statistics for carbon tracking and offset programs"
    )
    @APIResponse(responseCode = "200", description = "Summary retrieved successfully")
    public Uni<Response> getSummary() {
        return Uni.createFrom().item(() -> {
            try {
                LOG.info("GET /api/v11/carbon/summary");

                // Calculate summary metrics
                double totalOffsets = reports.stream()
                    .mapToDouble(CarbonReport::offsetAmount)
                    .sum();

                long verifiedReports = reports.stream()
                    .filter(r -> "VERIFIED".equals(r.status()))
                    .count();

                double totalCost = reports.stream()
                    .mapToDouble(CarbonReport::cost)
                    .sum();

                Map<String, Object> summary = new HashMap<>();
                summary.put("totalEmissions", 45678.90); // kg CO2
                summary.put("totalOffsets", totalOffsets);
                summary.put("netEmissions", 45678.90 - totalOffsets);
                summary.put("offsetPercentage", (totalOffsets / 45678.90) * 100);
                summary.put("totalReports", reports.size());
                summary.put("verifiedReports", verifiedReports);
                summary.put("pendingReports", reports.size() - verifiedReports);
                summary.put("totalInvestment", totalCost);
                summary.put("reductionTarget", 50000.0); // kg CO2
                summary.put("targetProgress", ((45678.90 - totalOffsets) / 50000.0) * 100);
                summary.put("carbonNeutralGoal", "2025-12-31");
                summary.put("timestamp", System.currentTimeMillis());

                return Response.ok(summary).build();

            } catch (Exception e) {
                LOG.errorf(e, "Failed to retrieve carbon summary");
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Failed to retrieve summary", "message", e.getMessage()))
                    .build();
            }
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    /**
     * GET /api/v11/carbon/reports
     *
     * Returns list of submitted carbon reports.
     */
    @GET
    @Path("/reports")
    @Operation(
        summary = "Get carbon reports",
        description = "Returns list of all submitted carbon offset reports"
    )
    @APIResponse(responseCode = "200", description = "Reports retrieved successfully")
    public Uni<Response> getReports(
            @QueryParam("status") String statusFilter,
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("20") int size) {

        return Uni.createFrom().item(() -> {
            try {
                LOG.infof("GET /api/v11/carbon/reports - status=%s, page=%d, size=%d",
                    statusFilter, page, size);

                // Filter reports
                List<CarbonReport> filteredReports = reports.stream()
                    .filter(r -> statusFilter == null || r.status().equals(statusFilter))
                    .sorted((r1, r2) -> r2.submittedAt().compareTo(r1.submittedAt()))
                    .toList();

                // Paginate
                int totalReports = filteredReports.size();
                int totalPages = (int) Math.ceil((double) totalReports / size);
                int fromIndex = page * size;
                int toIndex = Math.min(fromIndex + size, totalReports);

                List<CarbonReport> paginatedReports = filteredReports.subList(
                    Math.min(fromIndex, totalReports),
                    Math.min(toIndex, totalReports)
                );

                Map<String, Object> response = new HashMap<>();
                response.put("reports", paginatedReports);
                response.put("totalReports", totalReports);
                response.put("page", page);
                response.put("size", size);
                response.put("totalPages", totalPages);
                response.put("hasNext", page + 1 < totalPages);
                response.put("hasPrevious", page > 0);
                response.put("timestamp", System.currentTimeMillis());

                return Response.ok(response).build();

            } catch (Exception e) {
                LOG.errorf(e, "Failed to retrieve carbon reports");
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Failed to retrieve reports", "message", e.getMessage()))
                    .build();
            }
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    // ==================== HELPER METHODS ====================

    /**
     * Generate carbon emission metrics
     */
    private CarbonEmissions generateEmissionMetrics(String timeRange, String startDate, String endDate) {
        // Mock emission data (in production, calculate from actual blockchain operations)
        Map<String, Double> emissionsByOperation = new HashMap<>();
        emissionsByOperation.put("consensusValidation", 12345.67);
        emissionsByOperation.put("transactionProcessing", 8901.23);
        emissionsByOperation.put("stateStorage", 6543.21);
        emissionsByOperation.put("networkCommunication", 5432.10);
        emissionsByOperation.put("cryptographicOperations", 4567.89);
        emissionsByOperation.put("smartContractExecution", 3456.78);
        emissionsByOperation.put("dataReplication", 2345.67);
        emissionsByOperation.put("monitoring", 1234.56);
        emissionsByOperation.put("other", 1852.79);

        double totalEmissions = emissionsByOperation.values().stream()
            .mapToDouble(Double::doubleValue)
            .sum();

        // Time-series data
        List<EmissionPeriod> emissionsByPeriod = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            emissionsByPeriod.add(new EmissionPeriod(
                "2024-" + String.format("%02d", i + 1),
                3500.0 + (i * 250.5),
                300.0 + (i * 25.5),
                3200.0 + (i * 225.0)
            ));
        }

        // Offset credits
        double offsetCredits = reports.stream()
            .filter(r -> "VERIFIED".equals(r.status()))
            .mapToDouble(CarbonReport::offsetAmount)
            .sum();

        // Reduction targets
        Map<String, Object> reductionTargets = new HashMap<>();
        reductionTargets.put("targetYear", 2025);
        reductionTargets.put("baselineEmissions", 50000.0);
        reductionTargets.put("targetEmissions", 25000.0);
        reductionTargets.put("reductionPercentage", 50.0);
        reductionTargets.put("currentProgress", ((50000.0 - totalEmissions) / 50000.0) * 100);

        return new CarbonEmissions(
            totalEmissions,
            emissionsByOperation,
            emissionsByPeriod,
            offsetCredits,
            totalEmissions - offsetCredits,
            reductionTargets,
            timeRange,
            System.currentTimeMillis()
        );
    }

    // ==================== DATA MODELS ====================

    /**
     * Carbon emissions response
     */
    public record CarbonEmissions(
        double totalEmissions,
        Map<String, Double> emissionsByOperation,
        List<EmissionPeriod> emissionsByPeriod,
        double offsetCredits,
        double netEmissions,
        Map<String, Object> reductionTargets,
        String timeRange,
        long timestamp
    ) {}

    /**
     * Emission period data
     */
    public record EmissionPeriod(
        String period,
        double emissions,
        double offsets,
        double netEmissions
    ) {}

    /**
     * Carbon report request
     */
    public record CarbonReportRequest(
        String reportType,
        double offsetAmount,
        String offsetProvider,
        String verificationCertificate,
        double cost,
        String description,
        String submittedBy
    ) {}

    /**
     * Carbon report response
     */
    public record CarbonReportResponse(
        long reportId,
        String status,
        Instant submittedAt,
        boolean verificationRequired,
        String message,
        long timestamp
    ) {}

    /**
     * Carbon report entity
     */
    public record CarbonReport(
        long reportId,
        String reportType,
        double offsetAmount,
        String offsetProvider,
        String verificationCertificate,
        double cost,
        String description,
        String submittedBy,
        String status,
        Instant submittedAt,
        Instant verifiedAt
    ) {}
}
