package io.aurigraph.v11.crm.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * Lead Entity - Represents customer leads captured from inquiries and demos
 *
 * Maps to PostgreSQL leads table with support for:
 * - Lead scoring (0-100+) based on engagement
 * - Lead enrichment data from third-party APIs
 * - GDPR compliance fields
 * - Complete audit trail
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "leads", indexes = {
        @Index(name = "idx_leads_email", columnList = "email"),
        @Index(name = "idx_leads_status", columnList = "status"),
        @Index(name = "idx_leads_created_at", columnList = "created_at DESC"),
        @Index(name = "idx_leads_assigned_to_user_id", columnList = "assigned_to_user_id"),
        @Index(name = "idx_leads_lead_score", columnList = "lead_score DESC")
})
public class Lead extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // Personal Information
    @Column(nullable = false, length = 100)
    private String firstName;

    @Column(length = 100)
    private String lastName;

    @Column(nullable = false, length = 255, unique = true)
    private String email;

    @Column(length = 20)
    private String phoneNumber;

    @Column(name = "phone_number_encrypted")
    private byte[] phoneNumberEncrypted;  // Encrypted phone for GDPR

    // Company Information
    @Column(length = 255)
    private String companyName;

    @Column(length = 255)
    private String companyDomain;

    @Column(length = 50)
    private String companySizeRange;  // 1-50, 51-200, 201-500, 500+

    @Column(length = 100)
    private String industry;

    @Column(length = 100)
    private String jobTitle;

    @Column(length = 100)
    private String country;

    @Column(length = 100)
    private String stateProvince;

    @Column(length = 100)
    private String city;

    // Enrichment Data
    @Column(length = 500)
    private String companyLogoUrl;

    @Column(length = 500)
    private String companyLinkedinUrl;

    @Column(length = 500)
    private String companyWebsite;

    @Column(length = 100)
    private String annualRevenue;

    @Column(columnDefinition = "TEXT")
    private String companyDescription;

    @Column(length = 20)
    private String sicCode;

    // Lead Management
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LeadStatus status = LeadStatus.NEW;

    @Column(nullable = false)
    private Integer leadScore = 0;

    @Column(name = "assigned_to_user_id")
    private UUID assignedToUserId;

    @Column(name = "assigned_at")
    private ZonedDateTime assignedAt;

    // Inquiry Details
    @Column(length = 100)
    private String inquiryType;

    @Column(columnDefinition = "TEXT")
    private String inquiryMessage;

    @Column(length = 50)
    private String budgetRange;  // Under 10K, 10K-50K, etc.

    @Column(length = 50)
    private String timeline;  // Immediate, 1-3 months, etc.

    // Contact Preferences
    @Enumerated(EnumType.STRING)
    private ContactPreference preferredContactMethod = ContactPreference.EMAIL;

    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean doNotContact = false;

    // GDPR & Compliance
    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean gdprConsentGiven = false;

    @Column(name = "gdpr_consent_timestamp")
    private ZonedDateTime gdprConsentTimestamp;

    @Column(name = "gdpr_consent_version", length = 20)
    private String gdprConsentVersion;

    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean emailVerified = false;

    @Column(name = "email_verified_timestamp")
    private ZonedDateTime emailVerifiedTimestamp;

    // Tracking
    @Enumerated(EnumType.STRING)
    private LeadSource source;

    @Column(length = 100)
    private String utmSource;

    @Column(length = 100)
    private String utmMedium;

    @Column(length = 100)
    private String utmCampaign;

    @Column(length = 100)
    private String utmContent;

    @Column(length = 100)
    private String sessionId;

    @Column(length = 500)
    private String referrerUrl;

    // Audit Fields
    @Column(nullable = false, updatable = false)
    private ZonedDateTime createdAt;

    @Column(nullable = false)
    private ZonedDateTime updatedAt;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "updated_by")
    private UUID updatedBy;

    @PrePersist
    protected void onCreate() {
        createdAt = ZonedDateTime.now();
        updatedAt = ZonedDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = ZonedDateTime.now();
    }

    // Enum for Lead Status
    public enum LeadStatus {
        NEW,
        ENGAGED,
        QUALIFIED,
        PROPOSAL_SENT,
        CONVERTED,
        LOST,
        ARCHIVED
    }

    // Enum for Lead Source
    public enum LeadSource {
        WEBSITE_INQUIRY,
        DEMO_REQUEST,
        PARTNER_REFERRAL,
        DIRECT_SALES,
        WEBINAR,
        EVENT,
        OTHER
    }

    // Enum for Contact Preference
    public enum ContactPreference {
        EMAIL,
        PHONE,
        SMS,
        LINKEDIN,
        NONE
    }
}
