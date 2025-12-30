package io.aurigraph.v11.crm.dto;

import io.aurigraph.v11.crm.entity.Lead;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * DTO for Lead response from API
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeadResponse {
    private UUID id;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String companyName;
    private String jobTitle;
    private Lead.LeadSource source;
    private Lead.LeadStatus status;
    private Integer leadScore;
    private String inquiryType;
    private String companySizeRange;
    private String industry;
    private String budgetRange;
    private Boolean emailVerified;
    private Boolean gdprConsentGiven;
    private UUID assignedToUserId;
    private ZonedDateTime assignedAt;
    private ZonedDateTime createdAt;
    private ZonedDateTime updatedAt;

    /**
     * Convert Lead entity to LeadResponse DTO
     */
    public static LeadResponse fromEntity(Lead lead) {
        return LeadResponse.builder()
                .id(lead.getId())
                .firstName(lead.getFirstName())
                .lastName(lead.getLastName())
                .email(lead.getEmail())
                .phone(lead.getPhoneNumber())
                .companyName(lead.getCompanyName())
                .jobTitle(lead.getJobTitle())
                .source(lead.getSource())
                .status(lead.getStatus())
                .leadScore(lead.getLeadScore())
                .inquiryType(lead.getInquiryType())
                .companySizeRange(lead.getCompanySizeRange())
                .industry(lead.getIndustry())
                .budgetRange(lead.getBudgetRange())
                .emailVerified(lead.getEmailVerified())
                .gdprConsentGiven(lead.getGdprConsentGiven())
                .assignedToUserId(lead.getAssignedToUserId())
                .assignedAt(lead.getAssignedAt())
                .createdAt(lead.getCreatedAt())
                .updatedAt(lead.getUpdatedAt())
                .build();
    }
}
