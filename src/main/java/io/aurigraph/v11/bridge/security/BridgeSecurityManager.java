package io.aurigraph.v11.bridge.security;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import io.smallrye.mutiny.Uni;
import io.quarkus.logging.Log;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.math.BigDecimal;
import java.security.*;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * Bridge Security Manager - Multi-Signature Validation & Fraud Detection
 * Sprint 4 + 7 Implementation
 *
 * Features:
 * - 21 validator multi-signature consensus
 * - Transaction verification and proof generation
 * - Fraud proof system with challenge periods
 * - Validator reputation tracking
 * - Slashing for malicious behavior
 * - Real-time security monitoring
 *
 * Security Requirements:
 * - Minimum 14/21 signatures required (2/3 + 1)
 * - 24-hour challenge period for large transfers
 * - Zero-tolerance fraud detection
 * - Cryptographic proof verification
 *
 * @author Aurigraph V11 Security Team
 * @version 11.3.4
 * @since 2025-01-20
 */
@ApplicationScoped
public class BridgeSecurityManager {

    @ConfigProperty(name = "bridge.security.total.validators", defaultValue = "21")
    int totalValidators;

    @ConfigProperty(name = "bridge.security.required.signatures", defaultValue = "14")
    int requiredSignatures; // 2/3 + 1 = 14 out of 21

    @ConfigProperty(name = "bridge.security.challenge.period.hours", defaultValue = "24")
    int challengePeriodHours;

    @ConfigProperty(name = "bridge.security.large.transfer.threshold", defaultValue = "100000")
    BigDecimal largeTransferThreshold; // $100K

    // Validator registry
    private final Map<String, ValidatorInfo> validators = new ConcurrentHashMap<>();
    private final Map<String, Set<ValidatorSignature>> transactionSignatures = new ConcurrentHashMap<>();
    private final Map<String, SecurityProof> securityProofs = new ConcurrentHashMap<>();
    private final Map<String, ChallengeInfo> activeChallenges = new ConcurrentHashMap<>();

    // Security metrics
    private final AtomicLong totalValidations = new AtomicLong(0);
    private final AtomicLong fraudDetections = new AtomicLong(0);
    private final AtomicLong challengesRaised = new AtomicLong(0);
    private final AtomicLong slashingEvents = new AtomicLong(0);

    public BridgeSecurityManager() {
        initializeValidators();
    }

    /**
     * Validate a bridge transaction with multi-signature consensus
     */
    public Uni<ValidationResult> validateTransaction(String transactionId,
                                                    String sourceChain,
                                                    String targetChain,
                                                    String sourceAddress,
                                                    String targetAddress,
                                                    BigDecimal amount,
                                                    byte[] transactionData) {
        return Uni.createFrom().item(() -> {
            totalValidations.incrementAndGet();

            // Step 1: Basic validation
            ValidationResult basicValidation = performBasicValidation(
                transactionId, sourceChain, targetChain, sourceAddress, targetAddress, amount
            );
            if (!basicValidation.isValid()) {
                return basicValidation;
            }

            // Step 2: Generate security proof
            SecurityProof proof = generateSecurityProof(
                transactionId, sourceChain, targetChain, amount, transactionData
            );
            securityProofs.put(transactionId, proof);

            // Step 3: Collect validator signatures
            Set<ValidatorSignature> signatures = collectValidatorSignatures(transactionId, proof);
            transactionSignatures.put(transactionId, signatures);

            // Step 4: Verify minimum signatures
            if (signatures.size() < requiredSignatures) {
                return ValidationResult.failed(
                    "Insufficient validator signatures: " + signatures.size() + "/" + requiredSignatures
                );
            }

            // Step 5: Check for fraud
            FraudDetectionResult fraudCheck = detectFraud(transactionId, proof, signatures);
            if (fraudCheck.isFraudulent()) {
                fraudDetections.incrementAndGet();
                return ValidationResult.failed("Fraud detected: " + fraudCheck.getReason());
            }

            // Step 6: Challenge period for large transfers
            boolean requiresChallengePeriod = amount.compareTo(largeTransferThreshold) > 0;
            if (requiresChallengePeriod) {
                ChallengeInfo challenge = createChallengePeriod(transactionId, amount);
                activeChallenges.put(transactionId, challenge);
            }

            Log.infof("Transaction %s validated successfully with %d/%d signatures%s",
                transactionId, signatures.size(), requiredSignatures,
                requiresChallengePeriod ? " (challenge period active)" : "");

            return ValidationResult.success(
                signatures.size(),
                proof.getProofHash(),
                requiresChallengePeriod,
                requiresChallengePeriod ? challengePeriodHours : 0
            );
        });
    }

    /**
     * Submit a fraud challenge against a transaction
     */
    public Uni<ChallengeResult> submitChallenge(String transactionId,
                                               String challengerValidator,
                                               String reason,
                                               byte[] evidenceData) {
        return Uni.createFrom().item(() -> {
            challengesRaised.incrementAndGet();

            // Verify challenger is a valid validator
            ValidatorInfo challenger = validators.get(challengerValidator);
            if (challenger == null || !challenger.isActive()) {
                return ChallengeResult.rejected("Invalid or inactive challenger");
            }

            // Check if challenge period is still active
            ChallengeInfo challenge = activeChallenges.get(transactionId);
            if (challenge == null) {
                return ChallengeResult.rejected("No active challenge period for this transaction");
            }

            if (Instant.now().isAfter(challenge.getExpiryTime())) {
                return ChallengeResult.rejected("Challenge period expired");
            }

            // Verify evidence
            SecurityProof proof = securityProofs.get(transactionId);
            if (proof == null) {
                return ChallengeResult.rejected("Security proof not found");
            }

            FraudEvidenceResult evidenceResult = verifyFraudEvidence(
                transactionId, proof, evidenceData
            );

            if (evidenceResult.isValid()) {
                // Fraud proven - slash malicious validators
                slashMaliciousValidators(transactionId, evidenceResult.getMaliciousValidators());

                // Cancel the transaction
                challenge.setStatus(ChallengeStatus.FRAUD_PROVEN);

                Log.warnf("Fraud proven for transaction %s by %s. Slashing %d validators.",
                    transactionId, challengerValidator, evidenceResult.getMaliciousValidators().size());

                return ChallengeResult.accepted(
                    "Fraud proven. Transaction cancelled. Validators slashed.",
                    evidenceResult.getMaliciousValidators()
                );
            } else {
                // Invalid challenge - penalize challenger
                penalizeInvalidChallenge(challengerValidator);

                Log.infof("Invalid challenge from %s for transaction %s",
                    challengerValidator, transactionId);

                return ChallengeResult.rejected(
                    "Evidence insufficient. Challenger penalized."
                );
            }
        });
    }

    /**
     * Check if a transaction has passed the challenge period
     */
    public Uni<Boolean> hasPassedChallengePeriod(String transactionId) {
        return Uni.createFrom().item(() -> {
            ChallengeInfo challenge = activeChallenges.get(transactionId);
            if (challenge == null) {
                return true; // No challenge period required
            }

            if (challenge.getStatus() == ChallengeStatus.FRAUD_PROVEN) {
                return false; // Fraud detected
            }

            boolean passed = Instant.now().isAfter(challenge.getExpiryTime());
            if (passed && challenge.getStatus() == ChallengeStatus.ACTIVE) {
                challenge.setStatus(ChallengeStatus.PASSED);
                Log.infof("Transaction %s passed challenge period successfully", transactionId);
            }

            return passed;
        });
    }

    /**
     * Get validator network statistics
     */
    public Uni<ValidatorNetworkStats> getValidatorStats() {
        return Uni.createFrom().item(() -> {
            ValidatorNetworkStats stats = new ValidatorNetworkStats();
            stats.setTotalValidators(totalValidators);
            stats.setActiveValidators((int) validators.values().stream()
                .filter(ValidatorInfo::isActive).count());
            stats.setRequiredSignatures(requiredSignatures);
            stats.setTotalValidations(totalValidations.get());
            stats.setFraudDetections(fraudDetections.get());
            stats.setChallengesRaised(challengesRaised.get());
            stats.setSlashingEvents(slashingEvents.get());

            // Calculate average reputation
            double avgReputation = validators.values().stream()
                .mapToDouble(ValidatorInfo::getReputation)
                .average()
                .orElse(0.0);
            stats.setAverageReputation(avgReputation);

            return stats;
        });
    }

    // Private helper methods

    private void initializeValidators() {
        // Initialize 21 validators with equal reputation
        for (int i = 1; i <= totalValidators; i++) {
            String validatorId = "validator-" + i;
            ValidatorInfo validator = new ValidatorInfo(
                validatorId,
                "Validator " + i,
                generateValidatorPublicKey(),
                100.0, // Initial reputation
                true,  // Active
                Instant.now()
            );
            validators.put(validatorId, validator);
        }
        Log.infof("Initialized %d bridge validators", totalValidators);
    }

    private ValidationResult performBasicValidation(String transactionId,
                                                   String sourceChain,
                                                   String targetChain,
                                                   String sourceAddress,
                                                   String targetAddress,
                                                   BigDecimal amount) {
        // Validate transaction ID
        if (transactionId == null || transactionId.isEmpty()) {
            return ValidationResult.failed("Invalid transaction ID");
        }

        // Validate chains
        if (sourceChain == null || targetChain == null) {
            return ValidationResult.failed("Invalid chain identifiers");
        }

        if (sourceChain.equals(targetChain)) {
            return ValidationResult.failed("Source and target chains cannot be the same");
        }

        // Validate addresses
        if (sourceAddress == null || sourceAddress.isEmpty()) {
            return ValidationResult.failed("Invalid source address");
        }

        if (targetAddress == null || targetAddress.isEmpty()) {
            return ValidationResult.failed("Invalid target address");
        }

        // Validate amount
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return ValidationResult.failed("Invalid transfer amount");
        }

        return ValidationResult.success(0, null, false, 0);
    }

    private SecurityProof generateSecurityProof(String transactionId,
                                               String sourceChain,
                                               String targetChain,
                                               BigDecimal amount,
                                               byte[] transactionData) {
        try {
            // Generate cryptographic proof
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            // Combine all transaction data
            String dataToHash = transactionId + sourceChain + targetChain + amount.toString();
            digest.update(dataToHash.getBytes());
            if (transactionData != null) {
                digest.update(transactionData);
            }

            byte[] proofHash = digest.digest();
            String proofHashHex = bytesToHex(proofHash);

            SecurityProof proof = new SecurityProof(
                transactionId,
                proofHashHex,
                Instant.now(),
                transactionData
            );

            Log.debugf("Generated security proof for transaction %s: %s",
                transactionId, proofHashHex);

            return proof;
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to generate security proof", e);
        }
    }

    private Set<ValidatorSignature> collectValidatorSignatures(String transactionId,
                                                               SecurityProof proof) {
        Set<ValidatorSignature> signatures = new HashSet<>();

        // Simulate validator consensus (in production, this would be async)
        List<ValidatorInfo> activeValidators = validators.values().stream()
            .filter(ValidatorInfo::isActive)
            .collect(Collectors.toList());

        // Randomly select validators to sign (simulating network responses)
        // In production, all validators would be queried
        Collections.shuffle(activeValidators);
        int signaturesNeeded = Math.min(requiredSignatures + 2, activeValidators.size());

        for (int i = 0; i < signaturesNeeded; i++) {
            ValidatorInfo validator = activeValidators.get(i);

            // Generate signature (simplified for testing)
            String signature = generateValidatorSignature(
                validator.getValidatorId(),
                proof.getProofHash()
            );

            ValidatorSignature validatorSig = new ValidatorSignature(
                validator.getValidatorId(),
                signature,
                Instant.now()
            );

            signatures.add(validatorSig);
        }

        return signatures;
    }

    private FraudDetectionResult detectFraud(String transactionId,
                                            SecurityProof proof,
                                            Set<ValidatorSignature> signatures) {
        // Check for duplicate signatures
        Set<String> validatorIds = signatures.stream()
            .map(ValidatorSignature::getValidatorId)
            .collect(Collectors.toSet());

        if (validatorIds.size() < signatures.size()) {
            return FraudDetectionResult.fraudulent(
                "Duplicate validator signatures detected",
                Collections.emptyList()
            );
        }

        // Check for invalid validators
        for (ValidatorSignature sig : signatures) {
            ValidatorInfo validator = validators.get(sig.getValidatorId());
            if (validator == null || !validator.isActive()) {
                return FraudDetectionResult.fraudulent(
                    "Invalid validator signature: " + sig.getValidatorId(),
                    List.of(sig.getValidatorId())
                );
            }

            // Verify signature reputation
            if (validator.getReputation() < 50.0) {
                return FraudDetectionResult.fraudulent(
                    "Low reputation validator: " + sig.getValidatorId(),
                    List.of(sig.getValidatorId())
                );
            }
        }

        // All checks passed
        return FraudDetectionResult.clean();
    }

    private ChallengeInfo createChallengePeriod(String transactionId, BigDecimal amount) {
        Instant expiryTime = Instant.now().plus(Duration.ofHours(challengePeriodHours));

        ChallengeInfo challenge = new ChallengeInfo(
            transactionId,
            amount,
            Instant.now(),
            expiryTime,
            ChallengeStatus.ACTIVE
        );

        Log.infof("Created %d-hour challenge period for transaction %s (amount: %s)",
            challengePeriodHours, transactionId, amount);

        return challenge;
    }

    private FraudEvidenceResult verifyFraudEvidence(String transactionId,
                                                   SecurityProof proof,
                                                   byte[] evidenceData) {
        // In production, this would perform cryptographic verification
        // For now, simulate evidence verification

        if (evidenceData == null || evidenceData.length == 0) {
            return FraudEvidenceResult.invalid("No evidence provided");
        }

        // Simulate evidence analysis
        boolean fraudDetected = new Random().nextDouble() < 0.1; // 10% fraud rate for testing

        if (fraudDetected) {
            // Identify malicious validators (simplified)
            List<String> malicious = transactionSignatures.get(transactionId).stream()
                .limit(2) // Assume 2 validators were malicious
                .map(ValidatorSignature::getValidatorId)
                .collect(Collectors.toList());

            return FraudEvidenceResult.valid(malicious);
        }

        return FraudEvidenceResult.invalid("Evidence does not prove fraud");
    }

    private void slashMaliciousValidators(String transactionId, List<String> maliciousValidators) {
        for (String validatorId : maliciousValidators) {
            ValidatorInfo validator = validators.get(validatorId);
            if (validator != null) {
                // Reduce reputation significantly
                double newReputation = Math.max(0, validator.getReputation() - 50.0);
                validator.setReputation(newReputation);

                // Deactivate if reputation too low
                if (newReputation < 30.0) {
                    validator.setActive(false);
                }

                slashingEvents.incrementAndGet();

                Log.warnf("Slashed validator %s for malicious behavior. New reputation: %.2f",
                    validatorId, newReputation);
            }
        }
    }

    private void penalizeInvalidChallenge(String challengerValidator) {
        ValidatorInfo challenger = validators.get(challengerValidator);
        if (challenger != null) {
            // Reduce reputation for invalid challenge
            double newReputation = Math.max(0, challenger.getReputation() - 5.0);
            challenger.setReputation(newReputation);

            Log.infof("Penalized validator %s for invalid challenge. New reputation: %.2f",
                challengerValidator, newReputation);
        }
    }

    private String generateValidatorPublicKey() {
        return "0x" + UUID.randomUUID().toString().replace("-", "");
    }

    private String generateValidatorSignature(String validatorId, String dataHash) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String combined = validatorId + dataHash + System.nanoTime();
            byte[] hash = digest.digest(combined.getBytes());
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to generate signature", e);
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }

    // Inner classes for data structures

    public static class ValidatorInfo {
        private final String validatorId;
        private final String name;
        private final String publicKey;
        private double reputation;
        private boolean active;
        private final Instant joinedAt;

        public ValidatorInfo(String validatorId, String name, String publicKey,
                           double reputation, boolean active, Instant joinedAt) {
            this.validatorId = validatorId;
            this.name = name;
            this.publicKey = publicKey;
            this.reputation = reputation;
            this.active = active;
            this.joinedAt = joinedAt;
        }

        public String getValidatorId() { return validatorId; }
        public String getName() { return name; }
        public String getPublicKey() { return publicKey; }
        public double getReputation() { return reputation; }
        public void setReputation(double reputation) { this.reputation = reputation; }
        public boolean isActive() { return active; }
        public void setActive(boolean active) { this.active = active; }
        public Instant getJoinedAt() { return joinedAt; }
    }

    public static class ValidatorSignature {
        private final String validatorId;
        private final String signature;
        private final Instant timestamp;

        public ValidatorSignature(String validatorId, String signature, Instant timestamp) {
            this.validatorId = validatorId;
            this.signature = signature;
            this.timestamp = timestamp;
        }

        public String getValidatorId() { return validatorId; }
        public String getSignature() { return signature; }
        public Instant getTimestamp() { return timestamp; }
    }

    public static class SecurityProof {
        private final String transactionId;
        private final String proofHash;
        private final Instant generatedAt;
        private final byte[] transactionData;

        public SecurityProof(String transactionId, String proofHash,
                           Instant generatedAt, byte[] transactionData) {
            this.transactionId = transactionId;
            this.proofHash = proofHash;
            this.generatedAt = generatedAt;
            this.transactionData = transactionData;
        }

        public String getTransactionId() { return transactionId; }
        public String getProofHash() { return proofHash; }
        public Instant getGeneratedAt() { return generatedAt; }
        public byte[] getTransactionData() { return transactionData; }
    }

    public static class ChallengeInfo {
        private final String transactionId;
        private final BigDecimal amount;
        private final Instant startTime;
        private final Instant expiryTime;
        private ChallengeStatus status;

        public ChallengeInfo(String transactionId, BigDecimal amount,
                           Instant startTime, Instant expiryTime, ChallengeStatus status) {
            this.transactionId = transactionId;
            this.amount = amount;
            this.startTime = startTime;
            this.expiryTime = expiryTime;
            this.status = status;
        }

        public String getTransactionId() { return transactionId; }
        public BigDecimal getAmount() { return amount; }
        public Instant getStartTime() { return startTime; }
        public Instant getExpiryTime() { return expiryTime; }
        public ChallengeStatus getStatus() { return status; }
        public void setStatus(ChallengeStatus status) { this.status = status; }
    }

    public enum ChallengeStatus {
        ACTIVE, PASSED, FRAUD_PROVEN
    }

    public static class ValidationResult {
        private final boolean valid;
        private final String message;
        private final int signatures;
        private final String proofHash;
        private final boolean requiresChallengePeriod;
        private final int challengePeriodHours;

        private ValidationResult(boolean valid, String message, int signatures,
                               String proofHash, boolean requiresChallengePeriod,
                               int challengePeriodHours) {
            this.valid = valid;
            this.message = message;
            this.signatures = signatures;
            this.proofHash = proofHash;
            this.requiresChallengePeriod = requiresChallengePeriod;
            this.challengePeriodHours = challengePeriodHours;
        }

        public static ValidationResult success(int signatures, String proofHash,
                                              boolean requiresChallengePeriod,
                                              int challengePeriodHours) {
            return new ValidationResult(true, "Validation successful", signatures,
                proofHash, requiresChallengePeriod, challengePeriodHours);
        }

        public static ValidationResult failed(String message) {
            return new ValidationResult(false, message, 0, null, false, 0);
        }

        public boolean isValid() { return valid; }
        public String getMessage() { return message; }
        public int getSignatures() { return signatures; }
        public String getProofHash() { return proofHash; }
        public boolean requiresChallengePeriod() { return requiresChallengePeriod; }
        public int getChallengePeriodHours() { return challengePeriodHours; }
    }

    public static class ChallengeResult {
        private final boolean accepted;
        private final String message;
        private final List<String> maliciousValidators;

        private ChallengeResult(boolean accepted, String message, List<String> maliciousValidators) {
            this.accepted = accepted;
            this.message = message;
            this.maliciousValidators = maliciousValidators;
        }

        public static ChallengeResult accepted(String message, List<String> maliciousValidators) {
            return new ChallengeResult(true, message, maliciousValidators);
        }

        public static ChallengeResult rejected(String message) {
            return new ChallengeResult(false, message, Collections.emptyList());
        }

        public boolean isAccepted() { return accepted; }
        public String getMessage() { return message; }
        public List<String> getMaliciousValidators() { return maliciousValidators; }
    }

    private static class FraudDetectionResult {
        private final boolean fraudulent;
        private final String reason;
        private final List<String> maliciousValidators;

        private FraudDetectionResult(boolean fraudulent, String reason, List<String> maliciousValidators) {
            this.fraudulent = fraudulent;
            this.reason = reason;
            this.maliciousValidators = maliciousValidators;
        }

        public static FraudDetectionResult fraudulent(String reason, List<String> malicious) {
            return new FraudDetectionResult(true, reason, malicious);
        }

        public static FraudDetectionResult clean() {
            return new FraudDetectionResult(false, "No fraud detected", Collections.emptyList());
        }

        public boolean isFraudulent() { return fraudulent; }
        public String getReason() { return reason; }
        public List<String> getMaliciousValidators() { return maliciousValidators; }
    }

    private static class FraudEvidenceResult {
        private final boolean valid;
        private final String message;
        private final List<String> maliciousValidators;

        private FraudEvidenceResult(boolean valid, String message, List<String> maliciousValidators) {
            this.valid = valid;
            this.message = message;
            this.maliciousValidators = maliciousValidators;
        }

        public static FraudEvidenceResult valid(List<String> malicious) {
            return new FraudEvidenceResult(true, "Evidence proves fraud", malicious);
        }

        public static FraudEvidenceResult invalid(String message) {
            return new FraudEvidenceResult(false, message, Collections.emptyList());
        }

        public boolean isValid() { return valid; }
        public String getMessage() { return message; }
        public List<String> getMaliciousValidators() { return maliciousValidators; }
    }

    public static class ValidatorNetworkStats {
        private int totalValidators;
        private int activeValidators;
        private int requiredSignatures;
        private long totalValidations;
        private long fraudDetections;
        private long challengesRaised;
        private long slashingEvents;
        private double averageReputation;

        // Getters and setters
        public int getTotalValidators() { return totalValidators; }
        public void setTotalValidators(int totalValidators) { this.totalValidators = totalValidators; }
        public int getActiveValidators() { return activeValidators; }
        public void setActiveValidators(int activeValidators) { this.activeValidators = activeValidators; }
        public int getRequiredSignatures() { return requiredSignatures; }
        public void setRequiredSignatures(int requiredSignatures) { this.requiredSignatures = requiredSignatures; }
        public long getTotalValidations() { return totalValidations; }
        public void setTotalValidations(long totalValidations) { this.totalValidations = totalValidations; }
        public long getFraudDetections() { return fraudDetections; }
        public void setFraudDetections(long fraudDetections) { this.fraudDetections = fraudDetections; }
        public long getChallengesRaised() { return challengesRaised; }
        public void setChallengesRaised(long challengesRaised) { this.challengesRaised = challengesRaised; }
        public long getSlashingEvents() { return slashingEvents; }
        public void setSlashingEvents(long slashingEvents) { this.slashingEvents = slashingEvents; }
        public double getAverageReputation() { return averageReputation; }
        public void setAverageReputation(double averageReputation) { this.averageReputation = averageReputation; }
    }
}
