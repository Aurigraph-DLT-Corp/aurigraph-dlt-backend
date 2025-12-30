package io.aurigraph.v11.mobile;

import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * Mobile App REST API
 *
 * Endpoints for mobile app user management and statistics.
 *
 * @version 11.4.0
 * @since 2025-10-13
 */
@Path("/api/v11/mobile")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class MobileAppResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(MobileAppResource.class);

    @Inject
    MobileAppService mobileAppService;

    /**
     * Register new mobile app user
     *
     * POST /api/v11/mobile/register
     */
    @POST
    @Path("/register")
    public Uni<Response> registerUser(MobileAppUser user) {
        LOGGER.info("REST: Register mobile user - {}", user.getEmail());

        return mobileAppService.registerUser(user)
                .map(registered -> Response.ok(registered).build())
                .onFailure().recoverWithItem(error -> {
                    LOGGER.error("Registration failed: {}", error.getMessage());
                    return Response.status(Response.Status.BAD_REQUEST)
                            .entity(Map.of("error", error.getMessage()))
                            .build();
                });
    }

    /**
     * Get user by ID
     *
     * GET /api/v11/mobile/users/{userId}
     */
    @GET
    @Path("/users/{userId}")
    public Uni<Response> getUser(@PathParam("userId") String userId) {
        LOGGER.info("REST: Get mobile user - {}", userId);

        return mobileAppService.getUser(userId)
                .map(user -> Response.ok(user).build())
                .onFailure().recoverWithItem(error ->
                        Response.status(Response.Status.NOT_FOUND)
                                .entity(Map.of("error", error.getMessage()))
                                .build()
                );
    }

    /**
     * List all users
     *
     * GET /api/v11/mobile/users
     */
    @GET
    @Path("/users")
    public Uni<Response> listUsers(@QueryParam("deviceType") String deviceType) {
        LOGGER.info("REST: List mobile users - device: {}", deviceType);

        Uni<List<MobileAppUser>> usersUni;

        if (deviceType != null && !deviceType.isEmpty()) {
            try {
                MobileAppUser.DeviceType type = MobileAppUser.DeviceType.valueOf(deviceType.toUpperCase());
                usersUni = mobileAppService.listUsersByDevice(type);
            } catch (IllegalArgumentException e) {
                return Uni.createFrom().item(Response.status(Response.Status.BAD_REQUEST)
                        .entity(Map.of("error", "Invalid device type"))
                        .build());
            }
        } else {
            usersUni = mobileAppService.listUsers();
        }

        return usersUni.map(users -> Response.ok(users).build());
    }

    /**
     * Update user status
     *
     * PUT /api/v11/mobile/users/{userId}/status
     */
    @PUT
    @Path("/users/{userId}/status")
    public Uni<Response> updateUserStatus(
            @PathParam("userId") String userId,
            Map<String, Boolean> statusRequest
    ) {
        LOGGER.info("REST: Update user status - {}", userId);

        boolean isActive = statusRequest.getOrDefault("isActive", true);

        return mobileAppService.updateUserStatus(userId, isActive)
                .map(user -> Response.ok(user).build())
                .onFailure().recoverWithItem(error ->
                        Response.status(Response.Status.BAD_REQUEST)
                                .entity(Map.of("error", error.getMessage()))
                                .build()
                );
    }

    /**
     * Update user KYC status
     *
     * PUT /api/v11/mobile/users/{userId}/kyc
     */
    @PUT
    @Path("/users/{userId}/kyc")
    public Uni<Response> updateKycStatus(
            @PathParam("userId") String userId,
            Map<String, String> kycRequest
    ) {
        LOGGER.info("REST: Update KYC status - {}", userId);

        String kycStatus = kycRequest.get("kycStatus");
        if (kycStatus == null) {
            return Uni.createFrom().item(Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "kycStatus is required"))
                    .build());
        }

        return mobileAppService.updateKycStatus(userId, kycStatus)
                .map(user -> Response.ok(user).build())
                .onFailure().recoverWithItem(error ->
                        Response.status(Response.Status.BAD_REQUEST)
                                .entity(Map.of("error", error.getMessage()))
                                .build()
                );
    }

    /**
     * Get mobile app statistics
     *
     * GET /api/v11/mobile/stats
     */
    @GET
    @Path("/stats")
    public Response getStatistics() {
        LOGGER.info("REST: Get mobile app statistics");

        Map<String, Object> stats = mobileAppService.getStatistics();
        return Response.ok(stats).build();
    }

    /**
     * Delete user (GDPR)
     *
     * DELETE /api/v11/mobile/users/{userId}
     */
    @DELETE
    @Path("/users/{userId}")
    public Uni<Response> deleteUser(@PathParam("userId") String userId) {
        LOGGER.info("REST: Delete user - {}", userId);

        return mobileAppService.deleteUser(userId)
                .map(v -> Response.ok(Map.of("message", "User deleted successfully")).build())
                .onFailure().recoverWithItem(error ->
                        Response.status(Response.Status.BAD_REQUEST)
                                .entity(Map.of("error", error.getMessage()))
                                .build()
                );
    }

    /**
     * Record user login
     *
     * POST /api/v11/mobile/users/{userId}/login
     */
    @POST
    @Path("/users/{userId}/login")
    public Uni<Response> recordLogin(@PathParam("userId") String userId) {
        LOGGER.info("REST: Record login - {}", userId);

        return mobileAppService.recordLogin(userId)
                .map(user -> Response.ok(user).build())
                .onFailure().recoverWithItem(error ->
                        Response.status(Response.Status.NOT_FOUND)
                                .entity(Map.of("error", error.getMessage()))
                                .build()
                );
    }
}
