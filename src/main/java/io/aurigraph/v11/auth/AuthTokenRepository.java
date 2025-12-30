package io.aurigraph.v11.auth;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * AuthTokenRepository
 *
 * Data access layer for AuthToken entities
 * Provides reactive query methods for token operations
 */
@ApplicationScoped
public class AuthTokenRepository implements PanacheRepository<AuthToken> {

    /**
     * Find a token by its hash
     */
    public Optional<AuthToken> findByTokenHash(String tokenHash) {
        return find("tokenHash", tokenHash).firstResultOptional();
    }

    /**
     * Find a token by token ID
     */
    public Optional<AuthToken> findByTokenId(String tokenId) {
        return find("tokenId", tokenId).firstResultOptional();
    }

    /**
     * Find all active tokens for a user
     */
    public List<AuthToken> findActiveTokensByUserId(String userId) {
        return find(
            "userId = ?1 AND isRevoked = false AND status = ?2",
            userId,
            AuthToken.TokenStatus.ACTIVE
        ).list();
    }

    /**
     * Find all tokens for a user (including expired/revoked)
     */
    public List<AuthToken> findByUserId(String userId) {
        return find("userId", userId).list();
    }

    /**
     * Find active access tokens for a user
     */
    public List<AuthToken> findActiveAccessTokensByUserId(String userId) {
        return find(
            "userId = ?1 AND tokenType = ?2 AND isRevoked = false AND status = ?3",
            userId,
            AuthToken.TokenType.ACCESS,
            AuthToken.TokenStatus.ACTIVE
        ).list();
    }

    /**
     * Find active refresh tokens for a user
     */
    public List<AuthToken> findActiveRefreshTokensByUserId(String userId) {
        return find(
            "userId = ?1 AND tokenType = ?2 AND isRevoked = false AND status = ?3",
            userId,
            AuthToken.TokenType.REFRESH,
            AuthToken.TokenStatus.ACTIVE
        ).list();
    }

    /**
     * Find expired tokens (for cleanup)
     */
    public List<AuthToken> findExpiredTokens() {
        return find(
            "expiresAt < ?1 AND status != ?2",
            LocalDateTime.now(),
            AuthToken.TokenStatus.REVOKED
        ).list();
    }

    /**
     * Find tokens that expired more than N days ago
     */
    public List<AuthToken> findOldExpiredTokens(int daysOld) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(daysOld);
        return find(
            "expiresAt < ?1 AND status = ?2",
            cutoffDate,
            AuthToken.TokenStatus.EXPIRED
        ).list();
    }

    /**
     * Count active tokens for a user
     */
    public long countActiveTokensForUser(String userId) {
        return count(
            "userId = ?1 AND isRevoked = false AND status = ?2",
            userId,
            AuthToken.TokenStatus.ACTIVE
        );
    }

    /**
     * Revoke all tokens for a user
     */
    public int revokeAllTokensForUser(String userId, String reason) {
        return update(
            "isRevoked = true, revocationReason = ?1, revokedAt = ?2, status = ?3 " +
            "WHERE userId = ?4 AND isRevoked = false",
            reason,
            LocalDateTime.now(),
            AuthToken.TokenStatus.REVOKED,
            userId
        );
    }

    /**
     * Revoke all access tokens for a user (keep refresh tokens)
     */
    public int revokeAllAccessTokensForUser(String userId, String reason) {
        return update(
            "isRevoked = true, revocationReason = ?1, revokedAt = ?2, status = ?3 " +
            "WHERE userId = ?4 AND tokenType = ?5 AND isRevoked = false",
            reason,
            LocalDateTime.now(),
            AuthToken.TokenStatus.REVOKED,
            userId,
            AuthToken.TokenType.ACCESS
        );
    }

    /**
     * Delete expired tokens (cleanup)
     */
    public int deleteExpiredTokens() {
        return (int) delete(
            "expiresAt < ?1",
            LocalDateTime.now()
        );
    }

    /**
     * Delete old expired tokens (older than N days)
     */
    public int deleteOldExpiredTokens(int daysOld) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(daysOld);
        return (int) delete(
            "expiresAt < ?1 AND status = ?2",
            cutoffDate,
            AuthToken.TokenStatus.EXPIRED
        );
    }

    /**
     * Find the most recently created token for a user
     */
    public Optional<AuthToken> findMostRecentTokenForUser(String userId) {
        return find(
            "userId",
            Sort.descending("createdAt"),
            userId
        ).firstResultOptional();
    }

    /**
     * Find tokens that haven't been used for N days
     */
    public List<AuthToken> findUnusedTokens(int daysOld) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(daysOld);
        return find(
            "lastUsedAt < ?1 OR lastUsedAt IS NULL AND createdAt < ?2 AND isRevoked = false",
            cutoffDate,
            cutoffDate
        ).list();
    }

    /**
     * Count tokens by status for a user
     */
    public long countTokensByStatus(String userId, AuthToken.TokenStatus status) {
        return count("userId = ?1 AND status = ?2", userId, status);
    }

    /**
     * Find tokens from a specific IP address
     */
    public List<AuthToken> findTokensByClientIp(String clientIp) {
        return find("clientIp", clientIp).list();
    }

    /**
     * Find tokens from a specific IP for a user
     */
    public List<AuthToken> findTokensByUserIdAndClientIp(String userId, String clientIp) {
        return find("userId = ?1 AND clientIp = ?2", userId, clientIp).list();
    }
}
