package io.aurigraph.v11.governance;

import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;

import java.math.BigDecimal;
import java.util.List;

/**
 * Governance REST Resource
 *
 * REST API endpoints for governance operations including proposal creation,
 * voting, and execution.
 *
 * @author SCA (Security & Cryptography Agent)
 * @version 11.0.0
 * @since Sprint 8 - Governance & Quantum Signing
 */
@Path("/api/v11/governance")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class GovernanceResource {

    private static final Logger LOG = Logger.getLogger(GovernanceResource.class);

    @Inject
    GovernanceService governanceService;

    /**
     * POST /api/v11/governance/proposals
     *
     * Create a new governance proposal
     */
    @POST
    @Path("/proposals")
    public Uni<Response> createProposal(CreateProposalRequest request) {
        LOG.infof("Creating proposal: %s (type: %s, proposer: %s)",
                 request.title, request.type, request.proposer);

        return governanceService.createProposal(
            request.title,
            request.description,
            request.type,
            request.proposer,
            request.deposit,
            request.executionPayload
        )
        .onItem().transform(proposal ->
            Response.status(Response.Status.CREATED)
                   .entity(proposal)
                   .build()
        )
        .onFailure().recoverWithItem(failure -> {
            LOG.errorf("Failed to create proposal: %s", failure.getMessage());
            return Response.status(Response.Status.BAD_REQUEST)
                          .entity(new ErrorResponse(failure.getMessage()))
                          .build();
        });
    }

    /**
     * GET /api/v11/governance/proposals
     *
     * List all proposals with optional filtering
     */
    @GET
    @Path("/proposals")
    public Uni<Response> listProposals(
            @QueryParam("status") Proposal.ProposalStatus status,
            @QueryParam("type") Proposal.ProposalType type,
            @QueryParam("limit") @DefaultValue("20") int limit,
            @QueryParam("offset") @DefaultValue("0") int offset) {

        LOG.infof("Listing proposals: status=%s, type=%s, limit=%d, offset=%d",
                 status, type, limit, offset);

        return governanceService.listProposals(status, type, limit, offset)
                .onItem().transform(proposals ->
                    Response.ok(new ProposalListResponse(proposals, proposals.size(), limit, offset))
                           .build()
                )
                .onFailure().recoverWithItem(failure -> {
                    LOG.errorf("Failed to list proposals: %s", failure.getMessage());
                    return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                                  .entity(new ErrorResponse(failure.getMessage()))
                                  .build();
                });
    }

    /**
     * GET /api/v11/governance/proposals/{id}
     *
     * Get proposal by ID
     */
    @GET
    @Path("/proposals/{id}")
    public Uni<Response> getProposal(@PathParam("id") String proposalId) {
        LOG.infof("Getting proposal: %s", proposalId);

        return governanceService.getProposal(proposalId)
                .onItem().transform(proposal ->
                    Response.ok(proposal).build()
                )
                .onFailure().recoverWithItem(failure -> {
                    LOG.errorf("Failed to get proposal %s: %s", proposalId, failure.getMessage());
                    return Response.status(Response.Status.NOT_FOUND)
                                  .entity(new ErrorResponse(failure.getMessage()))
                                  .build();
                });
    }

    /**
     * POST /api/v11/governance/proposals/{id}/vote
     *
     * Cast a vote on a proposal
     */
    @POST
    @Path("/proposals/{id}/vote")
    public Uni<Response> castVote(@PathParam("id") String proposalId, CastVoteRequest request) {
        LOG.infof("Casting vote on proposal %s: voter=%s, option=%s, power=%s",
                 proposalId, request.voter, request.option, request.votingPower);

        return governanceService.castVote(
            proposalId,
            request.voter,
            request.option,
            request.votingPower,
            null  // Private key would be handled by secure key management in production
        )
        .onItem().transform(vote ->
            Response.status(Response.Status.CREATED)
                   .entity(vote)
                   .build()
        )
        .onFailure().recoverWithItem(failure -> {
            LOG.errorf("Failed to cast vote: %s", failure.getMessage());
            return Response.status(Response.Status.BAD_REQUEST)
                          .entity(new ErrorResponse(failure.getMessage()))
                          .build();
        });
    }

    /**
     * GET /api/v11/governance/proposals/{id}/results
     *
     * Get voting results for a proposal
     */
    @GET
    @Path("/proposals/{id}/results")
    public Uni<Response> getVotingResults(@PathParam("id") String proposalId) {
        LOG.infof("Getting voting results for proposal: %s", proposalId);

        return governanceService.getVotingResults(proposalId)
                .onItem().transform(results ->
                    Response.ok(results).build()
                )
                .onFailure().recoverWithItem(failure -> {
                    LOG.errorf("Failed to get voting results for %s: %s", proposalId, failure.getMessage());
                    return Response.status(Response.Status.NOT_FOUND)
                                  .entity(new ErrorResponse(failure.getMessage()))
                                  .build();
                });
    }

    /**
     * POST /api/v11/governance/proposals/{id}/execute
     *
     * Execute a passed proposal
     */
    @POST
    @Path("/proposals/{id}/execute")
    public Uni<Response> executeProposal(@PathParam("id") String proposalId) {
        LOG.infof("Executing proposal: %s", proposalId);

        return governanceService.executeProposal(proposalId)
                .onItem().transform(result ->
                    result.success() ?
                        Response.ok(result).build() :
                        Response.status(Response.Status.BAD_REQUEST)
                               .entity(result)
                               .build()
                )
                .onFailure().recoverWithItem(failure -> {
                    LOG.errorf("Failed to execute proposal %s: %s", proposalId, failure.getMessage());
                    return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                                  .entity(new ErrorResponse(failure.getMessage()))
                                  .build();
                });
    }

    /**
     * GET /api/v11/governance/metrics
     *
     * Get governance system metrics
     */
    @GET
    @Path("/metrics")
    public Response getMetrics() {
        LOG.info("Getting governance metrics");

        GovernanceService.GovernanceMetrics metrics = governanceService.getMetrics();
        return Response.ok(metrics).build();
    }

    /**
     * GET /api/v11/governance/config
     *
     * Get governance configuration
     */
    @GET
    @Path("/config")
    public Response getConfig() {
        LOG.info("Getting governance configuration");

        // Return current configuration (in production, would be persisted)
        GovernanceConfig config = new GovernanceConfig();
        return Response.ok(config).build();
    }

    // ==================== Request/Response DTOs ====================

    /**
     * Create proposal request
     */
    public static class CreateProposalRequest {
        public String title;
        public String description;
        public Proposal.ProposalType type;
        public String proposer;
        public BigDecimal deposit;
        public String executionPayload;
    }

    /**
     * Cast vote request
     */
    public static class CastVoteRequest {
        public String voter;
        public Vote.VoteOption option;
        public BigDecimal votingPower;
        public boolean timeLocked;
        public Long lockDurationDays;
    }

    /**
     * Proposal list response
     */
    public static class ProposalListResponse {
        public List<Proposal> proposals;
        public int total;
        public int limit;
        public int offset;

        public ProposalListResponse(List<Proposal> proposals, int total, int limit, int offset) {
            this.proposals = proposals;
            this.total = total;
            this.limit = limit;
            this.offset = offset;
        }
    }

    /**
     * Error response
     */
    public static class ErrorResponse {
        public String error;
        public long timestamp;

        public ErrorResponse(String error) {
            this.error = error;
            this.timestamp = System.currentTimeMillis();
        }
    }
}
