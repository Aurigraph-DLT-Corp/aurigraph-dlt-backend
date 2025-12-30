package io.aurigraph.v11.user;

import io.smallrye.mutiny.Uni;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.ValidationException;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * RoleResource - REST API endpoints for role management
 *
 * Provides CRUD operations for roles with permission management.
 * All endpoints are reactive using Uni for non-blocking operations.
 *
 * @author Backend Development Agent (BDA)
 * @since V11.3.1
 */
@Path("/api/v11/roles")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RoleResource {

    private static final Logger LOG = Logger.getLogger(RoleResource.class);

    @Inject
    RoleService roleService;

    /**
     * List all roles
     * GET /api/v11/roles
     * Requires: ADMIN or DEVOPS role
     */
    @GET
    @RolesAllowed({"ADMIN", "DEVOPS"})
    public Uni<Response> listRoles(
        @QueryParam("type") @DefaultValue("all") String type
    ) {
        return Uni.createFrom().item(() -> {
            LOG.infof("Listing roles - type: %s", type);

            List<Role> roles = switch (type.toLowerCase()) {
                case "system" -> roleService.listSystemRoles();
                case "custom" -> roleService.listCustomRoles();
                default -> roleService.listAllRoles();
            };

            List<RoleResponse> roleResponses = roles.stream()
                .map(this::toRoleResponse)
                .collect(Collectors.toList());

            return Response.ok(roleResponses).build();
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    /**
     * Get role by ID
     * GET /api/v11/roles/{id}
     * Requires: ADMIN or DEVOPS role
     */
    @GET
    @Path("/{id}")
    @RolesAllowed({"ADMIN", "DEVOPS"})
    public Uni<Response> getRole(@PathParam("id") String id) {
        return Uni.createFrom().item(() -> {
            try {
                LOG.infof("Getting role: %s", id);
                UUID roleId = UUID.fromString(id);
                Role role = roleService.findById(roleId);
                return Response.ok(toRoleResponse(role)).build();
            } catch (IllegalArgumentException e) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("Invalid role ID format"))
                    .build();
            } catch (ValidationException e) {
                return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorResponse(e.getMessage()))
                    .build();
            }
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    /**
     * Create new role
     * POST /api/v11/roles
     * Requires: ADMIN role
     */
    @POST
    @RolesAllowed("ADMIN")
    public Uni<Response> createRole(@Valid CreateRoleRequest request) {
        return Uni.createFrom().item(() -> {
            try {
                LOG.infof("Creating role: %s", request.name());

                Role role = roleService.createRole(
                    request.name(),
                    request.description(),
                    request.permissions()
                );

                return Response.status(Response.Status.CREATED)
                    .entity(toRoleResponse(role))
                    .build();
            } catch (ValidationException e) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse(e.getMessage()))
                    .build();
            }
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    /**
     * Update role
     * PUT /api/v11/roles/{id}
     * Requires: ADMIN role
     */
    @PUT
    @Path("/{id}")
    @RolesAllowed("ADMIN")
    public Uni<Response> updateRole(
        @PathParam("id") String id,
        @Valid UpdateRoleRequest request
    ) {
        return Uni.createFrom().item(() -> {
            try {
                LOG.infof("Updating role: %s", id);
                UUID roleId = UUID.fromString(id);

                Role role = roleService.updateRole(
                    roleId,
                    request.description(),
                    request.permissions()
                );

                return Response.ok(toRoleResponse(role)).build();
            } catch (IllegalArgumentException e) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("Invalid role ID format"))
                    .build();
            } catch (ValidationException e) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse(e.getMessage()))
                    .build();
            }
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    /**
     * Delete role
     * DELETE /api/v11/roles/{id}
     * Requires: ADMIN role
     */
    @DELETE
    @Path("/{id}")
    @RolesAllowed("ADMIN")
    public Uni<Response> deleteRole(@PathParam("id") String id) {
        return Uni.createFrom().item(() -> {
            try {
                LOG.infof("Deleting role: %s", id);
                UUID roleId = UUID.fromString(id);
                roleService.deleteRole(roleId);
                return Response.noContent().build();
            } catch (IllegalArgumentException e) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("Invalid role ID format"))
                    .build();
            } catch (ValidationException e) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse(e.getMessage()))
                    .build();
            }
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    /**
     * Get role permissions
     * GET /api/v11/roles/{id}/permissions
     * Requires: ADMIN or DEVOPS role
     */
    @GET
    @Path("/{id}/permissions")
    @RolesAllowed({"ADMIN", "DEVOPS"})
    public Uni<Response> getRolePermissions(@PathParam("id") String id) {
        return Uni.createFrom().item(() -> {
            try {
                LOG.infof("Getting permissions for role: %s", id);
                UUID roleId = UUID.fromString(id);
                String permissions = roleService.getPermissions(roleId);
                return Response.ok(new PermissionsResponse(permissions)).build();
            } catch (IllegalArgumentException e) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("Invalid role ID format"))
                    .build();
            } catch (ValidationException e) {
                return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorResponse(e.getMessage()))
                    .build();
            }
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    /**
     * Check role permission
     * GET /api/v11/roles/{id}/permissions/check?resource=transactions&action=write
     * Requires: ADMIN or DEVOPS role
     */
    @GET
    @Path("/{id}/permissions/check")
    @RolesAllowed({"ADMIN", "DEVOPS"})
    public Uni<Response> checkPermission(
        @PathParam("id") String id,
        @QueryParam("resource") String resource,
        @QueryParam("action") String action
    ) {
        return Uni.createFrom().item(() -> {
            try {
                LOG.infof("Checking permission for role: %s - resource: %s, action: %s",
                    id, resource, action);

                if (resource == null || action == null) {
                    return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new ErrorResponse("Resource and action query parameters are required"))
                        .build();
                }

                UUID roleId = UUID.fromString(id);
                boolean hasPermission = roleService.hasPermission(roleId, resource, action);

                return Response.ok(new PermissionCheckResponse(
                    roleId,
                    resource,
                    action,
                    hasPermission
                )).build();
            } catch (IllegalArgumentException e) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("Invalid role ID format"))
                    .build();
            } catch (ValidationException e) {
                return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorResponse(e.getMessage()))
                    .build();
            }
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    /**
     * Get role statistics
     * GET /api/v11/roles/{id}/statistics
     * Requires: ADMIN or DEVOPS role
     */
    @GET
    @Path("/{id}/statistics")
    @RolesAllowed({"ADMIN", "DEVOPS"})
    public Uni<Response> getRoleStatistics(@PathParam("id") String id) {
        return Uni.createFrom().item(() -> {
            try {
                LOG.infof("Getting statistics for role: %s", id);
                UUID roleId = UUID.fromString(id);
                RoleService.RoleStatistics stats = roleService.getRoleStatistics(roleId);
                return Response.ok(stats).build();
            } catch (IllegalArgumentException e) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("Invalid role ID format"))
                    .build();
            } catch (ValidationException e) {
                return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorResponse(e.getMessage()))
                    .build();
            }
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    /**
     * Convert Role entity to RoleResponse DTO
     */
    private RoleResponse toRoleResponse(Role role) {
        return new RoleResponse(
            role.id,
            role.name,
            role.description,
            role.permissions,
            role.userCount,
            role.isSystemRole,
            role.createdAt,
            role.updatedAt
        );
    }

    /**
     * Request/Response records
     */
    public record CreateRoleRequest(
        String name,
        String description,
        String permissions
    ) {}

    public record UpdateRoleRequest(
        String description,
        String permissions
    ) {}

    public record RoleResponse(
        UUID id,
        String name,
        String description,
        String permissions,
        int userCount,
        boolean isSystemRole,
        java.time.Instant createdAt,
        java.time.Instant updatedAt
    ) {}

    public record PermissionsResponse(
        String permissions
    ) {}

    public record PermissionCheckResponse(
        UUID roleId,
        String resource,
        String action,
        boolean hasPermission
    ) {}

    public record ErrorResponse(String message) {}
}
