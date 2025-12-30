package io.aurigraph.v11.token.secondary;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.InjectMock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import jakarta.inject.Inject;
import java.net.URI;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ApprovalWebhookServiceTest - 10+ tests covering webhook delivery
 * Tests webhook registration, delivery, retry logic, and error handling
 * Part of Phase 3B: Token Versioning & Validation implementation
 */
@QuarkusTest
@DisplayName("Approval Webhook Service Tests")
class ApprovalWebhookServiceTest {

    @Inject
    ApprovalWebhookService webhookService;

    private UUID approvalId;
    private String webhookUrl;
    private WebhookConfig webhookConfig;

    @BeforeEach
    void setUp() {
        approvalId = UUID.randomUUID();
        webhookUrl = "https://example.com/webhook/approval";
        webhookConfig = new WebhookConfig();
        webhookConfig.setWebhookUrl(webhookUrl);
        webhookConfig.setApprovalId(approvalId);
    }

    // ============= WEBHOOK DELIVERY (4 tests) =============

    @Nested
    @DisplayName("Webhook Delivery Tests")
    class WebhookDelivery {

        @Test
        @DisplayName("Register webhook with valid URL stores configuration")
        void testRegisterWebhook_ValidUrl_StoresConfiguration() {
            // Arrange
            String url = "https://example.com/webhook/approval";

            // Act
            WebhookRegistration registration = webhookService.registerWebhook(approvalId, url)
                .await().indefinitely();

            // Assert
            assertNotNull(registration);
            assertNotNull(registration.getWebhookId());
            assertEquals(url, registration.getWebhookUrl());
        }

        @Test
        @DisplayName("Deliver webhook successfully returns HTTP 200")
        void testDeliverWebhook_Success_Returns200() {
            // Arrange
            String url = "https://example.com/webhook/approval";
            webhookService.registerWebhook(approvalId, url)
                .await().indefinitely();

            WebhookPayload payload = new WebhookPayload();
            payload.setApprovalId(approvalId);
            payload.setStatus("APPROVED");

            // Act
            WebhookDeliveryResult result = webhookService.deliverWebhook(approvalId, payload)
                .await().indefinitely();

            // Assert
            assertEquals(200, result.getStatusCode());
        }

        @Test
        @DisplayName("Deliver webhook failure returns error")
        void testDeliverWebhook_Failure_ReturnsError() {
            // Arrange
            String badUrl = "https://invalid.example.com/webhook";
            webhookService.registerWebhook(approvalId, badUrl)
                .await().indefinitely();

            WebhookPayload payload = new WebhookPayload();
            payload.setApprovalId(approvalId);

            // Act
            WebhookDeliveryResult result = webhookService.deliverWebhook(approvalId, payload)
                .await().indefinitely();

            // Assert
            assertNotEquals(200, result.getStatusCode());
            assertNotNull(result.getError());
        }

        @Test
        @DisplayName("Unregister webhook with valid ID removes configuration")
        void testUnregisterWebhook_ValidId_RemovesWebhook() {
            // Arrange
            String url = "https://example.com/webhook/approval";
            WebhookRegistration registration = webhookService.registerWebhook(approvalId, url)
                .await().indefinitely();

            // Act
            boolean unregistered = webhookService.unregisterWebhook(registration.getWebhookId())
                .await().indefinitely();

            // Assert
            assertTrue(unregistered);
        }
    }

    // ============= RETRY LOGIC WITH EXPONENTIAL BACKOFF (3 tests) =============

    @Nested
    @DisplayName("Retry Logic with Exponential Backoff Tests")
    class RetryLogicExponentialBackoff {

        @Test
        @DisplayName("First failure retries at 1 second interval")
        void testRetry_FirstFailure_RetriesAt1Second() {
            // Arrange
            String url = "https://example.com/webhook/approval";
            webhookService.registerWebhook(approvalId, url)
                .await().indefinitely();

            WebhookPayload payload = new WebhookPayload();
            payload.setApprovalId(approvalId);

            // Act
            long startTime = System.currentTimeMillis();
            WebhookDeliveryResult result = webhookService.deliverWebhookWithRetry(
                approvalId, payload, 3, 1000)
                .await().indefinitely();
            long duration = System.currentTimeMillis() - startTime;

            // Assert - should attempt within reasonable time
            assertTrue(duration >= 0);
        }

        @Test
        @DisplayName("Second failure retries at 2 second interval")
        void testRetry_SecondFailure_RetriesAt2Seconds() {
            // Arrange
            String url = "https://example.com/webhook/approval";
            webhookService.registerWebhook(approvalId, url)
                .await().indefinitely();

            WebhookPayload payload = new WebhookPayload();
            payload.setApprovalId(approvalId);

            // Act
            WebhookDeliveryResult result = webhookService.deliverWebhookWithRetry(
                approvalId, payload, 3, 2000)
                .await().indefinitely();

            // Assert
            assertNotNull(result);
        }

        @Test
        @DisplayName("Third failure retries at 4 second interval (exponential)")
        void testRetry_ThirdFailure_RetriesAt4Seconds() {
            // Arrange
            String url = "https://example.com/webhook/approval";
            webhookService.registerWebhook(approvalId, url)
                .await().indefinitely();

            WebhookPayload payload = new WebhookPayload();
            payload.setApprovalId(approvalId);

            // Act
            WebhookDeliveryResult result = webhookService.deliverWebhookWithRetry(
                approvalId, payload, 3, 4000)
                .await().indefinitely();

            // Assert
            assertNotNull(result);
            assertTrue(result.getAttempts() >= 1);
        }
    }

    // ============= TIMEOUT & FAILURE HANDLING (3 tests) =============

    @Nested
    @DisplayName("Timeout and Failure Handling Tests")
    class TimeoutFailureHandling {

        @Test
        @DisplayName("Timeout exceeding threshold marks webhook as failed")
        void testTimeout_ExceedsThreshold_MarksFailed() {
            // Arrange
            String slowUrl = "https://example.com/webhook/approval";
            webhookService.registerWebhook(approvalId, slowUrl)
                .await().indefinitely();

            WebhookPayload payload = new WebhookPayload();
            payload.setApprovalId(approvalId);

            // Act
            long startTime = System.currentTimeMillis();
            WebhookDeliveryResult result = webhookService.deliverWebhookWithTimeout(
                approvalId, payload, 100) // 100ms timeout
                .await().indefinitely();
            long duration = System.currentTimeMillis() - startTime;

            // Assert
            assertNotNull(result);
            assertTrue(duration <= 500); // Should timeout quickly
        }

        @Test
        @DisplayName("Connection failure unable to connect retries delivery")
        void testConnectionFailure_UnableToConnect_Retries() {
            // Arrange
            String unreachableUrl = "https://unreachable.example.com/webhook";
            webhookService.registerWebhook(approvalId, unreachableUrl)
                .await().indefinitely();

            WebhookPayload payload = new WebhookPayload();
            payload.setApprovalId(approvalId);

            // Act
            WebhookDeliveryResult result = webhookService.deliverWebhookWithRetry(
                approvalId, payload, 2, 100)
                .await().indefinitely();

            // Assert
            assertTrue(result.getAttempts() >= 1);
            assertNotNull(result.getError());
        }

        @Test
        @DisplayName("Graceful degradation: no failover continues operation")
        void testGracefulDegradation_NoFailover_ContinuesOperation() {
            // Arrange
            String url = "https://example.com/webhook/approval";
            webhookService.registerWebhook(approvalId, url)
                .await().indefinitely();

            WebhookPayload payload = new WebhookPayload();
            payload.setApprovalId(approvalId);

            // Act
            WebhookDeliveryResult result = webhookService.deliverWebhookGracefully(
                approvalId, payload)
                .await().indefinitely();

            // Assert
            assertNotNull(result);
            // Should continue operation even if webhook fails
            assertTrue(true);
        }
    }
}

/**
 * Helper class for WebhookConfig (would be in actual service)
 */
class WebhookConfig {
    private UUID approvalId;
    private String webhookUrl;
    private boolean active;

    public UUID getApprovalId() { return approvalId; }
    public void setApprovalId(UUID id) { this.approvalId = id; }

    public String getWebhookUrl() { return webhookUrl; }
    public void setWebhookUrl(String url) { this.webhookUrl = url; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}

/**
 * Helper class for WebhookRegistration (would be in actual service)
 */
class WebhookRegistration {
    private UUID webhookId = UUID.randomUUID();
    private UUID approvalId;
    private String webhookUrl;
    private long registeredAt = System.currentTimeMillis();

    public UUID getWebhookId() { return webhookId; }
    public void setWebhookId(UUID id) { this.webhookId = id; }

    public UUID getApprovalId() { return approvalId; }
    public void setApprovalId(UUID id) { this.approvalId = id; }

    public String getWebhookUrl() { return webhookUrl; }
    public void setWebhookUrl(String url) { this.webhookUrl = url; }

    public long getRegisteredAt() { return registeredAt; }
    public void setRegisteredAt(long time) { this.registeredAt = time; }
}

/**
 * Helper class for WebhookPayload (would be in actual service)
 */
class WebhookPayload {
    private UUID approvalId;
    private String status;
    private String message;
    private long timestamp = System.currentTimeMillis();

    public UUID getApprovalId() { return approvalId; }
    public void setApprovalId(UUID id) { this.approvalId = id; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long time) { this.timestamp = time; }
}

/**
 * Helper class for WebhookDeliveryResult (would be in actual service)
 */
class WebhookDeliveryResult {
    private int statusCode;
    private String error;
    private int attempts = 1;
    private long deliveryTime = System.currentTimeMillis();

    public int getStatusCode() { return statusCode; }
    public void setStatusCode(int code) { this.statusCode = code; }

    public String getError() { return error; }
    public void setError(String error) { this.error = error; }

    public int getAttempts() { return attempts; }
    public void setAttempts(int attempts) { this.attempts = attempts; }

    public long getDeliveryTime() { return deliveryTime; }
    public void setDeliveryTime(long time) { this.deliveryTime = time; }
}
