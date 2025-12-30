package io.aurigraph.v11.user;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Role Entity - Represents user roles with granular permissions
 *
 * Stores permissions as a JSON structure for flexibility.
 * Default roles: ADMIN, USER, DEVOPS are created on startup.
 *
 * @author Backend Development Agent (BDA)
 * @since V11.3.1
 */
@Entity
@Table(name = "roles", indexes = {
    @Index(name = "idx_role_name", columnList = "name", unique = true)
})
public class Role extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", updatable = false, nullable = false)
    public UUID id;

    @NotBlank(message = "Role name is required")
    @Size(min = 2, max = 50, message = "Role name must be between 2 and 50 characters")
    @Column(name = "name", unique = true, nullable = false, length = 50)
    public String name;

    @Size(max = 255, message = "Description must be at most 255 characters")
    @Column(name = "description", length = 255)
    public String description;

    /**
     * Permissions stored as JSON string
     * Format: {"transactions": ["read", "write"], "users": ["read"], "admin": ["*"]}
     */
    @Column(name = "permissions", columnDefinition = "TEXT")
    public String permissions;

    @Column(name = "user_count")
    public int userCount = 0;

    @Column(name = "is_system_role")
    public Boolean isSystemRole = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    public Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    public Instant updatedAt;

    /**
     * Pre-persist callback - sets creation timestamp
     */
    @PrePersist
    public void prePersist() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    /**
     * Pre-update callback - updates modification timestamp
     */
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }

    /**
     * Custom finder methods using Panache
     */
    public static Role findByName(String name) {
        return find("name", name).firstResult();
    }

    public static List<Role> findSystemRoles() {
        return find("isSystemRole", true).list();
    }

    public static List<Role> findCustomRoles() {
        return find("isSystemRole", false).list();
    }

    /**
     * Check if role has a specific permission
     */
    public boolean hasPermission(String resource, String action) {
        if (permissions == null || permissions.isEmpty()) {
            return false;
        }

        // Simple permission check (can be enhanced with JSON parsing)
        String permKey = "\"" + resource + "\"";
        if (!permissions.contains(permKey)) {
            return false;
        }

        // Check for wildcard or specific action
        return permissions.contains("\"*\"") ||
               permissions.contains("\"" + action + "\"");
    }

    /**
     * Default role definitions
     */
    public static class DefaultRoles {
        public static final String ADMIN = "ADMIN";
        public static final String USER = "USER";
        public static final String DEVOPS = "DEVOPS";
        public static final String API_USER = "API_USER";
        public static final String READONLY = "READONLY";
    }

}
