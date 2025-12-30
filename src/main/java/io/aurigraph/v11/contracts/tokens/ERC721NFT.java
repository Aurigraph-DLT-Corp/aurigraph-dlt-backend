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
 * ERC-721 NFT Implementation for Real World Assets (RWA)
 * Provides full ERC-721 standard compliance with RWA-specific metadata
 * Features: Asset tokenization, digital twin integration, provenance tracking
 */
@ApplicationScoped
public class ERC721NFT {

    @Inject
    DigitalTwinService digitalTwinService;

    // NFT collections and token registry
    private final Map<String, NFTCollection> collections = new ConcurrentHashMap<>();
    private final Map<String, Map<BigInteger, NFTToken>> collectionTokens = new ConcurrentHashMap<>();
    private final Map<String, Map<BigInteger, String>> tokenOwners = new ConcurrentHashMap<>();
    private final Map<String, Map<BigInteger, String>> tokenApprovals = new ConcurrentHashMap<>();
    private final Map<String, Map<String, Map<String, Boolean>>> operatorApprovals = new ConcurrentHashMap<>();
    
    private final AtomicLong collectionCounter = new AtomicLong(0);

    /**
     * Deploy a new ERC-721 NFT collection for RWA
     */
    public Uni<String> deployCollection(ERC721DeployRequest request) {
        return Uni.createFrom().item(() -> {
            String collectionAddress = generateCollectionAddress(request);
            
            NFTCollection collection = new NFTCollection(
                collectionAddress,
                request.getName(),
                request.getSymbol(),
                request.getOwner(),
                request.getBaseTokenURI(),
                request.getAssetType(),
                Instant.now()
            );
            
            // Initialize collection storage
            collections.put(collectionAddress, collection);
            collectionTokens.put(collectionAddress, new ConcurrentHashMap<>());
            tokenOwners.put(collectionAddress, new ConcurrentHashMap<>());
            tokenApprovals.put(collectionAddress, new ConcurrentHashMap<>());
            operatorApprovals.put(collectionAddress, new ConcurrentHashMap<String, Map<String, Boolean>>());
            
            Log.infof("Deployed ERC-721 collection %s (%s) at address %s for asset type %s", 
                request.getName(), request.getSymbol(), collectionAddress, request.getAssetType());
            
            return collectionAddress;
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    /**
     * Mint a new NFT representing a real-world asset
     */
    public Uni<BigInteger> mintRWA(String collectionAddress, String to, RWAMetadata metadata) {
        return Uni.createFrom().item(() -> {
            NFTCollection collection = collections.get(collectionAddress);
            if (collection == null) {
                throw new NFTNotFoundException("Collection not found: " + collectionAddress);
            }
            
            // Generate token ID
            BigInteger tokenId = generateTokenId(collectionAddress);
            
            // Create digital twin for the asset
            String twinId = digitalTwinService.createDigitalTwin(
                metadata.getAssetId(),
                collection.getAssetType(),
                metadata.toString()
            ).await().indefinitely();
            
            // Create NFT token
            NFTToken token = new NFTToken(
                tokenId,
                collectionAddress,
                to,
                metadata.getAssetId(),
                twinId,
                metadata.getTokenURI(),
                metadata.toMap(),
                Instant.now()
            );
            
            // Store token and ownership
            collectionTokens.get(collectionAddress).put(tokenId, token);
            tokenOwners.get(collectionAddress).put(tokenId, to);
            
            // Update collection stats
            collection.incrementTotalSupply();
            
            Log.infof("Minted NFT token %s for asset %s in collection %s to %s", 
                tokenId, metadata.getAssetId(), collectionAddress, to);
            
            return tokenId;
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    /**
     * Get owner of a token
     */
    public Uni<String> ownerOf(String collectionAddress, BigInteger tokenId) {
        return Uni.createFrom().item(() -> {
            Map<BigInteger, String> owners = tokenOwners.get(collectionAddress);
            if (owners == null) {
                throw new NFTNotFoundException("Collection not found: " + collectionAddress);
            }
            
            String owner = owners.get(tokenId);
            if (owner == null) {
                throw new NFTNotFoundException("Token not found: " + tokenId);
            }
            
            return owner;
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    /**
     * Get balance of an owner
     */
    public Uni<Long> balanceOf(String collectionAddress, String owner) {
        return Uni.createFrom().item(() -> {
            Map<BigInteger, String> owners = tokenOwners.get(collectionAddress);
            if (owners == null) {
                throw new NFTNotFoundException("Collection not found: " + collectionAddress);
            }
            
            return owners.values().stream()
                .mapToLong(tokenOwner -> tokenOwner.equals(owner) ? 1 : 0)
                .sum();
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    /**
     * Transfer token from one owner to another
     */
    public Uni<Boolean> transferFrom(String collectionAddress, String from, String to, BigInteger tokenId, String caller) {
        return Uni.createFrom().item(() -> {
            // Validate collection exists
            if (!collections.containsKey(collectionAddress)) {
                throw new NFTNotFoundException("Collection not found: " + collectionAddress);
            }
            
            Map<BigInteger, String> owners = tokenOwners.get(collectionAddress);
            String currentOwner = owners.get(tokenId);
            
            if (currentOwner == null) {
                throw new NFTNotFoundException("Token not found: " + tokenId);
            }
            
            if (!currentOwner.equals(from)) {
                throw new NFTOperationException("From address is not the owner");
            }
            
            // Check authorization
            if (!isApprovedOrOwner(collectionAddress, caller, tokenId)) {
                throw new UnauthorizedException("Caller is not owner nor approved");
            }
            
            // Execute transfer
            owners.put(tokenId, to);
            
            // Clear any token approval
            Map<BigInteger, String> approvals = tokenApprovals.get(collectionAddress);
            if (approvals != null) {
                approvals.remove(tokenId);
            }
            
            // Update digital twin ownership
            NFTToken token = collectionTokens.get(collectionAddress).get(tokenId);
            if (token != null && token.getDigitalTwinId() != null) {
                AssetDigitalTwin digitalTwin = digitalTwinService.getDigitalTwin(token.getDigitalTwinId())
                    .await().indefinitely();
                if (digitalTwin != null) {
                    digitalTwin.recordOwnershipChange(to, java.time.Instant.now().toString(), java.math.BigDecimal.ONE);
                }
            }
            
            Log.infof("Transferred NFT token %s from %s to %s in collection %s", 
                tokenId, from, to, collectionAddress);
            
            return true;
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    /**
     * Safe transfer with data
     */
    public Uni<Boolean> safeTransferFrom(String collectionAddress, String from, String to, 
                                        BigInteger tokenId, byte[] data, String caller) {
        // For now, we'll implement this as a regular transfer
        // In a full implementation, this would check if the recipient can handle NFTs
        return transferFrom(collectionAddress, from, to, tokenId, caller);
    }

    /**
     * Approve another address to transfer a specific token
     */
    public Uni<Boolean> approve(String collectionAddress, String to, BigInteger tokenId, String caller) {
        return Uni.createFrom().item(() -> {
            Map<BigInteger, String> owners = tokenOwners.get(collectionAddress);
            if (owners == null) {
                throw new NFTNotFoundException("Collection not found: " + collectionAddress);
            }
            
            String owner = owners.get(tokenId);
            if (owner == null) {
                throw new NFTNotFoundException("Token not found: " + tokenId);
            }
            
            // Check if caller is owner or approved operator
            if (!owner.equals(caller) && !isApprovedForAll0(collectionAddress, owner, caller)) {
                throw new UnauthorizedException("Caller is not owner nor approved operator");
            }
            
            // Set approval
            tokenApprovals.get(collectionAddress).put(tokenId, to);
            
            Log.infof("Approved %s for token %s in collection %s", to, tokenId, collectionAddress);
            
            return true;
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    /**
     * Set approval for all tokens of an owner
     */
    public Uni<Boolean> setApprovalForAll(String collectionAddress, String operator, 
                                         boolean approved, String caller) {
        return Uni.createFrom().item(() -> {
            if (!collections.containsKey(collectionAddress)) {
                throw new NFTNotFoundException("Collection not found: " + collectionAddress);
            }
            
            // Set operator approval
            operatorApprovals.get(collectionAddress)
                .computeIfAbsent(caller, k -> new ConcurrentHashMap<String, Boolean>())
                .put(operator, approved);
            
            Log.infof("Set approval for all tokens: operator=%s, approved=%s, owner=%s in collection %s", 
                operator, approved, caller, collectionAddress);
            
            return true;
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    /**
     * Get approved address for a token
     */
    public Uni<String> getApproved(String collectionAddress, BigInteger tokenId) {
        return Uni.createFrom().item(() -> {
            Map<BigInteger, String> approvals = tokenApprovals.get(collectionAddress);
            if (approvals == null) {
                throw new NFTNotFoundException("Collection not found: " + collectionAddress);
            }
            
            // Check if token exists
            Map<BigInteger, String> owners = tokenOwners.get(collectionAddress);
            if (owners == null || !owners.containsKey(tokenId)) {
                throw new NFTNotFoundException("Token not found: " + tokenId);
            }
            
            return approvals.get(tokenId);
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    /**
     * Check if operator is approved for all tokens of owner
     */
    public Uni<Boolean> isApprovedForAll(String collectionAddress, String owner, String operator) {
        return Uni.createFrom().item(() -> isApprovedForAll0(collectionAddress, owner, operator))
            .runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    /**
     * Get token URI (metadata location)
     */
    public Uni<String> tokenURI(String collectionAddress, BigInteger tokenId) {
        return Uni.createFrom().item(() -> {
            Map<BigInteger, NFTToken> tokens = collectionTokens.get(collectionAddress);
            if (tokens == null) {
                throw new NFTNotFoundException("Collection not found: " + collectionAddress);
            }
            
            NFTToken token = tokens.get(tokenId);
            if (token == null) {
                throw new NFTNotFoundException("Token not found: " + tokenId);
            }
            
            return token.getTokenURI();
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    /**
     * Get token with full RWA metadata
     */
    public Uni<NFTToken> getToken(String collectionAddress, BigInteger tokenId) {
        return Uni.createFrom().item(() -> {
            Map<BigInteger, NFTToken> tokens = collectionTokens.get(collectionAddress);
            if (tokens == null) {
                throw new NFTNotFoundException("Collection not found: " + collectionAddress);
            }
            
            NFTToken token = tokens.get(tokenId);
            if (token == null) {
                throw new NFTNotFoundException("Token not found: " + tokenId);
            }
            
            return token;
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    /**
     * Get all tokens owned by an address
     */
    public Uni<List<NFTToken>> getTokensOfOwner(String collectionAddress, String owner) {
        return Uni.createFrom().item(() -> {
            Map<BigInteger, String> owners = tokenOwners.get(collectionAddress);
            Map<BigInteger, NFTToken> tokens = collectionTokens.get(collectionAddress);
            
            if (owners == null || tokens == null) {
                throw new NFTNotFoundException("Collection not found: " + collectionAddress);
            }
            
            List<NFTToken> result = new ArrayList<>();
            for (Map.Entry<BigInteger, String> entry : owners.entrySet()) {
                if (owner.equals(entry.getValue())) {
                    NFTToken token = tokens.get(entry.getKey());
                    if (token != null) {
                        result.add(token);
                    }
                }
            }
            
            return result;
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    /**
     * Get collection information
     */
    public Uni<NFTCollection> getCollection(String collectionAddress) {
        return Uni.createFrom().item(() -> {
            NFTCollection collection = collections.get(collectionAddress);
            if (collection == null) {
                throw new NFTNotFoundException("Collection not found: " + collectionAddress);
            }
            return collection;
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    /**
     * Get all collections
     */
    public Uni<List<NFTCollection>> getAllCollections() {
        return Uni.createFrom().item(() -> {
            List<NFTCollection> result = new ArrayList<>(collections.values());
            return result;
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    // Private helper methods

    private boolean isApprovedOrOwner(String collectionAddress, String spender, BigInteger tokenId) {
        Map<BigInteger, String> owners = tokenOwners.get(collectionAddress);
        String owner = owners.get(tokenId);
        
        if (owner == null) return false;
        if (owner.equals(spender)) return true;
        
        // Check token approval
        Map<BigInteger, String> approvals = tokenApprovals.get(collectionAddress);
        if (approvals != null && spender.equals(approvals.get(tokenId))) {
            return true;
        }
        
        // Check operator approval
        return isApprovedForAll0(collectionAddress, owner, spender);
    }

    private boolean isApprovedForAll0(String collectionAddress, String owner, String operator) {
        Map<String, Map<String, Boolean>> collectionApprovals = operatorApprovals.get(collectionAddress);
        if (collectionApprovals == null) return false;
        
        Map<String, Boolean> ownerOperators = collectionApprovals.get(owner);
        return ownerOperators != null && Boolean.TRUE.equals(ownerOperators.get(operator));
    }

    private String generateCollectionAddress(ERC721DeployRequest request) {
        long counter = collectionCounter.incrementAndGet();
        String input = request.getName() + request.getSymbol() + 
                      request.getOwner() + counter + System.nanoTime();
        return "0x" + Integer.toHexString(input.hashCode()).toUpperCase() + 
               Long.toHexString(System.currentTimeMillis()).toUpperCase();
    }

    private BigInteger generateTokenId(String collectionAddress) {
        NFTCollection collection = collections.get(collectionAddress);
        if (collection != null) {
            return BigInteger.valueOf(collection.getNextTokenId());
        }
        return BigInteger.ONE;
    }

    // Exception classes
    public static class NFTNotFoundException extends RuntimeException {
        public NFTNotFoundException(String message) { super(message); }
    }

    public static class NFTOperationException extends RuntimeException {
        public NFTOperationException(String message) { super(message); }
    }

    public static class UnauthorizedException extends RuntimeException {
        public UnauthorizedException(String message) { super(message); }
    }
}

/**
 * NFT Collection information
 */
class NFTCollection {
    private final String address;
    private final String name;
    private final String symbol;
    private final String owner;
    private final String baseTokenURI;
    private final String assetType;
    private final Instant deployedAt;
    private final AtomicLong totalSupply = new AtomicLong(0);
    private final AtomicLong tokenIdCounter = new AtomicLong(1);

    public NFTCollection(String address, String name, String symbol, String owner, 
                        String baseTokenURI, String assetType, Instant deployedAt) {
        this.address = address;
        this.name = name;
        this.symbol = symbol;
        this.owner = owner;
        this.baseTokenURI = baseTokenURI;
        this.assetType = assetType;
        this.deployedAt = deployedAt;
    }

    public long incrementTotalSupply() {
        return totalSupply.incrementAndGet();
    }

    public long getNextTokenId() {
        return tokenIdCounter.getAndIncrement();
    }

    // Getters
    public String getAddress() { return address; }
    public String getName() { return name; }
    public String getSymbol() { return symbol; }
    public String getOwner() { return owner; }
    public String getBaseTokenURI() { return baseTokenURI; }
    public String getAssetType() { return assetType; }
    public Instant getDeployedAt() { return deployedAt; }
    public long getTotalSupply() { return totalSupply.get(); }
}

/**
 * NFT Token representing a Real World Asset
 */
class NFTToken {
    private final BigInteger tokenId;
    private final String collectionAddress;
    private final String owner;
    private final String assetId;
    private final String digitalTwinId;
    private final String tokenURI;
    private final Map<String, Object> metadata;
    private final Instant mintedAt;

    public NFTToken(BigInteger tokenId, String collectionAddress, String owner, 
                   String assetId, String digitalTwinId, String tokenURI, 
                   Map<String, Object> metadata, Instant mintedAt) {
        this.tokenId = tokenId;
        this.collectionAddress = collectionAddress;
        this.owner = owner;
        this.assetId = assetId;
        this.digitalTwinId = digitalTwinId;
        this.tokenURI = tokenURI;
        this.metadata = metadata != null ? metadata : new HashMap<>();
        this.mintedAt = mintedAt;
    }

    // Getters
    public BigInteger getTokenId() { return tokenId; }
    public String getCollectionAddress() { return collectionAddress; }
    public String getOwner() { return owner; }
    public String getAssetId() { return assetId; }
    public String getDigitalTwinId() { return digitalTwinId; }
    public String getTokenURI() { return tokenURI; }
    public Map<String, Object> getMetadata() { return metadata; }
    public Instant getMintedAt() { return mintedAt; }

    @Override
    public String toString() {
        return String.format("NFTToken{tokenId=%s, collection='%s', assetId='%s', owner='%s'}",
            tokenId, collectionAddress, assetId, owner);
    }
}

/**
 * RWA-specific metadata for NFTs
 */
class RWAMetadata {
    private String assetId;
    private String assetType;
    private String tokenURI;
    private String description;
    private String location;
    private String valuation;
    private String certification;
    private Map<String, Object> attributes;

    public RWAMetadata(String assetId, String assetType, String tokenURI) {
        this.assetId = assetId;
        this.assetType = assetType;
        this.tokenURI = tokenURI;
        this.attributes = new HashMap<>();
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>(attributes);
        map.put("assetId", assetId);
        map.put("assetType", assetType);
        map.put("description", description);
        map.put("location", location);
        map.put("valuation", valuation);
        map.put("certification", certification);
        return map;
    }

    // Getters and setters
    public String getAssetId() { return assetId; }
    public void setAssetId(String assetId) { this.assetId = assetId; }

    public String getAssetType() { return assetType; }
    public void setAssetType(String assetType) { this.assetType = assetType; }

    public String getTokenURI() { return tokenURI; }
    public void setTokenURI(String tokenURI) { this.tokenURI = tokenURI; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getValuation() { return valuation; }
    public void setValuation(String valuation) { this.valuation = valuation; }

    public String getCertification() { return certification; }
    public void setCertification(String certification) { this.certification = certification; }

    public Map<String, Object> getAttributes() { return attributes; }
    public void setAttributes(Map<String, Object> attributes) { this.attributes = attributes; }
}

/**
 * ERC-721 deployment request
 */
class ERC721DeployRequest {
    private String name;
    private String symbol;
    private String owner;
    private String baseTokenURI;
    private String assetType;
    private Map<String, Object> metadata;

    public ERC721DeployRequest(String name, String symbol, String owner, String assetType) {
        this.name = name;
        this.symbol = symbol;
        this.owner = owner;
        this.assetType = assetType;
        this.baseTokenURI = "";
        this.metadata = new HashMap<>();
    }

    // Getters and setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }

    public String getOwner() { return owner; }
    public void setOwner(String owner) { this.owner = owner; }

    public String getBaseTokenURI() { return baseTokenURI; }
    public void setBaseTokenURI(String baseTokenURI) { this.baseTokenURI = baseTokenURI; }

    public String getAssetType() { return assetType; }
    public void setAssetType(String assetType) { this.assetType = assetType; }

    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
}