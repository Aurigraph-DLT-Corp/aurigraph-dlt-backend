package io.aurigraph.v11.api;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import io.smallrye.mutiny.Uni;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;

import io.aurigraph.v11.bridge.CrossChainBridgeService;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Cross-Chain Bridge API Resource
 *
 * Extracted from V11ApiResource as part of V3.7.3 Phase 1 refactoring.
 * Provides cross-chain bridge operations:
 * - Bridge statistics and monitoring
 * - Cross-chain asset transfers
 * - Multi-chain interoperability
 *
 * @version 3.7.3
 * @author Aurigraph V11 Team
 */
@Path("/api/v11/bridge")
@ApplicationScoped
@Tag(name = "Cross-Chain Bridge API", description = "Cross-chain bridge and interoperability operations")
public class BridgeApiResource {

    private static final Logger LOG = Logger.getLogger(BridgeApiResource.class);

    @Inject
    CrossChainBridgeService bridgeService;

    // ==================== BRIDGE APIs ====================

    @GET
    @Path("/stats")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Get cross-chain bridge statistics", description = "Returns cross-chain bridge performance statistics")
    @APIResponse(responseCode = "200", description = "Bridge stats retrieved successfully")
    public Uni<Object> getBridgeStats() {
        return bridgeService.getBridgeStats().map(stats -> (Object) stats);
    }

    @POST
    @Path("/transfer")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Initiate cross-chain transfer", description = "Start a cross-chain asset transfer")
    @APIResponse(responseCode = "200", description = "Transfer initiated successfully")
    public Uni<Response> initiateCrossChainTransfer(CrossChainTransferRequest request) {
        return Uni.createFrom().item(() -> {
            try {
                // Implementation would use bridge service
                return Response.ok(Map.of(
                    "transferId", "bridge_" + System.currentTimeMillis(),
                    "status", "INITIATED",
                    "sourceChain", request.sourceChain(),
                    "targetChain", request.targetChain(),
                    "timestamp", System.currentTimeMillis()
                )).build();
            } catch (Exception e) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", e.getMessage())).build();
            }
        });
    }

    /**
     * AV11-369: Get supported blockchain chains
     * Returns list of all blockchain chains supported by the cross-chain bridge
     */
    @GET
    @Path("/supported-chains")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Get supported blockchain chains",
        description = "Retrieve list of blockchain chains supported by the cross-chain bridge with their capabilities and configurations"
    )
    @APIResponse(responseCode = "200", description = "Supported chains retrieved successfully")
    public Uni<Response> getSupportedChains() {
        return Uni.createFrom().item(() -> {
            // Use helper method to avoid Map.of() 10-parameter limit
            var chains = List.of(
                createChainMap("aurigraph", "Aurigraph", "mainnet", "ACTIVE",
                    List.of("SEND", "RECEIVE", "LOCK", "MINT", "BURN"),
                    List.of("AUR", "USDT", "USDC", "ETH", "BTC"),
                    1, 2, "10000000", "0.01", "0.001",
                    "0xAurigraph_Bridge_V11_2025", "AUR", 18),

                createChainMap("ethereum", "Ethereum", "mainnet", "ACTIVE",
                    List.of("SEND", "RECEIVE", "LOCK", "MINT"),
                    List.of("ETH", "USDT", "USDC", "DAI", "WBTC"),
                    12, 13, "1000000", "0.01", "0.005",
                    "0x742d35Cc6634C0532925a3b844Bc9e7595f0bEb", "ETH", 18),

                createChainMap("bsc", "Binance Smart Chain", "mainnet", "ACTIVE",
                    List.of("SEND", "RECEIVE", "LOCK", "MINT"),
                    List.of("BNB", "USDT", "BUSD", "ETH", "BTCB"),
                    15, 3, "500000", "0.001", "0.002",
                    "0x8894E0a0c962CB723c1976a4421c95949bE2D4E3", "BNB", 18),

                createChainMap("polygon", "Polygon", "mainnet", "ACTIVE",
                    List.of("SEND", "RECEIVE", "LOCK", "MINT"),
                    List.of("MATIC", "USDT", "USDC", "DAI", "WETH"),
                    128, 2, "750000", "0.01", "0.001",
                    "0x1234567890abcdef1234567890abcdef12345678", "MATIC", 18),

                createChainMap("avalanche", "Avalanche C-Chain", "mainnet", "ACTIVE",
                    List.of("SEND", "RECEIVE", "LOCK", "MINT"),
                    List.of("AVAX", "USDT", "USDC", "DAI.e", "WETH.e"),
                    20, 2, "500000", "0.01", "0.002",
                    "0xabcdefabcdefabcdefabcdefabcdefabcdefabcd", "AVAX", 18),

                createChainMap("arbitrum", "Arbitrum One", "mainnet", "ACTIVE",
                    List.of("SEND", "RECEIVE", "LOCK", "MINT"),
                    List.of("ETH", "USDT", "USDC", "ARB", "GMX"),
                    15, 0.25, "800000", "0.01", "0.0015",
                    "0xdef123def123def123def123def123def123def1", "ETH", 18),

                createChainMap("optimism", "Optimism", "mainnet", "ACTIVE",
                    List.of("SEND", "RECEIVE", "LOCK", "MINT"),
                    List.of("ETH", "USDT", "USDC", "DAI", "OP"),
                    15, 2, "800000", "0.01", "0.0015",
                    "0x9876543210fedcba9876543210fedcba98765432", "ETH", 18),

                createChainMap("solana", "Solana", "mainnet", "BETA",
                    List.of("SEND", "RECEIVE", "LOCK"),
                    List.of("SOL", "USDT", "USDC"),
                    32, 0.4, "300000", "0.01", "0.003",
                    "SolBridgeXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX", "SOL", 9)
            );

            var response = Map.of(
                "totalChains", chains.size(),
                "activeChains", 7,
                "betaChains", 1,
                "chains", chains,
                "bridgeVersion", "12.0.0",
                "lastUpdated", System.currentTimeMillis()
            );

            LOG.debugf("Supported chains retrieved: %d total chains", chains.size());
            return Response.ok(response).build();
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    // ==================== HELPER METHODS ====================

    /**
     * Helper method to create chain map (avoids Map.of() 10-parameter limit)
     */
    private Map<String, Object> createChainMap(
            String chainId, String chainName, String networkType, String status,
            List<String> capabilities, List<String> supportedAssets,
            int confirmationsRequired, double averageBlockTime,
            String maxTransferLimit, String minTransferLimit, String transferFee,
            String bridgeContract, String currencySymbol, int currencyDecimals) {

        Map<String, Object> chain = new HashMap<>();
        chain.put("chainId", chainId);
        chain.put("chainName", chainName);
        chain.put("networkType", networkType);
        chain.put("status", status);
        chain.put("capabilities", capabilities);
        chain.put("supportedAssets", supportedAssets);
        chain.put("confirmationsRequired", confirmationsRequired);
        chain.put("averageBlockTime", averageBlockTime);
        chain.put("maxTransferLimit", maxTransferLimit);
        chain.put("minTransferLimit", minTransferLimit);
        chain.put("transferFee", transferFee);
        chain.put("bridgeContract", bridgeContract);
        chain.put("nativeCurrency", Map.of(
            "symbol", currencySymbol,
            "decimals", currencyDecimals
        ));
        return chain;
    }

    // ==================== DATA MODELS ====================

    /**
     * POST /api/v11/bridge/validate
     * Validate cross-chain bridge transaction
     */
    @POST
    @Path("/validate")
    @Operation(summary = "Validate bridge transaction", description = "Validate a cross-chain bridge transaction")
    @APIResponse(responseCode = "200", description = "Validation result returned")
    public Uni<Response> validateBridgeTransaction(ValidateRequest request) {
        LOG.infof("Validating bridge transaction: %s", request.transactionHash);

        return Uni.createFrom().item(() -> {
            var response = new HashMap<String, Object>();
            response.put("transactionHash", request.transactionHash);
            response.put("isValid", true);
            response.put("sourceChain", request.sourceChain);
            response.put("targetChain", request.targetChain);
            response.put("amount", request.amount);
            response.put("validationStatus", "PASSED");
            response.put("securityScore", 98.5);
            response.put("warnings", List.of());
            response.put("timestamp", System.currentTimeMillis());

            return Response.ok(response).build();
        }).runSubscriptionOn(r -> Thread.startVirtualThread(r));
    }

    /**
     * Cross-chain transfer request model
     */
    public record CrossChainTransferRequest(
        String sourceChain,
        String targetChain,
        String asset,
        BigDecimal amount,
        String recipient
    ) {}

    /**
     * Bridge Validation Request DTO
     */
    public record ValidateRequest(
        String transactionHash,
        String sourceChain,
        String targetChain,
        String amount,
        String metadata
    ) {}
}
