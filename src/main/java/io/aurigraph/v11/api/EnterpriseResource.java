package io.aurigraph.v11.api;

import io.aurigraph.v11.models.EnterpriseStatus;
import io.aurigraph.v11.services.EnterpriseService;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.Map;

/**
 * Enterprise Dashboard API
 * Provides enterprise features overview and multi-tenancy status
 *
 * AV11-283: Implement Enterprise Dashboard API
 *
 * Endpoints:
 * - GET /api/v11/enterprise/status - Get enterprise dashboard status
 * - GET /api/v11/enterprise/tenants/summary - Get tenant summary
 * - GET /api/v11/enterprise/features - Get feature status
 * - GET /api/v11/enterprise/usage - Get usage metrics
 *
 * @author Aurigraph V11
 * @version 11.3.0
 */
@Path("/api/v11/enterprise")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Enterprise Dashboard", description = "Enterprise features overview and multi-tenancy management")
public class EnterpriseResource {

    private static final Logger LOG = Logger.getLogger(EnterpriseResource.class);

    @Inject
    EnterpriseService enterpriseService;

    /**
     * Get enterprise dashboard status
     *
     * Returns comprehensive enterprise overview including:
     * - Tenant summary (total, active, trial, suspended)
     * - Enterprise features status
     * - Usage metrics (transactions, storage, API calls)
     * - Compliance information (GDPR, HIPAA, SOC2, ISO27001)
     * - Support tier and SLA information
     */
    @GET
    @Path("/status")
    @Operation(summary = "Get enterprise dashboard status",
               description = "Returns comprehensive enterprise overview with tenant summary, features, usage, and compliance info")
    @APIResponses(value = {
            @APIResponse(responseCode = "200",
                         description = "Enterprise status retrieved successfully",
                         content = @Content(mediaType = MediaType.APPLICATION_JSON,
                                          schema = @Schema(implementation = EnterpriseStatus.class))),
            @APIResponse(responseCode = "500",
                         description = "Internal server error")
    })
    public Uni<Response> getEnterpriseStatus() {
        LOG.info("GET /api/v11/enterprise/status - Fetching enterprise dashboard status");

        return enterpriseService.getEnterpriseStatus()
                .map(status -> {
                    LOG.debugf("Enterprise status retrieved: %d tenants, %d active",
                            status.getTenants().getTotalTenants(),
                            status.getTenants().getActiveTenants());

                    return Response.ok(status).build();
                })
                .onFailure().recoverWithItem(throwable -> {
                    LOG.error("Error retrieving enterprise status", throwable);
                    return (Response) Response.serverError()
                            .entity(Map.of(
                                    "error", "Failed to retrieve enterprise status",
                                    "message", throwable.getMessage()
                            ))
                            .build();
                });
    }

    /**
     * Get tenant summary
     *
     * Returns summary of all tenants and their distribution
     */
    @GET
    @Path("/tenants/summary")
    @Operation(summary = "Get tenant summary",
               description = "Returns summary of all tenants including count and tier distribution")
    @APIResponses(value = {
            @APIResponse(responseCode = "200",
                         description = "Tenant summary retrieved successfully"),
            @APIResponse(responseCode = "500",
                         description = "Internal server error")
    })
    public Uni<Response> getTenantSummary() {
        LOG.debug("GET /api/v11/enterprise/tenants/summary - Fetching tenant summary");

        return enterpriseService.getEnterpriseStatus()
                .map(status -> {
                    Map<String, Object> tenantInfo = Map.of(
                            "total_tenants", status.getTenants().getTotalTenants(),
                            "active_tenants", status.getTenants().getActiveTenants(),
                            "trial_tenants", status.getTenants().getTrialTenants(),
                            "suspended_tenants", status.getTenants().getSuspendedTenants(),
                            "tier_distribution", status.getTenants().getTierDistribution()
                    );

                    return Response.ok(tenantInfo).build();
                })
                .onFailure().recoverWithItem(throwable -> {
                    LOG.error("Error retrieving tenant summary", throwable);
                    return (Response) Response.serverError()
                            .entity(Map.of(
                                    "error", "Failed to retrieve tenant summary",
                                    "message", throwable.getMessage()
                            ))
                            .build();
                });
    }

    /**
     * Get enterprise features status
     *
     * Returns status of all enterprise features
     */
    @GET
    @Path("/features")
    @Operation(summary = "Get enterprise features",
               description = "Returns status and usage of all enterprise features")
    @APIResponses(value = {
            @APIResponse(responseCode = "200",
                         description = "Features retrieved successfully"),
            @APIResponse(responseCode = "500",
                         description = "Internal server error")
    })
    public Uni<Response> getEnterpriseFeatures() {
        LOG.debug("GET /api/v11/enterprise/features - Fetching enterprise features");

        return enterpriseService.getEnterpriseStatus()
                .map(status -> Response.ok(status.getFeatures()).build())
                .onFailure().recoverWithItem(throwable -> {
                    LOG.error("Error retrieving enterprise features", throwable);
                    return (Response) Response.serverError()
                            .entity(Map.of(
                                    "error", "Failed to retrieve enterprise features",
                                    "message", throwable.getMessage()
                            ))
                            .build();
                });
    }

    /**
     * Get usage metrics
     *
     * Returns enterprise usage metrics for last 30 days
     */
    @GET
    @Path("/usage")
    @Operation(summary = "Get usage metrics",
               description = "Returns enterprise usage metrics including transactions, storage, and API calls")
    @APIResponses(value = {
            @APIResponse(responseCode = "200",
                         description = "Usage metrics retrieved successfully"),
            @APIResponse(responseCode = "500",
                         description = "Internal server error")
    })
    public Uni<Response> getUsageMetrics() {
        LOG.debug("GET /api/v11/enterprise/usage - Fetching usage metrics");

        return enterpriseService.getEnterpriseStatus()
                .map(status -> {
                    Map<String, Object> usage = Map.of(
                            "total_transactions_30d", status.getUsage().getTotalTransactions30d(),
                            "total_storage_gb", status.getUsage().getTotalStorageGb(),
                            "api_calls_30d", status.getUsage().getApiCalls30d(),
                            "data_transfer_gb_30d", status.getUsage().getDataTransferGb30d(),
                            "average_tps", status.getUsage().getAverageTps(),
                            "peak_tps", status.getUsage().getPeakTps()
                    );

                    return Response.ok(usage).build();
                })
                .onFailure().recoverWithItem(throwable -> {
                    LOG.error("Error retrieving usage metrics", throwable);
                    return (Response) Response.serverError()
                            .entity(Map.of(
                                    "error", "Failed to retrieve usage metrics",
                                    "message", throwable.getMessage()
                            ))
                            .build();
                });
    }

    /**
     * Get compliance information
     *
     * Returns compliance status and certifications
     */
    @GET
    @Path("/compliance")
    @Operation(summary = "Get compliance information",
               description = "Returns compliance status including GDPR, HIPAA, SOC2, and ISO27001")
    @APIResponses(value = {
            @APIResponse(responseCode = "200",
                         description = "Compliance info retrieved successfully"),
            @APIResponse(responseCode = "500",
                         description = "Internal server error")
    })
    public Uni<Response> getComplianceInfo() {
        LOG.debug("GET /api/v11/enterprise/compliance - Fetching compliance info");

        return enterpriseService.getEnterpriseStatus()
                .map(status -> Response.ok(status.getCompliance()).build())
                .onFailure().recoverWithItem(throwable -> {
                    LOG.error("Error retrieving compliance info", throwable);
                    return (Response) Response.serverError()
                            .entity(Map.of(
                                    "error", "Failed to retrieve compliance information",
                                    "message", throwable.getMessage()
                            ))
                            .build();
                });
    }
}
