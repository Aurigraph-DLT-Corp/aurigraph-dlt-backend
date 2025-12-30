package io.aurigraph.v11.oracle.adapter;

import io.aurigraph.v11.oracle.OraclePriceData;
import io.quarkus.logging.Log;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Base Oracle Adapter Implementation
 * Provides common functionality for all oracle adapters
 *
 * @author Aurigraph V11 - Development Agent 4
 * @version 11.0.0
 * @sprint Sprint 16 - Oracle Verification System (AV11-483)
 */
public abstract class BaseOracleAdapter implements OracleAdapter {

    protected final String oracleId;
    protected final String providerName;
    protected final AtomicLong lastUpdateTimestamp;
    protected final AtomicReference<Double> reliabilityScore;
    protected final AtomicLong successfulFetches;
    protected final AtomicLong totalFetches;

    protected BaseOracleAdapter(String oracleId, String providerName) {
        this.oracleId = oracleId;
        this.providerName = providerName;
        this.lastUpdateTimestamp = new AtomicLong(System.currentTimeMillis());
        this.reliabilityScore = new AtomicReference<>(1.0);
        this.successfulFetches = new AtomicLong(0);
        this.totalFetches = new AtomicLong(0);
    }

    @Override
    public String getOracleId() {
        return oracleId;
    }

    @Override
    public String getProviderName() {
        return providerName;
    }

    @Override
    public long getLastUpdateTimestamp() {
        return lastUpdateTimestamp.get();
    }

    @Override
    public double getReliabilityScore() {
        return reliabilityScore.get();
    }

    @Override
    public CompletableFuture<Boolean> isHealthy() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Check if oracle has been updated recently (within 2x update frequency)
                long lastUpdate = getLastUpdateTimestamp();
                long currentTime = System.currentTimeMillis();
                long timeSinceUpdate = currentTime - lastUpdate;
                long maxUpdateGap = getUpdateFrequency() * 2;

                boolean isRecent = timeSinceUpdate < maxUpdateGap;
                boolean hasGoodReliability = getReliabilityScore() > 0.7;

                return isRecent && hasGoodReliability;
            } catch (Exception e) {
                Log.warnf("Health check failed for oracle %s: %s", oracleId, e.getMessage());
                return false;
            }
        });
    }

    @Override
    public CompletableFuture<OraclePriceData> fetchPrice(String assetId) {
        return CompletableFuture.supplyAsync(() -> {
            long startTime = System.currentTimeMillis();
            totalFetches.incrementAndGet();

            OraclePriceData priceData = new OraclePriceData(oracleId, providerName, providerName);
            priceData.setAssetId(assetId);

            try {
                // Call provider-specific implementation
                BigDecimal price = fetchPriceFromProvider(assetId);
                String signature = generateSignature(assetId, price);

                priceData.setPrice(price);
                priceData.setSignature(signature);
                priceData.setStatus("success");
                priceData.setTimestamp(Instant.now());
                priceData.setResponseTimeMs(System.currentTimeMillis() - startTime);

                // Update metrics
                lastUpdateTimestamp.set(System.currentTimeMillis());
                successfulFetches.incrementAndGet();
                updateReliabilityScore();

                Log.debugf("Successfully fetched price from %s for %s: %s", providerName, assetId, price);

            } catch (Exception e) {
                priceData.setStatus("failed");
                priceData.setErrorMessage(e.getMessage());
                priceData.setResponseTimeMs(System.currentTimeMillis() - startTime);

                updateReliabilityScore();
                Log.warnf("Failed to fetch price from %s for %s: %s", providerName, assetId, e.getMessage());
            }

            return priceData;
        });
    }

    /**
     * Provider-specific price fetch implementation
     * Must be implemented by concrete oracle adapters
     *
     * @param assetId The asset identifier
     * @return Price from the oracle provider
     * @throws Exception if fetch fails
     */
    protected abstract BigDecimal fetchPriceFromProvider(String assetId) throws Exception;

    /**
     * Generate signature for price data
     * In production, this would use the oracle's private key
     *
     * @param assetId The asset identifier
     * @param price The price value
     * @return Base64-encoded signature
     */
    protected abstract String generateSignature(String assetId, BigDecimal price);

    /**
     * Update reliability score based on success rate
     */
    protected void updateReliabilityScore() {
        long total = totalFetches.get();
        long successful = successfulFetches.get();

        if (total > 0) {
            double score = (double) successful / total;
            reliabilityScore.set(score);
        }
    }

    /**
     * Get statistics about this oracle adapter
     *
     * @return String with statistics
     */
    public String getStatistics() {
        return String.format(
            "Oracle %s (%s): %d/%d successful fetches (%.1f%%), reliability: %.2f",
            oracleId,
            providerName,
            successfulFetches.get(),
            totalFetches.get(),
            (double) successfulFetches.get() / Math.max(1, totalFetches.get()) * 100,
            reliabilityScore.get()
        );
    }
}
