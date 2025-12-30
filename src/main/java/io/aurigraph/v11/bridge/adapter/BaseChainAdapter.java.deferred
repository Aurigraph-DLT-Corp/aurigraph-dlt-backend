package io.aurigraph.v11.bridge.adapter;

import io.aurigraph.v11.bridge.ChainAdapter;
import io.aurigraph.v11.bridge.ChainInfo;
import io.aurigraph.v11.bridge.exception.BridgeException;
import io.aurigraph.v11.bridge.BridgeTransaction;
import io.aurigraph.v11.bridge.BridgeEvent;
import io.aurigraph.v11.bridge.model.BridgeChainConfig;
import io.aurigraph.v11.bridge.model.HTLCRequest;
import io.aurigraph.v11.bridge.persistence.BridgeTransactionRepository;
import io.quarkus.runtime.annotations.RegisterForReflection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.inject.Inject;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

/**
 * Abstract base class for all blockchain chain adapters
 * Defines the contract that all chain-specific implementations must follow
 *
 * Provides common functionality:
 * - Retry logic with exponential backoff
 * - Fee estimation
 * - Event filtering
 * - Connection validation
 *
 * @author Claude Code - Priority 3 Implementation
 * @version 1.0.0
 */
public abstract class BaseChainAdapter implements ChainAdapter {

    protected static final Logger logger = LoggerFactory.getLogger(BaseChainAdapter.class);

    // Chain configuration
    protected String chainId;
    protected String chainName;
    protected String rpcUrl;
    protected long blockTime; // milliseconds
    protected int confirmationsRequired;
    protected Map<String, String> contractAddresses;
    protected BigDecimal minBridgeAmount;
    protected BigDecimal maxBridgeAmount;
    protected BigDecimal baseFeePercent;

    // Dependencies (optional - can be null)
    @Inject
    protected BridgeTransactionRepository bridgeRepository;

    // Price oracle for fee estimation (optional)
    // @Inject(optional = true)
    // protected PriceOracleService priceOracle;

    // Retry configuration
    protected static final int MAX_RETRIES = 5;
    protected static final long BASE_RETRY_DELAY_MS = 1000;

    /**
     * Initialize chain-specific connection
     * Called by factory after dependency injection
     */
    protected abstract void initializeChainConnection() throws Exception;

    /**
     * Get chain metadata (block height, time, network info)
     */
    public abstract ChainInfo getChainInfo() throws Exception;

    /**
     * Get balance of an address (native or token)
     */
    public abstract BigDecimal getBalance(String address, String tokenAddress) throws Exception;

    /**
     * Deploy HTLC contract on this chain
     */
    @Override
    public abstract BridgeTransaction deployHTLC(HTLCRequest request) throws Exception;

    /**
     * Lock funds in HTLC contract
     */
    @Override
    public abstract void lockFunds(String htlcAddress, String tokenAddress, BigDecimal amount)
            throws Exception;

    /**
     * Unlock funds from HTLC with secret
     */
    @Override
    public abstract void unlockFunds(String htlcAddress, String secret) throws Exception;

    /**
     * Watch for events on this chain
     */
    @Override
    public abstract List<BridgeEvent> watchForEvents(String contractAddress, long fromBlock)
            throws Exception;

    /**
     * Initialize this adapter with configuration
     */
    public void initialize(BridgeChainConfig config) throws Exception {
        this.chainId = config.getChainId();
        this.chainName = config.getChainName();
        this.rpcUrl = config.getRpcUrl();
        this.blockTime = config.getBlockTime();
        this.confirmationsRequired = config.getConfirmationsRequired();
        this.contractAddresses = config.getContractAddresses();
        this.minBridgeAmount = config.getMinBridgeAmount();
        this.maxBridgeAmount = config.getMaxBridgeAmount();
        this.baseFeePercent = config.getBaseFeePercent();

        // Initialize chain-specific connection
        initializeChainConnection();
        logger.info("Adapter initialized for chain: {}", chainName);
    }

    /**
     * Validate connection to chain
     * Can be overridden by subclasses for custom validation
     */
    public void validateChainConnection() throws Exception {
        try {
            ChainInfo info = getChainInfo();
            if (info == null || info.getBlockHeight() <= 0) {
                throw new BridgeException("Invalid chain info returned: " + info);
            }
            logger.debug("Chain {} connection validated. Height: {}", chainName, info.getBlockHeight());
        } catch (Exception e) {
            throw new BridgeException("Chain " + chainName + " connection validation failed", e);
        }
    }

    /**
     * Execute operation with retry logic
     * Uses exponential backoff: 1s, 2s, 4s, 8s, 16s
     */
    protected <T> T executeWithRetry(String operationName, Callable<T> operation) throws Exception {
        Exception lastException = null;

        for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
            try {
                return operation.call();
            } catch (IOException | TimeoutException e) {
                lastException = e;

                if (attempt < MAX_RETRIES - 1) {
                    long delayMs = BASE_RETRY_DELAY_MS * (long) Math.pow(2, attempt);
                    logger.warn(
                        "Operation {} failed on chain {} (attempt {}/{}). Retrying in {} ms",
                        operationName, chainName, attempt + 1, MAX_RETRIES, delayMs
                    );
                    Thread.sleep(delayMs);
                } else {
                    logger.error(
                        "Operation {} failed on chain {} after {} attempts",
                        operationName, chainName, MAX_RETRIES
                    );
                }
            }
        }

        throw new BridgeException(
            "Operation " + operationName + " failed after " + MAX_RETRIES + " attempts on " + chainName,
            lastException
        );
    }

    /**
     * Estimate bridge fee for given amount
     * Base fee: baseFeePercent of amount
     * + per-transaction fee (chain-specific)
     */
    protected BigDecimal estimateBridgeFee(BigDecimal amount) {
        // Calculate percentage-based fee
        BigDecimal percentFee = amount.multiply(baseFeePercent);

        // Get per-transaction fee
        BigDecimal txFee = new BigDecimal(getBaseFeePerTransaction());

        // Combine percentage fee and transaction fee
        return percentFee.add(txFee);
    }

    /**
     * Get base fee per transaction for this chain
     * Override in subclasses for chain-specific values
     */
    protected abstract BigDecimal getBaseFeePerTransaction();

    /**
     * Filter events by timestamp
     */
    protected List<BridgeEvent> filterEventsBySince(List<BridgeEvent> events, long timestamp) {
        return events.stream()
            .filter(e -> e.getTimestamp() >= timestamp)
            .collect(Collectors.toList());
    }

    /**
     * Filter events by type
     */
    protected List<BridgeEvent> filterEventsByType(List<BridgeEvent> events, String eventType) {
        return events.stream()
            .filter(e -> eventType.equals(e.getEventType()))
            .collect(Collectors.toList());
    }

    /**
     * Validate bridge amount is within limits
     */
    protected void validateBridgeAmount(BigDecimal amount) throws BridgeException {
        if (amount.compareTo(minBridgeAmount) < 0) {
            throw new BridgeException(
                "Bridge amount " + amount + " is below minimum " + minBridgeAmount + " on " + chainName
            );
        }
        if (amount.compareTo(maxBridgeAmount) > 0) {
            throw new BridgeException(
                "Bridge amount " + amount + " exceeds maximum " + maxBridgeAmount + " on " + chainName
            );
        }
    }

    /**
     * Wait for confirmation blocks
     */
    protected void waitForConfirmations(long startHeight) throws Exception {
        long currentHeight = startHeight;
        long requiredHeight = startHeight + confirmationsRequired;

        while (currentHeight < requiredHeight) {
            ChainInfo info = getChainInfo();
            currentHeight = info.getBlockHeight();

            if (currentHeight < requiredHeight) {
                long remaining = requiredHeight - currentHeight;
                long estimatedWaitMs = remaining * blockTime;
                logger.debug(
                    "Waiting for confirmations on {}: {}/{} blocks, ~{} ms",
                    chainName, currentHeight - startHeight, confirmationsRequired, estimatedWaitMs
                );
                Thread.sleep(Math.min(blockTime, 30000)); // Check every blockTime or 30s, whichever is less
            }
        }
    }

    /**
     * Persist bridge transaction to repository
     */
    protected void persistBridgeTransaction(BridgeTransaction transaction) throws Exception {
        try {
            bridgeRepository.save(transaction);
        } catch (Exception e) {
            logger.error("Failed to persist bridge transaction: {}", transaction.getId(), e);
            throw new BridgeException("Failed to persist bridge transaction", e);
        }
    }

    @Override
    public String getChainName() {
        return chainName;
    }

    @Override
    public String getChainId() {
        return chainId;
    }

    @Override
    public String getRpcUrl() {
        return rpcUrl;
    }

    @Override
    public Map<String, String> getContractAddresses() {
        return new HashMap<>(contractAddresses);
    }
}
