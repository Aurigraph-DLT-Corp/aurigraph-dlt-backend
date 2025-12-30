package io.aurigraph.v11.contracts.traceability;

import io.quarkus.logging.Log;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Contract-Asset Traceability Service
 *
 * Provides comprehensive traceability from ActiveContracts through asset tokenization.
 * Enables full lineage tracking: Contract → Asset → Token → Shareholder
 *
 * @version 1.0.0
 * @author Aurigraph V11 Development Team
 */
@ApplicationScoped
public class ContractAssetTraceabilityService {

    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory
        .getLogger(ContractAssetTraceabilityService.class);

    // In-memory registry for contract-asset links
    private final Map<String, ContractAssetLink> linkRegistry = new ConcurrentHashMap<>();

    // Reverse index: assetId -> contractIds
    private final Map<String, Set<String>> assetToContractsIndex = new ConcurrentHashMap<>();

    // Reverse index: tokenId -> contractAssetLinkId
    private final Map<String, String> tokenToLinkIndex = new ConcurrentHashMap<>();

    /**
     * Link an ActiveContract to an RWA Asset
     */
    public Uni<ContractAssetLink> linkContractToAsset(
        String contractId,
        String contractName,
        String assetId,
        String assetName,
        String assetType,
        Double assetValuation,
        String tokenId,
        String tokenSymbol
    ) {
        return Uni.createFrom().item(() -> {
            LOGGER.info("Linking contract {} to asset {}", contractId, assetId);

            ContractAssetLink link = new ContractAssetLink(contractId, assetId, tokenId);
            link.setContractName(contractName);
            link.setAssetName(assetName);
            link.setAssetType(assetType);
            link.setAssetValuation(assetValuation);
            link.setTokenSymbol(tokenSymbol);
            link.setLinkedAt(Instant.now());
            link.setComplianceStatus("PENDING_VERIFICATION");
            link.setRiskLevel("MEDIUM");

            // Store in primary registry
            linkRegistry.put(link.getLinkId(), link);

            // Update reverse indexes
            assetToContractsIndex.computeIfAbsent(assetId, k -> ConcurrentHashMap.newKeySet())
                .add(contractId);
            tokenToLinkIndex.put(tokenId, link.getLinkId());

            LOGGER.info("Created traceability link: {}", link.getLinkId());
            return link;
        });
    }

    /**
     * Get all assets linked to a specific contract
     */
    public Uni<List<ContractAssetLink>> getAssetsByContract(String contractId) {
        return Uni.createFrom().item(() ->
            linkRegistry.values().stream()
                .filter(link -> link.getContractId().equals(contractId))
                .collect(Collectors.toList())
        );
    }

    /**
     * Get all contracts linked to a specific asset
     */
    public Uni<List<ContractAssetLink>> getContractsByAsset(String assetId) {
        return Uni.createFrom().item(() -> {
            Set<String> contractIds = assetToContractsIndex.getOrDefault(assetId, new HashSet<>());
            return linkRegistry.values().stream()
                .filter(link -> contractIds.contains(link.getContractId()))
                .collect(Collectors.toList());
        });
    }

    /**
     * Get traceability link by linkId
     */
    public Uni<Optional<ContractAssetLink>> getTraceabilityLink(String linkId) {
        return Uni.createFrom().item(() -> Optional.ofNullable(linkRegistry.get(linkId)));
    }

    /**
     * Get complete lineage from contract to token to shareholders
     */
    public Uni<ContractAssetLineage> getCompleteLineage(String contractId) {
        return Uni.createFrom().item(() -> {
            List<ContractAssetLink> assets = linkRegistry.values().stream()
                .filter(link -> link.getContractId().equals(contractId))
                .collect(Collectors.toList());

            ContractAssetLineage lineage = new ContractAssetLineage();
            lineage.setContractId(contractId);
            lineage.setAssets(assets);
            lineage.setTotalAssetValuation(
                assets.stream()
                    .mapToDouble(link -> link.getAssetValuation() != null ? link.getAssetValuation() : 0.0)
                    .sum()
            );
            lineage.setTotalTokensIssued(
                assets.stream()
                    .mapToLong(link -> link.getTotalShares() != null ? link.getTotalShares() : 0L)
                    .sum()
            );

            return lineage;
        });
    }

    /**
     * Update contract execution metrics
     */
    public Uni<ContractAssetLink> recordContractExecution(String linkId, boolean success) {
        return Uni.createFrom().item(() -> {
            ContractAssetLink link = linkRegistry.get(linkId);
            if (link != null) {
                link.recordExecution(success);
                link.setLastUpdatedAt(Instant.now());
                LOGGER.info("Recorded execution for link {}: success={}", linkId, success);
            }
            return link;
        });
    }

    /**
     * Update asset valuation and reflect in contract
     */
    public Uni<ContractAssetLink> updateAssetValuation(String linkId, Double newValuation) {
        return Uni.createFrom().item(() -> {
            ContractAssetLink link = linkRegistry.get(linkId);
            if (link != null) {
                Double oldValuation = link.getAssetValuation();
                link.setAssetValuation(newValuation);
                link.setLastUpdatedAt(Instant.now());
                link.getMetadata().put("valuationHistory",
                    Map.of("old", oldValuation, "new", newValuation, "updatedAt", Instant.now())
                );
                LOGGER.info("Updated valuation for link {}: {} -> {}", linkId, oldValuation, newValuation);
            }
            return link;
        });
    }

    /**
     * Update tokenization details
     */
    public Uni<ContractAssetLink> updateTokenizationDetails(
        String linkId,
        Long totalShares,
        Long sharesOutstanding,
        Instant tokenizedAt
    ) {
        return Uni.createFrom().item(() -> {
            ContractAssetLink link = linkRegistry.get(linkId);
            if (link != null) {
                link.setTotalShares(totalShares);
                link.setSharesOutstanding(sharesOutstanding);
                link.setTokenizedAt(tokenizedAt);
                link.setLastUpdatedAt(Instant.now());
                link.setComplianceStatus("TOKENIZED");
                LOGGER.info("Updated tokenization for link {}: {} shares", linkId, totalShares);
            }
            return link;
        });
    }

    /**
     * Get traceability summary
     */
    public Uni<TraceabilitySummary> getTraceabilitySummary() {
        return Uni.createFrom().item(() -> {
            TraceabilitySummary summary = new TraceabilitySummary();
            summary.setTotalLinks((long) linkRegistry.size());
            summary.setTotalContracts((long) linkRegistry.values().stream()
                .map(ContractAssetLink::getContractId)
                .distinct()
                .count());
            summary.setTotalAssets((long) linkRegistry.values().stream()
                .map(ContractAssetLink::getAssetId)
                .distinct()
                .count());
            summary.setTotalTokens((long) linkRegistry.values().stream()
                .map(ContractAssetLink::getTokenId)
                .distinct()
                .count());

            summary.setAverageLinkSuccessRate(
                linkRegistry.values().stream()
                    .mapToDouble(ContractAssetLink::getSuccessRate)
                    .average()
                    .orElse(0.0)
            );

            summary.setTotalAssetValue(
                linkRegistry.values().stream()
                    .mapToDouble(link -> link.getAssetValuation() != null ? link.getAssetValuation() : 0.0)
                    .sum()
            );

            return summary;
        });
    }

    /**
     * Search for links by criteria
     */
    public Uni<List<ContractAssetLink>> searchLinks(String assetType, String complianceStatus, String riskLevel) {
        return Uni.createFrom().item(() ->
            linkRegistry.values().stream()
                .filter(link ->
                    (assetType == null || link.getAssetType().equals(assetType)) &&
                    (complianceStatus == null || link.getComplianceStatus().equals(complianceStatus)) &&
                    (riskLevel == null || link.getRiskLevel().equals(riskLevel))
                )
                .collect(Collectors.toList())
        );
    }

    /**
     * Verify asset-contract binding integrity
     */
    public Uni<Map<String, Object>> verifyIntegrity(String linkId) {
        return Uni.createFrom().item(() -> {
            ContractAssetLink link = linkRegistry.get(linkId);
            Map<String, Object> verification = new HashMap<>();

            if (link != null) {
                verification.put("linkId", linkId);
                verification.put("isValid", true);
                verification.put("hasToken", link.getTokenId() != null && !link.getTokenId().isEmpty());
                verification.put("hasAsset", link.getAssetId() != null && !link.getAssetId().isEmpty());
                verification.put("hasContract", link.getContractId() != null && !link.getContractId().isEmpty());
                verification.put("successRate", link.getSuccessRate());
                verification.put("lastVerified", Instant.now());
                verification.put("status", "VERIFIED");
            } else {
                verification.put("isValid", false);
                verification.put("status", "NOT_FOUND");
            }

            return verification;
        });
    }
}
