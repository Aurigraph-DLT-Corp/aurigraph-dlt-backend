package io.aurigraph.v11.contracts.defi.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Sprint 4 DeFi Statistics Model
 * Comprehensive DeFi protocol statistics and analytics
 */
public class DeFiStats {
    
    @JsonProperty("protocol")
    private String protocol;
    
    @JsonProperty("totalValueLocked")
    private BigDecimal totalValueLocked;
    
    @JsonProperty("totalLiquidityUSD")
    private BigDecimal totalLiquidityUSD;
    
    @JsonProperty("totalBorrowedUSD")
    private BigDecimal totalBorrowedUSD;
    
    @JsonProperty("totalStakedUSD")
    private BigDecimal totalStakedUSD;
    
    @JsonProperty("totalVolumeUSD24h")
    private BigDecimal totalVolumeUSD24h;
    
    @JsonProperty("totalFeesUSD24h")
    private BigDecimal totalFeesUSD24h;
    
    @JsonProperty("uniqueUsers24h")
    private Long uniqueUsers24h;
    
    @JsonProperty("totalTransactions24h")
    private Long totalTransactions24h;
    
    @JsonProperty("averageAPR")
    private BigDecimal averageAPR;
    
    @JsonProperty("averageAPY")
    private BigDecimal averageAPY;
    
    @JsonProperty("totalPools")
    private Integer totalPools;
    
    @JsonProperty("activePools")
    private Integer activePools;
    
    @JsonProperty("totalFarms")
    private Integer totalFarms;
    
    @JsonProperty("activeFarms")
    private Integer activeFarms;
    
    @JsonProperty("totalLendingPositions")
    private Integer totalLendingPositions;
    
    @JsonProperty("activeLendingPositions")
    private Integer activeLendingPositions;
    
    @JsonProperty("healthFactor")
    private BigDecimal healthFactor;
    
    @JsonProperty("utilizationRate")
    private BigDecimal utilizationRate;
    
    @JsonProperty("liquidationThreshold")
    private BigDecimal liquidationThreshold;
    
    @JsonProperty("totalLiquidations24h")
    private Integer totalLiquidations24h;
    
    @JsonProperty("totalLiquidationValue24h")
    private BigDecimal totalLiquidationValue24h;
    
    @JsonProperty("impermanentLossTotal")
    private BigDecimal impermanentLossTotal;
    
    @JsonProperty("protocolRevenue24h")
    private BigDecimal protocolRevenue24h;
    
    @JsonProperty("timestamp")
    private Instant timestamp;
    
    @JsonProperty("tokenStats")
    private Map<String, TokenStats> tokenStats;
    
    @JsonProperty("poolStats")
    private Map<String, PoolStats> poolStats;
    
    @JsonProperty("farmStats")
    private Map<String, FarmStats> farmStats;
    
    @JsonProperty("riskMetrics")
    private RiskMetrics riskMetrics;
    
    // Nested classes for detailed statistics
    public static class TokenStats {
        @JsonProperty("token")
        private String token;
        
        @JsonProperty("totalLiquidity")
        private BigDecimal totalLiquidity;
        
        @JsonProperty("totalSupplied")
        private BigDecimal totalSupplied;
        
        @JsonProperty("totalBorrowed")
        private BigDecimal totalBorrowed;
        
        @JsonProperty("supplyAPY")
        private BigDecimal supplyAPY;
        
        @JsonProperty("borrowAPY")
        private BigDecimal borrowAPY;
        
        @JsonProperty("utilizationRate")
        private BigDecimal utilizationRate;
        
        @JsonProperty("price")
        private BigDecimal price;
        
        @JsonProperty("priceChange24h")
        private BigDecimal priceChange24h;
        
        // Constructors, getters, and setters
        public TokenStats() {}
        
        public TokenStats(String token) {
            this.token = token;
        }
        
        // Getters and Setters
        public String getToken() { return token; }
        public void setToken(String token) { this.token = token; }
        
        public BigDecimal getTotalLiquidity() { return totalLiquidity; }
        public void setTotalLiquidity(BigDecimal totalLiquidity) { this.totalLiquidity = totalLiquidity; }
        
        public BigDecimal getTotalSupplied() { return totalSupplied; }
        public void setTotalSupplied(BigDecimal totalSupplied) { this.totalSupplied = totalSupplied; }
        
        public BigDecimal getTotalBorrowed() { return totalBorrowed; }
        public void setTotalBorrowed(BigDecimal totalBorrowed) { this.totalBorrowed = totalBorrowed; }
        
        public BigDecimal getSupplyAPY() { return supplyAPY; }
        public void setSupplyAPY(BigDecimal supplyAPY) { this.supplyAPY = supplyAPY; }
        
        public BigDecimal getBorrowAPY() { return borrowAPY; }
        public void setBorrowAPY(BigDecimal borrowAPY) { this.borrowAPY = borrowAPY; }
        
        public BigDecimal getUtilizationRate() { return utilizationRate; }
        public void setUtilizationRate(BigDecimal utilizationRate) { this.utilizationRate = utilizationRate; }
        
        public BigDecimal getPrice() { return price; }
        public void setPrice(BigDecimal price) { this.price = price; }
        
        public BigDecimal getPriceChange24h() { return priceChange24h; }
        public void setPriceChange24h(BigDecimal priceChange24h) { this.priceChange24h = priceChange24h; }
    }
    
    public static class PoolStats {
        @JsonProperty("poolId")
        private String poolId;
        
        @JsonProperty("tvl")
        private BigDecimal tvl;
        
        @JsonProperty("volume24h")
        private BigDecimal volume24h;
        
        @JsonProperty("fees24h")
        private BigDecimal fees24h;
        
        @JsonProperty("apr")
        private BigDecimal apr;
        
        @JsonProperty("liquidityProviders")
        private Integer liquidityProviders;
        
        // Constructors, getters, and setters
        public PoolStats() {}
        
        public PoolStats(String poolId) {
            this.poolId = poolId;
        }
        
        public String getPoolId() { return poolId; }
        public void setPoolId(String poolId) { this.poolId = poolId; }
        
        public BigDecimal getTvl() { return tvl; }
        public void setTvl(BigDecimal tvl) { this.tvl = tvl; }
        
        public BigDecimal getVolume24h() { return volume24h; }
        public void setVolume24h(BigDecimal volume24h) { this.volume24h = volume24h; }
        
        public BigDecimal getFees24h() { return fees24h; }
        public void setFees24h(BigDecimal fees24h) { this.fees24h = fees24h; }
        
        public BigDecimal getApr() { return apr; }
        public void setApr(BigDecimal apr) { this.apr = apr; }
        
        public Integer getLiquidityProviders() { return liquidityProviders; }
        public void setLiquidityProviders(Integer liquidityProviders) { this.liquidityProviders = liquidityProviders; }
    }
    
    public static class FarmStats {
        @JsonProperty("farmId")
        private String farmId;
        
        @JsonProperty("tvl")
        private BigDecimal tvl;
        
        @JsonProperty("apy")
        private BigDecimal apy;
        
        @JsonProperty("totalStakers")
        private Integer totalStakers;
        
        @JsonProperty("dailyRewards")
        private BigDecimal dailyRewards;
        
        // Constructors, getters, and setters
        public FarmStats() {}
        
        public FarmStats(String farmId) {
            this.farmId = farmId;
        }
        
        public String getFarmId() { return farmId; }
        public void setFarmId(String farmId) { this.farmId = farmId; }
        
        public BigDecimal getTvl() { return tvl; }
        public void setTvl(BigDecimal tvl) { this.tvl = tvl; }
        
        public BigDecimal getApy() { return apy; }
        public void setApy(BigDecimal apy) { this.apy = apy; }
        
        public Integer getTotalStakers() { return totalStakers; }
        public void setTotalStakers(Integer totalStakers) { this.totalStakers = totalStakers; }
        
        public BigDecimal getDailyRewards() { return dailyRewards; }
        public void setDailyRewards(BigDecimal dailyRewards) { this.dailyRewards = dailyRewards; }
    }
    
    public static class RiskMetrics {
        @JsonProperty("totalRiskExposure")
        private BigDecimal totalRiskExposure;
        
        @JsonProperty("averageHealthFactor")
        private BigDecimal averageHealthFactor;
        
        @JsonProperty("positionsAtRisk")
        private Integer positionsAtRisk;
        
        @JsonProperty("concentrationRisk")
        private BigDecimal concentrationRisk;
        
        @JsonProperty("volatilityScore")
        private BigDecimal volatilityScore;
        
        // Constructors, getters, and setters
        public RiskMetrics() {}
        
        public BigDecimal getTotalRiskExposure() { return totalRiskExposure; }
        public void setTotalRiskExposure(BigDecimal totalRiskExposure) { this.totalRiskExposure = totalRiskExposure; }
        
        public BigDecimal getAverageHealthFactor() { return averageHealthFactor; }
        public void setAverageHealthFactor(BigDecimal averageHealthFactor) { this.averageHealthFactor = averageHealthFactor; }
        
        public Integer getPositionsAtRisk() { return positionsAtRisk; }
        public void setPositionsAtRisk(Integer positionsAtRisk) { this.positionsAtRisk = positionsAtRisk; }
        
        public BigDecimal getConcentrationRisk() { return concentrationRisk; }
        public void setConcentrationRisk(BigDecimal concentrationRisk) { this.concentrationRisk = concentrationRisk; }
        
        public BigDecimal getVolatilityScore() { return volatilityScore; }
        public void setVolatilityScore(BigDecimal volatilityScore) { this.volatilityScore = volatilityScore; }
    }
    
    // Main class constructors
    public DeFiStats() {
        this.timestamp = Instant.now();
        this.tokenStats = new HashMap<>();
        this.poolStats = new HashMap<>();
        this.farmStats = new HashMap<>();
        this.riskMetrics = new RiskMetrics();
        this.totalValueLocked = BigDecimal.ZERO;
        this.totalLiquidityUSD = BigDecimal.ZERO;
        this.totalBorrowedUSD = BigDecimal.ZERO;
        this.totalStakedUSD = BigDecimal.ZERO;
        this.totalVolumeUSD24h = BigDecimal.ZERO;
        this.totalFeesUSD24h = BigDecimal.ZERO;
        this.uniqueUsers24h = 0L;
        this.totalTransactions24h = 0L;
        this.averageAPR = BigDecimal.ZERO;
        this.averageAPY = BigDecimal.ZERO;
        this.totalPools = 0;
        this.activePools = 0;
        this.totalFarms = 0;
        this.activeFarms = 0;
        this.totalLendingPositions = 0;
        this.activeLendingPositions = 0;
        this.healthFactor = BigDecimal.ZERO;
        this.utilizationRate = BigDecimal.ZERO;
        this.liquidationThreshold = BigDecimal.ZERO;
        this.totalLiquidations24h = 0;
        this.totalLiquidationValue24h = BigDecimal.ZERO;
        this.impermanentLossTotal = BigDecimal.ZERO;
        this.protocolRevenue24h = BigDecimal.ZERO;
    }
    
    public DeFiStats(String protocol) {
        this();
        this.protocol = protocol;
    }
    
    // Getters and Setters
    public String getProtocol() { return protocol; }
    public void setProtocol(String protocol) { this.protocol = protocol; }
    
    public BigDecimal getTotalValueLocked() { return totalValueLocked; }
    public void setTotalValueLocked(BigDecimal totalValueLocked) { this.totalValueLocked = totalValueLocked; }
    
    public BigDecimal getTotalLiquidityUSD() { return totalLiquidityUSD; }
    public void setTotalLiquidityUSD(BigDecimal totalLiquidityUSD) { this.totalLiquidityUSD = totalLiquidityUSD; }
    
    public BigDecimal getTotalBorrowedUSD() { return totalBorrowedUSD; }
    public void setTotalBorrowedUSD(BigDecimal totalBorrowedUSD) { this.totalBorrowedUSD = totalBorrowedUSD; }
    
    public BigDecimal getTotalStakedUSD() { return totalStakedUSD; }
    public void setTotalStakedUSD(BigDecimal totalStakedUSD) { this.totalStakedUSD = totalStakedUSD; }
    
    public BigDecimal getTotalVolumeUSD24h() { return totalVolumeUSD24h; }
    public void setTotalVolumeUSD24h(BigDecimal totalVolumeUSD24h) { this.totalVolumeUSD24h = totalVolumeUSD24h; }
    
    public BigDecimal getTotalFeesUSD24h() { return totalFeesUSD24h; }
    public void setTotalFeesUSD24h(BigDecimal totalFeesUSD24h) { this.totalFeesUSD24h = totalFeesUSD24h; }
    
    public Long getUniqueUsers24h() { return uniqueUsers24h; }
    public void setUniqueUsers24h(Long uniqueUsers24h) { this.uniqueUsers24h = uniqueUsers24h; }
    
    public Long getTotalTransactions24h() { return totalTransactions24h; }
    public void setTotalTransactions24h(Long totalTransactions24h) { this.totalTransactions24h = totalTransactions24h; }
    
    public BigDecimal getAverageAPR() { return averageAPR; }
    public void setAverageAPR(BigDecimal averageAPR) { this.averageAPR = averageAPR; }
    
    public BigDecimal getAverageAPY() { return averageAPY; }
    public void setAverageAPY(BigDecimal averageAPY) { this.averageAPY = averageAPY; }
    
    public Integer getTotalPools() { return totalPools; }
    public void setTotalPools(Integer totalPools) { this.totalPools = totalPools; }
    
    public Integer getActivePools() { return activePools; }
    public void setActivePools(Integer activePools) { this.activePools = activePools; }
    
    public Integer getTotalFarms() { return totalFarms; }
    public void setTotalFarms(Integer totalFarms) { this.totalFarms = totalFarms; }
    
    public Integer getActiveFarms() { return activeFarms; }
    public void setActiveFarms(Integer activeFarms) { this.activeFarms = activeFarms; }
    
    public Integer getTotalLendingPositions() { return totalLendingPositions; }
    public void setTotalLendingPositions(Integer totalLendingPositions) { this.totalLendingPositions = totalLendingPositions; }
    
    public Integer getActiveLendingPositions() { return activeLendingPositions; }
    public void setActiveLendingPositions(Integer activeLendingPositions) { this.activeLendingPositions = activeLendingPositions; }
    
    public BigDecimal getHealthFactor() { return healthFactor; }
    public void setHealthFactor(BigDecimal healthFactor) { this.healthFactor = healthFactor; }
    
    public BigDecimal getUtilizationRate() { return utilizationRate; }
    public void setUtilizationRate(BigDecimal utilizationRate) { this.utilizationRate = utilizationRate; }
    
    public BigDecimal getLiquidationThreshold() { return liquidationThreshold; }
    public void setLiquidationThreshold(BigDecimal liquidationThreshold) { this.liquidationThreshold = liquidationThreshold; }
    
    public Integer getTotalLiquidations24h() { return totalLiquidations24h; }
    public void setTotalLiquidations24h(Integer totalLiquidations24h) { this.totalLiquidations24h = totalLiquidations24h; }
    
    public BigDecimal getTotalLiquidationValue24h() { return totalLiquidationValue24h; }
    public void setTotalLiquidationValue24h(BigDecimal totalLiquidationValue24h) { this.totalLiquidationValue24h = totalLiquidationValue24h; }
    
    public BigDecimal getImpermanentLossTotal() { return impermanentLossTotal; }
    public void setImpermanentLossTotal(BigDecimal impermanentLossTotal) { this.impermanentLossTotal = impermanentLossTotal; }
    
    public BigDecimal getProtocolRevenue24h() { return protocolRevenue24h; }
    public void setProtocolRevenue24h(BigDecimal protocolRevenue24h) { this.protocolRevenue24h = protocolRevenue24h; }
    
    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
    
    public Map<String, TokenStats> getTokenStats() { return tokenStats; }
    public void setTokenStats(Map<String, TokenStats> tokenStats) { this.tokenStats = tokenStats; }
    
    public Map<String, PoolStats> getPoolStats() { return poolStats; }
    public void setPoolStats(Map<String, PoolStats> poolStats) { this.poolStats = poolStats; }
    
    public Map<String, FarmStats> getFarmStats() { return farmStats; }
    public void setFarmStats(Map<String, FarmStats> farmStats) { this.farmStats = farmStats; }
    
    public RiskMetrics getRiskMetrics() { return riskMetrics; }
    public void setRiskMetrics(RiskMetrics riskMetrics) { this.riskMetrics = riskMetrics; }
    
    // Helper methods
    public void addTokenStats(String token, TokenStats stats) {
        if (tokenStats == null) {
            tokenStats = new HashMap<>();
        }
        tokenStats.put(token, stats);
    }
    
    public void addPoolStats(String poolId, PoolStats stats) {
        if (poolStats == null) {
            poolStats = new HashMap<>();
        }
        poolStats.put(poolId, stats);
    }
    
    public void addFarmStats(String farmId, FarmStats stats) {
        if (farmStats == null) {
            farmStats = new HashMap<>();
        }
        farmStats.put(farmId, stats);
    }
    
    public BigDecimal calculateOverallTVL() {
        return totalLiquidityUSD.add(totalBorrowedUSD).add(totalStakedUSD);
    }
    
    public BigDecimal calculateDailyVolume() {
        return totalVolumeUSD24h != null ? totalVolumeUSD24h : BigDecimal.ZERO;
    }
    
    public boolean isHealthy() {
        return healthFactor != null && healthFactor.compareTo(BigDecimal.valueOf(1.5)) > 0 &&
               utilizationRate != null && utilizationRate.compareTo(BigDecimal.valueOf(0.9)) < 0;
    }
}