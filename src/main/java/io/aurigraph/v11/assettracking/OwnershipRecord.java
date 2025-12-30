package io.aurigraph.v11.assettracking;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

/**
 * Ownership Record Data Transfer Object
 *
 * Represents a single ownership record in an asset's ownership history chain.
 * Tracks the owner, acquisition date, disposal date, ownership percentage, and transaction hash.
 *
 * @version 1.0.0
 * @author Aurigraph V11 Development Team
 */
public class OwnershipRecord {

    @JsonProperty("owner")
    private String owner;

    @JsonProperty("acquisitionDate")
    private Instant acquisitionDate;

    @JsonProperty("disposalDate")
    private Instant disposalDate;

    @JsonProperty("percentage")
    private Double percentage;

    @JsonProperty("txHash")
    private String txHash;

    // Constructors
    public OwnershipRecord() {
    }

    public OwnershipRecord(String owner, Instant acquisitionDate, Double percentage, String txHash) {
        this.owner = owner;
        this.acquisitionDate = acquisitionDate;
        this.percentage = percentage;
        this.txHash = txHash;
    }

    // Getters and Setters
    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public Instant getAcquisitionDate() {
        return acquisitionDate;
    }

    public void setAcquisitionDate(Instant acquisitionDate) {
        this.acquisitionDate = acquisitionDate;
    }

    public Instant getDisposalDate() {
        return disposalDate;
    }

    public void setDisposalDate(Instant disposalDate) {
        this.disposalDate = disposalDate;
    }

    public Double getPercentage() {
        return percentage;
    }

    public void setPercentage(Double percentage) {
        this.percentage = percentage;
    }

    public String getTxHash() {
        return txHash;
    }

    public void setTxHash(String txHash) {
        this.txHash = txHash;
    }

    /**
     * Check if this ownership record is currently active
     * (acquisition date is set and no disposal date, or disposal date is in the future)
     *
     * @return true if ownership is currently active
     */
    public boolean isActive() {
        if (acquisitionDate == null) {
            return false;
        }
        if (disposalDate == null) {
            return true;
        }
        return disposalDate.isAfter(Instant.now());
    }

    /**
     * Get ownership duration in days
     *
     * @return Duration in days, or -1 if acquisition date is null
     */
    public long getDurationDays() {
        if (acquisitionDate == null) {
            return -1;
        }
        Instant end = disposalDate != null ? disposalDate : Instant.now();
        return java.time.temporal.ChronoUnit.DAYS.between(acquisitionDate, end);
    }

    @Override
    public String toString() {
        return "OwnershipRecord{" +
                "owner='" + owner + '\'' +
                ", acquisitionDate=" + acquisitionDate +
                ", disposalDate=" + disposalDate +
                ", percentage=" + percentage +
                ", txHash='" + txHash + '\'' +
                ", active=" + isActive() +
                ", durationDays=" + getDurationDays() +
                '}';
    }
}
