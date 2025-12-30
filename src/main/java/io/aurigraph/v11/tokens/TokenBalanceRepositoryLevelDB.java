package io.aurigraph.v11.tokens;

import io.aurigraph.v11.storage.LevelDBRepository;
import io.aurigraph.v11.tokens.models.TokenBalance;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Token Balance Repository - LevelDB Implementation
 *
 * Manages token balance tracking for addresses using embedded LevelDB storage.
 * Uses composite key: "balance:{tokenId}:{address}"
 *
 * @version 2.0.0 (Oct 8, 2025 - LevelDB Migration)
 * @author Aurigraph V11 Development Team
 */
@ApplicationScoped
public class TokenBalanceRepositoryLevelDB extends LevelDBRepository<TokenBalance, String> {

    @Override
    protected Class<TokenBalance> getEntityClass() {
        return TokenBalance.class;
    }

    @Override
    protected String getKeyPrefix() {
        return "balance:";
    }

    @Override
    protected String getId(TokenBalance entity) {
        return buildCompositeId(entity.getTokenId(), entity.getAddress());
    }

    // ==================== COMPOSITE KEY HELPERS ====================

    private String buildCompositeId(String tokenId, String address) {
        return tokenId + ":" + address;
    }

    @Override
    protected String buildKey(String id) {
        return getKeyPrefix() + id;
    }

    // ==================== BASIC QUERIES ====================

    public Uni<Optional<TokenBalance>> findByTokenAndAddress(String tokenId, String address) {
        String compositeId = buildCompositeId(tokenId, address);
        return findById(compositeId);
    }

    public Uni<List<TokenBalance>> findByToken(String tokenId) {
        return findBy(b -> tokenId.equals(b.getTokenId()))
                .map(list -> list.stream()
                        .sorted(Comparator.comparing(TokenBalance::getBalance).reversed())
                        .collect(Collectors.toList()));
    }

    public Uni<List<TokenBalance>> findByAddress(String address) {
        return findBy(b -> address.equals(b.getAddress()))
                .map(list -> list.stream()
                        .sorted(Comparator.comparing(TokenBalance::getBalance).reversed())
                        .collect(Collectors.toList()));
    }

    // ==================== BALANCE QUERIES ====================

    public Uni<List<TokenBalance>> findNonZeroBalances(String tokenId) {
        return findBy(b -> tokenId.equals(b.getTokenId()) &&
                          b.getBalance() != null &&
                          b.getBalance().compareTo(BigDecimal.ZERO) > 0)
                .map(list -> list.stream()
                        .sorted(Comparator.comparing(TokenBalance::getBalance).reversed())
                        .collect(Collectors.toList()));
    }

    public Uni<List<TokenBalance>> findTopHolders(String tokenId, int limit) {
        return findNonZeroBalances(tokenId)
                .map(list -> list.stream()
                        .limit(limit)
                        .collect(Collectors.toList()));
    }

    public Uni<List<TokenBalance>> findByBalanceRange(String tokenId, BigDecimal min, BigDecimal max) {
        return findBy(b -> {
            if (!tokenId.equals(b.getTokenId())) return false;
            BigDecimal balance = b.getBalance();
            return balance != null &&
                   balance.compareTo(min) >= 0 &&
                   balance.compareTo(max) <= 0;
        }).map(list -> list.stream()
                .sorted(Comparator.comparing(TokenBalance::getBalance).reversed())
                .collect(Collectors.toList()));
    }

    // ==================== HOLDER STATISTICS ====================

    public Uni<Long> countHolders(String tokenId) {
        return countBy(b -> tokenId.equals(b.getTokenId()) &&
                           b.getBalance() != null &&
                           b.getBalance().compareTo(BigDecimal.ZERO) > 0);
    }

    public Uni<BigDecimal> getTotalBalance(String tokenId) {
        return findNonZeroBalances(tokenId)
                .map(balances -> balances.stream()
                        .map(TokenBalance::getBalance)
                        .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    public Uni<BigDecimal> getTotalLockedBalance(String tokenId) {
        return findByToken(tokenId)
                .map(balances -> balances.stream()
                        .map(TokenBalance::getLockedBalance)
                        .filter(locked -> locked != null)
                        .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    // ==================== CLEANUP ====================

    public Uni<Long> deleteZeroBalances() {
        return findBy(b -> {
            BigDecimal balance = b.getBalance();
            BigDecimal locked = b.getLockedBalance();
            boolean zeroBalance = balance == null || balance.compareTo(BigDecimal.ZERO) == 0;
            boolean zeroLocked = locked == null || locked.compareTo(BigDecimal.ZERO) == 0;
            return zeroBalance && zeroLocked;
        }).flatMap(list -> {
            List<String> keysToDelete = list.stream()
                    .map(this::getId)
                    .map(this::buildKey)
                    .collect(Collectors.toList());

            return levelDB.batchWrite(null, keysToDelete)
                    .map(v -> (long) keysToDelete.size());
        });
    }
}
