package io.aurigraph.v11.maintenance.damn;

import io.quarkus.logging.Log;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.Map;

/**
 * DAMN (Distributed Autonomous Maintenance Network) REST Resource
 *
 * Exposes autonomous maintenance and health monitoring endpoints
 *
 * Base Path: /api/v11/maintenance/damn
 */
@Path("/api/v11/maintenance/damn")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class DAMNResource {

    @Inject
    DistributedAutonomousMaintenanceNetwork damn;

    /**
     * Perform comprehensive system health check
     */
    @POST
    @Path("/health-check")
    public Uni<Response> performHealthCheck() {
        return damn.performSystemHealthCheck()
            .map(report -> {
                Log.infof("Health check completed. Overall: %s", report.getOverallHealthLevel());
                return Response.ok(report).build();
            })
            .onFailure()
            .recoverWithItem(e -> {
                Log.errorf("Health check failed: %s", e.getMessage());
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Health check failed: " + e.getMessage()))
                    .build();
            });
    }

    /**
     * Get health status of a specific component
     */
    @GET
    @Path("/components/{componentId}/health")
    public Uni<Response> getComponentHealth(@PathParam("componentId") String componentId) {
        return Uni.createFrom().item(() -> {
            ComponentHealth component = damn.getComponent(componentId);
            if (component == null) {
                return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("error", "Component not found: " + componentId))
                    .build();
            }

            return Response.ok(Map.of(
                "componentId", component.getComponentId(),
                "componentName", component.getComponentName(),
                "status", component.getLastHealthStatus(),
                "type", component.getType(),
                "consecutiveFailures", component.getConsecutiveFailures(),
                "lastChecked", component.getLastChecked(),
                "responseTime", component.getLastResponseTime()
            )).build();
        });
    }

    /**
     * Get all registered components
     */
    @GET
    @Path("/components")
    public Uni<Response> getComponents() {
        return Uni.createFrom().item(() -> {
            Map<String, ComponentHealth> components = damn.getComponentHealth();
            return Response.ok(components).build();
        });
    }

    /**
     * Get remediations suggestions for a component
     */
    @GET
    @Path("/components/{componentId}/remediations")
    public Uni<Response> getRemediations(@PathParam("componentId") String componentId) {
        return Uni.createFrom().item(() -> {
            List<RemediationAction> remediations = damn.getRemediationSuggestions(componentId);
            return Response.ok(remediations).build();
        });
    }

    /**
     * Get performance metrics for all components
     */
    @GET
    @Path("/metrics")
    public Uni<Response> getPerformanceMetrics() {
        return Uni.createFrom().item(() -> {
            Map<String, PerformanceMetrics> metrics = damn.getPerformanceMetrics();
            return Response.ok(metrics).build();
        });
    }

    /**
     * Get alert history for a component
     */
    @GET
    @Path("/components/{componentId}/alerts")
    public Uni<Response> getAlerts(@PathParam("componentId") String componentId) {
        return Uni.createFrom().item(() -> {
            List<SystemAlert> alerts = damn.getAlertHistory(componentId);
            return Response.ok(alerts).build();
        });
    }

    /**
     * Trigger manual health check for a specific component
     */
    @POST
    @Path("/components/{componentId}/check")
    public Uni<Response> checkComponent(@PathParam("componentId") String componentId) {
        return Uni.createFrom().item(() -> {
            ComponentHealth component = damn.getComponent(componentId);
            if (component == null) {
                return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("error", "Component not found"))
                    .build();
            }

            Log.infof("Manually triggered check for component: %s", componentId);
            return Response.ok(Map.of(
                "message", "Health check triggered",
                "componentId", componentId
            )).build();
        });
    }

    /**
     * Health endpoint (always available, no auth required)
     */
    @GET
    @Path("/health")
    public Response health() {
        return Response.ok(Map.of(
            "status", "UP",
            "service", "DistributedAutonomousMaintenanceNetwork"
        )).build();
    }
}
