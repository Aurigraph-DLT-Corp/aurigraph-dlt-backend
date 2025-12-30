package io.aurigraph.v11.contracts.defi.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Swap Result Model
 * Result of a token swap operation in DeFi protocols
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SwapResult {
    
    @JsonProperty("swapId")
    private String swapId;
    
    @JsonProperty("requestId")
    private String requestId;
    
    @JsonProperty("userAddress")
    private String userAddress;
    
    @JsonProperty("status")
    private SwapStatus status;
    
    @JsonProperty("swapType")
    private SwapType swapType;
    
    @JsonProperty("protocol")
    private String protocol;
    
    @JsonProperty("tokenIn")
    private String tokenIn;
    
    @JsonProperty("tokenOut")
    private String tokenOut;
    
    @JsonProperty("amountIn")
    private BigDecimal amountIn;
    
    @JsonProperty("amountOut")
    private BigDecimal amountOut;
    
    @JsonProperty("requestedAmountOut")
    private BigDecimal requestedAmountOut;
    
    @JsonProperty("minimumAmountOut")
    private BigDecimal minimumAmountOut;
    
    @JsonProperty("actualPrice")
    private BigDecimal actualPrice;
    
    @JsonProperty("expectedPrice")
    private BigDecimal expectedPrice;
    
    @JsonProperty("priceImpact")
    private BigDecimal priceImpact; // percentage
    
    @JsonProperty("slippage")
    private BigDecimal slippage; // percentage
    
    @JsonProperty("maxSlippage")
    private BigDecimal maxSlippage; // percentage
    
    @JsonProperty("swapFee")
    private BigDecimal swapFee;
    
    @JsonProperty("protocolFee")
    private BigDecimal protocolFee;
    
    @JsonProperty("gasFee")
    private BigDecimal gasFee;
    
    @JsonProperty("totalFees")
    private BigDecimal totalFees;
    
    @JsonProperty("exchangeRate")
    private BigDecimal exchangeRate;
    
    @JsonProperty("route")
    private List<SwapRoute> route;
    
    @JsonProperty("executionTime")
    private Long executionTime; // milliseconds
    
    @JsonProperty("blockNumber")
    private Long blockNumber;
    
    @JsonProperty("transactionHash")
    private String transactionHash;
    
    @JsonProperty("poolAddress")
    private String poolAddress;
    
    @JsonProperty("liquidityUsed")
    private BigDecimal liquidityUsed;
    
    @JsonProperty("remainingLiquidity")
    private BigDecimal remainingLiquidity;
    
    @JsonProperty("mevProtection")
    private Boolean mevProtection;
    
    @JsonProperty("frontRunProtection")
    private Boolean frontRunProtection;
    
    @JsonProperty("executedAt")
    private Instant executedAt;
    
    @JsonProperty("expiresAt")
    private Instant expiresAt;
    
    @JsonProperty("estimatedGas")
    private BigDecimal estimatedGas;
    
    @JsonProperty("actualGas")
    private BigDecimal actualGas;
    
    @JsonProperty("gasEfficiency")
    private BigDecimal gasEfficiency; // actualGas/estimatedGas
    
    @JsonProperty("errorMessage")
    private String errorMessage;
    
    @JsonProperty("warnings")
    private List<String> warnings;
    
    @JsonProperty("executionPrice")
    private BigDecimal executionPrice;
    
    @JsonProperty("mevProtected")
    private boolean mevProtected;
    
    @JsonProperty("metadata")
    private Map<String, Object> metadata;
    
    /**
     * Status of the swap operation
     */
    public enum SwapStatus {
        PENDING,
        EXECUTING,
        COMPLETED,
        FAILED,
        EXPIRED,
        CANCELLED,
        PARTIAL_FILL,
        SLIPPAGE_EXCEEDED,
        INSUFFICIENT_LIQUIDITY
    }
    
    /**
     * Type of swap operation
     */
    public enum SwapType {
        EXACT_INPUT,          // Exact amount in, minimum amount out
        EXACT_OUTPUT,         // Maximum amount in, exact amount out
        LIMIT_ORDER,          // Execute at specific price
        MARKET_ORDER,         // Execute at current market price
        STOP_LOSS,           // Execute when price drops below threshold
        TAKE_PROFIT,         // Execute when price rises above threshold
        DOLLAR_COST_AVERAGE, // Recurring swaps over time
        FLASH_SWAP          // Flash loan enabled swap
    }
    
    /**
     * Swap route through different pools/protocols
     */
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SwapRoute {
        @JsonProperty("stepNumber")
        private Integer stepNumber;
        
        @JsonProperty("protocol")
        private String protocol;
        
        @JsonProperty("poolAddress")
        private String poolAddress;
        
        @JsonProperty("poolFee")
        private BigDecimal poolFee;
        
        @JsonProperty("tokenIn")
        private String tokenIn;
        
        @JsonProperty("tokenOut")
        private String tokenOut;
        
        @JsonProperty("amountIn")
        private BigDecimal amountIn;
        
        @JsonProperty("amountOut")
        private BigDecimal amountOut;
        
        @JsonProperty("exchangeRate")
        private BigDecimal exchangeRate;
        
        @JsonProperty("priceImpact")
        private BigDecimal priceImpact;
        
        @JsonProperty("gasEstimate")
        private BigDecimal gasEstimate;
        
        @JsonProperty("liquidityUtilization")
        private BigDecimal liquidityUtilization; // percentage of pool liquidity used
    }
    
    // Constructor with essential fields - removed to allow Lombok to generate all variants
    // Lombok @NoArgsConstructor and @AllArgsConstructor will generate all needed constructors
    // Custom initialization can be done in a Builder or static factory method
    
    /**
     * Check if swap was successful
     */
    public boolean isSuccessful() {
        return status == SwapStatus.COMPLETED;
    }
    
    /**
     * Check if swap failed due to slippage
     */
    public boolean isSlippageExceeded() {
        return status == SwapStatus.SLIPPAGE_EXCEEDED ||
               (slippage != null && maxSlippage != null && slippage.compareTo(maxSlippage) > 0);
    }
    
    /**
     * Check if swap had high price impact
     */
    public boolean hasHighPriceImpact() {
        return priceImpact != null && priceImpact.compareTo(BigDecimal.valueOf(5.0)) > 0; // >5%
    }
    
    /**
     * Calculate slippage from expected vs actual amounts
     */
    public void calculateSlippage() {
        if (requestedAmountOut != null && amountOut != null && requestedAmountOut.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal difference = requestedAmountOut.subtract(amountOut);
            this.slippage = difference.divide(requestedAmountOut, 4, BigDecimal.ROUND_HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
        }
    }
    
    /**
     * Calculate total fees
     */
    public void calculateTotalFees() {
        BigDecimal total = BigDecimal.ZERO;
        
        if (swapFee != null) total = total.add(swapFee);
        if (protocolFee != null) total = total.add(protocolFee);
        if (gasFee != null) total = total.add(gasFee);
        
        this.totalFees = total;
    }
    
    /**
     * Calculate gas efficiency
     */
    public void calculateGasEfficiency() {
        if (estimatedGas != null && actualGas != null && estimatedGas.compareTo(BigDecimal.ZERO) > 0) {
            this.gasEfficiency = actualGas.divide(estimatedGas, 4, BigDecimal.ROUND_HALF_UP);
        }
    }
    
    /**
     * Get efficiency score (0-100)
     */
    public Double getEfficiencyScore() {
        double score = 100.0;
        
        // Penalty for high slippage
        if (slippage != null) {
            if (slippage.compareTo(BigDecimal.valueOf(1.0)) > 0) {
                score -= Math.min(30, slippage.doubleValue() * 10);
            }
        }
        
        // Penalty for high price impact
        if (priceImpact != null) {
            if (priceImpact.compareTo(BigDecimal.valueOf(2.0)) > 0) {
                score -= Math.min(25, priceImpact.doubleValue() * 5);
            }
        }
        
        // Penalty for gas inefficiency
        if (gasEfficiency != null) {
            if (gasEfficiency.compareTo(BigDecimal.valueOf(1.2)) > 0) {
                score -= Math.min(15, (gasEfficiency.doubleValue() - 1.0) * 20);
            }
        }
        
        // Penalty for high total fees
        if (totalFees != null && amountIn != null && amountIn.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal feePercentage = totalFees.divide(amountIn, 4, BigDecimal.ROUND_HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
            if (feePercentage.compareTo(BigDecimal.valueOf(1.0)) > 0) {
                score -= Math.min(20, feePercentage.doubleValue() * 5);
            }
        }
        
        return Math.max(0.0, Math.min(100.0, score));
    }
    
    /**
     * Set amount out
     */
    public void setAmountOut(BigDecimal amountOut) {
        this.amountOut = amountOut;
    }

    /**
     * Set price impact
     */
    public void setPriceImpact(BigDecimal priceImpact) {
        this.priceImpact = priceImpact;
    }

    /**
     * Set execution price
     */
    public void setExecutionPrice(BigDecimal executionPrice) {
        this.executionPrice = executionPrice;
    }

    /**
     * Set MEV protection status
     */
    public void setMEVProtected(boolean mevProtected) {
        this.mevProtected = mevProtected;
        this.mevProtection = mevProtected;
    }
    
    /**
     * Set slippage protection
     */
    public void setSlippageProtection(BigDecimal slippageProtection) {
        this.maxSlippage = slippageProtection;
    }
    
    /**
     * Add warning
     */
    public void addWarning(String warning) {
        if (warnings == null) {
            warnings = new java.util.ArrayList<>();
        }
        warnings.add(warning);
    }
    
    /**
     * Add route step
     */
    public void addRouteStep(SwapRoute step) {
        if (route == null) {
            route = new java.util.ArrayList<>();
        }
        route.add(step);
    }
    
    /**
     * Mark swap as completed
     */
    public void markCompleted(BigDecimal actualAmountOut, String txHash) {
        this.status = SwapStatus.COMPLETED;
        this.amountOut = actualAmountOut;
        this.transactionHash = txHash;
        calculateSlippage();
        calculateTotalFees();
        calculateGasEfficiency();
    }
    
    /**
     * Mark swap as failed
     */
    public void markFailed(String errorMessage) {
        this.status = SwapStatus.FAILED;
        this.errorMessage = errorMessage;
    }
    
    /**
     * Check if swap is within acceptable parameters
     */
    public boolean isWithinAcceptableParameters() {
        // Check slippage
        if (maxSlippage != null && slippage != null && slippage.compareTo(maxSlippage) > 0) {
            return false;
        }
        
        // Check minimum amount out
        if (minimumAmountOut != null && amountOut != null && amountOut.compareTo(minimumAmountOut) < 0) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Get profitability vs holding
     */
    public BigDecimal getProfitability() {
        if (amountOut != null && amountIn != null && amountIn.compareTo(BigDecimal.ZERO) > 0) {
            return amountOut.subtract(amountIn).divide(amountIn, 4, BigDecimal.ROUND_HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
        }
        return BigDecimal.ZERO;
    }
    
    /**
     * Get swap summary
     */
    public String getSwapSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append("Swap ").append(swapId).append(": ");
        summary.append(status).append(" - ");
        
        if (amountIn != null && amountOut != null) {
            summary.append(amountIn).append(" ").append(tokenIn)
                   .append(" -> ").append(amountOut).append(" ").append(tokenOut);
        }
        
        if (slippage != null) {
            summary.append(" (Slippage: ").append(String.format("%.2f", slippage)).append("%)");
        }
        
        if (totalFees != null) {
            summary.append(" [Fees: ").append(totalFees).append("]");
        }
        
        return summary.toString();
    }
}