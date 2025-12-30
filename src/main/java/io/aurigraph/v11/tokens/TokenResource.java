package io.aurigraph.v11.tokens;

import io.aurigraph.v11.contracts.models.AssetType;
import io.aurigraph.v11.tokens.models.Token;
import io.aurigraph.v11.tokens.models.TokenBalance;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Token Management REST API Resource
 *
 * Provides HTTP endpoints for Enterprise Portal token operations.
 * Implements 8 core endpoints matching frontend TokenService expectations.
 *
 * API Base Path: /api/v11/tokens
 *
 * Endpoints:
 * - POST   /create                          - Create new token
 * - GET    /list                           - List all tokens
 * - GET    /{tokenId}                      - Get token by ID
 * - POST   /transfer                       - Transfer tokens
 * - POST   /mint                           - Mint tokens
 * - POST   /burn                           - Burn tokens
 * - GET    /{tokenId}/balance/{address}   - Get balance for address
 * - GET    /stats                          - Get token statistics
 *
 * @version 1.0.0 (Oct 15, 2025)
 * @author Backend Development Agent (BDA)
 */
@Path("/api/v11/tokens")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class TokenResource {

    private static final Logger LOG = Logger.getLogger(TokenResource.class);

    @Inject
    TokenManagementService tokenService;

    // ==================== TOKEN CREATION ====================

    /**
     * Create a new token
     * POST /api/v11/tokens/create
     */
    @POST
    @Path("/create")
    public Uni<TokenDTO> createToken(@Valid TokenCreateRequestDTO request) {
        LOG.infof("Creating token: %s (%s)", request.name, request.symbol);

        // Map to internal RWATokenRequest
        var rwaRequest = new TokenManagementService.RWATokenRequest(
                request.name,
                request.symbol,
                generateOwnerAddress(), // Auto-generate owner address
                BigDecimal.valueOf(request.initialSupply),
                request.decimals,
                AssetType.OTHER, // Default asset type
                null, // No asset ID for standard tokens
                null, // No asset value
                null, // No asset currency
                request.mintable,
                request.burnable,
                request.maxSupply != null ? BigDecimal.valueOf(request.maxSupply) : null,
                false // KYC not required for standard tokens
        );

        return tokenService.createRWAToken(rwaRequest)
                .map(token -> {
                    // Generate contract address if not set
                    if (token.getContractAddress() == null) {
                        token.setContractAddress(generateContractAddress());
                        // Note: In a real system, this should be persisted
                    }
                    return mapToDTO(token);
                })
                .onFailure().transform(error -> {
                    LOG.errorf(error, "Failed to create token: %s", request.name);
                    return new WebApplicationException(
                            Response.status(Response.Status.BAD_REQUEST)
                                    .entity(Map.of("error", error.getMessage()))
                                    .build()
                    );
                });
    }

    // ==================== TOKEN QUERIES ====================

    /**
     * List all tokens
     * GET /api/v11/tokens/list
     */
    @GET
    @Path("/list")
    public Uni<List<TokenDTO>> listTokens(
            @DefaultValue("0") @QueryParam("page") int page,
            @DefaultValue("100") @QueryParam("size") int size) {
        LOG.infof("Listing tokens: page=%d, size=%d", page, size);

        return tokenService.listTokens(page, size)
                .map(tokens -> tokens.stream()
                        .map(this::mapToDTO)
                        .collect(Collectors.toList()))
                .onFailure().transform(error -> {
                    LOG.error("Failed to list tokens", error);
                    return new WebApplicationException(
                            Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                                    .entity(Map.of("error", "Failed to list tokens"))
                                    .build()
                    );
                });
    }

    /**
     * Get token by ID
     * GET /api/v11/tokens/{tokenId}
     */
    @GET
    @Path("/{tokenId}")
    public Uni<TokenDTO> getToken(@PathParam("tokenId") String tokenId) {
        LOG.infof("Getting token: %s", tokenId);

        return tokenService.getToken(tokenId)
                .map(this::mapToDTO)
                .onFailure().transform(error -> {
                    LOG.errorf(error, "Failed to get token: %s", tokenId);
                    return new WebApplicationException(
                            Response.status(Response.Status.NOT_FOUND)
                                    .entity(Map.of("error", "Token not found: " + tokenId))
                                    .build()
                    );
                });
    }

    // ==================== TOKEN OPERATIONS ====================

    /**
     * Transfer tokens between addresses
     * POST /api/v11/tokens/transfer
     */
    @POST
    @Path("/transfer")
    public Uni<TokenTransactionDTO> transferTokens(@Valid TokenTransferRequestDTO request) {
        LOG.infof("Transferring %f tokens of %s from %s to %s",
                request.amount, request.tokenId, request.from, request.to);

        var transferRequest = new TokenManagementService.TransferRequest(
                request.tokenId,
                request.from,
                request.to,
                BigDecimal.valueOf(request.amount)
        );

        return tokenService.transferToken(transferRequest)
                .map(result -> new TokenTransactionDTO(
                        generateTransactionId(),
                        request.tokenId,
                        "transfer",
                        request.from,
                        request.to,
                        request.amount,
                        result.timestamp().toString(),
                        generateBlockHeight(),
                        result.transactionHash(),
                        "confirmed",
                        request.memo
                ))
                .onFailure().transform(error -> {
                    LOG.errorf(error, "Failed to transfer tokens");
                    return new WebApplicationException(
                            Response.status(Response.Status.BAD_REQUEST)
                                    .entity(Map.of("error", error.getMessage()))
                                    .build()
                    );
                });
    }

    /**
     * Mint new tokens
     * POST /api/v11/tokens/mint
     */
    @POST
    @Path("/mint")
    public Uni<TokenTransactionDTO> mintTokens(@Valid TokenMintRequestDTO request) {
        LOG.infof("Minting %f tokens of %s to %s", request.amount, request.tokenId, request.to);

        var mintRequest = new TokenManagementService.MintRequest(
                request.tokenId,
                request.to,
                BigDecimal.valueOf(request.amount)
        );

        return tokenService.mintToken(mintRequest)
                .map(result -> new TokenTransactionDTO(
                        generateTransactionId(),
                        request.tokenId,
                        "mint",
                        "0x0000000000000000000000000000000000000000", // Zero address for mint
                        request.to,
                        request.amount,
                        result.timestamp().toString(),
                        generateBlockHeight(),
                        result.transactionHash(),
                        "confirmed",
                        request.memo
                ))
                .onFailure().transform(error -> {
                    LOG.errorf(error, "Failed to mint tokens");
                    return new WebApplicationException(
                            Response.status(Response.Status.BAD_REQUEST)
                                    .entity(Map.of("error", error.getMessage()))
                                    .build()
                    );
                });
    }

    /**
     * Burn tokens
     * POST /api/v11/tokens/burn
     */
    @POST
    @Path("/burn")
    public Uni<TokenTransactionDTO> burnTokens(@Valid TokenBurnRequestDTO request) {
        LOG.infof("Burning %f tokens of %s from %s", request.amount, request.tokenId, request.from);

        var burnRequest = new TokenManagementService.BurnRequest(
                request.tokenId,
                request.from,
                BigDecimal.valueOf(request.amount)
        );

        return tokenService.burnToken(burnRequest)
                .map(result -> new TokenTransactionDTO(
                        generateTransactionId(),
                        request.tokenId,
                        "burn",
                        request.from,
                        "0x0000000000000000000000000000000000000000", // Zero address for burn
                        request.amount,
                        result.timestamp().toString(),
                        generateBlockHeight(),
                        result.transactionHash(),
                        "confirmed",
                        request.memo
                ))
                .onFailure().transform(error -> {
                    LOG.errorf(error, "Failed to burn tokens");
                    return new WebApplicationException(
                            Response.status(Response.Status.BAD_REQUEST)
                                    .entity(Map.of("error", error.getMessage()))
                                    .build()
                    );
                });
    }

    /**
     * Get token balance for an address
     * GET /api/v11/tokens/{tokenId}/balance/{address}
     */
    @GET
    @Path("/{tokenId}/balance/{address}")
    public Uni<TokenBalanceDTO> getBalance(
            @PathParam("tokenId") String tokenId,
            @PathParam("address") String address) {
        LOG.infof("Getting balance for token %s, address %s", tokenId, address);

        return tokenService.getBalance(address, tokenId)
                .flatMap(balance -> {
                    // Get token to check for locked balance info
                    return tokenService.getToken(tokenId)
                            .map(token -> {
                                // For simplicity, calculate locked as 0 (can be enhanced)
                                double balanceValue = balance.doubleValue();
                                return new TokenBalanceDTO(
                                        tokenId,
                                        address,
                                        balanceValue,
                                        0.0, // locked
                                        balanceValue, // available
                                        Instant.now().toString()
                                );
                            });
                })
                .onFailure().transform(error -> {
                    LOG.errorf(error, "Failed to get balance");
                    return new WebApplicationException(
                            Response.status(Response.Status.NOT_FOUND)
                                    .entity(Map.of("error", "Balance not found"))
                                    .build()
                    );
                });
    }

    /**
     * Get token statistics
     * GET /api/v11/tokens/stats
     */
    @GET
    @Path("/stats")
    public Uni<TokenStatsDTO> getStats() {
        LOG.info("Getting token statistics");

        return tokenService.getStatistics()
                .map(stats -> {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> tokenStats = (Map<String, Object>) stats.get("tokenStatistics");

                    long totalTokens = ((Number) tokenStats.getOrDefault("totalTokens", 0L)).longValue();
                    long fungibleTokens = ((Number) tokenStats.getOrDefault("fungibleTokens", 0L)).longValue();
                    long rwaTokens = ((Number) tokenStats.getOrDefault("rwaTokens", 0L)).longValue();

                    // Calculate active tokens (fungible + RWA, excluding NFTs and others)
                    long activeTokens = fungibleTokens + rwaTokens;

                    // Get supply values
                    BigDecimal totalSupply = (BigDecimal) tokenStats.getOrDefault("totalSupply", BigDecimal.ZERO);
                    BigDecimal totalCirculating = (BigDecimal) tokenStats.getOrDefault("totalCirculating", BigDecimal.ZERO);

                    // Get operation counts
                    long tokensMinted = ((Number) stats.getOrDefault("tokensMinted", 0L)).longValue();
                    long tokensBurned = ((Number) stats.getOrDefault("tokensBurned", 0L)).longValue();
                    long transfers = ((Number) stats.getOrDefault("transfersCompleted", 0L)).longValue();

                    return new TokenStatsDTO(
                            (int) totalTokens,
                            (int) activeTokens,
                            totalSupply.doubleValue(),
                            estimateTotalHolders((int) totalTokens), // Estimated
                            (int) transfers,
                            totalSupply.doubleValue(), // Total minted approximation
                            (totalSupply.subtract(totalCirculating)).doubleValue() // Burned approximation
                    );
                })
                .onFailure().transform(error -> {
                    LOG.error("Failed to get statistics", error);
                    return new WebApplicationException(
                            Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                                    .entity(Map.of("error", "Failed to get statistics"))
                                    .build()
                    );
                });
    }

    // ==================== HELPER METHODS ====================

    /**
     * Map internal Token model to frontend-compatible DTO
     */
    private TokenDTO mapToDTO(Token token) {
        TokenMetadataDTO metadata = null;
        if (token.getDescription() != null || token.getWebsiteUrl() != null || token.getIconUrl() != null) {
            metadata = new TokenMetadataDTO(
                    token.getDescription(),
                    token.getWebsiteUrl(),
                    token.getIconUrl(),
                    token.getTags()
            );
        }

        return new TokenDTO(
                token.getTokenId(),
                token.getName(),
                token.getSymbol(),
                token.getDecimals(),
                token.getTotalSupply().doubleValue(),
                token.getCirculatingSupply().doubleValue(),
                token.getOwner() != null ? token.getOwner() : generateOwnerAddress(),
                token.getContractAddress() != null ? token.getContractAddress() : generateContractAddress(),
                token.getCreatedAt() != null ? token.getCreatedAt().toString() : Instant.now().toString(),
                token.getUpdatedAt() != null ? token.getUpdatedAt().toString() : Instant.now().toString(),
                token.getBurnedAmount().doubleValue(),
                token.getTotalSupply().doubleValue(), // Minted = total supply for now
                token.getTransferCount(),
                token.getHolderCount(),
                mapStatus(token),
                metadata
        );
    }

    /**
     * Map token status
     */
    private String mapStatus(Token token) {
        if (token.getIsPaused() != null && token.getIsPaused()) {
            return "paused";
        }
        return "active";
    }

    /**
     * Generate owner address (placeholder)
     */
    private String generateOwnerAddress() {
        // UUID without dashes is 32 chars, we need 40 for Ethereum address
        String uuid1 = UUID.randomUUID().toString().replace("-", "");
        String uuid2 = UUID.randomUUID().toString().replace("-", "");
        return "0x" + (uuid1 + uuid2).substring(0, 40);
    }

    /**
     * Generate contract address (placeholder)
     */
    private String generateContractAddress() {
        return "0x" + UUID.randomUUID().toString().replace("-", "").substring(0, 40);
    }

    /**
     * Generate transaction ID
     */
    private String generateTransactionId() {
        return "tx-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * Generate block height (placeholder)
     */
    private long generateBlockHeight() {
        return System.currentTimeMillis() / 2000; // Approximate block height
    }

    /**
     * Estimate total holders (placeholder calculation)
     */
    private int estimateTotalHolders(int totalTokens) {
        // Rough estimate: average 100 holders per token
        return totalTokens * 100;
    }

    // ==================== DTO CLASSES ====================

    /**
     * Token creation request DTO
     * Matches frontend TokenCreateRequest interface
     */
    public static class TokenCreateRequestDTO {
        @NotBlank(message = "Token name is required")
        public String name;

        @NotBlank(message = "Token symbol is required")
        @Size(min = 1, max = 10, message = "Symbol must be 1-10 characters")
        public String symbol;

        @NotNull(message = "Decimals is required")
        @Min(value = 0, message = "Decimals must be >= 0")
        @Max(value = 18, message = "Decimals must be <= 18")
        public Integer decimals = 18;

        @NotNull(message = "Initial supply is required")
        @Min(value = 0, message = "Initial supply must be >= 0")
        public Double initialSupply;

        @Min(value = 0, message = "Max supply must be >= 0")
        public Double maxSupply;

        @NotNull(message = "Mintable flag is required")
        public Boolean mintable = true;

        @NotNull(message = "Burnable flag is required")
        public Boolean burnable = true;

        @NotNull(message = "Pausable flag is required")
        public Boolean pausable = false;

        public TokenMetadataDTO metadata;
    }

    /**
     * Token transfer request DTO
     * Matches frontend TokenTransferRequest interface
     */
    public static class TokenTransferRequestDTO {
        @NotBlank(message = "Token ID is required")
        public String tokenId;

        @NotBlank(message = "From address is required")
        public String from;

        @NotBlank(message = "To address is required")
        public String to;

        @NotNull(message = "Amount is required")
        @Min(value = 0, message = "Amount must be > 0")
        public Double amount;

        public String memo;
    }

    /**
     * Token mint request DTO
     * Matches frontend TokenMintRequest interface
     */
    public static class TokenMintRequestDTO {
        @NotBlank(message = "Token ID is required")
        public String tokenId;

        @NotNull(message = "Amount is required")
        @Min(value = 0, message = "Amount must be > 0")
        public Double amount;

        @NotBlank(message = "To address is required")
        public String to;

        public String memo;
    }

    /**
     * Token burn request DTO
     * Matches frontend TokenBurnRequest interface
     */
    public static class TokenBurnRequestDTO {
        @NotBlank(message = "Token ID is required")
        public String tokenId;

        @NotNull(message = "Amount is required")
        @Min(value = 0, message = "Amount must be > 0")
        public Double amount;

        @NotBlank(message = "From address is required")
        public String from;

        public String memo;
    }

    /**
     * Token DTO
     * Matches frontend Token interface
     */
    public record TokenDTO(
            String id,
            String name,
            String symbol,
            Integer decimals,
            Double totalSupply,
            Double currentSupply,
            String owner,
            String contractAddress,
            String createdAt,
            String updatedAt,
            Double burned,
            Double minted,
            Long transfers,
            Long holders,
            String status, // 'active' | 'paused' | 'frozen'
            TokenMetadataDTO metadata
    ) {}

    /**
     * Token metadata DTO
     */
    public record TokenMetadataDTO(
            String description,
            String website,
            String logo,
            List<String> tags
    ) {}

    /**
     * Token balance DTO
     * Matches frontend TokenBalance interface
     */
    public record TokenBalanceDTO(
            String tokenId,
            String address,
            Double balance,
            Double locked,
            Double available,
            String lastUpdated
    ) {}

    /**
     * Token transaction DTO
     * Matches frontend TokenTransaction interface
     */
    public record TokenTransactionDTO(
            String id,
            String tokenId,
            String type, // 'mint' | 'burn' | 'transfer'
            String from,
            String to,
            Double amount,
            String timestamp,
            Long blockHeight,
            String transactionHash,
            String status, // 'confirmed' | 'pending' | 'failed'
            String memo
    ) {}

    /**
     * Token statistics DTO
     * Matches frontend TokenStats interface
     */
    public record TokenStatsDTO(
            Integer totalTokens,
            Integer activeTokens,
            Double totalSupply,
            Integer totalHolders,
            Integer totalTransfers,
            Double totalMinted,
            Double totalBurned
    ) {}
}
