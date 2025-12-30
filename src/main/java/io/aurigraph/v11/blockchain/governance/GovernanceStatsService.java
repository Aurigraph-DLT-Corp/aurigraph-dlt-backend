package io.aurigraph.v11.blockchain.governance;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * Governance Statistics Service
 *
 * Provides comprehensive governance and voting statistics for the Aurigraph V11 platform.
 * Tracks proposals, voting activity, participation rates, and governance analytics.
 *
 * Key Features:
 * - Real-time governance statistics
 * - Voting participation tracking
 * - Top voters identification
 * - Recent activity monitoring
 * - Proposal type breakdown
 * - Historical trend analysis
 *
 * @author Backend Development Agent (BDA) - Governance Specialist
 * @version 11.0.0
 * @since Phase 2 - Sprint 15
 */
@ApplicationScoped
public class GovernanceStatsService {

    private static final Logger LOG = Logger.getLogger(GovernanceStatsService.class);

    // Simulated governance data (in production, this would come from a database)
    private final GovernanceData governanceData;

    public GovernanceStatsService() {
        this.governanceData = new GovernanceData();
        LOG.info("GovernanceStatsService initialized");
    }

    /**
     * Get comprehensive governance statistics
     *
     * @return GovernanceStats containing all governance metrics
     */
    public Uni<GovernanceStats> getGovernanceStatistics() {
        return Uni.createFrom().item(() -> {
            LOG.info("Generating governance statistics");

            // Calculate current statistics
            int totalProposals = governanceData.getTotalProposals();
            int activeProposals = governanceData.getActiveProposals();
            int passedProposals = governanceData.getPassedProposals();
            int rejectedProposals = governanceData.getRejectedProposals();
            long totalVotes = governanceData.getTotalVotes();

            // Calculate participation metrics
            double participationRate = governanceData.calculateParticipationRate();
            double averageTurnout = governanceData.calculateAverageTurnout();

            // Get top voters
            List<TopVoter> topVoters = governanceData.getTopVoters(10);

            // Get recent activity
            List<RecentActivity> recentActivity = governanceData.getRecentActivity(20);

            // Get proposals breakdown by type
            Map<String, Integer> proposalsByType = governanceData.getProposalsByType();

            // Get historical trends
            List<GovernanceTrend> historicalTrends = governanceData.getHistoricalTrends(30);

            GovernanceStats stats = new GovernanceStats(
                totalProposals,
                activeProposals,
                passedProposals,
                rejectedProposals,
                totalVotes,
                participationRate,
                averageTurnout,
                topVoters,
                recentActivity,
                proposalsByType,
                historicalTrends,
                Instant.now().toString()
            );

            LOG.infof("Generated governance stats: %d total proposals, %.1f%% participation",
                     totalProposals, participationRate);

            return stats;
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    /**
     * Get governance statistics for a specific time period
     *
     * @param days Number of days to look back
     * @return GovernanceStats for the specified period
     */
    public Uni<GovernanceStats> getGovernanceStatisticsByPeriod(int days) {
        return Uni.createFrom().item(() -> {
            LOG.infof("Generating governance statistics for last %d days", days);

            // Filter data by time period
            Instant cutoff = Instant.now().minus(days, ChronoUnit.DAYS);

            // Calculate statistics for the period
            int totalProposals = governanceData.getProposalsSince(cutoff);
            int activeProposals = governanceData.getActiveProposalsSince(cutoff);
            int passedProposals = governanceData.getPassedProposalsSince(cutoff);
            int rejectedProposals = governanceData.getRejectedProposalsSince(cutoff);
            long totalVotes = governanceData.getVotesSince(cutoff);

            double participationRate = governanceData.calculateParticipationRateSince(cutoff);
            double averageTurnout = governanceData.calculateAverageTurnoutSince(cutoff);

            List<TopVoter> topVoters = governanceData.getTopVotersSince(cutoff, 10);
            List<RecentActivity> recentActivity = governanceData.getRecentActivitySince(cutoff, 20);
            Map<String, Integer> proposalsByType = governanceData.getProposalsByTypeSince(cutoff);
            List<GovernanceTrend> historicalTrends = governanceData.getHistoricalTrends(days);

            return new GovernanceStats(
                totalProposals,
                activeProposals,
                passedProposals,
                rejectedProposals,
                totalVotes,
                participationRate,
                averageTurnout,
                topVoters,
                recentActivity,
                proposalsByType,
                historicalTrends,
                Instant.now().toString()
            );
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    // ==================== DTOs ====================

    /**
     * Comprehensive governance statistics
     */
    public record GovernanceStats(
        int totalProposals,
        int activeProposals,
        int passedProposals,
        int rejectedProposals,
        long totalVotes,
        double participationRate,    // Percentage of eligible voters who participated
        double averageTurnout,        // Average turnout per proposal
        List<TopVoter> topVoters,
        List<RecentActivity> recentActivity,
        Map<String, Integer> proposalsByType,
        List<GovernanceTrend> historicalTrends,
        String timestamp
    ) {}

    /**
     * Top voter information
     */
    public record TopVoter(
        String voterAddress,
        String voterName,
        long votesCast,
        BigDecimal votingPower,
        double participationRate,
        long proposalsVoted
    ) {}

    /**
     * Recent governance activity
     */
    public record RecentActivity(
        String activityType,      // PROPOSAL_CREATED, VOTE_CAST, PROPOSAL_PASSED, PROPOSAL_REJECTED
        String proposalId,
        String proposalTitle,
        String actor,             // Address of the actor
        String details,
        String timestamp
    ) {}

    /**
     * Historical governance trend data
     */
    public record GovernanceTrend(
        String date,
        int proposalsCreated,
        int proposalsPassed,
        int proposalsRejected,
        long votesCast,
        double participationRate,
        double averageTurnout
    ) {}

    // ==================== Internal Governance Data Store ====================

    /**
     * Simulated governance data store
     * In production, this would be backed by a database
     */
    private static class GovernanceData {
        private final int totalProposals;
        private final int activeProposals;
        private final int passedProposals;
        private final int rejectedProposals;
        private final long totalVotes;
        private final long totalEligibleVoters;

        public GovernanceData() {
            // Initialize with realistic simulated data
            this.totalProposals = 45;
            this.activeProposals = 12;
            this.passedProposals = 28;
            this.rejectedProposals = 5;
            this.totalVotes = 125678;
            this.totalEligibleVoters = 15000;
        }

        public int getTotalProposals() {
            return totalProposals;
        }

        public int getActiveProposals() {
            return activeProposals;
        }

        public int getPassedProposals() {
            return passedProposals;
        }

        public int getRejectedProposals() {
            return rejectedProposals;
        }

        public long getTotalVotes() {
            return totalVotes;
        }

        public double calculateParticipationRate() {
            // Overall participation rate across all proposals
            double avgVotesPerProposal = (double) totalVotes / totalProposals;
            return (avgVotesPerProposal / totalEligibleVoters) * 100.0;
        }

        public double calculateAverageTurnout() {
            // Average turnout percentage per proposal
            return 42.5 + (ThreadLocalRandom.current().nextDouble() * 5.0);
        }

        public List<TopVoter> getTopVoters(int limit) {
            List<TopVoter> topVoters = new ArrayList<>();

            String[] voterNames = {
                "AuriWhale", "DeFiGuru", "GovernanceDAO", "StakeMaster", "CryptoSage",
                "BlockchainPro", "TokenHolder", "ValidatorOne", "DAOAdvocate", "ChainGovernor"
            };

            for (int i = 0; i < Math.min(limit, voterNames.length); i++) {
                String address = String.format("0xvoter-%02d-address", i + 1);
                long votesCast = 500 - (i * 35);
                BigDecimal votingPower = new BigDecimal(2500000 - (i * 150000));
                double participationRate = 98.0 - (i * 2.5);
                long proposalsVoted = 42 - (i * 2);

                topVoters.add(new TopVoter(
                    address,
                    voterNames[i],
                    votesCast,
                    votingPower,
                    participationRate,
                    proposalsVoted
                ));
            }

            return topVoters;
        }

        public List<RecentActivity> getRecentActivity(int limit) {
            List<RecentActivity> activities = new ArrayList<>();

            String[] activityTypes = {"PROPOSAL_CREATED", "VOTE_CAST", "PROPOSAL_PASSED", "PROPOSAL_REJECTED", "VOTE_CAST"};
            String[] proposalTitles = {
                "Increase Block Size to 15,000 Transactions",
                "Reduce Validator Commission Cap to 15%",
                "Implement Cross-Chain Bridge to Ethereum",
                "Upgrade HyperRAFT++ to v3.0",
                "Allocate 5M AUR to Community Treasury",
                "Implement AI-Driven Gas Fee Optimization",
                "Add CRYSTALS-Dilithium Level 6 Security",
                "Enable Multi-Signature Wallet Support",
                "Launch NFT Marketplace on Mainnet",
                "Introduce Liquid Staking Derivatives"
            };

            String[] actors = {
                "0xproposer-01", "0xvoter-25", "0xvalidator-03", "0xvoter-42", "0xproposer-02",
                "0xvoter-18", "0xvalidator-01", "0xvoter-33", "0xproposer-03", "0xvoter-07"
            };

            for (int i = 0; i < Math.min(limit, 20); i++) {
                String activityType = activityTypes[i % activityTypes.length];
                String proposalId = String.format("prop-%03d", (totalProposals - i));
                String proposalTitle = proposalTitles[i % proposalTitles.length];
                String actor = actors[i % actors.length];

                String details = switch (activityType) {
                    case "PROPOSAL_CREATED" -> "New " + getProposalType(i) + " proposal submitted";
                    case "VOTE_CAST" -> "Voted YES with " + (1500 + i * 50) + " voting power";
                    case "PROPOSAL_PASSED" -> "Proposal passed with 68.5% approval";
                    case "PROPOSAL_REJECTED" -> "Proposal rejected with 32.1% approval";
                    default -> "Governance action performed";
                };

                Instant timestamp = Instant.now().minusSeconds(i * 3600);

                activities.add(new RecentActivity(
                    activityType,
                    proposalId,
                    proposalTitle,
                    actor,
                    details,
                    timestamp.toString()
                ));
            }

            return activities;
        }

        public Map<String, Integer> getProposalsByType() {
            Map<String, Integer> breakdown = new LinkedHashMap<>();
            breakdown.put("PARAMETER_CHANGE", 15);
            breakdown.put("TEXT_PROPOSAL", 8);
            breakdown.put("TREASURY_SPEND", 12);
            breakdown.put("SOFTWARE_UPGRADE", 7);
            breakdown.put("COMMUNITY_POOL_SPEND", 3);
            return breakdown;
        }

        public List<GovernanceTrend> getHistoricalTrends(int days) {
            List<GovernanceTrend> trends = new ArrayList<>();

            for (int i = days - 1; i >= 0; i--) {
                Instant date = Instant.now().minus(i, ChronoUnit.DAYS);

                // Generate realistic trend data with some variance
                int proposalsCreated = i % 5 == 0 ? ThreadLocalRandom.current().nextInt(1, 4) : 0;
                int proposalsPassed = i % 7 == 0 ? ThreadLocalRandom.current().nextInt(0, 3) : 0;
                int proposalsRejected = i % 11 == 0 ? ThreadLocalRandom.current().nextInt(0, 2) : 0;
                long votesCast = ThreadLocalRandom.current().nextLong(2000, 5000);
                double participationRate = 40.0 + ThreadLocalRandom.current().nextDouble() * 15.0;
                double averageTurnout = 38.0 + ThreadLocalRandom.current().nextDouble() * 20.0;

                trends.add(new GovernanceTrend(
                    date.truncatedTo(ChronoUnit.DAYS).toString(),
                    proposalsCreated,
                    proposalsPassed,
                    proposalsRejected,
                    votesCast,
                    Math.round(participationRate * 10.0) / 10.0,
                    Math.round(averageTurnout * 10.0) / 10.0
                ));
            }

            return trends;
        }

        // Time-filtered methods

        public int getProposalsSince(Instant cutoff) {
            long daysSince = ChronoUnit.DAYS.between(cutoff, Instant.now());
            return Math.max(1, (int) (totalProposals * daysSince / 365));
        }

        public int getActiveProposalsSince(Instant cutoff) {
            return Math.min(activeProposals, getProposalsSince(cutoff));
        }

        public int getPassedProposalsSince(Instant cutoff) {
            long daysSince = ChronoUnit.DAYS.between(cutoff, Instant.now());
            return (int) (passedProposals * daysSince / 365);
        }

        public int getRejectedProposalsSince(Instant cutoff) {
            long daysSince = ChronoUnit.DAYS.between(cutoff, Instant.now());
            return (int) (rejectedProposals * daysSince / 365);
        }

        public long getVotesSince(Instant cutoff) {
            long daysSince = ChronoUnit.DAYS.between(cutoff, Instant.now());
            return (long) (totalVotes * daysSince / 365);
        }

        public double calculateParticipationRateSince(Instant cutoff) {
            return calculateParticipationRate();
        }

        public double calculateAverageTurnoutSince(Instant cutoff) {
            return calculateAverageTurnout();
        }

        public List<TopVoter> getTopVotersSince(Instant cutoff, int limit) {
            return getTopVoters(limit);
        }

        public List<RecentActivity> getRecentActivitySince(Instant cutoff, int limit) {
            return getRecentActivity(limit).stream()
                .filter(activity -> Instant.parse(activity.timestamp()).isAfter(cutoff))
                .collect(Collectors.toList());
        }

        public Map<String, Integer> getProposalsByTypeSince(Instant cutoff) {
            return getProposalsByType();
        }

        private String getProposalType(int index) {
            String[] types = {"PARAMETER_CHANGE", "TEXT_PROPOSAL", "TREASURY_SPEND", "SOFTWARE_UPGRADE"};
            return types[index % types.length];
        }
    }
}
