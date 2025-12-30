package io.aurigraph.v11.bridge;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Multi-Signature Validation - Tracks validator signatures for bridge transactions
 */
public class MultiSigValidation {
    private final String validationId;
    private final String transactionId;
    private final int requiredSignatures;
    private final Map<String, ValidatorSignature> signatures;
    private final Instant createdAt;
    private Instant completedAt;
    private boolean isValid;

    public MultiSigValidation(String validationId, String transactionId, int requiredSignatures) {
        this.validationId = validationId;
        this.transactionId = transactionId;
        this.requiredSignatures = requiredSignatures;
        this.signatures = new ConcurrentHashMap<>();
        this.createdAt = Instant.now();
        this.isValid = false;
    }

    public void addSignature(String validatorId, String signature) {
        signatures.put(validatorId, new ValidatorSignature(validatorId, signature, Instant.now()));
        checkThreshold();
    }

    private void checkThreshold() {
        if (signatures.size() >= requiredSignatures && !isValid) {
            isValid = true;
            completedAt = Instant.now();
        }
    }

    public boolean hasReachedThreshold() {
        return signatures.size() >= requiredSignatures;
    }

    public long getValidationTimeMs() {
        if (completedAt != null) {
            return completedAt.toEpochMilli() - createdAt.toEpochMilli();
        }
        return Instant.now().toEpochMilli() - createdAt.toEpochMilli();
    }

    // Getters
    public String getValidationId() { return validationId; }
    public String getTransactionId() { return transactionId; }
    public int getRequiredSignatures() { return requiredSignatures; }
    public Map<String, ValidatorSignature> getSignatures() { return signatures; }
    public int getSignatureCount() { return signatures.size(); }
    public boolean isValid() { return isValid; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getCompletedAt() { return completedAt; }

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
}
