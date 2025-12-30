package io.aurigraph.v11.demo.state;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * NodeState - Represents the current state of a node in the Aurigraph V11 network
 *
 * This class encapsulates all state information for a node including:
 * - Current operational status
 * - Node type and identity
 * - Timestamps and metadata
 * - Custom properties for node-specific data
 *
 * Thread-safety: This class is immutable except for metadata which uses ConcurrentHashMap
 *
 * @author Backend Development Agent (BDA)
 * @version 1.0
 * @since AV11-216
 */
public class NodeState {

    private final String nodeId;
    private final NodeType nodeType;
    private final NodeStatus status;
    private final Instant createdAt;
    private final Instant updatedAt;
    private final String lastTransitionReason;
    private final Map<String, Object> metadata;

    // Private constructor for builder pattern
    private NodeState(Builder builder) {
        this.nodeId = Objects.requireNonNull(builder.nodeId, "nodeId cannot be null");
        this.nodeType = Objects.requireNonNull(builder.nodeType, "nodeType cannot be null");
        this.status = Objects.requireNonNull(builder.status, "status cannot be null");
        this.createdAt = builder.createdAt != null ? builder.createdAt : Instant.now();
        this.updatedAt = builder.updatedAt != null ? builder.updatedAt : Instant.now();
        this.lastTransitionReason = builder.lastTransitionReason;
        this.metadata = new ConcurrentHashMap<>(builder.metadata);
    }

    // Getters
    public String getNodeId() {
        return nodeId;
    }

    public NodeType getNodeType() {
        return nodeType;
    }

    public NodeStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public String getLastTransitionReason() {
        return lastTransitionReason;
    }

    public Map<String, Object> getMetadata() {
        return new ConcurrentHashMap<>(metadata);
    }

    public Object getMetadataValue(String key) {
        return metadata.get(key);
    }

    /**
     * Creates a new NodeState with updated status
     *
     * @param newStatus the new status
     * @param reason reason for the transition
     * @return new NodeState instance with updated status
     */
    public NodeState withStatus(NodeStatus newStatus, String reason) {
        return new Builder()
            .nodeId(this.nodeId)
            .nodeType(this.nodeType)
            .status(newStatus)
            .createdAt(this.createdAt)
            .updatedAt(Instant.now())
            .lastTransitionReason(reason)
            .metadata(this.metadata)
            .build();
    }

    /**
     * Creates a new NodeState with additional metadata
     *
     * @param key metadata key
     * @param value metadata value
     * @return new NodeState instance with updated metadata
     */
    public NodeState withMetadata(String key, Object value) {
        Map<String, Object> newMetadata = new ConcurrentHashMap<>(this.metadata);
        newMetadata.put(key, value);
        return new Builder()
            .nodeId(this.nodeId)
            .nodeType(this.nodeType)
            .status(this.status)
            .createdAt(this.createdAt)
            .updatedAt(Instant.now())
            .lastTransitionReason(this.lastTransitionReason)
            .metadata(newMetadata)
            .build();
    }

    /**
     * Builder for NodeState
     */
    public static class Builder {
        private String nodeId;
        private NodeType nodeType;
        private NodeStatus status = NodeStatus.INITIALIZING;
        private Instant createdAt;
        private Instant updatedAt;
        private String lastTransitionReason;
        private Map<String, Object> metadata = new ConcurrentHashMap<>();

        public Builder nodeId(String nodeId) {
            this.nodeId = nodeId;
            return this;
        }

        public Builder nodeType(NodeType nodeType) {
            this.nodeType = nodeType;
            return this;
        }

        public Builder status(NodeStatus status) {
            this.status = status;
            return this;
        }

        public Builder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder updatedAt(Instant updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public Builder lastTransitionReason(String reason) {
            this.lastTransitionReason = reason;
            return this;
        }

        public Builder metadata(Map<String, Object> metadata) {
            if (metadata != null) {
                this.metadata = new ConcurrentHashMap<>(metadata);
            }
            return this;
        }

        public Builder addMetadata(String key, Object value) {
            this.metadata.put(key, value);
            return this;
        }

        public NodeState build() {
            return new NodeState(this);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NodeState nodeState = (NodeState) o;
        return Objects.equals(nodeId, nodeState.nodeId) &&
               nodeType == nodeState.nodeType &&
               status == nodeState.status;
    }

    @Override
    public int hashCode() {
        return Objects.hash(nodeId, nodeType, status);
    }

    @Override
    public String toString() {
        return String.format("NodeState{nodeId='%s', nodeType=%s, status=%s, updatedAt=%s, reason='%s'}",
                           nodeId, nodeType, status, updatedAt, lastTransitionReason);
    }
}
