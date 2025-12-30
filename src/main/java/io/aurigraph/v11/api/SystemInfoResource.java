package io.aurigraph.v11.api;

import io.aurigraph.v11.models.SystemInfo;
import io.aurigraph.v11.services.SystemInfoService;
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
 * System Information API
 * Provides platform version and configuration details
 *
 * AV11-290: Implement System Information API
 *
 * Endpoints:
 * - GET /api/v11/info - Get comprehensive system information
 * - GET /api/v11/info/version - Get version information only
 * - GET /api/v11/info/health - Get quick health status
 *
 * @author Aurigraph V11
 * @version 11.3.0
 */
@Path("/api/v11/info")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "System Information", description = "Platform version and configuration details")
public class SystemInfoResource {

    private static final Logger LOG = Logger.getLogger(SystemInfoResource.class);

    @Inject
    SystemInfoService systemInfoService;

    /**
     * Get comprehensive system information
     *
     * Returns complete platform information including:
     * - Platform name, version, environment
     * - Runtime details (Java, Quarkus, GraalVM)
     * - Enabled features and modules
     * - Network configuration
     * - Build information
     *
     * @return SystemInfo with all platform details
     */
    @GET
    @Operation(summary = "Get system information",
               description = "Returns comprehensive platform information including version, runtime, features, and configuration")
    @APIResponses(value = {
            @APIResponse(responseCode = "200",
                         description = "System information retrieved successfully",
                         content = @Content(mediaType = MediaType.APPLICATION_JSON,
                                          schema = @Schema(implementation = SystemInfo.class))),
            @APIResponse(responseCode = "500",
                         description = "Internal server error")
    })
    public Uni<Response> getSystemInfo() {
        LOG.info("GET /api/v11/info - Fetching system information");

        return systemInfoService.getSystemInfo()
                .map(info -> {
                    LOG.debugf("System info retrieved: %s v%s",
                            info.getPlatform().getName(),
                            info.getPlatform().getVersion());

                    return Response.ok(info).build();
                })
                .onFailure().recoverWithItem(throwable -> {
                    LOG.error("Error retrieving system information", throwable);
                    return (Response) Response.serverError()
                            .entity(Map.of(
                                    "error", "Failed to retrieve system information",
                                    "message", throwable.getMessage()
                            ))
                            .build();
                });
    }

    /**
     * Get version information only
     *
     * Lightweight endpoint that returns only version details
     *
     * @return Version information
     */
    @GET
    @Path("/version")
    @Operation(summary = "Get version information",
               description = "Returns lightweight version information only")
    @APIResponses(value = {
            @APIResponse(responseCode = "200",
                         description = "Version information retrieved successfully"),
            @APIResponse(responseCode = "500",
                         description = "Internal server error")
    })
    public Uni<Response> getVersionInfo() {
        LOG.debug("GET /api/v11/info/version - Fetching version information");

        return systemInfoService.getSystemInfo()
                .map(info -> {
                    Map<String, Object> version = Map.of(
                            "platform", info.getPlatform().getName(),
                            "version", info.getPlatform().getVersion(),
                            "api_version", info.getFeatures().getApiVersion(),
                            "build_version", info.getBuild().getVersion(),
                            "environment", info.getPlatform().getEnvironment()
                    );

                    return Response.ok(version).build();
                })
                .onFailure().recoverWithItem(throwable -> {
                    LOG.error("Error retrieving version information", throwable);
                    return (Response) Response.serverError()
                            .entity(Map.of(
                                    "error", "Failed to retrieve version information",
                                    "message", throwable.getMessage()
                            ))
                            .build();
                });
    }

    /**
     * Get quick health status
     *
     * Minimal endpoint for quick status checks
     *
     * @return Health status with uptime
     */
    @GET
    @Path("/health")
    @Operation(summary = "Get system health status",
               description = "Returns quick health status with uptime")
    @APIResponses(value = {
            @APIResponse(responseCode = "200",
                         description = "Health status retrieved successfully"),
            @APIResponse(responseCode = "500",
                         description = "Internal server error")
    })
    public Uni<Response> getHealthStatus() {
        LOG.debug("GET /api/v11/info/health - Fetching health status");

        return Uni.createFrom().item(() -> {
            Map<String, Object> health = Map.of(
                    "status", "online",
                    "uptime_seconds", systemInfoService.getUptimeSeconds(),
                    "timestamp", java.time.Instant.now().toString()
            );

            return Response.ok(health).build();
        }).onFailure().recoverWithItem(throwable -> {
            LOG.error("Error retrieving health status", throwable);
            return (Response) Response.serverError()
                    .entity(Map.of(
                            "error", "Failed to retrieve health status",
                            "message", throwable.getMessage()
                    ))
                    .build();
        });
    }
}
