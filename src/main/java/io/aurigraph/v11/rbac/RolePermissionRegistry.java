package io.aurigraph.v11.rbac;

import io.aurigraph.v11.rbac.models.RolePermission;
import io.aurigraph.v11.merkle.MerkleProof;
import io.aurigraph.v11.merkle.MerkleTreeRegistry;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Role-Permission Registry with Merkle Tree Support
 *
 * Registry for managing role-permission bindings with cryptographic
 * verification, audit trail support, and proof generation.
 *
 * Features:
 * - Role-permission assignment management
 * - Permission grant and revocation tracking
 * - Merkle tree cryptographic verification
 * - Proof generation and verification
 * - Audit trail maintenance
 * - Access control policy enforcement
 * - Role hierarchy support
 *
 * @version 11.5.0
 * @since 2025-10-30 - AV11-458: RolePermissionRegistry Merkle Tree
 */
@ApplicationScoped
public class RolePermissionRegistry extends MerkleTreeRegistry<RolePermission> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RolePermissionRegistry.class);

    @Override
    protected String serializeValue(RolePermission rolePermission) {
        return String.format("%s|%s|%s|%s|%s|%s|%s|%s",
            rolePermission.getPermissionId(),
            rolePermission.getRoleId(),
            rolePermission.getPermissionName(),
            rolePermission.getResource(),
            rolePermission.getAction(),
            rolePermission.getStatus(),
            rolePermission.getGrantedAt(),
            rolePermission.getVerificationHash()
        );
    }

    /**
     * Grant a permission to a role
     */
    public Uni<RolePermission> grantPermission(RolePermission rolePermission) {
        return Uni.createFrom().item(() -> {
            LOGGER.info("Granting permission: {} to role: {} (resource: {}, action: {})",
                rolePermission.getPermissionName(), rolePermission.getRoleId(),
                rolePermission.getResource(), rolePermission.getAction());

            if (rolePermission.getGrantedAt() == null) {
                rolePermission.setGrantedAt(Instant.now());
            }

            return rolePermission;
        }).flatMap(rp -> add(rp.getPermissionId(), rp).map(success -> rp));
    }

    /**
     * Get role permission by ID
     */
    public Uni<RolePermission> getPermission(String permissionId) {
        return get(permissionId).onItem().ifNull().failWith(() ->
            new RolePermissionNotFoundException("Role permission not found: " + permissionId));
    }

    /**
     * Revoke a permission from a role
     */
    public Uni<RolePermission> revokePermission(String permissionId, String revokedBy, String reason) {
        return getPermission(permissionId).map(rolePermission -> {
            rolePermission.setStatus(RolePermission.RolePermissionStatus.REVOKED);
            rolePermission.setRevokedAt(Instant.now());
            rolePermission.setRevokedBy(revokedBy);
            rolePermission.setRevocationReason(reason);
            rolePermission.setRevisionNumber(rolePermission.getRevisionNumber() + 1);
            registry.put(permissionId, rolePermission);
            LOGGER.info("Revoked permission: {} from role: {} | Reason: {}",
                permissionId, rolePermission.getRoleId(), reason);
            return rolePermission;
        });
    }

    /**
     * Get all permissions for a role
     */
    public Uni<List<RolePermission>> getPermissionsForRole(String roleId) {
        return Uni.createFrom().item(() -> {
            treeLock.readLock().lock();
            try {
                return registry.values().stream()
                    .filter(rp -> rp.getRoleId().equals(roleId))
                    .collect(Collectors.toList());
            } finally {
                treeLock.readLock().unlock();
            }
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    /**
     * Get active permissions for a role
     */
    public Uni<List<RolePermission>> getActivePermissionsForRole(String roleId) {
        return getPermissionsForRole(roleId).map(permissions ->
            permissions.stream()
                .filter(RolePermission::isActive)
                .collect(Collectors.toList())
        );
    }

    /**
     * Check if role has specific permission
     */
    public Uni<Boolean> hasPermission(String roleId, String resource, String action) {
        return getActivePermissionsForRole(roleId).map(permissions ->
            permissions.stream()
                .anyMatch(rp -> rp.getResource().equals(resource) && rp.getAction().equals(action))
        );
    }

    /**
     * Get permissions for specific resource
     */
    public Uni<List<RolePermission>> getPermissionsForResource(String resource) {
        return Uni.createFrom().item(() -> {
            treeLock.readLock().lock();
            try {
                return registry.values().stream()
                    .filter(rp -> rp.getResource().equals(resource))
                    .collect(Collectors.toList());
            } finally {
                treeLock.readLock().unlock();
            }
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    /**
     * Get permissions by action
     */
    public Uni<List<RolePermission>> getPermissionsByAction(String action) {
        return Uni.createFrom().item(() -> {
            treeLock.readLock().lock();
            try {
                return registry.values().stream()
                    .filter(rp -> rp.getAction().equals(action))
                    .collect(Collectors.toList());
            } finally {
                treeLock.readLock().unlock();
            }
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    /**
     * Verify permission validity
     */
    public Uni<PermissionValidationResult> verifyPermission(String permissionId) {
        return getPermission(permissionId).map(rolePermission -> {
            boolean isValid = rolePermission.isActive();
            String reason = null;

            if (rolePermission.isRevoked()) {
                reason = "Permission has been revoked: " + rolePermission.getRevocationReason();
            } else if (rolePermission.getStatus() == RolePermission.RolePermissionStatus.SUSPENDED) {
                reason = "Permission is suspended";
            } else if (rolePermission.getStatus() == RolePermission.RolePermissionStatus.PENDING_APPROVAL) {
                reason = "Permission is pending approval";
            }

            return new PermissionValidationResult(
                isValid,
                rolePermission.getStatus().toString(),
                isValid ? "Permission is valid" : reason
            );
        });
    }

    /**
     * Count permissions by status
     */
    public Uni<PermissionCountByStatus> countPermissionsByStatus() {
        return Uni.createFrom().item(() -> {
            treeLock.readLock().lock();
            try {
                Map<RolePermission.RolePermissionStatus, Long> counts = registry.values().stream()
                    .collect(Collectors.groupingBy(RolePermission::getStatus, Collectors.counting()));

                return new PermissionCountByStatus(
                    counts.getOrDefault(RolePermission.RolePermissionStatus.ACTIVE, 0L),
                    counts.getOrDefault(RolePermission.RolePermissionStatus.REVOKED, 0L),
                    counts.getOrDefault(RolePermission.RolePermissionStatus.PENDING_APPROVAL, 0L),
                    counts.getOrDefault(RolePermission.RolePermissionStatus.SUSPENDED, 0L)
                );
            } finally {
                treeLock.readLock().unlock();
            }
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    /**
     * Generate Merkle proof for a role permission
     */
    public Uni<MerkleProof.ProofData> getProof(String permissionId) {
        return generateProof(permissionId).map(MerkleProof::toProofData);
    }

    /**
     * Verify a Merkle proof
     */
    public Uni<PermissionVerificationResponse> verifyMerkleProof(MerkleProof.ProofData proofData) {
        return verifyProof(proofData.toMerkleProof()).map(valid ->
            new PermissionVerificationResponse(valid, valid ? "Merkle proof verified" : "Invalid Merkle proof")
        );
    }

    /**
     * Get Merkle root hash for audit
     */
    public Uni<PermissionRootHashResponse> getMerkleRootHash() {
        return getRootHash().flatMap(rootHash ->
            getTreeStats().map(stats -> new PermissionRootHashResponse(
                rootHash,
                Instant.now(),
                stats.getEntryCount(),
                stats.getTreeHeight()
            ))
        );
    }

    /**
     * Get registry statistics
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalPermissions", registry.size());

        long activeCount = registry.values().stream()
            .filter(RolePermission::isActive)
            .count();
        stats.put("activePermissions", activeCount);

        long revokedCount = registry.values().stream()
            .filter(RolePermission::isRevoked)
            .count();
        stats.put("revokedPermissions", revokedCount);

        long uniqueRoles = registry.values().stream()
            .map(RolePermission::getRoleId)
            .distinct()
            .count();
        stats.put("uniqueRoles", uniqueRoles);

        long uniqueResources = registry.values().stream()
            .map(RolePermission::getResource)
            .distinct()
            .count();
        stats.put("uniqueResources", uniqueResources);

        return stats;
    }

    // Custom Exceptions
    public static class RolePermissionNotFoundException extends RuntimeException {
        public RolePermissionNotFoundException(String message) {
            super(message);
        }
    }

    // Response Classes
    public static class PermissionValidationResult {
        private final boolean valid;
        private final String status;
        private final String message;

        public PermissionValidationResult(boolean valid, String status, String message) {
            this.valid = valid;
            this.status = status;
            this.message = message;
        }

        public boolean isValid() { return valid; }
        public String getStatus() { return status; }
        public String getMessage() { return message; }
    }

    public static class PermissionVerificationResponse {
        private final boolean valid;
        private final String message;

        public PermissionVerificationResponse(boolean valid, String message) {
            this.valid = valid;
            this.message = message;
        }

        public boolean isValid() { return valid; }
        public String getMessage() { return message; }
    }

    public static class PermissionRootHashResponse {
        private final String rootHash;
        private final Instant timestamp;
        private final int entryCount;
        private final int treeHeight;

        public PermissionRootHashResponse(String rootHash, Instant timestamp, int entryCount, int treeHeight) {
            this.rootHash = rootHash;
            this.timestamp = timestamp;
            this.entryCount = entryCount;
            this.treeHeight = treeHeight;
        }

        public String getRootHash() { return rootHash; }
        public Instant getTimestamp() { return timestamp; }
        public int getEntryCount() { return entryCount; }
        public int getTreeHeight() { return treeHeight; }
    }

    public static class PermissionCountByStatus {
        private final long active;
        private final long revoked;
        private final long pendingApproval;
        private final long suspended;

        public PermissionCountByStatus(long active, long revoked, long pendingApproval, long suspended) {
            this.active = active;
            this.revoked = revoked;
            this.pendingApproval = pendingApproval;
            this.suspended = suspended;
        }

        public long getActive() { return active; }
        public long getRevoked() { return revoked; }
        public long getPendingApproval() { return pendingApproval; }
        public long getSuspended() { return suspended; }
    }
}
