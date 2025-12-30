package io.aurigraph.v11.crm.repository;

import io.aurigraph.v11.crm.entity.Opportunity;
import io.quarkus.hibernate.orm.panache.PanacheRepository;

import jakarta.enterprise.context.ApplicationScoped;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

/**
 * OpportunityRepository - Data access layer for Opportunity entities
 *
 * Provides pipeline analysis, revenue forecasting, and opportunity tracking
 */
@ApplicationScoped
public class OpportunityRepository implements PanacheRepository<Opportunity> {

    /**
     * Find opportunity by UUID ID
     */
    public Opportunity findById(UUID id) {
        return find("id = ?1", id).firstResult();
    }

    /**
     * Find opportunities by lead
     */
    public List<Opportunity> findByLeadId(UUID leadId) {
        return find("leadId = ?1 ORDER BY createdAt DESC", leadId).list();
    }

    /**
     * Find opportunities by sales rep
     */
    public List<Opportunity> findByOwnedBy(UUID userId) {
        return find("ownedByUserId = ?1 ORDER BY stage ASC", userId).list();
    }

    /**
     * Find opportunities by stage
     */
    public List<Opportunity> findByStage(Opportunity.OpportunityStage stage) {
        return find("stage = ?1 ORDER BY probabilityPercent DESC", stage).list();
    }

    /**
     * Find open opportunities (not closed)
     */
    public List<Opportunity> findOpenOpportunities() {
        return find("stage NOT IN ?1 ORDER BY expectedCloseDate ASC",
                List.of(Opportunity.OpportunityStage.CLOSED_WON,
                        Opportunity.OpportunityStage.CLOSED_LOST))
                .list();
    }

    /**
     * Find at-risk opportunities
     */
    public List<Opportunity> findAtRisk() {
        return find("atRisk = TRUE ORDER BY probabilityPercent ASC").list();
    }

    /**
     * Find opportunities closing soon
     */
    public List<Opportunity> findClosingSoon(LocalDate daysFromNow) {
        return find("expectedCloseDate <= ?1 AND stage NOT IN ?2 ORDER BY expectedCloseDate ASC",
                daysFromNow,
                List.of(Opportunity.OpportunityStage.CLOSED_WON,
                        Opportunity.OpportunityStage.CLOSED_LOST))
                .list();
    }

    /**
     * Find won opportunities (closed won)
     */
    public List<Opportunity> findWonOpportunities() {
        return find("stage = ?1 ORDER BY actualCloseDate DESC", Opportunity.OpportunityStage.CLOSED_WON).list();
    }

    /**
     * Find lost opportunities
     */
    public List<Opportunity> findLostOpportunities() {
        return find("stage = ?1 ORDER BY actualCloseDate DESC", Opportunity.OpportunityStage.CLOSED_LOST).list();
    }

    /**
     * Calculate total pipeline value (estimated × probability)
     */
    public BigDecimal getTotalPipelineValue() {
        List<Opportunity> openOpps = findOpenOpportunities();
        return openOpps.stream()
                .map(Opportunity::getWeightedValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Calculate pipeline value by stage
     */
    public BigDecimal getPipelineValueByStage(Opportunity.OpportunityStage stage) {
        return findByStage(stage).stream()
                .map(Opportunity::getWeightedValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Calculate total won revenue
     */
    public BigDecimal getTotalWonRevenue() {
        List<Opportunity> wonOpps = findWonOpportunities();
        return wonOpps.stream()
                .map(opp -> opp.getActualValue() != null ? opp.getActualValue() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Get win rate
     */
    public double getWinRate() {
        long totalClosed = count("stage IN ?1",
                List.of(Opportunity.OpportunityStage.CLOSED_WON,
                        Opportunity.OpportunityStage.CLOSED_LOST));
        if (totalClosed == 0) return 0.0;

        long wonCount = count("stage", Opportunity.OpportunityStage.CLOSED_WON);
        return (double) wonCount / totalClosed * 100;
    }

    /**
     * Update opportunity stage
     */
    public void updateStage(UUID opportunityId, Opportunity.OpportunityStage newStage) {
        update("stage = ?1, stageChangedAt = ?2, updatedAt = ?2 WHERE id = ?3",
                newStage, ZonedDateTime.now(), opportunityId);
    }

    /**
     * Mark opportunity as at-risk
     */
    public void markAtRisk(UUID opportunityId, String reason, Integer riskProbability) {
        update("atRisk = TRUE, atRiskReason = ?1, riskProbabilityPercent = ?2, updatedAt = ?3 WHERE id = ?4",
                reason, riskProbability, ZonedDateTime.now(), opportunityId);
    }

    /**
     * Pipeline statistics DTO
     */
    public record PipelineStatistics(
            long totalOpportunities,
            long openOpportunities,
            long wonOpportunities,
            long lostOpportunities,
            BigDecimal totalPipelineValue,
            BigDecimal totalWonRevenue,
            double winRate
    ) {}
}
