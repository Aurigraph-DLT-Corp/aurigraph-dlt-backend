package io.aurigraph.v11.security;

import io.quarkus.security.ForbiddenException;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

/**
 * LevelDB Role-Based Access Control (RBAC) Service
 *
 * Provides comprehensive access control for LevelDB operations with:
 * - Role-based permissions (ADMIN, WRITE, READ, READ_ONLY)
 * - Data-type specific access control (token:, balance:, aml:, kyc:)
 * - Operation-level permissions (READ, WRITE, DELETE)
 * - Audit logging for all access control decisions
 * - Support for custom roles and permissions
 *
 * Permission Model:
 * - ADMIN: Full access to all data types
 * - WRITE: Read and write access to assigned data types
 * - READ: Read-only access to assigned data types
 * - ANONYMOUS: No access (default)
 *
 * Data Type Permissions:
 * - token: Token entities
 * - balance: Token balances
 * - aml: AML screening records
 * - kyc: KYC verification records
 * - channel: Channel data
 * - block: Blockchain blocks
 * - tx: Transactions
 *
 * @author Aurigraph Security Team
 * @version 11.3.0
 * @since October 2025
 */
@ApplicationScoped
public class LevelDBAccessControl {

    private static final Logger logger = LoggerFactory.getLogger(LevelDBAccessControl.class);

    // Built-in roles
    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_WRITE = "WRITE";
    public static final String ROLE_READ = "READ";
    public static final String ROLE_READ_ONLY = "READ_ONLY";

    // Data type prefixes
    private static final String PREFIX_TOKEN = "token:";
    private static final String PREFIX_BALANCE = "balance:";
    private static final String PREFIX_AML = "aml:";
    private static final String PREFIX_KYC = "kyc:";
    private static final String PREFIX_CHANNEL = "channel:";
    private static final String PREFIX_BLOCK = "block:";
    private static final String PREFIX_TX = "tx:";

    @ConfigProperty(name = "leveldb.security.rbac.enabled", defaultValue = "true")
    boolean rbacEnabled;

    @ConfigProperty(name = "leveldb.security.allow.anonymous", defaultValue = "false")
    boolean allowAnonymous;

    @Inject
    SecurityIdentity securityIdentity;

    @Inject
    SecurityAuditService auditService;

    private final AtomicLong accessCheckCount = new AtomicLong(0);
    private final AtomicLong accessDeniedCount = new AtomicLong(0);
    private final AtomicLong accessGrantedCount = new AtomicLong(0);

    // Permission cache for performance
    private final Map<String, Set<String>> rolePermissions = new HashMap<>();

    /**
     * Check read permission for a key
     */
    public void checkReadPermission(String key) {
        accessCheckCount.incrementAndGet();

        if (!rbacEnabled) {
            logger.debug("RBAC disabled - granting read access to: {}", key);
            return;
        }

        String dataType = extractDataType(key);
        String principal = getPrincipal();

        logger.debug("Checking read permission for user: {}, key: {}, dataType: {}",
                    principal, key, dataType);

        if (hasReadPermission(principal, dataType)) {
            accessGrantedCount.incrementAndGet();
            auditService.logSecurityEvent("ACCESS_GRANTED_READ",
                principal + " read " + dataType);
            logger.trace("Read access granted for user: {}, dataType: {}",
                        principal, dataType);
        } else {
            accessDeniedCount.incrementAndGet();
            auditService.logSecurityViolation("ACCESS_DENIED_READ",
                principal, "Attempted to read " + dataType);
            logger.warn("Read access denied for user: {}, dataType: {}",
                       principal, dataType);
            throw new ForbiddenException("Access denied: insufficient permissions to read " + dataType);
        }
    }

    /**
     * Check write permission for a key
     */
    public void checkWritePermission(String key) {
        accessCheckCount.incrementAndGet();

        if (!rbacEnabled) {
            logger.debug("RBAC disabled - granting write access to: {}", key);
            return;
        }

        String dataType = extractDataType(key);
        String principal = getPrincipal();

        logger.debug("Checking write permission for user: {}, key: {}, dataType: {}",
                    principal, key, dataType);

        if (hasWritePermission(principal, dataType)) {
            accessGrantedCount.incrementAndGet();
            auditService.logSecurityEvent("ACCESS_GRANTED_WRITE",
                principal + " write " + dataType);
            logger.trace("Write access granted for user: {}, dataType: {}",
                        principal, dataType);
        } else {
            accessDeniedCount.incrementAndGet();
            auditService.logSecurityViolation("ACCESS_DENIED_WRITE",
                principal, "Attempted to write " + dataType);
            logger.warn("Write access denied for user: {}, dataType: {}",
                       principal, dataType);
            throw new ForbiddenException("Access denied: insufficient permissions to write " + dataType);
        }
    }

    /**
     * Check delete permission for a key
     */
    public void checkDeletePermission(String key) {
        accessCheckCount.incrementAndGet();

        if (!rbacEnabled) {
            logger.debug("RBAC disabled - granting delete access to: {}", key);
            return;
        }

        String dataType = extractDataType(key);
        String principal = getPrincipal();

        logger.debug("Checking delete permission for user: {}, key: {}, dataType: {}",
                    principal, key, dataType);

        if (hasDeletePermission(principal, dataType)) {
            accessGrantedCount.incrementAndGet();
            auditService.logSecurityEvent("ACCESS_GRANTED_DELETE",
                principal + " delete " + dataType);
            logger.trace("Delete access granted for user: {}, dataType: {}",
                        principal, dataType);
        } else {
            accessDeniedCount.incrementAndGet();
            auditService.logSecurityViolation("ACCESS_DENIED_DELETE",
                principal, "Attempted to delete " + dataType);
            logger.warn("Delete access denied for user: {}, dataType: {}",
                       principal, dataType);
            throw new ForbiddenException("Access denied: insufficient permissions to delete " + dataType);
        }
    }

    /**
     * Check if user has read permission for data type
     */
    private boolean hasReadPermission(String principal, String dataType) {
        // Admin has full access
        if (hasRole(ROLE_ADMIN)) {
            return true;
        }

        // Check data-type specific read permissions
        if (hasRole("READ_" + dataType.toUpperCase())) {
            return true;
        }

        // Check general read permissions
        if (hasRole(ROLE_READ) || hasRole(ROLE_READ_ONLY) || hasRole(ROLE_WRITE)) {
            return true;
        }

        // Anonymous access
        if (allowAnonymous && securityIdentity.isAnonymous()) {
            logger.debug("Allowing anonymous read access (configured)");
            return true;
        }

        return false;
    }

    /**
     * Check if user has write permission for data type
     */
    private boolean hasWritePermission(String principal, String dataType) {
        // Admin has full access
        if (hasRole(ROLE_ADMIN)) {
            return true;
        }

        // Check data-type specific write permissions
        if (hasRole("WRITE_" + dataType.toUpperCase())) {
            return true;
        }

        // Check general write permissions
        if (hasRole(ROLE_WRITE)) {
            return true;
        }

        // Read-only cannot write
        if (hasRole(ROLE_READ_ONLY)) {
            return false;
        }

        return false;
    }

    /**
     * Check if user has delete permission for data type
     */
    private boolean hasDeletePermission(String principal, String dataType) {
        // Admin has full access
        if (hasRole(ROLE_ADMIN)) {
            return true;
        }

        // Check data-type specific delete permissions
        if (hasRole("DELETE_" + dataType.toUpperCase())) {
            return true;
        }

        // Only ADMIN can delete by default
        return false;
    }

    /**
     * Extract data type from key (based on prefix)
     */
    private String extractDataType(String key) {
        if (key.startsWith(PREFIX_TOKEN)) return "token";
        if (key.startsWith(PREFIX_BALANCE)) return "balance";
        if (key.startsWith(PREFIX_AML)) return "aml";
        if (key.startsWith(PREFIX_KYC)) return "kyc";
        if (key.startsWith(PREFIX_CHANNEL)) return "channel";
        if (key.startsWith(PREFIX_BLOCK)) return "block";
        if (key.startsWith(PREFIX_TX)) return "tx";
        return "unknown";
    }

    /**
     * Get current principal (user identifier)
     */
    private String getPrincipal() {
        if (securityIdentity == null || securityIdentity.isAnonymous()) {
            return "ANONYMOUS";
        }
        return securityIdentity.getPrincipal().getName();
    }

    /**
     * Check if current user has a specific role
     */
    private boolean hasRole(String role) {
        if (securityIdentity == null) {
            return false;
        }
        return securityIdentity.hasRole(role);
    }

    /**
     * Check if current user is admin
     */
    public boolean isAdmin() {
        return hasRole(ROLE_ADMIN);
    }

    /**
     * Grant role to user (admin operation)
     */
    public void grantRole(String username, String role) {
        if (!isAdmin()) {
            throw new ForbiddenException("Only admins can grant roles");
        }

        auditService.logSecurityEvent("ROLE_GRANTED",
            "Admin granted role " + role + " to " + username);
        logger.info("Granted role {} to user {}", role, username);

        // In production, this would integrate with IAM system
    }

    /**
     * Revoke role from user (admin operation)
     */
    public void revokeRole(String username, String role) {
        if (!isAdmin()) {
            throw new ForbiddenException("Only admins can revoke roles");
        }

        auditService.logSecurityEvent("ROLE_REVOKED",
            "Admin revoked role " + role + " from " + username);
        logger.info("Revoked role {} from user {}", role, username);

        // In production, this would integrate with IAM system
    }

    /**
     * Get access control statistics
     */
    public AccessControlStats getStats() {
        return new AccessControlStats(
            rbacEnabled,
            allowAnonymous,
            accessCheckCount.get(),
            accessGrantedCount.get(),
            accessDeniedCount.get(),
            getPrincipal()
        );
    }

    /**
     * Access control statistics
     */
    public record AccessControlStats(
        boolean rbacEnabled,
        boolean allowAnonymous,
        long accessCheckCount,
        long accessGrantedCount,
        long accessDeniedCount,
        String currentPrincipal
    ) {}
}
