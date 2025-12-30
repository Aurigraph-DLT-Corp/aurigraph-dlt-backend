package io.aurigraph.v11.demo.events;

import io.aurigraph.v11.demo.state.NodeState;
import io.aurigraph.v11.demo.state.NodeStatus;

import java.time.Instant;
import java.util.Objects;

/**
 * StateChangeEvent - Event emitted when a node state changes
 *
 * This event contains information about:
 * - The node that changed state
 * - Previous and new states
 * - Reason for the change
 * - Timestamp of the change
 *
 * @author Backend Development Agent (BDA)
 * @version 1.0
 * @since AV11-216
 */
public class StateChangeEvent {

    private final String nodeId;
    private final NodeStatus previousStatus;
    private final NodeStatus newStatus;
    private final String reason;
    private final Instant timestamp;
    private final NodeState nodeState;

    public StateChangeEvent(String nodeId, NodeStatus previousStatus, NodeStatus newStatus,
                           String reason, NodeState nodeState) {
        this.nodeId = Objects.requireNonNull(nodeId, "nodeId cannot be null");
        this.previousStatus = Objects.requireNonNull(previousStatus, "previousStatus cannot be null");
        this.newStatus = Objects.requireNonNull(newStatus, "newStatus cannot be null");
        this.reason = reason;
        this.timestamp = Instant.now();
        this.nodeState = nodeState;
    }

    public String getNodeId() {
        return nodeId;
    }

    public NodeStatus getPreviousStatus() {
        return previousStatus;
    }

    public NodeStatus getNewStatus() {
        return newStatus;
    }

    public String getReason() {
        return reason;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public NodeState getNodeState() {
        return nodeState;
    }

    public boolean isStatusChanged() {
        return previousStatus != newStatus;
    }

    @Override
    public String toString() {
        return String.format("StateChangeEvent{nodeId='%s', %s → %s, reason='%s', timestamp=%s}",
            nodeId, previousStatus, newStatus, reason, timestamp);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StateChangeEvent that = (StateChangeEvent) o;
        return Objects.equals(nodeId, that.nodeId) &&
               previousStatus == that.previousStatus &&
               newStatus == that.newStatus &&
               Objects.equals(timestamp, that.timestamp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nodeId, previousStatus, newStatus, timestamp);
    }
}
