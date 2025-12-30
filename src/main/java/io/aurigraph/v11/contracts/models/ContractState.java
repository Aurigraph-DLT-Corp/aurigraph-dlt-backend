package io.aurigraph.v11.contracts.models;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * Contract state management for smart contract execution
 * Maintains contract storage, balances, and metadata with thread safety
 */
public class ContractState {
    
    private final String contractAddress;
    private final Map<String, BigDecimal> balances;
    private final Map<String, Map<String, BigDecimal>> allowances; // owner -> spender -> amount
    private final Map<String, String> storage; // Generic key-value storage
    private final Instant createdAt;
    private Instant lastUpdated;
    
    // Token-specific state
    private BigDecimal totalSupply;
    private String owner;
    private String name;
    private String symbol;
    private int decimals;
    
    public ContractState(String contractAddress) {
        this.contractAddress = contractAddress;
        this.balances = new ConcurrentHashMap<>();
        this.allowances = new ConcurrentHashMap<>();
        this.storage = new ConcurrentHashMap<>();
        this.createdAt = Instant.now();
        this.lastUpdated = Instant.now();
        this.totalSupply = BigDecimal.ZERO;
        this.decimals = 18; // Default decimals
    }
    
    // Balance operations
    
    /**
     * Get balance for an account
     */
    public BigDecimal getBalance(String account) {
        return balances.getOrDefault(account, BigDecimal.ZERO);
    }
    
    /**
     * Set balance for an account
     */
    public void setBalance(String account, BigDecimal balance) {
        if (balance.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Balance cannot be negative");
        }
        balances.put(account, balance);
        updateLastModified();
    }
    
    /**
     * Transfer balance between accounts
     */
    public boolean transfer(String from, String to, BigDecimal amount) {
        BigDecimal fromBalance = getBalance(from);
        if (fromBalance.compareTo(amount) < 0) {
            return false; // Insufficient balance
        }
        
        setBalance(from, fromBalance.subtract(amount));
        setBalance(to, getBalance(to).add(amount));
        return true;
    }
    
    // Allowance operations
    
    /**
     * Get allowance amount
     */
    public BigDecimal getAllowance(String owner, String spender) {
        Map<String, BigDecimal> ownerAllowances = allowances.get(owner);
        if (ownerAllowances == null) {
            return BigDecimal.ZERO;
        }
        return ownerAllowances.getOrDefault(spender, BigDecimal.ZERO);
    }
    
    /**
     * Set allowance amount
     */
    public void setAllowance(String owner, String spender, BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Allowance cannot be negative");
        }
        
        allowances.computeIfAbsent(owner, k -> new ConcurrentHashMap<>())
                  .put(spender, amount);
        updateLastModified();
    }
    
    /**
     * Transfer from allowance
     */
    public boolean transferFrom(String owner, String spender, String to, BigDecimal amount) {
        BigDecimal allowance = getAllowance(owner, spender);
        BigDecimal ownerBalance = getBalance(owner);
        
        if (allowance.compareTo(amount) < 0 || ownerBalance.compareTo(amount) < 0) {
            return false; // Insufficient allowance or balance
        }
        
        // Update allowance
        setAllowance(owner, spender, allowance.subtract(amount));
        
        // Transfer tokens
        setBalance(owner, ownerBalance.subtract(amount));
        setBalance(to, getBalance(to).add(amount));
        
        return true;
    }
    
    // Generic storage operations
    
    /**
     * Store a value by key
     */
    public void setValue(String key, String value) {
        if (key == null || key.isEmpty()) {
            throw new IllegalArgumentException("Key cannot be null or empty");
        }
        
        if (value == null) {
            storage.remove(key);
        } else {
            storage.put(key, value);
        }
        updateLastModified();
    }
    
    /**
     * Get a value by key
     */
    public String getValue(String key) {
        return storage.get(key);
    }
    
    /**
     * Check if key exists
     */
    public boolean hasKey(String key) {
        return storage.containsKey(key);
    }
    
    /**
     * Remove a key-value pair
     */
    public String removeValue(String key) {
        String value = storage.remove(key);
        if (value != null) {
            updateLastModified();
        }
        return value;
    }
    
    // Token supply operations
    
    /**
     * Increase total supply (minting)
     */
    public void increaseTotalSupply(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        totalSupply = totalSupply.add(amount);
        updateLastModified();
    }
    
    /**
     * Decrease total supply (burning)
     */
    public void decreaseTotalSupply(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        if (totalSupply.compareTo(amount) < 0) {
            throw new IllegalArgumentException("Cannot burn more than total supply");
        }
        totalSupply = totalSupply.subtract(amount);
        updateLastModified();
    }
    
    // State management
    
    /**
     * Get state size (number of stored items)
     */
    public int getStateSize() {
        return balances.size() + allowances.size() + storage.size();
    }
    
    /**
     * Clear all state (dangerous operation)
     */
    public void clearState() {
        balances.clear();
        allowances.clear();
        storage.clear();
        updateLastModified();
    }
    
    /**
     * Create a snapshot of current state
     */
    public StateSnapshot createSnapshot() {
        return new StateSnapshot(
            contractAddress,
            new ConcurrentHashMap<>(balances),
            totalSupply,
            owner,
            name,
            symbol,
            decimals,
            Instant.now()
        );
    }
    
    private void updateLastModified() {
        this.lastUpdated = Instant.now();
    }
    
    // Getters and setters
    public String getContractAddress() { return contractAddress; }
    public BigDecimal getTotalSupply() { return totalSupply; }
    public void setTotalSupply(BigDecimal totalSupply) { 
        this.totalSupply = totalSupply; 
        updateLastModified();
    }
    
    public String getOwner() { return owner; }
    public void setOwner(String owner) { 
        this.owner = owner; 
        updateLastModified();
    }
    
    public String getName() { return name; }
    public void setName(String name) { 
        this.name = name; 
        updateLastModified();
    }
    
    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { 
        this.symbol = symbol; 
        updateLastModified();
    }
    
    public int getDecimals() { return decimals; }
    public void setDecimals(int decimals) { 
        this.decimals = decimals; 
        updateLastModified();
    }
    
    public Instant getCreatedAt() { return createdAt; }
    public Instant getLastUpdated() { return lastUpdated; }
    
    public Map<String, BigDecimal> getAllBalances() { 
        return new ConcurrentHashMap<>(balances); 
    }
    
    public Map<String, String> getAllStorage() { 
        return new ConcurrentHashMap<>(storage); 
    }
    
    @Override
    public String toString() {
        return String.format("ContractState{address='%s', accounts=%d, storage=%d, totalSupply=%s}", 
            contractAddress, balances.size(), storage.size(), totalSupply);
    }
    
    /**
     * Immutable state snapshot for rollback purposes
     */
    public static class StateSnapshot {
        private final String contractAddress;
        private final Map<String, BigDecimal> balances;
        private final BigDecimal totalSupply;
        private final String owner;
        private final String name;
        private final String symbol;
        private final int decimals;
        private final Instant timestamp;
        
        public StateSnapshot(String contractAddress, Map<String, BigDecimal> balances, 
                           BigDecimal totalSupply, String owner, String name, 
                           String symbol, int decimals, Instant timestamp) {
            this.contractAddress = contractAddress;
            this.balances = balances;
            this.totalSupply = totalSupply;
            this.owner = owner;
            this.name = name;
            this.symbol = symbol;
            this.decimals = decimals;
            this.timestamp = timestamp;
        }
        
        // Getters
        public String getContractAddress() { return contractAddress; }
        public Map<String, BigDecimal> getBalances() { return balances; }
        public BigDecimal getTotalSupply() { return totalSupply; }
        public String getOwner() { return owner; }
        public String getName() { return name; }
        public String getSymbol() { return symbol; }
        public int getDecimals() { return decimals; }
        public Instant getTimestamp() { return timestamp; }
    }
}