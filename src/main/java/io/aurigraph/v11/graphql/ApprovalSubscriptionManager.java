package io.aurigraph.v11.graphql;

import io.aurigraph.v11.token.secondary.*;
import io.quarkus.logging.Log;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.operators.multi.processors.BroadcastProcessor;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.graphql.Type;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * ApprovalSubscriptionManager - Story 8 Subscription Broadcasting
 *
 * Manages GraphQL subscriptions for real-time approval updates.
 * - Broadcasts approval status changes
 * - Broadcasts vote submissions
 * - Broadcasts consensus reached notifications
 * - Broadcasts webhook delivery status
 *
 * Uses Mutiny processors for reactive streaming.
 */
@ApplicationScoped
public class ApprovalSubscriptionManager {

    // Map of approval ID -> list of approval status change processors
    private final ConcurrentHashMap<String, BroadcastProcessor<ApprovalEvent>> approvalStatusProcessors =
            new ConcurrentHashMap<>();

    // Map of approval ID -> list of vote submission processors
    private final ConcurrentHashMap<String, BroadcastProcessor<VoteEvent>> voteSubmissionProcessors =
            new ConcurrentHashMap<>();

    // Map of approval ID -> consensus event processors
    private final ConcurrentHashMap<String, BroadcastProcessor<ConsensusEvent>> consensusProcessors =
            new ConcurrentHashMap<>();

    // Map of webhook ID -> webhook event processors
    private final ConcurrentHashMap<String, BroadcastProcessor<WebhookEvent>> webhookProcessors =
            new ConcurrentHashMap<>();

    /**
     * Subscribe to approval status changes for a specific approval
     */
    public Multi<ApprovalEvent> subscribeToApprovalStatusChanges(String approvalId) {
        Log.infof("New subscription: approval status changes for %s", approvalId);

        BroadcastProcessor<ApprovalEvent> processor = approvalStatusProcessors
                .computeIfAbsent(approvalId, k -> BroadcastProcessor.create());

        return processor.onOverflow().buffer(100);
    }

    /**
     * Subscribe to vote submissions for a specific approval
     */
    public Multi<VoteEvent> subscribeToVoteSubmissions(String approvalId) {
        Log.infof("New subscription: vote submissions for %s", approvalId);

        BroadcastProcessor<VoteEvent> processor = voteSubmissionProcessors
                .computeIfAbsent(approvalId, k -> BroadcastProcessor.create());

        return processor.onOverflow().buffer(100);
    }

    /**
     * Subscribe to consensus reached events for a specific approval
     */
    public Multi<ConsensusEvent> subscribeToConsensusEvents(String approvalId) {
        Log.infof("New subscription: consensus events for %s", approvalId);

        BroadcastProcessor<ConsensusEvent> processor = consensusProcessors
                .computeIfAbsent(approvalId, k -> BroadcastProcessor.create());

        return processor.onOverflow().buffer(100);
    }

    /**
     * Subscribe to webhook delivery status updates
     */
    public Multi<WebhookEvent> subscribeToWebhookDelivery(String webhookId) {
        Log.infof("New subscription: webhook delivery for %s", webhookId);

        BroadcastProcessor<WebhookEvent> processor = webhookProcessors
                .computeIfAbsent(webhookId, k -> BroadcastProcessor.create());

        return processor.onOverflow().buffer(100);
    }

    // ============================================================================
    // BROADCAST METHODS (called when events occur)
    // ============================================================================

    /**
     * Broadcast approval status change to all subscribers
     */
    public void broadcastApprovalStatusChange(String approvalId, ApprovalStatus newStatus) {
        BroadcastProcessor<ApprovalEvent> processor = approvalStatusProcessors.get(approvalId);
        if (processor != null) {
            try {
                ApprovalEvent event = new ApprovalEvent(approvalId, "STATUS_CHANGED", LocalDateTime.now());
                processor.onNext(event);
                Log.debugf("Broadcasted approval status change: %s -> %s", approvalId, newStatus);
            } catch (Exception e) {
                Log.debugf("Failed to broadcast to closed processor: %s", e.getMessage());
            }
        }
    }

    /**
     * Broadcast vote submission to all subscribers
     */
    public void broadcastVoteSubmitted(String approvalId, ValidatorVote vote) {
        BroadcastProcessor<VoteEvent> processor = voteSubmissionProcessors.get(approvalId);
        if (processor != null) {
            try {
                VoteEvent event = new VoteEvent(approvalId, vote.validatorId, vote.vote, LocalDateTime.now());
                processor.onNext(event);
                Log.debugf("Broadcasted vote submission: %s by %s", approvalId, vote.validatorId);
            } catch (Exception e) {
                Log.debugf("Failed to broadcast vote to closed processor: %s", e.getMessage());
            }
        }
    }

    /**
     * Broadcast consensus reached event
     */
    public void broadcastConsensusReached(String approvalId, ConsensusResult result, int totalVotes) {
        BroadcastProcessor<ConsensusEvent> processor = consensusProcessors.get(approvalId);
        if (processor != null) {
            try {
                ConsensusEvent event = new ConsensusEvent(approvalId, result, LocalDateTime.now(), totalVotes);
                processor.onNext(event);
                Log.debugf("Broadcasted consensus reached: %s (%s)", approvalId, result);
            } catch (Exception e) {
                Log.debugf("Failed to broadcast consensus to closed processor: %s", e.getMessage());
            }
        }
    }

    /**
     * Broadcast webhook delivery status
     */
    public void broadcastWebhookDelivery(String webhookId, int httpStatus, int responseTimeMs) {
        BroadcastProcessor<WebhookEvent> processor = webhookProcessors.get(webhookId);
        if (processor != null) {
            try {
                WebhookEvent event = new WebhookEvent(webhookId, "DELIVERY_COMPLETE",
                        httpStatus, responseTimeMs, LocalDateTime.now());
                processor.onNext(event);
                Log.debugf("Broadcasted webhook delivery: %s (status=%d, time=%dms)",
                        webhookId, httpStatus, responseTimeMs);
            } catch (Exception e) {
                Log.debugf("Failed to broadcast webhook to closed processor: %s", e.getMessage());
            }
        }
    }

    /**
     * Broadcast generic approval event
     */
    public void broadcastApprovalEvent(ApprovalEvent event) {
        BroadcastProcessor<ApprovalEvent> processor = approvalStatusProcessors.get(event.approvalId);
        if (processor != null) {
            try {
                processor.onNext(event);
                Log.debugf("Broadcasted approval event: %s (%s)", event.approvalId, event.eventType);
            } catch (Exception e) {
                Log.debugf("Failed to broadcast approval event to closed processor: %s", e.getMessage());
            }
        }
    }

    /**
     * Clean up resources for a subscription
     */
    public void cleanup(String subscriptionId) {
        approvalStatusProcessors.remove(subscriptionId);
        voteSubmissionProcessors.remove(subscriptionId);
        consensusProcessors.remove(subscriptionId);
        webhookProcessors.remove(subscriptionId);
        Log.debugf("Cleaned up subscription: %s", subscriptionId);
    }

    // ============================================================================
    // EVENT TYPES (GraphQL @Type classes for subscription responses)
    // ============================================================================

    @Type
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ApprovalEvent {
        public String approvalId;
        public String eventType;
        public LocalDateTime timestamp;
    }

    @Type
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VoteEvent {
        public String approvalId;
        public String validatorId;
        public VoteChoice choice;
        public LocalDateTime timestamp;
    }

    @Type
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConsensusEvent {
        public String approvalId;
        public ConsensusResult consensusResult;
        public LocalDateTime timestamp;
        public Integer totalVotes;
    }

    @Type
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WebhookEvent {
        public String webhookId;
        public String eventType;
        public Integer httpStatus;
        public Integer responseTimeMs;
        public LocalDateTime timestamp;
    }
}
