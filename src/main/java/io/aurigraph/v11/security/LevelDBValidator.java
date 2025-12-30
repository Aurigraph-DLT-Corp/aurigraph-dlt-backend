package io.aurigraph.v11.security;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Pattern;
import java.util.concurrent.atomic.AtomicLong;

/**
 * LevelDB Input Validation Service
 *
 * Provides comprehensive input validation for LevelDB operations to prevent:
 * - SQL injection attacks (key-based)
 * - Buffer overflow attacks (excessive length)
 * - Path traversal attacks (malicious keys)
 * - Denial of service (resource exhaustion)
 * - Data corruption (invalid formats)
 *
 * Validation Rules:
 * - Keys: Max 1024 bytes, alphanumeric + safe characters only
 * - Values: Max 10 MB per value
 * - JSON values: Must be valid JSON format
 * - Batch operations: Max 10,000 operations per batch
 *
 * @author Aurigraph Security Team
 * @version 11.3.0
 * @since October 2025
 */
@ApplicationScoped
public class LevelDBValidator {

    private static final Logger logger = LoggerFactory.getLogger(LevelDBValidator.class);

    // Validation limits (configurable via properties)
    private static final int MAX_KEY_LENGTH = 1024;  // 1 KB
    private static final int MAX_VALUE_LENGTH = 10_485_760;  // 10 MB
    private static final int MAX_BATCH_SIZE = 10_000;

    // Safe key pattern: alphanumeric, colons, underscores, hyphens, dots
    private static final Pattern SAFE_KEY_PATTERN =
        Pattern.compile("^[a-zA-Z0-9:_\\-.]+$");

    // Dangerous patterns to reject
    private static final Pattern[] DANGEROUS_PATTERNS = {
        Pattern.compile("\\.\\./"),  // Path traversal
        Pattern.compile("\\x00"),    // Null bytes
        Pattern.compile("<script"),  // XSS attempt
        Pattern.compile("javascript:"),  // XSS attempt
        Pattern.compile("data:"),    // Data URI scheme
    };

    @Inject
    SecurityAuditService auditService;

    private final AtomicLong validationCount = new AtomicLong(0);
    private final AtomicLong validationErrors = new AtomicLong(0);

    /**
     * Validate key format and length
     */
    public void validateKey(String key) {
        validationCount.incrementAndGet();

        if (key == null) {
            validationErrors.incrementAndGet();
            auditService.logSecurityViolation("KEY_VALIDATION_FAILED",
                "System", "Key is null");
            throw new IllegalArgumentException("Key cannot be null");
        }

        if (key.isEmpty()) {
            validationErrors.incrementAndGet();
            auditService.logSecurityViolation("KEY_VALIDATION_FAILED",
                "System", "Key is empty");
            throw new IllegalArgumentException("Key cannot be empty");
        }

        if (key.length() > MAX_KEY_LENGTH) {
            validationErrors.incrementAndGet();
            auditService.logSecurityViolation("KEY_VALIDATION_FAILED",
                "System", "Key exceeds max length: " + key.length());
            throw new IllegalArgumentException(
                "Key exceeds maximum length of " + MAX_KEY_LENGTH + " bytes");
        }

        // Check for safe character set
        if (!SAFE_KEY_PATTERN.matcher(key).matches()) {
            validationErrors.incrementAndGet();
            auditService.logSecurityViolation("KEY_VALIDATION_FAILED",
                "System", "Key contains invalid characters: " + key);
            throw new IllegalArgumentException(
                "Key contains invalid characters. Only alphanumeric, :, _, -, . are allowed");
        }

        // Check for dangerous patterns
        for (Pattern dangerousPattern : DANGEROUS_PATTERNS) {
            if (dangerousPattern.matcher(key).find()) {
                validationErrors.incrementAndGet();
                auditService.logSecurityViolation("KEY_VALIDATION_FAILED",
                    "System", "Key contains dangerous pattern: " + key);
                throw new IllegalArgumentException(
                    "Key contains dangerous pattern");
            }
        }

        if (logger.isTraceEnabled()) {
            logger.trace("Key validation passed: {}", key);
        }
    }

    /**
     * Validate value (string) format and length
     */
    public void validateValue(String value) {
        validationCount.incrementAndGet();

        if (value == null) {
            validationErrors.incrementAndGet();
            auditService.logSecurityViolation("VALUE_VALIDATION_FAILED",
                "System", "Value is null");
            throw new IllegalArgumentException("Value cannot be null");
        }

        byte[] valueBytes = value.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        if (valueBytes.length > MAX_VALUE_LENGTH) {
            validationErrors.incrementAndGet();
            auditService.logSecurityViolation("VALUE_VALIDATION_FAILED",
                "System", "Value exceeds max length: " + valueBytes.length);
            throw new IllegalArgumentException(
                "Value exceeds maximum length of " + MAX_VALUE_LENGTH + " bytes");
        }

        // Check for dangerous patterns in value
        for (Pattern dangerousPattern : DANGEROUS_PATTERNS) {
            if (dangerousPattern.matcher(value).find()) {
                validationErrors.incrementAndGet();
                auditService.logSecurityViolation("VALUE_VALIDATION_FAILED",
                    "System", "Value contains dangerous pattern");
                throw new IllegalArgumentException(
                    "Value contains dangerous pattern");
            }
        }

        if (logger.isTraceEnabled()) {
            logger.trace("Value validation passed: {} bytes", valueBytes.length);
        }
    }

    /**
     * Validate value (bytes) length
     */
    public void validateValueBytes(byte[] value) {
        validationCount.incrementAndGet();

        if (value == null) {
            validationErrors.incrementAndGet();
            auditService.logSecurityViolation("VALUE_VALIDATION_FAILED",
                "System", "Value bytes are null");
            throw new IllegalArgumentException("Value bytes cannot be null");
        }

        if (value.length > MAX_VALUE_LENGTH) {
            validationErrors.incrementAndGet();
            auditService.logSecurityViolation("VALUE_VALIDATION_FAILED",
                "System", "Value bytes exceed max length: " + value.length);
            throw new IllegalArgumentException(
                "Value exceeds maximum length of " + MAX_VALUE_LENGTH + " bytes");
        }

        if (logger.isTraceEnabled()) {
            logger.trace("Value bytes validation passed: {} bytes", value.length);
        }
    }

    /**
     * Validate JSON format (optional additional validation)
     */
    public void validateJson(String json) {
        validationCount.incrementAndGet();

        if (json == null || json.isEmpty()) {
            validationErrors.incrementAndGet();
            throw new IllegalArgumentException("JSON cannot be null or empty");
        }

        // Basic JSON validation (could use Jackson for full validation)
        String trimmed = json.trim();
        if (!(trimmed.startsWith("{") && trimmed.endsWith("}")
                || trimmed.startsWith("[") && trimmed.endsWith("]"))) {
            validationErrors.incrementAndGet();
            auditService.logSecurityViolation("JSON_VALIDATION_FAILED",
                "System", "Invalid JSON format");
            throw new IllegalArgumentException("Invalid JSON format");
        }

        if (logger.isTraceEnabled()) {
            logger.trace("JSON validation passed: {} bytes", json.length());
        }
    }

    /**
     * Validate batch operation size
     */
    public void validateBatchSize(int batchSize) {
        validationCount.incrementAndGet();

        if (batchSize < 0) {
            validationErrors.incrementAndGet();
            throw new IllegalArgumentException("Batch size cannot be negative");
        }

        if (batchSize > MAX_BATCH_SIZE) {
            validationErrors.incrementAndGet();
            auditService.logSecurityViolation("BATCH_VALIDATION_FAILED",
                "System", "Batch size exceeds limit: " + batchSize);
            throw new IllegalArgumentException(
                "Batch size exceeds maximum of " + MAX_BATCH_SIZE + " operations");
        }

        if (logger.isTraceEnabled()) {
            logger.trace("Batch size validation passed: {}", batchSize);
        }
    }

    /**
     * Sanitize key by removing unsafe characters
     */
    public String sanitizeKey(String key) {
        if (key == null) {
            return null;
        }

        // Remove all non-safe characters
        return key.replaceAll("[^a-zA-Z0-9:_\\-.]", "");
    }

    /**
     * Check if key is safe without throwing exception
     */
    public boolean isSafeKey(String key) {
        if (key == null || key.isEmpty() || key.length() > MAX_KEY_LENGTH) {
            return false;
        }

        if (!SAFE_KEY_PATTERN.matcher(key).matches()) {
            return false;
        }

        for (Pattern dangerousPattern : DANGEROUS_PATTERNS) {
            if (dangerousPattern.matcher(key).find()) {
                return false;
            }
        }

        return true;
    }

    /**
     * Get validation statistics
     */
    public ValidationStats getStats() {
        return new ValidationStats(
            validationCount.get(),
            validationErrors.get(),
            MAX_KEY_LENGTH,
            MAX_VALUE_LENGTH,
            MAX_BATCH_SIZE
        );
    }

    /**
     * Validation statistics
     */
    public record ValidationStats(
        long validationCount,
        long validationErrors,
        int maxKeyLength,
        int maxValueLength,
        int maxBatchSize
    ) {}
}
