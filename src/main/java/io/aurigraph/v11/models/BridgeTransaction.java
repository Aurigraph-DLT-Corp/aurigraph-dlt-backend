package io.aurigraph.v11.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.List;

/**
 * Bridge Transaction Model
 * Represents cross-chain transfer transaction details
 *
 * Used by /api/v11/bridge/history endpoint
 *
 * @author Aurigraph V11
 * @version 11.3.0
 */
public class BridgeTransaction {

    @JsonProperty("transaction_id")
    private String transactionId;

    @JsonProperty("bridge_id")
    private String bridgeId;

    @JsonProperty("source_chain")
    private String sourceChain;

    @JsonProperty("target_chain")
    private String targetChain;

    @JsonProperty("user_address")
    private String userAddress;

    @JsonProperty("asset")
    private AssetInfo asset;

    @JsonProperty("amount")
    private String amount;

    @JsonProperty("amount_usd")
    private double amountUsd;

    @JsonProperty("status")
    private String status; // "pending", "processing", "completed", "failed", "stuck"

    @JsonProperty("timestamps")
    private TransactionTimestamps timestamps;

    @JsonProperty("source_transaction")
    private ChainTransaction sourceTransaction;

    @JsonProperty("target_transaction")
    private ChainTransaction targetTransaction;

    @JsonProperty("fees")
    private TransactionFees fees;

    @JsonProperty("confirmations")
    private ConfirmationInfo confirmations;

    @JsonProperty("error")
    private TransactionError error;

    // Constructor
    public BridgeTransaction() {}

    // Getters and Setters
    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }

    public String getBridgeId() { return bridgeId; }
    public void setBridgeId(String bridgeId) { this.bridgeId = bridgeId; }

    public String getSourceChain() { return sourceChain; }
    public void setSourceChain(String sourceChain) { this.sourceChain = sourceChain; }

    public String getTargetChain() { return targetChain; }
    public void setTargetChain(String targetChain) { this.targetChain = targetChain; }

    public String getUserAddress() { return userAddress; }
    public void setUserAddress(String userAddress) { this.userAddress = userAddress; }

    public AssetInfo getAsset() { return asset; }
    public void setAsset(AssetInfo asset) { this.asset = asset; }

    public String getAmount() { return amount; }
    public void setAmount(String amount) { this.amount = amount; }

    public double getAmountUsd() { return amountUsd; }
    public void setAmountUsd(double amountUsd) { this.amountUsd = amountUsd; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public TransactionTimestamps getTimestamps() { return timestamps; }
    public void setTimestamps(TransactionTimestamps timestamps) { this.timestamps = timestamps; }

    public ChainTransaction getSourceTransaction() { return sourceTransaction; }
    public void setSourceTransaction(ChainTransaction sourceTransaction) { this.sourceTransaction = sourceTransaction; }

    public ChainTransaction getTargetTransaction() { return targetTransaction; }
    public void setTargetTransaction(ChainTransaction targetTransaction) { this.targetTransaction = targetTransaction; }

    public TransactionFees getFees() { return fees; }
    public void setFees(TransactionFees fees) { this.fees = fees; }

    public ConfirmationInfo getConfirmations() { return confirmations; }
    public void setConfirmations(ConfirmationInfo confirmations) { this.confirmations = confirmations; }

    public TransactionError getError() { return error; }
    public void setError(TransactionError error) { this.error = error; }

    /**
     * Asset Information
     */
    public static class AssetInfo {
        @JsonProperty("symbol")
        private String symbol;

        @JsonProperty("name")
        private String name;

        @JsonProperty("contract_address")
        private String contractAddress;

        @JsonProperty("decimals")
        private int decimals;

        public AssetInfo() {}

        public AssetInfo(String symbol, String name, String contractAddress, int decimals) {
            this.symbol = symbol;
            this.name = name;
            this.contractAddress = contractAddress;
            this.decimals = decimals;
        }

        // Getters and Setters
        public String getSymbol() { return symbol; }
        public void setSymbol(String symbol) { this.symbol = symbol; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getContractAddress() { return contractAddress; }
        public void setContractAddress(String contractAddress) { this.contractAddress = contractAddress; }

        public int getDecimals() { return decimals; }
        public void setDecimals(int decimals) { this.decimals = decimals; }
    }

    /**
     * Transaction Timestamps
     */
    public static class TransactionTimestamps {
        @JsonProperty("initiated")
        private Instant initiated;

        @JsonProperty("source_confirmed")
        private Instant sourceConfirmed;

        @JsonProperty("bridge_started")
        private Instant bridgeStarted;

        @JsonProperty("bridge_completed")
        private Instant bridgeCompleted;

        @JsonProperty("target_confirmed")
        private Instant targetConfirmed;

        @JsonProperty("total_duration_seconds")
        private long totalDurationSeconds;

        public TransactionTimestamps() {}

        // Getters and Setters
        public Instant getInitiated() { return initiated; }
        public void setInitiated(Instant initiated) { this.initiated = initiated; }

        public Instant getSourceConfirmed() { return sourceConfirmed; }
        public void setSourceConfirmed(Instant sourceConfirmed) { this.sourceConfirmed = sourceConfirmed; }

        public Instant getBridgeStarted() { return bridgeStarted; }
        public void setBridgeStarted(Instant bridgeStarted) { this.bridgeStarted = bridgeStarted; }

        public Instant getBridgeCompleted() { return bridgeCompleted; }
        public void setBridgeCompleted(Instant bridgeCompleted) { this.bridgeCompleted = bridgeCompleted; }

        public Instant getTargetConfirmed() { return targetConfirmed; }
        public void setTargetConfirmed(Instant targetConfirmed) { this.targetConfirmed = targetConfirmed; }

        public long getTotalDurationSeconds() { return totalDurationSeconds; }
        public void setTotalDurationSeconds(long totalDurationSeconds) { this.totalDurationSeconds = totalDurationSeconds; }
    }

    /**
     * Chain Transaction Details
     */
    public static class ChainTransaction {
        @JsonProperty("transaction_hash")
        private String transactionHash;

        @JsonProperty("block_number")
        private long blockNumber;

        @JsonProperty("block_hash")
        private String blockHash;

        @JsonProperty("from_address")
        private String fromAddress;

        @JsonProperty("to_address")
        private String toAddress;

        @JsonProperty("gas_used")
        private long gasUsed;

        @JsonProperty("gas_price_gwei")
        private double gasPriceGwei;

        public ChainTransaction() {}

        // Getters and Setters
        public String getTransactionHash() { return transactionHash; }
        public void setTransactionHash(String transactionHash) { this.transactionHash = transactionHash; }

        public long getBlockNumber() { return blockNumber; }
        public void setBlockNumber(long blockNumber) { this.blockNumber = blockNumber; }

        public String getBlockHash() { return blockHash; }
        public void setBlockHash(String blockHash) { this.blockHash = blockHash; }

        public String getFromAddress() { return fromAddress; }
        public void setFromAddress(String fromAddress) { this.fromAddress = fromAddress; }

        public String getToAddress() { return toAddress; }
        public void setToAddress(String toAddress) { this.toAddress = toAddress; }

        public long getGasUsed() { return gasUsed; }
        public void setGasUsed(long gasUsed) { this.gasUsed = gasUsed; }

        public double getGasPriceGwei() { return gasPriceGwei; }
        public void setGasPriceGwei(double gasPriceGwei) { this.gasPriceGwei = gasPriceGwei; }
    }

    /**
     * Transaction Fees
     */
    public static class TransactionFees {
        @JsonProperty("source_gas_fee_usd")
        private double sourceGasFeeUsd;

        @JsonProperty("bridge_fee_usd")
        private double bridgeFeeUsd;

        @JsonProperty("target_gas_fee_usd")
        private double targetGasFeeUsd;

        @JsonProperty("total_fee_usd")
        private double totalFeeUsd;

        @JsonProperty("fee_percentage")
        private double feePercentage;

        public TransactionFees() {}

        // Getters and Setters
        public double getSourceGasFeeUsd() { return sourceGasFeeUsd; }
        public void setSourceGasFeeUsd(double sourceGasFeeUsd) { this.sourceGasFeeUsd = sourceGasFeeUsd; }

        public double getBridgeFeeUsd() { return bridgeFeeUsd; }
        public void setBridgeFeeUsd(double bridgeFeeUsd) { this.bridgeFeeUsd = bridgeFeeUsd; }

        public double getTargetGasFeeUsd() { return targetGasFeeUsd; }
        public void setTargetGasFeeUsd(double targetGasFeeUsd) { this.targetGasFeeUsd = targetGasFeeUsd; }

        public double getTotalFeeUsd() { return totalFeeUsd; }
        public void setTotalFeeUsd(double totalFeeUsd) { this.totalFeeUsd = totalFeeUsd; }

        public double getFeePercentage() { return feePercentage; }
        public void setFeePercentage(double feePercentage) { this.feePercentage = feePercentage; }
    }

    /**
     * Confirmation Information
     */
    public static class ConfirmationInfo {
        @JsonProperty("source_confirmations")
        private int sourceConfirmations;

        @JsonProperty("source_required_confirmations")
        private int sourceRequiredConfirmations;

        @JsonProperty("target_confirmations")
        private int targetConfirmations;

        @JsonProperty("target_required_confirmations")
        private int targetRequiredConfirmations;

        @JsonProperty("is_finalized")
        private boolean isFinalized;

        public ConfirmationInfo() {}

        // Getters and Setters
        public int getSourceConfirmations() { return sourceConfirmations; }
        public void setSourceConfirmations(int sourceConfirmations) { this.sourceConfirmations = sourceConfirmations; }

        public int getSourceRequiredConfirmations() { return sourceRequiredConfirmations; }
        public void setSourceRequiredConfirmations(int sourceRequiredConfirmations) { this.sourceRequiredConfirmations = sourceRequiredConfirmations; }

        public int getTargetConfirmations() { return targetConfirmations; }
        public void setTargetConfirmations(int targetConfirmations) { this.targetConfirmations = targetConfirmations; }

        public int getTargetRequiredConfirmations() { return targetRequiredConfirmations; }
        public void setTargetRequiredConfirmations(int targetRequiredConfirmations) { this.targetRequiredConfirmations = targetRequiredConfirmations; }

        public boolean isFinalized() { return isFinalized; }
        public void setFinalized(boolean finalized) { isFinalized = finalized; }
    }

    /**
     * Transaction Error
     */
    public static class TransactionError {
        @JsonProperty("error_code")
        private String errorCode;

        @JsonProperty("error_message")
        private String errorMessage;

        @JsonProperty("failed_at")
        private Instant failedAt;

        @JsonProperty("retry_count")
        private int retryCount;

        @JsonProperty("can_retry")
        private boolean canRetry;

        public TransactionError() {}

        public TransactionError(String errorCode, String errorMessage) {
            this.errorCode = errorCode;
            this.errorMessage = errorMessage;
            this.failedAt = Instant.now();
        }

        // Getters and Setters
        public String getErrorCode() { return errorCode; }
        public void setErrorCode(String errorCode) { this.errorCode = errorCode; }

        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

        public Instant getFailedAt() { return failedAt; }
        public void setFailedAt(Instant failedAt) { this.failedAt = failedAt; }

        public int getRetryCount() { return retryCount; }
        public void setRetryCount(int retryCount) { this.retryCount = retryCount; }

        public boolean isCanRetry() { return canRetry; }
        public void setCanRetry(boolean canRetry) { this.canRetry = canRetry; }
    }
}
