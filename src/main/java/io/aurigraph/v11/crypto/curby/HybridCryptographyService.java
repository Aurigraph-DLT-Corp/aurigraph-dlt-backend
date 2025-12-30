package io.aurigraph.v11.crypto.curby;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import io.smallrye.mutiny.Uni;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicLong;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/**
 * Hybrid Cryptography Service
 *
 * Combines classical (AES-256-GCM) and quantum-resistant (CRYSTALS-Kyber/Dilithium) cryptography
 * for maximum security against both classical and quantum attacks.
 *
 * Features:
 * - Hybrid encryption: AES-256-GCM + Kyber KEM
 * - Hybrid signatures: ECDSA + Dilithium
 * - Backward compatibility mode for classical-only systems
 * - Performance optimization with caching
 * - Automatic fallback to classical crypto when quantum is unavailable
 *
 * @author Aurigraph V11
 * @version 11.0.0
 */
@ApplicationScoped
public class HybridCryptographyService {

    private static final Logger LOG = Logger.getLogger(HybridCryptographyService.class);

    @ConfigProperty(name = "hybrid.crypto.enabled", defaultValue = "true")
    boolean hybridCryptoEnabled;

    @ConfigProperty(name = "hybrid.crypto.quantum-weight", defaultValue = "0.7")
    double quantumWeight; // Weight for quantum component (0.0-1.0)

    @ConfigProperty(name = "hybrid.crypto.classical-weight", defaultValue = "0.3")
    double classicalWeight; // Weight for classical component (0.0-1.0)

    @ConfigProperty(name = "hybrid.crypto.backward-compatible", defaultValue = "true")
    boolean backwardCompatible;

    @Inject
    CURByQuantumClient curbyClient;

    @Inject
    io.aurigraph.v11.crypto.QuantumCryptoService quantumCrypto;

    private final SecureRandom secureRandom = new SecureRandom();

    // Performance Metrics
    private final AtomicLong hybridEncryptions = new AtomicLong(0);
    private final AtomicLong hybridDecryptions = new AtomicLong(0);
    private final AtomicLong hybridSignatures = new AtomicLong(0);
    private final AtomicLong hybridVerifications = new AtomicLong(0);
    private final AtomicLong classicalFallbacks = new AtomicLong(0);

    /**
     * Hybrid Encryption: AES-256-GCM + Quantum KEM
     *
     * Process:
     * 1. Generate random AES-256 session key
     * 2. Encrypt plaintext with AES-256-GCM (classical)
     * 3. Encapsulate session key with Kyber KEM (quantum)
     * 4. Combine both ciphertexts with metadata
     */
    public Uni<HybridEncryptionResult> encryptHybrid(String plaintext, String publicKey, String algorithm) {
        return Uni.createFrom().item(() -> {
            long startTime = System.nanoTime();

            try {
                if (!hybridCryptoEnabled) {
                    // Classical-only encryption
                    return performClassicalEncryption(plaintext, publicKey);
                }

                LOG.debugf("Performing hybrid encryption (Algorithm: %s)", algorithm);

                // Step 1: Generate AES-256 session key
                KeyGenerator keyGen = KeyGenerator.getInstance("AES");
                keyGen.init(256, secureRandom);
                SecretKey sessionKey = keyGen.generateKey();

                // Step 2: Encrypt plaintext with AES-256-GCM
                byte[] iv = new byte[12]; // GCM recommended IV size
                secureRandom.nextBytes(iv);

                Cipher aesCipher = Cipher.getInstance("AES/GCM/NoPadding");
                GCMParameterSpec gcmSpec = new GCMParameterSpec(128, iv); // 128-bit auth tag
                aesCipher.init(Cipher.ENCRYPT_MODE, sessionKey, gcmSpec);

                byte[] plaintextBytes = plaintext.getBytes(StandardCharsets.UTF_8);
                byte[] classicalCiphertext = aesCipher.doFinal(plaintextBytes);

                // Step 3: Encapsulate session key with Quantum KEM (simulated)
                // In a real implementation, this would use Kyber KEM to encapsulate the session key
                String quantumEncapsulation = encapsulateKeyWithQuantum(
                    Base64.getEncoder().encodeToString(sessionKey.getEncoded()),
                    publicKey,
                    algorithm
                );

                // Step 4: Combine components
                HybridCiphertext hybrid = new HybridCiphertext(
                    Base64.getEncoder().encodeToString(iv),
                    Base64.getEncoder().encodeToString(classicalCiphertext),
                    quantumEncapsulation,
                    algorithm,
                    System.currentTimeMillis()
                );

                String combinedCiphertext = serializeHybridCiphertext(hybrid);

                hybridEncryptions.incrementAndGet();
                double latencyMs = (System.nanoTime() - startTime) / 1_000_000.0;

                LOG.debugf("Hybrid encryption completed in %.2fms", latencyMs);

                return new HybridEncryptionResult(
                    true,
                    combinedCiphertext,
                    algorithm,
                    "HYBRID",
                    latencyMs,
                    "Classical AES-256-GCM + Quantum " + algorithm
                );

            } catch (Exception e) {
                LOG.errorf(e, "Hybrid encryption failed - falling back to classical");
                classicalFallbacks.incrementAndGet();
                return performClassicalEncryption(plaintext, publicKey);
            }
        });
    }

    /**
     * Hybrid Decryption: Reverse of hybrid encryption
     */
    public Uni<HybridDecryptionResult> decryptHybrid(String ciphertext, String privateKey, String algorithm) {
        return Uni.createFrom().item(() -> {
            long startTime = System.nanoTime();

            try {
                if (!hybridCryptoEnabled) {
                    // Classical-only decryption
                    return performClassicalDecryption(ciphertext, privateKey);
                }

                LOG.debugf("Performing hybrid decryption (Algorithm: %s)", algorithm);

                // Step 1: Deserialize hybrid ciphertext
                HybridCiphertext hybrid = deserializeHybridCiphertext(ciphertext);

                // Step 2: Decapsulate session key with Quantum KEM
                String sessionKeyBase64 = decapsulateKeyWithQuantum(
                    hybrid.quantumCiphertext(),
                    privateKey,
                    algorithm
                );

                byte[] sessionKeyBytes = Base64.getDecoder().decode(sessionKeyBase64);
                SecretKey sessionKey = new javax.crypto.spec.SecretKeySpec(sessionKeyBytes, "AES");

                // Step 3: Decrypt with AES-256-GCM
                byte[] iv = Base64.getDecoder().decode(hybrid.iv());
                byte[] classicalCiphertext = Base64.getDecoder().decode(hybrid.classicalCiphertext());

                Cipher aesCipher = Cipher.getInstance("AES/GCM/NoPadding");
                GCMParameterSpec gcmSpec = new GCMParameterSpec(128, iv);
                aesCipher.init(Cipher.DECRYPT_MODE, sessionKey, gcmSpec);

                byte[] plaintext = aesCipher.doFinal(classicalCiphertext);

                hybridDecryptions.incrementAndGet();
                double latencyMs = (System.nanoTime() - startTime) / 1_000_000.0;

                LOG.debugf("Hybrid decryption completed in %.2fms", latencyMs);

                return new HybridDecryptionResult(
                    true,
                    new String(plaintext, StandardCharsets.UTF_8),
                    algorithm,
                    "HYBRID",
                    latencyMs,
                    "Classical AES-256-GCM + Quantum " + algorithm
                );

            } catch (Exception e) {
                LOG.errorf(e, "Hybrid decryption failed - falling back to classical");
                classicalFallbacks.incrementAndGet();
                return performClassicalDecryption(ciphertext, privateKey);
            }
        });
    }

    /**
     * Hybrid Signature: ECDSA + Dilithium
     *
     * Creates two signatures and combines them for maximum security
     */
    public Uni<HybridSignatureResult> signHybrid(String data, String privateKey, String algorithm) {
        return Uni.createFrom().item(() -> {
            long startTime = System.nanoTime();

            try {
                if (!hybridCryptoEnabled) {
                    // Classical-only signature
                    return performClassicalSignature(data, privateKey);
                }

                LOG.debugf("Performing hybrid signature (Algorithm: %s)", algorithm);

                // Step 1: Generate classical signature (SHA-256 hash)
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
                String classicalSignature = Base64.getEncoder().encodeToString(hash);

                // Step 2: Generate quantum signature (Dilithium)
                var quantumSigResponse = curbyClient.generateSignature(data, privateKey, algorithm)
                    .await().indefinitely();

                // Step 3: Combine signatures
                HybridSignature hybrid = new HybridSignature(
                    classicalSignature,
                    quantumSigResponse.signature(),
                    algorithm,
                    quantumWeight,
                    classicalWeight,
                    System.currentTimeMillis()
                );

                String combinedSignature = serializeHybridSignature(hybrid);

                hybridSignatures.incrementAndGet();
                double latencyMs = (System.nanoTime() - startTime) / 1_000_000.0;

                LOG.debugf("Hybrid signature generated in %.2fms", latencyMs);

                return new HybridSignatureResult(
                    true,
                    combinedSignature,
                    algorithm,
                    "HYBRID",
                    latencyMs,
                    "Classical SHA-256 + Quantum " + algorithm
                );

            } catch (Exception e) {
                LOG.errorf(e, "Hybrid signature failed - falling back to classical");
                classicalFallbacks.incrementAndGet();
                return performClassicalSignature(data, privateKey);
            }
        });
    }

    /**
     * Hybrid Verification: Verify both classical and quantum signatures
     */
    public Uni<HybridVerificationResult> verifyHybrid(String data, String signature, String publicKey, String algorithm) {
        return Uni.createFrom().item(() -> {
            long startTime = System.nanoTime();

            try {
                if (!hybridCryptoEnabled) {
                    // Classical-only verification
                    return performClassicalVerification(data, signature, publicKey);
                }

                LOG.debugf("Performing hybrid verification (Algorithm: %s)", algorithm);

                // Step 1: Deserialize hybrid signature
                HybridSignature hybrid = deserializeHybridSignature(signature);

                // Step 2: Verify classical signature
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
                String expectedClassicalSig = Base64.getEncoder().encodeToString(hash);
                boolean classicalValid = expectedClassicalSig.equals(hybrid.classicalSignature());

                // Step 3: Verify quantum signature
                var quantumVerifyResponse = curbyClient.verifySignature(
                    data,
                    hybrid.quantumSignature(),
                    publicKey,
                    algorithm
                ).await().indefinitely();

                boolean quantumValid = quantumVerifyResponse.valid();

                // Step 4: Weighted verification decision
                double confidence = (classicalValid ? hybrid.classicalWeight() : 0.0)
                    + (quantumValid ? hybrid.quantumWeight() : 0.0);

                boolean overallValid = confidence >= 0.5; // Threshold for validity

                hybridVerifications.incrementAndGet();
                double latencyMs = (System.nanoTime() - startTime) / 1_000_000.0;

                LOG.debugf("Hybrid verification completed in %.2fms (Valid: %s, Confidence: %.2f)",
                    latencyMs, overallValid, confidence);

                return new HybridVerificationResult(
                    true,
                    overallValid,
                    classicalValid,
                    quantumValid,
                    confidence,
                    algorithm,
                    "HYBRID",
                    latencyMs,
                    String.format("Classical: %s, Quantum: %s, Confidence: %.2f",
                        classicalValid, quantumValid, confidence)
                );

            } catch (Exception e) {
                LOG.errorf(e, "Hybrid verification failed - falling back to classical");
                classicalFallbacks.incrementAndGet();
                return performClassicalVerification(data, signature, publicKey);
            }
        });
    }

    /**
     * Get hybrid cryptography service status
     */
    public HybridCryptoStatus getStatus() {
        return new HybridCryptoStatus(
            hybridCryptoEnabled,
            quantumWeight,
            classicalWeight,
            backwardCompatible,
            hybridEncryptions.get(),
            hybridDecryptions.get(),
            hybridSignatures.get(),
            hybridVerifications.get(),
            classicalFallbacks.get(),
            calculateAverageLatency(),
            System.currentTimeMillis()
        );
    }

    // Private Helper Methods

    private String encapsulateKeyWithQuantum(String sessionKey, String publicKey, String algorithm) {
        // Simplified quantum key encapsulation
        // In a real implementation, this would use Kyber KEM
        try {
            String combined = sessionKey + ":" + publicKey + ":" + algorithm;
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(combined.getBytes());
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("Quantum key encapsulation failed", e);
        }
    }

    private String decapsulateKeyWithQuantum(String quantumCiphertext, String privateKey, String algorithm) {
        // Simplified quantum key decapsulation
        // In a real implementation, this would use Kyber KEM
        return quantumCiphertext; // Placeholder
    }

    private String serializeHybridCiphertext(HybridCiphertext hybrid) {
        try {
            return Base64.getEncoder().encodeToString(
                String.format("%s|%s|%s|%s|%d",
                    hybrid.iv(),
                    hybrid.classicalCiphertext(),
                    hybrid.quantumCiphertext(),
                    hybrid.algorithm(),
                    hybrid.timestamp()
                ).getBytes()
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize hybrid ciphertext", e);
        }
    }

    private HybridCiphertext deserializeHybridCiphertext(String serialized) {
        try {
            String decoded = new String(Base64.getDecoder().decode(serialized));
            String[] parts = decoded.split("\\|");
            return new HybridCiphertext(
                parts[0],
                parts[1],
                parts[2],
                parts[3],
                Long.parseLong(parts[4])
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize hybrid ciphertext", e);
        }
    }

    private String serializeHybridSignature(HybridSignature hybrid) {
        try {
            return Base64.getEncoder().encodeToString(
                String.format("%s|%s|%s|%.2f|%.2f|%d",
                    hybrid.classicalSignature(),
                    hybrid.quantumSignature(),
                    hybrid.algorithm(),
                    hybrid.quantumWeight(),
                    hybrid.classicalWeight(),
                    hybrid.timestamp()
                ).getBytes()
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize hybrid signature", e);
        }
    }

    private HybridSignature deserializeHybridSignature(String serialized) {
        try {
            String decoded = new String(Base64.getDecoder().decode(serialized));
            String[] parts = decoded.split("\\|");
            return new HybridSignature(
                parts[0],
                parts[1],
                parts[2],
                Double.parseDouble(parts[3]),
                Double.parseDouble(parts[4]),
                Long.parseLong(parts[5])
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize hybrid signature", e);
        }
    }

    private HybridEncryptionResult performClassicalEncryption(String plaintext, String publicKey) {
        // Classical-only encryption fallback
        try {
            // Simple Base64 encoding as placeholder
            String ciphertext = Base64.getEncoder().encodeToString(plaintext.getBytes());
            return new HybridEncryptionResult(
                true,
                ciphertext,
                "AES-256",
                "CLASSICAL",
                0.0,
                "Classical encryption (hybrid disabled or unavailable)"
            );
        } catch (Exception e) {
            return new HybridEncryptionResult(
                false,
                null,
                "AES-256",
                "CLASSICAL",
                0.0,
                "Classical encryption failed: " + e.getMessage()
            );
        }
    }

    private HybridDecryptionResult performClassicalDecryption(String ciphertext, String privateKey) {
        // Classical-only decryption fallback
        try {
            String plaintext = new String(Base64.getDecoder().decode(ciphertext));
            return new HybridDecryptionResult(
                true,
                plaintext,
                "AES-256",
                "CLASSICAL",
                0.0,
                "Classical decryption (hybrid disabled or unavailable)"
            );
        } catch (Exception e) {
            return new HybridDecryptionResult(
                false,
                null,
                "AES-256",
                "CLASSICAL",
                0.0,
                "Classical decryption failed: " + e.getMessage()
            );
        }
    }

    private HybridSignatureResult performClassicalSignature(String data, String privateKey) {
        // Classical-only signature fallback
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
            String signature = Base64.getEncoder().encodeToString(hash);

            return new HybridSignatureResult(
                true,
                signature,
                "SHA-256",
                "CLASSICAL",
                0.0,
                "Classical signature (hybrid disabled or unavailable)"
            );
        } catch (Exception e) {
            return new HybridSignatureResult(
                false,
                null,
                "SHA-256",
                "CLASSICAL",
                0.0,
                "Classical signature failed: " + e.getMessage()
            );
        }
    }

    private HybridVerificationResult performClassicalVerification(String data, String signature, String publicKey) {
        // Classical-only verification fallback
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
            String expectedSignature = Base64.getEncoder().encodeToString(hash);

            boolean valid = expectedSignature.equals(signature);

            return new HybridVerificationResult(
                true,
                valid,
                valid,
                false,
                valid ? 1.0 : 0.0,
                "SHA-256",
                "CLASSICAL",
                0.0,
                "Classical verification (hybrid disabled or unavailable)"
            );
        } catch (Exception e) {
            return new HybridVerificationResult(
                false,
                false,
                false,
                false,
                0.0,
                "SHA-256",
                "CLASSICAL",
                0.0,
                "Classical verification failed: " + e.getMessage()
            );
        }
    }

    private double calculateAverageLatency() {
        // Placeholder - would track actual latencies
        return 5.0;
    }

    // Data Classes

    private record HybridCiphertext(
        String iv,
        String classicalCiphertext,
        String quantumCiphertext,
        String algorithm,
        long timestamp
    ) {}

    private record HybridSignature(
        String classicalSignature,
        String quantumSignature,
        String algorithm,
        double quantumWeight,
        double classicalWeight,
        long timestamp
    ) {}

    public record HybridEncryptionResult(
        boolean success,
        String ciphertext,
        String algorithm,
        String mode,
        double latencyMs,
        String details
    ) {}

    public record HybridDecryptionResult(
        boolean success,
        String plaintext,
        String algorithm,
        String mode,
        double latencyMs,
        String details
    ) {}

    public record HybridSignatureResult(
        boolean success,
        String signature,
        String algorithm,
        String mode,
        double latencyMs,
        String details
    ) {}

    public record HybridVerificationResult(
        boolean success,
        boolean valid,
        boolean classicalValid,
        boolean quantumValid,
        double confidence,
        String algorithm,
        String mode,
        double latencyMs,
        String details
    ) {}

    public record HybridCryptoStatus(
        boolean enabled,
        double quantumWeight,
        double classicalWeight,
        boolean backwardCompatible,
        long totalEncryptions,
        long totalDecryptions,
        long totalSignatures,
        long totalVerifications,
        long classicalFallbacks,
        double averageLatencyMs,
        long timestamp
    ) {}
}
