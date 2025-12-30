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
 * Lending Request Model
 * Request for lending operations in DeFi protocols (supply, borrow, repay, withdraw)
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class LendingRequest {
    
    @JsonProperty("requestId")
    private String requestId;
    
    @JsonProperty("userAddress")
    @NotBlank(message = "User address is required")
    private String userAddress;
    
    @JsonProperty("operationType")
    @NotNull(message = "Operation type is required")
    private LendingOperationType operationType;
    
    @JsonProperty("protocol")
    private String protocol; // e.g., "Aave", "Compound", "MakerDAO"
    
    @JsonProperty("asset")
    @NotBlank(message = "Asset is required")
    private String asset;
    
    @JsonProperty("amount")
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Amount must be positive")
    private BigDecimal amount;
    
    @JsonProperty("collateralAsset")
    private String collateralAsset;
    
    @JsonProperty("collateralAmount")
    private BigDecimal collateralAmount;
    
    @JsonProperty("interestRateMode")
    private InterestRateMode interestRateMode;
    
    @JsonProperty("maxInterestRate")
    private BigDecimal maxInterestRate; // For borrowing - maximum acceptable rate
    
    @JsonProperty("minInterestRate")
    private BigDecimal minInterestRate; // For lending - minimum acceptable rate
    
    @JsonProperty("duration")
    private Long duration; // Duration in seconds (for fixed-term loans)
    
    @JsonProperty("maturityDate")
    private Instant maturityDate;
    
    @JsonProperty("liquidationThreshold")
    private BigDecimal liquidationThreshold; // For borrowing
    
    @JsonProperty("healthFactorLimit")
    private BigDecimal healthFactorLimit; // Minimum health factor to maintain
    
    @JsonProperty("maxSlippage")
    private BigDecimal maxSlippage; // For operations involving swaps
    
    @JsonProperty("deadline")
    private Instant deadline;
    
    @JsonProperty("priorityFee")
    private BigDecimal priorityFee;
    
    @JsonProperty("maxGasPrice")
    private BigDecimal maxGasPrice;
    
    @JsonProperty("gasLimit")
    private BigDecimal gasLimit;
    
    @JsonProperty("flashLoanEnabled")
    private Boolean flashLoanEnabled;
    
    @JsonProperty("autoCompound")
    private Boolean autoCompound; // Automatically compound rewards
    
    @JsonProperty("autoRebalance")
    private Boolean autoRebalance; // Automatically rebalance position
    
    @JsonProperty("riskTolerance")
    private RiskTolerance riskTolerance;
    
    @JsonProperty("referralCode")
    private String referralCode;
    
    @JsonProperty("recipient")
    private String recipient; // For withdraw operations
    
    @JsonProperty("approvedSpender")
    private String approvedSpender;
    
    @JsonProperty("tags")
    private List<String> tags;
    
    @JsonProperty("clientId")
    private String clientId;
    
    @JsonProperty("requestedAt")
    private Instant requestedAt;
    
    @JsonProperty("metadata")
    private Map<String, Object> metadata;
    
    /**
     * Types of lending operations
     */
    public enum LendingOperationType {
        SUPPLY,              // Supply assets to earn interest
        WITHDRAW,            // Withdraw supplied assets
        BORROW,              // Borrow assets against collateral
        REPAY,               // Repay borrowed assets
        LIQUIDATE,           // Liquidate undercollateralized position
        CLAIM_REWARDS,       // Claim protocol rewards
        ENABLE_COLLATERAL,   // Enable asset as collateral
        DISABLE_COLLATERAL,  // Disable asset as collateral
        SWITCH_RATE_MODE,    // Switch between variable and stable rate
        FLASH_LOAN,          // Flash loan operation
        REFINANCE,           // Refinance existing loan
        LEVERAGE,            // Increase leverage using borrowed funds
        DELEVERAGE          // Reduce leverage by repaying debt
    }
    
    /**
     * Interest rate modes
     */
    public enum InterestRateMode {
        VARIABLE,            // Variable interest rate
        STABLE,              // Stable interest rate
        FIXED,               // Fixed interest rate (for specific duration)
        FLOATING,            // Floating rate tied to market conditions
        ZERO                 // Zero interest (promotional or special conditions)
    }
    
    /**
     * Risk tolerance levels
     */
    public enum RiskTolerance {
        VERY_LOW,            // Conservative, low-risk strategies
        LOW,                 // Below-average risk appetite
        MODERATE,            // Balanced risk/reward
        HIGH,                // Above-average risk for higher returns
        VERY_HIGH,           // Aggressive, high-risk strategies
        EXTREME              // Maximum risk tolerance
    }
    
    // Default values are set using @Builder.Default annotation
    // Use LendingRequest.builder().userAddress(...).operationType(...).asset(...).amount(...).build()
    // Or use the all-args constructor generated by @AllArgsConstructor
    
    /**
     * Validate the lending request
     */
    public boolean isValid() {
        if (userAddress == null || userAddress.trim().isEmpty()) return false;
        if (operationType == null) return false;
        if (asset == null || asset.trim().isEmpty()) return false;
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) return false;
        
        // Specific validations based on operation type
        switch (operationType) {
            case BORROW:
            case LEVERAGE:
                // Borrowing operations need collateral (unless flash loan)
                if (!Boolean.TRUE.equals(flashLoanEnabled)) {
                    return collateralAsset != null && collateralAmount != null && 
                           collateralAmount.compareTo(BigDecimal.ZERO) > 0;
                }
                break;
            case LIQUIDATE:
                // Liquidation needs target position info
                return recipient != null; // recipient would be the position being liquidated
            case FLASH_LOAN:
                return Boolean.TRUE.equals(flashLoanEnabled);
            case SWITCH_RATE_MODE:
                return interestRateMode != null;
        }
        
        return true;
    }
    
    /**
     * Check if request is expired
     */
    public boolean isExpired() {
        return deadline != null && deadline.isBefore(Instant.now());
    }
    
    /**
     * Check if this is a collateralized operation
     */
    public boolean isCollateralized() {
        return collateralAsset != null && collateralAmount != null && 
               collateralAmount.compareTo(BigDecimal.ZERO) > 0;
    }
    
    /**
     * Check if operation involves borrowing
     */
    public boolean involvesBorrowing() {
        return operationType == LendingOperationType.BORROW ||
               operationType == LendingOperationType.LEVERAGE ||
               operationType == LendingOperationType.FLASH_LOAN ||
               operationType == LendingOperationType.REFINANCE;
    }
    
    /**
     * Check if operation involves lending/supplying
     */
    public boolean involvesSupplying() {
        return operationType == LendingOperationType.SUPPLY ||
               operationType == LendingOperationType.ENABLE_COLLATERAL;
    }
    
    /**
     * Check if operation requires existing position
     */
    public boolean requiresExistingPosition() {
        return operationType == LendingOperationType.WITHDRAW ||
               operationType == LendingOperationType.REPAY ||
               operationType == LendingOperationType.CLAIM_REWARDS ||
               operationType == LendingOperationType.DISABLE_COLLATERAL ||
               operationType == LendingOperationType.SWITCH_RATE_MODE ||
               operationType == LendingOperationType.REFINANCE ||
               operationType == LendingOperationType.DELEVERAGE;
    }
    
    /**
     * Check if request requires immediate execution
     */
    public boolean requiresImmediateExecution() {
        return operationType == LendingOperationType.LIQUIDATE ||
               operationType == LendingOperationType.FLASH_LOAN ||
               (deadline != null && deadline.isBefore(Instant.now().plusSeconds(60)));
    }
    
    /**
     * Get effective recipient address
     */
    public String getEffectiveRecipient() {
        return recipient != null ? recipient : userAddress;
    }
    
    /**
     * Get collateral amount
     */
    public BigDecimal getCollateralAmount() {
        return collateralAmount;
    }

    /**
     * Get collateral token (alias for collateralAsset)
     */
    public String getCollateralToken() {
        return collateralAsset;
    }
    
    /**
     * Get borrow token (alias for asset)
     */
    public String getBorrowToken() {
        return asset;
    }
    
    /**
     * Get borrow amount (alias for amount)
     */
    public BigDecimal getBorrowAmount() {
        return amount;
    }
    
    /**
     * Calculate loan-to-value ratio if this is a borrowing operation
     */
    public BigDecimal calculateLTV() {
        if (!involvesBorrowing() || !isCollateralized()) {
            return null;
        }
        
        if (collateralAmount != null && amount != null && collateralAmount.compareTo(BigDecimal.ZERO) > 0) {
            return amount.divide(collateralAmount, 4, BigDecimal.ROUND_HALF_UP);
        }
        
        return null;
    }
    
    /**
     * Check if interest rate is acceptable
     */
    public boolean isInterestRateAcceptable(BigDecimal currentRate) {
        if (currentRate == null) return true;
        
        if (involvesBorrowing()) {
            // For borrowing, rate should be below maximum
            return maxInterestRate == null || currentRate.compareTo(maxInterestRate) <= 0;
        } else if (involvesSupplying()) {
            // For lending, rate should be above minimum
            return minInterestRate == null || currentRate.compareTo(minInterestRate) >= 0;
        }
        
        return true;
    }
    
    /**
     * Get risk score based on operation and parameters
     */
    public int getRiskScore() {
        int score = 0;
        
        // Base score from risk tolerance
        switch (riskTolerance) {
            case VERY_LOW: score += 10; break;
            case LOW: score += 20; break;
            case MODERATE: score += 40; break;
            case HIGH: score += 60; break;
            case VERY_HIGH: score += 80; break;
            case EXTREME: score += 100; break;
        }
        
        // Operation type risk
        switch (operationType) {
            case SUPPLY: score += 10; break;
            case WITHDRAW: score += 15; break;
            case BORROW: score += 40; break;
            case LIQUIDATE: score += 30; break;
            case LEVERAGE: score += 70; break;
            case FLASH_LOAN: score += 60; break;
            case DELEVERAGE: score += 20; break;
            default: score += 25;
        }
        
        // High amounts increase risk
        if (amount != null && amount.compareTo(BigDecimal.valueOf(100000)) > 0) {
            score += 20;
        }
        
        // Flash loans increase risk
        if (Boolean.TRUE.equals(flashLoanEnabled)) {
            score += 30;
        }
        
        return Math.min(100, score);
    }
    
    /**
     * Get priority score for processing
     */
    public int getPriorityScore() {
        int score = 0;
        
        // Urgent operations get higher priority
        if (operationType == LendingOperationType.LIQUIDATE) score += 100;
        if (operationType == LendingOperationType.FLASH_LOAN) score += 90;
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
        
        // High-value operations get priority
        if (amount != null && amount.compareTo(BigDecimal.valueOf(50000)) > 0) {
            score += 30;
        }
        
        return score;
    }
    
    /**
     * Create a copy of the request with updated parameters
     */
    public LendingRequest withUpdatedAmount(BigDecimal newAmount) {
        LendingRequest copy = LendingRequest.builder()
                .requestId(java.util.UUID.randomUUID().toString())
                .userAddress(this.userAddress)
                .operationType(this.operationType)
                .protocol(this.protocol)
                .asset(this.asset)
                .amount(newAmount)
                .collateralAsset(this.collateralAsset)
                .collateralAmount(this.collateralAmount)
                .interestRateMode(this.interestRateMode)
                .maxInterestRate(this.maxInterestRate)
                .minInterestRate(this.minInterestRate)
                .duration(this.duration)
                .maturityDate(this.maturityDate)
                .liquidationThreshold(this.liquidationThreshold)
                .healthFactorLimit(this.healthFactorLimit)
                .maxSlippage(this.maxSlippage)
                .deadline(this.deadline)
                .priorityFee(this.priorityFee)
                .maxGasPrice(this.maxGasPrice)
                .gasLimit(this.gasLimit)
                .flashLoanEnabled(this.flashLoanEnabled)
                .autoCompound(this.autoCompound)
                .autoRebalance(this.autoRebalance)
                .riskTolerance(this.riskTolerance)
                .referralCode(this.referralCode)
                .recipient(this.recipient)
                .approvedSpender(this.approvedSpender)
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
        summary.append("Lending Request ").append(requestId).append(": ");
        summary.append(operationType).append(" ").append(amount).append(" ").append(asset);
        
        if (protocol != null) {
            summary.append(" on ").append(protocol);
        }
        
        if (isCollateralized()) {
            summary.append(" (Collateral: ").append(collateralAmount).append(" ").append(collateralAsset).append(")");
        }
        
        if (interestRateMode != null && interestRateMode != InterestRateMode.VARIABLE) {
            summary.append(" [").append(interestRateMode).append(" rate]");
        }
        
        if (deadline != null) {
            summary.append(" [Deadline: ").append(deadline).append("]");
        }
        
        return summary.toString();
    }
}