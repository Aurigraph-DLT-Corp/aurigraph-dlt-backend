package io.aurigraph.v11.testing.builders;

import io.aurigraph.v11.token.secondary.ValidatorVote;
import io.aurigraph.v11.token.secondary.VoteChoice;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Fluent builder for ValidatorVote test objects.
 *
 * Provides a readable way to construct test vote objects for approval voting scenarios.
 *
 * Usage Example:
 * <pre>
 * ValidatorVote vote = new ValidatorVoteTestBuilder()
 *     .withValidatorId("validator-1")
 *     .withVote(VoteChoice.YES)
 *     .withJustification("Approved based on technical review")
 *     .build();
 * </pre>
 *
 * Convenience Methods:
 * <pre>
 * // Approve
 * new ValidatorVoteTestBuilder().approves().build()
 *
 * // Reject
 * new ValidatorVoteTestBuilder().rejects().build()
 *
 * // Abstain
 * new ValidatorVoteTestBuilder().abstains().build()
 * </pre>
 */
public class ValidatorVoteTestBuilder {

    private String validatorId = UUID.randomUUID().toString();
    private VoteChoice vote = VoteChoice.YES;
    private LocalDateTime submittedAt = LocalDateTime.now();
    private String justification = "Test vote";

    /**
     * Set the validator ID who is voting.
     */
    public ValidatorVoteTestBuilder withValidatorId(String validatorId) {
        this.validatorId = validatorId;
        return this;
    }

    /**
     * Set the vote choice (YES, NO, ABSTAIN).
     */
    public ValidatorVoteTestBuilder withVote(VoteChoice vote) {
        this.vote = vote;
        return this;
    }

    /**
     * Convenience: Set vote to YES (approval).
     */
    public ValidatorVoteTestBuilder approves() {
        this.vote = VoteChoice.YES;
        return this;
    }

    /**
     * Convenience: Set vote to NO (rejection).
     */
    public ValidatorVoteTestBuilder rejects() {
        this.vote = VoteChoice.NO;
        return this;
    }

    /**
     * Convenience: Set vote to ABSTAIN.
     */
    public ValidatorVoteTestBuilder abstains() {
        this.vote = VoteChoice.ABSTAIN;
        return this;
    }

    /**
     * Set the submission timestamp.
     */
    public ValidatorVoteTestBuilder withSubmittedAt(LocalDateTime submittedAt) {
        this.submittedAt = submittedAt;
        return this;
    }

    /**
     * Set the justification/reason for the vote.
     */
    public ValidatorVoteTestBuilder withJustification(String justification) {
        this.justification = justification;
        return this;
    }

    /**
     * Build the ValidatorVote object.
     */
    public ValidatorVote build() {
        ValidatorVote voteObj = new ValidatorVote();
        voteObj.validatorId = validatorId;
        voteObj.vote = vote;
        voteObj.votedAt = submittedAt;
        voteObj.reason = justification;
        voteObj.voteId = UUID.randomUUID();
        voteObj.approvalRequestId = UUID.randomUUID();
        return voteObj;
    }

}
