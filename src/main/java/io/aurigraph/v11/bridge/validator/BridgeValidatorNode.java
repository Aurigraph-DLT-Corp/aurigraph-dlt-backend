package io.aurigraph.v11.bridge.validator;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.util.Base64;

/**
 * Bridge Validator Node - Individual validator in the 7-node network
 *
 * Each validator node:
 * - Maintains a keypair for digital signatures
 * - Signs bridge transactions as part of multi-sig validation
 * - Participates in consensus for 4/7 quorum approval
 * - Tracks validator health and performance metrics
 *
 * Key Features:
 * - ECDSA-based digital signatures (SHA256withECDSA)
 * - Signature verification for transaction approval
 * - Validator reputation tracking
 * - Heartbeat monitoring for availability
 *
 * @author IBA + BDA
 * @version 11.1.0
 * @since Sprint 14
 */
public class BridgeValidatorNode {

    private final String validatorId;
    private final String validatorName;
    private final KeyPair keyPair;
    private final long createdAt;
    private boolean active;
    private long lastHeartbeat;
    private int successfulSignatures;
    private int failedSignatures;
    private double reputationScore;

    /**
     * Create a new bridge validator node
     *
     * @param validatorId Unique identifier for the validator
     * @param validatorName Human-readable validator name
     * @throws Exception If keypair generation fails
     */
    public BridgeValidatorNode(String validatorId, String validatorName) throws Exception {
        this.validatorId = validatorId;
        this.validatorName = validatorName;
        this.createdAt = System.currentTimeMillis();
        this.active = true;
        this.lastHeartbeat = System.currentTimeMillis();
        this.successfulSignatures = 0;
        this.failedSignatures = 0;
        this.reputationScore = 100.0; // Start with perfect reputation

        // Generate ECDSA keypair for signing
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("EC");
        keyGen.initialize(256); // NIST P-256 curve
        this.keyPair = keyGen.generateKeyPair();
    }

    /**
     * Sign a bridge transaction for multi-signature validation
     *
     * @param transactionData The transaction data to sign
     * @return Base64-encoded signature
     * @throws Exception If signing fails
     */
    public String signTransaction(String transactionData) throws Exception {
        try {
            Signature signature = Signature.getInstance("SHA256withECDSA");
            signature.initSign(keyPair.getPrivate());
            signature.update(transactionData.getBytes());
            byte[] signedData = signature.sign();

            successfulSignatures++;
            updateReputation();

            return Base64.getEncoder().encodeToString(signedData);
        } catch (Exception e) {
            failedSignatures++;
            updateReputation();
            throw e;
        }
    }

    /**
     * Verify a signature from another validator
     *
     * @param transactionData The transaction data that was signed
     * @param signature Base64-encoded signature to verify
     * @param validatorPublicKey The public key of the signing validator
     * @return true if signature is valid, false otherwise
     */
    public boolean verifySignature(String transactionData, String signature, PublicKey validatorPublicKey) {
        try {
            Signature signatureVerifier = Signature.getInstance("SHA256withECDSA");
            signatureVerifier.initVerify(validatorPublicKey);
            signatureVerifier.update(transactionData.getBytes());

            byte[] decodedSignature = Base64.getDecoder().decode(signature);
            return signatureVerifier.verify(decodedSignature);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Update validator reputation based on performance
     * Reputation ranges from 0-100
     * - Successful signatures increase reputation
     * - Failed signatures decrease reputation
     */
    private void updateReputation() {
        int total = successfulSignatures + failedSignatures;
        if (total == 0) {
            reputationScore = 100.0;
            return;
        }

        double successRate = (double) successfulSignatures / total;
        reputationScore = successRate * 100.0;

        // Penalty for inactivity
        long inactiveMinutes = (System.currentTimeMillis() - lastHeartbeat) / 60000;
        if (inactiveMinutes > 5) {
            reputationScore -= inactiveMinutes * 5; // 5 points per minute inactive
        }

        // Ensure reputation stays in valid range
        reputationScore = Math.max(0.0, Math.min(100.0, reputationScore));
    }

    /**
     * Send heartbeat to indicate validator is alive
     */
    public void sendHeartbeat() {
        this.lastHeartbeat = System.currentTimeMillis();
        this.active = true;
    }

    /**
     * Check if validator is responsive
     * Considers a validator inactive if no heartbeat in 5 minutes
     *
     * @return true if validator is responsive
     */
    public boolean isResponsive() {
        long inactiveMinutes = (System.currentTimeMillis() - lastHeartbeat) / 60000;
        return inactiveMinutes < 5;
    }

    // Getters

    public String getValidatorId() {
        return validatorId;
    }

    public String getValidatorName() {
        return validatorName;
    }

    public PublicKey getPublicKey() {
        return keyPair.getPublic();
    }

    public PrivateKey getPrivateKey() {
        return keyPair.getPrivate();
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public long getLastHeartbeat() {
        return lastHeartbeat;
    }

    public int getSuccessfulSignatures() {
        return successfulSignatures;
    }

    public int getFailedSignatures() {
        return failedSignatures;
    }

    public double getReputationScore() {
        return reputationScore;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    @Override
    public String toString() {
        return String.format("ValidatorNode[id=%s, name=%s, active=%s, reputation=%.1f, signatures=%d/%d]",
                validatorId, validatorName, active, reputationScore,
                successfulSignatures, successfulSignatures + failedSignatures);
    }
}
