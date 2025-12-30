package io.aurigraph.v11.transaction.services;

import io.aurigraph.v11.transaction.models.Transaction;
import io.aurigraph.v11.transaction.repositories.TransactionRepository;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
@Transactional
public class TransactionService {

    @Inject
    TransactionRepository transactionRepository;

    @Inject
    MonitoringService monitoringService;

    @Inject
    AIOptimizationService aiOptimizationService;

    public Transaction submitTransaction(
        String userId,
        String fromAddress,
        String toAddress,
        BigDecimal amount,
        Transaction.TransactionType type,
        Long gasLimit,
        BigDecimal gasPrice,
        String data
    ) throws Exception {
        Log.infof(
            "Submitting transaction - User: %s, From: %s, To: %s, Amount: %s",
            userId,
            fromAddress,
            toAddress,
            amount
        );

        // Generate transaction hash
        String txHash = generateTxHash();

        // Calculate fees
        BigDecimal totalFee = gasLimit != null && gasPrice != null
            ? BigDecimal.valueOf(gasLimit).multiply(gasPrice)
            : amount.multiply(BigDecimal.valueOf(0.001));

        // Create transaction
        Transaction transaction = Transaction.builder()
            .txHash(txHash)
            .userId(userId)
            .fromAddress(fromAddress)
            .toAddress(toAddress)
            .amount(amount)
            .transactionType(type)
            .status(Transaction.TransactionStatus.PENDING)
            .gasLimit(gasLimit)
            .gasPrice(gasPrice)
            .totalFee(totalFee)
            .data(data)
            .build();

        transactionRepository.persist(transaction);

        // Record metrics
        monitoringService.recordTransactionSubmitted(transaction);

        // Calculate AI optimization score
        BigDecimal optimizationScore = aiOptimizationService.calculateOptimizationScore(transaction);
        transaction.optimizationScore = optimizationScore;
        transactionRepository.persist(transaction);

        Log.infof("Transaction submitted: %s", txHash);

        return transaction;
    }

    public void validateTransaction(String txHash) throws Exception {
        Log.infof("Validating transaction: %s", txHash);

        Transaction transaction = transactionRepository.findByTxHash(txHash)
            .orElseThrow(() -> new IllegalArgumentException("Transaction not found"));

        transaction.status = Transaction.TransactionStatus.VALIDATING;
        transactionRepository.persist(transaction);

        // Perform validation logic
        validateTransactionLogic(transaction);

        transaction.status = Transaction.TransactionStatus.CONFIRMED;
        transaction.submittedAt = LocalDateTime.now();
        transactionRepository.persist(transaction);

        monitoringService.recordTransactionConfirmed(transaction);
        Log.infof("Transaction validated: %s", txHash);
    }

    private void validateTransactionLogic(Transaction transaction) throws Exception {
        // Validate amount is positive
        if (transaction.amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Invalid transaction amount");
        }

        // Validate addresses
        if (transaction.fromAddress == null || transaction.fromAddress.isEmpty()) {
            throw new IllegalArgumentException("Invalid from address");
        }

        // Additional validation logic
    }

    public void confirmTransaction(String txHash, Long blockNumber, String blockHash) throws Exception {
        Log.infof("Confirming transaction: %s in block: %d", txHash, blockNumber);

        Transaction transaction = transactionRepository.findByTxHash(txHash)
            .orElseThrow(() -> new IllegalArgumentException("Transaction not found"));

        transaction.status = Transaction.TransactionStatus.CONFIRMED;
        transaction.blockNumber = blockNumber;
        transaction.blockHash = blockHash;
        transaction.confirmationCount = 1;
        transaction.confirmedAt = LocalDateTime.now();

        transactionRepository.persist(transaction);
        monitoringService.recordTransactionConfirmed(transaction);
    }

    public void finalizeTransaction(String txHash) throws Exception {
        Log.infof("Finalizing transaction: %s", txHash);

        Transaction transaction = transactionRepository.findByTxHash(txHash)
            .orElseThrow(() -> new IllegalArgumentException("Transaction not found"));

        transaction.status = Transaction.TransactionStatus.FINALIZED;
        transaction.finalizedAt = LocalDateTime.now();

        if (transaction.submittedAt != null) {
            long finalityMs = java.time.temporal.ChronoUnit.MILLIS.between(
                transaction.submittedAt,
                transaction.finalizedAt
            );
            transaction.finalityTimeMs = finalityMs;
            monitoringService.recordFinality(finalityMs);
        }

        transactionRepository.persist(transaction);
        monitoringService.recordTransactionFinalized(transaction);
    }

    public void failTransaction(String txHash, String errorCode, String errorMessage) throws Exception {
        Log.warnf("Failing transaction: %s, error: %s", txHash, errorCode);

        Transaction transaction = transactionRepository.findByTxHash(txHash)
            .orElseThrow(() -> new IllegalArgumentException("Transaction not found"));

        transaction.status = Transaction.TransactionStatus.FAILED;
        transaction.errorCode = errorCode;
        transaction.errorMessage = errorMessage;
        transaction.updatedAt = LocalDateTime.now();

        transactionRepository.persist(transaction);
        monitoringService.recordTransactionFailed(transaction);
    }

    public Optional<Transaction> getTransaction(String txHash) {
        return transactionRepository.findByTxHash(txHash);
    }

    public Optional<Transaction> getTransactionById(Long id) {
        return transactionRepository.findByIdOptional(id);
    }

    public List<Transaction> getUserTransactions(String userId) {
        return transactionRepository.findByUserId(userId);
    }

    public List<Transaction> getPendingTransactions() {
        return transactionRepository.findPendingTransactions();
    }

    public List<Transaction> getConfirmedTransactions() {
        return transactionRepository.findConfirmedTransactions();
    }

    public long getPendingTransactionCount() {
        return transactionRepository.countPendingTransactions();
    }

    public List<Transaction> getTransactionsByDateRange(
        LocalDateTime startDate,
        LocalDateTime endDate
    ) {
        return transactionRepository.findByCreatedAtRange(startDate, endDate);
    }

    private String generateTxHash() {
        return "0x" + UUID.randomUUID().toString().replace("-", "").substring(0, 64);
    }

    public void incrementConfirmationCount(String txHash) {
        Transaction transaction = transactionRepository.findByTxHash(txHash)
            .orElseThrow(() -> new IllegalArgumentException("Transaction not found"));

        transaction.confirmationCount = (transaction.confirmationCount != null ? transaction.confirmationCount : 0) + 1;
        transactionRepository.persist(transaction);
    }
}
