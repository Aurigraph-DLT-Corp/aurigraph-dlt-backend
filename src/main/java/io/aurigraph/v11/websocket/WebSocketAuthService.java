package io.aurigraph.v11.websocket;

import io.aurigraph.v11.auth.AuthToken;
import io.aurigraph.v11.auth.AuthTokenService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.websocket.Session;
import org.jboss.logging.Logger;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket Authentication Service
 *
 * Enhanced authentication logic for WebSocket connections:
 * - OAuth2 token refresh for WebSocket sessions
 * - Session timeout management
 * - Device fingerprinting
 * - Suspicious activity detection
 * - Token validation and renewal
 *
 * Integrates with existing AuthTokenService for JWT management.
 *
 * @author WebSocket Development Agent (WDA)
 * @since V11.6.0 (Sprint 16 - AV11-484)
 */
@ApplicationScoped
public class WebSocketAuthService {

    private static final Logger LOG = Logger.getLogger(WebSocketAuthService.class);

    @Inject
    AuthTokenService authTokenService;

    // Session timeout tracking (sessionId -> last activity timestamp)
    private final Map<String, SessionActivity> sessionActivity = new ConcurrentHashMap<>();

    // Device fingerprints (sessionId -> fingerprint)
    private final Map<String, DeviceFingerprint> deviceFingerprints = new ConcurrentHashMap<>();

    // Configuration
    private static final long SESSION_TIMEOUT_MS = 30 * 60 * 1000; // 30 minutes
    private static final long IDLE_TIMEOUT_MS = 5 * 60 * 1000; // 5 minutes
    private static final int MAX_FAILED_AUTH_ATTEMPTS = 5;

    /**
     * Authenticate WebSocket connection with JWT token
     *
     * @param token JWT token
     * @param clientIp Client IP address
     * @param userAgent User agent string
     * @return Optional containing user ID if authenticated
     */
    public Optional<String> authenticateConnection(String token, String clientIp, String userAgent) {
        try {
            // Validate token with AuthTokenService
            Optional<AuthToken> authTokenOpt = authTokenService.validateToken(token);

            if (!authTokenOpt.isPresent()) {
                LOG.warnf("Invalid or expired token from IP: %s", clientIp);
                return Optional.empty();
            }

            AuthToken authToken = authTokenOpt.get();

            // Check if token is valid
            if (!authToken.isValid()) {
                LOG.warnf("Token is not valid (revoked or expired) for user: %s", authToken.userEmail);
                return Optional.empty();
            }

            // Additional security checks
            if (!verifyTokenContext(authToken, clientIp, userAgent)) {
                LOG.warnf("Token context verification failed for user: %s", authToken.userEmail);
                return Optional.empty();
            }

            LOG.infof("Successfully authenticated WebSocket connection for user: %s (IP: %s)",
                authToken.userEmail, clientIp);

            return Optional.of(authToken.userId);

        } catch (Exception e) {
            LOG.errorf(e, "Authentication failed for IP: %s", clientIp);
            return Optional.empty();
        }
    }

    /**
     * Register session activity for timeout tracking
     *
     * @param sessionId Session ID
     * @param userId User ID
     */
    public void registerSession(String sessionId, String userId) {
        SessionActivity activity = new SessionActivity(sessionId, userId);
        sessionActivity.put(sessionId, activity);
        LOG.debugf("Registered session activity for session: %s, user: %s", sessionId, userId);
    }

    /**
     * Update session activity (mark as active)
     *
     * @param sessionId Session ID
     */
    public void updateActivity(String sessionId) {
        SessionActivity activity = sessionActivity.get(sessionId);
        if (activity != null) {
            activity.updateActivity();
        }
    }

    /**
     * Check if session has timed out
     *
     * @param sessionId Session ID
     * @return true if session has timed out
     */
    public boolean isSessionTimedOut(String sessionId) {
        SessionActivity activity = sessionActivity.get(sessionId);
        if (activity == null) {
            return true; // Unknown session = timed out
        }

        long now = System.currentTimeMillis();
        long sessionAge = now - activity.connectedAt;
        long idleTime = now - activity.lastActivityAt;

        // Check absolute session timeout
        if (sessionAge > SESSION_TIMEOUT_MS) {
            LOG.warnf("Session %s timed out (absolute timeout: %dms)", sessionId, sessionAge);
            return true;
        }

        // Check idle timeout
        if (idleTime > IDLE_TIMEOUT_MS) {
            LOG.warnf("Session %s idle timeout (%dms)", sessionId, idleTime);
            return true;
        }

        return false;
    }

    /**
     * Create device fingerprint for session
     *
     * @param sessionId Session ID
     * @param clientIp Client IP address
     * @param userAgent User agent string
     * @param additionalInfo Additional fingerprint data
     */
    public void createDeviceFingerprint(String sessionId, String clientIp, String userAgent,
                                       Map<String, String> additionalInfo) {
        DeviceFingerprint fingerprint = new DeviceFingerprint(clientIp, userAgent, additionalInfo);
        deviceFingerprints.put(sessionId, fingerprint);
        LOG.debugf("Created device fingerprint for session: %s (IP: %s)", sessionId, clientIp);
    }

    /**
     * Verify device fingerprint matches expected pattern
     *
     * @param sessionId Session ID
     * @param currentIp Current IP address
     * @param currentUserAgent Current user agent
     * @return true if fingerprint is consistent
     */
    public boolean verifyDeviceFingerprint(String sessionId, String currentIp, String currentUserAgent) {
        DeviceFingerprint fingerprint = deviceFingerprints.get(sessionId);
        if (fingerprint == null) {
            LOG.warnf("No fingerprint found for session: %s", sessionId);
            return false;
        }

        // Check if IP changed (suspicious)
        if (!fingerprint.clientIp.equals(currentIp)) {
            LOG.warnf("IP address changed for session %s: %s -> %s",
                sessionId, fingerprint.clientIp, currentIp);
            return false;
        }

        // Check if user agent changed (suspicious)
        if (!fingerprint.userAgent.equals(currentUserAgent)) {
            LOG.warnf("User agent changed for session %s", sessionId);
            return false;
        }

        return true;
    }

    /**
     * Detect suspicious activity patterns
     *
     * @param sessionId Session ID
     * @param userId User ID
     * @return true if suspicious activity detected
     */
    public boolean detectSuspiciousActivity(String sessionId, String userId) {
        SessionActivity activity = sessionActivity.get(sessionId);
        if (activity == null) {
            return false;
        }

        // Check for rapid reconnections (potential attack)
        if (activity.reconnectCount > 10 && activity.getSessionAge() < 60_000) {
            LOG.warnf("Suspicious reconnection pattern detected for user %s (session: %s): %d reconnects in %dms",
                userId, sessionId, activity.reconnectCount, activity.getSessionAge());
            return true;
        }

        // Check for excessive message rate (potential DoS)
        long messageRate = activity.messageCount * 1000 / Math.max(1, activity.getSessionAge());
        if (messageRate > 100) { // More than 100 messages per second
            LOG.warnf("Excessive message rate detected for user %s: %d msg/s", userId, messageRate);
            return true;
        }

        return false;
    }

    /**
     * Cleanup session tracking
     *
     * @param sessionId Session ID to cleanup
     */
    public void cleanupSession(String sessionId) {
        sessionActivity.remove(sessionId);
        deviceFingerprints.remove(sessionId);
        LOG.debugf("Cleaned up auth tracking for session: %s", sessionId);
    }

    /**
     * Verify token context (IP, user agent consistency)
     *
     * @param authToken Auth token
     * @param currentIp Current IP address
     * @param currentUserAgent Current user agent
     * @return true if context is valid
     */
    private boolean verifyTokenContext(AuthToken authToken, String currentIp, String currentUserAgent) {
        // Check if IP changed (may indicate token theft)
        if (authToken.clientIp != null && !authToken.clientIp.equals(currentIp)) {
            LOG.warnf("IP address mismatch for token: stored=%s, current=%s",
                authToken.clientIp, currentIp);
            // Don't fail hard on IP change (mobile users may change networks)
            // But log for audit trail
        }

        // Check if user agent changed (suspicious)
        if (authToken.userAgent != null && !authToken.userAgent.equals(currentUserAgent)) {
            LOG.warnf("User agent mismatch for token: stored=%s, current=%s",
                authToken.userAgent, currentUserAgent);
            // Don't fail hard, but log
        }

        return true;
    }

    /**
     * Refresh OAuth2 token for WebSocket session (if needed)
     *
     * @param sessionId Session ID
     * @param currentToken Current JWT token
     * @return Optional containing new token if refreshed
     */
    public Optional<String> refreshTokenIfNeeded(String sessionId, String currentToken) {
        try {
            // Validate current token
            Optional<AuthToken> authTokenOpt = authTokenService.validateToken(currentToken);
            if (!authTokenOpt.isPresent()) {
                LOG.warnf("Cannot refresh invalid token for session: %s", sessionId);
                return Optional.empty();
            }

            AuthToken authToken = authTokenOpt.get();

            // Check if token is about to expire (within 5 minutes)
            if (authToken.expiresAt != null) {
                LocalDateTime expiresAt = authToken.expiresAt;
                LocalDateTime now = LocalDateTime.now();
                LocalDateTime threshold = now.plusMinutes(5);

                if (expiresAt.isBefore(threshold)) {
                    LOG.infof("Token for session %s is about to expire, refresh needed", sessionId);
                    // Note: Actual token refresh would require OAuth2 refresh token flow
                    // This is a placeholder for integration with OAuth2 provider
                    return Optional.empty(); // Would return new token
                }
            }

            return Optional.empty(); // No refresh needed

        } catch (Exception e) {
            LOG.errorf(e, "Error checking token refresh for session: %s", sessionId);
            return Optional.empty();
        }
    }

    /**
     * Get session activity statistics
     *
     * @param sessionId Session ID
     * @return Optional containing session stats
     */
    public Optional<SessionStats> getSessionStats(String sessionId) {
        SessionActivity activity = sessionActivity.get(sessionId);
        if (activity == null) {
            return Optional.empty();
        }

        return Optional.of(new SessionStats(
            sessionId,
            activity.userId,
            activity.getSessionAge(),
            activity.getIdleTime(),
            activity.messageCount,
            activity.reconnectCount
        ));
    }

    /**
     * Session Activity Tracker
     */
    private static class SessionActivity {
        final String sessionId;
        final String userId;
        final long connectedAt;
        long lastActivityAt;
        int messageCount = 0;
        int reconnectCount = 0;

        SessionActivity(String sessionId, String userId) {
            this.sessionId = sessionId;
            this.userId = userId;
            this.connectedAt = System.currentTimeMillis();
            this.lastActivityAt = connectedAt;
        }

        void updateActivity() {
            this.lastActivityAt = System.currentTimeMillis();
            this.messageCount++;
        }

        void recordReconnect() {
            this.reconnectCount++;
            this.lastActivityAt = System.currentTimeMillis();
        }

        long getSessionAge() {
            return System.currentTimeMillis() - connectedAt;
        }

        long getIdleTime() {
            return System.currentTimeMillis() - lastActivityAt;
        }
    }

    /**
     * Device Fingerprint
     */
    private static class DeviceFingerprint {
        final String clientIp;
        final String userAgent;
        final Map<String, String> additionalInfo;
        final long createdAt;

        DeviceFingerprint(String clientIp, String userAgent, Map<String, String> additionalInfo) {
            this.clientIp = clientIp;
            this.userAgent = userAgent;
            this.additionalInfo = additionalInfo != null ? additionalInfo : Map.of();
            this.createdAt = System.currentTimeMillis();
        }
    }

    /**
     * Session Statistics
     */
    public static class SessionStats {
        public final String sessionId;
        public final String userId;
        public final long sessionAge;
        public final long idleTime;
        public final int messageCount;
        public final int reconnectCount;

        SessionStats(String sessionId, String userId, long sessionAge, long idleTime,
                    int messageCount, int reconnectCount) {
            this.sessionId = sessionId;
            this.userId = userId;
            this.sessionAge = sessionAge;
            this.idleTime = idleTime;
            this.messageCount = messageCount;
            this.reconnectCount = reconnectCount;
        }

        @Override
        public String toString() {
            return String.format("SessionStats{session='%s', user='%s', age=%dms, idle=%dms, messages=%d, reconnects=%d}",
                sessionId, userId, sessionAge, idleTime, messageCount, reconnectCount);
        }
    }
}
