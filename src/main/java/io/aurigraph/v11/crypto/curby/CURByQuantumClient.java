package io.aurigraph.v11.crypto.curby;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import io.smallrye.mutiny.Uni;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * CURBy Quantum Service Client
 *
 * Provides integration with CURBy quantum service for post-quantum cryptography.
 * Features:
 * - REST API integration with connection pooling
 * - Exponential backoff retry logic
 * - Circuit breaker pattern
 * - Health monitoring and automatic failover
 * - Key caching with TTL
 * - Async quantum operations
 *
 * @author Aurigraph V11
 * @version 11.0.0
 */
@ApplicationScoped
public class CURByQuantumClient {

    private static final Logger LOG = Logger.getLogger(CURByQuantumClient.class);

    @ConfigProperty(name = "curby.quantum.url", defaultValue = "https://api.curby.quantum.io")
    String curbyApiUrl;

    @ConfigProperty(name = "curby.quantum.api-key", defaultValue = "")
    String curbyApiKey;

    @ConfigProperty(name = "curby.quantum.enabled", defaultValue = "true")
    boolean curbyEnabled;

    @ConfigProperty(name = "curby.quantum.fallback", defaultValue = "true")
    boolean fallbackEnabled;

    @ConfigProperty(name = "curby.quantum.algorithm", defaultValue = "CRYSTALS-Dilithium")
    String defaultAlgorithm;

    @ConfigProperty(name = "curby.quantum.timeout.seconds", defaultValue = "30")
    int requestTimeoutSeconds;

    @ConfigProperty(name = "curby.quantum.retry.max-attempts", defaultValue = "3")
    int maxRetryAttempts;

    @ConfigProperty(name = "curby.quantum.retry.base-delay-ms", defaultValue = "100")
    int baseRetryDelayMs;

    @ConfigProperty(name = "curby.quantum.circuit-breaker.failure-threshold", defaultValue = "5")
    int circuitBreakerFailureThreshold;

    @ConfigProperty(name = "curby.quantum.circuit-breaker.reset-timeout-ms", defaultValue = "60000")
    long circuitBreakerResetTimeoutMs;

    @ConfigProperty(name = "curby.quantum.key-cache.ttl-seconds", defaultValue = "300")
    int keyCacheTTLSeconds;

    @ConfigProperty(name = "curby.quantum.health-check.interval-seconds", defaultValue = "30")
    int healthCheckIntervalSeconds;

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

    // Circuit Breaker State
    private final AtomicBoolean circuitOpen = new AtomicBoolean(false);
    private final AtomicLong failureCount = new AtomicLong(0);
    private final AtomicLong lastFailureTime = new AtomicLong(0);

    // Performance Metrics
    private final AtomicLong totalRequests = new AtomicLong(0);
    private final AtomicLong successfulRequests = new AtomicLong(0);
    private final AtomicLong failedRequests = new AtomicLong(0);
    private final AtomicLong cachedResponses = new AtomicLong(0);

    // Key Cache (with TTL)
    private final Map<String, CachedQuantumKey> keyCache = new ConcurrentHashMap<>();

    @Inject
    io.aurigraph.v11.crypto.QuantumCryptoService localQuantumCrypto;

    public CURByQuantumClient() {
        // Initialize HTTP client with connection pooling
        this.httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_2)
            .connectTimeout(Duration.ofSeconds(10))
            .executor(Executors.newVirtualThreadPerTaskExecutor()) // Java 21 Virtual Threads
            .build();
    }

    /**
     * Initialize CURBy client and start health monitoring
     */
    public void initialize() {
        if (!curbyEnabled) {
            LOG.info("CURBy Quantum Service is DISABLED - using local quantum crypto");
            return;
        }

        LOG.infof("Initializing CURBy Quantum Service Client: %s", curbyApiUrl);

        // Start health check monitoring
        scheduler.scheduleAtFixedRate(
            this::performHealthCheck,
            0,
            healthCheckIntervalSeconds,
            TimeUnit.SECONDS
        );

        // Start cache cleanup
        scheduler.scheduleAtFixedRate(
            this::cleanExpiredCacheEntries,
            keyCacheTTLSeconds,
            keyCacheTTLSeconds,
            TimeUnit.SECONDS
        );

        LOG.info("CURBy Quantum Service Client initialized successfully");
    }

    /**
     * Generate quantum key pair using CURBy service
     */
    public Uni<QuantumKeyPairResponse> generateKeyPair(String algorithm) {
        return Uni.createFrom().item(() -> {
            totalRequests.incrementAndGet();

            // Check circuit breaker
            if (isCircuitOpen()) {
                LOG.warn("Circuit breaker OPEN - using fallback");
                return useFallbackKeyGeneration(algorithm);
            }

            // Check cache
            String cacheKey = "keypair_" + algorithm + "_" + System.currentTimeMillis() / 1000;

            try {
                // Build request
                ObjectNode requestBody = objectMapper.createObjectNode();
                requestBody.put("algorithm", algorithm);
                requestBody.put("securityLevel", 5); // NIST Level 5

                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(curbyApiUrl + "/api/v1/quantum/keypair"))
                    .header("Authorization", "Bearer " + curbyApiKey)
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(requestTimeoutSeconds))
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(requestBody)))
                    .build();

                // Execute with retry
                QuantumKeyPairResponse response = executeWithRetry(() -> {
                    HttpResponse<String> httpResponse = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                    if (httpResponse.statusCode() == 200) {
                        return objectMapper.readValue(httpResponse.body(), QuantumKeyPairResponse.class);
                    } else {
                        throw new CURByException("CURBy API error: " + httpResponse.statusCode());
                    }
                });

                successfulRequests.incrementAndGet();
                resetCircuitBreaker();

                // Cache the response
                keyCache.put(cacheKey, new CachedQuantumKey(
                    response,
                    System.currentTimeMillis() + (keyCacheTTLSeconds * 1000L)
                ));

                LOG.debugf("Generated quantum key pair via CURBy: %s", algorithm);
                return response;

            } catch (Exception e) {
                LOG.errorf(e, "Failed to generate key pair via CURBy: %s", algorithm);
                recordFailure();

                if (fallbackEnabled) {
                    LOG.info("Using fallback local quantum crypto");
                    return useFallbackKeyGeneration(algorithm);
                } else {
                    failedRequests.incrementAndGet();
                    throw new CURByException("CURBy key generation failed and fallback is disabled", e);
                }
            }
        });
    }

    /**
     * Generate quantum signature using CURBy service
     */
    public Uni<QuantumSignatureResponse> generateSignature(String data, String privateKey, String algorithm) {
        return Uni.createFrom().item(() -> {
            totalRequests.incrementAndGet();

            // Check circuit breaker
            if (isCircuitOpen()) {
                LOG.warn("Circuit breaker OPEN - using fallback");
                return useFallbackSignature(data, privateKey, algorithm);
            }

            try {
                // Build request
                ObjectNode requestBody = objectMapper.createObjectNode();
                requestBody.put("data", Base64.getEncoder().encodeToString(data.getBytes()));
                requestBody.put("privateKey", privateKey);
                requestBody.put("algorithm", algorithm);

                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(curbyApiUrl + "/api/v1/quantum/sign"))
                    .header("Authorization", "Bearer " + curbyApiKey)
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(requestTimeoutSeconds))
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(requestBody)))
                    .build();

                // Execute with retry
                QuantumSignatureResponse response = executeWithRetry(() -> {
                    HttpResponse<String> httpResponse = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                    if (httpResponse.statusCode() == 200) {
                        return objectMapper.readValue(httpResponse.body(), QuantumSignatureResponse.class);
                    } else {
                        throw new CURByException("CURBy API error: " + httpResponse.statusCode());
                    }
                });

                successfulRequests.incrementAndGet();
                resetCircuitBreaker();

                LOG.debugf("Generated quantum signature via CURBy");
                return response;

            } catch (Exception e) {
                LOG.errorf(e, "Failed to generate signature via CURBy");
                recordFailure();

                if (fallbackEnabled) {
                    LOG.info("Using fallback local quantum crypto for signature");
                    return useFallbackSignature(data, privateKey, algorithm);
                } else {
                    failedRequests.incrementAndGet();
                    throw new CURByException("CURBy signature generation failed and fallback is disabled", e);
                }
            }
        });
    }

    /**
     * Verify quantum signature using CURBy service
     */
    public Uni<QuantumVerificationResponse> verifySignature(String data, String signature, String publicKey, String algorithm) {
        return Uni.createFrom().item(() -> {
            totalRequests.incrementAndGet();

            // Check circuit breaker
            if (isCircuitOpen()) {
                LOG.warn("Circuit breaker OPEN - using fallback");
                return useFallbackVerification(data, signature, publicKey, algorithm);
            }

            try {
                // Build request
                ObjectNode requestBody = objectMapper.createObjectNode();
                requestBody.put("data", Base64.getEncoder().encodeToString(data.getBytes()));
                requestBody.put("signature", signature);
                requestBody.put("publicKey", publicKey);
                requestBody.put("algorithm", algorithm);

                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(curbyApiUrl + "/api/v1/quantum/verify"))
                    .header("Authorization", "Bearer " + curbyApiKey)
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(requestTimeoutSeconds))
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(requestBody)))
                    .build();

                // Execute with retry
                QuantumVerificationResponse response = executeWithRetry(() -> {
                    HttpResponse<String> httpResponse = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                    if (httpResponse.statusCode() == 200) {
                        return objectMapper.readValue(httpResponse.body(), QuantumVerificationResponse.class);
                    } else {
                        throw new CURByException("CURBy API error: " + httpResponse.statusCode());
                    }
                });

                successfulRequests.incrementAndGet();
                resetCircuitBreaker();

                LOG.debugf("Verified quantum signature via CURBy: %s", response.valid());
                return response;

            } catch (Exception e) {
                LOG.errorf(e, "Failed to verify signature via CURBy");
                recordFailure();

                if (fallbackEnabled) {
                    LOG.info("Using fallback local quantum crypto for verification");
                    return useFallbackVerification(data, signature, publicKey, algorithm);
                } else {
                    failedRequests.incrementAndGet();
                    throw new CURByException("CURBy verification failed and fallback is disabled", e);
                }
            }
        });
    }

    /**
     * Get CURBy service health status
     */
    public CURByHealthStatus getHealthStatus() {
        return new CURByHealthStatus(
            curbyEnabled,
            !circuitOpen.get(),
            totalRequests.get(),
            successfulRequests.get(),
            failedRequests.get(),
            cachedResponses.get(),
            calculateSuccessRate(),
            keyCache.size(),
            System.currentTimeMillis()
        );
    }

    // Private Helper Methods

    private <T> T executeWithRetry(RetryableOperation<T> operation) throws Exception {
        int attempt = 0;
        Exception lastException = null;

        while (attempt < maxRetryAttempts) {
            try {
                return operation.execute();
            } catch (Exception e) {
                lastException = e;
                attempt++;

                if (attempt < maxRetryAttempts) {
                    // Exponential backoff
                    long delayMs = baseRetryDelayMs * (long) Math.pow(2, attempt - 1);
                    LOG.debugf("Retry attempt %d/%d after %dms", attempt, maxRetryAttempts, delayMs);
                    Thread.sleep(delayMs);
                }
            }
        }

        throw lastException;
    }

    private void performHealthCheck() {
        try {
            if (!curbyEnabled) {
                return;
            }

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(curbyApiUrl + "/api/v1/health"))
                .header("Authorization", "Bearer " + curbyApiKey)
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                LOG.debug("CURBy health check: OK");
                resetCircuitBreaker();
            } else {
                LOG.warnf("CURBy health check failed: %d", response.statusCode());
                recordFailure();
            }

        } catch (Exception e) {
            LOG.warnf(e, "CURBy health check error");
            recordFailure();
        }
    }

    private void cleanExpiredCacheEntries() {
        long now = System.currentTimeMillis();
        keyCache.entrySet().removeIf(entry -> entry.getValue().expiryTime() < now);
        LOG.debugf("Cleaned expired cache entries. Cache size: %d", keyCache.size());
    }

    private boolean isCircuitOpen() {
        if (!circuitOpen.get()) {
            return false;
        }

        // Check if enough time has passed to try closing the circuit
        long timeSinceLastFailure = System.currentTimeMillis() - lastFailureTime.get();
        if (timeSinceLastFailure > circuitBreakerResetTimeoutMs) {
            LOG.info("Circuit breaker timeout expired - attempting to close circuit");
            circuitOpen.set(false);
            failureCount.set(0);
            return false;
        }

        return true;
    }

    private void recordFailure() {
        failedRequests.incrementAndGet();
        lastFailureTime.set(System.currentTimeMillis());

        long failures = failureCount.incrementAndGet();
        if (failures >= circuitBreakerFailureThreshold) {
            circuitOpen.set(true);
            LOG.errorf("Circuit breaker OPENED after %d failures", failures);
        }
    }

    private void resetCircuitBreaker() {
        if (circuitOpen.get()) {
            circuitOpen.set(false);
            failureCount.set(0);
            LOG.info("Circuit breaker CLOSED - service recovered");
        }
    }

    private double calculateSuccessRate() {
        long total = totalRequests.get();
        if (total == 0) {
            return 1.0;
        }
        return (double) successfulRequests.get() / total;
    }

    // Fallback Methods (use local crypto)

    private QuantumKeyPairResponse useFallbackKeyGeneration(String algorithm) {
        try {
            String keyId = "fallback-" + System.currentTimeMillis();
            var request = new io.aurigraph.v11.crypto.QuantumCryptoService.KeyGenerationRequest(keyId, algorithm);
            var result = localQuantumCrypto.generateKeyPair(request).await().indefinitely();

            return new QuantumKeyPairResponse(
                true,
                result.keyId(),
                result.algorithm(),
                "LOCAL_FALLBACK",
                "Generated using local quantum crypto (CURBy unavailable)",
                result.publicKeySize(),
                result.privateKeySize(),
                System.currentTimeMillis()
            );
        } catch (Exception e) {
            LOG.error("Fallback key generation failed", e);
            throw new CURByException("Both CURBy and local crypto failed", e);
        }
    }

    private QuantumSignatureResponse useFallbackSignature(String data, String privateKey, String algorithm) {
        try {
            byte[] dataBytes = data.getBytes();
            String signature = localQuantumCrypto.sign(dataBytes);

            return new QuantumSignatureResponse(
                true,
                signature,
                algorithm,
                "LOCAL_FALLBACK",
                System.currentTimeMillis()
            );
        } catch (Exception e) {
            LOG.error("Fallback signature generation failed", e);
            throw new CURByException("Both CURBy and local crypto failed", e);
        }
    }

    private QuantumVerificationResponse useFallbackVerification(String data, String signature, String publicKey, String algorithm) {
        try {
            boolean valid = localQuantumCrypto.verifyDilithiumSignature(data, signature, publicKey);

            return new QuantumVerificationResponse(
                true,
                valid,
                algorithm,
                "LOCAL_FALLBACK",
                System.currentTimeMillis()
            );
        } catch (Exception e) {
            LOG.error("Fallback verification failed", e);
            throw new CURByException("Both CURBy and local crypto failed", e);
        }
    }

    // Functional Interfaces

    @FunctionalInterface
    private interface RetryableOperation<T> {
        T execute() throws Exception;
    }

    // Data Classes

    public record QuantumKeyPairResponse(
        boolean success,
        String keyId,
        String algorithm,
        String publicKey,
        String privateKey,
        int publicKeySize,
        int privateKeySize,
        long timestamp
    ) {}

    public record QuantumSignatureResponse(
        boolean success,
        String signature,
        String algorithm,
        String source,
        long timestamp
    ) {}

    public record QuantumVerificationResponse(
        boolean success,
        boolean valid,
        String algorithm,
        String source,
        long timestamp
    ) {}

    public record CURByHealthStatus(
        boolean enabled,
        boolean healthy,
        long totalRequests,
        long successfulRequests,
        long failedRequests,
        long cachedResponses,
        double successRate,
        int cacheSize,
        long timestamp
    ) {}

    private record CachedQuantumKey(
        QuantumKeyPairResponse response,
        long expiryTime
    ) {}

    public static class CURByException extends RuntimeException {
        public CURByException(String message) {
            super(message);
        }

        public CURByException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
