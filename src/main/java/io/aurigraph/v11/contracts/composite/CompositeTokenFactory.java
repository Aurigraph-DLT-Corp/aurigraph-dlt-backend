package io.aurigraph.v11.contracts.composite;

import io.aurigraph.v11.contracts.rwa.*;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import io.quarkus.logging.Log;
import org.bouncycastle.crypto.digests.SHA3Digest;
import org.bouncycastle.util.encoders.Hex;

/**
 * Composite Token Factory for Real World Asset Tokenization
 * Creates comprehensive asset packages with primary + secondary tokens
 * Implements ERC-721 (primary) + ERC-1155 (secondary) token architecture
 */
@ApplicationScoped
public class CompositeTokenFactory {

    @Inject
    RWATokenizer rwaTokenizer;
    
    @Inject
    AssetValuationService valuationService;
    
    @Inject
    DigitalTwinService digitalTwinService;
    
    @Inject
    VerifierRegistry verifierRegistry;

    // Composite token registry
    private final Map<String, CompositeToken> compositeTokens = new ConcurrentHashMap<>();
    private final Map<String, List<SecondaryToken>> secondaryTokens = new ConcurrentHashMap<>();
    private final AtomicLong compositeTokenCounter = new AtomicLong(0);
    
    // Performance metrics
    private final AtomicLong totalCompositeTokensCreated = new AtomicLong(0);
    private final Map<String, AtomicLong> tokensByAssetType = new ConcurrentHashMap<>();

    /**
     * Create a complete composite token package for a real-world asset
     */
    public Uni<CompositeTokenResult> createCompositeToken(CompositeTokenCreationRequest request) {
        return Uni.createFrom().item(() -> {
            long startTime = System.nanoTime();
            String compositeId = generateCompositeTokenId(request);
            
            Log.infof("Creating composite token %s for asset %s of type %s", 
                compositeId, request.getAssetId(), request.getAssetType());
            
            // Step 1: Create primary asset token (ERC-721)
            PrimaryToken primaryToken = createPrimaryToken(compositeId, request);
            
            // Step 2: Create secondary tokens (ERC-1155)
            List<SecondaryToken> secondaryTokenList = createSecondaryTokens(compositeId, request);
            
            // Step 3: Create composite package
            CompositeToken compositeToken = CompositeToken.builder()
                .compositeId(compositeId)
                .assetId(request.getAssetId())
                .assetType(request.getAssetType())
                .primaryToken(primaryToken)
                .secondaryTokens(secondaryTokenList)
                .ownerAddress(request.getOwnerAddress())
                .createdAt(Instant.now())
                .status(CompositeTokenStatus.PENDING_VERIFICATION)
                .verificationLevel(VerificationLevel.NONE)
                .build();
            
            // Step 4: Store in registries
            compositeTokens.put(compositeId, compositeToken);
            secondaryTokens.put(compositeId, secondaryTokenList);
            
            // Step 5: Initialize verification process
            initiateVerificationProcess(compositeToken, request);
            
            // Step 6: Update metrics
            totalCompositeTokensCreated.incrementAndGet();
            tokensByAssetType.computeIfAbsent(request.getAssetType(), k -> new AtomicLong(0)).incrementAndGet();
            
            long endTime = System.nanoTime();
            
            CompositeTokenResult result = new CompositeTokenResult(
                compositeToken, true, "Composite token created successfully", 
                endTime - startTime
            );
            
            Log.infof("Successfully created composite token %s in %d ns", 
                compositeId, endTime - startTime);
            
            return result;
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    /**
     * Get composite token by ID
     */
    public Uni<CompositeToken> getCompositeToken(String compositeId) {
        return Uni.createFrom().item(() -> compositeTokens.get(compositeId))
            .runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    /**
     * Update secondary token in composite package
     */
    public Uni<Boolean> updateSecondaryToken(String compositeId, SecondaryTokenType tokenType, 
                                           Map<String, Object> updateData) {
        return Uni.createFrom().item(() -> {
            CompositeToken composite = compositeTokens.get(compositeId);
            if (composite == null) {
                return false;
            }
            
            List<SecondaryToken> tokens = secondaryTokens.get(compositeId);
            if (tokens == null) {
                return false;
            }
            
            // Find and update the specific secondary token
            for (SecondaryToken token : tokens) {
                if (token.getTokenType() == tokenType) {
                    token.updateData(updateData);
                    token.setLastUpdated(Instant.now());
                    
                    Log.infof("Updated secondary token %s for composite %s", 
                        tokenType, compositeId);
                    return true;
                }
            }
            
            return false;
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    /**
     * Add verification result to composite token
     */
    public Uni<Boolean> addVerificationResult(String compositeId, VerificationResult result) {
        return Uni.createFrom().item(() -> {
            CompositeToken composite = compositeTokens.get(compositeId);
            if (composite == null) {
                return false;
            }
            
            // Add verification to the verification token
            List<SecondaryToken> tokens = secondaryTokens.get(compositeId);
            for (SecondaryToken token : tokens) {
                if (token.getTokenType() == SecondaryTokenType.VERIFICATION) {
                    VerificationToken verificationToken = (VerificationToken) token;
                    verificationToken.addVerificationResult(result);
                    
                    // Check if we have reached consensus
                    if (verificationToken.hasConsensus()) {
                        composite.setStatus(CompositeTokenStatus.VERIFIED);
                        composite.setVerificationLevel(result.getVerificationLevel());
                        
                        Log.infof("Composite token %s verified with consensus", compositeId);
                    }
                    
                    return true;
                }
            }
            
            return false;
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    /**
     * Transfer ownership of entire composite token package
     */
    public Uni<Boolean> transferCompositeToken(String compositeId, String fromAddress, 
                                             String toAddress) {
        return Uni.createFrom().item(() -> {
            CompositeToken composite = compositeTokens.get(compositeId);
            if (composite == null) {
                return false;
            }
            
            // Verify current ownership
            if (!fromAddress.equals(composite.getOwnerAddress())) {
                throw new UnauthorizedTransferException("Unauthorized transfer attempt");
            }
            
            // Ensure token is verified before transfer
            if (composite.getStatus() != CompositeTokenStatus.VERIFIED) {
                throw new IllegalStateException("Cannot transfer unverified composite token");
            }
            
            // Update ownership in all tokens
            composite.setOwnerAddress(toAddress);
            composite.getPrimaryToken().setOwnerAddress(toAddress);
            
            // Update owner token specifically
            List<SecondaryToken> tokens = secondaryTokens.get(compositeId);
            for (SecondaryToken token : tokens) {
                if (token.getTokenType() == SecondaryTokenType.OWNER) {
                    OwnerToken ownerToken = (OwnerToken) token;
                    ownerToken.recordTransfer(fromAddress, toAddress);
                }
            }
            
            Log.infof("Transferred composite token %s from %s to %s", 
                compositeId, fromAddress, toAddress);
            
            return true;
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    /**
     * Get all secondary tokens for a composite token
     */
    public Uni<List<SecondaryToken>> getSecondaryTokens(String compositeId) {
        return Uni.createFrom().item(() -> 
            secondaryTokens.getOrDefault(compositeId, new ArrayList<>())
        ).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    /**
     * Get specific secondary token by type
     */
    public Uni<SecondaryToken> getSecondaryToken(String compositeId, SecondaryTokenType tokenType) {
        return Uni.createFrom().item(() -> {
            List<SecondaryToken> tokens = secondaryTokens.get(compositeId);
            if (tokens == null) {
                return null;
            }
            
            return tokens.stream()
                .filter(token -> token.getTokenType() == tokenType)
                .findFirst()
                .orElse(null);
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    /**
     * Get composite tokens by asset type
     */
    public Uni<List<CompositeToken>> getCompositeTokensByType(String assetType) {
        return Uni.createFrom().item(() -> {
            return compositeTokens.values().stream()
                .filter(token -> assetType.equals(token.getAssetType()))
                .toList();
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    /**
     * Get composite tokens by owner
     */
    public Uni<List<CompositeToken>> getCompositeTokensByOwner(String ownerAddress) {
        return Uni.createFrom().item(() -> {
            return compositeTokens.values().stream()
                .filter(token -> ownerAddress.equals(token.getOwnerAddress()))
                .toList();
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    /**
     * Get factory statistics
     */
    public Uni<CompositeTokenStats> getStats() {
        return Uni.createFrom().item(() -> {
            Map<String, Long> typeDistribution = new HashMap<>();
            Map<CompositeTokenStatus, Long> statusDistribution = new HashMap<>();
            
            for (CompositeToken token : compositeTokens.values()) {
                // Count by asset type
                String type = token.getAssetType();
                typeDistribution.merge(type, 1L, Long::sum);
                
                // Count by status
                CompositeTokenStatus status = token.getStatus();
                statusDistribution.merge(status, 1L, Long::sum);
            }
            
            return new CompositeTokenStats(
                compositeTokens.size(),
                typeDistribution,
                statusDistribution,
                totalCompositeTokensCreated.get(),
                calculateTotalValue()
            );
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    // Private helper methods

    private String generateCompositeTokenId(CompositeTokenCreationRequest request) {
        SHA3Digest digest = new SHA3Digest(256);
        String input = request.getAssetId() + request.getAssetType() + 
                      System.nanoTime() + compositeTokenCounter.incrementAndGet();
        byte[] inputBytes = input.getBytes();
        digest.update(inputBytes, 0, inputBytes.length);
        byte[] hash = new byte[32];
        digest.doFinal(hash, 0);
        return "wAUR-COMPOSITE-" + Hex.toHexString(hash).substring(0, 16).toUpperCase();
    }

    private PrimaryToken createPrimaryToken(String compositeId, CompositeTokenCreationRequest request) {
        String primaryTokenId = compositeId.replace("COMPOSITE", "ASSET");
        
        return PrimaryToken.builder()
            .tokenId(primaryTokenId)
            .compositeId(compositeId)
            .assetId(request.getAssetId())
            .assetType(request.getAssetType())
            .ownerAddress(request.getOwnerAddress())
            .legalTitle(request.getLegalTitle())
            .jurisdiction(request.getJurisdiction())
            .coordinates(request.getCoordinates())
            .fractionalizable(request.isFractionalizable())
            .createdAt(Instant.now())
            .build();
    }

    private List<SecondaryToken> createSecondaryTokens(String compositeId, CompositeTokenCreationRequest request) {
        List<SecondaryToken> tokens = new ArrayList<>();
        
        // 1. Owner Token (ERC-721)
        OwnerToken ownerToken = new OwnerToken(
            compositeId.replace("COMPOSITE", "OWNER"),
            compositeId,
            request.getOwnerAddress(),
            BigDecimal.valueOf(100), // 100% ownership initially
            new ArrayList<>() // Empty transfer history initially
        );
        tokens.add(ownerToken);
        
        // 2. Collateral Token (ERC-1155)
        CollateralToken collateralToken = new CollateralToken(
            compositeId.replace("COMPOSITE", "COLL"),
            compositeId,
            new ArrayList<>() // Empty collateral list initially
        );
        tokens.add(collateralToken);
        
        // 3. Media Token (ERC-1155)
        MediaToken mediaToken = new MediaToken(
            compositeId.replace("COMPOSITE", "MEDIA"),
            compositeId,
            new ArrayList<>() // Empty media list initially
        );
        tokens.add(mediaToken);
        
        // 4. Verification Token (ERC-721)
        VerificationToken verificationToken = new VerificationToken(
            compositeId.replace("COMPOSITE", "VERIFY"),
            compositeId,
            request.getRequiredVerificationLevel(),
            new ArrayList<>() // Empty verification results initially
        );
        tokens.add(verificationToken);
        
        // 5. Valuation Token (ERC-20)
        ValuationToken valuationToken = new ValuationToken(
            compositeId.replace("COMPOSITE", "VALUE"),
            compositeId,
            BigDecimal.ZERO, // Will be updated by valuation service
            new ArrayList<>() // Empty price history initially
        );
        tokens.add(valuationToken);
        
        // 6. Compliance Token (ERC-721)
        ComplianceToken complianceToken = new ComplianceToken(
            compositeId.replace("COMPOSITE", "COMPLY"),
            compositeId,
            ComplianceStatus.PENDING,
            new HashMap<>() // Empty compliance data initially
        );
        tokens.add(complianceToken);
        
        return tokens;
    }

    private void initiateVerificationProcess(CompositeToken compositeToken, CompositeTokenCreationRequest request) {
        // Request verification from appropriate tier verifiers
        VerificationLevel requiredLevel = request.getRequiredVerificationLevel();
        
        verifierRegistry.requestVerification(
            compositeToken.getCompositeId(),
            compositeToken.getAssetType(),
            requiredLevel,
            3 // Number of verifiers required for consensus
        );
        
        Log.infof("Initiated %s verification process for composite token %s", 
            requiredLevel, compositeToken.getCompositeId());
    }

    private BigDecimal calculateTotalValue() {
        return compositeTokens.values().stream()
            .map(token -> {
                // Get valuation from valuation token
                List<SecondaryToken> tokens = secondaryTokens.get(token.getCompositeId());
                if (tokens != null) {
                    for (SecondaryToken secondaryToken : tokens) {
                        if (secondaryToken.getTokenType() == SecondaryTokenType.VALUATION) {
                            ValuationToken valuationToken = (ValuationToken) secondaryToken;
                            return valuationToken.getCurrentValue();
                        }
                    }
                }
                return BigDecimal.ZERO;
            })
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // Exception classes
    public static class UnauthorizedTransferException extends RuntimeException {
        public UnauthorizedTransferException(String message) { super(message); }
    }
    
    public static class VerificationRequiredException extends RuntimeException {
        public VerificationRequiredException(String message) { super(message); }
    }
}