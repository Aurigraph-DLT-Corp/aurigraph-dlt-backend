package io.aurigraph.v11.token.secondary;

import io.quarkus.logging.Log;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;

/**
 * ApprovalWebhookService - Story 7 Component
 *
 * Manages webhook event delivery for approval system lifecycle events.
 * Implements reliable event delivery with exponential backoff retry logic.
 *
 * Key Responsibilities:
 * - Register/unregister webhook subscriptions
 * - Publish approval events to subscribed webhooks
 * - Retry failed webhook deliveries with exponential backoff
 * - Track webhook delivery success/failure metrics
 * - Handle webhook timeouts and network failures gracefully
 *
 * Event Types:
 * - APPROVAL_REQUEST_CREATED
 * - VOTE_SUBMITTED
 * - CONSENSUS_REACHED
 * - APPROVAL_EXECUTED
 * - APPROVAL_REJECTED
 * - VOTING_WINDOW_EXPIRED
 */
@ApplicationScoped
public class ApprovalWebhookService {

    @Inject
    VVBApprovalRegistry approvalRegistry;

    private static final int MAX_RETRIES = 3;
    private static final int INITIAL_BACKOFF_MS = 1000;
    private static final int MAX_BACKOFF_MS = 32000;
    private static final int WEBHOOK_TIMEOUT_SECONDS = 30;
    private static final int BATCH_SIZE = 10;

    // In-memory webhook registry (would be replaced with database in production)
    private final ConcurrentHashMap<String, WebhookSubscription> webhooks = new ConcurrentHashMap<>();
    private final BlockingQueue<WebhookEvent> eventQueue = new LinkedBlockingQueue<>(10000);
    private final ThreadPoolExecutor webhookExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(5);
    private final HttpClient httpClient = HttpClient.newBuilder()
        .connectTimeout(java.time.Duration.ofSeconds(WEBHOOK_TIMEOUT_SECONDS))
        .build();

    /**
     * Register a webhook subscription
     */
    public WebhookSubscription registerWebhook(String url, List<String> eventTypes, String secret) {
        String webhookId = UUID.randomUUID().toString();
        WebhookSubscription subscription = new WebhookSubscription(
            webhookId,
            url,
            eventTypes,
            secret,
            LocalDateTime.now()
        );
        webhooks.put(webhookId, subscription);
        Log.infof("Webhook registered: %s for events: %s", webhookId, eventTypes);
        return subscription;
    }

    /**
     * Unregister a webhook subscription
     */
    public void unregisterWebhook(String webhookId) {
        webhooks.remove(webhookId);
        Log.infof("Webhook unregistered: %s", webhookId);
    }

    /**
     * Publish approval event to registered webhooks
     */
    public void publishEvent(String eventType, String approvalId, Map<String, Object> data) {
        WebhookEvent event = new WebhookEvent(
            UUID.randomUUID().toString(),
            eventType,
            approvalId,
            data,
            LocalDateTime.now(),
            0
        );
        try {
            eventQueue.offer(event);
        } catch (Exception e) {
            Log.errorf(e, "Failed to queue webhook event: %s", eventType);
        }
    }

    /**
     * Background task to process queued webhook events
     * Runs periodically to deliver webhooks with retry logic
     */
    @Scheduled(every = "5s")
    @Transactional
    public void processWebhookQueue() {
        List<WebhookEvent> batch = new ArrayList<>();
        eventQueue.drainTo(batch, BATCH_SIZE);

        for (WebhookEvent event : batch) {
            // Filter webhooks interested in this event type
            webhooks.values().stream()
                .filter(sub -> sub.interestedInEventType(event.eventType))
                .forEach(subscription -> deliverWebhookWithRetry(event, subscription));
        }
    }

    /**
     * Deliver webhook with exponential backoff retry
     */
    private void deliverWebhookWithRetry(WebhookEvent event, WebhookSubscription subscription) {
        webhookExecutor.submit(() -> {
            int attempt = 0;
            while (attempt < MAX_RETRIES) {
                try {
                    if (deliverWebhook(event, subscription)) {
                        logWebhookSuccess(event, subscription);
                        return;
                    }
                    attempt++;
                    if (attempt < MAX_RETRIES) {
                        long backoffMs = Math.min(
                            INITIAL_BACKOFF_MS * (long) Math.pow(2, attempt - 1),
                            MAX_BACKOFF_MS
                        );
                        Thread.sleep(backoffMs);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    Log.errorf(e, "Webhook delivery failed: %s (attempt %d/%d)",
                        subscription.url, attempt + 1, MAX_RETRIES);
                    attempt++;
                }
            }
            logWebhookFailure(event, subscription, attempt);
        });
    }

    /**
     * Perform actual HTTP POST to webhook URL
     */
    private boolean deliverWebhook(WebhookEvent event, WebhookSubscription subscription) throws Exception {
        String payload = serializeWebhookPayload(event);
        String signature = generateHmacSignature(payload, subscription.secret);

        HttpRequest request = HttpRequest.newBuilder()
            .uri(new URI(subscription.url))
            .header("Content-Type", "application/json")
            .header("X-Aurigraph-Signature", "sha256=" + signature)
            .header("X-Aurigraph-Event", event.eventType)
            .header("X-Aurigraph-Delivery-ID", event.id)
            .POST(HttpRequest.BodyPublishers.ofString(payload))
            .timeout(java.time.Duration.ofSeconds(WEBHOOK_TIMEOUT_SECONDS))
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        return response.statusCode() >= 200 && response.statusCode() < 300;
    }

    /**
     * Generate HMAC-SHA256 signature for webhook verification
     */
    private String generateHmacSignature(String payload, String secret) {
        try {
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
            mac.init(new javax.crypto.spec.SecretKeySpec(secret.getBytes(), "HmacSHA256"));
            byte[] signatureBytes = mac.doFinal(payload.getBytes());
            return bytesToHex(signatureBytes);
        } catch (Exception e) {
            Log.errorf(e, "Failed to generate webhook signature");
            return "";
        }
    }

    /**
     * Convert bytes to hexadecimal string
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }

    /**
     * Serialize webhook event to JSON payload
     */
    private String serializeWebhookPayload(WebhookEvent event) {
        try {
            // Would use Jackson/JSON-B in production
            String dataJson = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(event.data);
            return String.format(
                "{\"id\":\"%s\",\"event\":\"%s\",\"approval_id\":\"%s\",\"timestamp\":\"%s\",\"data\":%s}",
                event.id,
                event.eventType,
                event.approvalId,
                event.timestamp,
                dataJson
            );
        } catch (Exception e) {
            Log.errorf(e, "Failed to serialize webhook event data");
            return String.format(
                "{\"id\":\"%s\",\"event\":\"%s\",\"approval_id\":\"%s\",\"timestamp\":\"%s\",\"data\":{}}",
                event.id,
                event.eventType,
                event.approvalId,
                event.timestamp
            );
        }
    }

    /**
     * Log successful webhook delivery
     */
    private void logWebhookSuccess(WebhookEvent event, WebhookSubscription subscription) {
        Log.infof("Webhook delivered successfully: %s (event: %s)", subscription.webhookId, event.eventType);
    }

    /**
     * Log failed webhook delivery
     */
    private void logWebhookFailure(WebhookEvent event, WebhookSubscription subscription, int attempts) {
        Log.warnf("Webhook delivery failed after %d attempts: %s (event: %s)",
            attempts, subscription.webhookId, event.eventType);
    }

    /**
     * Get webhook statistics
     */
    public WebhookStatistics getStatistics() {
        return new WebhookStatistics(
            webhooks.size(),
            eventQueue.size(),
            webhookExecutor.getActiveCount()
        );
    }

    /**
     * Webhook subscription data
     */
    public static class WebhookSubscription {
        public String webhookId;
        public String url;
        public List<String> eventTypes;
        public String secret;
        public LocalDateTime createdAt;

        public WebhookSubscription(String webhookId, String url, List<String> eventTypes,
                                   String secret, LocalDateTime createdAt) {
            this.webhookId = webhookId;
            this.url = url;
            this.eventTypes = eventTypes;
            this.secret = secret;
            this.createdAt = createdAt;
        }

        public boolean interestedInEventType(String eventType) {
            return eventTypes.contains("*") || eventTypes.contains(eventType);
        }
    }

    /**
     * Webhook event
     */
    public static class WebhookEvent {
        public String id;
        public String eventType;
        public String approvalId;
        public Map<String, Object> data;
        public LocalDateTime timestamp;
        public int deliveryAttempts;

        public WebhookEvent(String id, String eventType, String approvalId,
                           Map<String, Object> data, LocalDateTime timestamp, int deliveryAttempts) {
            this.id = id;
            this.eventType = eventType;
            this.approvalId = approvalId;
            this.data = data;
            this.timestamp = timestamp;
            this.deliveryAttempts = deliveryAttempts;
        }
    }

    /**
     * Webhook statistics
     */
    public static class WebhookStatistics {
        public int totalWebhooks;
        public int pendingEvents;
        public int activeDeliveries;

        public WebhookStatistics(int totalWebhooks, int pendingEvents, int activeDeliveries) {
            this.totalWebhooks = totalWebhooks;
            this.pendingEvents = pendingEvents;
            this.activeDeliveries = activeDeliveries;
        }
    }
}
