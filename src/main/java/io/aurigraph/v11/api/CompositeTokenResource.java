package io.aurigraph.v11.api;

import io.aurigraph.v11.contracts.composite.*;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import io.quarkus.logging.Log;

/**
 * REST API endpoints for Composite Token management
 * Provides comprehensive API for creating, managing, and verifying composite tokens
 */
@Path("/api/v11/composite-tokens")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CompositeTokenResource {

    @Inject
    CompositeTokenFactory compositeTokenFactory;

    // TODO: Fix - VerificationService should be a proper service class, not model
    // @Inject
    // VerificationService verificationService;

    @Inject
    VerifierRegistry verifierRegistry;

    /**
     * Create a new composite token package
     */
    @POST
    @Path("/create")
    public Uni<Response> createCompositeToken(CompositeTokenRequest request) {
        Log.infof("Creating composite token for asset: %s", request.getAssetId());
        
        // Convert to CompositeTokenCreationRequest
        CompositeTokenCreationRequest creationRequest = new CompositeTokenCreationRequest();
        creationRequest.setAssetId(request.getAssetId());
        creationRequest.setAssetType("GENERIC");
        creationRequest.setAssetValue(java.math.BigDecimal.valueOf(request.getTotalSupply()));
        creationRequest.setOwnerAddress("system");
        creationRequest.setMetadata(request.getMetadata());
        
        return compositeTokenFactory.createCompositeToken(creationRequest)
            .map(result -> {
                if (result.isSuccess()) {
                    return Response.ok(Map.of(
                        "success", true,
                        "compositeId", result.getCompositeToken().getCompositeId(),
                        "message", result.getMessage(),
                        "processingTime", result.getProcessingTime() + "ns"
                    )).build();
                } else {
                    return Response.status(Response.Status.BAD_REQUEST)
                        .entity(Map.of("success", false, "message", result.getMessage()))
                        .build();
                }
            })
            .onFailure().recoverWithItem(error -> {
                Log.errorf("Failed to create composite token: %s", error.getMessage());
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", error.getMessage()))
                    .build();
            });
    }

    /**
     * Get composite token by ID
     */
    @GET
    @Path("/{compositeId}")
    public Uni<Response> getCompositeToken(@PathParam("compositeId") String compositeId) {
        return compositeTokenFactory.getCompositeToken(compositeId)
            .map(token -> {
                if (token != null) {
                    return Response.ok(token).build();
                } else {
                    return Response.status(Response.Status.NOT_FOUND)
                        .entity(Map.of("error", "Composite token not found"))
                        .build();
                }
            });
    }

    /**
     * Get secondary tokens for a composite token
     */
    @GET
    @Path("/{compositeId}/secondary-tokens")
    public Uni<Response> getSecondaryTokens(@PathParam("compositeId") String compositeId) {
        return compositeTokenFactory.getSecondaryTokens(compositeId)
            .map(tokens -> Response.ok(Map.of(
                "compositeId", compositeId,
                "secondaryTokens", tokens,
                "count", tokens.size()
            )).build());
    }

    /**
     * Get specific secondary token
     */
    @GET
    @Path("/{compositeId}/secondary-tokens/{tokenType}")
    public Uni<Response> getSecondaryToken(
            @PathParam("compositeId") String compositeId,
            @PathParam("tokenType") String tokenType) {
        
        try {
            SecondaryTokenType type = SecondaryTokenType.valueOf(tokenType.toUpperCase());
            
            return compositeTokenFactory.getSecondaryToken(compositeId, type)
                .map(token -> {
                    if (token != null) {
                        return Response.ok(token).build();
                    } else {
                        return Response.status(Response.Status.NOT_FOUND)
                            .entity(Map.of("error", "Secondary token not found"))
                            .build();
                    }
                });
        } catch (IllegalArgumentException e) {
            return Uni.createFrom().item(
                Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "Invalid token type: " + tokenType))
                    .build()
            );
        }
    }

    /**
     * Update secondary token data
     */
    @PUT
    @Path("/{compositeId}/secondary-tokens/{tokenType}")
    public Uni<Response> updateSecondaryToken(
            @PathParam("compositeId") String compositeId,
            @PathParam("tokenType") String tokenType,
            Map<String, Object> updateData) {
        
        try {
            SecondaryTokenType type = SecondaryTokenType.valueOf(tokenType.toUpperCase());
            
            return compositeTokenFactory.updateSecondaryToken(compositeId, type, updateData)
                .map(success -> {
                    if (success) {
                        return Response.ok(Map.of(
                            "success", true,
                            "message", "Secondary token updated successfully"
                        )).build();
                    } else {
                        return Response.status(Response.Status.NOT_FOUND)
                            .entity(Map.of("error", "Token not found or update failed"))
                            .build();
                    }
                });
        } catch (IllegalArgumentException e) {
            return Uni.createFrom().item(
                Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "Invalid token type: " + tokenType))
                    .build()
            );
        }
    }

    /**
     * Initiate verification workflow
     */
    @POST
    @Path("/{compositeId}/verify")
    public Uni<Response> initiateVerification(
            @PathParam("compositeId") String compositeId,
            VerificationRequestDTO request) {
        
        Log.infof("Initiating verification for composite token: %s", compositeId);
        
        VerificationRequest verificationRequest = new VerificationRequest(
            java.util.UUID.randomUUID().toString(),  // requestId
            compositeId,                             // compositeId
            request.assetType,                       // assetType
            VerificationLevel.valueOf(request.requiredLevel),  // requiredLevel
            java.util.Collections.emptyList(),       // assignedVerifiers (will be populated later)
            java.time.Instant.now()                  // requestedAt
        );
        
        // TODO: Fix verification service implementation
        return Uni.createFrom().item(
            Response.status(Response.Status.NOT_IMPLEMENTED)
                .entity(Map.of("error", "Verification service not implemented"))
                .build()
        );
    }

    /**
     * Submit verification result
     */
    @POST
    @Path("/verification/{workflowId}/submit")
    public Uni<Response> submitVerificationResult(
            @PathParam("workflowId") String workflowId,
            @HeaderParam("X-Verifier-Id") String verifierId,
            VerificationSubmission submission) {
        
        if (verifierId == null || verifierId.isEmpty()) {
            return Uni.createFrom().item(
                Response.status(Response.Status.UNAUTHORIZED)
                    .entity(Map.of("error", "Verifier ID required in header"))
                    .build()
            );
        }
        
        // TODO: Fix verification service implementation
        return Uni.createFrom().item(
            Response.status(Response.Status.NOT_IMPLEMENTED)
                .entity(Map.of("error", "Verification service not implemented"))
                .build()
        );
        // TODO: Uncomment and fix when verification service is implemented
        // return verificationService.submitVerificationResult(workflowId, verifierId, submission);
    }

    /**
     * Get verification workflow status
     */
    @GET
    @Path("/verification/{workflowId}")
    public Uni<Response> getVerificationStatus(@PathParam("workflowId") String workflowId) {
        // TODO: Fix verification service implementation
        return Uni.createFrom().item(
            Response.status(Response.Status.NOT_IMPLEMENTED)
                .entity(Map.of("error", "Verification service not implemented"))
                .build()
        );
        // TODO: Uncomment and fix when verification service is implemented
        // return verificationService.getWorkflowStatus(workflowId);
    }

    /**
     * Get verification history for a composite token
     */
    @GET
    @Path("/{compositeId}/verification-history")
    public Uni<Response> getVerificationHistory(@PathParam("compositeId") String compositeId) {
        // TODO: Fix verification service implementation
        return Uni.createFrom().item(
            Response.status(Response.Status.NOT_IMPLEMENTED)
                .entity(Map.of("error", "Verification service not implemented"))
                .build()
        );
        // TODO: Implement when verification service is available
        // return verificationService.getVerificationHistory(compositeId)
        //     .map(workflows -> Response.ok(Map.of(
        //         "compositeId", compositeId,
        //         "verificationWorkflows", workflows,
        //         "count", workflows.size()
        //     )).build());
    }

    /**
     * Get audit trail for verification workflow
     */
    @GET
    @Path("/verification/{workflowId}/audit")
    public Uni<Response> getVerificationAudit(@PathParam("workflowId") String workflowId) {
        // TODO: Fix verification service implementation
        return Uni.createFrom().item(
            Response.status(Response.Status.NOT_IMPLEMENTED)
                .entity(Map.of("error", "Verification service not implemented"))
                .build()
        );
        // TODO: Uncomment and fix when verification service is implemented
        // return verificationService.getAuditTrail(workflowId);
    }

    /**
     * Transfer composite token ownership
     */
    @POST
    @Path("/{compositeId}/transfer")
    public Uni<Response> transferCompositeToken(
            @PathParam("compositeId") String compositeId,
            TransferRequest request) {
        
        return compositeTokenFactory.transferCompositeToken(
                compositeId, request.fromAddress, request.toAddress)
            .map(success -> {
                if (success) {
                    return Response.ok(Map.of(
                        "success", true,
                        "message", "Composite token transferred successfully",
                        "compositeId", compositeId,
                        "newOwner", request.toAddress
                    )).build();
                } else {
                    return Response.status(Response.Status.BAD_REQUEST)
                        .entity(Map.of("error", "Transfer failed"))
                        .build();
                }
            })
            .onFailure().recoverWithItem(error -> 
                Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", error.getMessage()))
                    .build()
            );
    }

    /**
     * Get composite tokens by asset type
     */
    @GET
    @Path("/by-type/{assetType}")
    public Uni<Response> getTokensByType(@PathParam("assetType") String assetType) {
        return compositeTokenFactory.getCompositeTokensByType(assetType)
            .map(tokens -> Response.ok(Map.of(
                "assetType", assetType,
                "tokens", tokens,
                "count", tokens.size()
            )).build());
    }

    /**
     * Get composite tokens by owner
     */
    @GET
    @Path("/by-owner/{ownerAddress}")
    public Uni<Response> getTokensByOwner(@PathParam("ownerAddress") String ownerAddress) {
        return compositeTokenFactory.getCompositeTokensByOwner(ownerAddress)
            .map(tokens -> Response.ok(Map.of(
                "ownerAddress", ownerAddress,
                "tokens", tokens,
                "count", tokens.size()
            )).build());
    }

    /**
     * Get factory statistics
     */
    @GET
    @Path("/stats")
    public Uni<Response> getFactoryStats() {
        return compositeTokenFactory.getStats()
            .map(stats -> Response.ok(stats).build());
    }

    /**
     * Register a new verifier
     */
    @POST
    @Path("/verifiers/register")
    public Uni<Response> registerVerifier(VerifierRegistrationDTO registration) {
        ThirdPartyVerifier verifier = new ThirdPartyVerifier(
            registration.name,
            VerifierTier.valueOf(registration.tier),
            registration.specialization,
            registration.description,
            registration.contactInfo
        );
        
        return verifierRegistry.registerVerifier(verifier)
            .map(verifierId -> Response.ok(Map.of(
                "success", true,
                "verifierId", verifierId,
                "message", "Verifier registered successfully"
            )).build())
            .onFailure().recoverWithItem(error -> 
                Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", error.getMessage()))
                    .build()
            );
    }

    /**
     * Get verifiers by tier
     */
    @GET
    @Path("/verifiers/tier/{tier}")
    public Uni<Response> getVerifiersByTier(@PathParam("tier") String tier) {
        try {
            VerifierTier verifierTier = VerifierTier.valueOf(tier.toUpperCase());
            
            return verifierRegistry.getVerifiersByTier(verifierTier)
                .map(verifiers -> Response.ok(Map.of(
                    "tier", tier,
                    "verifiers", verifiers,
                    "count", verifiers.size()
                )).build());
        } catch (IllegalArgumentException e) {
            return Uni.createFrom().item(
                Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "Invalid tier: " + tier))
                    .build()
            );
        }
    }

    /**
     * Get verifier performance metrics
     */
    @GET
    @Path("/verifiers/{verifierId}/performance")
    public Uni<Response> getVerifierPerformance(@PathParam("verifierId") String verifierId) {
        return verifierRegistry.getVerifierPerformance(verifierId)
            .map(performance -> {
                if (performance != null) {
                    return Response.ok(performance).build();
                } else {
                    return Response.status(Response.Status.NOT_FOUND)
                        .entity(Map.of("error", "Verifier not found"))
                        .build();
                }
            });
    }

    /**
     * Get verifier registry statistics
     */
    @GET
    @Path("/verifiers/stats")
    public Uni<Response> getVerifierStats() {
        return verifierRegistry.getVerifierStats()
            .map(stats -> Response.ok(stats).build());
    }

    /**
     * Health check endpoint
     */
    @GET
    @Path("/health")
    public Uni<Response> health() {
        return Uni.createFrom().item(
            Response.ok(Map.of(
                "status", "healthy",
                "service", "Composite Token API",
                "version", "1.0.0"
            )).build()
        );
    }
}

// Data Transfer Objects (DTOs)

class VerificationRequestDTO {
    public String assetType;
    public String requiredLevel;
    public String assetValue;
    public String payerAddress;
    public Integer verifierCount;
}

class TransferRequest {
    public String fromAddress;
    public String toAddress;
}

class VerifierRegistrationDTO {
    public String name;
    public String tier;
    public String specialization;
    public String description;
    public String contactInfo;
}