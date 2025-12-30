package io.aurigraph.v11.contracts.defi.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

/**
 * Sprint 4 DeFi Request/Response Models
 * All request and response classes for DeFi operations
 */
public class DeFiRequests {

    // Request Models
    public static class AddLiquidityRequest {
        @JsonProperty("poolId")
        private String poolId;
        
        @JsonProperty("userAddress")
        private String userAddress;
        
        @JsonProperty("token0Amount")
        private BigDecimal token0Amount;
        
        @JsonProperty("token1Amount")
        private BigDecimal token1Amount;
        
        @JsonProperty("minToken0")
        private BigDecimal minToken0;
        
        @JsonProperty("minToken1")
        private BigDecimal minToken1;
        
        @JsonProperty("enableILProtection")
        private boolean enableILProtection;
        
        // Constructors
        public AddLiquidityRequest() {}
        
        // Getters and setters
        public String getPoolId() { return poolId; }
        public void setPoolId(String poolId) { this.poolId = poolId; }
        
        public String getUserAddress() { return userAddress; }
        public void setUserAddress(String userAddress) { this.userAddress = userAddress; }
        
        public BigDecimal getToken0Amount() { return token0Amount; }
        public void setToken0Amount(BigDecimal token0Amount) { this.token0Amount = token0Amount; }
        
        public BigDecimal getToken1Amount() { return token1Amount; }
        public void setToken1Amount(BigDecimal token1Amount) { this.token1Amount = token1Amount; }
        
        public BigDecimal getMinToken0() { return minToken0; }
        public void setMinToken0(BigDecimal minToken0) { this.minToken0 = minToken0; }
        
        public BigDecimal getMinToken1() { return minToken1; }
        public void setMinToken1(BigDecimal minToken1) { this.minToken1 = minToken1; }
        
        public boolean isEnableILProtection() { return enableILProtection; }
        public void setEnableILProtection(boolean enableILProtection) { this.enableILProtection = enableILProtection; }
    }
    
    public static class RemoveLiquidityRequest {
        @JsonProperty("positionId")
        private String positionId;
        
        @JsonProperty("lpTokenAmount")
        private BigDecimal lpTokenAmount;
        
        // Constructors
        public RemoveLiquidityRequest() {}
        
        // Getters and setters
        public String getPositionId() { return positionId; }
        public void setPositionId(String positionId) { this.positionId = positionId; }
        
        public BigDecimal getLpTokenAmount() { return lpTokenAmount; }
        public void setLpTokenAmount(BigDecimal lpTokenAmount) { this.lpTokenAmount = lpTokenAmount; }
    }
    
    public static class StakeRequest {
        @JsonProperty("farmId")
        private String farmId;
        
        @JsonProperty("userAddress")
        private String userAddress;
        
        @JsonProperty("amount")
        private BigDecimal amount;
        
        @JsonProperty("enableAutoCompound")
        private boolean enableAutoCompound;
        
        @JsonProperty("compoundFrequency")
        private Long compoundFrequency;
        
        // Constructors
        public StakeRequest() {}
        
        // Getters and setters
        public String getFarmId() { return farmId; }
        public void setFarmId(String farmId) { this.farmId = farmId; }
        
        public String getUserAddress() { return userAddress; }
        public void setUserAddress(String userAddress) { this.userAddress = userAddress; }
        
        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }
        
        public boolean isEnableAutoCompound() { return enableAutoCompound; }
        public void setEnableAutoCompound(boolean enableAutoCompound) { this.enableAutoCompound = enableAutoCompound; }
        
        public Long getCompoundFrequency() { return compoundFrequency; }
        public void setCompoundFrequency(Long compoundFrequency) { this.compoundFrequency = compoundFrequency; }
    }
    
    // LendingRequest moved to separate file - io.aurigraph.v11.contracts.defi.models.LendingRequest
    
    // SwapRequest moved to separate file - io.aurigraph.v11.contracts.defi.models.SwapRequest
    
    // Response Models
    public static class LiquidityPositionResponse {
        @JsonProperty("position")
        private LiquidityPosition position;
        
        @JsonProperty("impermanentLossRisk")
        private BigDecimal impermanentLossRisk;
        
        public LiquidityPositionResponse() {}
        
        public LiquidityPositionResponse(LiquidityPosition position, BigDecimal impermanentLossRisk) {
            this.position = position;
            this.impermanentLossRisk = impermanentLossRisk;
        }
        
        public LiquidityPosition getPosition() { return position; }
        public void setPosition(LiquidityPosition position) { this.position = position; }
        
        public BigDecimal getImpermanentLossRisk() { return impermanentLossRisk; }
        public void setImpermanentLossRisk(BigDecimal impermanentLossRisk) { this.impermanentLossRisk = impermanentLossRisk; }
    }
    
    public static class WithdrawLiquidityResponse {
        @JsonProperty("token0Amount")
        private BigDecimal token0Amount;
        
        @JsonProperty("token1Amount")
        private BigDecimal token1Amount;
        
        @JsonProperty("feesEarned")
        private BigDecimal feesEarned;
        
        public WithdrawLiquidityResponse() {}
        
        public WithdrawLiquidityResponse(BigDecimal token0Amount, BigDecimal token1Amount, BigDecimal feesEarned) {
            this.token0Amount = token0Amount;
            this.token1Amount = token1Amount;
            this.feesEarned = feesEarned;
        }
        
        public BigDecimal getToken0Amount() { return token0Amount; }
        public void setToken0Amount(BigDecimal token0Amount) { this.token0Amount = token0Amount; }
        
        public BigDecimal getToken1Amount() { return token1Amount; }
        public void setToken1Amount(BigDecimal token1Amount) { this.token1Amount = token1Amount; }
        
        public BigDecimal getFeesEarned() { return feesEarned; }
        public void setFeesEarned(BigDecimal feesEarned) { this.feesEarned = feesEarned; }
    }
    
    public static class YieldFarmingResponse {
        @JsonProperty("rewards")
        private YieldFarmRewards rewards;
        
        public YieldFarmingResponse() {}
        
        public YieldFarmingResponse(YieldFarmRewards rewards) {
            this.rewards = rewards;
        }
        
        public YieldFarmRewards getRewards() { return rewards; }
        public void setRewards(YieldFarmRewards rewards) { this.rewards = rewards; }
    }
    
    public static class LoanPositionResponse {
        @JsonProperty("position")
        private LoanPosition position;
        
        @JsonProperty("riskScore")
        private BigDecimal riskScore;
        
        public LoanPositionResponse() {}
        
        public LoanPositionResponse(LoanPosition position, BigDecimal riskScore) {
            this.position = position;
            this.riskScore = riskScore;
        }
        
        public LoanPosition getPosition() { return position; }
        public void setPosition(LoanPosition position) { this.position = position; }
        
        public BigDecimal getRiskScore() { return riskScore; }
        public void setRiskScore(BigDecimal riskScore) { this.riskScore = riskScore; }
    }
    
    public static class SwapResponse {
        @JsonProperty("result")
        private SwapResult result;
        
        public SwapResponse() {}
        
        public SwapResponse(SwapResult result) {
            this.result = result;
        }
        
        public SwapResult getResult() { return result; }
        public void setResult(SwapResult result) { this.result = result; }
    }
}