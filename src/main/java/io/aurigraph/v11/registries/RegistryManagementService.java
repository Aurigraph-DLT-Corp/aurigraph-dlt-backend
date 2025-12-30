package io.aurigraph.v11.registries;

import io.aurigraph.v11.contracts.ActiveContract;
import io.aurigraph.v11.contracts.ActiveContractService;
import io.aurigraph.v11.models.TokenRegistry;
import io.aurigraph.v11.models.TokenRegistryService;
import io.aurigraph.v11.registry.RWATRegistry;
import io.aurigraph.v11.registry.RWATRegistryService;
import io.aurigraph.v11.compliance.erc3643.ComplianceRegistry;
import io.aurigraph.v11.merkle.MerkleTreeRegistry;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import io.quarkus.logging.Log;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Registry Management Service
 *
 * Unified service for multi-registry search and aggregation.
 * Provides search, statistics, and verification across all registry types:
 * - Smart Contracts
 * - Tokens (ERC20, ERC721, ERC1155)
 * - Real-World Assets (RWATs)
 * - Merkle Trees
 * - Compliance records
 *
 * @version 11.5.0
 * @since 2025-11-14
 */
@ApplicationScoped
public class RegistryManagementService {

    @Inject
    ActiveContractService activeContractService;

    @Inject
    TokenRegistryService tokenRegistryService;

    @Inject
    RWATRegistryService rwatRegistryService;

    @Inject
    ComplianceRegistry complianceRegistry;

    private static final int DEFAULT_LIMIT = 50;
    private static final int MAX_LIMIT = 500;

    /**
     * Search across all registries with keyword matching
     *
     * @param keyword the search keyword
     * @param types list of registry types to search (null = all types)
     * @param limit maximum number of results (default: 50, max: 500)
     * @param offset pagination offset
     * @return list of search results across all registries
     */
    public Uni<List<RegistrySearchResult>> searchAllRegistries(
            String keyword,
            List<String> types,
            int limit,
            int offset) {

        final int finalLimit = Math.min(Math.max(limit, 1), MAX_LIMIT);
        final int finalOffset = Math.max(offset, 0);

        Log.infof("Searching registries - keyword: %s, types: %s, limit: %d, offset: %d",
                keyword, types, limit, offset);

        // Determine which types to search
        List<RegistryType> registryTypes = determineRegistryTypes(types);

        List<Uni<List<RegistrySearchResult>>> searchUnis = new ArrayList<>();

        for (RegistryType type : registryTypes) {
            switch (type) {
                case SMART_CONTRACT:
                    searchUnis.add(searchSmartContracts(keyword));
                    break;
                case TOKEN:
                    searchUnis.add(searchTokens(keyword));
                    break;
                case RWA:
                    searchUnis.add(searchRWATs(keyword));
                    break;
                case MERKLE_TREE:
                    // Merkle tree search limited to specific queries
                    searchUnis.add(Uni.createFrom().item(Collections.emptyList()));
                    break;
                case COMPLIANCE:
                    searchUnis.add(searchCompliance(keyword));
                    break;
            }
        }

        return Uni.createFrom().item(() -> {
                List<RegistrySearchResult> allResults = new ArrayList<>();
                for (Uni<List<RegistrySearchResult>> uni : searchUnis) {
                    List<RegistrySearchResult> results = uni.await().indefinitely();
                    if (results != null) {
                        allResults.addAll(results);
                    }
                }
                return allResults.stream()
                        .skip(finalOffset)
                        .limit(finalLimit)
                        .collect(Collectors.toList());
            })
            .onFailure().recoverWithItem(error -> {
                Log.errorf("Error searching registries: %s", error.getMessage());
                return Collections.emptyList();
            });
    }

    /**
     * Get statistics for a specific registry type
     *
     * @param registryTypeStr the registry type identifier
     * @return statistics for that registry type
     */
    public Uni<RegistryStatistics> getRegistryStats(String registryTypeStr) {
        try {
            RegistryType registryType = RegistryType.fromString(registryTypeStr);
            Log.infof("Getting statistics for registry type: %s", registryType.getDisplayName());

            return switch (registryType) {
                case SMART_CONTRACT -> getContractStats();
                case TOKEN -> getTokenStats();
                case RWA -> getRWATStats();
                case MERKLE_TREE -> getMerkleStats();
                case COMPLIANCE -> getComplianceStats();
            };
        } catch (IllegalArgumentException e) {
            Log.warnf("Invalid registry type: %s", registryTypeStr);
            return Uni.createFrom().failure(e);
        }
    }

    /**
     * Get aggregated statistics across all registries
     *
     * @return combined statistics from all registries
     */
    public Uni<RegistryAggregation> getAggregatedStats() {
        Log.infof("Getting aggregated registry statistics");

        return Uni.combine().all()
                .unis(
                    getContractStats(),
                    getTokenStats(),
                    getRWATStats(),
                    getMerkleStats(),
                    getComplianceStats()
                )
                .asTuple()
                .map(tuple -> {
                    RegistryAggregation aggregation = new RegistryAggregation();

                    // Add stats for each registry type
                    RegistryStatistics contractStats = tuple.getItem1();
                    RegistryStatistics tokenStats = tuple.getItem2();
                    RegistryStatistics rwatStats = tuple.getItem3();
                    RegistryStatistics merkleStats = tuple.getItem4();
                    RegistryStatistics complianceStats = tuple.getItem5();

                    aggregation.addRegistryTypeStat(RegistryType.SMART_CONTRACT.getId(),
                        new RegistryAggregation.RegistryTypeAggregateData(
                            RegistryType.SMART_CONTRACT.getDisplayName(),
                            contractStats.getTotalEntries(),
                            contractStats.getVerifiedEntries()
                        ));

                    aggregation.addRegistryTypeStat(RegistryType.TOKEN.getId(),
                        new RegistryAggregation.RegistryTypeAggregateData(
                            RegistryType.TOKEN.getDisplayName(),
                            tokenStats.getTotalEntries(),
                            tokenStats.getVerifiedEntries()
                        ));

                    aggregation.addRegistryTypeStat(RegistryType.RWA.getId(),
                        new RegistryAggregation.RegistryTypeAggregateData(
                            RegistryType.RWA.getDisplayName(),
                            rwatStats.getTotalEntries(),
                            rwatStats.getVerifiedEntries()
                        ));

                    aggregation.addRegistryTypeStat(RegistryType.MERKLE_TREE.getId(),
                        new RegistryAggregation.RegistryTypeAggregateData(
                            RegistryType.MERKLE_TREE.getDisplayName(),
                            merkleStats.getTotalEntries(),
                            merkleStats.getVerifiedEntries()
                        ));

                    aggregation.addRegistryTypeStat(RegistryType.COMPLIANCE.getId(),
                        new RegistryAggregation.RegistryTypeAggregateData(
                            RegistryType.COMPLIANCE.getDisplayName(),
                            complianceStats.getTotalEntries(),
                            complianceStats.getVerifiedEntries()
                        ));

                    // Calculate totals
                    aggregation.setTotalEntries(
                        contractStats.getTotalEntries() +
                        tokenStats.getTotalEntries() +
                        rwatStats.getTotalEntries() +
                        merkleStats.getTotalEntries() +
                        complianceStats.getTotalEntries()
                    );

                    aggregation.setTotalVerifiedEntries(
                        contractStats.getVerifiedEntries() +
                        tokenStats.getVerifiedEntries() +
                        rwatStats.getVerifiedEntries() +
                        merkleStats.getVerifiedEntries() +
                        complianceStats.getVerifiedEntries()
                    );

                    aggregation.setTotalActiveEntries(
                        contractStats.getActiveEntries() +
                        tokenStats.getActiveEntries() +
                        rwatStats.getActiveEntries() +
                        merkleStats.getActiveEntries() +
                        complianceStats.getActiveEntries()
                    );

                    // Calculate coverage and health
                    aggregation.calculateVerificationCoverage();
                    aggregation.determineHealthStatus();

                    return aggregation;
                })
                .onFailure().recoverWithItem(error -> {
                    Log.errorf("Error getting aggregated statistics: %s", error.getMessage());
                    return new RegistryAggregation();
                });
    }

    /**
     * List all entries of a specific type with pagination
     *
     * @param registryTypeStr the registry type identifier
     * @param limit maximum number of results
     * @param offset pagination offset
     * @return list of entries
     */
    public Uni<List<RegistrySearchResult>> listByType(String registryTypeStr, int limit, int offset) {
        limit = Math.min(Math.max(limit, 1), MAX_LIMIT);
        offset = Math.max(offset, 0);

        Log.infof("Listing registry entries - type: %s, limit: %d, offset: %d",
                registryTypeStr, limit, offset);

        try {
            RegistryType registryType = RegistryType.fromString(registryTypeStr);

            return switch (registryType) {
                case SMART_CONTRACT -> listSmartContracts(limit, offset);
                case TOKEN -> listTokens(limit, offset);
                case RWA -> listRWATs(limit, offset);
                case MERKLE_TREE -> listMerkleTrees(limit, offset);
                case COMPLIANCE -> listComplianceRecords(limit, offset);
            };
        } catch (IllegalArgumentException e) {
            Log.warnf("Invalid registry type: %s", registryTypeStr);
            return Uni.createFrom().failure(e);
        }
    }

    /**
     * Verify an entry across registries
     *
     * @param entryId the entry identifier to verify
     * @return verification results
     */
    public Uni<Map<String, Object>> verifyEntry(String entryId) {
        Log.infof("Verifying entry: %s", entryId);

        return Uni.combine().all()
                .unis(
                    verifyInSmartContracts(entryId),
                    verifyInTokens(entryId),
                    verifyInRWATs(entryId),
                    verifyInCompliance(entryId)
                )
                .asTuple()
                .map(tuple -> {
                    Map<String, Object> results = new LinkedHashMap<>();
                    results.put("entryId", entryId);
                    results.put("timestamp", Instant.now());

                    Map<String, Boolean> verificationStatus = new LinkedHashMap<>();
                    verificationStatus.put("found_in_contracts", tuple.getItem1());
                    verificationStatus.put("found_in_tokens", tuple.getItem2());
                    verificationStatus.put("found_in_rwats", tuple.getItem3());
                    verificationStatus.put("found_in_compliance", tuple.getItem4());

                    results.put("verificationStatus", verificationStatus);
                    results.put("verified", verificationStatus.values().stream().anyMatch(v -> v));

                    return results;
                })
                .onFailure().recoverWithItem(error -> {
                    Log.errorf("Error verifying entry: %s", error.getMessage());
                    Map<String, Object> errorResult = new LinkedHashMap<>();
                    errorResult.put("entryId", entryId);
                    errorResult.put("error", error.getMessage());
                    errorResult.put("verified", false);
                    return errorResult;
                });
    }

    /**
     * Get registry summary
     *
     * @return summary information about all registries
     */
    public Uni<Map<String, Object>> getRegistrySummary() {
        Log.infof("Getting registry summary");

        return getAggregatedStats()
                .map(aggregation -> {
                    Map<String, Object> summary = new LinkedHashMap<>();
                    summary.put("totalRegistries", aggregation.getTotalRegistries());
                    summary.put("totalEntries", aggregation.getTotalEntries());
                    summary.put("verificationCoverage", String.format("%.2f%%", aggregation.getVerificationCoverage()));
                    summary.put("healthStatus", aggregation.getHealthStatus());
                    summary.put("lastUpdated", aggregation.getLastUpdatedTimestamp());

                    // Add registry type breakdown
                    Map<String, Map<String, Object>> breakdown = new LinkedHashMap<>();
                    for (Map.Entry<String, RegistryAggregation.RegistryTypeAggregateData> entry :
                            aggregation.getRegistryTypeStats().entrySet()) {
                        RegistryAggregation.RegistryTypeAggregateData data = entry.getValue();
                        Map<String, Object> typeData = new LinkedHashMap<>();
                        typeData.put("entries", data.getEntryCount());
                        typeData.put("verified", data.getVerifiedCount());
                        typeData.put("verification_rate", String.format("%.2f%%", data.getVerificationRate()));
                        breakdown.put(entry.getKey(), typeData);
                    }
                    summary.put("registryBreakdown", breakdown);

                    return summary;
                });
    }

    // Private helper methods

    private List<RegistryType> determineRegistryTypes(List<String> types) {
        if (types == null || types.isEmpty()) {
            return Arrays.asList(RegistryType.values());
        }

        return types.stream()
                .filter(RegistryType::isValid)
                .map(RegistryType::fromString)
                .collect(Collectors.toList());
    }

    private Uni<List<RegistrySearchResult>> searchSmartContracts(String keyword) {
        return activeContractService.listContracts()
                .map(contracts -> contracts.stream()
                    .filter(c -> matchesKeyword(c.getName(), c.getContractType(), keyword))
                    .map(c -> toSearchResult(c.getContractId(), "ActiveContract", c.getName(),
                        RegistryType.SMART_CONTRACT, c.getVersion()))
                    .collect(Collectors.toList())
                )
                .onFailure().recoverWithItem(Collections.emptyList());
    }

    private Uni<List<RegistrySearchResult>> searchTokens(String keyword) {
        return tokenRegistryService.listTokens()
                .map(tokens -> tokens.stream()
                    .filter(t -> matchesKeyword(t.getName(), t.getSymbol(), keyword))
                    .map(t -> toSearchResult(t.getTokenAddress(), "TokenRegistry", t.getName(),
                        RegistryType.TOKEN, ""))
                    .collect(Collectors.toList())
                )
                .onFailure().recoverWithItem(Collections.emptyList());
    }

    private Uni<List<RegistrySearchResult>> searchRWATs(String keyword) {
        return rwatRegistryService.searchRWATs(keyword)
                .map(rwats -> rwats.stream()
                    .map(r -> toSearchResult(r.getRwatId(), "RWATRegistry", r.getAssetName(),
                        RegistryType.RWA, r.getVerificationStatus().name()))
                    .collect(Collectors.toList())
                )
                .onFailure().recoverWithItem(Collections.emptyList());
    }

    private Uni<List<RegistrySearchResult>> searchCompliance(String keyword) {
        // Compliance search would be implemented based on compliance registry structure
        return Uni.createFrom().item(Collections.emptyList());
    }

    private Uni<RegistryStatistics> getContractStats() {
        return activeContractService.listContracts()
                .map(contracts -> {
                    RegistryStatistics stats = new RegistryStatistics(RegistryType.SMART_CONTRACT.getId());
                    stats.setTotalEntries(contracts.size());
                    stats.setActiveEntries(contracts.stream().filter(ActiveContract::isActive).count());
                    stats.setVerifiedEntries(contracts.stream().filter(ActiveContract::isFullySigned).count());
                    return stats;
                })
                .onFailure().recoverWithItem(new RegistryStatistics(RegistryType.SMART_CONTRACT.getId()));
    }

    private Uni<RegistryStatistics> getTokenStats() {
        return tokenRegistryService.listTokens()
                .map(tokens -> {
                    RegistryStatistics stats = new RegistryStatistics(RegistryType.TOKEN.getId());
                    stats.setTotalEntries(tokens.size());
                    return stats;
                })
                .onFailure().recoverWithItem(new RegistryStatistics(RegistryType.TOKEN.getId()));
    }

    private Uni<RegistryStatistics> getRWATStats() {
        return rwatRegistryService.getAll()
                .map(rwats -> {
                    RegistryStatistics stats = new RegistryStatistics(RegistryType.RWA.getId());
                    stats.setTotalEntries(rwats.size());
                    stats.setActiveEntries(rwats.stream().filter(RWATRegistry::isActive).count());
                    stats.setVerifiedEntries(rwats.stream()
                        .filter(r -> "VERIFIED".equals(r.getVerificationStatus().name()))
                        .count());
                    return stats;
                })
                .onFailure().recoverWithItem(new RegistryStatistics(RegistryType.RWA.getId()));
    }

    private Uni<RegistryStatistics> getMerkleStats() {
        // Merkle tree statistics
        RegistryStatistics stats = new RegistryStatistics(RegistryType.MERKLE_TREE.getId());
        return Uni.createFrom().item(stats);
    }

    private Uni<RegistryStatistics> getComplianceStats() {
        // Compliance statistics
        RegistryStatistics stats = new RegistryStatistics(RegistryType.COMPLIANCE.getId());
        return Uni.createFrom().item(stats);
    }

    private Uni<List<RegistrySearchResult>> listSmartContracts(int limit, int offset) {
        return activeContractService.listContracts()
                .map(contracts -> contracts.stream()
                    .skip(offset)
                    .limit(limit)
                    .map(c -> toSearchResult(c.getContractId(), "ActiveContract", c.getName(),
                        RegistryType.SMART_CONTRACT, c.getVersion()))
                    .collect(Collectors.toList())
                )
                .onFailure().recoverWithItem(Collections.emptyList());
    }

    private Uni<List<RegistrySearchResult>> listTokens(int limit, int offset) {
        return tokenRegistryService.listTokens()
                .map(tokens -> tokens.stream()
                    .skip(offset)
                    .limit(limit)
                    .map(t -> toSearchResult(t.getTokenAddress(), "TokenRegistry", t.getName(),
                        RegistryType.TOKEN, ""))
                    .collect(Collectors.toList())
                )
                .onFailure().recoverWithItem(Collections.emptyList());
    }

    private Uni<List<RegistrySearchResult>> listRWATs(int limit, int offset) {
        return rwatRegistryService.getAll()
                .map(rwats -> rwats.stream()
                    .skip(offset)
                    .limit(limit)
                    .map(r -> toSearchResult(r.getRwatId(), "RWATRegistry", r.getAssetName(),
                        RegistryType.RWA, r.getVerificationStatus().name()))
                    .collect(Collectors.toList())
                )
                .onFailure().recoverWithItem(Collections.emptyList());
    }

    private Uni<List<RegistrySearchResult>> listMerkleTrees(int limit, int offset) {
        return Uni.createFrom().item(Collections.emptyList());
    }

    private Uni<List<RegistrySearchResult>> listComplianceRecords(int limit, int offset) {
        return Uni.createFrom().item(Collections.emptyList());
    }

    private Uni<Boolean> verifyInSmartContracts(String entryId) {
        return activeContractService.getContract(entryId)
                .map(c -> true)
                .onFailure().recoverWithItem(false);
    }

    private Uni<Boolean> verifyInTokens(String entryId) {
        return tokenRegistryService.getTokenByAddress(entryId)
                .map(t -> true)
                .onFailure().recoverWithItem(false);
    }

    private Uni<Boolean> verifyInRWATs(String entryId) {
        return rwatRegistryService.getRWAT(entryId)
                .map(r -> true)
                .onFailure().recoverWithItem(false);
    }

    private Uni<Boolean> verifyInCompliance(String entryId) {
        return Uni.createFrom().item(
            complianceRegistry.getCompliance(entryId) != null
        );
    }

    private boolean matchesKeyword(String... fields) {
        return Arrays.stream(fields)
                .anyMatch(f -> f != null && !f.isEmpty());
    }

    private RegistrySearchResult toSearchResult(
            String id, String type, String name, RegistryType registryType, String status) {
        RegistrySearchResult result = new RegistrySearchResult(id, type, name, registryType.getId());
        result.setVerificationStatus(status);
        result.setLastUpdated(Instant.now());
        return result;
    }
}
