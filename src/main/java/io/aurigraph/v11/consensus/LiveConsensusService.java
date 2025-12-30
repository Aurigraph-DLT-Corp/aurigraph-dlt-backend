package io.aurigraph.v11.consensus;

import io.aurigraph.v11.models.ConsensusState;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.annotation.PostConstruct;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Live Consensus Service for AV11-269
 *
 * Provides real-time consensus state data from HyperRAFT++ consensus algorithm.
 * Integrates with HyperRAFTConsensusService to generate live consensus metrics
 * including leader information, epoch/round/term tracking, and performance data.
 *
 * This service simulates realistic consensus behavior with leader rotation,
 * dynamic performance metrics, and health monitoring.
 */
@ApplicationScoped
public class LiveConsensusService {

    private static final Logger LOG = Logger.getLogger(LiveConsensusService.class);

    @Inject
    HyperRAFTConsensusService consensusService;

    // Live consensus state tracking
    private final AtomicLong currentEpoch = new AtomicLong(1247);
    private final AtomicLong currentRound = new AtomicLong(89234);
    private final Random random = new Random();

    // Election timing
    private static final long BASE_ELECTION_TIMEOUT_MS = 5000; // 5 seconds base
    private static final long ELECTION_VARIANCE_MS = 2000;     // ±2 seconds variance

    @PostConstruct
    public void initialize() {
        LOG.info("Initializing Live Consensus Service for HyperRAFT++");
        startLiveMetricsUpdates();
    }

    /**
     * Get current live consensus state
     * Returns real-time HyperRAFT++ consensus metrics
     */
    public Uni<ConsensusState> getCurrentConsensusState() {
        return consensusService.getStats()
            .map(stats -> {
                // Get base data from consensus service
                String nodeId = stats.nodeId;
                String currentLeader = stats.leaderId;
                String nodeState = stats.state.name();
                long term = stats.currentTerm;
                long commitIndex = stats.commitIndex;
                long consensusLatency = stats.consensusLatency;
                long throughput = stats.throughput;
                int clusterSize = stats.clusterSize + 1; // +1 for leader

                // Calculate dynamic epoch and round
                long epoch = currentEpoch.get();
                long round = currentRound.get();

                // Calculate quorum size
                int quorumSize = ConsensusState.calculateQuorumSize(clusterSize);

                // Determine participants (simulate some nodes being active)
                int participants = clusterSize;
                if (random.nextDouble() < 0.05) { // 5% chance of degraded state
                    participants = Math.max(quorumSize, clusterSize - 1);
                }

                // Calculate last commit time
                Instant lastCommit = Instant.now().minus(consensusLatency, ChronoUnit.MILLIS);

                // Calculate next leader election time
                long electionTimeoutMs = BASE_ELECTION_TIMEOUT_MS +
                    (random.nextLong(ELECTION_VARIANCE_MS * 2) - ELECTION_VARIANCE_MS);
                Instant nextElection = Instant.now().plus(electionTimeoutMs, ChronoUnit.MILLIS);

                // Determine health status
                String consensusHealth = ConsensusState.determineHealth(
                    nodeState, participants, clusterSize, consensusLatency
                );
                boolean isHealthy = consensusHealth.equals("HEALTHY");

                // Calculate performance score
                double performanceScore = ConsensusState.calculatePerformanceScore(
                    consensusLatency, throughput, participants, clusterSize
                );

                // Convert throughput from TPS to blocks/sec (assume ~100 tx per block)
                double blocksPerSec = throughput / 100.0;

                ConsensusState state = ConsensusState.create(
                    currentLeader,
                    nodeId,
                    nodeState,
                    epoch,
                    round,
                    term,
                    participants,
                    quorumSize,
                    clusterSize,
                    lastCommit,
                    consensusLatency,
                    blocksPerSec,
                    commitIndex,
                    nextElection,
                    electionTimeoutMs,
                    consensusHealth,
                    isHealthy,
                    performanceScore
                );

                LOG.debugf("Generated live consensus state: %s", state.getSummary());
                return state;
            });
    }

    /**
     * Start background thread for updating live metrics
     * Updates epoch and round numbers to simulate consensus progression
     */
    private void startLiveMetricsUpdates() {
        Thread updateThread = new Thread(() -> {
            LOG.info("Started live consensus metrics update thread");

            while (true) {
                try {
                    updateLiveMetrics();
                    Thread.sleep(1000); // Update every second
                } catch (InterruptedException e) {
                    LOG.error("Live metrics update thread interrupted", e);
                    break;
                } catch (Exception e) {
                    LOG.error("Error updating live metrics", e);
                }
            }
        });

        updateThread.setDaemon(true);
        updateThread.setName("live-consensus-metrics-updater");
        updateThread.start();
    }

    /**
     * Update live metrics to simulate consensus progression
     */
    private void updateLiveMetrics() {
        // Increment round (happens frequently)
        long round = currentRound.incrementAndGet();

        // Increment epoch every ~100 rounds (simulate epoch transitions)
        if (round % 100 == 0) {
            long newEpoch = currentEpoch.incrementAndGet();
            LOG.debugf("Epoch transition: %d (round: %d)", newEpoch, round);
        }

        // Occasionally simulate leader election (rare, ~1% chance per update)
        if (random.nextDouble() < 0.01) {
            LOG.info("Simulating leader election event");
            // The HyperRAFTConsensusService will handle term increment
        }
    }

    /**
     * Get consensus statistics summary
     */
    public Uni<String> getConsensusSummary() {
        return getCurrentConsensusState()
            .map(ConsensusState::getSummary);
    }

    /**
     * Check if consensus is at optimal performance
     */
    public Uni<Boolean> isOptimalPerformance() {
        return getCurrentConsensusState()
            .map(ConsensusState::isOptimalPerformance);
    }

    /**
     * Get current epoch number
     */
    public long getCurrentEpoch() {
        return currentEpoch.get();
    }

    /**
     * Get current round number
     */
    public long getCurrentRound() {
        return currentRound.get();
    }
}
