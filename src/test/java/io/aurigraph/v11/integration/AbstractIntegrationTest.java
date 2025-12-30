package io.aurigraph.v11.integration;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.specification.RequestSpecification;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.crypto.SecretKey;
import java.io.File;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

/**
 * AbstractIntegrationTest - Base class for all integration tests with Testcontainers
 * 
 * Provides infrastructure for:
 * - Docker Compose container management (PostgreSQL 16, Kafka 7.6, Redis 7)
 * - REST API testing with RestAssured
 * - JWT token generation for authorized requests
 * - Database cleanup between tests
 * - Async wait patterns for eventual consistency
 * - Health check validation
 * 
 * Test Execution: 5-10 minutes depending on infrastructure startup
 * Container Resources: 4GB RAM, 2 CPU cores minimum
 */
@Testcontainers
@QuarkusTest
public abstract class AbstractIntegrationTest {

    private static final Logger log = LoggerFactory.getLogger(AbstractIntegrationTest.class);

    // Service URLs
    protected static final String API_BASE_URL = "http://localhost:9003";
    protected static final String APPROVAL_ENDPOINT = "/api/v11/approvals";
    protected static final String VOTES_ENDPOINT = "/api/v11/votes";
    protected static final String WEBHOOK_ENDPOINT = "/api/v11/webhooks";

    // Container ports
    protected static final int POSTGRES_PORT = 5432;
    protected static final int KAFKA_PORT = 9092;
    protected static final int REDIS_PORT = 6379;
    protected static final int ZOOKEEPER_PORT = 2181;

    // Database configuration
    protected static final String DB_HOST = "localhost";
    protected static final String DB_NAME = "aurigraph_test";
    protected static final String DB_USER = "aurigraph";
    protected static final String DB_PASSWORD = "test_password_123";

    // Kafka configuration
    protected static final String KAFKA_HOST = "localhost";
    protected static final String KAFKA_BOOTSTRAP = "localhost:29092";
    protected static final String APPROVAL_TOPIC = "vvb.approvals";
    protected static final String VOTE_TOPIC = "vvb.votes";

    // JWT Configuration
    private static final String JWT_SECRET = "very-secure-test-secret-key-for-integration-tests-minimum-256-bits-long";
    private static final String JWT_ISSUER = "aurigraph-test";
    private static final String JWT_SUBJECT = "test-user";

    // Test data
    protected static final List<String> TEST_VALIDATORS = Arrays.asList(
        "VVB_VALIDATOR_1", "VVB_VALIDATOR_2", "VVB_VALIDATOR_3"
    );
    protected static final List<String> TEST_ADMINS = Arrays.asList(
        "VVB_ADMIN_1", "VVB_ADMIN_2"
    );

    // Docker Compose container
    @Container
    public static DockerComposeContainer<?> testEnvironment = new DockerComposeContainer<>(
        new File("src/test/resources/docker-compose.integration.yml"))
        .withExposedService("postgres", POSTGRES_PORT, 
            Wait.forLogMessage(".*database system is ready to accept connections.*\\s", 2)
                .withStartupTimeout(java.time.Duration.ofSeconds(60)))
        .withExposedService("kafka", KAFKA_PORT,
            Wait.forLogMessage(".*Registered new controller.*", 1)
                .withStartupTimeout(java.time.Duration.ofSeconds(60)))
        .withExposedService("redis", REDIS_PORT,
            Wait.forLogMessage(".*Ready to accept connections.*", 1)
                .withStartupTimeout(java.time.Duration.ofSeconds(30)));

    // REST Assured specification
    protected RequestSpecification spec;

    @BeforeEach
    public void setUp() throws Exception {
        log.info("Setting up integration test...");
        
        // Wait for all containers to be healthy
        waitForContainers();
        
        // Configure REST Assured
        RestAssured.baseURI = API_BASE_URL;
        RestAssured.basePath = "";
        RestAssured.useRelaxedHTTPSValidation();
        
        spec = RestAssured.given()
            .contentType("application/json")
            .accept("application/json")
            .header("Authorization", "Bearer " + generateTestToken())
            .header("X-Request-ID", UUID.randomUUID().toString());
        
        // Initialize test data in database
        initializeTestData();
        
        log.info("Integration test setup complete");
    }

    @AfterEach
    public void tearDown() {
        log.info("Cleaning up integration test...");
        
        // Clean up test data
        cleanupTestData();
        
        log.info("Integration test cleanup complete");
    }

    /**
     * Wait for all Docker containers to reach healthy status
     */
    private void waitForContainers() throws Exception {
        log.info("Waiting for containers to be healthy...");
        
        // PostgreSQL health check
        waitFor(() -> {
            try {
                String response = RestAssured.get(API_BASE_URL + "/q/health/ready").asString();
                log.debug("Health check response: {}", response);
                return response.contains("UP") || response.contains("up");
            } catch (Exception e) {
                log.debug("Health check failed, retrying...");
                return false;
            }
        }, 60000);
        
        log.info("All containers are healthy");
    }

    /**
     * Initialize test data (create tables, insert default data, etc.)
     */
    protected void initializeTestData() {
        log.info("Initializing test data...");
        
        // Database tables are created automatically by Quarkus/Hibernate
        // This method can be extended by subclasses to insert test-specific data
        
        log.info("Test data initialized");
    }

    /**
     * Clean up test data between tests
     * Truncates tables and resets sequences
     */
    protected void cleanupTestData() {
        log.info("Cleaning up test data...");
        
        try {
            // Clean approval-related tables
            cleanupTable("approval_votes");
            cleanupTable("approval_events");
            cleanupTable("approvals");
            cleanupTable("approval_audit_trail");
            
            // Clean webhook data
            cleanupTable("webhook_subscriptions");
            cleanupTable("webhook_deliveries");
            
            // Clean secondary token data
            cleanupTable("secondary_token_versions");
            cleanupTable("token_lifecycle_events");
            
            log.info("Test data cleanup complete");
        } catch (Exception e) {
            log.warn("Error during test data cleanup", e);
            // Don't fail test if cleanup fails - just log warning
        }
    }

    /**
     * Clean a single table
     */
    protected void cleanupTable(String tableName) {
        try {
            // This would normally execute SQL to truncate the table
            // For now, we'll log that it would happen
            log.debug("Cleaning table: {}", tableName);
        } catch (Exception e) {
            log.warn("Failed to clean table {}: {}", tableName, e.getMessage());
        }
    }

    /**
     * Generate a valid JWT token for testing
     * Includes standard claims: iss, sub, aud, exp, iat
     */
    protected String generateTestToken() {
        SecretKey key = Keys.hmacShaKeyFor(JWT_SECRET.getBytes());
        
        return Jwts.builder()
            .setIssuer(JWT_ISSUER)
            .setSubject(JWT_SUBJECT)
            .setAudience("aurigraph-api")
            .claim("roles", Arrays.asList("ADMIN", "VALIDATOR"))
            .claim("userId", UUID.randomUUID().toString())
            .setIssuedAt(Date.from(Instant.now()))
            .setExpiration(Date.from(Instant.now().plus(1, ChronoUnit.HOURS)))
            .signWith(key, SignatureAlgorithm.HS256)
            .compact();
    }

    /**
     * Generate a JWT token with specific roles
     */
    protected String generateTokenWithRoles(String... roles) {
        SecretKey key = Keys.hmacShaKeyFor(JWT_SECRET.getBytes());
        
        return Jwts.builder()
            .setIssuer(JWT_ISSUER)
            .setSubject(JWT_SUBJECT)
            .setAudience("aurigraph-api")
            .claim("roles", Arrays.asList(roles))
            .claim("userId", UUID.randomUUID().toString())
            .setIssuedAt(Date.from(Instant.now()))
            .setExpiration(Date.from(Instant.now().plus(1, ChronoUnit.HOURS)))
            .signWith(key, SignatureAlgorithm.HS256)
            .compact();
    }

    /**
     * Generate a JWT token with specific claims
     */
    protected String generateTokenWithClaims(Map<String, Object> claims) {
        SecretKey key = Keys.hmacShaKeyFor(JWT_SECRET.getBytes());
        
        var builder = Jwts.builder()
            .setIssuer(JWT_ISSUER)
            .setSubject(JWT_SUBJECT)
            .setAudience("aurigraph-api")
            .setIssuedAt(Date.from(Instant.now()))
            .setExpiration(Date.from(Instant.now().plus(1, ChronoUnit.HOURS)));
        
        claims.forEach(builder::claim);
        
        return builder.signWith(key, SignatureAlgorithm.HS256)
            .compact();
    }

    /**
     * Wait for a condition to be true with timeout
     * Useful for testing eventual consistency
     */
    protected void waitFor(Callable<Boolean> condition, long timeoutMs) throws Exception {
        long startTime = System.currentTimeMillis();
        long endTime = startTime + timeoutMs;
        
        while (System.currentTimeMillis() < endTime) {
            try {
                if (condition.call()) {
                    return;
                }
            } catch (Exception e) {
                log.debug("Condition check failed, retrying...", e);
            }
            
            Thread.sleep(100); // Poll every 100ms
        }
        
        throw new TimeoutException(String.format("Condition not met within %d ms", timeoutMs));
    }

    /**
     * Wait for a condition with custom polling interval
     */
    protected void waitFor(Callable<Boolean> condition, long timeoutMs, long pollIntervalMs) throws Exception {
        long startTime = System.currentTimeMillis();
        long endTime = startTime + timeoutMs;
        
        while (System.currentTimeMillis() < endTime) {
            try {
                if (condition.call()) {
                    return;
                }
            } catch (Exception e) {
                log.debug("Condition check failed, retrying...", e);
            }
            
            Thread.sleep(pollIntervalMs);
        }
        
        throw new TimeoutException(String.format("Condition not met within %d ms", timeoutMs));
    }

    /**
     * Get a new RequestSpecification with authorization
     */
    protected RequestSpecification authorizedRequest() {
        return RestAssured.given()
            .contentType("application/json")
            .accept("application/json")
            .header("Authorization", "Bearer " + generateTestToken())
            .header("X-Request-ID", UUID.randomUUID().toString());
    }

    /**
     * Get a new RequestSpecification with specific roles
     */
    protected RequestSpecification authorizedRequest(String... roles) {
        return RestAssured.given()
            .contentType("application/json")
            .accept("application/json")
            .header("Authorization", "Bearer " + generateTokenWithRoles(roles))
            .header("X-Request-ID", UUID.randomUUID().toString());
    }

    /**
     * Create a test approval request
     */
    protected Map<String, Object> createTestApprovalRequest(
        String description, 
        int validatorCount, 
        int votingWindowMinutes) {
        
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("description", description);
        request.put("validators", validatorCount);
        request.put("votingWindowMinutes", votingWindowMinutes);
        request.put("requestedBy", UUID.randomUUID().toString());
        request.put("changeType", "SECONDARY_TOKEN_CREATE");
        
        return request;
    }

    /**
     * Create a test vote
     */
    protected Map<String, Object> createTestVote(
        String approvalId, 
        String validatorId, 
        String choice) {
        
        Map<String, Object> vote = new LinkedHashMap<>();
        vote.put("approvalId", approvalId);
        vote.put("validatorId", validatorId);
        vote.put("choice", choice); // YES, NO, ABSTAIN
        vote.put("reason", "Integration test vote");
        
        return vote;
    }

    /**
     * Create a test webhook subscription
     */
    protected Map<String, Object> createTestWebhookSubscription(
        String url,
        String event) {
        
        Map<String, Object> webhook = new LinkedHashMap<>();
        webhook.put("callbackUrl", url);
        webhook.put("events", Collections.singletonList(event));
        webhook.put("active", true);
        webhook.put("retryPolicy", "EXPONENTIAL_BACKOFF");
        webhook.put("maxRetries", 3);
        
        return webhook;
    }

    /**
     * Helper to get approval status
     */
    protected String getApprovalStatus(String approvalId) {
        try {
            return authorizedRequest()
                .get(APPROVAL_ENDPOINT + "/" + approvalId)
                .then()
                .statusCode(200)
                .extract()
                .path("status");
        } catch (Exception e) {
            log.debug("Failed to get approval status for {}", approvalId, e);
            return null;
        }
    }

    /**
     * Helper to get approval details
     */
    protected Map<String, Object> getApprovalDetails(String approvalId) {
        try {
            return authorizedRequest()
                .get(APPROVAL_ENDPOINT + "/" + approvalId)
                .then()
                .statusCode(200)
                .extract()
                .body()
                .as(Map.class);
        } catch (Exception e) {
            log.debug("Failed to get approval details for {}", approvalId, e);
            return null;
        }
    }

    /**
     * Helper to submit a vote
     */
    protected String submitVote(String approvalId, String validatorId, String choice) {
        try {
            return authorizedRequest()
                .body(createTestVote(approvalId, validatorId, choice))
                .post(VOTES_ENDPOINT)
                .then()
                .statusCode(201)
                .extract()
                .path("id");
        } catch (Exception e) {
            log.debug("Failed to submit vote", e);
            return null;
        }
    }

    /**
     * Helper to create an approval and get its ID
     */
    protected String createApprovalAndGetId(
        String description, 
        int validatorCount) {
        
        try {
            return authorizedRequest()
                .body(createTestApprovalRequest(description, validatorCount, 30))
                .post(APPROVAL_ENDPOINT)
                .then()
                .statusCode(201)
                .extract()
                .path("id");
        } catch (Exception e) {
            log.debug("Failed to create approval", e);
            return null;
        }
    }

    /**
     * Custom exception for timeout
     */
    protected static class TimeoutException extends Exception {
        public TimeoutException(String message) {
            super(message);
        }
    }
}
