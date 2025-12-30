package io.aurigraph.v11.api.phase4;

import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Sprint 39: Integration Marketplace REST API (18 pts)
 *
 * Endpoints for available integrations, installation, and installed integrations.
 * Extracted from Phase4EnterpriseResource for better maintainability.
 *
 * @author Backend Development Agent (BDA)
 * @version 11.0.0
 * @since Sprint 39
 */
@Path("/api/v11/enterprise")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class IntegrationResource {

    private static final Logger LOG = Logger.getLogger(IntegrationResource.class);

    /**
     * Get available integrations
     * GET /api/v11/enterprise/integrations/marketplace
     */
    @GET
    @Path("/integrations/marketplace")
    public Uni<IntegrationsList> getIntegrationsMarketplace(@QueryParam("category") String category) {
        LOG.info("Fetching integrations marketplace");

        return Uni.createFrom().item(() -> {
            IntegrationsList list = new IntegrationsList();
            list.totalIntegrations = 48;
            list.categories = 8;
            list.integrations = new ArrayList<>();

            String[] names = {"Slack", "Microsoft Teams", "PagerDuty", "Datadog", "New Relic", "Splunk", "Jira", "ServiceNow", "Salesforce", "HubSpot"};
            String[] categories = {"COMMUNICATION", "COMMUNICATION", "MONITORING", "MONITORING", "MONITORING", "LOGGING", "PROJECT_MGMT", "ITSM", "CRM", "MARKETING"};
            String[] statuses = {"AVAILABLE", "AVAILABLE", "AVAILABLE", "AVAILABLE", "AVAILABLE", "AVAILABLE", "AVAILABLE", "BETA", "AVAILABLE", "COMING_SOON"};

            for (int i = 0; i < names.length; i++) {
                Integration integration = new Integration();
                integration.integrationId = "int-" + String.format("%03d", i + 1);
                integration.name = names[i];
                integration.category = categories[i];
                integration.description = "Integrate Aurigraph with " + names[i];
                integration.status = statuses[i];
                integration.installs = 100 + (i * 50);
                integration.rating = 4.5 + (i % 3) * 0.1;
                integration.version = "1." + i + ".0";
                integration.requiresAuth = true;
                list.integrations.add(integration);
            }

            return list;
        });
    }

    /**
     * Install integration
     * POST /api/v11/enterprise/integrations/install
     */
    @POST
    @Path("/integrations/install")
    public Uni<Response> installIntegration(IntegrationInstallRequest request) {
        LOG.infof("Installing integration: %s", request.integrationId);

        return Uni.createFrom().item(() -> {
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("installId", "install-" + System.currentTimeMillis());
            result.put("integrationId", request.integrationId);
            result.put("status", "INSTALLING");
            result.put("estimatedTime", "1-2 minutes");
            result.put("message", "Integration installation started");

            return Response.ok(result).build();
        });
    }

    /**
     * Get installed integrations
     * GET /api/v11/enterprise/integrations/installed
     */
    @GET
    @Path("/integrations/installed")
    public Uni<InstalledIntegrationsList> getInstalledIntegrations() {
        LOG.info("Fetching installed integrations");

        return Uni.createFrom().item(() -> {
            InstalledIntegrationsList list = new InstalledIntegrationsList();
            list.totalInstalled = 12;
            list.activeIntegrations = 10;
            list.integrations = new ArrayList<>();

            String[] names = {"Slack", "Datadog", "PagerDuty", "Jira", "New Relic", "Microsoft Teams"};
            boolean[] active = {true, true, true, true, false, true};

            for (int i = 0; i < names.length; i++) {
                InstalledIntegration integration = new InstalledIntegration();
                integration.integrationId = "int-" + String.format("%03d", i + 1);
                integration.name = names[i];
                integration.status = active[i] ? "ACTIVE" : "INACTIVE";
                integration.installedAt = Instant.now().minus(90 - i * 10, ChronoUnit.DAYS).toString();
                integration.lastUsed = active[i] ? Instant.now().minus(i * 2, ChronoUnit.HOURS).toString() : null;
                integration.eventsProcessed = active[i] ? 1000 + (i * 500) : 0;
                integration.version = "1." + i + ".0";
                list.integrations.add(integration);
            }

            return list;
        });
    }

    // ==================== DTOs ====================

    public static class IntegrationsList {
        public int totalIntegrations;
        public int categories;
        public List<Integration> integrations;
    }

    public static class Integration {
        public String integrationId;
        public String name;
        public String category;
        public String description;
        public String status;
        public int installs;
        public double rating;
        public String version;
        public boolean requiresAuth;
    }

    public static class IntegrationInstallRequest {
        public String integrationId;
        public Map<String, String> configuration;
    }

    public static class InstalledIntegrationsList {
        public int totalInstalled;
        public int activeIntegrations;
        public List<InstalledIntegration> integrations;
    }

    public static class InstalledIntegration {
        public String integrationId;
        public String name;
        public String status;
        public String installedAt;
        public String lastUsed;
        public long eventsProcessed;
        public String version;
    }
}
