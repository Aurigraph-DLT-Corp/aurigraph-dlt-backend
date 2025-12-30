package io.aurigraph.v11.demo.repository;

import io.aurigraph.v11.demo.state.NodeState;
import io.aurigraph.v11.demo.state.NodeStatus;
import io.aurigraph.v11.demo.state.NodeType;
import io.smallrye.mutiny.Uni;

import java.util.List;
import java.util.Optional;

/**
 * NodeStateRepository - Repository interface for persisting node state
 *
 * This interface defines the contract for state persistence using LevelDB.
 * All operations are reactive and return Uni<T> for non-blocking I/O.
 *
 * Implementation Strategy:
 * - LevelDB per-node embedded storage
 * - Key format: "node:state:{nodeId}"
 * - Value: Serialized NodeState (JSON or Protocol Buffers)
 * - Atomic batch operations for consistency
 *
 * Future Integration:
 * - Redis caching layer for shared state
 * - State synchronization across cluster
 * - Snapshot and restore capabilities
 *
 * @author Backend Development Agent (BDA)
 * @version 1.0
 * @since AV11-216
 */
public interface NodeStateRepository {

    /**
     * Save or update a node state
     *
     * @param nodeState the node state to save
     * @return Uni containing the saved node state
     */
    Uni<NodeState> save(NodeState nodeState);

    /**
     * Find a node state by node ID
     *
     * @param nodeId the node ID
     * @return Uni containing Optional of NodeState
     */
    Uni<Optional<NodeState>> findById(String nodeId);

    /**
     * Find all node states
     *
     * @return Uni containing list of all node states
     */
    Uni<List<NodeState>> findAll();

    /**
     * Find node states by type
     *
     * @param nodeType the node type to filter by
     * @return Uni containing list of node states of the specified type
     */
    Uni<List<NodeState>> findByType(NodeType nodeType);

    /**
     * Find node states by status
     *
     * @param status the status to filter by
     * @return Uni containing list of node states with the specified status
     */
    Uni<List<NodeState>> findByStatus(NodeStatus status);

    /**
     * Delete a node state
     *
     * @param nodeId the node ID
     * @return Uni containing true if deleted, false if not found
     */
    Uni<Boolean> delete(String nodeId);

    /**
     * Check if a node state exists
     *
     * @param nodeId the node ID
     * @return Uni containing true if exists, false otherwise
     */
    Uni<Boolean> exists(String nodeId);

    /**
     * Count all node states
     *
     * @return Uni containing the count
     */
    Uni<Long> count();

    /**
     * Count node states by status
     *
     * @param status the status to count
     * @return Uni containing the count
     */
    Uni<Long> countByStatus(NodeStatus status);

    /**
     * Batch save multiple node states atomically
     *
     * @param nodeStates list of node states to save
     * @return Uni containing list of saved node states
     */
    Uni<List<NodeState>> saveAll(List<NodeState> nodeStates);

    /**
     * Delete all node states (use with caution)
     *
     * @return Uni containing the number of deleted states
     */
    Uni<Long> deleteAll();
}
