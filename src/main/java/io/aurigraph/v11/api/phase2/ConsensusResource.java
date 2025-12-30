package io.aurigraph.v11.api.phase2;

import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.util.*;

/**
 * Sprint 12: Consensus Monitoring REST API (21 pts)
 *
 * Endpoints for HyperRAFT++ consensus status, leader election history,
 * and performance metrics.
 * Extracted from Phase2BlockchainResource for better maintainability.
 *
 * @author Backend Development Agent (BDA)
 * @version 11.0.0
 * @since Sprint 12
 */
@Path("/api/v11/blockchain")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ConsensusResource {

    private static final Logger LOG = Logger.getLogger(ConsensusResource.class);

    /**
     * Get HyperRAFT++ consensus status
     * GET /api/v11/blockchain/consensus/status
     */
    @GET
    @Path("/consensus/status")
    public Uni<ConsensusStatus> getConsensusStatus() {
        LOG.info("Fetching consensus status");

        return Uni.createFrom().item(() -> {
            ConsensusStatus status = new ConsensusStatus();
            status.algorithm = "HyperRAFT++";
            status.currentLeader = "0xvalidator-01-address";
            status.currentTerm = 1542;
            status.currentRound = 125678;
            status.consensusLatency = 45.2;
            status.finalizationTime = 495;
            status.participatingValidators = 121;
            status.quorumSize = 81;
            status.consensusHealth = "HEALTHY";
            status.aiOptimizationActive = true;
            status.quantumResistant = true;
            return status;
        });
    }

    /**
     * Get leader election history
     * GET /api/v11/blockchain/consensus/leader-history
     */
    @GET
    @Path("/consensus/leader-history")
    public Uni<LeaderHistory> getLeaderHistory(@QueryParam("limit") @DefaultValue("50") int limit) {
        LOG.infof("Fetching leader election history (limit: %d)", limit);

        return Uni.createFrom().item(() -> {
            LeaderHistory history = new LeaderHistory();
            history.elections = new ArrayList<>();

            for (int i = 0; i < Math.min(limit, 10); i++) {
                LeaderElection election = new LeaderElection();
                election.term = 1542 - i;
                election.leader = "0xvalidator-0" + (i % 5 + 1) + "-address";
                election.electedAt = Instant.now().minusSeconds(i * 300).toString();
                election.votesReceived = 85 + (i % 10);
                election.totalVoters = 121;
                election.electionDuration = 150 + (i * 10);
                history.elections.add(election);
            }

            history.totalElections = history.elections.size();
            return history;
        });
    }

    /**
     * Get consensus performance metrics
     * GET /api/v11/blockchain/consensus/metrics
     */
    @GET
    @Path("/consensus/metrics")
    public Uni<ConsensusMetrics> getConsensusMetrics() {
        LOG.info("Fetching consensus performance metrics");

        return Uni.createFrom().item(() -> {
            ConsensusMetrics metrics = new ConsensusMetrics();
            metrics.averageConsensusLatency = 45.2;
            metrics.averageFinalizationTime = 495.0;
            metrics.successRate = 99.98;
            metrics.forkCount = 0;
            metrics.missedRounds = 12;
            metrics.totalRounds = 125678;
            metrics.averageParticipation = 98.5;
            metrics.aiOptimizationGain = 23.5;
            return metrics;
        });
    }

    // ==================== DTOs ====================

    public static class ConsensusStatus {
        public String algorithm;
        public String currentLeader;
        public long currentTerm;
        public long currentRound;
        public double consensusLatency;
        public int finalizationTime;
        public int participatingValidators;
        public int quorumSize;
        public String consensusHealth;
        public boolean aiOptimizationActive;
        public boolean quantumResistant;
    }

    public static class LeaderHistory {
        public List<LeaderElection> elections;
        public int totalElections;
    }

    public static class LeaderElection {
        public long term;
        public String leader;
        public String electedAt;
        public int votesReceived;
        public int totalVoters;
        public int electionDuration;
    }

    public static class ConsensusMetrics {
        public double averageConsensusLatency;
        public double averageFinalizationTime;
        public double successRate;
        public int forkCount;
        public int missedRounds;
        public long totalRounds;
        public double averageParticipation;
        public double aiOptimizationGain;
    }
}
