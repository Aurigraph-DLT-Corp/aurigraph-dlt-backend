package io.aurigraph.v11.live;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;

/**
 * Live Data API Resource
 *
 * Provides real-time monitoring endpoints for the Aurigraph V11 platform.
 * This resource exposes live data about validators, network state, and
 * performance metrics.
 *
 * Endpoints:
 * - GET /api/v11/live/validators - Get live validator monitoring data
 * - GET /api/v11/live/validators/{id} - Get specific validator details
 *
 * All endpoints return reactive Uni types for non-blocking operation
 * and leverage Java 21 virtual threads for high concurrency.
 *
 * @author Backend Development Agent (BDA) - Real-time Data Specialist
 * @ticket AV11-268
 * @version 1.0.0
 */
@Path("/api/v11/live")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Live Data API", description = "Real-time monitoring and live data endpoints")
public class LiveDataResource {

    private static final Logger LOG = Logger.getLogger(LiveDataResource.class);

    @Inject
    LiveValidatorsService validatorService;

    /**
     * Get live validator monitoring data
     *
     * Returns real-time status and performance metrics for all validators
     * in the network. Data is updated every 5 seconds by background threads.
     *
     * Response includes:
     * - Validator identity (ID, public key, name)
     * - Current status (ACTIVE, INACTIVE, JAILED)
     * - Performance metrics (uptime, blocks produced/missed)
     * - Staking information (stake amount, voting power)
     * - Last activity timestamps
     * - Network-wide statistics
     *
     * @return ValidatorStatusResponse with all validators and network stats
     */
    @GET
    @Path("/validators")
    @Operation(
        summary = "Get live validators monitoring",
        description = "Retrieve real-time validator status and performance metrics for all validators in the network"
    )
    @APIResponses({
        @APIResponse(
            responseCode = "200",
            description = "Successfully retrieved live validator data",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON,
                schema = @Schema(implementation = LiveValidatorsService.ValidatorStatusResponse.class)
            )
        ),
        @APIResponse(
            responseCode = "500",
            description = "Internal server error"
        )
    })
    public Uni<LiveValidatorsService.ValidatorStatusResponse> getLiveValidators() {
        LOG.info("API Request: GET /api/v11/live/validators - Fetching live validator data");

        return Uni.createFrom().item(() -> {
            try {
                LiveValidatorsService.ValidatorStatusResponse response = validatorService.getLiveValidatorStatus();
                LOG.infof("Live validators retrieved: %d total, %d active, %d inactive, %d jailed",
                         response.totalValidators(),
                         response.activeValidators(),
                         response.inactiveValidators(),
                         response.jailedValidators());
                return response;
            } catch (Exception e) {
                LOG.errorf(e, "Error fetching live validators: %s", e.getMessage());
                throw e;
            }
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    /**
     * Get specific validator details
     *
     * Returns detailed real-time information for a specific validator
     * identified by validator ID.
     *
     * @param validatorId The unique validator identifier (e.g., "validator_001")
     * @return ValidatorStatusDTO or 404 if not found
     */
    @GET
    @Path("/validators/{validatorId}")
    @Operation(
        summary = "Get specific validator details",
        description = "Retrieve real-time detailed information for a specific validator by ID"
    )
    @APIResponses({
        @APIResponse(
            responseCode = "200",
            description = "Validator found and details returned",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON,
                schema = @Schema(implementation = LiveValidatorsService.ValidatorStatusDTO.class)
            )
        ),
        @APIResponse(
            responseCode = "404",
            description = "Validator not found"
        )
    })
    public Uni<Response> getValidatorById(@PathParam("validatorId") String validatorId) {
        LOG.infof("API Request: GET /api/v11/live/validators/%s - Fetching validator details", validatorId);

        return Uni.createFrom().item(() -> {
            try {
                LiveValidatorsService.ValidatorStatusDTO validator = validatorService.getValidatorById(validatorId);

                if (validator == null) {
                    LOG.warnf("Validator not found: %s", validatorId);
                    return Response.status(Response.Status.NOT_FOUND)
                        .entity(new ErrorResponse(
                            "NOT_FOUND",
                            "Validator not found with ID: " + validatorId,
                            validatorId
                        ))
                        .build();
                }

                LOG.infof("Validator %s details retrieved: status=%s, uptime=%.2f%%, votingPower=%.2f%%",
                         validatorId, validator.status(), validator.uptime(), validator.votingPower());

                return Response.ok(validator).build();
            } catch (Exception e) {
                LOG.errorf(e, "Error fetching validator %s: %s", validatorId, e.getMessage());
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse(
                        "INTERNAL_ERROR",
                        "Error retrieving validator: " + e.getMessage(),
                        validatorId
                    ))
                    .build();
            }
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    /**
     * Health check endpoint for live validators monitoring
     *
     * Returns the health status of the live validator monitoring system.
     * This can be used to verify that the background update thread
     * is running and validators are being monitored.
     *
     * @return Health status response
     */
    @GET
    @Path("/validators/health")
    @Operation(
        summary = "Live validators service health check",
        description = "Check the health status of the live validator monitoring system"
    )
    public Uni<Response> validatorsHealthCheck() {
        return Uni.createFrom().item(() -> {
            try {
                LiveValidatorsService.ValidatorStatusResponse status = validatorService.getLiveValidatorStatus();

                return Response.ok(new HealthCheckResponse(
                    "UP",
                    "Live validator monitoring operational",
                    status.totalValidators(),
                    status.activeValidators(),
                    status.networkBlockHeight()
                )).build();
            } catch (Exception e) {
                LOG.error("Live validator health check failed", e);
                return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                    .entity(new HealthCheckResponse(
                        "DOWN",
                        "Live validator monitoring unavailable: " + e.getMessage(),
                        0,
                        0,
                        0L
                    ))
                    .build();
            }
        });
    }

    // ==================== DTOs ====================

    /**
     * Error response for API errors
     */
    public record ErrorResponse(
        String errorCode,
        String message,
        String requestedResource
    ) {}

    /**
     * Health check response
     */
    public record HealthCheckResponse(
        String status,
        String message,
        int totalValidators,
        int activeValidators,
        long networkBlockHeight
    ) {}
}
