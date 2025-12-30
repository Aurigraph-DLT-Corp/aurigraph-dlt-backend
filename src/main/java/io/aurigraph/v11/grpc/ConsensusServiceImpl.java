package io.aurigraph.v11.grpc;

import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.aurigraph.v11.proto.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import com.google.protobuf.Timestamp;

/**
 * ConsensusServiceImpl - HyperRAFT++ Distributed Consensus Service
 *
 * Implements 11 RPC methods for blockchain consensus with:
 * - Block proposal and voting mechanism
 * - Leader election with term-based voting
 * - Heartbeat and state synchronization
 * - Raft log replication and management
 * - Real-time consensus event streaming
 *
 * Target Performance: 1.1M-1.3M TPS (50-70% improvement from 776K baseline)
 * Protocol: gRPC with Protocol Buffers and HTTP/2 multiplexing
 */
@GrpcService
public class ConsensusServiceImpl implements io.aurigraph.v11.proto.ConsensusService {

    // Consensus state management
    private volatile io.aurigraph.v11.proto.ConsensusRole currentRole = io.aurigraph.v11.proto.ConsensusRole.ROLE_FOLLOWER;
    private volatile io.aurigraph.v11.proto.ConsensusPhase currentPhase = io.aurigraph.v11.proto.ConsensusPhase.PHASE_UNKNOWN;
    private volatile long currentTerm = 0L;
    private volatile String currentLeader = "";
    private volatile long lastHeartbeatTime = System.currentTimeMillis();

    // Block proposal and voting state
    private final Map<String, BlockProposal> proposals = new ConcurrentHashMap<>();
    private final Map<String, List<Vote>> votes = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> votersByBlockHash = new ConcurrentHashMap<>();

    // Leader election state
    private final Map<Long, List<ElectionVote>> electionVotes = new ConcurrentHashMap<>();
    private final Queue<String> electionQueue = new ConcurrentLinkedQueue<>();

    // Raft log and state
    private final List<LogEntry> raftLog = Collections.synchronizedList(new ArrayList<>());
    private volatile long lastAppliedIndex = 0L;
    private volatile long commitIndex = 0L;
    private final Map<String, Long> nextIndexMap = new ConcurrentHashMap<>(); // For each follower

    // Validator information
    private final Map<String, ValidatorInfo> validators = new ConcurrentHashMap<>();
    private final Map<String, Double> validatorReputation = new ConcurrentHashMap<>();

    // Consensus metrics
    private final AtomicLong totalBlocksCommitted = new AtomicLong(0);
    private final AtomicLong consensusLatencyMs = new AtomicLong(0);
    private final AtomicLong failedConsensusAttempts = new AtomicLong(0);
    private volatile int activeValidators = 0;
    private volatile int requiredMajority = 0;

    /**
     * RPC 1: proposeBlock - Submit block proposal for consensus voting
     */
    @Override
    public Uni<ProposeBlockResponse> proposeBlock(ProposeBlockRequest request) {
        return Uni.createFrom().item(() -> {
            try {
                String blockHash = request.getBlock().getBlockHash();
                BlockProposal proposal = BlockProposal.newBuilder()
                    .setBlockHash(blockHash)
                    .setBlock(request.getBlock())
                    .setProposerId(request.getProposerId())
                    .setProposalTerm(request.getProposalTerm())
                    .setProposalHeight(request.getBlock().getBlockHeight())
                    .setCreatedAt(getCurrentTimestamp())
                    .build();

                proposals.put(blockHash, proposal);
                currentPhase = ConsensusPhase.PHASE_PROPOSAL;
                votes.putIfAbsent(blockHash, Collections.synchronizedList(new ArrayList<>()));
                votersByBlockHash.putIfAbsent(blockHash, ConcurrentHashMap.newKeySet());

                return io.aurigraph.v11.proto.ProposeBlockResponse.newBuilder()
                    .setBlockHash(blockHash)
                    .setStatus(io.aurigraph.v11.proto.BlockStatus.BLOCK_PROPOSED)
                    .setVotesReceived(0)
                    .setVotesRequired(requiredMajority > 0 ? requiredMajority : calculateMajority(activeValidators))
                    .setTimestamp(getCurrentTimestamp())
                    .build();
            } catch (Exception e) {
                return io.aurigraph.v11.proto.ProposeBlockResponse.newBuilder()
                    .setStatus(io.aurigraph.v11.proto.BlockStatus.BLOCK_ORPHANED)
                    .setTimestamp(getCurrentTimestamp())
                    .build();
            }
        });
    }

    /**
     * RPC 2: voteOnBlock - Cast vote on proposed block
     */
    @Override
    public Uni<VoteOnBlockResponse> voteOnBlock(VoteOnBlockRequest request) {
        return Uni.createFrom().item(() -> {
            try {
                String blockHash = request.getBlockHash();
                String voterId = request.getVoterId();

                List<Vote> blockVotes = votes.getOrDefault(blockHash, Collections.synchronizedList(new ArrayList<>()));
                Set<String> voters = votersByBlockHash.getOrDefault(blockHash, ConcurrentHashMap.newKeySet());

                if (!voters.contains(voterId) && request.getVoteChoice()) {
                    Vote vote = Vote.newBuilder()
                        .setBlockHash(blockHash)
                        .setVoterId(voterId)
                        .setVoteTerm(request.getVoteTerm())
                        .setVoteChoice(request.getVoteChoice())
                        .setVoteTime(getCurrentTimestamp())
                        .setVoteSignature(request.getVoteSignature())
                        .build();

                    blockVotes.add(vote);
                    voters.add(voterId);
                    votes.put(blockHash, blockVotes);
                    votersByBlockHash.put(blockHash, voters);
                }

                int votesNeeded = requiredMajority > 0 ? requiredMajority : calculateMajority(activeValidators);
                int totalVotes = blockVotes.size();

                return VoteOnBlockResponse.newBuilder()
                    .setBlockHash(blockHash)
                    .setVoteAccepted(request.getVoteChoice())
                    .setTotalVotes(totalVotes)
                    .setVotesNeeded(votesNeeded)
                    .setTimestamp(getCurrentTimestamp())
                    .build();
            } catch (Exception e) {
                return VoteOnBlockResponse.newBuilder()
                    .setBlockHash(request.getBlockHash())
                    .setVoteAccepted(false)
                    .setTimestamp(getCurrentTimestamp())
                    .build();
            }
        });
    }

    /**
     * RPC 3: commitBlock - Finalize block commitment after consensus
     */
    @Override
    public Uni<CommitBlockResponse> commitBlock(CommitBlockRequest request) {
        return Uni.createFrom().item(() -> {
            try {
                String blockHash = request.getBlockHash();
                BlockProposal proposal = proposals.get(blockHash);

                if (proposal != null && request.getValidatorSignaturesCount() >= calculateMajority(activeValidators)) {
                    currentPhase = ConsensusPhase.PHASE_COMMITMENT;
                    totalBlocksCommitted.incrementAndGet();
                    commitIndex = Math.max(commitIndex, request.getBlock().getBlockHeight());
                    currentPhase = ConsensusPhase.PHASE_FINALIZATION;

                    // Add to raft log
                    LogEntry logEntry = LogEntry.newBuilder()
                        .setIndex(raftLog.size() + 1)
                        .setTerm(currentTerm)
                        .setCommand("COMMIT_BLOCK")
                        .setData(com.google.protobuf.ByteString.copyFromUtf8(blockHash))
                        .setCreatedAt(getCurrentTimestamp())
                        .build();
                    raftLog.add(logEntry);

                    return CommitBlockResponse.newBuilder()
                        .setBlockHash(blockHash)
                        .setStatus(BlockStatus.BLOCK_FINALIZED)
                        .setBlockHeight(request.getBlock().getBlockHeight())
                        .setConfirmationCount(request.getValidatorSignaturesCount())
                        .setCommitTime(getCurrentTimestamp())
                        .build();
                }

                return CommitBlockResponse.newBuilder()
                    .setBlockHash(blockHash)
                    .setStatus(io.aurigraph.v11.proto.BlockStatus.BLOCK_ORPHANED)
                    .setBlockHeight(request.getBlock().getBlockHeight())
                    .setConfirmationCount(0)
                    .setCommitTime(getCurrentTimestamp())
                    .build();
            } catch (Exception e) {
                return CommitBlockResponse.newBuilder()
                    .setBlockHash(request.getBlockHash())
                    .setStatus(io.aurigraph.v11.proto.BlockStatus.BLOCK_ORPHANED)
                    .setCommitTime(getCurrentTimestamp())
                    .build();
            }
        });
    }

    /**
     * RPC 4: requestLeaderElection - Initiate leader election
     */
    @Override
    public Uni<LeaderElectionResponse> requestLeaderElection(LeaderElectionRequest request) {
        return Uni.createFrom().item(() -> {
            try {
                String candidateId = request.getCandidateId();
                long electionTerm = request.getElectionTerm();

                electionQueue.offer(candidateId);
                electionVotes.putIfAbsent(electionTerm, Collections.synchronizedList(new ArrayList<>()));

                // Simulate election acceptance (in production, would collect actual votes)
                currentTerm = Math.max(currentTerm, electionTerm);
                currentRole = ConsensusRole.ROLE_CANDIDATE;

                int votesRequired = calculateMajority(activeValidators);

                return LeaderElectionResponse.newBuilder()
                    .setElectionAccepted(true)
                    .setNewLeaderId(candidateId)
                    .setElectionTerm(electionTerm)
                    .setVotesReceived(1)
                    .setVotesRequired(votesRequired)
                    .setTimestamp(getCurrentTimestamp())
                    .build();
            } catch (Exception e) {
                return LeaderElectionResponse.newBuilder()
                    .setElectionAccepted(false)
                    .setTimestamp(getCurrentTimestamp())
                    .build();
            }
        });
    }

    /**
     * RPC 5: heartbeat - Leader sends heartbeat to maintain leadership
     */
    @Override
    public Uni<HeartbeatResponse> heartbeat(HeartbeatRequest request) {
        return Uni.createFrom().item(() -> {
            try {
                lastHeartbeatTime = System.currentTimeMillis();
                currentTerm = Math.max(currentTerm, request.getCurrentTerm());

                if (!currentLeader.equals(request.getLeaderId())) {
                    currentLeader = request.getLeaderId();
                    currentRole = ConsensusRole.ROLE_FOLLOWER;
                }

                long nextIndex = Math.max(lastAppliedIndex + 1, request.getPrevLogIndex());
                nextIndexMap.put(request.getLeaderId(), nextIndex);

                return HeartbeatResponse.newBuilder()
                    .setFollowerId("")
                    .setCurrentTerm(currentTerm)
                    .setHeartbeatAccepted(true)
                    .setNextLogIndex(nextIndex)
                    .setTimestamp(getCurrentTimestamp())
                    .build();
            } catch (Exception e) {
                return HeartbeatResponse.newBuilder()
                    .setHeartbeatAccepted(false)
                    .setTimestamp(getCurrentTimestamp())
                    .build();
            }
        });
    }

    /**
     * RPC 6: syncState - Synchronize node state with leader
     */
    @Override
    public Uni<SyncStateResponse> syncState(SyncStateRequest request) {
        return Uni.createFrom().item(() -> {
            try {
                // Return log entries from requested index onward
                long startIndex = Math.min(request.getLastLogIndex() + 1, raftLog.size());
                List<LogEntry> entriesToSync = new ArrayList<>();

                for (int i = (int) startIndex; i < raftLog.size() && i < startIndex + 100; i++) {
                    entriesToSync.add(raftLog.get(i));
                }

                return SyncStateResponse.newBuilder()
                    .setSyncSuccessful(true)
                    .setRemoteTerm(currentTerm)
                    .setRemoteLogIndex(raftLog.size())
                    .addAllLogEntries(entriesToSync)
                    .setStateSnapshot("")
                    .setTimestamp(getCurrentTimestamp())
                    .build();
            } catch (Exception e) {
                return SyncStateResponse.newBuilder()
                    .setSyncSuccessful(false)
                    .setTimestamp(getCurrentTimestamp())
                    .build();
            }
        });
    }

    /**
     * RPC 7: getConsensusState - Query current consensus state
     */
    @Override
    public Uni<ConsensusStateResponse> getConsensusState(GetConsensusStateRequest request) {
        return Uni.createFrom().item(() -> {
            try {
                ConsensusState state = ConsensusState.newBuilder()
                    .setCurrentRole(currentRole)
                    .setCurrentPhase(currentPhase)
                    .setCurrentTerm(currentTerm)
                    .setCurrentLeader(currentLeader)
                    .setActiveValidators(activeValidators)
                    .setRequiredMajority(requiredMajority > 0 ? requiredMajority : calculateMajority(activeValidators))
                    .setLastHeartbeat(Timestamp.newBuilder()
                        .setSeconds(lastHeartbeatTime / 1000)
                        .setNanos((int) ((lastHeartbeatTime % 1000) * 1_000_000))
                        .build())
                    .setStateHash("")
                    .build();

                ConsensusStateResponse.Builder response = ConsensusStateResponse.newBuilder()
                    .setState(state);

                if (request.getIncludeValidators()) {
                    response.addAllValidators(validators.values());
                }

                if (request.getIncludeMetrics()) {
                    ConsensusMetrics metrics = ConsensusMetrics.newBuilder()
                        .setConsensusLatencyMs(consensusLatencyMs.get())
                        .setTotalValidators(validators.size())
                        .setActiveValidators(activeValidators)
                        .setTotalBlocksCommitted(totalBlocksCommitted.get())
                        .setAverageBlockTimeMs(totalBlocksCommitted.get() > 0 ? 1000.0 : 0.0)
                        .setFailedConsensusAttempts((int) failedConsensusAttempts.get())
                        .setNetworkHealthPercent(95.0)
                        .setMeasurementTime(getCurrentTimestamp())
                        .build();
                    response.setMetrics(metrics);
                }

                response.setTimestamp(getCurrentTimestamp());
                return response.build();
            } catch (Exception e) {
                return ConsensusStateResponse.newBuilder()
                    .setTimestamp(getCurrentTimestamp())
                    .build();
            }
        });
    }

    /**
     * RPC 8: getValidatorInfo - Retrieve specific validator information
     */
    @Override
    public Uni<ValidatorInfoResponse> getValidatorInfo(GetValidatorInfoRequest request) {
        return Uni.createFrom().item(() -> {
            try {
                String validatorId = request.getValidatorId();
                ValidatorInfo validator = validators.get(validatorId);

                if (validator != null) {
                    double reputation = validatorReputation.getOrDefault(validatorId, 75.0);

                    return ValidatorInfoResponse.newBuilder()
                        .setValidator(validator)
                        .setReputationScore(reputation)
                        .setBlocksValidated((int) (totalBlocksCommitted.get() / Math.max(validators.size(), 1)))
                        .setFailedValidations((int) (failedConsensusAttempts.get() / Math.max(validators.size(), 1)))
                        .setLastActivity(getCurrentTimestamp())
                        .build();
                }

                return ValidatorInfoResponse.newBuilder()
                    .setReputationScore(0.0)
                    .setBlocksValidated(0)
                    .setFailedValidations(0)
                    .setLastActivity(getCurrentTimestamp())
                    .build();
            } catch (Exception e) {
                return ValidatorInfoResponse.newBuilder()
                    .setReputationScore(0.0)
                    .setLastActivity(getCurrentTimestamp())
                    .build();
            }
        });
    }

    /**
     * RPC 9: submitConsensusMetrics - Submit validator metrics for monitoring
     */
    @Override
    public Uni<SubmitConsensusMetricsResponse> submitConsensusMetrics(SubmitConsensusMetricsRequest request) {
        return Uni.createFrom().item(() -> {
            try {
                ConsensusMetrics metrics = request.getMetrics();
                consensusLatencyMs.set((long) metrics.getConsensusLatencyMs());
                activeValidators = metrics.getActiveValidators();
                failedConsensusAttempts.addAndGet(metrics.getFailedConsensusAttempts());

                return SubmitConsensusMetricsResponse.newBuilder()
                    .setMetricsAccepted(true)
                    .setMessage("Metrics recorded from validator " + request.getValidatorId())
                    .setTimestamp(getCurrentTimestamp())
                    .build();
            } catch (Exception e) {
                return SubmitConsensusMetricsResponse.newBuilder()
                    .setMetricsAccepted(false)
                    .setMessage("Failed to record metrics: " + e.getMessage())
                    .setTimestamp(getCurrentTimestamp())
                    .build();
            }
        });
    }

    /**
     * RPC 10: getRaftLog - Query Raft log entries
     */
    @Override
    public Uni<RaftLogResponse> getRaftLog(GetRaftLogRequest request) {
        return Uni.createFrom().item(() -> {
            try {
                long startIndex = Math.max(0, request.getStartIndex() - 1);
                long endIndex = request.getEndIndex() > 0 ?
                    Math.min(request.getEndIndex(), raftLog.size()) :
                    raftLog.size();
                int limit = request.getLimit() > 0 ? request.getLimit() : 100;

                List<LogEntry> entries = new ArrayList<>();
                for (long i = startIndex; i < Math.min(endIndex, startIndex + limit) && i < raftLog.size(); i++) {
                    entries.add(raftLog.get((int) i));
                }

                return RaftLogResponse.newBuilder()
                    .addAllLogEntries(entries)
                    .setLogSize(raftLog.size())
                    .setLastAppliedIndex(lastAppliedIndex)
                    .setLastLogTerm(currentTerm)
                    .setTimestamp(getCurrentTimestamp())
                    .build();
            } catch (Exception e) {
                return RaftLogResponse.newBuilder()
                    .setLogSize(0)
                    .setLastAppliedIndex(lastAppliedIndex)
                    .setTimestamp(getCurrentTimestamp())
                    .build();
            }
        });
    }

    /**
     * RPC 11: streamConsensusEvents - Real-time consensus event stream (server-side streaming)
     */
    @Override
    public Multi<ConsensusEvent> streamConsensusEvents(StreamConsensusEventsRequest request) {
        return Multi.createFrom().ticks().every(java.time.Duration.ofMillis(500))
            .onItem().transform(i -> {
                // Generate consensus events based on current activity
                String eventType = "HEARTBEAT";
                String eventData = "";

                if (!proposals.isEmpty()) {
                    eventType = "PROPOSAL";
                    eventData = "Block proposal in progress";
                } else if (!votes.isEmpty()) {
                    eventType = "VOTE";
                    eventData = "Consensus voting active";
                } else if (!electionQueue.isEmpty()) {
                    eventType = "ELECTION";
                    eventData = "Leader election in progress";
                    String candidate = electionQueue.poll();
                    if (candidate != null) {
                        currentLeader = candidate;
                        currentRole = ConsensusRole.ROLE_LEADER;
                    }
                }

                return ConsensusEvent.newBuilder()
                    .setEventType(eventType)
                    .setEventId(UUID.randomUUID().toString())
                    .setSourceValidator(currentLeader.isEmpty() ? "system" : currentLeader)
                    .setEventTerm(currentTerm)
                    .setEventData(com.google.protobuf.ByteString.copyFromUtf8(eventData))
                    .setStreamId(UUID.randomUUID().toString())
                    .setEventSequence(totalBlocksCommitted.get())
                    .setTimestamp(getCurrentTimestamp())
                    .build();
            })
            .ifNoItem().after(java.time.Duration.ofSeconds(300))
            .recoverWithCompletion();
    }

    // Helper methods
    private int calculateMajority(int totalValidators) {
        return (totalValidators / 2) + 1;
    }

    private Timestamp getCurrentTimestamp() {
        long now = System.currentTimeMillis();
        return Timestamp.newBuilder()
            .setSeconds(now / 1000)
            .setNanos((int) ((now % 1000) * 1_000_000))
            .build();
    }
}
