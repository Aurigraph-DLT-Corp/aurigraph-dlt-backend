package io.aurigraph.v11.contracts.defi.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

/**
 * Sprint 4 Liquidity Pool Request Model
 * Generic request for liquidity pool operations
 */
public class LiquidityPoolRequest {
    
    @JsonProperty("poolId")
    private String poolId;
    
    @JsonProperty("userAddress")
    private String userAddress;
    
    @JsonProperty("operation")
    private PoolOperation operation;
    
    @JsonProperty("amount")
    private BigDecimal amount;
    
    @JsonProperty("slippageTolerance")
    private BigDecimal slippageTolerance;
    
    @JsonProperty("deadline")
    private Long deadline;
    
    public enum PoolOperation {
        ADD_LIQUIDITY,
        REMOVE_LIQUIDITY,
        SWAP,
        QUERY_PRICE,
        GET_RESERVES
    }
    
    // Constructors
    public LiquidityPoolRequest() {
        this.slippageTolerance = BigDecimal.valueOf(0.005); // 0.5% default slippage
        this.deadline = System.currentTimeMillis() + 1800000; // 30 minutes default
    }
    
    public LiquidityPoolRequest(String poolId, String userAddress, PoolOperation operation) {
        this();
        this.poolId = poolId;
        this.userAddress = userAddress;
        this.operation = operation;
    }
    
    // Getters and Setters
    public String getPoolId() { return poolId; }
    public void setPoolId(String poolId) { this.poolId = poolId; }
    
    public String getUserAddress() { return userAddress; }
    public void setUserAddress(String userAddress) { this.userAddress = userAddress; }
    
    public PoolOperation getOperation() { return operation; }
    public void setOperation(PoolOperation operation) { this.operation = operation; }
    
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    
    public BigDecimal getSlippageTolerance() { return slippageTolerance; }
    public void setSlippageTolerance(BigDecimal slippageTolerance) { this.slippageTolerance = slippageTolerance; }
    
    public Long getDeadline() { return deadline; }
    public void setDeadline(Long deadline) { this.deadline = deadline; }
}