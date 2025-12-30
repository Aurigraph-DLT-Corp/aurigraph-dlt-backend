package io.aurigraph.v11.contracts;

import io.aurigraph.v11.contracts.models.ActiveContract;
import io.aurigraph.v11.contracts.models.ActiveContract.ActiveContractStatus;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Active Contract Repository - JPA/Panache Implementation
 *
 * Provides database persistence for ActiveContract entities.
 * Supports contract lifecycle tracking, party management, and event queries.
 *
 * @version 3.8.0 (Phase 2 Day 10)
 * @author Aurigraph V11 Development Team
 */
@ApplicationScoped
public class ActiveContractRepository implements PanacheRepository<ActiveContract> {

    // ==================== BASIC QUERIES ====================

    public Optional<ActiveContract> findByContractId(String contractId) {
        return find("contractId", contractId).firstResultOptional();
    }

    public boolean existsByContractId(String contractId) {
        return count("contractId", contractId) > 0;
    }

    public List<ActiveContract> findByCreator(String creatorAddress) {
        return find("creatorAddress", Sort.descending("createdAt"), creatorAddress).list();
    }

    public List<ActiveContract> findByCreator(String creatorAddress, Page page) {
        return find("creatorAddress", Sort.descending("createdAt"), creatorAddress)
                .page(page)
                .list();
    }

    // ==================== STATUS QUERIES ====================

    public List<ActiveContract> findByStatus(ActiveContractStatus status) {
        return find("status", Sort.descending("createdAt"), status).list();
    }

    public List<ActiveContract> findByStatus(ActiveContractStatus status, Page page) {
        return find("status", Sort.descending("createdAt"), status)
                .page(page)
                .list();
    }

    public long countByStatus(ActiveContractStatus status) {
        return count("status", status);
    }

    public List<ActiveContract> findActiveContracts() {
        return find("status = ?1 and (expiresAt is null or expiresAt > ?2)",
                Sort.descending("lastEventAt"),
                ActiveContractStatus.ACTIVE,
                Instant.now())
                .list();
    }

    public List<ActiveContract> findPendingContracts() {
        return find("status", Sort.descending("createdAt"), ActiveContractStatus.PENDING).list();
    }

    public List<ActiveContract> findCompletedContracts() {
        return find("status", Sort.descending("completedAt"), ActiveContractStatus.COMPLETED).list();
    }

    // ==================== PARTY QUERIES ====================

    public List<ActiveContract> findByParty(String partyAddress) {
        return find("select c from ActiveContract c join c.parties p where p = ?1",
                Sort.descending("createdAt"), partyAddress)
                .list();
    }

    public List<ActiveContract> findActiveByParty(String partyAddress) {
        return find("select c from ActiveContract c join c.parties p where p = ?1 and c.status = ?2",
                Sort.descending("lastEventAt"),
                partyAddress,
                ActiveContractStatus.ACTIVE)
                .list();
    }

    public long countByParty(String partyAddress) {
        Long count = find("select count(c) from ActiveContract c join c.parties p where p = ?1", partyAddress)
                .project(Long.class)
                .firstResult();
        return count != null ? count : 0L;
    }

    // ==================== TIME-BASED QUERIES ====================

    public List<ActiveContract> findCreatedAfter(Instant after) {
        return find("createdAt > ?1", Sort.descending("createdAt"), after).list();
    }

    public List<ActiveContract> findCreatedBetween(Instant start, Instant end) {
        return find("createdAt >= ?1 and createdAt <= ?2",
                Sort.descending("createdAt"), start, end)
                .list();
    }

    public List<ActiveContract> findExpiringBefore(Instant before) {
        return find("expiresAt <= ?1 and status = ?2",
                Sort.ascending("expiresAt"),
                before,
                ActiveContractStatus.ACTIVE)
                .list();
    }

    public List<ActiveContract> findExpiredContracts() {
        return find("expiresAt < ?1 and status = ?2",
                Sort.descending("expiresAt"),
                Instant.now(),
                ActiveContractStatus.ACTIVE)
                .list();
    }

    public List<ActiveContract> findRecentlyActive(long secondsAgo) {
        Instant since = Instant.now().minusSeconds(secondsAgo);
        return find("lastEventAt > ?1 and status = ?2",
                Sort.descending("lastEventAt"),
                since,
                ActiveContractStatus.ACTIVE)
                .list();
    }

    // ==================== TYPE QUERIES ====================

    public List<ActiveContract> findByType(String contractType) {
        return find("contractType", Sort.descending("createdAt"), contractType).list();
    }

    public List<ActiveContract> findByTypeAndStatus(String contractType, ActiveContractStatus status) {
        return find("contractType = ?1 and status = ?2",
                Sort.descending("createdAt"),
                contractType,
                status)
                .list();
    }

    public long countByType(String contractType) {
        return count("contractType", contractType);
    }

    // ==================== EXECUTION QUERIES ====================

    public List<ActiveContract> findMostExecuted(int limit) {
        return find("executionCount > 0", Sort.descending("executionCount"))
                .page(Page.ofSize(limit))
                .list();
    }

    public List<ActiveContract> findRecentlyExecuted(long secondsAgo) {
        Instant since = Instant.now().minusSeconds(secondsAgo);
        return find("lastExecutionAt > ?1", Sort.descending("lastExecutionAt"), since).list();
    }

    public List<ActiveContract> findByExecutionStatus(String executionStatus) {
        return find("lastExecutionStatus", Sort.descending("lastExecutionAt"), executionStatus).list();
    }

    // ==================== NOTIFICATION QUERIES ====================

    public List<ActiveContract> findWithNotificationsEnabled() {
        return find("notificationEnabled = true and status = ?1",
                Sort.descending("lastEventAt"),
                ActiveContractStatus.ACTIVE)
                .list();
    }

    // ==================== SEARCH QUERIES ====================

    public List<ActiveContract> searchByName(String namePattern) {
        return find("lower(name) like lower(?1)",
                Sort.descending("createdAt"), "%" + namePattern + "%")
                .list();
    }

    public List<ActiveContract> searchByDescription(String descPattern) {
        return find("lower(description) like lower(?1)",
                Sort.descending("createdAt"), "%" + descPattern + "%")
                .list();
    }

    // ==================== STATISTICS ====================

    public ContractStatistics getStatistics() {
        long total = count();
        long pending = countByStatus(ActiveContractStatus.PENDING);
        long active = countByStatus(ActiveContractStatus.ACTIVE);
        long paused = countByStatus(ActiveContractStatus.PAUSED);
        long completed = countByStatus(ActiveContractStatus.COMPLETED);
        long terminated = countByStatus(ActiveContractStatus.TERMINATED);

        Long totalExecutions = find("select sum(executionCount) from ActiveContract")
                .project(Long.class)
                .firstResult();

        Long totalEvents = find("select sum(eventCount) from ActiveContract")
                .project(Long.class)
                .firstResult();

        return new ContractStatistics(
                total,
                pending,
                active,
                paused,
                completed,
                terminated,
                totalExecutions != null ? totalExecutions : 0L,
                totalEvents != null ? totalEvents : 0L
        );
    }

    // ==================== CLEANUP ====================

    public long deleteCompletedBefore(Instant before) {
        return delete("status = ?1 and completedAt < ?2", ActiveContractStatus.COMPLETED, before);
    }

    public long deleteTerminatedBefore(Instant before) {
        return delete("status = ?1 and terminatedAt < ?2", ActiveContractStatus.TERMINATED, before);
    }

    // ==================== DATA MODELS ====================

    public record ContractStatistics(
            long totalContracts,
            long pendingContracts,
            long activeContracts,
            long pausedContracts,
            long completedContracts,
            long terminatedContracts,
            long totalExecutions,
            long totalEvents
    ) {}
}
