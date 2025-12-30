package io.aurigraph.v11.rbac.models;

import java.time.Instant;
import java.util.Objects;

/**
 * Role-Permission Model for Access Control
 *
 * Represents the binding between a role and a permission
 * with grant/revoke tracking and audit support.
 *
 * @version 11.5.0
 * @since 2025-10-30 - AV11-458: RolePermissionRegistry Merkle Tree
 */
public class RolePermission {
    private String permissionId;
    private String roleId;
    private String permissionName;
    private String permissionDescription;
    private String resource;
    private String action;
    private String verificationHash;
    private Instant grantedAt;
    private Instant revokedAt;
    private RolePermissionStatus status;
    private String grantedBy;
    private String revokedBy;
    private String revocationReason;
    private int revisionNumber;

    public enum RolePermissionStatus {
        ACTIVE,
        REVOKED,
        PENDING_APPROVAL,
        SUSPENDED,
        EXPIRED
    }

    public RolePermission() {
        this.status = RolePermissionStatus.ACTIVE;
        this.revisionNumber = 1;
    }

    public RolePermission(String permissionId, String roleId, String permissionName,
                         String resource, String action, String grantedBy) {
        this.permissionId = permissionId;
        this.roleId = roleId;
        this.permissionName = permissionName;
        this.resource = resource;
        this.action = action;
        this.grantedBy = grantedBy;
        this.grantedAt = Instant.now();
        this.status = RolePermissionStatus.ACTIVE;
        this.revisionNumber = 1;
    }

    // Getters and Setters
    public String getPermissionId() {
        return permissionId;
    }

    public void setPermissionId(String permissionId) {
        this.permissionId = permissionId;
    }

    public String getRoleId() {
        return roleId;
    }

    public void setRoleId(String roleId) {
        this.roleId = roleId;
    }

    public String getPermissionName() {
        return permissionName;
    }

    public void setPermissionName(String permissionName) {
        this.permissionName = permissionName;
    }

    public String getPermissionDescription() {
        return permissionDescription;
    }

    public void setPermissionDescription(String permissionDescription) {
        this.permissionDescription = permissionDescription;
    }

    public String getResource() {
        return resource;
    }

    public void setResource(String resource) {
        this.resource = resource;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getVerificationHash() {
        return verificationHash;
    }

    public void setVerificationHash(String verificationHash) {
        this.verificationHash = verificationHash;
    }

    public Instant getGrantedAt() {
        return grantedAt;
    }

    public void setGrantedAt(Instant grantedAt) {
        this.grantedAt = grantedAt;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }

    public void setRevokedAt(Instant revokedAt) {
        this.revokedAt = revokedAt;
    }

    public RolePermissionStatus getStatus() {
        return status;
    }

    public void setStatus(RolePermissionStatus status) {
        this.status = status;
    }

    public String getGrantedBy() {
        return grantedBy;
    }

    public void setGrantedBy(String grantedBy) {
        this.grantedBy = grantedBy;
    }

    public String getRevokedBy() {
        return revokedBy;
    }

    public void setRevokedBy(String revokedBy) {
        this.revokedBy = revokedBy;
    }

    public String getRevocationReason() {
        return revocationReason;
    }

    public void setRevocationReason(String revocationReason) {
        this.revocationReason = revocationReason;
    }

    public int getRevisionNumber() {
        return revisionNumber;
    }

    public void setRevisionNumber(int revisionNumber) {
        this.revisionNumber = revisionNumber;
    }

    public boolean isActive() {
        return status == RolePermissionStatus.ACTIVE;
    }

    public boolean isRevoked() {
        return status == RolePermissionStatus.REVOKED;
    }

    public long getDurationDays() {
        if (revokedAt == null) {
            return java.time.temporal.ChronoUnit.DAYS.between(grantedAt, Instant.now());
        } else {
            return java.time.temporal.ChronoUnit.DAYS.between(grantedAt, revokedAt);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RolePermission that = (RolePermission) o;
        return Objects.equals(permissionId, that.permissionId) &&
               Objects.equals(roleId, that.roleId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(permissionId, roleId);
    }

    @Override
    public String toString() {
        return "RolePermission{" +
                "permissionId='" + permissionId + '\'' +
                ", roleId='" + roleId + '\'' +
                ", permissionName='" + permissionName + '\'' +
                ", action='" + action + '\'' +
                ", status=" + status +
                ", isActive=" + isActive() +
                '}';
    }
}
