package io.aurigraph.v11.compliance.reporting;

import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * KYC/AML compliance report
 */
public class KYCAMLReport {
    private LocalDate reportDate;
    private int totalIdentities;
    private int activeIdentities;
    private int revokedIdentities;
    private Map<String, Integer> kycLevelDistribution = new HashMap<>();
    private Map<String, Integer> countryDistribution = new HashMap<>();
    private double amlRiskRating;
    private Instant generatedAt;

    // Getters and Setters
    public LocalDate getReportDate() { return reportDate; }
    public void setReportDate(LocalDate reportDate) { this.reportDate = reportDate; }

    public int getTotalIdentities() { return totalIdentities; }
    public void setTotalIdentities(int totalIdentities) { this.totalIdentities = totalIdentities; }

    public int getActiveIdentities() { return activeIdentities; }
    public void setActiveIdentities(int activeIdentities) { this.activeIdentities = activeIdentities; }

    public int getRevokedIdentities() { return revokedIdentities; }
    public void setRevokedIdentities(int revokedIdentities) { this.revokedIdentities = revokedIdentities; }

    public Map<String, Integer> getKycLevelDistribution() { return kycLevelDistribution; }
    public void setKycLevelDistribution(Map<String, Integer> kycLevelDistribution) {
        this.kycLevelDistribution = kycLevelDistribution;
    }

    public Map<String, Integer> getCountryDistribution() { return countryDistribution; }
    public void setCountryDistribution(Map<String, Integer> countryDistribution) {
        this.countryDistribution = countryDistribution;
    }

    public double getAmlRiskRating() { return amlRiskRating; }
    public void setAmlRiskRating(double amlRiskRating) { this.amlRiskRating = amlRiskRating; }

    public Instant getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(Instant generatedAt) { this.generatedAt = generatedAt; }
}
