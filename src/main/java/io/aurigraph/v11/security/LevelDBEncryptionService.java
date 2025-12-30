package io.aurigraph.v11.security;

import io.smallrye.mutiny.Uni;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.concurrent.atomic.AtomicLong;

/**
 * LevelDB Encryption Service - AES-256-GCM Encryption at Rest
 *
 * Provides military-grade encryption for LevelDB data at rest using:
 * - AES-256-GCM (Galois/Counter Mode) for authenticated encryption
 * - 256-bit keys for maximum security
 * - 96-bit random IV per encryption operation
 * - 128-bit authentication tag for integrity verification
 * - NIST SP 800-38D compliant
 *
 * Security Features:
 * - Confidentiality: AES-256 encryption
 * - Integrity: GCM authentication tag
 * - Authenticity: AEAD (Authenticated Encryption with Associated Data)
 * - IV uniqueness: Cryptographically secure random IV per operation
 * - Side-channel resistance: Constant-time operations where possible
 *
 * @author Aurigraph Security Team
 * @version 11.3.0
 * @since October 2025
 */
@ApplicationScoped
public class LevelDBEncryptionService {

    private static final Logger logger = LoggerFactory.getLogger(LevelDBEncryptionService.class);

    // AES-256-GCM configuration (highest security)
    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int KEY_SIZE_BITS = 256;
    private static final int IV_SIZE_BYTES = 12;  // 96 bits (NIST recommended for GCM)
    private static final int AUTH_TAG_SIZE_BITS = 128;  // 128 bits authentication tag

    // Version byte for future algorithm upgrades
    private static final byte ENCRYPTION_VERSION = 0x01;

    @ConfigProperty(name = "leveldb.encryption.enabled", defaultValue = "true")
    boolean encryptionEnabled;

    @ConfigProperty(name = "leveldb.encryption.algorithm", defaultValue = "AES-256-GCM")
    String encryptionAlgorithm;

    @Inject
    LevelDBKeyManagementService keyManager;

    @Inject
    SecurityAuditService auditService;

    private final SecureRandom secureRandom;
    private final AtomicLong encryptionCount = new AtomicLong(0);
    private final AtomicLong decryptionCount = new AtomicLong(0);
    private final AtomicLong encryptionErrors = new AtomicLong(0);

    public LevelDBEncryptionService() {
        try {
            // Use strongest available PRNG
            this.secureRandom = SecureRandom.getInstanceStrong();
            logger.info("Initialized SecureRandom: {}", secureRandom.getAlgorithm());
        } catch (Exception e) {
            logger.error("Failed to initialize SecureRandom", e);
            throw new RuntimeException("Critical security initialization failure", e);
        }
    }

    /**
     * Encrypt data using AES-256-GCM
     *
     * Format: [VERSION:1][IV:12][CIPHERTEXT:N][TAG:16]
     *
     * @param plaintext Plaintext data to encrypt
     * @return Encrypted data with IV and authentication tag
     */
    public Uni<byte[]> encrypt(byte[] plaintext) {
        return Uni.createFrom().item(() -> {
            if (!encryptionEnabled) {
                logger.warn("Encryption disabled - storing plaintext (INSECURE!)");
                auditService.logSecurityEvent("ENCRYPTION_DISABLED", "Plaintext storage");
                return plaintext;
            }

            if (plaintext == null || plaintext.length == 0) {
                throw new IllegalArgumentException("Plaintext cannot be null or empty");
            }

            try {
                long startTime = System.nanoTime();

                // Get encryption key from key manager
                SecretKey key = keyManager.getDatabaseEncryptionKey();

                // Generate cryptographically secure random IV
                byte[] iv = new byte[IV_SIZE_BYTES];
                secureRandom.nextBytes(iv);

                // Initialize cipher
                Cipher cipher = Cipher.getInstance(ALGORITHM);
                GCMParameterSpec parameterSpec = new GCMParameterSpec(AUTH_TAG_SIZE_BITS, iv);
                cipher.init(Cipher.ENCRYPT_MODE, key, parameterSpec);

                // Encrypt plaintext (produces ciphertext + authentication tag)
                byte[] ciphertext = cipher.doFinal(plaintext);

                // Build output: [VERSION][IV][CIPHERTEXT+TAG]
                ByteBuffer buffer = ByteBuffer.allocate(1 + IV_SIZE_BYTES + ciphertext.length);
                buffer.put(ENCRYPTION_VERSION);
                buffer.put(iv);
                buffer.put(ciphertext);
                byte[] encrypted = buffer.array();

                // Metrics and audit
                encryptionCount.incrementAndGet();
                long durationNanos = System.nanoTime() - startTime;

                if (logger.isDebugEnabled()) {
                    logger.debug("Encrypted {} bytes to {} bytes in {} μs",
                        plaintext.length, encrypted.length, durationNanos / 1000);
                }

                return encrypted;

            } catch (Exception e) {
                encryptionErrors.incrementAndGet();
                auditService.logSecurityViolation("ENCRYPTION_FAILED",
                    "System", e.getMessage());
                logger.error("Encryption failed", e);
                throw new RuntimeException("Encryption failed", e);
            }
        });
    }

    /**
     * Decrypt data using AES-256-GCM
     *
     * Expected format: [VERSION:1][IV:12][CIPHERTEXT:N][TAG:16]
     *
     * @param encrypted Encrypted data with IV and authentication tag
     * @return Decrypted plaintext data
     */
    public Uni<byte[]> decrypt(byte[] encrypted) {
        return Uni.createFrom().item(() -> {
            if (!encryptionEnabled) {
                logger.warn("Encryption disabled - reading plaintext (INSECURE!)");
                return encrypted;
            }

            if (encrypted == null || encrypted.length < 1 + IV_SIZE_BYTES + AUTH_TAG_SIZE_BITS / 8) {
                throw new IllegalArgumentException("Invalid encrypted data format");
            }

            try {
                long startTime = System.nanoTime();

                // Parse encrypted data
                ByteBuffer buffer = ByteBuffer.wrap(encrypted);
                byte version = buffer.get();

                if (version != ENCRYPTION_VERSION) {
                    throw new IllegalArgumentException(
                        "Unsupported encryption version: " + version);
                }

                // Extract IV
                byte[] iv = new byte[IV_SIZE_BYTES];
                buffer.get(iv);

                // Extract ciphertext + tag
                byte[] ciphertext = new byte[buffer.remaining()];
                buffer.get(ciphertext);

                // Get decryption key
                SecretKey key = keyManager.getDatabaseEncryptionKey();

                // Initialize cipher for decryption
                Cipher cipher = Cipher.getInstance(ALGORITHM);
                GCMParameterSpec parameterSpec = new GCMParameterSpec(AUTH_TAG_SIZE_BITS, iv);
                cipher.init(Cipher.DECRYPT_MODE, key, parameterSpec);

                // Decrypt and verify authentication tag
                byte[] plaintext = cipher.doFinal(ciphertext);

                // Metrics
                decryptionCount.incrementAndGet();
                long durationNanos = System.nanoTime() - startTime;

                if (logger.isDebugEnabled()) {
                    logger.debug("Decrypted {} bytes to {} bytes in {} μs",
                        encrypted.length, plaintext.length, durationNanos / 1000);
                }

                return plaintext;

            } catch (Exception e) {
                encryptionErrors.incrementAndGet();
                auditService.logSecurityViolation("DECRYPTION_FAILED",
                    "System", e.getMessage());
                logger.error("Decryption failed - possible tampering detected", e);
                throw new RuntimeException("Decryption failed - data may be tampered", e);
            }
        });
    }

    /**
     * Encrypt a string value
     */
    public Uni<byte[]> encryptString(String plaintext) {
        if (plaintext == null) {
            return Uni.createFrom().nullItem();
        }
        return encrypt(plaintext.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    /**
     * Decrypt to string value
     */
    public Uni<String> decryptString(byte[] encrypted) {
        if (encrypted == null) {
            return Uni.createFrom().nullItem();
        }
        return decrypt(encrypted)
            .map(bytes -> new String(bytes, java.nio.charset.StandardCharsets.UTF_8));
    }

    /**
     * Check if data is encrypted (has version header)
     */
    public boolean isEncrypted(byte[] data) {
        if (data == null || data.length < 1) {
            return false;
        }
        return data[0] == ENCRYPTION_VERSION;
    }

    /**
     * Re-encrypt data with new key (for key rotation)
     */
    public Uni<byte[]> reencrypt(byte[] encrypted) {
        return decrypt(encrypted)
            .flatMap(this::encrypt);
    }

    /**
     * Get encryption statistics
     */
    public EncryptionStats getStats() {
        return new EncryptionStats(
            encryptionEnabled,
            encryptionAlgorithm,
            KEY_SIZE_BITS,
            encryptionCount.get(),
            decryptionCount.get(),
            encryptionErrors.get()
        );
    }

    /**
     * Encryption statistics
     */
    public record EncryptionStats(
        boolean enabled,
        String algorithm,
        int keySize,
        long encryptionCount,
        long decryptionCount,
        long errorCount
    ) {}
}
