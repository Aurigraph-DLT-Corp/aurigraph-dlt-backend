package io.aurigraph.v11.oracle;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Oracle Verification Entity
 * Persists oracle verification results to database
 *
 * @author Aurigraph V11 - Backend Development Agent
 * @version 11.0.0
 * @sprint Sprint 16 - Oracle Verification System (AV11-483)
 */
@Entity
@Table(name = "oracle_verifications",
    indexes = {
        @Index(name = "idx_verification_id", columnList = "verificationId"),
        @Index(name = "idx_asset_id", columnList = "assetId"),
        @Index(name = "idx_verification_timestamp", columnList = "verificationTimestamp"),
        @Index(name = "idx_verification_status", columnList = "verificationStatus")
    }
)
public class OracleVerificationEntity extends PanacheEntity {

    @Column(nullable = false, unique = true, length = 64)
    public String verificationId;

    @Column(nullable = false, length = 64)
    public String assetId;

    @Column(nullable = false, precision = 38, scale = 18)
    public BigDecimal claimedValue;

    @Column(precision = 38, scale = 18)
    public BigDecimal medianPrice;

    @Column(nullable = false)
    public boolean consensusReached;

    @Column(nullable = false)
    public double consensusPercentage;

    @Column(precision = 38, scale = 18)
    public BigDecimal priceVariance;

    @Column(nullable = false)
    public boolean isWithinTolerance;

    @Column(nullable = false)
    public double tolerancePercentage;

    @Column(nullable = false)
    public int totalOraclesQueried;

    @Column(nullable = false)
    public int successfulOracles;

    @Column(nullable = false)
    public int failedOracles;

    @Column(nullable = false)
    public Instant verificationTimestamp;

    @Column(nullable = false)
    public long totalVerificationTimeMs;

    @Column(nullable = false, length = 50)
    public String verificationStatus; // "APPROVED", "REJECTED", "INSUFFICIENT_DATA"

    @Column(length = 500)
    public String rejectionReason;

    @Column(precision = 38, scale = 18)
    public BigDecimal minPrice;

    @Column(precision = 38, scale = 18)
    public BigDecimal maxPrice;

    @Column(precision = 38, scale = 18)
    public BigDecimal averagePrice;

    @Column(precision = 38, scale = 18)
    public BigDecimal standardDeviation;

    @Column(nullable = false)
    public Instant createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (verificationTimestamp == null) {
            verificationTimestamp = Instant.now();
        }
    }

    /**
     * Convert entity to DTO
     */
    public OracleVerificationResult toDTO() {
        OracleVerificationResult result = new OracleVerificationResult();
        result.setVerificationId(this.verificationId);
        result.setAssetId(this.assetId);
        result.setClaimedValue(this.claimedValue);
        result.setMedianPrice(this.medianPrice);
        result.setConsensusReached(this.consensusReached);
        result.setConsensusPercentage(this.consensusPercentage);
        result.setPriceVariance(this.priceVariance);
        result.setWithinTolerance(this.isWithinTolerance);
        result.setTolerancePercentage(this.tolerancePercentage);
        result.setTotalOraclesQueried(this.totalOraclesQueried);
        result.setSuccessfulOracles(this.successfulOracles);
        result.setFailedOracles(this.failedOracles);
        result.setVerificationTimestamp(this.verificationTimestamp);
        result.setTotalVerificationTimeMs(this.totalVerificationTimeMs);
        result.setVerificationStatus(this.verificationStatus);
        result.setRejectionReason(this.rejectionReason);
        result.setMinPrice(this.minPrice);
        result.setMaxPrice(this.maxPrice);
        result.setAveragePrice(this.averagePrice);
        result.setStandardDeviation(this.standardDeviation);
        return result;
    }

    /**
     * Create entity from DTO
     */
    public static OracleVerificationEntity fromDTO(OracleVerificationResult result) {
        OracleVerificationEntity entity = new OracleVerificationEntity();
        entity.verificationId = result.getVerificationId();
        entity.assetId = result.getAssetId();
        entity.claimedValue = result.getClaimedValue();
        entity.medianPrice = result.getMedianPrice();
        entity.consensusReached = result.isConsensusReached();
        entity.consensusPercentage = result.getConsensusPercentage();
        entity.priceVariance = result.getPriceVariance();
        entity.isWithinTolerance = result.isWithinTolerance();
        entity.tolerancePercentage = result.getTolerancePercentage();
        entity.totalOraclesQueried = result.getTotalOraclesQueried();
        entity.successfulOracles = result.getSuccessfulOracles();
        entity.failedOracles = result.getFailedOracles();
        entity.verificationTimestamp = result.getVerificationTimestamp();
        entity.totalVerificationTimeMs = result.getTotalVerificationTimeMs();
        entity.verificationStatus = result.getVerificationStatus();
        entity.rejectionReason = result.getRejectionReason();
        entity.minPrice = result.getMinPrice();
        entity.maxPrice = result.getMaxPrice();
        entity.averagePrice = result.getAveragePrice();
        entity.standardDeviation = result.getStandardDeviation();
        return entity;
    }
}
