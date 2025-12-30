package io.aurigraph.v11.token.secondary;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Consensus Result Data Class
 *
 * Contains the result of consensus calculation for an approval request.
 * Provides detailed metrics about vote distribution and Byzantine FT status.
 *
 * Byzantine Fault Tolerance:
 * - Consensus requires >2/3 of active voters (non-abstaining)
 * - impossibleToReach indicates whether consensus is still possible
 * - Used to determine early approval/rejection before voting window closes
 *
 * @version 12.0.0
 * @since December 23, 2025
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConsensusResult {

    /**
     * Whether consensus has been reached.
     * True if >2/3 of active voters approve or reject.
     */
    private boolean consensusReached;

    /**
     * Whether consensus is impossible to reach with remaining votes.
     * True if mathematical impossibility exists (e.g., too many rejections).
     * Used for early termination without waiting for voting window.
     */
    private boolean impossibleToReach;

    /**
     * Number of YES votes received.
     */
    private int approvalCount;

    /**
     * Total number of voters (including abstainers).
     * Consensus requires >2/3 of (totalVoters - abstainers).
     */
    private int totalVoters;

    /**
     * Percentage of YES votes among active voters.
     * Calculated as: (approvalCount / (totalVoters - abstainers)) * 100
     * Range: 0.0 to 100.0
     */
    private double percentage;

    /**
     * Number of NO votes received.
     * Derived as: activeVoters - approvalCount - abstainCount
     */
    private int rejectionCount;

    /**
     * Number of ABSTAIN votes received.
     */
    private int abstainCount;

    /**
     * Check if consensus reached for approval.
     *
     * @return true if consensusReached and percentage > 66.67
     */
    public boolean isApproved() {
        return consensusReached && percentage > 66.67;
    }

    /**
     * Check if consensus reached for rejection.
     *
     * @return true if consensusReached and percentage < 33.33
     */
    public boolean isRejected() {
        return consensusReached && percentage < 33.33;
    }

    /**
     * Get active voter count (excluding abstainers).
     *
     * @return totalVoters - abstainCount
     */
    public int getActiveVoterCount() {
        return totalVoters - abstainCount;
    }

    /**
     * Get minimum votes required for approval.
     * Requires >2/3 majority, so (activeVoters / 3) + 1
     *
     * @return minimum votes needed for approval
     */
    public int getMinimumForApproval() {
        int activeVoters = getActiveVoterCount();
        return (activeVoters / 3) + 1;
    }

    /**
     * Get remaining votes needed for approval.
     *
     * @return votes still needed to reach approval threshold
     */
    public int getVotesNeededForApproval() {
        int minNeeded = getMinimumForApproval();
        return Math.max(0, minNeeded - approvalCount);
    }

    /**
     * Get consensus threshold percentage.
     *
     * @return 66.67 (represents >2/3 majority)
     */
    public double getConsensusThreshold() {
        return 66.67;
    }

    @Override
    public String toString() {
        return String.format(
            "ConsensusResult[reached=%b, impossible=%b, approval=%d, rejection=%d, " +
            "abstain=%d, total=%d, percentage=%.2f%%]",
            consensusReached, impossibleToReach, approvalCount, rejectionCount,
            abstainCount, totalVoters, percentage
        );
    }
}
