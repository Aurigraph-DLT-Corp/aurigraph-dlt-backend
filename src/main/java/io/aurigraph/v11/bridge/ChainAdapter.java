package io.aurigraph.v11.bridge;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Base interface for blockchain-specific adapters in Aurigraph V11 cross-chain bridge.
 * 
 * This interface defines the common operations that each blockchain adapter must
 * implement to enable seamless cross-chain interoperability. Each supported
 * blockchain (Ethereum, BSC, Polygon, Solana, etc.) will have its own adapter
 * implementation extending this interface.
 * 
 * Key Features:
 * - Blockchain-agnostic transaction processing
 * - Standardized asset handling across different chains
 * - Event monitoring and subscription capabilities
 * - Network health and status monitoring
 * - Gas/fee optimization and estimation
 * - Smart contract deployment and interaction
 * 
 * Performance Requirements:
 * - Support for 10K+ transactions per chain per day
 * - Sub-second transaction status updates
 * - Real-time event monitoring with 99.9% reliability
 * - Automatic failover and error recovery
 * 
 * @author Aurigraph DLT Platform
 * @version 11.0.0
 * @since 2025-01-01
 */
@ApplicationScoped
public interface ChainAdapter {

    /**
     * Gets the unique identifier for this blockchain.
     * 
     * @return the chain identifier (e.g., "ethereum", "bsc", "polygon")
     */
    String getChainId();

    /**
     * Gets comprehensive information about the blockchain network.
     * 
     * @return Uni containing the chain information
     */
    Uni<ChainInfo> getChainInfo();

    /**
     * Initializes the adapter with blockchain-specific configuration.
     * 
     * @param config the configuration for this chain adapter
     * @return Uni indicating success or failure of initialization
     */
    Uni<Boolean> initialize(ChainAdapterConfig config);

    /**
     * Checks if the adapter is properly connected to the blockchain network.
     * 
     * @return Uni containing the connection status
     */
    Uni<ConnectionStatus> checkConnection();

    /**
     * Sends a transaction to the blockchain network.
     * 
     * @param transaction the transaction to send
     * @param transactionOptions options for transaction execution
     * @return Uni containing the transaction result
     */
    Uni<TransactionResult> sendTransaction(
        ChainTransaction transaction,
        TransactionOptions transactionOptions
    );

    /**
     * Gets the current status of a transaction.
     * 
     * @param transactionHash the hash of the transaction
     * @return Uni containing the transaction status
     */
    Uni<TransactionStatus> getTransactionStatus(String transactionHash);

    /**
     * Waits for transaction confirmation with specified number of blocks.
     * 
     * @param transactionHash the hash of the transaction
     * @param requiredConfirmations the number of confirmations to wait for
     * @param timeout maximum time to wait for confirmations
     * @return Uni containing the confirmation result
     */
    Uni<ConfirmationResult> waitForConfirmation(
        String transactionHash,
        int requiredConfirmations,
        Duration timeout
    );

    /**
     * Gets the current balance of an address for a specific asset.
     * 
     * @param address the address to check balance for
     * @param assetIdentifier the asset identifier (contract address for tokens, null for native)
     * @return Uni containing the current balance
     */
    Uni<BigDecimal> getBalance(String address, String assetIdentifier);

    /**
     * Gets multiple balances for an address across different assets.
     * 
     * @param address the address to check balances for
     * @param assetIdentifiers the list of asset identifiers
     * @return Multi streaming balance information for each asset
     */
    Multi<AssetBalance> getBalances(String address, List<String> assetIdentifiers);

    /**
     * Estimates the gas/fee cost for a transaction.
     * 
     * @param transaction the transaction to estimate for
     * @return Uni containing the fee estimate
     */
    Uni<FeeEstimate> estimateTransactionFee(ChainTransaction transaction);

    /**
     * Gets current network fee information.
     * 
     * @return Uni containing current fee information
     */
    Uni<NetworkFeeInfo> getNetworkFeeInfo();

    /**
     * Deploys a smart contract to the blockchain.
     * 
     * @param contractDeployment the contract deployment configuration
     * @return Uni containing the deployment result
     */
    Uni<ContractDeploymentResult> deployContract(ContractDeployment contractDeployment);

    /**
     * Calls a smart contract function.
     * 
     * @param contractCall the contract function call configuration
     * @return Uni containing the call result
     */
    Uni<ContractCallResult> callContract(ContractFunctionCall contractCall);

    /**
     * Subscribes to blockchain events with filtering capabilities.
     * 
     * @param eventFilter the filter for events to monitor
     * @return Multi streaming blockchain events
     */
    Multi<BlockchainEvent> subscribeToEvents(EventFilter eventFilter);

    /**
     * Gets historical events from the blockchain.
     * 
     * @param eventFilter the filter for historical events
     * @param fromBlock starting block number
     * @param toBlock ending block number
     * @return Multi streaming historical events
     */
    Multi<BlockchainEvent> getHistoricalEvents(EventFilter eventFilter, long fromBlock, long toBlock);

    /**
     * Gets information about a specific block.
     * 
     * @param blockIdentifier block number or hash
     * @return Uni containing block information
     */
    Uni<BlockInfo> getBlockInfo(String blockIdentifier);

    /**
     * Gets the current block height of the blockchain.
     * 
     * @return Uni containing the current block height
     */
    Uni<Long> getCurrentBlockHeight();

    /**
     * Validates an address format for this blockchain.
     * 
     * @param address the address to validate
     * @return Uni containing validation result
     */
    Uni<AddressValidationResult> validateAddress(String address);

    /**
     * Monitors the health and performance of the blockchain network.
     * 
     * @param monitoringInterval the interval between health checks
     * @return Multi streaming network health metrics
     */
    Multi<NetworkHealth> monitorNetworkHealth(Duration monitoringInterval);

    /**
     * Gets comprehensive adapter performance statistics.
     * 
     * @param timeWindow the time window for statistics calculation
     * @return Uni containing adapter statistics
     */
    Uni<AdapterStatistics> getAdapterStatistics(Duration timeWindow);

    /**
     * Configures automatic retry policies for failed operations.
     * 
     * @param retryPolicy the retry policy configuration
     * @return Uni indicating success or failure of configuration
     */
    Uni<Boolean> configureRetryPolicy(RetryPolicy retryPolicy);

    /**
     * Shuts down the adapter and cleans up resources.
     * 
     * @return Uni indicating successful shutdown
     */
    Uni<Boolean> shutdown();

    // Inner classes and enums for data transfer objects

    /**
     * Information about the blockchain network.
     */
    public static class ChainInfo {
        public String chainId;
        public String chainName;
        public String nativeCurrency;
        public int decimals;
        public String rpcUrl;
        public String explorerUrl;
        public ChainType chainType;
        public ConsensusMechanism consensusMechanism;
        public long blockTime; // Average block time in milliseconds
        public BigDecimal avgGasPrice;
        public boolean supportsEIP1559; // For Ethereum-based chains
        public Map<String, Object> chainSpecificData;
    }

    /**
     * Types of blockchain networks.
     */
    public enum ChainType {
        MAINNET,
        TESTNET,
        LAYER2,
        SIDECHAIN,
        PARACHAIN,
        CUSTOM
    }

    /**
     * Consensus mechanisms supported.
     */
    public enum ConsensusMechanism {
        PROOF_OF_WORK,
        PROOF_OF_STAKE,
        PROOF_OF_AUTHORITY,
        DELEGATED_PROOF_OF_STAKE,
        PROOF_OF_HISTORY,
        NOMINATED_PROOF_OF_STAKE,  // Polkadot/Substrate NPoS
        CUSTOM
    }

    /**
     * Configuration for chain adapters.
     */
    public static class ChainAdapterConfig {
        public String chainId;
        public String rpcUrl;
        public String websocketUrl;
        public String privateKey; // For transaction signing
        public int maxRetries;
        public Duration timeout;
        public int confirmationBlocks;
        public boolean enableEvents;
        public Map<String, Object> chainSpecificConfig;
    }

    /**
     * Connection status to the blockchain.
     */
    public static class ConnectionStatus {
        public boolean isConnected;
        public long latencyMs;
        public String nodeVersion;
        public long syncedBlockHeight;
        public long networkBlockHeight;
        public boolean isSynced;
        public String errorMessage;
        public long lastChecked;
    }

    /**
     * Represents a blockchain transaction.
     */
    public static class ChainTransaction {
        public String from;
        public String to;
        public BigDecimal value;
        public String data; // Transaction data/input
        public BigDecimal gasLimit;
        public BigDecimal gasPrice;
        public BigDecimal maxFeePerGas; // EIP-1559
        public BigDecimal maxPriorityFeePerGas; // EIP-1559
        public Long nonce;
        public TransactionType transactionType;
        public Map<String, Object> chainSpecificFields;
    }

    /**
     * Types of transactions.
     */
    public enum TransactionType {
        TRANSFER,           // Simple value transfer
        CONTRACT_CALL,      // Smart contract function call
        CONTRACT_DEPLOY,    // Smart contract deployment
        TOKEN_TRANSFER,     // Token transfer
        MULTI_SEND,         // Multiple operations
        STAKING,            // Staking operations (Polkadot, Cosmos, etc.)
        CUSTOM              // Chain-specific transaction type
    }

    /**
     * Options for transaction execution.
     */
    public static class TransactionOptions {
        public boolean waitForConfirmation;
        public int requiredConfirmations;
        public Duration confirmationTimeout;
        public boolean enableRetry;
        public int maxRetries;
        public TransactionPriority priority;
    }

    /**
     * Priority levels for transactions.
     */
    public enum TransactionPriority {
        LOW,
        MEDIUM,
        HIGH,
        URGENT
    }

    /**
     * Result of transaction execution.
     */
    public static class TransactionResult {
        public String transactionHash;
        public TransactionExecutionStatus status;
        public long blockNumber;
        public String blockHash;
        public BigDecimal actualGasUsed;
        public BigDecimal actualFee;
        public String errorMessage;
        public Map<String, Object> logs;
        public long executionTime;
    }

    /**
     * Transaction execution status.
     */
    public enum TransactionExecutionStatus {
        PENDING,
        CONFIRMED,
        FINALIZED,          // Block finalized (Polkadot, Cosmos)
        FAILED,
        DROPPED,
        REPLACED
    }

    /**
     * Detailed transaction status information.
     */
    public static class TransactionStatus {
        public String transactionHash;
        public TransactionExecutionStatus status;
        public int confirmations;
        public long blockNumber;
        public String blockHash;
        public int transactionIndex;
        public BigDecimal gasUsed;
        public BigDecimal effectiveGasPrice;
        public boolean success;
        public String errorReason;
        public long timestamp;
    }

    /**
     * Result of waiting for transaction confirmation.
     */
    public static class ConfirmationResult {
        public String transactionHash;
        public boolean confirmed;
        public int actualConfirmations;
        public long confirmationTime;
        public TransactionStatus finalStatus;
        public boolean timedOut;
        public String errorMessage;
    }

    /**
     * Balance information for an asset.
     */
    public static class AssetBalance {
        public String address;
        public String assetIdentifier;
        public String assetSymbol;
        public BigDecimal balance;
        public BigDecimal balanceUSD; // USD value if available
        public int decimals;
        public AssetType assetType;
        public long lastUpdated;
    }

    /**
     * Types of assets supported.
     */
    public enum AssetType {
        NATIVE,         // Native blockchain currency
        ERC20_TOKEN,    // ERC-20 compatible token
        NFT,            // Non-fungible token
        MULTI_TOKEN,    // ERC-1155 style multi-token
        WRAPPED,        // Wrapped native currency
        CUSTOM          // Chain-specific asset type
    }

    /**
     * Fee estimate for transactions.
     */
    public static class FeeEstimate {
        public BigDecimal estimatedGas;
        public BigDecimal gasPrice;
        public BigDecimal maxFeePerGas; // EIP-1559
        public BigDecimal maxPriorityFeePerGas; // EIP-1559
        public BigDecimal totalFee;
        public BigDecimal totalFeeUSD; // USD value if available
        public FeeSpeed feeSpeed;
        public Duration estimatedConfirmationTime;
    }

    /**
     * Fee speed categories.
     */
    public enum FeeSpeed {
        SLOW,
        STANDARD,
        FAST,
        INSTANT
    }

    /**
     * Current network fee information.
     */
    public static class NetworkFeeInfo {
        public BigDecimal safeLowGasPrice;
        public BigDecimal standardGasPrice;
        public BigDecimal fastGasPrice;
        public BigDecimal instantGasPrice;
        public BigDecimal baseFeePerGas; // EIP-1559
        public double networkUtilization; // 0.0 to 1.0
        public long blockNumber;
        public long timestamp;
    }

    /**
     * Smart contract deployment configuration.
     */
    public static class ContractDeployment {
        public String bytecode;
        public String constructorData;
        public BigDecimal gasLimit;
        public BigDecimal gasPrice;
        public String deployer;
        public Map<String, Object> constructorArgs;
        public boolean verify; // Whether to verify contract on explorer
    }

    /**
     * Result of contract deployment.
     */
    public static class ContractDeploymentResult {
        public String contractAddress;
        public String transactionHash;
        public boolean success;
        public BigDecimal gasUsed;
        public String errorMessage;
        public boolean verified;
    }

    /**
     * Smart contract function call configuration.
     */
    public static class ContractFunctionCall {
        public String contractAddress;
        public String functionName;
        public List<Object> functionArgs;
        public String functionSignature;
        public BigDecimal value; // ETH/native currency to send
        public BigDecimal gasLimit;
        public BigDecimal gasPrice;
        public String caller;
        public boolean isReadOnly; // View/pure function call
    }

    /**
     * Result of contract function call.
     */
    public static class ContractCallResult {
        public Object returnValue;
        public String transactionHash; // Null for read-only calls
        public boolean success;
        public BigDecimal gasUsed;
        public String errorMessage;
        public Map<String, Object> events; // Emitted events
    }

    /**
     * Filter for blockchain events.
     */
    public static class EventFilter {
        public String contractAddress;
        public List<String> eventSignatures; // Event topics
        public String fromAddress;
        public String toAddress;
        public long fromBlock;
        public long toBlock;
        public Map<String, Object> additionalFilters;
    }

    /**
     * Blockchain event information.
     */
    public static class BlockchainEvent {
        public String transactionHash;
        public long blockNumber;
        public String blockHash;
        public int logIndex;
        public String contractAddress;
        public String eventSignature;
        public List<Object> eventData;
        public Map<String, Object> indexedData;
        public long timestamp;
        public EventType eventType;
    }

    /**
     * Types of blockchain events.
     */
    public enum EventType {
        TRANSFER,
        APPROVAL,
        CONTRACT_CREATED,
        CONTRACT_CALLED,
        BLOCK_MINED,
        CUSTOM
    }

    /**
     * Block information.
     */
    public static class BlockInfo {
        public long blockNumber;
        public String blockHash;
        public String parentHash;
        public long timestamp;
        public String miner; // Block producer
        public BigDecimal difficulty;
        public BigDecimal totalDifficulty;
        public long gasLimit;
        public long gasUsed;
        public int transactionCount;
        public List<String> transactionHashes;
        public Map<String, Object> extraData;
    }

    /**
     * Address validation result.
     */
    public static class AddressValidationResult {
        public String address;
        public boolean isValid;
        public AddressFormat format;
        public String normalizedAddress; // Checksummed or normalized format
        public String validationMessage;
    }

    /**
     * Address formats supported.
     */
    public enum AddressFormat {
        ETHEREUM_CHECKSUM,
        BITCOIN_P2PKH,
        BITCOIN_P2SH,
        BITCOIN_BECH32,
        SOLANA_BASE58,
        SUBSTRATE_SS58,     // Polkadot/Substrate SS58 format
        CUSTOM
    }

    /**
     * Network health metrics.
     */
    public static class NetworkHealth {
        public long timestamp;
        public boolean isHealthy;
        public long currentBlockHeight;
        public long averageBlockTime;
        public double networkHashRate; // If applicable
        public int activePeers;
        public double networkUtilization;
        public List<String> healthIssues;
        public NetworkStatus status;
    }

    /**
     * Network status levels.
     */
    public enum NetworkStatus {
        ONLINE,
        DEGRADED,
        CONGESTED,
        OFFLINE,
        MAINTENANCE
    }

    /**
     * Adapter performance statistics.
     */
    public static class AdapterStatistics {
        public String chainId;
        public long totalTransactions;
        public long successfulTransactions;
        public long failedTransactions;
        public double successRate;
        public double averageTransactionTime;
        public double averageConfirmationTime;
        public long totalGasUsed;
        public BigDecimal totalFeesSpent;
        public Map<TransactionType, Long> transactionsByType;
        public long statisticsTimeWindow;
    }

    /**
     * Retry policy configuration.
     */
    public static class RetryPolicy {
        public int maxRetries;
        public Duration initialDelay;
        public double backoffMultiplier;
        public Duration maxDelay;
        public List<String> retryableErrors;
        public boolean enableExponentialBackoff;
        public boolean enableJitter;
    }
}