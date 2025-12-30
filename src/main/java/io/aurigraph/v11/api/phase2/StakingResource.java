package io.aurigraph.v11.api.phase2;

import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.jboss.logging.Logger;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;

/**
 * Sprint 14: Staking Dashboard REST API (18 pts)
 *
 * Endpoints for staking info, pools, rewards calculation, and delegation overview.
 * Extracted from Phase2BlockchainResource for better maintainability.
 *
 * @author Backend Development Agent (BDA)
 * @version 11.0.0
 * @since Sprint 14
 */
@Path("/api/v11/blockchain")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class StakingResource {

    private static final Logger LOG = Logger.getLogger(StakingResource.class);

    /**
     * Get staking information overview
     * GET /api/v11/blockchain/staking/info
     */
    @GET
    @Path("/staking/info")
    public Uni<StakingInfo> getStakingInfo() {
        LOG.info("Fetching staking information");

        return Uni.createFrom().item(() -> {
            StakingInfo info = new StakingInfo();
            info.totalStaked = new BigDecimal("2450000000");
            info.totalValidators = 127;
            info.activeValidators = 121;
            info.averageAPY = 12.5;
            info.minStakeAmount = new BigDecimal("1000");
            info.unbondingPeriod = "7 days";
            info.totalDelegators = 15000;
            info.totalRewardsDistributed = new BigDecimal("125000000");
            info.rewardsDistributedToday = new BigDecimal("342465");
            info.networkStakingRatio = 68.5;
            return info;
        });
    }

    /**
     * Get staking pools overview
     * GET /api/v11/blockchain/staking/pools
     */
    @GET
    @Path("/staking/pools")
    public Uni<StakingPools> getStakingPools() {
        LOG.info("Fetching staking pools");

        return Uni.createFrom().item(() -> {
            StakingPools pools = new StakingPools();
            pools.totalPools = 25;
            pools.totalStaked = new BigDecimal("2450000000");
            pools.averageAPY = 12.5;
            pools.pools = new ArrayList<>();

            for (int i = 1; i <= 5; i++) {
                StakingPool pool = new StakingPool();
                pool.poolId = "pool-" + i;
                pool.poolName = "Aurigraph Pool " + i;
                pool.totalStake = new BigDecimal("500000000");
                pool.apr = 10.0 + i;
                pool.participantCount = 1000 + (i * 100);
                pool.minStake = new BigDecimal("1000");
                pool.unbondingPeriod = "7 days";
                pools.pools.add(pool);
            }

            return pools;
        });
    }

    /**
     * Calculate staking rewards
     * POST /api/v11/blockchain/staking/calculate-rewards
     */
    @POST
    @Path("/staking/calculate-rewards")
    public Uni<StakingRewards> calculateRewards(RewardsCalculation calc) {
        LOG.infof("Calculating rewards for %s AUR over %d days", calc.amount, calc.days);

        return Uni.createFrom().item(() -> {
            StakingRewards rewards = new StakingRewards();
            BigDecimal amount = new BigDecimal(calc.amount);
            BigDecimal apr = new BigDecimal(calc.apr != null ? calc.apr : "12.5");
            BigDecimal days = new BigDecimal(calc.days);

            BigDecimal dailyReward = amount.multiply(apr)
                .divide(new BigDecimal("100"), 6, RoundingMode.HALF_UP)
                .divide(new BigDecimal("365"), 6, RoundingMode.HALF_UP);
            BigDecimal totalReward = dailyReward.multiply(days);

            rewards.stakedAmount = amount;
            rewards.apr = apr;
            rewards.stakingPeriod = calc.days + " days";
            rewards.dailyReward = dailyReward;
            rewards.totalReward = totalReward;
            rewards.estimatedValue = amount.add(totalReward);

            return rewards;
        });
    }

    /**
     * Get delegation overview
     * GET /api/v11/blockchain/staking/delegations/{delegatorAddress}
     */
    @GET
    @Path("/staking/delegations/{delegatorAddress}")
    public Uni<DelegationOverview> getDelegations(@PathParam("delegatorAddress") String delegatorAddress) {
        LOG.infof("Fetching delegations for: %s", delegatorAddress);

        return Uni.createFrom().item(() -> {
            DelegationOverview overview = new DelegationOverview();
            overview.delegatorAddress = delegatorAddress;
            overview.totalDelegated = new BigDecimal("150000");
            overview.totalRewards = new BigDecimal("18750");
            overview.activeDelegations = 3;
            overview.delegations = new ArrayList<>();

            for (int i = 1; i <= 3; i++) {
                Delegation del = new Delegation();
                del.validatorAddress = "0xvalidator-0" + i;
                del.delegatedAmount = new BigDecimal("50000");
                del.rewards = new BigDecimal("6250");
                del.rewardShare = "85%";
                del.delegatedAt = Instant.now().minusSeconds(i * 86400L * 30).toString();
                overview.delegations.add(del);
            }

            return overview;
        });
    }

    // ==================== DTOs ====================

    public static class StakingInfo {
        public BigDecimal totalStaked;
        public int totalValidators;
        public int activeValidators;
        public double averageAPY;
        public BigDecimal minStakeAmount;
        public String unbondingPeriod;
        public int totalDelegators;
        public BigDecimal totalRewardsDistributed;
        public BigDecimal rewardsDistributedToday;
        public double networkStakingRatio;
    }

    public static class StakingPools {
        public int totalPools;
        public BigDecimal totalStaked;
        public double averageAPY;
        public List<StakingPool> pools;
    }

    public static class StakingPool {
        public String poolId;
        public String poolName;
        public BigDecimal totalStake;
        public double apr;
        public int participantCount;
        public BigDecimal minStake;
        public String unbondingPeriod;
    }

    public static class RewardsCalculation {
        public String amount;
        public int days;
        public String apr;
    }

    public static class StakingRewards {
        public BigDecimal stakedAmount;
        public BigDecimal apr;
        public String stakingPeriod;
        public BigDecimal dailyReward;
        public BigDecimal totalReward;
        public BigDecimal estimatedValue;
    }

    public static class DelegationOverview {
        public String delegatorAddress;
        public BigDecimal totalDelegated;
        public BigDecimal totalRewards;
        public int activeDelegations;
        public List<Delegation> delegations;
    }

    public static class Delegation {
        public String validatorAddress;
        public BigDecimal delegatedAmount;
        public BigDecimal rewards;
        public String rewardShare;
        public String delegatedAt;
    }
}
