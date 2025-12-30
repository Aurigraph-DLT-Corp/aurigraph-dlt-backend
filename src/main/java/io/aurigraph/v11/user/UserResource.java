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
 * UserResource - REST API endpoints for user management
 *
 * Provides CRUD operations for users with role-based access control.
 * All endpoints are reactive using Uni for non-blocking operations.
 *
 * @author Backend Development Agent (BDA)
 * @since V11.3.1
 */
@Path("/api/v11/users")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class UserResource {

    private static final Logger LOG = Logger.getLogger(UserResource.class);

    @Inject
    UserService userService;

    @Inject
    JwtService jwtService;

    /**
     * List all users with pagination
     * GET /api/v11/users?page=0&size=20
     * Requires: ADMIN or DEVOPS role
     */
    @GET
    @RolesAllowed({"ADMIN", "DEVOPS"})
    public Uni<Response> listUsers(
        @QueryParam("page") @DefaultValue("0") int page,
        @QueryParam("size") @DefaultValue("20") int size
    ) {
        return Uni.createFrom().item(() -> {
            LOG.infof("Listing users - page: %d, size: %d", page, size);

            List<User> users = userService.listUsers(page, size);
            long totalCount = userService.countUsers();

            // Convert to DTOs (hide password hash)
            List<UserResponse> userResponses = users.stream()
                .map(this::toUserResponse)
                .collect(Collectors.toList());

            PagedResponse<UserResponse> response = new PagedResponse<>(
                userResponses,
                page,
                size,
                totalCount
            );

            return Response.ok(response).build();
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    /**
     * Get user by ID
     * GET /api/v11/users/{id}
     * Requires: ADMIN, DEVOPS, or USER role
     */
    @GET
    @Path("/{id}")
    @RolesAllowed({"ADMIN", "DEVOPS", "USER"})
    public Uni<Response> getUser(@PathParam("id") String id) {
        return Uni.createFrom().item(() -> {
            try {
                LOG.infof("Getting user: %s", id);
                UUID userId = UUID.fromString(id);
                User user = userService.findById(userId);
                return Response.ok(toUserResponse(user)).build();
            } catch (IllegalArgumentException e) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("Invalid user ID format"))
                    .build();
            } catch (ValidationException e) {
                return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorResponse(e.getMessage()))
                    .build();
            }
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    /**
     * Create new user
     * POST /api/v11/users
     * Requires: ADMIN role
     */
    @POST
    @RolesAllowed("ADMIN")
    public Uni<Response> createUser(@Valid CreateUserRequest request) {
        return Uni.createFrom().item(() -> {
            try {
                LOG.infof("Creating user: %s", request.username());

                User user = userService.createUser(
                    request.username(),
                    request.email(),
                    request.password(),
                    request.roleName()
                );

                return Response.status(Response.Status.CREATED)
                    .entity(toUserResponse(user))
                    .build();
            } catch (ValidationException e) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse(e.getMessage()))
                    .build();
            }
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    /**
     * Update user
     * PUT /api/v11/users/{id}
     * Requires: ADMIN role
     */
    @PUT
    @Path("/{id}")
    @RolesAllowed("ADMIN")
    public Uni<Response> updateUser(
        @PathParam("id") String id,
        @Valid UpdateUserRequest request
    ) {
        return Uni.createFrom().item(() -> {
            try {
                LOG.infof("Updating user: %s", id);
                UUID userId = UUID.fromString(id);

                User user = userService.updateUser(
                    userId,
                    request.email(),
                    request.roleName()
                );

                return Response.ok(toUserResponse(user)).build();
            } catch (IllegalArgumentException e) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("Invalid user ID format"))
                    .build();
            } catch (ValidationException e) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse(e.getMessage()))
                    .build();
            }
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    /**
     * Delete user
     * DELETE /api/v11/users/{id}
     * Requires: ADMIN role
     */
    @DELETE
    @Path("/{id}")
    @RolesAllowed("ADMIN")
    public Uni<Response> deleteUser(@PathParam("id") String id) {
        return Uni.createFrom().item(() -> {
            try {
                LOG.infof("Deleting user: %s", id);
                UUID userId = UUID.fromString(id);
                userService.deleteUser(userId);
                return Response.noContent().build();
            } catch (IllegalArgumentException e) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("Invalid user ID format"))
                    .build();
            } catch (ValidationException e) {
                return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorResponse(e.getMessage()))
                    .build();
            }
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    /**
     * Update user role
     * PUT /api/v11/users/{id}/role
     * Requires: ADMIN role
     */
    @PUT
    @Path("/{id}/role")
    @RolesAllowed("ADMIN")
    public Uni<Response> updateUserRole(
        @PathParam("id") String id,
        UpdateRoleRequest request
    ) {
        return Uni.createFrom().item(() -> {
            try {
                LOG.infof("Updating role for user: %s to %s", id, request.roleName());
                UUID userId = UUID.fromString(id);

                User user = userService.updateUser(userId, null, request.roleName());
                return Response.ok(toUserResponse(user)).build();
            } catch (IllegalArgumentException e) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("Invalid user ID format"))
                    .build();
            } catch (ValidationException e) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse(e.getMessage()))
                    .build();
            }
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    /**
     * Update user status
     * PUT /api/v11/users/{id}/status
     * Requires: ADMIN role
     */
    @PUT
    @Path("/{id}/status")
    @RolesAllowed("ADMIN")
    public Uni<Response> updateUserStatus(
        @PathParam("id") String id,
        UpdateStatusRequest request
    ) {
        return Uni.createFrom().item(() -> {
            try {
                LOG.infof("Updating status for user: %s to %s", id, request.status());
                UUID userId = UUID.fromString(id);

                User user = userService.updateStatus(userId, request.status());
                return Response.ok(toUserResponse(user)).build();
            } catch (IllegalArgumentException e) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("Invalid user ID format or status"))
                    .build();
            } catch (ValidationException e) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse(e.getMessage()))
                    .build();
            }
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    /**
     * Update user password
     * PUT /api/v11/users/{id}/password
     * Requires: ADMIN or USER role (users can only change their own password)
     */
    @PUT
    @Path("/{id}/password")
    @RolesAllowed({"ADMIN", "USER"})
    public Uni<Response> updatePassword(
        @PathParam("id") String id,
        UpdatePasswordRequest request
    ) {
        return Uni.createFrom().item(() -> {
            try {
                LOG.infof("Updating password for user: %s", id);
                UUID userId = UUID.fromString(id);
                userService.updatePassword(userId, request.newPassword());
                return Response.ok(new SuccessResponse("Password updated successfully")).build();
            } catch (IllegalArgumentException e) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("Invalid user ID format"))
                    .build();
            } catch (ValidationException e) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse(e.getMessage()))
                    .build();
            }
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    /**
     * Authenticate user
     * POST /api/v11/users/authenticate
     * Returns JWT token on successful authentication
     */
    @POST
    @Path("/authenticate")
    public Uni<Response> authenticate(@Valid AuthenticateRequest request) {
        return Uni.createFrom().item(() -> {
            try {
                LOG.infof("Authenticating user: %s", request.username());
                User user = userService.authenticate(request.username(), request.password());

                // Generate JWT token for authenticated user
                String token = jwtService.generateToken(user);

                // Return both user info and JWT token
                AuthenticationResponse authResponse = new AuthenticationResponse(
                    toUserResponse(user),
                    token
                );

                return Response.ok(authResponse).build();
            } catch (ValidationException e) {
                return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(new ErrorResponse(e.getMessage()))
                    .build();
            }
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    /**
     * Convert User entity to UserResponse DTO
     */
    private UserResponse toUserResponse(User user) {
        return new UserResponse(
            user.id,
            user.username,
            user.email,
            user.role.name,
            user.role.id,
            user.status,
            user.createdAt,
            user.updatedAt,
            user.lastLoginAt,
            user.failedLoginAttempts,
            user.isLocked()
        );
    }

    /**
     * Request/Response records
     */
    public record CreateUserRequest(
        String username,
        String email,
        String password,
        String roleName
    ) {}

    public record UpdateUserRequest(
        String email,
        String roleName
    ) {}

    public record UpdateRoleRequest(
        String roleName
    ) {}

    public record UpdateStatusRequest(
        User.UserStatus status
    ) {}

    public record UpdatePasswordRequest(
        String newPassword
    ) {}

    public record AuthenticateRequest(
        String username,
        String password
    ) {}

    public record UserResponse(
        UUID id,
        String username,
        String email,
        String roleName,
        UUID roleId,
        User.UserStatus status,
        java.time.Instant createdAt,
        java.time.Instant updatedAt,
        java.time.Instant lastLoginAt,
        int failedLoginAttempts,
        boolean isLocked
    ) {}

    public record PagedResponse<T>(
        List<T> data,
        int page,
        int size,
        long totalCount
    ) {}

    public record AuthenticationResponse(
        UserResponse user,
        String token
    ) {}

    public record ErrorResponse(String message) {}

    public record SuccessResponse(String message) {}
}
