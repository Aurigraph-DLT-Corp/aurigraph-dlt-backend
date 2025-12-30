package io.aurigraph.v11.tokenization.fractionalization.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Represents a fractionalized asset
 * Large, indivisible assets broken into tradeable units
 *
 * @author Backend Development Agent (BDA)
 * @since Phase 1 Foundation - Fractionalization
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FractionalAsset {

    /**
     * Primary token ID (immutable, SHA3-256 hash reference)
     */
    private String primaryTokenId;

    /**
     * Asset ID (globally unique)
     */
    private String assetId;

    /**
     * Asset type (REAL_ESTATE, ART, IP, INFRASTRUCTURE)
     */
    private String assetType;

    /**
     * Asset description
     */
    private String description;

    /**
     * Initial asset valuation (third-party verified)
     */
    private BigDecimal initialValuation;

    /**
     * Current asset valuation
     */
    private BigDecimal currentValuation;

    /**
     * Total number of fractions (2-10M+)
     */
    private long totalFractions;

    /**
     * Value per fraction
     */
    private BigDecimal fractionValue;

    /**
     * Fractionalization contract address
     */
    private String contractAddress;

    /**
     * Distribution model (WATERFALL, CONSCIOUSNESS, TIERED, PRO_RATA)
     */
    private DistributionModel distributionModel;

    /**
     * Revaluation parameters
     */
    private RevaluationConfig revaluationConfig;

    /**
     * Custody information
     */
    private CustodyInfo custodyInfo;

    /**
     * Third-party verification provider
     */
    private String verificationProvider;

    /**
     * Merkle root (cryptographic proof of ownership)
     */
    private String merkleRoot;

    /**
     * Creation timestamp
     */
    private Instant createdAt;

    /**
     * Last valuation update timestamp
     */
    private Instant lastValuationUpdate;

    /**
     * Owner address
     */
    private String ownerAddress;

    /**
     * Fractionalization status
     */
    private FractionalizationStatus status;

    /**
     * Historical valuations
     */
    private List<ValuationHistory> valuationHistory;

    /**
     * Calculate percentage change from initial valuation
     */
    public BigDecimal calculateValuationChange() {
        if (initialValuation == null || initialValuation.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return currentValuation.subtract(initialValuation)
            .divide(initialValuation, 4, java.math.RoundingMode.HALF_UP)
            .multiply(BigDecimal.valueOf(100));
    }

    /**
     * Check if valuation change requires verification
     */
    public boolean requiresVerification() {
        BigDecimal changePercent = calculateValuationChange().abs();
        return changePercent.compareTo(revaluationConfig.getAllowedChangeThreshold()) > 0;
    }

    /**
     * Check if valuation change is prohibited
     */
    public boolean isProhibitedChange() {
        BigDecimal changePercent = calculateValuationChange().abs();
        return changePercent.compareTo(revaluationConfig.getProhibitedChangeThreshold()) > 0;
    }

    /**
     * Distribution models
     */
    public enum DistributionModel {
        WATERFALL,
        CONSCIOUSNESS_WEIGHTED,
        TIERED,
        PRO_RATA
    }

    /**
     * Fractionalization status
     */
    public enum FractionalizationStatus {
        ACTIVE,
        PAUSED,
        REVALUING,
        LIQUIDATING,
        TERMINATED
    }

    /**
     * Revaluation configuration
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RevaluationConfig {
        private RevaluationFrequency frequency;
        private BigDecimal allowedChangeThreshold; // <10%
        private BigDecimal restrictedChangeThreshold; // 10-50%
        private BigDecimal prohibitedChangeThreshold; // >50%
        private boolean requireOracleVerification;
        private int governanceApprovalThresholdPercent; // Default 66%

        public enum RevaluationFrequency {
            MONTHLY,
            QUARTERLY,
            SEMI_ANNUAL,
            ANNUAL
        }
    }

    /**
     * Custody information
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CustodyInfo {
        private String custodianName;
        private String custodianAddress;
        private String custodialAgreementHash;
        private String insuranceProvider;
        private BigDecimal insuranceCoverage;
        private String physicalLocation;
        private Instant lastAuditDate;
    }

    /**
     * Valuation history entry
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ValuationHistory {
        private Instant timestamp;
        private BigDecimal valuation;
        private String verificationSource;
        private BigDecimal changePercentage;
        private String merkleProof;
    }
}
