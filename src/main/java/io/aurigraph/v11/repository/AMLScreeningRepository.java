package io.aurigraph.v11.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.aurigraph.v11.contracts.rwa.compliance.entities.AMLScreeningRecord;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * AML Screening Record Repository
 * Manages AML screening records in LevelDB
 *
 * Key Format: "aml:{userId}:{screeningId}"
 *
 * @version 1.0.0
 * @since Phase 4 - LevelDB Migration (Oct 8, 2025)
 */
@ApplicationScoped
public class AMLScreeningRepository extends LevelDBRepository<AMLScreeningRecord> {

    @Inject
    public AMLScreeningRepository(ObjectMapper objectMapper) {
        super(objectMapper, AMLScreeningRecord.class, "aml-screening");
    }

    /**
     * Find AML screening record by screening ID
     *
     * @param screeningId Screening ID
     * @return Optional containing record if found
     */
    public Optional<AMLScreeningRecord> findByScreeningId(String screeningId) {
        // Search with prefix since we don't know the userId
        List<AMLScreeningRecord> records = findByKeyPrefix("aml:");

        return records.stream()
                .filter(record -> screeningId.equals(record.screeningId))
                .findFirst();
    }

    /**
     * Find AML screening record by user ID
     * Returns the most recent screening for the user
     *
     * @param userId User ID
     * @return Optional containing record if found
     */
    public Optional<AMLScreeningRecord> findByUserId(String userId) {
        String keyPrefix = "aml:" + userId + ":";
        List<AMLScreeningRecord> records = findByKeyPrefix(keyPrefix);

        if (records.isEmpty()) {
            Log.debugf("No AML records found for user: %s", userId);
            return Optional.empty();
        }

        // Return the most recent record
        return Optional.of(records.get(records.size() - 1));
    }

    /**
     * Find all AML records for a user
     *
     * @param userId User ID
     * @return List of all AML records for the user
     */
    public List<AMLScreeningRecord> findAllByUserId(String userId) {
        String keyPrefix = "aml:" + userId + ":";
        return findByKeyPrefix(keyPrefix);
    }

    /**
     * Save AML screening record
     * Automatically generates composite key from userId and screeningId
     *
     * @param record AML screening record
     */
    public void saveRecord(AMLScreeningRecord record) {
        if (record.userId == null || record.screeningId == null) {
            throw new IllegalArgumentException("userId and screeningId are required");
        }

        // Initialize timestamps if not set
        record.ensureCreatedAt();

        String key = generateKey(record.userId, record.screeningId);
        save(key, record);

        Log.infof("Saved AML record for user %s (screening: %s)",
                record.userId, record.screeningId);
    }

    /**
     * Delete AML record
     *
     * @param userId User ID
     * @param screeningId Screening ID
     */
    public void deleteRecord(String userId, String screeningId) {
        String key = generateKey(userId, screeningId);
        delete(key);
    }

    /**
     * Generate composite key
     *
     * @param userId User ID
     * @param screeningId Screening ID
     * @return Composite key
     */
    private String generateKey(String userId, String screeningId) {
        return "aml:" + userId + ":" + screeningId;
    }

    /**
     * Find records by jurisdiction
     *
     * @param jurisdiction Regulatory jurisdiction
     * @return List of matching records
     */
    public List<AMLScreeningRecord> findByJurisdiction(String jurisdiction) {
        return findAll().stream()
                .filter(record -> jurisdiction.equals(record.jurisdiction))
                .toList();
    }

    /**
     * Find records by risk level
     *
     * @param riskLevel Risk level (LOW, MEDIUM, HIGH, CRITICAL)
     * @return List of matching records
     */
    public List<AMLScreeningRecord> findByRiskLevel(String riskLevel) {
        return findAll().stream()
                .filter(record -> riskLevel.equals(record.riskLevel))
                .toList();
    }

    /**
     * Find records with sanctions hits
     *
     * @return List of records with sanctions matches
     */
    public List<AMLScreeningRecord> findWithSanctionsHits() {
        return findAll().stream()
                .filter(record -> Boolean.TRUE.equals(record.sanctionsHit))
                .toList();
    }

    /**
     * Find records with PEP status
     *
     * @return List of records with PEP matches
     */
    public List<AMLScreeningRecord> findWithPEPStatus() {
        return findAll().stream()
                .filter(record -> Boolean.TRUE.equals(record.pepStatus))
                .toList();
    }

    /**
     * Find records by risk score threshold
     *
     * @param minScore Minimum risk score
     * @return List of records with risk score >= minScore
     */
    public List<AMLScreeningRecord> findByRiskScoreAbove(int minScore) {
        return findAll().stream()
                .filter(record -> record.riskScore != null && record.riskScore.compareTo(BigDecimal.valueOf((long)minScore)) >= 0)
                .toList();
    }
}
