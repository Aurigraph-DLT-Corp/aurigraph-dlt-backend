package io.aurigraph.v11.bridge.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.List;

/**
 * Validator Network Statistics Model
 * Comprehensive statistics for the validator network in cross-chain bridge operations
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ValidatorNetworkStats {
    
    @JsonProperty("activeValidators")
    private Integer activeValidators;
    
    @JsonProperty("totalStake")
    private BigDecimal totalStake;
    
    @JsonProperty("networkHealth")
    private Double networkHealth; // percentage (0-100)
    
    @JsonProperty("totalValidators")
    private Integer totalValidators;
    
    @JsonProperty("inactiveValidators")
    private Integer inactiveValidators;
    
    @JsonProperty("averageStake")
    private BigDecimal averageStake;
    
    @JsonProperty("minimumStake")
    private BigDecimal minimumStake;
    
    @JsonProperty("maximumStake")
    private BigDecimal maximumStake;
    
    @JsonProperty("stakingRewards24h")
    private BigDecimal stakingRewards24h;
    
    @JsonProperty("totalSlashedAmount")
    private BigDecimal totalSlashedAmount;
    
    @JsonProperty("slashingEvents24h")
    private Integer slashingEvents24h;
    
    @JsonProperty("averageUptime")
    private Double averageUptime; // percentage (0-100)
    
    @JsonProperty("consensusParticipation")
    private Double consensusParticipation; // percentage (0-100)
    
    @JsonProperty("networkLatency")
    private Double networkLatency; // in milliseconds
    
    @JsonProperty("validatorPerformance")
    private Map<String, ValidatorPerformance> validatorPerformance;
    
    @JsonProperty("stakeDistribution")
    private StakeDistribution stakeDistribution;
    
    @JsonProperty("geographicDistribution")
    private Map<String, Integer> geographicDistribution;
    
    @JsonProperty("timestamp")
    private Instant timestamp;
    
    /**
     * Individual validator performance metrics
     */
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ValidatorPerformance {
        @JsonProperty("validatorId")
        private String validatorId;
        
        @JsonProperty("address")
        private String address;
        
        @JsonProperty("stake")
        private BigDecimal stake;
        
        @JsonProperty("uptime")
        private Double uptime; // percentage
        
        @JsonProperty("performance")
        private Double performance; // score (0-100)
        
        @JsonProperty("consensusVotes")
        private Long consensusVotes;
        
        @JsonProperty("missedVotes")
        private Long missedVotes;
        
        @JsonProperty("isActive")
        private Boolean isActive;
        
        @JsonProperty("lastActiveTime")
        private Instant lastActiveTime;
        
        @JsonProperty("slashCount")
        private Integer slashCount;
        
        @JsonProperty("rewards24h")
        private BigDecimal rewards24h;
        
        @JsonProperty("location")
        private String location;
        
        @JsonProperty("version")
        private String version;
    }
    
    /**
     * Stake distribution statistics
     */
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class StakeDistribution {
        @JsonProperty("top10ValidatorsStake")
        private BigDecimal top10ValidatorsStake;
        
        @JsonProperty("top10StakePercentage")
        private Double top10StakePercentage;
        
        @JsonProperty("top50ValidatorsStake")
        private BigDecimal top50ValidatorsStake;
        
        @JsonProperty("top50StakePercentage")
        private Double top50StakePercentage;
        
        @JsonProperty("nakamotoCoefficient")
        private Integer nakamotoCoefficient;
        
        @JsonProperty("herfindahlIndex")
        private Double herfindahlIndex;
        
        @JsonProperty("giniCoefficient")
        private Double giniCoefficient;
    }
    
    // Constructor with essential fields
    public ValidatorNetworkStats(Integer activeValidators, BigDecimal totalStake, Double networkHealth) {
        this.activeValidators = activeValidators;
        this.totalStake = totalStake;
        this.networkHealth = networkHealth;
        this.timestamp = Instant.now();
    }
    
    /**
     * Calculate network decentralization score (0-100)
     */
    public Double calculateDecentralizationScore() {
        if (stakeDistribution == null || totalValidators == null || totalValidators == 0) {
            return 0.0;
        }
        
        double score = 100.0;
        
        // Penalize high concentration in top validators
        if (stakeDistribution.getTop10StakePercentage() != null) {
            if (stakeDistribution.getTop10StakePercentage() > 50) {
                score -= (stakeDistribution.getTop10StakePercentage() - 50);
            }
        }
        
        // Reward high Nakamoto coefficient
        if (stakeDistribution.getNakamotoCoefficient() != null) {
            score += Math.min(20, stakeDistribution.getNakamotoCoefficient() * 2);
        }
        
        // Consider validator count
        if (totalValidators < 10) score -= 30;
        else if (totalValidators < 50) score -= 10;
        else if (totalValidators > 100) score += 10;
        
        return Math.max(0.0, Math.min(100.0, score));
    }
    
    /**
     * Calculate network security score (0-100)
     */
    public Double calculateSecurityScore() {
        if (activeValidators == null || totalStake == null) {
            return 0.0;
        }
        
        double score = networkHealth != null ? networkHealth : 0.0;
        
        // Adjust based on average uptime
        if (averageUptime != null) {
            score = (score + averageUptime) / 2;
        }
        
        // Adjust based on consensus participation
        if (consensusParticipation != null) {
            score = (score + consensusParticipation) / 2;
        }
        
        // Penalize for recent slashing events
        if (slashingEvents24h != null && slashingEvents24h > 0) {
            score -= Math.min(20, slashingEvents24h * 5);
        }
        
        return Math.max(0.0, Math.min(100.0, score));
    }
    
    /**
     * Check if network is healthy
     */
    public Boolean isNetworkHealthy() {
        return activeValidators != null && activeValidators >= 10 &&
               networkHealth != null && networkHealth >= 95.0 &&
               averageUptime != null && averageUptime >= 98.0 &&
               consensusParticipation != null && consensusParticipation >= 90.0;
    }
    
    /**
     * Get percentage of validators that are active
     */
    public Double getActiveValidatorPercentage() {
        if (totalValidators == null || totalValidators == 0) {
            return 0.0;
        }
        return (double) activeValidators / totalValidators * 100.0;
    }
    
    /**
     * Calculate voting power concentration
     */
    public Double getVotingPowerConcentration() {
        if (stakeDistribution != null && stakeDistribution.getTop10StakePercentage() != null) {
            return stakeDistribution.getTop10StakePercentage();
        }
        return 0.0;
    }
}