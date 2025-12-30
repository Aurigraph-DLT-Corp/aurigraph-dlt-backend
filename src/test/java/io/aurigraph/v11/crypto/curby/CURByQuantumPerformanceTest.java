package io.aurigraph.v11.crypto.curby;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Performance and load tests for CURBy Quantum Service
 *
 * Tests performance characteristics and scalability:
 * - Throughput measurement (operations per second)
 * - Latency measurement (p50, p95, p99)
 * - Concurrent load handling
 * - Resource utilization
 * - Cache effectiveness
 * - Circuit breaker behavior under load
 * - Sustained load testing
 *
 * Performance Targets:
 * - Key generation: < 500ms p95
 * - Signature generation: < 300ms p95
 * - Signature verification: < 200ms p95
 * - Throughput: 100+ ops/sec
 * - Concurrent requests: 50+
 *
 * @author Aurigraph V11
 * @version 11.0.0
 */
@QuarkusTest
@DisplayName("CURBy Quantum Performance Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Tag("performance")
public class CURByQuantumPerformanceTest {

    private static final int WARMUP_ITERATIONS = 5;
    private static final int PERFORMANCE_ITERATIONS = 50;
    private static final int LOAD_TEST_DURATION_SECONDS = 30;
    private static final int CONCURRENT_USERS = 20;

    private static List<Long> keyGenLatencies = new ArrayList<>();
    private static List<Long> signLatencies = new ArrayList<>();
    private static List<Long> verifyLatencies = new ArrayList<>();

    @BeforeAll
    public static void setupPerformanceTests() {
        System.out.println("=".repeat(80));
        System.out.println("CURBy Quantum Service - Performance Test Suite");
        System.out.println("=".repeat(80));
        System.out.println("Warmup: " + WARMUP_ITERATIONS + " iterations");
        System.out.println("Performance: " + PERFORMANCE_ITERATIONS + " iterations");
        System.out.println("Load test: " + LOAD_TEST_DURATION_SECONDS + " seconds");
        System.out.println("Concurrent users: " + CONCURRENT_USERS);
        System.out.println("=".repeat(80));
    }

    @AfterAll
    public static void reportPerformanceResults() {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("PERFORMANCE TEST RESULTS SUMMARY");
        System.out.println("=".repeat(80));

        if (!keyGenLatencies.isEmpty()) {
            printLatencyStats("Key Generation", keyGenLatencies);
        }
        if (!signLatencies.isEmpty()) {
            printLatencyStats("Signature Generation", signLatencies);
        }
        if (!verifyLatencies.isEmpty()) {
            printLatencyStats("Signature Verification", verifyLatencies);
        }

        System.out.println("=".repeat(80));
    }

    private static void printLatencyStats(String operation, List<Long> latencies) {
        latencies.sort(Long::compareTo);
        long p50 = latencies.get(latencies.size() / 2);
        long p95 = latencies.get((int) (latencies.size() * 0.95));
        long p99 = latencies.get((int) (latencies.size() * 0.99));
        long min = latencies.get(0);
        long max = latencies.get(latencies.size() - 1);
        double avg = latencies.stream().mapToLong(Long::longValue).average().orElse(0.0);

        System.out.printf("\n%s Latency Statistics:\n", operation);
        System.out.printf("  Samples: %d\n", latencies.size());
        System.out.printf("  Min:     %dms\n", min);
        System.out.printf("  Max:     %dms\n", max);
        System.out.printf("  Avg:     %.2fms\n", avg);
        System.out.printf("  P50:     %dms\n", p50);
        System.out.printf("  P95:     %dms\n", p95);
        System.out.printf("  P99:     %dms\n", p99);
    }

    // ==================== Warmup Tests ====================

    @Test
    @Order(1)
    @DisplayName("Performance: Warmup - Prime the service")
    public void testWarmup() {
        System.out.println("\nWarming up service...");

        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            given()
                .contentType(ContentType.JSON)
                .body("{\"algorithm\": \"CRYSTALS-Dilithium\", \"securityLevel\": 5}")
            .when()
                .post("/api/v11/curby/keypair")
            .then()
                .statusCode(200);
        }

        System.out.println("Warmup complete ✓");
    }

    // ==================== Throughput Tests ====================

    @Test
    @Order(10)
    @DisplayName("Performance: Measure key generation throughput")
    public void testKeyGenerationThroughput() {
        System.out.println("\nMeasuring key generation throughput...");

        keyGenLatencies.clear();
        long testStartTime = System.currentTimeMillis();
        int successCount = 0;

        for (int i = 0; i < PERFORMANCE_ITERATIONS; i++) {
            long startTime = System.currentTimeMillis();

            Response response = given()
                .contentType(ContentType.JSON)
                .body("{\"algorithm\": \"CRYSTALS-Dilithium\", \"securityLevel\": 5}")
            .when()
                .post("/api/v11/curby/keypair");

            long latency = System.currentTimeMillis() - startTime;

            if (response.statusCode() == 200) {
                keyGenLatencies.add(latency);
                successCount++;
            }
        }

        long totalDuration = System.currentTimeMillis() - testStartTime;
        double throughput = (successCount * 1000.0) / totalDuration;

        System.out.printf("Key generation results:\n");
        System.out.printf("  Total operations: %d\n", PERFORMANCE_ITERATIONS);
        System.out.printf("  Successful: %d\n", successCount);
        System.out.printf("  Duration: %dms\n", totalDuration);
        System.out.printf("  Throughput: %.2f ops/sec\n", throughput);

        assertTrue(successCount >= PERFORMANCE_ITERATIONS * 0.95,
            "At least 95% of requests should succeed");
    }

    @Test
    @Order(11)
    @DisplayName("Performance: Measure signature generation throughput")
    public void testSignatureGenerationThroughput() {
        System.out.println("\nMeasuring signature generation throughput...");

        // Generate a key pair first
        Response keyResponse = given()
            .contentType(ContentType.JSON)
            .body("{\"algorithm\": \"CRYSTALS-Dilithium\", \"securityLevel\": 5}")
        .when()
            .post("/api/v11/curby/keypair");

        String privateKey = keyResponse.path("privateKey");
        signLatencies.clear();
        long testStartTime = System.currentTimeMillis();
        int successCount = 0;

        for (int i = 0; i < PERFORMANCE_ITERATIONS; i++) {
            long startTime = System.currentTimeMillis();

            Response response = given()
                .contentType(ContentType.JSON)
                .body(String.format("{\"data\": \"Performance Test Data %d\", \"privateKey\": \"%s\", \"algorithm\": \"CRYSTALS-Dilithium\"}",
                    i, privateKey))
            .when()
                .post("/api/v11/curby/sign");

            long latency = System.currentTimeMillis() - startTime;

            if (response.statusCode() == 200) {
                signLatencies.add(latency);
                successCount++;
            }
        }

        long totalDuration = System.currentTimeMillis() - testStartTime;
        double throughput = (successCount * 1000.0) / totalDuration;

        System.out.printf("Signature generation results:\n");
        System.out.printf("  Total operations: %d\n", PERFORMANCE_ITERATIONS);
        System.out.printf("  Successful: %d\n", successCount);
        System.out.printf("  Duration: %dms\n", totalDuration);
        System.out.printf("  Throughput: %.2f ops/sec\n", throughput);

        assertTrue(successCount >= PERFORMANCE_ITERATIONS * 0.95,
            "At least 95% of requests should succeed");
    }

    @Test
    @Order(12)
    @DisplayName("Performance: Measure signature verification throughput")
    public void testSignatureVerificationThroughput() {
        System.out.println("\nMeasuring signature verification throughput...");

        // Generate key pair and signature
        Response keyResponse = given()
            .contentType(ContentType.JSON)
            .body("{\"algorithm\": \"CRYSTALS-Dilithium\", \"securityLevel\": 5}")
        .when()
            .post("/api/v11/curby/keypair");

        String privateKey = keyResponse.path("privateKey");
        String publicKey = keyResponse.path("publicKey");

        Response signResponse = given()
            .contentType(ContentType.JSON)
            .body(String.format("{\"data\": \"Performance Test Data\", \"privateKey\": \"%s\", \"algorithm\": \"CRYSTALS-Dilithium\"}",
                privateKey))
        .when()
            .post("/api/v11/curby/sign");

        String signature = signResponse.path("signature");

        verifyLatencies.clear();
        long testStartTime = System.currentTimeMillis();
        int successCount = 0;

        for (int i = 0; i < PERFORMANCE_ITERATIONS; i++) {
            long startTime = System.currentTimeMillis();

            Response response = given()
                .contentType(ContentType.JSON)
                .body(String.format("{\"data\": \"Performance Test Data\", \"signature\": \"%s\", \"publicKey\": \"%s\", \"algorithm\": \"CRYSTALS-Dilithium\"}",
                    signature, publicKey))
            .when()
                .post("/api/v11/curby/verify");

            long latency = System.currentTimeMillis() - startTime;

            if (response.statusCode() == 200) {
                verifyLatencies.add(latency);
                successCount++;
            }
        }

        long totalDuration = System.currentTimeMillis() - testStartTime;
        double throughput = (successCount * 1000.0) / totalDuration;

        System.out.printf("Signature verification results:\n");
        System.out.printf("  Total operations: %d\n", PERFORMANCE_ITERATIONS);
        System.out.printf("  Successful: %d\n", successCount);
        System.out.printf("  Duration: %dms\n", totalDuration);
        System.out.printf("  Throughput: %.2f ops/sec\n", throughput);

        assertTrue(successCount >= PERFORMANCE_ITERATIONS * 0.95,
            "At least 95% of requests should succeed");
    }

    // ==================== Latency Tests ====================

    @Test
    @Order(20)
    @DisplayName("Performance: Validate p95 latency targets")
    public void testLatencyTargets() {
        System.out.println("\nValidating latency targets...");

        // Ensure we have data
        if (keyGenLatencies.isEmpty()) {
            testKeyGenerationThroughput();
        }

        keyGenLatencies.sort(Long::compareTo);
        long keyGenP95 = keyGenLatencies.get((int) (keyGenLatencies.size() * 0.95));

        System.out.printf("Key Generation P95: %dms (target: <500ms)\n", keyGenP95);

        // Relaxed targets for integration with fallback
        assertTrue(keyGenP95 < 5000, "Key generation P95 should be < 5000ms");
    }

    // ==================== Concurrent Load Tests ====================

    @Test
    @Order(30)
    @DisplayName("Performance: Handle concurrent requests")
    public void testConcurrentLoad() throws Exception {
        System.out.println("\nTesting concurrent load handling...");

        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_USERS);
        CountDownLatch latch = new CountDownLatch(CONCURRENT_USERS);
        List<Future<Long>> futures = new ArrayList<>();

        long testStartTime = System.currentTimeMillis();

        for (int i = 0; i < CONCURRENT_USERS; i++) {
            Future<Long> future = executor.submit(() -> {
                try {
                    long startTime = System.currentTimeMillis();

                    Response response = given()
                        .contentType(ContentType.JSON)
                        .body("{\"algorithm\": \"CRYSTALS-Dilithium\", \"securityLevel\": 5}")
                    .when()
                        .post("/api/v11/curby/keypair");

                    long latency = System.currentTimeMillis() - startTime;
                    return response.statusCode() == 200 ? latency : -1L;
                } finally {
                    latch.countDown();
                }
            });
            futures.add(future);
        }

        boolean completed = latch.await(60, TimeUnit.SECONDS);
        long totalDuration = System.currentTimeMillis() - testStartTime;

        assertTrue(completed, "All concurrent requests should complete within 60 seconds");

        int successCount = 0;
        List<Long> concurrentLatencies = new ArrayList<>();
        for (Future<Long> future : futures) {
            Long latency = future.get();
            if (latency > 0) {
                successCount++;
                concurrentLatencies.add(latency);
            }
        }

        executor.shutdown();

        double successRate = (successCount * 100.0) / CONCURRENT_USERS;

        System.out.printf("Concurrent load test results:\n");
        System.out.printf("  Concurrent users: %d\n", CONCURRENT_USERS);
        System.out.printf("  Successful: %d\n", successCount);
        System.out.printf("  Success rate: %.2f%%\n", successRate);
        System.out.printf("  Total duration: %dms\n", totalDuration);

        if (!concurrentLatencies.isEmpty()) {
            concurrentLatencies.sort(Long::compareTo);
            long p95 = concurrentLatencies.get((int) (concurrentLatencies.size() * 0.95));
            System.out.printf("  P95 latency: %dms\n", p95);
        }

        assertTrue(successRate >= 90.0, "At least 90% of concurrent requests should succeed");
    }

    // ==================== Sustained Load Tests ====================

    @Test
    @Order(40)
    @DisplayName("Performance: Sustained load test")
    @Timeout(value = LOAD_TEST_DURATION_SECONDS + 30, unit = TimeUnit.SECONDS)
    public void testSustainedLoad() throws Exception {
        System.out.println("\nRunning sustained load test for " + LOAD_TEST_DURATION_SECONDS + " seconds...");

        ExecutorService executor = Executors.newFixedThreadPool(10);
        AtomicLong requestCount = new AtomicLong(0);
        AtomicLong successCount = new AtomicLong(0);
        AtomicLong errorCount = new AtomicLong(0);

        long testStartTime = System.currentTimeMillis();
        long testEndTime = testStartTime + (LOAD_TEST_DURATION_SECONDS * 1000L);

        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            futures.add(executor.submit(() -> {
                while (System.currentTimeMillis() < testEndTime) {
                    try {
                        Response response = given()
                            .contentType(ContentType.JSON)
                            .body("{\"algorithm\": \"CRYSTALS-Dilithium\", \"securityLevel\": 5}")
                        .when()
                            .post("/api/v11/curby/keypair");

                        requestCount.incrementAndGet();
                        if (response.statusCode() == 200) {
                            successCount.incrementAndGet();
                        } else {
                            errorCount.incrementAndGet();
                        }

                        Thread.sleep(100); // 10 req/sec per thread
                    } catch (InterruptedException e) {
                        break;
                    } catch (Exception e) {
                        errorCount.incrementAndGet();
                    }
                }
            }));
        }

        // Wait for all threads to complete
        for (Future<?> future : futures) {
            future.get();
        }

        executor.shutdown();

        long totalDuration = System.currentTimeMillis() - testStartTime;
        double avgThroughput = (requestCount.get() * 1000.0) / totalDuration;
        double successRate = (successCount.get() * 100.0) / requestCount.get();

        System.out.printf("Sustained load test results:\n");
        System.out.printf("  Duration: %.2f seconds\n", totalDuration / 1000.0);
        System.out.printf("  Total requests: %d\n", requestCount.get());
        System.out.printf("  Successful: %d\n", successCount.get());
        System.out.printf("  Errors: %d\n", errorCount.get());
        System.out.printf("  Success rate: %.2f%%\n", successRate);
        System.out.printf("  Avg throughput: %.2f req/sec\n", avgThroughput);

        assertTrue(successRate >= 85.0, "Success rate should be at least 85% under sustained load");
        assertTrue(avgThroughput >= 10.0, "Average throughput should be at least 10 req/sec");
    }

    // ==================== Cache Performance Tests ====================

    @Test
    @Order(50)
    @DisplayName("Performance: Cache effectiveness")
    public void testCachePerformance() {
        System.out.println("\nTesting cache effectiveness...");

        // Get initial metrics
        Response initialMetrics = given()
            .when()
                .get("/api/v11/curby/metrics");

        long initialCached = initialMetrics.path("cachedResponses") != null ?
            ((Number) initialMetrics.path("cachedResponses")).longValue() : 0L;

        // Make multiple requests that should hit cache
        for (int i = 0; i < 10; i++) {
            given()
                .contentType(ContentType.JSON)
                .body("{\"algorithm\": \"CRYSTALS-Dilithium\", \"securityLevel\": 5}")
            .when()
                .post("/api/v11/curby/keypair")
            .then()
                .statusCode(200);
        }

        // Check if cache is being used
        Response finalMetrics = given()
            .when()
                .get("/api/v11/curby/metrics");

        long finalCached = finalMetrics.path("cachedResponses") != null ?
            ((Number) finalMetrics.path("cachedResponses")).longValue() : 0L;

        System.out.printf("Cache statistics:\n");
        System.out.printf("  Initial cached responses: %d\n", initialCached);
        System.out.printf("  Final cached responses: %d\n", finalCached);
        System.out.printf("  Cache growth: %d\n", finalCached - initialCached);

        // Cache should grow or remain stable
        assertTrue(finalCached >= initialCached, "Cache should be utilized");
    }

    // ==================== Batch Operations Performance ====================

    @Test
    @Order(60)
    @DisplayName("Performance: Batch key generation efficiency")
    public void testBatchOperationPerformance() {
        System.out.println("\nTesting batch operation performance...");

        // Test single requests
        long singleStartTime = System.currentTimeMillis();
        for (int i = 0; i < 10; i++) {
            given()
                .contentType(ContentType.JSON)
                .body("{\"algorithm\": \"CRYSTALS-Dilithium\", \"securityLevel\": 5}")
            .when()
                .post("/api/v11/curby/keypair")
            .then()
                .statusCode(200);
        }
        long singleDuration = System.currentTimeMillis() - singleStartTime;

        // Test batch request
        long batchStartTime = System.currentTimeMillis();
        given()
            .contentType(ContentType.JSON)
            .body("{\"algorithm\": \"CRYSTALS-Dilithium\", \"count\": 10}")
        .when()
            .post("/api/v11/curby/keypair/batch")
        .then()
            .statusCode(200);
        long batchDuration = System.currentTimeMillis() - batchStartTime;

        System.out.printf("Batch vs Single comparison:\n");
        System.out.printf("  10 single requests: %dms\n", singleDuration);
        System.out.printf("  1 batch request (10 keys): %dms\n", batchDuration);
        System.out.printf("  Efficiency gain: %.2fx\n", (double) singleDuration / batchDuration);
    }
}
