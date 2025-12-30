package io.aurigraph.v11.repositories;

import io.aurigraph.v11.models.Block;
import io.aurigraph.v11.models.BlockStatus;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Block Repository
 *
 * Panache repository for Block entity providing database operations.
 * Part of Sprint 9 - Story 2 (AV11-052)
 *
 * @author Claude Code - Backend Development Agent (BDA)
 * @version 11.0.0
 * @since Sprint 9
 */
@ApplicationScoped
public class BlockRepository implements PanacheRepositoryBase<Block, String> {

    /**
     * Find block by height
     */
    public Optional<Block> findByHeight(Long height) {
        return find("height", height).firstResultOptional();
    }

    /**
     * Find block by hash
     */
    public Optional<Block> findByHash(String hash) {
        return find("hash", hash).firstResultOptional();
    }

    /**
     * Find latest block
     */
    public Optional<Block> findLatestBlock() {
        return find("order by height desc")
                .page(Page.ofSize(1))
                .firstResultOptional();
    }

    /**
     * Find blocks by status
     */
    public List<Block> findByStatus(BlockStatus status, int limit, int offset) {
        return find("status", Sort.by("height").descending(), status)
                .page(Page.of(offset / limit, limit))
                .list();
    }

    /**
     * Find blocks by validator address
     */
    public List<Block> findByValidator(String validatorAddress, int limit, int offset) {
        return find("validatorAddress", Sort.by("height").descending(), validatorAddress)
                .page(Page.of(offset / limit, limit))
                .list();
    }

    /**
     * Find blocks in height range
     */
    public List<Block> findByHeightRange(Long startHeight, Long endHeight) {
        return find("height >= ?1 and height <= ?2", Sort.by("height").ascending(), startHeight, endHeight)
                .list();
    }

    /**
     * Find blocks in time range
     */
    public List<Block> findInTimeRange(Instant startTime, Instant endTime, int limit, int offset) {
        return find("timestamp >= ?1 and timestamp <= ?2", Sort.by("timestamp").descending(), startTime, endTime)
                .page(Page.of(offset / limit, limit))
                .list();
    }

    /**
     * Find finalized blocks
     */
    public List<Block> findFinalizedBlocks(int limit, int offset) {
        return find("status", Sort.by("height").descending(), BlockStatus.FINALIZED)
                .page(Page.of(offset / limit, limit))
                .list();
    }

    /**
     * Find pending blocks
     */
    public List<Block> findPendingBlocks() {
        return find("status", Sort.by("timestamp").ascending(), BlockStatus.PENDING)
                .list();
    }

    /**
     * Count blocks by status
     */
    public long countByStatus(BlockStatus status) {
        return count("status", status);
    }

    /**
     * Count blocks by validator
     */
    public long countByValidator(String validatorAddress) {
        return count("validatorAddress", validatorAddress);
    }

    /**
     * Get latest block height
     */
    public Long getLatestBlockHeight() {
        return findLatestBlock()
                .map(Block::getHeight)
                .orElse(0L);
    }

    /**
     * Find all blocks with pagination (ordered by height descending)
     */
    public List<Block> findAllPaginated(int limit, int offset) {
        return findAll(Sort.by("height").descending())
                .page(Page.of(offset / limit, limit))
                .list();
    }

    /**
     * Check if block exists by height
     */
    public boolean existsByHeight(Long height) {
        return count("height", height) > 0;
    }

    /**
     * Check if block exists by hash
     */
    public boolean existsByHash(String hash) {
        return count("hash", hash) > 0;
    }

    /**
     * Count all blocks
     */
    public long countAll() {
        return count();
    }
}
