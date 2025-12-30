package io.aurigraph.v11.contracts.composite.models;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Verification Status Enum
 * Comprehensive status tracking for verification processes
 */
public enum VerificationStatus {
    
    // Initial States
    REQUESTED("requested", "Verification has been requested", 0),
    PENDING("pending", "Verification is pending assignment", 10),
    ASSIGNED("assigned", "Verification has been assigned to a verifier", 20),
    
    // Active States
    IN_PROGRESS("in_progress", "Verification is actively being processed", 30),
    DOCUMENT_REVIEW("document_review", "Documents are being reviewed", 35),
    FIELD_INSPECTION("field_inspection", "Field inspection is in progress", 40),
    TECHNICAL_ANALYSIS("technical_analysis", "Technical analysis is being conducted", 45),
    EXPERT_REVIEW("expert_review", "Expert review is in progress", 50),
    
    // Intermediate States
    ADDITIONAL_INFO_REQUIRED("additional_info_required", "Additional information is required", 25),
    CLARIFICATION_NEEDED("clarification_needed", "Clarification is needed from stakeholders", 26),
    AWAITING_DOCUMENTATION("awaiting_documentation", "Waiting for additional documentation", 27),
    AWAITING_ACCESS("awaiting_access", "Waiting for access to asset or location", 28),
    
    // Review States
    UNDER_REVIEW("under_review", "Verification results are under review", 60),
    PEER_REVIEW("peer_review", "Undergoing peer review process", 65),
    QUALITY_CHECK("quality_check", "Quality assurance check in progress", 70),
    SUPERVISORY_REVIEW("supervisory_review", "Under supervisory review", 72),
    LEGAL_REVIEW("legal_review", "Under legal compliance review", 74),
    
    // Approval States
    APPROVED("approved", "Verification has been approved", 80),
    CONDITIONALLY_APPROVED("conditionally_approved", "Approved with conditions", 85),
    PROVISIONALLY_APPROVED("provisionally_approved", "Provisionally approved pending final checks", 87),
    
    // Completion States
    COMPLETED("completed", "Verification has been completed successfully", 100),
    CERTIFIED("certified", "Asset has been certified", 100),
    VERIFIED("verified", "Asset has been verified", 100),
    
    // Negative Outcomes
    REJECTED("rejected", "Verification has been rejected", -10),
    FAILED("failed", "Verification failed due to issues found", -20),
    INVALID("invalid", "Verification request is invalid", -30),
    FRAUDULENT("fraudulent", "Fraudulent activity detected", -40),
    NON_COMPLIANT("non_compliant", "Asset does not meet compliance requirements", -25),
    
    // Administrative States
    CANCELLED("cancelled", "Verification has been cancelled", -5),
    WITHDRAWN("withdrawn", "Verification request has been withdrawn", -3),
    EXPIRED("expired", "Verification has expired", -15),
    SUSPENDED("suspended", "Verification has been suspended", -8),
    ON_HOLD("on_hold", "Verification is on hold", 15),
    
    // Error States
    ERROR("error", "An error occurred during verification", -50),
    SYSTEM_ERROR("system_error", "System error prevented completion", -45),
    TIMEOUT("timeout", "Verification timed out", -35),
    UNAVAILABLE("unavailable", "Verifier is unavailable", -6),
    
    // Appeal States
    APPEALED("appealed", "Verification result has been appealed", 55),
    APPEAL_UNDER_REVIEW("appeal_under_review", "Appeal is under review", 58),
    APPEAL_APPROVED("appeal_approved", "Appeal has been approved", 90),
    APPEAL_REJECTED("appeal_rejected", "Appeal has been rejected", -12),
    
    // Special States
    DISPUTED("disputed", "Verification result is disputed", 35),
    ESCALATED("escalated", "Case has been escalated", 45),
    PRIORITIZED("prioritized", "Verification has been given priority", 22),
    RUSH("rush", "Rush verification in progress", 32),
    
    // Monitoring States
    MONITORING("monitoring", "Under continuous monitoring", 95),
    PERIODIC_REVIEW("periodic_review", "Undergoing periodic review", 92),
    REVALIDATION_REQUIRED("revalidation_required", "Revalidation is required", 75),
    RENEWAL_PENDING("renewal_pending", "Renewal is pending", 78),
    
    // Legacy States
    LEGACY_APPROVED("legacy_approved", "Approved under legacy system", 82),
    MIGRATED("migrated", "Migrated from legacy system", 85),
    
    // Batch Processing States
    BATCH_PROCESSING("batch_processing", "Part of batch processing", 33),
    BATCH_COMPLETED("batch_completed", "Batch processing completed", 88),
    
    // Third-Party States
    THIRD_PARTY_VERIFICATION("third_party_verification", "Under third-party verification", 42),
    CROSS_VERIFICATION("cross_verification", "Undergoing cross-verification", 48),
    MULTI_VERIFIER_CONSENSUS("multi_verifier_consensus", "Awaiting multi-verifier consensus", 52);
    
    private final String code;
    private final String description;
    private final int progressScore; // -50 to 100, indicating progress level
    
    VerificationStatus(String code, String description, int progressScore) {
        this.code = code;
        this.description = description;
        this.progressScore = progressScore;
    }
    
    @JsonValue
    public String getCode() {
        return code;
    }
    
    public String getDescription() {
        return description;
    }
    
    public int getProgressScore() {
        return progressScore;
    }
    
    /**
     * Get verification status by code
     */
    public static VerificationStatus fromCode(String code) {
        if (code == null) return null;
        
        for (VerificationStatus status : values()) {
            if (status.code.equalsIgnoreCase(code)) {
                return status;
            }
        }
        return null;
    }
    
    /**
     * Check if status indicates completion (positive or negative)
     */
    public boolean isCompleted() {
        return this == COMPLETED || this == CERTIFIED || this == VERIFIED ||
               this == REJECTED || this == FAILED || this == INVALID ||
               this == FRAUDULENT || this == CANCELLED || this == WITHDRAWN ||
               this == EXPIRED || this == NON_COMPLIANT;
    }
    
    /**
     * Check if status indicates successful completion
     */
    public boolean isSuccessful() {
        return this == COMPLETED || this == CERTIFIED || this == VERIFIED ||
               this == APPROVED || this == CONDITIONALLY_APPROVED ||
               this == PROVISIONALLY_APPROVED || this == LEGACY_APPROVED ||
               this == BATCH_COMPLETED || this == APPEAL_APPROVED;
    }
    
    /**
     * Check if status indicates failure or rejection
     */
    public boolean isFailed() {
        return this == REJECTED || this == FAILED || this == INVALID ||
               this == FRAUDULENT || this == NON_COMPLIANT || this == ERROR ||
               this == SYSTEM_ERROR || this == TIMEOUT || this == APPEAL_REJECTED;
    }
    
    /**
     * Check if status indicates an active/in-progress state
     */
    public boolean isActive() {
        return progressScore > 0 && progressScore < 80 && !isCompleted();
    }
    
    /**
     * Check if status indicates a blocked/waiting state
     */
    public boolean isBlocked() {
        return this == ADDITIONAL_INFO_REQUIRED || this == CLARIFICATION_NEEDED ||
               this == AWAITING_DOCUMENTATION || this == AWAITING_ACCESS ||
               this == ON_HOLD || this == SUSPENDED || this == DISPUTED;
    }
    
    /**
     * Check if status can be appealed
     */
    public boolean canBeAppealed() {
        return this == REJECTED || this == FAILED || this == NON_COMPLIANT ||
               this == CONDITIONALLY_APPROVED;
    }
    
    /**
     * Check if status can be escalated
     */
    public boolean canBeEscalated() {
        return isActive() || isBlocked() || this == DISPUTED;
    }
    
    /**
     * Check if status requires action from requester
     */
    public boolean requiresRequesterAction() {
        return this == ADDITIONAL_INFO_REQUIRED || this == CLARIFICATION_NEEDED ||
               this == AWAITING_DOCUMENTATION;
    }
    
    /**
     * Check if status requires verifier action
     */
    public boolean requiresVerifierAction() {
        return this == ASSIGNED || this == IN_PROGRESS || this == DOCUMENT_REVIEW ||
               this == FIELD_INSPECTION || this == TECHNICAL_ANALYSIS ||
               this == EXPERT_REVIEW || this == UNDER_REVIEW || this == PEER_REVIEW ||
               this == QUALITY_CHECK;
    }
    
    /**
     * Check if status is terminal (no further state changes expected)
     */
    public boolean isTerminal() {
        return this == COMPLETED || this == CERTIFIED || this == VERIFIED ||
               this == REJECTED || this == FAILED || this == INVALID ||
               this == FRAUDULENT || this == CANCELLED || this == WITHDRAWN ||
               this == EXPIRED || this == APPEAL_REJECTED;
    }
    
    /**
     * Get the progress percentage (0-100)
     */
    public int getProgressPercentage() {
        if (progressScore < 0) return 0;
        return Math.min(100, progressScore);
    }
    
    /**
     * Get status category
     */
    public StatusCategory getCategory() {
        if (isSuccessful()) return StatusCategory.SUCCESS;
        if (isFailed()) return StatusCategory.FAILURE;
        if (isActive()) return StatusCategory.ACTIVE;
        if (isBlocked()) return StatusCategory.BLOCKED;
        if (progressScore < 0 && !isFailed()) return StatusCategory.CANCELLED;
        return StatusCategory.PENDING;
    }
    
    /**
     * Status categories for grouping
     */
    public enum StatusCategory {
        PENDING,
        ACTIVE,
        BLOCKED,
        SUCCESS,
        FAILURE,
        CANCELLED
    }
    
    /**
     * Get next possible statuses from current status
     */
    public VerificationStatus[] getPossibleNextStatuses() {
        switch (this) {
            case REQUESTED:
                return new VerificationStatus[]{PENDING, ASSIGNED, CANCELLED, INVALID};
            case PENDING:
                return new VerificationStatus[]{ASSIGNED, ON_HOLD, CANCELLED, EXPIRED};
            case ASSIGNED:
                return new VerificationStatus[]{IN_PROGRESS, ADDITIONAL_INFO_REQUIRED, UNAVAILABLE};
            case IN_PROGRESS:
                return new VerificationStatus[]{DOCUMENT_REVIEW, FIELD_INSPECTION, TECHNICAL_ANALYSIS, 
                                               EXPERT_REVIEW, UNDER_REVIEW, COMPLETED, FAILED};
            case UNDER_REVIEW:
                return new VerificationStatus[]{APPROVED, REJECTED, CONDITIONALLY_APPROVED, 
                                               ADDITIONAL_INFO_REQUIRED, QUALITY_CHECK};
            case APPROVED:
                return new VerificationStatus[]{COMPLETED, CERTIFIED, VERIFIED, APPEALED};
            case REJECTED:
                return new VerificationStatus[]{APPEALED, CANCELLED};
            case COMPLETED:
                return new VerificationStatus[]{MONITORING, PERIODIC_REVIEW, REVALIDATION_REQUIRED};
            default:
                return new VerificationStatus[0];
        }
    }
    
    /**
     * Check if transition to another status is valid
     */
    public boolean canTransitionTo(VerificationStatus newStatus) {
        if (isTerminal() && newStatus != this) return false;
        
        VerificationStatus[] allowedStatuses = getPossibleNextStatuses();
        for (VerificationStatus allowed : allowedStatuses) {
            if (allowed == newStatus) return true;
        }
        
        // Special case: can always escalate or appeal if allowed
        if ((newStatus == ESCALATED && canBeEscalated()) ||
            (newStatus == APPEALED && canBeAppealed())) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Get human-readable status message
     */
    public String getStatusMessage() {
        return description;
    }
    
    /**
     * Get estimated time to completion (in hours) - rough estimates
     */
    public Integer getEstimatedHoursToCompletion() {
        switch (getCategory()) {
            case PENDING: return 24;
            case ACTIVE:
                if (progressScore < 40) return 72;
                else if (progressScore < 70) return 48;
                else return 24;
            case BLOCKED: return null; // Depends on external factors
            case SUCCESS:
            case FAILURE:
            case CANCELLED:
                return 0;
            default: return 48;
        }
    }
}