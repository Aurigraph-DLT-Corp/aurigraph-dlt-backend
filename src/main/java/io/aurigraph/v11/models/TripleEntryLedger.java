package io.aurigraph.v11.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

/**
 * Triple-Entry Ledger Model for Aurigraph V11 - LevelDB Compatible
 *
 * Implements triple-entry accounting with blockchain receipts.
 *
 * Triple-Entry Accounting Principles:
 * - Every transaction has a debit, credit, AND blockchain receipt
 * - All entries are cryptographically signed and immutable
 * - Third entry is the blockchain hash/receipt providing independent verification
 * - Enables real-time reconciliation with blockchain state
 *
 * LevelDB Storage: Uses receiptHash as primary key
 * JSON Serializable: All fields stored as JSON in LevelDB
 *
 * @version 4.0.0 (LevelDB Migration - Oct 8, 2025)
 * @since Sprint 13 (AV11-060)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TripleEntryLedger {

    // Removed UUID id - using receiptHash as primary key for LevelDB

    // ==================== FIELDS ====================

    /**
     * Transaction reference - links all related ledger entries
     */
    @JsonProperty("transactionId")
    private String transactionId;

    /**
     * Blockchain receipt hash - PRIMARY KEY for LevelDB (the third entry providing cryptographic proof)
     */
    @JsonProperty("receiptHash")
    private String receiptHash;

    /**
     * Debit account (source of funds)
     */
    @JsonProperty("debitAccount")
    private String debitAccount;

    /**
     * Credit account (destination of funds)
     */
    @JsonProperty("creditAccount")
    private String creditAccount;

    /**
     * Amount transferred
     */
    @JsonProperty("amount")
    private BigDecimal amount;

    /**
     * Currency code
     */
    @JsonProperty("currency")
    @Builder.Default
    private String currency = "USD";

    /**
     * Exchange rate if multi-currency
     */
    @JsonProperty("exchangeRate")
    private BigDecimal exchangeRate;

    /**
     * Converted amount in base currency
     */
    @JsonProperty("baseAmount")
    private BigDecimal baseAmount;

    @JsonProperty("baseCurrency")
    private String baseCurrency;

    /**
     * Timestamps
     */
    @JsonProperty("timestamp")
    @Builder.Default
    private Instant timestamp = Instant.now();

    @JsonProperty("entryDate")
    @Builder.Default
    private LocalDate entryDate = LocalDate.now();

    @JsonProperty("valueDate")
    private LocalDate valueDate;

    /**
     * Entry type classification
     */
    @JsonProperty("entryType")
    @Builder.Default
    private EntryType entryType = EntryType.NORMAL;

    /**
     * Description and narrative
     */
    @JsonProperty("description")
    private String description;

    @JsonProperty("narrative")
    private String narrative;

    /**
     * Reference to contract if applicable
     */
    @JsonProperty("contractId")
    private String contractId;

    /**
     * Blockchain details
     */
    @JsonProperty("blockchainNetwork")
    @Builder.Default
    private String blockchainNetwork = "Aurigraph";

    @JsonProperty("blockNumber")
    private Long blockNumber;

    @JsonProperty("blockHash")
    private String blockHash;

    @JsonProperty("transactionIndex")
    private Integer transactionIndex;

    /**
     * Reconciliation status
     */
    @JsonProperty("reconciled")
    @Builder.Default
    private boolean reconciled = false;

    @JsonProperty("reconciledAt")
    private Instant reconciledAt;

    @JsonProperty("reconciledBy")
    private String reconciledBy;

    /**
     * Verification and validation
     */
    @JsonProperty("verified")
    @Builder.Default
    private boolean verified = false;

    @JsonProperty("verifiedAt")
    private Instant verifiedAt;

    @JsonProperty("verificationSignature")
    private String verificationSignature;

    /**
     * Cryptographic signature of the entry
     */
    @JsonProperty("entrySignature")
    private String entrySignature;

    @JsonProperty("publicKey")
    private String publicKey;

    /**
     * Account balances after this entry (for quick lookup)
     */
    @JsonProperty("debitAccountBalance")
    private BigDecimal debitAccountBalance;

    @JsonProperty("creditAccountBalance")
    private BigDecimal creditAccountBalance;

    /**
     * Status and flags
     */
    @JsonProperty("status")
    @Builder.Default
    private EntryStatus status = EntryStatus.PENDING;

    @JsonProperty("reversed")
    @Builder.Default
    private boolean reversed = false;

    @JsonProperty("reversalEntryId")
    private String reversalEntryId; // Changed from UUID to String for LevelDB

    @JsonProperty("reversedAt")
    private Instant reversedAt;

    /**
     * Audit and compliance
     */
    @JsonProperty("createdBy")
    private String createdBy;

    @JsonProperty("approvedBy")
    private String approvedBy;

    @JsonProperty("approvedAt")
    private Instant approvedAt;

    @JsonProperty("fiscalPeriod")
    private String fiscalPeriod; // e.g., "2025-Q1"

    /**
     * Additional metadata (JSON)
     */
    @JsonProperty("metadata")
    @Builder.Default
    private String metadata = "{}"; // JSON object

    /**
     * Tags for categorization (JSON array)
     */
    @JsonProperty("tags")
    @Builder.Default
    private String tags = "[]"; // JSON array

    /**
     * Related entries (for complex transactions)
     */
    @JsonProperty("relatedEntries")
    @Builder.Default
    private String relatedEntries = "[]"; // JSON array of entry IDs

    // ==================== LIFECYCLE METHODS ====================

    /**
     * Ensure timestamp is set (call before first persist)
     */
    public void ensureTimestamp() {
        if (timestamp == null) {
            timestamp = Instant.now();
        }
        if (entryDate == null) {
            entryDate = LocalDate.now();
        }
        if (status == null) {
            status = EntryStatus.PENDING;
        }
    }

    /**
     * Validation methods
     */
    public boolean isValid() {
        return amount != null &&
               amount.compareTo(BigDecimal.ZERO) > 0 &&
               debitAccount != null && !debitAccount.isEmpty() &&
               creditAccount != null && !creditAccount.isEmpty() &&
               receiptHash != null && !receiptHash.isEmpty();
    }

    public boolean isBalanced() {
        // In triple-entry, amounts must always match
        return amount != null && amount.compareTo(BigDecimal.ZERO) > 0;
    }

    public boolean requiresReconciliation() {
        return !reconciled && status == EntryStatus.POSTED;
    }

    /**
     * Business logic methods
     */
    public void markAsReconciled(String reconciledBy) {
        this.reconciled = true;
        this.reconciledAt = Instant.now();
        this.reconciledBy = reconciledBy;
    }

    public void markAsVerified(String signature) {
        this.verified = true;
        this.verifiedAt = Instant.now();
        this.verificationSignature = signature;
    }

    public void approve(String approver) {
        this.status = EntryStatus.APPROVED;
        this.approvedBy = approver;
        this.approvedAt = Instant.now();
    }

    public void post() {
        if (status == EntryStatus.APPROVED || status == EntryStatus.PENDING) {
            this.status = EntryStatus.POSTED;
        }
    }

    public void reverse(String reversalId) {
        this.reversed = true;
        this.reversalEntryId = reversalId;
        this.reversedAt = Instant.now();
        this.status = EntryStatus.REVERSED;
    }

    public void reject(String reason) {
        this.status = EntryStatus.REJECTED;
        // Store reason in metadata
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TripleEntryLedger that = (TripleEntryLedger) o;
        return Objects.equals(receiptHash, that.receiptHash);
    }

    @Override
    public int hashCode() {
        return Objects.hash(receiptHash);
    }

    @Override
    public String toString() {
        return String.format("TripleEntryLedger{txId='%s', debit='%s', credit='%s', amount=%s %s, receipt='%s', status=%s}",
            transactionId, debitAccount, creditAccount, amount, currency,
            receiptHash != null ? receiptHash.substring(0, Math.min(16, receiptHash.length())) + "..." : "none",
            status);
    }
}

/**
 * Entry Type Enum
 */
enum EntryType {
    NORMAL,          // Regular transaction
    OPENING,         // Opening balance
    CLOSING,         // Closing balance
    ADJUSTMENT,      // Adjustment entry
    REVERSAL,        // Reversal of previous entry
    ACCRUAL,         // Accrual entry
    PREPAYMENT,      // Prepayment
    PROVISION,       // Provision entry
    REVALUATION      // Currency revaluation
}

/**
 * Entry Status Enum
 */
enum EntryStatus {
    PENDING,         // Awaiting approval
    APPROVED,        // Approved but not posted
    POSTED,          // Posted to ledger
    RECONCILED,      // Reconciled with blockchain
    REVERSED,        // Reversed
    REJECTED,        // Rejected
    ARCHIVED         // Archived
}
