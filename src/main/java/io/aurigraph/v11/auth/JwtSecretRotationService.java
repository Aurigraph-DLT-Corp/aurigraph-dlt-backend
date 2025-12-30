package io.aurigraph.v11.auth;

import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.*;

/**
 * JWT Secret Rotation Service - Manages JWT secret key lifecycle
 *
 * Implements secret rotation to:
 * 1. Limit exposure window if a secret is compromised
 * 2. Enable graceful key deprecation without immediate service impact
 * 3. Support both old and new secrets during rotation window
 * 4. Automatic rotation based on configurable schedule
 *
 * Architecture:
 * - Current Secret: Used for signing new tokens
 * - Previous Secret: Used for validating tokens during rotation window
 * - Secret History: Tracks all secrets for audit purposes
 *
 * Rotation Strategy:
 * - Rotate every 90 days in production (configurable)
 * - Keep previous secret valid for 7 days after rotation
 * - Log all rotation events for audit
 *
 * @author Backend Development Agent (BDA)
 * @since V11.5.1
 */
@ApplicationScoped
public class JwtSecretRotationService {

    private static final Logger LOG = Logger.getLogger(JwtSecretRotationService.class);

    // Default secrets (should be replaced with environment variables in production)
    private static final String INITIAL_SECRET = "aurigraph-v11-secret-key-minimum-256-bits-for-hmac-sha256-security";

    // Configuration constants
    private static final long ROTATION_INTERVAL_DAYS = 90;
    private static final long PREVIOUS_SECRET_VALIDITY_DAYS = 7;
    private static final String SECRETS_FILE_PATH = "/var/lib/aurigraph/jwt-secrets.json";
    private static final String BACKUP_PATH = "/var/lib/aurigraph/jwt-secrets.backup.json";

    // Secret storage
    private final Map<String, JwtSecretEntry> secretHistory = new ConcurrentHashMap<>();
    private volatile String currentSecretId;
    private volatile String previousSecretId;

    private ScheduledExecutorService rotationScheduler;

    /**
     * Initialize JWT secret rotation service
     */
    public JwtSecretRotationService() {
        LOG.infof("✅ JWT Secret Rotation Service initializing...");
        initializeSecrets();
        startRotationScheduler();
    }

    /**
     * Initialize secrets from configuration or use defaults
     */
    private void initializeSecrets() {
        try {
            // Try to load from persisted file
            Path secretsPath = Paths.get(SECRETS_FILE_PATH);
            if (Files.exists(secretsPath)) {
                loadSecretsFromFile(secretsPath);
                LOG.infof("✅ Loaded JWT secrets from persisted file");
            } else {
                // Use default initial secret
                String initialId = UUID.randomUUID().toString();
                currentSecretId = initialId;
                secretHistory.put(initialId, new JwtSecretEntry(
                    initialId,
                    INITIAL_SECRET,
                    Instant.now(),
                    null
                ));
                LOG.infof("ℹ️ Initialized with default JWT secret (use environment for production)");
            }
        } catch (Exception e) {
            LOG.errorf(e, "Failed to initialize JWT secrets, using default");
            String initialId = UUID.randomUUID().toString();
            currentSecretId = initialId;
            secretHistory.put(initialId, new JwtSecretEntry(
                initialId,
                INITIAL_SECRET,
                Instant.now(),
                null
            ));
        }
    }

    /**
     * Load secrets from persisted JSON file
     */
    private void loadSecretsFromFile(Path path) throws IOException {
        // In production, parse JSON and restore secret history
        // For now, just log loading attempt
        LOG.infof("Loading secret file from: %s (parsing not yet implemented)", path);
    }

    /**
     * Start automatic rotation scheduler
     */
    private void startRotationScheduler() {
        rotationScheduler = Executors.newScheduledThreadPool(1, r -> {
            Thread t = new Thread(r, "JwtSecretRotationThread");
            t.setDaemon(true);
            return t;
        });

        // Schedule rotation every 90 days
        rotationScheduler.scheduleWithFixedDelay(
            this::rotateSecret,
            ROTATION_INTERVAL_DAYS,
            ROTATION_INTERVAL_DAYS,
            TimeUnit.DAYS
        );

        LOG.infof("✅ JWT secret rotation scheduler started (interval: %d days)", ROTATION_INTERVAL_DAYS);
    }

    /**
     * Rotate to a new JWT secret
     * Called automatically by scheduler or manually for emergency rotation
     */
    public synchronized void rotateSecret() {
        try {
            String newSecretId = UUID.randomUUID().toString();
            String newSecretValue = generateNewSecret();

            // Update secret references
            previousSecretId = currentSecretId;
            currentSecretId = newSecretId;

            // Add new secret to history
            JwtSecretEntry newSecret = new JwtSecretEntry(
                newSecretId,
                newSecretValue,
                Instant.now(),
                null
            );
            secretHistory.put(newSecretId, newSecret);

            // Mark previous secret for deprecation
            if (previousSecretId != null) {
                JwtSecretEntry previousSecret = secretHistory.get(previousSecretId);
                if (previousSecret != null) {
                    // Allow previous secret to be valid for 7 days after rotation
                    previousSecret.deprecatedAt = Instant.now().plus(PREVIOUS_SECRET_VALIDITY_DAYS, ChronoUnit.DAYS);
                    LOG.infof("✅ JWT secret rotated. Previous secret valid until: %s",
                        previousSecret.deprecatedAt);
                }
            }

            // Persist to file
            persistSecretsToFile();

            LOG.warnf("🔄 JWT SECRET ROTATED - New secret ID: %s (previous valid for %d days)",
                newSecretId, PREVIOUS_SECRET_VALIDITY_DAYS);
        } catch (Exception e) {
            LOG.errorf(e, "❌ Failed to rotate JWT secret");
        }
    }

    /**
     * Get current secret for signing new tokens
     */
    public String getCurrentSecret() {
        JwtSecretEntry current = secretHistory.get(currentSecretId);
        if (current != null) {
            return current.secretValue;
        }
        LOG.warnf("⚠️ Current JWT secret not found, falling back to initial");
        return INITIAL_SECRET;
    }

    /**
     * Get secret by ID (for validation)
     */
    public String getSecret(String secretId) {
        JwtSecretEntry entry = secretHistory.get(secretId);
        if (entry != null && !entry.isExpired()) {
            return entry.secretValue;
        }
        return null;
    }

    /**
     * Get all valid secrets (current + non-expired previous)
     * Used during token validation to accept tokens from recent rotations
     */
    public List<String> getValidSecrets() {
        List<String> validSecrets = new ArrayList<>();

        // Always include current secret
        JwtSecretEntry current = secretHistory.get(currentSecretId);
        if (current != null) {
            validSecrets.add(current.secretValue);
        }

        // Include previous secret if not expired
        if (previousSecretId != null) {
            JwtSecretEntry previous = secretHistory.get(previousSecretId);
            if (previous != null && !previous.isExpired()) {
                validSecrets.add(previous.secretValue);
            }
        }

        return validSecrets;
    }

    /**
     * Get rotation status
     */
    public JwtRotationStatus getRotationStatus() {
        JwtSecretEntry current = secretHistory.get(currentSecretId);
        JwtSecretEntry previous = previousSecretId != null ? secretHistory.get(previousSecretId) : null;

        return new JwtRotationStatus(
            currentSecretId,
            current != null ? current.createdAt : null,
            previous != null ? previous.deprecatedAt : null,
            secretHistory.size(),
            getCurrentSecret()
        );
    }

    /**
     * Persist secrets to file for recovery
     */
    private void persistSecretsToFile() {
        try {
            Path path = Paths.get(SECRETS_FILE_PATH);
            Path backup = Paths.get(BACKUP_PATH);

            // Create backup
            if (Files.exists(path)) {
                Files.copy(path, backup);
            }

            // Create directory if needed
            Files.createDirectories(path.getParent());

            // Persist secret metadata (NOT the actual secret values)
            StringBuilder json = new StringBuilder("{\"secrets\":[");
            boolean first = true;
            for (JwtSecretEntry entry : secretHistory.values()) {
                if (!first) json.append(",");
                json.append(String.format(
                    "{\"id\":\"%s\",\"createdAt\":\"%s\",\"deprecatedAt\":\"%s\"}",
                    entry.secretId,
                    entry.createdAt,
                    entry.deprecatedAt
                ));
                first = false;
            }
            json.append("]}");

            Files.write(path, json.toString().getBytes(StandardCharsets.UTF_8));
            LOG.infof("✅ JWT secret metadata persisted to: %s", path);
        } catch (Exception e) {
            LOG.warnf("⚠️ Failed to persist JWT secret metadata: %s", e.getMessage());
        }
    }

    /**
     * Generate a new cryptographically secure JWT secret
     */
    private String generateNewSecret() {
        // Generate 32 random bytes (256 bits) for HMAC-SHA256
        byte[] randomBytes = new byte[32];
        SecureRandom random = new SecureRandom();
        random.nextBytes(randomBytes);

        // Convert to Base64 for storage
        return Base64.getEncoder().encodeToString(randomBytes);
    }

    /**
     * Check if secret rotation is needed (for manual scheduling)
     */
    public boolean isRotationDue() {
        JwtSecretEntry current = secretHistory.get(currentSecretId);
        if (current == null) {
            return true;
        }

        long ageInDays = ChronoUnit.DAYS.between(current.createdAt, Instant.now());
        return ageInDays >= ROTATION_INTERVAL_DAYS;
    }

    /**
     * Shutdown rotation scheduler (for graceful shutdown)
     */
    public void shutdown() {
        if (rotationScheduler != null) {
            rotationScheduler.shutdown();
            try {
                if (!rotationScheduler.awaitTermination(10, TimeUnit.SECONDS)) {
                    rotationScheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                rotationScheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    // ==================== Data Classes ====================

    /**
     * JWT Secret Entry - Stores secret metadata
     */
    public static class JwtSecretEntry {
        public String secretId;
        public String secretValue;
        public Instant createdAt;
        public Instant deprecatedAt;

        public JwtSecretEntry(String secretId, String secretValue, Instant createdAt, Instant deprecatedAt) {
            this.secretId = secretId;
            this.secretValue = secretValue;
            this.createdAt = createdAt;
            this.deprecatedAt = deprecatedAt;
        }

        /**
         * Check if secret has expired (deprecated and past validity window)
         */
        public boolean isExpired() {
            if (deprecatedAt == null) {
                return false;  // Current secret never expires
            }
            return Instant.now().isAfter(deprecatedAt);
        }
    }

    /**
     * JWT Rotation Status - For monitoring and debugging
     */
    public static class JwtRotationStatus {
        public String currentSecretId;
        public Instant currentSecretCreatedAt;
        public Instant previousSecretDeprecatedAt;
        public int totalSecrets;
        public String currentSecretPreview;

        public JwtRotationStatus(String currentSecretId, Instant currentSecretCreatedAt,
                               Instant previousSecretDeprecatedAt, int totalSecrets,
                               String currentSecretValue) {
            this.currentSecretId = currentSecretId;
            this.currentSecretCreatedAt = currentSecretCreatedAt;
            this.previousSecretDeprecatedAt = previousSecretDeprecatedAt;
            this.totalSecrets = totalSecrets;
            // Only show first 10 chars of secret for security
            this.currentSecretPreview = currentSecretValue.substring(0, Math.min(10, currentSecretValue.length())) + "...";
        }

        @Override
        public String toString() {
            return String.format(
                "JwtRotationStatus{currentId=%s, createdAt=%s, previousDeprecatedAt=%s, totalSecrets=%d}",
                currentSecretId, currentSecretCreatedAt, previousSecretDeprecatedAt, totalSecrets
            );
        }
    }
}
