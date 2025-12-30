package io.aurigraph.v11.contracts.defi.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Builder;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Swap Request Model
 * Request for token swap operation in DeFi protocols
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SwapRequest {
    
    @JsonProperty("requestId")
    private String requestId;
    
    @JsonProperty("userAddress")
    @NotBlank(message = "User address is required")
    private String userAddress;
    
    @JsonProperty("tokenIn")
    @NotBlank(message = "Input token is required")
    private String tokenIn;
    
    @JsonProperty("tokenOut")
    @NotBlank(message = "Output token is required")
    private String tokenOut;
    
    @JsonProperty("amountIn")
    @NotNull(message = "Input amount is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Amount must be positive")
    private BigDecimal amountIn;
    
    @JsonProperty("amountOut")
    private BigDecimal amountOut; // For exact output swaps
    
    @JsonProperty("minimumAmountOut")
    private BigDecimal minimumAmountOut;
    
    @JsonProperty("maximumAmountIn")
    private BigDecimal maximumAmountIn; // For exact output swaps
    
    @JsonProperty("swapType")
    private SwapType swapType;
    
    @JsonProperty("maxSlippage")
    private BigDecimal maxSlippage; // percentage (e.g., 0.5 for 0.5%)
    
    @JsonProperty("deadline")
    private Instant deadline;
    
    @JsonProperty("priorityFee")
    private BigDecimal priorityFee;
    
    @JsonProperty("maxGasPrice")
    private BigDecimal maxGasPrice;
    
    @JsonProperty("gasLimit")
    private BigDecimal gasLimit;
    
    @JsonProperty("preferredProtocol")
    private String preferredProtocol;
    
    @JsonProperty("allowedProtocols")
    private List<String> allowedProtocols;
    
    @JsonProperty("excludedProtocols")
    private List<String> excludedProtocols;
    
    @JsonProperty("routingPreference")
    private RoutingPreference routingPreference;
    
    @JsonProperty("mevProtection")
    private Boolean mevProtection;
    
    @JsonProperty("frontRunProtection")
    private Boolean frontRunProtection;
    
    @JsonProperty("flashLoanEnabled")
    private Boolean flashLoanEnabled;
    
    @JsonProperty("partialFillEnabled")
    private Boolean partialFillEnabled;
    
    @JsonProperty("autoSlippage")
    private Boolean autoSlippage; // Automatically adjust slippage based on market conditions
    
    @JsonProperty("priceLimit")
    private BigDecimal priceLimit; // Maximum/minimum acceptable price
    
    @JsonProperty("recipient")
    private String recipient; // Different from userAddress if sending to someone else
    
    @JsonProperty("referralCode")
    private String referralCode;
    
    @JsonProperty("tags")
    private List<String> tags;
    
    @JsonProperty("clientId")
    private String clientId;
    
    @JsonProperty("requestedAt")
    private Instant requestedAt;
    
    @JsonProperty("metadata")
    private Map<String, Object> metadata;
    
    /**
     * Types of swap requests
     */
    public enum SwapType {
        EXACT_INPUT,          // Exact amount in, minimum amount out
        EXACT_OUTPUT,         // Maximum amount in, exact amount out
        LIMIT_ORDER,          // Execute at specific price or better
        MARKET_ORDER,         // Execute immediately at market price
        STOP_LOSS,           // Execute when price drops below threshold
        TAKE_PROFIT,         // Execute when price rises above threshold
        DOLLAR_COST_AVERAGE, // Recurring swaps over time
        FLASH_SWAP,          // Flash loan enabled swap
        TWAP_ORDER,          // Time-weighted average price order
        ICEBERG_ORDER        // Large order split into smaller chunks
    }
    
    /**
     * Routing preferences for swap execution
     */
    public enum RoutingPreference {
        BEST_PRICE,          // Optimize for best price
        LOWEST_GAS,          // Optimize for lowest gas cost
        FASTEST_EXECUTION,   // Optimize for speed
        LOWEST_SLIPPAGE,     // Optimize for minimal slippage
        BALANCED,            // Balance between price, gas, and slippage
        SINGLE_HOP,          // Direct swap only, no multi-hop routing
        MULTI_HOP_ONLY       // Allow only multi-hop routes
    }
    
    // Initialize default values using @Builder.Default or in constructor with parameters
    
    // Constructor with essential fields
    public SwapRequest(String userAddress, String tokenIn, String tokenOut, BigDecimal amountIn) {
        this.requestId = java.util.UUID.randomUUID().toString();
        this.requestedAt = Instant.now();
        this.swapType = SwapType.EXACT_INPUT;
        this.maxSlippage = BigDecimal.valueOf(0.5); // 0.5% default slippage
        this.deadline = Instant.now().plusSeconds(300); // 5 minutes default deadline
        this.routingPreference = RoutingPreference.BEST_PRICE;
        this.mevProtection = true;
        this.frontRunProtection = true;
        this.flashLoanEnabled = false;
        this.partialFillEnabled = false;
        this.autoSlippage = false;
        
        this.userAddress = userAddress;
        this.tokenIn = tokenIn;
        this.tokenOut = tokenOut;
        this.amountIn = amountIn;
        this.recipient = userAddress; // Default recipient to user address
    }
    
    /**
     * Validate the swap request
     */
    public boolean isValid() {
        if (userAddress == null || userAddress.trim().isEmpty()) return false;
        if (tokenIn == null || tokenIn.trim().isEmpty()) return false;
        if (tokenOut == null || tokenOut.trim().isEmpty()) return false;
        if (tokenIn.equals(tokenOut)) return false;
        
        switch (swapType) {
            case EXACT_INPUT:
                return amountIn != null && amountIn.compareTo(BigDecimal.ZERO) > 0;
            case EXACT_OUTPUT:
                return amountOut != null && amountOut.compareTo(BigDecimal.ZERO) > 0 &&
                       maximumAmountIn != null && maximumAmountIn.compareTo(BigDecimal.ZERO) > 0;
            case LIMIT_ORDER:
            case STOP_LOSS:
            case TAKE_PROFIT:
                return amountIn != null && amountIn.compareTo(BigDecimal.ZERO) > 0 &&
                       priceLimit != null && priceLimit.compareTo(BigDecimal.ZERO) > 0;
            default:
                return amountIn != null && amountIn.compareTo(BigDecimal.ZERO) > 0;
        }
    }
    
    /**
     * Check if request is expired
     */
    public boolean isExpired() {
        return deadline != null && deadline.isBefore(Instant.now());
    }
    
    /**
     * Check if request requires immediate execution
     */
    public boolean requiresImmediateExecution() {
        return swapType == SwapType.MARKET_ORDER || 
               (deadline != null && deadline.isBefore(Instant.now().plusSeconds(60)));
    }
    
    /**
     * Check if multi-hop routing is allowed
     */
    public boolean allowsMultiHopRouting() {
        return routingPreference != RoutingPreference.SINGLE_HOP;
    }
    
    /**
     * Check if single hop routing is preferred
     */
    public boolean prefersSingleHop() {
        return routingPreference == RoutingPreference.SINGLE_HOP;
    }
    
    /**
     * Get effective recipient address
     */
    public String getEffectiveRecipient() {
        return recipient != null ? recipient : userAddress;
    }
    
    /**
     * Check if MEV protection is enabled
     */
    public boolean isEnableMEVProtection() {
        return Boolean.TRUE.equals(mevProtection);
    }
    
    /**
     * Get token in
     */
    public String getTokenIn() {
        return tokenIn;
    }

    /**
     * Get token out
     */
    public String getTokenOut() {
        return tokenOut;
    }

    /**
     * Get amount in
     */
    public BigDecimal getAmountIn() {
        return amountIn;
    }

    /**
     * Get slippage tolerance (alias for maxSlippage)
     */
    public BigDecimal getSlippageTolerance() {
        return maxSlippage;
    }
    
    /**
     * Calculate minimum amount out based on slippage
     */
    public BigDecimal calculateMinimumAmountOut(BigDecimal expectedAmountOut) {
        if (minimumAmountOut != null) {
            return minimumAmountOut;
        }
        
        if (maxSlippage != null && expectedAmountOut != null) {
            BigDecimal slippageAmount = expectedAmountOut.multiply(maxSlippage)
                    .divide(BigDecimal.valueOf(100), 8, BigDecimal.ROUND_HALF_UP);
            return expectedAmountOut.subtract(slippageAmount);
        }
        
        return expectedAmountOut;
    }
    
    /**
     * Calculate maximum amount in based on slippage (for exact output swaps)
     */
    public BigDecimal calculateMaximumAmountIn(BigDecimal expectedAmountIn) {
        if (maximumAmountIn != null) {
            return maximumAmountIn;
        }
        
        if (maxSlippage != null && expectedAmountIn != null) {
            BigDecimal slippageAmount = expectedAmountIn.multiply(maxSlippage)
                    .divide(BigDecimal.valueOf(100), 8, BigDecimal.ROUND_HALF_UP);
            return expectedAmountIn.add(slippageAmount);
        }
        
        return expectedAmountIn;
    }
    
    /**
     * Check if protocol is allowed
     */
    public boolean isProtocolAllowed(String protocol) {
        if (excludedProtocols != null && excludedProtocols.contains(protocol)) {
            return false;
        }
        
        if (allowedProtocols != null && !allowedProtocols.isEmpty()) {
            return allowedProtocols.contains(protocol);
        }
        
        return true;
    }
    
    /**
     * Get priority score for routing
     */
    public int getPriorityScore() {
        int score = 0;
        
        // Higher priority for market orders and urgent requests
        if (swapType == SwapType.MARKET_ORDER) score += 100;
        if (requiresImmediateExecution()) score += 50;
        
        // Priority fee contribution
        if (priorityFee != null) {
            score += priorityFee.intValue();
        }
        
        // Time sensitivity
        if (deadline != null) {
            long secondsToDeadline = java.time.Duration.between(Instant.now(), deadline).getSeconds();
            if (secondsToDeadline < 60) score += 75;
            else if (secondsToDeadline < 300) score += 25;
        }
        
        return score;
    }
    
    /**
     * Create a copy of the request with updated parameters
     */
    public SwapRequest withUpdatedParameters(BigDecimal newAmountIn, BigDecimal newMaxSlippage) {
        SwapRequest copy = SwapRequest.builder()
                .requestId(java.util.UUID.randomUUID().toString())
                .userAddress(this.userAddress)
                .tokenIn(this.tokenIn)
                .tokenOut(this.tokenOut)
                .amountIn(newAmountIn)
                .amountOut(this.amountOut)
                .minimumAmountOut(this.minimumAmountOut)
                .maximumAmountIn(this.maximumAmountIn)
                .swapType(this.swapType)
                .maxSlippage(newMaxSlippage)
                .deadline(this.deadline)
                .priorityFee(this.priorityFee)
                .maxGasPrice(this.maxGasPrice)
                .gasLimit(this.gasLimit)
                .preferredProtocol(this.preferredProtocol)
                .allowedProtocols(this.allowedProtocols)
                .excludedProtocols(this.excludedProtocols)
                .routingPreference(this.routingPreference)
                .mevProtection(this.mevProtection)
                .frontRunProtection(this.frontRunProtection)
                .flashLoanEnabled(this.flashLoanEnabled)
                .partialFillEnabled(this.partialFillEnabled)
                .autoSlippage(this.autoSlippage)
                .priceLimit(this.priceLimit)
                .recipient(this.recipient)
                .referralCode(this.referralCode)
                .tags(this.tags)
                .clientId(this.clientId)
                .requestedAt(Instant.now())
                .metadata(this.metadata)
                .build();
        
        return copy;
    }
    
    /**
     * Get request summary
     */
    public String getRequestSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append("Swap Request ").append(requestId).append(": ");
        summary.append(amountIn).append(" ").append(tokenIn)
               .append(" -> ").append(tokenOut);
        
        if (swapType != SwapType.EXACT_INPUT) {
            summary.append(" (").append(swapType).append(")");
        }
        
        if (maxSlippage != null) {
            summary.append(" [Max Slippage: ").append(maxSlippage).append("%]");
        }
        
        if (deadline != null) {
            summary.append(" [Deadline: ").append(deadline).append("]");
        }
        
        return summary.toString();
    }
}