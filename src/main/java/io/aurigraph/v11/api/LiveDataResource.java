package io.aurigraph.v11.api;

import io.aurigraph.v11.consensus.LiveConsensusService;
import io.aurigraph.v11.models.ConsensusState;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;

/**
 * Live Data REST API Resource for AV11-269
 *
 * Provides real-time blockchain data endpoints including:
 * - Live consensus state (HyperRAFT++)
 * - Performance metrics
 * - Health monitoring
 *
 * All endpoints follow reactive programming patterns using Smallrye Mutiny
 * for high-performance, non-blocking I/O operations.
 *
 * @path /api/v11/live
 */
@Path("/api/v11/live")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class LiveDataResource {

    private static final Logger LOG = Logger.getLogger(LiveDataResource.class);

    @Inject
    LiveConsensusService liveConsensusService;

    /**
     * Get live HyperRAFT++ consensus state
     *
     * Returns real-time consensus metrics including:
     * - Current leader and node state
     * - Epoch, round, and term numbers
     * - Cluster participation and quorum
     * - Performance metrics (latency, throughput)
     * - Health status and next election time
     *
     * @return Uni<ConsensusState> Live consensus state
     *
     * Example: GET /api/v11/live/consensus
     */
    @GET
    @Path("/consensus")
    public Uni<ConsensusState> getLiveConsensus() {
        LOG.debug("Fetching live consensus state");

        return liveConsensusService.getCurrentConsensusState()
            .onItem().invoke(state -> {
                LOG.debugf("Returning consensus state: Leader=%s, Term=%d, Health=%s",
                    state.currentLeader(), state.term(), state.consensusHealth());
            })
            .onFailure().invoke(throwable -> {
                LOG.error("Failed to fetch live consensus state", throwable);
            });
    }

    /**
     * Get consensus summary in human-readable format
     *
     * @return Uni<String> Human-readable consensus summary
     *
     * Example: GET /api/v11/live/consensus/summary
     */
    @GET
    @Path("/consensus/summary")
    @Produces(MediaType.TEXT_PLAIN)
    public Uni<String> getConsensusSummary() {
        LOG.debug("Fetching consensus summary");
        return liveConsensusService.getConsensusSummary();
    }

    /**
     * Check if consensus is at optimal performance
     *
     * @return Uni<OptimalPerformanceResponse> Performance check result
     *
     * Example: GET /api/v11/live/consensus/optimal
     */
    @GET
    @Path("/consensus/optimal")
    public Uni<OptimalPerformanceResponse> checkOptimalPerformance() {
        LOG.debug("Checking optimal consensus performance");

        return liveConsensusService.isOptimalPerformance()
            .flatMap(isOptimal -> {
                return liveConsensusService.getCurrentConsensusState()
                    .map(state -> new OptimalPerformanceResponse(
                        isOptimal,
                        state.performanceScore(),
                        state.consensusHealth(),
                        state.consensusLatency(),
                        state.throughput(),
                        isOptimal ? "Consensus operating at optimal performance" :
                                   "Consensus performance below optimal threshold",
                        System.currentTimeMillis()
                    ));
            });
    }

    /**
     * Get current epoch information
     *
     * @return Uni<EpochInfo> Current epoch and round data
     *
     * Example: GET /api/v11/live/consensus/epoch
     */
    @GET
    @Path("/consensus/epoch")
    public Uni<EpochInfo> getEpochInfo() {
        LOG.debug("Fetching epoch information");

        return liveConsensusService.getCurrentConsensusState()
            .map(state -> new EpochInfo(
                state.epoch(),
                state.round(),
                state.term(),
                state.commitIndex(),
                state.lastCommit(),
                System.currentTimeMillis()
            ));
    }

    /**
     * Get cluster participation metrics
     *
     * @return Uni<ClusterParticipation> Cluster participation data
     *
     * Example: GET /api/v11/live/consensus/cluster
     */
    @GET
    @Path("/consensus/cluster")
    public Uni<ClusterParticipation> getClusterParticipation() {
        LOG.debug("Fetching cluster participation metrics");

        return liveConsensusService.getCurrentConsensusState()
            .map(state -> new ClusterParticipation(
                state.participants(),
                state.totalNodes(),
                state.quorumSize(),
                state.currentLeader(),
                state.nodeState(),
                state.consensusHealth(),
                (double) state.participants() / state.totalNodes(),
                System.currentTimeMillis()
            ));
    }

    /**
     * Get performance metrics
     *
     * @return Uni<PerformanceMetrics> Consensus performance data
     *
     * Example: GET /api/v11/live/consensus/performance
     */
    @GET
    @Path("/consensus/performance")
    public Uni<PerformanceMetrics> getPerformanceMetrics() {
        LOG.debug("Fetching consensus performance metrics");

        return liveConsensusService.getCurrentConsensusState()
            .map(state -> new PerformanceMetrics(
                state.consensusLatency(),
                state.throughput(),
                state.performanceScore(),
                state.commitIndex(),
                state.lastCommit(),
                state.isOptimalPerformance(),
                System.currentTimeMillis()
            ));
    }

    /**
     * Health check endpoint for live data service
     *
     * @return Response Health status
     *
     * Example: GET /api/v11/live/health
     */
    @GET
    @Path("/health")
    public Response healthCheck() {
        LOG.debug("Health check requested for live data service");

        return Response.ok()
            .entity(new HealthResponse(
                "HEALTHY",
                "Live data service operational",
                "LiveDataResource",
                System.currentTimeMillis()
            ))
            .build();
    }

    // ==================== Response DTOs ====================

    /**
     * Optimal performance check response
     */
    public record OptimalPerformanceResponse(
        boolean isOptimal,
        double performanceScore,
        String consensusHealth,
        long latencyMs,
        double throughputBlocksPerSec,
        String message,
        long timestamp
    ) {}

    /**
     * Epoch information response
     */
    public record EpochInfo(
        long epoch,
        long round,
        long term,
        long commitIndex,
        java.time.Instant lastCommit,
        long timestamp
    ) {}

    /**
     * Cluster participation response
     */
    public record ClusterParticipation(
        int activeParticipants,
        int totalNodes,
        int quorumSize,
        String currentLeader,
        String nodeState,
        String consensusHealth,
        double participationRate,
        long timestamp
    ) {}

    /**
     * Performance metrics response
     */
    public record PerformanceMetrics(
        long consensusLatencyMs,
        double throughputBlocksPerSec,
        double performanceScore,
        long commitIndex,
        java.time.Instant lastCommit,
        boolean isOptimal,
        long timestamp
    ) {}

    /**
     * Health response
     */
    public record HealthResponse(
        String status,
        String message,
        String service,
        long timestamp
    ) {}
}
