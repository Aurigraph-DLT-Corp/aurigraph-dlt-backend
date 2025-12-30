package io.aurigraph.v11.billing.models;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "billing", indexes = {
    @Index(name = "idx_subscription_id", columnList = "subscription_id"),
    @Index(name = "idx_billing_date", columnList = "billing_date"),
    @Index(name = "idx_user_id", columnList = "user_id")
})
public class Billing extends PanacheEntity {

    public enum BillingStatus {
        PENDING, PAID, FAILED, REFUNDED, CANCELLED
    }

    public enum ChargeType {
        SUBSCRIPTION, USAGE_BASED, OVERAGE, REFUND, ADJUSTMENT
    }

    @Column(name = "subscription_id", nullable = false)
    public Long subscriptionId;

    @Column(name = "user_id", nullable = false)
    public String userId;

    @Column(name = "amount", nullable = false)
    public BigDecimal amount;

    @Column(name = "tax_amount")
    public BigDecimal taxAmount;

    @Column(name = "total_amount", nullable = false)
    public BigDecimal totalAmount;

    @Column(name = "currency", nullable = false)
    public String currency = "USD";

    @Column(name = "charge_type", nullable = false)
    @Enumerated(EnumType.STRING)
    public ChargeType chargeType;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    public BillingStatus status;

    @Column(name = "billing_date", nullable = false)
    public LocalDateTime billingDate;

    @Column(name = "due_date")
    public LocalDateTime dueDate;

    @Column(name = "paid_date")
    public LocalDateTime paidDate;

    @Column(name = "payment_method")
    public String paymentMethod;

    @Column(name = "stripe_payment_intent_id")
    public String stripePaymentIntentId;

    @Column(name = "invoice_id")
    public Long invoiceId;

    @Column(name = "description")
    public String description;

    @Column(name = "metadata", columnDefinition = "jsonb")
    public String metadata;

    @Column(name = "created_at", nullable = false, updatable = false)
    public LocalDateTime createdAt;

    @Column(name = "updated_at")
    public LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.dueDate == null && this.billingDate != null) {
            this.dueDate = this.billingDate.plusDays(30);
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isOverdue() {
        return status != BillingStatus.PAID &&
               dueDate != null &&
               dueDate.isBefore(LocalDateTime.now());
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

    public static BillingBuilder builder() {
        return new BillingBuilder();
    }

    public static class BillingBuilder {
        private Long subscriptionId;
        private String userId;
        private BigDecimal amount;
        private BigDecimal taxAmount;
        private BigDecimal totalAmount;
        private String currency = "USD";
        private ChargeType chargeType;
        private BillingStatus status;
        private LocalDateTime billingDate;
        private LocalDateTime dueDate;
        private LocalDateTime paidDate;
        private String paymentMethod;
        private String stripePaymentIntentId;
        private Long invoiceId;
        private String description;
        private String metadata;

        public BillingBuilder subscriptionId(Long id) {
            this.subscriptionId = id;
            return this;
        }

        public BillingBuilder userId(String userId) {
            this.userId = userId;
            return this;
        }

        public BillingBuilder amount(BigDecimal amount) {
            this.amount = amount;
            return this;
        }

        public BillingBuilder taxAmount(BigDecimal amount) {
            this.taxAmount = amount;
            return this;
        }

        public BillingBuilder totalAmount(BigDecimal amount) {
            this.totalAmount = amount;
            return this;
        }

        public BillingBuilder currency(String currency) {
            this.currency = currency;
            return this;
        }

        public BillingBuilder chargeType(ChargeType type) {
            this.chargeType = type;
            return this;
        }

        public BillingBuilder status(BillingStatus status) {
            this.status = status;
            return this;
        }

        public BillingBuilder billingDate(LocalDateTime date) {
            this.billingDate = date;
            return this;
        }

        public BillingBuilder dueDate(LocalDateTime date) {
            this.dueDate = date;
            return this;
        }

        public BillingBuilder paidDate(LocalDateTime date) {
            this.paidDate = date;
            return this;
        }

        public BillingBuilder paymentMethod(String method) {
            this.paymentMethod = method;
            return this;
        }

        public BillingBuilder stripePaymentIntentId(String id) {
            this.stripePaymentIntentId = id;
            return this;
        }

        public BillingBuilder invoiceId(Long id) {
            this.invoiceId = id;
            return this;
        }

        public BillingBuilder description(String description) {
            this.description = description;
            return this;
        }

        public BillingBuilder metadata(String metadata) {
            this.metadata = metadata;
            return this;
        }

        public Billing build() {
            Billing billing = new Billing();
            billing.subscriptionId = this.subscriptionId;
            billing.userId = this.userId;
            billing.amount = this.amount;
            billing.taxAmount = this.taxAmount;
            billing.totalAmount = this.totalAmount;
            billing.currency = this.currency;
            billing.chargeType = this.chargeType;
            billing.status = this.status;
            billing.billingDate = this.billingDate;
            billing.dueDate = this.dueDate;
            billing.paidDate = this.paidDate;
            billing.paymentMethod = this.paymentMethod;
            billing.stripePaymentIntentId = this.stripePaymentIntentId;
            billing.invoiceId = this.invoiceId;
            billing.description = this.description;
            billing.metadata = this.metadata;
            return billing;
        }
    }
}
