package io.aurigraph.v11.models;

import io.smallrye.mutiny.Uni;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Token Registry Service
 *
 * Core service for managing token registrations and lifecycle.
 * Provides business logic for token registration, verification, listing,
 * and market data management. Uses in-memory ConcurrentHashMap for storage.
 *
 * @version 11.5.0
 * @since 2025-11-14
 */
@ApplicationScoped
public class TokenRegistryService {

    // In-memory storage using ConcurrentHashMap for thread-safe operations
    private final Map<String, TokenRegistry> tokenRegistry = new ConcurrentHashMap<>();

    /**
     * List all tokens in the registry
     *
     * @return Uni with list of all tokens
     */
    public Uni<List<TokenRegistry>> listTokens() {
        return Uni.createFrom().item(() -> {
            Log.debugf("Listing all tokens in registry (total: %d)", tokenRegistry.size());
            List<TokenRegistry> tokens = new ArrayList<>(tokenRegistry.values());
            // Sort by creation date descending (newest first)
            tokens.sort((a, b) -> {
                Instant aTime = a.getCreatedAt() != null ? a.getCreatedAt() : Instant.EPOCH;
                Instant bTime = b.getCreatedAt() != null ? b.getCreatedAt() : Instant.EPOCH;
                return bTime.compareTo(aTime);
            });
            return tokens;
        });
    }

    /**
     * Register a new token
     *
     * @param name Token name
     * @param symbol Token symbol
     * @param tokenType Token type (ERC20, ERC721, ERC1155)
     * @param creatorAddress Creator address
     * @return Uni with registered token
     */
    public Uni<TokenRegistry> registerToken(String name, String symbol, TokenType tokenType, String creatorAddress) {
        return Uni.createFrom().item(() -> {
            Log.infof("Registering token: %s (%s)", name, symbol);

            TokenRegistry token = new TokenRegistry(name, symbol, tokenType, creatorAddress);
            token.ensureCreatedAt();
            token.updateTimestamp();

            tokenRegistry.put(token.getTokenAddress(), token);
            return token;
        });
    }

    /**
     * Get token by address
     *
     * @param tokenAddress Token address
     * @return Uni with token if found
     */
    public Uni<Optional<TokenRegistry>> getTokenByAddress(String tokenAddress) {
        return Uni.createFrom().item(() -> {
            Log.debugf("Retrieving token: %s", tokenAddress);
            return Optional.ofNullable(tokenRegistry.get(tokenAddress));
        });
    }

    /**
     * Verify a token
     *
     * @param tokenAddress Token address
     * @return Uni with updated token
     */
    public Uni<TokenRegistry> verifyToken(String tokenAddress) {
        return Uni.createFrom().item(() -> {
            TokenRegistry token = tokenRegistry.get(tokenAddress);
            if (token == null) {
                throw new IllegalArgumentException("Token not found: " + tokenAddress);
            }
            token.verify();
            token.updateTimestamp();
            Log.infof("Token verified: %s", tokenAddress);
            return token;
        });
    }

    /**
     * List token on exchange
     *
     * @param tokenAddress Token address
     * @return Uni with updated token
     */
    public Uni<TokenRegistry> listToken(String tokenAddress) {
        return Uni.createFrom().item(() -> {
            TokenRegistry token = tokenRegistry.get(tokenAddress);
            if (token == null) {
                throw new IllegalArgumentException("Token not found: " + tokenAddress);
            }
            token.list();
            token.updateTimestamp();
            Log.infof("Token listed: %s", tokenAddress);
            return token;
        });
    }

    /**
     * Get tokens by type
     *
     * @param tokenType Token type filter
     * @return Uni with filtered tokens
     */
    public Uni<List<TokenRegistry>> getTokensByType(TokenType tokenType) {
        return Uni.createFrom().item(() -> {
            List<TokenRegistry> tokens = tokenRegistry.values().stream()
                    .filter(t -> t.getTokenType() == tokenType)
                    .sorted((a, b) -> {
                        Instant aTime = a.getCreatedAt() != null ? a.getCreatedAt() : Instant.EPOCH;
                        Instant bTime = b.getCreatedAt() != null ? b.getCreatedAt() : Instant.EPOCH;
                        return bTime.compareTo(aTime);
                    })
                    .toList();
            Log.debugf("Found %d tokens of type %s", tokens.size(), tokenType);
            return tokens;
        });
    }

    /**
     * Get RWA tokens only
     *
     * @return Uni with list of RWA tokens
     */
    public Uni<List<TokenRegistry>> getRWATokens() {
        return Uni.createFrom().item(() -> {
            List<TokenRegistry> rwaTokens = tokenRegistry.values().stream()
                    .filter(TokenRegistry::isRealWorldAsset)
                    .sorted((a, b) -> {
                        Instant aTime = a.getCreatedAt() != null ? a.getCreatedAt() : Instant.EPOCH;
                        Instant bTime = b.getCreatedAt() != null ? b.getCreatedAt() : Instant.EPOCH;
                        return bTime.compareTo(aTime);
                    })
                    .toList();
            Log.debugf("Found %d RWA tokens", rwaTokens.size());
            return rwaTokens;
        });
    }

    /**
     * Update token market price
     *
     * @param tokenAddress Token address
     * @param price New price
     * @param volume 24h volume
     * @return Uni with updated token
     */
    public Uni<TokenRegistry> updateMarketPrice(String tokenAddress, BigDecimal price, BigDecimal volume) {
        return Uni.createFrom().item(() -> {
            TokenRegistry token = tokenRegistry.get(tokenAddress);
            if (token == null) {
                throw new IllegalArgumentException("Token not found: " + tokenAddress);
            }
            token.updateMarketData(price, volume);
            token.updateTimestamp();
            Log.debugf("Updated market price for token %s: %s AUR", tokenAddress, price);
            return token;
        });
    }

    /**
     * Get token statistics
     *
     * @return Uni with statistics map
     */
    public Uni<Map<String, Object>> getTokenStatistics() {
        return Uni.createFrom().item(() -> {
            Map<String, Object> stats = new HashMap<>();

            long totalTokens = tokenRegistry.size();
            long verifiedTokens = tokenRegistry.values().stream()
                    .filter(t -> "VERIFIED".equals(t.getVerificationStatus().name()))
                    .count();
            long listedTokens = tokenRegistry.values().stream()
                    .filter(t -> "LISTED".equals(t.getListingStatus().name()))
                    .count();
            long rwaTokens = tokenRegistry.values().stream()
                    .filter(TokenRegistry::isRealWorldAsset)
                    .count();

            BigDecimal totalMarketCap = tokenRegistry.values().stream()
                    .map(TokenRegistry::getMarketCap)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            stats.put("totalTokens", totalTokens);
            stats.put("verifiedTokens", verifiedTokens);
            stats.put("listedTokens", listedTokens);
            stats.put("rwaTokens", rwaTokens);
            stats.put("totalMarketCap", totalMarketCap);
            stats.put("timestamp", Instant.now().toString());

            Log.debugf("Token statistics calculated - Total: %d, Verified: %d, Listed: %d, RWA: %d",
                    totalTokens, verifiedTokens, listedTokens, rwaTokens);
            return stats;
        });
    }

    /**
     * Delete token (soft delete)
     *
     * @param tokenAddress Token address
     * @return Uni indicating success
     */
    public Uni<Boolean> deleteToken(String tokenAddress) {
        return Uni.createFrom().item(() -> {
            TokenRegistry token = tokenRegistry.get(tokenAddress);
            if (token == null) {
                throw new IllegalArgumentException("Token not found: " + tokenAddress);
            }
            token.softDelete();
            token.updateTimestamp();
            Log.infof("Token soft deleted: %s", tokenAddress);
            return true;
        });
    }
}
