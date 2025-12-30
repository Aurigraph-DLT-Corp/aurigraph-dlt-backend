package io.aurigraph.v11.contracts.composite.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Builder;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.math.BigDecimal;

/**
 * Verification Service Interface Model
 * Defines the structure and capabilities of a verification service
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class VerificationService {
    
    @JsonProperty("serviceId")
    private String serviceId;
    
    @JsonProperty("serviceName")
    private String serviceName;
    
    @JsonProperty("serviceType")
    private ServiceType serviceType;
    
    @JsonProperty("providerAddress")
    private String providerAddress;
    
    @JsonProperty("providerName")
    private String providerName;
    
    @JsonProperty("status")
    private ServiceStatus status;
    
    @JsonProperty("tier")
    private ServiceTier tier;
    
    @JsonProperty("specializations")
    private List<VerificationSpecialization> specializations;
    
    @JsonProperty("supportedAssetTypes")
    private List<String> supportedAssetTypes;
    
    @JsonProperty("supportedVerificationTypes")
    private List<VerificationType> supportedVerificationTypes;
    
    @JsonProperty("minimumStake")
    private BigDecimal minimumStake;
    
    @JsonProperty("currentStake")
    private BigDecimal currentStake;
    
    @JsonProperty("fees")
    private ServiceFees fees;
    
    @JsonProperty("performance")
    private ServicePerformance performance;
    
    @JsonProperty("reputation")
    private ServiceReputation reputation;
    
    @JsonProperty("capacity")
    private ServiceCapacity capacity;
    
    @JsonProperty("serviceLevel")
    private ServiceLevel serviceLevel;
    
    @JsonProperty("compliance")
    private ComplianceInfo compliance;
    
    @JsonProperty("contactInfo")
    private ContactInfo contactInfo;
    
    @JsonProperty("operatingHours")
    private OperatingHours operatingHours;
    
    @JsonProperty("geographicCoverage")
    private List<String> geographicCoverage;
    
    @JsonProperty("languages")
    private List<String> languages;
    
    @JsonProperty("integrationEndpoints")
    private IntegrationEndpoints integrationEndpoints;
    
    @JsonProperty("metadata")
    private Map<String, Object> metadata;
    
    @JsonProperty("createdAt")
    private Instant createdAt;
    
    @JsonProperty("updatedAt")
    private Instant updatedAt;
    
    @JsonProperty("lastActiveAt")
    private Instant lastActiveAt;
    
    /**
     * Types of verification services
     */
    public enum ServiceType {
        INDIVIDUAL_VERIFIER,
        CORPORATE_VERIFIER,
        GOVERNMENT_AGENCY,
        CERTIFICATION_BODY,
        AUDIT_FIRM,
        INSURANCE_COMPANY,
        BANK,
        EXCHANGE,
        CUSTODIAN,
        ORACLE_SERVICE,
        AI_VERIFICATION,
        AUTOMATED_SERVICE,
        HYBRID_SERVICE
    }
    
    /**
     * Service status
     */
    public enum ServiceStatus {
        ACTIVE,
        INACTIVE,
        SUSPENDED,
        PENDING_APPROVAL,
        REJECTED,
        MAINTENANCE,
        DECOMMISSIONED
    }
    
    /**
     * Service tier levels
     */
    public enum ServiceTier {
        BASIC,
        STANDARD,
        PREMIUM,
        ENTERPRISE,
        INSTITUTIONAL
    }
    
    /**
     * Verification specializations
     */
    public enum VerificationSpecialization {
        REAL_ESTATE,
        PRECIOUS_METALS,
        ARTWORK,
        COLLECTIBLES,
        SECURITIES,
        COMMODITIES,
        INTELLECTUAL_PROPERTY,
        CARBON_CREDITS,
        RENEWABLE_ENERGY,
        SUPPLY_CHAIN,
        IDENTITY_VERIFICATION,
        DOCUMENT_AUTHENTICATION,
        FINANCIAL_AUDITING,
        LEGAL_COMPLIANCE,
        TECHNICAL_ANALYSIS,
        ENVIRONMENTAL_IMPACT,
        SOCIAL_GOVERNANCE,
        RISK_ASSESSMENT
    }
    
    /**
     * Types of verifications offered
     */
    public enum VerificationType {
        ASSET_EXISTENCE,
        ASSET_OWNERSHIP,
        ASSET_VALUATION,
        ASSET_CONDITION,
        LEGAL_COMPLIANCE,
        FINANCIAL_AUDIT,
        IDENTITY_VERIFICATION,
        DOCUMENT_VERIFICATION,
        CHAIN_OF_CUSTODY,
        PROVENANCE,
        AUTHENTICITY,
        QUALITY_ASSURANCE,
        ENVIRONMENTAL_IMPACT,
        SOCIAL_IMPACT,
        GOVERNANCE_COMPLIANCE,
        RISK_ASSESSMENT,
        INSURANCE_VALUATION,
        TAX_COMPLIANCE,
        REGULATORY_APPROVAL,
        THIRD_PARTY_VALIDATION
    }
    
    /**
     * Service fees structure
     */
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ServiceFees {
        @JsonProperty("baseFee")
        private BigDecimal baseFee;
        
        @JsonProperty("verificationFee")
        private BigDecimal verificationFee;
        
        @JsonProperty("urgentFee")
        private BigDecimal urgentFee;
        
        @JsonProperty("complexityMultiplier")
        private BigDecimal complexityMultiplier;
        
        @JsonProperty("volumeDiscounts")
        private Map<String, BigDecimal> volumeDiscounts; // volume tier -> discount %
        
        @JsonProperty("subscriptionFee")
        private BigDecimal subscriptionFee;
        
        @JsonProperty("feeCurrency")
        private String feeCurrency;
        
        @JsonProperty("paymentTerms")
        private String paymentTerms;
        
        @JsonProperty("refundPolicy")
        private String refundPolicy;
    }
    
    /**
     * Service performance metrics
     */
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ServicePerformance {
        @JsonProperty("averageResponseTime")
        private Long averageResponseTime; // in minutes
        
        @JsonProperty("completionRate")
        private BigDecimal completionRate; // percentage
        
        @JsonProperty("accuracyRate")
        private BigDecimal accuracyRate; // percentage
        
        @JsonProperty("totalVerifications")
        private Long totalVerifications;
        
        @JsonProperty("successfulVerifications")
        private Long successfulVerifications;
        
        @JsonProperty("averageRating")
        private BigDecimal averageRating; // 1-5 scale
        
        @JsonProperty("uptime")
        private BigDecimal uptime; // percentage
        
        @JsonProperty("lastMonthStats")
        private MonthlyStats lastMonthStats;
    }
    
    /**
     * Monthly statistics
     */
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class MonthlyStats {
        @JsonProperty("verificationsCompleted")
        private Long verificationsCompleted;
        
        @JsonProperty("averageResponseTime")
        private Long averageResponseTime;
        
        @JsonProperty("customerSatisfaction")
        private BigDecimal customerSatisfaction;
        
        @JsonProperty("revenue")
        private BigDecimal revenue;
    }
    
    /**
     * Service reputation metrics
     */
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ServiceReputation {
        @JsonProperty("trustScore")
        private BigDecimal trustScore; // 0-100
        
        @JsonProperty("certifications")
        private List<String> certifications;
        
        @JsonProperty("endorsements")
        private List<String> endorsements;
        
        @JsonProperty("complaints")
        private Long complaints;
        
        @JsonProperty("resolutions")
        private Long resolutions;
        
        @JsonProperty("penaltiesReceived")
        private Long penaltiesReceived;
        
        @JsonProperty("bondAmount")
        private BigDecimal bondAmount;
        
        @JsonProperty("insuranceCoverage")
        private BigDecimal insuranceCoverage;
    }
    
    /**
     * Service capacity information
     */
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ServiceCapacity {
        @JsonProperty("maxConcurrentVerifications")
        private Integer maxConcurrentVerifications;
        
        @JsonProperty("currentLoad")
        private Integer currentLoad;
        
        @JsonProperty("queueLength")
        private Integer queueLength;
        
        @JsonProperty("maxValuePerVerification")
        private BigDecimal maxValuePerVerification;
        
        @JsonProperty("totalCapacity")
        private BigDecimal totalCapacity;
        
        @JsonProperty("availableCapacity")
        private BigDecimal availableCapacity;
        
        @JsonProperty("utilizationRate")
        private BigDecimal utilizationRate; // percentage
    }
    
    /**
     * Service level agreements
     */
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ServiceLevel {
        @JsonProperty("standardResponseTime")
        private Long standardResponseTime; // hours
        
        @JsonProperty("urgentResponseTime")
        private Long urgentResponseTime; // hours
        
        @JsonProperty("availabilityGuarantee")
        private BigDecimal availabilityGuarantee; // percentage
        
        @JsonProperty("accuracyGuarantee")
        private BigDecimal accuracyGuarantee; // percentage
        
        @JsonProperty("penaltyClause")
        private String penaltyClause;
        
        @JsonProperty("escalationProcedure")
        private String escalationProcedure;
    }
    
    /**
     * Compliance information
     */
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ComplianceInfo {
        @JsonProperty("licenses")
        private List<String> licenses;
        
        @JsonProperty("jurisdictions")
        private List<String> jurisdictions;
        
        @JsonProperty("regulatoryApprovals")
        private List<String> regulatoryApprovals;
        
        @JsonProperty("auditReports")
        private List<String> auditReports;
        
        @JsonProperty("complianceScore")
        private BigDecimal complianceScore;
        
        @JsonProperty("lastAuditDate")
        private Instant lastAuditDate;
        
        @JsonProperty("nextAuditDue")
        private Instant nextAuditDue;
    }
    
    /**
     * Contact information
     */
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ContactInfo {
        @JsonProperty("email")
        private String email;
        
        @JsonProperty("phone")
        private String phone;
        
        @JsonProperty("website")
        private String website;
        
        @JsonProperty("address")
        private String address;
        
        @JsonProperty("supportEmail")
        private String supportEmail;
        
        @JsonProperty("emergencyContact")
        private String emergencyContact;
    }
    
    /**
     * Operating hours
     */
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class OperatingHours {
        @JsonProperty("timezone")
        private String timezone;
        
        @JsonProperty("businessHours")
        private String businessHours;
        
        @JsonProperty("supportHours")
        private String supportHours;
        
        @JsonProperty("emergencySupport")
        private Boolean emergencySupport;
        
        @JsonProperty("weekendSupport")
        private Boolean weekendSupport;
        
        @JsonProperty("holidaySchedule")
        private List<String> holidaySchedule;
    }
    
    /**
     * Integration endpoints
     */
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class IntegrationEndpoints {
        @JsonProperty("apiEndpoint")
        private String apiEndpoint;
        
        @JsonProperty("webhookUrl")
        private String webhookUrl;
        
        @JsonProperty("callbackUrl")
        private String callbackUrl;
        
        @JsonProperty("documentUploadUrl")
        private String documentUploadUrl;
        
        @JsonProperty("statusCheckUrl")
        private String statusCheckUrl;
        
        @JsonProperty("apiVersion")
        private String apiVersion;
        
        @JsonProperty("authenticationMethod")
        private String authenticationMethod;
    }
    
    // Constructor with essential fields
    public VerificationService(String serviceName, ServiceType serviceType, String providerAddress) {
        this.serviceId = java.util.UUID.randomUUID().toString();
        this.serviceName = serviceName;
        this.serviceType = serviceType;
        this.providerAddress = providerAddress;
        this.status = ServiceStatus.PENDING_APPROVAL;
        this.tier = ServiceTier.BASIC;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
        this.specializations = new java.util.ArrayList<>();
        this.supportedAssetTypes = new java.util.ArrayList<>();
        this.supportedVerificationTypes = new java.util.ArrayList<>();
        this.geographicCoverage = new java.util.ArrayList<>();
        this.languages = new java.util.ArrayList<>();
    }
    
    /**
     * Check if service is currently available
     */
    public boolean isAvailable() {
        return status == ServiceStatus.ACTIVE && 
               capacity != null && 
               capacity.getCurrentLoad() < capacity.getMaxConcurrentVerifications();
    }
    
    /**
     * Check if service supports specific asset type
     */
    public boolean supportsAssetType(String assetType) {
        return supportedAssetTypes == null || supportedAssetTypes.isEmpty() || 
               supportedAssetTypes.contains(assetType);
    }
    
    /**
     * Check if service supports specific verification type
     */
    public boolean supportsVerificationType(VerificationType verificationType) {
        return supportedVerificationTypes == null || supportedVerificationTypes.isEmpty() || 
               supportedVerificationTypes.contains(verificationType);
    }
    
    /**
     * Check if service covers specific jurisdiction
     */
    public boolean coversJurisdiction(String jurisdiction) {
        return geographicCoverage == null || geographicCoverage.isEmpty() || 
               geographicCoverage.contains(jurisdiction) || 
               geographicCoverage.contains("GLOBAL");
    }
    
    /**
     * Get overall service score for ranking
     */
    public BigDecimal getOverallScore() {
        BigDecimal score = BigDecimal.ZERO;
        int factors = 0;
        
        // Performance score (weight: 30%)
        if (performance != null) {
            if (performance.getCompletionRate() != null) {
                score = score.add(performance.getCompletionRate().multiply(BigDecimal.valueOf(0.15)));
                factors++;
            }
            if (performance.getAccuracyRate() != null) {
                score = score.add(performance.getAccuracyRate().multiply(BigDecimal.valueOf(0.15)));
                factors++;
            }
        }
        
        // Reputation score (weight: 40%)
        if (reputation != null && reputation.getTrustScore() != null) {
            score = score.add(reputation.getTrustScore().multiply(BigDecimal.valueOf(0.40)));
            factors++;
        }
        
        // Capacity utilization (weight: 15% - inverse, lower utilization = higher score)
        if (capacity != null && capacity.getUtilizationRate() != null) {
            BigDecimal utilizationScore = BigDecimal.valueOf(100).subtract(capacity.getUtilizationRate());
            score = score.add(utilizationScore.multiply(BigDecimal.valueOf(0.15)));
            factors++;
        }
        
        // Compliance score (weight: 15%)
        if (compliance != null && compliance.getComplianceScore() != null) {
            score = score.add(compliance.getComplianceScore().multiply(BigDecimal.valueOf(0.15)));
            factors++;
        }
        
        return factors > 0 ? score : BigDecimal.valueOf(50); // Default score if no data
    }
    
    /**
     * Calculate estimated fee for verification
     */
    public BigDecimal calculateEstimatedFee(BigDecimal assetValue, VerificationType verificationType, boolean urgent) {
        if (fees == null) return BigDecimal.ZERO;
        
        BigDecimal totalFee = fees.getBaseFee() != null ? fees.getBaseFee() : BigDecimal.ZERO;
        
        // Add verification fee
        if (fees.getVerificationFee() != null) {
            totalFee = totalFee.add(fees.getVerificationFee());
        }
        
        // Add urgent fee if applicable
        if (urgent && fees.getUrgentFee() != null) {
            totalFee = totalFee.add(fees.getUrgentFee());
        }
        
        // Apply complexity multiplier based on verification type
        if (fees.getComplexityMultiplier() != null) {
            BigDecimal multiplier = getComplexityMultiplier(verificationType);
            totalFee = totalFee.multiply(multiplier);
        }
        
        return totalFee;
    }
    
    /**
     * Get complexity multiplier based on verification type
     */
    private BigDecimal getComplexityMultiplier(VerificationType verificationType) {
        switch (verificationType) {
            case ASSET_EXISTENCE:
            case DOCUMENT_VERIFICATION:
                return BigDecimal.valueOf(1.0);
            case ASSET_OWNERSHIP:
            case IDENTITY_VERIFICATION:
                return BigDecimal.valueOf(1.2);
            case ASSET_VALUATION:
            case AUTHENTICITY:
                return BigDecimal.valueOf(1.5);
            case LEGAL_COMPLIANCE:
            case FINANCIAL_AUDIT:
                return BigDecimal.valueOf(2.0);
            case CHAIN_OF_CUSTODY:
            case PROVENANCE:
                return BigDecimal.valueOf(1.8);
            default:
                return BigDecimal.valueOf(1.3);
        }
    }
    
    /**
     * Update last active timestamp
     */
    public void updateLastActive() {
        this.lastActiveAt = Instant.now();
        this.updatedAt = Instant.now();
    }
    
    /**
     * Get service summary
     */
    public String getServiceSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append(serviceName).append(" (").append(serviceType).append(") - ");
        summary.append("Tier: ").append(tier).append(", ");
        summary.append("Status: ").append(status);
        
        if (performance != null && performance.getAverageRating() != null) {
            summary.append(", Rating: ").append(performance.getAverageRating()).append("/5");
        }
        
        if (reputation != null && reputation.getTrustScore() != null) {
            summary.append(", Trust: ").append(reputation.getTrustScore()).append("/100");
        }
        
        return summary.toString();
    }
}