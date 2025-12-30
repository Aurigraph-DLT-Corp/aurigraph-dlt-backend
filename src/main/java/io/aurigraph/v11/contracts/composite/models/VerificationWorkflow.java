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
 * Verification Workflow Model
 * Defines and tracks the complete workflow for verification processes
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class VerificationWorkflow {
    
    @JsonProperty("workflowId")
    private String workflowId;
    
    @JsonProperty("verificationId")
    private String verificationId;
    
    @JsonProperty("workflowType")
    private WorkflowType workflowType;
    
    @JsonProperty("currentStatus")
    private VerificationStatus currentStatus;
    
    @JsonProperty("currentStep")
    private WorkflowStep currentStep;
    
    @JsonProperty("steps")
    private List<WorkflowStep> steps;
    
    @JsonProperty("completedSteps")
    private List<String> completedSteps;
    
    @JsonProperty("pendingSteps")
    private List<String> pendingSteps;
    
    @JsonProperty("failedSteps")
    private List<String> failedSteps;
    
    @JsonProperty("skippedSteps")
    private List<String> skippedSteps;
    
    @JsonProperty("assignedVerifiers")
    private List<String> assignedVerifiers;
    
    @JsonProperty("stakeholders")
    private List<Stakeholder> stakeholders;
    
    @JsonProperty("sla")
    private WorkflowSLA sla;
    
    @JsonProperty("escalationRules")
    private List<EscalationRule> escalationRules;
    
    @JsonProperty("approvals")
    private List<Approval> approvals;
    
    @JsonProperty("notifications")
    private List<Notification> notifications;
    
    @JsonProperty("documents")
    private List<WorkflowDocument> documents;
    
    @JsonProperty("checkpoints")
    private List<Checkpoint> checkpoints;
    
    @JsonProperty("metrics")
    private WorkflowMetrics metrics;
    
    @JsonProperty("audit")
    private AuditTrail audit;
    
    @JsonProperty("startedAt")
    private Instant startedAt;
    
    @JsonProperty("expectedCompletionAt")
    private Instant expectedCompletionAt;
    
    @JsonProperty("actualCompletionAt")
    private Instant actualCompletionAt;
    
    @JsonProperty("lastActivityAt")
    private Instant lastActivityAt;
    
    @JsonProperty("metadata")
    private Map<String, Object> metadata;
    
    /**
     * Types of verification workflows
     */
    public enum WorkflowType {
        STANDARD_VERIFICATION,
        EXPRESS_VERIFICATION,
        COMPREHENSIVE_AUDIT,
        COMPLIANCE_CHECK,
        QUALITY_ASSURANCE,
        PEER_REVIEW,
        MULTI_PARTY_VERIFICATION,
        CONTINUOUS_MONITORING,
        REVALIDATION,
        APPEAL_PROCESS,
        DISPUTE_RESOLUTION,
        BATCH_PROCESSING,
        AUTOMATED_WORKFLOW,
        HYBRID_WORKFLOW,
        CUSTOM_WORKFLOW
    }
    
    /**
     * Individual workflow step
     */
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class WorkflowStep {
        @JsonProperty("stepId")
        private String stepId;
        
        @JsonProperty("stepName")
        private String stepName;
        
        @JsonProperty("stepType")
        private StepType stepType;
        
        @JsonProperty("description")
        private String description;
        
        @JsonProperty("order")
        private Integer order;
        
        @JsonProperty("status")
        private StepStatus status;
        
        @JsonProperty("assignedTo")
        private String assignedTo;
        
        @JsonProperty("dependencies")
        private List<String> dependencies; // Step IDs that must complete first
        
        @JsonProperty("isRequired")
        private Boolean isRequired;
        
        @JsonProperty("isParallel")
        private Boolean isParallel; // Can run in parallel with other steps
        
        @JsonProperty("timeoutMinutes")
        private Integer timeoutMinutes;
        
        @JsonProperty("maxRetries")
        private Integer maxRetries;
        
        @JsonProperty("currentRetries")
        private Integer currentRetries;
        
        @JsonProperty("autoAdvance")
        private Boolean autoAdvance; // Automatically advance to next step on success
        
        @JsonProperty("approvalRequired")
        private Boolean approvalRequired;
        
        @JsonProperty("inputs")
        private Map<String, Object> inputs;
        
        @JsonProperty("outputs")
        private Map<String, Object> outputs;
        
        @JsonProperty("validationRules")
        private List<ValidationRule> validationRules;
        
        @JsonProperty("startedAt")
        private Instant startedAt;
        
        @JsonProperty("completedAt")
        private Instant completedAt;
        
        @JsonProperty("estimatedDurationMinutes")
        private Integer estimatedDurationMinutes;
        
        @JsonProperty("actualDurationMinutes")
        private Integer actualDurationMinutes;
        
        @JsonProperty("notes")
        private String notes;
        
        @JsonProperty("errorMessage")
        private String errorMessage;
        
        public enum StepType {
            DOCUMENT_REVIEW,
            FIELD_INSPECTION,
            TECHNICAL_ANALYSIS,
            EXPERT_EVALUATION,
            AUTOMATED_CHECK,
            MANUAL_VERIFICATION,
            APPROVAL_GATE,
            NOTIFICATION,
            DATA_COLLECTION,
            QUALITY_CHECK,
            COMPLIANCE_REVIEW,
            STAKEHOLDER_APPROVAL,
            SYSTEM_INTEGRATION,
            REPORTING,
            ARCHIVAL
        }
        
        public enum StepStatus {
            PENDING,
            READY,
            IN_PROGRESS,
            COMPLETED,
            FAILED,
            CANCELLED,
            SKIPPED,
            ON_HOLD,
            TIMEOUT
        }
        
        @Data
        @Builder
        @AllArgsConstructor
        @NoArgsConstructor
        public static class ValidationRule {
            @JsonProperty("ruleId")
            private String ruleId;
            
            @JsonProperty("ruleName")
            private String ruleName;
            
            @JsonProperty("condition")
            private String condition;
            
            @JsonProperty("errorMessage")
            private String errorMessage;
            
            @JsonProperty("isBlocking")
            private Boolean isBlocking;
        }
    }
    
    /**
     * Workflow stakeholder
     */
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Stakeholder {
        @JsonProperty("stakeholderId")
        private String stakeholderId;
        
        @JsonProperty("role")
        private StakeholderRole role;
        
        @JsonProperty("permissions")
        private List<String> permissions;
        
        @JsonProperty("notificationPreferences")
        private Map<String, Boolean> notificationPreferences;
        
        @JsonProperty("isActive")
        private Boolean isActive;
        
        public enum StakeholderRole {
            REQUESTER,
            VERIFIER,
            APPROVER,
            REVIEWER,
            OBSERVER,
            ADMIN,
            AUDITOR,
            LEGAL_COUNSEL,
            TECHNICAL_EXPERT,
            COMPLIANCE_OFFICER
        }
    }
    
    /**
     * Service Level Agreement for workflow
     */
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class WorkflowSLA {
        @JsonProperty("maxDurationHours")
        private Integer maxDurationHours;
        
        @JsonProperty("targetDurationHours")
        private Integer targetDurationHours;
        
        @JsonProperty("priorityLevel")
        private PriorityLevel priorityLevel;
        
        @JsonProperty("businessHoursOnly")
        private Boolean businessHoursOnly;
        
        @JsonProperty("penaltyClause")
        private String penaltyClause;
        
        @JsonProperty("bonusClause")
        private String bonusClause;
        
        public enum PriorityLevel {
            LOW,
            NORMAL,
            HIGH,
            URGENT,
            CRITICAL
        }
    }
    
    /**
     * Escalation rule
     */
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class EscalationRule {
        @JsonProperty("ruleId")
        private String ruleId;
        
        @JsonProperty("condition")
        private EscalationCondition condition;
        
        @JsonProperty("thresholdMinutes")
        private Integer thresholdMinutes;
        
        @JsonProperty("escalateTo")
        private String escalateTo;
        
        @JsonProperty("action")
        private EscalationAction action;
        
        @JsonProperty("isActive")
        private Boolean isActive;
        
        public enum EscalationCondition {
            TIME_EXCEEDED,
            STEP_FAILED,
            NO_PROGRESS,
            QUALITY_ISSUE,
            STAKEHOLDER_DISPUTE,
            RESOURCE_UNAVAILABLE,
            COMPLIANCE_CONCERN
        }
        
        public enum EscalationAction {
            NOTIFY_SUPERVISOR,
            REASSIGN_VERIFIER,
            REQUEST_ADDITIONAL_RESOURCES,
            EXPEDITE_PROCESS,
            INVOKE_EMERGENCY_PROTOCOL,
            ESCALATE_TO_MANAGEMENT,
            CONTACT_EXTERNAL_EXPERT
        }
    }
    
    /**
     * Approval record
     */
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Approval {
        @JsonProperty("approvalId")
        private String approvalId;
        
        @JsonProperty("stepId")
        private String stepId;
        
        @JsonProperty("approverId")
        private String approverId;
        
        @JsonProperty("status")
        private ApprovalStatus status;
        
        @JsonProperty("decision")
        private ApprovalDecision decision;
        
        @JsonProperty("comments")
        private String comments;
        
        @JsonProperty("conditions")
        private List<String> conditions;
        
        @JsonProperty("requestedAt")
        private Instant requestedAt;
        
        @JsonProperty("decidedAt")
        private Instant decidedAt;
        
        @JsonProperty("expiresAt")
        private Instant expiresAt;
        
        public enum ApprovalStatus {
            PENDING,
            APPROVED,
            REJECTED,
            CONDITIONAL,
            EXPIRED,
            WITHDRAWN
        }
        
        public enum ApprovalDecision {
            APPROVE,
            REJECT,
            APPROVE_WITH_CONDITIONS,
            DEFER,
            ESCALATE
        }
    }
    
    /**
     * Notification record
     */
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Notification {
        @JsonProperty("notificationId")
        private String notificationId;
        
        @JsonProperty("recipientId")
        private String recipientId;
        
        @JsonProperty("type")
        private NotificationType type;
        
        @JsonProperty("channel")
        private NotificationChannel channel;
        
        @JsonProperty("subject")
        private String subject;
        
        @JsonProperty("message")
        private String message;
        
        @JsonProperty("sentAt")
        private Instant sentAt;
        
        @JsonProperty("deliveredAt")
        private Instant deliveredAt;
        
        @JsonProperty("readAt")
        private Instant readAt;
        
        public enum NotificationType {
            WORKFLOW_STARTED,
            STEP_COMPLETED,
            APPROVAL_REQUIRED,
            DEADLINE_APPROACHING,
            ESCALATION,
            COMPLETION,
            ERROR,
            REMINDER
        }
        
        public enum NotificationChannel {
            EMAIL,
            SMS,
            IN_APP,
            WEBHOOK,
            PUSH_NOTIFICATION
        }
    }
    
    /**
     * Workflow document
     */
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class WorkflowDocument {
        @JsonProperty("documentId")
        private String documentId;
        
        @JsonProperty("stepId")
        private String stepId;
        
        @JsonProperty("documentType")
        private DocumentType documentType;
        
        @JsonProperty("fileName")
        private String fileName;
        
        @JsonProperty("fileUrl")
        private String fileUrl;
        
        @JsonProperty("hash")
        private String hash;
        
        @JsonProperty("uploadedBy")
        private String uploadedBy;
        
        @JsonProperty("uploadedAt")
        private Instant uploadedAt;
        
        @JsonProperty("isRequired")
        private Boolean isRequired;
        
        @JsonProperty("isVerified")
        private Boolean isVerified;
        
        public enum DocumentType {
            INPUT_DOCUMENT,
            EVIDENCE,
            REPORT,
            CERTIFICATE,
            APPROVAL_FORM,
            CHECKLIST,
            PHOTO,
            VIDEO,
            AUDIT_TRAIL,
            COMPLIANCE_DOCUMENT
        }
    }
    
    /**
     * Workflow checkpoint
     */
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Checkpoint {
        @JsonProperty("checkpointId")
        private String checkpointId;
        
        @JsonProperty("name")
        private String name;
        
        @JsonProperty("timestamp")
        private Instant timestamp;
        
        @JsonProperty("status")
        private VerificationStatus status;
        
        @JsonProperty("progress")
        private BigDecimal progress; // percentage
        
        @JsonProperty("notes")
        private String notes;
        
        @JsonProperty("metrics")
        private Map<String, Object> metrics;
    }
    
    /**
     * Workflow metrics
     */
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class WorkflowMetrics {
        @JsonProperty("totalSteps")
        private Integer totalSteps;
        
        @JsonProperty("completedSteps")
        private Integer completedSteps;
        
        @JsonProperty("failedSteps")
        private Integer failedSteps;
        
        @JsonProperty("overallProgress")
        private BigDecimal overallProgress; // percentage
        
        @JsonProperty("averageStepDuration")
        private Long averageStepDuration; // minutes
        
        @JsonProperty("totalDuration")
        private Long totalDuration; // minutes
        
        @JsonProperty("efficiency")
        private BigDecimal efficiency; // actual vs estimated time
        
        @JsonProperty("qualityScore")
        private BigDecimal qualityScore; // 0-100
        
        @JsonProperty("stakeholderSatisfaction")
        private BigDecimal stakeholderSatisfaction; // 0-100
    }
    
    /**
     * Audit trail
     */
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class AuditTrail {
        @JsonProperty("events")
        private List<AuditEvent> events;
        
        @JsonProperty("integrityHash")
        private String integrityHash;
        
        @JsonProperty("isImmutable")
        private Boolean isImmutable;
        
        @Data
        @Builder
        @AllArgsConstructor
        @NoArgsConstructor
        public static class AuditEvent {
            @JsonProperty("eventId")
            private String eventId;
            
            @JsonProperty("timestamp")
            private Instant timestamp;
            
            @JsonProperty("actor")
            private String actor;
            
            @JsonProperty("action")
            private String action;
            
            @JsonProperty("target")
            private String target;
            
            @JsonProperty("oldValue")
            private String oldValue;
            
            @JsonProperty("newValue")
            private String newValue;
            
            @JsonProperty("reason")
            private String reason;
            
            @JsonProperty("ipAddress")
            private String ipAddress;
            
            @JsonProperty("userAgent")
            private String userAgent;
        }
    }
    
    // Constructor with essential fields
    public VerificationWorkflow(String verificationId, WorkflowType workflowType) {
        this.workflowId = java.util.UUID.randomUUID().toString();
        this.verificationId = verificationId;
        this.workflowType = workflowType;
        this.currentStatus = VerificationStatus.REQUESTED;
        this.startedAt = Instant.now();
        this.lastActivityAt = Instant.now();
        this.steps = new java.util.ArrayList<>();
        this.completedSteps = new java.util.ArrayList<>();
        this.pendingSteps = new java.util.ArrayList<>();
        this.failedSteps = new java.util.ArrayList<>();
        this.skippedSteps = new java.util.ArrayList<>();
        this.assignedVerifiers = new java.util.ArrayList<>();
        this.stakeholders = new java.util.ArrayList<>();
        this.escalationRules = new java.util.ArrayList<>();
        this.approvals = new java.util.ArrayList<>();
        this.notifications = new java.util.ArrayList<>();
        this.documents = new java.util.ArrayList<>();
        this.checkpoints = new java.util.ArrayList<>();
    }
    
    /**
     * Check if workflow is completed
     */
    public boolean isCompleted() {
        return currentStatus != null && currentStatus.isCompleted();
    }
    
    /**
     * Check if workflow is successful
     */
    public boolean isSuccessful() {
        return currentStatus != null && currentStatus.isSuccessful();
    }
    
    /**
     * Check if workflow is overdue
     */
    public boolean isOverdue() {
        return expectedCompletionAt != null && 
               expectedCompletionAt.isBefore(Instant.now()) && 
               !isCompleted();
    }
    
    /**
     * Get current progress percentage
     */
    public BigDecimal getCurrentProgress() {
        if (steps == null || steps.isEmpty()) {
            return BigDecimal.ZERO;
        }
        
        long completedCount = steps.stream()
                .filter(step -> step.getStatus() == WorkflowStep.StepStatus.COMPLETED)
                .count();
        
        return BigDecimal.valueOf(completedCount)
                .divide(BigDecimal.valueOf(steps.size()), 2, BigDecimal.ROUND_HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }
    
    /**
     * Get next step to execute
     */
    public WorkflowStep getNextStep() {
        return steps.stream()
                .filter(step -> step.getStatus() == WorkflowStep.StepStatus.READY ||
                               step.getStatus() == WorkflowStep.StepStatus.PENDING)
                .filter(step -> areDependenciesMet(step))
                .min((s1, s2) -> Integer.compare(s1.getOrder(), s2.getOrder()))
                .orElse(null);
    }
    
    /**
     * Check if step dependencies are met
     */
    private boolean areDependenciesMet(WorkflowStep step) {
        if (step.getDependencies() == null || step.getDependencies().isEmpty()) {
            return true;
        }
        
        return step.getDependencies().stream()
                .allMatch(depId -> completedSteps.contains(depId));
    }
    
    /**
     * Add checkpoint
     */
    public void addCheckpoint(String name, String notes) {
        Checkpoint checkpoint = Checkpoint.builder()
                .checkpointId(java.util.UUID.randomUUID().toString())
                .name(name)
                .timestamp(Instant.now())
                .status(currentStatus)
                .progress(getCurrentProgress())
                .notes(notes)
                .build();
        
        if (checkpoints == null) {
            checkpoints = new java.util.ArrayList<>();
        }
        checkpoints.add(checkpoint);
        updateLastActivity();
    }
    
    /**
     * Update last activity timestamp
     */
    public void updateLastActivity() {
        this.lastActivityAt = Instant.now();
    }
    
    /**
     * Get workflow summary
     */
    public String getWorkflowSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append("Workflow ").append(workflowId).append(" (").append(workflowType).append("): ");
        summary.append(currentStatus);
        
        if (metrics != null && metrics.getOverallProgress() != null) {
            summary.append(" - ").append(metrics.getOverallProgress()).append("% complete");
        }
        
        if (isOverdue()) {
            summary.append(" [OVERDUE]");
        }
        
        return summary.toString();
    }
}