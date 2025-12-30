package io.aurigraph.v11.demo.state;

import io.aurigraph.v11.demo.events.StateChangeEvent;
import io.aurigraph.v11.demo.events.StateChangeListener;
import io.aurigraph.v11.demo.repository.NodeStateRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * NodeStateManager - Thread-safe manager for node state transitions
 *
 * This service provides:
 * - Thread-safe state transitions with validation
 * - Event notification system for state changes
 * - Integration with persistence layer (LevelDB)
 * - State query and management operations
 *
 * Thread Safety:
 * - Uses ReadWriteLock for concurrent state access
 * - ConcurrentHashMap for in-memory state cache
 * - CopyOnWriteArrayList for listener management
 * - All state mutations are atomic and synchronized
 *
 * Performance Targets:
 * - State transition: <5ms
 * - State query: <1ms (from cache)
 * - Event notification: <2ms per listener
 *
 * @author Backend Development Agent (BDA)
 * @version 1.0
 * @since AV11-216
 */
@ApplicationScoped
public class NodeStateManager {

    private static final Logger LOG = Logger.getLogger(NodeStateManager.class);

    @Inject
    NodeStateRepository repository;

    // In-memory cache of node states for fast access
    private final ConcurrentHashMap<String, NodeState> stateCache = new ConcurrentHashMap<>();

    // Read-write lock for state transitions
    private final ConcurrentHashMap<String, ReadWriteLock> nodeLocks = new ConcurrentHashMap<>();

    // Registered state change listeners
    private final List<StateChangeListener> listeners = new CopyOnWriteArrayList<>();

    /**
     * Initialize a new node state
     *
     * @param nodeId the unique node identifier
     * @param nodeType the type of node
     * @return Uni containing the initialized node state
     */
    public Uni<NodeState> initializeNode(String nodeId, NodeType nodeType) {
        LOG.infof("Initializing node: %s (type: %s)", nodeId, nodeType);

        NodeState initialState = new NodeState.Builder()
            .nodeId(nodeId)
            .nodeType(nodeType)
            .status(NodeStatus.INITIALIZING)
            .lastTransitionReason("Node initialization started")
            .build();

        return repository.save(initialState)
            .invoke(state -> {
                stateCache.put(nodeId, state);
                nodeLocks.putIfAbsent(nodeId, new ReentrantReadWriteLock());
                notifyListeners(new StateChangeEvent(
                    nodeId,
                    NodeStatus.INITIALIZING,
                    NodeStatus.INITIALIZING,
                    "Node initialized",
                    state
                ));
            });
    }

    /**
     * Transition a node to a new state with validation
     *
     * @param nodeId the node ID
     * @param newStatus the target status
     * @param reason the reason for the transition
     * @return Uni containing the updated node state
     */
    public Uni<NodeState> transitionState(String nodeId, NodeStatus newStatus, String reason) {
        return getNodeState(nodeId)
            .onItem().transformToUni(optionalState -> {
                if (optionalState.isEmpty()) {
                    return Uni.createFrom().failure(
                        new IllegalArgumentException("Node not found: " + nodeId)
                    );
                }

                NodeState currentState = optionalState.get();
                ReadWriteLock lock = nodeLocks.computeIfAbsent(nodeId, k -> new ReentrantReadWriteLock());

                // Acquire write lock for state transition
                lock.writeLock().lock();
                try {
                    // Validate transition
                    StateTransition.validateTransition(currentState.getStatus(), newStatus);

                    // Create new state
                    String finalReason = (reason != null && !reason.trim().isEmpty()) ?
                        reason : StateTransition.getRecommendedReason(currentState.getStatus(), newStatus);

                    NodeState newState = currentState.withStatus(newStatus, finalReason);

                    // Persist and update cache
                    return repository.save(newState)
                        .invoke(savedState -> {
                            stateCache.put(nodeId, savedState);

                            // Notify listeners
                            StateChangeEvent event = new StateChangeEvent(
                                nodeId,
                                currentState.getStatus(),
                                newStatus,
                                finalReason,
                                savedState
                            );
                            notifyListeners(event);

                            LOG.infof("Node %s transitioned: %s → %s (reason: %s)",
                                nodeId, currentState.getStatus(), newStatus, finalReason);
                        });

                } catch (StateTransition.IllegalStateTransitionException e) {
                    LOG.errorf("Invalid state transition for node %s: %s", nodeId, e.getMessage());
                    return Uni.createFrom().failure(
                        new IllegalStateException("Invalid state transition: " + e.getMessage(), e)
                    );
                } finally {
                    lock.writeLock().unlock();
                }
            });
    }

    /**
     * Get the current state of a node
     *
     * @param nodeId the node ID
     * @return Uni containing Optional of NodeState
     */
    public Uni<Optional<NodeState>> getNodeState(String nodeId) {
        // Try cache first
        NodeState cachedState = stateCache.get(nodeId);
        if (cachedState != null) {
            return Uni.createFrom().item(Optional.of(cachedState));
        }

        // Fallback to repository
        return repository.findById(nodeId)
            .invoke(optionalState -> optionalState.ifPresent(state -> stateCache.put(nodeId, state)));
    }

    /**
     * Start a node (transition from INITIALIZING to RUNNING)
     *
     * @param nodeId the node ID
     * @return Uni containing the updated node state
     */
    public Uni<NodeState> startNode(String nodeId) {
        return transitionState(nodeId, NodeStatus.RUNNING, "Node started successfully");
    }

    /**
     * Pause a node (transition from RUNNING to PAUSED)
     *
     * @param nodeId the node ID
     * @param reason the reason for pausing
     * @return Uni containing the updated node state
     */
    public Uni<NodeState> pauseNode(String nodeId, String reason) {
        return transitionState(nodeId, NodeStatus.PAUSED, reason);
    }

    /**
     * Resume a node (transition from PAUSED to RUNNING)
     *
     * @param nodeId the node ID
     * @return Uni containing the updated node state
     */
    public Uni<NodeState> resumeNode(String nodeId) {
        return transitionState(nodeId, NodeStatus.RUNNING, "Node resumed from pause");
    }

    /**
     * Stop a node gracefully
     *
     * @param nodeId the node ID
     * @param reason the reason for stopping
     * @return Uni containing the updated node state
     */
    public Uni<NodeState> stopNode(String nodeId, String reason) {
        return transitionState(nodeId, NodeStatus.STOPPED, reason)
            .invoke(state -> {
                // Clean up resources
                stateCache.remove(nodeId);
                nodeLocks.remove(nodeId);
            });
    }

    /**
     * Mark a node as in error state
     *
     * @param nodeId the node ID
     * @param errorReason the error description
     * @return Uni containing the updated node state
     */
    public Uni<NodeState> markNodeError(String nodeId, String errorReason) {
        return transitionState(nodeId, NodeStatus.ERROR, errorReason);
    }

    /**
     * Get all node states
     *
     * @return Uni containing list of all node states
     */
    public Uni<List<NodeState>> getAllNodeStates() {
        return repository.findAll();
    }

    /**
     * Get nodes by type
     *
     * @param nodeType the node type
     * @return Uni containing list of node states
     */
    public Uni<List<NodeState>> getNodesByType(NodeType nodeType) {
        return repository.findByType(nodeType);
    }

    /**
     * Get nodes by status
     *
     * @param status the status
     * @return Uni containing list of node states
     */
    public Uni<List<NodeState>> getNodesByStatus(NodeStatus status) {
        return repository.findByStatus(status);
    }

    /**
     * Register a state change listener
     *
     * @param listener the listener to register
     */
    public void addStateChangeListener(StateChangeListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
            LOG.debugf("Registered state change listener: %s", listener.getClass().getSimpleName());
        }
    }

    /**
     * Unregister a state change listener
     *
     * @param listener the listener to unregister
     */
    public void removeStateChangeListener(StateChangeListener listener) {
        if (listener != null) {
            listeners.remove(listener);
            LOG.debugf("Unregistered state change listener: %s", listener.getClass().getSimpleName());
        }
    }

    /**
     * Get valid transitions from current state
     *
     * @param nodeId the node ID
     * @return Uni containing set of valid target states
     */
    public Uni<java.util.Set<NodeStatus>> getValidTransitions(String nodeId) {
        return getNodeState(nodeId)
            .map(optionalState -> {
                if (optionalState.isEmpty()) {
                    return java.util.Collections.emptySet();
                }
                return StateTransition.getValidTransitions(optionalState.get().getStatus());
            });
    }

    /**
     * Check if a transition is valid
     *
     * @param nodeId the node ID
     * @param targetStatus the target status
     * @return Uni containing true if transition is valid
     */
    public Uni<Boolean> isTransitionValid(String nodeId, NodeStatus targetStatus) {
        return getNodeState(nodeId)
            .map(optionalState -> {
                if (optionalState.isEmpty()) {
                    return false;
                }
                return StateTransition.isValidTransition(
                    optionalState.get().getStatus(),
                    targetStatus
                );
            });
    }

    /**
     * Notify all registered listeners of a state change
     *
     * @param event the state change event
     */
    private void notifyListeners(StateChangeEvent event) {
        if (!event.isStatusChanged() && event.getPreviousStatus() != NodeStatus.INITIALIZING) {
            return; // Don't notify if status hasn't actually changed (except for initialization)
        }

        for (StateChangeListener listener : listeners) {
            try {
                listener.onStateChange(event);
            } catch (Exception e) {
                LOG.errorf(e, "Error notifying listener %s of state change: %s",
                    listener.getClass().getSimpleName(), event);
            }
        }
    }

    /**
     * Get the number of registered listeners
     *
     * @return the listener count
     */
    public int getListenerCount() {
        return listeners.size();
    }

    /**
     * Clear the state cache (for testing or maintenance)
     */
    public void clearCache() {
        stateCache.clear();
        LOG.info("Node state cache cleared");
    }
}
