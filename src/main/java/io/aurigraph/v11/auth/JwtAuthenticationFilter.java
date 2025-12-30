package io.aurigraph.v11.auth;

import io.aurigraph.v11.user.JwtService;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;

import java.io.IOException;

/**
 * JWT Authentication Filter
 *
 * Validates JWT tokens in Authorization header for all protected endpoints.
 * Protected endpoints require:
 * - Authorization header with "Bearer <token>" format
 * - Valid JWT signature
 * - Token not revoked in database
 * - Token not expired
 *
 * Endpoints exempt from authentication:
 * - POST /api/v11/login/authenticate
 * - GET /api/v11/login/verify (checks session)
 * - POST /api/v11/login/logout
 * - All /ws/* endpoints (WebSocket handled separately)
 *
 * @author Backend Development Agent (BDA)
 * @since V11.5.0
 */
@Provider
@Priority(Priorities.AUTHENTICATION)
public class JwtAuthenticationFilter implements ContainerRequestFilter {

    private static final Logger LOG = Logger.getLogger(JwtAuthenticationFilter.class);

    @Inject
    JwtService jwtService;

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        // Get request path
        String path = requestContext.getUriInfo().getPath();

        // Skip authentication for public endpoints
        if (isPublicEndpoint(path)) {
            LOG.debugf("Skipping authentication for public endpoint: %s", path);
            return;
        }

        // Skip WebSocket endpoints (handled separately)
        if (path.startsWith("/ws/")) {
            LOG.debugf("Skipping JWT filter for WebSocket endpoint: %s", path);
            return;
        }

        // Get Authorization header
        String authHeader = requestContext.getHeaderString(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || authHeader.isEmpty()) {
            LOG.warnf("❌ Missing Authorization header for protected endpoint: %s", path);
            abortWithUnauthorized(requestContext, "Missing Authorization header");
            return;
        }

        // Extract Bearer token
        if (!authHeader.startsWith("Bearer ")) {
            LOG.warnf("❌ Invalid Authorization header format (expected 'Bearer <token>'): %s", path);
            abortWithUnauthorized(requestContext, "Invalid Authorization header format. Expected 'Bearer <token>'");
            return;
        }

        String token = authHeader.substring("Bearer ".length());

        // Validate JWT token
        if (!jwtService.validateToken(token)) {
            LOG.warnf("❌ Invalid or expired JWT token for endpoint: %s", path);
            abortWithUnauthorized(requestContext, "Invalid or expired JWT token");
            return;
        }

        // Extract user ID and add to request context
        String userId = jwtService.getUserIdFromToken(token);
        if (userId == null) {
            LOG.warnf("❌ Could not extract user ID from JWT token for endpoint: %s", path);
            abortWithUnauthorized(requestContext, "Could not extract user ID from token");
            return;
        }

        // Store userId in request context for downstream processing
        requestContext.setProperty("userId", userId);
        requestContext.setProperty("token", token);

        LOG.debugf("✅ JWT authentication successful for user %s on endpoint: %s", userId, path);
    }

    /**
     * Check if endpoint is public (doesn't require authentication)
     *
     * Public endpoints include:
     * - Login and authentication endpoints
     * - Health check endpoints
     * - Demo endpoints (for Enterprise Portal integration)
     * - Dashboard/analytics endpoints (for Enterprise Portal)
     * - Quarkus metrics endpoints
     */
    private boolean isPublicEndpoint(String path) {
        // Authentication endpoints
        if (path.equals("/api/v11/login/authenticate") ||
            path.equals("/api/v11/login/verify") ||
            path.equals("/api/v11/login/logout") ||
            path.equals("/api/v11/health") ||
            path.equals("/api/v11/info")) {
            return true;
        }

        // Quarkus health/metrics endpoints
        if (path.startsWith("/q/")) {
            return true;
        }

        // Static assets
        if (path.equals("/") ||
            path.startsWith("/assets/") ||
            path.startsWith("/static/")) {
            return true;
        }

        // Demo endpoints for Enterprise Portal (all demo/* endpoints are public)
        // This allows the portal to access demo features without authentication
        if (path.startsWith("/api/v11/demo/")) {
            LOG.debugf("Demo endpoint detected - allowing public access: %s", path);
            return true;
        }

        // Dashboard and analytics endpoints for Enterprise Portal public access
        // These endpoints provide read-only data for the portal dashboard
        if (path.startsWith("/api/v11/analytics/") ||
            path.startsWith("/api/v11/nodes") ||
            path.startsWith("/api/v11/consensus/") ||
            path.startsWith("/api/v11/blockchain/") ||
            path.startsWith("/api/v11/network/") ||
            path.startsWith("/api/v11/blocks") ||
            path.startsWith("/api/v11/transactions") ||
            path.startsWith("/api/v11/validators") ||
            path.startsWith("/api/v11/performance/") ||
            path.startsWith("/api/v11/live/")) {
            LOG.debugf("Dashboard endpoint detected - allowing public access: %s", path);
            return true;
        }

        // Portal API endpoints for Enterprise Portal (read-only data)
        // These are handled by PortalAPIGateway and provide demo/mock data
        if (path.equals("/api/v11/tokens") ||
            path.startsWith("/api/v11/tokens/") ||
            path.startsWith("/api/v11/rwa/") ||
            path.startsWith("/api/v11/staking/") ||
            path.startsWith("/api/v11/contracts/") ||
            path.startsWith("/api/v11/governance/") ||
            path.equals("/api/v11/stats")) {
            LOG.debugf("Portal API endpoint detected - allowing public access: %s", path);
            return true;
        }

        return false;
    }

    /**
     * Abort request with 401 Unauthorized
     */
    private void abortWithUnauthorized(ContainerRequestContext requestContext, String message) {
        requestContext.abortWith(
            Response.status(Response.Status.UNAUTHORIZED)
                .entity(new ErrorResponse(message))
                .build()
        );
    }

    /**
     * Error response DTO
     */
    public static class ErrorResponse {
        private String error;
        private long timestamp;

        public ErrorResponse(String error) {
            this.error = error;
            this.timestamp = System.currentTimeMillis();
        }

        public String getError() { return error; }
        public long getTimestamp() { return timestamp; }
    }
}
