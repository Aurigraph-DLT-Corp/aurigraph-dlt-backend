package io.aurigraph.v11.crypto.curby;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectSpy;
import io.restassured.http.ContentType;
import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mockito;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.Matchers.greaterThan;
import static org.mockito.ArgumentMatchers.*;

/**
 * Unit tests for CURByQuantumResource
 *
 * Tests all REST endpoints with comprehensive coverage:
 * - Key pair generation (single and batch)
 * - Signature generation
 * - Signature verification
 * - Health checks
 * - Metrics retrieval
 * - Error handling
 * - Input validation
 *
 * Target: 90%+ code coverage
 *
 * @author Aurigraph V11
 * @version 11.0.0
 */
@QuarkusTest
@DisplayName("CURBy Quantum Resource Tests")
public class CURByQuantumResourceTest {

    @InjectSpy
    CURByQuantumClient curbyClient;

    private CURByQuantumClient.QuantumKeyPairResponse mockKeyPairResponse;
    private CURByQuantumClient.QuantumSignatureResponse mockSignatureResponse;
    private CURByQuantumClient.QuantumVerificationResponse mockVerificationResponse;
    private CURByQuantumClient.CURByHealthStatus mockHealthStatus;

    @BeforeEach
    public void setup() {
        // Setup mock responses
        mockKeyPairResponse = new CURByQuantumClient.QuantumKeyPairResponse(
            true,
            "test-key-id-123",
            "CRYSTALS-Dilithium",
            "PUBLIC_KEY_BASE64_DATA",
            "PRIVATE_KEY_BASE64_DATA",
            2592,
            4896,
            System.currentTimeMillis()
        );

        mockSignatureResponse = new CURByQuantumClient.QuantumSignatureResponse(
            true,
            "SIGNATURE_BASE64_DATA",
            "CRYSTALS-Dilithium",
            "CURBY_API",
            System.currentTimeMillis()
        );

        mockVerificationResponse = new CURByQuantumClient.QuantumVerificationResponse(
            true,
            true,
            "CRYSTALS-Dilithium",
            "CURBY_API",
            System.currentTimeMillis()
        );

        mockHealthStatus = new CURByQuantumClient.CURByHealthStatus(
            true,
            true,
            1000L,
            950L,
            50L,
            100L,
            0.95,
            25,
            System.currentTimeMillis()
        );
    }

    // ==================== Key Pair Generation Tests ====================

    @Test
    @DisplayName("POST /api/v11/curby/keypair - Success")
    public void testGenerateKeyPair_Success() {
        Mockito.when(curbyClient.generateKeyPair(anyString()))
            .thenReturn(Uni.createFrom().item(mockKeyPairResponse));

        given()
            .contentType(ContentType.JSON)
            .body("{\"algorithm\": \"CRYSTALS-Dilithium\", \"securityLevel\": 5}")
        .when()
            .post("/api/v11/curby/keypair")
        .then()
            .statusCode(200)
            .body("success", is(true))
            .body("keyId", is("test-key-id-123"))
            .body("algorithm", is("CRYSTALS-Dilithium"))
            .body("publicKeySize", is(2592))
            .body("privateKeySize", is(4896))
            .body("timestamp", greaterThan(0L));
    }

    @Test
    @DisplayName("POST /api/v11/curby/keypair - Missing Algorithm")
    public void testGenerateKeyPair_MissingAlgorithm() {
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
    @DisplayName("POST /api/v11/curby/keypair - Empty Algorithm")
    public void testGenerateKeyPair_EmptyAlgorithm() {
        given()
            .contentType(ContentType.JSON)
            .body("{\"algorithm\": \"\", \"securityLevel\": 5}")
        .when()
            .post("/api/v11/curby/keypair")
        .then()
            .statusCode(400)
            .body("error", containsString("Algorithm is required"));
    }

    @Test
    @DisplayName("POST /api/v11/curby/keypair - Service Unavailable")
    public void testGenerateKeyPair_ServiceUnavailable() {
        Mockito.when(curbyClient.generateKeyPair(anyString()))
            .thenReturn(Uni.createFrom().failure(new CURByQuantumClient.CURByException("Service unavailable")));

        given()
            .contentType(ContentType.JSON)
            .body("{\"algorithm\": \"CRYSTALS-Dilithium\", \"securityLevel\": 5}")
        .when()
            .post("/api/v11/curby/keypair")
        .then()
            .statusCode(503)
            .body("error", containsString("Service unavailable"));
    }

    @Test
    @DisplayName("POST /api/v11/curby/keypair - Kyber Algorithm")
    public void testGenerateKeyPair_KyberAlgorithm() {
        CURByQuantumClient.QuantumKeyPairResponse kyberResponse = new CURByQuantumClient.QuantumKeyPairResponse(
            true,
            "test-key-kyber-456",
            "CRYSTALS-Kyber",
            "KYBER_PUBLIC_KEY",
            "KYBER_PRIVATE_KEY",
            1568,
            3168,
            System.currentTimeMillis()
        );

        Mockito.when(curbyClient.generateKeyPair("CRYSTALS-Kyber"))
            .thenReturn(Uni.createFrom().item(kyberResponse));

        given()
            .contentType(ContentType.JSON)
            .body("{\"algorithm\": \"CRYSTALS-Kyber\", \"securityLevel\": 5}")
        .when()
            .post("/api/v11/curby/keypair")
        .then()
            .statusCode(200)
            .body("success", is(true))
            .body("algorithm", is("CRYSTALS-Kyber"))
            .body("publicKeySize", is(1568))
            .body("privateKeySize", is(3168));
    }

    // ==================== Signature Generation Tests ====================

    @Test
    @DisplayName("POST /api/v11/curby/sign - Success")
    public void testGenerateSignature_Success() {
        Mockito.when(curbyClient.generateSignature(anyString(), anyString(), anyString()))
            .thenReturn(Uni.createFrom().item(mockSignatureResponse));

        given()
            .contentType(ContentType.JSON)
            .body("{\"data\": \"Hello World\", \"privateKey\": \"PRIVATE_KEY_DATA\", \"algorithm\": \"CRYSTALS-Dilithium\"}")
        .when()
            .post("/api/v11/curby/sign")
        .then()
            .statusCode(200)
            .body("success", is(true))
            .body("signature", is("SIGNATURE_BASE64_DATA"))
            .body("algorithm", is("CRYSTALS-Dilithium"))
            .body("source", is("CURBY_API"));
    }

    @Test
    @DisplayName("POST /api/v11/curby/sign - Missing Data")
    public void testGenerateSignature_MissingData() {
        given()
            .contentType(ContentType.JSON)
            .body("{\"privateKey\": \"PRIVATE_KEY_DATA\", \"algorithm\": \"CRYSTALS-Dilithium\"}")
        .when()
            .post("/api/v11/curby/sign")
        .then()
            .statusCode(400)
            .body("error", containsString("Data is required"));
    }

    @Test
    @DisplayName("POST /api/v11/curby/sign - Missing Private Key")
    public void testGenerateSignature_MissingPrivateKey() {
        given()
            .contentType(ContentType.JSON)
            .body("{\"data\": \"Hello World\", \"algorithm\": \"CRYSTALS-Dilithium\"}")
        .when()
            .post("/api/v11/curby/sign")
        .then()
            .statusCode(400)
            .body("error", containsString("Private key is required"));
    }

    @Test
    @DisplayName("POST /api/v11/curby/sign - Empty Data")
    public void testGenerateSignature_EmptyData() {
        given()
            .contentType(ContentType.JSON)
            .body("{\"data\": \"\", \"privateKey\": \"PRIVATE_KEY_DATA\", \"algorithm\": \"CRYSTALS-Dilithium\"}")
        .when()
            .post("/api/v11/curby/sign")
        .then()
            .statusCode(400)
            .body("error", containsString("Data is required"));
    }

    @Test
    @DisplayName("POST /api/v11/curby/sign - Service Failure")
    public void testGenerateSignature_ServiceFailure() {
        Mockito.when(curbyClient.generateSignature(anyString(), anyString(), anyString()))
            .thenReturn(Uni.createFrom().failure(new RuntimeException("Signature generation failed")));

        given()
            .contentType(ContentType.JSON)
            .body("{\"data\": \"Hello World\", \"privateKey\": \"PRIVATE_KEY_DATA\", \"algorithm\": \"CRYSTALS-Dilithium\"}")
        .when()
            .post("/api/v11/curby/sign")
        .then()
            .statusCode(503)
            .body("error", containsString("Service unavailable"));
    }

    @Test
    @DisplayName("POST /api/v11/curby/sign - Long Data")
    public void testGenerateSignature_LongData() {
        String longData = "X".repeat(10000);
        Mockito.when(curbyClient.generateSignature(eq(longData), anyString(), anyString()))
            .thenReturn(Uni.createFrom().item(mockSignatureResponse));

        given()
            .contentType(ContentType.JSON)
            .body(String.format("{\"data\": \"%s\", \"privateKey\": \"PRIVATE_KEY_DATA\", \"algorithm\": \"CRYSTALS-Dilithium\"}", longData))
        .when()
            .post("/api/v11/curby/sign")
        .then()
            .statusCode(200)
            .body("success", is(true));
    }

    // ==================== Signature Verification Tests ====================

    @Test
    @DisplayName("POST /api/v11/curby/verify - Success (Valid Signature)")
    public void testVerifySignature_Valid() {
        Mockito.when(curbyClient.verifySignature(anyString(), anyString(), anyString(), anyString()))
            .thenReturn(Uni.createFrom().item(mockVerificationResponse));

        given()
            .contentType(ContentType.JSON)
            .body("{\"data\": \"Hello World\", \"signature\": \"SIGNATURE_DATA\", \"publicKey\": \"PUBLIC_KEY_DATA\", \"algorithm\": \"CRYSTALS-Dilithium\"}")
        .when()
            .post("/api/v11/curby/verify")
        .then()
            .statusCode(200)
            .body("success", is(true))
            .body("valid", is(true))
            .body("algorithm", is("CRYSTALS-Dilithium"))
            .body("source", is("CURBY_API"));
    }

    @Test
    @DisplayName("POST /api/v11/curby/verify - Invalid Signature")
    public void testVerifySignature_Invalid() {
        CURByQuantumClient.QuantumVerificationResponse invalidResponse =
            new CURByQuantumClient.QuantumVerificationResponse(
                true, false, "CRYSTALS-Dilithium", "CURBY_API", System.currentTimeMillis()
            );

        Mockito.when(curbyClient.verifySignature(anyString(), anyString(), anyString(), anyString()))
            .thenReturn(Uni.createFrom().item(invalidResponse));

        given()
            .contentType(ContentType.JSON)
            .body("{\"data\": \"Hello World\", \"signature\": \"INVALID_SIGNATURE\", \"publicKey\": \"PUBLIC_KEY_DATA\", \"algorithm\": \"CRYSTALS-Dilithium\"}")
        .when()
            .post("/api/v11/curby/verify")
        .then()
            .statusCode(200)
            .body("success", is(true))
            .body("valid", is(false));
    }

    @Test
    @DisplayName("POST /api/v11/curby/verify - Missing Data")
    public void testVerifySignature_MissingData() {
        given()
            .contentType(ContentType.JSON)
            .body("{\"signature\": \"SIGNATURE_DATA\", \"publicKey\": \"PUBLIC_KEY_DATA\", \"algorithm\": \"CRYSTALS-Dilithium\"}")
        .when()
            .post("/api/v11/curby/verify")
        .then()
            .statusCode(400)
            .body("error", containsString("Data is required"));
    }

    @Test
    @DisplayName("POST /api/v11/curby/verify - Missing Signature")
    public void testVerifySignature_MissingSignature() {
        given()
            .contentType(ContentType.JSON)
            .body("{\"data\": \"Hello World\", \"publicKey\": \"PUBLIC_KEY_DATA\", \"algorithm\": \"CRYSTALS-Dilithium\"}")
        .when()
            .post("/api/v11/curby/verify")
        .then()
            .statusCode(400)
            .body("error", containsString("Signature is required"));
    }

    @Test
    @DisplayName("POST /api/v11/curby/verify - Missing Public Key")
    public void testVerifySignature_MissingPublicKey() {
        given()
            .contentType(ContentType.JSON)
            .body("{\"data\": \"Hello World\", \"signature\": \"SIGNATURE_DATA\", \"algorithm\": \"CRYSTALS-Dilithium\"}")
        .when()
            .post("/api/v11/curby/verify")
        .then()
            .statusCode(400)
            .body("error", containsString("Public key is required"));
    }

    @Test
    @DisplayName("POST /api/v11/curby/verify - Service Failure")
    public void testVerifySignature_ServiceFailure() {
        Mockito.when(curbyClient.verifySignature(anyString(), anyString(), anyString(), anyString()))
            .thenReturn(Uni.createFrom().failure(new RuntimeException("Verification service error")));

        given()
            .contentType(ContentType.JSON)
            .body("{\"data\": \"Hello World\", \"signature\": \"SIGNATURE_DATA\", \"publicKey\": \"PUBLIC_KEY_DATA\", \"algorithm\": \"CRYSTALS-Dilithium\"}")
        .when()
            .post("/api/v11/curby/verify")
        .then()
            .statusCode(503)
            .body("error", containsString("Service unavailable"));
    }

    // ==================== Health Status Tests ====================

    @Test
    @DisplayName("GET /api/v11/curby/health - Healthy")
    public void testGetHealthStatus_Healthy() {
        Mockito.when(curbyClient.getHealthStatus())
            .thenReturn(mockHealthStatus);

        given()
        .when()
            .get("/api/v11/curby/health")
        .then()
            .statusCode(200)
            .body("enabled", is(true))
            .body("healthy", is(true))
            .body("totalRequests", is(1000))
            .body("successfulRequests", is(950))
            .body("failedRequests", is(50))
            .body("successRate", is(0.95f))
            .body("cacheSize", is(25));
    }

    @Test
    @DisplayName("GET /api/v11/curby/health - Unhealthy")
    public void testGetHealthStatus_Unhealthy() {
        CURByQuantumClient.CURByHealthStatus unhealthyStatus = new CURByQuantumClient.CURByHealthStatus(
            true, false, 1000L, 400L, 600L, 50L, 0.40, 10, System.currentTimeMillis()
        );

        Mockito.when(curbyClient.getHealthStatus())
            .thenReturn(unhealthyStatus);

        given()
        .when()
            .get("/api/v11/curby/health")
        .then()
            .statusCode(503)
            .body("healthy", is(false))
            .body("successRate", is(0.40f));
    }

    // ==================== Metrics Tests ====================

    @Test
    @DisplayName("GET /api/v11/curby/metrics - Success")
    public void testGetMetrics_Success() {
        Mockito.when(curbyClient.getHealthStatus())
            .thenReturn(mockHealthStatus);

        given()
        .when()
            .get("/api/v11/curby/metrics")
        .then()
            .statusCode(200)
            .body("totalRequests", is(1000))
            .body("successfulRequests", is(950))
            .body("failedRequests", is(50))
            .body("successRate", is("95.00%"))
            .body("cachedResponses", is(100))
            .body("cacheSize", is(25))
            .body("circuitBreakerOpen", is(false))
            .body("serviceEnabled", is(true));
    }

    // ==================== Algorithms Tests ====================

    @Test
    @DisplayName("GET /api/v11/curby/algorithms - Success")
    public void testGetSupportedAlgorithms_Success() {
        given()
        .when()
            .get("/api/v11/curby/algorithms")
        .then()
            .statusCode(200)
            .body("default", is("CRYSTALS-Dilithium"))
            .body("recommendedSecurityLevel", is(5))
            .body("quantumSafe", is(true))
            .body("signatures.'CRYSTALS-Dilithium'.type", is("Digital Signature"))
            .body("signatures.'CRYSTALS-Dilithium'.securityLevel", is("NIST Level 5"))
            .body("encryption.'CRYSTALS-Kyber'.type", is("Key Encapsulation"));
    }

    // ==================== Batch Key Generation Tests ====================

    @Test
    @DisplayName("POST /api/v11/curby/keypair/batch - Success")
    public void testGenerateKeyPairBatch_Success() {
        Mockito.when(curbyClient.generateKeyPair(anyString()))
            .thenReturn(Uni.createFrom().item(mockKeyPairResponse));

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

    @Test
    @DisplayName("POST /api/v11/curby/keypair/batch - Count Too Low")
    public void testGenerateKeyPairBatch_CountTooLow() {
        given()
            .contentType(ContentType.JSON)
            .body("{\"algorithm\": \"CRYSTALS-Dilithium\", \"count\": 0}")
        .when()
            .post("/api/v11/curby/keypair/batch")
        .then()
            .statusCode(400)
            .body("error", containsString("Count must be between 1 and 100"));
    }

    @Test
    @DisplayName("POST /api/v11/curby/keypair/batch - Count Too High")
    public void testGenerateKeyPairBatch_CountTooHigh() {
        given()
            .contentType(ContentType.JSON)
            .body("{\"algorithm\": \"CRYSTALS-Dilithium\", \"count\": 101}")
        .when()
            .post("/api/v11/curby/keypair/batch")
        .then()
            .statusCode(400)
            .body("error", containsString("Count must be between 1 and 100"));
    }

    @Test
    @DisplayName("POST /api/v11/curby/keypair/batch - Maximum Allowed Count")
    public void testGenerateKeyPairBatch_MaxCount() {
        Mockito.when(curbyClient.generateKeyPair(anyString()))
            .thenReturn(Uni.createFrom().item(mockKeyPairResponse));

        given()
            .contentType(ContentType.JSON)
            .body("{\"algorithm\": \"CRYSTALS-Dilithium\", \"count\": 100}")
        .when()
            .post("/api/v11/curby/keypair/batch")
        .then()
            .statusCode(200)
            .body("success", is(true))
            .body("count", is(100));
    }

    @Test
    @DisplayName("POST /api/v11/curby/keypair/batch - Service Failure")
    public void testGenerateKeyPairBatch_ServiceFailure() {
        Mockito.when(curbyClient.generateKeyPair(anyString()))
            .thenReturn(Uni.createFrom().failure(new RuntimeException("Batch generation failed")));

        given()
            .contentType(ContentType.JSON)
            .body("{\"algorithm\": \"CRYSTALS-Dilithium\", \"count\": 5}")
        .when()
            .post("/api/v11/curby/keypair/batch")
        .then()
            .statusCode(500)
            .body("error", containsString("Batch generation failed"));
    }

    // ==================== Content Type Tests ====================

    @Test
    @DisplayName("POST /api/v11/curby/keypair - Invalid Content Type")
    public void testGenerateKeyPair_InvalidContentType() {
        given()
            .contentType(ContentType.TEXT)
            .body("algorithm=CRYSTALS-Dilithium")
        .when()
            .post("/api/v11/curby/keypair")
        .then()
            .statusCode(415); // Unsupported Media Type
    }

    @Test
    @DisplayName("POST /api/v11/curby/sign - Malformed JSON")
    public void testGenerateSignature_MalformedJSON() {
        given()
            .contentType(ContentType.JSON)
            .body("{\"data\": \"Hello World\", invalid json}")
        .when()
            .post("/api/v11/curby/sign")
        .then()
            .statusCode(400); // Bad Request
    }

    // ==================== CORS and Security Tests ====================

    @Test
    @DisplayName("OPTIONS /api/v11/curby/keypair - CORS Preflight")
    public void testCORSPreflight() {
        given()
            .header("Origin", "https://example.com")
            .header("Access-Control-Request-Method", "POST")
        .when()
            .options("/api/v11/curby/keypair")
        .then()
            .statusCode(anyOf(is(200), is(204))); // Either OK or No Content
    }
}
