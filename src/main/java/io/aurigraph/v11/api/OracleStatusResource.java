package io.aurigraph.v11.api;

import io.aurigraph.v11.models.OracleStatus;
import io.aurigraph.v11.services.OracleStatusService;
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
 * Oracle Status API
 * Provides oracle service health monitoring and performance metrics
 *
 * AV11-285: Implement Oracle Status API
 *
 * Endpoints:
 * - GET /api/v11/oracles/status - Get overall oracle status
 * - GET /api/v11/oracles/{oracleId} - Get specific oracle status
 * - GET /api/v11/oracles/summary - Get oracle summary
 *
 * @author Aurigraph V11
 * @version 11.3.0
 */
@Path("/api/v11/oracles")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Oracle Status", description = "Oracle service health monitoring and performance metrics")
public class OracleStatusResource {

    private static final Logger LOG = Logger.getLogger(OracleStatusResource.class);

    @Inject
    OracleStatusService oracleStatusService;

    /**
     * Get overall oracle status
     *
     * Returns comprehensive oracle health monitoring including all nodes, summary, and health score
     */
    @GET
    @Path("/status")
    @Operation(summary = "Get oracle status",
               description = "Returns comprehensive oracle health monitoring with all nodes, summary, and health score")
    @APIResponses(value = {
            @APIResponse(responseCode = "200",
                         description = "Oracle status retrieved successfully",
                         content = @Content(mediaType = MediaType.APPLICATION_JSON,
                                          schema = @Schema(implementation = OracleStatus.class))),
            @APIResponse(responseCode = "500",
                         description = "Internal server error")
    })
    public Uni<Response> getOracleStatus() {
        LOG.info("GET /api/v11/oracles/status - Fetching oracle status");

        return oracleStatusService.getOracleStatus()
                .map(status -> {
                    LOG.debugf("Oracle status retrieved: %d oracles, %.1f%% health score",
                            (Object) status.getOracles().size(),
                            (Object) status.getHealthScore());

                    return Response.ok(status).build();
                })
                .onFailure().recoverWithItem(throwable -> {
                    LOG.error("Error retrieving oracle status", throwable);
                    return (Response) Response.serverError()
                            .entity(Map.of(
                                    "error", "Failed to retrieve oracle status",
                                    "message", throwable.getMessage()
                            ))
                            .build();
                });
    }

    /**
     * Get specific oracle status
     *
     * Returns detailed status for a specific oracle node
     */
    @GET
    @Path("/{oracleId}")
    @Operation(summary = "Get specific oracle",
               description = "Returns detailed status for a specific oracle node")
    @APIResponses(value = {
            @APIResponse(responseCode = "200",
                         description = "Oracle retrieved successfully"),
            @APIResponse(responseCode = "404",
                         description = "Oracle not found"),
            @APIResponse(responseCode = "500",
                         description = "Internal server error")
    })
    public Uni<Response> getOracleById(@PathParam("oracleId") String oracleId) {
        LOG.debugf("GET /api/v11/oracles/%s - Fetching oracle details", oracleId);

        return oracleStatusService.getOracleById(oracleId)
                .map(oracle -> {
                    LOG.debugf("Oracle retrieved: %s (%s) - %s",
                            oracle.getOracleName(),
                            oracle.getOracleType(),
                            oracle.getStatus());

                    return Response.ok(oracle).build();
                })
                .onFailure(IllegalArgumentException.class).recoverWithItem(throwable -> {
                    LOG.warnf("Oracle not found: %s", oracleId);
                    return (Response) Response.status(Response.Status.NOT_FOUND)
                            .entity(Map.of(
                                    "error", "Oracle not found",
                                    "message", throwable.getMessage()
                            ))
                            .build();
                })
                .onFailure().recoverWithItem(throwable -> {
                    LOG.error("Error retrieving oracle", throwable);
                    return (Response) Response.serverError()
                            .entity(Map.of(
                                    "error", "Failed to retrieve oracle",
                                    "message", throwable.getMessage()
                            ))
                            .build();
                });
    }

    /**
     * Get oracle summary
     *
     * Returns summary statistics for all oracles
     */
    @GET
    @Path("/summary")
    @Operation(summary = "Get oracle summary",
               description = "Returns summary statistics including totals, averages, and type distribution")
    @APIResponses(value = {
            @APIResponse(responseCode = "200",
                         description = "Oracle summary retrieved successfully"),
            @APIResponse(responseCode = "500",
                         description = "Internal server error")
    })
    public Uni<Response> getOracleSummary() {
        LOG.debug("GET /api/v11/oracles/summary - Fetching oracle summary");

        return oracleStatusService.getOracleStatus()
                .map(status -> {
                    Map<String, Object> summaryInfo = Map.of(
                            "summary", status.getSummary(),
                            "health_score", status.getHealthScore(),
                            "timestamp", status.getTimestamp()
                    );

                    return Response.ok(summaryInfo).build();
                })
                .onFailure().recoverWithItem(throwable -> {
                    LOG.error("Error retrieving oracle summary", throwable);
                    return (Response) Response.serverError()
                            .entity(Map.of(
                                    "error", "Failed to retrieve oracle summary",
                                    "message", throwable.getMessage()
                            ))
                            .build();
                });
    }
}
