package io.aurigraph.v11.compliance.erc3643;

import jakarta.enterprise.context.ApplicationScoped;
import io.quarkus.logging.Log;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * ERC-3643 Compliance Registry - Manages compliance attestations and certifications
 * Tracks regulatory certifications, licenses, and compliance standards adherence
 *
 * Compliance: SOX, Dodd-Frank, MiFID II
 * Reference: https://eips.ethereum.org/EIPS/eip-3643
 */
@ApplicationScoped
public class ComplianceRegistry {

    // Compliance records per token
    private final Map<String, ComplianceRecord> complianceRecords = new ConcurrentHashMap<>();

    // Compliance modules (validators)
    private final Map<String, ComplianceModule> complianceModules = new ConcurrentHashMap<>();

    // Audit trail
    private final List<ComplianceEvent> auditTrail = Collections.synchronizedList(new ArrayList<>());

    // Statistics
    private final AtomicLong totalChecks = new AtomicLong(0);
    private final AtomicLong passedChecks = new AtomicLong(0);
    private final AtomicLong failedChecks = new AtomicLong(0);

    /**
     * Register a compliance record for a token
     */
    public ComplianceRecord registerCompliance(String tokenId, ComplianceRecord record) {
        if (tokenId == null || tokenId.isEmpty()) {
            throw new IllegalArgumentException("Token ID cannot be null or empty");
        }

        complianceRecords.put(tokenId, record);
        auditTrail.add(new ComplianceEvent(
            tokenId, "REGISTERED", "Compliance record registered",
            Instant.now()
        ));

        Log.infof("Compliance registered for token: %s", tokenId);
        return record;
    }

    /**
     * Get compliance record for a token
     */
    public ComplianceRecord getCompliance(String tokenId) {
        return complianceRecords.get(tokenId);
    }

    /**
     * Check if token is compliant with all requirements
     */
    public ComplianceCheckResult checkCompliance(String tokenId) {
        ComplianceRecord record = complianceRecords.get(tokenId);
        if (record == null) {
            totalChecks.incrementAndGet();
            failedChecks.incrementAndGet();
            return new ComplianceCheckResult(false, "No compliance record found");
        }

        List<String> violations = new ArrayList<>();

        // Check all registered modules
        for (ComplianceModule module : complianceModules.values()) {
            if (!module.validate(record)) {
                violations.add(module.getName() + ": " + module.getLastError());
            }
        }

        // Check certifications
        if (!record.certifications.isEmpty()) {
            for (Certification cert : record.certifications) {
                if (cert.isExpired()) {
                    violations.add("Certification expired: " + cert.getName());
                }
            }
        }

        boolean passed = violations.isEmpty();
        totalChecks.incrementAndGet();
        if (passed) {
            passedChecks.incrementAndGet();
        } else {
            failedChecks.incrementAndGet();
        }

        auditTrail.add(new ComplianceEvent(
            tokenId, "CHECKED", passed ? "PASSED" : "FAILED: " + String.join("; ", violations),
            Instant.now()
        ));

        return new ComplianceCheckResult(passed, violations.isEmpty() ? null : String.join("; ", violations));
    }

    /**
     * Add certification to compliance record
     */
    public void addCertification(String tokenId, Certification certification) {
        ComplianceRecord record = complianceRecords.get(tokenId);
        if (record != null) {
            record.addCertification(certification);
            auditTrail.add(new ComplianceEvent(
                tokenId, "CERTIFICATION_ADDED", "Added: " + certification.getName(),
                Instant.now()
            ));
            Log.infof("Certification added to token %s: %s", tokenId, certification.getName());
        }
    }

    /**
     * Remove certification from compliance record
     */
    public void removeCertification(String tokenId, String certificationName) {
        ComplianceRecord record = complianceRecords.get(tokenId);
        if (record != null) {
            record.removeCertification(certificationName);
            auditTrail.add(new ComplianceEvent(
                tokenId, "CERTIFICATION_REMOVED", "Removed: " + certificationName,
                Instant.now()
            ));
        }
    }

    /**
     * Register a compliance validation module
     */
    public void registerModule(String moduleName, ComplianceModule module) {
        complianceModules.put(moduleName, module);
        Log.infof("Compliance module registered: %s", moduleName);
    }

    /**
     * Get all registered modules
     */
    public Collection<ComplianceModule> getModules() {
        return complianceModules.values();
    }

    /**
     * Get audit trail for a token
     */
    public List<ComplianceEvent> getAuditTrail(String tokenId) {
        return auditTrail.stream()
            .filter(event -> event.getTokenId().equals(tokenId))
            .toList();
    }

    /**
     * Get compliance statistics
     */
    public ComplianceStats getStats() {
        return new ComplianceStats(
            totalChecks.get(),
            passedChecks.get(),
            failedChecks.get(),
            complianceRecords.size(),
            complianceModules.size(),
            auditTrail.size()
        );
    }

    // Inner classes

    /**
     * Compliance record for a token
     */
    public static class ComplianceRecord {
        private final String tokenId;
        private final String jurisdiction;
        private final Set<String> applicableRules = ConcurrentHashMap.newKeySet();
        private final Set<Certification> certifications = ConcurrentHashMap.newKeySet();
        private final Map<String, Object> metadata = new ConcurrentHashMap<>();
        private Instant createdAt = Instant.now();
        private Instant lastUpdatedAt = Instant.now();

        public ComplianceRecord(String tokenId, String jurisdiction) {
            this.tokenId = tokenId;
            this.jurisdiction = jurisdiction;
        }

        public String getTokenId() { return tokenId; }
        public String getJurisdiction() { return jurisdiction; }

        public void addRule(String rule) {
            applicableRules.add(rule);
            lastUpdatedAt = Instant.now();
        }

        public void removeRule(String rule) {
            applicableRules.remove(rule);
            lastUpdatedAt = Instant.now();
        }

        public boolean hasRule(String rule) { return applicableRules.contains(rule); }
        public Set<String> getRules() { return new HashSet<>(applicableRules); }

        public void addCertification(Certification certification) {
            certifications.add(certification);
            lastUpdatedAt = Instant.now();
        }

        public void removeCertification(String name) {
            certifications.removeIf(c -> c.getName().equals(name));
            lastUpdatedAt = Instant.now();
        }

        public Set<Certification> getCertifications() { return new HashSet<>(certifications); }

        public void setMetadata(String key, Object value) {
            metadata.put(key, value);
            lastUpdatedAt = Instant.now();
        }

        public Object getMetadata(String key) { return metadata.get(key); }

        public Instant getCreatedAt() { return createdAt; }
        public Instant getLastUpdatedAt() { return lastUpdatedAt; }
    }

    /**
     * Certification record
     */
    public static class Certification {
        private final String name;
        private final String issuer;
        private final Instant issuedDate;
        private final Instant expiryDate;
        private final String certificateHash;

        public Certification(String name, String issuer, Instant issuedDate,
                           Instant expiryDate, String certificateHash) {
            this.name = name;
            this.issuer = issuer;
            this.issuedDate = issuedDate;
            this.expiryDate = expiryDate;
            this.certificateHash = certificateHash;
        }

        public String getName() { return name; }
        public String getIssuer() { return issuer; }
        public Instant getIssuedDate() { return issuedDate; }
        public Instant getExpiryDate() { return expiryDate; }
        public String getCertificateHash() { return certificateHash; }

        public boolean isExpired() {
            return expiryDate != null && expiryDate.isBefore(Instant.now());
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Certification that = (Certification) o;
            return Objects.equals(name, that.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name);
        }
    }

    /**
     * Compliance validation module interface
     */
    public interface ComplianceModule {
        String getName();
        boolean validate(ComplianceRecord record);
        String getLastError();
    }

    /**
     * Compliance check result
     */
    public static class ComplianceCheckResult {
        private final boolean compliant;
        private final String details;

        public ComplianceCheckResult(boolean compliant, String details) {
            this.compliant = compliant;
            this.details = details;
        }

        public boolean isCompliant() { return compliant; }
        public String getDetails() { return details; }
    }

    /**
     * Compliance event for audit trail
     */
    public static class ComplianceEvent {
        private final String tokenId;
        private final String eventType;
        private final String description;
        private final Instant timestamp;

        public ComplianceEvent(String tokenId, String eventType, String description,
                              Instant timestamp) {
            this.tokenId = tokenId;
            this.eventType = eventType;
            this.description = description;
            this.timestamp = timestamp;
        }

        public String getTokenId() { return tokenId; }
        public String getEventType() { return eventType; }
        public String getDescription() { return description; }
        public Instant getTimestamp() { return timestamp; }
    }

    /**
     * Compliance statistics
     */
    public static class ComplianceStats {
        private final long totalChecks;
        private final long passedChecks;
        private final long failedChecks;
        private final int totalRecords;
        private final int totalModules;
        private final int auditTrailSize;

        public ComplianceStats(long totalChecks, long passedChecks, long failedChecks,
                              int totalRecords, int totalModules, int auditTrailSize) {
            this.totalChecks = totalChecks;
            this.passedChecks = passedChecks;
            this.failedChecks = failedChecks;
            this.totalRecords = totalRecords;
            this.totalModules = totalModules;
            this.auditTrailSize = auditTrailSize;
        }

        public long getTotalChecks() { return totalChecks; }
        public long getPassedChecks() { return passedChecks; }
        public long getFailedChecks() { return failedChecks; }
        public int getTotalRecords() { return totalRecords; }
        public int getTotalModules() { return totalModules; }
        public int getAuditTrailSize() { return auditTrailSize; }
        public double getComplianceRate() {
            return totalChecks == 0 ? 0 : (passedChecks * 100.0) / totalChecks;
        }
    }
}
