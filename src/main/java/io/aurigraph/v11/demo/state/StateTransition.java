package io.aurigraph.v11.demo.state;

import java.util.*;

/**
 * StateTransition - Manages and validates state transitions for nodes
 *
 * This class implements the state machine logic for node states with the following rules:
 *
 * Valid Transitions:
 * - INITIALIZING → RUNNING (successful initialization)
 * - INITIALIZING → ERROR (initialization failed)
 * - RUNNING → PAUSED (graceful pause)
 * - RUNNING → STOPPED (graceful shutdown)
 * - RUNNING → ERROR (runtime error)
 * - PAUSED → RUNNING (resume from pause)
 * - PAUSED → STOPPED (shutdown while paused)
 * - PAUSED → ERROR (error while paused)
 * - ERROR → STOPPED (cleanup after error)
 *
 * Terminal States:
 * - STOPPED: No transitions allowed
 * - ERROR: Can only transition to STOPPED
 *
 * @author Backend Development Agent (BDA)
 * @version 1.0
 * @since AV11-216
 */
public class StateTransition {

    // Define valid state transitions
    private static final Map<NodeStatus, Set<NodeStatus>> VALID_TRANSITIONS = new EnumMap<>(NodeStatus.class);

    static {
        // INITIALIZING can transition to RUNNING or ERROR
        VALID_TRANSITIONS.put(NodeStatus.INITIALIZING, EnumSet.of(
            NodeStatus.RUNNING,
            NodeStatus.ERROR
        ));

        // RUNNING can transition to PAUSED, STOPPED, or ERROR
        VALID_TRANSITIONS.put(NodeStatus.RUNNING, EnumSet.of(
            NodeStatus.PAUSED,
            NodeStatus.STOPPED,
            NodeStatus.ERROR
        ));

        // PAUSED can transition to RUNNING, STOPPED, or ERROR
        VALID_TRANSITIONS.put(NodeStatus.PAUSED, EnumSet.of(
            NodeStatus.RUNNING,
            NodeStatus.STOPPED,
            NodeStatus.ERROR
        ));

        // ERROR can only transition to STOPPED
        VALID_TRANSITIONS.put(NodeStatus.ERROR, EnumSet.of(
            NodeStatus.STOPPED
        ));

        // STOPPED is terminal - no valid transitions
        VALID_TRANSITIONS.put(NodeStatus.STOPPED, EnumSet.noneOf(NodeStatus.class));
    }

    /**
     * Validates if a state transition is allowed
     *
     * @param from current state
     * @param to desired state
     * @return true if transition is valid, false otherwise
     */
    public static boolean isValidTransition(NodeStatus from, NodeStatus to) {
        if (from == null || to == null) {
            return false;
        }

        // Same state is always valid (idempotent)
        if (from == to) {
            return true;
        }

        Set<NodeStatus> validTargets = VALID_TRANSITIONS.get(from);
        return validTargets != null && validTargets.contains(to);
    }

    /**
     * Gets all valid target states from the current state
     *
     * @param from current state
     * @return set of valid target states
     */
    public static Set<NodeStatus> getValidTransitions(NodeStatus from) {
        if (from == null) {
            return Collections.emptySet();
        }

        Set<NodeStatus> validTargets = VALID_TRANSITIONS.get(from);
        return validTargets != null ? EnumSet.copyOf(validTargets) : Collections.emptySet();
    }

    /**
     * Validates a state transition and throws exception if invalid
     *
     * @param from current state
     * @param to desired state
     * @throws IllegalStateTransitionException if transition is not allowed
     */
    public static void validateTransition(NodeStatus from, NodeStatus to)
            throws IllegalStateTransitionException {
        if (!isValidTransition(from, to)) {
            throw new IllegalStateTransitionException(
                String.format("Invalid state transition from %s to %s. Valid transitions from %s are: %s",
                    from, to, from, getValidTransitions(from))
            );
        }
    }

    /**
     * Checks if a state is terminal (no valid transitions from it)
     *
     * @param status the state to check
     * @return true if the state is terminal
     */
    public static boolean isTerminalState(NodeStatus status) {
        return status != null &&
               (status == NodeStatus.STOPPED ||
                VALID_TRANSITIONS.getOrDefault(status, Collections.emptySet()).isEmpty());
    }

    /**
     * Gets the recommended transition reason based on the state change
     *
     * @param from source state
     * @param to target state
     * @return recommended reason string
     */
    public static String getRecommendedReason(NodeStatus from, NodeStatus to) {
        if (from == to) {
            return "State unchanged";
        }

        return switch (to) {
            case RUNNING -> from == NodeStatus.INITIALIZING ?
                "Node initialization completed successfully" :
                "Node resumed from paused state";
            case PAUSED -> "Node paused for maintenance or configuration update";
            case STOPPED -> "Node stopped gracefully";
            case ERROR -> "Node encountered a critical error";
            default -> String.format("Transitioned from %s to %s", from, to);
        };
    }

    /**
     * Creates a transition result with validation
     *
     * @param from source state
     * @param to target state
     * @param reason reason for transition
     * @return TransitionResult object
     */
    public static TransitionResult createTransition(NodeStatus from, NodeStatus to, String reason) {
        boolean isValid = isValidTransition(from, to);
        String finalReason = (reason != null && !reason.trim().isEmpty()) ?
            reason : getRecommendedReason(from, to);

        return new TransitionResult(from, to, isValid, finalReason);
    }

    /**
     * Result of a state transition validation
     */
    public static class TransitionResult {
        private final NodeStatus fromState;
        private final NodeStatus toState;
        private final boolean valid;
        private final String reason;

        public TransitionResult(NodeStatus fromState, NodeStatus toState, boolean valid, String reason) {
            this.fromState = fromState;
            this.toState = toState;
            this.valid = valid;
            this.reason = reason;
        }

        public NodeStatus getFromState() {
            return fromState;
        }

        public NodeStatus getToState() {
            return toState;
        }

        public boolean isValid() {
            return valid;
        }

        public String getReason() {
            return reason;
        }

        @Override
        public String toString() {
            return String.format("TransitionResult{%s → %s, valid=%s, reason='%s'}",
                fromState, toState, valid, reason);
        }
    }

    /**
     * Exception thrown when an invalid state transition is attempted
     */
    public static class IllegalStateTransitionException extends Exception {
        public IllegalStateTransitionException(String message) {
            super(message);
        }
    }
}
