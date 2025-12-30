package io.aurigraph.v11.crm.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * Opportunity Entity - Represents sales opportunities in the pipeline
 *
 * Maps to PostgreSQL opportunities table with support for:
 * - Sales pipeline stage tracking (discovery → closed won/lost)
 * - Revenue forecasting with probability weighting
 * - Risk management (at-risk tracking, recovery actions)
 * - Competitive tracking
 * - Expansion opportunity linking (upsells, cross-sells, renewals)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "opportunities", indexes = {
        @Index(name = "idx_opp_lead_id", columnList = "lead_id"),
        @Index(name = "idx_opp_owned_by", columnList = "owned_by_user_id"),
        @Index(name = "idx_opp_stage", columnList = "stage"),
        @Index(name = "idx_opp_close_date", columnList = "expected_close_date")
})
public class Opportunity extends PanacheEntityBase {

    // Enums
    public enum OpportunityStage {
        DISCOVERY,
        ASSESSMENT,
        SOLUTION_DESIGN,
        PROPOSAL,
        NEGOTIATION,
        CLOSED_WON,
        CLOSED_LOST
    }

    public enum CloseReason {
        WON_BUDGET_APPROVED,
        WON_DECISION_MADE,
        WON_COMPETITIVE_WIN,
        LOST_BUDGET_DENIED,
        LOST_NO_DECISION,
        LOST_COMPETITIVE_LOSS,
        LOST_CHANGED_REQUIREMENTS,
        LOST_NO_LONGER_INTERESTED,
        LOST_NO_BUDGET
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "lead_id", nullable = false)
    private UUID leadId;

    @Column(nullable = false, length = 500)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "owned_by_user_id", nullable = false)
    private UUID ownedByUserId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OpportunityStage stage = OpportunityStage.DISCOVERY;

    @Column(name = "stage_changed_at")
    private ZonedDateTime stageChangedAt;

    // Revenue Tracking
    @Column(name = "estimated_value", precision = 15, scale = 2)
    private BigDecimal estimatedValue;

    @Column(name = "actual_value", precision = 15, scale = 2)
    private BigDecimal actualValue;

    @Column(length = 3)
    private String currency = "USD";

    @Column(name = "probability_percent", nullable = false)
    private Integer probabilityPercent = 0;

    @Column(name = "deal_size_category", length = 50)
    private String dealSizeCategory;

    // Dates
    @Column(name = "expected_close_date")
    private LocalDate expectedCloseDate;

    @Column(name = "actual_close_date")
    private LocalDate actualCloseDate;

    // Competitive Tracking
    @Column(name = "competing_vendors", columnDefinition = "TEXT")
    private String competingVendors;

    @Column(name = "competitive_advantage", columnDefinition = "TEXT")
    private String competitiveAdvantage;

    @Column(name = "win_strategy", columnDefinition = "TEXT")
    private String winStrategy;

    // Risk Management
    @Column(name = "at_risk")
    private Boolean atRisk = false;

    @Column(name = "at_risk_reason", columnDefinition = "TEXT")
    private String atRiskReason;

    @Column(name = "risk_probability_percent")
    private Integer riskProbabilityPercent = 0;

    @Column(name = "recovery_plan", columnDefinition = "TEXT")
    private String recoveryPlan;

    // Expansion Opportunities
    @Column(name = "is_expansion")
    private Boolean isExpansion = false;

    @Column(name = "parent_opportunity_id")
    private UUID parentOpportunityId;

    @Column(name = "expansion_type", length = 50)
    private String expansionType;

    // Close Outcome
    @Enumerated(EnumType.STRING)
    @Column(name = "close_reason")
    private CloseReason closeReason;

    // Audit Fields
    @Column(name = "created_at", nullable = false, updatable = false)
    private ZonedDateTime createdAt;

    @Column(name = "updated_at")
    private ZonedDateTime updatedAt;

    @Column(name = "created_by", length = 255)
    private String createdBy;

    @Column(name = "updated_by", length = 255)
    private String updatedBy;

    @PrePersist
    public void prePersist() {
        this.createdAt = ZonedDateTime.now();
        this.updatedAt = ZonedDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = ZonedDateTime.now();
    }

    /**
     * Calculate weighted value (estimatedValue × probabilityPercent / 100)
     */
    public BigDecimal getWeightedValue() {
        if (estimatedValue == null || probabilityPercent == null) {
            return BigDecimal.ZERO;
        }
        return estimatedValue.multiply(new BigDecimal(probabilityPercent).divide(new BigDecimal(100)));
    }
}
