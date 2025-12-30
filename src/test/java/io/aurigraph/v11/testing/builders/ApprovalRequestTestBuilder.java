package io.aurigraph.v11.testing.builders;

import io.aurigraph.v11.token.secondary.ApprovalStatus;
import io.aurigraph.v11.token.secondary.VVBApprovalRequest;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Fluent builder for VVBApprovalRequest test objects.
 *
 * Provides a readable way to construct test approval objects with specific configurations.
 *
 * Usage Example:
 * <pre>
 * VVBApprovalRequest approval = new ApprovalRequestTestBuilder()
 *     .withId(approvalId)
 *     .withStatus(ApprovalStatus.PENDING)
 *     .withValidators("validator-1", "validator-2", "validator-3")
 *     .withConsensusThreshold(2)
 *     .build();
 * </pre>
 *
 * Convenience Methods:
 * <pre>
 * // Approved state
 * new ApprovalRequestTestBuilder().approved().build()
 *
 * // Rejected state
 * new ApprovalRequestTestBuilder().rejected().build()
 *
 * // Expired state
 * new ApprovalRequestTestBuilder().expired().build()
 * </pre>
 */
public class ApprovalRequestTestBuilder {

    private UUID id = UUID.randomUUID();
    private UUID tokenVersionId = UUID.randomUUID();
    private ApprovalStatus status = ApprovalStatus.PENDING;
    private List<String> validators = List.of("validator-1", "validator-2", "validator-3");
    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt = LocalDateTime.now();
    private Instant expiresAt = Instant.now().plusSeconds(300);
    private int consensusThreshold = 2; // 2/3 validators needed

    /**
     * Set the approval ID.
     */
    public ApprovalRequestTestBuilder withId(UUID id) {
        this.id = id;
        return this;
    }

    /**
     * Set the token version ID this approval is for.
     */
    public ApprovalRequestTestBuilder withTokenVersionId(UUID tokenVersionId) {
        this.tokenVersionId = tokenVersionId;
        return this;
    }

    /**
     * Set the approval status.
     */
    public ApprovalRequestTestBuilder withStatus(ApprovalStatus status) {
        this.status = status;
        return this;
    }

    /**
     * Set the list of validators (variable args).
     */
    public ApprovalRequestTestBuilder withValidators(String... validators) {
        this.validators = List.of(validators);
        return this;
    }

    /**
     * Set the list of validators (list).
     */
    public ApprovalRequestTestBuilder withValidators(List<String> validators) {
        this.validators = validators;
        return this;
    }

    /**
     * Set the consensus threshold (number of votes required).
     */
    public ApprovalRequestTestBuilder withConsensusThreshold(int threshold) {
        this.consensusThreshold = threshold;
        return this;
    }

    /**
     * Set the creation timestamp.
     */
    public ApprovalRequestTestBuilder withCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    /**
     * Set the expiration time.
     */
    public ApprovalRequestTestBuilder withExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
        return this;
    }

    /**
     * Convenience: Set status to APPROVED.
     */
    public ApprovalRequestTestBuilder approved() {
        this.status = ApprovalStatus.APPROVED;
        this.updatedAt = LocalDateTime.now();
        return this;
    }

    /**
     * Convenience: Set status to REJECTED.
     */
    public ApprovalRequestTestBuilder rejected() {
        this.status = ApprovalStatus.REJECTED;
        this.updatedAt = LocalDateTime.now();
        return this;
    }

    /**
     * Convenience: Set status to EXPIRED with past expiration time.
     */
    public ApprovalRequestTestBuilder expired() {
        this.status = ApprovalStatus.EXPIRED;
        this.expiresAt = Instant.now().minusSeconds(1);
        this.updatedAt = LocalDateTime.now();
        return this;
    }

    /**
     * Build the VVBApprovalRequest object.
     */
    public VVBApprovalRequest build() {
        VVBApprovalRequest approval = new VVBApprovalRequest();
        approval.requestId = id;
        approval.tokenVersionId = tokenVersionId;
        approval.status = status;
        approval.totalValidators = validators.size();
        approval.approvalCount = 0;
        approval.createdAt = createdAt;
        approval.updatedAt = updatedAt;
        approval.votingWindowEnd = LocalDateTime.from(expiresAt.atZone(java.time.ZoneId.systemDefault()));
        return approval;
    }

}
