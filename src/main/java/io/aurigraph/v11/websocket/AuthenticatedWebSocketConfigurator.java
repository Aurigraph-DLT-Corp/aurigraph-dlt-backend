package io.aurigraph.v11.websocket;

import io.aurigraph.v11.user.JwtService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.websocket.HandshakeResponse;
import jakarta.websocket.server.HandshakeRequest;
import jakarta.websocket.server.ServerEndpointConfig;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.Map;

/**
 * WebSocket Configurator with JWT Authentication
 *
 * Extracts and validates JWT tokens from:
 * 1. Query parameter: ?token=<jwt>
 * 2. Authorization header: Authorization: Bearer <jwt>
 *
 * On successful authentication:
 * - Stores userId in endpoint UserProperties
 * - Stores token in endpoint UserProperties
 *
 * On authentication failure:
 * - Connection is established but userId is null
 * - Individual endpoints can reject unauthenticated connections
 *
 * @author WebSocket Development Agent (WDA)
 * @since V11.6.0 (Sprint 16 - AV11-484)
 */
@ApplicationScoped
public class AuthenticatedWebSocketConfigurator extends ServerEndpointConfig.Configurator {

    private static final Logger LOG = Logger.getLogger(AuthenticatedWebSocketConfigurator.class);

    // Note: CDI injection doesn't work in Configurator
    // We'll need to manually lookup JwtService using CDI.current()
    private JwtService jwtService;

    @Override
    public void modifyHandshake(ServerEndpointConfig config,
                                HandshakeRequest request,
                                HandshakeResponse response) {
        try {
            // Get JwtService instance via CDI programmatic lookup
            if (jwtService == null) {
                jwtService = jakarta.enterprise.inject.spi.CDI.current().select(JwtService.class).get();
            }

            String token = extractToken(request);

            if (token == null || token.isEmpty()) {
                LOG.warnf("⚠️ WebSocket handshake: No JWT token provided");
                config.getUserProperties().put("userId", null);
                config.getUserProperties().put("authenticated", false);
                return;
            }

            // Validate JWT token
            if (!jwtService.validateToken(token)) {
                LOG.warnf("⚠️ WebSocket handshake: Invalid or expired JWT token");
                config.getUserProperties().put("userId", null);
                config.getUserProperties().put("authenticated", false);
                config.getUserProperties().put("errorMessage", "Invalid or expired token");
                return;
            }

            // Extract user ID from token
            String userId = jwtService.getUserIdFromToken(token);
            if (userId == null) {
                LOG.warnf("⚠️ WebSocket handshake: Could not extract user ID from token");
                config.getUserProperties().put("userId", null);
                config.getUserProperties().put("authenticated", false);
                config.getUserProperties().put("errorMessage", "Could not extract user ID from token");
                return;
            }

            // Store authentication info in config
            config.getUserProperties().put("userId", userId);
            config.getUserProperties().put("token", token);
            config.getUserProperties().put("authenticated", true);

            LOG.infof("✅ WebSocket handshake: Authenticated user %s", userId);

        } catch (Exception e) {
            LOG.errorf(e, "❌ Error during WebSocket authentication handshake");
            config.getUserProperties().put("userId", null);
            config.getUserProperties().put("authenticated", false);
            config.getUserProperties().put("errorMessage", "Authentication error: " + e.getMessage());
        }
    }

    /**
     * Extract JWT token from request
     * Priority: 1. Query parameter, 2. Authorization header
     */
    private String extractToken(HandshakeRequest request) {
        // 1. Try query parameter: /ws/transactions?token=<jwt>
        Map<String, List<String>> params = request.getParameterMap();
        if (params.containsKey("token")) {
            List<String> tokens = params.get("token");
            if (tokens != null && !tokens.isEmpty()) {
                String token = tokens.get(0);
                LOG.debugf("Token extracted from query parameter");
                return token;
            }
        }

        // 2. Try Authorization header: Authorization: Bearer <jwt>
        Map<String, List<String>> headers = request.getHeaders();
        if (headers.containsKey("authorization") || headers.containsKey("Authorization")) {
            List<String> authHeaders = headers.get("authorization");
            if (authHeaders == null) {
                authHeaders = headers.get("Authorization");
            }

            if (authHeaders != null && !authHeaders.isEmpty()) {
                String authHeader = authHeaders.get(0);
                if (authHeader.startsWith("Bearer ")) {
                    String token = authHeader.substring("Bearer ".length());
                    LOG.debugf("Token extracted from Authorization header");
                    return token;
                }
            }
        }

        LOG.debugf("No token found in query parameter or Authorization header");
        return null;
    }
}
