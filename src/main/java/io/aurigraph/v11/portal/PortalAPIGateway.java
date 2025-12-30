package io.aurigraph.v11.portal;

import io.aurigraph.v11.portal.models.*;
import io.aurigraph.v11.portal.services.*;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.jboss.logging.Logger;

import java.util.List;

/**
 * Aurigraph Portal API Gateway
 * Central routing point for all Portal API requests
 * Routes requests to appropriate backend services and aggregates responses
 *
 * V4.8.0 - Phase 2: Live API Integration
 */
@Path("/api/v11")
@ApplicationScoped
public class PortalAPIGateway {

    private static final Logger LOG = Logger.getLogger(PortalAPIGateway.class);

    // Inject Portal Data Services
    @Inject
    BlockchainDataService blockchainDataService;

    @Inject
    TokenDataService tokenDataService;

    @Inject
    AnalyticsDataService analyticsDataService;

    @Inject
    NetworkDataService networkDataService;

    @Inject
    ContractDataService contractDataService;

    @Inject
    RWADataService rwaDataService;

    @Inject
    StakingDataService stakingDataService;

    // ============================================================
    // CORE API ENDPOINTS
    // ============================================================

    /**
     * GET /api/v11/health
     * Returns service health status
     * Real-time data from HyperRAFT consensus and network health service
     */
    @GET
    @Path("/health")
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<PortalResponse<HealthStatusDTO>> getHealth() {
        LOG.info("Health check requested");

        return blockchainDataService.getHealthStatus()
            .map(health -> PortalResponse.success(health, "Health check successful"))
            .onFailure()
            .recoverWithItem(throwable -> {
                LOG.error("Health check failed", throwable);
                return PortalResponse.error(503, "Service temporarily unavailable");
            });
    }

    /**
     * GET /api/v11/info
     * Returns system information
     */
    @GET
    @Path("/info")
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<PortalResponse<SystemInfoDTO>> getInfo() {
        LOG.info("System info requested");

        return blockchainDataService.getSystemInfo()
            .map(info -> PortalResponse.success(info, "System information retrieved"))
            .onFailure()
            .recoverWithItem(throwable -> {
                LOG.error("Failed to get system info", throwable);
                return PortalResponse.error(500, "Failed to retrieve system information");
            });
    }

    /**
     * GET /api/v11/blockchain/metrics
     * Returns real-time blockchain metrics
     * Live data from consensus service and network stats
     */
    @GET
    @Path("/blockchain/metrics")
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<PortalResponse<BlockchainMetricsDTO>> getBlockchainMetrics() {
        LOG.info("Blockchain metrics requested");

        return blockchainDataService.getBlockchainMetrics()
            .map(metrics -> PortalResponse.success(metrics, "Blockchain metrics retrieved"))
            .onFailure()
            .recoverWithItem(throwable -> {
                LOG.error("Failed to get blockchain metrics", throwable);
                return PortalResponse.error(500, "Failed to retrieve blockchain metrics");
            });
    }

    /**
     * GET /api/v11/blockchain/stats
     * Returns detailed blockchain statistics
     */
    @GET
    @Path("/blockchain/stats")
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<PortalResponse<BlockchainStatsDTO>> getBlockchainStats() {
        LOG.info("Blockchain stats requested");

        return blockchainDataService.getBlockchainStats()
            .map(stats -> PortalResponse.success(stats, "Blockchain statistics retrieved"))
            .onFailure()
            .recoverWithItem(throwable -> {
                LOG.error("Failed to get blockchain stats", throwable);
                return PortalResponse.error(500, "Failed to retrieve blockchain statistics");
            });
    }

    // ============================================================
    // BLOCKCHAIN ENDPOINTS
    // ============================================================

    /**
     * GET /api/v11/blocks
     * Returns list of recent blocks
     * Real data from blockchain state
     */
    @GET
    @Path("/blocks")
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<PortalResponse<List<BlockDTO>>> getBlocks(
            @QueryParam("limit") @DefaultValue("20") int limit) {
        LOG.infof("Blocks requested (limit: %d)", limit);

        return blockchainDataService.getLatestBlocks(Math.min(limit, 100))
            .map(blocks -> PortalResponse.success(blocks, "Blocks retrieved"))
            .onFailure()
            .recoverWithItem(throwable -> {
                LOG.error("Failed to get blocks", throwable);
                return PortalResponse.error(500, "Failed to retrieve blocks");
            });
    }

    /**
     * GET /api/v11/validators
     * Returns list of active validators
     * Real data from LiveValidatorService
     */
    @GET
    @Path("/validators")
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<PortalResponse<List<ValidatorDTO>>> getValidators() {
        LOG.info("Validators requested");

        return blockchainDataService.getValidators()
            .map(validators -> PortalResponse.success(validators, "Validators retrieved"))
            .onFailure()
            .recoverWithItem(throwable -> {
                LOG.error("Failed to get validators", throwable);
                return PortalResponse.error(500, "Failed to retrieve validators");
            });
    }

    /**
     * GET /api/v11/validators/{id}
     * Returns details for specific validator
     */
    @GET
    @Path("/validators/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<PortalResponse<ValidatorDetailDTO>> getValidatorDetails(@PathParam("id") String validatorId) {
        LOG.infof("Validator details requested: %s", validatorId);

        return blockchainDataService.getValidatorDetails(validatorId)
            .map(validator -> PortalResponse.success(validator, "Validator details retrieved"))
            .onFailure()
            .recoverWithItem(throwable -> {
                LOG.error("Failed to get validator details", throwable);
                return PortalResponse.error(500, "Failed to retrieve validator details");
            });
    }

    /**
     * GET /api/v11/transactions
     * Returns list of recent transactions
     */
    @GET
    @Path("/transactions")
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<PortalResponse<List<TransactionDTO>>> getTransactions(
            @QueryParam("limit") @DefaultValue("20") int limit) {
        LOG.infof("Transactions requested (limit: %d)", limit);

        try {
            return blockchainDataService.getTransactions(Math.min(limit, 100))
                .map(transactions -> {
                    LOG.infof("Retrieved %d transactions successfully", transactions.size());
                    return PortalResponse.success(transactions, "Transactions retrieved");
                })
                .onFailure()
                .recoverWithItem(throwable -> {
                    LOG.errorf(throwable, "Failed to get transactions: %s", throwable.getMessage());
                    return PortalResponse.error(500, "Failed to retrieve transactions: " + throwable.getMessage());
                });
        } catch (Exception e) {
            LOG.errorf(e, "Exception in getTransactions: %s", e.getMessage());
            return Uni.createFrom().item(PortalResponse.error(500, "Exception in getTransactions: " + e.getMessage()));
        }
    }

    /**
     * GET /api/v11/transactions/{id}
     * Returns details for specific transaction
     */
    @GET
    @Path("/transactions/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<PortalResponse<TransactionDetailDTO>> getTransactionDetails(@PathParam("id") String transactionId) {
        LOG.infof("Transaction details requested: %s", transactionId);

        return blockchainDataService.getTransactionDetails(transactionId)
            .map(transaction -> PortalResponse.success(transaction, "Transaction details retrieved"))
            .onFailure()
            .recoverWithItem(throwable -> {
                LOG.error("Failed to get transaction details", throwable);
                return PortalResponse.error(500, "Failed to retrieve transaction details");
            });
    }

    // ============================================================
    // TOKEN ENDPOINTS
    // ============================================================

    /**
     * GET /api/v11/tokens
     * Returns list of all tokens
     * Real data from token registry
     */
    @GET
    @Path("/tokens")
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<PortalResponse<List<TokenDTO>>> getTokens() {
        LOG.info("Tokens requested");

        return tokenDataService.getAllTokens()
            .map(tokens -> PortalResponse.success(tokens, "Tokens retrieved"))
            .onFailure()
            .recoverWithItem(throwable -> {
                LOG.error("Failed to get tokens", throwable);
                return PortalResponse.error(500, "Failed to retrieve tokens");
            });
    }

    /**
     * GET /api/v11/tokens/statistics
     * Returns token statistics
     */
    @GET
    @Path("/tokens/statistics")
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<PortalResponse<TokenStatisticsDTO>> getTokenStatistics() {
        LOG.info("Token statistics requested");

        return tokenDataService.getTokenStatistics()
            .map(stats -> PortalResponse.success(stats, "Token statistics retrieved"))
            .onFailure()
            .recoverWithItem(throwable -> {
                LOG.error("Failed to get token statistics", throwable);
                return PortalResponse.error(500, "Failed to retrieve token statistics");
            });
    }

    // ============================================================
    // ANALYTICS ENDPOINTS
    // ============================================================

    /**
     * GET /api/v11/analytics
     * Returns analytics data
     * Real data from analytics aggregation service
     */
    @GET
    @Path("/analytics")
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<PortalResponse<AnalyticsDTO>> getAnalytics() {
        LOG.info("Analytics requested");

        return analyticsDataService.getAnalytics()
            .map(analytics -> PortalResponse.success(analytics, "Analytics data retrieved"))
            .onFailure()
            .recoverWithItem(throwable -> {
                LOG.error("Failed to get analytics", throwable);
                return PortalResponse.error(500, "Failed to retrieve analytics");
            });
    }

    /**
     * GET /api/v11/analytics/performance
     * Returns performance analytics
     */
    @GET
    @Path("/analytics/performance")
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<PortalResponse<PerformanceAnalyticsDTO>> getAnalyticsPerformance() {
        LOG.info("Performance analytics requested");

        return analyticsDataService.getPerformanceAnalytics()
            .map(perf -> PortalResponse.success(perf, "Performance analytics retrieved"))
            .onFailure()
            .recoverWithItem(throwable -> {
                LOG.error("Failed to get performance analytics", throwable);
                return PortalResponse.error(500, "Failed to retrieve performance analytics");
            });
    }

    /**
     * GET /api/v11/ml/metrics
     * Returns ML model metrics
     * Real data from AI optimization service
     */
    @GET
    @Path("/ml/metrics")
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<PortalResponse<MLMetricsDTO>> getMLMetrics() {
        LOG.info("ML metrics requested");

        return analyticsDataService.getMLMetrics()
            .map(metrics -> PortalResponse.success(metrics, "ML metrics retrieved"))
            .onFailure()
            .recoverWithItem(throwable -> {
                LOG.error("Failed to get ML metrics", throwable);
                return PortalResponse.error(500, "Failed to retrieve ML metrics");
            });
    }

    /**
     * GET /api/v11/ml/performance
     * Returns ML performance data
     */
    @GET
    @Path("/ml/performance")
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<PortalResponse<MLPerformanceDTO>> getMLPerformance() {
        LOG.info("ML performance requested");

        return analyticsDataService.getMLPerformance()
            .map(perf -> PortalResponse.success(perf, "ML performance retrieved"))
            .onFailure()
            .recoverWithItem(throwable -> {
                LOG.error("Failed to get ML performance", throwable);
                return PortalResponse.error(500, "Failed to retrieve ML performance");
            });
    }

    /**
     * GET /api/v11/ml/predictions
     * Returns ML predictions
     */
    @GET
    @Path("/ml/predictions")
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<PortalResponse<MLPredictionsDTO>> getMLPredictions() {
        LOG.info("ML predictions requested");

        return analyticsDataService.getMLPredictions()
            .map(predictions -> PortalResponse.success(predictions, "ML predictions retrieved"))
            .onFailure()
            .recoverWithItem(throwable -> {
                LOG.error("Failed to get ML predictions", throwable);
                return PortalResponse.error(500, "Failed to retrieve ML predictions");
            });
    }

    /**
     * GET /api/v11/ml/confidence
     * Returns ML confidence scores
     */
    @GET
    @Path("/ml/confidence")
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<PortalResponse<MLConfidenceDTO>> getMLConfidence() {
        LOG.info("ML confidence requested");

        return analyticsDataService.getMLConfidence()
            .map(confidence -> PortalResponse.success(confidence, "ML confidence retrieved"))
            .onFailure()
            .recoverWithItem(throwable -> {
                LOG.error("Failed to get ML confidence", throwable);
                return PortalResponse.error(500, "Failed to retrieve ML confidence");
            });
    }

    // ============================================================
    // NETWORK ENDPOINTS
    // ============================================================

    /**
     * GET /api/v11/network/health
     * Returns network health status
     */
    @GET
    @Path("/network/health")
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<PortalResponse<NetworkHealthDTO>> getNetworkHealth() {
        LOG.info("Network health requested");

        return networkDataService.getNetworkHealth()
            .map(health -> PortalResponse.success(health, "Network health retrieved"))
            .onFailure()
            .recoverWithItem(throwable -> {
                LOG.error("Failed to get network health", throwable);
                return PortalResponse.error(500, "Failed to retrieve network health");
            });
    }

    /**
     * GET /api/v11/system/config
     * Returns system configuration
     */
    @GET
    @Path("/system/config")
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<PortalResponse<SystemConfigDTO>> getSystemConfig() {
        LOG.info("System config requested");

        return networkDataService.getSystemConfig()
            .map(config -> PortalResponse.success(config, "System configuration retrieved"))
            .onFailure()
            .recoverWithItem(throwable -> {
                LOG.error("Failed to get system config", throwable);
                return PortalResponse.error(500, "Failed to retrieve system configuration");
            });
    }

    /**
     * GET /api/v11/system/status
     * Returns system status
     */
    @GET
    @Path("/system/status")
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<PortalResponse<SystemStatusDTO>> getSystemStatus() {
        LOG.info("System status requested");

        return networkDataService.getSystemStatus()
            .map(status -> PortalResponse.success(status, "System status retrieved"))
            .onFailure()
            .recoverWithItem(throwable -> {
                LOG.error("Failed to get system status", throwable);
                return PortalResponse.error(500, "Failed to retrieve system status");
            });
    }

    /**
     * GET /api/v11/audit-trail
     * Returns audit logs
     */
    @GET
    @Path("/audit-trail")
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<PortalResponse<List<AuditTrailDTO>>> getAuditTrail(
            @QueryParam("limit") @DefaultValue("50") int limit) {
        LOG.infof("Audit trail requested (limit: %d)", limit);

        return networkDataService.getAuditTrail(Math.min(limit, 100))
            .map(logs -> PortalResponse.success(logs, "Audit logs retrieved"))
            .onFailure()
            .recoverWithItem(throwable -> {
                LOG.error("Failed to get audit trail", throwable);
                return PortalResponse.error(500, "Failed to retrieve audit logs");
            });
    }

    // ============================================================
    // RWA & TOKENIZATION ENDPOINTS
    // ============================================================

    /**
     * GET /api/v11/rwa/tokens
     * Returns real-world asset tokens
     */
    @GET
    @Path("/rwa/tokens")
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<PortalResponse<List<RWATokenDTO>>> getRWATokens() {
        LOG.info("RWA tokens requested");

        return rwaDataService.getRWATokens()
            .map(tokens -> PortalResponse.success(tokens, "RWA tokens retrieved"))
            .onFailure()
            .recoverWithItem(throwable -> {
                LOG.error("Failed to get RWA tokens", throwable);
                return PortalResponse.error(500, "Failed to retrieve RWA tokens");
            });
    }

    /**
     * GET /api/v11/rwa/pools
     * Returns RWA pools
     */
    @GET
    @Path("/rwa/pools")
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<PortalResponse<List<RWAPoolDTO>>> getRWAPools() {
        LOG.info("RWA pools requested");

        return rwaDataService.getRWAPools()
            .map(pools -> PortalResponse.success(pools, "RWA pools retrieved"))
            .onFailure()
            .recoverWithItem(throwable -> {
                LOG.error("Failed to get RWA pools", throwable);
                return PortalResponse.error(500, "Failed to retrieve RWA pools");
            });
    }

    /**
     * GET /api/v11/rwa/fractional
     * Returns fractional tokens
     */
    @GET
    @Path("/rwa/fractional")
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<PortalResponse<List<FractionalTokenDTO>>> getFractionalTokens() {
        LOG.info("Fractional tokens requested");

        return rwaDataService.getFractionalTokens()
            .map(tokens -> PortalResponse.success(tokens, "Fractional tokens retrieved"))
            .onFailure()
            .recoverWithItem(throwable -> {
                LOG.error("Failed to get fractional tokens", throwable);
                return PortalResponse.error(500, "Failed to retrieve fractional tokens");
            });
    }

    // ============================================================
    // SMART CONTRACT ENDPOINTS
    // ============================================================

    /**
     * GET /api/v11/contracts/ricardian
     * Returns Ricardian contracts
     */
    @GET
    @Path("/contracts/ricardian")
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<PortalResponse<List<RicardianContractDTO>>> getRicardianContracts() {
        LOG.info("Ricardian contracts requested");

        return contractDataService.getRicardianContracts()
            .map(contracts -> PortalResponse.success(contracts, "Ricardian contracts retrieved"))
            .onFailure()
            .recoverWithItem(throwable -> {
                LOG.error("Failed to get Ricardian contracts", throwable);
                return PortalResponse.error(500, "Failed to retrieve Ricardian contracts");
            });
    }

    /**
     * GET /api/v11/contracts/templates
     * Returns contract templates
     */
    @GET
    @Path("/contracts/templates")
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<PortalResponse<List<ContractTemplateDTO>>> getContractTemplates() {
        LOG.info("Contract templates requested");

        return contractDataService.getContractTemplates()
            .map(templates -> PortalResponse.success(templates, "Contract templates retrieved"))
            .onFailure()
            .recoverWithItem(throwable -> {
                LOG.error("Failed to get contract templates", throwable);
                return PortalResponse.error(500, "Failed to retrieve contract templates");
            });
    }

    /**
     * GET /api/v11/channels
     * Returns channels/subscriptions
     */
    @GET
    @Path("/channels")
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<PortalResponse<List<SmartChannelDTO>>> getChannels() {
        LOG.info("Channels requested");

        return contractDataService.getChannels()
            .map(channels -> PortalResponse.success(channels, "Channels retrieved"))
            .onFailure()
            .recoverWithItem(throwable -> {
                LOG.error("Failed to get channels", throwable);
                return PortalResponse.error(500, "Failed to retrieve channels");
            });
    }

    // ============================================================
    // STAKING ENDPOINTS
    // ============================================================

    /**
     * GET /api/v11/staking/info
     * Returns staking information
     */
    @GET
    @Path("/staking/info")
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<PortalResponse<StakingInfoDTO>> getStakingInfo() {
        LOG.info("Staking info requested");

        return stakingDataService.getStakingInfo()
            .map(info -> PortalResponse.success(info, "Staking information retrieved"))
            .onFailure()
            .recoverWithItem(throwable -> {
                LOG.error("Failed to get staking info", throwable);
                return PortalResponse.error(500, "Failed to retrieve staking information");
            });
    }

    /**
     * GET /api/v11/distribution/pools
     * Returns distribution pools
     */
    @GET
    @Path("/distribution/pools")
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<PortalResponse<List<RewardDistributionDTO>>> getDistributionPools() {
        LOG.info("Distribution pools requested");

        return stakingDataService.getDistributionPools()
            .map(pools -> PortalResponse.success(pools, "Distribution pools retrieved"))
            .onFailure()
            .recoverWithItem(throwable -> {
                LOG.error("Failed to get distribution pools", throwable);
                return PortalResponse.error(500, "Failed to retrieve distribution pools");
            });
    }

    // ============================================================
    // NOTE: Performance benchmark endpoint moved to PerformanceBenchmarkResource
    // for reliability (no CDI dependencies)
    // ============================================================

    // ============================================================
    // NOTE: Catch-all route removed to avoid CDI proxy issues
    // JAX-RS will return 404 for unknown endpoints by default
    // ============================================================
}
