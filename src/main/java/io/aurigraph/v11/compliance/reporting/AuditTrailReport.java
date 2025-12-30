package io.aurigraph.v11.compliance.reporting;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Audit trail report for compliance tracking
 */
public class AuditTrailReport {
    private String tokenId;
    private LocalDate reportDate;
    private List<AuditEntry> auditEntries = new ArrayList<>();
    private int entryCount;
    private Instant generatedAt;

    /**
     * Single audit entry
     */
    public static class AuditEntry {
        private String tokenId;
        private String from;
        private String to;
        private BigDecimal amount;
        private boolean success;
        private String reason;
        private Instant timestamp;

        // Getters and Setters
        public String getTokenId() { return tokenId; }
        public void setTokenId(String tokenId) { this.tokenId = tokenId; }

        public String getFrom() { return from; }
        public void setFrom(String from) { this.from = from; }

        public String getTo() { return to; }
        public void setTo(String to) { this.to = to; }

        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }

        public Instant getTimestamp() { return timestamp; }
        public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
    }

    // Getters and Setters
    public String getTokenId() { return tokenId; }
    public void setTokenId(String tokenId) { this.tokenId = tokenId; }

    public LocalDate getReportDate() { return reportDate; }
    public void setReportDate(LocalDate reportDate) { this.reportDate = reportDate; }

    public List<AuditEntry> getAuditEntries() { return auditEntries; }
    public void setAuditEntries(List<AuditEntry> auditEntries) { this.auditEntries = auditEntries; }

    public int getEntryCount() { return entryCount; }
    public void setEntryCount(int entryCount) { this.entryCount = entryCount; }

    public Instant getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(Instant generatedAt) { this.generatedAt = generatedAt; }
}
