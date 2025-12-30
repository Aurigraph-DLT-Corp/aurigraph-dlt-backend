package io.aurigraph.v11.auth;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * AuthToken Entity
 *
 * Stores JWT tokens in the database for:
 * - Token validation and revocation
 * - Token lifecycle tracking (created, last_used, expires)
 * - User session management
 * - Audit logging and security monitoring
 * - Token refresh tracking
 *
 * Features:
 * - Unique token storage with hash to prevent plaintext exposure
 * - User association for revocation by user
 * - IP address tracking for security monitoring
 * - User agent tracking for device identification
 * - Token type (ACCESS, REFRESH)
 * - Revocation capability with reason tracking
 */
@Entity
@Table(name = "auth_tokens", indexes = {
    @Index(name = "idx_user_id", columnList = "user_id"),
    @Index(name = "idx_token_hash", columnList = "token_hash", unique = true),
    @Index(name = "idx_expires_at", columnList = "expires_at"),
    @Index(name = "idx_is_revoked", columnList = "is_revoked"),
    @Index(name = "idx_created_at", columnList = "created_at")
})
public class AuthToken extends PanacheEntity {

    /**
     * Unique token identifier (not the actual token)
     */
    @Column(name = "token_id", nullable = false, unique = true)
    public String tokenId = UUID.randomUUID().toString();

    /**
     * User ID associated with this token
     */
    @Column(name = "user_id", nullable = false)
    public String userId;

    /**
     * User email for easy reference (denormalized for queries)
     */
    @Column(name = "user_email", nullable = false)
    public String userEmail;

    /**
     * Hash of the JWT token (using SHA-256)
     * Stored instead of plaintext for security
     */
    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    public String tokenHash;

    /**
     * Token type: ACCESS or REFRESH
     */
    @Column(name = "token_type", nullable = false)
    @Enumerated(EnumType.STRING)
    public TokenType tokenType = TokenType.ACCESS;

    /**
     * Token expiration timestamp
     */
    @Column(name = "expires_at", nullable = false)
    public LocalDateTime expiresAt;

    /**
     * Token creation timestamp
     */
    @Column(name = "created_at", nullable = false)
    public LocalDateTime createdAt = LocalDateTime.now();

    /**
     * Token last used timestamp
     */
    @Column(name = "last_used_at")
    public LocalDateTime lastUsedAt;

    /**
     * Client IP address where token was created
     */
    @Column(name = "client_ip", length = 45)
    public String clientIp;

    /**
     * User agent of client that created the token
     */
    @Column(name = "user_agent", length = 255)
    public String userAgent;

    /**
     * Token revocation status
     */
    @Column(name = "is_revoked", nullable = false)
    public Boolean isRevoked = false;

    /**
     * Reason for token revocation
     */
    @Column(name = "revocation_reason", length = 255)
    public String revocationReason;

    /**
     * Timestamp when token was revoked
     */
    @Column(name = "revoked_at")
    public LocalDateTime revokedAt;

    /**
     * Parent token ID (for tracking refresh token lineage)
     */
    @Column(name = "parent_token_id", length = 36)
    public String parentTokenId;

    /**
     * Whether this token has been refreshed
     */
    @Column(name = "is_refreshed", nullable = false)
    public Boolean isRefreshed = false;

    /**
     * ID of refresh token created from this token
     */
    @Column(name = "refresh_token_id", length = 36)
    public String refreshTokenId;

    /**
     * Token status: ACTIVE, EXPIRED, REVOKED, REFRESHED
     */
    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    public TokenStatus status = TokenStatus.ACTIVE;

    /**
     * Additional metadata (JSON format)
     */
    @Column(name = "metadata", columnDefinition = "TEXT")
    public String metadata;

    /**
     * Constructor for new tokens
     */
    public AuthToken() {
    }

    /**
     * Constructor with required fields
     */
    public AuthToken(String userId, String userEmail, String tokenHash, TokenType tokenType, LocalDateTime expiresAt) {
        this.userId = userId;
        this.userEmail = userEmail;
        this.tokenHash = tokenHash;
        this.tokenType = tokenType;
        this.expiresAt = expiresAt;
        this.createdAt = LocalDateTime.now();
    }

    /**
     * Check if token is valid (not expired, not revoked)
     */
    public boolean isValid() {
        return !isRevoked && 
               status == TokenStatus.ACTIVE && 
               !LocalDateTime.now().isAfter(expiresAt);
    }

    /**
     * Check if token is expired
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    /**
     * Mark token as used
     */
    public void markUsed() {
        this.lastUsedAt = LocalDateTime.now();
    }

    /**
     * Revoke the token
     */
    public void revoke(String reason) {
        this.isRevoked = true;
        this.revocationReason = reason;
        this.revokedAt = LocalDateTime.now();
        this.status = TokenStatus.REVOKED;
    }

    /**
     * Mark token as refreshed
     */
    public void markAsRefreshed(String newTokenId) {
        this.isRefreshed = true;
        this.refreshTokenId = newTokenId;
        this.status = TokenStatus.REFRESHED;
    }

    /**
     * Token type enum
     */
    public enum TokenType {
        ACCESS,      // JWT access token
        REFRESH      // Refresh token
    }

    /**
     * Token status enum
     */
    public enum TokenStatus {
        ACTIVE,      // Token is valid and usable
        EXPIRED,     // Token has expired
        REVOKED,     // Token was explicitly revoked
        REFRESHED    // Token was used to create a new token
    }
}
