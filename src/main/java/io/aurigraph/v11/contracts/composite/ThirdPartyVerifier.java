package io.aurigraph.v11.contracts.composite;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Third-party verifier entity
 */
public class ThirdPartyVerifier {
    private String verifierId;
    private String name;
    private VerifierTier tier;
    private String specialization;
    private String description;
    private String contactInfo;
    private VerifierStatus status;
    private Instant registeredAt;
    private Instant approvedAt;
    private int completedVerifications;
    private BigDecimal successRate;
    private long averageResponseTime; // in hours

    public ThirdPartyVerifier(String name, VerifierTier tier, String specialization, 
                            String description, String contactInfo) {
        this.name = name;
        this.tier = tier;
        this.specialization = specialization;
        this.description = description;
        this.contactInfo = contactInfo;
        this.status = VerifierStatus.PENDING_APPROVAL;
        this.completedVerifications = 0;
        this.successRate = BigDecimal.ZERO;
        this.averageResponseTime = 48; // Default 48 hours
    }

    public void incrementCompletedVerifications() {
        this.completedVerifications++;
    }

    // Getters and setters
    public String getVerifierId() { return verifierId; }
    public void setVerifierId(String verifierId) { this.verifierId = verifierId; }
    
    public String getName() { return name; }
    public VerifierTier getTier() { return tier; }
    public String getSpecialization() { return specialization; }
    public String getDescription() { return description; }
    public String getContactInfo() { return contactInfo; }
    
    public VerifierStatus getStatus() { return status; }
    public void setStatus(VerifierStatus status) { this.status = status; }
    
    public Instant getRegisteredAt() { return registeredAt; }
    public void setRegisteredAt(Instant registeredAt) { this.registeredAt = registeredAt; }
    
    public Instant getApprovedAt() { return approvedAt; }
    public void setApprovedAt(Instant approvedAt) { this.approvedAt = approvedAt; }
    
    public int getCompletedVerifications() { return completedVerifications; }
    public BigDecimal getSuccessRate() { return successRate; }
    public void setSuccessRate(BigDecimal successRate) { this.successRate = successRate; }
    
    public long getAverageResponseTime() { return averageResponseTime; }
    public void setAverageResponseTime(long averageResponseTime) { this.averageResponseTime = averageResponseTime; }
}