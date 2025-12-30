package io.aurigraph.v11.contracts.defi.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Sprint 4 Liquidity Pool Model
 * Represents an AMM liquidity pool with enhanced features
 */
public class LiquidityPool {
    
    @JsonProperty("poolId")
    private String poolId;
    
    @JsonProperty("token0")
    private String token0;
    
    @JsonProperty("token1")
    private String token1;
    
    @JsonProperty("token0Reserve")
    private BigDecimal token0Reserve;
    
    @JsonProperty("token1Reserve")
    private BigDecimal token1Reserve;
    
    @JsonProperty("totalLiquidity")
    private BigDecimal totalLiquidity;
    
    @JsonProperty("lpTokenSupply")
    private BigDecimal lpTokenSupply;
    
    @JsonProperty("fee")
    private BigDecimal fee;
    
    @JsonProperty("protocolFee")
    private BigDecimal protocolFee;
    
    @JsonProperty("totalVolumeUSD")
    private BigDecimal totalVolumeUSD;
    
    @JsonProperty("totalFeesUSD")
    private BigDecimal totalFeesUSD;
    
    @JsonProperty("apr")
    private BigDecimal apr;
    
    @JsonProperty("apy")
    private BigDecimal apy;
    
    @JsonProperty("impermanentLossProtection")
    private boolean impermanentLossProtection;
    
    @JsonProperty("active")
    private boolean active;
    
    @JsonProperty("createdAt")
    private Instant createdAt;
    
    @JsonProperty("lastUpdated")
    private Instant lastUpdated;
    
    // Constructors
    public LiquidityPool() {
        this.active = true;
        this.impermanentLossProtection = false;
        this.fee = BigDecimal.valueOf(0.003); // 0.3% default fee
        this.protocolFee = BigDecimal.valueOf(0.0005); // 0.05% protocol fee
        this.createdAt = Instant.now();
        this.lastUpdated = Instant.now();
        this.totalLiquidity = BigDecimal.ZERO;
        this.lpTokenSupply = BigDecimal.ZERO;
        this.totalVolumeUSD = BigDecimal.ZERO;
        this.totalFeesUSD = BigDecimal.ZERO;
        this.apr = BigDecimal.ZERO;
        this.apy = BigDecimal.ZERO;
    }
    
    public LiquidityPool(String poolId, String token0, String token1) {
        this();
        this.poolId = poolId;
        this.token0 = token0;
        this.token1 = token1;
    }
    
    // Getters and Setters
    public String getPoolId() { return poolId; }
    public void setPoolId(String poolId) { this.poolId = poolId; }
    
    public String getToken0() { return token0; }
    public void setToken0(String token0) { this.token0 = token0; }
    
    public String getToken1() { return token1; }
    public void setToken1(String token1) { this.token1 = token1; }
    
    public BigDecimal getToken0Reserve() { return token0Reserve; }
    public void setToken0Reserve(BigDecimal token0Reserve) { 
        this.token0Reserve = token0Reserve;
        this.lastUpdated = Instant.now();
    }
    
    public BigDecimal getToken1Reserve() { return token1Reserve; }
    public void setToken1Reserve(BigDecimal token1Reserve) { 
        this.token1Reserve = token1Reserve;
        this.lastUpdated = Instant.now();
    }
    
    public BigDecimal getTotalLiquidity() { return totalLiquidity; }
    public void setTotalLiquidity(BigDecimal totalLiquidity) { this.totalLiquidity = totalLiquidity; }
    
    public BigDecimal getLpTokenSupply() { return lpTokenSupply; }
    public void setLpTokenSupply(BigDecimal lpTokenSupply) { this.lpTokenSupply = lpTokenSupply; }
    
    public BigDecimal getFee() { return fee; }
    public void setFee(BigDecimal fee) { this.fee = fee; }
    
    public BigDecimal getProtocolFee() { return protocolFee; }
    public void setProtocolFee(BigDecimal protocolFee) { this.protocolFee = protocolFee; }
    
    public BigDecimal getTotalVolumeUSD() { return totalVolumeUSD; }
    public void setTotalVolumeUSD(BigDecimal totalVolumeUSD) { this.totalVolumeUSD = totalVolumeUSD; }
    
    public BigDecimal getTotalFeesUSD() { return totalFeesUSD; }
    public void setTotalFeesUSD(BigDecimal totalFeesUSD) { this.totalFeesUSD = totalFeesUSD; }
    
    public BigDecimal getApr() { return apr; }
    public void setApr(BigDecimal apr) { this.apr = apr; }
    
    public BigDecimal getApy() { return apy; }
    public void setApy(BigDecimal apy) { this.apy = apy; }
    
    public boolean isImpermanentLossProtection() { return impermanentLossProtection; }
    public void setImpermanentLossProtection(boolean impermanentLossProtection) { this.impermanentLossProtection = impermanentLossProtection; }
    
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    
    public Instant getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(Instant lastUpdated) { this.lastUpdated = lastUpdated; }
    
    // Helper Methods
    public BigDecimal getCurrentPrice() {
        if (token0Reserve == null || token1Reserve == null || 
            token0Reserve.equals(BigDecimal.ZERO) || token1Reserve.equals(BigDecimal.ZERO)) {
            return BigDecimal.ZERO;
        }
        return token1Reserve.divide(token0Reserve, 18, java.math.RoundingMode.HALF_UP);
    }
    
    public BigDecimal getInversePrice() {
        if (token0Reserve == null || token1Reserve == null || 
            token0Reserve.equals(BigDecimal.ZERO) || token1Reserve.equals(BigDecimal.ZERO)) {
            return BigDecimal.ZERO;
        }
        return token0Reserve.divide(token1Reserve, 18, java.math.RoundingMode.HALF_UP);
    }
    
    public void updateReserves(BigDecimal token0Reserve, BigDecimal token1Reserve) {
        setToken0Reserve(token0Reserve);
        setToken1Reserve(token1Reserve);
    }
}