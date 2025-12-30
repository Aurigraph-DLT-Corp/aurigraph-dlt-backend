package io.aurigraph.v11.api;

import io.aurigraph.v11.token.secondary.*;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Secondary Token Versioning REST API
 *
 * Endpoints for managing secondary token versions.
 * Path: /api/v12/secondary-tokens/{tokenId}/versions
 *
 * Operations:
 * - GET /versions - Get all versions of a token
 * - GET /versions/{versionNumber} - Get specific version
 * - POST /versions - Create new version
 * - PUT /versions/{versionNumber}/activate - Activate version
 * - PUT /versions/{versionNumber}/reject - Reject version
 * - PUT /versions/{versionNumber}/archive - Archive version
 *
 * @version 12.0.0
 * @since December 23, 2025
 */
@ApplicationScoped
@Path("/api/v12/secondary-tokens/{tokenId}/versions")
@Slf4j
public class SecondaryTokenVersionResource {

    @Inject
    SecondaryTokenVersioningService versioningService;

    @Inject
    SecondaryTokenVersionRepository versionRepository;

    // =========================================================================
    // GET Operations - Retrieve Versions
    // =========================================================================

    /**
     * Get all versions of a secondary token
     * GET /api/v12/secondary-tokens/{tokenId}/versions
     *
     * @param tokenId The token ID
     * @return List of versions for the token
     */
    @GET
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<List<SecondaryTokenVersionDTO>> getVersionHistory(
            @PathParam("tokenId") UUID tokenId) {

        log.debug("Fetching version history for token {}", tokenId);

        return Uni.createFrom().item(() ->
                SecondaryTokenVersion.findBySecondaryTokenId(tokenId)
                        .stream()
                        .map(SecondaryTokenVersionDTO::fromEntity)
                        .collect(Collectors.toList())
        );
    }

    /**
     * Get specific version by number
     * GET /api/v12/secondary-tokens/{tokenId}/versions/{versionNumber}
     *
     * @param tokenId Token ID
     * @param versionNumber Version number to retrieve
     * @return Version DTO or 404
     */
    @GET
    @Path("/{versionNumber}")
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<Response> getVersion(
            @PathParam("tokenId") UUID tokenId,
            @PathParam("versionNumber") Integer versionNumber) {

        log.debug("Fetching version {} of token {}", versionNumber, tokenId);

        return Uni.createFrom().item(() -> {
            List<SecondaryTokenVersion> versions = SecondaryTokenVersion.findBySecondaryTokenId(tokenId);
            SecondaryTokenVersion version = versions.stream()
                    .filter(v -> v.getVersionNumber().equals(versionNumber))
                    .findFirst()
                    .orElse(null);

            if (version == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(new ErrorResponse("Version not found"))
                        .build();
            }

            return Response.ok(SecondaryTokenVersionDTO.fromEntity(version)).build();
        });
    }

    /**
     * Get active version
     * GET /api/v12/secondary-tokens/{tokenId}/versions/active
     *
     * @param tokenId Token ID
     * @return Active version DTO or 404
     */
    @GET
    @Path("/active")
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<Response> getActiveVersion(
            @PathParam("tokenId") UUID tokenId) {

        log.debug("Fetching active version of token {}", tokenId);

        return Uni.createFrom().item(() -> {
            SecondaryTokenVersion activeVersion = SecondaryTokenVersion.findActiveVersion(tokenId);
            if (activeVersion == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(new ErrorResponse("No active version found"))
                        .build();
            }
            return Response.ok(SecondaryTokenVersionDTO.fromEntity(activeVersion)).build();
        });
    }

    // =========================================================================
    // POST Operations - Create Versions
    // =========================================================================

    /**
     * Create new version
     * POST /api/v12/secondary-tokens/{tokenId}/versions
     *
     * @param tokenId Token ID
     * @param request Create version request
     * @return Created version DTO with 201 status
     */
    @POST
    @Path("/")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<Response> createVersion(
            @PathParam("tokenId") UUID tokenId,
            CreateVersionRequest request) {

        log.info("Creating new version for token {}", tokenId);

        if (!tokenId.equals(request.getSecondaryTokenId())) {
            return Uni.createFrom().item(() ->
                    Response.status(Response.Status.BAD_REQUEST)
                            .entity(new ErrorResponse("Token ID mismatch"))
                            .build()
            );
        }

        return versioningService.createVersion(
                tokenId,
                request.getContent(),
                request.getVvbRequired() != null && request.getVvbRequired(),
                request.getPreviousVersionId()
        )
                .map(version -> {
                    log.info("Version {} created for token {}", version.getVersionNumber(), tokenId);
                    return Response.created(
                            URI.create("/api/v12/secondary-tokens/" + tokenId + "/versions/" + version.getVersionNumber())
                    ).entity(SecondaryTokenVersionDTO.fromEntity(version)).build();
                })
                .onFailure().recoverWithItem(failure -> {
                    log.error("Failed to create version", failure);
                    return Response.status(Response.Status.BAD_REQUEST)
                            .entity(new ErrorResponse("Failed to create version: " + failure.getMessage()))
                            .build();
                });
    }

    // =========================================================================
    // PUT Operations - State Transitions
    // =========================================================================

    /**
     * Activate version
     * PUT /api/v12/secondary-tokens/{tokenId}/versions/{versionNumber}/activate
     *
     * @param tokenId Token ID
     * @param versionNumber Version number to activate
     * @return Activated version DTO
     */
    @PUT
    @Path("/{versionNumber}/activate")
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<Response> activateVersion(
            @PathParam("tokenId") UUID tokenId,
            @PathParam("versionNumber") Integer versionNumber) {

        log.info("Activating version {} of token {}", versionNumber, tokenId);

        return Uni.createFrom().item(() -> {
            SecondaryTokenVersion version = findVersionByNumber(tokenId, versionNumber);
            if (version == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(new ErrorResponse("Version not found"))
                        .build();
            }
            return version;
        })
                .flatMap(version -> {
                    if (version instanceof Response) {
                        return Uni.createFrom().item(() -> (Response) version);
                    }
                    return versioningService.activateVersion(((SecondaryTokenVersion) version).getId())
                            .map(v -> Response.ok(SecondaryTokenVersionDTO.fromEntity(v)).build());
                })
                .onFailure().recoverWithItem(failure -> {
                    log.error("Failed to activate version", failure);
                    return Response.status(Response.Status.BAD_REQUEST)
                            .entity(new ErrorResponse("Failed to activate: " + failure.getMessage()))
                            .build();
                });
    }

    /**
     * Reject version
     * PUT /api/v12/secondary-tokens/{tokenId}/versions/{versionNumber}/reject
     *
     * @param tokenId Token ID
     * @param versionNumber Version number
     * @param request Rejection request with reason
     * @return Rejected version
     */
    @PUT
    @Path("/{versionNumber}/reject")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<Response> rejectVersion(
            @PathParam("tokenId") UUID tokenId,
            @PathParam("versionNumber") Integer versionNumber,
            RejectVersionRequest request) {

        log.warn("Rejecting version {} with reason: {}", versionNumber, request.getRejectionReason());

        String rejector = request.getRejectedBy() != null ? request.getRejectedBy() : "SYSTEM";

        return Uni.createFrom().item(() -> findVersionByNumber(tokenId, versionNumber))
                .flatMap(version -> {
                    if (version == null) {
                        return Uni.createFrom().item(() -> (SecondaryTokenVersion) null);
                    }
                    return versioningService.rejectVersion(version.getId(), request.getRejectionReason(), rejector);
                })
                .map(version -> {
                    if (version == null) {
                        return Response.status(Response.Status.NOT_FOUND)
                                .entity(new ErrorResponse("Version not found"))
                                .build();
                    }
                    return Response.ok(SecondaryTokenVersionDTO.fromEntity(version)).build();
                })
                .onFailure().recoverWithItem(failure -> {
                    log.error("Failed to reject version", failure);
                    return Response.status(Response.Status.BAD_REQUEST)
                            .entity(new ErrorResponse("Rejection failed: " + failure.getMessage()))
                            .build();
                });
    }

    /**
     * Archive version
     * PUT /api/v12/secondary-tokens/{tokenId}/versions/{versionNumber}/archive
     *
     * @param tokenId Token ID
     * @param versionNumber Version number
     * @return Archived version
     */
    @PUT
    @Path("/{versionNumber}/archive")
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<Response> archiveVersion(
            @PathParam("tokenId") UUID tokenId,
            @PathParam("versionNumber") Integer versionNumber) {

        log.info("Archiving version {}", versionNumber);

        return Uni.createFrom().item(() -> findVersionByNumber(tokenId, versionNumber))
                .flatMap(version -> {
                    if (version == null) {
                        return Uni.createFrom().item(() -> (SecondaryTokenVersion) null);
                    }
                    return versioningService.archiveVersion(version.getId());
                })
                .map(version -> {
                    if (version == null) {
                        return Response.status(Response.Status.NOT_FOUND)
                                .entity(new ErrorResponse("Version not found"))
                                .build();
                    }
                    return Response.ok(SecondaryTokenVersionDTO.fromEntity(version)).build();
                })
                .onFailure().recoverWithItem(failure -> {
                    log.error("Failed to archive version", failure);
                    return Response.status(Response.Status.BAD_REQUEST)
                            .entity(new ErrorResponse("Archive failed: " + failure.getMessage()))
                            .build();
                });
    }

    // =========================================================================
    // Helper Methods
    // =========================================================================

    private SecondaryTokenVersion findVersionByNumber(UUID tokenId, Integer versionNumber) {
        List<SecondaryTokenVersion> versions = SecondaryTokenVersion.findBySecondaryTokenId(tokenId);
        return versions.stream()
                .filter(v -> v.getVersionNumber().equals(versionNumber))
                .findFirst()
                .orElse(null);
    }

    // =========================================================================
    // Error Response DTO
    // =========================================================================

    private static class ErrorResponse {
        public String error;
        public long timestamp;

        ErrorResponse(String error) {
            this.error = error;
            this.timestamp = System.currentTimeMillis();
        }
    }
}
