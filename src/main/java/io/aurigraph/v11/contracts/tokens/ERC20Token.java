package io.aurigraph.v11.contracts.tokens;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import io.quarkus.logging.Log;
import io.aurigraph.v11.contracts.ContractExecutor;
import io.aurigraph.v11.contracts.models.*;

/**
 * ERC-20 Compatible Token Implementation for Aurigraph V11
 * Provides full ERC-20 standard compliance with quantum-safe security
 * Features: Minting, burning, allowances, transfer restrictions, gas optimization
 */
@ApplicationScoped
public class ERC20Token {

    @Inject
    ContractExecutor contractExecutor;

    // Token registry and state management
    private final Map<String, TokenInfo> tokens = new ConcurrentHashMap<>();
    private final Map<String, ContractState> tokenStates = new ConcurrentHashMap<>();

    /**
     * Deploy a new ERC-20 token
     */
    public Uni<String> deployToken(ERC20DeployRequest request) {
        return Uni.createFrom().item(() -> {
            String tokenAddress = generateTokenAddress(request);
            
            // Create token info
            TokenInfo tokenInfo = new TokenInfo(
                tokenAddress,
                request.getName(),
                request.getSymbol(),
                request.getDecimals(),
                request.getTotalSupply(),
                request.getOwner(),
                Instant.now()
            );
            
            // Initialize token state
            ContractState state = new ContractState(tokenAddress);
            state.setName(request.getName());
            state.setSymbol(request.getSymbol());
            state.setDecimals(request.getDecimals());
            state.setTotalSupply(request.getTotalSupply());
            state.setOwner(request.getOwner());
            
            // Mint initial supply to owner
            if (request.getTotalSupply().compareTo(BigDecimal.ZERO) > 0) {
                state.setBalance(request.getOwner(), request.getTotalSupply());
            }
            
            // Store in registries
            tokens.put(tokenAddress, tokenInfo);
            tokenStates.put(tokenAddress, state);
            
            Log.infof("Deployed ERC-20 token %s (%s) at address %s with supply %s", 
                request.getName(), request.getSymbol(), tokenAddress, request.getTotalSupply());
            
            return tokenAddress;
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    /**
     * Get token balance for an account
     */
    public Uni<BigDecimal> balanceOf(String tokenAddress, String account) {
        return executeTokenMethod(tokenAddress, "balanceOf", new Object[]{account})
            .map(result -> (BigDecimal) result);
    }

    /**
     * Transfer tokens between accounts
     */
    public Uni<Boolean> transfer(String tokenAddress, String from, String to, BigDecimal amount) {
        return executeTokenMethod(tokenAddress, "transfer", new Object[]{to, amount}, from)
            .map(result -> (Boolean) result);
    }

    /**
     * Transfer tokens from one account to another using allowance
     */
    public Uni<Boolean> transferFrom(String tokenAddress, String spender, 
                                    String from, String to, BigDecimal amount) {
        return Uni.createFrom().item(() -> {
            ContractState state = tokenStates.get(tokenAddress);
            if (state == null) {
                throw new TokenNotFoundException("Token not found: " + tokenAddress);
            }
            
            // Check allowance
            BigDecimal allowance = state.getAllowance(from, spender);
            if (allowance.compareTo(amount) < 0) {
                throw new TokenOperationException("Insufficient allowance");
            }
            
            // Check sender balance
            BigDecimal senderBalance = state.getBalance(from);
            if (senderBalance.compareTo(amount) < 0) {
                throw new TokenOperationException("Insufficient balance");
            }
            
            // Execute transfer
            state.setBalance(from, senderBalance.subtract(amount));
            state.setBalance(to, state.getBalance(to).add(amount));
            state.setAllowance(from, spender, allowance.subtract(amount));
            
            Log.infof("Transferred %s %s from %s to %s via %s", 
                amount, getTokenSymbol(tokenAddress), from, to, spender);
            
            return true;
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    /**
     * Approve spending allowance
     */
    public Uni<Boolean> approve(String tokenAddress, String owner, String spender, BigDecimal amount) {
        return executeTokenMethod(tokenAddress, "approve", new Object[]{spender, amount}, owner)
            .map(result -> (Boolean) result);
    }

    /**
     * Get allowance amount
     */
    public Uni<BigDecimal> allowance(String tokenAddress, String owner, String spender) {
        return Uni.createFrom().item(() -> {
            ContractState state = tokenStates.get(tokenAddress);
            if (state == null) {
                throw new TokenNotFoundException("Token not found: " + tokenAddress);
            }
            return state.getAllowance(owner, spender);
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    /**
     * Mint new tokens (owner only)
     */
    public Uni<Boolean> mint(String tokenAddress, String to, BigDecimal amount, String caller) {
        return Uni.createFrom().item(() -> {
            ContractState state = tokenStates.get(tokenAddress);
            if (state == null) {
                throw new TokenNotFoundException("Token not found: " + tokenAddress);
            }
            
            // Check if caller is owner
            if (!caller.equals(state.getOwner())) {
                throw new UnauthorizedException("Only owner can mint tokens");
            }
            
            // Mint tokens
            state.setBalance(to, state.getBalance(to).add(amount));
            state.increaseTotalSupply(amount);
            
            Log.infof("Minted %s %s to %s by %s", 
                amount, getTokenSymbol(tokenAddress), to, caller);
            
            return true;
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    /**
     * Burn tokens from an account
     */
    public Uni<Boolean> burn(String tokenAddress, String from, BigDecimal amount, String caller) {
        return Uni.createFrom().item(() -> {
            ContractState state = tokenStates.get(tokenAddress);
            if (state == null) {
                throw new TokenNotFoundException("Token not found: " + tokenAddress);
            }
            
            // Check if caller is owner or the account holder
            if (!caller.equals(state.getOwner()) && !caller.equals(from)) {
                throw new UnauthorizedException("Unauthorized burn attempt");
            }
            
            BigDecimal balance = state.getBalance(from);
            if (balance.compareTo(amount) < 0) {
                throw new TokenOperationException("Insufficient balance to burn");
            }
            
            // Burn tokens
            state.setBalance(from, balance.subtract(amount));
            state.decreaseTotalSupply(amount);
            
            Log.infof("Burned %s %s from %s by %s", 
                amount, getTokenSymbol(tokenAddress), from, caller);
            
            return true;
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    /**
     * Get token information
     */
    public Uni<TokenInfo> getTokenInfo(String tokenAddress) {
        return Uni.createFrom().item(() -> {
            TokenInfo info = tokens.get(tokenAddress);
            if (info == null) {
                throw new TokenNotFoundException("Token not found: " + tokenAddress);
            }
            return info;
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    /**
     * Get total supply of a token
     */
    public Uni<BigDecimal> totalSupply(String tokenAddress) {
        return Uni.createFrom().item(() -> {
            ContractState state = tokenStates.get(tokenAddress);
            if (state == null) {
                throw new TokenNotFoundException("Token not found: " + tokenAddress);
            }
            return state.getTotalSupply();
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    /**
     * Get token name
     */
    public Uni<String> name(String tokenAddress) {
        return Uni.createFrom().item(() -> {
            TokenInfo info = tokens.get(tokenAddress);
            return info != null ? info.getName() : null;
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    /**
     * Get token symbol
     */
    public Uni<String> symbol(String tokenAddress) {
        return Uni.createFrom().item(() -> {
            TokenInfo info = tokens.get(tokenAddress);
            return info != null ? info.getSymbol() : null;
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    /**
     * Get token decimals
     */
    public Uni<Integer> decimals(String tokenAddress) {
        return Uni.createFrom().item(() -> {
            TokenInfo info = tokens.get(tokenAddress);
            return info != null ? info.getDecimals() : null;
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    /**
     * Get all deployed tokens
     */
    public Uni<List<TokenInfo>> getAllTokens() {
        return Uni.createFrom().item(() -> {
            List<TokenInfo> result = new ArrayList<>(tokens.values());
            return result;
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    /**
     * Get token statistics
     */
    public Uni<Map<String, Object>> getTokenStatistics(String tokenAddress) {
        return Uni.createFrom().item(() -> {
            ContractState state = tokenStates.get(tokenAddress);
            TokenInfo info = tokens.get(tokenAddress);
            
            if (state == null || info == null) {
                throw new TokenNotFoundException("Token not found: " + tokenAddress);
            }
            
            Map<String, Object> stats = new HashMap<>();
            stats.put("name", info.getName());
            stats.put("symbol", info.getSymbol());
            stats.put("decimals", info.getDecimals());
            stats.put("totalSupply", state.getTotalSupply());
            stats.put("owner", state.getOwner());
            stats.put("uniqueHolders", state.getAllBalances().size());
            stats.put("deployedAt", info.getDeployedAt());
            stats.put("contractAddress", tokenAddress);
            
            return stats;
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    // Private helper methods

    private Uni<Object> executeTokenMethod(String tokenAddress, String method, Object[] params) {
        return executeTokenMethod(tokenAddress, method, params, "0x0");
    }

    private Uni<Object> executeTokenMethod(String tokenAddress, String method, Object[] params, String caller) {
        ExecutionRequest request = new ExecutionRequest(tokenAddress, method, params, 100000L, caller);
        return contractExecutor.executeContract(request)
            .map(result -> {
                if (result.getStatus() != ExecutionStatus.SUCCESS) {
                    throw new TokenOperationException("Token operation failed: " + result.getError());
                }
                return result.getResult();
            });
    }

    private String generateTokenAddress(ERC20DeployRequest request) {
        String input = request.getName() + request.getSymbol() + 
                      request.getOwner() + System.nanoTime();
        return "0x" + Integer.toHexString(input.hashCode()).toUpperCase() + 
               Long.toHexString(System.currentTimeMillis()).toUpperCase();
    }

    private String getTokenSymbol(String tokenAddress) {
        TokenInfo info = tokens.get(tokenAddress);
        return info != null ? info.getSymbol() : "UNKNOWN";
    }

    // Exception classes
    public static class TokenNotFoundException extends RuntimeException {
        public TokenNotFoundException(String message) { super(message); }
    }

    public static class TokenOperationException extends RuntimeException {
        public TokenOperationException(String message) { super(message); }
    }

    public static class UnauthorizedException extends RuntimeException {
        public UnauthorizedException(String message) { super(message); }
    }
}

/**
 * ERC-20 Token deployment request
 */
class ERC20DeployRequest {
    private String name;
    private String symbol;
    private int decimals = 18;
    private BigDecimal totalSupply;
    private String owner;
    private Map<String, Object> metadata;

    public ERC20DeployRequest(String name, String symbol, int decimals, 
                             BigDecimal totalSupply, String owner) {
        this.name = name;
        this.symbol = symbol;
        this.decimals = decimals;
        this.totalSupply = totalSupply;
        this.owner = owner;
        this.metadata = new HashMap<>();
    }

    // Getters and setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }

    public int getDecimals() { return decimals; }
    public void setDecimals(int decimals) { this.decimals = decimals; }

    public BigDecimal getTotalSupply() { return totalSupply; }
    public void setTotalSupply(BigDecimal totalSupply) { this.totalSupply = totalSupply; }

    public String getOwner() { return owner; }
    public void setOwner(String owner) { this.owner = owner; }

    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
}

/**
 * Token information
 */
class TokenInfo {
    private final String address;
    private final String name;
    private final String symbol;
    private final int decimals;
    private final BigDecimal initialSupply;
    private final String owner;
    private final Instant deployedAt;

    public TokenInfo(String address, String name, String symbol, int decimals, 
                    BigDecimal initialSupply, String owner, Instant deployedAt) {
        this.address = address;
        this.name = name;
        this.symbol = symbol;
        this.decimals = decimals;
        this.initialSupply = initialSupply;
        this.owner = owner;
        this.deployedAt = deployedAt;
    }

    // Getters
    public String getAddress() { return address; }
    public String getName() { return name; }
    public String getSymbol() { return symbol; }
    public int getDecimals() { return decimals; }
    public BigDecimal getInitialSupply() { return initialSupply; }
    public String getOwner() { return owner; }
    public Instant getDeployedAt() { return deployedAt; }

    @Override
    public String toString() {
        return String.format("TokenInfo{address='%s', name='%s', symbol='%s', decimals=%d, supply=%s}",
            address, name, symbol, decimals, initialSupply);
    }
}