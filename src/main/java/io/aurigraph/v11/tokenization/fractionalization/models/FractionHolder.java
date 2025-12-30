package io.aurigraph.v11.tokenization.fractionalization.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Represents a holder of fractional ownership
 * Tracks holdings, distributions, and governance participation
 *
 * @author Backend Development Agent (BDA)
 * @since Phase 1 Foundation - Fractionalization
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FractionHolder {

    /**
     * Holder wallet address
     */
    private String holderAddress;

    /**
     * Number of fractions owned
     */
    private long fractionCount;

    /**
     * Ownership percentage
     */
    private BigDecimal ownershipPercentage;

    /**
     * Total value of holdings (fractionCount * fractionValue)
     */
    private BigDecimal holdingValue;

    /**
     * Total dividends received
     */
    private BigDecimal totalDividendsReceived;

    /**
     * Acquisition timestamp
     */
    private Instant acquisitionDate;

    /**
     * Last distribution received
     */
    private Instant lastDistribution;

    /**
     * Tier level (for tiered distribution model)
     */
    private TierLevel tierLevel;

    /**
     * Governance participation score (0-100)
     */
    private int governanceScore;

    /**
     * Environmental/social impact score (for consciousness-weighted model)
     */
    private BigDecimal impactScore;

    /**
     * Holding duration in days
     */
    private long holdingDurationDays;

    /**
     * Tier levels for tiered distribution
     */
    public enum TierLevel {
        TIER_1(0, 1000),           // 0-1,000 fractions
        TIER_2(1001, 10000),       // 1,001-10K fractions
        TIER_3(10001, 100000),     // 10K-100K fractions
        INSTITUTIONAL(100001, Long.MAX_VALUE); // 100K+ fractions

        private final long minFractions;
        private final long maxFractions;

        TierLevel(long minFractions, long maxFractions) {
            this.minFractions = minFractions;
            this.maxFractions = maxFractions;
        }

        public long getMinFractions() {
            return minFractions;
        }

        public long getMaxFractions() {
            return maxFractions;
        }

        public static TierLevel fromFractionCount(long fractionCount) {
            if (fractionCount <= 1000) return TIER_1;
            if (fractionCount <= 10000) return TIER_2;
            if (fractionCount <= 100000) return TIER_3;
            return INSTITUTIONAL;
        }
    }

    /**
     * Calculate current tier level based on fraction count
     */
    public void updateTierLevel() {
        this.tierLevel = TierLevel.fromFractionCount(fractionCount);
    }

    /**
     * Update holding duration
     */
    public void updateHoldingDuration() {
        if (acquisitionDate != null) {
            long durationMillis = Instant.now().toEpochMilli() - acquisitionDate.toEpochMilli();
            this.holdingDurationDays = durationMillis / (1000 * 60 * 60 * 24);
        }
    }

    /**
     * Add fractions to holding
     */
    public void addFractions(long count, BigDecimal fractionValue) {
        this.fractionCount += count;
        this.holdingValue = BigDecimal.valueOf(fractionCount).multiply(fractionValue);
        updateTierLevel();
    }

    /**
     * Remove fractions from holding
     */
    public void removeFractions(long count, BigDecimal fractionValue) {
        if (count > this.fractionCount) {
            throw new IllegalArgumentException("Insufficient fractions");
        }
        this.fractionCount -= count;
        this.holdingValue = BigDecimal.valueOf(fractionCount).multiply(fractionValue);
        updateTierLevel();
    }

    /**
     * Record dividend payment
     */
    public void recordDividend(BigDecimal amount) {
        this.totalDividendsReceived = this.totalDividendsReceived.add(amount);
        this.lastDistribution = Instant.now();
    }
}
