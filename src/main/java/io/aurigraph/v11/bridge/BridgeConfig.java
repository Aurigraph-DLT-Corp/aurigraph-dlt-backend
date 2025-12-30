package io.aurigraph.v11.bridge;

import java.math.BigDecimal;
import java.time.Duration;

/**
 * Bridge Configuration - Configuration management for cross-chain bridge
 *
 * Centralizes all bridge configuration parameters:
 * - Multi-signature settings
 * - Atomic swap timeouts
 * - Fee structures
 * - Security thresholds
 *
 * @author Aurigraph V11 Bridge Team
 * @version 11.0.0
 */
public class BridgeConfig {

    // Multi-signature configuration
    private final int multiSigThreshold;
    private final int totalValidators;
    private final boolean multiSigEnabled;

    // Atomic swap configuration
    private final boolean atomicSwapEnabled;
    private final long atomicSwapTimeoutMs;
    private final long htlcLockTime;

    // Fee configuration
    private final double bridgeFeePercentage;
    private final BigDecimal minBridgeFee;
    private final BigDecimal maxBridgeFee;

    // Performance configuration
    private final int requiredConfirmations;
    private final Duration processingTimeout;
    private final int maxRetries;

    // Security configuration
    private final boolean rateLimitEnabled;
    private final int maxBridgesPerHour;
    private final BigDecimal maxSingleTransferAmount;

    private BridgeConfig(Builder builder) {
        this.multiSigThreshold = builder.multiSigThreshold;
        this.totalValidators = builder.totalValidators;
        this.multiSigEnabled = builder.multiSigEnabled;
        this.atomicSwapEnabled = builder.atomicSwapEnabled;
        this.atomicSwapTimeoutMs = builder.atomicSwapTimeoutMs;
        this.htlcLockTime = builder.htlcLockTime;
        this.bridgeFeePercentage = builder.bridgeFeePercentage;
        this.minBridgeFee = builder.minBridgeFee;
        this.maxBridgeFee = builder.maxBridgeFee;
        this.requiredConfirmations = builder.requiredConfirmations;
        this.processingTimeout = builder.processingTimeout;
        this.maxRetries = builder.maxRetries;
        this.rateLimitEnabled = builder.rateLimitEnabled;
        this.maxBridgesPerHour = builder.maxBridgesPerHour;
        this.maxSingleTransferAmount = builder.maxSingleTransferAmount;
    }

    // Getters
    public int getMultiSigThreshold() { return multiSigThreshold; }
    public int getTotalValidators() { return totalValidators; }
    public boolean isMultiSigEnabled() { return multiSigEnabled; }
    public boolean isAtomicSwapEnabled() { return atomicSwapEnabled; }
    public long getAtomicSwapTimeoutMs() { return atomicSwapTimeoutMs; }
    public long getHtlcLockTime() { return htlcLockTime; }
    public double getBridgeFeePercentage() { return bridgeFeePercentage; }
    public BigDecimal getMinBridgeFee() { return minBridgeFee; }
    public BigDecimal getMaxBridgeFee() { return maxBridgeFee; }
    public int getRequiredConfirmations() { return requiredConfirmations; }
    public Duration getProcessingTimeout() { return processingTimeout; }
    public int getMaxRetries() { return maxRetries; }
    public boolean isRateLimitEnabled() { return rateLimitEnabled; }
    public int getMaxBridgesPerHour() { return maxBridgesPerHour; }
    public BigDecimal getMaxSingleTransferAmount() { return maxSingleTransferAmount; }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private int multiSigThreshold = 2;
        private int totalValidators = 3;
        private boolean multiSigEnabled = true;
        private boolean atomicSwapEnabled = true;
        private long atomicSwapTimeoutMs = 300000;
        private long htlcLockTime = 3600;
        private double bridgeFeePercentage = 0.1;
        private BigDecimal minBridgeFee = new BigDecimal("0.001");
        private BigDecimal maxBridgeFee = new BigDecimal("100");
        private int requiredConfirmations = 12;
        private Duration processingTimeout = Duration.ofMinutes(10);
        private int maxRetries = 3;
        private boolean rateLimitEnabled = true;
        private int maxBridgesPerHour = 100;
        private BigDecimal maxSingleTransferAmount = new BigDecimal("1000000");

        public Builder multiSigThreshold(int val) { multiSigThreshold = val; return this; }
        public Builder totalValidators(int val) { totalValidators = val; return this; }
        public Builder multiSigEnabled(boolean val) { multiSigEnabled = val; return this; }
        public Builder atomicSwapEnabled(boolean val) { atomicSwapEnabled = val; return this; }
        public Builder atomicSwapTimeoutMs(long val) { atomicSwapTimeoutMs = val; return this; }
        public Builder htlcLockTime(long val) { htlcLockTime = val; return this; }
        public Builder bridgeFeePercentage(double val) { bridgeFeePercentage = val; return this; }
        public Builder minBridgeFee(BigDecimal val) { minBridgeFee = val; return this; }
        public Builder maxBridgeFee(BigDecimal val) { maxBridgeFee = val; return this; }
        public Builder requiredConfirmations(int val) { requiredConfirmations = val; return this; }
        public Builder processingTimeout(Duration val) { processingTimeout = val; return this; }
        public Builder maxRetries(int val) { maxRetries = val; return this; }
        public Builder rateLimitEnabled(boolean val) { rateLimitEnabled = val; return this; }
        public Builder maxBridgesPerHour(int val) { maxBridgesPerHour = val; return this; }
        public Builder maxSingleTransferAmount(BigDecimal val) { maxSingleTransferAmount = val; return this; }

        public BridgeConfig build() {
            return new BridgeConfig(this);
        }
    }
}
