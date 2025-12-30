package io.aurigraph.v11.crm.service;

import io.aurigraph.v11.crm.dto.CreateLeadRequest;
import io.aurigraph.v11.crm.entity.Lead;
import io.aurigraph.v11.crm.repository.LeadRepository;
import lombok.extern.slf4j.Slf4j;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * LeadService - Business logic for lead management
 *
 * Handles:
 * - Lead creation and enrichment
 * - Lead scoring based on engagement
 * - Lead status transitions
 * - GDPR compliance
 */
@Slf4j
@ApplicationScoped
public class LeadService {

    @Inject
    LeadRepository leadRepository;

    /**
     * Create a new lead from inquiry form
     */
    @Transactional
    public Lead createLead(CreateLeadRequest request) {
        log.info("Creating new lead: {}", request.getEmail());

        // Validate GDPR consent
        if (!Boolean.TRUE.equals(request.getGdprConsentGiven())) {
            throw new IllegalArgumentException("GDPR consent is required");
        }

        // Check if lead already exists
        Optional<Lead> existingLead = leadRepository.findByEmail(request.getEmail());
        if (existingLead.isPresent()) {
            log.info("Lead already exists: {}", request.getEmail());
            return existingLead.get();
        }

        // Create new lead
        Lead lead = Lead.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .phoneNumber(request.getPhone())
                .companyName(request.getCompanyName())
                .jobTitle(request.getJobTitle())
                .source(request.getSource())
                .inquiryType(request.getInquiryType())
                .companySizeRange(request.getCompanySizeRange())
                .industry(request.getIndustry())
                .budgetRange(request.getBudgetRange())
                .gdprConsentGiven(true)
                .gdprConsentTimestamp(ZonedDateTime.now())
                .status(Lead.LeadStatus.NEW)
                .leadScore(calculateInitialScore(request))
                .createdAt(ZonedDateTime.now())
                .updatedAt(ZonedDateTime.now())
                .build();

        leadRepository.persist(lead);
        log.info("Lead created with ID: {}", lead.getId());

        return lead;
    }

    /**
     * Get lead by ID
     */
    public Lead getLead(UUID id) {
        Optional<Lead> leadOpt = leadRepository.find("id", id).firstResultOptional();
        if (leadOpt.isEmpty()) {
            throw new IllegalArgumentException("Lead not found: " + id);
        }
        return leadOpt.get();
    }

    /**
     * Get lead by email
     */
    public Optional<Lead> getLeadByEmail(String email) {
        return leadRepository.findByEmail(email);
    }

    /**
     * Update lead status
     */
    @Transactional
    public void updateLeadStatus(UUID leadId, Lead.LeadStatus newStatus) {
        Lead lead = getLead(leadId);
        log.info("Updating lead {} status to {}", leadId, newStatus);
        leadRepository.updateStatus(leadId, newStatus);

        // Additional logic based on status change
        if (newStatus == Lead.LeadStatus.CONVERTED) {
            log.info("Lead {} converted! 🎉", leadId);
        } else if (newStatus == Lead.LeadStatus.LOST) {
            log.info("Lead {} marked as lost", leadId);
        }
    }

    /**
     * Update lead score (called after engagement activities)
     */
    @Transactional
    public void updateLeadScore(UUID leadId, Integer score) {
        Lead lead = getLead(leadId);
        Integer oldScore = lead.getLeadScore();
        leadRepository.updateScore(leadId, score);
        log.info("Lead {} score updated from {} to {}", leadId, oldScore, score);

        // Auto-qualify if score is high enough
        if (score >= 50 && lead.getStatus() == Lead.LeadStatus.NEW) {
            updateLeadStatus(leadId, Lead.LeadStatus.QUALIFIED);
        }
    }

    /**
     * Get all leads
     */
    public List<Lead> getAllLeads() {
        return leadRepository.listAll();
    }

    /**
     * Get leads by status
     */
    public List<Lead> getLeadsByStatus(Lead.LeadStatus status) {
        return leadRepository.findByStatus(status);
    }

    /**
     * Get high-value leads (high score)
     */
    public List<Lead> getHighValueLeads(Integer minScore) {
        return leadRepository.findHighScoreLeads(minScore);
    }

    /**
     * Get leads needing follow-up
     */
    public List<Lead> getLeadsNeedingFollowUp() {
        return leadRepository.findLeadsNeedingFollowUp();
    }

    /**
     * Assign lead to sales rep
     */
    @Transactional
    public void assignLead(UUID leadId, UUID userId) {
        Lead lead = getLead(leadId);
        lead.setAssignedToUserId(userId);
        lead.setAssignedAt(ZonedDateTime.now());
        lead.setUpdatedAt(ZonedDateTime.now());

        leadRepository.update("assignedToUserId = ?1, assignedAt = ?2 WHERE id = ?3",
                userId, ZonedDateTime.now(), leadId);

        log.info("Lead {} assigned to user {}", leadId, userId);
    }

    /**
     * Calculate initial lead score based on inquiry type
     */
    private Integer calculateInitialScore(CreateLeadRequest request) {
        Integer score = 5;  // Base score

        // Add points for inquiry type
        if ("Platform Demo".equals(request.getInquiryType())) {
            score += 20;
        } else if ("Partnership".equals(request.getInquiryType())) {
            score += 15;
        }

        // Add points for company size
        if ("500+".equals(request.getCompanySizeRange())) {
            score += 15;
        } else if ("201-500".equals(request.getCompanySizeRange())) {
            score += 10;
        }

        // Add points for specific industries
        if (request.getIndustry() != null &&
                (request.getIndustry().contains("Finance") ||
                 request.getIndustry().contains("Energy"))) {
            score += 10;
        }

        // Add points for budget
        if ("$100K+".equals(request.getBudgetRange())) {
            score += 20;
        } else if ("$50K-100K".equals(request.getBudgetRange())) {
            score += 15;
        }

        return Math.min(score, 50);  // Cap at 50 for new leads
    }

    /**
     * Verify email (called after email verification)
     */
    @Transactional
    public void verifyEmail(UUID leadId) {
        Lead lead = getLead(leadId);
        lead.setEmailVerified(true);
        lead.setEmailVerifiedTimestamp(ZonedDateTime.now());

        // Increase score for verified email
        updateLeadScore(leadId, lead.getLeadScore() + 5);
        log.info("Email verified for lead {}", leadId);
    }
}
