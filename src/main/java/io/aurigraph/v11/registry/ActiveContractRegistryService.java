package io.aurigraph.v11.registry;

import io.aurigraph.v11.contracts.ActiveContract;
import io.aurigraph.v11.contracts.ActiveContractService;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * ActiveContract Registry Service
 *
 * Public registry for searching and viewing deployed ActiveContracts.
 * Provides transparency and discoverability for all contracts on the platform.
 *
 * @version 11.4.0
 * @since 2025-10-13
 */
@ApplicationScoped
public class ActiveContractRegistryService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ActiveContractRegistryService.class);

    @Inject
    ActiveContractService contractService;

    /**
     * Search contracts by keyword
     */
    public Uni<List<ActiveContract>> searchContracts(String keyword) {
        return contractService.listContracts()
                .map(contracts -> contracts.stream()
                        .filter(c -> matchesKeyword(c, keyword))
                        .collect(Collectors.toList())
                );
    }

    /**
     * Get contract by ID (public view)
     */
    public Uni<ActiveContract> getContractPublic(String contractId) {
        return contractService.getContract(contractId);
    }

    /**
     * List contracts by category
     */
    public Uni<List<ActiveContract>> listByCategory(String category) {
        return contractService.listContractsByType(category);
    }

    /**
     * List recently deployed contracts
     */
    public Uni<List<ActiveContract>> listRecentContracts(int limit) {
        return contractService.listContracts()
                .map(contracts -> contracts.stream()
                        .sorted((a, b) -> b.getDeployedAt().compareTo(a.getDeployedAt()))
                        .limit(limit)
                        .collect(Collectors.toList())
                );
    }

    /**
     * Get registry statistics
     */
    public Map<String, Object> getRegistryStatistics() {
        Map<String, Object> stats = new HashMap<>();

        List<ActiveContract> allContracts = contractService.listContracts()
                .await().indefinitely();

        stats.put("totalContracts", allContracts.size());
        stats.put("activeContracts", allContracts.stream()
                .filter(ActiveContract::isActive).count());
        stats.put("verifiedContracts", allContracts.stream()
                .filter(c -> c.isFullySigned()).count());
        stats.put("totalExecutions", contractService.getMetrics().get("contractsExecuted"));

        // Count by type
        Map<String, Long> byType = allContracts.stream()
                .filter(c -> c.getContractType() != null)
                .collect(Collectors.groupingBy(
                        ActiveContract::getContractType,
                        Collectors.counting()
                ));
        stats.put("contractsByType", byType);

        // Count by language
        Map<String, Long> byLanguage = allContracts.stream()
                .filter(c -> c.getLanguage() != null)
                .collect(Collectors.groupingBy(
                        c -> c.getLanguage().name(),
                        Collectors.counting()
                ));
        stats.put("contractsByLanguage", byLanguage);

        return stats;
    }

    /**
     * List featured contracts (most executed, highest value, etc.)
     */
    public Uni<List<ActiveContract>> listFeaturedContracts(int limit) {
        return contractService.listContracts()
                .map(contracts -> contracts.stream()
                        .sorted((a, b) -> Long.compare(b.getExecutionCount(), a.getExecutionCount()))
                        .limit(limit)
                        .collect(Collectors.toList())
                );
    }

    // Helper method
    private boolean matchesKeyword(ActiveContract contract, String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return true;
        }

        String lowerKeyword = keyword.toLowerCase();

        return (contract.getName() != null && contract.getName().toLowerCase().contains(lowerKeyword)) ||
                (contract.getContractType() != null && contract.getContractType().toLowerCase().contains(lowerKeyword)) ||
                (contract.getAssetType() != null && contract.getAssetType().toLowerCase().contains(lowerKeyword)) ||
                (contract.getOwner() != null && contract.getOwner().toLowerCase().contains(lowerKeyword));
    }
}
