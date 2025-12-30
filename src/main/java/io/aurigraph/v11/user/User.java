package io.aurigraph.v11.user;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

/**
 * User Entity - Represents system users with RBAC support
 *
 * Uses Panache pattern for simplified data access.
 * Supports role-based access control with JWT authentication.
 *
 * @author Backend Development Agent (BDA)
 * @since V11.3.1
 */
@Entity
@Table(name = "users", indexes = {
    @Index(name = "idx_username", columnList = "username", unique = true),
    @Index(name = "idx_email", columnList = "email", unique = true),
    @Index(name = "idx_status", columnList = "status")
})
public class User extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", updatable = false, nullable = false)
    public UUID id;

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    @Column(name = "username", unique = true, nullable = false, length = 50)
    public String username;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    @Size(max = 100, message = "Email must be at most 100 characters")
    @Column(name = "email", unique = true, nullable = false, length = 100)
    public String email;

    @NotBlank(message = "Password hash is required")
    @Column(name = "password_hash", nullable = false, length = 255)
    public String passwordHash;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "role_id", nullable = false)
    public Role role;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    public UserStatus status = UserStatus.ACTIVE;

    @Column(name = "created_at", nullable = false, updatable = false)
    public Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    public Instant updatedAt;

    @Column(name = "last_login_at")
    public Instant lastLoginAt;

    @Column(name = "failed_login_attempts", nullable = false, columnDefinition = "INTEGER DEFAULT 0")
    public int failedLoginAttempts = 0;

    @Column(name = "locked_until")
    public Instant lockedUntil;

    /**
     * Pre-persist callback - sets creation timestamp
     */
    @PrePersist
    public void prePersist() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
        if (this.status == null) {
            this.status = UserStatus.ACTIVE;
        }
    }

    /**
     * Pre-update callback - updates modification timestamp
     */
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }

    /**
     * Check if user is locked due to failed login attempts
     */
    public boolean isLocked() {
        if (lockedUntil == null) {
            return false;
        }
        if (Instant.now().isAfter(lockedUntil)) {
            // Lock expired, clear it
            lockedUntil = null;
            failedLoginAttempts = 0;
            return false;
        }
        return true;
    }

    /**
     * Check if user is active and not locked
     */
    public boolean isActive() {
        return status == UserStatus.ACTIVE && !isLocked();
    }

    /**
     * Record failed login attempt
     */
    public void recordFailedLogin() {
        failedLoginAttempts++;
        if (failedLoginAttempts >= 5) {
            // Lock for 30 minutes after 5 failed attempts
            lockedUntil = Instant.now().plusSeconds(1800);
        }
    }

    /**
     * Record successful login
     */
    public void recordSuccessfulLogin() {
        lastLoginAt = Instant.now();
        failedLoginAttempts = 0;
        lockedUntil = null;
    }

    /**
     * Custom finder methods using Panache
     */
    public static User findByUsername(String username) {
        return find("username", username).firstResult();
    }

    public static User findByEmail(String email) {
        return find("email", email).firstResult();
    }

    /**
     * User Status Enum
     */
    public enum UserStatus {
        ACTIVE,
        INACTIVE,
        SUSPENDED,
        PENDING_VERIFICATION
    }
}
