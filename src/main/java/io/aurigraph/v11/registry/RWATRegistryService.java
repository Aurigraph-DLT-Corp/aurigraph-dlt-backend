package io.aurigraph.v11.registry;

import io.aurigraph.v11.merkle.MerkleProof;
import io.aurigraph.v11.merkle.MerkleTreeRegistry;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * RWAT Registry Service with Merkle Tree Support
 *
 * Backend service for Real-World Asset Tokens registry.
 * Provides searchability, discoverability, analytics, and cryptographic verification.
 *
 * Note: REST endpoints are exposed via RegistryResource, not directly by this service.
 *
 * @version 11.4.1
 * @since 2025-10-13
 */
@ApplicationScoped
public class RWATRegistryService extends MerkleTreeRegistry<RWATRegistry> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RWATRegistryService.class);

    @Override
    protected String serializeValue(RWATRegistry rwat) {
        return String.format("%s|%s|%s|%d|%s|%.2f|%s",
            rwat.getRwatId(),
            rwat.getAssetName(),
            rwat.getAssetType(),
            rwat.getTokenSupply(),
            rwat.getVerificationStatus(),
            rwat.getTotalValue(),
            rwat.getOwner()
        );
    }

    /**
     * Register a new RWAT
     */
    public Uni<RWATRegistry> registerRWAT(RWATRegistry rwat) {
        return Uni.createFrom().item(() -> {
            LOGGER.info("Registering RWAT: {}", rwat.getAssetName());

            // Generate RWAT ID
            rwat.setRwatId("RWAT-" + UUID.randomUUID().toString());
            rwat.setListedAt(Instant.now());
            rwat.setActive(true);

            // Calculate completeness score
            rwat.setCompletenessScore(calculateCompletenessScore(rwat));

            return rwat;
        }).flatMap(r -> add(r.getRwatId(), r).map(success -> r));
    }

    /**
     * Get RWAT by ID
     */
    public Uni<RWATRegistry> getRWAT(String rwatId) {
        return get(rwatId).onItem().ifNull().failWith(() ->
            new RWATNotFoundException("RWAT not found: " + rwatId));
    }

    /**
     * Generate Merkle proof for an RWAT
     */
    public Uni<MerkleProof.ProofData> getProof(String rwatId) {
        return generateProof(rwatId).map(MerkleProof::toProofData);
    }

    /**
     * Verify a Merkle proof
     */
    public Uni<VerificationResponse> verifyMerkleProof(MerkleProof.ProofData proofData) {
        return verifyProof(proofData.toMerkleProof()).map(valid ->
            new VerificationResponse(valid, valid ? "Proof verified successfully" : "Invalid proof")
        );
    }

    /**
     * Get current Merkle root hash
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
     * Search RWATs by keyword
     */
    public Uni<List<RWATRegistry>> searchRWATs(String keyword) {
        return getAll().map(rwats ->
            rwats.stream()
                .filter(r -> matchesKeyword(r, keyword))
                .collect(Collectors.toList())
        );
    }

    /**
     * List RWATs by asset type
     */
    public Uni<List<RWATRegistry>> listByAssetType(RWATRegistry.AssetType assetType) {
        return getAll().map(rwats ->
            rwats.stream()
                .filter(r -> r.getAssetType() == assetType)
                .collect(Collectors.toList())
        );
    }

    /**
     * List verified RWATs
     */
    public Uni<List<RWATRegistry>> listVerifiedRWATs() {
        return getAll().map(rwats ->
            rwats.stream()
                .filter(r -> r.getVerificationStatus() == RWATRegistry.VerificationStatus.VERIFIED)
                .collect(Collectors.toList())
        );
    }

    /**
     * List RWATs by location
     */
    public Uni<List<RWATRegistry>> listByLocation(String location) {
        return getAll().map(rwats ->
            rwats.stream()
                .filter(r -> r.getLocation() != null && r.getLocation().contains(location))
                .collect(Collectors.toList())
        );
    }

    /**
     * List recently listed RWATs
     */
    public Uni<List<RWATRegistry>> listRecentRWATs(int limit) {
        return getAll().map(rwats ->
            rwats.stream()
                .sorted((a, b) -> b.getListedAt().compareTo(a.getListedAt()))
                .limit(limit)
                .collect(Collectors.toList())
        );
    }

    /**
     * List top RWATs by trading volume
     */
    public Uni<List<RWATRegistry>> listTopByVolume(int limit) {
        return getAll().map(rwats ->
            rwats.stream()
                .sorted((a, b) -> Double.compare(b.getTradingVolume(), a.getTradingVolume()))
                .limit(limit)
                .collect(Collectors.toList())
        );
    }

    /**
     * Update RWAT verification status
     */
    public Uni<RWATRegistry> updateVerificationStatus(
            String rwatId,
            RWATRegistry.VerificationStatus status,
            String verifierId
    ) {
        return getRWAT(rwatId).map(rwat -> {
            rwat.setVerificationStatus(status);
            rwat.setVerifiedBy(verifierId);
            rwat.setVerifiedAt(Instant.now());
            registry.put(rwatId, rwat);
            LOGGER.info("RWAT verification updated: {} - {}", rwatId, status);
            return rwat;
        });
    }

    /**
     * Record trading activity
     */
    public Uni<RWATRegistry> recordTransaction(String rwatId, double transactionValue) {
        return getRWAT(rwatId).map(rwat -> {
            rwat.setTradingVolume(rwat.getTradingVolume() + transactionValue);
            rwat.setTransactionCount(rwat.getTransactionCount() + 1);
            registry.put(rwatId, rwat);
            return rwat;
        });
    }

    /**
     * Get registry statistics
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();

        stats.put("totalRWATs", registry.size());
        stats.put("activeRWATs", registry.values().stream().filter(RWATRegistry::isActive).count());
        stats.put("verifiedRWATs", registry.values().stream()
                .filter(r -> r.getVerificationStatus() == RWATRegistry.VerificationStatus.VERIFIED).count());

        // Total value locked (TVL)
        double tvl = registry.values().stream()
                .mapToDouble(RWATRegistry::getTotalValue)
                .sum();
        stats.put("totalValueLocked", tvl);

        // Total trading volume
        double totalVolume = registry.values().stream()
                .mapToDouble(RWATRegistry::getTradingVolume)
                .sum();
        stats.put("totalTradingVolume", totalVolume);

        // Count by asset type
        Map<String, Long> byType = registry.values().stream()
                .collect(Collectors.groupingBy(
                        r -> r.getAssetType().name(),
                        Collectors.counting()
                ));
        stats.put("assetsByType", byType);

        // Average completeness score
        double avgCompleteness = registry.values().stream()
                .mapToDouble(RWATRegistry::getCompletenessScore)
                .average()
                .orElse(0.0);
        stats.put("averageCompletenessScore", avgCompleteness);

        return stats;
    }

    // Helper methods
    private boolean matchesKeyword(RWATRegistry rwat, String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return true;
        }

        String lowerKeyword = keyword.toLowerCase();

        return (rwat.getAssetName() != null && rwat.getAssetName().toLowerCase().contains(lowerKeyword)) ||
                (rwat.getTokenSymbol() != null && rwat.getTokenSymbol().toLowerCase().contains(lowerKeyword)) ||
                (rwat.getLocation() != null && rwat.getLocation().toLowerCase().contains(lowerKeyword)) ||
                (rwat.getAssetCategory() != null && rwat.getAssetCategory().toLowerCase().contains(lowerKeyword));
    }

    private double calculateCompletenessScore(RWATRegistry rwat) {
        double score = 0.0;

        // Basic information (30%)
        if (rwat.getAssetName() != null && !rwat.getAssetName().isEmpty()) score += 0.1;
        if (rwat.getAssetType() != null) score += 0.1;
        if (rwat.getLocation() != null && !rwat.getLocation().isEmpty()) score += 0.1;

        // Documentation (40%)
        if (rwat.getDocumentCount() > 0) score += 0.2;
        if (rwat.getDocumentCount() >= 3) score += 0.1;
        if (rwat.getDocumentCount() >= 5) score += 0.1;

        // Media (30%)
        if (rwat.getPhotoCount() > 0) score += 0.15;
        if (rwat.getVideoCount() > 0) score += 0.15;

        return Math.min(score, 1.0);
    }

    // Custom Exception
    public static class RWATNotFoundException extends RuntimeException {
        public RWATNotFoundException(String message) {
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
