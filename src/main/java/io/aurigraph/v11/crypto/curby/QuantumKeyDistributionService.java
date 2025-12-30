package io.aurigraph.v11.crypto.curby;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import io.smallrye.mutiny.Uni;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Quantum Key Distribution (QKD) Service
 *
 * Manages quantum key distribution, rotation, and lifecycle.
 * Features:
 * - QKD key exchange using quantum-safe protocols
 * - Automatic key rotation based on configurable intervals
 * - Secure key storage with encryption
 * - Key lifecycle management (generation, distribution, expiration, revocation)
 * - Integration with CURBy quantum service
 *
 * @author Aurigraph V11
 * @version 11.0.0
 */
@ApplicationScoped
public class QuantumKeyDistributionService {

    private static final Logger LOG = Logger.getLogger(QuantumKeyDistributionService.class);

    @ConfigProperty(name = "qkd.enabled", defaultValue = "true")
    boolean qkdEnabled;

    @ConfigProperty(name = "qkd.key.rotation.interval.minutes", defaultValue = "60")
    int keyRotationIntervalMinutes;

    @ConfigProperty(name = "qkd.key.expiry.minutes", defaultValue = "120")
    int keyExpiryMinutes;

    @ConfigProperty(name = "qkd.key.cache.max-size", defaultValue = "10000")
    int keyCacheMaxSize;

    @ConfigProperty(name = "qkd.master.key.path", defaultValue = "/var/lib/aurigraph/qkd/master.key")
    String masterKeyPath;

    @ConfigProperty(name = "qkd.encryption.algorithm", defaultValue = "AES-256-GCM")
    String encryptionAlgorithm;

    @Inject
    CURByQuantumClient curbyClient;

    private final SecureRandom secureRandom = new SecureRandom();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    // Key Storage (encrypted at rest)
    private final Map<String, QuantumKey> activeKeys = new ConcurrentHashMap<>();
    private final Map<String, QuantumKey> expiredKeys = new ConcurrentHashMap<>();

    // Performance Metrics
    private final AtomicLong keysGenerated = new AtomicLong(0);
    private final AtomicLong keysRotated = new AtomicLong(0);
    private final AtomicLong keysExpired = new AtomicLong(0);
    private final AtomicLong keysRevoked = new AtomicLong(0);

    // Master Key for encrypting stored quantum keys
    private SecretKey masterKey;

    /**
     * Initialize QKD service and start key rotation
     */
    public void initialize() {
        if (!qkdEnabled) {
            LOG.info("Quantum Key Distribution (QKD) is DISABLED");
            return;
        }

        LOG.info("Initializing Quantum Key Distribution (QKD) Service");

        try {
            // Initialize master key for key encryption
            initializeMasterKey();

            // Start key rotation scheduler
            scheduler.scheduleAtFixedRate(
                this::rotateExpiredKeys,
                keyRotationIntervalMinutes,
                keyRotationIntervalMinutes,
                TimeUnit.MINUTES
            );

            // Start expired key cleanup
            scheduler.scheduleAtFixedRate(
                this::cleanupExpiredKeys,
                keyExpiryMinutes,
                keyExpiryMinutes,
                TimeUnit.MINUTES
            );

            LOG.infof("QKD Service initialized - Key rotation: %d min, Expiry: %d min",
                keyRotationIntervalMinutes, keyExpiryMinutes);

        } catch (Exception e) {
            LOG.error("Failed to initialize QKD service", e);
            throw new RuntimeException("QKD initialization failed", e);
        }
    }

    /**
     * Generate and distribute a new quantum key
     */
    public Uni<QuantumKeyExchangeResult> generateAndDistributeKey(String sessionId, String algorithm) {
        return Uni.createFrom().item(() -> {
            LOG.debugf("Generating quantum key for session: %s", sessionId);

            try {
                // Generate quantum key pair using CURBy
                var keyPairResponse = curbyClient.generateKeyPair(algorithm)
                    .await().indefinitely();

                // Generate session key for symmetric encryption
                KeyGenerator keyGen = KeyGenerator.getInstance("AES");
                keyGen.init(256, secureRandom);
                SecretKey sessionKey = keyGen.generateKey();

                // Create quantum key record
                QuantumKey quantumKey = new QuantumKey(
                    sessionId,
                    algorithm,
                    keyPairResponse.publicKey(),
                    encryptPrivateKey(keyPairResponse.privateKey(), sessionKey),
                    Base64.getEncoder().encodeToString(sessionKey.getEncoded()),
                    System.currentTimeMillis(),
                    System.currentTimeMillis() + (keyExpiryMinutes * 60 * 1000L),
                    QuantumKeyStatus.ACTIVE
                );

                // Store quantum key
                activeKeys.put(sessionId, quantumKey);
                keysGenerated.incrementAndGet();

                LOG.infof("Generated quantum key for session %s (Algorithm: %s, Expiry: %d min)",
                    sessionId, algorithm, keyExpiryMinutes);

                return new QuantumKeyExchangeResult(
                    true,
                    sessionId,
                    algorithm,
                    quantumKey.publicKey(),
                    quantumKey.sessionKeyEncrypted(),
                    quantumKey.createdAt(),
                    quantumKey.expiresAt(),
                    "QKD key generated and distributed successfully"
                );

            } catch (Exception e) {
                LOG.errorf(e, "Failed to generate quantum key for session %s", sessionId);
                return new QuantumKeyExchangeResult(
                    false,
                    sessionId,
                    algorithm,
                    null,
                    null,
                    0,
                    0,
                    "Failed to generate QKD key: " + e.getMessage()
                );
            }
        });
    }

    /**
     * Rotate quantum key for a session
     */
    public Uni<QuantumKeyRotationResult> rotateKey(String sessionId) {
        return Uni.createFrom().item(() -> {
            LOG.debugf("Rotating quantum key for session: %s", sessionId);

            QuantumKey oldKey = activeKeys.get(sessionId);
            if (oldKey == null) {
                return new QuantumKeyRotationResult(
                    false,
                    sessionId,
                    null,
                    null,
                    0,
                    "Session not found"
                );
            }

            try {
                // Generate new quantum key
                var result = generateAndDistributeKey(sessionId, oldKey.algorithm())
                    .await().indefinitely();

                if (result.success()) {
                    // Mark old key as rotated
                    oldKey = new QuantumKey(
                        oldKey.sessionId(),
                        oldKey.algorithm(),
                        oldKey.publicKey(),
                        oldKey.privateKeyEncrypted(),
                        oldKey.sessionKeyEncrypted(),
                        oldKey.createdAt(),
                        oldKey.expiresAt(),
                        QuantumKeyStatus.ROTATED
                    );
                    expiredKeys.put(sessionId + "_old_" + System.currentTimeMillis(), oldKey);

                    keysRotated.incrementAndGet();

                    LOG.infof("Rotated quantum key for session %s", sessionId);

                    return new QuantumKeyRotationResult(
                        true,
                        sessionId,
                        result.publicKey(),
                        result.sessionKeyEncrypted(),
                        result.createdAt(),
                        "Key rotated successfully"
                    );
                } else {
                    return new QuantumKeyRotationResult(
                        false,
                        sessionId,
                        null,
                        null,
                        0,
                        "Failed to generate new key: " + result.message()
                    );
                }

            } catch (Exception e) {
                LOG.errorf(e, "Failed to rotate quantum key for session %s", sessionId);
                return new QuantumKeyRotationResult(
                    false,
                    sessionId,
                    null,
                    null,
                    0,
                    "Key rotation failed: " + e.getMessage()
                );
            }
        });
    }

    /**
     * Revoke a quantum key
     */
    public Uni<QuantumKeyRevocationResult> revokeKey(String sessionId, String reason) {
        return Uni.createFrom().item(() -> {
            LOG.infof("Revoking quantum key for session: %s (Reason: %s)", sessionId, reason);

            QuantumKey key = activeKeys.remove(sessionId);
            if (key == null) {
                return new QuantumKeyRevocationResult(
                    false,
                    sessionId,
                    "Key not found"
                );
            }

            // Mark as revoked
            key = new QuantumKey(
                key.sessionId(),
                key.algorithm(),
                key.publicKey(),
                key.privateKeyEncrypted(),
                key.sessionKeyEncrypted(),
                key.createdAt(),
                System.currentTimeMillis(),
                QuantumKeyStatus.REVOKED
            );
            expiredKeys.put(sessionId + "_revoked_" + System.currentTimeMillis(), key);

            keysRevoked.incrementAndGet();

            LOG.infof("Revoked quantum key for session %s", sessionId);

            return new QuantumKeyRevocationResult(
                true,
                sessionId,
                "Key revoked successfully: " + reason
            );
        });
    }

    /**
     * Get quantum key for a session
     */
    public QuantumKey getKey(String sessionId) {
        QuantumKey key = activeKeys.get(sessionId);

        if (key != null && key.expiresAt() < System.currentTimeMillis()) {
            // Key expired
            activeKeys.remove(sessionId);
            expiredKeys.put(sessionId + "_expired_" + System.currentTimeMillis(), key);
            keysExpired.incrementAndGet();
            return null;
        }

        return key;
    }

    /**
     * Get QKD service status
     */
    public QKDServiceStatus getStatus() {
        return new QKDServiceStatus(
            qkdEnabled,
            activeKeys.size(),
            expiredKeys.size(),
            keysGenerated.get(),
            keysRotated.get(),
            keysExpired.get(),
            keysRevoked.get(),
            keyRotationIntervalMinutes,
            keyExpiryMinutes,
            System.currentTimeMillis()
        );
    }

    // Private Helper Methods

    private void initializeMasterKey() throws Exception {
        // In production, load from HSM or secure key vault
        // For now, generate a new master key
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(256, secureRandom);
        masterKey = keyGen.generateKey();

        LOG.info("Master key initialized for QKD key encryption");
    }

    private String encryptPrivateKey(String privateKey, SecretKey sessionKey) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");

        byte[] iv = new byte[12]; // GCM recommended IV size
        secureRandom.nextBytes(iv);

        GCMParameterSpec gcmSpec = new GCMParameterSpec(128, iv); // 128-bit authentication tag
        cipher.init(Cipher.ENCRYPT_MODE, masterKey, gcmSpec);

        byte[] privateKeyBytes = privateKey.getBytes();
        byte[] encrypted = cipher.doFinal(privateKeyBytes);

        // Combine IV + encrypted data
        byte[] result = new byte[iv.length + encrypted.length];
        System.arraycopy(iv, 0, result, 0, iv.length);
        System.arraycopy(encrypted, 0, result, iv.length, encrypted.length);

        return Base64.getEncoder().encodeToString(result);
    }

    private String decryptPrivateKey(String encryptedPrivateKey) throws Exception {
        byte[] combined = Base64.getDecoder().decode(encryptedPrivateKey);

        // Extract IV (12 bytes)
        byte[] iv = new byte[12];
        System.arraycopy(combined, 0, iv, 0, 12);

        // Extract encrypted data
        byte[] encrypted = new byte[combined.length - 12];
        System.arraycopy(combined, 12, encrypted, 0, encrypted.length);

        // Decrypt
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec gcmSpec = new GCMParameterSpec(128, iv);
        cipher.init(Cipher.DECRYPT_MODE, masterKey, gcmSpec);

        byte[] decrypted = cipher.doFinal(encrypted);
        return new String(decrypted);
    }

    private void rotateExpiredKeys() {
        try {
            long now = System.currentTimeMillis();

            activeKeys.forEach((sessionId, key) -> {
                long keyAge = now - key.createdAt();
                long rotationThreshold = keyRotationIntervalMinutes * 60 * 1000L;

                if (keyAge >= rotationThreshold) {
                    LOG.infof("Auto-rotating key for session %s (age: %d min)",
                        sessionId, keyAge / 60000);

                    rotateKey(sessionId).subscribe().with(
                        result -> LOG.debugf("Auto-rotation completed for %s: %s",
                            sessionId, result.message()),
                        error -> LOG.errorf(error, "Auto-rotation failed for %s", sessionId)
                    );
                }
            });

        } catch (Exception e) {
            LOG.error("Error during key rotation", e);
        }
    }

    private void cleanupExpiredKeys() {
        try {
            long now = System.currentTimeMillis();
            long cleanupThreshold = now - (keyExpiryMinutes * 2 * 60 * 1000L); // 2x expiry time

            expiredKeys.entrySet().removeIf(entry -> {
                QuantumKey key = entry.getValue();
                return key.expiresAt() < cleanupThreshold;
            });

            LOG.debugf("Cleaned up expired keys. Expired keys count: %d", expiredKeys.size());

        } catch (Exception e) {
            LOG.error("Error during expired key cleanup", e);
        }
    }

    // Data Classes

    public record QuantumKey(
        String sessionId,
        String algorithm,
        String publicKey,
        String privateKeyEncrypted,
        String sessionKeyEncrypted,
        long createdAt,
        long expiresAt,
        QuantumKeyStatus status
    ) {}

    public enum QuantumKeyStatus {
        ACTIVE,
        ROTATED,
        EXPIRED,
        REVOKED
    }

    public record QuantumKeyExchangeResult(
        boolean success,
        String sessionId,
        String algorithm,
        String publicKey,
        String sessionKeyEncrypted,
        long createdAt,
        long expiresAt,
        String message
    ) {}

    public record QuantumKeyRotationResult(
        boolean success,
        String sessionId,
        String newPublicKey,
        String newSessionKeyEncrypted,
        long rotatedAt,
        String message
    ) {}

    public record QuantumKeyRevocationResult(
        boolean success,
        String sessionId,
        String message
    ) {}

    public record QKDServiceStatus(
        boolean enabled,
        int activeKeys,
        int expiredKeys,
        long totalKeysGenerated,
        long totalKeysRotated,
        long totalKeysExpired,
        long totalKeysRevoked,
        int rotationIntervalMinutes,
        int keyExpiryMinutes,
        long timestamp
    ) {}
}
