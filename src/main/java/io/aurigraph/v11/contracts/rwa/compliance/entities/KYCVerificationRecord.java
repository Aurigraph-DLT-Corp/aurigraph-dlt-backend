package io.aurigraph.v11.contracts.rwa.compliance.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;

/**
 * KYC Verification Record Entity for RWA Compliance
 * Tracks know-your-customer verification results for users
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KYCVerificationRecord {

    public String userId;
    public String verificationId;
    public String jurisdiction;
    public String provider;
    public String status;
    public String documentType;
    public String referenceNumber;
    public Instant createdAt;
    public Instant updatedAt;
    public Instant expiresAt;

    /**
     * Ensure createdAt is populated
     */
    public void ensureCreatedAt() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
    }
}
