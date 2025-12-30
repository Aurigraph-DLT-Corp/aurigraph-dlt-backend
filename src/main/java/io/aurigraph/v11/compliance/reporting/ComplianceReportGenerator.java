package io.aurigraph.v11.compliance.reporting;

import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import io.aurigraph.v11.compliance.persistence.ComplianceRepository;
import io.aurigraph.v11.compliance.persistence.TransferAuditRepository;
import io.aurigraph.v11.compliance.persistence.IdentityRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Compliance Report Generator for SEC/Regulatory Reporting
 * Generates compliance reports for regulatory filings and audits
 */
@ApplicationScoped
public class ComplianceReportGenerator {

    @Inject
    ComplianceRepository complianceRepository;

    @Inject
    TransferAuditRepository transferAuditRepository;

    @Inject
    IdentityRepository identityRepository;

    /**
     * Generate comprehensive compliance report for a token
     */
    public ComplianceReport generateTokenComplianceReport(String tokenId, LocalDate startDate, LocalDate endDate) {
        Log.infof("Generating compliance report for token %s from %s to %s", tokenId, startDate, endDate);

        ComplianceReport report = new ComplianceReport();
        report.setTokenId(tokenId);
        report.setReportDate(LocalDate.now());
        report.setStartDate(startDate);
        report.setEndDate(endDate);
        report.setReportType("TOKEN_COMPLIANCE_REPORT");

        // Get compliance record
        var complianceEntity = complianceRepository.findByTokenId(tokenId).orElse(null);
        if (complianceEntity != null) {
            report.setJurisdiction(complianceEntity.jurisdiction);
            report.setComplianceStatus(complianceEntity.complianceStatus);
            report.setApplicableRules(parseRules(complianceEntity.applicableRules));
            report.setCertifications(parseCertifications(complianceEntity.certifications));
        }

        // Get transfer statistics for period
        Instant startInstant = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant endInstant = endDate.atStartOfDay(ZoneId.systemDefault()).toInstant();

        // For now, count all transfers (period filtering should be done in repository)
        long totalTransfers = transferAuditRepository.count();
        long approvedTransfers = transferAuditRepository.countApproved(tokenId);
        long rejectedTransfers = transferAuditRepository.countRejected(tokenId);

        report.setTotalTransfers(totalTransfers);
        report.setApprovedTransfers(approvedTransfers);
        report.setRejectedTransfers(rejectedTransfers);

        if (totalTransfers > 0) {
            double approvalRate = (approvedTransfers * 100.0) / totalTransfers;
            report.setApprovalRate(approvalRate);
        } else {
            report.setApprovalRate(0.0);
        }

        // Get identity statistics
        long totalIdentities = identityRepository.count();
        long activeIdentities = identityRepository.countActive();

        report.setTotalIdentities(totalIdentities);
        report.setActiveIdentities(activeIdentities);
        report.setRevokedIdentities(totalIdentities - activeIdentities);

        // Calculate risk metrics
        report.setRiskScore(calculateRiskScore(report));
        report.setFlaggedTransactions(identifyFlaggedTransactions());
        report.setComplianceIssues(identifyComplianceIssues(tokenId));

        report.setGeneratedAt(Instant.now());
        return report;
    }

    /**
     * Generate transfer compliance report
     */
    public TransferComplianceReport generateTransferReport(String tokenId) {
        Log.infof("Generating transfer compliance report for token %s", tokenId);

        TransferComplianceReport report = new TransferComplianceReport();
        report.setTokenId(tokenId);
        report.setReportDate(LocalDate.now());
        report.setGeneratedAt(Instant.now());

        // Get all transfers for token
        var transfers = transferAuditRepository.findByTokenId(tokenId);

        report.setTotalTransfers(transfers.size());
        report.setApprovedCount((int) transfers.stream().filter(t -> t.success).count());
        report.setRejectedCount((int) transfers.stream().filter(t -> !t.success).count());

        // Calculate transfer statistics
        BigDecimal totalAmount = transfers.stream()
            .filter(t -> t.success)
            .map(t -> t.amount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        report.setTotalTransferAmount(totalAmount);

        double averageAmount = transfers.stream()
            .filter(t -> t.success)
            .map(t -> t.amount.doubleValue())
            .mapToDouble(Double::doubleValue)
            .average()
            .orElse(0.0);
        report.setAverageTransferAmount(new BigDecimal(averageAmount));

        // Identify top transferors and recipients
        Map<String, Long> transferorCounts = transfers.stream()
            .collect(Collectors.groupingBy(t -> t.fromAddress, Collectors.counting()));
        report.setTopTransferers(getTopAddresses(transferorCounts, 5));

        Map<String, Long> recipientCounts = transfers.stream()
            .collect(Collectors.groupingBy(t -> t.toAddress, Collectors.counting()));
        report.setTopRecipients(getTopAddresses(recipientCounts, 5));

        return report;
    }

    /**
     * Generate KYC/AML report
     */
    public KYCAMLReport generateKYCAMLReport() {
        Log.info("Generating KYC/AML report");

        KYCAMLReport report = new KYCAMLReport();
        report.setReportDate(LocalDate.now());
        report.setGeneratedAt(Instant.now());

        // Get all identities
        var allIdentities = identityRepository.listAll();
        report.setTotalIdentities(allIdentities.size());

        long activeCount = identityRepository.countActive();
        report.setActiveIdentities((int) activeCount);

        long revokedCount = allIdentities.size() - (int) activeCount;
        report.setRevokedIdentities((int) revokedCount);

        // Risk assessment by KYC level
        Map<String, Integer> kycLevelDistribution = new HashMap<>();
        for (var identity : allIdentities) {
            kycLevelDistribution.merge(identity.kycLevel, 1, Integer::sum);
        }
        report.setKycLevelDistribution(kycLevelDistribution);

        // Geographic distribution
        Map<String, Integer> countryDistribution = new HashMap<>();
        for (var identity : allIdentities) {
            countryDistribution.merge(identity.country, 1, Integer::sum);
        }
        report.setCountryDistribution(countryDistribution);

        // Compliance risk metrics
        double riskRating = calculateAMLRisk(report);
        report.setAmlRiskRating(riskRating);

        return report;
    }

    /**
     * Generate audit trail report
     */
    public AuditTrailReport generateAuditTrailReport(String tokenId, int limit) {
        Log.infof("Generating audit trail report for token %s (limit: %d)", tokenId, limit);

        AuditTrailReport report = new AuditTrailReport();
        report.setTokenId(tokenId);
        report.setReportDate(LocalDate.now());
        report.setGeneratedAt(Instant.now());

        // Get recent transfers
        var transfers = transferAuditRepository.findByTokenId(tokenId);
        var sortedTransfers = transfers.stream()
            .sorted((t1, t2) -> t2.transactionTimestamp.compareTo(t1.transactionTimestamp))
            .limit(limit)
            .map(this::mapTransferToAuditEntry)
            .collect(Collectors.toList());

        report.setAuditEntries(sortedTransfers);
        report.setEntryCount(sortedTransfers.size());

        return report;
    }

    /**
     * Calculate risk score for compliance
     */
    private double calculateRiskScore(ComplianceReport report) {
        double riskScore = 0.0;

        // Approval rate affects risk (lower approval = higher risk)
        if (report.getApprovalRate() < 80.0) {
            riskScore += 20.0;
        }
        if (report.getApprovalRate() < 90.0) {
            riskScore += 10.0;
        }

        // Flagged transactions increase risk
        if (report.getFlaggedTransactions() > 0) {
            riskScore += report.getFlaggedTransactions() * 5.0;
        }

        // Compliance issues increase risk
        if (report.getComplianceIssues().size() > 0) {
            riskScore += report.getComplianceIssues().size() * 10.0;
        }

        // Normalize to 0-100 scale
        return Math.min(100.0, riskScore);
    }

    /**
     * Calculate AML/KYC risk
     */
    private double calculateAMLRisk(KYCAMLReport report) {
        double risk = 0.0;

        // High-risk KYC levels increase risk
        Integer level2Count = report.getKycLevelDistribution().getOrDefault("2", 0);
        Integer level1Count = report.getKycLevelDistribution().getOrDefault("1", 0);

        risk += level1Count * 15.0;  // Level 1 (basic) = higher risk
        risk += level2Count * 5.0;   // Level 2 (enhanced) = some risk

        // Revoked identities indicate risk
        risk += report.getRevokedIdentities() * 3.0;

        // Geographic risk factors (simplified)
        Set<String> highRiskCountries = Set.of("IR", "KP", "SY", "CU", "VE");
        for (String country : report.getCountryDistribution().keySet()) {
            if (highRiskCountries.contains(country)) {
                risk += report.getCountryDistribution().get(country) * 25.0;
            }
        }

        return Math.min(100.0, risk);
    }

    /**
     * Identify flagged transactions (suspicious patterns)
     */
    private int identifyFlaggedTransactions() {
        // In production, this would check for suspicious patterns
        // Like rapid multiple transfers, unusual amounts, etc.
        return 0;
    }

    /**
     * Identify compliance issues
     */
    private List<String> identifyComplianceIssues(String tokenId) {
        List<String> issues = new ArrayList<>();

        var complianceEntity = complianceRepository.findByTokenId(tokenId).orElse(null);
        if (complianceEntity != null) {
            if ("NON_COMPLIANT".equals(complianceEntity.complianceStatus)) {
                issues.add("Token marked as non-compliant");
            }
            if ("PENDING".equals(complianceEntity.complianceStatus)) {
                issues.add("Compliance status pending verification");
            }
        }

        return issues;
    }

    /**
     * Parse rules from comma-separated string
     */
    private List<String> parseRules(String rules) {
        if (rules == null || rules.isEmpty()) {
            return new ArrayList<>();
        }
        return Arrays.asList(rules.split(","));
    }

    /**
     * Parse certifications from JSON or comma-separated string
     */
    private List<String> parseCertifications(String certifications) {
        if (certifications == null || certifications.isEmpty()) {
            return new ArrayList<>();
        }
        return Arrays.asList(certifications.split(","));
    }

    /**
     * Get top N addresses by transaction count
     */
    private List<String> getTopAddresses(Map<String, Long> addressCounts, int limit) {
        return addressCounts.entrySet().stream()
            .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
            .limit(limit)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
    }

    /**
     * Map transfer entity to audit entry
     */
    private AuditTrailReport.AuditEntry mapTransferToAuditEntry(
        io.aurigraph.v11.compliance.persistence.TransferAuditEntity transfer) {
        AuditTrailReport.AuditEntry entry = new AuditTrailReport.AuditEntry();
        entry.setTokenId(transfer.tokenId);
        entry.setFrom(transfer.fromAddress);
        entry.setTo(transfer.toAddress);
        entry.setAmount(transfer.amount);
        entry.setSuccess(transfer.success);
        entry.setReason(transfer.rejectReason);
        entry.setTimestamp(transfer.transactionTimestamp);
        return entry;
    }
}
