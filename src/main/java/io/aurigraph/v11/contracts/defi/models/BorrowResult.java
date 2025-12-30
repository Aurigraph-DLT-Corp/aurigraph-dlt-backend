package io.aurigraph.v11.contracts.defi.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Sprint 4 Borrow Result Model
 * Result of borrowing operations in lending protocols
 */
public class BorrowResult extends LendingResult {
    
    @JsonProperty("borrowedAmount")
    private BigDecimal borrowedAmount;
    
    @JsonProperty("borrowToken")
    private String borrowToken;
    
    @JsonProperty("collateralToken")
    private String collateralToken;
    
    @JsonProperty("actualInterestRate")
    private BigDecimal actualInterestRate;
    
    @JsonProperty("rateType")
    private BorrowRequest.InterestRateType rateType;
    
    @JsonProperty("maturityDate")
    private Instant maturityDate;
    
    @JsonProperty("nextPaymentDate")
    private Instant nextPaymentDate;
    
    @JsonProperty("minimumPaymentAmount")
    private BigDecimal minimumPaymentAmount;
    
    @JsonProperty("totalRepaymentAmount")
    private BigDecimal totalRepaymentAmount;
    
    @JsonProperty("originationFee")
    private BigDecimal originationFee;
    
    @JsonProperty("riskWarnings")
    private String riskWarnings;
    
    // Constructors
    public BorrowResult() {
        super();
        setOperation(LendingOperation.BORROW);
        this.originationFee = BigDecimal.ZERO;
    }
    
    public BorrowResult(boolean success) {
        super(success);
        setOperation(LendingOperation.BORROW);
    }
    
    public BorrowResult(String positionId, String userAddress) {
        super(positionId, userAddress, LendingOperation.BORROW);
    }
    
    // Static factory methods
    public static BorrowResult successfulBorrow(String positionId, String userAddress, 
                                               BigDecimal borrowedAmount, String borrowToken) {
        BorrowResult result = new BorrowResult(positionId, userAddress);
        result.setBorrowedAmount(borrowedAmount);
        result.setBorrowToken(borrowToken);
        return result;
    }
    
    public static BorrowResult failedBorrow(String errorMessage) {
        BorrowResult result = new BorrowResult(false);
        result.setErrorMessage(errorMessage);
        return result;
    }
    
    // Getters and Setters
    public BigDecimal getBorrowedAmount() { return borrowedAmount; }
    public void setBorrowedAmount(BigDecimal borrowedAmount) { this.borrowedAmount = borrowedAmount; }
    
    public String getBorrowToken() { return borrowToken; }
    public void setBorrowToken(String borrowToken) { this.borrowToken = borrowToken; }
    
    public String getCollateralToken() { return collateralToken; }
    public void setCollateralToken(String collateralToken) { this.collateralToken = collateralToken; }
    
    public BigDecimal getActualInterestRate() { return actualInterestRate; }
    public void setActualInterestRate(BigDecimal actualInterestRate) { this.actualInterestRate = actualInterestRate; }
    
    public BorrowRequest.InterestRateType getRateType() { return rateType; }
    public void setRateType(BorrowRequest.InterestRateType rateType) { this.rateType = rateType; }
    
    public Instant getMaturityDate() { return maturityDate; }
    public void setMaturityDate(Instant maturityDate) { this.maturityDate = maturityDate; }
    
    public Instant getNextPaymentDate() { return nextPaymentDate; }
    public void setNextPaymentDate(Instant nextPaymentDate) { this.nextPaymentDate = nextPaymentDate; }
    
    public BigDecimal getMinimumPaymentAmount() { return minimumPaymentAmount; }
    public void setMinimumPaymentAmount(BigDecimal minimumPaymentAmount) { this.minimumPaymentAmount = minimumPaymentAmount; }
    
    public BigDecimal getTotalRepaymentAmount() { return totalRepaymentAmount; }
    public void setTotalRepaymentAmount(BigDecimal totalRepaymentAmount) { this.totalRepaymentAmount = totalRepaymentAmount; }
    
    public BigDecimal getOriginationFee() { return originationFee; }
    public void setOriginationFee(BigDecimal originationFee) { this.originationFee = originationFee; }
    
    public String getRiskWarnings() { return riskWarnings; }
    public void setRiskWarnings(String riskWarnings) { this.riskWarnings = riskWarnings; }
    
    // Helper methods
    public BigDecimal calculateCurrentDebt() {
        if (borrowedAmount == null || actualInterestRate == null) {
            return BigDecimal.ZERO;
        }
        
        // Simple interest calculation for demonstration
        long daysSinceBorrow = java.time.Duration.between(getTimestamp(), Instant.now()).toDays();
        BigDecimal interest = borrowedAmount
            .multiply(actualInterestRate)
            .multiply(BigDecimal.valueOf(daysSinceBorrow))
            .divide(BigDecimal.valueOf(365), 18, java.math.RoundingMode.HALF_UP);
            
        return borrowedAmount.add(interest);
    }
    
    public boolean isOverdue() {
        return nextPaymentDate != null && Instant.now().isAfter(nextPaymentDate);
    }
    
    public boolean isNearMaturity() {
        if (maturityDate == null) return false;
        
        // Consider near maturity if within 30 days
        Instant thirtyDaysFromNow = Instant.now().plusSeconds(30 * 24 * 60 * 60);
        return maturityDate.isBefore(thirtyDaysFromNow);
    }
}