package io.aurigraph.v11.consensus;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.jboss.logging.Logger;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * HyperRAFT++ Consensus Engine
 *
 * Phase 3: Consensus Integration
 * - State machine integration
 * - Transaction commit protocol
 * - Membership changes (add/remove nodes)
 * - Configuration changes
 * - Snapshot creation/restoration
 *
 * Performance Target: 2M+ TPS with <100ms consensus latency
 */
@ApplicationScoped
public class ConsensusEngine {

    private static final Logger LOG = Logger.getLogger(ConsensusEngine.class);

    // Node configuration
    private String nodeId;
    private RaftState.StateData state;
    private RaftState.ElectionConfig config;

    // Core components
    private LeaderElection.ElectionCoordinator electionCoordinator;
    private LogReplication.LogManager logManager;

    // Cluster management
    private final Set<String> clusterNodes = ConcurrentHashMap.newKeySet();

    // Background threads
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(3,
            Thread.ofVirtual().name("consensus-", 0).factory());
    private final AtomicBoolean running = new AtomicBoolean(false);

    // Configuration
    @ConfigProperty(name = "consensus.node.id", defaultValue = "")
    String configNodeId;

    @ConfigProperty(name = "consensus.cluster.size", defaultValue = "7")
    int clusterSize;

    @ConfigProperty(name = "consensus.high.performance", defaultValue = "true")
    boolean highPerformance;

    @ConfigProperty(name = "consensus.heartbeat.interval.ms", defaultValue = "50")
    int heartbeatIntervalMs;

    @PostConstruct
    public void initialize() {
        // Generate node ID if not configured
        nodeId = configNodeId.isEmpty() ?
                "node-" + UUID.randomUUID().toString().substring(0, 8) : configNodeId;

        // Initialize state
        state = new RaftState.StateData();

        // Choose configuration based on performance mode
        config = highPerformance ?
                RaftState.ElectionConfig.highPerformanceConfig() :
                RaftState.ElectionConfig.defaultConfig();

        // Initialize cluster nodes (simulate for now)
        for (int i = 0; i < clusterSize - 1; i++) {
            clusterNodes.add("node-" + i);
        }

        // Initialize election coordinator
        electionCoordinator = new LeaderElection.ElectionCoordinator(
                nodeId, state, config, clusterNodes);

        // Initialize log manager
        logManager = new LogReplication.LogManager(nodeId, state);

        // Initialize follower indices if leader
        if (state.getCurrentState() == RaftState.NodeState.LEADER) {
            for (String node : clusterNodes) {
                logManager.initializeFollowerIndices(node);
            }
        }

        LOG.infof("Consensus Engine initialized: nodeId=%s, cluster=%d nodes, mode=%s",
                nodeId, clusterSize, highPerformance ? "high-performance" : "standard");

        // Start background tasks
        startBackgroundTasks();
        running.set(true);
    }

    @PreDestroy
    public void shutdown() {
        running.set(false);
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        LOG.info("Consensus Engine shut down");
    }

    /**
     * Propose a value to the consensus cluster
     * This is the main entry point for transaction commits
     */
    public Uni<ProposeResult> propose(String command, byte[] data) {
        return Uni.createFrom().item(() -> {
            long startTime = System.nanoTime();

            // Only leader can propose
            if (state.getCurrentState() != RaftState.NodeState.LEADER) {
                String leaderId = state.getLeaderId();
                return new ProposeResult(false, "Not leader", leaderId,
                        (System.nanoTime() - startTime) / 1_000_000.0);
            }

            // Append to log
            List<String> commands = Collections.singletonList(command);
            boolean appended = logManager.appendEntriesAsLeader(commands)
                    .await().atMost(Duration.ofSeconds(1));

            if (!appended) {
                return new ProposeResult(false, "Failed to append entries", nodeId,
                        (System.nanoTime() - startTime) / 1_000_000.0);
            }

            // Replicate to followers (simplified - in production, would use gRPC)
            replicateToFollowers();

            long latency = (System.nanoTime() - startTime) / 1_000_000;
            LOG.debugf("Proposed command: %s (latency: %dms)", command, latency);

            return new ProposeResult(true, "Committed", nodeId, latency);
        });
    }

    /**
     * Add node to cluster (configuration change)
     */
    public Uni<Boolean> addNode(String newNodeId) {
        return Uni.createFrom().item(() -> {
            if (state.getCurrentState() != RaftState.NodeState.LEADER) {
                LOG.warn("Cannot add node: not leader");
                return false;
            }

            clusterNodes.add(newNodeId);
            electionCoordinator.addNode(newNodeId);
            logManager.initializeFollowerIndices(newNodeId);

            LOG.infof("Added node %s to cluster (total: %d nodes)",
                    newNodeId, clusterNodes.size() + 1);

            return true;
        });
    }

    /**
     * Remove node from cluster (configuration change)
     */
    public Uni<Boolean> removeNode(String nodeToRemove) {
        return Uni.createFrom().item(() -> {
            if (state.getCurrentState() != RaftState.NodeState.LEADER) {
                LOG.warn("Cannot remove node: not leader");
                return false;
            }

            if (nodeToRemove.equals(nodeId)) {
                LOG.warn("Cannot remove self from cluster");
                return false;
            }

            clusterNodes.remove(nodeToRemove);
            electionCoordinator.removeNode(nodeToRemove);

            LOG.infof("Removed node %s from cluster (total: %d nodes)",
                    nodeToRemove, clusterNodes.size() + 1);

            return true;
        });
    }

    /**
     * Get current consensus status
     */
    public Uni<ConsensusStatus> getStatus() {
        return Uni.createFrom().item(() -> {
            RaftState.StateSnapshot snapshot = new RaftState.StateSnapshot(state);
            LeaderElection.ElectionMetrics electionMetrics = electionCoordinator.getMetrics();
            LogReplication.ReplicationMetrics replicationMetrics = logManager.getMetrics();

            return new ConsensusStatus(
                    snapshot,
                    electionMetrics,
                    replicationMetrics,
                    clusterNodes.size() + 1,
                    running.get()
            );
        });
    }

    /**
     * Force election (for testing/debugging)
     */
    public Uni<LeaderElection.ElectionResult> forceElection() {
        return electionCoordinator.startElection();
    }

    /**
     * Get node ID
     */
    public String getNodeId() {
        return nodeId;
    }

    /**
     * Get current state
     */
    public RaftState.NodeState getCurrentState() {
        return state.getCurrentState();
    }

    /**
     * Get current term
     */
    public long getCurrentTerm() {
        return state.getCurrentTerm();
    }

    /**
     * Get leader ID
     */
    public String getLeaderId() {
        return state.getLeaderId();
    }

    /**
     * Check if this node is leader
     */
    public boolean isLeader() {
        return state.getCurrentState() == RaftState.NodeState.LEADER;
    }

    // Private methods

    private void startBackgroundTasks() {
        // Election timeout checker
        scheduler.scheduleAtFixedRate(this::checkElectionTimeout,
                config.electionTimeoutMin, config.electionTimeoutMin / 2, TimeUnit.MILLISECONDS);

        // Heartbeat sender (for leader)
        scheduler.scheduleAtFixedRate(this::sendHeartbeatsIfLeader,
                config.heartbeatInterval, config.heartbeatInterval, TimeUnit.MILLISECONDS);

        // Metrics reporter
        scheduler.scheduleAtFixedRate(this::reportMetrics,
                5, 5, TimeUnit.SECONDS);

        LOG.info("Started consensus background tasks");
    }

    private void checkElectionTimeout() {
        if (!running.get()) return;

        try {
            // Only followers and candidates check election timeout
            if (state.getCurrentState() == RaftState.NodeState.LEADER) {
                return;
            }

            if (electionCoordinator.hasElectionTimedOut()) {
                LOG.info("Election timeout occurred, starting election...");
                electionCoordinator.startElection()
                        .subscribe().with(
                                result -> LOG.infof("Election result: %s", result.message),
                                failure -> LOG.error("Election failed", failure)
                        );
            }
        } catch (Exception e) {
            LOG.error("Error in election timeout check", e);
        }
    }

    private void sendHeartbeatsIfLeader() {
        if (!running.get()) return;

        try {
            if (electionCoordinator.shouldSendHeartbeat()) {
                electionCoordinator.sendHeartbeat()
                        .subscribe().with(
                                responses -> LOG.debugf("Sent heartbeats, received %d responses",
                                        responses.size()),
                                failure -> LOG.error("Heartbeat failed", failure)
                        );
            }
        } catch (Exception e) {
            LOG.error("Error sending heartbeats", e);
        }
    }

    private void replicateToFollowers() {
        // Simplified replication - in production, would send AppendEntries RPCs
        // to all followers and wait for quorum
        long lastIndex = logManager.getLastLogIndex();

        for (String node : clusterNodes) {
            // Simulate successful replication
            LogReplication.AppendEntriesResponse response =
                    LogReplication.AppendEntriesResponse.success(
                            state.getCurrentTerm(), lastIndex, node);

            logManager.processAppendEntriesResponse(response);
        }
    }

    private void reportMetrics() {
        if (!running.get()) return;

        try {
            LeaderElection.ElectionMetrics electionMetrics = electionCoordinator.getMetrics();
            LogReplication.ReplicationMetrics replicationMetrics = logManager.getMetrics();

            LOG.debugf("Consensus Metrics - State: %s, Term: %d, CommitIndex: %d, " +
                      "Elections: %d, WinRate: %.2f%%, LogSize: %d, CommitRate: %.2f%%",
                    state.getCurrentState(),
                    state.getCurrentTerm(),
                    state.getCommitIndex(),
                    electionMetrics.electionsStarted,
                    electionMetrics.getWinRate() * 100,
                    replicationMetrics.logSize,
                    replicationMetrics.getCommitRate() * 100);

        } catch (Exception e) {
            LOG.debug("Error reporting metrics: " + e.getMessage());
        }
    }

    /**
     * Propose result
     */
    public static class ProposeResult {
        public final boolean success;
        public final String message;
        public final String leaderId;
        public final double latencyMs;

        public ProposeResult(boolean success, String message, String leaderId, double latencyMs) {
            this.success = success;
            this.message = message;
            this.leaderId = leaderId;
            this.latencyMs = latencyMs;
        }
    }

    /**
     * Comprehensive consensus status
     */
    public static class ConsensusStatus {
        public final RaftState.StateSnapshot stateSnapshot;
        public final LeaderElection.ElectionMetrics electionMetrics;
        public final LogReplication.ReplicationMetrics replicationMetrics;
        public final int clusterSize;
        public final boolean running;

        public ConsensusStatus(RaftState.StateSnapshot stateSnapshot,
                             LeaderElection.ElectionMetrics electionMetrics,
                             LogReplication.ReplicationMetrics replicationMetrics,
                             int clusterSize,
                             boolean running) {
            this.stateSnapshot = stateSnapshot;
            this.electionMetrics = electionMetrics;
            this.replicationMetrics = replicationMetrics;
            this.clusterSize = clusterSize;
            this.running = running;
        }

        public String getHealthStatus() {
            if (!running) return "STOPPED";
            if (stateSnapshot.state == RaftState.NodeState.LEADER) return "HEALTHY (LEADER)";
            if (stateSnapshot.leaderId != null) return "HEALTHY (FOLLOWER)";
            if (stateSnapshot.state == RaftState.NodeState.CANDIDATE) return "ELECTING";
            return "UNHEALTHY (NO LEADER)";
        }
    }
}
