package io.aurigraph.v11.contracts.defi.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Sprint 4 Liquidity Pool Result Model
 * Result of liquidity pool operations
 */
public class LiquidityPoolResult {
    
    @JsonProperty("success")
    private boolean success;
    
    @JsonProperty("transactionHash")
    private String transactionHash;
    
    @JsonProperty("poolId")
    private String poolId;
    
    @JsonProperty("userAddress")
    private String userAddress;
    
    @JsonProperty("operation")
    private LiquidityPoolRequest.PoolOperation operation;
    
    @JsonProperty("lpTokensReceived")
    private BigDecimal lpTokensReceived;
    
    @JsonProperty("token0Amount")
    private BigDecimal token0Amount;
    
    @JsonProperty("token1Amount")
    private BigDecimal token1Amount;
    
    @JsonProperty("priceImpact")
    private BigDecimal priceImpact;
    
    @JsonProperty("feesIncurred")
    private BigDecimal feesIncurred;
    
    @JsonProperty("gasUsed")
    private BigDecimal gasUsed;
    
    @JsonProperty("executionPrice")
    private BigDecimal executionPrice;
    
    @JsonProperty("timestamp")
    private Instant timestamp;
    
    @JsonProperty("errorMessage")
    private String errorMessage;
    
    // Constructors
    public LiquidityPoolResult() {
        this.timestamp = Instant.now();
        this.success = true;
    }
    
    public LiquidityPoolResult(boolean success) {
        this();
        this.success = success;
    }
    
    public LiquidityPoolResult(String poolId, String userAddress, LiquidityPoolRequest.PoolOperation operation) {
        this();
        this.poolId = poolId;
        this.userAddress = userAddress;
        this.operation = operation;
    }
    
    // Static factory methods
    public static LiquidityPoolResult success(String poolId, String userAddress, 
                                            LiquidityPoolRequest.PoolOperation operation) {
        return new LiquidityPoolResult(poolId, userAddress, operation);
    }
    
    public static LiquidityPoolResult failure(String errorMessage) {
        LiquidityPoolResult result = new LiquidityPoolResult(false);
        result.setErrorMessage(errorMessage);
        return result;
    }
    
    // Getters and Setters
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    
    public String getTransactionHash() { return transactionHash; }
    public void setTransactionHash(String transactionHash) { this.transactionHash = transactionHash; }
    
    public String getPoolId() { return poolId; }
    public void setPoolId(String poolId) { this.poolId = poolId; }
    
    public String getUserAddress() { return userAddress; }
    public void setUserAddress(String userAddress) { this.userAddress = userAddress; }
    
    public LiquidityPoolRequest.PoolOperation getOperation() { return operation; }
    public void setOperation(LiquidityPoolRequest.PoolOperation operation) { this.operation = operation; }
    
    public BigDecimal getLpTokensReceived() { return lpTokensReceived; }
    public void setLpTokensReceived(BigDecimal lpTokensReceived) { this.lpTokensReceived = lpTokensReceived; }
    
    public BigDecimal getToken0Amount() { return token0Amount; }
    public void setToken0Amount(BigDecimal token0Amount) { this.token0Amount = token0Amount; }
    
    public BigDecimal getToken1Amount() { return token1Amount; }
    public void setToken1Amount(BigDecimal token1Amount) { this.token1Amount = token1Amount; }
    
    public BigDecimal getPriceImpact() { return priceImpact; }
    public void setPriceImpact(BigDecimal priceImpact) { this.priceImpact = priceImpact; }
    
    public BigDecimal getFeesIncurred() { return feesIncurred; }
    public void setFeesIncurred(BigDecimal feesIncurred) { this.feesIncurred = feesIncurred; }
    
    public BigDecimal getGasUsed() { return gasUsed; }
    public void setGasUsed(BigDecimal gasUsed) { this.gasUsed = gasUsed; }
    
    public BigDecimal getExecutionPrice() { return executionPrice; }
    public void setExecutionPrice(BigDecimal executionPrice) { this.executionPrice = executionPrice; }
    
    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
    
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
}