package io.aurigraph.v11.bridge;

import io.aurigraph.v11.merkle.MerkleProof;
import io.aurigraph.v11.merkle.MerkleTreeRegistry;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Bridge Token Registry Service with Merkle Tree Support
 *
 * Backend service for cross-chain bridge token registry with cryptographic verification.
 * Manages token mappings across different blockchain networks.
 *
 * Features:
 * - Cross-chain token mapping
 * - Merkle tree cryptographic verification
 * - Proof generation and verification
 * - Root hash tracking for cross-chain validation
 * - Bridge transaction tracking
 *
 * @version 11.5.0
 * @since 2025-10-25 - AV11-456: BridgeTokenRegistry Merkle Tree
 */
@ApplicationScoped
public class BridgeTokenRegistryService extends MerkleTreeRegistry<BridgeToken> {

    private static final Logger LOGGER = LoggerFactory.getLogger(BridgeTokenRegistryService.class);

    @Override
    protected String serializeValue(BridgeToken token) {
        return String.format("%s|%s|%s|%s|%s|%s|%s|%s",
            token.getBridgeTokenId(),
            token.getSourceChain(),
            token.getSourceTokenAddress(),
            token.getDestinationChain(),
            token.getDestinationTokenAddress(),
            token.getTotalBridged(),
            token.getTokenSymbol(),
            token.getBridgeStatus()
        );
    }

    /**
     * Register a new bridge token mapping
     */
    public Uni<BridgeToken> registerBridgeToken(BridgeToken token) {
        return Uni.createFrom().item(() -> {
            LOGGER.info("Registering bridge token: {} -> {}",
                token.getSourceChain(), token.getDestinationChain());

            // Generate bridge token ID
            token.setBridgeTokenId("BRIDGE-" + UUID.randomUUID().toString());
            token.setCreatedAt(Instant.now());
            token.setBridgeStatus(BridgeStatus.ACTIVE);

            return token;
        }).flatMap(t -> add(t.getBridgeTokenId(), t).map(success -> t));
    }

    /**
     * Get bridge token by ID
     */
    public Uni<BridgeToken> getBridgeToken(String bridgeTokenId) {
        return get(bridgeTokenId).onItem().ifNull().failWith(() ->
            new BridgeTokenNotFoundException("Bridge token not found: " + bridgeTokenId));
    }

    /**
     * Get bridge tokens by source chain
     */
    public Uni<List<BridgeToken>> getBridgeTokensBySourceChain(String chainId) {
        return getAll().map(tokens ->
            tokens.stream()
                .filter(t -> t.getSourceChain().equals(chainId))
                .collect(Collectors.toList())
        );
    }

    /**
     * Get bridge tokens by destination chain
     */
    public Uni<List<BridgeToken>> getBridgeTokensByDestinationChain(String chainId) {
        return getAll().map(tokens ->
            tokens.stream()
                .filter(t -> t.getDestinationChain().equals(chainId))
                .collect(Collectors.toList())
        );
    }

    /**
     * Get bridge token by source token address
     */
    public Uni<BridgeToken> getBridgeTokenBySourceAddress(String sourceChain, String sourceAddress) {
        return getAll().map(tokens ->
            tokens.stream()
                .filter(t -> t.getSourceChain().equals(sourceChain) &&
                            t.getSourceTokenAddress().equals(sourceAddress))
                .findFirst()
                .orElse(null)
        );
    }

    /**
     * Update bridge token mapping
     */
    public Uni<BridgeToken> updateBridgeToken(String bridgeTokenId, BridgeToken updatedToken) {
        return getBridgeToken(bridgeTokenId).map(existing -> {
            updatedToken.setUpdatedAt(Instant.now());
            registry.put(bridgeTokenId, updatedToken);
            LOGGER.info("Bridge token updated: {}", bridgeTokenId);
            return updatedToken;
        }).flatMap(t -> add(bridgeTokenId, t).map(success -> t));
    }

    /**
     * Record bridge transaction
     */
    public Uni<BridgeToken> recordBridgeTransaction(String bridgeTokenId, BigDecimal amount, String txHash) {
        return getBridgeToken(bridgeTokenId).map(token -> {
            token.setTotalBridged(token.getTotalBridged().add(amount));
            token.setBridgeTransactionCount(token.getBridgeTransactionCount() + 1);
            token.setLastBridgeAt(Instant.now());
            registry.put(bridgeTokenId, token);
            LOGGER.info("Bridge transaction recorded: {} - {} units", bridgeTokenId, amount);
            return token;
        });
    }

    /**
     * Update bridge status
     */
    public Uni<BridgeToken> updateBridgeStatus(String bridgeTokenId, BridgeStatus status) {
        return getBridgeToken(bridgeTokenId).map(token -> {
            token.setBridgeStatus(status);
            token.setUpdatedAt(Instant.now());
            registry.put(bridgeTokenId, token);
            LOGGER.info("Bridge status updated: {} - {}", bridgeTokenId, status);
            return token;
        });
    }

    /**
     * Generate Merkle proof for a bridge token
     */
    public Uni<MerkleProof.ProofData> getProof(String bridgeTokenId) {
        return generateProof(bridgeTokenId).map(MerkleProof::toProofData);
    }

    /**
     * Verify a Merkle proof for cross-chain validation
     */
    public Uni<VerificationResponse> verifyMerkleProof(MerkleProof.ProofData proofData) {
        return verifyProof(proofData.toMerkleProof()).map(valid ->
            new VerificationResponse(valid, valid ? "Cross-chain proof verified successfully" : "Invalid cross-chain proof")
        );
    }

    /**
     * Get current Merkle root hash for cross-chain sync
     */
    public Uni<RootHashResponse> getMerkleRootHash() {
        return getRootHash().flatMap(rootHash ->
            getTreeStats().map(stats -> new RootHashResponse(
                rootHash,
                Instant.now(),
                stats.getEntryCount(),
                stats.getTreeHeight()
            ))
        );
    }

    /**
     * Get Merkle tree statistics
     */
    public Uni<MerkleTreeStats> getMerkleTreeStats() {
        return getTreeStats();
    }

    /**
     * Get bridge statistics
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();

        stats.put("totalBridgeTokens", registry.size());
        stats.put("activeBridges", registry.values().stream()
                .filter(t -> t.getBridgeStatus() == BridgeStatus.ACTIVE).count());

        // Total bridged volume
        BigDecimal totalBridged = registry.values().stream()
                .map(BridgeToken::getTotalBridged)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        stats.put("totalBridgedVolume", totalBridged);

        // Total bridge transactions
        long totalTransactions = registry.values().stream()
                .mapToLong(BridgeToken::getBridgeTransactionCount)
                .sum();
        stats.put("totalBridgeTransactions", totalTransactions);

        // Count by source chain
        Map<String, Long> bySourceChain = registry.values().stream()
                .collect(Collectors.groupingBy(
                        BridgeToken::getSourceChain,
                        Collectors.counting()
                ));
        stats.put("bridgesBySourceChain", bySourceChain);

        // Count by destination chain
        Map<String, Long> byDestChain = registry.values().stream()
                .collect(Collectors.groupingBy(
                        BridgeToken::getDestinationChain,
                        Collectors.counting()
                ));
        stats.put("bridgesByDestinationChain", byDestChain);

        return stats;
    }

    // Custom Exception
    public static class BridgeTokenNotFoundException extends RuntimeException {
        public BridgeTokenNotFoundException(String message) {
            super(message);
        }
    }

    // Response Classes
    public static class VerificationResponse {
        private final boolean valid;
        private final String message;

        public VerificationResponse(boolean valid, String message) {
            this.valid = valid;
            this.message = message;
        }

        public boolean isValid() {
            return valid;
        }

        public String getMessage() {
            return message;
        }
    }

    public static class RootHashResponse {
        private final String rootHash;
        private final Instant timestamp;
        private final int entryCount;
        private final int treeHeight;

        public RootHashResponse(String rootHash, Instant timestamp, int entryCount, int treeHeight) {
            this.rootHash = rootHash;
            this.timestamp = timestamp;
            this.entryCount = entryCount;
            this.treeHeight = treeHeight;
        }

        public String getRootHash() {
            return rootHash;
        }

        public Instant getTimestamp() {
            return timestamp;
        }

        public int getEntryCount() {
            return entryCount;
        }

        public int getTreeHeight() {
            return treeHeight;
        }
    }
}

/**
 * Bridge Token Model
 */
class BridgeToken {
    private String bridgeTokenId;
    private String sourceChain;
    private String sourceTokenAddress;
    private String destinationChain;
    private String destinationTokenAddress;
    private String tokenSymbol;
    private String tokenName;
    private BigDecimal totalBridged = BigDecimal.ZERO;
    private long bridgeTransactionCount = 0;
    private BridgeStatus bridgeStatus;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant lastBridgeAt;

    // Getters and Setters
    public String getBridgeTokenId() { return bridgeTokenId; }
    public void setBridgeTokenId(String bridgeTokenId) { this.bridgeTokenId = bridgeTokenId; }

    public String getSourceChain() { return sourceChain; }
    public void setSourceChain(String sourceChain) { this.sourceChain = sourceChain; }

    public String getSourceTokenAddress() { return sourceTokenAddress; }
    public void setSourceTokenAddress(String sourceTokenAddress) { this.sourceTokenAddress = sourceTokenAddress; }

    public String getDestinationChain() { return destinationChain; }
    public void setDestinationChain(String destinationChain) { this.destinationChain = destinationChain; }

    public String getDestinationTokenAddress() { return destinationTokenAddress; }
    public void setDestinationTokenAddress(String destinationTokenAddress) { this.destinationTokenAddress = destinationTokenAddress; }

    public String getTokenSymbol() { return tokenSymbol; }
    public void setTokenSymbol(String tokenSymbol) { this.tokenSymbol = tokenSymbol; }

    public String getTokenName() { return tokenName; }
    public void setTokenName(String tokenName) { this.tokenName = tokenName; }

    public BigDecimal getTotalBridged() { return totalBridged; }
    public void setTotalBridged(BigDecimal totalBridged) { this.totalBridged = totalBridged; }

    public long getBridgeTransactionCount() { return bridgeTransactionCount; }
    public void setBridgeTransactionCount(long bridgeTransactionCount) { this.bridgeTransactionCount = bridgeTransactionCount; }

    public BridgeStatus getBridgeStatus() { return bridgeStatus; }
    public void setBridgeStatus(BridgeStatus bridgeStatus) { this.bridgeStatus = bridgeStatus; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public Instant getLastBridgeAt() { return lastBridgeAt; }
    public void setLastBridgeAt(Instant lastBridgeAt) { this.lastBridgeAt = lastBridgeAt; }
}

/**
 * Bridge Status Enumeration
 */
enum BridgeStatus {
    ACTIVE,
    PAUSED,
    DEPRECATED,
    DISABLED
}
