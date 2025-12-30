package io.aurigraph.v11.tokens;

import io.aurigraph.v11.contracts.models.AssetType;
import io.aurigraph.v11.tokens.models.Token;
import io.aurigraph.v11.tokens.models.TokenBalance;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Token Management Service for Aurigraph V11
 *
 * Handles token operations including mint, burn, transfer, and RWA tokenization.
 * Supports ERC20-like fungible tokens, ERC721-like NFTs, and real-world asset tokens.
 *
 * REFACTORED: Fully reactive LevelDB implementation (Oct 9, 2025)
 * - Replaced Panache blocking repositories with reactive LevelDBRepository
 * - Removed @Transactional annotations (LevelDB handles atomicity)
 * - Converted all blocking operations to reactive Uni chains with flatMap
 *
 * @version 4.0.0 (Oct 9, 2025 - LevelDB Reactive Migration)
 * @author Aurigraph V11 Development Team
 */
@ApplicationScoped
public class TokenManagementService {

    private static final Logger LOG = Logger.getLogger(TokenManagementService.class);

    @Inject
    TokenRepositoryLevelDB tokenRepository;

    @Inject
    TokenBalanceRepositoryLevelDB balanceRepository;

    // Performance metrics
    private final AtomicLong tokensMinted = new AtomicLong(0);
    private final AtomicLong tokensBurned = new AtomicLong(0);
    private final AtomicLong transfersCompleted = new AtomicLong(0);
    private final AtomicLong rwaTokensCreated = new AtomicLong(0);

    // Virtual thread executor for high concurrency
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    // ==================== TOKEN OPERATIONS ====================

    /**
     * Mint new tokens to an address
     * REACTIVE: Fully chained flatMap operations, no blocking
     */
    public Uni<MintResult> mintToken(MintRequest request) {
        LOG.infof("Minting %s tokens of %s to %s",
                request.amount(), request.tokenId(), request.toAddress());

        return tokenRepository.findByTokenId(request.tokenId())
                .flatMap(optToken -> {
                    if (optToken.isEmpty()) {
                        return Uni.createFrom().failure(
                                new IllegalArgumentException("Token not found: " + request.tokenId()));
                    }

                    Token token = optToken.get();
                    token.mint(request.amount());

                    // Save token and update balance reactively
                    return tokenRepository.persist(token)
                            .flatMap(savedToken ->
                                    balanceRepository.findByTokenAndAddress(request.tokenId(), request.toAddress())
                                            .flatMap(optBalance -> {
                                                TokenBalance balance = optBalance.orElse(
                                                        new TokenBalance(request.tokenId(), request.toAddress(), BigDecimal.ZERO)
                                                );
                                                balance.add(request.amount());

                                                return balanceRepository.persist(balance)
                                                        .flatMap(savedBalance ->
                                                                // Update holder count reactively
                                                                balanceRepository.countHolders(request.tokenId())
                                                                        .flatMap(holderCount -> {
                                                                            savedToken.updateHolderCount(holderCount);
                                                                            return tokenRepository.persist(savedToken)
                                                                                    .map(finalToken -> {
                                                                                        tokensMinted.incrementAndGet();

                                                                                        return new MintResult(
                                                                                                request.tokenId(),
                                                                                                request.toAddress(),
                                                                                                request.amount(),
                                                                                                finalToken.getTotalSupply(),
                                                                                                savedBalance.getBalance(),
                                                                                                generateTransactionHash(),
                                                                                                Instant.now()
                                                                                        );
                                                                                    });
                                                                        })
                                                        );
                                            })
                            );
                });
    }

    /**
     * Burn tokens from an address
     * REACTIVE: Fully chained flatMap operations, no blocking
     */
    public Uni<BurnResult> burnToken(BurnRequest request) {
        LOG.infof("Burning %s tokens of %s from %s",
                request.amount(), request.tokenId(), request.fromAddress());

        return tokenRepository.findByTokenId(request.tokenId())
                .flatMap(optToken -> {
                    if (optToken.isEmpty()) {
                        return Uni.createFrom().failure(
                                new IllegalArgumentException("Token not found: " + request.tokenId()));
                    }

                    Token token = optToken.get();

                    return balanceRepository.findByTokenAndAddress(request.tokenId(), request.fromAddress())
                            .flatMap(optBalance -> {
                                if (optBalance.isEmpty()) {
                                    return Uni.createFrom().failure(
                                            new IllegalArgumentException("Balance not found"));
                                }

                                TokenBalance balance = optBalance.get();
                                balance.subtract(request.amount());
                                token.burn(request.amount());

                                // Save balance and token reactively
                                return balanceRepository.persist(balance)
                                        .flatMap(savedBalance ->
                                                tokenRepository.persist(token)
                                                        .flatMap(savedToken ->
                                                                // Update holder count reactively
                                                                balanceRepository.countHolders(request.tokenId())
                                                                        .flatMap(holderCount -> {
                                                                            savedToken.updateHolderCount(holderCount);
                                                                            return tokenRepository.persist(savedToken)
                                                                                    .map(finalToken -> {
                                                                                        tokensBurned.incrementAndGet();

                                                                                        return new BurnResult(
                                                                                                request.tokenId(),
                                                                                                request.fromAddress(),
                                                                                                request.amount(),
                                                                                                finalToken.getTotalSupply(),
                                                                                                finalToken.getBurnedAmount(),
                                                                                                generateTransactionHash(),
                                                                                                Instant.now()
                                                                                        );
                                                                                    });
                                                                        })
                                                        )
                                        );
                            });
                });
    }

    /**
     * Transfer tokens between addresses
     * REACTIVE: Fully chained flatMap operations, no blocking
     */
    public Uni<TransferResult> transferToken(TransferRequest request) {
        LOG.infof("Transferring %s tokens of %s from %s to %s",
                request.amount(), request.tokenId(), request.fromAddress(), request.toAddress());

        return tokenRepository.findByTokenId(request.tokenId())
                .flatMap(optToken -> {
                    if (optToken.isEmpty()) {
                        return Uni.createFrom().failure(
                                new IllegalArgumentException("Token not found: " + request.tokenId()));
                    }

                    Token token = optToken.get();

                    // Check if paused
                    if (token.getIsPaused()) {
                        return Uni.createFrom().failure(
                                new IllegalStateException("Token is paused"));
                    }

                    return balanceRepository.findByTokenAndAddress(request.tokenId(), request.fromAddress())
                            .flatMap(optFromBalance -> {
                                if (optFromBalance.isEmpty()) {
                                    return Uni.createFrom().failure(
                                            new IllegalArgumentException("Sender balance not found"));
                                }

                                TokenBalance fromBalance = optFromBalance.get();

                                return balanceRepository.findByTokenAndAddress(request.tokenId(), request.toAddress())
                                        .flatMap(optToBalance -> {
                                            TokenBalance toBalance = optToBalance.orElse(
                                                    new TokenBalance(request.tokenId(), request.toAddress(), BigDecimal.ZERO)
                                            );

                                            // Transfer
                                            fromBalance.subtract(request.amount());
                                            toBalance.add(request.amount());

                                            // Save both balances reactively
                                            return balanceRepository.persist(fromBalance)
                                                    .flatMap(savedFromBalance ->
                                                            balanceRepository.persist(toBalance)
                                                                    .flatMap(savedToBalance -> {
                                                                        // Update token transfer count
                                                                        token.recordTransfer();

                                                                        return tokenRepository.persist(token)
                                                                                .flatMap(savedToken ->
                                                                                        // Update holder count reactively
                                                                                        balanceRepository.countHolders(request.tokenId())
                                                                                                .flatMap(holderCount -> {
                                                                                                    savedToken.updateHolderCount(holderCount);
                                                                                                    return tokenRepository.persist(savedToken)
                                                                                                            .map(finalToken -> {
                                                                                                                transfersCompleted.incrementAndGet();

                                                                                                                return new TransferResult(
                                                                                                                        request.tokenId(),
                                                                                                                        request.fromAddress(),
                                                                                                                        request.toAddress(),
                                                                                                                        request.amount(),
                                                                                                                        savedFromBalance.getBalance(),
                                                                                                                        savedToBalance.getBalance(),
                                                                                                                        generateTransactionHash(),
                                                                                                                        Instant.now()
                                                                                                                );
                                                                                                            });
                                                                                                })
                                                                                );
                                                                    })
                                                    );
                                        });
                            });
                });
    }

    // ==================== QUERY OPERATIONS ====================

    /**
     * Get token balance for an address
     * REACTIVE: Direct repository chain, no blocking
     */
    public Uni<BigDecimal> getBalance(String address, String tokenId) {
        return balanceRepository.findByTokenAndAddress(tokenId, address)
                .map(optBalance -> optBalance
                        .map(TokenBalance::getBalance)
                        .orElse(BigDecimal.ZERO));
    }

    /**
     * Get total token supply
     * REACTIVE: Direct repository chain, no blocking
     */
    public Uni<TokenSupply> getTotalSupply(String tokenId) {
        return tokenRepository.findByTokenId(tokenId)
                .map(optToken -> {
                    if (optToken.isEmpty()) {
                        throw new IllegalArgumentException("Token not found: " + tokenId);
                    }
                    Token token = optToken.get();
                    return new TokenSupply(
                            tokenId,
                            token.getTotalSupply(),
                            token.getCirculatingSupply(),
                            token.getBurnedAmount(),
                            token.getMaxSupply()
                    );
                });
    }

    /**
     * Get token holders
     * REACTIVE: Combined repository operations
     */
    public Uni<List<TokenHolder>> getTokenHolders(String tokenId, int limit) {
        return tokenRepository.findByTokenId(tokenId)
                .flatMap(optToken -> {
                    if (optToken.isEmpty()) {
                        return Uni.createFrom().failure(
                                new IllegalArgumentException("Token not found: " + tokenId));
                    }
                    Token token = optToken.get();

                    return balanceRepository.findTopHolders(tokenId, limit)
                            .map(balances -> balances.stream()
                                    .map(balance -> new TokenHolder(
                                            balance.getAddress(),
                                            balance.getBalance(),
                                            calculatePercentage(balance.getBalance(), token.getTotalSupply()),
                                            balance.getLastTransferAt()
                                    ))
                                    .toList());
                });
    }

    // ==================== RWA OPERATIONS ====================

    /**
     * Create a new RWA token
     * REACTIVE: Fully chained flatMap operations
     */
    public Uni<Token> createRWAToken(RWATokenRequest request) {
        LOG.infof("Creating RWA token: %s for asset %s", request.name(), request.assetId());

        Token token = new Token();
        token.setTokenId(generateTokenId());
        token.setName(request.name());
        token.setSymbol(request.symbol());
        token.setTokenType(Token.TokenType.RWA_BACKED);
        token.setTotalSupply(request.totalSupply());
        token.setCirculatingSupply(request.totalSupply());
        token.setOwner(request.owner());
        token.setDecimals(request.decimals() != null ? request.decimals() : 18);

        // RWA fields
        token.setIsRWA(true);
        token.setAssetType(request.assetType());
        token.setAssetId(request.assetId());
        token.setAssetValue(request.assetValue());
        token.setAssetCurrency(request.assetCurrency());

        // Economics
        token.setIsMintable(request.isMintable() != null ? request.isMintable() : false);
        token.setIsBurnable(request.isBurnable() != null ? request.isBurnable() : false);
        token.setMaxSupply(request.maxSupply());

        // Compliance
        token.setKycRequired(request.kycRequired() != null ? request.kycRequired() : true);

        return tokenRepository.persist(token)
                .flatMap(savedToken -> {
                    // Create initial balance for owner if supply > 0
                    if (request.totalSupply().compareTo(BigDecimal.ZERO) > 0) {
                        TokenBalance ownerBalance = new TokenBalance(
                                savedToken.getTokenId(),
                                request.owner(),
                                request.totalSupply()
                        );

                        return balanceRepository.persist(ownerBalance)
                                .flatMap(savedBalance -> {
                                    savedToken.updateHolderCount(1);
                                    return tokenRepository.persist(savedToken)
                                            .map(finalToken -> {
                                                rwaTokensCreated.incrementAndGet();
                                                LOG.infof("RWA token created: %s", finalToken.getTokenId());
                                                return finalToken;
                                            });
                                });
                    } else {
                        rwaTokensCreated.incrementAndGet();
                        LOG.infof("RWA token created: %s", savedToken.getTokenId());
                        return Uni.createFrom().item(savedToken);
                    }
                });
    }

    /**
     * Tokenize an existing asset
     * REACTIVE: Delegates to createRWAToken
     */
    public Uni<Token> tokenizeAsset(AssetTokenizationRequest request) {
        LOG.infof("Tokenizing asset: %s", request.assetId());

        RWATokenRequest rwaRequest = new RWATokenRequest(
                request.assetName() + " Token",
                request.assetSymbol(),
                request.owner(),
                request.totalSupply(),
                18,
                request.assetType(),
                request.assetId(),
                request.assetValue(),
                request.assetCurrency(),
                request.isMintable(),
                request.isBurnable(),
                request.maxSupply(),
                request.kycRequired()
        );

        return createRWAToken(rwaRequest);
    }

    /**
     * Get token information
     * REACTIVE: Direct repository chain
     */
    public Uni<Token> getToken(String tokenId) {
        return tokenRepository.findByTokenId(tokenId)
                .map(optToken -> optToken.orElseThrow(
                        () -> new IllegalArgumentException("Token not found: " + tokenId)));
    }

    /**
     * List tokens with pagination
     * REACTIVE: Uses LevelDB listAll with stream limiting
     */
    public Uni<List<Token>> listTokens(int page, int size) {
        return tokenRepository.listAll()
                .map(tokens -> tokens.stream()
                        .skip((long) page * size)
                        .limit(size)
                        .toList());
    }

    // ==================== STATISTICS ====================

    /**
     * Get service statistics
     * REACTIVE: Combines multiple repository queries
     */
    public Uni<Map<String, Object>> getStatistics() {
        return tokenRepository.getStatistics()
                .map(tokenStats -> {
                    Map<String, Object> stats = new HashMap<>();

                    stats.put("tokensMinted", tokensMinted.get());
                    stats.put("tokensBurned", tokensBurned.get());
                    stats.put("transfersCompleted", transfersCompleted.get());
                    stats.put("rwaTokensCreated", rwaTokensCreated.get());

                    stats.put("tokenStatistics", Map.of(
                            "totalTokens", tokenStats.totalTokens(),
                            "fungibleTokens", tokenStats.fungibleTokens(),
                            "nonFungibleTokens", tokenStats.nonFungibleTokens(),
                            "rwaTokens", tokenStats.rwaTokens(),
                            "totalSupply", tokenStats.totalSupply(),
                            "totalCirculating", tokenStats.totalCirculating()
                    ));

                    stats.put("timestamp", Instant.now());

                    return stats;
                });
    }

    // ==================== HELPER METHODS ====================

    private String generateTokenId() {
        return "TOKEN_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8);
    }

    private String generateTransactionHash() {
        return "0x" + UUID.randomUUID().toString().replace("-", "");
    }

    private BigDecimal calculatePercentage(BigDecimal amount, BigDecimal total) {
        if (total.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return amount.divide(total, 4, BigDecimal.ROUND_HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }

    // ==================== DATA MODELS ====================

    public record MintRequest(
            String tokenId,
            String toAddress,
            BigDecimal amount
    ) {}

    public record MintResult(
            String tokenId,
            String toAddress,
            BigDecimal amount,
            BigDecimal newTotalSupply,
            BigDecimal recipientBalance,
            String transactionHash,
            Instant timestamp
    ) {}

    public record BurnRequest(
            String tokenId,
            String fromAddress,
            BigDecimal amount
    ) {}

    public record BurnResult(
            String tokenId,
            String fromAddress,
            BigDecimal amount,
            BigDecimal newTotalSupply,
            BigDecimal totalBurned,
            String transactionHash,
            Instant timestamp
    ) {}

    public record TransferRequest(
            String tokenId,
            String fromAddress,
            String toAddress,
            BigDecimal amount
    ) {}

    public record TransferResult(
            String tokenId,
            String fromAddress,
            String toAddress,
            BigDecimal amount,
            BigDecimal senderBalance,
            BigDecimal recipientBalance,
            String transactionHash,
            Instant timestamp
    ) {}

    public record TokenSupply(
            String tokenId,
            BigDecimal totalSupply,
            BigDecimal circulatingSupply,
            BigDecimal burnedAmount,
            BigDecimal maxSupply
    ) {}

    public record TokenHolder(
            String address,
            BigDecimal balance,
            BigDecimal percentage,
            Instant lastTransferAt
    ) {}

    public record RWATokenRequest(
            String name,
            String symbol,
            String owner,
            BigDecimal totalSupply,
            Integer decimals,
            AssetType assetType,
            String assetId,
            BigDecimal assetValue,
            String assetCurrency,
            Boolean isMintable,
            Boolean isBurnable,
            BigDecimal maxSupply,
            Boolean kycRequired
    ) {}

    public record AssetTokenizationRequest(
            String assetName,
            String assetSymbol,
            String owner,
            BigDecimal totalSupply,
            AssetType assetType,
            String assetId,
            BigDecimal assetValue,
            String assetCurrency,
            Boolean isMintable,
            Boolean isBurnable,
            BigDecimal maxSupply,
            Boolean kycRequired
    ) {}
}
