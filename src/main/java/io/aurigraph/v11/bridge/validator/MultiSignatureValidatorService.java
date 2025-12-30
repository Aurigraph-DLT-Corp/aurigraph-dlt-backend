package io.aurigraph.v11.bridge.validator;

import io.aurigraph.v11.bridge.BridgeTransactionStatus;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Multi-Signature Validator Service - 4-of-7 Quorum Consensus
 *
 * Manages a network of 7 validator nodes with 4-of-7 quorum multi-signature
 * consensus for bridge transaction approval. Critical for:
 * - Cross-chain transaction validation
 * - Preventing unauthorized bridge transfers
 * - Byzantine fault tolerance (tolerates up to 3 faulty validators)
 *
 * Architecture:
 * - 7 independent validator nodes with digital signatures
 * - 4/7 quorum requirement for transaction approval
 * - Reputation-based validator selection
 * - Automated failover for inactive validators
 *
 * Key Features:
 * - ECDSA-based multi-signature validation
 * - Reputation-based validator scoring
 * - Byzantine fault tolerance (BFT 3/7)
 * - Health monitoring and automatic recovery
 * - Comprehensive audit trail of all validations
 *
 * @author IBA + BDA
 * @version 11.1.0
 * @since Sprint 14
 */
@ApplicationScoped
public class MultiSignatureValidatorService {

    private final Map<String, BridgeValidatorNode> validators = new ConcurrentHashMap<>();
    private static final int TOTAL_VALIDATORS = 7;
    private static final int QUORUM_REQUIRED = 4;
    private static final long HEARTBEAT_TIMEOUT_MS = 5 * 60 * 1000; // 5 minutes

    @Inject
    io.aurigraph.v11.bridge.persistence.BridgeTransactionRepository bridgeRepository;

    /**
     * Initialize the validator network with 7 nodes
     * Called once during application startup
     *
     * @throws Exception If validator initialization fails
     */
    public void initializeValidators() throws Exception {
        Log.infof("Initializing %d-node validator network with %d/%d quorum...",
                TOTAL_VALIDATORS, QUORUM_REQUIRED, TOTAL_VALIDATORS);

        for (int i = 1; i <= TOTAL_VALIDATORS; i++) {
            String validatorId = String.format("validator-%d", i);
            String validatorName = String.format("Validator Node %d", i);
            BridgeValidatorNode node = new BridgeValidatorNode(validatorId, validatorName);
            validators.put(validatorId, node);
            Log.infof("✓ Initialized %s (%s)", validatorId, validatorName);
        }

        Log.infof("Validator network ready: %d/%d nodes active, %d/%d quorum required",
                getActiveValidators().size(), TOTAL_VALIDATORS, QUORUM_REQUIRED, TOTAL_VALIDATORS);
    }

    /**
     * Request multi-signature validation for a bridge transaction
     *
     * @param transactionId The transaction to validate
     * @param transactionData The transaction data to sign
     * @return Validation result with signatures and approval status
     */
    public MultiSignatureValidationResult validateTransaction(String transactionId, String transactionData) {
        List<BridgeValidatorNode> activeValidators = getActiveValidators();

        if (activeValidators.size() < QUORUM_REQUIRED) {
            Log.warnf("Insufficient active validators: %d/%d required for quorum",
                    activeValidators.size(), QUORUM_REQUIRED);
            return new MultiSignatureValidationResult(
                    transactionId,
                    false,
                    Collections.emptyList(),
                    "Insufficient active validators for quorum"
            );
        }

        // Select top validators by reputation for signing
        List<BridgeValidatorNode> selectedValidators = activeValidators.stream()
                .sorted((a, b) -> Double.compare(b.getReputationScore(), a.getReputationScore()))
                .limit(QUORUM_REQUIRED)
                .collect(Collectors.toList());

        List<ValidatorSignature> signatures = new ArrayList<>();
        int validSignatures = 0;

        // Request signatures from selected validators
        for (BridgeValidatorNode validator : selectedValidators) {
            try {
                String signature = validator.signTransaction(transactionData);
                signatures.add(new ValidatorSignature(
                        validator.getValidatorId(),
                        validator.getValidatorName(),
                        signature,
                        validator.getReputationScore()
                ));
                validSignatures++;
                Log.debugf("✓ Signature collected from %s (reputation: %.1f)",
                        validator.getValidatorId(), validator.getReputationScore());
            } catch (Exception e) {
                Log.warnf("Failed to get signature from %s: %s",
                        validator.getValidatorId(), e.getMessage());
            }
        }

        // Validate quorum reached
        boolean quorumReached = validSignatures >= QUORUM_REQUIRED;

        if (quorumReached) {
            Log.infof("✓ Quorum reached: %d/%d signatures collected for transaction %s",
                    validSignatures, QUORUM_REQUIRED, transactionId);
        } else {
            Log.warnf("✗ Quorum not reached: %d/%d signatures for transaction %s",
                    validSignatures, QUORUM_REQUIRED, transactionId);
        }

        return new MultiSignatureValidationResult(transactionId, quorumReached, signatures, null);
    }

    /**
     * Verify signatures for a transaction
     * Checks that all provided signatures are valid and from valid validators
     *
     * @param transactionId The transaction ID
     * @param transactionData The transaction data that was signed
     * @param signatures List of signatures to verify
     * @return true if all signatures are valid and quorum is met
     */
    public boolean verifyMultiSignature(String transactionId, String transactionData,
                                        List<ValidatorSignature> signatures) {
        if (signatures == null || signatures.isEmpty()) {
            Log.warnf("No signatures provided for transaction %s", transactionId);
            return false;
        }

        if (signatures.size() < QUORUM_REQUIRED) {
            Log.warnf("Insufficient signatures for transaction %s: %d/%d required",
                    transactionId, signatures.size(), QUORUM_REQUIRED);
            return false;
        }

        int validSignatures = 0;

        for (ValidatorSignature sig : signatures) {
            BridgeValidatorNode validator = validators.get(sig.validatorId);
            if (validator == null) {
                Log.warnf("Unknown validator %s for transaction %s",
                        sig.validatorId, transactionId);
                continue;
            }

            if (!validator.isActive()) {
                Log.warnf("Validator %s is inactive for transaction %s",
                        sig.validatorId, transactionId);
                continue;
            }

            boolean isValid = validator.verifySignature(transactionData, sig.signature, validator.getPublicKey());
            if (isValid) {
                validSignatures++;
                Log.debugf("✓ Signature verified from %s", sig.validatorId);
            } else {
                Log.warnf("✗ Invalid signature from %s", sig.validatorId);
            }
        }

        boolean quorumMet = validSignatures >= QUORUM_REQUIRED;
        if (quorumMet) {
            Log.infof("✓ Multi-signature verification passed: %d/%d valid signatures",
                    validSignatures, QUORUM_REQUIRED);
        }

        return quorumMet;
    }

    /**
     * Get list of active validators
     * A validator is active if it has responsive and has a reputation > 0
     *
     * @return List of active validator nodes
     */
    public List<BridgeValidatorNode> getActiveValidators() {
        return validators.values().stream()
                .filter(BridgeValidatorNode::isActive)
                .filter(BridgeValidatorNode::isResponsive)
                .filter(v -> v.getReputationScore() > 0)
                .collect(Collectors.toList());
    }

    /**
     * Get validator by ID
     *
     * @param validatorId The validator ID
     * @return Optional containing the validator or empty if not found
     */
    public Optional<BridgeValidatorNode> getValidator(String validatorId) {
        return Optional.ofNullable(validators.get(validatorId));
    }

    /**
     * Send heartbeat from a validator to indicate it's alive
     *
     * @param validatorId The validator ID
     * @return true if heartbeat was processed, false if validator not found
     */
    public boolean receiveHeartbeat(String validatorId) {
        BridgeValidatorNode validator = validators.get(validatorId);
        if (validator == null) {
            return false;
        }

        validator.sendHeartbeat();
        return true;
    }

    /**
     * Get validator network statistics
     *
     * @return Network statistics including active count, quorum status, etc.
     */
    public ValidatorNetworkStats getNetworkStats() {
        List<BridgeValidatorNode> activeValidators = getActiveValidators();
        double avgReputation = validators.values().stream()
                .mapToDouble(BridgeValidatorNode::getReputationScore)
                .average()
                .orElse(0.0);

        return new ValidatorNetworkStats(
                validators.size(),
                activeValidators.size(),
                QUORUM_REQUIRED,
                activeValidators.size() >= QUORUM_REQUIRED,
                avgReputation
        );
    }

    /**
     * Health check for the validator network
     * Monitors validator health and performs automatic failover if needed
     */
    public void performHealthCheck() {
        Log.debugf("Performing validator network health check...");

        long now = System.currentTimeMillis();
        int inactiveCount = 0;

        for (BridgeValidatorNode validator : validators.values()) {
            long inactiveMs = now - validator.getLastHeartbeat();

            if (inactiveMs > HEARTBEAT_TIMEOUT_MS && validator.isActive()) {
                Log.warnf("Validator %s is inactive (no heartbeat for %d minutes)",
                        validator.getValidatorId(), inactiveMs / 60000);
                validator.setActive(false);
                inactiveCount++;
            }
        }

        ValidatorNetworkStats stats = getNetworkStats();
        if (!stats.quorumAvailable) {
            Log.errorf("⚠️  CRITICAL: Validator network unable to meet quorum! " +
                    "Active: %d/%d, Required: %d",
                    stats.activeValidators, stats.totalValidators, stats.quorumRequired);
        }
    }

    /**
     * Get detailed validator status report
     *
     * @return List of all validators with their current status
     */
    public List<ValidatorStatus> getValidatorStatusReport() {
        return validators.values().stream()
                .map(v -> new ValidatorStatus(
                        v.getValidatorId(),
                        v.getValidatorName(),
                        v.isActive(),
                        v.isResponsive(),
                        v.getReputationScore(),
                        v.getSuccessfulSignatures(),
                        v.getFailedSignatures()
                ))
                .collect(Collectors.toList());
    }

    /**
     * Result of a multi-signature validation request
     */
    public static class MultiSignatureValidationResult {
        public final String transactionId;
        public final boolean approved;
        public final List<ValidatorSignature> signatures;
        public final String errorMessage;

        public MultiSignatureValidationResult(String transactionId, boolean approved,
                                             List<ValidatorSignature> signatures,
                                             String errorMessage) {
            this.transactionId = transactionId;
            this.approved = approved;
            this.signatures = signatures;
            this.errorMessage = errorMessage;
        }

        @Override
        public String toString() {
            return String.format("ValidationResult[tx=%s, approved=%s, signatures=%d, error=%s]",
                    transactionId, approved, signatures.size(), errorMessage);
        }
    }

    /**
     * Individual validator signature
     */
    public static class ValidatorSignature {
        public final String validatorId;
        public final String validatorName;
        public final String signature;
        public final double reputationScore;

        public ValidatorSignature(String validatorId, String validatorName,
                                 String signature, double reputationScore) {
            this.validatorId = validatorId;
            this.validatorName = validatorName;
            this.signature = signature;
            this.reputationScore = reputationScore;
        }
    }

    /**
     * Validator network statistics
     */
    public static class ValidatorNetworkStats {
        public final int totalValidators;
        public final int activeValidators;
        public final int quorumRequired;
        public final boolean quorumAvailable;
        public final double averageReputation;

        public ValidatorNetworkStats(int totalValidators, int activeValidators,
                                    int quorumRequired, boolean quorumAvailable,
                                    double averageReputation) {
            this.totalValidators = totalValidators;
            this.activeValidators = activeValidators;
            this.quorumRequired = quorumRequired;
            this.quorumAvailable = quorumAvailable;
            this.averageReputation = averageReputation;
        }

        @Override
        public String toString() {
            return String.format("ValidatorStats[active=%d/%d, quorum=%s, avgReputation=%.1f]",
                    activeValidators, totalValidators,
                    quorumAvailable ? "✓" : "✗", averageReputation);
        }
    }

    /**
     * Individual validator status
     */
    public static class ValidatorStatus {
        public final String validatorId;
        public final String validatorName;
        public final boolean active;
        public final boolean responsive;
        public final double reputationScore;
        public final int successfulSignatures;
        public final int failedSignatures;

        public ValidatorStatus(String validatorId, String validatorName, boolean active,
                              boolean responsive, double reputationScore,
                              int successfulSignatures, int failedSignatures) {
            this.validatorId = validatorId;
            this.validatorName = validatorName;
            this.active = active;
            this.responsive = responsive;
            this.reputationScore = reputationScore;
            this.successfulSignatures = successfulSignatures;
            this.failedSignatures = failedSignatures;
        }

        @Override
        public String toString() {
            return String.format("ValidatorStatus[%s, active=%s, responsive=%s, reputation=%.1f]",
                    validatorId, active, responsive, reputationScore);
        }
    }
}
