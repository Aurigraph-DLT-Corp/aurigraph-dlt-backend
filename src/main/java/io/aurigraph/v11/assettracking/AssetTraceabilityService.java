package io.aurigraph.v11.assettracking;

import io.quarkus.logging.Log;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Asset Traceability Service
 *
 * Provides comprehensive service for tracking the complete lifecycle of tokenized assets
 * with full audit trails, ownership history, and compliance tracking.
 *
 * Features:
 * - In-memory storage using ConcurrentHashMap for high-performance access
 * - Complete ownership chain tracking with timestamp and hash verification
 * - Comprehensive audit trail for all operations
 * - Fast lookups via reverse indexes
 * - Thread-safe operations for concurrent access
 *
 * @version 1.0.0
 * @author Aurigraph V11 Development Team
 */
@ApplicationScoped
public class AssetTraceabilityService {

    // Primary storage for asset traces
    private final Map<String, AssetTrace> assetTraces = new ConcurrentHashMap<>();

    // Reverse indexes for fast lookups
    private final Map<String, Set<String>> assetTypeIndex = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> ownerIndex = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> assetIdIndex = new ConcurrentHashMap<>();

    /**
     * Create a new asset trace
     *
     * @param assetId Unique asset identifier
     * @param assetName Human-readable asset name
     * @param assetType Asset type classification
     * @param valuation Asset valuation in base currency
     * @param owner Current owner identifier
     * @return AssetTrace for the newly created asset
     */
    public Uni<AssetTrace> createAssetTrace(String assetId, String assetName, String assetType,
                                           Double valuation, String owner) {
        return Uni.createFrom().item(() -> {
            Log.infof("Creating asset trace: assetId=%s, assetType=%s, owner=%s, valuation=%.2f",
                assetId, assetType, owner, valuation);

            // Generate unique trace ID
            String traceId = "trace_" + UUID.randomUUID().toString();

            // Create asset trace
            AssetTrace trace = new AssetTrace();
            trace.setTraceId(traceId);
            trace.setAssetId(assetId);
            trace.setAssetName(assetName);
            trace.setAssetType(assetType);
            trace.setValuation(valuation);
            trace.setCurrencyCode("USD");
            trace.setCurrentOwner(owner);
            trace.setLastUpdated(Instant.now());
            trace.setComplianceStatus("PENDING_VERIFICATION");

            // Initialize ownership history with initial owner
            OwnershipRecord initialOwner = new OwnershipRecord();
            initialOwner.setOwner(owner);
            initialOwner.setAcquisitionDate(Instant.now());
            initialOwner.setPercentage(100.0);
            initialOwner.setTxHash(generateTransactionHash(assetId, owner, Instant.now()));

            List<OwnershipRecord> ownershipHistory = new ArrayList<>();
            ownershipHistory.add(initialOwner);
            trace.setOwnershipHistory(ownershipHistory);

            // Initialize audit trail
            AuditTrailEntry creationAudit = new AuditTrailEntry();
            creationAudit.setEntryId("audit_" + UUID.randomUUID().toString());
            creationAudit.setAction("CREATED");
            creationAudit.setActor(owner);
            creationAudit.setTimestamp(Instant.now());
            creationAudit.setDetails(Map.of(
                "assetId", assetId,
                "assetName", assetName,
                "assetType", assetType,
                "valuation", valuation
            ));
            creationAudit.setStatus("SUCCESS");

            List<AuditTrailEntry> auditTrail = new ArrayList<>();
            auditTrail.add(creationAudit);
            trace.setAuditTrail(auditTrail);

            // Initialize metadata
            Map<String, Object> metadata = new ConcurrentHashMap<>();
            metadata.put("createdAt", Instant.now());
            metadata.put("version", "1.0.0");
            metadata.put("region", "global");
            trace.setMetadata(metadata);

            // Store in primary storage
            assetTraces.put(traceId, trace);

            // Update reverse indexes
            assetTypeIndex.computeIfAbsent(assetType, k -> ConcurrentHashMap.newKeySet()).add(traceId);
            ownerIndex.computeIfAbsent(owner, k -> ConcurrentHashMap.newKeySet()).add(traceId);
            assetIdIndex.computeIfAbsent(assetId, k -> ConcurrentHashMap.newKeySet()).add(traceId);

            Log.infof("Asset trace created successfully: %s", traceId);
            return trace;
        });
    }

    /**
     * Get asset trace by ID
     *
     * @param traceId Trace identifier
     * @return Optional containing AssetTrace if found
     */
    public Uni<Optional<AssetTrace>> getAssetTrace(String traceId) {
        return Uni.createFrom().item(() -> {
            Log.debugf("Retrieving asset trace: %s", traceId);
            return Optional.ofNullable(assetTraces.get(traceId));
        });
    }

    /**
     * Search assets with multiple filters
     *
     * @param assetType Filter by asset type (optional)
     * @param owner Filter by current owner (optional)
     * @param minVal Minimum valuation filter (optional)
     * @param maxVal Maximum valuation filter (optional)
     * @param limit Number of results to return
     * @param offset Pagination offset
     * @return List of matching AssetTrace objects
     */
    public Uni<List<AssetTrace>> searchAssets(String assetType, String owner, Double minVal, Double maxVal,
                                             int limit, int offset) {
        return Uni.createFrom().item(() -> {
            Log.debugf("Searching assets: assetType=%s, owner=%s, minVal=%.2f, maxVal=%.2f",
                assetType, owner, minVal != null ? minVal : 0, maxVal != null ? maxVal : Double.MAX_VALUE);

            // Start with all traces or filter by asset type
            Stream<AssetTrace> stream = assetType != null
                ? assetTypeIndex.getOrDefault(assetType, new HashSet<>()).stream()
                    .map(assetTraces::get)
                    .filter(Objects::nonNull)
                : assetTraces.values().stream();

            // Apply all filters
            List<AssetTrace> results = stream
                .filter(trace -> owner == null || trace.getCurrentOwner().equals(owner))
                .filter(trace -> minVal == null || trace.getValuation() >= minVal)
                .filter(trace -> maxVal == null || trace.getValuation() <= maxVal)
                .skip(offset)
                .limit(limit)
                .collect(Collectors.toList());

            Log.debugf("Asset search returned %d results (offset=%d, limit=%d)",
                results.size(), offset, limit);
            return results;
        });
    }

    /**
     * Record ownership transfer
     *
     * @param traceId Trace identifier
     * @param fromOwner Current owner
     * @param toOwner New owner
     * @param percentage Ownership percentage being transferred
     * @return Updated OwnershipRecord
     */
    public Uni<OwnershipRecord> transferOwnership(String traceId, String fromOwner, String toOwner,
                                                 Double percentage) {
        return Uni.createFrom().item(() -> {
            Log.infof("Recording ownership transfer: traceId=%s, from=%s, to=%s, percentage=%.1f",
                traceId, fromOwner, toOwner, percentage);

            AssetTrace trace = assetTraces.get(traceId);
            if (trace == null) {
                Log.warnf("Asset trace not found: %s", traceId);
                return null;
            }

            // Create new ownership record
            OwnershipRecord transfer = new OwnershipRecord();
            transfer.setOwner(toOwner);
            transfer.setAcquisitionDate(Instant.now());
            transfer.setPercentage(percentage);
            transfer.setTxHash(generateTransactionHash(trace.getAssetId(), toOwner, Instant.now()));

            // If this is a partial transfer, mark the old record's disposal date
            if (percentage < 100.0) {
                OwnershipRecord oldOwner = trace.getOwnershipHistory().get(trace.getOwnershipHistory().size() - 1);
                if (oldOwner.getOwner().equals(fromOwner)) {
                    oldOwner.setDisposalDate(Instant.now());
                }
            }

            // Add to ownership history
            trace.getOwnershipHistory().add(transfer);

            // Update current owner if full transfer
            if (percentage >= 100.0) {
                trace.setCurrentOwner(toOwner);
                // Remove old owner from index and add new one
                ownerIndex.getOrDefault(fromOwner, new HashSet<>()).remove(traceId);
                ownerIndex.computeIfAbsent(toOwner, k -> ConcurrentHashMap.newKeySet()).add(traceId);
            }

            // Update last modified timestamp
            trace.setLastUpdated(Instant.now());

            // Record audit entry
            AuditTrailEntry auditEntry = new AuditTrailEntry();
            auditEntry.setEntryId("audit_" + UUID.randomUUID().toString());
            auditEntry.setAction("TRANSFERRED");
            auditEntry.setActor(fromOwner);
            auditEntry.setTimestamp(Instant.now());
            auditEntry.setDetails(Map.of(
                "from", fromOwner,
                "to", toOwner,
                "percentage", percentage,
                "txHash", transfer.getTxHash()
            ));
            auditEntry.setStatus("SUCCESS");
            trace.getAuditTrail().add(auditEntry);

            Log.infof("Ownership transfer recorded successfully: %s -> %s (%.1f%%)", fromOwner, toOwner, percentage);
            return transfer;
        });
    }

    /**
     * Get ownership history for an asset
     *
     * @param traceId Trace identifier
     * @return Optional containing list of OwnershipRecord if found
     */
    public Uni<Optional<List<OwnershipRecord>>> getOwnershipHistory(String traceId) {
        return Uni.createFrom().item(() -> {
            Log.debugf("Retrieving ownership history: %s", traceId);
            AssetTrace trace = assetTraces.get(traceId);
            if (trace == null) {
                return Optional.empty();
            }
            return Optional.of(new ArrayList<>(trace.getOwnershipHistory()));
        });
    }

    /**
     * Get audit trail for an asset
     *
     * @param traceId Trace identifier
     * @return Optional containing list of AuditTrailEntry if found
     */
    public Uni<Optional<List<AuditTrailEntry>>> getAuditTrail(String traceId) {
        return Uni.createFrom().item(() -> {
            Log.debugf("Retrieving audit trail: %s", traceId);
            AssetTrace trace = assetTraces.get(traceId);
            if (trace == null) {
                return Optional.empty();
            }
            return Optional.of(new ArrayList<>(trace.getAuditTrail()));
        });
    }

    /**
     * Generate transaction hash for ownership records
     *
     * @param assetId Asset identifier
     * @param owner Owner identifier
     * @param timestamp Timestamp of transaction
     * @return Transaction hash (SHA-256)
     */
    private String generateTransactionHash(String assetId, String owner, Instant timestamp) {
        try {
            String input = assetId + owner + timestamp.toEpochMilli();
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes());
            return java.util.HexFormat.of().formatHex(hash);
        } catch (java.security.NoSuchAlgorithmException e) {
            Log.warnf("Error generating transaction hash: %s", e.getMessage());
            return UUID.randomUUID().toString();
        }
    }

    /**
     * Get total number of asset traces
     *
     * @return Total count
     */
    public long getTotalAssetTraces() {
        return assetTraces.size();
    }

    /**
     * Get total number of unique owners
     *
     * @return Total count
     */
    public long getTotalUniqueOwners() {
        return ownerIndex.size();
    }

    /**
     * Get total number of unique asset types
     *
     * @return Total count
     */
    public long getTotalUniqueAssetTypes() {
        return assetTypeIndex.size();
    }
}
