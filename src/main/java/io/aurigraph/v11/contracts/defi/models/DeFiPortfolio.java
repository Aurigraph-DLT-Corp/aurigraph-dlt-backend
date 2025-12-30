package io.aurigraph.v11.contracts.defi.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Sprint 4 DeFi Portfolio
 * Comprehensive view of user's DeFi positions across all protocols
 * Includes risk analytics and performance metrics
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class DeFiPortfolio {
    
    @JsonProperty("userAddress")
    private String userAddress;
    
    @JsonProperty("liquidityPositions")
    private List<LiquidityPosition> liquidityPositions;
    
    @JsonProperty("yieldPositions")
    private List<YieldFarmRewards> yieldPositions;
    
    @JsonProperty("loanPositions")
    private List<LoanPosition> loanPositions;
    
    @JsonProperty("totalValue")
    private BigDecimal totalValue;
    
    @JsonProperty("totalYieldEarned")
    private BigDecimal totalYieldEarned;
    
    @JsonProperty("riskScore")
    private BigDecimal riskScore;
    
    @JsonProperty("diversificationScore")
    private BigDecimal diversificationScore;
    
    @JsonProperty("lastUpdated")
    private Instant lastUpdated;
    
    // Constructors
    public DeFiPortfolio() {
        this.liquidityPositions = new ArrayList<>();
        this.yieldPositions = new ArrayList<>();
        this.loanPositions = new ArrayList<>();
        this.totalValue = BigDecimal.ZERO;
        this.totalYieldEarned = BigDecimal.ZERO;
        this.riskScore = BigDecimal.ZERO;
        this.diversificationScore = BigDecimal.ZERO;
        this.lastUpdated = Instant.now();
    }
    
    public DeFiPortfolio(String userAddress) {
        this();
        this.userAddress = userAddress;
    }
    
    // Getters and Setters
    public String getUserAddress() { return userAddress; }
    public void setUserAddress(String userAddress) { this.userAddress = userAddress; }
    
    public List<LiquidityPosition> getLiquidityPositions() { return liquidityPositions; }
    public void setLiquidityPositions(List<LiquidityPosition> liquidityPositions) { this.liquidityPositions = liquidityPositions; }
    
    public List<YieldFarmRewards> getYieldPositions() { return yieldPositions; }
    public void setYieldPositions(List<YieldFarmRewards> yieldPositions) { this.yieldPositions = yieldPositions; }
    
    public List<LoanPosition> getLoanPositions() { return loanPositions; }
    public void setLoanPositions(List<LoanPosition> loanPositions) { this.loanPositions = loanPositions; }
    
    public BigDecimal getTotalValue() { return totalValue; }
    public void setTotalValue(BigDecimal totalValue) { this.totalValue = totalValue; }
    
    public BigDecimal getTotalYieldEarned() { return totalYieldEarned; }
    public void setTotalYieldEarned(BigDecimal totalYieldEarned) { this.totalYieldEarned = totalYieldEarned; }
    
    public BigDecimal getRiskScore() { return riskScore; }
    public void setRiskScore(BigDecimal riskScore) { this.riskScore = riskScore; }
    
    public BigDecimal getDiversificationScore() { return diversificationScore; }
    public void setDiversificationScore(BigDecimal diversificationScore) { this.diversificationScore = diversificationScore; }
    
    public Instant getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(Instant lastUpdated) { this.lastUpdated = lastUpdated; }
}