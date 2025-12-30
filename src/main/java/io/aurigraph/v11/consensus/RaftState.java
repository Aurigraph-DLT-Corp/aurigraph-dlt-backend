package io.aurigraph.v11.consensus;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * HyperRAFT++ State Machine
 * Manages the state transitions for RAFT consensus
 *
 * Phase 1: Leader Election - State management
 *
 * State Transitions:
 * - FOLLOWER -> CANDIDATE (on election timeout)
 * - CANDIDATE -> LEADER (on election victory)
 * - CANDIDATE -> FOLLOWER (on higher term discovered)
 * - LEADER -> FOLLOWER (on higher term discovered)
 */
public class RaftState {

    /**
     * RAFT node states
     */
    public enum NodeState {
        FOLLOWER,   // Passive node receiving updates
        CANDIDATE,  // Node currently in election
        LEADER      // Active leader coordinating cluster
    }

    /**
     * Election configuration and timeouts
     */
    public static class ElectionConfig {
        // Base election timeout (milliseconds)
        public final int electionTimeoutMin;
        public final int electionTimeoutMax;

        // Heartbeat interval (milliseconds)
        public final int heartbeatInterval;

        // Election timeout jitter for split-brain prevention
        public final int jitterMs;

        public ElectionConfig(int electionTimeoutMin, int electionTimeoutMax,
                            int heartbeatInterval, int jitterMs) {
            this.electionTimeoutMin = electionTimeoutMin;
            this.electionTimeoutMax = electionTimeoutMax;
            this.heartbeatInterval = heartbeatInterval;
            this.jitterMs = jitterMs;
        }

        public static ElectionConfig defaultConfig() {
            return new ElectionConfig(
                150,  // min 150ms election timeout
                300,  // max 300ms election timeout
                50,   // 50ms heartbeat interval
                50    // 50ms jitter
            );
        }

        public static ElectionConfig highPerformanceConfig() {
            return new ElectionConfig(
                100,  // min 100ms election timeout (optimized)
                200,  // max 200ms election timeout (optimized)
                30,   // 30ms heartbeat interval (optimized)
                30    // 30ms jitter (optimized)
            );
        }
    }

    /**
     * State data holder
     */
    public static class StateData {
        // Current state
        private final AtomicReference<NodeState> currentState;

        // RAFT persistent state
        private final AtomicLong currentTerm;
        private final AtomicReference<String> votedFor;

        // RAFT volatile state
        private final AtomicLong commitIndex;
        private final AtomicLong lastApplied;

        // Leader volatile state
        private final AtomicReference<String> leaderId;

        // Election timing
        private final AtomicLong lastHeartbeatTime;
        private final AtomicLong electionStartTime;
        private final AtomicLong currentElectionTimeout;

        // Vote tracking
        private final AtomicLong votesReceived;

        public StateData() {
            this.currentState = new AtomicReference<>(NodeState.FOLLOWER);
            this.currentTerm = new AtomicLong(0);
            this.votedFor = new AtomicReference<>(null);
            this.commitIndex = new AtomicLong(0);
            this.lastApplied = new AtomicLong(0);
            this.leaderId = new AtomicReference<>(null);
            this.lastHeartbeatTime = new AtomicLong(System.currentTimeMillis());
            this.electionStartTime = new AtomicLong(0);
            this.currentElectionTimeout = new AtomicLong(0);
            this.votesReceived = new AtomicLong(0);
        }

        // Getters
        public NodeState getCurrentState() {
            return currentState.get();
        }

        public long getCurrentTerm() {
            return currentTerm.get();
        }

        public String getVotedFor() {
            return votedFor.get();
        }

        public long getCommitIndex() {
            return commitIndex.get();
        }

        public long getLastApplied() {
            return lastApplied.get();
        }

        public String getLeaderId() {
            return leaderId.get();
        }

        public long getLastHeartbeatTime() {
            return lastHeartbeatTime.get();
        }

        public long getElectionStartTime() {
            return electionStartTime.get();
        }

        public long getCurrentElectionTimeout() {
            return currentElectionTimeout.get();
        }

        public long getVotesReceived() {
            return votesReceived.get();
        }

        // State transition methods
        public boolean transitionTo(NodeState newState) {
            NodeState current = currentState.get();
            if (isValidTransition(current, newState)) {
                currentState.set(newState);
                return true;
            }
            return false;
        }

        public void incrementTerm() {
            currentTerm.incrementAndGet();
        }

        public boolean setTermIfHigher(long term) {
            long current = currentTerm.get();
            if (term > current) {
                currentTerm.set(term);
                return true;
            }
            return false;
        }

        public void setVotedFor(String nodeId) {
            votedFor.set(nodeId);
        }

        public void clearVote() {
            votedFor.set(null);
        }

        public void setLeaderId(String nodeId) {
            leaderId.set(nodeId);
        }

        public void updateHeartbeatTime() {
            lastHeartbeatTime.set(System.currentTimeMillis());
        }

        public void startElection(long timeout) {
            electionStartTime.set(System.currentTimeMillis());
            currentElectionTimeout.set(timeout);
            votesReceived.set(0);
        }

        public void incrementVotes() {
            votesReceived.incrementAndGet();
        }

        public void setCommitIndex(long index) {
            commitIndex.set(index);
        }

        public void setLastApplied(long index) {
            lastApplied.set(index);
        }

        public boolean incrementCommitIndex() {
            long current = commitIndex.get();
            commitIndex.set(current + 1);
            return true;
        }

        public boolean incrementLastApplied() {
            long current = lastApplied.get();
            lastApplied.set(current + 1);
            return true;
        }

        /**
         * Validate state transitions according to RAFT rules
         */
        private boolean isValidTransition(NodeState from, NodeState to) {
            if (from == to) return true;

            switch (from) {
                case FOLLOWER:
                    return to == NodeState.CANDIDATE;

                case CANDIDATE:
                    return to == NodeState.LEADER || to == NodeState.FOLLOWER;

                case LEADER:
                    return to == NodeState.FOLLOWER;

                default:
                    return false;
            }
        }
    }

    /**
     * Election timeout checker
     */
    public static class ElectionTimeoutChecker {
        private final ElectionConfig config;

        public ElectionTimeoutChecker(ElectionConfig config) {
            this.config = config;
        }

        /**
         * Check if election timeout has occurred
         */
        public boolean hasElectionTimedOut(StateData state) {
            long now = System.currentTimeMillis();
            long lastHeartbeat = state.getLastHeartbeatTime();
            long timeout = state.getCurrentElectionTimeout();

            // If no timeout set, use default
            if (timeout == 0) {
                timeout = generateElectionTimeout();
            }

            return (now - lastHeartbeat) > timeout;
        }

        /**
         * Generate randomized election timeout for split-brain prevention
         */
        public long generateElectionTimeout() {
            int range = config.electionTimeoutMax - config.electionTimeoutMin;
            int jitter = (int) (Math.random() * config.jitterMs);
            return config.electionTimeoutMin + (int)(Math.random() * range) + jitter;
        }

        /**
         * Check if heartbeat should be sent
         */
        public boolean shouldSendHeartbeat(long lastHeartbeatTime) {
            long now = System.currentTimeMillis();
            return (now - lastHeartbeatTime) >= config.heartbeatInterval;
        }

        /**
         * Get heartbeat interval
         */
        public int getHeartbeatInterval() {
            return config.heartbeatInterval;
        }
    }

    /**
     * State snapshot for monitoring/debugging
     */
    public static class StateSnapshot {
        public final NodeState state;
        public final long term;
        public final String votedFor;
        public final String leaderId;
        public final long commitIndex;
        public final long lastApplied;
        public final long votesReceived;
        public final long lastHeartbeat;
        public final Instant timestamp;

        public StateSnapshot(StateData data) {
            this.state = data.getCurrentState();
            this.term = data.getCurrentTerm();
            this.votedFor = data.getVotedFor();
            this.leaderId = data.getLeaderId();
            this.commitIndex = data.getCommitIndex();
            this.lastApplied = data.getLastApplied();
            this.votesReceived = data.getVotesReceived();
            this.lastHeartbeat = data.getLastHeartbeatTime();
            this.timestamp = Instant.now();
        }

        @Override
        public String toString() {
            return String.format(
                "StateSnapshot[state=%s, term=%d, leader=%s, commitIndex=%d, lastApplied=%d, votes=%d]",
                state, term, leaderId, commitIndex, lastApplied, votesReceived
            );
        }
    }
}
