package io.aurigraph.v11.api;

import io.aurigraph.v11.models.HSMStatus;
import io.aurigraph.v11.services.HSMStatusService;
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
import java.util.stream.Collectors;

/**
 * HSM Status API
 * Provides Hardware Security Module health and monitoring
 *
 * AV11-287: Implement HSM Status API
 *
 * Endpoints:
 * - GET /api/v11/security/hsm/status - Get HSM status
 * - GET /api/v11/security/hsm/modules - Get HSM modules
 * - GET /api/v11/security/hsm/operations - Get operation statistics
 *
 * @author Aurigraph V11
 * @version 11.3.0
 */
@Path("/api/v11/security/hsm")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "HSM Status", description = "Hardware Security Module health monitoring")
public class HSMStatusResource {

    private static final Logger LOG = Logger.getLogger(HSMStatusResource.class);

    @Inject
    HSMStatusService hsmStatusService;

    /**
     * Get HSM status
     */
    @GET
    @Path("/status")
    @Operation(summary = "Get HSM status",
               description = "Returns comprehensive HSM health and status information")
    @APIResponses(value = {
            @APIResponse(responseCode = "200",
                         description = "HSM status retrieved successfully",
                         content = @Content(mediaType = MediaType.APPLICATION_JSON,
                                          schema = @Schema(implementation = HSMStatus.class))),
            @APIResponse(responseCode = "500",
                         description = "Internal server error")
    })
    public Uni<Response> getHSMStatus() {
        LOG.info("GET /api/v11/security/hsm/status - Fetching HSM status");

        return hsmStatusService.getHSMStatus()
                .map(status -> {
                    LOG.debugf("HSM status retrieved: %s, modules: %d",
                            status.getOverallStatus(),
                            status.getModules().size());

                    return Response.ok(status).build();
                })
                .onFailure().recoverWithItem(throwable -> {
                    LOG.error("Error retrieving HSM status", throwable);
                    return (Response) Response.serverError()
                            .entity(Map.of(
                                    "error", "Failed to retrieve HSM status",
                                    "message", throwable.getMessage()
                            ))
                            .build();
                });
    }

    /**
     * Get HSM modules
     */
    @GET
    @Path("/modules")
    @Operation(summary = "Get HSM modules",
               description = "Returns list of HSM modules and their status")
    @APIResponses(value = {
            @APIResponse(responseCode = "200",
                         description = "HSM modules retrieved successfully"),
            @APIResponse(responseCode = "500",
                         description = "Internal server error")
    })
    public Uni<Response> getHSMModules() {
        LOG.debug("GET /api/v11/security/hsm/modules - Fetching HSM modules");

        return hsmStatusService.getHSMStatus()
                .map(status -> {
                    Map<String, Object> modules = Map.of(
                            "modules", status.getModules(),
                            "count", status.getModules().size(),
                            "overall_status", status.getOverallStatus()
                    );

                    return Response.ok(modules).build();
                })
                .onFailure().recoverWithItem(throwable -> {
                    LOG.error("Error retrieving HSM modules", throwable);
                    return (Response) Response.serverError()
                            .entity(Map.of(
                                    "error", "Failed to retrieve HSM modules",
                                    "message", throwable.getMessage()
                            ))
                            .build();
                });
    }

    /**
     * Get operation statistics
     */
    @GET
    @Path("/operations")
    @Operation(summary = "Get operation statistics",
               description = "Returns HSM operation statistics and throughput")
    @APIResponses(value = {
            @APIResponse(responseCode = "200",
                         description = "Operation statistics retrieved successfully"),
            @APIResponse(responseCode = "500",
                         description = "Internal server error")
    })
    public Uni<Response> getOperationStats() {
        LOG.debug("GET /api/v11/security/hsm/operations - Fetching operation stats");

        return hsmStatusService.getHSMStatus()
                .map(status -> {
                    Map<String, Object> ops = Map.of(
                            "operations_per_second", status.getOperations().getOperationsPerSecond(),
                            "total_operations", status.getOperations().getTotalOperations(),
                            "success_rate", status.getOperations().getSuccessRate(),
                            "average_latency_ms", status.getOperations().getAverageLatencyMs(),
                            "operation_breakdown", status.getOperations().getOperationBreakdown(),
                            "uptime_seconds", hsmStatusService.getUptimeSeconds()
                    );

                    return Response.ok(ops).build();
                })
                .onFailure().recoverWithItem(throwable -> {
                    LOG.error("Error retrieving operation statistics", throwable);
                    return (Response) Response.serverError()
                            .entity(Map.of(
                                    "error", "Failed to retrieve operation statistics",
                                    "message", throwable.getMessage()
                            ))
                            .build();
                });
    }

    /**
     * Get key storage information
     */
    @GET
    @Path("/storage")
    @Operation(summary = "Get key storage information",
               description = "Returns HSM key storage capacity and usage")
    @APIResponses(value = {
            @APIResponse(responseCode = "200",
                         description = "Storage information retrieved successfully"),
            @APIResponse(responseCode = "500",
                         description = "Internal server error")
    })
    public Uni<Response> getKeyStorage() {
        LOG.debug("GET /api/v11/security/hsm/storage - Fetching key storage info");

        return hsmStatusService.getHSMStatus()
                .map(status -> {
                    Map<String, Object> storage = Map.of(
                            "total_keys", status.getKeyStorage().getTotalKeys(),
                            "key_types", status.getKeyStorage().getKeyTypes(),
                            "storage_capacity_keys", status.getKeyStorage().getStorageCapacityKeys(),
                            "storage_used_percent", status.getKeyStorage().getStorageUsedPercent(),
                            "backed_up", status.getKeyStorage().isBackedUp(),
                            "last_backup", status.getKeyStorage().getLastBackup().toString()
                    );

                    return Response.ok(storage).build();
                })
                .onFailure().recoverWithItem(throwable -> {
                    LOG.error("Error retrieving key storage info", throwable);
                    return (Response) Response.serverError()
                            .entity(Map.of(
                                    "error", "Failed to retrieve key storage information",
                                    "message", throwable.getMessage()
                            ))
                            .build();
                });
    }
}
