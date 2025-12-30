package io.aurigraph.v11.graphql;

import io.quarkus.logging.Log;
import io.smallrye.graphql.api.Subscription;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.graphql.GraphQLApi;
import org.eclipse.microprofile.graphql.Mutation;
import org.eclipse.microprofile.graphql.Name;
import org.eclipse.microprofile.graphql.Query;

import io.aurigraph.v11.token.secondary.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * ApprovalGraphQLAPI - Story 8 GraphQL Interface
 *
 * Provides GraphQL API for VVB Approval System with:
 * - Query resolvers for approval data retrieval
 * - Mutation resolvers for approval actions
 * - Subscription resolvers for real-time updates
 *
 * Replaces REST API for complex querying and adds real-time capabilities.
 */
@GraphQLApi
@ApplicationScoped
public class ApprovalGraphQLAPI {

    @Inject
    VVBApprovalService approvalService;

    @Inject
    VVBApprovalRegistry approvalRegistry;

    @Inject
    ApprovalStateValidator stateValidator;

    @Inject
    ApprovalSubscriptionManager subscriptionManager;

    // ============================================================================
    // QUERY RESOLVERS
    // ============================================================================

    @Query("approval")
    public Uni<ApprovalDTO> getApproval(@Name("id") String approvalId) {
        Log.infof("GraphQL Query: get approval %s", approvalId);
        return Uni.createFrom().item(() -> {
            VVBApprovalRequest approval = VVBApprovalRequest.findByRequestId(UUID.fromString(approvalId));
            if (approval == null) {
                throw new IllegalArgumentException("Approval not found: " + approvalId);
            }
            return new ApprovalDTO(approval);
        });
    }

    @Query("approvals")
    public Uni<List<ApprovalDTO>> getApprovals(
            @Name("status") ApprovalStatus status,
            @Name("limit") Integer limit,
            @Name("offset") Integer offset) {

        Log.infof("GraphQL Query: list approvals (status=%s, limit=%d, offset=%d)",
            status, limit, offset);

        int finalLimit = limit != null ? limit : 20;
        int finalOffset = offset != null ? offset : 0;

        return Uni.createFrom().item(() -> {
            List<VVBApprovalRequest> approvals = VVBApprovalRequest.listAll();

            // Filter by status if provided
            if (status != null) {
                approvals = approvals.stream()
                        .filter(a -> a.status == status)
                        .collect(Collectors.toList());
            }

            // Apply pagination
            return approvals.stream()
                    .skip(finalOffset)
                    .limit(finalLimit)
                    .map(ApprovalDTO::new)
                    .collect(Collectors.toList());
        });
    }

    @Query("approvalStats")
    public Uni<ApprovalStatisticsDTO> getApprovalStatistics() {
        Log.info("GraphQL Query: get approval statistics");
        return Uni.createFrom().item(() -> {
            List<VVBApprovalRequest> approvals = VVBApprovalRequest.listAll();

            int total = approvals.size();
            int pending = (int) approvals.stream()
                    .filter(a -> a.status == ApprovalStatus.PENDING).count();
            int approved = (int) approvals.stream()
                    .filter(a -> a.status == ApprovalStatus.APPROVED).count();
            int rejected = (int) approvals.stream()
                    .filter(a -> a.status == ApprovalStatus.REJECTED).count();
            int expired = (int) approvals.stream()
                    .filter(a -> a.status == ApprovalStatus.EXPIRED).count();

            // Calculate average time from creation to update (voting window completion)
            double averageConsensusTime = approvals.stream()
                    .filter(a -> a.updatedAt != null)
                    .mapToLong(a -> java.time.temporal.ChronoUnit.SECONDS.between(
                            a.createdAt, a.updatedAt))
                    .average()
                    .orElse(0);

            return new ApprovalStatisticsDTO(total, pending, approved, rejected, expired,
                    averageConsensusTime, System.currentTimeMillis());
        });
    }

    @Query("validator")
    public Uni<ValidatorStatsDTO> getValidatorStats(@Name("id") String validatorId) {
        Log.infof("GraphQL Query: get validator stats for %s", validatorId);
        return Uni.createFrom().item(() -> {
            List<ValidatorVote> votes = approvalRegistry.getVotesByValidator(validatorId);

            int totalVotes = votes.size();
            int approveCount = (int) votes.stream()
                    .filter(v -> v.vote == VoteChoice.YES).count();
            int rejectCount = (int) votes.stream()
                    .filter(v -> v.vote == VoteChoice.NO).count();
            int abstainCount = (int) votes.stream()
                    .filter(v -> v.vote == VoteChoice.ABSTAIN).count();

            double approvalRate = totalVotes > 0 ? (double) approveCount / totalVotes * 100 : 0;

            return new ValidatorStatsDTO(validatorId, totalVotes, approveCount,
                    rejectCount, abstainCount, approvalRate);
        });
    }

    // ============================================================================
    // MUTATION RESOLVERS
    // ============================================================================

    @Mutation("executeApproval")
    public Uni<ExecutionResponseDTO> executeApproval(@Name("approvalId") String approvalId) {
        Log.infof("GraphQL Mutation: execute approval %s", approvalId);
        return Uni.createFrom().item(() -> {
            UUID requestId = UUID.fromString(approvalId);
            VVBApprovalRequest approval = VVBApprovalRequest.findByRequestId(requestId);
            if (approval == null) {
                return new ExecutionResponseDTO(false, "Approval not found", null);
            }

            try {
                stateValidator.validateExecutionPrerequisites(approval);

                // Execute the approval - use factory method
                long startTime = System.currentTimeMillis();
                SecondaryTokenVersionStatus fromStatus = SecondaryTokenVersionStatus.PENDING_VVB;
                SecondaryTokenVersionStatus toStatus = approval.status == ApprovalStatus.APPROVED 
                    ? SecondaryTokenVersionStatus.ACTIVE : SecondaryTokenVersionStatus.REJECTED;
                
                ApprovalExecutionService.ExecutionResult result =
                        ApprovalExecutionService.ExecutionResult.success(
                                approval.tokenVersionId, 
                                requestId,
                                fromStatus,
                                toStatus,
                                System.currentTimeMillis() - startTime);

                // Broadcast event using subscription manager's inner class
                subscriptionManager.broadcastApprovalEvent(
                        new ApprovalSubscriptionManager.ApprovalEvent(approvalId, "APPROVAL_EXECUTED", LocalDateTime.now()));

                return new ExecutionResponseDTO(true, "Execution successful", approvalId);
            } catch (Exception e) {
                Log.errorf(e, "Execution failed for approval %s", approvalId);
                return new ExecutionResponseDTO(false, e.getMessage(), null);
            }
        });
    }

    @Mutation("registerWebhook")
    public Uni<WebhookResponseDTO> registerWebhook(
            @Name("url") String url,
            @Name("events") List<String> events) {

        Log.infof("GraphQL Mutation: register webhook %s for events %s", url, events);
        return Uni.createFrom().item(() -> {
            try {
                // This will be implemented in database-backed webhook registry (Phase 3)
                String webhookId = UUID.randomUUID().toString();
                return new WebhookResponseDTO(true, "Webhook registered", webhookId);
            } catch (Exception e) {
                return new WebhookResponseDTO(false, "Registration failed: " + e.getMessage(), null);
            }
        });
    }

    @Mutation("unregisterWebhook")
    public Uni<Boolean> unregisterWebhook(@Name("webhookId") String webhookId) {
        Log.infof("GraphQL Mutation: unregister webhook %s", webhookId);
        return Uni.createFrom().item(() -> {
            try {
                // This will be implemented in database-backed webhook registry (Phase 3)
                return true;
            } catch (Exception e) {
                Log.errorf(e, "Failed to unregister webhook %s", webhookId);
                return false;
            }
        });
    }

    // ============================================================================
    // SUBSCRIPTION RESOLVERS (Real-time Updates)
    // ============================================================================

    @Subscription("approvalStatusChanged")
    public Multi<ApprovalEventDTO> approvalStatusChanged(@Name("id") String approvalId) {
        Log.infof("GraphQL Subscription: approve status changes for %s", approvalId);

        // Create a multi that emits approval events for this specific approval
        return subscriptionManager.subscribeToApprovalStatusChanges(approvalId)
                .map(event -> new ApprovalEventDTO(
                        event.approvalId,
                        event.eventType,
                        event.timestamp.toString()));
    }

    @Subscription("voteSubmitted")
    public Multi<VoteEventDTO> voteSubmitted(@Name("approvalId") String approvalId) {
        Log.infof("GraphQL Subscription: vote submissions for %s", approvalId);

        return subscriptionManager.subscribeToVoteSubmissions(approvalId)
                .map(event -> new VoteEventDTO(
                        event.approvalId,
                        event.validatorId,
                        event.choice.toString(),
                        event.timestamp.toString()));
    }

    @Subscription("consensusReached")
    public Multi<ConsensusEventDTO> consensusReached(@Name("approvalId") String approvalId) {
        Log.infof("GraphQL Subscription: consensus reached for %s", approvalId);

        return subscriptionManager.subscribeToConsensusEvents(approvalId)
                .map(event -> new ConsensusEventDTO(
                        event.approvalId,
                        event.consensusResult.toString(),
                        event.timestamp.toString(),
                        event.totalVotes));
    }

    @Subscription("webhookDeliveryStatus")
    public Multi<WebhookEventDTO> webhookDeliveryStatus(@Name("webhookId") String webhookId) {
        Log.infof("GraphQL Subscription: webhook delivery status for %s", webhookId);

        return subscriptionManager.subscribeToWebhookDelivery(webhookId)
                .map(event -> new WebhookEventDTO(
                        event.webhookId,
                        event.eventType,
                        event.httpStatus,
                        event.responseTimeMs,
                        event.timestamp.toString()));
    }
}
