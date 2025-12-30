package io.aurigraph.v11.tokens.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Token Balance Model for Aurigraph V11 - LevelDB Compatible
 *
 * Tracks token balances for each address.
 * Supports fungible and non-fungible token tracking.
 *
 * LevelDB Storage: Uses composite key "tokenId:address"
 * JSON Serializable: All fields stored as JSON in LevelDB
 *
 * @version 4.0.0 (LevelDB Migration - Oct 8, 2025)
 * @author Aurigraph V11 Development Team
 */
public class TokenBalance {

    @JsonProperty("tokenId")
    private String tokenId;

    @JsonProperty("address")
    private String address;

    @JsonProperty("balance")
    private BigDecimal balance;

    @JsonProperty("lockedBalance")
    private BigDecimal lockedBalance = BigDecimal.ZERO;

    @JsonProperty("lastTransferAt")
    private Instant lastTransferAt;

    @JsonProperty("updatedAt")
    private Instant updatedAt;

    // ==================== CONSTRUCTORS ====================

    public TokenBalance() {
        this.balance = BigDecimal.ZERO;
        this.lockedBalance = BigDecimal.ZERO;
        this.updatedAt = Instant.now();
    }

    public TokenBalance(String tokenId, String address, BigDecimal balance) {
        this();
        this.tokenId = tokenId;
        this.address = address;
        this.balance = balance;
    }

    // ==================== BUSINESS LOGIC METHODS ====================

    /**
     * Update timestamp (call before each persist)
     */
    public void updateTimestamp() {
        updatedAt = Instant.now();
    }

    /**
     * Add to balance
     */
    public void add(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        this.balance = this.balance.add(amount);
        this.lastTransferAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    /**
     * Subtract from balance
     */
    public void subtract(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        if (getAvailableBalance().compareTo(amount) < 0) {
            throw new IllegalStateException("Insufficient available balance");
        }
        this.balance = this.balance.subtract(amount);
        this.lastTransferAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    /**
     * Lock balance
     */
    public void lock(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        if (getAvailableBalance().compareTo(amount) < 0) {
            throw new IllegalStateException("Insufficient available balance to lock");
        }
        this.lockedBalance = this.lockedBalance.add(amount);
        this.updatedAt = Instant.now();
    }

    /**
     * Unlock balance
     */
    public void unlock(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        if (this.lockedBalance.compareTo(amount) < 0) {
            throw new IllegalStateException("Insufficient locked balance to unlock");
        }
        this.lockedBalance = this.lockedBalance.subtract(amount);
        this.updatedAt = Instant.now();
    }

    /**
     * Get available (unlocked) balance
     */
    public BigDecimal getAvailableBalance() {
        return balance.subtract(lockedBalance);
    }

    /**
     * Check if balance is zero
     */
    public boolean isZero() {
        return balance.compareTo(BigDecimal.ZERO) == 0;
    }

    // ==================== GETTERS AND SETTERS ====================

    public String getTokenId() { return tokenId; }
    public void setTokenId(String tokenId) { this.tokenId = tokenId; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public BigDecimal getBalance() { return balance; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }

    public BigDecimal getLockedBalance() { return lockedBalance; }
    public void setLockedBalance(BigDecimal lockedBalance) { this.lockedBalance = lockedBalance; }

    public Instant getLastTransferAt() { return lastTransferAt; }
    public void setLastTransferAt(Instant lastTransferAt) { this.lastTransferAt = lastTransferAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    @Override
    public String toString() {
        return String.format("TokenBalance{tokenId='%s', address='%s', balance=%s, locked=%s}",
                tokenId, address, balance, lockedBalance);
    }
}
