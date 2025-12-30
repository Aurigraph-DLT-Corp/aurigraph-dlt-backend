package io.aurigraph.v11.registries.smartcontract;

import io.quarkus.logging.Log;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Smart Contract Registry Service
 *
 * Core backend service for managing smart contract registrations and lifecycle.
 * Provides business logic for contract registration, deployment tracking, asset linking,
 * and status management. Uses in-memory ConcurrentHashMap for storage with
 * full audit trail support.
 *
 * @version 11.5.0
 * @since 2025-11-14
 */
@ApplicationScoped
public class SmartContractRegistryService {

    // In-memory storage using ConcurrentHashMap for thread-safe operations
    private final Map<String, SmartContractRegistryEntry> contractRegistry = new ConcurrentHashMap<>();
    private final Map<String, List<RegistryAuditEntry>> auditTrail = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> assetToContractIndex = new ConcurrentHashMap<>();

    // Audit entry for tracking operations
    private static class RegistryAuditEntry {
        String operation;
        String contractId;
        Instant timestamp;
        String details;

        RegistryAuditEntry(String operation, String contractId, String details) {
            this.operation = operation;
            this.contractId = contractId;
            this.timestamp = Instant.now();
            this.details = details;
        }
    }

    /**
     * Register a new smart contract in the registry
     *
     * @param contractId Unique contract identifier
     * @param contractName Human-readable contract name
     * @param description Contract description
     * @param deploymentAddress Contract address on blockchain
     * @param deploymentTxHash Deployment transaction hash
     * @param codeHash SHA-256 hash of contract code
     * @param status Initial contract status
     * @return Uni with registered contract entry
     */
    public Uni<SmartContractRegistryEntry> registerContract(
            String contractId,
            String contractName,
            String description,
            String deploymentAddress,
            String deploymentTxHash,
            String codeHash,
            String status) {
        return Uni.createFrom().item(() -> {
            Log.infof("Registering contract: %s (%s)", contractId, contractName);

            // Validation
            if (contractId == null || contractId.isBlank()) {
                throw new IllegalArgumentException("Contract ID cannot be null or empty");
            }
            if (contractRegistry.containsKey(contractId)) {
                throw new IllegalArgumentException("Contract already registered: " + contractId);
            }
            if (contractName == null || contractName.isBlank()) {
                throw new IllegalArgumentException("Contract name cannot be null or empty");
            }

            // Create registry entry
            SmartContractRegistryEntry entry = new SmartContractRegistryEntry(
                    contractId,
                    contractName,
                    description,
                    deploymentAddress,
                    deploymentTxHash,
                    codeHash,
                    null
            );

            // Set initial status
            try {
                ContractStatusEnum statusEnum = ContractStatusEnum.fromValue(status);
                entry.setCurrentStatus(statusEnum);
            } catch (IllegalArgumentException e) {
                Log.warnf("Invalid status '%s', defaulting to DRAFT", status);
                entry.setCurrentStatus(ContractStatusEnum.DRAFT);
            }

            // Create deployment info
            ContractDeploymentInfo deploymentInfo = new ContractDeploymentInfo(
                    deploymentAddress,
                    deploymentTxHash,
                    "mainnet",
                    0,
                    codeHash,
                    "1.0.0"
            );
            entry.setDeploymentInfo(deploymentInfo);

            // Store in registry
            contractRegistry.put(contractId, entry);
            auditTrail.put(contractId, new ArrayList<>());

            // Log audit entry
            recordAudit("REGISTER", contractId, "Contract registered: " + contractName);

            Log.infof("Contract registered successfully: %s", contractId);
            return entry;
        });
    }

    /**
     * Get contract details by ID
     *
     * @param contractId Contract ID to retrieve
     * @return Uni with contract entry
     */
    public Uni<SmartContractRegistryEntry> getContractDetails(String contractId) {
        return Uni.createFrom().item(() -> {
            SmartContractRegistryEntry entry = contractRegistry.get(contractId);
            if (entry == null) {
                throw new ContractRegistryException("Contract not found: " + contractId);
            }
            Log.debugf("Retrieved contract details: %s", contractId);
            return entry;
        });
    }

    /**
     * Search contracts by name and/or status with pagination
     *
     * @param name Contract name filter (case-insensitive substring match)
     * @param status Contract status filter
     * @param limit Maximum results to return
     * @param offset Starting index
     * @return Uni with list of matching contracts
     */
    public Uni<List<SmartContractRegistryEntry>> searchContracts(
            String name, String status, int limit, int offset) {
        return Uni.createFrom().item(() -> {
            Log.debugf("Searching contracts - name:%s, status:%s, limit:%d, offset:%d", name, status, limit, offset);

            List<SmartContractRegistryEntry> results = contractRegistry.values().stream()
                    .filter(entry -> filterByName(entry, name))
                    .filter(entry -> filterByStatus(entry, status))
                    .sorted((a, b) -> b.getRegisteredAt().compareTo(a.getRegisteredAt()))
                    .skip(Math.max(0, offset))
                    .limit(Math.max(1, limit))
                    .collect(Collectors.toList());

            Log.debugf("Search returned %d results", results.size());
            return results;
        });
    }

    /**
     * Get assets linked to a specific contract
     *
     * @param contractId Contract ID
     * @return Uni with set of linked asset IDs
     */
    public Uni<Set<String>> getLinkedAssets(String contractId) {
        return Uni.createFrom().item(() -> {
            SmartContractRegistryEntry entry = contractRegistry.get(contractId);
            if (entry == null) {
                throw new ContractRegistryException("Contract not found: " + contractId);
            }

            Set<String> assets = new HashSet<>(entry.getLinkedAssets());
            Log.debugf("Retrieved %d linked assets for contract: %s", assets.size(), contractId);
            return assets;
        });
    }

    /**
     * Update contract status with validation
     *
     * @param contractId Contract ID
     * @param newStatus New status value
     * @return Uni with updated contract entry
     */
    public Uni<SmartContractRegistryEntry> updateContractStatus(
            String contractId, String newStatus) {
        return Uni.createFrom().item(() -> {
            SmartContractRegistryEntry entry = contractRegistry.get(contractId);
            if (entry == null) {
                throw new ContractRegistryException("Contract not found: " + contractId);
            }

            try {
                ContractStatusEnum statusEnum = ContractStatusEnum.fromValue(newStatus);
                entry.updateStatus(statusEnum);
                recordAudit("STATUS_UPDATE", contractId, "Status updated to: " + statusEnum);
                Log.infof("Contract status updated: %s -> %s", contractId, statusEnum);
            } catch (IllegalArgumentException e) {
                throw new ContractRegistryException("Invalid status: " + newStatus);
            } catch (IllegalStateException e) {
                throw new ContractRegistryException("Invalid status transition: " + e.getMessage());
            }

            return entry;
        });
    }

    /**
     * Remove contract from registry
     *
     * @param contractId Contract ID to remove
     * @return Uni with removal status
     */
    public Uni<Boolean> removeContract(String contractId) {
        return Uni.createFrom().item(() -> {
            SmartContractRegistryEntry entry = contractRegistry.get(contractId);
            if (entry == null) {
                throw new ContractRegistryException("Contract not found: " + contractId);
            }

            // Remove from asset index
            entry.getLinkedAssets().forEach(assetId -> {
                Set<String> contracts = assetToContractIndex.get(assetId);
                if (contracts != null) {
                    contracts.remove(contractId);
                }
            });

            // Remove from registry
            contractRegistry.remove(contractId);
            recordAudit("REMOVE", contractId, "Contract removed from registry");

            Log.infof("Contract removed: %s", contractId);
            return true;
        });
    }

    /**
     * Link an asset to a contract
     *
     * @param contractId Contract ID
     * @param assetId Asset ID to link
     * @return Uni with updated contract entry
     */
    public Uni<SmartContractRegistryEntry> linkAsset(String contractId, String assetId) {
        return Uni.createFrom().item(() -> {
            SmartContractRegistryEntry entry = contractRegistry.get(contractId);
            if (entry == null) {
                throw new ContractRegistryException("Contract not found: " + contractId);
            }

            if (entry.linkAsset(assetId)) {
                assetToContractIndex.computeIfAbsent(assetId, k -> new HashSet<>()).add(contractId);
                recordAudit("LINK_ASSET", contractId, "Asset linked: " + assetId);
                Log.infof("Asset linked to contract: %s -> %s", contractId, assetId);
            } else {
                Log.warnf("Asset already linked: %s -> %s", contractId, assetId);
            }

            return entry;
        });
    }

    /**
     * Unlink an asset from a contract
     *
     * @param contractId Contract ID
     * @param assetId Asset ID to unlink
     * @return Uni with updated contract entry
     */
    public Uni<SmartContractRegistryEntry> unlinkAsset(String contractId, String assetId) {
        return Uni.createFrom().item(() -> {
            SmartContractRegistryEntry entry = contractRegistry.get(contractId);
            if (entry == null) {
                throw new ContractRegistryException("Contract not found: " + contractId);
            }

            if (entry.unlinkAsset(assetId)) {
                Set<String> contracts = assetToContractIndex.get(assetId);
                if (contracts != null) {
                    contracts.remove(contractId);
                }
                recordAudit("UNLINK_ASSET", contractId, "Asset unlinked: " + assetId);
                Log.infof("Asset unlinked from contract: %s -> %s", contractId, assetId);
            }

            return entry;
        });
    }

    /**
     * Get contracts using a specific asset
     *
     * @param assetId Asset ID
     * @return Uni with list of contracts using this asset
     */
    public Uni<List<SmartContractRegistryEntry>> getContractsForAsset(String assetId) {
        return Uni.createFrom().item(() -> {
            Set<String> contractIds = assetToContractIndex.getOrDefault(assetId, new HashSet<>());
            List<SmartContractRegistryEntry> contracts = contractIds.stream()
                    .map(contractRegistry::get)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            Log.debugf("Found %d contracts for asset: %s", contracts.size(), assetId);
            return contracts;
        });
    }

    /**
     * Get comprehensive contract statistics
     *
     * @return Uni with statistics map
     */
    public Uni<Map<String, Object>> getContractStatistics() {
        return Uni.createFrom().item(() -> {
            Map<String, Object> stats = new HashMap<>();

            long totalContracts = contractRegistry.size();
            long activeContracts = contractRegistry.values().stream()
                    .filter(SmartContractRegistryEntry::isActive)
                    .count();
            long auditedContracts = contractRegistry.values().stream()
                    .filter(c -> c.getCurrentStatus() == ContractStatusEnum.AUDITED)
                    .count();
            long deprecatedContracts = contractRegistry.values().stream()
                    .filter(c -> c.getCurrentStatus() == ContractStatusEnum.DEPRECATED)
                    .count();

            // Status breakdown
            Map<String, Long> statusBreakdown = contractRegistry.values().stream()
                    .collect(Collectors.groupingBy(
                            c -> c.getCurrentStatus().name(),
                            Collectors.counting()
                    ));

            // Asset linkage statistics
            long totalAssetLinks = contractRegistry.values().stream()
                    .mapToLong(c -> c.getLinkedAssetCount())
                    .sum();

            stats.put("totalContracts", totalContracts);
            stats.put("activeContracts", activeContracts);
            stats.put("auditedContracts", auditedContracts);
            stats.put("deprecatedContracts", deprecatedContracts);
            stats.put("statusBreakdown", statusBreakdown);
            stats.put("totalAssetLinks", totalAssetLinks);
            stats.put("averageAssetsPerContract",
                    totalContracts > 0 ? (double) totalAssetLinks / totalContracts : 0.0);
            stats.put("timestamp", Instant.now().toString());
            stats.put("registryVersion", "12.0.0");

            Log.debugf("Contract statistics calculated - Total: %d, Active: %d", totalContracts, activeContracts);
            return stats;
        });
    }

    /**
     * Get audit trail for a contract
     *
     * @param contractId Contract ID
     * @return Uni with audit entries
     */
    public Uni<List<Map<String, Object>>> getAuditTrail(String contractId) {
        return Uni.createFrom().item(() -> {
            SmartContractRegistryEntry entry = contractRegistry.get(contractId);
            if (entry == null) {
                throw new ContractRegistryException("Contract not found: " + contractId);
            }

            List<RegistryAuditEntry> trail = auditTrail.getOrDefault(contractId, new ArrayList<>());
            List<Map<String, Object>> auditList = trail.stream()
                    .map(audit -> {
                        Map<String, Object> auditMap = new java.util.HashMap<>();
                        auditMap.put("operation", audit.operation);
                        auditMap.put("timestamp", audit.timestamp.toString());
                        auditMap.put("details", audit.details);
                        return auditMap;
                    })
                    .collect(Collectors.toList());

            Log.debugf("Retrieved %d audit entries for contract: %s", auditList.size(), contractId);
            return auditList;
        });
    }

    // Helper methods

    private boolean filterByName(SmartContractRegistryEntry entry, String name) {
        if (name == null || name.isBlank()) {
            return true;
        }
        return entry.getContractName() != null &&
               entry.getContractName().toLowerCase().contains(name.toLowerCase());
    }

    private boolean filterByStatus(SmartContractRegistryEntry entry, String status) {
        if (status == null || status.isBlank()) {
            return true;
        }
        try {
            ContractStatusEnum statusEnum = ContractStatusEnum.fromValue(status);
            return entry.getCurrentStatus() == statusEnum;
        } catch (IllegalArgumentException e) {
            Log.warnf("Invalid status filter: %s", status);
            return false;
        }
    }

    private void recordAudit(String operation, String contractId, String details) {
        List<RegistryAuditEntry> trail = auditTrail.computeIfAbsent(contractId, k -> new ArrayList<>());
        trail.add(new RegistryAuditEntry(operation, contractId, details));
    }

    // Exception class

    /**
     * Exception for contract registry operations
     */
    public static class ContractRegistryException extends RuntimeException {
        public ContractRegistryException(String message) {
            super(message);
        }

        public ContractRegistryException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
