package io.aurigraph.v11.crm.dto;

import io.aurigraph.v11.crm.entity.Lead;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for creating a new lead from API request
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateLeadRequest {
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String companyName;
    private String jobTitle;
    private Lead.LeadSource source;
    private String inquiryType;
    private String companySizeRange;
    private String industry;
    private String budgetRange;
    private Boolean gdprConsentGiven;
    private String message;
}
