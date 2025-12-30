package io.aurigraph.v11.compliance.oracle;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import io.quarkus.logging.Log;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * OFAC Sanctions Oracle Service
 * Integrates with external OFAC/sanctions list providers
 * Checks identities and countries against sanctions lists
 */
@ApplicationScoped
public class OFACSanctionsOracle {

    // Cached sanctioned entities (address -> SanctionRecord)
    private final Map<String, SanctionRecord> sanctionedAddresses = new ConcurrentHashMap<>();

    // Cached sanctioned countries
    private final Set<String> restrictedCountries = ConcurrentHashMap.newKeySet();

    // Sanctioned individuals database
    private final Map<String, IndividualSanction> sanctionedIndividuals = new ConcurrentHashMap<>();

    // Oracle statistics
    private volatile long lastUpdateTime = Instant.now().getEpochSecond();
    private volatile long cacheHitCount = 0;
    private volatile long cacheMissCount = 0;

    /**
     * Check if address is sanctioned
     */
    public Uni<SanctionCheckResult> checkAddressSanction(String address) {
        return Uni.createFrom().item(() -> {
            SanctionRecord record = sanctionedAddresses.get(address);

            if (record != null) {
                cacheHitCount++;
                return new SanctionCheckResult(
                    address,
                    true,
                    "Address sanctioned: " + record.getReason(),
                    record.getListName(),
                    record.getAddedDate()
                );
            }

            cacheMissCount++;
            return new SanctionCheckResult(
                address,
                false,
                "Address not found on sanctions list",
                null,
                null
            );
        });
    }

    /**
     * Check if country is on sanctions list
     */
    public Uni<SanctionCheckResult> checkCountrySanction(String countryCode) {
        return Uni.createFrom().item(() -> {
            if (restrictedCountries.contains(countryCode)) {
                cacheHitCount++;
                return new SanctionCheckResult(
                    countryCode,
                    true,
                    "Country under sanctions",
                    "OFAC_COUNTRY_LIST",
                    Instant.now()
                );
            }

            cacheMissCount++;
            return new SanctionCheckResult(
                countryCode,
                false,
                "Country not sanctioned",
                null,
                null
            );
        });
    }

    /**
     * Check if individual name/document is sanctioned
     */
    public Uni<SanctionCheckResult> checkIndividualSanction(String firstName,
                                                           String lastName,
                                                           String documentNumber) {
        return Uni.createFrom().item(() -> {
            // Fuzzy match on name
            String fullName = firstName + " " + lastName;
            IndividualSanction match = sanctionedIndividuals.values().stream()
                .filter(s -> fuzzyMatch(fullName, s.getName()))
                .findFirst()
                .orElse(null);

            if (match != null) {
                cacheHitCount++;
                return new SanctionCheckResult(
                    fullName,
                    true,
                    "Individual sanctioned: " + match.getReason(),
                    match.getListName(),
                    match.getAddedDate()
                );
            }

            cacheMissCount++;
            return new SanctionCheckResult(
                fullName,
                false,
                "Individual not found on sanctions list",
                null,
                null
            );
        });
    }

    /**
     * Update sanctions list from OFAC
     */
    public Uni<OracleUpdateResult> updateSanctionsList() {
        return Uni.createFrom().item(() -> {
            Log.info("Updating OFAC sanctions list from oracle");

            // In production, this would call external OFAC API
            // For now, initialize with known sanctions
            initializeDefaultSanctions();

            long updateTime = Instant.now().getEpochSecond();
            long timeSinceLastUpdate = updateTime - lastUpdateTime;
            lastUpdateTime = updateTime;

            return new OracleUpdateResult(
                true,
                "Sanctions list updated",
                sanctionedAddresses.size(),
                restrictedCountries.size(),
                sanctionedIndividuals.size(),
                timeSinceLastUpdate
            );
        });
    }

    /**
     * Add address to sanctions list
     */
    public void addSanctionedAddress(String address, String reason, String listName) {
        SanctionRecord record = new SanctionRecord(
            address, reason, listName, Instant.now()
        );
        sanctionedAddresses.put(address, record);
        Log.warnf("Address added to sanctions: %s (reason: %s)", address, reason);
    }

    /**
     * Add country to sanctions list
     */
    public void addSanctionedCountry(String countryCode) {
        restrictedCountries.add(countryCode);
        Log.warnf("Country added to sanctions: %s", countryCode);
    }

    /**
     * Remove address from sanctions
     */
    public void removeSanctionedAddress(String address) {
        sanctionedAddresses.remove(address);
        Log.infof("Address removed from sanctions: %s", address);
    }

    /**
     * Get oracle statistics
     */
    public OracleStats getStats() {
        long totalChecks = cacheHitCount + cacheMissCount;
        double hitRate = totalChecks == 0 ? 0 : (cacheHitCount * 100.0) / totalChecks;

        return new OracleStats(
            sanctionedAddresses.size(),
            restrictedCountries.size(),
            sanctionedIndividuals.size(),
            totalChecks,
            cacheHitCount,
            cacheMissCount,
            hitRate,
            lastUpdateTime
        );
    }

    /**
     * Initialize default sanctions for testing
     */
    private void initializeDefaultSanctions() {
        // High-risk countries (OFAC)
        String[] sanctionedCountries = {
            "IR",  // Iran
            "KP",  // North Korea
            "SY",  // Syria
            "CU",  // Cuba
            "VE"   // Venezuela
        };

        for (String country : sanctionedCountries) {
            restrictedCountries.add(country);
        }

        // Sample sanctioned individuals
        sanctionedIndividuals.put(
            "Sanctioned-1",
            new IndividualSanction("John Doe", "Fraud", "OFAC_SDN", Instant.now())
        );

        // Sample sanctioned addresses
        sanctionedAddresses.put(
            "0x1234567890123456789012345678901234567890",
            new SanctionRecord(
                "0x1234567890123456789012345678901234567890",
                "Sanctions evasion",
                "OFAC_BLOCKED",
                Instant.now()
            )
        );
    }

    /**
     * Fuzzy string matching for name matching
     */
    private boolean fuzzyMatch(String s1, String s2) {
        String lower1 = s1.toLowerCase();
        String lower2 = s2.toLowerCase();

        // Simple substring matching (production would use Levenshtein distance)
        return lower1.contains(lower2) || lower2.contains(lower1);
    }

    // Inner classes

    /**
     * Sanction check result
     */
    public static class SanctionCheckResult {
        private final String identifier;
        private final boolean sanctioned;
        private final String message;
        private final String listName;
        private final Instant addedDate;

        public SanctionCheckResult(String identifier, boolean sanctioned, String message,
                                  String listName, Instant addedDate) {
            this.identifier = identifier;
            this.sanctioned = sanctioned;
            this.message = message;
            this.listName = listName;
            this.addedDate = addedDate;
        }

        public String getIdentifier() { return identifier; }
        public boolean isSanctioned() { return sanctioned; }
        public String getMessage() { return message; }
        public String getListName() { return listName; }
        public Instant getAddedDate() { return addedDate; }
    }

    /**
     * Sanction record
     */
    public static class SanctionRecord {
        private final String address;
        private final String reason;
        private final String listName;
        private final Instant addedDate;

        public SanctionRecord(String address, String reason, String listName, Instant addedDate) {
            this.address = address;
            this.reason = reason;
            this.listName = listName;
            this.addedDate = addedDate;
        }

        public String getAddress() { return address; }
        public String getReason() { return reason; }
        public String getListName() { return listName; }
        public Instant getAddedDate() { return addedDate; }
    }

    /**
     * Individual sanction record
     */
    public static class IndividualSanction {
        private final String name;
        private final String reason;
        private final String listName;
        private final Instant addedDate;

        public IndividualSanction(String name, String reason, String listName, Instant addedDate) {
            this.name = name;
            this.reason = reason;
            this.listName = listName;
            this.addedDate = addedDate;
        }

        public String getName() { return name; }
        public String getReason() { return reason; }
        public String getListName() { return listName; }
        public Instant getAddedDate() { return addedDate; }
    }

    /**
     * Oracle update result
     */
    public static class OracleUpdateResult {
        private final boolean success;
        private final String message;
        private final int sanctionedAddressesCount;
        private final int restrictedCountriesCount;
        private final int sanctionedIndividualsCount;
        private final long timeSinceLastUpdate;

        public OracleUpdateResult(boolean success, String message, int sanctionedAddressesCount,
                                 int restrictedCountriesCount, int sanctionedIndividualsCount,
                                 long timeSinceLastUpdate) {
            this.success = success;
            this.message = message;
            this.sanctionedAddressesCount = sanctionedAddressesCount;
            this.restrictedCountriesCount = restrictedCountriesCount;
            this.sanctionedIndividualsCount = sanctionedIndividualsCount;
            this.timeSinceLastUpdate = timeSinceLastUpdate;
        }

        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public int getSanctionedAddressesCount() { return sanctionedAddressesCount; }
        public int getRestrictedCountriesCount() { return restrictedCountriesCount; }
        public int getSanctionedIndividualsCount() { return sanctionedIndividualsCount; }
        public long getTimeSinceLastUpdate() { return timeSinceLastUpdate; }
    }

    /**
     * Oracle statistics
     */
    public static class OracleStats {
        private final int sanctionedAddresses;
        private final int restrictedCountries;
        private final int sanctionedIndividuals;
        private final long totalChecks;
        private final long cacheHits;
        private final long cacheMisses;
        private final double hitRate;
        private final long lastUpdateTime;

        public OracleStats(int sanctionedAddresses, int restrictedCountries,
                          int sanctionedIndividuals, long totalChecks, long cacheHits,
                          long cacheMisses, double hitRate, long lastUpdateTime) {
            this.sanctionedAddresses = sanctionedAddresses;
            this.restrictedCountries = restrictedCountries;
            this.sanctionedIndividuals = sanctionedIndividuals;
            this.totalChecks = totalChecks;
            this.cacheHits = cacheHits;
            this.cacheMisses = cacheMisses;
            this.hitRate = hitRate;
            this.lastUpdateTime = lastUpdateTime;
        }

        public int getSanctionedAddresses() { return sanctionedAddresses; }
        public int getRestrictedCountries() { return restrictedCountries; }
        public int getSanctionedIndividuals() { return sanctionedIndividuals; }
        public long getTotalChecks() { return totalChecks; }
        public long getCacheHits() { return cacheHits; }
        public long getCacheMisses() { return cacheMisses; }
        public double getHitRate() { return hitRate; }
        public long getLastUpdateTime() { return lastUpdateTime; }
    }
}
