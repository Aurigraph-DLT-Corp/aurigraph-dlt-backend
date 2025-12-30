package io.aurigraph.v11.crm.repository;

import io.aurigraph.v11.crm.entity.Lead;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Page;

import jakarta.enterprise.context.ApplicationScoped;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * LeadRepository - Data access layer for Lead entities
 *
 * Uses Quarkus Panache for simplified repository pattern with:
 * - Automatic transaction management
 * - Query methods for common operations
 * - Pagination and sorting support
 */
@ApplicationScoped
public class LeadRepository implements PanacheRepository<Lead> {

    /**
     * Find lead by UUID ID
     */
    public Lead findById(UUID id) {
        return find("id = ?1", id).firstResult();
    }

    /**
     * Find lead by email
     */
    public Optional<Lead> findByEmail(String email) {
        return find("email", email).firstResultOptional();
    }

    /**
     * Find leads by status
     */
    public List<Lead> findByStatus(Lead.LeadStatus status) {
        return find("status", status).list();
    }

    /**
     * Find leads assigned to user
     */
    public List<Lead> findByAssignedUser(UUID userId) {
        return find("assignedToUserId", userId).list();
    }

    /**
     * Find leads with high score (potential conversions)
     */
    public List<Lead> findHighScoreLeads(Integer minScore) {
        return find("leadScore >= ?1 ORDER BY leadScore DESC", minScore).list();
    }

    /**
     * Find recently created leads
     */
    public List<Lead> findRecentLeads(ZonedDateTime since, int limit) {
        return find("createdAt >= ?1 ORDER BY createdAt DESC", since)
                .page(Page.ofSize(limit))
                .list();
    }

    /**
     * Find leads needing follow-up
     */
    public List<Lead> findLeadsNeedingFollowUp() {
        return find("status IN ?1 AND doNotContact = FALSE ORDER BY createdAt ASC",
                List.of(Lead.LeadStatus.NEW, Lead.LeadStatus.ENGAGED))
                .list();
    }

    /**
     * Find leads by inquiry type
     */
    public List<Lead> findByInquiryType(String inquiryType) {
        return find("inquiryType", inquiryType).list();
    }

    /**
     * Count leads by status
     */
    public long countByStatus(Lead.LeadStatus status) {
        return count("status", status);
    }

    /**
     * Get lead statistics
     */
    public LeadStatistics getStatistics() {
        return new LeadStatistics(
                count(),
                countByStatus(Lead.LeadStatus.NEW),
                countByStatus(Lead.LeadStatus.QUALIFIED),
                countByStatus(Lead.LeadStatus.CONVERTED),
                countByStatus(Lead.LeadStatus.LOST)
        );
    }

    /**
     * Update lead status
     */
    public void updateStatus(UUID leadId, Lead.LeadStatus newStatus) {
        update("status = ?1, updatedAt = ?2 WHERE id = ?3",
                newStatus, ZonedDateTime.now(), leadId);
    }

    /**
     * Update lead score
     */
    public void updateScore(UUID leadId, Integer newScore) {
        update("leadScore = ?1, updatedAt = ?2 WHERE id = ?3",
                newScore, ZonedDateTime.now(), leadId);
    }

    /**
     * Lead statistics DTO
     */
    public record LeadStatistics(
            long total,
            long newLeads,
            long qualifiedLeads,
            long convertedLeads,
            long lostLeads
    ) {}
}
