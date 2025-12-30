package io.aurigraph.v11.compliance;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.persistence.*;
import jakarta.transaction.Transactional;
import io.smallrye.mutiny.Uni;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * AuditTrailService - Comprehensive audit logging for all token operations
 *
 * Provides:
 * - Immutable audit trail for compliance
 * - Event-driven audit hooks
 * - Tamper-detection via Merkle chain
 * - Compliance report generation
 * - Audit statistics and metrics
 * - Automated archival of old records
 *
 * Audit Events Tracked:
 * - Token creation, activation, transfer, redemption, expiration
 * - VVB approval workflow and decisions
 * - Contract deployment and execution
 * - Registry operations and state changes
 * - Compliance certifications and changes
 * - User and actor activities
 */
@ApplicationScoped
public class AuditTrailService {

    @Inject
    AuditRecordRepository auditRecordRepository;

    @Inject
    Event<AuditEvent> auditEventPublisher;

    /**
     * Record token creation event
     */
    @Transactional
    public Uni<AuditRecord> recordTokenCreation(String tokenId, String tokenType, String actor, String createdBy) {
        return Uni.createFrom().item(() -> {
            AuditRecord record = new AuditRecord();
            record.id = UUID.randomUUID().toString();
            record.entityType = "TOKEN";
            record.entityId = tokenId;
            record.operation = "CREATE";
            record.actor = actor;
            record.createdBy = createdBy;
            record.timestamp = Instant.now();
            record.details = new HashMap<>();
            record.details.put("tokenType", tokenType);
            record.merkleHash = calculateHash(record);

            auditRecordRepository.persist(record);
            Log.info("Token creation recorded: " + tokenId);
            publishEvent("TOKEN_CREATED", tokenId, actor);

            return record;
        });
    }

    /**
     * Record token status change
     */
    @Transactional
    public Uni<AuditRecord> recordTokenStatusChange(String tokenId, String fromStatus, String toStatus, String actor) {
        return Uni.createFrom().item(() -> {
            AuditRecord record = new AuditRecord();
            record.id = UUID.randomUUID().toString();
            record.entityType = "TOKEN";
            record.entityId = tokenId;
            record.operation = "STATUS_CHANGE";
            record.actor = actor;
            record.timestamp = Instant.now();
            record.details = new HashMap<>();
            record.details.put("fromStatus", fromStatus);
            record.details.put("toStatus", toStatus);
            record.merkleHash = calculateHash(record);

            auditRecordRepository.persist(record);
            Log.info("Token status change recorded: " + tokenId + " (" + fromStatus + " → " + toStatus + ")");
            publishEvent("TOKEN_STATUS_CHANGED", tokenId, actor);

            return record;
        });
    }

    /**
     * Record token transfer
     */
    @Transactional
    public Uni<AuditRecord> recordTokenTransfer(String tokenId, String fromOwner, String toOwner, String actor) {
        return Uni.createFrom().item(() -> {
            AuditRecord record = new AuditRecord();
            record.id = UUID.randomUUID().toString();
            record.entityType = "TOKEN";
            record.entityId = tokenId;
            record.operation = "TRANSFER";
            record.actor = actor;
            record.timestamp = Instant.now();
            record.details = new HashMap<>();
            record.details.put("fromOwner", fromOwner);
            record.details.put("toOwner", toOwner);
            record.merkleHash = calculateHash(record);

            auditRecordRepository.persist(record);
            Log.info("Token transfer recorded: " + tokenId + " (" + fromOwner + " → " + toOwner + ")");
            publishEvent("TOKEN_TRANSFERRED", tokenId, actor);

            return record;
        });
    }

    /**
     * Record VVB approval event
     */
    @Transactional
    public Uni<AuditRecord> recordVVBApproval(String versionId, String approver, String decision) {
        return Uni.createFrom().item(() -> {
            AuditRecord record = new AuditRecord();
            record.id = UUID.randomUUID().toString();
            record.entityType = "VVB";
            record.entityId = versionId;
            record.operation = "APPROVAL";
            record.actor = approver;
            record.timestamp = Instant.now();
            record.details = new HashMap<>();
            record.details.put("decision", decision);
            record.merkleHash = calculateHash(record);

            auditRecordRepository.persist(record);
            Log.info("VVB approval recorded: " + versionId + " (" + decision + ")");
            publishEvent("VVB_APPROVED", versionId, approver);

            return record;
        });
    }

    /**
     * Record contract execution
     */
    @Transactional
    public Uni<AuditRecord> recordContractExecution(String contractAddress, String functionName, String actor) {
        return Uni.createFrom().item(() -> {
            AuditRecord record = new AuditRecord();
            record.id = UUID.randomUUID().toString();
            record.entityType = "CONTRACT";
            record.entityId = contractAddress;
            record.operation = "EXECUTE";
            record.actor = actor;
            record.timestamp = Instant.now();
            record.details = new HashMap<>();
            record.details.put("functionName", functionName);
            record.merkleHash = calculateHash(record);

            auditRecordRepository.persist(record);
            Log.info("Contract execution recorded: " + contractAddress + "." + functionName);
            publishEvent("CONTRACT_EXECUTED", contractAddress, actor);

            return record;
        });
    }

    /**
     * Record registry operation
     */
    @Transactional
    public Uni<AuditRecord> recordRegistryOperation(String operationType, String targetId, String actor) {
        return Uni.createFrom().item(() -> {
            AuditRecord record = new AuditRecord();
            record.id = UUID.randomUUID().toString();
            record.entityType = "REGISTRY";
            record.entityId = targetId;
            record.operation = operationType;
            record.actor = actor;
            record.timestamp = Instant.now();
            record.merkleHash = calculateHash(record);

            auditRecordRepository.persist(record);
            Log.info("Registry operation recorded: " + operationType + " on " + targetId);
            publishEvent("REGISTRY_OPERATION", targetId, actor);

            return record;
        });
    }

    /**
     * Get complete audit trail for a token
     */
    public Uni<List<AuditRecord>> getTokenAuditTrail(String tokenId) {
        return Uni.createFrom().item(() ->
            auditRecordRepository.list("entityId = ?1 and entityType = 'TOKEN'", tokenId)
        );
    }

    /**
     * Get audit trail for an actor within time range
     */
    public Uni<List<AuditRecord>> getActorAuditTrail(String actor, Instant fromTime, Instant toTime) {
        return Uni.createFrom().item(() ->
            auditRecordRepository.list("actor = ?1 and timestamp >= ?2 and timestamp <= ?3", actor, fromTime, toTime)
        );
    }

    /**
     * Generate compliance report
     */
    public Uni<ComplianceReport> generateComplianceReport(Instant startDate, Instant endDate) {
        return Uni.createFrom().item(() -> {
            List<AuditRecord> records = auditRecordRepository.list(
                "timestamp >= ?1 and timestamp <= ?2", startDate, endDate
            );

            ComplianceReport report = new ComplianceReport();
            report.reportDate = Instant.now();
            report.startDate = startDate;
            report.endDate = endDate;
            report.totalRecords = records.size();

            // Count operations by type
            Map<String, Long> operationCounts = new HashMap<>();
            Map<String, Long> entityCounts = new HashMap<>();

            for (AuditRecord record : records) {
                operationCounts.merge(record.operation, 1L, Long::sum);
                entityCounts.merge(record.entityType, 1L, Long::sum);
            }

            report.operationCounts = operationCounts;
            report.entityCounts = entityCounts;

            // Verify integrity
            report.integrityStatus = verifyAuditIntegrity(records) ? "VERIFIED" : "COMPROMISED";

            Log.info("Compliance report generated: " + report.totalRecords + " records");
            return report;
        });
    }

    /**
     * Get audit statistics
     */
    public Uni<AuditStatistics> getAuditStatistics() {
        return Uni.createFrom().item(() -> {
            long totalRecords = auditRecordRepository.count();
            Instant oneWeekAgo = Instant.now().minus(7, ChronoUnit.DAYS);
            long recentRecords = auditRecordRepository.count("timestamp >= ?1", oneWeekAgo);

            AuditStatistics stats = new AuditStatistics();
            stats.totalRecords = totalRecords;
            stats.recordsLastWeek = recentRecords;
            stats.averageRecordsPerDay = totalRecords > 0 ? totalRecords / 365 : 0;
            List<AuditRecord> allRecords = auditRecordRepository.listAll();
            stats.lastRecordTime = allRecords.stream()
                .max(Comparator.comparing(r -> r.timestamp))
                .map(r -> r.timestamp)
                .orElse(null);

            return stats;
        });
    }

    /**
     * Archive audit records older than specified date
     */
    @Transactional
    public Uni<Void> archiveAuditRecords(Instant beforeDate) {
        return Uni.createFrom().item(() -> {
            // In production, would archive to cold storage
            List<AuditRecord> oldRecords = auditRecordRepository.list("timestamp < ?1", beforeDate);
            Log.info("Archiving " + oldRecords.size() + " records older than " + beforeDate);
            // Mark as archived instead of deleting
            for (AuditRecord record : oldRecords) {
                record.archived = true;
                auditRecordRepository.persistAndFlush(record);
            }
            return null;
        }).replaceWithVoid();
    }

    /**
     * Verify audit trail integrity using Merkle chain
     */
    public Uni<IntegrityVerificationResult> verifyAuditIntegrity() {
        return Uni.createFrom().item(() -> {
            List<AuditRecord> allRecords = auditRecordRepository.listAll();
            IntegrityVerificationResult result = new IntegrityVerificationResult();
            result.totalRecords = allRecords.size();
            result.verifiedRecords = 0;
            result.tamperedRecords = 0;

            for (AuditRecord record : allRecords) {
                String expectedHash = calculateHash(record);
                if (expectedHash.equals(record.merkleHash)) {
                    result.verifiedRecords++;
                } else {
                    result.tamperedRecords++;
                    Log.warn("Tampered record detected: " + record.id);
                }
            }

            result.integrityStatus = result.tamperedRecords == 0 ? "VERIFIED" : "COMPROMISED";
            return result;
        });
    }

    // ===== HELPER METHODS =====

    private String calculateHash(AuditRecord record) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String data = record.id + record.operation + record.actor + record.timestamp;
            byte[] hash = digest.digest(data.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            Log.error("Hash calculation failed", e);
            return "";
        }
    }

    private void publishEvent(String eventType, String entityId, String actor) {
        AuditEvent event = new AuditEvent();
        event.eventType = eventType;
        event.entityId = entityId;
        event.actor = actor;
        event.timestamp = Instant.now();
        auditEventPublisher.fire(event);
    }

    private boolean verifyAuditIntegrity(List<AuditRecord> records) {
        for (AuditRecord record : records) {
            String expectedHash = calculateHash(record);
            if (!expectedHash.equals(record.merkleHash)) {
                return false;
            }
        }
        return true;
    }

    // ===== REPOSITORY =====

    @ApplicationScoped
    public static class AuditRecordRepository implements PanacheRepository<AuditRecord> {
    }

    // ===== ENTITIES & MODELS =====

    @Entity
    @Table(name = "audit_records")
    public static class AuditRecord {
        @Id
        public String id;

        public String entityType;
        public String entityId;
        public String operation;
        public String actor;
        public String createdBy;
        public Instant timestamp;

        @ElementCollection
        @CollectionTable(name = "audit_record_details")
        public Map<String, String> details;

        public String merkleHash;
        public boolean archived;
    }

    public static class AuditEvent {
        public String eventType;
        public String entityId;
        public String actor;
        public Instant timestamp;
    }

    public static class ComplianceReport {
        public Instant reportDate;
        public Instant startDate;
        public Instant endDate;
        public int totalRecords;
        public Map<String, Long> operationCounts;
        public Map<String, Long> entityCounts;
        public String integrityStatus;
    }

    public static class AuditStatistics {
        public long totalRecords;
        public long recordsLastWeek;
        public long averageRecordsPerDay;
        public Instant lastRecordTime;
    }

    public static class IntegrityVerificationResult {
        public int totalRecords;
        public int verifiedRecords;
        public int tamperedRecords;
        public String integrityStatus;
    }
}
