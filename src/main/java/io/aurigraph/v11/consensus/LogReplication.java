package io.aurigraph.v11.consensus;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

/**
 * HyperRAFT++ Log Replication Implementation
 *
 * Phase 2: Log Replication
 * - Append entries RPC
 * - Log consistency checking
 * - Follower log synchronization
 * - Conflict resolution
 * - Snapshot handling
 *
 * Performance Target: <100ms for 1000 entries, zero data loss
 */
@ApplicationScoped
public class LogReplication {

    private static final Logger LOG = Logger.getLogger(LogReplication.class);

    /**
     * Log entry
     */
    public static class LogEntry {
        public final long index;
        public final long term;
        public final String command;
        public final byte[] data;
        public final Instant timestamp;

        public LogEntry(long index, long term, String command, byte[] data) {
            this.index = index;
            this.term = term;
            this.command = command;
            this.data = data;
            this.timestamp = Instant.now();
        }

        public LogEntry(long index, long term, String command) {
            this(index, term, command, new byte[0]);
        }

        @Override
        public String toString() {
            return String.format("LogEntry[index=%d, term=%d, command=%s]",
                    index, term, command);
        }
    }

    /**
     * AppendEntries RPC request
     */
    public static class AppendEntriesRequest {
        public final long term;
        public final String leaderId;
        public final long prevLogIndex;
        public final long prevLogTerm;
        public final List<LogEntry> entries;
        public final long leaderCommit;

        public AppendEntriesRequest(long term, String leaderId, long prevLogIndex,
                                   long prevLogTerm, List<LogEntry> entries,
                                   long leaderCommit) {
            this.term = term;
            this.leaderId = leaderId;
            this.prevLogIndex = prevLogIndex;
            this.prevLogTerm = prevLogTerm;
            this.entries = entries;
            this.leaderCommit = leaderCommit;
        }

        public boolean isHeartbeat() {
            return entries == null || entries.isEmpty();
        }
    }

    /**
     * AppendEntries RPC response
     */
    public static class AppendEntriesResponse {
        public final long term;
        public final boolean success;
        public final long matchIndex;
        public final long conflictIndex;
        public final long conflictTerm;
        public final String nodeId;

        public AppendEntriesResponse(long term, boolean success, long matchIndex,
                                    long conflictIndex, long conflictTerm, String nodeId) {
            this.term = term;
            this.success = success;
            this.matchIndex = matchIndex;
            this.conflictIndex = conflictIndex;
            this.conflictTerm = conflictTerm;
            this.nodeId = nodeId;
        }

        public static AppendEntriesResponse success(long term, long matchIndex, String nodeId) {
            return new AppendEntriesResponse(term, true, matchIndex, -1, -1, nodeId);
        }

        public static AppendEntriesResponse failure(long term, long conflictIndex,
                                                    long conflictTerm, String nodeId) {
            return new AppendEntriesResponse(term, false, -1, conflictIndex, conflictTerm, nodeId);
        }
    }

    /**
     * Log manager
     */
    public static class LogManager {
        private final String nodeId;
        private final List<LogEntry> log;
        private final RaftState.StateData state;

        // Log metrics
        private final AtomicLong entriesAppended = new AtomicLong(0);
        private final AtomicLong entriesCommitted = new AtomicLong(0);
        private final AtomicLong conflictsResolved = new AtomicLong(0);
        private final AtomicLong totalReplicationTime = new AtomicLong(0);

        // Match index tracking for followers (leader only)
        private final Map<String, Long> matchIndex = new ConcurrentHashMap<>();
        private final Map<String, Long> nextIndex = new ConcurrentHashMap<>();

        public LogManager(String nodeId, RaftState.StateData state) {
            this.nodeId = nodeId;
            this.state = state;
            this.log = new CopyOnWriteArrayList<>();

            // Initialize with sentinel entry
            log.add(new LogEntry(0, 0, "SENTINEL"));
        }

        /**
         * Append entries as leader
         */
        public Uni<Boolean> appendEntriesAsLeader(List<String> commands) {
            return Uni.createFrom().item(() -> {
                if (state.getCurrentState() != RaftState.NodeState.LEADER) {
                    LOG.warn("Cannot append entries: not a leader");
                    return false;
                }

                long startTime = System.nanoTime();
                long currentTerm = state.getCurrentTerm();

                // Create log entries
                List<LogEntry> newEntries = new ArrayList<>();
                for (String command : commands) {
                    long index = getLastLogIndex() + newEntries.size() + 1;
                    LogEntry entry = new LogEntry(index, currentTerm, command);
                    newEntries.add(entry);
                }

                // Append to our log
                log.addAll(newEntries);
                entriesAppended.addAndGet(newEntries.size());

                long replicationTime = System.nanoTime() - startTime;
                totalReplicationTime.addAndGet(replicationTime);

                LOG.infof("Leader %s appended %d entries at term %d",
                        nodeId, newEntries.size(), currentTerm);

                return true;
            });
        }

        /**
         * Handle AppendEntries RPC as follower
         */
        public Uni<AppendEntriesResponse> handleAppendEntries(AppendEntriesRequest request) {
            return Uni.createFrom().item(() -> {
                long currentTerm = state.getCurrentTerm();

                // Reply false if term < currentTerm
                if (request.term < currentTerm) {
                    LOG.debugf("Rejecting AppendEntries from %s (stale term: %d < %d)",
                            request.leaderId, request.term, currentTerm);
                    return AppendEntriesResponse.failure(currentTerm, -1, -1, nodeId);
                }

                // Update term if request has higher term
                if (request.term > currentTerm) {
                    state.setTermIfHigher(request.term);
                    state.transitionTo(RaftState.NodeState.FOLLOWER);
                }

                // Reset election timeout
                state.updateHeartbeatTime();
                state.setLeaderId(request.leaderId);

                // Handle heartbeat (no entries)
                if (request.isHeartbeat()) {
                    updateCommitIndex(request.leaderCommit);
                    return AppendEntriesResponse.success(request.term, getLastLogIndex(), nodeId);
                }

                // Check log consistency
                if (!isLogConsistent(request.prevLogIndex, request.prevLogTerm)) {
                    long conflictIndex = findConflictIndex(request.prevLogIndex);
                    long conflictTerm = 0;

                    // Only get conflict term if index is within bounds
                    if (conflictIndex >= 0 && conflictIndex < log.size()) {
                        conflictTerm = log.get((int) conflictIndex).term;
                    }

                    LOG.debugf("Log inconsistency at index %d (expected term %d)",
                            request.prevLogIndex, request.prevLogTerm);

                    return AppendEntriesResponse.failure(request.term, conflictIndex,
                            conflictTerm, nodeId);
                }

                // Append new entries and resolve conflicts
                appendAndResolveConflicts(request.prevLogIndex, request.entries);

                // Update commit index
                updateCommitIndex(request.leaderCommit);

                long matchIndex = getLastLogIndex();
                LOG.debug(String.format("Appended %d entries from leader %s (matchIndex: %d)",
                        request.entries.size(), request.leaderId, matchIndex));

                return AppendEntriesResponse.success(request.term, matchIndex, nodeId);
            });
        }

        /**
         * Process AppendEntries response as leader
         */
        public void processAppendEntriesResponse(AppendEntriesResponse response) {
            if (state.getCurrentState() != RaftState.NodeState.LEADER) {
                return;
            }

            // Step down if we discover higher term
            if (response.term > state.getCurrentTerm()) {
                state.setTermIfHigher(response.term);
                state.transitionTo(RaftState.NodeState.FOLLOWER);
                LOG.infof("Stepping down from leader (discovered higher term %d)", response.term);
                return;
            }

            if (response.success) {
                // Update match index and next index
                matchIndex.put(response.nodeId, response.matchIndex);
                nextIndex.put(response.nodeId, response.matchIndex + 1);

                // Try to advance commit index
                advanceCommitIndex();
            } else {
                // Decrement next index on failure (conflict resolution)
                long currentNext = nextIndex.getOrDefault(response.nodeId, getLastLogIndex());
                long newNext = response.conflictIndex > 0 ?
                        response.conflictIndex : Math.max(1, currentNext - 1);

                nextIndex.put(response.nodeId, newNext);
                conflictsResolved.incrementAndGet();

                LOG.debugf("Log conflict with %s, adjusting nextIndex to %d",
                        response.nodeId, newNext);
            }
        }

        /**
         * Get log entries for follower synchronization
         */
        public List<LogEntry> getEntriesFrom(long startIndex, int maxCount) {
            if (startIndex < 0 || startIndex > log.size()) {
                return Collections.emptyList();
            }

            int start = (int) startIndex;
            int end = Math.min(start + maxCount, log.size());

            return new ArrayList<>(log.subList(start, end));
        }

        /**
         * Get log entry at index
         */
        public LogEntry getEntry(long index) {
            if (index < 0 || index >= log.size()) {
                return null;
            }
            return log.get((int) index);
        }

        /**
         * Get last log index
         */
        public long getLastLogIndex() {
            return log.size() - 1;
        }

        /**
         * Get last log term
         */
        public long getLastLogTerm() {
            if (log.isEmpty()) {
                return 0;
            }
            return log.get(log.size() - 1).term;
        }

        /**
         * Get log size
         */
        public int getLogSize() {
            return log.size();
        }

        /**
         * Initialize indices for new follower (leader only)
         */
        public void initializeFollowerIndices(String followerId) {
            long lastIndex = getLastLogIndex();
            nextIndex.put(followerId, lastIndex + 1);
            matchIndex.put(followerId, 0L);
            LOG.infof("Initialized indices for follower %s (nextIndex: %d)",
                    followerId, lastIndex + 1);
        }

        /**
         * Get replication metrics
         */
        public ReplicationMetrics getMetrics() {
            double avgReplicationTime = entriesAppended.get() > 0 ?
                    (double) totalReplicationTime.get() / entriesAppended.get() / 1_000_000.0 : 0.0;

            return new ReplicationMetrics(
                    entriesAppended.get(),
                    entriesCommitted.get(),
                    conflictsResolved.get(),
                    log.size(),
                    state.getCommitIndex(),
                    avgReplicationTime
            );
        }

        // Private helper methods

        private boolean isLogConsistent(long prevLogIndex, long prevLogTerm) {
            // Special case: prevLogIndex 0 is always consistent (start of log)
            if (prevLogIndex == 0) {
                return true;
            }

            // Check if we have the entry at prevLogIndex
            if (prevLogIndex >= log.size()) {
                return false;
            }

            // Check if the term matches
            LogEntry entry = log.get((int) prevLogIndex);
            return entry.term == prevLogTerm;
        }

        private long findConflictIndex(long prevLogIndex) {
            // Find the first index of conflicting term
            if (prevLogIndex >= log.size()) {
                return log.size();
            }

            // If prevLogIndex is within bounds, find conflict term
            if (prevLogIndex >= 0 && prevLogIndex < log.size()) {
                long conflictTerm = log.get((int) prevLogIndex).term;
                for (int i = (int) prevLogIndex; i >= 0; i--) {
                    if (log.get(i).term != conflictTerm) {
                        return i + 1;
                    }
                }
            }
            return Math.min(prevLogIndex, log.size());
        }

        private void appendAndResolveConflicts(long prevLogIndex, List<LogEntry> entries) {
            int insertIndex = (int) prevLogIndex + 1;

            for (LogEntry entry : entries) {
                // If an existing entry conflicts, delete it and all following entries
                if (insertIndex < log.size()) {
                    LogEntry existing = log.get(insertIndex);
                    if (existing.term != entry.term) {
                        // Remove conflicting entries
                        log.subList(insertIndex, log.size()).clear();
                        conflictsResolved.incrementAndGet();
                    }
                }

                // Append new entry if not already present
                if (insertIndex >= log.size()) {
                    log.add(entry);
                    entriesAppended.incrementAndGet();
                }

                insertIndex++;
            }
        }

        private void updateCommitIndex(long leaderCommit) {
            if (leaderCommit > state.getCommitIndex()) {
                long newCommitIndex = Math.min(leaderCommit, getLastLogIndex());
                state.setCommitIndex(newCommitIndex);

                // Apply newly committed entries
                applyCommittedEntries();
            }
        }

        private void advanceCommitIndex() {
            long currentCommit = state.getCommitIndex();
            long lastIndex = getLastLogIndex();

            // Try to find the highest index replicated on majority
            for (long n = lastIndex; n > currentCommit; n--) {
                LogEntry entry = log.get((int) n);

                // Only commit entries from current term
                if (entry.term != state.getCurrentTerm()) {
                    continue;
                }

                // Count replicas
                long replicas = 1; // Count self
                for (long matchIdx : matchIndex.values()) {
                    if (matchIdx >= n) {
                        replicas++;
                    }
                }

                // Check if majority
                int quorum = (matchIndex.size() + 1) / 2 + 1;
                if (replicas >= quorum) {
                    state.setCommitIndex(n);
                    applyCommittedEntries();
                    LOG.debugf("Advanced commit index to %d (replicas: %d, quorum: %d)",
                            n, replicas, quorum);
                    break;
                }
            }
        }

        private void applyCommittedEntries() {
            long lastApplied = state.getLastApplied();
            long commitIndex = state.getCommitIndex();

            while (lastApplied < commitIndex) {
                lastApplied++;
                LogEntry entry = log.get((int) lastApplied);

                // Apply entry to state machine (simplified for now)
                applyToStateMachine(entry);

                state.setLastApplied(lastApplied);
                entriesCommitted.incrementAndGet();
            }

            if (lastApplied > state.getLastApplied()) {
                LOG.debugf("Applied entries up to index %d", lastApplied);
            }
        }

        private void applyToStateMachine(LogEntry entry) {
            // Simplified: just log the application
            LOG.debugf("Applied to state machine: %s", entry);
        }
    }

    /**
     * Snapshot metadata
     */
    public static class SnapshotMetadata {
        public final long lastIncludedIndex;
        public final long lastIncludedTerm;
        public final Instant timestamp;
        public final long sizeBytes;

        public SnapshotMetadata(long lastIncludedIndex, long lastIncludedTerm, long sizeBytes) {
            this.lastIncludedIndex = lastIncludedIndex;
            this.lastIncludedTerm = lastIncludedTerm;
            this.timestamp = Instant.now();
            this.sizeBytes = sizeBytes;
        }
    }

    /**
     * Replication metrics
     */
    public static class ReplicationMetrics {
        public final long entriesAppended;
        public final long entriesCommitted;
        public final long conflictsResolved;
        public final int logSize;
        public final long commitIndex;
        public final double avgReplicationTimeMs;

        public ReplicationMetrics(long entriesAppended, long entriesCommitted,
                                long conflictsResolved, int logSize,
                                long commitIndex, double avgReplicationTimeMs) {
            this.entriesAppended = entriesAppended;
            this.entriesCommitted = entriesCommitted;
            this.conflictsResolved = conflictsResolved;
            this.logSize = logSize;
            this.commitIndex = commitIndex;
            this.avgReplicationTimeMs = avgReplicationTimeMs;
        }

        public double getCommitRate() {
            return entriesAppended > 0 ?
                    (double) entriesCommitted / entriesAppended : 0.0;
        }
    }
}
