package io.aurigraph.v11.registries.compliance;

import io.quarkus.logging.Log;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Compliance Registry Service
 *
 * Core business logic for managing compliance certifications, regulatory status,
 * and compliance verification. Supports multi-standard compliance frameworks.
 *
 * Features:
 * - Certification lifecycle management (issuance, renewal, revocation)
 * - Multi-level compliance scoring (LEVEL_1 to LEVEL_5)
 * - Expiry detection and renewal notifications
 * - Audit trail tracking for all compliance operations
 * - Support for multiple compliance standards (ISO, SOC2, NIST, ERC-3643, etc.)
 * - Compliance metrics and analytics
 *
 * @version 11.5.0
 * @since 2025-11-14
 */
@ApplicationScoped
public class ComplianceRegistryService {

    // Storage for compliance registrations
    private final Map<String, ComplianceRegistryEntry> certifications = new ConcurrentHashMap<>();

    // Entity-to-certifications mapping
    private final Map<String, Set<String>> entityCertifications = new ConcurrentHashMap<>();

    // Compliance metrics
    private final ComplianceMetrics metrics = new ComplianceMetrics();

    /**
     * Add certification to an entity
     */
    public Uni<ComplianceRegistryEntry> addCertification(
            String entityId,
            String certificationType,
            String issuingAuthority,
            String certificationId,
            Instant issuanceDate,
            Instant expiryDate,
            String status
    ) {
        return Uni.createFrom().item(() -> {
            // Validate inputs
            if (entityId == null || entityId.isEmpty()) {
                throw new IllegalArgumentException("Entity ID cannot be null or empty");
            }
            if (certificationType == null || certificationType.isEmpty()) {
                throw new IllegalArgumentException("Certificate type cannot be null or empty");
            }
            if (issuanceDate == null || expiryDate == null) {
                throw new IllegalArgumentException("Issuance and expiry dates are required");
            }

            Log.infof("Adding certification: entity=%s, type=%s, certId=%s",
                    entityId, certificationType, certificationId);

            // Create entry
            ComplianceRegistryEntry entry = new ComplianceRegistryEntry(
                    entityId, certificationType, issuingAuthority, certificationId,
                    issuanceDate, expiryDate
            );

            // Set status
            if (status != null) {
                try {
                    entry.setCurrentStatus(
                            ComplianceRegistryEntry.CertificationStatus.valueOf(status)
                    );
                } catch (IllegalArgumentException e) {
                    entry.setCurrentStatus(ComplianceRegistryEntry.CertificationStatus.ACTIVE);
                }
            }

            // Determine compliance level based on certificate type
            ComplianceLevelEnum complianceLevel = determineComplianceLevel(certificationType);
            entry.setComplianceLevel(complianceLevel);

            // Store certification
            certifications.put(certificationId, entry);

            // Add to entity mapping
            entityCertifications.computeIfAbsent(entityId, k -> ConcurrentHashMap.newKeySet())
                    .add(certificationId);

            // Update metrics
            metrics.incrementTotalCertifications();
            if (entry.getCurrentStatus() == ComplianceRegistryEntry.CertificationStatus.ACTIVE) {
                metrics.incrementActiveCertifications();
            }

            entry.addAuditEvent("CREATED", "Certification created and registered", "SYSTEM");

            Log.infof("Certification added successfully: %s", entry);

            return entry;
        });
    }

    /**
     * Get all certifications for an entity
     */
    public Uni<List<ComplianceRegistryEntry>> getCertifications(String entityId) {
        return Uni.createFrom().item(() -> {
            Log.infof("Retrieving certifications for entity: %s", entityId);

            Set<String> certIds = entityCertifications.getOrDefault(entityId, Collections.emptySet());
            return certIds.stream()
                    .map(certifications::get)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        });
    }

    /**
     * Verify entity compliance status
     */
    public Uni<ComplianceVerificationResult> verifyCompliance(String entityId, String complianceLevelStr) {
        return Uni.createFrom().item(() -> {
            Log.infof("Verifying compliance for entity: %s", entityId);

            List<ComplianceRegistryEntry> entityCerts = certifications.values().stream()
                    .filter(c -> c.getEntityId().equals(entityId))
                    .collect(Collectors.toList());

            if (entityCerts.isEmpty()) {
                return new ComplianceVerificationResult(
                        false,
                        "Entity has no compliance certifications",
                        ComplianceLevelEnum.LEVEL_1,
                        0,
                        Collections.emptyList()
                );
            }

            // Check against required compliance level
            ComplianceLevelEnum requiredLevel = ComplianceLevelEnum.LEVEL_1;
            try {
                if (complianceLevelStr != null && !complianceLevelStr.isEmpty()) {
                    requiredLevel = ComplianceLevelEnum.valueOf("LEVEL_" + complianceLevelStr);
                }
            } catch (IllegalArgumentException ignored) {
                // Use default LEVEL_1
            }

            // Check if entity meets required level
            ComplianceLevelEnum maxLevel = entityCerts.stream()
                    .map(ComplianceRegistryEntry::getComplianceLevel)
                    .max(Comparator.comparingInt(ComplianceLevelEnum::getLevel))
                    .orElse(ComplianceLevelEnum.LEVEL_1);

            boolean compliant = maxLevel.meetsOrExceeds(requiredLevel);

            // Count active vs expired
            long activeCount = entityCerts.stream()
                    .filter(c -> c.getCurrentStatus() == ComplianceRegistryEntry.CertificationStatus.ACTIVE)
                    .count();
            long expiredCount = entityCerts.stream()
                    .filter(ComplianceRegistryEntry::isExpired)
                    .count();

            // Calculate compliance score
            double complianceScore = calculateEntityComplianceScore(entityId);

            List<String> issues = new ArrayList<>();
            if (expiredCount > 0) {
                issues.add("Entity has " + expiredCount + " expired certifications");
            }

            List<ComplianceRegistryEntry> criticalRenewals = entityCerts.stream()
                    .filter(ComplianceRegistryEntry::isInCriticalRenewalWindow)
                    .collect(Collectors.toList());
            if (!criticalRenewals.isEmpty()) {
                issues.add("Entity has " + criticalRenewals.size() + " certifications in critical renewal window");
            }

            Log.infof("Compliance verification result for %s: %s, score=%.2f%%",
                    entityId, compliant ? "COMPLIANT" : "NON-COMPLIANT", complianceScore);

            return new ComplianceVerificationResult(
                    compliant,
                    compliant ? "Entity meets required compliance level" :
                            "Entity does not meet required compliance level",
                    maxLevel,
                    complianceScore,
                    issues
            );
        });
    }

    /**
     * Get expired certifications
     */
    public Uni<List<ComplianceRegistryEntry>> getExpiredCertifications() {
        return Uni.createFrom().item(() -> {
            Log.infof("Retrieving expired certifications");

            return certifications.values().stream()
                    .filter(ComplianceRegistryEntry::isExpired)
                    .collect(Collectors.toList());
        });
    }

    /**
     * Renew certification
     */
    public Uni<ComplianceRegistryEntry> renewCertification(String certificationId, Instant newExpiryDate) {
        return Uni.createFrom().item(() -> {
            Log.infof("Renewing certification: %s", certificationId);

            ComplianceRegistryEntry entry = certifications.get(certificationId);
            if (entry == null) {
                throw new IllegalArgumentException("Certification not found: " + certificationId);
            }

            if (newExpiryDate == null || newExpiryDate.isBefore(Instant.now())) {
                throw new IllegalArgumentException("New expiry date must be in the future");
            }

            entry.renew(newExpiryDate);
            metrics.incrementRenewalCount();

            Log.infof("Certification renewed: %s, new expiry: %s", certificationId, newExpiryDate);

            return entry;
        });
    }

    /**
     * Revoke certification
     */
    public Uni<ComplianceRegistryEntry> revokeCertification(String certificationId) {
        return Uni.createFrom().item(() -> {
            Log.infof("Revoking certification: %s", certificationId);

            ComplianceRegistryEntry entry = certifications.get(certificationId);
            if (entry == null) {
                throw new IllegalArgumentException("Certification not found: " + certificationId);
            }

            entry.revoke("Admin revocation");
            metrics.decrementActiveCertifications();

            Log.infof("Certification revoked: %s", certificationId);

            return entry;
        });
    }

    /**
     * Get compliance metrics and statistics
     */
    public Uni<ComplianceMetrics> getComplianceMetrics() {
        return Uni.createFrom().item(() -> {
            Log.infof("Retrieving compliance metrics");

            // Update metrics
            metrics.setTotalEntities(entityCertifications.size());
            metrics.setAverageComplianceScore(
                    certifications.values().stream()
                            .map(c -> c.getEntityId())
                            .distinct()
                            .mapToDouble(this::calculateEntityComplianceScore)
                            .average()
                            .orElse(0.0)
            );

            // Count by level
            Map<String, Long> byLevel = certifications.values().stream()
                    .collect(Collectors.groupingBy(
                            c -> c.getComplianceLevel().name(),
                            Collectors.counting()
                    ));
            metrics.setCertificationsByLevel(byLevel);

            // Count by status
            Map<String, Long> byStatus = certifications.values().stream()
                    .collect(Collectors.groupingBy(
                            c -> c.getCurrentStatus().name(),
                            Collectors.counting()
                    ));
            metrics.setCertificationsByStatus(byStatus);

            return metrics;
        });
    }

    /**
     * Get certification by ID
     */
    public Uni<ComplianceRegistryEntry> getCertification(String certificationId) {
        return Uni.createFrom().item(() -> {
            ComplianceRegistryEntry entry = certifications.get(certificationId);
            if (entry == null) {
                throw new IllegalArgumentException("Certification not found: " + certificationId);
            }
            return entry;
        });
    }

    /**
     * List certifications by type
     */
    public Uni<List<ComplianceRegistryEntry>> getCertificationsByType(String certificationType) {
        return Uni.createFrom().item(() ->
                certifications.values().stream()
                        .filter(c -> c.getCertificationType().equalsIgnoreCase(certificationType))
                        .collect(Collectors.toList())
        );
    }

    /**
     * List certifications in renewal window
     */
    public Uni<List<ComplianceRegistryEntry>> getCertificationsInRenewalWindow() {
        return Uni.createFrom().item(() ->
                certifications.values().stream()
                        .filter(ComplianceRegistryEntry::isRenewalWindowOpen)
                        .collect(Collectors.toList())
        );
    }

    /**
     * List certifications in critical renewal window
     */
    public Uni<List<ComplianceRegistryEntry>> getCertificationsInCriticalWindow() {
        return Uni.createFrom().item(() ->
                certifications.values().stream()
                        .filter(ComplianceRegistryEntry::isInCriticalRenewalWindow)
                        .collect(Collectors.toList())
        );
    }

    /**
     * Calculate entity compliance score (0-100)
     */
    private double calculateEntityComplianceScore(String entityId) {
        List<ComplianceRegistryEntry> entityCerts = certifications.values().stream()
                .filter(c -> c.getEntityId().equals(entityId))
                .collect(Collectors.toList());

        if (entityCerts.isEmpty()) {
            return 0.0;
        }

        double totalScore = 0.0;
        for (ComplianceRegistryEntry cert : entityCerts) {
            // Active status contributes to score
            if (cert.getCurrentStatus() == ComplianceRegistryEntry.CertificationStatus.ACTIVE) {
                totalScore += cert.getComplianceLevel().getComplianceScore();
            }
            // Expired certifications don't contribute
        }

        // Normalize by number of certifications
        return (totalScore / entityCerts.size()) * 100.0 / 100.0;
    }

    /**
     * Determine compliance level based on certificate type
     */
    private ComplianceLevelEnum determineComplianceLevel(String certificationType) {
        String upper = certificationType.toUpperCase();

        // Quantum-safe certifications
        if (upper.contains("NIST") || upper.contains("QUANTUM") ||
                upper.contains("CRYSTALS") || upper.contains("DILITHIUM") ||
                upper.contains("KYBER")) {
            return ComplianceLevelEnum.LEVEL_5;
        }

        // Advanced certifications
        if (upper.contains("ISO") || upper.contains("SOC2") ||
                upper.contains("HIPAA") || upper.contains("GDPR")) {
            return ComplianceLevelEnum.LEVEL_3;
        }

        // Enhanced certifications
        if (upper.contains("KYC") || upper.contains("AML") ||
                upper.contains("MIFID") || upper.contains("DODD")) {
            return ComplianceLevelEnum.LEVEL_2;
        }

        // ERC-3643 is advanced
        if (upper.contains("ERC")) {
            return ComplianceLevelEnum.LEVEL_3;
        }

        // Default to LEVEL_1
        return ComplianceLevelEnum.LEVEL_1;
    }

    /**
     * Compliance Metrics DTO
     */
    public static class ComplianceMetrics {
        private long totalCertifications = 0;
        private long activeCertifications = 0;
        private long expiredCertifications = 0;
        private long renewalCount = 0;
        private int totalEntities = 0;
        private double averageComplianceScore = 0.0;
        private Map<String, Long> certificationsByLevel = new HashMap<>();
        private Map<String, Long> certificationsByStatus = new HashMap<>();

        // Getters and setters
        public long getTotalCertifications() { return totalCertifications; }
        public void setTotalCertifications(long total) { this.totalCertifications = total; }

        public long getActiveCertifications() { return activeCertifications; }
        public void setActiveCertifications(long active) { this.activeCertifications = active; }

        public long getExpiredCertifications() { return expiredCertifications; }
        public void setExpiredCertifications(long expired) { this.expiredCertifications = expired; }

        public long getRenewalCount() { return renewalCount; }
        public void setRenewalCount(long count) { this.renewalCount = count; }

        public int getTotalEntities() { return totalEntities; }
        public void setTotalEntities(int count) { this.totalEntities = count; }

        public double getAverageComplianceScore() { return averageComplianceScore; }
        public void setAverageComplianceScore(double score) { this.averageComplianceScore = score; }

        public Map<String, Long> getCertificationsByLevel() { return certificationsByLevel; }
        public void setCertificationsByLevel(Map<String, Long> map) { this.certificationsByLevel = map; }

        public Map<String, Long> getCertificationsByStatus() { return certificationsByStatus; }
        public void setCertificationsByStatus(Map<String, Long> map) { this.certificationsByStatus = map; }

        public void incrementTotalCertifications() { this.totalCertifications++; }
        public void incrementActiveCertifications() { this.activeCertifications++; }
        public void decrementActiveCertifications() { this.activeCertifications--; }
        public void incrementRenewalCount() { this.renewalCount++; }
    }

    /**
     * Compliance Verification Result
     */
    public static class ComplianceVerificationResult {
        private final boolean compliant;
        private final String message;
        private final ComplianceLevelEnum achievedLevel;
        private final double complianceScore;
        private final List<String> issues;

        public ComplianceVerificationResult(boolean compliant, String message,
                                           ComplianceLevelEnum achievedLevel,
                                           double complianceScore, List<String> issues) {
            this.compliant = compliant;
            this.message = message;
            this.achievedLevel = achievedLevel;
            this.complianceScore = complianceScore;
            this.issues = issues;
        }

        public boolean isCompliant() { return compliant; }
        public String getMessage() { return message; }
        public ComplianceLevelEnum getAchievedLevel() { return achievedLevel; }
        public double getComplianceScore() { return complianceScore; }
        public List<String> getIssues() { return issues; }
    }
}
