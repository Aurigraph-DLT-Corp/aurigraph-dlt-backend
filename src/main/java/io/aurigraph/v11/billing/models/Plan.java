package io.aurigraph.v11.billing.models;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "plan")
public class Plan extends PanacheEntity {

    public enum TierType {
        FREE, MID_TIER, ENTERPRISE, CUSTOM
    }

    @Column(name = "name", nullable = false, unique = true)
    public String name;

    @Column(name = "tier_type", nullable = false)
    @Enumerated(EnumType.STRING)
    public TierType tierType;

    @Column(name = "monthly_price")
    public BigDecimal monthlyPrice;

    @Column(name = "annual_price")
    public BigDecimal annualPrice;

    @Column(name = "description")
    public String description;

    @Column(name = "is_active")
    public Boolean isActive = true;

    // Feature limits as JSON
    @Column(name = "feature_limits", columnDefinition = "jsonb")
    public String featureLimits;

    // Transaction limits
    @Column(name = "max_transactions_per_month")
    public Integer maxTransactionsPerMonth;

    @Column(name = "max_transaction_amount")
    public BigDecimal maxTransactionAmount;

    // API rate limits
    @Column(name = "api_rate_limit_per_second")
    public Integer apiRateLimitPerSecond;

    // Storage limits (in GB)
    @Column(name = "storage_limit_gb")
    public Integer storageLimitGb;

    // User support level
    @Column(name = "support_level")
    public String supportLevel;

    // Trial period (days)
    @Column(name = "trial_days")
    public Integer trialDays;

    @Column(name = "created_at", nullable = false, updatable = false)
    public LocalDateTime createdAt;

    @Column(name = "updated_at")
    public LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Builder pattern for convenient object creation
    public static PlanBuilder builder() {
        return new PlanBuilder();
    }

    public static class PlanBuilder {
        private String name;
        private TierType tierType;
        private BigDecimal monthlyPrice;
        private BigDecimal annualPrice;
        private String description;
        private Boolean isActive = true;
        private String featureLimits;
        private Integer maxTransactionsPerMonth;
        private BigDecimal maxTransactionAmount;
        private Integer apiRateLimitPerSecond;
        private Integer storageLimitGb;
        private String supportLevel;
        private Integer trialDays;

        public PlanBuilder name(String name) {
            this.name = name;
            return this;
        }

        public PlanBuilder tierType(TierType tierType) {
            this.tierType = tierType;
            return this;
        }

        public PlanBuilder monthlyPrice(BigDecimal price) {
            this.monthlyPrice = price;
            return this;
        }

        public PlanBuilder annualPrice(BigDecimal price) {
            this.annualPrice = price;
            return this;
        }

        public PlanBuilder description(String description) {
            this.description = description;
            return this;
        }

        public PlanBuilder isActive(Boolean isActive) {
            this.isActive = isActive;
            return this;
        }

        public PlanBuilder featureLimits(String featureLimits) {
            this.featureLimits = featureLimits;
            return this;
        }

        public PlanBuilder maxTransactionsPerMonth(Integer max) {
            this.maxTransactionsPerMonth = max;
            return this;
        }

        public PlanBuilder maxTransactionAmount(BigDecimal amount) {
            this.maxTransactionAmount = amount;
            return this;
        }

        public PlanBuilder apiRateLimitPerSecond(Integer limit) {
            this.apiRateLimitPerSecond = limit;
            return this;
        }

        public PlanBuilder storageLimitGb(Integer limit) {
            this.storageLimitGb = limit;
            return this;
        }

        public PlanBuilder supportLevel(String level) {
            this.supportLevel = level;
            return this;
        }

        public PlanBuilder trialDays(Integer days) {
            this.trialDays = days;
            return this;
        }

        public Plan build() {
            Plan plan = new Plan();
            plan.name = this.name;
            plan.tierType = this.tierType;
            plan.monthlyPrice = this.monthlyPrice;
            plan.annualPrice = this.annualPrice;
            plan.description = this.description;
            plan.isActive = this.isActive;
            plan.featureLimits = this.featureLimits;
            plan.maxTransactionsPerMonth = this.maxTransactionsPerMonth;
            plan.maxTransactionAmount = this.maxTransactionAmount;
            plan.apiRateLimitPerSecond = this.apiRateLimitPerSecond;
            plan.storageLimitGb = this.storageLimitGb;
            plan.supportLevel = this.supportLevel;
            plan.trialDays = this.trialDays;
            return plan;
        }
    }
}
