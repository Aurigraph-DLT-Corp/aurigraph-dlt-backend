package io.aurigraph.v11.verification;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;

/**
 * Verification Certificate REST Resource - AV11-401
 * API endpoints for verification certificate management
 */
@Path("/api/v11/verification/certificates")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class VerificationCertificateResource {

    private static final Logger LOG = Logger.getLogger(VerificationCertificateResource.class);

    @Inject
    VerificationCertificateService certificateService;

    /**
     * Generate a new verification certificate
     * POST /api/v11/verification/certificates
     */
    @POST
    public Uni<Response> generateCertificate(VerificationCertificateService.CertificateRequest request) {
        LOG.infof("📜 Generating verification certificate for entity: %s", request.entityId());

        return certificateService.generateCertificate(request)
            .map(cert -> Response.status(Response.Status.CREATED)
                .entity(cert)
                .build())
            .onFailure().recoverWithItem(error -> {
                LOG.errorf(error, "Failed to generate certificate");
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Failed to generate certificate: " + error.getMessage()))
                    .build();
            });
    }

    /**
     * Get a certificate by ID
     * GET /api/v11/verification/certificates/{id}
     */
    @GET
    @Path("/{id}")
    public Uni<Response> getCertificate(@PathParam("id") String certificateId) {
        return certificateService.getCertificate(certificateId)
            .map(cert -> Response.ok(cert).build())
            .onFailure(VerificationCertificateService.CertificateNotFoundException.class)
            .recoverWithItem(error ->
                Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorResponse("Certificate not found: " + certificateId))
                    .build()
            )
            .onFailure().recoverWithItem(error ->
                Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Error retrieving certificate: " + error.getMessage()))
                    .build()
            );
    }

    /**
     * Verify a certificate's authenticity
     * GET /api/v11/verification/certificates/{id}/verify
     */
    @GET
    @Path("/{id}/verify")
    public Uni<Response> verifyCertificate(@PathParam("id") String certificateId) {
        LOG.infof("🔍 Verifying certificate: %s", certificateId);

        return certificateService.verifyCertificate(certificateId)
            .map(result -> Response.ok(result).build())
            .onFailure(VerificationCertificateService.CertificateNotFoundException.class)
            .recoverWithItem(error ->
                Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorResponse("Certificate not found: " + certificateId))
                    .build()
            )
            .onFailure().recoverWithItem(error ->
                Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Verification failed: " + error.getMessage()))
                    .build()
            );
    }

    /**
     * Revoke a certificate
     * POST /api/v11/verification/certificates/{id}/revoke
     */
    @POST
    @Path("/{id}/revoke")
    public Uni<Response> revokeCertificate(
        @PathParam("id") String certificateId,
        RevocationRequest request
    ) {
        LOG.warnf("🚫 Revoking certificate: %s - Reason: %s", certificateId, request.reason());

        return certificateService.revokeCertificate(certificateId, request.reason())
            .map(cert -> Response.ok(cert).build())
            .onFailure(VerificationCertificateService.CertificateNotFoundException.class)
            .recoverWithItem(error ->
                Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorResponse("Certificate not found: " + certificateId))
                    .build()
            )
            .onFailure().recoverWithItem(error ->
                Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Revocation failed: " + error.getMessage()))
                    .build()
            );
    }

    /**
     * Get all certificates for an entity
     * GET /api/v11/verification/certificates/entity/{entityId}
     */
    @GET
    @Path("/entity/{entityId}")
    public Uni<Response> getCertificatesByEntity(@PathParam("entityId") String entityId) {
        return certificateService.getCertificatesByEntity(entityId)
            .map(certs -> Response.ok(new EntityCertificatesResponse(
                entityId,
                certs.size(),
                certs
            )).build());
    }

    /**
     * Get certificate statistics
     * GET /api/v11/verification/certificates/stats
     */
    @GET
    @Path("/stats")
    public Uni<Response> getStatistics() {
        return certificateService.getStatistics()
            .map(stats -> Response.ok(stats).build());
    }

    // Data classes

    public record RevocationRequest(String reason) {}

    public record EntityCertificatesResponse(
        String entityId,
        int totalCertificates,
        java.util.List<VerificationCertificateService.VerificationCertificate> certificates
    ) {}

    public record ErrorResponse(String error) {}
}
