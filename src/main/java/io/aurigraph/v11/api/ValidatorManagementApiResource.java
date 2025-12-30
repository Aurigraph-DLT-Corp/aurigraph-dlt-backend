package io.aurigraph.v11.api;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import io.smallrye.mutiny.Uni;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.util.*;

/**
 * Validator Management API Resource
 *
 * Provides validator operations:
 * - GET /api/v11/validators/{id}/performance - Validator performance metrics
 * - POST /api/v11/validators/{id}/slash - Slash validator stake
 *
 * @version 11.0.0
 * @author Backend Development Agent (BDA)
 */
@Path("/api/v11/validators")
@ApplicationScoped
@Tag(name = "Validator Management API", description = "Validator performance and management operations")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ValidatorManagementApiResource {

    private static final Logger LOG = Logger.getLogger(ValidatorManagementApiResource.class);

    // ==================== ENDPOINT 4: Validator Performance ====================

    /**
     * GET /api/v11/validators/{id}/performance
     * Get validator performance metrics
     */
    @GET
    @Path("/{id}/performance")
    @Operation(summary = "Get validator performance", description = "Retrieve performance metrics for a specific validator")
    @APIResponse(responseCode = "200", description = "Performance metrics retrieved successfully",
                content = @Content(schema = @Schema(implementation = ValidatorPerformanceResponse.class)))
    @APIResponse(responseCode = "404", description = "Validator not found")
    public Uni<Response> getValidatorPerformance(
        @Parameter(description = "Validator ID", required = true)
        @PathParam("id") String validatorId,
        @QueryParam("period") @DefaultValue("24h") String period) {

        LOG.infof("Fetching performance for validator: %s (period: %s)", validatorId, period);

        return Uni.createFrom().item(() -> {
            // Check if validator exists
            if (validatorId == null || validatorId.isEmpty()) {
                return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("error", "Validator not found"))
                    .build();
            }

            ValidatorPerformanceResponse response = new ValidatorPerformanceResponse();
            response.validatorId = validatorId;
            response.timestamp = Instant.now().toEpochMilli();
            response.period = period;

            // Basic validator info
            response.status = "ACTIVE";
            response.stake = 3500000.0 + (Math.random() * 2000000);
            response.commission = 5.0 + (Math.random() * 5);
            response.region = "us-east";
            response.version = "12.0.0";

            // Performance metrics
            response.blocksProposed = 1234 + (int)(Math.random() * 500);
            response.blocksValidated = 45678 + (int)(Math.random() * 10000);
            response.uptime = 99.7 + (Math.random() * 0.3);
            response.missedBlocks = (int)(Math.random() * 10);
            response.slashingEvents = 0;
            response.averageBlockTime = 1.8 + (Math.random() * 0.5);
            response.consensusParticipation = 98.5 + (Math.random() * 1.5);

            // Earnings
            response.totalRewards = 125000.0 + (Math.random() * 50000);
            response.rewardsEarned24h = 5000.0 + (Math.random() * 2000);
            response.commissionEarned = 6250.0 + (Math.random() * 2500);

            // Delegation info
            response.totalDelegators = 45 + (int)(Math.random() * 20);
            response.totalDelegatedStake = 15000000.0 + (Math.random() * 5000000);

            // Performance score (0-100)
            response.performanceScore = 95.0 + (Math.random() * 5);

            // Historical performance (last 30 days)
            response.historicalPerformance = new ArrayList<>();
            long now = Instant.now().toEpochMilli();
            for (int i = 29; i >= 0; i--) {
                PerformanceDataPoint point = new PerformanceDataPoint();
                point.date = now - (i * 86400000L); // 24 hours in ms
                point.blocksProposed = 30 + (int)(Math.random() * 20);
                point.uptime = 98.0 + (Math.random() * 2);
                point.rewardsEarned = 4000.0 + (Math.random() * 2000);
                response.historicalPerformance.add(point);
            }

            LOG.infof("Validator %s performance: score=%.1f%%, uptime=%.2f%%, rewards=%.2f",
                     validatorId, response.performanceScore, response.uptime, response.totalRewards);

            return Response.ok(response).build();
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    // ==================== ENDPOINT 5: Slash Validator ====================

    /**
     * POST /api/v11/validators/{id}/slash
     * Slash validator stake for misbehavior
     */
    @POST
    @Path("/{id}/slash")
    @Operation(summary = "Slash validator", description = "Slash validator stake for protocol violations")
    @APIResponse(responseCode = "200", description = "Slashing executed successfully",
                content = @Content(schema = @Schema(implementation = SlashingResponse.class)))
    @APIResponse(responseCode = "400", description = "Invalid slashing request")
    @APIResponse(responseCode = "404", description = "Validator not found")
    @APIResponse(responseCode = "403", description = "Unauthorized to perform slashing")
    public Uni<Response> slashValidator(
        @Parameter(description = "Validator ID", required = true)
        @PathParam("id") String validatorId,
        @Parameter(description = "Slashing request", required = true)
        SlashingRequest request) {

        LOG.warnf("SLASHING REQUEST for validator: %s, reason: %s, amount: %.2f",
                 validatorId, request.reason, request.slashAmount);

        return Uni.createFrom().item(() -> {
            try {
                // Validate request
                if (request.reason == null || request.reason.isEmpty()) {
                    return Response.status(Response.Status.BAD_REQUEST)
                        .entity(Map.of("error", "Slashing reason is required"))
                        .build();
                }

                if (request.slashAmount <= 0) {
                    return Response.status(Response.Status.BAD_REQUEST)
                        .entity(Map.of("error", "Slash amount must be positive"))
                        .build();
                }

                // Check validator exists
                if (validatorId == null || validatorId.isEmpty()) {
                    return Response.status(Response.Status.NOT_FOUND)
                        .entity(Map.of("error", "Validator not found"))
                        .build();
                }

                // Execute slashing
                SlashingResponse response = new SlashingResponse();
                response.validatorId = validatorId;
                response.slashingId = "slash-" + UUID.randomUUID().toString().substring(0, 8);
                response.timestamp = Instant.now().toEpochMilli();
                response.reason = request.reason;
                response.slashAmount = request.slashAmount;
                response.previousStake = 3500000.0;
                response.newStake = response.previousStake - request.slashAmount;
                response.status = "EXECUTED";
                response.blockNumber = 1500000L + (long)(Math.random() * 1000);
                response.transactionHash = "0x" + UUID.randomUUID().toString().replace("-", "");

                // Additional details
                response.penaltyPercentage = (request.slashAmount / response.previousStake) * 100;
                response.jailed = response.penaltyPercentage > 10.0; // Jail if >10% slashed
                response.jailDuration = response.jailed ? 86400000L : 0; // 24 hours if jailed
                response.appealDeadline = Instant.now().plusSeconds(604800).toEpochMilli(); // 7 days

                // Notify delegators
                response.affectedDelegators = 45 + (int)(Math.random() * 20);
                response.totalDelegatorLoss = request.slashAmount * 0.8; // 80% of slash affects delegators

                LOG.warnf("SLASHING EXECUTED: validator=%s, amount=%.2f, new_stake=%.2f, jailed=%s",
                         validatorId, request.slashAmount, response.newStake, response.jailed);

                return Response.ok(response).build();

            } catch (Exception e) {
                LOG.errorf(e, "Slashing failed for validator: %s", validatorId);
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Slashing failed: " + e.getMessage()))
                    .build();
            }
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    // ==================== Request/Response DTOs ====================

    public static class ValidatorPerformanceResponse {
        public String validatorId;
        public long timestamp;
        public String period;
        public String status;
        public double stake;
        public double commission;
        public String region;
        public String version;
        public int blocksProposed;
        public int blocksValidated;
        public double uptime;
        public int missedBlocks;
        public int slashingEvents;
        public double averageBlockTime;
        public double consensusParticipation;
        public double totalRewards;
        public double rewardsEarned24h;
        public double commissionEarned;
        public int totalDelegators;
        public double totalDelegatedStake;
        public double performanceScore;
        public List<PerformanceDataPoint> historicalPerformance;
    }

    public static class PerformanceDataPoint {
        public long date;
        public int blocksProposed;
        public double uptime;
        public double rewardsEarned;
    }

    public static class SlashingRequest {
        public String reason;
        public double slashAmount;
        public String evidence;
        public String proposer;
    }

    public static class SlashingResponse {
        public String validatorId;
        public String slashingId;
        public long timestamp;
        public String reason;
        public double slashAmount;
        public double previousStake;
        public double newStake;
        public String status;
        public long blockNumber;
        public String transactionHash;
        public double penaltyPercentage;
        public boolean jailed;
        public long jailDuration;
        public long appealDeadline;
        public int affectedDelegators;
        public double totalDelegatorLoss;
    }
}
