package io.aurigraph.v11.contracts;

import io.aurigraph.v11.contracts.models.*;
import io.aurigraph.v11.contracts.RicardianContract;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.time.Instant;

/**
 * High-performance in-memory repository for Ricardian contracts
 * Will be replaced with JPA/database implementation in future sprints
 */
@ApplicationScoped
public class ContractRepository {
    
    // High-performance concurrent storage
    private final Map<String, RicardianContract> contracts = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> contractsByType = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> contractsByParty = new ConcurrentHashMap<>();
    private final Map<ContractStatus, Set<String>> contractsByStatus = new ConcurrentHashMap<>();
    
    // Initialize status maps
    public ContractRepository() {
        for (ContractStatus status : ContractStatus.values()) {
            contractsByStatus.put(status, ConcurrentHashMap.newKeySet());
        }
    }

    /**
     * Save or update a contract
     */
    public Uni<RicardianContract> save(RicardianContract contract) {
        return Uni.createFrom().item(() -> {
            // Store the contract
            contracts.put(contract.getContractId(), contract);
            
            // Update indexes for fast lookups
            updateIndexes(contract);
            
            return contract;
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }
    
    /**
     * Persist a contract (alias for save)
     */
    public void persist(RicardianContract contract) {
        save(contract).await().indefinitely();
    }
    
    /**
     * Find contract by contractId (alias for findById)
     */
    public RicardianContract findByContractId(String contractId) {
        return contracts.get(contractId);
    }
    
    /**
     * Find contract by address (alias for findByContractId for executor compatibility)
     */
    public RicardianContract findByAddress(String contractAddress) {
        return findByContractId(contractAddress);
    }
    
    /**
     * Search contracts with criteria
     */
    public List<RicardianContract> search(ContractSearchCriteria criteria) {
        return contracts.values().stream()
            .filter(contract -> matchesCriteria(contract, criteria))
            .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }
    
    private boolean matchesCriteria(RicardianContract contract, ContractSearchCriteria criteria) {
        if (criteria.getContractType() != null && !criteria.getContractType().equals(contract.getContractType())) {
            return false;
        }
        if (criteria.getStatus() != null && !criteria.getStatus().equals(contract.getStatus())) {
            return false;
        }
        if (criteria.getPartyAddress() != null) {
            boolean hasParty = contract.getParties().stream()
                .anyMatch(party -> criteria.getPartyAddress().equals(party.getAddress()));
            if (!hasParty) return false;
        }
        return true;
    }

    /**
     * Find contract by ID
     */
    public Uni<RicardianContract> findById(String contractId) {
        return Uni.createFrom().item(() -> contracts.get(contractId))
            .runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    /**
     * Find all contracts
     */
    public Uni<List<RicardianContract>> findAll() {
        return Uni.createFrom().<List<RicardianContract>>item(() -> new ArrayList<>(contracts.values()))
            .runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    /**
     * Find contracts by type (RWA, Carbon, RealEstate, etc.)
     */
    public Uni<List<RicardianContract>> findByType(String contractType) {
        return Uni.createFrom().<List<RicardianContract>>item(() -> {
            Set<String> contractIds = contractsByType.get(contractType);
            if (contractIds == null) {
                return new ArrayList<>();
            }
            
            List<RicardianContract> result = contractIds.stream()
                .map(contracts::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
            return result;
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    /**
     * Find contracts by party address
     */
    public Uni<List<RicardianContract>> findByParty(String partyAddress) {
        return Uni.createFrom().<List<RicardianContract>>item(() -> {
            Set<String> contractIds = contractsByParty.get(partyAddress);
            if (contractIds == null) {
                return new ArrayList<>();
            }
            
            List<RicardianContract> result = contractIds.stream()
                .map(contracts::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
            return result;
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    /**
     * Find contracts by status
     */
    public Uni<List<RicardianContract>> findByStatus(ContractStatus status) {
        return Uni.createFrom().<List<RicardianContract>>item(() -> {
            Set<String> contractIds = contractsByStatus.get(status);
            if (contractIds == null) {
                return new ArrayList<>();
            }
            
            List<RicardianContract> result = contractIds.stream()
                .map(contracts::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
            return result;
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    /**
     * Find active contracts
     */
    public Uni<List<RicardianContract>> findActiveContracts() {
        return findByStatus(ContractStatus.ACTIVE);
    }

    /**
     * Find contracts created after a specific time
     */
    public Uni<List<RicardianContract>> findCreatedAfter(Instant after) {
        return Uni.createFrom().item(() -> {
            List<RicardianContract> result = contracts.values().stream()
                .filter(contract -> contract.getCreatedAt() != null && 
                                  contract.getCreatedAt().isAfter(after))
                .collect(Collectors.toList());
            return result;
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    /**
     * Find contracts by template ID
     */
    public Uni<List<RicardianContract>> findByTemplateId(String templateId) {
        return Uni.createFrom().item(() -> {
            return contracts.values().stream()
                .filter(contract -> templateId.equals(contract.getTemplateId()))
                .collect(Collectors.toList());
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    /**
     * Count contracts by status
     */
    public Uni<Long> countByStatus(ContractStatus status) {
        return Uni.createFrom().item(() -> {
            Set<String> contractIds = contractsByStatus.get(status);
            return contractIds != null ? (long) contractIds.size() : 0L;
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    /**
     * Count total contracts
     */
    public Uni<Long> count() {
        return Uni.createFrom().item(() -> (long) contracts.size())
            .runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    /**
     * Delete contract by ID
     */
    public Uni<Boolean> deleteById(String contractId) {
        return Uni.createFrom().item(() -> {
            RicardianContract contract = contracts.remove(contractId);
            if (contract != null) {
                removeFromIndexes(contract);
                return true;
            }
            return false;
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    /**
     * Check if contract exists
     */
    public Uni<Boolean> existsById(String contractId) {
        return Uni.createFrom().item(() -> contracts.containsKey(contractId))
            .runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    /**
     * Get repository statistics
     */
    public Uni<RepositoryStats> getStats() {
        return Uni.createFrom().item(() -> {
            Map<ContractStatus, Long> statusCounts = new HashMap<>();
            for (ContractStatus status : ContractStatus.values()) {
                Set<String> contractIds = contractsByStatus.get(status);
                statusCounts.put(status, contractIds != null ? (long) contractIds.size() : 0L);
            }
            
            Map<String, Long> typeCounts = contractsByType.entrySet().stream()
                .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    entry -> (long) entry.getValue().size()
                ));
            
            return new RepositoryStats(
                contracts.size(),
                statusCounts,
                typeCounts,
                contractsByParty.size()
            );
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    /**
     * Search contracts by metadata
     */
    public Uni<List<RicardianContract>> searchByMetadata(String key, String value) {
        return Uni.createFrom().item(() -> {
            return contracts.values().stream()
                .filter(contract -> {
                    Map<String, String> metadata = contract.getMetadata();
                    return metadata != null && value.equals(metadata.get(key));
                })
                .collect(Collectors.toList());
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    /**
     * Find contracts requiring signatures from a specific party
     */
    public Uni<List<RicardianContract>> findPendingSignatures(String partyAddress) {
        return Uni.createFrom().item(() -> {
            return contracts.values().stream()
                .filter(contract -> {
                    // Contract has the party in parties list
                    boolean hasParty = contract.getParties().stream()
                        .anyMatch(party -> partyAddress.equals(party.getAddress()));
                    if (!hasParty) {
                        return false;
                    }
                    
                    // Check if party has already signed
                    boolean alreadySigned = contract.getSignatures().stream()
                        .anyMatch(sig -> partyAddress.equals(sig.getSignerAddress()));
                    
                    // Return contracts where party needs to sign
                    return !alreadySigned && 
                           (contract.getStatus() == ContractStatus.DRAFT || 
                            contract.getStatus() == ContractStatus.PENDING_SIGNATURES);
                })
                .collect(Collectors.toList());
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    // Private helper methods
    
    private void updateIndexes(RicardianContract contract) {
        String contractId = contract.getContractId();
        
        // Update type index
        if (contract.getContractType() != null) {
            contractsByType.computeIfAbsent(contract.getContractType(), 
                k -> ConcurrentHashMap.newKeySet()).add(contractId);
        }
        
        // Update party indexes
        if (contract.getParties() != null) {
            for (ContractParty party : contract.getParties()) {
                contractsByParty.computeIfAbsent(party.getAddress(), 
                    k -> ConcurrentHashMap.newKeySet()).add(contractId);
            }
        }
        
        // Update status index
        if (contract.getStatus() != null) {
            contractsByStatus.get(contract.getStatus()).add(contractId);
        }
    }
    
    private void removeFromIndexes(RicardianContract contract) {
        String contractId = contract.getContractId();
        
        // Remove from type index
        if (contract.getContractType() != null) {
            Set<String> typeContracts = contractsByType.get(contract.getContractType());
            if (typeContracts != null) {
                typeContracts.remove(contractId);
            }
        }
        
        // Remove from party indexes
        if (contract.getParties() != null) {
            for (ContractParty party : contract.getParties()) {
                Set<String> partyContracts = contractsByParty.get(party.getAddress());
                if (partyContracts != null) {
                    partyContracts.remove(contractId);
                }
            }
        }
        
        // Remove from status index
        if (contract.getStatus() != null) {
            contractsByStatus.get(contract.getStatus()).remove(contractId);
        }
    }
}

/**
 * Repository statistics
 */
class RepositoryStats {
    private final int totalContracts;
    private final Map<ContractStatus, Long> statusCounts;
    private final Map<String, Long> typeCounts;
    private final int uniqueParties;

    public RepositoryStats(int totalContracts, Map<ContractStatus, Long> statusCounts, 
                          Map<String, Long> typeCounts, int uniqueParties) {
        this.totalContracts = totalContracts;
        this.statusCounts = statusCounts;
        this.typeCounts = typeCounts;
        this.uniqueParties = uniqueParties;
    }

    // Getters
    public int getTotalContracts() { return totalContracts; }
    public Map<ContractStatus, Long> getStatusCounts() { return statusCounts; }
    public Map<String, Long> getTypeCounts() { return typeCounts; }
    public int getUniqueParties() { return uniqueParties; }

    @Override
    public String toString() {
        return String.format("RepositoryStats{total=%d, uniqueParties=%d, statusCounts=%s, typeCounts=%s}",
            totalContracts, uniqueParties, statusCounts, typeCounts);
    }
}
