package io.aurigraph.v11.portal.services;

import io.aurigraph.v11.contracts.rwa.DividendDistributionService;
import io.aurigraph.v11.portal.models.*;
import io.quarkus.logging.Log;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.Instant;
import java.util.*;

/**
 * StakingDataService provides staking and reward distribution data
 * Bridges Portal frontend requests to staking and rewards services
 *
 * INTEGRATION NOTE: This service is configured to receive dependency-injected
 * DividendDistributionService for real dividend/reward data. Currently uses mock data for demo.
 * Replace mock data calls with:
 * - dividendDistributionService.distributeDividends() for actual distributions
 * - dividendDistributionService.calculateNetDividendAmount() for calculations
 */
@ApplicationScoped
public class StakingDataService {

    @Inject
    DividendDistributionService dividendDistributionService;

    /**
     * Get staking information
     */
    public Uni<StakingInfoDTO> getStakingInfo() {
        return Uni.createFrom().item(() -> {
            Log.info("Fetching staking information");

            return StakingInfoDTO.builder()
                .totalStaked("45,000,000 AUR")
                .stakedPercentage(45.0)
                .totalValidators(128)
                .activeValidators(16)
                .minStakeAmount("2,500,000 AUR")
                .maxStakeAmount("unlimited")
                .averageValidatorStake("2,812,500 AUR")
                .medianValidatorStake("2,812,500 AUR")
                .unbondingPeriod(259200)
                .commissionMin(0.0)
                .commissionMax(25.0)
                .averageCommission(5.2)
                .annualRewardRate(8.5)
                .estimatedAnnualReward("$9,234,567")
                .slashingRate(10.0)
                .jailPeriod(86400)
                .totalRewardsPaid("$125,234,567")
                .rewardsDistributedLast24h("$234,567")
                .nextRewardDistribution(Instant.now().plusSeconds(86400L))
                .build();
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r))
         .onFailure().recoverWithItem(throwable -> {
             Log.error("Failed to get staking info", throwable);
             return StakingInfoDTO.builder()
                 .error(throwable.getMessage())
                 .build();
         });
    }

    /**
     * Get reward distribution pools
     */
    public Uni<List<RewardDistributionDTO>> getDistributionPools() {
        return Uni.createFrom().item(() -> {
            Log.info("Fetching reward distribution pools");

            List<RewardDistributionDTO> pools = new ArrayList<>();

            // Validator Rewards Pool
            pools.add(RewardDistributionDTO.builder()
                .poolId("POOL-VALIDATOR")
                .poolName("Validator Rewards Pool")
                .description("Rewards for active block validators")
                .poolType("validator")
                .totalRewards("$45,234,567")
                .rewardsDistributedLast24h("$123,456")
                .rewardsDistributedLast30d("$3,704,160")
                .participantCount(16)
                .averageRewardPerParticipant("$2,827,160")
                .rewardFrequency("every-block")
                .nextDistribution(Instant.now().plusSeconds(3L))
                .distributionPercentage(60.0)
                .status("active")
                .build());

            // Delegator Rewards Pool
            pools.add(RewardDistributionDTO.builder()
                .poolId("POOL-DELEGATOR")
                .poolName("Delegator Rewards Pool")
                .description("Rewards for token delegation")
                .poolType("delegator")
                .totalRewards("$23,567,234")
                .rewardsDistributedLast24h("$64,285")
                .rewardsDistributedLast30d("$1,928,550")
                .participantCount(234567)
                .averageRewardPerParticipant("$100.45")
                .rewardFrequency("daily")
                .nextDistribution(Instant.now().plusSeconds(43200L))
                .distributionPercentage(25.0)
                .status("active")
                .build());

            // Community Rewards Pool
            pools.add(RewardDistributionDTO.builder()
                .poolId("POOL-COMMUNITY")
                .poolName("Community Rewards Pool")
                .description("Community development and incentives")
                .poolType("community")
                .totalRewards("$10,345,678")
                .rewardsDistributedLast24h("$28,341")
                .rewardsDistributedLast30d("$850,260")
                .participantCount(567890)
                .averageRewardPerParticipant("$18.20")
                .rewardFrequency("weekly")
                .nextDistribution(Instant.now().plusSeconds(604800L))
                .distributionPercentage(10.0)
                .status("active")
                .build());

            // Treasury Pool
            pools.add(RewardDistributionDTO.builder()
                .poolId("POOL-TREASURY")
                .poolName("Treasury Pool")
                .description("Network treasury for governance")
                .poolType("treasury")
                .totalRewards("$5,234,890")
                .rewardsDistributedLast24h("$14,341")
                .rewardsDistributedLast30d("$430,170")
                .participantCount(1)
                .averageRewardPerParticipant("$5,234,890")
                .rewardFrequency("monthly")
                .nextDistribution(Instant.now().plusSeconds(2592000L))
                .distributionPercentage(5.0)
                .status("active")
                .build());

            return pools;
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r))
         .onFailure().recoverWithItem(throwable -> {
             Log.error("Failed to get distribution pools", throwable);
             return Collections.emptyList();
         });
    }

    /**
     * Get detailed reward statistics
     */
    public Uni<RewardStatisticsDTO> getRewardStatistics() {
        return Uni.createFrom().item(() -> {
            Log.info("Fetching reward statistics");

            return RewardStatisticsDTO.builder()
                .totalRewardsGenerated("$8,234,567")
                .totalRewardsDistributed("$125,234,567")
                .totalRewardsBurned("$345,678")
                .totalRewardsPending("$2,345,678")
                .dailyRewardGeneration("$225,123")
                .weeklyRewardGeneration("$1,575,861")
                .monthlyRewardGeneration("$6,753,690")
                .averageRewardWaitTime(86400)
                .minRewardAmount("$1.00")
                .maxRewardAmount("$2,345,678")
                .averageRewardAmount("$234.56")
                .medianRewardAmount("$100.00")
                .topRewardRecipient("validator-1")
                .topRewardAmount("$345,678")
                .rewardGrowthRate(5.2)
                .claimRate(98.5)
                .reinvestmentRate(42.3)
                .autoCompoundingEnabled(true)
                .taxRate(0.0)
                .build();
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r))
         .onFailure().recoverWithItem(throwable -> {
             Log.error("Failed to get reward statistics", throwable);
             return RewardStatisticsDTO.builder()
                 .error(throwable.getMessage())
                 .build();
         });
    }
}
