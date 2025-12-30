package io.aurigraph.v11.compliance.reporting;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Transfer compliance report
 */
public class TransferComplianceReport {
    private String tokenId;
    private LocalDate reportDate;
    private int totalTransfers;
    private int approvedCount;
    private int rejectedCount;
    private BigDecimal totalTransferAmount;
    private BigDecimal averageTransferAmount;
    private List<String> topTransferers = new ArrayList<>();
    private List<String> topRecipients = new ArrayList<>();
    private Instant generatedAt;

    // Getters and Setters
    public String getTokenId() { return tokenId; }
    public void setTokenId(String tokenId) { this.tokenId = tokenId; }

    public LocalDate getReportDate() { return reportDate; }
    public void setReportDate(LocalDate reportDate) { this.reportDate = reportDate; }

    public int getTotalTransfers() { return totalTransfers; }
    public void setTotalTransfers(int totalTransfers) { this.totalTransfers = totalTransfers; }

    public int getApprovedCount() { return approvedCount; }
    public void setApprovedCount(int approvedCount) { this.approvedCount = approvedCount; }

    public int getRejectedCount() { return rejectedCount; }
    public void setRejectedCount(int rejectedCount) { this.rejectedCount = rejectedCount; }

    public BigDecimal getTotalTransferAmount() { return totalTransferAmount; }
    public void setTotalTransferAmount(BigDecimal totalTransferAmount) { this.totalTransferAmount = totalTransferAmount; }

    public BigDecimal getAverageTransferAmount() { return averageTransferAmount; }
    public void setAverageTransferAmount(BigDecimal averageTransferAmount) { this.averageTransferAmount = averageTransferAmount; }

    public List<String> getTopTransferers() { return topTransferers; }
    public void setTopTransferers(List<String> topTransferers) { this.topTransferers = topTransferers; }

    public List<String> getTopRecipients() { return topRecipients; }
    public void setTopRecipients(List<String> topRecipients) { this.topRecipients = topRecipients; }

    public Instant getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(Instant generatedAt) { this.generatedAt = generatedAt; }
}
