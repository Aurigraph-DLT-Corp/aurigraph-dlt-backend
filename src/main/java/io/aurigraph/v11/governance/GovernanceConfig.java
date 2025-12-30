package io.aurigraph.v11.governance;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Governance Configuration Model
 *
 * Defines the governance parameters and rules for the Aurigraph platform.
 * Includes voting thresholds, timing parameters, and proposal requirements.
 *
 * @author SCA (Security & Cryptography Agent)
 * @version 11.0.0
 * @since Sprint 8 - Governance & Quantum Signing
 */
public class GovernanceConfig {

    // Voting thresholds (percentages as decimals, e.g., 0.50 = 50%)
    private BigDecimal defaultQuorumThreshold;      // Default: 40%
    private BigDecimal defaultApprovalThreshold;    // Default: 50%
    private BigDecimal defaultVetoThreshold;        // Default: 33.4%

    // Proposal-type specific thresholds
    private Map<Proposal.ProposalType, ThresholdConfig> typeSpecificThresholds;

    // Timing parameters
    private Duration votingPeriod;                  // Default: 7 days
    private Duration executionDelay;                // Default: 2 days
    private Duration proposalDeposit;               // Minimum deposit to create proposal

    // Proposal requirements
    private BigDecimal minProposalDeposit;          // Minimum AUR tokens to deposit
    private int maxActiveProposals;                 // Max concurrent active proposals
    private Duration proposalCooldown;              // Time between proposals from same address

    // Token locking parameters
    private BigDecimal timeLockedMultiplier;        // Voting power multiplier for locked tokens
    private Duration minLockDuration;               // Minimum lock duration for bonus
    private Duration maxLockDuration;               // Maximum lock duration

    // Multi-signature requirements
    private Map<String, Integer> roleBasedThresholds; // Role -> required signatures

    // Execution parameters
    private boolean autoExecuteEnabled;             // Auto-execute passed proposals
    private int maxExecutionRetries;                // Max retries for failed executions

    /**
     * Default constructor with standard values
     */
    public GovernanceConfig() {
        // Default voting thresholds
        this.defaultQuorumThreshold = new BigDecimal("0.40");      // 40%
        this.defaultApprovalThreshold = new BigDecimal("0.50");    // 50%
        this.defaultVetoThreshold = new BigDecimal("0.334");       // 33.4%

        // Default timing
        this.votingPeriod = Duration.ofDays(7);
        this.executionDelay = Duration.ofDays(2);

        // Default proposal requirements
        this.minProposalDeposit = new BigDecimal("1000");          // 1000 AUR tokens
        this.maxActiveProposals = 20;
        this.proposalCooldown = Duration.ofHours(24);

        // Default token locking
        this.timeLockedMultiplier = new BigDecimal("1.5");         // 1.5x voting power
        this.minLockDuration = Duration.ofDays(30);
        this.maxLockDuration = Duration.ofDays(365);

        // Default execution
        this.autoExecuteEnabled = true;
        this.maxExecutionRetries = 3;

        // Initialize maps
        this.typeSpecificThresholds = new HashMap<>();
        this.roleBasedThresholds = new HashMap<>();

        initializeDefaultTypeThresholds();
        initializeDefaultRoleThresholds();
    }

    /**
     * Initialize default thresholds for each proposal type
     */
    private void initializeDefaultTypeThresholds() {
        // Parameter changes require higher approval
        typeSpecificThresholds.put(
            Proposal.ProposalType.PARAMETER_CHANGE,
            new ThresholdConfig(
                new BigDecimal("0.50"),  // 50% quorum
                new BigDecimal("0.66"),  // 66% approval
                new BigDecimal("0.334")  // 33.4% veto
            )
        );

        // Software upgrades require high consensus
        typeSpecificThresholds.put(
            Proposal.ProposalType.SOFTWARE_UPGRADE,
            new ThresholdConfig(
                new BigDecimal("0.60"),  // 60% quorum
                new BigDecimal("0.75"),  // 75% approval
                new BigDecimal("0.25")   // 25% veto
            )
        );

        // Treasury spending requires moderate approval
        typeSpecificThresholds.put(
            Proposal.ProposalType.TREASURY_SPEND,
            new ThresholdConfig(
                new BigDecimal("0.45"),  // 45% quorum
                new BigDecimal("0.55"),  // 55% approval
                new BigDecimal("0.334")  // 33.4% veto
            )
        );

        // Text proposals have lower requirements
        typeSpecificThresholds.put(
            Proposal.ProposalType.TEXT_PROPOSAL,
            new ThresholdConfig(
                new BigDecimal("0.30"),  // 30% quorum
                new BigDecimal("0.50"),  // 50% approval
                new BigDecimal("0.40")   // 40% veto
            )
        );
    }

    /**
     * Initialize default role-based multi-sig thresholds
     */
    private void initializeDefaultRoleThresholds() {
        roleBasedThresholds.put("STANDARD", 1);        // 1-of-n
        roleBasedThresholds.put("TREASURER", 2);       // 2-of-n
        roleBasedThresholds.put("EXECUTIVE", 2);       // 2-of-3
        roleBasedThresholds.put("CRITICAL", 3);        // 3-of-5
        roleBasedThresholds.put("SECURITY", 5);        // 5-of-7
    }

    /**
     * Get threshold configuration for a specific proposal type
     */
    public ThresholdConfig getThresholdConfig(Proposal.ProposalType type) {
        return typeSpecificThresholds.getOrDefault(type,
            new ThresholdConfig(defaultQuorumThreshold, defaultApprovalThreshold, defaultVetoThreshold)
        );
    }

    /**
     * Get required signatures for a specific role
     */
    public int getRequiredSignatures(String role) {
        return roleBasedThresholds.getOrDefault(role.toUpperCase(), 1);
    }

    /**
     * Calculate effective voting power with time-lock multiplier
     */
    public BigDecimal calculateEffectiveVotingPower(BigDecimal basePower, Duration lockDuration) {
        if (lockDuration == null || lockDuration.compareTo(minLockDuration) < 0) {
            return basePower;
        }

        // Cap at max lock duration
        if (lockDuration.compareTo(maxLockDuration) > 0) {
            lockDuration = maxLockDuration;
        }

        return basePower.multiply(timeLockedMultiplier);
    }

    // Getters and Setters

    public BigDecimal getDefaultQuorumThreshold() {
        return defaultQuorumThreshold;
    }

    public void setDefaultQuorumThreshold(BigDecimal defaultQuorumThreshold) {
        this.defaultQuorumThreshold = defaultQuorumThreshold;
    }

    public BigDecimal getDefaultApprovalThreshold() {
        return defaultApprovalThreshold;
    }

    public void setDefaultApprovalThreshold(BigDecimal defaultApprovalThreshold) {
        this.defaultApprovalThreshold = defaultApprovalThreshold;
    }

    public BigDecimal getDefaultVetoThreshold() {
        return defaultVetoThreshold;
    }

    public void setDefaultVetoThreshold(BigDecimal defaultVetoThreshold) {
        this.defaultVetoThreshold = defaultVetoThreshold;
    }

    public Map<Proposal.ProposalType, ThresholdConfig> getTypeSpecificThresholds() {
        return typeSpecificThresholds;
    }

    public void setTypeSpecificThresholds(Map<Proposal.ProposalType, ThresholdConfig> typeSpecificThresholds) {
        this.typeSpecificThresholds = typeSpecificThresholds;
    }

    public Duration getVotingPeriod() {
        return votingPeriod;
    }

    public void setVotingPeriod(Duration votingPeriod) {
        this.votingPeriod = votingPeriod;
    }

    public Duration getExecutionDelay() {
        return executionDelay;
    }

    public void setExecutionDelay(Duration executionDelay) {
        this.executionDelay = executionDelay;
    }

    public BigDecimal getMinProposalDeposit() {
        return minProposalDeposit;
    }

    public void setMinProposalDeposit(BigDecimal minProposalDeposit) {
        this.minProposalDeposit = minProposalDeposit;
    }

    public int getMaxActiveProposals() {
        return maxActiveProposals;
    }

    public void setMaxActiveProposals(int maxActiveProposals) {
        this.maxActiveProposals = maxActiveProposals;
    }

    public Duration getProposalCooldown() {
        return proposalCooldown;
    }

    public void setProposalCooldown(Duration proposalCooldown) {
        this.proposalCooldown = proposalCooldown;
    }

    public BigDecimal getTimeLockedMultiplier() {
        return timeLockedMultiplier;
    }

    public void setTimeLockedMultiplier(BigDecimal timeLockedMultiplier) {
        this.timeLockedMultiplier = timeLockedMultiplier;
    }

    public Duration getMinLockDuration() {
        return minLockDuration;
    }

    public void setMinLockDuration(Duration minLockDuration) {
        this.minLockDuration = minLockDuration;
    }

    public Duration getMaxLockDuration() {
        return maxLockDuration;
    }

    public void setMaxLockDuration(Duration maxLockDuration) {
        this.maxLockDuration = maxLockDuration;
    }

    public Map<String, Integer> getRoleBasedThresholds() {
        return roleBasedThresholds;
    }

    public void setRoleBasedThresholds(Map<String, Integer> roleBasedThresholds) {
        this.roleBasedThresholds = roleBasedThresholds;
    }

    public boolean isAutoExecuteEnabled() {
        return autoExecuteEnabled;
    }

    public void setAutoExecuteEnabled(boolean autoExecuteEnabled) {
        this.autoExecuteEnabled = autoExecuteEnabled;
    }

    public int getMaxExecutionRetries() {
        return maxExecutionRetries;
    }

    public void setMaxExecutionRetries(int maxExecutionRetries) {
        this.maxExecutionRetries = maxExecutionRetries;
    }

    @Override
    public String toString() {
        return "GovernanceConfig{" +
               "quorum=" + defaultQuorumThreshold +
               ", approval=" + defaultApprovalThreshold +
               ", veto=" + defaultVetoThreshold +
               ", votingPeriod=" + votingPeriod +
               ", minDeposit=" + minProposalDeposit +
               '}';
    }

    /**
     * Threshold configuration for specific proposal types
     */
    public static class ThresholdConfig {
        private final BigDecimal quorumThreshold;
        private final BigDecimal approvalThreshold;
        private final BigDecimal vetoThreshold;

        public ThresholdConfig(BigDecimal quorumThreshold, BigDecimal approvalThreshold, BigDecimal vetoThreshold) {
            this.quorumThreshold = quorumThreshold;
            this.approvalThreshold = approvalThreshold;
            this.vetoThreshold = vetoThreshold;
        }

        public BigDecimal getQuorumThreshold() {
            return quorumThreshold;
        }

        public BigDecimal getApprovalThreshold() {
            return approvalThreshold;
        }

        public BigDecimal getVetoThreshold() {
            return vetoThreshold;
        }

        @Override
        public String toString() {
            return "ThresholdConfig{" +
                   "quorum=" + quorumThreshold +
                   ", approval=" + approvalThreshold +
                   ", veto=" + vetoThreshold +
                   '}';
        }
    }
}
