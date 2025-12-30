package io.aurigraph.v11.consensus;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * HyperRAFT++ Leader Election Implementation
 *
 * Phase 1: Leader Election
 * - Election timeout management
 * - Candidate state transitions
 * - Vote request/response handling
 * - Leader heartbeat mechanism
 * - Split-brain prevention
 *
 * Performance Target: <500ms election time, 99.9% availability
 */
@ApplicationScoped
public class LeaderElection {

    private static final Logger LOG = Logger.getLogger(LeaderElection.class);

    /**
     * Vote request message
     */
    public static class VoteRequest {
        public final long term;
        public final String candidateId;
        public final long lastLogIndex;
        public final long lastLogTerm;

        public VoteRequest(long term, String candidateId, long lastLogIndex, long lastLogTerm) {
            this.term = term;
            this.candidateId = candidateId;
            this.lastLogIndex = lastLogIndex;
            this.lastLogTerm = lastLogTerm;
        }
    }

    /**
     * Vote response message
     */
    public static class VoteResponse {
        public final long term;
        public final boolean voteGranted;
        public final String voterId;

        public VoteResponse(long term, boolean voteGranted, String voterId) {
            this.term = term;
            this.voteGranted = voteGranted;
            this.voterId = voterId;
        }
    }

    /**
     * Heartbeat message (AppendEntries with no entries)
     */
    public static class Heartbeat {
        public final long term;
        public final String leaderId;
        public final long prevLogIndex;
        public final long prevLogTerm;
        public final long leaderCommit;
        public final long timestamp;

        public Heartbeat(long term, String leaderId, long prevLogIndex,
                        long prevLogTerm, long leaderCommit) {
            this.term = term;
            this.leaderId = leaderId;
            this.prevLogIndex = prevLogIndex;
            this.prevLogTerm = prevLogTerm;
            this.leaderCommit = leaderCommit;
            this.timestamp = System.currentTimeMillis();
        }
    }

    /**
     * Heartbeat response
     */
    public static class HeartbeatResponse {
        public final long term;
        public final boolean success;
        public final String nodeId;
        public final long matchIndex;

        public HeartbeatResponse(long term, boolean success, String nodeId, long matchIndex) {
            this.term = term;
            this.success = success;
            this.nodeId = nodeId;
            this.matchIndex = matchIndex;
        }
    }

    /**
     * Election coordinator
     */
    public static class ElectionCoordinator {
        private final String nodeId;
        private final RaftState.StateData state;
        private final RaftState.ElectionTimeoutChecker timeoutChecker;
        private final Set<String> clusterNodes;

        // Vote tracking
        private final Map<Long, Set<String>> votesPerTerm = new ConcurrentHashMap<>();
        private final AtomicBoolean electionInProgress = new AtomicBoolean(false);

        // Election metrics
        private final AtomicLong electionsStarted = new AtomicLong(0);
        private final AtomicLong electionsWon = new AtomicLong(0);
        private final AtomicLong electionsLost = new AtomicLong(0);
        private final AtomicLong totalElectionTimeMs = new AtomicLong(0);

        public ElectionCoordinator(String nodeId, RaftState.StateData state,
                                  RaftState.ElectionConfig config,
                                  Set<String> clusterNodes) {
            this.nodeId = nodeId;
            this.state = state;
            this.timeoutChecker = new RaftState.ElectionTimeoutChecker(config);
            this.clusterNodes = new HashSet<>(clusterNodes);
        }

        /**
         * Start leader election
         * Transition to CANDIDATE and request votes from all nodes
         */
        public Uni<ElectionResult> startElection() {
            return Uni.createFrom().item(() -> {
                if (!electionInProgress.compareAndSet(false, true)) {
                    return new ElectionResult(false, "Election already in progress", state.getCurrentTerm());
                }

                long startTime = System.currentTimeMillis();
                electionsStarted.incrementAndGet();

                try {
                    // Transition to CANDIDATE
                    if (!state.transitionTo(RaftState.NodeState.CANDIDATE)) {
                        return new ElectionResult(false, "Failed to transition to CANDIDATE", state.getCurrentTerm());
                    }

                    // Increment current term
                    state.incrementTerm();
                    long currentTerm = state.getCurrentTerm();

                    // Vote for self
                    state.setVotedFor(nodeId);
                    state.incrementVotes();

                    // Initialize vote tracking for this term
                    Set<String> votes = ConcurrentHashMap.newKeySet();
                    votes.add(nodeId);
                    votesPerTerm.put(currentTerm, votes);

                    // Start election timeout
                    long timeout = timeoutChecker.generateElectionTimeout();
                    state.startElection(timeout);

                    LOG.infof("Node %s started election for term %d (timeout: %dms)",
                            nodeId, currentTerm, timeout);

                    // Request votes from all other nodes
                    VoteRequest voteRequest = createVoteRequest();
                    List<VoteResponse> responses = requestVotesFromCluster(voteRequest);

                    // Count votes
                    long votesReceived = processVoteResponses(responses, currentTerm);
                    int quorum = calculateQuorum();

                    long electionTime = System.currentTimeMillis() - startTime;
                    totalElectionTimeMs.addAndGet(electionTime);

                    // Check if won election
                    if (votesReceived >= quorum) {
                        electionsWon.incrementAndGet();
                        becomeLeader();
                        LOG.infof("Node %s won election for term %d with %d votes (quorum: %d, time: %dms)",
                                nodeId, currentTerm, votesReceived, quorum, electionTime);
                        return new ElectionResult(true, "Election won", currentTerm);
                    } else {
                        electionsLost.incrementAndGet();
                        becomeFollower();
                        LOG.infof("Node %s lost election for term %d with %d votes (quorum: %d, time: %dms)",
                                nodeId, currentTerm, votesReceived, quorum, electionTime);
                        return new ElectionResult(false, "Insufficient votes", currentTerm);
                    }

                } finally {
                    electionInProgress.set(false);
                }
            });
        }

        /**
         * Handle incoming vote request
         */
        public Uni<VoteResponse> handleVoteRequest(VoteRequest request) {
            return Uni.createFrom().item(() -> {
                long currentTerm = state.getCurrentTerm();

                // Reply false if term < currentTerm
                if (request.term < currentTerm) {
                    LOG.debugf("Rejecting vote for %s (stale term: %d < %d)",
                            request.candidateId, request.term, currentTerm);
                    return new VoteResponse(currentTerm, false, nodeId);
                }

                // Update term if request has higher term
                if (request.term > currentTerm) {
                    state.setTermIfHigher(request.term);
                    state.clearVote();
                    becomeFollower();
                }

                // Check if we've already voted in this term
                String votedFor = state.getVotedFor();
                if (votedFor != null && !votedFor.equals(request.candidateId)) {
                    LOG.debugf("Rejecting vote for %s (already voted for %s in term %d)",
                            request.candidateId, votedFor, request.term);
                    return new VoteResponse(currentTerm, false, nodeId);
                }

                // Grant vote if candidate's log is at least as up-to-date as ours
                boolean logUpToDate = isLogUpToDate(request.lastLogIndex, request.lastLogTerm);
                if (logUpToDate) {
                    state.setVotedFor(request.candidateId);
                    state.updateHeartbeatTime(); // Reset election timeout
                    LOG.infof("Granted vote to %s for term %d", request.candidateId, request.term);
                    return new VoteResponse(request.term, true, nodeId);
                } else {
                    LOG.debugf("Rejecting vote for %s (log not up-to-date)", request.candidateId);
                    return new VoteResponse(currentTerm, false, nodeId);
                }
            });
        }

        /**
         * Send heartbeat as leader
         */
        public Uni<List<HeartbeatResponse>> sendHeartbeat() {
            return Uni.createFrom().item(() -> {
                if (state.getCurrentState() != RaftState.NodeState.LEADER) {
                    return Collections.emptyList();
                }

                Heartbeat heartbeat = new Heartbeat(
                    state.getCurrentTerm(),
                    nodeId,
                    0, // prevLogIndex (simplified for now)
                    0, // prevLogTerm (simplified for now)
                    state.getCommitIndex()
                );

                List<HeartbeatResponse> responses = sendHeartbeatToCluster(heartbeat);
                state.updateHeartbeatTime();

                LOG.debugf("Sent heartbeat for term %d to %d nodes, received %d responses",
                        state.getCurrentTerm(), clusterNodes.size(), responses.size());

                return responses;
            });
        }

        /**
         * Handle incoming heartbeat
         */
        public Uni<HeartbeatResponse> handleHeartbeat(Heartbeat heartbeat) {
            return Uni.createFrom().item(() -> {
                long currentTerm = state.getCurrentTerm();

                // Reply false if term < currentTerm
                if (heartbeat.term < currentTerm) {
                    return new HeartbeatResponse(currentTerm, false, nodeId, 0);
                }

                // Update term and become follower if heartbeat has higher term
                if (heartbeat.term > currentTerm) {
                    state.setTermIfHigher(heartbeat.term);
                    becomeFollower();
                }

                // Reset election timeout
                state.updateHeartbeatTime();
                state.setLeaderId(heartbeat.leaderId);

                // Update commit index if leader's commit index is higher
                if (heartbeat.leaderCommit > state.getCommitIndex()) {
                    long newCommitIndex = Math.min(heartbeat.leaderCommit, heartbeat.prevLogIndex);
                    state.setCommitIndex(newCommitIndex);
                }

                LOG.debugf("Received heartbeat from leader %s for term %d",
                        heartbeat.leaderId, heartbeat.term);

                return new HeartbeatResponse(heartbeat.term, true, nodeId, state.getCommitIndex());
            });
        }

        /**
         * Check if election timeout has occurred
         */
        public boolean hasElectionTimedOut() {
            // Only check timeout if we're a follower or candidate
            RaftState.NodeState currentState = state.getCurrentState();
            if (currentState == RaftState.NodeState.LEADER) {
                return false;
            }

            return timeoutChecker.hasElectionTimedOut(state);
        }

        /**
         * Check if heartbeat should be sent
         */
        public boolean shouldSendHeartbeat() {
            return state.getCurrentState() == RaftState.NodeState.LEADER &&
                   timeoutChecker.shouldSendHeartbeat(state.getLastHeartbeatTime());
        }

        /**
         * Add node to cluster
         */
        public void addNode(String nodeId) {
            clusterNodes.add(nodeId);
            LOG.infof("Added node %s to cluster (total: %d)", nodeId, clusterNodes.size());
        }

        /**
         * Remove node from cluster
         */
        public void removeNode(String nodeId) {
            clusterNodes.remove(nodeId);
            LOG.infof("Removed node %s from cluster (total: %d)", nodeId, clusterNodes.size());
        }

        /**
         * Get election metrics
         */
        public ElectionMetrics getMetrics() {
            long totalElections = electionsStarted.get();
            double avgElectionTime = totalElections > 0 ?
                (double) totalElectionTimeMs.get() / totalElections : 0.0;

            return new ElectionMetrics(
                electionsStarted.get(),
                electionsWon.get(),
                electionsLost.get(),
                avgElectionTime,
                clusterNodes.size() + 1 // Include self
            );
        }

        // Private helper methods

        private VoteRequest createVoteRequest() {
            return new VoteRequest(
                state.getCurrentTerm(),
                nodeId,
                0, // lastLogIndex (simplified for now)
                0  // lastLogTerm (simplified for now)
            );
        }

        private List<VoteResponse> requestVotesFromCluster(VoteRequest request) {
            // Simplified: simulate vote requests to cluster
            // In production, this would use gRPC/network communication
            List<VoteResponse> responses = new ArrayList<>();

            for (String node : clusterNodes) {
                // Simulate vote response with 70% approval rate
                boolean granted = Math.random() > 0.3;
                responses.add(new VoteResponse(request.term, granted, node));
            }

            return responses;
        }

        private long processVoteResponses(List<VoteResponse> responses, long currentTerm) {
            Set<String> votes = votesPerTerm.get(currentTerm);
            if (votes == null) {
                votes = ConcurrentHashMap.newKeySet();
                votesPerTerm.put(currentTerm, votes);
            }

            for (VoteResponse response : responses) {
                if (response.voteGranted && response.term == currentTerm) {
                    votes.add(response.voterId);
                    state.incrementVotes();
                }

                // Step down if we discover a higher term
                if (response.term > currentTerm) {
                    state.setTermIfHigher(response.term);
                    becomeFollower();
                }
            }

            return state.getVotesReceived();
        }

        private List<HeartbeatResponse> sendHeartbeatToCluster(Heartbeat heartbeat) {
            // Simplified: simulate heartbeat to cluster
            // In production, this would use gRPC/network communication
            List<HeartbeatResponse> responses = new ArrayList<>();

            for (String node : clusterNodes) {
                // Simulate heartbeat response
                responses.add(new HeartbeatResponse(heartbeat.term, true, node, heartbeat.leaderCommit));
            }

            return responses;
        }

        private int calculateQuorum() {
            int totalNodes = clusterNodes.size() + 1; // Include self
            return (totalNodes / 2) + 1; // Majority
        }

        private boolean isLogUpToDate(long lastLogIndex, long lastLogTerm) {
            // Simplified: always return true for now
            // In production, compare with our log's last index/term
            return true;
        }

        private void becomeLeader() {
            state.transitionTo(RaftState.NodeState.LEADER);
            state.setLeaderId(nodeId);
            state.updateHeartbeatTime();
            LOG.infof("Node %s became LEADER for term %d", nodeId, state.getCurrentTerm());
        }

        private void becomeFollower() {
            state.transitionTo(RaftState.NodeState.FOLLOWER);
            state.clearVote();
            state.updateHeartbeatTime();
            LOG.debugf("Node %s became FOLLOWER for term %d", nodeId, state.getCurrentTerm());
        }
    }

    /**
     * Election result
     */
    public static class ElectionResult {
        public final boolean won;
        public final String message;
        public final long term;

        public ElectionResult(boolean won, String message, long term) {
            this.won = won;
            this.message = message;
            this.term = term;
        }
    }

    /**
     * Election metrics
     */
    public static class ElectionMetrics {
        public final long electionsStarted;
        public final long electionsWon;
        public final long electionsLost;
        public final double avgElectionTimeMs;
        public final int clusterSize;

        public ElectionMetrics(long electionsStarted, long electionsWon, long electionsLost,
                             double avgElectionTimeMs, int clusterSize) {
            this.electionsStarted = electionsStarted;
            this.electionsWon = electionsWon;
            this.electionsLost = electionsLost;
            this.avgElectionTimeMs = avgElectionTimeMs;
            this.clusterSize = clusterSize;
        }

        public double getWinRate() {
            return electionsStarted > 0 ?
                (double) electionsWon / electionsStarted : 0.0;
        }
    }
}
