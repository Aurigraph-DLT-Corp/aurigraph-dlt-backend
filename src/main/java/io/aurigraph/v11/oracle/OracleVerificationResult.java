package io.aurigraph.v11.oracle;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Oracle Verification Result DTO
 * Contains the result of multi-oracle consensus verification
 *
 * @author Aurigraph V11 - Backend Development Agent
 * @version 11.0.0
 * @sprint Sprint 16 - Oracle Verification System (AV11-483)
 */
public class OracleVerificationResult {

    @JsonProperty("verification_id")
    private String verificationId;

    @JsonProperty("asset_id")
    private String assetId;

    @JsonProperty("claimed_value")
    private BigDecimal claimedValue;

    @JsonProperty("median_price")
    private BigDecimal medianPrice;

    @JsonProperty("consensus_reached")
    private boolean consensusReached;

    @JsonProperty("consensus_percentage")
    private double consensusPercentage;

    @JsonProperty("price_variance")
    private BigDecimal priceVariance;

    @JsonProperty("is_within_tolerance")
    private boolean isWithinTolerance;

    @JsonProperty("tolerance_percentage")
    private double tolerancePercentage;

    @JsonProperty("total_oracles_queried")
    private int totalOraclesQueried;

    @JsonProperty("successful_oracles")
    private int successfulOracles;

    @JsonProperty("failed_oracles")
    private int failedOracles;

    @JsonProperty("oracle_responses")
    private List<OraclePriceData> oracleResponses;

    @JsonProperty("verification_timestamp")
    private Instant verificationTimestamp;

    @JsonProperty("total_verification_time_ms")
    private long totalVerificationTimeMs;

    @JsonProperty("verification_status")
    private String verificationStatus; // "APPROVED", "REJECTED", "INSUFFICIENT_DATA"

    @JsonProperty("rejection_reason")
    private String rejectionReason;

    @JsonProperty("min_price")
    private BigDecimal minPrice;

    @JsonProperty("max_price")
    private BigDecimal maxPrice;

    @JsonProperty("average_price")
    private BigDecimal averagePrice;

    @JsonProperty("standard_deviation")
    private BigDecimal standardDeviation;

    // Constructors
    public OracleVerificationResult() {
        this.verificationTimestamp = Instant.now();
    }

    public OracleVerificationResult(String assetId, BigDecimal claimedValue) {
        this();
        this.assetId = assetId;
        this.claimedValue = claimedValue;
    }

    // Getters and Setters
    public String getVerificationId() {
        return verificationId;
    }

    public void setVerificationId(String verificationId) {
        this.verificationId = verificationId;
    }

    public String getAssetId() {
        return assetId;
    }

    public void setAssetId(String assetId) {
        this.assetId = assetId;
    }

    public BigDecimal getClaimedValue() {
        return claimedValue;
    }

    public void setClaimedValue(BigDecimal claimedValue) {
        this.claimedValue = claimedValue;
    }

    public BigDecimal getMedianPrice() {
        return medianPrice;
    }

    public void setMedianPrice(BigDecimal medianPrice) {
        this.medianPrice = medianPrice;
    }

    public boolean isConsensusReached() {
        return consensusReached;
    }

    public void setConsensusReached(boolean consensusReached) {
        this.consensusReached = consensusReached;
    }

    public double getConsensusPercentage() {
        return consensusPercentage;
    }

    public void setConsensusPercentage(double consensusPercentage) {
        this.consensusPercentage = consensusPercentage;
    }

    public BigDecimal getPriceVariance() {
        return priceVariance;
    }

    public void setPriceVariance(BigDecimal priceVariance) {
        this.priceVariance = priceVariance;
    }

    public boolean isWithinTolerance() {
        return isWithinTolerance;
    }

    public void setWithinTolerance(boolean withinTolerance) {
        isWithinTolerance = withinTolerance;
    }

    public double getTolerancePercentage() {
        return tolerancePercentage;
    }

    public void setTolerancePercentage(double tolerancePercentage) {
        this.tolerancePercentage = tolerancePercentage;
    }

    public int getTotalOraclesQueried() {
        return totalOraclesQueried;
    }

    public void setTotalOraclesQueried(int totalOraclesQueried) {
        this.totalOraclesQueried = totalOraclesQueried;
    }

    public int getSuccessfulOracles() {
        return successfulOracles;
    }

    public void setSuccessfulOracles(int successfulOracles) {
        this.successfulOracles = successfulOracles;
    }

    public int getFailedOracles() {
        return failedOracles;
    }

    public void setFailedOracles(int failedOracles) {
        this.failedOracles = failedOracles;
    }

    public List<OraclePriceData> getOracleResponses() {
        return oracleResponses;
    }

    public void setOracleResponses(List<OraclePriceData> oracleResponses) {
        this.oracleResponses = oracleResponses;
    }

    public Instant getVerificationTimestamp() {
        return verificationTimestamp;
    }

    public void setVerificationTimestamp(Instant verificationTimestamp) {
        this.verificationTimestamp = verificationTimestamp;
    }

    public long getTotalVerificationTimeMs() {
        return totalVerificationTimeMs;
    }

    public void setTotalVerificationTimeMs(long totalVerificationTimeMs) {
        this.totalVerificationTimeMs = totalVerificationTimeMs;
    }

    public String getVerificationStatus() {
        return verificationStatus;
    }

    public void setVerificationStatus(String verificationStatus) {
        this.verificationStatus = verificationStatus;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public BigDecimal getMinPrice() {
        return minPrice;
    }

    public void setMinPrice(BigDecimal minPrice) {
        this.minPrice = minPrice;
    }

    public BigDecimal getMaxPrice() {
        return maxPrice;
    }

    public void setMaxPrice(BigDecimal maxPrice) {
        this.maxPrice = maxPrice;
    }

    public BigDecimal getAveragePrice() {
        return averagePrice;
    }

    public void setAveragePrice(BigDecimal averagePrice) {
        this.averagePrice = averagePrice;
    }

    public BigDecimal getStandardDeviation() {
        return standardDeviation;
    }

    public void setStandardDeviation(BigDecimal standardDeviation) {
        this.standardDeviation = standardDeviation;
    }

    @Override
    public String toString() {
        return String.format(
            "OracleVerificationResult{assetId='%s', medianPrice=%s, consensusReached=%b, " +
            "consensusPercentage=%.2f%%, verificationStatus='%s', successfulOracles=%d/%d}",
            assetId, medianPrice, consensusReached, consensusPercentage,
            verificationStatus, successfulOracles, totalOraclesQueried
        );
    }
}
