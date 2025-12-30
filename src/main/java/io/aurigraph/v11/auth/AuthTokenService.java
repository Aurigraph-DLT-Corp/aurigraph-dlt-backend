package io.aurigraph.v11.auth;

import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * AuthTokenService
 *
 * Manages JWT token lifecycle:
 * - Token creation and storage
 * - Token validation and verification
 * - Token revocation
 * - Token cleanup and expiration
 * - Multi-device session management
 * - Audit logging
 */
@ApplicationScoped
public class AuthTokenService {

    @Inject
    AuthTokenRepository tokenRepository;

    private static final int TOKEN_HASH_LENGTH = 64;
    private static final String HASH_ALGORITHM = "SHA-256";

    /**
     * Store a new JWT token in the database
     *
     * @param userId User ID
     * @param userEmail User email
     * @param token JWT token
     * @param tokenType Token type (ACCESS or REFRESH)
     * @param expiresAt Token expiration time
     * @param clientIp Client IP address
     * @param userAgent Client user agent
     * @return AuthToken entity
     */
    @Transactional
    public AuthToken storeToken(
            String userId,
            String userEmail,
            String token,
            AuthToken.TokenType tokenType,
            LocalDateTime expiresAt,
            String clientIp,
            String userAgent) {
        try {
            // Hash the token for secure storage
            String tokenHash = hashToken(token);

            // Create new token entity
            AuthToken authToken = new AuthToken(
                    userId,
                    userEmail,
                    tokenHash,
                    tokenType,
                    expiresAt
            );
            authToken.clientIp = clientIp;
            authToken.userAgent = userAgent;

            // Persist to database
            tokenRepository.persist(authToken);

            Log.infof("✅ Token stored for user %s (type: %s, expires: %s)",
                    userEmail, tokenType, expiresAt);

            return authToken;
        } catch (Exception e) {
            Log.errorf("❌ Failed to store token for user %s: %s", userEmail, e.getMessage());
            throw new RuntimeException("Token storage failed", e);
        }
    }

    /**
     * Validate token against stored hash in database
     *
     * @param token JWT token
     * @return Optional containing AuthToken if valid
     */
    public Optional<AuthToken> validateToken(String token) {
        try {
            String tokenHash = hashToken(token);
            Optional<AuthToken> tokenOptional = tokenRepository.findByTokenHash(tokenHash);

            if (tokenOptional.isPresent()) {
                AuthToken authToken = tokenOptional.get();

                // Check token validity
                if (!authToken.isValid()) {
                    Log.warnf("⚠️ Token for user %s is not valid (revoked/expired)", authToken.userEmail);
                    return Optional.empty();
                }

                // Mark token as used
                authToken.markUsed();
                tokenRepository.persist(authToken);

                Log.debugf("✅ Token validated for user %s", authToken.userEmail);
                return Optional.of(authToken);
            }

            Log.warnf("⚠️ Token not found in database");
            return Optional.empty();
        } catch (Exception e) {
            Log.errorf("❌ Token validation failed: %s", e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Get all active tokens for a user
     *
     * @param userId User ID
     * @return List of active tokens
     */
    public List<AuthToken> getActiveTokens(String userId) {
        return tokenRepository.findActiveTokensByUserId(userId);
    }

    /**
     * Get all active access tokens for a user
     *
     * @param userId User ID
     * @return List of active access tokens
     */
    public List<AuthToken> getActiveAccessTokens(String userId) {
        return tokenRepository.findActiveAccessTokensByUserId(userId);
    }

    /**
     * Revoke a specific token
     *
     * @param tokenHash Hash of token to revoke
     * @param reason Revocation reason
     */
    @Transactional
    public void revokeToken(String tokenHash, String reason) {
        Optional<AuthToken> tokenOptional = tokenRepository.findByTokenHash(tokenHash);
        if (tokenOptional.isPresent()) {
            AuthToken token = tokenOptional.get();
            token.revoke(reason);
            tokenRepository.persist(token);
            Log.infof("✅ Token revoked for user %s: %s", token.userEmail, reason);
        }
    }

    /**
     * Revoke all tokens for a user (logout all devices)
     *
     * @param userId User ID
     * @param reason Revocation reason
     */
    @Transactional
    public int revokeAllTokensForUser(String userId, String reason) {
        int count = tokenRepository.revokeAllTokensForUser(userId, reason);
        Log.infof("✅ Revoked %d tokens for user %s: %s", count, userId, reason);
        return count;
    }

    /**
     * Revoke all access tokens (keep refresh tokens for renewal)
     *
     * @param userId User ID
     * @param reason Revocation reason
     */
    @Transactional
    public int revokeAccessTokens(String userId, String reason) {
        int count = tokenRepository.revokeAllAccessTokensForUser(userId, reason);
        Log.infof("✅ Revoked %d access tokens for user %s: %s", count, userId, reason);
        return count;
    }

    /**
     * Mark a token as refreshed (used to create a new token)
     *
     * @param oldTokenHash Hash of old token
     * @param newTokenId ID of new token
     */
    @Transactional
    public void markTokenAsRefreshed(String oldTokenHash, String newTokenId) {
        Optional<AuthToken> tokenOptional = tokenRepository.findByTokenHash(oldTokenHash);
        if (tokenOptional.isPresent()) {
            AuthToken token = tokenOptional.get();
            token.markAsRefreshed(newTokenId);
            tokenRepository.persist(token);
            Log.debugf("✅ Token marked as refreshed: %s → %s", oldTokenHash, newTokenId);
        }
    }

    /**
     * Count active tokens for a user
     *
     * @param userId User ID
     * @return Count of active tokens
     */
    public long countActiveTokens(String userId) {
        return tokenRepository.countActiveTokensForUser(userId);
    }

    /**
     * Limit active tokens per user (for security - prevent unlimited device sessions)
     *
     * @param userId User ID
     * @param maxTokens Maximum allowed active tokens
     */
    @Transactional
    public void enforceTokenLimit(String userId, int maxTokens) {
        List<AuthToken> tokens = tokenRepository.findActiveTokensByUserId(userId);

        if (tokens.size() > maxTokens) {
            // Sort by last used time and revoke oldest
            tokens.stream()
                    .sorted((a, b) -> {
                        LocalDateTime timeA = a.lastUsedAt != null ? a.lastUsedAt : a.createdAt;
                        LocalDateTime timeB = b.lastUsedAt != null ? b.lastUsedAt : b.createdAt;
                        return timeA.compareTo(timeB);
                    })
                    .limit(tokens.size() - maxTokens)
                    .forEach(token -> {
                        token.revoke("Token limit exceeded");
                        tokenRepository.persist(token);
                        Log.infof("⚠️ Token revoked due to limit enforcement for user %s", userId);
                    });
        }
    }

    /**
     * Cleanup expired tokens (background task)
     *
     * @return Number of deleted tokens
     */
    @Transactional
    public int cleanupExpiredTokens() {
        try {
            int count = tokenRepository.deleteExpiredTokens();
            Log.infof("🧹 Cleaned up %d expired tokens", count);
            return count;
        } catch (Exception e) {
            Log.warnf("⚠️ Failed to cleanup expired tokens: %s", e.getMessage());
            return 0;
        }
    }

    /**
     * Cleanup old expired tokens (older than N days)
     *
     * @param daysOld Number of days
     * @return Number of deleted tokens
     */
    @Transactional
    public int cleanupOldExpiredTokens(int daysOld) {
        try {
            int count = tokenRepository.deleteOldExpiredTokens(daysOld);
            Log.infof("🧹 Cleaned up %d old expired tokens (>%d days)", count, daysOld);
            return count;
        } catch (Exception e) {
            Log.warnf("⚠️ Failed to cleanup old tokens: %s", e.getMessage());
            return 0;
        }
    }

    /**
     * Hash a JWT token using SHA-256
     * Prevents plaintext token storage
     *
     * @param token JWT token
     * @return Hash string (hex encoded)
     */
    private String hashToken(String token) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
        byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));

        // Convert to hex string
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    /**
     * Get token by hash
     *
     * @param tokenHash Token hash
     * @return Optional AuthToken
     */
    public Optional<AuthToken> getTokenByHash(String tokenHash) {
        return tokenRepository.findByTokenHash(tokenHash);
    }

    /**
     * Get token by token ID
     *
     * @param tokenId Token ID
     * @return Optional AuthToken
     */
    public Optional<AuthToken> getTokenById(String tokenId) {
        return tokenRepository.findByTokenId(tokenId);
    }

    /**
     * Get all tokens for a user (including expired/revoked) - for audit
     *
     * @param userId User ID
     * @return List of all tokens
     */
    public List<AuthToken> getAllTokensForUser(String userId) {
        return tokenRepository.findByUserId(userId);
    }

    /**
     * Async token validation (non-blocking)
     *
     * @param token JWT token
     * @return CompletionStage with validation result
     */
    public CompletionStage<Optional<AuthToken>> validateTokenAsync(String token) {
        return CompletableFuture.supplyAsync(() -> validateToken(token));
    }

    /**
     * Async token storage
     *
     * @param userId User ID
     * @param userEmail User email
     * @param token JWT token
     * @param tokenType Token type
     * @param expiresAt Expiration time
     * @param clientIp Client IP
     * @param userAgent User agent
     * @return CompletionStage with stored token
     */
    public CompletionStage<AuthToken> storeTokenAsync(
            String userId,
            String userEmail,
            String token,
            AuthToken.TokenType tokenType,
            LocalDateTime expiresAt,
            String clientIp,
            String userAgent) {
        return CompletableFuture.supplyAsync(() ->
                storeToken(userId, userEmail, token, tokenType, expiresAt, clientIp, userAgent)
        );
    }
}
