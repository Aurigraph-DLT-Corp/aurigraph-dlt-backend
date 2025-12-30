package io.aurigraph.v11.contracts.defi.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Sprint 4 Swap Models
 * Models for DEX operations, routing, and MEV protection
 */
public class SwapModels {

    public static class SwapResult {
        @JsonProperty("amountOut")
        private BigDecimal amountOut;
        
        @JsonProperty("priceImpact")
        private BigDecimal priceImpact;
        
        @JsonProperty("executionPrice")
        private BigDecimal executionPrice;
        
        @JsonProperty("mevProtected")
        private Boolean mevProtected;
        
        @JsonProperty("slippageProtection")
        private BigDecimal slippageProtection;
        
        @JsonProperty("transactionHash")
        private String transactionHash;
        
        @JsonProperty("timestamp")
        private Instant timestamp;
        
        public SwapResult() {
            this.timestamp = Instant.now();
            this.mevProtected = false;
        }
        
        public SwapResult(BigDecimal amountOut, BigDecimal priceImpact) {
            this();
            this.amountOut = amountOut;
            this.priceImpact = priceImpact;
        }
        
        // Getters and setters
        public BigDecimal getAmountOut() { return amountOut; }
        public void setAmountOut(BigDecimal amountOut) { this.amountOut = amountOut; }
        
        public BigDecimal getPriceImpact() { return priceImpact; }
        public void setPriceImpact(BigDecimal priceImpact) { this.priceImpact = priceImpact; }
        
        public BigDecimal getExecutionPrice() { return executionPrice; }
        public void setExecutionPrice(BigDecimal executionPrice) { this.executionPrice = executionPrice; }
        
        public Boolean getMEVProtected() { return mevProtected; }
        public void setMEVProtected(Boolean mevProtected) { this.mevProtected = mevProtected; }
        
        public BigDecimal getSlippageProtection() { return slippageProtection; }
        public void setSlippageProtection(BigDecimal slippageProtection) { this.slippageProtection = slippageProtection; }
        
        public String getTransactionHash() { return transactionHash; }
        public void setTransactionHash(String transactionHash) { this.transactionHash = transactionHash; }
        
        public Instant getTimestamp() { return timestamp; }
        public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
    }
    
    public static class SwapRoute {
        @JsonProperty("path")
        private List<String> path;
        
        @JsonProperty("pools")
        private List<String> pools;
        
        @JsonProperty("outputAmount")
        private BigDecimal outputAmount;
        
        @JsonProperty("priceImpact")
        private BigDecimal priceImpact;
        
        @JsonProperty("gasEstimate")
        private BigDecimal gasEstimate;
        
        public SwapRoute() {}
        
        // Getters and setters
        public List<String> getPath() { return path; }
        public void setPath(List<String> path) { this.path = path; }
        
        public List<String> getPools() { return pools; }
        public void setPools(List<String> pools) { this.pools = pools; }
        
        public BigDecimal getOutputAmount() { return outputAmount; }
        public void setOutputAmount(BigDecimal outputAmount) { this.outputAmount = outputAmount; }
        
        public BigDecimal getPriceImpact() { return priceImpact; }
        public void setPriceImpact(BigDecimal priceImpact) { this.priceImpact = priceImpact; }
        
        public BigDecimal getGasEstimate() { return gasEstimate; }
        public void setGasEstimate(BigDecimal gasEstimate) { this.gasEstimate = gasEstimate; }
    }
    
    public static class LiquidationAlert {
        @JsonProperty("positionId")
        private String positionId;
        
        @JsonProperty("userAddress")
        private String userAddress;
        
        @JsonProperty("riskLevel")
        private String riskLevel;
        
        @JsonProperty("healthFactor")
        private BigDecimal healthFactor;
        
        @JsonProperty("alertTime")
        private Instant alertTime;
        
        public LiquidationAlert() {
            this.alertTime = Instant.now();
        }
        
        public LiquidationAlert(LoanPosition position) {
            this();
            this.positionId = position.getPositionId();
            this.userAddress = position.getUserAddress();
            this.riskLevel = position.getRiskLevel() != null ? position.getRiskLevel().name() : "UNKNOWN";
            this.healthFactor = position.getHealthFactor();
        }
        
        // Getters and setters
        public String getPositionId() { return positionId; }
        public void setPositionId(String positionId) { this.positionId = positionId; }
        
        public String getUserAddress() { return userAddress; }
        public void setUserAddress(String userAddress) { this.userAddress = userAddress; }
        
        public String getRiskLevel() { return riskLevel; }
        public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }
        
        public BigDecimal getHealthFactor() { return healthFactor; }
        public void setHealthFactor(BigDecimal healthFactor) { this.healthFactor = healthFactor; }
        
        public Instant getAlertTime() { return alertTime; }
        public void setAlertTime(Instant alertTime) { this.alertTime = alertTime; }
    }
    
    public static class YieldOpportunity {
        @JsonProperty("protocol")
        private String protocol;
        
        @JsonProperty("farmId")
        private String farmId;
        
        @JsonProperty("stakingToken")
        private String stakingToken;
        
        @JsonProperty("baseAPR")
        private BigDecimal baseAPR;
        
        @JsonProperty("boostedAPR")
        private BigDecimal boostedAPR;
        
        @JsonProperty("riskAdjustedAPR")
        private BigDecimal riskAdjustedAPR;
        
        @JsonProperty("riskScore")
        private BigDecimal riskScore;
        
        @JsonProperty("totalValueLocked")
        private BigDecimal totalValueLocked;
        
        public YieldOpportunity() {}
        
        // Getters and setters
        public String getProtocol() { return protocol; }
        public void setProtocol(String protocol) { this.protocol = protocol; }
        
        public String getFarmId() { return farmId; }
        public void setFarmId(String farmId) { this.farmId = farmId; }
        
        public String getStakingToken() { return stakingToken; }
        public void setStakingToken(String stakingToken) { this.stakingToken = stakingToken; }
        
        public BigDecimal getBaseAPR() { return baseAPR; }
        public void setBaseAPR(BigDecimal baseAPR) { this.baseAPR = baseAPR; }
        
        public BigDecimal getBoostedAPR() { return boostedAPR; }
        public void setBoostedAPR(BigDecimal boostedAPR) { this.boostedAPR = boostedAPR; }
        
        public BigDecimal getRiskAdjustedAPR() { return riskAdjustedAPR; }
        public void setRiskAdjustedAPR(BigDecimal riskAdjustedAPR) { this.riskAdjustedAPR = riskAdjustedAPR; }
        
        public BigDecimal getRiskScore() { return riskScore; }
        public void setRiskScore(BigDecimal riskScore) { this.riskScore = riskScore; }
        
        public BigDecimal getTotalValueLocked() { return totalValueLocked; }
        public void setTotalValueLocked(BigDecimal totalValueLocked) { this.totalValueLocked = totalValueLocked; }
    }
}