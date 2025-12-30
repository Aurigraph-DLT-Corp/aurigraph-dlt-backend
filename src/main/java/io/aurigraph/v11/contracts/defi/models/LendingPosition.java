package io.aurigraph.v11.contracts.defi.models;

/**
 * Sprint 4 Lending Position
 * Alias for LoanPosition to maintain compatibility with different naming conventions
 * This class extends LoanPosition to provide lending-specific methods if needed
 */
public class LendingPosition extends LoanPosition {
    
    // Constructors
    public LendingPosition() {
        super();
    }
    
    public LendingPosition(String positionId, String userAddress, String protocolId) {
        super();
        setPositionId(positionId);
        setUserAddress(userAddress);
        setProtocolId(protocolId);
    }
    
    // Additional lending-specific methods can be added here if needed
    
    /**
     * Check if this is primarily a lending (supply) position
     */
    public boolean isSupplyPosition() {
        return getTotalBorrowedValue() == null || 
               getTotalBorrowedValue().compareTo(java.math.BigDecimal.ZERO) == 0;
    }
    
    /**
     * Check if this is primarily a borrowing position
     */
    public boolean isBorrowPosition() {
        return getTotalBorrowedValue() != null && 
               getTotalBorrowedValue().compareTo(java.math.BigDecimal.ZERO) > 0;
    }
    
    /**
     * Get the net position (positive for net lending, negative for net borrowing)
     */
    public java.math.BigDecimal getNetPosition() {
        java.math.BigDecimal collateral = getTotalCollateralValue() != null ? 
            getTotalCollateralValue() : java.math.BigDecimal.ZERO;
        java.math.BigDecimal borrowed = getTotalBorrowedValue() != null ? 
            getTotalBorrowedValue() : java.math.BigDecimal.ZERO;
        return collateral.subtract(borrowed);
    }
}