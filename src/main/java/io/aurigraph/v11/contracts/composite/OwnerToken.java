package io.aurigraph.v11.contracts.composite;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

/**
 * Owner Token (ERC-721) - Tracks ownership percentage and transfer history
 */
public class OwnerToken extends SecondaryToken {
    private String ownerAddress;
    private BigDecimal ownershipPercentage;
    private List<OwnershipTransfer> transferHistory;

    public OwnerToken(String tokenId, String compositeId, String ownerAddress, 
                     BigDecimal ownershipPercentage, List<OwnershipTransfer> transferHistory) {
        super(tokenId, compositeId, SecondaryTokenType.OWNER);
        this.ownerAddress = ownerAddress;
        this.ownershipPercentage = ownershipPercentage;
        this.transferHistory = new ArrayList<>(transferHistory);
    }

    @Override
    public void updateData(Map<String, Object> updateData) {
        if (updateData.containsKey("ownershipPercentage")) {
            this.ownershipPercentage = (BigDecimal) updateData.get("ownershipPercentage");
        }
        this.lastUpdated = Instant.now();
        this.data.putAll(updateData);
    }

    public void recordTransfer(String fromAddress, String toAddress) {
        OwnershipTransfer transfer = new OwnershipTransfer(
            fromAddress, toAddress, Instant.now(), ownershipPercentage
        );
        transferHistory.add(transfer);
        this.ownerAddress = toAddress;
        this.lastUpdated = Instant.now();
    }

    // Getters
    public String getOwnerAddress() { return ownerAddress; }
    public BigDecimal getOwnershipPercentage() { return ownershipPercentage; }
    public List<OwnershipTransfer> getTransferHistory() { return List.copyOf(transferHistory); }

    /**
     * Ownership transfer record
     */
    public static class OwnershipTransfer {
        private final String fromAddress;
        private final String toAddress;
        private final Instant transferDate;
        private final BigDecimal ownershipPercentage;

        public OwnershipTransfer(String fromAddress, String toAddress, 
                                Instant transferDate, BigDecimal ownershipPercentage) {
            this.fromAddress = fromAddress;
            this.toAddress = toAddress;
            this.transferDate = transferDate;
            this.ownershipPercentage = ownershipPercentage;
        }

        public String getFromAddress() { return fromAddress; }
        public String getToAddress() { return toAddress; }
        public Instant getTransferDate() { return transferDate; }
        public BigDecimal getOwnershipPercentage() { return ownershipPercentage; }
    }
}