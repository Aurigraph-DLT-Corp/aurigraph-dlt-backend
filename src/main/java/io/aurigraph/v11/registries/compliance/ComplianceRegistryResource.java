package io.aurigraph.v11.registries.compliance;

import io.quarkus.logging.Log;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Compliance Registry REST API Resource
 *
 * Provides REST endpoints for compliance certification management, verification,
 * and regulatory status tracking. Supports multiple compliance standards and
 * certification types.
 *
 * Base Path: /api/v11/registries/compliance
 *
 * Endpoints:
 * - POST /{entityId}/certify - Add compliance certification
 * - GET /{entityId}/certifications - Get all certifications for entity
 * - GET /verify/{entityId} - Verify entity compliance status
 * - GET /expired - List expired certifications
 * - GET /renewal-window - List certifications in renewal window
 * - GET /critical-window - List certifications in critical renewal window
 * - GET /{certId} - Get certification details
 * - GET /type/{type} - Get certifications by type
 * - PUT /{certId}/renew - Renew certification
 * - DELETE /{certId} - Revoke certification
 * - GET /metrics - Get compliance metrics
 *
 * @version 11.5.0
 * @since 2025-11-14
 */
@Path("/api/v11/registries/compliance")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ComplianceRegistryResource {

    @Inject
    ComplianceRegistryService complianceService;

    // ========== POST Endpoints ==========

    /**
     * Add compliance certification for an entity
     *
     * POST /api/v11/registries/compliance/{entityId}/certify
     */
    @POST
    @Path("/{entityId}/certify")
    public Uni<Response> addCertification(
            @PathParam("entityId") String entityId,
            CertificationRequest request
    ) {
        Log.infof("REST: Adding certification for entity: %s, type: %s", 
            entityId, request.certificationType);

        if (request.certificationType == null || request.certificationType.isEmpty()) {
            return Uni.createFrom().item(Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "Certificate type is required"))
                    .build());
        }

        if (request.issuanceDate == null || request.expiryDate == null) {
            return Uni.createFrom().item(Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "Issuance and expiry dates are required"))
                    .build());
        }

        String certificationId = "CERT-" + java.util.UUID.randomUUID().toString();

        return complianceService.addCertification(
                entityId,
                request.certificationType,
                request.issuingAuthority,
                certificationId,
                request.issuanceDate,
                request.expiryDate,
                request.status
        )
                .map(entry -> Response.status(Response.Status.CREATED).entity(entry).build())
                .onFailure().recoverWithItem(error -> {
                    Log.errorf("Failed to add certification: %s", error.getMessage());
                    return Response.status(Response.Status.BAD_REQUEST)
                            .entity(Map.of("error", error.getMessage()))
                            .build();
                });
    }

    // ========== GET Endpoints ==========

    /**
     * Get all certifications for an entity
     *
     * GET /api/v11/registries/compliance/{entityId}/certifications
     */
    @GET
    @Path("/{entityId}/certifications")
    public Uni<Response> getCertifications(@PathParam("entityId") String entityId) {
        Log.infof("REST: Getting certifications for entity: %s", entityId);

        return complianceService.getCertifications(entityId)
                .map(certifications -> Response.ok(Map.of(
                        "entityId", entityId,
                        "certifications", certifications,
                        "count", certifications.size()
                )).build())
                .onFailure().recoverWithItem(error ->
                        Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                                .entity(Map.of("error", error.getMessage()))
                                .build()
                );
    }

    /**
     * Verify entity compliance status
     *
     * GET /api/v11/registries/compliance/verify/{entityId}
     * Query params: complianceLevel=1-5 (optional)
     */
    @GET
    @Path("/verify/{entityId}")
    public Uni<Response> verifyCompliance(
            @PathParam("entityId") String entityId,
            @QueryParam("complianceLevel") String complianceLevel
    ) {
        Log.infof("REST: Verifying compliance for entity: %s, level: %s", entityId, complianceLevel);

        return complianceService.verifyCompliance(entityId, complianceLevel)
                .map(result -> {
                    if (result.isCompliant()) {
                        return Response.ok(result).build();
                    } else {
                        return Response.status(Response.Status.CONFLICT).entity(result).build();
                    }
                })
                .onFailure().recoverWithItem(error ->
                        Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                                .entity(Map.of("error", error.getMessage()))
                                .build()
                );
    }

    /**
     * List expired certifications
     *
     * GET /api/v11/registries/compliance/expired
     */
    @GET
    @Path("/expired")
    public Uni<Response> getExpiredCertifications() {
        Log.info("REST: Getting expired certifications");

        return complianceService.getExpiredCertifications()
                .map(expired -> Response.ok(Map.of(
                        "expired_certifications", expired,
                        "count", expired.size()
                )).build())
                .onFailure().recoverWithItem(error ->
                        Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                                .entity(Map.of("error", error.getMessage()))
                                .build()
                );
    }

    /**
     * List certifications in renewal window
     *
     * GET /api/v11/registries/compliance/renewal-window
     */
    @GET
    @Path("/renewal-window")
    public Uni<Response> getCertificationsInRenewalWindow() {
        Log.info("REST: Getting certifications in renewal window");

        return complianceService.getCertificationsInRenewalWindow()
                .map(renewals -> Response.ok(Map.of(
                        "renewal_certifications", renewals,
                        "count", renewals.size()
                )).build())
                .onFailure().recoverWithItem(error ->
                        Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                                .entity(Map.of("error", error.getMessage()))
                                .build()
                );
    }

    /**
     * List certifications in critical renewal window (last 30 days)
     *
     * GET /api/v11/registries/compliance/critical-window
     */
    @GET
    @Path("/critical-window")
    public Uni<Response> getCertificationsInCriticalWindow() {
        Log.info("REST: Getting certifications in critical renewal window");

        return complianceService.getCertificationsInCriticalWindow()
                .map(critical -> Response.ok(Map.of(
                        "critical_certifications", critical,
                        "count", critical.size()
                )).build())
                .onFailure().recoverWithItem(error ->
                        Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                                .entity(Map.of("error", error.getMessage()))
                                .build()
                );
    }

    /**
     * Get specific certification by ID
     *
     * GET /api/v11/registries/compliance/{certId}
     */
    @GET
    @Path("/{certId}")
    public Uni<Response> getCertification(@PathParam("certId") String certId) {
        Log.infof("REST: Getting certification: %s", certId);

        return complianceService.getCertification(certId)
                .map(cert -> Response.ok(cert).build())
                .onFailure().recoverWithItem(error -> {
                    Log.warnf("Certification not found: %s", certId);
                    return Response.status(Response.Status.NOT_FOUND)
                            .entity(Map.of("error", error.getMessage()))
                            .build();
                });
    }

    /**
     * Get certifications by type
     *
     * GET /api/v11/registries/compliance/type/{certificationType}
     */
    @GET
    @Path("/type/{certificationType}")
    public Uni<Response> getCertificationsByType(@PathParam("certificationType") String certificationType) {
        Log.infof("REST: Getting certifications of type: %s", certificationType);

        return complianceService.getCertificationsByType(certificationType)
                .map(certs -> Response.ok(Map.of(
                        "type", certificationType,
                        "certifications", certs,
                        "count", certs.size()
                )).build())
                .onFailure().recoverWithItem(error ->
                        Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                                .entity(Map.of("error", error.getMessage()))
                                .build()
                );
    }

    /**
     * Get compliance metrics
     *
     * GET /api/v11/registries/compliance/metrics
     */
    @GET
    @Path("/metrics")
    public Uni<Response> getComplianceMetrics() {
        Log.info("REST: Getting compliance metrics");

        return complianceService.getComplianceMetrics()
                .map(metrics -> Response.ok(metrics).build())
                .onFailure().recoverWithItem(error ->
                        Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                                .entity(Map.of("error", error.getMessage()))
                                .build()
                );
    }

    // ========== PUT Endpoints ==========

    /**
     * Renew certification
     *
     * PUT /api/v11/registries/compliance/{certId}/renew
     */
    @PUT
    @Path("/{certId}/renew")
    public Uni<Response> renewCertification(
            @PathParam("certId") String certId,
            RenewalRequest request
    ) {
        Log.infof("REST: Renewing certification: %s", certId);

        if (request.newExpiryDate == null) {
            return Uni.createFrom().item(Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "New expiry date is required"))
                    .build());
        }

        return complianceService.renewCertification(certId, request.newExpiryDate)
                .map(entry -> Response.ok(Map.of(
                        "message", "Certification renewed successfully",
                        "certification", entry
                )).build())
                .onFailure().recoverWithItem(error -> {
                    Log.errorf("Failed to renew certification: %s", error.getMessage());
                    return Response.status(Response.Status.BAD_REQUEST)
                            .entity(Map.of("error", error.getMessage()))
                            .build();
                });
    }

    // ========== DELETE Endpoints ==========

    /**
     * Revoke certification
     *
     * DELETE /api/v11/registries/compliance/{certId}
     */
    @DELETE
    @Path("/{certId}")
    public Uni<Response> revokeCertification(@PathParam("certId") String certId) {
        Log.infof("REST: Revoking certification: %s", certId);

        return complianceService.revokeCertification(certId)
                .map(entry -> Response.ok(Map.of(
                        "message", "Certification revoked successfully",
                        "certification", entry
                )).build())
                .onFailure().recoverWithItem(error -> {
                    Log.errorf("Failed to revoke certification: %s", error.getMessage());
                    return Response.status(Response.Status.BAD_REQUEST)
                            .entity(Map.of("error", error.getMessage()))
                            .build();
                });
    }

    // ========== Health Check ==========

    /**
     * Health check endpoint
     *
     * GET /api/v11/registries/compliance/health
     */
    @GET
    @Path("/health")
    public Response health() {
        return Response.ok(Map.of(
                "status", "healthy",
                "service", "Compliance Registry",
                "timestamp", Instant.now()
        )).build();
    }

    // ========== Request/Response DTOs ==========

    /**
     * Certification request DTO
     */
    public static class CertificationRequest {
        public String certificationType;
        public String issuingAuthority;
        public Instant issuanceDate;
        public Instant expiryDate;
        public String status;
    }

    /**
     * Renewal request DTO
     */
    public static class RenewalRequest {
        public Instant newExpiryDate;
        public String reason;
    }
}
