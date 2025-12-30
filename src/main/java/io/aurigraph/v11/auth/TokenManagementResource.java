package io.aurigraph.v11.auth;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * TokenManagementResource
 *
 * REST API endpoints for JWT token management:
 * - List active/all tokens for authenticated user
 * - Get token statistics and metrics
 * - Revoke specific tokens
 * - Revoke all tokens (logout all devices)
 * - View token audit trail
 *
 * Base Path: /api/v11/auth/tokens
 */
@Path("/api/v11/auth/tokens")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class TokenManagementResource {

    private static final Logger LOG = Logger.getLogger(TokenManagementResource.class);

    @Inject
    AuthTokenService authTokenService;

    /**
     * List all active tokens for the authenticated user
     *
     * GET /api/v11/tokens
     * Returns paginated list of active tokens
     */
    @GET
    @Path("/active")
    public Response getActiveTokens(
            @QueryParam("userId") String userId,
            @QueryParam("page") @DefaultValue("1") int page,
            @QueryParam("pageSize") @DefaultValue("10") int pageSize) {
        try {
            if (userId == null || userId.isBlank()) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("userId parameter is required"))
                    .build();
            }

            List<AuthToken> tokens = authTokenService.getActiveTokens(userId);

            // Apply pagination
            int startIdx = (page - 1) * pageSize;
            int endIdx = Math.min(startIdx + pageSize, tokens.size());
            List<AuthToken> paginatedTokens = tokens.subList(startIdx, endIdx);

            ActiveTokensResponse response = new ActiveTokensResponse(
                paginatedTokens,
                page,
                pageSize,
                tokens.size(),
                (int) Math.ceil((double) tokens.size() / pageSize)
            );

            LOG.infof("✅ Retrieved %d active tokens for user %s", tokens.size(), userId);
            return Response.ok(response).build();

        } catch (Exception e) {
            LOG.errorf(e, "Error retrieving active tokens");
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ErrorResponse("Failed to retrieve tokens: " + e.getMessage()))
                .build();
        }
    }

    /**
     * Get token statistics and metrics
     *
     * GET /api/v11/tokens/stats
     * Returns aggregated token statistics for user
     */
    @GET
    @Path("/stats")
    public Response getTokenStatistics(@QueryParam("userId") String userId) {
        try {
            if (userId == null || userId.isBlank()) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("userId parameter is required"))
                    .build();
            }

            List<AuthToken> allTokens = authTokenService.getAllTokensForUser(userId);

            // Calculate statistics
            long activeCount = allTokens.stream()
                .filter(t -> t.status == AuthToken.TokenStatus.ACTIVE && !t.isRevoked)
                .count();

            long accessTokenCount = allTokens.stream()
                .filter(t -> t.tokenType == AuthToken.TokenType.ACCESS && t.status == AuthToken.TokenStatus.ACTIVE)
                .count();

            long refreshTokenCount = allTokens.stream()
                .filter(t -> t.tokenType == AuthToken.TokenType.REFRESH && t.status == AuthToken.TokenStatus.ACTIVE)
                .count();

            long revokedCount = allTokens.stream()
                .filter(t -> t.isRevoked)
                .count();

            long expiredCount = allTokens.stream()
                .filter(t -> t.status == AuthToken.TokenStatus.EXPIRED)
                .count();

            // Get unique IPs
            Set<String> uniqueIps = allTokens.stream()
                .map(t -> t.clientIp)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

            // Get last used token
            LocalDateTime lastUsed = allTokens.stream()
                .map(t -> t.lastUsedAt != null ? t.lastUsedAt : t.createdAt)
                .max(LocalDateTime::compareTo)
                .orElse(null);

            TokenStatisticsResponse stats = new TokenStatisticsResponse(
                userId,
                activeCount,
                accessTokenCount,
                refreshTokenCount,
                revokedCount,
                expiredCount,
                uniqueIps.size(),
                lastUsed,
                allTokens.size()
            );

            LOG.infof("✅ Retrieved statistics for user %s: %d active, %d revoked", userId, activeCount, revokedCount);
            return Response.ok(stats).build();

        } catch (Exception e) {
            LOG.errorf(e, "Error retrieving token statistics");
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ErrorResponse("Failed to retrieve statistics: " + e.getMessage()))
                .build();
        }
    }

    /**
     * Revoke a specific token by token ID
     *
     * DELETE /api/v11/tokens/{tokenId}
     * Revokes a specific token with optional reason
     */
    @DELETE
    @Path("/{tokenId}")
    public Response revokeToken(
            @PathParam("tokenId") String tokenId,
            @QueryParam("reason") @DefaultValue("User revocation") String reason) {
        try {
            if (tokenId == null || tokenId.isBlank()) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("tokenId parameter is required"))
                    .build();
            }

            Optional<AuthToken> tokenOptional = authTokenService.getTokenById(tokenId);
            if (!tokenOptional.isPresent()) {
                return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorResponse("Token not found"))
                    .build();
            }

            AuthToken token = tokenOptional.get();
            authTokenService.revokeToken(token.tokenHash, reason);

            LOG.infof("✅ Token %s revoked for user %s: %s", tokenId, token.userEmail, reason);

            return Response.ok(new SuccessResponse("Token revoked successfully"))
                .build();

        } catch (Exception e) {
            LOG.errorf(e, "Error revoking token %s", tokenId);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ErrorResponse("Failed to revoke token: " + e.getMessage()))
                .build();
        }
    }

    /**
     * Revoke all tokens for user (logout all devices)
     *
     * DELETE /api/v11/tokens
     * Revokes all active tokens for the specified user
     */
    @DELETE
    @Path("/all/{userId}")
    public Response revokeAllTokens(
            @PathParam("userId") String userId,
            @QueryParam("reason") @DefaultValue("User revocation - logout all devices") String reason) {
        try {
            if (userId == null || userId.isBlank()) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("userId parameter is required"))
                    .build();
            }

            int revokedCount = authTokenService.revokeAllTokensForUser(userId, reason);

            LOG.infof("✅ Revoked %d tokens for user %s: %s", revokedCount, userId, reason);

            return Response.ok(new RevokeAllResponse(revokedCount, reason))
                .build();

        } catch (Exception e) {
            LOG.errorf(e, "Error revoking all tokens for user %s", userId);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ErrorResponse("Failed to revoke tokens: " + e.getMessage()))
                .build();
        }
    }

    /**
     * Get detailed information about a specific token
     *
     * GET /api/v11/tokens/{tokenId}/details
     */
    @GET
    @Path("/{tokenId}/details")
    public Response getTokenDetails(@PathParam("tokenId") String tokenId) {
        try {
            if (tokenId == null || tokenId.isBlank()) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("tokenId parameter is required"))
                    .build();
            }

            Optional<AuthToken> tokenOptional = authTokenService.getTokenById(tokenId);
            if (!tokenOptional.isPresent()) {
                return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorResponse("Token not found"))
                    .build();
            }

            AuthToken token = tokenOptional.get();

            // Calculate remaining time
            LocalDateTime now = LocalDateTime.now();
            long minutesRemaining = ChronoUnit.MINUTES.between(now, token.expiresAt);

            TokenDetailResponse response = new TokenDetailResponse(
                token.tokenId,
                token.userEmail,
                token.tokenType.name(),
                token.status.name(),
                token.createdAt,
                token.expiresAt,
                token.lastUsedAt,
                minutesRemaining,
                token.isRevoked,
                token.revocationReason,
                token.revokedAt,
                token.clientIp,
                token.userAgent
            );

            LOG.infof("✅ Retrieved details for token %s", tokenId);
            return Response.ok(response).build();

        } catch (Exception e) {
            LOG.errorf(e, "Error retrieving token details");
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ErrorResponse("Failed to retrieve token details: " + e.getMessage()))
                .build();
        }
    }

    /**
     * Get audit trail for token operations
     *
     * GET /api/v11/tokens/audit/{userId}
     */
    @GET
    @Path("/audit/{userId}")
    public Response getAuditTrail(
            @PathParam("userId") String userId,
            @QueryParam("limit") @DefaultValue("50") int limit) {
        try {
            if (userId == null || userId.isBlank()) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("userId parameter is required"))
                    .build();
            }

            List<AuthToken> allTokens = authTokenService.getAllTokensForUser(userId);

            AuditTrailResponse response = new AuditTrailResponse(
                userId,
                allTokens.size(),
                allTokens.stream()
                    .limit(limit)
                    .map(token -> new TokenAuditEntry(
                        token.tokenId,
                        token.tokenType.name(),
                        token.status.name(),
                        token.createdAt,
                        token.lastUsedAt,
                        token.isRevoked,
                        token.revocationReason,
                        token.clientIp,
                        token.userAgent
                    ))
                    .collect(Collectors.toList())
            );

            LOG.infof("✅ Retrieved audit trail for user %s (%d entries)", userId, allTokens.size());
            return Response.ok(response).build();

        } catch (Exception e) {
            LOG.errorf(e, "Error retrieving audit trail");
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ErrorResponse("Failed to retrieve audit trail: " + e.getMessage()))
                .build();
        }
    }

    /**
     * Get tokens by device (IP address)
     *
     * GET /api/v11/tokens/device/{clientIp}
     */
    @GET
    @Path("/device/{clientIp}")
    public Response getTokensByDevice(
            @PathParam("clientIp") String clientIp,
            @QueryParam("userId") String userId) {
        try {
            if (clientIp == null || clientIp.isBlank()) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("clientIp parameter is required"))
                    .build();
            }

            List<AuthToken> tokens;
            if (userId != null && !userId.isBlank()) {
                tokens = AuthToken.find(
                    "userId = ?1 AND clientIp = ?2",
                    userId, clientIp
                ).list();
            } else {
                tokens = AuthToken.find("clientIp", clientIp).list();
            }

            DeviceTokensResponse response = new DeviceTokensResponse(
                clientIp,
                tokens.size(),
                tokens.stream()
                    .filter(t -> t.status == AuthToken.TokenStatus.ACTIVE)
                    .count(),
                tokens
            );

            LOG.infof("✅ Retrieved %d tokens for device %s", tokens.size(), clientIp);
            return Response.ok(response).build();

        } catch (Exception e) {
            LOG.errorf(e, "Error retrieving device tokens");
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ErrorResponse("Failed to retrieve device tokens: " + e.getMessage()))
                .build();
        }
    }

    // ==================== Response DTOs ====================

    public static class ActiveTokensResponse {
        public List<AuthToken> tokens;
        public int page;
        public int pageSize;
        public int totalTokens;
        public int totalPages;

        public ActiveTokensResponse(List<AuthToken> tokens, int page, int pageSize, int totalTokens, int totalPages) {
            this.tokens = tokens;
            this.page = page;
            this.pageSize = pageSize;
            this.totalTokens = totalTokens;
            this.totalPages = totalPages;
        }

        public List<AuthToken> getTokens() { return tokens; }
        public int getPage() { return page; }
        public int getPageSize() { return pageSize; }
        public int getTotalTokens() { return totalTokens; }
        public int getTotalPages() { return totalPages; }
    }

    public static class TokenStatisticsResponse {
        public String userId;
        public long activeTokens;
        public long accessTokens;
        public long refreshTokens;
        public long revokedTokens;
        public long expiredTokens;
        public int uniqueDevices;
        public LocalDateTime lastTokenUsed;
        public long totalTokens;

        public TokenStatisticsResponse(String userId, long activeTokens, long accessTokens,
                                       long refreshTokens, long revokedTokens, long expiredTokens,
                                       int uniqueDevices, LocalDateTime lastTokenUsed, long totalTokens) {
            this.userId = userId;
            this.activeTokens = activeTokens;
            this.accessTokens = accessTokens;
            this.refreshTokens = refreshTokens;
            this.revokedTokens = revokedTokens;
            this.expiredTokens = expiredTokens;
            this.uniqueDevices = uniqueDevices;
            this.lastTokenUsed = lastTokenUsed;
            this.totalTokens = totalTokens;
        }

        public String getUserId() { return userId; }
        public long getActiveTokens() { return activeTokens; }
        public long getAccessTokens() { return accessTokens; }
        public long getRefreshTokens() { return refreshTokens; }
        public long getRevokedTokens() { return revokedTokens; }
        public long getExpiredTokens() { return expiredTokens; }
        public int getUniqueDevices() { return uniqueDevices; }
        public LocalDateTime getLastTokenUsed() { return lastTokenUsed; }
        public long getTotalTokens() { return totalTokens; }
    }

    public static class TokenDetailResponse {
        public String tokenId;
        public String userEmail;
        public String tokenType;
        public String status;
        public LocalDateTime createdAt;
        public LocalDateTime expiresAt;
        public LocalDateTime lastUsedAt;
        public long minutesRemaining;
        public boolean isRevoked;
        public String revocationReason;
        public LocalDateTime revokedAt;
        public String clientIp;
        public String userAgent;

        public TokenDetailResponse(String tokenId, String userEmail, String tokenType, String status,
                                   LocalDateTime createdAt, LocalDateTime expiresAt, LocalDateTime lastUsedAt,
                                   long minutesRemaining, boolean isRevoked, String revocationReason,
                                   LocalDateTime revokedAt, String clientIp, String userAgent) {
            this.tokenId = tokenId;
            this.userEmail = userEmail;
            this.tokenType = tokenType;
            this.status = status;
            this.createdAt = createdAt;
            this.expiresAt = expiresAt;
            this.lastUsedAt = lastUsedAt;
            this.minutesRemaining = minutesRemaining;
            this.isRevoked = isRevoked;
            this.revocationReason = revocationReason;
            this.revokedAt = revokedAt;
            this.clientIp = clientIp;
            this.userAgent = userAgent;
        }

        // Getters
        public String getTokenId() { return tokenId; }
        public String getUserEmail() { return userEmail; }
        public String getTokenType() { return tokenType; }
        public String getStatus() { return status; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public LocalDateTime getExpiresAt() { return expiresAt; }
        public LocalDateTime getLastUsedAt() { return lastUsedAt; }
        public long getMinutesRemaining() { return minutesRemaining; }
        public boolean isRevoked() { return isRevoked; }
        public String getRevocationReason() { return revocationReason; }
        public LocalDateTime getRevokedAt() { return revokedAt; }
        public String getClientIp() { return clientIp; }
        public String getUserAgent() { return userAgent; }
    }

    public static class RevokeAllResponse {
        public int revokedCount;
        public String reason;

        public RevokeAllResponse(int revokedCount, String reason) {
            this.revokedCount = revokedCount;
            this.reason = reason;
        }

        public int getRevokedCount() { return revokedCount; }
        public String getReason() { return reason; }
    }

    public static class AuditTrailResponse {
        public String userId;
        public int totalEntries;
        public List<TokenAuditEntry> entries;

        public AuditTrailResponse(String userId, int totalEntries, List<TokenAuditEntry> entries) {
            this.userId = userId;
            this.totalEntries = totalEntries;
            this.entries = entries;
        }

        public String getUserId() { return userId; }
        public int getTotalEntries() { return totalEntries; }
        public List<TokenAuditEntry> getEntries() { return entries; }
    }

    public static class TokenAuditEntry {
        public String tokenId;
        public String tokenType;
        public String status;
        public LocalDateTime createdAt;
        public LocalDateTime lastUsedAt;
        public boolean isRevoked;
        public String revocationReason;
        public String clientIp;
        public String userAgent;

        public TokenAuditEntry(String tokenId, String tokenType, String status,
                               LocalDateTime createdAt, LocalDateTime lastUsedAt,
                               boolean isRevoked, String revocationReason, String clientIp, String userAgent) {
            this.tokenId = tokenId;
            this.tokenType = tokenType;
            this.status = status;
            this.createdAt = createdAt;
            this.lastUsedAt = lastUsedAt;
            this.isRevoked = isRevoked;
            this.revocationReason = revocationReason;
            this.clientIp = clientIp;
            this.userAgent = userAgent;
        }

        // Getters
        public String getTokenId() { return tokenId; }
        public String getTokenType() { return tokenType; }
        public String getStatus() { return status; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public LocalDateTime getLastUsedAt() { return lastUsedAt; }
        public boolean isRevoked() { return isRevoked; }
        public String getRevocationReason() { return revocationReason; }
        public String getClientIp() { return clientIp; }
        public String getUserAgent() { return userAgent; }
    }

    public static class DeviceTokensResponse {
        public String clientIp;
        public int totalTokens;
        public long activeTokens;
        public List<AuthToken> tokens;

        public DeviceTokensResponse(String clientIp, int totalTokens, long activeTokens, List<AuthToken> tokens) {
            this.clientIp = clientIp;
            this.totalTokens = totalTokens;
            this.activeTokens = activeTokens;
            this.tokens = tokens;
        }

        public String getClientIp() { return clientIp; }
        public int getTotalTokens() { return totalTokens; }
        public long getActiveTokens() { return activeTokens; }
        public List<AuthToken> getTokens() { return tokens; }
    }

    public static class ErrorResponse {
        public String error;

        public ErrorResponse(String error) {
            this.error = error;
        }

        public String getError() { return error; }
    }

    public static class SuccessResponse {
        public String message;

        public SuccessResponse(String message) {
            this.message = message;
        }

        public String getMessage() { return message; }
    }
}
