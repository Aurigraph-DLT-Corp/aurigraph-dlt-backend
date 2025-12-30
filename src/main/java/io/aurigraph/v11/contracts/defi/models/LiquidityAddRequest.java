package io.aurigraph.v11.contracts.defi.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

/**
 * Sprint 4 Liquidity Add Request Model
 * Specific request for adding liquidity to pools
 */
public class LiquidityAddRequest extends LiquidityPoolRequest {
    
    @JsonProperty("token0")
    private String token0;
    
    @JsonProperty("token1")
    private String token1;
    
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
    
    @JsonProperty("autoStake")
    private boolean autoStake;
    
    @JsonProperty("targetFarmId")
    private String targetFarmId;
    
    // Constructors
    public LiquidityAddRequest() {
        super();
        setOperation(PoolOperation.ADD_LIQUIDITY);
        this.enableILProtection = false;
        this.autoStake = false;
    }
    
    public LiquidityAddRequest(String poolId, String userAddress, String token0, String token1,
                             BigDecimal token0Amount, BigDecimal token1Amount) {
        this();
        setPoolId(poolId);
        setUserAddress(userAddress);
        this.token0 = token0;
        this.token1 = token1;
        this.token0Amount = token0Amount;
        this.token1Amount = token1Amount;
        
        // Calculate minimum amounts with default 0.5% slippage tolerance
        this.minToken0 = token0Amount.multiply(BigDecimal.ONE.subtract(getSlippageTolerance()));
        this.minToken1 = token1Amount.multiply(BigDecimal.ONE.subtract(getSlippageTolerance()));
    }
    
    // Getters and Setters
    public String getToken0() { return token0; }
    public void setToken0(String token0) { this.token0 = token0; }
    
    public String getToken1() { return token1; }
    public void setToken1(String token1) { this.token1 = token1; }
    
    public BigDecimal getToken0Amount() { return token0Amount; }
    public void setToken0Amount(BigDecimal token0Amount) { 
        this.token0Amount = token0Amount;
        // Auto-calculate minimum if slippage tolerance is set
        if (getSlippageTolerance() != null) {
            this.minToken0 = token0Amount.multiply(BigDecimal.ONE.subtract(getSlippageTolerance()));
        }
    }
    
    public BigDecimal getToken1Amount() { return token1Amount; }
    public void setToken1Amount(BigDecimal token1Amount) { 
        this.token1Amount = token1Amount;
        // Auto-calculate minimum if slippage tolerance is set
        if (getSlippageTolerance() != null) {
            this.minToken1 = token1Amount.multiply(BigDecimal.ONE.subtract(getSlippageTolerance()));
        }
    }
    
    public BigDecimal getMinToken0() { return minToken0; }
    public void setMinToken0(BigDecimal minToken0) { this.minToken0 = minToken0; }
    
    public BigDecimal getMinToken1() { return minToken1; }
    public void setMinToken1(BigDecimal minToken1) { this.minToken1 = minToken1; }
    
    public boolean isEnableILProtection() { return enableILProtection; }
    public void setEnableILProtection(boolean enableILProtection) { this.enableILProtection = enableILProtection; }
    
    public boolean isAutoStake() { return autoStake; }
    public void setAutoStake(boolean autoStake) { this.autoStake = autoStake; }
    
    public String getTargetFarmId() { return targetFarmId; }
    public void setTargetFarmId(String targetFarmId) { this.targetFarmId = targetFarmId; }
    
    // Validation methods
    public boolean isValid() {
        return getPoolId() != null && 
               getUserAddress() != null &&
               token0 != null &&
               token1 != null &&
               token0Amount != null && token0Amount.compareTo(BigDecimal.ZERO) > 0 &&
               token1Amount != null && token1Amount.compareTo(BigDecimal.ZERO) > 0 &&
               minToken0 != null && minToken0.compareTo(BigDecimal.ZERO) >= 0 &&
               minToken1 != null && minToken1.compareTo(BigDecimal.ZERO) >= 0;
    }
}