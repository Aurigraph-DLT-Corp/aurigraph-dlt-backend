package io.aurigraph.v11.contracts.tokens;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.math.BigInteger;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import io.quarkus.logging.Log;
import io.aurigraph.v11.contracts.rwa.AssetDigitalTwin;
import io.aurigraph.v11.contracts.rwa.DigitalTwinService;

/**
 * ERC-1155 Multi-Token Standard Implementation for Aurigraph V11
 * Supports both fungible and non-fungible tokens in a single contract
 * Features: Batch operations, RWA integration, efficient storage
 */
@ApplicationScoped
public class ERC1155MultiToken {

    @Inject
    DigitalTwinService digitalTwinService;

    // Contract registry and state management
    private final Map<String, MultiTokenContract> contracts = new ConcurrentHashMap<>();
    
    // address -> tokenId -> balance
    private final Map<String, Map<BigInteger, Map<String, BigInteger>>> balances = new ConcurrentHashMap<>();
    
    // owner -> operator -> approved
    private final Map<String, Map<String, Map<String, Boolean>>> operatorApprovals = new ConcurrentHashMap<>();
    
    // tokenId -> total supply
    private final Map<String, Map<BigInteger, BigInteger>> tokenSupplies = new ConcurrentHashMap<>();
    
    // tokenId -> metadata URI
    private final Map<String, Map<BigInteger, String>> tokenURIs = new ConcurrentHashMap<>();
    
    // tokenId -> digital twin ID
    private final Map<String, Map<BigInteger, String>> tokenDigitalTwins = new ConcurrentHashMap<>();
    
    private final AtomicLong contractCounter = new AtomicLong(0);

    /**
     * Deploy a new ERC-1155 multi-token contract
     */
    public Uni<String> deployContract(ERC1155DeployRequest request) {
        return Uni.createFrom().item(() -> {
            String contractAddress = generateContractAddress(request);
            
            MultiTokenContract contract = new MultiTokenContract(
                contractAddress,
                request.getName(),
                request.getSymbol(),
                request.getOwner(),
                request.getBaseURI(),
                Instant.now()
            );
            
            // Initialize storage
            contracts.put(contractAddress, contract);
            balances.put(contractAddress, new ConcurrentHashMap<>());
            operatorApprovals.put(contractAddress, new ConcurrentHashMap<>());
            tokenSupplies.put(contractAddress, new ConcurrentHashMap<>());
            tokenURIs.put(contractAddress, new ConcurrentHashMap<>());
            tokenDigitalTwins.put(contractAddress, new ConcurrentHashMap<>());
            
            Log.infof("Deployed ERC-1155 contract %s (%s) at address %s", 
                request.getName(), request.getSymbol(), contractAddress);
            
            return contractAddress;
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    /**
     * Mint tokens (supports both fungible and non-fungible)
     */
    public Uni<Boolean> mint(String contractAddress, String to, BigInteger tokenId, 
                            BigInteger amount, String tokenURI, String assetId, String caller) {
        return Uni.createFrom().item(() -> {
            MultiTokenContract contract = contracts.get(contractAddress);
            if (contract == null) {
                throw new TokenNotFoundException("Contract not found: " + contractAddress);
            }
            
            // Check authorization (owner only for now)
            if (!caller.equals(contract.getOwner())) {
                throw new UnauthorizedException("Only owner can mint tokens");
            }
            
            // Initialize token if new
            Map<BigInteger, Map<String, BigInteger>> contractBalances = balances.get(contractAddress);
            contractBalances.computeIfAbsent(tokenId, k -> new ConcurrentHashMap<>());
            
            // Mint tokens
            Map<String, BigInteger> tokenBalances = contractBalances.get(tokenId);
            BigInteger currentBalance = tokenBalances.getOrDefault(to, BigInteger.ZERO);
            tokenBalances.put(to, currentBalance.add(amount));
            
            // Update total supply
            Map<BigInteger, BigInteger> supplies = tokenSupplies.get(contractAddress);
            BigInteger currentSupply = supplies.getOrDefault(tokenId, BigInteger.ZERO);
            supplies.put(tokenId, currentSupply.add(amount));
            
            // Set token URI if provided
            if (tokenURI != null && !tokenURI.isEmpty()) {
                tokenURIs.get(contractAddress).put(tokenId, tokenURI);
            }
            
            // Create digital twin for RWA if asset ID provided
            if (assetId != null && !assetId.isEmpty()) {
                String twinId = digitalTwinService.createDigitalTwin(
                    assetId, "MULTI_TOKEN", "{\"tokenId\": \"" + tokenId.toString() + "\"}"
                ).await().indefinitely();
                tokenDigitalTwins.get(contractAddress).put(tokenId, twinId);
            }
            
            Log.infof("Minted %s of token %s to %s in contract %s", 
                amount, tokenId, to, contractAddress);
            
            return true;
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    /**
     * Batch mint multiple tokens
     */
    public Uni<Boolean> mintBatch(String contractAddress, String to, BigInteger[] tokenIds, 
                                 BigInteger[] amounts, String[] tokenURIs, String caller) {
        return Uni.createFrom().item(() -> {
            if (tokenIds.length != amounts.length) {
                throw new IllegalArgumentException("Token IDs and amounts length mismatch");
            }
            
            for (int i = 0; i < tokenIds.length; i++) {
                String uri = (tokenURIs != null && i < tokenURIs.length) ? tokenURIs[i] : null;
                mint(contractAddress, to, tokenIds[i], amounts[i], uri, null, caller)
                    .await().indefinitely();
            }
            
            Log.infof("Batch minted %d token types to %s in contract %s", 
                tokenIds.length, to, contractAddress);
            
            return true;
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    /**
     * Get balance of a specific token for an account
     */
    public Uni<BigInteger> balanceOf(String contractAddress, String account, BigInteger tokenId) {
        return Uni.createFrom().item(() -> {
            Map<BigInteger, Map<String, BigInteger>> contractBalances = balances.get(contractAddress);
            if (contractBalances == null) {
                throw new TokenNotFoundException("Contract not found: " + contractAddress);
            }
            
            Map<String, BigInteger> tokenBalances = contractBalances.get(tokenId);
            if (tokenBalances == null) {
                return BigInteger.ZERO;
            }
            
            return tokenBalances.getOrDefault(account, BigInteger.ZERO);
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    /**
     * Get balances of multiple tokens for multiple accounts
     */
    public Uni<BigInteger[]> balanceOfBatch(String contractAddress, String[] accounts, BigInteger[] tokenIds) {
        return Uni.createFrom().item(() -> {
            if (accounts.length != tokenIds.length) {
                throw new IllegalArgumentException("Accounts and token IDs length mismatch");
            }
            
            BigInteger[] results = new BigInteger[accounts.length];
            for (int i = 0; i < accounts.length; i++) {
                results[i] = balanceOf(contractAddress, accounts[i], tokenIds[i])
                    .await().indefinitely();
            }
            
            return results;
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    /**
     * Transfer tokens from one account to another
     */
    public Uni<Boolean> safeTransferFrom(String contractAddress, String from, String to, 
                                        BigInteger tokenId, BigInteger amount, byte[] data, String caller) {
        return Uni.createFrom().item(() -> {
            // Check authorization
            if (!from.equals(caller) && !isApprovedForAll0(contractAddress, from, caller)) {
                throw new UnauthorizedException("Caller is not owner nor approved");
            }
            
            Map<BigInteger, Map<String, BigInteger>> contractBalances = balances.get(contractAddress);
            if (contractBalances == null) {
                throw new TokenNotFoundException("Contract not found: " + contractAddress);
            }
            
            Map<String, BigInteger> tokenBalances = contractBalances.get(tokenId);
            if (tokenBalances == null) {
                throw new TokenNotFoundException("Token not found: " + tokenId);
            }
            
            BigInteger senderBalance = tokenBalances.getOrDefault(from, BigInteger.ZERO);
            if (senderBalance.compareTo(amount) < 0) {
                throw new TokenOperationException("Insufficient balance");
            }
            
            // Execute transfer
            tokenBalances.put(from, senderBalance.subtract(amount));
            BigInteger receiverBalance = tokenBalances.getOrDefault(to, BigInteger.ZERO);
            tokenBalances.put(to, receiverBalance.add(amount));
            
            // Update digital twin if exists
            Map<BigInteger, String> contractDigitalTwins = tokenDigitalTwins.get(contractAddress);
            if (contractDigitalTwins != null) {
                String digitalTwinId = contractDigitalTwins.get(tokenId);
                if (digitalTwinId != null) {
                    AssetDigitalTwin digitalTwin = digitalTwinService.getDigitalTwin(digitalTwinId)
                        .await().indefinitely();
                    if (digitalTwin != null) {
                        digitalTwin.recordOwnershipChange(from, to, new java.math.BigDecimal(amount));
                    }
                }
            }
            
            Log.infof("Transferred %s of token %s from %s to %s in contract %s", 
                amount, tokenId, from, to, contractAddress);
            
            return true;
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    /**
     * Batch transfer multiple tokens
     */
    public Uni<Boolean> safeBatchTransferFrom(String contractAddress, String from, String to, 
                                             BigInteger[] tokenIds, BigInteger[] amounts, 
                                             byte[] data, String caller) {
        return Uni.createFrom().item(() -> {
            if (tokenIds.length != amounts.length) {
                throw new IllegalArgumentException("Token IDs and amounts length mismatch");
            }
            
            for (int i = 0; i < tokenIds.length; i++) {
                safeTransferFrom(contractAddress, from, to, tokenIds[i], amounts[i], data, caller)
                    .await().indefinitely();
            }
            
            Log.infof("Batch transferred %d token types from %s to %s in contract %s", 
                tokenIds.length, from, to, contractAddress);
            
            return true;
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    /**
     * Set approval for all tokens
     */
    public Uni<Boolean> setApprovalForAll(String contractAddress, String operator, 
                                         boolean approved, String caller) {
        return Uni.createFrom().item(() -> {
            Map<String, Map<String, Boolean>> contractApprovals = operatorApprovals.get(contractAddress);
            if (contractApprovals == null) {
                throw new TokenNotFoundException("Contract not found: " + contractAddress);
            }
            
            contractApprovals.computeIfAbsent(caller, k -> new ConcurrentHashMap<>())
                           .put(operator, approved);
            
            Log.infof("Set approval for all: operator=%s, approved=%s, owner=%s in contract %s", 
                operator, approved, caller, contractAddress);
            
            return true;
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    /**
     * Check if operator is approved for all tokens of owner
     */
    public Uni<Boolean> isApprovedForAll(String contractAddress, String owner, String operator) {
        return Uni.createFrom().item(() -> isApprovedForAll0(contractAddress, owner, operator))
            .runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    /**
     * Get URI for a token
     */
    public Uni<String> uri(String contractAddress, BigInteger tokenId) {
        return Uni.createFrom().item(() -> {
            Map<BigInteger, String> contractURIs = tokenURIs.get(contractAddress);
            if (contractURIs == null) {
                throw new TokenNotFoundException("Contract not found: " + contractAddress);
            }
            
            String tokenURI = contractURIs.get(tokenId);
            if (tokenURI != null) {
                return tokenURI;
            }
            
            // Fall back to base URI with token ID
            MultiTokenContract contract = contracts.get(contractAddress);
            if (contract != null && contract.getBaseURI() != null) {
                return contract.getBaseURI() + tokenId.toString();
            }
            
            return null;
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    /**
     * Get total supply of a token
     */
    public Uni<BigInteger> totalSupply(String contractAddress, BigInteger tokenId) {
        return Uni.createFrom().item(() -> {
            Map<BigInteger, BigInteger> supplies = tokenSupplies.get(contractAddress);
            if (supplies == null) {
                throw new TokenNotFoundException("Contract not found: " + contractAddress);
            }
            
            return supplies.getOrDefault(tokenId, BigInteger.ZERO);
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    /**
     * Burn tokens
     */
    public Uni<Boolean> burn(String contractAddress, String from, BigInteger tokenId, 
                            BigInteger amount, String caller) {
        return Uni.createFrom().item(() -> {
            // Check authorization
            if (!from.equals(caller) && !isApprovedForAll0(contractAddress, from, caller)) {
                throw new UnauthorizedException("Caller is not owner nor approved");
            }
            
            Map<BigInteger, Map<String, BigInteger>> contractBalances = balances.get(contractAddress);
            if (contractBalances == null) {
                throw new TokenNotFoundException("Contract not found: " + contractAddress);
            }
            
            Map<String, BigInteger> tokenBalances = contractBalances.get(tokenId);
            if (tokenBalances == null) {
                throw new TokenNotFoundException("Token not found: " + tokenId);
            }
            
            BigInteger balance = tokenBalances.getOrDefault(from, BigInteger.ZERO);
            if (balance.compareTo(amount) < 0) {
                throw new TokenOperationException("Insufficient balance to burn");
            }
            
            // Execute burn
            tokenBalances.put(from, balance.subtract(amount));
            
            // Update total supply
            Map<BigInteger, BigInteger> supplies = tokenSupplies.get(contractAddress);
            BigInteger currentSupply = supplies.getOrDefault(tokenId, BigInteger.ZERO);
            supplies.put(tokenId, currentSupply.subtract(amount));
            
            Log.infof("Burned %s of token %s from %s in contract %s", 
                amount, tokenId, from, contractAddress);
            
            return true;
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    /**
     * Get all tokens owned by an account
     */
    public Uni<List<TokenBalance>> getTokensOfOwner(String contractAddress, String owner) {
        return Uni.createFrom().item(() -> {
            Map<BigInteger, Map<String, BigInteger>> contractBalances = balances.get(contractAddress);
            if (contractBalances == null) {
                throw new TokenNotFoundException("Contract not found: " + contractAddress);
            }
            
            List<TokenBalance> result = new ArrayList<>();
            for (Map.Entry<BigInteger, Map<String, BigInteger>> tokenEntry : contractBalances.entrySet()) {
                BigInteger tokenId = tokenEntry.getKey();
                BigInteger balance = tokenEntry.getValue().getOrDefault(owner, BigInteger.ZERO);
                
                if (balance.compareTo(BigInteger.ZERO) > 0) {
                    String tokenURI = tokenURIs.get(contractAddress).get(tokenId);
                    String digitalTwinId = tokenDigitalTwins.get(contractAddress).get(tokenId);
                    
                    result.add(new TokenBalance(tokenId, balance, tokenURI, digitalTwinId));
                }
            }
            
            return result;
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    /**
     * Get contract information
     */
    public Uni<MultiTokenContract> getContract(String contractAddress) {
        return Uni.createFrom().item(() -> {
            MultiTokenContract contract = contracts.get(contractAddress);
            if (contract == null) {
                throw new TokenNotFoundException("Contract not found: " + contractAddress);
            }
            return contract;
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    // Private helper methods

    private boolean isApprovedForAll0(String contractAddress, String owner, String operator) {
        Map<String, Map<String, Boolean>> contractApprovals = operatorApprovals.get(contractAddress);
        if (contractApprovals == null) return false;
        
        Map<String, Boolean> ownerApprovals = contractApprovals.get(owner);
        return ownerApprovals != null && Boolean.TRUE.equals(ownerApprovals.get(operator));
    }

    private String generateContractAddress(ERC1155DeployRequest request) {
        long counter = contractCounter.incrementAndGet();
        String input = request.getName() + request.getSymbol() + 
                      request.getOwner() + counter + System.nanoTime();
        return "0x" + Integer.toHexString(input.hashCode()).toUpperCase() + 
               Long.toHexString(System.currentTimeMillis()).toUpperCase();
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
 * Multi-token contract information
 */
class MultiTokenContract {
    private final String address;
    private final String name;
    private final String symbol;
    private final String owner;
    private final String baseURI;
    private final Instant deployedAt;

    public MultiTokenContract(String address, String name, String symbol, String owner, 
                             String baseURI, Instant deployedAt) {
        this.address = address;
        this.name = name;
        this.symbol = symbol;
        this.owner = owner;
        this.baseURI = baseURI;
        this.deployedAt = deployedAt;
    }

    // Getters
    public String getAddress() { return address; }
    public String getName() { return name; }
    public String getSymbol() { return symbol; }
    public String getOwner() { return owner; }
    public String getBaseURI() { return baseURI; }
    public Instant getDeployedAt() { return deployedAt; }

    @Override
    public String toString() {
        return String.format("MultiTokenContract{address='%s', name='%s', symbol='%s'}",
            address, name, symbol);
    }
}

/**
 * Token balance information
 */
class TokenBalance {
    private final BigInteger tokenId;
    private final BigInteger balance;
    private final String tokenURI;
    private final String digitalTwinId;

    public TokenBalance(BigInteger tokenId, BigInteger balance, String tokenURI, String digitalTwinId) {
        this.tokenId = tokenId;
        this.balance = balance;
        this.tokenURI = tokenURI;
        this.digitalTwinId = digitalTwinId;
    }

    // Getters
    public BigInteger getTokenId() { return tokenId; }
    public BigInteger getBalance() { return balance; }
    public String getTokenURI() { return tokenURI; }
    public String getDigitalTwinId() { return digitalTwinId; }

    @Override
    public String toString() {
        return String.format("TokenBalance{tokenId=%s, balance=%s, uri='%s'}",
            tokenId, balance, tokenURI);
    }
}

/**
 * ERC-1155 deployment request
 */
class ERC1155DeployRequest {
    private String name;
    private String symbol;
    private String owner;
    private String baseURI;
    private Map<String, Object> metadata;

    public ERC1155DeployRequest(String name, String symbol, String owner, String baseURI) {
        this.name = name;
        this.symbol = symbol;
        this.owner = owner;
        this.baseURI = baseURI;
        this.metadata = new HashMap<>();
    }

    // Getters and setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }

    public String getOwner() { return owner; }
    public void setOwner(String owner) { this.owner = owner; }

    public String getBaseURI() { return baseURI; }
    public void setBaseURI(String baseURI) { this.baseURI = baseURI; }

    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
}