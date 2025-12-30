package io.aurigraph.v11.bridge;

import java.math.BigDecimal;

/**
 * Bridge fee estimate
 */
public class BridgeFeeEstimate {
    private final BigDecimal bridgeFee;
    private final BigDecimal gasFee;
    private final BigDecimal totalFee;
    private final String tokenSymbol;

    public BridgeFeeEstimate(BigDecimal bridgeFee, BigDecimal gasFee, BigDecimal totalFee, String tokenSymbol) {
        this.bridgeFee = bridgeFee;
        this.gasFee = gasFee;
        this.totalFee = totalFee;
        this.tokenSymbol = tokenSymbol;
    }

    // Getters
    public BigDecimal getBridgeFee() { return bridgeFee; }
    public BigDecimal getGasFee() { return gasFee; }
    public BigDecimal getTotalFee() { return totalFee; }
    public String getTokenSymbol() { return tokenSymbol; }
}
