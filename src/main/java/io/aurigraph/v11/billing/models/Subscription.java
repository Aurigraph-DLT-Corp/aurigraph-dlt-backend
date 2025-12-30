package io.aurigraph.v11.billing.models;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "subscription", indexes = {
    @Index(name = "idx_user_id", columnList = "user_id"),
    @Index(name = "idx_status", columnList = "status"),
    @Index(name = "idx_created_at", columnList = "created_at")
})
public class Subscription extends PanacheEntity {

    public enum SubscriptionStatus {
        ACTIVE, TRIAL, PAUSED, CANCELLED, EXPIRED, PENDING_PAYMENT
    }

    public enum BillingCycle {
        MONTHLY, ANNUAL, CUSTOM
    }

    @Column(name = "user_id", nullable = false)
    public String userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    public Plan plan;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    public SubscriptionStatus status;

    @Column(name = "billing_cycle", nullable = false)
    @Enumerated(EnumType.STRING)
    public BillingCycle billingCycle;

    @Column(name = "current_price")
    public BigDecimal currentPrice;

    @Column(name = "subscription_start_date")
    public LocalDateTime startDate;

    @Column(name = "subscription_end_date")
    public LocalDateTime endDate;

    @Column(name = "renewal_date")
    public LocalDateTime renewalDate;

    @Column(name = "trial_end_date")
    public LocalDateTime trialEndDate;

    @Column(name = "auto_renew")
    public Boolean autoRenew = true;

    @Column(name = "payment_method_id")
    public String paymentMethodId;

    @Column(name = "stripe_subscription_id")
    public String stripeSubscriptionId;

    @Column(name = "stripe_customer_id")
    public String stripeCustomerId;

    @Column(name = "discount_percentage")
    public BigDecimal discountPercentage;

    @Column(name = "discount_code")
    public String discountCode;

    @Column(name = "created_at", nullable = false, updatable = false)
    public LocalDateTime createdAt;

    @Column(name = "updated_at")
    public LocalDateTime updatedAt;

    @Column(name = "cancelled_at")
    public LocalDateTime cancelledAt;

    @Column(name = "cancellation_reason")
    public String cancellationReason;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isActive() {
        return status == SubscriptionStatus.ACTIVE || status == SubscriptionStatus.TRIAL;
    }

    public boolean isExpired() {
        return endDate != null && endDate.isBefore(LocalDateTime.now());
    }

    public long daysUntilRenewal() {
        if (renewalDate == null) {
            return -1;
        }
        return java.time.temporal.ChronoUnit.DAYS.between(
            LocalDateTime.now(),
            renewalDate
        );
    }

    public static SubscriptionBuilder builder() {
        return new SubscriptionBuilder();
    }

    public static class SubscriptionBuilder {
        private String userId;
        private Plan plan;
        private SubscriptionStatus status;
        private BillingCycle billingCycle;
        private BigDecimal currentPrice;
        private LocalDateTime startDate;
        private LocalDateTime endDate;
        private LocalDateTime renewalDate;
        private LocalDateTime trialEndDate;
        private Boolean autoRenew = true;
        private String paymentMethodId;
        private String stripeSubscriptionId;
        private String stripeCustomerId;
        private BigDecimal discountPercentage;
        private String discountCode;

        public SubscriptionBuilder userId(String userId) {
            this.userId = userId;
            return this;
        }

        public SubscriptionBuilder plan(Plan plan) {
            this.plan = plan;
            return this;
        }

        public SubscriptionBuilder status(SubscriptionStatus status) {
            this.status = status;
            return this;
        }

        public SubscriptionBuilder billingCycle(BillingCycle cycle) {
            this.billingCycle = cycle;
            return this;
        }

        public SubscriptionBuilder currentPrice(BigDecimal price) {
            this.currentPrice = price;
            return this;
        }

        public SubscriptionBuilder startDate(LocalDateTime date) {
            this.startDate = date;
            return this;
        }

        public SubscriptionBuilder endDate(LocalDateTime date) {
            this.endDate = date;
            return this;
        }

        public SubscriptionBuilder renewalDate(LocalDateTime date) {
            this.renewalDate = date;
            return this;
        }

        public SubscriptionBuilder trialEndDate(LocalDateTime date) {
            this.trialEndDate = date;
            return this;
        }

        public SubscriptionBuilder autoRenew(Boolean autoRenew) {
            this.autoRenew = autoRenew;
            return this;
        }

        public SubscriptionBuilder paymentMethodId(String id) {
            this.paymentMethodId = id;
            return this;
        }

        public SubscriptionBuilder stripeSubscriptionId(String id) {
            this.stripeSubscriptionId = id;
            return this;
        }

        public SubscriptionBuilder stripeCustomerId(String id) {
            this.stripeCustomerId = id;
            return this;
        }

        public SubscriptionBuilder discountPercentage(BigDecimal percentage) {
            this.discountPercentage = percentage;
            return this;
        }

        public SubscriptionBuilder discountCode(String code) {
            this.discountCode = code;
            return this;
        }

        public Subscription build() {
            Subscription subscription = new Subscription();
            subscription.userId = this.userId;
            subscription.plan = this.plan;
            subscription.status = this.status;
            subscription.billingCycle = this.billingCycle;
            subscription.currentPrice = this.currentPrice;
            subscription.startDate = this.startDate;
            subscription.endDate = this.endDate;
            subscription.renewalDate = this.renewalDate;
            subscription.trialEndDate = this.trialEndDate;
            subscription.autoRenew = this.autoRenew;
            subscription.paymentMethodId = this.paymentMethodId;
            subscription.stripeSubscriptionId = this.stripeSubscriptionId;
            subscription.stripeCustomerId = this.stripeCustomerId;
            subscription.discountPercentage = this.discountPercentage;
            subscription.discountCode = this.discountCode;
            return subscription;
        }
    }
}
