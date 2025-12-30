package io.aurigraph.v11.demo.repository;

import io.aurigraph.v11.demo.state.NodeState;
import io.aurigraph.v11.demo.state.NodeStatus;
import io.aurigraph.v11.demo.state.NodeType;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * InMemoryNodeStateRepository - In-memory implementation of NodeStateRepository
 *
 * This is a simple in-memory implementation for development and testing.
 * In production, this should be replaced with a LevelDB-backed implementation.
 *
 * Thread Safety:
 * - Uses ConcurrentHashMap for thread-safe operations
 * - All operations are atomic
 *
 * Limitations:
 * - Data is lost on application restart
 * - No persistence to disk
 * - Single-node only (no distributed state)
 *
 * @author Backend Development Agent (BDA)
 * @version 1.0
 * @since AV11-216
 */
@ApplicationScoped
@Named("inMemoryNodeStateRepository")
public class InMemoryNodeStateRepository implements NodeStateRepository {

    private final Map<String, NodeState> storage = new ConcurrentHashMap<>();

    @Override
    public Uni<NodeState> save(NodeState nodeState) {
        storage.put(nodeState.getNodeId(), nodeState);
        return Uni.createFrom().item(nodeState);
    }

    @Override
    public Uni<Optional<NodeState>> findById(String nodeId) {
        return Uni.createFrom().item(Optional.ofNullable(storage.get(nodeId)));
    }

    @Override
    public Uni<List<NodeState>> findAll() {
        return Uni.createFrom().item(List.copyOf(storage.values()));
    }

    @Override
    public Uni<List<NodeState>> findByType(NodeType nodeType) {
        List<NodeState> result = storage.values().stream()
            .filter(state -> state.getNodeType() == nodeType)
            .collect(Collectors.toList());
        return Uni.createFrom().item(result);
    }

    @Override
    public Uni<List<NodeState>> findByStatus(NodeStatus status) {
        List<NodeState> result = storage.values().stream()
            .filter(state -> state.getStatus() == status)
            .collect(Collectors.toList());
        return Uni.createFrom().item(result);
    }

    @Override
    public Uni<Boolean> delete(String nodeId) {
        boolean existed = storage.remove(nodeId) != null;
        return Uni.createFrom().item(existed);
    }

    @Override
    public Uni<Boolean> exists(String nodeId) {
        return Uni.createFrom().item(storage.containsKey(nodeId));
    }

    @Override
    public Uni<Long> count() {
        return Uni.createFrom().item((long) storage.size());
    }

    @Override
    public Uni<Long> countByStatus(NodeStatus status) {
        long count = storage.values().stream()
            .filter(state -> state.getStatus() == status)
            .count();
        return Uni.createFrom().item(count);
    }

    @Override
    public Uni<List<NodeState>> saveAll(List<NodeState> nodeStates) {
        nodeStates.forEach(state -> storage.put(state.getNodeId(), state));
        return Uni.createFrom().item(nodeStates);
    }

    @Override
    public Uni<Long> deleteAll() {
        long count = storage.size();
        storage.clear();
        return Uni.createFrom().item(count);
    }
}
