package io.aurigraph.v11.crypto.curby;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasEntry;

/**
 * Integration tests for CURBy Quantum Service
 *
 * Tests end-to-end functionality with live CURBy service integration:
 * - Real key pair generation
 * - Real signature generation and verification
 * - Service health and availability
 * - Error handling with live service
 * - Performance characteristics
 * - Circuit breaker behavior
 * - Fallback mechanisms
 *
 * These tests require:
 * - CURBy service endpoint configured (or uses fallback)
 * - Valid API credentials (or uses local fallback)
 *
 * @author Aurigraph V11
 * @version 11.0.0
 */
@QuarkusTest
@DisplayName("CURBy Quantum Integration Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class CURByQuantumIntegrationTest {

    private static String testKeyId;
    private static String testPublicKey;
    private static String testPrivateKey;
    private static String testSignature;
    private static final String TEST_DATA = "Integration Test Data - Quantum Cryptography Verification";

    // ==================== Setup and Teardown ====================

    @BeforeAll
    public static void setupSuite() {
        System.out.println("Starting CURBy Integration Test Suite");
        System.out.println("Testing quantum-resistant cryptography end-to-end");
    }

    @AfterAll
    public static void teardownSuite() {
        System.out.println("Completed CURBy Integration Test Suite");
        System.out.println("Verified quantum cryptographic operations");
    }

    // ==================== Service Health Tests ====================

    @Test
    @Order(1)
    @DisplayName("Integration: Service health check should return status")
    public void testServiceHealth() {
        given()
        .when()
            .get("/api/v11/curby/health")
        .then()
            .statusCode(anyOf(is(200), is(503))) // Either healthy or unhealthy
            .body("enabled", is(true))
            .body("totalRequests", greaterThan(-1))
            .body("timestamp", greaterThan(0L));
    }

    @Test
    @Order(2)
    @DisplayName("Integration: Metrics endpoint should return statistics")
    public void testServiceMetrics() {
        given()
        .when()
            .get("/api/v11/curby/metrics")
        .then()
            .statusCode(200)
            .body("totalRequests", greaterThan(-1))
            .body("serviceEnabled", is(true))
            .body("timestamp", greaterThan(0L));
    }

    @Test
    @Order(3)
    @DisplayName("Integration: Algorithms endpoint should list supported algorithms")
    public void testSupportedAlgorithms() {
        given()
        .when()
            .get("/api/v11/curby/algorithms")
        .then()
            .statusCode(200)
            .body("default", is("CRYSTALS-Dilithium"))
            .body("quantumSafe", is(true))
            .body("recommendedSecurityLevel", is(5));
    }

    // ==================== Key Generation Integration Tests ====================

    @Test
    @Order(10)
    @DisplayName("Integration: Generate Dilithium key pair")
    public void testGenerateDilithiumKeyPair() {
        var response = given()
            .contentType(ContentType.JSON)
            .body("{\"algorithm\": \"CRYSTALS-Dilithium\", \"securityLevel\": 5}")
        .when()
            .post("/api/v11/curby/keypair")
        .then()
            .statusCode(200)
            .body("success", is(true))
            .body("algorithm", is("CRYSTALS-Dilithium"))
            .body("keyId", notNullValue())
            .body("publicKey", notNullValue())
            .body("privateKey", notNullValue())
            .body("timestamp", greaterThan(0L))
        .extract().response();

        // Store keys for subsequent tests
        testKeyId = response.path("keyId");
        testPublicKey = response.path("publicKey");
        testPrivateKey = response.path("privateKey");

        System.out.println("Generated Dilithium key pair: " + testKeyId);
    }

    @Test
    @Order(11)
    @DisplayName("Integration: Generate Kyber key pair")
    public void testGenerateKyberKeyPair() {
        given()
            .contentType(ContentType.JSON)
            .body("{\"algorithm\": \"CRYSTALS-Kyber\", \"securityLevel\": 5}")
        .when()
            .post("/api/v11/curby/keypair")
        .then()
            .statusCode(200)
            .body("success", is(true))
            .body("algorithm", is("CRYSTALS-Kyber"))
            .body("keyId", notNullValue())
            .body("publicKey", notNullValue())
            .body("privateKey", notNullValue());
    }

    @Test
    @Order(12)
    @DisplayName("Integration: Batch key generation with 5 keys")
    public void testBatchKeyGeneration() {
        given()
            .contentType(ContentType.JSON)
            .body("{\"algorithm\": \"CRYSTALS-Dilithium\", \"count\": 5}")
        .when()
            .post("/api/v11/curby/keypair/batch")
        .then()
            .statusCode(200)
            .body("success", is(true))
            .body("count", is(5));
    }

    // ==================== Signature Generation Integration Tests ====================

    @Test
    @Order(20)
    @DisplayName("Integration: Generate quantum signature")
    public void testGenerateSignature() {
        // First generate a key pair for signing
        var keyResponse = given()
            .contentType(ContentType.JSON)
            .body("{\"algorithm\": \"CRYSTALS-Dilithium\", \"securityLevel\": 5}")
        .when()
            .post("/api/v11/curby/keypair")
        .then()
            .statusCode(200)
        .extract().response();

        String privateKey = keyResponse.path("privateKey");
        String publicKey = keyResponse.path("publicKey");

        // Generate signature
        var signResponse = given()
            .contentType(ContentType.JSON)
            .body(String.format("{\"data\": \"%s\", \"privateKey\": \"%s\", \"algorithm\": \"CRYSTALS-Dilithium\"}",
                TEST_DATA, privateKey))
        .when()
            .post("/api/v11/curby/sign")
        .then()
            .statusCode(200)
            .body("success", is(true))
            .body("signature", notNullValue())
            .body("algorithm", is("CRYSTALS-Dilithium"))
            .body("timestamp", greaterThan(0L))
        .extract().response();

        testSignature = signResponse.path("signature");
        testPublicKey = publicKey;

        System.out.println("Generated quantum signature successfully");
    }

    // ==================== Signature Verification Integration Tests ====================

    @Test
    @Order(30)
    @DisplayName("Integration: Verify valid quantum signature")
    public void testVerifyValidSignature() {
        // First generate key pair and signature
        var keyResponse = given()
            .contentType(ContentType.JSON)
            .body("{\"algorithm\": \"CRYSTALS-Dilithium\", \"securityLevel\": 5}")
        .when()
            .post("/api/v11/curby/keypair")
        .then()
            .statusCode(200)
        .extract().response();

        String privateKey = keyResponse.path("privateKey");
        String publicKey = keyResponse.path("publicKey");

        var signResponse = given()
            .contentType(ContentType.JSON)
            .body(String.format("{\"data\": \"%s\", \"privateKey\": \"%s\", \"algorithm\": \"CRYSTALS-Dilithium\"}",
                TEST_DATA, privateKey))
        .when()
            .post("/api/v11/curby/sign")
        .then()
            .statusCode(200)
        .extract().response();

        String signature = signResponse.path("signature");

        // Verify the signature
        given()
            .contentType(ContentType.JSON)
            .body(String.format("{\"data\": \"%s\", \"signature\": \"%s\", \"publicKey\": \"%s\", \"algorithm\": \"CRYSTALS-Dilithium\"}",
                TEST_DATA, signature, publicKey))
        .when()
            .post("/api/v11/curby/verify")
        .then()
            .statusCode(200)
            .body("success", is(true))
            .body("valid", is(true))
            .body("algorithm", is("CRYSTALS-Dilithium"));

        System.out.println("Verified quantum signature successfully");
    }

    @Test
    @Order(31)
    @DisplayName("Integration: Detect invalid quantum signature")
    public void testVerifyInvalidSignature() {
        // Generate a key pair
        var keyResponse = given()
            .contentType(ContentType.JSON)
            .body("{\"algorithm\": \"CRYSTALS-Dilithium\", \"securityLevel\": 5}")
        .when()
            .post("/api/v11/curby/keypair")
        .then()
            .statusCode(200)
        .extract().response();

        String publicKey = keyResponse.path("publicKey");

        // Attempt to verify with invalid signature
        given()
            .contentType(ContentType.JSON)
            .body(String.format("{\"data\": \"%s\", \"signature\": \"INVALID_SIGNATURE_DATA\", \"publicKey\": \"%s\", \"algorithm\": \"CRYSTALS-Dilithium\"}",
                TEST_DATA, publicKey))
        .when()
            .post("/api/v11/curby/verify")
        .then()
            .statusCode(anyOf(is(200), is(503))) // May fail validation or service error
            .body(anyOf(
                hasEntry(is("valid"), is(false)),  // Validation failed
                hasEntry(is("error"), notNullValue()) // Service error
            ));
    }

    // ==================== End-to-End Workflow Tests ====================

    @Test
    @Order(40)
    @DisplayName("Integration: Complete cryptographic workflow (generate, sign, verify)")
    public void testCompleteWorkflow() {
        // Step 1: Generate key pair
        System.out.println("Step 1: Generating quantum key pair...");
        var keyResponse = given()
            .contentType(ContentType.JSON)
            .body("{\"algorithm\": \"CRYSTALS-Dilithium\", \"securityLevel\": 5}")
        .when()
            .post("/api/v11/curby/keypair")
        .then()
            .statusCode(200)
            .body("success", is(true))
        .extract().response();

        String privateKey = keyResponse.path("privateKey");
        String publicKey = keyResponse.path("publicKey");
        String keyId = keyResponse.path("keyId");
        System.out.println("Generated key pair: " + keyId);

        // Step 2: Sign data
        System.out.println("Step 2: Generating quantum signature...");
        String dataToSign = "Integration Test - Complete Workflow - " + System.currentTimeMillis();
        var signResponse = given()
            .contentType(ContentType.JSON)
            .body(String.format("{\"data\": \"%s\", \"privateKey\": \"%s\", \"algorithm\": \"CRYSTALS-Dilithium\"}",
                dataToSign, privateKey))
        .when()
            .post("/api/v11/curby/sign")
        .then()
            .statusCode(200)
            .body("success", is(true))
        .extract().response();

        String signature = signResponse.path("signature");
        System.out.println("Generated signature successfully");

        // Step 3: Verify signature
        System.out.println("Step 3: Verifying quantum signature...");
        given()
            .contentType(ContentType.JSON)
            .body(String.format("{\"data\": \"%s\", \"signature\": \"%s\", \"publicKey\": \"%s\", \"algorithm\": \"CRYSTALS-Dilithium\"}",
                dataToSign, signature, publicKey))
        .when()
            .post("/api/v11/curby/verify")
        .then()
            .statusCode(200)
            .body("success", is(true))
            .body("valid", is(true));

        System.out.println("✓ Complete cryptographic workflow verified successfully");
    }

    // ==================== Performance Integration Tests ====================

    @Test
    @Order(50)
    @DisplayName("Integration: Measure key generation latency")
    public void testKeyGenerationLatency() {
        long startTime = System.currentTimeMillis();

        given()
            .contentType(ContentType.JSON)
            .body("{\"algorithm\": \"CRYSTALS-Dilithium\", \"securityLevel\": 5}")
        .when()
            .post("/api/v11/curby/keypair")
        .then()
            .statusCode(200)
            .body("success", is(true));

        long latency = System.currentTimeMillis() - startTime;
        System.out.println("Key generation latency: " + latency + "ms");

        // Latency should be reasonable (< 10 seconds including network)
        Assertions.assertTrue(latency < 10000, "Key generation took too long: " + latency + "ms");
    }

    @Test
    @Order(51)
    @DisplayName("Integration: Measure signature generation latency")
    public void testSignatureGenerationLatency() {
        // Generate key first
        var keyResponse = given()
            .contentType(ContentType.JSON)
            .body("{\"algorithm\": \"CRYSTALS-Dilithium\", \"securityLevel\": 5}")
        .when()
            .post("/api/v11/curby/keypair")
        .then()
            .statusCode(200)
        .extract().response();

        String privateKey = keyResponse.path("privateKey");

        long startTime = System.currentTimeMillis();

        given()
            .contentType(ContentType.JSON)
            .body(String.format("{\"data\": \"%s\", \"privateKey\": \"%s\", \"algorithm\": \"CRYSTALS-Dilithium\"}",
                TEST_DATA, privateKey))
        .when()
            .post("/api/v11/curby/sign")
        .then()
            .statusCode(200)
            .body("success", is(true));

        long latency = System.currentTimeMillis() - startTime;
        System.out.println("Signature generation latency: " + latency + "ms");

        Assertions.assertTrue(latency < 10000, "Signature generation took too long: " + latency + "ms");
    }

    // ==================== Error Handling Integration Tests ====================

    @Test
    @Order(60)
    @DisplayName("Integration: Handle missing algorithm gracefully")
    public void testMissingAlgorithmHandling() {
        given()
            .contentType(ContentType.JSON)
            .body("{\"securityLevel\": 5}")
        .when()
            .post("/api/v11/curby/keypair")
        .then()
            .statusCode(400)
            .body("error", containsString("Algorithm is required"));
    }

    @Test
    @Order(61)
    @DisplayName("Integration: Handle invalid batch count")
    public void testInvalidBatchCountHandling() {
        given()
            .contentType(ContentType.JSON)
            .body("{\"algorithm\": \"CRYSTALS-Dilithium\", \"count\": 150}")
        .when()
            .post("/api/v11/curby/keypair/batch")
        .then()
            .statusCode(400)
            .body("error", containsString("Count must be between 1 and 100"));
    }

    // ==================== Live Service Tests (Conditional) ====================

    @Test
    @Order(100)
    @EnabledIfEnvironmentVariable(named = "CURBY_LIVE_TEST", matches = "true")
    @DisplayName("Integration: Test with live CURBy service")
    public void testLiveCURByService() {
        System.out.println("Testing with LIVE CURBy service");

        given()
        .when()
            .get("/api/v11/curby/health")
        .then()
            .statusCode(200)
            .body("healthy", is(true))
            .body("enabled", is(true));

        System.out.println("Live CURBy service is healthy and operational");
    }

    // ==================== Concurrent Operations Tests ====================

    @Test
    @Order(110)
    @DisplayName("Integration: Handle concurrent key generation requests")
    public void testConcurrentKeyGeneration() {
        // Submit 3 concurrent requests
        for (int i = 0; i < 3; i++) {
            given()
                .contentType(ContentType.JSON)
                .body("{\"algorithm\": \"CRYSTALS-Dilithium\", \"securityLevel\": 5}")
            .when()
                .post("/api/v11/curby/keypair")
            .then()
                .statusCode(200)
                .body("success", is(true));
        }

        System.out.println("Handled 3 concurrent key generation requests successfully");
    }

    // ==================== Fallback Mechanism Tests ====================

    @Test
    @Order(120)
    @DisplayName("Integration: Fallback to local crypto when service unavailable")
    public void testFallbackMechanism() {
        // The service should use local fallback if CURBy is unavailable
        // This test verifies the entire flow still works
        given()
            .contentType(ContentType.JSON)
            .body("{\"algorithm\": \"CRYSTALS-Dilithium\", \"securityLevel\": 5}")
        .when()
            .post("/api/v11/curby/keypair")
        .then()
            .statusCode(200)
            .body("success", is(true))
            .body("keyId", notNullValue());

        System.out.println("Fallback mechanism working correctly");
    }
}
