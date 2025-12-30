package io.aurigraph.v11.graphql;

import io.aurigraph.v11.token.secondary.ApprovalStatus;
import io.aurigraph.v11.token.secondary.ValidatorVote;
import io.aurigraph.v11.token.secondary.VVBApprovalRequest;
import org.eclipse.microprofile.graphql.Type;

import java.time.LocalDateTime;
import java.util.List;

/**
 * ApprovalDTO - GraphQL Type for Approval responses
 *
 * Maps VVBApprovalRequest to GraphQL type with all approval data.
 */
@Type
@lombok.Data
@lombok.AllArgsConstructor
public class ApprovalDTO {
    public String id;
    public ApprovalStatus status;
    public String tokenVersionId;
    public Integer totalValidators;
    public LocalDateTime votingWindowEnd;
    public List<ValidatorVote> votes;
    public LocalDateTime consensusReachedAt;
    public LocalDateTime executedAt;
    public LocalDateTime rejectedAt;
    public LocalDateTime createdAt;

    public ApprovalDTO(VVBApprovalRequest approval) {
        this.id = approval.requestId != null ? approval.requestId.toString() : null;
        this.status = approval.status;
        this.tokenVersionId = approval.tokenVersionId != null ? approval.tokenVersionId.toString() : null;
        this.totalValidators = approval.totalValidators;
        this.votingWindowEnd = approval.votingWindowEnd;
        this.votes = null; // Set from separate votes query
        this.consensusReachedAt = null; // Not present in VVBApprovalRequest
        this.executedAt = null; // Not present in VVBApprovalRequest
        this.rejectedAt = null; // Not present in VVBApprovalRequest
        this.createdAt = approval.createdAt;
    }
}
