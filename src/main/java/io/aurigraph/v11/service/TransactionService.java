package io.aurigraph.v11.service;

import io.aurigraph.v11.proto.*;
import java.util.List;

/**
 * Transaction Service Interface
 *
 * Defines contract for transaction processing operations used by gRPC and REST services.
 * This is a temporary interface to support PHASE 4C-1 gRPC implementation.
 * Will be replaced with full service implementation in PHASE 4C-3.
 */
public interface TransactionService {

    /**
     * Submit a single transaction
     */
    String submitTransaction(Transaction transaction, boolean prioritize) throws Exception;

    /**
     * Get transaction by hash
     */
    Transaction getTransaction(String txnHash) throws Exception;

    /**
     * Get transaction status
     */
    TransactionStatus getTransactionStatus(String txnHash) throws Exception;

    /**
     * Get transaction receipt
     */
    TransactionReceipt getTransactionReceipt(String txnHash) throws Exception;

    /**
     * Cancel a pending transaction
     */
    boolean cancelTransaction(String txnHash) throws Exception;

    /**
     * Resend transaction with new gas price
     */
    String resendTransaction(String originalTxnHash, double newGasPrice) throws Exception;

    /**
     * Estimate gas for transaction
     */
    double estimateGas(String fromAddress, String toAddress, String data) throws Exception;

    /**
     * Validate transaction signature
     */
    boolean validateSignature(String signature, byte[] dataHash) throws Exception;

    /**
     * Get pending transactions
     */
    List<Transaction> getPendingTransactions(int limit, boolean sortByFee) throws Exception;

    /**
     * Get transaction history for address
     */
    List<Transaction> getTransactionHistory(String address, int limit, int offset) throws Exception;

    /**
     * Get count of pending transactions
     */
    int getPendingCount() throws Exception;

    /**
     * Get average gas price
     */
    double getAverageGasPrice() throws Exception;
}
