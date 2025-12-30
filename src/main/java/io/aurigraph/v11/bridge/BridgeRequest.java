package io.aurigraph.v11.bridge;

import java.math.BigDecimal;

/**
 * Bridge transaction request
 */
public class BridgeRequest {
    private String sourceChain;
    private String targetChain;
    private String sourceAddress;
    private String targetAddress;
    private String tokenContract;
    private String tokenSymbol;
    private BigDecimal amount;

    public BridgeRequest(String sourceChain, String targetChain, String sourceAddress,
                        String targetAddress, String tokenContract, String tokenSymbol, BigDecimal amount) {
        this.sourceChain = sourceChain;
        this.targetChain = targetChain;
        this.sourceAddress = sourceAddress;
        this.targetAddress = targetAddress;
        this.tokenContract = tokenContract;
        this.tokenSymbol = tokenSymbol;
        this.amount = amount;
    }

    // Getters
    public String getSourceChain() { return sourceChain; }
    public String getTargetChain() { return targetChain; }
    public String getSourceAddress() { return sourceAddress; }
    public String getTargetAddress() { return targetAddress; }
    public String getTokenContract() { return tokenContract; }
    public String getTokenSymbol() { return tokenSymbol; }
    public BigDecimal getAmount() { return amount; }
}
