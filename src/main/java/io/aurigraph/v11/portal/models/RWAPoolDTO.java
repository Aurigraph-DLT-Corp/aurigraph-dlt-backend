package io.aurigraph.v11.portal.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.List;

public class RWAPoolDTO {
    @JsonProperty("pool_id")
    private String poolId;
    @JsonProperty("pool_name")
    private String poolName;
    @JsonProperty("asset_class")
    private String assetClass;
    @JsonProperty("total_value_locked")
    private String totalValueLocked;
    @JsonProperty("token_count")
    private Integer tokenCount;
    @JsonProperty("lp_count")
    private Integer lpCount;
    @JsonProperty("apy_percentage")
    private Double apyPercentage;
    @JsonProperty("daily_volume")
    private String dailyVolume;
    @JsonProperty("min_investment")
    private String minInvestment;
    @JsonProperty("lockup_period")
    private String lockupPeriod;
    @JsonProperty("rebalance_frequency")
    private String rebalanceFrequency;
    @JsonProperty("risk_rating")
    private String riskRating;
    @JsonProperty("diversification_score")
    private Double diversificationScore;

    public RWAPoolDTO() {}

    private RWAPoolDTO(Builder builder) {
        this.poolId = builder.poolId;
        this.poolName = builder.poolName;
        this.assetClass = builder.assetClass;
        this.totalValueLocked = builder.totalValueLocked;
        this.tokenCount = builder.tokenCount;
        this.lpCount = builder.lpCount;
        this.apyPercentage = builder.apyPercentage;
        this.dailyVolume = builder.dailyVolume;
        this.minInvestment = builder.minInvestment;
        this.lockupPeriod = builder.lockupPeriod;
        this.rebalanceFrequency = builder.rebalanceFrequency;
        this.riskRating = builder.riskRating;
        this.diversificationScore = builder.diversificationScore;
    }

    public String getPoolId() { return poolId; }
    public String getPoolName() { return poolName; }
    public String getAssetClass() { return assetClass; }
    public String getTotalValueLocked() { return totalValueLocked; }
    public Integer getTokenCount() { return tokenCount; }
    public Integer getLpCount() { return lpCount; }
    public Double getApyPercentage() { return apyPercentage; }
    public String getDailyVolume() { return dailyVolume; }
    public String getMinInvestment() { return minInvestment; }
    public String getLockupPeriod() { return lockupPeriod; }
    public String getRebalanceFrequency() { return rebalanceFrequency; }
    public String getRiskRating() { return riskRating; }
    public Double getDiversificationScore() { return diversificationScore; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String poolId;
        private String poolName;
        private String assetClass;
        private String totalValueLocked;
        private Integer tokenCount;
        private Integer lpCount;
        private Double apyPercentage;
        private String dailyVolume;
        private String minInvestment;
        private String lockupPeriod;
        private String rebalanceFrequency;
        private String riskRating;
        private Double diversificationScore;

        public Builder poolId(String poolId) { this.poolId = poolId; return this; }
        public Builder poolName(String poolName) { this.poolName = poolName; return this; }
        public Builder assetClass(String assetClass) { this.assetClass = assetClass; return this; }
        public Builder totalValueLocked(String totalValueLocked) { this.totalValueLocked = totalValueLocked; return this; }
        public Builder tokenCount(Integer tokenCount) { this.tokenCount = tokenCount; return this; }
        public Builder lpCount(Integer lpCount) { this.lpCount = lpCount; return this; }
        public Builder apyPercentage(Double apyPercentage) { this.apyPercentage = apyPercentage; return this; }
        public Builder dailyVolume(String dailyVolume) { this.dailyVolume = dailyVolume; return this; }
        public Builder minInvestment(String minInvestment) { this.minInvestment = minInvestment; return this; }
        public Builder lockupPeriod(String lockupPeriod) { this.lockupPeriod = lockupPeriod; return this; }
        public Builder rebalanceFrequency(String rebalanceFrequency) { this.rebalanceFrequency = rebalanceFrequency; return this; }
        public Builder riskRating(String riskRating) { this.riskRating = riskRating; return this; }
        public Builder diversificationScore(Double diversificationScore) { this.diversificationScore = diversificationScore; return this; }

        public RWAPoolDTO build() { return new RWAPoolDTO(this); }
    }
}
