package io.aurigraph.v11.compliance.reporting;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Comprehensive compliance report for a token
 */
public class ComplianceReport {
    private String tokenId;
    private LocalDate reportDate;
    private LocalDate startDate;
    private LocalDate endDate;
    private String reportType;
    private String jurisdiction;
    private String complianceStatus;
    private List<String> applicableRules = new ArrayList<>();
    private List<String> certifications = new ArrayList<>();

    // Transfer statistics
    private long totalTransfers;
    private long approvedTransfers;
    private long rejectedTransfers;
    private double approvalRate;

    // Identity statistics
    private long totalIdentities;
    private long activeIdentities;
    private long revokedIdentities;

    // Risk assessment
    private double riskScore;
    private int flaggedTransactions;
    private List<String> complianceIssues = new ArrayList<>();

    private Instant generatedAt;

    // Getters and Setters
    public String getTokenId() { return tokenId; }
    public void setTokenId(String tokenId) { this.tokenId = tokenId; }

    public LocalDate getReportDate() { return reportDate; }
    public void setReportDate(LocalDate reportDate) { this.reportDate = reportDate; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public String getReportType() { return reportType; }
    public void setReportType(String reportType) { this.reportType = reportType; }

    public String getJurisdiction() { return jurisdiction; }
    public void setJurisdiction(String jurisdiction) { this.jurisdiction = jurisdiction; }

    public String getComplianceStatus() { return complianceStatus; }
    public void setComplianceStatus(String complianceStatus) { this.complianceStatus = complianceStatus; }

    public List<String> getApplicableRules() { return applicableRules; }
    public void setApplicableRules(List<String> applicableRules) { this.applicableRules = applicableRules; }

    public List<String> getCertifications() { return certifications; }
    public void setCertifications(List<String> certifications) { this.certifications = certifications; }

    public long getTotalTransfers() { return totalTransfers; }
    public void setTotalTransfers(long totalTransfers) { this.totalTransfers = totalTransfers; }

    public long getApprovedTransfers() { return approvedTransfers; }
    public void setApprovedTransfers(long approvedTransfers) { this.approvedTransfers = approvedTransfers; }

    public long getRejectedTransfers() { return rejectedTransfers; }
    public void setRejectedTransfers(long rejectedTransfers) { this.rejectedTransfers = rejectedTransfers; }

    public double getApprovalRate() { return approvalRate; }
    public void setApprovalRate(double approvalRate) { this.approvalRate = approvalRate; }

    public long getTotalIdentities() { return totalIdentities; }
    public void setTotalIdentities(long totalIdentities) { this.totalIdentities = totalIdentities; }

    public long getActiveIdentities() { return activeIdentities; }
    public void setActiveIdentities(long activeIdentities) { this.activeIdentities = activeIdentities; }

    public long getRevokedIdentities() { return revokedIdentities; }
    public void setRevokedIdentities(long revokedIdentities) { this.revokedIdentities = revokedIdentities; }

    public double getRiskScore() { return riskScore; }
    public void setRiskScore(double riskScore) { this.riskScore = riskScore; }

    public int getFlaggedTransactions() { return flaggedTransactions; }
    public void setFlaggedTransactions(int flaggedTransactions) { this.flaggedTransactions = flaggedTransactions; }

    public List<String> getComplianceIssues() { return complianceIssues; }
    public void setComplianceIssues(List<String> complianceIssues) { this.complianceIssues = complianceIssues; }

    public Instant getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(Instant generatedAt) { this.generatedAt = generatedAt; }
}
