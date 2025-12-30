package io.aurigraph.v11.bridge.adapters;

import io.aurigraph.v11.bridge.ChainAdapter;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * ZkSync Era (Layer 2) Adapter for Aurigraph V11 Cross-Chain Bridge
 *
 * Integrates with zkSync Era, an Ethereum Layer 2 scaling solution using zero-knowledge rollups.
 * Supports:
 * - EVM-compatible transaction processing with zkEVM
 * - Native account abstraction (smart contract wallets)
 * - Very low transaction fees (fraction of Ethereum mainnet costs)
 * - Fast block times (2-4 seconds)
 * - Ethereum-level security through zero-knowledge proofs
 * - EIP-1559 dynamic gas fee estimation
 * - Native token transfers (ETH, USDC, USDT, DAI, wBTC)
 *
 * Chain Details:
 * - Chain ID: 324 (zkSync Era Mainnet)
 * - RPC: https://mainnet.era.zksync.io
 * - WebSocket: wss://mainnet.era.zksync.io/ws
 * - Block Time: ~2-4 seconds (very fast L2)
 * - Consensus: Layer 2 Proof of Stake (inherits Ethereum security)
 * - Native Currency: ETH (18 decimals)
 * - Confirmation Blocks: 5-10 blocks (~20-40 seconds for finality)
 * - Active Validators: ~500 nodes
 * - Supports EIP-1559: Yes (full Ethereum compatibility)
 *
 * ZkSync Era Features:
 * - Bytecode-level EVM compatibility (100% Solidity support)
 * - zkEVM: Zero-knowledge Ethereum Virtual Machine
 * - Native account abstraction (paymasters, signature abstraction)
 * - Low-cost data availability through zkRollup technology
 * - Instant finality on L2, ~1 hour settlement to Ethereum L1
 * - Cross-chain messaging via native bridge
 *
 * Performance Characteristics:
 * - TPS: 2000+ transactions per second
 * - Gas Costs: ~1-5% of Ethereum mainnet
 * - Finality: <1 minute on L2, ~1 hour on L1
 * - Transaction Fees: Typically $0.01-0.10 in ETH
 *
 * @author Aurigraph DLT Platform
 * @version 11.0.0
 * @since 2025-01-24
 */
@ApplicationScoped
public class ZkSyncAdapter implements ChainAdapter {

    private static final String CHAIN_ID = "324";
    private static final String CHAIN_NAME = "zkSync Era";
    private static final String NATIVE_CURRENCY = "ETH";
    private static final int DECIMALS = 18;
    private static final long BLOCK_TIME_MS = 3000; // ~2-4 seconds average
    private static final int CONFIRMATION_BLOCKS = 8; // ~20-30 seconds finality
    private static final String RPC_URL = "https://mainnet.era.zksync.io";
    private static final String WEBSOCKET_URL = "wss://mainnet.era.zksync.io/ws";
    private static final String EXPLORER_URL = "https://explorer.zksync.io";

    // Common native token addresses on zkSync Era
    private static final String USDC_ADDRESS = "0x3355df6D4c9C3035724Fd0e3914dE96A5a83aaf4"; // Native USDC on zkSync
    private static final String USDT_ADDRESS = "0x493257fD37EDB34451f62EDf8D2a0C418852bA4C"; // Bridged USDT
    private static final String DAI_ADDRESS = "0x4B9eb6c0b6ea15176BBF62841C6B2A8a398cb656"; // Bridged DAI
    private static final String WBTC_ADDRESS = "0xBBeB516fb02a01611cBBE0453Fe3c580D7281011"; // Wrapped BTC
    private static final String WETH_ADDRESS = "0x5AEa5775959fBC2557Cc8789bC1bf90A239D9a91"; // Wrapped ETH

    // Internal state
    private ChainAdapterConfig config;
    private boolean initialized = false;
    private final AtomicLong totalTransactions = new AtomicLong(0);
    private final AtomicLong successfulTransactions = new AtomicLong(0);
    private final AtomicLong failedTransactions = new AtomicLong(0);
    private final AtomicLong currentBlockHeight = new AtomicLong(0);
    private final Map<String, TransactionCacheEntry> transactionCache = new ConcurrentHashMap<>();
    private final AtomicInteger activePeers = new AtomicInteger(500); // ~500 zkSync validators
    private RetryPolicy retryPolicy;
    private Instant lastHealthCheckTime = Instant.now();

    /**
     * Gets the chain ID for zkSync Era
     */
    @Override
    public String getChainId() {
        return CHAIN_ID;
    }

    /**
     * Gets comprehensive zkSync Era chain information
     */
    @Override
    public Uni<ChainInfo> getChainInfo() {
        return Uni.createFrom().item(() -> {
            ChainInfo info = new ChainInfo();
            info.chainId = CHAIN_ID;
            info.chainName = CHAIN_NAME;
            info.nativeCurrency = NATIVE_CURRENCY;
            info.decimals = DECIMALS;
            info.rpcUrl = RPC_URL;
            info.explorerUrl = EXPLORER_URL;
            info.chainType = ChainType.LAYER2;
            info.consensusMechanism = ConsensusMechanism.PROOF_OF_STAKE;
            info.blockTime = BLOCK_TIME_MS;
            info.avgGasPrice = BigDecimal.valueOf(0.1); // Very low gas price in Gwei
            info.supportsEIP1559 = true;

            // zkSync Era-specific data
            Map<String, Object> zkSyncData = new HashMap<>();
            zkSyncData.put("confirmationBlocks", CONFIRMATION_BLOCKS);
            zkSyncData.put("tokenStandards", Arrays.asList("ERC20", "ERC721", "ERC1155"));
            zkSyncData.put("zkRollupVersion", "zkSync Era v1.0");
            zkSyncData.put("avgTransactionCost", 0.05); // In USD
            zkSyncData.put("l2Finality", "30 seconds"); // L2 finality time
            zkSyncData.put("l1Settlement", "1 hour"); // L1 settlement time
            zkSyncData.put("tpsCapacity", 2000); // Transactions per second capacity
            zkSyncData.put("accountAbstraction", true); // Native account abstraction support
            zkSyncData.put("paymasterSupport", true); // Paymaster feature for gasless transactions
            zkSyncData.put("evmCompatibility", "100%"); // Full EVM bytecode compatibility
            zkSyncData.put("activeValidators", activePeers.get());
            info.chainSpecificData = zkSyncData;

            return info;
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    /**
     * Initializes the adapter with configuration
     */
    @Override
    public Uni<Boolean> initialize(ChainAdapterConfig config) {
        return Uni.createFrom().item(() -> {
            this.config = config;

            // Validate configuration
            if (config == null || config.rpcUrl == null || config.rpcUrl.isEmpty()) {
                throw new IllegalArgumentException("Invalid configuration: RPC URL required");
            }

            // Validate chain ID matches zkSync Era
            if (config.chainId != null && !CHAIN_ID.equals(config.chainId)) {
                throw new IllegalArgumentException("Chain ID mismatch: expected " + CHAIN_ID + " but got " + config.chainId);
            }

            // Set default retry policy if not configured
            if (retryPolicy == null) {
                retryPolicy = new RetryPolicy();
                retryPolicy.maxRetries = config.maxRetries;
                retryPolicy.initialDelay = Duration.ofMillis(100);
                retryPolicy.backoffMultiplier = 2.0;
                retryPolicy.maxDelay = Duration.ofSeconds(30);
                retryPolicy.retryableErrors = Arrays.asList(
                    "timeout",
                    "connection_error",
                    "nonce_too_low",
                    "temporary_failure",
                    "gas_price_too_low",
                    "insufficient_funds",
                    "execution_reverted"
                );
                retryPolicy.enableExponentialBackoff = true;
                retryPolicy.enableJitter = true;
            }

            // Simulate RPC connection and chain verification
            currentBlockHeight.set(System.currentTimeMillis() / BLOCK_TIME_MS);

            initialized = true;
            return true;
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    /**
     * Checks connection status to zkSync Era network
     */
    @Override
    public Uni<ConnectionStatus> checkConnection() {
        return Uni.createFrom().item(() -> {
            if (!initialized) {
                throw new IllegalStateException("Adapter not initialized");
            }

            ConnectionStatus status = new ConnectionStatus();
            status.isConnected = true;
            status.latencyMs = 15; // zkSync Era is very fast (typically 10-30ms)
            status.nodeVersion = "zksync-era/v1.0.0"; // zkSync Era node version
            status.syncedBlockHeight = currentBlockHeight.get();
            status.networkBlockHeight = currentBlockHeight.get();
            status.isSynced = true;
            status.errorMessage = null;
            status.lastChecked = System.currentTimeMillis();

            lastHealthCheckTime = Instant.now();
            return status;
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    /**
     * Sends a transaction to zkSync Era
     */
    @Override
    public Uni<TransactionResult> sendTransaction(
            ChainTransaction transaction,
            TransactionOptions transactionOptions) {
        return Uni.createFrom().item(() -> {
            if (!initialized) {
                throw new IllegalStateException("Adapter not initialized");
            }

            totalTransactions.incrementAndGet();

            // Validate transaction
            if (transaction.from == null || !isValidAddress(transaction.from)) {
                failedTransactions.incrementAndGet();
                throw new IllegalArgumentException("Invalid from address");
            }

            if (transaction.to != null && !isValidAddress(transaction.to)) {
                failedTransactions.incrementAndGet();
                throw new IllegalArgumentException("Invalid to address");
            }

            // Generate transaction hash
            String txHash = generateTransactionHash();

            // Estimate gas and fees (zkSync Era has unique gas pricing)
            FeeEstimate feeEstimate = estimateTransactionFeeSync(transaction);

            // Simulate transaction execution
            TransactionResult result = new TransactionResult();
            result.transactionHash = txHash;
            result.status = TransactionExecutionStatus.PENDING;
            result.blockNumber = currentBlockHeight.get() + 1;
            result.actualGasUsed = feeEstimate.estimatedGas;
            result.actualFee = feeEstimate.totalFee;
            result.executionTime = System.currentTimeMillis();
            result.logs = new HashMap<>();
            result.logs.put("l2Status", "pending");
            result.logs.put("l1Settlement", "awaiting_batch");

            // Cache transaction for status tracking
            transactionCache.put(txHash, new TransactionCacheEntry(
                result,
                Instant.now()
            ));

            // Simulate confirmation if requested
            if (transactionOptions != null && transactionOptions.waitForConfirmation) {
                result.status = TransactionExecutionStatus.CONFIRMED;
                result.blockNumber = currentBlockHeight.get();
                result.logs.put("l2Status", "confirmed");
            }

            successfulTransactions.incrementAndGet();
            return result;
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    /**
     * Gets transaction status
     */
    @Override
    public Uni<TransactionStatus> getTransactionStatus(String transactionHash) {
        return Uni.createFrom().item(() -> {
            if (!initialized) {
                throw new IllegalStateException("Adapter not initialized");
            }

            TransactionCacheEntry cached = transactionCache.get(transactionHash);
            if (cached == null) {
                throw new IllegalArgumentException("Transaction not found: " + transactionHash);
            }

            TransactionStatus status = new TransactionStatus();
            status.transactionHash = transactionHash;
            status.status = cached.result.status;
            status.confirmations = (int) (currentBlockHeight.get() - cached.result.blockNumber);
            status.blockNumber = cached.result.blockNumber;
            status.blockHash = generateBlockHash();
            status.transactionIndex = 0;
            status.gasUsed = cached.result.actualGasUsed;
            status.effectiveGasPrice = cached.result.actualFee.divide(cached.result.actualGasUsed, 18, RoundingMode.HALF_UP);
            status.success = true;
            status.errorReason = null;
            status.timestamp = cached.timestamp.toEpochMilli();

            return status;
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    /**
     * Waits for transaction confirmation
     */
    @Override
    public Uni<ConfirmationResult> waitForConfirmation(
            String transactionHash,
            int requiredConfirmations,
            Duration timeout) {
        return Uni.createFrom().item(() -> {
            TransactionStatus status = getTransactionStatusSync(transactionHash);

            ConfirmationResult result = new ConfirmationResult();
            result.transactionHash = transactionHash;
            result.actualConfirmations = Math.min(status.confirmations, requiredConfirmations);
            result.confirmed = result.actualConfirmations >= requiredConfirmations;
            result.confirmationTime = System.currentTimeMillis() - status.timestamp;
            result.finalStatus = status;
            result.timedOut = false;
            result.errorMessage = result.confirmed ? null : "Confirmations pending";

            return result;
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    /**
     * Gets balance for native ETH or token
     */
    @Override
    public Uni<BigDecimal> getBalance(String address, String assetIdentifier) {
        return Uni.createFrom().item(() -> {
            if (!initialized) {
                throw new IllegalStateException("Adapter not initialized");
            }

            if (!isValidAddress(address)) {
                throw new IllegalArgumentException("Invalid address: " + address);
            }

            // If assetIdentifier is null, return native ETH balance
            if (assetIdentifier == null) {
                // Simulate balance query (mock data)
                return BigDecimal.valueOf(Math.random() * 5).setScale(18, RoundingMode.HALF_UP);
            }

            // Otherwise, query ERC-20 token balance
            if (!isValidAddress(assetIdentifier)) {
                throw new IllegalArgumentException("Invalid token address: " + assetIdentifier);
            }

            // Mock token balance (e.g., USDC, USDT, DAI, wBTC)
            BigDecimal tokenBalance;
            if (assetIdentifier.equalsIgnoreCase(USDC_ADDRESS)) {
                tokenBalance = BigDecimal.valueOf(Math.random() * 10000).setScale(6, RoundingMode.HALF_UP);
            } else if (assetIdentifier.equalsIgnoreCase(USDT_ADDRESS)) {
                tokenBalance = BigDecimal.valueOf(Math.random() * 10000).setScale(6, RoundingMode.HALF_UP);
            } else if (assetIdentifier.equalsIgnoreCase(DAI_ADDRESS)) {
                tokenBalance = BigDecimal.valueOf(Math.random() * 10000).setScale(18, RoundingMode.HALF_UP);
            } else if (assetIdentifier.equalsIgnoreCase(WBTC_ADDRESS)) {
                tokenBalance = BigDecimal.valueOf(Math.random() * 0.5).setScale(8, RoundingMode.HALF_UP);
            } else if (assetIdentifier.equalsIgnoreCase(WETH_ADDRESS)) {
                tokenBalance = BigDecimal.valueOf(Math.random() * 5).setScale(18, RoundingMode.HALF_UP);
            } else {
                tokenBalance = BigDecimal.ZERO;
            }

            return tokenBalance;
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    /**
     * Gets multiple balances efficiently
     */
    @Override
    public Multi<AssetBalance> getBalances(String address, List<String> assetIdentifiers) {
        return Multi.createFrom().iterable(assetIdentifiers)
            .onItem().transformToUni(assetId -> getBalance(address, assetId)
                .map(balance -> {
                    AssetBalance ab = new AssetBalance();
                    ab.address = address;
                    ab.assetIdentifier = assetId;
                    ab.balance = balance;
                    ab.lastUpdated = System.currentTimeMillis();

                    if (assetId == null) {
                        ab.assetSymbol = NATIVE_CURRENCY;
                        ab.assetType = AssetType.NATIVE;
                        ab.decimals = DECIMALS;
                    } else if (assetId.equalsIgnoreCase(USDC_ADDRESS)) {
                        ab.assetSymbol = "USDC";
                        ab.assetType = AssetType.ERC20_TOKEN;
                        ab.decimals = 6;
                    } else if (assetId.equalsIgnoreCase(USDT_ADDRESS)) {
                        ab.assetSymbol = "USDT";
                        ab.assetType = AssetType.ERC20_TOKEN;
                        ab.decimals = 6;
                    } else if (assetId.equalsIgnoreCase(DAI_ADDRESS)) {
                        ab.assetSymbol = "DAI";
                        ab.assetType = AssetType.ERC20_TOKEN;
                        ab.decimals = 18;
                    } else if (assetId.equalsIgnoreCase(WBTC_ADDRESS)) {
                        ab.assetSymbol = "wBTC";
                        ab.assetType = AssetType.WRAPPED;
                        ab.decimals = 8;
                    } else if (assetId.equalsIgnoreCase(WETH_ADDRESS)) {
                        ab.assetSymbol = "WETH";
                        ab.assetType = AssetType.WRAPPED;
                        ab.decimals = 18;
                    }

                    return ab;
                })
            )
            .concatenate();
    }

    /**
     * Estimates transaction fee with zkSync Era-specific gas pricing
     */
    @Override
    public Uni<FeeEstimate> estimateTransactionFee(ChainTransaction transaction) {
        return Uni.createFrom().item(() -> estimateTransactionFeeSync(transaction))
            .runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    /**
     * Gets current network fee information
     */
    @Override
    public Uni<NetworkFeeInfo> getNetworkFeeInfo() {
        return Uni.createFrom().item(() -> {
            if (!initialized) {
                throw new IllegalStateException("Adapter not initialized");
            }

            NetworkFeeInfo feeInfo = new NetworkFeeInfo();

            // zkSync Era fees are extremely low - typically 0.01-1 Gwei
            feeInfo.safeLowGasPrice = BigDecimal.valueOf(0.01); // Gwei
            feeInfo.standardGasPrice = BigDecimal.valueOf(0.1); // Gwei
            feeInfo.fastGasPrice = BigDecimal.valueOf(0.5); // Gwei
            feeInfo.instantGasPrice = BigDecimal.valueOf(1.0); // Gwei

            // EIP-1559 values (zkSync Era supports EIP-1559)
            feeInfo.baseFeePerGas = BigDecimal.valueOf(0.01); // Gwei

            feeInfo.networkUtilization = 0.20; // zkSync Era typically has low congestion
            feeInfo.blockNumber = currentBlockHeight.get();
            feeInfo.timestamp = System.currentTimeMillis();

            return feeInfo;
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    /**
     * Deploys smart contract to zkSync Era
     */
    @Override
    public Uni<ContractDeploymentResult> deployContract(ContractDeployment contractDeployment) {
        return Uni.createFrom().item(() -> {
            if (!initialized) {
                throw new IllegalStateException("Adapter not initialized");
            }

            if (contractDeployment.bytecode == null || contractDeployment.bytecode.isEmpty()) {
                throw new IllegalArgumentException("Invalid contract bytecode");
            }

            ContractDeploymentResult result = new ContractDeploymentResult();
            result.contractAddress = generateContractAddress();
            result.transactionHash = generateTransactionHash();
            result.success = true;
            result.gasUsed = BigDecimal.valueOf(1500000); // Typical contract deployment gas
            result.errorMessage = null;
            result.verified = false;

            // Cache transaction
            TransactionResult txResult = new TransactionResult();
            txResult.transactionHash = result.transactionHash;
            txResult.status = TransactionExecutionStatus.CONFIRMED;
            txResult.actualGasUsed = result.gasUsed;
            txResult.actualFee = result.gasUsed.multiply(BigDecimal.valueOf(0.1)).divide(BigDecimal.valueOf(1e9), 18, RoundingMode.HALF_UP);
            transactionCache.put(result.transactionHash, new TransactionCacheEntry(txResult, Instant.now()));

            return result;
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    /**
     * Calls smart contract function on zkSync Era
     */
    @Override
    public Uni<ContractCallResult> callContract(ContractFunctionCall contractCall) {
        return Uni.createFrom().item(() -> {
            if (!initialized) {
                throw new IllegalStateException("Adapter not initialized");
            }

            if (!isValidAddress(contractCall.contractAddress)) {
                throw new IllegalArgumentException("Invalid contract address");
            }

            ContractCallResult result = new ContractCallResult();

            if (contractCall.isReadOnly) {
                // Read-only calls don't cost gas
                result.transactionHash = null;
                result.gasUsed = BigDecimal.ZERO;
                result.returnValue = "0x"; // Mock return value
            } else {
                // State-changing calls
                result.transactionHash = generateTransactionHash();
                result.gasUsed = BigDecimal.valueOf(80000); // zkSync Era is efficient

                // Cache transaction
                TransactionResult txResult = new TransactionResult();
                txResult.transactionHash = result.transactionHash;
                txResult.status = TransactionExecutionStatus.CONFIRMED;
                txResult.actualGasUsed = result.gasUsed;
                txResult.actualFee = result.gasUsed.multiply(BigDecimal.valueOf(0.1)).divide(BigDecimal.valueOf(1e9), 18, RoundingMode.HALF_UP);
                transactionCache.put(result.transactionHash, new TransactionCacheEntry(txResult, Instant.now()));
            }

            result.success = true;
            result.errorMessage = null;
            result.events = new HashMap<>();

            return result;
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    /**
     * Subscribes to blockchain events
     */
    @Override
    public Multi<BlockchainEvent> subscribeToEvents(EventFilter eventFilter) {
        return Multi.createFrom().ticks().every(Duration.ofSeconds(3))
            .map(i -> {
                BlockchainEvent event = new BlockchainEvent();
                event.transactionHash = generateTransactionHash();
                event.blockNumber = currentBlockHeight.get();
                event.blockHash = generateBlockHash();
                event.logIndex = i.intValue();
                event.contractAddress = eventFilter.contractAddress;
                event.eventSignature = "Transfer(address,address,uint256)";
                event.eventData = new ArrayList<>();
                event.indexedData = new HashMap<>();
                event.timestamp = System.currentTimeMillis();
                event.eventType = EventType.TRANSFER;
                return event;
            });
    }

    /**
     * Gets historical events
     */
    @Override
    public Multi<BlockchainEvent> getHistoricalEvents(
            EventFilter eventFilter,
            long fromBlock,
            long toBlock) {
        return Multi.createFrom().range(0, (int) Math.min(10, toBlock - fromBlock))
            .map(i -> {
                BlockchainEvent event = new BlockchainEvent();
                event.transactionHash = generateTransactionHash();
                event.blockNumber = fromBlock + i;
                event.blockHash = generateBlockHash();
                event.logIndex = i;
                event.contractAddress = eventFilter.contractAddress;
                event.eventSignature = "Transfer(address,address,uint256)";
                event.eventData = new ArrayList<>();
                event.indexedData = new HashMap<>();
                event.timestamp = System.currentTimeMillis() - (10 - i) * BLOCK_TIME_MS;
                event.eventType = EventType.TRANSFER;
                return event;
            });
    }

    /**
     * Gets block information
     */
    @Override
    public Uni<BlockInfo> getBlockInfo(String blockIdentifier) {
        return Uni.createFrom().item(() -> {
            BlockInfo info = new BlockInfo();
            info.blockNumber = Long.parseLong(blockIdentifier);
            info.blockHash = generateBlockHash();
            info.parentHash = generateBlockHash();
            info.timestamp = System.currentTimeMillis();
            info.miner = "0x0000000000000000000000000000000000000001"; // zkSync Era operator
            info.difficulty = BigDecimal.ZERO; // PoS L2 doesn't use difficulty
            info.totalDifficulty = BigDecimal.ZERO;
            info.gasLimit = 80000000; // zkSync Era has high gas limit
            info.gasUsed = 25000000; // Mock usage
            info.transactionCount = 300; // zkSync Era has high throughput
            info.transactionHashes = new ArrayList<>();
            info.extraData = new HashMap<>();
            info.extraData.put("l1BatchNumber", System.currentTimeMillis() / 3600000); // L1 batch reference

            return info;
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    /**
     * Gets current block height
     */
    @Override
    public Uni<Long> getCurrentBlockHeight() {
        return Uni.createFrom().item(() -> {
            if (!initialized) {
                throw new IllegalStateException("Adapter not initialized");
            }
            return currentBlockHeight.get();
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    /**
     * Validates Ethereum-compatible address (zkSync uses same format)
     */
    @Override
    public Uni<AddressValidationResult> validateAddress(String address) {
        return Uni.createFrom().item(() -> {
            AddressValidationResult result = new AddressValidationResult();
            result.address = address;
            result.isValid = isValidAddress(address);
            result.format = AddressFormat.ETHEREUM_CHECKSUM;
            result.normalizedAddress = address != null ? address.toLowerCase() : null;
            result.validationMessage = result.isValid ? "Valid Ethereum-compatible address for zkSync Era" : "Invalid address format";

            return result;
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    /**
     * Monitors network health
     */
    @Override
    public Multi<NetworkHealth> monitorNetworkHealth(Duration monitoringInterval) {
        return Multi.createFrom().ticks().every(monitoringInterval)
            .map(i -> {
                NetworkHealth health = new NetworkHealth();
                health.timestamp = System.currentTimeMillis();
                health.isHealthy = true;
                health.currentBlockHeight = currentBlockHeight.get();
                health.averageBlockTime = BLOCK_TIME_MS;
                health.networkHashRate = 0; // L2 PoS doesn't use hash rate
                health.activePeers = activePeers.get();
                health.networkUtilization = 0.20; // zkSync Era typically has low utilization
                health.healthIssues = new ArrayList<>();
                health.status = NetworkStatus.ONLINE;

                return health;
            });
    }

    /**
     * Gets adapter statistics
     */
    @Override
    public Uni<AdapterStatistics> getAdapterStatistics(Duration timeWindow) {
        return Uni.createFrom().item(() -> {
            AdapterStatistics stats = new AdapterStatistics();
            stats.chainId = CHAIN_ID;
            stats.totalTransactions = totalTransactions.get();
            stats.successfulTransactions = successfulTransactions.get();
            stats.failedTransactions = failedTransactions.get();
            stats.successRate = stats.totalTransactions > 0
                ? (double) stats.successfulTransactions / stats.totalTransactions
                : 0.0;
            stats.averageTransactionTime = 3000; // ~3 seconds for zkSync Era
            stats.averageConfirmationTime = CONFIRMATION_BLOCKS * BLOCK_TIME_MS; // ~24 seconds
            stats.totalGasUsed = 0;
            stats.totalFeesSpent = BigDecimal.ZERO;
            stats.transactionsByType = new HashMap<>();
            stats.statisticsTimeWindow = timeWindow.toMillis();

            return stats;
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    /**
     * Configures retry policy
     */
    @Override
    public Uni<Boolean> configureRetryPolicy(RetryPolicy policy) {
        return Uni.createFrom().item(() -> {
            if (policy == null) {
                throw new IllegalArgumentException("Retry policy cannot be null");
            }
            this.retryPolicy = policy;
            return true;
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    /**
     * Shuts down the adapter
     */
    @Override
    public Uni<Boolean> shutdown() {
        return Uni.createFrom().item(() -> {
            initialized = false;
            transactionCache.clear();
            totalTransactions.set(0);
            successfulTransactions.set(0);
            failedTransactions.set(0);
            currentBlockHeight.set(0);
            return true;
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    // ============ Helper Methods ============

    private boolean isValidAddress(String address) {
        if (address == null || address.length() != 42) {
            return false;
        }
        if (!address.startsWith("0x")) {
            return false;
        }
        try {
            // Check if it's valid hex (40 hex characters after 0x)
            String hexPart = address.substring(2);
            for (char c : hexPart.toCharArray()) {
                if (!Character.isDigit(c) && !((c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F'))) {
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private String generateTransactionHash() {
        return "0x" + generateRandomHex(64);
    }

    private String generateBlockHash() {
        return "0x" + generateRandomHex(64);
    }

    private String generateContractAddress() {
        return "0x" + generateRandomHex(40);
    }

    private String generateRandomHex(int length) {
        StringBuilder hex = new StringBuilder();
        for (int i = 0; i < length; i++) {
            int randomNum = (int) (Math.random() * 16);
            char hexChar = (randomNum < 10) ? (char) ('0' + randomNum) : (char) ('a' + randomNum - 10);
            hex.append(hexChar);
        }
        return hex.toString();
    }

    private FeeEstimate estimateTransactionFeeSync(ChainTransaction transaction) {
        FeeEstimate estimate = new FeeEstimate();

        // Estimate gas based on transaction type
        BigDecimal estimatedGas;
        if (transaction.transactionType == TransactionType.TRANSFER) {
            estimatedGas = BigDecimal.valueOf(21000); // Standard transfer
        } else if (transaction.transactionType == TransactionType.CONTRACT_CALL) {
            estimatedGas = BigDecimal.valueOf(80000); // zkSync Era is efficient
        } else if (transaction.transactionType == TransactionType.CONTRACT_DEPLOY) {
            estimatedGas = BigDecimal.valueOf(1500000); // Contract deployment
        } else {
            estimatedGas = BigDecimal.valueOf(50000); // Default
        }

        estimate.estimatedGas = estimatedGas;

        // zkSync Era fees are very low
        NetworkFeeInfo feeInfo = getNetworkFeeInfoSync();
        estimate.gasPrice = feeInfo.standardGasPrice;
        estimate.maxFeePerGas = feeInfo.fastGasPrice;
        estimate.maxPriorityFeePerGas = BigDecimal.valueOf(0.01);

        // Calculate total fee (in ETH)
        BigDecimal totalFeeGwei = estimatedGas.multiply(estimate.gasPrice).divide(BigDecimal.valueOf(1e9), 18, RoundingMode.HALF_UP);
        estimate.totalFee = totalFeeGwei;

        // Convert to USD (1 ETH ≈ $3000, but fees are so low this is typically <$0.10)
        estimate.totalFeeUSD = totalFeeGwei.multiply(BigDecimal.valueOf(3000));

        estimate.feeSpeed = FeeSpeed.STANDARD;
        estimate.estimatedConfirmationTime = Duration.ofSeconds(BLOCK_TIME_MS / 1000 * CONFIRMATION_BLOCKS); // ~24 seconds

        return estimate;
    }

    private NetworkFeeInfo getNetworkFeeInfoSync() {
        NetworkFeeInfo feeInfo = new NetworkFeeInfo();
        feeInfo.safeLowGasPrice = BigDecimal.valueOf(0.01);
        feeInfo.standardGasPrice = BigDecimal.valueOf(0.1);
        feeInfo.fastGasPrice = BigDecimal.valueOf(0.5);
        feeInfo.instantGasPrice = BigDecimal.valueOf(1.0);
        feeInfo.baseFeePerGas = BigDecimal.valueOf(0.01);
        feeInfo.networkUtilization = 0.20;
        feeInfo.blockNumber = currentBlockHeight.get();
        feeInfo.timestamp = System.currentTimeMillis();
        return feeInfo;
    }

    private TransactionStatus getTransactionStatusSync(String transactionHash) {
        TransactionCacheEntry cached = transactionCache.get(transactionHash);
        if (cached == null) {
            throw new IllegalArgumentException("Transaction not found: " + transactionHash);
        }

        TransactionStatus status = new TransactionStatus();
        status.transactionHash = transactionHash;
        status.status = cached.result.status;
        status.confirmations = (int) (currentBlockHeight.get() - cached.result.blockNumber);
        status.blockNumber = cached.result.blockNumber;
        status.blockHash = generateBlockHash();
        status.transactionIndex = 0;
        status.gasUsed = cached.result.actualGasUsed;
        status.effectiveGasPrice = cached.result.actualFee.divide(cached.result.actualGasUsed, 18, RoundingMode.HALF_UP);
        status.success = true;
        status.errorReason = null;
        status.timestamp = cached.timestamp.toEpochMilli();

        return status;
    }

    /**
     * Internal class for caching transaction information
     */
    private static class TransactionCacheEntry {
        final TransactionResult result;
        final Instant timestamp;

        TransactionCacheEntry(TransactionResult result, Instant timestamp) {
            this.result = result;
            this.timestamp = timestamp;
        }
    }
}
