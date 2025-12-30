package io.aurigraph.v11.repositories;

import io.aurigraph.v11.models.Transaction;
import io.aurigraph.v11.models.TransactionStatus;
import io.aurigraph.v11.models.TransactionType;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Page;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Transaction Repository
 *
 * Panache repository for Transaction entity providing database operations.
 * Part of Sprint 9 - Story 1 (AV11-051)
 *
 * @author Claude Code - Backend Development Agent (BDA)
 * @version 11.0.0
 * @since Sprint 9
 */
@ApplicationScoped
public class TransactionRepository implements PanacheRepositoryBase<Transaction, String> {

    /**
     * Find transaction by hash
     */
    public Optional<Transaction> findByHash(String hash) {
        return find("hash", hash).firstResultOptional();
    }

    /**
     * Find transactions by status
     */
    public List<Transaction> findByStatus(TransactionStatus status, int limit, int offset) {
        return find("status", status)
                .page(Page.of(offset / limit, limit))
                .list();
    }

    /**
     * Find transactions by type
     */
    public List<Transaction> findByType(TransactionType type, int limit, int offset) {
        return find("type", type)
                .page(Page.of(offset / limit, limit))
                .list();
    }

    /**
     * Find transactions by from address
     */
    public List<Transaction> findByFromAddress(String fromAddress, int limit, int offset) {
        return find("fromAddress", fromAddress)
                .page(Page.of(offset / limit, limit))
                .list();
    }

    /**
     * Find transactions by to address
     */
    public List<Transaction> findByToAddress(String toAddress, int limit, int offset) {
        return find("toAddress", toAddress)
                .page(Page.of(offset / limit, limit))
                .list();
    }

    /**
     * Find transactions by address (from or to)
     */
    public List<Transaction> findByAddress(String address, int limit, int offset) {
        return find("fromAddress = ?1 or toAddress = ?1", address)
                .page(Page.of(offset / limit, limit))
                .list();
    }

    /**
     * Find transactions by block hash
     */
    public List<Transaction> findByBlockHash(String blockHash) {
        return find("blockHash", blockHash).list();
    }

    /**
     * Find transactions by block height
     */
    public List<Transaction> findByBlockHeight(Long blockHeight) {
        return find("blockHeight", blockHeight).list();
    }

    /**
     * Find transactions in time range
     */
    public List<Transaction> findInTimeRange(Instant startTime, Instant endTime, int limit, int offset) {
        return find("timestamp >= ?1 and timestamp <= ?2", startTime, endTime)
                .page(Page.of(offset / limit, limit))
                .list();
    }

    /**
     * Find pending transactions ordered by priority
     */
    public List<Transaction> findPendingByPriority(int limit) {
        return find("status = ?1 order by priority desc, timestamp asc", TransactionStatus.PENDING)
                .page(Page.ofSize(limit))
                .list();
    }

    /**
     * Count transactions by status
     */
    public long countByStatus(TransactionStatus status) {
        return count("status", status);
    }

    /**
     * Count transactions by type
     */
    public long countByType(TransactionType type) {
        return count("type", type);
    }

    /**
     * Count transactions by address
     */
    public long countByAddress(String address) {
        return count("fromAddress = ?1 or toAddress = ?1", address);
    }

    /**
     * Find all transactions with pagination
     */
    public List<Transaction> findAllPaginated(int limit, int offset) {
        return findAll()
                .page(Page.of(offset / limit, limit))
                .list();
    }

    /**
     * Count all transactions
     */
    public long countAll() {
        return count();
    }
}
