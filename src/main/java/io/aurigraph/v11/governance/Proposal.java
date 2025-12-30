package io.aurigraph.v11.governance;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Governance Proposal Model
 *
 * Represents a proposal in the Aurigraph governance system.
 * Supports various proposal types including parameter changes, treasury spending,
 * software upgrades, and text proposals.
 *
 * @author SCA (Security & Cryptography Agent)
 * @version 11.0.0
 * @since Sprint 8 - Governance & Quantum Signing
 */
public class Proposal {

    /**
     * Proposal types supported by the governance system
     */
    public enum ProposalType {
        PARAMETER_CHANGE,    // Change blockchain parameters
        TEXT_PROPOSAL,       // Non-binding text proposal
        TREASURY_SPEND,      // Spend from treasury
        SOFTWARE_UPGRADE,    // Software/protocol upgrade
        COMMUNITY_POOL_SPEND // Community pool allocation
    }

    /**
     * Proposal status lifecycle
     */
    public enum ProposalStatus {
        PENDING,    // Awaiting voting period to start
        ACTIVE,     // Currently in voting period
        PASSED,     // Proposal passed and awaiting execution
        REJECTED,   // Proposal did not meet quorum or approval threshold
        EXECUTED,   // Proposal has been executed
        FAILED,     // Proposal execution failed
        EXPIRED     // Proposal expired without reaching quorum
    }

    // Core proposal fields
    private String id;
    private String title;
    private String description;
    private ProposalType type;
    private ProposalStatus status;
    private String proposer;

    // Timing fields
    private Instant submitTime;
    private Instant votingStartTime;
    private Instant votingEndTime;
    private Instant executionTime;

    // Voting results
    private BigDecimal yesVotes;
    private BigDecimal noVotes;
    private BigDecimal abstainVotes;
    private BigDecimal vetoVotes;
    private BigDecimal totalVotingPower;

    // Thresholds
    private BigDecimal quorumThreshold;      // Minimum participation required
    private BigDecimal approvalThreshold;    // Minimum yes% required
    private BigDecimal vetoThreshold;        // Maximum veto% allowed

    // Execution details
    private String executionPayload;         // JSON payload for execution
    private List<String> affectedParameters; // Parameters to be changed
    private BigDecimal requestedAmount;      // For treasury/spend proposals

    // Metadata
    private List<String> tags;
    private String ipfsHash;                 // Link to detailed proposal document
    private byte[] quantumSignature;         // Dilithium signature from proposer

    /**
     * Default constructor
     */
    public Proposal() {
        this.yesVotes = BigDecimal.ZERO;
        this.noVotes = BigDecimal.ZERO;
        this.abstainVotes = BigDecimal.ZERO;
        this.vetoVotes = BigDecimal.ZERO;
        this.totalVotingPower = BigDecimal.ZERO;
        this.affectedParameters = new ArrayList<>();
        this.tags = new ArrayList<>();
    }

    /**
     * Full constructor
     */
    public Proposal(String id, String title, String description, ProposalType type,
                   String proposer, Instant votingStartTime, Instant votingEndTime,
                   BigDecimal quorumThreshold, BigDecimal approvalThreshold) {
        this();
        this.id = id;
        this.title = title;
        this.description = description;
        this.type = type;
        this.proposer = proposer;
        this.status = ProposalStatus.PENDING;
        this.submitTime = Instant.now();
        this.votingStartTime = votingStartTime;
        this.votingEndTime = votingEndTime;
        this.quorumThreshold = quorumThreshold;
        this.approvalThreshold = approvalThreshold;
        this.vetoThreshold = new BigDecimal("0.334"); // Default 33.4% veto threshold
    }

    /**
     * Calculate current approval percentage
     */
    public BigDecimal getApprovalPercentage() {
        BigDecimal totalVotes = yesVotes.add(noVotes).add(abstainVotes).add(vetoVotes);
        if (totalVotes.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return yesVotes.divide(totalVotes, 4, BigDecimal.ROUND_HALF_UP)
                      .multiply(new BigDecimal("100"));
    }

    /**
     * Calculate participation rate
     */
    public BigDecimal getParticipationRate() {
        if (totalVotingPower.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal totalVotes = yesVotes.add(noVotes).add(abstainVotes).add(vetoVotes);
        return totalVotes.divide(totalVotingPower, 4, BigDecimal.ROUND_HALF_UP)
                        .multiply(new BigDecimal("100"));
    }

    /**
     * Check if proposal has reached quorum
     */
    public boolean hasReachedQuorum() {
        return getParticipationRate().compareTo(quorumThreshold) >= 0;
    }

    /**
     * Check if proposal is currently active for voting
     */
    public boolean isVotingActive() {
        if (status != ProposalStatus.ACTIVE) {
            return false;
        }
        Instant now = Instant.now();
        return now.isAfter(votingStartTime) && now.isBefore(votingEndTime);
    }

    /**
     * Check if proposal has passed all thresholds
     */
    public boolean hasPassed() {
        if (!hasReachedQuorum()) {
            return false;
        }

        // Check approval threshold
        if (getApprovalPercentage().compareTo(approvalThreshold) < 0) {
            return false;
        }

        // Check veto threshold
        BigDecimal totalVotes = yesVotes.add(noVotes).add(abstainVotes).add(vetoVotes);
        if (totalVotes.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal vetoPercentage = vetoVotes.divide(totalVotes, 4, BigDecimal.ROUND_HALF_UP)
                                                 .multiply(new BigDecimal("100"));
            if (vetoPercentage.compareTo(vetoThreshold) > 0) {
                return false;
            }
        }

        return true;
    }

    // Getters and Setters

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public ProposalType getType() {
        return type;
    }

    public void setType(ProposalType type) {
        this.type = type;
    }

    public ProposalStatus getStatus() {
        return status;
    }

    public void setStatus(ProposalStatus status) {
        this.status = status;
    }

    public String getProposer() {
        return proposer;
    }

    public void setProposer(String proposer) {
        this.proposer = proposer;
    }

    public Instant getSubmitTime() {
        return submitTime;
    }

    public void setSubmitTime(Instant submitTime) {
        this.submitTime = submitTime;
    }

    public Instant getVotingStartTime() {
        return votingStartTime;
    }

    public void setVotingStartTime(Instant votingStartTime) {
        this.votingStartTime = votingStartTime;
    }

    public Instant getVotingEndTime() {
        return votingEndTime;
    }

    public void setVotingEndTime(Instant votingEndTime) {
        this.votingEndTime = votingEndTime;
    }

    public Instant getExecutionTime() {
        return executionTime;
    }

    public void setExecutionTime(Instant executionTime) {
        this.executionTime = executionTime;
    }

    public BigDecimal getYesVotes() {
        return yesVotes;
    }

    public void setYesVotes(BigDecimal yesVotes) {
        this.yesVotes = yesVotes;
    }

    public BigDecimal getNoVotes() {
        return noVotes;
    }

    public void setNoVotes(BigDecimal noVotes) {
        this.noVotes = noVotes;
    }

    public BigDecimal getAbstainVotes() {
        return abstainVotes;
    }

    public void setAbstainVotes(BigDecimal abstainVotes) {
        this.abstainVotes = abstainVotes;
    }

    public BigDecimal getVetoVotes() {
        return vetoVotes;
    }

    public void setVetoVotes(BigDecimal vetoVotes) {
        this.vetoVotes = vetoVotes;
    }

    public BigDecimal getTotalVotingPower() {
        return totalVotingPower;
    }

    public void setTotalVotingPower(BigDecimal totalVotingPower) {
        this.totalVotingPower = totalVotingPower;
    }

    public BigDecimal getQuorumThreshold() {
        return quorumThreshold;
    }

    public void setQuorumThreshold(BigDecimal quorumThreshold) {
        this.quorumThreshold = quorumThreshold;
    }

    public BigDecimal getApprovalThreshold() {
        return approvalThreshold;
    }

    public void setApprovalThreshold(BigDecimal approvalThreshold) {
        this.approvalThreshold = approvalThreshold;
    }

    public BigDecimal getVetoThreshold() {
        return vetoThreshold;
    }

    public void setVetoThreshold(BigDecimal vetoThreshold) {
        this.vetoThreshold = vetoThreshold;
    }

    public String getExecutionPayload() {
        return executionPayload;
    }

    public void setExecutionPayload(String executionPayload) {
        this.executionPayload = executionPayload;
    }

    public List<String> getAffectedParameters() {
        return affectedParameters;
    }

    public void setAffectedParameters(List<String> affectedParameters) {
        this.affectedParameters = affectedParameters;
    }

    public BigDecimal getRequestedAmount() {
        return requestedAmount;
    }

    public void setRequestedAmount(BigDecimal requestedAmount) {
        this.requestedAmount = requestedAmount;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public String getIpfsHash() {
        return ipfsHash;
    }

    public void setIpfsHash(String ipfsHash) {
        this.ipfsHash = ipfsHash;
    }

    public byte[] getQuantumSignature() {
        return quantumSignature;
    }

    public void setQuantumSignature(byte[] quantumSignature) {
        this.quantumSignature = quantumSignature;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Proposal proposal = (Proposal) o;
        return Objects.equals(id, proposal.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Proposal{" +
               "id='" + id + '\'' +
               ", title='" + title + '\'' +
               ", type=" + type +
               ", status=" + status +
               ", proposer='" + proposer + '\'' +
               ", approval=" + getApprovalPercentage() + "%" +
               ", participation=" + getParticipationRate() + "%" +
               '}';
    }
}
