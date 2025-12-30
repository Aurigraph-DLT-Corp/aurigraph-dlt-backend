package io.aurigraph.v11.transaction.repositories;

import io.aurigraph.v11.transaction.models.Transaction;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class TransactionRepository implements PanacheRepository<Transaction> {

    public Optional<Transaction> findByTxHash(String txHash) {
        return find("txHash", txHash).firstResultOptional();
    }

    public List<Transaction> findByUserId(String userId) {
        return list("userId", userId);
    }

    public List<Transaction> findByStatus(Transaction.TransactionStatus status) {
        return list("status", status);
    }

    public List<Transaction> findPendingTransactions() {
        return list("status", Transaction.TransactionStatus.PENDING);
    }

    public List<Transaction> findConfirmedTransactions() {
        return list(
            "status in ?1",
            List.of(Transaction.TransactionStatus.CONFIRMED, Transaction.TransactionStatus.FINALIZED)
        );
    }

    public List<Transaction> findFailedTransactions() {
        return list(
            "status in ?1",
            List.of(Transaction.TransactionStatus.FAILED, Transaction.TransactionStatus.REJECTED)
        );
    }

    public List<Transaction> findByUserIdAndStatus(
        String userId,
        Transaction.TransactionStatus status
    ) {
        return list("userId = ?1 and status = ?2", userId, status);
    }

    public List<Transaction> findByTransactionType(Transaction.TransactionType type) {
        return list("transactionType", type);
    }

    public List<Transaction> findByBlockNumber(Long blockNumber) {
        return list("blockNumber", blockNumber);
    }

    public List<Transaction> findByCreatedAtRange(LocalDateTime startDate, LocalDateTime endDate) {
        return list(
            "createdAt >= ?1 and createdAt <= ?2",
            startDate,
            endDate
        );
    }

    public List<Transaction> findByUserIdAndDateRange(
        String userId,
        LocalDateTime startDate,
        LocalDateTime endDate
    ) {
        return list(
            "userId = ?1 and createdAt >= ?2 and createdAt <= ?3",
            userId,
            startDate,
            endDate
        );
    }

    public long countPendingTransactions() {
        return count("status", Transaction.TransactionStatus.PENDING);
    }

    public long countByUserId(String userId) {
        return count("userId", userId);
    }

    public List<Transaction> findConfirmedTransactionsByUserId(String userId) {
        return list(
            "userId = ?1 and status in ?2",
            userId,
            List.of(Transaction.TransactionStatus.CONFIRMED, Transaction.TransactionStatus.FINALIZED)
        );
    }

    public List<Transaction> findUnconfirmedTransactionsByUserId(String userId) {
        return list(
            "userId = ?1 and status in ?2",
            userId,
            List.of(
                Transaction.TransactionStatus.PENDING,
                Transaction.TransactionStatus.VALIDATING
            )
        );
    }

    public List<Transaction> findTransactionsSinceBlock(Long blockNumber) {
        return list("blockNumber > ?1", blockNumber);
    }

    public Long getMaxBlockNumber() {
        return find("select max(blockNumber) from Transaction")
            .project(Long.class)
            .firstResultOptional()
            .orElse(0L);
    }

    public List<Transaction> findRecentFailedTransactions(int limit) {
        return find("status in ?1 order by createdAt desc", List.of(
            Transaction.TransactionStatus.FAILED,
            Transaction.TransactionStatus.REJECTED
        ))
            .page(0, limit)
            .list();
    }
}
