package io.aurigraph.v11.contracts.defi;

import io.aurigraph.v11.contracts.defi.models.*;
import io.aurigraph.v11.contracts.defi.models.SwapModels.*;
import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Sprint 4 Enhanced Yield Farming Service
 * Manages yield farming operations with auto-compounding, multi-reward tokens,
 * and cross-protocol optimization
 */
@ApplicationScoped
public class YieldFarmingService {
    
    private static final Logger logger = LoggerFactory.getLogger(YieldFarmingService.class);
    
    // Farm storage
    private final Map<String, YieldFarm> farms = new ConcurrentHashMap<>();
    private final Map<String, List<YieldFarmRewards>> userRewards = new ConcurrentHashMap<>();
    private final Map<String, YieldFarmRewards> allRewards = new ConcurrentHashMap<>();
    
    // Performance tracking
    private final AtomicLong rewardIdGenerator = new AtomicLong(0);
    private volatile BigDecimal totalStaked = BigDecimal.ZERO;
    
    /**
     * Initialize yield farms with various protocols
     */
    public void initializeFarms() {
        logger.info("Initializing yield farms for Sprint 4");
        
        // High APR farms
        createFarm("FARM_ETH_USDC_LP", "ETH_USDC_LP", 
                  BigDecimal.valueOf(0.15), // 15% base APR
                  Arrays.asList("AURI", "ETH"), 
                  86400L); // 1 day lockup
        
        createFarm("FARM_BTC_ETH_LP", "BTC_ETH_LP", 
                  BigDecimal.valueOf(0.12), // 12% base APR
                  Arrays.asList("AURI", "BTC"), 
                  172800L); // 2 day lockup
        
        // Stablecoin farms (lower risk)
        createFarm("FARM_STABLE_LP", "USDC_USDT_LP", 
                  BigDecimal.valueOf(0.08), // 8% base APR
                  Arrays.asList("AURI"), 
                  43200L); // 12 hour lockup
        
        // Single token staking
        createFarm("FARM_AURI_STAKE", "AURI", 
                  BigDecimal.valueOf(0.20), // 20% base APR
                  Arrays.asList("AURI"), 
                  604800L); // 7 day lockup
        
        createFarm("FARM_ETH_STAKE", "ETH", 
                  BigDecimal.valueOf(0.05), // 5% base APR
                  Arrays.asList("ETH"), 
                  2592000L); // 30 day lockup
        
        logger.info("Initialized {} yield farms with various APRs", farms.size());
    }
    
    /**
     * Stake tokens in a yield farm
     */
    public YieldFarmRewards stake(String farmId, String userAddress, BigDecimal amount) {
        YieldFarm farm = farms.get(farmId);
        if (farm == null) {
            throw new IllegalArgumentException("Farm not found: " + farmId);
        }
        
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Invalid stake amount");
        }
        
        // Create reward entry
        String rewardId = generateRewardId();
        YieldFarmRewards rewards = new YieldFarmRewards(farmId, userAddress, amount, farm.getStakingToken());
        rewards.setBaseApr(farm.getBaseApr());
        rewards.setBoostMultiplier(calculateBoostMultiplier(userAddress, amount));
        rewards.setBoostedApr(rewards.getBaseApr().multiply(rewards.getBoostMultiplier()));
        rewards.setLockupPeriod(farm.getLockupPeriod());
        
        // Calculate risk-adjusted APR
        BigDecimal riskScore = calculateFarmRiskScore(farm);
        BigDecimal riskAdjustedAPR = rewards.getBoostedApr().multiply(BigDecimal.ONE.subtract(riskScore));
        rewards.setRiskAdjustedAPR(riskAdjustedAPR);
        rewards.setImpermanentLossRisk(calculateImpermanentLossRisk(farm));
        
        // Set up reward tokens
        List<YieldFarmRewards.RewardToken> rewardTokens = new ArrayList<>();
        for (String token : farm.getRewardTokens()) {
            YieldFarmRewards.RewardToken rewardToken = new YieldFarmRewards.RewardToken(
                token, token, calculateRewardRate(farm, token));
            rewardTokens.add(rewardToken);
        }
        rewards.setRewardTokens(rewardTokens);
        
        // Update farm stats
        farm.addStaker(userAddress, amount);
        
        // Store rewards
        allRewards.put(rewardId, rewards);
        userRewards.computeIfAbsent(userAddress, k -> new ArrayList<>()).add(rewards);
        
        // Update total staked
        updateTotalStaked();
        
        logger.info("Started yield farming: {} staked {} {} in farm {}", 
                   userAddress, amount, farm.getStakingToken(), farmId);
        return rewards;
    }
    
    /**
     * Compound rewards for a user
     */
    public void compound(String farmId, String userAddress) {
        List<YieldFarmRewards> userFarmRewards = getUserRewards(userAddress);
        
        for (YieldFarmRewards rewards : userFarmRewards) {
            if (rewards.getFarmId().equals(farmId) && rewards.shouldAutoCompound()) {
                BigDecimal pendingRewards = rewards.calculatePendingRewards();
                
                if (pendingRewards.compareTo(BigDecimal.ZERO) > 0) {
                    // Add pending rewards to staked amount
                    BigDecimal newStakedAmount = rewards.getStakedAmount().add(pendingRewards);
                    rewards.setStakedAmount(newStakedAmount);
                    
                    // Update claimed rewards
                    rewards.setClaimedRewards(rewards.getClaimedRewards().add(pendingRewards));
                    rewards.setLastCompoundTime(Instant.now());
                    
                    logger.debug("Auto-compounded {} for user {} in farm {}", 
                               pendingRewards, userAddress, farmId);
                }
            }
        }
    }
    
    /**
     * Find optimal yield distribution across protocols
     */
    public List<YieldOpportunity> findOptimalYieldDistribution(String userAddress, 
                                                               BigDecimal totalAmount, 
                                                               String baseToken) {
        List<YieldOpportunity> opportunities = new ArrayList<>();
        
        for (YieldFarm farm : farms.values()) {
            if (farm.getStakingToken().contains(baseToken) || baseToken.equals("ALL")) {
                YieldOpportunity opportunity = new YieldOpportunity();
                opportunity.setProtocol("Aurigraph DeFi");
                opportunity.setFarmId(farm.getFarmId());
                opportunity.setStakingToken(farm.getStakingToken());
                opportunity.setBaseAPR(farm.getBaseApr());
                
                // Calculate boosted APR based on amount
                BigDecimal boostMultiplier = calculateBoostMultiplier(userAddress, totalAmount);
                opportunity.setBoostedAPR(farm.getBaseApr().multiply(boostMultiplier));
                
                // Calculate risk-adjusted APR
                BigDecimal riskScore = calculateFarmRiskScore(farm);
                BigDecimal riskAdjustedAPR = opportunity.getBoostedAPR()
                    .multiply(BigDecimal.ONE.subtract(riskScore));
                opportunity.setRiskAdjustedAPR(riskAdjustedAPR);
                opportunity.setRiskScore(riskScore);
                opportunity.setTotalValueLocked(farm.getTotalStaked());
                
                opportunities.add(opportunity);
            }
        }
        
        // Sort by risk-adjusted APR
        opportunities.sort((a, b) -> b.getRiskAdjustedAPR().compareTo(a.getRiskAdjustedAPR()));
        
        return opportunities;
    }
    
    /**
     * Find best yield opportunities on a specific chain
     */
    public List<YieldFarmRewards> findBestYieldOnChain(String chainId, String baseToken, BigDecimal amount) {
        // Mock implementation - in production would query multiple chains
        List<YieldFarmRewards> chainRewards = new ArrayList<>();
        
        for (YieldFarm farm : farms.values()) {
            if (farm.getStakingToken().contains(baseToken)) {
                YieldFarmRewards rewards = new YieldFarmRewards(
                    farm.getFarmId(), "cross-chain-user", amount, farm.getStakingToken());
                rewards.setBaseApr(farm.getBaseApr());
                rewards.setBoostedApr(farm.getBaseApr().multiply(BigDecimal.valueOf(1.1))); // 10% cross-chain boost
                
                chainRewards.add(rewards);
            }
        }
        
        return chainRewards;
    }
    
    /**
     * Get user's yield farming rewards
     */
    public List<YieldFarmRewards> getUserRewards(String userAddress) {
        return userRewards.getOrDefault(userAddress, new ArrayList<>());
    }
    
    /**
     * Get total amount staked across all farms
     */
    public BigDecimal getTotalStaked() {
        return totalStaked;
    }
    
    // Private helper methods
    private void createFarm(String farmId, String stakingToken, BigDecimal baseApr, 
                          List<String> rewardTokens, Long lockupPeriod) {
        YieldFarm farm = new YieldFarm(farmId, stakingToken, baseApr, rewardTokens, lockupPeriod);
        farms.put(farmId, farm);
        logger.debug("Created yield farm: {} for token {} with {}% APR", 
                   farmId, stakingToken, baseApr.multiply(BigDecimal.valueOf(100)));
    }
    
    private String generateRewardId() {
        return "REWARD_" + rewardIdGenerator.incrementAndGet() + "_" + System.currentTimeMillis();
    }
    
    private BigDecimal calculateBoostMultiplier(String userAddress, BigDecimal amount) {
        // Boost based on stake amount and user history
        BigDecimal boostMultiplier = BigDecimal.ONE;
        
        // Amount-based boost (larger stakes get higher boost)
        if (amount.compareTo(BigDecimal.valueOf(10000)) >= 0) {
            boostMultiplier = boostMultiplier.add(BigDecimal.valueOf(0.5)); // 50% boost
        } else if (amount.compareTo(BigDecimal.valueOf(1000)) >= 0) {
            boostMultiplier = boostMultiplier.add(BigDecimal.valueOf(0.2)); // 20% boost
        } else if (amount.compareTo(BigDecimal.valueOf(100)) >= 0) {
            boostMultiplier = boostMultiplier.add(BigDecimal.valueOf(0.1)); // 10% boost
        }
        
        // User loyalty boost (mock implementation)
        List<YieldFarmRewards> userHistory = getUserRewards(userAddress);
        if (userHistory.size() > 5) {
            boostMultiplier = boostMultiplier.add(BigDecimal.valueOf(0.25)); // Loyalty boost
        }
        
        return boostMultiplier;
    }
    
    private BigDecimal calculateFarmRiskScore(YieldFarm farm) {
        // Risk scoring based on various factors
        BigDecimal riskScore = BigDecimal.ZERO;
        
        // APR-based risk (higher APR = higher risk)
        if (farm.getBaseApr().compareTo(BigDecimal.valueOf(0.15)) > 0) {
            riskScore = riskScore.add(BigDecimal.valueOf(0.3)); // High APR risk
        } else if (farm.getBaseApr().compareTo(BigDecimal.valueOf(0.08)) > 0) {
            riskScore = riskScore.add(BigDecimal.valueOf(0.15)); // Medium APR risk
        }
        
        // Token type risk
        if (farm.getStakingToken().contains("LP")) {
            riskScore = riskScore.add(BigDecimal.valueOf(0.1)); // LP token risk
        }
        
        // Lockup period risk (longer lockup = higher risk)
        if (farm.getLockupPeriod() > 604800) { // > 7 days
            riskScore = riskScore.add(BigDecimal.valueOf(0.2));
        }
        
        return riskScore.min(BigDecimal.valueOf(0.8)); // Cap at 80%
    }
    
    private BigDecimal calculateImpermanentLossRisk(YieldFarm farm) {
        // Calculate IL risk for LP token farms
        if (farm.getStakingToken().contains("LP")) {
            if (farm.getStakingToken().contains("STABLE")) {
                return BigDecimal.valueOf(0.02); // 2% IL risk for stable pairs
            } else {
                return BigDecimal.valueOf(0.15); // 15% IL risk for volatile pairs
            }
        }
        return BigDecimal.ZERO; // No IL risk for single token staking
    }
    
    private BigDecimal calculateRewardRate(YieldFarm farm, String rewardToken) {
        // Mock reward rate calculation
        BigDecimal baseRate = farm.getBaseApr().divide(BigDecimal.valueOf(365), 8, RoundingMode.HALF_UP);
        
        if (rewardToken.equals("AURI")) {
            return baseRate.multiply(BigDecimal.valueOf(0.7)); // 70% in AURI
        } else {
            return baseRate.multiply(BigDecimal.valueOf(0.3)); // 30% in other tokens
        }
    }
    
    private void updateTotalStaked() {
        BigDecimal newTotal = farms.values().stream()
            .map(YieldFarm::getTotalStaked)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        this.totalStaked = newTotal;
    }
    
    // Inner class for YieldFarm
    public static class YieldFarm {
        private String farmId;
        private String stakingToken;
        private BigDecimal baseApr;
        private List<String> rewardTokens;
        private Long lockupPeriod;
        private Map<String, BigDecimal> stakers;
        private BigDecimal totalStaked;
        private Instant createdAt;
        
        public YieldFarm(String farmId, String stakingToken, BigDecimal baseApr, 
                        List<String> rewardTokens, Long lockupPeriod) {
            this.farmId = farmId;
            this.stakingToken = stakingToken;
            this.baseApr = baseApr;
            this.rewardTokens = rewardTokens;
            this.lockupPeriod = lockupPeriod;
            this.stakers = new ConcurrentHashMap<>();
            this.totalStaked = BigDecimal.ZERO;
            this.createdAt = Instant.now();
        }
        
        public void addStaker(String userAddress, BigDecimal amount) {
            stakers.put(userAddress, stakers.getOrDefault(userAddress, BigDecimal.ZERO).add(amount));
            this.totalStaked = this.totalStaked.add(amount);
        }
        
        // Getters
        public String getFarmId() { return farmId; }
        public String getStakingToken() { return stakingToken; }
        public BigDecimal getBaseApr() { return baseApr; }
        public List<String> getRewardTokens() { return rewardTokens; }
        public Long getLockupPeriod() { return lockupPeriod; }
        public BigDecimal getTotalStaked() { return totalStaked; }
        public Map<String, BigDecimal> getStakers() { return stakers; }
        public Instant getCreatedAt() { return createdAt; }
    }
}