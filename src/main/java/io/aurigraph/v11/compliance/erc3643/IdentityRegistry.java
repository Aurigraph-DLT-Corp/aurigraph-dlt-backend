package io.aurigraph.v11.compliance.erc3643;

import jakarta.enterprise.context.ApplicationScoped;
import io.quarkus.logging.Log;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ERC-3643 Identity Registry - Manages KYC/AML verified identities
 * Stores verified identity credentials for token transfer eligibility
 *
 * Compliance: SEC regulations, AML/KYC requirements
 * Reference: https://eips.ethereum.org/EIPS/eip-3643
 */
@ApplicationScoped
public class IdentityRegistry {

    // Main registry: address -> IdentityRecord
    private final Map<String, IdentityRecord> identityRecords = new ConcurrentHashMap<>();

    // Inverse registry for lookups
    private final Map<String, String> kycDocumentToAddress = new ConcurrentHashMap<>();

    // Revocation list
    private final Set<String> revokedIdentities = ConcurrentHashMap.newKeySet();

    // Country restrictions
    private final Set<String> restrictedCountries = ConcurrentHashMap.newKeySet();

    // Statistics
    private volatile long totalRegistered = 0;
    private volatile long totalRevoked = 0;

    /**
     * Register a verified identity
     */
    public synchronized IdentityRecord registerIdentity(String address, IdentityVerification verification) {
        if (address == null || address.isEmpty()) {
            throw new IllegalArgumentException("Address cannot be null or empty");
        }

        if (identityRecords.containsKey(address)) {
            Log.warnf("Identity already registered for address: %s", address);
            return identityRecords.get(address);
        }

        IdentityRecord record = new IdentityRecord(
            address,
            verification.getKycLevel(),
            verification.getCountry(),
            verification.getDocumentHash(),
            Instant.now(),
            verification.getVerifierName(),
            verification.getDocumentExpiryDate()
        );

        identityRecords.put(address, record);
        kycDocumentToAddress.put(verification.getDocumentHash(), address);
        totalRegistered++;

        Log.infof("Identity registered for address: %s, KYC Level: %s",
            address, verification.getKycLevel());

        return record;
    }

    /**
     * Get identity record for an address
     */
    public IdentityRecord getIdentity(String address) {
        return identityRecords.get(address);
    }

    /**
     * Check if an address has valid KYC
     */
    public boolean isValidIdentity(String address) {
        IdentityRecord record = identityRecords.get(address);
        if (record == null) return false;

        // Check if revoked
        if (revokedIdentities.contains(address)) {
            return false;
        }

        // Check if country is restricted
        if (restrictedCountries.contains(record.getCountry())) {
            return false;
        }

        // Check if identity expired
        if (record.getExpiryDate() != null &&
            record.getExpiryDate().isBefore(Instant.now())) {
            return false;
        }

        return true;
    }

    /**
     * Update identity verification status
     */
    public boolean updateIdentityVerification(String address, IdentityVerification newVerification) {
        IdentityRecord existing = identityRecords.get(address);
        if (existing == null) {
            return false;
        }

        IdentityRecord updated = new IdentityRecord(
            address,
            newVerification.getKycLevel(),
            newVerification.getCountry(),
            newVerification.getDocumentHash(),
            existing.getRegistrationDate(),
            newVerification.getVerifierName(),
            newVerification.getDocumentExpiryDate()
        );

        identityRecords.put(address, updated);
        Log.infof("Identity updated for address: %s", address);
        return true;
    }

    /**
     * Revoke an identity (e.g., due to fraud, sanctions)
     */
    public boolean revokeIdentity(String address, String reason) {
        if (!identityRecords.containsKey(address)) {
            return false;
        }

        revokedIdentities.add(address);
        totalRevoked++;
        Log.warnf("Identity revoked for address: %s. Reason: %s", address, reason);
        return true;
    }

    /**
     * Restore a revoked identity
     */
    public boolean restoreIdentity(String address) {
        if (!revokedIdentities.contains(address)) {
            return false;
        }

        revokedIdentities.remove(address);
        Log.infof("Identity restored for address: %s", address);
        return true;
    }

    /**
     * Add country to restricted list
     */
    public void restrictCountry(String countryCode) {
        restrictedCountries.add(countryCode);
        Log.infof("Country restricted: %s", countryCode);
    }

    /**
     * Remove country from restricted list
     */
    public void unrestrictCountry(String countryCode) {
        restrictedCountries.remove(countryCode);
        Log.infof("Country unrestricted: %s", countryCode);
    }

    /**
     * Check if country is restricted
     */
    public boolean isCountryRestricted(String countryCode) {
        return restrictedCountries.contains(countryCode);
    }

    /**
     * Batch verify multiple identities
     */
    public Map<String, IdentityRecord> batchRegisterIdentities(
        List<String> addresses,
        List<IdentityVerification> verifications) {

        if (addresses.size() != verifications.size()) {
            throw new IllegalArgumentException("Addresses and verifications count mismatch");
        }

        Map<String, IdentityRecord> results = new HashMap<>();
        for (int i = 0; i < addresses.size(); i++) {
            IdentityRecord record = registerIdentity(addresses.get(i), verifications.get(i));
            results.put(addresses.get(i), record);
        }
        return results;
    }

    /**
     * Get all registered identities
     */
    public Collection<IdentityRecord> getAllIdentities() {
        return identityRecords.values();
    }

    /**
     * Get registry statistics
     */
    public RegistryStats getStats() {
        long activeIdentities = identityRecords.values().stream()
            .filter(record -> !revokedIdentities.contains(record.getAddress()))
            .count();

        return new RegistryStats(
            totalRegistered,
            activeIdentities,
            totalRevoked,
            restrictedCountries.size(),
            identityRecords.size()
        );
    }

    /**
     * Clear all data (for testing only)
     */
    public void clear() {
        identityRecords.clear();
        kycDocumentToAddress.clear();
        revokedIdentities.clear();
        restrictedCountries.clear();
        totalRegistered = 0;
        totalRevoked = 0;
    }

    // Inner classes

    /**
     * Stores verified identity information
     */
    public static class IdentityRecord {
        private final String address;
        private final String kycLevel;
        private final String country;
        private final String documentHash;
        private final Instant registrationDate;
        private final String verifierName;
        private final Instant expiryDate;

        public IdentityRecord(String address, String kycLevel, String country,
                             String documentHash, Instant registrationDate,
                             String verifierName, Instant expiryDate) {
            this.address = address;
            this.kycLevel = kycLevel;
            this.country = country;
            this.documentHash = documentHash;
            this.registrationDate = registrationDate;
            this.verifierName = verifierName;
            this.expiryDate = expiryDate;
        }

        public String getAddress() { return address; }
        public String getKycLevel() { return kycLevel; }
        public String getCountry() { return country; }
        public String getDocumentHash() { return documentHash; }
        public Instant getRegistrationDate() { return registrationDate; }
        public String getVerifierName() { return verifierName; }
        public Instant getExpiryDate() { return expiryDate; }
    }

    /**
     * Registry statistics
     */
    public static class RegistryStats {
        private final long totalRegistered;
        private final long activeIdentities;
        private final long revokedIdentities;
        private final int restrictedCountries;
        private final int totalRecords;

        public RegistryStats(long totalRegistered, long activeIdentities,
                            long revokedIdentities, int restrictedCountries,
                            int totalRecords) {
            this.totalRegistered = totalRegistered;
            this.activeIdentities = activeIdentities;
            this.revokedIdentities = revokedIdentities;
            this.restrictedCountries = restrictedCountries;
            this.totalRecords = totalRecords;
        }

        public long getTotalRegistered() { return totalRegistered; }
        public long getActiveIdentities() { return activeIdentities; }
        public long getRevokedIdentities() { return revokedIdentities; }
        public int getRestrictedCountries() { return restrictedCountries; }
        public int getTotalRecords() { return totalRecords; }
    }
}
