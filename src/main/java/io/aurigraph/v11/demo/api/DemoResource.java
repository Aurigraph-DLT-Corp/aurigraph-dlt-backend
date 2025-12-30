package io.aurigraph.v11.demo.api;

import io.aurigraph.v11.demo.model.Demo;
import io.quarkus.scheduler.Scheduled;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Demo Management REST API
 * Provides CRUD operations and timeout management for demos
 */
@Path("/api/v11/demos")
@Tag(name = "Demo Management", description = "Manage live demos with persistence and timeout")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class DemoResource {

    private static final Logger LOG = Logger.getLogger(DemoResource.class);
    private static final int DEFAULT_DURATION_MINUTES = 10;
    private static final int MAX_ADMIN_DURATION_MINUTES = 1440; // 24 hours

    @GET
    @Operation(summary = "Get all demos", description = "Returns all demos ordered by creation date")
    public List<Demo> getAllDemos() {
        LOG.info("Fetching all demos");
        return Demo.listAll();
    }

    @GET
    @Path("/active")
    @Operation(summary = "Get active demos", description = "Returns non-expired demos")
    public List<Demo> getActiveDemos() {
        LOG.info("Fetching active demos");
        return Demo.findAllActive();
    }

    @GET
    @Path("/{id}")
    @Operation(summary = "Get demo by ID", description = "Returns a specific demo")
    public Response getDemo(@PathParam("id") String id) {
        LOG.infof("Fetching demo: %s", id);
        Demo demo = Demo.findById(id);
        if (demo == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorResponse("Demo not found: " + id))
                    .build();
        }

        // Check if expired and update status
        if (demo.isExpired() && demo.status != Demo.DemoStatus.EXPIRED) {
            expireDemo(demo);
        }

        return Response.ok(demo).build();
    }

    @POST
    @Transactional
    @Operation(summary = "Create demo", description = "Register a new demo with optional custom duration")
    public Response createDemo(
            @Valid @NotNull DemoRequest request,
            @QueryParam("durationMinutes") Integer durationMinutes,
            @QueryParam("isAdmin") @DefaultValue("false") boolean isAdmin
    ) {
        LOG.infof("Creating demo: %s", request.demoName);

        // Generate unique ID
        String id = "demo_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 9);

        // Validate and set duration
        int finalDuration = durationMinutes != null ? durationMinutes : DEFAULT_DURATION_MINUTES;
        if (!isAdmin && finalDuration > DEFAULT_DURATION_MINUTES) {
            finalDuration = DEFAULT_DURATION_MINUTES;
        }
        if (isAdmin && finalDuration > MAX_ADMIN_DURATION_MINUTES) {
            finalDuration = MAX_ADMIN_DURATION_MINUTES;
        }

        // Create demo
        Demo demo = new Demo();
        demo.id = id;
        demo.demoName = request.demoName;
        demo.userEmail = request.userEmail;
        demo.userName = request.userName;
        demo.description = request.description;
        demo.status = Demo.DemoStatus.PENDING;
        demo.createdAt = LocalDateTime.now();
        demo.lastActivity = LocalDateTime.now();
        demo.durationMinutes = finalDuration;
        demo.expiresAt = LocalDateTime.now().plusMinutes(finalDuration);
        demo.isAdminDemo = isAdmin;
        demo.channelsJson = request.channelsJson;
        demo.validatorsJson = request.validatorsJson;
        demo.businessNodesJson = request.businessNodesJson;
        demo.slimNodesJson = request.slimNodesJson;
        demo.merkleRoot = request.merkleRoot != null ? request.merkleRoot : "";
        demo.transactionCount = 0;

        demo.persist();

        LOG.infof("✅ Demo created: %s (ID: %s, Duration: %d min, Expires: %s)",
                demo.demoName, demo.id, finalDuration, demo.expiresAt);

        return Response.status(Response.Status.CREATED).entity(demo).build();
    }

    @PUT
    @Path("/{id}")
    @Transactional
    @Operation(summary = "Update demo", description = "Update demo properties")
    public Response updateDemo(@PathParam("id") String id, @Valid @NotNull DemoUpdateRequest request) {
        LOG.infof("Updating demo: %s", id);
        Demo demo = Demo.findById(id);
        if (demo == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorResponse("Demo not found: " + id))
                    .build();
        }

        if (request.status != null) {
            demo.status = Demo.DemoStatus.valueOf(request.status);
        }
        if (request.transactionCount != null) {
            demo.transactionCount = request.transactionCount;
        }
        if (request.merkleRoot != null) {
            demo.merkleRoot = request.merkleRoot;
        }

        demo.lastActivity = LocalDateTime.now();
        demo.persist();

        LOG.infof("▶️ Demo updated: %s", demo.demoName);
        return Response.ok(demo).build();
    }

    @POST
    @Path("/{id}/start")
    @Transactional
    @Operation(summary = "Start demo", description = "Change demo status to RUNNING")
    public Response startDemo(@PathParam("id") String id) {
        LOG.infof("Starting demo: %s", id);
        Demo demo = Demo.findById(id);
        if (demo == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorResponse("Demo not found: " + id))
                    .build();
        }

        demo.status = Demo.DemoStatus.RUNNING;
        demo.lastActivity = LocalDateTime.now();
        demo.persist();

        LOG.infof("▶️ Demo started: %s", demo.demoName);
        return Response.ok(demo).build();
    }

    @POST
    @Path("/{id}/stop")
    @Transactional
    @Operation(summary = "Stop demo", description = "Change demo status to STOPPED")
    public Response stopDemo(@PathParam("id") String id) {
        LOG.infof("Stopping demo: %s", id);
        Demo demo = Demo.findById(id);
        if (demo == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorResponse("Demo not found: " + id))
                    .build();
        }

        demo.status = Demo.DemoStatus.STOPPED;
        demo.lastActivity = LocalDateTime.now();
        demo.persist();

        LOG.infof("⏸️ Demo stopped: %s", demo.demoName);
        return Response.ok(demo).build();
    }

    @POST
    @Path("/{id}/extend")
    @Transactional
    @Operation(summary = "Extend demo duration", description = "Add time to demo expiration (admin only)")
    public Response extendDemo(
            @PathParam("id") String id,
            @QueryParam("minutes") int additionalMinutes,
            @QueryParam("isAdmin") @DefaultValue("false") boolean isAdmin
    ) {
        LOG.infof("Extending demo: %s by %d minutes", id, additionalMinutes);

        if (!isAdmin) {
            return Response.status(Response.Status.FORBIDDEN)
                    .entity(new ErrorResponse("Only admins can extend demo duration"))
                    .build();
        }

        Demo demo = Demo.findById(id);
        if (demo == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorResponse("Demo not found: " + id))
                    .build();
        }

        demo.extend(additionalMinutes);
        demo.persist();

        LOG.infof("⏱️ Demo extended: %s - now expires at %s", demo.demoName, demo.expiresAt);
        return Response.ok(demo).build();
    }

    @POST
    @Path("/{id}/transactions")
    @Transactional
    @Operation(summary = "Add transactions", description = "Increment transaction count and update Merkle root")
    public Response addTransactions(
            @PathParam("id") String id,
            @QueryParam("count") @DefaultValue("1") long count,
            @QueryParam("merkleRoot") String merkleRoot
    ) {
        Demo demo = Demo.findById(id);
        if (demo == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorResponse("Demo not found: " + id))
                    .build();
        }

        demo.addTransactions(count);
        if (merkleRoot != null) {
            demo.merkleRoot = merkleRoot;
        }
        demo.persist();

        return Response.ok(demo).build();
    }

    @DELETE
    @Path("/{id}")
    @Transactional
    @Operation(summary = "Delete demo", description = "Remove demo from database")
    public Response deleteDemo(@PathParam("id") String id) {
        LOG.infof("Deleting demo: %s", id);
        Demo demo = Demo.findById(id);
        if (demo == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorResponse("Demo not found: " + id))
                    .build();
        }

        String demoName = demo.demoName;
        demo.delete();

        LOG.infof("🗑️ Demo deleted: %s", demoName);
        return Response.noContent().build();
    }

    /**
     * Auto-expire demos every minute
     */
    @Scheduled(every = "60s")
    @Transactional
    void checkExpiredDemos() {
        List<Demo> expiredDemos = Demo.findExpired();
        if (!expiredDemos.isEmpty()) {
            LOG.infof("⏰ Found %d expired demos, marking as EXPIRED", expiredDemos.size());
            for (Demo demo : expiredDemos) {
                expireDemo(demo);
            }
        }
    }

    /**
     * Auto-generate transactions for RUNNING demos every 5 seconds
     */
    @Scheduled(every = "5s")
    @Transactional
    void autoGenerateTransactions() {
        List<Demo> runningDemos = Demo.list("status = ?1 AND expiresAt > ?2",
            Demo.DemoStatus.RUNNING,
            java.time.LocalDateTime.now());

        if (!runningDemos.isEmpty()) {
            for (Demo demo : runningDemos) {
                // Generate 1-5 random transactions per demo
                int txCount = (int) (Math.random() * 5) + 1;
                demo.addTransactions(txCount);
                demo.persist();
            }
            LOG.debugf("📊 Auto-generated transactions for %d running demos", runningDemos.size());
        }
    }

    private void expireDemo(Demo demo) {
        demo.expire();
        demo.persist();
        LOG.infof("⏰ Demo expired: %s (ID: %s)", demo.demoName, demo.id);
    }

    // Request/Response DTOs
    public static class DemoRequest {
        public String demoName;
        public String userEmail;
        public String userName;
        public String description;
        public String channelsJson;
        public String validatorsJson;
        public String businessNodesJson;
        public String slimNodesJson;
        public String merkleRoot;
    }

    public static class DemoUpdateRequest {
        public String status;
        public Long transactionCount;
        public String merkleRoot;
    }

    public static class ErrorResponse {
        public String error;
        public ErrorResponse(String error) {
            this.error = error;
        }
    }
}
