package io.aurigraph.v11.graphql;

import org.eclipse.microprofile.graphql.Type;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * GraphQL Data Transfer Objects for Story 8 API
 *
 * Contains all DTO classes for GraphQL responses, statistics, and events.
 * All classes are package-private but marked with @Type for GraphQL schema generation.
 */

@Type
@Data
@NoArgsConstructor
@AllArgsConstructor
class ApprovalStatisticsDTO {
    public Integer totalApprovals;
    public Integer pending;
    public Integer approved;
    public Integer rejected;
    public Integer expired;
    public Double averageConsensusTimeSeconds;
    public Long timestamp;
}

@Type
@Data
@NoArgsConstructor
@AllArgsConstructor
class ValidatorStatsDTO {
    public String validatorId;
    public Integer totalVotes;
    public Integer approvesCount;
    public Integer rejectsCount;
    public Integer absorbCount;
    public Double approvalRate;
}

@Type
@Data
@NoArgsConstructor
@AllArgsConstructor
class ExecutionResponseDTO {
    public Boolean success;
    public String message;
    public String executionId;
}

@Type
@Data
@NoArgsConstructor
@AllArgsConstructor
class WebhookResponseDTO {
    public Boolean success;
    public String message;
    public String webhookId;
}

@Type
@Data
@NoArgsConstructor
@AllArgsConstructor
class ApprovalEventDTO {
    public String approvalId;
    public String eventType;
    public String timestamp;
}

@Type
@Data
@NoArgsConstructor
@AllArgsConstructor
class VoteEventDTO {
    public String approvalId;
    public String validatorId;
    public String choice;
    public String timestamp;
}

@Type
@Data
@NoArgsConstructor
@AllArgsConstructor
class ConsensusEventDTO {
    public String approvalId;
    public String result;
    public String timestamp;
    public Integer totalVotes;
}

@Type
@Data
@NoArgsConstructor
@AllArgsConstructor
class WebhookEventDTO {
    public String webhookId;
    public String eventType;
    public Integer httpStatus;
    public Integer responseTimeMs;
    public String timestamp;
}
