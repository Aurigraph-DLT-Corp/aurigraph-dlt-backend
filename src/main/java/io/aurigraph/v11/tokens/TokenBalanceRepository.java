package io.aurigraph.v11.tokens;

import io.aurigraph.v11.tokens.models.TokenBalance;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Token Balance Repository - JPA/Panache Implementation
 *
 * Manages token balance tracking for addresses.
 *
 * @version 3.8.0 (Phase 2 Day 8)
 * @author Aurigraph V11 Development Team
 */
@ApplicationScoped
public class TokenBalanceRepository implements PanacheRepository<TokenBalance> {

    // ==================== BASIC QUERIES ====================

    public Optional<TokenBalance> findByTokenAndAddress(String tokenId, String address) {
        return find("tokenId = ?1 and address = ?2", tokenId, address).firstResultOptional();
    }

    public List<TokenBalance> findByToken(String tokenId) {
        return find("tokenId", Sort.descending("balance"), tokenId).list();
    }

    public List<TokenBalance> findByAddress(String address) {
        return find("address", Sort.descending("balance"), address).list();
    }

    // ==================== BALANCE QUERIES ====================

    public List<TokenBalance> findNonZeroBalances(String tokenId) {
        return find("tokenId = ?1 and balance > 0",
                Sort.descending("balance"), tokenId)
                .list();
    }

    public List<TokenBalance> findTopHolders(String tokenId, int limit) {
        return find("tokenId = ?1 and balance > 0",
                Sort.descending("balance"), tokenId)
                .page(0, limit)
                .list();
    }

    public List<TokenBalance> findByBalanceRange(String tokenId, BigDecimal min, BigDecimal max) {
        return find("tokenId = ?1 and balance >= ?2 and balance <= ?3",
                Sort.descending("balance"), tokenId, min, max)
                .list();
    }

    // ==================== HOLDER STATISTICS ====================

    public long countHolders(String tokenId) {
        return count("tokenId = ?1 and balance > 0", tokenId);
    }

    public BigDecimal getTotalBalance(String tokenId) {
        List<TokenBalance> balances = findNonZeroBalances(tokenId);
        return balances.stream()
                .map(TokenBalance::getBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getTotalLockedBalance(String tokenId) {
        List<TokenBalance> balances = find("tokenId = ?1", tokenId).list();
        return balances.stream()
                .map(TokenBalance::getLockedBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // ==================== CLEANUP ====================

    public long deleteZeroBalances() {
        return delete("balance = 0 and lockedBalance = 0");
    }
}
