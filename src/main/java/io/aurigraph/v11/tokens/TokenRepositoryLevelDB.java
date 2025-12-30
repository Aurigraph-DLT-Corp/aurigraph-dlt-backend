package io.aurigraph.v11.tokens;

import io.aurigraph.v11.contracts.models.AssetType;
import io.aurigraph.v11.storage.LevelDBRepository;
import io.aurigraph.v11.tokens.models.Token;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Token Repository - LevelDB Implementation
 *
 * Provides per-node embedded storage for Token entities using LevelDB.
 * Replaces Panache/JPA with key-value storage.
 *
 * @version 2.0.0 (Oct 8, 2025 - LevelDB Migration)
 * @author Aurigraph V11 Development Team
 */
@ApplicationScoped
public class TokenRepositoryLevelDB extends LevelDBRepository<Token, String> {

    @Override
    protected Class<Token> getEntityClass() {
        return Token.class;
    }

    @Override
    protected String getKeyPrefix() {
        return "token:";
    }

    @Override
    protected String getId(Token entity) {
        return entity.getTokenId();
    }

    // ==================== BASIC QUERIES ====================

    public Uni<Optional<Token>> findByTokenId(String tokenId) {
        return findById(tokenId);
    }

    public Uni<Boolean> existsByTokenId(String tokenId) {
        return existsById(tokenId);
    }

    public Uni<List<Token>> findByOwner(String owner) {
        return findBy(t -> owner.equals(t.getOwner()))
                .map(list -> list.stream()
                        .sorted(Comparator.comparing(Token::getCreatedAt).reversed())
                        .collect(Collectors.toList()));
    }

    // ==================== TOKEN TYPE QUERIES ====================

    public Uni<List<Token>> findByTokenType(Token.TokenType tokenType) {
        return findBy(t -> t.getTokenType() == tokenType)
                .map(list -> list.stream()
                        .sorted(Comparator.comparing(Token::getCreatedAt).reversed())
                        .collect(Collectors.toList()));
    }

    public Uni<Long> countByTokenType(Token.TokenType tokenType) {
        return countBy(t -> t.getTokenType() == tokenType);
    }

    // ==================== RWA QUERIES ====================

    public Uni<List<Token>> findRWATokens() {
        return findBy(t -> Boolean.TRUE.equals(t.getIsRWA()))
                .map(list -> list.stream()
                        .sorted(Comparator.comparing(Token::getCreatedAt).reversed())
                        .collect(Collectors.toList()));
    }

    public Uni<List<Token>> findRWATokensByAssetType(AssetType assetType) {
        return findBy(t -> Boolean.TRUE.equals(t.getIsRWA()) && assetType.equals(t.getAssetType()))
                .map(list -> list.stream()
                        .sorted(Comparator.comparing(Token::getCreatedAt).reversed())
                        .collect(Collectors.toList()));
    }

    public Uni<Optional<Token>> findByAssetId(String assetId) {
        return findFirstBy(t -> assetId.equals(t.getAssetId()));
    }

    public Uni<Long> countRWATokens() {
        return countBy(t -> Boolean.TRUE.equals(t.getIsRWA()));
    }

    // ==================== SYMBOL & NAME QUERIES ====================

    public Uni<List<Token>> findBySymbol(String symbol) {
        return findBy(t -> symbol.equals(t.getSymbol()))
                .map(list -> list.stream()
                        .sorted(Comparator.comparing(Token::getCreatedAt).reversed())
                        .collect(Collectors.toList()));
    }

    public Uni<List<Token>> searchByName(String namePattern) {
        String pattern = namePattern.toLowerCase();
        return findBy(t -> t.getName() != null && t.getName().toLowerCase().contains(pattern))
                .map(list -> list.stream()
                        .sorted(Comparator.comparing(Token::getCreatedAt).reversed())
                        .collect(Collectors.toList()));
    }

    // ==================== CONTRACT QUERIES ====================

    public Uni<Optional<Token>> findByContractAddress(String contractAddress) {
        return findFirstBy(t -> contractAddress.equals(t.getContractAddress()));
    }

    public Uni<List<Token>> findTokensWithContract() {
        return findBy(t -> t.getContractAddress() != null)
                .map(list -> list.stream()
                        .sorted(Comparator.comparing(Token::getCreatedAt).reversed())
                        .collect(Collectors.toList()));
    }

    // ==================== SUPPLY QUERIES ====================

    public Uni<List<Token>> findByTotalSupplyRange(BigDecimal min, BigDecimal max) {
        return findBy(t -> {
            BigDecimal supply = t.getTotalSupply();
            return supply != null &&
                   supply.compareTo(min) >= 0 &&
                   supply.compareTo(max) <= 0;
        }).map(list -> list.stream()
                .sorted(Comparator.comparing(Token::getTotalSupply).reversed())
                .collect(Collectors.toList()));
    }

    public Uni<List<Token>> findTopBySupply(int limit) {
        return listAll().map(list -> list.stream()
                .filter(t -> t.getTotalSupply() != null)
                .sorted(Comparator.comparing(Token::getTotalSupply).reversed())
                .limit(limit)
                .collect(Collectors.toList()));
    }

    // ==================== MINTABLE/BURNABLE QUERIES ====================

    public Uni<List<Token>> findMintableTokens() {
        return findBy(t -> Boolean.TRUE.equals(t.getIsMintable()) && !Boolean.TRUE.equals(t.getIsPaused()))
                .map(list -> list.stream()
                        .sorted(Comparator.comparing(Token::getCreatedAt).reversed())
                        .collect(Collectors.toList()));
    }

    public Uni<List<Token>> findBurnableTokens() {
        return findBy(t -> Boolean.TRUE.equals(t.getIsBurnable()) && !Boolean.TRUE.equals(t.getIsPaused()))
                .map(list -> list.stream()
                        .sorted(Comparator.comparing(Token::getCreatedAt).reversed())
                        .collect(Collectors.toList()));
    }

    public Uni<List<Token>> findPausedTokens() {
        return findBy(t -> Boolean.TRUE.equals(t.getIsPaused()))
                .map(list -> list.stream()
                        .sorted(Comparator.comparing(Token::getUpdatedAt).reversed())
                        .collect(Collectors.toList()));
    }

    // ==================== TIME-BASED QUERIES ====================

    public Uni<List<Token>> findCreatedAfter(Instant after) {
        return findBy(t -> t.getCreatedAt() != null && t.getCreatedAt().isAfter(after))
                .map(list -> list.stream()
                        .sorted(Comparator.comparing(Token::getCreatedAt).reversed())
                        .collect(Collectors.toList()));
    }

    public Uni<List<Token>> findCreatedBetween(Instant start, Instant end) {
        return findBy(t -> {
            Instant created = t.getCreatedAt();
            return created != null &&
                   !created.isBefore(start) &&
                   !created.isAfter(end);
        }).map(list -> list.stream()
                .sorted(Comparator.comparing(Token::getCreatedAt).reversed())
                .collect(Collectors.toList()));
    }

    public Uni<List<Token>> findRecentlyTransferred(long secondsAgo) {
        Instant since = Instant.now().minusSeconds(secondsAgo);
        return findBy(t -> t.getLastTransferAt() != null && t.getLastTransferAt().isAfter(since))
                .map(list -> list.stream()
                        .sorted(Comparator.comparing(Token::getLastTransferAt).reversed())
                        .collect(Collectors.toList()));
    }

    // ==================== HOLDER QUERIES ====================

    public Uni<List<Token>> findByHolderCountRange(long min, long max) {
        return findBy(t -> t.getHolderCount() >= min && t.getHolderCount() <= max)
                .map(list -> list.stream()
                        .sorted(Comparator.comparing(Token::getHolderCount).reversed())
                        .collect(Collectors.toList()));
    }

    public Uni<List<Token>> findTopByHolders(int limit) {
        return listAll().map(list -> list.stream()
                .sorted(Comparator.comparing(Token::getHolderCount).reversed())
                .limit(limit)
                .collect(Collectors.toList()));
    }

    // ==================== TRANSFER QUERIES ====================

    public Uni<List<Token>> findMostTransferred(int limit) {
        return listAll().map(list -> list.stream()
                .filter(t -> t.getTransferCount() > 0)
                .sorted(Comparator.comparing(Token::getTransferCount).reversed())
                .limit(limit)
                .collect(Collectors.toList()));
    }

    // ==================== COMPLIANCE QUERIES ====================

    public Uni<List<Token>> findCompliantTokens() {
        return findBy(t -> Boolean.TRUE.equals(t.getIsCompliant()))
                .map(list -> list.stream()
                        .sorted(Comparator.comparing(Token::getCreatedAt).reversed())
                        .collect(Collectors.toList()));
    }

    public Uni<List<Token>> findByComplianceStandard(String standard) {
        return findBy(t -> standard.equals(t.getComplianceStandard()))
                .map(list -> list.stream()
                        .sorted(Comparator.comparing(Token::getCreatedAt).reversed())
                        .collect(Collectors.toList()));
    }

    public Uni<List<Token>> findKYCRequiredTokens() {
        return findBy(t -> Boolean.TRUE.equals(t.getKycRequired()))
                .map(list -> list.stream()
                        .sorted(Comparator.comparing(Token::getCreatedAt).reversed())
                        .collect(Collectors.toList()));
    }

    // ==================== TAG QUERIES ====================

    public Uni<List<Token>> findByTag(String tag) {
        return findBy(t -> t.getTags() != null && t.getTags().contains(tag))
                .map(list -> list.stream()
                        .sorted(Comparator.comparing(Token::getCreatedAt).reversed())
                        .collect(Collectors.toList()));
    }

    // ==================== STATISTICS ====================

    public Uni<TokenStatistics> getStatistics() {
        return listAll().map(tokens -> {
            long total = tokens.size();
            long fungible = tokens.stream().filter(t -> t.getTokenType() == Token.TokenType.FUNGIBLE).count();
            long nonFungible = tokens.stream().filter(t -> t.getTokenType() == Token.TokenType.NON_FUNGIBLE).count();
            long rwa = tokens.stream().filter(t -> Boolean.TRUE.equals(t.getIsRWA())).count();

            BigDecimal totalSupply = tokens.stream()
                    .map(Token::getTotalSupply)
                    .filter(s -> s != null)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal totalCirculating = tokens.stream()
                    .map(Token::getCirculatingSupply)
                    .filter(s -> s != null)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            return new TokenStatistics(
                    total,
                    fungible,
                    nonFungible,
                    rwa,
                    totalSupply,
                    totalCirculating
            );
        });
    }

    // ==================== DATA MODELS ====================

    public record TokenStatistics(
            long totalTokens,
            long fungibleTokens,
            long nonFungibleTokens,
            long rwaTokens,
            BigDecimal totalSupply,
            BigDecimal totalCirculating
    ) {}
}
