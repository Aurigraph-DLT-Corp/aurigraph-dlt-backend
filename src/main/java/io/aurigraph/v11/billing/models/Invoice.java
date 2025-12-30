package io.aurigraph.v11.billing.models;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "invoice", indexes = {
    @Index(name = "idx_subscription_id", columnList = "subscription_id"),
    @Index(name = "idx_invoice_number", columnList = "invoice_number"),
    @Index(name = "idx_status", columnList = "status")
})
public class Invoice extends PanacheEntity {

    public enum InvoiceStatus {
        DRAFT, SENT, VIEWED, PAID, PARTIALLY_PAID, OVERDUE, CANCELLED
    }

    @Column(name = "invoice_number", nullable = false, unique = true)
    public String invoiceNumber;

    @Column(name = "subscription_id", nullable = false)
    public Long subscriptionId;

    @Column(name = "user_id", nullable = false)
    public String userId;

    @Column(name = "subtotal", nullable = false)
    public BigDecimal subtotal;

    @Column(name = "tax_rate")
    public BigDecimal taxRate;

    @Column(name = "tax_amount")
    public BigDecimal taxAmount;

    @Column(name = "discount_amount")
    public BigDecimal discountAmount;

    @Column(name = "total_amount", nullable = false)
    public BigDecimal totalAmount;

    @Column(name = "amount_paid")
    public BigDecimal amountPaid = BigDecimal.ZERO;

    @Column(name = "currency", nullable = false)
    public String currency = "USD";

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    public InvoiceStatus status;

    @Column(name = "invoice_date", nullable = false)
    public LocalDateTime invoiceDate;

    @Column(name = "due_date")
    public LocalDateTime dueDate;

    @Column(name = "paid_date")
    public LocalDateTime paidDate;

    @Column(name = "sent_date")
    public LocalDateTime sentDate;

    @Column(name = "line_items", columnDefinition = "jsonb")
    public String lineItems;

    @Column(name = "notes")
    public String notes;

    @Column(name = "payment_terms")
    public String paymentTerms;

    @Column(name = "billing_address", columnDefinition = "jsonb")
    public String billingAddress;

    @Column(name = "pdf_url")
    public String pdfUrl;

    @Column(name = "stripe_invoice_id")
    public String stripeInvoiceId;

    @Column(name = "created_at", nullable = false, updatable = false)
    public LocalDateTime createdAt;

    @Column(name = "updated_at")
    public LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.dueDate == null && this.invoiceDate != null) {
            this.dueDate = this.invoiceDate.plusDays(30);
        }
        if (this.invoiceNumber == null) {
            this.invoiceNumber = generateInvoiceNumber();
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    private String generateInvoiceNumber() {
        long timestamp = System.currentTimeMillis();
        return "INV-" + timestamp;
    }

    public boolean isOverdue() {
        return status != InvoiceStatus.PAID &&
               dueDate != null &&
               dueDate.isBefore(LocalDateTime.now());
    }

    public BigDecimal getOutstandingAmount() {
        return totalAmount.subtract(amountPaid != null ? amountPaid : BigDecimal.ZERO);
    }

    public boolean isFullyPaid() {
        return amountPaid != null && amountPaid.compareTo(totalAmount) >= 0;
    }

    public long daysUntilDue() {
        if (dueDate == null) {
            return -1;
        }
        return java.time.temporal.ChronoUnit.DAYS.between(
            LocalDateTime.now(),
            dueDate
        );
    }

    public static InvoiceBuilder builder() {
        return new InvoiceBuilder();
    }

    public static class InvoiceBuilder {
        private String invoiceNumber;
        private Long subscriptionId;
        private String userId;
        private BigDecimal subtotal;
        private BigDecimal taxRate;
        private BigDecimal taxAmount;
        private BigDecimal discountAmount;
        private BigDecimal totalAmount;
        private BigDecimal amountPaid = BigDecimal.ZERO;
        private String currency = "USD";
        private InvoiceStatus status;
        private LocalDateTime invoiceDate;
        private LocalDateTime dueDate;
        private LocalDateTime paidDate;
        private LocalDateTime sentDate;
        private String lineItems;
        private String notes;
        private String paymentTerms;
        private String billingAddress;
        private String pdfUrl;
        private String stripeInvoiceId;

        public InvoiceBuilder invoiceNumber(String number) {
            this.invoiceNumber = number;
            return this;
        }

        public InvoiceBuilder subscriptionId(Long id) {
            this.subscriptionId = id;
            return this;
        }

        public InvoiceBuilder userId(String userId) {
            this.userId = userId;
            return this;
        }

        public InvoiceBuilder subtotal(BigDecimal amount) {
            this.subtotal = amount;
            return this;
        }

        public InvoiceBuilder taxRate(BigDecimal rate) {
            this.taxRate = rate;
            return this;
        }

        public InvoiceBuilder taxAmount(BigDecimal amount) {
            this.taxAmount = amount;
            return this;
        }

        public InvoiceBuilder discountAmount(BigDecimal amount) {
            this.discountAmount = amount;
            return this;
        }

        public InvoiceBuilder totalAmount(BigDecimal amount) {
            this.totalAmount = amount;
            return this;
        }

        public InvoiceBuilder amountPaid(BigDecimal amount) {
            this.amountPaid = amount;
            return this;
        }

        public InvoiceBuilder currency(String currency) {
            this.currency = currency;
            return this;
        }

        public InvoiceBuilder status(InvoiceStatus status) {
            this.status = status;
            return this;
        }

        public InvoiceBuilder invoiceDate(LocalDateTime date) {
            this.invoiceDate = date;
            return this;
        }

        public InvoiceBuilder dueDate(LocalDateTime date) {
            this.dueDate = date;
            return this;
        }

        public InvoiceBuilder paidDate(LocalDateTime date) {
            this.paidDate = date;
            return this;
        }

        public InvoiceBuilder sentDate(LocalDateTime date) {
            this.sentDate = date;
            return this;
        }

        public InvoiceBuilder lineItems(String items) {
            this.lineItems = items;
            return this;
        }

        public InvoiceBuilder notes(String notes) {
            this.notes = notes;
            return this;
        }

        public InvoiceBuilder paymentTerms(String terms) {
            this.paymentTerms = terms;
            return this;
        }

        public InvoiceBuilder billingAddress(String address) {
            this.billingAddress = address;
            return this;
        }

        public InvoiceBuilder pdfUrl(String url) {
            this.pdfUrl = url;
            return this;
        }

        public InvoiceBuilder stripeInvoiceId(String id) {
            this.stripeInvoiceId = id;
            return this;
        }

        public Invoice build() {
            Invoice invoice = new Invoice();
            invoice.invoiceNumber = this.invoiceNumber;
            invoice.subscriptionId = this.subscriptionId;
            invoice.userId = this.userId;
            invoice.subtotal = this.subtotal;
            invoice.taxRate = this.taxRate;
            invoice.taxAmount = this.taxAmount;
            invoice.discountAmount = this.discountAmount;
            invoice.totalAmount = this.totalAmount;
            invoice.amountPaid = this.amountPaid;
            invoice.currency = this.currency;
            invoice.status = this.status;
            invoice.invoiceDate = this.invoiceDate;
            invoice.dueDate = this.dueDate;
            invoice.paidDate = this.paidDate;
            invoice.sentDate = this.sentDate;
            invoice.lineItems = this.lineItems;
            invoice.notes = this.notes;
            invoice.paymentTerms = this.paymentTerms;
            invoice.billingAddress = this.billingAddress;
            invoice.pdfUrl = this.pdfUrl;
            invoice.stripeInvoiceId = this.stripeInvoiceId;
            return invoice;
        }
    }
}
