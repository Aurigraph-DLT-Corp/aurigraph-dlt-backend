package io.aurigraph.v11.billing.models;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payment", indexes = {
    @Index(name = "idx_subscription_id", columnList = "subscription_id"),
    @Index(name = "idx_billing_id", columnList = "billing_id"),
    @Index(name = "idx_status", columnList = "status"),
    @Index(name = "idx_created_at", columnList = "created_at")
})
public class Payment extends PanacheEntity {

    public enum PaymentStatus {
        PENDING, PROCESSING, COMPLETED, FAILED, REFUNDED, CANCELLED
    }

    public enum PaymentMethod {
        CREDIT_CARD, DEBIT_CARD, PAYPAL, BANK_TRANSFER, STRIPE, INVOICE
    }

    public enum PaymentGateway {
        STRIPE, PAYPAL, SQUARE, CUSTOM
    }

    @Column(name = "subscription_id", nullable = false)
    public Long subscriptionId;

    @Column(name = "billing_id", nullable = false)
    public Long billingId;

    @Column(name = "user_id", nullable = false)
    public String userId;

    @Column(name = "amount", nullable = false)
    public BigDecimal amount;

    @Column(name = "currency", nullable = false)
    public String currency = "USD";

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    public PaymentStatus status;

    @Column(name = "payment_method", nullable = false)
    @Enumerated(EnumType.STRING)
    public PaymentMethod paymentMethod;

    @Column(name = "payment_gateway", nullable = false)
    @Enumerated(EnumType.STRING)
    public PaymentGateway paymentGateway;

    @Column(name = "gateway_transaction_id")
    public String gatewayTransactionId;

    @Column(name = "stripe_payment_intent_id")
    public String stripePaymentIntentId;

    @Column(name = "stripe_charge_id")
    public String stripeChargeId;

    @Column(name = "paypal_transaction_id")
    public String paypalTransactionId;

    @Column(name = "last_four_digits")
    public String lastFourDigits;

    @Column(name = "card_brand")
    public String cardBrand;

    @Column(name = "payout_status")
    public String payoutStatus;

    @Column(name = "error_code")
    public String errorCode;

    @Column(name = "error_message")
    public String errorMessage;

    @Column(name = "refund_reason")
    public String refundReason;

    @Column(name = "refund_amount")
    public BigDecimal refundAmount;

    @Column(name = "processing_fee")
    public BigDecimal processingFee;

    @Column(name = "metadata", columnDefinition = "jsonb")
    public String metadata;

    @Column(name = "created_at", nullable = false, updatable = false)
    public LocalDateTime createdAt;

    @Column(name = "updated_at")
    public LocalDateTime updatedAt;

    @Column(name = "processed_at")
    public LocalDateTime processedAt;

    @Column(name = "refunded_at")
    public LocalDateTime refundedAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isSuccessful() {
        return status == PaymentStatus.COMPLETED;
    }

    public boolean canBeRefunded() {
        return status == PaymentStatus.COMPLETED;
    }

    public long minutesSinceCreation() {
        return java.time.temporal.ChronoUnit.MINUTES.between(
            createdAt,
            LocalDateTime.now()
        );
    }

    public static PaymentBuilder builder() {
        return new PaymentBuilder();
    }

    public static class PaymentBuilder {
        private Long subscriptionId;
        private Long billingId;
        private String userId;
        private BigDecimal amount;
        private String currency = "USD";
        private PaymentStatus status;
        private PaymentMethod paymentMethod;
        private PaymentGateway paymentGateway;
        private String gatewayTransactionId;
        private String stripePaymentIntentId;
        private String stripeChargeId;
        private String paypalTransactionId;
        private String lastFourDigits;
        private String cardBrand;
        private String payoutStatus;
        private String errorCode;
        private String errorMessage;
        private String refundReason;
        private BigDecimal refundAmount;
        private BigDecimal processingFee;
        private String metadata;

        public PaymentBuilder subscriptionId(Long id) {
            this.subscriptionId = id;
            return this;
        }

        public PaymentBuilder billingId(Long id) {
            this.billingId = id;
            return this;
        }

        public PaymentBuilder userId(String userId) {
            this.userId = userId;
            return this;
        }

        public PaymentBuilder amount(BigDecimal amount) {
            this.amount = amount;
            return this;
        }

        public PaymentBuilder currency(String currency) {
            this.currency = currency;
            return this;
        }

        public PaymentBuilder status(PaymentStatus status) {
            this.status = status;
            return this;
        }

        public PaymentBuilder paymentMethod(PaymentMethod method) {
            this.paymentMethod = method;
            return this;
        }

        public PaymentBuilder paymentGateway(PaymentGateway gateway) {
            this.paymentGateway = gateway;
            return this;
        }

        public PaymentBuilder gatewayTransactionId(String id) {
            this.gatewayTransactionId = id;
            return this;
        }

        public PaymentBuilder stripePaymentIntentId(String id) {
            this.stripePaymentIntentId = id;
            return this;
        }

        public PaymentBuilder stripeChargeId(String id) {
            this.stripeChargeId = id;
            return this;
        }

        public PaymentBuilder paypalTransactionId(String id) {
            this.paypalTransactionId = id;
            return this;
        }

        public PaymentBuilder lastFourDigits(String digits) {
            this.lastFourDigits = digits;
            return this;
        }

        public PaymentBuilder cardBrand(String brand) {
            this.cardBrand = brand;
            return this;
        }

        public PaymentBuilder payoutStatus(String status) {
            this.payoutStatus = status;
            return this;
        }

        public PaymentBuilder errorCode(String code) {
            this.errorCode = code;
            return this;
        }

        public PaymentBuilder errorMessage(String message) {
            this.errorMessage = message;
            return this;
        }

        public PaymentBuilder refundReason(String reason) {
            this.refundReason = reason;
            return this;
        }

        public PaymentBuilder refundAmount(BigDecimal amount) {
            this.refundAmount = amount;
            return this;
        }

        public PaymentBuilder processingFee(BigDecimal fee) {
            this.processingFee = fee;
            return this;
        }

        public PaymentBuilder metadata(String metadata) {
            this.metadata = metadata;
            return this;
        }

        public Payment build() {
            Payment payment = new Payment();
            payment.subscriptionId = this.subscriptionId;
            payment.billingId = this.billingId;
            payment.userId = this.userId;
            payment.amount = this.amount;
            payment.currency = this.currency;
            payment.status = this.status;
            payment.paymentMethod = this.paymentMethod;
            payment.paymentGateway = this.paymentGateway;
            payment.gatewayTransactionId = this.gatewayTransactionId;
            payment.stripePaymentIntentId = this.stripePaymentIntentId;
            payment.stripeChargeId = this.stripeChargeId;
            payment.paypalTransactionId = this.paypalTransactionId;
            payment.lastFourDigits = this.lastFourDigits;
            payment.cardBrand = this.cardBrand;
            payment.payoutStatus = this.payoutStatus;
            payment.errorCode = this.errorCode;
            payment.errorMessage = this.errorMessage;
            payment.refundReason = this.refundReason;
            payment.refundAmount = this.refundAmount;
            payment.processingFee = this.processingFee;
            payment.metadata = this.metadata;
            return payment;
        }
    }
}
