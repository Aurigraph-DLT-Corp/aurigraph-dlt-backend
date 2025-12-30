package io.aurigraph.v11.api.gateway;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import io.smallrye.mutiny.Uni;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

/**
 * Aurigraph V11 API Gateway
 * Provides rate limiting, authentication, and request routing
 * High-performance implementation for 2M+ TPS
 */
@Path("/gateway")
@ApplicationScoped
public class ApiGateway {

    private static final Logger LOG = Logger.getLogger(ApiGateway.class);

    @ConfigProperty(name = "aurigraph.gateway.rate-limit.default", defaultValue = "1000")
    int defaultRateLimit;

    @ConfigProperty(name = "aurigraph.gateway.rate-limit.window-seconds", defaultValue = "60")
    int rateLimitWindowSeconds;

    @ConfigProperty(name = "aurigraph.gateway.auth.enabled", defaultValue = "true")
    boolean authenticationEnabled;

    @Inject
    RateLimiter rateLimiter;

    @Inject
    AuthenticationService authService;

    @Inject
    GatewayMetrics metrics;

    // ==================== GATEWAY STATUS APIs ====================

    @GET
    @Path("/status")
    @Produces(MediaType.APPLICATION_JSON)
    public GatewayStatus getGatewayStatus() {
        return new GatewayStatus(
            "ACTIVE",
            "12.0.0",
            metrics.getTotalRequests(),
            metrics.getSuccessfulRequests(),
            metrics.getRateLimitedRequests(),
            metrics.getAuthenticationFailures(),
            defaultRateLimit,
            rateLimitWindowSeconds,
            authenticationEnabled,
            System.currentTimeMillis()
        );
    }

    @GET
    @Path("/metrics")
    @Produces(MediaType.APPLICATION_JSON)
    public GatewayMetricsResponse getMetrics() {
        return new GatewayMetricsResponse(
            metrics.getTotalRequests(),
            metrics.getSuccessfulRequests(),
            metrics.getRateLimitedRequests(),
            metrics.getAuthenticationFailures(),
            metrics.getAverageResponseTime(),
            metrics.getCurrentThroughput(),
            rateLimiter.getActiveClients(),
            System.currentTimeMillis()
        );
    }

    // ==================== RATE LIMITING CONFIGURATION ====================

    @POST
    @Path("/rate-limit/configure")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<Response> configureRateLimit(RateLimitConfiguration config) {
        return Uni.createFrom().item(() -> {
            try {
                rateLimiter.updateConfiguration(config.clientId(), config.requestsPerMinute(), config.burstLimit());
                
                LOG.infof("Rate limit configured for client %s: %d requests/min, burst: %d", 
                         config.clientId(), config.requestsPerMinute(), config.burstLimit());
                
                return Response.ok(Map.of(
                    "status", "CONFIGURED",
                    "clientId", config.clientId(),
                    "requestsPerMinute", config.requestsPerMinute(),
                    "burstLimit", config.burstLimit(),
                    "timestamp", System.currentTimeMillis()
                )).build();
            } catch (Exception e) {
                LOG.errorf(e, "Rate limit configuration failed for client %s", config.clientId());
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", e.getMessage())).build();
            }
        });
    }

    @GET
    @Path("/rate-limit/status/{clientId}")
    @Produces(MediaType.APPLICATION_JSON)
    public RateLimitStatus getRateLimitStatus(@PathParam("clientId") String clientId) {
        RateLimiter.ClientLimit limit = rateLimiter.getClientLimit(clientId);
        if (limit == null) {
            throw new NotFoundException("Client not found: " + clientId);
        }
        
        return new RateLimitStatus(
            clientId,
            limit.requestsPerMinute(),
            limit.burstLimit(),
            limit.currentCount(),
            limit.windowStart(),
            limit.isBlocked(),
            System.currentTimeMillis()
        );
    }

    // ==================== AUTHENTICATION MANAGEMENT ====================

    @POST
    @Path("/auth/token")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<Response> generateToken(TokenRequest request) {
        return Uni.createFrom().item(() -> {
            try {
                if (!authService.validateCredentials(request.clientId(), request.clientSecret())) {
                    metrics.incrementAuthenticationFailures();
                    return Response.status(Response.Status.UNAUTHORIZED)
                        .entity(Map.of("error", "Invalid credentials")).build();
                }
                
                String token = authService.generateToken(request.clientId(), request.scopes());
                long expiresAt = System.currentTimeMillis() + (24 * 60 * 60 * 1000); // 24 hours
                
                return Response.ok(Map.of(
                    "access_token", token,
                    "token_type", "Bearer",
                    "expires_in", 86400,
                    "expires_at", expiresAt,
                    "scope", request.scopes()
                )).build();
            } catch (Exception e) {
                LOG.errorf(e, "Token generation failed for client %s", request.clientId());
                metrics.incrementAuthenticationFailures();
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Token generation failed")).build();
            }
        });
    }

    @POST
    @Path("/auth/validate")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public TokenValidationResponse validateToken(TokenValidationRequest request) {
        try {
            AuthenticationService.TokenInfo tokenInfo = authService.validateToken(request.token());
            
            return new TokenValidationResponse(
                tokenInfo.isValid(),
                tokenInfo.clientId(),
                tokenInfo.scopes(),
                tokenInfo.expiresAt(),
                tokenInfo.issuedAt(),
                System.currentTimeMillis()
            );
        } catch (Exception e) {
            LOG.errorf(e, "Token validation failed");
            return new TokenValidationResponse(
                false, null, List.of(), 0L, 0L, System.currentTimeMillis()
            );
        }
    }

    // ==================== PROXY AND ROUTING ====================

    @Path("/proxy/{path: .*}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<Response> proxyGetRequest(
            @PathParam("path") String path,
            @HeaderParam("Authorization") String authorization,
            @HeaderParam("X-Client-ID") String clientId) {
        return handleProxyRequest(path, authorization, clientId, null);
    }

    @Path("/proxy/{path: .*}")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<Response> proxyPostRequest(
            @PathParam("path") String path,
            @HeaderParam("Authorization") String authorization,
            @HeaderParam("X-Client-ID") String clientId,
            String body) {
        return handleProxyRequest(path, authorization, clientId, body);
    }

    @Path("/proxy/{path: .*}")
    @PUT
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<Response> proxyPutRequest(
            @PathParam("path") String path,
            @HeaderParam("Authorization") String authorization,
            @HeaderParam("X-Client-ID") String clientId,
            String body) {
        return handleProxyRequest(path, authorization, clientId, body);
    }

    @Path("/proxy/{path: .*}")
    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<Response> proxyDeleteRequest(
            @PathParam("path") String path,
            @HeaderParam("Authorization") String authorization,
            @HeaderParam("X-Client-ID") String clientId) {
        return handleProxyRequest(path, authorization, clientId, null);
    }

    /**
     * Common proxy request handler for all HTTP methods
     */
    private Uni<Response> handleProxyRequest(
            String path,
            String authorization,
            String clientId,
            String body) {

        return Uni.createFrom().item(() -> {
            long startTime = System.nanoTime();
            metrics.incrementTotalRequests();

            try {
                // Extract client ID from authorization or header
                String resolvedClientId = resolveClientId(authorization, clientId);

                // Rate limiting check
                if (!rateLimiter.isAllowed(resolvedClientId)) {
                    metrics.incrementRateLimitedRequests();
                    return Response.status(429) // Too Many Requests
                        .entity(Map.of(
                            "error", "Rate limit exceeded",
                            "clientId", resolvedClientId,
                            "retryAfter", rateLimitWindowSeconds
                        )).build();
                }

                // Authentication check
                if (authenticationEnabled && !authService.isValidRequest(authorization, resolvedClientId)) {
                    metrics.incrementAuthenticationFailures();
                    return Response.status(Response.Status.UNAUTHORIZED)
                        .entity(Map.of("error", "Authentication failed")).build();
                }

                // Route to appropriate service
                Response response = routeRequest(path, body);

                // Update metrics
                long duration = System.nanoTime() - startTime;
                metrics.updateResponseTime(duration / 1_000_000.0); // Convert to milliseconds
                metrics.incrementSuccessfulRequests();

                return response;

            } catch (Exception e) {
                LOG.errorf(e, "Request proxying failed for path: %s", path);
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Internal server error")).build();
            }
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    // ==================== PRIVATE UTILITY METHODS ====================

    private String resolveClientId(String authorization, String clientIdHeader) {
        if (clientIdHeader != null && !clientIdHeader.isEmpty()) {
            return clientIdHeader;
        }
        
        if (authorization != null && authorization.startsWith("Bearer ")) {
            try {
                String token = authorization.substring(7);
                AuthenticationService.TokenInfo tokenInfo = authService.validateToken(token);
                return tokenInfo.clientId();
            } catch (Exception e) {
                LOG.debug("Failed to extract client ID from token", e);
            }
        }
        
        return "anonymous";
    }

    private Response routeRequest(String path, String body) {
        // Route to appropriate internal service based on path
        if (path.startsWith("v11/")) {
            return routeToV11Service(path.substring(3), body);
        } else if (path.startsWith("consensus/")) {
            return routeToConsensusService(path.substring(10), body);
        } else if (path.startsWith("bridge/")) {
            return routeToBridgeService(path.substring(7), body);
        } else {
            return Response.status(Response.Status.NOT_FOUND)
                .entity(Map.of("error", "Service not found for path: " + path)).build();
        }
    }

    private Response routeToV11Service(String subPath, String body) {
        // Simulate routing to V11 service
        return Response.ok(Map.of(
            "service", "v11",
            "path", subPath,
            "status", "routed",
            "timestamp", System.currentTimeMillis()
        )).build();
    }

    private Response routeToConsensusService(String subPath, String body) {
        // Simulate routing to consensus service
        return Response.ok(Map.of(
            "service", "consensus",
            "path", subPath,
            "status", "routed",
            "timestamp", System.currentTimeMillis()
        )).build();
    }

    private Response routeToBridgeService(String subPath, String body) {
        // Simulate routing to bridge service
        return Response.ok(Map.of(
            "service", "bridge",
            "path", subPath,
            "status", "routed",
            "timestamp", System.currentTimeMillis()
        )).build();
    }

    // ==================== DATA MODELS ====================

    public record GatewayStatus(
        String status,
        String version,
        long totalRequests,
        long successfulRequests,
        long rateLimitedRequests,
        long authenticationFailures,
        int defaultRateLimit,
        int rateLimitWindowSeconds,
        boolean authenticationEnabled,
        long timestamp
    ) {}

    public record GatewayMetricsResponse(
        long totalRequests,
        long successfulRequests,
        long rateLimitedRequests,
        long authenticationFailures,
        double averageResponseTimeMs,
        double currentThroughput,
        int activeClients,
        long timestamp
    ) {}

    public record RateLimitConfiguration(
        String clientId,
        int requestsPerMinute,
        int burstLimit
    ) {}

    public record RateLimitStatus(
        String clientId,
        int requestsPerMinute,
        int burstLimit,
        int currentCount,
        long windowStart,
        boolean blocked,
        long timestamp
    ) {}

    public record TokenRequest(
        String clientId,
        String clientSecret,
        List<String> scopes
    ) {}

    public record TokenValidationRequest(String token) {}

    public record TokenValidationResponse(
        boolean valid,
        String clientId,
        List<String> scopes,
        long expiresAt,
        long issuedAt,
        long timestamp
    ) {}
}

/**
 * Rate Limiter Service
 */
@ApplicationScoped
class RateLimiter {
    
    private static final Logger LOG = Logger.getLogger(RateLimiter.class);
    
    private final ConcurrentHashMap<String, ClientLimit> clientLimits = new ConcurrentHashMap<>();
    
    @ConfigProperty(name = "aurigraph.gateway.rate-limit.default", defaultValue = "1000")
    int defaultRateLimit;
    
    @ConfigProperty(name = "aurigraph.gateway.rate-limit.window-seconds", defaultValue = "60")
    int windowSeconds;

    public boolean isAllowed(String clientId) {
        ClientLimit limit = clientLimits.computeIfAbsent(clientId, this::createDefaultLimit);
        return limit.checkAndIncrement();
    }

    public void updateConfiguration(String clientId, int requestsPerMinute, int burstLimit) {
        ClientLimit newLimit = new ClientLimit(clientId, requestsPerMinute, burstLimit, windowSeconds * 1000L);
        clientLimits.put(clientId, newLimit);
        LOG.infof("Updated rate limit for client %s: %d req/min, burst: %d", clientId, requestsPerMinute, burstLimit);
    }

    public ClientLimit getClientLimit(String clientId) {
        return clientLimits.get(clientId);
    }

    public int getActiveClients() {
        return clientLimits.size();
    }

    private ClientLimit createDefaultLimit(String clientId) {
        return new ClientLimit(clientId, defaultRateLimit, defaultRateLimit / 4, windowSeconds * 1000L);
    }

    public static class ClientLimit {
        private final String clientId;
        private final int requestsPerMinute;
        private final int burstLimit;
        private final long windowMs;
        private volatile long windowStart;
        private final AtomicInteger currentCount;
        private volatile boolean blocked;

        public ClientLimit(String clientId, int requestsPerMinute, int burstLimit, long windowMs) {
            this.clientId = clientId;
            this.requestsPerMinute = requestsPerMinute;
            this.burstLimit = burstLimit;
            this.windowMs = windowMs;
            this.windowStart = System.currentTimeMillis();
            this.currentCount = new AtomicInteger(0);
            this.blocked = false;
        }

        public synchronized boolean checkAndIncrement() {
            long now = System.currentTimeMillis();
            
            // Reset window if expired
            if (now - windowStart >= windowMs) {
                windowStart = now;
                currentCount.set(0);
                blocked = false;
            }
            
            int current = currentCount.get();
            
            // Check burst limit
            if (current >= burstLimit) {
                blocked = true;
                return false;
            }
            
            // Check rate limit
            if (current >= requestsPerMinute) {
                blocked = true;
                return false;
            }
            
            currentCount.incrementAndGet();
            return true;
        }

        // Getters
        public String clientId() { return clientId; }
        public int requestsPerMinute() { return requestsPerMinute; }
        public int burstLimit() { return burstLimit; }
        public int currentCount() { return currentCount.get(); }
        public long windowStart() { return windowStart; }
        public boolean isBlocked() { return blocked; }
    }
}

/**
 * Authentication Service
 */
@ApplicationScoped
class AuthenticationService {
    
    private static final Logger LOG = Logger.getLogger(AuthenticationService.class);
    
    private final ConcurrentHashMap<String, ClientCredentials> clientCredentials = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, TokenInfo> activeTokens = new ConcurrentHashMap<>();

    public AuthenticationService() {
        // Initialize with default credentials (in production, load from secure storage)
        clientCredentials.put("aurigraph-client", new ClientCredentials(
            "aurigraph-client", 
            "aurigraph-secret-2025", 
            List.of("read", "write", "admin")
        ));
        clientCredentials.put("test-client", new ClientCredentials(
            "test-client", 
            "test-secret", 
            List.of("read")
        ));
    }

    public boolean validateCredentials(String clientId, String clientSecret) {
        ClientCredentials creds = clientCredentials.get(clientId);
        return creds != null && creds.clientSecret().equals(clientSecret);
    }

    public String generateToken(String clientId, List<String> requestedScopes) {
        ClientCredentials creds = clientCredentials.get(clientId);
        if (creds == null) {
            throw new IllegalArgumentException("Unknown client: " + clientId);
        }
        
        // Validate requested scopes
        List<String> grantedScopes = requestedScopes.stream()
            .filter(scope -> creds.allowedScopes().contains(scope))
            .toList();
        
        // Generate simple token (in production, use JWT or similar)
        String token = "aurigraph_token_" + clientId + "_" + System.currentTimeMillis();
        long expiresAt = System.currentTimeMillis() + (24 * 60 * 60 * 1000); // 24 hours
        
        TokenInfo tokenInfo = new TokenInfo(
            token, 
            clientId, 
            grantedScopes, 
            true, 
            System.currentTimeMillis(), 
            expiresAt
        );
        
        activeTokens.put(token, tokenInfo);
        LOG.infof("Generated token for client %s with scopes: %s", clientId, grantedScopes);
        
        return token;
    }

    public TokenInfo validateToken(String token) {
        TokenInfo tokenInfo = activeTokens.get(token);
        if (tokenInfo == null) {
            return new TokenInfo(token, null, List.of(), false, 0L, 0L);
        }
        
        // Check expiration
        if (System.currentTimeMillis() > tokenInfo.expiresAt()) {
            activeTokens.remove(token);
            return new TokenInfo(token, tokenInfo.clientId(), tokenInfo.scopes(), false, tokenInfo.issuedAt(), tokenInfo.expiresAt());
        }
        
        return tokenInfo;
    }

    public boolean isValidRequest(String authorization, String clientId) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return false;
        }
        
        String token = authorization.substring(7);
        TokenInfo tokenInfo = validateToken(token);
        
        return tokenInfo.isValid() && tokenInfo.clientId().equals(clientId);
    }

    public record ClientCredentials(String clientId, String clientSecret, List<String> allowedScopes) {}
    
    public record TokenInfo(
        String token, 
        String clientId, 
        List<String> scopes, 
        boolean isValid, 
        long issuedAt, 
        long expiresAt
    ) {}
}

/**
 * Gateway Metrics Service
 */
@ApplicationScoped
class GatewayMetrics {
    
    private final AtomicLong totalRequests = new AtomicLong(0);
    private final AtomicLong successfulRequests = new AtomicLong(0);
    private final AtomicLong rateLimitedRequests = new AtomicLong(0);
    private final AtomicLong authenticationFailures = new AtomicLong(0);
    private volatile double averageResponseTime = 0.0;
    private final AtomicLong totalResponseTime = new AtomicLong(0);
    private final AtomicLong responseCount = new AtomicLong(0);
    private volatile long lastThroughputUpdate = System.currentTimeMillis();
    private volatile double currentThroughput = 0.0;

    public void incrementTotalRequests() {
        totalRequests.incrementAndGet();
        updateThroughput();
    }

    public void incrementSuccessfulRequests() {
        successfulRequests.incrementAndGet();
    }

    public void incrementRateLimitedRequests() {
        rateLimitedRequests.incrementAndGet();
    }

    public void incrementAuthenticationFailures() {
        authenticationFailures.incrementAndGet();
    }

    public void updateResponseTime(double responseTimeMs) {
        totalResponseTime.addAndGet((long) responseTimeMs);
        long count = responseCount.incrementAndGet();
        averageResponseTime = totalResponseTime.get() / (double) count;
    }

    private void updateThroughput() {
        long now = System.currentTimeMillis();
        long timeDiff = now - lastThroughputUpdate;
        if (timeDiff >= 1000) { // Update every second
            currentThroughput = totalRequests.get() / ((now - lastThroughputUpdate) / 1000.0);
            lastThroughputUpdate = now;
        }
    }

    // Getters
    public long getTotalRequests() { return totalRequests.get(); }
    public long getSuccessfulRequests() { return successfulRequests.get(); }
    public long getRateLimitedRequests() { return rateLimitedRequests.get(); }
    public long getAuthenticationFailures() { return authenticationFailures.get(); }
    public double getAverageResponseTime() { return averageResponseTime; }
    public double getCurrentThroughput() { return currentThroughput; }
}

/**
 * Request Filter for API Gateway
 */
@Provider
@ApplicationScoped
class GatewayRequestFilter implements ContainerRequestFilter {
    
    private static final Logger LOG = Logger.getLogger(GatewayRequestFilter.class);
    
    @Inject
    GatewayMetrics metrics;

    @Override
    public void filter(ContainerRequestContext requestContext) {
        // Add request ID for tracing
        String requestId = "req_" + System.currentTimeMillis() + "_" + Math.random();
        requestContext.setProperty("requestId", requestId);
        
        // Log request
        LOG.debugf("Gateway request: %s %s [%s]", 
                  requestContext.getMethod(), 
                  requestContext.getUriInfo().getPath(),
                  requestId);
    }
}