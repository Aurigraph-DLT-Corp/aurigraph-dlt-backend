package io.aurigraph.v11.auth;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

import java.time.LocalDateTime;
import java.util.*;

/**
 * AdvancedTokenFeatureService
 *
 * Advanced token management features:
 * 1. Token Scopes - Granular permissions per token
 * 2. Token Blacklist - Immediately block compromised tokens
 * 3. Geo-Restrictions - Limit tokens by geographic location
 * 4. Device Fingerprinting - Verify device consistency
 * 5. Rate Limiting - Prevent token abuse
 */
@ApplicationScoped
public class AdvancedTokenFeatureService {

    private static final Logger LOG = Logger.getLogger(AdvancedTokenFeatureService.class);

    @Inject
    AuthTokenService authTokenService;

    // In-memory blacklist (can be persisted to Redis/database)
    private static final Set<String> tokenBlacklist = Collections.synchronizedSet(new HashSet<>());

    // In-memory rate limiter (can be persisted to Redis)
    private static final Map<String, TokenRateLimit> rateLimitMap = Collections.synchronizedMap(new HashMap<>());

    // Allowed geographic regions
    private static final Set<String> allowedRegions = new HashSet<>(Arrays.asList(
        "US", "EU", "ASIA", "ALL"
    ));

    /**
     * Define token scopes
     * Scopes control what API endpoints a token can access
     */
    public enum TokenScope {
        // Read scopes
        READ_PROFILE("read:profile", "Read user profile"),
        READ_TOKENS("read:tokens", "List and view tokens"),
        READ_TRANSACTIONS("read:transactions", "Read transactions"),
        READ_ANALYTICS("read:analytics", "View analytics"),

        // Write scopes
        WRITE_PROFILE("write:profile", "Modify profile"),
        WRITE_TRANSACTIONS("write:transactions", "Create transactions"),
        WRITE_CONTRACTS("write:contracts", "Deploy contracts"),

        // Admin scopes
        ADMIN_TOKENS("admin:tokens", "Manage all tokens"),
        ADMIN_USERS("admin:users", "Manage users"),
        ADMIN_SYSTEM("admin:system", "System administration");

        public final String scopeId;
        public final String description;

        TokenScope(String scopeId, String description) {
            this.scopeId = scopeId;
            this.description = description;
        }
    }

    /**
     * Add token to blacklist
     * Blacklisted tokens are immediately rejected even if not expired
     */
    @Transactional
    public void blacklistToken(String tokenHash, String reason) {
        tokenBlacklist.add(tokenHash);
        LOG.infof("⚠️ Token added to blacklist: %s (reason: %s)", tokenHash, reason);
    }

    /**
     * Check if token is blacklisted
     */
    public boolean isTokenBlacklisted(String tokenHash) {
        return tokenBlacklist.contains(tokenHash);
    }

    /**
     * Remove token from blacklist (rarely used)
     */
    public void removeTokenFromBlacklist(String tokenHash) {
        tokenBlacklist.remove(tokenHash);
        LOG.infof("✅ Token removed from blacklist: %s", tokenHash);
    }

    /**
     * Get blacklist size
     */
    public int getBlacklistSize() {
        return tokenBlacklist.size();
    }

    /**
     * Validate token scopes
     * Check if token has required scope to access endpoint
     */
    public boolean hasScope(AuthToken token, TokenScope requiredScope) {
        if (token == null || token.metadata == null) {
            return false;
        }

        try {
            // Parse metadata JSON to get scopes
            // In production, use Jackson ObjectMapper
            String metadata = token.metadata;
            return metadata.contains(requiredScope.scopeId);
        } catch (Exception e) {
            LOG.warnf("Error checking scope: %s", e.getMessage());
            return false;
        }
    }

    /**
     * Validate token scopes (multiple)
     */
    public boolean hasAnyScope(AuthToken token, TokenScope... requiredScopes) {
        return Arrays.stream(requiredScopes)
            .anyMatch(scope -> hasScope(token, scope));
    }

    /**
     * Validate all required scopes
     */
    public boolean hasAllScopes(AuthToken token, TokenScope... requiredScopes) {
        return Arrays.stream(requiredScopes)
            .allMatch(scope -> hasScope(token, scope));
    }

    /**
     * Validate geographic restriction
     */
    public boolean isLocationAllowed(AuthToken token, String clientCountryCode) {
        if (token == null || token.metadata == null) {
            return true; // No restriction
        }

        try {
            String metadata = token.metadata;
            if (!metadata.contains("geo_restriction")) {
                return true; // No restriction
            }

            // Check if country is in allowed list
            return metadata.contains(clientCountryCode) ||
                   metadata.contains("ALL");
        } catch (Exception e) {
            LOG.warnf("Error checking location restriction: %s", e.getMessage());
            return false;
        }
    }

    /**
     * Verify device fingerprint consistency
     * Prevent token use from different devices than where it was created
     */
    public boolean isDeviceConsistent(AuthToken token, String currentUserAgent, String currentClientIp) {
        if (token == null) {
            return false;
        }

        // Check if user agent is similar
        if (token.userAgent != null && !token.userAgent.equalsIgnoreCase(currentUserAgent)) {
            LOG.warnf("⚠️ User-Agent mismatch for token %s", token.tokenId);
            return false;
        }

        // For IP, allow slight variations (proxy changes)
        if (token.clientIp != null && !token.clientIp.equals(currentClientIp)) {
            LOG.warnf("⚠️ IP address change for token %s: %s → %s",
                token.tokenId, token.clientIp, currentClientIp);
            // Don't fail immediately, just log warning
            // In production, could require re-authentication
        }

        return true;
    }

    /**
     * Check rate limit for token
     * Prevent abuse through rate limiting
     */
    public boolean checkRateLimit(String tokenHash, int requestsPerMinute) {
        TokenRateLimit limit = rateLimitMap.computeIfAbsent(
            tokenHash,
            k -> new TokenRateLimit(requestsPerMinute)
        );

        return limit.checkLimit();
    }

    /**
     * Get rate limit status for token
     */
    public TokenRateLimitStatus getRateLimitStatus(String tokenHash) {
        TokenRateLimit limit = rateLimitMap.get(tokenHash);
        if (limit == null) {
            return new TokenRateLimitStatus(0, 0, false);
        }
        return limit.getStatus();
    }

    /**
     * Reset rate limit for token
     */
    public void resetRateLimit(String tokenHash) {
        rateLimitMap.remove(tokenHash);
        LOG.infof("✅ Rate limit reset for token: %s", tokenHash);
    }

    /**
     * Bulk add tokens to blacklist
     */
    @Transactional
    public int blacklistUserTokens(String userId, String reason) {
        List<AuthToken> tokens = AuthToken.find("userId", userId).list();
        tokens.forEach(token -> blacklistToken(token.tokenHash, reason));
        LOG.infof("✅ Blacklisted %d tokens for user %s: %s", tokens.size(), userId, reason);
        return tokens.size();
    }

    /**
     * Cleanup expired blacklist entries
     * (Optional - for persistent blacklist cleanup)
     */
    public void cleanupBlacklist() {
        int initialSize = tokenBlacklist.size();
        // In production with persistent blacklist, query and remove expired entries
        LOG.infof("🧹 Blacklist cleanup: %d entries", initialSize);
    }

    /**
     * Get blacklist statistics
     */
    public BlacklistStatistics getBlacklistStatistics() {
        return new BlacklistStatistics(
            tokenBlacklist.size(),
            rateLimitMap.size(),
            LocalDateTime.now()
        );
    }

    // ==================== Helper Classes ====================

    /**
     * Rate limit tracking for token
     */
    public static class TokenRateLimit {
        private final int maxRequests;
        private int requestCount;
        private LocalDateTime windowStart;

        public TokenRateLimit(int maxRequests) {
            this.maxRequests = maxRequests;
            this.requestCount = 0;
            this.windowStart = LocalDateTime.now();
        }

        public boolean checkLimit() {
            LocalDateTime now = LocalDateTime.now();
            long secondsElapsed = java.time.temporal.ChronoUnit.SECONDS.between(windowStart, now);

            // Reset window if 60 seconds have passed
            if (secondsElapsed >= 60) {
                this.requestCount = 0;
                this.windowStart = now;
            }

            // Check if limit exceeded
            if (requestCount >= maxRequests) {
                return false;
            }

            requestCount++;
            return true;
        }

        public TokenRateLimitStatus getStatus() {
            LocalDateTime now = LocalDateTime.now();
            long secondsRemaining = 60 - java.time.temporal.ChronoUnit.SECONDS.between(windowStart, now);
            return new TokenRateLimitStatus(requestCount, secondsRemaining, requestCount >= maxRequests);
        }
    }

    /**
     * Rate limit status response
     */
    public static class TokenRateLimitStatus {
        public int currentRequests;
        public long secondsRemaining;
        public boolean isLimited;

        public TokenRateLimitStatus(int currentRequests, long secondsRemaining, boolean isLimited) {
            this.currentRequests = currentRequests;
            this.secondsRemaining = secondsRemaining;
            this.isLimited = isLimited;
        }

        public int getCurrentRequests() { return currentRequests; }
        public long getSecondsRemaining() { return secondsRemaining; }
        public boolean isLimited() { return isLimited; }
    }

    /**
     * Blacklist statistics
     */
    public static class BlacklistStatistics {
        public int blacklistedTokens;
        public int activeRateLimits;
        public LocalDateTime timestamp;

        public BlacklistStatistics(int blacklistedTokens, int activeRateLimits, LocalDateTime timestamp) {
            this.blacklistedTokens = blacklistedTokens;
            this.activeRateLimits = activeRateLimits;
            this.timestamp = timestamp;
        }

        public int getBlacklistedTokens() { return blacklistedTokens; }
        public int getActiveRateLimits() { return activeRateLimits; }
        public LocalDateTime getTimestamp() { return timestamp; }
    }

    /**
     * Token scope requirement annotation
     */
    public static class ScopeRequirement {
        public TokenScope[] scopes;
        public boolean requireAll;

        public ScopeRequirement(TokenScope[] scopes, boolean requireAll) {
            this.scopes = scopes;
            this.requireAll = requireAll;
        }

        public TokenScope[] getScopes() { return scopes; }
        public boolean isRequireAll() { return requireAll; }
    }
}
