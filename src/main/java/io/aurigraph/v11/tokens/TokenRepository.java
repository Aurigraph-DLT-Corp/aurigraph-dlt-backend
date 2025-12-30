package io.aurigraph.v11.tokens;

import io.aurigraph.v11.contracts.models.AssetType;
import io.aurigraph.v11.tokens.models.Token;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Token Repository - JPA/Panache Implementation
 *
 * Provides database persistence for Token entities.
 * Supports standard tokens, RWA tokens, and NFTs.
 *
 * @version 3.8.0 (Phase 2 Day 8)
 * @author Aurigraph V11 Development Team
 */
@ApplicationScoped
public class TokenRepository implements PanacheRepository<Token> {

    // ==================== BASIC QUERIES ====================

    public Optional<Token> findByTokenId(String tokenId) {
        return find("tokenId", tokenId).firstResultOptional();
    }

    public boolean existsByTokenId(String tokenId) {
        return count("tokenId", tokenId) > 0;
    }

    public List<Token> findByOwner(String owner) {
        return find("owner", Sort.descending("createdAt"), owner).list();
    }

    public List<Token> findByOwner(String owner, Page page) {
        return find("owner", Sort.descending("createdAt"), owner)
                .page(page)
                .list();
    }

    // ==================== TOKEN TYPE QUERIES ====================

    public List<Token> findByTokenType(Token.TokenType tokenType) {
        return find("tokenType", Sort.descending("createdAt"), tokenType).list();
    }

    public List<Token> findByTokenType(Token.TokenType tokenType, Page page) {
        return find("tokenType", Sort.descending("createdAt"), tokenType)
                .page(page)
                .list();
    }

    public long countByTokenType(Token.TokenType tokenType) {
        return count("tokenType", tokenType);
    }

    // ==================== RWA QUERIES ====================

    public List<Token> findRWATokens() {
        return find("isRWA = true", Sort.descending("createdAt")).list();
    }

    public List<Token> findRWATokensByAssetType(AssetType assetType) {
        return find("isRWA = true and assetType = ?1",
                Sort.descending("createdAt"), assetType)
                .list();
    }

    public Optional<Token> findByAssetId(String assetId) {
        return find("assetId", assetId).firstResultOptional();
    }

    public long countRWATokens() {
        return count("isRWA = true");
    }

    // ==================== SYMBOL & NAME QUERIES ====================

    public List<Token> findBySymbol(String symbol) {
        return find("symbol", Sort.descending("createdAt"), symbol).list();
    }

    public List<Token> searchByName(String namePattern) {
        return find("lower(name) like lower(?1)",
                Sort.descending("createdAt"), "%" + namePattern + "%")
                .list();
    }

    // ==================== CONTRACT QUERIES ====================

    public Optional<Token> findByContractAddress(String contractAddress) {
        return find("contractAddress", contractAddress).firstResultOptional();
    }

    public List<Token> findTokensWithContract() {
        return find("contractAddress is not null", Sort.descending("createdAt")).list();
    }

    // ==================== SUPPLY QUERIES ====================

    public List<Token> findByTotalSupplyRange(BigDecimal min, BigDecimal max) {
        return find("totalSupply >= ?1 and totalSupply <= ?2",
                Sort.descending("totalSupply"), min, max)
                .list();
    }

    public List<Token> findTopBySupply(int limit) {
        return find("1=1", Sort.descending("totalSupply"))
                .page(Page.ofSize(limit))
                .list();
    }

    // ==================== MINTABLE/BURNABLE QUERIES ====================

    public List<Token> findMintableTokens() {
        return find("isMintable = true and isPaused = false",
                Sort.descending("createdAt"))
                .list();
    }

    public List<Token> findBurnableTokens() {
        return find("isBurnable = true and isPaused = false",
                Sort.descending("createdAt"))
                .list();
    }

    public List<Token> findPausedTokens() {
        return find("isPaused = true", Sort.descending("updatedAt")).list();
    }

    // ==================== TIME-BASED QUERIES ====================

    public List<Token> findCreatedAfter(Instant after) {
        return find("createdAt > ?1", Sort.descending("createdAt"), after).list();
    }

    public List<Token> findCreatedBetween(Instant start, Instant end) {
        return find("createdAt >= ?1 and createdAt <= ?2",
                Sort.descending("createdAt"), start, end)
                .list();
    }

    public List<Token> findRecentlyTransferred(long secondsAgo) {
        Instant since = Instant.now().minusSeconds(secondsAgo);
        return find("lastTransferAt > ?1", Sort.descending("lastTransferAt"), since).list();
    }

    // ==================== HOLDER QUERIES ====================

    public List<Token> findByHolderCountRange(long min, long max) {
        return find("holderCount >= ?1 and holderCount <= ?2",
                Sort.descending("holderCount"), min, max)
                .list();
    }

    public List<Token> findTopByHolders(int limit) {
        return find("1=1", Sort.descending("holderCount"))
                .page(Page.ofSize(limit))
                .list();
    }

    // ==================== TRANSFER QUERIES ====================

    public List<Token> findMostTransferred(int limit) {
        return find("transferCount > 0", Sort.descending("transferCount"))
                .page(Page.ofSize(limit))
                .list();
    }

    // ==================== COMPLIANCE QUERIES ====================

    public List<Token> findCompliantTokens() {
        return find("isCompliant = true", Sort.descending("createdAt")).list();
    }

    public List<Token> findByComplianceStandard(String standard) {
        return find("complianceStandard", Sort.descending("createdAt"), standard).list();
    }

    public List<Token> findKYCRequiredTokens() {
        return find("kycRequired = true", Sort.descending("createdAt")).list();
    }

    // ==================== TAG QUERIES ====================

    public List<Token> findByTag(String tag) {
        return find("select t from Token t join t.tags tag where tag = ?1",
                Sort.descending("createdAt"), tag)
                .list();
    }

    // ==================== STATISTICS ====================

    public TokenStatistics getStatistics() {
        long total = count();
        long fungible = countByTokenType(Token.TokenType.FUNGIBLE);
        long nonFungible = countByTokenType(Token.TokenType.NON_FUNGIBLE);
        long rwa = countRWATokens();

        BigDecimal totalSupply = listAll()
                .stream()
                .map(Token::getTotalSupply)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalCirculating = listAll()
                .stream()
                .map(Token::getCirculatingSupply)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new TokenStatistics(
                total,
                fungible,
                nonFungible,
                rwa,
                totalSupply,
                totalCirculating
        );
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
