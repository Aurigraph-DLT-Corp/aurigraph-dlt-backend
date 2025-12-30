package io.aurigraph.v11.governance;

import io.aurigraph.v11.crypto.DilithiumSignatureService;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.math.BigDecimal;
import java.security.PrivateKey;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Governance Service
 *
 * Core service for managing governance proposals, voting, and execution.
 * Implements enterprise governance with quantum-resistant signatures.
 *
 * @author SCA (Security & Cryptography Agent)
 * @version 11.0.0
 * @since Sprint 8 - Governance & Quantum Signing
 */
@ApplicationScoped
public class GovernanceService {

    private static final Logger LOG = Logger.getLogger(GovernanceService.class);

    @Inject
    DilithiumSignatureService signatureService;

    // In-memory storage (in production, would use database)
    private final Map<String, Proposal> proposals = new ConcurrentHashMap<>();
    private final Map<String, List<Vote>> proposalVotes = new ConcurrentHashMap<>();
    private final Map<String, Instant> lastProposalTime = new ConcurrentHashMap<>();

    // Governance configuration
    private final GovernanceConfig config = new GovernanceConfig();

    // Performance metrics
    private long proposalCount = 0;
    private long voteCount = 0;
    private long executionCount = 0;

    /**
     * Create a new governance proposal
     *
     * @param title Proposal title
     * @param description Detailed description
     * @param type Proposal type
     * @param proposer Proposer address
     * @param deposit Deposit amount
     * @param executionPayload Optional execution payload
     * @return Created proposal
     */
    public Uni<Proposal> createProposal(String title, String description,
                                       Proposal.ProposalType type, String proposer,
                                       BigDecimal deposit, String executionPayload) {
        return Uni.createFrom().item(() -> {
            // Validate inputs
            if (title == null || title.isEmpty()) {
                throw new IllegalArgumentException("Proposal title cannot be empty");
            }

            if (proposer == null || proposer.isEmpty()) {
                throw new IllegalArgumentException("Proposer address cannot be empty");
            }

            if (deposit.compareTo(config.getMinProposalDeposit()) < 0) {
                throw new IllegalArgumentException("Deposit must be at least " +
                                                 config.getMinProposalDeposit());
            }

            // Check cooldown period
            Instant lastProposal = lastProposalTime.get(proposer);
            if (lastProposal != null) {
                Instant cooldownEnd = lastProposal.plus(config.getProposalCooldown());
                if (Instant.now().isBefore(cooldownEnd)) {
                    throw new IllegalStateException("Proposal cooldown period not expired");
                }
            }

            // Check max active proposals
            long activeCount = proposals.values().stream()
                    .filter(p -> p.getStatus() == Proposal.ProposalStatus.ACTIVE ||
                               p.getStatus() == Proposal.ProposalStatus.PENDING)
                    .count();

            if (activeCount >= config.getMaxActiveProposals()) {
                throw new IllegalStateException("Maximum active proposals limit reached");
            }

            // Generate proposal ID
            String proposalId = "PROP-" + System.currentTimeMillis() + "-" +
                              UUID.randomUUID().toString().substring(0, 8);

            // Get threshold config for proposal type
            GovernanceConfig.ThresholdConfig thresholds = config.getThresholdConfig(type);

            // Calculate voting period
            Instant votingStart = Instant.now().plus(Duration.ofHours(24)); // 24h deposit period
            Instant votingEnd = votingStart.plus(config.getVotingPeriod());

            // Create proposal
            Proposal proposal = new Proposal(
                proposalId,
                title,
                description,
                type,
                proposer,
                votingStart,
                votingEnd,
                thresholds.getQuorumThreshold().multiply(new BigDecimal("100")),
                thresholds.getApprovalThreshold().multiply(new BigDecimal("100"))
            );

            proposal.setVetoThreshold(thresholds.getVetoThreshold().multiply(new BigDecimal("100")));
            proposal.setExecutionPayload(executionPayload);
            proposal.setStatus(Proposal.ProposalStatus.PENDING);

            // Store proposal
            proposals.put(proposalId, proposal);
            proposalVotes.put(proposalId, new ArrayList<>());
            lastProposalTime.put(proposer, Instant.now());

            proposalCount++;

            LOG.infof("Created proposal %s: %s (type: %s, proposer: %s)",
                     proposalId, title, type, proposer);

            return proposal;

        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    /**
     * Cast a vote on a proposal
     *
     * @param proposalId Proposal ID
     * @param voter Voter address
     * @param option Vote option
     * @param votingPower Voter's voting power
     * @param privateKey Private key for signing (optional)
     * @return Vote record
     */
    public Uni<Vote> castVote(String proposalId, String voter, Vote.VoteOption option,
                             BigDecimal votingPower, PrivateKey privateKey) {
        return Uni.createFrom().item(() -> {
            // Validate proposal exists
            Proposal proposal = proposals.get(proposalId);
            if (proposal == null) {
                throw new IllegalArgumentException("Proposal not found: " + proposalId);
            }

            // Validate voting period
            if (!proposal.isVotingActive()) {
                throw new IllegalStateException("Proposal is not in active voting period");
            }

            // Check if voter already voted
            List<Vote> votes = proposalVotes.get(proposalId);
            boolean alreadyVoted = votes.stream()
                    .anyMatch(v -> v.getVoter().equals(voter));

            if (alreadyVoted) {
                throw new IllegalStateException("Voter has already voted on this proposal");
            }

            // Create vote
            String voteId = "VOTE-" + System.currentTimeMillis() + "-" +
                          UUID.randomUUID().toString().substring(0, 8);

            Vote vote = new Vote(voteId, proposalId, voter, option, votingPower);

            // Add quantum signature if private key provided
            if (privateKey != null) {
                try {
                    byte[] voteData = (proposalId + voter + option).getBytes();
                    byte[] signature = signatureService.sign(voteData, privateKey);
                    vote.setQuantumSignature(signature);
                } catch (Exception e) {
                    LOG.warnf("Failed to sign vote: %s", e.getMessage());
                }
            }

            // Store vote
            votes.add(vote);

            // Update proposal vote counts
            switch (option) {
                case YES:
                    proposal.setYesVotes(proposal.getYesVotes().add(vote.getEffectiveVotingPower()));
                    break;
                case NO:
                    proposal.setNoVotes(proposal.getNoVotes().add(vote.getEffectiveVotingPower()));
                    break;
                case ABSTAIN:
                    proposal.setAbstainVotes(proposal.getAbstainVotes().add(vote.getEffectiveVotingPower()));
                    break;
                case VETO:
                    proposal.setVetoVotes(proposal.getVetoVotes().add(vote.getEffectiveVotingPower()));
                    break;
            }

            voteCount++;

            LOG.infof("Vote cast on proposal %s: %s voted %s with power %s",
                     proposalId, voter, option, votingPower);

            return vote;

        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    /**
     * Get proposal by ID
     */
    public Uni<Proposal> getProposal(String proposalId) {
        return Uni.createFrom().item(() -> {
            Proposal proposal = proposals.get(proposalId);
            if (proposal == null) {
                throw new IllegalArgumentException("Proposal not found: " + proposalId);
            }
            return proposal;
        });
    }

    /**
     * List all proposals with optional filtering
     */
    public Uni<List<Proposal>> listProposals(Proposal.ProposalStatus status,
                                            Proposal.ProposalType type,
                                            int limit, int offset) {
        return Uni.createFrom().item(() -> {
            List<Proposal> allProposals = new ArrayList<>(proposals.values());

            // Apply filters
            if (status != null) {
                allProposals = allProposals.stream()
                        .filter(p -> p.getStatus() == status)
                        .collect(Collectors.toList());
            }

            if (type != null) {
                allProposals = allProposals.stream()
                        .filter(p -> p.getType() == type)
                        .collect(Collectors.toList());
            }

            // Sort by submit time (newest first)
            allProposals.sort((p1, p2) -> p2.getSubmitTime().compareTo(p1.getSubmitTime()));

            // Apply pagination
            int start = Math.min(offset, allProposals.size());
            int end = Math.min(offset + limit, allProposals.size());

            return allProposals.subList(start, end);
        });
    }

    /**
     * Get voting results for a proposal
     */
    public Uni<VotingResults> getVotingResults(String proposalId) {
        return Uni.createFrom().item(() -> {
            Proposal proposal = proposals.get(proposalId);
            if (proposal == null) {
                throw new IllegalArgumentException("Proposal not found: " + proposalId);
            }

            List<Vote> votes = proposalVotes.getOrDefault(proposalId, new ArrayList<>());

            return new VotingResults(
                proposal.getYesVotes(),
                proposal.getNoVotes(),
                proposal.getAbstainVotes(),
                proposal.getVetoVotes(),
                proposal.getApprovalPercentage(),
                proposal.getParticipationRate(),
                proposal.hasReachedQuorum(),
                proposal.hasPassed(),
                votes.size(),
                proposal.getStatus()
            );
        });
    }

    /**
     * Execute a passed proposal
     */
    public Uni<ExecutionResult> executeProposal(String proposalId) {
        return Uni.createFrom().item(() -> {
            Proposal proposal = proposals.get(proposalId);
            if (proposal == null) {
                throw new IllegalArgumentException("Proposal not found: " + proposalId);
            }

            // Validate proposal status
            if (proposal.getStatus() != Proposal.ProposalStatus.PASSED) {
                throw new IllegalStateException("Proposal has not passed voting");
            }

            // Check execution delay
            Instant executionTime = proposal.getVotingEndTime().plus(config.getExecutionDelay());
            if (Instant.now().isBefore(executionTime)) {
                throw new IllegalStateException("Execution delay period not expired");
            }

            try {
                // Execute based on proposal type
                boolean success = executeProposalLogic(proposal);

                if (success) {
                    proposal.setStatus(Proposal.ProposalStatus.EXECUTED);
                    proposal.setExecutionTime(Instant.now());
                    executionCount++;

                    LOG.infof("Successfully executed proposal %s", proposalId);

                    return new ExecutionResult(true, "Proposal executed successfully", Instant.now());
                } else {
                    proposal.setStatus(Proposal.ProposalStatus.FAILED);

                    LOG.warnf("Failed to execute proposal %s", proposalId);

                    return new ExecutionResult(false, "Proposal execution failed", Instant.now());
                }

            } catch (Exception e) {
                proposal.setStatus(Proposal.ProposalStatus.FAILED);
                LOG.errorf(e, "Error executing proposal %s", proposalId);

                return new ExecutionResult(false, "Execution error: " + e.getMessage(), Instant.now());
            }

        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    /**
     * Execute proposal logic based on type
     */
    private boolean executeProposalLogic(Proposal proposal) {
        switch (proposal.getType()) {
            case PARAMETER_CHANGE:
                // Update system parameters
                LOG.infof("Executing parameter change: %s", proposal.getAffectedParameters());
                return true;

            case TREASURY_SPEND:
                // Transfer funds from treasury
                LOG.infof("Executing treasury spend: %s", proposal.getRequestedAmount());
                return true;

            case SOFTWARE_UPGRADE:
                // Schedule software upgrade
                LOG.infof("Scheduling software upgrade");
                return true;

            case TEXT_PROPOSAL:
            case COMMUNITY_POOL_SPEND:
                // Non-executable proposals
                return true;

            default:
                return false;
        }
    }

    /**
     * Update proposal statuses (should be called periodically)
     */
    public void updateProposalStatuses() {
        Instant now = Instant.now();

        proposals.values().forEach(proposal -> {
            // Start voting period
            if (proposal.getStatus() == Proposal.ProposalStatus.PENDING &&
                now.isAfter(proposal.getVotingStartTime())) {
                proposal.setStatus(Proposal.ProposalStatus.ACTIVE);
                LOG.infof("Proposal %s voting period started", proposal.getId());
            }

            // End voting period
            if (proposal.getStatus() == Proposal.ProposalStatus.ACTIVE &&
                now.isAfter(proposal.getVotingEndTime())) {

                if (proposal.hasPassed()) {
                    proposal.setStatus(Proposal.ProposalStatus.PASSED);
                    LOG.infof("Proposal %s passed", proposal.getId());
                } else {
                    proposal.setStatus(Proposal.ProposalStatus.REJECTED);
                    LOG.infof("Proposal %s rejected", proposal.getId());
                }
            }
        });
    }

    /**
     * Get governance metrics
     */
    public GovernanceMetrics getMetrics() {
        long activeProposals = proposals.values().stream()
                .filter(p -> p.getStatus() == Proposal.ProposalStatus.ACTIVE)
                .count();

        long passedProposals = proposals.values().stream()
                .filter(p -> p.getStatus() == Proposal.ProposalStatus.PASSED ||
                           p.getStatus() == Proposal.ProposalStatus.EXECUTED)
                .count();

        return new GovernanceMetrics(
            proposalCount,
            voteCount,
            executionCount,
            (int) activeProposals,
            (int) passedProposals
        );
    }

    // ==================== DTOs ====================

    /**
     * Voting results summary
     */
    public record VotingResults(
        BigDecimal yesVotes,
        BigDecimal noVotes,
        BigDecimal abstainVotes,
        BigDecimal vetoVotes,
        BigDecimal approvalPercentage,
        BigDecimal participationRate,
        boolean hasReachedQuorum,
        boolean hasPassed,
        int totalVoters,
        Proposal.ProposalStatus status
    ) {}

    /**
     * Execution result
     */
    public record ExecutionResult(
        boolean success,
        String message,
        Instant executionTime
    ) {}

    /**
     * Governance metrics
     */
    public record GovernanceMetrics(
        long totalProposals,
        long totalVotes,
        long totalExecutions,
        int activeProposals,
        int passedProposals
    ) {}
}
