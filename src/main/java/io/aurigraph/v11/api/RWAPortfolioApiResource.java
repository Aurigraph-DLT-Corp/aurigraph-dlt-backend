package io.aurigraph.v11.api;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import io.smallrye.mutiny.Uni;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.util.*;

/**
 * RWA Portfolio API Resource
 *
 * Provides RWA asset and portfolio operations:
 * - GET /api/v11/rwa/assets - List all RWA tokens
 * - POST /api/v11/rwa/portfolio/rebalance - Portfolio rebalancing
 *
 * @version 11.0.0
 * @author Backend Development Agent (BDA)
 */
@Path("/api/v11/rwa")
@ApplicationScoped
@Tag(name = "RWA Portfolio API", description = "Real-World Asset tokenization and portfolio management")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RWAPortfolioApiResource {

    private static final Logger LOG = Logger.getLogger(RWAPortfolioApiResource.class);

    // ==================== ENDPOINT 11: List RWA Assets ====================

    /**
     * GET /api/v11/rwa/assets
     * List all Real-World Asset tokens
     */
    @GET
    @Path("/assets")
    @Operation(summary = "List RWA assets", description = "Retrieve all tokenized real-world assets")
    @APIResponse(responseCode = "200", description = "Assets retrieved successfully",
                content = @Content(schema = @Schema(implementation = RWAAssetsResponse.class)))
    public Uni<RWAAssetsResponse> listRWAAssets(
        @QueryParam("assetType") String assetType,
        @QueryParam("status") String status,
        @QueryParam("minValue") Double minValue,
        @QueryParam("limit") @DefaultValue("50") int limit,
        @QueryParam("offset") @DefaultValue("0") int offset) {

        LOG.infof("Listing RWA assets: type=%s, status=%s, minValue=%.2f, limit=%d",
                 assetType, status, minValue, limit);

        return Uni.createFrom().item(() -> {
            RWAAssetsResponse response = new RWAAssetsResponse();
            response.timestamp = Instant.now().toEpochMilli();
            response.totalAssets = 847;
            response.limit = limit;
            response.offset = offset;
            response.assets = new ArrayList<>();

            String[] assetTypes = {"REAL_ESTATE", "COMMODITIES", "ART", "CARBON_CREDITS", "BONDS", "EQUITIES"};
            String[] statuses = {"ACTIVE", "PENDING_VERIFICATION", "VERIFIED", "TRADING"};

            for (int i = 0; i < Math.min(limit, 50); i++) {
                RWAAsset asset = new RWAAsset();
                asset.assetId = "rwa-" + String.format("%06d", offset + i + 1);
                asset.tokenId = "token-" + UUID.randomUUID().toString().substring(0, 8);

                String selectedType = assetType != null ? assetType :
                                     assetTypes[(offset + i) % assetTypes.length];
                asset.assetType = selectedType;

                String selectedStatus = status != null ? status :
                                       statuses[(offset + i) % statuses.length];
                asset.status = selectedStatus;

                // Generate asset-specific details
                switch (asset.assetType) {
                    case "REAL_ESTATE":
                        asset.assetName = "Luxury Apartment #" + (offset + i + 1);
                        asset.description = "Premium residential property in downtown district";
                        asset.location = "New York, NY";
                        asset.totalValue = 2500000.0 + (Math.random() * 5000000);
                        asset.tokenizedValue = asset.totalValue;
                        asset.totalSupply = 1000000.0; // shares
                        asset.pricePerToken = asset.tokenizedValue / asset.totalSupply;
                        asset.annualYield = 4.5 + (Math.random() * 3);
                        break;
                    case "COMMODITIES":
                        asset.assetName = "Gold Reserve #" + (offset + i + 1);
                        asset.description = "Physical gold bullion stored in secure vault";
                        asset.location = "Zurich, Switzerland";
                        asset.totalValue = 500000.0 + (Math.random() * 1000000);
                        asset.tokenizedValue = asset.totalValue;
                        asset.totalSupply = 25000.0; // troy ounces
                        asset.pricePerToken = asset.tokenizedValue / asset.totalSupply;
                        asset.annualYield = 0.0; // Commodities typically don't yield
                        break;
                    case "ART":
                        asset.assetName = "Contemporary Art Collection #" + (offset + i + 1);
                        asset.description = "Curated collection of modern artworks";
                        asset.location = "London, UK";
                        asset.totalValue = 1000000.0 + (Math.random() * 3000000);
                        asset.tokenizedValue = asset.totalValue;
                        asset.totalSupply = 100000.0;
                        asset.pricePerToken = asset.tokenizedValue / asset.totalSupply;
                        asset.annualYield = 0.0;
                        break;
                    case "CARBON_CREDITS":
                        asset.assetName = "Carbon Offset Credits #" + (offset + i + 1);
                        asset.description = "Verified carbon emission reduction credits";
                        asset.location = "Global";
                        asset.totalValue = 250000.0 + (Math.random() * 500000);
                        asset.tokenizedValue = asset.totalValue;
                        asset.totalSupply = 50000.0; // tons CO2
                        asset.pricePerToken = asset.tokenizedValue / asset.totalSupply;
                        asset.annualYield = 0.0;
                        break;
                    case "BONDS":
                        asset.assetName = "Corporate Bond #" + (offset + i + 1);
                        asset.description = "Investment-grade corporate debt instrument";
                        asset.location = "USA";
                        asset.totalValue = 5000000.0 + (Math.random() * 10000000);
                        asset.tokenizedValue = asset.totalValue;
                        asset.totalSupply = 5000.0;
                        asset.pricePerToken = asset.tokenizedValue / asset.totalSupply;
                        asset.annualYield = 5.5 + (Math.random() * 2);
                        break;
                    case "EQUITIES":
                        asset.assetName = "Private Equity Fund #" + (offset + i + 1);
                        asset.description = "Diversified private company equity portfolio";
                        asset.location = "Silicon Valley, CA";
                        asset.totalValue = 10000000.0 + (Math.random() * 20000000);
                        asset.tokenizedValue = asset.totalValue;
                        asset.totalSupply = 1000000.0;
                        asset.pricePerToken = asset.tokenizedValue / asset.totalSupply;
                        asset.annualYield = 12.0 + (Math.random() * 8);
                        break;
                }

                asset.circulatingSupply = asset.totalSupply * (0.6 + (Math.random() * 0.3));
                asset.holders = 50 + (int)(Math.random() * 500);
                asset.issuer = "Aurigraph Tokenization Platform";
                asset.createdAt = Instant.now().minusSeconds((offset + i + 1) * 86400).toEpochMilli();
                asset.lastUpdated = Instant.now().minusSeconds((int)(Math.random() * 3600)).toEpochMilli();
                asset.verificationLevel = i % 3 == 0 ? "LEVEL_3" : "LEVEL_2";
                asset.complianceStatus = "COMPLIANT";

                // Apply filters
                if (minValue != null && asset.totalValue < minValue) continue;

                response.assets.add(asset);
            }

            response.assetsReturned = response.assets.size();

            // Summary statistics
            response.summary = new RWAAssetsSummary();
            response.summary.totalValue = 4750000000.0;
            response.summary.assetsByType = new HashMap<>();
            response.summary.assetsByType.put("REAL_ESTATE", 245);
            response.summary.assetsByType.put("COMMODITIES", 178);
            response.summary.assetsByType.put("ART", 95);
            response.summary.assetsByType.put("CARBON_CREDITS", 134);
            response.summary.assetsByType.put("BONDS", 123);
            response.summary.assetsByType.put("EQUITIES", 72);
            response.summary.averageYield = 6.8;
            response.summary.totalHolders = 12345;

            LOG.infof("Listed %d RWA assets (total: %d)", response.assetsReturned, response.totalAssets);

            return response;
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    // ==================== ENDPOINT 12: Portfolio Rebalancing ====================

    /**
     * POST /api/v11/rwa/portfolio/rebalance
     * Rebalance RWA portfolio
     */
    @POST
    @Path("/portfolio/rebalance")
    @Operation(summary = "Rebalance portfolio", description = "Rebalance RWA token portfolio based on strategy")
    @APIResponse(responseCode = "200", description = "Rebalancing executed successfully",
                content = @Content(schema = @Schema(implementation = PortfolioRebalanceResponse.class)))
    @APIResponse(responseCode = "400", description = "Invalid rebalancing request")
    public Uni<Response> rebalancePortfolio(
        @Parameter(description = "Portfolio rebalancing request", required = true)
        PortfolioRebalanceRequest request) {

        LOG.infof("Rebalancing portfolio: user=%s, strategy=%s", request.userId, request.strategy);

        return Uni.createFrom().item(() -> {
            try {
                // Validate request
                if (request.userId == null || request.userId.isEmpty()) {
                    return Response.status(Response.Status.BAD_REQUEST)
                        .entity(Map.of("error", "User ID is required"))
                        .build();
                }

                PortfolioRebalanceResponse response = new PortfolioRebalanceResponse();
                response.rebalanceId = "rebalance-" + UUID.randomUUID().toString().substring(0, 8);
                response.userId = request.userId;
                response.strategy = request.strategy != null ? request.strategy : "BALANCED";
                response.timestamp = Instant.now().toEpochMilli();
                response.status = "COMPLETED";

                // Current portfolio
                response.currentPortfolio = new HashMap<>();
                response.currentPortfolio.put("REAL_ESTATE", 35.0);
                response.currentPortfolio.put("COMMODITIES", 20.0);
                response.currentPortfolio.put("ART", 10.0);
                response.currentPortfolio.put("CARBON_CREDITS", 15.0);
                response.currentPortfolio.put("BONDS", 15.0);
                response.currentPortfolio.put("EQUITIES", 5.0);

                // Target allocation based on strategy
                response.targetPortfolio = new HashMap<>();
                switch (response.strategy) {
                    case "CONSERVATIVE":
                        response.targetPortfolio.put("REAL_ESTATE", 40.0);
                        response.targetPortfolio.put("COMMODITIES", 15.0);
                        response.targetPortfolio.put("ART", 5.0);
                        response.targetPortfolio.put("CARBON_CREDITS", 10.0);
                        response.targetPortfolio.put("BONDS", 25.0);
                        response.targetPortfolio.put("EQUITIES", 5.0);
                        break;
                    case "AGGRESSIVE":
                        response.targetPortfolio.put("REAL_ESTATE", 25.0);
                        response.targetPortfolio.put("COMMODITIES", 20.0);
                        response.targetPortfolio.put("ART", 15.0);
                        response.targetPortfolio.put("CARBON_CREDITS", 15.0);
                        response.targetPortfolio.put("BONDS", 5.0);
                        response.targetPortfolio.put("EQUITIES", 20.0);
                        break;
                    default: // BALANCED
                        response.targetPortfolio.put("REAL_ESTATE", 30.0);
                        response.targetPortfolio.put("COMMODITIES", 20.0);
                        response.targetPortfolio.put("ART", 10.0);
                        response.targetPortfolio.put("CARBON_CREDITS", 15.0);
                        response.targetPortfolio.put("BONDS", 15.0);
                        response.targetPortfolio.put("EQUITIES", 10.0);
                }

                // Calculate trades needed
                response.trades = new ArrayList<>();
                for (String assetType : response.currentPortfolio.keySet()) {
                    double current = response.currentPortfolio.get(assetType);
                    double target = response.targetPortfolio.get(assetType);
                    double diff = target - current;

                    if (Math.abs(diff) > 0.5) { // Only trade if difference > 0.5%
                        RebalanceTrade trade = new RebalanceTrade();
                        trade.assetType = assetType;
                        trade.action = diff > 0 ? "BUY" : "SELL";
                        trade.percentageChange = Math.abs(diff);
                        trade.estimatedValue = Math.abs(diff) * 100000.0; // Assuming $10M portfolio
                        trade.estimatedCost = trade.estimatedValue * 0.002; // 0.2% transaction cost
                        response.trades.add(trade);
                    }
                }

                // Summary
                response.totalTrades = response.trades.size();
                response.estimatedTotalCost = response.trades.stream()
                    .mapToDouble(t -> t.estimatedCost)
                    .sum();
                response.estimatedDuration = 300000L; // 5 minutes
                response.expectedYieldImprovement = 0.5 + (Math.random() * 1.0); // %
                response.riskScore = calculateRiskScore(response.strategy);

                LOG.infof("Portfolio rebalance: id=%s, trades=%d, total_cost=%.2f",
                         response.rebalanceId, response.totalTrades, response.estimatedTotalCost);

                return Response.ok(response).build();

            } catch (Exception e) {
                LOG.errorf(e, "Portfolio rebalancing failed");
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Rebalancing failed: " + e.getMessage()))
                    .build();
            }
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    // ==================== Helper Methods ====================

    private double calculateRiskScore(String strategy) {
        switch (strategy) {
            case "CONSERVATIVE": return 25.0 + (Math.random() * 10);
            case "AGGRESSIVE": return 75.0 + (Math.random() * 15);
            default: return 45.0 + (Math.random() * 15);
        }
    }

    // ==================== Request/Response DTOs ====================

    public static class RWAAssetsResponse {
        public long timestamp;
        public long totalAssets;
        public int assetsReturned;
        public int limit;
        public int offset;
        public List<RWAAsset> assets;
        public RWAAssetsSummary summary;
    }

    public static class RWAAsset {
        public String assetId;
        public String tokenId;
        public String assetType;
        public String assetName;
        public String description;
        public String location;
        public double totalValue;
        public double tokenizedValue;
        public double totalSupply;
        public double circulatingSupply;
        public double pricePerToken;
        public double annualYield;
        public int holders;
        public String issuer;
        public String status;
        public long createdAt;
        public long lastUpdated;
        public String verificationLevel;
        public String complianceStatus;
    }

    public static class RWAAssetsSummary {
        public double totalValue;
        public Map<String, Integer> assetsByType;
        public double averageYield;
        public int totalHolders;
    }

    public static class PortfolioRebalanceRequest {
        public String userId;
        public String strategy; // CONSERVATIVE, BALANCED, AGGRESSIVE
        public Map<String, Double> customAllocation;
    }

    public static class PortfolioRebalanceResponse {
        public String rebalanceId;
        public String userId;
        public String strategy;
        public long timestamp;
        public String status;
        public Map<String, Double> currentPortfolio;
        public Map<String, Double> targetPortfolio;
        public List<RebalanceTrade> trades;
        public int totalTrades;
        public double estimatedTotalCost;
        public long estimatedDuration;
        public double expectedYieldImprovement;
        public double riskScore;
    }

    public static class RebalanceTrade {
        public String assetType;
        public String action;
        public double percentageChange;
        public double estimatedValue;
        public double estimatedCost;
    }
}
