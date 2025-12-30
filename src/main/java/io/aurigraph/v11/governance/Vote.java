package io.aurigraph.v11.governance;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

/**
 * Governance Vote Model
 *
 * Represents a vote cast on a governance proposal.
 * Supports token-weighted voting with quantum-resistant signatures.
 *
 * @author SCA (Security & Cryptography Agent)
 * @version 11.0.0
 * @since Sprint 8 - Governance & Quantum Signing
 */
public class Vote {

    /**
     * Vote options available to voters
     */
    public enum VoteOption {
        YES,        // Vote in favor of the proposal
        NO,         // Vote against the proposal
        ABSTAIN,    // Abstain from voting (counts toward quorum)
        VETO        // Strong veto (special threshold)
    }

    // Core vote fields
    private String voteId;
    private String proposalId;
    private String voter;
    private VoteOption option;
    private BigDecimal votingPower;

    // Timing
    private Instant voteTime;
    private Instant lockExpiry;  // For time-locked voting

    // Security
    private byte[] quantumSignature;  // Dilithium signature
    private String signatureKeyId;    // Key used for signing
    private boolean isMultiSig;       // Multi-signature vote flag
    private String[] coSigners;       // Co-signers for multi-sig votes

    // Metadata
    private String justification;     // Optional vote justification
    private boolean isTimeLocked;     // Time-locked vote
    private BigDecimal lockedAmount;  // Amount locked for voting

    /**
     * Default constructor
     */
    public Vote() {
        this.votingPower = BigDecimal.ZERO;
        this.lockedAmount = BigDecimal.ZERO;
        this.isTimeLocked = false;
        this.isMultiSig = false;
    }

    /**
     * Standard constructor for simple vote
     */
    public Vote(String voteId, String proposalId, String voter, VoteOption option, BigDecimal votingPower) {
        this();
        this.voteId = voteId;
        this.proposalId = proposalId;
        this.voter = voter;
        this.option = option;
        this.votingPower = votingPower;
        this.voteTime = Instant.now();
    }

    /**
     * Constructor for time-locked vote
     */
    public Vote(String voteId, String proposalId, String voter, VoteOption option,
                BigDecimal votingPower, Instant lockExpiry, BigDecimal lockedAmount) {
        this(voteId, proposalId, voter, option, votingPower);
        this.isTimeLocked = true;
        this.lockExpiry = lockExpiry;
        this.lockedAmount = lockedAmount;
    }

    /**
     * Check if vote is still valid (not expired for time-locked votes)
     */
    public boolean isValid() {
        if (!isTimeLocked) {
            return true;
        }
        return lockExpiry == null || Instant.now().isBefore(lockExpiry);
    }

    /**
     * Get effective voting power (may be multiplied for time-locked votes)
     */
    public BigDecimal getEffectiveVotingPower() {
        if (!isTimeLocked || !isValid()) {
            return votingPower;
        }

        // Time-locked votes get a 1.5x multiplier
        return votingPower.multiply(new BigDecimal("1.5"));
    }

    /**
     * Check if this is a multi-signature vote
     */
    public boolean hasMultiSignature() {
        return isMultiSig && coSigners != null && coSigners.length > 0;
    }

    // Getters and Setters

    public String getVoteId() {
        return voteId;
    }

    public void setVoteId(String voteId) {
        this.voteId = voteId;
    }

    public String getProposalId() {
        return proposalId;
    }

    public void setProposalId(String proposalId) {
        this.proposalId = proposalId;
    }

    public String getVoter() {
        return voter;
    }

    public void setVoter(String voter) {
        this.voter = voter;
    }

    public VoteOption getOption() {
        return option;
    }

    public void setOption(VoteOption option) {
        this.option = option;
    }

    public BigDecimal getVotingPower() {
        return votingPower;
    }

    public void setVotingPower(BigDecimal votingPower) {
        this.votingPower = votingPower;
    }

    public Instant getVoteTime() {
        return voteTime;
    }

    public void setVoteTime(Instant voteTime) {
        this.voteTime = voteTime;
    }

    public Instant getLockExpiry() {
        return lockExpiry;
    }

    public void setLockExpiry(Instant lockExpiry) {
        this.lockExpiry = lockExpiry;
    }

    public byte[] getQuantumSignature() {
        return quantumSignature;
    }

    public void setQuantumSignature(byte[] quantumSignature) {
        this.quantumSignature = quantumSignature;
    }

    public String getSignatureKeyId() {
        return signatureKeyId;
    }

    public void setSignatureKeyId(String signatureKeyId) {
        this.signatureKeyId = signatureKeyId;
    }

    public boolean isMultiSig() {
        return isMultiSig;
    }

    public void setMultiSig(boolean multiSig) {
        isMultiSig = multiSig;
    }

    public String[] getCoSigners() {
        return coSigners;
    }

    public void setCoSigners(String[] coSigners) {
        this.coSigners = coSigners;
    }

    public String getJustification() {
        return justification;
    }

    public void setJustification(String justification) {
        this.justification = justification;
    }

    public boolean isTimeLocked() {
        return isTimeLocked;
    }

    public void setTimeLocked(boolean timeLocked) {
        isTimeLocked = timeLocked;
    }

    public BigDecimal getLockedAmount() {
        return lockedAmount;
    }

    public void setLockedAmount(BigDecimal lockedAmount) {
        this.lockedAmount = lockedAmount;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Vote vote = (Vote) o;
        return Objects.equals(voteId, vote.voteId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(voteId);
    }

    @Override
    public String toString() {
        return "Vote{" +
               "voteId='" + voteId + '\'' +
               ", proposalId='" + proposalId + '\'' +
               ", voter='" + voter + '\'' +
               ", option=" + option +
               ", votingPower=" + votingPower +
               ", effectivePower=" + getEffectiveVotingPower() +
               ", timeLocked=" + isTimeLocked +
               ", multiSig=" + isMultiSig +
               '}';
    }
}
