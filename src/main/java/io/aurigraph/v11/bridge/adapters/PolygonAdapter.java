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
 * Polygon (Matic) Adapter for Aurigraph V11 Cross-Chain Bridge
 *
 * Integrates with Polygon PoS (Proof of Stake) Layer 2 solution.
 * Supports:
 * - EVM-compatible transaction processing
 * - EIP-1559 dynamic gas fee estimation
 * - ERC-20 token transfers and balance queries
 * - Very low transaction fees (typically $0.01-0.10 vs Ethereum's $5-50)
 * - Fast block times (~2 seconds)
 * - 128 block confirmation requirement for finality
 *
 * Chain Details:
 * - Chain ID: 137 (Polygon Mainnet)
 * - RPC: https://polygon-rpc.com
 * - WebSocket: wss://polygon-rpc.com
 * - Block Time: ~2000ms (2 seconds)
 * - Consensus: Proof of Stake
 * - Native Currency: MATIC (18 decimals)
 * - Supports EIP-1559: Yes
 *
 * @author Aurigraph DLT Platform
 * @version 11.0.0
 * @since 2025-01-01
 */
@ApplicationScoped
public class PolygonAdapter implements ChainAdapter {

    private static final String CHAIN_ID = "137";
    private static final String CHAIN_NAME = "Polygon Mainnet";
    private static final String NATIVE_CURRENCY = "MATIC";
    private static final int DECIMALS = 18;
    private static final long BLOCK_TIME_MS = 2000; // ~2 seconds
    private static final int CONFIRMATION_BLOCKS = 128;
    private static final String RPC_URL = "https://polygon-rpc.com";
    private static final String WEBSOCKET_URL = "wss://polygon-rpc.com";
    private static final String EXPLORER_URL = "https://polygonscan.com";

    // Common ERC-20 token addresses on Polygon
    private static final String USDC_ADDRESS = "0x2791Bca1f2de4661ED88A30C99A7a9449Aa84174";
    private static final String USDT_ADDRESS = "0xc2132D05D31c914a87C6611C10748AEb04B58e8F";
    private static final String DAI_ADDRESS = "0x8f3Cf7ad23Cd3CaDbD9735AFf958023239c6A063";

    // Internal state
    private ChainAdapterConfig config;
    private boolean initialized = false;
    private final AtomicLong totalTransactions = new AtomicLong(0);
    private final AtomicLong successfulTransactions = new AtomicLong(0);
    private final AtomicLong failedTransactions = new AtomicLong(0);
    private final AtomicLong currentBlockHeight = new AtomicLong(0);
    private final Map<String, TransactionCacheEntry> transactionCache = new ConcurrentHashMap<>();
    private final AtomicInteger activePeers = new AtomicInteger(50); // Mock peer count
    private RetryPolicy retryPolicy;
    private Instant lastHealthCheckTime = Instant.now();

    /**
     * Gets the chain ID for Polygon
     */
    @Override
    public String getChainId() {
        return CHAIN_ID;
    }

    /**
     * Gets comprehensive Polygon chain information
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
            info.avgGasPrice = BigDecimal.valueOf(50); // ~50 Gwei (in MATIC), very cheap
            info.supportsEIP1559 = true;

            // Polygon-specific data
            Map<String, Object> polygonData = new HashMap<>();
            polygonData.put("confirmationBlocks", CONFIRMATION_BLOCKS);
            polygonData.put("tokenStandards", Arrays.asList("ERC20", "ERC721", "ERC1155"));
            polygonData.put("bridgeNetwork", "Polygon Plasma Bridge");
            polygonData.put("avgTransactionCost", 0.01); // In USD
            info.chainSpecificData = polygonData;

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
                    "temporary_failure"
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
     * Checks connection status to Polygon network
     */
    @Override
    public Uni<ConnectionStatus> checkConnection() {
        return Uni.createFrom().item(() -> {
            if (!initialized) {
                throw new IllegalStateException("Adapter not initialized");
            }

            ConnectionStatus status = new ConnectionStatus();
            status.isConnected = true;
            status.latencyMs = 25; // Polygon is very fast
            status.nodeVersion = "bor/v0.4.0"; // Bor client version
            status.syncedBlockHeight = currentBlockHeight.get();
            status.networkBlockHeight = currentBlockHeight.get();
            status.isSynced = true;
            status.errorMessage = null;
            status.lastChecked = System.currentTimeMillis();

            return status;
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    /**
     * Sends a transaction to Polygon
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

            // Estimate gas and fees
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

            // Cache transaction for status tracking
            transactionCache.put(txHash, new TransactionCacheEntry(
                result,
                Instant.now()
            ));

            // Simulate confirmation
            if (transactionOptions != null && transactionOptions.waitForConfirmation) {
                result.status = TransactionExecutionStatus.CONFIRMED;
                result.blockNumber = currentBlockHeight.get();
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
     * Gets balance for native MATIC
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

            // If assetIdentifier is null, return native MATIC balance
            if (assetIdentifier == null) {
                // Simulate balance query (mock data)
                return BigDecimal.valueOf(Math.random() * 10).setScale(18, RoundingMode.HALF_UP);
            }

            // Otherwise, query ERC-20 token balance
            if (!isValidAddress(assetIdentifier)) {
                throw new IllegalArgumentException("Invalid token address: " + assetIdentifier);
            }

            // Mock token balance (e.g., USDC, USDT, DAI)
            BigDecimal tokenBalance;
            if (assetIdentifier.equalsIgnoreCase(USDC_ADDRESS)) {
                tokenBalance = BigDecimal.valueOf(Math.random() * 10000).setScale(6, RoundingMode.HALF_UP);
            } else if (assetIdentifier.equalsIgnoreCase(USDT_ADDRESS)) {
                tokenBalance = BigDecimal.valueOf(Math.random() * 10000).setScale(6, RoundingMode.HALF_UP);
            } else if (assetIdentifier.equalsIgnoreCase(DAI_ADDRESS)) {
                tokenBalance = BigDecimal.valueOf(Math.random() * 10000).setScale(18, RoundingMode.HALF_UP);
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
                    }

                    return ab;
                })
            )
            .concatenate();
    }

    /**
     * Estimates transaction fee with EIP-1559 support
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

            // Polygon fees are very low - typically 1-100 Gwei
            feeInfo.safeLowGasPrice = BigDecimal.valueOf(30); // Gwei
            feeInfo.standardGasPrice = BigDecimal.valueOf(50); // Gwei
            feeInfo.fastGasPrice = BigDecimal.valueOf(100); // Gwei
            feeInfo.instantGasPrice = BigDecimal.valueOf(150); // Gwei

            // EIP-1559 values
            feeInfo.baseFeePerGas = BigDecimal.valueOf(25); // Gwei

            feeInfo.networkUtilization = 0.35; // Polygon typically has low congestion
            feeInfo.blockNumber = currentBlockHeight.get();
            feeInfo.timestamp = System.currentTimeMillis();

            return feeInfo;
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    /**
     * Deploys smart contract
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
            result.gasUsed = BigDecimal.valueOf(2000000); // Typical contract deployment gas
            result.errorMessage = null;
            result.verified = false;

            // Cache transaction
            TransactionResult txResult = new TransactionResult();
            txResult.transactionHash = result.transactionHash;
            txResult.status = TransactionExecutionStatus.CONFIRMED;
            txResult.actualGasUsed = result.gasUsed;
            txResult.actualFee = result.gasUsed.multiply(BigDecimal.valueOf(50)).divide(BigDecimal.valueOf(1e9), 18, RoundingMode.HALF_UP);
            transactionCache.put(result.transactionHash, new TransactionCacheEntry(txResult, Instant.now()));

            return result;
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    /**
     * Calls smart contract
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
                result.gasUsed = BigDecimal.valueOf(100000);

                // Cache transaction
                TransactionResult txResult = new TransactionResult();
                txResult.transactionHash = result.transactionHash;
                txResult.status = TransactionExecutionStatus.CONFIRMED;
                txResult.actualGasUsed = result.gasUsed;
                txResult.actualFee = result.gasUsed.multiply(BigDecimal.valueOf(50)).divide(BigDecimal.valueOf(1e9), 18, RoundingMode.HALF_UP);
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
        return Multi.createFrom().ticks().every(Duration.ofSeconds(2))
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
            info.miner = "0x0000000000000000000000000000000000000001"; // Validator address
            info.difficulty = BigDecimal.ZERO; // PoS doesn't use difficulty
            info.totalDifficulty = BigDecimal.ZERO;
            info.gasLimit = 30000000; // Block gas limit
            info.gasUsed = 15000000; // Mock usage
            info.transactionCount = 200;
            info.transactionHashes = new ArrayList<>();
            info.extraData = new HashMap<>();

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
     * Validates Ethereum-compatible address
     */
    @Override
    public Uni<AddressValidationResult> validateAddress(String address) {
        return Uni.createFrom().item(() -> {
            AddressValidationResult result = new AddressValidationResult();
            result.address = address;
            result.isValid = isValidAddress(address);
            result.format = AddressFormat.ETHEREUM_CHECKSUM;
            result.normalizedAddress = address != null ? address.toLowerCase() : null;
            result.validationMessage = result.isValid ? "Valid Ethereum address" : "Invalid address format";

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
                health.networkHashRate = 0; // PoS doesn't use hash rate
                health.activePeers = activePeers.get();
                health.networkUtilization = 0.35;
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
            stats.averageTransactionTime = 2500; // ~2.5 seconds for Polygon
            stats.averageConfirmationTime = CONFIRMATION_BLOCKS * BLOCK_TIME_MS; // ~256 seconds
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
            estimatedGas = BigDecimal.valueOf(100000); // Average contract call
        } else if (transaction.transactionType == TransactionType.CONTRACT_DEPLOY) {
            estimatedGas = BigDecimal.valueOf(2000000); // Contract deployment
        } else {
            estimatedGas = BigDecimal.valueOf(50000); // Default
        }

        estimate.estimatedGas = estimatedGas;

        // Polygon fees are very low
        NetworkFeeInfo feeInfo = getNetworkFeeInfoSync();
        estimate.gasPrice = feeInfo.standardGasPrice;
        estimate.maxFeePerGas = feeInfo.fastGasPrice;
        estimate.maxPriorityFeePerGas = BigDecimal.valueOf(2);

        // Calculate total fee (in MATIC)
        BigDecimal totalFeeGwei = estimatedGas.multiply(estimate.gasPrice).divide(BigDecimal.valueOf(1e9), 18, RoundingMode.HALF_UP);
        estimate.totalFee = totalFeeGwei;

        // Convert to USD (1 MATIC ≈ $1)
        estimate.totalFeeUSD = totalFeeGwei;

        estimate.feeSpeed = FeeSpeed.STANDARD;
        estimate.estimatedConfirmationTime = Duration.ofSeconds(BLOCK_TIME_MS / 1000 * 5); // ~10 seconds

        return estimate;
    }

    private NetworkFeeInfo getNetworkFeeInfoSync() {
        NetworkFeeInfo feeInfo = new NetworkFeeInfo();
        feeInfo.safeLowGasPrice = BigDecimal.valueOf(30);
        feeInfo.standardGasPrice = BigDecimal.valueOf(50);
        feeInfo.fastGasPrice = BigDecimal.valueOf(100);
        feeInfo.instantGasPrice = BigDecimal.valueOf(150);
        feeInfo.baseFeePerGas = BigDecimal.valueOf(25);
        feeInfo.networkUtilization = 0.35;
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
