package io.aurigraph.v11.bridge.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

/**
 * Bridge Statistics Model
 * Comprehensive statistics for cross-chain bridge operations
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BridgeStats {
    
    @JsonProperty("totalTransactions")
    private Long totalTransactions;
    
    @JsonProperty("totalVolume")
    private BigDecimal totalVolume;
    
    @JsonProperty("averageTime")
    private Double averageTime; // in seconds
    
    @JsonProperty("successRate")
    private Double successRate; // percentage (0-100)
    
    @JsonProperty("totalVolume24h")
    private BigDecimal totalVolume24h;
    
    @JsonProperty("totalTransactions24h")
    private Long totalTransactions24h;
    
    @JsonProperty("averageTime24h")
    private Double averageTime24h;
    
    @JsonProperty("successRate24h")
    private Double successRate24h;
    
    @JsonProperty("failedTransactions")
    private Long failedTransactions;
    
    @JsonProperty("pendingTransactions")
    private Long pendingTransactions;
    
    @JsonProperty("totalFees")
    private BigDecimal totalFees;
    
    @JsonProperty("averageFee")
    private BigDecimal averageFee;
    
    @JsonProperty("uniqueAddresses")
    private Long uniqueAddresses;
    
    @JsonProperty("supportedChains")
    private Integer supportedChains;
    
    @JsonProperty("activeValidators")
    private Integer activeValidators;
    
    @JsonProperty("totalLiquidity")
    private BigDecimal totalLiquidity;
    
    @JsonProperty("chainStats")
    private Map<String, ChainStats> chainStats;
    
    @JsonProperty("timestamp")
    private Instant timestamp;
    
    /**
     * Statistics per blockchain chain
     */
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ChainStats {
        @JsonProperty("chainId")
        private String chainId;
        
        @JsonProperty("chainName")
        private String chainName;
        
        @JsonProperty("totalTransactions")
        private Long totalTransactions;
        
        @JsonProperty("totalVolume")
        private BigDecimal totalVolume;
        
        @JsonProperty("averageTime")
        private Double averageTime;
        
        @JsonProperty("successRate")
        private Double successRate;
        
        @JsonProperty("isActive")
        private Boolean isActive;
        
        @JsonProperty("liquidity")
        private BigDecimal liquidity;
        
        @JsonProperty("lastTransaction")
        private Instant lastTransaction;
    }
    
    // Constructor with defaults
    public BridgeStats(Long totalTransactions, BigDecimal totalVolume, Double averageTime, Double successRate) {
        this.totalTransactions = totalTransactions;
        this.totalVolume = totalVolume;
        this.averageTime = averageTime;
        this.successRate = successRate;
        this.timestamp = Instant.now();
    }
    
    /**
     * Calculate overall bridge health score (0-100)
     */
    public Double calculateHealthScore() {
        if (successRate == null) return 0.0;
        
        double score = successRate;
        
        // Adjust score based on average time (lower is better)
        if (averageTime != null && averageTime > 0) {
            if (averageTime < 30) score += 5; // Fast transactions
            else if (averageTime > 300) score -= 10; // Slow transactions
        }
        
        // Adjust score based on pending transactions
        if (pendingTransactions != null && totalTransactions != null && totalTransactions > 0) {
            double pendingRatio = (double) pendingTransactions / totalTransactions;
            if (pendingRatio > 0.1) score -= (pendingRatio * 20); // High pending ratio reduces score
        }
        
        return Math.max(0.0, Math.min(100.0, score));
    }
    
    /**
     * Check if bridge is operating optimally
     */
    public Boolean isHealthy() {
        return successRate != null && successRate >= 95.0 && 
               averageTime != null && averageTime <= 120.0 &&
               (pendingTransactions == null || pendingTransactions < 100);
    }
}