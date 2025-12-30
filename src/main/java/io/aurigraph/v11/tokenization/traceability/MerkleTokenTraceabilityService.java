package io.aurigraph.v11.tokenization.traceability;

import io.aurigraph.v11.registry.RWATRegistry;
import io.aurigraph.v11.registry.RWATRegistryService;
import io.aurigraph.v11.merkle.MerkleProof;
import io.aurigraph.v11.models.TokenRegistry;
import io.quarkus.logging.Log;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * MerkleTokenTraceabilityService - Links tokens to underlying real-world assets
 * through merkle tree registry cryptographic proofs.
 *
 * Provides comprehensive token traceability with:
 * - Asset-to-token linkage verification
 * - Merkle proof validation
 * - Ownership history tracking
 * - Compliance audit trails
 * - Real-time proof verification
 *
 * @author Aurigraph V12 Token Traceability Team
 * @version 1.0.0
 */
@ApplicationScoped
public class MerkleTokenTraceabilityService {

    @Inject
    RWATRegistryService rWATRegistryService;

    // In-memory cache of token traces (production would use LevelDB)
    private final Map<String, MerkleTokenTrace> tokenTraceCache = new ConcurrentHashMap<>();

    /**
     * Create a new token trace linking a token to its underlying asset
     *
     * @param tokenId - Unique token identifier
     * @param assetId - Real-world asset ID
     * @param assetType - Type of asset (REAL_ESTATE, CARBON_CREDIT, etc.)
     * @param ownerAddress - Current token owner address
     * @return Uni with the created trace
     */
    public Uni<MerkleTokenTrace> createTokenTrace(
            String tokenId,
            String assetId,
            String assetType,
            String ownerAddress) {

        return Uni.createFrom().item(() -> {
            Log.info("Creating token trace: " + tokenId + " -> " + assetId);

            MerkleTokenTrace trace = new MerkleTokenTrace(tokenId, assetId, assetType);
            trace.setOwnerAddress(ownerAddress);
            trace.setTokenCreationTimestamp(LocalDateTime.now());

            // Generate audit trail entry
            MerkleTokenTrace.AuditLogEntry entry = new MerkleTokenTrace.AuditLogEntry(
                    "CREATED",
                    "system",
                    "Token trace created for asset: " + assetId
            );
            trace.addAuditEntry(entry);

            tokenTraceCache.put(tokenId, trace);
            return trace;
        });
    }

    /**
     * Link a token to an underlying asset via merkle tree verification
     *
     * @param tokenId - Token ID to link
     * @param rwatId - Real-World Asset Token ID in merkle registry
     * @return Uni with verification result
     */
    public Uni<MerkleTokenTrace> linkTokenToAsset(String tokenId, String rwatId) {
        return Uni.createFrom().item(() -> {
            Log.info("Linking token " + tokenId + " to asset " + rwatId);

            MerkleTokenTrace trace = tokenTraceCache.get(tokenId);
            if (trace == null) {
                throw new RuntimeException("Token trace not found: " + tokenId);
            }

            // In production, this would call rWATRegistryService.getRWAT(rwatId)
            // For demonstration, we'll simulate the linking
            trace.setAssetId(rwatId);
            trace.setVerificationStatus("IN_REVIEW");

            // Add audit entry
            MerkleTokenTrace.AuditLogEntry entry = new MerkleTokenTrace.AuditLogEntry(
                    "LINKED_TO_ASSET",
                    "system",
                    "Token linked to RWAT asset: " + rwatId
            );
            trace.addAuditEntry(entry);

            tokenTraceCache.put(tokenId, trace);
            return trace;
        });
    }

    /**
     * Verify a token's underlying asset through merkle proof validation
     *
     * @param tokenId - Token to verify
     * @return Uni with verification result
     */
    public Uni<MerkleTokenTrace> verifyTokenAssetProof(String tokenId) {
        return Uni.createFrom().item(() -> {
            Log.info("Verifying token asset proof: " + tokenId);

            MerkleTokenTrace trace = tokenTraceCache.get(tokenId);
            if (trace == null) {
                throw new RuntimeException("Token trace not found: " + tokenId);
            }

            // Simulate merkle proof generation and validation
            // In production, this calls rWATRegistryService.getProof(assetId)
            generateMerkleProofPath(trace);

            // Validate the proof
            boolean proofValid = validateMerkleProof(trace);
            trace.setProofValid(proofValid);

            if (proofValid) {
                trace.setVerificationStatus("VERIFIED");
                trace.setAssetVerified(true);
                trace.setLastVerifiedTimestamp(LocalDateTime.now());
                trace.setNextVerificationDue(LocalDateTime.now().plus(90, ChronoUnit.DAYS));

                Log.info("✅ Token asset proof verified: " + tokenId);
            } else {
                trace.setVerificationStatus("REJECTED");
                trace.setAssetVerified(false);
                Log.warn("❌ Token asset proof validation failed: " + tokenId);
            }

            // Add audit entry
            MerkleTokenTrace.AuditLogEntry entry = new MerkleTokenTrace.AuditLogEntry(
                    "VERIFIED",
                    "system",
                    "Asset proof validation: " + (proofValid ? "SUCCESS" : "FAILED")
            );
            entry.setStatus(proofValid ? "SUCCESS" : "FAILED");
            trace.addAuditEntry(entry);

            tokenTraceCache.put(tokenId, trace);
            return trace;
        });
    }

    /**
     * Record an ownership transfer in the token trace
     *
     * @param tokenId - Token being transferred
     * @param fromAddress - Previous owner address
     * @param toAddress - New owner address
     * @param ownershipPercentage - Percentage of ownership transferred
     * @return Uni with updated trace
     */
    public Uni<MerkleTokenTrace> recordOwnershipTransfer(
            String tokenId,
            String fromAddress,
            String toAddress,
            Double ownershipPercentage) {

        return Uni.createFrom().item(() -> {
            Log.info("Recording ownership transfer: " + tokenId + " from " + fromAddress + " to " + toAddress);

            MerkleTokenTrace trace = tokenTraceCache.get(tokenId);
            if (trace == null) {
                throw new RuntimeException("Token trace not found: " + tokenId);
            }

            // Create ownership transfer record
            MerkleTokenTrace.OwnershipTransfer transfer = new MerkleTokenTrace.OwnershipTransfer(
                    fromAddress,
                    toAddress,
                    ownershipPercentage
            );
            transfer.setTransactionHash(generateTransactionHash());
            trace.addOwnershipTransfer(transfer);

            // Update current owner
            trace.setOwnerAddress(toAddress);
            trace.setFractionalOwnership(ownershipPercentage);

            // Add audit entry
            MerkleTokenTrace.AuditLogEntry entry = new MerkleTokenTrace.AuditLogEntry(
                    "TRANSFERRED",
                    toAddress,
                    "Ownership transferred from " + fromAddress + " (" + ownershipPercentage + "%)"
            );
            trace.addAuditEntry(entry);

            tokenTraceCache.put(tokenId, trace);
            return trace;
        });
    }

    /**
     * Get complete token trace with all audit history
     *
     * @param tokenId - Token to retrieve
     * @return Uni with complete trace
     */
    public Uni<MerkleTokenTrace> getTokenTrace(String tokenId) {
        return Uni.createFrom().item(() -> {
            MerkleTokenTrace trace = tokenTraceCache.get(tokenId);
            if (trace == null) {
                throw new RuntimeException("Token trace not found: " + tokenId);
            }
            return trace;
        });
    }

    /**
     * Query token traces by asset type
     *
     * @param assetType - Type of asset to query
     * @return Uni with list of traces
     */
    public Uni<List<MerkleTokenTrace>> getTracesByAssetType(String assetType) {
        return Uni.createFrom().item(() ->
            tokenTraceCache.values().stream()
                .filter(trace -> assetType.equals(trace.getAssetType()))
                .collect(Collectors.toList())
        );
    }

    /**
     * Query token traces by owner address
     *
     * @param ownerAddress - Owner address to query
     * @return Uni with list of traces
     */
    public Uni<List<MerkleTokenTrace>> getTracesByOwner(String ownerAddress) {
        return Uni.createFrom().item(() ->
            tokenTraceCache.values().stream()
                .filter(trace -> ownerAddress.equals(trace.getOwnerAddress()))
                .collect(Collectors.toList())
        );
    }

    /**
     * Query token traces by verification status
     *
     * @param status - Verification status to query
     * @return Uni with list of traces
     */
    public Uni<List<MerkleTokenTrace>> getTracesByVerificationStatus(String status) {
        return Uni.createFrom().item(() ->
            tokenTraceCache.values().stream()
                .filter(trace -> status.equals(trace.getVerificationStatus()))
                .collect(Collectors.toList())
        );
    }

    /**
     * Get compliance summary for a token
     *
     * @param tokenId - Token to check
     * @return Uni with compliance summary
     */
    public Uni<Map<String, Object>> getComplianceSummary(String tokenId) {
        return Uni.createFrom().item(() -> {
            MerkleTokenTrace trace = tokenTraceCache.get(tokenId);
            if (trace == null) {
                throw new RuntimeException("Token trace not found: " + tokenId);
            }

            Map<String, Object> summary = new HashMap<>();
            summary.put("token_id", tokenId);
            summary.put("asset_id", trace.getAssetId());
            summary.put("verification_status", trace.getVerificationStatus());
            summary.put("is_verified", trace.getAssetVerified());
            summary.put("proof_valid", trace.getProofValid());
            summary.put("last_verified", trace.getLastVerifiedTimestamp());
            summary.put("next_verification_due", trace.getNextVerificationDue());
            summary.put("compliance_certifications", trace.getComplianceCertifications());
            summary.put("total_transfers", trace.getOwnershipHistory().size());
            summary.put("audit_entries", trace.getAuditTrail().size());
            summary.put("requires_verification", isVerificationDue(trace));

            return summary;
        });
    }

    /**
     * Add compliance certification to token trace
     *
     * @param tokenId - Token to certify
     * @param certification - Certification string
     * @return Uni with updated trace
     */
    public Uni<MerkleTokenTrace> addComplianceCertification(String tokenId, String certification) {
        return Uni.createFrom().item(() -> {
            MerkleTokenTrace trace = tokenTraceCache.get(tokenId);
            if (trace == null) {
                throw new RuntimeException("Token trace not found: " + tokenId);
            }

            trace.addCertification(certification);

            // Add audit entry
            MerkleTokenTrace.AuditLogEntry entry = new MerkleTokenTrace.AuditLogEntry(
                    "CERTIFIED",
                    "compliance-system",
                    "Compliance certification added: " + certification
            );
            trace.addAuditEntry(entry);

            tokenTraceCache.put(tokenId, trace);
            return trace;
        });
    }

    /**
     * Generate merkle proof path for a token (simulated)
     */
    private void generateMerkleProofPath(MerkleTokenTrace trace) {
        // Simulate merkle proof generation
        List<MerkleTokenTrace.MerkleProofNode> proofPath = new ArrayList<>();

        // Layer 0: Asset hash
        trace.setUnderlyingAssetHash("0xa1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6");

        // Generate proof path (simulated)
        for (int i = 0; i < 4; i++) {
            MerkleTokenTrace.MerkleProofNode node = new MerkleTokenTrace.MerkleProofNode(
                    i,
                    "0x" + generateRandomHash().substring(0, 32),
                    "0x" + generateRandomHash().substring(0, 32),
                    i % 2 == 0 ? "LEFT" : "RIGHT"
            );
            proofPath.add(node);
        }

        // Root hash
        trace.setMerkleRootHash("0xf1e2d3c4b5a6978865544332211009f");
        trace.setMerkleProofPath(proofPath);

        Log.info("Generated merkle proof path with " + proofPath.size() + " nodes");
    }

    /**
     * Validate merkle proof path
     */
    private boolean validateMerkleProof(MerkleTokenTrace trace) {
        if (trace.getMerkleProofPath() == null || trace.getMerkleProofPath().isEmpty()) {
            return false;
        }

        // Simulate proof validation
        // In production, this would verify the merkle root using cryptographic hashing
        boolean isValid = trace.getMerkleProofPath().size() >= 2 &&
                trace.getMerkleRootHash() != null &&
                !trace.getMerkleRootHash().isEmpty();

        Log.info("Merkle proof validation: " + (isValid ? "✅ VALID" : "❌ INVALID"));
        return isValid;
    }

    /**
     * Check if verification is due for a token
     */
    private boolean isVerificationDue(MerkleTokenTrace trace) {
        if (trace.getNextVerificationDue() == null) {
            return true;
        }
        return LocalDateTime.now().isAfter(trace.getNextVerificationDue());
    }

    /**
     * Generate random hash (for simulation)
     */
    private String generateRandomHash() {
        StringBuilder hash = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < 64; i++) {
            hash.append(String.format("%x", random.nextInt(16)));
        }
        return hash.toString();
    }

    /**
     * Generate simulated transaction hash
     */
    private String generateTransactionHash() {
        return "0x" + generateRandomHash().substring(0, 64);
    }

    /**
     * Get all token traces
     */
    public Uni<List<MerkleTokenTrace>> getAllTraces() {
        return Uni.createFrom().item(() ->
            new ArrayList<>(tokenTraceCache.values())
        );
    }

    /**
     * Get trace statistics
     */
    public Uni<Map<String, Object>> getTraceStatistics() {
        return Uni.createFrom().item(() -> {
            Map<String, Object> stats = new HashMap<>();
            stats.put("total_traces", tokenTraceCache.size());
            stats.put("verified_traces", tokenTraceCache.values().stream()
                .filter(MerkleTokenTrace::getAssetVerified).count());
            stats.put("pending_verification", tokenTraceCache.values().stream()
                .filter(t -> !t.getAssetVerified()).count());

            long verifiedAssets = tokenTraceCache.values().stream()
                .filter(t -> "VERIFIED".equals(t.getVerificationStatus())).count();
            stats.put("verified_assets", verifiedAssets);

            long totalOwnershipTransfers = tokenTraceCache.values().stream()
                .mapToLong(t -> t.getOwnershipHistory().size()).sum();
            stats.put("total_ownership_transfers", totalOwnershipTransfers);

            long totalAuditEntries = tokenTraceCache.values().stream()
                .mapToLong(t -> t.getAuditTrail().size()).sum();
            stats.put("total_audit_entries", totalAuditEntries);

            return stats;
        });
    }
}
