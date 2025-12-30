package io.aurigraph.v11.compliance.erc3643;

import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import io.quarkus.logging.Log;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

/**
 * REST API endpoints for ERC-3643 compliance management
 * Provides endpoints for identity verification, transfer approval, and compliance reporting
 */
@Path("/api/v11/compliance/erc3643")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ComplianceResource {

    @Inject
    IdentityRegistry identityRegistry;

    @Inject
    TransferManager transferManager;

    @Inject
    ComplianceRegistry complianceRegistry;

    // ===== Identity Management Endpoints =====

    @POST
    @Path("/identities/register")
    public Uni<Response> registerIdentity(IdentityVerification verification) {
        Log.infof("Registering identity for address: %s", verification.getAddress());

        try {
            IdentityRegistry.IdentityRecord record = identityRegistry.registerIdentity(
                verification.getAddress(), verification
            );

            return Uni.createFrom().item(
                Response.status(Response.Status.CREATED)
                    .entity(Map.of(
                        "success", true,
                        "address", record.getAddress(),
                        "kycLevel", record.getKycLevel(),
                        "country", record.getCountry(),
                        "registeredAt", record.getRegistrationDate().toString()
                    ))
                    .build()
            );
        } catch (Exception e) {
            Log.errorf("Failed to register identity: %s", e.getMessage());
            return Uni.createFrom().item(
                Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", e.getMessage()))
                    .build()
            );
        }
    }

    @GET
    @Path("/identities/{address}")
    public Uni<Response> getIdentity(@PathParam("address") String address) {
        IdentityRegistry.IdentityRecord record = identityRegistry.getIdentity(address);

        if (record == null) {
            return Uni.createFrom().item(
                Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("error", "Identity not found"))
                    .build()
            );
        }

        return Uni.createFrom().item(
            Response.ok(Map.of(
                "address", record.getAddress(),
                "kycLevel", record.getKycLevel(),
                "country", record.getCountry(),
                "documentHash", record.getDocumentHash(),
                "verifier", record.getVerifierName(),
                "registeredAt", record.getRegistrationDate().toString(),
                "expiresAt", record.getExpiryDate() != null ?
                    record.getExpiryDate().toString() : null
            )).build()
        );
    }

    @GET
    @Path("/identities/{address}/valid")
    public Uni<Response> validateIdentity(@PathParam("address") String address) {
        boolean valid = identityRegistry.isValidIdentity(address);

        return Uni.createFrom().item(
            Response.ok(Map.of(
                "address", address,
                "valid", valid
            )).build()
        );
    }

    @POST
    @Path("/identities/{address}/revoke")
    public Uni<Response> revokeIdentity(
            @PathParam("address") String address,
            Map<String, String> request) {

        String reason = request.getOrDefault("reason", "No reason provided");
        boolean success = identityRegistry.revokeIdentity(address, reason);

        if (success) {
            return Uni.createFrom().item(
                Response.ok(Map.of(
                    "success", true,
                    "message", "Identity revoked: " + reason
                )).build()
            );
        } else {
            return Uni.createFrom().item(
                Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("error", "Identity not found"))
                    .build()
            );
        }
    }

    @POST
    @Path("/identities/{address}/restore")
    public Uni<Response> restoreIdentity(@PathParam("address") String address) {
        boolean success = identityRegistry.restoreIdentity(address);

        if (success) {
            return Uni.createFrom().item(
                Response.ok(Map.of(
                    "success", true,
                    "message", "Identity restored"
                )).build()
            );
        } else {
            return Uni.createFrom().item(
                Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("error", "Identity not found or not revoked"))
                    .build()
            );
        }
    }

    @GET
    @Path("/identities/stats")
    public Uni<Response> getIdentityStats() {
        IdentityRegistry.RegistryStats stats = identityRegistry.getStats();

        return Uni.createFrom().item(
            Response.ok(Map.of(
                "totalRegistered", stats.getTotalRegistered(),
                "activeIdentities", stats.getActiveIdentities(),
                "revokedIdentities", stats.getRevokedIdentities(),
                "restrictedCountries", stats.getRestrictedCountries(),
                "totalRecords", stats.getTotalRecords()
            )).build()
        );
    }

    // ===== Country Restrictions Endpoints =====

    @POST
    @Path("/countries/restrict/{countryCode}")
    public Uni<Response> restrictCountry(@PathParam("countryCode") String countryCode) {
        identityRegistry.restrictCountry(countryCode);

        return Uni.createFrom().item(
            Response.ok(Map.of(
                "success", true,
                "message", "Country restricted: " + countryCode
            )).build()
        );
    }

    @POST
    @Path("/countries/unrestrict/{countryCode}")
    public Uni<Response> unrestrictCountry(@PathParam("countryCode") String countryCode) {
        identityRegistry.unrestrictCountry(countryCode);

        return Uni.createFrom().item(
            Response.ok(Map.of(
                "success", true,
                "message", "Country unrestricted: " + countryCode
            )).build()
        );
    }

    // ===== Transfer Compliance Endpoints =====

    @POST
    @Path("/transfers/check")
    public Uni<Response> checkTransferCompliance(Map<String, Object> request) {
        String tokenId = (String) request.get("tokenId");
        String from = (String) request.get("from");
        String to = (String) request.get("to");
        BigDecimal amount = new BigDecimal(request.get("amount").toString());

        TransferManager.TransferResult result = transferManager.canTransfer(
            tokenId, from, to, amount
        );

        return Uni.createFrom().item(
            Response.ok(Map.of(
                "allowed", result.isAllowed(),
                "violations", result.getViolations()
            )).build()
        );
    }

    @POST
    @Path("/transfers/execute")
    public Uni<Response> executeTransfer(Map<String, Object> request) {
        String tokenId = (String) request.get("tokenId");
        String from = (String) request.get("from");
        String to = (String) request.get("to");
        BigDecimal amount = new BigDecimal(request.get("amount").toString());

        return transferManager.executeTransfer(tokenId, from, to, amount)
            .map(record -> Response.ok(Map.of(
                "success", true,
                "tokenId", record.getTokenId(),
                "from", record.getFrom(),
                "to", record.getTo(),
                "amount", record.getAmount(),
                "timestamp", record.getTimestamp().toString()
            )).build())
            .onFailure().recoverWithItem(error ->
                Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", error.getMessage()))
                    .build()
            );
    }

    @GET
    @Path("/transfers/stats")
    public Uni<Response> getTransferStats() {
        TransferManager.TransferStats stats = transferManager.getStats();

        return Uni.createFrom().item(
            Response.ok(Map.of(
                "totalTransfers", stats.getTotalTransfers(),
                "approvedTransfers", stats.getApprovedTransfers(),
                "rejectedTransfers", stats.getRejectedTransfers(),
                "approvalRate", String.format("%.2f%%", stats.getApprovalRate())
            )).build()
        );
    }

    @GET
    @Path("/transfers/history/{tokenId}")
    public Uni<Response> getTransferHistory(@PathParam("tokenId") String tokenId) {
        List<TransferManager.TransferRecord> history = transferManager.getTokenTransferHistory(tokenId);

        List<Object> records = history.stream()
            .map(record -> (Object) Map.of(
                "tokenId", record.getTokenId(),
                "from", record.getFrom(),
                "to", record.getTo(),
                "amount", record.getAmount(),
                "success", record.isSuccess(),
                "reason", record.getRejectReason() != null ?
                    record.getRejectReason() : "Approved",
                "timestamp", record.getTimestamp().toString()
            ))
            .toList();

        return Uni.createFrom().item(
            Response.ok(Map.of(
                "tokenId", (Object) tokenId,
                "records", records,
                "count", records.size()
            )).build()
        );
    }

    // ===== Compliance Registry Endpoints =====

    @POST
    @Path("/tokens/{tokenId}/compliance/register")
    public Uni<Response> registerTokenCompliance(
            @PathParam("tokenId") String tokenId,
            Map<String, String> request) {

        String jurisdiction = request.get("jurisdiction");
        ComplianceRegistry.ComplianceRecord record = new ComplianceRegistry.ComplianceRecord(
            tokenId, jurisdiction
        );

        String[] rules = request.get("rules").split(",");
        for (String rule : rules) {
            record.addRule(rule.trim());
        }

        ComplianceRegistry.ComplianceRecord registered = complianceRegistry.registerCompliance(
            tokenId, record
        );

        return Uni.createFrom().item(
            Response.status(Response.Status.CREATED)
                .entity(Map.of(
                    "success", true,
                    "tokenId", tokenId,
                    "jurisdiction", jurisdiction,
                    "rules", registered.getRules()
                ))
                .build()
        );
    }

    @POST
    @Path("/tokens/{tokenId}/compliance/check")
    public Uni<Response> checkTokenCompliance(@PathParam("tokenId") String tokenId) {
        ComplianceRegistry.ComplianceCheckResult result = complianceRegistry.checkCompliance(tokenId);

        return Uni.createFrom().item(
            Response.ok(Map.of(
                "tokenId", tokenId,
                "compliant", result.isCompliant(),
                "details", result.getDetails()
            )).build()
        );
    }

    @POST
    @Path("/tokens/{tokenId}/certifications/add")
    public Uni<Response> addCertification(
            @PathParam("tokenId") String tokenId,
            Map<String, String> request) {

        ComplianceRegistry.Certification cert = new ComplianceRegistry.Certification(
            request.get("name"),
            request.get("issuer"),
            Instant.now(),
            Instant.parse(request.get("expiryDate")),
            request.get("certificateHash")
        );

        complianceRegistry.addCertification(tokenId, cert);

        return Uni.createFrom().item(
            Response.ok(Map.of(
                "success", true,
                "message", "Certification added",
                "tokenId", tokenId,
                "certification", cert.getName()
            )).build()
        );
    }

    @GET
    @Path("/compliance/stats")
    public Uni<Response> getComplianceStats() {
        ComplianceRegistry.ComplianceStats stats = complianceRegistry.getStats();

        return Uni.createFrom().item(
            Response.ok(Map.of(
                "totalChecks", stats.getTotalChecks(),
                "passedChecks", stats.getPassedChecks(),
                "failedChecks", stats.getFailedChecks(),
                "complianceRate", String.format("%.2f%%", stats.getComplianceRate()),
                "totalRecords", stats.getTotalRecords(),
                "totalModules", stats.getTotalModules(),
                "auditTrailSize", stats.getAuditTrailSize()
            )).build()
        );
    }

    @GET
    @Path("/health")
    public Uni<Response> health() {
        return Uni.createFrom().item(
            Response.ok(Map.of(
                "status", "healthy",
                "service", "ERC-3643 Compliance API",
                "version", "1.0.0",
                "timestamp", Instant.now().toString()
            )).build()
        );
    }
}
