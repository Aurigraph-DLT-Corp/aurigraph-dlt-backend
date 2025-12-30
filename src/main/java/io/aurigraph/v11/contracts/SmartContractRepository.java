package io.aurigraph.v11.contracts;

import io.aurigraph.v11.contracts.models.AssetType;
import io.aurigraph.v11.contracts.models.ContractStatus;
import io.aurigraph.v11.contracts.models.SmartContract;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Smart Contract Repository - JPA/Panache Implementation
 *
 * Provides database persistence for SmartContract entities using Panache pattern.
 * Supports CRUD operations, advanced queries, and business-specific lookups.
 *
 * @version 3.8.0 (Phase 2)
 * @author Aurigraph V11 Development Team
 */
@ApplicationScoped
public class SmartContractRepository implements PanacheRepository<SmartContract> {

    // ==================== BASIC CRUD ====================

    /**
     * Find contract by contractId (business identifier)
     */
    public Optional<SmartContract> findByContractId(String contractId) {
        return find("contractId", contractId).firstResultOptional();
    }

    /**
     * Check if contract exists by contractId
     */
    public boolean existsByContractId(String contractId) {
        return count("contractId", contractId) > 0;
    }

    // ==================== OWNER QUERIES ====================

    /**
     * Find all contracts owned by a specific address
     */
    public List<SmartContract> findByOwner(String owner) {
        return find("owner", Sort.descending("createdAt"), owner).list();
    }

    /**
     * Find contracts by owner with pagination
     */
    public List<SmartContract> findByOwner(String owner, Page page) {
        return find("owner", Sort.descending("createdAt"), owner)
                .page(page)
                .list();
    }

    /**
     * Count contracts by owner
     */
    public long countByOwner(String owner) {
        return count("owner", owner);
    }

    // ==================== STATUS QUERIES ====================

    /**
     * Find contracts by status
     */
    public List<SmartContract> findByStatus(ContractStatus status) {
        return find("status", Sort.descending("createdAt"), status).list();
    }

    /**
     * Find contracts by status with pagination
     */
    public List<SmartContract> findByStatus(ContractStatus status, Page page) {
        return find("status", Sort.descending("createdAt"), status)
                .page(page)
                .list();
    }

    /**
     * Count contracts by status
     */
    public long countByStatus(ContractStatus status) {
        return count("status", status);
    }

    /**
     * Find all active contracts (ACTIVE status and not expired)
     */
    public List<SmartContract> findActiveContracts() {
        return find("status = ?1 and (expiresAt is null or expiresAt > ?2)",
                ContractStatus.ACTIVE, Instant.now())
                .list();
    }

    /**
     * Find active contracts with pagination
     */
    public List<SmartContract> findActiveContracts(Page page) {
        return find("status = ?1 and (expiresAt is null or expiresAt > ?2)",
                Sort.descending("lastExecutedAt"),
                ContractStatus.ACTIVE, Instant.now())
                .page(page)
                .list();
    }

    /**
     * Count active contracts
     */
    public long countActiveContracts() {
        return count("status = ?1 and (expiresAt is null or expiresAt > ?2)",
                ContractStatus.ACTIVE, Instant.now());
    }

    /**
     * Find expired contracts (expiresAt < now)
     */
    public List<SmartContract> findExpiredContracts() {
        return find("expiresAt < ?1", Instant.now()).list();
    }

    // ==================== TYPE QUERIES ====================

    /**
     * Find contracts by contract type
     */
    public List<SmartContract> findByContractType(SmartContract.ContractType contractType) {
        return find("contractType", Sort.descending("createdAt"), contractType).list();
    }

    /**
     * Find contracts by contract type with pagination
     */
    public List<SmartContract> findByContractType(SmartContract.ContractType contractType, Page page) {
        return find("contractType", Sort.descending("createdAt"), contractType)
                .page(page)
                .list();
    }

    /**
     * Count contracts by type
     */
    public long countByContractType(SmartContract.ContractType contractType) {
        return count("contractType", contractType);
    }

    // ==================== RWA (Real-World Asset) QUERIES ====================

    /**
     * Find all RWA contracts
     */
    public List<SmartContract> findRWAContracts() {
        return find("isRWA = true", Sort.descending("createdAt")).list();
    }

    /**
     * Find RWA contracts by asset type
     */
    public List<SmartContract> findRWAContractsByAssetType(AssetType assetType) {
        return find("isRWA = true and assetType = ?1",
                Sort.descending("createdAt"), assetType)
                .list();
    }

    /**
     * Find contracts by assetId
     */
    public Optional<SmartContract> findByAssetId(String assetId) {
        return find("assetId", assetId).firstResultOptional();
    }

    /**
     * Find all digital twin contracts
     */
    public List<SmartContract> findDigitalTwinContracts() {
        return find("isDigitalTwin = true", Sort.descending("createdAt")).list();
    }

    /**
     * Count RWA contracts
     */
    public long countRWAContracts() {
        return count("isRWA = true");
    }

    // ==================== TEMPLATE QUERIES ====================

    /**
     * Find contracts by template ID
     */
    public List<SmartContract> findByTemplateId(String templateId) {
        return find("templateId", Sort.descending("createdAt"), templateId).list();
    }

    /**
     * Find all template contracts
     */
    public List<SmartContract> findTemplateContracts() {
        return find("contractType", SmartContract.ContractType.TEMPLATE).list();
    }

    /**
     * Find contracts by parent contract ID
     */
    public List<SmartContract> findByParentContractId(String parentContractId) {
        return find("parentContractId", Sort.descending("createdAt"), parentContractId).list();
    }

    // ==================== VERIFICATION & SECURITY ====================

    /**
     * Find verified contracts
     */
    public List<SmartContract> findVerifiedContracts() {
        return find("isVerified = true", Sort.descending("createdAt")).list();
    }

    /**
     * Find contracts by security audit status
     */
    public List<SmartContract> findBySecurityAuditStatus(String auditStatus) {
        return find("securityAuditStatus", Sort.descending("updatedAt"), auditStatus).list();
    }

    /**
     * Find contracts pending verification
     */
    public List<SmartContract> findPendingVerification() {
        return find("isVerified = false and status = ?1",
                Sort.descending("createdAt"),
                ContractStatus.DEPLOYED)
                .list();
    }

    // ==================== TIME-BASED QUERIES ====================

    /**
     * Find contracts created after a specific timestamp
     */
    public List<SmartContract> findCreatedAfter(Instant after) {
        return find("createdAt > ?1", Sort.descending("createdAt"), after).list();
    }

    /**
     * Find contracts created between timestamps
     */
    public List<SmartContract> findCreatedBetween(Instant start, Instant end) {
        return find("createdAt >= ?1 and createdAt <= ?2",
                Sort.descending("createdAt"), start, end)
                .list();
    }

    /**
     * Find contracts deployed in a time range
     */
    public List<SmartContract> findDeployedBetween(Instant start, Instant end) {
        return find("deployedAt >= ?1 and deployedAt <= ?2",
                Sort.descending("deployedAt"), start, end)
                .list();
    }

    /**
     * Find contracts expiring soon (within next N seconds)
     */
    public List<SmartContract> findExpiringSoon(long secondsAhead) {
        Instant now = Instant.now();
        Instant future = now.plusSeconds(secondsAhead);
        return find("status = ?1 and expiresAt > ?2 and expiresAt <= ?3",
                Sort.ascending("expiresAt"),
                ContractStatus.ACTIVE, now, future)
                .list();
    }

    // ==================== PARTY & MULTI-PARTY QUERIES ====================

    /**
     * Find contracts where address is a party (using SQL)
     */
    public List<SmartContract> findByPartyAddress(String partyAddress) {
        return find("select c from SmartContract c join c.parties p where p = ?1",
                Sort.descending("createdAt"), partyAddress)
                .list();
    }

    // ==================== FINANCIAL QUERIES ====================

    /**
     * Find contracts by value range
     */
    public List<SmartContract> findByValueRange(BigDecimal minValue, BigDecimal maxValue) {
        return find("value >= ?1 and value <= ?2",
                Sort.descending("value"), minValue, maxValue)
                .list();
    }

    /**
     * Find contracts by currency
     */
    public List<SmartContract> findByCurrency(String currency) {
        return find("currency", Sort.descending("value"), currency).list();
    }

    /**
     * Calculate total value of contracts by status
     */
    public BigDecimal sumValueByStatus(ContractStatus status) {
        return find("status", status)
                .stream()
                .map(SmartContract::getValue)
                .filter(val -> val != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // ==================== EXECUTION METRICS ====================

    /**
     * Find most executed contracts (top N)
     */
    public List<SmartContract> findMostExecuted(int limit) {
        return find("executionCount > 0", Sort.descending("executionCount"))
                .page(Page.ofSize(limit))
                .list();
    }

    /**
     * Find contracts by execution count range
     */
    public List<SmartContract> findByExecutionCountRange(long min, long max) {
        return find("executionCount >= ?1 and executionCount <= ?2",
                Sort.descending("executionCount"), min, max)
                .list();
    }

    /**
     * Find recently executed contracts (within last N seconds)
     */
    public List<SmartContract> findRecentlyExecuted(long secondsAgo) {
        Instant since = Instant.now().minusSeconds(secondsAgo);
        return find("lastExecutedAt > ?1", Sort.descending("lastExecutedAt"), since).list();
    }

    // ==================== COMPLIANCE QUERIES ====================

    /**
     * Find contracts by jurisdiction
     */
    public List<SmartContract> findByJurisdiction(String jurisdiction) {
        return find("jurisdiction", Sort.descending("createdAt"), jurisdiction).list();
    }

    /**
     * Find contracts by compliance status
     */
    public List<SmartContract> findByComplianceStatus(String complianceStatus) {
        return find("complianceStatus", Sort.descending("updatedAt"), complianceStatus).list();
    }

    /**
     * Find KYC-verified contracts
     */
    public List<SmartContract> findKYCVerified() {
        return find("kycVerified = true", Sort.descending("createdAt")).list();
    }

    /**
     * Find AML-checked contracts
     */
    public List<SmartContract> findAMLChecked() {
        return find("amlChecked = true", Sort.descending("createdAt")).list();
    }

    /**
     * Find contracts requiring compliance checks
     */
    public List<SmartContract> findRequiringCompliance() {
        return find("(kycVerified = false or amlChecked = false) and status = ?1",
                Sort.descending("createdAt"),
                ContractStatus.DEPLOYED)
                .list();
    }

    // ==================== TAG QUERIES ====================

    /**
     * Find contracts by tag (using SQL join)
     */
    public List<SmartContract> findByTag(String tag) {
        return find("select c from SmartContract c join c.tags t where t = ?1",
                Sort.descending("createdAt"), tag)
                .list();
    }

    // ==================== SEARCH & ADVANCED QUERIES ====================

    /**
     * Search contracts by name (case-insensitive)
     */
    public List<SmartContract> searchByName(String namePattern) {
        return find("lower(name) like lower(?1)", Sort.descending("createdAt"), "%" + namePattern + "%").list();
    }

    /**
     * Search contracts by description (case-insensitive)
     */
    public List<SmartContract> searchByDescription(String descriptionPattern) {
        return find("lower(description) like lower(?1)", Sort.descending("createdAt"), "%" + descriptionPattern + "%").list();
    }

    /**
     * Advanced search with multiple criteria
     */
    public List<SmartContract> search(ContractSearchCriteria criteria) {
        StringBuilder query = new StringBuilder("1=1");
        Map<String, Object> params = Map.of();

        if (criteria.owner != null) {
            query.append(" and owner = :owner");
            params = Map.of("owner", criteria.owner);
        }
        if (criteria.status != null) {
            query.append(" and status = :status");
            params = Map.of("status", criteria.status);
        }
        if (criteria.contractType != null) {
            query.append(" and contractType = :contractType");
            params = Map.of("contractType", criteria.contractType);
        }
        if (criteria.isRWA != null) {
            query.append(" and isRWA = :isRWA");
            params = Map.of("isRWA", criteria.isRWA);
        }

        return find(query.toString(), Sort.descending("createdAt"), params).list();
    }

    // ==================== STATISTICS ====================

    /**
     * Get contract statistics
     */
    public ContractStatistics getStatistics() {
        long total = count();
        long active = countByStatus(ContractStatus.ACTIVE);
        long deployed = countByStatus(ContractStatus.DEPLOYED);
        long completed = countByStatus(ContractStatus.COMPLETED);
        long rwaCount = countRWAContracts();

        BigDecimal totalValue = listAll()
                .stream()
                .map(SmartContract::getValue)
                .filter(val -> val != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new ContractStatistics(total, active, deployed, completed, rwaCount, totalValue);
    }

    // ==================== BULK OPERATIONS ====================

    /**
     * Delete expired contracts (transactional)
     */
    @Transactional
    public long deleteExpiredContracts() {
        return delete("expiresAt < ?1 and status != ?2",
                Instant.now(), ContractStatus.ACTIVE);
    }

    /**
     * Update contract status in bulk
     */
    @Transactional
    public long updateStatusBulk(ContractStatus fromStatus, ContractStatus toStatus) {
        return update("status = ?1 where status = ?2", toStatus, fromStatus);
    }

    // ==================== DATA MODELS ====================

    /**
     * Search criteria for advanced contract search
     */
    public static class ContractSearchCriteria {
        public String owner;
        public ContractStatus status;
        public SmartContract.ContractType contractType;
        public Boolean isRWA;
        public AssetType assetType;
        public String jurisdiction;
        public Boolean kycVerified;
        public Boolean amlChecked;
        public Instant createdAfter;
        public Instant createdBefore;

        // Builder pattern methods
        public ContractSearchCriteria withOwner(String owner) {
            this.owner = owner;
            return this;
        }

        public ContractSearchCriteria withStatus(ContractStatus status) {
            this.status = status;
            return this;
        }

        public ContractSearchCriteria withContractType(SmartContract.ContractType contractType) {
            this.contractType = contractType;
            return this;
        }

        public ContractSearchCriteria withIsRWA(Boolean isRWA) {
            this.isRWA = isRWA;
            return this;
        }
    }

    /**
     * Contract statistics record
     */
    public record ContractStatistics(
            long total,
            long active,
            long deployed,
            long completed,
            long rwaContracts,
            BigDecimal totalValue
    ) {}
}
