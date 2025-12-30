package io.aurigraph.v11.oracle.adapter;

import io.aurigraph.v11.oracle.OraclePriceData;
import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;

/**
 * Oracle Adapter Interface
 * Defines the contract for all oracle provider integrations
 *
 * @author Aurigraph V11 - Development Agent 4
 * @version 11.0.0
 * @sprint Sprint 16 - Oracle Verification System (AV11-483)
 */
public interface OracleAdapter {

    /**
     * Get the oracle provider name
     *
     * @return Provider name (e.g., "Chainlink", "Pyth", "Band Protocol")
     */
    String getProviderName();

    /**
     * Get the oracle ID
     *
     * @return Unique oracle identifier
     */
    String getOracleId();

    /**
     * Check if the oracle is healthy and available
     *
     * @return CompletableFuture<Boolean> true if oracle is healthy
     */
    CompletableFuture<Boolean> isHealthy();

    /**
     * Fetch price data for a given asset
     *
     * @param assetId The asset identifier (e.g., "BTC/USD", "ETH/USD")
     * @return CompletableFuture<OraclePriceData> Price data with signature
     */
    CompletableFuture<OraclePriceData> fetchPrice(String assetId);

    /**
     * Get the last update timestamp for this oracle
     *
     * @return Last successful update timestamp in milliseconds
     */
    long getLastUpdateTimestamp();

    /**
     * Get the oracle's reliability score (0.0 to 1.0)
     * Based on historical uptime and accuracy
     *
     * @return Reliability score
     */
    double getReliabilityScore();

    /**
     * Get the oracle's stake weight for consensus voting
     * Higher stake = more voting power
     *
     * @return Stake weight (default 1.0)
     */
    default double getStakeWeight() {
        return 1.0;
    }

    /**
     * Get the oracle's supported asset pairs
     *
     * @return Array of supported asset pair identifiers
     */
    String[] getSupportedAssets();

    /**
     * Get the oracle's update frequency in milliseconds
     *
     * @return Update frequency
     */
    long getUpdateFrequency();
}
