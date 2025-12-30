package io.aurigraph.v11.crm.resource;

import io.aurigraph.v11.crm.dto.CreateLeadRequest;
import io.aurigraph.v11.crm.dto.LeadResponse;
import io.aurigraph.v11.crm.entity.Lead;
import io.aurigraph.v11.crm.service.LeadService;
import lombok.extern.slf4j.Slf4j;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * REST API endpoints for lead management
 * Base path: /api/v11/crm/leads
 */
@Slf4j
@Path("/api/v11/crm/leads")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CrmLeadResource {

    @Inject
    LeadService leadService;

    /**
     * Create a new lead
     * POST /api/v11/crm/leads
     */
    @POST
    public Response createLead(CreateLeadRequest request) {
        try {
            if (request == null || request.getEmail() == null || request.getEmail().isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new ErrorResponse("Email is required"))
                        .build();
            }

            Lead lead = leadService.createLead(request);
            LeadResponse response = LeadResponse.fromEntity(lead);

            return Response.status(Response.Status.CREATED)
                    .entity(response)
                    .build();
        } catch (IllegalArgumentException e) {
            log.warn("Validation error creating lead: {}", e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse(e.getMessage()))
                    .build();
        } catch (Exception e) {
            log.error("Error creating lead", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Error creating lead"))
                    .build();
        }
    }

    /**
     * Get lead by ID
     * GET /api/v11/crm/leads/{id}
     */
    @GET
    @Path("/{id}")
    public Response getLead(@PathParam("id") UUID id) {
        try {
            Lead lead = leadService.getLead(id);
            LeadResponse response = LeadResponse.fromEntity(lead);
            return Response.ok(response).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorResponse(e.getMessage()))
                    .build();
        }
    }

    /**
     * Get all leads
     * GET /api/v11/crm/leads
     */
    @GET
    public Response getAllLeads() {
        List<LeadResponse> leads = leadService.getAllLeads()
                .stream()
                .map(LeadResponse::fromEntity)
                .collect(Collectors.toList());
        return Response.ok(leads).build();
    }

    /**
     * Get leads by status
     * GET /api/v11/crm/leads/status/{status}
     */
    @GET
    @Path("/status/{status}")
    public Response getLeadsByStatus(@PathParam("status") String status) {
        try {
            Lead.LeadStatus leadStatus = Lead.LeadStatus.valueOf(status.toUpperCase());
            List<LeadResponse> leads = leadService.getLeadsByStatus(leadStatus)
                    .stream()
                    .map(LeadResponse::fromEntity)
                    .collect(Collectors.toList());
            return Response.ok(leads).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("Invalid status: " + status))
                    .build();
        }
    }

    /**
     * Get high-value leads
     * GET /api/v11/crm/leads/high-value?minScore=50
     */
    @GET
    @Path("/high-value")
    public Response getHighValueLeads(@QueryParam("minScore") @DefaultValue("50") Integer minScore) {
        List<LeadResponse> leads = leadService.getHighValueLeads(minScore)
                .stream()
                .map(LeadResponse::fromEntity)
                .collect(Collectors.toList());
        return Response.ok(leads).build();
    }

    /**
     * Get leads needing follow-up
     * GET /api/v11/crm/leads/follow-up
     */
    @GET
    @Path("/follow-up")
    public Response getLeadsNeedingFollowUp() {
        List<LeadResponse> leads = leadService.getLeadsNeedingFollowUp()
                .stream()
                .map(LeadResponse::fromEntity)
                .collect(Collectors.toList());
        return Response.ok(leads).build();
    }

    /**
     * Update lead status
     * PUT /api/v11/crm/leads/{id}/status
     */
    @PUT
    @Path("/{id}/status")
    public Response updateLeadStatus(@PathParam("id") UUID id, @QueryParam("status") String status) {
        try {
            Lead.LeadStatus leadStatus = Lead.LeadStatus.valueOf(status.toUpperCase());
            leadService.updateLeadStatus(id, leadStatus);
            Lead lead = leadService.getLead(id);
            return Response.ok(LeadResponse.fromEntity(lead)).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse(e.getMessage()))
                    .build();
        }
    }

    /**
     * Update lead score
     * PUT /api/v11/crm/leads/{id}/score
     */
    @PUT
    @Path("/{id}/score")
    public Response updateLeadScore(@PathParam("id") UUID id, @QueryParam("score") Integer score) {
        try {
            if (score == null || score < 0) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new ErrorResponse("Score must be a non-negative integer"))
                        .build();
            }
            leadService.updateLeadScore(id, score);
            Lead lead = leadService.getLead(id);
            return Response.ok(LeadResponse.fromEntity(lead)).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorResponse(e.getMessage()))
                    .build();
        }
    }

    /**
     * Assign lead to user
     * POST /api/v11/crm/leads/{id}/assign
     */
    @POST
    @Path("/{id}/assign")
    public Response assignLead(@PathParam("id") UUID leadId, @QueryParam("userId") UUID userId) {
        try {
            if (userId == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new ErrorResponse("userId is required"))
                        .build();
            }
            leadService.assignLead(leadId, userId);
            Lead lead = leadService.getLead(leadId);
            return Response.ok(LeadResponse.fromEntity(lead)).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorResponse(e.getMessage()))
                    .build();
        }
    }

    /**
     * Verify email
     * POST /api/v11/crm/leads/{id}/verify-email
     */
    @POST
    @Path("/{id}/verify-email")
    public Response verifyEmail(@PathParam("id") UUID id) {
        try {
            leadService.verifyEmail(id);
            Lead lead = leadService.getLead(id);
            return Response.ok(LeadResponse.fromEntity(lead)).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorResponse(e.getMessage()))
                    .build();
        }
    }

    /**
     * Error response DTO
     */
    public static class ErrorResponse {
        public String message;

        public ErrorResponse(String message) {
            this.message = message;
        }
    }
}
