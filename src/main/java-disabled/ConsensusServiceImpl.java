package io.aurigraph.v11.grpc.services;

// Explicit imports from proto-generated consensus package
import io.aurigraph.v11.grpc.consensus.AppendEntriesRequest;
import io.aurigraph.v11.grpc.consensus.AppendEntriesResponse;
import io.aurigraph.v11.grpc.consensus.BlockProposal;
import io.aurigraph.v11.grpc.consensus.ConsensusEvent;
import io.aurigraph.v11.grpc.consensus.ConsensusService;
import io.aurigraph.v11.grpc.consensus.ConsensusState;
import io.aurigraph.v11.grpc.consensus.EventStreamRequest;
import io.aurigraph.v11.grpc.consensus.EventType;
import io.aurigraph.v11.grpc.consensus.LogEntry;
import io.aurigraph.v11.grpc.consensus.NodeRole;
import io.aurigraph.v11.grpc.consensus.PeerState;
import io.aurigraph.v11.grpc.consensus.ProposalResponse;
import io.aurigraph.v11.grpc.consensus.SnapshotRequest;
import io.aurigraph.v11.grpc.consensus.SnapshotResponse;
import io.aurigraph.v11.grpc.consensus.StateRequest;
import io.aurigraph.v11.grpc.consensus.VoteRequest;
import io.aurigraph.v11.grpc.consensus.VoteResponse;

import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.Multi;
import jakarta.inject.Singleton;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * HyperRAFT++ Consensus Service Implementation
 * Sprint 13 - Workstream 2: Consensus Migration
 *
 * Implements the HyperRAFT++ consensus algorithm with:
 * - Leader election
 * - Log replication
 * - Snapshot management
 * - AI-based optimization hooks
 */
@GrpcService
@Singleton
public class ConsensusServiceImpl implements ConsensusService {

    private static final Logger LOG = Logger.getLogger(ConsensusServiceImpl.class);

    // Consensus state
    private final AtomicLong currentTerm = new AtomicLong(0);
    private final AtomicReference<String> votedFor = new AtomicReference<>(null);
    private final AtomicReference<NodeRole> currentRole = new AtomicReference<>(NodeRole.FOLLOWER);
    private final AtomicReference<String> currentLeader = new AtomicReference<>(null);
    private final AtomicLong commitIndex = new AtomicLong(0);
    private final AtomicLong lastApplied = new AtomicLong(0);

    // Log storage
    private final List<LogEntry> log = Collections.synchronizedList(new ArrayList<>());

    // Peer state tracking
    private final Map<String, PeerState> peerStates = new ConcurrentHashMap<>();

    // Node configuration
    private final String nodeId;
    private final int electionTimeoutMs = 5000;
    private long lastHeartbeat = System.currentTimeMillis();

    public ConsensusServiceImpl() {
        this.nodeId = "node-" + UUID.randomUUID().toString().substring(0, 8);
        LOG.infof("Consensus service initialized for node: %s", nodeId);
    }

    /**
     * Request Vote RPC - Leader Election
     * Sprint 13 - HyperRAFT++ implementation
     */
    @Override
    public Uni<VoteResponse> requestVote(VoteRequest request) {
        LOG.infof("Vote request from %s for term %d", request.getCandidateId(), request.getTerm());

        return Uni.createFrom().item(() -> {
            long term = currentTerm.get();
            String votedForCandidate = votedFor.get();

            // If request term is older, reject
            if (request.getTerm() < term) {
                return VoteResponse.newBuilder()
                    .setTerm(term)
                    .setVoteGranted(false)
                    .setVoterId(nodeId)
                    .build();
            }

            // Update term if newer
            if (request.getTerm() > term) {
                currentTerm.set(request.getTerm());
                votedFor.set(null);
                currentRole.set(NodeRole.FOLLOWER);
                term = request.getTerm();
                votedForCandidate = null;
            }

            // Grant vote if:
            // 1. Haven't voted yet or already voted for this candidate
            // 2. Candidate's log is at least as up-to-date as ours
            boolean canVote = (votedForCandidate == null ||
                              votedForCandidate.equals(request.getCandidateId()));
            boolean logUpToDate = isLogUpToDate(request.getLastLogIndex(), request.getLastLogTerm());

            boolean voteGranted = canVote && logUpToDate;

            if (voteGranted) {
                votedFor.set(request.getCandidateId());
                lastHeartbeat = System.currentTimeMillis();
                LOG.infof("Granted vote to %s for term %d", request.getCandidateId(), term);
            }

            return VoteResponse.newBuilder()
                .setTerm(term)
                .setVoteGranted(voteGranted)
                .setVoterId(nodeId)
                .build();
        }).runSubscriptionOn(runnable -> Thread.startVirtualThread(runnable));
    }

    /**
     * Append Entries RPC - Log Replication & Heartbeat
     * Sprint 13 - Core consensus mechanism
     */
    @Override
    public Uni<AppendEntriesResponse> appendEntries(AppendEntriesRequest request) {
        LOG.debugf("AppendEntries from %s, term %d, entries: %d",
            request.getLeaderId(), request.getTerm(), request.getEntriesCount());

        return Uni.createFrom().item(() -> {
            long term = currentTerm.get();

            // Reject if term is older
            if (request.getTerm() < term) {
                return AppendEntriesResponse.newBuilder()
                    .setTerm(term)
                    .setSuccess(false)
                    .setMatchIndex(0)
                    .setFollowerId(nodeId)
                    .build();
            }

            // Update term and become follower if term is newer
            if (request.getTerm() > term) {
                currentTerm.set(request.getTerm());
                currentRole.set(NodeRole.FOLLOWER);
                votedFor.set(null);
            }

            // Update leader and reset election timeout
            currentLeader.set(request.getLeaderId());
            lastHeartbeat = System.currentTimeMillis();

            // Heartbeat (no entries)
            if (request.getEntriesCount() == 0) {
                return AppendEntriesResponse.newBuilder()
                    .setTerm(currentTerm.get())
                    .setSuccess(true)
                    .setMatchIndex(log.size())
                    .setFollowerId(nodeId)
                    .build();
            }

            // Check log consistency
            if (!checkLogConsistency(request.getPrevLogIndex(), request.getPrevLogTerm())) {
                return AppendEntriesResponse.newBuilder()
                    .setTerm(currentTerm.get())
                    .setSuccess(false)
                    .setMatchIndex(log.size())
                    .setFollowerId(nodeId)
                    .build();
            }

            // Append new entries
            if (request.getBatchAppend()) {
                // Batch append for performance (Sprint 15 optimization)
                appendEntriesBatch(request.getEntriesList(), request.getPrevLogIndex());
            } else {
                appendEntriesSequential(request.getEntriesList(), request.getPrevLogIndex());
            }

            // Update commit index
            if (request.getLeaderCommit() > commitIndex.get()) {
                long newCommitIndex = Math.min(request.getLeaderCommit(), log.size());
                commitIndex.set(newCommitIndex);
                LOG.debugf("Updated commit index to %d", newCommitIndex);
            }

            return AppendEntriesResponse.newBuilder()
                .setTerm(currentTerm.get())
                .setSuccess(true)
                .setMatchIndex(log.size())
                .setFollowerId(nodeId)
                .build();
        }).runSubscriptionOn(runnable -> Thread.startVirtualThread(runnable));
    }

    /**
     * Install Snapshot RPC - Fast State Recovery
     * Sprint 13 - Snapshot mechanism
     */
    @Override
    public Uni<SnapshotResponse> installSnapshot(SnapshotRequest request) {
        LOG.infof("Installing snapshot from %s, last included: %d",
            request.getLeaderId(), request.getLastIncludedIndex());

        return Uni.createFrom().item(() -> {
            long term = currentTerm.get();

            // Reject if term is older
            if (request.getTerm() < term) {
                return SnapshotResponse.newBuilder()
                    .setTerm(term)
                    .setSuccess(false)
                    .build();
            }

            // Update term if newer
            if (request.getTerm() > term) {
                currentTerm.set(request.getTerm());
                currentRole.set(NodeRole.FOLLOWER);
                votedFor.set(null);
            }

            // Apply snapshot
            if (request.getDone()) {
                // Snapshot transfer complete
                applySnapshot(request.getSnapshotData(),
                            request.getLastIncludedIndex(),
                            request.getLastIncludedTerm());

                LOG.infof("Snapshot installed successfully, index: %d",
                    request.getLastIncludedIndex());
            }

            return SnapshotResponse.newBuilder()
                .setTerm(currentTerm.get())
                .setSuccess(true)
                .build();
        }).runSubscriptionOn(runnable -> Thread.startVirtualThread(runnable));
    }

    /**
     * Get current consensus state
     */
    @Override
    public Uni<ConsensusState> getConsensusState(StateRequest request) {
        return Uni.createFrom().item(() -> {
            List<PeerState> peers = new ArrayList<>(peerStates.values());

            return ConsensusState.newBuilder()
                .setNodeId(nodeId)
                .setRole(currentRole.get())
                .setCurrentTerm(currentTerm.get())
                .setVotedFor(votedFor.get() != null ? votedFor.get() : "")
                .setCommitIndex(commitIndex.get())
                .setLastApplied(lastApplied.get())
                .setCurrentLeader(currentLeader.get() != null ? currentLeader.get() : "")
                .setLogSize(log.size())
                .addAllPeers(peers)
                .build();
        });
    }

    /**
     * Stream consensus events
     * Sprint 13 - Real-time event streaming
     */
    @Override
    public Multi<ConsensusEvent> streamConsensusEvents(EventStreamRequest request) {
        LOG.info("Starting consensus event stream");

        return Multi.createFrom().ticks().every(java.time.Duration.ofSeconds(1))
            .onItem().transform(tick -> {
                return ConsensusEvent.newBuilder()
                    .setEventType(EventType.COMMIT_ADVANCED)
                    .setNodeId(nodeId)
                    .setTerm(currentTerm.get())
                    .setTimestamp(Instant.now().toEpochMilli())
                    .build();
            });
    }

    /**
     * Propose new block
     * Sprint 15 - Block proposal integration
     */
    @Override
    public Uni<ProposalResponse> proposeBlock(BlockProposal request) {
        LOG.infof("Block proposal %d from %s with %d transactions",
            request.getBlockNumber(), request.getProposerId(),
            request.getTransactionIdsCount());

        return Uni.createFrom().item(() -> {
            // Only leader can propose blocks
            if (currentRole.get() != NodeRole.LEADER) {
                return ProposalResponse.newBuilder()
                    .setAccepted(false)
                    .setBlockNumber(request.getBlockNumber())
                    .setMessage("Not the leader")
                    .build();
            }

            // Create log entry for block
            LogEntry entry = LogEntry.newBuilder()
                .setIndex(log.size() + 1)
                .setTerm(currentTerm.get())
                .setCommand(request.getBlockData())
                .setCommandType("BLOCK_PROPOSAL")
                .setTimestamp(Instant.now().toEpochMilli())
                .build();

            log.add(entry);

            return ProposalResponse.newBuilder()
                .setAccepted(true)
                .setBlockNumber(request.getBlockNumber())
                .setMessage("Block proposal accepted")
                .build();
        }).runSubscriptionOn(runnable -> Thread.startVirtualThread(runnable));
    }

    // Helper methods

    private boolean isLogUpToDate(long lastLogIndex, long lastLogTerm) {
        if (log.isEmpty()) {
            return true;
        }

        LogEntry lastEntry = log.get(log.size() - 1);
        if (lastLogTerm > lastEntry.getTerm()) {
            return true;
        }
        if (lastLogTerm == lastEntry.getTerm() && lastLogIndex >= lastEntry.getIndex()) {
            return true;
        }
        return false;
    }

    private boolean checkLogConsistency(long prevLogIndex, long prevLogTerm) {
        if (prevLogIndex == 0) {
            return true;
        }

        if (prevLogIndex > log.size()) {
            return false;
        }

        LogEntry entry = log.get((int) prevLogIndex - 1);
        return entry.getTerm() == prevLogTerm;
    }

    private void appendEntriesBatch(List<LogEntry> entries, long prevLogIndex) {
        // Sprint 15 - Optimized batch append
        int startIndex = (int) prevLogIndex;

        // Remove conflicting entries
        if (startIndex < log.size()) {
            log.subList(startIndex, log.size()).clear();
        }

        // Add new entries
        log.addAll(entries);
        LOG.debugf("Batch appended %d entries", entries.size());
    }

    private void appendEntriesSequential(List<LogEntry> entries, long prevLogIndex) {
        for (LogEntry entry : entries) {
            if (entry.getIndex() <= log.size()) {
                // Check for conflict
                LogEntry existingEntry = log.get((int) entry.getIndex() - 1);
                if (existingEntry.getTerm() != entry.getTerm()) {
                    // Remove conflicting entry and all that follow
                    log.subList((int) entry.getIndex() - 1, log.size()).clear();
                    log.add(entry);
                }
            } else {
                log.add(entry);
            }
        }
    }

    private void applySnapshot(com.google.protobuf.ByteString snapshotData,
                              long lastIncludedIndex,
                              long lastIncludedTerm) {
        // Remove log entries covered by snapshot
        if (lastIncludedIndex <= log.size()) {
            log.subList(0, (int) lastIncludedIndex).clear();
        }

        // Update state
        commitIndex.set(lastIncludedIndex);
        lastApplied.set(lastIncludedIndex);
    }

    /**
     * Start leader election
     * Sprint 13 - Election mechanism
     */
    public void startElection() {
        currentTerm.incrementAndGet();
        currentRole.set(NodeRole.CANDIDATE);
        votedFor.set(nodeId);

        LOG.infof("Starting election for term %d", currentTerm.get());

        // Request votes from all peers (would be implemented in full version)
        // For now, simulate becoming leader
        becomeLeader();
    }

    private void becomeLeader() {
        currentRole.set(NodeRole.LEADER);
        currentLeader.set(nodeId);
        LOG.infof("Became leader for term %d", currentTerm.get());

        // Initialize peer states
        initializePeerStates();
    }

    private void initializePeerStates() {
        // Initialize next/match indexes for all peers
        long nextIndex = log.size() + 1;

        peerStates.clear();
        // Would normally have actual peer list here
    }

    /**
     * Check if election timeout has expired
     */
    public boolean isElectionTimeoutExpired() {
        return System.currentTimeMillis() - lastHeartbeat > electionTimeoutMs;
    }

    public NodeRole getCurrentRole() {
        return currentRole.get();
    }

    public String getNodeId() {
        return nodeId;
    }
}
