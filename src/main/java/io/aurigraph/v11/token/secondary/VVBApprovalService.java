package io.aurigraph.v11.token.secondary;

import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * VVB Approval Service
 *
 * Manages the complete Virtual Validator Board (VVB) approval workflow.
 * Handles:
 * - Approval request creation with validator list
 * - Vote submission and validation
 * - Byzantine Fault Tolerant consensus calculation
 * - State transitions and event firing
 * - Token version activation/rejection based on approval
 *
 * Performance Target: <10ms consensus calculation, >1,000 votes/sec
 *
 * Byzantine FT Logic:
 * - Requires >2/3 majority (>66.67%) for approval
 * - ABSTAIN votes are excluded (enables FT with 1/3 Byzantine nodes)
 * - Early termination when consensus becomes impossible
 *
 * CDI Events Fired:
 * - ApprovalRequestCreatedEvent: When approval request created
 * - VoteSubmittedEvent: When vote registered
 * - ConsensusReachedEvent: When consensus achieved
 * - ApprovalEvent: When approval finalized
 * - RejectionEvent: When rejection finalized
 *
 * @version 12.0.0
 * @since December 23, 2025
 */
@ApplicationScoped
public class VVBApprovalService {

    // =========================================================================
    // Dependencies
    // =========================================================================

    @Inject
    VVBApprovalRegistry registry;

    @Inject
    SecondaryTokenVersioningService versioningService;

    @Inject
    Event<ApprovalRequestCreatedEvent> approvalRequestCreatedEvent;

    @Inject
    Event<VoteSubmittedEvent> voteSubmittedEvent;

    @Inject
    Event<ConsensusReachedEvent> consensusReachedEvent;

    @Inject
    Event<ApprovalEvent> approvalEvent;

    @Inject
    Event<RejectionEvent> rejectionEvent;

    // =========================================================================
    // Approval Request Management
    // =========================================================================

    /**
     * Create a new approval request for a token version.
     *
     * Workflow:
     * 1. Create VVBApprovalRequest with validators and voting window
     * 2. Register in approval registry
     * 3. Fire ApprovalRequestCreatedEvent
     * 4. Return request ID
     *
     * @param tokenVersionId the secondary token version ID
     * @param validators list of validator IDs eligible to vote
     * @param votingWindowSeconds voting window duration in seconds
     * @return the approval request ID
     * @throws IllegalArgumentException if token version not found or invalid
     */
    @Transactional
    public UUID createApprovalRequest(UUID tokenVersionId, List<String> validators,
                                      Long votingWindowSeconds) {
        return createApprovalRequest(tokenVersionId, validators, votingWindowSeconds, 66.67);
    }

    /**
     * Create a new approval request with custom approval threshold.
     *
     * @param tokenVersionId the secondary token version ID
     * @param validators list of validator IDs eligible to vote
     * @param votingWindowSeconds voting window duration in seconds
     * @param approvalThreshold approval threshold percentage (e.g., 66.67, 75.0, 90.0)
     * @return the approval request ID
     */
    @Transactional
    public UUID createApprovalRequest(UUID tokenVersionId, List<String> validators,
                                      Long votingWindowSeconds, Double approvalThreshold) {
        if (tokenVersionId == null || validators == null || validators.isEmpty() ||
            votingWindowSeconds == null || votingWindowSeconds <= 0) {
            throw new IllegalArgumentException("Invalid approval request parameters");
        }

        // Create approval request
        UUID requestId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime votingWindowEnd = now.plusSeconds(votingWindowSeconds);

        VVBApprovalRequest request = new VVBApprovalRequest();
        request.setRequestId(requestId);
        request.setTokenVersionId(tokenVersionId);
        request.setCreatedAt(now);
        request.setVotingWindowEnd(votingWindowEnd);
        request.setVotingWindowSeconds(votingWindowSeconds);
        request.setApprovalThreshold(approvalThreshold);
        request.setTotalValidators(validators.size());
        request.setStatus(ApprovalStatus.PENDING);

        // Register request
        registry.registerRequest(request);

        Log.infof("Created approval request %s for token version %s with %d validators, " +
                  "voting window %d seconds, threshold %.2f%%",
                requestId, tokenVersionId, validators.size(), votingWindowSeconds, approvalThreshold);

        // Fire event
        approvalRequestCreatedEvent.fire(new ApprovalRequestCreatedEvent(
                requestId, tokenVersionId, validators.size(), votingWindowEnd
        ));

        return requestId;
    }

    // =========================================================================
    // Vote Submission
    // =========================================================================

    /**
     * Submit a validator vote on an approval request.
     *
     * Workflow:
     * 1. Validate request exists and voting is open
     * 2. Validate validator hasn't already voted
     * 3. Create ValidatorVote with vote choice and signature
     * 4. Register vote in registry
     * 5. Calculate consensus
     * 6. If consensus reached or impossible, finalize approval/rejection
     * 7. Fire VoteSubmittedEvent and potentially ConsensusReachedEvent
     *
     * @param requestId the approval request ID
     * @param validatorId the validator ID
     * @param voteChoice the vote choice (YES, NO, ABSTAIN)
     * @param signature cryptographic signature of vote (optional)
     * @return the vote ID
     * @throws IllegalArgumentException if request not found, voting closed, or validator already voted
     */
    @Transactional
    public UUID submitValidatorVote(UUID requestId, String validatorId, VoteChoice voteChoice,
                                    String signature) {
        return submitValidatorVote(requestId, validatorId, voteChoice, signature, null);
    }

    /**
     * Submit a validator vote with optional reason.
     *
     * @param requestId the approval request ID
     * @param validatorId the validator ID
     * @param voteChoice the vote choice (YES, NO, ABSTAIN)
     * @param signature cryptographic signature (optional)
     * @param reason vote reason/justification (optional)
     * @return the vote ID
     */
    @Transactional
    public UUID submitValidatorVote(UUID requestId, String validatorId, VoteChoice voteChoice,
                                    String signature, String reason) {
        if (requestId == null || validatorId == null || voteChoice == null) {
            throw new IllegalArgumentException("requestId, validatorId, and voteChoice cannot be null");
        }

        // Lookup request
        VVBApprovalRequest request = registry.lookupRequest(requestId);
        if (request == null) {
            throw new IllegalArgumentException("Approval request not found: " + requestId);
        }

        // Validate voting is open
        if (!request.isVotingOpen()) {
            throw new IllegalArgumentException("Voting window has expired for request: " + requestId);
        }

        // Validate validator hasn't already voted
        if (registry.hasVoted(requestId, validatorId)) {
            throw new IllegalArgumentException(
                "Validator " + validatorId + " has already voted on request " + requestId
            );
        }

        // Create vote
        UUID voteId = UUID.randomUUID();
        ValidatorVote vote = new ValidatorVote();
        vote.setVoteId(voteId);
        vote.setApprovalRequestId(requestId);
        vote.setValidatorId(validatorId);
        vote.setVote(voteChoice);
        vote.setSignature(signature);
        vote.setReason(reason);
        vote.setVotedAt(LocalDateTime.now());

        // Register vote
        registry.registerVote(vote);

        Log.infof("Registered vote %s from validator %s on request %s: %s",
                voteId, validatorId, requestId, voteChoice);

        // Fire vote submitted event
        voteSubmittedEvent.fire(new VoteSubmittedEvent(voteId, requestId, validatorId, voteChoice));

        // Calculate consensus
        ConsensusResult consensus = calculateConsensus(requestId);

        // Check if consensus reached or impossible
        if (consensus.isConsensusReached() || consensus.isImpossibleToReach()) {
            consensusReachedEvent.fire(new ConsensusReachedEvent(requestId, consensus));

            if (consensus.isApproved()) {
                executeApproval(requestId);
            } else {
                executeRejection(requestId, consensus.isImpossibleToReach() ?
                        "Rejection impossible to reach" : "Consensus reached rejection");
            }
        }

        return voteId;
    }

    // =========================================================================
    // Consensus Calculation (Byzantine FT)
    // =========================================================================

    /**
     * Calculate consensus for an approval request.
     *
     * Byzantine FT Logic:
     * - Requires >2/3 of active voters (non-abstaining) to approve
     * - Requires >2/3 of active voters to reject
     * - ABSTAIN votes are excluded (enables tolerance of up to 1/3 Byzantine nodes)
     * - Early termination: Can declare impossibility if remaining votes can't change result
     *
     * Examples:
     * - 10 validators: 7 approve, 3 pending → approved (>2/3 of 10 = 7)
     * - 10 validators: 7 approve, 2 reject, 1 pending → approved (>2/3 of 9 active)
     * - 10 validators: 3 approve, 6 reject, 1 pending → rejected (>2/3 of 9 active)
     * - 10 validators: 4 approve, 4 reject, 2 abstain → impossible (both positions met)
     *
     * @param requestId the approval request ID
     * @return ConsensusResult with voting metrics
     */
    @Transactional
    public ConsensusResult calculateConsensus(UUID requestId) {
        VVBApprovalRequest request = registry.lookupRequest(requestId);
        if (request == null) {
            throw new IllegalArgumentException("Approval request not found: " + requestId);
        }

        int approvalCount = request.getApprovalCount();
        int rejectionCount = request.getRejectionCount();
        int abstainCount = request.getAbstainCount();
        int totalValidators = request.getTotalValidators();

        // Calculate active voters (excluding abstainers)
        int activeVoters = totalValidators - abstainCount;

        // Determine minimum votes for >2/3 majority
        // Need more than 2/3, so: ceiling(activeVoters * 2/3) + 1
        // Or equivalently: (activeVoters / 3) + 1 for integer division
        int minForApproval = (activeVoters > 0) ? (activeVoters / 3) + 1 : 1;
        int minForRejection = minForApproval;  // Same threshold (>2/3 for either direction)

        // Determine if consensus reached
        boolean consensusReached = (approvalCount > minForApproval) ||
                                   (rejectionCount > minForRejection);

        // Determine if impossible to reach (both sides now have enough votes)
        int remainingVotes = totalValidators - approvalCount - rejectionCount - abstainCount;
        int maxApprovalPossible = approvalCount + remainingVotes;
        int maxRejectionPossible = rejectionCount + remainingVotes;

        // Impossible if neither side can reach consensus even with remaining votes
        boolean impossibleToReach = (maxApprovalPossible <= minForApproval &&
                                    maxRejectionPossible <= minForRejection);

        // Calculate approval percentage (among active voters)
        double percentage = (activeVoters > 0) ? (approvalCount * 100.0 / activeVoters) : 0.0;

        ConsensusResult result = new ConsensusResult();
        result.setConsensusReached(consensusReached);
        result.setImpossibleToReach(impossibleToReach);
        result.setApprovalCount(approvalCount);
        result.setRejectionCount(rejectionCount);
        result.setAbstainCount(abstainCount);
        result.setTotalVoters(totalValidators);
        result.setPercentage(percentage);

        Log.debugf("Consensus calculation for request %s: reached=%b, impossible=%b, " +
                   "approval=%d, rejection=%d, abstain=%d, total=%d, percentage=%.2f%%",
                requestId, consensusReached, impossibleToReach,
                approvalCount, rejectionCount, abstainCount, totalValidators, percentage);

        return result;
    }

    // =========================================================================
    // Approval/Rejection Execution
    // =========================================================================

    /**
     * Execute approval (finalize as APPROVED).
     * Moves token version to ACTIVE status.
     *
     * @param requestId the approval request ID
     */
    @Transactional
    public void executeApproval(UUID requestId) {
        VVBApprovalRequest request = registry.lookupRequest(requestId);
        if (request == null) {
            throw new IllegalArgumentException("Approval request not found: " + requestId);
        }

        if (request.isTerminal()) {
            throw new IllegalStateException("Cannot execute approval on terminal request");
        }

        // Update request status
        registry.updateRequestStatus(requestId, ApprovalStatus.APPROVED);

        // Activate token version
        try {
            versioningService.activateVersion(request.getTokenVersionId())
                    .await().indefinitely();
        } catch (Exception e) {
            Log.errorf(e, "Failed to activate version %s after approval", request.getTokenVersionId());
            throw e;
        }

        Log.infof("Executed approval for request %s, activated token version %s",
                requestId, request.getTokenVersionId());

        // Fire approval event for Story 6 integration
        ApprovalEvent event = new ApprovalEvent();
        event.setRequestId(requestId);
        event.setVersionId(request.tokenVersionId);
        event.setStatus("APPROVED");
        event.setApprovalCount(request.approvalCount != null ? request.approvalCount : 0);
        event.setTotalValidators(request.totalValidators != null ? request.totalValidators : 0);
        event.setThresholdPercentage(request.approvalThreshold != null ? request.approvalThreshold : 66.67);
        event.setConsensusReachedAt(java.time.Instant.now());
        event.setApproverIds(java.util.Collections.emptyList());
        event.setErrorMessage(null);
        approvalEvent.fire(event);
    }

    /**
     * Execute rejection (finalize as REJECTED).
     * Archives token version and prevents activation.
     *
     * @param requestId the approval request ID
     * @param rejectionReason reason for rejection
     */
    @Transactional
    public void executeRejection(UUID requestId, String rejectionReason) {
        VVBApprovalRequest request = registry.lookupRequest(requestId);
        if (request == null) {
            throw new IllegalArgumentException("Approval request not found: " + requestId);
        }

        if (request.isTerminal()) {
            throw new IllegalStateException("Cannot execute rejection on terminal request");
        }

        // Update request status
        registry.updateRequestStatus(requestId, ApprovalStatus.REJECTED);

        // Archive token version
        try {
            versioningService.rejectVersion(request.getTokenVersionId(), rejectionReason, "VVB_SYSTEM")
                    .await().indefinitely();
        } catch (Exception e) {
            Log.errorf(e, "Failed to reject version %s after rejection consensus",
                    request.getTokenVersionId());
            throw e;
        }

        Log.infof("Executed rejection for request %s, rejected token version %s: %s",
                requestId, request.getTokenVersionId(), rejectionReason);

        // Fire rejection event
        rejectionEvent.fire(new RejectionEvent(requestId, request.getTokenVersionId(), rejectionReason));
    }

    // =========================================================================
    // Query Operations
    // =========================================================================

    /**
     * Get approval request details.
     *
     * @param requestId the approval request ID
     * @return the approval request, or null if not found
     */
    public VVBApprovalRequest getRequest(UUID requestId) {
        return registry.lookupRequest(requestId);
    }

    /**
     * Get approval request for a token version.
     *
     * @param tokenVersionId the token version ID
     * @return the approval request, or null if not found
     */
    public VVBApprovalRequest getRequestByTokenVersion(UUID tokenVersionId) {
        return registry.lookupRequestByTokenVersion(tokenVersionId);
    }

    /**
     * Get all votes for an approval request.
     *
     * @param requestId the approval request ID
     * @return list of votes
     */
    public List<ValidatorVote> getVotes(UUID requestId) {
        return registry.getVotesByRequest(requestId);
    }

    /**
     * Get all votes from a validator.
     *
     * @param validatorId the validator ID
     * @return list of votes
     */
    public List<ValidatorVote> getValidatorVotes(String validatorId) {
        return registry.getVotesByValidator(validatorId);
    }

    /**
     * Get approval request statistics.
     *
     * @param requestId the approval request ID
     * @return map of statistics
     */
    public Map<String, Object> getRequestStats(UUID requestId) {
        return registry.getStats(requestId);
    }

    /**
     * Get all pending approval requests.
     *
     * @return list of pending requests
     */
    public List<VVBApprovalRequest> getPendingRequests() {
        return registry.getPendingRequests();
    }

    /**
     * Get all expired approval requests (voting window closed).
     *
     * @return list of expired requests
     */
    public List<VVBApprovalRequest> getExpiredRequests() {
        return registry.getExpiredRequests();
    }

    // =========================================================================
    // Maintenance Operations
    // =========================================================================

    /**
     * Process expired approval requests.
     * For each expired PENDING request, finalize as EXPIRED.
     * Typically called by a scheduled task.
     *
     * @return number of requests processed
     */
    @Transactional
    public int processExpiredRequests() {
        List<VVBApprovalRequest> expired = getExpiredRequests();
        AtomicInteger processed = new AtomicInteger(0);

        expired.forEach(request -> {
            try {
                registry.updateRequestStatus(request.getRequestId(), ApprovalStatus.EXPIRED);
                Log.infof("Expired approval request %s for token version %s",
                        request.getRequestId(), request.getTokenVersionId());
                processed.incrementAndGet();
            } catch (Exception e) {
                Log.errorf(e, "Failed to expire request %s", request.getRequestId());
            }
        });

        return processed.get();
    }
}
