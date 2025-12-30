package io.aurigraph.v11.repositories;

import io.aurigraph.v11.models.Node;
import io.aurigraph.v11.models.NodeStatus;
import io.aurigraph.v11.models.NodeType;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Node Repository
 *
 * Panache repository for Node entity providing database operations.
 * Part of Sprint 9 - Story 3 (AV11-053)
 *
 * @author Claude Code - Backend Development Agent (BDA)
 * @version 11.0.0
 * @since Sprint 9
 */
@ApplicationScoped
public class NodeRepository implements PanacheRepositoryBase<Node, String> {

    /**
     * Find node by address
     */
    public Optional<Node> findByAddress(String address) {
        return find("address", address).firstResultOptional();
    }

    /**
     * Find nodes by status
     */
    public List<Node> findByStatus(NodeStatus status, int limit, int offset) {
        return find("status", Sort.by("lastHeartbeat").descending(), status)
                .page(Page.of(offset / limit, limit))
                .list();
    }

    /**
     * Find nodes by type
     */
    public List<Node> findByType(NodeType nodeType, int limit, int offset) {
        return find("nodeType", Sort.by("registeredAt").descending(), nodeType)
                .page(Page.of(offset / limit, limit))
                .list();
    }

    /**
     * Find all validator nodes
     */
    public List<Node> findValidators(int limit, int offset) {
        return find("isValidator = true", Sort.by("validatorRank").ascending())
                .page(Page.of(offset / limit, limit))
                .list();
    }

    /**
     * Find active validators (online and validating)
     */
    public List<Node> findActiveValidators() {
        return find("isValidator = true and (status = ?1 or status = ?2)", 
                    Sort.by("validatorRank").ascending(),
                    NodeStatus.ONLINE, NodeStatus.VALIDATING)
                .list();
    }

    /**
     * Find online nodes
     */
    public List<Node> findOnlineNodes(int limit, int offset) {
        return find("status = ?1 or status = ?2 or status = ?3",
                    Sort.by("lastHeartbeat").descending(),
                    NodeStatus.ONLINE, NodeStatus.SYNCING, NodeStatus.VALIDATING)
                .page(Page.of(offset / limit, limit))
                .list();
    }

    /**
     * Find nodes by region
     */
    public List<Node> findByRegion(String region, int limit, int offset) {
        return find("region", Sort.by("lastHeartbeat").descending(), region)
                .page(Page.of(offset / limit, limit))
                .list();
    }

    /**
     * Find stale nodes (heartbeat older than threshold)
     */
    public List<Node> findStaleNodes(Instant thresholdTime) {
        return find("lastHeartbeat < ?1 and (status = ?2 or status = ?3 or status = ?4)",
                    thresholdTime, NodeStatus.ONLINE, NodeStatus.SYNCING, NodeStatus.VALIDATING)
                .list();
    }

    /**
     * Count nodes by status
     */
    public long countByStatus(NodeStatus status) {
        return count("status", status);
    }

    /**
     * Count validators
     */
    public long countValidators() {
        return count("isValidator", true);
    }

    /**
     * Count active validators
     */
    public long countActiveValidators() {
        return count("isValidator = true and (status = ?1 or status = ?2)",
                    NodeStatus.ONLINE, NodeStatus.VALIDATING);
    }

    /**
     * Count online nodes
     */
    public long countOnlineNodes() {
        return count("status = ?1 or status = ?2 or status = ?3",
                    NodeStatus.ONLINE, NodeStatus.SYNCING, NodeStatus.VALIDATING);
    }

    /**
     * Find all nodes with pagination
     */
    public List<Node> findAllPaginated(int limit, int offset) {
        return findAll(Sort.by("registeredAt").descending())
                .page(Page.of(offset / limit, limit))
                .list();
    }

    /**
     * Check if node exists by address
     */
    public boolean existsByAddress(String address) {
        return count("address", address) > 0;
    }

    /**
     * Count all nodes
     */
    public long countAll() {
        return count();
    }

    /**
     * Find top validators by blocks produced
     */
    public List<Node> findTopValidators(int limit) {
        return find("isValidator = true", Sort.by("blocksProduced").descending())
                .page(Page.ofSize(limit))
                .list();
    }
}
