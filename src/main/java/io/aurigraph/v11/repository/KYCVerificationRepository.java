package io.aurigraph.v11.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.aurigraph.v11.contracts.rwa.compliance.entities.KYCVerificationRecord;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;
import java.util.Optional;

/**
 * KYC Verification Record Repository
 * Manages KYC verification records in LevelDB
 *
 * Key Format: "kyc:{userId}:{verificationId}"
 *
 * @version 1.0.0
 * @since Phase 4 - LevelDB Migration (Oct 8, 2025)
 */
@ApplicationScoped
public class KYCVerificationRepository extends LevelDBRepository<KYCVerificationRecord> {

    @Inject
    public KYCVerificationRepository(ObjectMapper objectMapper) {
        super(objectMapper, KYCVerificationRecord.class, "kyc-verification");
    }

    /**
     * Find KYC verification record by verification ID
     *
     * @param verificationId Verification ID
     * @return Optional containing record if found
     */
    public Optional<KYCVerificationRecord> findByVerificationId(String verificationId) {
        // Search with prefix since we don't know the userId
        List<KYCVerificationRecord> records = findByKeyPrefix("kyc:");

        return records.stream()
                .filter(record -> verificationId.equals(record.verificationId))
                .findFirst();
    }

    /**
     * Find KYC verification record by user ID
     * Returns the most recent verification for the user
     *
     * @param userId User ID
     * @return Optional containing record if found
     */
    public Optional<KYCVerificationRecord> findByUserId(String userId) {
        String keyPrefix = "kyc:" + userId + ":";
        List<KYCVerificationRecord> records = findByKeyPrefix(keyPrefix);

        if (records.isEmpty()) {
            Log.debugf("No KYC records found for user: %s", userId);
            return Optional.empty();
        }

        // Return the most recent record (assuming records are ordered by creation)
        return Optional.of(records.get(records.size() - 1));
    }

    /**
     * Find all KYC records for a user
     *
     * @param userId User ID
     * @return List of all KYC records for the user
     */
    public List<KYCVerificationRecord> findAllByUserId(String userId) {
        String keyPrefix = "kyc:" + userId + ":";
        return findByKeyPrefix(keyPrefix);
    }

    /**
     * Save KYC verification record
     * Automatically generates composite key from userId and verificationId
     *
     * @param record KYC verification record
     */
    public void saveRecord(KYCVerificationRecord record) {
        if (record.userId == null || record.verificationId == null) {
            throw new IllegalArgumentException("userId and verificationId are required");
        }

        // Initialize timestamps if not set
        record.ensureCreatedAt();

        String key = generateKey(record.userId, record.verificationId);
        save(key, record);

        Log.infof("Saved KYC record for user %s (verification: %s)",
                record.userId, record.verificationId);
    }

    /**
     * Delete KYC record
     *
     * @param userId User ID
     * @param verificationId Verification ID
     */
    public void deleteRecord(String userId, String verificationId) {
        String key = generateKey(userId, verificationId);
        delete(key);
    }

    /**
     * Generate composite key
     *
     * @param userId User ID
     * @param verificationId Verification ID
     * @return Composite key
     */
    private String generateKey(String userId, String verificationId) {
        return "kyc:" + userId + ":" + verificationId;
    }

    /**
     * Find records by jurisdiction
     *
     * @param jurisdiction Regulatory jurisdiction
     * @return List of matching records
     */
    public List<KYCVerificationRecord> findByJurisdiction(String jurisdiction) {
        return findAll().stream()
                .filter(record -> jurisdiction.equals(record.jurisdiction))
                .toList();
    }

    /**
     * Find records by provider
     *
     * @param provider KYC provider name
     * @return List of matching records
     */
    public List<KYCVerificationRecord> findByProvider(String provider) {
        return findAll().stream()
                .filter(record -> provider.equals(record.provider))
                .toList();
    }
}
