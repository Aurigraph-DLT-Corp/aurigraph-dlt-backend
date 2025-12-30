package io.aurigraph.v11.portal.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.List;

public class RWATokenizationDTO {
    @JsonProperty("total_r_w_a_value")
    private String totalRWAValue;
    @JsonProperty("total_tokenized_value")
    private String totalTokenizedValue;
    @JsonProperty("tokenization_ratio")
    private Double tokenizationRatio;
    @JsonProperty("total_r_w_a_tokens")
    private Integer totalRWATokens;
    @JsonProperty("active_r_w_a_tokens")
    private Integer activeRWATokens;
    @JsonProperty("paused_r_w_a_tokens")
    private Integer pausedRWATokens;
    @JsonProperty("total_fractional_tokens")
    private Integer totalFractionalTokens;
    @JsonProperty("total_unique_holders")
    private Integer totalUniqueHolders;
    @JsonProperty("average_holding_size")
    private String averageHoldingSize;
    @JsonProperty("total_value_locked")
    private String totalValueLocked;
    @JsonProperty("pool_count")
    private Integer poolCount;
    @JsonProperty("verified_assets")
    private Integer verifiedAssets;
    @JsonProperty("audit_compliance")
    private Double auditCompliance;
    @JsonProperty("registry_status")
    private String registryStatus;
    @JsonProperty("merkle_tree_height")
    private Integer merkleTreeHeight;
    @JsonProperty("verification_latency")
    private Long verificationLatency;
    @JsonProperty("error")
    private String error;

    public RWATokenizationDTO() {}

    private RWATokenizationDTO(Builder builder) {
        this.totalRWAValue = builder.totalRWAValue;
        this.totalTokenizedValue = builder.totalTokenizedValue;
        this.tokenizationRatio = builder.tokenizationRatio;
        this.totalRWATokens = builder.totalRWATokens;
        this.activeRWATokens = builder.activeRWATokens;
        this.pausedRWATokens = builder.pausedRWATokens;
        this.totalFractionalTokens = builder.totalFractionalTokens;
        this.totalUniqueHolders = builder.totalUniqueHolders;
        this.averageHoldingSize = builder.averageHoldingSize;
        this.totalValueLocked = builder.totalValueLocked;
        this.poolCount = builder.poolCount;
        this.verifiedAssets = builder.verifiedAssets;
        this.auditCompliance = builder.auditCompliance;
        this.registryStatus = builder.registryStatus;
        this.merkleTreeHeight = builder.merkleTreeHeight;
        this.verificationLatency = builder.verificationLatency;
        this.error = builder.error;
    }

    public String getTotalRWAValue() { return totalRWAValue; }
    public String getTotalTokenizedValue() { return totalTokenizedValue; }
    public Double getTokenizationRatio() { return tokenizationRatio; }
    public Integer getTotalRWATokens() { return totalRWATokens; }
    public Integer getActiveRWATokens() { return activeRWATokens; }
    public Integer getPausedRWATokens() { return pausedRWATokens; }
    public Integer getTotalFractionalTokens() { return totalFractionalTokens; }
    public Integer getTotalUniqueHolders() { return totalUniqueHolders; }
    public String getAverageHoldingSize() { return averageHoldingSize; }
    public String getTotalValueLocked() { return totalValueLocked; }
    public Integer getPoolCount() { return poolCount; }
    public Integer getVerifiedAssets() { return verifiedAssets; }
    public Double getAuditCompliance() { return auditCompliance; }
    public String getRegistryStatus() { return registryStatus; }
    public Integer getMerkleTreeHeight() { return merkleTreeHeight; }
    public Long getVerificationLatency() { return verificationLatency; }
    public String getError() { return error; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String totalRWAValue;
        private String totalTokenizedValue;
        private Double tokenizationRatio;
        private Integer totalRWATokens;
        private Integer activeRWATokens;
        private Integer pausedRWATokens;
        private Integer totalFractionalTokens;
        private Integer totalUniqueHolders;
        private String averageHoldingSize;
        private String totalValueLocked;
        private Integer poolCount;
        private Integer verifiedAssets;
        private Double auditCompliance;
        private String registryStatus;
        private Integer merkleTreeHeight;
        private Long verificationLatency;
        private String error;

        public Builder totalRWAValue(String totalRWAValue) { this.totalRWAValue = totalRWAValue; return this; }
        public Builder totalTokenizedValue(String totalTokenizedValue) { this.totalTokenizedValue = totalTokenizedValue; return this; }
        public Builder tokenizationRatio(Double tokenizationRatio) { this.tokenizationRatio = tokenizationRatio; return this; }
        public Builder totalRWATokens(Integer totalRWATokens) { this.totalRWATokens = totalRWATokens; return this; }
        public Builder activeRWATokens(Integer activeRWATokens) { this.activeRWATokens = activeRWATokens; return this; }
        public Builder pausedRWATokens(Integer pausedRWATokens) { this.pausedRWATokens = pausedRWATokens; return this; }
        public Builder totalFractionalTokens(Integer totalFractionalTokens) { this.totalFractionalTokens = totalFractionalTokens; return this; }
        public Builder totalUniqueHolders(Integer totalUniqueHolders) { this.totalUniqueHolders = totalUniqueHolders; return this; }
        public Builder averageHoldingSize(String averageHoldingSize) { this.averageHoldingSize = averageHoldingSize; return this; }
        public Builder totalValueLocked(String totalValueLocked) { this.totalValueLocked = totalValueLocked; return this; }
        public Builder poolCount(Integer poolCount) { this.poolCount = poolCount; return this; }
        public Builder verifiedAssets(Integer verifiedAssets) { this.verifiedAssets = verifiedAssets; return this; }
        public Builder auditCompliance(Double auditCompliance) { this.auditCompliance = auditCompliance; return this; }
        public Builder registryStatus(String registryStatus) { this.registryStatus = registryStatus; return this; }
        public Builder merkleTreeHeight(Integer merkleTreeHeight) { this.merkleTreeHeight = merkleTreeHeight; return this; }
        public Builder verificationLatency(Long verificationLatency) { this.verificationLatency = verificationLatency; return this; }
        public Builder error(String error) { this.error = error; return this; }

        public RWATokenizationDTO build() { return new RWATokenizationDTO(this); }
    }
}
