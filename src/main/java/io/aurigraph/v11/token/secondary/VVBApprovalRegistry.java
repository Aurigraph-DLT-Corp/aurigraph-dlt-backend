package io.aurigraph.v11.token.secondary;

import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Singleton;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * VVB Approval Registry
 *
 * Thread-safe in-memory registry for approval requests and validator votes.
 * Maintains 4 concurrent indexes for fast lookups and filtering.
 * All operations are O(1) or O(n) complexity with guaranteed thread safety.
 *
 * Indexes:
 * 1. requestsById: UUID -> VVBApprovalRequest (primary lookup)
 * 2. requestsByTokenVersion: UUID -> VVBApprovalRequest (version tracking)
 * 3. votesByRequest: UUID -> List<ValidatorVote> (request votes)
 * 4. votesByValidator: String -> List<ValidatorVote> (validator history)
 *
 * Performance Target: <5ms all lookups, thread-safe without external locking
 *
 * @version 12.0.0
 * @since December 23, 2025
 */
@ApplicationScoped
public class VVBApprovalRegistry {

    // =========================================================================
    // Concurrent Index Maps
    // =========================================================================

    /**
     * Index: Approval Request ID -> Approval Request.
     * Primary lookup index for requests.
     */
    private final ConcurrentHashMap<UUID, VVBApprovalRequest> requestsById = new ConcurrentHashMap<>();

    /**
     * Index: Token Version ID -> Approval Request.
     * Enables fast lookup of approval request for a token version.
     * Note: One-to-one relationship (each version has one approval request).
     */
    private final ConcurrentHashMap<UUID, VVBApprovalRequest> requestsByTokenVersion = new ConcurrentHashMap<>();

    /**
     * Index: Approval Request ID -> List of ValidatorVotes.
     * Enables fast lookup of all votes for a request.
     */
    private final ConcurrentHashMap<UUID, List<ValidatorVote>> votesByRequest = new ConcurrentHashMap<>();

    /**
     * Index: Validator ID -> List of ValidatorVotes.
     * Enables fast lookup of all votes by a validator (audit trail).
     */
    private final ConcurrentHashMap<String, List<ValidatorVote>> votesByValidator = new ConcurrentHashMap<>();

    // =========================================================================
    // Request Registry Operations
    // =========================================================================

    /**
     * Register a new approval request.
     * Adds request to requestsById and requestsByTokenVersion indexes.
     *
     * @param request the approval request to register
     * @throws IllegalArgumentException if request already exists
     */
    public void registerRequest(VVBApprovalRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request cannot be null");
        }
        request.validate();

        if (requestsById.containsKey(request.getRequestId())) {
            throw new IllegalArgumentException(
                "Approval request with ID " + request.getRequestId() + " already exists"
            );
        }

        requestsById.put(request.getRequestId(), request);
        requestsByTokenVersion.put(request.getTokenVersionId(), request);

        Log.debugf("Registered approval request: %s for token version: %s",
                request.getRequestId(), request.getTokenVersionId());
    }

    /**
     * Lookup approval request by request ID.
     *
     * @param requestId the request ID
     * @return the approval request, or null if not found
     */
    public VVBApprovalRequest lookupRequest(UUID requestId) {
        if (requestId == null) {
            return null;
        }
        return requestsById.get(requestId);
    }

    /**
     * Lookup approval request by token version ID.
     *
     * @param tokenVersionId the token version ID
     * @return the approval request, or null if not found
     */
    public VVBApprovalRequest lookupRequestByTokenVersion(UUID tokenVersionId) {
        if (tokenVersionId == null) {
            return null;
        }
        return requestsByTokenVersion.get(tokenVersionId);
    }

    /**
     * Update approval request status.
     * Used after consensus calculation to finalize approval/rejection.
     *
     * @param requestId the request ID
     * @param newStatus the new approval status
     * @throws IllegalArgumentException if request not found
     */
    public void updateRequestStatus(UUID requestId, ApprovalStatus newStatus) {
        VVBApprovalRequest request = lookupRequest(requestId);
        if (request == null) {
            throw new IllegalArgumentException("Approval request not found: " + requestId);
        }

        request.setStatus(newStatus);
        request.setUpdatedAt(java.time.LocalDateTime.now());

        Log.debugf("Updated approval request %s status to %s", requestId, newStatus);
    }

    /**
     * Remove approval request from registry (archival).
     * Used after request is finalized and archived.
     *
     * @param requestId the request ID
     */
    public void removeRequest(UUID requestId) {
        VVBApprovalRequest request = requestsById.remove(requestId);
        if (request != null) {
            requestsByTokenVersion.remove(request.getTokenVersionId());
            Log.debugf("Removed approval request: %s", requestId);
        }
    }

    // =========================================================================
    // Vote Registry Operations
    // =========================================================================

    /**
     * Register a validator vote.
     * Adds vote to votesByRequest and votesByValidator indexes.
     *
     * @param vote the validator vote to register
     * @throws IllegalArgumentException if vote already exists or request not found
     */
    public void registerVote(ValidatorVote vote) {
        if (vote == null) {
            throw new IllegalArgumentException("vote cannot be null");
        }
        vote.validate();

        // Verify approval request exists
        VVBApprovalRequest request = lookupRequest(vote.getApprovalRequestId());
        if (request == null) {
            throw new IllegalArgumentException(
                "Approval request not found: " + vote.getApprovalRequestId()
            );
        }

        // Check for duplicate vote (same request + validator)
        List<ValidatorVote> requestVotes = votesByRequest.get(vote.getApprovalRequestId());
        if (requestVotes != null) {
            boolean alreadyVoted = requestVotes.stream()
                    .anyMatch(v -> v.getValidatorId().equals(vote.getValidatorId()));
            if (alreadyVoted) {
                throw new IllegalArgumentException(
                    "Validator " + vote.getValidatorId() +
                    " has already voted on request " + vote.getApprovalRequestId()
                );
            }
        }

        // Add vote to request votes list (create if doesn't exist)
        votesByRequest.computeIfAbsent(vote.getApprovalRequestId(),
                k -> Collections.synchronizedList(new ArrayList<>()))
                .add(vote);

        // Add vote to validator votes list (create if doesn't exist)
        votesByValidator.computeIfAbsent(vote.getValidatorId(),
                k -> Collections.synchronizedList(new ArrayList<>()))
                .add(vote);

        // Update request vote counts
        switch (vote.getVote()) {
            case YES -> request.setApprovalCount(request.getApprovalCount() + 1);
            case NO -> request.setRejectionCount(request.getRejectionCount() + 1);
            case ABSTAIN -> request.setAbstainCount(request.getAbstainCount() + 1);
        }

        Log.debugf("Registered vote from validator %s on request %s: %s",
                vote.getValidatorId(), vote.getApprovalRequestId(), vote.getVote());
    }

    // =========================================================================
    // Vote Query Operations
    // =========================================================================

    /**
     * Get all votes for an approval request.
     *
     * @param requestId the approval request ID
     * @return list of votes (empty list if none found)
     */
    public List<ValidatorVote> getVotesByRequest(UUID requestId) {
        if (requestId == null) {
            return Collections.emptyList();
        }
        List<ValidatorVote> votes = votesByRequest.get(requestId);
        return votes != null ? new ArrayList<>(votes) : Collections.emptyList();
    }

    /**
     * Get all votes from a validator.
     *
     * @param validatorId the validator ID
     * @return list of votes (empty list if none found)
     */
    public List<ValidatorVote> getVotesByValidator(String validatorId) {
        if (validatorId == null) {
            return Collections.emptyList();
        }
        List<ValidatorVote> votes = votesByValidator.get(validatorId);
        return votes != null ? new ArrayList<>(votes) : Collections.emptyList();
    }

    /**
     * Count approval votes for a request.
     *
     * @param requestId the approval request ID
     * @return count of YES votes
     */
    public long countApprovals(UUID requestId) {
        return getVotesByRequest(requestId).stream()
                .filter(v -> v.getVote() == VoteChoice.YES)
                .count();
    }

    /**
     * Count rejection votes for a request.
     *
     * @param requestId the approval request ID
     * @return count of NO votes
     */
    public long countRejections(UUID requestId) {
        return getVotesByRequest(requestId).stream()
                .filter(v -> v.getVote() == VoteChoice.NO)
                .count();
    }

    /**
     * Count abstain votes for a request.
     *
     * @param requestId the approval request ID
     * @return count of ABSTAIN votes
     */
    public long countAbstains(UUID requestId) {
        return getVotesByRequest(requestId).stream()
                .filter(v -> v.getVote() == VoteChoice.ABSTAIN)
                .count();
    }

    /**
     * Check if validator has voted on a request.
     *
     * @param requestId the approval request ID
     * @param validatorId the validator ID
     * @return true if validator has voted
     */
    public boolean hasVoted(UUID requestId, String validatorId) {
        List<ValidatorVote> votes = getVotesByRequest(requestId);
        return votes.stream()
                .anyMatch(v -> v.getValidatorId().equals(validatorId));
    }

    // =========================================================================
    // Statistics & Metrics
    // =========================================================================

    /**
     * Get statistics for an approval request.
     * Includes vote counts and percentages.
     *
     * @param requestId the approval request ID
     * @return map of statistics
     */
    public Map<String, Object> getStats(UUID requestId) {
        VVBApprovalRequest request = lookupRequest(requestId);
        if (request == null) {
            return Collections.emptyMap();
        }

        List<ValidatorVote> votes = getVotesByRequest(requestId);
        long approvals = countApprovals(requestId);
        long rejections = countRejections(requestId);
        long abstains = countAbstains(requestId);
        long totalVotes = votes.size();
        long activeVotes = approvals + rejections;

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("requestId", requestId);
        stats.put("tokenVersionId", request.getTokenVersionId());
        stats.put("status", request.getStatus());
        stats.put("approvalCount", approvals);
        stats.put("rejectionCount", rejections);
        stats.put("abstainCount", abstains);
        stats.put("totalVotes", totalVotes);
        stats.put("totalValidators", request.getTotalValidators());
        stats.put("votesRemaining", request.getTotalValidators() - totalVotes);
        stats.put("approvalPercentage", activeVotes > 0 ? (approvals * 100.0 / activeVotes) : 0.0);
        stats.put("isVotingOpen", request.isVotingOpen());
        stats.put("votingWindowEnd", request.getVotingWindowEnd());
        stats.put("secondsRemaining", request.getSecondsRemaining());

        return stats;
    }

    /**
     * Get registry statistics.
     *
     * @return map of registry statistics
     */
    public Map<String, Object> getRegistryStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalRequests", requestsById.size());
        stats.put("totalVotes", getTotalVoteCount());
        stats.put("pendingRequests", getPendingRequestCount());
        stats.put("activeValidators", votesByValidator.size());
        return stats;
    }

    /**
     * Get count of total votes across all requests.
     *
     * @return total vote count
     */
    private long getTotalVoteCount() {
        return votesByRequest.values().stream()
                .mapToLong(List::size)
                .sum();
    }

    /**
     * Get count of pending approval requests.
     *
     * @return pending request count
     */
    private long getPendingRequestCount() {
        return requestsById.values().stream()
                .filter(r -> r.getStatus() == ApprovalStatus.PENDING)
                .count();
    }

    /**
     * Get all pending approval requests.
     *
     * @return list of pending requests
     */
    public List<VVBApprovalRequest> getPendingRequests() {
        return requestsById.values().stream()
                .filter(r -> r.getStatus() == ApprovalStatus.PENDING)
                .sorted(Comparator.comparing(VVBApprovalRequest::getCreatedAt))
                .collect(Collectors.toList());
    }

    /**
     * Get all requests that have expired voting window.
     *
     * @return list of expired requests
     */
    public List<VVBApprovalRequest> getExpiredRequests() {
        return requestsById.values().stream()
                .filter(VVBApprovalRequest::hasExpired)
                .filter(r -> r.getStatus() == ApprovalStatus.PENDING)
                .sorted(Comparator.comparing(VVBApprovalRequest::getVotingWindowEnd))
                .collect(Collectors.toList());
    }

    // =========================================================================
    // Cleanup Operations
    // =========================================================================

    /**
     * Clear all data from registry.
     * Used for testing and cleanup.
     */
    public void clear() {
        requestsById.clear();
        requestsByTokenVersion.clear();
        votesByRequest.clear();
        votesByValidator.clear();
        Log.info("VVB Approval Registry cleared");
    }

    /**
     * Get size of primary index (for testing).
     *
     * @return number of approval requests
     */
    public int size() {
        return requestsById.size();
    }
}
